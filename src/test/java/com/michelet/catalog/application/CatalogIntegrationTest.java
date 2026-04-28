package com.michelet.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
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

    @Autowired
    private MongoTemplate mongoTemplate;

    // 1. 테스트 실행 시 MongoDB 8.0 도커 컨테이너를 띄움
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0.0");

    // 2. 스프링 부트가 띄워진 도커 컨테이너의 주소를 바라보도록 설정 덮어쓰기
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Test
    @DisplayName("MongoDB 컨테이너가 정상적으로 연결되고 애플리케이션 컨텍스트가 로드된다")
    void contextLoads() {
        // 컨테이너 구동 확인
        assertThat(mongoDBContainer.isRunning()).isTrue();

        // 실제 DB 연결 및 통신 확인 (ping 명령 수행)
        Document pingResult = mongoTemplate.getDb().runCommand(new Document("ping", 1));
        assertThat(pingResult.get("ok")).isEqualTo(1.0);
    }
}
