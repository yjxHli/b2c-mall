package com.yjx.b2c.product.service.impl;

import cn.hutool.core.lang.Assert;
import com.yjx.b2c.product.componet.CreateProductTemplate;
import com.yjx.b2c.product.enums.ProductTypeEnum;
import com.yjx.b2c.product.service.SkuService;
import com.yjx.b2c.product.web.request.CreateProductRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author yjx
 * @date 2026/9/18
 * @description 根据不同商品类型选择对应的新建商品模板
 */
@Service
public class SkuServiceImpl implements SkuService {
    private final Map<ProductTypeEnum, CreateProductTemplate> templateMap = new EnumMap<>(ProductTypeEnum.class);

    @Autowired
    public SkuServiceImpl(List<CreateProductTemplate> templates) {
        for (CreateProductTemplate template : templates) {
            templateMap.put(template.productType(), template);
        }
    }

    @Override
    public Integer createProduct(CreateProductRequest req) {
        ProductTypeEnum type = ProductTypeEnum.of(req == null ? null : req.getProductType());
        Assert.notNull(type, "不支持的商品类型");
        CreateProductTemplate template = templateMap.get(type);
        Assert.notNull(template, "不支持的商品类型");
        return template.createProduct(req);
    }
}
