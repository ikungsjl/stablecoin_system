package com.stablecoin.collateral.controller;

import com.stablecoin.collateral.dto.ApiResponse;
import com.stablecoin.collateral.dto.DepositRequest;
import com.stablecoin.collateral.entity.CollateralDeposit;
import com.stablecoin.collateral.service.CollateralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "抵押物管理", description = "抵押物存入与查询接口")
@RestController
@RequestMapping("/api/collateral")
@RequiredArgsConstructor
public class CollateralController {

    private final CollateralService collateralService;

    @Operation(summary = "抵押物存入")
    @PostMapping("/deposit")
    public ApiResponse<CollateralDeposit> deposit(@Valid @RequestBody DepositRequest request) {
        return ApiResponse.success(collateralService.deposit(request));
    }

    @Operation(summary = "查询存入记录")
    @GetMapping("/deposits")
    public ApiResponse<List<CollateralDeposit>> listDeposits(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ApiResponse.success(collateralService.listDeposits(start, end));
    }

    @Operation(summary = "根据交易哈希查询")
    @GetMapping("/deposits/{txHash}")
    public ApiResponse<CollateralDeposit> getByTxHash(@PathVariable String txHash) {
        return ApiResponse.success(collateralService.getByTxHash(txHash));
    }

    @Operation(summary = "储备验证（供课题1调用）")
    @PostMapping("/reserve-check")
    public ApiResponse<?> reserveCheck(
            @RequestParam java.math.BigDecimal stablecoinSupply) {
        return ApiResponse.success(
                // 使用 ReserveService，此处注入在 ReserveController
                // 简化：直接返回当前储备
                collateralService.getTotalReserve());
    }
}
