# jOOQ와 PostgreSQL 통합: 파티셔닝

> 원문: https://blog.jooq.org/integrating-jooq-with-postgresql-partitioning/

이 게스트 포스트는 jOOQ 통합 파트너인 UWS Software Service에서 작성했습니다.

jOOQ는 이 기능에 대한 명시적인 지원을 제공하지 않지만, 상당히 쉽게 통합할 수 있습니다. 이 글에서는 테이블 파티셔닝을 위해 jOOQ를 사용하는 방법에 대한 3가지 접근 방식을 설명하고, 데이터베이스의 다중 테넌시(multi-tenancy)로 전환하기 위한 준비 작업도 소개하겠습니다.

## PostgreSQL 파티셔닝

PostgreSQL의 파티셔닝은 테이블 상속을 기반으로 합니다. 이것은 큰 단일 테이블을 작은 조각들로 나누어 디스크에 저장된 여러 작은 테이블로 분할하는 것을 의미합니다. 각 파티션은 부모 테이블로부터 컬럼과 제약조건을 상속받는 일반적인 테이블입니다. 이 개념은 데이터 범위가 서로 겹치지 않는 "범위 파티셔닝(range partitioning)"에서 특히 유용합니다.

아래의 `AUTHOR` 테이블 예제에서는 데이터가 `authorgroup_id`를 기준으로 분리되며, 각 `authorgroup_id`마다 하나의 테이블이 존재합니다:

```sql
-- 부모 테이블
CREATE TABLE author (
  authorgroup_id int,
  LastName varchar(255)
);

-- 파티션 1
CREATE TABLE author_1 (
  CONSTRAINT authorgroup_id_check_1
    CHECK ((authorgroup_id = 1))
) INHERITS (author);

-- 파티션 2
CREATE TABLE author_2 (
  CONSTRAINT authorgroup_id_check_2
    CHECK ((authorgroup_id = 2))
) INHERITS (author);
```

자식 테이블(author_1, author_2)은 데이터 분리를 강제하기 위한 CHECK 제약조건 정의만 포함하고 있으며, 실제 데이터는 포함하지 않습니다. 부모 테이블로부터 모든 컬럼과 제약조건을 상속받습니다.

## 접근 방식 1: 단순한 방법

첫 번째 접근 방식은 각 파티션에 대해 별도의 jOOQ 클래스를 생성하고 이를 개별적으로 처리하는 것입니다:

```java
// AUTHOR_1 파티션에 삽입
InsertQuery query1 = dsl.insertQuery(AUTHOR_1);
query1.addValue(AUTHOR_1.ID, 1);
query1.addValue(AUTHOR_1.LAST_NAME, "Nowak");
query1.execute();

// AUTHOR_2 파티션에 삽입
InsertQuery query2 = dsl.insertQuery(AUTHOR_2);
query2.addValue(AUTHOR_2.ID, 1);
query2.addValue(AUTHOR_2.LAST_NAME, "Nowak");
query2.execute();
```

이 방법은 직관적이지만 각 파티션마다 별도의 클래스가 생성되어 코드베이스가 복잡해지는 단점이 있습니다. 또한 파티션을 반복 처리할 때 불편합니다.

## 접근 방식 2: 런타임 스키마 매핑

jOOQ의 런타임 스키마 매핑은 종종 데이터베이스 환경을 구현하는 데 사용됩니다. 이 기능은 DSLContext를 생성할 때 구성되므로 시스템 전체 설정으로 간주될 수 있습니다. 이 접근 방식을 통해 단일 논리적 테이블을 구성을 통해 특정 파티션에 매핑할 수 있으며, 엄격한 데이터 분리가 필요한 다중 테넌시 시나리오를 지원합니다.

```java
Settings settings = new Settings()
  .withRenderMapping(new RenderMapping()
  .withSchemata(
      new MappedSchema().withInput("DEV")
                        .withOutput("MY_BOOK_WORLD")
                        .withTables(
      new MappedTable().withInput("AUTHOR")
                       .withOutput("AUTHOR_1"))));

// 매핑된 설정으로 DSLContext 생성
DSLContext create = DSL.using(connection,
  SQLDialect.ORACLE, settings);
```

이 접근 방식은 환경별 또는 다중 테넌트 구성을 가능하게 하지만, 쿼리당 정확히 하나의 테이블만 지원합니다.

## 접근 방식 3: 동적 타입 안전 접근

세 번째 접근 방식은 더 유연한 방법으로, 다중 테넌시가 필요하지 않을 때 사용할 수 있습니다. `forPartition()` 유틸리티 클래스와 함께 커스텀 Builder 패턴을 사용하여 여러 클래스를 생성하지 않고도 파티션을 동적으로 참조할 수 있으며, 루프 기반 파티션 반복을 가능하게 하면서 타입 안전성을 유지합니다.

```java
// 동적 파티션 접근 - 루프를 통한 반복
for(int i=1; i<=2; i++) {
  Builder part = forPartition(i);
  InsertQuery query = dsl.insertQuery(part.table(AUTHOR));
  query.addValue(part.field(AUTHOR.ID), 1);
  query.execute();
}
```

이 Builder 패턴 추상화를 통해 개발자는 각 파티션에 대해 별도의 클래스를 생성하지 않고도 파티션을 동적으로 참조할 수 있습니다. 파티션 번호가 추상화되어 'AUTHOR_1' 대신 'AUTHOR' 테이블을 사용할 수 있습니다. Builder는 테이블과 필드 참조를 래핑하여 타입 안전성을 유지하면서 파티션에 대한 우아한 반복을 가능하게 합니다.

## 빌드 프로세스 통합

Maven 구성에서 정규식 패턴을 사용하여 파티션에 대해 생성된 클래스를 제외할 수 있습니다. 이렇게 하면 코드 생성 중에 파티션 테이블이 코드베이스를 오염시키는 것을 방지할 수 있습니다:

```xml
<generator>
  <name>org.jooq.util.DefaultGenerator</name>
  <database>
    <name>org.jooq.util.postgres.PostgresDatabase</name>
    <includes>.*</includes>
    <!-- 파티션 테이블 제외 (예: AUTHOR_1, AUTHOR_2 등) -->
    <excludes>.*_[0-9]+</excludes>
  </database>
</generator>
```

`<excludes>.*_[0-9]+</excludes>` 패턴은 숫자로 끝나는 테이블 이름(즉, 파티션 테이블)이 jOOQ 코드 생성에서 제외되도록 합니다.

## 성능 고려사항

PostgreSQL 문서에 따르면, "테이블이 매우 클 때만 일반적으로 이점이 있습니다." 파티셔닝 결정은 애플리케이션별 요인에 따라 달라지며, 테이블 크기가 데이터베이스 서버의 물리적 메모리를 초과하기 전까지는 이점이 실현되지 않을 수 있다는 지침이 있습니다.

## 결론

jOOQ는 PostgreSQL 파티셔닝에 대한 명시적인 지원을 제공하지 않지만, 위에서 설명한 세 가지 접근 방식을 통해 효과적으로 통합할 수 있습니다:

1. 단순한 방법: 각 파티션에 대해 별도의 클래스를 생성하고 개별적으로 처리
2. 런타임 스키마 매핑: 다중 테넌시 시나리오에 적합한 시스템 전체 설정
3. 동적 타입 안전 접근: Builder 패턴을 사용한 유연하고 타입 안전한 파티션 접근

적절한 접근 방식을 선택하고 빌드 프로세스에서 파티션 테이블을 제외하면, 타입 안전성을 유지하면서 불필요한 생성 클래스를 피할 수 있습니다.

## 참고 자료

- [GitHub 소스 코드](https://github.com/ggajos/jooq-partition-example)
- [PostgreSQL 파티셔닝 문서](https://www.postgresql.org/docs/)
- [jOOQ 런타임 스키마 매핑 문서](https://www.jooq.org/doc/)
