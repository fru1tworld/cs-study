# 튜토리얼: Go와 Gin을 사용한 RESTful API 개발

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
