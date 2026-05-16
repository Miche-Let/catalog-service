package com.michelet.catalog.domain.repository;

import com.michelet.catalog.domain.model.ProductView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ProductViewRepository extends MongoRepository<ProductView, String> {

    @Query("{ 'is_visible': true }")
    Page<ProductView> findAllVisibleProducts(Pageable pageable);

    Optional<ProductView> findByProductId(UUID productId);

    @Query("{ 'options.option_id': ?0 }")
    Optional<ProductView> findByOptionId(UUID optionId);

    Page<ProductView> findByStatusIn(List<String> statuses, Pageable pageable);
}
