package com.stablecoin.collateral.service.impl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 风险分析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAnalysisResult implements Serializable {

    /** 规则判断的风险等级 */
    private String ruleBasedRisk;

    /** ML 预测的风险等级 */
    private String mlPredictedRisk;

    /** ML 预测的置信度 */
    private Double mlConfidence;

    /** 最终风险等级 */
    private String finalRisk;

    /** 使用的特征 */
    private Map<String, Object> features;

    /** ML 预测来源 */
    private String mlSource;

    /**
     * 判断是否为高风险
     */
    public boolean isHighRisk() {
        return "WARNING".equals(finalRisk) || "CRITICAL".equals(finalRisk);
    }

    /**
     * 判断规则和 ML 是否一致
     */
    public boolean isConsistent() {
        return ruleBasedRisk.equals(mlPredictedRisk);
    }
}
