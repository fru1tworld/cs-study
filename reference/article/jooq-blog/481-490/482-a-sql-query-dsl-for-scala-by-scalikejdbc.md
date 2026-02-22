# ScalikeJDBC의 Scala용 SQL 쿼리 DSL

> 원문: https://blog.jooq.org/a-sql-query-dsl-for-scala-by-scalikejdbc/

Scala용 SQL API는 정말 많이 존재합니다. Manuel Bernhardt가 [자신의 블로그](http://manuel.bernhardt.io/2014/02/04/a-quick-tour-of-relational-database-access-with-scala/)와 [Stack Overflow의 관련 질문](http://stackoverflow.com/q/1942644/521799)에서 유용한 목록을 정리해 두었습니다.

오늘 소개하고 싶은 것은 [ScalikeJDBC](http://scalikejdbc.org/documentation/query-dsl.html)입니다. 이것은 ASL 2.0 라이선스로 제공되는 SQL 쿼리 DSL(도메인 특화 언어)입니다. 다음 예제를 살펴보세요:

```scala
val orders: List[Order] = withSQL {
  select
    .from(Order as o)
    .innerJoin(Product as p).on(o.productId, p.id)
    .leftJoin(Account as a).on(o.accountId, a.id)
    .where.eq(o.productId, 123)
    .orderBy(o.id).desc
    .limit(4)
    .offset(0)
  }.map(Order(o, p, a)).list.apply()
```

jOOQ와 유사해 보이지만, SELECT DSL이 좀 더 엄격해 보입니다. 복잡한 WHERE 절 술어를 어떻게 구성해야 할지 명확하지 않습니다.

## Scala의 함수형 특성 활용

ScalikeJDBC가 영리하게 활용하는 것 중 하나는 Scala의 람다 표현식입니다. Scala에서는 람다 표현식이 어디에나 사용될 수 있고, 모든 것이 표현식이기 때문입니다. 이것은 모든 단일 DSL 메서드 호출이 또 다른 람다 표현식을 반환하여 임시 DSL 상태(중간 SQL 문을 나타내는)를 변환할 수 있다는 의미입니다. 이는 동적 SQL 구성에 매우 유용합니다.

다음 예제에서는 `accountRequired` 플래그를 확인하여 쿼리에 조건부로 JOIN을 추가합니다:

```scala
def findOrder(id: Long, accountRequired: Boolean) =
withSQL {
  select
    .from[Order](Order as o)
    .innerJoin(Product as p).on(o.productId, p.id)
    .map { sql =>
      if (accountRequired)
        sql.leftJoin(Account as a)
           .on(o.accountId, a.id)
      else
        sql
    }.where.eq(o.id, 13)
  }.map { rs =>
    if (accountRequired)
      Order(o, p, a)(rs)
    else
      Order(o, p)(rs)
  }.single.apply()
```

`map` 메서드가 중간 DSL 상태를 변환하는 데 사용되며, 람다 표현식을 통해 조건부 쿼리 구성이 가능해집니다.

## 문제점

SQL 쿼리 DSL이 계속해서 새롭게 만들어지는 이유는 SQL이 본질적으로 타입 안전하고 조합 가능한 언어이기 때문입니다. 이로 인해 문자열 기반 API는 문제가 됩니다. 하지만 잘못 설계된 DSL은 사용성 문제를 야기합니다.

예를 들어, 선택적 술어를 처리하는 다음 코드를 보세요:

```scala
val ids = withSQL {
  select(o.result.id).from(Order as o)
    .where(sqls.toAndConditionOpt(
      productId.map(id => sqls.eq(o.productId, id)),
      accountId.map(id => sqls.eq(o.accountId, id))
    ))
    .orderBy(o.id)
}.map(_.int(1)).list.apply()
```

`toAndConditionOpt` 메서드는 정말 예상치 못한 방식입니다. 이것은 "최소 놀람의 원칙(principle of least astonishment)"을 위반합니다. 사용자가 직관적으로 이해하기 어려운 API 설계입니다.

## jOOQ의 접근 방식

jOOQ는 이 문제를 해결하기 위해 API 설계를 SQL 자체의 구조를 반영하는 정형 BNF 문법에 기반합니다. 이를 통해 SQL을 이미 알고 있는 개발자라면 jOOQ의 fluent API를 직관적으로 사용할 수 있습니다.

타입 안전한 내부 DSL은 JDBC/ODBC의 한계를 해결하여 컴파일 타임 검증과 데이터베이스 작업에 대한 향상된 조합성을 제공합니다.
