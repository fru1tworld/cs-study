# Java 8 금요일 선물: Map 향상

> 원문: https://blog.jooq.org/java-8-friday-goodies-map-enhancements/

Data Geekery에서는 Java를 사랑합니다. 그리고 우리는 jOOQ의 유창한 API와 쿼리 DSL이 Java 8의 새로운 기능들을 최대한 활용할 수 있게 되어 정말 기쁩니다.

## Java 8 금요일

매주 금요일마다 여러분에게 Java 8의 새로운 튜토리얼 스타일의 기능을 몇 가지 보여드리고 있습니다. 이것들은 우리가 Data Geekery에서 jOOQ와 함께 Java 8을 활용하기 위해 연구한 것들입니다. 즐기세요!

## Map 향상

많은 Java 8의 선물들이 Streams API를 통해 우리에게 왔습니다. 하지만 몇 가지 유용한 작은 메서드들이 `java.util.Map`에도 추가되었습니다. JDK 8 Javadoc에서 "Default Methods"를 검색해보면 이들이 기본 메서드로 구현되어 하위 호환성을 유지하고 있음을 알 수 있습니다.

### compute() 메서드

때때로 map에서 어떤 값을 가져와서 계산을 수행한 다음, 그것을 다시 map에 저장하고 싶을 때가 있습니다. 이 작업은 상당히 장황할 수 있습니다. 만약 여러분의 map이 ConcurrentHashMap이라면, 이것은 비원자적으로 수행되었습니다. `compute()`, `computeIfAbsent()`, `computeIfPresent()` 메서드들이 이를 도와줍니다!

다음 map을 가정해 봅시다:

```java
Map<String, Integer> map = new HashMap<>();
map.put("A", 1);
map.put("B", 2);
map.put("C", 3);
```

`compute()`는 모든 키에 대해 동작합니다:

```java
// 키 "A"의 값에 41을 더합니다
System.out.println(map.compute("A",
    (k, v) -> v == null ? 42 : v + 41));
// 결과: 42
// map은 이제 {A=42, B=2, C=3}
```

`computeIfAbsent()`는 키가 존재하지 않을 때만 계산합니다:

```java
// 키 "X"가 없으므로 새로운 값 42를 계산합니다
System.out.println(map.computeIfAbsent("X",
    k -> 42));
// 결과: 42
// map은 이제 {A=42, B=2, C=3, X=42}
```

`computeIfPresent()`는 키가 이미 존재할 때만 계산합니다:

```java
// 키 "A"가 존재하므로 값에 41을 더합니다
System.out.println(map.computeIfPresent("A",
    (k, v) -> v + 41));
// 결과: 83
// map은 이제 {A=83, B=2, C=3, X=42}
```

ConcurrentHashMap에서의 핵심 이점은 전체 연산이 원자적으로 실행되어 스레드 안전성이 보장된다는 것입니다.

### forEach()

이것은 아주 멋진 작은 추가 기능입니다. 이전에는 다음과 같이 작성해야 했습니다:

```java
for (Entry<String, Integer> entry : map.entrySet()) {
    System.out.println(entry.getKey() + "=" + entry.getValue());
}
```

이제는 다음과 같이 간단하게 작성할 수 있습니다:

```java
map.forEach((k, v) ->
    System.out.println(k + "=" + v));
// 출력:
// A=1
// B=2
// C=3
```

### merge()

이 메서드는 삽입, 업데이트, 삭제를 하나의 원자적 연산으로 결합합니다.

계약은 다음과 같습니다: 키가 없거나 null이면 값을 연결하고, 그렇지 않으면 리매핑 함수를 사용하여 대체합니다. null을 반환하면 항목을 제거합니다.

중요한 주의사항: 이 메서드의 동작은 Java 8 빌드 116과 129 사이에서 크게 변경되었습니다. 이후 버전에서는 두 번째 인자로 null 값을 금지하고 null 값을 부재한 키와 동일하게 취급합니다.

실제 예제 (단어 빈도 카운터):

```java
Map<String, Integer> wordCount = new HashMap<>();
String[] words = {"apple", "banana", "apple", "cherry", "banana", "apple"};

Arrays.asList(words).forEach(word ->
    wordCount.merge(word, 1, (old, v) -> old + v));

// 결과: {apple=3, banana=2, cherry=1}
```

이 예제에서 `merge()`는 다음과 같이 동작합니다:
- 단어가 map에 없으면 값 1을 넣습니다
- 단어가 이미 있으면 기존 값에 1을 더합니다

### getOrDefault()

이것은 매우 유용한 메서드입니다. 키가 없을 때만 기본값을 반환합니다.

```java
System.out.println(map.getOrDefault("A", 42));
// 결과: 1 (A가 존재하므로)

System.out.println(map.getOrDefault("X", 42));
// 결과: 42 (X가 존재하지 않으므로)
```

중요한 제한: 저장된 값 자체가 null일 때는 NullPointerException을 방지하지 않습니다:

```java
map.put("X", null);
System.out.println(map.getOrDefault("X", 21) + 21);
// NullPointerException 발생!
```

이는 `getOrDefault()`가 키가 존재할 때는 저장된 값(null일지라도)을 반환하기 때문입니다.

### 추가 메서드

ConcurrentHashMap에서 가져온 몇 가지 편의 메서드들도 있습니다:

putIfAbsent() - 키가 없을 때만 삽입:

```java
map.putIfAbsent("A", 42); // A가 이미 있으므로 무시됨
map.putIfAbsent("X", 42); // X가 없으므로 삽입됨
```

remove(key, value) - 값이 일치할 때만 제거:

```java
map.remove("A", 1);  // A의 값이 1이면 제거
map.remove("A", 99); // A의 값이 99가 아니면 제거하지 않음
```

replace() - 기존 항목 업데이트:

```java
map.replace("A", 1, 42); // A의 값이 1이면 42로 대체
map.replace("A", 100);   // A가 존재하면 100으로 대체
```

## null 처리의 복잡성

Java 8 Map 향상은 null 의미론에 관한 기존의 혼란을 심화시킵니다. Map은 null 값을 저장할 수 있지만, 여러 메서드가 컨텍스트에 따라 null을 다르게 처리합니다.

예를 들어:
- `compute()` - null을 반환하면 항목을 제거합니다
- `merge()` - null 값은 부재한 키와 동일하게 취급됩니다
- `getOrDefault()` - 키가 존재하면 null 값도 반환합니다

## 모범 사례 권장 사항

예상치 못한 동작을 방지하려면 map에 null 키와 null 값을 모두 저장하는 것을 피하세요. 이렇게 하면 새로운 Map 메서드들을 사용할 때 혼란을 줄일 수 있습니다.

## 결론

Java 8의 Map 향상은 일상적인 map 조작을 훨씬 더 간결하고 표현력 있게 만들어줍니다. 특히:

- `compute()` 계열 메서드는 가져오기-계산하기-저장하기 패턴을 원자적으로 처리합니다
- `forEach()`는 람다를 사용한 우아한 반복을 가능하게 합니다
- `merge()`는 값 누적 시나리오에 완벽합니다
- `getOrDefault()`는 null 체크 보일러플레이트를 줄여줍니다

다음 주에는 또 다른 흥미로운 Java 8 기능을 살펴보겠습니다. 계속 지켜봐 주세요!
