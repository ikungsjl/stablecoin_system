package com.stablecoin.collateral.scheduler;

import com.stablecoin.collateral.service.ReserveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：定期执行储备验证快照
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReserveCheckScheduler {

    private final ReserveService reserveService;

    /**
     * 每60秒执行一次储备率检查
     * fixedRateString 读取 application.yml 配置，默认60秒
     */
    @Scheduled(fixedRateString = "${app.reserve.check-interval-ms:60000}")
    public void scheduledReserveCheck() {
        log.debug("[Scheduler] 执行定时储备验证...");
        try {
            reserveService.checkAndSnapshot();
        } catch (Exception e) {
            log.error("[Scheduler] 储备验证失败: {}", e.getMessage());
        }
    }
}
