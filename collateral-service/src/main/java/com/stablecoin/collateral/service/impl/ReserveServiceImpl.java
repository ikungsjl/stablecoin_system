package com.stablecoin.collateral.service.impl;

import com.stablecoin.collateral.client.IssuanceServiceClient;
import com.stablecoin.collateral.dto.DashboardResponse;
import com.stablecoin.collateral.dto.ReserveCheckResponse;
import com.stablecoin.collateral.entity.ReservePool;
import com.stablecoin.collateral.entity.ReserveSnapshot;
import com.stablecoin.collateral.entity.RiskAlert;
import com.stablecoin.collateral.exception.BusinessException;
import com.stablecoin.collateral.mq.CollateralEventPublisher;
import com.stablecoin.collateral.repository.ReserveAssetRepository;
import com.stablecoin.collateral.repository.ReservePoolRepository;
import com.stablecoin.collateral.repository.ReserveSnapshotRepository;
import com.stablecoin.collateral.repository.RiskAlertRepository;
import com.stablecoin.collateral.service.ReserveService;
import com.stablecoin.collateral.service.RiskAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReserveServiceImpl implements ReserveService {

    private final ReservePoolRepository reservePoolRepository;
    private final ReserveSnapshotRepository snapshotRepository;
    private final RiskAlertRepository alertRepository;
    private final RiskAlertService riskAlertService;
    private final CollateralEventPublisher eventPublisher;
    private final ReserveAssetRepository assetRepository;
    private final IssuanceServiceClient issuanceServiceClient;

    @Value("${app.reserve.healthy-threshold:1.10}")
    private BigDecimal healthyThreshold;
    @Value("${app.reserve.normal-threshold:1.00}")
    private BigDecimal normalThreshold;
    @Value("${app.reserve.warning-threshold:0.90}")
    private BigDecimal warningThreshold;
    @Value("${app.reserve.low-risk-ratio-threshold:0.80}")
    private BigDecimal lowRiskRatioThreshold;

    private final AtomicReference\u003cBigDecimal\u003e cachedSupply = new AtomicReference\u003c\u003e(BigDecimal.ZERO);
    private final AtomicReference\u003cString\u003e lastRiskLevel = new AtomicReference\u003c\u003e(ReserveSnapshot.RiskLevel.HEALTHY.name());
    private final AtomicReference\u003cBoolean\u003e lastLowRiskAlertTriggered = new AtomicReference\u003c\u003e(false);

    // ----------------------------------------------------------------
    // checkAndSnapshot
    // ----------------------------------------------------------------

    @Override
    @Transactional
    public ReserveSnapshot checkAndSnapshot() {
        ReservePool pool = getReservePool();
        BigDecimal supply = fetchStablecoinSupply();
        BigDecimal ratio;
        String riskLevel;
        if (supply.compareTo(BigDecimal.ZERO) == 0) {
            ratio = new BigDecimal("999.999999");
            riskLevel = ReserveSnapshot.RiskLevel.HEALTHY.name();
        } else {
            ratio = pool.getTotalUsdAmount().divide(supply, 6, RoundingMode.HALF_UP);
            riskLevel = evaluateRiskLevel(ratio);
        }
        ReserveSnapshot snapshot = ReserveSnapshot.builder()
                .reserveAmount(pool.getTotalUsdAmount())
                .stablecoinSupply(supply)
                .reserveRatio(ratio)
                .riskLevel(riskLevel)
                .snapshotAt(LocalDateTime.now())
                .build();
        snapshotRepository.save(snapshot);
        log.info("[Reserve] snapshot: reserve={} supply={} ratio={} level={}",
                pool.getTotalUsdAmount(), supply, ratio, riskLevel);
        checkLowRiskAssetRatio();
        handleRisk(ratio, riskLevel, pool.getTotalUsdAmount(), supply);
        return snapshot;
    }

    // ----------------------------------------------------------------
    // checkReserve - 课题1调用：验证储备，充足则持久化 supply
    // ----------------------------------------------------------------

    @Override
    @Transactional
    public ReserveCheckResponse checkReserve(BigDecimal stablecoinSupply) {
        ReservePool pool = getReservePool();
        BigDecimal ratio = stablecoinSupply.compareTo(BigDecimal.ZERO) == 0
                ? new BigDecimal("999.999999")
                : pool.getTotalUsdAmount().divide(stablecoinSupply, 6, RoundingMode.HALF_UP);
        String riskLevel = evaluateRiskLevel(ratio);
        boolean available = ratio.compareTo(normalThreshold) \u003e= 0;

        String message;
        if (available) {
            // 储备充足：持久化流通量，供仪表盘和快照使用
            BigDecimal previous = pool.getStablecoinSupply() != null
                    ? pool.getStablecoinSupply() : BigDecimal.ZERO;
            pool.setStablecoinSupply(stablecoinSupply);
            reservePoolRepository.save(pool);
            cachedSupply.set(stablecoinSupply);
            log.info("[Reserve] checkReserve PASS: supply {} -> {} ratio={} level={}",
                    previous, stablecoinSupply, ratio, riskLevel);
            message = String.format(
                    "储备充足，已记录流通量 %s USD，储备率 %.6f，风险等级 %s",
                    stablecoinSupply.toPlainString(), ratio, riskLevel);
        } else {
            // 储备不足：不更新 supply，返回拒绝信息
            log.warn("[Reserve] checkReserve FAIL: supply={} reserve={} ratio={} level={}",
                    stablecoinSupply, pool.getTotalUsdAmount(), ratio, riskLevel);
            message = String.format(
                    "储备不足，无法支持发行 %s USD 稳定币。当前储备 %s USD，储备率 %.6f，" +
                    "请先通过 POST /api/collateral/deposit 补充抵押物",
                    stablecoinSupply.toPlainString(),
                    pool.getTotalUsdAmount().toPlainString(), ratio);
        }

        return ReserveCheckResponse.builder()
                .reserveRatio(ratio)
                .riskLevel(riskLevel)
                .available(available)
                .totalReserve(pool.getTotalUsdAmount())
                .stablecoinSupply(stablecoinSupply)
                .message(message)
                .build();
    }

    // ----------------------------------------------------------------
    // getDashboard
    // ----------------------------------------------------------------

    @Override
    public DashboardResponse getDashboard() {
        ReservePool pool = getReservePool();
        // 优先使用持久化的流通量
        BigDecimal supply = (pool.getStablecoinSupply() != null
                && pool.getStablecoinSupply().compareTo(BigDecimal.ZERO) \u003e 0)
                ? pool.getStablecoinSupply() : cachedSupply.get();
        BigDecimal ratio = supply.compareTo(BigDecimal.ZERO) == 0
                ? new BigDecimal("999.999999")
                : pool.getTotalUsdAmount().divide(supply, 6, RoundingMode.HALF_UP);
        long activeCount = alertRepository.findByStatusOrderByCreatedAtDesc(
                RiskAlert.AlertStatus.ACTIVE.name()).size();
        BigDecimal assetTotal = assetRepository.sumTotalUsdValue();
        BigDecimal lowRisk  = sumAssetByRisk(1);
        BigDecimal medRisk  = sumAssetByRisk(2);
        BigDecimal highRisk = sumAssetByRisk(3);
        return DashboardResponse.builder()
                .totalReserve(pool.getTotalUsdAmount())
                .lockedAmount(pool.getLockedAmount())
                .availableReserve(pool.getAvailableAmount())
                .stablecoinSupply(supply)
                .reserveRatio(ratio)
                .riskLevel(evaluateRiskLevel(ratio))
                .activeAlertCount(activeCount)
                .updatedAt(pool.getLastUpdated())
                .assetTotalValue(assetTotal)
                .lowRiskRatio(assetTotal.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                        : lowRisk.divide(assetTotal, 6, RoundingMode.HALF_UP))
                .mediumRiskRatio(assetTotal.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                        : medRisk.divide(assetTotal, 6, RoundingMode.HALF_UP))
                .highRiskRatio(assetTotal.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                        : highRisk.divide(assetTotal, 6, RoundingMode.HALF_UP))
                .estimatedAnnualIncome(assetRepository.estimateAnnualInterestIncome())
                .build();
    }

    @Override
    public List\u003cReserveSnapshot\u003e getHistory(LocalDateTime start, LocalDateTime end) {
        return snapshotRepository.findBySnapshotAtBetweenOrderBySnapshotAtAsc(start, end);
    }

    @Override
    public BigDecimal fetchStablecoinSupply() {
        // 优先级1：从课题1实时拉取
        BigDecimal supply = issuanceServiceClient.fetchTotalSupply();
        if (supply != null) {
            cachedSupply.set(supply);
            return supply;
        }
        // 优先级2：本地持久化缓存
        ReservePool pool = getReservePool();
        if (pool.getStablecoinSupply() != null
                && pool.getStablecoinSupply().compareTo(BigDecimal.ZERO) \u003e 0) {
            log.warn("[Reserve] 课题1不可达，使用持久化流通量: {}", pool.getStablecoinSupply());
            cachedSupply.set(pool.getStablecoinSupply());
            return pool.getStablecoinSupply();
        }
        // 优先级3：内存缓存
        log.warn("[Reserve] 使用内存缓存流通量: {}", cachedSupply.get());
        return cachedSupply.get();
    }

    // ----------------------------------------------------------------
    // private helpers
    // ----------------------------------------------------------------

    private String evaluateRiskLevel(BigDecimal ratio) {
        if (ratio.compareTo(healthyThreshold) \u003e= 0) return ReserveSnapshot.RiskLevel.HEALTHY.name();
        if (ratio.compareTo(normalThreshold)  \u003e= 0) return ReserveSnapshot.RiskLevel.NORMAL.name();
        if (ratio.compareTo(warningThreshold) \u003e= 0) return ReserveSnapshot.RiskLevel.WARNING.name();
        return ReserveSnapshot.RiskLevel.CRITICAL.name();
    }

    private void checkLowRiskAssetRatio() {
        BigDecimal totalAsset = assetRepository.sumTotalUsdValue();
        if (totalAsset.compareTo(BigDecimal.ZERO) == 0) return;
        BigDecimal lowRiskRatio = sumAssetByRisk(1).divide(totalAsset, 6, RoundingMode.HALF_UP);
        boolean insufficient = lowRiskRatio.compareTo(lowRiskRatioThreshold) \u003c 0;
        boolean prev = lastLowRiskAlertTriggered.get();
        if (insufficient \u0026\u0026 !prev) {
            BigDecimal gap = lowRiskRatioThreshold.subtract(lowRiskRatio);
            riskAlertService.triggerLowRiskAssetAlert(lowRiskRatio, gap);
            eventPublisher.publishLowRiskAssetAlert(lowRiskRatio, gap);
            lastLowRiskAlertTriggered.set(true);
            log.warn("[Reserve] 低风险资产不足: ratio={} gap={}", lowRiskRatio, gap);
        } else if (!insufficient \u0026\u0026 prev) {
            riskAlertService.resolveLowRiskAssetAlert();
            eventPublisher.publishLowRiskAssetRecovered(lowRiskRatio);
            lastLowRiskAlertTriggered.set(false);
            log.info("[Reserve] 低风险资产已恢复: ratio={}", lowRiskRatio);
        }
    }

    private void handleRisk(BigDecimal ratio, String riskLevel,
                            BigDecimal reserve, BigDecimal supply) {
        String prev = lastRiskLevel.get();
        boolean changed = !riskLevel.equals(prev);
        if (riskLevel.equals(ReserveSnapshot.RiskLevel.WARNING.name()) ||
            riskLevel.equals(ReserveSnapshot.RiskLevel.CRITICAL.name())) {
            BigDecimal gap = supply.subtract(reserve).max(BigDecimal.ZERO);
            riskAlertService.triggerReserveInsufficientAlert(ratio, gap);
            eventPublisher.publishRiskAlert(riskLevel, ratio, gap);
            issuanceServiceClient.notifyReserveInsufficient(ratio, riskLevel, gap);
            if (changed) { eventPublisher.publishRiskLevelChange(prev, riskLevel); lastRiskLevel.set(riskLevel); }
        } else if (riskLevel.equals(ReserveSnapshot.RiskLevel.HEALTHY.name())) {
            eventPublisher.publishReserveHealthy(ratio);
            issuanceServiceClient.notifyReserveRecovered(ratio);
            if (changed) { eventPublisher.publishRiskLevelChange(prev, riskLevel); lastRiskLevel.set(riskLevel); }
        }
    }

    private BigDecimal sumAssetByRisk(Integer riskLevel) {
        return assetRepository.findByRiskLevelOrderByUsdValueDesc(riskLevel).stream()
                .map(a -> a.getUsdValue()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ReservePool getReservePool() {
        return reservePoolRepository.findById(1L)
                .orElseThrow(() -> new BusinessException("Reserve pool not initialized"));
    }
}
