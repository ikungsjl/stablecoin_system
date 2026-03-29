package com.stablecoin.collateral.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/** 储备验证响应 DTO（供课题1调用） */
@Data
@Builder
public class ReserveCheckResponse {

    /** 当前储备率 */
    private BigDecimal reserveRatio;

    /** 风险等级 */
    private String riskLevel;

    /** 是否允许继续发行 */
    private boolean available;

    /** 当前总储备 */
    private BigDecimal totalReserve;

    /** 当前稳定币流通量 */
    private BigDecimal stablecoinSupply;

    /** 提示信息 */
    private String message;
}
