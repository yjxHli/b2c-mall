package com.yjx.b2c.product.componet;

import cn.hutool.core.lang.Assert;
import com.yjx.b2c.product.enums.ProductTypeEnum;
import com.yjx.b2c.product.web.request.CreateProductRequest;
import org.springframework.stereotype.Component;

/**
 * @author yjx
 * @date 2026/9/15
 * @description 虚拟商品：校验合规
 */
@Component
public class VirtualProductComponent extends CreateProductTemplate {

    @Override
    public ProductTypeEnum productType() {
        return ProductTypeEnum.VIRTUAL;
    }

    @Override
    protected void validateStock(CreateProductRequest req) {
        // 虚拟商品库存不限，不做库存校验
    }

    @Override
    protected void auditCompliance(CreateProductRequest req) {
        Assert.notBlank(req.getDeliveryContent(), "虚拟商品必须填写交付内容");
        Assert.isFalse(containsForbiddenWord(req.getDeliveryContent()), "交付内容包含违禁词");
    }
}
