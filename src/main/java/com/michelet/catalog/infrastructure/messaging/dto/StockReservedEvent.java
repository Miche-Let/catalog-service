package com.michelet.catalog.infrastructure.messaging.dto;

import java.util.UUID;

// 인벤토리 서비스에서 전송하는 JSON 구조와 매핑됨
public record StockReservedEvent(
    UUID optionId,
    Integer totalQuantity,
    Integer currentDailyStock
) {
    // Record 생성 시점에 데이터 검증
    public StockReservedEvent {
        if (optionId == null) {
            throw new IllegalArgumentException("optionId must not be null");
        }
        if (totalQuantity == null || totalQuantity < 0) {
            throw new IllegalArgumentException("totalQuantity must not be null or negative");
        }
        if (currentDailyStock == null || currentDailyStock < 0) {
            throw new IllegalArgumentException("currentDailyStock must not be null or negative");
        }
    }
}
