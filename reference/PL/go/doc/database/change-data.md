# 데이터를 반환하지 않는 SQL 문 실행

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
