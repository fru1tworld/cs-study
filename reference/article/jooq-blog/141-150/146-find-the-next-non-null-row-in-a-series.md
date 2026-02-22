# SQL에서 시계열의 다음 비NULL 행 찾기

> 원문: https://blog.jooq.org/find-the-next-non-null-row-in-a-series-with-sql/

최근 reddit에서 재미있는 SQL 질문을 우연히 발견했습니다. 이 질문은 일부 이벤트가 발생한 시계열 데이터 포인트에 관한 것이었습니다. 각 이벤트에 대해 시작 시간과 종료 시간이 있습니다.

```
timestamp             start    end
-----------------------------------
2018-09-03 07:00:00   1        null
2018-09-03 08:00:00   null     null
2018-09-03 09:00:00   null     null
2018-09-03 10:00:00   null     1
2018-09-03 12:00:00   null     null
2018-09-03 13:00:00   null     null
2018-09-03 14:00:00   1        null
2018-09-03 15:00:00   null     1
```

쿼리의 원하는 출력은 다음과 같은 추가 `count` 컬럼이 되어야 합니다:

```
timestamp             start    end    count
-------------------------------------------
2018-09-03 07:00:00   1        null   4
2018-09-03 08:00:00   null     null   null
2018-09-03 09:00:00   null     null   null
2018-09-03 10:00:00   null     1      null
2018-09-03 12:00:00   null     null   null
2018-09-03 13:00:00   null     null   null
2018-09-03 14:00:00   1        null   2
2018-09-03 15:00:00   null     1      null
```

따라서 규칙은 간단합니다. 이벤트가 시작될 때마다, 이벤트가 다시 종료될 때까지 몇 개의 연속적인 항목이 필요한지 알고 싶습니다. 이것이 어떻게 의미가 있는지 시각적으로 볼 수 있습니다:

```
timestamp             start    end    count
-------------------------------------------
2018-09-03 07:00:00   1        null   4     -- 이 이벤트에 4개의 행
2018-09-03 08:00:00   null     null   null
2018-09-03 09:00:00   null     null   null
2018-09-03 10:00:00   null     1      null

2018-09-03 12:00:00   null     null   null  -- 여기엔 이벤트 없음
2018-09-03 13:00:00   null     null   null

2018-09-03 14:00:00   1        null   2     -- 이 이벤트에 2개의 행
2018-09-03 15:00:00   null     1      null
```

당면한 문제에 대한 몇 가지 관찰과 가정:

- 두 이벤트가 겹치는 일은 절대 없습니다
- 시계열은 단조롭게 진행되지 않습니다. 즉, 대부분의 데이터 포인트가 1시간 간격이더라도 데이터 포인트 사이에 더 크거나 작은 간격이 있을 수 있습니다
- 그러나 시리즈에 동일한 타임스탬프는 두 개 존재하지 않습니다

이 문제를 어떻게 해결할 수 있을까요?

## 먼저 데이터셋 만들기

이 예제에서는 PostgreSQL을 사용할 것이지만, 윈도우 함수를 지원하는 모든 데이터베이스에서 작동합니다. 요즘 대부분의 데이터베이스가 그렇습니다. PostgreSQL에서는 `VALUES()` 절을 사용하여 메모리에 데이터를 쉽게 생성할 수 있습니다. 단순함을 위해 타임스탬프 대신 타임스탬프의 정수 표현을 사용할 것입니다. 4와 6 사이에 동일한 비정상적인 간격을 포함시켰습니다:

```sql
values (1, 1, null),
       (2, null, null),
       (3, null, null),
       (4, null, 1),
       (6, null, null),
       (7, null, null),
       (8, 1, null),
       (9, null, 1)
```

이 문을 실행하면(네, PostgreSQL에서는 독립 실행형 문입니다!), 데이터베이스는 단순히 우리가 보낸 값을 에코백합니다:

```
column1 |column2 |column3 |
--------|--------|--------|
1       |1       |        |
2       |        |        |
3       |        |        |
4       |        |1       |
6       |        |        |
7       |        |        |
8       |1       |        |
9       |        |1       |
```

## 비단조적으로 증가하는 시리즈를 다루는 방법

`column1`이 단조롭게 증가하지 않는다는 사실은 이벤트의 길이를 계산하는 수단으로 사용할 수 없다는/신뢰할 수 없다는 것을 의미합니다. 보장된 단조 증가 정수 집합을 가진 추가 컬럼을 계산해야 합니다. `ROW_NUMBER()` 윈도우 함수가 이에 완벽합니다. 다음 SQL 문을 고려해 보세요:

```sql
with
  d(a, b, c) as (
	values (1, 1, null),
	       (2, null, null),
	       (3, null, null),
	       (4, null, 1),
	       (6, null, null),
	       (7, null, null),
	       (8, 1, null),
	       (9, null, 1)
  ),
  t as (
    select
      row_number() over (order by a) as rn, a, b, c
    from d
  )
select * from t;
```

새로운 `rn` 컬럼은 `a`의 순서를 기반으로 각 행에 대해 계산된 행 번호입니다. 단순함을 위해 다음과 같이 별칭을 지정했습니다:

- `a = timestamp`
- `b = start`
- `c = end`

이 쿼리의 결과는 다음과 같습니다:

```
rn |a |b |c |
---|--|--|--|
1  |1 |1 |  |
2  |2 |  |  |
3  |3 |  |  |
4  |4 |  |1 |
5  |6 |  |  |
6  |7 |  |  |
7  |8 |1 |  |
8  |9 |  |1 |
```

아직 특별한 것은 없습니다.

## 이제 이 rn 컬럼을 사용하여 이벤트의 길이를 어떻게 찾을까요?

시각적으로, 이벤트의 길이가 `RN2 - RN1 + 1` 공식을 사용하여 계산될 수 있다는 아이디어를 빠르게 얻을 수 있습니다:

```
rn |a |b |c |
---|--|--|--|
1  |1 |1 |  | RN1 = 1
2  |2 |  |  |
3  |3 |  |  |
4  |4 |  |1 | RN2 = 4

5  |6 |  |  |
6  |7 |  |  |

7  |8 |1 |  | RN1 = 7
8  |9 |  |1 | RN2 = 8
```

두 개의 이벤트가 있습니다:

- 4 – 1 + 1 = 4
- 8 – 7 + 1 = 2

따라서 우리가 해야 할 일은 RN1에서 이벤트의 각 시작점에 대해 해당하는 RN2를 찾고 산술 연산을 수행하는 것입니다. 구문이 꽤 많지만 어렵지 않으니 설명하는 동안 참아주세요:

```sql
with
  d(a, b, c) as (
	values (1, 1, null),
	       (2, null, null),
	       (3, null, null),
	       (4, null, 1),
	       (6, null, null),
	       (7, null, null),
	       (8, 1, null),
	       (9, null, 1)
  ),
  t as (
    select
      row_number() over (order by a) as rn, a, b, c
    from d
  )

-- 여기가 흥미로운 부분:
select
  a, b, c,
  case
    when b is not null then
      min(case when c is not null then rn end)
        over (order by rn
          rows between 1 following and unbounded following)
      - rn + 1
  end cnt
from t;
```

이 새로운 `cnt` 컬럼을 단계별로 살펴보겠습니다. 먼저 쉬운 부분: CASE 표현식 다음과 같은 case 표현식이 있습니다:

```sql
case
  when b is not null then
    ...
end cnt
```

이것이 하는 일은 `b is not null`인지 확인하고 이것이 참이면 무언가를 계산하는 것입니다. `b = start`임을 기억하세요. 따라서 이벤트가 시작된 행에 계산된 값을 넣습니다. 그것이 요구 사항이었습니다. 새로운 윈도우 함수 그래서 거기서 무엇을 계산할까요?

```sql
min(...) over (...) ...
```

데이터 윈도우에서 최솟값을 찾는 윈도우 함수입니다. 그 최솟값은 RN2, 즉 이벤트가 끝나는 다음 행 번호 값입니다. 그래서 그 값을 얻기 위해 `min()` 함수에 무엇을 넣을까요?

```sql
min(case when c is not null then rn end)
over (...)
...
```

또 다른 case 표현식입니다. `c is not null`일 때 이벤트가 종료되었음을 알 수 있습니다(`c = end`임을 기억하세요). 그리고 이벤트가 종료되면 해당 행의 `rn` 값을 찾고 싶습니다. 따라서 이벤트를 시작한 행 _이후의_ 모든 행에 대한 해당 case 표현식의 최솟값이 됩니다. 시각적으로:

```
rn |a |b |c | case 표현식 | 최소 "다음" 값
---|--|--|--|------------|----------------
1  |1 |1 |  | null       | 4
2  |2 |  |  | null       | null
3  |3 |  |  | null       | null
4  |4 |  |1 | 4          | null

5  |6 |  |  | null       | null
6  |7 |  |  | null       | null

7  |8 |1 |  | null       | 8
8  |9 |  |1 | 8          | null
```

이제 현재 행 _다음에 오는_ 모든 행의 윈도우를 형성하기 위해 `OVER()` 절만 지정하면 됩니다.

```sql
min(case when c is not null then rn end)
  over (order by rn
    rows between 1 following and unbounded following)
...
```

윈도우는 `rn`으로 정렬되며 현재 행 다음 1행에서 시작하고(`1 following`) 무한대에서 끝납니다(`unbounded following`). 이제 남은 것은 산술 연산을 수행하는 것입니다:

```sql
min(case when c is not null then rn end)
  over (order by rn
    rows between 1 following and unbounded following)
- rn + 1
```

이것은 `RN2 - RN1 + 1`을 계산하는 장황한 방법이며, 이벤트를 시작하는 컬럼에서만 그렇게 합니다. 위의 전체 쿼리 결과는 이제 다음과 같습니다:

```
a |b |c |cnt |
--|--|--|----|
1 |1 |  |4   |
2 |  |  |    |
3 |  |  |    |
4 |  |1 |    |
6 |  |  |    |
7 |  |  |    |
8 |1 |  |2   |
9 |  |1 |    |
```

이 블로그에서 윈도우 함수에 대해 더 읽어보세요.

---

## 댓글

### 댓글 1 - Łukasz
2018년 9월 3일 16:58

훌륭한 수수께끼네요. 제 접근 방식은 다음과 같습니다:

데이터:

```sql
CREATE TABLE tab(a,b,c)
AS
values (1, 1, null),
       (2, null, null),
       (3, null, null),
       (4, null, 1),
       (6, null, null),
       (7, null, null),
       (8, 1, null),
       (9, null, 1),
       (10, 1,null),
       (11,null,1),
       (12,1,1);
```

쿼리:

```sql
SELECT a,b,c,
   CASE WHEN b = 1 AND c= 1 THEN 1
        WHEN b =1 THEN COUNT(*) OVER(PARTITION BY s)+1
   END AS cnt
FROM (SELECT *, SUM(COALESCE(b,0)+COALESCE(c,0)) OVER(ORDER BY a) s
      FROM tab) s
```

데모: https://dbfiddle.uk/?rdbms=postgres_11&fiddle=0775e1cac55b860dc452e6e8fb31c059

### 댓글 2 - lukaseder (저자 답변)
2018년 9월 3일 17:43

아주 좋습니다 :-)

### 댓글 3 - Łukasz
2018년 9월 3일 18:07

저는 "갭과 아일랜드" 클래스 문제를 해결할 때 추가 파티셔닝(SUM과 CASE의 조합)의 열렬한 팬입니다. 저에게는 여러 `ROW_NUMBER()/MAX()/MIN()`을 사용한 계산보다 이해하고 유지 관리하기가 더 쉽습니다.

또 다른 예시 "연속 구간에 대한 컬럼 증가": https://stackoverflow.com/a/51954348/5070879

### 댓글 4 - KPNC
2018년 9월 4일 10:08

오타: min(case when c is not null then rn en)
*end

### 댓글 5 - lukaseder (저자 답변)
2018년 9월 4일 15:03

감사합니다, 수정했습니다
