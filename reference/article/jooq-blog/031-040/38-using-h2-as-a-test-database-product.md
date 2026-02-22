# jOOQ와 함께 H2를 테스트 데이터베이스 제품으로 사용하기

> 원문: https://blog.jooq.org/using-h2-as-a-test-database-product/

H2 데이터베이스는 주로 자바 개발자들이 테스트 용도로 사용하는 매우 인기 있는 인메모리 데이터베이스 제품입니다. DB-Engines 랭킹에 따르면 50위에 위치하며, CockroachDB, Ignite, Single Store, Interbase, Ingres, Google BigTable과 같은 제품들보다 높은 순위를 기록하고 있습니다. 이 제품들은 모두 jOOQ에서 지원됩니다.

## SQL 표준화

표준 SQL을 작성하는 것만으로도 충분히 어렵습니다. SQL 방언에 구애받지 않는 SQL을 작성하는 것은 매우 어렵습니다! 예를 들어, 다음 쿼리를 살펴보겠습니다. 이 쿼리는 H2의 네이티브 문법을 사용합니다:

```sql
SELECT v
FROM VALUES (1), (2), (3) AS t (v)
ORDER BY v
FETCH FIRST 2 ROWS ONLY
```

이 쿼리는 SQL Server에서는 문법 요구사항이 다르기 때문에 실패합니다. SQL Server에서는 VALUES 절에 괄호가 필요하고, `OFFSET` 절이 필수입니다. SQL Server의 표준 SQL `OFFSET .. FETCH` 절 해석에서는 `OFFSET` 절이 필수이기 때문입니다. 따라서 다음과 같이 수정해야 합니다:

```sql
SELECT v
FROM (VALUES (1), (2), (3)) AS t (v)
ORDER BY v
OFFSET 0 ROWS
FETCH FIRST 2 ROWS ONLY
```

이러한 복잡성을 jOOQ가 자동으로 처리해줍니다. jOOQ를 사용하면 다음과 같이 작성할 수 있습니다:

```java
ctx.select(v)
   .from(values(row(1), row(2), row(3)).as("t", "v"))
   .orderBy(v)
   .limit(2)
   .fetch();
```

jOOQ는 이를 각 데이터베이스 방언에 맞게 자동으로 변환해줍니다.

## H2의 호환 모드

H2는 다양한 데이터베이스 시스템에 대한 호환 모드를 제공합니다. T-SQL 애플리케이션의 경우 `MODE=MSSQLServer`를 활성화하고 다음과 같이 작성할 수 있습니다:

```sql
SELECT TOP 2 v
FROM (VALUES (1), (2), (3)) AS t (v)
ORDER BY v;
```

그러나 H2의 호환 모드는 _일반 SQL 사용_ 에만 유용하며, jOOQ와 같은 SQL 생성기와 함께 사용하기에는 적합하지 않습니다.

## jOOQ 사용자를 위한 핵심 설정 안내

jOOQ와 함께 H2를 사용할 때 올바른 접근 방식은 다음과 같습니다:

- jOOQ의 `SQLDialect.H2`를 사용할 것
- H2를 호환 모드 없이 사용할 것

H2에서 `SQLDialect.SQLSERVER`를 사용하면 문제가 발생합니다. jOOQ가 실제 SQL Server 데이터베이스라고 가정하기 때문입니다. jOOQ는 이미 자체적으로 호환성 처리를 구현하고 있으므로, H2에 호환 모드를 설정하고 `SQLDialect.SQLSERVER`를 사용하면 끝없는 호환성 문제가 발생하게 됩니다.

jOOQ가 생성하는 SQL은 네이티브 H2 SQL이며, H2의 호환 모드 설정을 인식하지 못합니다. 따라서 호환 모드를 켜면 오히려 충돌이 발생합니다.

## Testcontainers: 더 나은 대안

H2를 이용한 테스트는 점점 더 구식이 되어가고 있습니다. Testcontainers는 Docker에서 실제 데이터베이스 인스턴스를 구동하여 훨씬 우수한 대안을 제공합니다. 이 방식의 장점은 다음과 같습니다:

- 코드가 간결해짐
- 벤더별 고유 기능에 접근 가능
- 호환성 문제 완전 제거

Testcontainers를 사용하면 실제 프로덕션 데이터베이스와 동일한 데이터베이스에서 테스트를 수행할 수 있으므로, 호환성 관련 우회 방법이 필요 없어집니다. jOOQ 코드 생성에서도 testcontainers 사용을 권장합니다.

## 예외 상황: RDBMS에 독립적인 애플리케이션

여러 데이터베이스를 지원해야 하는 멀티 데이터베이스 제품의 경우, H2는 다른 방언에 대한 테스트를 실행하기 전에 "스모크 테스트" 용도로 여전히 유용합니다. H2는 빠른 시작 속도 덕분에 빠른 검증에 적합합니다. 하지만 이 경우에도 jOOQ와 함께 사용할 때는 네이티브 H2 모드가 필수이며, 절대로 호환 모드를 사용해서는 안 됩니다.

## 결론

단일 RDBMS 애플리케이션의 경우, testcontainers가 H2의 이점을 능가합니다. 멀티 데이터베이스 제품의 경우, H2는 여전히 가치가 있지만 네이티브 설정이 필요하며, 호환 모드는 절대로 사용해서는 안 됩니다.
