# 문자열 (Strings)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/strings.html>

배열(array)과 마찬가지로, 문자열(string)도 그 자체가 직접 시퀀스(sequence)인 것은 아니지만 시퀀스로 변환할 수 있으며, 문자열 위에서 모든 시퀀스 연산을 지원합니다. 다음은 문자열에 대해 호출할 수 있는 연산의 몇 가지 예입니다.

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

> 📘 **처음 배우는 분께** — 스칼라의 `String`은 사실 자바의 `java.lang.String` 그대로입니다. 자바의 문자열에는 `reverse`, `map`, `drop` 같은 컬렉션 메서드가 없는데도 위 예제가 동작하는 이유는, 스칼라가 문자열을 컬렉션처럼 다룰 수 있게 해 주는 **암시적 변환**(implicit conversion)을 제공하기 때문입니다.

이러한 연산은 두 가지 암시적 변환에 의해 지원됩니다. 첫 번째는 우선순위가 낮은 변환으로, `String`을 `immutable.IndexedSeq`의 서브클래스(subclass)인 `WrappedString`으로 매핑합니다. 위 예제의 마지막 줄에서 문자열이 `Seq`로 변환될 때 적용된 것이 바로 이 변환입니다. 다른 하나는 우선순위가 높은 변환으로, 문자열을 `StringOps` 객체로 매핑하며, 이 객체는 불변(immutable) 시퀀스의 모든 메서드를 문자열에 추가해 줍니다. 위 예제에서 `reverse`, `map`, `drop`, `slice`를 호출할 때 암시적으로 삽입된 것이 이 변환입니다.

> 💡 **왜 두 가지 변환이 필요한가** — 두 변환은 반환 타입이 다릅니다. `StringOps`를 거치는 고우선순위 변환은 결과를 다시 `String`으로 돌려주므로(`str.reverse`의 결과가 `String`인 것처럼) "문자열을 다뤘으니 문자열이 나온다"는 자연스러운 기대를 지켜 줍니다. 반면 `WrappedString`을 거치는 저우선순위 변환은 문자열을 진짜 `Seq[Char]` 값으로 취급해야 할 때, 즉 `Seq[Char]` 타입이 요구되는 자리에 문자열을 넘길 때 사용됩니다.

> ⚠️ **짚고 넘어가기** — 메서드 호출에는 `StringOps` 변환이, 타입이 `Seq[Char]`로 요구되는 자리에는 `WrappedString` 변환이 적용된다는 점이 핵심입니다. 두 변환이 충돌하지 않도록 우선순위가 나뉘어 있을 뿐, 사용자가 어느 쪽을 쓸지 직접 고를 일은 거의 없습니다.
