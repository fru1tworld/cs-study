# Java EE와 jOOQ를 사용하는 초보자 가이드

> 원문: https://blog.jooq.org/a-beginners-guide-to-using-java-ee-with-jooq/

Java EE에는 이미 JPA(Java Persistence API)가 포함되어 있습니다. JPA는 데이터베이스 엔티티를 Java 클래스에 매핑하기 위한 것으로, 레코드 지향적(record-oriented) 비즈니스 로직에 가장 적합합니다. Gavin King은 한때 다음과 같이 말했습니다:

> "SQL로 CRUD를 작성하기에는 인생이 너무 짧다"

Gavin은 물론 JPA의 뛰어난 기능 중 하나인 자동 "더티 체킹(dirty checking)"을 언급한 것입니다. 하지만 Gavin은 데이터베이스가 단순한 "CRUD 저장소" 이상의 역할을 한다는 것도 알고 있었습니다.

## CRUD를 넘어서

비즈니스 인텔리전스, 리포팅, 배치/벌크 데이터 처리 또는 비즈니스 로직 자체에 복잡한 쿼리가 포함된 경우, 단순한 CRUD보다 훨씬 더 고급 기능이 필요합니다. 흥미롭게도 이에 대해 JPQL(Java Persistence Query Language)과 Criteria API가 있습니다. 하지만 이러한 도구들에도 한계가 있습니다.

Michael Simons가 작성한 "Criteria API에서 jOOQ로" 마이그레이션에 대한 비교 글을 살펴보시면 좋습니다. 결국 JPA의 네이티브 쿼리 API를 사용하게 되는 경우가 많습니다.

## jOOQ는 무엇인가요?

jOOQ는 본질적으로 타입 안전한 JDBC입니다. 그 이상도 그 이하도 아닙니다. jOOQ는 어떤 트랜잭션 모델이나 세션 모델도 강제하지 않습니다. jOOQ를 JPA의 대체품으로 생각하지 않는 것이 좋습니다. 대신, 다음과 같은 경우에 jOOQ를 사용하세요:

- 리포팅 쿼리
- 배치 및 벌크 데이터 처리
- 복잡한 비즈니스 규칙을 포함하는 쿼리

## Java EE에서 jOOQ 사용하기

Java EE에서 jOOQ를 사용하는 것은 정말 간단합니다. EJB(Enterprise JavaBeans)를 사용해 세션과 범위 관리를, CDI(Contexts and Dependency Injection)를 사용해 의존성 주입을, jOOQ를 사용해 데이터베이스 상호작용을 처리하면 됩니다.

### 표준 JDBC 사용 예제

먼저 DataSource를 EJB에 주입하는 방법을 살펴보겠습니다:

```java
@Stateless
public class LibraryEJB {

    @Resource(lookup="java:data-source-configuration")
    private DataSource ds;

    public List<Author> fetchAuthors() throws SQLException {
        List<Author> result = new ArrayList<>();

        // JDBC를 사용한 전통적인 방식
        try (Connection con = ds.getConnection();
             PreparedStatement stmt = con.prepareStatement(
                 "SELECT * FROM AUTHOR ORDER BY ID");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                result.add(new Author(
                    rs.getInt("ID"),
                    rs.getString("FIRST_NAME"),
                    rs.getString("LAST_NAME")
                ));
            }
        }

        return result;
    }
}
```

위 코드는 표준 JDBC를 사용하여 작성되었습니다. 연결(Connection), 명령문(PreparedStatement), 결과 집합(ResultSet)을 수동으로 관리해야 합니다. try-with-resources를 사용하더라도 여전히 상당한 양의 보일러플레이트 코드가 필요합니다.

### jOOQ 사용 예제

이제 동일한 작업을 jOOQ로 수행하는 방법을 살펴보겠습니다:

```java
@Stateless
public class LibraryEJB {

    @Resource(lookup="java:data-source-configuration")
    private DataSource ds;

    public Result<AuthorRecord> fetchAuthors() {
        // jOOQ를 사용한 간결한 방식
        return DSL.using(ds, H2)
                  .selectFrom(AUTHOR)
                  .orderBy(AUTHOR.ID)
                  .fetch();
    }
}
```

코드가 훨씬 간결해졌습니다! jOOQ는 다음과 같은 장점을 제공합니다:

1. 자동 리소스 관리: jOOQ는 연결과 명령문을 자동으로 닫습니다. 수동으로 관리할 필요가 없습니다.

2. 타입 안전성: 컴파일 시점에 SQL 오류를 잡을 수 있습니다. `AUTHOR` 테이블과 `AUTHOR.ID` 컬럼은 jOOQ의 코드 생성기가 생성한 타입 안전한 참조입니다.

3. SQL에 가까운 문법: jOOQ의 DSL은 실제 SQL과 매우 유사하게 읽힙니다. SQL을 알고 있다면 jOOQ를 쉽게 배울 수 있습니다.

## 트랜잭션 관리

Java EE에서 트랜잭션은 일반적으로 컨테이너가 관리합니다. EJB의 경우 기본적으로 컨테이너 관리 트랜잭션(CMT)이 적용됩니다. jOOQ는 이러한 트랜잭션 모델과 완벽하게 호환됩니다.

jOOQ에 DataSource를 전달하면, jOOQ는 DataSource에서 연결을 가져옵니다. Java EE 환경에서 이 DataSource는 일반적으로 JTA(Java Transaction API) 트랜잭션에 참여하는 연결을 반환합니다. 따라서 별도의 트랜잭션 관리 코드가 필요하지 않습니다.

```java
@Stateless
public class LibraryEJB {

    @Resource(lookup="java:data-source-configuration")
    private DataSource ds;

    // 이 메서드는 자동으로 트랜잭션 내에서 실행됩니다
    public void updateAuthor(int id, String firstName, String lastName) {
        DSL.using(ds, H2)
           .update(AUTHOR)
           .set(AUTHOR.FIRST_NAME, firstName)
           .set(AUTHOR.LAST_NAME, lastName)
           .where(AUTHOR.ID.eq(id))
           .execute();
    }
}
```

## WildFly에서의 예제

WildFly(이전의 JBoss AS)에서 jOOQ를 사용하는 실제 작동하는 예제가 GitHub에 있습니다. 이 예제는 Arun Gupta와 함께 진행한 웨비나를 위해 만들어졌으며, 다음 주제들을 다룹니다:

- jOOQ의 목적과 기능
- JDBC 및 JPA와의 비교
- Java EE 애플리케이션과의 통합
- 성능 및 확장성 고려사항
- CRUD 대 도메인이 풍부한 애플리케이션에서의 적합성

## jOOQ와 JPA 함께 사용하기

jOOQ와 JPA를 함께 사용할 수도 있습니다. 간단한 CRUD 작업에는 JPA를 사용하고, 복잡한 쿼리에는 jOOQ를 사용하는 것이 좋은 전략입니다.

```java
@Stateless
public class LibraryEJB {

    @PersistenceContext
    private EntityManager em;

    @Resource(lookup="java:data-source-configuration")
    private DataSource ds;

    // JPA를 사용한 간단한 CRUD
    public Author findAuthor(int id) {
        return em.find(Author.class, id);
    }

    public void saveAuthor(Author author) {
        em.persist(author);
    }

    // jOOQ를 사용한 복잡한 쿼리
    public Result<Record> fetchAuthorsWithBookCount() {
        return DSL.using(ds, H2)
                  .select(AUTHOR.ID,
                          AUTHOR.FIRST_NAME,
                          AUTHOR.LAST_NAME,
                          count(BOOK.ID).as("book_count"))
                  .from(AUTHOR)
                  .leftJoin(BOOK).on(BOOK.AUTHOR_ID.eq(AUTHOR.ID))
                  .groupBy(AUTHOR.ID, AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME)
                  .orderBy(count(BOOK.ID).desc())
                  .fetch();
    }
}
```

## 결론

Java EE와 jOOQ는 완벽하게 조화를 이룹니다. Java EE가 세션, 트랜잭션, 의존성 주입을 처리하는 동안, jOOQ는 데이터베이스와의 타입 안전한 상호작용을 담당합니다. JPA가 간단한 CRUD에 적합하다면, jOOQ는 복잡한 쿼리에 빛을 발합니다.

jOOQ를 Java EE 프로젝트에 도입하면 다음과 같은 이점을 얻을 수 있습니다:

- 타입 안전성: SQL 오류를 런타임이 아닌 컴파일 시점에 발견
- 생산성 향상: 보일러플레이트 코드 감소
- 유지보수성: SQL에 가까운 문법으로 코드 가독성 향상
- 유연성: JPA와 함께 사용하여 각 도구의 장점 활용

인생은 CRUD를 SQL로 작성하기에는 짧을 수 있지만, 복잡한 쿼리를 안전하고 효율적으로 작성하는 것은 그만한 가치가 있습니다. jOOQ가 바로 그것을 가능하게 해줍니다.
