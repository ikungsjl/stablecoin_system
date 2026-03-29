package com.stablecoin.collateral.service;

import com.stablecoin.collateral.dto.DashboardResponse;
import com.stablecoin.collateral.dto.ReserveCheckResponse;
import com.stablecoin.collateral.entity.ReserveSnapshot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ReserveService {

    /** 执行一次储备验证，生成快照，触发风险判断 */
    ReserveSnapshot checkAndSnapshot();

    /** 供课题1调用：验证当前储备是否支持继续发行 */
    ReserveCheckResponse checkReserve(BigDecimal stablecoinSupply);

    /** 获取仪表盘数据 */
    DashboardResponse getDashboard();

    /** 获取储备率历史（折线图数据） */
    List<ReserveSnapshot> getHistory(LocalDateTime start, LocalDateTime end);

    /** 获取当前稳定币流通量（调用课题1 API 或缓存） */
    BigDecimal fetchStablecoinSupply();
}
