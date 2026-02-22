# 새로운 Traverser API를 사용한 jOOQ 표현식 트리 탐색

> 원문: https://blog.jooq.org/traversing-jooq-expression-trees-with-the-new-traverser-api/

## 개요

jOOQ 3.16부터 프레임워크는 내부 쿼리 객체 모델(QOM)에 접근하고 조작하기 위한 실험적 API를 도입했습니다. 이러한 기능을 통해 개발자는 SQL 표현식 트리를 탐색하고 변환할 수 있으며, 이는 jOOQ의 파서를 사용하거나 동적 SQL 변환을 구현하는 경우에 특히 유용합니다.

## 쿼리 객체 모델(QOM) API

새로운 `org.jooq.impl.QOM` 타입은 쿼리 객체 모델을 공개 API로 노출합니다. `SUBSTRING()`과 같은 표현식을 생성할 때, 개발자는 "$" 접두사가 붙은 접근자 메서드를 사용하여 내부 구성 요소에 접근할 수 있습니다:

```java
Field<String> field = substring(BOOK.TITLE, 2, 4);
if (field instanceof QOM.Substring substring) {
    Field<String> string = substring.$string();
    Field<? extends Number> startingPosition =
        substring.$startingPosition();
}
```

각 접근자에는 수정된 값으로 새로운 불변 인스턴스를 생성하는 대응하는 변경자 메서드가 있습니다. 이러한 설계는 원본 객체를 변경하지 않고 유지합니다.

## 표현식 트리 탐색

`Traverser` API(상용 배포판에서 사용 가능)는 JDK `Collector`와 유사하게 동작하지만 트리에 특화된 기능을 제공합니다:

- 트리 요소를 방문하기 전후에 이벤트를 수신합니다
- 하위 트리로 재귀할지 여부를 결정합니다
- 조기 탐색 종료가 가능합니다
- 병렬 처리는 지원하지 않습니다

### 탐색 예제

표현식에 포함된 모든 QueryPart 객체 수를 세는 예제:

```java
System.out.println(
    T_BOOK.ID.eq(1).or(T_BOOK.ID.eq(2))
        .$traverse(() -> 0, (c, p) -> c + 1)
);
// Output: 7
```

탐색은 깊이 우선 순서로 요소를 처리합니다: OR 표현식, 두 개의 EQ 표현식, 테이블/컬럼 참조, 그리고 리터럴 값입니다.

### 요소 수집

```java
System.out.println(
    T_BOOK.ID.eq(1).or(T_BOOK.ID.eq(2))
        .$traverse(Traversers.collecting(Collectors.toList()))
);
```

JDK `Collector` 인스턴스는 `Traverser` 구현체로 직접 사용할 수 있습니다.

## 표현식 트리 변환

`$replace()` 메서드는 패턴 매칭과 SQL 재작성을 가능하게 합니다. 중복된 부정을 제거하는 예제:

```java
Condition c = not(not(BOOK.ID.eq(1)));
System.out.println(c.$replace(q ->
    q instanceof Not n1 && n1.$arg1() instanceof Not n2
        ? n2.$arg1()
        : q
));
// Output: "BOOK"."ID" = 1
```

## 활용 사례

SQL 최적화 및 정규화
- 중복 제거 (예: `UPPER(UPPER(x))` -> `UPPER(x)`)
- 중복 SQL 식별을 위한 패턴 감지
- 조인 제거 및 기타 표준 최적화

행 수준 보안(Row Level Security)
사용자 컨텍스트에 따라 데이터 접근을 제한하는 조건을 쿼리에 투명하게 주입합니다. `SELECT * FROM account`와 같은 쿼리를 적절한 `WHERE` 절이 포함된 쿼리로 변환합니다.

소프트 삭제(Soft Deletion)
`DELETE` 문을 삭제 플래그를 설정하는 `UPDATE` 문으로 변환하고, `SELECT` 문에서는 삭제된 레코드를 필터링하도록 수정합니다.

감사 컬럼 지원(Audit Column Support)
DML 작업 전반에 걸쳐 `CREATED_AT`, `CREATED_BY`, `MODIFIED_AT`, `MODIFIED_BY`와 같은 감사 필드를 자동으로 업데이트합니다.

## 현재 제한 사항 (jOOQ 3.16)

- SELECT 문만 지원합니다
- 탐색이 아직 JOIN 트리나 UNION/INTERSECT/EXCEPT 서브쿼리 내부까지 침투하지 않습니다
- API는 실험적이며 호환되지 않는 변경이 있을 수 있습니다

## 호환성

이러한 기능은 DSL 구성, SQL 파싱, 방언 변환, 진단 유틸리티 전반에 걸쳐 사용 사례에 구애받지 않고 작동하므로, 다양한 애플리케이션 아키텍처에 적용할 수 있습니다.
