# 제13장. 동시성 제어 (Concurrency Control)

이 장에서는 여러 세션이 동시에 같은 데이터에 접근할 때 PostgreSQL의 동작을 설명합니다. 목표는 모든 세션에 대해 효율적인 접근을 허용하면서 엄격한 데이터 무결성을 유지하는 것입니다.

---

## 13.1. 소개 (Introduction)

PostgreSQL은 동시 데이터 접근을 관리하면서 데이터 일관성을 유지하기 위해 핵심 메커니즘으로 다중 버전 동시성 제어(MVCC, Multiversion Concurrency Control) 를 사용합니다.

### MVCC란?

MVCC는 다중 버전 모델로, 각 SQL 문은 특정 시점에 존재했던 데이터 스냅샷(데이터베이스 버전)을 봅니다. 이는 기반 데이터의 현재 상태와 무관합니다.

- 같은 행을 수정하는 동시 트랜잭션에 의해 생성된 일관성 없는 데이터를 문장이 보는 것을 방지합니다.
- 각 데이터베이스 세션에 트랜잭션 격리 를 제공합니다.

### 주요 장점

1. 논블로킹 동시성
   - 읽기 잠금과 쓰기 잠금이 충돌하지 않습니다.
   - 읽기가 쓰기를 절대 차단하지 않습니다.
   - 쓰기가 읽기를 절대 차단하지 않습니다.

2. 잠금 경합 최소화
   - 전통적인 잠금 방법론을 회피합니다.
   - 다중 사용자 환경에서 합리적인 성능을 제공합니다.

3. 엄격한 격리 수준
   - PostgreSQL은 가장 엄격한 격리 수준에서도 읽기/쓰기 논블로킹 보장을 유지합니다.
   - 혁신적인 직렬화 가능 스냅샷 격리(SSI, Serializable Snapshot Isolation) 를 사용합니다.

### 대체 잠금 옵션

PostgreSQL은 다음과 같은 애플리케이션을 위해 명시적 잠금 메커니즘도 제공합니다:
- 전체 트랜잭션 격리가 필요하지 않은 경우
- 수동으로 충돌 지점을 관리하고 싶은 경우:
  - 테이블 수준 잠금
  - 행 수준 잠금
  - 권고 잠금(Advisory Lock) - 애플리케이션 정의, 단일 트랜잭션에 묶이지 않음

### 성능 고려사항

> MVCC를 적절히 사용하면 일반적으로 명시적 잠금보다 더 나은 성능을 제공합니다.

---

## 13.2. 트랜잭션 격리 (Transaction Isolation)

PostgreSQL은 SQL 표준의 네 가지 트랜잭션 격리 수준을 구현하지만, 내부적으로는 세 가지 구분된 수준만 구현됩니다. Read Uncommitted는 Read Committed와 동일하게 동작합니다. 이는 PostgreSQL의 다중 버전 동시성 제어(MVCC) 아키텍처에 맞춰 의도적으로 설계된 것입니다.

### 트랜잭션 격리 현상

표준은 다양한 격리 수준에서 방지해야 하는 네 가지 현상을 정의합니다:

- 더티 읽기(Dirty Read): 트랜잭션이 커밋되지 않은 동시 트랜잭션이 쓴 데이터를 읽음
- 비반복적 읽기(Nonrepeatable Read): 트랜잭션이 데이터를 다시 읽을 때 다른 커밋된 트랜잭션에 의해 수정되어 있음
- 팬텀 읽기(Phantom Read): 트랜잭션이 쿼리를 다시 실행할 때 다른 트랜잭션으로 인해 결과 집합이 변경됨
- 직렬화 이상(Serialization Anomaly): 커밋된 트랜잭션 그룹이 모든 가능한 순차 실행 순서와 일치하지 않는 결과를 생성함

### 격리 수준 비교표

| 격리 수준 | 더티 읽기 | 비반복적 읽기 | 팬텀 읽기 | 직렬화 이상 |
|-----------|-----------|---------------|-----------|-------------|
| Read Uncommitted | 허용됨 (PG에서는 아님) | 가능 | 가능 | 가능 |
| Read Committed | 불가능 | 가능 | 가능 | 가능 |
| Repeatable Read | 불가능 | 불가능 | 허용됨 (PG에서는 아님) | 가능 |
| Serializable | 불가능 | 불가능 | 불가능 | 불가능 |

### 격리 수준 설정

`SET TRANSACTION` 명령을 사용합니다:

```sql
SET TRANSACTION ISOLATION LEVEL { SERIALIZABLE | REPEATABLE READ | READ COMMITTED | READ UNCOMMITTED };
```

---

### 13.2.1. Read Committed 격리 수준

Read Committed 는 PostgreSQL의 기본 격리 수준입니다.

#### 동작 방식

- `SELECT` 쿼리는 쿼리가 시작되기 전에 커밋된 데이터만 봅니다.
- 커밋되지 않은 데이터나 쿼리 실행 중에 동시 트랜잭션이 커밋한 변경사항을 절대 보지 않습니다.
- 쿼리가 시작되는 순간의 데이터베이스 스냅샷을 효과적으로 제공합니다.
- `SELECT`는 자신의 트랜잭션 내에서 이전 업데이트의 효과를 봅니다.
- 단일 트랜잭션 내의 두 연속 `SELECT` 명령은 다른 트랜잭션이 그 사이에 커밋하면 다른 데이터를 볼 수 있습니다.

#### 명령 동작

`UPDATE`, `DELETE`, `SELECT FOR UPDATE`, `SELECT FOR SHARE`:
- 명령 시작 시점에 커밋된 대상 행을 찾습니다.
- 대상 행이 다른 동시 트랜잭션에 의해 업데이트되었으면 대기합니다.
- 첫 번째 업데이터가 롤백하면: 두 번째 업데이터가 원래 발견한 행으로 진행할 수 있습니다.
- 첫 번째 업데이터가 커밋하면: 두 번째 업데이터는 삭제된 행을 무시하거나 업데이트된 버전으로 진행합니다.
- 업데이트된 행에서 검색 조건을 다시 평가합니다.

`INSERT ... ON CONFLICT DO UPDATE`:
- 각 행은 삽입되거나 업데이트됩니다.
- Read Committed 모드에서는 UPDATE 절이 명령에 통상적으로 보이지 않는 행에 영향을 줄 수 있습니다(충돌이 다른 트랜잭션에서 발생한 경우).

`INSERT ... ON CONFLICT DO NOTHING`:
- INSERT 스냅샷에 아직 보이지 않는 다른 트랜잭션의 결과로 인해 삽입이 진행되지 않을 수 있습니다.

`MERGE`:
- INSERT, UPDATE, DELETE의 다양한 조합을 지정할 수 있습니다.
- 행이 동시에 업데이트되었지만 조인 조건이 여전히 통과하면: MERGE는 업데이트된 버전에서 작업을 수행합니다.
- 조인 조건이 실패하면: MERGE는 NOT MATCHED 작업을 평가합니다.
- 행이 삭제되면: MERGE는 NOT MATCHED [BY TARGET] 작업을 평가합니다.
- 조건은 업데이트된 버전의 행에서 다시 평가됩니다.

#### 예제: 계좌 이체

```sql
BEGIN;
UPDATE accounts SET balance = balance + 100.00 WHERE acctnum = 12345;
UPDATE accounts SET balance = balance - 100.00 WHERE acctnum = 7534;
COMMIT;
```

각 명령은 영향을 주는 행의 업데이트된 버전을 보며, 불일치를 방지합니다.

#### 문제가 되는 예제

```sql
BEGIN;
UPDATE website SET hits = hits + 1;
-- 다른 세션에서 실행:  DELETE FROM website WHERE hits = 10;
COMMIT;
```

`DELETE`는 아무 효과가 없습니다. 업데이트 전 행 값(9)이 건너뛰어지고, DELETE가 잠금을 획득할 때 새 값은 11(10이 아님)이기 때문입니다. 이는 Read Committed 모드가 단일 명령에 걸쳐 절대적인 일관성을 제공하지 않을 수 있음을 보여줍니다.

#### 요약

- 빠르고 사용하기 간단합니다.
- 많은 애플리케이션에 적합합니다.
- 엄격한 일관성이 필요한 복잡한 쿼리와 업데이트에는 불충분합니다.

---

### 13.2.2. Repeatable Read 격리 수준

#### 동작 방식

- 트랜잭션이 시작되기 전에 커밋된 데이터만 봅니다.
- 커밋되지 않은 데이터나 트랜잭션 실행 중에 동시 트랜잭션이 커밋한 변경사항을 절대 보지 않습니다.
- 각 쿼리는 자신의 트랜잭션 내에서 이전 업데이트의 효과를 봅니다(아직 커밋되지 않음).
- SQL 표준보다 강한 보장: 더티 읽기, 비반복적 읽기, 팬텀 읽기를 방지합니다(직렬화 이상 제외).

#### Read Committed와의 주요 차이점

- 스냅샷은 각 문장이 아닌 트랜잭션의 첫 번째 비트랜잭션 제어 문장 시작 시 촬영됩니다.
- 단일 트랜잭션 내에서 연속적인 `SELECT` 명령은 같은 데이터를 봅니다.
- 트랜잭션이 시작된 후 커밋된 다른 트랜잭션의 변경사항을 보지 않습니다.

#### 명령 동작

`UPDATE`, `DELETE`, `MERGE`, `SELECT FOR UPDATE`, `SELECT FOR SHARE`:
- 트랜잭션 시작 시점에 커밋된 대상 행을 찾습니다.
- 대상 행이 다른 동시 트랜잭션에 의해 업데이트되었으면 대기합니다.
- 첫 번째 업데이터가 롤백하면: Repeatable Read 트랜잭션은 원래 발견한 행으로 진행합니다.
- 첫 번째 업데이터가 커밋하면: Repeatable Read 트랜잭션은 오류와 함께 롤백됩니다:
  ```
  ERROR:  could not serialize access due to concurrent update
  ```

#### 오류 처리

애플리케이션은 트랜잭션을 재시도할 준비가 되어 있어야 합니다:

```
직렬화 오류를 받으면:
1. 현재 트랜잭션을 중단
2. 처음부터 전체 트랜잭션을 재시도
3. 두 번째 시도는 이전에 커밋된 변경을 초기 뷰의 일부로 봄
4. 새 버전의 행을 시작점으로 사용하여 논리적 충돌 없음
```

참고: 업데이트하는 트랜잭션만 재시도가 필요합니다. 읽기 전용 트랜잭션은 직렬화 충돌이 절대 없습니다.

#### 중요한 주의사항

Repeatable Read가 안정적인 뷰를 제공하지만, 일부 순차 실행과 일관되지 않을 수 있습니다. 예:

> 이 수준의 읽기 전용 트랜잭션도 배치가 완료되었음을 나타내도록 업데이트된 제어 레코드를 볼 수 있지만, 제어 레코드의 이전 리비전을 읽었기 때문에 (배치의 논리적 일부인) 상세 레코드를 보지 못할 수 있습니다.

비즈니스 규칙을 강제하기 위해 명시적 잠금을 신중히 사용해야 합니다.

#### 구현

- _스냅샷 격리_ 기술을 사용합니다.
- 전통적인 잠금 시스템과 동작 및 성능의 차이가 있습니다.
- 자세한 정보는 학술 문헌을 참조하세요(문서 참조 섹션 참조).

#### 역사적 참고

PostgreSQL 9.1 이전에는 Serializable 격리를 요청하면 여기 설명된 동작을 제공했습니다. 레거시 동작을 유지하려면 Repeatable Read를 요청하세요.

---

### 13.2.3. Serializable 격리 수준

#### 동작 방식

- 가장 엄격한 트랜잭션 격리
- 모든 커밋된 트랜잭션에 대해 직렬 트랜잭션 실행을 에뮬레이션합니다.
- 마치 트랜잭션이 동시가 아닌 순차적으로 하나씩 실행된 것처럼 동작합니다.
- 애플리케이션은 직렬화 실패로 인해 트랜잭션을 재시도할 준비가 되어 있어야 합니다.
- Repeatable Read와 정확히 같지만 비직렬 동작을 유발할 수 있는 조건을 모니터링합니다.
- _직렬화 이상_을 유발할 수 있는 조건을 감지합니다.

#### 모니터링

- Repeatable Read를 넘어서는 추가 차단 없음
- 모니터링에 약간의 오버헤드
- 직렬화 이상 조건 감지 시 직렬화 실패를 트리거합니다.

#### 예제: 직렬화 이상

초기 테이블 상태:
```
 class | value
-------+-------
     1 |    10
     1 |    20
     2 |   100
     2 |   200
```

트랜잭션 A:
```sql
SELECT SUM(value) FROM mytab WHERE class = 1;
-- 결과: 30
INSERT INTO mytab (class, value) VALUES (2, 30);
```

트랜잭션 B (동시):
```sql
SELECT SUM(value) FROM mytab WHERE class = 2;
-- 결과: 300
INSERT INTO mytab (class, value) VALUES (1, 300);
```

Repeatable Read에서: 둘 다 커밋됩니다 (일관성 없음)

Serializable에서: 하나는 커밋되고, 다른 하나는 롤백됩니다:
```
ERROR:  could not serialize access due to read/write dependencies among transactions
```

이유:
- A가 B보다 먼저 실행되면: B는 합계 = 330 (300이 아님)을 계산할 것입니다.
- B가 A보다 먼저 실행되면: A는 합계 != 30을 계산할 것입니다.

#### 지연 가능 트랜잭션 (Deferrable Transactions)

영구 테이블에서 읽은 데이터는 읽기 트랜잭션이 성공적으로 커밋될 때까지 유효하다고 간주되어서는 안 됩니다, 다음을 제외하고:
- _지연 가능_ 읽기 전용 트랜잭션: 데이터는 읽는 즉시 유효합니다.
- 이러한 트랜잭션은 이상 문제가 없음이 보장된 스냅샷을 획득할 때까지 읽기 전에 대기합니다.

#### 술어 잠금 (Predicate Locking)

PostgreSQL은 진정한 직렬화 가능성을 보장하기 위해 _술어 잠금_을 사용합니다:

주요 특성:
- 쓰기가 이전 읽기에 영향을 미쳤을 시점을 결정하는 잠금을 유지합니다.
- 차단을 유발하지 않으며 교착 상태를 유발할 수 없습니다.
- 동시 Serializable 트랜잭션 간의 종속성을 식별하고 표시합니다.
- 트랜잭션이 실제로 접근한 데이터를 기반으로 합니다.

Read Committed/Repeatable Read와의 대조:
- 전체 테이블 잠금 또는 `SELECT FOR UPDATE`/`SELECT FOR SHARE`가 필요할 수 있습니다.
- 다른 트랜잭션을 차단하고 디스크 접근을 유발할 수 있습니다.

#### SIRead 잠금

- `pg_locks` 시스템 뷰에 `SIReadLock` 모드로 나타납니다.
- 메모리 고갈을 방지하기 위해 여러 세분화된 잠금이 더 조대한 잠금으로 결합됩니다.
- READ ONLY 트랜잭션은 완료 전에 SIRead 잠금을 해제할 수 있습니다.
- 시작 시 술어 잠금을 취하지 않고 종종 해제됩니다.
- `SERIALIZABLE READ ONLY DEFERRABLE`은 이상 충돌이 불가능함이 확립될 때까지 차단됩니다.

#### 개발 단순화

일관된 Serializable 사용은 보장을 제공합니다: 성공적으로 커밋된 동시 Serializable 트랜잭션의 모든 집합은 직렬 실행과 같은 효과를 가집니다.

주요 요구사항:
- 단일 트랜잭션이 혼자 실행될 때 올바른 작업을 수행함을 보여주세요.
- Serializable 트랜잭션의 모든 조합에서 작동할 것이라는 확신
- 일반화된 직렬화 실패 처리 구현 (SQLSTATE '40001')
- 어떤 트랜잭션이 충돌에 기여하는지 예측하기 어려움

#### 잠재적 문제

PostgreSQL의 Serializable은 진정한 직렬 실행에서 발생하지 않는 오류를 허용할 수 있습니다:

예제: 고유 제약 조건 위반
```
명시적으로 키가 존재하지 않음을 확인한 후에도 동시 Serializable
트랜잭션에서 고유 제약 조건 위반을 볼 수 있습니다.
```

해결책: 잠재적으로 충돌하는 키를 삽입하는 모든 Serializable 트랜잭션은 명시적으로 먼저 확인해야 합니다:
- 기존 최대 키를 선택하고 1을 더합니다.
- 또는 키가 존재하지 않음을 명시적으로 선택하여 확인합니다.

#### 성능 최적화 권장사항

1. 가능한 경우 트랜잭션을 `READ ONLY`로 선언

2. 연결 풀을 사용하여 활성 연결 제어
   - Serializable 트랜잭션이 있는 바쁜 시스템에서 중요합니다.

3. 트랜잭션 범위 최소화
   - 무결성에 필요한 것만 포함합니다.

4. 유휴 트랜잭션 방지
   - `idle_in_transaction_session_timeout` 구성 매개변수를 사용합니다.

5. 가능한 경우 명시적 잠금 제거
   - `SELECT FOR UPDATE` 및 `SELECT FOR SHARE` 제거
   - Serializable이 자동 보호를 제공합니다.

6. 술어 잠금 메모리 관리
   - `max_pred_locks_per_transaction` 증가
   - `max_pred_locks_per_relation` 증가
   - `max_pred_locks_per_page` 증가
   - 여러 페이지 잠금을 관계 수준 잠금으로 결합하는 것을 방지합니다.

7. 순차 스캔 최적화
   - 순차 스캔은 관계 수준 술어 잠금을 필요로 합니다.
   - 직렬화 실패율을 증가시킵니다.
   - `random_page_cost` 감소
   - `cpu_tuple_cost` 증가
   - 감소된 롤백과 쿼리 실행 시간 변화의 균형을 고려합니다.

#### 구현

- _직렬화 가능 스냅샷 격리_ 기술을 사용합니다.
- 스냅샷 격리에 직렬화 이상 검사를 추가하여 구축합니다.
- 전통적인 잠금 시스템과 동작/성능의 차이가 있습니다.

### 중요 참고 사항

#### 시퀀스 동작

일부 PostgreSQL 데이터 유형은 특별한 트랜잭션 규칙을 가집니다:

시퀀스 (`serial` 열 포함):
- 변경 사항은 모든 트랜잭션에 즉시 표시됩니다.
- 트랜잭션이 중단되어도 변경 사항은 롤백되지 않습니다.
- 참조: 섹션 9.17 및 섹션 8.1.4

---

## 13.3. 명시적 잠금 (Explicit Locking)

PostgreSQL은 테이블의 데이터에 대한 동시 접근을 제어하기 위해 다양한 잠금 모드를 제공합니다. 이러한 모드는 MVCC가 원하는 동작을 제공하지 않을 때 애플리케이션 제어 잠금을 가능하게 합니다. 대부분의 PostgreSQL 명령은 실행 중 테이블 안전을 보장하기 위해 적절한 잠금을 자동으로 획득합니다.

미결 잠금을 보려면 `pg_locks` 시스템 뷰를 사용하세요.

---

### 13.3.1. 테이블 수준 잠금

모든 잠금 모드는 테이블 수준 잠금이지만, 일부 이름은 역사적입니다. 잠금은 호환성에 따라 충돌합니다 - 두 트랜잭션은 같은 테이블에 충돌하는 잠금을 동시에 유지할 수 없습니다.

#### 테이블 수준 잠금 모드

| 잠금 모드 | 충돌 대상 | 용도 |
|-----------|-----------|------|
| ACCESS SHARE | ACCESS EXCLUSIVE만 | `SELECT` 및 읽기 전용 쿼리 |
| ROW SHARE | EXCLUSIVE, ACCESS EXCLUSIVE | `SELECT FOR UPDATE`, `FOR NO KEY UPDATE`, `FOR SHARE`, `FOR KEY SHARE` |
| ROW EXCLUSIVE | SHARE, SHARE ROW EXCLUSIVE, EXCLUSIVE, ACCESS EXCLUSIVE | `UPDATE`, `DELETE`, `INSERT`, `MERGE` |
| SHARE UPDATE EXCLUSIVE | SHARE UPDATE EXCLUSIVE, SHARE, SHARE ROW EXCLUSIVE, EXCLUSIVE, ACCESS EXCLUSIVE | `VACUUM`, `ANALYZE`, `CREATE INDEX CONCURRENTLY`, `CREATE STATISTICS`, `COMMENT ON`, `REINDEX CONCURRENTLY` |
| SHARE | ROW EXCLUSIVE, SHARE UPDATE EXCLUSIVE, SHARE ROW EXCLUSIVE, EXCLUSIVE, ACCESS EXCLUSIVE | `CREATE INDEX` (CONCURRENTLY 없이) |
| SHARE ROW EXCLUSIVE | ROW EXCLUSIVE, SHARE UPDATE EXCLUSIVE, SHARE, SHARE ROW EXCLUSIVE, EXCLUSIVE, ACCESS EXCLUSIVE | `CREATE TRIGGER`, 일부 `ALTER TABLE` 형태 |
| EXCLUSIVE | ROW SHARE, ROW EXCLUSIVE, SHARE UPDATE EXCLUSIVE, SHARE, SHARE ROW EXCLUSIVE, EXCLUSIVE, ACCESS EXCLUSIVE | `REFRESH MATERIALIZED VIEW CONCURRENTLY` |
| ACCESS EXCLUSIVE | 모든 모드 | `DROP TABLE`, `TRUNCATE`, `REINDEX`, `CLUSTER`, `VACUUM FULL`, `REFRESH MATERIALIZED VIEW` |

중요: `ACCESS EXCLUSIVE`만 `SELECT` 문(`FOR UPDATE/SHARE` 없이)을 차단합니다.

#### 잠금 동작

- 잠금은 트랜잭션 종료까지 유지됩니다.
- 세이브포인트 후에 획득한 잠금은 세이브포인트가 롤백되면 해제됩니다.
- PL/pgSQL 예외 블록 내의 잠금은 오류 시 해제됩니다.

#### 표 13.2: 충돌하는 잠금 모드

| 요청된 잠금 | ACCESS SHARE | ROW SHARE | ROW EXCL. | SHARE UPDATE EXCL. | SHARE | SHARE ROW EXCL. | EXCL. | ACCESS EXCL. |
|-------------|--------------|-----------|-----------|-------------------|-------|-----------------|-------|--------------|
| ACCESS SHARE | | | | | | | | X |
| ROW SHARE | | | | | | | X | X |
| ROW EXCL. | | | | | X | X | X | X |
| SHARE UPDATE EXCL. | | | | X | X | X | X | X |
| SHARE | | | X | X | | X | X | X |
| SHARE ROW EXCL. | | | X | X | X | X | X | X |
| EXCL. | | X | X | X | X | X | X | X |
| ACCESS EXCL. | X | X | X | X | X | X | X | X |

---

### 13.3.2. 행 수준 잠금

행 수준 잠금은 같은 행에 대한 쓰기자와 잠금자만 차단하며, 데이터 조회는 차단하지 않습니다. 트랜잭션 종료 시 또는 세이브포인트 롤백 중에 해제됩니다.

#### 행 수준 잠금 모드

FOR UPDATE
- 업데이트를 위해 행을 잠급니다.
- 다른 트랜잭션의 `UPDATE`, `DELETE`, `SELECT FOR UPDATE`, `SELECT FOR NO KEY UPDATE`, `SELECT FOR SHARE`, `SELECT FOR KEY SHARE`를 방지합니다.
- `REPEATABLE READ` 또는 `SERIALIZABLE`에서 트랜잭션 시작 이후 행이 변경되면 오류를 발생시킵니다.
- `DELETE` 및 일부 `UPDATE` 작업에서 획득됩니다.

FOR NO KEY UPDATE
- `FOR UPDATE`보다 약합니다.
- `SELECT FOR KEY SHARE`를 차단하지 않습니다.
- `FOR UPDATE`를 획득하지 않는 `UPDATE` 작업에서 획득됩니다.

FOR SHARE
- 배타적 잠금이 아닌 공유 잠금을 획득합니다.
- `UPDATE`, `DELETE`, `SELECT FOR UPDATE`, `SELECT FOR NO KEY UPDATE`를 차단합니다.
- `SELECT FOR SHARE` 또는 `SELECT FOR KEY SHARE`를 방지하지 않습니다.

FOR KEY SHARE
- `FOR SHARE`보다 약합니다.
- `DELETE` 및 키 변경 `UPDATE` 작업을 차단합니다.
- `SELECT FOR NO KEY UPDATE`, `SELECT FOR SHARE`, 또는 `SELECT FOR KEY SHARE`를 차단하지 않습니다.

#### 표 13.3: 충돌하는 행 수준 잠금

| 요청된 잠금 | FOR KEY SHARE | FOR SHARE | FOR NO KEY UPDATE | FOR UPDATE |
|-------------|---------------|-----------|-------------------|------------|
| FOR KEY SHARE | | | | X |
| FOR SHARE | | | X | X |
| FOR NO KEY UPDATE | | X | X | X |
| FOR UPDATE | X | X | X | X |

참고: PostgreSQL은 잠긴 행 수에 제한이 없지만, 잠금은 디스크 쓰기를 유발할 수 있습니다(예: `SELECT FOR UPDATE`는 행을 잠금으로 표시함).

---

### 13.3.3. 페이지 수준 잠금

페이지 수준 공유/배타적 잠금은 공유 버퍼 풀의 테이블 페이지에 대한 읽기/쓰기 접근을 제어합니다. 이러한 잠금은 행이 가져와지거나 업데이트된 직후에 해제됩니다. 애플리케이션 개발자는 일반적으로 페이지 수준 잠금에 관심을 가질 필요가 없습니다.

---

### 13.3.4. 교착 상태 (Deadlocks)

교착 상태는 두 개 이상의 트랜잭션이 다른 트랜잭션이 필요로 하는 잠금을 유지할 때 발생합니다. PostgreSQL은 교착 상태를 자동으로 감지하고 한 트랜잭션을 중단하여 해결합니다.

#### 교착 상태 시나리오 예제

트랜잭션 1:
```sql
UPDATE accounts SET balance = balance + 100.00 WHERE acctnum = 11111;
```

트랜잭션 2:
```sql
UPDATE accounts SET balance = balance + 100.00 WHERE acctnum = 22222;
UPDATE accounts SET balance = balance - 100.00 WHERE acctnum = 11111;
```

트랜잭션 1 (계속):
```sql
UPDATE accounts SET balance = balance - 100.00 WHERE acctnum = 22222;
```

이것은 교착 상태를 만듭니다: 트랜잭션 1은 트랜잭션 2의 acctnum 22222에 대한 잠금을 기다리고, 트랜잭션 2는 트랜잭션 1의 acctnum 11111에 대한 잠금을 기다립니다.

#### 교착 상태 방지

1. 모든 애플리케이션에서 일관된 순서로 잠금 획득
2. 각 객체에 대해 가장 제한적인 잠금 모드를 먼저 획득
3. 트랜잭션을 장기간 열어두지 않기
4. 교착 상태로 인해 중단된 트랜잭션 재시도

---

### 13.3.5. 권고 잠금 (Advisory Locks)

권고 잠금은 애플리케이션 정의 의미를 가집니다. 시스템은 사용을 강제하지 않습니다 - 애플리케이션이 올바르게 사용해야 합니다. 비관적 잠금 전략에 유용합니다.

#### 획득 수준

세션 수준:
- 명시적으로 해제되거나 세션이 종료될 때까지 유지됩니다.
- 트랜잭션 의미를 따르지 않습니다.
- 롤백이 잠금을 해제하지 않습니다.
- 다중 획득은 각각에 대응하는 잠금 해제가 필요합니다.

트랜잭션 수준:
- 트랜잭션 종료 시 자동으로 해제됩니다.
- 명시적 잠금 해제 작업이 없습니다.
- 단기 사용에 더 편리합니다.

#### 주요 특성

- 테이블 플래그보다 빠릅니다.
- 테이블 블로트를 방지합니다.
- 세션 종료 시 자동으로 정리됩니다.
- 세션과 트랜잭션 수준 요청은 예상대로 서로를 차단합니다.
- 프로세스는 같은 잠금을 여러 번 유지할 수 있습니다(여러 잠금 해제 필요).

#### 중요 고려사항

권고 잠금과 일반 잠금은 다음에 의해 정의된 메모리 풀을 공유합니다:
- `max_locks_per_transaction`
- `max_connections`

LIMIT 및 정렬에 대한 주의:

```sql
-- OK
SELECT pg_advisory_lock(id) FROM foo WHERE id = 12345;

-- 위험: LIMIT이 잠금 전에 적용되지 않을 수 있음
SELECT pg_advisory_lock(id) FROM foo WHERE id > 12345 LIMIT 100;

-- OK: 하위 쿼리를 사용하여 잠금 전에 LIMIT 보장
SELECT pg_advisory_lock(q.id) FROM
(
  SELECT id FROM foo WHERE id > 12345 LIMIT 100
) q;
```

위험한 형태는 예상치 못한 잠금(댕글링 잠금)을 획득할 수 있으며 `pg_locks`에서 볼 수 있지만 세션 종료까지 해제되지 않습니다.

#### 권고 잠금 함수

권고 잠금을 조작하기 위한 함수는 PostgreSQL 문서의 섹션 9.28.10에 설명되어 있습니다.

---

## 13.4. 애플리케이션 수준의 데이터 일관성 검사

애플리케이션 수준의 데이터 일관성 검사는 특히 동시 환경에서 데이터 무결성과 관련된 비즈니스 규칙을 유지하는 데 중요합니다. 접근 방식은 사용 중인 트랜잭션 격리 수준에 따라 다릅니다.

---

### 13.4.1. Serializable 트랜잭션으로 일관성 강제하기

#### 핵심 개념

Serializable 트랜잭션 격리 수준 이 모든 쓰기와 데이터의 일관된 뷰가 필요한 모든 읽기에 사용되면, 일관성을 보장하기 위한 추가 노력이 필요하지 않습니다.

#### 작동 방식

- Serializable 트랜잭션은 위험한 읽기/쓰기 충돌 패턴에 대한 논블로킹 모니터링 을 추가한 Repeatable Read 트랜잭션입니다.
- 명백한 실행 순서에서 사이클을 유발할 수 있는 패턴이 감지되면, 관련된 트랜잭션 중 하나가 사이클을 깨기 위해 롤백됩니다.

#### 모범 사례

1. 일관성이 중요한 작업에 Serializable 사용: Serializable 트랜잭션을 사용하도록 작성된 소프트웨어는 PostgreSQL에서 "그냥 작동"해야 합니다.

2. 자동 재시도 메커니즘: 직렬화 실패로 롤백된 트랜잭션을 자동으로 재시도하는 프레임워크 사용

3. 기본 격리 수준 설정:
   ```sql
   SET default_transaction_isolation = serializable;
   ```

4. 격리 수준 강제: 다른 트랜잭션 격리 수준이 부주의하게 사용되거나 무결성 검사를 우회하는 것을 방지하기 위해 트리거 검사 추가

#### 경고: 데이터 복제 제한

Serializable 트랜잭션 무결성 보호는 다음에 확장되지 않습니다:
- 핫 스탠바이 모드
- 논리적 복제본

이러한 시나리오에서는 대신 기본 서버에서 명시적 잠금이 있는 Repeatable Read 를 사용하세요.

---

### 13.4.2. 명시적 차단 잠금으로 일관성 강제하기

#### 사용 시기

비직렬화 가능 쓰기가 가능할 때, 명시적 잠금을 사용하여 행 유효성을 보장하고 동시 업데이트로부터 보호합니다.

#### 잠금 메커니즘

```sql
SELECT FOR UPDATE      -- 동시 업데이트에 대해 반환된 행을 잠금
SELECT FOR SHARE       -- 공유 모드로 반환된 행을 잠금
LOCK TABLE table_name  -- 전체 테이블을 잠금
```

#### 중요한 동작 참고

중요한 주의사항: `SELECT FOR UPDATE`는 잠금이 해제된 후 다른 트랜잭션이 선택된 행을 업데이트하거나 삭제하는 것을 방지하지 않습니다 .

동시 수정을 방지하려면:
- 실제로 행을 UPDATE 해야 합니다, 값이 변경되지 않더라도
- `SELECT FOR UPDATE`는 다른 트랜잭션이 같은 잠금을 획득하거나 UPDATE/DELETE 작업을 실행하는 것을 일시적으로만 차단합니다.
- 트랜잭션이 커밋되거나 롤백되면, 차단된 트랜잭션은 실제 UPDATE가 수행되지 않으면 진행됩니다.

#### 예제 시나리오: 글로벌 유효성 검사

문제: 은행 애플리케이션이 두 테이블에서 대변 합계가 차변 합계와 같은지 확인해야 함

잘못된 접근 방식 (Read Committed):
```sql
-- 이것들은 신뢰할 수 없게 작동함 - 두 번째 쿼리는 커밋되지 않은 트랜잭션을 포함
SELECT sum(credits) FROM credits_table;
SELECT sum(debits) FROM debits_table;
```

더 나은 접근 방식 (명시적 잠금이 있는 Repeatable Read):
```sql
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
LOCK TABLE credits_table IN SHARE MODE;
LOCK TABLE debits_table IN SHARE MODE;
SELECT sum(credits) FROM credits_table;
SELECT sum(debits) FROM debits_table;
COMMIT;
```

이유: SHARE 모드 잠금은 잠긴 테이블에 (현재 트랜잭션을 제외하고) 커밋되지 않은 변경이 없음을 보장합니다.

#### 중요한 타이밍 고려사항

Repeatable Read 모드에서: 쿼리를 수행하기 전에 잠금을 획득하세요.
- Repeatable Read 트랜잭션의 스냅샷은 첫 번째 쿼리 또는 데이터 수정 명령(`SELECT`, `INSERT`, `UPDATE`, `DELETE`, 또는 `MERGE`) 시작 시 고정됩니다.
- 트랜잭션이 획득한 잠금은 다른 수정 트랜잭션이 실행 중이지 않음을 보장합니다.
- 그러나 스냅샷이 잠금 획득보다 앞서면, 현재 커밋된 변경보다 앞설 수 있습니다.

모범 사례: 스냅샷이 고정되기 전에 명시적으로 잠금을 획득하세요.

---

#### 요약 비교

| 접근 방식 | 격리 수준 | 장점 | 단점 |
|-----------|-----------|------|------|
| Serializable 트랜잭션 | SERIALIZABLE | 추가 코딩 불필요; 자동 충돌 감지 | 핫 스탠바이/논리적 복제와 작동 안 함; 재시도 로직 필요 |
| 명시적 차단 잠금 | READ COMMITTED 또는 REPEATABLE READ | 복제와 작동; 세밀한 제어 | 신중한 애플리케이션 로직 필요; 수동 잠금 관리 |

---

## 13.5. 직렬화 실패 처리

Repeatable Read 와 Serializable 격리 수준 모두 직렬화 이상을 방지하기 위해 설계된 오류를 생성할 수 있습니다. 이러한 수준을 사용하는 애플리케이션은 직렬화 오류로 인해 실패한 트랜잭션을 재시도할 준비가 되어 있어야 합니다.

### 오류 코드와 유형

#### 주요 오류
- SQLSTATE 코드: `40001` (`serialization_failure`)
  - 오류 메시지 텍스트는 상황에 따라 다릅니다.
  - 항상 트랜잭션 재시도가 필요한 직렬화 이상을 나타냅니다.

#### 관련 오류 (재시도가 필요할 수 있음)

1. 교착 상태 실패
   - SQLSTATE 코드: `40P01` (`deadlock_detected`)
   - 이것도 재시도하는 것이 좋습니다.

2. 고유 키 실패 (주의해서 사용)
   - SQLSTATE 코드: `23505` (`unique_violation`)
   - 여러 인스턴스가 동시에 같은 새 기본 키 값을 선택할 때 직렬화 실패를 나타낼 수 있습니다.
   - 일시적 실패가 아닌 지속적인 오류일 수 있습니다 - 더 신중한 처리 필요

3. 제외 제약 조건 실패 (주의해서 사용)
   - SQLSTATE 코드: `23P01` (`exclusion_violation`)
   - 고유 키 실패와 유사한 고려사항

### 트랜잭션 재시도를 위한 주요 요구사항

1. 전체 트랜잭션 재시도
   - 어떤 SQL을 발행할지 결정하는 모든 로직 포함
   - 어떤 값을 사용할지 결정하는 모든 로직 포함
   - PostgreSQL은 정확성 문제로 인해 자동 재시도 기능을 제공하지 않습니다.

2. 여러 번의 시도가 필요할 수 있음
   - 트랜잭션 재시도가 첫 번째 재시도에서 완료를 보장하지 않습니다.
   - 높은 경합 시나리오에서는 많은 시도가 필요할 수 있습니다.
   - 충돌하는 준비된 트랜잭션은 커밋되거나 롤백될 때까지 진행을 차단할 수 있습니다.

3. 예외 처리 전략
   - `serialization_failure` 오류를 무조건 재시도
   - 다른 오류 코드는 일시적 실패가 아닌 지속적인 조건을 나타낼 수 있으므로 주의하세요.

---

## 13.6. 주의사항 (Caveats)

이 섹션은 PostgreSQL의 다중 버전 동시성 제어(MVCC)를 사용할 때 중요한 제한사항과 고려사항을 문서화합니다.

### 1. MVCC 안전하지 않은 DDL 명령

영향 받는 명령:
- `TRUNCATE`
- 테이블을 다시 작성하는 형태의 `ALTER TABLE`

문제: 이러한 명령은 MVCC 안전하지 않습니다. DDL 명령이 커밋된 후, DDL 명령이 커밋되기 전에 촬영된 스냅샷을 사용하는 동시 트랜잭션에게 테이블이 비어 있는 것처럼 보입니다.

중요 참고: 이것은 DDL 명령이 시작되기 전에 테이블에 접근하지 않은 트랜잭션에만 영향을 미칩니다. 이전에 테이블에 접근한 트랜잭션은 `ACCESS SHARE` 테이블 잠금을 유지하며, 이는 DDL 명령이 완료될 때까지 차단합니다.

의미:
- 대상 테이블에 대한 연속 쿼리에서 명백한 불일치 없음
- 대상 테이블과 데이터베이스의 다른 테이블 간에 가시적 불일치를 유발할 수 있음

### 2. 핫 스탠바이에서의 Serializable 격리 수준

제한: Serializable 트랜잭션 격리 수준은 핫 스탠바이 복제 대상(섹션 26.4)에서 지원되지 않습니다.

현재 지원: 핫 스탠바이 모드에서 사용 가능한 가장 엄격한 격리 수준은 Repeatable Read 입니다.

고려사항: 기본 서버에서 모든 영구 데이터베이스 쓰기를 Serializable 트랜잭션 내에서 수행하면 모든 스탠바이가 결국 일관된 상태에 도달하도록 보장하지만, 스탠바이에서의 Repeatable Read 트랜잭션은 때때로 기본 서버에서의 어떤 직렬 실행과도 일치하지 않는 일시적 상태를 볼 수 있습니다.

### 3. 시스템 카탈로그 접근 이상

문제: 시스템 카탈로그에 대한 내부 접근은 현재 트랜잭션의 격리 수준을 사용하지 않습니다.

결과:
- 새로 생성된 데이터베이스 객체(테이블 등)는 포함된 행이 보이지 않더라도 동시 Repeatable Read 및 Serializable 트랜잭션에 보입니다.
- 더 높은 격리 수준에서의 명시적 시스템 카탈로그 쿼리는 동시에 생성된 객체를 나타내는 행을 보지 못합니다.

이것은 암시적과 명시적 카탈로그 접근 사이에 비대칭을 만듭니다.

---

## 13.7. 잠금과 인덱스 (Locking and Indexes)

PostgreSQL은 테이블 데이터에 대한 논블로킹 읽기/쓰기 접근을 제공하지만, 논블로킹 읽기/쓰기 접근은 현재 모든 인덱스 접근 방법에 대해 제공되지 않습니다. 다른 인덱스 유형은 잠금을 다르게 처리합니다.

### 인덱스 유형과 잠금 동작

#### B-tree, GiST, SP-GiST 인덱스
- 잠금 유형: 단기 공유/배타적 페이지 수준 잠금
- 잠금 해제: 각 인덱스 행이 가져와지거나 삽입된 직후
- 동시성: 교착 상태 조건 없이 가장 높은 동시성 제공

#### Hash 인덱스
- 잠금 유형: 공유/배타적 해시 버킷 수준 잠금
- 잠금 해제: 전체 버킷이 처리된 후
- 동시성: 인덱스 수준 잠금보다 나은 동시성이지만, 잠금이 하나의 인덱스 작업보다 더 오래 유지되므로 교착 상태 가능

#### GIN 인덱스
- 잠금 유형: 단기 공유/배타적 페이지 수준 잠금
- 잠금 해제: 각 인덱스 행이 가져와지거나 삽입된 직후
- 참고: GIN 인덱싱된 값의 삽입은 일반적으로 행당 여러 인덱스 키 삽입을 생성하므로, GIN은 단일 값 삽입에 대해 상당한 작업을 수행할 수 있습니다.

### 권장사항

동시 애플리케이션의 경우:
- B-tree 인덱스가 최고의 성능을 제공 하며 스칼라 데이터를 인덱싱해야 하는 동시 애플리케이션에 권장되는 인덱스 유형입니다.
- Hash 인덱스보다 더 많은 기능을 제공합니다.
- 비스칼라 데이터의 경우, B-tree가 해당 데이터 유형에 유용하지 않으므로 대신 GiST, SP-GiST, 또는 GIN 인덱스 를 사용하세요.

---

## 요약

PostgreSQL의 명시적 잠금 시스템은 테이블 수준 잠금, 행 수준 잠금, 권고 잠금을 통해 동시 접근에 대한 세밀한 제어를 제공합니다. 적절한 잠금 전략 - 일관된 잠금 순서, 적절한 잠금 모드, 교착 상태 처리 - 은 강력한 동시 데이터베이스 애플리케이션에 필수적입니다.
