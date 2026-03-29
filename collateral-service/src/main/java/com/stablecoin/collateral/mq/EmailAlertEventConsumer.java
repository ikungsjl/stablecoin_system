package com.stablecoin.collateral.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.stablecoin.collateral.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 邮件告警消费者
 * 监听风险等级变化事件，当风险等级变为 CRITICAL 时发送告警邮件
 */
@Slf4j
@Component
public class EmailAlertEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Value("${app.email.admin-recipients:admin@stablecoin-system.com}")
    private String adminRecipients;

    @Autowired
    public EmailAlertEventConsumer(@Lazy EmailService emailService, ObjectMapper objectMapper) {
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    /**
     * 监听风险等级变化事件
     * 当风险等级变为 CRITICAL 时发送告警邮件
     */
    @RabbitListener(
        queues = "${mq.queue.risk-level-change:queue.risk.level.change}",
        ackMode = "MANUAL"
    )
    public void onRiskLevelChange(Message message, Channel channel) throws Exception {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            Map<String, Object> event = objectMapper.readValue(
                    message.getBody(), Map.class);

            String fromLevel = (String) event.get("fromLevel");
            String toLevel = (String) event.get("toLevel");

            log.info("[EmailAlert] 收到风险等级变化事件: {} -> {}", fromLevel, toLevel);

            // 只在风险等级变为 CRITICAL 时发送告警邮件
            if ("CRITICAL".equals(toLevel)) {
                sendCriticalRiskAlert(fromLevel, toLevel, event);
            }
            // 从 WARNING/CRITICAL 恢复到 HEALTHY 时发送恢复通知
            else if ("HEALTHY".equals(toLevel) && 
                     ("WARNING".equals(fromLevel) || "CRITICAL".equals(fromLevel))) {
                sendRiskRecoveryNotification(event);
            }

            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("[EmailAlert] 处理风险等级变化事件失败: {}", e.getMessage());
            // 重新入队重试
            channel.basicNack(tag, false, true);
        }
    }

    /**
     * 发送 CRITICAL 风险告警邮件
     */
    private void sendCriticalRiskAlert(String fromLevel, String toLevel, Map<String, Object> event) {
        try {
            List<String> recipients = parseRecipients(adminRecipients);
            
            // 从事件中提取数据（如果有的话）
            BigDecimal reserveRatio = extractBigDecimal(event.get("reserveRatio"));
            BigDecimal gapAmount = extractBigDecimal(event.get("gapAmount"));

            log.warn("[EmailAlert] 发送 CRITICAL 风险告警邮件 recipients={}", recipients);
            emailService.sendRiskAlertEmail(recipients, "CRITICAL", reserveRatio, gapAmount);

        } catch (Exception e) {
            log.error("[EmailAlert] 发送 CRITICAL 告警邮件失败: {}", e.getMessage());
        }
    }

    /**
     * 发送风险恢复通知邮件
     */
    private void sendRiskRecoveryNotification(Map<String, Object> event) {
        try {
            List<String> recipients = parseRecipients(adminRecipients);
            BigDecimal reserveRatio = extractBigDecimal(event.get("reserveRatio"));

            log.info("[EmailAlert] 发送风险恢复通知邮件 recipients={}", recipients);
            emailService.sendRiskRecoveryEmail(recipients, reserveRatio);

        } catch (Exception e) {
            log.error("[EmailAlert] 发送恢复通知邮件失败: {}", e.getMessage());
        }
    }

    private List<String> parseRecipients(String recipientsStr) {
        return Arrays.asList(recipientsStr.split(","));
    }

    private BigDecimal extractBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return new BigDecimal(value.toString());
        return BigDecimal.ZERO;
    }
}
