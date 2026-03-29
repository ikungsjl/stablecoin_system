package com.stablecoin.collateral.repository;

import com.stablecoin.collateral.entity.ReservePool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservePoolRepository extends JpaRepository<ReservePool, Long> {
    // 始终只有一条记录，通过 findById(1L) 获取
}
