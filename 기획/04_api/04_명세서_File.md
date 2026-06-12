# 04. API 명세서 — File

> 담당 파트: **File (통합 도메인)**
> 베이스 경로: `/api/files/**`
> 파일 업로드 요청은 Post/User/Request API에서 multipart로 받고, 실제 저장/검증/메타데이터 관리는 FileService에 위임한다. File은 `domainType` + `domainId`로 연결 대상을 표현한다.

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| F-1 | GET | `/api/files/{fileId}` | USER | 파일 다운로드/이미지 조회 |
| F-2 | GET | `/api/files/{fileId}/meta` | USER | 파일 메타데이터 조회 |
| F-3 | DELETE | `/api/files/{fileId}` | USER (소유자/관리자) | 파일 삭제 또는 비활성화 |

> 다운로드 로그 및 파일 통계 API는 MVP 범위에서 제외한다. 필요하면 v1.1에서 `analytics` 또는 `audit` 도메인으로 분리한다.

---

## F-1. GET `/api/files/{fileId}`

**설명**: 에셋 파일, 게시글 대표 썸네일 파일, 프로필 이미지, 요청 참고 이미지를 공통 조회한다. Post/User/Request는 File FK가 아니라 저장된 URL을 보존한다.
**인증**: USER

### 요청

```http
GET /api/files/137
Authorization: Bearer <jwt>
```

| Path | 타입 | 비고 |
|---|---|---|
| fileId | long | 대상 파일 |

### 응답 200

```
Content-Type: application/octet-stream (또는 image/png 등 추론)
Content-Disposition: attachment; filename*=UTF-8''chair-low-poly.fbx
Content-Length: 524288
Cache-Control: no-store

<binary>
```

이미지 파일은 브라우저 표시가 가능하도록 `inline` 정책을 선택할 수 있다. 기본은 `domainType`과 `fileType`에 따라 FileService가 결정한다.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | 토큰 없음 |
| 403 | `FORBIDDEN` | 접근 권한 없는 파일 |
| 404 | `FILE_NOT_FOUND` | fileId 미존재 |
| 410 | `FILE_DELETED` | soft delete 또는 비활성 파일 |
| 500 | `STORAGE_READ_FAILED` | 디스크/S3 읽기 실패 |

---

## F-2. GET `/api/files/{fileId}/meta`

**설명**: 파일 메타데이터 조회. 다운로드/이미지 렌더링 전에 파일명·사이즈·domainType·fileType을 확인할 때 사용.
**인증**: USER

### 요청

```http
GET /api/files/137/meta
Authorization: Bearer <jwt>
```

### 응답 200

```json
{
  "success": true,
  "message": "요청 성공",
  "data": {
    "id": 137,
    "originalName": "chair-low-poly.fbx",
    "savedName": "550e8400.fbx",
    "savedUrl": "post/42/model/2026-05-27/batch-8f3a91/0001/550e8400.fbx",
    "extension": "fbx",
    "sizeBytes": 524288,
    "domainType": "POST",
    "domainId": 42,
    "uploadedBy": 12,
    "uploadOrder": 0,
    "contentType": "application/octet-stream",
    "fileType": "MODEL",
    "createdAt": "2026-05-21T14:30:15"
  }
}
```

| 필드 | 의미 |
|---|---|
| originalName | 사용자가 업로드한 원본 파일명 |
| savedName | 저장소에 실제 저장된 파일명. UUID 기반 충돌 방지 이름 |
| savedUrl | 저장 URL 또는 storage key/path |
| domainType | 연결 도메인. 예: `POST`, `USER`, `REQUEST` |
| domainId | 연결 도메인 리소스 id. 예: 게시글 id, 유저 id, 요청 id |
| uploadedBy | 업로드한 사용자 id |
| uploadOrder | 동일 리소스 내 파일 표시/처리 순서 |
| contentType | MIME 타입. 예: `image/png`, `application/octet-stream` |
| fileType | 파일 목적 분류. `MODEL`, `REFERENCE`, `THUMBNAIL`, `PROFILE` |

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | 접근 권한 없는 파일 |
| 404 | `FILE_NOT_FOUND` | |

---

## F-3. DELETE `/api/files/{fileId}`

**설명**: 파일 삭제 또는 비활성화. MVP에서는 DB row를 보존하는 soft delete를 권장한다.
**인증**: USER (소유자 또는 ADMIN)

### 요청

```http
DELETE /api/files/137
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
| 403 | `FORBIDDEN` | 소유자/관리자가 아님 |
| 404 | `FILE_NOT_FOUND` | |
| 409 | `FILE_IN_USE` | 현재 게시글/요청/프로필에서 사용 중이라 단독 삭제 불가 |
| 500 | `STORAGE_DELETE_FAILED` | 저장소 삭제 실패 |

---

## 부록 — 업로드 서비스 컨트랙트

업로드는 별도 REST 엔드포인트가 없고 각 도메인 API에서 multipart로 받는다. File 도메인의 서비스 계약:

```java
public interface FileService {
    StoredFile save(FileDomainType domainType, Long domainId, Long uploadedBy, MultipartFile file);
    List<StoredFile> saveAll(FileDomainType domainType, Long domainId, Long uploadedBy, List<MultipartFile> files);
    FileResource load(Long fileId, Long requesterId);
    FileResponse meta(Long fileId, Long requesterId);
    void delete(Long fileId, Long requesterId);
}
```

| domainType | 업로드 주체 | 대표 fileType | 제약 |
|---|---|---|---|
| `POST` | `POST /api/posts` 의 `files[]` | `MODEL` | 에셋 파일 `fbx, blend, obj, glb, gltf, zip`, 파일당 20MB |
| `POST` | `POST /api/posts` 의 `thumbnail` | `THUMBNAIL` | 대표 썸네일 `png, jpg, jpeg`, 5MB |
| `USER` | `POST /api/users/me/profile-image` | `PROFILE` | 프로필 이미지 `png, jpg, jpeg`, 5MB |
| `REQUEST` | `POST /api/requests` 의 `referenceThumbnail` | `REFERENCE` | 참고 썸네일 `png, jpg, jpeg`, 5MB |

| fileType | 기준 |
|---|---|
| `MODEL` | 게시글 본문 에셋 파일. `fbx`, `blend`, `obj`, `glb`, `gltf`, `zip` |
| `REFERENCE` | 요청 게시판 참고 이미지. `png`, `jpg`, `jpeg` |
| `THUMBNAIL` | 게시글 대표 썸네일 이미지. `png`, `jpg`, `jpeg` |
| `PROFILE` | 유저 프로필 이미지. `png`, `jpg`, `jpeg` |

| 위반 | 에러 |
|---|---|
| 0바이트 파일 | 400 `FILE_EMPTY` |
| 확장자 불일치 | 400 `FILE_EXTENSION_NOT_ALLOWED` |
| 크기 초과 | 400 `FILE_TOO_LARGE` |
| 저장 실패 | 500 `STORAGE_WRITE_FAILED` |

저장 실패 시 이미 저장된 파일은 cleanup한다. 저장 경로는 직접 노출하지 않고 `fileId`를 통해서만 조회한다. DB에는 원본 파일명(`originalName`), 저장 파일명(`savedName`), 저장 URL/key(`savedUrl`)을 분리해서 보존한다.

### 저장 URL 정책

저장된 파일 URL/key는 다음 정책으로 생성한다.

```text
{targetType}/{targetId}/{fileType}/{uploadedAt}/{uploadBatchId}/{sortOrder}/{uuid}.{extension}
```

| 세그먼트 | 예시 | 설명 |
|---|---|---|
| targetType | `post`, `user`, `request` | 연결 대상 |
| targetId | `42` | 연결 대상 id |
| fileType | `model`, `reference`, `thumbnail`, `profile` | 파일 목적 분류. DB enum 값은 대문자(`MODEL` 등), 저장 URL 세그먼트는 소문자 사용 |
| uploadedAt | `2026-05-27` | 업로드 일자 |
| uploadBatchId | `batch-8f3a91` | 한 번의 multipart 업로드 묶음 |
| sortOrder | `0001`, `0002`, `0003` | 동일 리소스 내 순서 |
| uuid | `550e8400...` | 충돌 방지 식별자 |

`savedName`은 `{uuid}.{extension}` 값을 저장하고, `savedUrl`은 위 정책으로 만든 전체 URL/key를 저장한다.

파일 보관 기간은 프로젝트 종료 후 한 달 뒤까지로 잡고, 현재 기준 삭제 예정일은 `2026-09-30`이다.
