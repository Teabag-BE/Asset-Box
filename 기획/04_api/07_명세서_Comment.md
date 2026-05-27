# 07. API 명세서 — Comment

> ⚠️ **2026-05-22 회의 반영분과 충돌 시** [`02_엔드포인트_목록_상태코드분리_수정본.md`](./02_엔드포인트_목록_상태코드분리_수정본.md) **이 진실의 단일 원본.** 이 파일은 회의 반영 핵심 변경만 머리에 박아둠. 본문 상세는 회의록과 합쳐서 읽기.


> 담당 파트: **Comment+Category+Search** (Post 댓글) / Post-B (Request 댓글, 같은 패턴)
> 베이스 경로: `/api/posts/{postId}/comments/**`, `/api/requests/{requestId}/comments/**`

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| C-1 | GET | `/api/posts/{postId}/comments` | USER | Post 댓글 목록 (트리) |
| C-2 | POST | `/api/posts/{postId}/comments` | USER | Post 댓글 작성 (대댓글 포함) |
| C-3 | DELETE | `/api/posts/{postId}/comments/{commentId}` | USER (작성자) | Post 댓글 삭제 (soft) |
| C-4 | GET | `/api/requests/{requestId}/comments` | USER | Request 댓글 목록 |
| C-5 | POST | `/api/requests/{requestId}/comments` | USER | Request 댓글 작성 |
| C-6 | DELETE | `/api/requests/{requestId}/comments/{commentId}` | USER (작성자) | Request 댓글 삭제 |

---

## 공통

- soft delete: `deleted=true` 플래그. 응답에서는 `content="삭제된 댓글입니다."`, `authorId/Nickname=null` 로 마스킹.
- 대댓글은 1단계만 (parent 의 parent 가 있으면 400 `COMMENT_NESTED_TOO_DEEP`).
- parent 가 다른 글의 댓글이면 400 `COMMENT_PARENT_MISMATCH`.

---

## C-1. GET `/api/posts/{postId}/comments`

**설명**: 게시글의 댓글 트리. 루트 댓글 + 각 루트의 답글들. 시간 ASC.
**인증**: USER

### 요청

```http
GET /api/posts/42/comments?page=0&size=20
Authorization: Bearer <jwt>
```

| Query | 타입 | 기본 | 비고 |
|---|---|---|---|
| page, size | int | 0/20 | 루트 댓글 기준 페이징. 각 답글은 함께 직렬화 |

### 응답 200

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 901,
        "authorId": 12,
        "authorNickname": "김TA",
        "content": "좋은 모델 감사합니다!",
        "deleted": false,
        "parentId": null,
        "replies": [
          {
            "id": 902,
            "authorId": 18,
            "authorNickname": "박TA",
            "content": "다른 자세도 만들어볼게요.",
            "deleted": false,
            "parentId": 901,
            "replies": [],
            "createdAt": "2026-05-22T09:11:00"
          }
        ],
        "createdAt": "2026-05-21T16:00:00"
      },
      {
        "id": 904,
        "authorId": null,
        "authorNickname": null,
        "content": "삭제된 댓글입니다.",
        "deleted": true,
        "parentId": null,
        "replies": [],
        "createdAt": "2026-05-22T10:00:00"
      }
    ],
    "page": 0, "size": 20, "totalElements": 7, "totalPages": 1,
    "first": true, "last": true
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 404 | `POST_NOT_FOUND` | postId 미존재 |
| 400 | `PAGINATION_SIZE_TOO_LARGE` | |

---

## C-2. POST `/api/posts/{postId}/comments`

**설명**: 게시글 댓글 작성. `parentId` 있으면 답글.
**인증**: USER

### 요청

```http
POST /api/posts/42/comments
Content-Type: application/json
Authorization: Bearer <jwt>
```

```json
{
  "content": "정말 잘 만들었네요!",
  "parentId": null
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| content | string | NotBlank, max 2000자 |
| parentId | long? | null이면 루트 댓글 |

### 응답 201

```json
{
  "success": true,
  "data": {
    "id": 905,
    "authorId": 12,
    "authorNickname": "김TA",
    "content": "정말 잘 만들었네요!",
    "deleted": false,
    "parentId": null,
    "replies": [],
    "createdAt": "2026-05-22T11:30:00"
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | content 누락 / 2000자 초과 |
| 400 | `COMMENT_PARENT_MISMATCH` | parentId 가 다른 게시글의 댓글 |
| 400 | `COMMENT_NESTED_TOO_DEEP` | parent의 parent가 있음 (2단계 이상 시도) |
| 400 | `COMMENT_PARENT_DELETED` | parent가 soft delete됨 |
| 401 | `UNAUTHORIZED` | |
| 404 | `POST_NOT_FOUND` | |
| 404 | `COMMENT_PARENT_NOT_FOUND` | parentId 미존재 |

---

## C-3. DELETE `/api/posts/{postId}/comments/{commentId}`

**설명**: 댓글 삭제. soft. 작성자 본인만.
**인증**: USER (작성자)

### 요청

```http
DELETE /api/posts/42/comments/905
Authorization: Bearer <jwt>
```

### 응답 200

```json
{ "success": true, "data": null }
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | 작성자 아님 |
| 404 | `POST_NOT_FOUND` | |
| 404 | `COMMENT_NOT_FOUND` | |
| 400 | `COMMENT_POST_MISMATCH` | commentId가 다른 게시글의 댓글 |
| 410 | `COMMENT_ALREADY_DELETED` | 이미 soft delete됨 |

---

## C-4. GET `/api/requests/{requestId}/comments`

**설명**: Request 게시판의 댓글 트리. Post 댓글과 동일 패턴, 단 익명 노출 X.
**인증**: USER

### 요청

```http
GET /api/requests/11/comments?page=0&size=20
Authorization: Bearer <jwt>
```

### 응답 200

`C-1` 의 응답과 동일 스키마. (스키마 자체는 동일, 별도 엔티티 `RequestComment`)

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 404 | `REQUEST_NOT_FOUND` | |

---

## C-5. POST `/api/requests/{requestId}/comments`

**설명**: Request 댓글 작성.
**인증**: USER

### 요청

```json
{
  "content": "Blender 4.0 호환이면 좋겠어요.",
  "parentId": null
}
```

### 응답 201

`C-2` 응답 스키마 동일.

### 에러

`C-2` 와 동일 + `REQUEST_NOT_FOUND` 가 `POST_NOT_FOUND` 자리.

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | content 길이 위반 |
| 400 | `COMMENT_PARENT_MISMATCH` | parent가 다른 요청의 댓글 |
| 400 | `COMMENT_NESTED_TOO_DEEP` | |
| 400 | `COMMENT_PARENT_DELETED` | |
| 401 | `UNAUTHORIZED` | |
| 404 | `REQUEST_NOT_FOUND` | |
| 404 | `COMMENT_PARENT_NOT_FOUND` | |

---

## C-6. DELETE `/api/requests/{requestId}/comments/{commentId}`

**설명**: Request 댓글 삭제 (soft).
**인증**: USER (작성자)

### 응답 200

```json
{ "success": true, "data": null }
```

### 에러

`C-3` 과 동일 + Post → Request 치환.

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | 작성자 아님 |
| 404 | `REQUEST_NOT_FOUND` | |
| 404 | `COMMENT_NOT_FOUND` | |
| 400 | `COMMENT_REQUEST_MISMATCH` | |
| 410 | `COMMENT_ALREADY_DELETED` | |
