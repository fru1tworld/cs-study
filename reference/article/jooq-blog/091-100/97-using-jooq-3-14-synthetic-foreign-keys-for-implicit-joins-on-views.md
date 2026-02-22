# jOOQ 3.14 합성 외래 키를 사용하여 뷰에 암묵적 조인 작성하기

> 원문: https://blog.jooq.org/using-jooq-3-14-synthetic-foreign-keys-to-write-implicit-joins-on-views/

jOOQ의 매우 멋진 기능 중 하나는 암묵적 조인(implicit join)입니다. 이 기능은 타입 안전한 방식으로 일대일(to-one) 관계를 탐색하며, LEFT JOIN을 암묵적으로 생성합니다. 예를 들어, Sakila 데이터베이스에서 다음과 같이 작성할 수 있습니다:

`customer → address → city → country` 관계를 탐색하면 조인 조건을 명시적으로 작성하지 않아도 여러 JOIN 절이 자동으로 생성됩니다.

## 전통적인 데이터베이스의 문제점

모든 데이터베이스 스키마가 대리 키(surrogate key)를 사용하는 것은 아닙니다. 표준 SQL의 `INFORMATION_SCHEMA`(H2, HSQLDB, MariaDB, MySQL, PostgreSQL, SQL Server에서 찾을 수 있음)는 대리 키 대신 복합 자연 키(composite natural key)를 자주 사용합니다.

HSQLDB의 `DOMAIN_CONSTRAINTS` 뷰를 전통적인 명시적 조인으로 복합 키를 사용해 쿼리하려면, `CATALOG`, `SCHEMA`, `NAME` 세 개의 컬럼을 결합하는 장황한 join-on 절이 필요합니다.

## 합성 외래 키 솔루션

jOOQ 3.14부터 합성 외래 키(synthetic foreign keys)를 사용하면 데이터베이스의 네이티브 지원 없이도 뷰에 제약 조건을 정의할 수 있습니다. XML 설정을 통해 다음을 지정할 수 있습니다:

- 정규식 패턴을 사용한 필드 선택으로 뷰에 합성 기본 키 정의
- 복합 키를 통해 테이블을 연결하는 합성 외래 키 정의
- 유연한 필드 순서를 가진 명명된 제약 조건

## 설정 예시

다음 XML 설정은 코드 생성기가 뷰를 마치 다음과 같은 제약 조건이 있는 것처럼 처리하도록 합니다:

```sql
ALTER TABLE CHECK_CONSTRAINTS ADD PRIMARY KEY (CONSTRAINT_CATALOG, CONSTRAINT_SCHEMA, CONSTRAINT_NAME);
ALTER TABLE DOMAINS ADD PRIMARY KEY (DOMAIN_CATALOG, DOMAIN_SCHEMA, DOMAIN_NAME);
```

그리고 테이블 간의 해당 외래 키 관계도 포함됩니다.

```xml
<configuration>
  <generator>
    <database>
      <syntheticObjects>
        <primaryKeys>
          <primaryKey>
            <tables>CHECK_CONSTRAINTS|CONSTRAINTS|TABLE_CONSTRAINTS</tables>
            <fields>
              <field>CONSTRAINT_(CATALOG|SCHEMA|NAME)</field>
            </fields>
          </primaryKey>
          <primaryKey>
            <tables>DOMAINS</tables>
            <fields>
              <field>DOMAIN_(CATALOG|SCHEMA|NAME)</field>
            </fields>
          </primaryKey>
        </primaryKeys>
        <foreignKeys>
          <foreignKey>
            <tables>DOMAIN_CONSTRAINTS</tables>
            <fields>
              <field>CONSTRAINT_(CATALOG|SCHEMA|NAME)</field>
            </fields>
            <referencedTable>CHECK_CONSTRAINTS</referencedTable>
          </foreignKey>
          <foreignKey>
            <tables>DOMAIN_CONSTRAINTS</tables>
            <fields>
              <field>DOMAIN_(CATALOG|SCHEMA|NAME)</field>
            </fields>
            <referencedTable>DOMAINS</referencedTable>
          </foreignKey>
        </foreignKeys>
      </syntheticObjects>
    </database>
  </generator>
</configuration>
```

## 이전: 전통적인 조인 구문

```java
Domains d = DOMAINS.as("d");
DomainConstraints dc = DOMAIN_CONSTRAINTS.as("dc");
CheckConstraints cc = CHECK_CONSTRAINTS.as("cc");

for (Record record : create()
    .select(
        d.DOMAIN_SCHEMA,
        d.DOMAIN_NAME,
        d.DATA_TYPE,
        d.CHARACTER_MAXIMUM_LENGTH,
        d.NUMERIC_PRECISION,
        d.NUMERIC_SCALE,
        d.DOMAIN_DEFAULT,
        cc.CHECK_CLAUSE)
    .from(d)
    .join(dc)
        .on(row(d.DOMAIN_CATALOG, d.DOMAIN_SCHEMA, d.DOMAIN_NAME)
        .eq(dc.DOMAIN_CATALOG, dc.DOMAIN_SCHEMA, dc.DOMAIN_NAME))
    .join(cc)
        .on(row(dc.CONSTRAINT_CATALOG,
                dc.CONSTRAINT_SCHEMA,
                dc.CONSTRAINT_NAME)
        .eq(cc.CONSTRAINT_CATALOG,
                cc.CONSTRAINT_SCHEMA,
                cc.CONSTRAINT_NAME))
    .where(d.DOMAIN_SCHEMA.in(getInputSchemata()))
    .orderBy(d.DOMAIN_SCHEMA, d.DOMAIN_NAME)
) { ... }
```

## 이후: 암묵적 조인 구문

```java
DomainConstraints dc = DOMAIN_CONSTRAINTS.as("dc");

for (Record record : create()
    .select(
        dc.domains().DOMAIN_SCHEMA,
        dc.domains().DOMAIN_NAME,
        dc.domains().DATA_TYPE,
        dc.domains().CHARACTER_MAXIMUM_LENGTH,
        dc.domains().NUMERIC_PRECISION,
        dc.domains().NUMERIC_SCALE,
        dc.domains().DOMAIN_DEFAULT,
        dc.checkConstraints().CHECK_CLAUSE)
    .from(dc)
    .where(dc.domains().DOMAIN_SCHEMA.in(getInputSchemata()))
    .orderBy(dc.domains().DOMAIN_SCHEMA, dc.domains().DOMAIN_NAME)
) { ... }
```

## 활성화되는 이점

합성 메타데이터를 통해 다음 기능들이 활성화됩니다:

- 암묵적 JOIN 구문
- 생성된 레코드에 대한 탐색 메서드
- 내장 키 기능
- 타입 안전한 경로 탐색

## 개선된 쿼리 패턴

리팩토링된 쿼리는 명시적 복합 키 조건 대신 암묵적 탐색 메서드(`dc.domains()`, `dc.checkConstraints()`)를 사용합니다. 이를 통해 정확성 보장을 유지하면서 더 깔끔하고 유지보수하기 쉬운 코드를 작성할 수 있습니다.

키 구조가 변경되더라도 복합 키 조인 조건의 복잡성을 제거하면서 정확성을 유지합니다. 탐색 메서드(`domains()`, `checkConstraints()`)가 수동 조인 로직을 대체하여 오류 가능성을 줄이고 가독성을 향상시킵니다.

## 향후 개선 사항

계획된 개선 사항으로는 암묵적 일대다(to-many) 조인, DML 지원, 더 넓은 접근성을 위한 파서 통합이 있습니다.
