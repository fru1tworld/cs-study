# 모듈 레퍼런스

# go.mod 파일 레퍼런스

> **원문:** https://go.dev/doc/modules/gomod-ref

## 개요

각 Go 모듈은 모듈의 속성(다른 모듈과 Go 버전에 대한 의존성 포함)을 설명하는 `go.mod` 파일로 정의됩니다.

### 주요 속성

- **모듈 경로**: 현재 모듈의 고유 식별자(버전 번호와 결합됨); 모듈 내 모든 패키지의 임포트 접두사 역할
- **최소 Go 버전**: 현재 모듈에 필요한 최소 Go 버전
- **필요한 모듈**: 현재 모듈에 필요한 다른 모듈의 최소 버전 목록
- **Replace/Exclude 지시문**: 필요한 모듈을 다른 모듈 버전이나 로컬 디렉터리로 대체하거나, 특정 버전을 제외하는 지시

## go.mod 파일 생성

`go.mod` 파일은 다음을 사용하여 생성됩니다:

```bash
$ go mod init example/mymodule
```

## 의존성 관리

`go` 명령을 사용하여 의존성을 관리하고 `go.mod`를 일관되게 유지합니다:
- [`go get`](/ref/mod#go-get)
- [`go mod tidy`](/ref/mod#go-mod-tidy)
- [`go mod edit`](/ref/mod#go-mod-edit)

도움말 보기: `go help mod tidy`

---

## go.mod 파일 예시

```
module example.com/mymodule

go 1.14

require (
    example.com/othermodule v1.2.3
    example.com/thismodule v1.2.3
    example.com/thatmodule v1.2.3
)

replace example.com/thatmodule => ../thatmodule
exclude example.com/thismodule v1.3.0
```

---

## 지시문 레퍼런스

### `module`

**목적**: 모듈의 모듈 경로(고유 식별자)를 선언

**구문**:
```
module module-path
```

**매개변수**:
- `module-path`: 모듈의 고유 식별자, 일반적으로 모듈을 다운로드할 수 있는 저장소 위치

**예시**:
```
module example.com/mymodule
```

```
module example.com/mymodule/v2
```

**참고**:
- v2 이상의 모듈은 경로가 메이저 버전 번호로 끝나야 함(`/v2`)
- 모범 사례: 처음에 게시하지 않더라도 저장소 경로를 사용
- 임시 모듈의 경우 모듈 이름과 함께 소유하거나 통제하는 도메인 사용
- 저장소 위치가 불확실한 경우 `<회사명>/stringtools`와 같은 안전한 대체물 사용

---

### `go`

**목적**: 모듈이 작성된 Go 버전을 나타냄

**구문**:
```
go minimum-go-version
```

**매개변수**:
- `minimum-go-version`: 이 모듈의 패키지를 컴파일하는 데 필요한 최소 Go 버전

**예시**:
```
go 1.14
```

**참고**:

- **Go 1.21+**: 필수 요구 사항(Go 툴체인이 더 새로운 버전을 선언하는 모듈을 거부)
- **1.21 이전**: 권고 사항만
- Go 툴체인 선택에 영향("[Go 툴체인](/doc/toolchain)" 참조)
- 언어 기능 제한: 컴파일러가 지정된 버전 이후에 도입된 기능을 거부
- `go` 명령 동작에 영향:
  - **Go 1.14+**: `vendor/modules.txt`가 있고 일관되면 자동 벤더링이 활성화될 수 있음
  - **Go 1.16+**: `all` 패키지 패턴이 메인 모듈에서 전이적으로 임포트된 패키지만 매칭
  - **Go 1.17+**:
    - 전이적으로 임포트된 패키지를 제공하는 각 모듈에 대한 명시적 `require` 지시문
    - 간접 의존성이 별도의 블록에 기록됨
    - `go mod vendor`가 벤더된 의존성의 `go.mod`와 `go.sum` 파일을 생략
    - `go mod vendor`가 `vendor/modules.txt`에 각 의존성의 `go` 버전을 기록
  - **Go 1.21+**:
    - `go` 줄이 필요한 최소 버전을 선언
    - `go` 줄이 모든 의존성 이상이어야 함
    - 더 이상 이전 Go 버전과의 호환성을 유지하지 않음
    - `go.sum`의 체크섬에 더 신중함

**최대**: `go.mod` 파일당 하나의 `go` 지시문

---

### `toolchain`

**목적**: 이 모듈에서 사용할 권장 Go 툴체인 선언

**구문**:
```
toolchain toolchain-name
```

**매개변수**:
- `toolchain-name`: 권장 툴체인 이름. 표준 형식: `go_V_` (예: `go1.21.0`, `go1.18rc1`)
  - 특수 값 `default`: 자동 툴체인 전환 비활성화

**예시**:
```
toolchain go1.21.0
```

**참고**:
- 모듈이 메인 모듈이고 기본 툴체인이 더 오래된 경우에만 효과가 있음
- 툴체인 선택에 대한 자세한 내용은 "[Go 툴체인](/doc/toolchain)" 참조

---

### `godebug`

**목적**: 메인 패키지에 대한 기본 [GODEBUG](/doc/godebug) 설정을 나타냄

**구문**:
```
godebug debug-key=debug-value
```

**매개변수**:
- `debug-key`: 설정 이름([GODEBUG 히스토리](/doc/godebug#history) 참조)
- `debug-value`: 설정 값(달리 지정되지 않는 한 비활성화는 `0`, 활성화는 `1`)

**예시**:
```
godebug asynctimerchan=0
```

```
godebug (
    default=go1.21
    panicnil=1
)
```

**참고**:
- 현재 모듈의 메인 패키지와 테스트 바이너리에만 적용됨
- 모듈이 의존성으로 사용될 때는 효과 없음
- 툴체인 기본값을 재정의하고, 메인 패키지의 명시적 `//go:debug` 줄에 의해 재정의됨
- 자세한 내용은 "[Go, 하위 호환성, 그리고 GODEBUG](/doc/godebug)" 참조

---

### `require`

**목적**: 모듈을 의존성으로 선언하고 최소 필요 버전을 지정

**구문**:
```
require module-path module-version
```

**매개변수**:
- `module-path`: 일반적으로 저장소 도메인과 모듈 이름의 연결(v2+는 `/v2`로 끝나야 함)
- `module-version`: 릴리스 버전(예: `v1.2.3`) 또는 의사 버전(예: `v0.0.0-20200921210052-fa0125251cc4`)

**예시**:
```
require example.com/othermodule v1.2.3
```

```
require example.com/othermodule v0.0.0-20200921210052-fa0125251cc4
```

**참고**:
- 패키지 임포트 시 `go` 명령에 의해 자동으로 추가됨
- Go는 태그가 없는 버전에 대해 의사 버전 번호를 할당
- `replace` 지시문과 결합하여 비저장소 위치를 사용할 수 있음
- 의사 버전은 버전이 아직 태그되지 않았을 때 Go 도구에 의해 생성됨

**관련 주제**:
- [의존성 추가](/doc/modules/managing-dependencies#adding_dependency)
- [특정 의존성 버전 가져오기](/doc/modules/managing-dependencies#getting_version)
- [사용 가능한 업데이트 발견](/doc/modules/managing-dependencies#discovering_updates)
- [의존성 업그레이드 또는 다운그레이드](/doc/modules/managing-dependencies#upgrading)
- [코드의 의존성 동기화](/doc/modules/managing-dependencies#synchronizing)
- [모듈 버전 번호 매기기](/doc/modules/version-numbers)

---

### `tool`

**목적**: 패키지를 의존성으로 추가하고 `go tool`로 실행할 수 있게 함

**구문**:
```
tool package-path
```

**매개변수**:
- `package-path`: 도구의 패키지 경로(모듈 경로와 모듈 내 패키지 경로의 연결)

**예시**:

현재 모듈의 도구:
```
module example.com/mymodule

tool example.com/mymodule/cmd/mytool
```

별도 모듈의 도구:
```
module example.com/mymodule

tool example.com/atool/cmd/atool

require example.com/atool v1.2.3
```

**참고**:
- `go tool mytool` 또는 전체 경로 `go tool example.com/mymodule/cmd/mytool`로 실행
- 워크스페이스 모드에서는 모든 워크스페이스 모듈의 도구를 실행할 수 있음
- 도구 모듈 버전을 선택하려면 `require` 지시문이 필요
- `replace` 및 `exclude` 지시문이 도구와 의존성에 적용됨
- [도구 의존성](/doc/modules/managing-dependencies#tools) 참조

---

### `replace`

**목적**: 모듈 버전을 다른 모듈 버전이나 로컬 디렉터리로 대체

**구문**:
```
replace module-path [module-version] => replacement-path [replacement-version]
```

**매개변수**:
- `module-path`: 대체할 모듈 경로
- `module-version` (선택): 대체할 특정 버전; 생략하면 모든 버전이 대체됨
- `replacement-path`: 대체할 모듈 경로 또는 로컬 디렉터리 경로
- `replacement-version` (선택): replacement-path가 모듈 경로(로컬 디렉터리가 아님)인 경우에만

**예시**:

포크된 저장소로 대체:
```
require example.com/othermodule v1.2.3

replace example.com/othermodule => example.com/myfork/othermodule v1.2.3-fixed
```

다른 버전으로 대체:
```
require example.com/othermodule v1.2.2

replace example.com/othermodule => example.com/othermodule v1.2.3
```

특정 버전 대체:
```
replace example.com/othermodule v1.2.5 => example.com/othermodule v1.2.3
```

로컬 코드로 대체(모든 버전):
```
require example.com/othermodule v1.2.3

replace example.com/othermodule => ../othermodule
```

특정 버전을 로컬 코드로 대체:
```
require example.com/othermodule v1.2.5

replace example.com/othermodule v1.2.5 => ../othermodule
```

**참고**:
- 대체 시 임포트 문을 변경하지 마세요—원래 모듈 경로를 사용
- 메인 모듈에서만 적용됨; 의존성에서는 무시됨
- 수정 사항을 개발하거나 테스트할 때 모듈 경로를 임시로 대체
- 특정 버전 없이 `require`와 함께 가짜 버전 사용:
  ```
  require example.com/mod v0.0.0-replace

  replace example.com/mod v0.0.0-replace => ./mod
  ```
- 참고: `replace`가 메인 모듈에서만 적용되므로 이는 종속 모듈을 손상시킴

**사용 사례**:
- 아직 저장소에 없는 새 모듈 코드 테스트
- 복제된 의존성 저장소의 수정 사항 테스트
- 의존성의 포크된 버전 사용
- 개발을 위해 로컬 모듈 코드 사용

**관련 주제**:
- [자신의 저장소 포크에서 외부 모듈 코드 요청](/doc/modules/managing-dependencies#external_fork)
- [로컬 디렉터리의 모듈 코드 요청](/doc/modules/managing-dependencies#local_directory)
- [모듈 버전 번호 매기기](/doc/modules/version-numbers)

---

### `exclude`

**목적**: 의존성 그래프에서 특정 모듈 버전을 제외

**구문**:
```
exclude module-path module-version
```

**매개변수**:
- `module-path`: 제외할 모듈 경로
- `module-version`: 제외할 특정 버전

**예시**:
```
exclude example.com/theirmodule v1.3.0
```

**참고**:
- 로드할 수 없는 간접 필요 모듈에 사용(예: 유효하지 않은 체크섬)
- 메인 모듈에서만 적용됨; 의존성에서는 무시됨
- `go mod edit`를 사용하여 설정 가능:
  ```bash
  go mod edit -exclude=example.com/theirmodule@v1.3.0
  ```
- [모듈 버전 번호 매기기](/doc/modules/version-numbers) 참조

---

### `retract`

**목적**: 의존해서는 안 되는 버전 또는 버전 범위를 나타냄

**구문**:
```
retract version // rationale
retract [version-low,version-high] // rationale
```

**매개변수**:
- `version`: 철회할 단일 버전
- `version-low`: 철회 범위의 하한(포함)
- `version-high`: 철회 범위의 상한(포함)
- `rationale` (선택): 철회 이유를 설명하는 주석

**예시**:

단일 버전:
```
retract v1.1.0 // 실수로 게시됨.
```

버전 범위:
```
retract [v1.0.0,v1.0.5] // 일부 플랫폼에서 빌드 실패.
```

**참고**:
- 철회된 버전은 `go get`, `go mod tidy` 또는 다른 명령을 통해 자동으로 업그레이드되지 않음
- `go list -m -u`에서 사용 가능한 업데이트로 표시되지 않음
- 철회된 버전은 기존 의존자를 위해 계속 사용 가능해야 함
- 삭제된 버전은 미러(예: proxy.golang.org)에 남아 있을 수 있음
- 철회된 버전에 의존하는 사용자는 `go get` 또는 `go list -m -u`를 통해 알림을 받음
- 최신 모듈 버전의 `retract` 지시문을 읽어서 발견됨
- 최신 버전은 (순서대로) 다음에 의해 결정됨:
  1. 가장 높은 릴리스 버전
  2. 가장 높은 프리릴리스 버전
  3. 저장소의 기본 브랜치 팁에 대한 의사 버전

**철회 게시**:
- 발견을 위해 거의 항상 새로운, 더 높은 버전을 태그해야 함
- 철회를 알리기 위해서만 버전을 게시할 수 있음
- 새 버전이 자신을 철회할 수 있음:
  ```
  retract v1.0.0 // 실수로 게시됨.
  retract v1.0.1 // 철회만 포함.
  ```

**중요**: 게시되면 버전을 변경할 수 없음. 나중에 다른 커밋에 태그하면 `go.sum` 또는 체크섬 데이터베이스에서 체크섬 불일치가 발생할 수 있음.

**발견**:
- 철회된 버전은 `go list -m -versions` 출력에 표시되지 않음
- `-retracted` 플래그를 사용하여 표시: `go list -m -versions -retracted`
- [`go list -m`](/ref/mod#go-list-m) 레퍼런스 참조

---

## 요약 표

| 지시문 | 목적 | 필수 |
|--------|------|------|
| `module` | 모듈 경로 선언 | 예 |
| `go` | 최소 Go 버전 설정 | 권장 |
| `toolchain` | Go 툴체인 권장 | 선택 |
| `godebug` | GODEBUG 기본값 설정 | 선택 |
| `require` | 의존성 선언 | 필요 시 |
| `tool` | 도구 의존성 선언 | 선택 |
| `replace` | 모듈 버전/경로 대체 | 선택 |
| `exclude` | 모듈 버전 제외 | 선택 |
| `retract` | 버전 철회 | 선택 |


---

# Go 모듈 레퍼런스

> **원문:** https://go.dev/ref/mod

## 소개

모듈은 Go가 의존성을 관리하는 방식입니다. 이 문서는 Go의 모듈 시스템에 대한 상세한 레퍼런스 매뉴얼로, Go 프로젝트의 생성, 버전 관리, 배포를 다룹니다.

## 모듈, 패키지, 버전

### 핵심 개념

**모듈**은 함께 릴리스, 버전 관리, 배포되는 패키지의 모음입니다. 모듈은 `go.mod` 파일에 선언된 모듈 경로로 식별됩니다.

- **모듈 경로**: 모듈의 정규 이름 (예: `golang.org/x/net`)
- **패키지 경로**: 모듈 경로 + 하위 디렉터리 (예: `golang.org/x/net/html`)
- **모듈 루트**: `go.mod` 파일이 있는 디렉터리

### 버전

버전은 시맨틱 버전 관리를 따릅니다 (예: `v1.2.3`):

```
vMAJOR.MINOR.PATCH[-prerelease][+metadata]
```

예시: `v0.0.0`, `v1.12.134`, `v8.0.5-pre`, `v2.0.9+meta`

**버전 규칙:**
- **메이저 버전**: 하위 호환되지 않는 변경 시 증가
- **마이너 버전**: 하위 호환되는 기능 추가 시 증가
- **패치 버전**: 버그 수정 시 증가
- **프리릴리스**: `-` 접미사가 붙은 버전(예: `v1.2.3-pre`)은 불안정함
- **빌드 메타데이터**: `+` 접미사는 비교 시 무시됨 (`+incompatible` 제외)

불안정 버전(메이저 버전 0 또는 프리릴리스)은 호환성을 보장하지 않습니다.

### 의사 버전(Pseudo-versions)

의사 버전은 VCS의 특정 리비전 정보를 인코딩합니다:

```
vX.0.0-yyyymmddhhmmss-abcdefabcdef (기본 버전 없음)
vX.Y.Z-pre.0.yyyymmddhhmmss-abcdefabcdef (프리릴리스 기반)
vX.Y.(Z+1)-0.yyyymmddhhmmss-abcdefabcdef (릴리스 기반)
```

**예시:** `v0.0.0-20191109021931-daa7c04131f5`

### 메이저 버전 접미사

v2 이상의 모듈은 모듈 경로에 메이저 버전 접미사가 필요합니다:
- `v1.0.0` → 경로: `example.com/mod`
- `v2.0.0` → 경로: `example.com/mod/v2`

이를 통해 동일한 빌드에서 여러 메이저 버전이 공존할 수 있습니다.

## `go.mod` 파일 형식

### 기본 구조

```go
module example.com/my/thing

go 1.23.0

require example.com/other/thing v1.0.2
require example.com/new/thing/v2 v2.3.4
exclude example.com/old/thing v1.2.3
replace example.com/bad/thing v1.4.5 => example.com/good/thing v1.4.5
retract [v1.9.0, v1.9.5]
```

### 지시문

#### `module` 지시문

```
module golang.org/x/net
```

메인 모듈의 경로를 선언합니다. 정확히 한 번만 필요합니다.

**사용 중단(Deprecation):**
```
// Deprecated: use example.com/mod/v2 instead.
module example.com/mod
```

#### `go` 지시문

```
go 1.23.0
```

필요한 최소 Go 버전을 지정합니다. 언어 기능 사용 가능 여부와 컴파일러 동작을 설정합니다.

#### `require` 지시문

```
require (
    golang.org/x/crypto v1.4.5 // indirect
    golang.org/x/text v1.6.7
)
```

최소 필요 모듈 버전을 선언합니다. `// indirect` 주석은 해당 모듈에서 직접 임포트하지 않음을 나타냅니다.

#### `exclude` 지시문

```
exclude (
    golang.org/x/crypto v1.4.5
    golang.org/x/text v1.6.7
)
```

특정 버전의 로드를 방지합니다. 메인 모듈의 `go.mod`에서만 적용됩니다.

#### `replace` 지시문

```
replace (
    golang.org/x/net v1.2.3 => example.com/fork/net v1.4.5
    golang.org/x/net => example.com/fork/net v1.4.5
    golang.org/x/net v1.2.3 => ./fork/net
    golang.org/x/net => ./fork/net
)
```

모듈 버전을 대체 모듈(로컬 경로 또는 다른 모듈)로 교체합니다. 메인 모듈의 `go.mod`에서만 적용됩니다.

#### `retract` 지시문

```
retract (
    v1.0.0 // 실수로 게시됨.
    [v1.0.0, v1.9.9] // 철회만 포함.
)
```

버전을 사용에 부적합하다고 표시합니다. 사용자는 철회된 버전으로 자동 업그레이드되지 않습니다.

#### `tool` 지시문

```
tool (
    golang.org/x/tools/cmd/stringer
    example.com/module/cmd/a
)
```

`go tool`을 통해 사용할 수 있는 도구로 패키지를 추가합니다.

#### `ignore` 지시문

```
ignore (
    ./node_modules
    static
    ./third_party/javascript
)
```

패키지 패턴 매칭 시 지정된 디렉터리를 무시하도록 go 명령에 지시합니다.

#### `toolchain` 지시문

```
toolchain go1.21.0
```

이 모듈에서 사용할 권장 Go 툴체인을 선언합니다.

#### `godebug` 지시문

```
godebug (
    panicnil=1
    asynctimerchan=0
)
```

이 모듈이 메인 모듈일 때 적용할 GODEBUG 설정을 선언합니다.

## 최소 버전 선택(MVS)

MVS는 모듈 버전을 선택하는 Go의 알고리즘입니다:

1. 메인 모듈에서 시작하여 의존성 그래프를 순회
2. 각 모듈의 가장 높은 필요 버전을 추적
3. **빌드 목록** 생성 - 빌드에 사용될 모듈 버전 목록

**핵심 원칙:** 빌드 목록에는 모든 요구 사항을 충족하는 최소 버전이 포함됩니다.

### 예시

```
Main은 다음을 필요로 함:
  - A ≥ 1.2
  - B ≥ 1.2

A 1.2는 C ≥ 1.3을 필요로 함
B 1.2는 C ≥ 1.4를 필요로 함
C 1.3은 D ≥ 1.2를 필요로 함
C 1.4는 D ≥ 1.2를 필요로 함

결과: A 1.2, B 1.2, C 1.4, D 1.2
```

필요하지 않은 한 더 높은 버전은 선택되지 않습니다.

### 교체와 제외

**교체:**
```
replace golang.org/x/net v1.2.3 => example.com/fork/net v1.4.5
```
하나의 모듈을 다른 모듈로 교체합니다(의존성이 다를 수 있음).

**제외:**
```
exclude golang.org/x/net v1.2.3
```
그래프에서 버전을 제거합니다. 의존하는 측은 다음으로 높은 버전을 사용합니다.

## `go.work` 파일 (워크스페이스)

워크스페이스를 통해 여러 모듈을 함께 개발할 수 있습니다:

```
go 1.23.0

use (
    ./my/first/thing
    ./my/second/thing
)

replace example.com/bad/thing v1.4.5 => example.com/good/thing v1.4.5
```

### 지시문

#### `go` 지시문
```
go 1.23.0
```

필수. 워크스페이스의 Go 버전을 지정합니다.

#### `use` 지시문
```
use (
    ./mymod
    ../othermod
    ./subdir/thirdmod
)
```

워크스페이스에 모듈을 추가합니다.

#### `replace` 지시문
`go.mod`와 유사하지만 워크스페이스 모듈의 모든 replace를 재정의합니다.

## 모듈 인식 명령

### 빌드 명령

다음 명령들은 모두 모듈을 인식합니다:
- `go build`
- `go fix`
- `go generate`
- `go install`
- `go list`
- `go run`
- `go test`
- `go vet`

### 일반 플래그

```bash
-mod=mod      # go.mod 자동 업데이트 (누락된 패키지 찾기)
-mod=readonly # go.mod 업데이트 필요 시 오류 보고 (기본값)
-mod=vendor   # vendor 디렉터리 사용, 네트워크/캐시 사용 안 함
```

### `go get`

```bash
# 특정 모듈 업그레이드
go get golang.org/x/net

# 임포트된 패키지를 제공하는 모듈 업그레이드
go get -u ./...

# 버전 지정
go get golang.org/x/text@v0.3.2

# 브랜치로 업데이트
go get golang.org/x/text@master

# 의존성 제거
go get golang.org/x/text@none

# 최소 Go 버전 업그레이드
go get go

# 권장 툴체인 업그레이드
go get toolchain
go get toolchain@patch
```

**플래그:**
- `-d`: 빌드/설치하지 않고 의존성만 관리
- `-u`: 모든 의존성 업그레이드
- `-u=patch`: 최신 패치 버전으로 업그레이드
- `-t`: 테스트 의존성 고려

### `go install`

```bash
# 최신 버전 설치 (로컬 go.mod 무시)
go install golang.org/x/tools/gopls@latest

# 특정 버전 설치
go install golang.org/x/tools/gopls@v0.6.4

# 로컬 go.mod 버전 사용하여 설치
go install golang.org/x/tools/gopls

# 디렉터리의 모든 프로그램 설치
go install ./cmd/...
```

Go 1.16부터 프로그램 빌드 및 설치에 선호됩니다.

### `go list -m`

```bash
# 모든 의존성 나열
go list -m all

# 모듈의 버전 나열
go list -m -versions example.com/m

# 업그레이드 정보와 함께 나열
go list -m -u all

# JSON 출력
go list -m -json example.com/m@latest

# 철회된 버전 표시
go list -m -retracted example.com/m@latest
```

**Module 구조체 필드:**
- `Path`: 모듈 경로
- `Version`: 선택된 버전
- `Versions`: 사용 가능한 모든 버전
- `Replace`: 대체 모듈
- `Update`: 사용 가능한 업데이트 (`-u` 사용 시)
- `Retracted`: 철회 정보
- `Deprecated`: 사용 중단 메시지
- `Dir`: 모듈 파일이 있는 디렉터리
- `GoMod`: go.mod 파일 경로
- `GoVersion`: 모듈에서 사용된 Go 버전

### `go mod download`

```bash
# 모든 의존성 다운로드
go mod download

# 특정 모듈 다운로드
go mod download golang.org/x/mod@v0.2.0

# JSON 출력
go mod download -json

# 실행된 명령 표시
go mod download -x
```

### `go mod tidy`

```bash
go mod tidy
```

누락된 요구 사항을 추가하고 사용되지 않는 것을 제거합니다. `go.mod`와 `go.sum`을 업데이트합니다.

### `go mod vendor`

```bash
go mod vendor
```

모든 의존성의 복사본과 `vendor/modules.txt` 매니페스트가 있는 `vendor/` 디렉터리를 생성합니다.

### `go mod init`

```bash
go mod init example.com/mymodule
```

새 `go.mod` 파일을 생성합니다.

### `go mod edit`

```bash
go mod edit -module=example.com/newname
go mod edit -go=1.21
go mod edit -require=golang.org/x/text@v0.3.0
```

`go.mod` 파일의 저수준 편집.

## 모듈 그래프 가지치기 (Go 1.17+)

`go 1.17` 이상의 모듈에서는 모듈 그래프가 가지치기됩니다:

- `go 1.17+`를 지정하는 의존성의 **직접적인** 요구 사항만 포함
- `go 1.17+` 모듈의 전이적 의존성은 가지치기됨
- `go 1.16` 이하 의존성은 전체 전이적 클로저를 포함

**이점:** 더 작은 빌드 목록과 빠른 모듈 로딩.

### 지연 모듈 로딩 (Go 1.17+)

`go` 명령은 필요할 때까지 전체 모듈 그래프 로딩을 피합니다:

1. 메인 모듈의 `go.mod`만 로드
2. 요청된 패키지 로드 시도
3. 패키지를 찾지 못하면 필요 시 전체 그래프 로드
4. 로드된 모듈의 로컬 일관성 검증

## 벤더링

다음으로 벤더된 의존성을 생성합니다:

```bash
go mod vendor
```

생성되는 것:
- 모듈 복사본이 있는 `vendor/` 디렉터리
- `vendor/modules.txt` 매니페스트

**사용:**
```bash
go build -mod=vendor        # vendor 디렉터리 사용
go mod vendor -o=...        # vendor 업데이트
```

벤더링 활성화:
- `go.mod`가 `go 1.14+`를 지정하고 `vendor/`가 존재하면 자동
- `-mod=vendor`로 명시적 활성화

## 환경 변수

### 주요 모듈 환경 변수

- `GO111MODULE`: `on`, `off`, 또는 `auto` (모듈 인식 모드 제어)
- `GOMODCACHE`: 모듈 캐시 위치 (기본값: `$GOPATH/pkg/mod`)
- `GOPROXY`: 쉼표로 구분된 프록시 URL 목록
- `GOSUMDB`: 체크섬 데이터베이스 URL
- `GOPRIVATE`: 비공개 모듈의 쉼표로 구분된 패턴
- `GONOPROXY`: 직접 가져올 패턴 (프록시 경유 안 함)
- `GOINSECURE`: 안전하지 않은 스킴을 허용할 패턴
- `GOWORK`: `go.work` 파일 경로 (또는 워크스페이스 모드 비활성화를 위해 `off`)

## 버전 쿼리

`go get` 및 기타 명령에서 지원:

```bash
@latest          # 최신 릴리스 또는 프리릴리스
@upgrade         # 최신 버전 (go get의 기본값)
@patch           # 최신 패치 릴리스
v1.2.3           # 특정 버전
v1.2             # 버전 접두사
master           # 브랜치 이름
daa7c041         # 커밋 해시 (의사 버전으로 변환됨)
```

## 모듈이 없는 저장소

Go는 `go.mod`가 없는 저장소에서도 작동할 수 있습니다:

- 필요 시 `go.mod`를 합성
- 모듈 프록시를 사용하여 합성된 `go.mod` 제공
- 메이저 버전 2+로 태그된 버전은 `+incompatible` 접미사를 얻음

예시:
```
require example.com/m v4.1.2+incompatible
```

## 요약

Go 모듈은 다음을 제공합니다:
- **버전 관리**: 메이저 버전 접미사를 통한 시맨틱 버전 관리
- **의존성 관리**: 결정론적 빌드를 위한 MVS 알고리즘
- **유연성**: 특수한 경우를 위한 replace 및 exclude 지시문
- **재현성**: 잠금 파일(`go.sum`)과 최소 버전 선택
- **도구**: 관리를 위한 풍부한 `go mod` 명령 세트

모든 모듈 정보는 사람이 읽고 기계가 쓸 수 있는 `go.mod` 파일에 선언적으로 지정됩니다.

