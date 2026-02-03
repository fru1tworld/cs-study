# SQL 윈도우 함수에서 IGNORE NULLS를 사용하여 빈 값 채우기

> 원문: https://blog.jooq.org/using-ignore-nulls-with-sql-window-functions-to-fill-gaps/

## 서론

최근 트위터에서 매우 흥미로운 SQL 질문을 발견했습니다:

> 안녕하세요 [@sfonplsql](https://twitter.com/sfonplsql?ref_src=twsrc%5Etfw) 저희에게 이런 시나리오가 있습니다. 1월 1일 시장 가치 100, 1월 2일 120, 다음 항목은 1월 25일 125입니다. 1월 3일부터 1월 24일까지의 값은 120이어야 합니다. 어떻게 구현할 수 있을까요? 감사합니다 [@oraclebase](https://twitter.com/oraclebase?ref_src=twsrc%5Etfw)
>
> — Vikki (@vikkiarul) [2019년 4월 23일](https://twitter.com/vikkiarul/status/1120669222672261120?ref_src=twsrc%5Etfw)

질문을 다시 정리하면: 우리에게는 희소한(sparse) 데이터 포인트 집합이 있습니다:

```
+------------+-------+
| VALUE_DATE | VALUE |
+------------+-------+
| 2019-01-01 |   100 |
| 2019-01-02 |   120 |
| 2019-01-05 |   125 |
| 2019-01-06 |   128 |
| 2019-01-10 |   130 |
+------------+-------+
```

날짜는 이산적이고 연속적인 데이터 포인트로 나열될 수 있으므로, 2019-01-02와 2019-01-05 사이 또는 2019-01-06과 2019-01-10 사이의 빈 공간을 채우면 어떨까요? 원하는 출력은 다음과 같습니다:

```
+------------+-------+
| VALUE_DATE | VALUE |
+------------+-------+
| 2019-01-01 |   100 |
| 2019-01-02 |   120 | <-+
| 2019-01-03 |   120 |   | -- 생성됨
| 2019-01-04 |   120 |   | -- 생성됨
| 2019-01-05 |   125 |
| 2019-01-06 |   128 | <-+
| 2019-01-07 |   128 |   | -- 생성됨
| 2019-01-08 |   128 |   | -- 생성됨
| 2019-01-09 |   128 |   | -- 생성됨
| 2019-01-10 |   130 |
+------------+-------+
```

생성된 컬럼에서는 가장 최근 값을 반복하면 됩니다.

## SQL로 이것을 어떻게 할 수 있을까요?

이 예제에서는 Oracle SQL을 사용하겠습니다. 원래 질문자가 Oracle로 이것을 하기를 기대했기 때문입니다. 아이디어는 두 단계로 이것을 수행하는 것입니다:

1. 첫 번째와 마지막 데이터 포인트 사이의 모든 날짜를 생성합니다
2. 각 날짜에 대해 현재 데이터 포인트를 찾거나, 가장 최근 데이터 포인트를 찾습니다

하지만 먼저 데이터를 생성해 봅시다:

```sql
create table t (value_date, value) as
  select date '2019-01-01', 100 from dual union all
  select date '2019-01-02', 120 from dual union all
  select date '2019-01-05', 125 from dual union all
  select date '2019-01-06', 128 from dual union all
  select date '2019-01-10', 130 from dual;
```

### 1. 모든 날짜 생성하기

Oracle에서는 이를 위해 편리한 `CONNECT BY` 구문을 사용할 수 있습니다. `WITH`를 사용한 SQL 표준 재귀나 `PIPELINED` 함수 등 다른 도구를 사용하여 빈 공간을 채울 날짜를 생성할 수도 있지만, 저는 이 목적에는 `CONNECT BY`를 선호합니다. 다음과 같이 작성합니다:

```sql
select (
  select min(t.value_date)
  from t
) + level - 1 as value_date
from dual
connect by level <= (
  select max(t.value_date) - min(t.value_date) + 1
  from t
)
```

이것은 다음을 생성합니다:

```
VALUE_DATE|
----------|
2019-01-01|
2019-01-02|
2019-01-03|
2019-01-04|
2019-01-05|
2019-01-06|
2019-01-07|
2019-01-08|
2019-01-09|
2019-01-10|
```

이제 위의 쿼리를 파생 테이블로 감싸고 실제 데이터 집합과 왼쪽 조인합니다:

```sql
select
  d.value_date,
  t.value
from (
  select (
    select min(t.value_date)
    from t
  ) + level - 1 as value_date
  from dual
  connect by level <= (
    select max(t.value_date) - min(t.value_date) + 1
    from t
  )
) d
left join t
on d.value_date = t.value_date
order by d.value_date;
```

날짜 빈 공간은 이제 채워졌지만, 값 컬럼은 여전히 희소합니다:

```
VALUE_DATE|VALUE|
----------|-----|
2019-01-01|  100|
2019-01-02|  120|
2019-01-03|     |
2019-01-04|     |
2019-01-05|  125|
2019-01-06|  128|
2019-01-07|     |
2019-01-08|     |
2019-01-09|     |
2019-01-10|  130|
```

### 2. 값 빈 공간 채우기

각 행에서 `VALUE` 컬럼은 실제 값을 포함하거나, 모든 null을 무시하면서 현재 행 이전의 "last_value"를 포함해야 합니다. 제가 이 요구사항을 특정 영어 표현을 사용하여 작성한 것에 주목하세요. 이제 그 문장을 직접 SQL로 번역할 수 있습니다:

```sql
last_value (t.value) ignore nulls over (order by d.value_date)
```

윈도우 함수에 `ORDER BY` 절을 추가했으므로, 기본 프레임 `RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW`가 적용됩니다. 이것은 구어적으로 "모든 이전 행"을 의미합니다. (기술적으로 이것은 정확하지 않습니다. 이것은 현재 행의 값보다 작거나 같은 값을 가진 모든 행을 의미합니다 - [Kim Berg Hansen의 댓글](https://blog.jooq.org/using-ignore-nulls-with-sql-window-functions-to-fill-gaps/#comment-316715)을 참조하세요) 편리하네요! 우리는 null을 무시하면서 모든 이전 행의 윈도우에서 마지막 값을 찾으려고 합니다. 이것은 표준 SQL이지만, 안타깝게도 모든 RDBMS가 `IGNORE NULLS`를 지원하지는 않습니다. jOOQ에서 지원하는 것들 중에서 현재 이 구문을 지원하는 것들은 다음과 같습니다:

* DB2
* H2
* Informix
* Oracle
* Redshift
* Sybase SQL Anywhere
* Teradata

때때로 정확한 표준 구문은 지원되지 않지만, 표준 기능은 지원됩니다. 다양한 구문 변형을 보려면 [https://www.jooq.org/translate](https://www.jooq.org/translate)를 사용하세요. 전체 쿼리는 이제 다음과 같습니다:

```sql
select
  d.value_date,
  last_value (t.value) ignore nulls over (order by d.value_date)
from (
  select (
    select min(t.value_date)
    from t
  ) + level - 1 as value_date
  from dual
  connect by level <= (
    select max(t.value_date) - min(t.value_date) + 1
    from t
  )
) d
left join t
on d.value_date = t.value_date
order by d.value_date;
```

... 그리고 원하는 결과를 산출합니다:

```
VALUE_DATE         |VALUE|
-------------------|-----|
2019-01-01 00:00:00|  100|
2019-01-02 00:00:00|  120|
2019-01-03 00:00:00|  120|
2019-01-04 00:00:00|  120|
2019-01-05 00:00:00|  125|
2019-01-06 00:00:00|  128|
2019-01-07 00:00:00|  128|
2019-01-08 00:00:00|  128|
2019-01-09 00:00:00|  128|
2019-01-10 00:00:00|  130|
```

## 다른 RDBMS

이 솔루션은 `CONNECT BY`와 같은 Oracle 특정 기능을 사용했습니다. 다른 RDBMS에서는 데이터를 생성하는 다른 방법을 사용하여 동일한 아이디어를 구현할 수 있습니다. 이 글은 오직 `IGNORE NULLS` 사용에만 초점을 맞추고 있습니다. 관심이 있으시다면, 댓글에 여러분의 RDBMS에 대한 대안 솔루션을 자유롭게 게시해 주세요.
