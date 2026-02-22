# Chapter 7. 쿼리 (Queries)

> PostgreSQL 18 공식 문서 번역

---

## 목차

- [7.1. 개요](#71-개요)
- [7.2. 테이블 표현식](#72-테이블-표현식)
  - [7.2.1. FROM 절](#721-from-절)
  - [7.2.2. WHERE 절](#722-where-절)
  - [7.2.3. GROUP BY 및 HAVING 절](#723-group-by-및-having-절)
  - [7.2.4. GROUPING SETS, CUBE, ROLLUP](#724-grouping-sets-cube-rollup)
  - [7.2.5. 윈도우 함수 처리](#725-윈도우-함수-처리)
- [7.3. SELECT 목록](#73-select-목록)
  - [7.3.1. SELECT 목록 항목](#731-select-목록-항목)
  - [7.3.2. 열 레이블](#732-열-레이블)
  - [7.3.3. DISTINCT](#733-distinct)
- [7.4. 쿼리 결합 (UNION, INTERSECT, EXCEPT)](#74-쿼리-결합-union-intersect-except)
- [7.5. 행 정렬 (ORDER BY)](#75-행-정렬-order-by)
- [7.6. LIMIT과 OFFSET](#76-limit과-offset)
- [7.7. VALUES 목록](#77-values-목록)
- [7.8. WITH 쿼리 (공통 테이블 표현식)](#78-with-쿼리-공통-테이블-표현식)
  - [7.8.1. WITH에서의 SELECT](#781-with에서의-select)
  - [7.8.2. 재귀 쿼리](#782-재귀-쿼리)
  - [7.8.3. 공통 테이블 표현식 구체화](#783-공통-테이블-표현식-구체화)
  - [7.8.4. WITH에서의 데이터 수정 문장](#784-with에서의-데이터-수정-문장)

---

## 7.1. 개요

데이터베이스에서 데이터를 검색하는 과정 또는 그러한 명령을 쿼리(query) 라고 합니다. SQL에서는 `SELECT` 명령을 사용하여 쿼리를 지정합니다.

### SELECT 명령의 일반 구문

```sql
[WITH with_queries] SELECT select_list FROM table_expression [sort_specification]
```

다음 섹션에서는 선택 목록(select list), 테이블 표현식(table expression), 정렬 사양(sort specification)의 세부 사항을 설명합니다. `WITH` 쿼리는 고급 기능이므로 마지막에 다룹니다.

### 기본 쿼리 예제

#### 1. 모든 행과 열 검색

```sql
SELECT * FROM table1;
```

`table1`이라는 테이블이 있다면, 이 명령은 `table1`의 모든 행과 모든 사용자 정의 열을 검색합니다. 선택 목록 사양 `*`는 테이블 표현식이 생성하는 모든 열을 의미합니다. 선택 목록은 열의 부분집합을 선택하거나 열을 사용하여 계산을 수행할 수도 있습니다.

#### 2. 특정 열 선택 및 계산

```sql
SELECT a, b + c FROM table1;
```

`table1`이 `a`, `b`, `c` 열을 가지고 있을 때(그리고 아마도 다른 열들도), `b`와 `c`가 숫자 데이터 타입이라면 위 쿼리는 `a` 열과 `b + c`의 계산 결과를 반환합니다. 선택 목록과 값 표현식에 대한 자세한 내용은 7.3절을 참조하십시오.

#### 3. SELECT를 계산기로 사용

```sql
SELECT 3 * 4;
```

`FROM` 절은 테이블 표현식이 필요하지 않으므로 생략할 수 있습니다. 테이블 표현식 없이 SELECT 명령을 사용하여 간단한 계산을 수행할 수 있습니다.

#### 4. 함수 호출

```sql
SELECT random();
```

선택 목록의 표현식이 변하는 결과를 반환하는 경우에 유용합니다.

---

## 7.2. 테이블 표현식

테이블 표현식(table expression) 은 테이블을 계산합니다. 테이블 표현식은 `FROM` 절을 포함하며, 선택적으로 `WHERE`, `GROUP BY`, `HAVING` 절이 따라올 수 있습니다. 간단한 테이블 표현식은 디스크상의 테이블(소위 기본 테이블)만 참조하지만, 더 복잡한 표현식을 사용하여 기본 테이블을 다양한 방식으로 수정하거나 결합할 수 있습니다.

`WHERE`, `GROUP BY`, `HAVING` 절은 `FROM` 절에서 파생된 테이블에 대한 일련의 변환 파이프라인을 지정합니다. 이 모든 변환은 SELECT 목록에 전달될 행들을 제공하여 쿼리의 출력 행을 계산하는 가상 테이블을 생성합니다.

---

### 7.2.1. FROM 절

`FROM` 절은 쉼표로 구분된 테이블 참조 목록에 제공된 하나 이상의 다른 테이블로부터 테이블을 파생시킵니다.

#### 문법

```sql
FROM table_reference [, table_reference [, ...]]
```

테이블 참조는 다음이 될 수 있습니다:

- 테이블 이름 (스키마 정규화 가능)
- 파생 테이블 (서브쿼리)
- `JOIN` 구성
- 이들의 복합 조합

`FROM` 절에 여러 테이블 참조가 나열되면, 테이블들은 교차 조인(cross-join)됩니다 (즉, 해당 행들의 데카르트 곱이 형성됩니다; 아래 참조). `FROM` 목록의 결과는 `WHERE`, `GROUP BY`, `HAVING` 절에 의해 변환될 수 있고 최종적으로 전체 테이블 표현식의 결과인 중간 가상 테이블입니다.

테이블 참조가 테이블 상속 계층의 부모인 테이블의 이름을 지정하면, 테이블 참조는 해당 테이블뿐만 아니라 모든 하위 테이블의 행도 생성합니다. 단, 테이블 이름 앞에 `ONLY` 키워드를 붙이면 해당 테이블만 포함하고 하위 테이블은 포함하지 않습니다. 테이블 이름 뒤에 `*`를 쓰면 하위 테이블이 포함되어야 함을 명시적으로 지정합니다.

---

#### 7.2.1.1. 조인된 테이블 (Joined Tables)

조인된 테이블은 특정 조인 유형의 규칙에 따라 두 개의 (실제 또는 파생된) 테이블로부터 파생되는 테이블입니다. 내부, 외부, 교차 조인이 사용 가능합니다. 조인된 테이블의 일반적인 구문은 다음과 같습니다:

```sql
T1 join_type T2 [ join_condition ]
```

모든 유형의 조인은 함께 연결되거나 중첩될 수 있습니다: `T1`과 `T2` 중 하나 또는 둘 다 조인된 테이블일 수 있습니다. 조인 절 주위에 괄호를 사용하여 조인 순서를 제어할 수 있습니다. 괄호가 없으면 조인 절은 왼쪽에서 오른쪽으로 중첩됩니다.

##### 교차 조인 (Cross Join)

```sql
T1 CROSS JOIN T2
```

`T1`과 `T2`의 가능한 모든 행 조합(즉, 데카르트 곱)에 대해 조인된 테이블은 `T1`의 모든 열 다음에 `T2`의 모든 열로 구성된 행을 포함합니다. 테이블들이 각각 N개와 M개의 행을 가지면, 조인된 테이블은 N * M개의 행을 가집니다.

`FROM T1 CROSS JOIN T2`는 `FROM T1 INNER JOIN T2 ON TRUE`와 동등합니다(아래 참조). 또한 `FROM T1, T2`와도 동등합니다.

> 참고: 이 후자의 동등성은 두 개 이상의 테이블이 있을 때 정확히 성립하지 않습니다. `JOIN`이 `,`보다 더 강하게 결합하기 때문입니다. 예를 들어 `FROM T1 CROSS JOIN T2 INNER JOIN T3 ON condition`은 `FROM T1, T2 INNER JOIN T3 ON condition`과 같지 않습니다. 후자의 경우 `condition`이 `T1`을 참조할 수 있지만 전자에서는 그렇지 않습니다.

##### 자격 조인 (Qualified Joins)

```sql
T1 { [INNER] | { LEFT | RIGHT | FULL } [OUTER] } JOIN T2 ON boolean_expression
T1 { [INNER] | { LEFT | RIGHT | FULL } [OUTER] } JOIN T2 USING ( join_column_list )
T1 NATURAL { [INNER] | { LEFT | RIGHT | FULL } [OUTER] } JOIN T2
```

`INNER`와 `OUTER`라는 단어는 모든 형식에서 선택사항입니다. `INNER`가 기본값입니다; `LEFT`, `RIGHT`, `FULL`은 외부 조인을 의미합니다.

조인 조건은 `ON` 또는 `USING` 절에서 지정하거나, `NATURAL`이라는 단어로 암시적으로 지정합니다. 조인 조건은 두 소스 테이블에서 어떤 행이 "일치"하는 것으로 간주되는지 결정합니다. 아래에서 자세히 설명합니다.

조인 유형별 설명:

| 조인 유형 | 설명 |
|----------|------|
| `INNER JOIN` | `T1`의 각 행 `R1`에 대해, 조인된 테이블은 `T2`에서 `R1`과 조인 조건을 만족하는 각 행에 대한 행을 가집니다. |
| `LEFT OUTER JOIN` | 먼저 내부 조인을 수행합니다. 그런 다음 `T2`의 어떤 행과도 조인 조건을 만족하지 않는 `T1`의 각 행에 대해, `T2`의 열에 null 값이 있는 조인된 행이 추가됩니다. 따라서 조인된 테이블은 항상 `T1`의 각 행에 대해 최소 하나의 행을 가집니다. |
| `RIGHT OUTER JOIN` | 먼저 내부 조인을 수행합니다. 그런 다음 `T1`의 어떤 행과도 조인 조건을 만족하지 않는 `T2`의 각 행에 대해, `T1`의 열에 null 값이 있는 조인된 행이 추가됩니다. 이것은 왼쪽 조인의 반대입니다: 결과 테이블은 항상 `T2`의 각 행에 대해 행을 가집니다. |
| `FULL OUTER JOIN` | 먼저 내부 조인을 수행합니다. 그런 다음 `T2`의 어떤 행과도 조인 조건을 만족하지 않는 `T1`의 각 행에 대해, `T2`의 열에 null 값이 있는 조인된 행이 추가됩니다. 또한 `T1`의 어떤 행과도 조인 조건을 만족하지 않는 `T2`의 각 행에 대해, `T1`의 열에 null 값이 있는 조인된 행이 추가됩니다. |

예제 테이블:

```
t1:
 num | name
-----+------
   1 | a
   2 | b
   3 | c

t2:
 num | value
-----+-------
   1 | xxx
   3 | yyy
   5 | zzz
```

##### ON 절

`ON` 절은 가장 일반적인 유형의 조인 조건입니다: `WHERE` 절에서 사용되는 것과 같은 종류의 불린 값 표현식을 사용합니다. `T1`과 `T2`의 행 쌍은 `ON` 표현식이 true로 평가되면 일치합니다.

##### ON 절 vs WHERE 절

```sql
-- ON 절: 조인 전에 필터링
SELECT * FROM t1 LEFT JOIN t2 ON t1.num = t2.num AND t2.value = 'yyy';

-- WHERE 절: 조인 후에 필터링 (다른 결과)
SELECT * FROM t1 LEFT JOIN t2 ON t1.num = t2.num WHERE t2.value = 'yyy';
```

차이점: `ON` 절의 제약은 조인 전에 처리되고, `WHERE` 절의 제약은 조인 후에 처리됩니다. 이것은 내부 조인에서는 중요하지 않지만, 외부 조인에서는 매우 중요합니다.

##### USING 절

`USING` 절은 조인의 양쪽 모두 조인 열에 동일한 이름을 사용하는 특정 상황을 활용하는 약식 표현입니다. 쉼표로 구분된 공유 열 이름 목록을 받아서 동등성 비교를 포함하는 조인 조건을 형성합니다.

```sql
T1 JOIN T2 USING (a, b)
```

위는 다음과 동등합니다:

```sql
T1 JOIN T2 ON T1.a = T2.a AND T1.b = T2.b
```

또한 `JOIN USING`의 출력은 중복 열을 억제합니다: 두 일치 열 모두를 출력할 필요가 없습니다. 값이 동일해야 하기 때문입니다. `JOIN ON`이 `T1`의 모든 열을 생성하고 그 다음에 `T2`의 모든 열을 생성하는 반면, `JOIN USING`은 나열된 순서대로 각 열 쌍에 대해 하나의 출력 열을 생성하고, 그 다음에 `T1`의 나머지 열, 그 다음에 `T2`의 나머지 열을 생성합니다.

##### NATURAL 조인

`NATURAL`은 두 테이블에 공통으로 나타나는 모든 열 이름으로 구성된 `USING` 목록을 형성하는 `USING`의 약식입니다.

```sql
T1 NATURAL JOIN T2
```

`USING`과 마찬가지로, 이러한 열은 출력 테이블에 한 번만 나타납니다. 공통 열 이름이 없으면, `NATURAL JOIN`은 `CROSS JOIN`처럼 동작합니다.

> 주의: 스키마 변경으로 새로운 공통 열이 추가되면 자동으로 조인 대상이 변경되므로 `NATURAL`을 사용할 때 주의해야 합니다.

---

#### 7.2.1.2. 테이블 및 열 별칭

테이블과 복잡한 테이블 참조에 임시 이름을 부여하여 쿼리의 나머지 부분에서 파생된 테이블을 참조하는 데 사용할 수 있습니다. 이를 테이블 별칭이라고 합니다.

##### 문법

```sql
FROM table_reference AS alias
```

또는

```sql
FROM table_reference alias
```

`AS` 키워드는 선택사항입니다. `alias`는 임의의 식별자가 될 수 있습니다.

테이블 별칭의 일반적인 적용은 조인되는 테이블에 짧은 식별자를 할당하여 조인 조건에서 열 참조를 읽기 쉽게 유지하는 것입니다.

##### 예제

```sql
-- 긴 테이블 이름 단축
SELECT * FROM some_very_long_table_name s
JOIN another_fairly_long_name a ON s.id = a.num;
```

별칭은 현재 쿼리에서 테이블 참조의 새 이름이 됩니다. 별칭이 지정되면 쿼리의 다른 곳에서 원래 이름으로 테이블을 참조할 수 없습니다.

```sql
-- 잘못됨: 원래 테이블 이름으로 참조
SELECT * FROM my_table AS m WHERE my_table.a > 5;

-- 올바름: 별칭 사용
SELECT * FROM my_table AS m WHERE m.a > 5;
```

##### 자기 조인 (Self Join)

테이블 별칭은 주로 표기상의 편의를 위한 것이지만, 테이블을 자기 자신과 조인할 때는 반드시 사용해야 합니다.

```sql
SELECT * FROM people AS mother
JOIN people AS child ON mother.id = child.mother_id;
```

##### 열 별칭

```sql
FROM table_reference [AS] alias ( column1 [, column2 [, ...]] )
```

열 별칭이 지정되면 실제 열 이름을 숨기고 대신 지정된 별칭을 사용합니다. 테이블 별칭과 마찬가지로 열 별칭도 원래 이름을 숨깁니다.

```sql
-- 예제
SELECT * FROM (VALUES (1, 'a'), (2, 'b')) AS t(num, text);
```

---

#### 7.2.1.3. 서브쿼리 (Subqueries)

테이블을 참조하는 서브쿼리는 괄호로 묶어야 하며, 별칭을 할당해야 합니다(7.2.1.2절 참조). 서브쿼리는 임시 테이블을 생성합니다.

```sql
FROM (SELECT * FROM table1) AS alias_name
```

이 예제는 `FROM table1 AS alias_name`을 쓰는 것과 동등합니다. 더 흥미로운 경우는 그룹화나 집계가 서브쿼리에 포함될 때인데, 외부 쿼리에서는 할 수 없는 경우입니다.

서브쿼리는 `VALUES` 목록이 될 수도 있습니다:

```sql
FROM (VALUES ('anne', 'smith'), ('bob', 'jones'), ('joe', 'blow'))
     AS names(first, last)
```

다시 말하지만, 테이블 별칭이 필요합니다. `VALUES` 목록의 열에 별칭을 할당하는 것은 선택사항이지만 좋은 관행입니다. 자세한 내용은 7.7절을 참조하십시오.

SQL 표준에 따르면 서브쿼리에는 테이블 별칭을 제공해야 합니다. PostgreSQL은 별칭을 생략할 수 있지만, 이식성을 위해 별칭을 작성하는 것이 좋습니다.

---

#### 7.2.1.4. 테이블 함수 (Table Functions)

테이블 함수는 스칼라나 복합 데이터 타입(행)의 집합을 생성하는 함수입니다. 테이블의 열로 구성된 것처럼 테이블, 뷰 또는 쿼리의 `FROM` 절의 서브쿼리처럼 사용됩니다.

테이블 함수가 반환하는 열은 `LATERAL` 또는 `JOIN`에 앞서 `FROM` 항목처럼 쿼리에 포함될 수 있습니다.

##### 기본 문법

```sql
function_call [WITH ORDINALITY] [[AS] table_alias [(column_alias [, ... ])]]

ROWS FROM( function_call [, ... ] ) [WITH ORDINALITY] [[AS] table_alias [(column_alias [, ... ])]]
```

##### WITH ORDINALITY

`WITH ORDINALITY` 절이 지정되면, 함수 결과 열에 추가 열이 추가됩니다. 이 열은 `bigint` 타입이며 1부터 시작하여 함수 결과 집합의 각 행에 순번을 매깁니다.

```sql
SELECT * FROM unnest(array['a','b','c']) WITH ORDINALITY AS t(item, num);

 item | num
------+-----
 a    |   1
 b    |   2
 c    |   3
```

##### 예제

```sql
CREATE TABLE foo (fooid int, foosubid int, fooname text);

CREATE FUNCTION getfoo(int) RETURNS SETOF foo AS $$
    SELECT * FROM foo WHERE fooid = $1;
$$ LANGUAGE SQL;

SELECT * FROM getfoo(1) AS t1;
```

여기서 `getfoo`는 `foo` 테이블과 동일한 열을 포함하는 것으로 정의되었습니다. `getfoo`가 `SELECT * FROM getfoo(1) AS t1(a, b, c)`로 호출되었다면 "a", "b", "c"가 정의된 열 이름을 재정의합니다.

##### ROWS FROM 구문

여러 테이블 함수를 `ROWS FROM` 구문을 사용하여 결합할 수 있습니다. 이러한 호출의 결과는 병렬로 반환됩니다: 각 행마다 결과 행이 생성되고, 개별 결과에 해당하는 열이 채워집니다. 함수가 같은 수의 행을 생성하지 않으면, 더 짧은 것의 자리에 null 값이 대체되어 항상 가장 많은 행을 생성하는 함수만큼의 행이 결과에 포함됩니다.

```sql
SELECT *
FROM ROWS FROM
    (
        json_to_recordset('[{"a":40,"b":"foo"},{"a":"100","b":"bar"}]')
            AS (a INTEGER, b TEXT),
        generate_series(1, 3)
    ) AS x (p, q, s)
ORDER BY p;

  p  |  q  | s
-----+-----+---
  40 | foo | 1
 100 | bar | 2
     |     | 3
```

##### record 반환 함수

동적 열 구조를 지정할 수 있습니다:

```sql
SELECT *
FROM dblink('dbname=mydb', 'SELECT proname, prosrc FROM pg_proc')
      AS t1(proname name, prosrc text)
WHERE proname LIKE 'bytea%';
```

`dblink` 함수(dblink 모듈의 일부)는 원격 쿼리를 실행합니다. 어떤 쿼리에도 사용할 수 있으므로 `record`를 반환하도록 선언되어 있습니다. 호출하는 쿼리에서 예상되는 열을 지정해야 파서가 무엇을 예상해야 하는지 알 수 있습니다.

---

#### 7.2.1.5. LATERAL 서브쿼리

`FROM` 절에 나타나는 서브쿼리는 `LATERAL` 키워드로 시작될 수 있습니다. 이렇게 하면 앞의 `FROM` 항목이 제공하는 열을 참조할 수 있습니다. (`LATERAL` 없이는 각 서브쿼리가 독립적으로 평가되므로 다른 `FROM` 항목을 교차 참조할 수 없습니다.)

테이블 함수도 `FROM` 절에 나타날 때 `LATERAL` 키워드로 시작될 수 있지만, 함수의 경우 키워드는 선택사항입니다. 함수의 인수는 어떤 경우에도 앞의 `FROM` 항목이 제공하는 열에 대한 참조를 포함할 수 있습니다.

`LATERAL` 항목은 `FROM` 목록의 최상위 수준이나 `JOIN` 트리 내에서 나타날 수 있습니다. 후자의 경우 `JOIN` 오른쪽에 있으면 `JOIN`의 왼쪽에 있는 모든 항목을 참조할 수도 있습니다.

`FROM` 항목이 `LATERAL` 교차 참조를 포함할 때, 평가는 다음과 같이 진행됩니다: 교차 참조된 열을 제공하는 `FROM` 항목의 각 행 또는 열을 제공하는 여러 `FROM` 항목의 행 집합에 대해, `LATERAL` 항목은 해당 행이나 행 집합의 열 값을 사용하여 평가됩니다. 결과 행은 평소처럼 계산된 행과 조인됩니다. 이것은 열 소스 테이블의 각 행에 대해 반복됩니다.

```sql
SELECT * FROM foo, LATERAL (SELECT * FROM bar WHERE bar.id = foo.bar_id) ss;
```

이 예제에서 `LATERAL`은 유용하지 않습니다. 일반 서브쿼리에서와 같이 `foo.bar_id`에 대한 참조를 `WHERE` 표현식으로 넣을 수 있기 때문입니다. 그러나 함수에서 사용할 때 매우 유용합니다:

```sql
-- 함수 인수로 이전 행의 값 전달
SELECT p1.id, p2.id, v1, v2
FROM polygons p1, polygons p2,
     LATERAL vertices(p1.poly) v1,
     LATERAL vertices(p2.poly) v2
WHERE (v1 <-> v2) < 10 AND p1.id != p2.id;
```

##### LEFT JOIN + LATERAL

`LATERAL` 서브쿼리에 `LEFT JOIN`을 사용하는 것도 유용합니다. 이렇게 하면 `LATERAL` 서브쿼리가 행을 생성하지 않더라도 소스 행이 결과에 나타납니다.

```sql
-- 특정 제조사가 생산 중인 제품이 없을 수 있음
SELECT m.name
FROM manufacturers m
LEFT JOIN LATERAL get_product_names(m.id) pname ON true
WHERE pname IS NULL;
```

---

### 7.2.2. WHERE 절

`WHERE` 절의 구문은 다음과 같습니다:

```sql
WHERE search_condition
```

여기서 `search_condition`은 불린(진리값) 타입의 값을 반환하는 모든 값 표현식(4장 참조)입니다.

`FROM` 절의 처리가 완료된 후, 파생된 가상 테이블의 각 행은 검색 조건에 대해 검사됩니다. 조건의 결과가 true이면 행은 출력 테이블에 유지되고, 그렇지 않으면(즉, 결과가 false이거나 null이면) 버려집니다. 검색 조건은 일반적으로 `FROM` 절에서 생성된 테이블의 열을 최소 하나 이상 참조합니다. 이것은 필수는 아니지만, 그렇지 않으면 `WHERE` 절이 상당히 무의미해집니다.

> 참고: 내부 조인의 조인 조건은 `WHERE` 절에 작성하거나 `JOIN` 절에 작성할 수 있습니다. 예를 들어 다음 테이블 표현식들은 동등합니다:
>
> ```sql
> FROM a, b WHERE a.id = b.id AND b.val > 5
> ```
>
> 와
>
> ```sql
> FROM a INNER JOIN b ON (a.id = b.id) WHERE b.val > 5
> ```
>
> 또는 다음과도:
>
> ```sql
> FROM a NATURAL JOIN b WHERE b.val > 5
> ```
>
> 어느 것을 사용할지는 주로 스타일의 문제입니다. `FROM` 절의 `JOIN` 구문은 다른 SQL 데이터베이스 시스템에 이식할 때 가장 문제가 적을 수 있습니다. 외부 조인의 경우 `JOIN` 절을 사용하는 것 외에는 선택의 여지가 없습니다. 외부 조인의 `ON` 또는 `USING` 절은 `WHERE` 조건과 동등하지 않습니다. 최종 결과에서 어떤 행을 제거할지뿐만 아니라 어떤 행을 추가할지(일치하지 않는 입력 행에 대해)도 결정하기 때문입니다.

#### 예제

```sql
SELECT ... FROM fdt WHERE c1 > 5

SELECT ... FROM fdt WHERE c1 IN (1, 2, 3)

SELECT ... FROM fdt WHERE c1 IN (SELECT c1 FROM t2)

SELECT ... FROM fdt WHERE c1 IN (SELECT c3 FROM t2 WHERE c2 = fdt.c1 + 10)

SELECT ... FROM fdt WHERE c1 BETWEEN (SELECT c3 FROM t2 WHERE c2 = fdt.c1 + 10) AND 100

SELECT ... FROM fdt WHERE EXISTS (SELECT c1 FROM t2 WHERE c2 > fdt.c1)
```

`fdt`는 `FROM` 절에서 파생된 테이블입니다. `WHERE` 절의 필터 조건을 만족하지 않는 행은 `fdt`에서 제거됩니다. 선택 목록에서 스칼라 서브쿼리를 값 표현식으로 사용하는 것에 주목하십시오. 다른 쿼리와 마찬가지로 서브쿼리는 복잡한 테이블 표현식을 사용할 수 있습니다. 또한 `fdt`가 서브쿼리에서 어떻게 참조되는지도 주목하십시오. `c1`을 `fdt.c1`로 정규화하는 것은 `c1`이 서브쿼리의 파생된 입력 테이블의 열 이름이기도 한 경우에만 필요합니다. 그러나 필요하지 않더라도 열 이름을 정규화하면 명확성이 추가됩니다. 이 예제는 외부 쿼리의 열 명명 범위가 내부 쿼리로 어떻게 확장되는지 보여줍니다.

---

### 7.2.3. GROUP BY 및 HAVING 절

`WHERE` 필터를 통과한 후, 파생된 입력 테이블은 `GROUP BY` 절을 사용하여 그룹화되고, 그런 다음 `HAVING` 절을 사용하여 그룹 행을 제거할 수 있습니다.

```sql
SELECT select_list
    FROM ...
    [WHERE ...]
    GROUP BY grouping_column_reference [, grouping_column_reference]...
```

`GROUP BY` 절은 나열된 모든 열에서 같은 값을 공유하는 모든 행을 그룹화하는 데 사용됩니다. 열이 나열되는 순서는 중요하지 않습니다. 그 효과는 각 공통 값 집합을 그룹을 나타내는 하나의 그룹 행으로 결합하는 것입니다. 이렇게 하면 출력에서 중복을 제거하거나 이러한 그룹에 적용되는 집계를 계산할 수 있습니다.

```sql
SELECT x, sum(y) FROM test1 GROUP BY x;
```

일반적으로 테이블이 그룹화되면 `GROUP BY`에 나열되지 않은 열은 집계 표현식을 제외하고는 참조할 수 없습니다. 집계 표현식이 있는 예:

```sql
SELECT x, sum(y) FROM test1 GROUP BY x;
```

규칙: `GROUP BY`에 없는 열은 집계 함수 내에서만 사용 가능합니다.

```sql
-- 잘못됨: y는 집계 함수가 아님
SELECT x, y FROM test1 GROUP BY x;

-- 올바름
SELECT x, sum(y) FROM test1 GROUP BY x;
```

위의 두 번째 경우에서, 집계 `sum`은 전체 그룹에 대해 단일 값을 계산하므로 각 `x` 값과 연관될 수 있는 각 `y` 값에 대해 모호함이 없습니다.

#### 함수적 종속성

엄격히 말해서, `GROUP BY`는 테이블 별칭이 아닌 소스 테이블 열 이름으로만 그룹화할 수 있지만, PostgreSQL은 이를 확장하여 선택 목록의 열로도 그룹화할 수 있게 합니다. 열 이름 대신 값 표현식으로 그룹화하는 것도 허용됩니다.

그룹화된 열에 함수적 종속성이 있는 경우(예: 기본 키 제약), 그룹화되지 않은 열도 사용할 수 있습니다. 테이블의 기본 키가 `GROUP BY` 절에 포함되어 있으면 해당 테이블의 열은 참조할 수 있습니다. 왜냐하면 그룹화된 열에 대한 함수적 종속성이 있기 때문입니다.

```sql
-- product_id가 primary key면, name과 price는 자동으로 결정됨
SELECT product_id, p.name, (sum(s.units) * p.price) AS sales
    FROM products p LEFT JOIN sales s USING (product_id)
    GROUP BY product_id, p.name, p.price;

-- product_id가 products의 primary key면 간단히:
SELECT product_id, p.name, (sum(s.units) * p.price) AS sales
    FROM products p LEFT JOIN sales s USING (product_id)
    GROUP BY product_id;
```

#### HAVING 절

테이블을 그룹화하고 관심 있는 그룹만 선택하려면 `WHERE` 절 대신 `HAVING` 절을 사용합니다. `HAVING` 절은 `GROUP BY` 후에 그룹을 필터링합니다.

```sql
SELECT select_list FROM ... [WHERE ...] GROUP BY ... HAVING boolean_expression
```

`HAVING` 절의 표현식은 그룹화된 표현식과 그룹화되지 않은 표현식(후자는 반드시 집계 함수를 포함해야 함) 모두를 참조할 수 있습니다.

```sql
-- 예제
SELECT x, count(*) FROM test1 GROUP BY x HAVING count(*) > 3;
```

보다 현실적인 예제:

```sql
SELECT product_id, p.name, (sum(s.units) * (p.price - p.cost)) AS profit
    FROM products p LEFT JOIN sales s USING (product_id)
    WHERE s.date > CURRENT_DATE - INTERVAL '4 weeks'
    GROUP BY product_id, p.name, p.price, p.cost
    HAVING sum(p.price * s.units) > 5000;
```

위 예제에서 `WHERE` 절은 그룹화되지 않은 열로 행을 선택합니다(표현식은 최근 4주 내의 판매에만 해당). 반면 `HAVING` 절은 5000보다 큰 총 판매를 가진 그룹으로 출력을 제한합니다. 집계 표현식이 쿼리 내에서 동일할 필요는 없습니다.

#### 집계 함수 없는 GROUP BY

쿼리에 집계 함수 호출이 포함되어 있지만 `GROUP BY` 절이 없으면 그룹화가 여전히 발생합니다: 결과는 단일 그룹 행입니다(또는 `HAVING`에 의해 단일 행이 제거되면 행이 전혀 없을 수 있습니다).

```sql
-- GROUP BY 없이 집계 함수 사용 = 단일 그룹으로 취급
SELECT sum(y) FROM test1;
```

`HAVING` 절이 포함되어 있으면 `GROUP BY` 절이 없거나 집계 함수 호출이 없더라도 마찬가지입니다.

```sql
SELECT count(*) FROM test1 HAVING count(*) > 0;
```

---

### 7.2.4. GROUPING SETS, CUBE, ROLLUP

이전 섹션에서 설명한 것보다 더 복잡한 그룹화 연산이 `GROUPING SETS` 개념을 사용하여 가능합니다. `FROM`과 `WHERE` 절로 선택된 데이터는 지정된 각 그룹화 집합에 의해 별도로 그룹화되고, 각 그룹에 대해 단순한 `GROUP BY` 절과 마찬가지로 집계가 계산된 다음 결과가 반환됩니다.

#### GROUPING SETS

```sql
SELECT ... FROM ... GROUP BY GROUPING SETS ( (e1, e2), (e1), () )
```

각 서브리스트별로 별도로 집계합니다. 빈 그룹 `()`은 모든 행을 단일 그룹으로 집계합니다(집계 함수가 `GROUP BY` 절 없이 사용되는 경우처럼).

다음과 같이 개별 쿼리 결과를 `UNION ALL`한 것과 동일한 결과를 더 효율적으로 생성합니다:

```sql
SELECT a, b, SUM(c) FROM tab GROUP BY a, b
UNION ALL
SELECT a, NULL, SUM(c) FROM tab GROUP BY a
UNION ALL
SELECT NULL, NULL, SUM(c) FROM tab;
```

개별 그룹화 집합에 교차 참조되지 않은 열 자리에 null 값이 대체됩니다.

#### ROLLUP

```sql
ROLLUP ( e1, e2, e3, ... )
```

계층적 분석에 유용합니다 (예: 부서별, 부서그룹별, 회사 전체).

`ROLLUP`은 표현식의 선두 부분의 각 접미사로 구성된 그룹화 집합과 빈 그룹화 집합을 나타냅니다. 동등 표현:

```sql
GROUPING SETS (
    ( e1, e2, e3 ),
    ( e1, e2 ),
    ( e1 ),
    ( )
)
```

`ROLLUP`은 데이터 분석 및 보고에서 자주 사용됩니다.

예제: 직급, 부서, 회사별 총 급여

```sql
SELECT dept, salary_group, sum(salary)
FROM employees
GROUP BY ROLLUP (dept, salary_group);
```

#### CUBE

```sql
CUBE ( e1, e2, ... )
```

모든 가능한 부분집합의 집계(멱집합)를 생성합니다.

```sql
CUBE ( a, b, c )
```

동등 표현:

```sql
GROUPING SETS (
    ( a, b, c ),
    ( a, b    ),
    ( a,    c ),
    ( a       ),
    (    b, c ),
    (    b    ),
    (       c ),
    (         )
)
```

`CUBE`는 `ROLLUP`과 같은 멀티 레벨 분석에 유용합니다.

#### 복합 원소

`CUBE`나 `ROLLUP`의 개별 원소는 괄호 안에 그룹화된 원소 목록이거나 단일 표현식일 수 있습니다. 목록인 경우, 개별 그룹화 집합을 생성할 때 단일 단위로 취급됩니다.

```sql
-- 괄호 내 원소들을 단위로 취급
CUBE ( (a, b), (c, d) )

GROUPING SETS (
    ( a, b, c, d ),
    ( a, b       ),
    (       c, d ),
    (            )
)
```

```sql
ROLLUP ( a, (b, c), d )

-- 다음과 동등:
GROUPING SETS (
    ( a, b, c, d ),
    ( a, b, c    ),
    ( a          ),
    (            )
)
```

#### 다중 그룹화 항목

`GROUP BY` 절에서 여러 그룹화 항목을 함께 지정하면 그룹화 집합의 최종 목록은 개별 항목의 데카르트 곱입니다.

```sql
GROUP BY a, CUBE (b, c), GROUPING SETS ((d), (e))

-- 데카르트 곱:
GROUP BY GROUPING SETS (
    (a, b, c, d), (a, b, c, e),
    (a, b, d),    (a, b, e),
    (a, c, d),    (a, c, e),
    (a, d),       (a, e)
)
```

#### 중복 제거

`GROUP BY`에서 직접 여러 그룹화 항목을 지정할 때, 중복 그룹화 집합은 제거되지 않습니다. 중복을 제거하려면 `DISTINCT` 절을 사용합니다.

```sql
GROUP BY DISTINCT ROLLUP (a, b), ROLLUP (a, c)

-- 다음과 동등:
GROUP BY GROUPING SETS (
    (a, b, c),
    (a, b),
    (a, c),
    (a),
    ()
)
```

이것은 `SELECT DISTINCT`와 동일하지 않습니다: 출력 행은 여전히 중복될 수 있으며, null 값이 그룹화되어 생긴 것인지 실제 null 값인지 구분되지 않습니다.

> 참고: `(a, b)` 구문은 일반적으로 표현식에서 행 생성자로 인식됩니다. `GROUP BY` 절 내에서는 표현식의 최상위 수준에서 이것이 적용되지 않으며, `(a, b)`는 위에서 설명한 대로 표현식 목록으로 파싱됩니다. 어떤 이유로 그룹화 표현식에서 행 생성자가 필요하다면 `ROW(a, b)`를 사용하십시오.

---

### 7.2.5. 윈도우 함수 처리

쿼리에 윈도우 함수가 포함되어 있으면(8.4절, 9.22절, 4.2.8절 참조), 이러한 함수는 그룹화, 집계 및 `HAVING` 필터링이 수행된 후에 평가됩니다. 즉, 쿼리가 집계 함수, `GROUP BY` 또는 `HAVING`을 사용하면, 윈도우 함수가 보는 행은 원본 테이블 행이 아니라 그룹 행입니다.

```sql
SELECT
    row_number() OVER (PARTITION BY dept ORDER BY salary DESC) as rank,
    name,
    salary
FROM employees;
```

동작 원리:

- `GROUP BY`가 있으면 윈도우 함수는 그룹 행을 봅니다.
- `GROUP BY`가 없으면 원본 테이블 행을 봅니다.

순서 보장:

여러 윈도우 함수가 사용될 때, 윈도우 정의에서 구문적으로 동등한 `PARTITION BY` 및 `ORDER BY` 절을 가진 모든 윈도우 함수는 데이터에 대해 단일 패스로 평가되도록 보장됩니다. 따라서 `ORDER BY`가 순서를 고유하게 결정하지 않더라도 동일한 정렬 순서를 봅니다.

```sql
-- 같은 PARTITION BY/ORDER BY를 가진 윈도우 함수들은
-- 같은 입력 행 순서를 봄
SELECT
    row_number() OVER (ORDER BY id),
    rank() OVER (ORDER BY id)
FROM table_t;

-- 다른 ORDER BY를 가진 함수들 간에는 보장 없음
SELECT
    row_number() OVER (ORDER BY id),
    rank() OVER (ORDER BY salary DESC)
FROM table_t;
```

그러나 다른 `PARTITION BY` 또는 `ORDER BY` 사양을 가진 함수에 대해서는 이러한 보장이 없습니다.

현재 윈도우 함수는 항상 정렬이 수행되는 `ORDER BY`를 필요로 하며, 이것이 적용되는 함수가 연속적인 행에 대해 동등한 `ORDER BY` 값에 대해 결정론적 결과를 줄 것이라는 보장이 있습니다.

윈도우 함수는 쿼리의 선택 목록과 `ORDER BY` 절에서 허용됩니다. 그룹화 절이나 `HAVING` 절에서는 허용되지 않습니다. 값 표현식으로 사용된 곳에서는 윈도우 함수를 집계 함수 인수나 다른 윈도우 함수의 인수로 사용할 수 없습니다.

권장사항:

쿼리 결과에 특정 순서가 필요하면 쿼리의 최상위 수준에서 `ORDER BY`를 사용하십시오.

```sql
-- 최상위 ORDER BY 명시 권장
SELECT * FROM (
    SELECT row_number() OVER (ORDER BY id) as rn
    FROM table_t
) AS t
ORDER BY rn;
```

---

## 7.3. SELECT 목록

앞 섹션에서 보여준 것처럼, `SELECT` 명령의 테이블 표현식은 가능하면 테이블들을 결합하고, `WHERE` 절로 행을 필터링하고, `GROUP BY`로 그룹을 형성하여 중간 가상 테이블을 구성합니다. 이 테이블은 최종적으로 선택 목록(select list) 에 전달됩니다. 선택 목록은 중간 테이블에서 실제로 출력될 열을 결정합니다.

---

### 7.3.1. SELECT 목록 항목

가장 간단한 종류의 선택 목록은 `*`로, 테이블 표현식이 생성하는 모든 열을 출력합니다.

```sql
SELECT * FROM ...
```

그렇지 않으면 선택 목록은 쉼표로 구분된 값 표현식 목록입니다(4장에서 정의됨).

```sql
SELECT a, b, c FROM ...
```

열 이름 `a`, `b`, `c`는 `FROM` 절에서 참조된 테이블의 실제 열 이름이거나 7.2.1.2절에서 설명한 대로 부여된 별칭입니다. 선택 목록에서 사용할 수 있는 이름 공간은 `WHERE` 절과 동일하며, 그룹화를 사용하지 않는 한 `HAVING` 절과 같습니다(그룹화를 사용하면 `HAVING` 절과 동일합니다).

둘 이상의 테이블에 같은 이름의 열이 있으면 테이블 이름도 지정해야 합니다.

```sql
SELECT tbl1.a, tbl2.a, tbl1.b FROM ...
```

특정 테이블의 모든 열을 원하면:

```sql
SELECT tbl1.*, tbl2.a FROM ...
```

(Section 8.16.5도 참조하십시오.)

값 표현식이 선택 목록에 사용되면 개념적으로 반환된 테이블에 새로운 가상 열을 추가합니다. 값 표현식은 각 결과 행에 대해 한 번 평가됩니다. 표현식은 `FROM` 절의 테이블 표현식의 열을 참조할 필요가 없습니다; 예를 들어 상수 산술 표현식일 수 있습니다.

---

### 7.3.2. 열 레이블

선택 목록의 항목에 이름을 할당하여 후속 처리(예: `ORDER BY` 절에서 사용 또는 클라이언트 응용 프로그램에 의한 표시)에 사용할 수 있습니다.

```sql
SELECT a AS value, b + c AS sum FROM ...
```

`AS`를 사용하여 출력 열 이름을 명시하지 않으면 시스템이 기본 열 이름을 할당합니다:

- 단순 열 참조: 참조된 열의 이름이 사용됩니다.
- 함수 호출: 함수의 이름이 사용됩니다.
- 복잡한 표현식: 시스템이 일반적인 이름(예: `?column?`)을 생성합니다.

`AS` 키워드는 일반적으로 선택사항이지만, 원하는 열 이름이 PostgreSQL 키워드와 일치하는 경우 `AS`를 사용하거나 열 이름을 큰따옴표로 묶어야 합니다.

오류 예시 (`FROM`은 예약어):

```sql
SELECT a from, b + c AS sum FROM ...  -- 오류!
```

올바른 사용:

```sql
SELECT a AS from, b + c AS sum FROM ...
```

또는

```sql
SELECT a "from", b + c AS sum FROM ...
```

권장사항: 향후 키워드 추가 가능성에 대비하여 항상 `AS`를 사용하거나 출력 열 이름을 큰따옴표로 묶는 것이 좋습니다.

> 참고: 여기서 출력 열의 명명은 `FROM` 절의 명명(7.2.1.2절 참조)과 다릅니다. 열을 두 번 이름 지을 수 있지만, 선택 목록에서 할당된 이름이 전달됩니다.

---

### 7.3.3. DISTINCT

선택 목록이 처리된 후, 결과 테이블에서 선택적으로 중복 행을 제거할 수 있습니다. `DISTINCT` 키워드가 `SELECT` 바로 뒤에 작성되어 이를 지정합니다:

```sql
SELECT DISTINCT select_list ...
```

(`DISTINCT` 대신에 `ALL` 키워드를 사용하여 모든 행을 유지하는 기본 동작을 지정할 수 있습니다.)

동작 방식:

- 두 행은 최소한 하나의 열 값이 다르면 서로 다른(distinct) 것으로 간주됩니다.
- 이 비교에서 Null 값은 같은 것으로 간주됩니다.

대안으로, 임의의 표현식이 어떤 행을 구별되는 것으로 간주할지 결정할 수 있습니다:

```sql
SELECT DISTINCT ON (expression [, expression ...]) select_list ...
```

동작 방식:

여기서 `expression`은 모든 행에 대해 평가되는 임의의 값 표현식입니다. 모든 표현식이 같은 행 집합은 중복으로 간주되며, 집합의 첫 번째 행만 출력에 유지됩니다. "첫 번째 행"은 `DISTINCT` 필터에 도달하기 전에 충분한 열에 대해 행을 정렬하는 `ORDER BY`(아래 참조)를 사용하지 않는 한 예측할 수 없습니다.

`DISTINCT ON` 처리는 `ORDER BY` 정렬 후에 발생합니다.

`DISTINCT ON` 표현식은 최좌측 `ORDER BY` 표현식과 일치해야 합니다. `ORDER BY` 절은 일반적으로 각 `DISTINCT ON` 그룹 내에서 원하는 행의 우선순위를 결정하기 위해 추가 표현식을 포함합니다.

> 참고: `DISTINCT ON`은 SQL 표준의 일부가 아니며, 결과의 잠재적으로 비결정적 성질 때문에 때때로 나쁜 스타일로 간주됩니다. `GROUP BY`와 `FROM`의 서브쿼리를 신중하게 사용하여 이 구조를 피할 수 있지만, 종종 가장 편리한 대안입니다.

---

## 7.4. 쿼리 결합 (UNION, INTERSECT, EXCEPT)

두 쿼리의 결과를 집합 연산(합집합, 교집합, 차집합)을 사용하여 결합할 수 있습니다.

### 기본 문법

```sql
query1 UNION [ALL] query2
query1 INTERSECT [ALL] query2
query1 EXCEPT [ALL] query2
```

여기서 `query1`과 `query2`는 지금까지 논의된 모든 기능을 사용할 수 있는 쿼리입니다.

### 각 연산의 설명

#### UNION

`UNION`은 `query2`의 결과를 `query1`의 결과에 효과적으로 추가합니다(두 쿼리가 같은 수의 열을 반환해야 하고 해당 열이 호환되는 데이터 타입을 가져야 하므로 특정 순서가 보장되지는 않습니다). 또한 `DISTINCT`처럼 중복 행을 결과에서 제거합니다. 단, `UNION ALL`을 사용하면 중복 행이 유지됩니다.

#### INTERSECT

`INTERSECT`는 `query1`과 `query2` 모두에 있는 모든 행을 반환합니다. 중복 행은 `INTERSECT ALL`을 사용하지 않는 한 제거됩니다.

#### EXCEPT

`EXCEPT`는 `query1`에는 있지만 `query2`에는 없는 모든 행을 반환합니다(이를 때때로 두 쿼리 간의 차집합 이라고 합니다). 중복 행은 `EXCEPT ALL`을 사용하지 않는 한 제거됩니다.

### 중요 조건

두 쿼리는 "union compatible" 이어야 합니다:

- 동일한 개수의 열을 반환해야 합니다.
- 해당하는 열들이 호환되는 데이터 타입을 가져야 합니다.

### 연산 결합 및 우선순위

동일한 쿼리에서 `UNION`, `INTERSECT`, `EXCEPT`를 결합할 수 있습니다.

```sql
query1 UNION query2 EXCEPT query3
```

위는 다음과 동일합니다:

```sql
(query1 UNION query2) EXCEPT query3
```

`INTERSECT`는 다른 두 연산보다 더 강하게 결합합니다.

```sql
query1 UNION query2 INTERSECT query3
```

위는 다음과 동일합니다:

```sql
query1 UNION (query2 INTERSECT query3)
```

우선순위: `INTERSECT`가 `UNION`과 `EXCEPT`보다 더 높은 우선순위를 가집니다.

결합성: `UNION`과 `EXCEPT`는 좌측 결합(왼쪽에서 오른쪽으로)됩니다.

### LIMIT과 함께 사용하기

```sql
-- 이 쿼리는:
SELECT a FROM b UNION SELECT x FROM y LIMIT 10

-- 다음과 동일 (LIMIT이 전체 결과에 적용):
(SELECT a FROM b UNION SELECT x FROM y) LIMIT 10

-- 한 쿼리에만 LIMIT을 적용하려면 괄호 필수:
SELECT a FROM b UNION (SELECT x FROM y LIMIT 10)
```

### 괄호 사용

개별 쿼리를 괄호로 감싸는 것이 중요합니다. 특히 `LIMIT`, `ORDER BY` 등의 절을 사용할 때 필수입니다. 괄호가 없으면 이러한 절은 개별 `SELECT`가 아닌 `UNION`의 출력에 적용되는 것으로 간주됩니다.

---

## 7.5. 행 정렬 (ORDER BY)

쿼리가 출력 테이블을 생성한 후(`UNION`, `INTERSECT`, `EXCEPT`가 있으면 선택 목록 처리 및 결합 후), 선택적으로 정렬할 수 있습니다. 정렬을 선택하지 않으면 행은 지정되지 않은 순서로 반환됩니다. 그 경우 실제 순서는 스캔 및 조인 계획 유형과 디스크상의 순서에 따라 달라지지만 의존해서는 안 됩니다. 특정 출력 순서는 정렬 단계가 명시적으로 선택된 경우에만 보장됩니다.

### 기본 문법

```sql
SELECT select_list
    FROM table_expression
    ORDER BY sort_expression1 [ASC | DESC] [NULLS { FIRST | LAST }]
             [, sort_expression2 [ASC | DESC] [NULLS { FIRST | LAST }] ...]
```

정렬 표현식은 쿼리의 선택 목록에서 유효한 모든 표현식이 될 수 있습니다.

```sql
SELECT a, b FROM table1 ORDER BY a + b, c;
```

여러 정렬 표현식이 지정되면, 앞선 값이 같은 경우 뒤의 값을 사용하여 정렬합니다.

### 정렬 방향

각 표현식 뒤에 선택적으로 `ASC`(오름차순) 또는 `DESC`(내림차순) 키워드를 사용하여 정렬 방향을 설정할 수 있습니다.

- ASC (Ascending): 기본값, 작은 값이 먼저 옵니다.
- DESC (Descending): 큰 값이 먼저 옵니다.

```sql
ORDER BY x, y DESC
-- 이것은 다음과 같습니다: ORDER BY x ASC, y DESC
-- (ORDER BY x DESC, y DESC와 같지 않음)
```

### NULL 값 처리

`NULLS FIRST` 및 `NULLS LAST` 옵션은 null 값이 null이 아닌 값보다 먼저 또는 나중에 정렬되는지 결정합니다.

- NULLS FIRST: NULL 값이 먼저 나타납니다.
- NULLS LAST: NULL 값이 나중에 나타납니다.
- 기본값: `DESC` 정렬은 `NULLS FIRST`, 그 외의 경우는 `NULLS LAST`입니다.

### 출력 열 이름/번호로 정렬

정렬 표현식으로 출력 열 이름을 사용할 수 있습니다:

```sql
SELECT a + b AS sum, c FROM table1 ORDER BY sum;
```

출력 열의 번호를 사용할 수도 있습니다:

```sql
SELECT a, max(b) FROM table1 GROUP BY a ORDER BY 1;
```

### 제한사항

출력 열 이름을 표현식에 포함할 수 없음:

출력 열 이름은 단독으로 사용해야 하며, 표현식에서 사용할 수 없습니다.

```sql
-- 잘못된 예:
SELECT a + b AS sum, c FROM table1 ORDER BY sum + c;  -- 오류

-- 이것은 작동합니다:
SELECT a + b AS sum, c FROM table1 ORDER BY a + b + c;
```

이 제한은 모호함을 줄이기 위해 존재합니다.

### UNION/INTERSECT/EXCEPT와 함께 사용

결합된 쿼리 결과에 `ORDER BY`를 적용할 수 있지만, 이 경우 출력 열 이름 또는 번호로만 정렬할 수 있고 표현식은 사용할 수 없습니다.

### 기술 참고사항

PostgreSQL은 기본 B-tree 연산자 클래스 를 사용하여 정렬 순서를 결정합니다. 일반적으로 데이터 타입의 `<`(less than)와 `>`(greater than) 연산자가 이 정렬 순서에 대응됩니다.

---

## 7.6. LIMIT과 OFFSET

`LIMIT`과 `OFFSET`은 쿼리에 의해 생성된 행의 일부만 반환할 수 있게 해줍니다.

### 기본 문법

```sql
SELECT select_list
    FROM table_expression
    [ ORDER BY ... ]
    [ LIMIT { count | ALL } ]
    [ OFFSET start ]
```

### LIMIT

`count` 숫자가 지정되면 그 수 이하의 행이 반환됩니다(쿼리 자체가 더 적은 행을 생성하면 더 적음).

- `LIMIT ALL`은 `LIMIT` 절을 생략하는 것과 같습니다.
- `LIMIT NULL`도 생략하는 것과 같습니다(플레이스홀더 매개변수를 사용할 때 유용).

### OFFSET

`OFFSET`은 행을 반환하기 시작하기 전에 그 수만큼의 행을 건너뛰게 합니다.

- `OFFSET 0`은 `OFFSET`을 생략하는 것과 같습니다.
- `OFFSET NULL`도 생략하는 것과 같습니다.

### OFFSET과 LIMIT 함께 사용

`OFFSET`과 `LIMIT`이 모두 나타나면, `LIMIT` 개수의 행을 반환하기 시작하기 전에 `OFFSET` 행을 건너뜁니다.

### 중요 주의사항

#### 1. ORDER BY 필수

`LIMIT`을 사용할 때는 `ORDER BY` 절을 사용하여 결과 행을 고유한 순서로 제한하는 것이 중요합니다. 그렇지 않으면 쿼리 행의 예측할 수 없는 부분집합을 받게 됩니다. 10번째에서 20번째 행을 요청할 수 있지만, 어떤 순서로 10번째에서 20번째입니까? `ORDER BY`를 지정하지 않으면 순서를 알 수 없습니다.

#### 2. 일관성 문제

`LIMIT`/`OFFSET` 값을 변경하면서 다른 `OFFSET` 값으로 쿼리 결과의 다른 부분을 선택할 때 `ORDER BY`를 사용하지 않으면 일관성 없는 결과를 얻게 됩니다. 쿼리 최적화기는 결과 순서에 영향을 줄 수 있는 쿼리 계획을 선택할 때 `LIMIT`을 고려합니다. 따라서 다른 `LIMIT`/`OFFSET` 값으로 같은 쿼리를 실행하면 `ORDER BY`로 결과 순서를 강제하지 않는 한 다른 순서로 결과가 생성될 수 있습니다. 이것은 버그가 아닙니다; SQL이 특정 순서로 쿼리 결과를 전달하겠다고 약속하지 않는 한 보장되지 않는다는 사실의 본질적인 결과입니다.

#### 3. 성능

큰 `OFFSET` 값은 비효율적일 수 있습니다. 서버는 건너뛴 행도 내부적으로 계산해야 하기 때문입니다.

---

## 7.7. VALUES 목록

`VALUES`는 디스크에 실제로 테이블을 생성하고 채우지 않고도 쿼리에서 사용할 수 있는 "상수 테이블" 을 생성하는 방법을 제공합니다.

### 구문

```sql
VALUES ( expression [, ...] ) [, ...]
```

### 규칙

- 각 괄호로 묶인 표현식 목록은 테이블의 한 행을 생성합니다.
- 모든 목록은 동일한 수의 요소(즉, 테이블의 열 수)를 가져야 합니다.
- 각 목록의 대응하는 항목은 호환 가능한 데이터 타입을 가져야 합니다.
- 각 열에 할당되는 실제 데이터 타입은 `UNION`과 동일한 규칙으로 결정됩니다(10.5절 참조).

### 기본 예제

```sql
VALUES (1, 'one'), (2, 'two'), (3, 'three');
```

이것은 2개의 열과 3개의 행을 가진 테이블을 반환합니다. 다음과 효과적으로 동등합니다:

```sql
SELECT 1 AS column1, 'one' AS column2
UNION ALL
SELECT 2, 'two'
UNION ALL
SELECT 3, 'three';
```

### 열 이름 지정

기본적으로 PostgreSQL은 `VALUES` 테이블의 열에 `column1`, `column2` 등의 이름을 할당합니다. 열 이름은 SQL 표준에서 지정되지 않았으며 다른 데이터베이스 시스템에서는 다를 수 있으므로, 일반적으로 테이블 별칭 목록으로 기본 이름을 재정의하는 것이 좋습니다:

```sql
SELECT * FROM (VALUES (1, 'one'), (2, 'two'), (3, 'three')) AS t (num, letter);

 num | letter
-----+--------
   1 | one
   2 | two
   3 | three
(3 rows)
```

### 사용 가능한 위치

구문적으로 `VALUES` 다음에 표현식 목록이 오는 것은 다음과 같이 취급됩니다:

```sql
SELECT select_list FROM table_expression
```

그리고 `SELECT`가 나타날 수 있는 곳이면 어디든지 나타날 수 있습니다:

- `UNION`의 일부로 사용
- `ORDER BY`, `LIMIT` (또는 동등하게 `FETCH FIRST`), `OFFSET` 절 추가 가능
- `INSERT` 명령의 데이터 소스로 가장 일반적으로 사용됨
- 서브쿼리로 사용 가능

---

## 7.8. WITH 쿼리 (공통 테이블 표현식)

`WITH`는 더 큰 쿼리에서 사용할 보조 문장을 작성하는 방법을 제공합니다. 이러한 문장들은 공통 테이블 표현식(Common Table Expressions) 또는 CTE 라고 불리며, 한 번의 쿼리 실행 동안만 존재하는 임시 테이블을 정의하는 것으로 생각할 수 있습니다. `WITH` 절의 각 보조 문장은 `SELECT`, `INSERT`, `UPDATE`, `DELETE` 또는 `MERGE`일 수 있으며, `WITH` 절 자체는 동일한 유형의 기본 문장에 첨부됩니다.

---

### 7.8.1. WITH에서의 SELECT

`WITH`에서 `SELECT`를 사용하는 기본 가치는 복잡한 쿼리를 더 간단한 부분으로 분해하는 것입니다. 예를 들면:

```sql
WITH regional_sales AS (
    SELECT region, SUM(amount) AS total_sales
    FROM orders
    GROUP BY region
), top_regions AS (
    SELECT region
    FROM regional_sales
    WHERE total_sales > (SELECT SUM(total_sales)/10 FROM regional_sales)
)
SELECT region,
       product,
       SUM(quantity) AS product_units,
       SUM(amount) AS product_sales
FROM orders
WHERE region IN (SELECT region FROM top_regions)
GROUP BY region, product;
```

이 예제는 상위 판매 지역의 제품별 판매 합계만 표시합니다.

`WITH` 절은 두 개의 보조 문장 `regional_sales`와 `top_regions`를 정의합니다. 여기서 `regional_sales`의 출력은 `top_regions`에서 사용되고 `top_regions`의 출력은 기본 `SELECT` 쿼리에서 사용됩니다. 이 예제는 `WITH` 없이도 작성할 수 있지만, 두 레벨의 중첩된 서브-`SELECT`가 필요했을 것입니다. 이 방법이 따라가기가 조금 더 쉽습니다.

---

### 7.8.2. 재귀 쿼리

선택적 `RECURSIVE` 수정자는 `WITH`를 단순한 구문 편의에서 표준 SQL에서 불가능한 일을 수행하는 기능으로 변환합니다. `RECURSIVE`를 사용하면 `WITH` 쿼리가 자신의 출력을 참조할 수 있습니다. 매우 간단한 예는 1부터 100까지의 정수를 합하는 이 쿼리입니다:

```sql
WITH RECURSIVE t(n) AS (
    VALUES (1)
  UNION ALL
    SELECT n+1 FROM t WHERE n < 100
)
SELECT sum(n) FROM t;
```

재귀 `WITH` 쿼리의 일반적인 형식은 항상:

1. 비재귀 항 (기본 케이스)
2. `UNION` (또는 `UNION ALL`)
3. 재귀 항 (비재귀 항에서 파생된 행으로 시작하여 자신의 출력을 참조)

재귀 쿼리가 반복적으로 평가되는 것을 이해해야 합니다:

1. 비재귀 항을 평가합니다. `UNION`(단, `UNION ALL`은 아님)의 경우 중복 행을 버립니다. 나머지 모든 행을 재귀 쿼리의 결과에 포함하고, 임시 작업 테이블 에도 배치합니다.
2. 작업 테이블이 비어 있지 않은 한 다음 단계를 반복합니다:
   a. 재귀 항을 평가하고, 재귀 자기참조를 작업 테이블의 현재 내용으로 대체합니다. `UNION`(단, `UNION ALL`은 아님)의 경우 중복 행과 이전 결과 행을 복제하는 행을 버립니다. 나머지 모든 행을 재귀 쿼리의 결과에 포함하고, 임시 중간 테이블 에도 배치합니다.
   b. 작업 테이블의 내용을 중간 테이블의 내용으로 교체하고, 중간 테이블을 비웁니다.

> 참고: 엄밀히 말해서, 이 프로세스는 재귀가 아니라 반복이지만, `RECURSIVE`는 SQL 표준 위원회가 선택한 용어입니다.

위 예제에서 작업 테이블은 각 단계에서 단일 행만 가지며, 연속 단계에서 1에서 100까지의 값을 취합니다. 100번째 단계에서는 `WHERE` 절 때문에 출력이 없으므로 쿼리가 종료됩니다.

#### 계층적 데이터 예제: 제품 부품

재귀 쿼리는 일반적으로 계층적 또는 트리 구조 데이터를 다루는 데 사용됩니다. 유용한 예로 직접 및 간접 부품을 모두 포함하는 제품의 모든 하위 부품을 찾는 쿼리가 있습니다. 데이터의 시작점은 테이블입니다:

```sql
WITH RECURSIVE included_parts(sub_part, part, quantity) AS (
    SELECT sub_part, part, quantity FROM parts WHERE part = 'our_product'
  UNION ALL
    SELECT p.sub_part, p.part, p.quantity * pr.quantity
    FROM included_parts pr, parts p
    WHERE p.part = pr.sub_part
)
SELECT sub_part, SUM(quantity) as total_quantity
FROM included_parts
GROUP BY sub_part;
```

---

#### 7.8.2.1. 검색 순서 (Search Order)

재귀 쿼리로 트리 구조를 계산할 때 결과를 깊이 우선 또는 너비 우선 순서로 정렬하고 싶을 수 있습니다.

##### 깊이 우선 순서 (Depth-First Order)

깊이 우선 순서를 위해 각 결과 행에 대해 지금까지 방문한 행의 배열을 계산합니다. 예를 들어 `link` 필드를 사용하여 행을 연결하는 테이블 `tree`를 검색하는 이 쿼리를 고려하십시오:

```sql
WITH RECURSIVE search_tree(id, link, data) AS (
    SELECT t.id, t.link, t.data
    FROM tree t
  UNION ALL
    SELECT t.id, t.link, t.data
    FROM tree t, search_tree st
    WHERE t.id = st.link
)
SELECT * FROM search_tree;
```

깊이 우선 순서를 추가하려면:

```sql
WITH RECURSIVE search_tree(id, link, data, path) AS (
    SELECT t.id, t.link, t.data, ARRAY[t.id]
    FROM tree t
  UNION ALL
    SELECT t.id, t.link, t.data, path || t.id
    FROM tree t, search_tree st
    WHERE t.id = st.link
)
SELECT * FROM search_tree ORDER BY path;
```

일반적으로 고유하게 행을 식별하기 위해 여러 필드를 사용해야 하는 경우 행의 배열을 사용합니다:

```sql
WITH RECURSIVE search_tree(id, link, data, path) AS (
    SELECT t.id, t.link, t.data, ARRAY[ROW(t.f1, t.f2)]
    FROM tree t
  UNION ALL
    SELECT t.id, t.link, t.data, path || ROW(t.f1, t.f2)
    FROM tree t, search_tree st
    WHERE t.id = st.link
)
SELECT * FROM search_tree ORDER BY path;
```

> 팁: 단일 필드만 추적해야 하는 일반적인 경우에는 `ROW()` 구문을 생략합니다. 이렇게 하면 복합 타입 배열 대신 단순 배열이 사용되어 효율성이 향상됩니다.

##### 너비 우선 순서 (Breadth-First Order)

너비 우선 순서를 위해서는 검색 깊이를 추적하는 열을 추가할 수 있습니다:

```sql
WITH RECURSIVE search_tree(id, link, data, depth) AS (
    SELECT t.id, t.link, t.data, 0
    FROM tree t
  UNION ALL
    SELECT t.id, t.link, t.data, depth + 1
    FROM tree t, search_tree st
    WHERE t.id = st.link
)
SELECT * FROM search_tree ORDER BY depth;
```

안정적인 정렬을 얻으려면 데이터 열을 보조 정렬 열로 추가합니다.

##### 내장 SEARCH 문법

이러한 구문 형식은 `SEARCH` 절을 사용하여 내장 구문으로 사용할 수 있습니다:

```sql
WITH RECURSIVE search_tree(id, link, data) AS (
    SELECT t.id, t.link, t.data
    FROM tree t
  UNION ALL
    SELECT t.id, t.link, t.data
    FROM tree t, search_tree st
    WHERE t.id = st.link
) SEARCH DEPTH FIRST BY id SET ordercol
SELECT * FROM search_tree ORDER BY ordercol;
```

```sql
WITH RECURSIVE search_tree(id, link, data) AS (
    SELECT t.id, t.link, t.data
    FROM tree t
  UNION ALL
    SELECT t.id, t.link, t.data
    FROM tree t, search_tree st
    WHERE t.id = st.link
) SEARCH BREADTH FIRST BY id SET ordercol
SELECT * FROM search_tree ORDER BY ordercol;
```

`SEARCH` 절은 지정된 열 이름으로 암시적으로 CTE 출력 행에 열을 추가합니다(마지막 예제에서 `ordercol`). 이 열은 `ORDER BY` 절에서 사용될 때만 최종 쿼리에 나타납니다.

---

#### 7.8.2.2. 순환 감지 (Cycle Detection)

재귀 쿼리를 작업할 때 재귀 항이 결국 새로운 행을 반환하지 않도록 하는 것이 중요합니다. 그렇지 않으면 쿼리가 무한 루프에 빠집니다. 때때로 `UNION` 대신 `UNION ALL`을 사용하면 이미 출력된 행을 버림으로써 이를 달성할 수 있습니다. 그러나 종종 순환에는 완전히 동일하지 않은 출력 행이 포함됩니다: 이러한 경우 이전에 동일한 지점에 도달했는지 확인하기 위해 하나 또는 몇 개의 필드만 확인해야 할 수 있습니다.

이러한 상황을 처리하는 표준 방법은 이미 방문한 값의 배열을 계산하는 것입니다:

```sql
WITH RECURSIVE search_graph(id, link, data, depth, is_cycle, path) AS (
    SELECT g.id, g.link, g.data, 0,
      false,
      ARRAY[g.id]
    FROM graph g
  UNION ALL
    SELECT g.id, g.link, g.data, sg.depth + 1,
      g.id = ANY(path),
      path || g.id
    FROM graph g, search_graph sg
    WHERE g.id = sg.link AND NOT is_cycle
)
SELECT * FROM search_graph;
```

순환을 방지하는 것 외에도, 배열 값은 종종 특정 행에 도달하기 위해 취한 "경로"를 나타내므로 그 자체로 유용합니다.

일반적으로 순환을 인식하기 위해 여러 필드를 확인해야 하는 경우 행의 배열을 사용합니다:

```sql
WITH RECURSIVE search_graph(id, link, data, depth, is_cycle, path) AS (
    SELECT g.id, g.link, g.data, 0,
      false,
      ARRAY[ROW(g.f1, g.f2)]
    FROM graph g
  UNION ALL
    SELECT g.id, g.link, g.data, sg.depth + 1,
      ROW(g.f1, g.f2) = ANY(path),
      path || ROW(g.f1, g.f2)
    FROM graph g, search_graph sg
    WHERE g.id = sg.link AND NOT is_cycle
)
SELECT * FROM search_graph;
```

##### 내장 CYCLE 문법

이 구문은 `CYCLE` 절을 사용하여 내장 구문으로 사용할 수 있습니다:

```sql
WITH RECURSIVE search_graph(id, link, data, depth) AS (
    SELECT g.id, g.link, g.data, 1
    FROM graph g
  UNION ALL
    SELECT g.id, g.link, g.data, sg.depth + 1
    FROM graph g, search_graph sg
    WHERE g.id = sg.link
) CYCLE id SET is_cycle USING path
SELECT * FROM search_graph;
```

순환 경로 열은 깊이 우선 순서 열과 같은 방식으로 계산됩니다. 쿼리는 `SEARCH`와 `CYCLE` 절을 모두 가질 수 있지만, 깊이 우선 검색 사양과 순환 사양은 동일한 경로 열을 생성합니다. 따라서 깊이 우선 순서와 순환 감지가 모두 필요하면 `CYCLE` 절만 사용하고 경로 열로 정렬하는 것이 더 효율적입니다. 너비 우선 순서가 필요하지만 순환 감지도 필요하다면 두 절을 모두 사용해야 합니다.

##### 무한 루프 테스트 팁

쿼리가 루프에 빠지는지 여부가 확실하지 않을 때 테스트하는 유용한 방법은 부모 쿼리에 `LIMIT`을 배치하는 것입니다.

```sql
WITH RECURSIVE t(n) AS (
    SELECT 1
  UNION ALL
    SELECT n+1 FROM t
)
SELECT n FROM t LIMIT 100;
```

이것이 작동하는 이유는 PostgreSQL 구현이 부모 쿼리가 실제로 가져오는 만큼의 `WITH` 쿼리 행만 평가하기 때문입니다. 이 트릭을 프로덕션 환경에서 사용하는 것은 권장되지 않습니다. 다른 시스템이 다르게 작동할 수 있기 때문입니다. 또한 외부 쿼리가 재귀 쿼리의 모든 결과를 정렬하거나 다른 테이블과 조인하려고 하면 보통 작동하지 않습니다. 이러한 경우 외부 쿼리가 보통 `WITH` 쿼리의 모든 출력을 어쨌든 가져오려고 시도하기 때문입니다.

---

### 7.8.3. 공통 테이블 표현식 구체화

`WITH` 쿼리의 유용한 속성은 부모 쿼리나 형제 `WITH` 쿼리에서 두 번 이상 참조되더라도 일반적으로 부모 쿼리 실행당 한 번만 평가된다는 것입니다. 따라서 여러 곳에서 필요한 비용이 많이 드는 계산을 `WITH` 쿼리 내에 배치하여 중복 작업을 피할 수 있습니다. 또 다른 가능한 응용은 부작용이 있는 함수의 원치 않는 다중 평가를 방지하는 것입니다. 그러나 이 동전의 다른 면은 최적화기가 부모 쿼리의 제한을 다중 참조 `WITH` 쿼리로 밀어넣을 수 없다는 것입니다. 다중 참조 `WITH` 쿼리는 부모 쿼리가 나중에 버릴 수 있는 출력을 포함하여 작성된 대로 평가됩니다. (그러나 위에서 언급했듯이, 쿼리에 대한 참조가 제한된 수의 행만 요청하면 평가가 일찍 중지될 수 있습니다.)

그러나 `WITH` 쿼리가 비재귀적이고 부작용이 없으면(즉, 휘발성 함수를 포함하지 않는 `SELECT`이면) 부모 쿼리에 폴딩되어 두 쿼리 레벨의 공동 최적화가 가능합니다.

기본적으로 이것은 `WITH` 쿼리가 부모 쿼리에서 정확히 한 번만 참조될 때 발생합니다. 명시적 힌트를 사용하여 이 결정을 재정의할 수 있습니다.

#### 자동 폴딩 예제

```sql
WITH w AS (
    SELECT * FROM big_table
)
SELECT * FROM w WHERE key = 123;
```

이는 다음과 같이 최적화됩니다:

```sql
SELECT * FROM big_table WHERE key = 123;
```

특히 `big_table`에 `key` 열에 인덱스가 있다면 유용합니다. `WITH` 절에 `MATERIALIZED`를 지정하면 강제로 별도 계산됩니다.

#### 다중 참조 시 자동 구체화

```sql
WITH w AS (
    SELECT * FROM big_table
)
SELECT * FROM w AS w1 JOIN w AS w2 ON w1.key = w2.ref
WHERE w2.key = 123;
```

이 쿼리는 `WITH` 쿼리를 구체화하여 `big_table`의 임시 복사본을 생성한 다음 자체적으로 조인합니다. `w`의 사용은 `w2.key = 123` 조건과 관련이 없으므로 해당 제한은 구체화된 `WITH`를 사용하는 것으로부터 이익을 얻지 못합니다.

#### NOT MATERIALIZED 강제

`w`가 한 번 스캔되고 `w2.key = 123` 제한이 처음부터 적용되도록 다시 작성할 수 있습니다. 이렇게 하면 `big_table`의 많은 부분을 읽어야 할 수 있지만 그것은 사용자의 문제입니다. 이 원하는 동작은 다음과 같이 얻을 수 있습니다:

```sql
WITH w AS NOT MATERIALIZED (
    SELECT * FROM big_table
)
SELECT * FROM w AS w1 JOIN w AS w2 ON w1.key = w2.ref
WHERE w2.key = 123;
```

이렇게 하면 부모 쿼리의 제한사항이 다중 참조 `WITH` 쿼리에 직접 적용됩니다.

#### MATERIALIZED가 필요한 경우

반면에, `MATERIALIZED`를 지정하면 다중 참조가 없더라도 `WITH` 쿼리의 폴딩이 방지됩니다. 예를 들어:

```sql
WITH w AS MATERIALIZED (
    SELECT key, very_expensive_function(val) as f FROM some_table
)
SELECT * FROM w AS w1 JOIN w AS w2 ON w1.f = w2.f;
```

`WITH` 쿼리의 구체화는 `very_expensive_function`이 각 테이블 행에 대해 두 번이 아닌 한 번만 평가되도록 합니다.

> 참고: 이 예제들에서 `SELECT * FROM big_table`은 한 번만 사용되므로 폴딩되는 것보다 `w`가 위의 예제에서 별도로 구체화되는 것이 더 나은 것처럼 보일 수 있습니다. 그러나 이것은 `w1.key = w2.ref` 조인이 w의 두 인스턴스를 완전히 연결할 수 있다면 나쁜 선택입니다. `w`에서 생성된 임시 테이블의 행만 결합하는 것이 `big_table`에서 직접 같은 결과를 계산하는 것보다 훨씬 저렴합니다. 그래서 이것이 PostgreSQL이 `MATERIALIZED`를 암시적으로 적용하는 조건입니다.

---

### 7.8.4. WITH에서의 데이터 수정 문장

`WITH`에서 데이터 수정 문장(`INSERT`, `UPDATE`, `DELETE` 또는 `MERGE`)을 사용할 수 있습니다. 이를 통해 동일한 쿼리에서 여러 가지 다른 작업을 수행할 수 있습니다.

#### 기본 예제: 행 이동

```sql
WITH moved_rows AS (
    DELETE FROM products
    WHERE
        "date" >= '2010-10-01' AND
        "date" < '2010-11-01'
    RETURNING *
)
INSERT INTO products_log
SELECT * FROM moved_rows;
```

이 쿼리는 효과적으로 `products`의 행을 `products_log`로 이동합니다:

1. `products`에서 지정된 행 삭제
2. `RETURNING`을 통해 삭제된 내용 반환
3. 그 데이터를 `products_log`에 삽입

### 중요 포인트

`WITH`의 데이터 수정 문장은 정확히 한 번 실행되며, 항상 완료까지 실행됩니다. 기본 쿼리가 해당 출력의 전부(또는 어떤 것도)를 읽는지 여부와 무관합니다. 이것은 `WITH`의 `SELECT`에 대한 규칙과 다릅니다: 앞 섹션에서 설명한 것처럼 `SELECT`의 실행은 기본 쿼리가 출력을 요청하는 한도까지만 수행됩니다.

`WITH` 절의 서브 문장은 서로 그리고 기본 쿼리와 동시에 실행됩니다. 따라서 `WITH`에서 데이터 수정 문장을 사용할 때 지정된 업데이트가 실제로 발생하는 순서는 예측할 수 없습니다. 모든 문장은 동일한 스냅샷 으로 실행되므로(13장 참조), 대상 테이블에 대한 서로의 효과를 "볼" 수 없습니다.

### RETURNING이 없는 경우

```sql
WITH t AS (
    DELETE FROM foo
)
DELETE FROM bar;
```

이 경우:
- `foo`와 `bar`의 모든 행을 제거합니다.
- 클라이언트에 보고되는 영향받은 행 수는 `bar`에서 삭제된 행만 포함합니다.

### 재귀적 자기참조

재귀적 자기참조는 데이터 수정 문장에서 허용되지 않습니다. 어떤 경우에는 재귀 `WITH`의 출력을 참조하여 이 제한을 우회할 수 있습니다:

```sql
WITH RECURSIVE included_parts(sub_part, part) AS (
    SELECT sub_part, part FROM parts WHERE part = 'our_product'
  UNION ALL
    SELECT p.sub_part, p.part
    FROM included_parts pr, parts p
    WHERE p.part = pr.sub_part
)
DELETE FROM parts
  WHERE part IN (SELECT part FROM included_parts);
```

이 쿼리는 제품이나 그 하위 부품의 직접 또는 간접 부품인 모든 부품을 제거합니다.

### 스냅샷의 영향

데이터 수정 문장에서 `RETURNING` 데이터는 동일한 `WITH` 쿼리의 다른 데이터 수정 문장과 기본 쿼리에서 볼 수 있는 변경 사항을 의사소통하는 유일한 방법입니다. 예:

```sql
WITH t AS (
    UPDATE products SET price = price * 1.05
    RETURNING *
)
SELECT * FROM products;
```

외부 `SELECT`는 원래 가격 을 반환합니다. `UPDATE`의 효과가 아직 보이지 않기 때문입니다.

```sql
WITH t AS (
    UPDATE products SET price = price * 1.05
    RETURNING *
)
SELECT * FROM t;
```

외부 `SELECT`는 업데이트된 데이터 를 반환합니다.

### 중요 제한사항

#### 동일 행 중복 수정 불가

단일 문장에서 동일한 행을 두 번 수정하려고 시도하는 것은 지원되지 않습니다. 수정 중 하나만 발생하지만 어느 것인지 쉽게(또는 전혀) 예측하기 어렵습니다. 이것은 동일한 행을 두 번 삭제하거나 업데이트하려는 시도에도 적용됩니다. 따라서 일반적으로 단일 문장 내에서 두 서브 문장이 동일한 행을 수정하는 가능성을 피해야 합니다.

#### 규칙 제한

데이터 수정 문장의 대상 테이블은:
- 조건부 규칙을 가질 수 없습니다.
- `ALSO` 규칙을 가질 수 없습니다.
- 여러 문장으로 확장되는 `INSTEAD` 규칙을 가질 수 없습니다.

---

## 요약

| 기능 | 설명 |
|------|------|
| SELECT | 데이터베이스에서 데이터를 검색하는 기본 명령 |
| FROM | 쿼리의 데이터 소스를 지정 |
| JOIN | 여러 테이블을 결합 (INNER, LEFT, RIGHT, FULL, CROSS) |
| WHERE | 행을 필터링하는 조건 |
| GROUP BY | 동일한 값을 가진 행들을 그룹화 |
| HAVING | 그룹을 필터링하는 조건 |
| GROUPING SETS | 여러 그룹화 집합에 대한 집계 |
| ROLLUP | 계층적 그룹화 |
| CUBE | 모든 조합에 대한 그룹화 |
| 윈도우 함수 | 행 그룹에 대한 계산을 수행 |
| DISTINCT | 중복 행 제거 |
| UNION/INTERSECT/EXCEPT | 쿼리 결과 집합 연산 |
| ORDER BY | 결과 정렬 |
| LIMIT/OFFSET | 결과 행 수 제한 |
| VALUES | 상수 테이블 생성 |
| WITH (CTE) | 임시 테이블 정의로 복잡한 쿼리 단순화 |
| RECURSIVE | 자기참조 가능한 재귀 쿼리 |
| SEARCH | 깊이/너비 우선 순서 자동 생성 |
| CYCLE | 순환 감지 자동화 |
| MATERIALIZED | 강제 구체화 (중복 계산 방지) |
| NOT MATERIALIZED | 강제 폴딩 (최적화 허용) |
| Data-Modifying WITH | `INSERT/UPDATE/DELETE/MERGE`를 `WITH`에서 사용 |

---

## 참고

- PostgreSQL 18 공식 문서: [Chapter 7. Queries](https://www.postgresql.org/docs/18/queries.html)
