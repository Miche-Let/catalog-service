package com.michelet.catalog.presentation;

import com.michelet.catalog.application.ProductViewQueryService;
import com.michelet.catalog.presentation.dto.OptionValidationResponse;
import com.michelet.common.response.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/options")
@RequiredArgsConstructor
public class InternalOptionController {

    private final ProductViewQueryService productViewQueryService;

    // FIXME 임시적용 - common webmvc 수정되면 여기도 수정
    @GetMapping("/{optionId}/validate")
    public ResponseEntity<ApiResponse<OptionValidationResponse>> validateOption(
        @RequestHeader(value = "X-User-Role", required = true) String userRole,
        @PathVariable UUID optionId
    ) {
        if (!"SYSTEM".equals(userRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "시스템 내부 통신만 접근 가능합니다.");
        }

        OptionValidationResponse response = productViewQueryService.validateOptionInternal(optionId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
