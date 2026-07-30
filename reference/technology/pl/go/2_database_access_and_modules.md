# Go 데이터베이스 접근과 모듈

## 데이터베이스 접근

## 튜토리얼: 관계형 데이터베이스 접근

> **원문:** https://go.dev/doc/tutorial/database-access

### 개요

이 튜토리얼은 Go와 표준 라이브러리의 `database/sql` 패키지를 사용하여 관계형 데이터베이스에 접근하는 기본 사항을 소개합니다. 예제 프로젝트는 빈티지 재즈 레코드에 대한 데이터 저장소입니다.

### 사전 요구 사항

- **MySQL 관계형 데이터베이스 관리 시스템(DBMS)** 설치
- **Go 설치** - [Go 설치](/doc/install) 참조
- **텍스트 편집기** - 코드 편집용
- **명령 터미널** (Linux, Mac의 터미널, Windows의 PowerShell 또는 cmd)

### 튜토리얼 섹션

#### 1. 코드를 위한 폴더 생성

```bash
$ cd
$ mkdir data-access
$ cd data-access
$ go mod init example/data-access
```

이 명령은 의존성 추적을 위한 `go.mod` 파일을 생성합니다.

#### 2. 데이터베이스 설정

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

#### 3. 데이터베이스 드라이버 임포트

Go-MySQL-Driver(`github.com/go-sql-driver/mysql`)를 사용합니다:

```go
package main

import "github.com/go-sql-driver/mysql"
```

#### 4. 데이터베이스 핸들 가져오기 및 연결

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

#### 5. 여러 행 쿼리

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

#### 6. 단일 행 쿼리

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

#### 7. 데이터 추가

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

### 전체 코드

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

### 핵심 요점

- `sql.Open()`은 데이터베이스 핸들을 초기화합니다
- `db.Ping()`은 연결을 확인합니다
- `db.Query()`는 여러 행을 반환합니다; `rows.Next()`와 `rows.Scan()`을 사용합니다
- `db.QueryRow()`는 단일 행을 반환합니다
- `db.Exec()`는 데이터를 반환하지 않는 문을 실행합니다
- **매개변수화된 쿼리**(`?` 사용)는 SQL 인젝션을 방지합니다
- 리소스를 해제하기 위해 `defer rows.Close()`를 사용합니다


---

## 관계형 데이터베이스 접근

> **원문:** https://go.dev/doc/database

### 개요

Go를 사용하면 다양한 데이터베이스와 데이터 접근 방식을 애플리케이션에 통합할 수 있습니다. 표준 라이브러리의 [`database/sql`](https://pkg.go.dev/database/sql) 패키지는 관계형 데이터베이스에 접근하는 데 사용됩니다.

소개 튜토리얼은 [튜토리얼: 관계형 데이터베이스 접근](/doc/tutorial/database-access)을 참조하세요.

### 대체 데이터 접근 기술

Go는 표준 `database/sql` 패키지 외에도 추가적인 데이터 접근 기술을 지원합니다:

#### 객체-관계 매핑(ORM) 라이브러리
`database/sql`이 저수준 데이터 접근 로직을 제공하는 반면, 더 높은 수준의 추상화도 사용할 수 있습니다:
- **[GORM](https://gorm.io/index.html)** - ([패키지 레퍼런스](https://pkg.go.dev/gorm.io/gorm))
- **[ent](https://entgo.io/)** - ([패키지 레퍼런스](https://pkg.go.dev/entgo.io/ent))

#### NoSQL 데이터 저장소
Go 커뮤니티는 대부분의 NoSQL 데이터 저장소를 위한 드라이버를 개발했습니다:
- **[MongoDB](https://docs.mongodb.com/drivers/go/)**
- **[Couchbase](https://docs.couchbase.com/go-sdk/current/hello-world/overview.html)**

추가 드라이버는 [pkg.go.dev](https://pkg.go.dev/)에서 찾을 수 있습니다.

### 지원되는 데이터베이스 관리 시스템

Go는 모든 일반적인 관계형 데이터베이스 관리 시스템을 지원합니다:
- MySQL
- Oracle
- PostgreSQL
- SQL Server
- SQLite
- 기타

전체 드라이버 목록은 [SQLDrivers](/wiki/SQLDrivers) 페이지에서 확인할 수 있습니다.

### 주요 기능 및 주제

#### 쿼리 실행 또는 데이터베이스 변경을 위한 함수

`database/sql` 패키지는 특수화된 함수를 제공합니다:
- **`Query`** / **`QueryRow`** - 쿼리 실행
  - `QueryRow`는 단일 행 결과에 최적화되어 오버헤드를 줄임
- **`Exec`** - `INSERT`, `UPDATE`, 또는 `DELETE` 문으로 데이터베이스 변경

관련 문서:
- [데이터를 반환하지 않는 SQL 문 실행](/doc/database/change-data)
- [데이터 쿼리](/doc/database/querying)

#### 트랜잭션

`sql.Tx`를 통해 트랜잭션으로 데이터베이스 작업을 실행할 수 있습니다:
- 여러 작업이 함께 실행됨
- **커밋**(모든 변경 사항을 원자적으로 적용) 또는 **롤백**(변경 사항 폐기)으로 종료

자세한 내용은 [트랜잭션 실행](/doc/database/execute-transactions)을 참조하세요.

#### 쿼리 취소

`context.Context`를 사용하여 데이터베이스 작업을 취소합니다:
- 클라이언트 연결이 닫힐 때 취소
- 작업이 원하는 시간을 초과하면 취소
- database/sql 함수는 `Context`를 인수로 받음
- 작업에 대한 타임아웃 또는 마감 시간 지정
- 리소스를 해제하기 위해 취소 요청을 전파

[진행 중인 작업 취소](/doc/database/cancel-operations)를 참조하세요.

#### 관리되는 연결 풀

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

## 데이터베이스 핸들 열기

> **원문:** https://go.dev/doc/database/open-handle

### 개요

[`database/sql`](https://pkg.go.dev/database/sql) 패키지는 연결을 관리할 필요성을 줄여 데이터베이스 접근을 단순화합니다. 많은 데이터 접근 API와 달리 `database/sql`에서는 연결을 명시적으로 열고, 작업을 수행하고, 연결을 닫지 않습니다. 대신 코드는 연결 풀을 나타내는 데이터베이스 핸들을 열고, 핸들로 데이터 접근 작업을 실행하며, 검색된 행이나 준비된 문에 의해 보유된 리소스와 같이 리소스를 해제해야 할 때만 `Close` 메서드를 호출합니다.

[`sql.DB`](https://pkg.go.dev/database/sql#DB)로 표현되는 데이터베이스 핸들은 코드를 대신하여 연결을 처리하고, 열고 닫습니다. 코드가 핸들을 사용하여 데이터베이스 작업을 실행하면 해당 작업은 데이터베이스에 대한 동시 접근을 갖습니다.

**참고:** 데이터베이스 연결을 예약할 수도 있습니다. 자세한 내용은 [전용 연결 사용](/doc/database/manage-connections#dedicated_connections)을 참조하세요.

### 상위 수준 단계

데이터베이스 핸들을 열 때 다음 단계를 따릅니다:

1. **드라이버 찾기** - 드라이버는 Go 코드와 데이터베이스 간의 요청과 응답을 변환합니다
2. **데이터베이스 핸들 열기** - 드라이버를 임포트한 후 특정 데이터베이스에 대한 핸들을 엽니다
3. **연결 확인** - 연결이 설정될 수 있는지 확인합니다

### 데이터베이스 드라이버 찾기 및 임포트

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

### 데이터베이스 핸들 열기

`sql.DB` 데이터베이스 핸들은 개별적으로 또는 트랜잭션으로 데이터베이스에서 읽고 쓰는 기능을 제공합니다.

다음 중 하나를 호출하여 데이터베이스 핸들을 얻을 수 있습니다:
- `sql.Open` (연결 문자열을 받음)
- `sql.OpenDB` (`driver.Connector`를 받음)

둘 다 [`sql.DB`](https://pkg.go.dev/database/sql#DB)에 대한 포인터를 반환합니다.

**참고:** 데이터베이스 자격 증명을 Go 소스 코드에서 제외하세요.

#### 연결 문자열로 열기

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

#### Connector로 열기

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

#### 오류 처리

코드는 `sql.Open`의 오류를 확인해야 합니다. 이것은 연결 오류가 아니라 `sql.Open`이 핸들을 초기화할 수 없는 경우의 오류입니다(예: DSN을 파싱할 수 없는 경우).

### 연결 확인

데이터베이스 핸들을 열 때 `sql` 패키지는 즉시 새 데이터베이스 연결을 생성하지 않을 수 있습니다. 연결이 설정될 수 있는지 확인하려면 [`Ping`](https://pkg.go.dev/database/sql#DB.Ping) 또는 [`PingContext`](https://pkg.go.dev/database/sql#DB.PingContext)를 호출합니다.

**예시:**

```go
db, err = sql.Open("mysql", connString)

// 성공적인 연결을 확인합니다.
if err := db.Ping(); err != nil {
    log.Fatal(err)
}
```

### 데이터베이스 자격 증명 저장

데이터베이스 자격 증명을 Go 소스 코드에 저장하지 마세요. 대신 코드 외부의 위치에 저장합니다:
- 자격 증명을 저장하고 API를 제공하는 비밀 관리 앱
- 비밀 관리자에서 로드되는 환경 변수

**환경 변수 사용 예시:**

```go
username := os.Getenv("DB_USER")
password := os.Getenv("DB_PASS")
```

이 접근 방식은 로컬 테스트를 위해 환경 변수를 직접 설정할 수도 있게 합니다.

### 리소스 해제

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

## 데이터 쿼리

> **원문:** https://go.dev/doc/database/querying

### 개요

데이터를 반환하는 SQL 문을 실행할 때 `database/sql` 패키지에서 제공하는 `Query` 메서드를 사용합니다. 각 메서드는 `Scan` 메서드를 사용하여 데이터를 변수에 복사할 수 있는 `Row` 또는 `Rows`를 반환합니다. 이러한 메서드는 `SELECT` 문을 실행합니다.

데이터를 반환하지 않는 문에는 대신 `Exec` 또는 `ExecContext` 메서드를 사용합니다.

`database/sql` 패키지는 결과를 위한 쿼리를 실행하는 두 가지 방법을 제공합니다:

- **단일 행 쿼리** – `QueryRow`는 데이터베이스에서 최대 단일 `Row`를 반환합니다
- **여러 행 쿼리** – `Query`는 코드가 순회할 수 있는 `Rows` 구조체로 모든 일치하는 행을 반환합니다

#### 준비된 문
코드가 동일한 SQL 문을 반복적으로 실행하는 경우 준비된 문 사용을 고려하세요.

#### 보안 경고
**SQL 문을 조립하기 위해 `fmt.Sprintf`와 같은 문자열 포매팅 함수를 사용하지 마세요!** 이것은 SQL 인젝션 위험을 초래합니다.

---

### 단일 행 쿼리

`QueryRow`는 최대 단일 데이터베이스 행을 검색합니다(예: 고유 ID로 데이터를 조회할 때). 여러 행이 반환되면 `Scan`은 첫 번째를 제외한 모두를 폐기합니다.

`QueryRowContext`는 `QueryRow`처럼 작동하지만 `context.Context` 인수를 받습니다.

#### 예시
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

#### 오류 처리
`QueryRow`는 자체적으로 오류를 반환하지 않습니다. 대신 `Scan`이 결합된 조회 및 스캔의 모든 오류를 보고합니다. 쿼리가 행을 찾지 못하면 `sql.ErrNoRows`를 반환합니다.

#### 매개변수 플레이스홀더 참고
매개변수 플레이스홀더는 DBMS와 드라이버에 따라 다릅니다. 예를 들어 Postgres용 pq 드라이버는 `?` 대신 `$1`을 필요로 합니다.

#### 단일 행 반환 함수

| 함수 | 설명 |
|------|------|
| `DB.QueryRow` / `DB.QueryRowContext` | 독립적으로 단일 행 쿼리 실행 |
| `Tx.QueryRow` / `Tx.QueryRowContext` | 트랜잭션 내에서 단일 행 쿼리 실행 |
| `Stmt.QueryRow` / `Stmt.QueryRowContext` | 준비된 문을 사용하여 단일 행 쿼리 실행 |
| `Conn.QueryRowContext` | 예약된 연결과 함께 사용 |

---

### 여러 행 쿼리

`Query` 또는 `QueryContext`를 사용하여 여러 행을 쿼리하면 쿼리 결과를 나타내는 `Rows`가 반환됩니다. `Rows.Next`를 사용하여 반환된 행을 반복합니다. 각 반복은 `Scan`을 호출하여 컬럼 값을 변수에 복사합니다.

`QueryContext`는 `Query`처럼 작동하지만 `context.Context` 인수를 받습니다.

#### 예시
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

#### 중요 참고 사항
- 리소스를 해제하기 위해 항상 `rows.Close()`를 defer합니다
- 결과를 순회한 후 `sql.Rows`의 오류를 확인합니다

#### 여러 행 반환 함수

| 함수 | 설명 |
|------|------|
| `DB.Query` / `DB.QueryContext` | 독립적으로 쿼리 실행 |
| `Tx.Query` / `Tx.QueryContext` | 트랜잭션 내에서 쿼리 실행 |
| `Stmt.Query` / `Stmt.QueryContext` | 준비된 문을 사용하여 쿼리 실행 |
| `Conn.QueryContext` | 예약된 연결과 함께 사용 |

---

### Nullable 컬럼 값 처리

`database/sql` 패키지는 컬럼 값이 null일 수 있을 때 `Scan` 함수를 위한 특수 타입을 제공합니다. 각 타입에는 non-null인 경우 값을 담는 필드와 `Valid` 필드가 포함됩니다.

#### 예시
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

#### Null 타입 옵션
- `NullBool`
- `NullFloat64`
- `NullInt32`
- `NullInt64`
- `NullString`
- `NullTime`

---

### 컬럼에서 데이터 가져오기

행을 순회할 때 `Scan`을 사용하여 컬럼 값을 Go 값에 복사합니다. 모든 드라이버는 기본 변환(예: SQL `INT`에서 Go `int`로)을 지원하며, 일부 드라이버는 이러한 변환을 확장합니다.

#### 변환 동작
- `Scan`은 유사한 타입을 변환합니다(예: SQL `CHAR`, `VARCHAR`, `TEXT`에서 Go `string`으로)
- `Scan`은 호환되는 Go 타입으로도 변환할 수 있습니다(예: `strconv.Atoi`를 사용하여 숫자를 포함하는 `VARCHAR`에서 `int`로)

---

### 여러 결과 집합 처리

데이터베이스 작업이 여러 결과 집합을 반환할 때 `Rows.NextResultSet`을 사용합니다. 이것은 여러 테이블을 별도로 쿼리하는 SQL을 보낼 때 유용합니다.

`Rows.NextResultSet`은 다음 결과 집합을 준비하고 다음 결과 집합이 있는지 나타내는 boolean을 반환합니다.

#### 예시
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

## 연결 관리

> **원문:** https://go.dev/doc/database/manage-connections

### 개요

대부분의 프로그램에서는 `sql.DB` 연결 풀의 기본값을 조정할 필요가 없습니다. 그러나 일부 고급 프로그램에서는 연결 풀 매개변수를 튜닝하거나 연결을 명시적으로 다루어야 할 수 있습니다.

[`sql.DB`](https://pkg.go.dev/database/sql#DB) 데이터베이스 핸들은 여러 고루틴에서 동시에 안전하게 사용할 수 있습니다(다른 언어에서 "스레드 안전(thread-safe)"이라고 부르는 것과 동일합니다). 일부 다른 데이터베이스 접근 라이브러리는 한 번에 하나의 작업에만 사용할 수 있는 연결을 기반으로 합니다. 이러한 차이를 해소하기 위해 각 `sql.DB`는 기본 데이터베이스에 대한 활성 연결 풀을 관리하며, Go 프로그램에서 병렬 처리를 위해 필요에 따라 새 연결을 생성합니다.

연결 풀은 대부분의 데이터 접근 요구에 적합합니다. `sql.DB`의 `Query` 또는 `Exec` 메서드를 호출하면 `sql.DB` 구현은 풀에서 사용 가능한 연결을 검색하거나, 필요한 경우 새 연결을 생성합니다. 패키지는 연결이 더 이상 필요하지 않을 때 풀로 반환합니다. 이를 통해 데이터베이스 접근의 높은 수준의 병렬 처리를 지원합니다.

---

### 연결 풀 속성 설정 {#connection_pool_properties}

`sql` 패키지가 연결 풀을 관리하는 방식을 안내하는 속성을 설정할 수 있습니다. 이러한 속성의 효과에 대한 통계를 얻으려면 [`DB.Stats`](https://pkg.go.dev/database/sql#DB.Stats)를 사용합니다.

#### 최대 열린 연결 수 설정

[`DB.SetMaxOpenConns`](https://pkg.go.dev/database/sql#DB.SetMaxOpenConns)는 열린 연결 수에 제한을 둡니다. 이 제한을 초과하면 새 데이터베이스 작업은 기존 작업이 완료될 때까지 대기하며, 그때 `sql.DB`가 다른 연결을 생성합니다. 기본적으로 `sql.DB`는 연결이 필요할 때 기존 연결이 모두 사용 중이면 새 연결을 생성합니다.

**경고:** 제한을 설정하면 데이터베이스 사용이 잠금(lock)이나 세마포어(semaphore)를 획득하는 것과 유사해지며, 그 결과 애플리케이션이 새 데이터베이스 연결을 기다리며 교착 상태(deadlock)에 빠질 수 있습니다.

#### 최대 유휴 연결 수 설정

[`DB.SetMaxIdleConns`](https://pkg.go.dev/database/sql#DB.SetMaxIdleConns)는 `sql.DB`가 유지하는 최대 유휴 연결(idle connection) 수의 제한을 변경합니다.

SQL 작업이 주어진 데이터베이스 연결에서 완료되면 일반적으로 즉시 종료되지 않습니다. 애플리케이션이 곧 다시 필요로 할 수 있으며, 열린 연결을 유지하면 다음 작업을 위해 데이터베이스에 다시 연결하는 것을 피할 수 있기 때문입니다. 기본적으로 `sql.DB`는 임의의 순간에 두 개의 유휴 연결을 유지합니다. 제한을 높이면 상당한 병렬 처리가 있는 프로그램에서 빈번한 재연결을 피할 수 있습니다.

#### 연결의 최대 유휴 시간 설정

[`DB.SetConnMaxIdleTime`](https://pkg.go.dev/database/sql#DB.SetConnMaxIdleTime)은 연결이 닫히기 전에 유휴 상태로 있을 수 있는 최대 시간을 설정합니다. 이로 인해 `sql.DB`는 주어진 기간보다 오래 유휴 상태였던 연결을 닫습니다.

기본적으로 유휴 연결이 연결 풀에 추가되면 다시 필요할 때까지 그곳에 유지됩니다. `DB.SetMaxIdleConns`를 사용하여 병렬 활동의 폭주(burst) 중에 허용되는 유휴 연결 수를 늘리는 경우, `DB.SetConnMaxIdleTime`도 함께 사용하여 시스템이 조용해질 때 해당 연결을 나중에 해제하도록 배치할 수 있습니다.

#### 연결의 최대 수명 설정

[`DB.SetConnMaxLifetime`](https://pkg.go.dev/database/sql#DB.SetConnMaxLifetime)을 사용하면 연결이 닫히기 전에 열린 상태로 유지될 수 있는 최대 시간을 설정합니다.

기본적으로 연결은 위에서 설명한 제한에 따라 임의의 긴 시간 동안 사용하고 재사용할 수 있습니다. 로드 밸런싱된 데이터베이스 서버를 사용하는 것과 같은 일부 시스템에서는 애플리케이션이 재연결 없이 특정 연결을 너무 오래 사용하지 않도록 하는 것이 도움이 될 수 있습니다.

**핵심 포인트:**
- `SetMaxOpenConns` — 열린 연결의 최대 수를 제한합니다
- `SetMaxIdleConns` — 유휴 연결의 최대 수를 제한합니다 (기본값: 2)
- `SetConnMaxIdleTime` — 유휴 연결의 최대 유휴 시간을 설정합니다
- `SetConnMaxLifetime` — 연결의 최대 수명을 설정합니다

---

### 전용 연결 사용 {#dedicated_connections}

`database/sql` 패키지에는 데이터베이스가 특정 연결에서 실행되는 일련의 작업에 암묵적인 의미를 부여할 수 있을 때 사용하는 함수들이 포함되어 있습니다.

가장 일반적인 예는 트랜잭션(transaction)으로, 일반적으로 `BEGIN` 명령으로 시작하여 `COMMIT` 또는 `ROLLBACK` 명령으로 종료하며, 해당 명령 사이에 연결에서 발행된 모든 명령을 전체 트랜잭션에 포함합니다. 이 사용 사례에 대해서는 `sql` 패키지의 트랜잭션 지원을 사용하세요. [트랜잭션 실행](/doc/database/execute-transactions)을 참조하세요.

일련의 개별 작업이 모두 동일한 연결에서 실행되어야 하는 다른 사용 사례의 경우, `sql` 패키지는 전용 연결을 제공합니다. [`DB.Conn`](https://pkg.go.dev/database/sql#DB.Conn)은 전용 연결인 [`sql.Conn`](https://pkg.go.dev/database/sql#Conn)을 가져옵니다. `sql.Conn`은 `BeginTx`, `ExecContext`, `PingContext`, `PrepareContext`, `QueryContext`, `QueryRowContext` 메서드를 가지며, 이들은 DB의 동일한 메서드처럼 동작하지만 전용 연결만 사용합니다. 전용 연결 사용이 완료되면 `Conn.Close`를 사용하여 해제해야 합니다.


---

## 데이터를 반환하지 않는 SQL 문 실행

> **원문:** https://go.dev/doc/database/change-data

### 개요

데이터를 반환하지 않는 데이터베이스 작업을 수행할 때 `database/sql` 패키지의 `Exec` 또는 `ExecContext` 메서드를 사용합니다. 이 방법으로 실행하는 SQL 문에는 `INSERT`, `DELETE`, `UPDATE`가 포함됩니다.

**핵심 사항:** 쿼리가 행을 반환할 수 있는 경우 대신 `Query` 또는 `QueryContext` 메서드를 사용합니다. 자세한 내용은 [데이터베이스 쿼리](/doc/database/querying)를 참조하세요.

### ExecContext 메서드

`ExecContext` 메서드는 `Exec` 메서드처럼 작동하지만 [진행 중인 작업 취소](/doc/database/cancel-operations)에서 설명된 대로 추가 `context.Context` 인수를 받습니다.

### 코드 예시

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

### 반환 값

`DB.Exec`는 값을 반환합니다: [`sql.Result`](https://pkg.go.dev/database/sql#Result)와 오류. 오류가 `nil`이면 `Result`를 사용하여:
- 마지막으로 삽입된 항목의 ID를 가져올 수 있습니다(예시처럼)
- 작업에 영향을 받은 행 수를 검색할 수 있습니다

### 중요 참고 사항

**매개변수 플레이스홀더:** 준비된 문의 매개변수 플레이스홀더는 사용하는 DBMS와 드라이버에 따라 다릅니다. 예를 들어 Postgres용 [pq 드라이버](https://pkg.go.dev/github.com/lib/pq)는 `?` 대신 `$1`과 같은 플레이스홀더를 필요로 합니다.

**준비된 문:** 코드가 동일한 SQL 문을 반복적으로 실행하는 경우 SQL 문에서 재사용 가능한 준비된 문을 생성하기 위해 `sql.Stmt` 사용을 고려하세요. 자세한 내용은 [준비된 문 사용](/doc/database/prepared-statements)을 참조하세요.

**보안 경고:** SQL 문을 조립하기 위해 `fmt.Sprintf`와 같은 문자열 포매팅 함수를 사용하지 마세요! SQL 인젝션 위험을 초래할 수 있습니다. 자세한 내용은 [SQL 인젝션 위험 피하기](/doc/database/sql-injection)를 참조하세요.

### 행을 반환하지 않는 SQL 문 실행 함수

| 함수 | 설명 |
|------|------|
| `DB.Exec` `DB.ExecContext` | 독립적으로 단일 SQL 문을 실행합니다. |
| `Tx.Exec` `Tx.ExecContext` | 더 큰 트랜잭션 내에서 SQL 문을 실행합니다. 자세한 내용은 [트랜잭션 실행](/doc/database/execute-transactions)을 참조하세요. |
| `Stmt.Exec` `Stmt.ExecContext` | 이미 준비된 SQL 문을 실행합니다. 자세한 내용은 [준비된 문 사용](/doc/database/prepared-statements)을 참조하세요. |
| `Conn.ExecContext` | 예약된 연결과 함께 사용합니다. 자세한 내용은 [연결 관리](/doc/database/manage-connections)를 참조하세요. |

---

## 모듈 가이드

## 모듈 개발 및 게시

> **원문:** https://go.dev/doc/modules/developing

### 개요

관련 패키지를 모듈로 모은 다음, 다른 개발자가 사용할 수 있도록 모듈을 게시할 수 있습니다. 이 주제에서는 모듈 개발 및 게시에 대한 개요를 제공합니다.

모듈의 개발, 게시 및 사용을 지원하기 위해 다음을 사용합니다:

- **워크플로** - 모듈을 개발하고 게시하며 시간이 지남에 따라 새 버전으로 수정하는 과정
- **설계 관행** - 모듈 사용자가 이해하고 안정적인 방식으로 새 버전으로 업그레이드하는 데 도움이 되는 방법
- **분산 게시 시스템** - 버전 번호와 함께 자신의 저장소에서 모듈을 게시하고 코드를 검색
- **패키지 검색 엔진 및 문서 브라우저** (pkg.go.dev) - 개발자가 모듈을 찾을 수 있는 곳
- **모듈 버전 번호 규칙** - 안정성과 하위 호환성에 대한 기대를 전달
- **Go 도구** - 다른 개발자가 의존성을 더 쉽게 관리할 수 있게 함

### 모듈 개발 및 게시 워크플로

다른 사람들을 위해 모듈을 게시하려면 해당 모듈을 더 쉽게 사용할 수 있도록 몇 가지 규칙을 채택합니다.

다음은 상위 수준 단계입니다:

1. 모듈에 포함할 패키지를 **설계하고 코딩**
2. Go 도구를 통해 다른 사람들이 사용할 수 있도록 보장하는 규칙을 사용하여 저장소에 **코드를 커밋**
3. 개발자가 발견할 수 있도록 **모듈을 게시**
4. 각 버전의 안정성과 하위 호환성을 알리는 버전 번호 규칙을 사용하여 시간이 지남에 따라 **버전을 수정**

### 설계 및 개발

개발자가 모듈의 함수와 패키지가 일관된 전체를 형성할 때 더 쉽게 찾고 사용할 수 있습니다. 모듈의 공개 API를 설계할 때 기능을 집중적이고 구분되게 유지합니다.

**하위 호환성을 염두에 두고** 모듈을 설계하고 개발하면 사용자가 자신의 코드에 대한 변경을 최소화하면서 업그레이드할 수 있습니다. 코드의 특정 기술은 하위 호환성을 깨는 버전 릴리스를 피하는 데 도움이 될 수 있습니다.

**게시하기 전에** replace 지시문을 사용하여 로컬 파일 시스템의 모듈을 참조할 수 있어 모듈이 아직 개발 중일 때 클라이언트 코드를 더 쉽게 작성할 수 있습니다.

### 분산 게시

Go에서는 다른 개발자가 사용할 수 있도록 **저장소에서 코드를 태깅하여** 모듈을 게시합니다. Go 도구가 저장소나 프록시 서버에서 직접 모듈을 다운로드할 수 있으므로 중앙 집중식 서비스에 푸시할 필요가 없습니다.

개발자는 Go 도구(`go get` 명령 포함)를 사용하여 모듈의 코드를 다운로드하여 컴파일합니다. 이 모델을 지원하기 위해 Go 도구가 저장소에서 모듈의 소스를 검색할 수 있도록 하는 규칙과 모범 사례를 따릅니다.

### 패키지 발견

모듈을 게시한 후 **pkg.go.dev의 Go 패키지 발견 사이트**에서 볼 수 있게 되어 개발자가 검색하고 문서를 읽을 수 있습니다.

모듈을 사용하려면 개발자는:
1. 모듈에서 패키지를 임포트
2. `go get` 명령을 실행하여 컴파일할 소스 코드를 다운로드

### 버전 관리

모듈을 수정하고 개선할 때 각 버전의 안정성과 하위 호환성을 알리도록 설계된 **시맨틱 버전 관리 모델에 기반하여 버전 번호를 할당**합니다. 이를 통해 개발자는 모듈이 안정적인지, 업그레이드에 중요한 변경 사항이 포함되어 있는지 판단할 수 있습니다.

저장소에서 번호로 **모듈의 소스를 태깅하여** 모듈의 버전 번호를 나타냅니다.

---

**관련 리소스:**
- [모듈 릴리스 및 버전 관리 워크플로](release-workflow)
- [모듈 소스 관리](/doc/modules/managing-source)
- [모듈 게시](publishing)
- [모듈 버전 번호 매기기](/doc/modules/version-numbers)
- [메이저 버전 업데이트 개발](major-version)
- [모듈 호환성 유지](/blog/module-compatibility)


---

## 메이저 버전 업데이트 개발

> **원문:** https://go.dev/doc/modules/major-version

### 개요

잠재적인 새 버전의 변경 사항이 모듈 사용자에 대한 하위 호환성을 보장할 수 없을 때 메이저 버전을 업데이트해야 합니다. 이는 일반적으로 이전 버전을 사용하는 클라이언트 코드를 깨뜨리는 방식으로 모듈의 공개 API(public API)를 변경할 때 발생합니다.

**핵심 포인트:**
- 각 릴리스 유형(메이저, 마이너, 패치 또는 프리릴리스)은 사용자에게 다른 의미를 가짐
- 개발자들은 이러한 차이를 통해 릴리스가 나타내는 위험 수준을 이해
- 버전 번호는 이전 릴리스 이후의 변경 특성을 정확하게 반영해야 함
- 자세한 내용은 [모듈 버전 번호 매기기](version-numbers)를 참조

---

### 메이저 버전 업데이트 시 고려 사항

메이저 버전 업데이트는 개발자 본인과 사용자 모두에게 상당한 변화를 의미하므로, 반드시 필요한 경우에만 새 메이저 버전으로 업데이트해야 합니다. 다음 사항을 고려하세요:

#### 1. 사용자 커뮤니케이션

새 메이저 버전이 이전 버전 지원에 어떤 의미를 갖는지 사용자에게 명확히 전달합니다:
- 이전 버전이 더 이상 사용되지 않는(deprecated) 것인지?
- 이전 버전을 계속 지원할 것인지?
- 이전 버전에 대한 버그 수정을 유지할 것인지?

#### 2. 유지 보수 부담

두 버전(이전 버전과 새 버전)을 동시에 유지할 준비가 되어 있어야 합니다. 한 버전에서 버그를 수정하면 종종 다른 버전에도 해당 수정 사항을 포팅(port)해야 합니다.

#### 3. 모듈 경로(Module Path) 변경

새 메이저 버전은 의존성 관리 관점에서 새로운 모듈입니다. 메이저 버전은 이전 버전과 다른 모듈 경로를 가집니다.

**예시:**
```
이전 버전: example.com/mymodule
새 버전:   example.com/mymodule/v2
```

사용자는 단순히 업그레이드하는 것이 아니라 새 모듈을 사용하도록 업데이트해야 합니다.

#### 4. 임포트 경로(Import Path) 업데이트

새 메이저 버전을 개발할 때 새 모듈에서 패키지를 임포트하는 모든 곳의 임포트 경로를 업데이트해야 합니다. 사용자도 새 메이저 버전으로 업그레이드하려면 자신의 임포트 경로를 업데이트해야 합니다.

---

### 메이저 릴리스를 위한 브랜치 생성

가장 간단한 접근 방식은 이전 메이저 버전의 최신 버전에서 저장소를 브랜치(branch)하는 것입니다.

#### 예시: v2 브랜치 생성

```bash
$ cd mymodule
$ git checkout -b v2
Switched to a new branch "v2"
```

이 시점에서 저장소는 master(또는 main) 브랜치에서 v2 브랜치로 분기됩니다. 이후 v2 브랜치에서 호환성을 깨는 변경 작업을 진행합니다.

---

### 새 버전을 위한 필수 변경 사항

소스가 브랜치되면 다음 변경 사항을 적용합니다:

#### 1. go.mod 파일 업데이트

모듈 경로에 새 메이저 버전 번호를 추가합니다:

```
// 기존 버전
module example.com/mymodule

// 새 버전
module example.com/mymodule/v2
```

#### 2. 임포트 문(Import Statement) 업데이트

Go 코드에서 임포트하는 모든 패키지 경로의 모듈 경로 부분에 메이저 버전 번호를 추가합니다:

```go
// 이전 임포트 문
import "example.com/mymodule/package1"

// 새 임포트 문
import "example.com/mymodule/v2/package1"
```

**핵심 포인트:**
- 모듈 내부의 모든 임포트 경로를 새 메이저 버전으로 업데이트해야 함
- 외부 사용자도 새 메이저 버전을 사용하려면 임포트 경로를 변경해야 함
- go.mod 파일의 모듈 경로 변경은 사실상 새로운 모듈을 생성하는 것

---

### 관련 리소스

- [모듈 개발 및 게시](developing) - 모듈 개발 및 게시 개요
- [모듈 릴리스 및 버전 관리 워크플로](release-workflow) - 전체 릴리스 워크플로
- [모듈 게시](publishing) - 모듈 게시 단계
- [모듈 버전 번호 매기기](version-numbers) - 버전 번호 규칙


---

## 의존성 관리

> **원문:** https://go.dev/doc/modules/managing-dependencies

### 개요

코드가 외부 패키지를 사용할 때, 해당 패키지(모듈로 배포됨)는 의존성이 됩니다. Go는 외부 의존성을 통합하면서 Go 애플리케이션을 안전하게 유지하는 데 도움이 되는 의존성 관리 도구를 제공합니다.

### 의존성 사용 및 관리 워크플로

가장 일반적인 의존성 관리 단계는 다음과 같습니다:

1. [pkg.go.dev](https://pkg.go.dev)에서 **유용한 패키지를 찾습니다**
2. 코드에서 원하는 **패키지를 임포트합니다**
3. 의존성 추적을 위해 **코드를 모듈에 추가합니다** (아직 없는 경우)
4. **외부 패키지를 의존성으로 추가**하여 관리합니다
5. 시간이 지남에 따라 필요에 따라 **의존성 버전을 업그레이드하거나 다운그레이드합니다**

### 모듈로서의 의존성 관리

Go는 다음을 포함하는 시스템을 통해 의존성을 관리합니다:

- **분산 게시 시스템** - 개발자가 자신의 저장소에서 버전 번호와 함께 모듈을 게시
- **패키지 검색 엔진** - 모듈을 찾기 위한 pkg.go.dev
- **버전 번호 규칙** - 안정성과 하위 호환성을 위한 시맨틱 버전 관리
- **Go 도구** - 의존성 관리를 위한 명령어

### 유용한 패키지 찾기 및 임포트

[pkg.go.dev](https://pkg.go.dev)에서 패키지를 검색합니다. 패키지 경로를 복사하여 import 문에 붙여넣습니다:

```go
import "rsc.io/quote"
```

### 코드에서 의존성 추적 활성화

`go mod init`을 사용하여 go.mod 파일을 생성합니다:

```bash
$ go mod init example/mymodule
```

이 명령은:
- 프로젝트 루트에 go.mod 파일을 생성
- 추가하는 모든 의존성을 추적
- 검증을 위한 체크섬이 포함된 go.sum 파일도 생성

### 모듈 이름 짓기

모듈 경로는 다음 형식을 따릅니다:

```
<prefix>/<descriptive-text>
```

**접두사 옵션:**
- 저장소 위치(게시된 모듈에 권장): `github.com/<project-name>/`
- 통제하는 이름(회사 이름 등)

**예약된 접두사:**
- `test` - 다른 모듈을 로컬에서 테스트하는 모듈용
- `example` - 문서 및 튜토리얼용

### 의존성 추가

`go get` 명령을 사용하여 의존성을 추가합니다:

```bash
# 현재 디렉터리의 패키지에 대한 모든 의존성 추가
$ go get .

# 특정 의존성 추가
$ go get example.com/theirmodule
```

`go get` 명령은:
- go.mod에 `require` 지시문을 추가
- 모듈 소스 코드를 다운로드
- 보안을 위해 각 모듈을 인증

### 특정 의존성 버전 가져오기

```bash
# 특정 버전 가져오기
$ go get example.com/theirmodule@v1.3.4

# 최신 버전 가져오기
$ go get example.com/theirmodule@latest
```

go.mod 파일에 표시됩니다:
```
require example.com/theirmodule v1.3.4
```

### 사용 가능한 업데이트 발견

`go list`를 사용하여 새 버전을 확인합니다:

```bash
# 사용 가능한 최신 버전과 함께 모든 의존성 나열
$ go list -m -u all

# 특정 모듈 확인
$ go list -m -u example.com/theirmodule
```

### 의존성 업그레이드 또는 다운그레이드

1. `go list -m -u`를 사용하여 새 버전을 발견
2. `go get`과 버전 지정을 사용하여 특정 버전 추가

### 코드의 의존성 동기화

`go mod tidy`를 사용하여 관리되는 의존성이 임포트된 패키지와 일치하도록 합니다:

```bash
$ go mod tidy
```

이 명령은:
- 임포트에 필요한 누락된 모듈을 추가
- 사용되지 않는 모듈을 제거
- 제거된 모듈을 보려면 `-v` 플래그 사용

### 게시되지 않은 모듈 코드에 대한 개발 및 테스트

#### 로컬 디렉터리의 모듈 코드 요청

go.mod에서 `replace` 지시문을 사용합니다:

```
module example.com/mymodule

go 1.23.0

require example.com/theirmodule v0.0.0-unpublished

replace example.com/theirmodule v0.0.0-unpublished => ../theirmodule
```

Go 도구를 사용하여 설정합니다:

```bash
$ go mod edit -replace=example.com/theirmodule@v0.0.0-unpublished=../theirmodule
$ go get example.com/theirmodule@v0.0.0-unpublished
```

#### 자신의 저장소 포크에서 외부 모듈 코드 요청

모듈 경로를 포크로 대체합니다:

```
module example.com/mymodule

go 1.23.0

require example.com/theirmodule v1.2.3

replace example.com/theirmodule v1.2.3 => example.com/myfork/theirmodule v1.2.3-fixed
```

명령어로 설정:

```bash
$ go list -m example.com/theirmodule
example.com/theirmodule v1.2.3
$ go mod edit -replace=example.com/theirmodule@v1.2.3=example.com/myfork/theirmodule@v1.2.3-fixed
```

### 저장소 식별자를 사용하여 특정 커밋 가져오기

커밋 해시나 브랜치와 함께 `go get`을 사용합니다:

```bash
# 특정 커밋 가져오기
$ go get example.com/theirmodule@4cf76c2

# 특정 브랜치 가져오기
$ go get example.com/theirmodule@bugfixes
```

### 의존성 제거

사용되지 않는 모든 의존성 제거:

```bash
$ go mod tidy
```

특정 의존성 제거:

```bash
$ go get example.com/theirmodule@none
```

이는 또한 제거된 모듈에 의존하는 다른 의존성을 다운그레이드하거나 제거합니다.

### 도구 의존성

Go 1.24+에서는 다음으로 도구 의존성을 추가합니다:

```bash
$ go get -tool golang.org/x/tools/cmd/stringer
```

이는 go.mod에 `tool` 지시문을 추가합니다. 도구 실행:

```bash
$ go tool stringer
```

동일한 경로 조각을 가진 여러 도구나 Go 배포 도구와 일치하는 경우:

```bash
$ go tool golang.org/x/tools/cmd/stringer
```

사용 가능한 모든 도구 나열:

```bash
$ go tool
```

수동으로 도구 지시문을 추가하지만 해당 `require` 지시문이 존재하는지 확인합니다. 누락된 요구 사항을 추가하려면 `go mod tidy`를 실행합니다.

도구 의존성:
- 최소 버전 선택에 참여
- `require`, `replace`, `exclude` 지시문을 준수
- 모듈 가지치기로 인해 보통 모듈의 요구 사항이 되지 않음

### 모듈 프록시 서버 지정

`GOPROXY` 환경 변수를 설정합니다:

```
GOPROXY="https://proxy.golang.org,direct"
```

**기본 동작:**
- 먼저 Google이 운영하는 공개 프록시 사용
- 모듈 저장소에서 직접 다운로드로 대체

**쉼표로 여러 프록시:**
```
GOPROXY="https://proxy.example.com,https://proxy2.example.com"
```
HTTP 404 또는 410 오류에서만 다음 URL 시도.

**파이프로 여러 프록시:**
```
GOPROXY="https://proxy.example.com|https://proxy2.example.com"
```
HTTP 오류 코드에 관계없이 다음 URL 시도.

#### 비공개 모듈

비공개 모듈에 대해 `GOPRIVATE` 환경 변수를 구성합니다:

```
GOPRIVATE=*.corp.example.com,*.research.example.com
```

프록시를 사용하지 않아야 하는 모듈을 지정하는 `GONOPROXY`도 지원합니다.


---

## 모듈 릴리스 및 버전 관리 워크플로

> **원문:** https://go.dev/doc/modules/release-workflow

### 개요

다른 개발자가 사용할 모듈을 개발할 때, 모듈을 사용하는 개발자에게 신뢰할 수 있고 일관된 경험을 보장하는 워크플로를 따를 수 있습니다.

**관련 주제:**
- [모듈 개발 및 게시](developing)
- [의존성 관리](/doc/modules/managing-dependencies)
- [모듈 버전 번호 매기기](/doc/modules/version-numbers)

---

### 일반적인 워크플로 단계

다음 순서는 예제 새 모듈에 대한 릴리스 및 버전 관리 워크플로 단계를 보여줍니다:

1. **모듈을 시작**하고 개발자가 사용하기 쉽고 유지 관리하기 쉽도록 소스를 구성합니다.
   - [튜토리얼: Go 모듈 만들기](/doc/tutorial/create-module) 참조
   - [모듈 소스 관리](/doc/modules/managing-source) 참조

2. 게시되지 않은 모듈의 함수를 호출하는 **로컬 클라이언트 코드를 작성**합니다.
   - 게시하기 전에 로컬 디렉터리에서 모듈 코드를 테스트
   - [게시되지 않은 모듈에 대한 코딩](#게시되지-않은-모듈에-대한-코딩) 참조

3. 알파 및 베타와 같은 **v0 프리릴리스 게시를 시작**합니다.
   - [프리릴리스 버전 게시](#프리릴리스-버전-게시) 참조

4. 안정성이 보장되지 않는 **v0을 릴리스**합니다.
   - [첫 번째(불안정) 버전 게시](#첫-번째불안정-버전-게시) 참조

5. **v0의 새 버전을 릴리스**합니다.
   - 버그 수정(패치 릴리스)
   - 공개 API 추가(마이너 릴리스)
   - v0에서는 호환성을 깨는 변경 허용
   - [버그 수정 게시](#버그-수정-게시) 및 [호환성을 깨지 않는 API 변경 게시](#호환성을-깨지-않는-api-변경-게시) 참조

6. 안정 버전을 위한 **알파 및 베타로 프리릴리스를 게시**합니다.
   - [프리릴리스 버전 게시](#프리릴리스-버전-게시) 참조

7. 첫 번째 안정 릴리스로 **v1을 릴리스**합니다.
   - [첫 번째 안정 버전 게시](#첫-번째-안정-버전-게시) 참조

8. **v1에서 버그를 계속 수정하고 모듈의 공개 API에 추가**합니다.
   - [버그 수정 게시](#버그-수정-게시) 및 [호환성을 깨지 않는 API 변경 게시](#호환성을-깨지-않는-api-변경-게시) 참조

9. **새 메이저 버전에서 호환성을 깨는 변경을 게시**합니다.
   - [호환성을 깨는 API 변경 게시](#호환성을-깨는-api-변경-게시) 참조

---

### 게시되지 않은 모듈에 대한 코딩

모듈이나 모듈의 새 버전을 개발하기 시작할 때는 아직 게시되지 않은 상태입니다. 게시 전에는 Go 명령을 사용하여 모듈을 의존성으로 추가할 수 없습니다.

**해결책:** 클라이언트 모듈의 go.mod 파일에서 `replace` 지시문을 사용하여 로컬에서 모듈을 참조합니다.

자세한 내용은 [로컬 디렉터리의 모듈 코드 요청](managing-dependencies#local_directory)을 참조하세요.

---

### 프리릴리스 버전 게시

다른 사람들이 시험해보고 피드백을 줄 수 있도록 프리릴리스 버전을 게시할 수 있습니다. 프리릴리스 버전에는 안정성 보장이 포함되지 않습니다.

**프리릴리스 버전 형식:** 버전 번호에 프리릴리스 식별자가 추가됩니다.

예시:
```
v0.2.1-beta.1
v1.2.3-alpha
```

**중요:** `go` 명령이 기본적으로 릴리스 버전을 선호하므로 프리릴리스를 사용하는 개발자는 `go get` 명령으로 버전을 명시적으로 지정해야 합니다.

예시:
```bash
go get example.com/theirmodule@v1.2.3-alpha
```

**게시:** 저장소에서 모듈 코드를 태그하고 태그에 프리릴리스 식별자를 지정합니다. [모듈 게시](publishing)를 참조하세요.

---

### 첫 번째(불안정) 버전 게시

불안정 릴리스는 **v0.x.x** 범위의 버전 번호를 가집니다. v0 버전은 안정성이나 하위 호환성을 보장하지 않지만 v1으로 안정성 약속을 하기 전에 피드백을 받고 API를 다듬을 수 있는 방법을 제공합니다.

변경할 때마다 v0 버전 번호의 마이너 및 패치 부분을 증가시킬 수 있습니다. 예를 들어:
- v0.0.0을 릴리스한 후 버그 수정으로 v0.0.1을 릴리스할 수 있습니다

예시 버전 번호:
```
v0.1.3
```

**게시:** 저장소에서 모듈 코드를 태그하고 태그에 v0 버전 번호를 지정합니다. [모듈 게시](publishing)를 참조하세요.

---

### 첫 번째 안정 버전 게시

첫 번째 안정 릴리스는 **v1.x.x** 버전 번호를 가집니다.

**v1 릴리스로 다음을 약속하게 됩니다:**

- 개발자가 자신의 코드를 깨지 않고 메이저 버전의 후속 마이너 및 패치 릴리스로 업그레이드할 수 있음
- 하위 호환성을 깨는 모듈의 공개 API(함수 및 메서드 시그니처) 변경을 더 이상 하지 않음
- 하위 호환성을 깨는 내보낸 타입을 제거하지 않음
- API에 대한 향후 변경(예: 구조체에 새 필드 추가)은 하위 호환되며 새 마이너 릴리스에 포함됨
- 버그 수정(예: 보안 수정)은 패치 릴리스 또는 마이너 릴리스의 일부로 포함됨

**참고:** 첫 번째 메이저 버전이 v0 릴리스일 수 있지만, v0 버전은 안정성이나 하위 호환성 보장을 알리지 않습니다. 결과적으로 v0에서 v1로 증가할 때 v0 릴리스가 안정적으로 간주되지 않았으므로 하위 호환성을 깨는 것에 신경 쓸 필요가 없습니다.

예시 안정 버전 번호:
```
v1.0.0
```

**게시:** 저장소에서 모듈 코드를 태그하고 태그에 v1 버전 번호를 지정합니다. [모듈 게시](publishing)를 참조하세요.

---

### 버그 수정 게시

버그 수정으로만 제한된 릴리스를 게시할 수 있습니다. 이를 **패치 릴리스**라고 합니다.

**특징:**
- 사소한 변경만 포함
- 모듈의 공개 API에 변경 없음
- 개발자가 코드를 변경하지 않고 안전하게 업그레이드 가능
- 전이적 의존성을 패치 릴리스 이상으로 업그레이드하지 않아야 함

**버전 증가:** 모듈 버전 번호의 패치 부분을 증가시킵니다.

예시:
```
이전 버전: v1.0.0
새 버전: v1.0.1
```

**게시:** 저장소에서 모듈 코드를 태그하고 태그의 패치 버전 번호를 증가시킵니다. [모듈 게시](publishing)를 참조하세요.

---

### 호환성을 깨지 않는 API 변경 게시

모듈의 공개 API를 호환성을 깨지 않는 방식으로 변경하고 **마이너 버전 릴리스**로 게시할 수 있습니다.

**특징:**
- API를 변경하지만 호출 코드를 깨지 않는 방식으로
- 모듈 의존성 변경을 포함할 수 있음
- 새로운 함수, 메서드, 구조체 필드 또는 타입 추가
- 기존 코드에 대한 하위 호환성과 안정성 보장

**버전 증가:** 모듈 버전 번호의 마이너 부분을 증가시킵니다.

예시:
```
이전 버전: v1.0.1
새 버전: v1.1.0
```

**게시:** 저장소에서 모듈 코드를 태그하고 태그의 마이너 버전 번호를 증가시킵니다. [모듈 게시](publishing)를 참조하세요.

---

### 호환성을 깨는 API 변경 게시

**메이저 버전 릴리스**를 게시하여 하위 호환성을 깨는 버전을 게시할 수 있습니다.

**특징:**
- 하위 호환성을 보장하지 않음
- 이전 버전을 사용하는 코드를 깨는 모듈의 공개 API 변경 포함
- 모듈에 의존하는 코드에 파괴적인 영향으로 인해 최후의 수단이어야 함

호환성을 깨는 변경을 피하는 전략에 대해서는 [모듈 호환성 유지](/blog/module-compatibility)를 참조하세요.

#### 메이저 버전 업데이트 게시 단계:

1. **새 버전의 소스를 위한 장소를 만듭니다**
   - 저장소에 새 메이저 버전과 후속 마이너 및 패치 버전을 위한 새 브랜치를 만듭니다
   - [모듈 소스 관리](/doc/modules/managing-source) 참조

2. **go.mod에서 모듈 경로를 수정합니다**
   - 모듈 경로에 새 메이저 버전 번호를 추가합니다

   예시:
   ```
   example.com/mymodule/v2
   ```

   이 변경은 효과적으로 새 모듈을 만들고 패키지 경로를 변경하여 개발자가 실수로 호환성을 깨는 버전을 임포트하지 않도록 합니다. 업그레이드하려는 사람들은 이전 경로를 새 경로로 명시적으로 대체합니다.

3. **코드에서 패키지 경로를 업데이트합니다**
   - 업데이트하는 모듈에서 패키지를 임포트하는 모든 패키지 경로를 변경합니다
   - 모듈 경로를 변경했기 때문에 필요합니다

4. **프리릴리스 버전을 게시합니다**
   - 새 릴리스와 마찬가지로 공식 릴리스 전에 피드백과 버그 리포트를 받기 위해 프리릴리스 버전을 게시합니다

5. **새 메이저 버전을 게시합니다**
   - 저장소에서 모듈 코드를 태그하고 태그의 메이저 버전 번호를 증가시킵니다
   - 예시: v1.5.2에서 v2.0.0으로
   - [모듈 게시](/doc/modules/publishing) 참조

---

이 문서는 Go에서 모듈 릴리스 및 버전 관리를 위한 포괄적인 가이드를 제공하며, 개발부터 안정 릴리스 및 메이저 버전 업데이트까지의 전체 수명 주기를 다룹니다.


---

## 모듈 버전 번호 매기기

> **원문:** https://go.dev/doc/modules/version-numbers

### 개요

모듈 개발자는 모듈 버전 번호의 각 부분을 사용하여 버전의 안정성과 하위 호환성을 알립니다. 각 새 릴리스에서 모듈의 릴리스 버전 번호는 이전 릴리스 이후 모듈 변경의 특성을 구체적으로 반영합니다.

### 시맨틱 버전 관리 모델

릴리스된 모듈은 시맨틱 버전 관리 모델을 따르는 버전 번호로 게시됩니다:

**형식: vMAJOR.MINOR.PATCH[-PRERELEASE]**

예시: v1.4.0-beta.2

### 버전 구성 요소 및 의미

| 버전 단계 | 예시 | 개발자에게 전달하는 메시지 |
|-----------|------|---------------------------|
| **개발 중** | v0.x.x 또는 의사 버전 | 모듈이 불안정함; 하위 호환성 보장 없음 |
| **메이저 버전** | v1.x.x | 하위 호환되지 않는 공개 API 변경 |
| **마이너 버전** | vx.4.x | 하위 호환되는 공개 API 변경 |
| **패치 버전** | vx.x.1 | 공개 API나 의존성에 영향을 주지 않는 변경 |
| **프리릴리스 버전** | vx.x.x-beta.2 | 프리릴리스 마일스톤(알파/베타); 안정성 보장 없음 |

### 상세 설명

#### 개발 중

하위 호환성 보장이 없는 불안정한 모듈 상태를 알립니다. 두 가지 형태가 있습니다:

**의사 버전 번호 형식:**
```
v0.0.0-20170915032832-14c0d48ead0c
```

**구문:** `baseVersionPrefix-timestamp-revisionIdentifier`

- **baseVersionPrefix**: vX.0.0 또는 vX.Y.Z-0, 이전 시맨틱 버전 태그에서 파생되거나 없으면 vX.0.0
- **timestamp**: yymmddhhmmss (리비전 생성의 UTC 시간)
- **revisionIdentifier**: 12자 커밋 해시 접두사(또는 Subversion의 경우 0으로 패딩된 리비전 번호)

**v0 번호:**
```
v0.x.x
```
프로덕션에서 사용할 수 있지만 v0 버전은 안정성이나 하위 호환성을 보장하지 않습니다.

#### 메이저 버전 (v1 이상)

모듈의 공개 API에 대한 하위 호환되지 않는 변경을 알립니다.

**예시:** `v1.x.x`

- 개발자에게 상당한 혼란을 나타냄
- v1을 넘어 업그레이드할 때 모듈 경로에 메이저 버전이 추가됨: `module example.com/mymodule/v2 v2.0.0`
- 별도의 히스토리를 가진 새 모듈을 생성

#### 마이너 버전

하위 호환되는 공개 API 변경을 알립니다.

**예시:** `vx.4.x`

- 하위 호환성과 안정성을 보장
- 새로운 함수, 메서드, 구조체 필드 또는 타입을 포함할 수 있음
- 모듈의 자체 의존성 업데이트가 허용됨
- 개발자가 코드를 변경하지 않고 업그레이드 가능

#### 패치 버전

모듈의 공개 API나 의존성에 영향을 주지 않는 변경을 알립니다.

**예시:** `vx.x.1`

- 버그 수정과 같은 사소한 변경만 포함
- 하위 호환성과 안정성을 보장
- 코드 변경 없이 안전하게 업그레이드 가능

#### 프리릴리스 버전

안정성 보장이 없는 프리릴리스 마일스톤(알파, 베타 등)을 알립니다.

**예시:** `vx.x.x-beta.2`

- 하이픈과 식별자가 추가됨
- 모든 major.minor.patch 조합과 함께 사용 가능

### 핵심 참고 사항

- v0 버전은 안정성이나 하위 호환성을 보장하지 않음; v1+ 버전은 v0 소비자에 대한 하위 호환성을 깨뜨릴 수 있음
- 직접 만들지 말고 항상 Go 도구가 의사 버전 번호를 생성하도록 함
- v1보다 높은 메이저 버전으로 게시된 모듈의 경우 모듈 경로에 메이저 버전 번호가 포함됨

---

## 모듈 레퍼런스

## go.mod 파일 레퍼런스

> **원문:** https://go.dev/doc/modules/gomod-ref

### 개요

각 Go 모듈은 모듈의 속성(다른 모듈과 Go 버전에 대한 의존성 포함)을 설명하는 `go.mod` 파일로 정의됩니다.

#### 주요 속성

- **모듈 경로**: 현재 모듈의 고유 식별자(버전 번호와 결합됨); 모듈 내 모든 패키지의 임포트 접두사 역할
- **최소 Go 버전**: 현재 모듈에 필요한 최소 Go 버전
- **필요한 모듈**: 현재 모듈에 필요한 다른 모듈의 최소 버전 목록
- **Replace/Exclude 지시문**: 필요한 모듈을 다른 모듈 버전이나 로컬 디렉터리로 대체하거나, 특정 버전을 제외하는 지시

### go.mod 파일 생성

`go.mod` 파일은 다음을 사용하여 생성됩니다:

```bash
$ go mod init example/mymodule
```

### 의존성 관리

`go` 명령을 사용하여 의존성을 관리하고 `go.mod`를 일관되게 유지합니다:
- [`go get`](/ref/mod#go-get)
- [`go mod tidy`](/ref/mod#go-mod-tidy)
- [`go mod edit`](/ref/mod#go-mod-edit)

도움말 보기: `go help mod tidy`

---

### go.mod 파일 예시

```
module example.com/mymodule

go 1.14

require (
    example.com/othermodule v1.2.3
    example.com/thismodule v1.2.3
    example.com/thatmodule v1.2.3
)

replace example.com/thatmodule => ../thatmodule
exclude example.com/thismodule v1.3.0
```

---

### 지시문 레퍼런스

#### `module`

**목적**: 모듈의 모듈 경로(고유 식별자)를 선언

**구문**:
```
module module-path
```

**매개변수**:
- `module-path`: 모듈의 고유 식별자, 일반적으로 모듈을 다운로드할 수 있는 저장소 위치

**예시**:
```
module example.com/mymodule
```

```
module example.com/mymodule/v2
```

**참고**:
- v2 이상의 모듈은 경로가 메이저 버전 번호로 끝나야 함(`/v2`)
- 모범 사례: 처음에 게시하지 않더라도 저장소 경로를 사용
- 임시 모듈의 경우 모듈 이름과 함께 소유하거나 통제하는 도메인 사용
- 저장소 위치가 불확실한 경우 `<회사명>/stringtools`와 같은 안전한 대체물 사용

---

#### `go`

**목적**: 모듈이 작성된 Go 버전을 나타냄

**구문**:
```
go minimum-go-version
```

**매개변수**:
- `minimum-go-version`: 이 모듈의 패키지를 컴파일하는 데 필요한 최소 Go 버전

**예시**:
```
go 1.14
```

**참고**:

- **Go 1.21+**: 필수 요구 사항(Go 툴체인이 더 새로운 버전을 선언하는 모듈을 거부)
- **1.21 이전**: 권고 사항만
- Go 툴체인 선택에 영향("[Go 툴체인](/doc/toolchain)" 참조)
- 언어 기능 제한: 컴파일러가 지정된 버전 이후에 도입된 기능을 거부
- `go` 명령 동작에 영향:
  - **Go 1.14+**: `vendor/modules.txt`가 있고 일관되면 자동 벤더링이 활성화될 수 있음
  - **Go 1.16+**: `all` 패키지 패턴이 메인 모듈에서 전이적으로 임포트된 패키지만 매칭
  - **Go 1.17+**:
    - 전이적으로 임포트된 패키지를 제공하는 각 모듈에 대한 명시적 `require` 지시문
    - 간접 의존성이 별도의 블록에 기록됨
    - `go mod vendor`가 벤더된 의존성의 `go.mod`와 `go.sum` 파일을 생략
    - `go mod vendor`가 `vendor/modules.txt`에 각 의존성의 `go` 버전을 기록
  - **Go 1.21+**:
    - `go` 줄이 필요한 최소 버전을 선언
    - `go` 줄이 모든 의존성 이상이어야 함
    - 더 이상 이전 Go 버전과의 호환성을 유지하지 않음
    - `go.sum`의 체크섬에 더 신중함

**최대**: `go.mod` 파일당 하나의 `go` 지시문

---

#### `toolchain`

**목적**: 이 모듈에서 사용할 권장 Go 툴체인 선언

**구문**:
```
toolchain toolchain-name
```

**매개변수**:
- `toolchain-name`: 권장 툴체인 이름. 표준 형식: `go_V_` (예: `go1.21.0`, `go1.18rc1`)
  - 특수 값 `default`: 자동 툴체인 전환 비활성화

**예시**:
```
toolchain go1.21.0
```

**참고**:
- 모듈이 메인 모듈이고 기본 툴체인이 더 오래된 경우에만 효과가 있음
- 툴체인 선택에 대한 자세한 내용은 "[Go 툴체인](/doc/toolchain)" 참조

---

#### `godebug`

**목적**: 메인 패키지에 대한 기본 [GODEBUG](/doc/godebug) 설정을 나타냄

**구문**:
```
godebug debug-key=debug-value
```

**매개변수**:
- `debug-key`: 설정 이름([GODEBUG 히스토리](/doc/godebug#history) 참조)
- `debug-value`: 설정 값(달리 지정되지 않는 한 비활성화는 `0`, 활성화는 `1`)

**예시**:
```
godebug asynctimerchan=0
```

```
godebug (
    default=go1.21
    panicnil=1
)
```

**참고**:
- 현재 모듈의 메인 패키지와 테스트 바이너리에만 적용됨
- 모듈이 의존성으로 사용될 때는 효과 없음
- 툴체인 기본값을 재정의하고, 메인 패키지의 명시적 `//go:debug` 줄에 의해 재정의됨
- 자세한 내용은 "[Go, 하위 호환성, 그리고 GODEBUG](/doc/godebug)" 참조

---

#### `require`

**목적**: 모듈을 의존성으로 선언하고 최소 필요 버전을 지정

**구문**:
```
require module-path module-version
```

**매개변수**:
- `module-path`: 일반적으로 저장소 도메인과 모듈 이름의 연결(v2+는 `/v2`로 끝나야 함)
- `module-version`: 릴리스 버전(예: `v1.2.3`) 또는 의사 버전(예: `v0.0.0-20200921210052-fa0125251cc4`)

**예시**:
```
require example.com/othermodule v1.2.3
```

```
require example.com/othermodule v0.0.0-20200921210052-fa0125251cc4
```

**참고**:
- 패키지 임포트 시 `go` 명령에 의해 자동으로 추가됨
- Go는 태그가 없는 버전에 대해 의사 버전 번호를 할당
- `replace` 지시문과 결합하여 비저장소 위치를 사용할 수 있음
- 의사 버전은 버전이 아직 태그되지 않았을 때 Go 도구에 의해 생성됨

**관련 주제**:
- [의존성 추가](/doc/modules/managing-dependencies#adding_dependency)
- [특정 의존성 버전 가져오기](/doc/modules/managing-dependencies#getting_version)
- [사용 가능한 업데이트 발견](/doc/modules/managing-dependencies#discovering_updates)
- [의존성 업그레이드 또는 다운그레이드](/doc/modules/managing-dependencies#upgrading)
- [코드의 의존성 동기화](/doc/modules/managing-dependencies#synchronizing)
- [모듈 버전 번호 매기기](/doc/modules/version-numbers)

---

#### `tool`

**목적**: 패키지를 의존성으로 추가하고 `go tool`로 실행할 수 있게 함

**구문**:
```
tool package-path
```

**매개변수**:
- `package-path`: 도구의 패키지 경로(모듈 경로와 모듈 내 패키지 경로의 연결)

**예시**:

현재 모듈의 도구:
```
module example.com/mymodule

tool example.com/mymodule/cmd/mytool
```

별도 모듈의 도구:
```
module example.com/mymodule

tool example.com/atool/cmd/atool

require example.com/atool v1.2.3
```

**참고**:
- `go tool mytool` 또는 전체 경로 `go tool example.com/mymodule/cmd/mytool`로 실행
- 워크스페이스 모드에서는 모든 워크스페이스 모듈의 도구를 실행할 수 있음
- 도구 모듈 버전을 선택하려면 `require` 지시문이 필요
- `replace` 및 `exclude` 지시문이 도구와 의존성에 적용됨
- [도구 의존성](/doc/modules/managing-dependencies#tools) 참조

---

#### `replace`

**목적**: 모듈 버전을 다른 모듈 버전이나 로컬 디렉터리로 대체

**구문**:
```
replace module-path [module-version] => replacement-path [replacement-version]
```

**매개변수**:
- `module-path`: 대체할 모듈 경로
- `module-version` (선택): 대체할 특정 버전; 생략하면 모든 버전이 대체됨
- `replacement-path`: 대체할 모듈 경로 또는 로컬 디렉터리 경로
- `replacement-version` (선택): replacement-path가 모듈 경로(로컬 디렉터리가 아님)인 경우에만

**예시**:

포크된 저장소로 대체:
```
require example.com/othermodule v1.2.3

replace example.com/othermodule => example.com/myfork/othermodule v1.2.3-fixed
```

다른 버전으로 대체:
```
require example.com/othermodule v1.2.2

replace example.com/othermodule => example.com/othermodule v1.2.3
```

특정 버전 대체:
```
replace example.com/othermodule v1.2.5 => example.com/othermodule v1.2.3
```

로컬 코드로 대체(모든 버전):
```
require example.com/othermodule v1.2.3

replace example.com/othermodule => ../othermodule
```

특정 버전을 로컬 코드로 대체:
```
require example.com/othermodule v1.2.5

replace example.com/othermodule v1.2.5 => ../othermodule
```

**참고**:
- 대체 시 임포트 문을 변경하지 마세요—원래 모듈 경로를 사용
- 메인 모듈에서만 적용됨; 의존성에서는 무시됨
- 수정 사항을 개발하거나 테스트할 때 모듈 경로를 임시로 대체
- 특정 버전 없이 `require`와 함께 가짜 버전 사용:
  ```
  require example.com/mod v0.0.0-replace

  replace example.com/mod v0.0.0-replace => ./mod
  ```
- 참고: `replace`가 메인 모듈에서만 적용되므로 이는 종속 모듈을 손상시킴

**사용 사례**:
- 아직 저장소에 없는 새 모듈 코드 테스트
- 복제된 의존성 저장소의 수정 사항 테스트
- 의존성의 포크된 버전 사용
- 개발을 위해 로컬 모듈 코드 사용

**관련 주제**:
- [자신의 저장소 포크에서 외부 모듈 코드 요청](/doc/modules/managing-dependencies#external_fork)
- [로컬 디렉터리의 모듈 코드 요청](/doc/modules/managing-dependencies#local_directory)
- [모듈 버전 번호 매기기](/doc/modules/version-numbers)

---

#### `exclude`

**목적**: 의존성 그래프에서 특정 모듈 버전을 제외

**구문**:
```
exclude module-path module-version
```

**매개변수**:
- `module-path`: 제외할 모듈 경로
- `module-version`: 제외할 특정 버전

**예시**:
```
exclude example.com/theirmodule v1.3.0
```

**참고**:
- 로드할 수 없는 간접 필요 모듈에 사용(예: 유효하지 않은 체크섬)
- 메인 모듈에서만 적용됨; 의존성에서는 무시됨
- `go mod edit`를 사용하여 설정 가능:
  ```bash
  go mod edit -exclude=example.com/theirmodule@v1.3.0
  ```
- [모듈 버전 번호 매기기](/doc/modules/version-numbers) 참조

---

#### `retract`

**목적**: 의존해서는 안 되는 버전 또는 버전 범위를 나타냄

**구문**:
```
retract version // rationale
retract [version-low,version-high] // rationale
```

**매개변수**:
- `version`: 철회할 단일 버전
- `version-low`: 철회 범위의 하한(포함)
- `version-high`: 철회 범위의 상한(포함)
- `rationale` (선택): 철회 이유를 설명하는 주석

**예시**:

단일 버전:
```
retract v1.1.0 // 실수로 게시됨.
```

버전 범위:
```
retract [v1.0.0,v1.0.5] // 일부 플랫폼에서 빌드 실패.
```

**참고**:
- 철회된 버전은 `go get`, `go mod tidy` 또는 다른 명령을 통해 자동으로 업그레이드되지 않음
- `go list -m -u`에서 사용 가능한 업데이트로 표시되지 않음
- 철회된 버전은 기존 의존자를 위해 계속 사용 가능해야 함
- 삭제된 버전은 미러(예: proxy.golang.org)에 남아 있을 수 있음
- 철회된 버전에 의존하는 사용자는 `go get` 또는 `go list -m -u`를 통해 알림을 받음
- 최신 모듈 버전의 `retract` 지시문을 읽어서 발견됨
- 최신 버전은 (순서대로) 다음에 의해 결정됨:
  1. 가장 높은 릴리스 버전
  2. 가장 높은 프리릴리스 버전
  3. 저장소의 기본 브랜치 팁에 대한 의사 버전

**철회 게시**:
- 발견을 위해 거의 항상 새로운, 더 높은 버전을 태그해야 함
- 철회를 알리기 위해서만 버전을 게시할 수 있음
- 새 버전이 자신을 철회할 수 있음:
  ```
  retract v1.0.0 // 실수로 게시됨.
  retract v1.0.1 // 철회만 포함.
  ```

**중요**: 게시되면 버전을 변경할 수 없음. 나중에 다른 커밋에 태그하면 `go.sum` 또는 체크섬 데이터베이스에서 체크섬 불일치가 발생할 수 있음.

**발견**:
- 철회된 버전은 `go list -m -versions` 출력에 표시되지 않음
- `-retracted` 플래그를 사용하여 표시: `go list -m -versions -retracted`
- [`go list -m`](/ref/mod#go-list-m) 레퍼런스 참조

---

### 요약 표

| 지시문 | 목적 | 필수 |
|--------|------|------|
| `module` | 모듈 경로 선언 | 예 |
| `go` | 최소 Go 버전 설정 | 권장 |
| `toolchain` | Go 툴체인 권장 | 선택 |
| `godebug` | GODEBUG 기본값 설정 | 선택 |
| `require` | 의존성 선언 | 필요 시 |
| `tool` | 도구 의존성 선언 | 선택 |
| `replace` | 모듈 버전/경로 대체 | 선택 |
| `exclude` | 모듈 버전 제외 | 선택 |
| `retract` | 버전 철회 | 선택 |


---

## Go 모듈 레퍼런스

> **원문:** https://go.dev/ref/mod

### 소개

모듈은 Go가 의존성을 관리하는 방식입니다. 이 문서는 Go의 모듈 시스템에 대한 상세한 레퍼런스 매뉴얼로, Go 프로젝트의 생성, 버전 관리, 배포를 다룹니다.

### 모듈, 패키지, 버전

#### 핵심 개념

**모듈**은 함께 릴리스, 버전 관리, 배포되는 패키지의 모음입니다. 모듈은 `go.mod` 파일에 선언된 모듈 경로로 식별됩니다.

- **모듈 경로**: 모듈의 정규 이름 (예: `golang.org/x/net`)
- **패키지 경로**: 모듈 경로 + 하위 디렉터리 (예: `golang.org/x/net/html`)
- **모듈 루트**: `go.mod` 파일이 있는 디렉터리

#### 버전

버전은 시맨틱 버전 관리를 따릅니다 (예: `v1.2.3`):

```
vMAJOR.MINOR.PATCH[-prerelease][+metadata]
```

예시: `v0.0.0`, `v1.12.134`, `v8.0.5-pre`, `v2.0.9+meta`

**버전 규칙:**
- **메이저 버전**: 하위 호환되지 않는 변경 시 증가
- **마이너 버전**: 하위 호환되는 기능 추가 시 증가
- **패치 버전**: 버그 수정 시 증가
- **프리릴리스**: `-` 접미사가 붙은 버전(예: `v1.2.3-pre`)은 불안정함
- **빌드 메타데이터**: `+` 접미사는 비교 시 무시됨 (`+incompatible` 제외)

불안정 버전(메이저 버전 0 또는 프리릴리스)은 호환성을 보장하지 않습니다.

#### 의사 버전(Pseudo-versions)

의사 버전은 VCS의 특정 리비전 정보를 인코딩합니다:

```
vX.0.0-yyyymmddhhmmss-abcdefabcdef (기본 버전 없음)
vX.Y.Z-pre.0.yyyymmddhhmmss-abcdefabcdef (프리릴리스 기반)
vX.Y.(Z+1)-0.yyyymmddhhmmss-abcdefabcdef (릴리스 기반)
```

**예시:** `v0.0.0-20191109021931-daa7c04131f5`

#### 메이저 버전 접미사

v2 이상의 모듈은 모듈 경로에 메이저 버전 접미사가 필요합니다:
- `v1.0.0` → 경로: `example.com/mod`
- `v2.0.0` → 경로: `example.com/mod/v2`

이를 통해 동일한 빌드에서 여러 메이저 버전이 공존할 수 있습니다.

### `go.mod` 파일 형식

#### 기본 구조

```go
module example.com/my/thing

go 1.23.0

require example.com/other/thing v1.0.2
require example.com/new/thing/v2 v2.3.4
exclude example.com/old/thing v1.2.3
replace example.com/bad/thing v1.4.5 => example.com/good/thing v1.4.5
retract [v1.9.0, v1.9.5]
```

#### 지시문

##### `module` 지시문

```
module golang.org/x/net
```

메인 모듈의 경로를 선언합니다. 정확히 한 번만 필요합니다.

**사용 중단(Deprecation):**
```
// Deprecated: use example.com/mod/v2 instead.
module example.com/mod
```

##### `go` 지시문

```
go 1.23.0
```

필요한 최소 Go 버전을 지정합니다. 언어 기능 사용 가능 여부와 컴파일러 동작을 설정합니다.

##### `require` 지시문

```
require (
    golang.org/x/crypto v1.4.5 // indirect
    golang.org/x/text v1.6.7
)
```

최소 필요 모듈 버전을 선언합니다. `// indirect` 주석은 해당 모듈에서 직접 임포트하지 않음을 나타냅니다.

##### `exclude` 지시문

```
exclude (
    golang.org/x/crypto v1.4.5
    golang.org/x/text v1.6.7
)
```

특정 버전의 로드를 방지합니다. 메인 모듈의 `go.mod`에서만 적용됩니다.

##### `replace` 지시문

```
replace (
    golang.org/x/net v1.2.3 => example.com/fork/net v1.4.5
    golang.org/x/net => example.com/fork/net v1.4.5
    golang.org/x/net v1.2.3 => ./fork/net
    golang.org/x/net => ./fork/net
)
```

모듈 버전을 대체 모듈(로컬 경로 또는 다른 모듈)로 교체합니다. 메인 모듈의 `go.mod`에서만 적용됩니다.

##### `retract` 지시문

```
retract (
    v1.0.0 // 실수로 게시됨.
    [v1.0.0, v1.9.9] // 철회만 포함.
)
```

버전을 사용에 부적합하다고 표시합니다. 사용자는 철회된 버전으로 자동 업그레이드되지 않습니다.

##### `tool` 지시문

```
tool (
    golang.org/x/tools/cmd/stringer
    example.com/module/cmd/a
)
```

`go tool`을 통해 사용할 수 있는 도구로 패키지를 추가합니다.

##### `ignore` 지시문

```
ignore (
    ./node_modules
    static
    ./third_party/javascript
)
```

패키지 패턴 매칭 시 지정된 디렉터리를 무시하도록 go 명령에 지시합니다.

##### `toolchain` 지시문

```
toolchain go1.21.0
```

이 모듈에서 사용할 권장 Go 툴체인을 선언합니다.

##### `godebug` 지시문

```
godebug (
    panicnil=1
    asynctimerchan=0
)
```

이 모듈이 메인 모듈일 때 적용할 GODEBUG 설정을 선언합니다.

### 최소 버전 선택(MVS)

MVS는 모듈 버전을 선택하는 Go의 알고리즘입니다:

1. 메인 모듈에서 시작하여 의존성 그래프를 순회
2. 각 모듈의 가장 높은 필요 버전을 추적
3. **빌드 목록** 생성 - 빌드에 사용될 모듈 버전 목록

**핵심 원칙:** 빌드 목록에는 모든 요구 사항을 충족하는 최소 버전이 포함됩니다.

#### 예시

```
Main은 다음을 필요로 함:
  - A ≥ 1.2
  - B ≥ 1.2

A 1.2는 C ≥ 1.3을 필요로 함
B 1.2는 C ≥ 1.4를 필요로 함
C 1.3은 D ≥ 1.2를 필요로 함
C 1.4는 D ≥ 1.2를 필요로 함

결과: A 1.2, B 1.2, C 1.4, D 1.2
```

필요하지 않은 한 더 높은 버전은 선택되지 않습니다.

#### 교체와 제외

**교체:**
```
replace golang.org/x/net v1.2.3 => example.com/fork/net v1.4.5
```
하나의 모듈을 다른 모듈로 교체합니다(의존성이 다를 수 있음).

**제외:**
```
exclude golang.org/x/net v1.2.3
```
그래프에서 버전을 제거합니다. 의존하는 측은 다음으로 높은 버전을 사용합니다.

### `go.work` 파일 (워크스페이스)

워크스페이스를 통해 여러 모듈을 함께 개발할 수 있습니다:

```
go 1.23.0

use (
    ./my/first/thing
    ./my/second/thing
)

replace example.com/bad/thing v1.4.5 => example.com/good/thing v1.4.5
```

#### 지시문

##### `go` 지시문
```
go 1.23.0
```

필수. 워크스페이스의 Go 버전을 지정합니다.

##### `use` 지시문
```
use (
    ./mymod
    ../othermod
    ./subdir/thirdmod
)
```

워크스페이스에 모듈을 추가합니다.

##### `replace` 지시문
`go.mod`와 유사하지만 워크스페이스 모듈의 모든 replace를 재정의합니다.

### 모듈 인식 명령

#### 빌드 명령

다음 명령들은 모두 모듈을 인식합니다:
- `go build`
- `go fix`
- `go generate`
- `go install`
- `go list`
- `go run`
- `go test`
- `go vet`

#### 일반 플래그

```bash
-mod=mod      # go.mod 자동 업데이트 (누락된 패키지 찾기)
-mod=readonly # go.mod 업데이트 필요 시 오류 보고 (기본값)
-mod=vendor   # vendor 디렉터리 사용, 네트워크/캐시 사용 안 함
```

#### `go get`

```bash
# 특정 모듈 업그레이드
go get golang.org/x/net

# 임포트된 패키지를 제공하는 모듈 업그레이드
go get -u ./...

# 버전 지정
go get golang.org/x/text@v0.3.2

# 브랜치로 업데이트
go get golang.org/x/text@master

# 의존성 제거
go get golang.org/x/text@none

# 최소 Go 버전 업그레이드
go get go

# 권장 툴체인 업그레이드
go get toolchain
go get toolchain@patch
```

**플래그:**
- `-d`: 빌드/설치하지 않고 의존성만 관리
- `-u`: 모든 의존성 업그레이드
- `-u=patch`: 최신 패치 버전으로 업그레이드
- `-t`: 테스트 의존성 고려

#### `go install`

```bash
# 최신 버전 설치 (로컬 go.mod 무시)
go install golang.org/x/tools/gopls@latest

# 특정 버전 설치
go install golang.org/x/tools/gopls@v0.6.4

# 로컬 go.mod 버전 사용하여 설치
go install golang.org/x/tools/gopls

# 디렉터리의 모든 프로그램 설치
go install ./cmd/...
```

Go 1.16부터 프로그램 빌드 및 설치에 선호됩니다.

#### `go list -m`

```bash
# 모든 의존성 나열
go list -m all

# 모듈의 버전 나열
go list -m -versions example.com/m

# 업그레이드 정보와 함께 나열
go list -m -u all

# JSON 출력
go list -m -json example.com/m@latest

# 철회된 버전 표시
go list -m -retracted example.com/m@latest
```

**Module 구조체 필드:**
- `Path`: 모듈 경로
- `Version`: 선택된 버전
- `Versions`: 사용 가능한 모든 버전
- `Replace`: 대체 모듈
- `Update`: 사용 가능한 업데이트 (`-u` 사용 시)
- `Retracted`: 철회 정보
- `Deprecated`: 사용 중단 메시지
- `Dir`: 모듈 파일이 있는 디렉터리
- `GoMod`: go.mod 파일 경로
- `GoVersion`: 모듈에서 사용된 Go 버전

#### `go mod download`

```bash
# 모든 의존성 다운로드
go mod download

# 특정 모듈 다운로드
go mod download golang.org/x/mod@v0.2.0

# JSON 출력
go mod download -json

# 실행된 명령 표시
go mod download -x
```

#### `go mod tidy`

```bash
go mod tidy
```

누락된 요구 사항을 추가하고 사용되지 않는 것을 제거합니다. `go.mod`와 `go.sum`을 업데이트합니다.

#### `go mod vendor`

```bash
go mod vendor
```

모든 의존성의 복사본과 `vendor/modules.txt` 매니페스트가 있는 `vendor/` 디렉터리를 생성합니다.

#### `go mod init`

```bash
go mod init example.com/mymodule
```

새 `go.mod` 파일을 생성합니다.

#### `go mod edit`

```bash
go mod edit -module=example.com/newname
go mod edit -go=1.21
go mod edit -require=golang.org/x/text@v0.3.0
```

`go.mod` 파일의 저수준 편집.

### 모듈 그래프 가지치기 (Go 1.17+)

`go 1.17` 이상의 모듈에서는 모듈 그래프가 가지치기됩니다:

- `go 1.17+`를 지정하는 의존성의 **직접적인** 요구 사항만 포함
- `go 1.17+` 모듈의 전이적 의존성은 가지치기됨
- `go 1.16` 이하 의존성은 전체 전이적 클로저를 포함

**이점:** 더 작은 빌드 목록과 빠른 모듈 로딩.

#### 지연 모듈 로딩 (Go 1.17+)

`go` 명령은 필요할 때까지 전체 모듈 그래프 로딩을 피합니다:

1. 메인 모듈의 `go.mod`만 로드
2. 요청된 패키지 로드 시도
3. 패키지를 찾지 못하면 필요 시 전체 그래프 로드
4. 로드된 모듈의 로컬 일관성 검증

### 벤더링

다음으로 벤더된 의존성을 생성합니다:

```bash
go mod vendor
```

생성되는 것:
- 모듈 복사본이 있는 `vendor/` 디렉터리
- `vendor/modules.txt` 매니페스트

**사용:**
```bash
go build -mod=vendor        # vendor 디렉터리 사용
go mod vendor -o=...        # vendor 업데이트
```

벤더링 활성화:
- `go.mod`가 `go 1.14+`를 지정하고 `vendor/`가 존재하면 자동
- `-mod=vendor`로 명시적 활성화

### 환경 변수

#### 주요 모듈 환경 변수

- `GO111MODULE`: `on`, `off`, 또는 `auto` (모듈 인식 모드 제어)
- `GOMODCACHE`: 모듈 캐시 위치 (기본값: `$GOPATH/pkg/mod`)
- `GOPROXY`: 쉼표로 구분된 프록시 URL 목록
- `GOSUMDB`: 체크섬 데이터베이스 URL
- `GOPRIVATE`: 비공개 모듈의 쉼표로 구분된 패턴
- `GONOPROXY`: 직접 가져올 패턴 (프록시 경유 안 함)
- `GOINSECURE`: 안전하지 않은 스킴을 허용할 패턴
- `GOWORK`: `go.work` 파일 경로 (또는 워크스페이스 모드 비활성화를 위해 `off`)

### 버전 쿼리

`go get` 및 기타 명령에서 지원:

```bash
@latest          # 최신 릴리스 또는 프리릴리스
@upgrade         # 최신 버전 (go get의 기본값)
@patch           # 최신 패치 릴리스
v1.2.3           # 특정 버전
v1.2             # 버전 접두사
master           # 브랜치 이름
daa7c041         # 커밋 해시 (의사 버전으로 변환됨)
```

### 모듈이 없는 저장소

Go는 `go.mod`가 없는 저장소에서도 작동할 수 있습니다:

- 필요 시 `go.mod`를 합성
- 모듈 프록시를 사용하여 합성된 `go.mod` 제공
- 메이저 버전 2+로 태그된 버전은 `+incompatible` 접미사를 얻음

예시:
```
require example.com/m v4.1.2+incompatible
```

### 요약

Go 모듈은 다음을 제공합니다:
- **버전 관리**: 메이저 버전 접미사를 통한 시맨틱 버전 관리
- **의존성 관리**: 결정론적 빌드를 위한 MVS 알고리즘
- **유연성**: 특수한 경우를 위한 replace 및 exclude 지시문
- **재현성**: 잠금 파일(`go.sum`)과 최소 버전 선택
- **도구**: 관리를 위한 풍부한 `go mod` 명령 세트

모든 모듈 정보는 사람이 읽고 기계가 쓸 수 있는 `go.mod` 파일에 선언적으로 지정됩니다.
