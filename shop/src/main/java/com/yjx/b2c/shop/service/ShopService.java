package com.yjx.b2c.shop.service;

import com.yjx.b2c.common.bean.ClientInfo;
import com.yjx.b2c.shop.web.request.ShopLoginRequestVO;
import com.yjx.b2c.shop.web.request.ShopRegisterRequestVO;

/**
 * @author yjx
 * @date 2026/9/10
 * @description
 */
public interface ShopService {
    public Integer register(ShopRegisterRequestVO vo);

    Integer login(ShopLoginRequestVO vo, ClientInfo clientInfo);
}