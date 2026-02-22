# JDBCLint로 JDBC API 오용 감지하기

> 원문: https://blog.jooq.org/detect-jdbc-api-misusage-with-jdbclint/

JDBCLint는 Apache 라이선스로 배포되는 JDBC 프록시 도구로, JDBC API의 부적절한 사용을 감지하도록 설계되었습니다.

## JDBCLint가 하는 일

이 도구는 JDBC 객체 생명주기에 대한 런타임 유효성 검사를 수행합니다. 여기에는 다음이 포함됩니다:

- 중복 ResultSet 종료 감지
- 종료되지 않은 ResultSet 식별 (finalizer에서 플래그 지정)
- ResultSet에서 읽지 않은 컬럼 보고

## 통합 방법

구현은 간단합니다. 개발자는 프록시를 사용하여 커넥션 객체를 래핑하면 됩니다:

```java
Connection connection = DriverManager.getConnection(...);
connection = ConnectionProxy.newInstance(
    connection, new Properties());
```

## 실용적 가치

저자에 따르면, JDBCLint는 코드 검사가 아닌 런타임 모니터링을 통해 "레거시 애플리케이션에서 매우 미묘한 메모리 누수를 찾는 데 개발자를 도움"으로써 FindBugs와 같은 정적 분석 도구를 보완합니다.

## 설정 가능성

개별 검사는 속성(properties)을 통해 비활성화할 수 있어, 팀이 특정 요구 사항에 맞게 유효성 검사 규칙을 사용자 정의할 수 있습니다.

## 추가 정보

JDBCLint는 Andrew Gaul이 Maginatics에서 만든 Java 개발 도구로, 프로그래머가 올바르고 효율적인 JDBC API 코드를 작성하도록 돕습니다. Java 6이 필요하며 추가적인 런타임 의존성이 없습니다.

이 도구는 12가지 다른 JDBC 사용 문제 패턴을 감지합니다:

- Connection 생명주기 문제: 중복 종료, 누락된 종료, 누락된 commit/rollback
- PreparedStatement 오용: 부적절한 종료, 누락된 실행
- ResultSet 문제: 소비되지 않은 행, 부적절한 종료, 읽지 않은 컬럼

### 작동 방식

"JDBC lint는 Connection과 같은 구체적인 구현을 동적 프록시 클래스로 래핑"하여 메서드 호출을 가로채고 실행 전후에 유효성 검사를 수행합니다. 이는 안전 검사를 추가하면서 원래 클래스의 동작을 보존합니다.

### 설정 및 사용

개발자는 Connection 또는 DataSource 객체를 래핑하고 Java 속성을 통해 동작을 구성합니다. 이 도구는 다음을 수행할 수 있습니다:

- 오류를 stderr(기본값) 또는 지정된 로그 파일에 보고
- 감지된 문제에 대해 RuntimeException, SQLException을 던지거나 종료
- 속성 설정을 통해 개별 경고를 선택적으로 비활성화

### 보완 도구

FindBugs는 리소스 종료 실패를 감지하고, Java 7의 try-with-resources는 유사한 실수를 방지하며, JDBI는 어노테이션 기반 SQL 접근 방식을 제공합니다. JDBC Lint는 이러한 도구들을 대체하는 것이 아니라 함께 작동합니다.

라이선스: Apache License 2.0
