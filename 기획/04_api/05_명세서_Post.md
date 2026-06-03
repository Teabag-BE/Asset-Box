# 05. API 명세서 — Post

> ⚠️ **2026-05-26 회의 반영** — Post는 소분류(depth=3)에 연결하고, 대표 썸네일은 `thumbnailUrl`로 가진다. 대표 썸네일은 File FK가 아니라 URL 문자열로 저장한다.


> 담당 파트: **Post-A**
> 베이스 경로: `/api/posts/**`, `/api/admin/posts/**`

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| P-1 | POST | `/api/posts` | USER | 게시글 작성 (multipart) |
| P-2 | GET | `/api/posts` | USER | 게시글 목록 / 검색 |
| P-3 | GET | `/api/posts/popular-tags` | USER | 인기 태그 |
| P-4 | GET | `/api/posts/liked` | USER | 내가 좋아요 누른 글 |
| P-5 | GET | `/api/posts/{postId}` | USER | 게시글 상세 (view +1) |
| P-6 | PUT | `/api/posts/{postId}` | USER (작성자) | 게시글 수정 |
| P-7 | DELETE | `/api/posts/{postId}` | USER (작성자) | 게시글 삭제 (soft) |
| P-8 | POST | `/api/posts/{postId}/like` | USER | 좋아요 토글 |
| P-9 | GET | `/api/admin/posts` | ADMIN | 어드민 게시글 목록 (조회만) |

---

## P-1. POST `/api/posts`

**설명**: 게시글 + 파일 + 대표 썸네일 + 태그를 한 번에 작성. multipart. `categoryId`는 반드시 소분류(`categories.depth=3`)여야 한다.
**인증**: USER

### 요청

```http
POST /api/posts
Content-Type: multipart/form-data; boundary=...
Authorization: Bearer <jwt>
```

```
--boundary
Content-Disposition: form-data; name="data"
Content-Type: application/json

{
  "title": "캐주얼 의자 (low poly)",
  "content": "Blender 4.0 / 트라이앵글 1.2k\n자유롭게 사용 가능합니다.",
  "categoryId": 12,
  "tags": ["furniture", "low-poly", "chair"],
  "linkedRequestId": 11
}
--boundary
Content-Disposition: form-data; name="files"; filename="chair-low-poly.fbx"
Content-Type: application/octet-stream

<binary>
--boundary
Content-Disposition: form-data; name="thumbnail"; filename="thumb.png"
Content-Type: image/png

<binary>
--boundary--
```

| Part | 필드 | 타입 | 제약 |
|---|---|---|---|
| data | title | string | NotBlank, max 100 |
| data | content | string | NotBlank |
| data | categoryId | long | 소분류(depth=3)만 허용 |
| data | tags | string[] | 0~10개, 각 30자 |
| data | linkedRequestId | long? | 요청 결과물일 때만. assignee 본인 검증 후 요청 자동 COMPLETED |
| files | binary | - | 1개 이상 권장 |
| thumbnail | image/png \| jpeg | - | 0 또는 1개, ≤ 5MB. 저장 후 URL을 `posts.thumbnail_url`에 저장 |

### 응답 201

```json
{
  "success": true,
  "message": "요청 성공",
  "data": {
    "id": 42,
    "title": "캐주얼 의자 (low poly)",
    "content": "Blender 4.0 ...",
    "authorNickname": "김TA",
    "authorId": 12,
    "categoryId": 12,
    "categoryName": "소품",
    "categoryPath": ["소품", "가구", "의자"],
    "tags": ["furniture", "low-poly", "chair"],
    "thumbnailUrl": "/api/files/138",
    "totalFileSize": 524288,
    "imageResolution": "1024x1024",
    "polygon": 1200,
    "files": [
      {
        "id": 137,
        "originalName": "chair-low-poly.fbx",
        "extension": "fbx",
        "sizeBytes": 524288,
        "uploadOrder": 0
      }
    ],
    "viewCount": 0,
    "likeCount": 0,
    "downloadCount": 0,
    "liked": false,
    "linkedRequestId": 11,
    "createdAt": "2026-05-21T14:30:15"
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | title/content 누락 |
| 400 | `FILE_EXTENSION_NOT_ALLOWED` | 확장자 위반 |
| 400 | `FILE_TOO_LARGE` | 파일 합계 20MB 초과 |
| 400 | `FILE_EMPTY` | 0바이트 파일 |
| 400 | `THUMBNAIL_MULTIPLE_NOT_ALLOWED` | 썸네일 2개 이상 |
| 400 | `TAGS_TOO_MANY` | 11개 이상 |
| 401 | `UNAUTHORIZED` | |
| 404 | `CATEGORY_NOT_FOUND` | categoryId 미존재 |
| 400 | `CATEGORY_DEPTH_INVALID` | categoryId가 소분류(depth=3)가 아님 |
| 403 | `POST_LINKED_REQUEST_FORBIDDEN` | linkedRequestId 요청의 assignee가 아님 |
| 400 | `POST_LINKED_REQUEST_INVALID_STATUS` | linkedRequestId 요청이 IN_PROGRESS가 아님 |
| 404 | `REQUEST_NOT_FOUND` | linkedRequestId 미존재 |
| 500 | `STORAGE_WRITE_FAILED` | 디스크 저장 실패 (트랜잭션 롤백 + cleanup) |

### 부수효과

- `linkedRequestId`가 있으면 같은 트랜잭션에서 Request 도메인이 `linkedPostId`를 세팅하고 status를 `COMPLETED`로 바꾼다.
- 요청자에게 시스템 DM을 발송한다.

---

## P-2. GET `/api/posts`

**설명**: 게시글 목록 + 검색의 단일 입구. 카테고리/태그/키워드/작성자/팀 필터 + 정렬.
**인증**: USER

### 요청

```http
GET /api/posts?page=0&size=20&sort=likeCount,desc&q=chair&tag=low-poly&categoryId=12
```

| Query | 타입 | 기본 | 비고 |
|---|---|---|---|
| page | int | 0 | |
| size | int | 20 | 최대 50 |
| sort | string | `createdAt,desc` | 화이트리스트: `createdAt`, `likeCount`, `viewCount` |
| q | string? | - | title + content 부분 매치 |
| tag | string? | - | 정확 매치 (정규화 후) |
| categoryId | long? | - | 소분류(depth=3) 정확 매치 |
| authorId | long? | - | |
| linkedRequestId | long? | - | 특정 요청의 결과 게시글 필터 |

### 응답 200

```json
{
  "success": true,
  "message": "요청 성공",
  "data": {
    "items": [
      {
        "id": 42,
        "title": "캐주얼 의자 (low poly)",
        "authorNickname": "김TA",
        "authorId": 12,
        "categoryId": 12,
        "categoryPath": ["소품", "가구", "의자"],
        "tags": ["furniture", "low-poly", "chair"],
        "thumbnailUrl": "/api/files/138",
        "fileExtension": "fbx",
        "totalFileSize": 524288,
        "imageResolution": "1024x1024",
        "polygon": 1200,
        "viewCount": 173,
        "likeCount": 24,
        "commentCount": 5,
        "downloadCount": null,
        "linkedRequestId": 11,
        "createdAt": "2026-05-21T14:30:15"
      }
    ],
    "page": 0, "size": 20, "totalElements": 137, "totalPages": 7,
    "first": true, "last": false
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `PAGINATION_SIZE_TOO_LARGE` | size > 50 |
| 400 | `SORT_KEY_NOT_ALLOWED` | sort 키 위반 |
| 401 | `UNAUTHORIZED` | |
| 404 | `CATEGORY_NOT_FOUND` | categoryId 미존재 |
| 400 | `CATEGORY_DEPTH_INVALID` | categoryId가 소분류(depth=3)가 아님 |

---

## P-3. GET `/api/posts/popular-tags`

**설명**: 사용 빈도 기준 상위 태그. Caffeine 캐시 60초.
**인증**: USER

### 요청

```http
GET /api/posts/popular-tags?limit=10
```

| Query | 타입 | 기본 | 비고 |
|---|---|---|---|
| limit | int | 10 | 최대 50 |

### 응답 200

```json
{
  "success": true,
  "message": "요청 성공",
  "data": [
    { "name": "low-poly", "count": 87 },
    { "name": "furniture", "count": 64 },
    { "name": "stylized", "count": 41 }
  ]
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `LIMIT_TOO_LARGE` | limit > 50 |
| 401 | `UNAUTHORIZED` | |

---

## P-4. GET `/api/posts/liked`

**설명**: 현재 사용자가 좋아요 누른 게시글 목록.
**인증**: USER

### 요청

```http
GET /api/posts/liked?page=0&size=20
Authorization: Bearer <jwt>
```

### 응답 200

`P-2` 와 동일 스키마 (Summary).

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |

---

## P-5. GET `/api/posts/{postId}`

**설명**: 게시글 상세. 조회수 +1 (별도 트랜잭션).
**인증**: USER

### 요청

```http
GET /api/posts/42
Authorization: Bearer <jwt>
```

### 응답 200

`P-1` 의 응답과 동일 스키마. 단 viewCount 가 +1 된 값.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 404 | `POST_NOT_FOUND` | |
| 410 | `POST_DELETED` | 작성자 soft delete |

---

## P-6. PUT `/api/posts/{postId}`

**설명**: 게시글 수정. 본문 + 카테고리 + 태그. `linkedRequestId`는 작성 시 1회만 가능하며 수정 불가.
**인증**: USER (작성자 본인만)

### 요청

```http
PUT /api/posts/42
Content-Type: application/json
Authorization: Bearer <jwt>
```

```json
{
  "title": "캐주얼 의자 (low poly, v2)",
  "content": "트라이앵글 800으로 축소.",
  "categoryId": 12,
  "tags": ["furniture", "low-poly", "chair", "v2"]
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| title | string | NotBlank |
| content | string | NotBlank |
| categoryId | long | 소분류(depth=3)만 허용 |
| tags | string[]? | |

### 응답 200

`P-1` 응답과 동일.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | |
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | 작성자 아님 |
| 404 | `POST_NOT_FOUND` | |
| 404 | `CATEGORY_NOT_FOUND` | |
| 400 | `CATEGORY_DEPTH_INVALID` | categoryId가 소분류(depth=3)가 아님 |
| 409 | `POST_LINKED_REQUEST_IMMUTABLE` | linkedRequestId 변경 시도 |

---

## P-7. DELETE `/api/posts/{postId}`

**설명**: 게시글 삭제. **soft delete** (`deleted=true`). 어드민 외엔 목록/상세에서 안 보임. 작성자 본인만.
**인증**: USER (작성자)

### 요청

```http
DELETE /api/posts/42
Authorization: Bearer <jwt>
```

### 응답 200

```json
{ "success": true, "message": "요청 성공", "data": null }
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | 작성자 아님 |
| 404 | `POST_NOT_FOUND` | |

---

## P-8. POST `/api/posts/{postId}/like`

**설명**: 좋아요 토글. 누른 적 없으면 +1, 있으면 -1. 단일 POST로 처리.
**인증**: USER

### 요청

```http
POST /api/posts/42/like
Authorization: Bearer <jwt>
```

### 응답 200

```json
{
  "success": true,
  "message": "요청 성공",
  "data": {
    "likeCount": 25,
    "liked": true
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 404 | `POST_NOT_FOUND` | |

---

## P-9. GET `/api/admin/posts`

**설명**: 어드민용 게시글 목록. soft delete된 글도 포함 가능. 조회/모니터링 전용.
**인증**: ADMIN

### 요청

```http
GET /api/admin/posts?page=0&size=20&includeDeleted=true&authorId=12
```

| Query | 타입 | 기본 | 비고 |
|---|---|---|---|
| includeDeleted | bool | true | 기본 어드민은 모두 보기 |
| 그 외 | - | - | P-2 와 동일 |

### 응답 200

P-2 응답 + 각 항목에 `deleted` (bool) 필드 추가.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | |

> 게시글 강제 삭제(hard delete), 복구, 분쟁 처리 API는 본 프로젝트 범위 밖이다. 작성자 본인의 soft delete만 지원한다.

---

## 부록 — 응답 스키마 요약

### PostSummaryResponse (목록)

```ts
{
  id: long, title: string, authorNickname: string, authorId: long,
  categoryId: long, categoryPath: string[], tags: string[],
  thumbnailUrl: string | null, fileExtension: string | null,
  viewCount: long, likeCount: long, commentCount: long, downloadCount: long | null,
  linkedRequestId: long | null,
  createdAt: ISODateTime
}
```

### PostDetailResponse (상세)

```ts
{
  id, title, content, authorNickname, authorId,
  categoryId, categoryName, categoryPath, tags: string[],
  thumbnailUrl,
  files: FileInfo[],
  viewCount, likeCount, downloadCount, liked: boolean,
  linkedRequestId: long | null,
  createdAt
}

FileInfo = { id, originalName, extension, sizeBytes }
```
