# jOOQ 3.20 출시 - ClickHouse, Databricks, 그리고 훨씬 더 많은 DuckDB 지원, 새로운 모듈, Oracle 타입 계층 구조, 더 많은 공간 함수 지원, DECFLOAT 및 시노님 지원, 숨겨진 컬럼, Scala 3, Kotlin 2 등

> 원문: [jOOQ 3.20 released with ClickHouse, Databricks, and much more DuckDB support, new modules, Oracle type hierarchies, more spatial support, decfloat and synonym support, hidden columns, Scala 3, Kotlin](https://blog.jooq.org/jooq-3-20-released-with-clickhouse-databricks-and-much-more-duckdb-support-new-modules-oracle-type-hierarchies-more-spatial-support-decfloat-and-synonym-support-hidden-columns-scala-3-kotlin/)

## 새로운 데이터베이스 방언(Dialect)

jOOQ 3.20은 2개의 새로운 실험적 SQL 방언을 도입합니다: jOOQ 오픈 소스 에디션을 포함한 모든 에디션에서 사용 가능한 ClickHouse와 jOOQ 엔터프라이즈 에디션에서 사용 가능한 Databricks입니다.

### ClickHouse

ClickHouse 지원은 실험적(experimental) 상태로 유지됩니다. ClickHouse는 역사적으로 벤더 고유의 구문을 사용해왔으며, SQL 표준 준수를 향해 점진적으로 이동하고 있는 빠르게 변화하는 SQL을 가지고 있기 때문입니다. 다른 데이터베이스에서 기대하는 것과 많은 동작이 다르며, 특히 NULL 처리가 표준 SQL과 매우 다릅니다.

### Databricks

Databricks 지원 역시 실험적 상태로 시작됩니다. Databricks는 "매우 유망한" 플랫폼으로 설명되며, 버전 3.21에서 포괄적인 기능 지원이 계획되어 있습니다.

## DuckDB 지원 강화

이번 릴리스에서는 DuckDB 지원이 크게 확장되었습니다. 추가된 기능은 다음과 같습니다:

- ARRAY, ROW, STRUCT 지원: DuckDB의 복합 타입에 대한 지원이 추가되었습니다.
- MULTISET 기능: MULTISET 처리가 가능해졌습니다.
- JSON 처리: JSON 데이터 핸들링이 개선되었습니다.
- 날짜-시간 산술: 날짜 및 시간 연산 기능이 추가되었습니다.
- 시퀀스: 시퀀스 지원이 포함되었습니다.
- 확장된 DDL/DML 지원: DDL 및 DML 기능이 확장되었습니다.
- 공간(Spatial) 기능: 공간 함수 지원이 추가되었습니다.
- 기타 추가 기능

## 새로운 확장 모듈

세 가지 새로운 전문 모듈이 도입되어 코어 라이브러리에서 무거운 의존성을 제거했습니다.

### jOOQ-reactor-extensions

Reactor의 리액티브 스트림 API와 통합하기 위한 모듈입니다. 새로운 `SubscriberProvider` SPI를 통해 R2DBC 내부에서 Reactor Context 인식이 가능합니다.

### jOOQ-beans-extensions

`@ConstructorProperties` 어노테이션 지원을 담당하는 모듈입니다. 이를 통해 JDK 데스크탑 모듈 의존성이 코어에서 분리되었습니다.

### jOOQ-jpa-extensions

`@Column`, `@Table` 등의 JPA 어노테이션 지원을 재배치한 모듈입니다. 이를 통해 선택적이었던 `jakarta.persistence` 의존성이 코어 라이브러리에서 제거되었습니다.

## Oracle 타입 계층 구조

Oracle은 풍부한 객체 지향 PL/SQL 언어 기능을 갖춘, 가장 정교한 ORDBMS 구현체입니다. jOOQ 3.20에서는 코드 생성기와 런타임 라이브러리 모두에서 PL/SQL OBJECT 타입 계층 구조를 지원합니다. 이를 통해 정교한 데이터베이스 스키마를 사용하는 환경에서 jOOQ의 활용도가 높아집니다.

이 기능은 상용(commercial) 전용 기능입니다.

## 공간 함수(Spatial) 지원 강화

많은 추가 공간 함수가 jOOQ의 공간 도구에 통합되었으며, 특히 DuckDB와 Oracle 구현이 개선되었습니다. 사용자는 매뉴얼에서 공간 함수와 술어(predicate)에 대한 전체 세부 사항을 참조할 수 있습니다.

이 기능은 상용(commercial) 전용 기능입니다.

## DECFLOAT 지원

여러 데이터베이스 시스템은 REAL, DOUBLE PRECISION, FLOAT 같은 기존의 이진 부동소수점 옵션 외에 십진 부동소수점(decimal floating point) 타입을 제공합니다. 새로운 `org.jooq.Decfloat` 타입은 코드 생성 시 이러한 특수 수치 타입을 캡처할 수 있게 합니다.

## 시노님(Synonym) 지원

여러 방언에서는 데이터베이스 객체에 대한 대체 이름으로 SYNONYM 또는 ALIAS 객체를 지원합니다. jOOQ 3.20 릴리스에서는 코드 생성과 DDL 작업 모두에서 이러한 시노님을 처리합니다. 향후 Kotlin/Scala 타입 별칭과 같은 개선도 고려되고 있습니다.

이 기능은 상용(commercial) 전용 기능입니다.

## 숨겨진 컬럼(Hidden Columns)

이제 컬럼을 애스터리스크(`*`) 확장, `selectFrom()` 호출, 생성된 레코드에서 숨길 수 있으며, 명시적으로 참조하면 여전히 사용할 수 있습니다. 이 기능은 기존 구조를 깨뜨리지 않고 스키마 진화(schema evolution)를 용이하게 합니다. 컬럼 사용 중단(deprecation) 도구와 함께 사용할 수 있습니다.

이 기능은 상용(commercial) 전용 기능입니다.

## Kotlin 2 및 Scala 3 지원

jOOQ 3.20은 공식적으로 Kotlin 2와 Scala 3을 모두 지원합니다. 코어 라이브러리, 코드 생성 도구, 확장 모듈 전반에 걸쳐 포괄적인 통합 테스트가 이루어졌습니다.

## JDK 21 기준선

오픈 소스 에디션은 이제 최소 기준선으로 JDK 21을 요구합니다. 다만, 이전 JDK 버전은 상용 배포판을 통해 계속 지원됩니다.

## 레코드 더티 트래킹(Record Dirty Tracking)

사용자는 기본 "터치(touched)" 의미론을 "수정(modified)" 의미론으로 재정의할 수 있습니다. 이를 통해 실제로 수정된 값만 데이터베이스에 전송하여 보다 효율적인 데이터베이스 업데이트가 가능합니다.

## DML 조인(Join) 개선

`DELETE .. USING`과 `UPDATE .. FROM` 구문이 SQL 변환을 통해 모든 RDBMS 플랫폼에서 동작하도록 지원됩니다. MERGE 구문도 개선되어 `BY SOURCE`/`BY TARGET` 지원과 다중 `WHEN NOT MATCHED AND` 절을 사용할 수 있게 되었습니다.

## 코드 생성 개선

인터페이스, `immutablePOJO`, UDT 생성에서 이러한 기능을 조합할 때 발생하는 엣지 케이스가 수정되었습니다.

## 매뉴얼 검색

사용자 문서에 페이지 내 검색(in-page search) 기능이 추가되었습니다.
