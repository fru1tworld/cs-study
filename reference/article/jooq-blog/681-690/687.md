# Java의 Array, List, Set, Map, Tuple, Record 리터럴

> 원문: https://blog.jooq.org/array-list-set-map-tuple-record-literals-in-java/

때때로 JavaScript의 표현력에 감탄하다 보면, Java에서 고급 리터럴 문법이 없다는 것이 아쉬울 때가 있습니다. 람다 표현식 외에도, 리터럴을 사용하여 상수 데이터 구조를 생성할 수 있는 기능은 매우 유용할 것입니다. JavaScript에서는 맵을 생성하는 것이 매우 간단합니다:

```javascript
var map = { "a":1, "b":2, "c":3 };
```

이것은 특히 API에 복잡한 매개변수를 전달할 때 장황한 Java 방식과 대조됩니다.

## Java 배열 리터럴에 대한 논의

Java는 할당문에서는 배열 리터럴을 지원하지만, 메서드 인자에서는 지원하지 않습니다:

```java
// 이것은 동작합니다:
int[] array = { 1, 2, 3 };

// 이것은 컴파일되지 않습니다:
class Test {
  public void callee(int[] array) {}
  public void caller() {
    callee({1, 2, 3});  // 컴파일 오류
  }
}
```

## Brian Goetz의 제안

lambda-dev 메일링 리스트에서 Goetz는 잠재적인 리터럴 문법을 제안했습니다:

```
#[ 1, 2, 3 ]                          // Array, list, set
#{ "foo" : "bar", "blah" : "wooga" }  // Map 리터럴
#/(\\d+)$/                             // 정규식
#(a, b)                               // Tuple
#(a: 3, b: 4)                         // Record
#"There are {foo.size()} foos"        // 문자열 리터럴
```

하지만 그는 다음과 같이 덧붙였습니다: "이 모든 것을 즉시 (혹은 영원히) 수용하지는 않을 것입니다."

## jOOQ와 Record에 대한 비전

진정한 레코드 지원의 전망은 저를 흥분시킵니다. 언어 수준의 튜플과 레코드 지원이 있다면, 개발자들은 데이터베이스 결과를 더 표현력 있게 다룰 수 있을 것입니다:

```java
for (val record : create.select(
                       BOOK.AUTHOR_ID.as("author"),
                       count().as("books"))
                    .from(BOOK)
                    .groupBy(BOOK.AUTHOR_ID)
                    .fetch()) {
  int author = record.author;
  int books = record.books;
}
```

## 결론

아직은 추측에 불과하지만, 진정한 튜플과 레코드 지원은 Java 생태계 전반에 걸쳐 상당한 기능을 해방시킬 수 있으며, 라이브러리와 API 설계를 근본적으로 개선할 수 있을 것입니다.
