# 더미 테이블과 SQL 문제

> 원문: https://blog.jooq.org/sql-trouble-with-dummy-tables/

주로 업무 프로젝트에서 Oracle을 사용하다 보니, [DUAL](https://en.wikipedia.org/wiki/DUAL_table) 더미 테이블이라는 개념이 꽤 직관적으로 느껴지게 되었다. 그 테이블의 용도를 알아내려고 이것저것 만져보던 시절은 거의 떠올리지 않게 되었다 (예를 들어, DUAL이 아직 물리적 객체였던 시절에 이 테이블에 데이터를 써넣어서 데이터베이스 전체를 죽여버린다든지...).

다른 많은 RDBMS에서는 더미 테이블이 필요하지 않으며, 다음과 같은 구문을 바로 실행할 수 있다:

```sql
SELECT 1;
SELECT 1 + 1;
SELECT SQRT(2);
```

위와 같은 구문이 일반적으로 가능한 RDBMS는 다음과 같다:

- H2
- MySQL
- Ingres
- Postgres
- SQLite
- SQL Server
- Sybase ASE

반면 다른 RDBMS에서는 Oracle처럼 더미 테이블이 필요하다. 따라서 다음과 같이 작성해야 한다:

```sql
SELECT 1       FROM DUAL;
SELECT 1 + 1   FROM DUAL;
SELECT SQRT(2) FROM DUAL;
```

각 RDBMS와 해당하는 더미 테이블은 다음과 같다:

- DB2: SYSIBM.DUAL
- Derby: SYSIBM.SYSDUMMY1
- H2: 선택적으로 DUAL 지원
- HSQLDB: INFORMATION_SCHEMA.SYSTEM_USERS
- MySQL: 선택적으로 DUAL 지원
- Oracle: DUAL
- Sybase SQL Anywhere: SYS.DUMMY

## 더미 테이블을 피할 때 발생하는 문제

H2나 MySQL에서 더미 테이블을 사용하지 않는 것이 SQL을 더 읽기 쉽게 만든다고 생각하는 사람도 있겠지만, 그렇게 할 경우 문제가 발생할 수 있다는 점을 알아둘 필요가 있다:

다음과 같은 절은 MySQL에서 일부 상황에서 문제를 일으키는 것으로 보인다:

```sql
-- 이것은 문제를 일으킬 수 있다
exists (select 1 where 1 = 0)

-- 이것은 정상 동작한다
exists (select 1 from dual where 1 = 0)
```

이와 유사한 절들이 더 있다.

Ingres에서는 FROM 절 없이 WHERE, GROUP BY, HAVING 절을 사용할 수 없다. 더미 테이블 없이 이를 해결하려면 자체적으로 더미 서브쿼리를 만들어야 한다:

```sql
SELECT 1 WHERE 1 = 1

-- 중첩 SELECT를 사용한 우회 방법
SELECT 1 FROM (SELECT 1) AS DUAL WHERE 1 = 1
```

일반적으로 jOOQ는 이러한 사실들을 클라이언트 코드로부터 숨겨주어, 항상 더미 테이블 없이 간단한 형태를 사용할 수 있게 해준다. 일부 SQL 방언의 지나치게 제한적인 구문 규칙에 대해 걱정할 필요가 없다.
