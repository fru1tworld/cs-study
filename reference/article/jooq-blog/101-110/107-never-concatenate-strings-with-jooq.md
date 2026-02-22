# jOOQ에서 절대 문자열을 연결하지 마라

> 원문: https://blog.jooq.org/never-concatenate-strings-with-jooq/

*2020년 3월 4일, lukaseder 작성*

jOOQ는 광범위한 SQL 구문을 네이티브로 지원합니다. 대부분의 개발자들은 jOOQ로 동적 SQL을 작성할 때 문자열 연결에 의존할 필요가 없습니다. 이는 전통적인 JDBC 접근 방식과는 다릅니다. 하지만, 벤더 특화 기능 중 일부는 jOOQ에서 직접 지원되지 않는 경우가 있습니다. 그런 경우를 위해, jOOQ는 다양한 API 요소를 구성할 수 있는 "평문 SQL(plain SQL)" API를 제공합니다.

## 평문 SQL 사용 예제

프레임워크는 여러 컨텍스트에서 평문 SQL 구성을 허용합니다:

```java
import static org.jooq.impl.DSL.*;

// 컬럼 표현식
Field<String> f = field("cool_function(1, 2, 3)", String.class);

// 조건절
Condition c = condition("col1 <fancy operator> col2");

// 테이블
Table<?> t = table("wicked_table_valued_function(x, y)");
```

## 문제가 되는 접근 방식

인자를 동적으로 전달할 때, 개발자들은 타입 안전한 대안이 있음에도 불구하고 문자열을 연결하려 할 수 있습니다:

```java
field("cool_function(1, " + MY_TABLE.MY_COLUMN + ", 3)");
```

이것은 두 가지 중요한 이유로 피해야 합니다:

1. 보안 위험: jOOQ는 일반적으로 SQL 인젝션을 방지하지만, 문자열 연결은 취약점을 다시 도입합니다. 사용자 입력의 연결은 특히 위험합니다. jOOQ는 Checker Framework나 Google ErrorProne을 통한 PlainSQL 검사기로 보호 기능을 제공합니다.

2. SQL 구문 오류: 문자열 연결은 방언(dialect) 특화 컨텍스트가 없습니다. `MY_TABLE.MY_COLUMN.toString()`을 호출하면 `SQLDialect` 설정과 기타 관련 플래그가 누락되어, 일반적이고 잠재적으로 올바르지 않은 SQL이 생성됩니다.

## 올바른 해결책: 평문 SQL 템플릿

jOOQ의 평문 SQL 템플릿은 `{0}, {1}, {2}`와 같은 플레이스홀더 구문을 사용합니다:

```java
field("cool_function(1, {0}, 3)", MY_TABLE.MY_COLUMN);
```

반복적으로 사용할 경우, 커스텀 메서드 내에 캡슐화하세요:

```java
public static Field<String> coolFunction(Field<?> field) {
    return field("cool_function(1, {0}, 3)", field);
}
```

이를 간단히 호출합니다:

```java
coolFunction(MY_TABLE.MY_COLUMN)
```

## 핵심 원칙

jOOQ를 사용하면 문자열 연결이 불필요해집니다. 대신 다음을 사용하세요:
- 타입 안전한 DSL API
- 평문 SQL 템플릿 API (이상적으로는 커스텀 타입 안전 메서드로 래핑)
