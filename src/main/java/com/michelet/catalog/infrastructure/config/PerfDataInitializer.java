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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("perf")
@RequiredArgsConstructor
public class PerfDataInitializer implements ApplicationRunner {

    private final ProductViewRepository productViewRepository;

    // Order 서비스가 검증할 고정 UUID 타겟
    public static final UUID TEST_PRODUCT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555"); // 고정 상품 ID
    public static final UUID TEST_OPTION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID TEST_RESTAURANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Override
    public void run(ApplicationArguments args) {
        long currentCount = productViewRepository.count();

        // 1. 부하테스트 주문 타겟 고정 데이터 삽입
        try {
            // DB에 이미 타겟 데이터가 있는지 검사하여 중복 적재 차단!
            boolean targetExists = false;
            try {
                targetExists = productViewRepository.findByProductId(TEST_PRODUCT_ID).isPresent();
            } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
                targetExists = true; // 이미 2개 이상 복제되어 있다면 존재하는 것으로 취급
            }

            if (!targetExists) {
                ProductView.OptionView targetOption = new ProductView.OptionView(
                    TEST_OPTION_ID, "타겟 부하테스트 옵션", BigDecimal.ZERO,
                    1000000, 1000000, 1000000
                );
                ProductView targetProduct = ProductView.builder()
                    .productId(TEST_PRODUCT_ID) // 랜덤 UUID 대신 고정 ID 사용
                    .restaurantId(TEST_RESTAURANT_ID)
                    .name("타겟 부하테스트 상품")
                    .category("MEALKIT")
                    .status("ACTIVE")
                    .basePrice(BigDecimal.valueOf(10000))
                    .isVisible(true)
                    .display(new ProductView.Display(LocalDateTime.now(), LocalDateTime.now().plusYears(1)))
                    .options(List.of(targetOption))
                    .build();

                // TOCTOU 동시성 예외 방어를 위한 save 단위 try-catch
                try {
                    productViewRepository.save(targetProduct);
                    log.info("[PerfDataInitializer] 카탈로그 타겟 고정 데이터(OptionId: {}) 세팅 완료", TEST_OPTION_ID);
                } catch (DuplicateKeyException e) {
                    log.info("[PerfDataInitializer] 동시 기동으로 인해 타겟 데이터가 이미 존재하여 생략합니다: {}", TEST_PRODUCT_ID);
                }
            } else {
                log.info("[PerfDataInitializer] 타겟 데이터가 이미 존재하여 생략합니다: {}", TEST_PRODUCT_ID);
            }
        } catch (Exception e) {
            // 그 외의 에러(DB 다운 등)는 삼키지 않고 던짐
            log.error("타겟 데이터 세팅 중 예상치 못한 에러 발생", e);
            throw e;
        }

        // 2. 카탈로그 조회 부하용 10만 건 더미 데이터 삽입
        // 무조건 10만 개가 아니라 '부족한 개수(remaining)'만 계산해서 추가
        long remaining = Math.max(0, 100000 - currentCount);
        if (remaining > 0) {
            log.info("[PerfDataInitializer]-catalog : 1차 카탈로그 조회를 위해 {}건의 더미 도큐먼트를 생성함", remaining);
            List<ProductView> products = new ArrayList<>();
            for (int i = 0; i < remaining; i++) {
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

                // i % 10000 대신 명확하게 리스트 사이즈로 체크
                if (products.size() == 10000) {
                    productViewRepository.saveAll(products);
                    products.clear();
                    log.info("... 누적 {}건 저장 완료", i + 1);
                }
            }
            // 남은 찌꺼기 데이터가 있으면 마저 저장
            if (!products.isEmpty()) {
                productViewRepository.saveAll(products);
            }
            log.info("[PerfDataInitializer] 카탈로그 데이터 생성 완료 (현재 총 {}건)", productViewRepository.count());
        }
    }
}
