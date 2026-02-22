# SQL에서 날짜를 포맷하지 마라. DATE 리터럴을 사용하라!

> 원문: https://blog.jooq.org/dont-format-dates-in-sql-use-the-date-literal/

얼마 전 Stack Overflow에서 [이 질문](http://stackoverflow.com/q/32507055/521799)을 보고 놀랐다.

```sql
TO_DATE ('20150801', 'yyyymmdd') AS DAY_20150801_TOTAL,
TO_DATE ('20150802', 'yyyymmdd') AS DAY_20150802_TOTAL,
TO_DATE ('20150803', 'yyyymmdd') AS DAY_20150803_TOTAL,
TO_DATE ('20150804', 'yyyymmdd') AS DAY_20150804_TOTAL,
TO_DATE ('20150805', 'yyyymmdd') AS DAY_20150805_TOTAL,
TO_DATE ('20150806', 'yyyymmdd') AS DAY_20150806_TOTAL,
TO_DATE ('20150807', 'yyyymmdd') AS DAY_20150807_TOTAL,
TO_DATE ('20150808', 'yyyymmdd') AS DAY_20150808_TOTAL,
TO_DATE ('20150809', 'yyyymmdd') AS DAY_20150809_TOTAL,
TO_DATE ('20150810', 'yyyymmdd') AS DAY_20150810_TOTAL,
TO_DATE ('20150811', 'yyyymmdd') AS DAY_20150811_TOTAL,
...
```

이것은 단지 예시 데이터였지만, 많은 사람들이 실제로 이렇게 날짜를 사용하는 것을 본다. 비록 `TO_DATE()`와 `TO_TIMESTAMP()` 함수 자체는 나쁘지 않지만 (입력 데이터를 파싱할 때는), 상수에 대해 이것들을 사용하는 것은 분명 필요하지 않다.

## SQL 표준 DATE 리터럴 사용하기

`DATE`와 `TIMESTAMP`를 위한 SQL 표준 리터럴이 있다는 것을 아는가? 다음과 같이 사용할 수 있다:

```sql
SELECT
  DATE '2015-08-01' AS d,
  TIMESTAMP '2015-08-01 15:30:00' AS ts
FROM DUAL;
```

이것은 대부분의 데이터베이스에서 작동한다:

- DB2
- Firebird
- HSQLDB
- Ingres
- MySQL
- Oracle
- PostgreSQL
- SQLite
- 그 외 다수

Microsoft SQL Server와 Sybase에서도 다음과 같은 대체 구문을 사용할 수 있다:

```sql
SELECT
  CAST('2015-08-01' AS DATE) AS d,
  CAST('2015-08-01 15:30:00' AS DATETIME) AS ts
```

## 장점

1. 성능: 상수는 SQL 파서에 의해서만 파싱되며, 실행 엔진에 의해 파싱되지 않는다. 이것은 `TO_DATE()` 함수 호출과 비교했을 때 더 효율적이다.

2. 가독성: ISO 8601 표준 형식(`YYYY-MM-DD`)을 사용하므로 누구나 쉽게 읽을 수 있다.

3. 간결함: 포맷 문자열을 지정할 필요가 없다.

4. 이식성: 표준 SQL이므로 대부분의 데이터베이스에서 작동한다.

## 결론

작은 개선이 모여 큰 차이를 만든다. 이것은 분명 작은 개선이지만, 코드 품질과 성능에 기여한다. 날짜 상수가 필요할 때마다 `TO_DATE()`나 `TO_TIMESTAMP()` 함수를 사용하는 대신, SQL 표준 `DATE` 및 `TIMESTAMP` 리터럴을 사용하라!
