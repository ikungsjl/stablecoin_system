package com.stablecoin.collateral.scheduler;

import com.stablecoin.collateral.service.AuditReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计报告生成调度器
 * 每天凌晨 00:00 生成前一天的审计报告
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditReportScheduler {

    private final AuditReportService auditReportService;

    /**
     * 每天凌晨 00:00 执行
     * cron 表达式：0 0 0 * * ? 表示每天 00:00:00
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void generateDailyAuditReport() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime yesterday = now.minusDays(1);
            
            // 前一天的 00:00:00 到 23:59:59
            LocalDateTime periodStart = yesterday.withHour(0).withMinute(0).withSecond(0);
            LocalDateTime periodEnd = yesterday.withHour(23).withMinute(59).withSecond(59);

            log.info("[AuditScheduler] 开始生成每日审计报告 period={} ~ {}", periodStart, periodEnd);
            
            auditReportService.generateReport(periodStart, periodEnd, "system-scheduler");
            
            log.info("[AuditScheduler] 每日审计报告生成完成");
        } catch (Exception e) {
            log.error("[AuditScheduler] 每日审计报告生成失败: {}", e.getMessage(), e);
        }
    }
}
