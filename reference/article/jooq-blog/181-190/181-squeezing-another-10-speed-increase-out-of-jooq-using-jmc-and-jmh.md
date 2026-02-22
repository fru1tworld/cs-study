# JMC와 JMH를 사용하여 jOOQ에서 10% 추가 속도 향상 끌어내기

> 원문: https://blog.jooq.org/squeezing-another-10-speed-increase-out-of-jooq-using-jmc-and-jmh/

저자: Lukas Eder

작성일: 2017년 11월 1일

---

## 서론

이 글에서는 JMC(Java Mission Control)를 사용하여 발견한 핫스팟을 반복적으로 개선하고 JMH(Java Microbenchmark Harness)로 검증하여 jOOQ에서 대략 10%의 속도 향상을 끌어낸 최근의 노력에 대해 논의할 것입니다. 이 글은 아주 작은 개선이라도 큰 효과를 낼 수 있는 알고리즘에 마이크로 최적화를 적용하는 방법을 보여줍니다.

JMH는 아마 경쟁자가 없겠지만, JMC는 JProfiler, YourKit, 또는 심지어 직접 수행하는 jstack 샘플링으로도 쉽게 대체할 수 있습니다. 저는 JMC가 JDK에 포함되어 있고 JDK 8과 9 기준으로 개발 목적으로는 무료로 사용할 수 있기 때문에 JMC를 사용합니다(자신이 "개발" 중인지 확실하지 않다면 Oracle에 문의하는 것이 좋습니다). JMC가 가까운 미래에 OpenJDK에 기여될 수 있다는 소문이 있습니다.

## 마이크로 최적화

마이크로 최적화는 로컬 알고리즘(예: 루프)에서 아주 작은 개선을 끌어내어 전체 애플리케이션/라이브러리에 큰 효과를 주는 멋진 기법입니다. 이는 해당 로컬 알고리즘이 여러 번 호출되기 때문입니다. 이것은 본질적으로 항상 4개의 중첩 루프를 실행하는 라이브러리인 jOOQ에서 절대적으로 해당되는 경우입니다:

1. S: 모든 가능한 SQL 문에 대한 "루프"
2. E: 해당 문의 모든 실행에 대한 "루프"
3. R: 결과의 모든 행에 대한 루프
4. C: 행의 모든 열에 대한 루프

이러한 4단계 중첩 루프는 알고리즘의 다항 복잡도라고 부를 수 있는 것을 만들어냅니다. 복잡도를 O(N⁴)라고 부를 수는 없지만(4개의 "N"이 모두 같지 않으므로), 확실히 O(S x E x R x C)입니다(이하 "S-E-R-C 루프"라고 부르겠습니다). 훈련받지 않은 눈에도 가장 안쪽의 "C 루프"에서 발생하는 모든 것이 치명적인 영향을 미칠 수 있다는 것이 명백해집니다.

## 이러한 루프에서 결함을 발견하는 방법은?

우리는 모든 사용자에게 영향을 미치는 문제, 즉 한번 수정되면 모든 사람을 위해 jOOQ의 성능을 예를 들어 10% 향상시킬 종류의 문제를 찾고 있습니다. 이는 JIT가 스택 할당, 인라이닝과 같은 것들을 수행하여 로컬에서 극적으로 개선하지는 않지만 전역적으로, 모든 사람을 위해 그렇게 하는 것과 유사합니다.

### 큰 "S 루프" 얻기

첫 번째 옵션은 벤치마크에서 프로파일링 세션을 실행하는 것입니다. 예를 들어, JMC 프로파일링 세션에서 전체 "S-E-R-C 루프"를 실행할 수 있는데, 여기서 "S 루프"는 모든 문에 대한 루프, 즉 모든 통합 테스트에 대한 루프입니다. 불행히도, 이 접근 방식에서는 "E 루프"(jOOQ 통합 테스트의 경우)가 문당 단일 실행입니다. 의미 있는 결과를 얻으려면 통합 테스트를 여러 번, 많이 실행해야 합니다.

또한, jOOQ 통합 테스트가 수천 개의 고유한 쿼리를 실행하지만, 대부분의 쿼리는 여전히 각각 개별 SQL 기능(예: lateral join)에 초점을 맞추는 비교적 단순합니다. 최종 사용자 애플리케이션에서 쿼리는 특정 기능을 덜 사용할 수 있지만, 훨씬 더 복잡합니다. 즉, 많은 일반 조인이 있습니다.

이 기법은 jOOQ 깊숙한 곳에서 모든 쿼리에 나타나는 문제를 찾는 데 유용합니다 - 예를 들어 JDBC 인터페이스에서. 그러나 이 접근 방식을 사용하여 개별 기능을 테스트할 수는 없습니다.

### 큰 "E 루프" 얻기

또 다른 옵션은 몇 개의 문(작은 "S 루프")을 명시적 루프에서 여러 번 실행하는 단일 테스트를 작성하는 것입니다(큰 "E 루프"). 이것은 특정 병목 현상을 높은 신뢰도로 찾을 수 있다는 장점이 있지만, 단점은: 특정적이라는 것입니다. 예를 들어, 문자열 연결 함수에서 작은 병목 현상을 발견하면, 확실히 수정할 가치가 있지만 대부분의 사용자에게는 영향을 미치지 않습니다.

이 접근 방식은 개별 기능을 테스트하는 데 유용합니다. 모든 쿼리에 영향을 미치는 문제를 찾는 데도 유용할 수 있지만, "S 루프"가 최대화되는 이전 경우보다 낮은 신뢰도로 그렇습니다.

### 큰 "R 루프"와 "C 루프" 얻기

큰 결과 집합을 만드는 것은 쉽고 이러한 벤치마크의 일부가 되어야 합니다. 큰 결과 집합의 경우 모든 결함이 크게 증폭되므로 이러한 것들을 수정하는 것은 가치가 있습니다. 그러나 이러한 문제는 쿼리 작성 프로세스나 실행 프로세스가 아닌 실제 결과 집합에만 영향을 미칩니다. 물론, 대부분의 문은 삽입/업데이트 등이 아니라 쿼리일 것입니다. 그러나 이것은 염두에 두어야 합니다.

## 큰 "E 루프"에서의 문제에 대한 최적화

위의 모든 시나리오는 다른 최적화 세션이며 각각 별도의 블로그 글이 될 자격이 있습니다. 이 글에서는 H2 데이터베이스에서 단일 쿼리를 300만 번 실행할 때 발견되고 수정된 것을 설명합니다. H2 데이터베이스는 동일한 프로세스의 메모리에서 실행될 수 있어 jOOQ에 비해 가장 적은 추가 오버헤드를 가지므로 여기서 선택되었습니다 - 따라서 jOOQ의 오버헤드 기여가 프로파일링 세션/벤치마크에서 중요해집니다.

사실, 이러한 벤치마크에서 jOOQ(또는 Hibernate 등)이 이전에 많은 사람들이 했던 것처럼 JDBC 전용 솔루션에 비해 상당히 저조하게 수행되는 것처럼 보일 수 있습니다. 이것은 스스로에게 상기시켜야 할 중요한 순간입니다:

> "벤치마크는 실제 사용 사례를 반영하지 않습니다! 프로덕션 시스템에서 정확히 동일한 쿼리를 300만 번 실행하지 않을 것이고, 프로덕션 시스템은 H2에서 실행되지 않습니다. 벤치마크는 너무 많은 캐싱, 버퍼링의 혜택을 받아서 벤치마크만큼 빠르게 수행되지 않을 것입니다. 벤치마크에서 잘못된 결론을 도출하지 않도록 항상 주의하세요!"

이것은 꼭 말해야 하므로, 웹에서 찾는 모든 벤치마크를 의심의 눈으로 받아들이세요.

### 프로파일링되는 쿼리

```java
ctx.select(
      AUTHOR.FIRST_NAME,
      AUTHOR.LAST_NAME,
      BOOK.ID,
      BOOK.TITLE)
   .from(BOOK)
   .join(AUTHOR).on(BOOK.AUTHOR_ID.eq(AUTHOR.ID))
   .where(BOOK.ID.eq(1))
   .and(BOOK.TITLE.isNull().or(BOOK.TITLE.ne(randomValue)));
```

이 간단한 쿼리는 우스울 정도로 4개의 행과 4개의 열을 반환하므로 "R 루프"와 "C 루프"는 무시할 수 있습니다. 이 벤치마크는 데이터베이스가 실행 시간에 많이 기여하지 않는 경우 jOOQ 쿼리 실행의 오버헤드를 정말로 테스트하고 있습니다. 다시 말하지만, 실제 시나리오에서는 데이터베이스에서 훨씬 더 많은 오버헤드가 발생할 것입니다.

---

## 발견된 최적화 문제들

### 1. 상수 값의 인스턴스 할당

이 실수는 샘플링된 시간의 1.1%만 기여하여 엄청난 오버헤드를 차지하지는 않았지만, 저의 호기심을 자극했습니다. jOOQ 버전 3.10에서 jOOQ OFFSET / LIMIT 동작을 인코딩하는 SelectQueryImpl의 Limit 클래스는 바인드 변수인 이 DSL.val() 것을 계속 할당했습니다. 물론, limit은 바인드 변수와 함께 작동하지만, 이것은 jOOQ API 사용자가 LIMIT 절을 추가할 때가 아니라 SelectQueryImpl이 초기화될 때 발생했습니다.

원본 코드:
```java
private static final Field<Integer> ZERO              = zero();
private static final Field<Integer> ONE               = one();
private Field<Integer>              numberOfRowsOrMax =
    DSL.inline(Integer.MAX_VALUE);
```

해결책:
```java
private static final Param<Integer> MAX               =
    DSL.inline(Integer.MAX_VALUE);
private Field<Integer>              numberOfRowsOrMax = MAX;
```

이것이 명백히 더 좋습니다! 바인드 변수의 할당을 피할 뿐만 아니라, Integer.MAX_VALUE의 박싱도 피합니다(샘플링 스크린샷에서도 볼 수 있습니다).

이슈: https://github.com/jOOQ/jOOQ/issues/6635

### 2. 내부에서 리스트 복사

jOOQ는 (안타깝게도) 때때로 배열 간에 데이터를 복사합니다. 예를 들어 Strings를 jOOQ 래퍼 타입으로 래핑하거나, 숫자를 문자열로 변환하는 등. 이러한 루프 자체는 나쁘지 않지만, "S-E-R-C 루프" 어딘가 안에 있다는 것을 기억하세요. 따라서 문을 300만 번 실행할 때 이러한 복사 작업이 수억 번 실행될 수 있습니다.

QualifiedName 클래스는 우연한 수정이 부작용을 일으키지 않도록 반환하기 전에 인수를 복제했습니다:

원본 코드:
```java
private static final String[] nonEmpty(String[] qualifiedName) {
    String[] result;
    ...
    if (nulls > 0) {
        result = new String[qualifiedName.length - nulls];
        ...
    }
    else {
        result = qualifiedName.clone();
    }
    return result;
}
```

약간의 분석 후, 이 메서드의 소비자가 하나뿐이고 그 소비자를 떠나지 않는다는 것을 알 수 있었습니다. 따라서 clone 호출을 제거해도 안전합니다.

이슈: https://github.com/jOOQ/jOOQ/issues/6640

### 3. 루프에서 검사 실행

CombinedCondition 생성자에 비용이 많이 드는 오버헤드가 있습니다. 문제는 이것입니다:

원본 코드:
```java
CombinedCondition(Operator operator, Collection<? extends Condition> conditions) {
    ...
    for (Condition condition : conditions)
        if (condition == null)
            throw new IllegalArgumentException("The argument 'conditions' must not contain null");

    ...
    init(operator, conditions);
}
```

발생할 때 그냥 NPE와 함께 살면 어떨까요? 이것은 상당히 예상치 못한 일이어야 합니다(맥락상, jOOQ는 이와 같은 매개변수를 거의 확인하지 않으므로, 일관성을 위해서도 이것을 제거해야 합니다).

이슈: https://github.com/jOOQ/jOOQ/issues/6666

### 4. 리스트의 지연 초기화

JDBC API의 특성상 ThreadLocal 변수를 사용해야 하는데, 매우 안타깝게도 부모 SQLData 객체에서 자식으로 인수를 전달하는 것이 불가능합니다. 특히 Oracle TABLE/VARRAY와 OBJECT 타입의 중첩을 결합할 때 그렇습니다.

DefaultExecuteContext에서 생성자에 오버헤드가 있습니다:

원본 코드:
```java
BLOBS.set(new ArrayList<Blob>());
CLOBS.set(new ArrayList<Clob>());
SQLXMLS.set(new ArrayList<SQLXML>());
ARRAYS.set(new ArrayList<Array>());
```

쿼리 실행을 시작할 때마다 이러한 각 타입에 대해 리스트를 초기화합니다. 모든 변수 바인딩 로직은 가능한 할당된 BLOB 또는 CLOB 등을 등록하여 실행이 끝날 때 정리할 수 있도록 합니다(모든 사람이 알지 못하는 JDBC 4.0 기능입니다!):

원본 코드:
```java
static final void register(Blob blob) {
    BLOBS.get().add(blob);
}

static final void clean() {
    List<Blob> blobs = BLOBS.get();

    if (blobs != null) {
        for (Blob blob : blobs)
            JDBCUtils.safeFree(blob);

        BLOBS.remove();
    }
    ...
}
```

JDBC를 직접 사용하는 경우 Blob.free() 등을 호출하는 것을 잊지 마세요! 하지만 사실, 대부분의 경우 이러한 것들이 정말로 필요하지 않습니다. Oracle에서만, 그리고 일부 JDBC 제한으로 인해 TABLE / VARRAY 또는 OBJECT 타입을 사용하는 경우에만 필요합니다. 왜 다른 데이터베이스의 모든 사용자에게 이 오버헤드로 벌을 줍니까?

회귀를 도입할 위험이 있는 정교한 리팩토링 대신, 이러한 리스트를 지연 초기화할 수 있습니다. clean() 메서드는 그대로 두고, 생성자에서 초기화를 제거하고, register() 로직을 다음으로 교체합니다:

해결책:
```java
static final void register(Blob blob) {
    List<Blob> list = BLOBS.get();

    if (list == null) {
        list = new ArrayList<Blob>();
        BLOBS.set(list);
    }

    list.add(blob);
}
```

이슈: https://github.com/jOOQ/jOOQ/issues/6669

### 5. String.replace() 사용

이것은 주로 JDK 8에서만의 문제입니다. JDK 9는 내부적으로 더 이상 정규 표현식에 의존하지 않음으로써 문자열 교체를 수정했습니다. 그러나 JDK 8에서는(그리고 jOOQ는 여전히 Java 6을 지원하므로 이것이 관련됩니다), 문자열 교체가 정규 표현식을 통해 작동합니다.

JDK 8에서 `String.replace()`는 내부적으로 `Pattern.compile()`을 사용하여 정규 표현식 패턴을 컴파일합니다. 이는 단순한 문자열 교체에 대해 불필요한 오버헤드를 발생시킵니다. Pattern 컴파일은 상당한 할당 압력을 유발하며, 자주 호출되는 코드 경로에서는 이것이 측정 가능한 성능 저하로 이어집니다.

이슈: https://github.com/jOOQ/jOOQ/issues/6672

관련 글: [JDK String.replace() vs Apache Commons StringUtils.replace() 벤치마킹](https://blog.jooq.org/benchmarking-jdk-string-replace-vs-apache-commons-stringutils-replace/)

### 6. 비활성화될 SPI 등록

ExecuteListener SPI를 추상화하는 내부 ExecuteListeners 유틸리티가 있습니다. 사용자는 이러한 리스너를 등록하고 쿼리 렌더링, 변수 바인딩, 쿼리 실행 및 기타 생명주기 이벤트를 수신할 수 있습니다. 기본적으로 사용자에 의한 이러한 ExecuteListener는 없지만, 항상 하나의 내부 ExecuteListener가 있습니다:

원본 코드:
```java
private static ExecuteListener[] listeners(ExecuteContext ctx) {
    List<ExecuteListener> result = new ArrayList<ExecuteListener>();

    for (ExecuteListenerProvider provider : ctx.configuration()
                                               .executeListenerProviders())
        if (provider != null)
            result.add(provider.provide());

    if (!FALSE.equals(ctx.settings().isExecuteLogging()))
        result.add(new LoggerListener());

    return result.toArray(EMPTY_EXECUTE_LISTENER);
}
```

LoggerListener는 사용자가 해당 기능을 끄지 않는 한 기본적으로 추가됩니다. 이는 거의 항상 이 ArrayList를 얻고, 거의 항상 이 리스트를 반복하고, 거의 항상 이 LoggerListener를 호출한다는 것을 의미합니다. 하지만 이것이 무엇을 하나요? DEBUG 및 TRACE 수준에서 로깅합니다. 예를 들어:

```java
@Override
public void executeEnd(ExecuteContext ctx) {
    if (ctx.rows() >= 0)
        if (log.isDebugEnabled())
            log.debug("Affected row(s)", ctx.rows());
}
```

이것을 초기화하는 개선된 로직은 다음과 같습니다:

해결책:
```java
private static final ExecuteListener[] listeners(ExecuteContext ctx) {
    List<ExecuteListener> result = null;

    for (ExecuteListenerProvider provider : ctx.configuration()
                                               .executeListenerProviders())
        if (provider != null)
            (result = init(result)).add(provider.provide());

    if (!FALSE.equals(ctx.settings().isExecuteLogging())) {
        if (LOGGER_LISTENER_LOGGER.isDebugEnabled())
            (result = init(result)).add(new LoggerListener());
    }

    return result == null ? null : result.toArray(EMPTY_EXECUTE_LISTENER);
}
```

더 이상 ArrayList를 할당하지 않고(이것은 조급할 수 있습니다. JIT가 이 할당이 발생하지 않도록 재작성했을 수 있지만, 괜찮습니다), DEBUG 또는 TRACE 로깅이 활성화된 경우에만, 즉 어떤 작업을 수행할 경우에만 LoggerListener를 추가합니다.

이슈: https://github.com/jOOQ/jOOQ/issues/6747

### 7. 지연 할당이 작동하는 곳에서의 즉시 할당

때때로 동일한 정보의 두 가지 다른 표현이 필요합니다. "원시" 표현과 일부 목적을 위해 더 유용하게 전처리된 표현입니다. 이것은 예를 들어 QualifiedField에서 수행되었습니다:

원본 코드:
```java
private final Name          name;
private final Table<Record> table;

QualifiedField(Name name, DataType<T> type) {
    super(name, type);

    this.name = name;
    this.table = name.qualified()
        ? DSL.table(name.qualifier())
        : null;
}

@Override
public final void accept(Context<?> ctx) {
    ctx.visit(name);
}

@Override
public final Table<Record> getTable() {
    return table;
}
```

name은 정말로 이 클래스의 핵심입니다. SQL 문자열에서 자신을 생성하는 정규화된 이름입니다. Table 표현은 메타 모델을 탐색할 때 유용하지만, 이것은 jOOQ의 내부 및/또는 사용자 대면 코드에서 거의 수행되지 않습니다. 그러나 이 즉시 초기화는 비용이 많이 듭니다.

Name.qualifier() 호출에 의해 상당히 많은 UnqualifiedName[] 배열이 할당됩니다. 해당 table 참조를 non-final로 만들고 지연 계산할 수 있습니다:

해결책:
```java
private final Name              name;
private Table<Record>           table;

QualifiedField(Name name, DataType<T> type) {
    super(name, type);

    this.name = name;
}

@Override
public final Table<Record> getTable() {
    if (table == null)
        table = name.qualified() ? DSL.table(name.qualifier()) : null;

    return table;
}
```

name이 final이기 때문에 table을 "사실상 final"이라고 부를 수 있습니다 - 이러한 특정 타입이 jOOQ 내에서 불변이기 때문에 스레드 안전성 문제가 없을 것입니다.

이슈: https://github.com/jOOQ/jOOQ/issues/6755

---

## 결과

지금까지, (솔직히 Eclipse 외부에서 꽤 바쁜 머신에서 실행된) 프로파일러 세션을 기반으로 많은 쉬운 것들을 "개선"했습니다. 이것은 매우 과학적이지 않았습니다. 눈에 띌 정도로 높은 숫자를 가져서 제 관심을 끌었던 "병목 현상"을 추적했을 뿐입니다. 이것을 "마이크로 최적화"라고 하며, "S-E-R-C 루프"에 있는 경우에만 수고할 가치가 있습니다. 즉, 최적화하는 코드가 많이 실행되는 경우에만요.

많은 사람들이 사용하는 라이브러리인 jOOQ를 개발하는 저에게는 이러한 최적화로부터 모든 사람이 혜택을 받기 때문에 거의 항상 해당됩니다. 다른 많은 경우에는 이것을 "조급한 최적화"라고 부를 수 있습니다.

> "올바르게 만들고, 명확하게 만들고, 간결하게 만들고, 빠르게 만드세요. 그 순서대로." – Wes Dyer

하지만 일단 최적화하면 멈추지 말아야 합니다. 위의 많은 문제에 대해 개별 JMH 벤치마크를 수행하여 정말로 개선인지 확인했습니다. 그러나 때때로 JMH 벤치마크에서 개선처럼 보이지 않는 것이 더 큰 그림에서는 여전히 개선일 것입니다. JVM은 100단계 깊이의 모든 메서드를 인라인하지 않습니다. 알고리즘이 복잡하면 마이크로 최적화가 JMH 벤치마크에서는 효과가 없을 수 있지만 여전히 효과가 있을 수 있습니다. 불행히도 이것은 매우 정확한 과학이 아니지만, 충분한 직관으로 최적화할 올바른 지점을 찾을 것입니다.

제 경우, 두 패치 릴리스에 걸쳐 진행 상황을 검증했습니다: 3.10.0 -> 3.10.1 -> 3.10.2(아직 출시되지 않음), 전체 쿼리 실행(H2 부분 포함)에 대해 JMH 벤치마크를 실행하여. 위와 유사한 최적화 약 15개를 적용한 결과(약 2일 분량의 노력)는 다음과 같습니다:

### 벤치마크 결과

JDK 9 (9+181)

jOOQ 3.10.0 오픈 소스 에디션
```
Benchmark                          Mode   Cnt       Score      Error  Units
ExecutionBenchmark.testExecution   thrpt   21  101891.108 ± 7283.832  ops/s
```

jOOQ 3.10.2 오픈 소스 에디션
```
Benchmark                          Mode   Cnt       Score      Error  Units
ExecutionBenchmark.testExecution   thrpt   21  110982.940 ± 2374.504  ops/s
```

JDK 8 (1.8.0_145)

jOOQ 3.10.0 오픈 소스 에디션
```
Benchmark                          Mode   Cnt       Score      Error  Units
ExecutionBenchmark.testExecution   thrpt   21  110178.873 ± 2134.894  ops/s
```

jOOQ 3.10.2 오픈 소스 에디션
```
Benchmark                          Mode   Cnt       Score      Error  Units
ExecutionBenchmark.testExecution   thrpt   21  118795.922 ± 2661.653  ops/s
```

보시다시피, 두 JDK 버전 모두에서 대략 10%의 속도 향상을 얻었습니다. 흥미로운 점은 이 벤치마크에서 JDK 8이 JDK 9보다 약 10% 더 빠른 것 같다는 것입니다. 비록 이것은 제가 아직 고려하지 않은 다양한 것들 때문일 수 있으며, 이 논의의 범위를 벗어납니다.

---

## 결론

성능을 다루는 이 반복적 접근 방식은 라이브러리 작성자에게 확실히 가치가 있습니다:

1. 대표적인 벤치마크를 실행합니다(작업을 수백만 번 반복)
2. 프로파일링합니다
3. "병목 현상"을 추적합니다
4. 회귀 위험 없이 쉽게 수정할 수 있으면 수정합니다
5. 반복합니다
6. 잠시 후 JMH로 검증합니다

개별 개선은 측정하거나 올바르게 측정하기가 상당히 어렵습니다. 하지만 10-15개를 수행하면 합산되기 시작하고 중요해집니다. 10%는 차이를 만들 수 있습니다.

여러분의 댓글, 대안 기법, 대안 도구 등을 기대합니다! 이 글이 마음에 드셨다면 [Java에서의 Top 10 쉬운 성능 최적화](https://blog.jooq.org/top-10-easy-performance-optimisations-in-java/)도 좋아하실 것입니다.

---

## 관련 링크

- [Java Mission Control](https://www.oracle.com/technetwork/java/javaseproducts/mission-control/java-mission-control-1998576.html)
- [Java Microbenchmark Harness](http://openjdk.java.net/projects/code-tools/jmh/)
- [Oracle JDK 라이선스](https://www.oracle.com/technetwork/java/javase/terms/license/index.html)
- [OpenJDK JMC 기여](https://blogs.oracle.com/java-platform-group/faster-and-easier-use-and-redistribution-of-java-se)
- [Lateral Join에 대한 이전 글](https://blog.jooq.org/add-lateral-joins-or-cross-apply-to-your-sql-tool-chain/)
- [JIT 최적화에 대한 이전 글](https://blog.jooq.org/the-java-jit-compiler-is-darn-good-in-optimization/)
- [String.replace() 벤치마크 글](https://blog.jooq.org/benchmarking-jdk-string-replace-vs-apache-commons-stringutils-replace/)
- [쿼리 벤치마킹 글](https://blog.jooq.org/how-to-benchmark-alternative-sql-queries-to-find-the-fastest-query/)
- [관련 이슈](https://github.com/jOOQ/jOOQ/issues/4205)
- [Top 10 성능 최적화](https://blog.jooq.org/top-10-easy-performance-optimisations-in-java/)

---

## 댓글 섹션

SG의 댓글 (2017년 11월 29일 22:24):

아마도 Java 8과 9 사이의 성능 차이는 "JEP 248: G1을 기본 가비지 컬렉터로 만들기"로 설명될 수 있을 것입니다. 이전 GC를 사용하여 다시 시도해 보시겠습니까?

Lukas Eder의 답글 (2017년 11월 30일 12:05):

솔직히 인정해야겠는데, 그것을 확인하지 않았습니다. 반면에, String 클래스는 내용을 char[]가 아닌 byte[]로 저장하는 주요 내부 재작성을 거쳤습니다. 이것도 상당한 영향을 미칠 수 있습니다...
