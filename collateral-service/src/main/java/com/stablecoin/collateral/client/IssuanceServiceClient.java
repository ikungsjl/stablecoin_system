package com.stablecoin.collateral.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 课题1（issuance-service）API 客户端
 * 课题2通过此类主动通知课题1储备风险状态，课题1根据通知决定是否暂停/恢复发行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssuanceServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.issuance-service.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.issuance-service.api-key:internal-api-key-001}")
    private String apiKey;

    /**
     * 通知课题1：储备不足，请求暂停稳定币发行
     * 对应课题1接口：POST /api/issuance/suspend
     *
     * @param reserveRatio 当前储备率
     * @param riskLevel    风险等级 WARNING / CRITICAL
     * @param gapAmount    缺口金额（USD）
     */
    public void notifyReserveInsufficient(BigDecimal reserveRatio,
                                          String riskLevel,
                                          BigDecimal gapAmount) {
        String url = baseUrl + "/api/issuance/suspend";
        Map<String, Object> body = new HashMap<>();
        body.put("reason", "RESERVE_INSUFFICIENT");
        body.put("riskLevel", riskLevel);
        body.put("reserveRatio", reserveRatio);
        body.put("gapAmount", gapAmount);
        body.put("source", "collateral-service");

        try {
            HttpEntity<Map<String, Object>> request = buildRequest(body);
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);
            log.info("[IssuanceClient] 通知课题1暂停发行 -> HTTP {} ratio={} level={}",
                    response.getStatusCode(), reserveRatio, riskLevel);
        } catch (Exception e) {
            // 课题1未启动时不影响课题2正常运行，仅记录警告
            log.warn("[IssuanceClient] 通知课题1暂停发行失败（课题1可能未启动）: {}", e.getMessage());
        }
    }

    /**
     * 通知课题1：储备已恢复，可以继续发行
     * 对应课题1接口：POST /api/issuance/resume
     *
     * @param reserveRatio 当前储备率
     */
    public void notifyReserveRecovered(BigDecimal reserveRatio) {
        String url = baseUrl + "/api/issuance/resume";
        Map<String, Object> body = new HashMap<>();
        body.put("reason", "RESERVE_RECOVERED");
        body.put("reserveRatio", reserveRatio);
        body.put("source", "collateral-service");

        try {
            HttpEntity<Map<String, Object>> request = buildRequest(body);
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);
            log.info("[IssuanceClient] 通知课题1恢复发行 -> HTTP {} ratio={}",
                    response.getStatusCode(), reserveRatio);
        } catch (Exception e) {
            log.warn("[IssuanceClient] 通知课题1恢复发行失败（课题1可能未启动）: {}", e.getMessage());
        }
    }

    /**
     * 查询课题1当前稳定币流通量
     * 对应课题1接口：GET /api/supply/total
     *
     * @return 流通量（USD），失败时返回 null
     */
    public BigDecimal fetchTotalSupply() {
        String url = baseUrl + "/api/supply/total";
        try {
            HttpEntity<Void> request = new HttpEntity<>(buildHeaders());
            ResponseEntity<BigDecimal> response =
                    restTemplate.exchange(url, HttpMethod.GET, request, BigDecimal.class);
            return response.getBody();
        } catch (Exception e) {
            log.warn("[IssuanceClient] 获取流通量失败: {}", e.getMessage());
            return null;
        }
    }

    // ---- private helpers ----

    private HttpEntity<Map<String, Object>> buildRequest(Map<String, Object> body) {
        return new HttpEntity<>(body, buildHeaders());
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);
        return headers;
    }
}
