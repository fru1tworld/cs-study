# jOOQ에서 .execute() 호출을 다시는 잊지 마라

> 원문: https://blog.jooq.org/never-again-forget-to-call-execute-in-jooq/

게시일: 2021년 3월 30일
저자: Lukas Eder

## 문제점

jOOQ 사용자들 사이에서 매우 흔한 실수가 하나 있다. DML 문(INSERT, UPDATE, DELETE, MERGE)에서 `.execute()` 호출을 잊어버리는 것이다. 개발자들은 종종 아무 일도 일어나지 않는다는 것을 깨닫지 못한 채 다음과 같은 코드를 작성한다:

```java
ctx.insertInto(T)
   .columns(T.A, T.B)
   .values(1, 2);
```

`.execute()` 호출이 없으면, insert는 절대로 실행되지 않는다. 이것은 특히 교활한 버그인데, 코드가 문법적으로는 완벽하게 올바르게 보이지만 실제로는 아무런 부수 효과(side effect)도 발생하지 않기 때문이다.

## 왜 이런 일이 발생하는가

이것이 SELECT 쿼리에서 `.fetch()` 호출을 잊어버리는 것과 다른 점은, SELECT의 경우 결과를 받지 못하면 즉시 알아차릴 수 있다는 것이다. 결과가 필요하기 때문에 자연스럽게 `.fetch()`를 호출하게 된다.

하지만 DML 작업에서는 상황이 다르다. jOOQ의 DML, DDL 및 관련 문장들은 부수 효과(side effect)를 주요 목적으로 생성한다. 반환 값(일반적으로 업데이트 카운트를 나타내는 정수)은 부차적인 것이다. 대부분의 경우 개발자들은 이 값에 관심이 없다.

이는 `StringBuilder`나 `Stream`과 같은 다른 플루언트(fluent) API와 대조적인데, 그런 API에서는 개발자들이 적극적으로 출력을 원하기 때문에 결과를 소비하는 것을 잊어버리면 더 명확하게 드러난다.

저자는 다음과 같이 언급한다: "우리는 `execute()` 결과를 원하지 않는다" - 개발자들이 관심을 갖는 것은 정수 반환 값이 아니라 부수 효과이기 때문이다.

## 해결책

jOOQ 팀은 모든 관련 API 메서드에 `@CheckReturnValue` 어노테이션(JSR-305 표준을 모방)을 추가했다. 이를 통해 IntelliJ IDEA와 유사한 도구들이 개발자가 쿼리 결과를 소비하는 것을 잊었을 때 경고를 표시할 수 있게 되었다.

이제 IntelliJ는 사용자가 jOOQ의 DSL 메서드 결과를 소비하는 것을 잊을 때마다 경고한다.

구현에는 다음이 포함되었다:
- jOOQ의 전체 API에 어노테이션 적용
- JetBrains 및 Tagir Valeev와 협력하여 `@Contract` 어노테이션 개선
- 이러한 흔한 실수를 잡아내는 IDE 경고 활성화

이 어노테이션은 반환 값의 소비가 필요한 메서드를 표시하여, 누락된 `.execute()` 호출에 대해 IDE 경고를 유도한다.

## 올바른 사용법

올바른 코드는 다음과 같이 `.execute()`를 호출해야 한다:

```java
ctx.insertInto(T)
   .columns(T.A, T.B)
   .values(1, 2)
   .execute();  // 이것을 잊지 말 것!
```

## 결론

JetBrains와의 이러한 협업은 런타임 이전에 흔한 실수를 방지하기 위한 코드 어노테이션의 실용적인 적용을 나타낸다.

이제 개발자들이 `.execute()`를 잊으면, IntelliJ가 시각적 경고를 제공하여 수 시간에 걸친 혼란스러운 디버깅을 방지해 준다. `@CheckReturnValue` 어노테이션의 도입으로, jOOQ 사용자들은 더 이상 `.execute()` 호출을 잊어버리는 실수로 인해 디버깅에 시간을 낭비할 필요가 없다.
