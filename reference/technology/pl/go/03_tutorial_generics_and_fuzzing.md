# 튜토리얼: 제네릭과 퍼징

# 튜토리얼: 제네릭 시작하기

> **원문:** https://go.dev/doc/tutorial/generics

## 개요
이 튜토리얼은 Go의 제네릭 기본 사항을 소개합니다. 호출 코드에서 제공하는 타입 집합 중 하나와 작동하는 함수나 타입을 선언하고 사용하는 방법을 보여줍니다.

## 사전 준비 사항
- Go 1.18 이상
- 코드 에디터
- 명령 터미널

## 튜토리얼 섹션

### 1. 코드용 폴더 생성

**설정 지침:**

```bash
# 홈 디렉터리로 이동
$ cd

# generics 폴더 생성
$ mkdir generics
$ cd generics

# Go 모듈 초기화
$ go mod init example/generics
```

### 2. 비제네릭 함수 추가

다음 코드로 `main.go`를 생성합니다:

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

**실행:**
```bash
$ go run .
Non-Generic Sums: 46 and 62.97
```

### 3. 여러 타입을 처리하는 제네릭 함수 추가

다음 제네릭 함수를 추가합니다:

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

**핵심 개념:**
- **타입 매개변수**: `K`와 `V` (대괄호 안에)
- **K 제약 조건**: `comparable` - `==`와 `!=` 연산자를 사용할 수 있는 모든 타입 허용
- **V 제약 조건**: `int64 | float64` - int64 또는 float64를 허용하는 유니온 타입

main()을 업데이트합니다:
```go
fmt.Printf("Generic Sums: %v and %v\n",
    SumIntsOrFloats[string, int64](ints),
    SumIntsOrFloats[string, float64](floats))
```

**실행:**
```bash
$ go run .
Non-Generic Sums: 46 and 62.97
Generic Sums: 46 and 62.97
```

### 4. 제네릭 함수 호출 시 타입 인수 제거

Go는 함수 인수에서 타입 인수를 추론할 수 있습니다:

```go
fmt.Printf("Generic Sums, type parameters inferred: %v and %v\n",
    SumIntsOrFloats(ints),
    SumIntsOrFloats(floats))
```

**실행:**
```bash
$ go run .
Non-Generic Sums: 46 and 62.97
Generic Sums: 46 and 62.97
Generic Sums, type parameters inferred: 46 and 62.97
```

**참고:** 제네릭 함수에 인수가 없을 때는 타입 인수를 생략할 수 없습니다.

### 5. 타입 제약 조건 선언

재사용 가능한 타입 제약 조건을 인터페이스로 선언합니다:

```go
type Number interface {
    int64 | float64
}
```

제약 조건을 사용하는 새 제네릭 함수를 추가합니다:

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

main()을 업데이트합니다:
```go
fmt.Printf("Generic Sums with Constraint: %v and %v\n",
    SumNumbers(ints),
    SumNumbers(floats))
```

**실행:**
```bash
$ go run .
Non-Generic Sums: 46 and 62.97
Generic Sums: 46 and 62.97
Generic Sums, type parameters inferred: 46 and 62.97
Generic Sums with Constraint: 46 and 62.97
```

## 전체 코드

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

## 추천 다음 주제
- [Go 투어](/tour/)
- [Effective Go](/doc/effective_go)
- [Go 코드 작성법](/doc/code)


---

# 튜토리얼: 퍼징 시작하기

> **원문:** https://go.dev/doc/tutorial/fuzz

## 개요
이 튜토리얼은 Go의 퍼징 기본 사항을 소개합니다. 퍼징은 테스트에 무작위 데이터를 실행하여 SQL 인젝션, 버퍼 오버플로우, 서비스 거부, 크로스 사이트 스크립팅 공격과 같은 취약점이나 충돌을 유발하는 입력을 찾습니다.

## 사전 준비 사항
- **Go 1.18 이상** - [설치 지침](/doc/install)
- **텍스트 에디터** - 아무 에디터나 가능
- **명령 터미널** - Linux, Mac, PowerShell, 또는 Windows의 cmd
- **AMD64 또는 ARM64 아키텍처** - 커버리지 계측이 포함된 퍼징에 필요

## 튜토리얼 섹션

### 1. 코드용 폴더 생성

```bash
$ cd
$ mkdir fuzz
$ cd fuzz
$ go mod init example/fuzz
```

### 2. 테스트할 코드 추가

**main.go:**
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

**코드 실행:**
```bash
$ go run .
original: "The quick brown fox jumped over the lazy dog"
reversed: "god yzal eht revo depmuj xof nworb kciuq ehT"
reversed again: "The quick brown fox jumped over the lazy dog"
```

### 3. 단위 테스트 추가

**reverse_test.go:**
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

**테스트 실행:**
```bash
$ go test
PASS
ok      example/fuzz  0.013s
```

### 4. 퍼즈 테스트 추가

**FuzzReverse로 변환:**
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

**단위 테스트와의 주요 차이점:**
- 함수 시그니처: `TestXxx` 대신 `FuzzXxx`, `*testing.T` 대신 `*testing.F` 사용
- `f.Add()`를 사용하여 시드 코퍼스 입력 제공
- `f.Fuzz()`를 `*testing.T`와 퍼징 매개변수 타입을 받는 대상 함수와 함께 사용
- 예상 출력을 예측할 수 없음; 대신 함수의 속성을 검증

**퍼즈 테스트 실행:**
```bash
$ go test
PASS
ok      example/fuzz  0.013s
```

**퍼징으로 실행:**
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

### 5. 유효하지 않은 문자열 오류 수정

**문제:** 바이트 단위 역순이 멀티바이트 UTF-8 문자를 깨뜨립니다.

**해결책:** 바이트 대신 룬 단위로 역순 처리합니다.

**업데이트된 Reverse 함수:**
```go
func Reverse(s string) string {
    r := []rune(s)
    for i, j := 0, len(r)-1; i < len(r)/2; i, j = i+1, j-1 {
        r[i], r[j] = r[j], r[i]
    }
    return string(r)
}
```

**테스트 통과:**
```bash
$ go test
PASS
ok      example/fuzz  0.016s
```

### 6. 이중 역순 오류 수정

**문제:** 함수가 유효하지 않은 UTF-8 입력 문자열을 처리하지 않습니다.

**해결책:** 유효하지 않은 UTF-8에 대한 오류 처리를 추가합니다.

**최종 main.go:**
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

**최종 reverse_test.go:**
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

**시간 제한으로 실행:**
```bash
$ go test -fuzz=Fuzz -fuzztime 30s
fuzz: elapsed: 0s, gathering baseline coverage: 0/5 completed
fuzz: elapsed: 0s, gathering baseline coverage: 5/5 completed, now fuzzing with 4 workers
...
fuzz: elapsed: 30s, execs: 1172817 (30281/sec), new interesting: 17 (total: 17)
PASS
ok      example/fuzz  31.025s
```

## 핵심 퍼징 개념

1. **시드 코퍼스:** `f.Add()`를 통해 제공되는 초기 테스트 케이스
2. **퍼즈 대상:** 무작위 입력을 받는 `f.Fuzz()` 내의 함수
3. **속성 테스트:** 정확한 출력 대신 속성(예: 이중 역순 = 원본)을 검증
4. **코퍼스 파일:** 실패한 입력이 `testdata/fuzz/{FuzzTestName}/`에 저장됨

## 유용한 플래그

- `-fuzz=Fuzz`: 퍼징 실행
- `-fuzztime`: 퍼징 지속 시간 설정 (예: `-fuzztime 30s`)
- `-run`: 특정 테스트 또는 코퍼스 항목 실행
- `-v`: 상세 출력

## 리소스

- [Go 퍼징 문서](/security/fuzz/)
- [Go의 문자열, 바이트, 룬, 문자](/blog/strings)
- [퍼징 트로피 케이스](/wiki/Fuzzing-trophy-case)
- Gophers Slack의 [#fuzzing 채널](https://gophers.slack.com/archives/CH5KV1AKE)

