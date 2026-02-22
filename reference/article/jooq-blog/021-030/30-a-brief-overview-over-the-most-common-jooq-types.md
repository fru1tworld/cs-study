# 가장 많이 사용되는 jOOQ 타입에 대한 간략한 개요

> 원문: https://blog.jooq.org/a-brief-overview-over-the-most-common-jooq-types/

jOOQ를 처음 접하는 개발자에게 jOOQ API의 타입 시스템은 압도적으로 느껴질 수 있습니다. SQL에서는 이러한 구조적 개념들이 영어와 유사한 문법 뒤에 숨겨져 있지만, jOOQ는 이러한 개념들을 명시적으로 드러냅니다. 이 글은 가장 중요한 jOOQ 타입들을 치트시트 형태로 제공합니다.

## Configuration 타입

`Configuration` 타입은 모든 설정의 중심 허브 역할을 합니다. `Settings`, 커스텀 SPI 구현체, 그리고 JDBC/R2DBC 연결에 대한 참조를 포함합니다.

Configuration 내의 주요 SPI는 다음과 같습니다:

- ConnectionProvider: 연결 획득 및 해제 의미론을 관리합니다.
- ExecuteListenerProvider: 쿼리 실행 생명주기를 관리하는 구현체입니다.
- RecordMapperProvider: 레코드가 커스텀 클래스에 매핑되는 방식을 제어합니다.

## Scope 타입

`Scope` 타입은 `Configuration`의 컨텍스트 내에서 생성되며, 해당 Configuration이 포함하는 객체에 대한 접근을 제공합니다. 주요 Scope 타입은 다음과 같습니다:

- Context: 쿼리 표현식 트리의 단일 순회를 처리하여 SQL 문자열과 바인드 변수를 생성합니다.
- DSLContext: Configuration 내에서 쿼리를 구성하기 위한 주요 DSL API입니다.
- ExecuteContext: JDBC 리소스를 사용하여 단일 쿼리 실행을 관리합니다.

## Settings

`Settings`는 jOOQ 동작을 제어하는 스칼라 설정 플래그입니다. 예를 들어, 실행 로깅 토글, JDBC fetch size 지정, statement 타입 선택(prepared vs. standard statement) 등이 있습니다. 버전 3.17 기준으로 160개 이상의 설정이 존재합니다.

## DSL 타입

DSL API는 쿼리를 구성하는 두 가지 접근 방식을 제공합니다.

### 정적 DSL

정적 `DSL` 클래스는 `Query`, `Field`, `Condition`, `Table` 타입을 포함한 모든 `QueryPart` 타입의 진입점을 제공합니다. 이들은 어떠한 Configuration도 첨부하지 않고 구성됩니다.

### 컨텍스트 DSL

`DSLContext`는 Configuration 컨텍스트 내에서의 쿼리 구성에 중점을 둡니다. 생성된 쿼리는 `execute()`나 `fetch()` 같은 메서드를 사용하여 직접 실행할 수 있으며, 동기 및 비동기 작업을 모두 지원합니다.

### Step 타입

`SelectFromStep`과 같은 중간 "Step" 타입은 fluent API의 단계를 나타냅니다. 이들은 구현의 부산물이므로 프로덕션 코드에서 직접 참조해서는 안 됩니다.

## QueryPart 타입

`QueryPart`는 jOOQ의 전체 표현식 트리의 기본 타입입니다. 구성되는 모든 요소가 이 타입을 확장합니다:

```java
QueryPart p1 = TABLE;
QueryPart p2 = TABLE.COLUMN;
QueryPart p3 = TABLE.COLUMN.eq(1);
```

렌더링은 `DSLContext::render`를 통해 수행됩니다:

```java
String sql = ctx.render(TABLE.COLUMN.eq(1));
```

### Table

`Table`은 FROM 절 및 DML 대상에 사용되는 테이블 참조를 나타냅니다. 변형으로는 생성된 코드 테이블, 구성된 테이블 참조, plain SQL 테이블, 조인된 테이블, 별칭 테이블, VALUES 생성자, 파생 테이블, 테이블 값 함수, XMLTABLE, JSON_TABLE 표현식 등이 있습니다.

```java
Table<?> joined = CUSTOMER
    .join(ADDRESS)
    .on(ADDRESS.CUSTOMER_ID.eq(CUSTOMER.CUSTOMER_ID));
```

### Field

`Field`는 SELECT 절, GROUP BY, ORDER BY, 함수 인자, CASE 표현식 등에서 사용할 수 있는 열 표현식을 나타냅니다.

### Condition

`Condition`은 `and()` 및 `or()` 같은 추가 메서드를 가진 `Field<Boolean>`입니다. WHERE, CONNECT BY, HAVING, QUALIFY 절에서 사용됩니다.

### Row

`Row`는 다중 차수 조건 연산 및 중첩 레코드 프로젝션을 위한 튜플을 모델링하며, 필드 표현식의 구조적 그룹을 생성합니다.

### Select

`Select`는 `ResultQuery`를 확장하며, 최상위 쿼리, 스칼라 서브쿼리, 파생 테이블, 또는 union 서브쿼리로 사용됩니다.

### ResultQuery

`ResultQuery`는 `Record` 값을 다양한 컬렉션 형태(스트림, 결과, 퍼블리셔, CompletionStage)로 생성하는 실행 가능한 쿼리를 나타냅니다.

### Query

`Query`는 Configuration이 필요한 실행 가능한 문장을 나타냅니다. 실행 과정에는 SQL 생성, 문장 준비, 바인드 변수 바인딩, 실행, 그리고 선택적으로 결과 페치가 포함됩니다.

### Statement

`Statement`(JDBC의 `Statement`와는 다릅니다)는 익명 블록, 프로시저 본문, 함수 본문, 트리거 본문 내의 절차적 문장을 나타냅니다.

## QOM 타입

QOM(Query Object Model) 타입은 내부 모델 API를 공개적으로 선언하여, 트리 순회 및 SQL 변환 연산을 지원합니다.

## Result 타입

서로 다른 실행 메서드는 서로 다른 결과 타입을 생성합니다.

### Result

`Result`는 매핑 편의 메서드를 갖춘 `List<Record>`로 기능하며, 즉시(eagerly) 페치된 JDBC 결과를 나타냅니다. 중간 크기의 데이터셋에 적합합니다.

### Cursor

`Cursor`는 열린 JDBC ResultSet을 유지하는 `Iterable<Record>`로, 지연(lazy) 페치를 가능하게 합니다. 대용량 데이터셋에 이상적입니다.

### Record

`Record`는 데이터베이스 레코드의 기본 타입입니다. 특수화된 타입은 다음과 같습니다:

- Record1 ~ Record22: 타입 안전한 열 표현을 위한 타입입니다.
- TableRecord: 단일 테이블 레코드를 나타냅니다.
- UpdatableRecord: 알려진 기본 키를 가진 레코드로, CRUD 연산을 지원합니다.

## SPI 타입

jOOQ는 동작을 확장하기 위한 다양한 Service Provider Interface 타입을 제공합니다.

### Converter

`Converter<T, U>`는 내장 JDBC 타입과 사용자 정의 타입 사이를 한 쌍의 함수를 통해 변환합니다. 하나는 데이터베이스에서 변환하는 함수이고, 다른 하나는 바인딩을 위해 데이터베이스로 변환하는 함수입니다.

### Binding

`Binding<T, U>`는 JDBC의 "get" 메서드(ResultSet, CallableStatement, SQLInput)와 "set" 메서드(PreparedStatement, SQLOutput)와의 상호작용을 지정하며, 바인드 값에 대한 렌더링 사양도 포함하여 JDBC 상호작용을 완전히 재정의할 수 있습니다.

---

이 레퍼런스를 북마크해 두세요. 향후 새로운 중요한 개념이 등장하면 더 많은 타입이 추가될 예정입니다.
