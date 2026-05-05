package com.michelet.catalog.infrastructure.config;

import com.michelet.common.auth.webmvc.filter.InternalAuthFilter;
import com.michelet.common.auth.webmvc.internal.InternalTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.application.name:catalog-service}")
    private String applicationName;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, InternalTokenProvider tokenProvider) throws Exception {

        // common 모듈의 필터를 가져오되, 일반 API(/api/v1/**)는 검사하지 않도록 예외 처리만 추가
        InternalAuthFilter cleanInternalFilter = new InternalAuthFilter(tokenProvider, applicationName) {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                return !request.getRequestURI().startsWith("/internal/");
            }
        };

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/internal/**").permitAll()
                .requestMatchers("/api/v1/**").permitAll()
                .anyRequest().authenticated()
            )
            // 커스텀 필터 등록
            .addFilterBefore(cleanInternalFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
