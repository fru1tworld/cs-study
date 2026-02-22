# 모든 객체가 배열이라면? 더 이상 NullPointerException 없음!

> 원문: https://blog.jooq.org/what-if-every-object-was-an-array-no-more-nullpointerexceptions/

## 서론

이 글은 프로그래밍 언어에서 널(nullability)을 처리하는 색다른 접근 방식을 탐구합니다. NULL을 특별한 케이스로 취급하는 대신, 모든 객체가 배열이나 컬렉션처럼 동작하는 모델을 제안하여 널 체크의 필요성을 없애고자 합니다.

## NULL 문제

프로그래밍 언어 설계자들은 NULL 구현에 어려움을 겪고 있습니다. Java의 처리 방식은 일관성이 없습니다:

```java
null == null  // true를 반환
null.toString()  // 예외 발생
int value = (Integer) null  // 컴파일 불가
```

"NULL을 쓸 것인가, 말 것인가? 프로그래밍 언어 설계자들은 필연적으로 NULL을 지원할지 말지 결정해야 합니다. 그리고 그들은 이것을 제대로 처리하는 데 어려움을 겪어왔습니다."

대안적인 접근 방식들이 존재합니다: SQL은 3값 논리(three-value logic)를 사용하고, Groovy는 널 안전 역참조 연산자를 제공하며, Scala와 같은 언어는 Option 타입을 제공하는데 Java 8은 Optional로 이를 따라갔습니다.

## 제안된 해결책

저자는 모든 객체를 배열로 취급할 것을 제안합니다. 이렇게 하면 단일 참조와 컬렉션을 문법적으로 통합할 수 있습니다:

```java
class Customer {
  String firstNames;   // String[] firstNames로 읽음
  String lastName;     // String[] lastName으로 읽음
  Order orders;        // Order[] orders로 읽음
}
```

## 실용적 이점

이 모델을 사용하면 연산이 컬렉션 전체에 균일하게 작동합니다:

```java
customer.orders.shipped()  // 모든 주문을 체크
customer.orders.length     // 컬렉션 크기
customer.orders.add(new Order())
customer.orders.clear()
customer.orders.value.sum()
```

"각 메서드는 항상 배열의 모든 요소에 대해 호출됩니다. 현재의 단일 참조 대 다중 참조 의미론은 명명 규칙으로 문서화될 것입니다."

## jQuery를 영감으로

jQuery는 프로토타입 조작을 통해 이 접근 방식을 잘 보여줍니다:

```javascript
$('div#unique-id').html('new content').click(function() { ... });
$('div.any-class').html('new content').click(function() { ... });
```

두 쿼리 모두 0개, 1개, 또는 수백 개의 요소와 일치하든 상관없이 동일한 문법을 사용합니다.

## 기존 구현들

이 개념은 "배열 프로그래밍(array programming)"이라고 불리며, MATLAB과 R 같은 언어에서 구현되어 있습니다. XQuery는 이 접근 방식의 정적 타입 언어 예시입니다.

## 결론

저자는 이 모델이 널 체크를 길이 체크로 대체하여 널 포인터 예외를 제거하고, 명명 규칙을 통해 타입 안전성을 유지하면서 코드를 단순화한다고 제안합니다.
