## 🔗 관련 이슈
- close #

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
