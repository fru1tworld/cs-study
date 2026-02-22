# 키셋을 사용한 더 빠른 SQL 페이지네이션, 계속

> 원문: https://blog.jooq.org/faster-sql-pagination-with-keysets-continued/

키셋 페이지네이션(seek 메서드라고도 함)은 매우 큰 결과 집합에서도 상수 시간 페이지네이션을 수행할 수 있는 매우 강력한 기술입니다. 반면 "전통적인" OFFSET 페이지네이션은 큰 페이지 번호에서 필연적으로 느려집니다. 이 기술에 대해서는 이전에 블로그에서 다룬 적이 있습니다. 오늘은 계속해서 기존 인덱스를 활용하여 상수 시간 페이지네이션을 달성하는 방법의 또 다른 측면을 살펴보겠습니다. 이 기술은 예를 들어 소셜 미디어 피드를 구현할 때처럼 큰 결과 집합을 효율적으로 페이징해야 할 때 유용합니다.

## 아이디어

아이디어는 다음과 같습니다. 첫 번째 페이지를 가져올 때, 나중에 해당 페이지로 빠르게 건너뛸 수 있도록 모든(또는 일부) 페이지의 경계도 함께 가져옵니다. 페이지 경계를 캐시한 후에는 특정 페이지로 빠르게 건너뛰기 위해 "seek" 메서드/키셋 페이지네이션을 사용할 수 있습니다.

## 예제

간단한 예제를 사용해 봅시다. 1000개의 레코드가 있는 테이블이 있고, VALUE 열과 ID 열을 기준으로 정렬한다고 가정합니다. VALUE 값은 0에서 99 사이의 무작위 값입니다. 다음은 PostgreSQL 예제입니다:

```sql
-- 테스트 테이블 생성
create table t (
  id int not null primary key,
  value int not null
);

-- 무작위 데이터 삽입
insert into t
select s, floor(random() * 100)
from generate_series(1, 1000) as s;
```

## 일반적인 OFFSET 페이지네이션

일반적인 OFFSET 페이지네이션을 사용하면, 페이지 6(페이지 크기 5, 0부터 시작하는 페이지 번호)을 다음과 같이 가져옵니다:

```sql
select id, value
from t
order by value, id
limit 5
offset 25
```

결과는 다음과 같습니다:

```
id  | value
----|------
533 |   2
732 |   2
771 |   2
775 |   2
822 |   2
```

이 쿼리의 문제점은 데이터베이스가 페이지 번호가 증가함에 따라 점점 더 많은 데이터를 건너뛰어야 한다는 것입니다. 이는 큰 페이지 번호에서 성능 저하를 야기합니다.

## 페이지 경계 계산하기

페이지 경계를 계산하기 위해 윈도우 함수를 사용할 수 있습니다. 아이디어는 각 레코드에 페이지 경계 여부를 나타내는 플래그를 추가하는 것입니다:

```sql
select
  t.id,
  t.value,
  case row_number()
       over(order by t.value, t.id) % 5
    when 0 then 1
    else 0
  end page_boundary
from t
order by t.value, t.id
```

이 쿼리는 각 레코드에 대해 해당 레코드가 페이지 경계인지 여부를 나타내는 `page_boundary` 열을 추가합니다. 5개 레코드마다(페이지 크기가 5이므로) 페이지 경계가 됩니다.

## 페이지 번호와 함께 경계 가져오기

이제 페이지 경계만 필터링하고 페이지 번호를 추가합니다:

```sql
select
  x.value,
  x.id,
  row_number()
    over(order by x.value, x.id) + 1 page_number
from (
  select
    t.id,
    t.value,
    case row_number()
         over(order by t.value, t.id) % 5
      when 0 then 1
      else 0
    end page_boundary
  from t
) x
where x.page_boundary = 1
```

결과는 다음과 같습니다:

```
value | id  | page_number
------|-----|------------
0     | 303 |      2
0     | 639 |      3
1     | 98  |      4
1     | 429 |      5
2     | 533 |      6
...
```

이제 각 페이지의 시작 위치를 나타내는 키셋 값을 갖게 되었습니다. 이 값들을 캐시에 저장합니다.

## 키셋 페이지네이션 쿼리

경계가 캐시되면, 페이지 6으로 점프하는 것은 매우 간단해집니다. 캐시에서 페이지 6의 경계 값 `(2, 533)`을 가져와서 다음과 같이 쿼리합니다:

```sql
select id, value
from t
where (value, id) > (2, 533)
order by value, id
limit 5
```

이 쿼리는 인덱스를 사용하여 정확히 원하는 위치에서 시작할 수 있으므로 상수 시간에 실행됩니다. OFFSET을 사용할 때처럼 레코드를 건너뛰기 위해 스캔할 필요가 없습니다.

## jOOQ에서의 키셋 페이지네이션

jOOQ 3.3부터는 SEEK 절을 사용하여 키셋 페이지네이션을 간편하게 사용할 수 있습니다:

```java
DSL.using(configuration)
   .select(T.ID, T.VALUE)
   .from(T)
   .orderBy(T.VALUE, T.ID)
   .seek(2, 533)
   .limit(5);
```

jOOQ는 자동으로 적절한 WHERE 조건을 생성하여 지정된 키셋 값 이후의 레코드만 가져옵니다.

## 윈도우 함수가 없는 데이터베이스

MySQL과 같이 윈도우 함수가 없는(MySQL 8.0 이전) 데이터베이스에서는 스칼라 서브쿼리를 사용하여 동일한 결과를 얻을 수 있습니다:

```sql
select
  t.id,
  t.value,
  case (
      select count(*)
      from t t2
      where (t2.value, t2.id) <= (t.value, t.id)
    ) % 5
    when 0 then 1
    else 0
  end page_boundary
from t
order by t.value, t.id
```

그러나 이 접근 방식은 각 행에 대해 서브쿼리를 실행해야 하므로 성능이 좋지 않을 수 있습니다. 윈도우 함수를 사용할 수 있는 데이터베이스에서는 윈도우 함수 접근 방식이 훨씬 더 효율적입니다.

## 결론

키셋 페이지네이션은 기존 인덱스를 활용하여 상수 시간 페이지네이션을 달성하는 강력한 기술입니다. 이 기술은 다음과 같은 경우에 특히 유용합니다:

- 페이지네이션하는 동안 기본 데이터가 안정적으로 유지될 때
- 임의의 페이지 번호로 점프하는 것보다 일관성이 더 중요할 때
- 매우 큰 결과 집합을 효율적으로 페이징해야 할 때

페이지 경계를 미리 계산하고 캐시함으로써, 사용자가 특정 페이지로 점프할 때 빠른 응답 시간을 보장할 수 있습니다. 이 기술은 모든 SQL 개발자의 도구 상자에 포함되어야 합니다.
