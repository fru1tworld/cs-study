# 튜토리얼: 멀티 모듈 워크스페이스 시작하기

## 개요
이 튜토리얼은 Go의 멀티 모듈 워크스페이스의 기본 사항을 소개합니다. 멀티 모듈 워크스페이스를 사용하면 여러 모듈에서 동시에 코드를 작성하고 있다고 Go 명령에 알리고, 해당 모듈의 코드를 쉽게 빌드하고 실행할 수 있습니다.

## 사전 준비 사항
- **Go 1.18 이상** (필수)
- **코드 에디터** (아무 텍스트 에디터나 가능)
- **명령 터미널** (Linux/Mac 터미널 또는 Windows의 PowerShell/cmd)

## 1단계: 코드용 모듈 생성

### 설정
1. 명령 프롬프트를 열고 홈 디렉터리로 이동합니다:
   ```bash
   $ cd
   ```

2. 워크스페이스 디렉터리를 생성합니다:
   ```bash
   $ mkdir workspace
   $ cd workspace
   ```

### 모듈 초기화
1. `hello` 모듈을 생성하고 초기화합니다:
   ```bash
   $ mkdir hello
   $ cd hello
   $ go mod init example.com/hello
   ```

2. `golang.org/x/example/hello/reverse`에 대한 의존성을 추가합니다:
   ```bash
   $ go get golang.org/x/example/hello/reverse
   ```

3. `hello.go`를 생성합니다:
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

4. 프로그램을 실행합니다:
   ```bash
   $ go run .
   olleH
   ```

## 2단계: 워크스페이스 생성

### 워크스페이스 초기화
`workspace` 디렉터리에서 다음을 실행합니다:
```bash
$ go work init ./hello
```

이렇게 하면 다음 내용의 `go.work` 파일이 생성됩니다:
```
go 1.18

use ./hello
```

**주요 지시문:**
- `go 1.18` - 해석을 위한 Go 버전을 지정합니다
- `use ./hello` - hello 모듈을 워크스페이스의 메인 모듈로 지정합니다

### 워크스페이스에서 프로그램 실행
```bash
$ go run ./hello
olleH
```

Go 명령은 워크스페이스의 모든 모듈을 메인 모듈로 포함하므로, 모듈 디렉터리 외부에서도 코드를 실행할 수 있습니다.

## 3단계: 예제 모듈 다운로드 및 수정

### 저장소 복제
워크스페이스 디렉터리에서:
```bash
$ git clone https://go.googlesource.com/example
```

### 워크스페이스에 모듈 추가
```bash
$ go work use ./example/hello
```

`go.work` 파일이 이제 다음과 같이 됩니다:
```
go 1.18

use (
    ./hello
    ./example/hello
)
```

### 새 함수 추가
`workspace/example/hello/reverse/int.go`를 생성합니다:
```go
package reverse

import "strconv"

// Int는 정수 i의 십진수 역순을 반환합니다.
func Int(i int) int {
    i, _ = strconv.Atoi(String(strconv.Itoa(i)))
    return i
}
```

### Hello 프로그램 업데이트
`workspace/hello/hello.go`를 수정합니다:
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

### 업데이트된 코드 실행
```bash
$ go run ./hello
olleH 10642
```

## 워크스페이스 명령어

`go` 명령은 워크스페이스 관리를 위해 다음 하위 명령을 제공합니다:

| 명령어 | 목적 |
|--------|------|
| `go work init [dir]` | 모듈로 새 go.work 파일 생성 |
| `go work use [-r] [dir]` | 모듈 디렉터리에 대한 `use` 지시문 추가/제거; `-r`은 하위 디렉터리를 재귀적으로 검색 |
| `go work edit` | go.work 파일 편집 (`go mod edit`와 유사) |
| `go work sync` | 워크스페이스의 빌드 목록에서 각 워크스페이스 모듈로 의존성 동기화 |

## 릴리스 워크플로우 참고

이러한 모듈을 적절히 릴리스하려면:
1. 모듈의 버전 관리 저장소에서 커밋에 태그를 지정합니다
2. `hello/go.mod`의 모듈 요구 사항을 업데이트합니다:
   ```bash
   cd hello
   go get golang.org/x/example/hello@v0.1.0
   ```

자세한 내용은 [모듈 릴리스 워크플로우 문서](/doc/modules/release-workflow)를 참조하세요.

## 핵심 요점

- `go.work`는 `replace` 지시문 없이 여러 모듈에서 작업할 수 있게 합니다
- 워크스페이스의 모든 모듈은 빌드 중 메인 모듈로 처리됩니다
- 워크스페이스 모듈의 로컬 변경 사항은 의존 모듈에 즉시 반영됩니다
- 워크스페이스 명령은 멀티 모듈 프로젝트 관리를 단순화합니다
