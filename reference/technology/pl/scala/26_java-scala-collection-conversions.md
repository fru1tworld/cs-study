# Java와 Scala 컬렉션 간 변환

> **원문:** https://docs.scala-lang.org/overviews/collections-2.13/conversions-between-java-and-scala-collections.html

---

## 목차

1. [왜 변환이 필요한가](#1-왜-변환이-필요한가)
2. [`CollectionConverters` — `asJava` / `asScala`](#2-collectionconverters--asjava--asscala)
3. [양방향 변환 쌍](#3-양방향-변환-쌍)
4. [단방향 변환 (Java로만 가는 변환)](#4-단방향-변환-java로만-가는-변환)
5. [변환의 동작 원리 — 래퍼(wrapper)이지 복사가 아니다](#5-변환의-동작-원리--래퍼wrapper이지-복사가-아니다)
6. [주의할 점 — 불변 컬렉션을 Java로 넘길 때](#6-주의할-점--불변-컬렉션을-java로-넘길-때)
7. [정리](#7-정리)

---

## 1. 왜 변환이 필요한가

Scala는 JVM 위에서 동작하며 Java 라이브러리와 자유롭게 상호운용(interop)할 수 있다는 것이 큰 장점입니다. 하지만 컬렉션(collection)만큼은 Java와 Scala가 **서로 다른 타입 체계**를 갖고 있습니다.

- Java 컬렉션(`java.util.List`, `java.util.Map` 등)은 기본적으로 **가변(mutable)** 이며, 인터페이스 하나에 가변/불변 구분이 없습니다.
- Scala 컬렉션은 `scala.collection.immutable`과 `scala.collection.mutable`로 **가변성을 타입으로 구분**합니다.

그래서 Java API를 호출하거나 Java 라이브러리에 값을 넘길 때, 예를 들어 Scala의 `List`를 Java 메서드가 요구하는 `java.util.List`로 바꿔야 하는 상황이 자주 생깁니다. 매번 수동으로 순회하며 새 컬렉션을 만드는 건 번거로우므로, 표준 라이브러리가 이 변환을 **확장 메서드(extension method) 형태**로 제공합니다.

> 📘 **처음 배우는 분께 — 확장 메서드로 제공된다는 것**
>
> `asJava`, `asScala`는 Java나 Scala 컬렉션 타입 자체에 원래 있는 메서드가 아닙니다. 특정 객체를 `import`하면 마치 그 타입에 원래 있던 메서드처럼 호출할 수 있게 되는 **암시적 확장(implicit extension)** 입니다. Scala 2의 `implicit class`, Scala 3의 `extension`이 이 메커니즘을 뒷받침합니다(자세한 내용은 `03_contextual_extensions_typeclasses.md` 참고).

---

## 2. `CollectionConverters` — `asJava` / `asScala`

변환 기능은 `scala.jdk.CollectionConverters` 객체에 모여 있습니다.

```scala
// Scala 2.13
import scala.jdk.CollectionConverters._

// Scala 3
import scala.jdk.CollectionConverters.*
```

이 객체를 임포트하면 두 개의 확장 메서드가 활성화됩니다.

| 메서드 | 방향 | 설명 |
|---|---|---|
| `.asJava` | Scala → Java | Scala 컬렉션을 대응하는 Java 컬렉션 인터페이스로 바꿔줍니다 |
| `.asScala` | Java → Scala | Java 컬렉션을 대응하는 Scala 컬렉션 타입으로 바꿔줍니다 |

```scala
import scala.jdk.CollectionConverters.*
import scala.collection.mutable

val buf: mutable.Buffer[Int] = mutable.ArrayBuffer(1, 2, 3)
val javaList: java.util.List[Int] = buf.asJava   // Scala -> Java

val fromJava: mutable.Buffer[Int] = javaList.asScala   // Java -> Scala, 다시 원래 타입으로
```

> ⚠️ **짚고 넘어가기 — 옛 이름 `JavaConverters`, `JavaConversions`**
>
> Scala 2.13 이전에는 `scala.collection.JavaConverters`(명시적 `.asJava`/`.asScala` 방식)와, 자동으로 암시적 변환이 걸리던 `scala.collection.JavaConversions`(지금은 제거됨) 두 가지가 있었습니다. 현재는 `scala.jdk.CollectionConverters` 하나로 통합되었고, 명시적으로 `.asJava`/`.asScala`를 호출하는 방식만 남았습니다. 오래된 코드나 라이브러리에서 `JavaConverters`를 임포트하는 걸 봐도 놀라지 마세요 — 같은 역할의 옛 이름입니다.

---

## 3. 양방향 변환 쌍

다음 타입들은 **양쪽 어느 방향으로 변환해도 원래 타입으로 되돌아오는** 쌍입니다. 즉 `x.asJava.asScala`가 다시 원래와 같은 종류의 컬렉션이 됩니다.

| Scala 타입 | Java 타입 |
|---|---|
| `Iterator` | `java.util.Iterator` |
| `Iterator` | `java.util.Enumeration` |
| `Iterable` | `java.lang.Iterable` |
| `Iterable` | `java.util.Collection` |
| `mutable.Buffer` | `java.util.List` |
| `mutable.Set` | `java.util.Set` |
| `mutable.Map` | `java.util.Map` |
| `mutable.ConcurrentMap` | `java.util.concurrent.ConcurrentMap` |

공통점은 왼쪽이 전부 **가변(mutable) 컬렉션이거나, 가변성을 따지지 않는 최상위 트레이트**(`Iterator`, `Iterable`)라는 것입니다. Java 컬렉션은 원래 가변이 기본이므로, Scala의 가변 컬렉션과 자연스럽게 짝을 이룹니다.

```scala
import scala.jdk.CollectionConverters.*

val jMap = new java.util.HashMap[String, Int]()
jMap.put("a", 1)

val sMap: scala.collection.mutable.Map[String, Int] = jMap.asScala
sMap("b") = 2          // Scala 쪽에서 수정하면
jMap.get("b")           // Java 쪽 원본에도 반영됨 (2)
```

---

## 4. 단방향 변환 (Java로만 가는 변환)

아래 타입들은 Java 쪽으로 감쌀 수는 있지만, 대응하는 왕복 쌍이 없어서 한쪽 방향(one-way)으로만 씁니다.

- `Seq`, `Set`, `Map` 같은 **불변(immutable) 컬렉션**은 Java 타입 체계가 "수정 불가"를 표현하지 못하므로 애초에 되돌아오는 쌍을 정의하지 않습니다.
- `mutable.Seq`는 가변이긴 하지만 원소 추가/삭제(`add`/`remove`) 연산이 없는 고정 크기 컬렉션이라, `java.util.List`가 요구하는 연산을 전부 지원하지 못합니다. (반면 3번 표의 `mutable.Buffer`는 크기 변경까지 지원하므로 양방향 쌍이 됩니다.)

| Scala 타입 | Java 타입 |
|---|---|
| `Seq` | `java.util.List` |
| `mutable.Seq` | `java.util.List` |
| `Set` | `java.util.Set` |
| `Map` | `java.util.Map` |

```scala
import scala.jdk.CollectionConverters.*

val immutableList: List[Int] = List(1, 2, 3)
val jList: java.util.List[Int] = immutableList.asJava   // Scala 불변 -> Java

jList.add(4)   // 컴파일은 되지만 실행 시 예외!
```

> 💡 **왜 필요한가 — 그런데 왜 굳이 `asJava`를 열어두나**
>
> 대부분의 Java API는 인자로 `java.util.List` 등 인터페이스 타입만 요구할 뿐, 그 리스트를 수정하지는 않습니다(읽기 전용 용도로만 순회하는 경우가 대부분). 이런 상황에서는 Scala의 불변 `List`를 감싸서 넘겨주는 것만으로 충분합니다. 문제는 Java 쪽 코드가 실수로(혹은 의도적으로) `add`나 `remove`를 호출할 때 발생합니다 — 아래 6번 항목에서 다룹니다.

---

## 5. 변환의 동작 원리 — 래퍼(wrapper)이지 복사가 아니다

`asJava`/`asScala`는 **데이터를 복사하지 않습니다.** 대신 원본 컬렉션을 감싸는 **래퍼 객체**(wrapper object)를 하나 만들어서, 모든 연산을 그 안의 원본 컬렉션으로 그대로 전달(forward)합니다.

- 래퍼를 통해 원소를 추가/삭제하면 **원본 컬렉션에도 즉시 반영**됩니다(3번 예시가 이를 보여줍니다).
- 같은 이유로, **왕복 변환(round-trip)은 원본 객체를 그대로 돌려줍니다.** 예를 들어 `mutable.Buffer`를 `.asJava.asScala`로 왕복시키면 매번 새 복사본이 생기는 게 아니라 원래 그 `Buffer` 인스턴스를 다시 얻습니다.

```scala
import scala.jdk.CollectionConverters.*

val original = scala.collection.mutable.Buffer(1, 2, 3)
val roundTrip = original.asJava.asScala

roundTrip eq original   // true — 새 컬렉션이 아니라 같은 객체
```

> 📘 **처음 배우는 분께 — "래퍼"라는 게 뭘 뜻하나**
>
> `asJava`를 호출해서 얻은 `java.util.List`는 사실 원본 Scala 컬렉션을 감싸고 있는 얇은 껍데기입니다. 그 껍데기의 `add`, `get`, `size` 같은 메서드 구현부는 전부 "원본 Scala 컬렉션의 대응 연산을 호출해라"라고만 되어 있습니다. 그래서 두 "겉모습"(Java 쪽 `List`, Scala 쪽 `Buffer`)은 사실 하나의 데이터를 공유하는 서로 다른 창(window)일 뿐입니다.

---

## 6. 주의할 점 — 불변 컬렉션을 Java로 넘길 때

Scala의 `scala.collection.immutable.List` 같은 **불변 컬렉션**을 `.asJava`로 감싸서 Java 코드에 넘기면, 그 Java 쪽 `java.util.List`는 **컴파일 시점에는 평범한 가변 리스트처럼 보입니다.** Java 타입 체계에는 "이건 수정 불가"를 표현할 방법이 없기 때문입니다.

- `add`, `remove`, `set` 같은 변경 연산을 호출하면 컴파일은 통과하지만, **런타임에 `UnsupportedOperationException`이 발생**합니다.
- 이는 버그가 아니라 **의도된 안전장치**입니다 — 원본이 불변이라는 계약을 실제로 어기는 순간 예외로 막아 주는 것입니다.

```scala
import scala.jdk.CollectionConverters.*

val wrapped: java.util.List[Int] = List(1, 2, 3).asJava

wrapped.get(0)     // 1 — 읽기는 문제 없음
wrapped.add(4)     // java.lang.UnsupportedOperationException
```

> ⚠️ **짚고 넘어가기 — Java 쪽 개발자에게 미리 알려야 하는 이유**
>
> 이 예외는 코드를 실행하기 전까지는 드러나지 않습니다. Scala 쪽에서 불변 컬렉션을 Java API에 넘길 때는, 그 Java 코드가 리스트를 **읽기 전용으로만 쓰는지** 미리 확인하는 게 안전합니다. 반대로 Java 쪽에서 정말 값을 채워 넣어야 한다면, Scala 쪽 가변 컬렉션(`mutable.Buffer` 등, 3번 표)으로 넘기거나, `.asJava` 결과를 감싼 뒤 Java 쪽에서 방어적으로 복사(`new ArrayList<>(wrapped)`)해서 쓰는 편이 낫습니다.

---

## 7. 정리

- 변환은 `scala.jdk.CollectionConverters`를 임포트한 뒤 `.asJava` / `.asScala`를 호출하는 것으로 끝입니다.
- `Iterator`, `Iterable`과 **가변** 컬렉션(`Buffer`, `Set`, `Map`, `ConcurrentMap`)은 **양방향**으로 자연스럽게 왕복됩니다.
- **불변** 컬렉션(`Seq`, `Set`, `Map`)과 크기 변경이 안 되는 `mutable.Seq`는 Java 쪽으로 가는 **단방향**만 제공되며, 불변 컬렉션을 Java 쪽에서 수정하려 하면 `UnsupportedOperationException`이 발생합니다.
- 모든 변환은 **복사가 아니라 래퍼**이므로, 한쪽에서 가변 컬렉션을 수정하면 다른 쪽에도 즉시 반영됩니다.

---

## 참고 자료

- [Conversions Between Java and Scala Collections (공식 문서)](https://docs.scala-lang.org/overviews/collections-2.13/conversions-between-java-and-scala-collections.html)
- [Scala Collections 개요](https://docs.scala-lang.org/overviews/collections-2.13/introduction.html)
