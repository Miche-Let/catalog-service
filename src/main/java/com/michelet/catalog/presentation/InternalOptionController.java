package com.michelet.catalog.presentation;

import com.michelet.catalog.application.ProductViewQueryService;
import com.michelet.catalog.presentation.dto.OptionValidationResponse;
import com.michelet.common.response.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/options")
@RequiredArgsConstructor
public class InternalOptionController {

    private final ProductViewQueryService productViewQueryService;

    @GetMapping("/{optionId}/validate")
    public ResponseEntity<ApiResponse<OptionValidationResponse>> validateOption(
        @PathVariable UUID optionId
    ) {
        OptionValidationResponse response = productViewQueryService.validateOptionInternal(optionId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
