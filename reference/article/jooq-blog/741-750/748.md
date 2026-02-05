# Java의 Arrays.asList(...)는 과소사용되고 있다

> 원문: https://blog.jooq.org/javas-arrays-aslist-is-underused/

깔끔하고 간결한 코드를 작성하는 것은 Java에서도 충분히 가능한 일이며, 요즘 유행하는 새롭고 화려한 스크립팅 언어들에서만 가능한 것이 아닙니다. 다음은 Java 5의 가변 인자(varargs) `Arrays.asList()` 메서드를 유용한 맥락에서 활용하는 몇 가지 예시입니다.

## n개의 상수 값에 대해 블록을 실행하기

```java
for (String value : Arrays.asList(VAL_A, VAL_B, VAL_C)) {
  doSomething(value);
}
```

## 집합 내 존재 여부를 확인하기 위한 SQL 스타일의 IN 연산자 만들기

```java
if (Arrays.asList(VAL_A, VAL_B, VAL_C).contains(value)) {
  doSomething();
}
```

물론 구문적으로 더 깔끔한 것은 다음과 같은 표현일 것입니다:

```java
if (value in [VAL_A, VAL_B, VAL_C]) {
  doSomething();
}
```

이 예시는 제가 올린 Stack Overflow 질문 중 하나에서 가져온 것입니다. 이와 비슷한 것이 충분히 생각해 볼 만한데, Java에서 컬렉션 리터럴을 지원하자는 Josh Bloch의 오래된 명세 요청이 있었기 때문입니다. 안타깝게도 이것은 결국 JLS(Java Language Specification)에 반영되지 못했습니다...
