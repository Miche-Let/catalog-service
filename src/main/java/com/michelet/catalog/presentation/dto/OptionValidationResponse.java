package com.michelet.catalog.presentation.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OptionValidationResponse(
    UUID optionId,
    String name,
    BigDecimal totalPrice // 기본가 + 옵션 추가 금액
) {
}
