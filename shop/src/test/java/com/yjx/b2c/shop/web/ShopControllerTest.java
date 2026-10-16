package com.yjx.b2c.shop.web;

import com.yjx.b2c.shop.service.ShopService;
import com.yjx.b2c.shop.web.aspect.MethodArgNotValidExceptionAspect;
import com.yjx.b2c.shop.web.request.ShopRegisterRequestVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 注册接口契约测试（任务项 1.1.x / 1.2.x）。
 * 事件发布职责已在阶段 9.3 下沉到 Service，事件断言见 ShopServiceImplTest。
 */
class ShopControllerTest {

    private static final String VALID_BODY =
            "{\"shopName\":\"测试店铺\",\"adminAccount\":\"admin\",\"adminPassword\":\"123456\"}";

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

    @Test
    @DisplayName("1.1.1/1.2.1 POST /shop/register 接受 JSON 并返回业务码 200")
    void registerReturnsOk() throws Exception {
        when(shopService.register(any(ShopRegisterRequestVO.class))).thenReturn(100);

        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("1.2.1 非 POST 方法访问注册路径返回 405")
    void getRegisterIsMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/shop/register")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("1.1.1 未声明 Content-Type 时不得返回业务成功码")
    void missingContentTypeIsRejected() throws Exception {
        mockMvc.perform(post("/shop/register").content(VALID_BODY))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("1.2.2 请求字段完整映射到 VO 并传递给 Service")
    void requestIsDeserializedIntoVo() throws Exception {
        when(shopService.register(any(ShopRegisterRequestVO.class))).thenReturn(100);

        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());

        ArgumentCaptor<ShopRegisterRequestVO> captor = ArgumentCaptor.forClass(ShopRegisterRequestVO.class);
        verify(shopService).register(captor.capture());
        ShopRegisterRequestVO vo = captor.getValue();
        assertThat(vo.getShopName()).isEqualTo("测试店铺");
        assertThat(vo.getAdminAccount()).isEqualTo("admin");
        assertThat(vo.getAdminPassword()).isEqualTo("123456");
    }

    @Test
    @DisplayName("1.2.4 Service 抛业务异常时响应业务码 400 且保留原始消息")
    void serviceFailureBecomesBadRequest() throws Exception {
        when(shopService.register(any(ShopRegisterRequestVO.class)))
                .thenThrow(new IllegalArgumentException("店铺名已存在"));

        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("店铺名已存在"));
    }

    @Test
    @DisplayName("1.2.6 落库失败时响应 400")
    void saveFailureBecomesBadRequest() throws Exception {
        when(shopService.register(any(ShopRegisterRequestVO.class)))
                .thenThrow(new IllegalArgumentException("保存到数据库异常"));

        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("9.1 业务异常携带错误码，直接映射到响应 status")
    void bizExceptionCarriesCode() throws Exception {
        when(shopService.register(any(ShopRegisterRequestVO.class)))
                .thenThrow(new com.yjx.b2c.common.exception.BizException(500, "保存到数据库异常"));

        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("保存到数据库异常"));
    }

    @Test
    @DisplayName("1.1.4 成功响应回传新建店铺 ID")
    void registerReturnsShopId() throws Exception {
        when(shopService.register(any(ShopRegisterRequestVO.class))).thenReturn(100);

        mockMvc.perform(post("/shop/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(jsonPath("$.data").value(100));
    }
}
