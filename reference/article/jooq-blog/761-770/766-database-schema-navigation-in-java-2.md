# Java에서 데이터베이스 스키마 탐색

> 원문: https://blog.jooq.org/database-schema-navigation-in-java-2/

2011년 9월 11일, lukaseder 게시

jOOQ의 중요한 부분 중 하나는 데이터베이스 스키마 탐색 모듈인 jooq-meta이다. 이 모듈은 코드 생성기가 관련 스키마 객체를 발견하는 데 사용된다. 왜 SchemaCrawler나 SchemaSpy 같은 다른 라이브러리를 사용하지 않고 직접 만들었는지에 대한 질문을 여러 번 받았는데, 실제로 다른 안정적인 서드파티 제품에 의존할 수 없다는 것은 유감스러운 일이다. 다음은 데이터베이스 스키마 탐색에 대한 몇 가지 생각이다.

## 표준

SQL-92 표준은 RDBMS가 딕셔너리 테이블을 포함하는 INFORMATION_SCHEMA를 어떻게 구현*해야 하는지* 정의하고 있다. 그리고 실제로 일부 RDBMS는 표준 사양의 일부를 구현하고 있다. 이러한 RDBMS들은 표준의 일정한 구현을 함께 제공한다.

표준에 가까운 경우

- HSQLDB: 실제 표준에 매우 가까움
- Postgres: 표준에 가깝지만 일부 수정이 있음 (독자적인 딕셔너리 테이블도 보유)
- SQL Server: 표준에 가깝지만 상당히 불완전함 (독자적인 딕셔너리 테이블도 보유)

표준의 자유로운 해석

- H2 (최근에 일부 하위 호환성이 깨지는 변경이 있었음)
- MySQL (5.0 이후에만 해당, 독자적인 딕셔너리 테이블도 보유)

다른 RDBMS들은 딕셔너리 테이블에 대한 자체적인 아이디어를 제공한다. 이것은 jOOQ 같은 스키마 탐색 도구에게 매우 까다로운 부분이다. 딕셔너리 테이블의 현황은 다음과 같이 설명할 수 있다 (나의 편향된 의견):

깔끔하고 잘 문서화된 딕셔너리 테이블

- DB2: 이 딕셔너리 테이블들은 이름은 다르지만 표준과 비슷한 형태를 띤다. 직관적으로 느껴진다.
- Oracle: 내 의견으로는 표준이 제안한 것보다 더 나은 딕셔너리 뷰 세트를 가지고 있다. 이해하기 매우 쉽고 인터넷 곳곳에 잘 문서화되어 있다.
- SQLite: 딕셔너리 테이블은 없지만, SQLite의 저장 프로시저는 사용하기 매우 간단하다. 결국 단순한 데이터베이스이기 때문이다.

이해하기 어렵고 잘 문서화되지 않은 딕셔너리 테이블

- Derby: 관계(relation), 키(key) 등과 같은 일반적인 데이터베이스 용어를 사용하는 대신 집합체(conglomerate)라는 개념을 만들어냈다.
- MySQL: 이전의 mysql 스키마는 상당히 골치 아팠다. 다행히 MySQL 5.0부터는 더 이상 그렇지 않다.
- Ingres: 음... Ingres는 오래된 데이터베이스이다. 70년대에는 사용성이 주요 관심사가 아니었다...
- Sybase SQL Anywhere: 복잡한 관계로 조인해야 하는 객체가 많다. 문서가 부족하다.
- Sybase ASE: SQL Anywhere보다 훨씬 더 어렵다. 일부 데이터는 "트릭"을 써야만 얻을 수 있다.

## JDBC 추상화

딕셔너리 테이블의 다양성은 표준 추상화를 절실히 요구하는 것처럼 보인다. SQL-92 표준은 실제로 이러한 RDBMS 대부분에서 구현될 수 있지만, JDBC 추상화는 그보다 더 낫다. JDBC는 DatabaseMetaData 객체를 알고 있으며 데이터베이스 스키마를 쉽게 탐색할 수 있도록 해준다. 불행히도, 이 API는 종종 SQLFeatureNotSupportedException을 던진다. 어떤 JDBC 드라이버가 이 API를 얼마나 구현하고 있는지, 그리고 언제 우회 방법이 필요한지에 대한 일반적인 규칙이 없다. jOOQ 코드 생성에 있어서, 이러한 사실들은 이 API를 상당히 쓸모없게 만든다.

## 다른 도구들

앞서 언급한 바와 같이, 오픈소스 세계에는 몇 가지 다른 도구들이 있다. 다음은 jOOQ에서 이러한 도구들을 사용하는 데 있어 몇 가지 단점이다:

- 내가 아는 두 도구 모두 LGPL로 라이선스되어 있어, jOOQ의 Apache 2 라이선스와 잘 호환되지 않는다.
- 두 도구 모두 엔티티-관계를 매우 잘 탐색하지만, UDT(사용자 정의 타입), 고급 저장 프로시저 사용(예: 커서 반환, UDT 반환 등), ARRAY와 같은 많은 비표준 구조에 대한 지원이 부족한 것으로 보인다.
- SchemaCrawler는 8개의 RDBMS만 지원하지만, jOOQ는 현재 12개를 지원한다.
- 두 도구 모두 상당히 비활성 상태이다.

자세한 정보는 해당 사이트를 방문하면 된다:

- SchemaCrawler
- SchemaSpy

## jooq-meta

위의 이유들로 인해, jOOQ는 자체 데이터베이스 스키마 탐색 모듈인 jooq-meta를 함께 제공한다. 이 모듈은 JDBC의 DatabaseMetaData, SchemaCrawler 또는 SchemaSpy의 대안으로 독립적으로 사용할 수 있다. jooq-meta는 jOOQ로 작성된 쿼리를 사용하여 데이터베이스 메타데이터를 탐색하므로, 통합 테스트 스위트의 일부이기도 하다. 예를 들어, Ingres의 외래 키 관계가 jooq-meta로 어떻게 탐색되는지 살펴보자:

```java
Result<Record> result = create()
    .select(
        IirefConstraints.REF_CONSTRAINT_NAME.trim(),
        IirefConstraints.UNIQUE_CONSTRAINT_NAME.trim(),
        IirefConstraints.REF_TABLE_NAME.trim(),
        IiindexColumns.COLUMN_NAME.trim())
    .from(IICONSTRAINTS)
    .join(IIREF_CONSTRAINTS)
    .on(Iiconstraints.CONSTRAINT_NAME.equal(IirefConstraints.REF_CONSTRAINT_NAME))
    .and(Iiconstraints.SCHEMA_NAME.equal(IirefConstraints.REF_SCHEMA_NAME))
    .join(IICONSTRAINT_INDEXES)
    .on(Iiconstraints.CONSTRAINT_NAME.equal(IiconstraintIndexes.CONSTRAINT_NAME))
    .and(Iiconstraints.SCHEMA_NAME.equal(IiconstraintIndexes.SCHEMA_NAME))
    .join(IIINDEXES)
    .on(IiconstraintIndexes.INDEX_NAME.equal(Iiindexes.INDEX_NAME))
    .and(IiconstraintIndexes.SCHEMA_NAME.equal(Iiindexes.INDEX_OWNER))
    .join(IIINDEX_COLUMNS)
    .on(Iiindexes.INDEX_NAME.equal(IiindexColumns.INDEX_NAME))
    .and(Iiindexes.INDEX_OWNER.equal(IiindexColumns.INDEX_OWNER))
    .where(Iiconstraints.SCHEMA_NAME.equal(getSchemaName()))
    .and(Iiconstraints.CONSTRAINT_TYPE.equal("R"))
    .orderBy(
        IirefConstraints.REF_TABLE_NAME.asc(),
        IirefConstraints.REF_CONSTRAINT_NAME.asc(),
        IiindexColumns.KEY_SEQUENCE.asc())
    .fetch();
```

## 결론

다시 한번 말할 수 있는 것은 "RDBMS의 세계는 매우 이질적이다. Java에서의 데이터베이스 추상화는 JDBC, Hibernate/JPA, 그리고 SchemaCrawler, SchemaSpy, jooq-meta와 같은 서드파티 라이브러리 같은 기술들에서 어느 정도까지만 확립되어 있다."는 것이다.
