# R2DBC 사용 시 실수로 발생하는 블로킹 호출 감지하기

> 원문: https://blog.jooq.org/detect-accidental-blocking-calls-when-using-r2dbc/

얼마 전, jOOQ는 반환 타입에 null 가능성 정보를 어노테이션으로 표시하기 위해 `org.jetbrains:annotations` 의존성을 jOOQ API에 추가했습니다. 예를 들어, DSL 전체는 non-nullable입니다:

```java
public interface SelectWhereStep<R extends Record>
extends SelectConnectByStep<R> {

    @NotNull @CheckReturnValue
    @Support
    SelectConditionStep<R> where(Condition condition);

    // ...
}
```

특히 Kotlin 사용자에게 이러한 보장을 제공하는 것은 합리적입니다. `Select!<Record!>`와 같은 복잡한 타입을 최소한 `Select<Record!>`로 줄일 수 있기 때문입니다. 또한 `@CheckReturnValue` 어노테이션도 주목하세요. IntelliJ는 이를 일부 정적 분석에 활용합니다.

다른 경우는 더 명확합니다:

```java
public interface ResultQuery<R extends Record>
extends Fields, Query, Iterable<R>, Publisher<R> {

    @Nullable
    @Blocking
    R fetchOne() throws TooManyRowsException;

    @NotNull
    @Blocking
    R fetchSingle() throws NoDataFoundException, TooManyRowsException;

    // ...
}
```

이 두 가지 fetch 메서드의 차이점은 `fetchOne()`은 0~1개의 결과 레코드를 기대하는 반면, `fetchSingle()`은 정확히 1개의 레코드를 기대한다는 것입니다. 두 메서드 모두 그 외의 경우에는 예외를 던지지만, 후자만이 non-nullable 레코드 값을 보장할 수 있습니다.

## 그런데 이 @Blocking 어노테이션은 뭘까요?

jOOQ 3.17에서는 JDBC 위에서 쿼리를 실행하는 모든 jOOQ API에 `org.jetbrains.annotations.Blocking` 어노테이션을 추가했습니다. 이는 JDBC 기반 jOOQ 쿼리를 사용하는 사용자에게는 전혀 영향을 미치지 않지만, jOOQ와 함께 R2DBC를 사용하여 리액티브 쿼리를 수행하는 경우, 실수로 jOOQ의 수많은 블로킹 실행 메서드 중 하나를 호출하고 싶은 유혹에 빠질 수 있습니다.

이제 더 이상 그럴 일은 없습니다! IntelliJ가 이러한 호출이 부적절하다고 경고해 줄 것입니다.

최소한 정적 분석을 활성화하면, 이를 경고로 설정할지 오류로 설정할지는 여러분에게 달려 있지만, 적어도 무언가 잘못하려고 한다는 것을 알아차릴 수 있을 것입니다.

Eclipse는 아직 이러한 종류의 정적 분석을 지원하지 않습니다. 만약 지원해야 한다고 생각하신다면, 이 이슈에 투표해 주세요: https://bugs.eclipse.org/bugs/show_bug.cgi?id=578310
