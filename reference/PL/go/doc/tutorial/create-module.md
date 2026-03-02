# 튜토리얼: Go 모듈 생성하기

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
