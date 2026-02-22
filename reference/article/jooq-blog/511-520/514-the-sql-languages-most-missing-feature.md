# SQL 언어의 가장 부족한 기능

> 원문: https://blog.jooq.org/the-sql-languages-most-missing-feature/

SQL은 매우 표현력이 풍부한 언어입니다. 하지만 SQL의 표현력에도 불구하고, 이 언어에는 정말 부족한 기능이 하나 있습니다. 바로 공통 컬럼 표현식(common column expressions)입니다.

## 문제점

SQL에서 복잡한 표현식을 작성할 때, 동일한 표현식을 여러 절에서 반복해서 작성해야 하는 경우가 많습니다. 다음 예제를 살펴보겠습니다:

```sql
SELECT   first_name || ' ' || last_name
FROM     customers
WHERE    first_name || ' ' || last_name LIKE 'L% E%'
GROUP BY first_name || ' ' || last_name
ORDER BY first_name || ' ' || last_name
```

보시다시피, `first_name || ' ' || last_name`이라는 동일한 연결(concatenation) 표현식이 SELECT, WHERE, GROUP BY, ORDER BY 절에서 네 번이나 반복됩니다. 이는 코드의 가독성을 떨어뜨리고 유지보수를 어렵게 만듭니다. 만약 이 표현식을 수정해야 한다면, 네 곳 모두를 수정해야 합니다.

## 현재의 우회 방법들

### 1. 컬럼 별칭(Column Aliasing) - 부분적인 해결책

SELECT 절에서 정의한 컬럼 별칭을 ORDER BY 절에서는 사용할 수 있습니다:

```sql
SELECT   first_name || ' ' || last_name AS name
FROM     customers
WHERE    first_name || ' ' || last_name LIKE 'L% E%'
GROUP BY first_name || ' ' || last_name
ORDER BY name
```

하지만 이 방법은 ORDER BY 절에서만 별칭을 사용할 수 있다는 한계가 있습니다. WHERE 절과 GROUP BY 절에서는 여전히 전체 표현식을 반복해야 합니다. 이는 SQL의 논리적 처리 순서 때문입니다 - WHERE와 GROUP BY는 SELECT보다 먼저 처리되므로, SELECT에서 정의한 별칭을 참조할 수 없습니다.

### 2. 파생 테이블(Derived Tables) 또는 CTE 사용 - 더 장황한 방법

공통 테이블 표현식(CTE)이나 파생 테이블을 사용하면 표현식 반복을 피할 수 있습니다:

```sql
WITH c(name) AS (
  SELECT first_name || ' ' || last_name
  FROM   customers
)
SELECT name
FROM c
WHERE name LIKE 'L% E%'
GROUP BY name
ORDER BY name
```

이 방법은 작동하지만, 단순히 컬럼 표현식에 이름을 붙이기 위해 전체 쿼리 구조를 변경해야 한다는 점에서 과도하게 장황합니다. 간단한 문제를 해결하기 위해 복잡한 구조를 도입하는 것은 바람직하지 않습니다.

### 3. LATERAL JOIN 또는 CROSS APPLY 사용

PostgreSQL에서는 LATERAL JOIN을, SQL Server에서는 CROSS APPLY를 사용하여 이 문제를 해결할 수 있습니다:

```sql
SELECT name
FROM   customers
CROSS JOIN LATERAL (
  SELECT first_name || ' ' || last_name AS name
) AS computed
WHERE  name LIKE 'L% E%'
GROUP BY name
ORDER BY name
```

이 방법도 작동하지만, 여전히 원래 의도보다 더 복잡한 구문을 사용해야 합니다.

## 제안하는 해결책: 공통 컬럼 표현식

SQL에 진정으로 필요한 것은 테이블 표현식에 붙일 수 있는 범위 지정(scoped) WITH 절을 사용한 공통 컬럼 표현식입니다:

```sql
FROM     customers
WITH     name AS first_name || ' ' || last_name
```

이렇게 하면 마치 해당 추가 컬럼이 포함된 파생 테이블을 만든 것과 논리적으로 동일한 효과를 얻을 수 있습니다. 이 `name` 컬럼은 쿼리의 모든 절(SELECT, WHERE, GROUP BY, ORDER BY 등)에서 마치 테이블의 실제 컬럼인 것처럼 사용할 수 있게 됩니다.

완전한 쿼리는 다음과 같이 작성될 수 있습니다:

```sql
SELECT   name
FROM     customers
WITH     name AS first_name || ' ' || last_name
WHERE    name LIKE 'L% E%'
GROUP BY name
ORDER BY name
```

이것은 훨씬 더 깔끔하고 유지보수하기 쉬운 코드입니다.

## 결론

SQL:1999에서 쿼리를 위한 공통 테이블 표현식(CTE)이 도입된 이후, SQL은 표현식 재사용 측면에서 큰 발전을 이루었습니다. 하지만 컬럼 수준에서의 유사한 기능은 여전히 부재합니다.

SQL 언어가 가장 절실하게 필요로 하는 기능은 바로 공통 컬럼 표현식입니다. 이 기능이 있다면 SQL의 고질적인 장황함 문제를 해결하고, 더 간결하고 유지보수하기 쉬운 쿼리를 작성할 수 있을 것입니다.

안타깝게도 현재 어떤 주요 데이터베이스 벤더도 이 기능을 구현하지 않았으며, SQL 표준에도 포함되어 있지 않습니다. 하지만 이 기능이 미래의 SQL 표준에 추가된다면, SQL 개발자들의 생산성과 코드 품질에 큰 도움이 될 것입니다.
