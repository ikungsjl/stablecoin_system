package com.stablecoin.collateral.service;

import com.stablecoin.collateral.dto.DepositRequest;
import com.stablecoin.collateral.entity.CollateralDeposit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CollateralService {

    /** 处理抵押物存入 */
    CollateralDeposit deposit(DepositRequest request);

    /** 查询存入记录列表 */
    List<CollateralDeposit> listDeposits(LocalDateTime start, LocalDateTime end);

    /** 根据 txHash 查询单条记录 */
    CollateralDeposit getByTxHash(String txHash);

    /** 查询总储备（USD） */
    BigDecimal getTotalReserve();

    /** 增加储备（赎回时扣减 - 传负数） */
    void updateReservePool(BigDecimal deltaUsd);

    /** 锁定/解锁金额（赎回处理中） */
    void lockAmount(BigDecimal amount);
    void unlockAmount(BigDecimal amount);
}
