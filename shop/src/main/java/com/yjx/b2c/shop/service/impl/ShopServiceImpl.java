package com.yjx.b2c.shop.service.impl;

import cn.hutool.core.lang.Assert;
import com.google.common.eventbus.EventBus;
import com.yjx.b2c.common.bean.ClientInfo;
import com.yjx.b2c.shop.dao.mapper.ShopMapper;
import com.yjx.b2c.shop.dao.po.ShopPO;
import com.yjx.b2c.shop.service.ShopService;
import com.yjx.b2c.shop.service.event.ShopLoginEvent;
import com.yjx.b2c.shop.web.request.ShopLoginRequestVO;
import com.yjx.b2c.shop.web.request.ShopRegisterRequestVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * @author yjx
 * @date 2026/9/10
 * @description
 */
@Service
public class ShopServiceImpl implements ShopService {
    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private EventBus eventBus;

    @Override
    public Integer register(ShopRegisterRequestVO vo) {
        int count = shopMapper.exist(vo.getShopName());
        Assert.equals(count, 0, "店铺名已存在");

        ShopPO shopPO = new ShopPO();
        BeanUtils.copyProperties(vo, shopPO);
        shopPO.setStatus(1);

        Assert.notBlank(shopPO.getShopName());
        Assert.notBlank(shopPO.getAdminAccount());
        Assert.notBlank(shopPO.getAdminPassword());

        int row = shopMapper.registerShop(shopPO);
        Assert.equals(row, 1, "保存到数据库异常");
        return shopPO.getId();
    }

    @Override
    public Integer login(ShopLoginRequestVO vo, ClientInfo clientInfo) {
        Assert.notBlank(vo.getAdminAccount(), "账户不能为空");
        Assert.notBlank(vo.getAdminPassword(), "账户密码不能为空");

        ShopPO shop = shopMapper.findByAccount(vo.getAdminAccount());
        Assert.notNull(shop, "账号或密码错误");
        Assert.equals(shop.getAdminPassword(), vo.getAdminPassword(), "账号或密码错误");
        Assert.equals(shop.getStatus(), 1, "店铺已被禁用");

        ShopLoginEvent event = new ShopLoginEvent();
        event.setShopId(shop.getId());
        event.setShopName(shop.getShopName());
        event.setAdminAccount(shop.getAdminAccount());
        event.setClientInfo(clientInfo);
        eventBus.post(event);
        return shop.getId();
    }
}