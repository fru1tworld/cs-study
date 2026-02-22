> 원문: https://blog.jooq.org/prevent-sql-injection-with-sql-builders-like-jooq/

# jOOQ와 같은 SQL 빌더로 SQL 인젝션 방지하기

## 개요

SQL 인젝션(SQL Injection)은 여전히 가장 심각한 보안 취약점 중 하나입니다. 이 글에서는 jOOQ와 같은 SQL 빌더를 사용하여 SQL 인젝션을 효과적으로 방지하는 방법을 살펴봅니다.

## SQL 인젝션의 실제 위협

많은 사람들이 SQL 인젝션의 위험성을 데이터베이스 파괴 정도로만 생각하지만, 실제로 더 심각한 위협은 데이터 유출(Data Leakage)입니다. sqlmap과 같은 도구를 사용하면 공격자가 데이터베이스에서 신용카드 정보나 기타 민감한 데이터를 다운로드할 수 있습니다.

## 해결책 #1: SQL 빌더 사용 (jOOQ)

문자열 연결(String Concatenation)을 사용하는 대신, 쿼리 DSL을 사용해야 합니다.

취약한 패턴 (PreparedStatement 사용):
```java
try (PreparedStatement s = c.prepareStatement(
    "SELECT first_name, last_name FROM users WHERE user_id = ?")) {
    s.setInt(1, userId);
```

PreparedStatement도 올바르게 사용하면 안전하지만, 개발자가 실수로 문자열 연결을 사용할 여지가 있습니다.

타입 안전한 접근 방식 (jOOQ 사용):
```java
Result<?> result =
ctx.select(USERS.FIRST_NAME, USERS.LAST_NAME)
   .from(USERS)
   .where(USERS.USER_ID.eq(userId))
   .fetch();
```

이 접근 방식은 Java 컴파일러의 타입 검사를 통해 우발적인 인젝션 취약점을 제거합니다. SQL 문자열을 직접 다루지 않기 때문에 인젝션이 발생할 여지가 원천적으로 차단됩니다.

## 해결책 #2: 뷰(View)와 저장 프로시저(Stored Procedure)

SQL을 데이터베이스로 이동시키면 추가적인 보안 계층을 제공받을 수 있습니다:

- 타입 검사: 데이터베이스 수준에서의 타입 검증
- 접근 제어: 권한(Grant)을 통한 세밀한 접근 관리
- 내장 SQL 제거: 애플리케이션 코드에서 SQL 문자열 자체가 없어짐

## JPQL 인젝션 주의

JPA 애플리케이션에서도 동일한 위험이 존재합니다. 개발자가 JPQL 쿼리를 문자열 연결로 구성하면 JPQL 인젝션이 발생할 수 있습니다. SQL 인젝션 방지 원칙은 JPQL에도 동일하게 적용됩니다.

## Plain SQL API 사용 시 주의사항

jOOQ는 고급 사용 사례를 위한 "Plain SQL" API를 제공합니다. 하지만 이것은 예외적인 경우에만 사용해야 하며, 기본 방식이 되어서는 안 됩니다. Plain SQL을 사용하면 타입 안전성의 이점을 잃게 됩니다.

## 핵심 원칙

근본적인 방어 전략은 문자열 기반의 내장 SQL을 피하는 것입니다. 이를 위한 두 가지 방법이 있습니다:

1. 내부 DSL 사용: jOOQ와 같이 외부 언어(SQL)를 모델링하는 내부 DSL을 사용
2. 외부 DSL을 외부에 유지: 뷰, 저장 프로시저 등 데이터베이스 객체를 통해 SQL을 데이터베이스 계층에 보관

문자열을 완전히 피하는 것이 SQL 인젝션에 대한 가장 확실한 방어입니다.
