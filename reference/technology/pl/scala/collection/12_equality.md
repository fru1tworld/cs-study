# 동등성 (Equality)

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
