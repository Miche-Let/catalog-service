package com.michelet.catalog.infrastructure.messaging;

import com.michelet.catalog.application.ProductViewCommandService;
import com.michelet.catalog.infrastructure.messaging.dto.DailyStockResetEvent;
import com.michelet.catalog.infrastructure.messaging.dto.ProductCreatedEvent;
import com.michelet.catalog.infrastructure.messaging.dto.ProductStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public void consumeProductCreatedEvent(ProductCreatedEvent event) {
        log.info("product.created 이벤트 수신: {}", event);
        productViewCommandService.createProductView(event);
    }

    // 상품 상태 변경 리스너
    @KafkaListener(
        topics = "${catalog.kafka.topic.status-changed:product.status-changed}",
        groupId = "${spring.kafka.consumer.group-id:catalog-service-consumer}"
    )
    public void consumeProductStatusChangedEvent(ProductStatusChangedEvent event) {
        log.info("product.status-changed 이벤트 수신: {}", event);
        productViewCommandService.updateProductStatus(event);
    }

    // 일일 재고 초기화 리스너
    @KafkaListener(
        topics = "${catalog.kafka.topic.daily-reset:stock.daily-reset}",
        groupId = "${spring.kafka.consumer.group-id:catalog-service-consumer}"
    )
    public void consumeDailyStockResetEvent(DailyStockResetEvent event) {
        log.info("stock.daily-reset 이벤트 수신: {}", event);
        productViewCommandService.resetAllDailyStocks(event);
    }
}
