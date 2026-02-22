# 매개변수 없는 제네릭 메서드 안티패턴

> 원문: https://blog.jooq.org/the-parameterless-generic-method-antipattern/

Stack Overflow의 한 질문이 문제가 있는 Java 제네릭 패턴을 조명했습니다. 다음 메서드를 살펴보세요:

```java
<X extends CharSequence> X getCharSequence() {
    return (X) "hello";
}
```

안전하지 않은 캐스트가 의심스러워 보이지만, Java 8은 다음 할당을 오류 없이 컴파일합니다:

```java
Integer x = getCharSequence();
```

이것은 근본적으로 잘못된 것입니다. `Integer`는 `final` 클래스이고 `CharSequence`를 구현할 수 없기 때문입니다. 그러나 Java의 제네릭 타입 시스템은 `final` 클래스를 무시하고 `X`에 대해 `Integer & CharSequence`라는 교차 타입(intersection type)을 추론한 후 `Integer`로 업캐스트합니다. 컴파일러는 이를 허용하지만, 런타임에 `ClassCastException`이 발생합니다.

## 핵심 문제

실제 문제는 이 특정 예제를 넘어섭니다. 저자는 다음과 같이 말합니다: "반환 타입에만 제네릭인 메서드는 (거의) 올바르지 않습니다."

`Collections.emptyList()`와 같은 메서드에는 예외가 있습니다. 이 메서드가 작동하는 이유는 빈 컬렉션의 의미론 때문입니다—빈 리스트는 어떤 제네릭 타입 요구 사항도 만족시킵니다. 빌더 메서드도 자격이 있습니다. 처음에 빈 객체를 생성하기 때문입니다.

`getCharSequence()`를 포함한 대부분의 다른 메서드에서 의미론적으로 올바른 유일한 반환 값은 `null`입니다. `null`은 어떤 참조 타입에도 할당 가능하기 때문입니다. 그러나 이는 메서드의 의도된 목적을 무산시킵니다.

## 함수형 프로그래밍 관점

메서드는 (대부분) 부작용 없는 함수로 작동합니다. 매개변수가 없는 함수는 `emptyList()`처럼 일관되게 동일한 값을 반환해야 합니다. `<T>`나 `<X extends CharSequence>`와 같은 제네릭 타입 매개변수는 타입 소거(erasure) 때문에 진정한 매개변수로 기능하지 않습니다—메서드 내부에서 검사할 수 없습니다.

핵심 원칙: "반환 타입에만 제네릭인 메서드는 (거의) 올바르지 않습니다."

## 문제가 있는 메서드 찾기

저자는 제네릭이면서 매개변수가 없는 메서드를 식별하는 Guava를 사용한 클래스패스 스캐너를 제공합니다:

```java
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.stream.Stream;
import com.google.common.reflect.ClassPath;

public class Scanner {
    public static void main(String[] args) throws Exception {
        ClassPath
           .from(Thread.currentThread().getContextClassLoader())
           .getTopLevelClasses()
           .stream()
           .filter(info -> !info.getPackageName().startsWith("slick")
                        && !info.getPackageName().startsWith("scala"))
           .flatMap(info -> {
               try {
                   return Stream.of(info.load());
               }
               catch (Throwable ignore) {
                   return Stream.empty();
               }
           })
           .flatMap(c -> {
               try {
                   return Stream.of(c.getMethods());
               }
               catch (Throwable ignore) {
                   return Stream.<Method> of();
               }
           })
           .filter(m -> m.getTypeParameters().length > 0
                     && m.getParameterCount() == 0)
           .sorted(Comparator.comparing(Method::toString))
           .map(Method::toGenericString)
           .forEach(System.out::println);
    }
}
```

이 스캐너는 타입 매개변수는 있지만 매개변수가 없는 메서드를 식별하여 코드베이스에서 잠재적인 안티패턴을 드러냅니다.
