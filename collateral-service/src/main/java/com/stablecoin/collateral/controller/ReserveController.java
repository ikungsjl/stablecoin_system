package com.stablecoin.collateral.controller;

import com.stablecoin.collateral.dto.ApiResponse;
import com.stablecoin.collateral.dto.DashboardResponse;
import com.stablecoin.collateral.dto.ReserveCheckResponse;
import com.stablecoin.collateral.entity.ReserveSnapshot;
import com.stablecoin.collateral.service.ReserveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "储备管理", description = "储备验证、快照与仪表盘接口")
@RestController
@RequestMapping("/api/reserve")
@RequiredArgsConstructor
public class ReserveController {

    private final ReserveService reserveService;

    @Operation(summary = "仪表盘数据")
    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> dashboard() {
        return ApiResponse.success(reserveService.getDashboard());
    }

    @Operation(summary = "手动触发储备验证快照")
    @PostMapping("/snapshot")
    public ApiResponse<ReserveSnapshot> snapshot() {
        return ApiResponse.success(reserveService.checkAndSnapshot());
    }

    @Operation(summary = "储备率历史记录（折线图数据）")
    @GetMapping("/history")
    public ApiResponse<List<ReserveSnapshot>> history(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ApiResponse.success(reserveService.getHistory(start, end));
    }

    @Operation(summary = "验证当前储备是否支持继续发行（供课题1调用）")
    @PostMapping("/check")
    public ApiResponse<ReserveCheckResponse> check(
            @RequestParam BigDecimal stablecoinSupply) {
        return ApiResponse.success(reserveService.checkReserve(stablecoinSupply));
    }
}
