package com.stablecoin.collateral.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 仪表盘数据 DTO */
@Data
@Builder
public class DashboardResponse {

    /** 当前总储备（USD） */
    private BigDecimal totalReserve;

    /** 当前锁定中金额 */
    private BigDecimal lockedAmount;

    /** 当前可用储备 */
    private BigDecimal availableReserve;

    /** 当前稳定币流通量 */
    private BigDecimal stablecoinSupply;

    /** 当前储备率 */
    private BigDecimal reserveRatio;

    /** 当前风险等级：HEALTHY / NORMAL / WARNING / CRITICAL */
    private String riskLevel;

    /** 活跃警报数量 */
    private long activeAlertCount;

    /** 数据更新时间 */
    private LocalDateTime updatedAt;

    // ---- 储备资产配置摘要（对照泰达币资产构成） ----

    /** 资产配置总价值（USD），应与 totalReserve 接近 */
    private BigDecimal assetTotalValue;

    /** 低风险资产占比（国债+逆回购+货币基金+现金） */
    private BigDecimal lowRiskRatio;

    /** 中风险资产占比（黄金+其他） */
    private BigDecimal mediumRiskRatio;

    /** 高风险资产占比（比特币） */
    private BigDecimal highRiskRatio;

    /** 预期年化利息收入（USD），主要来自国债和逆回购 */
    private BigDecimal estimatedAnnualIncome;
}
