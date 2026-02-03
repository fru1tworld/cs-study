# jOOQ에서의 중첩 트랜잭션

> 원문: https://blog.jooq.org/nested-transactions-in-jooq/

## 소개

jOOQ 3.4부터 jOOQ에서 JDBC 위에 트랜잭션 로직을 단순화하는 API가 존재했으며, jOOQ 3.17과 [#13502](https://github.com/jOOQ/jOOQ/issues/13502)부터는 리액티브 애플리케이션을 위해 R2DBC 위에서도 동등한 API가 제공된다.

jOOQ는 암묵적인 어노테이션 기반 접근 방식이 아닌, 명시적이고 API 기반의 로직을 사용하여 트랜잭션을 구현한다. 이 글에서는 jOOQ가 트랜잭션 API를 어떻게 설계했는지, 그리고 왜 Spring의 `Propagation.NESTED` 시맨틱을 기본값으로 사용하는지를 살펴본다.

## JDBC의 기본값을 따르다

JDBC와 R2DBC에서 독립적인 문(statement)은 기본적으로 비트랜잭션이며 자동 커밋된다. jOOQ는 이 패턴을 따른다:

```java
ctx.insertInto(BOOK)
   .columns(BOOK.ID, BOOK.TITLE)
   .values(1, "Beginning jOOQ")
   .values(2, "jOOQ Masterclass")
   .execute();
```

그러나 대부분의 실제 사용 사례에서는 트랜잭션 로직이 필요하다.

## 트랜잭션 람다

jOOQ는 람다 기반의 트랜잭션 API를 제공한다:

```java
ctx.transaction(trx -> {
    trx.dsl()
       .insertInto(AUTHOR)
       .columns(AUTHOR.ID, AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME)
       .values(1, "Tayo", "Koleoso")
       .values(2, "Anghel", "Leonard")
       .execute();

    trx.dsl()
       .insertInto(BOOK)
       .columns(BOOK.ID, BOOK.AUTHOR_ID, BOOK.TITLE)
       .values(1, 1, "Beginning jOOQ")
       .values(2, 2, "jOOQ Masterclass")
       .execute();
});
```

정상적으로 완료되면 커밋되고, 예외가 발생하면 롤백된다. Spring의 `@Transactional`과 유사하지만 명시적이다.

## 제어 흐름을 직접 관리할 수 있다

개발자는 복구 가능한 예외를 우아하게 처리할 수 있다:

```java
ctx.transaction(trx -> {
    try {
        trx.dsl()
           .insertInto(AUTHOR)
           .columns(AUTHOR.ID, AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME)
           .values(1, "Tayo", "Koleoso")
           .values(2, "Anghel", "Leonard")
           .execute();
    }
    catch (DataAccessException e) {
        if (e.sqlStateClass() != C23_INTEGRITY_CONSTRAINT_VIOLATION)
            throw e;
    }

    trx.dsl()
       .insertInto(BOOK)
       .columns(BOOK.ID, BOOK.AUTHOR_ID, BOOK.TITLE)
       .values(1, 1, "Beginning jOOQ")
       .values(2, 2, "jOOQ Masterclass")
       .execute();
});
```

무시된 예외가 발생하더라도 트랜잭션은 계속 진행된다.

## 트랜잭션 전파

Spring과 Jakarta EE는 다양한 전파 모드를 제공한다. Spring은 기본값으로 `REQUIRED`를 사용하는 반면, jOOQ는 `NESTED`를 기본값으로 사용한다. 저자는 `NESTED`가 더 논리적이라고 주장한다:

> "NESTED 트랜잭션 단위는 진정한 의미의 트랜잭션이다. REQUIRED 트랜잭션 단위는 최상위 레벨에서 호출되는지 중첩된 범위에서 호출되는지에 따라 데이터를 이상한 상태로 남길 수 있다."

### 다른 프레임워크가 REQUIRED를 사용하는 이유

가능한 이유는 다음과 같다:

- `NESTED`는 SAVEPOINT 지원이 필요하다 (모든 RDBMS에서 보편적으로 지원되지는 않음)
- SAVEPOINT 연산에는 오버헤드가 수반된다
- 프레임워크 설계자들이 정확성보다 편의성을 우선시했다
- JPA 엔티티가 중첩 트랜잭션에서 손상될 수 있다

### jOOQ의 관점

jOOQ는 명시적이고 올바른 시맨틱을 우선시한다. 다음 코드 패턴을 살펴보자:

```java
@Transactional
void tx() {
    tx1();
    try {
        tx2();
    }
    catch (Exception e) {
        log.info(e);
    }
    continueWorkOnTx1();
}

@Transactional
void tx1() { ... }

@Transactional
void tx2() { ... }
```

프로그래머의 의도는 다음과 같아 보인다: `tx2()`를 독립적으로 시도하고, 실패하면 로그를 남기고 계속 진행한다. 그러나 Spring의 `REQUIRED`를 사용하면 `tx1()`과 `tx2()` 모두 완전히 롤백되어 외부 트랜잭션이 손상된다. 이는 코드의 명백한 의도와 모순된다.

## jOOQ는 NESTED 시맨틱을 구현한다

jOOQ는 명시적인 중첩 트랜잭션 지원을 제공한다:

```java
ctx.transaction(trx -> {
    trx.dsl().transaction(trx1 -> {
        // ...
    });

    try {
        trx.dsl().transaction(trx2 -> {
            // ...
        });
    }
    catch (Exception e) {
        log.info(e);
    }

    continueWorkOnTrx1(trx);
});
```

`trx2`가 실패하면 `trx2`만 롤백되며, `trx1`과 외부 작업은 영향을 받지 않는다. "이 외의 다른 동작을 원할 수는 없을 것이다. 만약 그렇다면 애초에 트랜잭션을 중첩하지 않았을 것이기 때문이다."

## R2DBC 트랜잭션

jOOQ 3.17 이상에서는 블로킹 API 대신 Publisher를 사용하여 동일한 시맨틱으로 R2DBC를 지원한다:

```java
Flux<?> flux = Flux.from(ctx.transactionPublisher(trx -> Flux
    .from(trx.dsl()
        .insertInto(AUTHOR)
        .columns(AUTHOR.ID, AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME)
        .values(1, "Tayo", "Koleoso")
        .values(2, "Anghel", "Leonard"))
    .thenMany(trx.dsl()
        .insertInto(BOOK)
        .columns(BOOK.ID, BOOK.AUTHOR_ID, BOOK.TITLE)
        .values(1, 1, "Beginning jOOQ")
        .values(2, 2, "jOOQ Masterclass"))
}));
```

중첩된 리액티브 트랜잭션은 `thenMany()`와 같은 스트림 결합자를 사용하여 유사한 패턴으로 구현된다.

## 결론

"트랜잭션 중첩은 때때로 유용하다. jOOQ에서는 모든 것이 일반적으로 명시적이기 때문에 트랜잭션 전파가 Jakarta EE나 Spring에서만큼 큰 주제가 되지 않는다."

jOOQ는 명시적인 NESTED 시맨틱을 기본값으로 선택했다. 이는 개발자가 실제로 코드를 작성하는 방식에 부합하며, 암묵적인 REQUIRED 시맨틱보다 더 예측 가능하고 올바른 동작을 제공하기 때문이다.
