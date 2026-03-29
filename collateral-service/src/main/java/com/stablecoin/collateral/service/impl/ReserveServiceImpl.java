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

    private final AtomicReference<BigDecimal> cachedSupply =
            new AtomicReference<>(BigDecimal.ZERO);
    
    /** 缓存上一次的风险等级，用于检测风险变化 */
    private final AtomicReference<String> lastRiskLevel =
            new AtomicReference<>(ReserveSnapshot.RiskLevel.HEALTHY.name());

    /** 缓存上一次的低风险资产占比告警状态 */
    private final AtomicReference<Boolean> lastLowRiskAlertTriggered =
            new AtomicReference<>(false);

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

        // 检查低风险资产占比
        checkLowRiskAssetRatio();

        handleRisk(ratio, riskLevel, pool.getTotalUsdAmount(), supply);
        return snapshot;
    }

    @Override
    public ReserveCheckResponse checkReserve(BigDecimal stablecoinSupply) {
        ReservePool pool = getReservePool();
        BigDecimal ratio = stablecoinSupply.compareTo(BigDecimal.ZERO) == 0
                ? new BigDecimal("999.999999")
                : pool.getTotalUsdAmount().divide(stablecoinSupply, 6, RoundingMode.HALF_UP);
        String riskLevel = evaluateRiskLevel(ratio);
        boolean available = ratio.compareTo(normalThreshold) >= 0;
        return ReserveCheckResponse.builder()
                .reserveRatio(ratio)
                .riskLevel(riskLevel)
                .available(available)
                .totalReserve(pool.getTotalUsdAmount())
                .stablecoinSupply(stablecoinSupply)
                .message(available ? "Reserve sufficient" : "Reserve insufficient, issuance suspended")
                .build();
    }

    @Override
    public DashboardResponse getDashboard() {
        ReservePool pool = getReservePool();
        BigDecimal supply = cachedSupply.get();
        BigDecimal ratio = supply.compareTo(BigDecimal.ZERO) == 0
                ? new BigDecimal("999.999999")
                : pool.getTotalUsdAmount().divide(supply, 6, RoundingMode.HALF_UP);

        long activeCount = alertRepository.findByStatusOrderByCreatedAtDesc(
                RiskAlert.AlertStatus.ACTIVE.name()).size();

        BigDecimal assetTotal = assetRepository.sumTotalUsdValue();
        BigDecimal annualIncome = assetRepository.estimateAnnualInterestIncome();
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
                .estimatedAnnualIncome(annualIncome)
                .build();
    }

    @Override
    public List<ReserveSnapshot> getHistory(LocalDateTime start, LocalDateTime end) {
        return snapshotRepository.findBySnapshotAtBetweenOrderBySnapshotAtAsc(start, end);
    }

    @Override
    public BigDecimal fetchStablecoinSupply() {
        BigDecimal supply = issuanceServiceClient.fetchTotalSupply();
        if (supply != null) {
            cachedSupply.set(supply);
            return supply;
        }
        log.warn("[Reserve] Failed to fetch stablecoin supply, using cached: {}", cachedSupply.get());
        return cachedSupply.get();
    }

    // ---- private helpers ----

    private String evaluateRiskLevel(BigDecimal ratio) {
        if (ratio.compareTo(healthyThreshold) >= 0) return ReserveSnapshot.RiskLevel.HEALTHY.name();
        if (ratio.compareTo(normalThreshold)  >= 0) return ReserveSnapshot.RiskLevel.NORMAL.name();
        if (ratio.compareTo(warningThreshold) >= 0) return ReserveSnapshot.RiskLevel.WARNING.name();
        return ReserveSnapshot.RiskLevel.CRITICAL.name();
    }

    /**
     * 检查低风险资产占比是否满足要求（≥ 80%）
     * 如果不满足，触发告警
     */
    private void checkLowRiskAssetRatio() {
        BigDecimal totalAsset = assetRepository.sumTotalUsdValue();
        if (totalAsset.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal lowRiskAsset = sumAssetByRisk(1);
        BigDecimal lowRiskRatio = lowRiskAsset.divide(totalAsset, 6, RoundingMode.HALF_UP);

        boolean isLowRiskInsufficient = lowRiskRatio.compareTo(lowRiskRatioThreshold) < 0;

        log.info("[Reserve] 低风险资产占比检查: ratio={} threshold={} insufficient={}",
                lowRiskRatio, lowRiskRatioThreshold, isLowRiskInsufficient);

        // 检测告警状态变化
        boolean previousAlertTriggered = lastLowRiskAlertTriggered.get();

        if (isLowRiskInsufficient && !previousAlertTriggered) {
            // 从充足变为不充足，触发告警
            BigDecimal gap = lowRiskRatioThreshold.subtract(lowRiskRatio);
            riskAlertService.triggerLowRiskAssetAlert(lowRiskRatio, gap);
            eventPublisher.publishLowRiskAssetAlert(lowRiskRatio, gap);
            lastLowRiskAlertTriggered.set(true);
            log.warn("[Reserve] 低风险资产占比不足告警已触发: ratio={} gap={}", lowRiskRatio, gap);

        } else if (!isLowRiskInsufficient && previousAlertTriggered) {
            // 从不充足变为充足，清除告警
            riskAlertService.resolveLowRiskAssetAlert();
            eventPublisher.publishLowRiskAssetRecovered(lowRiskRatio);
            lastLowRiskAlertTriggered.set(false);
            log.info("[Reserve] 低风险资产占比已恢复: ratio={}", lowRiskRatio);
        }
    }

    /**
     * 风险处理：
     * - WARNING / CRITICAL：保存告警 + 内部 MQ 广播 + 主动调用课题1 API 暂停发行 + 发布风险变化事件
     * - HEALTHY：内部 MQ 广播恢复 + 主动调用课题1 API 恢复发行 + 发布风险变化事件
     * 
     * 风险变化事件由 AuditReportEventConsumer 异步消费，触发审计报告生成
     */
    private void handleRisk(BigDecimal ratio, String riskLevel,
                            BigDecimal reserve, BigDecimal supply) {
        String previousRiskLevel = lastRiskLevel.get();
        boolean riskLevelChanged = !riskLevel.equals(previousRiskLevel);

        if (riskLevel.equals(ReserveSnapshot.RiskLevel.WARNING.name()) ||
            riskLevel.equals(ReserveSnapshot.RiskLevel.CRITICAL.name())) {

            BigDecimal gap = supply.subtract(reserve).max(BigDecimal.ZERO);

            // 1. 保存内部风险告警记录
            riskAlertService.triggerReserveInsufficientAlert(ratio, gap);

            // 2. 内部 MQ 广播告警
            eventPublisher.publishRiskAlert(riskLevel, ratio, gap);

            // 3. 主动调用课题1 REST API，通知暂停稳定币发行
            issuanceServiceClient.notifyReserveInsufficient(ratio, riskLevel, gap);

            // 4. 风险等级变化时发布事件，触发审计报告异步生成
            if (riskLevelChanged) {
                eventPublisher.publishRiskLevelChange(previousRiskLevel, riskLevel);
                lastRiskLevel.set(riskLevel);
            }

        } else if (riskLevel.equals(ReserveSnapshot.RiskLevel.HEALTHY.name())) {

            // 1. 内部 MQ 广播恢复事件
            eventPublisher.publishReserveHealthy(ratio);

            // 2. 主动调用课题1 REST API，通知恢复稳定币发行
            issuanceServiceClient.notifyReserveRecovered(ratio);

            // 3. 风险等级变化时发布事件，触发审计报告异步生成
            if (riskLevelChanged) {
                eventPublisher.publishRiskLevelChange(previousRiskLevel, riskLevel);
                lastRiskLevel.set(riskLevel);
            }
        }
    }

    private BigDecimal sumAssetByRisk(Integer riskLevel) {
        return assetRepository.findByRiskLevelOrderByUsdValueDesc(riskLevel).stream()
                .map(a -> a.getUsdValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ReservePool getReservePool() {
        return reservePoolRepository.findById(1L)
                .orElseThrow(() -> new BusinessException("Reserve pool not initialized"));
    }
}
