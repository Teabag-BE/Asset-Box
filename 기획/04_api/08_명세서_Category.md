# 08. API 명세서 — Category

> 담당 파트: **Comment+Category+Search**
> 베이스 경로: `/api/categories/**`
> 카테고리는 셀프 참조 트리 (최대 3단계: 대 / 중 / 소). 클라이언트가 한 번에 다 받지 않고 **드릴다운으로 lazy 로딩**. 게시글은 최종 소분류(`depth=3`)에만 연결된다.

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| K-1 | GET | `/api/categories/roots` | USER | 대분류 (depth=1) |
| K-2 | GET | `/api/categories/{parentId}/children` | USER | 특정 카테고리의 하위 |
| K-3 | POST | `/api/categories` | ADMIN | 카테고리 생성 |
| K-4 | PATCH | `/api/categories/{id}` | ADMIN | 이름 수정 |
| K-5 | DELETE | `/api/categories/{id}` | ADMIN | 카테고리 삭제 |

---

## K-1. GET `/api/categories/roots`

**설명**: 대분류 목록. depth=1, `sortOrder` 기준으로 정렬한다.
**인증**: USER

### 요청

```http
GET /api/categories/roots
Authorization: Bearer <jwt>
```

### 응답 200

```json
{
  "success": true,
  "message": "요청 성공",
  "data": [
    { "id": 1, "name": "캐릭터",   "parentId": null, "depth": 1, "sortOrder": 1 },
    { "id": 2, "name": "환경",     "parentId": null, "depth": 1, "sortOrder": 2 },
    { "id": 3, "name": "소품",     "parentId": null, "depth": 1, "sortOrder": 3 },
    { "id": 4, "name": "이펙트",   "parentId": null, "depth": 1, "sortOrder": 4 }
  ]
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | 토큰 없음 |

시드 데이터가 없으면 빈 배열.

---

## K-2. GET `/api/categories/{parentId}/children`

**설명**: 특정 카테고리의 직속 자식. depth+1 만, 손자는 미포함. 같은 parent 아래에서는 `sortOrder` 기준으로 정렬한다.
**인증**: USER

### 요청

```http
GET /api/categories/3/children   # "소품" 의 중분류 조회
Authorization: Bearer <jwt>
```

### 응답 200

```json
{
  "success": true,
  "message": "요청 성공",
  "data": [
    { "id": 11, "name": "가구",   "parentId": 3, "depth": 2, "sortOrder": 1 },
    { "id": 12, "name": "조명",   "parentId": 3, "depth": 2, "sortOrder": 2 },
    { "id": 13, "name": "주방용품","parentId": 3, "depth": 2, "sortOrder": 3 }
  ]
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | 토큰 없음 |
| 404 | `CATEGORY_NOT_FOUND` | parentId 미존재 |

---

## K-3. POST `/api/categories`

**설명**: 카테고리 생성. parentId 가 null이면 대분류, 있으면 그 카테고리의 자식. depth는 자동 계산.
**인증**: ADMIN

### 요청

```json
{
  "name": "의자",
  "parentId": 11,
  "sortOrder": 1
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| name | string | NotBlank, 1~50자 |
| parentId | long? | null이면 대분류 |
| sortOrder | int? | 같은 parent 아래 표시 순서. 없으면 마지막 순서로 배치 |

### 응답 201

```json
{
  "success": true,
  "message": "요청 성공",
  "data": {
    "id": 101,
    "name": "의자",
    "parentId": 11,
    "depth": 3,
    "sortOrder": 1
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | name 누락 / 길이 위반 |
| 400 | `CATEGORY_DEPTH_EXCEEDED` | parent.depth >= 3 인데 자식 시도 (depth 4 차단) |
| 400 | `CATEGORY_NAME_DUPLICATED_IN_PARENT` | 같은 parent 아래 동일 name 존재 |
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | USER 호출 |
| 404 | `CATEGORY_NOT_FOUND` | parentId 미존재 |

---

## K-4. PATCH `/api/categories/{id}`

**설명**: 이름과 표시 순서 수정. 트리 구조(parent) 변경은 미지원 (v1.1).
**인증**: ADMIN

### 요청

```json
{
  "name": "체어",
  "sortOrder": 2
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| name | string? | 1~50자 |
| sortOrder | int? | 같은 parent 아래 표시 순서 |

### 응답 200

`K-3` 응답.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | 길이 위반 |
| 400 | `CATEGORY_NAME_DUPLICATED_IN_PARENT` | |
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | |
| 404 | `CATEGORY_NOT_FOUND` | |

---

## K-5. DELETE `/api/categories/{id}`

**설명**: 카테고리 삭제. 자식 있으면 거부. 소분류에 연결된 게시글이 있으면 거부한다.
**인증**: ADMIN

### 요청

```http
DELETE /api/categories/101
Authorization: Bearer <admin-jwt>
```

### 응답 200

```json
{ "success": true, "message": "요청 성공", "data": null }
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | |
| 404 | `CATEGORY_NOT_FOUND` | |
| 409 | `CATEGORY_HAS_CHILDREN` | 자식 카테고리 존재 |
| 409 | `CATEGORY_HAS_POSTS` | 해당 소분류에 게시글이 남아있음 |

> 정책: 자식·게시글이 있으면 삭제 거부. cascade 이동은 v1.1로 둔다.
