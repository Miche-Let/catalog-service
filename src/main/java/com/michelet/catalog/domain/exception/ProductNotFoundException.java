package com.michelet.catalog.domain.exception;

import com.michelet.common.exception.BusinessException;

public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException() {
        super(CatalogErrorCode.PRODUCT_NOT_FOUND);
    }
}
