package com.enviro.brain.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${enviro.api.key}")
    private String apiKey;

    @Bean
    public ApiKeyAuthInterceptor apiKeyAuthInterceptor() {
        return new ApiKeyAuthInterceptor(apiKey);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyAuthInterceptor())
                .addPathPatterns("/api/**");
    }
}
