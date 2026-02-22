# 매우 특이하지만 아마도 영리한 Kotlin 언어 기능

> 원문: https://blog.jooq.org/a-very-peculiar-but-possibly-cunning-kotlin-language-feature/

이것은 저를 놀라게 했습니다. jOOQ에서 이 흥미로운 새 언어를 가장 잘 활용하는 방법을 배우기 위해 Kotlin 언어를 공부하던 중, 이 퍼즐러를 발견했습니다.

다음 코드를 살펴보세요:

```kotlin
fun main(args: Array<String>) {
    (1..5).forEach {
        if (it == 3)
            return
        print(it)
    }
    print("done")
}
```

이 코드는 무엇을 출력할까요?

Java 8의 동일한 코드를 보면:

```java
public static void main(String[] args) {
    IntStream.rangeClosed(1, 5).forEach(it -> {
        if (it == 3)
            return;
        System.out.print(it);
    });
    System.out.print("done");
}
```

Java 8 버전은 다음을 출력합니다:

```
1245done
```

Java에서 람다 내부의 `return`은 람다 자체에서만 반환되기 때문입니다. 람다는 3을 제외한 모든 숫자에 대해 실행되고, 그 다음 "done"이 출력됩니다.

하지만 Kotlin 버전은 다음을 출력합니다:

```
12
```

왜냐하면 Kotlin에서 람다/클로저 내부의 `return` 문은 람다 자체에서 반환되는 것이 아니라 둘러싸는 함수 범위에서 반환되기 때문입니다!

## 이유가 무엇일까요?

이것은 의도된 동작입니다. JetBrains의 개발자인 Dmitry Jemerov가 트위터에서 그 이유를 설명했습니다:

> "이유는 매우 간단합니다: 우리는 내장된 언어 기능처럼 정확히 작동하는 람다를 원합니다"

> "따라서 'synchronized' 함수에 전달된 람다에서의 'return'은 Java의 'synchronized' 블록에서의 'return'과 같은 동작을 해야 합니다"

## inline 함수의 역할

Kotlin에서는 `try-with-resources`나 `synchronized` 블록과 같은 전통적인 언어 기능을 제거하고 대신 라이브러리 함수로 구현했습니다. 이러한 라이브러리 함수가 언어 구조처럼 느껴지도록 하기 위해, `return` 문은 람다가 아닌 외부 범위로 점프하는 일관된 의미론을 가져야 했습니다.

예를 들어, `synchronized` 함수를 살펴보세요:

```kotlin
fun main(args : Array<String>) {
    val lock = Object()
    val x = synchronized(lock, {
        if (1 == 1)
            return
        "1"
    })
    print(x)
}
```

`synchronized` 함수는 다음과 같이 정의되어 있습니다:

```kotlin
public inline fun <R> synchronized(lock: Any, block: () -> R): R {
    monitorEnter(lock)
    try {
        return block()
    }
    finally {
        monitorExit(lock)
    }
}
```

핵심은 `inline` 수식어입니다. 인라인 함수는 람다 코드를 별도의 함수로 실행하지 않고 호출 지점에 직접 임베딩합니다. 이는 별도의 스택 프레임이 생성되지 않음을 의미합니다. 결과적으로 람다의 `return`은 자연스럽게 둘러싸는 범위에서 빠져나가게 되며, 이는 Java의 synchronized 블록에서의 return과 동등합니다.

## 대안: 레이블이 지정된 return

만약 람다에서만 반환하고 싶다면, 레이블이 지정된 return을 사용할 수 있습니다:

```kotlin
fun main(args: Array<String>) {
    (1..5).forEach {
        if (it == 3)
            return@forEach
        print(it)
    }
    print("done")
}
```

이렇게 하면 예상대로 `1245done`이 출력됩니다. `return@forEach`는 `forEach` 람다에서만 반환하도록 명시적으로 지정합니다.

## 결론

이상합니다. 영리합니다. 똑똑합니다. 하지만 약간 예상치 못한 동작입니다.

Java 개발자들에게는 놀라울 수 있지만, 한 번 이해하면 잠재적으로 강력한 기능입니다. 이 설계 결정이 진정한 혁신을 나타내는지, 아니면 언어 진화에서 나중에 후회할 선택인지는 시간이 말해줄 것입니다.
