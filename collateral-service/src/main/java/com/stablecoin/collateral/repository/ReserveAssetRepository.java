package com.stablecoin.collateral.repository;

import com.stablecoin.collateral.entity.ReserveAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReserveAssetRepository extends JpaRepository<ReserveAsset, Long> {

    Optional<ReserveAsset> findByAssetType(String assetType);

    List<ReserveAsset> findByRiskLevelOrderByUsdValueDesc(Integer riskLevel);

    List<ReserveAsset> findAllByOrderByUsdValueDesc();

    @Query("SELECT COALESCE(SUM(a.usdValue), 0) FROM ReserveAsset a")
    BigDecimal sumTotalUsdValue();

    /** 预期年化利息收入 = SUM(持仓 * 收益率) */
    @Query("SELECT COALESCE(SUM(a.usdValue * a.annualYield), 0) FROM ReserveAsset a WHERE a.annualYield > 0")
    BigDecimal estimateAnnualInterestIncome();
}
