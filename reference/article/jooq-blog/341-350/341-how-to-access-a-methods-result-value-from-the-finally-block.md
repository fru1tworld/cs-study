# finally 블록에서 메서드의 결과 값에 접근하는 방법

> 원문: https://blog.jooq.org/how-to-access-a-methods-result-value-from-the-finally-block/

JVM은 스택 기반 머신이지만, Java 언어는 실제로 해당 스택에 접근할 수 있는 방법을 제공하지 않습니다. 드문 경우이긴 하지만 때때로 매우 유용할 수 있는데도 말입니다.

## 예제

메서드의 결과 값은 스택에 저장됩니다. 다음 예제를 살펴보겠습니다:

```java
public int method() {
    if (something)
        return 1;

    ...
    if (somethingElse)
        return 2;

    ...
    return 0;
}
```

정지 문제(halting problem), 에러 처리 및 기타 학술적인 논의를 무시한다면, 위 메서드는 "확실히" `1`, `2`, 또는 `0` 중 하나의 값을 반환할 것입니다. 그리고 그 값은 메서드를 빠져나가기 전에 스택에 저장됩니다. 때로는 특정 결과 값이 반환될 때만 어떤 동작을 수행해야 하는 경우가 있을 수 있습니다. 사람들은 그러면 다중 `return` 문이 나쁜 것인지에 대한 오래된 논쟁에 빠져들 수 있고, 전체 메서드를 다음과 같이 작성해야 한다고 주장할 수도 있습니다:

```java
public int method() {
    int result = 0;

    if (something)
        result = 1;

    ...
    if (somethingElse)
        result = 2;

    ...
    // 반환 전 중요한 동작 수행
    if (result == 1337)
        log.info("hehehe ;-)");

    return result;
}
```

물론 위의 예제는 잘못되었습니다. 왜냐하면 이전에는 `if (something) return 1`과 `if (something) return 2` 문이 즉시 메서드 실행을 중단했기 때문입니다. "단일 return 문" 기법으로 동일한 동작을 달성하려면 코드를 다음과 같이 다시 작성해야 합니다:

```java
public int method() {
    int result = 0;

    if (something)
        result = 1;
    else {

        ...
        if (somethingElse)
            result = 2;
        else {
            ...
        }
    }

    // 반환 전 중요한 동작 수행
    if (result == 1337)
        log.info("hehehe ;-)");

    return result;
}
```

… 그리고 물론 중괄호 사용이나 들여쓰기 수준에 대해 계속 논쟁할 수 있는데, 이는 결국 아무것도 얻지 못했다는 것을 보여줍니다.

## 스택에서 반환 값에 접근하기

원래 구현에서 우리가 정말로 하고 싶었던 것은 반환 직전에 스택에 어떤 값이 있는지, 즉 어떤 값이 반환될 것인지 확인하는 것입니다. 다음은 의사(pseudo) Java 코드입니다:

```java
public int method() {
    try {
        if (something)
            return 1;

        ...
        if (somethingElse)
            return 2;

        ...
        return 0;
    }

    // 반환 전 중요한 동작 수행
    finally {
        if (reflectionMagic.methodResult == 1337)
            log.info("hehehe ;-)");
    }
}
```

좋은 소식은: 네, 할 수 있습니다! 위의 목표를 달성하기 위한 간단한 트릭이 있습니다:

```java
public int method() {
    int result = 0;

    try {
        if (something)
            return result = 1;

        ...
        if (somethingElse)
            return result = 2;

        ...
        return result = 0;
    }

    // 반환 전 중요한 동작 수행
    finally {
        if (result == 1337)
            log.info("hehehe ;-)");
    }
}
```

덜 좋은 소식은: 결과를 명시적으로 할당하는 것을 절대 잊어서는 안 됩니다. 하지만 가끔씩 이 기법은 Java 언어가 실제로 허용하지 않는 "메서드 스택에 접근"할 때 매우 유용할 수 있습니다.

## 물론…

물론 다음과 같은 지루한 해결책을 사용할 수도 있습니다:

```java
public int method() {
    int result = actualMethod();

    if (result == 1337)
        log.info("hehehe ;-)");

    return result;
}

public int actualMethod() {
    if (something)
        return result = 1;

    ...
    if (somethingElse)
        return result = 2;

    ...
    return result = 0;
}
```

… 그리고 아마도 대부분의 경우 이 기법이 실제로 더 낫습니다(약간 더 읽기 쉽기 때문에). 하지만 때로는 `finally` 블록에서 로깅 이상의 작업을 수행하고 싶거나, 결과 값 이상의 것에 접근하고 싶고, 메서드를 리팩토링하고 싶지 않을 때가 있습니다.

## 다른 접근 방식?

이제 여러분 차례입니다. 여러분이 선호하는 대안적인 접근 방식은 무엇인가요(코드 예제와 함께)? 예를 들어, Try 모나드를 사용하는 것은? 아니면 Aspect를 사용하는 것은?
