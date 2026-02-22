# Java 8 금요일 선물: 쉬운 로컬 캐싱

> 원문: https://blog.jooq.org/java-8-friday-goodies-easy-as-pie-local-caching/

Data Geekery에서 우리는 Java를 사랑합니다. 그리고 jOOQ의 fluent API와 쿼리 DSL에 완전히 빠져 있기 때문에, Java 8이 우리 생태계에 가져올 것들에 대해 정말 기대하고 있습니다. 우리는 Java 8의 멋진 기능들에 대해 몇 차례 블로그에 글을 올렸고, 이제 새로운 블로그 시리즈를 시작할 때가 되었다고 느낍니다...

## Java 8 금요일

매주 금요일, 우리는 람다 표현식, 확장 메서드 및 기타 훌륭한 기능들을 활용하는 몇 가지 멋진 새로운 튜토리얼 스타일의 Java 8 기능들을 보여드립니다. 소스 코드는 GitHub에서 찾을 수 있습니다.

## Java 8 선물: 쉬운 로컬 캐싱

이제 이 시리즈에서 지금까지 가장 놀라운 내용 중 하나를 준비하세요. 우리는 오래된 HashMap과 람다 표현식을 사용하여 로컬 캐시를 구현하는 매우 쉬운 방법을 보여드리겠습니다. 왜냐하면 이제 Map은 키가 없는 경우 새 값을 원자적으로 계산하는 새로운 방법을 갖게 되었기 때문입니다. 캐시에 완벽합니다. 코드를 자세히 살펴봅시다:

### 초기 순진한 구현

```java
public static void main(String[] args) {
    for (int i = 0; i < 10; i++)
        System.out.println(
            "f(" + i + ") = " + fibonacci(i));
}

static int fibonacci(int i) {
    if (i == 0)
        return i;

    if (i == 1)
        return 1;

    System.out.println("Calculating f(" + i + ")");
    return fibonacci(i - 2) + fibonacci(i - 1);
}
```

네. 이것은 순진한 방식입니다. fibonacci(5)와 같은 작은 숫자에 대해서도, 위의 알고리즘은 동일한 계산을 기하급수적으로 반복하기 때문에 엄청난 양의 줄을 출력합니다:

```
Calculating f(6)
Calculating f(4)
Calculating f(2)
Calculating f(3)
Calculating f(2)
Calculating f(5)
Calculating f(3)
Calculating f(2)
Calculating f(4)
Calculating f(2)
Calculating f(3)
Calculating f(2)
f(6) = 8
```

### Map.computeIfAbsent()로 캐시 구축하기

우리가 하고 싶은 것은 이전에 계산된 피보나치 수의 캐시를 구축하는 것입니다. 가장 직접적인 기술은 모든 값을 캐시에 메모이제이션하는 것입니다. 다음은 캐시를 구축하는 방법입니다:

```java
static Map<Integer, Integer> cache = new HashMap<>();
```

중요한 부가 설명: 이 블로그 글의 이전 버전에서는 여기에서 `ConcurrentHashMap`을 사용했습니다. `computeIfAbsent()`를 사용하여 재귀적으로 값을 계산할 때는 ConcurrentHashMap을 사용하지 마세요. 자세한 내용은 여기에서: https://stackoverflow.com/q/28840047/521799

완료! 앞서 언급했듯이, 우리는 새로 추가된 `Map.computeIfAbsent()` 메서드를 사용하여 주어진 키에 대한 값이 아직 없는 경우에만 소스 함수에서 새 값을 계산합니다. 캐싱입니다! 그리고 이 메서드는 원자적으로 실행되는 것이 보장되고, HashMap을 사용하기 때문에, 이 캐시는 어디에도 수동으로 synchronized를 적용하지 않고도 스레드 안전합니다. 그리고 피보나치 수 계산 외에 다른 용도로도 재사용할 수 있습니다. 하지만 먼저 이 캐시를 fibonacci() 함수에 적용해 봅시다.

```java
static int fibonacci(int i) {
    if (i == 0)
        return i;

    if (i == 1)
        return 1;

    return cache.computeIfAbsent(i, (key) ->
                 fibonacci(i - 2)
               + fibonacci(i - 1));
}
```

그게 다입니다. 이보다 더 간단할 수 없습니다! 증거가 필요하신가요? 실제로 새 값을 계산할 때마다 콘솔에 메시지를 기록해 보겠습니다:

```java
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
```

위 프로그램은 다음을 출력합니다:

```
f(0) = 0
f(1) = 1
Slow calculation of 2
f(2) = 1
Slow calculation of 3
f(3) = 2
Slow calculation of 4
f(4) = 3
Slow calculation of 5
f(5) = 5
Slow calculation of 6
f(6) = 8
Slow calculation of 7
f(7) = 13
Slow calculation of 8
f(8) = 21
Slow calculation of 9
f(9) = 34
```

## Java 7에서는 어떻게 했을까요?

좋은 질문입니다. 많은 코드로요. 아마도 double-checked locking을 사용하여 다음과 같이 작성했을 것입니다:

```java
static int fibonacciJava7(int i) {
    if (i == 0)
        return i;

    if (i == 1)
        return 1;

    Integer result = cache.get(i);
    if (result == null) {
        synchronized (cache) {
            result = cache.get(i);

            if (result == null) {
                System.out.println(
                    "Slow calculation of " + i);

                result = fibonacci(i - 2)
                       + fibonacci(i - 1);
                cache.put(i, result);
            }
        }
    }

    return result;
}
```

설득되셨나요?

참고로, 실제 솔루션에서는 아마도 Guava Caches를 사용할 것입니다.

## 결론

람다는 Java 8의 한 부분에 불과합니다. 중요한 부분이지만, 라이브러리에 추가된 모든 새로운 기능들과 이제 람다와 함께 활용할 수 있는 것들을 잊지 맙시다!

이 블로그 시리즈의 다음 주에는 Java 8이 기존 및 새로운 동시성 API를 어떻게 개선하는지 살펴볼 예정이니, 계속 지켜봐 주세요!

## Java 8에 대한 더 많은 정보

그동안 Eugen Paraschiv의 훌륭한 Java 8 리소스 페이지를 살펴보세요.
