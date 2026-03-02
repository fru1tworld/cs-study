# 튜토리얼: 퍼징 시작하기

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
