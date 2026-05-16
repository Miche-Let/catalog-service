package com.michelet.catalog.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public CacheManager catalogCacheManager(RedisConnectionFactory connectionFactory) {
        // 역직렬화 시 날짜 포맷 깨짐 방지 및 다형성 타입 보존을 위한 커스텀 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // 무방비 역직렬화 공격 차단을 위한 화이트리스트 보안 필터 적용
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.michelet.catalog")
            .allowIfSubType("org.springframework.data.domain")
            .allowIfSubType("java.util")
            .allowIfSubType("java.time")
            .allowIfSubType("java.math")
            .allowIfBaseType(Object.class)
            .build();

        objectMapper.activateDefaultTyping(
            ptv,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            // 캐시 유효 시간 설정 (기본 1시간)
            .entryTtl(Duration.ofHours(1))
            // 캐시 Key는 String으로 직렬화
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            // 캐시 Value는 JSON 구조로 직렬화하여 가독성 및 호환성 보장
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            // 몽고DB의 데이터가 null일 경우 캐싱을 방지 (Cache Penetration 방어)
            .disableCachingNullValues();

        return RedisCacheManager.RedisCacheManagerBuilder
            .fromConnectionFactory(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
}
