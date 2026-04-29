package com.michelet.catalog.application;

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
        return productViewRepository.findByProductId(productId)
            .map(ProductViewResponse::from)
            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));
    }
}
