# Java 자동 언박싱 함정. 조심하라!

> 원문: https://blog.jooq.org/java-auto-unboxing-gotcha-beware/

이 글은 자동 언박싱과 관련된 Java 삼항 연산자의 놀라운 동작을 살펴봅니다. 다음 코드를 작성하면:

```java
Object o = true ? new Integer(1) : new Double(2.0);
System.out.println(o);
```

출력 결과는 `1`이 아닌 `1.0`입니다. 이는 직관에 반하는 결과입니다.

## 근본 원인

Java 언어 명세(JLS) 15.25절에 따르면, "이진 숫자 승격(binary numeric promotion)은 암묵적으로 언박싱 변환을 수행할 수 있습니다." 삼항 연산자가 서로 다른 숫자 래퍼 타입의 분기를 평가할 때, 두 피연산자를 원시 타입 동등물로 언박싱하고, 승격된 타입으로 결과를 다시 박싱하는 숫자 승격 규칙을 적용합니다.

이 경우:
- `Integer(1)`은 `int`로 언박싱됩니다
- `Double(2.0)`은 `double`로 언박싱됩니다
- 이진 승격이 `int`를 `double`로 변환합니다
- 결과는 `Double(1.0)`이 됩니다

## 치명적인 위험: NullPointerException

이 글은 `null` 값이 관련될 때 이 동작이 숨겨진 위험을 만든다고 경고합니다:

```java
Integer i = new Integer(1);
if (i.equals(1))
    i = null;
Double d = new Double(2.0);
Object o = true ? i : d; // NullPointerException!
```

`null` 값에 대한 자동 언박싱 시도는 예외를 발생시킵니다.

## 권장 해결책

`Object`로 명시적 캐스팅하여 암묵적 숫자 승격을 방지하세요:

```java
Object o1 = true ? (Object) new Integer(1) : new Double(2.0);
System.out.println(o1); // 출력: 1
```

이렇게 하면 이진 숫자 승격을 트리거하지 않고 타입 일관성을 보장합니다.

## 핵심 요점

이 함정을 발견한 Paul Miner에게 감사를 표합니다. 이 글은 JPL의 코딩 표준(R40)을 참조하는데, 복잡한 승격 규칙 때문에 조건식에서 하나의 일관된 결과 타입을 사용할 것을 권장합니다.
