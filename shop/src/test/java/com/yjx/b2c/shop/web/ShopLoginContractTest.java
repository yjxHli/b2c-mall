package com.yjx.b2c.shop.web;

import com.yjx.b2c.common.bean.ClientInfo;
import com.yjx.b2c.common.exception.BizException;
import com.yjx.b2c.shop.service.ShopService;
import com.yjx.b2c.shop.support.LoginFixtures;
import com.yjx.b2c.shop.web.aspect.MethodArgNotValidExceptionAspect;
import com.yjx.b2c.shop.web.request.ShopLoginRequestVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.yjx.b2c.shop.support.LoginFixtures.ACCOUNT;
import static com.yjx.b2c.shop.support.LoginFixtures.PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 店铺登录的 HTTP 契约（F1.1 / F1.2 / F2.1 / F3.3）。
 *
 * @author yjx
 * @date 2026/10/16
 */
class ShopLoginContractTest {

    private static final String VALID_BODY = "{\"adminAccount\":\"admin\",\"adminPassword\":\"123456\"}";

    private ShopService shopService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        shopService = mock(ShopService.class);
        ShopController controller = new ShopController();
        ReflectionTestUtils.setField(controller, "shopService", shopService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new MethodArgNotValidExceptionAspect())
                .build();
    }

    @Test
    @DisplayName("1.1.1/1.1.3/1.1.4 POST /shop/login 返回 200 与身份 ID")
    void loginReturnsId() throws Exception {
        when(shopService.login(any(ShopLoginRequestVO.class), any())).thenReturn(7);

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(nullValue()))
                .andExpect(jsonPath("$.data").value(7));
    }

    @Test
    @DisplayName("1.1.1 表单编码不被接受")
    void formEncodedRejected() throws Exception {
        mockMvc.perform(post("/shop/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("adminAccount", ACCOUNT)
                        .param("adminPassword", PASSWORD))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("1.1.2/1.2.1 请求字段名与 VO 属性严格对应并原样传入 Service")
    void fieldNamesMapExactly() throws Exception {
        when(shopService.login(any(ShopLoginRequestVO.class), any())).thenReturn(7);

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());

        ArgumentCaptor<ShopLoginRequestVO> captor = ArgumentCaptor.forClass(ShopLoginRequestVO.class);
        verify(shopService).login(captor.capture(), any());
        assertThat(captor.getValue().getAdminAccount()).isEqualTo(ACCOUNT);
        assertThat(captor.getValue().getAdminPassword()).isEqualTo(PASSWORD);
    }

    @Test
    @DisplayName("1.1.2 下划线字段无法映射，落到校验失败")
    void underscoreFieldNotMapped() throws Exception {
        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"admin_account\":\"admin\",\"admin_password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400));
        verify(shopService, never()).login(any(), any());
    }

    @Test
    @DisplayName("1.1.5 仅 POST 命中登录端点")
    void onlyPostMatches() throws Exception {
        mockMvc.perform(get("/shop/login")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/shop/logins").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("1.1.6 不要求任何自定义请求头")
    void noCustomHeadersRequired() throws Exception {
        when(shopService.login(any(ShopLoginRequestVO.class), any())).thenReturn(7);

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("1.2.2/1.2.3 Controller 从请求构造 ClientInfo 并传入 Service")
    void buildsClientInfoFromRequest() throws Exception {
        when(shopService.login(any(ShopLoginRequestVO.class), any())).thenReturn(7);

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .header("X-Forwarded-For", "1.1.1.1, 2.2.2.2")
                        .header("User-Agent", LoginFixtures.USER_AGENT))
                .andExpect(status().isOk());

        ArgumentCaptor<ClientInfo> captor = ArgumentCaptor.forClass(ClientInfo.class);
        verify(shopService).login(any(ShopLoginRequestVO.class), captor.capture());
        ClientInfo info = captor.getValue();
        assertThat(info).isNotNull();
        assertThat(info.getIp()).isEqualTo("1.1.1.1");
        assertThat(info.getUserAgent()).isEqualTo(LoginFixtures.USER_AGENT);
        assertThat(info.getDevice()).isNotBlank().contains("/");
        assertThat(info.getLocation()).isNotBlank();
    }

    @Test
    @DisplayName("1.2.3 缺少 User-Agent 时设备回落为未知而非 null")
    void missingUserAgentFallsBack() throws Exception {
        when(shopService.login(any(ShopLoginRequestVO.class), any())).thenReturn(7);

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());

        ArgumentCaptor<ClientInfo> captor = ArgumentCaptor.forClass(ClientInfo.class);
        verify(shopService).login(any(ShopLoginRequestVO.class), captor.capture());
        assertThat(captor.getValue().getDevice()).isEqualTo("未知");
    }

    @Test
    @DisplayName("1.2.4 Service 返回 ID 时回填成功响应")
    void serviceIdIsReturned() throws Exception {
        when(shopService.login(any(ShopLoginRequestVO.class), any())).thenReturn(4242);

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(4242));
    }

    @Test
    @DisplayName("1.2.5 BizException 按 code 透传")
    void bizExceptionCodePreserved() throws Exception {
        when(shopService.login(any(ShopLoginRequestVO.class), any()))
                .thenThrow(BizException.badRequest("店铺名已存在"));

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("店铺名已存在"));

        when(shopService.login(any(ShopLoginRequestVO.class), any()))
                .thenThrow(BizException.serverError("服务器内部错误"));

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    @DisplayName("1.2.6 IllegalArgumentException 转为 400 且保留原消息")
    void illegalArgumentBecomesBadRequest() throws Exception {
        when(shopService.login(any(ShopLoginRequestVO.class), any()))
                .thenThrow(new IllegalArgumentException("账号或密码错误"));

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("账号或密码错误"));
    }

    @Test
    @DisplayName("1.2.7 校验失败返回 400 且不进入 Service")
    void blankFieldsRejectedBeforeService() throws Exception {
        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400));
        verify(shopService, never()).login(any(), any());
    }

    @Test
    @DisplayName("1.2.9 [红灯] 未预期异常也返回 BaseResponseVO 的 500")
    void unexpectedExceptionBecomesServerError() throws Exception {
        when(shopService.login(any(ShopLoginRequestVO.class), any()))
                .thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    @DisplayName("L6 超长账号在校验层被拒绝，不进入 BCrypt")
    void overlongAccountRejected() throws Exception {
        String tooLong = "a".repeat(101);

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adminAccount\":\"" + tooLong + "\",\"adminPassword\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400));
        verify(shopService, never()).login(any(), any());
    }

    @Test
    @DisplayName("2.1.1/2.1.5 失败响应 HTTP 200、业务码 400 且为 JSON")
    void failureUsesHttp200AndBiz400() throws Exception {
        when(shopService.login(any(ShopLoginRequestVO.class), any()))
                .thenThrow(new IllegalArgumentException("账号或密码错误"));

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("2.1.2 失败响应 data 为 null")
    void failureDataIsNull() throws Exception {
        when(shopService.login(any(ShopLoginRequestVO.class), any()))
                .thenThrow(new IllegalArgumentException("账号或密码错误"));

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("2.1.3/2.1.4 失败消息统一且不含内部细节")
    void failureMessageIsGenericAndClean() throws Exception {
        when(shopService.login(any(ShopLoginRequestVO.class), any()))
                .thenThrow(new IllegalArgumentException("账号或密码错误"));

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(jsonPath("$.message").value("账号或密码错误"))
                .andExpect(jsonPath("$.message").value(not(containsString("BCrypt"))))
                .andExpect(jsonPath("$.message").value(not(containsString("tb_"))))
                .andExpect(jsonPath("$.message").value(not(containsString("salt"))));
    }

    @Test
    @DisplayName("3.3.2 禁用提示的错误码为业务码 400")
    void disabledUsesBiz400() throws Exception {
        when(shopService.login(any(ShopLoginRequestVO.class), any()))
                .thenThrow(new IllegalArgumentException("店铺已被禁用"));

        mockMvc.perform(post("/shop/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("店铺已被禁用"));
    }
}
