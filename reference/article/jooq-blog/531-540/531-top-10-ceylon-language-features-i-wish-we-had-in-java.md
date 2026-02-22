# Java에 있었으면 하는 Ceylon 언어 기능 Top 10

> 원문: https://blog.jooq.org/top-10-ceylon-language-features-i-wish-we-had-in-java/

이 글에서는 Red Hat이 2013년 11월 12일에 출시한 Ceylon 1.0.0의 흥미로운 기능들을 소개합니다. 필자는 Hibernate 창시자인 Gavin King, Kotlin 기여자인 Ross Tate, Scala 커미터이자 Google Dart 엔지니어인 Lukas Rytz 등 언어 전문가들과 상의하여 가장 흥미로운 Ceylon 기능들을 선정했습니다.

## 1. 모듈(Modules)

Ceylon의 모듈 시스템은 Java의 복잡한 Maven/OSGi 설정에 비해 의존성 관리를 크게 단순화합니다. 단 몇 줄의 코드만으로 패키지의 jar 수준 가시성을 제어할 수 있습니다.

Ceylon 예제:
```ceylon
"역대 두 번째로 좋은 ORM 솔루션!"
license "http://www.gnu.org/licenses/lgpl.html"
module org.hibernate "3.0.0.beta" {
    import ceylon.collection "1.0.0";
    import java.base "7";
    shared import java.jdbc "7";
}
```

## 2. 시퀀스(Sequences)

Ceylon은 리터럴 표기법과 타입 안전성을 갖춘 일급(first-class) 시퀀스를 지원합니다. `{String+}`는 비어 있지 않은 시퀀스를, `{String*}`는 비어 있을 수 있는 시퀀스를 나타내며, 범위 연산과 함께 배열/튜플 리터럴도 지원합니다.

예제:
```ceylon
{String+} words = { "hello", "world" };

String[] operators = [ "+", "-", "*", "/" ];
String? plus = operators[0];
String[] multiplicative = operators[2..3];

[Float,Float,String] point = [0.0, 0.0, "origin"];
```

## 3. Nullable 타입

타입에 `?` 구문을 사용하여 nullable로 표시할 수 있습니다. Ceylon은 기본적으로 non-nullable이며, 흐름 감지 타이핑(flow-sensitive typing)을 통해 조건문 범위 내에서 nullable 타입을 자동으로 non-nullable로 승격시킵니다.

예제:
```ceylon
void hello() {
    String? name = process.arguments.first;
    String greeting;
    if (exists name) {
        greeting = "Hello, ``name``!";
    }
    else {
        greeting = "Hello, World!";
    }
    print(greeting);
}
```

`else`를 사용한 대안:
```ceylon
String greeting = "Hello, " + (name else "World");
```

## 4. 기본 매개변수(Defaulted Parameters)

메서드는 오버로딩 없이도 기본 매개변수 값을 지원합니다. 이는 PL/SQL의 접근 방식과 유사합니다.

예제:
```ceylon
void hello(String name="World") {
    print("Hello, ``name``!");
}
```

## 5. 유니온 타입(Union Types)

`String|Integer`와 같은 유니온 타입을 사용하면 오버로딩 없이 호환되지 않는 여러 타입을 메서드가 받을 수 있습니다.

예제:
```ceylon
void printType(String|Integer|Float val) { ... }

printType("hello");
printType(69);
printType(-1.0);

void printType(String|Integer|Float val) {
    switch (val)
    case (is String) { print("String: ``val``"); }
    case (is Integer) { print("Integer: ``val``"); }
    case (is Float) { print("Float: ``val``"); }
}
```

열거 타입:
```ceylon
abstract class Point()
        of Polar | Cartesian {
    // ...
}
```

## 6. 교차 타입(Intersection Types)

교차 타입은 여러 타입 제약을 결합합니다. 흐름 감지 타이핑 및 제네릭과 상호작용합니다.

예제:
```ceylon
value x = X();
//x의 타입은 X
if (something) {
    x = Y();
    //x의 타입은 Y
}
//x의 타입은 X|Y
if (is Z x) {
    //x의 타입은 <X|Y>&Z
}
```

## 7. 타입 별칭(Type Aliases)

타입 별칭은 서브타입을 생성하지 않으면서 `Map<String, List<Map<Integer, String>>>`와 같은 복잡한 제네릭 타입에 읽기 쉬운 이름을 제공합니다. 확장 가능한 매크로처럼 작동합니다.

예제:
```ceylon
interface People => Set<Person>;

People?      p1 = null;
Set<Person>? p2 = p1;
People?      p3 = p2;
```

## 8. 타입 추론(Type Inference)

`value` 키워드는 복잡한 제네릭 및 유니온 타입에서도 지역 변수에 대한 타입 추론을 가능하게 합니다.

예제:
```ceylon
interface Foo {}
interface Bar {}
object foobar satisfies Foo&Bar {}
//추론된 타입 Basic&Foo&Bar
value fb = foobar;
//추론된 타입 {Basic&Foo&Bar+}
value fbs = { foobar, foobar };
```

## 9. 선언부 변성(Declaration-Site Variance)

Ceylon은 Java의 사용부 와일드카드와 달리 타입 선언 시점에 공변성(covariance)과 반공변성(contravariance)을 지원하여 장황함을 줄입니다.

C# 비교:
```csharp
interface IEnumerator<out T>
{
    T Current { get; }
    bool MoveNext();
}

IEnumerator<Cat> cats = ...
IEnumerator<Animal> animals = cats;
```

Java의 대안은 와일드카드가 필요합니다:
```java
Iterator<Cat> cats = ...
Iterator<? extends Animal> animals = cats;
```

## 10. 함수와 메서드(Functions and Methods)

함수는 일관된 구문을 사용하여 패키지 수준, 클래스 메서드, 또는 지역 중첩 함수로 선언할 수 있습니다.

예제:
```ceylon
Integer f1() => 1;
class C() {
    shared Integer f2() {
        Integer f3() => 2;
        return f3();
    }
}

print(f1());
print(C().f2());
```

## 결론

필자는 Eclipse IDE 지원과 문서화 웹사이트가 포함된 무료 다운로드를 통해 Ceylon을 탐험해 볼 것을 권장합니다. 또한 Java 9나 10에서 이러한 기능들을 채택하는 것을 고려해야 한다고 제안합니다.
