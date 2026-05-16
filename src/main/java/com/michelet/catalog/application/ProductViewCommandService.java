package com.michelet.catalog.application;

import com.michelet.catalog.domain.exception.ProductNotFoundException;
import com.michelet.catalog.domain.model.ProductView;
import com.michelet.catalog.domain.model.ProductView.OptionView;
import com.michelet.catalog.domain.repository.ProductViewRepository;
import com.michelet.catalog.infrastructure.messaging.dto.DailyStockResetEvent;
import com.michelet.catalog.infrastructure.messaging.dto.ProductCreatedEvent;
import com.michelet.catalog.infrastructure.messaging.dto.ProductStatusChangedEvent;
import com.michelet.catalog.infrastructure.messaging.dto.ProductUpdatedEvent;
import com.michelet.catalog.infrastructure.messaging.dto.StockReservedEvent;
import com.michelet.catalog.infrastructure.messaging.dto.StockRestoredEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
        // 이벤트 객체 자체의 Null 체킹을 통해 안전성 보장 및 DLT 격리 유도
        if (event == null) {
            throw new IllegalArgumentException("이벤트 페이로드가 null입니다.");
        }

        log.info("재고 차감 이벤트 수신: optionId={}, total={}, daily={}",
            event.optionId(), event.totalQuantity(), event.currentDailyStock());

        // 분리된 진입점들을 하나의 내부 공통 업데이트 파이프라인으로 통합 라우팅
        this.updateStockQuantity(
            event.optionId(),
            event.totalQuantity(),
            event.currentDailyStock()
        );
    }

    /**
     * 재고 복구 이벤트를 처리하여 MongoDB 데이터를 갱신함
     */
    public void applyStockRestoredEvent(StockRestoredEvent event) {
        // 객체 자체의 Null 체킹을 통해 안전성 보장 및 DLT 격리 유도
        if (event == null) {
            throw new IllegalArgumentException("이벤트 페이로드가 null입니다.");
        }

        log.info("재고 복구 이벤트 수신: optionId={}, total={}, daily={}",
            event.optionId(), event.totalQuantity(), event.currentDailyStock());

        // 분리된 진입점들을 하나의 내부 공통 업데이트 파이프라인으로 통합 라우팅
        this.updateStockQuantity(
            event.optionId(),
            event.totalQuantity(),
            event.currentDailyStock()
        );
    }

    /**
     * 내부적으로 몽고DB의 특정 상품 서치 및 스냅샷 원자적 갱신을 전담하는 공통 파이프라인
     */
    private void updateStockQuantity(UUID optionId, int totalQuantity, int currentDailyStock) {
        // 1. 해당 옵션을 가지고 있는 상품 문서 찾기
        ProductView productView = productViewRepository.findByOptionId(optionId)
            .orElseThrow(ProductNotFoundException::new);

        // 2. 문서 내의 재고 데이터를 업데이트 (Inventory가 보내준 최종 수치로 덮어쓰기)
        productView.updateStock(
            optionId,
            totalQuantity,
            currentDailyStock
        );

        // 3. MongoDB에 저장 (덮어쓰기)
        productViewRepository.save(productView);
        log.info("MongoDB 재고 상태 최종 동기화 완료: productId={}", productView.getProductId());
    }

    // 상품 업데이트 이벤트 수신 처리
    public void updateProductView(ProductUpdatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("이벤트 페이로드가 null입니다.");
        }

        if (event.productId() == null) {
            throw new IllegalArgumentException("productId는 null일 수 없습니다.");
        }

        ProductView view = productViewRepository.findByProductId(event.productId())
            .orElseThrow(() -> {
                log.warn("상품 정보 업데이트 동기화 실패 (상품 없음): productId={}", event.productId());
                return new ProductNotFoundException();
            });

        view.update(event.name(), event.category(), event.basePrice(), event.attributes());
        productViewRepository.save(view);
        log.info("MongoDB 상품 정보 업데이트 동기화 완료: productId={}", event.productId());
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

        // 풀스캔으로 인한 OOM 방지를 위해 활성화/품절된 타겟 도큐먼트군만 페이징 세그먼트화
        List<String> targetStatuses = List.of("ACTIVE", "SOLDOUT");
        int pageSize = 100;
        PageRequest pageRequest = PageRequest.of(0, pageSize);
        Page<ProductView> page;
        int totalProcessed = 0;

        do {
            // findAll() 대신 인덱스를 타는 커스텀 findByStatusIn 쿼리 메서드 호출
            page = productViewRepository.findByStatusIn(targetStatuses, pageRequest);
            if (page.isEmpty()) {
                break;
            }

            List<ProductView> batch = page.getContent();
            // 성공한 상품만 담을 새로운 리스트(합격자 명단st) 생성
            List<ProductView> successBatch = new ArrayList<>(batch.size());

            for (ProductView product : batch) {
                try {
                    product.resetDailyStock();
                    successBatch.add(product); // 에러 없이 통과한 상품만 리스트에 추가
                } catch (IllegalStateException e) {
                    // 실패한 상품은 로그만 남기고 successBatch에 추가하지 않음 (저장 제외)
                    log.error("일일 재고 리셋 실패 (데이터 정합성 오류) - 부분 갱신 방지를 위해 저장 제외 처리: productId={}, message={}",
                        product.getProductId(), e.getMessage());
                }
            }

            // 무조건 batch를 저장하는 것이 아니라, 검증을 통과한 successBatch만 안전하게 DB에 저장
            productViewRepository.saveAll(successBatch);
            totalProcessed += successBatch.size();

            pageRequest = pageRequest.next();
        } while (page.hasNext());

        log.info("MongoDB 카탈로그 조건별 청크 단위 배치 재고 리셋 완료: {}건", totalProcessed);
    }
}
