# jOOQ로 다른 RDBMS에서 SQL Server FOR XML 및 FOR JSON 구문 사용하기

> 원문: https://blog.jooq.org/using-sql-server-for-xml-and-for-json-syntax-on-other-rdbms-with-jooq/

SQL Server는 편리한 `FOR XML` 또는 `FOR JSON` 구문을 사용하여 평면적인 테이블 형태의 SQL 결과 집합을 관례에 따라 계층적 구조로 변환하는 것을 지원합니다. 이것은 정말 편리하고 표준 SQL/XML 또는 SQL/JSON API보다 덜 장황합니다 – 비록 표준 방식이 더 강력하긴 하지만요. 이 블로그 글에서는 SQL Server 구문의 몇 가지 핵심 기능과 그것들이 표준 SQL에서 어떻게 대응되는지 보여드리고자 합니다. jOOQ 3.14는 SQL Server의 구문과 표준 구문을 모두 지원할 예정이며, 하나에서 다른 것으로 변환할 수 있어서 Db2, MariaDB, MySQL, Oracle, PostgreSQL에서도 SQL Server 구문을 사용할 수 있게 됩니다. 현재 개발 상태를 [저희 웹사이트에서](https://www.jooq.org/translate) 직접 사용해 보실 수 있습니다. 항상 그렇듯이, Sakila 데이터베이스를 사용한 간단한 예제를 맛보기로 보여드리겠습니다:

## 맛보기 예제

SQL Server:
```sql
SELECT a.first_name, a.last_name, f.title
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
JOIN film f ON fa.film_id = f.film_id
FOR XML RAW;
```

Db2, Oracle, PostgreSQL:
```sql
SELECT xmlagg(xmlelement(
  NAME row,
  xmlattributes(
    t.first_name AS first_name,
    t.last_name AS last_name,
    t.title AS title
  )
))
FROM (
  -- 원본 쿼리
  SELECT a.first_name, a.last_name, f.title
  FROM actor a
  JOIN film_actor fa ON a.actor_id = fa.actor_id
  JOIN film f ON fa.film_id = f.film_id
) AS t
```

출력 (두 경우 모두):
```xml
<row first_name="PENELOPE" last_name="GUINESS" title="OKLAHOMA JUMANJI"/>
<row first_name="PENELOPE" last_name="GUINESS" title="RULES HUMAN"/>
<row first_name="PENELOPE" last_name="GUINESS" title="SPLASH GUMP"/>
<row first_name="PENELOPE" last_name="GUINESS" title="VERTIGO NORTHWEST"/>
<row first_name="PENELOPE" last_name="GUINESS" title="WESTWARD SEABISCUIT"/>
<row first_name="PENELOPE" last_name="GUINESS" title="WIZARD COLDBLOODED"/>
<row first_name="NICK" last_name="WAHLBERG" title="ADAPTATION HOLES"/>
<row first_name="NICK" last_name="WAHLBERG" title="APACHE DIVINE"/>
```

## FOR XML과 FOR JSON 개념

위의 맛보기에서 볼 수 있듯이, SQL Server 구문은 훨씬 덜 장황하고 간결합니다. 그리고 합리적인 기본 동작을 생성하는 것으로 보입니다. 반면 Db2, Oracle, PostgreSQL (그리고 SQL 표준) SQL/XML API는 더 장황하지만, 더 강력하기도 합니다. 예를 들어, 컬럼 a를 속성 x에 매핑하고 컬럼 b를 중첩된 XML 요소 y에 매핑하는 것이 매우 쉽습니다. 두 접근 방식의 장점은 명확합니다. SQL Server의 접근 방식은 일반적인 경우에 훨씬 더 사용하기 편리합니다. 하지만 일반적인 경우란 무엇일까요? SQL 결과 집합과 XML/JSON 데이터 구조 사이의 몇 가지 주요 유사점을 정리해 보겠습니다:

- 테이블은 XML 요소, 또는 JSON 배열입니다
- 행은 XML 요소, 또는 JSON 객체입니다
- 컬럼 값은 XML 요소나 속성, 또는 JSON 속성입니다
- GROUP BY와 ORDER BY는 데이터를 중첩하는 방법으로 볼 수 있습니다

### 테이블은 XML 요소 또는 JSON 배열입니다

테이블(즉, 데이터 집합)은 XML과 JSON 문서 모두에게 낯선 개념이 아닙니다. XML에서 데이터 집합을 표현하는 가장 자연스러운 방법은 같은 요소 이름을 사용하는 요소들의 집합이며, 선택적으로 래퍼 요소로 감싸는 것입니다. 예를 들어:

```xml
<!-- 래퍼 요소 포함 -->
<films>
  <film title="OKLAHOMA JUMANJI"/>
  <film title="RULES HUMAN"/>
  <film title="SPLASH GUMP"/>
</films>

<!-- 래퍼 요소 없이 -->
<film title="OKLAHOMA JUMANJI"/>
<film title="RULES HUMAN"/>
<film title="SPLASH GUMP"/>
```

래퍼 요소를 추가할지 여부의 구분은 데이터를 중첩할 때 주로 중요합니다. JSON에서는 테이블을 표현하기 위한 명백한 데이터 구조 선택은 배열입니다. 예를 들어:

```json
[
  {"title": "OKLAHOMA JUMANJI"},
  {"title": "RULES HUMAN"},
  {"title": "SPLASH GUMP"}
]
```

### 행은 XML 요소 또는 JSON 객체입니다

위에서 이미 보았듯이, SQL 행은 XML에서 요소를 사용하여 표현됩니다.

```xml
<film title="OKLAHOMA JUMANJI"/>
```

문제는 요소 이름이 무엇이어야 하는가입니다. 일반적으로 다음 중 하나가 될 수 있습니다:

- "row"와 같은 표준 이름
- 행이 속한 테이블의 이름
- 사용자 정의 이름

JSON에서는 객체입니다.

```json
{"title": "OKLAHOMA JUMANJI"}
```

XML과 달리, 요소 이름 같은 것이 없으므로 행은 "익명"입니다. 행 타입은 JSON 객체가 포함된 테이블/배열에 의해 정의됩니다.

### 컬럼 값은 XML 요소나 속성, 또는 JSON 속성입니다

XML에서 SQL 컬럼 값을 표현하는 방법에는 몇 가지 더 많은 선택지가 있습니다. 주로 두 가지 선택:

- 값을 속성으로 표현
- 값을 요소로 표현

스칼라 값은 속성으로 쉽게 표현할 수 있습니다. 값이 추가 중첩이 필요한 경우(예: 배열, 사용자 정의 타입 등), 요소가 더 나은 선택입니다. 대부분의 경우, 선택은 중요하지 않으므로 둘 다 선택할 수 있습니다:

```xml
<!-- 속성 사용 -->
<film film_id="635" title="OKLAHOMA JUMANJI"/>

<!-- 테이블과 컬럼 이름에서 요소 사용 -->
<film>
  <film_id>635</film_id>
  <title>OKLAHOMA JUMANJI</title>
</film>

<!-- 표준 요소 이름 사용 -->
<row>
  <value name="film_id" value="635"/>
  <value name="title" value="OKLAHOMA JUMANJI"/>
</row>
```

XML에서 컬럼 값을 표현하는 다른 합리적인 기본 옵션들이 몇 가지 더 있습니다. 반면 JSON에서는 두 가지 주요 합리적인 접근 방식이 있습니다. 대부분의 경우, 컬럼 값이 컬럼 이름으로 식별되는 객체가 선택될 것입니다. 하지만 SQL 레코드가 "구조체"와 "튜플"의 혼합인 것처럼, 컬럼 값을 배열 인덱스에 매핑하는 표현도 상상할 수 있습니다:

```json
// 객체 사용
{"film_id": 635, "title": "OKLAHOMA JUMANJI"}

// 배열 사용
[635, "OKLAHOMA JUMANJI"]
```

### GROUP BY와 ORDER BY는 데이터를 중첩하는 방법으로 볼 수 있습니다

지금까지 모든 데이터는 SQL 테이블처럼 평면적인 방식으로 표현되었습니다. XML 요소나 JSON 배열을 어떤 래퍼 요소나 객체로 감싸거나, XML 데이터를 속성보다 더 많은 요소로 표현할 때 약간의 중첩이 있었지만, 데이터는 여전히 항상 테이블 형태였습니다. 매우 자주, 우리는 데이터를 계층적 형태로 소비하고 싶어합니다. 배우가 영화에 출연했으므로, 모든 영화에 대해 배우 정보를 반복하는 대신 배우별로 영화를 그룹화하고 싶습니다. 일반적으로, `GROUP BY`나 `ORDER BY` 같은 연산이 이 목적을 수행합니다. `GROUP BY`는 모든 데이터를 그룹당 중첩된 데이터 구조(예: 문자열, 배열, XML 요소, JSON 배열, JSON 객체)로 집계할 수 있게 합니다. `ORDER BY`도 "시각적으로" 같은 일을 합니다 – 아마도 좀 덜 형식적으로. 이 XML 요소 집합을 보면, 배우별로 "그룹화"(즉, 정렬)되어 있음을 시각적으로 볼 수 있습니다:

```xml
<row first_name="PENELOPE" last_name="GUINESS" title="OKLAHOMA JUMANJI"/>
<row first_name="PENELOPE" last_name="GUINESS" title="RULES HUMAN"/>
<row first_name="PENELOPE" last_name="GUINESS" title="SPLASH GUMP"/>
<row first_name="PENELOPE" last_name="GUINESS" title="VERTIGO NORTHWEST"/>
<row first_name="PENELOPE" last_name="GUINESS" title="WESTWARD SEABISCUIT"/>
<row first_name="PENELOPE" last_name="GUINESS" title="WIZARD COLDBLOODED"/>
<row first_name="NICK" last_name="WAHLBERG" title="ADAPTATION HOLES"/>
<row first_name="NICK" last_name="WAHLBERG" title="APACHE DIVINE"/>
```

SQL Server는 이러한 그룹화를 최소 두 가지 방법으로 지원합니다:

- `ORDER BY`를 사용하여 관례에 의한 암시적 방식
- 상관 서브쿼리를 생성하여 명시적 방식

암시적 접근 방식은 위의 평면적 표현을 다음과 같이 변환할 수 있습니다:

```xml
<a first_name="PENELOPE" last_name="GUINESS">
    <f title="OKLAHOMA JUMANJI"/>
    <f title="RULES HUMAN"/>
    <f title="SPLASH GUMP"/>
    <f title="VERTIGO NORTHWEST"/>
    <f title="WESTWARD SEABISCUIT"/>
    <f title="WIZARD COLDBLOODED"/>
</a>
<a first_name="NICK" last_name="WAHLBERG">
    <f title="ADAPTATION HOLES"/>
    <f title="APACHE DIVINE"/>
</a>
```

… 여기서 "a"와 "f"는 쿼리의 테이블 이름입니다 (`actor a`와 `film f`).

## FOR XML과 FOR JSON은 자세히 어떻게 작동하나요?

SQL Server에서 결합할 수 있는 여러 기능이 있습니다. 전체 그림은 [문서](https://docs.microsoft.com/en-us/sql/relational-databases/xml/for-xml-sql-server)에서 볼 수 있습니다. 이 블로그 글에서는 몇 가지 기능을 생략하겠습니다.

- 변환 알고리즘 `RAW` (평면 결과, XML만 해당), `AUTO` (계층적, 자동 결과), `PATH` (계층적, 명시적 결과)
- "root" 이름, XML 래퍼 요소 또는 JSON 래퍼 객체에 해당
- XML만 해당: 값을 `ELEMENTS`에 배치할지 속성에 배치할지
- JSON만 해당: `INCLUDE_NULL_VALUES`는 `NULL` 값이 명시적인지, 암시적(JSON 객체에서 부재)인지 지정
- JSON만 해당: `WITHOUT_ARRAY_WRAPPER`는 JSON 객체 집합이 JSON 배열로 나열될지, 쉼표로 구분된 객체 목록(다른 쿼리와 결합 가능)으로 나열될지 지정

이것이 전부는 아닙니다. 더 많은 플래그와 기능이 있지만, 이론으로 논의하는 대신 몇 가지 예제를 살펴보겠습니다:

### FOR XML RAW

값에 대한 속성을 가진 평면 결과 생성

SQL Server:
```sql
SELECT a.first_name, a.last_name, f.title
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
JOIN film f ON fa.film_id = f.film_id
ORDER BY 1, 2, 3
FOR XML RAW;
```

표준 SQL:
```sql
SELECT xmlagg(xmlelement(
  NAME row,
  xmlattributes(
    t.first_name AS first_name,
    t.last_name AS last_name,
    t.title AS title
  )
))
FROM (
  SELECT a.first_name, a.last_name, f.title
  FROM actor a
  JOIN film_actor fa ON a.actor_id = fa.actor_id
  JOIN film f ON fa.film_id = f.film_id
  ORDER BY 1, 2, 3
) AS t
```

출력:
```xml
<row first_name="NICK" last_name="WAHLBERG" title="SMILE EARRING"/>
<row first_name="NICK" last_name="WAHLBERG" title="WARDROBE PHANTOM"/>
<row first_name="PENELOPE" last_name="GUINESS" title="ACADEMY DINOSAUR"/>
<row first_name="PENELOPE" last_name="GUINESS" title="ANACONDA CONFESSIONS"/>
```

### FOR XML RAW, ROOT

값에 대한 속성을 가진 평면 결과 생성, 그리고 나열된 요소들을 감싸는 루트 요소

SQL Server:
```sql
SELECT a.first_name, a.last_name, f.title
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
JOIN film f ON fa.film_id = f.film_id
ORDER BY 1, 2, 3
FOR XML RAW, ROOT('rows');
```

표준 SQL:
```sql
SELECT xmlelement(
  NAME rows,
  xmlagg(xmlelement(
    NAME row,
    xmlattributes(
      t.first_name AS first_name,
      t.last_name AS last_name,
      t.title AS title
    )
  ))
)
FROM (
  SELECT a.first_name, a.last_name, f.title
  FROM actor a
  JOIN film_actor fa ON a.actor_id = fa.actor_id
  JOIN film f ON fa.film_id = f.film_id
  ORDER BY 1, 2, 3
) AS t
```

출력:
```xml
<rows>
  <row first_name="NICK" last_name="WAHLBERG" title="SMILE EARRING"/>
  <row first_name="NICK" last_name="WAHLBERG" title="WARDROBE PHANTOM"/>
  <row first_name="PENELOPE" last_name="GUINESS" title="ACADEMY DINOSAUR"/>
  <row first_name="PENELOPE" last_name="GUINESS" title="ANACONDA CONFESSIONS"/>
</rows>
```

### FOR XML RAW, ELEMENTS

값에 대한 요소를 가진 평면 결과 생성.

SQL Server:
```sql
SELECT a.first_name, a.last_name, f.title
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
JOIN film f ON fa.film_id = f.film_id
ORDER BY 1, 2, 3
FOR XML RAW, ELEMENTS;
```

표준 SQL:
```sql
SELECT xmlagg(xmlelement(
  NAME row,
  xmlelement(
    NAME first_name,
    first_name
  ),
  xmlelement(
    NAME last_name,
    last_name
  ),
  xmlelement(
    NAME title,
    title
  )
))
FROM (
  SELECT a.first_name, a.last_name, f.title
  FROM actor a
  JOIN film_actor fa ON a.actor_id = fa.actor_id
  JOIN film f ON fa.film_id = f.film_id
  ORDER BY 1, 2, 3
) AS t
```

출력:
```xml
<row>
    <first_name>NICK</first_name>
    <last_name>WAHLBERG</last_name>
    <title>SMILE EARRING</title>
</row>
<row>
    <first_name>NICK</first_name>
    <last_name>WAHLBERG</last_name>
    <title>WARDROBE PHANTOM</title>
</row>
<row>
    <first_name>PENELOPE</first_name>
    <last_name>GUINESS</last_name>
    <title>ACADEMY DINOSAUR</title>
</row>
<row>
    <first_name>PENELOPE</first_name>
    <last_name>GUINESS</last_name>
    <title>ANACONDA CONFESSIONS</title>
</row>
```

이것은 `ROOT`와도 결합할 수 있지만, 간결함을 위해 생략합니다.

### FOR XML/JSON AUTO

이 접근 방식은 쿼리 구조에서 결과를 완전히 자동으로 도출합니다. 주로:

- `SELECT` 절은 XML 또는 JSON 데이터가 어떤 순서로 중첩되는지 정의합니다.
- `FROM` 절은 (별칭을 통해) 테이블 이름을 정의하며, 이것이 XML 요소 또는 JSON 객체 속성 이름으로 변환됩니다.
- `ORDER BY` 절은 "그룹화"를 생성하며, 이것이 XML 요소 또는 JSON 객체 중첩으로 변환됩니다.

SQL Server:
```sql
SELECT a.first_name, a.last_name, f.title
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
JOIN film f ON fa.film_id = f.film_id
ORDER BY 1, 2, 3
FOR XML AUTO;
```

표준 SQL:
```sql
SELECT xmlagg(e)
FROM (
  SELECT xmlelement(
    NAME a,
    xmlattributes(
      t.first_name AS first_name,
      t.last_name AS last_name
    ),
    xmlagg(xmlelement(
      NAME f,
      xmlattributes(t.title AS title)
    ))
  ) AS e
  FROM (
    SELECT a.first_name, a.last_name, f.title
    FROM actor a
    JOIN film_actor fa ON a.actor_id = fa.actor_id
    JOIN film f ON fa.film_id = f.film_id
    ORDER BY 1, 2, 3
  ) AS t
  GROUP BY
    first_name,
    last_name
) AS t
```

이 에뮬레이션이 `GROUP BY`와 함께 두 단계의 `XMLAGG`를 필요로 하는 것을 주목하세요. 더 많은 테이블이 조인되고 프로젝션되면 더 복잡해집니다! 여기에 더 복잡한 예제를 추가하지는 않겠지만, 온라인에서 시도해 보세요! 이것은 다음을 생성합니다

```xml
<a first_name="NICK" last_name="WAHLBERG">
    <f title="SMILE EARRING"/>
    <f title="WARDROBE PHANTOM"/>
</a>
<a first_name="PENELOPE" last_name="GUINESS">
    <f title="ACADEMY DINOSAUR"/>
    <f title="ANACONDA CONFESSIONS"/>
</a>
```

JSON으로 같은 것을 다시 시도해 봅시다:

SQL Server:
```sql
SELECT a.first_name, a.last_name, f.title
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
JOIN film f ON fa.film_id = f.film_id
ORDER BY 1, 2, 3
FOR JSON AUTO;
```

표준 SQL:
```sql
SELECT json_arrayagg(e)
FROM (
  SELECT JSON_OBJECT(
    KEY 'FIRST_NAME' VALUE first_name,
    KEY 'LAST_NAME' VALUE last_name,
    KEY 'F' VALUE JSON_ARRAYAGG(JSON_OBJECT(
      KEY 'TITLE' VALUE title
      ABSENT ON NULL
    ))
    ABSENT ON NULL
  ) e
  FROM (
    SELECT a.first_name, a.last_name, f.title
    FROM actor a
    JOIN film_actor fa ON a.actor_id = fa.actor_id
    JOIN film f ON fa.film_id = f.film_id
    ORDER BY 1, 2, 3
  ) t
  GROUP BY
    first_name,
    last_name
) t
```

출력:
```json
[
    {
        "first_name": "NICK",
        "last_name": "WAHLBERG",
        "f": [
            {
                "title": "SMILE EARRING"
            },
            {
                "title": "WARDROBE PHANTOM"
            }
        ]
    },
    {
        "first_name": "PENELOPE",
        "last_name": "GUINESS",
        "f": [
            {
                "title": "ACADEMY DINOSAUR"
            },
            {
                "title": "ANACONDA CONFESSIONS"
            }
        ]
    }
]
```

### FOR XML/JSON AUTO, ROOT

이전처럼, 필요하다면 이것을 루트 XML 요소나 루트 JSON 객체로 감쌀 수 있습니다.

SQL Server:
```sql
SELECT a.first_name, a.last_name, f.title
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
JOIN film f ON fa.film_id = f.film_id
ORDER BY 1, 2, 3
FOR XML AUTO, ROOT;
```

표준 SQL:
```sql
SELECT xmlelement(
  NAME root,
  xmlagg(e)
)
FROM (
  SELECT xmlelement(
    NAME a,
    xmlattributes(
      t.first_name AS first_name,
      t.last_name AS last_name
    ),
    xmlagg(xmlelement(
      NAME f,
      xmlattributes(t.title AS title)
    ))
  ) e
  FROM (
    SELECT a.first_name, a.last_name, f.title
    FROM actor a
    JOIN film_actor fa ON a.actor_id = fa.actor_id
    JOIN film f ON fa.film_id = f.film_id
    ORDER BY 1, 2, 3
  ) t
  GROUP BY
    first_name,
    last_name
) t
```

이것은 이전과 같은 일을 하지만, 이전의 루트 `XMLAGG()` 요소를 또 다른 `XMLELEMENT()` 함수 호출로 감쌀 뿐입니다. 이것은 다음을 생성합니다

```xml
<root>
    <a first_name="NICK" last_name="WAHLBERG">
        <f title="SMILE EARRING"/>
        <f title="WARDROBE PHANTOM"/>
    </a>
    <a first_name="PENELOPE" last_name="GUINESS">
        <f title="ACADEMY DINOSAUR"/>
        <f title="ANACONDA CONFESSIONS"/>
    </a>
</root>
```

JSON으로 같은 것을 다시 시도해 봅시다:

SQL Server:
```sql
SELECT a.first_name, a.last_name, f.title
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
JOIN film f ON fa.film_id = f.film_id
ORDER BY 1, 2, 3
FOR JSON AUTO, ROOT;
```

표준 SQL:
```sql
SELECT JSON_OBJECT(KEY 'a' VALUE json_arrayagg(e))
FROM (
  SELECT JSON_OBJECT(
    KEY 'FIRST_NAME' VALUE first_name,
    KEY 'LAST_NAME' VALUE last_name,
    KEY 'F' VALUE JSON_ARRAY_AGG(JSON_OBJECT(
      KEY 'TITLE' VALUE title
      ABSENT ON NULL
    ))
    ABSENT ON NULL
  ) e
  FROM (
    SELECT a.first_name, a.last_name, f.title
    FROM actor a
    JOIN film_actor fa ON a.actor_id = fa.actor_id
    JOIN film f ON fa.film_id = f.film_id
    ORDER BY 1, 2, 3
  ) t
  GROUP BY
    first_name,
    last_name
) t
```

출력:
```json
{
    "a": [
        {
            "first_name": "NICK",
            "last_name": "WAHLBERG",
            "f": [
                {
                    "title": "SMILE EARRING"
                },
                {
                    "title": "WARDROBE PHANTOM"
                }
            ]
        },
        {
            "first_name": "PENELOPE",
            "last_name": "GUINESS",
            "f": [
                {
                    "title": "ACADEMY DINOSAUR"
                },
                {
                    "title": "ANACONDA CONFESSIONS"
                }
            ]
        }
    ]
}
```

### FOR XML AUTO, ELEMENTS

이전처럼, 속성을 생성하는 대신 요소를 생성하기로 결정할 수 있습니다 (XML에서만):

SQL Server:
```sql
SELECT a.first_name, a.last_name, f.title
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
JOIN film f ON fa.film_id = f.film_id
ORDER BY 1, 2, 3
FOR XML AUTO, ELEMENTS;
```

표준 SQL:
```sql
SELECT xmlagg(e)
FROM (
  SELECT xmlelement(
    NAME a,
    xmlelement(
      NAME first_name,
      first_name
    ),
    xmlelement(
      NAME last_name,
      last_name
    ),
    xmlagg(xmlelement(
      NAME f,
      xmlelement(
        NAME title,
        title
      )
    ))
  ) e
  FROM (
    SELECT a.first_name, a.last_name, f.title
    FROM actor a
    JOIN film_actor fa ON a.actor_id = fa.actor_id
    JOIN film f ON fa.film_id = f.film_id
    ORDER BY 1, 2, 3
  ) t
  GROUP BY
    first_name,
    last_name
) t
```

`XMLATTRIBUTES()` 호출 대신 `XMLELEMENT()` 호출 집합이 만들어진다는 점 외에는 많이 변하지 않았습니다. 이것은 다음을 생성합니다

```xml
<a>
    <first_name>NICK</first_name>
    <last_name>WAHLBERG</last_name>
    <f>
        <title>SMILE EARRING</title>
    </f>
    <f>
        <title>WARDROBE PHANTOM</title>
    </f>
</a>
<a>
    <first_name>PENELOPE</first_name>
    <last_name>GUINESS</last_name>
    <f>
        <title>ACADEMY DINOSAUR</title>
    </f>
    <f>
        <title>ANACONDA CONFESSIONS</title>
    </f>
</a>
```

### FOR XML/JSON PATH

`PATH` 전략은 제가 개인적으로 가장 좋아하는 것입니다. 중첩된 XML 또는 JSON 경로 구조를 더 명시적으로 생성하는 데 사용되며, 프로젝션을 함께 그룹화할 때 추가 중첩 수준도 허용합니다. 이것은 예제로 가장 잘 보여집니다. 이제 컬럼에 별칭을 사용하고 있으며, 별칭은 `'/'` (슬래시)를 사용한 XPath 표현식처럼 보입니다:

SQL Server:
```sql
SELECT
  a.first_name AS [author/first_name],
  a.last_name AS [author/last_name],
  f.title
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
JOIN film f ON fa.film_id = f.film_id
ORDER BY 1, 2, 3
FOR XML PATH;
```

표준 SQL:
```sql
SELECT xmlagg(xmlelement(
  NAME row,
  xmlelement(
    NAME author,
    xmlelement(
      NAME first_name,
      "author/first_name"
    ),
    xmlelement(
      NAME last_name,
      "author/last_name"
    )
  ),
  xmlelement(
    NAME title,
    title
  )
))
FROM (
  SELECT
    a.first_name AS "author/first_name",
    a.last_name AS "author/last_name",
    f.title
  FROM actor a
  JOIN film_actor fa ON a.actor_id = fa.actor_id
  JOIN film f ON fa.film_id = f.film_id
  ORDER BY 1, 2, 3
) t
```

관례에 의해 author 관련 컬럼에 대해 `row/author` 요소 아래에 추가 중첩 수준이 생기는 것을 확인하세요:

```xml
<row>
    <author>
        <first_name>NICK</first_name>
        <last_name>WAHLBERG</last_name>
    </author>
    <title>SMILE EARRING</title>
</row>
<row>
    <author>
        <first_name>NICK</first_name>
        <last_name>WAHLBERG</last_name>
    </author>
    <title>WARDROBE PHANTOM</title>
</row>
<row>
    <author>
        <first_name>PENELOPE</first_name>
        <last_name>GUINESS</last_name>
    </author>
    <title>ACADEMY DINOSAUR</title>
</row>
<row>
    <author>
        <first_name>PENELOPE</first_name>
        <last_name>GUINESS</last_name>
    </author>
    <title>ANACONDA CONFESSIONS</title>
</row>
```

이것은 정말 멋집니다! SQL Server 구문은 이 일반적인 사용 사례에 확실히 훨씬 더 편리합니다. JSON으로 같은 것을 다시 시도해 봅시다. 우리가 바꾸는 유일한 것은 이제 슬래시(`'/'`) 대신 점(`'.'`)을 사용하는 JSON-path-ish 구문을 사용한다는 것입니다:

SQL Server:
```sql
SELECT
  a.first_name AS [author.first_name],
  a.last_name AS [author.last_name],
  f.title
FROM actor a
JOIN film_actor fa ON a.actor_id = fa.actor_id
JOIN film f ON fa.film_id = f.film_id
ORDER BY 1, 2, 3
FOR JSON PATH;
```

표준 SQL:
```sql
SELECT JSON_ARRAYAGG(JSON_OBJECT(
  KEY 'author' VALUE JSON_OBJECT(
    KEY 'first_name' VALUE author.first_name,
    KEY 'last_name' VALUE author.last_name
  ),
  KEY 'TITLE' VALUE title
  ABSENT ON NULL
))
FROM (
  SELECT
    a.first_name AS "author.first_name",
    a.last_name AS "author.last_name",
    f.title
  FROM actor a
  JOIN film_actor fa ON a.actor_id = fa.actor_id
  JOIN film f ON fa.film_id = f.film_id
  ORDER BY 1, 2, 3
) t
```

출력:
```json
[
    {
        "author": {
            "first_name": "NICK",
            "last_name": "WAHLBERG"
        },
        "title": "SMILE EARRING"
    },
    {
        "author": {
            "first_name": "NICK",
            "last_name": "WAHLBERG"
        },
        "title": "WARDROBE PHANTOM"
    },
    {
        "author": {
            "first_name": "PENELOPE",
            "last_name": "GUINESS"
        },
        "title": "ACADEMY DINOSAUR"
    },
    {
        "author": {
            "first_name": "PENELOPE",
            "last_name": "GUINESS"
        },
        "title": "ANACONDA CONFESSIONS"
    }
]
```

컬렉션의 중첩을 포함하여 더 정교한 중첩을 위해서는 SQL Server에서 상관 서브쿼리가 필요하며, 이것도 `FOR XML` 또는 `FOR JSON` 구문을 사용합니다.

## 결론

XML과 JSON은 데이터베이스 외부와 내부에서 인기 있는 문서 형식입니다. SQL Server는 대부분의 경우에 가장 편리한 구문을 가지고 있는 반면, 표준 SQL은 훨씬 더 기본적이고, 따라서 더 강력한 구조를 지원합니다. 표준 SQL에서는 거의 모든 종류의 XML 또는 JSON 프로젝션이 가능하며, `XMLTABLE()`과 `JSON_TABLE()`을 사용하면 문서를 다시 SQL 테이블로 변환할 수도 있습니다. 많은 애플리케이션에서 이러한 XML 또는 JSON 기능을 네이티브로 사용하면 훨씬 적은 보일러플레이트 코드로 이어질 것입니다. 많은 애플리케이션이 단지 형식 간에 데이터를 변환하기 위해 데이터베이스와 일부 클라이언트 사이에 미들웨어가 필요하지 않기 때문입니다. 대부분의 ORM은 다양한 이유로 이 기능을 노출하지 않으며, 주된 이유는 악마가 세부 사항에 있기 때문입니다. XML과 JSON 모두 잘 표준화되어 있지만, 구현은 크게 다릅니다:

- SQL/XML 표준은 주로 DB2, Oracle, PostgreSQL에 의해 구현됩니다. 많은 방언이 _일부_ XML 기능을 제공하지만, 표준과 앞의 세 가지만큼 인상적이지는 않습니다. SQL Server는 표준 XML 직렬화에 매우 강력한 `FOR XML`을 가지고 있지만, 엣지 케이스에서는 사용하기 약간 어려울 수 있습니다
- SQL/JSON 표준은 늦게 추가되었고 다시 DB2와 Oracle에 의해 대부분 구현되었지만, MariaDB와 MySQL에서도 점점 더 구현되고 있습니다. PostgreSQL(그리고 그 결과로 CockroachDB 같은 호환 방언들)은 표준과 호환되지 않는 자체 독점 함수와 API를 가지고 있었습니다. 그리고 다시, SQL Server는 표준 직렬화에 잘 작동하지만 엣지 케이스에서는 약간 덜 잘 작동하는 `FOR JSON`을 가지고 있습니다

이러한 기술들은 많은 미묘한 차이 때문에 클라이언트에서 잘 채택되지 않습니다. jOOQ는 핵심 기능을 숨기지 않으면서 수년간 이러한 사소한 차이들을 평준화해 왔습니다. SQL/XML과 SQL/JSON은 jOOQ 3.14(2020년 2분기 출시 예정)를 위한 완벽한 사용 사례이며, 이제 jOOQ Professional 및 Enterprise Edition에서 표준 SQL/XML 및 SQL/JSON 구문과 SQL Server `FOR XML` 및 `FOR JSON` 구문을 모두 사용할 수 있게 됩니다. jOOQ 3.14가 출시되기 전에, 저희 웹사이트에서 현재 기능을 이미 사용해 볼 수 있습니다: https://www.jooq.org/translate

---

게시일: 2020년 5월 5일
작성자: lukaseder
