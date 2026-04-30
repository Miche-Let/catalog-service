package com.michelet.catalog.presentation;

import com.michelet.catalog.application.ProductViewQueryService;
import com.michelet.catalog.presentation.dto.ProductViewResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductViewController {

    private final ProductViewQueryService productViewQueryService;

    private static final int MAX_PAGE_SIZE = 50;

    @GetMapping
    public ResponseEntity<Page<ProductViewResponse>> getProducts(
        @PageableDefault(size = 10) Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("한 번에 최대 " + MAX_PAGE_SIZE + "개까지만 조회할 수 있습니다.");
        }

        return ResponseEntity.ok(productViewQueryService.getProducts(pageable));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductViewResponse> getProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(productViewQueryService.getProduct(productId));
    }
}
