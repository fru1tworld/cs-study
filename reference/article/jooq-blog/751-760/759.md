# Oracle의 CONNECT BY 절을 사용한 재귀 쿼리

> 원문: https://blog.jooq.org/recursive-queries-with-oracles-connect-by-clause/

SQL에서 재귀적 또는 계층적 쿼리는 다루기 까다로운 부분이다. 일부 RDBMS는 공통 테이블 표현식(CTE)에서 재귀를 허용하지만, 이러한 쿼리는 가독성이 상당히 떨어지는 경향이 있다. Oracle의 경우는 그렇지 않은데, 재귀 CTE 외에도 전용 CONNECT BY 절을 지원하기 때문이다. 이 절의 일반적인 구문은 다음과 같다:

```
--   SELECT ..
--     FROM ..
--    WHERE ..
 CONNECT BY [NOCYCLE] condition [AND condition, ...]
[START WITH condition]
-- GROUP BY ..
```

반복 쿼리는 jOOQ에서도 매우 간단하게 표현할 수 있다. 다음과 같이 connectBy() 메서드를 사용하면 된다:

```java
// 일부 Oracle 전용 기능은
// OracleFactory에서만 사용 가능하다
OracleFactory create = new OracleFactory(connection);

// 1, 2, 3, 4, 5 요소를 가진 테이블을 가져온다
create.select(create.rownum())
      .connectBy(create.level().lessOrEqual(5))
      .fetch();
```

재귀 쿼리도 간단하게 표현할 수 있다. ID, PARENT_ID, NAME 컬럼을 가진 DIRECTORY 테이블이 있다고 가정해 보자. 모든 디렉터리를 재귀적으로 조회하고 절대 경로를 계산하려면 jOOQ에서 다음과 같은 쿼리를 작성할 수 있다. Oracle의 CONNECT BY, START WITH, PRIOR 키워드와 SYS_CONNECT_BY_PATH 함수의 사용법에 주목하라:

```java
OracleFactory ora = new OracleFactory(connection);

List<?> paths =
ora.select(ora.sysConnectByPath(Directory.NAME, "/").substring(2))
   .from(Directory)
   .connectBy(ora.prior(Directory.ID).equal(Directory.PARENT_ID))
   .startWith(Directory.PARENT_ID.isNull())
   .orderBy(ora.literal(1))
   .fetch(0);
```

jOOQ의 출력은 다음과 같을 수 있다:

```
+------------------------------------------------+
|substring                                       |
+------------------------------------------------+
|C:                                              |
|C:/eclipse                                      |
|C:/eclipse/configuration                        |
|C:/eclipse/dropins                              |
|C:/eclipse/eclipse.exe                          |
+------------------------------------------------+
|...21 record(s) truncated...
```

가까운 미래에 jOOQ는 CONNECT BY 구문과 API를 다른 RDBMS의 공통 테이블 표현식 구문으로 변환할 예정이다. 이를 통해 CTE를 지원하는 모든 데이터베이스에서 Oracle의 CONNECT BY 구문을 사용하여 계층적 쿼리를 표현할 수 있게 된다. 더 자세한 내용은 다음을 참고하라:

- CONNECT BY 절에 대한 jOOQ 매뉴얼 섹션
- CONNECT BY 절에 대한 Oracle 공식 문서
