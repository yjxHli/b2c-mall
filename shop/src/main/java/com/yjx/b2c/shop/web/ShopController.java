package com.yjx.b2c.shop.web;

import com.yjx.b2c.common.bean.ClientInfo;
import com.yjx.b2c.common.response.BaseResponseVO;
import com.yjx.b2c.common.util.ClientInfoUtil;
import com.yjx.b2c.shop.service.ShopService;
import com.yjx.b2c.shop.web.request.ShopLoginRequestVO;
import com.yjx.b2c.shop.web.request.ShopRegisterRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author yjx
 * @date 2026/9/10
 * @description
 */
@RestController
@RequestMapping("/shop")
public class ShopController {
    @Autowired
    public ShopService shopService;

    @PostMapping("/register")
    public BaseResponseVO<Integer> register(@RequestBody @Validated ShopRegisterRequestVO req) {
        Integer id = shopService.register(req);
        return new BaseResponseVO<>(id);
    }

    @PostMapping("/login")
    public BaseResponseVO<Integer> login(@RequestBody @Validated ShopLoginRequestVO req, HttpServletRequest request) {
        ClientInfo clientInfo = ClientInfoUtil.of(request);
        Integer id = shopService.login(req, clientInfo);
        return new BaseResponseVO<>(id);
    }
}
