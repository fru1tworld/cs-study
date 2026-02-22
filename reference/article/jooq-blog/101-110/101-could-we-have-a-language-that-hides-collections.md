# 컬렉션을 숨기는 언어가 가능할까?

> 원문: https://blog.jooq.org/could-we-have-a-language-that-hides-collections-from-us/

방금 버그를 하나 수정했습니다. 이 수정 작업에서 `Object[]` 배열을 단순히 `null`이 아닌 각 타입의 초기값으로 초기화해야 했습니다. 즉, `boolean`은 `false`, `int`는 `0`, `double`은 `0.0` 등으로 말이죠.

```java
Object[] converted = new Object[parameterTypes.length];
for (int i = 0; i < converted.length; i++)
    converted[i] = Reflect.initValue(parameterTypes[i]);
```

주관적으로 말하면 8E17번째로, 저는 루프를 작성했습니다. 루프 구조의 각 요소에 대해 메서드를 호출하는 것 외에는 아무런 흥미로운 일도 하지 않는 루프를 말입니다.

제가 정말 하고 싶었던 것은 이것입니다. 저에게는 `Reflect.initValue()`라는 메서드가 있습니다. 제가 정말 하고 싶은 것은 어떤 방식으로든 다음과 같이 하는 것입니다:

```java
converted = initValue(parameterTypes);
```

네, 생각해봐야 할 미묘한 부분들이 있습니다. 예를 들어 이것이 배열을 초기화하는 것인지 아니면 배열에 값을 할당하는 것인지 같은 것들 말입니다. 지금은 그런 것들은 잊어버리세요. 큰 그림을 먼저 봅시다.

핵심은, 아무도 루프를 작성하는 것을 즐기지 않는다는 것입니다. 아무도 map/flatMap을 작성하는 것도 즐기지 않습니다:

```java
Stream.of(parameterTypes)
      .map(Reflect::initValue)
      .toArray(converted);
```

이것은 제가 작성하거나 읽는 것을 전혀 즐기지 않는, 너무나 쓸모없고, 반복적이며, 인프라적인 의식(ceremony)입니다. 여기서 제 '비즈니스 로직'은 단순히 `converted = initValue(parameterTypes);`입니다. 저에게는 3개의 요소가 있습니다: 소스 데이터 구조, 대상 데이터 구조, 그리고 매핑 함수. 그것이 제 코드에서 보여야 할 전부입니다.

반복하는 방법에 대한 모든 인프라는 완전히 의미없고 지루합니다.

사실, SQL 조인도 종종 마찬가지입니다. 우리는 기본 키 / 외래 키 관계를 사용하므로, 부모 테이블과 자식 테이블 사이의 경로는 대부분의 경우 매우 명확합니다.

```sql
SELECT
  cu.first_name,
  cu.last_name,
  co.country
FROM customer AS cu
JOIN address USING (address_id)
JOIN city USING (city_id)
JOIN country AS co USING (country_id)
```

조인은 멋지고, 관계 대수도 멋집니다. 하지만 대부분의 경우, 이것은 이해하기 쉬운 비즈니스 로직을 작성하는 데 방해가 될 뿐입니다.

제 생각에, 이것은 Hibernate의 가장 큰 혁신 중 하나입니다 (아마 다른 것들도 이렇게 했을 것이고, 어쩌면 Hibernate보다 먼저 했을 수도 있습니다): 암묵적 조인(implicit joins)입니다. jOOQ도 이것을 복사했습니다.

```
cu.address.city.country.country
```

명시적 조인을 작성할 때는 많은 의식이 필요하지만, 대안적이고 직관적인 문법이 훨씬 더 편리할 것입니다. 암묵적 조인 문법이 무엇을 의미하는지는 즉시 명확합니다. 명시적 조인을 작성하는 문법적 의식은 필요하지 않습니다.

다시 말하지만, 조인은 정말 멋지고, 파워 유저들은 필요할 때 사용할 수 있을 것입니다. 하지만 인정합시다, 모든 조인의 80%는 지루하며, 문법적 설탕(syntactic sugar)으로 대체될 수 있습니다.

물론, 이 제안은 완벽하지 않을 것입니다. 왜냐하면 오래된 언어에 이렇게 중요한 기능을 도입할 때의 수많은 엣지 케이스를 다루지 않기 때문입니다. 하지만 다시, 큰 그림에 집중할 수 있다면, 다음과 같이 할 수 있다면 좋지 않을까요:

```java
Author[] authors = ...
String[] firstNames = authors.firstName;
```

배열 사용은 무시하세요. `List`, `Stream`, `Iterable` 등 어떤 데이터 구조든 될 수 있습니다. 이것이 다음 외에 다른 의미를 가질 수 있을까요:

```java
int[] firstNameLengths = authors.firstName.length();
```

또는:

```java
Book[] books = authors.books;
```

왜 우리는 이런 것들을 계속 명시적으로 작성해야 할까요? 이것들은 비즈니스 로직이 아닙니다. 이것들은 의미없고, 지루한, 인프라입니다.

네, 분명히 많은 엣지 케이스가 있지만, '매우 명백한' 케이스도 많이 있습니다. 의식적인 매핑 로직이 완전히 명백하고 지루한 경우 말입니다. 하지만 이것은 작성하고 읽는 데 방해가 되며, 많은 경우에 명백해 보임에도 불구하고, 여전히 오류가 발생하기 쉽습니다!

저는 APL의 아이디어를 다시 살펴볼 때라고 생각합니다. APL에서는 모든 것이 배열이고, 그 결과로 차수(arity) 1 타입에 대한 연산을 차수 N 타입에도 동일하게 적용할 수 있습니다. 왜냐하면 그 구분은 종종 별로 유용하지 않기 때문입니다.

Java와 같은 언어에 이것을 개조(retrofit)하는 것은 상상하기 어렵지만, 새로운 언어는 영원히 null을 없앨 수 있을 것입니다. 왜냐하면 차수 0-1은 차수 N의 특수한 경우일 뿐이기 때문입니다: 빈 배열.

여러분의 생각을 기대합니다.
