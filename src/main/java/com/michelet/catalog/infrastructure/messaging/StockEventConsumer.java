package com.michelet.catalog.infrastructure.messaging;

import com.michelet.catalog.application.ProductViewCommandService;
import com.michelet.catalog.infrastructure.messaging.dto.StockReservedEvent;
import com.michelet.catalog.infrastructure.messaging.dto.StockRestoredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventConsumer {

    private final ProductViewCommandService productViewCommandService;

    @KafkaListener(topics = "stock.reserved", groupId = "catalog-service-consumer")
    public void consumeStockReservedEvent(StockReservedEvent event) {
        log.info("stock.reserved 이벤트 수신: {}", event);
        // KafkaListener Container가 에러를 인지하고 재시도 할 수 있도록
        productViewCommandService.applyStockReservedEvent(event);
    }

    @KafkaListener(topics = "stock.restored", groupId = "catalog-service-consumer")
    public void consumeStockRestored(StockRestoredEvent event) {
        log.info("stock.restored 이벤트 수신: {}", event);
        productViewCommandService.applyStockRestoredEvent(event);
    }

    //TODO 향후 추가될 리스너들:
    // @KafkaListener(topics = "stock.daily-reset", ...)
}
