# Java 8 금요일 선물: 로컬 트랜잭션 범위

> 원문: https://blog.jooq.org/java-8-friday-goodies-local-transaction-scope/

Data Geekery에서 우리는 Java 8에 열광하고 있으며, jOOQ의 유창한 API와 함께 람다 표현식을 사용하는 것에 흥분하고 있습니다. 우리는 몇 가지 Java 8 선물에 대해 블로깅해왔으며, 이제 새로운 블로그 시리즈를 시작합니다.

## Java 8 금요일

매주 금요일마다 Java 8의 새로운 튜토리얼과 함께 몇 가지 멋진 내용을 선보일 것입니다. 이 튜토리얼 스타일의 블로그 글은 lambda와 함께 제공되며, 전체 소스 코드를 GitHub에서 찾아볼 수 있습니다.

## 로컬 범위 지정

JavaScript 개발자들은 로컬 범위 지정에 익숙합니다. JavaScript는 함수만 새로운 범위를 시작하는 함수 범위만 알기 때문에 JavaScript 개발자들은 일반적으로 매우 특정한 패턴을 사용하여 로컬 범위를 수동으로 도입합니다:

```javascript
(function() {
    var local = function() {
        scoping();
    },
    scoping = function() {
        alert('If you really must');
    };
    local();
})();
```

어색함은 JavaScript의 디자인 패턴이 될 수 있습니다.

Java에서도 흥미로운 로컬 범위를 생성할 수 있습니다. 예를 들어:

```java
new Object() {
    void local() {
        scoping();
    }
    void scoping() {
        System.out.println("아야, 내 손가락. 타이핑이 너무 많아");
    }
}.local();
```

Java 8에서는 모든 것이 변하며, 로컬 범위 지정도 마찬가지입니다. 로컬 범위를 사용할 수 있는 가장 유용한 방법 중 하나를 살펴보겠습니다.

## 로컬 트랜잭션 범위

이 패턴은 이 예제에서 jOOQ를 사용하기 때문에 조금 편향되어 있지만, 트랜잭션 범위 내에서 작업을 실행하기 위해 JDBC를 전달하는 것과 같은 용도로도 재사용할 수 있습니다. 아이디어는 람다 표현식 내에서 트랜잭션 상태가 암시적으로 관리되는 것입니다. 트랜잭션을 시작할 필요 없이 람다 표현식 코드가 성공적으로 완료되면 트랜잭션이 커밋됩니다. 예외가 발생하면 트랜잭션이 롤백됩니다.

다음은 `Transactional` 함수형 인터페이스입니다:

```java
@FunctionalInterface
interface Transactional {
    void run(DSLContext ctx);
}
```

그리고 위의 함수형 인터페이스를 람다 표현식으로 사용하는 것을 지원하는 `TransactionRunner`입니다:

```java
class TransactionRunner {
    private final boolean silent;
    private final Connection connection;

    TransactionRunner(Connection connection) {
        this(connection, true);
    }

    TransactionRunner(Connection connection, boolean silent) {
        this.connection = connection;
        this.silent = silent;
    }

    void run(Transactional tx) {
        // 편의를 위해 트랜잭션 유틸리티에 jOOQ 사용
        final DefaultConnectionProvider c =
            new DefaultConnectionProvider(connection);
        final Configuration configuration =
            new DefaultConfiguration()
                .set(c).set(SQLDialect.H2);

        try {
            tx.run(DSL.using(configuration));
            c.commit();
        }
        catch (RuntimeException e) {
            c.rollback();
            System.err.println(e.getMessage());
            if (!silent)
                throw e;
        }
    }
}
```

## 설정

위의 모든 것은 프레임워크 코드이며, 한 번 작성하면 개발자들이 다시는 이것에 대해 걱정할 필요가 없습니다. 이제 어떻게 사용하는지 살펴보겠습니다. 먼저 연결을 얻고 `TransactionRunner`를 설정해야 합니다:

```java
public static void main(String[] args) throws Exception {
    Class.forName("org.h2.Driver");
    try (Connection c = DriverManager.getConnection(
            "jdbc:h2:~/test-scope-goodies", "sa", "")) {
        c.setAutoCommit(false);
        TransactionRunner silent = new TransactionRunner(c);

        // 트랜잭션 코드가 이어집니다...
    }
}
```

## 사용법

이제 이 멋진 API를 살펴보세요. 이 블로그 글에 적합한 것처럼, 새 테이블을 만드는 것으로 시작합니다:

```java
// 이 트랜잭션은 성공합니다
silent.run(ctx -> {
    ctx.execute("drop table if exists person");
    ctx.execute("create table person(" +
                "  id integer," +
                "  first_name varchar(50)," +
                "  last_name varchar(50)," +
                "  primary key(id)"+
                ")");
});
```

멋지지 않나요? `silent` 러너를 사용하여 트랜잭션을 열고 위의 DDL을 실행합니다. 트랜잭션이 성공하면 커밋됩니다.

이제 약간의 위반을 시도해 보겠습니다:

```java
// 이 트랜잭션은 기본 키 위반으로 인해 실패합니다
silent.run(ctx -> {
    ctx.execute("insert into person values(1, 'John', 'Smith');");
    ctx.execute("insert into person values(1, 'Steve', 'Adams');");
    // 실패 - 중복 기본 키로 인해 롤백됩니다
});

// 이 트랜잭션은 다시 성공합니다
silent.run(ctx -> {
    ctx.execute("insert into person values(2, 'Jane', 'Miller');");
    // 성공합니다
});
```

## 결과 확인

정말로 무슨 일이 일어났는지 확인해 보겠습니다:

```java
// 테이블의 내용을 확인합니다
silent.run(ctx -> {
    System.out.println(ctx.fetch("select * from person"));
});
```

위의 코드는 다음을 출력합니다:

```
+----+----------+---------+
|  ID|FIRST_NAME|LAST_NAME|
+----+----------+---------+
|   2|Jane      |Miller   |
+----+----------+---------+
```

결과는 두 번째 트랜잭션에서 삽입이 실패하고 롤백되었으며, 세 번째 트랜잭션이 커밋되었음을 보여줍니다.

## 중첩 트랜잭션

이 접근 방식이 내부 작업을 수행하는 메서드 내에서 중첩된 호출을 허용해야 한다면, `TransactionRunner`를 중첩 수준을 추적하도록 조정할 수 있으며, 각 새로운 중첩 트랜잭션 수준에서 새 세이브포인트를 생성하고 나중에 적절히 커밋하거나 롤백하여 세이브포인트 기능을 구현할 수 있습니다.

## 결론

이 모든 것들은 바닐라 Java 7으로도 수행할 수 있습니다. 하지만 람다 표현식 덕분에 클라이언트 코드가 훨씬 더 깔끔하고 읽기 쉬워집니다. 트랜잭션 관리를 위한 반복적인 try-finally 블록을 제거하면서 깨끗하고 읽기 쉬운 코드를 유지할 수 있습니다.

다음 시간에는 Java 8을 사용한 로컬 캐싱 범위를 살펴볼 것입니다.
