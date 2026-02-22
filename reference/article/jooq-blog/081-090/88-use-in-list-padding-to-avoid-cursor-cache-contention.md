# 커서 캐시 경합 문제를 피하기 위해 JDBC 애플리케이션에 IN 리스트 패딩 사용하기

> 원문: https://blog.jooq.org/use-in-list-padding-to-your-jdbc-application-to-avoid-cursor-cache-contention-problems/

게시일: 2021년 4월 22일
저자: lukaseder
블로그: Java, SQL and jOOQ

## 문제 설명

이 글에서는 중요한 프로덕션 이슈를 다룹니다: 서로 다른 크기의 IN 리스트를 가진 별개의 SQL 쿼리들이 강력한 실행 계획 캐시를 가진 RDBMS(Db2, Oracle, SQL Server)에서 각각 별도의 실행 계획으로 파싱되고 캐시된다는 것입니다. 다음 쿼리들은 모두 서로 다른 쿼리로 간주됩니다:

```sql
SELECT * FROM t WHERE id IN (?);
SELECT * FROM t WHERE id IN (?, ?);
SELECT * FROM t WHERE id IN (?, ?, ?);
SELECT * FROM t WHERE id IN (?, ?, ?, ?);
SELECT * FROM t WHERE id IN (?, ?, ?, ?, ?);
```

저는 피크 로드 시간에 이 문제로 인해 전체 Oracle 인스턴스가 다운되는 것을 본 적이 있습니다.

## 해결책: IN 리스트 패딩

이 기법은 IN 리스트를 가장 가까운 2의 거듭제곱으로 "패딩"하며, 마지막 값을 반복합니다:

```sql
SELECT * FROM t WHERE id IN (?);                      -- 그대로 유지
SELECT * FROM t WHERE id IN (?, ?);                   -- 그대로 유지
SELECT * FROM t WHERE id IN (?, ?, ?, ?);             -- 3에서 4로 패딩
SELECT * FROM t WHERE id IN (?, ?, ?, ?);             -- 그대로 유지
SELECT * FROM t WHERE id IN (?, ?, ?, ?, ?, ?, ?, ?); -- 5에서 8로 패딩
```

이 메커니즘은 가장 가까운 2의 거듭제곱으로 패딩하고 마지막 값을 반복하는 방식으로 작동합니다. 이를 통해 데이터베이스가 길이 1, 2, 3, 4, 5 등의 리스트에 대해 각각 별도의 실행 계획을 생성하는 대신, 2의 거듭제곱에 해당하는 길이의 실행 계획만 보게 됩니다.

## 구현

jOOQ는 버전 3.9(2016년 말)부터 IN 리스트 패딩을 지원해왔습니다. `ParsingConnection`을 사용하면 jOOQ를 사용하지 않는 시스템에도 이 기법을 적용할 수 있습니다:

```java
// 임의의 JDBC Connection이 jOOQ에 의해 래핑되어
// "ParsingConnection"으로 대체됩니다. 이것도 JDBC Connection입니다
DSLContext ctx = DSL.using(connection);
ctx.settings().setInListPadding(true);
Connection c = ctx.parsingConnection();

// 나머지 코드는 그대로 유지됩니다. jOOQ의 존재를 인식하지 못합니다
for (int i = 0; i < 10; i++) {
    try (PreparedStatement s = c.prepareStatement(
        "select 1 from dual where 1 in (" +
            IntStream.rangeClosed(0, i)
                     .mapToObj(x -> "?")
                     .collect(Collectors.joining(", ")) +
        ")")
    ) {
        for (int j = 0; j <= i; j++)
            s.setInt(j + 1, j + 1);

        try (ResultSet rs = s.executeQuery()) {
            while (rs.next())
                System.out.println(rs.getInt(1));
        }
    }
}
```

## 디버그 출력 예제

이 예제는 증가하는 IN 리스트 크기를 가진 10개의 쿼리를 생성합니다. 디버그 로그는 변환 과정을 보여줍니다:

```
Translating from         : select 1 from dual where 1 in (?)
Translating to           : select 1 from dual where 1 in (?)

Translating from         : select 1 from dual where 1 in (?, ?)
Translating to           : select 1 from dual where 1 in (?, ?)

Translating from         : select 1 from dual where 1 in (?, ?, ?)
Translating to           : select 1 from dual where 1 in (?, ?, ?, ?)

Translating from         : select 1 from dual where 1 in (?, ?, ?, ?)
Translating to           : select 1 from dual where 1 in (?, ?, ?, ?)

Translating from         : select 1 from dual where 1 in (?, ?, ?, ?, ?)
Translating to           : select 1 from dual where 1 in (?, ?, ?, ?, ?, ?, ?, ?)

Translating from         : select 1 from dual where 1 in (?, ?, ?, ?, ?, ?)
Translating to           : select 1 from dual where 1 in (?, ?, ?, ?, ?, ?, ?, ?)

Translating from         : select 1 from dual where 1 in (?, ?, ?, ?, ?, ?, ?)
Translating to           : select 1 from dual where 1 in (?, ?, ?, ?, ?, ?, ?, ?)

Translating from         : select 1 from dual where 1 in (?, ?, ?, ?, ?, ?, ?, ?)
Translating to           : select 1 from dual where 1 in (?, ?, ?, ?, ?, ?, ?, ?)

Translating from         : select 1 from dual where 1 in (?, ?, ?, ?, ?, ?, ?, ?, ?)
Translating to           : select 1 from dual where 1 in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)

Translating from         : select 1 from dual where 1 in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
Translating to           : select 1 from dual where 1 in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
```

위 출력에서 볼 수 있듯이, 1, 2, 4, 8개의 요소를 가진 리스트는 이미 2의 거듭제곱 경계에 있으므로 패딩 조정이 필요 없이 그대로 통과됩니다. 반면 3개의 요소는 4개로, 5-7개의 요소는 8개로, 9-10개의 요소는 16개로 패딩됩니다.

## 대안적 해결책

이것이 우회적 해결책임을 인정하며, 배열이나 임시 테이블을 사용하는 것을 포함한 더 나은 해결책들이 있습니다.

## 결론

이것이 "핵(hack)"이지만 빠른 프로덕션 수정을 제공합니다. jOOQ 라이브러리는 2016년부터 이 기능을 지원해왔으며, 더 새로운 `ParsingConnection`을 통해 jOOQ를 사용하지 않는 시스템의 임의의 SQL 쿼리에도 이 기법을 적용할 수 있습니다.

jOOQ는 주로 임베디드 SQL을 위한 내부 DSL이지만, `ParsingConnection`은 모든 JDBC SQL 문을 파싱하고 변환할 수 있게 해주어 커서 캐시 경합 문제에 대한 빠른 프로덕션 수정을 제공합니다.
