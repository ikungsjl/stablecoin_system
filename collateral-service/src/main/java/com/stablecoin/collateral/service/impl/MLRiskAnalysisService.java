package com.stablecoin.collateral.service.impl;

import com.stablecoin.collateral.ml.MLPredictionResult;
import com.stablecoin.collateral.ml.MLRiskPredictorClient;
import com.stablecoin.collateral.repository.ReserveAssetRepository;
import com.stablecoin.collateral.repository.ReservePoolRepository;
import com.stablecoin.collateral.repository.RiskAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 机器学习风险分析服务
 * 综合使用规则和 ML 模型进行风险分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MLRiskAnalysisService {

    private final MLRiskPredictorClient mlPredictor;
    private final ReservePoolRepository poolRepository;
    private final ReserveAssetRepository assetRepository;
    private final RiskAlertRepository alertRepository;

    /**
     * 综合分析风险（规则 + ML）
     *
     * @param reserveRatio 储备率
     * @param lowRiskRatio 低风险资产占比
     * @param stablecoinSupply 稳定币流通量
     * @return 分析结果
     */
    public RiskAnalysisResult analyzeRisk(
            BigDecimal reserveRatio,
            BigDecimal lowRiskRatio,
            BigDecimal stablecoinSupply) {

        // 1. 规则判断
        String ruleBasedRisk = evaluateRiskByRules(reserveRatio, lowRiskRatio);

        // 2. 收集特征
        Map<String, Object> features = collectFeatures(
                reserveRatio, lowRiskRatio, stablecoinSupply);

        // 3. ML 预测
        MLPredictionResult mlResult = mlPredictor.predict(features);

        // 4. 综合判断
        String finalRisk = combineResults(ruleBasedRisk, mlResult);

        log.info("[MLRiskAnalysis] 风险分析完成: 规则={} ML={} 最终={}",
                ruleBasedRisk, mlResult.getRiskLevel(), finalRisk);

        return RiskAnalysisResult.builder()
                .ruleBasedRisk(ruleBasedRisk)
                .mlPredictedRisk(mlResult.getRiskLevel())
                .mlConfidence(mlResult.getConfidence())
                .finalRisk(finalRisk)
                .features(features)
                .mlSource(mlResult.getSource())
                .build();
    }

    /**
     * 基于规则的风险评估
     */
    private String evaluateRiskByRules(BigDecimal reserveRatio, BigDecimal lowRiskRatio) {
        if (reserveRatio.compareTo(new BigDecimal("1.10")) >= 0 &&
            lowRiskRatio.compareTo(new BigDecimal("0.80")) >= 0) {
            return "HEALTHY";
        } else if (reserveRatio.compareTo(new BigDecimal("1.00")) >= 0 &&
                   lowRiskRatio.compareTo(new BigDecimal("0.80")) >= 0) {
            return "NORMAL";
        } else if (reserveRatio.compareTo(new BigDecimal("0.90")) >= 0 ||
                   lowRiskRatio.compareTo(new BigDecimal("0.75")) >= 0) {
            return "WARNING";
        } else {
            return "CRITICAL";
        }
    }

    /**
     * 收集特征
     */
    private Map<String, Object> collectFeatures(
            BigDecimal reserveRatio,
            BigDecimal lowRiskRatio,
            BigDecimal stablecoinSupply) {

        Map<String, Object> features = new HashMap<>();

        // 基础特征
        features.put("reserve_ratio", reserveRatio.doubleValue());
        features.put("low_risk_ratio", lowRiskRatio.doubleValue());
        features.put("stablecoin_supply", stablecoinSupply.doubleValue());

        // 储备特征
        BigDecimal totalReserve = poolRepository.findById(1L)
                .map(pool -> pool.getTotalUsdAmount())
                .orElse(BigDecimal.ZERO);
        features.put("total_reserve", totalReserve.doubleValue());

        // 波动率特征（简化计算）
        BigDecimal volatility = calculateVolatility(reserveRatio);
        features.put("volatility", volatility.doubleValue());

        // 趋势特征（简化计算）
        BigDecimal trend = calculateTrend(reserveRatio);
        features.put("trend", trend.doubleValue());

        // 告警特征
        long alertCount = alertRepository.findByStatusOrderByCreatedAtDesc("ACTIVE").size();
        features.put("alert_count", (double) alertCount);

        return features;
    }

    /**
     * 计算波动率（简化版）
     * 实际应该基于历史数据计算
     */
    private BigDecimal calculateVolatility(BigDecimal reserveRatio) {
        // 简化：根据储备率偏离 1.0 的程度计算波动率
        BigDecimal deviation = reserveRatio.subtract(BigDecimal.ONE).abs();
        return deviation.min(new BigDecimal("0.5"));
    }

    /**
     * 计算趋势（简化版）
     * 实际应该基于历史数据计算
     */
    private BigDecimal calculateTrend(BigDecimal reserveRatio) {
        // 简化：返回 0
        return BigDecimal.ZERO;
    }

    /**
     * 综合规则和 ML 结果
     *
     * 策略：
     * - 如果 ML 置信度 > 0.8，优先使用 ML 预测
     * - 否则使用规则判断
     * - 如果两者都预测为高风险，则取更高的风险等级
     */
    private String combineResults(String ruleRisk, MLPredictionResult mlResult) {
        Map<String, Integer> riskLevels = Map.of(
                "HEALTHY", 0,
                "NORMAL", 1,
                "WARNING", 2,
                "CRITICAL", 3
        );

        // 如果 ML 置信度高，优先使用 ML 预测
        if (mlResult.isConfident()) {
            log.debug("[MLRiskAnalysis] ML 置信度高 ({}), 使用 ML 预测", mlResult.getConfidence());
            return mlResult.getRiskLevel();
        }

        // 否则取更高的风险等级
        int ruleLevel = riskLevels.getOrDefault(ruleRisk, 1);
        int mlLevel = riskLevels.getOrDefault(mlResult.getRiskLevel(), 1);

        if (mlLevel > ruleLevel) {
            log.debug("[MLRiskAnalysis] ML 预测风险更高，使用 ML 结果");
            return mlResult.getRiskLevel();
        } else {
            log.debug("[MLRiskAnalysis] 规则预测风险更高或相同，使用规则结果");
            return ruleRisk;
        }
    }
}
