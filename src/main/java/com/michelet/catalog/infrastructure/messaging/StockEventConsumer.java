package com.michelet.catalog.infrastructure.messaging;

import com.michelet.catalog.application.ProductViewCommandService;
import com.michelet.catalog.infrastructure.messaging.dto.StockReservedEvent;
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
        try {
            productViewCommandService.applyStockReservedEvent(event);
        } catch (Exception e) {
            log.error("stock.reserved 이벤트 처리 중 오류 발생: {}", event, e);
            // TODO: DLT(Dead Letter Topic)로 전송하거나 재시도 로직 추가 (추후 구현?)
        }
    }
}
