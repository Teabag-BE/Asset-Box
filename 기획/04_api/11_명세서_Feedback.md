# 11. API 명세서 — Feedback

> ⚠️ **2026-05-26 회의 반영** — Feedback은 `userId` 필수이며 User와 1:N으로 연결한다. 익명 피드백은 없다.


> 담당 파트: **Infra** (소형 운영 도메인)
> 베이스 경로: `/api/feedback`, `/api/admin/feedback/**`

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| FB-1 | POST | `/api/feedback` | USER | 피드백 보내기 |
| FB-2 | GET | `/api/admin/feedback` | ADMIN | 어드민 피드백 목록 |
| FB-3 | PATCH | `/api/admin/feedback/{id}/read` | ADMIN | 읽음 처리 |
| FB-4 | GET | `/api/admin/feedback/count-new` | ADMIN | 신규 피드백 수 (헤더 뱃지) |

---

## FB-1. POST `/api/feedback`

**설명**: 운영자에게 의견 보내기. 로그인 사용자만 작성 가능하며 userId/nickname을 자동 기록한다.
**인증**: USER

### 요청

```http
POST /api/feedback
Content-Type: application/json
Authorization: Bearer <jwt>
```

```json
{
  "title": "다운로드 속도가 가끔 느려요",
  "content": "오후 3시쯤 fbx 파일 다운로드가 멈춤. Mac, Chrome."
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| title | string | NotBlank, max 100 |
| content | string | NotBlank, max 2000 |

### 응답 201

```json
{
  "success": true,
  "data": null
}
```

> 응답에 피드백 id 등을 노출하지 않음.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | title/content 누락 / 길이 위반 |
| 401 | `UNAUTHORIZED` | 토큰 없음 |

---

## FB-2. GET `/api/admin/feedback`

**설명**: 어드민 피드백 목록.
**인증**: ADMIN

### 요청

```http
GET /api/admin/feedback?page=0&size=20&status=NEW
Authorization: Bearer <admin-jwt>
```

| Query | 타입 | 기본 | 비고 |
|---|---|---|---|
| page, size | int | 0/20 | |
| status | string? | - | `NEW`, `READ` |
| sort | string | `createdAt,desc` | |

### 응답 200

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 31,
        "title": "다운로드 속도가 가끔 느려요",
        "content": "오후 3시쯤 ...",
        "userId": 12,
        "userNickname": "김TA",
        "status": "NEW",
        "createdAt": "2026-05-23T15:00:00"
      },
      {
        "id": 30,
        "title": "요청 게시판 UX 개선",
        "content": "...",
        "userId": 18,
        "userNickname": "박TA",
        "status": "NEW",
        "createdAt": "2026-05-23T11:00:00"
      }
    ],
    "page": 0, "size": 20, "totalElements": 31, "totalPages": 2,
    "first": true, "last": false
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `FEEDBACK_STATUS_UNKNOWN` | status 값 enum 아님 |
| 400 | `PAGINATION_SIZE_TOO_LARGE` | |
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | USER 호출 |

---

## FB-3. PATCH `/api/admin/feedback/{id}/read`

**설명**: NEW → READ 전이.
**인증**: ADMIN

### 요청

```http
PATCH /api/admin/feedback/31/read
Authorization: Bearer <admin-jwt>
```

### 응답 200

```json
{
  "success": true,
  "data": {
    "id": 31,
    "title": "...",
    "content": "...",
    "userId": 12,
    "userNickname": "김TA",
    "status": "READ",
    "createdAt": "2026-05-23T15:00:00"
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | |
| 404 | `FEEDBACK_NOT_FOUND` | |
| 409 | `FEEDBACK_ALREADY_READ` | (선택) 이미 READ 인데 다시 요청 |

---

## FB-4. GET `/api/admin/feedback/count-new`

**설명**: 신규(NEW) 피드백 수. 어드민 헤더 뱃지에 사용.
**인증**: ADMIN

### 요청

```http
GET /api/admin/feedback/count-new
```

### 응답 200

```json
{
  "success": true,
  "data": { "count": 7 }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | |
