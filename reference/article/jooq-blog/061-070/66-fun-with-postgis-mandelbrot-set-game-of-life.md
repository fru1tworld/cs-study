# PostGIS로 재미있게: 만델브로 집합, 라이프 게임 등

> 원문: https://blog.jooq.org/fun-with-postgis-mandelbrot-set-game-of-life-and-more/

최근 저는 PostgreSQL의 공간 확장 기능인 PostGIS를 가지고 놀면서 SQL로 시각적인 패턴과 수학적 구조를 생성하는 창의적인 방법들을 탐구해 보았습니다. 이 글에서는 몇 가지 재미있는 프로젝트를 공유하려고 합니다.

## GIS 함수로 jOOQ 로고 만들기

기본적인 GIS 연산을 시연하기 위해, 저는 jOOQ 로고의 네 글자를 기본 도형 연산을 사용하여 구성해 보았습니다. 사용된 주요 함수들은 다음과 같습니다:

- `st_polygonfromtext`: WKT 텍스트로부터 다각형을 생성
- `st_scale`: 도형의 크기를 조정
- `st_translate`: 도형을 새 위치로 이동
- `st_union`: 여러 도형을 결합
- `st_difference`: 한 도형에서 다른 도형을 제거

정사각형을 결합하고 기하학적 집합 연산을 수행함으로써, 데이터베이스 내에서 로고를 시각적으로 재현할 수 있습니다.

```sql
WITH
  sprites AS (
    SELECT
      s AS square,
      st_scale(s, 4, 4) AS square4
    FROM (VALUES
      (st_polygonfromtext('polygon ((0 0, 1 0, 1 1, 0 1, 0 0))'))
    ) t (s)
  ),
  letters AS (
    SELECT
      st_difference(
        st_difference(
          st_difference(
            square4,
            st_translate(st_scale(square, 2, 3), 1, 1)
          ),
          st_translate(st_scale(square, 1, 2), 0, 2)
        ),
        st_translate(st_scale(square, 1, 0.5), 3, 2.5)
      ) AS j,
      st_difference(square4, st_translate(square, 1, 1)) AS o1,
      st_difference(square4, st_translate(square, 2, 2)) AS o2,
      st_union(
        st_difference(
          square4,
          st_translate(st_scale(square, 2, 2), 1, 1)
        ),
        st_translate(st_scale(square, 1, 2.5), 1.5, -1)
      ) as q
    FROM sprites
  )
SELECT st_union(v)
FROM letters,
  LATERAL (VALUES
    (st_translate(j, 0, 5)),
    (st_translate(o1, 5, 5)),
    (o2),
    (st_translate(q, 5, 0))
  ) t (v);
```

## 만델브로 집합 생성

유명한 프랙탈인 만델브로 집합을 계산하기 위해 복소수 연산을 사용하는 재귀 SQL 쿼리를 작성했습니다. 이 구현은 공식 z(n) = z(n-1)² + c를 사용합니다.

핵심 측면은 다음과 같습니다:

- 복소수 계산을 반복하는 재귀 CTE
- 반복을 추적하는 윈도우 함수
- 확대/축소 기능을 갖춘 유명한 프랙탈 형태의 시각화
- 다양한 영역을 탐색할 수 있는 조정 가능한 확대 파라미터

```sql
WITH RECURSIVE
  -- 차원 파라미터: 실수부 범위(r1, r2), 허수부 범위(i1, i2), 스텝(s), 반복 횟수(it), 발산 임계값(p)
  dims (r1, r2, i1, i2, s, it, p) AS (
    VALUES (
      -2::float, 1::float,
      -1.5::float, 1.5::float,
      0.01::float, 100, 256.0::float
    )
  ),
  sprites (s) AS (VALUES
    (st_polygonfromtext('polygon ((0 0, 0 1, 1 1, 1 0, 0 0))'))
  ),
  -- 정수 좌표 그리드 생성
  n1 (r, i) AS (
    SELECT r, i
    FROM dims,
      generate_series((r1 / s)::int, (r2 / s)::int) r,
      generate_series((i1 / s)::int, (i2 / s)::int) i
  ),
  -- 실수 좌표로 변환
  n2 (r, i) AS (
    SELECT r::float * s::float, i::float * s::float
    FROM dims, n1
  ),
  -- 재귀적으로 z(n) = z(n-1)² + c 계산
  l (cr, ci, zr, zi, g, it, p) AS (
    SELECT r, i, 0::float, 0::float, 0, it, p FROM n2, dims
    UNION ALL
    SELECT cr, ci, zr*zr - zi*zi + cr, 2*zr*zi + ci, g + 1, it, p
    FROM l
    WHERE g < it AND zr*zr + zi*zi < p
  ),
  -- 각 점의 최종 반복 결과 선택
  x (cr, ci, zr, zi, g) AS (
    SELECT DISTINCT ON (cr, ci) cr, ci, zr, zi, g
    FROM l
    ORDER BY cr, ci, g DESC
  )
-- 발산한 점들만 시각화 (집합에 포함되지 않는 점들)
SELECT st_union(
    st_translate(sprites.s, round(cr / dims.s), round(ci / dims.s))
  )
FROM x, sprites, dims
WHERE zr*zr + zi*zi > p;
```

## 콘웨이의 라이프 게임

라이프 게임 구현은 윈도우 함수를 사용하여 100×100 그리드에서 이웃을 효율적으로 계산합니다.

구현된 규칙:
- 살아있는 셀이 2-3개의 이웃을 가지면 생존
- 죽은 셀이 정확히 3개의 이웃을 가지면 살아남
- 그 외 모든 셀은 죽거나 죽은 상태 유지

윈도우 명세는 2D 좌표를 1D SQL 행에 매핑합니다. 100×100 그리드에서 셀 (3,7)은 위치 307로 인코딩됩니다. 세 개의 이웃 윈도우(w1, w2, w3)는 각각 이전 행, 현재 행, 다음 행의 이웃을 나타냅니다.

```sql
WITH RECURSIVE
  sprites (s) AS (VALUES (
    st_polygonfromtext('polygon ((0 0, 0 1, 1 1, 1 0, 0 0))')
  )),
  -- 초기 상태: 10% 확률로 살아있는 셀 생성
  m (x, y, b) AS (
    SELECT x, y,
      CASE WHEN random() > 0.9 THEN 1 ELSE 0 END
    FROM generate_series(1, 100) x,
      generate_series(1, 100) y
  ),
  -- 진화 단계
  e (x, y, b, g) AS (
    SELECT x, y, b, 1 FROM m
    UNION ALL
    SELECT e.x, e.y,
      CASE
        -- 살아있는 셀: 이웃이 2-3개면 생존
        WHEN e.b = 1 AND
          sum(e.b) OVER w1 + sum(e.b) OVER w2 + sum(e.b) OVER w3 IN (2, 3)
          THEN 1
        -- 죽은 셀: 이웃이 정확히 3개면 탄생
        WHEN e.b = 0 AND
          sum(e.b) OVER w1 + sum(e.b) OVER w2 + sum(e.b) OVER w3 = 3
          THEN 1
        ELSE 0
      END, e.g + 1
    FROM e
    WHERE e.g < 100
    WINDOW
      o AS (ORDER BY x, y),
      -- 이전 행의 이웃 (위쪽 3칸)
      w1 AS (o ROWS BETWEEN 101 PRECEDING AND 99 PRECEDING),
      -- 현재 행의 이웃 (좌우 각 1칸, 자기 자신 제외)
      w2 AS (o ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING EXCLUDE CURRENT ROW),
      -- 다음 행의 이웃 (아래쪽 3칸)
      w3 AS (o ROWS BETWEEN 99 FOLLOWING AND 101 FOLLOWING)
  )
-- 100세대를 10x10 그리드로 시각화
SELECT st_union(st_translate(
  sprites.s,
  (x + (g - 1) % 10 * 120)::float,
  (y - (g - 1) / 10 * 120)::float
))
FROM e, sprites
WHERE b = 1;
```

## 글라이더 건 패턴

고스퍼 글라이더 건(Gosper Glider Gun)은 라이프 게임에서 가장 유명한 패턴 중 하나입니다. 이 패턴은 주기적으로 "글라이더"라고 불리는 이동하는 패턴을 생성합니다. 고정된 초기 구성을 하드코딩하여 진화 패턴이 어떻게 나타나는지 보여줍니다.

```sql
WITH RECURSIVE
  sprites (s) AS (VALUES (
    st_polygonfromtext('polygon ((0 0, 0 1, 1 1, 1 0, 0 0))')
  )),
  -- 글라이더 건 초기 패턴 정의
  m (x, y, b) AS (
    SELECT x, y,
      CASE WHEN (x, y) IN (
        -- 왼쪽 정사각형 블록
        (2, 6), (2, 7), (3, 6), (3, 7),
        -- 왼쪽 구조물
        (12, 6), (12, 7), (12, 8),
        (13, 5), (13, 9),
        (14, 4), (14, 10),
        (15, 4), (15, 10),
        (16, 7),
        (17, 5), (17, 9),
        (18, 6), (18, 7), (18, 8),
        (19, 7),
        -- 오른쪽 구조물
        (22, 4), (22, 5), (22, 6),
        (23, 4), (23, 5), (23, 6),
        (24, 3), (24, 7),
        (26, 2), (26, 3), (26, 7), (26, 8),
        -- 오른쪽 정사각형 블록
        (36, 4), (36, 5), (37, 4), (37, 5)
      ) THEN 1 ELSE 0 END
    FROM generate_series(1, 100) x,
      generate_series(1, 100) y
  ),
  -- 진화 단계 (라이프 게임과 동일한 로직)
  e (x, y, b, g) AS (
    SELECT x, y, b, 1 FROM m
    UNION ALL
    SELECT e.x, e.y,
      CASE
        WHEN e.b = 1 AND
          sum(e.b) OVER w1 + sum(e.b) OVER w2 + sum(e.b) OVER w3 IN (2, 3)
          THEN 1
        WHEN e.b = 0 AND
          sum(e.b) OVER w1 + sum(e.b) OVER w2 + sum(e.b) OVER w3 = 3
          THEN 1
        ELSE 0
      END, e.g + 1
    FROM e
    WHERE e.g < 100
    WINDOW
      o AS (ORDER BY x, y),
      w1 AS (o ROWS BETWEEN 101 PRECEDING AND 99 PRECEDING),
      w2 AS (o ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING EXCLUDE CURRENT ROW),
      w3 AS (o ROWS BETWEEN 99 FOLLOWING AND 101 FOLLOWING)
  )
SELECT st_union(st_translate(
  sprites.s,
  (x + (g - 1) % 10 * 120)::float,
  (y - (g - 1) / 10 * 120)::float
))
FROM e, sprites
WHERE b = 1;
```

## 기술적 하이라이트

이러한 시각화들은 DBeaver의 WKT(Well-Known Text) 형식에 대한 네이티브 지원을 활용하여 쿼리 결과를 직접 공간 시각화할 수 있게 합니다. 모든 패턴은 재귀 CTE와 윈도우 함수를 사용한 순수 SQL로 생성됩니다.

2D 좌표를 SQL 윈도우 프레임 명세에 창의적으로 매핑하는 것이 핵심 기법입니다. 100×100 그리드에서 셀 (3,7)은 위치 307로 인코딩되며, 이웃은 정밀한 `ROWS BETWEEN` 절을 사용하여 계산됩니다.

이 접근 방식은 SQL의 표현력을 보여주며, 데이터베이스 쿼리가 단순한 데이터 검색을 넘어 복잡한 수학적 시뮬레이션과 시각화까지 수행할 수 있음을 증명합니다.
