# 데이터베이스 접근

# 튜토리얼: 관계형 데이터베이스 접근

> **원문:** https://go.dev/doc/tutorial/database-access

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

- `sql.Open()`은 데이터베이스 핸들을 초기화합니다
- `db.Ping()`은 연결을 확인합니다
- `db.Query()`는 여러 행을 반환합니다; `rows.Next()`와 `rows.Scan()`을 사용합니다
- `db.QueryRow()`는 단일 행을 반환합니다
- `db.Exec()`는 데이터를 반환하지 않는 문을 실행합니다
- **매개변수화된 쿼리**(`?` 사용)는 SQL 인젝션을 방지합니다
- 리소스를 해제하기 위해 `defer rows.Close()`를 사용합니다


---

# 관계형 데이터베이스 접근

> **원문:** https://go.dev/doc/database

## 개요

Go를 사용하면 다양한 데이터베이스와 데이터 접근 방식을 애플리케이션에 통합할 수 있습니다. 표준 라이브러리의 [`database/sql`](https://pkg.go.dev/database/sql) 패키지는 관계형 데이터베이스에 접근하는 데 사용됩니다.

소개 튜토리얼은 [튜토리얼: 관계형 데이터베이스 접근](/doc/tutorial/database-access)을 참조하세요.

## 대체 데이터 접근 기술

Go는 표준 `database/sql` 패키지 외에도 추가적인 데이터 접근 기술을 지원합니다:

### 객체-관계 매핑(ORM) 라이브러리
`database/sql`이 저수준 데이터 접근 로직을 제공하는 반면, 더 높은 수준의 추상화도 사용할 수 있습니다:
- **[GORM](https://gorm.io/index.html)** - ([패키지 레퍼런스](https://pkg.go.dev/gorm.io/gorm))
- **[ent](https://entgo.io/)** - ([패키지 레퍼런스](https://pkg.go.dev/entgo.io/ent))

### NoSQL 데이터 저장소
Go 커뮤니티는 대부분의 NoSQL 데이터 저장소를 위한 드라이버를 개발했습니다:
- **[MongoDB](https://docs.mongodb.com/drivers/go/)**
- **[Couchbase](https://docs.couchbase.com/go-sdk/current/hello-world/overview.html)**

추가 드라이버는 [pkg.go.dev](https://pkg.go.dev/)에서 찾을 수 있습니다.

## 지원되는 데이터베이스 관리 시스템

Go는 모든 일반적인 관계형 데이터베이스 관리 시스템을 지원합니다:
- MySQL
- Oracle
- PostgreSQL
- SQL Server
- SQLite
- 기타

전체 드라이버 목록은 [SQLDrivers](/wiki/SQLDrivers) 페이지에서 확인할 수 있습니다.

## 주요 기능 및 주제

### 쿼리 실행 또는 데이터베이스 변경을 위한 함수

`database/sql` 패키지는 특수화된 함수를 제공합니다:
- **`Query`** / **`QueryRow`** - 쿼리 실행
  - `QueryRow`는 단일 행 결과에 최적화되어 오버헤드를 줄임
- **`Exec`** - `INSERT`, `UPDATE`, 또는 `DELETE` 문으로 데이터베이스 변경

관련 문서:
- [데이터를 반환하지 않는 SQL 문 실행](/doc/database/change-data)
- [데이터 쿼리](/doc/database/querying)

### 트랜잭션

`sql.Tx`를 통해 트랜잭션으로 데이터베이스 작업을 실행할 수 있습니다:
- 여러 작업이 함께 실행됨
- **커밋**(모든 변경 사항을 원자적으로 적용) 또는 **롤백**(변경 사항 폐기)으로 종료

자세한 내용은 [트랜잭션 실행](/doc/database/execute-transactions)을 참조하세요.

### 쿼리 취소

`context.Context`를 사용하여 데이터베이스 작업을 취소합니다:
- 클라이언트 연결이 닫힐 때 취소
- 작업이 원하는 시간을 초과하면 취소
- database/sql 함수는 `Context`를 인수로 받음
- 작업에 대한 타임아웃 또는 마감 시간 지정
- 리소스를 해제하기 위해 취소 요청을 전파

[진행 중인 작업 취소](/doc/database/cancel-operations)를 참조하세요.

### 관리되는 연결 풀

`sql.DB` 데이터베이스 핸들에는 필요에 따라 자동으로 연결을 생성하고 폐기하는 내장 연결 풀이 포함되어 있습니다.

**기본 사용:**
- `sql.DB`는 Go로 데이터베이스에 접근하는 가장 일반적인 방법
- [데이터베이스 핸들 열기](/doc/database/open-handle) 참조

**고급 구성:**
- 연결 풀 속성을 수동으로 구성
- [연결 풀 속성 설정](/doc/database/manage-connections#connection_pool_properties) 참조

**전용 단일 연결:**
- 트랜잭션이 부적절할 때 [`sql.Conn`](https://pkg.go.dev/database/sql#Conn) 사용

**전용 연결 사용 사례:**
- 커스텀 트랜잭션 시맨틱으로 DDL을 통한 스키마 변경
- 임시 테이블을 생성하는 쿼리 잠금 작업 수행

자세한 내용은 [전용 연결 사용](/doc/database/manage-connections#dedicated_connections)을 참조하세요.


---

# 데이터베이스 핸들 열기

> **원문:** https://go.dev/doc/database/open-handle

## 개요

[`database/sql`](https://pkg.go.dev/database/sql) 패키지는 연결을 관리할 필요성을 줄여 데이터베이스 접근을 단순화합니다. 많은 데이터 접근 API와 달리 `database/sql`에서는 연결을 명시적으로 열고, 작업을 수행하고, 연결을 닫지 않습니다. 대신 코드는 연결 풀을 나타내는 데이터베이스 핸들을 열고, 핸들로 데이터 접근 작업을 실행하며, 검색된 행이나 준비된 문에 의해 보유된 리소스와 같이 리소스를 해제해야 할 때만 `Close` 메서드를 호출합니다.

[`sql.DB`](https://pkg.go.dev/database/sql#DB)로 표현되는 데이터베이스 핸들은 코드를 대신하여 연결을 처리하고, 열고 닫습니다. 코드가 핸들을 사용하여 데이터베이스 작업을 실행하면 해당 작업은 데이터베이스에 대한 동시 접근을 갖습니다.

**참고:** 데이터베이스 연결을 예약할 수도 있습니다. 자세한 내용은 [전용 연결 사용](/doc/database/manage-connections#dedicated_connections)을 참조하세요.

## 상위 수준 단계

데이터베이스 핸들을 열 때 다음 단계를 따릅니다:

1. **드라이버 찾기** - 드라이버는 Go 코드와 데이터베이스 간의 요청과 응답을 변환합니다
2. **데이터베이스 핸들 열기** - 드라이버를 임포트한 후 특정 데이터베이스에 대한 핸들을 엽니다
3. **연결 확인** - 연결이 설정될 수 있는지 확인합니다

## 데이터베이스 드라이버 찾기 및 임포트

사용하는 DBMS를 지원하는 데이터베이스 드라이버가 필요합니다. 데이터베이스용 드라이버를 찾으려면 [SQLDrivers](/wiki/SQLDrivers)를 참조하세요.

드라이버를 코드에서 사용 가능하게 하려면 다른 Go 패키지처럼 임포트합니다:

```go
import "github.com/go-sql-driver/mysql"
```

드라이버 패키지에서 직접 함수를 호출하지 않는 경우 블랭크 임포트를 사용합니다:

```go
import _ "github.com/go-sql-driver/mysql"
```

**모범 사례:** 데이터베이스 작업에 데이터베이스 드라이버 자체의 API 사용을 피합니다. 대신 `database/sql` 패키지의 함수를 사용하여 코드를 DBMS와 느슨하게 결합시킵니다.

## 데이터베이스 핸들 열기

`sql.DB` 데이터베이스 핸들은 개별적으로 또는 트랜잭션으로 데이터베이스에서 읽고 쓰는 기능을 제공합니다.

다음 중 하나를 호출하여 데이터베이스 핸들을 얻을 수 있습니다:
- `sql.Open` (연결 문자열을 받음)
- `sql.OpenDB` (`driver.Connector`를 받음)

둘 다 [`sql.DB`](https://pkg.go.dev/database/sql#DB)에 대한 포인터를 반환합니다.

**참고:** 데이터베이스 자격 증명을 Go 소스 코드에서 제외하세요.

### 연결 문자열로 열기

연결 문자열을 사용하여 연결하려면 [`sql.Open` 함수](https://pkg.go.dev/database/sql#Open)를 사용합니다.

**MySQL 예시:**

```go
db, err = sql.Open("mysql", "username:password@tcp(127.0.0.1:3306)/jazzrecords")
if err != nil {
    log.Fatal(err)
}
```

**MySQL 드라이버 Config를 사용한 구조화된 접근 방식:**

```go
// 연결 속성을 지정합니다.
cfg := mysql.NewConfig()
cfg.User = username
cfg.Passwd = password
cfg.Net = "tcp"
cfg.Addr = "127.0.0.1:3306"
cfg.DBName = "jazzrecords"

// 데이터베이스 핸들을 가져옵니다.
db, err = sql.Open("mysql", cfg.FormatDSN())
if err != nil {
    log.Fatal(err)
}
```

### Connector로 열기

연결 문자열에서 사용할 수 없는 드라이버별 연결 기능을 활용하려면 [`sql.OpenDB 함수`](https://pkg.go.dev/database/sql#OpenDB)를 사용합니다.

**예시:**

```go
// 연결 속성을 지정합니다.
cfg := mysql.NewConfig()
cfg.User = username
cfg.Passwd = password
cfg.Net = "tcp"
cfg.Addr = "127.0.0.1:3306"
cfg.DBName = "jazzrecords"

// 드라이버별 connector를 가져옵니다.
connector, err := mysql.NewConnector(&cfg)
if err != nil {
    log.Fatal(err)
}

// 데이터베이스 핸들을 가져옵니다.
db = sql.OpenDB(connector)
```

### 오류 처리

코드는 `sql.Open`의 오류를 확인해야 합니다. 이것은 연결 오류가 아니라 `sql.Open`이 핸들을 초기화할 수 없는 경우의 오류입니다(예: DSN을 파싱할 수 없는 경우).

## 연결 확인

데이터베이스 핸들을 열 때 `sql` 패키지는 즉시 새 데이터베이스 연결을 생성하지 않을 수 있습니다. 연결이 설정될 수 있는지 확인하려면 [`Ping`](https://pkg.go.dev/database/sql#DB.Ping) 또는 [`PingContext`](https://pkg.go.dev/database/sql#DB.PingContext)를 호출합니다.

**예시:**

```go
db, err = sql.Open("mysql", connString)

// 성공적인 연결을 확인합니다.
if err := db.Ping(); err != nil {
    log.Fatal(err)
}
```

## 데이터베이스 자격 증명 저장

데이터베이스 자격 증명을 Go 소스 코드에 저장하지 마세요. 대신 코드 외부의 위치에 저장합니다:
- 자격 증명을 저장하고 API를 제공하는 비밀 관리 앱
- 비밀 관리자에서 로드되는 환경 변수

**환경 변수 사용 예시:**

```go
username := os.Getenv("DB_USER")
password := os.Getenv("DB_PASS")
```

이 접근 방식은 로컬 테스트를 위해 환경 변수를 직접 설정할 수도 있게 합니다.

## 리소스 해제

`database/sql` 패키지로 연결을 명시적으로 관리하거나 닫지 않지만, 코드는 더 이상 필요하지 않을 때 획득한 리소스를 해제해야 합니다:
- 쿼리에서 반환된 데이터를 나타내는 `sql.Rows`
- 준비된 문을 나타내는 `sql.Stmt`

일반적으로 `Close` 함수 호출을 defer하여 둘러싸는 함수가 종료되기 전에 리소스가 해제되도록 합니다.

**예시:**

```go
rows, err := db.Query("SELECT * FROM album WHERE artist = ?", artist)
if err != nil {
    log.Fatal(err)
}
defer rows.Close()

// 반환된 행을 순회합니다.
```


---

# 데이터 쿼리

> **원문:** https://go.dev/doc/database/querying

## 개요

데이터를 반환하는 SQL 문을 실행할 때 `database/sql` 패키지에서 제공하는 `Query` 메서드를 사용합니다. 각 메서드는 `Scan` 메서드를 사용하여 데이터를 변수에 복사할 수 있는 `Row` 또는 `Rows`를 반환합니다. 이러한 메서드는 `SELECT` 문을 실행합니다.

데이터를 반환하지 않는 문에는 대신 `Exec` 또는 `ExecContext` 메서드를 사용합니다.

`database/sql` 패키지는 결과를 위한 쿼리를 실행하는 두 가지 방법을 제공합니다:

- **단일 행 쿼리** – `QueryRow`는 데이터베이스에서 최대 단일 `Row`를 반환합니다
- **여러 행 쿼리** – `Query`는 코드가 순회할 수 있는 `Rows` 구조체로 모든 일치하는 행을 반환합니다

### 준비된 문
코드가 동일한 SQL 문을 반복적으로 실행하는 경우 준비된 문 사용을 고려하세요.

### 보안 경고
**SQL 문을 조립하기 위해 `fmt.Sprintf`와 같은 문자열 포매팅 함수를 사용하지 마세요!** 이것은 SQL 인젝션 위험을 초래합니다.

---

## 단일 행 쿼리

`QueryRow`는 최대 단일 데이터베이스 행을 검색합니다(예: 고유 ID로 데이터를 조회할 때). 여러 행이 반환되면 `Scan`은 첫 번째를 제외한 모두를 폐기합니다.

`QueryRowContext`는 `QueryRow`처럼 작동하지만 `context.Context` 인수를 받습니다.

### 예시
```go
func canPurchase(id int, quantity int) (bool, error) {
    var enough bool
    // 단일 행을 기반으로 값을 쿼리합니다.
    if err := db.QueryRow("SELECT (quantity >= ?) from album where id = ?",
        quantity, id).Scan(&enough); err != nil {
        if err == sql.ErrNoRows {
            return false, fmt.Errorf("canPurchase %d: unknown album", id)
        }
        return false, fmt.Errorf("canPurchase %d: %v", id, err)
    }
    return enough, nil
}
```

### 오류 처리
`QueryRow`는 자체적으로 오류를 반환하지 않습니다. 대신 `Scan`이 결합된 조회 및 스캔의 모든 오류를 보고합니다. 쿼리가 행을 찾지 못하면 `sql.ErrNoRows`를 반환합니다.

### 매개변수 플레이스홀더 참고
매개변수 플레이스홀더는 DBMS와 드라이버에 따라 다릅니다. 예를 들어 Postgres용 pq 드라이버는 `?` 대신 `$1`을 필요로 합니다.

### 단일 행 반환 함수

| 함수 | 설명 |
|------|------|
| `DB.QueryRow` / `DB.QueryRowContext` | 독립적으로 단일 행 쿼리 실행 |
| `Tx.QueryRow` / `Tx.QueryRowContext` | 트랜잭션 내에서 단일 행 쿼리 실행 |
| `Stmt.QueryRow` / `Stmt.QueryRowContext` | 준비된 문을 사용하여 단일 행 쿼리 실행 |
| `Conn.QueryRowContext` | 예약된 연결과 함께 사용 |

---

## 여러 행 쿼리

`Query` 또는 `QueryContext`를 사용하여 여러 행을 쿼리하면 쿼리 결과를 나타내는 `Rows`가 반환됩니다. `Rows.Next`를 사용하여 반환된 행을 반복합니다. 각 반복은 `Scan`을 호출하여 컬럼 값을 변수에 복사합니다.

`QueryContext`는 `Query`처럼 작동하지만 `context.Context` 인수를 받습니다.

### 예시
```go
func albumsByArtist(artist string) ([]Album, error) {
    rows, err := db.Query("SELECT * FROM album WHERE artist = ?", artist)
    if err != nil {
        return nil, err
    }
    defer rows.Close()

    // 반환된 행의 데이터를 담을 album 슬라이스.
    var albums []Album

    // 행을 순회하면서 Scan을 사용하여 컬럼 데이터를 구조체 필드에 할당합니다.
    for rows.Next() {
        var alb Album
        if err := rows.Scan(&alb.ID, &alb.Title, &alb.Artist,
            &alb.Price, &alb.Quantity); err != nil {
            return albums, err
        }
        albums = append(albums, alb)
    }
    if err = rows.Err(); err != nil {
        return albums, err
    }
    return albums, nil
}
```

### 중요 참고 사항
- 리소스를 해제하기 위해 항상 `rows.Close()`를 defer합니다
- 결과를 순회한 후 `sql.Rows`의 오류를 확인합니다

### 여러 행 반환 함수

| 함수 | 설명 |
|------|------|
| `DB.Query` / `DB.QueryContext` | 독립적으로 쿼리 실행 |
| `Tx.Query` / `Tx.QueryContext` | 트랜잭션 내에서 쿼리 실행 |
| `Stmt.Query` / `Stmt.QueryContext` | 준비된 문을 사용하여 쿼리 실행 |
| `Conn.QueryContext` | 예약된 연결과 함께 사용 |

---

## Nullable 컬럼 값 처리

`database/sql` 패키지는 컬럼 값이 null일 수 있을 때 `Scan` 함수를 위한 특수 타입을 제공합니다. 각 타입에는 non-null인 경우 값을 담는 필드와 `Valid` 필드가 포함됩니다.

### 예시
```go
var s sql.NullString
err := db.QueryRow("SELECT name FROM customer WHERE id = ?", id).Scan(&s)
if err != nil {
    log.Fatal(err)
}

// 고객 이름을 찾고, 없으면 플레이스홀더를 사용합니다.
name := "Valued Customer"
if s.Valid {
    name = s.String
}
```

### Null 타입 옵션
- `NullBool`
- `NullFloat64`
- `NullInt32`
- `NullInt64`
- `NullString`
- `NullTime`

---

## 컬럼에서 데이터 가져오기

행을 순회할 때 `Scan`을 사용하여 컬럼 값을 Go 값에 복사합니다. 모든 드라이버는 기본 변환(예: SQL `INT`에서 Go `int`로)을 지원하며, 일부 드라이버는 이러한 변환을 확장합니다.

### 변환 동작
- `Scan`은 유사한 타입을 변환합니다(예: SQL `CHAR`, `VARCHAR`, `TEXT`에서 Go `string`으로)
- `Scan`은 호환되는 Go 타입으로도 변환할 수 있습니다(예: `strconv.Atoi`를 사용하여 숫자를 포함하는 `VARCHAR`에서 `int`로)

---

## 여러 결과 집합 처리

데이터베이스 작업이 여러 결과 집합을 반환할 때 `Rows.NextResultSet`을 사용합니다. 이것은 여러 테이블을 별도로 쿼리하는 SQL을 보낼 때 유용합니다.

`Rows.NextResultSet`은 다음 결과 집합을 준비하고 다음 결과 집합이 있는지 나타내는 boolean을 반환합니다.

### 예시
```go
rows, err := db.Query("SELECT * from album; SELECT * from song;")
if err != nil {
    log.Fatal(err)
}
defer rows.Close()

// 첫 번째 결과 집합을 순회합니다.
for rows.Next() {
    // 결과 집합을 처리합니다.
}

// 다음 결과 집합으로 이동합니다.
rows.NextResultSet()

// 두 번째 결과 집합을 순회합니다.
for rows.Next() {
    // 두 번째 집합을 처리합니다.
}

// 두 결과 집합의 오류를 확인합니다.
if err := rows.Err(); err != nil {
    log.Fatal(err)
}
```


---

# 연결 관리

> **원문:** https://go.dev/doc/database/manage-connections

## 개요

대부분의 프로그램에서는 `sql.DB` 연결 풀의 기본값을 조정할 필요가 없습니다. 그러나 일부 고급 프로그램에서는 연결 풀 매개변수를 튜닝하거나 연결을 명시적으로 다루어야 할 수 있습니다.

[`sql.DB`](https://pkg.go.dev/database/sql#DB) 데이터베이스 핸들은 여러 고루틴에서 동시에 안전하게 사용할 수 있습니다(다른 언어에서 "스레드 안전(thread-safe)"이라고 부르는 것과 동일합니다). 일부 다른 데이터베이스 접근 라이브러리는 한 번에 하나의 작업에만 사용할 수 있는 연결을 기반으로 합니다. 이러한 차이를 해소하기 위해 각 `sql.DB`는 기본 데이터베이스에 대한 활성 연결 풀을 관리하며, Go 프로그램에서 병렬 처리를 위해 필요에 따라 새 연결을 생성합니다.

연결 풀은 대부분의 데이터 접근 요구에 적합합니다. `sql.DB`의 `Query` 또는 `Exec` 메서드를 호출하면 `sql.DB` 구현은 풀에서 사용 가능한 연결을 검색하거나, 필요한 경우 새 연결을 생성합니다. 패키지는 연결이 더 이상 필요하지 않을 때 풀로 반환합니다. 이를 통해 데이터베이스 접근의 높은 수준의 병렬 처리를 지원합니다.

---

## 연결 풀 속성 설정 {#connection_pool_properties}

`sql` 패키지가 연결 풀을 관리하는 방식을 안내하는 속성을 설정할 수 있습니다. 이러한 속성의 효과에 대한 통계를 얻으려면 [`DB.Stats`](https://pkg.go.dev/database/sql#DB.Stats)를 사용합니다.

### 최대 열린 연결 수 설정

[`DB.SetMaxOpenConns`](https://pkg.go.dev/database/sql#DB.SetMaxOpenConns)는 열린 연결 수에 제한을 둡니다. 이 제한을 초과하면 새 데이터베이스 작업은 기존 작업이 완료될 때까지 대기하며, 그때 `sql.DB`가 다른 연결을 생성합니다. 기본적으로 `sql.DB`는 연결이 필요할 때 기존 연결이 모두 사용 중이면 새 연결을 생성합니다.

**경고:** 제한을 설정하면 데이터베이스 사용이 잠금(lock)이나 세마포어(semaphore)를 획득하는 것과 유사해지며, 그 결과 애플리케이션이 새 데이터베이스 연결을 기다리며 교착 상태(deadlock)에 빠질 수 있습니다.

### 최대 유휴 연결 수 설정

[`DB.SetMaxIdleConns`](https://pkg.go.dev/database/sql#DB.SetMaxIdleConns)는 `sql.DB`가 유지하는 최대 유휴 연결(idle connection) 수의 제한을 변경합니다.

SQL 작업이 주어진 데이터베이스 연결에서 완료되면 일반적으로 즉시 종료되지 않습니다. 애플리케이션이 곧 다시 필요로 할 수 있으며, 열린 연결을 유지하면 다음 작업을 위해 데이터베이스에 다시 연결하는 것을 피할 수 있기 때문입니다. 기본적으로 `sql.DB`는 임의의 순간에 두 개의 유휴 연결을 유지합니다. 제한을 높이면 상당한 병렬 처리가 있는 프로그램에서 빈번한 재연결을 피할 수 있습니다.

### 연결의 최대 유휴 시간 설정

[`DB.SetConnMaxIdleTime`](https://pkg.go.dev/database/sql#DB.SetConnMaxIdleTime)은 연결이 닫히기 전에 유휴 상태로 있을 수 있는 최대 시간을 설정합니다. 이로 인해 `sql.DB`는 주어진 기간보다 오래 유휴 상태였던 연결을 닫습니다.

기본적으로 유휴 연결이 연결 풀에 추가되면 다시 필요할 때까지 그곳에 유지됩니다. `DB.SetMaxIdleConns`를 사용하여 병렬 활동의 폭주(burst) 중에 허용되는 유휴 연결 수를 늘리는 경우, `DB.SetConnMaxIdleTime`도 함께 사용하여 시스템이 조용해질 때 해당 연결을 나중에 해제하도록 배치할 수 있습니다.

### 연결의 최대 수명 설정

[`DB.SetConnMaxLifetime`](https://pkg.go.dev/database/sql#DB.SetConnMaxLifetime)을 사용하면 연결이 닫히기 전에 열린 상태로 유지될 수 있는 최대 시간을 설정합니다.

기본적으로 연결은 위에서 설명한 제한에 따라 임의의 긴 시간 동안 사용하고 재사용할 수 있습니다. 로드 밸런싱된 데이터베이스 서버를 사용하는 것과 같은 일부 시스템에서는 애플리케이션이 재연결 없이 특정 연결을 너무 오래 사용하지 않도록 하는 것이 도움이 될 수 있습니다.

**핵심 포인트:**
- `SetMaxOpenConns` — 열린 연결의 최대 수를 제한합니다
- `SetMaxIdleConns` — 유휴 연결의 최대 수를 제한합니다 (기본값: 2)
- `SetConnMaxIdleTime` — 유휴 연결의 최대 유휴 시간을 설정합니다
- `SetConnMaxLifetime` — 연결의 최대 수명을 설정합니다

---

## 전용 연결 사용 {#dedicated_connections}

`database/sql` 패키지에는 데이터베이스가 특정 연결에서 실행되는 일련의 작업에 암묵적인 의미를 부여할 수 있을 때 사용하는 함수들이 포함되어 있습니다.

가장 일반적인 예는 트랜잭션(transaction)으로, 일반적으로 `BEGIN` 명령으로 시작하여 `COMMIT` 또는 `ROLLBACK` 명령으로 종료하며, 해당 명령 사이에 연결에서 발행된 모든 명령을 전체 트랜잭션에 포함합니다. 이 사용 사례에 대해서는 `sql` 패키지의 트랜잭션 지원을 사용하세요. [트랜잭션 실행](/doc/database/execute-transactions)을 참조하세요.

일련의 개별 작업이 모두 동일한 연결에서 실행되어야 하는 다른 사용 사례의 경우, `sql` 패키지는 전용 연결을 제공합니다. [`DB.Conn`](https://pkg.go.dev/database/sql#DB.Conn)은 전용 연결인 [`sql.Conn`](https://pkg.go.dev/database/sql#Conn)을 가져옵니다. `sql.Conn`은 `BeginTx`, `ExecContext`, `PingContext`, `PrepareContext`, `QueryContext`, `QueryRowContext` 메서드를 가지며, 이들은 DB의 동일한 메서드처럼 동작하지만 전용 연결만 사용합니다. 전용 연결 사용이 완료되면 `Conn.Close`를 사용하여 해제해야 합니다.


---

# 데이터를 반환하지 않는 SQL 문 실행

> **원문:** https://go.dev/doc/database/change-data

## 개요

데이터를 반환하지 않는 데이터베이스 작업을 수행할 때 `database/sql` 패키지의 `Exec` 또는 `ExecContext` 메서드를 사용합니다. 이 방법으로 실행하는 SQL 문에는 `INSERT`, `DELETE`, `UPDATE`가 포함됩니다.

**핵심 사항:** 쿼리가 행을 반환할 수 있는 경우 대신 `Query` 또는 `QueryContext` 메서드를 사용합니다. 자세한 내용은 [데이터베이스 쿼리](/doc/database/querying)를 참조하세요.

## ExecContext 메서드

`ExecContext` 메서드는 `Exec` 메서드처럼 작동하지만 [진행 중인 작업 취소](/doc/database/cancel-operations)에서 설명된 대로 추가 `context.Context` 인수를 받습니다.

## 코드 예시

다음 예시는 [`DB.Exec`](https://pkg.go.dev/database/sql#DB.Exec)를 사용하여 `album` 테이블에 새 레코드 앨범을 추가하는 문을 실행합니다:

```go
func AddAlbum(alb Album) (int64, error) {
    result, err := db.Exec("INSERT INTO album (title, artist) VALUES (?, ?)", alb.Title, alb.Artist)
    if err != nil {
        return 0, fmt.Errorf("AddAlbum: %v", err)
    }

    // 클라이언트를 위해 새 앨범의 생성된 ID를 가져옵니다.
    id, err := result.LastInsertId()
    if err != nil {
        return 0, fmt.Errorf("AddAlbum: %v", err)
    }
    // 새 앨범의 ID를 반환합니다.
    return id, nil
}
```

## 반환 값

`DB.Exec`는 값을 반환합니다: [`sql.Result`](https://pkg.go.dev/database/sql#Result)와 오류. 오류가 `nil`이면 `Result`를 사용하여:
- 마지막으로 삽입된 항목의 ID를 가져올 수 있습니다(예시처럼)
- 작업에 영향을 받은 행 수를 검색할 수 있습니다

## 중요 참고 사항

**매개변수 플레이스홀더:** 준비된 문의 매개변수 플레이스홀더는 사용하는 DBMS와 드라이버에 따라 다릅니다. 예를 들어 Postgres용 [pq 드라이버](https://pkg.go.dev/github.com/lib/pq)는 `?` 대신 `$1`과 같은 플레이스홀더를 필요로 합니다.

**준비된 문:** 코드가 동일한 SQL 문을 반복적으로 실행하는 경우 SQL 문에서 재사용 가능한 준비된 문을 생성하기 위해 `sql.Stmt` 사용을 고려하세요. 자세한 내용은 [준비된 문 사용](/doc/database/prepared-statements)을 참조하세요.

**보안 경고:** SQL 문을 조립하기 위해 `fmt.Sprintf`와 같은 문자열 포매팅 함수를 사용하지 마세요! SQL 인젝션 위험을 초래할 수 있습니다. 자세한 내용은 [SQL 인젝션 위험 피하기](/doc/database/sql-injection)를 참조하세요.

## 행을 반환하지 않는 SQL 문 실행 함수

| 함수 | 설명 |
|------|------|
| `DB.Exec` `DB.ExecContext` | 독립적으로 단일 SQL 문을 실행합니다. |
| `Tx.Exec` `Tx.ExecContext` | 더 큰 트랜잭션 내에서 SQL 문을 실행합니다. 자세한 내용은 [트랜잭션 실행](/doc/database/execute-transactions)을 참조하세요. |
| `Stmt.Exec` `Stmt.ExecContext` | 이미 준비된 SQL 문을 실행합니다. 자세한 내용은 [준비된 문 사용](/doc/database/prepared-statements)을 참조하세요. |
| `Conn.ExecContext` | 예약된 연결과 함께 사용합니다. 자세한 내용은 [연결 관리](/doc/database/manage-connections)를 참조하세요. |

