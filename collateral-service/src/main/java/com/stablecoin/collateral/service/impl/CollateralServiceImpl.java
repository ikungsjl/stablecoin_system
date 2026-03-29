package com.stablecoin.collateral.service.impl;

import com.stablecoin.collateral.dto.DepositRequest;
import com.stablecoin.collateral.entity.CollateralDeposit;
import com.stablecoin.collateral.entity.ReservePool;
import com.stablecoin.collateral.exception.BusinessException;
import com.stablecoin.collateral.repository.CollateralDepositRepository;
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
 * 前端课题传入 txHash 和 depositedAt，课题2验证并记录
 * 每次存入或赎回（锁定）操作后，自动触发风险预判
 */
@Slf4j
@Service
public class CollateralServiceImpl implements CollateralService {

    private final CollateralDepositRepository depositRepository;
    private final ReservePoolRepository reservePoolRepository;
    private final ReserveService reserveService;

    @Autowired
    public CollateralServiceImpl(
            CollateralDepositRepository depositRepository,
            ReservePoolRepository reservePoolRepository,
            @Lazy ReserveService reserveService) {
        this.depositRepository = depositRepository;
        this.reservePoolRepository = reservePoolRepository;
        this.reserveService = reserveService;
    }

    @Override
    @Transactional
    public CollateralDeposit deposit(DepositRequest request) {
        // 1. 验证 txHash 唯一性（防止重复存入）
        if (depositRepository.findByTxHash(request.getTxHash()).isPresent()) {
            throw new BusinessException("交易已存在: " + request.getTxHash());
        }

        // 2. 保存存入记录（使用前端传入的 txHash 和 depositedAt）
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

        // 3. 更新储备池（1 USD 存入 = 储备 +1 USD）
        updateReservePool(request.getAmount());

        log.info("[Collateral] 存入成功 txHash={} amount={} USD operator={} depositedAt={}",
                request.getTxHash(), request.getAmount(), request.getOperator(), request.getDepositedAt());

        // 4. 触发风险预判
        triggerRiskCheck("deposit");

        return deposit;
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

        // 锁定减少可用储备，触发风险预判
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

    /**
     * 触发风险预判
     * 若风险检查失败不影响主业务流程（存入/赎回照常完成）
     */
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
