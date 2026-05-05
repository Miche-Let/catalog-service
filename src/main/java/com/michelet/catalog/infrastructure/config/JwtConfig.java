package com.michelet.catalog.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${internal.auth.secret:}")
    private String internalSecret;

    @PostConstruct
    public void validateSecret() {
        if (jwtSecret.isBlank()) {
            throw new IllegalStateException("보안 오류: JWT_SECRET이 설정되지 않았습니다.");
        }
        if (internalSecret.isBlank()) {
            throw new IllegalStateException("보안 오류: INTERNAL_AUTH_SECRET이 설정되지 않았습니다.");
        }
    }
}
