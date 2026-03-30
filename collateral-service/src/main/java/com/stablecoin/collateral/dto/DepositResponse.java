package com.stablecoin.collateral.dto;

import com.stablecoin.collateral.entity.CollateralDeposit;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 抵押物存入响应 DTO
 * 在原有存入记录基础上，附加稳定币发行状态信息：
 *   - 若课题1可用稳定币余额充足，直接分配；
 *   - 若不足，课题2已请求课题1发行补充，并在此处告知调用方。
 */
@Data
@Builder
public class DepositResponse {

    /** 存入成功的抵押物记录 */
    private CollateralDeposit deposit;

    /** 本次存入对应需分配的稳定币金额（USD，等于存入金额） */
    private BigDecimal requiredStablecoin;

    /** 通知课题1后课题1当前可用稳定币余额（可能为 null，代表课题1未响应） */
    private BigDecimal availableSupply;

    /**
     * 稳定币发行状态：
     *   SUFFICIENT   — 课题1余额充足，无需额外发行
     *   ISSUANCE_REQUESTED — 余额不足，已请求课题1发行
     *   ISSUANCE_UNAVAILABLE — 课题1服务不可用，请求失败
     */
    private IssuanceStatus issuanceStatus;

    /** 给调用方的提示信息 */
    private String message;

    public enum IssuanceStatus {
        SUFFICIENT,
        ISSUANCE_REQUESTED,
        ISSUANCE_UNAVAILABLE
    }
}
