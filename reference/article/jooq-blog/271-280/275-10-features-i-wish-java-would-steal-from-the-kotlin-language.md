# Java가 Kotlin 언어에서 훔쳤으면 하는 10가지 기능

> 원문: https://blog.jooq.org/10-features-i-wish-java-would-steal-from-the-kotlin-language/

저는 유니콘을 바라지 않을 것입니다. 하지만 큰 위험 없이 Java 언어에 도입될 수 있는 몇 가지 쉽게 따먹을 수 있는 과일들이 있습니다.

## 기능 1: 데이터 클래스

개념: 데이터 클래스는 hashCode(), equals(), toString() 메서드와 같은 보일러플레이트 코드를 자동으로 생성합니다.

Java의 문제점:
```java
public class Person {
    final String firstName;
    final String lastName;
    public JavaPerson(...) { ... }
    // Getter, Hashcode/equals, toString이 필요함
}
```

Kotlin의 해결책:
```kotlin
data class Person(
  val firstName: String,
  val lastName: String
)
```

핵심 이점: "데이터 클래스는 데이터를 저장하는 데 사용되기 때문에... hashCode(), equals(), toString()과 같은 것들의 구현은 명백하며 기본적으로 제공될 수 있습니다."

추가 기능 - 구조 분해:
```kotlin
val jon = Person("Jon", "Doe")
val (firstName, lastName) = jon
```

---

## 기능 2: 기본 매개변수

개념: 함수 매개변수에 기본값을 가질 수 있어서, 여러 개의 오버로드된 메서드가 필요 없게 됩니다.

Java의 현재 상태:
```java
interface Stream<T> {
    Stream<T> sorted();
    Stream<T> sorted(Comparator<? super T> comparator);
}
```

Kotlin의 접근 방식:
```kotlin
fun sorted(comparator : Comparator<T>
         = Comparator.naturalOrder()) : Stream<T>
```

복잡한 예제:
```kotlin
fun reformat(str: String,
             normalizeCase: Boolean = true,
             upperCaseFirstLetter: Boolean = true,
             divideByCamelHumps: Boolean = false,
             wordSeparator: Char = ' ') {
...
}
```

다양한 호출 방식:
```kotlin
reformat(str)
reformat(str, true, true, false, '_')
reformat(str,
  normalizeCase = true,
  upperCaseFirstLetter = true,
  divideByCamelHumps = false,
  wordSeparator = '_'
)
```

장점: "명명된 매개변수는... 특히 인덱스가 아닌 이름으로 인자를 전달할 때 유용합니다."

---

## 기능 3: 단순화된 instanceof 검사 (When 표현식)

개념: when 표현식을 통한 패턴 매칭으로, 문장이 아닌 표현식으로 할당할 수 있습니다.

기본 예제:
```kotlin
val hasPrefix = when(x) {
  is String -> x.startsWith("prefix")
  else -> false
}
```

범위 검사:
```kotlin
when (x) {
  in 1..10 -> print("x는 범위 내에 있습니다")
  in validNumbers -> print("x는 유효합니다")
  !in 10..20 -> print("x는 범위 밖에 있습니다")
  else -> print("위의 어느 것도 아닙니다")
}
```

SQL과의 비교: 저자는 Kotlin의 when 표현식이 SQL CASE 문과 강력함과 표현력에서 유사하다고 언급합니다.

---

## 기능 4: Map 키/값 순회

개념: 장황한 구문 없이 for 루프에서 직접 맵 항목을 구조 분해할 수 있습니다.

Kotlin 구문:
```kotlin
val map: Map<String, Int> = ...

for ((k, v) in map) {
    ...
}
```

현재 Java의 대안:
```java
map.forEach((k, v) -> {
    ...
});
```

제안: "`Map<K, V>`가 `Iterable<Entry<K, V>>`를 확장하여" 순회를 더 직관적으로 만들 것.

---

## 기능 5: Map 접근 리터럴

개념: 배열 구문을 미러링하여 맵 접근에 대괄호 구문을 사용합니다.

Kotlin 구현:
```kotlin
val map = hashMapOf<String, Int>()
map.put("a", 1)
println(map["a"])
```

작동 원리: "`x[y]`는 `x.get(y)`로 지원되는 메서드 호출의 구문 설탕일 뿐입니다."

jOOQ를 활용한 실제 응용:
```kotlin
ctx.select(a.FIRST_NAME, a.LAST_NAME, b.TITLE)
   .from(a)
   .join(b).on(a.ID.eq(b.AUTHOR_ID))
   .orderBy(1, 2, 3)
   .forEach {
       println("""${it[b.TITLE]}
               by ${it[a.FIRST_NAME]} ${it[a.LAST_NAME]}""")
   }
```

---

## 기능 6: 확장 함수

개념: 상속이나 래퍼 클래스 없이 기존 타입에 메서드를 추가합니다.

기본 구문:
```kotlin
fun MutableList<Int>.swap(index1: Int, index2: Int) {
  val tmp = this[index1]
  this[index1] = this[index2]
  this[index2] = tmp
}

val l = mutableListOf(1, 2, 3)
l.swap(0, 2)
```

라이브러리 응용 예제 (jOOλ):
```kotlin
list.stream()
    .zipWithIndex()
    .forEach(System.out::println);
```

주의 사항: 저자는 논란을 인정합니다: "확장 함수는 실제로 단지 설탕 처리된 정적 메서드일 뿐입니다. 이는 객체 지향 애플리케이션 설계에 상당한 위험이 됩니다."

---

## 기능 7: 안전 호출 연산자와 엘비스 연산자

개념: 명시적인 null 검사나 Optional 래핑 없이 null-안전 탐색을 제공합니다.

안전 호출 연산자 (`?.`):
```kotlin
String name = bob?.department?.head?.name
```

동등한 Java (Optional 접근 방식 - 장황함):
```java
Optional<String> name = bob
    .flatMap(Person::getDepartment)
    .map(Department::getHead)
    .flatMap(Person::getName);
```

디슈가링된 Java 동등 코드:
```java
String name = null;
if (bob != null) {
    Department d = bob.department
    if (d != null) {
        Person h = d.head;
        if (h != null)
            name = h.name;
    }
}
```

저자의 입장: "저는 Kotlin의 이런 종류의 실용주의가 정말 좋습니다... 일상 업무로 돌아가기 전에 간단한 null 안전 연산자."

엘비스 연산자 (`?:`): 왼쪽이 null일 때 기본값을 제공합니다.

---

## 기능 8: 모든 것이 표현식

개념: 값을 반환하는 문장(if/else, when, try/catch)을 변수에 할당할 수 있습니다.

If-Else 표현식:
```kotlin
val max = if (a > b) a else b
```

표현식으로서의 When:
```kotlin
val hasPrefix = when(x) {
  is String -> x.startsWith("prefix")
  else -> false
}
```

표현식으로서의 Try:
```kotlin
val result = try {
    count()
} catch (e: ArithmeticException) {
    throw IllegalStateException(e)
}
```

비교: "이것이 다음의 동등한 것보다 훨씬 더 유용하지 않습니까?" (Java의 if-else 문장 버전이 열등한 것으로 표시됨)

---

## 기능 9: 단일 표현식 함수

개념: 간단한 구현을 가진 함수는 명시적인 본문 블록 없이 간결한 구문을 사용할 수 있습니다.

애노테이션 예제 (현재 Java):
```java
public @interface AliasFor {
    @AliasFor("attribute")
    String value() default "";
    @AliasFor("value")
    String attribute() default "";
}
```

제안된 단순화된 구문:
```java
public @interface AliasFor {
    String value() = "";
    String attribute() = "";
}

// 또는 인터페이스의 경우:
public interface AliasFor {
    String value() = "";
    String attribute() = "";
}
```

참고: 저자는 Java의 기존 구문 제약을 감안할 때 이것이 비실용적일 수 있음을 인정합니다.

---

## 기능 10: 흐름 민감 타이핑

개념: 명시적 캐스팅 없이 제어 흐름 구조 내에서 타입 축소가 자동으로 인식됩니다.

Kotlin 예제:
```kotlin
when (x) {
    is String -> println(x.length)
}
```

Java 동등 코드 (캐스팅 필요):
```java
if (x instanceof String)
    System.out.println(((String) x).length());
```

응용: "결국, 우리는 이미 Java 8 이후로 흐름 민감 final 지역 변수를 갖고 있습니다."

관련 개념: 예외 처리를 통한 합 타입 (Java 7+):
```java
try {
    ...
}
catch (IOException | SQLException e) {
    // e는 IOException 타입이거나 SQLException 타입일 수 있습니다
}
```

---

## 기능 11 (보너스): 선언 위치 변성

개념: 제네릭 타입 변성이 사용 위치가 아닌 타입 정의 시점에 선언됩니다.

Kotlin/C# 접근 방식:
```kotlin
interface IEnumerable<out T>
```

이점: 장황한 와일드카드 구문을 제거합니다.

현재 Java의 문제점:
```java
Iterable<String> strings = Arrays.asList("abc");
Iterable<Object> objects = strings; // 컴파일 에러
```

복잡한 제네릭 시그니처 (현재 Java):
```java
<R> Stream<R> flatMap(
  Function<? super T, ? extends Stream<? extends R>> mapper
);
```

제안된 단순화된 버전:
```java
interface Function<in T, out R> {}
interface Stream<out T> {}
```

저자의 결론: "이것은 Java의 (가까운) 미래 버전에서 논의되고 있습니다."

---

## 전반적인 주제

기사 전반에 걸쳐 저자는 다음과 같이 주장합니다: "저는 유니콘을 바라지 않을 것입니다. 하지만 큰 위험 없이 Java 언어에 도입될 수 있는 몇 가지 쉽게 따먹을 수 있는 과일들이 있습니다."
