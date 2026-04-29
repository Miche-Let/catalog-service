package com.michelet.catalog.domain.repository;

import com.michelet.catalog.domain.model.ProductView;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductViewRepository extends MongoRepository<ProductView, String> {

    // @Query 어노테이션 제거함!
    // Spring Data MongoDB가 'options' 배열 안의 'optionId' 필드를 자동으로 찾아줌
    Optional<ProductView> findByOptionsOptionId(UUID optionId);
}
