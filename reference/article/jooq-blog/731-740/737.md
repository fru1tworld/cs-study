# API 메서드 오버로딩에 주의하라 - 속편

> 원문: https://blog.jooq.org/overload-api-methods-with-care-the-sequel/

[이전 블로그 글](https://blog.jooq.org/overload-api-methods-with-care/)에서 제네릭을 사용한 문제 있는 API 메서드 오버로딩에 대해 이야기하면서 속편을 약속했었다. 그때 언급한 것 이상으로 더 많은 문제를 만났기 때문인데, 여기 그 속편이다.

## 제네릭과 가변인자의 문제

가변인자(varargs)는 Java 5에서 도입된 또 하나의 훌륭한 기능이다. 단순한 문법적 편의(syntactic sugar)에 불과하지만, 메서드에 배열을 전달할 때 상당한 양의 코드를 줄일 수 있다:

```java
// 가변인자를 사용하거나 사용하지 않는 메서드 선언
public static String concat1(int[] values);
public static String concat2(int... values);

// 위 두 메서드는 사실상 동일하다.
String s1 = concat1(new int[] { 1, 2, 3 });
String s2 = concat2(new int[] { 1, 2, 3 });

// 단지, concat2는 이렇게도 편리하게 호출할 수 있다
String s3 = concat2(1, 2, 3);
```

이것은 잘 알려진 사실이다. 이는 원시 타입 배열에서도 `Object[]`에서도 동일하게 동작한다. 또한 `T`가 제네릭 타입인 `T[]`에서도 동작한다!

```java
// 이제 가변인자 매개변수에 제네릭 타입을 사용할 수 있다:
public static <T> T[] array(T... values);

// 위 메서드는 (오토박싱과 함께) "타입 안전하게" 호출할 수 있다:
Integer[] ints   = array(1, 2, 3);
String[] strings = array("1", "2", "3");

// Object도 T로 추론될 수 있으므로, 이렇게도 할 수 있다:
Object[] applesAndOranges = array(1, "2", 3.0);
```

마지막 예제가 사실 문제를 이미 암시하고 있다. `T`에 상한 경계(upper bound)가 없으면, 타입 안전성은 완전히 사라진다. 이것은 환상에 불과한데, 결국 가변인자 매개변수는 항상 `Object...`로 추론될 수 있기 때문이다. 그리고 여기서 이것이 이러한 API를 오버로딩할 때 어떻게 문제를 일으키는지 보여주겠다.

```java
// "편의"를 위해 오버로딩된 메서드. 두 번째 메서드를 호출할 때
// 발생하는 컴파일러 경고는 무시하자
public static <T> Field<T> myFunction(T... params);
public static <T> Field<T> myFunction(Field<T>... params);
```

처음에는 이것이 좋은 아이디어처럼 보일 수 있다. 인자 목록은 상수 값(`T...`)이 될 수도 있고 동적 필드(`Field<T>...`)가 될 수도 있다. 그래서 원칙적으로 다음과 같은 것들을 할 수 있다:

```java
// 외부 함수는 내부 함수들로부터 <T>를 Integer로 추론할 수 있고,
// 내부 함수들은 T...로부터 <T>를 Integer로 추론할 수 있다
Field<Integer> f1 = myFunction(myFunction(1), myFunction(2, 3));

// 하지만 주의하라, 이것도 컴파일된다!
Field<?> f2 = myFunction(myFunction(1), myFunction(2.0, 3.0));
```

내부 함수들은 `<T>`를 각각 `Integer`와 `Double`로 추론할 것이다. 호환되지 않는 반환 타입 `Field<Integer>`와 `Field<Double>`로 인해, "의도된" 메서드인 `Field<T>...` 인자를 가진 메서드는 더 이상 적용할 수 없게 된다. 따라서 유일하게 적용 가능한 메서드로서 `T...`를 가진 첫 번째 메서드가 컴파일러에 의해 연결된다. 하지만 `<T>`에 대해 (가능하게) 추론되는 경계를 여러분은 추측하지 못할 것이다. 다음은 가능한 추론 타입들이다:

```java
// 이것은 항상 할 수 있다:
Field<?> f2 = myFunction(myFunction(1), myFunction(2.0, 3.0));

// 하지만 이것들은 실제로 여러분이 하려는 것이 무엇인지 보여준다
Field<? extends Field<?>>                       f3 = // ...
Field<? extends Field<? extends Number>>        f4 = // ...
Field<? extends Field<? extends Comparable<?>>> f5 = // ...
Field<? extends Field<? extends Serializable>>  f6 = // ...
```

컴파일러는 `<T>`에 대한 유효한 상한 경계로 `Field<? extends Number & Comparable<?> & Serializable>`과 같은 것을 추론할 수 있다. 그러나 `<T>`에 대한 유효한 정확한 경계(exact bound)는 존재하지 않는다. 따라서 `<? extends [상한 경계]>`가 필요하게 된다.

## 결론

가변인자 매개변수를 제네릭과 결합할 때, 특히 오버로딩된 메서드에서 주의하라. 사용자가 제네릭 타입 매개변수를 여러분이 의도한 대로 정확하게 바인딩하면 모든 것이 잘 동작한다. 하지만 단 하나의 오타가 있으면 (예: `Integer`를 `Double`로 혼동하는 것), API 사용자는 난관에 빠지게 된다. 그리고 정상적인 사람이라면 아무도 다음과 같은 컴파일러 오류 메시지를 읽을 수 없기 때문에, 그들은 자신의 실수를 쉽게 찾지 못할 것이다:

```java
Test.java:58: incompatible types
found   : Test.Field<Test.Field<
          ? extends java.lang.Number&java.lang.Comparable<
          ? extends java.lang.Number&java.lang.Comparable<?>>>>
required: Test.Field<java.lang.Integer>
        Field<Integer> f2 = myFunction(myFunction(1),
                                       myFunction(2.0, 3.0));
```
