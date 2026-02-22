# 미들웨어에서 매핑하지 마라. SQL의 XML 또는 JSON 연산자를 사용하라

> 원문: https://blog.jooq.org/stop-mapping-stuff-in-your-middleware-use-sqls-xml-or-json-operators-instead/

저자: lukaseder
작성일: 2019년 11월 13일 (2022년 9월 25일 업데이트)

## 핵심 주장

필자는 개발자들에게 데이터베이스 결과를 JSON/XML로 변환하는 과도한 미들웨어 코드 작성을 그만둘 것을 권고한다. 대신 SQL Server, PostgreSQL 등의 데이터베이스가 제공하는 네이티브 JSON 및 XML 연산자를 사용하면 원하는 구조의 출력을 직접 생성할 수 있다.

## 문제점

전형적인 접근 방식은 다음과 같다:
- 도메인 주도 설계(DDD) 책을 읽는다
- 엔티티, DTO, 팩토리, 빌더를 만든다
- 불변성에 대해 논쟁한다 (Lombok, Autovalue, Immutables)
- JPA와 Hibernate 중 무엇을 선택할지 고민한다
- JSON 매핑을 위해 Jackson을 추가한다
- N+1 쿼리 문제를 디버깅한다
- 서로 충돌하는 어노테이션 시스템을 조합한다

필자의 지시: 이것을 즉시 중단하라.

## 해결책: SQL 네이티브 연산자

예제 사용 사례: 배우 목록과 그들의 영화 카테고리, 그리고 관련 영화들을 조회한다.

### SQL Server 예제 (JSON 출력)

```sql
SELECT
  a.first_name,
  a.last_name, (
    SELECT
      c.name, (
        SELECT title
        FROM film AS f
        JOIN film_category AS fc ON f.film_id = fc.film_id
        JOIN film_actor AS fa ON fc.film_id = fa.film_id
        WHERE fc.category_id = c.category_id
        AND a.actor_id = fa.actor_id
        FOR JSON PATH
      ) AS films
    FROM category AS c
    JOIN film_category AS fc ON c.category_id = fc.category_id
    JOIN film_actor AS fa ON fc.film_id = fa.film_id
    WHERE fa.actor_id = a.actor_id
    GROUP BY c.category_id, c.name
    FOR JSON PATH
  ) AS categories
FROM actor AS a
FOR JSON PATH, ROOT ('actors')
```

이 쿼리는 다음과 같은 결과를 생성한다:

```json
[{
  "first_name": "PENELOPE",
  "last_name": "GUINESS",
  "categories": [{
    "name": "Animation",
    "films": [{
      "title": "ANACONDA CONFESSIONS"
    }]
   }, {
    "name": "Family",
    "films": [{
      "title": "KING EVOLUTION"
    }, {
      "title": "SPLASH GUMP"
    }]
  }]
}]
```

### XML 버전

단순히 `FOR JSON PATH`를 `FOR XML PATH`로 대체하면 된다:

```sql
SELECT
  a.first_name,
  a.last_name, (
    SELECT
      c.name, (
        SELECT title
        FROM film AS f
        JOIN film_category AS fc ON f.film_id = fc.film_id
        JOIN film_actor AS fa ON fc.film_id = fa.film_id
        WHERE fc.category_id = c.category_id
        AND a.actor_id = fa.actor_id
        FOR XML PATH ('film'), TYPE
      ) AS films
    FROM category AS c
    JOIN film_category AS fc ON c.category_id = fc.category_id
    JOIN film_actor AS fa ON fc.film_id = fa.film_id
    WHERE fa.actor_id = a.actor_id
    GROUP BY c.category_id, c.name
    FOR XML PATH ('category'), TYPE
  ) AS categories
FROM actor AS a
FOR XML PATH ('actor'), ROOT ('actors')
```

결과:

```xml
<actors>
  <actor>
    <first_name>PENELOPE</first_name>
    <last_name>GUINESS</last_name>
    <categories>
      <category>
        <name>Animation</name>
        <films>
          <film>
            <title>ANACONDA CONFESSIONS</title>
          </film>
        </films>
      </category>
    </categories>
  </actor>
</actors>
```

## 핵심 기술적 요점

장점:
- 출력 구조가 변경되어도 도메인 모델을 수정할 필요가 없다
- 복잡한 매핑 어노테이션이 필요 없다
- 중첩 쿼리를 통해 N+1 쿼리 문제를 제거한다
- 단일 책임: 데이터베이스가 원하는 형식을 생성한다
- 개발 시간이 극적으로 단축된다

지원되는 방식:
- JDBC
- jOOQ
- JdbcTemplate
- MyBatis
- JPA 네이티브 쿼리

## FAQ 섹션

"내가 사용하는 SQL API가 이것을 처리할 수 없다"
대신 뷰를 작성하라. jOOQ 3.14 이상은 이 기능을 지원한다.

"SQL은 악이다"
강력한 JSON 기능을 가진 PostgreSQL을 사용하라.

"테스트가 더 어렵다"
TestContainers를 사용하여 스키마가 있는 테스트 데이터베이스를 띄워라.

"모킹이 더 낫다"
필자는 데이터베이스를 모킹한다는 것은 데이터베이스를 직접 다시 작성한다는 것을 의미한다고 주장한다.

"우리는 이미 미들웨어에 수년의 인력을 투자했다"
이것은 매몰 비용 오류를 반영한다.

"이건 90년대 스타일의 2계층 아키텍처다"
필자는 이것을 오히려 장점으로 본다: 5%의 시간만 들이고, 실제 가치를 위한 95%의 시간을 더 확보할 수 있다.

## 철학적 입장

핵심 원칙: *"미들웨어에서 소비하지 않을 데이터를 미들웨어에서 매핑하지 마라."*

필자는 단순한 데이터 포맷팅을 위해 정교한 Java 추상화 계층을 구축하는 대신, 일반적인 3세대 언어(3GL)의 역량을 넘어서는 쿼리 최적화를 갖춘 선언적 4세대 언어(4GL)로서의 SQL을 활용할 것을 주장한다.

---

데이터베이스 예제: Sakila 데이터베이스 (영화 대여 스키마) 사용
주로 다룬 벤더: SQL Server, PostgreSQL
