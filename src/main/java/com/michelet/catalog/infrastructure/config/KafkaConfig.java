package com.michelet.catalog.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    /**
     * 카프카 컨슈머 에러 핸들러 설정
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        // 1. 에러가 난 메시지를 DLT(Dead Letter Topic, 예: stock.reserved.DLT)로 보내는 역할
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        // 2. 기본 재시도 정책: 1초(1000ms) 간격으로 최대 3번 재시도
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));

        // 3. 특정 예외(IllegalArgumentException)는 재시도해봤자 의미 없으므로 재시도 없이 즉시 DLT로 직행하도록 설정
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        return errorHandler;
    }
}
