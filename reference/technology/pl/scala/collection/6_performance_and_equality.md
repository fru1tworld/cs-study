# 성능 특성과 동등성

## 성능 특성 (Performance Characteristics)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/performance-characteristics.html>

앞선 설명들을 통해 컬렉션 타입마다 성능 특성이 다르다는 점이 분명해졌습니다. 어떤 컬렉션 타입을 다른 타입 대신 선택하는 주된 이유가 바로 이 성능 특성인 경우가 많습니다. 컬렉션에 대한 흔히 쓰이는 몇 가지 연산의 성능 특성을 아래 두 표에 요약해 두었습니다.

시퀀스(sequence) 타입의 성능 특성:

|                | head | tail | apply | update | prepend | append | insert |
| -------------- | ---- | ---- | ----- | ------ | ------- | ------ | ------ |
| **불변(immutable)** |      |      |       |        |         |        |        |
| `List`         | C    | C    | L     | L      | C       | L      | -      |
| `LazyList`     | C    | C    | L     | L      | C       | L      | -      |
| `ArraySeq`     | C    | L    | C     | L      | L       | L      | -      |
| `Vector`       | eC   | eC   | eC    | eC     | eC      | eC     | -      |
| `Queue`        | aC   | aC   | L     | L      | C       | C      | -      |
| `Range`        | C    | C    | C     | -      | -       | -      | -      |
| `String`       | C    | L    | C     | L      | L       | L      | -      |
| **가변(mutable)**   |      |      |       |        |         |        |        |
| `ArrayBuffer`  | C    | L    | C     | C      | L       | aC     | L      |
| `ListBuffer`   | C    | L    | L     | L      | C       | C      | L      |
| `StringBuilder`| C    | L    | C     | C      | L       | aC     | L      |
| `Queue`        | C    | L    | L     | L      | C       | C      | L      |
| `ArraySeq`     | C    | L    | C     | C      | -       | -      | -      |
| `Stack`        | C    | L    | L     | L      | C       | L      | L      |
| `Array`        | C    | L    | C     | C      | -       | -      | -      |
| `ArrayDeque`   | C    | L    | C     | C      | aC      | aC     | L      |

집합(set)과 맵(map) 타입의 성능 특성:

|                     | lookup | add  | remove | min            |
| ------------------- | ------ | ---- | ------ | -------------- |
| **불변(immutable)**  |        |      |        |                |
| `HashSet`/`HashMap` | eC     | eC   | eC     | L              |
| `TreeSet`/`TreeMap` | Log    | Log  | Log    | Log            |
| `BitSet`            | C      | L    | L      | eC<sup>1</sup> |
| `VectorMap`         | eC     | eC   | aC     | L              |
| `ListMap`           | L      | L    | L      | L              |
| **가변(mutable)**    |        |      |        |                |
| `HashSet`/`HashMap` | eC     | eC   | eC     | L              |
| `WeakHashMap`       | eC     | eC   | eC     | L              |
| `BitSet`            | C      | aC   | C      | eC<sup>1</sup> |
| `TreeSet`           | Log    | Log  | Log    | Log            |

각주: <sup>1</sup> 비트가 조밀하게 채워져 있다고(densely packed) 가정합니다.

두 표의 항목이 뜻하는 바는 다음과 같습니다.

|         |                                           |
| ------- | ----------------------------------------- |
| **C**   | 연산이 (빠른) 상수 시간(constant time)에 수행됩니다. |
| **eC**  | 연산이 사실상 상수 시간(effectively constant time)에 수행됩니다. 다만 벡터의 최대 길이나 해시 키의 분포 같은 몇 가지 가정에 좌우될 수 있습니다. |
| **aC**  | 연산이 분할 상환 상수 시간(amortized constant time)에 수행됩니다. 개별 호출 중 일부는 더 오래 걸릴 수 있지만, 많은 연산을 수행하면 연산 하나당 평균적으로 상수 시간만 소요됩니다. |
| **Log** | 연산에 컬렉션 크기의 로그(logarithm)에 비례하는 시간이 걸립니다. |
| **L**   | 연산이 선형(linear)입니다. 즉 컬렉션 크기에 비례하는 시간이 걸립니다. |
| **-**   | 해당 연산이 지원되지 않습니다. |

> 📘 **처음 배우는 분께** — 이 표기는 알고리즘 수업에서 배우는 빅오(Big-O) 표기와 대응합니다. C는 O(1), Log는 O(log n), L은 O(n)에 해당합니다. eC와 aC는 둘 다 "거의 O(1)"이지만 의미가 다릅니다. eC는 트리 깊이 제한이나 해시 분포 같은 구조적 가정 아래에서 항상 상수에 가깝다는 뜻이고, aC는 이따금 비싼 호출(예: 내부 배열 확장)이 있어도 여러 호출에 걸쳐 비용을 나누면 평균이 상수라는 뜻입니다.

> 💡 **왜 필요한가** — 컬렉션 선택은 API의 편의성보다 접근 패턴이 좌우하는 경우가 많습니다. 예컨대 앞쪽에 원소를 계속 붙이는 작업이라면 prepend가 C인 `List`가 알맞고, 임의 인덱스 접근이 잦다면 apply가 C인 `ArraySeq`나 eC인 `Vector`가 알맞습니다. 이 표는 그런 결정을 한눈에 내릴 수 있게 해 주는 치트시트입니다.

첫 번째 표는 불변과 가변을 아우르는 시퀀스 타입을 다음 연산 기준으로 다룹니다.

|             |                                                     |
| ----------- | ---------------------------------------------------- |
| **head**    | 시퀀스의 첫 번째 원소를 선택합니다. |
| **tail**    | 첫 번째 원소를 제외한 나머지 모든 원소로 이루어진 새 시퀀스를 만듭니다. |
| **apply**   | 인덱싱(indexing)입니다. |
| **update**  | 불변 시퀀스에서는 (`updated`를 이용한) 함수형 갱신, 가변 시퀀스에서는 (`update`를 이용한) 부수 효과(side effect)를 동반한 갱신입니다. |
| **prepend** | 시퀀스 맨 앞에 원소를 추가합니다. 불변 시퀀스에서는 새 시퀀스를 만들어 내고, 가변 시퀀스에서는 기존 시퀀스를 수정합니다. |
| **append**  | 시퀀스 맨 뒤에 원소를 추가합니다. 불변 시퀀스에서는 새 시퀀스를 만들어 내고, 가변 시퀀스에서는 기존 시퀀스를 수정합니다. |
| **insert**  | 시퀀스의 임의 위치에 원소를 삽입합니다. 가변 시퀀스에서만 직접 지원됩니다. |

두 번째 표는 가변 및 불변 집합과 맵을 다음 연산 기준으로 다룹니다.

|            |                                                     |
| ---------- | ---------------------------------------------------- |
| **lookup** | 어떤 원소가 집합에 들어 있는지 검사하거나, 키에 연결된 값을 선택합니다. |
| **add**    | 집합에 새 원소를 추가하거나, 맵에 키/값 쌍을 추가합니다. |
| **remove** | 집합에서 원소를 제거하거나, 맵에서 키를 제거합니다. |
| **min**    | 집합의 가장 작은 원소, 또는 맵의 가장 작은 키를 구합니다. |

> ⚠️ **짚고 넘어가기** — 불변 컬렉션의 update, prepend, append가 표에 실려 있다고 해서 제자리 수정이 가능하다는 뜻이 아닙니다. 불변 컬렉션에서 이 연산들은 언제나 *새 컬렉션을 돌려주며*, 표의 수치는 그 새 컬렉션을 만드는 데 드는 비용입니다. 불변 `List`의 prepend가 C인 이유도 기존 리스트를 통째로 복사하는 것이 아니라 기존 리스트를 꼬리로 공유하는 새 노드 하나만 만들면 되기 때문입니다. 또한 첫 표에 `String`과 `StringBuilder`가 등장하듯, 스칼라에서는 문자열도 시퀀스 연산의 대상으로 취급된다는 점을 기억해 두시기 바랍니다.

---

## 동등성 (Equality)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/equality.html>

컬렉션 라이브러리는 동등성(equality)과 해싱(hashing)에 대해 일관된 접근 방식을 따릅니다. 기본 아이디어는 이렇습니다. 먼저 컬렉션을 집합(set), 맵(map), 시퀀스(sequence)라는 범주로 나눕니다. 서로 다른 범주에 속한 컬렉션은 항상 같지 않습니다. 예를 들어 `Set(1, 2, 3)`은 같은 원소를 담고 있더라도 `List(1, 2, 3)`과 같지 않습니다. 반면 같은 범주 안에서는, 두 컬렉션이 같은 원소를 가질 때 그리고 오직 그럴 때에만 서로 같습니다(시퀀스의 경우에는 같은 원소가 같은 순서로 있어야 합니다). 예를 들어 `List(1, 2, 3) == Vector(1, 2, 3)`이고, `HashSet(1, 2) == TreeSet(2, 1)`입니다.

> 📘 **처음 배우는 분께** — 여기서 말하는 동등성은 `==` 연산자로 비교했을 때 `true`가 나오는지를 뜻합니다. 스칼라의 `==`는 참조(주소) 비교가 아니라 `equals` 메서드를 호출하는 값 비교이며, 컬렉션들은 "구체적인 구현 클래스가 무엇인가"가 아니라 "어떤 범주에 속하고 어떤 원소를 담고 있는가"를 기준으로 `equals`를 구현하고 있습니다.

> 💡 **왜 필요한가** — 구현 클래스까지 같아야 동등하다고 정의하면, `List`를 성능상 이유로 `Vector`로 바꾸는 순간 기존 비교 코드가 전부 깨집니다. 범주 + 원소 기준의 동등성 덕분에 사용자는 구체 구현을 자유롭게 교체할 수 있고, 라이브러리 내부 구현이 바뀌어도 동작이 유지됩니다.

동등성 검사에서 컬렉션이 가변(mutable)인지 불변(immutable)인지는 중요하지 않습니다. 가변 컬렉션의 경우, 동등성 검사가 수행되는 시점의 현재 원소들만을 기준으로 판단합니다. 이는 가변 컬렉션이 원소가 추가되거나 제거됨에 따라 시점마다 서로 다른 컬렉션과 같아질 수 있다는 뜻입니다. 이 점은 가변 컬렉션을 해시맵(hashmap)의 키로 사용할 때 빠지기 쉬운 함정이 됩니다. 예를 들면 다음과 같습니다.

```scala
scala> import collection.mutable.{HashMap, ArrayBuffer}
import collection.mutable.{HashMap, ArrayBuffer}

scala> val buf = ArrayBuffer(1, 2, 3)
val buf: scala.collection.mutable.ArrayBuffer[Int] =
  ArrayBuffer(1, 2, 3)

scala> val map = HashMap(buf -> 3)
val map: scala.collection.mutable.HashMap[scala.collection.
  mutable.ArrayBuffer[Int],Int] = Map((ArrayBuffer(1, 2, 3),3))

scala> map(buf)
val res13: Int = 3

scala> buf(0) += 1

scala> map(buf)
  java.util.NoSuchElementException: key not found:
    ArrayBuffer(2, 2, 3)
```

이 예제에서 마지막 줄의 조회는 십중팔구 실패합니다. 바로 앞 줄에서 배열 버퍼 `buf`의 해시 코드(hash code)가 바뀌었기 때문입니다. 그 결과 해시 코드 기반 조회는 `buf`가 실제로 저장된 위치와는 다른 위치를 살펴보게 됩니다.

> ⚠️ **짚고 넘어가기** — 원문의 "most likely"(십중팔구)라는 표현에 주의하세요. 항상 실패한다는 뜻이 아닙니다. 바뀐 해시 코드가 우연히 원래 값과 같은 버킷(bucket)에 대응하면 조회가 성공할 수도 있습니다. 즉 이 버그는 비결정적으로 나타나기 때문에 더 위험합니다. 교훈은 명확합니다. 해시 기반 자료구조의 키로는 불변 컬렉션을 사용하고, 가변 컬렉션을 키로 써야 한다면 키로 쓰는 동안 절대 변경하지 말아야 합니다.
