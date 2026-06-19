# 코드 생성 (buf generate / buf.gen.yaml)

> 이 문서는 buf의 코드 생성(buf.gen.yaml v2, 플러그인, managed mode, inputs)을 한국어로 정리한 것입니다.
> 원본: https://buf.build/docs/configuration/v2/buf-gen-yaml/ , https://buf.build/docs/generate/tutorial/ , https://buf.build/docs/generate/managed-mode/

---

## 목차

1. [buf generate 개요](#1-buf-generate-개요)
2. [buf.gen.yaml v2 전체 필드](#2-bufgenyaml-v2-전체-필드)
3. [플러그인 설정 (local / remote / protoc_builtin)](#3-플러그인-설정-local--remote--protoc_builtin)
4. [inputs 설정](#4-inputs-설정)
5. [managed mode](#5-managed-mode)
6. [실전 예제](#6-실전-예제)
7. [기본 워크플로](#7-기본-워크플로)

---

## 1. buf generate 개요

`buf generate`는 `buf.gen.yaml`에 정의된 플러그인을 실행해 코드 스텁(stub)을 생성합니다. 입력은 기본적으로 현재 디렉터리이며, `buf.gen.yaml`의 `inputs` 또는 명령행 인자로 지정할 수 있습니다.

```bash
buf generate                       # buf.gen.yaml + 현재 디렉터리(또는 inputs)
buf generate proto                 # 특정 디렉터리를 입력으로
buf generate --template buf.gen.yaml
buf generate buf.build/acme/weather  # BSR 모듈을 입력으로
```

---

## 2. buf.gen.yaml v2 전체 필드

| 필드 | 필수 | 설명 |
|------|------|------|
| `version` | 필수 | `v2` |
| `plugins` | 필수 | 코드 생성기 목록 |
| `managed` | 선택 | managed mode 설정(아래 5절) |
| `inputs` | 선택 | 코드 생성 입력 소스 목록 |
| `clean` | 선택 | `true`면 생성 전에 출력 디렉터리를 삭제 |

---

## 3. 플러그인 설정 (local / remote / protoc_builtin)

각 `plugins` 항목은 플러그인 타입 한 가지를 지정합니다.

### 플러그인 타입(택1)

- **`remote`**: BSR 원격 플러그인. 형식 `buf.build/<owner>/<name>:<version>`. 로컬에 플러그인 바이너리 설치 불필요.
- **`local`**: 로컬 플러그인 바이너리. 바이너리 이름(`$PATH` 탐색), 상대/절대 경로, 또는 `[바이너리, 인자...]` 배열.
- **`protoc_builtin`**: protoc 내장 생성기(cpp, csharp, java, python, ruby 등). `protoc_path`로 protoc 경로 지정 필요.

### 공통 키

| 키 | 설명 |
|----|------|
| `out` | (필수) 생성 파일 출력 디렉터리 |
| `opt` | 플러그인 옵션(문자열 또는 목록). 예: `paths=source_relative` |
| `strategy` | `directory`(기본, 디렉터리별 병렬) 또는 `all`(전체 한 번에) |
| `include_imports` | import도 함께 생성(WKT 제외) |
| `include_wkt` | Well-Known Types도 생성(`include_imports` 필요) |
| `types` / `exclude_types` | 생성할/제외할 타입을 fully-qualified 이름으로 제한 |

---

## 4. inputs 설정

`inputs` 배열은 다양한 소스 타입을 지원합니다.

| 타입 | 설명 / 주요 하위 키 |
|------|---------------------|
| `directory` | 로컬 디렉터리 경로 |
| `module` | BSR 모듈 `buf.build/owner/name[:label]`. `types`/`paths`/`exclude_*` |
| `git_repo` | Git 저장소. `branch`/`tag`/`ref`/`subdir`/`depth`/`recurse_submodules` |
| `tarball` | tarball(로컬/http/https/ssh). `compression`/`strip_components`/`subdir` |
| `zip_archive` | zip 아카이브. `strip_components`/`subdir` |
| `proto_file` | 단일 `.proto` 파일. `include_package_files` |
| `binary_image` / `json_image` / `txt_image` / `yaml_image` | buf 이미지 형식 |

---

## 5. managed mode

managed mode는 `go_package`, `java_package` 같은 언어별 파일 옵션을 `.proto`에 직접 넣지 않고 `buf.gen.yaml`에서 생성 시점에 적용하는 기능입니다. `.proto`를 언어 중립적으로 유지하고, 소비자마다 다른 네임스페이스로 코드를 생성할 수 있습니다.

### 활성화

```yaml
version: v2
managed:
  enabled: true
plugins:
  - remote: buf.build/protocolbuffers/go:v1.34.2
    out: gen/go
```

### 관리되는 주요 옵션

- Go: `go_package`, `go_package_prefix`
- Java: `java_package`, `java_package_prefix/suffix`, `java_multiple_files`, `java_outer_classname`
- C#: `csharp_namespace`, `csharp_namespace_prefix`
- 기타: PHP, Ruby, Swift, Objective-C 옵션과 `optimize_for`, `jstype`(필드 옵션)

### override / disable

`override`는 특정 module/path/file/field에 대해 값을 덮어쓰며, **나중에 매칭된 규칙이 우선**합니다. `disable`은 managed mode 적용 자체를 막습니다(외부 의존성에 흔히 사용). 둘 다 매칭되면 `disable`이 우선합니다.

```yaml
managed:
  enabled: true
  override:
    - file_option: go_package_prefix
      value: github.com/myorg/api
    - file_option: java_package_prefix
      value: com
    - file_option: java_multiple_files
      value: "true"
    - file_option: go_package_prefix
      path: internal/
      value: github.com/myorg/api/internal
  disable:
    - module: buf.build/googleapis/googleapis
```

---

## 6. 실전 예제

원격 플러그인 + managed mode를 사용하는 현실적인 `buf.gen.yaml`입니다.

```yaml
version: v2
clean: true

managed:
  enabled: true
  override:
    - file_option: go_package_prefix
      value: github.com/myorg/api/gen/go
  disable:
    - module: buf.build/googleapis/googleapis

plugins:
  # protobuf Go 메시지 생성 (원격 플러그인)
  - remote: buf.build/protocolbuffers/go:v1.34.2
    out: gen/go
    opt: paths=source_relative

  # gRPC Go 서비스 스텁 생성 (원격 플러그인)
  - remote: buf.build/grpc/go:v1.4.0
    out: gen/go
    opt: paths=source_relative

inputs:
  - directory: proto
  - module: buf.build/googleapis/googleapis
```

위 설정으로 `buf generate`를 실행하면 `gen/go/`에 메시지와 gRPC 스텁이 생성됩니다.

---

## 7. 기본 워크플로

공식 튜토리얼 기준의 전형적인 흐름입니다.

```bash
# 1) 프로젝트 초기화
mkdir buf-codegen-quickstart && cd buf-codegen-quickstart
buf config init   # buf.yaml 생성

# buf.yaml (v2)
#   version: v2
#   modules:
#     - path: proto

# 2) proto 작성: proto/acme/weather/v1/weather.proto

# 3) buf.gen.yaml 작성 후 코드 생성
buf generate
```

로컬 플러그인으로 시작했다가 원격 플러그인으로 바꾸려면 `local: protoc-gen-go`를 `remote: buf.build/protocolbuffers/go:v1.34.2`로 교체하면 됩니다. 로컬 플러그인을 쓸 경우 해당 바이너리를 미리 설치합니다.

```bash
go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
export PATH="$PATH:$(go env GOPATH)/bin"
```
