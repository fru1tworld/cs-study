# Scala를 며칠 사용한 후 Java로 돌아올 때 가장 짜증나는 10가지

> 원문: https://blog.jooq.org/the-10-most-annoying-things-coming-back-to-java-after-some-days-of-scala/

파서 개발을 위해 Scala를 실험한 후, 저는 Scala Parsers API가 매우 적합하다는 것을 발견했습니다. 추가적인 런타임 의존성이 있지만, 파서를 Java 인터페이스 뒤에 래핑할 수 있기 때문에 상호 운용성 문제는 최소화됩니다. Scala 문법에 며칠간 적응한 후, Java 개발로 돌아왔을 때 가장 그리웠던 10가지 기능을 정리해 보았습니다.

## 1. 멀티라인 문자열

이 기능은 연결 연산 없이도 SQL 문과 문자열 콘텐츠를 가독성 있게 작성할 수 있게 해줍니다. Scala는 삼중 따옴표 문자열을 지원하며, 선택적으로 `s` 접두사를 사용한 문자열 보간으로 변수를 주입할 수 있습니다.

```scala
println ("""Dear reader,

If we had this feature in Java,
wouldn't that be great?

Yours Sincerely,
Lukas""")
```

보간을 사용한 예:

```scala
val predicate =
  if (someCondition)
    "AND a.id = 1"
  else
    ""

println(
  DSL.using(configuration)
      .fetch(s"""
            SELECT a.first_name, a.last_name, b.title
            FROM author a
            JOIN book b ON a.id = b.author_id
            WHERE 1 = 1 $predicate
            ORDER BY a.id, b.id
            """)
)
```

## 2. 세미콜론

Scala는 타입 안전성과 구조화된 코드 포매팅 규칙 덕분에 줄 끝에 세미콜론이 필요하지 않습니다. 언어 설계가 세미콜론 추론을 신뢰성 있게 만들어줍니다.

```scala
val a = thisIs.soMuchBetter()
val b = no.semiColons()
val c = at.theEndOfALine()
```

## 3. 괄호

인자가 없는 메서드는 괄호 없이 호출할 수 있습니다. 이는 괄호 없는 호출이 부수 효과가 없음을 나타내는 규칙을 따르며, 프로퍼티 접근과 유사합니다.

```scala
val s = myObject.toString
```

## 4. 타입 추론

Scala는 Java의 제한된 기능에 비해 훨씬 뛰어난 타입 추론을 제공합니다. 명시적 타입 어노테이션 없이 변수를 선언할 수 있으며, 컴파일러가 문맥에서 타입을 결정합니다.

```scala
val s = myObject.toString

// 또는 명시적 타입 지정:
val s : String = myObject.toString
```

## 5. 케이스 클래스

케이스 클래스는 생성자, getter, setter, equals, hashCode, toString 메서드가 포함된 불변 POJO를 자동으로 생성합니다. 속성만 명시적으로 선언하면 됩니다.

```scala
case class Person(firstName: String, lastName: String)

Person("George", "Orwell")
```

저자는 Project Lombok과 같은 어노테이션 기반 코드 생성 도구를 비판하며, Scala가 어노테이션은 외부 도구보다 언어 기능으로 작동할 때 가장 효과적이라는 것을 보여준다고 지적합니다.

## 6. 어디서나 메서드(함수!)

함수는 패키지, 객체, 또는 다른 메서드 내부 로컬 등 어떤 스코프 레벨에서도 정의할 수 있습니다. 클래스 멤버로만 정의해야 하는 Java와 대조적입니다.

```scala
def m1(i : Int) = i + 1

object Test {
    def m2(i : Int) = i + 2

    def main(args: Array[String]): Unit = {
        def m3(i : Int) = i + 3

        println(m1(1))
        println(m2(1))
        println(m3(1))
    }
}
```

## 7. REPL

Scala의 Read-Eval-Print Loop는 테스트 클래스나 main 메서드 없이도 알고리즘과 개념을 대화형으로 테스트할 수 있게 해줍니다. println 같은 함수는 항상 사용 가능합니다.

```scala
println(SomeOtherClass.testMethod)
```

## 8. 배열이 (그렇게) 특별한 경우가 아님

Scala에서 배열은 map과 같은 메서드를 사용할 수 있는 일반 컬렉션처럼 동작합니다. Java 배열을 괴롭히는 이상한 타입 공변성과 초기화 규칙이 없습니다.

```scala
val a = new Array[String](3);
a(0) = "A"
a(1) = "B"
a(2) = "C"
a.map(v => v + ":")

// 출력 Array(A:, B:, C:)
```

## 9. 심볼릭 메서드 이름

메서드는 `*`나 `||` 같은 연산자 심볼을 사용할 수 있어서, 수학 및 문자열 연산에 직관적인 문법을 가능하게 합니다. 이는 특히 SQL 표현식에서 읽기 쉬운 DSL 구현을 용이하게 합니다.

```scala
val x = BigDecimal(3);
val y = BigDecimal(4);
val z = x * y
```

Scala에서 jOOQ 사용 예:

```scala
select (
  AUTHOR.FIRST_NAME || " " || AUTHOR.LAST_NAME,
  AUTHOR.AGE - 10
)
from AUTHOR
where AUTHOR.ID > 10
fetch
```

주의: 연산자 오버로딩은 라이브러리에 의해 남용될 위험이 있으며 코드의 의미를 모호하게 만들 수 있습니다.

## 10. 튜플

Scala는 타입이 지정되고 인덱싱된 요소를 가진 익명 튜플 타입을 지원합니다. 이는 커스텀 제네릭 튜플 클래스를 작성해야 하는 Java와 대조적입니다. 튜플은 SQL row-value 표현식 의미론을 가능하게 합니다.

```scala
val t1 = (1, "A")

val t2 = (1, "A", (2, "B"))
```

장황한 보일러플레이트가 필요한 Java 동등 코드:

```java
Tuple2<Integer, String> t1 = new Tuple2<>(1, "A");

Tuple3<Integer, String, Tuple2<Integer, String>> t2 =
    new Tuple3<>(1, "A", new Tuple2<>(2, "B"));
```

row value 표현식을 활용한 jOOQ 예:

```java
DSL.using(configuration)
   .select(T1.SOME_VALUE)
   .from(T1)
   .where(
      row(T1.COL1, T1.COL2)
      .in(select(T2.A, T2.B).from(T2))
   )
   .fetch();
```

## 결론

Scala가 상당한 장점을 제공하지만, 완전한 마이그레이션은 권장하지 않습니다. 이 언어에는 "결국 당신의 얼굴에 터질" 기능들이 포함되어 있는데, 암시적 변환, 로컬 임포트, 심볼 위주의 API 등이 그렇습니다. 하지만 나열된 대부분의 장점들은 최소한의 하위 호환성 위험과 상당한 생산성 향상을 통해 Java에서 구현 가능합니다. Java 9에서 제안된 기능들—값 타입, 선언 위치 변성, 특수화—은 현대화에 대한 약속을 제공하며, 일상적인 개발 워크플로우를 향상시키는 실용적인 개선을 위한 여지를 만들어줍니다.
