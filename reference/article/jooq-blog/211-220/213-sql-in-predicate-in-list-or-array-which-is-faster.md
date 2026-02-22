# SQL IN 조건절: IN 리스트와 배열 중 어느 것이 더 빠를까?

> 원문: https://blog.jooq.org/sql-in-predicate-with-in-list-or-with-array-which-is-faster/

하! 또 덕후 함정에 빠졌습니다:

https://stackoverflow.com/questions/43099226/how-to-make-jooq-to-use-arrays-in-the-in-clause/43102102

한 jOOQ 사용자가 왜 jOOQ가 다음과 같은 조건절에 대해 `IN` 리스트를 생성하는지 궁금해했습니다:

Java

```java
COLUMN.in(1, 2, 3, 4)
```

SQL

```sql
COLUMN in (?, ?, ?, ?)
```

... 사실 다음과 같은 조건절을 생성할 수도 있었는데 말이죠:

```sql
COLUMN = any(?::int[])
```

두 번째 경우에는 4개 대신 단 하나의 바인드 변수만 있으면 되고, SQL 생성 및 파싱 작업이 "훨씬" 줄어들 것입니다 (크기 4의 `IN` 리스트에서는 아닐 수도 있지만, 50개 값의 리스트를 상상해 봅시다).

## 주의 사항

먼저 주의 사항입니다: 커서 캐시(cursor cache) / 실행 계획 캐시(plan cache)가 있는 데이터베이스(예: Oracle이나 SQL Server)에서는 긴 `IN` 리스트를 사용할 때 주의해야 합니다. 왜냐하면 매번 실행할 때마다 하드 파싱(hard parse)이 발생할 가능성이 높기 때문입니다. 정확히 같은 조건절(리스트에 371개의 요소가 있는)을 다시 실행할 때쯤이면, 실행 계획이 이미 캐시에서 제거되었을 것입니다. 따라서 캐시의 이점을 실제로 누릴 수 없습니다.

저는 이 문제를 알고 있으며, 이것은 곧 다른 블로그 포스트의 주제가 될 것입니다. 지금은 PostgreSQL의 "실행 계획 캐시"가 그다지 정교하지 않은 PostgreSQL에 집중합시다.

## 추측하지 말고, 측정하라

질문은 SQL 문 파싱 속도를 개선하는 것에 관한 것이었습니다. 파서는 정말 빠르기 때문에 파싱은 문제가 되지 않습니다. 실행 계획을 생성하는 것은 확실히 더 많은 시간이 걸리지만, 다시 말해 PostgreSQL의 실행 계획 캐시가 그다지 정교하지 않기 때문에 여기서는 이 문제가 영향을 미치지 않습니다. 그래서 질문은 정말로 다음과 같습니다:

`IN` 리스트가 PostgreSQL에서 정말 그렇게 나쁜가요?

배열 바인드 변수가 훨씬 더 나을까요?

벤치마킹에 관한 최근 포스트 이후, 우리는 절대 추측하지 말고 항상 측정해야 한다는 것을 알게 되었습니다. 저는 다시 Sakila 데이터베이스를 사용하여 다음 두 쿼리를 실행합니다:

```sql
-- IN 리스트
SELECT *
FROM film
JOIN film_actor USING (film_id)
JOIN actor USING (actor_id)
WHERE film_id IN (?, ?, ?, ?)

-- 배열
SELECT *
FROM film
JOIN film_actor USING (film_id)
JOIN actor USING (actor_id)
WHERE film_id = ANY(?)
```

먼저 길이 4의 리스트로 시도해 봅시다. 벤치마크는 다음과 같습니다:

```sql
DO $$
DECLARE
  v_ts TIMESTAMP;
  v_repeat CONSTANT INT := 1000;
  rec RECORD;
  v_e1 INT := 1;
  v_e2 INT := 2;
  v_e3 INT := 4;
  v_e4 INT := 8;
  v_any_arr INT[] := ARRAY[v_e1, v_e2, v_e3, v_e4];
BEGIN
  FOR r IN 1..5 LOOP
    v_ts := clock_timestamp();

    FOR i IN 1..v_repeat LOOP
      FOR rec IN (
        SELECT *
        FROM film
        JOIN film_actor USING (film_id)
        JOIN actor USING (actor_id)
        WHERE film_id IN (v_e1, v_e2, v_e3, v_e4)
      ) LOOP
        NULL;
      END LOOP;
    END LOOP;

    RAISE INFO 'Run %, Statement 1: %',
      r, (clock_timestamp() - v_ts);
    v_ts := clock_timestamp();

    FOR i IN 1..v_repeat LOOP
      FOR rec IN (
        SELECT *
        FROM film
        JOIN film_actor USING (film_id)
        JOIN actor USING (actor_id)
        WHERE film_id = ANY(v_any_arr)
      ) LOOP
        NULL;
      END LOOP;
    END LOOP;

    RAISE INFO 'Run %, Statement 2: %',
      r, (clock_timestamp() - v_ts);
  END LOOP;
END$$;
```

결과는 다음과 같습니다:

```
INFO:  Run 1, Statement 1: 00:00:00.112195
INFO:  Run 1, Statement 2: 00:00:00.450461
INFO:  Run 2, Statement 1: 00:00:00.109792
INFO:  Run 2, Statement 2: 00:00:00.446518
INFO:  Run 3, Statement 1: 00:00:00.105413
INFO:  Run 3, Statement 2: 00:00:00.44298
INFO:  Run 4, Statement 1: 00:00:00.108249
INFO:  Run 4, Statement 2: 00:00:00.476527
INFO:  Run 5, Statement 1: 00:00:00.120229
INFO:  Run 5, Statement 2: 00:00:00.448214
```

흥미롭습니다. `IN` 리스트가 배열 바인드 변수보다 *매번* 4배 더 빠릅니다 (이것은 배열/리스트의 크기입니다!) 그러면 8개 값으로 시도해 봅시다. 값들과 수정된 쿼리 1은 다음과 같습니다:

```sql
-- 값들
  v_e1 INT := 1;
  v_e2 INT := 2;
  v_e3 INT := 4;
  v_e4 INT := 8;
  v_e5 INT := 16;
  v_e6 INT := 32;
  v_e7 INT := 64;
  v_e8 INT := 128;
  v_any_arr INT[] := ARRAY[v_e1, v_e2, v_e3, v_e4, v_e5, v_e6, v_e7, v_e8];

-- 수정된 쿼리 1 ...
        WHERE film_id IN (v_e1, v_e2, v_e3, v_e4, v_e5, v_e6, v_e7, v_e8)
-- ...
```

결과는 여전히 인상적입니다:

```
INFO:  Run 1, Statement 1: 00:00:00.182646
INFO:  Run 1, Statement 2: 00:00:00.63624
INFO:  Run 2, Statement 1: 00:00:00.184814
INFO:  Run 2, Statement 2: 00:00:00.685976
INFO:  Run 3, Statement 1: 00:00:00.188108
INFO:  Run 3, Statement 2: 00:00:00.634903
INFO:  Run 4, Statement 1: 00:00:00.184933
INFO:  Run 4, Statement 2: 00:00:00.626616
INFO:  Run 5, Statement 1: 00:00:00.185879
INFO:  Run 5, Statement 2: 00:00:00.636723
```

`IN` 리스트 쿼리는 이제 거의 2배 더 오래 걸리지만 (정확히 2배는 아님), 배열 쿼리는 약 1.5배 더 오래 걸립니다. 배열의 크기가 증가하면 배열이 더 나은 선택이 되는 것처럼 보입니다. 그러면 해봅시다! `IN` 리스트에 32개의 바인드 변수, 또는 각각 32개의 배열 요소로:

```
INFO:  Run 1, Statement 1: 00:00:00.905064
INFO:  Run 1, Statement 2: 00:00:00.752819
INFO:  Run 2, Statement 1: 00:00:00.760475
INFO:  Run 2, Statement 2: 00:00:00.758247
INFO:  Run 3, Statement 1: 00:00:00.777667
INFO:  Run 3, Statement 2: 00:00:00.895875
INFO:  Run 4, Statement 1: 00:00:01.308167
INFO:  Run 4, Statement 2: 00:00:00.789537
INFO:  Run 5, Statement 1: 00:00:00.788606
INFO:  Run 5, Statement 2: 00:00:00.776159
```

둘 다 거의 비슷하게 빠릅니다. 64개 바인드 값!

```
INFO:  Run 1, Statement 1: 00:00:00.915069
INFO:  Run 1, Statement 2: 00:00:01.058966
INFO:  Run 2, Statement 1: 00:00:00.951488
INFO:  Run 2, Statement 2: 00:00:00.906285
INFO:  Run 3, Statement 1: 00:00:00.907489
INFO:  Run 3, Statement 2: 00:00:00.892393
INFO:  Run 4, Statement 1: 00:00:00.900424
INFO:  Run 4, Statement 2: 00:00:00.903447
INFO:  Run 5, Statement 1: 00:00:00.961805
INFO:  Run 5, Statement 2: 00:00:00.951697
```

여전히 거의 같습니다. 좋아요... 인턴! 이리 와봐. 이 쿼리에 128개 바인드 값을 "생성"해야 해.

네, 예상대로입니다. 마침내, 배열이 `IN` 리스트보다 더 나은 성능을 보이기 시작합니다:

```
INFO:  Run 1, Statement 1: 00:00:01.122866
INFO:  Run 1, Statement 2: 00:00:01.083816
INFO:  Run 2, Statement 1: 00:00:01.416469
INFO:  Run 2, Statement 2: 00:00:01.134882
INFO:  Run 3, Statement 1: 00:00:01.122723
INFO:  Run 3, Statement 2: 00:00:01.087755
INFO:  Run 4, Statement 1: 00:00:01.143148
INFO:  Run 4, Statement 2: 00:00:01.124902
INFO:  Run 5, Statement 1: 00:00:01.236722
INFO:  Run 5, Statement 2: 00:00:01.113741
```

## Oracle 사용하기

Oracle도 배열 타입을 가지고 있습니다 (먼저 명목 타입으로 선언해야 하지만, 여기서는 문제가 되지 않습니다).

다음은 몇 가지 벤치마크 결과입니다 (항상 그렇듯이, 실제 벤치마크 결과가 아니라 익명화된 측정 단위입니다. 즉, 이것은 초가 아니라... Larry입니다):

4개 바인드 값

```
Run 1, Statement 1 : 01.911000000
Run 1, Statement 2 : 02.852000000
Run 2, Statement 1 : 01.659000000
Run 2, Statement 2 : 02.680000000
Run 3, Statement 1 : 01.628000000
Run 3, Statement 2 : 02.664000000
Run 4, Statement 1 : 01.629000000
Run 4, Statement 2 : 02.657000000
Run 5, Statement 1 : 01.636000000
Run 5, Statement 2 : 02.688000000
```

128개 바인드 값

```
Run 1, Statement 1 : 04.010000000
Run 1, Statement 2 : 06.275000000
Run 2, Statement 1 : 03.749000000
Run 2, Statement 2 : 05.440000000
Run 3, Statement 1 : 03.985000000
Run 3, Statement 2 : 05.387000000
Run 4, Statement 1 : 03.807000000
Run 4, Statement 2 : 05.688000000
Run 5, Statement 1 : 03.782000000
Run 5, Statement 2 : 05.803000000
```

바인드 값의 크기는 크게 중요하지 않은 것 같습니다. `IN` 리스트에 비해 배열 바인드 변수를 사용하는 데 항상 일정한 오버헤드가 있지만, 이것은 벤치마킹 오류일 수도 있습니다. 예를 들어, 두 쿼리에 `/*+GATHER_PLAN_STATISTICS*/` 힌트를 추가하면, 흥미롭게도 배열을 사용한 쿼리가 상당히 빨라졌지만, `IN` 리스트 쿼리는 영향을 받지 않았습니다... 이상하죠? 설명은 댓글에서 찾을 수 있습니다, 특히 Kim Berg Hansen의 댓글:

> 컬렉션의 기본 카디널리티(cardinality)가 옵티마이저에 의해 8168로 가정되는 것 같습니다 (적어도 제 12.1.0.2 db에서는). 그래서 아마도 해시 조인과 함께 actor의 풀 스캔을 사용할 것입니다.

실제로, `TABLE()` 생성자는 (이 경우) 배열에 훨씬 적은 데이터가 포함되어 있음에도 불구하고 항상 8168의 상수 카디널리티 추정치를 산출하는 것 같습니다. 따라서 작은 배열에 대해 중첩 루프 조인(nested loop join)을 얻으려면 대략적인 카디널리티를 힌트로 제공하는 것이 도움이 될 수 있습니다.

## 결론

이 글은 작은 리스트에서 왜 그렇게 큰 차이가 있는지, 그리고 이점이 꽤 큰 리스트에서만 나타나는지에 대해서는 다루지 않습니다.

하지만 다시 한 번 보여주었습니다. SQL에서 성급하게 최적화하지 말고, *측정하고, 측정하고, 측정*해야 한다는 것을. 동적 SQL 쿼리의 `IN` 리스트는 커서 캐시 / 실행 계획 캐시 포화(saturation)와 많은 "하드 파싱"을 초래할 때 프로덕션에서 큰 문제가 될 수 있습니다. 따라서 배열 사용의 이점은 내용이 클 때 훨씬 더 극적입니다. `IN` 리스트보다 실행 계획을 훨씬 더 자주 재사용할 수 있기 때문입니다.

하지만 `IN` 리스트가 단일 실행에서는 더 빠를 수 있습니다.

어떤 경우든: 인터넷 어딘가에서 찾은 조언을 따를 때는 신중하게 선택하세요. 이 조언을 따를 때도 마찬가지입니다. 저는 PostgreSQL 9.5와 Oracle 11gR2 XE에서 벤치마크를 실행했습니다. 둘 다 최신 데이터베이스 버전이 아닙니다. 여러분의 "개선"이 실제로 진정한 개선인지 확인하기 위해, 여러분 쪽에서 다시 측정해 보세요! 그리고 의심스러우면, 실제로 문제가 있다고 확신할 때까지 최적화하지 마세요.
