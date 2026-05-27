# 06. API 명세서 — Request (요청 게시판)

> 담당 파트: **Request**
> 베이스 경로: `/api/requests/**`
> 댓글 엔드포인트는 `07_명세서_Comment.md` 참고.

요청 게시판은 **요청자와 Assignee(TA) 중심**으로 흐른다. Admin은 이 흐름에 개입하지 않고 조회/모니터링만 한다.

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| R-1 | POST | `/api/requests` | USER | 요청 작성 |
| R-2 | GET | `/api/requests` | USER | 요청 목록 |
| R-3 | GET | `/api/requests/{id}` | USER | 요청 상세 |
| R-4 | PUT | `/api/requests/{id}` | USER (요청자) | 요청 수정 (REQUESTED 상태만) |
| R-5 | PATCH | `/api/requests/{id}/assign` | USER | TA가 본인을 assignee로 수락 |
| R-6 | PATCH | `/api/requests/{id}/reject` | USER (assignee 또는 요청자) | 요청 반려 |
| R-7 | PATCH | `/api/requests/{id}/reopen` | USER (요청자) | 반려 요청 재오픈 |
| R-8 | DELETE | `/api/requests/{id}` | USER (요청자) | 요청 삭제 (REQUESTED 상태만 soft delete) |

> 별도 `/status`, `/review`, `/link-post` 엔드포인트는 만들지 않는다. 완료는 `POST /api/posts` 의 `linkedRequestId` 로 자동 처리한다.

---

## 상태 흐름

```text
REQUESTED(요청됨) → IN_REVIEW(검토중) → IN_PROGRESS(제작중) → COMPLETED(완료)
                                      ↘ REJECTED(반려됨)
```

MVP 권장 흐름은 `REQUESTED → IN_PROGRESS → COMPLETED` 직행이다. `IN_REVIEW` 는 enum에는 남기되 별도 `/review` API 없이 v1.1에서 도입 검토한다.

| 전이 | 트리거 | actor |
|---|---|---|
| `REQUESTED → IN_PROGRESS` | `PATCH /assign` | TA(USER) 본인 |
| `IN_PROGRESS → COMPLETED` | `POST /api/posts` 의 `linkedRequestId` | assignee 본인 |
| `REQUESTED/IN_PROGRESS → REJECTED` | `PATCH /reject` | assignee 또는 요청자 |
| `REJECTED → REQUESTED` | `PATCH /reopen` | 요청자 |

상태 변경 성공 시 요청자에게 시스템 DM을 발송한다.

---

## R-1. POST `/api/requests`

**설명**: 새 에셋 의뢰. status=`REQUESTED`, teamId 스냅샷.
**인증**: USER

### 요청

```http
POST /api/requests
Content-Type: multipart/form-data
Authorization: Bearer <jwt>
```

```
data={
  "title": "캐주얼 의자가 필요해요",
  "content": "로우폴리. 카페 씬에 둘 작은 1인용 의자.",
  "assetType": "Furniture",
  "preferredStyle": "Stylized / Casual",
  "engine": "Blender",
  "deadline": "2026-06-15"
}
referenceThumbnail=@reference.png   # 선택
```

| Part | 필드 | 타입 | 제약 |
|---|---|---|---|
| data | title | string | NotBlank, max 100 |
| data | content | string | NotBlank |
| data | assetType | string? | max 60 |
| data | preferredStyle | string? | max 60 |
| data | engine | string? | max 60 |
| data | deadline | date? | 오늘 이후 권장 |
| referenceThumbnail | binary? | image | png/jpg/jpeg, File purpose=`REQUEST_REFERENCE` |

### 응답 201

```json
{
  "success": true,
  "data": {
    "id": 11,
    "title": "캐주얼 의자가 필요해요",
    "content": "로우폴리. 카페 씬에 둘 작은 1인용 의자.",
    "assetType": "Furniture",
    "preferredStyle": "Stylized / Casual",
    "engine": "Blender",
    "deadline": "2026-06-15",
    "status": "REQUESTED",
    "requesterId": 12,
    "requesterNickname": "김TA",
    "assigneeId": null,
    "assigneeNickname": null,
    "linkedPostId": null,
    "referenceFileId": 301,
    "createdAt": "2026-05-21T14:30:15",
    "updatedAt": "2026-05-21T14:30:15"
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | title/content 누락, 길이 위반 |
| 400 | `REQUEST_DEADLINE_PAST` | deadline이 과거 |
| 400 | `FILE_EXTENSION_NOT_ALLOWED` | 참고 이미지 확장자 위반 |
| 401 | `UNAUTHORIZED` | |

---

## R-2. GET `/api/requests`

**설명**: 요청 목록. 운영자도 같은 목록 API로 모니터링한다.
**인증**: USER

### 요청

```http
GET /api/requests?page=0&size=20&status=IN_PROGRESS&assigneeId=18
Authorization: Bearer <jwt>
```

| Query | 타입 | 기본 | 비고 |
|---|---|---|---|
| page, size | int | 0/20 | |
| sort | string | `createdAt,desc` | 화이트리스트: `createdAt`, `deadline` |
| status | string? | - | `REQUESTED`, `IN_REVIEW`, `IN_PROGRESS`, `COMPLETED`, `REJECTED` |
| assigneeId | long? | - | |
| requesterId | long? | - | |
| teamId | long? | - | |
| q | string? | - | title LIKE |

### 응답 200

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 11,
        "title": "캐주얼 의자가 필요해요",
        "assetType": "Furniture",
        "engine": "Blender",
        "deadline": "2026-06-15",
        "status": "IN_PROGRESS",
        "requesterId": 12,
        "requesterNickname": "김TA",
        "assigneeId": 18,
        "assigneeNickname": "박TA",
        "linkedPostId": null,
        "createdAt": "2026-05-21T14:30:15"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 22,
    "totalPages": 2,
    "first": true,
    "last": false
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `REQUEST_STATUS_UNKNOWN` | status 값이 enum 아님 |
| 400 | `SORT_KEY_NOT_ALLOWED` | |
| 401 | `UNAUTHORIZED` | |

---

## R-3. GET `/api/requests/{id}`

**설명**: 요청 상세. `linkedPostId`, `referenceFileId` 포함.
**인증**: USER

### 요청

```http
GET /api/requests/11
Authorization: Bearer <jwt>
```

### 응답 200

`R-1` 의 응답 스키마와 동일.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 404 | `REQUEST_NOT_FOUND` | |
| 410 | `REQUEST_DELETED` | soft delete된 요청 |

---

## R-4. PUT `/api/requests/{id}`

**설명**: 요청 수정. **status=REQUESTED 상태일 때만** 수정 가능. 요청자 본인만.
**인증**: USER (요청자)

### 요청

```http
PUT /api/requests/11
Content-Type: multipart/form-data
Authorization: Bearer <jwt>
```

`R-1` 과 같은 multipart 구조.

### 응답 200

`R-1` 응답.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | |
| 400 | `REQUEST_NOT_EDITABLE` | status가 REQUESTED 아님 |
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | 요청자 아님 |
| 404 | `REQUEST_NOT_FOUND` | |

---

## R-5. PATCH `/api/requests/{id}/assign`

**설명**: TA가 본인을 assignee로 등록한다. 본문을 받지 않고 호출자가 자동 assignee가 된다. 성공 시 `IN_PROGRESS`.
**인증**: USER

### 요청

```http
PATCH /api/requests/11/assign
Authorization: Bearer <jwt>
```

### 응답 200

`R-1` 응답 (`assigneeId=현재 사용자`, `status=IN_PROGRESS`).

### 부수효과

- 요청자에게 시스템 DM: "`{assigneeNickname}`님이 요청을 수락했습니다."

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 404 | `REQUEST_NOT_FOUND` | |
| 409 | `REQUEST_ASSIGN_TAKEN` | 이미 다른 assignee가 있음 |
| 409 | `REQUEST_ASSIGN_SELF_DUPLICATED` | 본인이 이미 assignee |
| 409 | `REQUEST_COMPLETED_LOCKED` | 이미 COMPLETED |

---

## R-6. PATCH `/api/requests/{id}/reject`

**설명**: 요청 반려. assignee 또는 요청자가 사유를 남기고 `REJECTED`로 바꾼다.
**인증**: USER (assignee 또는 요청자)

### 요청

```json
{
  "reason": "현재 작업 범위에서 처리하기 어렵습니다."
}
```

### 응답 200

`R-1` 응답 (`status=REJECTED`).

### 부수효과

- 요청자에게 시스템 DM 발송

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | reason 길이 위반 |
| 401 | `UNAUTHORIZED` | |
| 403 | `REQUEST_REJECT_FORBIDDEN` | assignee/요청자가 아님 |
| 404 | `REQUEST_NOT_FOUND` | |
| 409 | `REQUEST_COMPLETED_LOCKED` | 이미 COMPLETED |

---

## R-7. PATCH `/api/requests/{id}/reopen`

**설명**: 반려된 요청을 요청자가 다시 연다. 기본 복귀 상태는 `REQUESTED`.
**인증**: USER (요청자)

### 요청

```json
{
  "targetStatus": "REQUESTED"
}
```

### 응답 200

`R-1` 응답 (`status=REQUESTED`, assignee 초기화 여부는 정책에 맞춰 구현).

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `REQUEST_REOPEN_TARGET_INVALID` | targetStatus가 REQUESTED/IN_REVIEW가 아님 |
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | 요청자 아님 |
| 404 | `REQUEST_NOT_FOUND` | |
| 409 | `REQUEST_NOT_REJECTED` | REJECTED 상태가 아님 |

---

## R-8. DELETE `/api/requests/{id}`

**설명**: 요청 삭제. `REQUESTED` 상태에서 요청자 본인만 soft delete.
**인증**: USER (요청자)

### 요청

```http
DELETE /api/requests/11
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
| 403 | `FORBIDDEN` | 요청자 아님 |
| 404 | `REQUEST_NOT_FOUND` | |
| 409 | `REQUEST_IN_PROGRESS_LOCKED` | REQUESTED가 아닌 상태 |

---

## 부록 — Post 작성에 의한 자동 완료 컨트랙트

Request 도메인은 Post 도메인이 호출할 내부 메서드를 제공한다.

```java
RequestResponse completeByLinkedPost(Long requestId, Long assigneeId, Long linkedPostId);
```

검증:

- request 존재
- request.assigneeId == assigneeId
- request.status == `IN_PROGRESS`
- linkedPostId 1회만 설정 가능

성공 시:

- request.status = `COMPLETED`
- request.linkedPostId = linkedPostId
- 요청자에게 시스템 DM: "`요청이 완료되었습니다 → /posts/{linkedPostId}`"
