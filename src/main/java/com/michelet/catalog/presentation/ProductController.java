package com.michelet.catalog.presentation;

import com.michelet.catalog.application.ProductQueryService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductQueryService productQueryService;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "success", true,
            "data", productQueryService.getHealthStatus(),
            "message", "Catalog Query Service is running"
        );
    }
}
