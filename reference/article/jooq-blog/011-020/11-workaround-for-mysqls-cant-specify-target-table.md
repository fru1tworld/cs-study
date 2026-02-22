# MySQL의 "can't specify target table for update in FROM clause" 오류 해결 방법

> 원문: https://blog.jooq.org/workaround-for-mysqls-cant-specify-target-table-for-update-in-from-clause-error/

MySQL은 UPDATE 문에서 서브쿼리가 동일한 테이블을 참조할 때 해당 테이블을 업데이트하는 것을 금지한다. 다음 예제는 실패한다:

```sql
create table t (i int primary key, j int);
insert into t values (1, 1);

update t
set j = (select max(j) from t) + 1;
```

이 쿼리는 다음과 같은 오류를 발생시킨다: "You can't specify target table 't' for update in FROM clause"

## 왜 이것이 중요한가

이 제한은 오랫동안 MySQL 버그로 간주되어 왔다(최소 버그 #66455부터 보고됨). MariaDB 10.2와 SingleStore 6(구 MemSQL)을 포함한 대부분의 다른 관계형 데이터베이스 시스템은 이 시나리오를 문제 없이 처리한다.

## jOOQ의 해결 방법

jOOQ 라이브러리는 문제가 되는 UPDATE 및 DELETE 문을 자동으로 변환하여, 서브쿼리를 파생 테이블(derived table)로 감싸는 방식으로 이 문제를 해결한다:

```sql
update t
set j = (
  select *
  from (
    select max(j) from t
  ) t
) + 1;
```

이 해결 방법은 쿼리의 의도된 로직을 유지하면서 구문 오류를 제거한다. 이 접근 방식은 MySQL 8.0 이상의 공식 MySQL 문서에 기록된 해결책을 반영한 것이다.

이 제한은 UPDATE 및 DELETE 작업에서 술어(predicate)가 대상 테이블 자체에 의존하는 자기 참조적 조건에 영향을 미친다. jOOQ는 대상 테이블에 의존하는 술어를 가진 UPDATE 또는 DELETE 문을 감지하면 이 변환을 자동으로 적용한다. 따라서 jOOQ를 사용하는 개발자는 MySQL의 이 제한을 우회하기 위해 쿼리를 수동으로 재구성할 필요가 없다.
