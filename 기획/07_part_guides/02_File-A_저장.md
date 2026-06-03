# 파트 가이드 — File (통합 도메인, 2명 페어)

> ⚠️ **2026-05-26 회의 반영** — 본 문서는 5/26 ERD 변경 기준 패치본.

## 회의 반영 핵심 변경 (File 통합 도메인)

> 회의 결정: **File-A / File-B 분리가 없어지고 통합 1개 도메인.** 두 명이 같은 도메인을 페어 프로그래밍으로 같이 맡되, 내부에서 자유롭게 분담. (03번 File-B 가이드는 v1.1 참고용으로만 보관)

| 영역 | 새 내용 |
|---|---|
| **단일 도메인** | 저장 + 조회 + 메타 + 삭제 모두 한 도메인. `domainType` + `domainId` 로 연결 대상을 표현 |
| **단일 입구** | `GET /api/files/{fileId}` 가 모든 파일/이미지 조회의 입구. 게시글·프로필·참고이미지 전부 여기 |
| **업로드 위임** | 업로드 자체는 Post / User / Request API 가 multipart 로 받고 `FileService.save(domainType, domainId, uploadedBy, file)` 에 위임 |
| **대표 썸네일 위치** | 대표 썸네일은 File FK가 아니라 Post의 `thumbnailUrl` 문자열이 가진다. File에는 `thumbnail` boolean 없음 |
| **두 명 내부 분담 (권장)** | A=업로드/저장 백엔드/검증 정책 · B=조회/메타/삭제/권한. 둘이 페어로 인터페이스 합의 |
| **8가지 책임** | 저장 / 조회 / 메타 / 삭제 / 공용 서비스 / domainType 정책 / 권한 검증 / 저장 백엔드 추상화 |
| **MVP 제외 (v1.1)** | DownloadLog, top-files, 통계, S3 전환, presigned URL |

> 아래 본문 옛 가이드(File-A 단독)와 충돌 시 본 박스 우선.



> 모르는 단어는 [`../00_overview/01_용어집.md`](../00_overview/01_용어집.md) 참고.
> 비유: **택배 보관소.** 사람들이 맡긴 박스(파일)를 깔끔하게 분류해서 선반(디스크/S3)에 올린다. 받을 때 박스의 무게·내용물(확장자)을 검사하고, 손상되면 거절한다.

> 짝꿍: 같은 File 통합 도메인의 페어. A/B 이름은 내부 분담용일 뿐, MVP 컨트랙트는 하나다.

---

## 1. 책임 한 줄

파일 업로드 위임 처리, 확장자/사이즈 검증, 저장 추상화(`FileStorageService`), 파일 조회/메타/삭제까지 포함한 통합 File 라이프사이클.

---

## 2. 패키지 / 파일 소유권

### 내 소유 (자유 수정)
```
com.assetbox.file/
├─ service/
│   ├─ FileStorageService.java        (인터페이스 — 페어 공동)
│   ├─ LocalFileStorageService.java
│   └─ FileService.java               (save/load/meta/delete)
├─ repository/
│   └─ FileRepository.java
└─ domain/
    └─ File.java
```

### 공동 (페어 — File-B와 매일 동기화)
- `FileStorageService.java` 인터페이스 → A가 저장 메서드, B가 로딩 메서드를 함께 정의

### 절대 손대지 말 것
- `Post` / `Request` / 다른 도메인 패키지
- v1.1 참고용 `DownloadLog` 계열을 MVP 범위로 되살리지 말 것

---

## 3. 외부 컨트랙트 (내가 제공)

```java
public interface FileService {
    StoredFile save(FileDomainType domainType, Long domainId, Long uploadedBy, MultipartFile file);
    List<StoredFile> saveAll(FileDomainType domainType, Long domainId, Long uploadedBy, List<MultipartFile> files);
    FileResource load(Long fileId, Long requesterId);
    FileResponse meta(Long fileId, Long requesterId);
    void delete(Long fileId, Long requesterId);
}
```

### 컨트랙트 보장
- domainType: `POST / USER / REQUEST`
- 확장자 화이트리스트: 에셋 `fbx, blend, obj, glb, gltf, zip`, 이미지 `png, jpg, jpeg`
- 합계 사이즈 ≤ 20MB
- `uploadedBy` 로 업로더 사용자 id를 보존
- `uploadOrder` 로 동일 리소스 내 파일 표시/처리 순서를 보존
- `contentType` 으로 MIME 타입을 보존 (`image/png`, `application/octet-stream` 등)
- `fileType` 으로 파일 목적 분류를 보존 (`MODEL`, `REFERENCE`, `THUMBNAIL`, `PROFILE`)
- 게시글 대표 썸네일, 유저 프로필, 요청 참고 썸네일 파일은 `png/jpg/jpeg` 만, 각각 5MB 이하. 저장된 URL을 각 도메인이 `thumbnailUrl`, `profileUrl`, `referenceThumbnailUrl`로 보존
- 저장 실패 시 이미 디스크에 쓴 파일은 cleanup (`@Transactional` + try-catch + 파일 삭제)
- `File.savedName` 은 UUID 기반 실제 저장 파일명이다. 예: `550e8400.fbx`
- `File.savedUrl` 은 **storage backend가 바뀌어도 의미를 유지하는 URL/key** 로 관리한다. 로컬은 조회 가능한 상대 URL, S3는 object key 또는 CDN URL을 저장한다.

---

## 4. 의존하는 컨트랙트

- **없음(엔티티 의존 X)**: postId 만 받는다. 굳이 PostService를 부를 필요 없음 (호출 시점에 이미 Post가 저장된 상태).

---

## 5. 단계별 작업 가이드

### M0 (5/22 ~ 5/25): 코드·문서 정독
- [ ] 기존 `file/` 패키지 정독, 인터페이스가 잘 분리되었는지 확인
- [ ] 페어와 30분 미팅 — `FileService` / `FileStorageService` 인터페이스 합의

### M1 (5/27 ~ 6/5): MVP
- [ ] `FileService.save/saveAll` 트랜잭션 + 보상 로직 (디스크 cleanup) 견고화
- [ ] 확장자 검증 상수 `FileExtensionPolicy` 추출, 변경 한 곳에서
- [ ] 파일 조회 `GET /api/files/{fileId}`, 메타 `GET /api/files/{fileId}/meta` 구현
- [ ] File 응답 DTO `FileResponse{id, originalName, savedName, savedUrl, extension, sizeBytes, domainType, domainId, uploadedBy, uploadOrder, contentType, fileType}` 통일
- [ ] Post-A와 페어 작업: `POST /api/posts` 의 multipart 파싱 + FileService 호출 흐름
- [ ] 게시글 수정 시 파일 부분 교체 정책 합의 (전체 재업로드 vs 추가/삭제 별도 API) — 본 MVP는 **전체 재업로드** 로 시작 (간단)
- [ ] 통합 테스트 데이(6/1) 참여

### M2 (6/6 ~ 6/14)
- [ ] 파일 soft delete/비활성화 정책 점검
- [ ] 저장 백엔드 인터페이스 점검 (S3 전환 v1.1 준비) — `FileStorageService` 만 구현체 바꾸면 되는 설계 유지

### M3 (6/15 ~ 6/16)
- [ ] 업로드 실패 케이스 정리 (네트워크 중단, 디스크 가득 참 등)
- [ ] 로그/메트릭: 업로드 횟수/평균 사이즈 (Infra 협업)

---

## 6. 인수 기준 (AC)

### F-01 업로드
- [ ] 확장자 위반 → 400 `FILE_EXTENSION_NOT_ALLOWED`
- [ ] 합계 20MB 초과 → 400 `FILE_TOO_LARGE`
- [ ] 0바이트 파일 → 400 `FILE_EMPTY`
- [ ] DB에 File 저장 + 디스크에 파일 존재 확인 (통합 테스트)
- [ ] 도중 실패 시 cleanup (트랜잭션 롤백 + 디스크 파일 삭제)

### F-02 게시글 대표 썸네일 파일
- [ ] File은 이미지 확장자/용량 검증과 저장만 담당
- [ ] 게시글당 대표 썸네일 1개 제약은 Post의 `thumbnailUrl`로 보장
- [ ] 기존 썸네일 교체 정책은 PostService가 기존 파일 비활성화를 FileService에 요청

### F-04 파일 삭제/비활성화
- [ ] 소유자/관리자만 삭제 가능
- [ ] 사용 중인 파일 단독 삭제는 409 `FILE_IN_USE`
- [ ] soft delete 후 조회 시 410 `FILE_DELETED`

---

## 7. 충돌 방지 / 함정

| 함정 | 결과 | 회피 |
|---|---|---|
| 저장 실패 시 cleanup 누락 | 디스크에 쓰레기 누적 | try-finally 로 보장. 통합 테스트에서 의도적 실패 케이스 검증 |
| Post에서 File 직접 생성 | 책임 경계 깨짐 | "FileService.save/saveAll 만 사용" 룰. Post-A와 매주 확인 |
| 저장 경로를 절대경로로 박음 | 운영 환경 깨짐 | 항상 상대 경로 + Storage 인터페이스가 base 결합 |
| `MultipartFile.getOriginalFilename()` 그대로 저장 | 경로 트래버설 위험 | UUID 또는 hash 기반으로 storedName 생성, originalName은 메타로만 |
| v1.1용 DownloadLog를 MVP에 끼워 넣음 | API/ERD 범위 폭증 | 다운로드 로그·통계는 v1.1로 분리 |
