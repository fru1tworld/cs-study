# jOOQ 내부: SQL 프래그먼트 올리기

> 원문: https://blog.jooq.org/jooq-internals-pushing-up-sql-fragments/

2021년 2월 4일/2021년 2월 5일 lukaseder 작성

---

## 소개

지난 13년 동안, jOOQ는 사용자에게 노출되지 않는 상당히 많은 내부 기능들을 축적해 왔습니다. 그 중 매우 흥미로운 기능 중 하나는 임의의 jOOQ 표현식 트리 요소가 SQL 프래그먼트를 상위 레벨로 밀어 올릴 수 있는 기능입니다.

## 어떻게 작동하는가?

### jOOQ 표현식 트리 모델

jOOQ 쿼리를 작성할 때, 실제로는 실제 SQL 구문처럼 보이는 SQL 문의 표현식 트리를 생성하는 것입니다. 예를 들어:

```java
ctx.select(BOOK.ID, BOOK.TITLE)
   .from(BOOK)
   .where(BOOK.AUTHOR_ID.eq(1))
   .fetch();
```

jOOQ에서 이것은 다음과 같은 형태의 표현식 트리에 불과합니다.

[표현식 트리 구조를 보여주는 ASCII 트리 다이어그램]

SQL을 생성할 때, jOOQ는 대부분 트리를 [너비 우선](https://xkcd.com/2407/)으로 순회합니다 (농담입니다. 대부분 깊이 우선이지만, 한 레벨 아래로 내려가기 전에 같은 레벨의 일부 자식 요소들을 먼저 고려하는 경우가 많습니다). 각 요소를 `StringBuilder`에 수집하여 예상되는 형태로 만듭니다:

```sql
SELECT book.id, book.title
FROM book
WHERE book.author_id = 1
```

[QueryPart](https://www.jooq.org/javadoc/latest/org.jooq/org/jooq/QueryPart.html)라고 불리는 이러한 표현식 트리 요소들 각각은 자신의 SQL을 어떻게 렌더링할지 스스로 결정할 수 있습니다. 예를 들어, `CompareCondition`은 대략 다음과 같은 시퀀스를 생성합니다:

```
<lhs> <operator> <rhs>
```

... 그 자식들이 무엇이든 간에 SQL 생성을 자식들에게 추가로 위임합니다. `TableField`는 예를 들어 [스키마 매핑(멀티 테넌시) 기능](https://www.jooq.org/doc/latest/manual/sql-building/dsl-context/custom-settings/settings-render-mapping/)을 기반으로 `Field` 참조를 완전히 / 부분적으로 / 또는 전혀 한정할지 여부를 결정합니다.

`[Substring](https://www.jooq.org/doc/current/manual/sql-building/column-expressions/string-functions/substring-function/)`과 같은 함수를 사용하는 경우, 해당 함수는 자체적으로 SQL을 어떻게 생성할지 결정할 수 있습니다. 매뉴얼에서 볼 수 있듯이, 이것들은 모두 동일합니다:

```sql
-- ACCESS
mid('hello world', 7)

-- ASE, SQLDATAWAREHOUSE, SQLSERVER
substring('hello world', 7, 2147483647)

-- AURORA_MYSQL, AURORA_POSTGRES, COCKROACHDB, CUBRID, H2,
-- HANA, HSQLDB, IGNITE, MARIADB, MEMSQL, MYSQL, POSTGRES, REDSHIFT,
-- SNOWFLAKE, SYBASE, VERTICA
substring('hello world', 7)

-- DB2, DERBY, INFORMIX, ORACLE, SQLITE
substr('hello world', 7)

-- FIREBIRD, TERADATA
substring('hello world' FROM 7)
```

[이런 방식으로, jOOQ 표현식 트리는 모든 방언에서 모든 구문을 에뮬레이션할 수 있습니다.](https://blog.jooq.org/top-10-sql-dialect-emulations-implemented-in-jooq/)

### 하지만 에뮬레이션이 로컬이 아니라면?

## 비로컬 에뮬레이션

때때로, QueryPart는 작동하기 위해 비로컬 구문 요소의 존재를 가정해야 합니다. 최근 사례는 [https://github.com/jOOQ/jOOQ/issues/11366](https://github.com/jOOQ/jOOQ/issues/11366)이었습니다.

jOOQ에서 이 [절차적 로직](https://www.jooq.org/doc/latest/manual/sql-building/procedural-statements/)을 작성할 때:

```java
for_(i).in(1, 10).loop(
    insertInto(t).columns(a).values(i)
)
```

> 물론, 이렇게 하지 않을 것입니다. 대신 벌크 삽입 문을 작성하고 SQL만으로 이 문제를 해결할 것입니다! 하지만 여러분에게는 이유가 있겠죠?

그러면, [인덱스가 있는 FOR 루프](https://www.jooq.org/doc/latest/manual/sql-building/procedural-statements/procedural-for/)는 일부 방언에서 동등한 [WHILE 문](https://www.jooq.org/doc/latest/manual/sql-building/procedural-statements/procedural-while/)을 사용하여 에뮬레이션해야 할 수 있습니다. 따라서, 예를 들어 Oracle에서 얻을 수 있는 이 직관적인 절차적 SQL 생성 대신:

```sql
FOR i IN 1 .. 10 LOOP
  INSERT INTO t (a) VALUES (i);
END LOOP;
```

... MySQL에서는 대략 이렇게 생성합니다:

```sql
DECLARE i INT DEFAULT 1;
WHILE i <= 10 DO
  INSERT INTO t (a) VALUES (i);
  SET i = i + 1;
END WHILE;
```

이것은 이전에 보여드린 것처럼 여전히 완전히 로컬에서 수행할 수 있습니다. `FOR` 표현식 트리 요소가 대신 `DECLARE`와 `WHILE` 쿼리 파트를 로컬에서 생성할 수 있습니다. 하지만 로컬 변수가 불가능하다면? Firebird처럼 블록 스코프가 없다면?

Firebird에서는 모든 로컬 변수가 최상위 선언 섹션에 선언되어야 합니다. 위의 코드를 익명 블록에서 실행하면, 올바르게 생성된 절차적 SQL은 다음과 같습니다:

```sql
EXECUTE BLOCK AS
  DECLARE i INT;
BEGIN
  :i = 1; -- 루프 변수는 여전히 로컬에서 초기화되어야 함
  WHILE (:i <= 10) DO BEGIN
    INSERT INTO t (a) VALUES (i);
    :i = :i + 1;
  END
END
```

다음과 같이 루프를 절차적 제어 흐름 요소에 더 중첩할 때도 여전히 마찬가지입니다:

```java
for_(i).in(1, 10).loop(
    for_(j).in(1, 10).loop(
        insertInto(t).columns(a, b).values(i, j)
    )
)
```

> 물론, 여전히 이렇게 하지 않을 것입니다. 대신 데카르트 곱에서 벌크 삽입 문을 작성하고 SQL만으로 이 문제를 해결할 것입니다! 하지만 아무튼, 예제를 단순하게 유지합시다

이제 중첩된 변수 선언이 있으며, MySQL에서는 여전히 잘 작동합니다:

```sql
DECLARE i INT DEFAULT 1;
WHILE i <= 10 DO
  BEGIN
    DECLARE j INT DEFAULT 1;
    WHILE j <= 10 DO
      INSERT INTO t (a, b) VALUES (i, j);
      SET j = j + 1;
    END WHILE;
  END
  SET i = i + 1;
END WHILE;
```

하지만 Firebird에서는 두 선언 모두 최상위로 밀어 올려야 합니다:

```sql
EXECUTE BLOCK AS
  DECLARE i INT;
  DECLARE j INT;
BEGIN
  :i = 1; -- 루프 변수는 여전히 로컬에서 초기화되어야 함
  WHILE (:i <= 10) DO BEGIN
    :j = 1; -- 루프 변수는 여전히 로컬에서 초기화되어야 함
    WHILE (:j <= 10) DO BEGIN
      INSERT INTO t (a, b) VALUES (i, j);
      :j = :j + 1;
    END
    :i = :i + 1;
  END
END
```

이것은 아직 모든 엣지 케이스를 처리하지는 않지만(예: 일부 방언은 PL/SQL처럼 로컬 변수를 "숨기는" 것을 허용함), jOOQ 3.15부터 지원될 간단한 [프로시저, 함수](https://github.com/jOOQ/jOOQ/issues/9190), 그리고 [트리거](https://github.com/jOOQ/jOOQ/issues/6956)에 대해서는 이미 많은 부분을 처리합니다.

## 어떻게 작동하는가?

### 대안 1: 표현식 트리 변환

이러한 표현식 트리 변환을 작동시키는 방법은 여러 가지가 있습니다. 장기적으로, 우리는 내부 표현식 트리 모델을 재설계하고, [jOOQ 파서](https://www.jooq.org/doc/latest/manual/sql-building/sql-parser/)와 표현식 트리 변환을 독립 제품으로 사용하고자 하는 사람들에게도 공개 API로 제공할 것입니다. 어느 정도는 [행 수준 보안에 대한 이 게시물에서 보여준 것처럼 `VisitListener` SPI를 사용하여](https://blog.jooq.org/implementing-client-side-row-level-security-with-jooq/) 이미 가능하지만, 현재 구현은 복잡합니다.

또한, 표현식 트리가 비로컬 변환을 필요로 하는 경우는 (지금까지는) 비교적 드뭅니다. 이는 가능한 후보를 _매번_ 적극적으로 찾으려고 하는 것이 아마도 과잉일 것임을 의미합니다.

### 대안 2: 지연 표현식 트리 변환

표현식 트리를 "지연하여" 변환할 수 있습니다. 즉, 여전히 불필요하다고 가정하고, 필요한 경우 예외를 던지고 다시 시작합니다. 우리는 실제로 이것을 합니다. [jOOQ에서 "패턴"은 `ControlFlowSignal`이라고 불리며](https://blog.jooq.org/rare-uses-of-a-controlflowexception/), 주로 다른 방언의 문당 최대 바인드 파라미터 수 제한을 해결하는 데 사용됩니다. 즉, 바인드 값을 세고, SQL Server에서 2000개(SQL Server는 2100개를 지원하지만 jtds는 2000개만 지원)보다 많으면 [정적 문에서 인라인 값을 사용하여](https://www.jooq.org/doc/latest/manual/sql-execution/statement-type/) SQL을 처음부터 다시 생성합니다.

[항상 jOOQ와 마찬가지로, 이러한 제한을 자신의 값으로 재구성할 수 있습니다](https://www.jooq.org/doc/latest/manual/sql-building/dsl-context/custom-settings/settings-inline-threshold/).

또 다른 경우는 Oracle의 오래된 `ROWNUM` 필터링에서 마이그레이션할 때 [`ROWNUM`을 `LIMIT` 변환](https://www.jooq.org/doc/latest/manual/sql-building/queryparts/sql-transformation/transform-rownum/)을 켜는 것을 잊은 경우입니다. _매번_ `ROWNUM` 인스턴스를 적극적으로 검색하는 것은 어리석은 일입니다. 대신, Oracle을 사용하지 않을 때 하나를 만나면 전체 SQL 쿼리를 다시 생성합니다.

여기서의 가정은 이러한 일들이 _매우_ 드물게 발생하며, 발생하더라도 여러분이 생각하지 못했고, 프로덕션에서 쿼리가 실패하는 것을 원하지 않는다는 것입니다. 아마도 이미 느린 쿼리가 jOOQ가 생성하는 데 _조금 더_ 시간이 걸린다는 사실은 쿼리가 여전히 Just Work™하기 위해 지불할 가치가 있는 대가입니다.

### 대안 3: 생성된 SQL 문자열 패칭

이것이 우리가 실제로 하는 것입니다.

거의 모든 SQL 변환이 로컬이라고 가정하고(`Substring` 예제처럼), 그렇지 않은 경우 SQL을 패치하는 것이 좋습니다. 결국, 우리는 단지 SQL 문자열을 생성하는 것입니다! 따라서, 특별한 텍스트 영역에 특별한 마커를 넣고, 그 영역을 대체 SQL 내용으로 교체할 수 있는 인프라를 도입하는 것은 어떨까요.

수정 [#11366](https://github.com/jOOQ/jOOQ/issues/11366) 없이, 생성된 코드는 다음과 같았을 수 있습니다:

```sql
EXECUTE BLOCK AS
  -- 여기에 특수 마커
BEGIN
  -- 스코프 진입
  DECLARE i INT DEFAULT 1;
  WHILE (:i <= 10) DO BEGIN
    DECLARE j INT DEFAULT 1;
    WHILE (:j <= 10) DO BEGIN
      INSERT INTO t (a, b) VALUES (i, j);
      :j = :j + 1;
    END
    :i = :i + 1;
  END
  -- 스코프 종료
END
```

이것은 Firebird에서 작동하지 않으므로, 수정을 적용합니다. 익명 블록의 SQL 생성에 의해 배치되는 저렴한 특수 마커가 있으며, 프로시저, 함수, 트리거에도 마찬가지입니다. 예를 들어:

```sql
CREATE FUNCTION func1()
RETURNS INTEGER
AS
  -- 여기에 특수 마커
BEGIN
  -- 스코프 진입
  RETURN 1;
  -- 스코프 종료
END
```

이제, `org.jooq.impl.DeclarationImpl` 쿼리 파트가 로컬에서 SQL을 생성할 때마다, 다음과 같이 생성하는 대신:

```sql
DECLARE i INT DEFAULT 1;
DECLARE j INT;
```

우리는 (로컬에서) 다음을 생성합니다:

```sql
:i = 1;
-- j는 여기서 초기화되지 않으므로, 로컬에서 할 일이 없음
```

동시에, `org.jooq.impl.DeclarationImpl`을 전체 스코프에서 볼 수 있는 컨텍스트 변수에 푸시합니다("스코프 진입" 및 "스코프 종료" 주석 참조).

스코프를 종료하자마자, 수집된 모든 선언을 렌더링해야 하며, 이번에는 기본값 없이, 예를 들어:

```sql
DECLARE i INT;
DECLARE j INT;
```

... 그런 다음 마커가 있던 위치에 렌더링된 SQL을 삽입합니다. 이후의 모든 마커는 물론 텍스트 길이의 차이만큼 이동합니다.

## jOOQ에서의 응용

이것은 현재 jOOQ 내에서 몇 번 사용됩니다:

- `BOOLEAN` 파라미터 / 반환 값이 있는 Oracle PL/SQL 함수 호출을 에뮬레이션하기 위해. 일부 `BOOLEAN`에서 `NUMBER`로의 변환 로직과 함께 합성 `WITH` 절을 생성하는 생성된 SQL을 패치합니다. [Oracle 12c 이후, Oracle은 WITH에 포함된 PL/SQL을 지원하며](https://oracle-base.com/articles/12c/with-clause-enhancements-12cr1#functions), 이것은 꽤 멋진 기능입니다!

- [전체 암시적 JOIN 기능은 이런 방식으로 구현됩니다](https://blog.jooq.org/type-safe-implicit-join-through-path-navigation-in-jooq-3-11/)! 마커는 `FROM` 절의 각 테이블을 구분합니다(예: `FROM BOOK`), 그리고 그러한 마킹된 테이블에서 시작하는 경로가 쿼리에서 발견되면(예: `SELECT BOOK.AUTHOR.FIRST_NAME`), 1) 경로를 생성하는 대신, 경로에 대한 합성 별칭이 컬럼을 한정하는 데 사용되고, 2) `FROM BOOK` 테이블을 생성하는 대신, 필요한 모든 to-one 관계를 조인하는 합성 `LEFT JOIN` 또는 `INNER JOIN`이 생성됩니다. 아래에 예제가 나와 있습니다.

- 위의 Firebird([그리고 아마도 T-SQL도, 두고 봅시다](https://github.com/jOOQ/jOOQ/issues/8314)) 절차적 로컬 변수 스코핑 수정은 이런 방식으로 구현됩니다.

- `CREATE OR REPLACE PROCEDURE x` 에뮬레이션처럼 `CREATE PROCEDURE x` 앞에 `DROP PROCEDURE x`를 추가하는 것과 같이 완전한 문 앞에 SQL을 추가해야 하는 몇 가지 에뮬레이션도 비슷한 방식으로 작동합니다. 이러한 유형의 출력은 문 배치에 다른 문을 추가한다는 점에서 "특별"합니다. 이는 JDBC에서 배치를 호출할 때 이것이 생성하는 가능한 결과 집합이나 업데이트 카운트를 건너뛰도록 주의해야 함을 의미합니다.

### 향후 응용 분야에는 다음이 포함될 수 있습니다:

- 다양한 에뮬레이션에 매우 유용한 더 많은 최상위 CTE

### 암시적 조인 예제:

```java
SELECT
  book.author.first_name,
  book.author.last_name
FROM book -- book 주위에 특수 마커
```

위의 SQL은 어떤 방언에서도 작동하지 않으며, jOOQ에만 해당됩니다. 경로의 해시 코드를 기반으로 각 고유 경로에 대한 별칭을 생성하므로, 쿼리는 대신 다음과 같이 보일 수 있습니다:

```sql
SELECT
  -- 경로는 생성되지 않고, 이에 대한 별칭이 생성됨
  alias_xyz.first_name,
  alias_xyz.last_name
FROM (
  -- 마킹된 "book" 테이블은 조인 트리로 대체됨
  book
    LEFT JOIN author AS alias_xyz
    ON book.author_id = author.id
)
```

결과 SQL 문자열에서 `book`을 `(book LEFT JOIN ...)`으로 단순히 대체합니다. 스코프를 정의하고 각 스코프에 대한 컨텍스트와 변수를 등록할 수 있는 인프라 덕분에, 이것은 임의의 중첩 수준에서 작동합니다. 다음과 같은 것에서도 각 경로 표현식에 대해 적절한 `book` 식별자를 항상 식별할 수 있습니다:

```sql
SELECT
  book.author.first_name,
  book.author.last_name,

  -- 중첩된 스코프가 외부 스코프에서 "book" 식별자를
  -- 숨기기 때문에 여기서는 다른 book 테이블
  (SELECT count(*) FROM book),
  (SELECT count(DISTINCT book.author.first_name) FROM book),

  -- 다시 외부 book
  (SELECT book.author.first_name)
FROM
  book
```

위의 것은 동일한 조인 그래프로 `book`의 두 마킹된 발생을 패치하여 다음과 같이 에뮬레이션됩니다:

```sql
SELECT
  alias_xyz.first_name,
  alias_xyz.last_name,

  -- 이 book에는 패칭이 수행되지 않음
  (SELECT count(*) FROM book),

  -- alias_xyz 별칭이 다시 사용됨, 경로가 동일함
  (SELECT count(DISTINCT alias_xyz.first_name)

  -- 그리고 book 테이블이 동일한 left join으로 다시 패치됨
   FROM (
     book
       LEFT JOIN author AS alias_xyz
       ON book.author_id = author.id
  )),

  -- 다시 외부 book
  (SELECT alias_xyz.first_name)
FROM (
  book
    LEFT JOIN author AS alias_xyz
    ON book.author_id = author.id
)
```

정교한 것만큼 복잡하게 들리지만, 정말 정말 잘 작동합니다.

아마도 미래에는 결과 문자열을 패칭하는 것보다 표현식 트리 변환이 선호될 것입니다. 지금까지 현재의 응용 분야에서는 이것이 최소 저항, 그리고 최고 성능의 경로였습니다.

---

태그: firebird, implicit JOIN, jooq, local variables, procedural logic, sql, stored procedures

lukaseder 작성

"I made jOOQ"

lukaseder의 모든 게시물 보기
