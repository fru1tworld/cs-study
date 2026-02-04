# MySQL 5.7에서 윈도우 함수 에뮬레이션하기

> 원문: https://blog.jooq.org/emulating-window-functions-in-mysql-5-7/

## 소개

MySQL 8은 윈도우 함수 지원을 통해 상당한 개선을 도입했다. "윈도우 함수 이전의 SQL과 윈도우 함수 이후의 SQL이 있다"라고 할 수 있다. MySQL 5.7에 머물러 있는 사용자들은 우회 기법을 활용할 수 있지만, 이러한 기법에는 한계가 있다.

## 로컬 변수 사용하기

로컬 변수를 쿼리 내에서 선언하고 증가시켜 윈도우 함수를 시뮬레이션할 수 있다. 기본적인 예제는 행 번호 매기기를 보여준다:

```sql
SELECT
  a,
  @rn := @rn + 1
FROM
  (
    SELECT 3 AS a UNION ALL
    SELECT 4 AS a
  ) AS t,
  (SELECT @rn := 0) r
ORDER BY a;
```

이렇게 하면 순차적인 번호가 매겨진다: 값이 3인 행은 1을 받고, 값이 4인 행은 2를 받는다.

### 쿼리 최적화에 대한 주의사항

이 접근법은 옵티마이저가 특정 순서로 연산을 실행하는 것에 의존한다. `DISTINCT`를 추가하면 행별 정렬 가정이 깨지기 때문에 에뮬레이션이 망가진다:

```sql
SELECT DISTINCT
  a, @rn := @rn + 1
FROM (
  SELECT 3 AS a UNION ALL
  SELECT 4 AS a
) AS t, (SELECT @rn := 0) r
ORDER BY a DESC;
```

결과가 신뢰할 수 없게 된다. MySQL 8 이상에서는 이 패턴에 대해 사용 중단 경고를 발행한다.

## ORDER BY를 사용한 PARTITION BY 에뮬레이션

파티셔닝은 파티션 값이 변경되는 시점을 확인하여 에뮬레이션할 수 있다:

```sql
SELECT
  a, b,
  ROW_NUMBER() OVER (PARTITION BY a ORDER BY b DESC) AS rn1,
  IF (
    @prev = a,
    @rn := @rn + 1,
    CASE WHEN (@prev := a) IS NOT NULL OR TRUE THEN @rn := 1 END
  ) AS rn2
FROM (
  SELECT 1 AS a, 3 AS b UNION ALL
  SELECT 2 AS a, 4 AS b UNION ALL
  SELECT 1 AS a, 5 AS b UNION ALL
  SELECT 2 AS a, 6 AS b
) AS t, (SELECT @rn := 0, @prev := NULL) r
ORDER BY a, b DESC;
```

원하는 `PARTITION BY`와 `ORDER BY` 절 모두 최상위 쿼리에 나타나야 한다.

## JSON을 사용한 PARTITION BY 에뮬레이션

더 견고한 접근법은 JSON 객체를 사용하여 파티션별 행 번호를 추적하는 것이다:

```sql
SELECT
  a, b,
  ROW_NUMBER() OVER (PARTITION BY a ORDER BY b DESC) AS rn1,
  json_extract(
    @rn := json_set(
      @rn, @path := concat('$."', a, '"'),
      (coalesce(json_extract(@rn, @path), 0) + 1)
    ),
    @path
  ) AS rn2,
  @rn AS debug
FROM (
  SELECT 1 AS a, 3 AS b UNION ALL
  SELECT 2 AS a, 4 AS b UNION ALL
  SELECT 1 AS a, 5 AS b UNION ALL
  SELECT 2 AS a, 6 AS b
) AS t, (SELECT @rn := '{}') r
ORDER BY b DESC;
```

이 메커니즘의 작동 방식은 다음과 같다:
- 빈 JSON 객체로 시작한다
- 현재 파티션의 행 번호를 추출한다 (처음에는 null)
- 새 값을 증가시키고 저장한다
- 결과를 생성하기 위해 다시 추출한다

## PARTITION BY를 사용한 DENSE_RANK 에뮬레이션

`DENSE_RANK`를 구현하려면 이전 행 번호와 이전 정렬 값을 모두 추적해야 한다:

```sql
SELECT
  a, b,
  DENSE_RANK() OVER (PARTITION BY a ORDER BY b DESC) AS rn1,
  json_extract(
    @rn := json_set(@rn,
      @rnpath := concat('$."rn-', a, '"'),
      (coalesce(json_extract(@rn, @rnpath), 0) + IF (
        json_extract(@rn, @prepath := concat('$."pre-v-', a, '"')) = b,
        0, 1
      )),
      @prepath,
      b
    ),
    @rnpath
  ) AS rn2,
  @rn AS debug
FROM (
  SELECT 1 AS a, 3 AS b UNION ALL
  SELECT 1 AS a, 5 AS b UNION ALL
  SELECT 1 AS a, 5 AS b UNION ALL
  SELECT 2 AS a, 6 AS b
) AS t, (SELECT @rn := '{}') r
ORDER BY b DESC;
```

이 접근법은 파티션별로 이전 값을 저장하고, 값이 다를 때만 순위를 증가시킨다.

## PARTITION BY를 사용한 RANK 에뮬레이션

`RANK`는 정렬 값과 함께 이전 순위 값도 기억해야 하므로 더 복잡하다:

```sql
SELECT
  a, b,
  RANK() OVER (PARTITION BY a ORDER BY b DESC) AS rn1,
  coalesce(
    json_extract(
      @rn := json_set(@rn,
        @rnpath := concat('$."rn-', a, '"'),
        @currn := coalesce(json_extract(@rn, @rnpath), 0) + 1,
        @prevpath := concat('$."pre-v-', a, '"'),
        b,
        @prernpath := concat('$."pre-rn-', a, '"'),
        IF (json_extract(@rn, @prevpath) = b,
          coalesce(json_extract(@rn, @prernpath), @currn) div 1,
          @currn
        )
      ),
      @prernpath
    ),
    @currn
  ) AS rn2,
  @rn AS debug
FROM (
  SELECT 1 AS a, 3 AS b UNION ALL
  SELECT 1 AS a, 5 AS b UNION ALL
  SELECT 1 AS a, 5 AS b UNION ALL
  SELECT 2 AS a, 6 AS b
) AS t, (SELECT @rn := '{}') r
ORDER BY b DESC;
```

이것은 파티션별로 세 가지 변수를 유지한다: 행 번호, 이전 정렬 값, 그리고 이전 순위.

## PERCENT_RANK와 CUME_DIST

이들은 로컬 변수 접근법으로 쉽게 에뮬레이션할 수 없다. 이 기법이 행 단위 방식으로 제공할 수 없는 파티션 전체 카운트가 필요하기 때문이다.

## LEAD, LAG 및 관련 함수

이들은 JSON 배열이나 이전 값을 유지하여 부분적으로 에뮬레이션할 수 있다:
- `LAG`는 파티션별로 이전 값을 기억해야 한다
- `LEAD`는 `ORDER BY` 절을 반전시켜 구현할 수 있다
- `FIRST_VALUE`는 첫 번째 파티션 값을 저장하고 재생산한다
- `LAST_VALUE`는 정렬을 반전시킨 역방향이다
- `NTH_VALUE`는 대상 행을 식별하기 위한 카운터가 필요하다
- `IGNORE NULLS`는 추적에서 null 값을 건너뛰어야 한다

복잡한 `RANGE`나 `ROWS` 절은 이를 상당히 더 어렵게 만든다.

## 집계 함수

`ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW`를 사용하는 역방향 집계 윈도우 함수는 에뮬레이션할 수 있다:

```sql
SELECT
  a, b,
  SUM(b) OVER w AS sum1,
  json_extract(
    @w := json_set(@w,
      @spath := concat('$."s-', a, '"'),
      (coalesce(json_extract(@w, @spath), 0) + b),
      @cpath := concat('$."c-', a, '"'),
      (coalesce(json_extract(@w, @cpath), 0) + 1)
    ),
    @spath
  ) AS sum2,
  COUNT(*) OVER w AS cnt1,
  json_extract(@w, @cpath) AS cnt2,
  AVG(b) OVER w AS avg1,
  json_extract(@w, @spath) / json_extract(@w, @cpath) AS avg2,
  @w AS debug
FROM (
  SELECT 1 AS a, 3 AS b UNION ALL
  SELECT 1 AS a, 5 AS b UNION ALL
  SELECT 1 AS a, 5 AS b UNION ALL
  SELECT 2 AS a, 6 AS b
) AS t, (SELECT @w := '{}') r
WINDOW w AS (
  PARTITION BY a
  ORDER BY b DESC
  ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
)
ORDER BY b DESC;
```

이것은 파티션별로 누적 합계와 카운트를 저장한다.

## 주요 주의사항

에뮬레이션 기법에는 상당한 제한이 있다:
- `DISTINCT`를 사용할 수 없다
- 윈도우 사양과 다른 임의의 `ORDER BY` 절을 사용할 수 없다
- 서로 다른 사양을 가진 여러 윈도우 함수를 사용할 수 없다
- 전방 참조 윈도우 프레임을 사용할 수 없다
- 여러 `PARTITION BY` 표현식은 복잡한 키 처리가 필요하다
- JSON 경로 표현식은 적절한 따옴표 처리가 필요하다
- JSON에서 데이터 타입 정보가 손실될 수 있다

## 결론

이러한 접근법들은 쿼리 구조에 대한 "무거운 가정" 하에서만 작동한다는 점을 강조한다. 절박한 상황에 있는 MySQL 5.7 사용자에게 유용할 수 있지만, 권장 사항은 명확하다: 적절한 윈도우 함수 지원을 사용하려면 MySQL 8로 업그레이드하라.
