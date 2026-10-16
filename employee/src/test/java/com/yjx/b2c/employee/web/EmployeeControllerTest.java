package com.yjx.b2c.employee.web;

import com.yjx.b2c.employee.service.EmployeeService;
import com.yjx.b2c.employee.web.aspect.GlobalExceptionHandler;
import com.yjx.b2c.common.contract.AddEmployeeRequestVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * employee 侧保存接口契约测试（任务项 4.4.1）。
 */
class EmployeeControllerTest {

    private static final String BODY =
            "{\"shopId\":88,\"username\":\"admin\",\"password\":\"123456\"}";

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
    @DisplayName("4.4.1 /employee/save 返回新建员工 ID")
    void saveReturnsId() throws Exception {
        when(employeeService.addEmployee(any(AddEmployeeRequestVO.class))).thenReturn(7);

        mockMvc.perform(post("/employee/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(7));

        ArgumentCaptor<AddEmployeeRequestVO> captor = ArgumentCaptor.forClass(AddEmployeeRequestVO.class);
        verify(employeeService).addEmployee(captor.capture());
        assertThat(captor.getValue().getShopId()).isEqualTo(88);
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("4.4.1 缺少 shopId 时 Service 抛异常，接口返回 400")
    void missingShopIdBecomesBadRequest() throws Exception {
        when(employeeService.addEmployee(any(AddEmployeeRequestVO.class)))
                .thenThrow(new IllegalArgumentException("保存员工信息失败"));

        mockMvc.perform(post("/employee/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(400));
    }
}
