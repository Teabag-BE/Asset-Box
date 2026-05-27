# 파트 가이드 — File-B (v1.1 보관)

> ⚠️ **2026-05-22 회의 반영**
>
> 본 MVP에서는 File-A/File-B 분리가 폐기되고 **File 통합 도메인**으로 운영한다. 현재 유효한 가이드는 [`02_File-A_저장.md`](./02_File-A_저장.md) 이다.

---

## 현재 상태

| 항목 | 결정 |
|---|---|
| 담당 방식 | File 2명이 통합 도메인을 페어로 담당 |
| 유효 API | `GET /api/files/{fileId}`, `GET /api/files/{fileId}/meta`, `DELETE /api/files/{fileId}` |
| 업로드 | Post/User/Request API에서 multipart로 받고 FileService에 위임 |
| 제외 | DownloadLog, top-files, 파일 통계, `/api/admin/download-logs/**` |
| 재검토 시점 | v1.1 또는 Analytics/Audit 도메인 분리 시 |

---

## v1.1 후보 메모

아래 기능은 구현하지 말고 백로그로만 둔다.

- 다운로드 로그 적재
- 파일별 다운로드 수
- 기간별 top-files
- IP/User-Agent 기반 감사 로그
- 비동기 적재용 `@Async` 튜닝

MVP 문서나 API 명세에 위 기능을 다시 추가하면 `04_api/02_엔드포인트_목록_상태코드분리_수정본.md` 와 충돌한다.
