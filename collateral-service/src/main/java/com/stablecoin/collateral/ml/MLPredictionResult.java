package com.stablecoin.collateral.ml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 机器学习预测结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLPredictionResult implements Serializable {

    /** 预测的风险等级 */
    private String riskLevel;

    /** 预测置信度 (0-1) */
    private Double confidence;

    /** 预测来源 (ml-model / rule-based / fallback) */
    private String source;

    /** 错误信息（如果有） */
    private String error;

    /**
     * 判断预测是否可信
     * 置信度 > 0.8 时认为可信
     */
    public boolean isConfident() {
        return confidence != null && confidence > 0.8;
    }

    /**
     * 判断是否为高风险
     */
    public boolean isHighRisk() {
        return "WARNING".equals(riskLevel) || "CRITICAL".equals(riskLevel);
    }
}
