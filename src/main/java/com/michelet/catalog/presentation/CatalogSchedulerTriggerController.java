package com.michelet.catalog.presentation;

import com.michelet.catalog.application.CatalogVisibilityScheduler;
import com.michelet.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/scheduler")
@RequiredArgsConstructor
@Profile("!prod")
public class CatalogSchedulerTriggerController {

    private final CatalogVisibilityScheduler catalogVisibilityScheduler;

    // 자정 노출 동기화 스케줄러 강제 갱신 버튼 - 테스트용!
    @PostMapping("/trigger-visibility")
    public ResponseEntity<ApiResponse<String>> triggerVisibility() {
        catalogVisibilityScheduler.syncVisibilityAtMidnight();
        return ResponseEntity.ok(ApiResponse.ok("카탈로그 자정 노출 동기화 스케줄러 작동 완료 (캐시 무효화됨)"));
    }
}
