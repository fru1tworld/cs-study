# jOOQ로 Java에서 PL/SQL 프로시저에 접근하기, JPublisher 대안

> 원문: https://blog.jooq.org/access-pl-sql-procedures-from-java-with-jooq-a-jpublisher-alternative/

2014년 11월 4일 | 2022년 10월 20일

저자: lukaseder

---

## 소개

이 글은 Java 개발자들이 Oracle의 폐기된 JPublisher 도구의 우수한 대안으로서 jOOQ를 통해 PL/SQL을 활용하는 방법을 보여줍니다. 저자는 실용적인 예제로 Sakila 데이터베이스(Oracle 포트)를 사용합니다.

## 문제점: PL/SQL에 대한 JDBC의 복잡성

전통적인 JDBC로 PL/SQL에 접근하려면 `java.sql.Struct`와 `java.sql.Array` 객체를 다루는 광범위한 수작업이 필요합니다. 저자는 영화 정보를 조회하는 예제를 통해 이를 설명합니다:

```java
while (rs.next()) {
    Struct film_info_t = (Struct) rs.getObject(1);
    Struct film_t = (Struct) film_info_t.getAttributes()[0];
    String title = (String) film_t.getAttributes()[1];
    Clob description_clob = (Clob) film_t.getAttributes()[2];
    String description = description_clob.getSubString(1,
        (int) description_clob.length());
}
```

### JDBC의 주요 문제점:
- 인덱스를 통한 수동 속성 접근
- 복잡한 중첩 STRUCT 역참조
- 불편한 CLOB 리소스 관리
- 컴파일 타임 안전성 없는 타입 캐스팅
- 복잡한 배열 역직렬화
- STRUCT 연산에 대한 Javadoc이나 타입 힌트 없음

---

## 데이터베이스 스키마 예제

이 글은 Oracle 객체 타입을 사용합니다:

```sql
CREATE TYPE LANGUAGE_T AS OBJECT (
  language_id SMALLINT,
  name CHAR(20),
  last_update DATE
);
/

CREATE TYPE FILM_T AS OBJECT (
  film_id int,
  title VARCHAR(255),
  description CLOB,
  release_year VARCHAR(4),
  language LANGUAGE_T,
  rental_duration SMALLINT,
  rental_rate DECIMAL(4,2),
  length SMALLINT,
  replacement_cost DECIMAL(5,2),
  rating VARCHAR(10),
  special_features VARCHAR(100),
  last_update DATE
);
/

CREATE TYPE FILMS_T AS TABLE OF FILM_T;
/

CREATE TYPE ACTOR_T AS OBJECT (
  actor_id numeric,
  first_name VARCHAR(45),
  last_name VARCHAR(45),
  last_update DATE
);
/

CREATE TYPE ACTORS_T AS TABLE OF ACTOR_T;
/

CREATE TYPE CATEGORY_T AS OBJECT (
  category_id SMALLINT,
  name VARCHAR(25),
  last_update DATE
);
/

CREATE TYPE CATEGORIES_T AS TABLE OF CATEGORY_T;
/

CREATE TYPE FILM_INFO_T AS OBJECT (
  film FILM_T,
  actors ACTORS_T,
  categories CATEGORIES_T
);
/
```

### PL/SQL 패키지 명세:

```sql
CREATE OR REPLACE PACKAGE RENTALS AS
  FUNCTION GET_ACTOR(p_actor_id INT) RETURN ACTOR_T;
  FUNCTION GET_ACTORS RETURN ACTORS_T;
  FUNCTION GET_FILM(p_film_id INT) RETURN FILM_T;
  FUNCTION GET_FILMS RETURN FILMS_T;
  FUNCTION GET_FILM_INFO(p_film_id INT) RETURN FILM_INFO_T;
  FUNCTION GET_FILM_INFO(p_film FILM_T) RETURN FILM_INFO_T;
END RENTALS;
/
```

---

## JDBC 배열 처리

중첩 테이블 타입을 조회하려면 추가적인 복잡성이 필요합니다:

```java
Array actors_t = (Array) film_info_t.getAttributes()[1];
Array categories_t = (Array) film_info_t.getAttributes()[2];

System.out.println("Actors     : ");

Object[] actors = (Object[]) actors_t.getArray();
for (Object actor : actors) {
    Struct actor_t = (Struct) actor;
    System.out.println(
        "  " + actor_t.getAttributes()[1]
       + " " + actor_t.getAttributes()[2]);
}
```

---

## jOOQ 솔루션

jOOQ는 보일러플레이트를 제거하는 생성된 타입 안전 코드를 제공합니다:

```java
FilmInfoTRecord film_info_t = Rentals.getFilmInfo1(
    configuration, new BigInteger("1"));

FilmTRecord film_t = film_info_t.getFilm();

System.out.println("Film       : " + film_t.getTitle());
System.out.println("Description: " + film_t.getDescription());
System.out.println("Language   : " + film_t.getLanguage().getName());

System.out.println("Actors     : ");
for (ActorTRecord actor_t : film_info_t.getActors()) {
    System.out.println(
        "  " + actor_t.getFirstName()
       + " " + actor_t.getLastName());
}

System.out.println("Categories     : ");
for (CategoryTRecord category_t : film_info_t.getCategories()) {
    System.out.println(category_t.getName());
}
```

---

## Java 8을 사용한 고급 jOOQ 예제

이 글은 Java 8 스트림을 사용하여 고객을 필터링하고 대여 이력에 접근하는 더 복잡한 사용법을 보여줍니다:

```java
dsl().select(Rentals.getCustomer(
          CUSTOMER.CUSTOMER_ID
      ))
     .from(CUSTOMER)
     .where(CUSTOMER.FIRST_NAME.eq("JAMIE"))
     .fetch()
     .map(Record1::value1)
     .forEach(customer -> {
         System.out.println("Customer  : ");
         System.out.println("- Name    : "
           + customer.getFirstName()
           + " " + customer.getLastName());
         System.out.println("- E-Mail  : "
           + customer.getEmail());
         System.out.println("- Address : "
           + customer.getAddress().getAddress());
         System.out.println("            "
           + customer.getAddress().getPostalCode()
           + " " + customer.getAddress().getCity().getCity());
         System.out.println("            "
           + customer.getAddress().getCity().getCountry().getCountry());

         CustomerRentalHistoryTRecord history =
           Rentals.getCustomerRentalHistory2(dsl().configuration(), customer);

         System.out.println("  Customer Rental History : ");
         System.out.println("    Films                 : ");

         history.getFilms().forEach(film -> {
             System.out.println("      Film                : "
               + film.getTitle());
             System.out.println("        Language          : "
               + film.getLanguage().getName());
             System.out.println("        Description       : "
               + film.getDescription());

             FilmInfoTRecord info =
               Rentals.getFilmInfo2(dsl().configuration(), film);

             info.getActors().forEach(actor -> {
                 System.out.println("          Actor           : "
                   + actor.getFirstName() + " " + actor.getLastName());
             });

             info.getCategories().forEach(category -> {
                 System.out.println("          Category        : "
                   + category.getName());
             });
         });
     });
```

### 샘플 출력:

```
Customer  :
- Name    : JAMIE RICE
- E-Mail  : JAMIE.RICE@sakilacustomer.org
- Address : 879 Newcastle Way
            90732 Sterling Heights
            United States
  Customer Rental History :
    Films                 :
      Film                : ALASKA PHANTOM
        Language          : English
        Description       : A Fanciful Saga of a Hunter
                            And a Pastry Chef who must
                            Vanquish a Boy in Australia
          Actor           : VAL BOLGER
          Actor           : BURT POSEY
          Actor           : SIDNEY CROWE
          Actor           : SYLVESTER DERN
          Actor           : ALBERT JOHANSSON
          Actor           : GENE MCKELLEN
          Actor           : JEFF SILVERSTONE
          Category        : Music
      Film                : ALONE TRIP
        Language          : English
        Description       : A Fast-Paced Character
                            Study of a Composer And a
                            Dog who must Outgun a Boat
                            in An Abandoned Fun House
          Actor           : ED CHASE
          Actor           : KARL BERRY
          Actor           : UMA WOOD
          Actor           : WOODY JOLIE
          Actor           : SPENCER DEPP
          Actor           : CHRIS DEPP
          Actor           : LAURENCE BULLOCK
          Actor           : RENEE BALL
          Category        : Music
```

---

## jOOQ가 이를 달성하는 방법

매핑 메커니즘에 대한 독자의 질문에 답하면서 저자는 다음과 같이 설명합니다:

"데이터베이스 메타데이터를 역공학하는 소스 코드 생성기가 있습니다. Oracle의 경우, ALL_OBJECTS에서 패키지, 함수, 프로시저를 검색합니다. VARRAY와 TABLE 타입을 찾기 위한 ALL_COLL_TYPES 쿼리와 OBJECT 타입을 찾기 위한 ALL_TYPES 쿼리도 있습니다. 결국, 모든 PL/SQL 코드는 Java API 표현을 갖게 됩니다. 이 방식으로, PL/SQL 코드를 변경할 때마다 해당 Java API도 자동으로 변경되어, 호출 코드가 더 이상 올바르지 않으면 잠재적으로 컴파일 오류가 발생합니다."

---

## JPublisher 맥락

이 글은 Oracle이 Java-PL/SQL 통합을 위한 역사적 솔루션으로 JPublisher를 제공했다고 언급합니다. Oracle 12.2부터 JPublisher는 폐기되었으며, 이로 인해 jOOQ가 선호되는 현대적 대안이 되었습니다.

---

## 언급된 주요 리소스

- Sakila 데이터베이스 (Oracle 포트): https://www.jooq.org/sakila
- jOOQ: https://www.jooq.org
- jOOQ 3.9의 PL/SQL RECORD 타입에 관한 관련 글

---

## 댓글 섹션

한 독자가 jOOQ가 어떤 메서드가 어떤 데이터베이스 연산에 해당하는지 어떻게 식별하는지 물었고, 저자는 Oracle 메타데이터 뷰를 쿼리하는 코드 생성 프로세스에 대한 기술적 설명을 제공했습니다.
