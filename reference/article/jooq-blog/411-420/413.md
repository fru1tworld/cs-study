# 이것이 최종 토론이다!

> 원문: https://blog.jooq.org/this-is-the-final-discussion/

말장난 의도함... Java의 `final`에 대해 토론해 봅시다. 최근, 우리의 인기 블로그 게시물 ["Java 코딩 시 10가지 미묘한 모범 사례"](https://blog.jooq.org/10-subtle-best-practices-when-coding-java/)가 JavaWorld에서 요약되어 링크되면서 상당한 부활을 이루었고 새로운 댓글들이 달렸습니다. 특히, JavaWorld 편집자들은 Java 키워드 "`final`"에 대한 우리의 의견에 이의를 제기했습니다:

> 더 논란이 되는 것은, Eder가 메서드를 기본적으로 final로 만드는 것이 안전한지에 대한 질문을 다룬다는 것입니다: "만약 모든 소스 코드를 완전히 제어할 수 있다면, 메서드를 기본적으로 final로 만드는 것에 전혀 문제가 없습니다. 왜냐하면:" "메서드를 오버라이드해야 한다면(정말 그래야 하나요?), 언제든지 final 키워드를 제거할 수 있습니다" "더 이상 실수로 어떤 메서드도 오버라이드하지 않게 됩니다"

네, 정말 그렇습니다. 모든 클래스, 메서드, 필드 및 지역 변수는 기본적으로 final이어야 하고 키워드를 통해 변경 가능해야 합니다. 다음은 필드와 지역 변수입니다:

```java
int finalInt   = 1;
val int finalInt   = 2;
var int mutableInt = 3;
```

Scala/C# 스타일의 `val` 키워드가 정말 필요한지는 논쟁의 여지가 있습니다. 하지만 분명히, 필드/변수를 다시 수정하려면, 이를 명시적으로 허용하는 키워드가 있어야 합니다. 메서드도 마찬가지입니다 - 그리고 저는 일관성과 규칙성 향상을 위해 Java 8의 `default` 키워드를 사용하고 있습니다:

```java
class FinalClass {
    void finalMethod() {}
}

default class ExtendableClass {
            void finalMethod      () {}
    default void overridableMethod() {}
}
```

이것이 우리 의견으로는 완벽한 세상이겠지만, Java는 반대로 `default`(오버라이드 가능, 변경 가능)를 기본값으로 하고 `final`(오버라이드 불가, 불변)을 명시적 옵션으로 합니다.

## 뭐, 그것과 함께 살아가겠습니다

... 그리고 API 설계자로서 (물론 [jOOQ](https://www.jooq.org) API에서), 우리는 위에서 언급한 더 합리적인 기본값을 Java가 가지고 있는 것처럼 최소한 가장하기 위해 모든 곳에 `final`을 기꺼이 넣을 것입니다. 하지만 많은 사람들이 이 평가에 동의하지 않으며, 대부분 같은 이유 때문입니다:

> osgi 환경에서 주로 작업하는 사람으로서, 저도 전적으로 동의합니다. 하지만 다른 API 설계자가 같은 방식으로 느꼈다고 보장할 수 있나요? 저는 사용자가 기본적으로 확장할 수 있는 것에 제한을 두어 사용자의 실수를 사전에 방지하는 것보다 API 설계자의 실수를 사전에 방지하는 것이 더 낫다고 생각합니다. – [eliasv on reddit](http://www.reddit.com/r/java/comments/2fgo3t/whiley_final_should_be_default_for_classes_in_java/ck92cu3)

또는...

> 강력히 반대합니다. 저는 차라리 public 라이브러리에서 final과 private를 금지하고 싶습니다. 정말로 무언가를 확장해야 하는데 그럴 수 없을 때 너무 고통스럽습니다. 의도적으로 코드를 잠그는 것은 두 가지를 의미할 수 있습니다. 그것이 형편없거나, 완벽하거나. 하지만 완벽하다면, 아무도 그것을 확장할 필요가 없으니, 왜 그것에 대해 신경 쓰나요. 물론 final을 사용해야 할 유효한 이유가 있지만, 새 버전의 라이브러리로 누군가를 깨뜨릴 것에 대한 두려움은 그중 하나가 아닙니다. – [meotau on reddit](http://www.reddit.com/r/java/comments/2fgo3t/whiley_final_should_be_default_for_classes_in_java/ck90xkn)

또는...

> 이에 대해 이미 매우 유용한 대화를 나눴다는 것을 알지만, 이 스레드의 다른 분들에게 상기시켜 드리자면: 'final'에 대한 논쟁의 많은 부분은 컨텍스트에 달려 있습니다: 이것이 public API인가요, 아니면 내부 코드인가요? 전자의 경우, final에 대한 좋은 논거가 있다는 데 동의합니다. 후자의 경우, final은 거의 항상 나쁜 아이디어입니다. – [Charles Roth on our blog](https://blog.jooq.org/10-subtle-best-practices-when-coding-java/#comment-55722)

이러한 모든 주장은 한 방향으로 향하는 경향이 있습니다: "우리는 형편없는 코드에서 작업하고 있으니 고통을 덜기 위해 최소한 어떤 우회 방법이 필요합니다."

## 하지만 이렇게 생각해 보면 어떨까요:

위의 모든 사람들이 염두에 두고 있는 API 설계자들은 정확히 당신이 확장을 통해 패치하고 싶어하는 그 끔찍한 API를 만들 것입니다. 공교롭게도, 같은 API 설계자는 `final` 키워드의 유용성과 의사소통성에 대해 반성하지 않을 것이고, 따라서 Java 언어에서 요구하지 않는 한 절대 사용하지 않을 것입니다. 윈-윈(비록 형편없는 API, 불안정한 우회 방법과 패치가 있지만). 자신의 API에 final을 사용하고 싶어하는 API 설계자들은 API(그리고 잘 정의된 확장 포인트/SPI)를 올바르게 설계하는 방법에 대해 많이 반성할 것이므로, 무언가가 `final`인 것에 대해 걱정할 필요가 없을 것입니다. 다시 말해, 윈-윈(그리고 멋진 API). 게다가, 후자의 경우, 해킹하려는 이상한 해커가 오직 고통과 괴로움으로 이어질 방식으로 당신의 API를 해킹하고 깨뜨리는 것을 막을 수 있지만, 그것은 실제로 손실이 아닙니다.

## Final 인터페이스 메서드

앞서 언급한 이유들로, 저는 여전히 Java 8 인터페이스에서 `final`이 불가능하다는 것을 깊이 유감스럽게 생각합니다. [Brian Goetz가 이것이 왜 그렇게 결정되었는지에 대해 훌륭한 설명을 제공했습니다](https://stackoverflow.com/a/23476994/521799). 사실, 평소의 설명입니다. 이것이 변경의 주요 설계 목표가 아니었다는 것에 대한 것 ;-) 하지만 만약 우리가 다음과 같이 가지고 있다면 언어의 일관성, 규칙성에 대해 생각해 보세요:

```java
default interface ImplementableInterface {
            void abstractMethod   () ;
            void finalMethod      () {}
    default void overridableMethod() {}
}
```

(몸을 숨기고 도망감...) 또는, `default`를 기본값으로 하는 우리의 현상 유지에서 더 현실적으로:

```java
interface ImplementableInterface {
          void abstractMethod   () ;
    final void finalMethod      () {}
          void overridableMethod() {}
}
```

## 마지막으로

그래서 다시, 이 토론에 대한 당신의 (최종) 생각은 무엇인가요? 아직 충분히 듣지 못했다면, [whiley 프로그래밍 언어](http://whiley.org/)의 저자인 [Dr. David Pearce](http://homepages.ecs.vuw.ac.nz/~djp/)의 [이 훌륭한 게시물도 읽어보세요](http://whiley.org/2011/12/06/final-should-be-default-for-classes-in-java/).
