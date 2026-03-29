package com.stablecoin.collateral.controller;

import com.stablecoin.collateral.dto.ApiResponse;
import com.stablecoin.collateral.entity.AuditReport;
import com.stablecoin.collateral.service.AuditReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "审计报告", description = "审计报告生成与查询接口")
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditReportController {

    private final AuditReportService auditReportService;

    @Operation(summary = "查询所有审计报告")
    @GetMapping("/reports")
    public ApiResponse<List<AuditReport>> listReports() {
        return ApiResponse.success(auditReportService.listReports());
    }

    @Operation(summary = "根据报告编号查询")
    @GetMapping("/reports/{reportNo}")
    public ApiResponse<AuditReport> getByReportNo(@PathVariable String reportNo) {
        return ApiResponse.success(auditReportService.getByReportNo(reportNo));
    }

    @Operation(summary = "手动生成审计报告",
               description = "指定时间范围，手动生成审计报告（通常由定时任务自动生成）")
    @PostMapping("/reports/generate")
    public ApiResponse<AuditReport> generateReport(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end,
            @RequestParam(defaultValue = "manual-trigger") String operator) {
        return ApiResponse.success(auditReportService.generateReport(start, end, operator));
    }
}
