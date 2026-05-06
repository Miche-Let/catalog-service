package com.michelet.catalog.presentation;

import com.michelet.catalog.application.ProductViewQueryService;
import com.michelet.catalog.presentation.dto.ProductViewResponse;
import com.michelet.common.auth.core.annotation.RequireRole;
import com.michelet.common.auth.core.enums.UserRole;
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

    // 일반 고객용 목록 조회 API (노출 중인 상품만)
    @GetMapping
    public ResponseEntity<Page<ProductViewResponse>> getActiveProducts(
        @PageableDefault(size = 10) Pageable pageable) {
        validatePageSize(pageable);
        return ResponseEntity.ok(productViewQueryService.getActiveProducts(pageable));
    }

    // 관리자/점주용 전체 목록 조회 API (숨김, 품절 포함 전체 상품)
    @GetMapping("/all")
    @RequireRole({UserRole.OWNER, UserRole.MASTER})
    public ResponseEntity<Page<ProductViewResponse>> getAllProducts(
        @PageableDefault(size = 10) Pageable pageable) {
        validatePageSize(pageable);
        return ResponseEntity.ok(productViewQueryService.getAllProducts(pageable));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductViewResponse> getProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(productViewQueryService.getProduct(productId));
    }

    private void validatePageSize(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("한 번에 최대 " + MAX_PAGE_SIZE + "개까지만 조회할 수 있습니다.");
        }
    }
}
