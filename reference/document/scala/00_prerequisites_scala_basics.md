# 시작하기 전에 — Scala 핵심 개념 선행 학습

---

## 콜아웃(인용 블록) 읽는 법

이후 문서들에는 원문 번역 사이사이에 다음 세 종류의 보충 설명이 들어 있습니다.

> 📘 **처음 배우는 분께** — 그 자리에서 알아야 할 개념을 짧게 설명합니다. (이게 *무엇*인가)

> 💡 **왜 필요한가** — 이 기능이 실제로 어떤 문제를 푸는지 설명합니다. (이게 *왜* 있는가)

> ⚠️ **짚고 넘어가기** — 직역이라 흐릿한 부분, 오해하기 쉬운 부분을 바로잡습니다.

원문 번역은 그대로이며, 이 콜아웃만 추가로 삽입된 것입니다.

---

## 1. Scala의 가장 기본적인 사고방식: "거의 모든 것이 표현식"

Scala에서는 `if`, `match`, 블록 `{ ... }` 같은 구문이 **값을 반환하는 표현식(expression)**입니다.
Java의 `if`는 문(statement)이라 값이 없지만, Scala의 `if`는 값을 반환합니다.

```scala
val x = if (n > 0) "양수" else "음수 또는 0"   // if가 문자열을 돌려줌
```

- `val` : 한 번 정해지면 바뀌지 않는 값 (불변). 기본적으로 `val`을 씁니다.
- `var` : 바뀔 수 있는 변수 (가변). 되도록 피합니다.
- `def` : 메서드(함수) 정의.
- `object` : 인스턴스가 딱 하나뿐인 싱글턴 객체. (Java의 `static` 자리에 대신 쓰임)

함수형 언어의 성격이 강하므로, "값을 바꾼다"기보다 "새 값을 만들어 반환한다"는 방식으로 코드를 작성합니다.

---

## 2. `trait` — 인터페이스 + 믹스인

`trait`는 Java의 `interface`와 비슷하지만 **구현(메서드 본문)과 필드도 가질 수 있는** 더 강력한 개념입니다.
하나의 클래스가 여러 trait를 함께 상속받을 수 있어서 "믹스인(mixin)"이라고도 부릅니다.

```scala
trait Greeter:
  def greet(name: String): String = s"Hello, $name"   // 기본 구현이 있음

class PoliteGreeter extends Greeter   // 그대로 물려받음
```

> 이후 문서에서 "트레이트 매개변수(trait parameter)", "투명 트레이트(transparent trait)" 같은 표현이
> 나오는데, 전부 이 `trait`를 확장한 이야기입니다.

---

## 3. `case class`와 패턴 매칭 — 데이터를 담고 분해하는 도구

`case class`는 "데이터를 담는 용도"의 클래스를 최소한의 코드로 정의할 수 있게 해줍니다.
일반 클래스와 달리 다음 기능이 자동으로 제공됩니다.

- `new` 없이 생성: `Point(1, 2)`
- 값 비교(`==`)가 내용 기준으로 동작
- 보기 좋은 `toString`
- **패턴 매칭으로 분해**가 가능

```scala
case class Point(x: Int, y: Int)

val p = Point(1, 2)
p match
  case Point(0, 0) => "원점"
  case Point(x, y) => s"($x, $y)"   // x, y로 내용을 꺼냄
```

`match`는 다른 언어의 `switch`보다 훨씬 강력하며, **구조를 분해하면서 분기**합니다.
함수형 Scala 코드의 핵심 도구입니다.

---

## 4. `sealed` — "이 타입의 자식은 여기 있는 게 전부다"

`sealed`를 붙이면 해당 타입의 상속이 **같은 파일 내로 제한**됩니다.
덕분에 컴파일러가 "가능한 모든 경우"를 파악할 수 있어, `match`에서 빠뜨린 경우가 있으면 경고해 줍니다.

```scala
sealed trait Shape
case class Circle(r: Double) extends Shape
case class Square(s: Double) extends Shape
// 이 파일 밖에서는 Shape를 더 상속할 수 없음
```

이 "sealed trait + case class들" 조합이 곧 아래 8번의 **ADT**입니다.
Scala 3의 `enum`은 바로 이 패턴을 짧게 쓰기 위한 문법이라, 05번 문서의 핵심 배경이 됩니다.

---

## 5. 컴패니언 객체(companion object)

클래스와 **같은 이름**으로 같은 파일에 선언한 `object`를 컴패니언 객체라 합니다.
보통 해당 타입과 관련된 유틸리티·생성 함수를 모아두는 용도로 사용합니다. (Java의 static 멤버 역할)

```scala
class Pizza(val toppings: List[String])

object Pizza:                       // Pizza의 컴패니언 객체
  def cheese = new Pizza(List("cheese"))   // 미리 만들어 둔 인스턴스
```

---

## 6. 제네릭(타입 매개변수)과 변성(variance)

`List[A]`처럼 대괄호로 받는 것이 **타입 매개변수**입니다. (Java의 `<A>` 제네릭과 같은 개념)

**변성(variance)**은 "`Dog`가 `Animal`의 하위 타입일 때, `List[Dog]`도 `List[Animal]`의 하위 타입으로 취급할 수 있는가?"에 대한 규칙입니다.

- `List[+A]` : **공변(covariant)**. `Dog <: Animal`이면 `List[Dog] <: List[Animal]`. (`+`)
- `Foo[-A]` : **반공변(contravariant)**. 방향이 반대. (`-`)
- `Bar[A]` : **무공변(invariant)**. 아무 관계 없음. (기본값)

05번(변성 고려), 04번(타입) 문서에서 `+`, `-` 기호가 이 뜻으로 나옵니다.

---

## 7. 고차 타입(higher-kinded types)

`List` 자체는 타입이 아니라 **"타입을 받아 타입을 만드는 것"**입니다(`List[Int]`가 되어야 비로소 완전한 타입).
이처럼 "타입을 생성하는 타입"을 다루는 기능을 고차 타입이라고 합니다.

```scala
trait Functor[F[_]]:   // F는 List, Option 처럼 "타입 하나를 받는 타입 생성자"
  def map[A, B](fa: F[A])(f: A => B): F[B]
```

`F[_]`의 밑줄은 "여기에 타입 하나가 들어갈 자리가 있다"는 뜻입니다.
01번 문서의 "타입 람다", "종류 다형성(kind polymorphism)"이 이 주제의 연장선입니다.

---

## 8. 대수적 데이터 타입(ADT)

ADT는 "데이터를 **합(OR)**과 **곱(AND)**으로 조합하는 방식"입니다.

- **곱(product)**: 여러 값을 동시에 가짐 → `case class Point(x: Int, y: Int)` (x **그리고** y)
- **합(sum)**: 여러 모양 중 하나 → `sealed trait Shape` 아래 `Circle` **또는** `Square`

```scala
sealed trait Tree
case class Leaf(value: Int) extends Tree
case class Branch(left: Tree, right: Tree) extends Tree
```

"정해진 몇 가지 경우로만 구성된 데이터"를 표현하는 것이 ADT이며,
`match`로 모든 경우를 안전하게 처리할 수 있습니다. 05번 문서의 핵심 주제입니다.

---

## 9. `implicit` — Scala 2의 "컴파일러가 알아서 채워주는 것" (가장 중요)

`implicit`은 이후 문서(특히 02·03번)를 이해하는 데 가장 중요한 개념입니다.
한 문장으로: **"코드에 명시하지 않아도 컴파일러가 스코프 내에서 적절한 값을 찾아 자동으로 채워 넣는 장치"**입니다.

Scala 2에서 `implicit` 키워드는 **네 가지 서로 다른 용도**로 쓰였습니다. (Scala 3는 이를 의도별로 분리했습니다 → 02·03번 문서.)

**(1) 암시적 매개변수 (implicit parameter)** — 인자를 생략하면 컴파일러가 채워줌

```scala
def greet(implicit name: String): String = s"Hi, $name"

implicit val me: String = "Kim"   // 범위 안에 implicit 값을 둠
greet   // name을 안 넘겨도 me가 자동으로 들어감 → "Hi, Kim"
```

→ Scala 3에서는 `using`(매개변수)과 `given`(값)으로 분리됩니다. **(02번 문서)**

**(2) 암시적 변환 (implicit conversion)** — 타입을 자동으로 바꿔줌

```scala
implicit def intToStr(n: Int): String = n.toString
val s: String = 42   // Int인데 String 자리에 들어감 → 자동 변환됨
```

→ 지나치게 암묵적이라 위험할 수 있습니다. Scala 3에서는 `Conversion` 타입으로 명시화됩니다. **(10번 문서)**

**(3) 암시적 클래스 (implicit class)** — 기존 타입에 메서드를 덧붙임

```scala
implicit class IntOps(n: Int):
  def squared: Int = n * n
3.squared   // Int에 없던 메서드를 쓴 것처럼 → 9
```

→ Scala 3에서는 `extension`(확장 메서드)으로 대체됩니다. **(03번 문서)**

**(4) 타입 클래스 패턴** — 아래 10번 참고. **(03번 문서)**

> 핵심: `implicit`은 "편리하지만 어디서 무엇이 끼어들었는지 추적하기 어렵다"는 비판을 받았고,
> Scala 3는 이를 `given` / `using` / `extension` / `Conversion`으로 **의도별로 분리**해 명확하게 만들었습니다.
> 02번 문서가 처음부터 "implicit 비판 5가지"로 시작하는 이유가 바로 이것입니다.

---

## 10. 타입 클래스(type class) 패턴

타입 클래스는 "이미 만들어진 타입(수정할 수 없는 타입 포함)에 **나중에 기능을 추가하는**" 방법입니다.
상속과 달리, 타입 자체를 수정하지 않고 외부에서 기능을 부여합니다.

생각하는 순서:
1. "이런 능력이 필요하다"는 인터페이스를 `trait`로 정의 (예: 비교할 수 있음 `Ord[T]`)
2. 특정 타입(예: `Int`)에 대한 그 능력의 구현을 **implicit/given으로** 제공
3. 함수는 "그 능력을 가진 타입이면 받겠다"고 요구

```scala
trait Show[A]:                          // 1. "문자열로 보여줄 수 있다"는 능력
  def show(a: A): String

given Show[Int] with                    // 2. Int에 대한 구현 (Scala 3 문법)
  def show(a: Int) = s"Int($a)"

def print[A](a: A)(using s: Show[A]) =  // 3. Show 능력이 있는 A만 받음
  println(s.show(a))
```

이 패턴이 03번 문서의 전체 주제입니다. Scala 2에서는 구현을 `implicit val/def`로 제공했고,
Scala 3에서는 `given`으로 제공합니다.

---

## 11. 자잘하지만 문서에 나오는 것들

- **value class** : 런타임 오버헤드 없이 타입을 한 겹 감싸는 클래스(`extends AnyVal`). Scala 3의 **opaque type**이 많은 경우 이를 대체합니다. (04·06번)
- **package object** : 패키지 단위로 공용 정의를 모아두던 곳. Scala 3는 파일 최상위에 **최상위 정의(top-level definition)**를 바로 선언할 수 있어 거의 불필요해졌습니다. (07번)
- **by-name 매개변수** (`x: => T`) : 인자를 **전달 시점이 아니라 사용 시점에** 평가합니다(지연 평가). (02·10번)
- **디슈가링(desugaring)** : 편의 문법(syntactic sugar)을 컴파일러가 더 기본적인 형태로 변환하는 과정. "이 문법은 사실 이런 코드와 동일하다"는 설명에서 자주 등장합니다. (05·06번)
- **에타 확장(eta expansion)** : 메서드(`def`)를 함수 값(`f: A => B`)으로 자동 변환하는 것. (10번)

---

## 12. Scala 2 → Scala 3 대응표 (한눈에)

이후 문서를 읽다가 "이게 Scala 2의 무엇을 대체한 건지?" 궁금할 때 참고하세요.

| Scala 2 | Scala 3 | 관련 문서 |
|---|---|---|
| `implicit val` / `implicit object` | `given` | 02 |
| `implicit` 매개변수 | `using` 절 | 02 |
| `implicit class` | `extension` (확장 메서드) | 03 |
| `implicit def`(변환) | `Conversion` 인스턴스 | 02, 10 |
| `sealed trait` + `case class` (ADT) | `enum` | 05 |
| value class (`AnyVal`) | opaque type | 04, 06 |
| package object | 최상위 정의(top-level) | 07 |
| 합성 타입 `A with B` | 교집합 타입 `A & B` | 04, 10 |
| 와일드카드 타입 `Foo[_]` | `Foo[?]` | 07, 10 |
| `def f(x: T) { ... }` (프로시저 문법) | `def f(x: T): Unit = { ... }` | 11 |
| scala-reflect 매크로(실험적) | inline + quotes/splices | 08, 09 |

---

## 추천 학습 순서

처음 학습하는 분이라면 다음 순서를 권합니다.

1. **이 문서(00)** — 기초 개념
2. **01** — Scala 3 전체 조감도 (어떤 게 있는지 지도 보기)
3. **05** — enum/ADT (가장 직관적이고 실용적)
4. **02 → 03** — given/using → 확장 메서드/타입 클래스 (Scala 3의 핵심, 가장 어려움)
5. **04, 06, 07** — 타입/모델링/문법 등 나머지 기능
6. **10, 11, 12** — 바뀐 것 / 사라진 것 / 마이그레이션
7. **08, 09** — 메타프로그래밍 (가장 고급, 나중에)

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Scala Book (입문자용 튜토리얼)](https://docs.scala-lang.org/scala3/book/introduction.html)
- [Scala 3 Reference](https://docs.scala-lang.org/scala3/reference/)
