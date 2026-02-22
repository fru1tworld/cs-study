# 모든 것을 메서드로 추출하지 마라

> 원문: https://blog.jooq.org/dont-extract-everything-into-a-method/

일부 개발자들은 다음과 같은 글들을 도그마처럼 받아들인다:

- 메서드는 한 가지 일만 해야 한다
- 메서드 내 추상화 수준은 동일해야 한다

나는 이 글에서 이러한 "모범 사례"들에 대해 의문을 제기하려 한다. 모든 것을 메서드로 추출하는 것이 항상 최선은 아니다. 때로는 명령형 스타일이 더 단순하다(더 좋다는 게 아니라, 그냥 더 단순하다).

## 반복문에서 빠져나오기

`break`와 `continue`는 흔히 사용된다. 약간 이상해 보일 수 있지만, 한번 살펴보자.

```java
for (int i = 0;; i++)
    // 42에서 멈춤
    if (i == 42)
        break;
    // 짝수는 무시
    else if (i % 2 == 0)
        continue;
    // 메인 동작
    else
        System.out.println("i = " + i);
```

물론 이 예제는 다음과 같이 리팩토링할 수 있다:

```java
for (int i = 0; i < 42; i++)
    if (i % 2 != 0)
        System.out.println("i = " + i);
```

위 리팩토링된 예제가 더 간단해 보일 수 있다. 하지만 때로는 `continue`를 사용하면 조건문의 복잡한 중첩 없이도 복잡한 반복문 로직을 훨씬 더 간단하게 작성할 수 있다. jOOQ의 `AbstractRecord.compareTo()` 메서드를 살펴보자:

```java
for (int i = 0; i < size(); i++) {
    final Object thisValue = get(i);
    final Object thatValue = that.get(i);

    // 둘 다 null이면 비교할 필요 없음
    if (thisValue == null && thatValue == null)
        continue;
    // null 값은 제일 큼
    else if (thisValue == null)
        return 1;
    else if (thatValue == null)
        return -1;
    else {
        // ... 실제로 흥미로운 비교 로직
    }
}
return 0;
```

`continue`를 제거하려면 마지막 `else` 블록 안에 다음의 모든 것을 중첩해야 한다. 또는 이 로직을 자체 메서드로 추출해야 한다.

```java
for (int i = 0; i < size(); i++) {
    final Object thisValue = get(i);
    final Object thatValue = that.get(i);

    if (!(thisValue == null && thatValue == null)) {
        if (thisValue == null)
            return 1;
        else if (thatValue == null)
            return -1;
        else {
            // ... 실제로 흥미로운 비교 로직
        }
    }
}
return 0;
```

`continue` 문을 제거하기 위해 추가적인 들여쓰기와 중첩이 필요해졌다. 이게 더 나은 것일까? 물론 사소한 차이다. 개인적으로 나는 `continue`가 있는 버전이 더 읽기 쉽다고 생각한다. 왜냐하면 다음에 무엇이 올지 생각할 때 곧바로 "OK, 두 값이 모두 null이면 신경 쓸 필요 없고 다음으로 넘어간다"라고 바로 이해할 수 있기 때문이다.

이 논쟁은 이 블로그 포스트의 핵심이 아니다. 핵심은 다음 기능이다.

## If문에서 빠져나오기

Java(그리고 많은 다른 언어들)에서 잘 알려지지 않은 기능이 있다. `break`(그리고 `continue`)를 레이블과 함께 사용하면 외부 반복문뿐 아니라 어떤 문장에서든 빠져나올 수 있다. 다음 예제를 보자:

```java
public class FunWithBreak {
    public static void main(String[] args) {
        label:
        if (args.length > 0) {
            System.out.println("인자가 있습니다!");

            for (String arg : args)
                if ("ignore-args".equals(arg))
                    break label;

            for (String arg : args)
                System.out.println("인자 : " + arg);
        }
        else {
            System.out.println("인자 없음");
        }

        System.out.println("프로그램 종료");
    }
}
```

위 예제에서 `break label` 문을 통해 `if`문에서 빠져나올 수 있다. 이 프로그램을 인자 없이 실행하면:

```
인자 없음
프로그램 종료
```

`a`, `b` 인자와 함께 실행하면:

```
인자가 있습니다!
인자 : a
인자 : b
프로그램 종료
```

`a`, `b`, `ignore-args` 인자와 함께 실행하면:

```
인자가 있습니다!
프로그램 종료
```

## 대안

위 예제는 물론 별도의 메서드로 추출할 수도 있다:

```java
public class FunWithBreak {
    public static void main(String[] args) {
        if (args.length > 0)
            nameThisLater(args);
        else
            System.out.println("인자 없음");

        System.out.println("프로그램 종료");
    }

    private static void nameThisLater(String[] args) {
        System.out.println("인자가 있습니다!");

        for (String arg : args)
            if ("ignore-args".equals(arg))
                return;

        for (String arg : args)
            System.out.println("인자 : " + arg);
    }
}
```

여기서 `return`문은 `break`문과 유사한 역할을 한다. 더 낫거나 더 읽기 쉬운가? 음, 이것도 역시 사소한 차이다. 당신이 결정하라. 하지만 이제 모든 로컬 상태를 새 메서드에 전달해야 하고, 그 메서드에 이름을 붙여야 한다. 만약 한 군데서만 호출될 "쓰레기" 메서드를 원하지 않는다면 어떻게 할까? 아니면 모든 메서드에 단일 return 문만 있어야 한다고 믿는다면? (그건 최악이다. 그러지 마라)

```java
public class FunWithBreak {
    public static void main(String[] args) {
        if (args.length > 0) {
            ifBlock(args);
        }
        else {
            System.out.println("인자 없음");
        }

        System.out.println("프로그램 종료");
    }

    private static void ifBlock(String[] args) {
        System.out.println("인자가 있습니다!");

        boolean ignoreArgs = false;
        for (String arg : args) {
            if ("ignore-args".equals(arg)) {
                ignoreArgs = true;
                break;
            }
        }

        if (!ignoreArgs)
            for (String arg : args)
                System.out.println("인자 : " + arg);
    }
}
```

보라, 이 코드가 정말 더 나은가? 물론 단순한 예제에서는 그렇게 나빠 보이지 않는다. 하지만 이제 어디서나 추가적인 `boolean` 상태 변수를 확인해야 한다. 메서드에서 빠져나올 때마다 `ignoreArgs` 플래그를 확인해야 한다. 물론 명시적인 상태 기계를 만들고 싶다면 그렇게 하라. 하지만 그게 항상 최선의 해결책은 아니다.

레이블이 붙은 `break`문은 더 간단하고 직관적이다.

## 보너스: 레이블 위치

재미있는 사실: 레이블은 `if` 키워드와 블록 사이에도 넣을 수 있다:

```java
public class FunWithBreak {
    public static void main(String[] args) {
        if (args.length > 0) label: {
            System.out.println("인자가 있습니다!");

            for (String arg : args)
                if ("ignore-args".equals(arg))
                    break label;

            for (String arg : args)
                System.out.println("인자 : " + arg);
        }
        else {
            System.out.println("인자 없음");
        }

        System.out.println("프로그램 종료");
    }
}
```

이렇게 하면 레이블의 범위가 블록 안에서만 유효하다. 좋지 않은가!

## 보너스 보너스

다음 프로그램은 무엇을 출력할까?

```java
for (int i = 0; i < 3; i++) {

    hmmm:
    try {
        System.out.println("Try      : " + i);
        if (i % 2 == 0)
            break hmmm;
        else
            throw new RuntimeException("흠...");
    }
    finally {
        System.out.println("Finally  : " + i);
        if (i < 2)
            continue;
    }

    System.out.println("Loop end : " + i);
}
```

정답:

```
Try      : 0
Finally  : 0
Try      : 1
Finally  : 1
Try      : 2
Finally  : 2
Loop end : 2
```

좋아, 이건 좀 과한 것 같다. 이런 코드를 작성한다면 별도의 메서드로 추출하는 게 낫겠다.

## 결론

단순함은 아름다움처럼 보는 사람의 눈에 달려 있다. 때로는 명령형 스타일이 더 단순하다(더 좋다는 게 아니라, 그냥 더 단순하다). 모든 것을 메서드로 추출하는 것이 항상 정답은 아니다. 레이블이 붙은 `break`와 `continue` 같은 언어 기능을 적절히 활용하면 코드를 더 읽기 쉽게 만들 수 있다. 하지만 역시 때에 따라 다르다. 핵심은 도그마에 얽매이지 않고, 상황에 맞는 최선의 선택을 하는 것이다.
