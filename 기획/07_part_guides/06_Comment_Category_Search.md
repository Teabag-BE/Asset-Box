# 파트 가이드 — Comment + Category + Search (단독)

> ⚠️ **2026-05-22 회의 반영** — 본 문서는 회의 결과 패치본. 회의록 단일 진실 원본은 [`../04_api/02_엔드포인트_목록_상태코드분리_수정본.md`](../04_api/02_엔드포인트_목록_상태코드분리_수정본.md).



> 모르는 단어는 [`../00_overview/01_용어집.md`](../00_overview/01_용어집.md) 참고.
> 비유: **도서관 사서.** 책에 라벨(카테고리·태그)을 붙이고, 독자가 책을 찾을 때 검색 도와주고, 책 옆에 붙은 메모(댓글)도 정리한다. 도메인이 셋이지만, 셋 다 "글의 부수적 정보"라는 공통점으로 한 사람이 맡는다.

---

## 1. 책임 한 줄

`Comment` (Post 댓글), `Category` (트리), `Tag` (M:N + findOrCreate + 인기 캐시), **검색 입구(`GET /api/posts` 의 쿼리 해석)**.

> Request 도메인의 RequestComment 는 Post-B 소유지만, Comment 패턴을 본 파트가 정의·유지보수. Post-B는 그 패턴을 답습.

---

## 2. 패키지 / 파일 소유권

### 내 소유
```
com.assetbox.comment/    (전체)
com.assetbox.category/   (전체)
com.assetbox.tag/        (전체)
```

### 공동
- `PostController` 의 `popular-tags`, `search` 쿼리 입구 — Post-A가 컨트롤러 메서드를 제공하되, 로직은 본 파트의 service 가 처리하도록 위임
- `RequestCommentController` — Post-B 소유지만, Comment 패턴 변경 시 동기화

### 절대 손대지 말 것
- Post-A의 `Post` 엔티티 / Repository (검색 쿼리는 PostRepository에 메서드를 **추가**하는 방식으로 Post-A와 합의 후 머지)

---

## 3. 외부 컨트랙트

```java
public interface CommentService {
    Comment requireExists(Long commentId);
    Page<CommentResponse> listByPost(Long postId, Pageable p);
    CommentResponse create(Long postId, Long authorId, CommentCreateRequest req);
    void softDelete(Long commentId, Long requesterId);
}

public interface CategoryService {
    Category requireExists(Long id);
    List<CategoryResponse> roots();
    List<CategoryResponse> children(Long parentId);
    CategoryResponse create(CategoryCreateRequest req);   // ADMIN
    CategoryResponse update(Long id, CategoryUpdateRequest req); // ADMIN, name/sortOrder 수정
    void delete(Long id);                                  // ADMIN, 자식 있으면 400
}

public interface TagService {
    Set<Tag> findOrCreateAll(Collection<String> names);
    List<PopularTagDto> popularTags(int limit);           // Caffeine 캐시 60s
}
```

---

## 4. 의존하는 컨트랙트

- `PostService.requireExists(postId)` — 댓글 작성 시
- `UserService.requireExists(authorId)`
- (검색) `PostRepository` 의 Specification — Post-A와 사전 합의로 인덱스/메서드 추가

---

## 5. 단계별 작업 가이드

### M0 (5/22 ~ 5/25): 코드·문서 정독
- [ ] 세 도메인의 기존 코드 정독
- [ ] `CategorySeeder` 동작 확인 (대분류·중분류 데이터)

### M1 (5/27 ~ 6/5): MVP
- [ ] 카테고리 roots / children API 완성, depth 검증, `sortOrder` 정렬
- [ ] Tag findOrCreate (대소문자 정규화, 30자 제한, 공백 trim)
- [ ] Comment 작성 / 목록 / 삭제(soft)
- [ ] **검색 통합 입구**: PostController가 받은 쿼리(`q, tag, categoryId, authorId, sort`)를 본 파트의 service가 PostRepository로 위임. Specification 사용 권장.
- [ ] 통합 테스트 데이(6/1) 참여 — 카테고리 드릴다운 → 검색 → 댓글 작성 사이클

### M2 (6/6 ~ 6/14): SHOULD
- [ ] 카테고리 CRUD (ADMIN)
- [ ] 인기 태그 캐시 (Caffeine `@Cacheable`)
- [ ] 정렬 옵션 (likeCount/viewCount/createdAt 화이트리스트)
- [ ] RequestComment 패턴 점검 (Post-B 협업)

### M3 (6/15 ~ 6/16)
- [ ] 검색 응답시간 측정 + 인덱스 추가 (Infra 협업)
- [ ] 인기 태그가 자주 비어있는 케이스 fallback

---

## 6. 인수 기준 (AC)

### K-01 카테고리 lazy
- [ ] roots: depth=1 인 카테고리만 조회. 정렬은 `sortOrder` 기준
- [ ] children: depth ≤ 3, 4단계 시도 시 생성 자체가 400

### K-04 Tag findOrCreate
- [ ] 동시 호출 시 중복 생성 방지 (UNIQUE 제약 + 재조회 패턴)
- [ ] 정규화: trim + lowercase 추천. 정책 합의 후 적용
- [ ] 30자 초과 400

### K-06 검색
- [ ] q는 title/content LIKE %q%
- [ ] tag는 exact match
- [ ] categoryId는 게시글에 직접 연결된 소분류(depth=3)만 허용. 대/중분류 자식 포함 검색은 v1.1
- [ ] sort 화이트리스트 외 400

### C-01 댓글
- [ ] post 미존재 404
- [ ] parent 가 다른 글의 댓글이면 400 `COMMENT_PARENT_MISMATCH`
- [ ] soft delete 시 content 가 "삭제된 댓글입니다" 로 응답 마스킹

---

## 7. 충돌 방지 / 함정

| 함정 | 결과 | 회피 |
|---|---|---|
| `PostRepository` 에 검색 쿼리를 본인 PR로 막 추가 | Post-A와 충돌 | PR 전 Post-A에 알림, Specification 패턴 사용 |
| Tag 정규화 정책이 도메인마다 다름 | 같은 태그가 둘로 분기 | TagService 한 곳에서만 정규화 |
| Caffeine 캐시가 미적용된 상태로 평균 호출 측정 | 부정확 | `@EnableCaching` 확인 + 테스트에 캐시 미적용 분기 |
| 카테고리 depth>3 검증을 빼먹음 | 트리 무한확장 | Category 엔티티 생성자에서 이미 검증, 우회 경로 차단 |
| 댓글 페이지네이션 sort 누락 | 순서 들쑥날쑥 | `createdAt ASC` 기본 |
