package com.stablecoin.collateral.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 抵押物存入请求 DTO
 * 前端课题传入交易哈希、时间戳等信息，课题2验证并记录
 */
@Data
public class DepositRequest {

    @NotBlank(message = "交易哈希不能为空")
    @Pattern(regexp = "^0x[a-fA-F0-9]{64}$", message = "交易哈希格式错误，应为 0x + 64位十六进制")
    private String txHash;

    @NotNull(message = "存入金额不能为空")
    @DecimalMin(value = "0.000001", message = "存入金额必须大于0")
    private BigDecimal amount;

    @NotNull(message = "存入时间不能为空")
    private LocalDateTime depositedAt;

    @NotBlank(message = "操作员不能为空")
    private String operator;

    private String remark;
}
