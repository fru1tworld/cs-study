# jOOQ에 대해 몰랐을 수 있는 5가지

> 원문: https://blog.jooq.org/5-things-you-may-not-have-known-about-jooq/

jOOQ는 이제 꽤 오랜 시간(2009년부터!) 존재해왔고, 이제 우리는 SQL과 Java 언어에 대해 꽤 많은 것들을 경험했다고 말할 수 있습니다.

여기 jOOQ에 대해 알지 못했을 수 있는 몇 가지 사항들이 있습니다. jOOQ의 핵심 디자인 철학을 반영하는 것들입니다.

## 1. 모든 컬럼 타입은 Nullable이다

처음에는 예를 들어 `Option(al)`을 통해 null 가능성을 인코딩하는 것이 합리적으로 보일 수 있지만, 무언가를 outer join하는 순간 이는 깨져버립니다.

다음 쿼리를 고려해보세요:

```sql
SELECT p.*
FROM dual
LEFT JOIN person p ON p.first_name = 'Ooops, no one by that name'
```

위 쿼리는 모든 컬럼에 NULL 값만 있는 단일 person 레코드를 생성합니다. `NOT NULL` 제약 조건에도 불구하고 말입니다. 이런! non-optional 타입에서 null을 받게 됩니다.

비슷한 일이 union, grouping set, 함수, 그리고 몇 가지 다른 연산에서도 발생할 수 있습니다. SQL에서 모든 타입은 항상 nullable입니다. 우리는 단순히 이것을 받아들여야 합니다.

모든 영리한 타입 안전성은 SQL 논리에 반합니다. 만약 여러분의 API가 이렇게 한다면, 사용 사례의 80%에서 약간의 편의성을 얻는 대신 20%의 사용 사례에서 큰 불편함을 겪게 될 것입니다. Java에서 모든 비원시(non-primitive) 타입도 nullable이므로, 우리는 완벽하고 직관적인 매칭을 갖고 있다는 점을 고려하면 이는 합리적인 트레이드오프가 아닙니다.

만약 이 제약 조건 정보가 Java 클래스에 주석으로 달려야 한다면, JPA의 `@Column(nullable=true)` 주석은 타입에 대한 어떤 함의도 없이 단순히 제약 조건에 매핑되기 때문에 허용됩니다. 함의는 영속성 동작에 적용되며, 이는 합리적입니다.

## 2. SQL은 값(Values) 기반이지, 아이덴티티(Identity) 기반이 아니다

SQL에서 모든 것은 값입니다. 아이덴티티는 없습니다.

이것은 필요하지 않습니다. 왜냐하면 SQL은 multiset 기반 언어이기 때문입니다. 우리는 CRUD 연산이 다르게 생각하게 만들 수 있지만, 항상 개별 레코드가 아닌 전체 데이터셋에 대해 작업합니다.

SQL은 관계 대수학과 달리 집합(set)이 아닌 백(bag) 또는 multiset에 대해 작업합니다. 즉, 중복 값을 허용하는 데이터 구조입니다. Multiset은 분석을 훨씬 더 강력하게 만들지만, OLTP는 더 어렵게 만듭니다.

jOOQ를 사용할 때는 값 기반의 레코드 중심 프로그래밍 접근 방식이 권장됩니다. jOOQ 쿼리의 결과 집합은 정말로 "그저" 레코드의 스트림이며, 이 스트림의 요소들을 다시 영속화하는 것에 대해 전혀 생각하지 않고 추가로 변환됩니다.

jOOQ(또는 SQL) 쓰기 연산도 레코드(값)를 데이터베이스로 다시 스트리밍하는 multiset 기반입니다.

이것은 Hibernate/JPA가 하는 것과 완전히 반대입니다. Hibernate는 객체-그래프 영속성 API이기 때문에 기본 키를 통해 엔티티 아이덴티티를 에뮬레이션해야 합니다.

jOOQ는 테이블과 "값 기반" 레코드를 프로그래밍 모델의 중심에 둠으로써 이러한 사고방식을 장려합니다.

## 3. ResultQuery는 Iterable이다

jOOQ의 `ResultQuery`는 `Iterable`입니다. 이는 "foreach"를 사용할 수 있다는 것을 의미합니다:

```java
ResultQuery<?> query = DSL.using(configuration)
    .select(PERSON.FIRST_NAME, PERSON.LAST_NAME)
    .from(PERSON);

// Java 5 스타일
for (Record record : query)
    System.out.println(record);

// Java 8 스타일
query.forEach(System.out::println);
```

이것이 의미가 있는 이유는 SQL 쿼리가 튜플 집합의 설명이기 때문입니다. SQL은 함수형 프로그래밍 언어이며, 일부 동시성 측면을 잊는다면 원칙적으로 부작용이 없습니다. 이는 쿼리가 정말로 튜플의 집합이라는 것을 의미합니다. 이 생각을 염두에 두면, 우리는 단순히 그것을 반복할 수 있습니다.

## 4. 순서가 지정되면 순서가 유지된다

기본적으로 jOOQ는 `Result` 타입 또는 `List` 타입을 반환하지만, `ResultQuery.fetchMap()` 메서드와 같은 많은 유틸리티 메서드가 있습니다:

```java
Map<Integer, String> people = DSL.using(configuration)
    .select(PERSON.ID, PERSON.FIRST_NAME)
    .from(PERSON)
    .orderBy(PERSON.ID)
    .fetchMap(PERSON.ID, PERSON.FIRST_NAME);
```

내부적으로 jOOQ는 모든 데이터를 `LinkedHashMap`에 수집합니다. 이는 유사한 `HashMap`보다 약간 더 리소스 집약적인 맵입니다. 자주 사용하지 않았다면, `Map.entrySet()` 및 다른 모든 메서드를 사용하여 맵을 반복할 때 삽입 순서를 유지하는 맵입니다.

이것은 맵을 표시할 때 매우 유용합니다. 결국, 순서를 지정했다면 결과에서 그 순서가 나타나기를 원했던 것입니다.

정렬은 비용이 많이 드는 연산(O(n log n))입니다. 개발자가 이 비용을 들였다면, 후속 데이터 변환 전체에서 그 순서가 유지되기를 의도한 것입니다.

## 5. 동적 SQL이 기본이다

여기서 흥미로운 점은 jOOQ 쿼리가 항상 동적 SQL 쿼리라는 것입니다. 다음은 이에 대한 예시입니다:

```java
for (PersonRecord rec : DSL.using(configuration)
        .selectFrom(person)
        .where(active
            ? PERSON.ACTIVE.isTrue()
            : trueCondition()))
    out.println(rec.getFirstName() + " " + rec.getLastName());
```

위 접근 방식은 특정 술어(predicate)를 문장에 추가해야 하는지 여부를 결정하기 위해 인라인 표현식을 사용합니다. 만약 그 술어가 더 복잡해진다면, 술어의 구성을 지역 변수나 함수로 추출할 수 있습니다:

```java
Condition condition = trueCondition();

if (active)
    condition = PERSON.ACTIVE.eq(active);

if (searchForFirstName)
    condition = condition.and(PERSON.FIRST_NAME.like(pattern));

for (PersonRecord rec : DSL.using(configuration)
        .selectFrom(person)
        .where(condition))
    out.println(rec.getFirstName() + " " + rec.getLastName());
```

상황이 더욱 복잡해지면, 로직을 메서드나 함수로 분리하는 것을 좋아할 수 있습니다.

jOOQ와 같은 타입 안전 임베디드 DSL은 동적 SQL에 매우 강력합니다. 왜냐하면 jOOQ DSL로 구성하는 쿼리는 본질적으로 동적 쿼리이기 때문입니다. 편리한 API("DSL")를 사용하여 쿼리 표현식 트리를 구성하고 있으며, SQL 문이 정적이라고 생각하더라도 마찬가지입니다.

배후에서 구축되는 표현식 트리는 런타임에 복잡한 변환에 사용할 수 있습니다. 예를 들어 특정 쿼리에 행 수준 보안을 적용하거나, 스키마 기반 멀티테넌시를 적용하는 것입니다. Java 코드는 정확히 동일하게 유지되지만, 생성된 SQL 문자열은 라이브러리 코드에 의해 배후에서 변환될 수 있습니다.

jOOQ가 동적 SQL을 작성해야 한다는 것을 의미하지는 않습니다. jOOQ가 생성한 SQL 문자열을 캐시에 저장하거나, jOOQ와 함께 저장 프로시저를 사용할 수 있습니다. 실제로 jOOQ는 저장 프로시저 사용을 권장합니다!

## 결론

SQL 쿼리는 튜플 집합의 설명인 동시에 함수입니다. jOOQ는 이러한 방식으로 SQL에 대해 생각하도록 도와줍니다. SQL 쿼리는 함수형/선언적 컬렉션 설명입니다. 이 패러다임을 염두에 두면 정말 강력한 SQL 문을 작성할 수 있으며, jOOQ는 이 패러다임이 jOOQ API 디자인의 핵심이기 때문에 이를 권장합니다.
