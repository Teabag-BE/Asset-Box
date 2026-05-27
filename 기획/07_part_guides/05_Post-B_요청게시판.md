# 파트 가이드 — Request (요청 게시판, 단독)

> ⚠️ **2026-05-22 회의 반영** — 본 문서는 회의 결과 패치본. 회의록 단일 진실 원본은 [`../04_api/02_엔드포인트_목록_상태코드분리_수정본.md`](../04_api/02_엔드포인트_목록_상태코드분리_수정본.md).

## 회의 반영 핵심 변경 (Request 파트)

| 영역 | 새 내용 |
|---|---|
| **어드민 개입 없음** | `assign / reject / reopen` 모두 USER 권한. 어드민은 모니터링만 |
| **assign = TA 본인 수락** | `PATCH /api/requests/{id}/assign` 본문 없음. 호출자가 자동 assignee. 자동 `IN_PROGRESS`. 이미 assignee 있으면 409 |
| **`/review` 단계 삭제** | 본 MVP는 REQUESTED → IN_PROGRESS 직행 |
| **`/link-post` 단계 삭제** | Post 작성 시 `linkedRequestId` 로 자동 처리. 별도 엔드포인트 X |
| **자동 COMPLETED** | Post 도메인이 자동 호출. RequestService 는 PostService 가 호출할 트랜잭션 메서드 제공 |
| **참고 썸네일** | 요청 작성 multipart 에 `referenceThumbnail` 추가. purpose=REQUEST_REFERENCE 로 File 도메인 저장 |
| **DM 알림** | 모든 상태 변경 시 시스템 발신자가 요청자에게 DM 자동 발송 |
| **페어 → 단독** | 회의 결과 Request 도메인 단독 1명 (이전 Post-B 호칭은 폐기) |



> 모르는 단어는 [`../00_overview/01_용어집.md`](../00_overview/01_용어집.md) 참고.
> 비유: **민원 창구.** 시민(요청자)이 "이거 만들어 주세요" 신청서 제출 → 담당 공무원(TA) 배정 → 처리 중 → 완료 시 결과물 첨부. 단계마다 발송되는 안내문(DM 알림)이 끊기면 시민이 결과를 못 본다.

> 짝꿍: **Post-A**. 같은 패턴이지만 라이프사이클이 다르고, 완료 시 Post-A의 `linkedRequestId` 작성 흐름과 연결된다. 매주 1회 통합 점검.

---

## 1. 책임 한 줄

`RequestPost` + `RequestComment` 의 CRUD, **요청자/assignee 중심 상태 흐름**, TA 본인 수락(assign), Post 작성 시 자동 완료, 상태 변경 DM 알림.

---

## 2. 패키지 / 파일 소유권

### 내 소유
```
com.assetbox.request/
├─ controller/  RequestPostController.java, RequestCommentController.java
├─ service/     RequestPostService.java, RequestStatusService.java, RequestCommentService.java
├─ repository/  RequestPostRepository.java, RequestCommentRepository.java
├─ domain/      RequestPost.java, RequestComment.java, RequestStatus.java
└─ dto/         RequestCreateRequest, RequestResponse, RequestStatusChangeRequest
```

### 절대 손대지 말 것
- Post-A의 `post/` 전체 (엔티티 import 도 지양 — `PostService.requireExists` 만)
- DM의 `message/` 전체 (`MessageService.send` 만 호출)

---

## 3. 외부 컨트랙트

```java
public interface RequestPostService {
    RequestPost requireExists(Long id);
    RequestResponse get(Long id);
    RequestResponse create(Long requesterId, RequestCreateRequest req);
    RequestResponse update(Long id, Long requesterId, RequestCreateRequest req);
    RequestResponse assign(Long id, Long currentUserId);       // USER 본인 수락
    RequestResponse reject(Long id, Long currentUserId, String reason);
    RequestResponse reopen(Long id, Long requesterId, RequestStatus targetStatus);
    RequestResponse completeByLinkedPost(Long id, Long assigneeId, Long postId);
    void softDelete(Long id, Long requesterId);
    Page<RequestResponse> search(RequestSearchCriteria c, Pageable p);
}

public interface RequestStatusService {
    void requireValidTransition(RequestStatus from, RequestStatus to, AuthUser actor);
}
```

### 상태 전이 표 (RequestStatusService에서 강제)

| from | to | 트리거 | actor |
|---|---|---|---|
| REQUESTED | IN_PROGRESS | `/assign` | USER 본인 |
| IN_PROGRESS | COMPLETED | Post 작성의 `linkedRequestId` | assignee 본인 |
| REQUESTED/IN_PROGRESS | REJECTED | `/reject` | assignee 또는 요청자 |
| REJECTED | REQUESTED | `/reopen` | 요청자 |

`IN_REVIEW` 는 enum에는 남기지만 MVP에서는 별도 `/review` 단계 없이 v1.1에서 도입 검토.

---

## 4. 의존하는 컨트랙트

- `UserService.requireExists(userId)` — assignee 검증, requester 검증
- `UserService.getSystemUserId()` — 시스템 DM 발신자
- `PostService` 가 저장한 postId — `completeByLinkedPost` 호출 시
- `MessageService.send(fromUserId, toUserId, content)` — 상태 변경 알림

---

## 5. 단계별 작업 가이드

### M0 (5/22 ~ 5/25): 코드·문서 정독
- [ ] `request/` 정독, RequestStatus enum 확인
- [ ] DM/User/Post 담당과 합의: 시스템 발신자 ID 보장 시점·방식

### M1 (5/27 ~ 6/3): MVP
- [ ] `create()` — status=REQUESTED, teamId 스냅샷
- [ ] `update()` — status==REQUESTED 일 때만 허용 (그 외 400)
- [ ] `assign()` — USER 본인 수락. 본문 없음. 이미 assignee 있으면 409. status 자동 IN_PROGRESS
- [ ] `completeByLinkedPost()` — Post 작성 트랜잭션에서 호출. assignee 본인 검증, 자동 COMPLETED
- [ ] **상태 변경마다 DM 알림 호출** — `MessageService.send(system, requester, ...)`
- [ ] 한 트랜잭션 안에서 호출: DM 실패 시 상태 전이도 롤백 (학습 효과 위해 일부러 일관 모드)
- [ ] 통합 테스트 데이(6/1) — 한 요청을 처음부터 끝까지

### M2 (6/4 ~ 6/11): SHOULD
- [ ] `reject()` — assignee 또는 요청자만, DM 발송
- [ ] `reopen()` — 요청자만, REJECTED → REQUESTED
- [ ] RequestComment + 대댓글 (Comment 패턴 그대로)
- [ ] 검색·필터 (status, assigneeId, requesterId, teamId)

### M3 (6/12 ~ 6/16)
- [ ] 마감일 임박 알림은 v1.1 — 본 버전은 마감일 표시만
- [ ] 요청 흐름 로그/메트릭 점검 (Infra 협업)

---

## 6. 인수 기준 (AC)

### R-04 상태 전이
- [ ] 표 외 전이 400 `REQUEST_STATUS_TRANSITION_INVALID`
- [ ] 상태 변경 성공 시 DM 1통이 requester에게 (시스템 발신)
- [ ] 트랜잭션: DM 실패 시 상태 변경 롤백

### R-05 배정
- [ ] 본문 없이 호출자 본인이 assignee
- [ ] 이미 assignee 있으면 409 `REQUEST_ASSIGN_TAKEN`
- [ ] 배정 시 status REQUESTED → IN_PROGRESS 자동

### R-09 자동 완료
- [ ] assignee 가 아닌 사람 호출 403
- [ ] postId 미존재 404
- [ ] 성공 시 status=COMPLETED, DM 1통

---

## 7. 충돌 방지 / 함정

| 함정 | 결과 | 회피 |
|---|---|---|
| `Message` 엔티티를 import 해서 직접 저장 | 도메인 경계 깨짐 | `MessageService.send` 만 사용 |
| 상태 전이 검증을 컨트롤러에 흩뿌림 | 일관성 무너짐 | `RequestStatusService` 한 곳에서만 |
| `Post` 엔티티 import 해서 linkedPostId를 직접 다룸 | 책임 경계 깨짐 | linkedPostId 는 `completeByLinkedPost` 안에서만 변경 |
| 시스템 발신자 user 존재 보장 안 됨 | DM 송신 실패 | `AdminBootstrapRunner` 가 시스템 유저 보장 (Infra 협업) |
| DM 실패해도 상태 전이 성공 | 알림 누락 | 같은 트랜잭션에 묶음 (이번 학기 정책) |
