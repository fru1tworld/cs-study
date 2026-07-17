# 전략적 Scala 스타일: 데이터 타입 설계

> 원문: [Strategic Scala Style: Designing Datatypes](https://www.lihaoyi.com/post/StrategicScalaStyleDesigningDatatypes.html)
> 저자: Li Haoyi | 번역일: 2026-07-10

Scala로 프로그래밍할 때 반복을 피하는 주요 방법은 두 가지다. 자주 쓰는 절차나 계산을 나타내는 *함수*를 정의할 수 있고, 함께 전달하거나 함께 사용하곤 하는 데이터 묶음을 나타내는 *데이터 타입*을 — 예를 들어 `class`나 `case class`로 — 정의할 수 있다.

*함수*에 대해서는 많은 사람이 의견을 갖고 있다. "순수"해야 한다, 너무 길면 안 된다, 들여쓰기가 *이만큼* 이상 깊어지면 안 된다 등등. 반면 좋은 데이터 타입이 어떤 모습인지에 대해서는 훨씬 적게 쓰였다. 데이터 타입도 Scala 코드베이스에서 함수만큼 중요한 역할을 하는데도 말이다. 이 글은 내 Scala 프로그램을 구성하는 `class`와 `case class`를 설계할 때 내가 따르는 고려 사항과 가이드라인, 그리고 그것을 여러분의 Scala 코드에 적용하는 방법을 탐구한다.

Java에서처럼 Scala에서도 모든 것은 `class`나 `object` 안에 살아야 한다. 하지만 "코드를 담아 둘" 자리로만 존재하는 종류의 `class`와, 인스턴스화해서 프로그램 곳곳에 전달하는 값을 나타내는 종류의 클래스 사이에는 질적인 차이가 있다. 후자를 나는 *데이터 타입(Datatype)*이라 부르겠다.

데이터 타입의 예로는 이런 것들이 있다.

- `java.io.File`
- `scala.Tuple2`
- `scala.concurrent.Future`
- `java.awt.Point2D`

반면 데이터 타입이 아닌 클래스는 이런 것들이다.

- `java.lang.System`
- `scala.Predef`
- `java.lang.Math`

데이터 타입은 인스턴스화하고, 전달하고, 값이나 필드에 저장했다가 나중에 사용한다. 함수를 정의하면 같은 로직을 코드 곳곳에 복사·붙여넣기하는 것을 피할 수 있듯이, 데이터 타입을 정의하면 같은 파라미터 묶음이나 값 묶음을 코드 곳곳에 복사·붙여넣기하는 것을 피할 수 있다. `xPosition: Double`과 `yPosition: Double`을 사방에 돌리는 대신 `position: scala.Tuple2[Double, Double]`이나 `position: java.awt.Point2D`를 돌릴 수 있다.

내장 데이터 타입 외에도 라이브러리에 정의된 데이터 타입을 쓰게 되고, 프로그램이 커지면서 직접 정의하게 된다. 이 글은 자신만의 데이터 타입을 설계할 때 기억해야 할 고려 사항과 가이드라인을 탐구하고, Scala가 데이터를 모델링하도록 제공하는 넘쳐나는 방법들을 정리하는 데 도움을 줄 것이다.

- 불투명하게 또는 투명하게?
  - 불투명성은 불변 조건을 강제한다
  - 불투명성은 방어 코드를 아껴 준다
  - 투명성은 복잡성을 줄인다
  - 투명성과 불투명성은 스펙트럼이다
- 데이터 타입은 전역적(Total)이어야 한다
  - 자체 검사
  - 구조적 강제
- 데이터 타입은 정규화되어야 한다
  - 수동 정규화
  - 자동 정규화
- 데이터 타입은 즉시 초기화되어야 한다
  - 클래스 초기화
  - 컬렉션 초기화
- 결론

이 글은 *전략적 Scala 스타일* 시리즈의 네 번째 글로, [Principle of Least Power](https://www.lihaoyi.com/post/StrategicScalaStylePrincipleofLeastPower.html), [Conciseness & Names](https://www.lihaoyi.com/post/StrategicScalaStyleConcisenessNames.html), [Practical Type Safety](https://www.lihaoyi.com/post/StrategicScalaStylePracticalTypeSafety.html)에 이어진다. 다른 글들처럼 고유한 관례가 있을 법한 라이브러리나 프레임워크 없이 바닐라 Scala에 집중한다. 중급 수준의 글이라 독자가 Scala의 언어 기능에 익숙하다고 기대하지만, Scala의 화려한 디자인 패턴이나 화려한 프레임워크에 익숙할 필요는 없다.

Scala를 한동안 써 온 많은 사람에게는 이 내용 대부분이 "당연"하게 느껴질 수 있다. 그래도 이 글이 그것을 당연하게 여기지 않는 사람들을 위해 이 "당연한" 지식을 성문화하고, 앞으로의 논의를 위한 토대가 되기를 바란다.

그럼 첫 번째 고려 사항으로 넘어가자. 데이터 타입을 불투명하게 만들어야 할까, 투명하게 만들어야 할까?

## 불투명하게 또는 투명하게?

데이터 타입을 설계할 때 내려야 하는 첫 번째 결정은 이것이다. 데이터 타입이 얼마나 불투명해야 하는가?

- 내부를 숨기고 캡슐화해야 하는가?

- 외부 코드가 보고 쓸 수 있게 내부를 전부 노출해야 하는가?

첫 번째 경우는 "전통적인" Java 스타일 객체지향 프로그래밍에 대응하고, 두 번째 경우는 더 "함수형"인 Scala 스타일에 대응한다. 둘 다 Scala 프로그램에서 쓰임새가 있다.

예를 들어, 설정 파일을 파싱하다 생기는 문법 오류를 나타내는 `ParseError` 데이터 타입을 설계한다고 하자. 다음 항목들을 사용할 수 있게 만들고 싶다고 상상해 보자.

- 오류가 발생한 `line`과 `col`
- 무엇이 잘못됐는지 설명하는 사람이 읽을 수 있는 `message`

생성자, 초기화 로직, 필요한 메서드를 노출하는 불투명한 데이터 타입으로 정의하는 것을 상상할 수 있다.

```scala
class ParseError(index: Int, input: String){
  ... 어떤 계산 ...

  def line: Int = ...
  def col: Int = ...
  def message: String = ...
}
```

여기서는 `ParseError`를 생성할 때 `index`와 `input`을 받지만 둘 다 public이 아니다. 대신 사람들이 사용할 `line`, `col`, `message`만 `def`로 노출한다. 나는 이것을 *불투명(Opaque)*하다고 부른다. `ParseError`를 쓰는 사람은 그것이 실제로 어떻게 동작하는지 들여다볼 수 없고, 노출된 몇 개의 `def`만 볼 수 있기 때문이다. 어떤 계산이 일어나든 `ParseError`의 본문 안에 완전히 숨겨져 있다.

또 다른 방법은 `case class`로 만드는 것이다.

```scala
case class ParseError(line: Int, col: Int, message: String)
```

`case class`이므로 생성자 파라미터 `line`, `col`, `message`가 전부 자동으로 public이다. `ParseError` 본문에서는 어떤 계산도 하지 않는다. `index`와 `input`으로부터 `line`, `col`, `message`를 계산하는 일은 `ParseError`가 만들어지기 *전에*, 그 *바깥에서* 일어나야 한다. 나는 이것을 *투명(Transparent)*하다고 부른다. `ParseError`를 쓰는 사람이 바깥에서 그것에 대해 알아야 할 전부를 볼 수 있기 때문이다. 숨겨진 데이터가 저장되어 있지도, 숨겨진 계산이 수행되지도 않는다. 그저 정수 두 개(`line`과 `col`)와 문자열 하나(`msg`)의 묶음일 뿐이다.

일반적으로 내부가 더 복잡하고 내부 불변 조건이 더 많은 데이터 타입은 불투명한 `class`로 다루는 것이 낫고, 더 단순한 데이터 타입은 투명한 `case class`로 다루는 것이 낫다. 다음 이유들이 그 까닭을 보여 준다.

- 불투명성은 불변 조건을 강제한다
- 불투명성은 방어 코드를 아껴 준다
- 투명성은 복잡성을 줄인다

투명성과 불투명성은 스펙트럼이라는 점도 알아 둘 만하다. 어느 한쪽을 고르는 완전한 이분법적 결정이 아니며, 원하는 트레이드오프 조합에 맞춰 투명성-불투명성 스펙트럼 위의 한 지점을 고를 수 있다.

### 불투명성은 불변 조건을 강제한다

불투명한 데이터 타입에서는 불변 조건을 강제하기가 더 쉽다. "원본" `index`/`input` 데이터로부터 계산된 `def`들을 만드는 일이 생성자 안에서 이루어지므로, 누군가 `line`, `col`, `message`에 말이 안 되는 값이 담긴 "나쁜" `ParseError`를 만들 방법이 없다. `message`가 항상 특정 스타일이라면, 실수로 그것이 `"I am Cow"`를 반환하게 만들 수 없다. `line`이나 `col`이 항상 0보다 크다면, (`ParseError` 자체의 내부를 신뢰하는 한) 갑자기 음수를 반환하는 일이 절대 없다고 확신할 수 있다.

반면 투명한 데이터 타입에서는 `line`이나 `col`에 음의 정수를 쉽게 넘길 수 있다. `message`에 이상한 문자열을 넘길 수 있고, 투명한 데이터 타입은 그것을 기꺼이 노출할 것이다.

투명한 데이터 타입에서도 일부 불변 조건은 데이터 타입의 구조로 강제할 수 있다. 다른 불변 조건은 자체 검사로 검증할 수 있다. 그래도 불변 조건을 구조적으로 강제하기 어렵고 자체 검사를 잔뜩 추가하는 것도 지겨운 경우라면, 데이터 타입을 그냥 불투명하게 만드는 것이 정답일 수 있다.

### 불투명성은 방어 코드를 아껴 준다

위의 case class 같은 투명한 데이터 구조에서도 불변 조건을 강제할 수는 있지만, 추가 단계가 필요하다. 예를 들면

- `message`가 `null`이 아님을 보장하는 `assert`

- `line`과 `col`이 음수가 아님을 보장하는 `assert`

- (경우에 따라) `message`가 어떤 형식을 따름을 보장하는 `assert`

투명한 데이터 타입에서 이런 것들을 강제하려면 assert가 필수다. 하류의 어떤 코드가 우리의 `ParseError(line: Int, col: Int, message: String)` 생성자에 무엇을 욱여넣으려 할지 누가 알겠는가! 반면 불투명한 데이터 타입에서는 assert가 그만큼 필요하지 않다. 이 값들을 계산할 수 있는 것은 데이터 타입 자신뿐이다. 그래서 assert 없이도 그것이 옳은 일을 하고 외부 코드가 간섭할 수 없다고 믿을 수 있다.

### 투명성은 복잡성을 줄인다

로직을 불투명한 데이터 타입 안에 캡슐화하는 것은 매력적으로 보이지만, 저주가 될 수도 있다. 불투명한 데이터 타입은 복잡할 수 있는데 그 복잡성이 숨겨져 있어서, 미묘한 문제를 일으키기 전까지는 눈치채지 못하는 경우가 많다.

예를 들어 바깥에서는 불투명한 `ParseError`가 생성된 뒤에도 `input: String`에 대한 참조를 붙들고 있는지 알 수 없다. 파싱의 입력은 대개 에러 메시지에 담기는 어떤 데이터보다도 훨씬 크므로, 무심코 `ParseError` 여러 개를 붙들고 있으면 상당한 메모리 누수가 생길 수 있다!

또 다른 불확실성은 `line`, `col`, `message`가 어떻게 채워지느냐다.

- 생성 시에 한 번 미리 계산되는가?

- 접근할 때마다 다시 계산되는가?

- lazy라서, 처음 접근할 때는 계산이 느릴 수 있지만 이후에는 빨라지는가?

- 메모리를 얼마나 차지하고 계산에 얼마나 걸리는가?

이것들은 중요한 질문일 수 있다! 물론 코드를 파헤치면 답을 알아낼 수 있지만, 그러기가 만만치 않을 수 있다.

`case class`로 모델링한 투명한 `ParseError`에서는 시그니처를 한 번 훑는 것으로 이 질문들에 전부 답이 나온다. `line`, `col`, `message`가 전부 처음부터 즉시(eager) 계산되어 있다는 것이 한눈에 보인다. 어마어마하게 클 수도 있는 `input: String`을 붙들고 메모리를 쓰는 일이 없다는 것도 한눈에 보인다. 불투명한 데이터 타입이 복잡성을 숨기고 불변 조건을 유지하는 데 도움을 준다면, 투명한 데이터 타입은 복잡성을 아예 제거하는 데 도움을 준다. `case class`에는 필드에 데이터가 담겨 있고, 그것이 그것에 대해 알아야 할 전부다.

### 투명성과 불투명성은 스펙트럼이다

많은 것이 그렇듯 *투명성*과 *불투명성*은 이분법적 선택이 아니다. 중간까지만 갈 수도 있다. 데이터 타입 필드의 일부는 "멍청한" 생성자 인자로 두고, 다른 "똑똑한" 필드는 클래스 본문에서 계산하는 식이다. 위의 "불투명한" 예제보다 더 불투명하게 갈 수도 있다. 예를 들어 생성자를 `private`으로 숨기고 팩토리 메서드(예: 동반 객체의 `apply`)를 통해서만 인스턴스를 만들게 하는 것이다.

```scala
object ParseError{
  def apply(...): ParseError = {
    ... 어떤 계산 ...
    new ParseError(...)
  }
}
class ParseError private(index: Int, input: String){
  ... 어떤 계산 ...

  def line: Int = ...
  def col: Int = ...
  def message: String = ...
}
```

일반적으로 내부가 더 복잡하고 내부 불변 조건이 더 많은 데이터 타입은 불투명한 `class`로, 더 단순한 데이터 타입은 투명한 `case class`로 다루는 것이 낫지만, 절대적인 규칙은 없다. 다음에 데이터 타입을 고를 때는 투명성-불투명성 스펙트럼의 어디에 놓을지 고민해 볼 가치가 있다!

## 데이터 타입은 전역적(Total)이어야 한다

사람들이 *함수*가 Total하다고 말할 때는 보통, 컴파일러를 만족시키는 어떤 인자를 넘기든 런타임에 "무효한" 결과를 내지 않는다는 — 예를 들어 예외로 터지지 않는다는 — 뜻이다. 코드를 추론할 때 갖고 있으면 편리한 속성이고(예: "이 코드는 절대 던지지 않는다"), 현실에서 도달하기는 어렵지만 그래도 지향할 가치가 있다.

내가 *데이터 타입*이 Total하다고 말할 때는, 어떻게 생성하든 "무효"할 수 없다는 뜻이다. 다시 말해 여러분의 `class`나 `case class`를, 그 용도에 비추어 말이 안 되는 인스턴스로 생성할 수 없어야 한다. 예를 들어 무효할 *수 있는* 데이터 타입의 예를 보자.

```scala
case class URL(value: String)
case class EmailAddress(value: String)
/**
 * 하위 폴더 없이 텍스트 파일로 가득한 폴더를 나타낸다.
 * 각 파일의 이름과 텍스트 내용을 튜플로 함께 저장한다
 */
case class FolderContents(value: Seq[(String, String))
```

`URL`, `EmailAddress`, `FolderContents`를 모델링하는 그럴듯한 방법처럼 보이지만, 무효한 인스턴스를 만들 수 있다.

- `new URL("http:/www.google.com")`은 무효하다. `http` 뒤에 이중 `//`가 필요하다

- `new EmailAddress("haoyi.com")`은 이메일 주소로서 무효하다. 이메일 주소에는 어딘가에 `@`가 있어야 한다

- `new FolderContents(Seq("file.txt" -> "Hello", "file.txt" -> "World"))`는 무효하다. 같은 이름의 파일이 두 개 있을 수 없다

예를 들어 누군가 `http:/www.google.com`으로 HTTP 요청을 보내려 하면 형식이 잘못된 URL이라 실패할 것이다. 그리고 URL이 사용되기 전까지는 이를 알 수 없으므로, 우리가 주의를 기울이지 않는 사이 — 어쩌면 토요일 자정 넘긴 새벽 1시에 — 나중에 터지기 전까지 몇 분이고 몇 시간이고 프로그램에 저장되어 있을 수 있다. 좋지 않다!

이들이 Total하다면, `URL` 객체를 갖고 있는 한 그것이 "올바른 형식"이고 사용하려 할 때 자신의 잘못된 형식 때문에 터지지 않는다고 확신할 수 있을 것이다. 물론 런타임 문제(와이파이가 끊겼나?)로 여전히 실패할 수 있지만, 적어도 "내부" 문제로는 실패하지 않는다. 실패하더라도 생성하려는 시점에 일찍 실패하므로, 버그를 일찍 고치고 걱정 없이 퇴근할 수 있다.

확실히 갖고 싶은 좋은 속성인데, 어떻게 달성할까? 데이터 타입을 *Total*하게 만드는 주요 기법은 두 가지다.

- 자체 검사
- 구조적 강제

### 자체 검사

데이터 타입이 절대 *무효*하지 않도록 강제하는 간단한 방법은 생성자에 단언(assertion)을 추가해서, 누군가 우리가 염두에 둔 규칙을 어기는 무효한 인스턴스를 만들려고 하면 예외를 던지는 것이다.

```scala
case class URL(value: String){
  assert(value.contains("//"))
}
case class EmailAddress(value: String){
  assert(value.contains("@"))
}
/**
 * 하위 폴더 없이 텍스트 파일로 가득한 폴더를 나타낸다.
 * 각 파일의 이름과 텍스트 내용을 튜플로 함께 저장한다
 */
class FolderContents(value: Vector[(String, String)){
  assert(value.map(_._1).distinct == value.map(_._1))
}
```

이제 무효한 인스턴스를 만들려고 하면 우리 손에 들어오기 전에 실패한다.

```scala
@ new EmailAddress("haoyi.com")
java.lang.AssertionError: assertion failed
  scala.Predef$.assert(Predef.scala:156)
```

그리고 누군가 `EmailAddress`를 함수 인자로 넘기든, 우리가 어떤 객체의 필드에서 그것을 읽든, 그 `EmailAddress`가 모든 이메일 주소에 기대하는 최소한의 기본 속성을 만족한다고 확신할 수 있다. 이 경우 반드시 `@`를 포함한다는 것이다. `@`를 포함하지 않았다면 생성 중에 예외를 던졌을 테고, 우리 손에 들어올 수 없었을 것이다. 갖고 있으면 좋은 속성이다!

이 방식으로 전역성을 구현하면 몇 가지 이점이 있다.

- *정말 쉽다*. 클래스 본문에서 반드시 참*이어야 하는* 것들을 assert하면 끝이다

- 기존 코드를 거의 바꿀 필요가 없다. 모두가 늘 하던 대로 `EmailAddress`를 만들고 `.value`를 쓸 수 있다. 다만 이제 나쁜 것을 만들려고 하면 터진다.

문제도 있다.

- 이 자체 검사는 매번 수행하기에 비쌀 수 있다! 예를 들어 `FolderContents` 클래스의 자체 검사는 완전히 새로운 `Vector`를 *세 개*나 만들어 비교한 뒤 버린다.

- 불완전할 수 있다. `URL`에는 `//`를 포함한다는 것 외에도 다른 제약이 있을 텐데, 이 검사는 그것들을 잡지 못한다.

- 검사가 복잡해질수록 더 느려지고 틀리기도 쉬워진다.

따라서 전역성을 보장하는 또 다른 방법인 구조적 강제를 고려해 볼 가치가 있는 경우가 많다.

### 구조적 강제

전역성의 구조적 강제란 데이터 타입이 정의된 방식 자체만으로 무효한 데이터를 담을 수 없게 만드는 것이다. 예를 들어 위의 경우들을 이렇게 정의할 수 있다.

```scala
case class URL(protocol: String, host: String, path: String){
  def value = protocol + "://" + host + "/" + path
}

case class EmailAddress(prefix: String, suffix: String)
  def value = prefix + "@" + suffix
}
/**
 * 하위 폴더 없이 텍스트 파일로 가득한 폴더를 나타낸다
 */
case class FolderContents(value: Map[String, String])
```

`URL`과 `EmailAddress`의 경우, "날것의" `String`에서 출발해 그 구성 요소의 존재를 assert하려 하는 대신, 구성 요소에서 출발해 필요할 때만 `String`으로 변환한다.

`FolderContents`의 경우, 데이터에 대해 아는 것에 맞는 더 나은 데이터 구조를 고른다. `Map`은 무슨 짓을 해도 키 하나에 값 하나만 가질 수 있다. 여기서는 별도의 `def value`를 제공할 필요조차 없을 수 있다. `Map[String, String]`은 `Seq[(String, String)]`만큼이나 다루기 쉽기 때문이다.

여기에도 여전히 assert를 넣고 싶을 수 있다. 예를 들어 `EmailAddress`의 `prefix`와 `suffix`가 `@`를 포함하지 않는다고 assert할 수 있다.

```scala
case class EmailAddress(prefix: String, suffix: String)
  assert(!prefix.contains('@'))
  assert(!suffix.contains('@'))
  def value = prefix + "@" + suffix
}
```

이렇게 하면 위에서는 없던 추가 검사를 얻는다. 이메일 주소에 `@`가 정확히 하나 — 더도 덜도 아니고 — 있다는 것이다. 같은 검사를 자체 검사로 추가할 수도 있었지만, 그러면 assert가 더 복잡해지고, 따라서 더 느려지고 더 틀리기 쉬워진다.

*구조적 강제*는 자체 검사에 비해 많은 장점이 있다.

- 누군가 기본 구성 요소로부터 데이터 타입의 인스턴스를 만들고 싶다면 assert를 건너뛰고 직접 만들 수 있어서, 불필요한 계산을 크게 아낄 수 있다.

- "무엇을 담는지"에 대한 제약은 클래스 본문의 임의의 `assert`들보다 클래스의 필드로 표현될 때 미래의 유지보수자에게 대개 훨씬 분명하다

구조적 강제를 쓰더라도, 이런 데이터 타입을 만드는 "안전하지 않은 입력에서 파싱하기" 메서드를 두는 것은 여전히 가치 있을 수 있다. 예를 들어 입력이 무효하면 예외를 던지거나

```scala
object EmailAddress{
  def parse(input: String): EmailAddress = {
    input.split('@') match{
      case Array(prefix, suffix) => new EmailAddress(prefix, suffix)
      case _ => throw new IllegalArgumentException("Invalid email address: " + input)
  }
}
case class EmailAddress(prefix: String, suffix: String)
  assert(!prefix.contains('@'))
  assert(!suffix.contains('@'))
  def value = prefix + "@" + suffix
}
```

`Option`을 반환하는 식이다.

```scala
object EmailAddress{
  def parse(input: String): Option[EmailAddress] = {
    input.split('@') match{
      case Array(prefix, suffix) => Some(new EmailAddress(prefix, suffix))
      case _ => None
  }
}
case class EmailAddress(prefix: String, suffix: String)
  assert(!prefix.contains('@'))
  assert(!suffix.contains('@'))
  def value = prefix + "@" + suffix
}
```

결국 프로그램의 가장자리에서는 여러분의 특별한 `EmailAddress` 클래스가 아니라 `String`을 쓰곤 하는 외부 API와 반드시 접점을 가져야 할 테니까.

탈출구가 있더라도 전역성의 구조적 강제는 가치 있다. 대개 `EmailAddress.parse` 같은 탈출구의 사용은 프로그램의 가장자리로 밀려난다. 프로그램의 아무 데서나 무효한 문자열로부터 이메일 주소를 "실수로" 파싱하게 되는 일은 없을 것이다! 하지만 평범한 `String`을, 심지어 위에서 본 것처럼 감싼 `class EmailAddress(value: String)`을 돌린다면, 이메일 주소 `String`을 다른 아무 `String`과 혼동하기 아주 쉽고, `.substring` 같은 문자열 메서드를 실수로 써서 무효한 `EmailAddress`가 남을 수 있다는 것을 눈치채지 못하기도 쉽다.

## 데이터 타입은 정규화되어야 한다

이상적으로 데이터 타입은 *정규화(Normalized)*되어야 한다. 내용이 다른데 "같은" 인스턴스가 여러 개 존재하는 것을 허용하지 말아야 한다는 뜻이다. 위에서 "무효"를 정의했을 때처럼 "같음"도 주관적이고 무엇을 하려는지에 달렸다. 예를 들면

- 절대 파일시스템 경로 `/home/haoyi/./.ssh`는 경로 `/home/haoyi/.ssh`와 같고, 이는 경로 `/home/haoyi/.ssh/config/..`와 같다

- 상대 파일시스템 경로 `./.ssh/../..`는 상대 경로 `..`와 같다

- ANSI 색이 입혀진 문자열 `"\u001b[31mHello\u001b[0mWorld"`는 터미널에 표시될 때 ANSI 색 문자열 `"\u001b[31mHello\u001b[0m\u001b[0mWorld"`와 같다. 앞의 "빨강"(`\u001b[31m`)이 중간의 "리셋"(`\u001b[0m`)으로 되돌려지는데, 리셋이 연달아 둘이든 하나든 상관없다. 어차피 색을 똑같이 리셋하고 똑같이 렌더링된다.

정규성(Canonicity)은 신경 쓰지 않고 전역성만 신경 쓴다면, 두 파일시스템 경로를 이렇게 정의할 수 있다.

```scala
case class AbsPath(value: String){
  assert(value(0) == '/')
}
case class RelPath(value: String){
  assert(value(0) != '/')
}
```

`assert`는 두 타입에 대해 우리가 아는 것(절대 경로는 `/`로 시작하고 상대 경로는 아니다)을 강제한다. 무효한 상대·절대 경로를 만들려고 하면 그 효과를 볼 수 있다.

```scala
@ AbsPath("foo/bar/..")
java.lang.AssertionError: assertion failed

@ RelPath("/foo/bar/..")
java.lang.AssertionError: assertion failed
```

데이터가 *유효*함을 보장하는 데는 훌륭하지만, 데이터가 정규화됨을 보장하는 데는 도움이 안 된다. 예를 들어 두 경로를 비교하려 하면 기본 비교는 통하지 않는다.

```scala
@ AbsPath("/foo/bar/..") == AbsPath("/foo")
res3: Boolean = false
```

어떻게 해야 할까?

### 수동 정규화

한 가지 선택지는 비교 등을 하기 전에 데이터 타입을 "정규 형태"로 만들기 위해 호출해야 하는 `.normalize()`나 `.canonicalize()` 메서드를 제공하는 것이다. 예를 들어 `java.io.File`은 [getCanonicalFile](https://docs.oracle.com/javase/7/docs/api/java/io/File.html#getCanonicalFile\(\)) 메서드로 이를 제공하며, `AbsPath`용으로 우리만의 버전을 작성할 수도 있다.

```scala
case class AbsPath(value: String){
  assert(value(0) == '/')
  def normalized() = {
    val parts = value.split('/')
    val output = collection.mutable.Buffer.empty[String]
    parts.foreach{
      case "." => // 아무것도 하지 않음
      case ".." => output.remove(output.length - 1)
      case segment => output.append(segment)
    }
    AbsPath(output.mkString("/"))
  }
}
```

`RelPath`에도 비슷한 것을 작성할 수 있지만 아이디어는 같다. 필요할 때 정규화할 수 있는 선택지를 사용자에게 주는 것이다.

이건 동작하고, `AbsPath`끼리 동등성을 비교하기 전에 `.normalized()`를 호출하는 것만 기억하면 올바른 답을 얻는다.

```scala
@ AbsPath("/foo/bar/..") == AbsPath("/foo")
res11: Boolean = false

@ AbsPath("/foo/bar/..").normalized()
res12: AbsPath = AbsPath("/foo")

@ AbsPath("/foo").normalized()
res13: AbsPath = AbsPath("/foo")

@ AbsPath("/foo/bar/..").normalized() == AbsPath("/foo").normalized()
res14: Boolean = true
```

프로그래밍 세계는 수십 년 동안 이렇게 해 왔다. Java, Python을 비롯한 수많은 언어가 파일시스템 경로를 사실상 문자열에 정규화 메서드를 얹은 형태로 제공한다. 하지만 이상적이지는 않다. 잊어버리기 쉽고, 정규화는 비싸서 *불필요하게* 정규화하고 싶지도 않다! 결국 필요한 딱 그만큼만 *정확히* 정규화해야 하는 곤란한 처지에 놓이고, 그러지 못하면 버그나 성능 낭비를 겪는다.

더 잘할 수 있고, 그것이 자동 정규화다.

### 자동 정규화

*자동 정규화*는 구조적 강제와 비슷한 원리이고 자주 함께 쓰인다. 올바른 메서드를 언제 호출할지 기억하려 애쓰는 대신, *데이터 타입이 생성될 때마다* 정규화된 상태이도록 보장함으로써 데이터 타입의 정규화를 강제하는 것이다. 수동 정규화에 비해 여러 장점이 있다.

- 찾기가 더 쉽다! 예를 들어 로그에 정규화된 경로가 찍혀 있다면, 같은 경로의 열댓 가지 변형을 grep하는 대신 그것 하나만 grep해서 그것이 찍힌 다른 모든 곳을 찾을 수 있다고 확신할 수 있다. 데이터베이스, 캐시, 콘솔 출력에 저장된 데이터에도 똑같이 적용된다.

- `==`로 비교하거나 정규화가 필요한 다른 일(`Map`, `Set`에 넣기 등)을 하기 전에 정규화하는 것을 *잊을* 수 없다. 잊으면 경로가 *어떻게* 생성됐는지가 — "같은" 경로인데도 — 프로그램의 동작에 영향을 주는 미묘한 버그가 생긴다.

- 실수로 *이중 정규화*할 수 없다. 어디서 왔는지 모르는 `AbsPath`나 `java.io.File`을 다른 곳에서 받으면(코드베이스는 넓은 곳이니...) "혹시 몰라서" 한 번 더 정규화하고 싶은 유혹이 자주 든다. 불필요하게 반복하면 낭비가 되는데, *자동 정규화*에서는 그런 일이 일어나지 않는다.

- 더 효율적이다! 데이터 타입을 정규 형태로 다루는 것은 "보통" 형태로 다루는 것보다 거의 항상 쉽다. 예를 들어 한 경로가 다른 경로의 하위 경로인지 비교하는 일은 두 경로가 이미 정규화되어 있으면 훨씬 쉽다. 이미 정규화된 데이터를 정규화된 채로 유지하는 비용은 대개 필요할 때 수동으로 정규화하는 것보다 비싸지 않고, 실제로는 더 싼 경우가 많다.

예를 들어 파일시스템 경로를 정규화 메서드가 딸린 case class로 정의하는 대신

```scala
case class AbsPath(value: String){
  assert(value(0) == '/')
  def normalized() = {
    val parts = value.split('/')
    val output = collection.mutable.Buffer.empty[String]
    parts.foreach{
      case "." => // 아무것도 하지 않음
      case ".." => output.remove(output.length - 1)
      case segment => output.append(segment)
    }
    AbsPath(output.mkString("/"))
  }
}
case class RelPath(value: String){
  assert(value(0) != '/')
  def normalized() = { ??? }
}
```

`parse` 메서드가 `case class`를 만들기 전에 정규화를 수행하는 case class로 정의할 수 있다.

```scala
object AbsPath{
  def parse(input: String) = {
    val output = collection.mutable.Buffer.empty[String]
    input.drop(1).split('/').foreach{
      case "." => // 아무것도 하지 않음
      case ".." => output.remove(output.length - 1)
      case segment => output.append(segment)
    }
    AbsPath(output)
  }
}
case class AbsPath(segments: Seq[String]){
  assert(!segments.contains("..") && !segments.contains("."))
  def value = "/" + segments.mkString("/")
}
object RelPath{
  def parse(input: String) = {
    var ups = 0
    val output = collection.mutable.Buffer.empty[String]
    input.split('/').foreach{
      case "." => // 아무것도 하지 않음
      case ".." =>
        if (output.nonEmpty) output.remove(output.length - 1)
        else ups += 1
      case segment => output.append(segment)
    }
    RelPath(ups, output)
  }
}
case class RelPath(ups: Int, segments: Seq[String]){
  assert(!segments.contains("..") && !segments.contains("."))
  def value = (Array.fill(ups)("..") ++ segments).mkString("/")
}
```

무엇을 한 걸까?

- 각 클래스의 `normalize` 메서드 본문을 각자의 `parse` 함수로 옮겼다

- 두 경로의 표현을 멍청한 `String`에서 개별 경로 세그먼트를 나타내는 `Seq[String]`로 바꿨다

- 세그먼트가 `.`이나 `..`일 수 없음을 assert로 강제했다!

- `RelPath`가 `ups: Int` 개수를 유지하게 했다

앞의 세 가지는 비교적 알기 쉽다. 결국 경로란 경로 세그먼트의 나열일 뿐이고, 절대 경로에서 파일의 정규화된 버전에는 `.`이나 `..` 세그먼트가 절대 없다.

```scala
@ new java.io.File("/foo/../bar/././baz/..").getCanonicalPath
res24: String = "/bar"
```

마지막 항목은 조금 미묘하다. 상대 경로의 정규화된 버전에는 `..`가 있을 *수 있지만*, *경로의 시작 부분에만* 있을 수 있다! 예를 들어 상대 경로

- `foo/../../bar/../../././baz`

는 `.`들을 제거하며 단계별로 줄일 수 있고

- `foo/../../bar/../../baz`

`..`들을 왼쪽으로 접어 나가면

- `../bar/../../baz`
- `../../baz`

모든 `..`가 가장 왼쪽 세그먼트에, `..`가 아닌 것들이 오른쪽 세그먼트에 오게 된다. 따라서 이 정규 형태에서 상대 경로는 그저 왼쪽의 `..` 개수와 오른쪽의 `..` 아닌 세그먼트들의 `Seq`일 뿐이다! 어쨌든 위 정의에서 `AbsPath.parse`와 `RelPath.parse` 메서드가 이 정규화를 수행하도록 만들었고, 전부 정의하고 나면 동작한다. `AbsPath`도

```scala
@ AbsPath.parse("/foo/bar/..")
res19: AbsPath = AbsPath(ArrayBuffer("foo"))

@ AbsPath.parse("/foo")
res20: AbsPath = AbsPath(ArrayBuffer("foo"))

@ AbsPath.parse("/foo/bar/..") == AbsPath.parse("/foo")
res21: Boolean = true
```

`RelPath`도

```scala
@ RelPath.parse("../../baz")
res38: RelPath = RelPath(2, ArrayBuffer("baz"))

@ RelPath.parse("foo/../../bar/../../././baz")
res39: RelPath = RelPath(2, ArrayBuffer("baz"))

@ RelPath.parse("foo/../../bar/../../././baz") == RelPath.parse("../../baz")
res40: Boolean = true
```

항상 정규화된 상태에 있다.

실제 데이터 타입에는 `.parse`와 `==` 말고도 더 많은 연산이 있을 테고, 그 모든 연산이 데이터 타입을 정규 형태로 유지하도록 보장해야 한다. 그래도 그렇게 하는 것은 대개 (런타임 비용도) 비교적 싸고 (구현하기도) 쉬워서 끔찍한 비용은 아니다.

자동 정규화 버전에서는 `==` 동등성이 통하는 것을 볼 수 있다. `Set`이나 `Map`에 마음껏 넣을 수 있다. 이 섹션 첫머리에서 언급한 좋은 점들을 전부, 앞으로 영영 걱정할 필요 없이 얻는다!

## 데이터 타입은 즉시 초기화되어야 한다

데이터 타입은 이상적으로 생성자나 팩토리 함수가 반환하는 즉시 완전히 초기화되어 사용할 준비가 되어 있어야 한다. "반쯤 구워진" 인스턴스를 만들어 놓고 사람들이 나중에 채워 주기를 바라서는 안 된다. 생성자를 호출하면

```scala
val myFoo = new Foo(...)
```

`myFoo`는 완전해야 하고 즉시 사용할 수 있어야 한다. 이것이 중요한 흔한 경우가 둘 있다.

- 클래스 초기화
- 컬렉션 초기화

### 클래스 초기화

Scala에서는 생성자 파라미터로 클래스를 초기화해야 한다. 기본값 등을 줘야 한다면 그렇게 하라.

```scala
class MyAction(text: String, image: String, tooltip: String = "")

val action = new MyAction("My Action Text", "Some Image")
```

클래스를 반쯤 구워진, 사용할 수 없는 상태로 인스턴스화해 놓고 "나중에" 와서 빠진 필드를 전부 채우도록 데이터 타입을 설계하지 마라. Scala에서는 덜 흔하지만 Java 라이브러리에서는 아주 흔하다. 예를 들어 다음 Java의 `class MyAction` 정의는

```java
public class MyAction {
    private String _text     = "";
    private String _tooltip  = "";
    private String _imageUrl = "";

    public MyAction()
    {
       // 여기서 할 일 없음.
    }

    public MyAction text(string value)
    {
       this._text = value;
       return this;
    }

    public MyAction tooltip(string value)
    {
       this._tooltip = value;
       return this;
    }

    public MyAction image(string value)
    {
       this._imageUrl = value;
       return this;
    }
}
```

`MyAction`을 이렇게 생성하게 한다.

```scala
val action = new MyAction()
action.text("My Action Text")
action.tooltip("My Action Tool tip")
action.image("Some Image");
```

여기에는 문제가 있다. `new MyAction()`과 `action.image` 사이 어딘가에서, 아래 `준비 안 됨` 구역 아무 데서나 실수로 `action`을 사용하면

```scala
// 사용 불가...
val action = new MyAction()
// 준비 안 됨
action.text("My Action Text")
// 준비 안 됨
action.tooltip("My Action Tool tip")
// 준비 안 됨
action.image("Some Image");
// ...준비 완료
```

일부 필드가 알 수 없게 `""`로 설정된 반쯤 만들어진 `MyAction`을 손에 쥐게 된다. 좋지 않다!

더 나쁜 것은, 누군가 여러분의 `MyAction` 클래스를 쓰다가 초기화 단계 중 하나를 아예 잊어버릴 수 있다는 사실이다.

```scala
val action = new MyAction()
action.text("My Action Text")
action.tooltip("My Action Tool tip")
// 이런
```

그리고 한참 뒤, 프로그램의 멀리 떨어진 어딘가에서, 왜 `image`가 `""`가 되는지 의아해하게 된다. 십중팔구 "한참 뒤"란 토요일 자정 넘긴 새벽 1시이고, "멀리 떨어진 어딘가"란 최대한 빨리 고쳐야 하는 고용주의 데이터센터... 그것도 여러분 손으로 말이다.

이런 API는 "[플루언트하게](https://en.wikipedia.org/wiki/Fluent_interface)" 호출되기도 좋다. 예를 들어

```scala
val action = new MyAction()
    .text("My Action Text")
    .tooltip("My Action Tool tip")
    .image("Some Image");
```

초기화 호출을 줄줄이 연결할 수 있다. 이는 `준비 안 됨` 문제를 고치는 데 도움이 된다.

```scala
// 사용 불가...
val action = new MyAction()
    .text("My Action Text")
    .tooltip("My Action Tool tip")
    .image("Some Image");
// ...준비 완료
```

컴파일러가 아직 `val action`을 사용하지 못하게 하는 `사용 불가`에서 완전히 초기화된 `준비 완료`로 곧바로 건너뛰는 것을 볼 수 있다. 컴파일러는 쓰게 해 주지만 실제로는 준비되지 않아 런타임에 오동작하거나 예외를 던지는 `준비 안 됨` 단계를 전혀 거치지 않는다.

훌륭하지만, 누군가 초기화 호출 하나를 잊는 경우에는 도움이 안 된다.

```scala
// 사용 불가...
val action = new MyAction()
    .text("My Action Text")
    .tooltip("My Action Tool tip")
    // 이런
// 준비 안 됨
```

[빌더 클래스](https://en.wikipedia.org/wiki/Builder_pattern)를 써서 플루언트 생성자의 각 단계를 진짜 `MyAction`이 아닌 빌더로 만들면 이를 우회하는 방법이 있다. 미완성 action을 실수로 사용하는 것은 막아 주지만, 작성해야 할 보일러플레이트가 어마어마하다!

다행히 Scala에서는 특별한 플루언트 빌더 클래스가 필요 없다. 그냥 생성자를 쓰면 되고, 일부 인자에 기본값을 줄 수도 있다(Java에서는 할 수 없는 일이고, 그래서 플루언트/빌더 패턴이 생겨났다). Scala에서는 클래스를 이렇게 정의하는 것이 훨씬 단순하다.

```scala
class MyAction(text: String, image: String, tooltip: String = "")
```

호출은 이렇게 하거나

```scala
val action = new MyAction("My Action Text", "Some Image")
```

이렇게 한다.

```scala
val action = new MyAction(
  text = "My Action Text",
  image = "Some Image"
)
```

그리고 누군가 반쯤 완성된 클래스를 잘못 쓸 걱정을 할 필요가 없다. 인스턴스화 지점 주변에 `action` 인스턴스가 존재하지만 사용할 준비는 안 된 구간이 없기 때문이다.

```scala
// 사용 불가...
val action = new MyAction(
  text = "My Action Text",
  image = "Some Image"
)
// ...준비 완료
```

필수 인자를 잊으면 컴파일러가 친절하게 알려 준다.

```scala
val action = new MyAction(
  text = "My Action Text",
  tooltip = "My Action Tool tip"
)
// not enough arguments for constructor MyAction: (text: String, image: String, tooltip: String)ammonite.session.cmd18.MyAction.
// Unspecified value parameter image.
// val action = new MyAction(
//              ^
// Compilation Failed
```

이 경우는 단순하지만, 생성자가 크고 다루기 불편해지더라도 이 스타일을 유지하려고 노력해야 한다. 어쩌면 생성자에 인자가 11개일 수도 있다.

```scala
class Interpreter(prompt0: Ref[String],
                  frontEnd0: Ref[FrontEnd],
                  width: => Int,
                  height: => Int,
                  colors0: Ref[Colors],
                  printer: Printer,
                  storage: Storage,
                  history: => History,
                  predef: String,
                  wd: Path,
                  replArgs: Seq[Bind[_]]){
  ...
}
```

그렇더라도, 조각조각 초기화하려다가 누군가 반쯤 구워진 인스턴스를 사용해서 이상한 버그와 씨름하게 될 위험을 무릅쓰는 것보다는, 전부 넘겨서 인스턴스를 한 번에 초기화하는 편이 낫다.

### 컬렉션 초기화

Java 생태계의 기존 라이브러리 다수에서는, 무언가를 만들고 시간을 들여 초기화하면서, 초기화가 끝나기 전에는 아무도 그것을 쓰지 않기를 바라는 방식이 흔하다.

흔한 골칫거리 하나가 Java 컬렉션 라이브러리다.

```scala
val listA = new java.util.ArrayList[String]();

listA.add("element 1");
listA.add("element 2");
listA.add("element 3");
```

`ArrayList`는 가변 컬렉션이지만 가변적으로 쓰이지 않는 경우가 많다. 빈 채로 만들고, 내용을 넣어 초기화한 뒤, 그 후로는 절대 바꾸지 않는다. 하지만 `listA`가 사용할 준비가 안 된 지점들을 살펴보면, 코드를 작성하는 누군가가 준비 안 된 `ArrayList`를 손에 넣을 수 있는 공간이 많다는 것을 알 수 있다.

```scala
// 사용 불가...
val listA = new java.util.ArrayList[String]();
// 준비 안 됨
listA.add("element 1");
// 준비 안 됨
listA.add("element 2");
// 준비 안 됨
listA.add("element 3");
// ...준비 완료
```

작은 예제라 실수할 가능성이 낮아 보일 수 있지만, 더 큰 실제 코드베이스에서 이 `준비 안 됨` 지점들은 전부 터지기를 기다리는 버그다. 누군가 어떤 초기화 메서드를 호출하는 코드를 추가했는데, 그 메서드가 `준비 안 됨` 상태의 `listA`를 발견하고 그냥 사용해 버려 이상한 버그가 생기는 것이다.

반면 Scala에서 리스트 초기화를 생각해 보면

```scala
// 사용 불가...
val listB = List[String](
  "element 1",
  "element 2",
  "element 3"
)
// ...준비 완료
```

`사용 불가`에서 `준비 완료`로 곧바로 건너뛰며, 중간 상태가 바깥세상에 전혀 노출되지 않는 것을 볼 수 있다. 물론 `List(...)` 생성자 *안에서는* 리스트를 조각조각 조립하는 데 시간이 걸리겠지만, *외부 코드*는 그중 아무것도 볼 수 없다. 역시 외부 코드에는 `listB`가 `사용 불가`에서 `준비 완료`로 "즉시" 건너뛰는 것만 보이고, 다른 코드에서 부분적으로 미완성인 리스트를 무심코 사용할 여지가 없다.

Scala 컬렉션 라이브러리는 이미 작성되어 있지만, 언젠가 직접 컬렉션을 작성하게 된다면 이 원칙을 기억하라. 커스텀 컬렉션 데이터 구조를 작성하게 된다면, 한 번에 "즉시" 초기화할 수 있도록 만들어라.

## 결론

이 글에서는 다음 가이드라인들을 다뤘다.

- 불투명하게 또는 투명하게?
- 데이터 타입은 전역적(Total)이어야 한다
- 데이터 타입은 정규화되어야 한다
- 데이터 타입은 즉시 초기화되어야 한다

이것들은 내 Scala 프로그램에서 데이터를 `class`나 `case class`로 어떻게 표현할지 결정할 때 내가 따르는 가이드라인 몇 개와 그 이면의 근거일 뿐이다. 어느 것도 "새롭"거나 학술적으로 흥미롭지 않으며, Scala를 한동안 써 온 사람에게는 실제로 "당연"할 것이다. 그래도 Scala 코드에 대해 이야기하고, 자신만의 데이터 타입을 설계하는 사람에게 Scala 프로그래밍 언어가 제공하는 수많은 해법을 평가할 토대를 쌓으려는 초·중급 Scala 프로그래머에게 유용하기를 바란다.

여러분의 Scala 애플리케이션을 지탱하는 데이터 타입을 설계할 때 즐겨 쓰는 팁은 무엇인가? 아래 댓글로 알려 달라!
