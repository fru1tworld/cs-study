# SQL Server와 CUBRID에서 FOR UPDATE 시뮬레이션

> 원문: https://blog.jooq.org/for-update-simulation-in-sql-server-and-cubrid/

비관적 잠금(Pessimistic locking)은 후속 UPDATE 작업이나 데이터베이스 기반의 프로세스 간 동기화를 위해 명시적으로 행을 잠그는 것을 가능하게 합니다. 대부분의 데이터베이스는 SQL 표준 FOR UPDATE 절을 지원하지만, 구현 방식은 다양합니다.

## SQL 표준 예제

```sql
-- 세 개의 행에 대한 행 잠금
SELECT *
  FROM author
 WHERE id IN (3, 4, 5)
   FOR UPDATE

-- 세 개의 행에서 두 개의 컬럼에 대한 셀 잠금
SELECT *
  FROM author
 WHERE id in (3, 4, 5)
   FOR UPDATE OF first_name, last_name
```

## Oracle 확장

Oracle은 SKIP LOCKED와 같은 유용한 확장을 제공합니다:

```sql
SELECT *
  FROM author
 WHERE id IN (3, 4, 5)
   FOR UPDATE SKIP LOCKED
```

이는 실패하는 대신 잠긴 레코드를 건너뜁니다 - 큐 테이블에서 매우 유용합니다.

## SQL Server와 CUBRID의 제한사항

SQL Server는 커서 내에서만 FOR UPDATE를 지원하며, WITH (updlock)과 같은 독자적인 확장을 제공하는데, 이는 개별 레코드가 아닌 전체 페이지를 잠급니다. CUBRID는 SQL에서 비관적 잠금을 지원하지 않지만, TYPE_SCROLL_SENSITIVE와 CONCUR_UPDATABLE 플래그를 사용한 JDBC를 통해 시뮬레이션이 가능합니다.

## JDBC 시뮬레이션 접근 방식

```java
PreparedStatement stmt = connection.prepareStatement(
  "SELECT * FROM author WHERE id IN (3, 4, 5)",
  ResultSet.TYPE_SCROLL_SENSITIVE,
  ResultSet.CONCUR_UPDATABLE);
ResultSet rs = stmt.executeQuery();

while (rs.next()) {
  // 행 잠금을 위해 기본 키를 UPDATE하거나, 셀 잠금을 위해 다른 컬럼을 UPDATE
  rs.updateObject(1, rs.getObject(1));
  rs.updateRow();

  // 레코드 처리
}
```

## 단점

주요 제한사항은 스크롤 가능한 커서를 유지하면서 레코드를 순차적으로 잠그는 것과 관련이 있으며, 여러 스레드가 충돌하는 잠금 순서(오름차순 대 내림차순 ID 순서)로 실행될 때 데드락과 경쟁 조건의 위험을 초래합니다.

## jOOQ 통합 테스트 예제

```java
Factory create1 = // ...
Factory create2 = // ...

final Vector<String> execOrder = new Vector<String>();

try {
    final Thread t1 = new Thread(new Runnable() {
        @Override
        public void run() {
            Thread.sleep(2000);
            execOrder.add("t1-block");
            try {
                create1
                    .select(AUTHOR.ID)
                    .from(AUTHOR)
                    .forUpdate()
                    .fetch();
            }
            catch (Exception ignore) {
            }
            finally {
                execOrder.add("t1-fail-or-t2-commit");
            }
        }
    });

    final Thread t2 = new Thread(new Runnable() {
        @Override
        public void run() {
            execOrder.add("t2-exec");
            Result<?> result2 = create2
                .selectFrom(AUTHOR)
                .forUpdate()
                .fetch();
            assertEquals(2, result2.size());

            execOrder.add("t2-signal");

            try {
                Thread.sleep(4000);
            }
            catch (Exception ignore) {
            }

            execOrder.add("t1-fail-or-t2-commit");

            try {
                create2.getConnection().commit();
            }
            catch (Exception e) {}
        }
    });

    t1.start();
    t2.start();

    t1.join();
    t2.join();

    assertEquals(asList(
      "t2-exec",
      "t2-signal",
      "t1-block",
      "t1-fail-or-t2-commit",
      "t1-fail-or-t2-commit"), execOrder);
}
```

## 결론

데이터베이스마다 FOR UPDATE를 다르게 구현합니다 - 일부는 잠금 획득 중에 타임아웃되고, 다른 일부는 즉시 실패합니다. Oracle은 명시적인 타임아웃 제어를 위해 WAIT/NOWAIT 절을 지정할 수 있습니다.
