# Java에서 리플렉션을 사용하는 더 깔끔한 방법

> 원문: https://blog.jooq.org/a-neater-way-to-use-reflection-in-java/

Java에서 리플렉션은 정말 어색하게 느껴집니다. `java.lang.reflect` API는 매우 강력하고 완전하며, 그런 의미에서 매우 장황하기도 합니다. 대부분의 스크립팅 언어와 달리, 리플렉션을 사용하여 메서드와 필드에 동적으로 접근하는 편리한 방법이 없습니다. 편리하다는 것은 다음과 같은 것을 의미합니다.

PHP 예제:

```php
$method = 'my_method';
$field  = 'my_field';

// 메서드를 동적으로 호출
$object->$method();

// 필드에 동적으로 접근
$object->$field;
```

또는 더 나은 예로, JavaScript:

```javascript
var method   = 'my_method';
var field    = 'my_field';

// 함수를 동적으로 호출
object[method]();

// 필드에 동적으로 접근
object[field];
```

우리 Java 개발자들에게는, 이것은 꿈에서나 가능한 일입니다. 우리는 이렇게 작성해야 합니다:

```java
String method = "my_method";
String field  = "my_field";

// 메서드를 동적으로 호출
object.getClass().getMethod(method).invoke(object);

// 필드에 동적으로 접근
object.getClass().getField(field).get(object);
```

분명히, 이것은 `NullPointerException`, `InvocationTargetException`, `IllegalAccessException`, `IllegalArgumentException`, `SecurityException`, 원시 타입(primitive types) 등을 처리하지 않습니다. 엔터프라이즈 세계에서는 모든 것이 안전하고 보안되어야 하며, Java 설계자들은 리플렉션을 사용할 때 발생할 수 있는 모든 가능한 문제를 고려했습니다. 하지만 많은 경우, 우리는 우리가 무엇을 하고 있는지 알고 있으며 그러한 기능 대부분에 신경 쓰지 않습니다. 우리는 덜 장황한 방식을 선호합니다. 그래서 저는 jOO\* 패밀리에 또 다른 형제를 만들었습니다: jOOR (Java Object Oriented Reflection). 이것이 킬러 라이브러리는 아니지만, 간단하고 유창한(fluent) 솔루션을 찾고 있는 한두 명의 개발자에게 유용할 수 있습니다.

다음은 제가 최근 Stack Overflow에서 접한 예제로, jOOR가 딱 들어맞을 수 있는 경우입니다:

고전적인 리플렉션 사용 예제:

```java
try {
  Method m1 = department.getClass().getMethod("getEmployees");
  Employee employees = (Employee[]) m1.invoke(department);

  for (Employee employee : employees) {
    Method m2 = employee.getClass().getMethod("getAddress");
    Address address = (Address) m2.invoke(employee);

    Method m3 = address.getClass().getMethod("getStreet");
    Street street = (Street) m3.invoke(address);

    System.out.println(street);
  }
}
// 어차피 무시할 가능성이 높은 많은 체크 예외들이 있습니다
catch (Exception ignore) {
  // ... 또는 그냥 선호하는 런타임 예외로 감싸세요:
  throw new RuntimeException(e);
}
```

그리고 jOOR를 사용한 동일한 예제:

```java
Employee[] employees = on(department).call("getEmployees").get();

for (Employee employee : employees) {
  Street street = on(employee).call("getAddress").call("getStreet").get();
  System.out.println(street);
}
```

또 다른 예제:

```java
String world =
on("java.lang.String")  // Class.forName()과 같음
 .create("Hello World") // 가장 구체적으로 일치하는 생성자를 호출
 .call("substring", 6)  // 가장 구체적으로 일치하는 메서드를 호출
 .call("toString")      // toString()을 호출
 .get()                 // 래핑된 객체를 가져옴, 이 경우 String
```

jOOR를 여기서 무료로 받으세요: https://github.com/jOOQ/jOOR
