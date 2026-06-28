# 파트 가이드 — User (단독)

> ⚠️ **2026-05-26 회의 반영** — 본 문서는 5/26 ERD 변경 기준 패치본.

## 회의 반영 핵심 변경 (User 파트)

| 영역 | 새 내용 |
|---|---|
| **인증 방식** | Form + **Google OAuth + Naver OAuth** 병행. 구현 순서: Form → Google → Naver |
| **이메일 화이트리스트** | 가입 가능 이메일 목록을 `application.yml` 또는 환경변수로 관리. 미포함 시 403 `USER_EMAIL_NOT_WHITELISTED` |
| **신규 필드** | `name` (NotBlank), `provider` (`LOCAL/GOOGLE/NAVER`), `major` (NULL 허용 — 보완 페이지에서 입력) |
| **제거 필드** | `bio` 는 User 엔티티/응답/수정 API에서 제외 |
| **보완 페이지 강제** | `major == null` 사용자는 로그인 후 다른 페이지 차단. 403 `USER_PROFILE_INCOMPLETE` |
| **수정 권한** | 일반 USER는 `name / major / provider` 직접 수정 불가. 운영자(SUPER_ADMIN) 만 가능 |
| **OAuth 유저 password** | NULL 허용. `provider`, `provider_subject` 컬럼 추가 |
| **제외 (MVP 범위 외)** | 유저 강제 삭제 / 제재 (`SUSPENDED`). 본인 탈퇴는 v1.1 |

> 아래 본문 옛 가이드와 충돌 시 본 박스 우선.



> 모르는 단어는 [`../00_overview/01_용어집.md`](../00_overview/01_용어집.md) 참고.
> 비유: **건물의 출입증 발급실.** 모든 사람이 처음 들어올 때 거치고, 들어온 후엔 출입증만 보여주면 통과. 출입증(JWT)이 위변조되지 않도록 발급실이 모든 책임을 진다.

---

## 1. 책임 한 줄

회원가입, 로그인(JWT 발급/검증), 프로필, 권한 위계를 책임진다. **모든 도메인이 의존하는 가장 기반 파트.**

---

## 2. 패키지 / 파일 소유권

### 내 소유 (자유 수정)
```
com.assetbox.user/
├─ controller/  UserController.java, AdminUserController.java
├─ service/     UserService.java
├─ repository/  UserRepository.java
├─ domain/      User.java, Role.java, AuthProvider.java
└─ dto/         SignupRequest, LoginRequest, LoginResponse, UserResponse, UserUpdateRequest
```

### 공동 (수정 시 PM/Infra 알림 필수)
```
com.assetbox.common.security/
├─ JwtProvider.java
├─ JwtAuthenticationFilter.java
├─ CurrentUser.java / CurrentUserArgumentResolver.java
├─ AuthUser.java
└─ StompJwtChannelInterceptor.java        (DM의 WS와 공유 — DM 담당과 협의)
com.assetbox.common.config/
└─ SecurityConfig.java                     (Infra와 공동, 인증 규칙 변경 시 협의)
```

### 절대 손대지 말 것
- 다른 도메인의 controller / service / repository / domain
- `common.config.SecurityConfig` 의 인증 규칙 (`requestMatchers...permitAll/hasRole`) — 변경은 PR + Infra 리뷰

---

## 3. 외부 컨트랙트 (내가 제공)

### 서비스 메서드 (다른 도메인이 호출)
```java
public interface UserService {
    User requireExists(Long userId);                       // 미존재시 USER_NOT_FOUND
    Optional<User> findByEmail(String email);
    User getMe(Long currentUserId);
    UserResponse toResponse(User user);                    // DTO 매핑 일관
    Long getSystemUserId();                                // Request → DM 알림에서 사용
}
```

### REST 엔드포인트
`04_api/02_엔드포인트_목록.md` 의 User 섹션 그대로.

### 보안 컨트랙트
- `@CurrentUser AuthUser user` 어노테이션으로 인증된 사용자를 컨트롤러에서 주입받음
- `AuthUser{ Long id, String email, Set<Role> roles }`
- ROLE 네이밍: SecurityConfig 의 RoleHierarchy 그대로 (`ROLE_SUPER_ADMIN > ROLE_ADMIN > ROLE_USER`)

---

## 4. 의존하는 컨트랙트 (내가 호출)

- (없음) — 가장 기반 파트라 외부 의존이 없다. 단, `BCryptPasswordEncoder` 는 `common.config.SecurityConfig` 빈.

---

## 5. 단계별 작업 가이드

### M0 (5/22 ~ 5/25): 코드·문서 정독
- [ ] 본 문서와 `04_api/03_명세서_User.md` / `04_api/02_...수정본.md` 대조
- [ ] Form + OAuth2(Google/Naver) 구현 순서, 이메일 화이트리스트, `name/provider/major` 정책 확인

### M1 (5/27 ~ 6/3): MVP
- [ ] 회원가입: 이메일 화이트리스트, 중복 시 409 + `USER_EMAIL_DUPLICATED`
- [ ] 로그인: BCrypt 비교 → JWT 발급, 실패 시 401 + `LOGIN_FAILED` (이메일 존재 여부는 노출 X), `profileRequired` 포함
- [ ] `/me` GET/PUT, `UserResponse` 매핑 표준화 (avatarUrl 빌드: `/api/users/{id}/avatar` 또는 File URL)
- [ ] Google OAuth 우선 연결, Naver는 여력 있으면 후속
- [ ] `UserService.requireExists` 가 모든 도메인에서 호출 가능한지 확인 — 다른 도메인 페어와 1회 미팅 (10분)
- [ ] `getSystemUserId()` — DM 알림용 시스템 발신자 ID. AdminBootstrapRunner와 함께 보장 (Infra 협업)
- [ ] 권한 변경: SUPER_ADMIN 만 가능 + 자기 자신은 강등 불가
- [ ] AvatarUpload: 확장자/사이즈 검증, 저장은 FileStorageService 호출 (File-A 협업)
- [ ] 통합 테스트 데이(6/1) 참석 — 다른 도메인 흐름에서 User 호출이 깨지지 않는지 확인
- [ ] M1 락 (6/3 EOD) 전까지 P0 픽스

### M2 (6/4 ~ 6/11, 6/9 베타): SHOULD 기능
- [ ] 디렉토리 / 검색 (`/users/directory`, `/users/search`) — DM 상대 검색용
- [ ] 어드민 유저 목록 조회 + SUPER_ADMIN 권한 변경 정책

### M3 (6/12 ~ 6/16): 안정화
- [ ] 토큰 만료 / 갱신 UX 다듬기 (만료된 토큰 401 + 명시적 코드 `TOKEN_EXPIRED`)
- [ ] 로깅: 로그인 실패 누적 모니터링 메트릭(Infra 협업)

---

## 6. 인수 기준 (AC)

### U-01 회원가입
- [ ] 이메일 형식 검증 (`@Email`)
- [ ] 비밀번호 8자 이상 (정책 합의 후 적용)
- [ ] 응답에 password 절대 노출 X
- [ ] 중복 이메일 409 `USER_EMAIL_DUPLICATED`

### U-02 로그인
- [ ] 응답 `{ accessToken, tokenType, profileRequired }`
- [ ] 잘못된 비번/없는 이메일 모두 동일한 401 `LOGIN_FAILED` (계정 존재 노출 X)
- [ ] 토큰에 userId, role 포함

### U-03 /me
- [ ] 비인증 401
- [ ] PUT 시 nickname 수정 가능
- [ ] OAuth 첫 가입으로 `major == null` 인 경우만 major 최초 보완 허용
- [ ] 이미 설정된 name/major, provider, role/email 변경 불가

### U-06 권한 변경
- [ ] 본인의 role 변경 시 403 `FORBIDDEN_SELF_ROLE_CHANGE`
- [ ] SUPER_ADMIN → SUPER_ADMIN 다중 허용

---

## 7. 충돌 방지 / 함정

| 함정 | 결과 | 회피 |
|---|---|---|
| `SecurityConfig` 에 새 엔드포인트의 permitAll 추가 잊음 | 다른 파트의 GET이 401 됨 | 새 익명 엔드포인트가 생기면 해당 파트가 PR에 SecurityConfig 변경을 같이 올림 + User/Infra 리뷰 |
| User 엔티티에 새 필드를 추가하고 응답 DTO 미변경 | 프론트가 못 봄 | UserResponse 매핑 표준 함수 한 곳에서만 변환 |
| BCryptPasswordEncoder 빈을 새로 만듦 | 빈 충돌 | 무조건 `common.config.SecurityConfig` 의 것 사용 |
| JWT 시크릿/만료 변경을 직접 main.yml 에 박음 | 운영에서 깨짐 | 환경변수로만, Infra와 협의 |
| 다른 도메인이 User 엔티티를 직접 `UserRepository` 통해 들고 다님 | 의존 폭주 | "다른 도메인은 `UserService.requireExists(id)` 만 쓴다" 를 강제 |
