# 린팅 (buf lint)

> 이 문서는 buf lint의 사용법, 규칙 카테고리, 주요 규칙, 무시(ignore) 설정을 한국어로 정리한 것입니다.
> 원본: https://buf.build/docs/lint/ , https://buf.build/docs/lint/rules/

---

## 목차

1. [buf lint 개요](#1-buf-lint-개요)
2. [규칙 카테고리와 계층](#2-규칙-카테고리와-계층)
3. [카테고리별 주요 규칙](#3-카테고리별-주요-규칙)
4. [설정 (use / except / ignore / ignore_only)](#4-설정-use--except--ignore--ignore_only)
5. [주석 무시 (comment ignores)](#5-주석-무시-comment-ignores)
6. [규칙 목록 확인](#6-규칙-목록-확인)

---

## 1. buf lint 개요

`buf lint`는 `.proto` 파일을 스타일/구조 규칙에 따라 검사합니다. 설정은 `buf.yaml`의 `lint` 섹션에서 읽으며, 별도 설정이 없으면 기본값으로 `STANDARD` 규칙 세트를 적용합니다.

```bash
buf lint                 # 현재 워크스페이스 검사
buf lint proto           # 특정 디렉터리 검사
buf lint --error-format=json
```

위반이 있으면 위반 위치와 규칙 ID를 출력하고 0이 아닌 종료 코드를 반환합니다(CI에서 활용).

---

## 2. 규칙 카테고리와 계층

buf 린트 규칙은 5개 카테고리로 묶이며, 그중 3개는 엄격도(strictness) 계층을 이룹니다(상위가 하위를 포함).

- **MINIMAL**: 가장 기본적인 구조 요구. (패키지 선언, 파일-패키지 경로 일치, import 순환 금지, 디렉터리당 단일 패키지)
- **BASIC**: MINIMAL + 커뮤니티 표준 스타일. (PascalCase 메시지/enum, lower_snake_case 필드/패키지, UPPER_SNAKE_CASE enum 값, public import 금지)
- **STANDARD**(기본값): BASIC + buf 권장 사항. (패키지 버전 접미사, 서비스 명명 규칙, RPC 요청/응답 패턴, Protovalidate 검증, enum zero-value 명명)

독립 카테고리(계층에 속하지 않음):

- **COMMENTS**: enum/message/field/oneof/service/RPC/enum 값에 문서 주석 요구.
- **UNARY_RPC**: 스트리밍 RPC 금지(Twirp 등 스트리밍 미지원 전송에 유용).

---

## 3. 카테고리별 주요 규칙

### MINIMAL

| 규칙 ID | 검사 내용 |
|---------|-----------|
| `PACKAGE_DEFINED` | 모든 파일에 package 선언 필요 |
| `PACKAGE_DIRECTORY_MATCH` | 파일 경로와 패키지명 일치 |
| `DIRECTORY_SAME_PACKAGE` | 한 디렉터리의 파일은 동일 패키지 |
| `PACKAGE_SAME_DIRECTORY` | 디렉터리당 단일 패키지 |
| `PACKAGE_NO_IMPORT_CYCLE` | 패키지 간 순환 import 금지 (v2 전용) |

### BASIC (MINIMAL 포함)

| 규칙 ID | 검사 내용 |
|---------|-----------|
| `ENUM_PASCAL_CASE` | enum 이름 PascalCase |
| `ENUM_VALUE_UPPER_SNAKE_CASE` | enum 값 UPPER_SNAKE_CASE |
| `ENUM_FIRST_VALUE_ZERO` | 첫 enum 값은 0 |
| `ENUM_NO_ALLOW_ALIAS` | enum alias 금지 |
| `MESSAGE_PASCAL_CASE` | 메시지 이름 PascalCase |
| `FIELD_LOWER_SNAKE_CASE` | 필드 이름 lower_snake_case |
| `FIELD_NOT_REQUIRED` | `required` 라벨 금지 |
| `ONEOF_LOWER_SNAKE_CASE` | oneof 이름 lower_snake_case |
| `PACKAGE_LOWER_SNAKE_CASE` | 패키지 이름 lower_snake_case |
| `SERVICE_PASCAL_CASE` / `RPC_PASCAL_CASE` | 서비스/RPC PascalCase |
| `IMPORT_NO_PUBLIC` / `IMPORT_USED` | public import 금지 / import는 반드시 사용 |
| `SYNTAX_SPECIFIED` | syntax 선언 필수 |

### STANDARD (BASIC 포함)

| 규칙 ID | 검사 내용 |
|---------|-----------|
| `PACKAGE_VERSION_SUFFIX` | 패키지는 버전 접미사로 끝남(v1, v2, v1alpha 등) |
| `ENUM_ZERO_VALUE_SUFFIX` | enum 0 값은 `_UNSPECIFIED`로 끝남 |
| `ENUM_VALUE_PREFIX` | enum 값은 enum 이름을 접두사로 |
| `FILE_LOWER_SNAKE_CASE` | 파일명 lower_snake_case |
| `SERVICE_SUFFIX` | 서비스 이름은 `Service`로 끝남 |
| `RPC_REQUEST_STANDARD_NAME` | 요청 메시지는 `*Request` |
| `RPC_RESPONSE_STANDARD_NAME` | 응답 메시지는 `*Response` |
| `RPC_REQUEST_RESPONSE_UNIQUE` | RPC마다 요청/응답 메시지 고유 |
| `PROTOVALIDATE` | Protovalidate 제약 구문 검증 |

### COMMENTS / UNARY_RPC

- COMMENTS: `COMMENT_ENUM`, `COMMENT_MESSAGE`, `COMMENT_FIELD`, `COMMENT_SERVICE`, `COMMENT_RPC`, `COMMENT_ONEOF`, `COMMENT_ENUM_VALUE`
- UNARY_RPC: `RPC_NO_CLIENT_STREAMING`, `RPC_NO_SERVER_STREAMING`

> 그 외 `STABLE_PACKAGE_NO_IMPORT_UNSTABLE`(안정 패키지가 alpha/beta를 import 금지)는 명시적으로 활성화해야 합니다.

---

## 4. 설정 (use / except / ignore / ignore_only)

| 옵션 | 설명 |
|------|------|
| `use` | 적용할 규칙/카테고리(기본 `STANDARD`) |
| `except` | `use`에서 제외할 규칙 |
| `ignore` | 모든 규칙에서 제외할 파일/디렉터리 |
| `ignore_only` | 규칙별로 특정 파일/디렉터리만 제외 |
| `enum_zero_value_suffix` | 요구하는 enum 0값 접미사(기본 `_UNSPECIFIED`) |
| `service_suffix` | 요구하는 서비스 접미사(기본 `Service`) |

```yaml
version: v2
lint:
  use:
    - STANDARD
    - COMMENTS          # 문서 주석도 강제
  except:
    - COMMENT_ONEOF     # 그중 oneof 주석은 예외
  ignore:
    - proto/legacy      # 디렉터리 전체 무시
  ignore_only:
    PACKAGE_VERSION_SUFFIX:
      - proto/internal  # 이 규칙만 특정 경로에서 무시
  enum_zero_value_suffix: _UNSPECIFIED
  service_suffix: Service
```

v2에서 모듈별로 다른 규칙을 적용하려면 `modules[].lint`에 작성하며, 이는 워크스페이스 설정을 완전히 대체합니다.

---

## 5. 주석 무시 (comment ignores)

특정 위치에서만 규칙을 끄려면 `.proto` 안에 주석을 둡니다. v2에서는 기본 활성화되어 있습니다(v1은 `allow_comment_ignores: true` 필요).

```proto
// buf:lint:ignore SERVICE_SUFFIX
service Weather {
  // buf:lint:ignore RPC_REQUEST_STANDARD_NAME
  rpc Get(GetInput) returns (GetOutput);
}
```

---

## 6. 규칙 목록 확인

현재 설정 또는 카테고리에 포함된 규칙을 확인할 수 있습니다.

```bash
buf config ls-lint-rules                 # 현재 설정 기준 활성 규칙
buf config ls-lint-rules --version v2    # v2 전체 규칙
buf config ls-lint-rules --configured    # buf.yaml에 설정된 규칙
```
