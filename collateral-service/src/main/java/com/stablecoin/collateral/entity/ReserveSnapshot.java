package com.stablecoin.collateral.entity;

import lombok.*;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reserve_snapshots")
public class ReserveSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserve_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal reserveAmount;

    @Column(name = "stablecoin_supply", nullable = false, precision = 20, scale = 6)
    private BigDecimal stablecoinSupply;

    @Column(name = "reserve_ratio", nullable = false, precision = 10, scale = 6)
    private BigDecimal reserveRatio;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(name = "snapshot_at", nullable = false)
    private LocalDateTime snapshotAt;

    @PrePersist
    public void prePersist() {
        if (this.snapshotAt == null) this.snapshotAt = LocalDateTime.now();
    }

    public enum RiskLevel {
        HEALTHY, NORMAL, WARNING, CRITICAL
    }
}
