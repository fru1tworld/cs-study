# jOOQ 쿼리를 jOOQ로 실행해야 하는 이유

> 원문: https://blog.jooq.org/why-you-should-execute-jooq-queries-with-jooq/

이전에 이 블로그에서 jOOQ의 코드 생성기 없이도 jOOQ를 사용할 수 있음에도 불구하고, 왜 코드 생성기를 사용해야 하는지 설명하는 글을 작성한 적이 있습니다. 비슷한 맥락에서, Stack Overflow에서 수많은 jOOQ 질문에 답변하면서, 누군가가 jOOQ로 쿼리를 작성한 뒤 다른 곳에서 실행하는 경우를 많이 보았습니다. 예를 들면:

* JPA
* JDBC / R2DBC
* JdbcTemplate (Spring)
* 기타 등등

jOOQ 자체는 특정 방식을 강요하지 않으며 가능한 모든 사용 사례를 수용하려고 합니다. 모든 jOOQ `Query`는 `Query.getSQL()`을 사용하여 SQL을 렌더링할 수 있고, `Query.getBindValues()`로 바인드 값을 생성할 수 있으므로, 원칙적으로 jOOQ 쿼리를 다른 곳에서 실행하는 것은 전혀 가능합니다.

이렇게 하는 것이 유효한 사용 사례는 다음과 같습니다:

* 주로 JPA 기반 애플리케이션에서 2-3개의 동적 쿼리에만 jOOQ를 사용하고, 해당 쿼리로 엔티티(DTO가 아닌)를 가져와야 하는 경우. 매뉴얼의 예시를 참고할 수 있습니다. (만약 수많은 쿼리에 jOOQ를 사용한다면, 애초에 엔티티가 여전히 필요한지 의문을 가지기 시작할 것입니다.)

사실상 이것이 전부입니다. 유효하지 않지만 과대평가된 사용 사례는 다음과 같습니다:

* 다른 모든 것이 여전히 JdbcTemplate 등을 사용하고 있기 때문에 jOOQ로 천천히 마이그레이션하고 싶은 경우. 이것이 왜 jOOQ에서 SQL을 추출하는 좋은 사용 사례가 아닌지 나중에 설명하겠습니다.

이 글에서는 jOOQ로 쿼리를 실행하는 것의 수많은 이점을 예시를 통해 보여주고, 결과적으로 왜 jOOQ를 "올인"해서 사용해야 하는지 설명하고자 합니다.

이 글은 jOOQ로 쿼리를 작성하는 것의 이점은 모두 생략하며, 이미 쿼리 빌딩에 jOOQ가 올바른 선택이라는 결정을 내렸다고 가정합니다.

## 타입 안전성

jOOQ의 주요 이점 중 하나는 SQL을 작성할 때뿐만 아니라 유지보수할 때에도 타입 안전성을 제공한다는 것입니다. 이 중 상당 부분은 jOOQ의 DSL과 코드 생성을 통해 달성되지만, 그것이 전부는 아닙니다. jOOQ로 쿼리를 실행할 때도 타입 안전성의 혜택을 받을 수 있습니다. 예를 들어, 다음은 중첩된 SQL 컬렉션을 Java `Map`으로 타입 안전하게 가져오는 쿼리입니다:

```java
// 대상 데이터 타입입니다
record Film(
    String title,
    Map<LocalDate, BigDecimal> revenue
) {}

// 이 쿼리는 완전히 타입 안전합니다. 변경하면 더 이상 컴파일되지 않습니다
List<Film> result =
ctx.select(
        FILM.TITLE,
        multiset(
            select(
                PAYMENT.PAYMENT_DATE.cast(LOCALDATE),
                sum(PAYMENT.AMOUNT))
            .from(PAYMENT)
            .where(PAYMENT.rental().inventory().FILM_ID
                .eq(FILM.FILM_ID))
            .groupBy(PAYMENT.PAYMENT_DATE.cast(LOCALDATE))
            .orderBy(PAYMENT.PAYMENT_DATE.cast(LOCALDATE))
        )
        // Field<Result<Record2<LocalDate, BigDecimal>>>를
        // Field<Map<LocalDate, BigDecimal>>로 변환
        .convertFrom(r -> r.collect(Records.intoMap())
   )
   .from(FILM)
   .orderBy(FILM.TITLE)

   // Record2<String, Map<LocalDate, BigDecimal>>를
   // List<Film>으로 변환
   .fetch(Records.mapping(Film::new))
```

다시 한번 말하지만, 쿼리 작성 자체가 이미 타입 안전하며 이는 훌륭합니다. 하지만 그 이상으로, 마지막의 `fetch(mapping(Film::new))` 호출도 타입 안전합니다! 이것은 쿼리가 생성하는 `(String, Map<LocalDate, BigDecimal>)` 구조를 준수하는 값을 생성해야 합니다. 더 자세한 정보는 링크된 블로그 게시물에서 확인할 수 있습니다.

다른 어떤 실행 엔진에서도 이 수준의 타입 안전성(및 매핑)을 얻을 수 없습니다. SQL 문자열과 바인드 값을 추출하면, 결과 집합을 알 수 없는 JDBC 수준으로 돌아가게 됩니다:

* JDBC(JdbcTemplate 포함)에서는 모든 `ResultSet` 내용이 매우 일반적(generic)입니다. 컬럼 수를 알 수 없고, 위치를 알 수 없으며, 데이터 타입도 컴파일러에게 알려지지 않습니다.
* JPA의 DTO 가져오기 API에서는 `Object[]`만 얻게 되는데, 이는 JDBC보다 크게 나을 것이 없습니다. 오히려 API조차 제공되지 않기 때문에 JDBC보다 한 단계 후퇴한 것이라고 할 수 있습니다.

jOOQ의 타입 안전성을 항상 사용할 필요는 없으며, 언제든 이를 해제할 수 있지만, 적어도 기본적으로 제공된다는 것이 중요합니다!

### 예시: 리액티브 쿼리

이 타입 안전성의 좋은 예시는 R2DBC를 사용하여 리액티브 쿼리를 실행할 때입니다. R2DBC에서 직접 쿼리를 실행하는 것을 선호하는 사람은 없을 것입니다. jOOQ를 사용하면 쿼리를 예를 들어 reactor `Flux`에 포함시켜 자동 실행과 매핑을 할 수 있기 때문입니다.

```java
record Table(String schema, String table) {}

Flux.from(ctx
        .select(
            INFORMATION_SCHEMA.TABLES.TABLE_SCHEMA,
            INFORMATION_SCHEMA.TABLES.TABLE_NAME)
        .from(INFORMATION_SCHEMA.TABLES))

    // Record2<String, String>에서 Table::new로의 타입 안전한 매핑
    .map(Records.mapping(Table::new))
    .doOnNext(System.out::println)
    .subscribe();
```

## 매핑

이전 예시에서 이미 jOOQ에서 매핑이 자동으로 제공된다는 것을 암시했습니다. jOOQ `Record` 또는 `Record[N]` 타입을 사용자 타입으로 매핑하는 방법은 여러 가지가 있습니다. 가장 널리 사용되는 방법은 다음과 같습니다:

* 리플렉션 기반의 역사적인 `DefaultRecordMapper`로, `Result.into(Class)` API를 사용합니다.
* 위의 예시와 같이 `Record[N]` 타입을 생성자 참조(또는 다른 함수)에 매핑하는 최근에 추가된 타입 안전한 레코드 매퍼.

하지만 레코드 매핑이 전부는 아닙니다. 데이터 타입 변환도 있습니다!

* `Converter`와 `Binding` SPI는 생성된 코드에 첨부하여 항상 자동으로 변환할 수 있습니다. JPA에도 엔티티에 대한 유사한 메커니즘이 있지만, jOOQ SQL 쿼리를 문자열 형태로 추출하면 이러한 것들에 접근할 수 없습니다.
* `ConverterProvider`를 통한 모든 데이터베이스 타입 `T`와 사용자 타입 `U` 간의 자동 타입 변환. 기본 구현은 `Integer`와 `Long`, `LocalDate`와 `String` 등 사이의 변환을 매핑할 수 있습니다. 이것은 JAXB를 사용한 SQL/XML에서 Java 객체로의 매핑이나, Jackson 또는 Gson을 사용한 SQL/JSON에서 Java 객체로의 자동 매핑에 매우 유용합니다! 별도의 설정 없이 바로 작동합니다!
* 임시(Ad-hoc) 변환기는 쿼리별로 데이터베이스 타입(예: 생성된 코드에서)을 사용자 타입으로 타입 안전하게 매핑할 수 있습니다. 이것은 데이터베이스 스키마에 전역적으로 `Converter` 구현을 첨부할 수 없는 경우에 매우 강력합니다. `MULTISET`이나 중첩된 `ROW` 표현식과 함께 사용할 때도 매우 잘 작동합니다.

## 실행 에뮬레이션

일부 SQL 기능은 주로 jOOQ로 쿼리를 실행할 때 런타임에 에뮬레이션됩니다. 여기에는 다음이 포함됩니다:

* `MULTISET` (중첩 컬렉션)
* `ROW` (중첩 레코드)

수많은 jOOQ 사용자들이 사랑하게 된 이러한 기능들은 jOOQ 외부에서는 사용할 수 없습니다. 이러한 쿼리에 대해 생성된 SQL은 방언에 따라 SQL/XML 또는 SQL/JSON을 사용하여 중첩된 컬렉션과 레코드를 인코딩합니다. 물론 자체 데이터 액세스 레이어에서 JSON을 Java 객체로 역직렬화하는 것을 다시 구현할 수도 있지만, 왜 그래야 할까요? jOOQ의 것이 매우 잘 작동하며, 위에서 언급했듯이 타입 안전하기까지 합니다. 직접 다시 구현한다면, 같은 수준의 타입 안전성을 달성하기는 어려울 것입니다.

또 다른 멋진 실행 관련 기능은 다음과 같습니다:

* 배치 `Connection`

이것은 연속된 SQL 문의 배치 처리를 어떤 API 개입 없이 자동으로 에뮬레이션합니다.

## 사용자 정의 타입

서버 측과 클라이언트 측 모두에서 사용자 정의 타입을 사용하고 싶다면, 모든 데이터 타입 바인딩이 jOOQ에 내장되어 있어 바로 사용할 수 있습니다. 예를 들어, PostgreSQL이나 Oracle에서 (문법은 약간 다름):

```sql
CREATE TYPE name AS (
  first_name TEXT,
  last_name TEXT
);

CREATE TABLE user (
  id BIGINT PRIMARY KEY,
  name name NOT NULL
);
```

코드 생성기가 이러한 타입을 자동으로 인식할 뿐만 아니라, 타입 안전한 방식으로 가져올 수도 있습니다:

```java
Result<Record2<Long, NameRecord>> r =
ctx.select(USER.ID, USER.NAME)
   .from(USER)
   .fetch();
```

그런 다음, 당연히 해당 레코드에 타입 안전한 매핑이든 리플렉션 기반 매핑이든 원하는 대로 적용할 수 있습니다. 이러한 UDT 지원이 다른 실행 모드에서는 잘 작동하지 않을 것이라고 생각합니다. 시도해 볼 수는 있습니다. 생성된 UDT 타입은 JDBC의 `SQLData`를 구현하므로, JDBC 문에 바인딩하는 것은 기본적으로 가능해야 합니다. 하지만 여전히 엣지 케이스가 있습니다.

## 저장 프로시저

`OUT` 또는 `IN OUT` 파라미터를 바인딩하는 것은 JDBC, R2DBC 또는 JPA의 하위 수준 API로는 다소 번거롭습니다. 저장 프로시저 호출을 실행할 때도 jOOQ를 사용하는 것이 어떨까요? 다음이 주어졌을 때:

```sql
CREATE OR REPLACE PROCEDURE my_proc (
  i1 NUMBER,
  io1 IN OUT NUMBER,
  o1 OUT NUMBER,
  o2 OUT NUMBER,
  io2 IN OUT NUMBER,
  i2 NUMBER
) IS
BEGIN
  o1 := io1;
  io1 := i1;

  o2 := io2;
  io2 := i2;
END my_proc;
```

어느 쪽을 선호하시나요? 이것(JDBC)?

```java
try (CallableStatement s = c.prepareCall(
    "{ call my_proc(?, ?, ?, ?, ?, ?) }"
)) {

    // 모든 입력 값을 설정
    s.setInt(1, 1); // i1
    s.setInt(2, 2); // io1
    s.setInt(5, 5); // io2
    s.setInt(6, 6); // i2

    // 모든 출력 값의 타입을 등록
    s.registerOutParameter(2, Types.INTEGER); // io1
    s.registerOutParameter(3, Types.INTEGER); // o1
    s.registerOutParameter(4, Types.INTEGER); // o2
    s.registerOutParameter(5, Types.INTEGER); // io2

    s.executeUpdate();

    System.out.println("io1 = " + s.getInt(2));
    System.out.println("o1 = " + s.getInt(3));
    System.out.println("o2 = " + s.getInt(4));
    System.out.println("io2 = " + s.getInt(5));
}
```

아니면 이것?

```java
// 인덱스로 인자를 전달하는 짧은 형태 (타입 안전):
MyProc result = Routines.myProc(configuration, 1, 2, 5, 6);

// 이름으로 인자를 전달하는 명시적 형태 (타입 안전):
MyProc call = new MyProc();
call.setI1(1);
call.setIo1(2);
call.setIo2(5);
call.setI2(6);
call.execute(configuration);

System.out.println("io1 = " + call.getIo1());
System.out.println("o1 = " + call.getO1());
System.out.println("o2 = " + call.getO2());
System.out.println("io2 = " + call.getIo2());
```

사용자 정의 타입을 받거나 반환하는 저장 프로시저를 호출하려고 하면 이 비교는 훨씬 더 명확해집니다.

## ID 값 가져오기

이것은 SQL 방언과 JDBC 드라이버 간에 정말 고통스럽습니다! 일부 SQL 방언은 네이티브 지원을 제공합니다:

* Db2, H2: `FINAL TABLE` (데이터 변경 델타 테이블)
* Firebird, MariaDB, Oracle, PostgreSQL: `RETURNING` (다만 Oracle에서는 많은 과제가 있음)
* SQL Server: `OUTPUT`

그 외에는 종종 여러 쿼리를 실행하거나 대체 JDBC API를 사용해야 합니다. jOOQ가 여러분을 위해 수행하는 고통스러운 작업을 엿보고 싶다면, 여기를 참조하세요.

## 간단한 CRUD

JPA를 사용하는 경우, 이것은 아마도 jOOQ의 킬러 기능은 아닐 것입니다. JPA가 연관 관계 매핑 등에서 jOOQ보다 더 정교한 ORM이기 때문입니다. 하지만 JPA를 사용하지 않는 경우(예: JdbcTemplate이나 JDBC를 직접 사용), 인생의 선택을 의심하면서 `INSERT`, `UPDATE`, `DELETE`, `MERGE` 문을 매우 반복적으로 작성하고 있을 수 있습니다. 그럴 바에 간단히 `UpdatableRecord` API를 사용하여 jOOQ의 CRUD API를 사용하면 됩니다.

수동 DML은 특히 대량 데이터 처리에서 제 역할이 있지만, 그 외에는 어느 쪽을 선호하시나요?

```sql
IF new_record THEN
  INSERT INTO t (a, b, c) VALUES (1, 2, 3) RETURNING id INTO :id;
ELSE
  UPDATE t SET a = 1, b = 2, c = 3 WHERE id = :id;
END IF;
```

아니면 간단히:

```java
t.setA(1);
t.setB(2);
t.setC(3);
t.store();
```

참고로, `TRecord`는 당연히 생성된 것이며, JSON 등에서 가져올 수도 있습니다. 아래를 참조하세요!

## 데이터 임포트 및 익스포트

jOOQ는 다양한 데이터 형식으로의 데이터 임포트/익스포트를 기본적으로 지원합니다:

* XML
* CSV
* JSON
* HTML (익스포트 전용)
* Text (익스포트 전용)
* Charts (익스포트 전용)

## 더 나은 기본값

JDBC와 비교하여, jOOQ는 대부분의 개발자에게 더 나은 기본값을 구현합니다. 이것이 JDBC가 잘못되었다는 의미는 아닙니다. JDBC는 그것이 만들어진 목적, 즉 저수준 네트워크 프로토콜 추상화 SPI에 대해 올바른 선택을 했습니다. jOOQ에 있어서 JDBC를 내부적으로 사용하는 것은 매우 강력했습니다.

하지만 사용자에게 있어서 모든 것이 항상 다음과 같다는 것은 짜증나는 일입니다:

* 리소스가 많이 필요하고
* 지연 로딩(Lazy)

위의 사항은 다음과 같은 결과를 초래합니다:

* `try-with-resources`를 이용한 리소스 관리
* `PreparedStatement`와 같은 리소스의 수동 재사용으로, 유지보수하기 어려운 상태 보존 코드 생성

jOOQ에서는 쿼리가 생성하는 모든 것이 기본적으로 메모리에 즉시(eagerly) 가져와집니다. 이것이 대부분의 사용자가 필요로 하는 기본값이며, 내부적으로 `ResultSet`, `Statement`, `Connection`과 같은 리소스를 더 빠르게 닫을 수 있게 합니다. 물론, 필요하다면 R2DBC를 사용한 리액티브 처리를 포함하여 지연 스트리밍 처리를 선택할 수도 있습니다!

## 그 외 더 많은 것들

언급할 가치가 있는 것들이 더 많이 있습니다:

* jOOQ API를 사용하여 데이터베이스 메타데이터에 쉽게 접근할 수 있습니다.
* jOOQ에서 모킹이 간단합니다 (다만, 아껴서 사용하세요!)
* jOOQ의 진단 기능을 사용할 수 있으며, 곧 훨씬 더 좋아질 것입니다.

## jOOQ 외부에서 실행하는 것의 이점은 거의 없음

약속했듯이, jOOQ 외부에서 실행하는 것의 이점이 거의 없는 이유를 설명하고자 합니다. 단, JPA 엔티티로 데이터를 가져오고 싶은 경우는 예외이며, 이 경우 JPA가 엔티티 라이프사이클을 관리해야 합니다.

하지만 DTO를 가져올 때는 jOOQ 쿼리를 실행하기 위해 JPA를 사용하는 것의 이점이 없습니다. jOOQ로 JPA 관리 트랜잭션에서 직접 쿼리를 실행하는 것은 매우 쉽습니다. 플러싱은 어느 쪽이든 필요하므로, 이점이 없습니다. 그 외에, JPA, JDBC, JdbcTemplate은 다음과 같은 것을 하지 않습니다:

* jOOQ가 동일하게 잘 또는 더 잘할 수 없는 것
* jOOQ가 맞지 않는 것 (트랜잭션, 커넥션 라이프사이클, 매핑 등)

jOOQ는 값 기반 SQL 쿼리를 실행하는 다른 모든 방법의 드롭인 대체품으로 사용할 수 있습니다. 즉, 엔티티가 아닌 DTO로 데이터를 매핑할 때마다 사용할 수 있습니다. 클래식 POJO, Java 16 레코드, Kotlin 데이터 클래스, Scala 케이스 클래스 등을 포함한 모든 대상 데이터 구조로 데이터를 매핑할 수 있으며, 앞서 본 것처럼 XML, JSON, CSV로도 가능합니다.

사실, 이전의 저수준 가져오기 및 매핑 코드에서 jOOQ로 이전하면 엄청난 양의 반복적인 보일러플레이트를 제거하게 될 가능성이 높습니다.

## 결론

jOOQ를 코드 생성과 함께 사용해야 하는 이유에 대한 이전 글과 마찬가지로, 이 글은 쿼리 빌딩뿐만 아니라 jOOQ의 모든 이점을 올인해서 사용하도록 여러분을 설득했을 것입니다. 이러한 기능과 API의 설계에 많은 (정말 많은) 고민이 들어갔습니다. 한번 익숙해지면 수동 배관 작업보다 이것들이 더 낫다는 것을 확신하게 될 것입니다.
