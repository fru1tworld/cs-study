# Op4j와 Lambda-J. Java의 더 나은 유창성을 위해

> 원문: https://blog.jooq.org/op4j-and-lambda-j-for-more-fluency-in-java/

저는 최근 Java의 `Arrays.asList()`와 같은 간단한 구성 요소가 충분히 활용되지 않고 있다는 내용에 대해 블로그에 글을 썼습니다: https://blog.jooq.org/javas-arrays-aslist-is-underused/

저는 유창한(fluent) API로 작업하는 것을 좋아합니다. 하지만 언어 확장, 연산자 오버로딩, 진정한 제네릭, 확장 메서드, 클로저, 람다 표현식, 함수형 구성 요소 등의 기능을 지원하는 다른 언어들과 비교하면, Java 세계에서 유창한 API는 여전히 꽤 드문 편입니다. 그래도 저는 Java의 JVM과 전반적인 문법을 좋아합니다. 그리고 존재하는 수많은 라이브러리들도요. 최근 Op4j라는 정말 멋져 보이는 라이브러리를 발견했습니다: http://www.op4j.org/

이 라이브러리는 제가 매일 사용하고 싶은 종류의 구성 요소를 정확히 갖추고 있습니다. 몇 가지 예시를 보겠습니다(문서에서 가져옴):

```java
// 항상 Op.*를 주요 진입점으로 정적 임포트합니다
import static org.op4j.Op.*;
import static org.op4j.functions.FnString.*;

// 배열을 대문자로 변환
String[] values = ...;
List<String> upperStrs =
  on(values).toList().map(toUpperCase()).get();

// 문자열을 정수로 변환
String[] values = ...;
List<Integer> intValueList =
  on(values).toList().forEach().exec(toInteger()).get();
```

문서 페이지에는 더 많은 예시가 있으며, API는 방대하고 상당히 확장 가능해 보입니다: http://www.op4j.org/apidocs/op4j/index.html

이 라이브러리는 Lambda-J를 떠올리게 합니다. Lambda-J는 정적인 방식으로 클로저/람다와 유사한 표현식을 도입하여 Java에 더 많은 유창성을 가져오려는 또 다른 시도입니다: https://code.google.com/p/lambdaj/

처음 보기에 Op4j는 더 객체 지향적이고 직관적으로 보이는 반면, Lambda-J는 인스트루멘테이션과 리플렉션의 고급 사용에 의존하는 것으로 보입니다. 다소 복잡한 Lambda-J 사용 예시를 보겠습니다:

```java
Closure println = closure(); {
  of(System.out).println(var(String.class));
}
```

위 문법은 이해하기 쉽지 않습니다. `closure()`는 라이브러리의 어떤 정적(ThreadLocal) 상태를 수정하는 것으로 보이며, 이후 정적 메서드 `of()`에서 이를 사용할 수 있습니다. `of()`는 어떤 타입의 매개변수든 받을 수 있으며, 그 동일성과 타입(!)을 가정합니다. 어떻게든, 이후 정의된 클로저에 String 타입의 객체를 "적용(apply)"할 수 있습니다:

```java
println.apply("one");
println.each("one", "two", "three");
```
