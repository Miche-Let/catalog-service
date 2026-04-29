package com.michelet.catalog.presentation;

import com.michelet.catalog.application.ProductViewQueryService;
import com.michelet.catalog.presentation.dto.ProductViewResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @GetMapping
    public ResponseEntity<Page<ProductViewResponse>> getProducts(Pageable pageable) {
        return ResponseEntity.ok(productViewQueryService.getProducts(pageable));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductViewResponse> getProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(productViewQueryService.getProduct(productId));
    }
}
