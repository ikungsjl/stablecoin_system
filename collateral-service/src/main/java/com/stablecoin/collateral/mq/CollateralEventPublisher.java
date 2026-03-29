package com.stablecoin.collateral.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 抵押物服务事件发布器
 * 发布内部事件：储备风险告警、储备恢复、风险等级变化、低风险资产告警
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollateralEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${mq.exchange.collateral-events:collateral.events}")
    private String collateralExchange;

    @Value("${mq.routing-key.reserve-risk-alert:reserve.risk.alert}")
    private String riskAlertRoutingKey;

    @Value("${mq.routing-key.reserve-healthy:reserve.healthy}")
    private String reserveHealthyRoutingKey;

    @Value("${mq.routing-key.risk-level-change:risk.level.change}")
    private String riskLevelChangeRoutingKey;

    /** 发布储备风险警报事件 */
    public void publishRiskAlert(String riskLevel, BigDecimal reserveRatio, BigDecimal gapAmount) {
        Map<String, Object> message = new HashMap<>();
        message.put("eventType", "RESERVE_RISK_ALERT");
        message.put("riskLevel", riskLevel);
        message.put("reserveRatio", reserveRatio);
        message.put("gapAmount", gapAmount);
        message.put("timestamp", System.currentTimeMillis());

        rabbitTemplate.convertAndSend(collateralExchange, riskAlertRoutingKey, message);
        log.info("[MQ] 发布风险警报: level={} ratio={} gap={}", riskLevel, reserveRatio, gapAmount);
    }

    /** 发布储备恢复健康事件 */
    public void publishReserveHealthy(BigDecimal reserveRatio) {
        Map<String, Object> message = new HashMap<>();
        message.put("eventType", "RESERVE_HEALTHY");
        message.put("reserveRatio", reserveRatio);
        message.put("timestamp", System.currentTimeMillis());

        rabbitTemplate.convertAndSend(collateralExchange, reserveHealthyRoutingKey, message);
        log.info("[MQ] 发布储备健康事件: ratio={}", reserveRatio);
    }

    /**
     * 发布风险等级变化事件
     * 触发审计报告异步生成
     */
    public void publishRiskLevelChange(String fromLevel, String toLevel) {
        Map<String, Object> message = new HashMap<>();
        message.put("eventType", "RISK_LEVEL_CHANGE");
        message.put("fromLevel", fromLevel);
        message.put("toLevel", toLevel);
        message.put("eventTime", LocalDateTime.now().toString());
        message.put("timestamp", System.currentTimeMillis());

        rabbitTemplate.convertAndSend(collateralExchange, riskLevelChangeRoutingKey, message);
        log.info("[MQ] 发布风险等级变化事件: {} -> {}", fromLevel, toLevel);
    }

    /** 发布低风险资产占比不足告警事件 */
    public void publishLowRiskAssetAlert(BigDecimal lowRiskRatio, BigDecimal gap) {
        Map<String, Object> message = new HashMap<>();
        message.put("eventType", "LOW_RISK_ASSET_ALERT");
        message.put("lowRiskRatio", lowRiskRatio);
        message.put("gap", gap);
        message.put("timestamp", System.currentTimeMillis());

        rabbitTemplate.convertAndSend(collateralExchange, riskAlertRoutingKey, message);
        log.warn("[MQ] 发布低风险资产占比不足告警: ratio={} gap={}", lowRiskRatio, gap);
    }

    /** 发布低风险资产占比已恢复事件 */
    public void publishLowRiskAssetRecovered(BigDecimal lowRiskRatio) {
        Map<String, Object> message = new HashMap<>();
        message.put("eventType", "LOW_RISK_ASSET_RECOVERED");
        message.put("lowRiskRatio", lowRiskRatio);
        message.put("timestamp", System.currentTimeMillis());

        rabbitTemplate.convertAndSend(collateralExchange, reserveHealthyRoutingKey, message);
        log.info("[MQ] 发布低风险资产占比已恢复事件: ratio={}", lowRiskRatio);
    }
}
