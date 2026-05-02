package com.michelet.catalog.application;

import com.michelet.catalog.domain.exception.ProductNotFoundException;
import com.michelet.catalog.domain.model.ProductView;
import com.michelet.catalog.domain.model.ProductView.OptionView;
import com.michelet.catalog.domain.repository.ProductViewRepository;
import com.michelet.catalog.infrastructure.messaging.dto.ProductCreatedEvent;
import com.michelet.catalog.infrastructure.messaging.dto.StockReservedEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductViewCommandService {

    private final ProductViewRepository productViewRepository;

    /**
     * 상품 등록 이벤트를 처리 - MongoDB에 초기 데이터를 적재
     */
    public void createProductView(ProductCreatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("이벤트 페이로드가 null입니다.");
        }

        // 멱등성 보장 - 객체 생성 전에 가장 먼저 중복 여부를 검사
        if (productViewRepository.findByProductId(event.productId()).isPresent()) {
            log.warn("중복 product.created 이벤트 무시: productId={}", event.productId());
            return;
        }

        log.info("product.created 이벤트 수신 - 상품 등록 초기 데이터 적재: productId={}", event.productId());

        List<OptionView> optionViews = event.options().stream()
            .map(opt -> new ProductView.OptionView(
                opt.optionId(),
                opt.name(),
                opt.addPrice(),
                opt.totalQuantity(),
                opt.currentDailyStock()
            ))
            .toList();

        ProductView.Display display = new ProductView.Display(event.startAt(), event.endAt());

        ProductView productView = ProductView.builder()
            .productId(event.productId())
            .restaurantId(event.restaurantId())
            .name(event.name())
            .category(event.category())
            .metadata(event.attributes())
            .isVisible(false) // 등록 직후 is_visible = false 정책 반영 - TODO 나중에 시간에 맞춰 오픈되도록 해야함
            .display(display)
            .options(optionViews)
            .build();

        productViewRepository.save(productView);
        log.info("MongoDB 초기 데이터 적재 완료: productId={}", productView.getProductId());
    }

    /**
     * 재고 예약 이벤트를 처리하여 MongoDB 데이터를 갱신함
     */
    public void applyStockReservedEvent(StockReservedEvent event) {
        // 이벤트 객체 자체가 null인지 가장 먼저 확인
        if (event == null) {
            throw new IllegalArgumentException("이벤트 페이로드가 null입니다.");
        }

        log.info("재고 차감 이벤트 수신: optionId={}, total={}, daily={}",
            event.optionId(), event.totalQuantity(), event.currentDailyStock());

        // 1. 해당 옵션을 가지고 있는 상품 문서 찾기
        ProductView productView = productViewRepository.findByOptionsOptionId(event.optionId())
            .orElseThrow(ProductNotFoundException::new);

        // 2. 문서 내의 재고 데이터를 업데이트
        productView.updateStock(event.optionId(), event.totalQuantity(), event.currentDailyStock());

        // 3. MongoDB에 저장 (덮어쓰기)
        productViewRepository.save(productView);
        log.info("MongoDB 재고 업데이트 완료: productId={}", productView.getProductId());
    }
}
