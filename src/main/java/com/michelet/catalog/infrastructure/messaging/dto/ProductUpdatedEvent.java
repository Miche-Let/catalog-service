package com.michelet.catalog.infrastructure.messaging.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ProductUpdatedEvent(
    UUID productId,
    String name,
    String category,
    BigDecimal basePrice,
    Map<String, Object> attributes
) {
}
