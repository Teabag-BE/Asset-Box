# 01. ERD (Entity Relationship Diagram)

> ⚠️ **2026-05-26 회의 반영** — 본 문서는 5/26 회의 결과를 반영한 v3 기준본. ERD/API/파트 가이드가 충돌하면 본 문서의 "회의 반영 ERD 변경"과 "한 장 다이어그램"을 우선한다.

## 회의 반영 ERD 변경 (핵심 — 본 박스 기준이 최신)


| 엔티티                         | 변경                                                                                                                  |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| **User**                    | `bio` 제거. 실명 컬럼은 `name VARCHAR(50) NOT NULL` 로 사용                                                                   |
| **User**                    | 기존 프로필 이미지 경로 필드 제거. 프로필 이미지는 `profile_url VARCHAR(255) NULL` 로 URL만 보존 |
| **User**                    | `provider_subject` 제거. OAuth 연결 여부는 `is_oauth_linked BOOLEAN NOT NULL DEFAULT FALSE` 로 관리 |
| **User**                    | `provider VARCHAR(20) NOT NULL` 추가 (`LOCAL / GOOGLE / NAVER`) |
| **EmailWhiteList**          | 이메일 화이트리스트 테이블 추가. 가입 가능 이메일을 `email VARCHAR(50) NOT NULL` 로 관리 |
| **Post**                    | 컬럼 추가: `linked_request_id BIGINT NULL` — 요청 게시판과 1:1 연결. 게시글 작성 시 박으면 요청 자동 COMPLETED                               |
| **Post**                    | 인덱스 추가: `idx_posts_linked_request (linked_request_id)`                                                              |
| **Post**                    | 기존 썸네일 파일 id 필드 제거. 대표 썸네일은 `thumbnail_url VARCHAR(255) NULL` 로 URL만 보존하고 File과 FK 연결하지 않는다 |
| **Post**                    | 컬럼 추가: `total_file_size BIGINT NOT NULL DEFAULT 0`, `image_resolution VARCHAR(50) NULL`, `polygon BIGINT NOT NULL DEFAULT 0` |
| **Post ↔ Category**         | 게시글은 소분류(`categories.depth = 3`)에 연결한다. 대/중분류 ID로 작성 요청하면 400 `CATEGORY_DEPTH_INVALID`                              |
| **Category**                | `sort_order INT` 를 사용해 같은 parent 아래 표시 순서를 관리한다 |
| **File (통합 도메인)** | `domain_type` + `domain_id` 로 연결 대상을 표현한다. 대표 썸네일은 File 분류가 아니라 Post의 `thumbnail_url`이 가진다 |
| **File**               | 컬럼 추가: `saved_name VARCHAR(200) NOT NULL`, `saved_url VARCHAR(300) NOT NULL`, `domain_type VARCHAR(30) NOT NULL`, `domain_id BIGINT NOT NULL`, `uploaded_by BIGINT NOT NULL`, `upload_order BIGINT NOT NULL DEFAULT 0`, `content_type VARCHAR(100) NOT NULL`, `file_type VARCHAR(30) NOT NULL` |
| **RequestPost**             | `reference_thumbnail_url VARCHAR(255) NULL`, `deleted_at TIMESTAMP NULL` 로 관리한다. `linked_post_id` 는 게시글 작성에서 역방향으로 자동 세팅됨 |
| **RequestComment**          | Request 댓글은 `request_id`, `author_id`, `parent_id`, `deleted_at` 를 가진 별도 테이블로 관리한다 |
| **DownloadLog**             | **MVP 테이블 생성하지 않음** — v1.1 로 보류                                                                                     |
| **Feedback**                | 최신 ERDCloud 기준 MVP에서는 `id BIGINT` 만 둔다. 상세 피드백 필드는 v1.1에서 확정한다 |


> 본 박스 기준이 최신. 아래 본문 옛 ERD 와 충돌 시 본 박스 우선.

> 비유: **연락처 앱에서 사람과 사람을 잇는 화살표.** 화살표가 어디로 향하는지(소유 관계), 점선인지(선택), 1:N인지 N:M인지가 곧 ERD다.
>
> 이 문서는 **2026-05-26 회의 기준**으로 정리한 v3 ERD입니다. 변경 시 PR 본문에 "[ERD]" 태그 + 본 문서 동시 수정.

---

## 한 장 다이어그램

- 수정사항
- User에서 `bio` 제거, 실명은 `name` 으로 관리
- User에 `provider`, `is_oauth_linked` 추가, `provider_subject` 제거
- Post는 Category 소분류(depth=3)와 연결
- Post에 `total_file_size`, `image_resolution`, `polygon` 추가
- Feedback은 최신 ERDCloud 기준 MVP에서 `id`만 확정
- User 프로필 이미지는 `profile_url`, Post 대표 썸네일은 `thumbnail_url`, Request 참고 썸네일은 `reference_thumbnail_url` 로 URL만 보존
- File의 `thumbnail` 플래그와 Post의 기존 썸네일 파일 id 필드를 제거. 파일 순서는 `upload_order`로 관리
- File은 `content_type`으로 MIME 타입, `file_type`으로 파일 분류를 보존
- Category는 `sort_order`로 표시 순서를 보존
- RequestPost / RequestComment는 `deleted_at`으로 soft delete 처리

```text
┌────────────────────┐
│        User        │
│ id PK              │
│ email UK           │
│ password NULL      │
│ public_email       │
│ name               │
│ nickname           │
│ major              │
│ description        │
│ provider           │
│ profile_url        │
│ role               │
│ is_oauth_linked    │
│ created_at         │
│ updated_at         │
│ deleted_at         │
└──┬────────┬────────┘
   │        │
   │ 1      │ 1
   │        │
   │ N      │ N
┌──▼────────▼────────┐        N    M      ┌──────────────┐
│        Post        │────────────────────│      Tag     │
│ id PK              │     post_tags      │ id PK        │
│ category_id FK     │                    │ name UK      │
│ author_id FK       │                    └──────────────┘
│ thumbnail_url      │
│ linked_request_id  │◀──────────────┐
│ title              │               │
│ content            │               │ 1
│ view_count         │               │
│ like_count         │               │
│ deleted_at         │               │
│ total_file_size    │               │
│ image_resolution   │               │
│ polygon            │               │
└──┬────────┬────────┘               │
   │        │                        │
   │ N      │ N                      │
   │        │                        │
┌──▼───┐ ┌──▼────────────┐      ┌────▼──────────────┐
│Like  │ │   Comment     │      │    RequestPost    │
│user  │ │ post_id FK    │      │ id PK             │
│post  │ │ author_id FK  │      │ requester_id FK   │
│UNIQUE│ │ parent_id FK  │◀─┐   │ assignee_id FK    │
└──────┘ │ content       │  │   │ linked_post_id FK │
         │ deleted_at    │──┘   │ reference_thumb...│
         └───────────────┘      │ title             │
                                │ content           │
┌────────────────────┐          │ status            │
│      Category      │          │ deleted_at         │
│ id PK              │          └───────┬────────────┘
│ name               │                  │ 1
│ parent_id FK       │◀─┐               │
│ depth              │──┘               │ N
│ sort_order         │          ┌───────▼────────────┐
└─────────▲──────────┘          │  RequestComment    │
          │ 1                   │ request_id FK      │
          │                     │ author_id FK       │
          │ N                   │ parent_id FK       │
          │                     │ content            │
          │                     │ deleted_at         │
          │                     └────────────────────┘
          │
┌─────────┴──────────┐
│   Post.category    │
│ depth=3 only       │
└────────────────────┘

┌────────────────────┐
│        File        │
│ id PK              │
│ original_name      │
│ saved_name         │
│ saved_url          │
│ extension          │
│ size_bytes         │
│ content_type       │
│ file_type          │
│ domain_type        │
│ domain_id          │
│ upload_order       │
│ uploaded_by FK     │──────▶ User.id
│ deleted_at         │
└────────────────────┘
  domain_type + domain_id 로 POST / USER / REQUEST 등 연결
  대표 썸네일·프로필·요청 참고 썸네일은 FK가 아니라 각 도메인의 URL 컬럼으로 보존

┌────────────────────┐
│      Message       │
│ id PK              │
│ sender_id FK       │──────▶ User.id
│ receiver_id FK     │──────▶ User.id
│ content            │
│ is_read            │
│ created_at         │
│ updated_at         │
└────────────────────┘

┌────────────────────┐        ┌────────────────────┐
│   EmailWhiteList   │        │      Feedback      │
│ id PK              │        │ id PK              │
│ email              │        └────────────────────┘
└────────────────────┘
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
| profile_url      | VARCHAR(255) | NULL               | 프로필 이미지 URL                |
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


| 컬럼               | 타입           | 제약                           | 비고                  |
| ---------------- | ------------ | ---------------------------- | ------------------- |
| id               | BIGINT       | PK, AUTO_INCREMENT           |                     |
| category_id      | BIGINT       | FK → categories.id, NOT NULL | 소분류(depth=3)만 허용    |
| author_id        | BIGINT       | FK → users.id, NOT NULL      | 작성자                 |
| thumbnail_url    | VARCHAR(255) | NULL                         | 대표 썸네일 URL. File FK 없음 |
| linked_request_id | BIGINT      | FK → request_posts.id        | 요청 결과 게시글 연결        |
| title            | VARCHAR(100) | NOT NULL                     |                     |
| content          | TEXT         | NOT NULL                     |                     |
| view_count       | BIGINT       |                              | 조회수                 |
| like_count       | BIGINT       |                              | 좋아요 수               |
| deleted_at       | TIMESTAMP    | NULL                         | soft delete         |
| created_at       | TIMESTAMP    | NOT NULL                     | BaseEntity          |
| updated_at       | TIMESTAMP    |                              | BaseEntity          |
| total_file_size  | BIGINT       | NOT NULL                     | 업로드 이미지/파일 총량      |
| image_resolution | VARCHAR(100) |                              | 대표 이미지 해상도         |
| polygon          | BIGINT       |                              | 3D 에셋 폴리곤 수        |


**인덱스**: `(author_id)`, `(category_id)`, `(linked_request_id)`

### post_tags (조인 테이블)

| 컬럼      | 타입   | 제약                          | 비고 |
| -------- | ------ | ----------------------------- | ---- |
| post_id  | BIGINT | PK, FK → posts.id, NOT NULL   |      |
| tag_id   | BIGINT | PK, FK → tags.id, NOT NULL    |      |

### post_likes

| 컬럼       | 타입      | 제약                         | 비고       |
| ---------- | --------- | ---------------------------- | ---------- |
| id         | BIGINT    | PK, AUTO_INCREMENT           |            |
| user_id    | BIGINT    | FK → users.id, NOT NULL      |            |
| post_id    | BIGINT    | FK → posts.id, NOT NULL      |            |
| created_at | TIMESTAMP |                              | BaseEntity |
| updated_at | TIMESTAMP |                              | BaseEntity |

**제약**: `UNIQUE(user_id, post_id)`

### files


| 컬럼                      | 타입           | 제약                     | 비고                                      |
| ----------------------- | ------------ | ---------------------- | --------------------------------------- |
| id                      | BIGINT       | PK                     |                                         |
| original_name           | VARCHAR(200) | NOT NULL               | 원본 파일명                                |
| saved_name              | VARCHAR(200) | NOT NULL               | 저장소에 실제 저장된 파일명. UUID 기반 충돌 방지 이름 |
| saved_url               | VARCHAR(300) | NOT NULL               | 저장 URL 또는 경로                          |
| extension               | VARCHAR(30)  | NOT NULL               |                                         |
| size_bytes              | BIGINT       | NOT NULL               |                                         |
| content_type            | VARCHAR(100) | NOT NULL               | MIME 타입. 예: `image/png`, `application/octet-stream` |
| file_type               | VARCHAR(30)  | NOT NULL               | 파일 목적 분류. `MODEL` / `REFERENCE` / `THUMBNAIL` / `PROFILE` |
| domain_type             | VARCHAR(30)  | NOT NULL               | 연결 도메인. 예: POST / USER / REQUEST       |
| domain_id               | BIGINT       | NOT NULL               | 연결 도메인 리소스 id                        |
| deleted_at              | TIMESTAMP    | NULL                   | soft delete                             |
| created_at              | TIMESTAMP    | NOT NULL               | BaseEntity                              |
| updated_at              | TIMESTAMP    | NOT NULL               | BaseEntity                              |
| upload_order            | BIGINT       | NOT NULL DEFAULT 0     | 동일 리소스 내 파일 표시/처리 순서                  |
| uploaded_by             | BIGINT       | NOT NULL               | 업로더 user id                             |


**왜 domain_id에 FK를 안 거나?** → 향후 파일 도메인을 분리 서비스로 떼어내기 쉽도록 외래키 제약 대신 ID 참조만. 정합성은 서비스 레이어에서 보장한다. 게시글 대표 썸네일, 유저 프로필, 요청 참고 썸네일은 각 도메인이 URL 문자열로 가진다.

### download_logs

MVP 최신 ERDCloud에는 포함하지 않는다. 다운로드 로그/통계는 v1.1에서 별도 `DownloadLog` 또는 감사(audit) 테이블로 확정한다.


### categories


| 컬럼         | 타입          | 제약                       | 비고              |
| ---------- | ----------- | ------------------------ | --------------- |
| id         | BIGINT      | PK, AUTO_INCREMENT       |                 |
| name       | VARCHAR(50) | NOT NULL                 |                 |
| parent_id  | BIGINT      | FK → categories.id, NULL | 셀프 참조           |
| depth      | INT         | NOT NULL                 | 1=대 / 2=중 / 3=소 |
| sort_order | INT         |                          | 같은 parent 내 표시 순서 |
| created_at | TIMESTAMP   |                          | BaseEntity      |
| updated_at | TIMESTAMP   |                          | BaseEntity      |


### tags

| 컬럼       | 타입         | 제약                       | 비고       |
| ---------- | ------------ | -------------------------- | ---------- |
| id         | BIGINT       | PK, AUTO_INCREMENT         |            |
| name       | VARCHAR(30)  | NOT NULL, UNIQUE           |            |
| created_at | TIMESTAMP    |                            | BaseEntity |
| updated_at | TIMESTAMP    |                            | BaseEntity |

### comments


| 컬럼        | 타입           | 제약                     | 비고          |
| --------- | ------------ | ---------------------- | ----------- |
| id        | BIGINT       | PK, AUTO_INCREMENT     |             |
| post_id   | BIGINT       | FK → posts.id, NOT NULL |             |
| parent_id | BIGINT       | FK → comments.id, NULL | 대댓글 셀프 참조   |
| author_id | BIGINT       | FK → users.id, NOT NULL |             |
| content   | VARCHAR(2000) | NOT NULL               |             |
| deleted_at | TIMESTAMP   | NULL                    | soft delete |
| created_at | TIMESTAMP   |                        | BaseEntity  |
| updated_at | TIMESTAMP   |                        | BaseEntity  |


### request_posts


| 컬럼             | 타입           | 제약                            | 비고                              |
| ---------------- | ------------ | ------------------------------- | --------------------------------- |
| id               | BIGINT       | PK, AUTO_INCREMENT              |                                   |
| requester_id     | BIGINT       | FK → users.id, NOT NULL         | 요청자                            |
| assignee_id      | BIGINT       | FK → users.id, NULL             | 담당 TA                           |
| linked_post_id   | BIGINT       | FK → posts.id, NULL             | 완료 시 결과물 연결                |
| reference_thumbnail_url | VARCHAR(255) | NULL                       | 요청 참고 썸네일 URL               |
| title            | VARCHAR(100) | NOT NULL                        |                                   |
| content          | TEXT         | NOT NULL                        |                                   |
| asset_type       | VARCHAR(50)  |                                 | 요청 에셋 종류                    |
| preferred_style  | VARCHAR(50)  |                                 | 선호 스타일                       |
| engine           | VARCHAR(50)  |                                 | Unity / Unreal 등                 |
| deadline         | DATE         |                                 |                                   |
| status           | VARCHAR(20)  | NOT NULL                        | REQUESTED/IN_PROGRESS/COMPLETED   |
| deleted_at       | TIMESTAMP    | NULL                            | soft delete                       |
| created_at       | TIMESTAMP    |                                 | BaseEntity                        |
| updated_at       | TIMESTAMP    |                                 | BaseEntity                        |


**상태 전이 (강제)**:

```
REQUESTED ─▶ IN_PROGRESS ─▶ COMPLETED
```

MVP에서는 검토중, 반려, 재오픈, 취소/삭제 흐름을 제공하지 않는다.

### request_comments

| 컬럼         | 타입           | 제약                              | 비고          |
| ------------ | ------------ | --------------------------------- | ------------- |
| id           | BIGINT       | PK, AUTO_INCREMENT                |               |
| request_id   | BIGINT       | FK → request_posts.id, NOT NULL   |               |
| author_id    | BIGINT       | FK → users.id, NOT NULL           |               |
| parent_id    | BIGINT       | FK → request_comments.id, NULL    | 대댓글 셀프 참조 |
| content      | VARCHAR(2000) | NOT NULL                         |               |
| deleted_at   | TIMESTAMP    | NULL                              | soft delete   |
| created_at   | TIMESTAMP    | NOT NULL                          | BaseEntity    |
| updated_at   | TIMESTAMP    |                                   | BaseEntity    |

### messages

| 컬럼        | 타입           | 제약                         | 비고       |
| ----------- | ------------ | ---------------------------- | ---------- |
| id          | BIGINT       | PK, AUTO_INCREMENT           |            |
| sender_id   | BIGINT       | FK → users.id, NOT NULL      | 발신자     |
| receiver_id | BIGINT       | FK → users.id, NOT NULL      | 수신자     |
| content     | VARCHAR(2000) | NOT NULL                    |            |
| is_read     | BOOLEAN      | NOT NULL                     | 읽음 여부  |
| created_at  | TIMESTAMP    | NOT NULL                     | BaseEntity |
| updated_at  | TIMESTAMP    |                              | BaseEntity |

인덱스: `(sender_id)`, `(receiver_id)`.

### feedbacks

| 컬럼 | 타입   | 제약                 | 비고 |
| ---- | ------ | -------------------- | ---- |
| id   | BIGINT | PK, AUTO_INCREMENT   |      |

---

## 설계 결정 사유 정리


| 결정                                        | 사유                                                                               |
| ----------------------------------------- | -------------------------------------------------------------------------------- |
| **카테고리 셀프 참조 트리 + sort_order**              | 화이트보드의 "단계별 드릴다운"과 1:1. 같은 parent 아래 표시 순서는 `sort_order` 로 제어한다.                    |
| **File FK 미설정**                       | 파일 도메인 분리 준비. `domain_type` + `domain_id` 로 참조하고 정합성은 서비스 레이어에서 보장한다.             |
| **Post.thumbnail_url / User.profile_url** | 썸네일·프로필은 File FK가 아니라 URL 문자열로 보존한다. 화면 표시용 이미지는 URL만 있으면 충분하고 도메인 결합을 줄일 수 있다. |
| **Post.category_id는 depth=3만 허용**         | 탐색 UI가 대/중/소분류 드릴다운이므로 게시글은 최종 소분류에만 연결한다.                                       |
| **soft delete (`deleted_at`)** | 댓글/포스트/요청은 신고·분쟁 흔적 보존. 최신 ERDCloud 기준으로 삭제 여부는 `deleted_at` 으로 관리한다.                    |
| **Tag 별도 테이블 + M:N**                      | 한 글에 여러 태그, 한 태그에 여러 글. findOrCreate 패턴으로 무한 증식 방지.                              |
| **request_post.linked_post_id (Post FK)** | 완료된 요청은 결과 게시글과 연결. 회의록 결정.                                                      |


---

## "한 사이클" 데이터 흐름 예시

> 시나리오: TA-김씨가 "캐주얼 의자 모델 필요" 요청 → TA-박씨가 직접 수락 → 박씨가 의자 만들어 게시글 작성 → 요청 완료 처리.

```
1. INSERT request_posts (requester=김씨, status='REQUESTED')
2. UPDATE request_posts SET assignee_id=박씨, status='IN_PROGRESS' WHERE id=1
   └─ MessageService.send(system→김씨, "요청이 박씨에게 배정되었습니다")
3. INSERT posts (author=박씨, category_id=소분류.id, thumbnail_url=저장된URL, ...), INSERT files (...)
4. UPDATE request_posts SET linked_post_id=새포스트.id, status='COMPLETED' WHERE id=1
   └─ MessageService.send(system→김씨, "요청이 완료되었습니다 → /posts/{id}")
```

요청과 결과 게시글은 서로 다른 테이블이지만 `linked_post_id` 로 1:1 연결.

---

## 마이그레이션 정책

- **M0 (5/22)**: 위 스키마를 v1로 동결. 그 후 변경은 모두 PR + Infra 리뷰 필수.
- **dev 프로파일**: `ddl-auto: create-drop` 로 매번 재생성. 시드는 `CategorySeeder` + `AdminBootstrapRunner` 가 담당.
- **prod 전환 시 (M2 후반, 6/12)**: `validate` 로 잠금. 이후 변경은 Flyway migration 으로만 반영한다.
- **Flyway Migration**: Spring Boot starter 의존성에 포함한다. MVP부터 스키마 변경 이력을 관리한다.
