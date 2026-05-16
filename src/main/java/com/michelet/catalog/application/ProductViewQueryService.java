package com.michelet.catalog.application;

import com.michelet.catalog.domain.exception.ProductNotFoundException;
import com.michelet.catalog.domain.exception.ProductNotVisibleException;
import com.michelet.catalog.domain.model.ProductView;
import com.michelet.catalog.domain.repository.ProductViewRepository;
import com.michelet.catalog.presentation.dto.OptionValidationResponse;
import com.michelet.catalog.presentation.dto.ProductViewResponse;
import com.michelet.catalog.presentation.dto.RestPageImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductViewQueryService {

    private final ProductViewRepository productViewRepository;

    // 고객용 - 노출 중인 상품만 조회
    // Cache-Aside 패턴 도입: 페이지 번호 단위로 독립적 캐시 생성 관리 - 페이지 크기와 정렬 조건을 캐시 키에 포함하여 충돌 방지
    @Cacheable(
        value = "products_cache",
        key = "T(java.lang.String).format('%d-%d-%s', #pageable.pageNumber, #pageable.pageSize, #pageable.sort.toString())",
        cacheManager = "catalogCacheManager"
    )
    public Page<ProductViewResponse> getActiveProducts(Pageable pageable) {
        log.info("[Cache Miss] 몽고DB로 직행하여 노출 상품 목록 조회 활성화 - Page: {}", pageable.getPageNumber());

        Page<ProductView> page = productViewRepository.findAllVisibleProducts(pageable);
        List<ProductViewResponse> content = page.getContent().stream()
            .map(ProductViewResponse::from)
            .toList();

        // 500 역직렬화 에러를 막기 위해 일반 PageImpl 대신 커스텀 RestPageImpl 객체에 담아 반환!
        return new RestPageImpl<>(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    // 점주/관리자용 - 상태 상관없이 전체 상품 조회 (캐시 없음)
    public Page<ProductViewResponse> getAllProducts(Pageable pageable) {
        return productViewRepository.findAll(pageable)
            .map(ProductViewResponse::from);
    }

    public ProductViewResponse getProduct(UUID productId) {
        // Application 계층이 Web 계층의 상태 코드를 직접 반환하지 않도록... 순수 도메인 예외를 정의해 던짐
        return productViewRepository.findByProductId(productId)
            .map(ProductViewResponse::from)
            .orElseThrow(ProductNotFoundException::new);
    }

    // 내부 통신용 옵션 유효성 검증 로직
    public OptionValidationResponse validateOptionInternal(UUID optionId) {
        // 1. 해당 옵션을 포함하는 상품 뷰 조회 (없으면 예외)
        ProductView productView = productViewRepository.findByOptionId(optionId)
            .orElseThrow(ProductNotFoundException::new);

        // 2. 전시 상태 확인
        if (!productView.isVisible()) {
            throw new ProductNotVisibleException();
        }

        // 옵션 리스트 Null 및 Empty 방어
        if (productView.getOptions() == null || productView.getOptions().isEmpty()) {
            throw new ProductNotFoundException();
        }

        // 3. 옵션 상세 정보 추출
        ProductView.OptionView option = productView.getOptions().stream()
            .filter(o -> o.getOptionId().equals(optionId))
            .findFirst()
            .orElseThrow(ProductNotFoundException::new);

        // DB 조회 값이 Null일 경우 기본값(0) 처리하여 NPE 차단
        BigDecimal basePrice = productView.getBasePrice() != null ? productView.getBasePrice() : BigDecimal.ZERO;
        BigDecimal addPrice = option.getAddPrice() != null ? option.getAddPrice() : BigDecimal.ZERO;

        // 4. 총 가격(기본가 + 추가금액) 계산하여 반환
        BigDecimal totalPrice = basePrice.add(addPrice);
        return new OptionValidationResponse(option.getOptionId(), option.getName(), totalPrice);
    }
}
