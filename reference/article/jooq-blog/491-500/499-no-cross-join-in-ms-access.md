# MS Access에서 CROSS JOIN 없음

> 원문: https://blog.jooq.org/no-cross-join-in-ms-access/

게시일: 2014년 2월 12일
저자: lukaseder

다가오는 jOOQ 3.3에서는 JDBC-ODBC 브릿지(JDK의 Java SE 7까지 포함됨)를 통한 MS Access 데이터베이스 지원이 통합되고 있습니다. 참고로 이 브릿지는 Java 8에서 제거될 예정입니다. 대안으로는 HSQLDB 파서와 Jackcess를 결합한 ucanaccess를 통해 접근할 수 있습니다. MS Access는 db-engines.com의 DBMS 순위에서 상위 10위 안에 들지만, 많은 이상한 특성들을 가지고 있습니다. 그 중 하나의 중요한 제한 사항은 공식적인 CROSS JOIN 연산이 없다는 것입니다.

대부분의 데이터베이스는 명시적인 CROSS JOIN을 지원합니다:

```sql
SELECT     p1.name player1,
           p2.name player2
FROM       player p1
CROSS JOIN player p2
```

동일한 쿼리를 ANSI SQL-92 이전의 쉼표로 구분된 테이블 목록으로 작성할 수 있습니다:

```sql
SELECT     p1.name player1,
           p2.name player2
FROM       player p1,
           player p2
```

## 누락된 CROSS JOIN을 우회하는 방법

일반적으로 누락된 CROSS JOIN 지원은 TRUE 술어를 사용한 INNER JOIN으로 에뮬레이션됩니다:

```sql
SELECT     p1.name player1,
           p2.name player2
FROM       player p1
JOIN       player p2
ON         1 = 1
```

그러나 이것은 MS Access에서 실패합니다. 왜냐하면 JOIN은 명시적으로 컬럼 참조를 요구하기 때문입니다. 문서에는 다음과 같이 명시되어 있습니다: "FROM table1 INNER JOIN table2 ON table1.field1 compopr table2.field2"

일부 사용 사례에 대한 우회 방법은 숫자 컬럼에 0을 곱하는 것입니다:

```sql
SELECT     p1.name player1,
           p2.name player2
FROM       player p1
JOIN       player p2
ON         p1.id * 0 = p2.id * 0
```

그러나 이러한 쿼리에서는 최적화가 저하될 수 있습니다.

## 요약

MS Access는 CROSS JOIN을 지원하지 않습니다. 권장되는 우회 방법은 쉼표로 구분된 테이블 목록을 사용하는 것이며, jOOQ는 더 정교한 SQL 변환을 개발하고 있습니다.
