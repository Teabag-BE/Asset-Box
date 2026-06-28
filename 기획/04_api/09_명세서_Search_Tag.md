# 09. API 명세서 — Search & Tag (통합 검색 입구)

> 담당 파트: **Comment+Category+Search**
> 베이스 경로: 별도 없음. **모든 검색은 `GET /api/posts` 한 입구**로 모입니다. 인기 태그는 `GET /api/posts/popular-tags`.

> 비유: **백화점 안내 데스크.** 손님이 "샹들리에 어디예요?" 물어보면 결국 가구 매장 직원이 답한다. 검색 도메인은 별도 매장이 아니라 안내 카운터이고, 실제 응답은 Post 매장의 데이터.

---

## 1. 왜 별도 엔드포인트가 없는가

검색을 별도로 두면 다음 함정이 생깁니다:
- `/search/posts`, `/search/tags`, `/search/users` 등 N개 엔드포인트로 분기 → 프론트 라우팅 복잡
- 정렬·페이지네이션 규약이 도메인별로 달라짐
- 응답 스키마가 별도 → 같은 게시글인데 형태가 다름

**결정**: `GET /api/posts` 의 쿼리 파라미터 조합으로 검색을 표현. 별도 검색 도메인은 의도적으로 만들지 않음.

| 검색 의도 | URL |
|---|---|
| 키워드 검색 | `GET /api/posts?q=chair` |
| 태그 검색 | `GET /api/posts?tag=low-poly` |
| 카테고리 드릴다운 | `GET /api/posts?categoryId=12` |
| 작성자별 | `GET /api/posts?authorId=12` |
| 팀별 | `GET /api/posts?teamId=3` |
| 조합 | `GET /api/posts?q=chair&tag=low-poly&categoryId=12&sort=likeCount,desc` |

---

## 2. 정렬 옵션

| sort | 의미 | 기본 |
|---|---|---|
| `createdAt,desc` | 최신순 | ✓ |
| `createdAt,asc` | 오래된순 | |
| `likeCount,desc` | 인기순 | |
| `viewCount,desc` | 조회순 | |

화이트리스트 외는 400 `SORT_KEY_NOT_ALLOWED`.

---

## 3. 쿼리 결합 의미론

여러 파라미터를 동시에 보내면 **AND** 결합:

```
q=chair AND tag=low-poly AND categoryId=12
```

빈 값은 무시:
- `?q=` → q 무시
- `?tag=` → tag 무시

`tag` 는 **정확 매치**입니다. 부분 매치는 v1.1 (Elasticsearch 도입 시).

`q` 는 title + content 부분 매치 (LIKE %q%). 본 MVP는 단일 토큰 매치만, 향후 토크나이저는 v1.1.

`categoryId` 는 게시글에 직접 연결된 **소분류(depth=3)** 기준으로만 검색한다. 대/중분류의 자식 포함 검색은 v1.1.

---

## 4. 인기 태그

### S-1. GET `/api/posts/popular-tags`

**설명**: 사용 빈도 상위 태그. Caffeine 캐시 60초.
**인증**: USER

`05_명세서_Post.md` 의 **P-3** 와 동일. 본 문서에서는 검색 시나리오 관점에서 한 번 더 참조합니다.

### 응답 예

```json
{
  "success": true,
  "data": [
    { "name": "low-poly", "count": 87 },
    { "name": "furniture", "count": 64 },
    { "name": "stylized", "count": 41 }
  ]
}
```

---

## 5. 태그 정규화 규칙 (TagService 내부)

`Post 작성/수정 시 tags[]` 를 받으면 `TagService.findOrCreateAll` 가 다음을 수행:

| 입력 | 정규화 결과 |
|---|---|
| `"  Furniture  "` (앞뒤 공백) | `"furniture"` |
| `"Low-Poly"` | `"low-poly"` |
| `"한글태그"` | `"한글태그"` (변경 없음) |
| 30자 초과 | 400 `TAG_NAME_TOO_LONG` |
| 빈 문자열 | 무시 (배열에서 제외) |

정규화 규칙:
1. trim
2. 영문은 lowercase
3. 한글·숫자·하이픈·언더스코어는 그대로
4. 그 외 특수문자는 400 `TAG_NAME_INVALID_CHAR`

이 규칙은 `TagService` 단일 책임. 다른 도메인은 정규화를 직접 하지 않습니다.

---

## 6. 검색 응답 캐싱 / 성능 가이드

| 케이스 | 캐시 / 인덱스 |
|---|---|
| 인기 태그 | Caffeine `@Cacheable("popular-tags")` TTL 60s |
| `tag=...` 매칭 | `tags.name` UNIQUE 인덱스 + post_tags 조인 |
| `categoryId=...` | `posts.category_id` 인덱스. 값은 소분류(depth=3) |
| `authorId=...` | `posts.author_id` 인덱스 |
| `q=...` LIKE | MySQL: 인덱스 활용 어려움. v1.1 Elasticsearch |

응답 시간 SLA (목표): p95 < 400ms (개발 환경 기준, 50명 동시).

---

## 7. 자주 묻는 시나리오

### Q. "유저 검색"은 어디?
A. `GET /api/users/search?q=...` — `03_명세서_User.md` U-7 참조.

### Q. "요청 검색"은?
A. `GET /api/requests?q=...&status=...` — `06_명세서_Request.md` R-2 참조.

### Q. "댓글 검색"은?
A. v1.1. 본 MVP 미지원.

### Q. "Elasticsearch / Lucene 도입은?"
A. v1.1 백로그. M3까지는 RDB LIKE + 인덱스로 충분.

---

## 8. 에러 통합

검색 관련 모든 에러는 `GET /api/posts` (Post-2) 의 에러 표를 따릅니다.

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `PAGINATION_SIZE_TOO_LARGE` | size > 50 |
| 400 | `SORT_KEY_NOT_ALLOWED` | 화이트리스트 외 sort |
| 400 | `TAG_NAME_TOO_LONG` | 태그 길이 30자 초과 (작성 시) |
| 400 | `TAG_NAME_INVALID_CHAR` | 허용 외 특수문자 |
| 404 | `CATEGORY_NOT_FOUND` | categoryId 미존재 |
| 400 | `CATEGORY_DEPTH_INVALID` | categoryId가 소분류(depth=3)가 아님 |
| 404 | `USER_NOT_FOUND` | authorId 미존재 |
