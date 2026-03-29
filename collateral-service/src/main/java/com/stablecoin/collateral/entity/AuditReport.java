package com.stablecoin.collateral.entity;

import lombok.*;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_reports")
public class AuditReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_no", nullable = false, unique = true, length = 50)
    private String reportNo;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Column(name = "avg_ratio", precision = 10, scale = 6)
    private BigDecimal avgRatio;

    @Column(name = "min_ratio", precision = 10, scale = 6)
    private BigDecimal minRatio;

    @Column(name = "max_ratio", precision = 10, scale = 6)
    private BigDecimal maxRatio;

    @Column(name = "total_deposit", precision = 20, scale = 6)
    private BigDecimal totalDeposit;

    @Column(name = "total_issuance", precision = 20, scale = 6)
    private BigDecimal totalIssuance;

    @Column(name = "total_redemption", precision = 20, scale = 6)
    private BigDecimal totalRedemption;

    @Column(name = "alert_count")
    private Integer alertCount;

    /** 完整报告数据以 JSON 字符串存储 */
    @Column(name = "report_data", columnDefinition = "TEXT")
    private String reportDataJson;

    @Column(name = "generated_by", length = 100)
    private String generatedBy;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    public void prePersist() {
        this.generatedAt = LocalDateTime.now();
        if (this.alertCount == null) this.alertCount = 0;
    }
}
