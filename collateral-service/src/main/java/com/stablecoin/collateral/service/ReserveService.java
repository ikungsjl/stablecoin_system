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

    /**
     * 供课题1调用：验证当前储备是否支持发行 stablecoinSupply 数量的稳定币。
     * 若储备充足（ratio >= 1.0），则将 stablecoinSupply 持久化到本地储备池，
     * 作为当前流通量记录，并触发快照。
     * 若储备不足，返回 available=false，不更新本地记录。
     */
    ReserveCheckResponse checkReserve(BigDecimal stablecoinSupply);

    /** 获取仪表盘数据 */
    DashboardResponse getDashboard();

    /** 获取储备率历史（折线图数据） */
    List<ReserveSnapshot> getHistory(LocalDateTime start, LocalDateTime end);

    /** 获取当前稳定币流通量（优先本地持久化值，课题1不可达时也可用） */
    BigDecimal fetchStablecoinSupply();
}
