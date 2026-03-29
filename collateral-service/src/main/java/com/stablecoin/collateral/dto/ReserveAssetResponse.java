package com.stablecoin.collateral.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 储备资产响应 DTO
 * 包含动态计算的实际占比
 */
@Data
@Builder
public class ReserveAssetResponse {

    /** 资产类型 */
    private String assetType;

    /** 资产名称 */
    private String assetName;

    /** 持有价值（USD） */
    private BigDecimal usdValue;

    /** 目标占比（配置值） */
    private BigDecimal targetAllocationRatio;

    /** 实际占比（动态计算：usdValue / 总储备） */
    private BigDecimal actualAllocationRatio;

    /** 风险等级（1=LOW, 2=MEDIUM, 3=HIGH） */
    private Integer riskLevel;

    /** 风险等级名称 */
    private String riskLevelName;

    /** 预期年化收益率 */
    private BigDecimal annualYield;

    /** 预期年化收益金额 */
    private BigDecimal estimatedAnnualIncome;

    /** 描述 */
    private String description;
}
