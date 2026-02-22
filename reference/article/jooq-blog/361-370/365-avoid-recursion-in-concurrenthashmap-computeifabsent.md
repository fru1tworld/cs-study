# ConcurrentHashMap.computeIfAbsent()에서 재귀를 피하라

> 원문: https://blog.jooq.org/avoid-recursion-in-concurrenthashmap-computeifabsent/

게시일: 2015년 3월 4일

작성자: lukaseder

---

때때로 어떤 버그를 우연히 발견하고 그것이 쉽게 고칠 수 없을 것이라는 것을 알 때, 그것에 대해 블로그에 글을 씁니다. 아마도 나중에 제 글이 검색 엔진에 의해 발견되어 미래의 프로그래머들을 구원할 수 있을 것입니다.

## 문제

자바 8에서 도입된 `ConcurrentHashMap.computeIfAbsent()` 메서드는 매우 멋진 메서드이지만, 잘못 사용하면 프로그램이 영원히 멈출 수 있습니다. 다음 코드를 고려해보세요:

```java
public class Test {
    static Map<Integer, Integer> cache = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println(fibonacci(8));
    }

    static int fibonacci(int i) {
        if (i == 0)
            return i;

        if (i == 1)
            return 1;

        return cache.computeIfAbsent(i, (key) -> {
            System.out.println(
                "Slow calculation of " + key);

            return fibonacci(i - 2) + fibonacci(i - 1);
        });
    }
}
```

이것은 피보나치 수를 "느리게" 계산하고 그것들을 캐시에 저장하는 "영리한" 알고리즘입니다. 훌륭해 보이나요? 이것을 실행하면 절대 끝나지 않습니다! (적어도 제 기계와 JDK 버전에서는요. 공유 뮤텍스와 관련된 것이므로 다른 결과를 얻을 수도 있습니다.)

## 문제가 되는 이유

Javadoc에는 이렇게 명시되어 있습니다:

> 계산이 진행되는 동안 다른 스레드에 의한 이 맵의 업데이트 작업이 차단될 수 있으므로, 계산은 짧고 간단해야 하며, 이 맵의 다른 매핑을 업데이트하려고 시도해서는 안 됩니다.

실제로, JDK 1.8.0_25-b18까지는 위와 같은 재귀 계산이 영원히 무한 루프에 빠질 뿐 `IllegalStateException`을 던지지 않습니다.

## 해결책

간단한 해결책이 있습니다. `ConcurrentHashMap` 대신 그냥 `HashMap`을 사용하세요:

```java
static Map<Integer, Integer> cache = new HashMap<>();
```

`HashMap.computeIfAbsent()` 메서드는 이러한 재귀 연산을 제한하지 않습니다. 그러나 물론 동시성 보장은 사라집니다.

## 더 심각한 문제

여기서 더 심각한 문제가 있습니다. 하위 타입이 상위 타입의 계약을 재정의합니다. 일반적인 `Map<Integer, Integer>` 타입으로 참조를 선언하면, 실제 구현이 `ConcurrentHashMap`인지 `HashMap`인지 쉽게 알 수 없으며, 따라서 이 제약 조건을 예상하지 못할 수 있습니다.

이것은 Liskov 치환 원칙(LSP)의 미묘한 위반입니다. 우리는 일반적인 `Map` 인터페이스를 통해 작업하고 있으므로, 특정 구현의 제약 조건에 대해 알 필요가 없어야 합니다. 하지만 `ConcurrentHashMap`은 다르게 동작합니다.

## 버그 리포트

이 문제에 대해 두 개의 버그 리포트가 제출되었습니다:

- JDK-8062841 - 이전에 해결되지 않은 리포트
- JDK-8074374 - 저자가 제출한 리포트

## 결론

절대로 `ConcurrentHashMap.computeIfAbsent()` 메서드 내부에서 재귀하지 마세요. 이것은 프로그램이 영원히 멈추게 할 수 있습니다. 캐시와 재귀가 필요하다면 `HashMap`을 사용하거나, 재귀를 피하도록 알고리즘을 다시 설계하세요.
