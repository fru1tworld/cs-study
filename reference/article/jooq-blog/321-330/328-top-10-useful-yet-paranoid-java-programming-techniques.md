# 유용하지만 편집증적인 Java 프로그래밍 기법 Top 10

> 원문: https://blog.jooq.org/top-10-useful-yet-paranoid-java-programming-techniques/

거의 20년간의 코딩 경험을 돌아보면, 저는 방어적 프로그래밍 습관을 받아들이게 되었습니다. 이는 머피의 법칙에 기반합니다: "잘못될 수 있는 것은 반드시 잘못된다."

이 습관들은 언어 설계 결함과 API 불일치로 인해 발생하는 불필요한 버그를 방지합니다.

## 1. 문자열 리터럴을 먼저 작성하라

`.equals()` 비교에서 문자열 리터럴을 왼쪽에 배치하세요:

```java
// 나쁜 예
if (variable.equals("literal")) { ... }

// 좋은 예
if ("literal".equals(variable)) { ... }
```

이렇게 하면 변수가 null일 때 `NullPointerException`을 방지할 수 있습니다. 이것은 가장 널리 알려진 기법 중 하나입니다. 별것 아닌 것처럼 보이지만, null 체크를 잊어버리는 실수를 완전히 방지합니다.

## 2. 초기 JDK API를 신뢰하지 마라

일부 레거시 API는 예기치 않게 null을 반환합니다. `File.list()` 예시:

```java
String[] files = file.list();

// 항상 null 체크를 추가하세요
if (files != null) {
    for (int i = 0; i < files.length; i++) {
        // 파일 처리
    }
}
```

Javadoc에는 경로가 디렉토리를 나타내지 않으면 "null을 반환한다"고 명시되어 있습니다. 하지만 `file.isDirectory()`를 먼저 호출했더라도, 파일 시스템이 그 사이에 변경되었을 수 있습니다. 따라서 방어적 체크를 추가하세요.

## 3. "-1"을 신뢰하지 마라

`String.indexOf()`는 찾지 못하면 -1을 반환합니다. 긍정적인 체크를 사용하세요:

```java
// 나쁜 예
if (string.indexOf(character) != -1) { ... }

// 좋은 예
if (string.indexOf(character) >= 0) { ... }
```

이것은 다른 센티넬 값을 사용할 수 있는 미래의 API 변경에 대비합니다. 물론 `indexOf()`가 변경될 가능성은 낮지만, 누가 압니까? 또한 `>= 0`은 "찾았다"는 의미를 더 명확하게 표현합니다.

## 4. 실수로 인한 할당을 피하라

할당 오류를 방지하기 위해 리터럴을 먼저 배치하세요. Java의 컴파일러가 JavaScript보다 이 문제를 더 잘 방지하지만, 여전히 좋은 습관입니다:

```java
// 더 좋음 (오류 발생)
if (5 = variable) { ... }

// 의도한 것
if (5 == variable) { ... }
```

boolean 변수의 경우 특히 주의하세요:

```java
// 실수로 할당할 수 있음
if (active = true) { ... }

// 더 안전함
if (true == active) { ... }

// 가장 좋음 - 비교 자체를 제거
if (active) { ... }
```

## 5. Null과 길이를 모두 확인하라

존재 여부와 비어있지 않음을 모두 검증하세요:

```java
// 나쁜 예
if (array.length > 0) { ... }

// 좋은 예
if (array != null && array.length > 0) { ... }
```

배열이 어디서 왔는지 절대 확신할 수 없습니다. 리팩토링 후 갑자기 null이 될 수 있습니다. 같은 원칙이 컬렉션에도 적용됩니다:

```java
if (collection != null && !collection.isEmpty()) { ... }
```

## 6. 모든 메서드를 Final로 만들어라

서브타이핑을 위해 명시적으로 설계되지 않았다면 메서드를 `final`로 만드세요:

```java
// 나쁜 예
public void boom() { ... }

// 좋은 예
public final void dontTouch() { ... }
```

이것은 실수로 인한 또는 문제가 되는 오버라이드를 방지합니다. 물론 이것이 공개 API라면, 서브클래스에서 오버라이드할 수 있도록 메서드를 열어두고 싶을 수 있습니다. 하지만 대부분의 내부 코드에서는 `final`이 안전한 기본값입니다.

상속 대신 합성을 선호하라는 격언이 있습니다. `final` 메서드는 이 방향으로 강제합니다.

## 7. 모든 변수와 매개변수를 Final로 만들어라

실수로 인한 재할당을 방지하세요:

```java
// 나쁜 예
void input(String importantMessage) {
    String answer = "...";
    answer = importantMessage = "LOL 실수";
}

// 좋은 예
final void input(final String importantMessage) {
    final String answer = "...";
}
```

저는 이 관행을 자주 적용하지 않는다는 것을 인정합니다. 그래야 하지만, Java의 장황함이 이를 귀찮게 만듭니다. IDE가 이를 시각적으로 표시할 수 있다면(실제로 많은 IDE가 그렇게 합니다), 코드에 작성하지 않고도 경고를 받을 수 있습니다.

매개변수의 경우, 일부 코딩 표준에서는 매개변수 재할당이 혼란스러울 수 있으므로 항상 `final`을 요구합니다.

## 8. 오버로딩할 때 제네릭을 신뢰하지 마라

제네릭은 raw 캐스트를 통해 우회될 수 있습니다. 방어적 타입 체크를 추가하세요:

```java
// 나쁜 예
<T> void bad(T value) {
    bad(Collections.singletonList(value));
}

<T> void bad(List<T> values) {
    // ...
}

// 좋은 예
final <T> void good(final T value) {
    if (value instanceof List)
        good((List<?>) value);
    else
        good(Collections.singletonList(value));
}

final <T> void good(final List<T> values) {
    // ...
}
```

이것은 타입 소거로 인해 발생하는 미묘한 버그를 방지합니다. 누군가 raw 타입으로 `bad(someRawList)`를 호출하면, 첫 번째 오버로드는 `Collections.singletonList()`로 리스트를 다시 감쌀 것입니다 - 아마도 의도한 것이 아닐 겁니다.

## 9. Switch의 Default에서 항상 예외를 던져라

모든 switch에는 예외를 던지는 default 케이스가 필요합니다:

```java
switch (value) {
    case 1: foo(); break;
    case 2: bar(); break;
    default:
        throw new ThreadDeath("가르쳐주겠다");
}
```

미래의 코드 변경은 새로운 값을 도입할 것입니다. 처리되지 않은 케이스에 대해 즉시 실패하면 디버깅이 훨씬 쉬워집니다. `ThreadDeath`는 농담입니다 - 아마도 `IllegalStateException`이나 사용자 정의 예외를 사용해야 할 것입니다.

enum에서도 이것을 잊지 마세요:

```java
switch (myEnum) {
    case VALUE_A: ...; break;
    case VALUE_B: ...; break;
    default:
        throw new IllegalStateException("처리되지 않은 enum 값: " + myEnum);
}
```

누군가 나중에 `VALUE_C`를 추가하면, 컴파일러는 경고하지 않지만 런타임에 즉시 알게 될 것입니다.

## 10. Switch에서 중괄호를 사용하라

서로 다른 case 문에서 선언된 변수들은 스코프를 공유합니다. 블록을 사용하세요:

```java
// 나쁜 예, 컴파일되지 않음
switch (value) {
    case 1: int j = 1; break;
    case 2: int j = 2; break;
}

// 좋은 예
switch (value) {
    case 1: {
        final int j = 1;
        // j 처리
        break;
    }
    case 2: {
        final int j = 2;
        // j 처리
        break;
    }
    default:
        throw new ThreadDeath("가르쳐주겠다");
}
```

switch 문은 case 레이블이 단일 스코프를 공유하는 goto 호출처럼 작동합니다. 중괄호는 각 케이스에 대해 새로운 스코프를 생성하여 변수 섀도잉을 피하고 가독성을 향상시킵니다.

## 결론

편집증적 프로그래밍은 장황하지만, 언어 특이점과 API 불일치로 인해 발생하는 반복적이고 불필요한 버그를 방지합니다. 수십 년간의 개발 경험 후에, 이러한 예방 가능한 문제를 피하는 것은 추가된 장황함을 정당화합니다.

이러한 기법 중 일부는 논란의 여지가 있습니다. 예를 들어, "Yoda 조건"(`"literal".equals(variable)`)은 일부 개발자들에게 불필요하게 장황하거나 가독성이 떨어진다고 여겨집니다. 그러나 핵심은 이러한 규칙을 맹목적으로 따르는 것이 아니라 그 이유를 이해하는 것입니다.

결국, 좋은 코드는 방어적이면서도 읽기 쉬운 코드입니다. 팀의 코딩 표준과 상황에 맞게 이러한 기법들을 적절히 적용하세요.
