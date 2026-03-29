package com.stablecoin.collateral.service.impl;

import com.stablecoin.collateral.dto.ReserveAssetResponse;
import com.stablecoin.collateral.entity.ReserveAsset;
import com.stablecoin.collateral.repository.ReserveAssetRepository;
import com.stablecoin.collateral.repository.ReservePoolRepository;
import com.stablecoin.collateral.service.ReserveAssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 储备资产服务实现
 * 提供动态计算占比的资产查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReserveAssetServiceImpl implements ReserveAssetService {

    private final ReserveAssetRepository assetRepository;
    private final ReservePoolRepository poolRepository;

    @Override
    public List<ReserveAssetResponse> getPortfolio() {
        // 获取总储备
        BigDecimal totalReserve = poolRepository.findById(1L)
                .map(pool -> pool.getTotalUsdAmount())
                .orElse(BigDecimal.ZERO);

        // 查询所有资产
        List<ReserveAsset> assets = assetRepository.findAll();

        // 转换为响应 DTO，动态计算占比
        return assets.stream()
                .map(asset -> convertToResponse(asset, totalReserve))
                .toList();
    }

    @Override
    public ReserveAssetResponse getAssetByType(String assetType) {
        BigDecimal totalReserve = poolRepository.findById(1L)
                .map(pool -> pool.getTotalUsdAmount())
                .orElse(BigDecimal.ZERO);

        ReserveAsset asset = assetRepository.findByAssetType(assetType)
                .orElseThrow(() -> new RuntimeException("资产不存在: " + assetType));

        return convertToResponse(asset, totalReserve);
    }

    /**
     * 将资产实体转换为响应 DTO，动态计算占比
     */
    private ReserveAssetResponse convertToResponse(ReserveAsset asset, BigDecimal totalReserve) {
        // 动态计算实际占比
        BigDecimal actualRatio = asset.calculateActualRatio(totalReserve);

        // 计算预期年化收益
        BigDecimal estimatedIncome = asset.getUsdValue()
                .multiply(asset.getAnnualYield())
                .setScale(6, RoundingMode.HALF_UP);

        return ReserveAssetResponse.builder()
                .assetType(asset.getAssetType())
                .assetName(asset.getAssetName())
                .usdValue(asset.getUsdValue())
                .targetAllocationRatio(asset.getTargetAllocationRatio())
                .actualAllocationRatio(actualRatio)
                .riskLevel(asset.getRiskLevel())
                .riskLevelName(asset.getRiskLevelName())
                .annualYield(asset.getAnnualYield())
                .estimatedAnnualIncome(estimatedIncome)
                .description(asset.getDescription())
                .build();
    }
}
