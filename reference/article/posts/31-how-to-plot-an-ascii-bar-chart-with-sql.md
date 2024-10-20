# SQL로 ASCII 막대 차트를 그리는 방법

> 원문: https://blog.jooq.org/how-to-plot-an-ascii-bar-chart-with-sql/

비싼 Tableau 구독이 필요 없습니다. Microsoft Excel도 버리세요. 네이티브 PostgreSQL만으로 데이터를 빠르게 시각화할 수 있습니다!

아시다시피, jOOQ는 jOOQ 결과로부터 [멋진 ASCII 차트를 생성](https://blog.jooq.org/formatting-ascii-charts-with-jooq/)할 수 있습니다. 하지만 그러려면 jOOQ를 사용해야 하고, 여러분은 Java/Kotlin/Scala로 코딩하지 않기 때문에 jOOQ를 사용하지 않을 수도 있습니다 (만약 그렇다면, jOOQ를 사용하고 있겠죠).

그래서 저는 생각했습니다. SQL(구체적으로는 PostgreSQL)로 직접 해보면 어떨까?

Christian Nockemann의 트윗:

> "이것을 역-[@lukaseder](https://twitter.com/lukaseder)라고 부르겠습니다: 'DB는 쓸모없으니, 모든 걸 애플리케이션에서 하라.'" -- 2022년 8월 31일

저는 이 쿼리를 [GitHub 저장소](https://github.com/jOOQ/sql-scripts/blob/main/PostgreSQL/bar-charts.sql)에서 유지 관리하며 계속 발전시키고 있습니다. 한번 살펴보세요.

## 쿼리

```sql
-- 이 예제는 https://www.jooq.org/sakila 를 사용하지만,
-- "source" 테이블을 다른 것으로 교체할 수 있습니다
with

  -- 이 부분은 여러분의 필요에 맞게 수정할 수 있습니다
  --------------------------------------------------------------

  -- 여러분의 데이터 생성 쿼리를 여기에 작성하세요
  source (key, value) as (
    select payment_date::date::timestamp, sum(amount)
    from payment
    where extract(year from payment_date) < 2006
    group by payment_date::date::timestamp
    order by payment_date::date::timestamp
  ),

  -- 일부 설정 항목:
  constants as (
    select

      -- y축의 높이
      15 as height,

      -- x축의 너비, normalise_x가 true인 경우에만 적용, 그 외에는 무시됨
      25 as width,

      -- 막대 문자
      '##' as characters,

      -- 막대 사이의 문자
      ' ' as separator,

      -- y축 레이블의 패딩
      10 as label_pad,

      -- x축 데이터를 정규화할지 여부
      -- - 빈 간격 채우기 (int, bigint, numeric, timestamp,
      --   timestamptz인 경우)
      -- - x축을 "width"에 맞게 스케일링
      true as normalise_x
  ),

  -- 나머지는 수정할 필요가 없습니다
  --------------------------------------

  -- 소스 데이터의 사전 계산된 차원
  source_dimensions (kmin, kmax, kstep, vmin, vmax) as (
    select
      min(key), max(key),
      (max(key) - min(key)) / max(width),
      min(value), max(value)
    from source, constants
  ),

  -- 정규화된 데이터, 키 데이터 타입이 generate_series로 생성될 수 있는 경우
  -- (int, bigint, numeric, timestamp, timestamptz) 빈 간격을 채움
  source_normalised (key, value) as (
    select k, coalesce(sum(source.value), 0)
    from source_dimensions
      cross join constants
      cross join lateral
        generate_series(kmin, kmax, kstep) as t (k)
      left join source
        on source.key >= t.k and source.key < t.k + kstep
    group by k
  ),

  -- 정규화된 버전이 마음에 들지 않으면
  -- source_normalised를 source로 교체하세요
  actual_source (i, key, value) as (
    select row_number() over (order by key), key, value
    from source_normalised, constants
    where normalise_x
    union all
    select row_number() over (order by key), key, value
    from source, constants
    where not normalise_x
  ),

  -- 실제 데이터의 사전 계산된 차원
  actual_dimensions (
    kmin, kmax, kstep, vmin, vmax, width_or_count
  ) as (
    select
      min(key), max(key),
      (max(key) - min(key)) / max(width),
      min(value), max(value),
      case
        when every(normalise_x) then least(max(width), count(*)::int)
        else count(*)::int
      end
    from actual_source, constants
  ),

  -- 추가적인 편의 기능
  dims_and_consts as (
    with
      temp as (
        select *,
        (length(characters) + length(separator))
          * width_or_count as bar_width
      from actual_dimensions, constants
    )
    select *,
      (bar_width - length(kmin::text) - length(kmax::text))
        as x_label_pad
    from temp
  ),

  -- 모든 (x, y) 데이터 포인트에 대한 카르테시안 곱
  x (x) as (
    select generate_series(1, width_or_count) from dims_and_consts
  ),
  y (y) as (
    select generate_series(1, height) from dims_and_consts
  ),

  -- ASCII 차트 렌더링
  chart (rn, chart) as (
    select
      y,
      lpad(y * (vmax - vmin) / height || '', label_pad)
        || ' | '
        || string_agg(
             case
               when height * actual_source.value / (vmax - vmin)
                 >= y then characters
               else repeat(' ', length(characters))
             end, separator
             order by x
           )
    from
      x left join actual_source on actual_source.i = x,
      y, dims_and_consts
    group by y, vmin, vmax, height, label_pad
    union all
    select
      0,
      repeat('-', label_pad)
        || '-+-'
        || repeat('-', bar_width)
    from dims_and_consts
    union all
    select
      -1,
      repeat(' ', label_pad)
        || ' | '
        || case
             when x_label_pad < 1 then ''
             else kmin || repeat(' ', x_label_pad) || kmax
           end
    from dims_and_consts
  )
select chart
from chart
order by rn desc
;
```

## 결과

sakila 데이터베이스에 대해 실행하면, 다음과 같은 멋진 차트를 얻을 수 있습니다:

```
 chart                                                                                   |
-----------------------------------------------------------------------------------------+
 11251.7400 |                                                       ##                   |
 10501.6240 |                                                       ##                   |
 9751.50800 |                                                       ##                   |
 9001.39200 |                                                       ##                   |
 8251.27600 |                                                       ##                   |
 7501.16000 |                                     ##                ##             ## ##  |
 6751.04400 |                                     ##                ##             ## ##  |
 6000.92800 |                                     ##                ##             ## ##  |
 5250.81200 |                   ##                ##             ## ##             ## ##  |
 4500.69600 |                   ##                ##             ## ##             ## ##  |
 3750.58000 |                   ## ##             ## ##          ## ##             ## ##  |
 3000.46400 |                   ## ##             ## ##          ## ##             ## ##  |
 2250.34800 |    ##             ## ##          ## ## ##          ## ## ##          ## ##  |
 1500.23200 | ## ##             ## ##          ## ## ##          ## ## ##          ## ## ##|
 750.116000 | ## ##             ## ##          ## ## ##          ## ## ##          ## ## ##|
 -----------+-----------------------------------------------------------------------------
            | 2005-05-24 00:00:00                                     2005-08-23 00:00:00 |
```

정말 대단하지 않나요!

## 어떻게 작동하나요?

이 쿼리는 3개의 부분으로 구성됩니다:

- `source`: 데이터를 생성하는 실제 쿼리입니다. 이 부분을 여러분의 쿼리로 대체할 수 있습니다.
- `constants`: 차원, 막대 차트 문자 등을 조정할 수 있는 설정 섹션입니다.
- 나머지 부분: 수정할 필요가 없습니다.

위의 `source` 예제는 다음과 같습니다:

```sql
source (key, value) as (
  select payment_date::date::timestamp, sum(amount)
  from payment
  where extract(year from payment_date) < 2006
  group by payment_date::date::timestamp
  order by payment_date::date::timestamp
)
```

이것은 payment 테이블에서 결제 날짜별 모든 수익을 생성합니다. `generate_series`와의 호환성을 위해 날짜를 타임스탬프로 캐스팅합니다.

여러분이 해야 할 일은 키/값 형태의 데이터 세트를 생성하는 것뿐입니다. 이것을 다른 것으로 대체할 수 있습니다. 예를 들어, 누적 수익을 얻으려면:

```sql
source (key, value) as (
  select
    payment_date::date::timestamp,
    sum(sum(amount)) over (order by payment_date::date::timestamp)
  from payment
  where extract(year from payment_date) < 2006
  group by payment_date::date::timestamp
  order by payment_date::date::timestamp
)
```

... 이 경우 (현재로서는) 정규화를 `false`로 되돌려야 합니다 (빈 간격 채우기가 아직 정확하지 않습니다).

또한, 공간을 절약하기 위해 막대를 좀 더 슬림하게 만들었습니다:

```sql
'#' as characters,
'' as separator,
false as normalise_x
```

그러면 매니저에게 보여주고 싶은 수익의 기하급수적 증가를 보여주는 멋진 차트를 얻을 수 있습니다 (실제로는 기하급수적이 아닙니다. 왜냐하면 이제 빈 간격이 반영되지 않기 때문입니다. 하지만 뭐, 어차피 생성된 샘플 데이터일 뿐이니까요):

```
 chart                                                |
------------------------------------------------------+
 66872.4100 |                                        #|
 62414.2493 |                                       ##|
 57956.0886 |                                     ####|
 53497.9280 |                                   ######|
 49039.7673 |                                  #######|
 44581.6066 |                               ##########|
 40123.4460 |                              ###########|
 35665.2853 |                            #############|
 31207.1246 |                          ###############|
 26748.9640 |                       ##################|
 22290.8033 |                     ####################|
 17832.6426 |                   ######################|
 13374.4820 |                #########################|
 8916.32133 |            #############################|
 4458.16066 |        #################################|
 -----------+-----------------------------------------|
            | 2005-05-24 00:00:00  2005-08-23 00:00:00|
```

## 직접 해보세요

[여기에서](https://github.com/jOOQ/sql-scripts/blob/main/PostgreSQL/bar-charts.sql) 직접 사용해 보세요. 개선 사항이 있으면 풀 리퀘스트를 보내주세요.

도전 과제:

- 누적(Stacked) 차트
- 누적 데이터에 대한 빈 간격 채우기
- 기타 기능?

가능성은 무한합니다!
