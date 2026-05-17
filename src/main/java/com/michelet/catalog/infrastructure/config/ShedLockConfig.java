package com.michelet.catalog.infrastructure.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
// order = 0 으로 설정하여 @CacheEvict 보다 먼저 락을 획득하도록 강제!
// - 이렇게 해야 락을 획득한 딱 1대의 서버만 @CacheEvict를 실행할 수 있음
@EnableSchedulerLock(
    defaultLockAtMostFor = "PT30M", // 30분이 지나면 Redis에서 자동으로 락을 해제하여 다음 실행이 가능하게 함
    order = 0
)
public class ShedLockConfig {

    // ShedLock이 자물쇠 정보를 어디에 저장할지를 결정
    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        // Redis를 이용해 'catalog-service-lock'이라는 이름의 글로벌 자물쇠 환경 구성
        return new RedisLockProvider(connectionFactory, "catalog-service-lock");
    }
}
