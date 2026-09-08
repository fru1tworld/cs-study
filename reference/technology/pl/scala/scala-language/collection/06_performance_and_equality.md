# 성능 특성과 동등성

## 성능 특성 (Performance Characteristics)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/performance-characteristics.html>

컬렉션 타입마다 성능 특성이 다르다. 따라서 어떤 연산을 주로 수행하는지가 컬렉션 타입을 선택하는 중요한 기준이 된다.
아래에서는 흔히 쓰이는 연산의 비용을 불변 컬렉션과 가변 컬렉션으로 나눠 살펴본다.

### 시퀀스(sequence) 타입의 성능 특성

- 불변(immutable)
  - `List`: head C, tail C, apply L, update L, prepend C, append L, insert -
  - `LazyList`: head C, tail C, apply L, update L, prepend C, append L, insert -
  - `ArraySeq`: head C, tail L, apply C, update L, prepend L, append L, insert -
  - `Vector`: head eC, tail eC, apply eC, update eC, prepend eC, append eC, insert -
  - `Queue`: head aC, tail aC, apply L, update L, prepend C, append C, insert -
  - `Range`: head C, tail C, apply C, update -, prepend -, append -, insert -
  - `String`: head C, tail L, apply C, update L, prepend L, append L, insert -
- 가변(mutable)
  - `ArrayBuffer`: head C, tail L, apply C, update C, prepend L, append aC, insert L
  - `ListBuffer`: head C, tail L, apply L, update L, prepend C, append C, insert L
  - `StringBuilder`: head C, tail L, apply C, update C, prepend L, append aC, insert L
  - `Queue`: head C, tail L, apply L, update L, prepend C, append C, insert L
  - `ArraySeq`: head C, tail L, apply C, update C, prepend -, append -, insert -
  - `Stack`: head C, tail L, apply L, update L, prepend C, append L, insert L
  - `Array`: head C, tail L, apply C, update C, prepend -, append -, insert -
  - `ArrayDeque`: head C, tail L, apply C, update C, prepend aC, append aC, insert L

### 집합(set)과 맵(map) 타입의 성능 특성

- 불변(immutable)
  - `HashSet`/`HashMap`: lookup eC, add eC, remove eC, min L
  - `TreeSet`/`TreeMap`: lookup Log, add Log, remove Log, min Log
  - `BitSet`: lookup C, add L, remove L, min eC(비트가 조밀하게 채워져 있다고 가정)
  - `VectorMap`: lookup eC, add eC, remove aC, min L
  - `ListMap`: lookup L, add L, remove L, min L
- 가변(mutable)
  - `HashSet`/`HashMap`: lookup eC, add eC, remove eC, min L
  - `WeakHashMap`: lookup eC, add eC, remove eC, min L
  - `BitSet`: lookup C, add aC, remove C, min eC(비트가 조밀하게 채워져 있다고 가정)
  - `TreeSet`: lookup Log, add Log, remove Log, min Log

### 표기 의미

- C: 연산이 (빠른) 상수 시간(constant time)에 수행됨
- eC: 연산이 사실상 상수 시간(effectively constant time)에 수행됨 → 벡터의 최대 길이, 해시 키의 분포 같은 몇 가지 가정에 좌우될 수 있음
- aC: 연산이 분할 상환 상수 시간(amortized constant time)에 수행됨 → 개별 호출 중 일부는 더 오래 걸릴 수 있으나, 많은 연산을 수행하면 연산 하나당 평균적으로 상수 시간만 소요됨
- Log: 연산에 컬렉션 크기의 로그(logarithm)에 비례하는 시간이 걸림
- L: 연산이 선형(linear)임 → 컬렉션 크기에 비례하는 시간이 걸림
- -: 해당 연산이 지원되지 않음

이 표기는 빅오(Big-O) 표기와도 대응한다. C는 O(1), Log는 O(log n), L은 O(n)에 해당한다. eC는 트리 깊이 제한이나 해시 분포 같은 구조적 가정 아래에서 비용이 항상 상수에 가깝다는 뜻이다. 반면 aC는 내부 배열 확장처럼 이따금 비싼 호출이 있어도, 여러 호출에 걸쳐 비용을 나누면 평균이 상수라는 뜻이다.

컬렉션을 선택할 때는 이 비용을 실제 접근 패턴과 함께 봐야 한다. 앞에 원소를 계속 붙인다면 prepend가 C인 `List`가 알맞고, 임의 인덱스 접근이 잦다면 apply가 C인 `ArraySeq`나 eC인 `Vector`가 알맞다. 같은 API를 제공하더라도 자주 쓰는 연산에 따라 적합한 구현이 달라진다.

### 연산 정의: 시퀀스

- head: 시퀀스의 첫 번째 원소를 선택
- tail: 첫 번째 원소를 제외한 나머지 모든 원소로 이루어진 새 시퀀스를 만듦
- apply: 인덱싱(indexing)
- update: 불변 시퀀스에서는 (`updated`를 이용한) 함수형 갱신, 가변 시퀀스에서는 (`update`를 이용한) 부수 효과(side effect)를 동반한 갱신
- prepend: 시퀀스 맨 앞에 원소를 추가 → 불변 시퀀스는 새 시퀀스를 만들어 냄, 가변 시퀀스는 기존 시퀀스를 수정
- append: 시퀀스 맨 뒤에 원소를 추가 → 불변 시퀀스는 새 시퀀스를 만들어 냄, 가변 시퀀스는 기존 시퀀스를 수정
- insert: 시퀀스의 임의 위치에 원소를 삽입 → 가변 시퀀스에서만 직접 지원됨

### 연산 정의: 집합과 맵

- lookup: 어떤 원소가 집합에 들어 있는지 검사하거나, 키에 연결된 값을 선택
- add: 집합에 새 원소를 추가하거나, 맵에 키/값 쌍을 추가
- remove: 집합에서 원소를 제거하거나, 맵에서 키를 제거
- min: 집합의 가장 작은 원소, 또는 맵의 가장 작은 키를 구함

불변 컬렉션의 update, prepend, append 비용은 원본을 제자리에서 바꾸는 비용이 아니라 새 컬렉션을 만드는 비용이다. 불변 `List`의 prepend가 C인 이유도 여기에 있다. 기존 리스트를 통째로 복사하지 않고, 그 리스트를 꼬리로 공유하는 노드 하나만 만들면 된다. 첫 표에 `String`과 `StringBuilder`가 포함된 것은 스칼라에서 문자열도 시퀀스 연산의 대상으로 다루기 때문이다.

## 동등성 (Equality)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/equality.html>

컬렉션 라이브러리는 동등성(equality)과 해싱(hashing)을 집합(set), 맵(map), 시퀀스(sequence)라는 범주에 따라 판단한다. 서로 다른 범주의 컬렉션은 원소가 같아도 같지 않다. 예를 들어 `Set(1, 2, 3)`과 `List(1, 2, 3)`은 서로 다르다.

같은 범주에서는 같은 원소를 가지면 서로 같고, 시퀀스는 순서까지 같아야 한다. 따라서 `List(1, 2, 3) == Vector(1, 2, 3)`과 `HashSet(1, 2) == TreeSet(2, 1)`은 모두 참이다.

여기서 동등성은 `==`로 비교했을 때 `true`가 나오는지를 뜻한다. 스칼라의 `==`는 참조(주소) 대신 `equals`를 통해 값을 비교하며, 컬렉션의 `equals`는 구현 클래스보다 범주와 원소를 기준으로 삼는다. 덕분에 성능 때문에 `List`를 `Vector`로 바꾸더라도 원소와 순서가 같으면 기존 비교 동작을 유지할 수 있다.

가변(mutable) 여부도 동등성의 기준은 아니다. 다만 가변 컬렉션은 비교하는 시점의 원소로 판단하므로 내용을 바꾸면 비교 결과도 달라질 수 있다. 이 성질은 가변 컬렉션을 해시맵(hashmap)의 키로 쓸 때 문제가 된다.

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

마지막 조회는 십중팔구 실패한다. 앞에서 `buf`를 변경하면서 해시 코드(hash code)도 바뀌었기 때문에, 조회가 `buf`를 저장한 곳과 다른 위치를 살펴보게 된다.

원문의 "most likely"는 항상 실패한다는 뜻은 아니다. 바뀐 해시 코드가 우연히 같은 버킷(bucket)에 대응하면 조회가 성공할 수도 있어, 문제가 드러나는지 예측하기 어렵다. 따라서 해시 기반 자료구조의 키에는 불변 컬렉션을 쓰는 것이 좋다. 가변 컬렉션을 사용해야 한다면 키로 쓰는 동안 내용을 변경하지 않아야 한다.
