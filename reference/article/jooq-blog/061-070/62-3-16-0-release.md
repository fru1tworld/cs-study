# 3.16.0 릴리스 - 새로운 공개 쿼리 객체 모델 API, 공간 지원, YugabyteDB 지원 등

> 원문: https://blog.jooq.org/3-16-0-release-with-a-new-public-query-object-model-api-spatial-support-yugabytedb-support-and-much-more/

이번 릴리스는 사용자들이 오랫동안 요청해 온 두 가지 장기적이고 복잡한 기능 요청을 다룹니다: jOOQ의 쿼리 객체 모델(QOM)을 조작하기 위한 공개 API와 공간 지원입니다.

## 새로운 쿼리 객체 모델 (QOM)

모든 jOOQ 쿼리는 직관적인 DSL을 통해 구성된 표현식 트리로 모델링됩니다. 일부 사용 사례에서는 역사적인 모델 API 버전(예: SelectQuery)이 존재하지만, 이러한 모델들은 읽거나 변환할 수 없습니다. 이제 우리는 사용자가 소비하고 조작할 수 있도록 대부분의 표현식 트리 모델을 공개 API로 제공하기 시작합니다. 모든 표현식 트리 요소는 `org.jooq.impl.QOM`에 해당하는 타입을 가집니다. 모든 타입은 `"$"` 접두사가 붙은 메서드 이름을 사용하여 구성 요소에 대한 접근을 제공합니다. 예를 들어:

```java
// DSL을 사용하여 필드 생성
Field<String> field = substring(BOOK.TITLE, 2, 4);

// 모델 API를 사용하여 표현식 내부에 접근
if (field instanceof QOM.Substring substring) {
    Field<String> string = substring.$string();
    Field<? extends Number> startingPosition = substring.$startingPosition();
    Field<? extends Number> length = substring.$length();
}
```

새로운 API는 실험적이며 다음 마이너 릴리스에서 변경될 수 있습니다.

라이선스를 보유한 파워 유저들은 표현식 트리를 순회하고 변환하기 위한 보조 API를 얻게 됩니다. 예를 들어 순회:

```java
// 조건에 있는 쿼리 파트의 수를 셉니다
long count = BOOK.ID.eq(1).or(BOOK.ID.eq(2))
    .$traverse(Traversers.collecting(Collectors.counting()));
```

위의 예제는 조건 내의 쿼리 파트 수를 계산하여 "7개의 쿼리 파트를 포함"이라는 결과를 출력합니다.

그리고 변환의 예:

```java
// 이중 부정 NOT NOT 제거
Condition c = not(not(BOOK.ID.eq(1)));
System.out.println(c.$replace(q ->
    q instanceof QOM.Not n1 && n1.$arg1() instanceof QOM.Not n2
        ? n2.$arg1()
        : q
));
```

위 코드는 중복된 NOT 연산자를 제거하여 다음과 같이 출력합니다:

```sql
"BOOK"."ID" = 1
```

이 새로운 API는 다음과 같은 훨씬 더 정교한 동적 SQL 사용 사례에 매우 강력합니다:

- 위의 NOT NOT 예제와 같은 SQL 표현식 최적화
- 행 수준 보안(Row level security)
- 소프트 삭제(Soft deletion)
- 공유 스키마 멀티 테넌시(Shared schema multi tenancy)
- 감사 컬럼 지원(Audit column support)
- 그 외 많은 것들 (향후 블로그와 기본 제공 변환 기능을 기대해 주세요)

더 많은 정보는 다음을 참조하세요: https://www.jooq.org/doc/dev/manual/sql-building/model-api/

## 공간 지원

상업적 라이선스를 보유한 고객들에게 배포되기 시작하는 오랫동안 기다려온 기능은 공간 지원입니다. 많은 데이터베이스 방언(dialect)이 ISO/IEC 13249-3:2016 SQL 표준 확장을 지원하며, 마침내 우리도 지원합니다. jOOQ는 WKB(Well-Known Binary) 또는 WKT(Well-Known Text) 데이터를 포함하는 표준화된 바인드 변수로 사용할 수 있는 GEOMETRY 및 GEOGRAPHY 데이터를 위한 새로운 보조 데이터 타입과 다양한 기본 제공 함수 및 술어(predicate)를 도입합니다.

## 새로운 방언 및 버전

jOOQ 오픈 소스 에디션을 포함한 모든 jOOQ 에디션에 공식적으로 지원되는 또 다른 새로운 SQL 방언이 추가되었습니다: YugabyteDB입니다. 이것은 후원 통합이었습니다. Yugabyte에 매우 감사드립니다!

지원되는 버전은 다음과 같습니다:

- Firebird 4.0
- H2 2.0.202
- MariaDB 10.6
- PostgreSQL 14
- Oracle 21c

## 계산 컬럼, 읽기 전용 컬럼, ROWID 포함

많은 방언이 계산 컬럼("생성" 컬럼)을 지원하며, 이제 jOOQ에서도 이를 지원합니다. 대부분의 사용 사례에서 이것은 jOOQ 사용에 영향을 미치지 않지만, 특히 CRUD 코드를 작성할 때 새로운 읽기 전용 컬럼 기능은 CRUD 작업에서 계산 컬럼을 수동으로 제외해야 하는 것을 피하는 데 매우 유용할 수 있습니다. 이것은 또한 사용자가 기본 키 대신 합성 ROWID 컬럼으로 작업하도록 선택할 수 있는 새롭고 개선된 ROWID 지원도 포함합니다.

## Jakarta EE

우리는 Java EE에서 Jakarta EE 의존성으로 전환했습니다. 이 변경은 현재 하위 호환성이 없습니다. 그 이유는:

- 관련 코드 유지 관리를 크게 용이하게 합니다
- 두 의존성을 모두 갖는 것으로 인한 수많은 사용자 문제를 방지합니다
- 우리는 실제로 Java EE / Jakarta EE와 긴밀하게 통합하지 않습니다

다음 Jakarta EE 모듈이 영향을 받습니다:

- JAXB: 구성 객체를 로드하는 데 사용합니다
- Validation: 코드 생성기에 의해 생성될 수 있는 어노테이션입니다
- JPA: DefaultRecordMapper와 JPADatabase에서 사용됩니다

## 다양한 개선 사항

모든 마이너 릴리스와 마찬가지로 많은 작은 개선 사항이 구현되었습니다:

- PostgreSQL 프로시저가 이제 코드 생성 및 런타임에서 지원됩니다
- MULTISET 에뮬레이션을 포함한 SQLite JSON 지원이 추가되었습니다
- 많은 MULTISET / ROW 개선 사항이 구현되었습니다
- R2DBC 0.9가 릴리스되었으며, 우리의 의존성을 업그레이드했습니다
- Java 17 배포판은 이제 Java 16 대신 Java 17을 필요로 합니다
- jOOQ 3.6 이전 버전의 사용 중단(deprecation)이 제거되었습니다

전체 릴리스 노트는 여기서 확인할 수 있습니다.
