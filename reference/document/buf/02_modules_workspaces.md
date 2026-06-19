# 모듈과 워크스페이스 (buf.yaml)

> 이 문서는 buf의 모듈/워크스페이스 개념과 buf.yaml(v1·v2) 설정, 의존성·buf.lock을 한국어로 정리한 것입니다.
> 원본: https://buf.build/docs/cli/modules-workspaces/ , https://buf.build/docs/configuration/v2/buf-yaml/ , https://buf.build/docs/configuration/v1/buf-yaml/

---

## 목차

1. [모듈(Module)이란](#1-모듈module이란)
2. [워크스페이스(Workspace)란](#2-워크스페이스workspace란)
3. [buf.yaml v2 전체 필드](#3-bufyaml-v2-전체-필드)
4. [buf.yaml v1 전체 필드](#4-bufyaml-v1-전체-필드)
5. [v1과 v2의 차이](#5-v1과-v2의-차이)
6. [의존성(deps)과 import 해석](#6-의존성deps과-import-해석)
7. [buf.lock 파일](#7-buflock-파일)

---

## 1. 모듈(Module)이란

모듈은 buf가 하나의 단위로 다루는 `.proto` 파일들의 디렉터리 트리입니다. 빌드/버전/게시/의존의 대상이 되는 기본 단위이며, BSR의 한 저장소(repository)에 대응합니다.

모듈 내부에서 import는 항상 **모듈 루트(root) 기준**으로 해석됩니다. 즉 `proto/acme/weather/v1/api.proto` 파일은 import 시 `acme/weather/v1/api.proto`로 참조됩니다(파일 위치 기준이 아님).

---

## 2. 워크스페이스(Workspace)란

워크스페이스는 하나 이상의 모듈을 함께 설정하는 단위입니다. 워크스페이스 수준에서 다음을 공유합니다.

- 공통 lint / breaking 기본 규칙
- 외부 BSR 의존성(deps)
- 같은 워크스페이스 내 모듈 간 상호 import (별도 의존성 선언 불필요)

---

## 3. buf.yaml v2 전체 필드

v2의 `buf.yaml`은 하나 이상의 모듈을 담는 워크스페이스를 정의합니다.

| 필드 | 필수 | 설명 |
|------|------|------|
| `version` | 필수 | `v2` |
| `modules` | 필수 | 워크스페이스를 구성하는 모듈 목록 |
| `deps` | 선택 | BSR 외부 모듈 의존성(워크스페이스 전체에서 공유) |
| `lint` | 선택 | 모듈별 오버라이드가 없을 때 적용되는 기본 lint 규칙 |
| `breaking` | 선택 | 기본 breaking 규칙 |
| `plugins` | 선택 | 커스텀 lint/breaking 규칙 플러그인 |
| `policies` | 선택 | 워크스페이스 간 공유 가능한 재사용 규칙 세트 |

### modules 항목 구조

| 키 | 필수 | 설명 |
|----|------|------|
| `path` | 필수 | `buf.yaml` 기준 상대 디렉터리 |
| `name` | 선택 | 이 디렉터리를 식별하는 BSR 경로(커밋/라벨 이력 추적) |
| `includes` | 선택 | 탐색을 특정 하위 디렉터리로 제한 |
| `excludes` | 선택 | 탐색에서 제외할 하위 디렉터리(included 범위 안에 있어야 함) |
| `lint` | 선택 | 모듈별 lint 오버라이드 — 워크스페이스 설정을 **완전히 대체**(병합 안 함) |
| `breaking` | 선택 | 모듈별 breaking 오버라이드(lint과 동일하게 완전 대체) |

### v2 예제

```yaml
version: v2

modules:
  - path: proto/api
    name: buf.build/company/api
    lint:
      use:
        - STANDARD
      except:
        - IMPORT_USED
    breaking:
      use:
        - WIRE
  - path: proto/internal
    name: buf.build/company/internal
    excludes:
      - proto/internal/test

deps:
  - buf.build/googleapis/googleapis

lint:
  use:
    - STANDARD
  enum_zero_value_suffix: _UNSPECIFIED
  service_suffix: Service

breaking:
  use:
    - FILE
  ignore:
    - proto/internal/v1alpha1
```

---

## 4. buf.yaml v1 전체 필드

v1의 `buf.yaml`은 **단일 모듈**을 정의합니다.

| 필드 | 필수 | 설명 |
|------|------|------|
| `version` | 필수 | `v1` (또는 `v1beta1`) |
| `name` | 선택 | 모듈을 유일하게 식별하는 BSR 모듈명. 예: `buf.build/acme/petapis` |
| `deps` | 선택 | 모듈 의존성 목록(커밋/태그 지정 가능) |
| `build` | 선택 | `excludes`로 `.proto` 탐색에서 제외할 디렉터리 지정 |
| `lint` | 선택 | lint 규칙(use/except/ignore/ignore_only 등) |
| `breaking` | 선택 | breaking 규칙(use/except/ignore/ignore_unstable_packages 등) |

### v1 예제

```yaml
version: v1
name: buf.build/acme/payment-apis

deps:
  - buf.build/googleapis/googleapis:v1beta1.1.0
  - buf.build/acme/common-types

build:
  excludes:
    - third_party

lint:
  use:
    - STANDARD
  except:
    - ENUM_NO_ALLOW_ALIAS
  ignore:
    - internal/legacy.proto
  ignore_only:
    FILE_LOWER_SNAKE_CASE:
      - proto/v1alpha1
  enum_zero_value_suffix: _UNSPECIFIED
  service_suffix: Service

breaking:
  use:
    - FILE
  ignore:
    - proto/v1alpha1
  ignore_unstable_packages: true
```

v1에서 멀티 모듈 워크스페이스를 쓰려면 별도의 `buf.work.yaml`로 모듈 경로를 나열합니다.

```yaml
# buf.work.yaml (v1 워크스페이스)
version: v1
directories:
  - proto
  - vendor
```

---

## 5. v1과 v2의 차이

| 항목 | v1 | v2 |
|------|----|----|
| 모듈 정의 | `buf.yaml` 당 단일 모듈 | `modules`로 여러 모듈 지원 |
| 워크스페이스 | 별도 `buf.work.yaml` 필요 | `buf.yaml`의 `modules`로 통합 |
| 모듈별 lint/breaking 오버라이드 | 없음 | 있음(완전 대체) |
| 같은 path 멀티 모듈 | 불가 | includes/excludes로 가능 |
| 주석 무시(comment ignores) | `allow_comment_ignores: true` 필요 | 기본 활성(`disallow_comment_ignores: false`) |

기존 v1 설정은 `buf config migrate`로 v2로 변환할 수 있습니다.

---

## 6. 의존성(deps)과 import 해석

buf는 import를 두 단계로 해석합니다.

1. **로컬 해석**: 현재 워크스페이스의 모든 모듈에서 먼저 찾습니다. 같은 워크스페이스의 모듈은 별도 의존성 선언 없이 서로 import할 수 있습니다.
2. **외부 해석**: 로컬에서 못 찾으면 `deps`에 선언된 BSR 모듈을 다운로드합니다.

`deps` 항목은 커밋/라벨을 함께 지정할 수 있습니다.

```yaml
deps:
  - buf.build/googleapis/googleapis              # 최신 기본 라벨
  - buf.build/acme/pkg:47b927cbb41c4fdea1292ba   # 특정 커밋 고정
  - buf.build/googleapis/googleapis:v1beta1.1.0  # 라벨 지정
```

선언 후 `buf dep update`로 의존성을 해석해 `buf.lock`에 기록합니다.

---

## 7. buf.lock 파일

`buf.lock`은 외부 BSR 의존성을 특정 커밋으로 고정하여 재현 가능한 빌드를 보장하는 잠금 파일입니다. `buf dep update`가 자동 생성/갱신하며 직접 편집하지 않습니다.

```bash
buf dep update   # deps를 해석해 buf.lock 갱신
buf dep graph    # 의존성 그래프 출력
buf dep prune    # 사용하지 않는 의존성 정리 (v2)
```
