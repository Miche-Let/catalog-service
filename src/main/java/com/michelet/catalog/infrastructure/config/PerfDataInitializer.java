package com.michelet.catalog.infrastructure.config;

import com.michelet.catalog.domain.model.ProductView;
import com.michelet.catalog.domain.repository.ProductViewRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("perf")
@RequiredArgsConstructor
public class PerfDataInitializer implements ApplicationRunner {

    private final ProductViewRepository productViewRepository;

    // Order 서비스가 검증할 고정 UUID 타겟
    public static final UUID TEST_OPTION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID TEST_RESTAURANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Override
    public void run(ApplicationArguments args) {
        long count = productViewRepository.count();

        // 1. 부하테스트 주문 타겟 고정 데이터 삽입
        try {
            ProductView.OptionView targetOption = new ProductView.OptionView(
                TEST_OPTION_ID, "타겟 부하테스트 옵션", BigDecimal.ZERO,
                1000000, 1000000, 1000000
            );
            ProductView targetProduct = ProductView.builder()
                .productId(UUID.randomUUID())
                .restaurantId(TEST_RESTAURANT_ID)
                .name("타겟 부하테스트 상품")
                .category("MEALKIT")
                .status("ACTIVE")
                .basePrice(BigDecimal.valueOf(10000))
                .isVisible(true)
                .display(new ProductView.Display(LocalDateTime.now(), LocalDateTime.now().plusYears(1)))
                .options(List.of(targetOption))
                .build();
            productViewRepository.save(targetProduct);
            log.info("[PerfDataInitializer] 카탈로그 타겟 고정 데이터(OptionId: {}) 세팅 완료", TEST_OPTION_ID);
        } catch (Exception e) {
            log.warn("타겟 데이터 세팅 무시 (이미 존재할 수 있음): {}", e.getMessage());
        }

        // 2. 카탈로그 조회 부하용 10만 건 더미 데이터 삽입 (없을 때만)
        if (count < 100000) {
            log.info("[PerfDataInitializer]-catalog : 1차 카탈로그 조회를 위해 10만 건의 더미 도큐먼트를 생성함");
            List<ProductView> products = new ArrayList<>();
            for (int i = 0; i < 100000; i++) {
                ProductView.OptionView option = new ProductView.OptionView(
                    UUID.randomUUID(), "테스트 옵션 " + i, BigDecimal.ZERO,
                    1000, 1000, 1000
                );
                ProductView product = ProductView.builder()
                    .productId(UUID.randomUUID())
                    .restaurantId(UUID.randomUUID())
                    .name("부하테스트 상품 " + i)
                    .category("MEALKIT")
                    .status("ACTIVE")
                    .basePrice(BigDecimal.valueOf(10000))
                    .isVisible(true)
                    .display(new ProductView.Display(LocalDateTime.now(), LocalDateTime.now().plusYears(1)))
                    .options(List.of(option))
                    .build();
                products.add(product);

                if (i % 10000 == 0 && i > 0) {
                    productViewRepository.saveAll(products);
                    products.clear();
                    log.info("... {}건 저장 완료", i);
                }
            }
            if (!products.isEmpty()) {
                productViewRepository.saveAll(products);
            }
            log.info("[PerfDataInitializer] 카탈로그 10만 건 데이터 생성 완료");
        }
    }
}
