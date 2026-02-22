# jOOQ를 사용할 때와 네이티브 SQL을 사용할 때

> 원문: https://blog.jooq.org/when-to-use-jooq-and-when-to-use-native-sql/

## jOOQ를 사용할 때 "복잡한" 쿼리를 jOOQ API로 작성해야 할지, 네이티브 SQL로 구현해야 할지 결정하는 것은 자주 마주치는 고민이다.

jOOQ 매뉴얼에는 동일한 쿼리를 나란히 비교하는 예제가 가득하다. 예를 들면:

jOOQ 사용:

```java
ctx.select(AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME, count())
   .from(AUTHOR)
   .join(BOOK).on(AUTHOR.ID.eq(BOOK.AUTHOR_ID))
   .groupBy(AUTHOR.ID, AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME)
   .fetch();
```

네이티브 SQL 사용:

```sql
SELECT author.first_name, author.last_name, COUNT(*)
FROM author
JOIN book ON author.id = book.author_id
GROUP BY author.id, author.first_name, author.last_name;
```

네이티브 SQL의 경우에도 jOOQ의 plain SQL 템플릿 API를 사용할 수 있으며, Java 텍스트 블록을 활용하면 이상적이다. 이렇게 하면 다음과 같은 jOOQ의 여러 기능을 활용할 수 있다:

* 더 간단한 바인드 값 처리
* 동적 텍스트 기반 SQL을 위한 템플릿
* 모든 매핑 유틸리티
* 트랜잭션 API 또는 R2DBC 지원

jOOQ를 사용하면 다음과 같이 작성하게 된다:

```java
ctx.fetch(
    """
    SELECT author.first_name, author.last_name, COUNT(*)
    FROM author
    JOIN book ON author.id = book.author_id
    GROUP BY author.id, author.first_name, author.last_name
    """
);
```

## 명백한 장단점

먼저, 어떤 상황에서든 jOOQ를 사용하는 것에 대한 명백한 장단점이 있다.

장점:

다음과 같은 경우에는 jOOQ보다 나은 것을 찾기 어렵다:

* SQL이 동적일 때
* 여러 데이터베이스 방언(dialect)을 동시에(제품이 여러 RDBMS를 지원하는 경우) 또는 순차적으로(결국 RDBMS를 전환해야 할 수 있는 경우) 지원해야 할 때. 방언 간의 많은 차이점을 보여주는 예시로는 SQL/JSON 지원이 있다.
* jOOQ의 코드 생성 및 매핑을 통한 타입 안전성을 선호할 때
* 복잡한 데이터 트리를 조회해야 할 때
* 런타임에 SQL을 다양한 방식으로 변환해야 할 때
* 코드 생성을 통해 저장 프로시저 및 함수에 바인딩해야 할 때
* SQL 인젝션이 우려될 때
* 데이터를 내보내야 할 때
* 런타임 SQL에 대해 진단 및 린팅을 실행하고 싶을 때

모든 장점은 위 링크에서 연결된 글에서 설명하고 있으므로 여기서 다시 반복하지 않겠다.

단점:

jOOQ가 때때로 방해가 될 수 있다:

* 수많은 공통 테이블 표현식(CTE)과 파생 테이블을 사용하는 대형 정적 쿼리가 있을 때
* Java IDE 대신 SQL 편집기에서 쿼리를 개발하는 것을 선호할 때

이 두 가지 항목을 빠르게 살펴보자.

* CTE와 파생 테이블은 jOOQ에서 쿼리에 내장하는 대신 미리 선언해야 한다. 이는 대부분의 경우 jOOQ의 일반적인 타입 안전성을 유지하기 어렵고, 문자열 식별자를 사용하여 쿼리를 구성하게 된다는 것을 의미한다. 쿼리가 동적일 때는 이 접근 방식이 여전히 매우 강력하다. 하지만 정적일 때는 jOOQ가 문제를 해결하기보다 사용성 문제를 더 많이 일으키는 것처럼 보일 수 있다.
* jOOQ 쿼리를 순수하게 Java로 개발하면서 테스트 주도 개발(TDD) 방식을 따르고, 가급적 testcontainers에서 실행하는 것이 완전히 가능하긴 하지만, 네이티브 SQL로 쿼리를 작성하는 것이 더 편할 수 있다. 예를 들어 Dbeaver나 원하는 다른 SQL 편집기에서 말이다. 이 경우, 완성된 쿼리를 jOOQ로 변환해야 한다. 자동 변환 서비스("Java dialect" 사용)를 제공하고 있지만, 특히 나중에 쿼리를 다시 편집해야 할 때 여전히 "맞지 않는" 느낌이 들 수 있다.

## 두 세계의 장점 모두 취하기

jOOQ는 맞지 않는 곳에서 jOOQ를 사용하도록 강요하지 않는다. 이전 글에서 ORM(객체 그래프 영속성 구현)이 더 잘 작동하는 경우에 대해 이미 자세히 설명한 바 있다. 이 경우에는 순수 SQL에 대해 논의하고 있는데, jOOQ가 ORM에 비해 빛나는 영역이지만, 특정 "유형의 SQL 쿼리"에는 적합하지 않을 수 있다.

그 유형은 복잡한 정적 쿼리이다. jOOQ 관점에서는 Oracle `MODEL` 절과 같은 매우 고급 벤더 특화 SQL 기능을 사용하지 않는 한 상당히 먼 곳까지 도달할 수 있다. 하지만 이것은 취향의 문제이다. 특정 쿼리가 jOOQ에 비해 너무 복잡하다고 느끼는 경우 jOOQ가 권장하는 방법은 다음 중 하나이다:

* plain SQL 템플릿으로 되돌리기 (이상적으로는 jOOQ를 사용)

또는 더 나은 방법으로, 그 로직을 다음으로 추출하기:

* SQL 뷰
* SQL 테이블 값 함수

뷰는 모든 주요 RDBMS에서 지원되며 매우 과소평가되고 있다. 테이블 값 함수는 인수를 받기 때문에 더 조합 가능하며, 일부 RDBMS(예: SQL Server)에서는 인라인될 수도 있다. 두 경우 모두 네이티브 SQL 구문을 유지하면서도 동시에 RDBMS가 객체를 컴파일하므로 타입 안전성의 이점을 누릴 수 있다.

또한, 앞서 언급했듯이 통합 테스트를 사용하여 애플리케이션 개발에 TDD 방식을 따르는 것을 강력히 권장한다. 이렇게 하면 Flyway나 Liquibase 또는 기타 데이터베이스 변경 관리 수단을 사용하여 스키마에 뷰와 테이블 값 함수를 추가하는 것이 간단해진다.

변경이 적용된 후 jOOQ 코드를 재생성하면, 타입 안전성을 잃지 않고 새로운 스키마 객체를 Java 애플리케이션에서 즉시 사용할 수 있다.

이러한 실용적인 접근 방식을 통해 jOOQ와 네이티브 SQL 두 세계의 장점을 모두 취할 수 있다.
