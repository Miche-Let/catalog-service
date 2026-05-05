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
import org.springframework.util.AntPathMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.application.name:catalog-service}")
    private String applicationName;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, InternalTokenProvider tokenProvider) throws Exception {

        InternalAuthFilter cleanInternalFilter = new InternalAuthFilter(tokenProvider, applicationName) {
            private final AntPathMatcher pathMatcher = new AntPathMatcher();

            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                // request.getServletPath()를 사용하면 Context-Path가 있어도 이를 제외한 순수 경로만 가져옴
                String path = request.getServletPath();
                return !pathMatcher.match("/internal/**", path);
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
