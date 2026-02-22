# Java 개발자가 SQL 작성 시 저지르는 10가지 흔한 실수

> 원문: https://blog.jooq.org/10-common-mistakes-java-developers-make-when-writing-sql/

Java는 아마도 역사상 가장 성공적인 프로그래밍 언어일 것입니다. 그래서 자바 개발자들이 특별하고 중요하다는 것은 의심할 여지가 없습니다. 하지만 그들 중 많은 수가 SQL을 작성할 때 매우 흔한 실수들을 저지릅니다. 이 글에서는 그런 실수들 중 10가지를 살펴보겠습니다.

## 1. NULL을 잊어버리는 것

NULL은 아마도 SQL에서 가장 어려운 개념일 것입니다. "NULL"은 "UNKNOWN(알 수 없음)"이라고도 불립니다. 그것이 바로 핵심입니다. NULL은 빈 문자열이 아닙니다. NULL은 0이 아닙니다. NULL은 false가 아닙니다. NULL은 빈 목록이 아닙니다. NULL은 "알 수 없음"입니다.

즉, 우리는 값이 무엇인지 모릅니다. 이것이 중요한 이유는 무엇일까요?

```sql
-- 이것은 절대 참이 아닙니다!
NULL = NULL

-- 이것도 절대 참이 아닙니다!
NULL <> NULL

-- 이것은 NULL입니다!
NULL + 5

-- 이것도 NULL입니다!
NULL || 'text'
```

Java 개발자로서, 여러분은 null에 익숙합니다. 하지만 Java의 null은 SQL의 NULL과 다릅니다. Java에서 `null == null`은 true입니다. SQL에서 `NULL = NULL`은 절대 true가 아닙니다. 그것은 NULL입니다(즉, "알 수 없음").

해결책: 모든 술어(predicate)와 함수에서 명시적으로 NULL을 고려하세요. IS NULL, IS NOT NULL, COALESCE(), NVL(), IFNULL(), NULLIF() 등을 적극적으로 사용하세요.

## 2. Java 메모리에서 데이터를 처리하는 것

많은 Java 개발자들이 SQL을 잘 모릅니다. 몇 가지 JOIN은 할 수 있고, 간단한 UNION도 할 수 있으며, 약간의 서브쿼리도 할 수 있습니다. 하지만 윈도우 함수? GROUPING SETS? MODEL과 MATCH_RECOGNIZE 절(Oracle)? 많은 Java 개발자들은 이것들이 무엇인지조차 모릅니다. 그래서 그들은 모든 데이터를 Java 메모리로 가져와서 거기서 변환, 집계, 그룹화를 수행합니다.

하지만 데이터베이스 내에서 데이터를 집계, 변환, 그룹화하는 것이 거의 항상 Java 코드에서 하는 것보다 훨씬 빠르고 쉽습니다. 그렇게 하면 네트워크를 통해 전송되는 데이터 양도 줄어듭니다.

해결책: SQL을 작성할 때마다, 모든 데이터를 Java 메모리로 가져오지 않고도 원하는 결과를 데이터베이스 서버 내에서 직접 생성할 수 있는지 스스로에게 물어보세요.

## 3. UNION ALL 대신 UNION을 사용하는 것

UNION과 UNION ALL의 차이가 무엇인지 아시나요?

- UNION: 두 서브쿼리를 결합하고 중복을 제거합니다
- UNION ALL: 두 서브쿼리를 결합하고 중복을 유지합니다

중복 제거는 값비싼 정렬 연산을 수반하며, 대부분의 경우 불필요합니다. 두 서브쿼리가 설계상 중복 행을 생성하지 않는다면, 또는 중복이 있어도 상관없다면, UNION ALL을 사용하세요.

해결책: UNION을 작성할 때마다, 실제로 UNION ALL을 원하는 것은 아닌지 스스로에게 물어보세요.

## 4. JDBC 페이지네이션으로 대용량 결과를 페이지네이션하는 것

대부분의 데이터베이스는 LIMIT ... OFFSET, TOP ... START AT, OFFSET ... FETCH 또는 ROW_NUMBER() / ROWNUM 필터링과 같은 절을 통해 페이지네이션을 지원합니다.

이러한 절들은 Java 메모리에서 데이터를 페이지네이션하는 것보다 훨씬 빠릅니다. 특히 큰 오프셋에서 그렇습니다. 데이터베이스에서 모든 행을 가져온 다음 Java에서 페이지네이션하지 마세요.

해결책: 가능하면 항상 데이터베이스 페이지네이션 기능을 사용하세요.

## 5. Java 메모리에서 데이터를 조인하는 것

JOIN을 처음 배울 때, 어떤 개발자들은 JOIN이 느리다고 생각합니다. 이것은 몇몇 불운한 쿼리 실행 계획에서 비롯된 선입견일 수 있습니다. 사실, SQL 데이터베이스는 JOIN을 매우 빠르게 수행할 수 있습니다. 적절한 인덱스와 통계 메타데이터가 있다면, 비용 기반 최적화기는 NESTED LOOP JOIN, MERGE JOIN, HASH JOIN 중에서 해당 연산에 가장 적합한 것을 선택합니다.

그러나 많은 Java 개발자들은 개별적으로 여러 쿼리를 실행하고, 결과를 Java 맵에 로드한 다음, Java에서 조인을 수행합니다. 이것은 훨씬 느리고 장황합니다.

해결책: 여러 쿼리를 실행하고 있다면, 그것들을 하나의 쿼리로 통합할 수 있는지 스스로에게 물어보세요.

## 6. 카르테시안 곱의 중복을 제거하기 위해 DISTINCT 또는 UNION을 사용하는 것

무언가가 잘못되었을 때 느릿느릿한 쿼리에서 예기치 않게 중복이 발생하면, 어떤 개발자들은 DISTINCT 키워드를 "수정"으로 사용합니다. 어떤 면에서 이것은 효과가 있습니다. 증상이 사라지기 때문입니다. 하지만 원인은 아닙니다.

DISTINCT를 사용하는 것은 진짜 문제를 숨기는 것입니다 - JOIN 술어의 누락입니다. DISTINCT는 대용량 결과 집합에서 값비싼 정렬 연산을 수행합니다. 이것은 쿼리 성능에 큰 영향을 미칩니다.

해결책: 예기치 않은 중복이 발생하면 JOIN 조건을 확인하세요. 아마도 어딘가에 조건을 잊어버렸을 것입니다.

## 7. MERGE 문을 사용하지 않는 것

MERGE 문은 SQL에서 가장 강력하면서도 과소평가된 문 중 하나입니다. 매우 다양한 시나리오에서 UPSERT(UPDATE 또는 INSERT, 행이 존재하는지에 따라) 연산을 수행할 수 있습니다.

일부 개발자들은 먼저 UPDATE를 실행하고, 어떤 행도 업데이트되지 않으면 INSERT를 실행합니다. 또는 그 반대로 합니다. 이것은 두 가지 문이 필요하고, 경쟁 조건이 발생할 수 있습니다.

해결책: UPSERT 연산을 수행해야 할 때는 MERGE 문(또는 데이터베이스별 대안인 ON DUPLICATE KEY UPDATE, ON CONFLICT 등)을 사용하세요.

## 8. 윈도우 함수 대신 집계 함수를 사용하는 것

윈도우 함수가 소개되기 전에는 SQL에서 집계된 값과 집계되지 않은 값을 함께 조회하려면 GROUP BY를 사용한 서브쿼리가 필요했습니다.

```sql
-- 오래된 방식: GROUP BY 서브쿼리 사용
SELECT
  a.first_name,
  a.last_name,
  a.revenue,
  (SELECT SUM(revenue) FROM actors) total_revenue
FROM
  actors a;

-- 더 나은 방식: 윈도우 함수 사용
SELECT
  first_name,
  last_name,
  revenue,
  SUM(revenue) OVER() total_revenue
FROM
  actors;
```

윈도우 함수는 SQL:2003에서 도입되었으며, 이제 모든 주요 데이터베이스에서 지원됩니다. 이것들은 복잡한 집계, 순위, 실행 합계 등을 훨씬 더 읽기 쉽게 만들어 줍니다.

해결책: GROUP BY 서브쿼리를 작성할 때마다, 윈도우 함수가 더 나은 선택인지 스스로에게 물어보세요.

## 9. 정렬 간접 참조를 위해 인메모리 정렬을 사용하는 것

SQL의 ORDER BY 절은 CASE 표현식을 통해 정렬 간접 참조(sort indirection)를 지원합니다. 조건에 따라 다른 열로 정렬해야 할 때가 있습니다:

```sql
SELECT *
FROM table
ORDER BY
  CASE
    WHEN condition THEN column1
    ELSE column2
  END
```

절대로 Java에서 데이터를 정렬하지 마세요. 데이터베이스가 하도록 하세요. 데이터베이스가 훨씬 빠르게 할 수 있습니다.

해결책: 복잡한 정렬이 필요할 때 ORDER BY 절에서 CASE 표현식을 사용하세요.

## 10. 많은 레코드를 하나씩 삽입하는 것

JDBC는 배치 연산을 통해 INSERT 성능을 크게 향상시킬 수 있습니다. 모든 레코드를 하나씩 삽입하지 마세요:

```java
// 나쁜 예: 하나씩 삽입
for (Record r : records) {
  stmt.executeUpdate(
    "INSERT INTO t (a, b, c) VALUES ("
    + r.a + ", " + r.b + ", " + r.c + ")");
}

// 좋은 예: 배치 삽입
PreparedStatement stmt = connection.prepareStatement(
  "INSERT INTO t (a, b, c) VALUES (?, ?, ?)");
for (Record r : records) {
  stmt.setInt(1, r.a);
  stmt.setString(2, r.b);
  stmt.setDate(3, r.c);
  stmt.addBatch();
}
stmt.executeBatch();
```

배치 연산을 사용하면 여러 INSERT 문을 하나의 네트워크 왕복으로 데이터베이스에 보낼 수 있으므로 오버헤드가 크게 줄어듭니다.

해결책: 여러 레코드를 삽입할 때는 항상 JDBC 배치 연산을 사용하세요.

## 결론

SQL과 Java는 매우 다른 프로그래밍 패러다임을 가지고 있습니다. Java는 객체 지향적이고 명령형입니다. SQL은 선언적이고 집합 기반입니다. Java 개발자로서 SQL을 효과적으로 사용하려면, 여러분의 프로그래밍 패러다임을 재고해야 합니다. 객체나 알고리즘 대신 집합과 관계에 대해 생각하세요.

이 10가지 실수를 피한다면, 여러분의 SQL은 훨씬 더 효율적이고 유지보수하기 쉬워질 것입니다.

## 추천 도서

SQL 실수와 안티패턴에 대해 더 자세히 알고 싶다면, 다음 책들을 추천합니다:

- "SQL Antipatterns" - Bill Karwin 저
- "SQL Performance Explained" - Markus Winand 저
