package com.yjx.b2c.employee.web;

import com.yjx.b2c.common.bean.ClientInfo;
import com.yjx.b2c.common.exception.BizException;
import com.yjx.b2c.employee.service.EmployeeService;
import com.yjx.b2c.employee.support.LoginFixtures;
import com.yjx.b2c.employee.web.aspect.GlobalExceptionHandler;
import com.yjx.b2c.employee.web.request.LoginRequestVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.yjx.b2c.employee.support.LoginFixtures.PASSWORD;
import static com.yjx.b2c.employee.support.LoginFixtures.SHOP_ID;
import static com.yjx.b2c.employee.support.LoginFixtures.USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
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
 * 员工登录的 HTTP 契约（F1.1 / F1.2 / F2.1 / F3.3 / F6.2）。
 *
 * @author yjx
 * @date 2026/10/16
 */
class EmployeeLoginContractTest {

    private static final String VALID_BODY =
            "{\"username\":\"admin\",\"password\":\"123456\",\"shopId\":88}";

    private EmployeeService employeeService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        employeeService = mock(EmployeeService.class);
        EmployeeController controller = new EmployeeController();
        ReflectionTestUtils.setField(controller, "employeeService", employeeService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("1.1.1/1.1.3/1.1.4 POST /employee/login 返回 200 与身份 ID")
    void loginReturnsId() throws Exception {
        when(employeeService.login(any(LoginRequestVO.class), any())).thenReturn(7);

        mockMvc.perform(post("/employee/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(7));
    }

    @Test
    @DisplayName("1.1.1 表单编码不被接受")
    void formEncodedRejected() throws Exception {
        mockMvc.perform(post("/employee/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", USERNAME))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("1.1.2/1.2.1 字段名严格对应并原样传入 Service")
    void fieldNamesMapExactly() throws Exception {
        when(employeeService.login(any(LoginRequestVO.class), any())).thenReturn(7);

        mockMvc.perform(post("/employee/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());

        ArgumentCaptor<LoginRequestVO> captor = ArgumentCaptor.forClass(LoginRequestVO.class);
        verify(employeeService).login(captor.capture(), any());
        assertThat(captor.getValue().getUsername()).isEqualTo(USERNAME);
        assertThat(captor.getValue().getPassword()).isEqualTo(PASSWORD);
        assertThat(captor.getValue().getShopId()).isEqualTo(SHOP_ID);
    }

    @Test
    @DisplayName("1.1.5 仅 POST 命中登录端点")
    void onlyPostMatches() throws Exception {
        mockMvc.perform(get("/employee/login")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/employee/logins").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("1.2.2/1.2.3 Controller 构造 ClientInfo 并传入 Service")
    void buildsClientInfo() throws Exception {
        when(employeeService.login(any(LoginRequestVO.class), any())).thenReturn(7);

        mockMvc.perform(post("/employee/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .header("X-Forwarded-For", "1.1.1.1, 2.2.2.2")
                        .header("User-Agent", LoginFixtures.USER_AGENT))
                .andExpect(status().isOk());

        ArgumentCaptor<ClientInfo> captor = ArgumentCaptor.forClass(ClientInfo.class);
        verify(employeeService).login(any(LoginRequestVO.class), captor.capture());
        assertThat(captor.getValue().getIp()).isEqualTo("1.1.1.1");
        assertThat(captor.getValue().getUserAgent()).isEqualTo(LoginFixtures.USER_AGENT);
        assertThat(captor.getValue().getDevice()).isNotBlank().contains("/");
    }

    @Test
    @DisplayName("1.2.5 BizException 按 code 透传")
    void bizExceptionCodePreserved() throws Exception {
        when(employeeService.login(any(LoginRequestVO.class), any()))
                .thenThrow(BizException.serverError("保存员工信息失败"));

        mockMvc.perform(post("/employee/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    @DisplayName("1.2.6 IllegalArgumentException 转为 400 且保留消息")
    void illegalArgumentBecomesBadRequest() throws Exception {
        when(employeeService.login(any(LoginRequestVO.class), any()))
                .thenThrow(new IllegalArgumentException("账号或密码错误"));

        mockMvc.perform(post("/employee/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("账号或密码错误"));
    }

    @Test
    @DisplayName("1.2.8 [红灯] 校验异常返回 BaseResponseVO 的 400")
    void validationFailureBecomesBiz400() throws Exception {
        mockMvc.perform(post("/employee/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400));
        verify(employeeService, never()).login(any(), any());
    }

    @Test
    @DisplayName("1.2.9 [红灯] 未预期异常返回 BaseResponseVO 的 500")
    void unexpectedExceptionBecomesServerError() throws Exception {
        when(employeeService.login(any(LoginRequestVO.class), any()))
                .thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/employee/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    @DisplayName("6.2.1 [红灯] 缺少 shopId 时请求被拒绝")
    void missingShopIdRejected() throws Exception {
        mockMvc.perform(post("/employee/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(jsonPath("$.status").value(400));
        verify(employeeService, never()).login(any(), any());
    }

    @Test
    @DisplayName("L6 超长账号在校验层被拒绝")
    void overlongUsernameRejected() throws Exception {
        String tooLong = "a".repeat(51);

        mockMvc.perform(post("/employee/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + tooLong + "\",\"password\":\"123456\",\"shopId\":88}"))
                .andExpect(jsonPath("$.status").value(400));
        verify(employeeService, never()).login(any(), any());
    }

    @Test
    @DisplayName("2.1.1/2.1.2/2.1.5 失败响应 HTTP 200 + 业务码 400 + data 为 null")
    void failureUsesHttp200AndBiz400() throws Exception {
        when(employeeService.login(any(LoginRequestVO.class), any()))
                .thenThrow(new IllegalArgumentException("账号或密码错误"));

        mockMvc.perform(post("/employee/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @DisplayName("2.1.3/2.1.4 失败消息统一且不含内部细节")
    void failureMessageIsGeneric() throws Exception {
        when(employeeService.login(any(LoginRequestVO.class), any()))
                .thenThrow(new IllegalArgumentException("账号或密码错误"));

        mockMvc.perform(post("/employee/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(jsonPath("$.message").value("账号或密码错误"))
                .andExpect(jsonPath("$.message").value(not(containsString("BCrypt"))))
                .andExpect(jsonPath("$.message").value(not(containsString("tb_"))));
    }

    @Test
    @DisplayName("3.3.2 禁用提示的错误码为业务码 400")
    void disabledUsesBiz400() throws Exception {
        when(employeeService.login(any(LoginRequestVO.class), any()))
                .thenThrow(new IllegalArgumentException("账号已被禁用"));

        mockMvc.perform(post("/employee/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("账号已被禁用"));
    }
}
