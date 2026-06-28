# 10. API 명세서 — Message (DM)

> 담당 파트: **DM**
> 베이스 경로: `/api/messages/**`
> WebSocket: `/ws` 엔드포인트 + STOMP 토픽

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| M-1 | POST | `/api/messages` | USER | 메시지 송신 |
| M-2 | GET | `/api/messages/inbox` | USER | 인박스 (대화 상대별 최신) |
| M-3 | GET | `/api/messages/conversation/{partnerId}` | USER | 특정 상대와 대화 (DESC) |
| M-4 | GET | `/api/messages/unread` | USER | 안 읽은 수 |
| M-5 | (WS) | `/user/queue/messages` | USER | 수신 푸시 (구독만) |
| M-6 | (WS) | `/user/queue/messages.unread` | USER | 안 읽은 수 변동 푸시 |

> M2부터 WebSocket 도입. M1까지는 REST 폴링으로 충분.
> 어드민의 DM 강제 열람(`/api/admin/messages/**`)은 분쟁 처리가 우리 책임 영역이 아니므로 본 프로젝트에서 제외한다.

---

## M-1. POST `/api/messages`

**설명**: 메시지 송신. 트랜잭션 안에서 DB 저장 + 트랜잭션 커밋 후 WS 푸시.
**인증**: USER

### 요청

```http
POST /api/messages
Content-Type: application/json
Authorization: Bearer <jwt>
```

```json
{
  "toUserId": 18,
  "content": "의자 요청 받아서 작업 시작합니다."
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| toUserId | long | NotNull, 자기 자신 불가 |
| content | string | NotBlank, max 2000자 |

### 응답 201

```json
{
  "success": true,
  "data": {
    "id": 5001,
    "senderId": 12,
    "senderNickname": "김TA",
    "receiverId": 18,
    "receiverNickname": "박TA",
    "content": "의자 요청 받아서 작업 시작합니다.",
    "read": false,
    "createdAt": "2026-05-23T11:00:00"
  }
}
```

### 부수효과

- DB에 `messages` 1행 INSERT (read=false)
- 트랜잭션 커밋 후(`TransactionSynchronization.afterCommit`):
  - 수신자가 WS 구독 중이면 `/user/queue/messages` 로 푸시
  - 수신자의 unread 카운트 변동 → `/user/queue/messages.unread` 푸시

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | toUserId 누락 / content 누락 |
| 400 | `MESSAGE_SELF_NOT_ALLOWED` | toUserId == 본인 |
| 400 | `MESSAGE_CONTENT_TOO_LONG` | 2000자 초과 |
| 401 | `UNAUTHORIZED` | |
| 404 | `USER_NOT_FOUND` | toUserId 미존재 |
| 403 | `MESSAGE_RECEIVER_BLOCKED` | (v1.1) 차단 관계 |

---

## M-2. GET `/api/messages/inbox`

**설명**: 대화 상대별 최신 메시지 1건 요약 목록. 마지막 메시지 시각 DESC.
**인증**: USER

### 요청

```http
GET /api/messages/inbox?page=0&size=20
Authorization: Bearer <jwt>
```

### 응답 200

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "partnerId": 18,
        "partnerNickname": "박TA",
        "lastMessage": "의자 요청 받아서 작업 시작합니다.",
        "lastMessageAt": "2026-05-23T11:00:00",
        "unreadCount": 1
      },
      {
        "partnerId": 99,
        "partnerNickname": "SYSTEM",
        "lastMessage": "[요청 #11] 박TA에게 배정되었습니다.",
        "lastMessageAt": "2026-05-22T15:00:00",
        "unreadCount": 0
      }
    ],
    "page": 0, "size": 20, "totalElements": 2, "totalPages": 1,
    "first": true, "last": true
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 400 | `PAGINATION_SIZE_TOO_LARGE` | |

---

## M-3. GET `/api/messages/conversation/{partnerId}`

**설명**: 특정 상대와의 대화 전체. 시간 DESC. 진입 시점에 본 엔드포인트 호출과 함께 unread → read 처리 가능 (또는 별도 markRead 정책 — 본 명세는 **조회 시 자동 read 처리**).
**인증**: USER

### 요청

```http
GET /api/messages/conversation/18?page=0&size=20
Authorization: Bearer <jwt>
```

### 응답 200

```json
{
  "success": true,
  "data": {
    "partnerId": 18,
    "partnerNickname": "박TA",
    "messages": {
      "items": [
        {
          "id": 5001,
          "senderId": 12,
          "senderNickname": "김TA",
          "receiverId": 18,
          "receiverNickname": "박TA",
          "content": "의자 요청 받아서 작업 시작합니다.",
          "read": true,
          "createdAt": "2026-05-23T11:00:00"
        },
        {
          "id": 4988,
          "senderId": 18,
          "senderNickname": "박TA",
          "receiverId": 12,
          "receiverNickname": "김TA",
          "content": "안녕하세요!",
          "read": true,
          "createdAt": "2026-05-22T16:30:00"
        }
      ],
      "page": 0, "size": 20, "totalElements": 43, "totalPages": 3,
      "first": true, "last": false
    }
  }
}
```

### 부수효과

- 본인이 수신자(receiver=me) 인 메시지들의 `read` 를 true 로 업데이트
- `/user/queue/messages.unread` 푸시 (감소)

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 404 | `USER_NOT_FOUND` | partnerId 미존재 |
| 400 | `MESSAGE_SELF_NOT_ALLOWED` | partnerId == 본인 |

---

## M-4. GET `/api/messages/unread`

**설명**: 헤더 뱃지용 안 읽은 메시지 수.
**인증**: USER

### 요청

```http
GET /api/messages/unread
Authorization: Bearer <jwt>
```

### 응답 200

```json
{
  "success": true,
  "data": { "count": 3 }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |

---

## M-5. (WS) `/user/queue/messages` — 구독

**설명**: 메시지 수신 실시간 푸시. 클라이언트는 구독만, 발행 X.

### 연결

```
ws://host/ws
STOMP CONNECT
  accept-version: 1.2
  host: host
  Authorization: Bearer <jwt>
```

### 구독

```
SUBSCRIBE
  id: sub-0
  destination: /user/queue/messages
```

### 푸시되는 메시지 형태

```json
{
  "id": 5001,
  "senderId": 12,
  "senderNickname": "김TA",
  "receiverId": 18,
  "receiverNickname": "박TA",
  "content": "...",
  "read": false,
  "createdAt": "2026-05-23T11:00:00"
}
```

### 에러

| 상황 | 결과 |
|---|---|
| JWT 누락 / 만료 | CONNECT 거부 (ERROR 프레임) |
| 다른 사용자의 큐 SUBSCRIBE 시도 | 무시 (Spring의 user-destination 라우팅) |
| 클라이언트가 발행 시도 (`SEND /app/...`) | 무시 (서버는 발행 채널 미공개) |

---

## M-6. (WS) `/user/queue/messages.unread` — 구독

**설명**: 안 읽은 수 변동 시 푸시. 메시지 수신·읽음 처리 양쪽에서 발생.

### 푸시 형태

```json
{ "count": 4 }
```

수신자가 페이지를 켜둔 상태에서 자동으로 헤더 뱃지가 갱신되도록.

## 부록 — 시스템 발신자

요청 게시판의 상태 변경 알림(`Request → DM`)은 **시스템 유저**가 발신합니다.

- 시스템 유저는 `AdminBootstrapRunner` 가 부팅 시 보장 (`email=system@assetbox.local`, role=ADMIN, nickname="SYSTEM")
- `UserService.getSystemUserId()` 가 그 id 반환
- 시스템 메시지의 content 는 다음 패턴 권장:
  - `[요청 #{id}] {assigneeNickname}님이 요청을 수락했습니다.`
  - `[요청 #{id}] 요청이 반려되었습니다.`
  - `[요청 #{id}] 요청이 다시 열렸습니다.`
  - `[요청 #{id}] 완료되었습니다 → /posts/{linkedPostId}`

수신자(요청자)는 다른 일반 메시지와 동일하게 인박스에서 봄.
