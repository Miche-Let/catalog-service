package com.michelet.catalog.infrastructure.messaging;

import com.michelet.catalog.application.ProductViewCommandService;
import com.michelet.catalog.infrastructure.messaging.dto.ProductCreatedEvent;
import com.michelet.catalog.infrastructure.messaging.dto.ProductStatusChangedEvent;
import com.michelet.catalog.infrastructure.messaging.dto.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ProductViewCommandService productViewCommandService;

    @KafkaListener(
        topics = "${catalog.kafka.topic.product-created:product.created}",
        groupId = "${spring.kafka.consumer.group-id:catalog-service-consumer}"
    )
    @CacheEvict(value = "products_cache", allEntries = true, cacheManager = "catalogCacheManager")
    // 이벤트 유입 즉시 메인 화면 캐시 제거
    public void consumeProductCreatedEvent(ProductCreatedEvent event) {
        log.info("product.created 이벤트 수신: {}", event);
        productViewCommandService.createProductView(event);
    }

    // 상품 수정 리스너
    @KafkaListener(
        topics = "${catalog.kafka.topic.product-updated:product.updated}",
        groupId = "${spring.kafka.consumer.group-id:catalog-service-consumer}"
    )
    @Caching(
        evict = {
            @CacheEvict(value = "products_cache", allEntries = true, cacheManager = "catalogCacheManager"),
            @CacheEvict(value = "product_detail_cache", allEntries = true, cacheManager = "catalogCacheManager")
        }
    )
    // 이벤트 유입 즉시 메인 화면 캐시 제거
    public void consumeProductUpdatedEvent(ProductUpdatedEvent event) {
        log.info("product.updated 이벤트 수신: {}", event);
        productViewCommandService.updateProductView(event);
    }

    // 상품 상태 변경 리스너
    @KafkaListener(
        topics = "${catalog.kafka.topic.status-changed:product.status-changed}",
        groupId = "${spring.kafka.consumer.group-id:catalog-service-consumer}"
    )
    @Caching(
        evict = {
            @CacheEvict(value = "products_cache", allEntries = true, condition = "#event.newStatus() != 'SOLDOUT'", cacheManager = "catalogCacheManager"),
            @CacheEvict(value = "product_detail_cache", allEntries = true, condition = "#event.newStatus() != 'SOLDOUT'", cacheManager = "catalogCacheManager")
        }
    )
    // SOLDOUT 이외 상태 변경 이벤트 유입 시 메인 화면 캐시 제거
    public void consumeProductStatusChangedEvent(ProductStatusChangedEvent event) {
        log.info("product.status-changed 이벤트 수신: {}", event);
        productViewCommandService.updateProductStatus(event);
    }
}
