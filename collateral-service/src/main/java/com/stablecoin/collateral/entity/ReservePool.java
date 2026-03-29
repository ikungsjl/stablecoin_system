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
@Table(name = "reserve_pool")
public class ReservePool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_usd_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal totalUsdAmount;

    @Column(name = "locked_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal lockedAmount;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.lastUpdated = LocalDateTime.now();
        if (this.totalUsdAmount == null) this.totalUsdAmount = BigDecimal.ZERO;
        if (this.lockedAmount == null) this.lockedAmount = BigDecimal.ZERO;
    }

    @Transient
    public BigDecimal getAvailableAmount() {
        return totalUsdAmount.subtract(lockedAmount);
    }
}
