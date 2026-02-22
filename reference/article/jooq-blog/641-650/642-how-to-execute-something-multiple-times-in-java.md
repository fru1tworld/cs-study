# Java에서 동일한 코드를 여러 번 실행하는 방법

> 원문: https://blog.jooq.org/how-to-execute-something-multiple-times-in-java/

게시일: 2013년 2월 27일, 작성자: lukaseder

## 소개

단위 테스트와 통합 테스트를 작성할 때, 서로 다른 구성, 매개변수 또는 인수로 코드를 여러 번 실행해야 하는 경우가 자주 있습니다. 예를 들어, "limit"이나 "timeout" 값을 1, 10, 100으로 테스트하려면 반복이 필요합니다.

## 초기 접근 방식

가장 명확한 방법은 별도의 메서드 호출을 추출하는 것입니다:

```java
@Test
public void test() {
    runCode(1);
    runCode(10);
    runCode(100);
}

private void runCode(int argument) {
    // 실제 테스트 실행
    assertNotNull(new MyObject(argument).execute());
}
```

그러나 이 방식은 해당 테스트 케이스 외부에서는 재사용할 수 없는 추출된 메서드를 생성하므로, 별도의 메서드로 만들 만한 가치가 있는지 의문이 듭니다.

## 권장 대안

더 우아한 해결책은 인라인 배열이나 리스트와 함께 for 루프를 사용하는 것입니다:

```java
@Test
public void test() {

    // 값 1, 10, 100에 대해 내용을 3번 반복
    for (int argument : new int[] { 1, 10, 100 }) {

        // 실제 테스트 실행
        assertNotNull(new MyObject(argument).execute());
    }

    // 또는 유사한 효과를 가진 Arrays.asList()를 사용:
    for (Integer argument : Arrays.asList(1, 10, 100)) {

        // 실제 테스트 실행
        assertNotNull(new MyObject(argument).execute());
    }
}
```

이 접근 방식은 불필요한 메서드 추출을 제거하면서 테스트 로직을 간결하고 읽기 쉽게 유지하는 더 깔끔하고 유지보수하기 쉬운 코드를 제공합니다.
