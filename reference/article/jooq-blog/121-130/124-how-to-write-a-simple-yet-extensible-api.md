# 간단하면서도 확장 가능한 API를 작성하는 방법

> 원문: https://blog.jooq.org/how-to-write-a-simple-yet-extensible-api/

간단한 API를 작성하는 것은 그 자체로 이미 하나의 예술입니다. 마크 트웨인이 말했듯이:

> "짧은 편지를 쓸 시간이 없어서 긴 편지를 썼습니다."

하지만 초보자와 대부분의 사용자를 위해 API를 간단하게 유지하면서 동시에 파워 유저를 위해 확장 가능하게 만드는 것은 훨씬 더 어려운 도전처럼 보입니다. 하지만 정말 그럴까요?

## "확장 가능하다"는 것은 무엇을 의미하는가?

구체적인 예를 들어 "확장 가능하다(extensible)"가 무엇을 의미하는지 설명해 보겠습니다. jOOQ에서는 SQL 조건자(predicate)를 다음과 같이 작성할 수 있습니다:

```java
ctx.select(T.A, T.B)
   .from(T)
   .where(T.C.eq(1)) // 여기에 바인드 값이 있는 조건자
   .fetch();
```

기본적으로 jOOQ는 바인드 변수를 사용하는 SQL을 생성합니다:

```sql
SELECT t.a, t.b
FROM t
WHERE t.c = ?
```

하지만 때로는 이렇게 하고 싶지 않을 수 있습니다. 바인드 값 대신 인라인 값을 사용하고 싶을 때가 있습니다.

### 쿼리 단위 접근 방식

특정 쿼리에서만 인라인 값을 사용하려면 `inline()` 함수를 사용할 수 있습니다:

```java
ctx.select(T.A, T.B)
   .from(T)
   .where(T.C.eq(inline(1))) // 여기에 바인드 값이 없는 조건자
   .fetch();
```

### 전역 접근 방식

모든 쿼리에서 인라인 값을 사용하려면 `Configuration` 객체를 통해 전역적으로 설정할 수 있습니다:

```java
ctx2 = DSL.using(ctx
    .configuration()
    .derive()
    .set(new Settings()
    .withStatementType(StatementType.STATIC_STATEMENT)));

// 이제 기존 DSLContext 대신 이 새로운 DSLContext를 사용합니다
ctx2.select(T.A, T.B)
    .from(T)
    .where(T.C.eq(1)) // 더 이상 바인드 변수가 아닙니다
    .fetch();
```

## 이러한 확장성을 제공하는 다양한 접근 방식

### ThreadLocal 안티패턴

어떤 사람들은 ThreadLocal을 사용하여 컨텍스트 정보를 전달하려고 할 수 있습니다. 이 접근 방식에 대해 강력히 경고합니다:

1. 해킹에 불과합니다: 쉽게 깨지고 유지보수가 어려워집니다
2. 비동기/리액티브 환경에서 실패합니다: 실행이 스레드 간에 점프하는 async/reactive/parallel stream 컨텍스트에서는 작동하지 않습니다
3. 근본적으로 잘못된 설계입니다: 이는 올바른 설계가 아닌 해킹입니다

### 의존성 주입(Dependency Injection)

Spring과 같은 프레임워크는 어노테이션 기반 의존성 주입을 사용합니다. JAX-RS는 `@Context`와 `@QueryParam` 어노테이션을 사용하여 이를 보여줍니다:

```java
@GET
@Produces("text/plain")
@Path("/api")
public String method(
    @Context HttpServletRequest request,
    @QueryParam("arg") String arg
) {
    ...
}
```

이 방식은 정적 환경에서는 잘 작동하지만 "어노테이션 기반 마법"에 의존합니다.

### SPI(Service Provider Interface) - 권장 접근 방식

jOOQ는 `Configuration` 객체를 확장 포인트의 중앙 집중식 위치로 사용합니다. 사용자는 이 `Configuration`에 커스텀 구현을 등록할 수 있으며, 이는 간단한 기본 동작에 영향을 주지 않습니다.

jOOQ의 주요 SPI들은 다음과 같습니다:

#### Clock

`System.currentTimeMillis()` 대신 커스텀 시간 소스를 사용할 수 있게 해줍니다. 테스트나 특정 시간대 처리에 유용합니다.

#### ConnectionProvider

JDBC Connection의 라이프사이클을 관리합니다. `acquire()`와 `release()` 메서드를 통해 커넥션 획득과 해제를 제어합니다.

#### ExecuteListener

jOOQ 쿼리 관리 라이프사이클 전체에 연결할 수 있는 매우 유용하고 간단한 방법입니다. SQL 문자열 생성부터 JDBC 문 준비, 변수 바인딩, 실행, 결과 집합 가져오기까지 모든 단계에 훅을 걸 수 있습니다. 로깅 및 문 패칭에 사용됩니다.

#### ExecutorProvider

비동기 작업을 위한 표준 JDK `Executor`를 제공합니다.

#### MetaProvider

데이터베이스 메타데이터 조회를 처리합니다.

#### RecordMapperProvider와 RecordUnmapperProvider

jOOQ Record와 Java 클래스 간의 매핑을 커스터마이즈합니다.

#### TransactionProvider

트랜잭션 관리를 커스터마이즈할 수 있게 해줍니다.

### Configuration 라이프사이클 유연성

사용자는 `Configuration`의 범위를 다음과 같이 관리할 수 있습니다:

- 쿼리 단위(per-query)
- 스레드 단위(per-thread)
- 요청 단위(per-request)
- 세션 단위(per-session)
- 애플리케이션 단위(per-application)

jOOQ는 구현 선택에 대해 중립적입니다. 스레딩 모델이나 배포 컨텍스트에 대한 가정을 강요하지 않습니다.

## 설계 원칙

SPI 접근 방식이 이러한 목표를 달성하는 방법:

- 기본값을 간단하고 직관적으로 유지합니다: 대부분의 사용자는 확장 포인트를 알 필요가 없습니다
- 확장 포인트를 명시적이고 발견 가능하게 만듭니다: 파워 유저는 어디를 봐야 하는지 알 수 있습니다
- 유연한 라이프사이클 관리를 허용합니다: 쿼리별, 요청별, 애플리케이션별
- 마법이나 암시적 동작을 요구하지 않습니다: 어노테이션 기반 마법 대신 명시적이고 타입이 지정된 SPI를 사용합니다

"SPI를 매우 쉽게 발견할 수 있게 만드세요" - 구현이 필요한 명시적 타입을 사용하면 문서화 부담이 줄어듭니다. 사용자는 인터페이스를 보고 무엇을 구현해야 하는지 즉시 알 수 있습니다.

## 결론

간단한 API를 작성하는 것은 어렵습니다. 하지만 간단한 방식으로 확장 가능하게 만드는 것은 그렇지 않습니다.

핵심 권장 사항은 어노테이션 기반 마법 대신 명시적이고 타입이 지정된 SPI를 통해 확장 포인트를 쉽게 발견할 수 있게 만드는 것입니다. 이렇게 하면 문서화 부담이 줄어들고 파워 유저가 핵심 API를 오염시키지 않으면서 동작을 커스터마이즈할 수 있는 발견 가능하고 유지보수 가능한 방법을 제공합니다.

잘 설계된 단순함은 자연스럽게 확장성을 가능하게 합니다. 선택적 매개변수로 API 명확성을 희생하는 대신, 명시적 SPI가 있는 중앙 구성 포인트가 파워 유저에게 핵심 API를 오염시키지 않으면서 동작을 커스터마이즈할 수 있는 발견 가능하고 유지보수 가능한 방법을 제공합니다.

"단순함, 발견 가능성, 일관성, 편의성"이 훌륭한 API를 정의합니다.
