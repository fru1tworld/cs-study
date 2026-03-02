# 데이터베이스 핸들 열기

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
