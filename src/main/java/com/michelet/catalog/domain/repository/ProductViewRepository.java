package com.michelet.catalog.domain.repository;

import com.michelet.catalog.domain.model.ProductView;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductViewRepository extends MongoRepository<ProductView, String> {
    Optional<ProductView> findByOptionsOptionId(UUID optionId);

    Optional<ProductView> findByProductId(UUID productId);

    Page<ProductView> findByIsVisibleTrue(Pageable pageable);
}
