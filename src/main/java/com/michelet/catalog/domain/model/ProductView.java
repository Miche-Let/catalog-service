package com.michelet.catalog.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "p_product_views")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductView {

    @Id
    private String id; // MongoDB의 ObjectId

    @Indexed(unique = true)
    @Field("product_id")
    private UUID productId; // PostgreSQL의 상품 ID

    @Indexed
    @Field("restaurant_id")
    private UUID restaurantId;

    private String name;
    private String category;
    private Map<String, Object> metadata;

    @Field("is_visible")
    private boolean isVisible;

    private Display display;
    private List<OptionView> options;

    @Builder
    private ProductView(UUID productId, UUID restaurantId, String name, String category, Map<String, Object> metadata,
                        boolean isVisible, Display display, List<OptionView> options) {
        this.productId = productId;
        this.restaurantId = restaurantId;
        this.name = name;
        this.category = category;
        this.metadata = metadata;
        this.isVisible = isVisible;
        this.display = display;
        this.options = options;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Display {
        @Field("start_at")
        private LocalDateTime startAt;

        @Field("end_at")
        private LocalDateTime endAt;

        public Display(LocalDateTime startAt, LocalDateTime endAt) {
            this.startAt = startAt;
            this.endAt = endAt;
        }
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class OptionView {
        @Field("option_id")
        private UUID optionId;

        private String name;

        @Field("add_price")
        private BigDecimal addPrice;

        @Field("total_quantity")
        private Integer totalQuantity;

        @Field("current_daily_stock")
        private Integer currentDailyStock;

        public OptionView(UUID optionId, String name, BigDecimal addPrice, Integer totalQuantity,
                          Integer currentDailyStock) {
            this.optionId = optionId;
            this.name = name;
            this.addPrice = addPrice;
            this.totalQuantity = totalQuantity;
            this.currentDailyStock = currentDailyStock;
        }
    }

    /**
     * 재고 예약(차감) 이벤트를 수신했을 때 호출되어 옵션의 재고를 갱신함
     */
    public void updateStock(UUID targetOptionId, Integer newTotalQuantity, Integer newCurrentDailyStock) {
        if (this.options == null) {
            return;
        }

        for (OptionView option : this.options) {
            if (option.getOptionId().equals(targetOptionId)) {
                option.totalQuantity = newTotalQuantity;
                option.currentDailyStock = newCurrentDailyStock;
                break;
            }
        }
    }
}
