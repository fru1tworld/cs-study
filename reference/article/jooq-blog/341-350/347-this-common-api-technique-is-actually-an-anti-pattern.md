# 이 일반적인 API 기법은 실제로 안티패턴이다

> 원문: https://blog.jooq.org/this-common-api-technique-is-actually-an-anti-pattern/

솔직히 말해서, 우리도 이 기법에 유혹당해 사용해왔습니다. 겉보기에 불필요해 보이는 캐스팅을 피할 수 있게 해주기 때문에 정말 편리하거든요. 바로 다음과 같은 기법입니다:

```java
interface SomeWrapper {
  <T> T get();
}
```

이제 wrapper에서 어떤 타입으로든 타입 안전하게 할당할 수 있습니다:

```java
SomeWrapper wrapper = ...

// 당연히 됨
Object a = wrapper.get();

// 음...
Number b = wrapper.get();

// 위험함
String[][] c = wrapper.get();

// 가능성 낮음
javax.persistence.SqlResultSetMapping d =
    wrapper.get();
```

이것이 실제로 jOOR를 사용할 때 쓸 수 있는 API입니다. jOOR는 통합 테스트를 개선하기 위해 우리가 작성하고 오픈소스로 공개한 리플렉션 라이브러리입니다. jOOR를 사용하면 다음과 같이 작성할 수 있습니다:

```java
Employee[] employees = on(department)
    .call("getEmployees").get();

for (Employee employee : employees) {
    Street street = on(employee)
        .call("getAddress")
        .call("getStreet")
        .get();
    System.out.println(street);
}
```

API는 꽤 단순합니다. `on()` 메서드는 `Object`나 `Class`를 래핑합니다. `call()` 메서드는 리플렉션을 사용하여 해당 객체의 메서드를 호출합니다(정확한 시그니처를 요구하지 않고, 메서드가 public일 필요도 없으며, checked 예외도 던지지 않습니다). 그리고 캐스팅 없이 `get()`을 호출하여 결과를 임의의 참조 타입에 할당할 수 있습니다. 이것은 jOOR 같은 리플렉션 라이브러리에서는 아마 괜찮을 것입니다. 왜냐하면 라이브러리 전체가 실제로 타입 안전하지 않기 때문입니다. 리플렉션이니까 타입 안전할 수가 없죠. 하지만 "찝찝한" 느낌은 남습니다. 호출 지점에 결과 타입에 대한 약속을 하는 느낌, 지킬 수 없는 약속, 그리고 결국 `ClassCastException`으로 이어질 약속 말입니다. `ClassCastException`은 Java 5와 제네릭 이후에 시작한 주니어 개발자들은 거의 모르는 과거의 유물이죠.

## 하지만 JDK 라이브러리도 그렇게 하는데...

네, 그렇습니다. 하지만 아주 드물게, 그리고 제네릭 타입 파라미터가 정말로 중요하지 않을 때만 그렇게 합니다. 예를 들어, `Collection.emptyList()`를 가져올 때, 그 구현은 다음과 같습니다:

```java
@SuppressWarnings("unchecked")
public static final <T> List<T> emptyList() {
    return (List<T>) EMPTY_LIST;
}
```

`EMPTY_LIST`가 `List`에서 `List<T>`로 안전하지 않게 캐스팅되는 것은 사실입니다. 하지만 의미론적 관점에서 이것은 안전한 캐스팅입니다. 이 `List` 참조를 수정할 수 없고, 비어있기 때문에 `List<T>`의 어떤 메서드도 대상 타입과 일치하지 않는 `T`나 `T[]` 인스턴스를 반환하지 않습니다. 따라서 다음 모두가 유효합니다:

```java
// 완벽하게 괜찮음
List<?> a = emptyList();

// 네
List<Object> b = emptyList();

// 좋아요
List<Number> c = emptyList();

// 문제없음
List<String[][]> d = emptyList();

// 꼭 그래야 한다면
List<javax.persistence.SqlResultSetMapping> e
    = emptyList();
```

그래서 항상 그렇듯이, JDK 라이브러리 설계자들은 여러분이 얻을 수 있는 제네릭 타입에 대해 거짓 약속을 하지 않도록 세심한 주의를 기울였습니다. 이것은 다른 타입이 더 적합할 것이라고 _알고 있는_ 경우에도 종종 Object 타입을 받게 된다는 것을 의미합니다. 하지만 여러분이 이것을 안다고 해도, 컴파일러는 알지 못합니다. 타입 소거(Erasure)에는 대가가 따르고, 그 대가는 wrapper나 컬렉션이 비어있을 때 지불됩니다. 그러한 표현식의 포함된 타입을 알 방법이 없으므로, 안다고 가장하지 마세요. 다시 말해서:

> 단지 캐스팅을 피하기 위한 제네릭 메서드 안티패턴을 사용하지 마세요
