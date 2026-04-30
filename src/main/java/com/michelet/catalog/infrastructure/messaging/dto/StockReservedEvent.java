package com.michelet.catalog.infrastructure.messaging.dto;

import java.util.Objects;
import java.util.UUID;

// 인벤토리 서비스에서 전송하는 JSON 구조와 매핑됨
public record StockReservedEvent(
    UUID optionId,
    Integer totalQuantity,
    Integer currentDailyStock
) {
    // Record 생성 시점에 데이터 검증
    public StockReservedEvent {
        // 1. 필수 값(Null) 검증
        Objects.requireNonNull(optionId, "옵션 ID(optionId)는 필수입니다.");
        Objects.requireNonNull(totalQuantity, "총 재고 수량(totalQuantity)은 필수입니다.");
        Objects.requireNonNull(currentDailyStock, "일일 재고 수량(currentDailyStock)은 필수입니다.");

        // 2. 비즈니스 값(음수) 검증
        if (totalQuantity < 0 || currentDailyStock < 0) {
            throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
        }
    }
}
