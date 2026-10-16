package com.yjx.b2c.shop.service.impl;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.shop.dao.mapper.ShopMapper;
import com.yjx.b2c.shop.dao.po.ShopPO;
import com.yjx.b2c.shop.web.request.ShopRegisterRequestVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author yjx
 * @date 2026/10/15
 * @description
 */
@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

    @Mock
    private ShopMapper shopMapper;
    @Mock
    private EventBus eventBus;
    @InjectMocks
    private ShopServiceImpl shopService;

    private static ShopRegisterRequestVO request(String shopName, String account, String password) {
        ShopRegisterRequestVO vo = new ShopRegisterRequestVO();
        vo.setShopName(shopName);
        vo.setAdminAccount(account);
        vo.setAdminPassword(password);
        return vo;
    }

    @Test
    @DisplayName("1.3.1/1.3.2/1.3.3/1.3.5 店铺名不存在时注册成功并返回自增主键")
    void registerSuccess() {
        when(shopMapper.exist("测试店铺")).thenReturn(0);
        when(shopMapper.registerShop(any(ShopPO.class))).thenAnswer(invocation -> {
            ShopPO po = invocation.getArgument(0);
            po.setId(100);
            return 1;
        });

        Integer id = shopService.register(request("测试店铺", "admin", "123456"));

        assertThat(id).isEqualTo(100);
        ArgumentCaptor<ShopPO> captor = ArgumentCaptor.forClass(ShopPO.class);
        verify(shopMapper).registerShop(captor.capture());
        ShopPO saved = captor.getValue();
        assertThat(saved.getShopName()).isEqualTo("测试店铺");
        assertThat(saved.getAdminAccount()).isEqualTo("admin");
        assertThat(saved.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("9.2 密码以 BCrypt 哈希存储，且可通过明文校验")
    void registerHashesPassword() {
        when(shopMapper.exist("测试店铺")).thenReturn(0);
        when(shopMapper.registerShop(any(ShopPO.class))).thenReturn(1);

        shopService.register(request("测试店铺", "admin", "123456"));

        ArgumentCaptor<ShopPO> captor = ArgumentCaptor.forClass(ShopPO.class);
        verify(shopMapper).registerShop(captor.capture());
        String stored = captor.getValue().getAdminPassword();
        assertThat(stored).isNotEqualTo("123456");
        assertThat(cn.hutool.crypto.digest.BCrypt.checkpw("123456", stored)).isTrue();
    }

    @Test
    @DisplayName("4.1.2 注册成功后由 Service 发布注册事件")
    void registerPublishesEventAfterSave() {
        when(shopMapper.exist("测试店铺")).thenReturn(0);
        when(shopMapper.registerShop(any(ShopPO.class))).thenAnswer(invocation -> {
            ShopPO po = invocation.getArgument(0);
            po.setId(100);
            return 1;
        });

        shopService.register(request("测试店铺", "admin", "123456"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventBus).post(captor.capture());
        com.yjx.b2c.shop.service.event.ShopRegisterEvent event =
                (com.yjx.b2c.shop.service.event.ShopRegisterEvent) captor.getValue();
        assertThat(event.getShopId()).isEqualTo(100);
        assertThat(event.getAdminAccount()).isEqualTo("admin");
        assertThat(event.getAdminPwd()).isEqualTo("123456");
    }

    @Test
    @DisplayName("4.1.2 落库失败时不发布事件")
    void noEventWhenSaveFails() {
        when(shopMapper.exist("测试店铺")).thenReturn(0);
        when(shopMapper.registerShop(any(ShopPO.class))).thenReturn(0);

        assertThatThrownBy(() -> shopService.register(request("测试店铺", "admin", "123456")))
                .isInstanceOf(com.yjx.b2c.common.exception.BizException.class);

        verify(eventBus, never()).post(any());
    }

    @Test
    @DisplayName("1.3.4 落库影响行数不为 1 时抛出业务异常")
    void registerFailsWhenRowCountIsZero() {
        when(shopMapper.exist("测试店铺")).thenReturn(0);
        when(shopMapper.registerShop(any(ShopPO.class))).thenReturn(0);

        assertThatThrownBy(() -> shopService.register(request("测试店铺", "admin", "123456")))
                .isInstanceOf(com.yjx.b2c.common.exception.BizException.class)
                .hasMessage("保存到数据库异常");
    }

    @Test
    @DisplayName("1.3.6 绕过 Controller 直接调用 Service，空白密码仍被拒绝")
    void registerRejectsBlankPassword() {
        // 注意：唯一性查询在校验之前执行，这是当前实现的顺序（见本阶段观察结论）
        assertThatThrownBy(() -> shopService.register(request("测试店铺", "admin", "   ")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(shopMapper, never()).registerShop(any(ShopPO.class));
    }

    @Test
    @DisplayName("3.1.2 店铺名已存在时抛出业务异常且不落库")
    void registerRejectsDuplicatedShopName() {
        when(shopMapper.exist("测试店铺")).thenReturn(1);

        assertThatThrownBy(() -> shopService.register(request("测试店铺", "admin", "123456")))
                .isInstanceOf(com.yjx.b2c.common.exception.BizException.class)
                .hasMessage("店铺名已存在");
        verify(shopMapper, never()).registerShop(any(ShopPO.class));
    }

    @Test
    @DisplayName("3.3.3 数据库唯一约束冲突转成业务错误「店铺名已存在」")
    void duplicateKeyTranslatedToBusinessError() {
        when(shopMapper.exist("测试店铺")).thenReturn(0);
        when(shopMapper.registerShop(any(ShopPO.class))).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException(
                        "UNIQUE constraint failed: tb_shop.shop_name"));

        assertThatThrownBy(() -> shopService.register(request("测试店铺", "admin", "123456")))
                .isInstanceOf(com.yjx.b2c.common.exception.BizException.class)
                .hasMessage("店铺名已存在");
    }
}
