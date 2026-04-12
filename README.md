# Commit Convention
Commit 작성 시 아래 항목을 기준으로 작성
## Commit Text
```text
type: 메시지

ex) init: 프로젝트 설정
```

## Commit Type

| 커밋 유형      | 의미                             |
|------------|--------------------------------|
| `feat`     | 새로운 기능 추가, 기존 기능을 요구 사항에 맞게 수정 |
| `fix`      | 버그 수정                          |
| `build`    | 빌드 관련 수정, 의존성 추가/삭제, 배포 설정 변경  |
| `refactor` | 기능 변화 없는 코드 구조 개선              |
| `chore`    | 기타 자잘한 작업, 설정 파일 수정            |
| `test`     | 테스트 코드 추가 또는 수정                |
| `docs`     | 문서 수정                          |
| `init`     | 프로젝트 생성                        |

# Pull Request Convention
PR 작성 시 `pull_request_template.md` 템플릿을 기준으로 작성
## Pull Request Title
```text
[#티켓 번호] 제목 형식으로 명확하고 간결하게 작성

ex) [PAD-1] 프로젝트 설정 
```

## Pull Request Template

```text
## 티켓 번호
ticket: #PAD-1

## 작업 내용

## 참고 사항 (선택)
```
