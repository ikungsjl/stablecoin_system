package com.stablecoin.collateral.entity;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 储备资产配置实体
 * 模拟泰达币真实储备构成（参考 Tether Q4 2024 Attestation Report）：
 *   - 美国短期国债 ~84%  年化收益约5.3%
 *   - 货币市场基金 ~4%   年化收益约5.2%
 *   - 隔夜逆回购   ~8%   年化收益约5.4%
 *   - 现金         ~1%   年化收益约0.1%
 *   - 黄金         ~4%   无固定收益
 *   - 比特币       ~2%   无固定收益
 *   - 其他资产     ~1%   年化收益约3.0%
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reserve_assets")
public class ReserveAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 资产类型：US_TREASURY / MONEY_MARKET / OVERNIGHT_REPO / CASH / GOLD / BITCOIN / OTHER */
    @Column(name = "asset_type", nullable = false, unique = true, length = 30)
    private String assetType;

    /** 资产名称（展示用） */
    @Column(name = "asset_name", nullable = false, length = 100)
    private String assetName;

    /** 持有价值（USD）- 减少初始值 */
    @Column(name = "usd_value", nullable = false, precision = 20, scale = 6)
    private BigDecimal usdValue;

    /**
     * 占总储备比例（0.000000 ~ 1.000000）
     * 注意：这是配置的目标占比，实际占比由 usdValue / 总储备 动态计算
     * 数据库中存储的是目标占比，用于初始化和调整
     */
    @Column(name = "target_allocation_ratio", nullable = false, precision = 8, scale = 6)
    private BigDecimal targetAllocationRatio;

    /**
     * 风险等级：1 = LOW / 2 = MEDIUM / 3 = HIGH
     * 使用数字标识，便于排序和计算
     */
    @Column(name = "risk_level", nullable = false)
    private Integer riskLevel;

    /** 预期年化收益率（如 0.053000 = 5.3%） */
    @Column(name = "annual_yield", precision = 8, scale = 6)
    private BigDecimal annualYield;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 计算实际占比（动态计算）
     * @param totalReserve 总储备金额
     * @return 实际占比
     */
    public BigDecimal calculateActualRatio(BigDecimal totalReserve) {
        if (totalReserve == null || totalReserve.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return this.usdValue.divide(totalReserve, 6, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 获取风险等级名称
     */
    public String getRiskLevelName() {
        return switch (this.riskLevel) {
            case 1 -> "LOW";
            case 2 -> "MEDIUM";
            case 3 -> "HIGH";
            default -> "UNKNOWN";
        };
    }

    public enum AssetType {
        US_TREASURY("美国短期国债", 1, new java.math.BigDecimal("0.053000"), new java.math.BigDecimal("0.84")),
        MONEY_MARKET("货币市场基金", 1, new java.math.BigDecimal("0.052000"), new java.math.BigDecimal("0.04")),
        OVERNIGHT_REPO("隔夜逆回购协议", 1, new java.math.BigDecimal("0.054000"), new java.math.BigDecimal("0.08")),
        CASH("现金及银行存款", 1, new java.math.BigDecimal("0.001000"), new java.math.BigDecimal("0.01")),
        GOLD("实物黄金", 2, new java.math.BigDecimal("0.000000"), new java.math.BigDecimal("0.04")),
        BITCOIN("比特币", 3, new java.math.BigDecimal("0.000000"), new java.math.BigDecimal("0.02")),
        OTHER("其他资产", 2, new java.math.BigDecimal("0.030000"), new java.math.BigDecimal("0.01"));

        public final String displayName;
        public final Integer riskLevel;  // 1=LOW, 2=MEDIUM, 3=HIGH
        public final java.math.BigDecimal defaultYield;
        public final java.math.BigDecimal targetRatio;

        AssetType(String displayName, Integer riskLevel, java.math.BigDecimal defaultYield, java.math.BigDecimal targetRatio) {
            this.displayName = displayName;
            this.riskLevel = riskLevel;
            this.defaultYield = defaultYield;
            this.targetRatio = targetRatio;
        }
    }
}
