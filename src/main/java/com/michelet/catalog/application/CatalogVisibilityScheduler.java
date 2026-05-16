package com.michelet.catalog.application;

import com.michelet.catalog.domain.model.ProductView;
import com.michelet.catalog.domain.repository.ProductViewRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogVisibilityScheduler {

    private final ProductViewRepository productViewRepository;

    // 매일 자정(00:00:00)에 실행되며 캐시를 한 번에 폭파함
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @CacheEvict(value = "products_cache", allEntries = true, cacheManager = "catalogCacheManager")
    public void syncVisibilityAtMidnight() {
        log.info("[Catalog Batch] 자정 isVisible 일괄 동기화 스케줄러 시작");

        int pageSize = 500;
        // OOM 방지를 위해 500건씩 청크 단위로 나누어 스캔 (productId 오름차순 정렬)
        PageRequest pageRequest = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "productId"));
        Page<ProductView> page;
        int totalUpdated = 0;

        do {
            page = productViewRepository.findAll(pageRequest);
            if (page.isEmpty()) {
                break;
            }

            List<ProductView> batch = page.getContent();
            List<ProductView> updateBatch = new ArrayList<>();

            for (ProductView product : batch) {
                // ACTIVE 상태인 상품만 true, 나머지는 false여야 함
                boolean targetVisibility = "ACTIVE".equals(product.getStatus());

                // 실제 노출 상태와 목표 상태가 다를 때만 업데이트 대상에 추가
                if (product.isVisible() != targetVisibility) {
                    product.updateVisibility(targetVisibility);
                    updateBatch.add(product);
                }
            }

            // 변경된 데이터만 선별하여 MongoDB에 저장
            if (!updateBatch.isEmpty()) {
                productViewRepository.saveAll(updateBatch);
                totalUpdated += updateBatch.size();
            }

            pageRequest = pageRequest.next();
        } while (page.hasNext());

        log.info("[Catalog Batch] 총 {}개의 상품 노출 상태(isVisible) 동기화 및 캐시 무효화 완료", totalUpdated);
    }
}
