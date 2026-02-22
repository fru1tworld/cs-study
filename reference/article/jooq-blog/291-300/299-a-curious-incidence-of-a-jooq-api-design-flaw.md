# jOOQ API 설계 결함의 흥미로운 사건

> 원문: https://blog.jooq.org/a-curious-incidence-of-a-jooq-api-design-flaw/

jOOQ는 Java(호스트 언어)에서 SQL 언어(외부 DSL)를 모델링하는 내부 도메인 특화 언어(DSL)입니다. jOOQ API의 주요 메커니즘은 다음 인기 있는 글에서 설명하고 있습니다: "The Java Fluent API Designer Crash Course". 해당 글의 규칙에 따라 누구나 Java(또는 대부분의 다른 호스트 언어)에서 내부 DSL을 구현할 수 있습니다.

## SQL 언어 기능 예시: BOOLEAN

SQL 언어의 좋은 점 중 하나는 SQL:1999에서 뒤늦게 도입된 `BOOLEAN` 타입입니다. 물론 boolean 없이도 `1`과 `0`을 통해 `TRUE`와 `FALSE` 값을 모델링하고, `CASE`를 사용하여 술어를 값으로 변환할 수 있습니다:

```
CASE WHEN A = B THEN 1 ELSE 0 END
```

하지만 진정한 `BOOLEAN` 지원이 있으면 Sakila 데이터베이스에서 실행되는 다음 PostgreSQL 쿼리처럼 멋진 쿼리를 작성할 수 있습니다:

```sql
SELECT
  f.title,
  string_agg(a.first_name, ', ') AS actors
FROM film AS f
JOIN film_actor AS fa USING (film_id)
JOIN actor AS a USING (actor_id)
GROUP BY film_id
HAVING every(a.first_name LIKE '%A%')
```

위 쿼리의 결과는 다음과 같습니다:

```
TITLE                    ACTORS
-----------------------------------------------------
AMISTAD MIDSUMMER        CARY, DARYL, SCARLETT, SALMA
ANNIE IDENTITY           CATE, ADAM, GRETA
ANTHEM LUKE              MILLA, OPRAH
ARSENIC INDEPENDENCE     RITA, CUBA, OPRAH
BIRD INDEPENDENCE        FAY, JAYNE
...
```

다시 말해, 영화에 출연한 모든 배우의 이름에 문자 "A"가 포함된 모든 영화를 찾고 있습니다. 이는 boolean 표현식/술어 `first_name LIKE '%A%'`에 대한 집계를 통해 수행됩니다:

```
HAVING every(a.first_name LIKE '%A%')
```

이제 jOOQ API 관점에서 보면, 다양한 인자 타입을 받는 `having()` 메서드의 오버로드를 제공해야 합니다:

```java
// "전통적인" 술어를 받는 메서드들
having(Condition... conditions);
having(Collection<? extends Condition> conditions);

// BOOLEAN 타입을 받는 메서드
having(Field<Boolean> condition);
```

물론 이러한 오버로드는 `HAVING` 절뿐만 아니라 술어/boolean 값을 받는 모든 API 메서드에서 사용 가능합니다. 앞서 언급했듯이, SQL:1999 이후로 jOOQ의 `Condition`과 `Field<Boolean>`은 실제로 동일한 것입니다. jOOQ는 명시적 API를 통해 둘 사이의 변환을 허용합니다:

```java
Condition condition1 = FIRST_NAME.like("%A%");
Field<Boolean> field = field(condition1);
Condition condition2 = condition(field);
```

…그리고 오버로드는 변환을 더 편리하게 암시적으로 만들어 줍니다.

## 그래서 문제가 뭔가요?

문제는 또 다른 편의 오버로드인 `having(Boolean)` 메서드를 추가하는 것이 좋은 아이디어라고 생각했다는 것입니다. 이 메서드에서 상수, nullable `BOOLEAN` 값을 쿼리에 도입할 수 있어 동적 SQL을 빌드하거나 일부 술어를 주석 처리할 때 유용할 수 있습니다:

```java
DSL.using(configuration)
   .select()
   .from(TABLE)
   .where(true)
// .and(predicate1)
   .and(predicate2)
// .and(predicate3)
   .fetch();
```

아이디어는 어떤 술어를 일시적으로 제거하든 `WHERE` 키워드가 절대 주석 처리되지 않는다는 것입니다. 불행히도 이 오버로드를 추가하면서 IDE 자동 완성을 사용하는 개발자들에게 골칫거리가 생겼습니다. 다음 두 메서드 호출을 살펴보세요:

```java
// jOOQ API 사용
Condition condition1 = FIRST_NAME.eq   ("ADAM");
Condition condition2 = FIRST_NAME.equal("ADAM");

// Object.equals 사용 (실수)
boolean = FIRST_NAME.equals("ADAM");
```

(실수로) `equal()` 메서드에 문자 "s"를 추가하면 – 대부분 IDE 자동 완성 때문에 – 전체 술어 표현식의 의미가 크게 바뀝니다. SQL을 생성하는 데 사용할 수 있는 jOOQ 표현식 트리 요소에서 "일반적인" boolean 값(당연히 항상 `false`를 반환)으로 바뀝니다. 마지막 오버로드를 추가하기 전에는 이것이 문제가 되지 않았습니다. Java `boolean` 타입을 받는 적용 가능한 오버로드가 없었기 때문에 `equals()` 메서드 사용은 컴파일되지 않았습니다.

```java
// "전통적인" 술어를 받는 메서드들
having(Condition condition);
having(Condition... conditions);
having(Collection<? extends Condition> conditions);

// BOOLEAN 타입을 받는 메서드
having(Field<Boolean> condition);

// 이 메서드는 jOOQ 3.7 이전에는 존재하지 않았습니다
// having(Boolean condition);
```

jOOQ 3.7 이후, 컴파일러가 더 이상 경고하지 않아 이 실수가 사용자 코드에서 눈에 띄지 않게 되었고, 잘못된 SQL을 생성하게 되었습니다.

## 결론: 내부 DSL을 설계할 때 주의하세요. 호스트 언어의 "결함"을 물려받습니다

Java는 모든 타입이 `java.lang.Object`를 상속받도록 보장되어 있고, 그와 함께 다음 메서드들을 상속받는다는 점에서 "결함"이 있습니다: `getClass()`, `clone()`, `finalize()`, `equals()`, `hashCode()`, `toString()`, `notify()`, `notifyAll()`, 그리고 `wait()`. 대부분의 API에서 이것은 그다지 큰 문제가 아닙니다. 위의 메서드 이름을 재사용할 필요가 없습니다(제발, 하지 마세요). 하지만 내부 DSL을 설계할 때, 이러한 `Object` 메서드 이름들은 (언어 키워드와 마찬가지로) 설계 공간을 제한합니다. 이는 `equal(s)`의 경우에 특히 명백합니다. 우리는 교훈을 얻었고, `having(Boolean)` 오버로드와 모든 유사한 오버로드를 더 이상 사용하지 않도록 지정(deprecated)했으며 제거할 것입니다.
