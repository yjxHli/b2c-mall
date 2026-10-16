package com.yjx.b2c.shop.service.impl;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.common.exception.BizException;
import com.yjx.b2c.shop.dao.mapper.ShopMapper;
import com.yjx.b2c.shop.dao.po.ShopPO;
import com.yjx.b2c.shop.web.request.ShopRegisterRequestVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 店铺账号唯一性对登录的前提保障（F6.1）。
 *
 * @author yjx
 * @date 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
class ShopRegisterAccountUniquenessTest {

    @Mock
    private ShopMapper shopMapper;
    @Mock
    private EventBus eventBus;
    @InjectMocks
    private ShopServiceImpl shopService;

    private static ShopRegisterRequestVO request(String shopName, String account) {
        ShopRegisterRequestVO vo = new ShopRegisterRequestVO();
        vo.setShopName(shopName);
        vo.setAdminAccount(account);
        vo.setAdminPassword("123456");
        return vo;
    }

    @Test
    @DisplayName("6.1.3 [红灯] 注册拒绝已存在的管理员账号")
    void registerRejectsDuplicateAdminAccount() {
        when(shopMapper.exist("新店铺")).thenReturn(0);
        when(shopMapper.existByAccount("admin")).thenReturn(1);

        assertThatThrownBy(() -> shopService.register(request("新店铺", "admin")))
                .isInstanceOf(BizException.class)
                .hasMessage("账号已存在");
        verify(shopMapper, never()).registerShop(any(ShopPO.class));
    }

    @Test
    @DisplayName("6.1.4 [红灯] 账号唯一约束冲突翻译为业务码 400")
    void accountUniqueViolationTranslatedToBadRequest() {
        when(shopMapper.exist("新店铺")).thenReturn(0);
        when(shopMapper.existByAccount("admin")).thenReturn(0);
        when(shopMapper.registerShop(any(ShopPO.class))).thenThrow(
                new DataIntegrityViolationException("UNIQUE constraint failed: tb_shop.admin_account"));

        assertThatThrownBy(() -> shopService.register(request("新店铺", "admin")))
                .isInstanceOf(BizException.class)
                .hasMessage("账号已存在");
    }
}
