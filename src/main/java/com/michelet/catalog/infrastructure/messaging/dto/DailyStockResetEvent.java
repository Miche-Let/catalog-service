package com.michelet.catalog.infrastructure.messaging.dto;

import java.time.LocalDate;

public record DailyStockResetEvent(
    LocalDate resetDate,
    int updatedOptionCount
) {
}
