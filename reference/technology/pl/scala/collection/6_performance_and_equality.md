# 성능 특성과 동등성

## 성능 특성 (Performance Characteristics)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/performance-characteristics.html>

- 컬렉션 타입마다 성능 특성이 다름 → 다른 타입 대신 특정 타입을 선택하는 주된 이유가 되는 경우가 많음
- 흔히 쓰이는 연산의 성능 특성을 아래에 정리

### 시퀀스(sequence) 타입의 성능 특성

- 불변(immutable)
  - `List`: head C · tail C · apply L · update L · prepend C · append L · insert -
  - `LazyList`: head C · tail C · apply L · update L · prepend C · append L · insert -
  - `ArraySeq`: head C · tail L · apply C · update L · prepend L · append L · insert -
  - `Vector`: head eC · tail eC · apply eC · update eC · prepend eC · append eC · insert -
  - `Queue`: head aC · tail aC · apply L · update L · prepend C · append C · insert -
  - `Range`: head C · tail C · apply C · update - · prepend - · append - · insert -
  - `String`: head C · tail L · apply C · update L · prepend L · append L · insert -
- 가변(mutable)
  - `ArrayBuffer`: head C · tail L · apply C · update C · prepend L · append aC · insert L
  - `ListBuffer`: head C · tail L · apply L · update L · prepend C · append C · insert L
  - `StringBuilder`: head C · tail L · apply C · update C · prepend L · append aC · insert L
  - `Queue`: head C · tail L · apply L · update L · prepend C · append C · insert L
  - `ArraySeq`: head C · tail L · apply C · update C · prepend - · append - · insert -
  - `Stack`: head C · tail L · apply L · update L · prepend C · append L · insert L
  - `Array`: head C · tail L · apply C · update C · prepend - · append - · insert -
  - `ArrayDeque`: head C · tail L · apply C · update C · prepend aC · append aC · insert L

### 집합(set)과 맵(map) 타입의 성능 특성

- 불변(immutable)
  - `HashSet`/`HashMap`: lookup eC · add eC · remove eC · min L
  - `TreeSet`/`TreeMap`: lookup Log · add Log · remove Log · min Log
  - `BitSet`: lookup C · add L · remove L · min eC(비트가 조밀하게 채워져 있다고 가정)
  - `VectorMap`: lookup eC · add eC · remove aC · min L
  - `ListMap`: lookup L · add L · remove L · min L
- 가변(mutable)
  - `HashSet`/`HashMap`: lookup eC · add eC · remove eC · min L
  - `WeakHashMap`: lookup eC · add eC · remove eC · min L
  - `BitSet`: lookup C · add aC · remove C · min eC(비트가 조밀하게 채워져 있다고 가정)
  - `TreeSet`: lookup Log · add Log · remove Log · min Log

### 표기 의미

- C: 연산이 (빠른) 상수 시간(constant time)에 수행됨
- eC: 연산이 사실상 상수 시간(effectively constant time)에 수행됨 → 벡터의 최대 길이·해시 키의 분포 같은 몇 가지 가정에 좌우될 수 있음
- aC: 연산이 분할 상환 상수 시간(amortized constant time)에 수행됨 → 개별 호출 중 일부는 더 오래 걸릴 수 있으나, 많은 연산을 수행하면 연산 하나당 평균적으로 상수 시간만 소요됨
- Log: 연산에 컬렉션 크기의 로그(logarithm)에 비례하는 시간이 걸림
- L: 연산이 선형(linear)임 → 컬렉션 크기에 비례하는 시간이 걸림
- -: 해당 연산이 지원되지 않음

참고 — 처음 배우는 분께
- 이 표기는 알고리즘 수업에서 배우는 빅오(Big-O) 표기와 대응함 → C는 O(1), Log는 O(log n), L은 O(n)에 해당
- eC와 aC는 둘 다 "거의 O(1)"이지만 의미가 다름
  - eC: 트리 깊이 제한·해시 분포 같은 구조적 가정 아래에서 항상 상수에 가깝다는 뜻
  - aC: 이따금 비싼 호출(예: 내부 배열 확장)이 있어도 여러 호출에 걸쳐 비용을 나누면 평균이 상수라는 뜻

왜 필요한가
- 컬렉션 선택은 API의 편의성보다 접근 패턴이 좌우하는 경우가 많음
- 앞쪽에 원소를 계속 붙이는 작업 → prepend가 C인 `List`가 알맞음
- 임의 인덱스 접근이 잦음 → apply가 C인 `ArraySeq`나 eC인 `Vector`가 알맞음
- 이 표는 그런 결정을 한눈에 내릴 수 있게 해 주는 치트시트

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

주의 — 짚고 넘어가기
- 불변 컬렉션의 update, prepend, append가 표에 실려 있다고 해서 제자리 수정이 가능하다는 뜻이 아님
- 불변 컬렉션에서 이 연산들은 언제나 새 컬렉션을 돌려줌 → 표의 수치는 그 새 컬렉션을 만드는 데 드는 비용
- 불변 `List`의 prepend가 C인 이유 → 기존 리스트를 통째로 복사하는 것이 아니라 기존 리스트를 꼬리로 공유하는 새 노드 하나만 만들면 됨
- 첫 표에 `String`과 `StringBuilder`가 등장하듯, 스칼라에서는 문자열도 시퀀스 연산의 대상으로 취급됨

---

## 동등성 (Equality)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/equality.html>

- 컬렉션 라이브러리는 동등성(equality)과 해싱(hashing)에 대해 일관된 접근 방식을 따름
- 기본 아이디어
  - 컬렉션을 집합(set)·맵(map)·시퀀스(sequence)라는 범주로 나눔
  - 서로 다른 범주에 속한 컬렉션은 항상 같지 않음 → 예: `Set(1, 2, 3)`은 같은 원소를 담고 있더라도 `List(1, 2, 3)`과 같지 않음
  - 같은 범주 안에서는 두 컬렉션이 같은 원소를 가질 때 그리고 오직 그럴 때에만 서로 같음(시퀀스는 같은 원소가 같은 순서로 있어야 함)
    - 예: `List(1, 2, 3) == Vector(1, 2, 3)`, `HashSet(1, 2) == TreeSet(2, 1)`

참고 — 처음 배우는 분께
- 여기서 말하는 동등성은 `==` 연산자로 비교했을 때 `true`가 나오는지를 뜻함
- 스칼라의 `==`는 참조(주소) 비교가 아니라 `equals` 메서드를 호출하는 값 비교
- 컬렉션들은 "구체적인 구현 클래스가 무엇인가"가 아니라 "어떤 범주에 속하고 어떤 원소를 담고 있는가"를 기준으로 `equals`를 구현함

왜 필요한가
- 구현 클래스까지 같아야 동등하다고 정의하면, `List`를 성능상 이유로 `Vector`로 바꾸는 순간 기존 비교 코드가 전부 깨짐
- 범주 + 원소 기준의 동등성 덕분에 사용자는 구체 구현을 자유롭게 교체할 수 있고, 라이브러리 내부 구현이 바뀌어도 동작이 유지됨

- 동등성 검사에서 컬렉션이 가변(mutable)인지 불변(immutable)인지는 중요하지 않음
- 가변 컬렉션은 동등성 검사가 수행되는 시점의 현재 원소들만을 기준으로 판단 → 원소가 추가되거나 제거됨에 따라 시점마다 서로 다른 컬렉션과 같아질 수 있음
- 이 점은 가변 컬렉션을 해시맵(hashmap)의 키로 사용할 때 빠지기 쉬운 함정이 됨. 예시:

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

- 이 예제에서 마지막 줄의 조회는 십중팔구 실패함
- 원인: 바로 앞 줄에서 배열 버퍼 `buf`의 해시 코드(hash code)가 바뀜 → 해시 코드 기반 조회는 `buf`가 실제로 저장된 위치와는 다른 위치를 살펴보게 됨

주의 — 짚고 넘어가기
- 원문의 "most likely"(십중팔구)라는 표현에 주의 → 항상 실패한다는 뜻은 아님
- 바뀐 해시 코드가 우연히 원래 값과 같은 버킷(bucket)에 대응하면 조회가 성공할 수도 있음
- 즉 이 버그는 비결정적으로 나타나기 때문에 더 위험함
- 교훈: 해시 기반 자료구조의 키로는 불변 컬렉션을 사용하고, 가변 컬렉션을 키로 써야 한다면 키로 쓰는 동안 절대 변경하지 말 것
