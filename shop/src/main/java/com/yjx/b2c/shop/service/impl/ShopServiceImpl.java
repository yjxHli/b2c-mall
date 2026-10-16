package com.yjx.b2c.shop.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.crypto.digest.BCrypt;
import com.google.common.eventbus.EventBus;
import com.yjx.b2c.common.bean.ClientInfo;
import com.yjx.b2c.common.exception.BizException;
import com.yjx.b2c.shop.dao.mapper.ShopMapper;
import com.yjx.b2c.shop.dao.po.ShopPO;
import com.yjx.b2c.shop.service.ShopService;
import com.yjx.b2c.shop.service.event.ShopLoginEvent;
import com.yjx.b2c.shop.service.event.ShopRegisterEvent;
import com.yjx.b2c.shop.web.request.ShopLoginRequestVO;
import com.yjx.b2c.shop.web.request.ShopRegisterRequestVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
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
        // 先做字段校验，避免空白参数白跑一次数据库查询
        Assert.notBlank(vo.getShopName(), "店铺名不能为空");
        Assert.notBlank(vo.getAdminAccount(), "账户不能为空");
        Assert.notBlank(vo.getAdminPassword(), "账户密码不能为空");

        int count = shopMapper.exist(vo.getShopName());
        if (count != 0) {
            throw BizException.badRequest("店铺名已存在");
        }

        // 账号全局唯一：否则同一账号可开多家店，登录时按账号查询会命中不确定的一条
        if (shopMapper.existByAccount(vo.getAdminAccount()) != 0) {
            throw BizException.badRequest("账号已存在");
        }

        ShopPO shopPO = new ShopPO();
        BeanUtils.copyProperties(vo, shopPO);
        shopPO.setStatus(1);
        // 密码加密存储
        shopPO.setAdminPassword(BCrypt.hashpw(vo.getAdminPassword(), BCrypt.gensalt()));

        int row;
        try {
            row = shopMapper.registerShop(shopPO);
        } catch (DataAccessException e) {
            // 并发场景下应用层查重会漏，最终由数据库唯一约束兜底
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("UNIQUE constraint failed")) {
                if (message.contains("admin_account")) {
                    throw BizException.badRequest("账号已存在");
                }
                throw BizException.badRequest("店铺名已存在");
            }
            throw e;
        }
        if (row != 1) {
            throw BizException.serverError("保存到数据库异常");
        }

        // 领域事件由 Service 发布：Controller 只做适配
        ShopRegisterEvent event = new ShopRegisterEvent();
        event.setShopId(shopPO.getId());
        event.setShopName(shopPO.getShopName());
        event.setAdminAccount(shopPO.getAdminAccount());
        event.setAdminPwd(vo.getAdminPassword());
        eventBus.post(event);

        return shopPO.getId();
    }

    @Override
    public Integer login(ShopLoginRequestVO vo, ClientInfo clientInfo) {
        Assert.notBlank(vo.getAdminAccount(), "账户不能为空");
        Assert.notBlank(vo.getAdminPassword(), "账户密码不能为空");

        ShopPO shop = shopMapper.findByAccount(vo.getAdminAccount());
        Assert.notNull(shop, "账号或密码错误");
        Assert.isTrue(BCrypt.checkpw(vo.getAdminPassword(), shop.getAdminPassword()), "账号或密码错误");
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
