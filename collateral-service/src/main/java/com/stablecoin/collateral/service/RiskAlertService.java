package com.stablecoin.collateral.service;

import com.stablecoin.collateral.entity.RiskAlert;

import java.math.BigDecimal;
import java.util.List;

public interface RiskAlertService {

    /** 触发储备不足警报 */
    RiskAlert triggerReserveInsufficientAlert(BigDecimal reserveRatio, BigDecimal gapAmount);

    /** 触发大额赎回警报 */
    RiskAlert triggerLargeRedemptionAlert(BigDecimal amount, BigDecimal totalReserve);

    /** 触发低风险资产占比不足警报 */
    RiskAlert triggerLowRiskAssetAlert(BigDecimal lowRiskRatio, BigDecimal gap);

    /** 解决低风险资产占比不足警报 */
    void resolveLowRiskAssetAlert();

    /** 查询所有活跃警报 */
    List<RiskAlert> getActiveAlerts();

    /** 确认警报（人工操作） */
    RiskAlert acknowledgeAlert(Long alertId, String operator);

    /** 解决警报 */
    RiskAlert resolveAlert(Long alertId, String operator);
}
