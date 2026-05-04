package com.michelet.catalog.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.michelet.catalog.application.ProductViewQueryService;
import com.michelet.catalog.presentation.dto.ProductViewResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductViewController.class)
class ProductViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductViewQueryService productViewQueryService;

    @Test
    @DisplayName("성공: 카탈로그 목록 조회 시 VIA_DTO 페이지 직렬화 규약을 준수하여 응답한다.")
    void getProducts_PageSerialization_ViaDto() throws Exception {
        // given
        ProductViewResponse response = new ProductViewResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "테스트 상품",
            "카테고리",
            "ACTIVE",
            BigDecimal.valueOf(15000),
            null,
            true,
            List.of()
        );
        PageRequest pageRequest = PageRequest.of(0, 20);
        // 1개의 요소를 가진 페이지 Mock 응답 생성
        given(productViewQueryService.getProducts(eq(pageRequest)))
            .willReturn(new PageImpl<>(List.of(response), pageRequest, 1));

        // when & then
        mockMvc.perform(get("/api/v1/products")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            // 1. VIA_DTO 규약: 실제 데이터는 'content' 배열 내부에 존재
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].name").value("테스트 상품"))
            .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.content[0].basePrice").value(15000))
            // 2. VIA_DTO 규약: 페이징 메타데이터는 'page' 객체 내부에 응집
            .andExpect(jsonPath("$.page.size").value(20))
            .andExpect(jsonPath("$.page.number").value(0))
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.page.totalPages").value(1));

        then(productViewQueryService).should().getProducts(eq(pageRequest));
    }

    @Test
    @DisplayName("성공: size=50(최대 허용값) 요청을 정상 처리한다.")
    void getProducts_MaxPageSizeAccepted() throws Exception {
        // given
        ProductViewResponse response = new ProductViewResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "테스트 상품",
            "카테고리",
            "ACTIVE",
            BigDecimal.valueOf(15000),
            null,
            true,
            List.of()
        );
        PageRequest pageRequest = PageRequest.of(0, 50);
        given(productViewQueryService.getProducts(eq(pageRequest)))
            .willReturn(new PageImpl<>(List.of(response), pageRequest, 1));

        // when & then
        mockMvc.perform(get("/api/v1/products")
                .param("page", "0")
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.page.size").value(50));

        then(productViewQueryService).should().getProducts(eq(pageRequest));
    }
}
