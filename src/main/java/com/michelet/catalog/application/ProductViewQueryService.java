package com.michelet.catalog.application;

import com.michelet.catalog.domain.exception.ProductNotFoundException;
import com.michelet.catalog.domain.exception.ProductNotVisibleException;
import com.michelet.catalog.domain.model.ProductView;
import com.michelet.catalog.domain.repository.ProductViewRepository;
import com.michelet.catalog.presentation.dto.OptionValidationResponse;
import com.michelet.catalog.presentation.dto.ProductViewResponse;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductViewQueryService {

    private final ProductViewRepository productViewRepository;

    public Page<ProductViewResponse> getProducts(Pageable pageable) {
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
        ProductView productView = productViewRepository.findByOptionsOptionId(optionId)
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
