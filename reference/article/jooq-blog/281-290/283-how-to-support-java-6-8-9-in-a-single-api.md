# 단일 API에서 Java 6, 8, 9를 지원하는 방법

> 원문: https://blog.jooq.org/how-to-support-java-6-8-9-in-a-single-api/

jOOQ 3.7에서 우리는 마침내 Java 8 기능에 대한 공식 지원을 추가했습니다. 이를 통해 다음과 같은 많은 멋진 개선사항이 가능해졌습니다:

결과 스트림 생성

```java
try (Stream<Record2<String, String>> stream =
     DSL.using(configuration)
        .select(FIRST_NAME, LAST_NAME)
        .from(PERSON)
        .stream()) {

    List<String> people =
    stream.map(p -> p.value1() + " " + p.value2())
          .collect(Collectors.toList());
}
```

비동기 문장 호출 (jOOQ 3.8+)

```java
CompletionStage<Record> result =
DSL.using(configuration)
   .select(...)
   .from(COMPLEX_TABLE)
   .fetchAsync();

result.thenComposing(r -> ...);
```

하지만 당연히 우리는 오래된 애플리케이션 서버 등을 사용하기 때문에 Java 6에 묶여 있는 유료 고객들을 실망시키고 싶지 않았습니다.

## 단일 API에서 여러 Java 버전을 지원하는 방법

이것이 우리가 상용 고객을 위해 [Java 6 버전의 jOOQ를 계속 배포하는](https://www.jooq.org/download/versions) 이유입니다. 어떻게 했을까요? 매우 쉽습니다. 우리의 상용 코드베이스(메인 코드베이스)에는 다음 예제와 같은 수많은 "플래그"가 포함되어 있습니다:

```java
public interface Query
extends
    QueryPart,
    Attachable
    /* [java-8] */, AutoCloseable /* [/java-8] */
{

    int execute() throws DataAccessException;

    /* [java-8] */
    CompletionStage<Integer> executeAsync();
    CompletionStage<Integer> executeAsync(Executor executor);
    /* [/java-8] */

}
```

(물론 `AutoCloseable`은 이미 Java 7에서 사용 가능했지만, 우리는 Java 7 버전이 없습니다.)

jOOQ를 빌드할 때, 전처리기를 사용하여 소스 파일에서 로직을 제거한 후 여러 번 빌드합니다:

- 상용 Java 8 버전이 먼저 있는 그대로 빌드됩니다
- 상용 Java 6 버전은 `[java-8]`과 `[/java-8]` 마커 사이의 모든 코드를 제거하여 두 번째로 빌드됩니다
- 상용 무료 체험판 버전은 상용 버전에 일부 코드를 추가하여 빌드됩니다
- 오픈 소스 버전은 `[pro]`와 `[/pro]` 마커 사이의 모든 코드를 제거하여 세 번째로 빌드됩니다

## 이 접근 방식의 장점

이 접근 방식은 다른 방식들과 비교하여 여러 가지 장점이 있습니다:

- 원본 상용 소스 코드라는 단일 진실 공급원(single source of truth)만 가지고 있습니다.
- 라인 번호가 모든 다른 버전에서 동일합니다
- API가 어느 정도 호환됩니다
- 클래스 로딩이나 리플렉션을 통한 마법이 필요 없습니다

단점은 다음과 같습니다:

- 여러 저장소가 있어서 저장소에 커밋하는 것이 조금 느립니다.
- 다른 버전들을 여러 번 빌드하고 통합 테스트해야 하므로 릴리스 배포에 더 오랜 시간이 걸립니다
- 때때로 마커 추가를 잊어버려서 Java-6 야간 빌드가 실패할 때 다시 빌드해야 합니다
- Java 6 버전에 포함된 일반 코드(대부분의 코드)에서는 여전히 람다 표현식을 사용할 수 없습니다

우리의 의견으로는 장점이 확실히 더 큽니다. 고객들이 최신 Java 기능을 사용할 수 있고, 이전 버전에 묶여 있는 고객들도 여전히 최신 jOOQ 버전으로 업그레이드할 수 있다면, 우리가 최첨단 Java 기능을 구현하지 못해도 괜찮습니다.

우리는 기존 사용자에 대한 타협 없이 JDK 9 기능인 모듈성과 [새로운 Flow API](http://hg.openjdk.java.net/jdk9/dev/jdk/file/tip/src/java.base/share/classes/java/util/concurrent/Flow.java)를 지원하기를 기대하고 있습니다.
