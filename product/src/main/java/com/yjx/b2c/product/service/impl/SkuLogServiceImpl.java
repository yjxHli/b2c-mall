package com.yjx.b2c.product.service.impl;

import cn.hutool.core.lang.Assert;
import com.yjx.b2c.product.dao.mapper.SkuLogMapper;
import com.yjx.b2c.product.dao.po.SkuLogPO;
import com.yjx.b2c.product.service.SkuLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yjx
 * @date 2026/9/18
 * @description
 */
@Service
public class SkuLogServiceImpl implements SkuLogService {
    @Autowired
    private SkuLogMapper skuLogMapper;

    @Override
    public void saveLog(Integer skuId, Integer shopId, Integer operatorId, String action, String detail) {
        SkuLogPO po = new SkuLogPO();
        po.setSkuId(skuId);
        po.setShopId(shopId);
        po.setOperatorId(operatorId);
        po.setAction(action);
        po.setDetail(detail);

        Integer row = skuLogMapper.save(po);
        Assert.equals(row, 1, "保存商品操作日志失败");
    }
}
