# Go 설치와 튜토리얼

## 설치 및 코드 구성

## Go 다운로드 및 설치

> 원문: https://go.dev/doc/install

### 개요
Linux·Mac·Windows 등 다양한 운영체제에서 Go를 다운로드하고 설치하는 방법을 설명하는 문서.

### 관련 자료
- [Go 설치 관리](/doc/manage-install) - 여러 버전 설치 및 삭제 방법
- [소스에서 Go 설치하기](/doc/install/source) - 소스 코드에서 Go를 빌드하는 방법

---

### Linux 설치

1. 이전 Go 설치 제거
   ```bash
   $ rm -rf /usr/local/go && tar -C /usr/local -xzf go1.14.3.linux-amd64.tar.gz
   ```
   - root 권한이나 `sudo` 필요할 수 있음
   - 중요: 기존 /usr/local/go 디렉터리에 압축을 풀면 안 됨 → 설치가 손상될 수 있음

2. /usr/local/go/bin을 PATH에 추가
   - `$HOME/.profile` 또는 `/etc/profile`(시스템 전체 설치의 경우)에 다음 줄 추가
   ```bash
   export PATH=$PATH:/usr/local/go/bin
   ```
   - 변경 사항은 다음 로그인 시 적용됨 → 즉시 적용하려면 `source $HOME/.profile` 실행

3. 설치 확인
   ```bash
   $ go version
   ```

4. 설치된 버전이 출력되는지 확인

---

### macOS 설치

1. Go 설치
   - 다운로드한 패키지 파일을 엶
   - 설치 안내에 따름
   - `/usr/local/go`에 설치됨
   - 자동으로 `/usr/local/go/bin`을 PATH에 추가함
   - 참고: 변경 사항을 적용하려면 터미널 세션을 다시 시작해야 할 수 있음

2. 설치 확인
   ```bash
   $ go version
   ```

3. 설치된 버전이 출력되는지 확인

---

### Windows 설치

1. Go 설치
   - MSI 설치 파일을 엶
   - 설치 안내에 따름
   - 기본 설치 위치: `Program Files` 또는 `Program Files (x86)`
   - 설치 중 위치 변경 가능
   - 환경 변경 사항 적용 → 명령 프롬프트를 닫았다가 다시 열기

2. 설치 확인
   - 시작 메뉴 클릭
   - 검색 상자에 `cmd` 입력 후 Enter
   - 명령 프롬프트에서 다음 입력
     ```bash
     $ go version
     ```
   - 설치된 버전이 출력되는지 확인

---

### 다음 단계

설치 완료 후 [시작하기 튜토리얼](https://go.dev/doc/tutorial/getting-started) 방문 → 간단한 Go 코드 작성(약 10분 소요).

---

### 문제 보고

Go 코드나 문서의 버그·실수·불일치 발견 시:
- [이슈 트래커](https://github.com/golang/go/issues)에 티켓 제출
- 새 이슈 생성 전 기존 이슈 확인


---

## Go 코드 작성법

> 원문: https://go.dev/doc/code

### 소개
모듈 내에서 간단한 Go 패키지를 개발하는 방법을 보여주고, Go 모듈·패키지·명령을 가져오고 빌드하고 설치하는 표준 방법인 `go` 도구를 소개하는 문서.

### 코드 구성

#### 핵심 개념
- 패키지: 같은 디렉터리에 있는 소스 파일들의 모음 → 함께 컴파일됨
- 모듈: 함께 릴리스되는 관련 Go 패키지들의 모음
- 저장소: 하나 이상의 모듈을 포함 → 일반적으로 루트에 하나의 모듈이 있음
- go.mod 파일: 모듈 경로(모듈 내 모든 패키지의 import 경로 접두사)를 선언

#### 모듈 경로
- 패키지의 import 경로 접두사 역할
- `go` 명령이 모듈을 다운로드해야 하는 위치를 나타냄
- 예: `golang.org/x/tools`

#### Import 경로
- 패키지를 import하는 데 사용되는 문자열
- 모듈 경로 + 모듈 내 하위 디렉터리의 조합
- 예: `github.com/google/go-cmp/cmp` (모듈) + `cmp/` (디렉터리) = `github.com/google/go-cmp/cmp` (import 경로)

---

### 첫 번째 프로그램

#### 1단계: 모듈 생성
```bash
$ mkdir hello
$ cd hello
$ go mod init example/user/hello
go: creating new go.mod: module example/user/hello
```

`go.mod` 파일 생성됨:
```
module example/user/hello

go 1.16
```

#### 2단계: hello.go 생성
```go
package main

import "fmt"

func main() {
    fmt.Println("Hello, world.")
}
```

참고: Go 소스 파일의 첫 번째 문장은 `package name`이어야 함 → 실행 가능한 명령은 항상 `package main` 사용.

#### 3단계: 빌드 및 설치
```bash
$ go install example/user/hello
```

이 명령이 하는 일:
- `hello` 명령을 빌드함
- 실행 가능한 바이너리를 생성함
- `$HOME/go/bin/hello`(또는 Windows에서는 `%USERPROFILE%\go\bin\hello.exe`)에 설치함

#### 설치 디렉터리 제어
- GOBIN: 설정 시 바이너리가 여기에 설치됨
- GOPATH: 설정 시 바이너리가 첫 번째 디렉터리의 `bin` 하위 디렉터리에 설치됨
- 기본값: `$HOME/go/bin` 또는 `%USERPROFILE%\go\bin`

#### 환경 변수 설정
```bash
$ go env -w GOBIN=/somewhere/else/bin
$ go env -u GOBIN  # 설정 해제
```

#### 프로그램 실행
```bash
$ export PATH=$PATH:$(dirname $(go list -f '{{.Target}}' .))
$ hello
Hello, world.
```

#### 버전 관리 초기화 (선택사항)
```bash
$ git init
$ git add go.mod hello.go
$ git commit -m "initial commit"
```

---

### 모듈에서 패키지 Import하기

#### morestrings 패키지 생성

`$HOME/hello/morestrings/reverse.go` 생성:

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

참고: 대문자로 시작하는 함수는 export되어 다른 패키지에서 사용 가능.

#### 패키지 컴파일 테스트
```bash
$ cd $HOME/hello/morestrings
$ go build
```

#### hello 프로그램에서 패키지 사용

`$HOME/hello/hello.go` 수정:

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

#### 설치 및 실행
```bash
$ go install example/user/hello
$ hello
Hello, Go!
```

---

### 원격 모듈에서 패키지 Import하기

#### 예제: google/go-cmp 사용

원격 패키지를 import하도록 프로그램 수정:

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

#### 의존성 다운로드
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

#### 설치 및 실행
```bash
$ go install example/user/hello
$ hello
Hello, Go!
  string(
-     "Hello World",
+     "Hello Go",
  )
```

#### 모듈 캐시
- 다운로드된 모듈은 `GOPATH`의 `pkg/mod` 하위 디렉터리에 저장됨
- 해당 버전을 필요로 하는 모든 모듈 간에 공유됨
- 파일은 읽기 전용으로 표시됨
- 캐시 지우기: `go clean -modcache`

---

### 테스트

#### 테스트 프레임워크
- `go test` 명령 사용
- `testing` 패키지 사용
- 테스트 파일은 `_test.go`로 끝남
- 테스트 함수는 `TestXXX`로 이름 지정, 시그니처는 `func (t *testing.T)`

#### reverse_test.go 생성

`$HOME/hello/morestrings/reverse_test.go` 생성:

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

#### 테스트 실행
```bash
$ cd $HOME/hello/morestrings
$ go test
PASS
ok  	example/user/hello/morestrings 0.165s
```

참고: 테스트가 `t.Error` 또는 `t.Fail`을 호출하면 실패로 간주됨.

자세한 내용: `go help test` 및 [testing 패키지 문서](/pkg/testing/)

---

### 다음 단계

1. 메일링 리스트 구독: [golang-announce](https://groups.google.com/group/golang-announce)
2. 읽기: [Effective Go](/doc/effective_go.html) - 명확하고 관용적인 Go 코드 작성 팁
3. 학습: [Go 투어](/tour/) - 언어를 제대로 배우기
4. 탐색: [문서 페이지](/doc/#articles) - Go에 대한 심층 기사

---

### 도움 받기

- 실시간 도움: [gophers Slack 서버](https://gophers.slack.com/messages/general/) ([초대](https://invite.slack.golangbridge.org/))
- 메일링 리스트: [Go Nuts](https://groups.google.com/group/golang-nuts)
- 버그 보고: [Go 이슈 트래커](/issue)

---

## 튜토리얼: 시작하기와 모듈 생성

## 튜토리얼: Go 시작하기

> 원문: https://go.dev/doc/tutorial/getting-started

### 개요
Go 프로그래밍에 대한 간략한 소개. 설치·기본 코드 작성·`go` 명령어 사용·패키지 검색·외부 모듈 함수 호출 등을 다룸.

### 사전 준비 사항
- 프로그래밍 경험 (함수에 대한 이해가 도움됨)
- 코드 에디터 (VSCode·GoLand·Vim 권장)
- 명령 터미널 (Linux/Mac 터미널, 또는 Windows의 PowerShell/cmd)

### 설치
[다운로드 및 설치](/doc/install) 단계 따르기.

### 코드 작성하기

#### 1단계: 프로젝트 디렉터리 생성
```bash
# Linux 또는 Mac에서:
cd

# Windows에서:
cd %HOMEPATH%

# hello 디렉터리 생성
mkdir hello
cd hello
```

#### 2단계: 의존성 추적 활성화
`go mod init` 명령을 실행하여 `go.mod` 파일 생성:

```bash
$ go mod init example/hello
go: creating new go.mod: module example/hello
```

#### 3단계: Hello World 코드 작성
다음 코드로 `hello.go` 파일 생성:

```go
package main

import "fmt"

func main() {
    fmt.Println("Hello, World!")
}
```

코드 설명:
- `main` 패키지 선언 (같은 디렉터리의 함수들을 그룹화)
- `fmt` 패키지 import (텍스트 포맷팅을 위한 표준 라이브러리)
- `main()` 함수 구현 (main 패키지 실행 시 기본으로 실행됨)

#### 4단계: 코드 실행
```bash
$ go run .
Hello, World!
```

`go run` 명령은 프로그램을 컴파일하고 실행함 → 다른 `go` 명령을 보려면 `go help` 사용.

---

### 외부 패키지의 코드 호출하기

#### 1단계: 외부 패키지 찾기
1. [pkg.go.dev](https://pkg.go.dev) 방문
2. "quote" 패키지 검색
3. 검색 결과에서 `rsc.io/quote` v1 확인
4. Documentation 섹션에서 `Go` 함수 확인

#### 2단계: 외부 패키지 Import 및 사용
`hello.go` 수정:

```go
package main

import "fmt"

import "rsc.io/quote"

func main() {
    fmt.Println(quote.Go())
}
```

#### 3단계: 모듈 요구 사항 추가
```bash
$ go mod tidy
go: finding module for package rsc.io/quote
go: found rsc.io/quote in rsc.io/quote v1.5.2
```

이 명령이 하는 일:
- `rsc.io/quote` 모듈을 찾아 다운로드함
- 최신 버전(v1.5.2)을 다운로드함
- 모듈 인증을 위한 `go.sum` 파일을 생성함

#### 4단계: 수정된 코드 실행
```bash
$ go run .
Don't communicate by sharing memory, share memory by communicating.
```

---

### 다음 단계

[Go 모듈 생성하기](/doc/tutorial/create-module.md) 튜토리얼로 학습 계속.


---

## 튜토리얼: Go 모듈 생성하기

> 원문: https://go.dev/doc/tutorial/create-module

### 개요
Go 언어의 기본 기능을 소개하는 튜토리얼의 첫 번째 부분. 라이브러리 모듈과 호출 애플리케이션, 두 개의 모듈을 생성하는 방법을 안내함.

### 튜토리얼 순서
1. 모듈 생성하기 - 다른 모듈에서 호출할 수 있는 함수가 포함된 작은 모듈 작성
2. [다른 모듈에서 코드 호출하기](/doc/tutorial/call-module-code.html)
3. [에러 반환 및 처리하기](/doc/tutorial/handle-errors.html)
4. [임의의 인사말 반환하기](/doc/tutorial/random-greeting.html)
5. [여러 사람에게 인사말 반환하기](/doc/tutorial/greetings-multiple-people.html)
6. [테스트 추가하기](/doc/tutorial/add-a-test.html)
7. [애플리케이션 컴파일 및 설치하기](/doc/tutorial/compile-install.html)

### 사전 준비 사항
- 프로그래밍 경험 - 함수·반복문·배열에 대한 이해
- 코드 에디터 - VSCode(무료)·GoLand(유료)·Vim(무료)
- 명령 터미널 - Linux/Mac 터미널 또는 Windows의 PowerShell/cmd

### 다른 사람이 사용할 수 있는 모듈 시작하기

#### 1단계: 홈 디렉터리로 이동
```bash
# Linux 또는 Mac에서:
cd

# Windows에서:
cd %HOMEPATH%
```

#### 2단계: greetings 디렉터리 생성
```bash
mkdir greetings
cd greetings
```

#### 3단계: 모듈 초기화
```bash
go mod init example.com/greetings
```

출력:
```
go: creating new go.mod: module example.com/greetings
```

`go mod init` 명령은 코드의 의존성과 코드가 지원하는 Go 버전을 추적하는 `go.mod` 파일을 생성함.

#### 4단계: 코드 파일 생성
텍스트 에디터에서 `greetings.go`라는 파일 생성.

#### 5단계: 코드 추가
다음 코드를 `greetings.go`에 붙여넣기:

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

### 코드 설명

이 코드가 하는 일:

1. 패키지 선언 - `package greetings`는 관련 함수들을 모음
2. Hello 함수 구현
   - `string` 타입의 `name` 매개변수를 받음
   - `string`을 반환함
   - 대문자로 시작하는 함수는 export되어 다른 패키지에서 호출 가능

3. 변수 선언 - `message`가 인사말을 담음
   - `:=` 단축 연산자를 사용하여 선언과 초기화를 동시에 수행
   - 다음과 동일함:
     ```go
     var message string
     message = fmt.Sprintf("Hi, %v. Welcome!", name)
     ```

4. fmt.Sprintf 사용 - 포맷된 인사말 메시지를 생성
   - `%v` 포맷 동사에 `name` 매개변수 값을 대체
   - 완성된 인사말 텍스트를 반환

### 핵심 개념

- 모듈은 관련 패키지들을 그룹화하여 독립적이고 유용한 기능을 제공함
- go.mod 파일은 의존성과 Go 버전 요구 사항을 지정함
- Export된 이름 (대문자로 시작)은 다른 패키지에서 호출 가능함
- 모듈 경로 (`example.com/greetings`)는 Go 도구가 다운로드할 수 있어야 함

---

다음 단계: [다른 모듈에서 코드 호출하기](/doc/tutorial/call-module-code.html)

---

## 튜토리얼: 제네릭과 퍼징

## 튜토리얼: 제네릭 시작하기

> 원문: https://go.dev/doc/tutorial/generics

### 개요
Go의 제네릭 기본 사항을 소개하는 튜토리얼. 호출 코드에서 제공하는 타입 집합 중 하나와 작동하는 함수나 타입을 선언하고 사용하는 방법을 보여줌.

### 사전 준비 사항
- Go 1.18 이상
- 코드 에디터
- 명령 터미널

### 튜토리얼 섹션

#### 1. 코드용 폴더 생성

설정 지침:

```bash
# 홈 디렉터리로 이동
$ cd

# generics 폴더 생성
$ mkdir generics
$ cd generics

# Go 모듈 초기화
$ go mod init example/generics
```

#### 2. 비제네릭 함수 추가

다음 코드로 `main.go` 생성:

```go
package main

import "fmt"

// SumInts는 m의 값들을 더합니다.
func SumInts(m map[string]int64) int64 {
    var s int64
    for _, v := range m {
        s += v
    }
    return s
}

// SumFloats는 m의 값들을 더합니다.
func SumFloats(m map[string]float64) float64 {
    var s float64
    for _, v := range m {
        s += v
    }
    return s
}

func main() {
    // 정수 값을 위한 맵 초기화
    ints := map[string]int64{
        "first":  34,
        "second": 12,
    }

    // 부동소수점 값을 위한 맵 초기화
    floats := map[string]float64{
        "first":  35.98,
        "second": 26.99,
    }

    fmt.Printf("Non-Generic Sums: %v and %v\n",
        SumInts(ints),
        SumFloats(floats))
}
```

실행:
```bash
$ go run .
Non-Generic Sums: 46 and 62.97
```

#### 3. 여러 타입을 처리하는 제네릭 함수 추가

다음 제네릭 함수 추가:

```go
// SumIntsOrFloats는 맵 m의 값들을 합산합니다.
// 맵 값으로 int64와 float64를 모두 지원합니다.
func SumIntsOrFloats[K comparable, V int64 | float64](m map[K]V) V {
    var s V
    for _, v := range m {
        s += v
    }
    return s
}
```

핵심 개념:
- 타입 매개변수: `K`와 `V` (대괄호 안에)
- K 제약 조건: `comparable` - `==`와 `!=` 연산자를 사용할 수 있는 모든 타입 허용
- V 제약 조건: `int64 | float64` - int64 또는 float64를 허용하는 유니온 타입

main()을 업데이트:
```go
fmt.Printf("Generic Sums: %v and %v\n",
    SumIntsOrFloats[string, int64](ints),
    SumIntsOrFloats[string, float64](floats))
```

실행:
```bash
$ go run .
Non-Generic Sums: 46 and 62.97
Generic Sums: 46 and 62.97
```

#### 4. 제네릭 함수 호출 시 타입 인수 제거

Go는 함수 인수에서 타입 인수를 추론 가능:

```go
fmt.Printf("Generic Sums, type parameters inferred: %v and %v\n",
    SumIntsOrFloats(ints),
    SumIntsOrFloats(floats))
```

실행:
```bash
$ go run .
Non-Generic Sums: 46 and 62.97
Generic Sums: 46 and 62.97
Generic Sums, type parameters inferred: 46 and 62.97
```

참고: 제네릭 함수에 인수가 없을 때는 타입 인수를 생략할 수 없음.

#### 5. 타입 제약 조건 선언

재사용 가능한 타입 제약 조건을 인터페이스로 선언:

```go
type Number interface {
    int64 | float64
}
```

제약 조건을 사용하는 새 제네릭 함수 추가:

```go
// SumNumbers는 맵 m의 값들을 합산합니다.
// 맵 값으로 정수와 부동소수점을 모두 지원합니다.
func SumNumbers[K comparable, V Number](m map[K]V) V {
    var s V
    for _, v := range m {
        s += v
    }
    return s
}
```

main()을 업데이트:
```go
fmt.Printf("Generic Sums with Constraint: %v and %v\n",
    SumNumbers(ints),
    SumNumbers(floats))
```

실행:
```bash
$ go run .
Non-Generic Sums: 46 and 62.97
Generic Sums: 46 and 62.97
Generic Sums, type parameters inferred: 46 and 62.97
Generic Sums with Constraint: 46 and 62.97
```

### 전체 코드

```go
package main

import "fmt"

type Number interface {
    int64 | float64
}

func main() {
    // 정수 값을 위한 맵 초기화
    ints := map[string]int64{
        "first":  34,
        "second": 12,
    }

    // 부동소수점 값을 위한 맵 초기화
    floats := map[string]float64{
        "first":  35.98,
        "second": 26.99,
    }

    fmt.Printf("Non-Generic Sums: %v and %v\n",
        SumInts(ints),
        SumFloats(floats))

    fmt.Printf("Generic Sums: %v and %v\n",
        SumIntsOrFloats[string, int64](ints),
        SumIntsOrFloats[string, float64](floats))

    fmt.Printf("Generic Sums, type parameters inferred: %v and %v\n",
        SumIntsOrFloats(ints),
        SumIntsOrFloats(floats))

    fmt.Printf("Generic Sums with Constraint: %v and %v\n",
        SumNumbers(ints),
        SumNumbers(floats))
}

// SumInts는 m의 값들을 더합니다.
func SumInts(m map[string]int64) int64 {
    var s int64
    for _, v := range m {
        s += v
    }
    return s
}

// SumFloats는 m의 값들을 더합니다.
func SumFloats(m map[string]float64) float64 {
    var s float64
    for _, v := range m {
        s += v
    }
    return s
}

// SumIntsOrFloats는 맵 m의 값들을 합산합니다.
// 맵 값으로 부동소수점과 정수를 모두 지원합니다.
func SumIntsOrFloats[K comparable, V int64 | float64](m map[K]V) V {
    var s V
    for _, v := range m {
        s += v
    }
    return s
}

// SumNumbers는 맵 m의 값들을 합산합니다.
// 맵 값으로 정수와 부동소수점을 모두 지원합니다.
func SumNumbers[K comparable, V Number](m map[K]V) V {
    var s V
    for _, v := range m {
        s += v
    }
    return s
}
```

### 추천 다음 주제
- [Go 투어](/tour/)
- [Effective Go](/doc/effective_go)
- [Go 코드 작성법](/doc/code)


---

## 튜토리얼: 퍼징 시작하기

> 원문: https://go.dev/doc/tutorial/fuzz

### 개요
Go의 퍼징 기본 사항을 소개하는 튜토리얼. 퍼징은 테스트에 무작위 데이터를 실행 → SQL 인젝션·버퍼 오버플로우·서비스 거부·크로스 사이트 스크립팅 공격과 같은 취약점이나 충돌을 유발하는 입력을 찾음.

### 사전 준비 사항
- Go 1.18 이상 - [설치 지침](/doc/install)
- 텍스트 에디터 - 아무 에디터나 가능
- 명령 터미널 - Linux, Mac, PowerShell, 또는 Windows의 cmd
- AMD64 또는 ARM64 아키텍처 - 커버리지 계측이 포함된 퍼징에 필요

### 튜토리얼 섹션

#### 1. 코드용 폴더 생성

```bash
$ cd
$ mkdir fuzz
$ cd fuzz
$ go mod init example/fuzz
```

#### 2. 테스트할 코드 추가

main.go:
```go
package main

import "fmt"

func main() {
    input := "The quick brown fox jumped over the lazy dog"
    rev := Reverse(input)
    doubleRev := Reverse(rev)
    fmt.Printf("original: %q\n", input)
    fmt.Printf("reversed: %q\n", rev)
    fmt.Printf("reversed again: %q\n", doubleRev)
}

func Reverse(s string) string {
    b := []byte(s)
    for i, j := 0, len(b)-1; i < len(b)/2; i, j = i+1, j-1 {
        b[i], b[j] = b[j], b[i]
    }
    return string(b)
}
```

코드 실행:
```bash
$ go run .
original: "The quick brown fox jumped over the lazy dog"
reversed: "god yzal eht revo depmuj xof nworb kciuq ehT"
reversed again: "The quick brown fox jumped over the lazy dog"
```

#### 3. 단위 테스트 추가

reverse_test.go:
```go
package main

import (
    "testing"
)

func TestReverse(t *testing.T) {
    testcases := []struct {
        in, want string
    }{
        {"Hello, world", "dlrow ,olleH"},
        {" ", " "},
        {"!12345", "54321!"},
    }
    for _, tc := range testcases {
        rev := Reverse(tc.in)
        if rev != tc.want {
                t.Errorf("Reverse: %q, want %q", rev, tc.want)
        }
    }
}
```

테스트 실행:
```bash
$ go test
PASS
ok      example/fuzz  0.013s
```

#### 4. 퍼즈 테스트 추가

FuzzReverse로 변환:
```go
package main

import (
    "testing"
    "unicode/utf8"
)

func FuzzReverse(f *testing.F) {
    testcases := []string{"Hello, world", " ", "!12345"}
    for _, tc := range testcases {
        f.Add(tc)  // f.Add를 사용하여 시드 코퍼스 제공
    }
    f.Fuzz(func(t *testing.T, orig string) {
        rev := Reverse(orig)
        doubleRev := Reverse(rev)
        if orig != doubleRev {
            t.Errorf("Before: %q, after: %q", orig, doubleRev)
        }
        if utf8.ValidString(orig) && !utf8.ValidString(rev) {
            t.Errorf("Reverse produced invalid UTF-8 string %q", rev)
        }
    })
}
```

단위 테스트와의 주요 차이점:
- 함수 시그니처: `TestXxx` 대신 `FuzzXxx`, `*testing.T` 대신 `*testing.F` 사용
- `f.Add()`를 사용하여 시드 코퍼스 입력 제공
- `f.Fuzz()`를 `*testing.T`와 퍼징 매개변수 타입을 받는 대상 함수와 함께 사용
- 예상 출력을 예측할 수 없음 → 대신 함수의 속성을 검증

퍼즈 테스트 실행:
```bash
$ go test
PASS
ok      example/fuzz  0.013s
```

퍼징으로 실행:
```bash
$ go test -fuzz=Fuzz
fuzz: elapsed: 0s, gathering baseline coverage: 0/3 completed
fuzz: elapsed: 0s, gathering baseline coverage: 3/3 completed, now fuzzing with 8 workers
fuzz: minimizing 38-byte failing input file...
--- FAIL: FuzzReverse (0.01s)
    --- FAIL: FuzzReverse (0.00s)
        reverse_test.go:20: Reverse produced invalid UTF-8 string "\x9c\xdd"

    Failing input written to testdata/fuzz/FuzzReverse/af69258a12129d6cbba438df5d5f25ba0ec050461c116f777e77ea7c9a0d217a
```

#### 5. 유효하지 않은 문자열 오류 수정

문제: 바이트 단위 역순이 멀티바이트 UTF-8 문자를 깨뜨림.

해결책: 바이트 대신 룬 단위로 역순 처리.

업데이트된 Reverse 함수:
```go
func Reverse(s string) string {
    r := []rune(s)
    for i, j := 0, len(r)-1; i < len(r)/2; i, j = i+1, j-1 {
        r[i], r[j] = r[j], r[i]
    }
    return string(r)
}
```

테스트 통과:
```bash
$ go test
PASS
ok      example/fuzz  0.016s
```

#### 6. 이중 역순 오류 수정

문제: 함수가 유효하지 않은 UTF-8 입력 문자열을 처리하지 않음.

해결책: 유효하지 않은 UTF-8에 대한 오류 처리 추가.

최종 main.go:
```go
package main

import (
    "errors"
    "fmt"
    "unicode/utf8"
)

func main() {
    input := "The quick brown fox jumped over the lazy dog"
    rev, revErr := Reverse(input)
    doubleRev, doubleRevErr := Reverse(rev)
    fmt.Printf("original: %q\n", input)
    fmt.Printf("reversed: %q, err: %v\n", rev, revErr)
    fmt.Printf("reversed again: %q, err: %v\n", doubleRev, doubleRevErr)
}

func Reverse(s string) (string, error) {
    if !utf8.ValidString(s) {
        return s, errors.New("input is not valid UTF-8")
    }
    r := []rune(s)
    for i, j := 0, len(r)-1; i < len(r)/2; i, j = i+1, j-1 {
        r[i], r[j] = r[j], r[i]
    }
    return string(r), nil
}
```

최종 reverse_test.go:
```go
package main

import (
    "testing"
    "unicode/utf8"
)

func FuzzReverse(f *testing.F) {
    testcases := []string{"Hello, world", " ", "!12345"}
    for _, tc := range testcases {
        f.Add(tc) // f.Add를 사용하여 시드 코퍼스 제공
    }
    f.Fuzz(func(t *testing.T, orig string) {
        rev, err1 := Reverse(orig)
        if err1 != nil {
            return
        }
        doubleRev, err2 := Reverse(rev)
        if err2 != nil {
             return
        }
        if orig != doubleRev {
            t.Errorf("Before: %q, after: %q", orig, doubleRev)
        }
        if utf8.ValidString(orig) && !utf8.ValidString(rev) {
            t.Errorf("Reverse produced invalid UTF-8 string %q", rev)
        }
    })
}
```

시간 제한으로 실행:
```bash
$ go test -fuzz=Fuzz -fuzztime 30s
fuzz: elapsed: 0s, gathering baseline coverage: 0/5 completed
fuzz: elapsed: 0s, gathering baseline coverage: 5/5 completed, now fuzzing with 4 workers
...
fuzz: elapsed: 30s, execs: 1172817 (30281/sec), new interesting: 17 (total: 17)
PASS
ok      example/fuzz  31.025s
```

### 핵심 퍼징 개념

1. 시드 코퍼스: `f.Add()`를 통해 제공되는 초기 테스트 케이스
2. 퍼즈 대상: 무작위 입력을 받는 `f.Fuzz()` 내의 함수
3. 속성 테스트: 정확한 출력 대신 속성(예: 이중 역순 = 원본)을 검증
4. 코퍼스 파일: 실패한 입력이 `testdata/fuzz/{FuzzTestName}/`에 저장됨

### 유용한 플래그

- `-fuzz=Fuzz`: 퍼징 실행
- `-fuzztime`: 퍼징 지속 시간 설정 (예: `-fuzztime 30s`)
- `-run`: 특정 테스트 또는 코퍼스 항목 실행
- `-v`: 상세 출력

### 리소스

- [Go 퍼징 문서](/security/fuzz/)
- [Go의 문자열, 바이트, 룬, 문자](/blog/strings)
- [퍼징 트로피 케이스](/wiki/Fuzzing-trophy-case)
- Gophers Slack의 [#fuzzing 채널](https://gophers.slack.com/archives/CH5KV1AKE)

---

## 튜토리얼: 웹 서비스와 워크스페이스

## 튜토리얼: Go와 Gin을 사용한 RESTful API 개발

> 원문: https://go.dev/doc/tutorial/web-service-gin

### 개요
Go와 Gin 웹 프레임워크를 사용하여 RESTful 웹 서비스 API를 구축하는 방법을 소개하는 튜토리얼. 요청 라우팅·요청 세부 정보 검색·JSON 응답 마샬링을 다룸.

### 사전 준비 사항
- Go 1.16 이상
- 텍스트 에디터
- 명령 터미널
- `curl` 도구

### API 설계

빈티지 음반 가게를 위한 API를 구축하는 튜토리얼. 엔드포인트 구성:

- `/albums`
  - 메서드: GET
  - 설명: 모든 앨범을 JSON으로 가져오기
- `/albums`
  - 메서드: POST
  - 설명: JSON 요청 본문에서 새 앨범 추가
- `/albums/:id`
  - 메서드: GET
  - 설명: ID로 특정 앨범 가져오기

### 단계별 구현

#### 1. 프로젝트 구조 생성

```bash
$ mkdir web-service-gin
$ cd web-service-gin
$ go mod init example/web-service-gin
```

#### 2. 데이터 구조 생성

앨범 구조체와 시드 데이터로 `main.go` 생성:

```go
package main

import (
    "net/http"
    "github.com/gin-gonic/gin"
)

// album은 음반 앨범에 대한 데이터를 나타냅니다.
type album struct {
    ID     string  `json:"id"`
    Title  string  `json:"title"`
    Artist string  `json:"artist"`
    Price  float64 `json:"price"`
}

// albums 슬라이스는 음반 앨범 데이터를 시드합니다.
var albums = []album{
    {ID: "1", Title: "Blue Train", Artist: "John Coltrane", Price: 56.99},
    {ID: "2", Title: "Jeru", Artist: "Gerry Mulligan", Price: 17.99},
    {ID: "3", Title: "Sarah Vaughan and Clifford Brown", Artist: "Sarah Vaughan", Price: 39.99},
}
```

#### 3. 핸들러: 모든 앨범 가져오기

```go
// getAlbums는 모든 앨범 목록을 JSON으로 응답합니다.
func getAlbums(c *gin.Context) {
    c.IndentedJSON(http.StatusOK, albums)
}
```

#### 4. 핸들러: 새 앨범 추가

```go
// postAlbums는 요청 본문에서 받은 JSON으로부터 앨범을 추가합니다.
func postAlbums(c *gin.Context) {
    var newAlbum album

    // BindJSON을 호출하여 받은 JSON을 newAlbum에 바인딩합니다.
    if err := c.BindJSON(&newAlbum); err != nil {
        return
    }

    // 새 앨범을 슬라이스에 추가합니다.
    albums = append(albums, newAlbum)
    c.IndentedJSON(http.StatusCreated, newAlbum)
}
```

#### 5. 핸들러: 특정 앨범 가져오기

```go
// getAlbumByID는 클라이언트가 보낸 id 매개변수와 일치하는
// ID 값을 가진 앨범을 찾아 응답으로 반환합니다.
func getAlbumByID(c *gin.Context) {
    id := c.Param("id")

    // 앨범 목록을 순회하며 매개변수와 일치하는
    // ID 값을 가진 앨범을 찾습니다.
    for _, a := range albums {
        if a.ID == id {
            c.IndentedJSON(http.StatusOK, a)
            return
        }
    }
    c.IndentedJSON(http.StatusNotFound, gin.H{"message": "album not found"})
}
```

#### 6. Main 함수

```go
func main() {
    router := gin.Default()
    router.GET("/albums", getAlbums)
    router.GET("/albums/:id", getAlbumByID)
    router.POST("/albums", postAlbums)

    router.Run("localhost:8080")
}
```

### 서비스 실행

```bash
$ go get .
$ go run .
```

### curl로 테스트

모든 앨범 가져오기:
```bash
$ curl http://localhost:8080/albums
```

새 앨범 추가:
```bash
$ curl http://localhost:8080/albums \
    --include \
    --header "Content-Type: application/json" \
    --request "POST" \
    --data '{"id": "4","title": "The Modern Sound of Betty Carter","artist": "Betty Carter","price": 49.99}'
```

특정 앨범 가져오기:
```bash
$ curl http://localhost:8080/albums/2
```

### 전체 코드

최종 애플리케이션은 위에서 보여준 모든 핸들러와 main 함수를 결합 → 인메모리 저장소로 앨범 레코드를 관리하는 완전히 기능하는 RESTful API를 구현함.

### 핵심 Gin 개념

- gin.Context: 요청 세부 정보를 전달하고, JSON을 검증 및 직렬화함
- Context.IndentedJSON(): 구조체를 포맷된 JSON으로 직렬화함
- Context.BindJSON(): 요청 본문 JSON을 구조체에 바인딩함
- Context.Param(): 경로 매개변수를 검색함
- router.GET/POST: HTTP 메서드와 경로를 핸들러에 매핑함
- :id 표기법: 라우트에서 경로 매개변수를 정의함


---

## 튜토리얼: 멀티 모듈 워크스페이스 시작하기

> 원문: https://go.dev/doc/tutorial/workspaces

### 개요
Go의 멀티 모듈 워크스페이스의 기본 사항을 소개하는 튜토리얼. 멀티 모듈 워크스페이스를 사용하면 여러 모듈에서 동시에 코드를 작성하고 있다고 Go 명령에 알림 → 해당 모듈의 코드를 쉽게 빌드하고 실행 가능해짐.

### 사전 준비 사항
- Go 1.18 이상 (필수)
- 코드 에디터 (아무 텍스트 에디터나 가능)
- 명령 터미널 (Linux/Mac 터미널 또는 Windows의 PowerShell/cmd)

### 1단계: 코드용 모듈 생성

#### 설정
1. 명령 프롬프트를 열고 홈 디렉터리로 이동:
   ```bash
   $ cd
   ```

2. 워크스페이스 디렉터리 생성:
   ```bash
   $ mkdir workspace
   $ cd workspace
   ```

#### 모듈 초기화
1. `hello` 모듈 생성 및 초기화:
   ```bash
   $ mkdir hello
   $ cd hello
   $ go mod init example.com/hello
   ```

2. `golang.org/x/example/hello/reverse`에 대한 의존성 추가:
   ```bash
   $ go get golang.org/x/example/hello/reverse
   ```

3. `hello.go` 생성:
   ```go
   package main

   import (
       "fmt"

       "golang.org/x/example/hello/reverse"
   )

   func main() {
       fmt.Println(reverse.String("Hello"))
   }
   ```

4. 프로그램 실행:
   ```bash
   $ go run .
   olleH
   ```

### 2단계: 워크스페이스 생성

#### 워크스페이스 초기화
`workspace` 디렉터리에서 다음 실행:
```bash
$ go work init ./hello
```

다음 내용의 `go.work` 파일 생성됨:
```
go 1.18

use ./hello
```

주요 지시문:
- `go 1.18` - 해석을 위한 Go 버전을 지정함
- `use ./hello` - hello 모듈을 워크스페이스의 메인 모듈로 지정함

#### 워크스페이스에서 프로그램 실행
```bash
$ go run ./hello
olleH
```

Go 명령은 워크스페이스의 모든 모듈을 메인 모듈로 포함 → 모듈 디렉터리 외부에서도 코드 실행 가능.

### 3단계: 예제 모듈 다운로드 및 수정

#### 저장소 복제
워크스페이스 디렉터리에서:
```bash
$ git clone https://go.googlesource.com/example
```

#### 워크스페이스에 모듈 추가
```bash
$ go work use ./example/hello
```

`go.work` 파일이 이제 다음과 같이 됨:
```
go 1.18

use (
    ./hello
    ./example/hello
)
```

#### 새 함수 추가
`workspace/example/hello/reverse/int.go` 생성:
```go
package reverse

import "strconv"

// Int는 정수 i의 십진수 역순을 반환합니다.
func Int(i int) int {
    i, _ = strconv.Atoi(String(strconv.Itoa(i)))
    return i
}
```

#### Hello 프로그램 업데이트
`workspace/hello/hello.go` 수정:
```go
package main

import (
    "fmt"

    "golang.org/x/example/hello/reverse"
)

func main() {
    fmt.Println(reverse.String("Hello"), reverse.Int(24601))
}
```

#### 업데이트된 코드 실행
```bash
$ go run ./hello
olleH 10642
```

### 워크스페이스 명령어

`go` 명령이 워크스페이스 관리를 위해 제공하는 하위 명령:

- `go work init [dir]`: 모듈로 새 go.work 파일 생성
- `go work use [-r] [dir]`: 모듈 디렉터리에 대한 `use` 지시문 추가/제거
  - `-r`은 하위 디렉터리를 재귀적으로 검색
- `go work edit`: go.work 파일 편집 (`go mod edit`와 유사)
- `go work sync`: 워크스페이스의 빌드 목록에서 각 워크스페이스 모듈로 의존성 동기화

### 릴리스 워크플로우 참고

이러한 모듈을 적절히 릴리스하는 방법:
1. 모듈의 버전 관리 저장소에서 커밋에 태그를 지정
2. `hello/go.mod`의 모듈 요구 사항을 업데이트:
   ```bash
   cd hello
   go get golang.org/x/example/hello@v0.1.0
   ```

자세한 내용은 [모듈 릴리스 워크플로우 문서](/doc/modules/release-workflow) 참조.

### 핵심 요점

- `go.work`는 `replace` 지시문 없이 여러 모듈에서 작업 가능하게 함
- 워크스페이스의 모든 모듈은 빌드 중 메인 모듈로 처리됨
- 워크스페이스 모듈의 로컬 변경 사항은 의존 모듈에 즉시 반영됨
- 워크스페이스 명령은 멀티 모듈 프로젝트 관리를 단순화함
