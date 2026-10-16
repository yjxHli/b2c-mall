package com.yjx.b2c.shop.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yjx.b2c.shop.service.ShopService;
import com.yjx.b2c.shop.web.aspect.MethodArgNotValidExceptionAspect;
import com.yjx.b2c.shop.web.request.ShopRegisterRequestVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 注册入参校验契约测试（任务项 2.1.x / 2.2.x）。
 */
class ShopRegisterValidationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ShopService shopService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        shopService = mock(ShopService.class);

        ShopController controller = new ShopController();
        ReflectionTestUtils.setField(controller, "shopService", shopService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new MethodArgNotValidExceptionAspect())
                .setValidator(validator)
                .build();
    }

    private String body(String shopName, String account, String password) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("shopName", shopName);
        map.put("adminAccount", account);
        map.put("adminPassword", password);
        return objectMapper.writeValueAsString(map);
    }

    private void expectRejected(String json, String expectedMessagePart) throws Exception {
        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString(expectedMessagePart)));
    }

    @Test
    @DisplayName("2.1.1 店铺名为 null / 空串 / 纯空白都返回 400")
    void blankShopNameRejected() throws Exception {
        for (String bad : new String[]{null, "", "   "}) {
            expectRejected(body(bad, "admin", "123456"), "店铺名不能为空");
        }
    }

    @Test
    @DisplayName("2.1.2 管理员账号为空返回 400")
    void blankAccountRejected() throws Exception {
        expectRejected(body("测试店铺", "  ", "123456"), "账户不能为空");
    }

    @Test
    @DisplayName("2.1.3 管理员密码为空返回 400")
    void blankPasswordRejected() throws Exception {
        expectRejected(body("测试店铺", "admin", ""), "账户密码");
    }

    @Test
    @DisplayName("2.1.4 店铺名长度上界为 6：6 通过、7 拒绝")
    void shopNameLengthBoundary() throws Exception {
        when(shopService.register(any(ShopRegisterRequestVO.class))).thenReturn(100);

        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("一二三四五六", "admin", "123456")))
                .andExpect(jsonPath("$.status").value(200));

        expectRejected(body("一二三四五六七", "admin", "123456"), "店铺名过长");
    }

    @Test
    @DisplayName("2.1.5 管理员账号超长应被拒绝")
    void accountTooLongRejected() throws Exception {
        expectRejected(body("测试店铺", "a".repeat(101), "123456"), "账户");
    }

    @Test
    @DisplayName("2.1.6 密码少于 6 位应被拒绝")
    void passwordTooShortRejected() throws Exception {
        expectRejected(body("测试店铺", "admin", "12345"), "密码");
    }

    @Test
    @DisplayName("2.2.2 多个字段同时非法时聚合全部提示")
    void multipleErrorsAggregated() throws Exception {
        expectRejected(body("", "", ""), "店铺名不能为空");
        expectRejected(body("", "", ""), "账户不能为空");
    }

    @Test
    @DisplayName("2.2.4 校验失败时不得调用 Service")
    void noServiceCallWhenInvalid() throws Exception {
        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "admin", "123456")))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(shopService);
    }
}
