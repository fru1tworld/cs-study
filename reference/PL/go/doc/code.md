# Go 코드 작성법

## 소개
이 문서는 모듈 내에서 간단한 Go 패키지를 개발하는 방법을 보여주고, Go 모듈, 패키지, 명령을 가져오고 빌드하고 설치하는 표준 방법인 `go` 도구를 소개합니다.

## 코드 구성

### 핵심 개념:
- **패키지**: 같은 디렉터리에 있는 소스 파일들의 모음으로 함께 컴파일됨
- **모듈**: 함께 릴리스되는 관련 Go 패키지들의 모음
- **저장소**: 하나 이상의 모듈을 포함; 일반적으로 루트에 하나의 모듈이 있음
- **go.mod 파일**: 모듈 경로(모듈 내 모든 패키지의 import 경로 접두사)를 선언

### 모듈 경로:
- 패키지의 import 경로 접두사 역할
- `go` 명령이 모듈을 다운로드해야 하는 위치를 나타냄
- 예: `golang.org/x/tools`

### Import 경로:
- 패키지를 import하는 데 사용되는 문자열
- 모듈 경로 + 모듈 내 하위 디렉터리의 조합
- 예: `github.com/google/go-cmp/cmp` (모듈) + `cmp/` (디렉터리) = `github.com/google/go-cmp/cmp` (import 경로)

---

## 첫 번째 프로그램

### 1단계: 모듈 생성
```bash
$ mkdir hello
$ cd hello
$ go mod init example/user/hello
go: creating new go.mod: module example/user/hello
```

이렇게 하면 `go.mod` 파일이 생성됩니다:
```
module example/user/hello

go 1.16
```

### 2단계: hello.go 생성
```go
package main

import "fmt"

func main() {
    fmt.Println("Hello, world.")
}
```

**참고**: Go 소스 파일의 첫 번째 문장은 `package name`이어야 합니다. 실행 가능한 명령은 항상 `package main`을 사용해야 합니다.

### 3단계: 빌드 및 설치
```bash
$ go install example/user/hello
```

이 명령은:
- `hello` 명령을 빌드합니다
- 실행 가능한 바이너리를 생성합니다
- `$HOME/go/bin/hello`(또는 Windows에서는 `%USERPROFILE%\go\bin\hello.exe`)에 설치합니다

### 설치 디렉터리 제어:
- **GOBIN**: 설정되면 바이너리가 여기에 설치됨
- **GOPATH**: 설정되면 바이너리가 첫 번째 디렉터리의 `bin` 하위 디렉터리에 설치됨
- **기본값**: `$HOME/go/bin` 또는 `%USERPROFILE%\go\bin`

### 환경 변수 설정:
```bash
$ go env -w GOBIN=/somewhere/else/bin
$ go env -u GOBIN  # 설정 해제
```

### 프로그램 실행:
```bash
$ export PATH=$PATH:$(dirname $(go list -f '{{.Target}}' .))
$ hello
Hello, world.
```

### 버전 관리 초기화 (선택사항):
```bash
$ git init
$ git add go.mod hello.go
$ git commit -m "initial commit"
```

---

## 모듈에서 패키지 Import하기

### morestrings 패키지 생성

`$HOME/hello/morestrings/reverse.go`를 생성합니다:

```go
// Package morestrings는 표준 "strings" 패키지에서 제공하는 것 이상의
// UTF-8 인코딩 문자열을 조작하는 추가 함수를 구현합니다.
package morestrings

// ReverseRunes는 인수 문자열을 룬 단위로 왼쪽에서 오른쪽으로 역순으로 반환합니다.
func ReverseRunes(s string) string {
    r := []rune(s)
    for i, j := 0, len(r)-1; i < len(r)/2; i, j = i+1, j-1 {
        r[i], r[j] = r[j], r[i]
    }
    return string(r)
}
```

**참고**: 대문자로 시작하는 함수는 export되어 다른 패키지에서 사용할 수 있습니다.

### 패키지 컴파일 테스트:
```bash
$ cd $HOME/hello/morestrings
$ go build
```

### hello 프로그램에서 패키지 사용

`$HOME/hello/hello.go`를 수정합니다:

```go
package main

import (
    "fmt"
    "example/user/hello/morestrings"
)

func main() {
    fmt.Println(morestrings.ReverseRunes("!oG ,olleH"))
}
```

### 설치 및 실행:
```bash
$ go install example/user/hello
$ hello
Hello, Go!
```

---

## 원격 모듈에서 패키지 Import하기

### 예제: google/go-cmp 사용

원격 패키지를 import하도록 프로그램을 수정합니다:

```go
package main

import (
    "fmt"
    "example/user/hello/morestrings"
    "github.com/google/go-cmp/cmp"
)

func main() {
    fmt.Println(morestrings.ReverseRunes("!oG ,olleH"))
    fmt.Println(cmp.Diff("Hello World", "Hello Go"))
}
```

### 의존성 다운로드:
```bash
$ go mod tidy
go: finding module for package github.com/google/go-cmp/cmp
go: found github.com/google/go-cmp/cmp in github.com/google/go-cmp v0.5.4
```

업데이트된 `go.mod`:
```
module example/user/hello

go 1.16

require github.com/google/go-cmp v0.5.4
```

### 설치 및 실행:
```bash
$ go install example/user/hello
$ hello
Hello, Go!
  string(
-     "Hello World",
+     "Hello Go",
  )
```

### 모듈 캐시:
- 다운로드된 모듈은 `GOPATH`의 `pkg/mod` 하위 디렉터리에 저장됨
- 해당 버전을 필요로 하는 모든 모듈 간에 공유됨
- 파일은 읽기 전용으로 표시됨
- 캐시 지우기: `go clean -modcache`

---

## 테스트

### 테스트 프레임워크:
- `go test` 명령 사용
- `testing` 패키지 사용
- 테스트 파일은 `_test.go`로 끝남
- 테스트 함수는 `TestXXX`로 이름 지정, 시그니처는 `func (t *testing.T)`

### reverse_test.go 생성

`$HOME/hello/morestrings/reverse_test.go`를 생성합니다:

```go
package morestrings

import "testing"

func TestReverseRunes(t *testing.T) {
    cases := []struct {
        in, want string
    }{
        {"Hello, world", "dlrow ,olleH"},
        {"Hello, 世界", "界世 ,olleH"},
        {"", ""},
    }
    for _, c := range cases {
        got := ReverseRunes(c.in)
        if got != c.want {
            t.Errorf("ReverseRunes(%q) == %q, want %q", c.in, got, c.want)
        }
    }
}
```

### 테스트 실행:
```bash
$ cd $HOME/hello/morestrings
$ go test
PASS
ok  	example/user/hello/morestrings 0.165s
```

**참고**: 테스트가 `t.Error` 또는 `t.Fail`을 호출하면 테스트는 실패로 간주됩니다.

자세한 내용: `go help test` 및 [testing 패키지 문서](/pkg/testing/)

---

## 다음 단계

1. **메일링 리스트 구독**: [golang-announce](https://groups.google.com/group/golang-announce)
2. **읽기**: [Effective Go](/doc/effective_go.html) - 명확하고 관용적인 Go 코드 작성 팁
3. **학습**: [Go 투어](/tour/) - 언어를 제대로 배우기
4. **탐색**: [문서 페이지](/doc/#articles) - Go에 대한 심층 기사

---

## 도움 받기

- **실시간 도움**: [gophers Slack 서버](https://gophers.slack.com/messages/general/) ([초대](https://invite.slack.golangbridge.org/))
- **메일링 리스트**: [Go Nuts](https://groups.google.com/group/golang-nuts)
- **버그 보고**: [Go 이슈 트래커](/issue)
