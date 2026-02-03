# JPA의 네이티브 쿼리나 @Formula에서 jOOQ로 벤더 독립적 SQL 작성하기

> 원문: https://blog.jooq.org/using-jooq-to-write-vendor-agnostic-sql-with-jpas-native-query-or-formula/

JPA를 사용하면서 가끔 네이티브 쿼리나 Hibernate의 `@Formula` 어노테이션, 또는 Spring Data의 `@Query(nativeQuery = true)` 기능을 통해 벤더 종속적인 SQL을 작성해야 할 때가 있습니다. 이러한 레거시 JPA 애플리케이션은 종종 데이터베이스별 SQL 구문을 포함하고 있어, 다른 데이터베이스 시스템으로 마이그레이션할 때 이식성 문제가 발생합니다.

## 문제점

예를 들어, Oracle의 `NVL()` 함수를 사용하는 네이티브 쿼리가 있다고 가정해 봅시다. 이 함수는 MariaDB에서는 작동하지만, MySQL이나 SQL Server에서는 "인식할 수 없는 함수" 오류가 발생합니다. 마찬가지로 테이블 별칭에 사용되는 `AS` 키워드도 데이터베이스마다 다르게 작동합니다.

```java
// Oracle 특화 SQL - 다른 데이터베이스에서는 작동하지 않을 수 있음
entityManager.createNativeQuery(
    "SELECT NVL(a.middle_name, 'N/A') AS middle_name FROM author AS a"
);
```

## 해결책: 파싱 커넥션

jOOQ는 이 문제를 해결하기 위한 파싱 커넥션(Parsing Connection) 기능을 제공합니다. 이것은 "실제 커넥션에 대한 프록시로, JDBC 레벨에서 모든 SQL 구문을 가로채서 대상 다이얼렉트(dialect)로 변환합니다."

구현 방법은 매우 간단합니다:

```java
DataSource originalDataSource = ...;

// 대상 다이얼렉트로 SQL을 자동 변환하는 파싱 DataSource 생성
DataSource parsingDataSource = DSL
    .using(originalDataSource, dialect)
    .parsingDataSource();
```

파싱 커넥션은 자동으로:
- `NVL()`을 MySQL에서는 `IFNULL()`로, SQL Server에서는 `COALESCE()`로 변환합니다
- 불필요한 `AS` 키워드와 같은 호환되지 않는 구문 요소를 제거합니다

## 실용적인 활용 사례

이 기능은 다음과 함께 작동합니다:

### JPA의 createNativeQuery()

```java
// jOOQ 파싱 커넥션을 통해 자동으로 대상 데이터베이스에 맞게 변환됨
Query query = entityManager.createNativeQuery(
    "SELECT NVL(a.first_name, 'Unknown') FROM author AS a"
);
```

### Hibernate의 @Formula 어노테이션

```java
@Entity
public class Author {
    @Id
    private Long id;

    private Integer yearOfBirth;

    // 이 Formula는 jOOQ에 의해 자동으로 대상 다이얼렉트에 맞게 변환됨
    @Formula("year_of_birth between 1981 and 1996")
    private Boolean isMillennial;
}
```

위의 불리언 표현식은 다이얼렉트별로 적절히 변환됩니다. 예를 들어 SQL Server에서는 NULL-safe한 CASE 문으로 변환됩니다.

### Spring Data의 @Query 어노테이션

```java
public interface AuthorRepository extends JpaRepository<Author, Long> {

    // 네이티브 쿼리도 파싱 커넥션을 통해 투명하게 처리됨
    @Query(value = "SELECT NVL(a.bio, 'No bio available') FROM author a",
           nativeQuery = true)
    List<String> findAllBios();
}
```

파싱 커넥션은 Spring Data의 네이티브 쿼리를 데이터베이스별로 별도의 리포지토리 구현 없이 투명하게 처리합니다.

## 성능 고려사항

파싱 커넥션은 동일한 쿼리의 반복 변환 오버헤드를 방지하기 위해 LRU 캐시(기본값 8192개 항목)를 사용합니다. 따라서 동일한 SQL 구문이 여러 번 실행되더라도 파싱은 한 번만 수행됩니다.

## 고급 커스터마이징: ParseListener SPI

지원되지 않는 SQL 기능의 경우, `ParseListener` SPI를 구현하여 커스텀 다이얼렉트별 술어(predicate)를 처리하고 데이터베이스 시스템 간에 적절히 변환할 수 있습니다.

예를 들어, MySQL의 `LOGICAL_XOR`과 같은 벤더별 구조체에 대한 변환 규칙을 정의할 수 있습니다:

```java
DSLContext ctx = DSL.using(connection, SQLDialect.MYSQL);
ctx.configuration().set(new DefaultParseListener() {
    @Override
    public Field<?> parseField(ParseContext parseContext) {
        // 커스텀 함수나 구문에 대한 파싱 로직 구현
        return super.parseField(parseContext);
    }
});
```

## 결론

jOOQ의 파싱 레이어를 도입하면 점진적인 마이그레이션의 이점을 얻을 수 있습니다. 전체 DSL을 도입하지 않고도 SQL 표준화를 달성할 수 있습니다. 이는 특히 레거시 JPA 애플리케이션에서 데이터베이스 독립성을 확보하면서도 기존 코드를 크게 변경하지 않아도 되는 실용적인 접근 방식입니다.

기존 JPA 애플리케이션을 다른 데이터베이스로 마이그레이션해야 하거나, 멀티 데이터베이스 환경을 지원해야 하는 경우, jOOQ의 파싱 커넥션은 매우 유용한 도구가 될 것입니다.
