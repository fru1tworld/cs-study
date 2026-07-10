# 튜토리얼: 웹 서비스와 워크스페이스

# 튜토리얼: Go와 Gin을 사용한 RESTful API 개발

> **원문:** https://go.dev/doc/tutorial/web-service-gin

## 개요
이 튜토리얼은 Go와 Gin 웹 프레임워크를 사용하여 RESTful 웹 서비스 API를 구축하는 방법을 소개합니다. 요청 라우팅, 요청 세부 정보 검색, JSON 응답 마샬링을 다룹니다.

## 사전 준비 사항
- Go 1.16 이상
- 텍스트 에디터
- 명령 터미널
- `curl` 도구

## API 설계

이 튜토리얼은 빈티지 음반 가게를 위한 API를 구축하며, 다음 엔드포인트를 포함합니다:

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/albums` | GET | 모든 앨범을 JSON으로 가져오기 |
| `/albums` | POST | JSON 요청 본문에서 새 앨범 추가 |
| `/albums/:id` | GET | ID로 특정 앨범 가져오기 |

## 단계별 구현

### 1. 프로젝트 구조 생성

```bash
$ mkdir web-service-gin
$ cd web-service-gin
$ go mod init example/web-service-gin
```

### 2. 데이터 구조 생성

앨범 구조체와 시드 데이터로 `main.go`를 생성합니다:

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

### 3. 핸들러: 모든 앨범 가져오기

```go
// getAlbums는 모든 앨범 목록을 JSON으로 응답합니다.
func getAlbums(c *gin.Context) {
    c.IndentedJSON(http.StatusOK, albums)
}
```

### 4. 핸들러: 새 앨범 추가

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

### 5. 핸들러: 특정 앨범 가져오기

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

### 6. Main 함수

```go
func main() {
    router := gin.Default()
    router.GET("/albums", getAlbums)
    router.GET("/albums/:id", getAlbumByID)
    router.POST("/albums", postAlbums)

    router.Run("localhost:8080")
}
```

## 서비스 실행

```bash
$ go get .
$ go run .
```

## curl로 테스트

**모든 앨범 가져오기:**
```bash
$ curl http://localhost:8080/albums
```

**새 앨범 추가:**
```bash
$ curl http://localhost:8080/albums \
    --include \
    --header "Content-Type: application/json" \
    --request "POST" \
    --data '{"id": "4","title": "The Modern Sound of Betty Carter","artist": "Betty Carter","price": 49.99}'
```

**특정 앨범 가져오기:**
```bash
$ curl http://localhost:8080/albums/2
```

## 전체 코드

최종 애플리케이션은 위에서 보여준 모든 핸들러와 main 함수를 결합하여, 인메모리 저장소로 앨범 레코드를 관리하는 완전히 기능하는 RESTful API를 구현합니다.

## 핵심 Gin 개념

- **gin.Context**: 요청 세부 정보를 전달하고, JSON을 검증 및 직렬화합니다
- **Context.IndentedJSON()**: 구조체를 포맷된 JSON으로 직렬화합니다
- **Context.BindJSON()**: 요청 본문 JSON을 구조체에 바인딩합니다
- **Context.Param()**: 경로 매개변수를 검색합니다
- **router.GET/POST**: HTTP 메서드와 경로를 핸들러에 매핑합니다
- **:id 표기법**: 라우트에서 경로 매개변수를 정의합니다


---

# 튜토리얼: 멀티 모듈 워크스페이스 시작하기

> **원문:** https://go.dev/doc/tutorial/workspaces

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

