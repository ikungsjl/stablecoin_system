package com.stablecoin.collateral.controller;

import com.stablecoin.collateral.dto.ApiResponse;
import com.stablecoin.collateral.entity.RiskAlert;
import com.stablecoin.collateral.service.RiskAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "风险警报", description = "风险警报查询与处理接口")
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class RiskAlertController {

    private final RiskAlertService riskAlertService;

    @Operation(summary = "查询所有活跃警报")
    @GetMapping("/active")
    public ApiResponse<List<RiskAlert>> getActiveAlerts() {
        return ApiResponse.success(riskAlertService.getActiveAlerts());
    }

    @Operation(summary = "确认警报")
    @PutMapping("/{id}/acknowledge")
    public ApiResponse<RiskAlert> acknowledge(
            @PathVariable Long id,
            @RequestParam String operator) {
        return ApiResponse.success(riskAlertService.acknowledgeAlert(id, operator));
    }

    @Operation(summary = "解决警报")
    @PutMapping("/{id}/resolve")
    public ApiResponse<RiskAlert> resolve(
            @PathVariable Long id,
            @RequestParam String operator) {
        return ApiResponse.success(riskAlertService.resolveAlert(id, operator));
    }
}
