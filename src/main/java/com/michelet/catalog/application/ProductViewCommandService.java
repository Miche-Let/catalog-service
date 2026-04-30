package com.michelet.catalog.application;

import com.michelet.catalog.domain.model.ProductView;
import com.michelet.catalog.domain.repository.ProductViewRepository;
import com.michelet.catalog.infrastructure.messaging.dto.StockReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductViewCommandService {

    private final ProductViewRepository productViewRepository;

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
            .orElseThrow(() -> new IllegalArgumentException("해당 옵션을 가진 상품을 찾을 수 없습니다: " + event.optionId()));

        // 2. 문서 내의 재고 데이터를 업데이트
        productView.updateStock(event.optionId(), event.totalQuantity(), event.currentDailyStock());

        // 3. MongoDB에 저장 (덮어쓰기)
        productViewRepository.save(productView);
        log.info("MongoDB 재고 업데이트 완료: productId={}", productView.getProductId());
    }
}
