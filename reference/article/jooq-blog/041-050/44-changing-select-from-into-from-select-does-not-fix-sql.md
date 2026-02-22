# SELECT .. FROM을 FROM .. SELECT로 바꾼다고 SQL이 "고쳐지는" 것은 아니다

> 원문: https://blog.jooq.org/changing-select-from-into-from-select-does-not-fix-sql/

## 영어라는 언어

SQL 명령은 SELECT, INSERT, UPDATE, DELETE, MERGE, TRUNCATE, CREATE, ALTER, DROP과 같은 명령형 동사로 시작한다. 이는 자연어의 패턴을 반영한 것으로, 우리가 느낌표와 함께 명령을 내리는 방식과 유사하다.

## 연산의 순서

비판론자들은 SQL의 문법이 논리적 실행 순서와 맞지 않는다고 주장한다. 논리적 순서는 FROM -> WHERE -> SELECT이지만, SQL은 SELECT -> FROM으로 읽힌다. 더 논리적인 문법은 다음과 같을 것이다:

```
FROM book
WHERE book.title LIKE 'A%'
SELECT book.id, book.title
```

이는 Java Stream API 패턴과 유사하다:

```
books.stream()
     .filter(book -> book.title.startsWith("A"))
     .map(book -> new B(book.id, book.title))
```

FROM을 먼저 쓰는 순서의 장점으로는 문법과 논리 사이의 괴리를 없앨 수 있다는 점, WHERE 절에서 SELECT 별칭을 참조할 수 없는 이유에 대한 혼란을 방지할 수 있다는 점, 그리고 더 나은 자동 완성이 가능하다는 점 등이 있다.

## RETURNING 절과 대안들

일부 데이터베이스는 DML 문에 대해 실용적인 해결책을 구현했다. PostgreSQL, Oracle, Firebird는 다음과 같은 문법을 사용한다:

```
INSERT INTO book (id, title)
VALUES (3, 'The Book')
RETURNING id, created_at
```

SQL 표준은 데이터 변경 델타 테이블을 통한 대안적인 문법을 정의하고 있다:

```
SELECT id, created_at
FROM FINAL TABLE (
  INSERT INTO book (id, title)
  VALUES (3, 'The Book')
) AS book
```

SQL Server는 자체적인 OUTPUT 절 방식을 사용한다.

## Cypher 쿼리 언어

Neo4j의 Cypher 언어는 흥미로운 중간 지점을 제시한다. Cypher는 명령형 동사로 시작하는 SQL의 장점을 유지하면서(FROM 대신 MATCH를 사용), 논리적 연산 순서를 MATCH .. RETURN으로 뒤집었다. 이 접근 방식은:

- 직관적인 명령 구조를 보존한다
- 연산 전반에 걸쳐 일관된 프로젝션을 구현한다
- 읽기와 쓰기 연산 모두에 동사를 재사용한다

서로 다른 데이터 패러다임에서 동작하지만, "Cypher는 고수준 설계 측면에서 일반적으로 SQL보다 우수한 문법을 제공한다."

## jOOQ가 SQL을 "고치지" 않는 이유

jOOQ는 의도적으로 SQL 문법과 API 사이의 1:1 매핑을 유지한다. 그 근거는 실용적이다: SQL에는 SELECT-FROM 순서 문제 이상의 근본적인 이상한 점들이 있다. "GROUP BY 의미론은 여전히 문제가 있고," "DISTINCT와 ORDER BY 사이의 관계"는 문법을 재배열하더라도 지속적인 혼란을 야기한다.

부분적인 수정을 시도하면 불일치가 생긴다. FROM-SELECT를 채택한다면, 개선은 어디서 멈춰야 하는가? GROUP BY 의미론, ORDER BY와 함께하는 DISTINCT의 동작 등 다른 이상한 점들은 여전히 남아 있을 것이다:

```
FROM book
WHERE book.title LIKE 'A%'
SELECT book.title
DISTINCT
ORDER BY book.title
```

"DISTINCT를 사용하지 않는 쿼리에서는 가능함에도 불구하고, DISTINCT를 사용할 때는 SELECT 외부의 표현식으로 ORDER BY를 할 수 없다"는 어색한 제약은 변하지 않을 것이다.

## 대안적 DSL 접근 방식

일부 경쟁 라이브러리들은 SQL을 "고치는" 것을 수용한다:

Slick (Scala):
```
coffees.map(_.price).max
```
이는 다음을 생성한다: `SELECT max(price) FROM coffees`

Exposed (Kotlin):
```
StarWarsFilms
  .slice(StarWarsFilms.sequelId.count(), StarWarsFilms.director)
  .selectAll()
  .groupBy(StarWarsFilms.director)
```

Exposed는 새로운 용어를 도입한다: `slice`(SELECT와 유사)와 `selectAll`(관계 대수의 "선택", WHERE와 유사).

## jOOQ의 설계 철학

jOOQ는 이 길을 거부한다. 1:1 매핑은 사용자가 GROUPING SETS, CONNECT BY, CTE, FOR XML/JSON과 같은 복잡한 SQL 기능을 중간 문법을 발명하지 않고도 활용할 수 있게 해준다. 이 접근 방식은 완전성을 보장한다 - jOOQ는 경쟁 프레임워크들이 지원하기 어려워하는 고급 기능에 대한 우회책을 발명할 필요가 없다.

대안적 문법의 문제는 지속 가능성이다. 단순한 SELECT-FROM 빌더는 여러 API에서 작동하지만, 고급 기능을 처리하도록 발전시키는 것은 점점 어려워진다. 대안 프레임워크들은 복잡한 시나리오에서 종종 "네이티브 SQL을 사용하라"는 해결책에 의존하게 된다.

## 합성 문법과 편의 문법

jOOQ는 엄격한 SQL 모방에 두 가지 예외를 허용한다:

합성 문법(Synthetic syntax)은 SQL이 결국 채택할 수 있는 기능으로 SQL을 향상시키며, 방언이 혁신을 도입하는 방식과 유사하다:
- BigQuery의 `* EXCEPT (...)`
- PostgreSQL의 `DISTINCT ON`
- 역사적으로 채택된 `LIMIT .. OFFSET`

jOOQ는 보수적인 태도를 유지하는데, 각 합성 요소가 다른 변환을 복잡하게 만들기 때문이다.

편의 문법(Convenience syntax)은 선택적 절 생략을 허용한다:
```
select(someFunction());  // 필수인 FROM을 생략
selectFrom(someTable);   // 명시적 SELECT 목록을 생략
```

jOOQ는 합리적으로 기본값을 채운다(FROM DUAL, SELECT *).

## 결론

언어를 설계하는 것은 - 내부 DSL을 포함하여 - "믿을 수 없을 정도로 어렵다." 포괄적인 SQL 기능 지원을 하려면 "고치겠다"는 열망을 버리고 모든 것을 "지원하겠다"는 자세를 수용해야 한다.

"SQL은 있는 그대로의 SQL이다. 그리고 그것은 문법이 SELECT .. FROM이지, FROM .. SELECT가 아니라는 것을 의미한다."

1:1 접근 방식은 13년에 걸쳐 실행 가능함이 입증되었으며, 대안 프레임워크들이 수용하기 어려워할 난해한 SQL 기능들을 지속적으로 발견하게 해주었다. 이 철학은 인지된 개선보다 완전성과 학습 용이성을 우선시한다.
