# PostgreSQL 11의 SQL 표준 GROUPS 및 EXCLUDE 윈도우 함수 절 지원

> 원문: https://blog.jooq.org/postgresql-11s-support-for-sql-standard-groups-and-exclude-window-function-clauses/

윈도우 함수(window function)는 SQL:2003에서 추가된 매우 강력한 기능입니다. 이 블로그에서 윈도우 함수에 대해 여러 차례 설명한 바 있습니다. 많은 사람들이 `ROWS BETWEEN ... AND ...` 절(프레임 절이라고도 함)에 익숙하며, `RANGE BETWEEN ... AND ...`도 알고 있을 것입니다. 하지만 오직 PostgreSQL 11과 H2 1.4.198만이 이제 `GROUPS`도 지원합니다.

이 세 가지 프레임 유닛 간의 차이점과 새로운 `EXCLUDE` 절에 대해 살펴보겠습니다.

## ROWS

`ROWS`는 구현이 쉽습니다. 프레임 내의 정확한 행 수를 셉니다. 따라서 다음과 같이 작성하면:

```sql
SELECT
  payment_date,
  amount,
  avg(amount) OVER (
    ORDER BY payment_date, payment_id
    ROWS BETWEEN 2 PRECEDING AND 2 FOLLOWING
  )
FROM payment;
```

이 쿼리는 다음과 같은 결과를 생성합니다:

```
payment_date        |amount|avg                   |
--------------------|------|----------------------|
2005-05-24 22:53:30 |  2.99|3.3233333333333333    |
2005-05-24 22:54:33 |  2.99|3.3225000000000000    |
2005-05-24 23:03:39 |  4.99|4.1900000000000000    |
2005-05-24 23:04:41 |  4.99|3.9880000000000000    |
2005-05-24 23:05:21 |  6.99|4.3900000000000000    |
...
```

윈도우 내에 정확히 현재 행 앞의 2개 행, 현재 행, 그리고 현재 행 뒤의 2개 행이 있게 됩니다(데이터 세트의 경계를 제외하면 항상 5개 행). 이렇게 하면 슬라이딩 평균을 계산할 수 있습니다.

## RANGE

`RANGE`는 논리적 윈도잉을 수행합니다. 여기서는 행의 개수를 세는 것이 아니라 값의 오프셋을 찾습니다. 이것은 `DATE` 또는 `TIMESTAMP` 열과 함께 사용하면 특히 유용합니다. 예를 들어:

```sql
SELECT
  payment_id,
  customer_id,
  date_trunc('hour', payment_date) AS hour,
  array_agg(payment_id) OVER (
    ORDER BY date_trunc('hour', payment_date)
    RANGE BETWEEN INTERVAL '2 hours' PRECEDING
              AND INTERVAL '2 hours' FOLLOWING
  )
FROM payment
ORDER BY date_trunc('hour', payment_date), payment_id;
```

이 쿼리는 다음과 같은 결과를 생성합니다:

```
payment_id|customer_id|hour               |array_agg                                     |
----------|-----------|-------------------|----------------------------------------------|
       39 |        53 |2005-05-24 22:00:00|{39,638,1314,2550,3318,4432,4775,...}         |
      638 |       295 |2005-05-24 22:00:00|{39,638,1314,2550,3318,4432,4775,...}         |
     1314 |       400 |2005-05-24 22:00:00|{39,638,1314,2550,3318,4432,4775,...}         |
...
```

이제 결과가 달라집니다. 프레임에 정확히 2개의 행이 선행하는 것이 아니라 결제 시간보다 2시간 이전의 모든 행이 포함됩니다. `2005-05-24 22:00:00`의 경우 `2005-05-24 20:00:00`까지의 모든 결제가 포함되지만, 해당 시간대에 결제가 없었기 때문에 `2005-05-24 22:00:00`부터 시작됩니다.

## GROUPS

이것이 새로운 기능입니다. 모든 동일 값 그룹(tied row groups)을 셉니다. `ROWS`와 `RANGE`의 중간 형태로 생각할 수 있습니다:

```sql
SELECT
  payment_id,
  customer_id,
  date_trunc('hour', payment_date) AS hour,
  array_agg(payment_id) OVER (
    ORDER BY date_trunc('hour', payment_date)
    GROUPS BETWEEN 2 PRECEDING AND 2 FOLLOWING
  )
FROM payment
ORDER BY date_trunc('hour', payment_date), payment_id;
```

`ROWS`와 `RANGE`의 중요한 차이점은 다음과 같습니다:
- ROWS는 정확한 행 개수를 셉니다
- RANGE는 값 오프셋을 기준으로 논리적 윈도잉을 수행합니다
- GROUPS는 동일한 정렬 값을 가진 행들의 그룹을 셉니다

`GROUPS`를 사용하면 동일한 정렬 값을 가진 행들이 하나의 그룹으로 처리됩니다. `GROUPS BETWEEN 2 PRECEDING AND 2 FOLLOWING`은 현재 그룹 앞의 2개 그룹, 현재 그룹, 그리고 현재 그룹 뒤의 2개 그룹을 포함합니다. 이는 같은 시간대의 모든 결제가 동일한 윈도우 집계 값을 갖게 됨을 의미합니다.

## EXCLUDE 절

PostgreSQL 11에서 추가된 또 다른 기능은 `EXCLUDE` 절입니다. 이 절을 사용하면 윈도우 프레임에서 특정 행을 제외할 수 있습니다.

네 가지 제외 옵션이 있습니다:

- EXCLUDE CURRENT ROW: 현재 행만 제외합니다
- EXCLUDE GROUP: 현재 그룹의 모든 동일 값 행을 제외합니다
- EXCLUDE TIES: 현재 행과 동일한 정렬 값을 가진 다른 행들을 제외합니다
- EXCLUDE NO OTHERS: 기본 동작입니다 (모든 행 포함)

다음은 `ROWS`와 함께 `EXCLUDE CURRENT ROW`를 사용하는 예제입니다:

```sql
SELECT
  v,
  array_agg(v) OVER (
    ORDER BY v
    ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
    EXCLUDE CURRENT ROW
  )
FROM (VALUES (1), (2), (2), (3), (4)) t(v);
```

이 쿼리는 현재 행을 제외한 앞뒤 1개 행의 값을 집계합니다.

```sql
SELECT
  v,
  array_agg(v) OVER (
    ORDER BY v
    ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
    EXCLUDE GROUP
  )
FROM (VALUES (1), (2), (2), (3), (4)) t(v);
```

`EXCLUDE GROUP`을 사용하면 현재 행과 동일한 값을 가진 모든 행이 제외됩니다.

```sql
SELECT
  v,
  array_agg(v) OVER (
    ORDER BY v
    ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING
    EXCLUDE TIES
  )
FROM (VALUES (1), (2), (2), (3), (4)) t(v);
```

`EXCLUDE TIES`는 현재 행은 유지하되, 동일한 값을 가진 다른 행들을 제외합니다.

제외로 인해 윈도우가 비어 있게 되면 `NULL`이 반환됩니다.

## jOOQ 지원

jOOQ 3.12에서 이 기능에 대한 지원이 추가될 예정입니다:
- https://github.com/jOOQ/jOOQ/issues/7646
- https://github.com/jOOQ/jOOQ/issues/7647

## 결론

`GROUPS` 프레임 유닛과 `EXCLUDE` 절은 윈도우 함수를 더욱 유연하게 사용할 수 있게 해주는 SQL 표준 기능입니다. `GROUPS`는 특히 동일한 값을 가진 행들을 자연스럽게 그룹화할 때 유용하며, `EXCLUDE` 절은 윈도우 프레임에서 특정 행을 제외해야 할 때 사용할 수 있습니다.

`EXCLUDE` 절의 실제 프로덕션 사용 사례에 대해 알고 계신다면 댓글로 알려주세요!
