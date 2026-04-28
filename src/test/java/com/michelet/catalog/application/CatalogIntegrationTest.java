package com.michelet.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers // 도커를 띄워주는 어노테이션
@SpringBootTest
@ActiveProfiles("test")
class CatalogIntegrationTest {

    // 1. 테스트 실행 시 MongoDB 8.0 도커 컨테이너를 띄움
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8");

    // 2. 스프링 부트가 띄워진 도커 컨테이너의 주소를 바라보도록 설정 덮어쓰기
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Test
    @DisplayName("MongoDB 컨테이너가 정상적으로 연결되고 애플리케이션 컨텍스트가 로드된다")
    void contextLoads() {
        assertThat(mongoDBContainer.isRunning()).isTrue();
    }
}
