# Scala 3 컨텍스트 추상화: given/using

---

## 목차

1. [컨텍스트 추상화 개요](#1-컨텍스트-추상화-개요)
2. [주어진 인스턴스 (Given Instances)](#2-주어진-인스턴스-given-instances)
3. [using 절 (Using Clauses)](#3-using-절-using-clauses)
4. [컨텍스트 바운드 (Context Bounds)](#4-컨텍스트-바운드-context-bounds)
5. [given 임포트 (Given Imports)](#5-given-임포트-given-imports)
6. [이름에 의한 컨텍스트 매개변수 (By-Name Context Parameters)](#6-이름에-의한-컨텍스트-매개변수-by-name-context-parameters)
7. [Scala 2 implicit와의 관계](#7-scala-2-implicit와의-관계)
8. [참고 자료](#참고-자료)

---

## 1. 컨텍스트 추상화 개요

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

### 1.1 기존 implicit 시스템에 대한 비판

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

### 1.2 재설계된 접근법

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

### 1.3 관련 기능

이 섹션은 다음 기능들도 다룹니다.

- 컨텍스트 바운드(context bound) — 이전 버전과 동일하게 유지
- 확장 메서드(extension method) — 암시적 클래스(implicit class)를 대체
- 타입 클래스(type class) 구현 패턴
- 대수적 데이터 타입(ADT)에 대한 타입 클래스 도출(type class derivation)
- 다중우주 동등성(multiversal equality)
- 컨텍스트 함수(context function)
- 이름에 의한 컨텍스트 매개변수(by-name context parameter)
- Scala 2 implicit으로부터의 마이그레이션 안내

### 1.4 설계 철학

이 재설계는 관심사의 분리(separation of concerns)를 강조합니다. given 정의를 위한 별도 구문, 매개변수를 위한 using 절, 범위 관리를 위한 given 임포트, 암시적 변환을 위한 변환 클래스를 각각 명확히 구분합니다. 이 접근법은 기능 간 상호작용을 줄이고, 일관성을 높이며, 숙련된 개발자와 입문자 모두에게 효과적으로 작동하는 직교적(orthogonal)인 언어 구조를 만드는 것을 목표로 합니다.

---

## 2. 주어진 인스턴스 (Given Instances)

### 2.1 개요

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

### 2.2 기본 구조

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

### 2.3 익명 givens (Anonymous Givens)

> 📘 **처음 배우는 분께 — 이름 없는 given**
>
> given은 주로 "컴파일러가 타입을 보고 자동으로 골라 쓰는" 용도라, 사람이 이름으로 직접 부를 일이 거의 없습니다. 그래서 **이름을 생략**해도 됩니다.
>
> 이름을 비워 두면 컴파일러가 타입으로부터 `given_Ord_Int` 같은 이름을 알아서 붙입니다. "값은 등록하되 이름은 신경 쓰지 않겠다"는 흔한 경우에 씁니다.

이름은 생략할 수 있으며, 이 경우 컴파일러가 구현된 타입으로부터 식별자를 합성합니다. 예를 들어 이름 없는 `Ord[Int]`는 `given_Ord_Int`가 되고, 이름 없는 `Ord[List[T]]`는 `given_Ord_List`가 됩니다.

### 2.4 별칭 givens (Alias Givens)

별칭 givens는 표현식(expression)을 사용하여 인스턴스를 정의합니다.

```scala
given global: ExecutionContext = ForkJoinPool()
```

이것은 익명으로도 가능합니다.

```scala
given Position = enclosingTree.position
```

이러한 별칭 givens는 첫 접근 시점에 초기화됩니다. 무조건적(unconditional) 인스턴스의 경우 초기화는 스레드 안전(thread-safe)하며 캐시됩니다.

### 2.5 초기화 동작 (Initialization Behavior)

- 매개변수가 없는 **무조건적 givens**는 첫 접근 시점에 요청에 따라(on-demand) 초기화됩니다.
- **불변(immutable) 별칭 givens**는 캐시된 필드 오버헤드 없이 단순한 포워더(forwarder)로 동작합니다.
- **조건부 givens**는 참조될 때마다 새로운 인스턴스를 생성합니다.

### 2.6 구문 개요

givens는 다음 패턴을 따릅니다.

- 예약어 `given`
- 선택적 이름과 콜론(`:`)
- 선택적 조건(타입 매개변수, 항 매개변수, 또는 함수 인자 타입)
- 단일 구현 타입과 함께
  - `=` 표현식(별칭 형태, alias form), 또는
  - 생성자 적용(constructor application)과 템플릿 본문(structural form, 구조적 형태)

---

## 3. using 절 (Using Clauses)

### 3.1 개요

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

### 3.2 기본 구문

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

### 3.3 익명 컨텍스트 매개변수 (Anonymous Context Parameters)

컨텍스트 매개변수가 다른 함수로 단순히 전달(forward)되기만 할 때는 그 이름을 생략할 수 있습니다.

```scala
def maximum[T](xs: List[T])(using Ord[T]): T =
  xs.reduceLeft(max)
```

매개변수는 전체 선언 형태인 `(p_1: T_1, ..., p_n: T_n)` 또는 타입만 지정하는 형태인 `T_1, ..., T_n` 중 하나로 지정할 수 있습니다. 다만 가변 인자(vararg) 매개변수는 `using` 절에서 지원되지 않습니다.

### 3.4 클래스 컨텍스트 매개변수 (Class Context Parameters)

클래스 컨텍스트 매개변수에 `val` 또는 `var`를 붙이면 클래스 멤버(member)가 됩니다.

```scala
class GivenIntBox(using val usingParameter: Int):
  def myInt = summon[Int]

val b = GivenIntBox(using 23)
```

클래스 내부에서 발생할 수 있는 모호성(ambiguity)을 피할 수 있어, 명시적인 `given` 멤버를 별도로 선언하는 것보다 이 방식이 권장됩니다.

### 3.5 복잡한 인자의 추론 (Inferring Complex Arguments)

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

### 3.6 다중 using 절 (Multiple using Clauses)

여러 개의 `using` 절이 공존할 수 있으며, 일반 매개변수와 자유롭게 혼합될 수 있습니다.

```scala
def f(u: Universe)(using ctx: u.Context)(using s: ctx.Symbol, k: ctx.Kind) = ...
```

인자는 왼쪽에서 오른쪽으로 매칭됩니다. 따라서 `ctx`를 먼저 공급하지 않고 `f(global)(using sym, kind)`처럼 호출하면 실패합니다.

### 3.7 명시적 using과 암시적 using

인자가 `using`을 통해 명시적으로 공급되면 누락된 매개변수는 기본값(default)으로 채워집니다. 암시적으로 공급될 때는 누락된 매개변수에 암시적 값(implicit value)이 사용됩니다. 이 차이는 중요합니다.

```scala
def f(using p: Boolean, i: Int, s: String = "hello, world") = ...

f(using p = false, i = 27)  // s는 암시적 String이 아니라 기본값을 사용
```

기본값이 암시적 값을 가릴(shadow) 때 컴파일러는 경고를 발생시킵니다.

### 3.8 인스턴스 소환 (Summoning Instances)

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

### 3.9 구문 문법 (Syntax Grammar)

```
ClsParamClause      ::=  ... | UsingClsParamClause
DefParamClause      ::=  ... | UsingParamClause
UsingClsParamClause ::=  '(' 'using' (ClsParams | Types) ')'
UsingParamClause    ::=  '(' 'using' (DefTermParams | Types) ')'
ParArgumentExprs    ::=  ... | '(' 'using' ExprsInParens ')'
```

`using` 키워드는 소프트 키워드(soft keyword)로, 매개변수/인자 목록의 시작 위치에서만 키워드로 인식됩니다.

---

## 4. 컨텍스트 바운드 (Context Bounds)

### 4.1 핵심 개념

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

### 4.2 이름 있는 컨텍스트 바운드 (Named Context Bounds)

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

### 4.3 집합 컨텍스트 바운드 (Aggregate Context Bounds)

여러 개의 컨텍스트 바운드는 중괄호(`{}`)를 사용하여 지정할 수 있습니다.

```scala
def showMax[X : {Ord, Show}](x: X, y: X): String
```

이름 있는 변형도 마찬가지로 작동합니다.

```scala
def showMax[X : {Ord as ordering, Show as show}](x: X, y: X): String =
  show.asString(ordering.max(x, y))
```

### 4.4 매개변수 배치 규칙 (Parameter Placement Rules)

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

### 4.5 다형 함수 타입 (Polymorphic Function Types)

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

### 4.6 추상 타입 멤버 (Abstract Type Members)

추상 타입(abstract type)에 대한 컨텍스트 바운드(Scala 3.6+)는 지연된 givens(deferred givens)로 확장됩니다.

```scala
class Collection:
  type Element: Ord
// 다음으로 확장됨:
class Collection:
  type Element
  given Ord[Element] = deferred
```

### 4.7 구문 (Syntax)

형식 문법은 다음을 포함합니다.

```
ContextBounds     ::=  ContextBound
                    |  '{' ContextBound {',' ContextBound} '}'
ContextBound      ::=  Type ['as' id]
```

---

## 5. given 임포트 (Given Imports)

### 5.1 핵심 개념

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

### 5.2 기본 구문

객체로부터 주어진 인스턴스만 임포트하려면 다음을 사용합니다.

```scala
import A.given
```

일반 임포트와 given 임포트를 결합하려면 다음을 사용합니다.

```scala
import A.{given, *}
```

### 5.3 타입에 의한 임포트 (Importing By Type)

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

### 5.4 마이그레이션 지원 (Migration Support)

명세는 Scala 2의 implicit 시스템을 고려한 전환(transition) 규칙을 포함합니다. 초기에는 `given` 선택자가 구식(old-style) implicit도 함께 임포트합니다. 버전 3.1에서 디프리케이션(deprecation) 경고가 도입되었고, 이후 릴리스에서 점진적으로 강제(enforcement)됩니다. 이를 통해 라이브러리의 점진적 마이그레이션이 가능합니다.

### 5.5 구문 문법 (Syntax Grammar)

형식 명세는 `WildCardSelector`를 `'*'` 또는 `'given' [InfixType]`로 정의하여, 더 넓은 임포트 표현식 구조 안에서 이름 없는(unnamed) given 임포트와 타입 제약된(type-constrained) given 임포트를 모두 허용합니다.

---

## 6. 이름에 의한 컨텍스트 매개변수 (By-Name Context Parameters)

### 6.1 개요

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

### 6.2 핵심 메커니즘

이 기능은 `(ev: => Codec[T])` 같은 구문으로 이름에 의한 컨텍스트 매개변수를 선언합니다. 인자가 실제로 필요해질 때까지 평가를 지연시키며, Scala의 일반 by-name 매개변수와 동일한 방식으로 동작합니다.

### 6.3 예시 구조

참고 문서는 `Codec` 트레이트(trait) 예제를 제공하는데, 그 구조는 다음과 같습니다.

- `intCodec` 주어진 인스턴스가 `Codec[Int]`를 처리합니다.
- `optionCodec` 주어진 인스턴스가 이름에 의한 매개변수 `ev: => Codec[T]`를 받아 `Codec[Option[T]]`를 처리합니다.
- `None`이 매칭될 때, 내부 타입에 대한 코덱은 불필요하게 평가되지 않습니다.

이러한 재귀적 정의(`Codec[Option[Option[Int]]]` 같은 경우)에서, 이름에 의한 매개변수를 사용하지 않으면 컴파일러가 무한히 펼쳐지는 발산을 일으킬 수 있습니다. 지연 평가를 통해 이를 방지합니다.

### 6.4 합성 알고리즘 (Synthesis Algorithm)

컴파일러는 다음 단계를 통해 이름에 의한 컨텍스트 매개변수의 인자를 합성합니다.

1. 필요한 타입의 새(fresh) given을 생성합니다.
2. 초기 후보 검색(initial candidate search) 중에는 해당 given을 사용 불가능 상태로 유지합니다(루프 방지).
3. 다른 by-name 매개변수에 대한 중첩 검색(nested search)에서는 사용 가능 상태로 전환합니다.
4. 합성된 표현식이 이 지역(local) given을 참조하면 `{ given lv: T = E; lv }` 형태로 감쌉니다.
5. 그렇지 않으면 표현식을 변경 없이 반환합니다.

### 6.5 실질적 이점 (Practical Benefit)

> 합성된 컨텍스트 매개변수의 인자는, 그렇게 하지 않으면 발산하는 확장을 방지하기 위해 필요한 경우 지역 `val`로 뒷받침됩니다.

이 방식 덕분에 `Option[Option[Int]]` 같은 재귀적 제네릭 타입이 무한 루프 없이 올바르게 해소됩니다.

---

## 7. Scala 2 implicit와의 관계

Scala 3 참고 문서는 새로운 컨텍스트 추상화 기능이 Scala 2의 implicit 시스템과 어떻게 대응되는지 설명합니다.

> 💡 **왜 필요한가 — 이 절을 읽는 법**
>
> 지금까지 본 `given`·`using`·`Conversion`은 전부 Scala 2의 `implicit` 한 단어가 하던 일을 나눠 가진 것입니다. 이 절은 그 대응 관계를 정리한 변환표라고 보면 됩니다.
>
> Scala 3는 호환을 위해 옛 `implicit`과 새 `given`/`using`을 **둘 다** 지원합니다(7.3절). 그래서 옛 코드를 당장 안 고쳐도 돌아가며, 점진적으로 옮길 수 있습니다. 처음 배우는 분이라면 이 절은 "옛 코드를 만났을 때 돌아와 볼 표" 정도로 넘겨도 좋습니다.

### 7.1 Scala 3 → Scala 2 매핑

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

### 7.2 Scala 2 → Scala 3 매핑

**암시적 변환 (Implicit Conversions)**
`scala.Conversion`의 주어진 인스턴스로 표현될 수 있습니다. 예를 들어, 암시적 변환 함수는 given Conversion 인스턴스가 됩니다.

**암시적 클래스 (Implicit Classes)**
확장 메서드(선호됨) 또는 클래스/given Conversion 쌍으로 매핑됩니다.

**암시적 값 (Implicit Values)**
일반 `val`과 별칭 given(alias given)의 쌍으로 표현될 수 있습니다.

**추상 implicit (Abstract Implicits)**
일반 추상 정의(abstract definition)와 별칭 given으로 표현됩니다.

### 7.3 호환성 정책

문서는 Scala 3가 호환성을 위해 구식 implicit과 새로운 given/using 두 시스템을 모두 지원한다고 명시합니다. 향후 버전에서 구식 implicit은 디프리케이션될 수 있으며, 자동 재작성(automatic rewriting) 도구를 통해 마이그레이션을 지원합니다.

---

## 참고 자료

- [Scala 3 공식 문서](https://docs.scala-lang.org/scala3/)
- [Contextual Abstractions](https://docs.scala-lang.org/scala3/reference/contextual/)
