package com.yjx.b2c.shop.service.impl;

import com.google.common.eventbus.EventBus;
import com.yjx.b2c.shop.dao.mapper.ShopMapper;
import com.yjx.b2c.shop.dao.po.ShopPO;
import com.yjx.b2c.shop.service.event.ShopLoginEvent;
import com.yjx.b2c.shop.support.LoginFixtures;
import com.yjx.b2c.shop.web.request.ShopLoginRequestVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.yjx.b2c.shop.support.LoginFixtures.ACCOUNT;
import static com.yjx.b2c.shop.support.LoginFixtures.PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 店铺管理员登录的 Service 层行为（F1.3 / F2.2 / F3.1）。
 *
 * @author yjx
 * @date 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
class ShopLoginServiceImplTest {

    @Mock
    private ShopMapper shopMapper;
    @Mock
    private EventBus eventBus;
    @InjectMocks
    private ShopServiceImpl shopService;

    private ShopLoginRequestVO request(String account, String password) {
        return LoginFixtures.loginRequest(account, password);
    }

    @Test
    @DisplayName("1.3.1 空白账号被拒绝且不查询数据库")
    void blankAccountRejectedWithoutQuery() {
        for (String blank : new String[]{null, "", "   "}) {
            assertThatThrownBy(() -> shopService.login(request(blank, PASSWORD), LoginFixtures.clientInfo()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verify(shopMapper, never()).findByAccount(any());
    }

    @Test
    @DisplayName("1.3.2 空白密码被拒绝且不查询数据库")
    void blankPasswordRejectedWithoutQuery() {
        for (String blank : new String[]{null, "", "   "}) {
            assertThatThrownBy(() -> shopService.login(request(ACCOUNT, blank), LoginFixtures.clientInfo()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verify(shopMapper, never()).findByAccount(any());
    }

    @Test
    @DisplayName("1.3.3 使用请求中的账号查询唯一记录")
    void loginQueriesByAccount() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(1, ACCOUNT, PASSWORD, 1));

        shopService.login(request(ACCOUNT, PASSWORD), LoginFixtures.clientInfo());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(shopMapper).findByAccount(captor.capture());
        assertThat(captor.getValue()).isEqualTo(ACCOUNT);
    }

    @Test
    @DisplayName("1.3.4 用 BCrypt 比对明文密码与库中哈希")
    void loginComparesBcryptHash() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(1, ACCOUNT, PASSWORD, 1));

        assertThat(shopService.login(request(ACCOUNT, PASSWORD), LoginFixtures.clientInfo())).isEqualTo(1);
    }

    @Test
    @DisplayName("1.3.5 密码比对大小写敏感，不做归一化")
    void passwordComparisonIsCaseSensitive() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(1, ACCOUNT, "Abc123", 1));

        assertThatThrownBy(() -> shopService.login(request(ACCOUNT, "abc123"), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("账号或密码错误");
    }

    @Test
    @DisplayName("1.3.6 [红灯] 库中非 BCrypt 口令不泄露加密内部错误")
    void nonBcryptStoredPasswordFailsAsCredentialError() {
        ShopPO legacy = new ShopPO();
        legacy.setId(1);
        legacy.setAdminAccount(ACCOUNT);
        legacy.setAdminPassword("123456");
        legacy.setStatus(1);
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(legacy);

        assertThatThrownBy(() -> shopService.login(request(ACCOUNT, PASSWORD), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("账号或密码错误");
    }

    @Test
    @DisplayName("1.3.7 校验全部通过后才发布登录事件")
    void publishesLoginEventAfterValidation() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(1, ACCOUNT, PASSWORD, 1));

        shopService.login(request(ACCOUNT, PASSWORD), LoginFixtures.clientInfo());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventBus).post(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ShopLoginEvent.class);
    }

    @Test
    @DisplayName("1.3.8 事件字段完整且与库中记录一致")
    void eventCarriesShopIdentityAndClientInfo() {
        ShopPO po = LoginFixtures.shopPO(9, ACCOUNT, PASSWORD, 1);
        po.setShopName("旗舰店");
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(po);

        shopService.login(request(ACCOUNT, PASSWORD), LoginFixtures.clientInfo());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventBus).post(captor.capture());
        ShopLoginEvent event = (ShopLoginEvent) captor.getValue();
        assertThat(event.getShopId()).isEqualTo(9);
        assertThat(event.getShopName()).isEqualTo("旗舰店");
        assertThat(event.getAdminAccount()).isEqualTo(ACCOUNT);
        assertThat(event.getClientInfo()).isEqualTo(LoginFixtures.clientInfo());
    }

    @Test
    @DisplayName("1.3.9 clientInfo 为 null 时 Service 仍返回成功")
    void nullClientInfoDoesNotBreakService() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(1, ACCOUNT, PASSWORD, 1));

        assertThat(shopService.login(request(ACCOUNT, PASSWORD), null)).isEqualTo(1);
        verify(eventBus).post(any());
    }

    @Test
    @DisplayName("1.3.10 查询异常向上传播，不被吞成账号不存在")
    void mapperExceptionPropagates() {
        RuntimeException boom = new RuntimeException("db down");
        when(shopMapper.findByAccount(ACCOUNT)).thenThrow(boom);

        assertThatThrownBy(() -> shopService.login(request(ACCOUNT, PASSWORD), LoginFixtures.clientInfo()))
                .isSameAs(boom);
    }

    @Test
    @DisplayName("1.3.11 返回库中主键，而非入参或事件副本")
    void returnsPersistedId() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(77, ACCOUNT, PASSWORD, 1));

        assertThat(shopService.login(request(ACCOUNT, PASSWORD), LoginFixtures.clientInfo())).isEqualTo(77);
    }

    @Test
    @DisplayName("2.2.1 账号不存在时提示统一且不发布事件")
    void unknownAccountRejectedWithGenericMessage() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(null);

        assertThatThrownBy(() -> shopService.login(request(ACCOUNT, PASSWORD), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("账号或密码错误");
        verify(eventBus, never()).post(any());
    }

    @Test
    @DisplayName("2.2.2 密码错误时提示统一且不发布事件")
    void wrongPasswordRejectedWithGenericMessage() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(1, ACCOUNT, PASSWORD, 1));

        assertThatThrownBy(() -> shopService.login(request(ACCOUNT, "wrong-pass"), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("账号或密码错误");
        verify(eventBus, never()).post(any());
    }

    @Test
    @DisplayName("2.2.3 账号不存在与密码错误的响应完全不可区分")
    void unknownAccountAndWrongPasswordAreIndistinguishable() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(null);
        String unknown = failureMessage(request(ACCOUNT, PASSWORD));

        org.mockito.Mockito.reset(shopMapper);
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(1, ACCOUNT, PASSWORD, 1));
        String wrongPassword = failureMessage(request(ACCOUNT, "wrong-pass"));

        assertThat(unknown).isEqualTo(wrongPassword).isEqualTo("账号或密码错误");
    }

    private String failureMessage(ShopLoginRequestVO vo) {
        try {
            shopService.login(vo, LoginFixtures.clientInfo());
            return "<no-exception>";
        } catch (RuntimeException ex) {
            return ex.getMessage();
        }
    }

    @Test
    @DisplayName("2.2.4 失败路径不产生任何写副作用")
    void failedLoginHasNoWriteSideEffects() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(1, ACCOUNT, PASSWORD, 1));

        assertThatThrownBy(() -> shopService.login(request(ACCOUNT, "wrong-pass"), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(shopMapper, never()).updateLoginStatus(any());
        verify(eventBus, never()).post(any());
    }

    @Test
    @DisplayName("2.2.5 空密码走空白校验语义")
    void emptyPasswordUsesBlankSemantics() {
        assertThatThrownBy(() -> shopService.login(request(ACCOUNT, ""), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("账户密码不能为空");
        verify(shopMapper, never()).findByAccount(any());
    }

    @Test
    @DisplayName("2.2.6 [红灯] 明文口令不出现在 VO 的 toString 中")
    void toStringDoesNotExposePassword() {
        assertThat(request(ACCOUNT, PASSWORD).toString()).doesNotContain(PASSWORD);
    }

    @Test
    @DisplayName("3.1.1 status=0 且密码正确时拒绝登录")
    void disabledShopRejected() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(1, ACCOUNT, PASSWORD, 0));

        assertThatThrownBy(() -> shopService.login(request(ACCOUNT, PASSWORD), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("店铺已被禁用");
    }

    @Test
    @DisplayName("3.1.2 status=1 时允许登录")
    void enabledShopAllowed() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(5, ACCOUNT, PASSWORD, 1));

        assertThat(shopService.login(request(ACCOUNT, PASSWORD), LoginFixtures.clientInfo())).isEqualTo(5);
    }

    @Test
    @DisplayName("3.1.3 禁用账号不发布登录事件")
    void disabledShopPublishesNoEvent() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(1, ACCOUNT, PASSWORD, 0));

        assertThatThrownBy(() -> shopService.login(request(ACCOUNT, PASSWORD), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(eventBus, never()).post(any());
    }

    @Test
    @DisplayName("3.1.4 状态判定在密码校验之后")
    void disabledCheckRunsAfterPasswordCheck() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(1, ACCOUNT, PASSWORD, 0));

        assertThatThrownBy(() -> shopService.login(request(ACCOUNT, "wrong-pass"), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("账号或密码错误");
    }

    @Test
    @DisplayName("3.1.5 status 为 null 时按拒绝处理")
    void nullStatusRejected() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(1, ACCOUNT, PASSWORD, null));

        assertThatThrownBy(() -> shopService.login(request(ACCOUNT, PASSWORD), LoginFixtures.clientInfo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("店铺已被禁用");
    }

    @Test
    @DisplayName("3.1.6 禁用文案为店铺专属")
    void disabledMessageIsShopSpecific() {
        when(shopMapper.findByAccount(ACCOUNT)).thenReturn(LoginFixtures.shopPO(1, ACCOUNT, PASSWORD, 0));

        assertThatThrownBy(() -> shopService.login(request(ACCOUNT, PASSWORD), LoginFixtures.clientInfo()))
                .hasMessage("店铺已被禁用");
    }
}
