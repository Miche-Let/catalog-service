package com.michelet.catalog.infrastructure.messaging;

import com.michelet.catalog.application.ProductViewCommandService;
import com.michelet.catalog.infrastructure.messaging.dto.DailyStockResetEvent;
import com.michelet.catalog.infrastructure.messaging.dto.StockReservedEvent;
import com.michelet.catalog.infrastructure.messaging.dto.StockRestoredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventConsumer {

    private final ProductViewCommandService productViewCommandService;

    @KafkaListener(
        topics = "${catalog.kafka.topic.stock-reserved:stock.reserved}",
        groupId = "${spring.kafka.consumer.group-id:catalog-service-consumer}"
    )
    @CacheEvict(value = "products_cache", allEntries = true, cacheManager = "catalogCacheManager") // 재고 차감 시 캐시 초기화 트리거
    public void consumeStockReservedEvent(StockReservedEvent event) {
        log.info("stock.reserved 이벤트 수신: {}", event);
        // KafkaListener Container가 에러를 인지하고 재시도 할 수 있도록
        productViewCommandService.applyStockReservedEvent(event);
    }

    @KafkaListener(
        topics = "${catalog.kafka.topic.stock-restored:stock.restored}",
        groupId = "${spring.kafka.consumer.group-id:catalog-service-consumer}"
    )
    @CacheEvict(value = "products_cache", allEntries = true, cacheManager = "catalogCacheManager") // 재고 원복 시 캐시 초기화 트리거
    public void consumeStockRestoredEvent(StockRestoredEvent event) {
        log.info("stock.restored 이벤트 수신: {}", event);
        productViewCommandService.applyStockRestoredEvent(event);
    }

    // 일일 재고 초기화 리스너
    @KafkaListener(
        topics = "${catalog.kafka.topic.daily-reset:stock.daily-reset}",
        groupId = "${spring.kafka.consumer.group-id:catalog-service-consumer}"
    )
    @CacheEvict(value = "products_cache", allEntries = true, cacheManager = "catalogCacheManager")
    // 자정 전수 스캔 초기화 시 캐시 전면 무효화
    public void consumeDailyStockResetEvent(DailyStockResetEvent event) {
        log.info("stock.daily-reset 이벤트 수신: {}", event);
        productViewCommandService.resetAllDailyStocks(event);
    }
}
