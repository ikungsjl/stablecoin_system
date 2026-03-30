package com.stablecoin.collateral.repository;

import com.stablecoin.collateral.entity.RiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {

    /** 查询所有活跃警报 */
    List<RiskAlert> findByStatusOrderByCreatedAtDesc(String status);

    /** 检查是否已存在同类型的活跃警报（去重） */
    Optional<RiskAlert> findTopByAlertTypeAndStatusOrderByCreatedAtDesc(
            String alertType, String status);

    /** 统计时间段内警报数量 */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /** 查询时间段内所有警报（用于审计报告） */
    List<RiskAlert> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end);
}
