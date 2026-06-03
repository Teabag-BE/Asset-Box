# 01. ERD (Entity Relationship Diagram)

> ⚠️ **2026-05-26 회의 반영** — 본 문서는 5/26 회의 결과를 반영한 v3 기준본. ERD/API/파트 가이드가 충돌하면 본 문서의 "회의 반영 ERD 변경"과 "한 장 다이어그램"을 우선한다.

## 회의 반영 ERD 변경 (핵심 — 본 박스 기준이 최신)


| 엔티티                         | 변경                                                                                                                  |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| **User**                    | `bio` 제거. 실명 컬럼은 `name VARCHAR(50) NOT NULL` 로 사용                                                                   |
| **User**                    | `provider_subject` 제거. OAuth 연결 여부는 `is_oauth_linked BOOLEAN NOT NULL DEFAULT FALSE` 로 관리 |
| **User**                    | `provider VARCHAR(20) NOT NULL` 추가 (`LOCAL / GOOGLE / NAVER`) |
| **EmailWhiteList**          | 이메일 화이트리스트 테이블 추가. 가입 가능 이메일을 `email VARCHAR(50) NOT NULL` 로 관리 |
| **Post**                    | 컬럼 추가: `linked_request_id BIGINT NULL` — 요청 게시판과 1:1 연결. 게시글 작성 시 박으면 요청 자동 COMPLETED                               |
| **Post**                    | 인덱스 추가: `idx_posts_linked_request (linked_request_id)`                                                              |
| **Post**                    | 썸네일 대표 여부를 `AssetFile.thumbnail` 에 두지 않고 `thumbnail_file_id BIGINT NULL` 로 Post가 직접 가진다                             |
| **Post**                    | 컬럼 추가: `total_file_size BIGINT NOT NULL DEFAULT 0`, `image_resolution VARCHAR(50) NULL`, `polygon BIGINT NOT NULL DEFAULT 0` |
| **Post ↔ Category**         | 게시글은 소분류(`categories.depth = 3`)에 연결한다. 대/중분류 ID로 작성 요청하면 400 `CATEGORY_DEPTH_INVALID`                              |
| **AssetFile (File 통합 도메인)** | `domain_type` + `domain_id` 로 연결 대상을 표현한다. `POST_THUMBNAIL` 목적은 제거하고 대표 썸네일은 Post가 가진다 |
| **AssetFile**               | 컬럼 추가: `domain_type VARCHAR(30) NOT NULL`, `domain_id BIGINT NOT NULL`, `uploaded_by BIGINT NOT NULL`, `upload_order BIGINT NOT NULL DEFAULT 0` |
| **RequestPost**             | 구조 변경 없음. `linked_post_id` 는 게시글 작성에서 역방향으로 자동 세팅됨                                                                  |
| **DownloadLog**             | **MVP 테이블 생성하지 않음** — v1.1 로 보류                                                                                     |
| **Feedback**                | `user_id BIGINT NOT NULL` FK로 User와 1:N 연결. 익명 피드백 없음                                                               |


> 본 박스 기준이 최신. 아래 본문 옛 ERD 와 충돌 시 본 박스 우선.

> 비유: **연락처 앱에서 사람과 사람을 잇는 화살표.** 화살표가 어디로 향하는지(소유 관계), 점선인지(선택), 1:N인지 N:M인지가 곧 ERD다.
>
> 이 문서는 **2026-05-26 회의 기준**으로 정리한 v3 ERD입니다. 변경 시 PR 본문에 "[ERD]" 태그 + 본 문서 동시 수정.

---

## 한 장 다이어그램

- 수정사항
- - User에서 `bio` 제거, 실명은 `name` 으로 관리
- User에 `provider`, `is_oauth_linked` 추가, `provider_subject` 제거
- Post는 Category 소분류(depth=3)와 연결
- Post에 `total_file_size`, `image_resolution`, `polygon` 추가
- Feedback은 `user_id` FK로 User와 1:N 연결
- AssetFile의 `thumbnail` 플래그를 제거하고 Post의 `thumbnail_file_id` 로 이동. 파일 순서는 `upload_order`로 관리

```
┌──────────────┐         ┌────────────────┐       N    M  ┌────────┐
│    User      │ 1     N │      Post      │───────────────│  Tag   │
│ id PK        │─────────│ id PK          │ post_tags     └────────┘
│ email UK     │ author  │ author_id FK   │
│ password     │         │ category_id FK │ N
│ name         │         │ thumbnail_file │────────────┐
│ nickname     │         │                │            │
│ provider     │         │ linked_request │            │
│ oauth_linked │         │ total_file_size│            │
│              │                 │ 1                   │
│ role(enum)   │                 │                     │
│ avatar_path  │                 │ N                   │
└──────┬───────┘          ┌──────┴───────┐      ┌──────┴───────┐
       │ 1              N │  AssetFile   │      │  AssetFile   │
       │──────────────────│ id PK        │      │ (thumbnail)  │
       │ uploaded_by      │ domain_id    │      │ id PK        │
       │                  │ domain_type  │      └──────────────┘
       │                  │ upload_order │
       │                  │ saved_url    │
       │                  │ deleted_at   │      ┌────────────┐
       │                  └──────────────┘      │ Category   │
       │                                        │ id PK      │
       │                           N          1 │ parent_id  │◀─┐
       │                  Post.category_id ─────│ depth(1~3) │  │ 셀프 참조
       │                  (depth=3 only)        │            │──┘
       │                                        └────────────┘
       │
       │ 1     N ┌─────────────┐
       ├─────────│  PostLike   │  (user_id, post_id) UNIQUE
       │         └─────────────┘
       │
       │ 1     N ┌─────────────┐         ┌─────────────┐
       ├─────────│  Comment    │ N     1 │   Post      │
       │ author  │ post_id FK  │─────────│ (위 박스)   │
       │         │ parent_id FK│◀─┐      └─────────────┘
       │         │ deleted     │  │ 대댓글 셀프 참조
       │         └─────────────┘──┘
       │
       │ 1     N ┌─────────────┐ 1    1  ┌────────────┐
       ├─────────│ RequestPost │─────────│ Post       │ (linked_post_id, nullable)
       │         │ requester_id│         └────────────┘
       │         │ assignee_id │
       │         │ status(enum)│
       │         └─────┬───────┘
       │               │ 1
       │               │ N
       │         ┌─────┴───────┐
       │         │RequestComm. │ (대댓글 parent_id, 셀프 참조)
       │         └─────────────┘
       │
       │ 1     N ┌─────────────┐
       └─────────│  Message    │ (sender_id, receiver_id 모두 User)
                 │ sender_id   │
                 │ receiver_id │
                 │ content     │
                 │ read        │
                 └─────────────┘
└──────────────┘
       1          N
       ┌──────────────┐
       │  Feedback    │
       │  id PK       │
       │  user_id FK  │
       │  title       │
       │  content     │
       │  status(enum)│
       └──────────────┘
```

---

## 테이블 상세

### users


| 컬럼               | 타입           | 제약                 | 비고                         |
| ---------------- | ------------ | ------------------ | -------------------------- |
| id               | BIGINT       | PK, AUTO_INCREMENT |                            |
| email            | VARCHAR(50)  | NOT NULL, UNIQUE   | 로그인 ID                     |
| password         | VARCHAR(255) | NULL               | BCrypt 해시. OAuth 유저는 NULL  |
| public_email     | VARCHAR(50)  | NULL               | 공개 이메일                     |
| name             | VARCHAR(50)  | NOT NULL           | 실명                         |
| nickname         | VARCHAR(30)  | NOT NULL           |                            |
| major            | VARCHAR(50)  | NOT NULL           | 전공                         |
| description      | TEXT         | NULL               | 자기소개                      |
| provider         | VARCHAR(20)  | NOT NULL           | LOCAL / GOOGLE / NAVER     |
| avatar_path      | VARCHAR(255) | NOT NULL           | 프로필 이미지 경로               |
| role             | VARCHAR(20)  | NOT NULL           | USER / ADMIN / SUPER_ADMIN |
| is_oauth_linked  | BOOLEAN      | NOT NULL DEFAULT FALSE | OAuth 연결 여부              |
| created_at       | TIMESTAMP    | NOT NULL           | BaseEntity                 |
| updated_at       | TIMESTAMP    | NOT NULL           | BaseEntity                 |
| deleted_at       | TIMESTAMP    | NULL               | soft delete                |

### email_white_list

| 컬럼       | 타입          | 제약                 | 비고              |
| ---------- | ----------- | ------------------ | ----------------- |
| id         | BIGINT      | PK, AUTO_INCREMENT |                   |
| email      | VARCHAR(50) | NOT NULL           | 가입 허용 이메일    |


### posts


| 컬럼                      | 타입           | 제약                           | 비고                       |
| ----------------------- | ------------ | ---------------------------- | ------------------------ |
| id                      | BIGINT       | PK                           |                          |
| title                   | VARCHAR(100) | NOT NULL                     |                          |
| content                 | TEXT         | NOT NULL                     |                          |
| author_id               | BIGINT       | FK → users.id, NOT NULL      |                          |
| category_id             | BIGINT       | FK → categories.id, NOT NULL | 소분류(depth=3)만 허용         |
| thumbnail_file_id       | BIGINT       | NULL                         | 대표 썸네일 AssetFile id      |
| view_count              | BIGINT       | NOT NULL DEFAULT 0           |                          |
| like_count              | BIGINT       | NOT NULL DEFAULT 0           |                          |
| total_file_size         | BIGINT       | NOT NULL DEFAULT 0           | 업로드 이미지/파일 총량       |
| image_resolution        | VARCHAR(50)  | NULL                         | 대표 이미지 해상도            |
| polygon                 | BIGINT       | NOT NULL DEFAULT 0           | 3D 에셋 폴리곤 수             |
| deleted_at              | TIMESTAMP    | NULL                         | soft delete              |
| created_at / updated_at | TIMESTAMP    | NOT NULL                     | BaseEntity               |


**인덱스**: `(author_id)`, `(category_id)`, `(thumbnail_file_id)`

### post_tags (조인 테이블)

| post_id | tag_id | PK 복합 |

### post_likes

| id PK | user_id FK | post_id FK | UNIQUE(user_id, post_id) |

### asset_files


| 컬럼                      | 타입           | 제약                     | 비고                                      |
| ----------------------- | ------------ | ---------------------- | --------------------------------------- |
| id                      | BIGINT       | PK                     |                                         |
| original_name           | VARCHAR(200) | NOT NULL               | 원본 파일명                                |
| saved_url               | VARCHAR(200) | NOT NULL               | 저장 URL 또는 경로                          |
| extension               | VARCHAR(30)  | NOT NULL               |                                         |
| size_bytes              | BIGINT       | NOT NULL               |                                         |
| domain_type             | VARCHAR(30)  | NOT NULL               | 연결 도메인. 예: POST / USER / REQUEST       |
| domain_id               | BIGINT       | NOT NULL               | 연결 도메인 리소스 id                        |
| deleted_at              | TIMESTAMP    | NULL                   | soft delete                             |
| created_at              | TIMESTAMP    | NOT NULL               | BaseEntity                              |
| updated_at              | TIMESTAMP    | NOT NULL               | BaseEntity                              |
| upload_order            | BIGINT       | NOT NULL DEFAULT 0     | 동일 리소스 내 파일 표시/처리 순서                  |
| uploaded_by             | BIGINT       | NOT NULL               | 업로더 user id                             |


**왜 domain_id에 FK를 안 거나?** → 향후 파일 도메인을 분리 서비스로 떼어내기 쉽도록 외래키 제약 대신 ID 참조만. 정합성은 서비스 레이어에서 보장. 게시글 대표 썸네일 여부는 `posts.thumbnail_file_id`가 가진다.

### download_logs


| 컬럼               | 타입           | 비고            |
| ---------------- | ------------ | ------------- |
| id PK            |              |               |
| user_id          | NULL 허용      | 비로그인도 다운로드 가능 |
| post_id, file_id | NOT NULL     | FK X (분리 준비)  |
| original_name    | VARCHAR(200) | 사후 분석용 스냅샷    |
| ip_address       | VARCHAR(50)  |               |
| user_agent       | VARCHAR(512) | truncate      |
| created_at       |              | 다운로드 시점       |


### categories


| 컬럼         | 타입          | 제약                       | 비고              |
| ---------- | ----------- | ------------------------ | --------------- |
| id PK      |             |                          |                 |
| name       | VARCHAR(50) | NOT NULL                 |                 |
| parent_id  | BIGINT      | FK → categories.id, NULL | 셀프 참조           |
| depth      | INT         | NOT NULL                 | 1=대 / 2=중 / 3=소 |

정렬은 MVP에서 `id` 또는 `name` 기준으로 처리하고, 별도 `sort_order` 컬럼은 사용하지 않는다.


### tags

| id PK | name VARCHAR(30) UNIQUE NOT NULL |

### comments


| 컬럼        | 타입                     | 비고          |
| --------- | ---------------------- | ----------- |
| id PK     |                        |             |
| post_id   | FK NOT NULL            |             |
| author_id | FK NOT NULL            |             |
| parent_id | FK NULL                | 대댓글 셀프 참조   |
| content   | VARCHAR(2000) NOT NULL |             |
| deleted   | BOOLEAN                | soft delete |


### request_posts


| 컬럼                                  | 타입                     | 비고                                                 |
| ----------------------------------- | ---------------------- | -------------------------------------------------- |
| id PK                               |                        |                                                    |
| title                               | VARCHAR(100)           |                                                    |
| content                             | TEXT                   |                                                    |
| asset_type, preferred_style, engine | VARCHAR(60)            | 요청 메타                                              |
| deadline                            | DATE                   |                                                    |
| status                              | VARCHAR(20)            | REQUESTED/IN_PROGRESS/COMPLETED                    |
| requester_id                        | FK → users.id NOT NULL |                                                    |
| assignee_id                         | FK → users.id NULL     | TA                                                 |
| linked_post_id                      | FK → posts.id NULL     | 완료 시 결과물 연결                                        |
| deleted                             | BOOLEAN                |                                                    |


**상태 전이 (강제)**:

```
REQUESTED ─▶ IN_PROGRESS ─▶ COMPLETED
```

MVP에서는 검토중, 반려, 재오픈, 취소/삭제 흐름을 제공하지 않는다.

### request_comments

구조는 `comments` 와 동일. (단, post_id 자리에 request_id)

### messages

| id PK | sender_id FK | receiver_id FK | content VARCHAR(2000) | is_read BOOLEAN | created_at | updated_at |
인덱스: `(sender_id)`, `(receiver_id)`.

### feedbacks

| id PK | user_id FK NOT NULL | title | content | user_nickname VARCHAR | status (NEW/READ) |

---

## 설계 결정 사유 정리


| 결정                                        | 사유                                                                               |
| ----------------------------------------- | -------------------------------------------------------------------------------- |
| **카테고리 셀프 참조 트리**                         | 화이트보드의 "단계별 드릴다운"과 1:1. 트리를 한 번에 안 내려준다.                                         |
| `**AssetFile`/`DownloadLog`에 FK 미설정**     | 파일 도메인 분리 준비. 정합성은 서비스 레이어에서.                                                    |
| **Post.thumbnail_file_id**                | 대표 썸네일은 게시글의 단일 속성. AssetFile에 boolean 플래그를 두면 여러 파일이 대표로 표시될 수 있어 Post가 직접 가진다. |
| **Post.category_id는 depth=3만 허용**         | 탐색 UI가 대/중/소분류 드릴다운이므로 게시글은 최종 소분류에만 연결한다.                                       |
| **soft delete (`deleted` 플래그)**           | 댓글/포스트는 신고/분쟁 흔적 보존. 어드민이 복구 가능.                                                 |
| **Tag 별도 테이블 + M:N**                      | 한 글에 여러 태그, 한 태그에 여러 글. findOrCreate 패턴으로 무한 증식 방지.                              |
| **request_post.linked_post_id (Post FK)** | 완료된 요청은 결과 게시글과 연결. 회의록 결정.                                                      |


---

## "한 사이클" 데이터 흐름 예시

> 시나리오: TA-김씨가 "캐주얼 의자 모델 필요" 요청 → TA-박씨가 직접 수락 → 박씨가 의자 만들어 게시글 작성 → 요청 완료 처리.

```
1. INSERT request_posts (requester=김씨, status='REQUESTED')
2. UPDATE request_posts SET assignee_id=박씨, status='IN_PROGRESS' WHERE id=1
   └─ MessageService.send(system→김씨, "요청이 박씨에게 배정되었습니다")
3. INSERT posts (author=박씨, category_id=소분류.id, ...), INSERT asset_files (postId=새포스트.id), UPDATE posts SET thumbnail_file_id=썸네일파일.id
4. UPDATE request_posts SET linked_post_id=새포스트.id, status='COMPLETED' WHERE id=1
   └─ MessageService.send(system→김씨, "요청이 완료되었습니다 → /posts/{id}")
```

요청과 결과 게시글은 서로 다른 테이블이지만 `linked_post_id` 로 1:1 연결.

---

## 마이그레이션 정책

- **M0 (5/22)**: 위 스키마를 v1로 동결. 그 후 변경은 모두 PR + Infra 리뷰 필수.
- **dev 프로파일**: `ddl-auto: create-drop` 로 매번 재생성. 시드는 `CategorySeeder` + `AdminBootstrapRunner` 가 담당.
- **prod 전환 시 (M2 후반, 6/12)**: `validate` 로 잠금. 이후 변경은 v1.1 백로그로.
- **Flyway / Liquibase 도입은 v1.1.** 이번 학기엔 도입 비용이 학습 효과보다 큼.
