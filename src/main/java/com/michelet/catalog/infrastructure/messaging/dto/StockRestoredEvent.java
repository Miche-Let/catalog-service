package com.michelet.catalog.infrastructure.messaging.dto;

import java.util.UUID;

public record StockRestoredEvent(
    UUID optionId,
    int totalQuantity,
    int currentDailyStock
) {
}
