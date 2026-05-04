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
