# 컬렉션 처음부터 만들기 (Creating Collections From Scratch)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/creating-collections-from-scratch.html>

세 개의 정수로 이루어진 리스트를 만드는 `List(1, 2, 3)` 구문과 두 개의 바인딩(binding)을 가진 맵을 만드는 `Map('A' -> 1, 'C' -> 2)` 구문이 있습니다. 이것은 사실 스칼라 컬렉션의 보편적인 기능입니다. 어떤 컬렉션 이름이든 가져다가 그 뒤에 괄호로 감싼 원소 목록을 붙일 수 있습니다. 그 결과는 주어진 원소들을 담은 새 컬렉션이 됩니다. 몇 가지 예를 더 들어 보겠습니다.

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

"내부적으로는" 위의 각 줄이 어떤 객체의 `apply` 메서드 호출입니다. 예를 들어 위의 세 번째 줄은 다음과 같이 확장됩니다.

```scala
val c = List.apply(1.0, 2.0)
```

즉 이것은 `List` 클래스의 동반 객체(companion object)에 있는 `apply` 메서드를 호출하는 것입니다. 이 메서드는 임의 개수의 인자를 받아 그것들로부터 리스트를 만들어 냅니다. 스칼라 라이브러리의 모든 컬렉션 클래스는 이런 `apply` 메서드를 가진 동반 객체를 갖고 있습니다. 컬렉션 클래스가 `List`, `LazyList`, `Vector` 같은 구체적인 구현을 나타내든, `Seq`, `Set`, `Iterable` 같은 추상 기반 클래스이든 상관없습니다. 후자의 경우 `apply`를 호출하면 그 추상 기반 클래스의 어떤 기본 구현이 만들어집니다. 예를 들면 다음과 같습니다.

```scala
scala> List(1, 2, 3)
val res17: List[Int] = List(1, 2, 3)

scala> Iterable(1, 2, 3)
val res18: Iterable[Int] = List(1, 2, 3)

scala> mutable.Iterable(1, 2, 3)
val res19: scala.collection.mutable.Iterable[Int] = ArrayBuffer(1, 2, 3)
```

> 📘 **처음 배우는 분께** — 동반 객체는 클래스와 같은 이름을 가진 싱글턴 객체로, 그 클래스의 "정적 멤버" 역할을 하는 곳입니다. 그리고 스칼라에서 `obj(args)`라고 쓰면 컴파일러가 `obj.apply(args)`로 바꿔 줍니다. 그래서 `new` 없이 `List(1, 2, 3)`처럼 생성자를 부르는 듯한 문법이 가능한 것입니다.

> ⚠️ **짚고 넘어가기** — 위 예에서 `Iterable(1, 2, 3)`의 결과 타입은 `Iterable[Int]`이지만 실제 런타임 값은 `List(1, 2, 3)`입니다. 추상 타입의 `apply`는 "그 추상 타입을 만족하는 어떤 기본 구현"을 골라 줄 뿐이며, 어떤 구현이 선택되는지는 라이브러리의 결정 사항입니다. 불변(immutable) `Iterable`의 기본 구현은 `List`, 가변(mutable) `Iterable`의 기본 구현은 `ArrayBuffer`인 것을 위 출력에서 확인할 수 있습니다.

`apply` 외에도 모든 컬렉션 동반 객체는 빈 컬렉션을 반환하는 `empty` 멤버를 정의합니다. 따라서 `List()` 대신 `List.empty`, `Map()` 대신 `Map.empty`라고 쓸 수 있고, 다른 컬렉션도 마찬가지입니다.

컬렉션 동반 객체가 제공하는 연산들은 아래 표에 정리되어 있습니다. 간단히 말하면 다음과 같습니다.

- `concat` — 임의 개수의 컬렉션을 하나로 이어 붙입니다.
- `fill`과 `tabulate` — 주어진 크기의 1차원 또는 다차원 컬렉션을 만들되, 각 원소를 어떤 식(expression)이나 표를 채우는 함수(tabulating function)로 초기화합니다.
- `range` — 일정한 간격(step)을 가진 정수 컬렉션을 생성합니다.
- `iterate`와 `unfold` — 시작 원소나 상태에 함수를 반복 적용한 결과로 이루어진 컬렉션을 생성합니다.

> 💡 **왜 필요한가** — 이런 팩토리 메서드가 없다면 "0부터 99까지의 제곱수 리스트" 같은 것을 만들 때 가변 버퍼를 만들어 루프를 돌며 채워 넣어야 합니다. `Vector.tabulate(100)(i => i * i)`처럼 한 줄로 선언적으로 쓰면 코드가 짧아질 뿐 아니라, 중간에 가변 상태가 노출되지 않아 실수의 여지도 줄어듭니다. `unfold`는 "다음 값과 다음 상태"를 함수로 기술해서, 종료 조건이 있는 수열 생성(예: 피보나치 수열의 앞부분)을 재귀나 루프 없이 표현할 수 있게 해 줍니다.

### 시퀀스를 위한 팩토리 메서드 (Factory Methods for Sequences)

| 사용법 (WHAT IT IS) | 의미 (WHAT IT DOES) |
| --- | --- |
| `C.empty` | 빈 컬렉션입니다. |
| `C(x, y, z)` | 원소 `x, y, z`로 이루어진 컬렉션입니다. |
| `C.concat(xs, ys, zs)` | `xs, ys, zs`의 원소들을 이어 붙여 얻는 컬렉션입니다. |
| `C.fill(n){e}` | 길이 `n`의 컬렉션으로, 각 원소를 식 `e`로 계산합니다. |
| `C.fill(m, n){e}` | 크기 `m×n`인 컬렉션의 컬렉션으로, 각 원소를 식 `e`로 계산합니다. (더 높은 차원의 버전도 존재합니다.) |
| `C.tabulate(n){f}` | 길이 `n`의 컬렉션으로, 각 인덱스 i의 원소를 `f(i)`로 계산합니다. |
| `C.tabulate(m, n){f}` | 크기 `m×n`인 컬렉션의 컬렉션으로, 각 인덱스 `(i, j)`의 원소를 `f(i, j)`로 계산합니다. (더 높은 차원의 버전도 존재합니다.) |
| `C.range(start, end)` | 정수 `start` … `end-1`로 이루어진 컬렉션입니다. |
| `C.range(start, end, step)` | `start`에서 시작해 `step`씩 증가하며 `end` 값 직전까지(단, `end`는 제외) 나아가는 정수 컬렉션입니다. |
| `C.iterate(x, n)(f)` | 길이 `n`의 컬렉션으로, 원소는 `x`, `f(x)`, `f(f(x))`, … 입니다. |
| `C.unfold(init)(f)` | `init` 상태에서 시작하여, 함수 `f`로 다음 원소와 다음 상태를 계산해 나가는 컬렉션입니다. |
