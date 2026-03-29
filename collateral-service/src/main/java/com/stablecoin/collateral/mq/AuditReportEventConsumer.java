package com.stablecoin.collateral.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.stablecoin.collateral.service.AuditReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审计报告事件消费者
 * 监听风险等级变化事件，异步生成审计报告
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditReportEventConsumer {

    private final AuditReportService auditReportService;
    private final ObjectMapper objectMapper;

    /**
     * 监听风险等级变化事件
     * 当风险等级发生变化时，异步生成审计报告
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
            LocalDateTime eventTime = LocalDateTime.parse((String) event.get("eventTime"));

            log.info("[AuditConsumer] 收到风险等级变化事件: {} -> {}", fromLevel, toLevel);

            // 生成过去1小时的审计报告
            LocalDateTime reportStart = eventTime.minusHours(1);
            LocalDateTime reportEnd = eventTime;

            auditReportService.generateReport(
                    reportStart,
                    reportEnd,
                    "system-mq-risk-change-" + fromLevel + "-to-" + toLevel
            );

            log.info("[AuditConsumer] 审计报告生成完成");
            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("[AuditConsumer] 处理风险等级变化事件失败: {}", e.getMessage());
            // 重新入队重试
            channel.basicNack(tag, false, true);
        }
    }
}
