# 배열과 문자열

## 배열 (Arrays)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/arrays.html>

- 배열(array)은 스칼라에서 특별한 종류의 컬렉션(collection)
- 스칼라 배열은 자바 배열과 일대일로 대응
  - `Array[Int]` → 자바 `int[]`
  - `Array[Double]` → 자바 `double[]`
  - `Array[String]` → 자바 `String[]`
- 동시에 자바 배열보다 더 많은 것을 제공
  - 제네릭(generic) 가능 → `T`가 타입 파라미터(type parameter)나 추상 타입(abstract type)인 `Array[T]` 사용 가능
  - 스칼라 시퀀스(sequence)와 호환 → `Seq[T]`가 요구되는 자리에 `Array[T]`를 넘길 수 있음
  - 모든 시퀀스 연산도 지원

```scala
scala> val a1 = Array(1, 2, 3)
val a1: Array[Int] = Array(1, 2, 3)

scala> val a2 = a1.map(_ * 3)
val a2: Array[Int] = Array(3, 6, 9)

scala> val a3 = a2.filter(_ % 2 != 0)
val a3: Array[Int] = Array(3, 9)

scala> a3.reverse
val res0: Array[Int] = Array(9, 3)
```

참고 — 처음 배우는 분께
- 자바에서 배열은 `map`이나 `filter` 같은 메서드가 전혀 없는 원시적인 자료구조
- 위 예제는 배열에 대고 그런 메서드를 그대로 호출
- 스칼라 배열이 내부적으로는 자바 배열과 완전히 같은데도 이런 일이 가능한 비밀이 이 문서의 주제

- 스칼라 배열이 자바 배열과 똑같이 표현되는데 추가 기능이 지원되는 이유 → 스칼라의 배열 구현이 암시적 변환(implicit conversion)을 체계적으로 활용
- 스칼라에서 배열은 시퀀스인 척하지 않음 → 네이티브 배열의 데이터 타입 표현이 `Seq`의 서브타입(subtype)이 아니므로 실제로 그럴 수도 없음
- 대신 배열과 `scala.collection.mutable.ArraySeq` 클래스의 인스턴스 사이에 암시적인 "감싸기(wrapping)" 변환 존재
  - `ArraySeq`는 `Seq`의 서브클래스(subclass)

```scala
scala> val seq: collection.Seq[Int] = a1
val seq: scala.collection.Seq[Int] = ArraySeq(1, 2, 3)

scala> val a4: Array[Int] = seq.toArray
val a4: Array[Int] = Array(1, 2, 3)

scala> a1 eq a4
val res1: Boolean = false
```

- 위 상호작용이 보여주는 것
  - 배열에서 `ArraySeq`로의 암시적 변환 → 배열이 시퀀스와 호환됨
  - 반대 방향(`ArraySeq` → `Array`)은 `Iterable`에 정의된 `toArray` 메서드 사용
  - 마지막 REPL 줄 → 감쌌다가 `toArray`로 다시 풀면 원래 배열의 복사본이 만들어짐

- 배열에 적용되는 또 다른 암시적 변환
  - 배열 자체를 시퀀스로 바꾸지는 않으면서, 모든 시퀀스 메서드를 배열에 "추가"
  - 여기서 "추가"란 배열이 `ArrayOps` 타입의 다른 객체로 감싸진다는 뜻 → 이 객체가 모든 시퀀스 메서드를 지원
  - 일반적으로 `ArrayOps` 객체는 수명이 짧음 → 시퀀스 메서드 호출이 끝나면 더 이상 접근할 수 없게 되어 저장 공간이 재활용될 수 있음
  - 최신 VM은 이 객체의 생성 자체를 아예 생략하는 경우가 많음

왜 필요한가 — 변환이 두 가지인 이유
- `ArraySeq` 변환 → 배열을 `Seq` 타입의 값으로 취급해야 할 때(예: `Seq[Int]`를 받는 함수에 배열을 넘길 때)
- `ArrayOps` 변환 → 배열에 시퀀스 메서드를 호출하고 싶을 뿐, 결과는 여전히 배열이길 원할 때
- 하나의 변환만 있었다면 `a1.reverse`의 결과가 `Array`가 아닌 `Seq`가 되어 버려서, 배열로 시작한 연산이 배열로 끝난다는 자연스러운 기대가 깨졌을 것

배열에 대한 두 암시적 변환의 차이는 다음 REPL 대화에서 드러남.

```scala
scala> val seq: collection.Seq[Int] = a1
val seq: scala.collection.Seq[Int] = ArraySeq(1, 2, 3)

scala> seq.reverse
val res2: scala.collection.Seq[Int] = ArraySeq(3, 2, 1)

scala> val ops: collection.ArrayOps[Int] = a1
val ops: scala.collection.ArrayOps[Int] = scala.collection.ArrayOps@2d7df55

scala> ops.reverse
val res3: Array[Int] = Array(3, 2, 1)
```

- `ArraySeq`인 `seq`에 `reverse`를 호출 → 다시 `ArraySeq`
  - `ArraySeq`는 `Seq` → 어떤 `Seq`에든 `reverse`를 호출하면 다시 `Seq`가 나오므로 논리적
- `ArrayOps` 클래스의 값인 `ops`에 `reverse`를 호출 → `Seq`가 아니라 `Array`가 나옴

- 위 `ArrayOps` 예제는 `ArraySeq`와의 차이를 보여주기 위한 것일 뿐 상당히 인위적
- 보통은 `ArrayOps` 클래스의 값을 직접 정의할 일이 전혀 없음 → 그냥 배열에 `Seq` 메서드를 호출하면 됨

```scala
scala> a1.reverse
val res4: Array[Int] = Array(3, 2, 1)
```

`ArrayOps` 객체는 암시적 변환에 의해 자동으로 삽입됨 → 위 줄은 다음과 동등.

```scala
scala> intArrayOps(a1).reverse
val res5: Array[Int] = Array(3, 2, 1)
```

- 여기서 `intArrayOps`는 앞에서 삽입되었던 그 암시적 변환
- 컴파일러가 `ArraySeq`로 가는 다른 암시적 변환 대신 `intArrayOps`를 고른 이유 → 두 변환 모두 배열을, 입력이 요구한 `reverse` 메서드를 지원하는 타입으로 매핑하기 때문에 우선순위 규칙이 필요
- 두 암시적 변환에는 우선순위가 매겨져 있음
  - `ArrayOps` 변환이 `ArraySeq` 변환보다 우선순위 높음
  - 전자는 `Predef` 객체에 정의
  - 후자는 `Predef`가 상속하는 `scala.LowPriorityImplicits` 클래스에 정의
  - 서브클래스와 서브객체(subobject)의 암시(implicit)는 베이스 클래스(base class)의 암시보다 우선 → 두 변환이 모두 적용 가능하면 `Predef`에 있는 쪽이 선택됨
- 문자열(string)에도 이와 아주 유사한 방식이 적용됨

주의 — 짚고 넘어가기
- "우선순위가 높다"는 것은 컴파일러가 성능적으로 더 빠르게 처리한다는 뜻이 아님
- 두 암시적 변환이 동시에 적용 가능할 때 모호성 오류를 내는 대신 어느 쪽을 쓸지 정해 주는 언어 차원의 규칙
- 상속 계층에서 더 구체적인 쪽(서브클래스)에 정의된 암시가 이긴다는 규칙을 이용해, 스칼라 표준 라이브러리가 의도적으로 `ArrayOps`가 먼저 선택되도록 배치해 둔 것

- 배열이 시퀀스와 호환되고 모든 시퀀스 연산을 지원하는 원리는 위와 같음 → 이어서 제네릭성(genericity) 문제
- 자바에서는 `T`가 타입 파라미터일 때 `T[]`라고 쓸 수 없음 → 스칼라의 `Array[T]`는 어떻게 표현되는가
  - 제네릭 배열 `Array[T]`는 런타임(runtime)에 자바의 여덟 가지 원시(primitive) 배열 타입(`byte[]`·`short[]`·`char[]`·`int[]`·`long[]`·`float[]`·`double[]`·`boolean[]`) 중 어느 것일 수도, 객체의 배열일 수도 있음
  - 이 모든 타입을 아우르는 유일한 공통 런타임 타입은 `AnyRef`(또는 동등하게 `java.lang.Object`) → 스칼라 컴파일러는 `Array[T]`를 그 타입으로 매핑
  - 런타임에 `Array[T]` 타입 배열의 원소에 접근하거나 갱신할 때는, 실제 배열 타입을 판별하는 일련의 타입 테스트가 수행된 뒤 자바 배열에 대한 올바른 배열 연산이 이어짐
  - 이 타입 테스트는 배열 연산을 어느 정도 느리게 만듦 → 제네릭 배열에 대한 접근은 원시 배열·객체 배열 접근보다 서너 배 느릴 것으로 예상 → 최대 성능이 필요하다면 제네릭 배열보다 구체적인(concrete) 배열을 선호
- 제네릭 배열 타입을 표현하는 것만으로는 부족 → 제네릭 배열을 생성하는 방법도 필요, 이것이 훨씬 더 어려운 문제
- 문제를 설명하기 위해 배열을 생성하는 제네릭 메서드를 작성하려는 다음 시도를 살펴봄.

Scala 2:

```scala
// 이 코드는 잘못되었습니다!
def evenElems[T](xs: Vector[T]): Array[T] = {
  val arr = new Array[T]((xs.length + 1) / 2)
  for (i <- 0 until xs.length by 2)
    arr(i / 2) = xs(i)
  arr
}
```

Scala 3:

```scala
// 이 코드는 잘못되었습니다!
def evenElems[T](xs: Vector[T]): Array[T] =
  val arr = new Array[T]((xs.length + 1) / 2)
  for i <- 0 until xs.length by 2 do
    arr(i / 2) = xs(i)
  arr
```

- `evenElems` 메서드는 인자로 받은 벡터(vector) `xs`에서 짝수 위치에 있는 모든 원소로 이루어진 새 배열을 반환
- 본문의 첫 줄은 인자와 같은 원소 타입을 가지는 결과 배열을 생성 → `T`의 실제 타입 파라미터에 따라 `Array[Int]`·`Array[Boolean]`·다른 원시 타입 배열·어떤 참조 타입(reference type)의 배열일 수 있음
- 이 타입들은 런타임 표현이 모두 다름 → 스칼라 런타임이 올바른 것을 고를 수 있는가
  - 주어진 정보만으로는 불가능 → 타입 파라미터 `T`에 대응하는 실제 타입이 런타임에는 지워지기(erased) 때문
- 위 코드를 컴파일하면 다음과 같은 오류 메시지가 나타남.

Scala 2:

```
error: cannot find class manifest for element type T
  val arr = new Array[T]((arr.length + 1) / 2)
            ^
```

Scala 3:

```
-- Error: ----------------------------------------------------------------------
3 |  val arr = new Array[T]((xs.length + 1) / 2)
  |                                             ^
  |                                             No ClassTag available for T
```

참고 — 처음 배우는 분께
- "타입이 지워진다"는 것은 타입 소거(type erasure)를 말함
- JVM에서는 제네릭 타입 정보가 컴파일 후에 사라져서, 런타임의 코드는 `T`가 `Int`였는지 `String`이었는지 알 수 없음
- 리스트 같은 일반 컬렉션은 원소를 전부 객체로 담기 때문에 문제되지 않지만, 배열은 `int[]`와 `String[]`의 메모리 구조 자체가 달라서 만들 때 실제 타입을 반드시 알아야 함
- 그래서 배열 생성에서만 이 문제가 표면에 드러남

- 필요한 것 → `evenElems`의 실제 타입 파라미터가 무엇인지에 대한 런타임 힌트를 제공해서 컴파일러를 도와주는 일
- 이 런타임 힌트는 `scala.reflect.ClassTag` 타입의 클래스 매니페스트(class manifest) 형태
  - 클래스 매니페스트는 어떤 타입의 최상위 클래스가 무엇인지를 기술하는 타입 기술자(type descriptor) 객체
  - 타입의 모든 측면을 기술하는 `scala.reflect.Manifest` 타입의 완전한 매니페스트(full manifest)도 있으나, 배열 생성에는 클래스 매니페스트만 있으면 됨

스칼라 컴파일러는 지시하기만 하면 클래스 매니페스트를 자동으로 만들어 줌 → "지시"란 다음처럼 클래스 매니페스트를 암시적 파라미터(implicit parameter)로 요구하는 것.

Scala 2:

```scala
def evenElems[T](xs: Vector[T])(implicit m: ClassTag[T]): Array[T] = ...
```

Scala 3:

```scala
def evenElems[T](xs: Vector[T])(using m: ClassTag[T]): Array[T] = ...
```

더 짧은 대안 문법으로는 컨텍스트 바운드(context bound)를 사용해서 타입에 클래스 매니페스트가 딸려 오도록 요구 가능 → 타입 뒤에 콜론과 클래스 이름 `ClassTag`를 붙임.

Scala 2:

```scala
import scala.reflect.ClassTag
// 이 코드는 동작합니다
def evenElems[T: ClassTag](xs: Vector[T]): Array[T] = {
  val arr = new Array[T]((xs.length + 1) / 2)
  for (i <- 0 until xs.length by 2)
    arr(i / 2) = xs(i)
  arr
}
```

Scala 3:

```scala
import scala.reflect.ClassTag
// 이 코드는 동작합니다
def evenElems[T: ClassTag](xs: Vector[T]): Array[T] =
  val arr = new Array[T]((xs.length + 1) / 2)
  for i <- 0 until xs.length by 2 do
    arr(i / 2) = xs(i)
  arr
```

- 수정된 두 버전의 `evenElems`는 정확히 같은 의미
- 어느 쪽이든 `Array[T]`가 만들어질 때 컴파일러는 타입 파라미터 `T`에 대한 클래스 매니페스트, 즉 `ClassTag[T]` 타입의 암시적 값을 찾음
  - 그런 값이 발견되면 그 매니페스트를 사용해 올바른 종류의 배열을 만듦
  - 그렇지 않으면 위와 같은 오류 메시지가 나타남

다음은 `evenElems` 메서드를 사용하는 REPL 상호작용.

```scala
scala> evenElems(Vector(1, 2, 3, 4, 5))
val res6: Array[Int] = Array(1, 3, 5)

scala> evenElems(Vector("this", "is", "a", "test", "run"))
val res7: Array[java.lang.String] = Array(this, a, run)
```

- 두 경우 모두 스칼라 컴파일러가 원소 타입(처음엔 `Int`, 다음엔 `String`)에 대한 클래스 매니페스트를 자동으로 만들어 `evenElems` 메서드의 암시적 파라미터에 넘겨줌
- 컴파일러는 모든 구체 타입에 대해서는 이렇게 할 수 있지만, 인자 자체가 클래스 매니페스트 없는 또 다른 타입 파라미터인 경우에는 불가능 → 다음은 실패.

Scala 2:

```scala
scala> def wrap[U](xs: Vector[U]) = evenElems(xs)
<console>:6: error: No ClassTag available for U.
     def wrap[U](xs: Vector[U]) = evenElems(xs)
                                           ^
```

Scala 3:

```
-- Error: ----------------------------------------------------------------------
6 |def wrap[U](xs: Vector[U]) = evenElems(xs)
  |                                          ^
  |                                          No ClassTag available for U
```

- 여기서 벌어진 일 → `evenElems`가 타입 파라미터 `U`에 대한 클래스 매니페스트를 요구하는데 아무것도 발견되지 않음
- 해결책 → `U`에 대한 또 다른 암시적 클래스 매니페스트를 요구 → 다음은 동작.

```scala
scala> def wrap[U: ClassTag](xs: Vector[U]) = evenElems(xs)
def wrap[U](xs: Vector[U])(implicit evidence$1: scala.reflect.ClassTag[U]): Array[U]
```

이 예제는 또한 `U` 정의에 붙은 컨텍스트 바운드가, 여기서는 `evidence$1`이라는 이름이 붙은 `ClassTag[U]` 타입의 암시적 파라미터에 대한 축약 표현일 뿐이라는 것도 보여줌.

주의 — 짚고 넘어가기
- 이 문서는 "클래스 매니페스트"라는 용어를 계속 사용하지만, 코드에 실제로 등장하는 타입은 `ClassTag`
- `Manifest`와 `ClassManifest`는 스칼라의 옛 API이고, 현재는 `ClassTag`가 그 역할을 대신함(스칼라 2.13 문서가 옛 용어를 관성적으로 유지하고 있는 것)
- 새 코드를 작성할 때 기억할 것은 하나 → 타입 파라미터 `T`로 배열을 만들어야 한다면 `[T: ClassTag]`를 붙임

- 요약 → 제네릭 배열 생성에는 클래스 매니페스트가 필요
  - 타입 파라미터 `T`의 배열을 생성할 때마다 `T`에 대한 암시적 클래스 매니페스트도 함께 제공해야 함
  - 가장 쉬운 방법 → `[T: ClassTag]`처럼 타입 파라미터를 `ClassTag` 컨텍스트 바운드와 함께 선언

---

## 문자열 (Strings)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/strings.html>

- 배열(array)과 마찬가지로 문자열(string)도 그 자체가 직접 시퀀스(sequence)인 것은 아니지만 시퀀스로 변환 가능
- 문자열 위에서 모든 시퀀스 연산 지원

다음은 문자열에 대해 호출할 수 있는 연산의 몇 가지 예.

```scala
scala> val str = "hello"
val str: java.lang.String = hello

scala> str.reverse
val res6: String = olleh

scala> str.map(_.toUpper)
val res7: String = HELLO

scala> str.drop(3)
val res8: String = lo

scala> str.slice(1, 4)
val res9: String = ell

scala> val s: Seq[Char] = str
val s: Seq[Char] = hello
```

참고 — 처음 배우는 분께
- 스칼라의 `String`은 사실 자바의 `java.lang.String` 그대로
- 자바의 문자열에는 `reverse`·`map`·`drop` 같은 컬렉션 메서드가 없는데도 위 예제가 동작하는 이유 → 스칼라가 문자열을 컬렉션처럼 다룰 수 있게 해 주는 암시적 변환(implicit conversion)을 제공하기 때문

- 이러한 연산은 두 가지 암시적 변환에 의해 지원됨
  - 첫 번째는 우선순위가 낮은 변환 → `String`을 `immutable.IndexedSeq`의 서브클래스(subclass)인 `WrappedString`으로 매핑, 위 예제의 마지막 줄에서 문자열이 `Seq`로 변환될 때 적용된 것이 이 변환
  - 다른 하나는 우선순위가 높은 변환 → 문자열을 `StringOps` 객체로 매핑, 이 객체는 불변(immutable) 시퀀스의 모든 메서드를 문자열에 추가, 위 예제에서 `reverse`·`map`·`drop`·`slice`를 호출할 때 암시적으로 삽입된 것이 이 변환

왜 필요한가 — 두 가지 변환이 필요한 이유
- 두 변환은 반환 타입이 다름
- `StringOps`를 거치는 고우선순위 변환은 결과를 다시 `String`으로 돌려줌(`str.reverse`의 결과가 `String`인 것처럼) → "문자열을 다뤘으니 문자열이 나온다"는 자연스러운 기대를 지켜 줌
- `WrappedString`을 거치는 저우선순위 변환은 문자열을 진짜 `Seq[Char]` 값으로 취급해야 할 때, 즉 `Seq[Char]` 타입이 요구되는 자리에 문자열을 넘길 때 사용됨

주의 — 짚고 넘어가기
- 메서드 호출에는 `StringOps` 변환이, 타입이 `Seq[Char]`로 요구되는 자리에는 `WrappedString` 변환이 적용됨
- 두 변환이 충돌하지 않도록 우선순위가 나뉘어 있을 뿐, 사용자가 어느 쪽을 쓸지 직접 고를 일은 거의 없음
</content>
