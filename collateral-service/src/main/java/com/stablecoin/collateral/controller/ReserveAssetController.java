package com.stablecoin.collateral.controller;

import com.stablecoin.collateral.dto.ApiResponse;
import com.stablecoin.collateral.dto.ReserveAssetResponse;
import com.stablecoin.collateral.service.ReserveAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "储备资产", description = "储备资产配置与组合管理")
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class ReserveAssetController {

    private final ReserveAssetService assetService;

    @Operation(summary = "获取完整资产组合",
               description = "返回所有储备资产，包含动态计算的实际占比")
    @GetMapping("/portfolio")
    public ApiResponse<List<ReserveAssetResponse>> getPortfolio() {
        return ApiResponse.success(assetService.getPortfolio());
    }

    @Operation(summary = "获取单个资产详情",
               description = "根据资产类型获取单个资产信息")
    @GetMapping("/assets/{assetType}")
    public ApiResponse<ReserveAssetResponse> getAsset(@PathVariable String assetType) {
        return ApiResponse.success(assetService.getAssetByType(assetType));
    }
}
