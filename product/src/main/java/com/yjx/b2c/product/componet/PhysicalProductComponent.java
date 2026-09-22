package com.yjx.b2c.product.componet;

import cn.hutool.core.lang.Assert;
import com.yjx.b2c.product.enums.ProductTypeEnum;
import com.yjx.b2c.product.web.request.CreateProductRequest;
import org.springframework.stereotype.Component;

/**
 * @author yjx
 * @date 2026/9/15
 * @description 实物商品：校验库存
 */
@Component
public class PhysicalProductComponent extends CreateProductTemplate {

    @Override
    public ProductTypeEnum productType() {
        return ProductTypeEnum.PHYSICAL;
    }

    @Override
    protected void validateStock(CreateProductRequest req) {
        Assert.notNull(req.getStock(), "实物商品库存不能为空");
        Assert.isTrue(req.getStock() > 0, "实物商品库存必须大于0");
    }

    @Override
    protected void auditCompliance(CreateProductRequest req) {
        // 实物商品无额外合规要求，通用违禁词校验已在模板中完成
    }
}
