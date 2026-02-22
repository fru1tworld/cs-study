# Java 8의 메서드 참조가 오버로딩에 추가 제한을 가한다

> 원문: https://blog.jooq.org/java-8s-method-references-put-further-restrictions-on-overloading/

메서드 오버로딩은 개발자들 사이에서 항상 엇갈린 반응을 불러일으켜 왔습니다. 이 블로그에서는 이전에 여러 글에서 이 주제를 다룬 바 있습니다:

- You Will Regret Applying Overloading with Lambdas!
- Keeping things DRY: Method overloading
- Why Everyone Hates Operator Overloading
- API Designers, be Careful

## 오버로딩의 두 가지 주요 이유

메서드 오버로딩은 두 가지 주요 목적을 수행합니다:

1. 기본 인자 - 호출자가 자주 사용되는 매개변수를 생략할 수 있게 함
2. 분리된 인자 타입 대안 - 유사한 작업에 대해 다른 매개변수 타입을 수용

두 동기 모두 API의 편의성을 향상시키는 것을 목표로 합니다.

## 예제: 기본 인자

```java
public class Integer {
    public static int parseInt(String s) {
        return parseInt(s,10);
    }

    public static int parseInt(String s, int radix) {}
}
```

첫 번째 메서드는 radix 매개변수를 기본값 10으로 설정하여 편의성을 제공합니다.

## 예제: 분리된 타입 대안

```java
public class String {
    public static String valueOf(char c) {
        char data[] = {c};
        return new String(data, true);
    }

    public static String valueOf(boolean b) {
        return b ? "true" : "false";
    }
}
```

서로 다른 매개변수 타입이 일관된 의미를 유지하면서 최적화된 처리를 받습니다.

## 실용적인 변환 예제

```java
public class IOUtils {
    public static void copy(InputStream input, OutputStream output);
    public static void copy(InputStream input, Writer output);
    public static void copy(InputStream input, Writer output, String encoding);
    public static void copy(InputStream input, Writer output, Charset encoding);
}
```

이것은 기본 매개변수와 호환되지 않는 타입 대안 모두를 보여줍니다.

## 언어 설계에 대한 부연

이 글은 유니온 타입과 기본 인자가 Java의 진화에서 영구적으로 제외되었을 가능성이 높다고 언급합니다. 유니온 타입은 단순한 문법적 설탕으로 보일 수 있지만, 기본 인자는 JVM이 명명된 매개변수를 지원하지 않는 한 문제가 될 것입니다. Ceylon 언어가 이를 잘 보여주는데, 메서드 오버로딩 자체 없이도 오버로딩 사용 사례의 약 99%를 제공합니다.

## 근본적인 문제

오버로딩은 순전히 인간이 API와 상호작용하는 것을 용이하게 하기 위해 존재합니다. 런타임 실행에서 메서드 이름은 중요하지 않습니다—오직 고유한 시그니처만 중요합니다. 그러나 컴파일에는 각 Java 버전마다 더 복잡해지는 복잡한 오버로드 해결 알고리즘이 필요합니다.

## Java 8 메서드 참조 문제

Josh Bloch가 이 문제에 대한 설득력 있는 예시를 제시했습니다:

```java
static void pfc(List<Integer> x) {
    x.stream().map(Integer::toString).forEach(
    s -> System.out.println(s.charAt(0)));
}
```

Eclipse는 다음과 같이 보고합니다: "Ambiguous method reference: both toString() and toString(int) from the type Integer are eligible" (모호한 메서드 참조: Integer 타입의 toString()과 toString(int) 둘 다 적합함)

메서드 참조 문법은 다음 사이를 구분할 수 없습니다:
- 인스턴스 메서드: `i.toString()`
- 정적 메서드: `Integer.toString(i)`

## 해결 방법

람다 표현식은 즉시 모호성을 해결합니다:

```java
x.stream().map(i -> i.toString());
x.stream().map(i -> Integer.toString(i));
```

또는 상위 타입을 참조하면 모호성이 제거됩니다:

```java
x.stream().map(Object::toString);
```

## 중요한 설계 지침

이 글은 강조합니다: "유사한 인스턴스와 정적 메서드 오버로드를 절대 혼합하지 마십시오"

이 제약은 정적 오버로드가 `java.lang.Object`의 메서드를 재정의할 때 더욱 증폭되며, 이는 이전에 문서화된 바 있습니다.

## 권장 설계 패턴

단일 클래스 내에서 인스턴스와 정적 오버로드를 혼합하는 대신, JDK는 "동반 클래스"를 사용합니다:

```java
// 인스턴스 로직
public interface Collection<E> {}
public class Object {}

// 유틸리티
public class Collections {}
public final class Objects {}
```

이러한 네임스페이스 분리는 오버로딩 복잡성을 우아하게 우회합니다.

## 결론

이 글의 핵심 요점: "추가된 편의성이 정말로 가치를 더하지 않는 한 오버로딩을 피하십시오!"

Java 8이 메서드 참조를 도입한 이후 메서드 오버로딩은 점점 더 문제가 되고 있습니다. API 설계자들은 겉보기에 올바른 클라이언트 코드를 컴파일러가 거부하는 것을 방지하기 위해 더욱 주의를 기울여야 합니다.
