# SQL로 Microsoft Excel 스타일의 피벗 테이블과 총계 만들기

> 원문: https://blog.jooq.org/creating-a-microsoft-excel-style-pivot-table-with-grand-totals-in-sql/

이 글에서는 SQL을 사용하여 총계가 포함된 Excel 피벗 테이블 기능을 구현하는 방법을 설명합니다. 두 가지 핵심 SQL 기능을 활용합니다: 집계 계산을 위한 `GROUPING SETS`(또는 `CUBE`)와 데이터를 읽기 좋은 형식으로 재구성하는 `PIVOT`입니다.

## 문제 정의

자전거 가게의 정규화된 재고 데이터에서 시작하여, 이를 다음 내용을 보여주는 비정규화된 피벗 테이블로 변환하는 것이 목표입니다:
- 종류와 색상별 자전거 수량
- 각 자전거 종류별 소계
- 각 색상별 소계
- 전체 총계

## 해결 방법

### 1단계: CUBE()로 모든 합계 생성하기

`CUBE()` 함수는 가능한 모든 그룹화 조합을 효율적으로 생성합니다. `CUBE()`(그리고 `ROLLUP()`과 `GROUPING SETS()`)는 수동으로 작성한 `UNION ALL` 문의 문법적 설탕(syntax sugar)에 불과하지만, 쿼리 최적화 측면에서 훨씬 뛰어납니다.

```sql
SELECT Name, Colour, COUNT(*) AS Total
FROM Bikes
GROUP BY CUBE (Name, Colour)
ORDER BY Name, Colour
```

이 쿼리는 다양한 집계 수준을 나타내는 NULL 값을 생성하며, 이후 "Total" 레이블로 변환됩니다.

### 2단계: 데이터 피벗하기

`PIVOT` 절은 행을 열로 변환합니다:

```sql
PIVOT (
  SUM(Count) FOR Colour IN (
    Red, Blue, Black, Silver, Yellow, Total
  )
) AS p
```

## 데이터베이스별 구현

### SQL Server 구현

```sql
WITH Bikes(Name, Colour) AS (
  SELECT * FROM (
    VALUES ('Mountain Bikes', 'Black'),
           ('Mountain Bikes', 'Black'),
           ('Mountain Bikes', 'Silver'),
           ('Road Bikes', 'Red'),
           ('Road Bikes', 'Red'),
           ('Road Bikes', 'Red'),
           ('Road Bikes', 'Black'),
           ('Road Bikes', 'Yellow'),
           ('Touring Bikes', 'Blue'),
           ('Touring Bikes', 'Blue'),
           ('Touring Bikes', 'Yellow')
  ) AS Bikes(Name, Colour)
)
SELECT
  Name,
  COALESCE(Red, 0) AS Red,
  COALESCE(Blue, 0) AS Blue,
  COALESCE(Black, 0) AS Black,
  COALESCE(Silver, 0) AS Silver,
  COALESCE(Yellow, 0) AS Yellow,
  COALESCE(Grey, 0) AS Grey,
  COALESCE(Multi, 0) AS Multi,
  COALESCE(Uncoloured, 0) AS Uncoloured,
  Total
FROM (
  SELECT
    COALESCE(Name, 'Total') AS Name,
    COALESCE(Colour, 'Total') AS Colour,
    COUNT(*) AS Count
  FROM Bikes
  GROUP BY CUBE (Name, Colour)
) AS t
PIVOT (
  SUM(Count) FOR Colour IN (
    Red, Blue, Black, Silver, Yellow,
    Grey, Multi, Uncoloured, Total
  )
) AS p
ORDER BY CASE Name WHEN 'Total' THEN 1 ELSE 0 END, Name
```

### Oracle 구현

```sql
WITH Bikes(Name, Colour) AS (
  SELECT 'Mountain Bikes', 'Black'  FROM dual UNION ALL
  SELECT 'Mountain Bikes', 'Black'  FROM dual UNION ALL
  SELECT 'Mountain Bikes', 'Silver' FROM dual UNION ALL
  SELECT 'Road Bikes',     'Red'    FROM dual UNION ALL
  SELECT 'Road Bikes',     'Red'    FROM dual UNION ALL
  SELECT 'Road Bikes',     'Red'    FROM dual UNION ALL
  SELECT 'Road Bikes',     'Black'  FROM dual UNION ALL
  SELECT 'Road Bikes',     'Yellow' FROM dual UNION ALL
  SELECT 'Touring Bikes',  'Blue'   FROM dual UNION ALL
  SELECT 'Touring Bikes',  'Blue'   FROM dual UNION ALL
  SELECT 'Touring Bikes',  'Yellow' FROM dual
)
SELECT
  Name,
  COALESCE(Red, 0) AS Red,
  COALESCE(Blue, 0) AS Blue,
  COALESCE(Black, 0) AS Black,
  COALESCE(Silver, 0) AS Silver,
  COALESCE(Yellow, 0) AS Yellow,
  COALESCE(Grey, 0) AS Grey,
  COALESCE(Multi, 0) AS Multi,
  COALESCE(Uncoloured, 0) AS Uncoloured,
  Total
FROM (
  SELECT
    COALESCE(Name, 'Total') AS Name,
    COALESCE(Colour, 'Total') AS Colour,
    COUNT(*) AS Count
  FROM Bikes
  GROUP BY CUBE (Name, Colour)
) t
PIVOT (
  SUM(Count) FOR Colour IN (
    'Red' AS Red,
    'Blue' AS Blue,
    'Black' AS Black,
    'Silver' AS Silver,
    'Yellow' AS Yellow,
    'Grey' AS Grey,
    'Multi' AS Multi,
    'Uncoloured' AS Uncoloured,
    'Total' AS Total
  )
) p
ORDER BY CASE Name WHEN 'Total' THEN 1 ELSE 0 END, Name
```

### PostgreSQL 구현

PostgreSQL은 네이티브 `PIVOT` 문법을 지원하지 않으므로, `FILTER` 절과 함께 수동 집계를 사용합니다:

```sql
WITH Bikes(Name, Colour) AS (
  SELECT * FROM (
    VALUES ('Mountain Bikes', 'Black'),
           ('Mountain Bikes', 'Black'),
           ('Mountain Bikes', 'Silver'),
           ('Road Bikes', 'Red'),
           ('Road Bikes', 'Red'),
           ('Road Bikes', 'Red'),
           ('Road Bikes', 'Black'),
           ('Road Bikes', 'Yellow'),
           ('Touring Bikes', 'Blue'),
           ('Touring Bikes', 'Blue'),
           ('Touring Bikes', 'Yellow')
  ) AS Bikes(Name, Colour)
)
SELECT
  Name,
  COALESCE(SUM(Count) FILTER (WHERE Colour = 'Red'       ), 0) AS Red,
  COALESCE(SUM(Count) FILTER (WHERE Colour = 'Blue'      ), 0) AS Blue,
  COALESCE(SUM(Count) FILTER (WHERE Colour = 'Black'     ), 0) AS Black,
  COALESCE(SUM(Count) FILTER (WHERE Colour = 'Silver'    ), 0) AS Silver,
  COALESCE(SUM(Count) FILTER (WHERE Colour = 'Yellow'    ), 0) AS Yellow,
  COALESCE(SUM(Count) FILTER (WHERE Colour = 'Grey'      ), 0) AS Grey,
  COALESCE(SUM(Count) FILTER (WHERE Colour = 'Multi'     ), 0) AS Multi,
  COALESCE(SUM(Count) FILTER (WHERE Colour = 'Uncoloured'), 0) AS Uncoloured,
  COALESCE(SUM(Count) FILTER (WHERE Colour = 'Total'     ), 0) AS Total
FROM (
  SELECT
    COALESCE(Name, 'Total') AS Name,
    COALESCE(Colour, 'Total') AS Colour,
    COUNT(*) AS Count
  FROM Bikes
  GROUP BY CUBE (Name, Colour)
) AS t
GROUP BY Name
ORDER BY CASE Name WHEN 'Total' THEN 1 ELSE 0 END, Name
```

### MySQL 구현

MySQL은 `CUBE()`를 지원하지 않으므로 `WITH ROLLUP`을 사용하고, `PIVOT` 대신 `CASE WHEN` 집계를 사용합니다:

```sql
WITH Bikes(Name, Colour) AS (
  SELECT 'Mountain Bikes', 'Black'  UNION ALL
  SELECT 'Mountain Bikes', 'Black'  UNION ALL
  SELECT 'Mountain Bikes', 'Silver' UNION ALL
  SELECT 'Road Bikes',     'Red'    UNION ALL
  SELECT 'Road Bikes',     'Red'    UNION ALL
  SELECT 'Road Bikes',     'Red'    UNION ALL
  SELECT 'Road Bikes',     'Black'  UNION ALL
  SELECT 'Road Bikes',     'Yellow' UNION ALL
  SELECT 'Touring Bikes',  'Blue'   UNION ALL
  SELECT 'Touring Bikes',  'Blue'   UNION ALL
  SELECT 'Touring Bikes',  'Yellow'
)
SELECT
  Name,
  COALESCE(SUM(CASE WHEN Colour = 'Red'        THEN Count END), 0) AS Red,
  COALESCE(SUM(CASE WHEN Colour = 'Blue'       THEN Count END), 0) AS Blue,
  COALESCE(SUM(CASE WHEN Colour = 'Black'      THEN Count END), 0) AS Black,
  COALESCE(SUM(CASE WHEN Colour = 'Silver'     THEN Count END), 0) AS Silver,
  COALESCE(SUM(CASE WHEN Colour = 'Yellow'     THEN Count END), 0) AS Yellow,
  COALESCE(SUM(CASE WHEN Colour = 'Grey'       THEN Count END), 0) AS Grey,
  COALESCE(SUM(CASE WHEN Colour = 'Multi'      THEN Count END), 0) AS Multi,
  COALESCE(SUM(CASE WHEN Colour = 'Uncoloured' THEN Count END), 0) AS Uncoloured,
  COALESCE(SUM(CASE WHEN Name != 'Total' OR Colour != 'Total' THEN Count END), 0) AS Total
FROM (
  SELECT
    COALESCE(Name, 'Total') AS Name,
    COALESCE(Colour, 'Total') AS Colour,
    COUNT(*) AS Count
  FROM Bikes
  GROUP BY Colour, Name WITH ROLLUP
) AS t
GROUP BY Name
ORDER BY CASE Name WHEN 'Total' THEN 1 ELSE 0 END, Name
```

## 핵심 요점

"데이터와 SQL을 다룰 때는 항상 SQL로 우아한 해결책을 찾아보세요."

`CUBE()`와 같은 내장 함수를 사용하면 일반적으로 수동으로 작성한 `UNION ALL` 쿼리보다 더 나은 최적화를 얻을 수 있습니다. 네이티브 SQL 피벗 및 그룹화 기능을 사용하면 애플리케이션 레이어에서 데이터를 변환하는 방식보다 더 우아하고 데이터베이스에 최적화된 솔루션을 만들 수 있습니다.
