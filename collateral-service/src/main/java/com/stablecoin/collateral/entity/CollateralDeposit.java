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
@Table(name = "collateral_deposits")
public class CollateralDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tx_hash", nullable = false, unique = true, length = 66)
    private String txHash;

    @Column(name = "amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "usd_amount", nullable = false, precision = 20, scale = 6)
    private BigDecimal usdAmount;

    @Column(name = "exchange_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "operator", nullable = false, length = 100)
    private String operator;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "deposited_at", nullable = false)
    private LocalDateTime depositedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.depositedAt == null) this.depositedAt = LocalDateTime.now();
        if (this.status == null) this.status = Status.CONFIRMED.name();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Status {
        PENDING, CONFIRMED, FAILED
    }
}
