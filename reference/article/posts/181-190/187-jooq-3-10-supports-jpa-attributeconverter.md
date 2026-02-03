# jOOQ 3.10은 JPA AttributeConverter를 지원한다

> 원문: https://blog.jooq.org/jooq-3-10-supports-jpa-attributeconverter/

jOOQ의 숨겨진 멋진 기능 중 하나는 `JPADatabase`인데, 이 기능을 사용하면 기존에 작성된 JPA 애노테이션이 붙은 엔티티 집합을 리버스 엔지니어링하여 jOOQ 코드를 생성할 수 있다. 예를 들어, 다음과 같은 엔티티들을 작성할 수 있다:

```java
@Entity
public class Actor {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    public Integer actorId;

    @Column
    public String firstName;

    @Column
    public String lastName;

    @ManyToMany(fetch = LAZY, mappedBy = "actors",
        cascade = CascadeType.ALL)
    public Set<Film> films = new HashSet<>();

    public Actor(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}

@Entity
public class Film {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    public Integer filmId;

    @Column
    public String title;

    @Column(name = "RELEASE_YEAR")
    @Convert(converter = YearConverter.class)
    public Year releaseYear;

    @ManyToMany(fetch = LAZY, cascade = CascadeType.ALL)
    public Set<Actor> actors = new HashSet<>();

    public Film(String title, Year releaseYear) {
        this.title = title;
        this.releaseYear = releaseYear;
    }
}
```

(단순한 예제일 뿐이다. `@ManyToMany` 매핑의 주의사항에 대해서는 논하지 않겠다). 더 자세한 정보를 원한다면, 전체 예제는 Github에서 찾을 수 있다:

- https://github.com/jOOQ/jOOQ/tree/master/jOOQ-examples/jOOQ-jpa-example-entities
- https://github.com/jOOQ/jOOQ/tree/master/jOOQ-examples/jOOQ-jpa-example

이제 `RELEASE_YEAR` 컬럼의 데이터베이스 타입 `INT`를 편의를 위해 멋진 JSR-310 `java.time.Year` 타입으로 매핑하는 수고를 들였다는 사실에 주목하자. 이것은 JPA 2.1의 `AttributeConverter`를 사용하여 수행되었으며, 다음과 같이 간단하게 생겼다:

```java
public class YearConverter
implements AttributeConverter<Year, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Year attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public Year convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : Year.of(dbData);
    }
}
```

## jOOQ의 JPADatabase 사용하기

이제 jOOQ의 JPADatabase를 사용하면 입력 엔티티들(예: 패키지 이름)을 간단히 설정하고 거기서 jOOQ 코드를 생성할 수 있다. 이것은 뒤에서 다음과 같은 알고리즘으로 동작한다:

- Spring이 클래스패스에서 애노테이션이 붙은 모든 엔티티를 발견하는 데 사용된다
- Hibernate가 해당 엔티티들로부터 인메모리 H2 데이터베이스를 생성하는 데 사용된다
- jOOQ가 이 H2 데이터베이스를 다시 리버스 엔지니어링하여 jOOQ 코드를 생성하는 데 사용된다

이것은 대부분의 사용 사례에서 꽤 잘 동작하는데, JPA 애노테이션이 붙은 엔티티들은 이미 매우 벤더에 독립적이며 많은 벤더 특화 기능에 대한 접근을 제공하지 않기 때문이다. 따라서 우리는 jOOQ로 다음과 같은 종류의 쿼리를 완벽하게 쉽게 작성할 수 있다:

```java
ctx.select(
        ACTOR.FIRSTNAME,
        ACTOR.LASTNAME,
        count().as("Total"),
        count().filterWhere(LANGUAGE.NAME.eq("English"))
          .as("English"),
        count().filterWhere(LANGUAGE.NAME.eq("German"))
          .as("German"),
        min(FILM.RELEASE_YEAR),
        max(FILM.RELEASE_YEAR))
   .from(ACTOR)
   .join(FILM_ACTOR)
     .on(ACTOR.ACTORID.eq(FILM_ACTOR.ACTORS_ACTORID))
   .join(FILM)
     .on(FILM.FILMID.eq(FILM_ACTOR.FILMS_FILMID))
   .join(LANGUAGE)
     .on(FILM.LANGUAGE_LANGUAGEID.eq(LANGUAGE.LANGUAGEID))
   .groupBy(
        ACTOR.ACTORID,
        ACTOR.FIRSTNAME,
        ACTOR.LASTNAME)
   .orderBy(ACTOR.FIRSTNAME, ACTOR.LASTNAME, ACTOR.ACTORID)
   .fetch()
```

(멋진 FILTER 절에 대한 더 자세한 정보는 여기를 참조) 이 예제에서는 기사에서 생략한 `LANGUAGE` 테이블도 사용하고 있다. 위 쿼리의 출력은 대략 다음과 같다:

```
+---------+---------+-----+-------+------+----+----+
|FIRSTNAME|LASTNAME |Total|English|German|min |max |
+---------+---------+-----+-------+------+----+----+
|Daryl    |Hannah   |    1|      1|     0|2015|2015|
|David    |Carradine|    1|      1|     0|2015|2015|
|Michael  |Angarano |    1|      0|     1|2017|2017|
|Reece    |Thompson |    1|      0|     1|2017|2017|
|Uma      |Thurman  |    2|      1|     1|2015|2017|
+---------+---------+-----+-------+------+----+----+
```

보다시피, 이것은 jOOQ와 JPA의 매우 적합한 조합이다. JPA는 JPA의 유용한 객체 그래프 영속화 기능을 통해 데이터를 삽입하는 데 사용되었고, jOOQ는 동일한 테이블에서 리포팅하는 데 사용되었다. 이제 우리가 이미 이 멋진 `AttributeConverter`를 작성했으므로, 추가적인 노력 없이 jOOQ 쿼리에도 적용하고 jOOQ에서도 `java.time.Year` 데이터 타입을 얻고 싶을 것이다.

## jOOQ 3.10 자동 변환

jOOQ 3.10에서는 더 이상 아무것도 할 필요가 없다. 기존 JPA 컨버터가 자동으로 jOOQ 컨버터로 매핑되며, 생성된 jOOQ 코드는 다음과 같다:

```java
// 이 생성된 코드에 대해 걱정하지 마라
public final TableField<FilmRecord, Year> RELEASE_YEAR =
    createField("RELEASE_YEAR", org.jooq.impl.SQLDataType.INTEGER,
        this, "", new JPAConverter(YearConverter.class));
```

... 이로 인해 이전 jOOQ 쿼리가 이제 다음과 같은 타입을 반환한다:

```
Record7<String, String, Integer, Integer, Integer, Year, Year>
```

다행히도, Hibernate 메타 모델이 엔티티와 테이블 간의 바인딩을 매우 편리하게 탐색할 수 있게 해주기 때문에 이것은 구현하기 상당히 쉬웠다. 다음 기사에서 설명한 것처럼 말이다: https://vladmihalcea.com/2017/08/24/how-to-get-the-entity-mapping-to-database-table-binding-metadata-from-hibernate/

jOOQ 3.11에서는 더 많은 유사한 기능이 제공될 예정인데, 예를 들어 JPA `@Embedded` 타입의 리버스 엔지니어링도 살펴볼 것이다. https://github.com/jOOQ/jOOQ/issues/6518 를 참조하라.

이 예제를 실행해 보고 싶다면, GitHub에서 jOOQ/JPA 예제를 확인해 보라:

- https://github.com/jOOQ/jOOQ/tree/master/jOOQ-examples/jOOQ-jpa-example-entities
- https://github.com/jOOQ/jOOQ/tree/master/jOOQ-examples/jOOQ-jpa-example
