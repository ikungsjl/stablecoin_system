package com.stablecoin.collateral.repository;

import com.stablecoin.collateral.entity.ReserveSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReserveSnapshotRepository extends JpaRepository<ReserveSnapshot, Long> {

    /** 查询指定时间段的快照（用于图表） */
    List<ReserveSnapshot> findBySnapshotAtBetweenOrderBySnapshotAtAsc(
            LocalDateTime start, LocalDateTime end);

    /** 最新一条快照 */
    ReserveSnapshot findTopByOrderBySnapshotAtDesc();

    /** 查询时间段内平均储备率 */
    @Query("SELECT AVG(s.reserveRatio) FROM ReserveSnapshot s " +
           "WHERE s.snapshotAt BETWEEN :start AND :end")
    BigDecimal avgRatioBetween(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    /** 查询时间段内最低储备率 */
    @Query("SELECT MIN(s.reserveRatio) FROM ReserveSnapshot s " +
           "WHERE s.snapshotAt BETWEEN :start AND :end")
    BigDecimal minRatioBetween(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    /** 查询时间段内最高储备率 */
    @Query("SELECT MAX(s.reserveRatio) FROM ReserveSnapshot s " +
           "WHERE s.snapshotAt BETWEEN :start AND :end")
    BigDecimal maxRatioBetween(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);
}
