# 왜 SQLJ는 죽었는가?

> 원문: https://blog.jooq.org/why-did-sqlj-die/

때때로 [SQLJ](https://en.wikipedia.org/wiki/SQLJ)가 어딘가에서 불쑥 나타나곤 하는데, 대부분 매우 오래된/엔터프라이즈적인 맥락이나 학술적인 맥락에서입니다.

하지만 SQLJ에 대해 생각해 보면, 그렇게 나쁜 아이디어는 아닙니다. SQLJ는:

- ANSI와 ISO 표준입니다
- SQL 표준의 일부입니다
- 이해하기 상당히 쉽습니다
- JDBC에 대한 상당히 강력한 확장입니다

그렇다면 왜 죽었을까요 (혹은 더 정확히 말하면, 왜 실제로 인기를 얻지 못했을까요)? [이 질문이 Stack Overflow에 올라왔고](https://stackoverflow.com/q/18829460/521799), [저는 답변을 남겼습니다](https://stackoverflow.com/a/20787316/521799).

여러분이 이미 SQL을 임베딩하기로 결정했다고 가정해 봅시다 ([템플릿 메커니즘을 통해 외부화하거나](https://blog.jooq.org/sql-templating-with-jooq-or-mybatis/ "SQL Templating with jOOQ or MyBatis"), [ORM으로 숨기거나](https://blog.jooq.org/popular-orms-dont-do-sql/ "Popular ORMs Don't do SQL"), [저장 프로시저를 사용하는 것](https://blog.jooq.org/what-are-procedures-and-functions-after-all/ "What are procedures and functions after all?")과는 반대로). SQL을 임베딩하는 데 있어 SQLJ가 최적의 솔루션이 아닌 몇 가지 이유가 있습니다:

### IDE 지원

[Pro*C](https://en.wikipedia.org/wiki/Pro*C)가 90년대에 C와 C++에서 잘 작동했지만, Java는 2000년대 초반에 정말로 급성장했습니다. Java와 함께 [Eclipse](http://eclipse.org/), [NetBeans](https://netbeans.org/), [JBuilder](http://www.embarcadero.com/products/jbuilder) 등 점점 더 많은 강력한 IDE들이 등장했습니다. 하지만 Java 전처리기와 IDE는 결코 친구가 되지 못했는데, 하나의 언어를 파싱하는 것만으로도 충분히 어렵기 때문입니다. 두 개의 언어를 파싱하고 도구를 제공하는 것은 훨씬 더 어렵습니다.

실제로 SQLJ는 주변의 Java 코드를 타입 안전하지 않게 만들었는데, IDE와 컴파일러가 .sqlj 파일이 전처리되기 전에는 처리할 수 없었기 때문입니다.

### SQL의 인기

사람들이 SQL 자체가 죽을 것이라고 생각하기 시작한 시기가 있었습니다. 먼저 [ORM의 등장과 함께 그렇게 생각했고](http://www.codinghorror.com/blog/2006/06/object-relational-mapping-is-the-vietnam-of-computer-science.html), 그다음에는 NoSQL의 등장과 함께 그렇게 생각했습니다. 사람들은 [DBA가 죽었다고 생각했습니다. 다시 한번](http://evdbt.com/the-dba-is-dead/).

글쎄요, 이것은 여러 번 틀렸다고 증명되었지만, 확실히 SQLJ 때문은 아니었습니다.

### 타입 안전성

2000년대 후반에는 Java의 [jOOQ](https://www.jooq.org)나 .NET의 LINQ-to-SQL과 같이 SQLJ에 대한 타입 안전한 대안들이 등장했는데, 이들은 구문 자동완성과 같은 IDE 기능을 활용합니다. 내부 도메인 특화 언어 / 쿼리 DSL이 됨으로써, 이러한 API들은 임베디드 SQL에 타입 안전성을 가져다줄 뿐만 아니라, SQLJ가 지원하지 않는 동적 SQL 생성도 가능하게 합니다.

### 예측

다른 언어에 SQL을 임베딩하는 것은 유용한 일이지만, SQLJ는 이 문제를 적절하게 해결하지 못했습니다. 따라서, 편히 쉬세요, SQLJ (R.I.P., SQLJ)
