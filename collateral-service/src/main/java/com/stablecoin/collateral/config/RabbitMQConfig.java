package com.stablecoin.collateral.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 — 仅用于课题2内部模块间通信
 * 主要用途：
 * 1. 储备风险告警事件广播
 * 2. 风险等级变化事件 → 触发审计报告异步生成
 * 课题间通信通过 REST API 实现，不使用 MQ
 */
@Configuration
public class RabbitMQConfig {

    @Value("${mq.exchange.collateral-events:collateral.events}")
    private String collateralExchange;

    @Value("${mq.queue.reserve-risk-alert:queue.reserve.risk.alert}")
    private String riskAlertQueue;

    @Value("${mq.queue.risk-level-change:queue.risk.level.change}")
    private String riskLevelChangeQueue;

    @Value("${mq.routing-key.reserve-risk-alert:reserve.risk.alert}")
    private String riskAlertRoutingKey;

    @Value("${mq.routing-key.reserve-healthy:reserve.healthy}")
    private String reserveHealthyRoutingKey;

    @Value("${mq.routing-key.risk-level-change:risk.level.change}")
    private String riskLevelChangeRoutingKey;

    // ---- Exchange ----

    /** 课题2内部事件 Topic Exchange */
    @Bean
    public TopicExchange collateralEventExchange() {
        return new TopicExchange(collateralExchange, true, false);
    }

    // ---- Queues ----

    /** 储备风险告警队列（供内部监控模块订阅） */
    @Bean
    public Queue riskAlertQueue() {
        return QueueBuilder.durable(riskAlertQueue).build();
    }

    /** 风险等级变化队列（触发审计报告生成） */
    @Bean
    public Queue riskLevelChangeQueue() {
        return QueueBuilder.durable(riskLevelChangeQueue).build();
    }

    // ---- Bindings ----

    @Bean
    public Binding riskAlertBinding() {
        return BindingBuilder.bind(riskAlertQueue())
                .to(collateralEventExchange())
                .with(riskAlertRoutingKey);
    }

    @Bean
    public Binding reserveHealthyBinding() {
        return BindingBuilder.bind(riskAlertQueue())
                .to(collateralEventExchange())
                .with(reserveHealthyRoutingKey);
    }

    @Bean
    public Binding riskLevelChangeBinding() {
        return BindingBuilder.bind(riskLevelChangeQueue())
                .to(collateralEventExchange())
                .with(riskLevelChangeRoutingKey);
    }

    // ---- Message Converter & Template ----

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
