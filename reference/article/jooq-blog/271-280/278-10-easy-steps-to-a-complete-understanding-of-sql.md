# SQL을 완전히 이해하기 위한 10가지 쉬운 단계

> 원문: https://blog.jooq.org/10-easy-steps-to-a-complete-understanding-of-sql/

## 소개

이 글은 흔한 문제를 다룹니다: 많은 프로그래머들이 SQL을 두려워합니다. SQL은 명령형, 객체지향, 함수형 프로그래밍 패러다임과 근본적으로 다른 선언적 언어이기 때문입니다. SQL 교육을 진행하고 jOOQ 라이브러리를 개발하는 저자는 세 부류의 독자를 위해 SQL을 쉽게 설명하고자 합니다: SQL을 사용해봤지만 완전히 이해하지 못한 사람들, SQL을 알지만 문법을 깊이 살펴본 적 없는 사람들, 그리고 다른 사람에게 SQL을 가르치는 사람들입니다.

이 튜토리얼은 SELECT 문에만 집중합니다.

---

## 1단계: SQL은 선언적이다

핵심 개념: SQL은 선언적으로 동작합니다—결과를 *어떻게* 계산할지가 아니라 *무엇을* 원하는지를 명시합니다.

예시:
```sql
SELECT first_name, last_name
FROM employees
WHERE salary > 100000
```

핵심 교훈: 대부분의 프로그래머는 명령형으로 생각합니다(이것을 하고, 저것을 하고, 조건을 확인하고). 계산 단계를 지시하기보다 원하는 결과를 *선언*하는 방향으로 사고방식을 전환하세요. 임시 변수, 루프, 순차적 함수 호출을 버리세요.

---

## 2단계: SQL 문법 순서는 직관적이지 않다

어휘 순서 (작성하는 순서):
- SELECT [DISTINCT]
- FROM
- WHERE
- GROUP BY
- HAVING
- UNION
- ORDER BY

논리적 순서 (실행되는 순서):
- FROM
- WHERE
- GROUP BY
- HAVING
- SELECT
- DISTINCT
- UNION
- ORDER BY

세 가지 중요한 점:

1. FROM이 먼저 실행됩니다. 작업이 시작되기 전에 디스크에서 메모리로 데이터를 로드합니다.

2. SELECT는 대부분의 절 이후에 실행됩니다. 특히 FROM과 GROUP BY 이후에 실행됩니다. 이것이 WHERE 절에서 SELECT 절의 별칭을 참조할 수 없는 이유입니다:

```sql
SELECT A.x + A.y AS z
FROM A
WHERE z = 10 -- 여기서 z를 사용할 수 없습니다!
```

해결 방법: 표현식을 반복하거나 파생 테이블/CTE를 사용하세요:

```sql
SELECT A.x + A.y AS z
FROM A
WHERE (A.x + A.y) = 10
```

3. UNION이 ORDER BY보다 먼저 옵니다. 두 순서 모두에서 그렇습니다. 개별 UNION 하위 쿼리는 일반적으로 독립적으로 정렬할 수 없습니다; 일부 데이터베이스에서는 하위 쿼리 정렬을 허용하지만 UNION 후 유지가 보장되지 않습니다.

핵심 교훈: 어휘 순서와 논리적 순서를 모두 이해하면 흔한 실수를 방지할 수 있습니다. 이상적으로 SQL은 어휘 순서가 논리적 실행을 반영하도록 설계되었어야 합니다.

---

## 3단계: SQL은 테이블 참조가 중심이다

기본 원칙: 컬럼이 일등 시민이 아닙니다—테이블 참조가 일등 시민입니다.

FROM 절은 결합된 테이블 참조를 생성합니다. 다음과 같이 작성하면:

```sql
FROM a, b
```

이것은 `a의_컬럼 수 + b의_컬럼 수`의 차수를 가진 결합된 테이블을 생성합니다. `a`에 3개의 레코드가 있고 `b`에 5개의 레코드가 있으면, 출력에는 15개의 레코드가 포함됩니다 (3 × 5 카르테시안 곱).

이 "출력"은 후속 절(WHERE, GROUP BY)을 통해 흐르며, 각 절이 관계를 변환합니다. 관계 대수 관점에서 SQL 테이블은 *관계* 또는 *튜플의 집합*이며, 각 절이 관계를 변환하여 새로운 관계를 생성합니다.

핵심 교훈: 데이터가 SQL 절을 통해 "파이프라인"처럼 흐르는 것을 이해하기 위해 항상 테이블 참조 관점에서 생각하세요.

---

## 4단계: 테이블 참조는 강력하다

테이블 참조는 단순한 테이블 이름 이상으로 확장됩니다:

```
<테이블 참조> ::=
    <테이블 이름>
  | <파생 테이블>
  | <조인된 테이블>
```

조인된 테이블은 단순 테이블 이름을 대체할 수 있습니다:

```sql
FROM a1 JOIN a2 ON a1.id = a2.id, b
```

이 결합된 참조는 `a1+a2+b`의 차수를 갖습니다. 파생 테이블은 조인된 테이블보다 더 정교합니다.

핵심 교훈: 복잡한 SQL 구조를 이해하기 위해 테이블 참조 구성을 마스터하세요.

---

## 5단계: 쉼표로 구분된 테이블보다 JOIN을 사용하라

나쁜 방식:
```sql
FROM a, b, c, d
WHERE a.a1 = b.bx
AND a.a2 = c.c1
-- 등등...
```

좋은 방식: 명시적 JOIN 구문을 사용하세요—더 안전하고, 더 표현력 있고, 오류 가능성이 적습니다:

```sql
FROM a
JOIN b ON a.a1 = b.bx
JOIN c ON a.a2 = c.c1
```

장점:
- 조인 조건이 조인된 테이블 근처에 있어 누락을 방지합니다
- INNER JOIN, OUTER JOIN 및 기타 변형을 명시적으로 구분합니다

핵심 교훈: 항상 JOIN 구문을 사용하세요; 쉼표로 구분된 테이블 참조는 버리세요.

---

## 6단계: SQL JOIN 연산

다섯 가지 관계형 조인 유형이 있습니다. SQL 용어는 다를 수 있습니다:

### EQUI JOIN

가장 일반적이며 두 가지 하위 유형이 있습니다:

- INNER JOIN: 저자와 그들의 책; 책이 없는 저자는 제외됩니다.
```sql
author JOIN book ON author.id = book.author_id
```

- OUTER JOIN (LEFT, RIGHT, FULL): 저자와 그들의 책, 책이 없는 저자에 대한 "빈" 레코드 추가(책 컬럼은 NULL).
```sql
author LEFT OUTER JOIN book ON author.id = book.author_id
```

### SEMI JOIN

다른 테이블에 일치하는 항목이 존재하는 한 테이블의 행만 중복 없이 반환합니다—결과의 "절반"만:

```sql
-- IN 사용
FROM author
WHERE author.id IN (SELECT book.author_id FROM book)

-- EXISTS 사용
FROM author
WHERE EXISTS (SELECT 1 FROM book WHERE book.author_id = author.id)
```

고려사항:
- IN 조건은 일반적으로 더 읽기 쉽습니다
- EXISTS 조건은 더 표현력 있습니다
- 성능은 일반적으로 동일하지만 데이터베이스별 변형이 있습니다

흔한 실수: SEMI JOIN을 시뮬레이션하기 위해 INNER JOIN과 DISTINCT를 사용하는 것—비효율적이고 잘못된 방식입니다:

```sql
SELECT DISTINCT first_name, last_name
FROM author
JOIN book ON author.id = book.author_id
-- 나쁨! 느리고 여러 JOIN이 있을 때 의미적으로 틀립니다
```

### ANTI JOIN

SEMI JOIN의 반대—다른 곳에 일치하는 항목이 *존재하지 않는* 한 테이블의 행:

```sql
-- IN 사용
FROM author
WHERE author.id NOT IN (SELECT book.author_id FROM book)

-- EXISTS 사용
FROM author
WHERE NOT EXISTS (SELECT 1 FROM book WHERE book.author_id = author.id)
```

주의: NOT IN은 NULL 처리에 복잡성이 있습니다.

### CROSS JOIN

카르테시안 곱을 생성합니다—첫 번째 테이블의 모든 레코드와 두 번째 테이블의 모든 레코드:

```sql
author CROSS JOIN book
```

### DIVISION

관계 나눗셈은 JOIN의 역연산입니다—SQL에서 표현하기 매우 어려우며 이 튜토리얼의 범위를 벗어납니다.

핵심 교훈: 관계형 조인 연산을 깊이 이해하고 SQL로 올바르게 변환하세요.

---

## 7단계: 파생 테이블은 테이블 변수처럼 작동한다

SQL이 선언적이지만(변수가 존재하지 않아야 하지만), 파생 테이블은 이 기능을 근사합니다—괄호로 감싼 하위 쿼리:

```sql
FROM (SELECT * FROM author)
```

일부 방언에서는 상관 이름(별칭)이 필요합니다:

```sql
FROM (SELECT * FROM author) a
```

논리적 순서 문제 우회:

중첩을 통해 SELECT와 WHERE에서 컬럼 표현식을 재사용합니다:

```sql
SELECT first_name, last_name, age
FROM (
  SELECT first_name, last_name, current_date - date_of_birth age
  FROM author
)
WHERE age > 10000
```

공통 테이블 표현식(CTE): SQL:1999 이후 표준에서는 파생 테이블을 재사용하기 위한 WITH 절을 도입했습니다:

```sql
WITH a AS (
  SELECT first_name, last_name, current_date - date_of_birth age
  FROM author
)
SELECT *
FROM a
WHERE age > 10000
```

뷰: 더 넓은 재사용을 위해 파생 테이블을 독립적인 뷰로 외부화합니다.

핵심 교훈: 파생 테이블과 CTE는 테이블 참조를 활용하여 순서 문제에 대한 우아한 해결책을 제공합니다.

---

## 8단계: GROUP BY는 테이블 참조를 변환한다

결합된 테이블 참조에 GROUP BY를 적용하면:

```sql
FROM a, b
GROUP BY A.x, A.y, B.z
```

...오직 *세 개의 컬럼만 남는* 새로운 테이블을 생성합니다—SELECT를 포함한 후속 절에서 사용 가능한 컬럼을 줄입니다. 이것이 SELECT 절에 GROUP BY 컬럼(또는 집계 함수 인수)만 나타나는 이유입니다:

```sql
SELECT A.x, A.y, SUM(A.z)
FROM A
GROUP BY A.x, A.y
```

MySQL 참고: 역사적으로 MySQL은 이 표준을 위반하여 SELECT에서 그룹화되지 않은 컬럼을 허용했습니다. 버전 8부터 기본적으로 엄격 모드가 활성화되어 있습니다.

핵심 교훈: GROUP BY는 테이블 참조를 구조적으로 변환하여 후속 컬럼 가용성을 제한합니다.

---

## 9단계: SELECT는 관계 대수에서 프로젝션이다

테이블 참조를 생성, 필터링, 변환한 후 SELECT는 최종 형태로 *프로젝트*합니다—마치 프로젝터가 각 레코드를 변환하는 것처럼.

SELECT 내의 규칙:

1. 이전 테이블 참조에서 생성 가능한 컬럼만 참조하세요
2. GROUP BY가 있으면 그룹화된 컬럼만 참조하거나 집계 함수를 사용하세요
3. GROUP BY가 없으면 집계 대신 윈도우 함수를 사용하세요
4. GROUP BY 없이 집계 함수와 비집계 함수를 결합하지 마세요
5. 집계 내의 함수와 그 반대에 대한 복잡한 래핑 규칙이 있습니다
6. 명시적 GROUP BY 없이 집계가 나타나면 암시적 빈 GROUPING SETS(SQL:2003)가 적용됩니다

복잡성 설명: GROUP BY 없이 집계가 나타나면 암시적 빈 그룹화 집합이 활성화되어 SELECT의 프로젝션이 논리적으로 앞선 절에 소급하여 영향을 미칩니다.

핵심 교훈: SELECT는 단순해 보이지만 가장 복잡한 절입니다. SELECT를 마스터하기 전에 다른 절을 먼저 마스터하세요.

---

## 10단계: DISTINCT, UNION, ORDER BY, OFFSET은 간단하다

SELECT의 복잡성 이후, 나머지 연산은 단순해집니다:

### 집합 연산

- DISTINCT: 프로젝션 후 중복 제거
- UNION: 하위 쿼리를 연결하고 중복 제거
- UNION ALL: 하위 쿼리를 연결하고 중복 유지
- EXCEPT: 두 번째 하위 쿼리에 나타나는 첫 번째 하위 쿼리의 레코드 제거
- INTERSECT: 두 하위 쿼리 모두에 있는 레코드만 유지

중복 제거가 종종 불필요합니다; UNION ALL이 자주 충분합니다.

### 정렬 연산

정렬은 관계형이 아닙니다—SQL에 특화되어 있습니다. ORDER BY와 OFFSET...FETCH는 인덱싱된 레코드 접근을 보장하는 *유일한* 방법입니다. 다른 모든 순서는 임의적입니다.

구문 변형:
- SQL 표준: ORDER BY...OFFSET...FETCH
- MySQL/PostgreSQL: LIMIT...OFFSET
- SQL Server/Sybase: TOP...START AT

핵심 교훈: 이러한 연산들은 이전 단계들에 비해 개념적으로 간단합니다.

---

## 결론

SQL 숙달에는 연습이 필요합니다. 이 열 가지 단계는 기초적인 이해를 제공하지만, 흔한 실수에 노출되면서 학습이 가속화됩니다. 저자는 세 가지 관련 글을 참조합니다:

- "Java 개발자가 SQL을 작성할 때 저지르는 10가지 흔한 실수"
- "Java 개발자가 SQL을 작성할 때 저지르는 10가지 추가 실수"
- "Java 개발자가 SQL을 작성할 때 저지르는 또 다른 10가지 흔한 실수"

이 블로그는 테이블 참조 이해, 논리적 순서와 어휘 순서의 존중, 그리고 SQL의 선언적 특성 인식이 효과적인 쿼리 작성의 기본임을 강조합니다.
