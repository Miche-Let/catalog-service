package com.michelet.catalog.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.michelet.catalog.presentation.dto.OptionValidationResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public CacheManager catalogCacheManager(RedisConnectionFactory connectionFactory) {

        // 다형성 타입이 필요한 캐시용 (products_cache 등)
        ObjectMapper polymorphicMapper = new ObjectMapper();
        polymorphicMapper.registerModule(new JavaTimeModule());
        polymorphicMapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.michelet")
                .allowIfSubType("org.springframework.data.domain")
                .allowIfSubType("java.util")       // 컬렉션 타입 허용 필수
                .allowIfSubType("java.time")       // LocalDateTime 등 날짜 타입
                .allowIfSubType("java.math")       // BigDecimal 등
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer polymorphicSerializer =
            new GenericJackson2JsonRedisSerializer(polymorphicMapper);

        // 타입 정보 불필요한 단순 DTO용 (product_validate_cache)
        // OptionValidationResponse는 UUID/String/BigDecimal만 있어 @class 불필요
        Jackson2JsonRedisSerializer<OptionValidationResponse> validateSerializer =
            new Jackson2JsonRedisSerializer<>(OptionValidationResponse.class);

        // 기본 설정 (products_cache 등에 적용)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(polymorphicSerializer))
            .disableCachingNullValues();

        // product_validate_cache 전용 설정
        RedisCacheConfiguration validateConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(validateSerializer))
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("product_validate_cache", validateConfig);

        return RedisCacheManager.RedisCacheManagerBuilder
            .fromConnectionFactory(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
