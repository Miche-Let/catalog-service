package com.michelet.catalog.infrastructure.messaging.dto;

import java.util.UUID;

public record ProductStatusChangedEvent(
    UUID productId,
    String newStatus
) {
}
