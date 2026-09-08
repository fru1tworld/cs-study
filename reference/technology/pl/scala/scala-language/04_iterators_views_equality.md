# Scala 이터레이터, 뷰, LazyList와 동등성, 성능

## 이터레이터, 뷰, LazyList

> 원문: https://docs.scala-lang.org/overviews/collections-2.13/iterators.html , https://docs.scala-lang.org/overviews/collections-2.13/views.html

Scala 컬렉션은 기본적으로 즉시(strict) 평가하므로 `map`, `filter`를 호출할 때 결과 전체를 계산해 메모리에 만든다. 이터레이터, 뷰, LazyList는 이 계산을 결과가 필요한 시점까지 미루는 지연(lazy) 평가를 사용하지만, 결과를 소비하고 재사용하는 방식은 서로 다르다.

<a id="1-세-가지-지연-도구-한눈에-구분하기"></a>
### 1. 세 가지 지연 도구, 한눈에 구분하기

- Iterator
  - 정체: 컬렉션이 아니라 "커서(위치 포인터)"
  - 지연되는 것: 원소를 언제 계산할지
  - 다시 쓸 수 있나: 아니오, 한 번 지나가면 끝(소모성)
- View
  - 정체: 컬렉션을 감싼 "변환 계획서"
  - 지연되는 것: `map`/`filter` 같은 변환의 실행 시점
  - 다시 쓸 수 있나: 예, 몇 번이든 다시 순회 가능
- LazyList
  - 정체: 컬렉션 그 자체(`List`처럼 저장, 재사용 가능)
  - 지연되는 것: 머리, 꼬리 모든 원소의 계산 시점
  - 다시 쓸 수 있나: 예, 계산한 값은 캐시되어 재사용

<a id="2-이터레이터iterator"></a>

### 2. 이터레이터(Iterator)

<a id="21-이터레이터는-컬렉션이-아니다"></a>

#### 2.1 이터레이터는 컬렉션이 아님

`Iterator`는 컬렉션의 원소를 하나씩 순서대로 꺼내는 방법이다. 원소를 직접 담아 두는 자료구조와는 역할이 다르며, 다음 두 메서드로 순회를 진행한다.

- `next()`: 다음 원소를 꺼내고 커서를 한 칸 전진
  - 더 꺼낼 게 없으면 `NoSuchElementException`
- `hasNext`: 꺼낼 원소가 남아 있는지 여부

```scala
val it = Iterator(1, 2, 3)
while it.hasNext do println(it.next())
// 1
// 2
// 3
```

- `for (x <- it) ...`, `it.foreach(println)`도 위와 동일하게 동작하는 순회 방법

<a id="22-순회와-소비"></a>

#### 2.2 순회와 소비

이터레이터는 커서처럼 이동하므로 한 번 지나간 자리로 되돌아갈 수 없다. 아래에서는 `foreach`가 원소를 모두 소비한 뒤 `hasNext`로 남은 원소가 없음을 확인한다. 이 상태에서 `next()`로 다시 꺼내려 하면 앞서 설명한 예외가 발생한다.

```scala
val it = Iterator(1, 2, 3)
it.foreach(println)  // 1 2 3 출력, 커서가 끝에 도달함
it.hasNext            // false. 더 이상 꺼낼 게 없음
```

<a id="23-변환-메서드--파괴적으로-진행된다"></a>

#### 2.3 변환 메서드: 파괴적으로 진행됨

- `map`, `filter`, `flatMap`, `zip`, `grouped`, `sliding` 같은 메서드는 `List`에서 쓰던 것과 이름은 같지만 동작 방식은 다름 → 원본 이터레이터의 커서를 그대로 소모하며 진행

```scala
val it = Iterator(1, 2, 3, 4, 5)
val evens = it.filter(_ % 2 == 0)  // it는 이 시점부터 이미 앞으로 당겨져 있음
evens.toList                       // List(2, 4)
it.hasNext                         // false. it는 이미 다 소모됨
```

계산 자체는 지연된다. `filter`를 호출하는 순간 전체를 걸러내지 않고, 다음 원소가 필요할 때 필요한 만큼 검사한다. 그래서 무한한 이터레이터에도 `filter`와 `map`을 적용할 수 있다.

다만 한 번 소비한 이터레이터를 재사용하면 남은 원소가 없어 빈 결과를 얻는다. 같은 데이터를 여러 갈래로 처리하려면 `duplicate`를 사용하거나 원본 컬렉션에서 새 이터레이터를 얻어야 한다.

<a id="24-duplicate로-독립된-사본-만들기"></a>

#### 2.4 duplicate로 독립된 사본 만들기

- 한 이터레이터를 두 갈래로 나누어 각각 다르게 처리하고 싶다면 `duplicate` 사용

```scala
val (a, b) = Iterator(1, 2, 3).duplicate
a.toList  // List(1, 2, 3)
b.toList  // List(1, 2, 3). a와 독립적으로 소모됨
```

- 두 이터레이터는 서로 영향을 주지 않고 각자 소모됨

<a id="25-bufferediterator--미리-엿보기"></a>

#### 2.5 BufferedIterator: 미리 엿보기

- 보통의 이터레이터는 "다음 값이 뭔지 미리 보고 싶다"는 요청을 지원하지 않음(`next()`를 부르는 순간 커서가 이동해 버리므로). `buffered`를 사용하면 커서를 이동시키지 않고 다음 값을 미리 볼 수 있는 `head` 메서드가 생김

```scala
val it = Iterator("", "", "hello", "world").buffered
while it.hasNext && it.head.isEmpty do it.next()  // 빈 문자열만 건너뜀
it.next()  // "hello". 첫 유효한 값을 놓치지 않고 꺼냄
```

- `head`로 미리 확인한 값은 커서를 옮기지 않으므로, 조건에 따라 "꺼낼지 말지"를 안전하게 결정 가능

<a id="26-다른-컬렉션으로-변환하기"></a>

#### 2.6 다른 컬렉션으로 변환하기

- 이터레이터의 계산 결과를 실제 컬렉션으로 확정하려면 `toList`, `toVector`, `toArray`, `toSet`, `toMap` 등 사용
  - 이 시점에 비로소 남은 모든 원소가 계산되어 메모리에 실체화됨

<a id="3-뷰view"></a>

### 3. 뷰(View)

<a id="31-뷰는-변환-계획서다"></a>

#### 3.1 뷰는 "변환 계획서"다

일반 컬렉션에 `map`, `filter`를 연달아 호출하면 각 단계가 즉시(strict) 실행되어 매번 새 컬렉션을 만든다. `.view`를 붙이면 같은 호출이 변환 계획만 기록하고, 결과가 필요해 강제하는 시점에 계산한다. 다음 두 표현식의 차이는 첫 번째 `map`의 결과를 중간 Vector로 만드는지에 있다.

```scala
val v = (1 to 5).toVector

v.map(_ + 1).map(_ * 2)          // 즉시 평가: 중간 Vector가 하나 더 생김
v.view.map(_ + 1).map(_ * 2).toVector  // 지연 평가: 중간 결과 없이 한 번에 계산
```

<a id="32-view와-forceto"></a>

#### 3.2 view와 force(to)

- `collection.view`: 컬렉션을 뷰로 감쌈(변환 시작)
- `.to(List)`, `.toVector`, `.force` 등: 뷰에 쌓인 변환을 실제로 실행해 다시 즉시 컬렉션으로 만듦(강제 실행)

```scala
val words = List("apple", "banana", "kiwi", "fig")

val short: List[String] =
  words.view.filter(_.length <= 4).map(_.toUpperCase).to(List)
// List("KIWI", "FIG")
```

- `to(List)`를 호출하기 전까지는 `filter`도 `map`도 실제로 아무 원소도 건드리지 않음

<a id="33-중간-컬렉션을-만들지-않는-이유"></a>

#### 3.3 중간 컬렉션을 만들지 않는 이유

`v.map(f).map(g)`를 즉시 평가하면 각 단계에서 전체 크기의 새 컬렉션을 만든다. 뷰는 변환을 기록해 두었다가 강제할 때 각 원소에 `f`와 `g`를 차례로 적용한다. 따라서 변환 계획을 만드는 비용은 원소 개수와 무관한 O(1)로 취급한다.

앞쪽 일부만 필요할 때도 뷰로 불필요한 계산을 줄일 수 있다. 백만 개짜리 컬렉션에서 조건에 맞는 원소 100개를 찾는다고 해 보자. 즉시 평가하는 `filter`는 먼저 전체를 훑어 새 컬렉션을 만들지만, 뷰에 `filter`와 `take(100)`을 적용하면 100개를 찾은 순간 나머지 검사를 멈춘다.

```scala
words.view.filter(_.length > 3).take(2).toList
```

- 다만 `sorted`는 답을 구하려면 전체 원소를 순회해야 하므로 뷰를 써도 지연의 이점이 없다.

<a id="34-주의-부작용과-재평가"></a>

#### 3.4 주의: 부작용과 재평가

- 뷰는 "언제 실제로 실행되는지"가 코드만 봐서는 눈에 잘 안 띈다는 함정이 있음

```scala
val v = (1 to 3).view.map { i =>
  println(s"계산 중: $i")
  i * 2
}
// 아직 아무것도 출력되지 않음: map은 계획만 세운 상태

v.toList  // 이제야 "계산 중: 1", "계산 중: 2", "계산 중: 3"이 출력됨
```

뷰를 `toList`나 `foreach`로 강제(force)하기 전에는 로깅이나 외부 호출 같은 부작용도 실행되지 않는다. 반대로 강제할 때마다 변환이 다시 실행되므로 출력, 액터 생성, DB 호출도 그 횟수만큼 반복될 수 있다. 따라서 부작용을 목적으로 뷰를 구성했다면 언제, 몇 번 강제하는지 함께 확인해야 한다.

<a id="4-lazylist"></a>

### 4. LazyList

`LazyList`는 Scala 2.13에서 옛 `Stream`을 대체한 컬렉션이다. `List`처럼 순서 있는 원소를 저장하고 여러 번 순회할 수 있지만, 원소는 필요한 시점에 계산한다. 한 번 계산한 값은 메모이제이션하므로 이후 순회에서는 다시 계산하지 않는다.

<a id="41-머리도-꼬리도-지연된다"></a>

#### 4.1 머리도 꼬리도 지연됨

- 과거 `Stream`은 `head`를 만드는 시점에 바로 계산해 버려서(꼬리만 지연) 종종 예상 밖의 즉시 평가가 일어남
  - `LazyList`는 이 문제를 고쳐 `head`와 `tail` 모두 실제로 접근하기 전까지는 계산하지 않음
  - `LazyList(1, 2, 3)`을 만드는 것만으로는 아무 원소도 계산되지 않고, `head`나 `tail`을 불러야 그 자리의 값이 비로소 계산됨

```scala
val lz = LazyList(1, 2, 3)  // 아직 아무 원소도 계산되지 않음
lz.head        // 1. 이 호출 시점에 비로소 계산됨
lz.tail.head   // 2. 두 번째 원소도 이 시점에 계산됨
```

<a id="42-무한열-표현하기"></a>

#### 4.2 무한열 표현하기

- 계산을 미룰 수 있다는 성질 덕분에 끝이 없는 목록도 정의 가능
  - 필요한 만큼만 `take`로 잘라내면 무한 목록이라도 문제없이 다룰 수 있음

```scala
def naturals(n: Int): LazyList[Int] = n #:: naturals(n + 1)

val nats = naturals(1)      // 무한한 자연수 목록
nats.take(5).toList          // List(1, 2, 3, 4, 5). 앞 5개만 실제로 계산됨
```

- `#::`는 `LazyList`에서 `::`(cons) 대신 쓰는 지연 연결 연산자
  - 오른쪽 항인 `naturals(n + 1)`은 실제로 `tail`에 접근하기 전까지 평가되지 않음

<a id="43-메모이제이션memoization에-따른-메모리-누수-주의"></a>

#### 4.3 메모이제이션(memoization)에 따른 메모리 누수 주의

메모이제이션은 계산을 줄이는 대신 계산한 원소를 메모리에 유지한다. 목록 앞부분을 가리키는 참조가 남아 있으면 이미 소비한 원소도 해제되지 않아 계속 쌓일 수 있다. 무한열이나 아주 긴 `LazyList`를 순회할 때는 처음 원소를 담은 `val`을 얼마나 오래 유지하는지 살펴봐야 한다.

<a id="5-세-도구-비교표"></a>

### 5. 세 도구 비교표

- 재순회 가능 여부
  - Iterator: 불가(1회성)
  - View: 가능
  - LazyList: 가능
- 계산 결과 캐시 여부
  - Iterator: 해당 없음(순회하며 즉시 소모)
  - View: 캐시 안 함(매번 재계산)
  - LazyList: 캐시함(메모이제이션)
- 무한 시퀀스 표현
  - Iterator: 가능
  - View: 원본이 유한하면 유한
  - LazyList: 가능
- 대표 사용 시점
  - Iterator: 한 번만 훑고 버릴 대량 데이터 처리
  - View: 중간 컬렉션 없이 변환 체이닝
  - LazyList: 재사용 가능한 지연 목록, 무한열
- 부작용 함수와 함께 쓸 때 위험
  - Iterator: 낮음(순서대로 한 번씩만 실행)
  - View: 높음(강제할 때마다 재실행)
  - LazyList: 낮음(한 번 계산되면 캐시됨)

<a id="6-참고-자료"></a>

### 6. 참고 자료

- [Scala Collections: Iterators](https://docs.scala-lang.org/overviews/collections-2.13/iterators.html)
- [Scala Collections: Views](https://docs.scala-lang.org/overviews/collections-2.13/views.html)
- [Scala 표준 라이브러리 API: LazyList](https://www.scala-lang.org/api/current/scala/collection/immutable/LazyList.html)

## 컬렉션의 동등성과 성능 특성

> 원문: https://docs.scala-lang.org/overviews/collections-2.13/equality.html

> 원문: https://docs.scala-lang.org/overviews/collections-2.13/performance-characteristics.html

<a id="1-개요"></a>
### 1. 개요

지연 평가 여부 외에도 컬렉션을 선택할 때 살펴볼 기준이 있다. 공식 문서는 동등성과 연산별 성능을 각각 설명한다.

- 동등성(equality): `==`로 두 컬렉션을 비교했을 때 언제 같다고 판단되는지
- 성능 특성(performance characteristics): `head`, `apply`, `add` 같은 연산이 각 컬렉션 타입에서 상수 시간인지 선형 시간인지

동등성 규칙은 가변 컬렉션을 해시맵 키로 쓸 때의 동작과 연결된다. 성능 특성은 반복문 안에서 `List`에 인덱스로 접근하는 것처럼, 자주 실행하는 연산의 비용을 판단하는 데 필요하다.

<a id="2-컬렉션-동등성의-규칙"></a>

### 2. 컬렉션 동등성(`==`)의 규칙

- 참고: 왜 필요한가
- Java에서는 컬렉션 종류(`ArrayList` vs `LinkedList`)가 다르면 `equals`가 대체로 내용까지 비교해 주지만, 인터페이스가 다르면(`List` vs `Set`) 아예 비교 자체가 어색함
- Scala는 "컬렉션이 속한 큰 분류(세트/맵/시퀀스)만 같으면, 구체적인 구현 타입은 신경 쓰지 말고 내용물로 비교한다"는 일관된 규칙을 세워 이 혼란을 없앰

<a id="21-규칙-1--카테고리가-다르면-무조건-다르다"></a>

#### 2.1 규칙 1: 카테고리가 다르면 무조건 다르다

- Scala 컬렉션은 크게 세 카테고리로 나뉨

- Seq (순서가 있는 시퀀스: `List`, `Vector`, `ArraySeq` ...)
- Set (집합: `HashSet`, `TreeSet` ...)
- Map (맵: `HashMap`, `TreeMap` ...)

- 카테고리가 다르면 원소가 똑같아도 항상 `false`.

```scala
Set(1, 2, 3) == List(1, 2, 3)   // false. Set과 Seq는 카테고리가 다름
List(1, 2, 3) == Seq(1, 2, 3)   // true. 둘 다 Seq 카테고리
```

<a id="22-규칙-2--같은-카테고리면-구현이-아니라-내용물로-비교한다"></a>

#### 2.2 규칙 2: 같은 카테고리면 구현이 아니라 내용물로 비교함

- 같은 카테고리 안에서는 구체적인 클래스가 달라도 원소만 같으면 동등
  - Seq는 순서까지 같아야 하고, Set/Map은 순서와 무관하게 원소 집합만 같으면 됨

```scala
List(1, 2, 3) == Vector(1, 2, 3)       // true. 둘 다 Seq, 순서도 같음
List(1, 2, 3) == List(3, 2, 1)         // false. Seq는 순서까지 비교
HashSet(1, 2) == TreeSet(2, 1)         // true. Set은 순서 무관
```

- 주의
- "구현이 다르면 안 같다"고 생각하기 쉽지만 정반대
  - `List`와 `Vector`처럼 내부 구조가 완전히 다른 두 타입도 카테고리(Seq)와 내용(원소, 순서)만 맞으면 서로 `==`로 비교했을 때 `true`
- 배열(`Array`)만은 예외다. Scala 2.13 기준 `Array`의 `==`는 참조 동등성(reference equality)을 그대로 물려받으므로 원소가 같아도 다른 배열 인스턴스면 `false`
- 배열 내용 비교에는 `sameElements`를 쓰거나 `.toSeq`로 감싸 비교해야 함

<a id="23-가변-컬렉션과-동등성--해시-키로-쓸-때의-함정"></a>

#### 2.3 가변 컬렉션과 동등성: 해시 키로 쓸 때의 함정

가변(mutable) 컬렉션의 동등성은 현재 담고 있는 원소로 결정된다. 내용을 바꾸면 `==` 결과와 `hashCode`도 바뀔 수 있으므로, 이를 `HashMap`의 키로 쓰면 다음과 같이 조회에 실패할 수 있다.

```scala
import scala.collection.mutable.{ArrayBuffer, HashMap}

val buf = ArrayBuffer(1, 2, 3)
val map = HashMap(buf -> "value")

map(buf)          // "value". 아직은 정상 조회

buf(0) += 1       // buf의 내용이 바뀜 → hashCode도 바뀜
map(buf)          // NoSuchElementException! 버킷 위치가 어긋나 못 찾음
```

`HashMap`은 키를 넣을 때의 `hashCode`로 버킷 위치를 정한다. 이후 키로 쓴 가변 컬렉션을 변경하면 `hashCode`도 달라져, 조회할 때 계산한 버킷이 저장 당시의 버킷과 어긋난다. 데이터가 맵에 남아 있어도 찾지 못하는 이유다.

- 실전 원칙: 해시 기반 컬렉션(`HashMap`, `HashSet`)의 키, 원소로는 불변 컬렉션만 사용
  - 가변 컬렉션을 키로 써야 한다면 넣은 뒤 다시는 그 내용을 바꾸지 않는다는 확신이 있을 때만 허용

<a id="3-성능-특성-표기법"></a>

### 3. 성능 특성 표기법

- 공식 문서는 각 연산의 시간 복잡도를 다음 다섯 기호로 요약

- C: 상수 시간(Constant): 컬렉션 크기와 무관하게 항상 빠름
- eC: 사실상 상수 시간(effectively Constant): 이론적으로는 조건이 있지만(예: 벡터 길이가 현실적 한계 안일 때) 실질적으로 상수로 취급 가능
- aC: 상환 상수 시간(amortized Constant): 개별 호출은 느릴 수 있어도, 여러 번 호출한 평균은 상수 시간(예: 배열이 꽉 차면 가끔 통째로 재할당하지만 자주 있는 일은 아님)
- Log: 로그 시간: 컬렉션 크기의 로그에 비례(균형 트리 등)
- L: 선형 시간(Linear). 컬렉션 크기에 비례하므로 커질수록 느려짐
- –: 미지원: 해당 연산 자체를 제공하지 않음

- 참고: "eC"와 "aC" 구분
- eC(사실상 상수): 이론적 최악의 경우엔 로그 시간이지만 실제 크기 범위에서는 상수와 다름없음(`Vector`가 대표적)
- aC(상환 상수): 가끔 한 번 비싼 연산(배열 재할당 등)이 끼어도, 여러 번 호출을 평균 내면 상수(`ArrayBuffer.append`가 대표적)
- 차이: eC는 "구조가 원래 빠르다", aC는 "가끔 비싸지만 평균은 싸다"

<a id="4-순차-컬렉션sequence의-성능"></a>

### 4. 순차 컬렉션(Sequence)의 성능

- 시퀀스는 `head`(첫 원소), `tail`(첫 원소 제외 나머지), `apply`(인덱스 접근), `update`(특정 위치 변경), `prepend`(앞에 추가), `append`(뒤에 추가) 여섯 연산 기준으로 비교

<a id="41-불변-시퀀스"></a>

#### 4.1 불변 시퀀스

- `List`: head C, tail C, apply L, update L, prepend C, append L
- `LazyList`: head C, tail C, apply L, update L, prepend C, append L
- `Vector`: head eC, tail eC, apply eC, update eC, prepend eC, append eC
- `ArraySeq`: head C, tail L, apply C, update L, prepend L, append L
- `Range`: head C, tail C, apply C, update –, prepend –, append –
- `Queue`: head aC, tail aC, apply L, update L, prepend C, append C
- `String`: head C, tail L, apply C, update L, prepend L, append L

- `List`: "머리와 꼬리로 이루어진 연결 리스트" → 앞쪽 연산(`head`, `tail`, `prepend`)은 전부 상수 시간
  - 인덱스로 임의 위치에 접근(`apply`)하거나 끝에 추가(`append`)하려면 리스트를 처음부터 끝까지 훑어야 해 선형 시간
- `Vector`: 내부적으로 갈래가 32개인 트리 구조 → 이론적으로는 `Log`지만, 트리 깊이가 실전 데이터 크기에서 사실상 상수(트리 깊이 ≤ 6~7 정도)라 모든 주요 연산이 `eC`로 균형 잡혀 있음
  - "이것도 저것도 적당히 빠른" 범용 시퀀스가 필요할 때의 기본 선택지
- `ArraySeq`(불변): 배열 하나를 그대로 감쌈 → 인덱스 접근(`apply`)은 `C`지만, 불변이라 원소 하나를 바꾸려면(`update`) 배열 전체를 복사해야 해 `L`
- `Range`: `start`, `end`, `step` 세 숫자만 들고 있는 컬렉션 → `head`/`apply`가 계산만으로 끝나 `C`. 대신 원소를 하나씩 끼워 넣거나 값을 바꾼다는 개념 자체가 없어 `update`/`prepend`/`append`는 아예 지원하지 않음(`–`)
- `Queue`: 앞쪽 리스트와 뒤집힌 뒤쪽 리스트 두 개로 구현 → `prepend`/`append` 모두 어느 한쪽 리스트에 얹기만 하면 되어 `C`지만, `head`/`tail`은 뒤쪽 리스트를 뒤집어야 할 때가 있어 `aC`(상환 상수)

```scala
// List: 앞에서부터 처리하는 재귀 알고리즘에 적합
def sumList(xs: List[Int]): Int =
  if xs.isEmpty then 0 else xs.head + sumList(xs.tail)   // head/tail 모두 C

// Vector: 인덱스 접근이 잦은 코드에 적합
val v = Vector(1, 2, 3, 4, 5)
v(2)          // eC. List였다면 L
v.updated(2, 99)   // eC. List.updated는 L
```

<a id="42-가변-시퀀스"></a>

#### 4.2 가변 시퀀스

- `ArrayBuffer`: head C, tail L, apply C, update C, prepend L, append aC
- `ListBuffer`: head C, tail L, apply L, update L, prepend C, append C
- `Array`: head C, tail L, apply C, update C, prepend –, append –
- `StringBuilder`: head C, tail L, apply C, update C, prepend L, append aC

- `ArrayBuffer`: 내부가 배열이라 인덱스 접근, 변경(`apply`, `update`)이 `C`이고, 뒤에 추가(`append`)는 배열이 꽉 찼을 때만 가끔 재할당하므로 `aC`. 반면 앞에 추가(`prepend`)는 기존 원소를 전부 한 칸씩 밀어야 해 `L`. 뒤에서만 추가/삭제하는 스택, 버퍼 용도에 최적
- `ListBuffer`: `List`를 뒤에서부터 빠르게 만들기 위한 가변 버퍼로, 양 끝(`prepend`, `append`)이 모두 `C`. 대신 인덱스 접근(`apply`)은 여전히 리스트 순회가 필요해 `L`

```scala
// ArrayBuffer: 뒤쪽 추가 + 인덱스 접근이 잦을 때
val buf = scala.collection.mutable.ArrayBuffer.empty[Int]
buf += 1; buf += 2; buf += 3   // append aC
buf(1)                          // apply C
```

<a id="5-집합맵setmap의-성능"></a>

### 5. 집합/맵(Set/Map)의 성능

- Set/Map은 `lookup`(조회), `add`(추가), `remove`(삭제), `min`(최솟값) 네 연산으로 비교

- 불변 `HashSet`/`HashMap`: lookup eC, add eC, remove eC, min L
- 불변 `TreeSet`/`TreeMap`: lookup Log, add Log, remove Log, min Log
- 불변 `BitSet`: lookup C, add L, remove L, min eC
- 불변 `VectorMap`: lookup eC, add eC, remove aC, min L
- 불변 `ListMap`: lookup L, add L, remove L, min L
- 가변 `HashSet`/`HashMap`: lookup eC, add eC, remove eC, min L
- 가변 `TreeSet`: lookup Log, add Log, remove Log, min Log
- 가변 `BitSet`: lookup C, add aC, remove C, min eC

- 해시 기반(`HashSet`/`HashMap`): 해시코드로 버킷을 바로 찾아가므로 조회, 추가, 삭제가(가변, 불변 모두) 전부 사실상 상수 시간(`eC`). 단, 최솟값(`min`)은 정렬 정보를 따로 유지하지 않으므로 전체를 훑어야 해 `L`. 순서가 중요하지 않은 대다수 상황의 기본 선택지
- 트리 기반(`TreeSet`/`TreeMap`): 균형 이진 트리(레드-블랙 트리)로 구현되어 모든 연산이 `Log`로 고르지만, 그 대가로 원소가 항상 정렬된 순서를 유지
  - 정렬된 순회나 범위 조회(`range`, `from`, `until`)가 필요할 때 선택
- `BitSet`: 정수 집합을 비트 배열로 표현 → 값의 범위가 촘촘하게 몰려 있을 때(dense) 조회(`C`)와 최솟값 조회(`eC`)가 빠름
  - 추가(`add`)는 불변은 새 비트 배열을 만들어야 해 `L`, 가변은 배열을 가끔만 늘리면 되어 `aC`. 희소(sparse)하고 값의 범위가 아주 넓으면 이 장점이 사라지므로 주의 필요
- `VectorMap`/`ListMap`: `VectorMap`은 삽입 순서를 기억하면서도 조회, 추가는 해시 기반이라 `eC`를 유지
  - `ListMap`은 내부가 연결 리스트라 대부분의 연산이 `L` → 삽입 순서 보존이 꼭 필요한 게 아니라면 `VectorMap`이 더 나은 선택

```scala
import scala.collection.immutable.{HashSet, TreeSet}

val hs = HashSet(3, 1, 2)
hs.contains(2)     // eC. 순서 없는 빠른 조회

val ts = TreeSet(3, 1, 2)
ts.min             // Log. 정렬 구조 덕분에 순서 관련 연산이 안정적
ts.range(1, 3)     // 정렬 순회가 필요하면 TreeSet
```

<a id="6-실전-선택-가이드"></a>

### 6. 실전 선택 가이드

- 앞에서부터 재귀적으로 처리(패턴 매칭, `head`/`tail`) → `List` (앞쪽 연산이 전부 `C`)
- 인덱스 접근, 갱신이 잦은 불변 컬렉션 → `Vector` (모든 연산이 균형 있게 `eC`)
- 뒤에서만 추가/삭제하는 누적 버퍼(가변) → `ArrayBuffer` (append `aC`, 인덱스 `C`)
- 해시맵/해시셋 키로 쓸 값 → 불변 컬렉션 (가변 컬렉션은 내용 변경 시 해시코드가 바뀌어 조회 실패 위험)
- 순서 없이 빠른 존재 확인만 필요 → `HashSet`/`HashMap` (lookup `eC`)
- 정렬된 순서 유지, 범위 조회 필요 → `TreeSet`/`TreeMap` (모든 연산 `Log`이지만 정렬 보장)
- 촘촘한 정수 집합 → `BitSet` (lookup `C`, min `eC`)

### 참고 자료

- [Equality (Scala 2.13 Collections)](https://docs.scala-lang.org/overviews/collections-2.13/equality.html)
- [Performance Characteristics (Scala 2.13 Collections)](https://docs.scala-lang.org/overviews/collections-2.13/performance-characteristics.html)
