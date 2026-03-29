package com.stablecoin.collateral.repository;

import com.stablecoin.collateral.entity.CollateralDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollateralDepositRepository extends JpaRepository<CollateralDeposit, Long> {

    Optional<CollateralDeposit> findByTxHash(String txHash);

    List<CollateralDeposit> findByStatusOrderByDepositedAtDesc(String status);

    List<CollateralDeposit> findByDepositedAtBetweenOrderByDepositedAtDesc(
            LocalDateTime start, LocalDateTime end);

    /** 统计指定时间段内确认存入的总金额（USD） */
    @Query("SELECT COALESCE(SUM(d.usdAmount), 0) FROM CollateralDeposit d " +
           "WHERE d.status = 'CONFIRMED' AND d.depositedAt BETWEEN :start AND :end")
    BigDecimal sumConfirmedUsdAmountBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /** 统计全部确认存入总额 */
    @Query("SELECT COALESCE(SUM(d.usdAmount), 0) FROM CollateralDeposit d WHERE d.status = 'CONFIRMED'")
    BigDecimal sumAllConfirmedUsdAmount();
}
