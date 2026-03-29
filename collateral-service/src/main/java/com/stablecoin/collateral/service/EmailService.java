package com.stablecoin.collateral.service;

import java.util.List;

/**
 * 邮件服务接口
 */
public interface EmailService {

    /**
     * 发送风险告警邮件
     * @param recipients 收件人列表
     * @param riskLevel 风险等级
     * @param reserveRatio 储备率
     * @param gapAmount 缺口金额
     */
    void sendRiskAlertEmail(List<String> recipients, String riskLevel, 
                           java.math.BigDecimal reserveRatio, java.math.BigDecimal gapAmount);

    /**
     * 发送风险恢复邮件
     * @param recipients 收件人列表
     * @param reserveRatio 储备率
     */
    void sendRiskRecoveryEmail(List<String> recipients, java.math.BigDecimal reserveRatio);
}
