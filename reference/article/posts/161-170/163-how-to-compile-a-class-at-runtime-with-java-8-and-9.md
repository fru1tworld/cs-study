> 원문: https://blog.jooq.org/how-to-compile-a-class-at-runtime-with-java-8-and-9/

# Java 8과 9에서 런타임에 클래스를 컴파일하는 방법

`java.compiler` 모듈을 사용하여 런타임에 클래스를 컴파일할 수 있다는 것은 정말 유용합니다. 예를 들어, 데이터베이스에서 Java 소스 파일을 로드하고, 즉석에서 컴파일하여, 마치 애플리케이션의 일부인 것처럼 해당 코드를 실행할 수 있습니다.

jOOR 0.9.8에서 이 기능을 구현했습니다. 이제 다음과 같이 간단하게 작성할 수 있습니다:

```java
Supplier<String> supplier = Reflect.compile(
    "com.example.CompileTest",
    "package com.example;\n" +
    "class CompileTest\n" +
    "implements java.util.function.Supplier<String> {\n" +
    "  public String get() {\n" +
    "    return \"Hello World!\";\n" +
    "  }\n" +
    "}\n"
).create().get();
```

위 예제에서는 `Supplier<String>`을 구현하는 클래스를 런타임에 컴파일합니다. jOOR 라이브러리를 사용하면 동적으로 클래스를 컴파일하고 인스턴스화하는 과정을 최소한의 코드로 처리할 수 있습니다.

## Java 8과 Java 9+의 구현 차이점

Java 9의 더 엄격한 모듈 시스템(Module System)으로 인해 두 가지 버전이 필요합니다.

### Java 8 방식

Java 8에서는 리플렉션(Reflection)을 사용하여 내부 JDK API에 접근하고, ClassLoader의 `defineClass`를 호출합니다.

### Java 9+ 방식

Java 9 이상에서는 더 정교한 기술이 필요합니다:

- `StackWalker`를 사용하여 호출하는 클래스를 식별
- 같은 패키지의 컴파일된 클래스에 대해 `MethodHandles.privateLookupIn()` 사용
- 다른 패키지 간 시나리오를 위한 커스텀 ClassLoader 사용

Java 9에서 도입된 `MethodHandles.Lookup.defineClass()`를 활용하는데, 호출자 컨텍스트를 결정하고 패키지-프라이빗(package-private) 접근을 관리하기 위해 더 복잡한 스택 워킹(stack walking)이 필요합니다.

## 기술적 과제

호출하는 클래스를 식별하기 위해 `StackWalker`를 사용해야 하며, 컴파일된 클래스가 호출자와 같은 패키지를 공유하는지 여부를 판단해야 합니다. 이는 클래스 계층 구조에서 private 인터페이스에 대한 접근에 영향을 미칩니다.

## 결론

리플렉션은 Java 9에서 확실히 더 간단해지지 않았습니다! 모듈 기반 접근 제어를 처리하는 복잡성이 증가했기 때문에, 이전에는 간단했던 리플렉션 기반 코드가 더 복잡해졌습니다.

## 실용적인 활용

런타임 컴파일은 다음과 같은 시나리오를 가능하게 합니다:
- 데이터베이스에서 Java 소스를 로드
- 애플리케이션 내에서 동적으로 컴파일된 코드 실행
