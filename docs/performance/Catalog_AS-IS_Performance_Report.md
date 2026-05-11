# 📊 [Catalog Service] AS-IS 아키텍처 성능 병목 리포트 (1차 부하테스트)

본 문서는 현재 카탈로그 시스템(AS-IS) 아키텍처가 대규모 트래픽을 맞이했을 때 발생하는 **읽기(Read) 병목 현상**을 수치와 로그로 증명한 1차 성능 테스트 리포트입니다.

---

## 1. 카탈로그 읽기(Read) 성능 한계 증명

> **테스트 목적:** 캐시(Redis)가 적용되지 않은 상태에서 대량의 Read 트래픽이 디스크 기반 DB(MongoDB)로 직접 향할 때 발생하는 I/O 지연 증명

### 📌 더미 데이터 세팅 및 JMeter 타격 결과

- **환경:** MongoDB 내 `p_product_views` 10만 건 더미 데이터 세팅 완료 (`PerfDataInitializer` 사용)
- **요청 타겟:** `GET /api/v1/products` (카탈로그 메인 API)
- **부하 규모:** 300 Threads, Loop 20 (총 6,000건 요청)
- **결과 요약:**
    - **평균 지연 시간(Average):** `1,962ms` (약 2초)
    - **최대 지연 시간(Max):** `4,146ms` (약 4초)
- **분석:** 단순 조회 API임에도 매번 디스크(DB)를 직접 스캔하므로 Disk I/O 병목이 발생하여 평균 2초라는 치명적인 지연이 발생했습니다. 실 서비스의 메인 화면 로딩 속도로는 부적합한 수치입니다.

<img width="855" height="227" alt="read_catalog_as-is_jmeter" src="https://github.com/user-attachments/assets/07f88a8e-87b1-4a73-b6aa-ddc94f4c5b4b" />

---

## 💡 종합 결론 및 향후 작업방향 (TO-BE 기획)

1. **읽기 성능 단축:** 카탈로그 메인 조회 API에 **Redis 인메모리 캐시**를 도입하여 Disk I/O 병목을 제거합니다.
2. **목표 수치:** 캐시 히트(Cache Hit)를 통해 기존 평균 2초(2000ms) 대의 지연 시간을 **10ms 이하로 단축**하는 것을 목표로 합니다.
