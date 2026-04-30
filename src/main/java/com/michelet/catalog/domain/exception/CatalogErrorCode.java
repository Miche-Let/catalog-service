package com.michelet.catalog.domain.exception;

import com.michelet.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CatalogErrorCode implements ErrorCode {
    
    PRODUCT_NOT_FOUND(404, "PRODUCT_001", "해당 상품을 찾을 수 없습니다.");

    private final int httpStatus;
    private final String code;
    private final String message;
}
