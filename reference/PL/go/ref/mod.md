# Go 모듈 레퍼런스

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
