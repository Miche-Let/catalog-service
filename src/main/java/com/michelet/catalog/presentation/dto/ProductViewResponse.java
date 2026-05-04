package com.michelet.catalog.presentation.dto;

import com.michelet.catalog.domain.model.ProductView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductViewResponse(
    UUID productId,
    UUID restaurantId,
    String name,
    String category,
    String status,
    BigDecimal basePrice,
    Map<String, Object> metadata,
    boolean isVisible,
    List<OptionResponse> options
) {
    public static ProductViewResponse from(ProductView productView) {
        List<OptionResponse> optionResponses = productView.getOptions() != null ?
            productView.getOptions().stream()
                .map(OptionResponse::from)
                .toList() : List.of();

        return new ProductViewResponse(
            productView.getProductId(),
            productView.getRestaurantId(),
            productView.getName(),
            productView.getCategory(),
            productView.getStatus(),
            productView.getBasePrice(),
            productView.getMetadata(),
            productView.isVisible(),
            optionResponses
        );
    }

    public record OptionResponse(
        UUID optionId,
        String name,
        BigDecimal addPrice,
        Integer totalQuantity,
        Integer currentDailyStock,
        Integer dailyLimit
    ) {
        public static OptionResponse from(ProductView.OptionView optionView) {
            return new OptionResponse(
                optionView.getOptionId(),
                optionView.getName(),
                optionView.getAddPrice(),
                optionView.getTotalQuantity(),
                optionView.getCurrentDailyStock(),
                optionView.getDailyLimit()
            );
        }
    }
}
