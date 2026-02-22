# Java 16에서 C 스타일 로컬 정적 변수 작성하기

> 원문: https://blog.jooq.org/write-c-style-local-static-variables-in-java-16/

게시일: 2021년 10월 22일, Lukas Eder

Java 16에서 [JEP 395](https://openjdk.java.net/jeps/395)가 도입되면서, 내부 클래스에서 정적 멤버 선언에 대한 제한이 완화되었습니다.

이를 통해 흥미로운 트릭을 사용할 수 있게 되었는데, C 스타일의 로컬 정적 변수를 에뮬레이트하는 것입니다. 로컬 정적 변수란 한 번만 초기화되고 여러 메서드 실행 간에 공유되는 변수입니다.

## 문제점

다음 코드를 가정해 봅시다:

```java
import static java.util.regex.Pattern.compile;
import java.util.regex.Pattern;

public class Test {
    public static void main(String[] args) {
        check("a");
        check("b");
    }

    static void check(String string) {
        System.out.println("check(" + string + "): "
            + compile("a").matcher(string).find());
    }
}
```

이것은 출력합니다:

```
check(a): true
check(b): false
```

위 코드의 문제점은 `check()` 메서드가 호출될 때마다 정규 표현식을 다시 컴파일한다는 것입니다. 이는 비효율적입니다.

간단한 해결책이 있습니다. 패턴을 클래스 수준 상수로 추출하면 됩니다:

```java
static final Pattern P_CHECK = compile("a");

static void check(String string) {
    System.out.println("check(" + string + "): "
        + P_CHECK.matcher(string).find());
}
```

패턴이 한 번만 컴파일된다는 점에서는 좋습니다.

하지만 이제 클래스 네임스페이스에 새로운 멤버가 생겼다는 점에서는 좋지 않습니다. 이렇게 하면 다음과 같이 됩니다:

- 정규 표현식이 한 번만 컴파일되는 것: 좋음
- 메서드 근처 범위에 패턴을 유지하는 것: 좋지 않음 (네임스페이스에서 이름이 벗어나 더 이상 메서드 내에 있지 않음)
- 네임스페이스를 오염시키는 것: 좋지 않음 (수백 개의 패턴이 있다면 어떻게 하시겠습니까?)

## 해결책

이상적으로 Java는 C 스타일의 로컬 정적 변수를 지원했을 것입니다. 하지만 그렇지 않습니다. 그래도 위에서 언급한 바와 같이 JEP 395를 사용하면 로컬 클래스를 통해 이를 에뮬레이트할 수 있습니다:

```java
static void check(String string) {
    var patterns = new Object() {
        static final Pattern P_CHECK = compile("a");
    };

    System.out.println("check(" + string + "): "
        + patterns.P_CHECK.matcher(string).find());
}
```

이렇게 하면 모든 장점을 얻을 수 있습니다:

- 정규 표현식이 한 번만 컴파일되는 것: 좋음
- 패턴을 메서드에 로컬하게 유지하는 것: 좋음
- 네임스페이스를 오염시키지 않는 것: 좋음

여기서 핵심은 `var` 키워드입니다. `var` 없이는 `P_CHECK` 정적 필드를 갖는 익명 클래스의 타입을 표기할 방법이 없습니다.

물론, 추가 클래스와 불필요한 객체를 생성하고 있지만, 탈출 분석(escape analysis)이 이를 할당하지 않도록 방지할 수 있을 것입니다.

## 대안

댓글에서 언급된 더 깔끔한 대안은 명명된 로컬 클래스를 사용하는 것입니다:

```java
static void check(String string) {
    class Static {
        static final Pattern P_CHECK = compile("a");
    }

    System.out.println("check(" + string + "): "
        + Static.P_CHECK.matcher(string).find());
}
```

이 접근 방식은 불필요한 객체 인스턴스를 생성하지 않으며, 스코프 캡처링에 대한 우려도 없습니다. 또한 `var` 키워드에 의존하지 않고도 정적 필드에 접근할 수 있습니다.
