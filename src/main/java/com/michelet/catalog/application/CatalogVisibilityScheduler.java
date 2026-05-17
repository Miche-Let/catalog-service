package com.michelet.catalog.application;

import com.michelet.catalog.domain.model.ProductView;
import com.michelet.catalog.domain.repository.ProductViewRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.OptimisticLockingFailureException;
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
    private final CacheManager catalogCacheManager;

    // 매일 새벽 12시 25분(00:25:00)에 실행. beforeInvocation=true로 실행 직전 1차 캐시 비움
    @Scheduled(cron = "0 25 0 * * *", zone = "Asia/Seoul")
    // 다중 인스턴스 환경에서 스케줄러 중복 실행 방지 (ShedLock)
    @SchedulerLock(
        name = "syncVisibilityAtMidnight",
        lockAtLeastFor = "PT5M", // 작업이 5분 내로 끝나도 락을 5분간 유지
        lockAtMostFor = "PT25M" // 25분 후 Redis에서 자동으로 락을 해제
    )
    @CacheEvict(value = "products_cache", allEntries = true, cacheManager = "catalogCacheManager", beforeInvocation = true)
    public void syncVisibilityAtMidnight() {
        log.info("[Catalog Batch] 일일 isVisible 일괄 동기화 스케줄러 시작");

        int totalUpdated = 0;

        try {
            int pageSize = 500;
            // OOM 방지를 위해 500건씩 청크 단위로 나누어 스캔 (productId 오름차순 정렬)
            PageRequest pageRequest = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "productId"));
            Page<ProductView> page;

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

                // 변경된 데이터만 선별하여 MongoDB에 저장 (낙관적 락 충돌 폴백 처리 적용)
                if (!updateBatch.isEmpty()) {
                    try {
                        productViewRepository.saveAll(updateBatch);
                        totalUpdated += updateBatch.size();
                    } catch (OptimisticLockingFailureException e) {
                        log.warn("[Catalog Batch] 배치 저장 중 낙관적 락 충돌 발생. 단건 저장으로 폴백(Fallback)합니다.", e);
                        for (ProductView product : updateBatch) {
                            try {
                                // 최신 버전을 DB에서 다시 조회하여 업데이트 (Stale Entity 재사용 방지)
                                productViewRepository.findByProductId(product.getProductId())
                                    .ifPresent(freshProduct -> {
                                        boolean targetVisibility = "ACTIVE".equals(freshProduct.getStatus());
                                        if (freshProduct.isVisible() != targetVisibility) {
                                            freshProduct.updateVisibility(targetVisibility);
                                            productViewRepository.save(freshProduct);
                                        }
                                    });
                                totalUpdated++;
                            } catch (Exception innerException) {
                                log.error("[Catalog Batch] 상품 노출 상태 단건 업데이트 실패. productId: {}", product.getProductId(),
                                    innerException);
                            }
                        }
                    }
                }

                pageRequest = pageRequest.next();
            } while (page.hasNext());

            log.info("[Catalog Batch] 총 {}개의 상품 노출 상태(isVisible) 동기화 완료", totalUpdated);

        } finally {
            // 배치 실행 도중 유저 조회로 인해 오염되었을 수 있는 캐시를 마지막에 확실하게 정리
            Cache cache = catalogCacheManager.getCache("products_cache");
            if (cache != null) {
                cache.clear();
                log.info("[Catalog Batch] 배치 종료 후 products_cache 명시적 초기화 완료 (Stale 데이터 방지)");
            }
        }
    }
}
