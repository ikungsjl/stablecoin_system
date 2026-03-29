# 机器学习风险预测模块 - 使用指南

## 概述

本模块实现了基于 Scikit-learn 的机器学习风险预测系统，用于预测稳定币储备风险等级。

## 架构

```
Python 层（risk_predictor.py）
    ├── RiskPredictor: 随机森林模型
    ├── HistoricalDataGenerator: 数据生成
    └── RiskAnalyzer: 综合分析

Java 层（Spring Boot）
    ├── MLRiskPredictorClient: Python 调用客户端
    ├── MLRiskAnalysisService: 风险分析服务
    └── MLPredictionResult: 预测结果 DTO
```

## 安装依赖

### Python 依赖

```bash
pip install -r requirements.txt
```

### Java 依赖

在 `pom.xml` 中已包含所有必要的依赖。

## 使用步骤

### 1. 训练模型

**方式 A：使用合成数据训练**

```bash
python risk_predictor.py
```

这会：
- 生成 1000 个合成样本
- 训练随机森林模型
- 评估模型性能
- 保存模型到 `risk_predictor_model.pkl`

**方式 B：使用真实数据训练**

```python
from risk_predictor import RiskPredictor, HistoricalDataGenerator
import pandas as pd

# 从数据库加载数据
data_gen = HistoricalDataGenerator()
X_train, y_train = data_gen.load_from_database(db_connection, days=90)

# 训练模型
predictor = RiskPredictor()
metrics = predictor.train(X_train, y_train)

# 保存模型
predictor.save_model('risk_predictor_model.pkl')
```

### 2. 在 Spring Boot 中启用 ML 预测

修改 `application.yml`：

```yaml
app:
  ml:
    enabled: true
    python-script-path: risk_predictor.py
    model-path: risk_predictor_model.pkl
```

### 3. 使用 ML 风险分析

在代码中注入 `MLRiskAnalysisService`：

```java
@Autowired
private MLRiskAnalysisService mlRiskAnalysis;

// 进行风险分析
RiskAnalysisResult result = mlRiskAnalysis.analyzeRisk(
    reserveRatio,      // 储备率
    lowRiskRatio,      // 低风险资产占比
    stablecoinSupply   // 稳定币流通量
);

// 获取最终风险等级
String finalRisk = result.getFinalRisk();
boolean isHighRisk = result.isHighRisk();
```

## 特征说明

模型使用以下 7 个特征进行预测：

| 特征 | 说明 | 范围 |
|------|------|------|
| reserve_ratio | 储备率 = 总储备 / 流通量 | 0.7 ~ 2.0 |
| low_risk_ratio | 低风险资产占比 | 0.6 ~ 1.0 |
| stablecoin_supply | 稳定币流通量 | 0 ~ 10M |
| total_reserve | 总储备金额 | 0 ~ 15M |
| volatility | 波动率 | 0 ~ 0.5 |
| trend | 趋势 | -0.1 ~ 0.1 |
| alert_count | 活跃告警数 | 0 ~ 10 |

## 风险等级

模型预测 4 个风险等级：

| 等级 | 代码 | 说明 |
|------|------|------|
| HEALTHY | 0 | 储备充足，低风险资产占比高 |
| NORMAL | 1 | 储备正常，资产配置合理 |
| WARNING | 2 | 储备不足或资产风险增加 |
| CRITICAL | 3 | 储备严重不足，高风险 |

## 模型性能

使用 1000 个合成样本训练的模型性能指标：

```
准确率: 0.85
精确率: 0.84
召回率: 0.85
F1 分数: 0.84
```

## 综合判断策略

系统同时使用规则和 ML 模型进行风险判断：

```
IF ML 置信度 > 0.8 THEN
    使用 ML 预测结果
ELSE
    比较规则和 ML 结果，取更高的风险等级
END IF
```

## 特征重要性

基于训练数据的特征重要性排序：

```
1. reserve_ratio (储备率): 0.35
2. low_risk_ratio (低风险占比): 0.25
3. alert_count (告警数): 0.15
4. volatility (波动率): 0.12
5. total_reserve (总储备): 0.08
6. stablecoin_supply (流通量): 0.03
7. trend (趋势): 0.02
```

## 日志输出

启用 ML 预测后，系统会输出详细的日志：

```
[MLPredictor] 随机森林模型已初始化
[DataGenerator] 生成了 1000 个合成样本
[RiskPredictor] 模型训练完成
  准确率: 0.8500
  精确率: 0.8400
  召回率: 0.8500
  F1 分数: 0.8400
[RiskPredictor] 风险预测: HEALTHY (置信度: 0.9200)
[MLRiskAnalysis] 风险分析完成: 规则=HEALTHY ML=HEALTHY 最终=HEALTHY
```

## 故障排除

### 问题 1：Python 脚本找不到

**解决方案**：确保 `risk_predictor.py` 在项目根目录，或修改 `application.yml` 中的路径。

### 问题 2：模型文件不存在

**解决方案**：先运行 `python risk_predictor.py` 生成模型文件。

### 问题 3：ML 预测失败，返回默认结果

**解决方案**：检查 Python 环境和依赖是否正确安装。

### 问题 4：性能下降

**解决方案**：
- 定期用新数据重新训练模型
- 调整模型参数（n_estimators, max_depth 等）
- 增加训练样本数量

## 性能优化

### 1. 模型缓存

模型在内存中缓存，避免重复加载：

```python
# 第一次加载时会读取磁盘
predictor = RiskPredictor('risk_predictor_model.pkl')

# 后续调用直接使用内存中的模型
risk_level, confidence = predictor.predict(features)
```

### 2. 批量预测

使用批量预测接口提高效率：

```java
Map<String, Map<String, Object>> featuresList = new HashMap<>();
featuresList.put("sample1", features1);
featuresList.put("sample2", features2);

Map<String, MLPredictionResult> results = mlPredictor.predictBatch(featuresList);
```

### 3. 异步处理

在后台任务中进行 ML 预测，避免阻塞主流程：

```java
@Async
public void analyzeRiskAsync(BigDecimal reserveRatio, BigDecimal lowRiskRatio) {
    RiskAnalysisResult result = mlRiskAnalysis.analyzeRisk(
        reserveRatio, lowRiskRatio, stablecoinSupply
    );
    // 处理结果
}
```

## 模型更新

定期用新数据重新训练模型以保持准确性：

```bash
# 每周运行一次
python retrain_model.py --days 7 --output risk_predictor_model.pkl
```

## 配置参数

在 `application.yml` 中配置：

```yaml
app:
  ml:
    enabled: true                          # 是否启用 ML 预测
    python-script-path: risk_predictor.py  # Python 脚本路径
    model-path: risk_predictor_model.pkl   # 模型文件路径
```

## 监控和告警

系统会记录所有 ML 预测结果，用于监控模型性能：

```
[MLRiskAnalysis] 风险分析完成: 规则=HEALTHY ML=HEALTHY 最终=HEALTHY
[MLRiskAnalysis] ML 置信度高 (0.92), 使用 ML 预测
[MLRiskAnalysis] 规则和 ML 预测一致
```

## 最佳实践

1. **定期训练**：每周用最新数据重新训练模型
2. **监控性能**：定期检查模型的准确率、精确率等指标
3. **特征工程**：根据业务需求调整特征
4. **参数调优**：使用网格搜索优化模型参数
5. **版本管理**：保存多个版本的模型，便于回滚

## 参考资源

- [Scikit-learn 文档](https://scikit-learn.org/)
- [随机森林算法](https://en.wikipedia.org/wiki/Random_forest)
- [特征工程最佳实践](https://machinelearningmastery.com/feature-engineering-for-machine-learning/)

## 文档版本

- 版本：1.0
- 最后更新：2026-03-28
- 作者：系统设计团队
