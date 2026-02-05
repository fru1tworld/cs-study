# Java에 대해 몰랐던 10가지

> 원문: https://blog.jooq.org/10-things-you-didnt-know-about-java/

## 1. 체크 예외는 JVM에 존재하지 않는다

맞습니다! JVM은 체크 예외라는 개념을 전혀 모르며, 이는 Java 언어만의 개념입니다. 오늘날 모든 사람들은 체크 예외가 실수였다는 데 동의합니다. Bruce Eckel이 GeeCON Prague 폐막 기조연설에서 말했듯이, Java 이후의 어떤 언어도 체크 예외를 채택하지 않았으며, 심지어 Java 8도 새로운 Streams API에서 더 이상 체크 예외를 수용하지 않습니다(이는 람다에서 IO나 JDBC를 사용할 때 약간의 고통이 될 수 있습니다). JVM이 체크 예외를 모른다는 증거가 필요하신가요? 다음 코드를 시도해 보세요:

```java
public class Test {

    // 여기에 throws 절이 없음
    public static void main(String[] args) {
        doThrow(new SQLException());
    }

    static void doThrow(Exception e) {
        Test.<RuntimeException> doThrow0(e);
    }

    @SuppressWarnings("unchecked")
    static <E extends Exception>
    void doThrow0(Exception e) throws E {
        throw (E) e;
    }
}
```

이 코드는 컴파일될 뿐만 아니라 실제로 `SQLException`을 던집니다. 이를 위해 Lombok의 [`@SneakyThrows`](http://projectlombok.org/features/SneakyThrows.html)조차 필요하지 않습니다.

## 2. 반환 타입만 다른 메서드 오버로드가 가능하다

다음 코드는 컴파일되지 않습니다, 맞죠?... 맞습니다. Java 언어는 `throws` 절이나 `return` 타입이 다르더라도 동일한 클래스 내에서 두 메서드가 "오버라이드 동등(override-equivalent)"한 것을 허용하지 않습니다.

```java
class Test {
    Object x() { return "abc"; }
    String x() { return "123"; }
}
```

하지만 잠깐만요. [`Class.getMethod(String, Class...)`](http://docs.oracle.com/javase/8/docs/api/java/lang/Class.html#getMethod-java.lang.String-java.lang.Class...-)의 Javadoc을 확인해 보세요. 다음과 같이 설명되어 있습니다:

> Java 언어는 동일한 시그니처를 가지지만 반환 타입이 다른 여러 메서드를 클래스에서 선언하는 것을 금지하지만, Java 가상 머신은 그렇지 않기 때문에 클래스에 일치하는 메서드가 둘 이상 있을 수 있습니다. 가상 머신의 이러한 유연성은 다양한 언어 기능을 구현하는 데 사용될 수 있습니다. 예를 들어, 공변 반환(covariant return)은 브릿지 메서드를 사용하여 구현할 수 있습니다; 브릿지 메서드와 오버라이드되는 메서드는 동일한 시그니처를 가지지만 반환 타입이 다릅니다.

다음 예제를 보세요:

```java
abstract class Parent<T> {
    abstract T x();
}

class Child extends Parent<String> {
    @Override
    String x() { return "abc"; }
}
```

`Child`의 바이트코드를 확인해 보세요:

```
  // Method descriptor #15 ()Ljava/lang/String;
  // Stack: 1, Locals: 1
  java.lang.String x();
    0  ldc <String "abc"> [16]
    2  areturn
      Line numbers:
        [pc: 0, line: 7]
      Local variable table:
        [pc: 0, pc: 3] local: this index: 0 type: Child

  // Method descriptor #18 ()Ljava/lang/Object;
  // Stack: 1, Locals: 1
  bridge synthetic java.lang.Object x();
    0  aload_0 [this]
    1  invokevirtual Child.x() : java.lang.String [19]
    4  areturn
      Line numbers:
        [pc: 0, line: 1]
```

따라서 `T`는 바이트 코드에서 실제로 `Object`입니다. 이는 잘 알려진 사실입니다. 합성 브릿지 메서드(synthetic bridge method)는 특정 호출 지점에서 `Parent.x()` 시그니처의 반환 타입이 `Object`로 예상될 수 있기 때문에 컴파일러에 의해 실제로 생성됩니다.

## 3. 2차원 배열의 여러 선언 구문

네, 사실입니다. 여러분의 머릿속 파서가 아래 메서드들의 반환 타입을 즉시 이해하지 못할 수 있지만, 이들은 모두 동일합니다!

```java
class Test {
    int[][] a()  { return new int[0][]; }
    int[] b() [] { return new int[0][]; }
    int c() [][] { return new int[0][]; }
}
```

다음 코드도 마찬가지입니다:

```java
class Test {
    int[][] a = {{}};
    int[] b[] = {{}};
    int c[][] = {{}};
}
```

타입 어노테이션을 사용하면 상황이 더 복잡해집니다:

```java
@Target(ElementType.TYPE_USE)
@interface Crazy {}

class Test {
    @Crazy int[][]  a1 = {{}};
    int @Crazy [][] a2 = {{}};
    int[] @Crazy [] a3 = {{}};

    @Crazy int[] b1[]  = {{}};
    int @Crazy [] b2[] = {{}};
    int[] b3 @Crazy [] = {{}};

    @Crazy int c1[][]  = {{}};
    int c2 @Crazy [][] = {{}};
    int c3[] @Crazy [] = {{}};
}
```

## 4. 조건 표현식의 기이한 동작

조건 표현식을 사용하는 것에 대해 모든 것을 알고 있다고 생각하셨나요? 말씀드리자면, 그렇지 않습니다. 대부분의 분들은 아래 두 코드 조각이 동등하다고 생각하실 것입니다:

```java
Object o1 = true ? new Integer(1) : new Double(2.0);
```

```java
Object o2;

if (true)
    o2 = new Integer(1);
else
    o2 = new Double(2.0);
```

하지만 이 프로그램은 다음을 출력합니다:

```
1.0
1
```

네! 조건 연산자는 "필요한" 경우 숫자 타입 프로모션을 구현하는데, 그 "필요한"에 매우매우매우 강한 인용 부호를 붙여야 합니다. 왜냐하면, 이 프로그램이 `NullPointerException`을 던질 것이라고 예상하셨나요?

```java
Integer i = new Integer(1);
if (i.equals(1))
    i = null;
Double d = new Double(2.0);
Object o = true ? i : d; // NullPointerException!
System.out.println(o);
```

## 5. 복합 대입 연산자

충분히 기이한가요? 다음 두 코드 조각을 살펴봅시다:

```java
i += j;
i = i + j;
```

직관적으로 이들은 동등해야 합니다, 맞죠? 하지만 무슨 일인지. 그렇지 않습니다! JLS에는 다음과 같이 명시되어 있습니다:

> E1 op= E2 형태의 복합 대입 표현식은 E1 = (T)((E1) op (E2))와 동등합니다. 여기서 T는 E1의 타입이며, E1은 한 번만 평가됩니다.

Peter Lawrey의 예제를 보세요:

```java
byte b = 10;
b *= 5.7;
System.out.println(b); // 57 출력
```

```java
byte b = 100;
b /= 2.5;
System.out.println(b); // 40 출력
```

```java
char ch = '0';
ch *= 1.1;
System.out.println(ch); // '4' 출력
```

```java
char ch = 'A';
ch *= 1.5;
System.out.println(ch); // 'a' 출력
```

## 6. 무작위 정수

다음 테스트 코드를 보세요:

```java
for (int i = 0; i < 10; i++) {
  System.out.println((Integer) i);
}
```

가능한 출력:

```
92
221
45
48
236
183
39
193
33
84
```

해결책은 [여기](http://blog.jooq.org/2013/10/17/add-some-entropy-to-your-jvm/)에서 확인할 수 있으며, 리플렉션을 통해 JDK의 `Integer` 캐시를 오버라이드한 다음 오토박싱과 오토언박싱을 사용하는 것과 관련이 있습니다. 집에서 따라하지 마세요!

## 7. GOTO

이것은 제가 가장 좋아하는 것 중 하나입니다. Java에는 GOTO가 있습니다! 입력해 보세요...

```java
int goto = 1;
```

오류 출력:

```
Test.java:44: error: <identifier> expected
    int goto = 1;
       ^
```

`goto`가 사용되지 않는 예약어이기 때문입니다. 하지만 실제로 Java에서 점프가 가능합니다.

앞으로 점프하는 예제:

```java
label: {
  // 무언가 수행
  if (check) break label;
  // 더 많은 것을 수행
}
```

바이트코드:

```
2  iload_1 [check]
3  ifeq 6          // 앞으로 점프
6  ..
```

뒤로 점프하는 예제:

```java
label: do {
  // 무언가 수행
  if (check) continue label;
  // 더 많은 것을 수행
  break label;
} while(true);
```

바이트코드:

```
 2  iload_1 [check]
 3  ifeq 9
 6  goto 2          // 뒤로 점프
 9  ..
```

## 8. 타입 별칭

Ceylon에서는 타입 별칭을 정의할 수 있습니다:

```
interface People => Set<Person>;
```

Java에서도 제네릭을 사용하여 클래스나 메서드 범위 내에서 유사한 효과를 얻을 수 있습니다:

```java
class Test<I extends Integer> {
    <L extends Long> void x(I i, L l) {
        System.out.println(
            i.intValue() + ", " +
            l.longValue()
        );
    }
}
```

사용:

```java
new Test().x(1, 2L);
```

## 9. 결정 불가능한 타입 관계

다음 타입 정의를 보세요:

```java
interface Type<T> {}

class C implements Type<Type<? super C>> {}
class D<P> implements Type<Type<? super D<D<P>>>> {}
```

테스트 코드:

```java
class Test {
    Type<? super C> c = new C();
    Type<? super D<Byte>> d = new D<Byte>();
}
```

C의 타입 검사 분석:

```
Step 0) C <?: Type<? super C>
Step 1) Type<Type<? super C>> <?: Type (상속)
Step 2) C (와일드카드 ? super C 검사)
Step . . . (영원히 순환)
```

D의 타입 검사 분석:

```
Step 0) D<Byte> <?: Type<? super C<Byte>>
Step 1) Type<Type<? super D<D<Byte>>>> <?: Type<? super D<Byte>>
Step 2) D<Byte> <?: Type<? super D<D<Byte>>>
Step 3) Type<type<? super C<C>>> <?: Type<? super C<C>>
Step 4) D<D<Byte>> <?: Type<? super D<D<Byte>>>
Step . . . (영원히 확장)
```

위의 코드를 Eclipse에서 컴파일해 보세요, 크래시가 발생합니다! Java의 일부 타입 관계는 *결정 불가능*합니다!

## 10. 타입 교차

기본 예제:

```java
class Test<T extends Serializable & Cloneable> {
}
```

컴파일 예제:

```java
// 컴파일되지 않음
Test<String> s = null;

// 컴파일됨
Test<Date> d = null;
```

제약 조건이 있는 메서드:

```java
<T extends Runnable & Serializable> void execute(T t) {}
```

람다 캐스팅:

```java
execute((Runnable & Serializable) (() -> {}));
```

## 결론

저는 보통 SQL에 대해서만 이 말을 하지만, 이 글을 다음과 같이 마무리할 때가 되었습니다: "Java는 그 신비로움이 그 힘에 의해서만 능가되는 장치입니다."
