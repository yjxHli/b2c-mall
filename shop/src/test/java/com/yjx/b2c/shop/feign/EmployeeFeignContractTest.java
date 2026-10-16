package com.yjx.b2c.shop.feign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yjx.b2c.common.response.BaseResponseVO;
import com.yjx.b2c.common.contract.AddEmployeeRequestVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feign 契约形状测试（任务项 4.3.x）：不依赖真实网络，验证声明与线格式。
 */
class EmployeeFeignContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("4.3.1 Feign 客户端指向 employee-service 的 /employee")
    void feignClientAnnotation() {
        FeignClient annotation = EmployeeFeignClient.class.getAnnotation(FeignClient.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("employee-service");
        assertThat(annotation.path()).isEqualTo("/employee");
    }

    @Test
    @DisplayName("4.3.2 save 方法声明为 POST /save")
    void saveMethodMapping() throws Exception {
        PostMapping mapping = EmployeeFeignClient.class
                .getMethod("save", AddEmployeeRequestVO.class)
                .getAnnotation(PostMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/save");
    }

    @Test
    @DisplayName("4.3.2 请求体 JSON 恰好是 shopId/username/password 三个键")
    void requestBodyShape() throws Exception {
        AddEmployeeRequestVO req = new AddEmployeeRequestVO();
        req.setShopId(88);
        req.setUsername("admin");
        req.setPassword("123456");

        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(req));

        assertThat(node.size()).isEqualTo(3);
        assertThat(node.get("shopId").asInt()).isEqualTo(88);
        assertThat(node.get("username").asText()).isEqualTo("admin");
        assertThat(node.get("password").asText()).isEqualTo("123456");
    }

    @Test
    @DisplayName("4.3.3 响应可反序列化为 BaseResponseVO<Integer>")
    void responseShape() throws Exception {
        BaseResponseVO<Integer> resp = objectMapper.readValue(
                "{\"status\":200,\"message\":null,\"data\":7}",
                new TypeReference<BaseResponseVO<Integer>>() {
                });

        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(resp.getData()).isEqualTo(7);
    }
}
