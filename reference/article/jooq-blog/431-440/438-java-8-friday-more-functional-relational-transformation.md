# Java 8 금요일: 더 많은 함수형 관계형 변환

> 원문: https://blog.jooq.org/java-8-friday-more-functional-relational-transformation/

과거에 우리는 매주 금요일마다 Java 8의 새로운 기능에 관한 글을 제공해 왔습니다. 매우 흥미로운 블로그 시리즈였지만, 이제 우리의 핵심 콘텐츠인 Java와 SQL에 다시 집중하고자 합니다. 앞으로도 가끔 Java 8에 관해 블로깅하겠지만, 더 이상 매주 금요일은 아닙니다(여러분 중 일부는 이미 눈치채셨겠지만). Java 8 금요일 시리즈의 마지막 짧은 글에서, 우리는 미래가 (ORM과 대조적으로) 함수형 관계형 데이터 변환에 속한다고 믿는다는 사실을 다시 강조하고자 합니다.

우리는 이제 약 20년 동안 객체 지향 소프트웨어 개발 패러다임을 사용해 왔습니다. 우리 중 많은 사람들이 이에 대해 매우 독단적이었습니다. 그러나 지난 10년 동안 "새로운" 패러다임이 프로그래밍 커뮤니티에서 점점 더 많은 관심을 받기 시작했습니다: 함수형 프로그래밍입니다. 하지만 함수형 프로그래밍은 그렇게 새로운 것이 아닙니다. Lisp는 매우 초기의 함수형 프로그래밍 언어였습니다. XSLT와 SQL 또한 어느 정도 함수형(그리고 선언적!)입니다. 우리는 SQL의 함수형(그리고 선언적!) 특성의 열렬한 팬이기 때문에, 이제 Java에서 SQL 데이터베이스에서 추출한 테이블 형식 데이터를 변환하는 정교한 도구를 갖게 되어 매우 기쁩니다. 바로 Streams입니다!

## SQL ResultSet은 Stream과 매우 유사합니다

이전에 지적했듯이, JDBC ResultSet과 Java 8 Stream은 매우 유사합니다. jOOQ를 사용할 때 이것은 더욱 사실인데, jOOQ는 JDBC ResultSet을 `org.jooq.Result`로 대체하고, 이것은 `java.util.List`를 확장하므로 자동으로 모든 Streams 기능을 상속받습니다. BOOK과 AUTHOR 레코드 간의 일대다 관계를 가져올 수 있는 다음 쿼리를 살펴보세요:

```java
Map<Record2<String, String>,
    List<Record2<Integer, String>>> booksByAuthor =

// 이 작업은 데이터베이스에서 수행됩니다
// --------------------------------------
ctx.select(
        BOOK.ID,
        BOOK.TITLE,
        AUTHOR.FIRST_NAME,
        AUTHOR.LAST_NAME
    )
   .from(BOOK)
   .join(AUTHOR)
   .on(BOOK.AUTHOR_ID.eq(AUTHOR.ID))
   .orderBy(BOOK.ID)
   .fetch()

// 이 작업은 Java 메모리에서 수행됩니다
// -------------------------------------
   .stream()

   // AUTHOR별로 BOOK을 그룹화합니다
   .collect(groupingBy(

        // 이것이 그룹화 키입니다
        r -> r.into(AUTHOR.FIRST_NAME,
                    AUTHOR.LAST_NAME),

        // 이것이 대상 데이터 구조입니다
        LinkedHashMap::new,

        // 이것이 각 그룹에 대해 생성될 값입니다: BOOK의 리스트
        mapping(
            r -> r.into(BOOK.ID, BOOK.TITLE),
            toList()
        )
    ));
```

Java 8 Streams API의 유창함은 jOOQ로 SQL을 작성하는 데 익숙한 사람에게 매우 자연스럽습니다. 물론 jOOQ 외에 다른 것을 사용할 수도 있습니다. 예를 들어 Spring의 JdbcTemplate, Apache Commons DbUtils, 또는 단순히 JDBC ResultSet을 Iterator로 감싸는 방법이 있습니다... ORM과 비교했을 때 이 접근 방식의 매우 좋은 점은 전혀 마법이 일어나지 않는다는 것입니다. 모든 매핑 로직이 명시적이고, Java 제네릭 덕분에 완전히 타입 안전합니다. 이 예제에서 booksByAuthor 출력의 타입은 복잡하고 읽기/쓰기가 약간 어렵지만, 완전히 설명적이고 유용합니다.

## POJO를 사용한 동일한 함수형 변환

jOOQ의 `Record2` 튜플 타입을 사용하는 것이 마음에 들지 않으신다면, 걱정하지 마세요. 다음과 같이 자신만의 데이터 전송 객체를 지정할 수 있습니다:

```java
class Book {
    public int id;
    public String title;

    @Override
    public String toString() { ... }

    @Override
    public int hashCode() { ... }

    @Override
    public boolean equals(Object obj) { ... }
}

static class Author {
    public String firstName;
    public String lastName;

    @Override
    public String toString() { ... }

    @Override
    public int hashCode() { ... }

    @Override
    public boolean equals(Object obj) { ... }
}
```

위의 DTO를 사용하면, 이제 jOOQ의 내장 POJO 매핑을 활용하여 jOOQ 레코드를 자신만의 도메인 클래스로 변환할 수 있습니다:

```java
Map<Author, List<Book>> booksByAuthor =
ctx.select(
        BOOK.ID,
        BOOK.TITLE,
        AUTHOR.FIRST_NAME,
        AUTHOR.LAST_NAME
    )
   .from(BOOK)
   .join(AUTHOR)
   .on(BOOK.AUTHOR_ID.eq(AUTHOR.ID))
   .orderBy(BOOK.ID)
   .fetch()
   .stream()
   .collect(groupingBy(

        // 이것이 그룹화 키입니다
        r -> r.into(Author.class),
        LinkedHashMap::new,

        // 이것이 그룹화 값 리스트입니다
        mapping(
            r -> r.into(Book.class),
            toList()
        )
    ));
```

## 명시성 vs 암시성

Data Geekery에서, 우리는 Java 개발자들에게 새로운 시대가 시작되었다고 믿습니다. Annotatiomania(어노테이션 광란)가 (드디어!) 끝나고 사람들이 어노테이션 마법을 통한 그 모든 암시적 동작을 가정하는 것을 멈추는 시대 말입니다. ORM은 각 어노테이션이 다른 어노테이션과 어떻게 작동하는지 설명하는 방대한 양의 명세에 의존합니다. JPA가 우리에게 가져다 준 이런 잘 이해되지 않는 어노테이션 언어를 역공학하거나 (디버깅하는 것은!) 어렵습니다. 반면에, SQL은 꽤 잘 이해되어 있습니다. 테이블은 다루기 쉬운 데이터 구조이고, 그 테이블을 좀 더 객체 지향적이거나 좀 더 계층적으로 구조화된 것으로 변환해야 한다면, 단순히 그 테이블에 함수를 적용하고 직접 값을 그룹화하면 됩니다! 명시적으로 그 값들을 그룹화함으로써, 여러분은 매핑을 완전히 제어할 수 있습니다. jOOQ를 사용하면 SQL을 완전히 제어할 수 있는 것처럼 말입니다. 이것이 우리가 향후 5년 안에 ORM이 관련성을 잃고 사람들이 Java 8 Streams를 사용하여 명시적이고, 상태 없고, 마법 없는 데이터 변환 기법을 다시 수용하기 시작할 것이라고 믿는 이유입니다.
