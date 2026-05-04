package com.michelet.catalog.application;

import com.michelet.catalog.domain.exception.ProductNotFoundException;
import com.michelet.catalog.domain.model.ProductView;
import com.michelet.catalog.domain.model.ProductView.OptionView;
import com.michelet.catalog.domain.repository.ProductViewRepository;
import com.michelet.catalog.infrastructure.messaging.dto.DailyStockResetEvent;
import com.michelet.catalog.infrastructure.messaging.dto.ProductCreatedEvent;
import com.michelet.catalog.infrastructure.messaging.dto.ProductStatusChangedEvent;
import com.michelet.catalog.infrastructure.messaging.dto.StockReservedEvent;
import com.michelet.catalog.infrastructure.messaging.dto.StockRestoredEvent;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
                opt.currentDailyStock(),
                opt.dailyLimit()
            ))
            .toList();

        ProductView.Display display = new ProductView.Display(event.startAt(), event.endAt());

        LocalDateTime now = LocalDateTime.now();
        boolean initialVisibility = false;
        String initialStatus = "HIDDEN";

        if (event.startAt() != null && !now.isBefore(event.startAt()) &&
            (event.endAt() == null || now.isBefore(event.endAt()))) {
            initialVisibility = true;
            initialStatus = "ACTIVE";
        }

        ProductView productView = ProductView.builder()
            .productId(event.productId())
            .restaurantId(event.restaurantId())
            .name(event.name())
            .category(event.category())
            .status(initialStatus) //동적 상태 세팅
            .basePrice(event.basePrice())
            .metadata(event.attributes())
            .isVisible(initialVisibility)
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
        log.info("MongoDB 재고 차감 업데이트 완료: productId={}", productView.getProductId());
    }

    /**
     * 재고 복구 이벤트를 처리하여 MongoDB 데이터를 갱신함
     */
    public void applyStockRestoredEvent(StockRestoredEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("이벤트 페이로드가 null입니다.");
        }

        log.info("재고 복구 이벤트 수신: optionId={}, total={}, daily={}",
            event.optionId(), event.totalQuantity(), event.currentDailyStock());

        // 1. 해당 옵션을 가지고 있는 상품 문서 찾기
        ProductView productView = productViewRepository.findByOptionsOptionId(event.optionId())
            .orElseThrow(ProductNotFoundException::new);

        // 2. 문서 내의 재고 데이터를 업데이트 (Inventory가 보내준 복구된 최종 재고로 덮어쓰기)
        productView.updateStock(event.optionId(), event.totalQuantity(), event.currentDailyStock());

        // 3. MongoDB에 저장 (덮어쓰기)
        productViewRepository.save(productView);
        log.info("MongoDB 재고 복구 업데이트 완료: productId={}", productView.getProductId());
    }

    /**
     * 상품 전시 상태 업데이트 이벤트를 처리함
     */
    public void updateProductStatus(ProductStatusChangedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("이벤트 페이로드가 null입니다.");
        }

        // 상품이 없을 경우 예외를 던져 DLT로 이동하여 재처리 가능하도록 유도
        ProductView view = productViewRepository.findByProductId(event.productId())
            .orElseThrow(() -> {
                log.warn("상품 상태 동기화 실패 (해당 상품을 찾을 수 없음): productId={}", event.productId());
                return new ProductNotFoundException();
            });

        view.updateStatus(event.newStatus());
        productViewRepository.save(view);
        log.info("MongoDB 상품 상태 동기화 완료: productId={}, status={}", event.productId(), event.newStatus());
    }

    /**
     * 전체 상품 일일 재고 리셋 이벤트를 처리함
     */
    public void resetAllDailyStocks(DailyStockResetEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("이벤트 페이로드가 null입니다.");
        }

        // findAll()로 인한 OOM 방지를 위해 Batch Processing(Chunk) 도입
        int pageSize = 100;
        PageRequest pageRequest = PageRequest.of(0, pageSize);
        Page<ProductView> page;
        int totalProcessed = 0;

        do {
            page = productViewRepository.findAll(pageRequest);
            if (page.isEmpty()) {
                break;
            }

            List<ProductView> batch = page.getContent();
            for (ProductView product : batch) {
                product.resetDailyStock();
            }
            productViewRepository.saveAll(batch);
            totalProcessed += batch.size();

            pageRequest = pageRequest.next();
        } while (page.hasNext());

        log.info("MongoDB 카탈로그 전체 일일 재고 리셋 완료: {}건", totalProcessed);
    }
}
