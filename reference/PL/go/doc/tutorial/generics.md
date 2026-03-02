# 튜토리얼: 제네릭 시작하기

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
