# DAO를 쓸 것인가, 쓰지 않을 것인가

> 원문: https://blog.jooq.org/to-dao-or-not-to-dao/

2023년 12월 6일, lukaseder 작성

jOOQ의 `DAO` API는 jOOQ에서 가장 논쟁이 많은 기능 중 하나입니다. 처음 구현되었을 때, 이것은 단순히 다음과 같은 이유로 구현되었습니다:

* 구현하기가 너무 쉬웠기 때문에
* 단순한 CRUD 작업에 매우 유용해 보였기 때문에
* 많은 개발자들이 원하는 것이었기 때문에

세 번째 항목에 대한 강력한 힌트는 Spring Data의 리포지토리 "패턴"이 얼마나 인기 있는지를 보면 알 수 있습니다. 많은 개발자들은 개별 쿼리에 대해 깊이 고민하지 않고, 단지 빠르게 데이터를 가져오고 저장하기를 원합니다.

> 재미있는 사실은 대부분의 사람들이 Spring Data를 단순한 CRUD 용도로만 사용한다는 것입니다. 이 프레임워크가 DDD에 대한 강한 의견과 그에 수반되는 개념들(애그리거트, 애그리거트 루트 등)을 기반으로 만들어졌음에도 불구하고 말입니다.

jOOQ `DAO`는 단순히 다음으로 구성되어 있기 때문에 구현이 쉬웠습니다:

* 몇 가지 공통 메서드를 가진 제네릭 `DAO` API
* 생성된 POJO와 일부 보조 쿼리 메서드에 대한 바인딩을 구현하는, 테이블마다 생성되는 클래스

다시 말해, 모든 테이블(예: `ACCOUNT`)에 대해 "무료로" `Account` POJO와 "무료로" `AccountDao` DAO를 얻게 되며, 다음과 같이 사용할 수 있습니다:

```java
// DAO는 보통 어떤 방식으로든 주입됩니다
@Autowired
AccountDao dao;

// 그리고:
dao.insert(new Account(1, "name"));
Account account = dao.findById(1);
```

꽤 유용해 보이지 않나요?

## 대신 SQL을 사용하는 것은 어떨까요?

Spring Data가 (철저한 DDD 적용보다는) 주로 빠른 데이터 접근을 위해 사용되는 것처럼, DAO도 마찬가지입니다. 그리고 두 접근 방식 모두 사용자들이 데이터 집합 관점에서 개별 쿼리를 고민하기보다는 빠른 접근을 선호하도록 훈련시킵니다. 정말 안타까운 일입니다!

DAO나 리포지토리를 사용하기 시작하면, 다음과 같은 코드를 얼마나 많이 보게 되나요?

```java
for (Account account :
    accountDao.fetchByType("Some Type")
) {
    for (Transaction transaction :
        transactionDao.fetchByTransactionId(account.getId()
    ) {
        doSomething(transaction);
    }
}
```

위 코드 스니펫에서 두려운 N+1 문제가 나타나고 있습니다. *모든* 계정에 대해 트랜잭션을 가져오는 쿼리를 실행하고 있기 때문입니다!

안타깝게도, 두 개의 중첩 루프가 코드의 같은 위치에 있는 위의 예시처럼 명확하지 않은 경우가 많습니다. 많은 경우 이러한 루프는 "재사용 가능한" 메서드 안에 숨겨져 있으며, 깊이 고민하지 않고 루프 안에서 호출됩니다.

사실 다음 쿼리를 작성하는 것이 그렇게 어렵지 않을 텐데 말입니다:

```java
for (Transaction transaction : ctx
    .selectFrom(TRANSACTION)
    .where(exists(
        selectOne()
        .from(TRANSACTION.account())
        .where(TRANSACTION.account().TYPE.eq("Some Type"))
    ))
    .fetchInto(Transaction.class)
) {
    doSomething(transaction);
}
```

꽤 명확해 보이지 않나요?

> 위 예시는 jOOQ 3.19의 암시적 경로 상관관계(implicit path correlation)라는 기능을 사용하고 있습니다. 상관 서브쿼리를 더 장황하지만 동등한 조건자를 추가하는 대신, 암시적 조인 경로 `TRANSACTION.account()`를 사용하여 표현할 수 있습니다.

한 단계 더 나아가면, 프로젝션도 최적화할 수 있을 가능성이 높습니다. `TRANSACTION`의 모든 컬럼이 필요하지 않을 수도 있기 때문입니다.

## 쓰기 작업에서의 추가 최적화

사실, `doSomething()`을 살펴봅시다. 이것도 `DAO`를 사용하고 있을 수 있습니다:

```java
void doSomething(Transaction transaction) {
    transaction.setSomeCounter(transaction.getSomeCounter + 1);
    transactionDao.update(transaction);
}
```

그래서 쿼리에서 N+1 문제가 발생했을 뿐만 아니라, 전체 `UPDATE` 문을 다음과 같이 (배치가 아닌) 벌크로 구현할 수 있었습니다:

```java
ctx
    .update(TRANSACTION)
    .set(TRANSACTION.SOME_COUNTER, TRANSACTION.SOME_COUNTER.plus(1))
    .where(exists(
        selectOne()
        .from(TRANSACTION.account())
        .where(TRANSACTION.account().TYPE.eq("Some Type"))
    ))
    .execute();
```

훨씬 더 빠를 뿐만 아니라, 훨씬 더 깔끔해 보이지 않나요?

## DAO를 쓸 것인가, 쓰지 않을 것인가

기억하세요: `DAO` API는 좋은 것이기 때문에 jOOQ에 추가된 것이 아닙니다. 구현과 사용이 쉬웠고, 인기 있는 "패턴"이었기 때문에 추가된 것입니다. 이것은 *매우* 원시적인 CRUD에만 유용한 *매우* 제한된 API입니다.

하지만 jOOQ의 가장 큰 강점은 `DAO`에 있지 않습니다. 표준화된 그리고 벤더별 SQL 지원의 방대한 양, 동적 SQL 기능, 저장 프로시저 지원, 그 외 훨씬 더 많은 것에 있습니다. `DAO`가 리포지토리를 사용하던 사람들을 끌어들일 수 있지만, 리포지토리와 같은 문제를 겪습니다:

> "진지한 데이터베이스 상호작용에 충분한 경우가 거의 없다는 단순한 사실."

그러므로, 꼭 필요하다면 `DAO` API를 사용하되, 절제하여 사용하세요. 예를 들어 실제 쿼리를 구현하는 더 정교하고 전문화된 DAO 하위 클래스의 공통 기본 클래스로 사용하는 것이 좋습니다. 그리고 SQL은 단순한 CRUD 이상의 훨씬 더 많은 것이라는 점을 항상 기억하세요.
