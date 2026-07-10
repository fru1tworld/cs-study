# Java와 Scala 컬렉션 간 변환 (Conversions Between Java and Scala Collections)

> 원문: <https://docs.scala-lang.org/overviews/collections-2.13/conversions-between-java-and-scala-collections.html>

Scala처럼 Java에도 풍부한 컬렉션 라이브러리가 있습니다. 두 라이브러리 사이에는 비슷한 점이 많습니다. 예를 들어 두 라이브러리 모두 이터레이터(iterator), 이터러블(iterable), 집합(set), 맵(map), 시퀀스(sequence)를 갖추고 있습니다. 하지만 중요한 차이점도 있습니다. 특히 Scala 라이브러리는 불변(immutable) 컬렉션에 훨씬 더 큰 비중을 두며, 컬렉션을 새로운 컬렉션으로 변환하는 연산을 훨씬 많이 제공합니다.

때로는 한 컬렉션 프레임워크에서 다른 쪽으로 넘어가야 할 때가 있습니다. 예를 들어 기존 Java 컬렉션을 마치 Scala 컬렉션인 것처럼 다루고 싶을 수 있습니다. 또는 Scala 컬렉션을, 그에 대응하는 Java 컬렉션을 기대하는 Java 메서드에 넘기고 싶을 수도 있습니다. Scala가 [CollectionConverters](https://www.scala-lang.org/api/2.13.18/scala/jdk/CollectionConverters$.html) 객체에서 주요 컬렉션 타입 전부에 대한 암시적 변환(implicit conversion)을 제공하기 때문에, 이런 작업은 아주 쉽게 할 수 있습니다. 구체적으로는 다음 타입들 사이의 양방향 변환을 찾을 수 있습니다.

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

> 💡 **왜 필요한가** — Scala는 JVM 위에서 돌아가므로 실무에서는 Java 라이브러리(JDBC, 각종 클라이언트 SDK 등)와 섞여 쓰이는 일이 아주 흔합니다. 그런데 Java 라이브러리는 `java.util.List` 같은 Java 컬렉션을 주고받고, Scala 코드는 `List`, `Map` 같은 Scala 컬렉션을 씁니다. 이 변환 기능이 없다면 두 세계를 오갈 때마다 원소를 하나씩 옮겨 담는 코드를 직접 작성해야 합니다. `CollectionConverters`는 이 경계를 메서드 호출 한 번으로 넘게 해 줍니다.

이 변환들을 사용하려면 [CollectionConverters](https://www.scala-lang.org/api/2.13.18/scala/jdk/CollectionConverters$.html) 객체에서 임포트하면 됩니다.

**Scala 2:**

```scala
scala> import scala.jdk.CollectionConverters._
import scala.jdk.CollectionConverters._
```

**Scala 3:**

```scala
scala> import scala.jdk.CollectionConverters.*
import scala.jdk.CollectionConverters.*
```

이렇게 하면 `asScala`와 `asJava`라는 확장 메서드(extension method)를 통해 Scala 컬렉션과 그에 대응하는 Java 컬렉션 사이의 변환이 가능해집니다.

**Scala 2:**

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

**Scala 3:**

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

내부적으로 이 변환들은 모든 연산을 원래의 컬렉션 객체에 그대로 전달하는 "래퍼(wrapper)" 객체를 만드는 방식으로 동작합니다. 따라서 Java와 Scala 사이를 변환할 때 컬렉션이 복사되는 일은 결코 없습니다. 흥미로운 성질 하나는, 예를 들어 어떤 Java 타입을 그에 대응하는 Scala 타입으로 변환했다가 다시 원래의 Java 타입으로 되돌리는 왕복 변환을 하면, 처음 시작했던 것과 동일한(identical) 컬렉션 객체를 얻게 된다는 점입니다.

> 📘 **처음 배우는 분께** — 래퍼 방식이란 데이터를 새 그릇에 옮겨 담는 것이 아니라, 기존 컬렉션을 감싸서 다른 인터페이스로 보이게 하는 얇은 껍데기를 하나 씌우는 것입니다. 그래서 변환 비용은 원소 개수와 무관하게 O(1)이고, 래퍼를 통해 값을 수정하면 원본 컬렉션에도 그대로 반영됩니다. 두 객체가 같은 데이터를 공유하고 있기 때문입니다.

특정한 다른 Scala 컬렉션들도 Java 쪽으로 변환할 수는 있지만, 원래의 Scala 타입으로 되돌아오는 변환은 제공되지 않습니다.

```
Seq           =>    java.util.List
mutable.Seq   =>    java.util.List
Set           =>    java.util.Set
Map           =>    java.util.Map
```

Java는 타입 수준에서 가변(mutable) 컬렉션과 불변 컬렉션을 구분하지 않기 때문에, 예를 들어 `scala.immutable.List`를 변환하면 `java.util.List`가 만들어지는데, 이 리스트에서는 모든 변경 연산이 "UnsupportedOperationException"을 던집니다. 다음은 그 예입니다.

**Scala 2와 3:**

```scala
scala> val jul = List(1, 2, 3).asJava
val jul: java.util.List[Int] = [1, 2, 3]

scala> jul.add(7)
java.lang.UnsupportedOperationException
    at java.util.AbstractList.add(AbstractList.java:148)
```

> ⚠️ **짚고 넘어가기** — `asJava`가 성공적으로 `java.util.List`를 돌려줬다고 해서 그 리스트를 마음대로 수정할 수 있다는 뜻이 아닙니다. 타입은 `java.util.List`지만 실체는 여전히 불변 Scala 리스트를 감싼 래퍼이므로, `add`나 `remove` 같은 변경 연산은 컴파일 시점이 아니라 **실행 시점**에 예외로 실패합니다. 불변 Scala 컬렉션을 Java 코드에 넘길 때는 상대 코드가 컬렉션을 수정하려 들지 않는지 확인해야 합니다. 수정이 필요하다면 처음부터 가변 컬렉션(`mutable.Buffer` 등)을 변환해서 넘기는 것이 안전합니다.
