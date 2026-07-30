# Scala 3 컨텍스트 추상화: given/using, 확장 메서드, 타입 클래스

## Scala 3 컨텍스트 추상화: given/using

---

### 목차

1. [컨텍스트 추상화 개요](#1-컨텍스트-추상화-개요)
2. [주어진 인스턴스 (Given Instances)](#2-주어진-인스턴스-given-instances)
3. [using 절 (Using Clauses)](#3-using-절-using-clauses)
4. [컨텍스트 바운드 (Context Bounds)](#4-컨텍스트-바운드-context-bounds)
5. [given 임포트 (Given Imports)](#5-given-임포트-given-imports)
6. [이름에 의한 컨텍스트 매개변수 (By-Name Context Parameters)](#6-이름에-의한-컨텍스트-매개변수-by-name-context-parameters)
7. [Scala 2 implicit와의 관계](#7-scala-2-implicit와의-관계)
8. [참고 자료](#참고-자료)

---

### 1. 컨텍스트 추상화 개요

> 📘 **처음 배우는 분께 — implicit이 뭔가요?**
>
> `implicit`은 Scala 2의 핵심 장치로, 한 문장으로 "**코드에 직접 안 써도 컴파일러가 범위 안에서 알맞은 값을 찾아 자동으로 채워 넣어 주는 것**"입니다.
>
> 예를 들어 `def greet(implicit name: String)`처럼 매개변수에 `implicit`을 붙여 두면, 범위 안에 `implicit val me = "Kim"` 같은 값이 있을 때 `greet`만 호출해도 컴파일러가 `me`를 자동으로 넘겨 줍니다.
>
> 편리하지만 "어디서 무엇이 끼어들었는지" 추적하기 어렵다는 비판을 받았습니다. 그래서 Scala 3는 이 한 단어를 용도별로 `given`(값 제공) / `using`(매개변수) / `extension` / `Conversion`으로 쪼갰습니다. 이 문서가 "implicit 비판 5가지"로 시작하는 이유가 바로 이것입니다. (선행 학습 문서 00번 9·10번 항목 참고)

컨텍스트 추상화(Contextual Abstraction)는 항(term)을 암시적으로 추론(implicit term inference)하는 메커니즘으로, Scala 언어에서 가장 강력하면서도 가장 많은 논쟁을 낳은 기능 중 하나입니다. Scala 3는 이 컨텍스트 추상화를 처리하는 방식을 전면적으로 재설계했습니다.

> ⚠️ **짚고 넘어가기 — "항(term)을 암시적으로 추론한다"는 말**
>
> 여기서 **항**(term)은 "값으로 평가되는 코드 조각"(예: `intOrd`, `42`, `me`)을 가리키는 용어입니다. 타입(type)과 대비되는 말이라고 보면 됩니다.
>
> 따라서 "항을 암시적으로 추론한다(implicit term inference)"는 곧 "**컴파일러가 빠진 값을 알아서 찾아 끼워 넣는다**"는 뜻입니다. 위 implicit 설명과 같은 이야기를 어렵게 표현한 것뿐입니다.

#### 1.1 기존 implicit 시스템에 대한 비판

Scala 3 문서는 Scala의 기존 implicit 설계에 대해 다섯 가지 주요 비판점을 제시합니다.

**(1) 과도한 사용과 오용 (Overuse and Misuse)**
`implicit` 수식어(modifier)가 너무 많은 언어 구문에 적용되어, 암시적 값(implicit value)과 암시적 변환(implicit conversion)을 혼동하기 쉽습니다. 다음 두 정의는 구문적으로 매우 유사하지만 근본적으로 다른 목적을 가집니다.

```scala
implicit def i1(implicit x: T): C[T]
implicit def i2(x: T): C[T]
```

`i1`은 암시적 매개변수를 받아 `C[T]`를 합성하는 정의이고, `i2`는 `T`를 `C[T]`로 변환하는 암시적 변환입니다. 그러나 이 둘을 구별할 명확한 표시가 없습니다.

> 📘 **처음 배우는 분께 — 위 두 줄이 왜 헷갈리는가**
>
> 둘 다 `implicit def`로 시작하지만 하는 일이 완전히 다릅니다.
> - `i1`: 괄호 안 `(implicit x: T)` → "x를 자동으로 받아서" `C[T]`를 **만들어 주는** 도구.
> - `i2`: 괄호 안 `(x: T)` → `T`를 `C[T]`로 **변환해 주는** 마법(자동 형변환).
>
> 사람 눈에는 거의 똑같이 생겼는데 의미는 정반대라서, "이 implicit이 대체 무슨 역할이지?"를 매번 헷갈리게 만든다는 비판입니다.

**(2) 숨겨진 의존성 (Hidden Dependencies)**
implicit은 임포트(import) 내 어디에나 등장할 수 있어서, 프로그램이 실제로 어떤 implicit을 사용하는지 추적하기 어렵습니다. 이 때문에 "올바른 임포트 문을 추가하면 신비롭게 해결되는" 혼란스러운 타입 오류가 자주 발생합니다.

**(3) 간접적인 의도 표현 (Indirect Intent Signaling)**
`implicit` 수식어는 의도(intent)가 아니라 메커니즘(mechanism)만 전달합니다. 그 기저에 깔린 디슈가링(desugaring) 규칙을 이해하지 못하면 서로 다른 implicit 사용 사례를 구별하기 어렵습니다.

> ⚠️ **짚고 넘어가기 — "의도 vs 메커니즘"**
>
> `implicit`이라는 단어는 "이건 암시적으로 처리돼"라는 **방법**(메커니즘)만 말해 줄 뿐, "값을 제공하려는 건지 / 변환하려는 건지"라는 **목적**(의도)은 알려 주지 않습니다.
>
> Scala 3는 목적별로 이름을 다르게 지어(`given`=값 제공, `using`=값 받기 등) 코드만 봐도 의도가 드러나게 했습니다. 참고로 디슈가링(desugaring)은 "편한 문법을 컴파일러가 더 기본적인 형태로 풀어내는 과정"을 뜻합니다.

**(4) 매개변수 구문의 한계 (Parameter Syntax Limitations)**
암시적 매개변수(implicit parameter)는 인자(argument) 전달 구문에서 대칭성이 부족합니다. 예를 들어 다음 메서드는

```scala
def currentMap(implicit ctx: Context): Map[String, Int]
```

암시적 매개변수 구역(section) 뒤에 일반 인자를 받을 수 없습니다. 이 때문에 `.apply()` 호출 같은 어색한 우회 방법을 써야 합니다. 또한 메서드는 단 하나의 암시적 매개변수 구역만 가질 수 있으며, 그것은 반드시 마지막에 위치해야 합니다.

**(5) 도구 지원의 어려움 (Tooling Challenges)**
컨텍스트에 의존하는 implicit은 IDE 자동 완성, Scaladoc 문서 생성, 오류 진단을 복잡하게 만듭니다. implicit 검색에 실패하면 불명확한 오류 메시지가 출력되는 경우가 많습니다.

#### 1.2 재설계된 접근법

이러한 문제를 해결하기 위해 Scala 3는 네 가지 근본적인 변화를 도입했습니다.

> 💡 **왜 필요한가 — 한 단어를 넷으로 쪼갠 이유**
>
> Scala 2는 `implicit` 한 단어로 (1) 값 제공, (2) 값 받기, (3) 메서드 덧붙이기, (4) 타입 변환을 **전부** 했습니다. 그래서 위 비판처럼 헷갈렸습니다.
>
> Scala 3는 이 네 가지 일을 각각 다른 문법으로 분리했습니다: `given`(값 제공) · `using`(값 받기) · `extension`(메서드 덧붙이기, 03번 문서) · `Conversion`(타입 변환). 아래 네 항목이 바로 그 분리입니다.

- **주어진 인스턴스 (Given Instances)**: 암시적 정의(implicit definition)를 전용 구문으로 대체합니다. 여러 언어 구문에 `implicit`을 흩뿌리는 대신, 합성 가능한(synthesizable) 항을 정의하는 단일 메커니즘을 제공합니다.
- **using 절 (Using Clauses)**: 암시적 매개변수와 인자를 위한 명시적 구문을 제공합니다. 어떤 매개변수가 추론된 값을 받는지에 대한 모호함을 제거하고, 여러 개의 매개변수 구역을 허용합니다.
- **given 임포트 (Given Imports)**: 주어진 인스턴스만을 위한 전용 임포트 범주를 만들어, 일반 임포트 목록 안에 숨어 들어가는 것을 방지합니다.
- **명시적 변환 클래스 (Explicit Conversion Class)**: 암시적 변환을 표준 `Conversion` 클래스로 통합하여, 규율 없는(undisciplined) 변환 구문을 제거합니다.

#### 1.3 관련 기능

이 섹션은 다음 기능들도 다룹니다.

- 컨텍스트 바운드(context bound) — 이전 버전과 동일하게 유지
- 확장 메서드(extension method) — 암시적 클래스(implicit class)를 대체
- 타입 클래스(type class) 구현 패턴
- 대수적 데이터 타입(ADT)에 대한 타입 클래스 도출(type class derivation)
- 다중우주 동등성(multiversal equality)
- 컨텍스트 함수(context function)
- 이름에 의한 컨텍스트 매개변수(by-name context parameter)
- Scala 2 implicit으로부터의 마이그레이션 안내

#### 1.4 설계 철학

이 재설계는 관심사의 분리(separation of concerns)를 강조합니다. given 정의를 위한 별도 구문, 매개변수를 위한 using 절, 범위 관리를 위한 given 임포트, 암시적 변환을 위한 변환 클래스를 각각 명확히 구분합니다. 이 접근법은 기능 간 상호작용을 줄이고, 일관성을 높이며, 숙련된 개발자와 입문자 모두에게 효과적으로 작동하는 직교적(orthogonal)인 언어 구조를 만드는 것을 목표로 합니다.

---

### 2. 주어진 인스턴스 (Given Instances)

#### 2.1 개요

> 📘 **처음 배우는 분께 — given이란**
>
> `given`은 **"이 타입에는 이 값을 표준으로 써라"하고 컴파일러에게 미리 등록해 두는 것**입니다. Scala 2에서 `implicit val`/`implicit object`로 하던 일을 대신합니다.
>
> 예를 들어 `given intOrd: Ord[Int]: ...`라고 써 두면, 나중에 누군가 "`Ord[Int]`가 필요해"라고 할 때 컴파일러가 자동으로 이 `intOrd`를 가져다 씁니다. 즉 **값을 "제공"하는 쪽**이 `given`입니다.

주어진 인스턴스(given instance), 줄여서 "givens"는 특정 타입의 정준(canonical) 값을 정의하여, 컨텍스트 매개변수(context parameter)에 대한 인자를 합성(synthesize)하는 데 사용됩니다. 이는 Scala 3 컨텍스트 추상화 시스템의 핵심을 이룹니다.

> ⚠️ **짚고 넘어가기 — "정준(canonical)" / "합성(synthesize)"**
>
> - **정준 값(canonical value)**: "그 타입에 대한 **대표(기본) 값**"이라는 뜻입니다. `Ord[Int]`라면 "정수를 어떻게 비교할지에 대한 표준 답안" 하나를 정해 두는 셈입니다.
> - **합성(synthesize)**: "컴파일러가 빠진 인자를 **알아서 만들어/찾아 채워 넣는다**"는 뜻입니다. 새 객체를 마법으로 지어낸다기보다, 등록된 given을 골라 끼워 넣는 작업으로 이해하면 됩니다.

#### 2.2 기본 구조

주어진 인스턴스 선언은 `given` 키워드로 시작합니다. 다음은 대표적인 예입니다.

```scala
trait Ord[T]:
  def compare(x: T, y: T): Int
  extension (x: T)
    def < (y: T) = compare(x, y) < 0
    def > (y: T) = compare(x, y) > 0

given intOrd: Ord[Int]:
  def compare(x: Int, y: Int) =
    if x < y then -1 else if x > y then +1 else 0
```

여기서 `intOrd`는 `Ord[Int]` 타입의 주어진 인스턴스이며, `compare` 메서드를 구현합니다.

조건부(conditional) givens는 컨텍스트 바운드(context bound)를 통해 다른 인스턴스에 의존할 수 있습니다.

```scala
given listOrd: [T: Ord] => Ord[List[T]]:
  def compare(xs: List[T], ys: List[T]): Int = (xs, ys) match
    case (Nil, Nil) => 0
    case (Nil, _) => -1
    case (_, Nil) => +1
    case (x :: xs1, y :: ys1) =>
      val fst = summon[Ord[T]].compare(x, y)
      if fst != 0 then fst else compare(xs1, ys1)
```

위 정의는 "`Ord[T]`에 대한 주어진 인스턴스가 존재한다면, `Ord[List[T]]`에 대한 주어진 인스턴스를 만들 수 있다"는 의미입니다. `[T: Ord] =>` 부분이 조건(즉, 컨텍스트 매개변수 `using Ord[T]`)을 나타냅니다.

> 💡 **왜 필요한가 — 조건부 given의 쓸모**
>
> "정수 비교법(`Ord[Int]`)"만 등록해 두면, "정수 **리스트**"든 "정수 리스트의 리스트"든 자동으로 비교할 수 있게 됩니다.
>
> 위 코드는 곧 "원소를 비교할 수 있으면(`Ord[T]`), 그 원소들의 리스트도 비교해 주겠다(`Ord[List[T]]`)"는 **레시피**입니다. 컴파일러가 이 레시피를 재귀적으로 조립해, `Int` 하나만 알려 줘도 `List[List[Int]]`까지 비교법을 스스로 만들어 냅니다. 코드 안의 `summon[Ord[T]]`는 "지금 범위에 등록된 `Ord[T]`를 꺼내 줘"라는 뜻입니다(아래 3.8 참고).

#### 2.3 익명 givens (Anonymous Givens)

> 📘 **처음 배우는 분께 — 이름 없는 given**
>
> given은 주로 "컴파일러가 타입을 보고 자동으로 골라 쓰는" 용도라, 사람이 이름으로 직접 부를 일이 거의 없습니다. 그래서 **이름을 생략**해도 됩니다.
>
> 이름을 비워 두면 컴파일러가 타입으로부터 `given_Ord_Int` 같은 이름을 알아서 붙입니다. "값은 등록하되 이름은 신경 쓰지 않겠다"는 흔한 경우에 씁니다.

이름은 생략할 수 있으며, 이 경우 컴파일러가 구현된 타입으로부터 식별자를 합성합니다. 예를 들어 이름 없는 `Ord[Int]`는 `given_Ord_Int`가 되고, 이름 없는 `Ord[List[T]]`는 `given_Ord_List`가 됩니다.

#### 2.4 별칭 givens (Alias Givens)

별칭 givens는 표현식(expression)을 사용하여 인스턴스를 정의합니다.

```scala
given global: ExecutionContext = ForkJoinPool()
```

이것은 익명으로도 가능합니다.

```scala
given Position = enclosingTree.position
```

이러한 별칭 givens는 첫 접근 시점에 초기화됩니다. 무조건적(unconditional) 인스턴스의 경우 초기화는 스레드 안전(thread-safe)하며 캐시됩니다.

#### 2.5 초기화 동작 (Initialization Behavior)

- 매개변수가 없는 **무조건적 givens**는 첫 접근 시점에 요청에 따라(on-demand) 초기화됩니다.
- **불변(immutable) 별칭 givens**는 캐시된 필드 오버헤드 없이 단순한 포워더(forwarder)로 동작합니다.
- **조건부 givens**는 참조될 때마다 새로운 인스턴스를 생성합니다.

#### 2.6 구문 개요

givens는 다음 패턴을 따릅니다.

- 예약어 `given`
- 선택적 이름과 콜론(`:`)
- 선택적 조건(타입 매개변수, 항 매개변수, 또는 함수 인자 타입)
- 단일 구현 타입과 함께
  - `=` 표현식(별칭 형태, alias form), 또는
  - 생성자 적용(constructor application)과 템플릿 본문(structural form, 구조적 형태)

---

### 3. using 절 (Using Clauses)

#### 3.1 개요

> 📘 **처음 배우는 분께 — using이란**
>
> `using`은 given과 짝을 이루는 **받는 쪽**입니다. 매개변수에 `using`을 붙이면 "이 인자는 내가 직접 안 넘겨도 컴파일러가 등록된 given에서 찾아 채워 줘"라는 뜻이 됩니다. Scala 2의 `implicit` **매개변수** 자리를 대신합니다.
>
> 정리하면: `given`은 값을 **공급**하고, `using`은 그 값을 **요청**합니다. 둘이 타입으로 짝지어집니다.

함수형 프로그래밍은 대부분의 의존성을 단순한 함수 매개변수화(function parameterization)로 표현하는 경향이 있습니다. 그러나 이는 반복적인 인자 전달로 이어질 수 있습니다. 컨텍스트 매개변수(context parameter)를 사용하면 컴파일러가 인자를 자동으로 합성하여 이 문제를 해결합니다.

> 💡 **왜 필요한가 — "반복적인 인자 전달"이란**
>
> 예를 들어 정렬·비교 로직 여기저기서 `Ord[T]`를 인자로 넘겨야 한다면, 함수마다 `ord`를 손으로 끼워 전달하는 코드가 끝없이 반복됩니다.
>
> `using`을 쓰면 그 값을 한 번 given으로 등록해 두고, 이후엔 컴파일러가 알아서 전달합니다. "어디서나 필요하지만 매번 손으로 넘기긴 귀찮은 값"(실행 컨텍스트, 설정, 비교 규칙 등)을 깔끔하게 다루는 장치입니다.

#### 3.2 기본 구문

컨텍스트 매개변수는 `using` 키워드로 도입됩니다.

```scala
def max[T](x: T, y: T)(using ord: Ord[T]): T =
  if ord.compare(x, y) < 0 then y else x
```

이 함수는 인자를 명시적으로 전달하거나, 암시적 해소(implicit resolution)를 통해 호출할 수 있습니다.

```scala
max(2, 3)(using intOrd)  // 명시적
max(2, 3)                // 암시적
```

> ⚠️ **짚고 넘어가기 — Scala 2와 달라진 점**
>
> Scala 2에서는 명시적으로 넘길 때 `max(2, 3)(intOrd)`처럼 그냥 인자처럼 넣었습니다. 이러면 "이게 보통 인자인지 implicit 인자인지" 구분이 안 됐습니다.
>
> Scala 3는 받을 때도 `using`, **직접 넘길 때도** `(using intOrd)`처럼 `using`을 붙이게 했습니다. 덕분에 "이건 컨텍스트 인자다"가 호출부에서도 한눈에 보입니다. 물론 평소엔 `max(2, 3)`처럼 생략해 자동으로 채우게 두면 됩니다.

#### 3.3 익명 컨텍스트 매개변수 (Anonymous Context Parameters)

컨텍스트 매개변수가 다른 함수로 단순히 전달(forward)되기만 할 때는 그 이름을 생략할 수 있습니다.

```scala
def maximum[T](xs: List[T])(using Ord[T]): T =
  xs.reduceLeft(max)
```

매개변수는 전체 선언 형태인 `(p_1: T_1, ..., p_n: T_n)` 또는 타입만 지정하는 형태인 `T_1, ..., T_n` 중 하나로 지정할 수 있습니다. 다만 가변 인자(vararg) 매개변수는 `using` 절에서 지원되지 않습니다.

#### 3.4 클래스 컨텍스트 매개변수 (Class Context Parameters)

클래스 컨텍스트 매개변수에 `val` 또는 `var`를 붙이면 클래스 멤버(member)가 됩니다.

```scala
class GivenIntBox(using val usingParameter: Int):
  def myInt = summon[Int]

val b = GivenIntBox(using 23)
```

클래스 내부에서 발생할 수 있는 모호성(ambiguity)을 피할 수 있어, 명시적인 `given` 멤버를 별도로 선언하는 것보다 이 방식이 권장됩니다.

#### 3.5 복잡한 인자의 추론 (Inferring Complex Arguments)

컨텍스트 매개변수는 정교한 타입 주도(type-driven) 해소를 가능하게 합니다.

```scala
def descending[T](using asc: Ord[T]): Ord[T] = new Ord[T]:
  def compare(x: T, y: T) = asc.compare(y, x)

def minimum[T](xs: List[T])(using Ord[T]) =
  maximum(xs)(using descending)
```

다음 호출들은 모두 동등하게 정규화(normalize)됩니다.

- `minimum(xs)`
- `maximum(xs)(using descending)`
- `maximum(xs)(using descending(using intOrd))`

#### 3.6 다중 using 절 (Multiple using Clauses)

여러 개의 `using` 절이 공존할 수 있으며, 일반 매개변수와 자유롭게 혼합될 수 있습니다.

```scala
def f(u: Universe)(using ctx: u.Context)(using s: ctx.Symbol, k: ctx.Kind) = ...
```

인자는 왼쪽에서 오른쪽으로 매칭됩니다. 따라서 `ctx`를 먼저 공급하지 않고 `f(global)(using sym, kind)`처럼 호출하면 실패합니다.

#### 3.7 명시적 using과 암시적 using

인자가 `using`을 통해 명시적으로 공급되면 누락된 매개변수는 기본값(default)으로 채워집니다. 암시적으로 공급될 때는 누락된 매개변수에 암시적 값(implicit value)이 사용됩니다. 이 차이는 중요합니다.

```scala
def f(using p: Boolean, i: Int, s: String = "hello, world") = ...

f(using p = false, i = 27)  // s는 암시적 String이 아니라 기본값을 사용
```

기본값이 암시적 값을 가릴(shadow) 때 컴파일러는 경고를 발생시킵니다.

#### 3.8 인스턴스 소환 (Summoning Instances)

> 📘 **처음 배우는 분께 — summon이란**
>
> `summon[T]`은 "**지금 범위에 등록된 타입 `T`의 given을 꺼내 줘**"라고 컴파일러에게 요청하는 명령입니다. ("소환(summon)" = 불러오기)
>
> `using` 매개변수를 따로 선언하지 않고도, 코드 한가운데에서 즉석으로 given이 필요할 때 씁니다. Scala 2의 `implicitly[T]`에 해당합니다. 예: `summon[Ord[Int]]` → 등록된 정수 비교 규칙을 그 자리에서 가져옵니다.

`summon` 메서드는 주어진 인스턴스를 가져옵니다.

```scala
def summon[T](using x: T): x.type = x

summon[Ord[List[Int]]]  // listOrd(using intOrd)로 해소됨
```

#### 3.9 구문 문법 (Syntax Grammar)

```
ClsParamClause      ::=  ... | UsingClsParamClause
DefParamClause      ::=  ... | UsingParamClause
UsingClsParamClause ::=  '(' 'using' (ClsParams | Types) ')'
UsingParamClause    ::=  '(' 'using' (DefTermParams | Types) ')'
ParArgumentExprs    ::=  ... | '(' 'using' ExprsInParens ')'
```

`using` 키워드는 소프트 키워드(soft keyword)로, 매개변수/인자 목록의 시작 위치에서만 키워드로 인식됩니다.

---

### 4. 컨텍스트 바운드 (Context Bounds)

#### 4.1 핵심 개념

> 📘 **처음 배우는 분께 — 컨텍스트 바운드란**
>
> `[T: Ord]`는 "**`T`를 비교할 수 있어야 한다(`Ord[T]`가 있어야 한다)**"는 조건을 타입 자리에 짧게 적은 것입니다. 줄임 표기일 뿐, 풀어 쓰면 `using Ord[T]` 매개변수를 추가하는 것과 똑같습니다.
>
> "이 함수는 비교 가능한 타입에만 쓸 수 있다"는 요구사항을 한 글자로 표현하는 편의 문법입니다.

컨텍스트 바운드(context bound)는 타입 매개변수에 의존하는 컨텍스트 매개변수를 표현하는 약식(shorthand) 구문입니다. `[T: Ord]` 표기법은 `[T](using Ord[T])`와 동등합니다.

> ⚠️ **짚고 넘어가기 — 변경점이 아니라 "유지된 것"**
>
> 다른 기능들과 달리 컨텍스트 바운드는 Scala 2에서도 거의 똑같이 있었습니다(1.3절 "이전 버전과 동일하게 유지"). 이 절은 새 문법 소개라기보다, given/using과 어떻게 맞물리는지를 정리하는 부분입니다.

문서에 명시된 바와 같이: "메서드 또는 클래스의 타입 매개변수 `T`에 붙은 `: Ord` 같은 바운드는, 둘러싸는 메서드의 시그니처에 추가되는 컨텍스트 매개변수 `using Ord[T]`를 나타냅니다."

#### 4.2 이름 있는 컨텍스트 바운드 (Named Context Bounds)

Scala 3.6부터 컨텍스트 바운드는 `as` 절을 통해 이름을 가질 수 있습니다.

```scala
def reduce[A: Monoid as m](xs: List[A]): A =
  xs.foldLeft(m.unit)(_ `combine` _)
```

이는 `def reduce[A](xs: List[A])(using m: Monoid[A]): A`로 확장(expand)되며, 이름 있는 증인(witness) `m`을 메서드 본문에서 직접 참조할 수 있게 합니다.

> ⚠️ **짚고 넘어가기 — "증인(witness)"**
>
> 여기서 **증인**(witness)은 "그 조건이 충족됨을 보증하는 실제 값"을 가리키는 용어입니다. `Monoid[A]`라는 능력이 있다는 것을 실제로 보증하는 그 given 값(`m`)이 곧 증인입니다.
>
> 기존 `[A: Monoid]`만으로는 그 값을 이름으로 부를 수 없었는데, `as m`을 붙이면 본문에서 `m.unit`처럼 직접 쓸 수 있게 됩니다.

#### 4.3 집합 컨텍스트 바운드 (Aggregate Context Bounds)

여러 개의 컨텍스트 바운드는 중괄호(`{}`)를 사용하여 지정할 수 있습니다.

```scala
def showMax[X : {Ord, Show}](x: X, y: X): String
```

이름 있는 변형도 마찬가지로 작동합니다.

```scala
def showMax[X : {Ord as ordering, Show as show}](x: X, y: X): String =
  show.asString(ordering.max(x, y))
```

#### 4.4 매개변수 배치 규칙 (Parameter Placement Rules)

생성된 컨텍스트 매개변수는 다음 세 가지 규칙을 따릅니다.

1. 바운드 이름이 후속 절(clause)에서 참조되면, 해당 using 절을 그 절 앞에 배치합니다.
2. 마지막 매개변수 절이 이미 `using` 절이라면, 새 매개변수를 그 앞에 병합(merge)합니다.
3. 그 외의 경우, 끝에 새로운 using 절을 추가합니다.

**규칙 1에 의한 예:**

```scala
def run[P : Parser as p](in: p.Input): p.Result
// 다음으로 확장됨:
def run[P](using p: Parser[P])(in: p.Input): p.Result
```

여기서 `p`는 `in: p.Input`에서 이름으로 참조되므로, `using p: Parser[P]`가 `(in: p.Input)` 절보다 앞에 배치됩니다.

#### 4.5 다형 함수 타입 (Polymorphic Function Types)

컨텍스트 바운드는 다형 함수 타입(polymorphic function type)과 함께 작동합니다(Scala 3.6+).

```scala
type Comparer = [X: Ord] => (x: X, y: X) => Boolean
val less: Comparer = [X: Ord as ord] => (x: X, y: X) =>
  ord.compare(x, y) < 0
```

이는 컨텍스트 함수 타입(context function type, `?=>`)을 사용하여 확장됩니다.

```scala
type Comparer = [X] => (x: X, y: X) => Ord[X] ?=> Boolean
```

#### 4.6 추상 타입 멤버 (Abstract Type Members)

추상 타입(abstract type)에 대한 컨텍스트 바운드(Scala 3.6+)는 지연된 givens(deferred givens)로 확장됩니다.

```scala
class Collection:
  type Element: Ord
// 다음으로 확장됨:
class Collection:
  type Element
  given Ord[Element] = deferred
```

#### 4.7 구문 (Syntax)

형식 문법은 다음을 포함합니다.

```
ContextBounds     ::=  ContextBound
                    |  '{' ContextBound {',' ContextBound} '}'
ContextBound      ::=  Type ['as' id]
```

---

### 5. given 임포트 (Given Imports)

#### 5.1 핵심 개념

> 📘 **처음 배우는 분께 — given 임포트란**
>
> given은 코드에 안 써도 자동으로 끌려 들어오는 값이라, 어떤 파일에서 무심코 `import` 한 것만으로 동작이 바뀔 수 있습니다. 그래서 Scala 3는 **given만 따로 가져오는 전용 import 문법**을 두었습니다.
>
> 즉 일반 `import A.*`로는 given이 따라오지 않고, given을 쓰려면 `import A.given`처럼 **명시적으로** 가져와야 합니다.

Scala 3는 `given` 인스턴스를 위한 전용 임포트 메커니즘을 제공합니다. 핵심 차이는 다음과 같습니다.

> 일반 와일드카드 선택자 `*`는 givens와 확장(extension)을 제외한 모든 정의를 범위로 가져오는 반면, `given` 선택자는 모든 givens(확장에서 비롯된 것 포함)를 범위로 가져옵니다.

즉 `import A.*`는 일반 정의만 가져오며 주어진 인스턴스는 포함하지 않습니다. 주어진 인스턴스를 가져오려면 `given` 선택자를 명시해야 합니다.

> 💡 **왜 필요한가 — "숨겨진 의존성" 비판을 푸는 곳**
>
> 1.1절의 비판 (2)번 "implicit이 import 목록 어딘가에 숨어 든다"를 정확히 겨냥한 설계입니다. Scala 2에서는 `import A._` 한 줄에 implicit이 슬쩍 섞여 들어와 "왜 이 import를 지웠더니 컴파일이 깨지지?" 같은 혼란이 잦았습니다.
>
> Scala 3는 given을 `*`에서 분리해, "자동으로 끌려오는 값을 가져오겠다"는 의도를 코드에 드러내도록 강제합니다.

#### 5.2 기본 구문

객체로부터 주어진 인스턴스만 임포트하려면 다음을 사용합니다.

```scala
import A.given
```

일반 임포트와 given 임포트를 결합하려면 다음을 사용합니다.

```scala
import A.{given, *}
```

#### 5.3 타입에 의한 임포트 (Importing By Type)

와일드카드 임포트에만 의존하는 대신, 타입 선택자(by-type selector)를 사용하여 어떤 given 타입을 임포트할지 명시할 수 있습니다.

```scala
import A.given TC
```

이것은 `A` 안에서 타입 `TC`에 부합하는 모든 given을 임포트합니다. 여러 타입을 지정할 수도 있습니다.

```scala
import A.{given T1, ..., given Tn}
```

매개변수화된 타입(parameterized type)의 경우, 와일드카드 인자(`?`)로 범위를 명확히 합니다.

```scala
import Instances.{given Ordering[?], given ExecutionContext}
```

타입 기반 임포트와 이름 기반 임포트는 함께 쓸 수 있으며, 이 경우 타입 기반 선택자는 절(clause)의 마지막에 위치합니다.

#### 5.4 마이그레이션 지원 (Migration Support)

명세는 Scala 2의 implicit 시스템을 고려한 전환(transition) 규칙을 포함합니다. 초기에는 `given` 선택자가 구식(old-style) implicit도 함께 임포트합니다. 버전 3.1에서 디프리케이션(deprecation) 경고가 도입되었고, 이후 릴리스에서 점진적으로 강제(enforcement)됩니다. 이를 통해 라이브러리의 점진적 마이그레이션이 가능합니다.

#### 5.5 구문 문법 (Syntax Grammar)

형식 명세는 `WildCardSelector`를 `'*'` 또는 `'given' [InfixType]`로 정의하여, 더 넓은 임포트 표현식 구조 안에서 이름 없는(unnamed) given 임포트와 타입 제약된(type-constrained) given 임포트를 모두 허용합니다.

---

### 6. 이름에 의한 컨텍스트 매개변수 (By-Name Context Parameters)

#### 6.1 개요

> 📘 **처음 배우는 분께 — "이름에 의한(by-name)" 매개변수란**
>
> 보통 매개변수는 함수를 **부르는 순간** 값을 계산해서 넘깁니다. 반면 `x: => T`처럼 `=>`를 붙인 **by-name 매개변수**는 값을 미리 계산하지 않고, **함수 안에서 실제로 `x`를 쓰는 순간**에야 계산합니다(지연 평가).
>
> 핵심은 "안 쓰면 아예 계산되지 않는다"는 점입니다. 이걸 given에도 적용한 것이 이 절의 주제입니다.

이름에 의한 컨텍스트 매개변수(by-name context parameter)는 컨텍스트 인자를 지연 평가(lazily evaluated)하여 발산하는(divergent) 추론 루프를 방지합니다.

> 💡 **왜 필요한가 — 무한 루프 방지**
>
> `Codec[Option[Option[Int]]]`처럼 타입이 자기 자신을 품고 도는 경우, 컴파일러가 "이걸 만들려면 저게 필요하고, 저걸 만들려면 또…"를 끝없이 펼쳐 멈추지 못할 수 있습니다(이것이 **발산, divergence**).
>
> by-name(`ev: => Codec[T]`)으로 받으면 "당장 필요할 때만 계산"하므로, 실제로 안 쓰는 가지는 펼치지 않아 이 무한 전개를 끊을 수 있습니다. 문서가 설명하듯이: "컨텍스트 매개변수는 발산하는 추론 확장(divergent inferred expansion)을 피하기 위해 이름에 의한(by-name) 방식으로 선언될 수 있습니다."

#### 6.2 핵심 메커니즘

이 기능은 `(ev: => Codec[T])` 같은 구문으로 이름에 의한 컨텍스트 매개변수를 선언합니다. 인자가 실제로 필요해질 때까지 평가를 지연시키며, Scala의 일반 by-name 매개변수와 동일한 방식으로 동작합니다.

#### 6.3 예시 구조

참고 문서는 `Codec` 트레이트(trait) 예제를 제공하는데, 그 구조는 다음과 같습니다.

- `intCodec` 주어진 인스턴스가 `Codec[Int]`를 처리합니다.
- `optionCodec` 주어진 인스턴스가 이름에 의한 매개변수 `ev: => Codec[T]`를 받아 `Codec[Option[T]]`를 처리합니다.
- `None`이 매칭될 때, 내부 타입에 대한 코덱은 불필요하게 평가되지 않습니다.

이러한 재귀적 정의(`Codec[Option[Option[Int]]]` 같은 경우)에서, 이름에 의한 매개변수를 사용하지 않으면 컴파일러가 무한히 펼쳐지는 발산을 일으킬 수 있습니다. 지연 평가를 통해 이를 방지합니다.

#### 6.4 합성 알고리즘 (Synthesis Algorithm)

컴파일러는 다음 단계를 통해 이름에 의한 컨텍스트 매개변수의 인자를 합성합니다.

1. 필요한 타입의 새(fresh) given을 생성합니다.
2. 초기 후보 검색(initial candidate search) 중에는 해당 given을 사용 불가능 상태로 유지합니다(루프 방지).
3. 다른 by-name 매개변수에 대한 중첩 검색(nested search)에서는 사용 가능 상태로 전환합니다.
4. 합성된 표현식이 이 지역(local) given을 참조하면 `{ given lv: T = E; lv }` 형태로 감쌉니다.
5. 그렇지 않으면 표현식을 변경 없이 반환합니다.

#### 6.5 실질적 이점 (Practical Benefit)

> 합성된 컨텍스트 매개변수의 인자는, 그렇게 하지 않으면 발산하는 확장을 방지하기 위해 필요한 경우 지역 `val`로 뒷받침됩니다.

이 방식 덕분에 `Option[Option[Int]]` 같은 재귀적 제네릭 타입이 무한 루프 없이 올바르게 해소됩니다.

---

### 7. Scala 2 implicit와의 관계

Scala 3 참고 문서는 새로운 컨텍스트 추상화 기능이 Scala 2의 implicit 시스템과 어떻게 대응되는지 설명합니다.

> 💡 **왜 필요한가 — 이 절을 읽는 법**
>
> 지금까지 본 `given`·`using`·`Conversion`은 전부 Scala 2의 `implicit` 한 단어가 하던 일을 나눠 가진 것입니다. 이 절은 그 대응 관계를 정리한 변환표라고 보면 됩니다.
>
> Scala 3는 호환을 위해 옛 `implicit`과 새 `given`/`using`을 **둘 다** 지원합니다(7.3절). 그래서 옛 코드를 당장 안 고쳐도 돌아가며, 점진적으로 옮길 수 있습니다. 처음 배우는 분이라면 이 절은 "옛 코드를 만났을 때 돌아와 볼 표" 정도로 넘겨도 좋습니다.

#### 7.1 Scala 3 → Scala 2 매핑

**주어진 인스턴스 (Given Instances)**
implicit 객체(object) 또는 implicit 메서드를 가진 클래스로 변환됩니다. 단순한 given 인스턴스는 implicit 객체가 되고, 매개변수화된 givens는 클래스/implicit 메서드 조합이 됩니다.

**using 절 (Using Clauses)**
암시적 매개변수 절(implicit parameter clause)에 대응합니다. 핵심 차이는, 명시적 인자를 전달할 때 일반 적용(application) 구문이 아닌 `(using ...)` 구문을 사용해야 한다는 점입니다.

**컨텍스트 바운드 (Context Bounds)**
두 버전에서 동일하게 작동하며, 각자의 암시적 매개변수 형태로 확장됩니다.

**확장 메서드 (Extension Methods)**
직접적인 Scala 2 대응물은 없지만, 암시적 클래스(implicit class)를 통해 시뮬레이션할 수 있습니다.

**타입 클래스 도출(Type Class Derivation), 컨텍스트 함수 타입(Context Function Types), 이름에 의한 암시적 매개변수(Implicit By-Name Parameters)**
Scala 2 대응물이 없습니다.

#### 7.2 Scala 2 → Scala 3 매핑

**암시적 변환 (Implicit Conversions)**
`scala.Conversion`의 주어진 인스턴스로 표현될 수 있습니다. 예를 들어, 암시적 변환 함수는 given Conversion 인스턴스가 됩니다.

**암시적 클래스 (Implicit Classes)**
확장 메서드(선호됨) 또는 클래스/given Conversion 쌍으로 매핑됩니다.

**암시적 값 (Implicit Values)**
일반 `val`과 별칭 given(alias given)의 쌍으로 표현될 수 있습니다.

**추상 implicit (Abstract Implicits)**
일반 추상 정의(abstract definition)와 별칭 given으로 표현됩니다.

#### 7.3 호환성 정책

문서는 Scala 3가 호환성을 위해 구식 implicit과 새로운 given/using 두 시스템을 모두 지원한다고 명시합니다. 향후 버전에서 구식 implicit은 디프리케이션될 수 있으며, 자동 재작성(automatic rewriting) 도구를 통해 마이그레이션을 지원합니다.

---

### 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Contextual Abstractions](https://docs.scala-lang.org/scala3/reference/contextual/)

---

## Scala 3 컨텍스트 추상화: 확장 메서드와 타입 클래스

---

### 목차

1. [확장 메서드(Extension Methods)](#1-확장-메서드extension-methods)
   - 1.1. [개요](#11-개요)
   - 1.2. [확장 메서드의 변환(Translation)](#12-확장-메서드의-변환translation)
   - 1.3. [연산자 확장(Operator Extensions)](#13-연산자-확장operator-extensions)
   - 1.4. [제네릭 확장(Generic Extensions)](#14-제네릭-확장generic-extensions)
   - 1.5. [집합 확장(Collective Extensions)](#15-집합-확장collective-extensions)
   - 1.6. [확장 메서드 해석(Resolution)](#16-확장-메서드-해석resolution)
   - 1.7. [호출 해석 규칙](#17-호출-해석-규칙)
   - 1.8. [확장 내부에서의 참조](#18-확장-내부에서의-참조)
   - 1.9. [문법(Syntax)](#19-문법syntax)
2. [타입 클래스(Type Classes)](#2-타입-클래스type-classes)
   - 2.1. [개요](#21-개요)
   - 2.2. [세미그룹(Semigroup)과 모노이드(Monoid)](#22-세미그룹semigroup과-모노이드monoid)
   - 2.3. [펑터(Functor)](#23-펑터functor)
   - 2.4. [모나드(Monad)](#24-모나드monad)
   - 2.5. [요약](#25-요약)
3. [타입 클래스 파생(Type Class Derivation)](#3-타입-클래스-파생type-class-derivation)
   - 3.1. [개요](#31-개요)
   - 3.2. [정확한 동작 메커니즘](#32-정확한-동작-메커니즘)
   - 3.3. [Mirror 타입 클래스](#33-mirror-타입-클래스)
   - 3.4. [derived 메서드 구현](#34-derived-메서드-구현)
   - 3.5. [문법](#35-문법)
   - 3.6. [핵심 설계 원칙](#36-핵심-설계-원칙)
4. [다중우주 동등성(Multiversal Equality)](#4-다중우주-동등성multiversal-equality)
   - 4.1. [개요](#41-개요)
   - 4.2. [핵심 문제](#42-핵심-문제)
   - 4.3. [CanEqual 해법](#43-canequal-해법)
   - 4.4. [여러 CanEqual 인스턴스](#44-여러-canequal-인스턴스)
   - 4.5. [하위 호환성](#45-하위-호환성)
   - 4.6. [CanEqual 인스턴스 파생](#46-canequal-인스턴스-파생)
   - 4.7. [동등성 검사 규칙](#47-동등성-검사-규칙)
   - 4.8. [사전 정의된 인스턴스](#48-사전-정의된-인스턴스)
   - 4.9. [왜 타입 파라미터가 두 개인가](#49-왜-타입-파라미터가-두-개인가)
5. [컨텍스트 함수(Context Functions)](#5-컨텍스트-함수context-functions)
   - 5.1. [개요](#51-개요)
   - 5.2. [핵심 문법](#52-핵심-문법)
   - 5.3. [자동 변환](#53-자동-변환)
   - 5.4. [예제: 빌더 패턴](#54-예제-빌더-패턴)
   - 5.5. [예제: 사후 조건(Postconditions)](#55-예제-사후-조건postconditions)
   - 5.6. [핵심 구분점](#56-핵심-구분점)
6. [암시적 변환(Implicit Conversions)](#6-암시적-변환implicit-conversions)
   - 6.1. [핵심 개념](#61-핵심-개념)
   - 6.2. [정의 예제](#62-정의-예제)
   - 6.3. [변환이 적용되는 시점](#63-변환이-적용되는-시점)
   - 6.4. [실전 예제](#64-실전-예제)

---

### 1. 확장 메서드(Extension Methods)

#### 1.1. 개요

> 📘 **처음 배우는 분께 — 확장 메서드란**
>
> "이미 존재하는 타입에 내가 메서드를 덧붙이는 것"이라고 생각하면 됩니다. 예를 들어 `Int`는 표준 라이브러리에 들어 있어서 내가 고칠 수 없는데, `3.squared` 같은 메서드를 마치 원래 있었던 것처럼 쓰고 싶을 때가 있습니다. 확장 메서드가 바로 그걸 가능하게 해 줍니다. Scala 2에서 `implicit class`로 하던 일(00번 문서 9-(3))을 Scala 3에서는 `extension` 키워드로 더 직접적으로 합니다.

확장 메서드(extension method)는 어떤 타입이 최초로 정의된 이후에 그 타입에 새로운 메서드를 추가할 수 있게 해 줍니다. 즉, 타입의 원본 소스 코드를 수정하지 않고도 메서드를 덧붙일 수 있습니다. 공식 문서의 기초 예제는 다음과 같습니다.

```scala
case class Circle(x: Double, y: Double, radius: Double)

extension (c: Circle)
  def circumference: Double = c.radius * math.Pi * 2
```

이렇게 정의한 메서드는 일반적인 점(`.`) 표기법으로 호출할 수 있습니다.

```scala
circle.circumference
```

> 💡 **왜 필요한가**
>
> 내가 만들지 않은 타입(표준 라이브러리의 `Int`, `String`, 남의 라이브러리 클래스 등)은 소스를 고칠 수 없습니다. 그렇다고 `circumference(circle)`처럼 함수를 따로 부르면 점 표기법의 자연스러운 흐름(`circle.circumference`)이 깨집니다. 확장 메서드는 "원본을 안 건드리면서도 그 타입에 원래 있던 메서드처럼" 쓰게 해 주어, 라이브러리를 내 입맛에 맞게 확장하기 좋습니다.

#### 1.2. 확장 메서드의 변환(Translation)

> 📘 **처음 배우는 분께 — 리시버(receiver)와 변환**
>
> "리시버"는 점 앞에 오는 값, 즉 메서드를 받는 대상입니다(`circle.circumference`에서 `circle`). 확장 메서드는 내부적으로 이 리시버를 그냥 "첫 번째 인자"로 받는 보통 함수로 바뀌어, `circle.circumference`와 `circumference(circle)`이 같은 뜻이 됩니다. 점 표기법은 사람이 읽기 좋으라고 입힌 겉모습일 뿐이고, 이렇게 편한 문법을 기본 형태로 풀어내는 것을 "변환(translation)" 또는 디슈가링이라 합니다(00번 문서 11번).

컴파일러는 확장 메서드를, 확장 대상 값(receiver)을 첫 번째 파라미터로 받는 특수한 함수로 변환합니다. 위에서 정의한 `circumference`는 다음과 동등한 형태로 변환됩니다.

```scala
<extension> def circumference(c: Circle): Double = c.radius * math.Pi * 2
```

즉 `circle.circumference`와 `circumference(circle)`는 동일한 결과를 낳습니다.

#### 1.3. 연산자 확장(Operator Extensions)

확장 메서드 문법은 연산자 정의도 지원합니다.

```scala
extension (x: String)
  def < (y: String): Boolean = ...
extension (x: Elem)
  def +: (xs: Seq[Elem]): Seq[Elem] = ...
extension (x: Number)
  infix def min (y: Number): Number = ...
```

`+:`와 같은 우결합(right-associative) 연산자는, 기대되는 의미(semantics)를 유지하기 위해 변환 과정에서 파라미터 순서가 재배치(reordering)됩니다.

#### 1.4. 제네릭 확장(Generic Extensions)

확장에 타입 파라미터(type parameter)를 추가할 수 있습니다.

```scala
extension [T](xs: List[T])
  def second = xs.tail.head

extension [T: Numeric](x: T)
  def + (y: T): T = summon[Numeric[T]].plus(x, y)
```

메서드 수준의 타입 파라미터는 확장 수준의 타입 파라미터와 결합됩니다.

```scala
extension [T](xs: List[T])
  def sumBy[U: Numeric](f: T => U): U = ...
```

확장 파라미터에 대한 타입 인수(type argument)는 확장 문법이 아닌 일반 호출 문법을 사용해 지정해야 합니다.

```scala
sumBy[String](List("a", "bb", "ccc"))(_.length)
```

또한 확장은 컨텍스트 파라미터(contextual parameter)를 위한 `using` 절도 지원합니다.

#### 1.5. 집합 확장(Collective Extensions)

> 📘 **처음 배우는 분께 — 집합 확장**
>
> 같은 타입에 메서드를 여러 개 붙이고 싶을 때, `extension (...)`을 매번 반복하지 않고 한 번만 쓴 뒤 그 아래에 `def`들을 죽 나열하는 방식입니다. "이 타입에 대한 확장 메서드 모음" 하나를 만든다고 보면 됩니다. 묶음 안의 메서드들은 같은 리시버를 공유하므로, 서로를 `xs.someMethod` 없이 그냥 `someMethod`로 부를 수 있습니다.

동일한 리시버(receiver) 타입을 공유하는 여러 확장 메서드를 하나로 묶을 수 있습니다.

```scala
extension (ss: Seq[String])

  def longestStrings: Seq[String] =
    val maxLength = ss.map(_.length).max
    ss.filter(_.length == maxLength)

  def longestString: String = longestStrings.head
```

명시적으로 중괄호를 사용할 수도 있습니다.

```scala
extension (ss: Seq[String]) {

  def longestStrings: Seq[String] = {
    val maxLength = ss.map(_.length).max
    ss.filter(_.length == maxLength)
  }

  def longestString: String = longestStrings.head
}
```

집합 확장 내부에서 메서드들은 공유된 리시버를 통해 서로를 암시적으로 참조할 수 있습니다.

집합 확장은 타입 파라미터와 `using` 절도 지원합니다.

```scala
extension [T](xs: List[T])(using Ordering[T])
  def smallest(n: Int): List[T] = xs.sorted.take(n)
  def smallestIndices(n: Int): List[Int] =
    val limit = smallest(n).max
    xs.zipWithIndex.collect { case (x, i) if x <= limit => i }
```

#### 1.6. 확장 메서드 해석(Resolution)

> 📘 **처음 배우는 분께 — "해석(resolution)"과 암시적 스코프(implicit scope)**
>
> `circle.circumference`라고 썼을 때, 컴파일러는 "이 확장 메서드를 어디서 찾아야 하는가?"를 판단해야 합니다. 이렇게 "쓸 수 있는 후보를 찾아 연결하는" 과정을 해석(resolution)이라 하고, 아래 네 가지는 그 탐색 위치 목록입니다.
>
> 이 중 핵심 개념이 **암시적 스코프**입니다. 보통은 어떤 이름을 쓰려면 현재 스코프에 보이거나 `import` 되어 있어야 하는데, 컴파일러는 그 외에 "관련된 타입의 컴패니언 객체"도 자동으로 들여다봅니다. 예컨대 `List` 값을 다룰 때는 `import` 없이도 `object List` 안을 뒤져 줍니다. 이렇게 "타입과 관련돼 자동으로 함께 검색되는 영역"이 암시적 스코프이며, 덕분에 타입 정의 옆에 둔 확장/given을 따로 import하지 않아도 됩니다.

컴파일러는 다음 네 가지 메커니즘을 통해 확장 메서드가 적용 가능한지를 인식합니다.

1. **스코프 내에서 가시적(Visible in scope)** — 둘러싼 스코프 안에 정의되거나, 상속되거나, 임포트된 경우
2. **주어진 인스턴스의 멤버(Given instance members)** — 가시적인 주어진 인스턴스(given instance) 안에 포함된 경우
3. **리시버의 암시적 스코프(Implicit scope of receiver)** — 리시버 타입의 암시적 스코프(implicit scope)에 정의된 경우
4. **암시적 스코프 내 주어진 인스턴스(Given in implicit scope)** — 리시버의 암시적 스코프에 있는 주어진 인스턴스 안에 포함된 경우

스코프 기반 가용성을 보여 주는 예제입니다.

```scala
trait IntOps:
  extension (i: Int) def isZero: Boolean = i == 0

  extension (i: Int) def safeMod(x: Int): Option[Int] =
    if x.isZero then None
    else Some(i % x)

object IntOpsEx extends IntOps:
  extension (i: Int) def safeDiv(x: Int): Option[Int] =
    if x.isZero then None
    else Some(i / x)

trait SafeDiv:
  import IntOpsEx.*

  extension (i: Int) def divide(d: Int): Option[(Int, Int)] =
    (i.safeDiv(d), i.safeMod(d)) match
      case (Some(d), Some(r)) => Some((d, r))
      case _ => None
```

확장 메서드는 주어진 인스턴스를 통해 가용해집니다.

```scala
given ops1: IntOps()
1.safeMod(2)
```

암시적 스코프를 활용하는 예제입니다.

```scala
class List[T]:
  ...
object List:
  ...
  extension [T](xs: List[List[T]])
    def flatten: List[T] = xs.foldLeft(List.empty[T])(_ ++ _)

  given [T: Ordering] => Ordering[List[T]]:
    extension (xs: List[T])
      def < (ys: List[T]): Boolean = ...
end List

List(List(1, 2), List(3, 4)).flatten
List(1, 2) < List(3)
```

#### 1.7. 호출 해석 규칙

컴파일러가 `m`이 직접적인 멤버가 아닌 선택 표현식 `e.m[Ts]`를 만나면, 두 가지 재작성(rewriting) 전략을 순서대로 시도합니다.

**첫 번째**로, 완화된 임포트 규칙과 함께 표준 이름 해석(name resolution)을 적용해 `m[Ts](e)`로 재작성합니다. 모호한 임포트가 있을 때는, 오직 하나만 유효한 타입 검사를 통과하는 경우 비(非)와일드카드 임포트(non-wildcard import)를 우선합니다.

**두 번째**로, 첫 번째 접근이 실패하면 적격한(eligible) 객체들(리시버 타입의 암시적 스코프에 있거나 가시적인 주어진 인스턴스인 객체) 안에서 확장 메서드 `m`을 탐색하고, `o.m[Ts](e)`로 재작성합니다.

#### 1.8. 확장 내부에서의 참조

확장 메서드가 같은 집합 확장 내의 다른 확장 메서드를 참조할 때, 리시버는 암시적으로 전달됩니다. 재귀 호출의 경우를 봅시다.

```scala
extension (s: String)
  def position(ch: Char, n: Int): Int =
    if n < s.length && s(n) != ch then position(ch, n + 1)
    else n
```

재귀 호출 `position(ch, n + 1)`는 `s.position(ch, n + 1)`로 확장되며, 메서드 전체는 다음과 같이 변환됩니다.

```scala
def position(s: String)(ch: Char, n: Int): Int =
  if n < s.length && s(n) != ch then position(s)(ch, n + 1)
  else n
```

#### 1.9. 문법(Syntax)

확장 선언은 다음 패턴을 따릅니다.

```
Extension  ::=  'extension' [DefTypeParamClause] {UsingParamClause}
                '(' DefParam ')' {UsingParamClause} ExtMethods
ExtMethods ::=  ExtMethod | [nl] '{' ExtMethod {semi ExtMethod} '}' 
                | indent ExtMethod {semi ExtMethod} outdent
ExtMethod  ::=  {Annotation [nl]} {Modifier} 'def' DefDef
```

`extension` 키워드는 문맥 민감(context-sensitive)합니다. 즉, 문장(statement)을 시작하면서 그 뒤에 `[` 또는 `(`가 따라올 때에만 키워드로 인식됩니다.

---

### 2. 타입 클래스(Type Classes)

#### 2.1. 개요

> 📘 **처음 배우는 분께 — 타입 클래스란 (큰 그림)**
>
> 타입 클래스는 "상속을 쓰지 않고, 이미 있는 타입에 나중에 기능을 부여하는 방법"입니다. (00번 문서 10번이 직접 배경입니다.) 보통은 어떤 타입에 능력을 주려면 그 타입이 인터페이스를 `extends` 하게 만들어야 하는데, 남이 만든 타입(`Int`, `String` 등)은 그렇게 고칠 수가 없습니다. 타입 클래스는 대신 "능력을 담은 별도의 객체"를 바깥에서 만들어 두고, 필요할 때 컴파일러가 그걸 찾아 끼워 줍니다.
>
> 큰 그림 3단계: ① `trait Show[A]`처럼 "이런 능력"을 정의하고, ② `given Show[Int]`처럼 특정 타입에 대한 구현을 제공하고, ③ 함수가 `(using Show[A])`로 "그 능력이 있는 타입만 받겠다"고 요구합니다.

> ⚠️ **짚고 넘어가기 — "닫힌(closed) 데이터 타입"**
>
> 여기서 "닫힌 데이터 타입"이란 enum이나 `sealed trait`처럼 "더 이상 케이스를 추가할 수 없게 봉인된 타입"이 아니라, 단순히 "내가 손댈 수 없는, 이미 완성되어 닫혀 있는 타입(예: 표준 라이브러리 타입)"을 가리키는 뜻으로 읽으면 됩니다. 핵심은 "그 타입의 소스를 못 고쳐도 기능을 더할 수 있다"는 것입니다.

타입 클래스(type class)는 추상적이고 매개변수화된(parameterized) 타입으로, 닫힌(closed) 데이터 타입에 서브타이핑(subtyping) 없이 새로운 동작을 추가할 수 있게 합니다. Scala 3에서 타입 클래스는 타입 파라미터를 가진 트레이트(trait)이며, 구현은 `extends` 키워드 대신 **주어진 인스턴스**(given instance)로 제공합니다.

#### 2.2. 세미그룹(Semigroup)과 모노이드(Monoid)

> 📘 **처음 배우는 분께 — 세미그룹과 모노이드 (겁먹지 마세요)**
>
> 수학 용어라 어렵게 들리지만 뜻은 단순합니다. **세미그룹**은 "같은 타입 두 개를 합쳐 하나로 만드는 `combine` 연산을 가진 것"입니다 (문자열 이어붙이기, 숫자 더하기 등). **모노이드**는 거기에 "아무것도 아닌 기본값 `unit`"이 하나 더 있는 것입니다 (문자열의 빈 문자열 `""`, 덧셈의 `0`). `unit`은 무엇과 합쳐도 상대를 그대로 두는 "항등원"입니다. 이것들은 타입 클래스의 *예시*일 뿐이니, 정의 자체보다 "이게 타입 클래스로 어떻게 표현되나"에 집중하세요.

기초적인 타입 클래스 정의는 다음과 같습니다.

```scala
trait SemiGroup[T]:
  extension (x: T) def combine (y: T): T

trait Monoid[T] extends SemiGroup[T]:
  def unit: T
```

`String`에 대한 구현입니다.

```scala
given Monoid[String]:
  extension (x: String) def combine (y: String): String = x.concat(y)
  def unit: String = ""
```

`Int`에 대한 구현입니다.

```scala
given Monoid[Int]:
  extension (x: Int) def combine (y: Int): Int = x + y
  def unit: Int = 0
```

컨텍스트 바운드(context bound)와 함께 사용하는 예입니다.

```scala
def combineAll[T: Monoid as m](xs: List[T]): T =
  xs.foldLeft(m.unit)(_.combine(_))
```

여기서 `[T: Monoid as m]`는 컨텍스트 바운드를 통해 도입된 `Monoid[T]` 인스턴스에 `m`이라는 이름을 부여하는 Scala 3의 문법입니다.

> 📘 **처음 배우는 분께 — 컨텍스트 바운드 `[T: Monoid]`**
>
> `[T: Monoid]`는 "`T`라는 타입을 받되, 그 `T`에 대한 `Monoid[T]` 인스턴스(능력)가 존재해야 한다"는 요구를 짧게 쓴 것입니다. 즉 `(using Monoid[T])`를 타입 파라미터 옆에 붙여 줄인 약식 표기입니다. 다만 이렇게만 쓰면 그 인스턴스에 이름이 없어 본문에서 `m.unit`처럼 직접 못 부르는데, Scala 3의 `as m`이 거기에 `m`이라는 이름을 붙여 줍니다. (필요하면 02번 문서의 `using`/컨텍스트 바운드 설명을 참고하세요.)

#### 2.3. 펑터(Functor)

> 📘 **처음 배우는 분께 — 펑터와 `F[_]`**
>
> `F[_]`는 "`List`, `Option`처럼 안에 다른 타입 하나를 담는 그릇"을 뜻합니다 (00번 문서 7번의 고차 타입). 밑줄은 "여기에 타입이 하나 들어갈 자리가 있다"는 표시입니다. **펑터**는 한마디로 "그 그릇을 열지 않고도, 안에 든 값에 함수를 적용할 수 있는 것"입니다. `List(1,2,3).map(_ + 1)`이 바로 그것 — 리스트라는 그릇은 그대로 두고 안의 값만 바꿉니다. 펑터는 이 `map` 능력을 타입 클래스로 일반화한 것입니다.

펑터(Functor)는 값을 "맵핑(map over)"하여, 구조를 보존하면서 변환할 수 있는 능력을 제공합니다. 정의에는 타입 생성자(type constructor) `F[_]`가 사용됩니다.

```scala
trait Functor[F[_]]:
  extension [A](x: F[A])
    def map[B](f: A => B): F[B]
```

`List` 인스턴스는 다음과 같습니다.

```scala
given Functor[List]:
  extension [A](xs: List[A])
    def map[B](f: A => B): List[B] =
      xs.map(f)
```

테스트 메서드 사용 예입니다.

```scala
def assertTransformation[F[_]: Functor, A, B](expected: F[B], original: F[A], mapping: A => B): Unit =
  assert(expected == original.map(mapping))

assertTransformation(List("a1", "b1"), List("a", "b"), elt => s"${elt}1")
```

#### 2.4. 모나드(Monad)

> 📘 **처음 배우는 분께 — 모나드는 펑터의 확장판**
>
> 모나드는 펑터에서 한 발 더 나아간 것입니다. 펑터의 `map`은 "그릇 속 값을 보통 값으로 바꾼다"면, 모나드의 `flatMap`은 "그릇 속 값을 다시 그릇이 든 값으로 바꾼 뒤, 그릇이 이중으로 겹치지 않게 평평하게 펴 준다"는 차이가 있습니다 (`List[List[B]]`가 `List[B]`로). `pure`는 "보통 값 하나를 그릇에 담는" 가장 기본 동작입니다. 여기서도 "이런 능력들을 타입 클래스로 정의했다"는 점만 잡으면 충분합니다.

모나드(Monad)는 펑터를 확장하여 `flatMap`과 `pure` 연산을 추가합니다.

```scala
trait Monad[F[_]] extends Functor[F]:

  def pure[A](x: A): F[A]

  extension [A](x: F[A])
    def flatMap[B](f: A => F[B]): F[B]
    def map[B](f: A => B) = x.flatMap(f.andThen(pure))

end Monad
```

##### List 구현

```scala
given listMonad: Monad[List]:
  def pure[A](x: A): List[A] =
    List(x)
  extension [A](xs: List[A])
    def flatMap[B](f: A => List[B]): List[B] =
      xs.flatMap(f)
```

##### Option 구현

```scala
given optionMonad: Monad[Option]:
  def pure[A](x: A): Option[A] =
    Option(x)
  extension [A](xo: Option[A])
    def flatMap[B](f: A => Option[B]): Option[B] = xo match
      case Some(x) => f(x)
      case None => None
```

##### Reader 모나드

Reader 모나드는 함수에 대해 동작하며, 동일한 파라미터(설정(configuration), 컨텍스트, 환경(environment))를 필요로 하는 함수들을 결합할 때 유용합니다.

```scala
type ConfigDependent[Result] = Config => Result

given configDependentMonad: Monad[ConfigDependent]:

  def pure[A](x: A): ConfigDependent[A] =
    config => x

  extension [A](x: ConfigDependent[A])
    def flatMap[B](f: A => ConfigDependent[B]): ConfigDependent[B] =
      config => f(x(config))(config)

end configDependentMonad
```

타입 람다(type lambda)를 사용한 버전입니다.

```scala
given configDependentMonad: Monad[[Result] =>> Config => Result]:

  def pure[A](x: A): Config => A =
    config => x

  extension [A](x: Config => A)
    def flatMap[B](f: A => Config => B): Config => B =
      config => f(x(config))(config)

end configDependentMonad
```

임의의 컨텍스트 타입에 대해 매개변수화된 Reader 모나드입니다.

```scala
given readerMonad: [Ctx] => Monad[[X] =>> Ctx => X]:

  def pure[A](x: A): Ctx => A =
    ctx => x

  extension [A](x: Ctx => A)
    def flatMap[B](f: A => Ctx => B): Ctx => B =
      ctx => f(x(ctx))(ctx)

end readerMonad
```

#### 2.5. 요약

> 💡 **왜 필요한가 — 상속 대신 타입 클래스**
>
> 상속(서브타입 다형성)에서는 어떤 타입에 능력을 주려면 그 타입을 정의할 때 미리 `extends`로 인터페이스를 물려받게 해야 합니다. 즉 능력이 타입 정의에 박혀 버립니다. 그래서 남이 이미 만들어 둔 타입에는 능력을 추가할 수 없습니다. 타입 클래스는 그 능력을 타입 정의 *바깥*의 `given` 인스턴스로 떼어 놓기 때문에, 누가 만든 타입이든 어디서든 나중에 능력을 붙일 수 있습니다 — 이 "어디서나 정의 가능"이 핵심 장점입니다.

타입 클래스는 구현 전략 면에서 서브타입 다형성(subtype polymorphism)과 다릅니다. 전통적인 상속(inheritance)에서는 어떤 클래스에 새로운 인터페이스를 추가하려면 그 클래스의 소스 코드를 변경해야 하지만, 타입 클래스의 인스턴스는 어디서나 정의할 수 있어 더 우수한 확장성(extensibility)을 제공합니다. Scala 3는 트레이트, 주어진 인스턴스, 확장 메서드, 컨텍스트 바운드, 타입 람다를 결합해 타입 클래스를 자연스럽게 표현합니다.

---

### 3. 타입 클래스 파생(Type Class Derivation)

#### 3.1. 개요

> 📘 **처음 배우는 분께 — 타입 클래스 파생(derivation)이란**
>
> 타입 클래스를 쓰려면 타입마다 `given` 인스턴스를 손으로 적어 줘야 합니다. 그런데 `case class Point(x: Int, y: Int)`처럼 구조가 뻔한 타입은 "각 필드의 인스턴스를 조합하면 끝"이라 매번 쓰기 번거롭습니다. **파생**은 타입 정의에 `derives Eq`처럼 한 줄 붙이면, 컴파일러가 그 구조를 보고 인스턴스를 *자동으로* 만들어 주는 기능입니다. 즉 "given을 손으로 안 쓰고 컴파일러에게 짜 달라고 시키는 것"입니다.
>
> 3장은 그 자동 생성이 *어떻게* 동작하는지를 파헤치는데, 내용이 상당히 깊습니다. 처음에는 "`derives`를 붙이면 인스턴스가 공짜로 생긴다"는 결론만 가져가도 충분하고, Mirror·inline 같은 세부는 라이브러리를 직접 만들 때 돌아오면 됩니다.

타입 클래스 파생(type class derivation)은 특정 조건을 만족하는 타입 클래스에 대해 주어진 인스턴스를 자동으로 생성합니다. 여기서 타입 클래스란, 동작 대상 타입을 결정하는 단일 타입 파라미터를 가진 트레이트 또는 클래스를 가리킵니다.

예를 들어, `derives` 절(clause)을 가진 enum은 다음과 같습니다.

```scala
enum Tree[T] derives Eq, Ordering, Show:
  case Branch(left: Tree[T], right: Tree[T])
  case Leaf(elem: T)
```

이는 컴패니언 객체(companion object)에 다음 인스턴스들을 생성합니다.

```scala
given [T: Eq]       => Eq[Tree[T]]       = Eq.derived
given [T: Ordering] => Ordering[Tree[T]] = Ordering.derived
given [T: Show]     => Show[Tree[T]]     = Show.derived
```

#### 3.2. 정확한 동작 메커니즘

이 프레임워크는 타입 클래스 `TC`가 타입 인수를 어떻게 받는지에 따라 경우를 구분합니다.

> 📘 **처음 배우는 분께 — 카인드(kind)가 `*`**
>
> 본문의 "카인드(kind)가 `*`"라는 말은 `Int`처럼 그 자체로 완성된 타입을 뜻하고, `List`처럼 `List[Int]`가 되어야 완성되는 그릇 타입과 구분됩니다(00번 문서 7번 고차 타입).

##### 경우 1: TC가 파라미터 하나를 받는 경우

모든 인수의 카인드(kind)가 `*`일 때, 생성되는 인스턴스는 다음을 따릅니다.

```scala
given [T_1: TC, ..., T_N: TC] => TC[DerivingType[T_1, ..., T_N]] = TC.derived
```

단순한 케이스 클래스(case class)의 예입니다.

```scala
case class Point(x: Int, y: Int) derives Ordering
```

이는 다음을 생성합니다.

```scala
object Point:
  given Ordering[Point] = Ordering.derived
```

##### 경우 2: CanEqual 타입 클래스

`CanEqual`을 파생할 때, 프레임워크는 양쪽 측면에 "DerivingType의 모든 파라미터를 두 번씩" 갖는 인스턴스를 생성하며, 컨텍스트 바운드는 카인드가 `*`인 파라미터에 대해서만 적용됩니다.

##### 경우 3: 고차(Higher-Kinded) 파라미터

`F`와 `DerivingType`이 일치하는 카인드 파라미터를 가질 때, 생성되는 인스턴스는 타입 람다를 사용합니다.

#### 3.3. Mirror 타입 클래스

> 📘 **처음 배우는 분께 — Mirror란**
>
> 파생이 동작하려면 컴파일러가 "이 타입이 어떤 필드/케이스로 이루어져 있는지"를 알아야 합니다. `Mirror`는 바로 그 구조 정보를 담아 컴파일 시점에 컴파일러가 자동으로 만들어 주는 "타입의 설계도"입니다. 예를 들어 `case class Point(x: Int, y: Int)`라면 "필드가 둘이고 이름은 `x`,`y`, 타입은 `Int`,`Int`"라는 정보를, `sealed trait`라면 "어떤 케이스들이 있는지"를 알려 줍니다.
>
> 두 종류가 있습니다. **Product**는 곱 타입 — 여러 필드를 *동시에* 갖는 case class용(00번 문서 8번). **Sum**은 합 타입 — 여러 케이스 *중 하나*인 sealed/enum용입니다. 파생 코드는 이 설계도를 읽어 각 부품의 인스턴스를 모아 전체 인스턴스를 조립합니다.

`Mirror` 기반 구조(infrastructure)는 ADT(대수적 데이터 타입, Algebraic Data Type) 구조에 대한 컴파일 시점(compile-time) 타입 정보를 제공합니다.

```scala
sealed trait Mirror:
  type MirroredType
  type MirroredElemTypes
  type MirroredMonoType
  type MirroredLabel <: String
  type MirroredElemLabels <: Tuple

object Mirror:
  trait Product extends Mirror:
    def fromProduct(p: scala.Product): MirroredMonoType
  
  trait Sum extends Mirror:
    def ordinal(x: MirroredMonoType): Int
```

컴파일러는 다음에 대해 자동으로 `Mirror` 인스턴스를 생성합니다.

- enum 및 enum 케이스
- 케이스 객체(case object)
- 가시적인 생성자(visible constructor)를 가진 케이스 클래스
- 도달 가능한(reachable) 자식 케이스를 가진 봉인된(sealed) 클래스/트레이트

위의 `Tree` 예제에 대해 컴파일러는 다음을 제공합니다.

```scala
// Tree에 대한 Mirror (Sum 타입)
new Mirror.Sum:
  type MirroredType = Tree
  type MirroredElemTypes[T] = (Branch[T], Leaf[T])
  type MirroredMonoType = Tree[_]
  type MirroredLabel = "Tree"
  type MirroredElemLabels = ("Branch", "Leaf")
  def ordinal(x: MirroredMonoType): Int = x match
    case _: Branch[_] => 0
    case _: Leaf[_] => 1

// Branch에 대한 Mirror (Product 타입)
new Mirror.Product:
  type MirroredType = Branch
  type MirroredElemTypes[T] = (Tree[T], Tree[T])
  type MirroredMonoType = Branch[_]
  type MirroredLabel = "Branch"
  type MirroredElemLabels = ("left", "right")
  def fromProduct(p: Product): MirroredMonoType = new Branch(...)
```

#### 3.4. derived 메서드 구현

> 📘 **처음 배우는 분께 — `derived` 메서드와 `inline`**
>
> `derives Eq`라고 적으면 컴파일러는 실제로 `Eq.derived`를 호출합니다. 그러니 타입 클래스를 *만드는* 사람은 컴패니언 객체에 `derived` 메서드를 한 번 작성해 둬야 합니다 — 이게 "구조 설계도(Mirror)를 받아 인스턴스를 조립하는 공장"입니다. 타입 클래스를 *쓰기만* 하는 입장이라면 이 메서드를 직접 부를 일은 없습니다.
>
> `inline`은 "이 코드를 호출하는 게 아니라, 컴파일 시점에 호출 자리에 펼쳐 넣어라"는 지시입니다. 구조 정보가 컴파일 시점에만 있으므로, 파생 로직도 컴파일 시점에 펼쳐져야 동작합니다. (inline의 자세한 내용은 08·09번 메타프로그래밍 문서의 주제이니, 여기서는 "런타임이 아니라 컴파일 때 실행되는 코드"라는 감만 잡으면 됩니다.)

타입 클래스 작성자는 보통 `Mirror` 파라미터를 받는 인라인(inline) 메서드로 `derived`를 구현합니다.

```scala
import scala.deriving.Mirror

inline def derived[T](using Mirror.Of[T]): TC[T] = ...
```

##### 완전한 Eq 예제

인라인 메서드와 패턴 매칭(pattern matching)을 사용하는 완전한 저수준(low-level) 구현입니다.

```scala
import scala.collection.AbstractIterable
import scala.compiletime.{erasedValue, error, summonInline}
import scala.deriving.*

inline def summonInstances[T, Elems <: Tuple]: List[Eq[?]] =
  inline erasedValue[Elems] match
    case _: (elem *: elems) => deriveOrSummon[T, elem] :: summonInstances[T, elems]
    case _: EmptyTuple => Nil

inline def deriveOrSummon[T, Elem]: Eq[Elem] =
  inline erasedValue[Elem] match
    case _: T => deriveRec[T, Elem]
    case _    => summonInline[Eq[Elem]]

inline def deriveRec[T, Elem]: Eq[Elem] =
  inline erasedValue[T] match
    case _: Elem => error("infinite recursive derivation")
    case _       => Eq.derived[Elem](using summonInline[Mirror.Of[Elem]])

trait Eq[T]:
  def eqv(x: T, y: T): Boolean

object Eq:
  given Eq[Int]:
    def eqv(x: Int, y: Int) = x == y

  def check(x: Any, y: Any, elem: Eq[?]): Boolean =
    elem.asInstanceOf[Eq[Any]].eqv(x, y)

  def iterable[T](p: T): Iterable[Any] = new AbstractIterable[Any]:
    def iterator: Iterator[Any] = p.asInstanceOf[Product].productIterator

  def eqSum[T](s: Mirror.SumOf[T], elems: => List[Eq[?]]): Eq[T] =
    new Eq[T]:
      def eqv(x: T, y: T): Boolean =
        val ordx = s.ordinal(x)
        (s.ordinal(y) == ordx) && check(x, y, elems(ordx))

  def eqProduct[T](p: Mirror.ProductOf[T], elems: => List[Eq[?]]): Eq[T] =
    new Eq[T]:
      def eqv(x: T, y: T): Boolean =
        iterable(x).lazyZip(iterable(y)).lazyZip(elems).forall(check)

  inline def derived[T](using m: Mirror.Of[T]): Eq[T] =
    lazy val elemInstances = summonInstances[T, m.MirroredElemTypes]
    inline m match
      case s: Mirror.SumOf[T]     => eqSum(s, elemInstances)
      case p: Mirror.ProductOf[T] => eqProduct(p, elemInstances)
end Eq
```

재귀적 ADT로 테스트하는 예입니다.

```scala
enum Lst[+T] derives Eq:
  case Cns(t: T, ts: Lst[T])
  case Nl

@main def test(): Unit =
  import Lst.*
  val eqoi = summon[Eq[Lst[Int]]]
  assert(eqoi.eqv(23 :: 47 :: Nl, 23 :: 47 :: Nl))
  assert(!eqoi.eqv(23 :: Nl, 7 :: Nl))
```

#### 3.5. 문법

```
InheritClauses    ::=  ['extends' ConstrApps] ['derives' QualId {',' QualId}]
```

이제 `extends` 절에서도 여러 타입을 쉼표로 구분할 수 있습니다.

```scala
class A extends B, C { ... }
```

#### 3.6. 핵심 설계 원칙

이 프레임워크는 "미러링된 타입의 속성을 타입 멤버(type member)로 인코딩하는" 방식과 값 수준(value-level) 메커니즘이 함께 동작하는, 최소한의 저수준 설계를 지향합니다. 속성을 항(term)이 아닌 타입으로 인코딩해 바이트코드(bytecode) 크기를 작게 유지합니다. `Mirror` 기반 구조는 기존의 `Product` 기반 구조를 확장하며, 일반적으로 컴패니언 객체가 구현을 담당합니다.

상위 수준 라이브러리(Shapeless 3, Magnolia 스타일 등)는 더 정교한 파생 패턴을 위해 이 기반 위에 구축될 수 있습니다.

---

### 4. 다중우주 동등성(Multiversal Equality)

#### 4.1. 개요

> 📘 **처음 배우는 분께 — 왜 갑자기 동등성?**
>
> 이 절은 "타입 클래스를 실제로 활용한 예"로 읽으면 자연스럽습니다. 문제는 이것입니다 — 기존 Scala/Java에서는 `==`로 *아무거나* 비교할 수 있어서, `Int`와 `String`을 비교하는 실수도 컴파일이 통과하고 그냥 `false`가 나옵니다. **다중우주 동등성**은 "비교해도 되는 타입 쌍"을 `CanEqual`이라는 타입 클래스로 등록해 두고, 등록 안 된 조합은 컴파일 에러로 막는 옵트인 기능입니다. "옵트인(opt-in)"이란 기본은 꺼져 있고 내가 켜야 적용된다는 뜻입니다.

Scala 3는 동등성 비교(equality comparison) 시 타입 안전성을 강화하기 위한 옵트인(opt-in) 메커니즘으로 다중우주 동등성(multiversal equality)을 도입합니다. 임의의 두 값을 비교할 수 있게 하는 일반 동등성(universal equality)과 달리, 이 기능은 `CanEqual`이라는 이항(binary) 타입 클래스로 비교를 제한합니다.

#### 4.2. 핵심 문제

일반 동등성은 안전성 취약점을 만듭니다. 다음을 고려해 봅시다.

```scala
val x = ... // 타입 T
val y = ... // 타입 S 이지만, 사실 T 여야 함
x == y      // 타입 검사를 통과하며, 항상 false 를 반환함
```

타입이 일치하지 않아도 비교가 통과되므로, 런타임에서야 드러나는 버그를 감출 수 있습니다.

#### 4.3. CanEqual 해법

`CanEqual` 타입 클래스는 동등성을 의도된 타입 쌍으로만 제한합니다.

```scala
class T derives CanEqual
```

또는 명시적 인스턴스를 제공할 수도 있습니다.

```scala
given CanEqual[T, T] = CanEqual.derived
```

`CanEqual`의 정의는 다음과 같습니다.

```scala
@implicitNotFound("Values of types ${L} and ${R} cannot be compared with == or !=")
sealed trait CanEqual[-L, -R]

object CanEqual:
  object derived extends CanEqual[Any, Any]
```

#### 4.4. 여러 CanEqual 인스턴스

선택적인 비교를 허용하기 위해 여러 인스턴스를 정의할 수 있습니다.

```scala
given CanEqual[A, A] = CanEqual.derived
given CanEqual[B, B] = CanEqual.derived
given CanEqual[A, B] = CanEqual.derived
given CanEqual[B, A] = CanEqual.derived
```

#### 4.5. 하위 호환성

> ⚠️ **짚고 넘어가기 — 기본은 예전과 똑같다**
>
> 이 기능을 켜는 스위치가 `strictEquality`(엄격 모드)입니다. 이걸 켜지 않으면 `canEqualAny`라는 "아무거나 비교 허용" 폴백 인스턴스가 자동으로 적용되어, 기존 Scala 코드처럼 평소대로 `==`가 동작합니다. 즉 `import scala.language.strictEquality`를 직접 하기 전에는 컴파일 에러로 막히지 않습니다 — 이 절의 안전 검사는 "내가 켜야 비로소" 작동합니다.

"폴백(fallback)" 인스턴스인 `canEqualAny`는 `strictEquality`가 활성화되지 않는 한 하위 호환성을 보존합니다.

```scala
def canEqualAny[L, R]: CanEqual[L, R] = CanEqual.derived
```

엄격 모드(strict mode)는 다음을 통해 활성화합니다.

```scala
import scala.language.strictEquality
```

또는 명령줄 플래그 `-language:strictEquality`를 사용합니다.

#### 4.6. CanEqual 인스턴스 파생

타입 클래스 파생을 통해 인스턴스 생성을 단순화할 수 있습니다.

```scala
class Box[T](x: T) derives CanEqual
```

이는 다음을 생성합니다.

```scala
given [T, U] => CanEqual[T, U] => CanEqual[Box[T], Box[U]] =
  CanEqual.derived
```

동작을 보여 주는 예제입니다.

```scala
new Box(1) == new Box(1L)   // OK. CanEqual[Int, Long]이 존재함
new Box(1) == new Box("a")  // 오류: 비교할 수 없음
new Box(1) == 1             // 오류: 비교할 수 없음
```

#### 4.7. 동등성 검사 규칙

`strictEquality`가 비활성화된 상태에서, 비교는 다음 경우에 성공합니다.

1. 타입이 정확히 일치하거나,
2. 한 타입이 다른 타입의 리프팅(lifting)된 버전의 서브타입이거나,
3. 두 타입 모두 반사적(reflexive) `CanEqual` 인스턴스를 갖지 않을 때

핵심 정의입니다.

- **리프팅(Lifting)**: 공변(covariant) 위치의 추상 타입을 상한(upper bound)으로 치환하는 것
- **반사적 인스턴스(Reflexive instance)**: `CanEqual[T, T]`에 대한 암시적 탐색(implicit search)이 성공하는 경우

#### 4.8. 사전 정의된 인스턴스

`CanEqual` 객체는 다음 타입에 대해 인스턴스를 정의합니다.

- 원시 타입(`Byte`, `Short`, `Char`, `Int`, `Long`, `Float`, `Double`, `Boolean`, `Unit`)
- 자바 타입(`Number`, `Boolean`, `Character`)
- 컬렉션(`Seq`, `Set`)

사전 정의된 비교의 규칙입니다.

- 모든 원시 수치(numeric) 타입은 서로 비교 가능
- 원시 수치 타입은 `java.lang.Number`의 서브타입과 비교 가능
- `Boolean`은 `java.lang.Boolean`과 비교 가능
- `Char`는 `java.lang.Character`와 비교 가능
- 시퀀스는 원소 타입이 비교 가능하면 비교 가능
- 집합(Set)은 원소 타입이 비교 가능하면 비교 가능
- 임의의 `AnyRef` 서브타입은 `Null`과 비교 가능

#### 4.9. 왜 타입 파라미터가 두 개인가

두 파라미터 설계는 변성(variance) 안전성을 갖춘 API 설계를 가능하게 합니다. 예를 들어 다음과 같이 더 안전한 `contains` 메서드를 정의할 수 있습니다.

```scala
def contains[U >: T](x: U)(using CanEqual[T, U]): Boolean
```

이는 하위 호환성을 유지하면서도 비교를 제한합니다. 단일 파라미터 설계였다면 안전성 이점을 훼손하는 일반(universal) 인스턴스가 필요해져, 생태계의 점진적 마이그레이션이 불가능해집니다.

---

### 5. 컨텍스트 함수(Context Functions)

#### 5.1. 개요

> 📘 **처음 배우는 분께 — 컨텍스트 함수와 `?=>`**
>
> 보통 함수 타입은 `A => B`("A를 받아 B를 돌려줌")로 씁니다. **컨텍스트 함수**는 화살표가 `?=>`인 함수로, 그 인자를 *내가 직접 넘기는 게 아니라 컴파일러가 `using`/`given`으로 자동으로 채워 주는* 함수입니다. 즉 `A ?=> B`는 "A를 컨텍스트(암시적 인자)로 받아 B를 돌려주는 함수"입니다. 02번 문서의 `using`/`given`을 함수 값으로 만든 버전이라고 보면 됩니다 — 덕분에 `using` 인자를 일일이 손으로 넘기지 않는 DSL을 만들 수 있습니다.

컨텍스트 함수(context function)는 컨텍스트 파라미터(context parameter)만을 인자로 받는 함수로, `?=>` 화살표 문법으로 구분됩니다. 그 타입을 _컨텍스트 함수 타입(context function type)_ 이라 합니다.

#### 5.2. 핵심 문법

컨텍스트 함수 타입은 다음과 같이 선언합니다.

```scala
type Executable[T] = ExecutionContext ?=> T
```

이 패턴을 사용하는 함수는 컴파일러가 합성(synthesize)한 인수와 함께 호출됩니다.

```scala
def f(x: Int): ExecutionContext ?=> Int = ...

f(2)(using ec)   // 명시적 인수
f(2)             // 인수가 추론됨
```

#### 5.3. 자동 변환

어떤 표현식의 기대 타입(expected type)이 컨텍스트 함수 타입 `(T_1, ..., T_n) ?=> U`이고 그 표현식이 아직 컨텍스트 함수 리터럴(literal)이 아닌 경우, 자동으로 다음으로 변환됩니다.

```scala
(x_1: T1, ..., x_n: Tn) ?=> E
```

합성된 파라미터들은 타입 검사 중에 주어진 인스턴스(given)로 사용할 수 있게 됩니다.

#### 5.4. 예제: 빌더 패턴

> 💡 **왜 필요한가 — 빌더 DSL에서**
>
> 아래 예제에서 `table { row { cell("A") } }`처럼 중첩해서 쓸 때, 안쪽 `cell`은 자기가 어느 `Row`에 속하는지, `row`는 어느 `Table`에 속하는지 알아야 합니다. 이걸 인자로 일일이 넘기면(`cell("A", currentRow)`) 코드가 지저분해집니다. 컨텍스트 함수는 바깥 블록이 만든 `Table`/`Row`를 `given`으로 깔아 두고 안쪽이 자동으로 집어 쓰게 하므로, 사용자는 그런 배선을 신경 쓰지 않고 구조만 적으면 됩니다.

컨텍스트 함수를 사용하면 컨텍스트 객체를 메서드 호출마다 명시적으로 전달하지 않고도 중첩(nested) 구조를 구성하는 깔끔한 DSL 스타일 문법을 작성할 수 있습니다.

```scala
import scala.collection.mutable.ArrayBuffer

class Table:
  val rows = new ArrayBuffer[Row]
  def add(r: Row): Unit = rows += r
  override def toString = rows.mkString("Table(", ", ", ")")

class Row:
  val cells = new ArrayBuffer[Cell]
  def add(c: Cell): Unit = cells += c
  override def toString = cells.mkString("Row(", ", ", ")")

case class Cell(elem: String)

def table(init: Table ?=> Unit) =
  given t: Table = Table()
  init
  t

def row(init: Row ?=> Unit)(using t: Table) =
  given r: Row = Row()
  init
  t.add(r)

def cell(str: String)(using r: Row) =
  r.add(new Cell(str))
```

여기서 `table`, `row`, `cell`은 각각 컨텍스트 함수를 받아, 현재의 `Table` 또는 `Row`를 주어진 인스턴스로 제공합니다. 이를 통해 사용자는 `using` 인수를 명시적으로 넘기지 않고도 중첩된 빌더 블록을 작성할 수 있습니다.

#### 5.5. 예제: 사후 조건(Postconditions)

불투명 타입 별칭(opaque type alias)을 컨텍스트 함수와 함께 사용하면 단언(assertion) 검사를 위한 무비용(zero-overhead) 추상화를 만들 수 있습니다. 결과 값을 조건식 안에서 박싱(boxing) 부담 없이 참조할 수 있습니다.

```scala
object PostConditions:
  opaque type WrappedResult[T] = T

  def result[T](using r: WrappedResult[T]): T = r

  extension [T](x: T)
    def ensuring(condition: WrappedResult[T] ?=> Boolean): T =
      assert(condition(using x))
      x
end PostConditions

import PostConditions.{ensuring, result}

val s = List(1, 2, 3).sum.ensuring(result == 6)
```

여기서 `ensuring`은 `WrappedResult[T] ?=> Boolean` 타입의 컨텍스트 함수를 조건으로 받습니다. 호출 시 `x`(현재 결과 값)가 주어진 인스턴스로 전달되고, 조건식 안에서 `result`를 통해 그 값을 참조할 수 있습니다. `WrappedResult`가 불투명 타입이므로 런타임에 별도의 래퍼(wrapper) 객체가 생성되지 않아 오버헤드가 없습니다.

#### 5.6. 핵심 구분점

컨텍스트 함수 리터럴은 파라미터와 결과 사이에 `?=>`를 사용하며, 일반 함수 타입이 아닌 컨텍스트 함수 타입을 갖는다는 점에서 표준 함수 리터럴과 구분됩니다.

---

### 6. 암시적 변환(Implicit Conversions)

#### 6.1. 핵심 개념

> 📘 **처음 배우는 분께 — 암시적 변환이란**
>
> 어떤 타입 자리에 다른 타입의 값이 들어오면, 컴파일러가 등록된 변환 함수를 *자동으로* 끼워 넣어 타입을 바꿔 주는 기능입니다 (00번 문서 9-(2)). Scala 2에서는 `implicit def`로 했는데 "어디서 무슨 변환이 몰래 끼어들었는지 추적이 어렵다"는 악명이 있었습니다. Scala 3는 이를 `scala.Conversion`이라는 *명시적 타입을 가진* given 인스턴스로만 제한해, "변환을 정의했다"는 사실이 코드에 분명히 드러나게 했습니다. 강력하지만 남용하면 코드가 마법처럼 불투명해지니 꼭 필요할 때만 쓰세요.

암시적 변환(implicit conversion)은 `scala.Conversion` 클래스의 주어진 인스턴스로 구현됩니다. 클래스 시그니처는 다음과 같습니다.

```scala
abstract class Conversion[-T, +U] extends (T => U):
  def apply (x: T): U
```

타입 `T`의 값을 타입 `U`로 자동 변환하는 메커니즘을 제공합니다.

#### 6.2. 정의 예제

`String`에서 `Token`으로의 변환은 다음과 같이 정의할 수 있습니다.

```scala
given Conversion[String, Token]:
  def apply(str: String): Token = new KeyWord(str)
```

또는 별칭(alias)을 사용해 더 간결하게 정의할 수 있습니다.

```scala
given Conversion[String, Token] = new KeyWord(_)
```

#### 6.3. 변환이 적용되는 시점

컴파일러는 다음 세 가지 상황에서 암시적 변환을 자동으로 호출합니다.

1. 타입 `S`가 기대되는 위치에 타입 `T`의 표현식이 제공되지만, `T`가 `S`의 서브타입이 아닐 때
2. `e`가 타입 `T`인 선택 표현식 `e.m`에서, `T`에 멤버 `m`이 없을 때
3. `T`가 `m`을 정의하지만 제공된 인수와 일치하는 버전이 없는 적용 표현식 `e.m(args)`에서

적합한 `scala.Conversion` 인스턴스가 존재하면 표현식 `e`는 `C.apply(e)`로 대체됩니다.

#### 6.4. 실전 예제

**자동 박싱(auto-boxing) 변환:**

```scala
given int2Integer: Conversion[Int, java.lang.Integer] =
  java.lang.Integer.valueOf(_)
```

**enum 기반 라우팅을 활용한 마그넷 패턴(Magnet Pattern):**

마그넷 패턴은 여러 메서드 변형(variant)을 특화된(specialized) 타입으로의 변환을 통해 하나로 통합합니다. 오버로딩(overloading)이 불가능하거나 과도한 중복이 발생할 때 유용합니다.

```scala
object Completions:

  enum CompletionArg:
    case Error(s: String)
    case Response(f: Future[HttpResponse])
    case Status(code: Future[StatusCode])

  object CompletionArg:

    given fromString    : Conversion[String, CompletionArg]               = Error(_)
    given fromFuture    : Conversion[Future[HttpResponse], CompletionArg] = Response(_)
    given fromStatusCode: Conversion[Future[StatusCode], CompletionArg]   = Status(_)
  end CompletionArg
  import CompletionArg.*

  def complete[T](arg: CompletionArg) = arg match
    case Error(s) => ...
    case Response(f) => ...
    case Status(code) => ...

end Completions
```

이 패턴에서 `complete` 메서드는 단일 타입인 `CompletionArg`만을 받지만, `String`, `Future[HttpResponse]`, `Future[StatusCode]` 등 다양한 인수 타입을 암시적 변환을 통해 받아들일 수 있습니다.

> **참고:** Scala 3에서 암시적 변환을 정의하는 권장 방식은 `scala.Conversion`의 주어진 인스턴스를 사용하는 것입니다. 구(舊) Scala 2 스타일의 `implicit def` 변환과 달리, 이 방식은 변환을 명시적인 타입으로 식별 가능하게 만듭니다.

---

### 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Contextual Abstractions](https://docs.scala-lang.org/scala3/reference/contextual/)
- [Extension Methods](https://docs.scala-lang.org/scala3/reference/contextual/extension-methods.html)
- [Type Classes](https://docs.scala-lang.org/scala3/reference/contextual/type-classes.html)
- [Type Class Derivation](https://docs.scala-lang.org/scala3/reference/contextual/derivation.html)
- [Multiversal Equality](https://docs.scala-lang.org/scala3/reference/contextual/multiversal-equality.html)
- [Context Functions](https://docs.scala-lang.org/scala3/reference/contextual/context-functions.html)
- [Implicit Conversions](https://docs.scala-lang.org/scala3/reference/contextual/conversions.html)
