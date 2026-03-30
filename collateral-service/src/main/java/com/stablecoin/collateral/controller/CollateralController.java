package com.stablecoin.collateral.controller;

import com.stablecoin.collateral.dto.ApiResponse;
import com.stablecoin.collateral.dto.DepositRequest;
import com.stablecoin.collateral.dto.DepositResponse;
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

    /**
     * 抵押物存入
     *
     * 存入成功后会自动检查课题1的可用稳定币余额：
     *   - issuanceStatus = SUFFICIENT            余额充足，无需额外发行
     *   - issuanceStatus = ISSUANCE_REQUESTED    余额不足，已请求课题1补充发行
     *   - issuanceStatus = ISSUANCE_UNAVAILABLE  课题1服务不可达，请人工确认
     */
    @Operation(
        summary = "抵押物存入",
        description = "记录抵押物存入并自动检查稳定币余额。" +
            "若课题1可用余额不足，系统将自动请求课题1发行对应金额的稳定币。" +
            "响应中 issuanceStatus 字段标明发行状态：" +
            "SUFFICIENT=余额充足；" +
            "ISSUANCE_REQUESTED=已请求课题1发行；" +
            "ISSUANCE_UNAVAILABLE=课题1不可达"
    )
    @PostMapping("/deposit")
    public ApiResponse<DepositResponse> deposit(@Valid @RequestBody DepositRequest request) {
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

    @Operation(
        summary = "储备验证（供课题1调用）",
        description = "返回当前总储备金额（USD），供课题1判断是否允许继续发行"
    )
    @PostMapping("/reserve-check")
    public ApiResponse<java.math.BigDecimal> reserveCheck(
            @RequestParam java.math.BigDecimal stablecoinSupply) {
        return ApiResponse.success(collateralService.getTotalReserve());
    }
}
