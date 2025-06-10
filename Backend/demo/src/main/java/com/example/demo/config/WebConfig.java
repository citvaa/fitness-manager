package com.example.demo.config;

import com.example.demo.interceptor.JwtInterceptor;
import com.example.demo.interceptor.RoleInterceptor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;
    private final RoleInterceptor roleInterceptor;

    @Override
    public void addInterceptors(@NotNull InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .order(1)
                .excludePathPatterns("/api/user/login", "/swagger-ui.html", "/api/user/register", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml");

        registry.addInterceptor(roleInterceptor)
                .order(2)
                .excludePathPatterns("/api/user/login", "/swagger-ui.html", "/api/user/register", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml");
    }
}
