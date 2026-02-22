# Java 8 가상 확장 메서드

> 원문: https://blog.jooq.org/java-8-virtual-extension-methods/

저는 한동안 Java 8 Lambda 표현식 프로젝트의 발전 과정을 지켜봐 왔으며, 현재의 진행 상태에 대해 정말 기대가 됩니다. 제가 찾은 가장 최근의 "이해하기 쉬운" 프레젠테이션은 다음과 같습니다: [http://blogs.oracle.com/briangoetz/resource/devoxx-lang-lib-vm-co-evol.pdf](http://blogs.oracle.com/briangoetz/resource/devoxx-lang-lib-vm-co-evol.pdf)

이제, API 설계자로서 저는 가상 확장 메서드의 개념에 특히 관심이 있으며, "default" 확장 메서드뿐만 아니라 "final" 확장 메서드를 도입하는 것이 고려되었는지 궁금했습니다. 예를 들어:

```java
interface A {
  void a();
  void b() default { System.out.println("b"); };
  void c() final { System.out.println("c"); };
}
```

위의 인터페이스 A를 구현할 때, 다음과 같습니다:

- `a()`는 반드시 구현해야 합니다
- `b()`는 구현/오버라이드할 수 있습니다
- `c()`는 오버라이드할 수 없습니다

장점:

1. API 설계자들이 클라이언트 코드가 기본 구현을 "불법적으로" 오버라이드하는 위험 없이 편의 메서드를 더 쉽게 만들 수 있습니다. 이것이 "final"의 주요 목적 중 하나입니다.
2. 람다 표현식이 순수한 "함수형 인터페이스"(단일 메서드 인터페이스)에만 제한될 필요가 없습니다. 함수형 인터페이스가 임의의 수의 final 확장 메서드를 가지더라도 여전히 "함수형"일 수 있기 때문입니다. 예를 들어, 위의 인터페이스 A는 `b()`가 제거되거나 `b()`도 final로 선언된다면 함수형 인터페이스가 됩니다.
3. 확장 메서드가 일반 메서드와 더 많은 특성을 공유하게 됩니다. 일반 메서드도 final일 수 있기 때문입니다. 리플렉션 API와 JVM에 있어서도 이점이 될 것으로 생각합니다.
4. JVM은 확장 메서드를 지원하기 위해 어차피 수정됩니다. Java 8의 추진력을 이 기능에도 활용할 수 있을 것입니다. 즉, 지금이 이것에 대해 생각할 적기입니다.

단점:

"다이아몬드 인터페이스 상속"의 경우, 클래스가 여러 개의 충돌하는 final 메서드 구현을 상속받을 수 있습니다. 이는 기존 코드에서 새로운 컴파일 오류를 발생시킬 수 있습니다. 이 하위 호환성 부재가 가장 큰 단점이라고 생각합니다.

다중 상속 자체와 마찬가지로, 신중한 API 설계자는 final 확장 메서드를 사용하여 자신의 API를 더욱 개선할 수 있는 반면, 덜 신중한 API 설계자는 클라이언트 코드를 깨뜨릴 수 있습니다. 하지만 이는 이전의 "final" 사용에서도 마찬가지이므로, final 확장 메서드는 Java 8에 매우 훌륭한 추가 기능이 될 것이라고 생각합니다. 전체 메일과 후속 논의는 lambda-dev 메일링 리스트에서 확인하세요: [http://mail.openjdk.java.net/pipermail/lambda-dev/2011-December/004426.html](http://mail.openjdk.java.net/pipermail/lambda-dev/2011-December/004426.html)
