# jOOQ의 ExecuteListener로 연결에서 쓰기 작업 방지하기

> 원문: https://blog.jooq.org/using-jooqs-executelistener-to-prevent-write-operations-on-a-connection/

## 소개

데이터 접근 계층에서 보안은 매우 중요합니다. 대부분의 상용 데이터베이스는 데이터베이스 접근 권한 부여를 통해 세밀한 권한 제어를 제공합니다. 예를 들어, 관리자는 GRANT 문을 사용하여 특정 테이블이나 뷰에 대한 사용자의 접근을 제한할 수 있습니다:

```sql
GRANT SELECT ON table TO user;
```

이 접근 방식은 데이터베이스 수준에서 직접 특정 데이터베이스 객체에 대한 쓰기 작업을 방지합니다.

## 도전 과제: 데이터베이스 제어를 사용할 수 없는 경우

모든 데이터베이스가 정교한 접근 권한 구현을 제공하는 것은 아닙니다. 또한 운영상의 제약으로 인해 애플리케이션이 이러한 기능을 활용하지 못할 수도 있습니다. 이러한 경우, 대략적인 접근 제어를 위해 jOOQ의 `ExecuteListener`를 사용하거나 세밀한 제어를 위해 jOOQ의 `VisitListener`를 사용하여 클라이언트 측에서 보안을 구현해야 합니다.

## 해결책 1: 읽기 전용 접근을 위한 ExecuteListener

다음은 `ExecuteListener`를 사용한 기본 예제입니다:

```java
class ReadOnlyListener extends DefaultExecuteListener {
    @Override
    public void executeStart(ExecuteContext ctx) {
        if (ctx.type() != READ)
            throw new DataAccessException("No privilege to execute " + ctx.sql());
            // "실행 권한이 없습니다: " + ctx.sql()
    }
}
```

이 리스너를 jOOQ Configuration에 통합하면 해당 연결에서 쓰기 작업이 불가능해집니다. 구현이 간단하고 효과적입니다.

## 해결책 2: 테이블 수준 제어를 위한 VisitListener

테이블별 접근 제어 목록(ACL) 구현과 같이 더 세밀한 제어가 필요한 경우, `VisitListener`가 필요한 기능을 제공합니다. 다음은 간단한 예제입니다:

```java
static class ACLListener extends DefaultVisitListener {

    @Override
    public void visitStart(VisitContext context) {
        if (context.queryPart() instanceof Table
                && Arrays.asList(context.clauses()).contains(INSERT_INSERT_INTO)
                && ((Table<?>) context.queryPart()).getName().equals("AUTHOR"))
            throw new DataAccessException("No privilege to insert into AUTHOR");
            // "AUTHOR 테이블에 삽입 권한이 없습니다"
    }
}
```

이 구현은 클라이언트 세션이 특정 AUTHOR 테이블에 대해 INSERT 문을 실행하는 것을 방지합니다.

## 향후 개발

향후 jOOQ 릴리스에서는 GitHub 이슈 #5197을 해결하여 ACL VisitListener를 내장 기능으로 포함할 계획입니다.
