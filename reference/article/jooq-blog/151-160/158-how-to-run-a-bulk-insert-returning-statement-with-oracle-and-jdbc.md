# Oracle과 JDBC에서 대량 INSERT .. RETURNING 문 실행하기

> 원문: https://blog.jooq.org/how-to-run-a-bulk-insert-returning-statement-with-oracle-and-jdbc/

## 소개

이 글에서는 다양한 데이터베이스에서 RETURNING 절을 포함한 대량 INSERT 문을 실행하는 방법을 설명합니다. 특히 Oracle의 고유한 접근 방식에 중점을 둡니다.

## 데이터베이스별 접근 방식

### DB2와 표준 SQL

DB2는 SQL 표준 `FINAL TABLE` 구문을 지원합니다:

```sql
SELECT * FROM FINAL TABLE (
  INSERT INTO x (j) VALUES ('a'), ('b'), ('c')
);
```

이 구문은 JDBC와 원활하게 작동합니다.

### PostgreSQL과 Firebird

PostgreSQL과 Firebird는 벤더 특화 `INSERT .. RETURNING` 구문을 사용합니다:

```sql
INSERT INTO x (j) VALUES ('a'), ('b'), ('c') RETURNING *;
```

이는 DB2의 접근 방식과 동일하게 작동하는 벤더 특화 확장입니다.

### Oracle의 구현

Oracle은 네이티브 SQL에서 동등한 기능을 제공하지 않으므로 PL/SQL 우회 방법이 필요합니다. 해결책은 다음을 포함합니다:

1. 입력/출력용 보조 배열 타입(array type) 생성
2. 대량 작업을 위한 `FORALL` 루프 사용
3. 결과 수집을 위한 `BULK COLLECT INTO` 구현
4. JDBC에서 `ARRAY` 파라미터와 함께 `CallableStatement` 활용

## Oracle PL/SQL 솔루션

Oracle은 보조 타입을 생성하고 `BULK COLLECT`를 사용해야 합니다:

```plsql
CREATE TYPE t_i AS TABLE OF NUMBER(38);
CREATE TYPE t_j AS TABLE OF VARCHAR2(50);
CREATE TYPE t_k AS TABLE OF DATE;

DECLARE
  in_j t_j := t_j('a', 'b', 'c');
  out_i t_i;
  out_j t_j;
  out_k t_k;
BEGIN
  FORALL i IN 1 .. in_j.COUNT
    INSERT INTO x (j)
    VALUES (in_j(i))
    RETURNING i, j, k
    BULK COLLECT INTO out_i, out_j, out_k;
END;
```

## Java JDBC 구현

다음은 Oracle에서 JDBC를 사용하여 대량 INSERT RETURNING을 실행하는 완전한 코드 예제입니다:

```java
try (Connection con = DriverManager.getConnection(url, props);
    Statement s = con.createStatement();
    CallableStatement c = con.prepareCall(
        "DECLARE "
      + "  v_j t_j := ?; "
      + "BEGIN "
      + "  FORALL j IN 1 .. v_j.COUNT "
      + "    INSERT INTO x (j) VALUES (v_j(j)) "
      + "    RETURNING i, j, k "
      + "    BULK COLLECT INTO ?, ?, ?; "
      + "END;")) {

    try {
        // 테이블과 보조 타입 생성
        s.execute(
            "CREATE TABLE x ("
          + "  i INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
          + "  j VARCHAR2(50),"
          + "  k DATE DEFAULT SYSDATE"
          + ")");
        s.execute("CREATE TYPE t_i AS TABLE OF NUMBER(38)");
        s.execute("CREATE TYPE t_j AS TABLE OF VARCHAR2(50)");
        s.execute("CREATE TYPE t_k AS TABLE OF DATE");

        // 입력 및 출력 배열 바인딩
        c.setArray(1, ((OracleConnection) con).createARRAY(
            "T_J", new String[] { "a", "b", "c" })
        );
        c.registerOutParameter(2, Types.ARRAY, "T_I");
        c.registerOutParameter(3, Types.ARRAY, "T_J");
        c.registerOutParameter(4, Types.ARRAY, "T_K");

        // 실행 및 결과 가져오기
        c.execute();
        Object[] i = (Object[]) c.getArray(2).getArray();
        Object[] j = (Object[]) c.getArray(3).getArray();
        Object[] k = (Object[]) c.getArray(4).getArray();

        System.out.println(Arrays.asList(i));
        System.out.println(Arrays.asList(j));
        System.out.println(Arrays.asList(k));
    }
    finally {
        try {
            s.execute("DROP TYPE t_i");
            s.execute("DROP TYPE t_j");
            s.execute("DROP TYPE t_k");
            s.execute("DROP TABLE x");
        }
        catch (SQLException ignore) {}
    }
}
```

## 코드 설명

1. 타입 생성: 각 컬럼에 대한 Oracle 컬렉션 타입(`TABLE OF`)을 생성합니다.
2. 입력 배열 바인딩: `OracleConnection.createARRAY()`를 사용하여 입력 값 배열을 생성하고 바인딩합니다.
3. 출력 파라미터 등록: `registerOutParameter()`를 사용하여 RETURNING 절의 결과를 받을 출력 파라미터를 등록합니다.
4. 실행: `CallableStatement.execute()`를 호출하여 PL/SQL 블록을 실행합니다.
5. 결과 검색: `getArray()`를 통해 반환된 값을 가져옵니다.

## jOOQ의 향후 지원

jOOQ는 이러한 데이터베이스 간의 차이를 추상화하여 개발자가 다음과 같이 균일한 API를 사용할 수 있도록 할 계획입니다:

```java
.returning(X.I, X.J, X.K).fetch()
```

라이브러리가 자동으로 데이터베이스별 구현을 처리합니다.

## 결론

Oracle의 접근 방식은 다소 장황하지만, 과도한 네트워크 왕복 없이 효율적인 대량 작업을 달성할 수 있습니다. `FORALL`과 `BULK COLLECT`를 사용하면 단일 네트워크 라운드트립으로 여러 행을 삽입하고 생성된 값을 반환받을 수 있습니다.
