package com.michelet.catalog.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductQueryService {

    public String getHealthStatus() {
        return "Catalog Query Service is Healthy";
    }
}