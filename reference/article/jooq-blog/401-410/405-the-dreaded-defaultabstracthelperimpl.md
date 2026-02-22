# 두려운 DefaultAbstractHelperImpl

> 원문: https://blog.jooq.org/the-dreaded-defaultabstracthelperimpl/

얼마 전, 우리는 Spring API 빙고라고 부르는 재미있는 게임을 공개했습니다. 이것은 다음과 같은 의미 있는 클래스 이름을 만들어내는 Spring의 엄청난 창의성에 대한 헌사이자 찬사입니다:

- FactoryAdvisorAdapterHandlerLoader
- ContainerPreTranslatorInfoDisposable
- BeanFactoryDestinationResolver
- LocalPersistenceManagerFactoryBean

위 클래스 중 두 개는 실제로 존재합니다. 어떤 것인지 찾을 수 있나요? 못 찾겠다면, Spring API 빙고를 플레이해보세요! 분명히, Spring API는 다음의 문제로 고통받고 있습니다...

## 이름 짓기

> 컴퓨터 과학에는 단 두 가지 어려운 문제만 있습니다. 캐시 무효화, 이름 짓기, 그리고 off-by-one 오류 – Tim Bray가 Phil Karlton을 인용

Java 소프트웨어에서 벗어나기 어려운 접두사나 접미사가 몇 가지 있습니다. Twitter에서의 최근 토론을 생각해보세요. 이것은 불가피하게 (매우) 흥미로운 토론으로 이어졌습니다:

> Interface: PaymentService, Implementation: PaymentServiceImpl이 있을 때, 테스트는 PaymentServiceTest가 아니라 PaymentServiceImplTest라고 불러야 합니다
>
> — Tomek Bujok (@tombujok) 2014년 10월 8일

네, `Impl` 접미사는 흥미로운 주제입니다. 왜 이것이 있고, 왜 계속 이런 방식으로 이름을 짓는 걸까요?

## 명세 vs. 구현체

Java는 기이한 언어입니다. Java가 발명될 당시, 객체 지향은 핫한 주제였습니다. 하지만 절차적 언어들도 흥미로운 기능들을 가지고 있었습니다. 당시 매우 흥미로운 언어 중 하나는 Ada였습니다(그리고 Ada에서 크게 파생된 PL/SQL도 있었습니다). Ada(PL/SQL처럼)는 프로시저와 함수를 패키지로 합리적으로 구성하며, 이는 두 가지 형태로 제공됩니다: specification(명세)과 body(본문).

Wikipedia 예제에서:

```ada
-- 명세
package Example is
  procedure Print_and_Increment (j: in Out Number);
end Example;

-- 본문
package body Example is

  procedure Print_and_Increment (j: in Out Number) is
  begin
    -- [...]
  end Print_and_Increment;

begin
  -- [...]
end Example;
```

항상 이렇게 해야 하며, 두 가지는 정확히 같은 이름 `Example`로 지어집니다. 그리고 이들은 `Example.ads`(ad는 Ada, s는 specification)와 `Example.adb`(b는 body)라는 두 개의 다른 파일에 저장됩니다. PL/SQL은 이를 따라 패키지 파일을 `Example.pks`와 `Example.pkb`로 명명합니다(pk는 Package).

Java는 주로 다형성과 클래스의 작동 방식 때문에 다른 길을 갔습니다:

- 클래스는 명세와 본문 둘 다를 하나로 담고 있습니다
- 인터페이스는 구현 클래스와 같은 이름을 가질 수 없습니다 (물론, 주로 많은 구현이 있기 때문입니다)

특히, 클래스는 명세만 있거나 부분적인 본문(추상 클래스일 때)을 가진 하이브리드일 수 있고, 완전한 명세와 본문(구체 클래스일 때)을 가질 수 있습니다.

## 이것이 Java 네이밍에 어떻게 적용되는가

모든 사람이 명세와 본문의 깔끔한 분리를 좋아하는 것은 아니며, 이것은 확실히 논쟁의 여지가 있습니다. 하지만 Ada적인 마인드셋에 있다면, 아마도 각 클래스에 대해 하나의 인터페이스를 원할 것입니다, 최소한 API가 노출되는 곳에서는요. 우리는 jOOQ에서도 같은 일을 하며, 이름을 짓는 다음과 같은 정책을 수립했습니다:

### *Impl

해당 인터페이스와 1:1 관계에 있는 모든 구현(본문)은 `Impl` 접미사가 붙습니다. 가능하다면, 우리는 이러한 구현들을 패키지 프라이빗으로 유지하고 `org.jooq.impl` 패키지 안에 봉인하려고 합니다. 예시:

- `Cursor`와 그에 해당하는 `CursorImpl`
- `DAO`와 그에 해당하는 `DAOImpl`
- `Record`와 그에 해당하는 `RecordImpl`

이 엄격한 네이밍 체계는 어떤 것이 인터페이스(따라서 공개 API)이고, 어떤 것이 구현인지 즉시 명확하게 만들어줍니다. 우리는 Java가 이 점에서 Ada와 더 비슷했으면 좋겠지만, 우리에게는 다형성이 있고, 그것은 훌륭합니다, 그리고...

### Abstract*

... 그리고 이것은 기반 클래스에서 코드를 재사용하는 것으로 이어집니다. 우리 모두 알다시피, 공통 기반 클래스는 (거의) 항상 추상적이어야 합니다. 단순히 그것들이 대부분 해당 명세의 _불완전한_ 구현(본문)이기 때문입니다. 따라서, 우리는 해당 인터페이스와 1:1 관계에 있는 많은 부분 구현을 가지고 있으며, `Abstract` 접두사를 붙입니다. 대부분의 경우, 이러한 부분 구현들도 패키지 프라이빗이고 `org.jooq.impl` 패키지 안에 봉인되어 있습니다. 예시:

- `Field`와 그에 해당하는 `AbstractField`
- `Query`와 그에 해당하는 `AbstractQuery`
- `ResultQuery`와 그에 해당하는 `AbstractResultQuery`

특히, `ResultQuery`는 `Query`를 _확장하는_ 인터페이스이고, 따라서 `AbstractResultQuery`는 `AbstractQuery`를 _확장하는_ 부분 구현이며, 이것도 역시 부분 구현입니다. 부분 구현을 갖는 것은 우리 API에서 완벽하게 이치에 맞습니다. 왜냐하면 우리 API는 내부 DSL(도메인 특화 언어)이고 따라서 구체적인 `Field`가 실제로 무엇을 하는지와 관계없이 항상 동일한 수천 개의 메서드를 가지고 있기 때문입니다 – 예를 들어 `Substring`

### Default*

우리는 모든 API 관련 작업을 인터페이스로 합니다. 이것은 다음과 같은 인기 있는 Java SE API에서 이미 매우 효과적임이 증명되었습니다:

- Collections
- Streams
- JDBC
- DOM

우리는 또한 모든 SPI(Service Provider Interface) 관련 작업도 인터페이스로 합니다. API 진화 측면에서 API와 SPI 사이에는 본질적인 차이가 하나 있습니다:

- API는 사용자가 _소비_하며, 거의 _구현_하지 않습니다
- SPI는 사용자가 _구현_하며, 거의 _소비_하지 않습니다

JDK를 개발하는 것이 아니라면(따라서 완전히 미친 하위 호환성 규칙이 없다면), _API_ 인터페이스에 새 메서드를 추가하는 것은 아마도 대부분 안전합니다. 실제로, 우리는 매 마이너 릴리스마다 그렇게 합니다. 왜냐하면 아무도 우리 DSL을 구현하리라 기대하지 않기 때문입니다(`Field`의 286개 메서드나 `DSL`의 677개 메서드를 누가 구현하려 하겠습니까? 그건 미친 짓입니다!). 하지만 SPI는 다릅니다. `*Listener`나 `*Provider`와 같은 접미사가 붙은 SPI를 사용자에게 제공할 때, 새 메서드를 그냥 간단히 추가할 수 없습니다 – 최소한 Java 8 이전에는요, 구현을 깨뜨릴 것이고, 그런 구현이 많이 있기 때문입니다. 뭐, 우리는 여전히 그렇게 합니다, 왜냐하면 JDK 하위 호환성 규칙이 없기 때문입니다. 우리는 더 완화된 규칙을 가지고 있습니다. 하지만 우리는 사용자들이 인터페이스를 직접 구현하지 말고 대신 비어있는 `Default` 구현을 확장할 것을 제안합니다. 예를 들어 `ExecuteListener`와 그에 해당하는 `DefaultExecuteListener`:

```java
public interface ExecuteListener {
    void start(ExecuteContext ctx);
    void renderStart(ExecuteContext ctx);
    // [...]
}

public class DefaultExecuteListener
implements ExecuteListener {

    @Override
    public void start(ExecuteContext ctx) {}

    @Override
    public void renderStart(ExecuteContext ctx) {}

    // [...]
}
```

그래서, `Default*`는 API 소비자가 사용하고 인스턴스화할 수 있는 단일 공개 구현을 제공하거나, SPI 구현자가 하위 호환성 문제의 위험 없이 확장할 수 있도록 하는 데 일반적으로 사용되는 접두사입니다. 이것은 Java 6/7의 인터페이스 default 메서드 부재에 대한 해결책이라고 할 수 있으며, 이것이 접두사 네이밍이 더욱 적절한 이유입니다.

### 이 규칙의 Java 8 버전

사실, 이 관행은 Java-8 호환 SPI를 지정하는 "좋은" 규칙이 인터페이스를 사용하고 _모든_ 메서드를 빈 본문의 _default_로 만드는 것임을 분명하게 합니다. jOOQ가 Java 6을 지원하지 않았다면, 우리는 아마도 `ExecuteListener`를 이렇게 지정했을 것입니다:

```java
public interface ExecuteListener {
    default void start(ExecuteContext ctx) {}
    default void renderStart(ExecuteContext ctx) {}
    // [...]
}
```

### *Utils 또는 *Helper

좋아요, 여기 mock/테스트/커버리지 전문가와 애호가들을 위한 것이 있습니다. 모든 종류의 정적 유틸리티 메서드를 위한 "덤프"를 갖는 것은 _완전히 괜찮습니다_.

> 제발, "그런 사람"이 되지 마세요! :-)

유틸리티 클래스를 식별하는 다양한 기법이 있습니다. 이상적으로는, 네이밍 컨벤션을 정하고 그것을 고수합니다. 예를 들어 _*Utils_. 우리 관점에서, 이상적으로는 매우 특정한 도메인에 엄격하게 묶이지 않은 모든 유틸리티 메서드를 단일 클래스에 모두 덤프할 것입니다. 왜냐하면 솔직히, 그 유틸리티 메서드를 찾기 위해 수백만 개의 클래스를 살펴보는 것을 언제 마지막으로 좋아하셨나요? 결코 없습니다. 우리는 `org.jooq.impl.Utils`를 가지고 있습니다. 왜냐고요? 이렇게 할 수 있게 해주기 때문입니다:

```java
import static org.jooq.impl.Utils.*;
```

이것은 그러면 거의 당신의 애플리케이션 전체에 "최상위 함수"와 같은 것이 있는 것처럼 느껴집니다. "전역" 함수. 우리는 이것이 좋은 것이라고 생각합니다. 그리고 우리는 "이것을 mock할 수 없다"는 주장을 전혀 받아들이지 않으니, 토론을 시작하려고 시도조차 하지 마세요.

### 토론

... 또는, 사실, 토론을 시작해봅시다. 당신의 기법은 무엇이고, 왜 그런가요? 시작하는 데 도움이 되도록, Tom Bujok의 원래 트윗에 대한 몇 가지 반응이 있습니다:

> @tombujok 아니요. PaymentServiceImplTestImpl!
>
> — Konrad 'ktoso' Malawski (@ktosopl) 2014년 10월 8일

> @tombujok 인터페이스를 없애세요
>
> — Simon Martinelli (@simas_ch) 2014년 10월 8일

> @tombujok 모든 것에 Impl!
>
> — Bartosz Majsak (@majson) 2014년 10월 8일

> @tombujok @lukaseder @ktosopl 근본 원인은 클래스가 *Impl이라고 불리면 *안 된다*는 것입니다, 하지만 당신이 어차피 우리 모두를 트롤링하고 있었다는 걸 알아요
>
> — Peter Kofler (@codecopkofler) 2014년 10월 9일

시작해봅시다 ;-)

---

## 댓글

### 댓글 1
Developer Dude - 2014년 10월 21일 18:35

Util/Helper 클래스를 무엇이든 위한 "덤프"로 사용하는 것에 동의하지 않습니다.

저는 현재 helpers, advisors, managers, brokers, addins, utils, helperadvisorbrokerutilsadinfinitum이 있는 매우 지저분한 레거시 코드베이스를 리팩토링하고 있습니다.

클래스는 이름이 무엇이든 간에 간결하고 일관성이 있어야 합니다. 정적 메서드를 위한 덤핑 그라운드가 되어서는 안 됩니다. 그렇지 않으면 다른 쓰레기 덤프처럼 매우 지저분하고 비일관적이고 뒤죽박죽인 쓰레기 덤프가 될 가능성이 높습니다.

네이밍에 관해서는 – 클래스의 이름은 그것이 하는 일에 최대한 가까워야 합니다. helper가 뭔가요? 무엇을 돕는 건가요? 개발자들은 이러한 클래스에 아무거나 다 던지는 경향이 있습니다. 일관성 있고 간결한 인터페이스를 가진 Apache StringUtils 같은 것을 만들 규율과 인내심을 가진 사람은 거의 없습니다.

그리고 아니요 – 저는 Impl이 전혀 마음에 들지 않습니다. 예전에는 사용했었지만, 그것이 중복이라고 결정했습니다. Abstract와 때때로 Default는 여전히 사용하지만, 드물게요.

요즘에는 IDE와 코드를 빠르게 훑어보는 것만으로도 무언가가 인터페이스인지 아닌지 알 수 있습니다. 추상 클래스도 마찬가지입니다. Default 구현 – 이것들은 이름에 표기할 가치가 있습니다.

### 댓글 2
lukaseder - 2014년 10월 22일 00:24

helper 덤프에 대한 비판에 본질적으로 동의합니다. 목표는 `StringUtils`와 같은 것이어야 합니다. 두 가지를 고려해야 합니다:

1. Java는 최상위 함수를 지원하지 않습니다. 우리는 그것들을 어떤 클래스에 넣어야 합니다 (제 요점)
2. ... 이런 이유로, 그 클래스는 그 "최상위" 함수들을 쉽게 찾을 수 있는 방식으로 이름 지어져야 합니다 (당신의 요점)

저도 Impl이 마음에 들지 않지만, 명세와 구현이 1:1 관계에 있을 때, 지금까지 더 나은 해결책을 찾지 못했습니다.

### 댓글 3
Michael Scharhag (@mscharhag) - 2014년 10월 26일 11:10

첫 번째 단락을 제외하고는 당신의 글 대부분에 동의합니다. 제 생각에 Spring은 네이밍 문제가 있는 것과는 거리가 멉니다. 많은 클래스 이름이 일반 용어(adapter, factory, resolver, delegate 등)의 블랙 매직 믹스처럼 들릴 수 있다는 것은 알겠습니다. 하지만 Spring의 개념에 익숙하다면 많은 클래스 이름이 매우 자기 설명적이 됩니다.

Spring에서 가장 긴 클래스 이름 중 일부를 만드는 몇 가지 예시가 있습니다:

*HandlerMethodArgumentResolver:
Spring의 웹 프레임워크에서 웹 요청을 처리하는 클래스를 Handlers라고 합니다. 이러한 Handler는 일반적으로 들어오는 요청을 받아 어떤 종류의 응답을 생성하는 하나 이상의 메서드(=HandlerMethod)를 가집니다.
Handler 메서드는 이러한 handler 메서드가 호출될 때 Spring이 전달하는 다양한 인수를 가질 수 있습니다. 예를 들어: handler 메서드가 Locale 타입의 파라미터를 정의하면, Spring은 자동으로 요청 locale을 전달합니다.
이러한 인수는 HandlerMethodArgumentResolver에 의해 해결됩니다 (꽤 명확한 이름이죠?).
HandlerMethodArgumentResolver는 많은 다른 구현을 가진 인터페이스입니다. 예를 들어:
PathVariableMethodArgumentResolver – 요청된 url에서 path 변수를 해결
MatrixVariableMapMethodArgumentResolver – matrix 변수를 해결
ServletCookieValueMethodArgumentResolver – cookie 인수를 해결

*BeanPostProcessor
BeanPostProcessor는 빈이 생성된 후 커스텀 수정을 구현하는 데 사용할 수 있습니다. 이것은 다양한 기능을 구현하기 위해 Spring 자체에서 많이 사용됩니다. 다시 말하지만, BeanPostProcessor는 그러한 인터페이스에 대해 최악의 이름은 아니라고 생각합니다.
구현 이름도 매우 설명적입니다:
RequiredAnnotationBeanPostProcessor – 새로 생성된 빈에서 @Required 어노테이션을 처리
InitDestroyAnnotationBeanPostProcessor – 생성된 빈에서 init/destroy 메서드 호출을 담당
PersistenceExceptionTranslationPostProcessor – 다양한 영속성 솔루션의 예외를 Spring의 DataAccessException으로 변환

더 긴 이름들:

BufferedImageHttpMessageConverter:
BufferedImage 객체를 읽고/쓰는 데 사용할 수 있는 HttpMessageConverter의 구현

QualifierAnnotationAutowireCandidateResolver:
@Qualifier 어노테이션을 기반으로 자동 와이어링에 적합한 후보를 해결하는 AutowireCanditateResolver

JodaDateTimeFormatAnnotationFormatterFactory:
Joda-Time을 사용하여 DateTimeFormat 어노테이션을 처리하기 위한 AnnotationFormatterFactory

Jsr310DateTimeFormatAnnotationFormatterFactory:
JSR-310 Date/Time API를 사용하여 DateTimeFormat 어노테이션을 처리하기 위한 AnnotationFormatterFactory

BeanFactoryDestinationResolver(당신의 예시)는 들어본 적이 없습니다. 하지만 Spring의 DestinationResolver 인터페이스가 무엇에 사용되는지는 압니다. 그래서 BeanFactoryDestinationResolver가 무엇을 할 수 있는지 매우 명확해집니다.

일부 이름이 매우 길 수 있지만, 클래스 이름만 읽어도 클래스가 무엇을 하는지 꽤 잘 추측할 수 있는 경우가 많습니다. 3천 개 이상의 클래스를 가진 프레임워크로서는 이것이 상당한 성과라고 생각합니다.

### 댓글 4
lukaseder - 2014년 10월 27일 20:12

두 가지 포인트가 있습니다 (그 부분이 다른 무엇보다도 "재미있는" 불평에 가깝다는 사실은 별개로):

1. 저는 그 타입들의 2/3가 공개 API의 일부가 될 필요가 있다고 의심합니다. 정말로 의심합니다
2. 서브타입 다형성은 Spring에서 완전히 과도하게 사용되며, 제네릭 다형성이 훨씬 나았을 것입니다
3. 저는 개인적으로 Spring의 2/3가 일반적으로 정말로 필요한지 의심합니다. JodaDateTimeFormatAnnotationFormatterFactory 같은 것의 요점을 정말 모르겠습니다. 이것은 한 줄짜리 코드와 `String.format()` 정도로 작성할 수 있다고 확신합니다. 하지만 그건 KISS를 추구하는 제 생각일 수도 있습니다 ;-)
