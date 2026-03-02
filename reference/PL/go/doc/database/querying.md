# 데이터 쿼리

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
