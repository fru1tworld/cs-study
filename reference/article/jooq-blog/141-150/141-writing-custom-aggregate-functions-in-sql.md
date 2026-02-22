# Java 8 Stream Collector처럼 SQL에서 커스텀 집계 함수 작성하기

> 원문: https://blog.jooq.org/writing-custom-aggregate-functions-in-sql/

모든 SQL 데이터베이스는 표준 집계 함수인 `COUNT()`, `SUM()`, `AVG()`, `MIN()`, `MAX()`를 지원합니다. 일부 데이터베이스는 `EVERY()`, `STDDEV_POP()`, `STDDEV_SAMP()`, `VAR_POP()`, `VAR_SAMP()`, `ARRAY_AGG()`, `STRING_AGG()` 등의 추가 집계 함수도 지원합니다.

하지만 직접 만들고 싶다면 어떨까요?

## Java 8 Stream과 Collector

Java 8 스트림을 사용할 때, `Collector`를 사용하면 자체 집계 함수를 쉽게 만들 수 있습니다. 예를 들어, 스트림에서 두 번째로 높은 값을 찾고 싶다고 가정해 봅시다. 가장 높은 값은 다음과 같이 얻을 수 있습니다:

```java
System.out.println(
    Stream.of(1, 2, 3, 4)
          .collect(Collectors.maxBy(Integer::compareTo))
);
```

결과:

```
Optional[4]
```

그렇다면 두 번째로 높은 값은 어떻게 구할까요? 스트림을 정렬하고 건너뛸 수도 있지만, 그것은 최적화된 방법이 아닙니다. `Collector`를 사용하면 다음과 같은 로직을 작성할 수 있습니다:

```java
System.out.println(
    Stream.of(1, 6, 2, 3, 4, 4, 5)
          .collect(Collector.of(

    // Collector.supplier(): 중간 데이터 구조 초기화
    () -> new int[] { Integer.MIN_VALUE, Integer.MIN_VALUE },

    // Collector.accumulator(): 스트림 값을 중간 데이터 구조에 누적
    (a, i) -> {
        if (a[0] < i) {
            a[1] = a[0];
            a[0] = i;
        }
        else if (a[1] < i)
            a[1] = i;
    },

    // Collector.combiner(): 병렬 스트림에서 두 중간 데이터 구조 병합
    (a1, a2) -> {
        throw new UnsupportedOperationException("병렬 스트림 지원 안 함");
    },

    // Collector.finisher(): 중간 데이터 구조에서 최종 값 추출
    a -> a[1]
)));
```

결과:

```
5
```

보시다시피, `Collector` 객체는 네 개의 "함수"로 구성됩니다:

- Supplier: 중간 데이터 구조를 초기화합니다. 우리의 경우, 최댓값(인덱스 0)과 두 번째 최댓값(인덱스 1)을 담는 두 원소짜리 `int[]` 배열입니다.
- Accumulator: 스트림 값을 중간 데이터 구조에 누적합니다. 우리의 경우, 새 값이 현재 최댓값보다 크면 이전 최댓값을 두 번째 최댓값 위치로 이동하고 새 값을 최댓값 위치에 저장합니다. 새 값이 최댓값보다는 작지만 두 번째 최댓값보다 크면 두 번째 최댓값 위치에 저장합니다.
- Combiner: 두 개의 중간 데이터 구조를 병합합니다. 이것은 병렬 스트림에서만 사용되며, 여기서는 간결함을 위해 생략합니다.
- Finisher: 중간 데이터 구조에서 최종 값을 추출합니다. 우리의 경우, 두 번째 최댓값이 저장된 인덱스 1의 값입니다.

## Oracle에서의 동일한 접근 방식

Oracle에서는 Java의 `Collector` 메서드와 대응되는 ODCI(Oracle Data Cartridge Interface) 메서드를 구현하는 커스텀 타입을 생성해야 합니다. 다소 의례적인 구문이 필요하지만, 아이디어는 동일합니다:

```sql
-- 타입 명세 생성
CREATE TYPE u_second_max AS OBJECT (

  -- Collector.supplier()와 동등: 중간 데이터 구조
  MAX NUMBER,
  SECMAX NUMBER,

  -- Collector.supplier()와 동등
  STATIC FUNCTION ODCIAggregateInitialize(ctx IN OUT u_second_max) RETURN NUMBER,

  -- Collector.accumulator()와 동등
  MEMBER FUNCTION ODCIAggregateIterate(self IN OUT u_second_max, value IN NUMBER) RETURN NUMBER,

  -- Collector.combiner()와 동등
  MEMBER FUNCTION ODCIAggregateMerge(self IN OUT u_second_max, ctx2 IN u_second_max) RETURN NUMBER,

  -- Collector.finisher()와 동등
  MEMBER FUNCTION ODCIAggregateTerminate(self IN u_second_max, returnValue OUT NUMBER, flags IN NUMBER) RETURN NUMBER
);
/
```

그리고 타입 본문을 생성합니다:

```sql
CREATE TYPE BODY u_second_max IS

  -- Collector.supplier()와 동등
  STATIC FUNCTION ODCIAggregateInitialize(ctx IN OUT u_second_max)
  RETURN NUMBER IS
  BEGIN
    ctx := u_second_max(NULL, NULL);
    RETURN ODCIConst.Success;
  END;

  -- Collector.accumulator()와 동등
  MEMBER FUNCTION ODCIAggregateIterate(self IN OUT u_second_max, value IN NUMBER)
  RETURN NUMBER IS
  BEGIN
    IF value > self.MAX OR self.MAX IS NULL THEN
      self.SECMAX := self.MAX;
      self.MAX := value;
    ELSIF value > self.SECMAX OR self.SECMAX IS NULL THEN
      self.SECMAX := value;
    END IF;
    RETURN ODCIConst.Success;
  END;

  -- Collector.combiner()와 동등
  MEMBER FUNCTION ODCIAggregateMerge(self IN OUT u_second_max, ctx2 IN u_second_max)
  RETURN NUMBER IS
  BEGIN
    IF ctx2.MAX > self.MAX OR self.MAX IS NULL THEN
      IF self.MAX > ctx2.SECMAX OR ctx2.SECMAX IS NULL THEN
        self.SECMAX := self.MAX;
      ELSE
        self.SECMAX := ctx2.SECMAX;
      END IF;
      self.MAX := ctx2.MAX;
    ELSIF ctx2.MAX > self.SECMAX OR self.SECMAX IS NULL THEN
      self.SECMAX := ctx2.MAX;
    END IF;
    RETURN ODCIConst.Success;
  END;

  -- Collector.finisher()와 동등
  MEMBER FUNCTION ODCIAggregateTerminate(self IN u_second_max, returnValue OUT NUMBER, flags IN NUMBER)
  RETURN NUMBER IS
  BEGIN
    returnValue := self.SECMAX;
    RETURN ODCIConst.Success;
  END;
END;
/
```

이제 위 타입을 사용하여 집계 함수를 생성할 수 있습니다:

```sql
CREATE FUNCTION SECOND_MAX(input NUMBER) RETURN NUMBER
PARALLEL_ENABLE AGGREGATE USING u_second_max;
/
```

이 집계 함수는 다른 집계 함수처럼 사용할 수 있습니다:

```sql
WITH t(v) AS (
  SELECT 1 FROM dual UNION ALL
  SELECT 2 FROM dual UNION ALL
  SELECT 3 FROM dual UNION ALL
  SELECT 4 FROM dual UNION ALL
  SELECT 5 FROM dual UNION ALL
  SELECT 6 FROM dual
)
SELECT MAX(v), SECOND_MAX(v)
FROM t;
```

결과:

```
MAX(V)  SECOND_MAX(V)
------- -------------
6       5
```

그리고 최고의 장점은, 이 집계 함수를 무료로 윈도우 함수로도 사용할 수 있다는 것입니다:

```sql
WITH t(v) AS (
  SELECT 1 FROM dual UNION ALL
  SELECT 2 FROM dual UNION ALL
  SELECT 3 FROM dual UNION ALL
  SELECT 4 FROM dual UNION ALL
  SELECT 5 FROM dual UNION ALL
  SELECT 6 FROM dual
)
SELECT
  v,
  MAX(v) OVER (ORDER BY v),
  SECOND_MAX(v) OVER (ORDER BY v)
FROM t;
```

결과:

```
V  MAX(V) OVER (ORDER BY V)  SECOND_MAX(V) OVER (ORDER BY V)
-- ------------------------  --------------------------------
1  1                         (null)
2  2                         1
3  3                         2
4  4                         3
5  5                         4
6  6                         5
```

매우 멋지지 않나요?

## PostgreSQL에서의 동일한 접근 방식

PostgreSQL의 구문은 Oracle보다 훨씬 간단합니다. 중간 데이터 구조 타입(공급자), SFUNC(누적기), FINALFUNC(종료기)만 지정하면 됩니다:

```sql
-- 중간 상태를 저장할 타입 생성
CREATE TYPE u_second_max AS (
  MAX INT,
  SECMAX INT
);

-- 상태 전이 함수 (accumulator)
CREATE FUNCTION u_second_max_sfunc(s u_second_max, v INT)
RETURNS u_second_max AS
$$
BEGIN
  IF s IS NULL THEN
    s := (NULL, NULL)::u_second_max;
  END IF;

  IF v > s.MAX OR s.MAX IS NULL THEN
    s.SECMAX := s.MAX;
    s.MAX := v;
  ELSIF v > s.SECMAX OR s.SECMAX IS NULL THEN
    s.SECMAX := v;
  END IF;

  RETURN s;
END;
$$ LANGUAGE plpgsql;

-- 최종 함수 (finisher)
CREATE FUNCTION u_second_max_finalfunc(s u_second_max)
RETURNS INT AS
$$
BEGIN
  RETURN s.SECMAX;
END;
$$ LANGUAGE plpgsql;

-- 집계 함수 생성
CREATE AGGREGATE SECOND_MAX(INT) (
  SFUNC = u_second_max_sfunc,    -- Collector.accumulator()와 동등
  STYPE = u_second_max,          -- Collector.supplier()와 동등 (타입 지정)
  FINALFUNC = u_second_max_finalfunc  -- Collector.finisher()와 동등
);
```

이 집계 함수를 사용하면:

```sql
WITH t(v) AS (
  VALUES (1), (2), (3), (4), (5), (6)
)
SELECT MAX(v), SECOND_MAX(v)
FROM t;
```

결과:

```
max | second_max
----+-----------
  6 |          5
```

PostgreSQL에서도 마찬가지로 윈도우 함수로 사용할 수 있습니다:

```sql
WITH t(v) AS (
  VALUES (1), (2), (3), (4), (5), (6)
)
SELECT
  v,
  MAX(v) OVER (ORDER BY v),
  SECOND_MAX(v) OVER (ORDER BY v)
FROM t;
```

결과:

```
v | max | second_max
--+-----+-----------
1 |   1 |     (null)
2 |   2 |          1
3 |   3 |          2
4 |   4 |          3
5 |   5 |          4
6 |   6 |          5
```

## 결론

많은 다른 데이터베이스들도 사용자 정의 집계 함수를 지정할 수 있습니다. 데이터베이스 매뉴얼의 세부 사항을 참고하여 더 알아보세요. 이들은 항상 Java 8의 `Collector`와 동일한 방식으로 작동합니다.

핵심 매핑:

| Java 8 Collector | Oracle ODCI | PostgreSQL |
|-----------------|-------------|------------|
| `supplier()` | `ODCIAggregateInitialize` | `STYPE` (타입 지정) |
| `accumulator()` | `ODCIAggregateIterate` | `SFUNC` |
| `combiner()` | `ODCIAggregateMerge` | (자동 지원) |
| `finisher()` | `ODCIAggregateTerminate` | `FINALFUNC` |

이 패턴을 이해하면 표준 집계 함수로는 불가능한 복잡한 집계 로직을 데이터베이스에서 직접 구현할 수 있습니다. 그리고 추가 코드 없이 윈도우 함수로도 사용할 수 있다는 것은 큰 보너스입니다!
