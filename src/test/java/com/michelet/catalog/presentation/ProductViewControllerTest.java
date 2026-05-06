package com.michelet.catalog.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.michelet.catalog.application.ProductViewQueryService;
import com.michelet.catalog.presentation.dto.ProductViewResponse;
import com.michelet.common.auth.core.context.UserContext;
import com.michelet.common.auth.core.enums.UserRole;
import com.michelet.common.auth.webmvc.aop.AuthorizationAspect;
import com.michelet.common.auth.webmvc.context.UserContextHolder;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = ProductViewController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@EnableAspectJAutoProxy // AOP 기능을 활성화 - 권한 검증 Aspect가 작동하도록 설정
@Import(AuthorizationAspect.class)
class ProductViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductViewQueryService productViewQueryService;

    @AfterEach
    void tearDown() {
        // 다른 테스트에 영향을 주지 않도록 매번 Context 초기화
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("성공: [고객용] 카탈로그 목록 조회 시 제대로 응답한다.")
    void getActiveProducts_PageSerialization_ViaDto() throws Exception {
        // given
        ProductViewResponse response = new ProductViewResponse(
            UUID.randomUUID(), UUID.randomUUID(), "테스트 상품", "카테고리",
            "ACTIVE", BigDecimal.valueOf(15000), null, true, List.of()
        );
        PageRequest pageRequest = PageRequest.of(0, 20);
        given(productViewQueryService.getActiveProducts(eq(pageRequest)))
            .willReturn(new PageImpl<>(List.of(response), pageRequest, 1));

        // when & then
        mockMvc.perform(get("/api/v1/products")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            // VIA_DTO 규약: 실제 데이터는 'content' 배열 내부에 존재
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].name").value("테스트 상품"))
            .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
            // VIA_DTO 규약: 페이징 메타데이터는 'page' 객체 내부에 응집
            .andExpect(jsonPath("$.page.size").value(20));

        then(productViewQueryService).should().getActiveProducts(eq(pageRequest));
    }

    @Test
    @DisplayName("성공: [고객용] size=50(최대 허용값) 요청을 정상 처리한다.")
    void getActiveProducts_MaxPageSizeAccepted() throws Exception {
        // given
        ProductViewResponse response = new ProductViewResponse(
            UUID.randomUUID(), UUID.randomUUID(), "테스트 상품", "카테고리",
            "ACTIVE", BigDecimal.valueOf(15000), null, true, List.of()
        );
        PageRequest pageRequest = PageRequest.of(0, 50);
        given(productViewQueryService.getActiveProducts(eq(pageRequest)))
            .willReturn(new PageImpl<>(List.of(response), pageRequest, 1));

        // when & then
        mockMvc.perform(get("/api/v1/products")
                .param("page", "0")
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size").value(50));

        then(productViewQueryService).should().getActiveProducts(eq(pageRequest));
    }

    @Test
    @DisplayName("성공: [점주용] 올바른 권한(OWNER)이 있을 경우 전체 카탈로그 목록 조회 요청을 정상 처리한다.")
    void getAllProducts_Success() throws Exception {
        // given
        UserContext mockContext = mock(UserContext.class);
        given(mockContext.isAuthenticated()).willReturn(true);
        given(mockContext.hasRole(UserRole.OWNER)).willReturn(true);
        UserContextHolder.set(mockContext);

        ProductViewResponse response = new ProductViewResponse(
            UUID.randomUUID(), UUID.randomUUID(), "숨겨진 테스트 상품", "카테고리",
            "HIDDEN", BigDecimal.valueOf(15000), null, false, List.of()
        );
        PageRequest pageRequest = PageRequest.of(0, 20);

        given(productViewQueryService.getAllProducts(eq(pageRequest)))
            .willReturn(new PageImpl<>(List.of(response), pageRequest, 1));

        // when & then
        mockMvc.perform(get("/api/v1/products/all")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].status").value("HIDDEN"));

        then(productViewQueryService).should().getAllProducts(eq(pageRequest));
    }

    @Test
    @DisplayName("실패: 인증 정보가 없는 상태로 전체 목록 조회 시 401 예외 발생")
    void getAllProducts_Unauthenticated_Returns401() throws Exception {
        // given: UserContextHolder가 빈 상태 (미인증)

        // when & then
        mockMvc.perform(get("/api/v1/products/all")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(result -> {
                // GlobalExceptionHandler가 500으로 바꾸기 전, AOP가 발생시킨 원본 예외를 직접 검증
                Exception ex = result.getResolvedException();
                org.assertj.core.api.Assertions.assertThat(ex)
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
                org.assertj.core.api.Assertions.assertThat(
                    ((org.springframework.web.server.ResponseStatusException) ex).getStatusCode().value()
                ).isEqualTo(401); // 401 UNAUTHORIZED 검증
            });
    }

    @Test
    @DisplayName("실패: OWNER/MASTER 권한이 없는 유저가 전체 목록 조회 시 403 예외 발생")
    void getAllProducts_Unauthorized_Returns403() throws Exception {
        // given: 인증은 되었으나 USER 권한만 가진 상태 설정
        UserContext mockContext = mock(UserContext.class);
        given(mockContext.isAuthenticated()).willReturn(true);
        given(mockContext.hasRole(UserRole.OWNER)).willReturn(false);
        given(mockContext.hasRole(UserRole.MASTER)).willReturn(false);
        UserContextHolder.set(mockContext);

        // when & then
        mockMvc.perform(get("/api/v1/products/all")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(result -> {
                Exception ex = result.getResolvedException();
                org.assertj.core.api.Assertions.assertThat(ex)
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
                org.assertj.core.api.Assertions.assertThat(
                    ((org.springframework.web.server.ResponseStatusException) ex).getStatusCode().value()
                ).isEqualTo(403); // 403 FORBIDDEN 검증
            });
    }
}
