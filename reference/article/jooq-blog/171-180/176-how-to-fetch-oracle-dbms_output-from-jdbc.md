# JDBC에서 Oracle DBMS_OUTPUT 가져오는 방법

> 원문: https://blog.jooq.org/how-to-fetch-oracle-dbms_output-from-jdbc/

## 개요

Oracle 저장 프로시저(stored procedure)를 사용할 때, `DBMS_OUTPUT` 명령으로부터 디버그 로그 정보를 얻는 것은 흔한 일입니다. 하지만 JDBC를 통해 저장 프로시저를 호출할 때는 이 출력이 자동으로 가져와지지 않습니다. SQL*Plus에서는 `SET SERVEROUTPUT ON`을 사용하면 되지만, JDBC에서는 다른 접근 방식이 필요합니다.

## 해결 방법

세 가지 단계가 필요합니다:

1. 출력 활성화: `DBMS_OUTPUT.ENABLE()` 실행
2. 프로시저 실행: 저장 프로시저 실행
3. 출력 가져오기 및 비활성화: `DBMS_OUTPUT.GET_LINES()`로 출력을 가져온 후 `DBMS_OUTPUT.DISABLE()` 실행

## 핵심 JDBC 예제

```java
try (Connection c = DriverManager.getConnection(url, properties);
     Statement s = c.createStatement()) {

    try {
        s.executeUpdate("begin dbms_output.enable(); end;");
        s.executeUpdate("begin my_procedure(1, 2); end;");

        try (CallableStatement call = c.prepareCall(
            "declare " +
            "  num integer := 1000;" +
            "begin " +
            "  dbms_output.get_lines(?, num);" +
            "end;"
        )) {
            call.registerOutParameter(1, Types.ARRAY,
                "DBMSOUTPUT_LINESARRAY");
            call.execute();

            Array array = null;
            try {
                array = call.getArray(1);
                Stream.of((Object[]) array.getArray())
                      .forEach(System.out::println);
            }
            finally {
                if (array != null)
                    array.free();
            }
        }
    }
    finally {
        s.executeUpdate("begin dbms_output.disable(); end;");
    }
}
```

### 코드 설명

1. `dbms_output.enable()`을 호출하여 출력 캡처를 활성화합니다.
2. 저장 프로시저를 실행합니다.
3. `CallableStatement`를 사용하여 `dbms_output.get_lines()`를 호출합니다.
4. 출력 파라미터를 `DBMSOUTPUT_LINESARRAY` 타입의 배열로 등록합니다.
5. 반환된 배열을 처리하고 리소스를 해제합니다.
6. finally 블록에서 `dbms_output.disable()`을 호출하여 정리합니다.

## 재사용 가능한 유틸리티 메서드

위 로직을 재사용 가능한 유틸리티 메서드로 감쌀 수 있습니다:

```java
static void logServerOutput(Connection connection,
    WhyUNoCheckedExceptionRunnable runnable) throws Exception {
    // 활성화, runnable 실행, 출력 가져오기, 비활성화
}
```

## jOOQ 통합

jOOQ 3.11 이상에서는 `FetchServerOutputListener`를 통해 네이티브 지원을 제공합니다. 다음과 같이 설정합니다:

```java
DSLContext ctx = DSL.using(c,
    new Settings().withFetchServerOutputSize(10));
ctx.execute("begin my_procedure(1, 2); end;");
```

이렇게 설정하면 프레임워크의 리스너(listener) 인프라를 통해 출력이 자동으로 디버그 로그에 나타납니다. `withFetchServerOutputSize(10)` 설정을 통해 수동 개입 없이 서버 출력을 자동으로 캡처할 수 있습니다.

## 결론

Oracle `DBMS_OUTPUT`을 JDBC에서 가져오는 것은 기본적으로 지원되지 않지만, 위에서 설명한 패턴을 사용하면 쉽게 구현할 수 있습니다. jOOQ를 사용하는 경우에는 `ExecuteListener` SPI를 통해 더욱 간편하게 처리할 수 있습니다.
