# 타입 안전 쿼리 DSL을 위한 고급 Java 트릭

> 원문: https://blog.jooq.org/advanced-java-trickery-for-typesafe-query-dsls/

2014년 1월 9일 lukaseder 작성

Hacker News를 탐색하다가 Benji Weber가 Java 8로 타입 안전한 데이터베이스 상호작용을 구현하려는 흥미로운 시도를 발견했습니다. Benji는 jOOQ와 유사한 타입 안전 쿼리 DSL을 만들었지만, 핵심적인 차이점이 있습니다. Java 8 메서드 참조를 사용하여 POJO를 내부 분석(introspect)하고 쿼리 요소를 추론합니다.

## 메서드 참조 방식의 코드 예제

```java
Optional<Person> person =
    from(Person.class)
        .where(Person::getFirstName)
        .like("%ji")
        .and(Person::getLastName)
        .equalTo("weber")
        .select(
            personMapper,
            connectionFactory::openConnection);
```

이것은 다음과 같은 SQL로 변환됩니다:

```sql
SELECT * FROM person
WHERE first_name LIKE ? AND last_name = ?
```

## 역사적 맥락

유사한 접근 방식이 이전에 다음 프로젝트들에서 구현되었습니다:

- JaQu: H2 데이터베이스 관리자인 Thomas Muller가 만든 프로젝트로, 바이트코드 내부 분석을 사용하여 Java boolean 표현식을 SQL로 변환합니다.
- LambdaJ: Java 8 이전에 Java에 람다 표현식을 도입하려는 시도
- OhmDB: 유연한 쿼리 DSL을 갖춘 NoSQL 데이터 저장소

## JaQu의 바이트코드 내부 분석 예제

```java
Timestamp ts =
  Timestamp.valueOf("2005-05-05 05:05:05");
Time t = Time.valueOf("23:23:23");

long count = db.from(co).
    where(new Filter() { public boolean where() {
        return co.id == x
            && co.name.equals(name)
            && co.value == new BigDecimal("1")
            && co.amount == 1L
            && co.birthday.before(new Date())
            && co.created.before(ts)
            && co.time.before(t);
        } }).selectCount();
```

핵심적인 혁신은 CGLIB와 바이트코드 인스트루멘테이션에 의존하는 대신 Java 8의 메서드 참조를 활용한다는 점입니다.

## 저자의 평가

저자는 이러한 접근 방식에 대해 회의적인 시각을 표명합니다. 언어 및 바이트코드 변환이 견고한 결과를 가져오지 못할 것이라고 언급합니다. 여기에는 다양한 기술 블로그에서 Hibernate의 프록시 메커니즘에 대한 비판이 참조됩니다. 대신 jOOQ는 API 소비자가 무슨 일이 일어나고 있는지 완전히 제어할 수 있는 WYSIWYG(What You See Is What You Get) 접근 방식을 선호합니다.

이러한 "영리한 아이디어"에 대한 독자들의 의견을 묻는 것으로 글이 마무리됩니다.
