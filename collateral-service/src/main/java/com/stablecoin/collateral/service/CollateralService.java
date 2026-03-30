package com.stablecoin.collateral.service;

import com.stablecoin.collateral.dto.DepositRequest;
import com.stablecoin.collateral.dto.DepositResponse;
import com.stablecoin.collateral.entity.CollateralDeposit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CollateralService {

    /**
     * 处理抵押物存入：
     * 1. 记录存入并更新储备池
     * 2. 检查课题1可用稳定币余额是否足够支付本次存入对应金额
     * 3. 不足时自动请求课题1发行稳定币
     */
    DepositResponse deposit(DepositRequest request);

    /** 查询存入记录列表 */
    List<CollateralDeposit> listDeposits(LocalDateTime start, LocalDateTime end);

    /** 根据 txHash 查询单条记录 */
    CollateralDeposit getByTxHash(String txHash);

    /** 查询总储备（USD） */
    BigDecimal getTotalReserve();

    /** 增加/减少储备（传负数为扣减） */
    void updateReservePool(BigDecimal deltaUsd);

    /** 锁定金额（赎回处理中） */
    void lockAmount(BigDecimal amount);

    /** 解锁金额 */
    void unlockAmount(BigDecimal amount);
}
