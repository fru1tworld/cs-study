# 데이터베이스를 쉽게 모킹하기

> 원문: https://blog.jooq.org/easy-mocking-of-your-database/

테스트 주도 개발을 올바르게 사용하면 여러 가지 이점이 있습니다. 소프트웨어 품질 향상, 프로세스 개선, 개발자 경험 향상 등이 그것입니다. 5개 이상의 애플리케이션을 작성하고 나면, 숙련된 팀은 테스트 코드 커버리지를 거의 완전히 달성하는 데 필요한 예산과 그 종류를 점점 더 잘 판단할 수 있게 됩니다. 코드 커버리지 외에도 기능 커버리지와 사이드 이펙트 커버리지도 있다는 점에 유의하세요. 이 모든 차원에서 "완전한" 테스트 커버리지는 수익 체감의 법칙을 따르며, 이를 달성하기가 점점 더 어려워집니다.

다음 유형의 코드 커버리지가 있습니다:

- 자동화된 유닛 테스트. 많은 수의 최소화된 테스트가 코드 커버리지를 최대화하며, 회귀를 방지하거나 잡아내는 데 도움이 됩니다.
- 자동화된 통합 테스트. 소규모 테스트가 코드 커버리지를 최대화하고 기능 커버리지를 높이며, 역시 회귀를 방지하거나 잡아내는 데 도움이 됩니다.
- 수동 "스모크 테스트". 이러한 테스트는 사이드 이펙트 커버리지를 위해 필요합니다.
- 수동 "인수 테스트". 이러한 테스트는 주로 품질 보증 및 계약 목적으로 필요합니다.
- 전혀 테스트되지 않는 코드. 이러한 코드는 위의 4가지 범주 중 하나에 의해 테스트되지 않았기 때문에 테스트하기에 너무 비용이 많이 들거나 "사소한" 것으로 간주됩니다.

매우 복잡한 시스템에서 이상적인 테스트 커버리지 조합은 위 4가지 범주 중 어느 것도 치우치지 않고 골고루 혼합된 것입니다. 데이터베이스가 관련된 경우, 사람들은 아마 빠르게 통합 테스트 작성으로 넘어갈 것입니다. Derby, H2, HSQLDB와 같은 임베디드 데이터베이스를 사용한 통합 테스트는 겉보기에 간단하고, 모든 통합 테스트 세트에 걸쳐 제어된 트랜잭션 수명 주기를 처리하기 쉬워 보입니다.

하지만 시간이 지남에 따라 통합 테스트 스위트의 양이 계속 늘어나면, 점점 더 많은 테스트 데이터 설정 작업을 하게 되고, 복잡한 테스트 데이터 설정의 부작용으로 인한 회귀를 디버깅해야 하며, 통합 테스트 데이터베이스 스키마를 서로 다른 RDBMS 구현에 반복적으로 적용해야 합니다... 물론 테스트 데이터베이스에 대해 쿼리하고 쓰기를 수행하는 모든 코드는 블랙박스 테스트를 거치게 됩니다. 따라서 테스트는 완전한 E2E 테스트가 되고, JDBC나 jOOQ 또는 JPA 사용 코드는 모킹되지 않습니다.

## JDBC 모킹의 어려움

JDBC는 모킹하기 까다로운 API입니다. 많은 구성 옵션과 상태 저장 작업이 있습니다. 우리는 jOOQ에서 JDBC가 얼마나 복잡한 API인지 직접 알고 있습니다. 코드가 최적으로 수행되기를 원하면서도 이식 가능하도록 유지하는 것은 상당히 어렵습니다. 하지만 jOOQ를 개발하면서 많이 배웠고, 이러한 지식을 공유할 생각입니다. 이 기사는 아마도 단순히 jOOQ 3.0의 또 다른 새로운 기능에 관한 것일 수도 있지만, 사실 이 기능을 사용하면 JDBC를 사용하는 모든 Java 애플리케이션에 도움이 됩니다.

기존 솔루션을 간단히 살펴보겠습니다:

- MockRunner: JDBC 전용 확장이 있는 일반적인 모킹 프레임워크
- jMock: 일반적인 Java 모킹 프레임워크
- mockito: 일반적인 Java 모킹 프레임워크
- DBUnit: 데이터베이스 테스팅 프레임워크 (모킹이 아님)

Stack Overflow에서 mockito를 사용하여 JDBC를 모킹하는 것이 얼마나 힘든지에 대한 논의를 찾아볼 수 있습니다. 일반적인 모킹 프레임워크를 사용할 때의 문제점은 다양한 JDBC 객체 유형을 모킹하는 것이 정말 어렵다는 것입니다. 고려해야 할 것은:

- `java.sql.Connection`
- `java.sql.Statement` (및 하위 유형들)
- `java.sql.ResultSet`
- `java.sql.ResultSetMetaData`
- 다양한 부가적인 유형들

## jOOQ의 솔루션: MockConnection

이것이 바로 jOOQ 3.0의 `org.jooq.tools.jdbc.MockConnection`을 사용하여 JDBC를 매우 쉽게 모킹할 수 있도록 만들고자 했던 이유입니다. 이것은 `java.sql.Connection`의 구현으로, 테스트에서 다른 어떤 JDBC Connection처럼 사용할 수 있습니다. 단순히 `MockDataProvider`를 주입하기만 하면 됩니다:

```java
// 이 콜백이 JDBC 목을 만드는 데 필요한 전부입니다.
// 여기서 원하는 로직을 구현해야 합니다.
// true가 될 수 있는 복잡한 if / else 문이나
// 여러 SQL 문자열과 결과 쌍으로 구성된
// 간단한 HashMap이 될 수 있습니다.
MockDataProvider provider = new MockDataProvider() {

    // jOOQ의 MockStatement가 JDBC로
    // 실행될 모든 쿼리를 이 메서드에 전달합니다
    @Override
    public MockResult[] execute(MockExecuteContext context)
    throws SQLException {

        // 여기서 jOOQ Result 객체를 사용하여
        // 조건부로 원하는 MockResult를
        // 반환합니다. DSLContext.fetch(ResultSet)를
        // 사용하여 JDBC ResultSet에서 jOOQ Result를
        // 생성할 수 있습니다.
        // 또는 CSV, XML, JSON, 텍스트 등에서
        // 데이터를 가져올 수도 있습니다.
        DSLContext create = DSL.using(configuration);
        Result<MyTableRecord> result = create.newResult(MY_TABLE);
        result.add(create.newRecord(MY_TABLE));

        return new MockResult[] {
            new MockResult(1, result)
        };
    }
};

// 제공자로 MockConnection을 감싸기만 하면 됩니다
Connection connection = new MockConnection(provider);

// 평소처럼 jOOQ를 사용하기만 하면 됩니다
DSLContext create = DSL.using(connection, dialect);
assertEquals(1, create.selectOne().fetch().size());
```

보시다시피, `MockDataProvider`를 구현함으로써 모든 SQL 문을 가로채고 결과를 반환하거나 예외를 던질 수 있습니다. 위의 예시는 최소한의 테스트만 필요하다는 것을 보여주며, 이제 하나의 `Connection` / `MockDataProvider` 구현에서 완전한 테스트 기능 커버리지를 달성하기 위해 로직을 상당히 복잡하게 만들 수 있습니다.

이제 JDBC API에서 가로챌 수 있는 것에 대한 몇 가지 SPI 개선 사항이 있습니다. 현재 `MockExecuteContext` API는 다음을 할 수 있습니다:

- 실행된 SQL과 바인드 값에 접근
- 일반 SQL 문과 배치 SQL 문을 구분
- jOOQ의 `Result` 객체를 사용하여 결과 반환 (CSV, XML, JSON, TEXT 등에서 가져올 수 있음)
- 생성된 키 반환
- JDBC 직렬화를 위한 jOOQ의 MockStatement 활용

## MockFileDatabase

jOOQ가 실험적으로 제공하는 것은 H2 테스트 스크립트에서 영감을 받은 형식을 사용하는 파일 기반 모킹 솔루션인 `MockFileDatabase`입니다. `MockFileDatabase`를 사용하면 다음과 같은 형식의 텍스트 파일을 제공할 수 있습니다:

```
# 결과가 있는 완전한 문장
select 'A' from dual;
> A
> -
> A
@ rows: 1

# 결과가 있는 테이블에서의 선택
select "TABLE1"."ID1", "TABLE1"."NAME1" from "TABLE1";
> ID1 NAME1
> --- -----
> 1   X
> 2   Y
@ rows: 2

# 결과가 없는 문장
update "TABLE1" set "NAME1" = 'A';
@ rows: 1
```

`MockFileDatabase`를 사용하면 모킹할 각 SQL 문에 대해 하나의 항목을 만들 수 있습니다. 항목은 정확히 일치해야 하는 SQL 문자열로 시작합니다 (현재는 정규식 지원 없음). SQL 문 뒤에는 jOOQ의 `Result.format()`에서 생성된 형식을 사용하여 지정된 결과가 따릅니다. 이 형식은 jOOQ의 `MockResultSetMetaData`로 역직렬화할 수 있습니다.

다음은 테스트에서 이 파일을 사용하는 방법입니다:

```java
// 일부 파일에서 모든 SQL/결과 쌍을 포함하는 파일 데이터베이스를
// 초기화합니다
MockFileDatabase db = new MockFileDatabase(
    new File("/path/to/mock-data.txt"));

// 언제나처럼 MockConnection을 제공하고 jOOQ를 사용하면 됩니다
DSLContext create = DSL.using(new MockConnection(db), dialect);
assertEquals(1, create.selectOne().fetch().size());
```

## 향후 개선 사항

계획된 개선 사항은 다음과 같습니다:

- SQL 문에 대한 정규식 패턴 매칭
- 추가 형식 지원
- 배치 문 동작 지정

## 더 넓은 응용

이 기사의 가장 중요한 점은 바로 이것입니다: jOOQ의 `MockConnection`은 JDBC를 사용하는 모든 도구와 함께 사용할 수 있습니다. 이것은 jOOQ뿐만 아니라 JPA, Hibernate, iBatis, 그리고 레거시 JDBC 코드에서도 사용할 수 있습니다. jOOQ는 이제 여러분이 선호하는 JDBC 모킹 프레임워크가 되었습니다!

더 읽어보기: [jOOQ 매뉴얼의 목 데이터베이스 페이지](https://www.jooq.org/doc/latest/manual/tools/jdbc-mocking/)

## 결론

이 글은 데이터베이스 관련 코드를 테스트할 때 통합 테스트와 유닛 테스트 사이의 적절한 균형을 찾는 것에 관한 것입니다. jOOQ의 `MockConnection`과 `MockDataProvider`를 사용하면 복잡한 JDBC API를 직접 모킹하지 않고도 간단한 인터페이스 하나만 구현하여 데이터베이스 상호작용을 쉽게 모킹할 수 있습니다.

대부분의 경우 통합 테스트가 유닛 테스트보다 더 효과적입니다. 더 적은 노력으로 더 많은 커버리지를 제공하고 복잡한 API 모킹을 피할 수 있기 때문입니다. 하지만 유닛 테스트가 필요한 경우, jOOQ의 모킹 도구가 그 작업을 훨씬 쉽게 만들어 줍니다.
