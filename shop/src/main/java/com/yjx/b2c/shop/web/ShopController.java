package com.yjx.b2c.shop.web;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.common.bean.ClientInfo;
import com.yjx.b2c.common.response.BaseResponseVO;
import com.yjx.b2c.common.util.ClientInfoUtil;
import com.yjx.b2c.shop.dao.po.ShopPO;
import com.yjx.b2c.shop.service.ShopService;
import com.yjx.b2c.shop.service.event.ShopRegisterEvent;
import com.yjx.b2c.shop.web.request.ShopLoginRequestVO;
import com.yjx.b2c.shop.web.request.ShopRegisterRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
    @Autowired
    public EventBus eventBus;

    @PostMapping("/register")
    public BaseResponseVO<?> register(@RequestBody @Validated ShopRegisterRequestVO req) {
        Integer id = shopService.register(req);

        ShopRegisterEvent event = new ShopRegisterEvent();
        event.setShopId(id);
        event.setShopName(req.getShopName());
        event.setAdminAccount(req.getAdminAccount());
        event.setAdminPwd(req.getAdminPassword());
        eventBus.post(event);
        return new BaseResponseVO<>();
    }

    @PostMapping("/login")
    public BaseResponseVO<Integer> login(@RequestBody @Validated ShopLoginRequestVO req, HttpServletRequest request) {
        ClientInfo clientInfo = ClientInfoUtil.of(request);
        Integer id = shopService.login(req, clientInfo);
        return new BaseResponseVO<>(id);
    }
}
