# SQL 비호환성: NOT IN과 NULL 값

> 원문: https://blog.jooq.org/sql-incompatibilities-not-in-and-null-values/

*작성자: lukaseder*
*작성일: 2012년 1월 27일 (2022년 4월 5일 수정)*

---

이것은 수많은 SQL 개발자들의 삶에서 수많은 디버깅 시간을 소비하게 만든 문제입니다. NOT IN 술어와 안티 조인(anti-join)에서 NULL 값이 등장할 수 있는 다양한 상황들이 있습니다. 다음은 전형적인 상황입니다:

```sql
with data as (
  select 1 as id from dual union all
  select 2 as id from dual union all
  select 3 as id from dual
)
select * from data
where id not in (1, null)
```

이 쿼리가 무엇을 반환할 것이라고 생각하시나요? `dual`이 Oracle 데이터베이스를 가리키므로, 아마 "빈 결과 집합"이라고 말씀하실 수 있을 것입니다. 그리고 Oracle에서는 맞습니다.

## 데이터베이스별 동작 차이

빈 결과 집합을 반환하는 데이터베이스들:
- DB2
- Derby
- H2
- Ingres
- Oracle
- Postgres
- SQL Server
- SQLite
- Sybase

빈 결과 집합을 반환하지 않는 데이터베이스들:
- HSQLDB
- MySQL
- Sybase ASE

## 왜 이런 차이가 발생하는가?

다음 술어를 보겠습니다:

```sql
id not in (1, null)
```

이것은 다음과 동등하게 볼 수 있습니다:

```sql
id != 1 and id != null
```

위 술어에서 `id != null`을 충족하는 id는 없습니다(null 자체도 충족하지 못합니다). 따라서 빈 결과 집합이 됩니다.

## SQL 1992 표준 분석

SQL 1992 명세서 8.4절에 따르면, 공식적인 정의는 다음과 같습니다:

3) `RVC NOT IN IPV` 표현식은 `NOT ( RVC IN IPV )`와 동등합니다.

4) `RVC IN IPV` 표현식은 `RVC = ANY IPV`와 동등합니다.

이를 통해 논리적 변환 과정을 추적할 수 있습니다:

1. `ID NOT IN (1, NULL)`
2. `NOT (ID IN (1, NULL))` 와 동등
3. `NOT (ID = ANY(1, NULL))` 와 동등
4. `NOT (ID = 1 OR ID = NULL)` 와 동등
5. `NOT (ID = 1) AND NOT (ID = NULL)` 와 동등

이 결과는 항상 UNKNOWN이 됩니다.

## HSQLDB에 대하여

한 가지 흥미로운 점은, HSQLDB 2.0이 이 경우에 표준을 준수하지 않는 것으로 보인다는 것입니다. `NOT()` 내부의 표현식을 먼저 평가한 후 `NOT()`을 적용하는 것과, `NOT()`을 정규화된 불리언 표현식으로 변환한 다음 표현식을 평가하는 것의 결과가 다르게 나옵니다.

## MySQL에 대하여

MySQL은 SQL 표준 준수를 강하게 무시하는 것으로 알려져 있으므로, 이 구문에서도 표준과 다르게 동작하는 것이 놀라운 일은 아닙니다.

## 결론

SQL 개발자들에게 이 모든 것은 한 가지를 의미할 뿐입니다: NOT IN 술어에서 NULL을 사용하지 마세요. 그렇지 않으면 큰 낭패를 봅니다!
