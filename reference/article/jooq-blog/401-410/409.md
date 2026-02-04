# jOOQ와 Java 8의 CompletableFuture로 비동기 SQL 실행

> 원문: https://blog.jooq.org/asynchronous-sql-execution-with-jooq-and-java-8s-completablefuture/

리액티브 프로그래밍은 새로운 유행어인데, 이는 본질적으로 비동기 프로그래밍 또는 메시징을 의미합니다. 사실 함수형 문법은 비동기 실행 체인을 구조화하는 데 큰 도움이 되며, 오늘은 jOOQ와 새로운 CompletableFuture API를 사용하여 Java 8에서 이를 어떻게 할 수 있는지 살펴보겠습니다. 실제로 이는 매우 간단합니다:

```java
// 비동기 호출 체인을 시작합니다
CompletableFuture

    // 이 람다는 삽입된 행의 수를 나타내는
    // int 값을 제공합니다
    .supplyAsync(() -> DSL
        .using(configuration)
        .insertInto(AUTHOR, AUTHOR.ID, AUTHOR.LAST_NAME)
        .values(3, "Hitchcock")
        .execute()
    )

    // 이것은 새로 삽입된 저자에 대한
    // AuthorRecord 값을 제공합니다
    .handleAsync((rows, throwable) -> DSL
        .using(configuration)
        .fetchOne(AUTHOR, AUTHOR.ID.eq(3))
    )

    // 이것은 행의 수를 나타내는 int 값을
    // 제공해야 하지만, 실제로는 제약 조건
    // 위반 예외를 던집니다
    .handleAsync((record, throwable) -> {
        record.changed(true);
        return record.insert();
    })

    // 이것은 삭제된 행의 수를 나타내는
    // int 값을 제공합니다
    .handleAsync((rows, throwable) -> DSL
        .using(configuration)
        .delete(AUTHOR)
        .where(AUTHOR.ID.eq(3))
        .execute()
    )

    // 이것은 호출 스레드에게 모든 체인된
    // 실행 단위가 실행될 때까지 기다리라고 지시합니다
    .join();
```

여기서 실제로 무슨 일이 일어났을까요? 특별한 것은 없습니다. 4개의 실행 블록이 있습니다:

1. 새로운 AUTHOR를 삽입하는 블록
2. 동일한 AUTHOR를 다시 가져오는 블록
3. 새로 가져온 AUTHOR를 다시 삽입하는 블록 (예외를 던집니다)
4. 던져진 예외를 무시하고 AUTHOR를 다시 삭제하는 블록

마지막으로, 실행 체인이 구성되면 호출 스레드는 `CompletableFuture.join()` 메서드를 사용하여 전체 체인에 합류합니다. 이 메서드는 본질적으로 `Future.get()` 메서드와 동일하지만, 검사 예외(checked exception)를 던지지 않는다는 점이 다릅니다.

## 다른 API와의 비교

Scala의 Slick과 같은 다른 API들은 `flatMap()`과 같은 호출을 통해 "표준 API"로 유사한 것들을 구현했습니다. 우리는 현재 이러한 API들을 모방하지 않을 것입니다. 새로운 Java 8 API가 네이티브 Java 사용자들에게 훨씬 더 관용적이 될 것이라고 믿기 때문입니다. 특히 SQL을 실행할 때는 커넥션 풀링과 트랜잭션을 올바르게 처리하는 것이 핵심입니다. 비동기적으로 체인된 실행 블록들의 의미론과 이들이 트랜잭션과 어떻게 관련되는지는 매우 미묘합니다. 만약 트랜잭션이 하나 이상의 블록에 걸쳐야 한다면, jOOQ의 `Configuration`과 그 안에 포함된 `ConnectionProvider`를 통해 직접 이를 인코딩해야 합니다.

## 블로킹 JDBC

분명히 이러한 솔루션들에는 항상 하나의 블로킹 장벽이 있을 것이며, 그것은 바로 JDBC 자체입니다 - 이것을 비동기 API로 전환하는 것은 매우 어렵습니다. 사실 진정으로 비동기 쿼리 실행과 커서를 지원하는 데이터베이스는 거의 없습니다. 대부분의 경우 단일 데이터베이스 세션은 한 번에 하나의 쿼리를 위해 하나의 스레드에서만 사용될 수 있기 때문입니다. 우리는 여러분의 비동기 SQL 쿼리 요구 사항에 대해 알고 싶으니, 이 글에 자유롭게 댓글을 남겨주세요!
