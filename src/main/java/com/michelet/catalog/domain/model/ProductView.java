package com.michelet.catalog.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "p_product_views")
@CompoundIndex(def = "{'options.option_id': 1}") // 중첩 필드에 대한 인덱스 추가
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductView {

    @Id
    private String id; // MongoDB의 ObjectId

    @Version // 몽고DB 동시성 충돌 방지 - 낙관락
    private Long version;

    @Indexed(unique = true)
    @Field("product_id")
    private UUID productId; // PostgreSQL의 상품 ID

    @Indexed
    @Field("restaurant_id")
    private UUID restaurantId;

    private String name;
    private String category;
    private String status;

    @Field("base_price")
    private BigDecimal basePrice;

    private Map<String, Object> metadata;

    @Field("is_visible")
    private boolean isVisible;

    private Display display;
    private List<OptionView> options;

    @Builder
    private ProductView(UUID productId, UUID restaurantId, String name, String category, String status,
                        BigDecimal basePrice,
                        Map<String, Object> metadata,
                        boolean isVisible, Display display, List<OptionView> options) {
        this.productId = productId;
        this.restaurantId = restaurantId;
        this.name = name;
        this.category = category;
        this.status = status;
        this.basePrice = basePrice;
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

        @Field("daily_limit")
        private Integer dailyLimit;

        public OptionView(UUID optionId, String name, BigDecimal addPrice, Integer totalQuantity,
                          Integer currentDailyStock, Integer dailyLimit) {
            this.optionId = optionId;
            this.name = name;
            this.addPrice = addPrice;
            this.totalQuantity = totalQuantity;
            this.currentDailyStock = currentDailyStock;
            this.dailyLimit = dailyLimit;
        }

        // 재고 리셋을 위한 setter 대용 메서드
        public void resetDailyStock() {
            if (this.totalQuantity == null) {
                throw new IllegalStateException(
                    "총 재고(totalQuantity)가 null일 수 없습니다. DB 데이터 정합성 오류입니다: optionId=" + this.optionId);
            }
            if (this.dailyLimit != null) {
                this.currentDailyStock = Math.min(this.dailyLimit, this.totalQuantity);
            } else {
                this.currentDailyStock = this.totalQuantity;
            }
        }

        // 재고 업데이트를 위한 메서드
        public void updateStock(Integer total, Integer current) {
            if (total == null || current == null) {
                throw new IllegalArgumentException("재고 수량은 null일 수 없습니다.");
            }
            if (total < 0 || current < 0) {
                throw new IllegalArgumentException("재고 수량은 0 이상이어야 합니다.");
            }
            if (current > total) {
                throw new IllegalArgumentException("일일 재고가 총 재고를 초과할 수 없습니다.");
            }

            this.totalQuantity = total;
            this.currentDailyStock = current;
        }
    }

    /**
     * 재고 예약(차감)/복구 이벤트를 수신했을 때 호출되어 옵션의 재고를 갱신함
     */
    public void updateStock(UUID targetOptionId, Integer newTotalQuantity, Integer newCurrentDailyStock) {
        if (this.options == null) {
            return;
        }

        for (OptionView option : this.options) {
            if (option != null && Objects.equals(option.getOptionId(), targetOptionId)) {
                option.updateStock(newTotalQuantity, newCurrentDailyStock);
                break;
            }
        }
    }

    // 전시 상태 변경
    public void updateStatus(String status) {
        if (status == null || (!"ACTIVE".equals(status) && !"HIDDEN".equals(status) && !"DELETED".equals(status)
            && !"SOLDOUT".equals(status))) {
            throw new IllegalArgumentException("유효하지 않은 상태 값입니다: " + status);
        }

        this.status = status;
        // 상태가 ACTIVE가 아니면 노출 여부(isVisible)도 자동으로 관리
        this.isVisible = "ACTIVE".equals(status);
    }

    // 상품 부분 업데이트 로직
    public void update(String name, String category, BigDecimal basePrice, Map<String, Object> metadata) {
        if (name != null) {
            this.name = name;
        }
        if (category != null) {
            this.category = category;
        }
        if (basePrice != null) {
            this.basePrice = basePrice;
        }
        if (metadata != null) {
            this.metadata = metadata;
        }
    }

    // 일일 재고 리셋
    public void resetDailyStock() {
        if (this.options != null) {
            for (OptionView option : this.options) {
                option.resetDailyStock();
            }
        }
    }
}
