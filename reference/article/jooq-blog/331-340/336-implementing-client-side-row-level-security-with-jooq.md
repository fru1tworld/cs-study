# jOOQ로 클라이언트 측 행 수준 보안 구현하기

> 원문: https://blog.jooq.org/implementing-client-side-row-level-security-with-jooq/

## 업데이트

jOOQ 3.19부터는 정책(policies)이 기본적으로 지원됩니다. 이 기능을 사용하면 이 글에서 설명하는 수동 접근 방식이 더 이상 필요하지 않습니다. 이 글은 "뷰의 제약조건(Constraints on Views)"에 대한 이전 글의 후속편입니다.

## 행 수준 보안이란?

행 수준 보안(Row-Level Security)은 본질적으로 특정 데이터베이스 세션이 데이터베이스의 _일부_ 행에만 접근할 수 있고 다른 행에는 접근할 수 없음을 의미합니다.

은행 업무를 예로 들어보겠습니다. 결혼한 부부 John과 Jane이 있습니다. 둘 다 계좌를 가지고 있으며, John은 Jane에게 자신의 "비밀 포커 자금" 계좌를 숨기고 싶어합니다. 다른 계좌들은 공유하지만 이 계좌만은 비밀로 유지하고 싶은 것입니다.

행 수준 보안은 제한된 데이터를 완전히 숨깁니다. 이는 직접적인 쿼리뿐만 아니라 집계(aggregation)와 필터링 작업에도 영향을 미칩니다. 예를 들어, Jane이 모든 계좌의 합계를 조회하면 John의 비밀 계좌는 완전히 제외됩니다.

Oracle이나 PostgreSQL 같은 일부 데이터베이스는 네이티브 행 수준 보안을 제공합니다. 하지만 모든 데이터베이스가 이 기능을 갖추고 있지는 않습니다. 이러한 기능이 없는 데이터베이스의 경우 대안적인 접근 방식이 필요합니다.

## 레거시 데이터베이스 접근 방식

네이티브 지원이 없는 데이터베이스의 경우, 컨텍스트 변수와 함께 뷰를 사용할 수 있습니다. Oracle에서는 `SYS_CONTEXT`를 사용하여 세션별 접근을 설정합니다:

```sql
CREATE VIEW my_accounts AS
SELECT ...
WHERE p.privilege_owner = SYS_CONTEXT('banking', 'user');
```

## 최신 jOOQ 솔루션 (3.19+)

jOOQ 3.19 버전부터는 내장된 정책 지원이 제공됩니다. 다음과 같이 접근합니다:

```java
Configuration configuration = ...;
configuration.set(new DefaultPolicyProvider()
    .append(ACCOUNT_PRIVILEGES,
        ACCOUNT_PRIVILEGES.PRIVILEGE_OWNER.eq("My User"))
);
```

이 방식은 서브쿼리와 DML 문을 포함한 모든 쿼리에 자동으로 조건자(predicate)를 추가합니다.

## VisitListener를 사용한 기존 구현 방식

네이티브 정책 기능이 도입되기 전에는 jOOQ의 `VisitListener` SPI를 사용하여 커스텀 솔루션을 구현할 수 있었습니다. 이 Service Provider Interface는 jOOQ 사용자가 SQL 문이 생성되는 동안 SQL AST(Abstract Syntax Tree) 변환을 수행할 수 있게 해줍니다.

### 구현 요구사항

솔루션은 여러 시나리오를 처리해야 합니다:

간단한 WHERE 절 추가: 기본 쿼리에 조건을 추가하여 변환:
```sql
FROM accounts
-- 다음으로 변환됩니다
FROM accounts WHERE accounts.id IN (?, ?)
```

기존 WHERE 절: WHERE를 대체하지 않고 AND로 추가 조건 연결:
```sql
WHERE accounts.account_owner = 'John'
-- 다음으로 변환됩니다
WHERE accounts.account_owner = 'John' AND accounts.id IN (?, ?)
```

테이블 별칭: 별칭이 사용될 때 올바른 참조 유지 (예: "accounts a"):
```sql
FROM accounts a
-- 다음으로 변환됩니다
FROM accounts a WHERE a.id IN (?, ?)
```

서브쿼리와 조인: 중첩된 쿼리에 재귀적으로 제한을 적용하여 적절한 중첩 레벨에 조건이 나타나도록 함.

외래 키 참조: 참조되는 테이블이 명시적으로 조인되지 않았을 때도 조건자를 추가하여 관계 경로를 통해 데이터 무결성 보호.

## 기술적 구현 세부사항

### 스택 기반 아키텍처

구현은 VisitContext에서 두 개의 스택을 유지합니다:
- 각 서브쿼리 레벨의 조건자(predicates)를 추적하는 조건 스택
- WHERE 절이 이미 존재하는지 나타내는 플래그 스택

핵심 아이디어는 어떤 형태로든 `accounts` 테이블을 참조하는 모든 쿼리를 찾아 간단한 조건자를 추가하여 접근 제어를 구현하는 것입니다.

### Import 문

```java
import static java.util.Arrays.asList;
import static org.jooq.Clause.DELETE;
import static org.jooq.Clause.DELETE_DELETE;
import static org.jooq.Clause.DELETE_WHERE;
import static org.jooq.Clause.INSERT;
import static org.jooq.Clause.SELECT;
import static org.jooq.Clause.SELECT_FROM;
import static org.jooq.Clause.SELECT_WHERE;
import static org.jooq.Clause.TABLE_ALIAS;
import static org.jooq.Clause.UPDATE;
import static org.jooq.Clause.UPDATE_UPDATE;
import static org.jooq.Clause.UPDATE_WHERE;
import static org.jooq.test.h2.generatedclasses.Tables.ACCOUNTS;
import static org.jooq.test.h2.generatedclasses.Tables.TRANSACTIONS;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.jooq.Clause;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Operator;
import org.jooq.QueryPart;
import org.jooq.Table;
import org.jooq.VisitContext;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultVisitListener;
```

### 기본 필터 클래스 구조

```java
public class AccountIDFilter extends DefaultVisitListener {

    final Integer[] ids;

    public AccountIDFilter(Integer... ids) {
        this.ids = ids;
    }
}
```

### 스택 관리 메서드

```java
void push(VisitContext context) {
    conditionStack(context).push(new ArrayList<>());
    whereStack(context).push(false);
}

void pop(VisitContext context) {
    whereStack(context).pop();
    conditionStack(context).pop();
}

Deque<List<Condition>> conditionStack(VisitContext context) {
    Deque<List<Condition>> data = (Deque<List<Condition>>)
        context.data("conditions");

    if (data == null) {
        data = new ArrayDeque<>();
        context.data("conditions", data);
    }

    return data;
}

Deque<Boolean> whereStack(VisitContext context) {
    Deque<Boolean> data = (Deque<Boolean>)
        context.data("predicates");

    if (data == null) {
        data = new ArrayDeque<>();
        context.data("predicates", data);
    }

    return data;
}
```

### 접근자 유틸리티 메서드

```java
List<Condition> conditions(VisitContext context) {
    return conditionStack(context).peek();
}

boolean where(VisitContext context) {
    return whereStack(context).peek();
}

void where(VisitContext context, boolean value) {
    whereStack(context).pop();
    whereStack(context).push(value);
}
```

### Clause Start 리스너

```java
@Override
public void clauseStart(VisitContext context) {

    // 새로운 SELECT 절 / 중첩 select 또는 DML 문에 진입
    if (context.clause() == SELECT ||
        context.clause() == UPDATE ||
        context.clause() == DELETE ||
        context.clause() == INSERT) {
        push(context);
    }
}
```

### Clause End 리스너

```java
@Override
public void clauseEnd(VisitContext context) {

    // 조건자가 있다면 수집된 모든 조건자를 WHERE 절에 추가
    if (context.clause() == SELECT_WHERE ||
        context.clause() == UPDATE_WHERE ||
        context.clause() == DELETE_WHERE) {
        List<Condition> conditions =
            conditions(context);

        if (conditions.size() > 0) {
            context.context()
                   .formatSeparator()
                   .keyword(where(context)
                   ? "and"
                   : "where"
                   )
                   .sql(' ');

            context.context().visit(
                DSL.condition(Operator.AND, conditions)
            );
        }
    }

    // SELECT 절 / 중첩 select 또는 DML 문을 떠남
    if (context.clause() == SELECT ||
        context.clause() == UPDATE ||
        context.clause() == DELETE ||
        context.clause() == INSERT) {
        pop(context);
    }
}
```

### Visit End 메서드

```java
@Override
public void visitEnd(VisitContext context) {

    // 이것이 무엇을 의미하는지 곧 살펴보겠습니다...
    pushConditions(context, ACCOUNTS,
        ACCOUNTS.ID, ids);
    pushConditions(context, TRANSACTIONS,
        TRANSACTIONS.ACCOUNT_ID, ids);

    // WHERE 절 내에서 조건을 렌더링하고 있는지 확인
    // 이 경우, jOOQ가 WHERE 키워드를 렌더링할 것임을 확신할 수 있습니다
    if (context.queryPart() instanceof Condition) {
        List<Clause> clauses = clauses(context);

        if (clauses.contains(SELECT_WHERE) ||
            clauses.contains(UPDATE_WHERE) ||
            clauses.contains(DELETE_WHERE)) {
            where(context, true);
        }
    }
}
```

### Push Conditions 핵심 로직

```java
<E> void pushConditions(
        VisitContext context,
        Table<?> table,
        Field<E> field,
        E... values) {

    // 주어진 테이블을 방문하고 있는지 확인
    if (context.queryPart() == table) {
        List<Clause> clauses = clauses(context);

        // ... 그리고 현재 서브셀렉트의 FROM 절 컨텍스트에 있는지 확인
        if (clauses.contains(SELECT_FROM) ||
            clauses.contains(UPDATE_UPDATE) ||
            clauses.contains(DELETE_DELETE)) {

            // TABLE_ALIAS를 선언하고 있다면...
            // (예: "ACCOUNTS" as "a")
            if (clauses.contains(TABLE_ALIAS)) {
                QueryPart[] parts = context.queryParts();

                // ... QueryPart 방문 경로를 거슬러 올라가
                // 정의하는 별칭 테이블을 찾고, 그로부터
                // 별칭된 필드를 추출합니다. (즉, "a" 참조)
                for (int i = parts.length - 2; i >= 0; i--) {
                    if (parts[i] instanceof Table) {
                        field = ((Table<?>) parts[i]).field(field);
                        break;
                    }
                }
            }

            // (잠재적으로 별칭이 지정된) 테이블의
            // 필드에 대한 조건을 푸시
            conditions(context).add(field.in(values));
        }
    }
}
```

### Clause 헬퍼 메서드

```java
List<Clause> clauses(VisitContext context) {
    List<Clause> result = asList(context.clauses());
    int index = result.lastIndexOf(SELECT);

    if (index > 0)
        return result.subList(index, result.size() - 1);
    else
        return result;
}
```

### 사용/테스트 예제

```java
Configuration configuration = create().configuration();

// 이 설정은 모든 행에 대한 전체 접근 권한을 가집니다
DSLContext fullaccess = DSL.using(configuration);

// 이 설정은 ID 1과 2에 대해서만 제한된 접근 권한을 가집니다
DSLContext restricted = DSL.using(
    configuration.derive(
        DefaultVisitListenerProvider.providers(
            new AccountIDFilter(1, 2)
        )
    )
);

// 계좌 조회
assertEquals(3, fullaccess.fetch(ACCOUNTS).size());
assertEquals(2, restricted.fetch(ACCOUNTS).size());
```

## 테스트 케이스와 예제

구현은 생성된 SQL을 보여주는 여러 테스트 시나리오를 통해 검증됩니다:

기본 조회: 특정 ID(1과 2)에 대한 계좌 접근 제한. 전체 접근은 3개의 계좌를 반환하고 제한된 접근은 2개를 반환합니다.

트랜잭션 필터링: 관련 테이블을 통해 조건자가 어떻게 전파되는지 보여줍니다. 제한된 접근은 6개의 트랜잭션 중 5개를 반환합니다.

조건부 쿼리: 기존 WHERE 절이 있는 쿼리를 테스트하여 절 대체가 아닌 AND 추가 동작을 확인합니다.

별칭 테이블: 별칭이 올바르게 추적되고 제한이 정확히 적용되는지 확인합니다.

중첩 쿼리: 서브쿼리가 제한된 테이블을 참조할 때 여러 중첩 레벨에서 조건자가 나타나는지 검증합니다.

## 중요한 보안 관련 주의사항

보안에서 항상 그렇듯이, 여러 계층에서 보안을 구현해야 합니다. SQL AST 변환은 간단하지 않으며, 위의 시나리오들은 불완전합니다.

제한 사항:
- jOOQ AST를 사용하여 빌드된 쿼리에서만 작동합니다
- 일반 SQL 쿼리에서는 작동하지 않습니다
- JDBC를 통해 직접 실행되는 쿼리에서는 작동하지 않습니다
- Hibernate에서는 작동하지 않습니다
- 저장 프로시저에서는 작동하지 않습니다
- `accounts` 테이블을 참조하는 뷰에서는 작동하지 않습니다
- 단순 테이블 시노님(synonyms)에서는 작동하지 않습니다

이 글을 행 수준 보안의 완전한 솔루션이 아닌, AST 변환 수행 방법을 보여주는 튜토리얼로 읽어주세요.

## 결론 및 확장 응용

행 수준 보안 외에도 `VisitListener` SPI는 다른 변환을 가능하게 합니다:
- WHERE 절이 없는 DML 쿼리에 대해 예외 발생시키기
- 조건에 따라 테이블을 대안으로 교체하기

jOOQ의 장점은 다음과 같습니다: jOOQ를 사용하면 SQL을 변환하기 위해 파싱할 필요가 없습니다 (SQL 방언에 따라 파싱은 매우 어렵습니다). jOOQ의 플루언트 API를 사용하여 이미 수동으로 Abstract Syntax Tree를 빌드하므로, 이러한 모든 기능을 무료로 얻을 수 있습니다.
