# BNF 표기법으로 본 jOOQ의 Fluent API

> 원문: https://blog.jooq.org/jooqs-fluent-api-in-bnf-notation/

저는 최근에 [Java에서 fluent API를 일반적으로 설계하는 방법](https://blog.jooq.org/the-java-fluent-api-designer-crash-course/)에 관한 글을 게시했습니다.

fluent API라 함은, 아래와 같은 단순한 구조를 의미하는 것이 아닙니다.

```java
new Builder().withSomething(x).withSomethingElse(y)
```

위의 코드는 정교한 형식 언어 정의 없이 단순한 [메서드 체이닝](https://stackoverflow.com/questions/293353/fluent-interfaces-method-chaining)일 뿐입니다. 대부분의 메서드 체이닝 컨텍스트에서, 메서드 호출 순서는 API에 있어 중요하지 않으며 자유롭게 선택할 수 있습니다(즉, "withAnotherThing()"을 먼저 호출하고, 그다음에 "withSomethingElse()"를 호출할 수 있습니다).

그 대신, 제가 의미하는 것은 BNF 표기법(또는 이에 상응하는 것)을 사용하여 형식적으로 정의된 본격적인 [도메인 특화 언어(domain specific language)](https://en.wikipedia.org/wiki/Domain-specific_language)입니다.

## jOOQ의 Fluent API

오늘은 jOOQ가 BNF 표기법을 사용하여 어떻게 진정한 도메인 특화 언어로 형식적으로 표현될 수 있는지에 대해 몇 가지 통찰을 공유하고자 합니다. jOOQ 자체는 사실상 그 자체로 하나의 SQL 방언으로 진화해 왔습니다. jOOQ는 대부분의 표준 DML SQL 문과 절뿐만 아니라, 많은 벤더별 SQL 문과 절도 알고 있습니다. jOOQ 2.0이 이해하는 [SELECT](https://en.wikipedia.org/wiki/Select_%28SQL%29) 문을 살펴보겠습니다:

```
SELECT ::=
   ( ( 'select' | 'selectDistinct' ) FIELD* |
       'selectOne' |
       'selectZero' |
       'selectCount' )
     ( 'select' FIELD* )*
     ( 'hint' SQL )*
     ( 'from' ( TABLE+ | SQL )
       ( ( 'join' | 'leftOuterJoin' | 'rightOuterJoin' | 'fullOuterJoin' )
         ( TABLE | SQL )
         ( 'on' ( CONDITION+ | SQL ) MORE_CONDITIONS? |
           'using' FIELD+ ) |
         ( 'crossJoin' | 'naturalJoin' |
           'naturalLeftOuterJoin' | 'naturalRightOuterJoin' )
       ( TABLE | SQL ) )* )?
     ( ( 'where' ( CONDITION+ | SQL ) |
         ( 'whereExists' | 'whereNotExists' ) SELECT ) MORE_CONDITIONS? )?
     ( ( 'connectBy' | 'connectByNoCycle' )
       ( CONDITION | SQL )
       ( 'and' ( CONDITION | SQL ) )*
       ( 'startWith' ( CONDITION | SQL ) )? )?
     ( 'groupBy' GROUP_FIELD+ )?
     ( 'having' ( CONDITION+ | SQL ) MORE_CONDITIONS? )?
     ( 'orderBy' ( FIELD+ | SORT_FIELD+ | INT+ ) )?
     ( ( 'limit' INT ( 'offset' INT | INT )? ) |
       ( ( 'forUpdate'
           ( 'of' ( FIELD+ | TABLE+ ) )?
           ( 'wait' INT |
             'noWait' |
             'skipLocked' )? ) |
         'forShare' ) )?
   ( ( 'union' | 'unionAll' | 'except' | 'intersect' ) SELECT )*
```

또는 훨씬 더 읽기 쉬운 그래픽 표현(railroad diagram)으로 나타낼 수도 있습니다.

[JPA Criteria Query API](http://docs.oracle.com/javaee/6/tutorial/doc/gjrij.html)나 QueryDSL과 같은 더 단순한 쿼리 API들과 달리, jOOQ는 SQL 구문 정확성을 정말로 강조합니다. 이런 방식 덕분에, "JOIN" 절 이전에 최소 하나의 테이블 소스를 선언하지 않고 "JOIN" 절을 제공하거나, "JOIN" 절을 먼저 정의하지 않고 "ON" 절을 제공하거나, "CROSS JOIN"에 "ON" 절을 제공하는 등의 비논리적인 방식으로 복잡한 SQL 쿼리를 구성하는 것이 불가능합니다.

## 결론

이 BNF와 그 결과물(jOOQ)을 제가 이전 블로그 게시물에서 [fluent API 설계](https://blog.jooq.org/the-java-fluent-api-designer-crash-course/)에 대해 작성한 내용과 비교해 보면, 이 과정을 완전히 형식화할 수 있는 잠재력이 있다는 것을 알 수 있습니다. 원칙적으로, 임의의 BNF는 Java에서 직접 도메인 특화 언어를 모델링하는 Java 인터페이스 집합으로 형식적으로 변환될 수 있습니다.
