# 튜토리얼: Go 시작하기

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
