package com.yjx.b2c.product.service;

import com.yjx.b2c.product.web.request.CreateProductRequest;

/**
 * @author yjx
 * @date 2026/9/15
 * @description
 */
public interface SkuService {

    Integer createProduct(CreateProductRequest req);
}
