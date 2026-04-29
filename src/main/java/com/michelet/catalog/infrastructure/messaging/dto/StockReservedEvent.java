package com.michelet.catalog.infrastructure.messaging.dto;

import java.util.UUID;

// 인벤토리 서비스에서 전송하는 JSON 구조와 매핑됨
public record StockReservedEvent(
    UUID optionId,
    Integer totalQuantity,
    Integer currentDailyStock
) {
}
