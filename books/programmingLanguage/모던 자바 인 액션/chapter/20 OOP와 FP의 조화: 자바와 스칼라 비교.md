# 20 OOP와 FP의 조화: 자바와 스칼라 비교

## 스칼라 소개

### 스칼라의 특징

- ** 객체지향과 함수형의 조화**: 순수 객체지향과 함수형 프로그래밍의 특징을 모두 가짐
- **Java 플랫폼**: JVM에서 실행되며 Java와 상호운용 가능
- ** 간결한 문법**: Java보다 간결하면서도 표현력이 뛰어남
- ** 정적 타입**: 강력한 타입 시스템과 타입 추론 지원

### Hello World

```scala
object Hello {
  def main(args: Array[String]): Unit = {
    println("Hello, World!")
  }
}
```

## 함수형 프로그래밍

### 일급 함수 (First-Class Functions)

```scala
val add: (Int, Int) => Int = (x, y) => x + y
val multiply = (x: Int, y: Int) => x * y
```

### 불변성 (Immutability)

```scala
// 불변 변수
val x = 10  // 재할당 불가

// 가변 변수
var y = 20  // 재할당 가능
```

### 고차 함수

```scala
def applyOperation(x: Int, y: Int, op: (Int, Int) => Int): Int = {
  op(x, y)
}

applyOperation(5, 3, add)  // 8
applyOperation(5, 3, multiply)  // 15
```

## 컬렉션

### 불변 컬렉션

```scala
val numbers = List(1, 2, 3, 4, 5)
val doubled = numbers.map(_ * 2)
val evens = numbers.filter(_ % 2 == 0)
```

### 컬렉션 변환

```scala
// map
val names = List("Alice", "Bob", "Charlie")
val lengths = names.map(_.length)

// flatMap
val nested = List(List(1, 2), List(3, 4))
val flattened = nested.flatMap(identity)

// reduce
val sum = numbers.reduce(_ + _)
```

## 트레이트 (Trait)

### 트레이트 정의

```scala
trait Sized {
  var size: Int = 0
  def isEmpty = size == 0
}

class Box extends Sized

val box = new Box
box.size = 10
box.isEmpty  // false
```

### 믹스인 (Mixin)

```scala
trait Logging {
  def log(message: String): Unit = {
    println(s"LOG: $message")
  }
}

class Service extends Logging {
  def doSomething(): Unit = {
    log("Doing something")
  }
}
```

### 다중 트레이트

```scala
trait A {
  def hello = "A"
}

trait B {
  def hello = "B"
}

class C extends A with B {
  override def hello = "C: " + super.hello
}
```

## 패턴 매칭

### 기본 패턴 매칭

```scala
def matchTest(x: Any): String = x match {
  case 1 => "one"
  case "two" => "string two"
  case y: Int => s"scala int: $y"
  case _ => "many"
}
```

### case 클래스와 패턴 매칭

```scala
sealed trait Expr
case class Number(n: Int) extends Expr
case class Sum(e1: Expr, e2: Expr) extends Expr
case class Multiply(e1: Expr, e2: Expr) extends Expr

def eval(expr: Expr): Int = expr match {
  case Number(n) => n
  case Sum(e1, e2) => eval(e1) + eval(e2)
  case Multiply(e1, e2) => eval(e1) * eval(e2)
}
```

### Option 패턴 매칭

```scala
def toInt(s: String): Option[Int] = {
  try {
    Some(Integer.parseInt(s.trim))
  } catch {
    case e: Exception => None
  }
}

toInt("123") match {
  case Some(n) => println(n)
  case None => println("Not a number")
}
```

## 클래스와 객체

### case 클래스

```scala
case class Person(name: String, age: Int)

val alice = Person("Alice", 25)
val bob = alice.copy(name = "Bob")  // 불변 복사

// 자동 생성되는 기능들
// - equals, hashCode
// - toString
// - copy
// - apply, unapply (패턴 매칭)
```

### 컴패니언 객체 (Companion Object)

```scala
class Circle(val radius: Double) {
  def area = Circle.pi * radius * radius
}

object Circle {
  private val pi = 3.14159

  def apply(radius: Double) = new Circle(radius)
}

val circle = Circle(5.0)  // apply 메서드 호출
```

## 함수형 프로그래밍 패턴

### 커링 (Currying)

```scala
def multiply(x: Int)(y: Int): Int = x * y

val double = multiply(2)_
double(5)  // 10
```

### 부분 적용 함수

```scala
def sum(a: Int, b: Int, c: Int): Int = a + b + c

val addOne = sum(1, _: Int, _: Int)
addOne(2, 3)  // 6
```

## 자바와 스칼라 비교

### 간결성

**Java:**

```java
public class Person {
    private final String name;
    private final int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() { return name; }
    public int getAge() { return age; }

    @Override
    public boolean equals(Object o) { /* ... */ }
    @Override
    public int hashCode() { /* ... */ }
    @Override
    public String toString() { /* ... */ }
}
```

**Scala:**

```scala
case class Person(name: String, age: Int)
```

### 함수형 프로그래밍

**Java:**

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
List<Integer> doubled = numbers.stream()
    .map(n -> n * 2)
    .collect(Collectors.toList());
```

**Scala:**

```scala
val numbers = List(1, 2, 3, 4, 5)
val doubled = numbers.map(_ * 2)
```

## 타입 시스템

### 타입 추론

```scala
val name = "Alice"  // String으로 추론
val age = 25        // Int로 추론

def add(x: Int, y: Int) = x + y  // 반환 타입 Int로 추론
```

### 제네릭

```scala
class Stack[T] {
  private var elements: List[T] = Nil

  def push(x: T): Unit = {
    elements = x :: elements
  }

  def pop(): T = {
    val result = elements.head
    elements = elements.tail
    result
  }
}
```

## for 표현식

### for 컴프리헨션

```scala
// 기본 사용
for (i <- 1 to 10) println(i)

// 조건 추가
for (i <- 1 to 10 if i % 2 == 0) println(i)

// 결과 생성
val doubled = for (i <- 1 to 10) yield i * 2

// 중첩 루프
val pairs = for {
  x <- 1 to 3
  y <- 1 to 3
} yield (x, y)
```

## 교훈

1. ** 스칼라는 OOP와 FP를 자연스럽게 결합한다**
2. ** 불변성을 기본으로 하되 필요시 가변성 사용**
3. **case 클래스로 간결한 도메인 모델 작성**
4. ** 패턴 매칭으로 조건 로직을 선언적으로 표현**
5. ** 트레이트를 활용한 믹스인 합성**
6. ** 타입 시스템의 강력함과 추론의 편리함**
7. **Java와의 상호운용성 활용**
8. ** 함수형 프로그래밍 기법을 자연스럽게 적용**
