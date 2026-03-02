# 튜토리얼: 관계형 데이터베이스 접근

## 개요

이 튜토리얼은 Go와 표준 라이브러리의 `database/sql` 패키지를 사용하여 관계형 데이터베이스에 접근하는 기본 사항을 소개합니다. 예제 프로젝트는 빈티지 재즈 레코드에 대한 데이터 저장소입니다.

## 사전 요구 사항

- **MySQL 관계형 데이터베이스 관리 시스템(DBMS)** 설치
- **Go 설치** - [Go 설치](/doc/install) 참조
- **텍스트 편집기** - 코드 편집용
- **명령 터미널** (Linux, Mac의 터미널, Windows의 PowerShell 또는 cmd)

## 튜토리얼 섹션

### 1. 코드를 위한 폴더 생성

```bash
$ cd
$ mkdir data-access
$ cd data-access
$ go mod init example/data-access
```

이 명령은 의존성 추적을 위한 `go.mod` 파일을 생성합니다.

### 2. 데이터베이스 설정

앨범 테이블이 있는 MySQL 데이터베이스를 생성합니다:

```sql
DROP TABLE IF EXISTS album;
CREATE TABLE album (
  id         INT AUTO_INCREMENT NOT NULL,
  title      VARCHAR(128) NOT NULL,
  artist     VARCHAR(255) NOT NULL,
  price      DECIMAL(5,2) NOT NULL,
  PRIMARY KEY (`id`)
);

INSERT INTO album
  (title, artist, price)
VALUES
  ('Blue Train', 'John Coltrane', 56.99),
  ('Giant Steps', 'John Coltrane', 63.99),
  ('Jeru', 'Gerry Mulligan', 17.99),
  ('Sarah Vaughan', 'Sarah Vaughan', 34.98);
```

### 3. 데이터베이스 드라이버 임포트

Go-MySQL-Driver(`github.com/go-sql-driver/mysql`)를 사용합니다:

```go
package main

import "github.com/go-sql-driver/mysql"
```

### 4. 데이터베이스 핸들 가져오기 및 연결

```go
package main

import (
    "database/sql"
    "fmt"
    "log"
    "os"

    "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    // 연결 속성을 캡처합니다.
    cfg := mysql.NewConfig()
    cfg.User = os.Getenv("DBUSER")
    cfg.Passwd = os.Getenv("DBPASS")
    cfg.Net = "tcp"
    cfg.Addr = "127.0.0.1:3306"
    cfg.DBName = "recordings"

    // 데이터베이스 핸들을 가져옵니다.
    var err error
    db, err = sql.Open("mysql", cfg.FormatDSN())
    if err != nil {
        log.Fatal(err)
    }

    pingErr := db.Ping()
    if pingErr != nil {
        log.Fatal(pingErr)
    }
    fmt.Println("Connected!")
}
```

**코드 실행:**
```bash
$ go get .
$ export DBUSER=username
$ export DBPASS=password
$ go run .
Connected!
```

### 5. 여러 행 쿼리

`Album` 구조체와 쿼리 함수를 정의합니다:

```go
type Album struct {
    ID     int64
    Title  string
    Artist string
    Price  float32
}

// albumsByArtist는 지정된 아티스트 이름을 가진 앨범을 쿼리합니다.
func albumsByArtist(name string) ([]Album, error) {
    // 반환된 행의 데이터를 담을 albums 슬라이스.
    var albums []Album

    rows, err := db.Query("SELECT * FROM album WHERE artist = ?", name)
    if err != nil {
        return nil, fmt.Errorf("albumsByArtist %q: %v", name, err)
    }
    defer rows.Close()
    // 행을 순회하면서 Scan을 사용하여 컬럼 데이터를 구조체 필드에 할당합니다.
    for rows.Next() {
        var alb Album
        if err := rows.Scan(&alb.ID, &alb.Title, &alb.Artist, &alb.Price); err != nil {
            return nil, fmt.Errorf("albumsByArtist %q: %v", name, err)
        }
        albums = append(albums, alb)
    }
    if err := rows.Err(); err != nil {
        return nil, fmt.Errorf("albumsByArtist %q: %v", name, err)
    }
    return albums, nil
}
```

`main()`에 추가:
```go
albums, err := albumsByArtist("John Coltrane")
if err != nil {
    log.Fatal(err)
}
fmt.Printf("Albums found: %v\n", albums)
```

**출력:**
```
Connected!
Albums found: [{1 Blue Train John Coltrane 56.99} {2 Giant Steps John Coltrane 63.99}]
```

### 6. 단일 행 쿼리

```go
// albumByID는 지정된 ID를 가진 앨범을 쿼리합니다.
func albumByID(id int64) (Album, error) {
    // 반환된 행의 데이터를 담을 album.
    var alb Album

    row := db.QueryRow("SELECT * FROM album WHERE id = ?", id)
    if err := row.Scan(&alb.ID, &alb.Title, &alb.Artist, &alb.Price); err != nil {
        if err == sql.ErrNoRows {
            return alb, fmt.Errorf("albumsById %d: no such album", id)
        }
        return alb, fmt.Errorf("albumsById %d: %v", id, err)
    }
    return alb, nil
}
```

`main()`에 추가:
```go
// 쿼리를 테스트하기 위해 ID 2를 하드코딩합니다.
alb, err := albumByID(2)
if err != nil {
    log.Fatal(err)
}
fmt.Printf("Album found: %v\n", alb)
```

**출력:**
```
Album found: {2 Giant Steps John Coltrane 63.99}
```

### 7. 데이터 추가

```go
// addAlbum은 지정된 앨범을 데이터베이스에 추가하고
// 새 항목의 앨범 ID를 반환합니다.
func addAlbum(alb Album) (int64, error) {
    result, err := db.Exec("INSERT INTO album (title, artist, price) VALUES (?, ?, ?)", alb.Title, alb.Artist, alb.Price)
    if err != nil {
        return 0, fmt.Errorf("addAlbum: %v", err)
    }
    id, err := result.LastInsertId()
    if err != nil {
        return 0, fmt.Errorf("addAlbum: %v", err)
    }
    return id, nil
}
```

`main()`에 추가:
```go
albID, err := addAlbum(Album{
    Title:  "The Modern Sound of Betty Carter",
    Artist: "Betty Carter",
    Price:  49.99,
})
if err != nil {
    log.Fatal(err)
}
fmt.Printf("ID of added album: %v\n", albID)
```

**출력:**
```
ID of added album: 5
```

## 전체 코드

```go
package main

import (
    "database/sql"
    "fmt"
    "log"
    "os"

    "github.com/go-sql-driver/mysql"
)

var db *sql.DB

type Album struct {
    ID     int64
    Title  string
    Artist string
    Price  float32
}

func main() {
    // 연결 속성을 캡처합니다.
    cfg := mysql.NewConfig()
    cfg.User = os.Getenv("DBUSER")
    cfg.Passwd = os.Getenv("DBPASS")
    cfg.Net = "tcp"
    cfg.Addr = "127.0.0.1:3306"
    cfg.DBName = "recordings"

    // 데이터베이스 핸들을 가져옵니다.
    var err error
    db, err = sql.Open("mysql", cfg.FormatDSN())
    if err != nil {
        log.Fatal(err)
    }

    pingErr := db.Ping()
    if pingErr != nil {
        log.Fatal(pingErr)
    }
    fmt.Println("Connected!")

    albums, err := albumsByArtist("John Coltrane")
    if err != nil {
        log.Fatal(err)
    }
    fmt.Printf("Albums found: %v\n", albums)

    // 쿼리를 테스트하기 위해 ID 2를 하드코딩합니다.
    alb, err := albumByID(2)
    if err != nil {
        log.Fatal(err)
    }
    fmt.Printf("Album found: %v\n", alb)

    albID, err := addAlbum(Album{
        Title:  "The Modern Sound of Betty Carter",
        Artist: "Betty Carter",
        Price:  49.99,
    })
    if err != nil {
        log.Fatal(err)
    }
    fmt.Printf("ID of added album: %v\n", albID)
}

// albumsByArtist는 지정된 아티스트 이름을 가진 앨범을 쿼리합니다.
func albumsByArtist(name string) ([]Album, error) {
    // 반환된 행의 데이터를 담을 albums 슬라이스.
    var albums []Album

    rows, err := db.Query("SELECT * FROM album WHERE artist = ?", name)
    if err != nil {
        return nil, fmt.Errorf("albumsByArtist %q: %v", name, err)
    }
    defer rows.Close()
    // 행을 순회하면서 Scan을 사용하여 컬럼 데이터를 구조체 필드에 할당합니다.
    for rows.Next() {
        var alb Album
        if err := rows.Scan(&alb.ID, &alb.Title, &alb.Artist, &alb.Price); err != nil {
            return nil, fmt.Errorf("albumsByArtist %q: %v", name, err)
        }
        albums = append(albums, alb)
    }
    if err := rows.Err(); err != nil {
        return nil, fmt.Errorf("albumsByArtist %q: %v", name, err)
    }
    return albums, nil
}

// albumByID는 지정된 ID를 가진 앨범을 쿼리합니다.
func albumByID(id int64) (Album, error) {
    // 반환된 행의 데이터를 담을 album.
    var alb Album

    row := db.QueryRow("SELECT * FROM album WHERE id = ?", id)
    if err := row.Scan(&alb.ID, &alb.Title, &alb.Artist, &alb.Price); err != nil {
        if err == sql.ErrNoRows {
            return alb, fmt.Errorf("albumsById %d: no such album", id)
        }
        return alb, fmt.Errorf("albumsById %d: %v", id, err)
    }
    return alb, nil
}

// addAlbum은 지정된 앨범을 데이터베이스에 추가하고
// 새 항목의 앨범 ID를 반환합니다.
func addAlbum(alb Album) (int64, error) {
    result, err := db.Exec("INSERT INTO album (title, artist, price) VALUES (?, ?, ?)", alb.Title, alb.Artist, alb.Price)
    if err != nil {
        return 0, fmt.Errorf("addAlbum: %v", err)
    }
    id, err := result.LastInsertId()
    if err != nil {
        return 0, fmt.Errorf("addAlbum: %v", err)
    }
    return id, nil
}
```

## 핵심 요점

- **`sql.Open()`**은 데이터베이스 핸들을 초기화합니다
- **`db.Ping()`**은 연결을 확인합니다
- **`db.Query()`**는 여러 행을 반환합니다; `rows.Next()`와 `rows.Scan()`을 사용합니다
- **`db.QueryRow()`**는 단일 행을 반환합니다
- **`db.Exec()`**는 데이터를 반환하지 않는 문을 실행합니다
- **매개변수화된 쿼리**(`?` 사용)는 SQL 인젝션을 방지합니다
- 리소스를 해제하기 위해 `defer rows.Close()`를 사용합니다
