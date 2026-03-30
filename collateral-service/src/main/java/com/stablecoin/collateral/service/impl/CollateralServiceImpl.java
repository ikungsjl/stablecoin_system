package com.stablecoin.collateral.service.impl;

import com.stablecoin.collateral.client.IssuanceServiceClient;
import com.stablecoin.collateral.dto.DepositRequest;
import com.stablecoin.collateral.dto.DepositResponse;
import com.stablecoin.collateral.entity.CollateralDeposit;
import com.stablecoin.collateral.entity.ReserveAsset;
import com.stablecoin.collateral.entity.ReservePool;
import com.stablecoin.collateral.exception.BusinessException;
import com.stablecoin.collateral.repository.CollateralDepositRepository;
import com.stablecoin.collateral.repository.ReserveAssetRepository;
import com.stablecoin.collateral.repository.ReservePoolRepository;
import com.stablecoin.collateral.service.CollateralService;
import com.stablecoin.collateral.service.ReserveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 抵押物服务实现
 *
 * 存入流程：
 *   1. 验证 txHash 唯一性（防重入）
 *   2. 保存存入记录
 *   3. 更新储备池 total_usd_amount（+存入金额）
 *   4. 同步增加 reserve_assets 中 CASH 资产的 usd_value（方案A：存入视为现金入账）
 *   5. 检查课题1可用稳定币余额，不足则请求发行
 *   6. 触发储备风险快照
 */
@Slf4j
@Service
public class CollateralServiceImpl implements CollateralService {

    /** 存入的抵押物先以现金形式记入储备资产 */
    private static final String CASH_ASSET_TYPE = ReserveAsset.AssetType.CASH.name();

    private final CollateralDepositRepository depositRepository;
    private final ReservePoolRepository reservePoolRepository;
    private final ReserveAssetRepository assetRepository;
    private final ReserveService reserveService;
    private final IssuanceServiceClient issuanceServiceClient;

    @Autowired
    public CollateralServiceImpl(
            CollateralDepositRepository depositRepository,
            ReservePoolRepository reservePoolRepository,
            ReserveAssetRepository assetRepository,
            @Lazy ReserveService reserveService,
            IssuanceServiceClient issuanceServiceClient) {
        this.depositRepository = depositRepository;
        this.reservePoolRepository = reservePoolRepository;
        this.assetRepository = assetRepository;
        this.reserveService = reserveService;
        this.issuanceServiceClient = issuanceServiceClient;
    }

    @Override
    @Transactional
    public DepositResponse deposit(DepositRequest request) {
        // 1. 验证 txHash 唯一性（防止重复存入）
        if (depositRepository.findByTxHash(request.getTxHash()).isPresent()) {
            throw new BusinessException("交易已存在: " + request.getTxHash());
        }

        // 2. 保存存入记录
        CollateralDeposit deposit = CollateralDeposit.builder()
                .txHash(request.getTxHash())
                .amount(request.getAmount())
                .currency("USD")
                .usdAmount(request.getAmount())
                .exchangeRate(BigDecimal.ONE)
                .operator(request.getOperator())
                .status(CollateralDeposit.Status.CONFIRMED.name())
                .remark(request.getRemark())
                .depositedAt(request.getDepositedAt())
                .build();
        depositRepository.save(deposit);

        // 3. 更新储备池总额
        updateReservePool(request.getAmount());

        // 4. 同步增加 CASH 资产 usd_value（存入的抵押物以现金形式进入储备）
        int updated = assetRepository.incrementUsdValue(CASH_ASSET_TYPE, request.getAmount());
        if (updated == 0) {
            // CASH 记录不存在时记录警告，不阻断主流程
            log.warn("[Collateral] reserve_assets 中未找到 CASH 记录，请检查初始化数据 txHash={}",
                    request.getTxHash());
        } else {
            log.info("[Collateral] CASH 资产已同步增加 amount={} USD txHash={}",
                    request.getAmount(), request.getTxHash());
        }

        log.info("[Collateral] 存入成功 txHash={} amount={} USD operator={} depositedAt={}",
                request.getTxHash(), request.getAmount(),
                request.getOperator(), request.getDepositedAt());

        // 5. 检查课题1可用稳定币余额，按需请求发行
        DepositResponse.IssuanceStatus issuanceStatus;
        String message;
        BigDecimal availableSupply = null;
        BigDecimal required = request.getAmount();

        try {
            availableSupply = issuanceServiceClient.fetchTotalSupply();

            if (availableSupply == null) {
                issuanceStatus = DepositResponse.IssuanceStatus.ISSUANCE_UNAVAILABLE;
                message = "存入已记录，但无法连接课题1服务，请手动确认稳定币发行状态";
                log.warn("[Collateral] 课题1服务不可达，无法检查稳定币余额 txHash={}",
                        request.getTxHash());
            } else if (availableSupply.compareTo(required) >= 0) {
                issuanceStatus = DepositResponse.IssuanceStatus.SUFFICIENT;
                message = String.format(
                        "存入成功，课题1可用稳定币余额充足（可用: %s USD，本次需分配: %s USD）",
                        availableSupply.toPlainString(), required.toPlainString());
                log.info("[Collateral] 稳定币余额充足，无需发行 available={} required={} txHash={}",
                        availableSupply, required, request.getTxHash());
            } else {
                BigDecimal shortage = required.subtract(availableSupply);
                log.warn("[Collateral] 稳定币余额不足，请求课题1发行 available={} required={} shortage={} txHash={}",
                        availableSupply, required, shortage, request.getTxHash());

                boolean accepted = issuanceServiceClient.requestIssuance(
                        shortage, request.getTxHash(), request.getOperator());

                if (accepted) {
                    issuanceStatus = DepositResponse.IssuanceStatus.ISSUANCE_REQUESTED;
                    message = String.format(
                            "存入成功，稳定币余额不足（可用: %s USD，需分配: %s USD，缺口: %s USD），已请求课题1补充发行",
                            availableSupply.toPlainString(),
                            required.toPlainString(),
                            shortage.toPlainString());
                } else {
                    issuanceStatus = DepositResponse.IssuanceStatus.ISSUANCE_UNAVAILABLE;
                    message = String.format(
                            "存入成功，但稳定币余额不足（可用: %s USD，需分配: %s USD），且课题1发行请求失败，请手动处理",
                            availableSupply.toPlainString(),
                            required.toPlainString());
                    log.error("[Collateral] 课题1发行请求失败，需人工介入 txHash={} shortage={}",
                            request.getTxHash(), shortage);
                }
            }
        } catch (Exception e) {
            issuanceStatus = DepositResponse.IssuanceStatus.ISSUANCE_UNAVAILABLE;
            message = "存入已记录，稳定币余额检查异常: " + e.getMessage();
            log.error("[Collateral] 稳定币余额检查异常 txHash={}: {}",
                    request.getTxHash(), e.getMessage());
        }

        // 6. 触发储备风险快照
        triggerRiskCheck("deposit");

        return DepositResponse.builder()
                .deposit(deposit)
                .requiredStablecoin(required)
                .availableSupply(availableSupply)
                .issuanceStatus(issuanceStatus)
                .message(message)
                .build();
    }

    @Override
    public List<CollateralDeposit> listDeposits(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null)
            return depositRepository.findByDepositedAtBetweenOrderByDepositedAtDesc(start, end);
        return depositRepository.findAll();
    }

    @Override
    public CollateralDeposit getByTxHash(String txHash) {
        return depositRepository.findByTxHash(txHash)
                .orElseThrow(() -> new BusinessException("交易记录不存在: " + txHash));
    }

    @Override
    public BigDecimal getTotalReserve() {
        return getReservePool().getTotalUsdAmount();
    }

    @Override
    @Transactional
    public void updateReservePool(BigDecimal deltaUsd) {
        ReservePool pool = getReservePool();
        pool.setTotalUsdAmount(pool.getTotalUsdAmount().add(deltaUsd));
        reservePoolRepository.save(pool);
    }

    @Override
    @Transactional
    public void lockAmount(BigDecimal amount) {
        ReservePool pool = getReservePool();
        pool.setLockedAmount(pool.getLockedAmount().add(amount));
        reservePoolRepository.save(pool);
        triggerRiskCheck("lock");
    }

    @Override
    @Transactional
    public void unlockAmount(BigDecimal amount) {
        ReservePool pool = getReservePool();
        pool.setLockedAmount(pool.getLockedAmount().subtract(amount).max(BigDecimal.ZERO));
        reservePoolRepository.save(pool);
    }

    // ---- private helpers ----

    private void triggerRiskCheck(String trigger) {
        try {
            log.debug("[Collateral] 触发风险预判 trigger={}", trigger);
            reserveService.checkAndSnapshot();
        } catch (Exception e) {
            log.warn("[Collateral] 风险预判执行失败，不影响主业务: {}", e.getMessage());
        }
    }

    private ReservePool getReservePool() {
        return reservePoolRepository.findById(1L)
                .orElseThrow(() -> new BusinessException("储备池未初始化，请先执行 init.sql"));
    }
}
