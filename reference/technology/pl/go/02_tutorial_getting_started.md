# 튜토리얼: 시작하기와 모듈 생성

# 튜토리얼: Go 시작하기

> **원문:** https://go.dev/doc/tutorial/getting-started

## 개요
이 튜토리얼은 Go 프로그래밍에 대한 간략한 소개를 제공합니다. 설치, 기본 코드 작성, `go` 명령어 사용, 패키지 검색, 외부 모듈 함수 호출 등을 다룹니다.

## 사전 준비 사항
- 프로그래밍 경험 (함수에 대한 이해가 도움됨)
- 코드 에디터 (VSCode, GoLand 또는 Vim 권장)
- 명령 터미널 (Linux/Mac 터미널, 또는 Windows의 PowerShell/cmd)

## 설치
[다운로드 및 설치](/doc/install) 단계를 따르세요.

## 코드 작성하기

### 1단계: 프로젝트 디렉터리 생성
```bash
# Linux 또는 Mac에서:
cd

# Windows에서:
cd %HOMEPATH%

# hello 디렉터리 생성
mkdir hello
cd hello
```

### 2단계: 의존성 추적 활성화
`go mod init` 명령을 실행하여 `go.mod` 파일을 생성합니다:

```bash
$ go mod init example/hello
go: creating new go.mod: module example/hello
```

### 3단계: Hello World 코드 작성
다음 코드로 `hello.go` 파일을 생성합니다:

```go
package main

import "fmt"

func main() {
    fmt.Println("Hello, World!")
}
```

**코드 설명:**
- `main` 패키지를 선언합니다 (같은 디렉터리의 함수들을 그룹화)
- `fmt` 패키지를 import합니다 (텍스트 포맷팅을 위한 표준 라이브러리)
- `main()` 함수를 구현합니다 (main 패키지 실행 시 기본으로 실행됨)

### 4단계: 코드 실행
```bash
$ go run .
Hello, World!
```

`go run` 명령은 프로그램을 컴파일하고 실행합니다. 다른 `go` 명령을 보려면 `go help`를 사용하세요.

---

## 외부 패키지의 코드 호출하기

### 1단계: 외부 패키지 찾기
1. [pkg.go.dev](https://pkg.go.dev)를 방문합니다
2. "quote" 패키지를 검색합니다
3. 검색 결과에서 `rsc.io/quote` v1을 찾습니다
4. Documentation 섹션에서 `Go` 함수를 확인합니다

### 2단계: 외부 패키지 Import 및 사용
`hello.go`를 수정합니다:

```go
package main

import "fmt"

import "rsc.io/quote"

func main() {
    fmt.Println(quote.Go())
}
```

### 3단계: 모듈 요구 사항 추가
```bash
$ go mod tidy
go: finding module for package rsc.io/quote
go: found rsc.io/quote in rsc.io/quote v1.5.2
```

이 명령은:
- `rsc.io/quote` 모듈을 찾아 다운로드합니다
- 최신 버전(v1.5.2)을 다운로드합니다
- 모듈 인증을 위한 `go.sum` 파일을 생성합니다

### 4단계: 수정된 코드 실행
```bash
$ go run .
Don't communicate by sharing memory, share memory by communicating.
```

---

## 다음 단계

[Go 모듈 생성하기](/doc/tutorial/create-module.md) 튜토리얼을 통해 학습을 계속하세요.


---

# 튜토리얼: Go 모듈 생성하기

> **원문:** https://go.dev/doc/tutorial/create-module

## 개요
이것은 Go 언어의 기본 기능을 소개하는 튜토리얼의 첫 번째 부분입니다. 라이브러리 모듈과 호출 애플리케이션, 두 개의 모듈을 생성하는 방법을 안내합니다.

## 튜토리얼 순서
1. **모듈 생성하기** - 다른 모듈에서 호출할 수 있는 함수가 포함된 작은 모듈 작성하기
2. [다른 모듈에서 코드 호출하기](/doc/tutorial/call-module-code.html)
3. [에러 반환 및 처리하기](/doc/tutorial/handle-errors.html)
4. [임의의 인사말 반환하기](/doc/tutorial/random-greeting.html)
5. [여러 사람에게 인사말 반환하기](/doc/tutorial/greetings-multiple-people.html)
6. [테스트 추가하기](/doc/tutorial/add-a-test.html)
7. [애플리케이션 컴파일 및 설치하기](/doc/tutorial/compile-install.html)

## 사전 준비 사항
- **프로그래밍 경험** - 함수, 반복문, 배열에 대한 이해
- **코드 에디터** - VSCode(무료), GoLand(유료), 또는 Vim(무료)
- **명령 터미널** - Linux/Mac 터미널 또는 Windows의 PowerShell/cmd

## 다른 사람이 사용할 수 있는 모듈 시작하기

### 1단계: 홈 디렉터리로 이동
```bash
# Linux 또는 Mac에서:
cd

# Windows에서:
cd %HOMEPATH%
```

### 2단계: greetings 디렉터리 생성
```bash
mkdir greetings
cd greetings
```

### 3단계: 모듈 초기화
```bash
go mod init example.com/greetings
```

**출력:**
```
go: creating new go.mod: module example.com/greetings
```

`go mod init` 명령은 코드의 의존성과 코드가 지원하는 Go 버전을 추적하는 `go.mod` 파일을 생성합니다.

### 4단계: 코드 파일 생성
텍스트 에디터에서 `greetings.go`라는 파일을 생성합니다.

### 5단계: 코드 추가
다음 코드를 `greetings.go`에 붙여넣습니다:

```go
package greetings

import "fmt"

// Hello는 지정된 사람에 대한 인사말을 반환합니다.
func Hello(name string) string {
    // 이름을 메시지에 포함하는 인사말을 반환합니다.
    message := fmt.Sprintf("Hi, %v. Welcome!", name)
    return message
}
```

## 코드 설명

**이 코드가 하는 일:**

1. **패키지 선언** - `package greetings`는 관련 함수들을 모읍니다
2. **Hello 함수 구현:**
   - `string` 타입의 `name` 매개변수를 받습니다
   - `string`을 반환합니다
   - 대문자로 시작하는 함수는 export되어 다른 패키지에서 호출할 수 있습니다

3. **변수 선언** - `message`가 인사말을 담습니다
   - `:=` 단축 연산자를 사용하여 선언과 초기화를 동시에 수행합니다
   - 다음과 동일합니다:
     ```go
     var message string
     message = fmt.Sprintf("Hi, %v. Welcome!", name)
     ```

4. **fmt.Sprintf 사용** - 포맷된 인사말 메시지를 생성합니다
   - `%v` 포맷 동사에 `name` 매개변수 값을 대체합니다
   - 완성된 인사말 텍스트를 반환합니다

## 핵심 개념

- **모듈**은 관련 패키지들을 그룹화하여 독립적이고 유용한 기능을 제공합니다
- **go.mod 파일**은 의존성과 Go 버전 요구 사항을 지정합니다
- **Export된 이름** (대문자로 시작)은 다른 패키지에서 호출할 수 있습니다
- **모듈 경로** (`example.com/greetings`)는 Go 도구가 다운로드할 수 있어야 합니다

---

**다음 단계:** [다른 모듈에서 코드 호출하기](/doc/tutorial/call-module-code.html)

