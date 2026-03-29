package com.stablecoin.collateral.service;

import com.stablecoin.collateral.dto.ReserveAssetResponse;
import java.util.List;

/**
 * 储备资产服务接口
 */
public interface ReserveAssetService {

    /**
     * 获取完整的资产组合（包含动态计算的占比）
     */
    List<ReserveAssetResponse> getPortfolio();

    /**
     * 根据资产类型获取单个资产
     */
    ReserveAssetResponse getAssetByType(String assetType);
}
