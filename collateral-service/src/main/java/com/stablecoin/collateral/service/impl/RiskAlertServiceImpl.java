package com.stablecoin.collateral.service.impl;

import com.stablecoin.collateral.entity.RiskAlert;
import com.stablecoin.collateral.exception.BusinessException;
import com.stablecoin.collateral.repository.RiskAlertRepository;
import com.stablecoin.collateral.service.RiskAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAlertServiceImpl implements RiskAlertService {

    private final RiskAlertRepository alertRepository;

    @Value("${app.risk.large-redemption-ratio:0.20}")
    private BigDecimal largeRedemptionRatio;

    @Override
    @Transactional
    public RiskAlert triggerReserveInsufficientAlert(BigDecimal reserveRatio, BigDecimal gapAmount) {
        // 去重：若已有同类型 ACTIVE 警报则跳过
        boolean exists = alertRepository
                .findTopByAlertTypeAndStatusOrderByCreatedAtDesc(
                        RiskAlert.AlertType.RESERVE_INSUFFICIENT.name(),
                        RiskAlert.AlertStatus.ACTIVE.name())
                .isPresent();
        if (exists) {
            log.debug("[RiskAlert] 储备不足警报已存在，跳过重复创建");
            return null;
        }

        String riskLevel = reserveRatio.compareTo(new BigDecimal("0.90")) < 0
                ? "CRITICAL" : "WARNING";

        RiskAlert alert = RiskAlert.builder()
                .alertType(RiskAlert.AlertType.RESERVE_INSUFFICIENT.name())
                .riskLevel(riskLevel)
                .reserveRatio(reserveRatio)
                .gapAmount(gapAmount)
                .description(String.format("储备率 %.2f%% 低于安全阈值，缺口金额 %.2f USD",
                        reserveRatio.multiply(new BigDecimal("100")).doubleValue(),
                        gapAmount.doubleValue()))
                .status(RiskAlert.AlertStatus.ACTIVE.name())
                .build();
        alertRepository.save(alert);
        log.warn("[RiskAlert] 触发警报: {} ratio={} gap={}", riskLevel, reserveRatio, gapAmount);
        return alert;
    }

    @Override
    @Transactional
    public RiskAlert triggerLargeRedemptionAlert(BigDecimal amount, BigDecimal totalReserve) {
        RiskAlert alert = RiskAlert.builder()
                .alertType(RiskAlert.AlertType.LARGE_REDEMPTION.name())
                .riskLevel("WARNING")
                .gapAmount(amount)
                .description(String.format("大额赎回请求 %.2f USD，占总储备 %.1f%%",
                        amount.doubleValue(),
                        amount.divide(totalReserve, 4, java.math.RoundingMode.HALF_UP)
                              .multiply(new BigDecimal("100")).doubleValue()))
                .status(RiskAlert.AlertStatus.ACTIVE.name())
                .build();
        alertRepository.save(alert);
        return alert;
    }

    @Override
    @Transactional
    public RiskAlert triggerLowRiskAssetAlert(BigDecimal lowRiskRatio, BigDecimal gap) {
        // 去重：若已有同类型 ACTIVE 警报则跳过
        boolean exists = alertRepository
                .findTopByAlertTypeAndStatusOrderByCreatedAtDesc(
                        RiskAlert.AlertType.LOW_RISK_ASSET_INSUFFICIENT.name(),
                        RiskAlert.AlertStatus.ACTIVE.name())
                .isPresent();
        if (exists) {
            log.debug("[RiskAlert] 低风险资产占比不足警报已存在，跳过重复创建");
            return null;
        }

        RiskAlert alert = RiskAlert.builder()
                .alertType(RiskAlert.AlertType.LOW_RISK_ASSET_INSUFFICIENT.name())
                .riskLevel("WARNING")
                .reserveRatio(lowRiskRatio)
                .gapAmount(gap)
                .description(String.format("低风险资产占比 %.2f%% 低于 80%% 要求，缺口 %.2f%%",
                        lowRiskRatio.multiply(new BigDecimal("100")).doubleValue(),
                        gap.multiply(new BigDecimal("100")).doubleValue()))
                .status(RiskAlert.AlertStatus.ACTIVE.name())
                .build();
        alertRepository.save(alert);
        log.warn("[RiskAlert] 触发低风险资产占比不足警报: ratio={} gap={}", lowRiskRatio, gap);
        return alert;
    }

    @Override
    @Transactional
    public void resolveLowRiskAssetAlert() {
        // 查找并解决低风险资产占比不足的 ACTIVE 警报
        alertRepository
                .findTopByAlertTypeAndStatusOrderByCreatedAtDesc(
                        RiskAlert.AlertType.LOW_RISK_ASSET_INSUFFICIENT.name(),
                        RiskAlert.AlertStatus.ACTIVE.name())
                .ifPresent(alert -> {
                    alert.setStatus(RiskAlert.AlertStatus.RESOLVED.name());
                    alert.setResolvedBy("system-auto");
                    alert.setResolvedAt(LocalDateTime.now());
                    alertRepository.save(alert);
                    log.info("[RiskAlert] 低风险资产占比不足警报已自动解决");
                });
    }

    @Override
    public List<RiskAlert> getActiveAlerts() {
        return alertRepository.findByStatusOrderByCreatedAtDesc(
                RiskAlert.AlertStatus.ACTIVE.name());
    }

    @Override
    @Transactional
    public RiskAlert acknowledgeAlert(Long alertId, String operator) {
        RiskAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException("警报不存在: " + alertId));
        alert.setStatus(RiskAlert.AlertStatus.ACKNOWLEDGED.name());
        alert.setResolvedBy(operator);
        return alertRepository.save(alert);
    }

    @Override
    @Transactional
    public RiskAlert resolveAlert(Long alertId, String operator) {
        RiskAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException("警报不存在: " + alertId));
        alert.setStatus(RiskAlert.AlertStatus.RESOLVED.name());
        alert.setResolvedBy(operator);
        alert.setResolvedAt(LocalDateTime.now());
        return alertRepository.save(alert);
    }
}
