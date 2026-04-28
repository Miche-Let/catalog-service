//package com.michelet.catalog.infrastructure.config;
//
//import org.slf4j.MDC;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.domain.AuditorAware;
//
//import java.util.Optional;
//import java.util.UUID;
//
//@Configuration
//public class AuditorConfig {
//
//    @Bean
//    public AuditorAware<UUID> auditorAware() {
//        return () -> {
//            String userId = MDC.get("userId");
//            if (userId == null || userId.isBlank()) {
//                return Optional.of(UUID.fromString("00000000-0000-0000-0000-000000000000"));
//            }
//            return Optional.of(UUID.fromString(userId));
//        };
//    }
//}
