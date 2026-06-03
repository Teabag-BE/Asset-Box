# 파트 가이드 — Post (게시글 본체, 단독)

> ⚠️ **2026-05-26 회의 반영** — 본 문서는 5/26 ERD 변경 기준 패치본.

## 회의 반영 핵심 변경 (Post 파트)

| 영역 | 새 내용 |
|---|---|
| **익명 열람 차단** | 게시글 목록·상세 모두 **로그인 필수**. 비회원 진입 401. 저작권/내부공유 정책 |
| **`linkedRequestId` 자동 완료** | 작성 본문(`data`)에 `linkedRequestId` 박으면 트랜잭션 내에서 해당 요청 `status → COMPLETED` 자동 전환. 별도 link-post 호출 X. **assignee 본인만** 가능 (검증) |
| **`linkedRequestId` 수정 불가** | 작성 시 1회만 박을 수 있고 PUT 으로 못 바꿈 |
| **목록 필터** | 쿼리에 `linkedRequestId` 추가 (특정 요청에 연결된 결과 게시글 검색) |
| **카테고리 연결** | 게시글은 소분류(`Category.depth=3`)에만 연결. 대/중분류로 작성하면 400 `CATEGORY_DEPTH_INVALID` |
| **대표 썸네일** | `AssetFile.thumbnail` 플래그를 쓰지 않고 `Post.thumbnailFileId` 로 단일 대표 썸네일을 가진다 |
| **파일 위임** | 업로드는 multipart 로 받되 저장은 **File 통합 도메인** 의 `FileService.save(purpose=ASSET, ownerId, file)` |
| **MVP 제외** | 어드민 강제 삭제(hard delete), 분쟁 처리 — 본 프로젝트 범위 외 |
| **페어 → 단독** | 회의 결과 Post 도메인 단독 1명 |



> 모르는 단어는 [`../00_overview/01_용어집.md`](../00_overview/01_용어집.md) 참고.
> 비유: **갤러리의 큐레이터.** 작품(에셋)을 진열대에 올리고, 라벨(태그·카테고리)을 붙이고, 관람객 수(조회수)와 인기도(좋아요)를 집계한다.

> 짝꿍: **Post-B (요청 게시판)**. 같은 게시글 패턴이지만 도메인 분리. 매주 1회 통합 점검.

---

## 1. 책임 한 줄

`Post` 엔티티의 CRUD, 좋아요(`PostLike`), 검색·정렬 입구, `linkedRequestId` 기반 요청 자동 완료, 어드민 조회용 목록.

---

## 2. 패키지 / 파일 소유권

### 내 소유
```
com.assetbox.post/
├─ controller/  PostController.java, AdminPostController.java
├─ service/     PostService.java, PostLikeService.java
├─ repository/  PostRepository.java, PostLikeRepository.java
├─ domain/      Post.java, PostLike.java
└─ dto/         PostCreateRequest, PostUpdateRequest, PostResponse, PostSummaryResponse
```

### 절대 손대지 말 것
- Post-B의 `request/` 전체
- File / Tag / Category / Comment 의 엔티티·리포지토리

---

## 3. 외부 컨트랙트

```java
public interface PostService {
    Post requireExists(Long postId);                  // notFound -> POST_NOT_FOUND
    PostResponse get(Long postId, Long requesterId);  // 조회수 +1
    PostResponse create(Long authorId, PostCreateRequest req,
                        List<MultipartFile> files, MultipartFile thumbnail);
    PostResponse update(Long postId, Long requesterId, PostUpdateRequest req);
    void softDelete(Long postId, Long requesterId);

    // 검색·목록 (Comment+Cat+Search가 호출하는 것이 아니라 컨트롤러가 직접 호출)
    Page<PostSummaryResponse> search(PostSearchCriteria criteria, Pageable pageable);
}
```

### Request 도메인과의 연결
- Post 작성 시 `linkedRequestId`가 있으면 `RequestService.completeByLinkedPost(requestId, authorId, postId)` 호출
- 별도 `/link-post` 호출은 없음

---

## 4. 의존하는 컨트랙트

- `UserService.requireExists(authorId)` — 작성자 검증
- `CategoryService.requireLeaf(categoryId)` — 소분류(depth=3) 검증
- `TagService.findOrCreateAll(List<String>)` — Set<Tag> 반환
- `FileService.save/saveAll(purpose, ownerId, uploadedBy, file)` — File
- `RequestService.completeByLinkedPost(requestId, assigneeId, linkedPostId)` — 요청 자동 완료

---

## 5. 단계별 작업 가이드

### M0 (5/22 ~ 5/25): 코드·문서 정독
- [ ] 기존 PostController, PostService 코드 정독
- [ ] PostCreateRequest 의 JSON 부분과 multipart 파싱 흐름 확인 (`@RequestPart`)

### M1 (5/27 ~ 6/5): MVP
- [ ] `create()` 트랜잭션: User 검증 → 소분류 Category 검증 → Post 저장 → Tag findOrCreate → FileService 저장 → `thumbnailFileId` 세팅 → linkedRequestId 있으면 Request 자동 완료
- [ ] `search()` 쿼리: criteria(`categoryId, tag, q, authorId, linkedRequestId, sort`) → Specification 또는 동적 JPQL
  - 정렬 화이트리스트: `createdAt`, `likeCount`, `viewCount` 만 허용
- [ ] `get()` 의 viewCount +1 은 별도 트랜잭션(`Propagation.REQUIRES_NEW`) — 통계 안정성

- [ ] PostResponse 매핑: User/Category/Tags/Files 를 DTO로 — 엔티티 직노출 X
- [ ] 통합 테스트 데이(6/1) — 작성·조회·파일조회·좋아요 사이클

### M2 (6/6 ~ 6/14): SHOULD
- [ ] `POST /posts/{id}/like` 토글 — UNIQUE 위반 예외를 좋아요 취소로 해석
- [ ] `/posts/liked` — 내가 좋아요 누른 글
- [ ] 어드민 게시글 목록 조회 (`/api/admin/posts`)
- [ ] popular-tags Caffeine 캐시 (Tag 파트 협업)

### M3 (6/15 ~ 6/16)
- [ ] viewCount 폭주 방지 (같은 사용자 30초 내 동일 글 카운트 X — 간단한 Bucket)
- [ ] 검색 응답시간 모니터링 (Infra 협업)

---

## 6. 인수 기준 (AC)

### P-01 작성
- [ ] author=현재 사용자
- [ ] 카테고리 미존재 404, 대/중분류 선택 400 `CATEGORY_DEPTH_INVALID`
- [ ] 태그 자유 입력, 동일 이름은 같은 Tag로 (대소문자 정규화)
- [ ] files 1개 이상 권장 (썸네일 없으면 경고 안내)
- [ ] 응답에 file 목록 + tag 목록 + categoryPath(대>중>소)
- [ ] linkedRequestId가 있으면 assignee 본인만 성공, 요청 status=COMPLETED, linkedPostId 세팅, DM 발송

### P-02 목록·검색
- [ ] `q` 는 title LIKE + content LIKE
- [ ] `tag` 는 Tag.name 매칭 후 IN
- [ ] `categoryId` 는 소분류(depth=3) 정확 매치
- [ ] `linkedRequestId` 필터 지원
- [ ] `sort` 화이트리스트 외는 400
- [ ] page 기본 0, size 기본 20, 최대 50

### P-04 수정
- [ ] 작성자 외 403 `FORBIDDEN`
- [ ] files 전체 재업로드 정책 (M2까지)

### P-06 좋아요
- [ ] 토글: 누른 적 있으면 취소
- [ ] likeCount 증감은 Post.addLike/removeLike 사용 (낙관락 검토)

---

## 7. 충돌 방지 / 함정

| 함정 | 결과 | 회피 |
|---|---|---|
| `Page<Post>` 그대로 컨트롤러 반환 | 응답 모양 제각각 | 항상 `PageResponse.of(page, this::toSummary)` |
| author 응답에 User 엔티티 통째로 | 직렬화 폭주 / 비번 노출 | `UserService.toResponse` 또는 자체 매퍼 사용 |
| Tag/Category/File 엔티티를 PostController에서 직접 import | 책임 경계 깨짐 | 서비스 경유 |
| 좋아요 동시 누름 → 카운트 어긋남 | 데이터 불일치 | UNIQUE 제약 + 트랜잭션 + 좋아요 카운트는 DB COUNT 또는 낙관락 |
| 페이지네이션 sort 무검증 | SQL 인젝션 위험 | 화이트리스트 |
| Post-B(Request) 와 같은 시점에 같은 DTO 이름 만듦 | 컴파일 충돌 | 패키지 분리로 무관 — 단, 클래스명에 `Post`/`Request` prefix |
