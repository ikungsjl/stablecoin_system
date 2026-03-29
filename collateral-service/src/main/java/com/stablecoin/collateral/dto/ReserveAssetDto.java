package com.stablecoin.collateral.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ReserveAssetDto {

    @Data
    public static class UpdateRequest {
        private String assetType;
        private BigDecimal usdValue;
        private String description;
    }

    @Data
    @Builder
    public static class AssetItem {
        private Long id;
        private String assetType;
        private String assetName;
        private BigDecimal usdValue;
        private BigDecimal allocationRatio;
        private String riskLevel;
        private BigDecimal annualYield;
        private BigDecimal estimatedAnnualIncome;
        private String description;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    public static class PortfolioResponse {
        private List<AssetItem> assets;
        private BigDecimal totalUsdValue;
        private BigDecimal lowRiskRatio;
        private BigDecimal mediumRiskRatio;
        private BigDecimal highRiskRatio;
        private BigDecimal estimatedAnnualIncome;
        private LocalDateTime updatedAt;
    }
}
