package com.michelet.catalog.application;

import com.michelet.catalog.domain.exception.ProductNotFoundException;
import com.michelet.catalog.domain.repository.ProductViewRepository;
import com.michelet.catalog.presentation.dto.ProductViewResponse;
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
}
