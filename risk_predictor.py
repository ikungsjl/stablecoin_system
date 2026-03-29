"""
稳定币风险预测模块
使用 Scikit-learn 实现简单的机器学习算法预测储备风险
"""

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score
import joblib
import logging
from datetime import datetime, timedelta
from typing import Dict, List, Tuple

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class RiskPredictor:
    """
    风险预测器
    使用随机森林算法预测储备风险等级
    """

    def __init__(self, model_path: str = None):
        """
        初始化风险预测器
        
        Args:
            model_path: 预训练模型路径，如果为 None 则创建新模型
        """
        self.model = None
        self.scaler = StandardScaler()
        self.feature_names = [
            'reserve_ratio',           # 储备率
            'low_risk_ratio',          # 低风险资产占比
            'stablecoin_supply',       # 稳定币流通量
            'total_reserve',           # 总储备
            'volatility',              # 波动率
            'trend',                   # 趋势
            'alert_count'              # 告警数
        ]
        
        if model_path:
            self.load_model(model_path)
        else:
            self._initialize_model()

    def _initialize_model(self):
        """初始化随机森林模型"""
        self.model = RandomForestClassifier(
            n_estimators=100,
            max_depth=10,
            min_samples_split=5,
            min_samples_leaf=2,
            random_state=42,
            n_jobs=-1
        )
        logger.info("[RiskPredictor] 随机森林模型已初始化")

    def train(self, historical_data: pd.DataFrame, labels: np.ndarray):
        """
        训练模型
        
        Args:
            historical_data: 历史数据 DataFrame，包含特征列
            labels: 标签数组 (0=HEALTHY, 1=NORMAL, 2=WARNING, 3=CRITICAL)
        """
        # 特征工程
        X = historical_data[self.feature_names].values
        
        # 数据标准化
        X_scaled = self.scaler.fit_transform(X)
        
        # 分割训练集和测试集
        X_train, X_test, y_train, y_test = train_test_split(
            X_scaled, labels, test_size=0.2, random_state=42
        )
        
        # 训练模型
        self.model.fit(X_train, y_train)
        
        # 评估模型
        y_pred = self.model.predict(X_test)
        accuracy = accuracy_score(y_test, y_pred)
        precision = precision_score(y_test, y_pred, average='weighted', zero_division=0)
        recall = recall_score(y_test, y_pred, average='weighted', zero_division=0)
        f1 = f1_score(y_test, y_pred, average='weighted', zero_division=0)
        
        logger.info(f"[RiskPredictor] 模型训练完成")
        logger.info(f"  准确率: {accuracy:.4f}")
        logger.info(f"  精确率: {precision:.4f}")
        logger.info(f"  召回率: {recall:.4f}")
        logger.info(f"  F1 分数: {f1:.4f}")
        
        return {
            'accuracy': accuracy,
            'precision': precision,
            'recall': recall,
            'f1': f1
        }

    def predict(self, features: Dict[str, float]) -> Tuple[str, float]:
        """
        预测风险等级
        
        Args:
            features: 特征字典，包含所有必要的特征
            
        Returns:
            (风险等级, 置信度)
        """
        if self.model is None:
            raise ValueError("模型未训练，请先调用 train() 方法")
        
        # 构建特征向量
        X = np.array([[
            features.get('reserve_ratio', 1.0),
            features.get('low_risk_ratio', 0.8),
            features.get('stablecoin_supply', 0),
            features.get('total_reserve', 0),
            features.get('volatility', 0),
            features.get('trend', 0),
            features.get('alert_count', 0)
        ]])
        
        # 标准化
        X_scaled = self.scaler.transform(X)
        
        # 预测
        prediction = self.model.predict(X_scaled)[0]
        probabilities = self.model.predict_proba(X_scaled)[0]
        confidence = np.max(probabilities)
        
        # 映射到风险等级
        risk_levels = ['HEALTHY', 'NORMAL', 'WARNING', 'CRITICAL']
        risk_level = risk_levels[prediction]
        
        logger.info(f"[RiskPredictor] 风险预测: {risk_level} (置信度: {confidence:.4f})")
        
        return risk_level, confidence

    def predict_batch(self, features_list: List[Dict[str, float]]) -> List[Tuple[str, float]]:
        """
        批量预测
        
        Args:
            features_list: 特征字典列表
            
        Returns:
            [(风险等级, 置信度), ...] 列表
        """
        results = []
        for features in features_list:
            risk_level, confidence = self.predict(features)
            results.append((risk_level, confidence))
        return results

    def get_feature_importance(self) -> Dict[str, float]:
        """获取特征重要性"""
        if self.model is None:
            raise ValueError("模型未训练")
        
        importance = {}
        for name, imp in zip(self.feature_names, self.model.feature_importances_):
            importance[name] = float(imp)
        
        # 按重要性排序
        return dict(sorted(importance.items(), key=lambda x: x[1], reverse=True))

    def save_model(self, model_path: str):
        """保存模型"""
        joblib.dump(self.model, model_path)
        joblib.dump(self.scaler, model_path.replace('.pkl', '_scaler.pkl'))
        logger.info(f"[RiskPredictor] 模型已保存到 {model_path}")

    def load_model(self, model_path: str):
        """加载模型"""
        self.model = joblib.load(model_path)
        self.scaler = joblib.load(model_path.replace('.pkl', '_scaler.pkl'))
        logger.info(f"[RiskPredictor] 模型已从 {model_path} 加载")


class HistoricalDataGenerator:
    """
    历史数据生成器
    用于生成训练数据
    """

    @staticmethod
    def generate_synthetic_data(num_samples: int = 1000) -> Tuple[pd.DataFrame, np.ndarray]:
        """
        生成合成历史数据用于训练
        
        Args:
            num_samples: 样本数量
            
        Returns:
            (特征 DataFrame, 标签数组)
        """
        np.random.seed(42)
        
        # 生成特征
        reserve_ratio = np.random.uniform(0.7, 2.0, num_samples)
        low_risk_ratio = np.random.uniform(0.6, 1.0, num_samples)
        stablecoin_supply = np.random.uniform(0, 10000000, num_samples)
        total_reserve = np.random.uniform(0, 15000000, num_samples)
        volatility = np.random.uniform(0, 0.5, num_samples)
        trend = np.random.uniform(-0.1, 0.1, num_samples)
        alert_count = np.random.randint(0, 10, num_samples)
        
        # 根据特征生成标签
        labels = np.zeros(num_samples, dtype=int)
        
        for i in range(num_samples):
            # 规则：根据储备率和低风险资产占比判断风险等级
            if reserve_ratio[i] >= 1.1 and low_risk_ratio[i] >= 0.8:
                labels[i] = 0  # HEALTHY
            elif reserve_ratio[i] >= 1.0 and low_risk_ratio[i] >= 0.8:
                labels[i] = 1  # NORMAL
            elif reserve_ratio[i] >= 0.9 or low_risk_ratio[i] >= 0.75:
                labels[i] = 2  # WARNING
            else:
                labels[i] = 3  # CRITICAL
            
            # 加入随机噪声
            if np.random.random() < 0.1:
                labels[i] = np.random.randint(0, 4)
        
        # 创建 DataFrame
        data = pd.DataFrame({
            'reserve_ratio': reserve_ratio,
            'low_risk_ratio': low_risk_ratio,
            'stablecoin_supply': stablecoin_supply,
            'total_reserve': total_reserve,
            'volatility': volatility,
            'trend': trend,
            'alert_count': alert_count
        })
        
        logger.info(f"[DataGenerator] 生成了 {num_samples} 个合成样本")
        
        return data, labels

    @staticmethod
    def load_from_database(db_connection, days: int = 30) -> Tuple[pd.DataFrame, np.ndarray]:
        """
        从数据库加载历史数据
        
        Args:
            db_connection: 数据库连接
            days: 加载过去多少天的数据
            
        Returns:
            (特征 DataFrame, 标签数组)
        """
        # 这是一个示例实现，实际使用时需要连接真实数据库
        logger.info(f"[DataGenerator] 从数据库加载过去 {days} 天的数据")
        
        # 示例：从 reserve_snapshots 表加载数据
        query = f"""
        SELECT 
            reserve_ratio,
            risk_level,
            snapshot_at
        FROM reserve_snapshots
        WHERE snapshot_at >= NOW() - INTERVAL '{days} days'
        ORDER BY snapshot_at DESC
        """
        
        # 实际实现需要执行查询并处理结果
        # df = pd.read_sql(query, db_connection)
        
        return None, None


class RiskAnalyzer:
    """
    风险分析器
    综合使用规则和机器学习进行风险分析
    """

    def __init__(self, predictor: RiskPredictor):
        """
        初始化风险分析器
        
        Args:
            predictor: 风险预测器实例
        """
        self.predictor = predictor

    def analyze(self, features: Dict[str, float]) -> Dict:
        """
        综合分析风险
        
        Args:
            features: 特征字典
            
        Returns:
            分析结果字典
        """
        # 规则判断
        rule_based_risk = self._rule_based_analysis(features)
        
        # 机器学习预测
        ml_risk, ml_confidence = self.predictor.predict(features)
        
        # 综合判断
        final_risk = self._combine_results(rule_based_risk, ml_risk, ml_confidence)
        
        return {
            'rule_based_risk': rule_based_risk,
            'ml_predicted_risk': ml_risk,
            'ml_confidence': ml_confidence,
            'final_risk': final_risk,
            'features': features,
            'timestamp': datetime.now().isoformat()
        }

    def _rule_based_analysis(self, features: Dict[str, float]) -> str:
        """基于规则的风险分析"""
        reserve_ratio = features.get('reserve_ratio', 1.0)
        low_risk_ratio = features.get('low_risk_ratio', 0.8)
        
        if reserve_ratio >= 1.1 and low_risk_ratio >= 0.8:
            return 'HEALTHY'
        elif reserve_ratio >= 1.0 and low_risk_ratio >= 0.8:
            return 'NORMAL'
        elif reserve_ratio >= 0.9 or low_risk_ratio >= 0.75:
            return 'WARNING'
        else:
            return 'CRITICAL'

    def _combine_results(self, rule_risk: str, ml_risk: str, ml_confidence: float) -> str:
        """
        综合规则和机器学习结果
        
        策略：
        - 如果 ML 置信度 > 0.8，优先使用 ML 预测
        - 否则使用规则判断
        - 如果两者都预测为高风险，则取更高的风险等级
        """
        risk_levels = {'HEALTHY': 0, 'NORMAL': 1, 'WARNING': 2, 'CRITICAL': 3}
        
        if ml_confidence > 0.8:
            return ml_risk
        
        rule_level = risk_levels[rule_risk]
        ml_level = risk_levels[ml_risk]
        
        # 取更高的风险等级
        if ml_level > rule_level:
            return ml_risk
        else:
            return rule_risk


# 使用示例
if __name__ == '__main__':
    # 1. 生成训练数据
    logger.info("=" * 50)
    logger.info("步骤 1: 生成训练数据")
    logger.info("=" * 50)
    
    data_gen = HistoricalDataGenerator()
    X_train, y_train = data_gen.generate_synthetic_data(num_samples=1000)
    
    # 2. 创建并训练模型
    logger.info("\n" + "=" * 50)
    logger.info("步骤 2: 训练模型")
    logger.info("=" * 50)
    
    predictor = RiskPredictor()
    metrics = predictor.train(X_train, y_train)
    
    # 3. 查看特征重要性
    logger.info("\n" + "=" * 50)
    logger.info("步骤 3: 特征重要性")
    logger.info("=" * 50)
    
    importance = predictor.get_feature_importance()
    for feature, imp in importance.items():
        logger.info(f"  {feature}: {imp:.4f}")
    
    # 4. 进行预测
    logger.info("\n" + "=" * 50)
    logger.info("步骤 4: 风险预测")
    logger.info("=" * 50)
    
    test_features = {
        'reserve_ratio': 1.5,
        'low_risk_ratio': 0.85,
        'stablecoin_supply': 5000000,
        'total_reserve': 7500000,
        'volatility': 0.05,
        'trend': 0.02,
        'alert_count': 0
    }
    
    risk_level, confidence = predictor.predict(test_features)
    logger.info(f"预测风险等级: {risk_level}")
    logger.info(f"置信度: {confidence:.4f}")
    
    # 5. 综合分析
    logger.info("\n" + "=" * 50)
    logger.info("步骤 5: 综合风险分析")
    logger.info("=" * 50)
    
    analyzer = RiskAnalyzer(predictor)
    analysis_result = analyzer.analyze(test_features)
    
    logger.info(f"规则判断: {analysis_result['rule_based_risk']}")
    logger.info(f"ML 预测: {analysis_result['ml_predicted_risk']} (置信度: {analysis_result['ml_confidence']:.4f})")
    logger.info(f"最终风险等级: {analysis_result['final_risk']}")
    
    # 6. 保存模型
    logger.info("\n" + "=" * 50)
    logger.info("步骤 6: 保存模型")
    logger.info("=" * 50)
    
    predictor.save_model('risk_predictor_model.pkl')
