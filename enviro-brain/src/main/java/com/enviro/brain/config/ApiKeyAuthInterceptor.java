package com.enviro.brain.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    private final String apiKey;

    public ApiKeyAuthInterceptor(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();

        // Whitelist: skip /actuator/health and /error
        if ("/actuator/health".equals(uri) || "/error".equals(uri)) {
            return true;
        }

        String clientKey = request.getHeader("X-API-Key");

        if (clientKey == null || clientKey.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":401,\"message\":\"Missing X-API-Key header\"}");
            return false;
        }

        if (!apiKey.equals(clientKey)) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":401,\"message\":\"Invalid API Key\"}");
            return false;
        }

        return true;
    }
}
