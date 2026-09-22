package com.yjx.b2c.product.web.controller;

import com.yjx.b2c.common.response.BaseResponseVO;
import com.yjx.b2c.product.service.SkuService;
import com.yjx.b2c.product.web.request.CreateProductRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yjx
 * @date 2026/9/15
 * @description
 */
@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private SkuService skuService;

    @PostMapping("/create")
    public BaseResponseVO<Integer> create(@RequestBody @Validated CreateProductRequest req) {
        return new BaseResponseVO<>(skuService.createProduct(req));
    }
}
