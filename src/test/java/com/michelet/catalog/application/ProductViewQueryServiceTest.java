package com.michelet.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.michelet.catalog.domain.exception.ProductNotFoundException;
import com.michelet.catalog.domain.model.ProductView;
import com.michelet.catalog.domain.repository.ProductViewRepository;
import com.michelet.catalog.presentation.dto.ProductViewResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ProductViewQueryServiceTest {

    @InjectMocks
    private ProductViewQueryService productViewQueryService;

    @Mock
    private ProductViewRepository productViewRepository;

    @Test
    @DisplayName("성공: 카탈로그 상품 목록을 페이징하여 조회할 수 있다.")
    void getProducts_Success() {
        // given
        UUID productId = UUID.randomUUID();
        ProductView.OptionView option = new ProductView.OptionView(UUID.randomUUID(), "기본", BigDecimal.ZERO, 100, 20);
        ProductView productView = ProductView.builder()
            .productId(productId)
            .name("밀키트 A")
            .isVisible(true)
            .options(List.of(option))
            .build();

        PageRequest pageRequest = PageRequest.of(0, 10);
        given(productViewRepository.findAll(pageRequest)).willReturn(new PageImpl<>(List.of(productView)));

        // when
        Page<ProductViewResponse> result = productViewQueryService.getProducts(pageRequest);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).productId()).isEqualTo(productId);
//        assertThat(result.getContent().get(0).name()).isEqualTo("밀키트 A");
//        assertThat(result.getContent().get(0).options().get(0).currentDailyStock()).isEqualTo(20);
    }

    @Test
    @DisplayName("성공: 특정 ID의 상품을 단건 조회할 수 있다.")
    void getProduct_Success() {
        // given
        UUID productId = UUID.randomUUID();
        ProductView.OptionView option = new ProductView.OptionView(UUID.randomUUID(), "기본", BigDecimal.ZERO, 100, 20);
        ProductView productView = ProductView.builder()
            .productId(productId)
            .name("단건 상품")
            .isVisible(true)
            .options(List.of(option))
            .build();

        given(productViewRepository.findByProductId(productId)).willReturn(Optional.of(productView));

        // when
        ProductViewResponse result = productViewQueryService.getProduct(productId);

        // then
        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.name()).isEqualTo("단건 상품");
    }

    @Test
    @DisplayName("실패: 존재하지 않는 상품 ID로 조회하면 ProductNotFoundException이 발생한다.")
    void getProduct_NotFound_ThrowsException() {
        // given
        UUID productId = UUID.randomUUID();
        given(productViewRepository.findByProductId(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productViewQueryService.getProduct(productId))
            .isInstanceOf(ProductNotFoundException.class)
            .hasMessageContaining("상품을 찾을 수 없습니다");
    }
}
