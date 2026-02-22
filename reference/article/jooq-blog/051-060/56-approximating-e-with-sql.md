# SQL로 자연상수 e 근사하기

> 원문: https://blog.jooq.org/approximating-e-with-sql/

PostgreSQL을 사용하고 있다면, 다음과 같은 멋진 쿼리를 실행해 볼 수 있습니다:

```sql
WITH RECURSIVE
  r (r, i) AS (
    SELECT random(), i
    FROM generate_series(1, 1000000) AS t (i)
  ),
  s (ri, s, i) AS (
    SELECT i, r, i
    FROM r
    UNION ALL
    SELECT s.ri, r.r + s.s, s.i + 1
    FROM r
    JOIN s ON r.i = s.i + 1
    WHERE r.r + s.s <= 1
  ),
  n (n) AS (
    SELECT max(i) - min(i) + 2
    FROM s
    GROUP BY ri
  )
SELECT avg(n)
FROM n
```

이 쿼리는 (잠시 후) 무엇을 출력할까요? 바로 `e`를 (거의) 출력합니다. 다음은 몇 가지 실행 결과 예시입니다:

```
2.7169115477960698
2.7164145522690296
2.7172065451410937
2.7170815462660836
```

완벽하지는 않죠. 다음은 SQL로 작성한 더 나은 근사값입니다:

```sql
SELECT exp(1);
```

결과:

```
2.718281828459045
```

충분히 가깝습니다... 그러면 이 쿼리는 어떻게 동작할까요? 이것은 여러 번 설명된 바 있는 멋진 근사 방법입니다. 예를 들어 [여기](https://math.stackexchange.com/q/111314/261588)에서 확인할 수 있습니다. 말로 표현하면:

> 평균적으로, 0과 1 사이의 난수 값들의 합이 1을 초과할 때까지 필요한 난수의 개수는 e개이다.

쿼리를 다시 살펴보겠습니다:

```sql
WITH RECURSIVE
  -- "0과 1 사이의 난수 값들"
  r (r, i) AS (
    SELECT random(), i
    FROM generate_series(1, 1000000) AS t (i)
  ),
  s (ri, s, i) AS (
    SELECT i, r, i
    FROM r
    UNION ALL
    SELECT s.ri, r.r + s.s, s.i + 1
    FROM r
    JOIN s ON r.i = s.i + 1
    -- "... 합이 1을 초과할 때까지"
    WHERE r.r + s.s <= 1
  ),
  -- "~까지 필요한 값의 개수"
  n (n) AS (
    SELECT max(i) - min(i) + 2
    FROM s
    GROUP BY ri
  )
-- "평균적으로"
SELECT avg(n)
FROM n
```

말로 풀어서, 위에서 아래로 읽으면:

* 0과 1 사이의 난수 100만 개를 생성합니다
* 각 값을 시작점으로, 합이 1을 초과하지 않는 한 연속된 값들을 더해 나갑니다
* 각 값에 대해 합이 1을 초과할 때까지 몇 개의 값이 필요했는지 확인합니다
* 그 값의 개수에 대한 평균을 구합니다

매우 비효율적이지만, 그게 요점은 아니었습니다. :-)
