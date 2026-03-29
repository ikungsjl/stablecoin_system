package com.stablecoin.collateral.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablecoin.collateral.entity.AuditReport;
import com.stablecoin.collateral.entity.ReserveSnapshot;
import com.stablecoin.collateral.exception.BusinessException;
import com.stablecoin.collateral.repository.*;
import com.stablecoin.collateral.service.AuditReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditReportServiceImpl implements AuditReportService {

    private final AuditReportRepository reportRepository;
    private final ReserveSnapshotRepository snapshotRepository;
    private final CollateralDepositRepository depositRepository;
    private final RiskAlertRepository alertRepository;
    private final ReserveAssetRepository assetRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AuditReport generateReport(LocalDateTime start, LocalDateTime end, String generatedBy) {
        // 1. 收集数据
        BigDecimal avgRatio = snapshotRepository.avgRatioBetween(start, end);
        BigDecimal minRatio = snapshotRepository.minRatioBetween(start, end);
        BigDecimal maxRatio = snapshotRepository.maxRatioBetween(start, end);
        BigDecimal totalDeposit = depositRepository.sumConfirmedUsdAmountBetween(start, end);
        long alertCount = alertRepository.countByCreatedAtBetween(start, end);

        // 2. 生成报告编号
        String reportNo = "RPT-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

        // 3. 构建详细报告数据
        Map<String, Object> reportData = buildDetailedReportData(start, end, 
                avgRatio, minRatio, maxRatio, totalDeposit, alertCount);

        String reportDataJson;
        try {
            reportDataJson = objectMapper.writeValueAsString(reportData);
        } catch (Exception e) {
            reportDataJson = "{}";
        }

        // 4. 保存报告
        AuditReport report = AuditReport.builder()
                .reportNo(reportNo)
                .periodStart(start)
                .periodEnd(end)
                .avgRatio(avgRatio)
                .minRatio(minRatio)
                .maxRatio(maxRatio)
                .totalDeposit(totalDeposit)
                .totalIssuance(BigDecimal.ZERO)
                .totalRedemption(BigDecimal.ZERO)
                .alertCount((int) alertCount)
                .reportDataJson(reportDataJson)
                .generatedBy(generatedBy)
                .build();

        reportRepository.save(report);
        log.info("[Audit] 审计报告生成: {} period={} ~ {} generatedBy={}", 
                reportNo, start, end, generatedBy);
        return report;
    }

    @Override
    public List<AuditReport> listReports() {
        return reportRepository.findAllByOrderByGeneratedAtDesc();
    }

    @Override
    public AuditReport getByReportNo(String reportNo) {
        return reportRepository.findByReportNo(reportNo)
                .orElseThrow(() -> new BusinessException("报告不存在: " + reportNo));
    }

    /**
     * 构建详细的审计报告数据
     * 包含：储备率统计、存入统计、风险告警、资产配置等
     */
    private Map<String, Object> buildDetailedReportData(
            LocalDateTime start, LocalDateTime end,
            BigDecimal avgRatio, BigDecimal minRatio, BigDecimal maxRatio,
            BigDecimal totalDeposit, long alertCount) {

        Map<String, Object> data = new HashMap<>();

        // 基础统计
        data.put("periodStart", start.toString());
        data.put("periodEnd", end.toString());
        data.put("generatedAt", LocalDateTime.now().toString());

        // 储备率统计
        Map<String, Object> ratioStats = new HashMap<>();
        ratioStats.put("average", avgRatio);
        ratioStats.put("minimum", minRatio);
        ratioStats.put("maximum", maxRatio);
        data.put("reserveRatioStats", ratioStats);

        // 存入统计
        Map<String, Object> depositStats = new HashMap<>();
        depositStats.put("totalAmount", totalDeposit);
        depositStats.put("count", depositRepository.countByDepositedAtBetween(start, end));
        data.put("depositStats", depositStats);

        // 风险告警统计
        Map<String, Object> alertStats = new HashMap<>();
        alertStats.put("totalCount", alertCount);
        List<String> riskLevels = alertRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end)
                .stream()
                .map(alert -> alert.getRiskLevel())
                .distinct()
                .toList();
        alertStats.put("riskLevels", riskLevels);
        data.put("alertStats", alertStats);

        // 资产配置快照
        Map<String, Object> assetSnapshot = new HashMap<>();
        assetSnapshot.put("totalValue", assetRepository.sumTotalUsdValue());
        assetSnapshot.put("lowRiskRatio", calculateRiskRatio(1));
        assetSnapshot.put("mediumRiskRatio", calculateRiskRatio(2));
        assetSnapshot.put("highRiskRatio", calculateRiskRatio(3));
        assetSnapshot.put("estimatedAnnualIncome", assetRepository.estimateAnnualInterestIncome());
        data.put("assetSnapshot", assetSnapshot);

        // 储备快照（期间内最后一条快照）
        List<ReserveSnapshot> snapshots = snapshotRepository
                .findBySnapshotAtBetweenOrderBySnapshotAtAsc(start, end);
        if (!snapshots.isEmpty()) {
            ReserveSnapshot lastSnapshot = snapshots.get(snapshots.size() - 1);
            Map<String, Object> reserveSnapshot = new HashMap<>();
            reserveSnapshot.put("reserveAmount", lastSnapshot.getReserveAmount());
            reserveSnapshot.put("stablecoinSupply", lastSnapshot.getStablecoinSupply());
            reserveSnapshot.put("reserveRatio", lastSnapshot.getReserveRatio());
            reserveSnapshot.put("riskLevel", lastSnapshot.getRiskLevel());
            reserveSnapshot.put("snapshotAt", lastSnapshot.getSnapshotAt().toString());
            data.put("finalReserveSnapshot", reserveSnapshot);
        }

        return data;
    }

    /**
     * 计算指定风险等级的资产占比
     * @param riskLevel 风险等级（1=LOW, 2=MEDIUM, 3=HIGH）
     */
    private BigDecimal calculateRiskRatio(Integer riskLevel) {
        BigDecimal total = assetRepository.sumTotalUsdValue();
        if (total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        
        BigDecimal riskAmount = assetRepository.findByRiskLevelOrderByUsdValueDesc(riskLevel)
                .stream()
                .map(a -> a.getUsdValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return riskAmount.divide(total, 6, java.math.RoundingMode.HALF_UP);
    }
}
