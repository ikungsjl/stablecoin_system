package com.stablecoin.collateral.repository;

import com.stablecoin.collateral.entity.AuditReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditReportRepository extends JpaRepository<AuditReport, Long> {

    Optional<AuditReport> findByReportNo(String reportNo);

    List<AuditReport> findAllByOrderByGeneratedAtDesc();
}
