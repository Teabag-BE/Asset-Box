# Asset 게시글 API 명세

이 문서는 프론트엔드에서 Asset 게시글 작성, 상세 조회, 다운로드, 3D 뷰어, 삭제 정책을 연동할 때 필요한 API 계약을 정리합니다.

## 공통 응답 형식

모든 API는 `ApiResponse` 래퍼로 응답합니다.

```json
{
  "success": true,
  "message": "요청 처리 메시지",
  "data": {},
  "error": null
}
```

실패 응답 예시:

```json
{
  "success": false,
  "message": "해당 요청이 실패되었습니다.",
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```

## 1. Asset 게시글 작성

```http
POST /api/posts
Content-Type: multipart/form-data
```

### Multipart 필드

| 필드명 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `request` | JSON | O | 게시글 본문 정보 |
| `thumbnail` | File | O | 게시글 썸네일 이미지 |
| `assetZip` | File | O | 3D 에셋 ZIP 파일 |

### `request` 예시

```json
{
  "title": "의자 모델",
  "content": "Low poly chair asset",
  "categoryId": 1,
  "tags": ["chair", "lowpoly"],
  "linkedRequestId": null
}
```

### 업로드 정책

- Asset 게시글은 `assetZip` 단일 ZIP 파일만 업로드합니다.
- 원본 ZIP은 다운로드용으로 저장됩니다.
- ZIP 내부의 FBX는 `MODEL` 타입으로 저장됩니다.
- ZIP 내부의 이미지 텍스처는 `TEXTURE` 타입으로 저장됩니다.
- 프론트엔드는 FBX/TEXTURE를 직접 업로드하지 않습니다.

## 2. 게시글 상세 조회

```http
GET /api/posts/{postId}
```

상세 조회 응답의 핵심 필드는 `files`, `downloadFile`, `viewer`입니다.

### 응답 예시

```json
{
  "success": true,
  "message": "게시글 단건 조회에 성공했습니다.",
  "data": {
    "id": 1,
    "title": "의자 모델",
    "content": "Low poly chair asset",
    "authorId": 1,
    "categoryId": 1,
    "categoryPath": [],
    "thumbnailKey": "assets/post/1/thumbnail/...",
    "thumbnailUrl": "https://s3...",
    "files": [
      {
        "fileId": 10,
        "originalName": "chair_asset.zip",
        "extension": "zip",
        "s3Key": "posts/1/original/....zip",
        "accessUrl": "https://s3...",
        "sizeBytes": 15432000,
        "fileType": "ZIP",
        "uploadOrder": 1
      }
    ],
    "downloadFile": {
      "fileId": 10,
      "originalName": "chair_asset.zip",
      "extension": "zip",
      "sizeBytes": 15432000,
      "fileType": "ZIP"
    },
    "tags": ["chair", "lowpoly"],
    "linkedRequestId": null,
    "viewer": {
      "postId": 1,
      "model": {
        "originalName": "chair.fbx",
        "accessUrl": "https://s3...",
        "fileType": "MODEL"
      },
      "textures": [
        {
          "originalName": "basecolor.png",
          "accessUrl": "https://s3...",
          "fileType": "TEXTURE"
        },
        {
          "originalName": "normal.jpg",
          "accessUrl": "https://s3...",
          "fileType": "TEXTURE"
        }
      ]
    }
  },
  "error": null
}
```

### `files`

- 상세 조회의 `files`에는 다운로드용 ZIP 파일만 포함됩니다.
- `MODEL`, `TEXTURE`는 `files`에 포함하지 않습니다.
- 다운로드 버튼에는 `files`보다 `downloadFile` 사용을 권장합니다.

### `downloadFile`

`downloadFile`은 사용자가 다운로드해야 하는 원본 ZIP 파일입니다.

```json
{
  "fileId": 10,
  "originalName": "chair_asset.zip",
  "extension": "zip",
  "sizeBytes": 15432000,
  "fileType": "ZIP"
}
```

프론트 다운로드 버튼은 반드시 `downloadFile.fileId`를 사용합니다.

```text
사용자 다운로드 클릭
-> downloadFile.fileId 확인
-> GET /api/files/{fileId}/download-url 호출
-> 반환된 URL로 이동 또는 다운로드 실행
```

### `viewer`

`viewer`는 three.js 미리보기 렌더링에 필요한 정보입니다.

```json
{
  "postId": 1,
  "model": {
    "originalName": "chair.fbx",
    "accessUrl": "https://s3...",
    "fileType": "MODEL"
  },
  "textures": []
}
```

- `model`: FBX 파일입니다.
- `textures`: 텍스처 이미지 목록입니다.
- `textures`는 없을 수 있으며, 이 경우 빈 배열 `[]`로 내려옵니다.
- `viewer`는 상세 조회에서 `null`일 수 있습니다. 이 경우 뷰어 영역을 숨기거나 미리보기 없음 상태로 처리합니다.

## 3. 뷰어 전용 조회

```http
GET /api/posts/{postId}/viewer
```

### 응답 예시

```json
{
  "success": true,
  "message": "게시글 단건 조회에 성공했습니다.",
  "data": {
    "postId": 1,
    "model": {
      "originalName": "chair.fbx",
      "accessUrl": "https://s3...",
      "fileType": "MODEL"
    },
    "textures": []
  },
  "error": null
}
```

### 케이스

| 케이스 | 응답 |
| --- | --- |
| MODEL 1개, TEXTURE 여러 개 | 정상 응답 |
| MODEL 1개, TEXTURE 없음 | 정상 응답, `textures: []` |
| MODEL 없음 | 예외 응답 |
| MODEL 2개 이상 | 예외 응답 |
| 게시글 없음 | 예외 응답 |

## 4. 다운로드 URL 요청

```http
GET /api/files/{fileId}/download-url
```

`fileId`는 게시글 상세 조회의 `downloadFile.fileId`를 사용합니다.

### 응답 예시

```json
{
  "success": true,
  "message": "Presigned URL 발급에 성공했습니다.",
  "data": "https://s3...",
  "error": null
}
```

### 다운로드 흐름

```text
GET /api/posts/1
-> data.downloadFile.fileId = 10

GET /api/files/10/download-url
-> data = ZIP 다운로드용 presigned URL

window.location.href = data
```

주의:

- 다운로드는 ZIP 파일 기준입니다.
- `MODEL`, `TEXTURE`의 fileId를 다운로드 버튼에 연결하지 않습니다.
- `MODEL`, `TEXTURE`는 뷰어 API에서만 사용합니다.

## 5. 게시글 삭제

```http
DELETE /api/posts/{postId}
```

### 응답 예시

```json
{
  "success": true,
  "message": "게시글 삭제에 성공했습니다.",
  "data": null,
  "error": null
}
```

### 삭제 후 7일 보관 정책

삭제 API 호출 후 프론트에서는 즉시 삭제된 것으로 처리하면 됩니다.

백엔드 내부 정책:

- 게시글은 즉시 soft delete 됩니다.
- ZIP, MODEL, TEXTURE 파일 메타데이터도 즉시 soft delete 됩니다.
- 썸네일과 S3 파일은 즉시 물리 삭제하지 않습니다.
- `purgeAt = deletedAt + 7일`로 실제 삭제 예정 시각을 기록합니다.
- 7일 후 스케줄러가 S3 객체를 실제 삭제합니다.
- S3 삭제 성공 시 `storageDeletedAt`을 기록합니다.
- S3 삭제 실패 시 로그를 남기고 다음 배치에서 재시도합니다.

조회 정책:

- soft delete 된 게시글은 목록/상세 조회에 노출되지 않습니다.
- soft delete 된 파일은 `files`, `downloadFile`, `viewer` 응답에 포함되지 않습니다.

## 6. 프론트 구현 체크리스트

- 게시글 작성 multipart 필드명은 `request`, `thumbnail`, `assetZip`을 사용합니다.
- `request`는 JSON 파트로 전송합니다.
- 다운로드 버튼은 `data.downloadFile.fileId`를 사용합니다.
- 다운로드 URL 응답의 `data` 값을 브라우저 이동 또는 다운로드 처리에 사용합니다.
- 상세 조회의 `files`는 ZIP만 온다고 가정합니다.
- three.js 뷰어는 `viewer.model.accessUrl`과 `viewer.textures[].accessUrl`을 사용합니다.
- `viewer === null`이면 미리보기 없음 상태로 처리합니다.
- `viewer.textures`가 빈 배열이어도 정상 케이스로 처리합니다.
- 삭제 성공 후에는 프론트 목록/상세 상태에서 해당 게시글을 제거합니다.
