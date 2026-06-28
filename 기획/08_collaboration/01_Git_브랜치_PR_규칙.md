# 01. Git · 브랜치 · PR 규칙

> 비유: **8명이 같은 모래성을 만든다.** 각자 다른 구역에서 모래를 쌓되, 가운데 만나는 부분(공유 코드)은 사전에 합의된 시간에만 살짝 다듬는다. 그렇지 않으면 발자국에 짓밟힌다.
>
> 모르는 단어는 `[../00_overview/01_용어집.md](../00_overview/01_용어집.md)` 의 H장(Git / 협업) 참고.

---

## 브랜치 전략 — main + dev + prod 3단 + 짧은 피처 브랜치

> 8명·한 달짜리 학습 프로젝트라 git-flow 풀세트는 과체중이지만, 안전한 검증을 위해 **3단 브랜치**를 둠.
>
> 비유: **공장 컨베이어 벨트.** feature(생산 라인) → dev(품질 검사) → prod(스트레스 테스트장) → main(출하).

### 흐름

```
feature/<domain>-<주제>/#<이슈번호>     ← 본인 작업 (1~3일 안에 머지)
        │ PR
        ▼
       dev          ← 개발 통합. 모든 feature PR이 여기로 머지됨. 항상 빌드 통과.
        │ PR (안정화 시점)
        ▼
       prod         ← 테스트/QA 환경 브랜치. 베타·정식 배포 전 실제 운영 환경에 띄워서 검증.
        │ PR (검증 OK)
        ▼
       main         ← 실제 배포 대상. TA반 베타(6/9), 정식(6/16) 마일스톤 시점에 prod → main 머지.
```

### 각 브랜치의 의미

| 브랜치 | 의미 | 머지 가능한 출처 | 보호 |
|---|---|---|---|
| `feature/...` | 개인 작업 브랜치. 1~3일 안에 머지. | (출발점: dev) | - |
| `dev` | 개발 통합. 항상 빌드/테스트 통과 유지. | feature/* | PR 필수, CI 그린 |
| `prod` | 테스트 환경 배포 검증용. 운영 프로파일로 띄움. | dev | PR 필수, 추가 검증 |
| `main` | 실제 배포본. 마일스톤 시점에만 머지. | prod | PR 필수, PM 승인 |

> "**DEV 최신화**" = 본인 feature 시작 전·머지 전에 **dev 최신화 → rebase 아닌 merge**로 합쳐와서 충돌 미리 해결. (rebase는 정책상 사용 안 함)

### 브랜치 종류(prefix)

```
feature/<domain>-<주제>/#<이슈>     ← 새 기능
fix/<domain>-<버그>/#<이슈>          ← 버그 픽스
chore/<주제>/#<이슈>                  ← 인프라/문서/설정
docs/<주제>/#<이슈>                   ← 기획 폴더 변경
refactor/<domain>-<주제>/#<이슈>      ← 도메인 내부 리팩터링
```

### 브랜치 이름 규칙 (끝에 GitHub 이슈 번호 붙이기)

```
feature/post-create-multipart/#1
feature/user-login/#5
fix/dm-unread-count/#4
chore/ci-add-coverage/#8
docs/api-update-search/#9
```

도메인 prefix: `user / file / post / request / comment / category / tag / message / infra / common`

> **이슈 번호** = GitHub Issues 의 번호. PR 본문에서 `Closes #5` 처럼 연결하면 머지 시 자동으로 이슈가 닫힙니다.

### 절대 안 하는 것

- `main` / `prod` / `dev` 직 push 금지 (전부 PR 필수)
- 1주일 넘게 묵힌 피처 브랜치 (3일 안에 못 끝낼 일은 더 잘게 쪼개라)
- 한 브랜치에 두 도메인을 동시에 손대기
- **rebase 사용 금지** (충돌 해결도 일반 merge로)

---

## PR 규칙

### PR 사이즈

- **변경 줄 수 < 400 LoC 권장.** 더 크면 잘게 쪼개라.
- 한 PR = 하나의 목적. "리팩터링 + 신기능" 같은 짬뽕 금지.

### PR 제목 + 개별 커밋 메시지 형식 (통일)

PR 제목과 개별 커밋 메시지를 **같은 형식**으로 통일합니다.

```
[FEAT] 게시글 작성 + 파일 첨부 multipart 처리
[FIX] DM 안 읽은 수가 새 메시지 후 갱신 안 되는 문제
[DOCS] API 검색 쿼리 파라미터 추가
[CHORE] CI에 coverage 단계 추가
[REFACTOR] PostService 좋아요 토글 분리
[TEST] Post 작성 인수 기준 테스트 추가
```

형식: `[TYPE] 한국어 한 줄 요약`

타입(대괄호 안, 대문자): **FEAT / FIX / CHORE / DOCS / REFACTOR / TEST / PERF / WIP**

- WIP = Work In Progress, 작업 중 임시 커밋. 머지 전에는 정상 타입으로 정리.

### PR 본문 템플릿

```markdown
## 왜
- (한 줄: 어떤 기능/버그를 위해)

## 어떻게
- 핵심 변경 1
- 핵심 변경 2

## 영향 받는 도메인
- [ ] User
- [ ] File
- [x] Post
- [ ] Request
- [ ] Comment/Category/Search
- [ ] DM
- [ ] Infra


## 테스트
- (어떻게 검증했는지: 단위 테스트 / 통합테스트=> 해당 API 모두) + 슬라이스 테스트는 자율

## 체크리스트
- [ ] `./gradlew clean compileJava test` 통과
- [ ] 응답이 `ApiResponse<T>` 래핑 (Response 형식 응답 통일)
- [ ] API 변경 있으면 `04_api/` 동기 수정

## ERD/API 변경
- [ ] 변경 없음
- [ ] 있음 → `기획/04_api/` 또는 `03_erd/` 도 같은 PR에서 수정

## 기타 사항 기입
```

### 리뷰어 지정


| 변경 영역                                 | 필수 리뷰어            |
| ------------------------------------- | ----------------- |
| 본인 도메인 코드만                            | 같은 페어 (있다면) + PM  |
| `common.config.SecurityConfig`        | User + Infra + PM |
| `common.config.WebSocketConfig`       | DM + Infra        |
| `common.exception`                    | 각 도메인별 + PM       |
| `04_api/` 또는 `03_erd/` 변경             | 영향 도메인 전부 + PM    |
| 엔티티 신규/변경                             | Infra + 영향 도메인    |
| build.gradle / Dockerfile / CI / yaml | Infra             |


> 리뷰어 지정은 PR 작성자가 위 표를 보고 수동으로 추가. PM 이 매일 스탠드업에서 누락 점검.

### 리뷰 규칙

- 평일 PR 등록 후 24시간 내 1차 리뷰
- 못 받았다면 PM에 핑 (`#assetbox-help`)
- 머지는 **승인 1개 + CI 그린(Approved)**

### 머지 방식

- **일반 Merge (no-fast-forward) 만 사용.** PR 의 커밋을 그대로 살린 채 머지 커밋을 추가로 만든다. 누가 무엇을 했는지 추적 가능.
- **Rebase 사용 금지.** 충돌 해결도 일반 merge 로.
- **Squash merge 사용 금지.** 커밋이 압축돼서 디버깅·되돌리기 어려워짐.
- 머지 커밋 메시지 = PR 제목과 동일 (`[FEAT] ...`)
- **브랜치는 머지 후 삭제하지 않고 유지.** 회고 / 재참조용.

> 비유: **일반 Merge** = 두 강이 만나서 합류 지점(머지 커밋)을 표시하고 같이 흐름. **Rebase** = 한 강의 물을 다른 강 위로 옮겨 한 줄기로 만듦. 우리는 합류 지점 표시 방식.

---

## "충돌"을 줄이는 6가지 룰

1. **동일 파일을 두 명이 동시에 수정하지 않는다.** 그 파일이 공유 자원(`SecurityConfig`, `build.gradle`, `application.yml`)이면 변경 전에 슬랙으로 선언.
2. **컨트랙트(04_api, 03_erd)는 코드보다 먼저 머지.** 도메인 PR 이 머지된 후 코드가 따라온다.
3. **작게 자주 머지.** 일주일짜리 PR 보다 하루짜리 PR 5개가 안전.
4. **공유 자원 변경 = 그날의 첫 작업.** dev 최신화 → 변경 → 푸시 → PR → 리뷰 빨리 받기.
5. **DEV 최신화를 매일 첫 작업으로.** `git fetch origin && git merge origin/dev` (rebase 아님). 자기 feature 브랜치도 매일 dev 와 합쳐서 충돌 미리 해결.
6. **막히면 30분 안에 슬랙 핑.** 혼자 끙끙대지 말 것. `#assetbox-help`.

---

## CI 게이트 (= GitHub Actions)

PR 이 머지되려면 CI 에서 다음이 모두 통과:

- `./gradlew clean compileJava` — 컴파일
- `./gradlew test` — 테스트 (M1 점진 적용, M2 부터 필수)
- (선택) Docker 이미지 빌드 (M2 부터)
- (선택) 정적 분석 / 포맷 검사

CI 실패한 PR 은 머지 불가 (브랜치 보호 룰).

> 단계별 CI 강도:
> - `feature → dev`: 컴파일 + 테스트 통과 필수
> - `dev → prod`: + Docker 이미지 빌드 + prod 프로파일 부팅 확인
> - `prod → main`: + 마일스톤 시점 PM 승인