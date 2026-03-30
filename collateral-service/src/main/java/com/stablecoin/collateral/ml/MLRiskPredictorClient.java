package com.stablecoin.collateral.ml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 机器学习风险预测客户端
 * 通过 Python 脚本调用 Scikit-learn 模型进行风险预测
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MLRiskPredictorClient {

    private final ObjectMapper objectMapper;

    @Value("${app.ml.python-script-path:risk_predictor.py}")
    private String pythonScriptPath;

    @Value("${app.ml.model-path:risk_predictor_model.pkl}")
    private String modelPath;

    @Value("${app.ml.enabled:false}")
    private boolean mlEnabled;

    /**
     * 预测风险等级
     *
     * @param features 特征字典
     * @return 预测结果
     */
    public MLPredictionResult predict(Map<String, Object> features) {
        if (!mlEnabled) {
            log.debug("[MLPredictor] ML 预测已禁用，返回默认结果");
            return MLPredictionResult.builder()
                    .riskLevel("NORMAL")
                    .confidence(0.5)
                    .source("rule-based")
                    .build();
        }

        try {
            String pythonCode = buildPythonCode(features);
            String result = executePython(pythonCode);
            MLPredictionResult prediction = objectMapper.readValue(result, MLPredictionResult.class);
            log.info("[MLPredictor] 风险预测完成: {} (置信度: {})",
                    prediction.getRiskLevel(), prediction.getConfidence());
            return prediction;
        } catch (Exception e) {
            log.warn("[MLPredictor] ML 预测失败，返回默认结果: {}", e.getMessage());
            return MLPredictionResult.builder()
                    .riskLevel("NORMAL")
                    .confidence(0.5)
                    .source("fallback")
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * 构建 Python 代码（声明抛出受检异常，由调用方的 catch(Exception) 统一处理）
     */
    private String buildPythonCode(Map<String, Object> features) throws JsonProcessingException {
        return String.format("""
                import sys
                import json
                sys.path.insert(0, '.')
                from risk_predictor import RiskPredictor
                
                # 加载模型
                predictor = RiskPredictor('%s')
                
                # 特征数据
                features = %s
                
                # 预测
                risk_level, confidence = predictor.predict(features)
                
                # 输出结果
                result = {
                    'riskLevel': risk_level,
                    'confidence': float(confidence),
                    'source': 'ml-model'
                }
                print(json.dumps(result))
                """, modelPath, objectMapper.writeValueAsString(features));
    }

    /**
     * 执行 Python 代码
     */
    private String executePython(String pythonCode) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("python", "-c", pythonCode);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python 脚本执行失败，退出码: " + exitCode);
        }

        return output.toString();
    }

    /**
     * 批量预测
     */
    public Map<String, MLPredictionResult> predictBatch(
            Map<String, Map<String, Object>> featuresList) {
        Map<String, MLPredictionResult> results = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : featuresList.entrySet()) {
            results.put(entry.getKey(), predict(entry.getValue()));
        }
        return results;
    }
}
