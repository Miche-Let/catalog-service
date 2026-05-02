package com.michelet.catalog.infrastructure.messaging;

import com.michelet.catalog.application.ProductViewCommandService;
import com.michelet.catalog.infrastructure.messaging.dto.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ProductViewCommandService productViewCommandService;

    @KafkaListener(topics = "product.created", groupId = "catalog-service-consumer")
    public void consumeProductCreatedEvent(ProductCreatedEvent event) {
        log.info("product.created 이벤트 수신: {}", event);
        productViewCommandService.createProductView(event);
    }

    //TODO 향후 추가될 리스너들:
    // @KafkaListener(topics = "product.status-updated", ...)
    // @KafkaListener(topics = "product.visible-updated", ...)
}
