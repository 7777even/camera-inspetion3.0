package com.enviro.brain.config;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiKeyAuthInterceptor 单元测试")
class ApiKeyAuthInterceptorTest {

    private ApiKeyAuthInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new ApiKeyAuthInterceptor("dev-api-key-2026");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("有效 API Key 应放行")
    void preHandle_withValidApiKey_shouldReturnTrue() throws Exception {
        request.addHeader("X-API-Key", "dev-api-key-2026");
        request.setRequestURI("/api/v1/sync/watermark");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getStatus()).isNotEqualTo(401);
    }

    @Test
    @DisplayName("缺少 X-API-Key 头应返回 401")
    void preHandle_withoutApiKey_shouldReturn401() throws Exception {
        request.setRequestURI("/api/v1/sync/watermark");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Missing X-API-Key header");
    }

    @Test
    @DisplayName("无效 API Key 应返回 401")
    void preHandle_withInvalidApiKey_shouldReturn401() throws Exception {
        request.addHeader("X-API-Key", "wrong-key");
        request.setRequestURI("/api/v1/sync/watermark");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid API Key");
    }

    @Test
    @DisplayName("白名单 /actuator/health 免认证")
    void preHandle_healthEndpoint_shouldBypass() throws Exception {
        request.setRequestURI("/actuator/health");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("白名单 /error 免认证")
    void preHandle_errorEndpoint_shouldBypass() throws Exception {
        request.setRequestURI("/error");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }
}
