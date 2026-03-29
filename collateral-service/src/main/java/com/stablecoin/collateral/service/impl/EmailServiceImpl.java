package com.stablecoin.collateral.service.impl;

import com.stablecoin.collateral.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 邮件服务实现
 * 发送风险告警和恢复通知邮件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@stablecoin-system.com}")
    private String fromEmail;

    @Value("${app.email.admin-recipients:admin@stablecoin-system.com}")
    private String adminRecipients;

    @Override
    public void sendRiskAlertEmail(List<String> recipients, String riskLevel,
                                   BigDecimal reserveRatio, BigDecimal gapAmount) {
        try {
            String subject = "[紧急告警] 储备风险等级变为 " + riskLevel;
            String body = buildRiskAlertEmailBody(riskLevel, reserveRatio, gapAmount);

            sendEmail(recipients, subject, body);
            log.info("[Email] 风险告警邮件已发送 recipients={} level={}", recipients, riskLevel);
        } catch (Exception e) {
            log.error("[Email] 风险告警邮件发送失败: {}", e.getMessage());
        }
    }

    @Override
    public void sendRiskRecoveryEmail(List<String> recipients, BigDecimal reserveRatio) {
        try {
            String subject = "[恢复通知] 储备风险等级已恢复为 HEALTHY";
            String body = buildRiskRecoveryEmailBody(reserveRatio);

            sendEmail(recipients, subject, body);
            log.info("[Email] 风险恢复邮件已发送 recipients={}", recipients);
        } catch (Exception e) {
            log.error("[Email] 风险恢复邮件发送失败: {}", e.getMessage());
        }
    }

    // ---- private helpers ----

    private void sendEmail(List<String> recipients, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(recipients.toArray(new String[0]));
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    private String buildRiskAlertEmailBody(String riskLevel, BigDecimal reserveRatio, BigDecimal gapAmount) {
        return String.format(
                "系统检测到储备风险告警\n\n" +
                "告警时间: %s\n" +
                "风险等级: %s\n" +
                "当前储备率: %.6f\n" +
                "缺口金额: %.2f USD\n\n" +
                "请立即采取措施补充储备或暂停稳定币发行。\n\n" +
                "---\n" +
                "稳定币系统自动告警",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                riskLevel,
                reserveRatio.doubleValue(),
                gapAmount.doubleValue()
        );
    }

    private String buildRiskRecoveryEmailBody(BigDecimal reserveRatio) {
        return String.format(
                "系统检测到储备风险已恢复\n\n" +
                "恢复时间: %s\n" +
                "当前储备率: %.6f\n" +
                "风险等级: HEALTHY\n\n" +
                "系统已恢复正常，可继续稳定币发行。\n\n" +
                "---\n" +
                "稳定币系统自动通知",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                reserveRatio.doubleValue()
        );
    }
}
