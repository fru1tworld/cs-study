# jOOQ API에서 "String"이란 무엇인가?

> 원문: https://blog.jooq.org/whats-a-string-in-the-jooq-api/

2020년 4월 3일, lukaseder

jOOQ의 가장 큰 강점 중 하나는 타입 안전한 SQL API라는 점입니다. "타입 안전"이란 jOOQ 쿼리의 모든 객체가 `Condition`, `Field`, `Table`과 같이 잘 정의된 타입을 가진다는 것을 의미합니다. 이러한 타입들은 타입 안전한 방식으로 사용할 수 있습니다:

```java
ctx.select(T.A)      // Field
   .from(T)          // Table
   .where(T.B.eq(1)) // Condition
   .fetch();
```

그러나 타입 안전성을 우회하여 Plain SQL 템플릿으로 jOOQ를 확장하고 싶은 경우가 있습니다. 이런 경우에는 jOOQ API에 String 객체를 전달하게 됩니다. 하지만 모든 String 객체가 같은 것은 아닙니다. jOOQ API에는 4가지 주요 String 타입이 있습니다.

## 타입 1: 바인드 값

가장 명확한 String 타입은 바인드 값 또는 리터럴입니다. 다음과 같이 명시적으로 생성할 수 있습니다:

```java
import static org.jooq.impl.DSL.*;

Field<String> bind = val("abc");
Field<String> literal = inline("xyz");
```

기본적으로 첫 번째는 생성된 SQL에서 바인드 파라미터 마커 "?"를 생성하고, 두 번째는 이스케이프된 문자열 리터럴 'xyz'를 생성합니다. jOOQ API가 T 타입을 기대하는 곳에 String 값을 전달하면, 암묵적으로 `DSL.val(T)`를 사용하여 래핑됩니다:

```java
ctx.select(T.A)
   .from(T)
   .where(T.C.eq("xyz")) // 암묵적 바인드 값
   .fetch();
```

String 값이 `Field<String>`으로 래핑되기 때문에 이것은 여전히 타입 안전한 사용입니다.

## 타입 2: Plain SQL 템플릿

jOOQ에 특정 벤더 전용 기능이 없을 때는 Plain SQL 템플릿을 사용합니다. Plain SQL 템플릿은 명시적으로 생성할 수 있습니다:

```java
Field<Integer> field = field("(1 + 2)", SQLDataType.INTEGER);
Table<?> table       = table("generate_series(1, 10)");
Condition condition  = condition("some_function() = 1");
```

이러한 표현식은 다른 표현식과 마찬가지로 쿼리에 포함될 수 있습니다:

```java
ctx.select(field)
   .from(table)
   .where(condition)
   .fetch();
```

일부 쿼리 메서드에는 편의 오버로드가 있습니다:

```java
ctx.select(field("(1 + 2)", SQLDataType.INTEGER)) // SELECT에는 없음
   .from("generate_series(1, 10)")
   .where("some_function() = 1")
   .fetch();
```

참고: jOOQ 3.13 기준으로 `select()` 메서드에는 이 편의 API가 없습니다.

중요한 주의사항: 이러한 API를 사용하면 문자열로 SQL을 구성할 때 JDBC나 JPQL에서 발생하는 SQL 인젝션 위험에 노출됩니다. Plain SQL 템플릿을 절대로 연결하거나 이러한 문자열에 사용자 입력을 사용하지 마십시오. 대신 템플릿 언어를 사용하여 사용자 입력을 바인드 변수로 변환하십시오:

```java
ctx.select(...)
   .from(...)
   .where("some_function() = ?", 1) // 바인드 변수
   .fetch();

ctx.select(...)
   .from(...)
   .where("some_function() = {0}", val(1)) // 템플릿
   .fetch();
```

jOOQ의 대부분의 쿼리 API에서 String 타입은 Plain SQL 템플릿을 위한 것입니다. 이 모든 API는 문서화 목적과 기본적으로 이러한 API 사용을 비활성화할 수 있는 정적 검사기로 전처리하기 위해 `@org.jooq.PlainSQL`로 주석이 달려 있어 보안을 강화할 수 있습니다.

## 타입 3: 이름(식별자)

일부 쿼리 API에서 String은 Plain SQL 템플릿을 위한 편의가 아니라 이름과 식별자를 위한 것입니다. 모든 DDL 문은 이 방식으로 문자열을 사용합니다. 정규화된 또는 비정규화된 식별자를 명시적으로 생성할 수 있습니다:

```java
// 비정규화된 테이블 식별자
Name table = name("t");

// 정규화된 컬럼 식별자
Name field = name("t", "col");
```

테이블 생성과 같은 DDL 문에서 이러한 식별자를 사용합니다:

```java
ctx.createTable(table)
   .column(field, SQLDataType.INTEGER)
   .execute();
```

정규화의 필요성은 컨텍스트에 따라 다릅니다. 이 경우 field 정규화는 필요하지 않습니다. 편의를 위해 `createTable(String)` API에서 String 타입을 사용할 수 있습니다:

```java
ctx.createTable("t")
   .column("col", SQLDataType.INTEGER)
   .execute();
```

이러한 문자열은 설명한 것처럼 `DSL.name(String)`으로 래핑됩니다.

주의: jOOQ에서는 기본적으로 모든 식별자가 인용됩니다(`RenderQuotedNames.EXPLICIT_DEFAULT_QUOTED`). 이것은 다음과 같은 이점을 제공합니다:

- 특수 문자와 키워드 충돌이 올바르게 처리됩니다
- 인용은 SQL 인젝션을 방지합니다
- 인용된 식별자를 지원하는 방언에서 대소문자 구분이 올바르게 처리됩니다

대가는 인용된 식별자가 원하지 않을 때 대소문자를 구분하게 될 수 있다는 것입니다. 설정에서 인용을 끄면 이 문제를 해결할 수 있습니다. 예를 들어 `RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED`를 설정하면 됩니다. 그러나 식별자 이름을 먼저 정제하지 않으면 SQL 인젝션 위험에 다시 노출된다는 점에 주의하십시오.

## 타입 4: 키워드

키워드도 jOOQ에서 문자열입니다. 드문 경우지만 키워드를 문자열로 표현한 것을 `org.jooq.Keyword` 타입으로 래핑할 수 있습니다. jOOQ 3.13 기준으로 주요 이점은 일관된 키워드 스타일입니다. 클라이언트 코드에서 이 기능을 거의 사용하지 않기 때문에 편의 API는 없습니다. `DSL.keyword(String)`만 사용할 수 있습니다:

```java
Keyword current = keyword("current");
Keyword time = keyword("time");
```

Plain SQL 템플릿에서 키워드를 사용합니다:

```java
Field<Time> currentTime = field(
  "{0} {1}",
  SQLDataType.TIME,
  current, time
);
```

---

태그: jooq, Plain SQL Templating, String, Stringly typed, Type safety
