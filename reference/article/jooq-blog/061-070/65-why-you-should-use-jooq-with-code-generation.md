# jOOQ를 코드 생성과 함께 사용해야 하는 이유

> 원문: https://blog.jooq.org/why-you-should-use-jooq-with-code-generation/

## 서론

Stack Overflow의 jOOQ 관련 질문들에서 반복적으로 나타나는 패턴이 있습니다. 많은 사용자들이 설정이 복잡해 보인다는 이유로 코드 생성기 사용을 피합니다. 하지만 초기 투자는 문제 예방과 향상된 기능을 통해 상당한 배당금을 지급합니다.

## jOOQ 코드 생성기란?

jOOQ는 Java에서 타입 안전하고 내장된 동적 SQL을 가능하게 하는 내부 DSL로 동작합니다. 코드 생성기는 데이터베이스 메타데이터의 Java 표현을 생성하여 `AUTHOR.FIRST_NAME`과 같은 테이블 및 컬럼 참조를 만들어냅니다. 이렇게 생성된 객체들은 다음을 캡슐화합니다:

- 카탈로그/스키마 정보
- 테이블과 컬럼 연관 관계
- 데이터 타입 명세
- 커스텀 컨버터와 바인딩
- 제약 조건 메타데이터 (기본키, 유니크, 외래키)
- Identity와 기본값 표현식
- 타입 안전한 별칭(aliasing) 메서드

일반적인 jOOQ 쿼리는 이 접근 방식을 보여줍니다:

```java
var author =
ctx.select(AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME)
   .from(AUTHOR)
   .where(AUTHOR.ID.eq(10))
   .fetchOne();
```

## 코드 생성의 대가는 무엇인가?

사용자들은 종종 코드 생성 구현을 주저하며, 즉각적인 실험을 선호합니다. 하지만 설정 장벽은 생각보다 낮습니다. 튜토리얼과 예제가 온보딩을 단순화합니다. 대안적인 접근 방식도 존재합니다:

- 초기 탐색을 위한 jbang 사용
- 라이브 데이터베이스 연결 없이 SQL 파일에서 생성하는 DDLDatabase
- 빠른 샘플 데이터베이스 프로비저닝을 위한 testcontainers

저자는 초기 투자가 몇 분 정도의 추가 설정만 필요하면서도 막대한 수익을 제공한다고 강조합니다.

## 그 대가로 무엇을 얻는가?

### 1. 컴파일 타임 타입 안전성

SQL 구문 검증을 넘어서, 코드 생성은 스키마 객체에 대한 타입 안전성을 제공합니다. 모든 데이터베이스 요소는 생성된 Java 표현을 받습니다:

* 테이블, 컬럼, 제약 조건, 인덱스
* 시퀀스, 프로시저, 함수
* 패키지, 스키마, 카탈로그
* 도메인, enum 타입, 객체 타입

IDE 자동 완성이 모든 스키마 요소에 대해 사용 가능해집니다. 가장 중요한 것은 컬럼 이름 변경이나 삭제가 컴파일 실패를 일으켜 회귀를 방지한다는 것입니다. 데이터 타입 불일치는 컴파일러 오류를 발생시킵니다:

```java
// FIRST_NAME은 TableField<AuthorRecord, String>
// 잘못된 타입 할당이 컴파일 타임에 잡힘
```

이 접근 방식은 중복된 타입 선언을 제거합니다. 타입이 DDL에 존재하는데, 왜 Java에서 다시 선언해야 할까요?

```sql
CREATE TABLE author (
  id INTEGER NOT NULL PRIMARY KEY,
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
);
```

### 2. 스키마 인트로스펙션

생성된 코드는 컬럼 정의와 SQL 주석으로의 빠른 탐색을 가능하게 합니다:

```sql
COMMENT ON COLUMN author.first_name IS 'The author''s first name';
```

이러한 주석들은 Javadoc에 나타나 코드 이해를 돕습니다. IDE의 호출 계층 검색은 애플리케이션 전체에서 정확한 컬럼 사용을 보여주며, 대소문자를 구분하는 문자열 리터럴을 매칭하는 텍스트 검색보다 우수합니다.

### 3. 런타임 메타 모델: 데이터 타입

타입 정보는 중요한 런타임 목적을 수행합니다. 많은 JDBC 드라이버와 SQL 방언은 명시적인 데이터 타입 전달을 요구합니다.

바인드 변수에 충분한 타입 컨텍스트가 없을 때:

Derby와 이전 Db2 버전은 타입이 없는 null을 거부합니다:

```sql
select null
from sysibm.sysdummy1;
-- Error: Syntax error: Encountered "null" at line 1, column 8.
```

올바른 구문은 캐스팅을 요구합니다:

```sql
select cast(null as int)
from sysibm.sysdummy1;
```

jOOQ는 통합 테스트가 방언 요구사항을 밝혀낼 때 필요한 캐스트를 자동으로 추가합니다. 하지만 타입 정보가 제공되어야 합니다. 일반 SQL 템플릿은 `Field<Object>` 참조를 생성하여 데이터베이스 변경 시 잠재적으로 문제를 일으킬 수 있습니다.

Field<Object>가 컴파일 오류로 이어질 때:

Java 8의 제네릭 오버로드 해결은 독특한 시나리오를 만듭니다. 타입이 지정된 매개변수는 올바르게 동작하지만:

```java
Parameter<String> p1 = ...
Field<String> f1 = ...
setValue(p1, f1); // 정상적으로 컴파일됨
```

Object 타입 매개변수는 모호성을 만듭니다:

```java
Parameter<Object> p2 = ...
Field<Object> f2 = ...
setValue(p2, f2); // 컴파일 오류
```

일반 SQL 템플릿은 모든 곳에서 `Field<Object>`를 생성하여, 결국 UPDATE 절의 컴파일을 실패하게 만듭니다.

### 4. 에뮬레이션

일부 에뮬레이션은 생성된 메타데이터를 필요로 합니다. `INSERT .. RETURNING` 에뮬레이션은 기본키와 identity 정보에 의존합니다. 이것 없이 jOOQ는 최적의 실행 전략을 결정할 수 없습니다.

```java
AuthorRecord author =
ctx.insertInto(AUTHOR, AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME)
   .values("John", "Doe")
   .returning(AUTHOR.ID)
   .fetchOne();
```

jOOQ는 메타데이터가 사용 가능할 때 Firebird, Oracle, PostgreSQL, SQL Server(OUTPUT을 통해), Db2, H2에서 이 단일 쿼리를 실행할 수 있습니다. 또는 필요할 때 JDBC의 `getGeneratedKeys()`나 보조 쿼리를 사용합니다.

### 5. 컨버터

SQL은 기본 타입(숫자, 문자열, 시간)을 제공합니다. 애플리케이션 코드는 도메인 특화 타입의 이점을 얻습니다. 컨버터는 타입 매핑을 가능하게 합니다:

```java
record LeFirstName(String firstName) {}
```

수동 접근 방식은 장황한 필드 정의를 요구합니다:

```java
Record2<LeFirstName, String> author =
ctx.select(
        field("author.first_name", VARCHAR.asConvertedDataType(
            LeFirstName.class, LeFirstName::new, LeFirstName::firstName
        )),
        field("author.last_name", VARCHAR))
   .from("author")
   .where(field("author.id", INTEGER).eq(10))
   .fetchOne();
```

코드 생성은 모든 일치하는 컬럼에 자동으로 컨버터를 적용하여 실수로 인한 누락을 방지합니다:

```java
// codegen 설정 후, 커스텀 타입이 모든 곳에 적용됨
Record2<LeFirstName, String> author =
ctx.select(AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME)
   .from(AUTHOR)
   .where(AUTHOR.ID.eq(10))
   .fetchOne();
```

### 6. 타입 안전한 별칭

별칭이 지정된 테이블은 타입 안전성을 유지합니다:

```java
var a = AUTHOR.as("a");

var author =
ctx.select(a.FIRST_NAME, a.LAST_NAME)
   .from(a)
   .where(a.ID.eq(10))
   .fetchOne();
```

호출 계층 검색은 별칭이 지정된 컬럼 사용을 정확하게 추적합니다. 오타나 컬럼 이름 변경 시 컴파일 실패가 발생합니다.

### 7. 암시적 조인

jOOQ의 강력한 기능 중 하나인 암시적 조인은 외래키 관계를 사용하여 반복적인 조인 조건을 제거합니다:

```java
ctx.select(
          BOOK.author().FIRST_NAME,
          BOOK.author().LAST_NAME,
          BOOK.TITLE,
          BOOK.language().CD.as("language"))
   .from(BOOK)
   .fetch();
```

이것은 다음을 대체합니다:

```java
ctx.select(
          AUTHOR.FIRST_NAME,
          AUTHOR.LAST_NAME,
          BOOK.TITLE,
          LANGUAGE.CD.as("language"))
   .from(BOOK)
   .leftJoin(AUTHOR).on(BOOK.AUTHOR_ID.eq(AUTHOR.ID))
   .leftJoin(LANGUAGE).on(BOOK.LANGUAGE_ID.eq(LANGUAGE.ID))
   .fetch();
```

이 기능은 생성된 메타데이터를 필요로 합니다. `BOOK.author()`와 `BOOK.language()` 메서드는 반환 타입과 함께 생성되어야 합니다.

### 8. CRUD

jOOQ는 기본 CRUD 작업을 위한 UpdatableRecord 편의 기능을 제공합니다:

```java
BookRecord book =
ctx.selectFrom(BOOK)
   .where(BOOK.ID.eq(17))
   .fetchOne();

book.setTitle("New title");
book.store();
```

선택적 낙관적 잠금 지원이 존재합니다. 모든 CRUD 기능은 생성된 코드를 필요로 합니다.

### 9. 저장 프로시저

생성된 코드는 JDBC 복잡성을 숨기면서 프로시저 바인딩 로직을 래핑합니다. PostgreSQL의 경우:

```sql
CREATE FUNCTION p_count_authors_by_name (
  author_name VARCHAR,
  result OUT INTEGER
)
AS $$
DECLARE
  v_result INT;
BEGIN
  SELECT COUNT(*)
  INTO v_result
  FROM author
  WHERE first_name LIKE author_name
  OR last_name LIKE author_name;

  result := v_result;
END;
$$ LANGUAGE plpgsql;
```

생성된 Java:

```java
public static Integer pCountAuthorsByName(
      Configuration configuration
    , String authorName
) {
    // 구현 세부 사항은 자동으로 처리됨
}
```

코드 생성은 복잡한 경우를 처리합니다: SQL 타입, PL/SQL 타입, 레코드 타입, 테이블 타입, 연관 배열, ref 커서. 프로시저가 변경되면 클라이언트 코드의 컴파일 실패가 개발자에게 호출을 리팩터링해야 한다고 알립니다.

### 10. 멀티테넌시

jOOQ는 스키마 매핑을 통한 스키마 수준의 멀티테넌시를 지원하여, 런타임에 카탈로그, 스키마, 테이블 이름을 동적으로 변경합니다. 일반 SQL 템플릿은 임의의 SQL 문자열을 포함하므로 이 기능을 활용할 수 없습니다. 생성된 코드는 한정자 제어를 가능하게 하여 환경 간 스키마 마이그레이션을 단순화합니다.

### 11. 임베디드 타입

임베디드 타입은 여러 데이터베이스 컬럼을 단일 클라이언트 측 값으로 래핑하여 UDT와 유사한 구조를 만듭니다:

```java
// AMOUNT와 CURRENCY 컬럼을 결합
record MonetaryAmount(BigDecimal amount, String currency) {}
```

임베디드 타입은 임베디드 키(BOOK.AUTHOR_ID가 AUTHOR.ID와만 비교되도록 강제)와 임베디드 도메인(클라이언트 코드에서 데이터베이스 시맨틱 타입 재사용)에서 뛰어납니다. 현재 코드 생성에서만 사용 가능합니다.

### 12. 데이터 변경 관리

코드 생성은 초기 데이터 변경 관리 설정(Flyway, Liquibase)을 권장합니다. 코드 생성을 CI/CD 파이프라인에 통합하면 응집력 있는 워크플로우가 생성됩니다:

1. DDL 변경 사항이 자동으로 적용됨
2. 코드가 재생성됨
3. 통합 테스트가 실행됨
4. 결과가 단위로 배포됨

testcontainers와 jOOQ를 사용하면 CI/CD 동안 자동화된 코드 생성이 가능하여 데이터베이스 스키마와 Java 코드가 동기화된 상태를 유지합니다.

이 통합은 프로젝트 중간에 기하급수적으로 어려워지는 데이터 변경 관리를 미루는 흔한 실수를 방지합니다.

## 결론

jOOQ의 코드 생성기는 기본적인 편의성을 넘어 수많은 기능을 활성화합니다:

- 복잡한 모델 지원 (단순화된 조인)
- 뷰 (테이블과 함께 생성됨)
- 데이터 타입 (완전히 표현됨)
- 프로시저 (투명하게 바인딩됨)
- 기타 등등

코드 생성은 데이터베이스 기능의 파워 유저가 되도록 권장하며, 신중하게 설계된 스키마를 애플리케이션 전체에서 활용하게 합니다.

## 예외

유일한 예외는 컴파일 타임에 알 수 없는 동적 스키마의 경우 발생합니다. 이 아키텍처를 사용하는 시스템은 거의 없습니다. 동적 SQL 쿼리도 여전히 컴파일 타임에 알려진 스키마에서 작동할 수 있으므로, 대부분의 경우 이 예외는 적용되지 않습니다.
