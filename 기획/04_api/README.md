# 04. API 문서 인덱스

> 비유: **레시피북 목차.** 표준 양념(01) → 메뉴판(02) → 도메인별 상세 레시피(03~11) → 양념 사전(12).

> ⚠️ **2026-05-22 회의 반영분이 02 파일에 정리되어 있음.** 03~11 명세서는 회의 반영 변경을 머리에 박아둠. 충돌 시 **02 파일이 진실의 단일 원본 (Single Source of Truth)**.

---

## 읽는 순서

| 순서 | 파일 | 무엇 |
|---|---|---|
| 1 | `01_API_표준.md` | 응답 포맷, 인증, 페이지네이션, 멀티파트, 명세서 읽는 법 |
| 2 | **`02_엔드포인트_목록_상태코드분리_수정본.md`** | **회의 반영 메뉴판 (SSoT).** 모든 엔드포인트 한 장 요약 |
| 3 | `03_명세서_User.md` | 회원·인증(Form/OAuth)·프로필 |
| 4 | `04_명세서_File.md` | 통합 File 도메인 (저장/조회/메타/삭제) |
| 5 | `05_명세서_Post.md` | 게시글 + 좋아요 + linkedRequestId 자동 완료 |
| 6 | `06_명세서_Request.md` | 요청 게시판 + 어드민 개입 없는 자동 완료 흐름 |
| 7 | `07_명세서_Comment.md` | Post / Request 양쪽 댓글 |
| 8 | `08_명세서_Category.md` | 카테고리 트리 + 어드민 |
| 9 | `09_명세서_Search_Tag.md` | 검색 통합 입구 + 인기 태그 + 정규화 규칙 |
| 10 | `10_명세서_Message.md` | DM REST. WebSocket은 M2, 어드민 열람 제외 |
| 11 | `11_명세서_Feedback.md` | 피드백 (로그인 필수) |
| 12 | `12_에러_코드_사전.md` | 전 도메인 에러 코드 통합 사전 + ErrorCode enum 패턴 |

---

## 2026-05-22 회의 반영 핵심 (요약)

- **인증**: Form + Google + Naver OAuth 병행. **이메일 화이트리스트**. `name/major/provider` 기준.
- **권한**: 분쟁 처리·강제 삭제·메시지 열람 등 강한 운영 권한 **제외**. Admin은 모니터링 중심.
- **File 통합 도메인**: 에셋 + 게시글 썸네일 + 프로필 이미지 + 요청 참고 이미지를 한 도메인에서. DownloadLog 는 v1.1 로 보류.
- **게시글**: **비회원 열람 불가**. 작성 시 `linkedRequestId` 필드로 요청 자동 완료 가능.
- **요청 게시판**: TA(USER) 가 본인을 assignee 로 수락 → 작업 → 게시글에 `linkedRequestId` 박으면 **자동 COMPLETED** (GitHub Issue 닫히는 패턴). **어드민 개입 없음.**
- **Feedback**: 익명 X, 로그인 필수.

---

## 도메인 ↔ 명세서 ↔ 담당 파트

| 도메인 | 명세서 | 담당 파트 |
|---|---|---|
| User | `03_명세서_User.md` | User |
| File | `04_명세서_File.md` | File-A + File-B |
| Post | `05_명세서_Post.md` | Post-A |
| Request | `06_명세서_Request.md` | Post-B |
| Comment | `07_명세서_Comment.md` | Comment+Category+Search (Post 댓글) / Post-B (Request 댓글) |
| Category | `08_명세서_Category.md` | Comment+Category+Search |
| Search / Tag | `09_명세서_Search_Tag.md` | Comment+Category+Search |
| Message | `10_명세서_Message.md` | DM |
| Feedback | `11_명세서_Feedback.md` | Infra |
| (전 도메인) | `12_에러_코드_사전.md` | PM (게이트키퍼) |

---

## 변경 시 절차 (재확인)

1. **기획 폴더가 코드보다 먼저.** 변경하고 싶은 엔드포인트가 있으면 본 04_api/ 의 명세서를 먼저 수정 PR.
2. 그 다음 코드 PR.
3. 에러 코드 추가 시 `12_에러_코드_사전.md` 와 영향 명세서(03~11) 양쪽 수정 (한 PR).
4. 응답 스키마 / 필드 제거·이름 변경은 **위클리 회의 안건**. 추가는 자유롭게 PR.
5. PM 승인 후 머지.

---

## 빠른 참조 — 헤더 / 응답 표준

### 요청 헤더 (인증 필요 시)
```
Authorization: Bearer <JWT>
Content-Type: application/json    # 또는 multipart/form-data
Accept: application/json
```

### 성공 응답
```json
{ "success": true, "message": "성공 내용 description", "data": { ... } }
```

### 실패 응답
```json
{ "success": false, "error": { "code": "...", "message": "..." } }
```

### 페이지네이션 응답
```json
{
  "success": true,
  "message": "목록 조회 성공 description",
  "data": {
    "items": [ ... ],
    "page": 0, "size": 20, "totalElements": 137, "totalPages": 7,
    "first": true, "last": false
  }
}
```

---

## 명세서 한 장 통계 (검증용)

| 도메인 | 엔드포인트 수 | 에러 코드 종류 (대략) |
|---|---|---|
| User | 12 | 9 |
| File | 3 + 업로드 컨트랙트 | 9 |
| Post | 9 | 13 |
| Request | 8 | 12 |
| Comment | 6 | 8 |
| Category | 5 | 5 |
| Search/Tag | 1 (Post에 흡수) | 2 |
| Message | 4 REST + 2 M2 WS | 4 |
| Feedback | 4 | 4 |
| **합계** | **약 47개** | **사전 기준** |

> 본 명세서는 **MVP + SHOULD 기능까지** 포함합니다. WON'T(v1.1)는 본 문서에 등장하지 않으며, 백로그(`06_features/01_기능_카탈로그.md`)에서 추적됩니다.
