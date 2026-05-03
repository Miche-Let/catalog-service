package com.michelet.catalog.domain.exception;

import com.michelet.common.exception.BusinessException;

public class ProductNotVisibleException extends BusinessException {

    public ProductNotVisibleException() {
        super(CatalogErrorCode.PRODUCT_NOT_VISIBLE);
    }
}
