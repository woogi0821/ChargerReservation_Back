package com.simplecoding.chargerreservation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${spring.react.ip}")
    String reactIp;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        reactIp,                      // http://localhost:5173 유지
                        "http://localhost:5500",       // ✅ 추가 — 라이브 서버
                        "http://127.0.0.1:5500",       // ✅ 추가 — 라이브 서버
                        "http://localhost:5501",        // ✅ 추가 — 라이브 서버 (포트 다를 때)
                        "http://127.0.0.1:5501"         // ✅ 추가 — 라이브 서버 (포트 다를 때)
                )
                .allowedMethods(
                        HttpMethod.GET.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.DELETE.name(),
                        HttpMethod.PATCH.name()
                )
                .allowCredentials(true);
    }
}