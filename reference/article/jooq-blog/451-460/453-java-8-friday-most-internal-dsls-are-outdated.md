# Java 8 금요일: 대부분의 내부 DSL은 구식이다

> 원문: https://blog.jooq.org/java-8-friday-most-internal-dsls-are-outdated/

Data Geekery에서 우리는 Java를 사랑합니다. 그리고 jOOQ의 유창한 API와 쿼리 DSL에 깊이 빠져 있기 때문에, Java 8이 우리 생태계에 가져다 줄 것들에 대해 정말 기대하고 있습니다.

## Java 8 금요일

매주 금요일, 우리는 람다 표현식, 확장 메서드 및 기타 훌륭한 것들을 활용하는 새로운 튜토리얼 스타일의 Java 8 기능을 소개합니다. 소스 코드는 GitHub에서 확인할 수 있습니다.

## 대부분의 내부 DSL은 구식이다

이것은 현재 시장에서 가장 진보된 내부 DSL 중 하나를 제공하는 벤더가 하기에는 대담한 진술입니다. 설명하겠습니다.

## 언어는 어렵다

새로운 언어나 API를 배운다는 것은 키워드, 구문, 명령문 유형, 표현식을 배워야 한다는 것을 의미합니다. 외부 DSL, 내부 DSL, 일반 API 모두 마찬가지입니다.

JUnit 사용자들은 6개 언어로 제공되는 hamcrest 매처를 채택해 왔습니다. 도메인 특화 언어 관용구는 산문처럼 읽힙니다:

```java
assertThat(theBiscuit, equalTo(myBiscuit));
assertThat(theBiscuit, is(equalTo(myBiscuit)));
assertThat(theBiscuit, is(myBiscuit));
```

그러나 이 코드를 작성하려면 메서드가 어디서 오는지, 어떤 메서드를 사용할 수 있는지, 커스텀 Matcher를 어떻게 확장하는지, 그리고 모범 사례가 무엇인지 이해해야 합니다.

커스텀 매처 예제는 그 복잡성을 보여줍니다:

```java
public class IsNotANumber
extends TypeSafeMatcher<Double> {

  @Override
  public boolean matchesSafely(Double number) {
    return number.isNaN();
  }

  public void describeTo(Description description) {
    description.appendText("not a number");
  }

  @Factory
  public static <T> Matcher<Double> notANumber() {
    return new IsNotANumber();
  }
}
```

DSL은 만들기 쉽고 재미있을 수도 있지만, 유지보수 위험을 초래합니다. 커스텀 DSL은 함수형 대안보다 낫지 않으며 유지보수하기가 더 어렵습니다.

## DSL을 함수로 대체하기

간단한 Java 8 테스트 API를 생각해 봅시다:

```java
static <T> void assertThat(
    T actual,
    Predicate<T> expected
) {
    assertThat(actual, expected, "Test failed");
}

static <T> void assertThat(
    T actual,
    Predicate<T> expected,
    String message
) {
    assertThat(() -> actual, expected, message);
}

static <T> void assertThat(
    Supplier<T> actual,
    Predicate<T> expected
) {
    assertThat(actual, expected, "Test failed");
}

static <T> void assertThat(
    Supplier<T> actual,
    Predicate<T> expected,
    String message
) {
    if (!expected.test(actual.get()))
        throw new AssertionError(message);
}
```

hamcrest 표현식과 함수형 대안을 비교해 봅시다:

```java
// 이전
assertThat(theBiscuit, equalTo(myBiscuit));
assertThat(theBiscuit, is(equalTo(myBiscuit)));
assertThat(theBiscuit, is(myBiscuit));
assertThat(Math.sqrt(-1), is(notANumber()));

// 이후
assertThat(theBiscuit, b -> b == myBiscuit);
assertThat(Math.sqrt(-1), n -> Double.isNaN(n));
```

람다 표현식과 잘 설계된 `assertThat()` API가 있다면, 개발자들은 매처 기반 단언 메서드를 찾지 않을 것입니다. 메서드 참조는 더 깔끔한 문법을 제공합니다:

```java
static void assertThat(
    double actual,
    DoublePredicate expected
) { ... }

// 사용법
assertThat(Math.sqrt(-1), Double::isNaN);
```

## 그래도...

람다와 스트림은 매처와 결합될 수 있습니다. jOOQ 통합 테스트가 이를 보여줍니다:

```java
String dialectString =
    System.getProperty("org.jooq.test-dialects");

assumeThat(dialectString, not(isOneOf("", null)));

assumeThat(
    dialect().name().toLowerCase(),
    isOneOf(stream(dialectString.split("[,;]"))
        .map(String::trim)
        .map(String::toLowerCase)
        .toArray(String[]::new))
);
```

그러나 같은 결과를 더 간단하게 얻을 수 있습니다:

```java
assumeThat(dialectString, StringUtils::isNotEmpty);
assumeThat(
    dialect().name().toLowerCase(),
    d -> stream(dialectString.split("[,;]"))
        .map(String::trim)
        .map(String::toLowerCase())
        .anyMatch(d::equals)
);
```

일반 람다와 스트림은 Hamcrest 매처와 그 DSL의 필요성을 없앱니다. 개발자들이 일상 업무에서 Streams API에 익숙해짐에 따라, Hamcrest의 낯섦은 더 커질 것입니다. JUnit 유지관리자들은 Java 8 API를 선호하여 Hamcrest를 폐기하는 것을 고려해야 합니다.

## Hamcrest는 이제 나쁜 것인가?

Hamcrest는 그 역사적 목적을 다했고, 개발자들은 이에 익숙해졌습니다. 그러나 Java 개발자들은 "지난 10년간 잘못된 방향으로 가고 있었습니다." 람다 표현식의 부재는 비대해지고 이제는 쓸모없는 라이브러리들을 만들어냈습니다. 많은 내부 DSL과 어노테이션 기반 접근 방식들도 비슷한 문제를 겪고 있습니다 - 원래 문제를 해결하지 못해서가 아니라 Java 8 준비가 되어 있지 않기 때문입니다.

Hamcrest의 Matcher 타입은 함수형 인터페이스가 아니지만, 변환은 간단할 것입니다. CustomMatcher 로직은 default 메서드로 Matcher 인터페이스로 마이그레이션되어야 합니다. AssertJ와 같은 대안들은 람다와 Streams API를 통해 쓸모없어진 DSL을 만들었습니다. DSL을 고집하는 개발자들에게는 Spock이 더 나은 선택입니다.

## 다른 예제들

Hamcrest는 표준 JDK 8 구문과 잠재적으로 JUnit에 도착할 유틸리티 메서드를 통한 DSL 쓸모없음을 예시합니다. Java 8은 수십 년 된 DSL 논쟁에 상당한 영향을 미칠 것입니다. Streams API가 데이터 변환과 구축 접근 방식을 변환시키기 때문입니다. 많은 현재 DSL은 Java 8 준비가 되어 있지 않으며 함수형으로 설계되지 않았습니다. 함수로 더 잘 모델링될 수 있는 어려운 개념들을 위한 과도한 키워드를 가지고 있습니다.

예외도 있습니다: jOOQ나 jRTF와 같은 DSL은 실제 외부 DSL을 1:1 방식으로 모델링하여 기존 키워드와 구문 요소를 상속받아 초기 학습을 더 쉽게 합니다.

## 당신의 생각은?

이러한 가정에 대해 어떤 의견을 가지고 계신가요? 어떤 좋아하는 내부 DSL이 Java 8로 인해 5년 내에 사라지거나 변형될 것이라고 생각하시나요?

---

## 댓글 섹션 요약

Duncan은 Hamcrest의 주요 가치가 단언 실패를 설명하는 것이라고 지적했습니다 - 테스팅에 필수적입니다.

Tim은 이 글이 Hamcrest의 재사용성과 조합성 강점을 놓쳤다고 제안했지만, 람다와 default 메서드가 Hamcrest의 문법을 크게 향상시킬 수 있다고 인정했습니다.

Jason W. Thompson은 동의하지 않았으며, 품질 매처가 단순한 술어보다 복잡한 객체 단언을 더 잘 처리한다고 강조했습니다. 특히 오류 보고 세부 사항과 관련하여.

저자는 대화를 인정하면서도 함수형 프로그래밍 채택이 결국 주류 Java 개발에서 매처 기반 접근 방식보다 람다 표현식과 Streams API를 선호하게 될 것이라고 반복했습니다.
