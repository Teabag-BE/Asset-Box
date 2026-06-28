# 파트 가이드 — Infra (단독, PM 보조)

> ⚠️ **2026-05-22 회의 반영** — 본 문서는 회의 결과 패치본. 회의록 단일 진실 원본은 [`../04_api/02_엔드포인트_목록_상태코드분리_수정본.md`](../04_api/02_엔드포인트_목록_상태코드분리_수정본.md).



> 모르는 단어는 [`../00_overview/01_용어집.md`](../00_overview/01_용어집.md) 참고.
> 비유: **건물 관리실.** 전기·수도·CCTV·소방을 책임진다. 각 가게(도메인)는 자기 영업만 신경 쓰고, 인프라는 그들 모두가 멈추지 않게 하는 토대.

---

## 1. 책임 한 줄

빌드/배포/CI·CD/Docker/CORS/예외/시드/모니터링, 그리고 **DB 스키마 게이트키핑**. 도메인 코드는 손대지 않는다. 도메인 PR이 안전한지 인프라 관점에서 검토.

---

## 2. 패키지 / 파일 소유권

### 내 소유 (자유 수정)
```
assetbox/
├─ build.gradle
├─ Dockerfile
├─ .github/workflows/*           (CI/CD)
├─ src/main/resources/
│   ├─ application.yml
│   ├─ application-dev.yml
│   └─ application-prod.yml
└─ src/main/java/com/assetbox/common/
   ├─ config/
   │   ├─ SecurityConfig.java         (User와 공동 — 인증 규칙은 User 협의)
   │   ├─ WebMvcConfig.java
   │   ├─ WebSocketConfig.java        (DM과 공동 — 토픽/엔드포인트는 DM 협의)
   │   ├─ AsyncConfig.java            ★ 추가 책임
   │   └─ AdminBootstrapRunner.java
   ├─ dto/ApiResponse.java
   ├─ exception/
   │   ├─ BusinessException.java
   │   └─ GlobalExceptionHandler.java
   └─ BaseEntity.java
docker-compose.yml
prometheus.yml
```

### 절대 손대지 말 것
- 어느 도메인의 controller/service/repository/domain/dto

---

## 3. 외부 컨트랙트 (내가 제공)

| 자원 | 무엇 |
|---|---|
| `ApiResponse<T>` | 모든 응답 표준 래퍼 — 모든 도메인이 사용 |
| `BusinessException.notFound/badRequest/unauthorized/forbidden` | 모든 도메인이 사용 |
| `BaseEntity` | 모든 엔티티가 상속 |
| `@Async` 풀 (`AsyncConfig`) | v1.1 비동기 작업 대비. MVP에서는 필수 사용처 없음 |
| 환경변수: `JWT_SECRET, CORS_ALLOWED_ORIGINS, DB_URL, ...` | 도메인 코드에서 직접 시스템 환경변수 읽지 말 것 → application.yml 거치기 |

---

## 4. 의존하는 컨트랙트

- (없음) — 인프라는 기반.

---

## 5. 단계별 작업 가이드

### M0 (5/22 ~ 5/25): 코드·문서 정독
- [ ] 현재 `build.gradle` 의 `--add-opens` 블록 보존 확인 (Lombok + JDK 21 호환)
- [ ] `application-dev.yml` (H2, create-drop) / `application-prod.yml` (MySQL, validate) 분리 확인
- [ ] CI 워크플로우: PR 시 `./gradlew clean build` 통과 보장

### M1 (5/27 ~ 6/3): MVP 인프라
- [ ] Docker Compose dev 환경 (백+프+H2 또는 MySQL 옵셔널)
- [ ] CORS 화이트리스트 (`CORS_ALLOWED_ORIGINS`) 환경변수
- [ ] GlobalExceptionHandler — 모든 예외 → ApiResponse 매핑
- [ ] AdminBootstrapRunner — 시스템 유저(이메일 `system@assetbox.local`) 보장 (DM 협업)
- [ ] AsyncConfig — v1.1 대비 기본 빈만 준비
- [ ] 통합 테스트 데이(6/1) — Docker compose up 으로 한 사이클
- [ ] 각 도메인 PR이 머지된 직후, build/CI 통과 모니터링

### M2 (6/4 ~ 6/11, 6/9 베타)
- [ ] Prometheus + `/actuator/prometheus` 노출
- [ ] 운영 프로파일 prod 검증 (MySQL 연결, ddl=validate)
- [ ] **6/8 배포 리허설** 주관
- [ ] **6/9 TA반 베타 배포** 주관
- [ ] 6/10~6/11 베타 안정화 지원 (6/11 KMF 일정 고려)

### M3 (6/12 ~ 6/16)
- [ ] 메트릭 대시보드 (Prometheus 쿼리 예시 문서화)
- [ ] 로그 레벨 / 로테이션 점검
- [ ] 6/15 회고 호스트 (PM과 공동)

---

## 6. 인수 기준 (AC)

### I-01 Docker
- [ ] `docker compose up` 한 명령으로 백+프+DB 동시 기동
- [ ] 환경변수만으로 origins/jwt/db 교체 가능

### I-02 CI
- [ ] PR이 열리면 자동으로 빌드/테스트 실행
- [ ] 빌드 실패 PR 머지 차단

### I-04 예외 매핑
- [ ] `BusinessException` → ApiResponse.error + 정확한 HTTP status
- [ ] `MethodArgumentNotValidException` → 400 + 필드별 에러
- [ ] 그 외 RuntimeException → 500 + 코드 `INTERNAL_ERROR` (스택트레이스 비노출)

### I-05 Prometheus
- [ ] `/actuator/prometheus` 가 익명 차단 + IP 또는 토큰 게이트 (prod)
- [ ] 기본 JVM/HTTP 메트릭 수집 확인

---

## 7. 충돌 방지 / 함정

| 함정 | 결과 | 회피 |
|---|---|---|
| `SecurityConfig` 의 requestMatchers 를 다른 도메인이 마음대로 추가 | 인증 규칙 혼선 | "SecurityConfig 변경 PR은 Infra+User 리뷰 필수" 규칙 |
| 도메인 PR에 DDL 영향이 있는데 Infra 리뷰 누락 | 운영 깨짐 | 엔티티 변경 PR 은 작성자가 Infra 를 리뷰어로 수동 추가. PM 이 매일 스탠드업에서 누락 점검 |
| ddl=create-drop 인 채로 배포 | 데이터 날아감 | prod 프로파일 검사 — 부팅 로그에 명시 |
| `--add-opens` 블록을 누가 지움 | JDK 21+ 컴파일 깨짐 | CLAUDE.md + 본 가이드에 명시. PR 리뷰 시 자동 grep |
| 환경변수 누락된 채 prod 부팅 | 침묵 실패 | `application-prod.yml` 에서 필수 키는 `${VAR:?required}` 형태로 |
| 시스템 발신자 유저 누락 | DM 알림 깨짐 | AdminBootstrapRunner에서 보장 + 통합 테스트로 검증 |

---

## DDL 게이트키퍼 체크리스트 (엔티티 변경 PR 리뷰 시)

- [ ] 새 컬럼이 nullable / default 가 있는가? (prod 무중단 전제는 v1.1이지만 학습용으로 의식)
- [ ] FK / UNIQUE 추가 시 기존 데이터와 충돌하지 않는가?
- [ ] 인덱스 추가 비용은 합리적인가?
- [ ] 인접 도메인의 응답 DTO에 영향은 없는가?
- [ ] CategorySeeder / AdminBootstrapRunner 가 새 데이터를 채워야 하는가?
