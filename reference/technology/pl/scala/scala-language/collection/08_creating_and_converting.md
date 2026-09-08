# 컬렉션 처음부터 만들기와 Java 컬렉션 변환

## 컬렉션 처음부터 만들기 (Creating Collections From Scratch)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/creating-collections-from-scratch.html>

`List(1, 2, 3)`으로 세 정수를 담은 리스트를 만들고, `Map('A' -> 1, 'C' -> 2)`로 두 바인딩(binding)을 담은 맵을 만들 수 있다.
- 사실 스칼라 컬렉션 전체가 지원하는 보편적인 기능 → 어떤 컬렉션 이름이든 뒤에 괄호로 감싼 원소 목록을 붙이면, 주어진 원소들을 담은 새 컬렉션이 만들어짐

```scala
val a = Iterable()                // 빈 컬렉션
val b = List()                    // 빈 리스트
val c = List(1.0, 2.0)            // 원소 1.0, 2.0을 가진 리스트
val d = Vector(1.0, 2.0)          // 원소 1.0, 2.0을 가진 벡터
val e = Iterator(1, 2, 3)         // 세 개의 정수를 반환하는 이터레이터
val f = Set(dog, cat, bird)       // 세 동물로 이루어진 집합
val g = HashSet(dog, cat, bird)   // 같은 동물들로 이루어진 해시 집합
val h = Map('a' -> 7, 'b' -> 0)   // 문자에서 정수로 가는 맵
```

- 내부적으로는 위 각 줄이 어떤 객체의 `apply` 메서드 호출
  - 예: 세 번째 줄은 다음과 같이 확장됨

```scala
val c = List.apply(1.0, 2.0)
```

- 즉 `List` 클래스의 동반 객체(companion object)에 있는 `apply` 메서드 호출 → 임의 개수의 인자를 받아 그것들로부터 리스트를 생성
- 스칼라 라이브러리의 모든 컬렉션 클래스는 이런 `apply` 메서드를 가진 동반 객체를 둠
  - 구체적인 구현(`List`, `LazyList`, `Vector` 등)이든, 추상 기반 클래스(`Seq`, `Set`, `Iterable` 등)이든 동일
  - 추상 기반 클래스의 `apply`를 호출하면 그 기반 클래스의 어떤 기본 구현이 만들어짐

```scala
scala> List(1, 2, 3)
val res17: List[Int] = List(1, 2, 3)

scala> Iterable(1, 2, 3)
val res18: Iterable[Int] = List(1, 2, 3)

scala> mutable.Iterable(1, 2, 3)
val res19: scala.collection.mutable.Iterable[Int] = ArrayBuffer(1, 2, 3)
```

동반 객체는 클래스와 같은 이름을 가진 싱글턴 객체로, 그 클래스의 정적 멤버 역할을 한다. 컴파일러가 `obj(args)`를 `obj.apply(args)`로 바꾸므로 `new` 없이 `List(1, 2, 3)`처럼 작성할 수 있다.

추상 타입의 동반 객체도 호출할 수 있지만, 어떤 구체 구현을 반환할지는 라이브러리가 결정한다. 위에서 `Iterable(1, 2, 3)`의 결과 타입은 `Iterable[Int]`지만 런타임 값은 `List(1, 2, 3)`이다. 출력에서 보듯 불변(immutable) `Iterable`의 기본 구현은 `List`이고, 가변(mutable) `Iterable`의 기본 구현은 `ArrayBuffer`다.

- `apply` 외에도 모든 컬렉션 동반 객체는 빈 컬렉션을 반환하는 `empty` 멤버 정의
  - `List()` 대신 `List.empty`, `Map()` 대신 `Map.empty`처럼 사용 가능 → 다른 컬렉션도 동일

- 컬렉션 동반 객체가 제공하는 연산 요약
  - `concat`: 임의 개수의 컬렉션을 하나로 이어 붙임
  - `fill`과 `tabulate`: 주어진 크기의 1차원 또는 다차원 컬렉션 생성, 각 원소는 식(expression)이나 표를 채우는 함수(tabulating function)로 초기화
  - `range`: 일정한 간격(step)을 가진 정수 컬렉션 생성
  - `iterate`와 `unfold`: 시작 원소나 상태에 함수를 반복 적용한 결과로 이루어진 컬렉션 생성

예를 들어 0부터 99까지의 제곱수를 담으려면 가변 버퍼를 루프로 채울 수도 있지만, `Vector.tabulate(100)(i => i * i)`로 각 인덱스의 값을 바로 정의할 수 있다. 중간의 가변 상태를 직접 관리하지 않아도 되는 것이다. 종료 조건이 있는 수열이라면 `unfold`에 다음 값과 다음 상태를 계산하는 함수를 넘겨, 피보나치 수열의 앞부분 같은 컬렉션을 만들 수 있다.

#### 시퀀스를 위한 팩토리 메서드 (Factory Methods for Sequences)

- `C.empty`: 빈 컬렉션
- `C(x, y, z)`: 원소 `x, y, z`로 이루어진 컬렉션
- `C.concat(xs, ys, zs)`: `xs, ys, zs`의 원소들을 이어 붙여 얻는 컬렉션
- `C.fill(n){e}`: 길이 `n`의 컬렉션, 각 원소를 식 `e`로 계산
- `C.fill(m, n){e}`: 크기 `m×n`인 컬렉션의 컬렉션, 각 원소를 식 `e`로 계산(더 높은 차원의 버전도 존재)
- `C.tabulate(n){f}`: 길이 `n`의 컬렉션, 각 인덱스 `i`의 원소를 `f(i)`로 계산
- `C.tabulate(m, n){f}`: 크기 `m×n`인 컬렉션의 컬렉션, 각 인덱스 `(i, j)`의 원소를 `f(i, j)`로 계산(더 높은 차원의 버전도 존재)
- `C.range(start, end)`: 정수 `start` … `end-1`로 이루어진 컬렉션
- `C.range(start, end, step)`: `start`에서 시작해 `step`씩 증가하며 `end` 값 직전까지(단, `end`는 제외) 나아가는 정수 컬렉션
- `C.iterate(x, n)(f)`: 길이 `n`의 컬렉션, 원소는 `x`, `f(x)`, `f(f(x))`, …
- `C.unfold(init)(f)`: `init` 상태에서 시작해, 함수 `f`로 다음 원소와 다음 상태를 계산해 나가는 컬렉션

## Java와 Scala 컬렉션 간 변환 (Conversions Between Java and Scala Collections)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/conversions-between-java-and-scala-collections.html>

- Scala처럼 Java에도 풍부한 컬렉션 라이브러리 존재 → 두 라이브러리 사이에 비슷한 점이 많음
  - 예: 두 라이브러리 모두 이터레이터(iterator), 이터러블(iterable), 집합(set), 맵(map), 시퀀스(sequence)를 갖춤
- 중요한 차이점도 있음
  - Scala 라이브러리는 불변(immutable) 컬렉션에 훨씬 더 큰 비중을 둠
  - 컬렉션을 새로운 컬렉션으로 변환하는 연산을 훨씬 많이 제공

- 때로는 한 컬렉션 프레임워크에서 다른 쪽으로 넘어가야 할 때 존재
  - 예: 기존 Java 컬렉션을 마치 Scala 컬렉션인 것처럼 다루고 싶은 경우
  - 예: Scala 컬렉션을, 그에 대응하는 Java 컬렉션을 기대하는 Java 메서드에 넘기고 싶은 경우
- Scala가 [CollectionConverters](https://www.scala-lang.org/api/2.13.18/scala/jdk/CollectionConverters$.html) 객체에서 주요 컬렉션 타입 전부에 대한 암시적 변환(implicit conversion)을 제공 → 이런 작업이 쉬워짐
  - 구체적으로 다음 타입들 사이의 양방향 변환 지원

```
Iterator               <=>     java.util.Iterator
Iterator               <=>     java.util.Enumeration
Iterable               <=>     java.lang.Iterable
Iterable               <=>     java.util.Collection
mutable.Buffer         <=>     java.util.List
mutable.Set            <=>     java.util.Set
mutable.Map            <=>     java.util.Map
mutable.ConcurrentMap  <=>     java.util.concurrent.ConcurrentMap
```

JDBC나 클라이언트 SDK 같은 Java 라이브러리를 Scala에서 호출할 때도 이 변환을 사용한다. Java API는 `java.util.List`를 요구하고 Scala 코드는 Scala의 `List`를 가지고 있더라도, 원소를 하나씩 옮겨 담는 대신 `CollectionConverters`가 제공하는 메서드로 전달할 수 있다.

- 이 변환들을 사용하려면 [CollectionConverters](https://www.scala-lang.org/api/2.13.18/scala/jdk/CollectionConverters$.html) 객체에서 임포트

- Scala 2:

```scala
scala> import scala.jdk.CollectionConverters._
import scala.jdk.CollectionConverters._
```

- Scala 3:

```scala
scala> import scala.jdk.CollectionConverters.*
import scala.jdk.CollectionConverters.*
```

- 이렇게 하면 `asScala`와 `asJava`라는 확장 메서드(extension method)로 Scala 컬렉션과 그에 대응하는 Java 컬렉션 사이의 변환 가능

- Scala 2:

```scala
scala> import collection.mutable._
import collection.mutable._

scala> val jul: java.util.List[Int] = ArrayBuffer(1, 2, 3).asJava
val jul: java.util.List[Int] = [1, 2, 3]

scala> val buf: Seq[Int] = jul.asScala
val buf: scala.collection.mutable.Seq[Int] = ArrayBuffer(1, 2, 3)

scala> val m: java.util.Map[String, Int] = HashMap("abc" -> 1, "hello" -> 2).asJava
val m: java.util.Map[String,Int] = {abc=1, hello=2}
```

- Scala 3:

```scala
scala> import collection.mutable.*
import collection.mutable.*

scala> val jul: java.util.List[Int] = ArrayBuffer(1, 2, 3).asJava
val jul: java.util.List[Int] = [1, 2, 3]

scala> val buf: Seq[Int] = jul.asScala
val buf: scala.collection.mutable.Seq[Int] = ArrayBuffer(1, 2, 3)

scala> val m: java.util.Map[String, Int] = HashMap("abc" -> 1, "hello" -> 2).asJava
val m: java.util.Map[String,Int] = {abc=1, hello=2}
```

이 변환은 원소를 복사하는 대신, 모든 연산을 원래 컬렉션 객체에 전달하는 래퍼(wrapper)를 만든다. 기존 데이터를 다른 인터페이스로 다루는 방식이므로 변환 비용은 원소 개수와 무관하게 O(1)이다. 두 객체가 데이터를 공유하기 때문에 래퍼를 통해 수정한 값은 원본에도 반영된다.

Java 컬렉션을 대응하는 Scala 타입으로 변환했다가 원래 Java 타입으로 되돌리면 처음과 동일한(identical) 컬렉션 객체를 얻는다. 이 역시 변환 과정에서 데이터를 복사하지 않기 때문이다.

- 특정한 다른 Scala 컬렉션들도 Java 쪽으로 변환할 수는 있지만, 원래의 Scala 타입으로 되돌아오는 변환은 제공되지 않음

```
Seq           =>    java.util.List
mutable.Seq   =>    java.util.List
Set           =>    java.util.Set
Map           =>    java.util.Map
```

- Java는 타입 수준에서 가변(mutable) 컬렉션과 불변 컬렉션을 구분하지 않음
  - 예: `scala.immutable.List`를 변환하면 `java.util.List`가 만들어지는데, 이 리스트에서는 모든 변경 연산이 `UnsupportedOperationException`을 던짐

- Scala 2와 3:

```scala
scala> val jul = List(1, 2, 3).asJava
val jul: java.util.List[Int] = [1, 2, 3]

scala> jul.add(7)
java.lang.UnsupportedOperationException
    at java.util.AbstractList.add(AbstractList.java:148)
```

`asJava`의 반환 타입이 `java.util.List`여도 원본은 여전히 불변 Scala 리스트다. 따라서 `add`나 `remove`를 호출하는 코드는 컴파일되지만 실행 시점에 예외가 발생한다. Java 코드에 불변 컬렉션을 넘길 때는 그 코드가 컬렉션을 수정하는지 확인해야 한다. 수정이 필요하다면 `mutable.Buffer` 같은 가변 컬렉션을 변환해서 넘긴다.
