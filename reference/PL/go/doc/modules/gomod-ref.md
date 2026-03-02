# go.mod 파일 레퍼런스

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
