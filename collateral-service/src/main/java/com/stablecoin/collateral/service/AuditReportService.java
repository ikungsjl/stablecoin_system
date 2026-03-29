package com.stablecoin.collateral.service;

import com.stablecoin.collateral.entity.AuditReport;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditReportService {

    /** 生成指定时间段的审计报告 */
    AuditReport generateReport(LocalDateTime start, LocalDateTime end, String generatedBy);

    /** 查询所有报告 */
    List<AuditReport> listReports();

    /** 根据报告编号查询 */
    AuditReport getByReportNo(String reportNo);
}
