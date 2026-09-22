package com.yjx.b2c.product.service;

/**
 * @author yjx
 * @date 2026/9/15
 * @description
 */
public interface SkuLogService {

    void saveLog(Integer skuId, Integer shopId, Integer operatorId, String action, String detail);
}
