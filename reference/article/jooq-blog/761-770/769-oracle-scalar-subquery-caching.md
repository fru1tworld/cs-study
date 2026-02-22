# Oracle 스칼라 서브쿼리 캐싱

> 원문: https://blog.jooq.org/oracle-scalar-subquery-caching/

대규모 시스템에서 실행되는 SQL을 (jOOQ를 사용하든 순수 JDBC를 사용하든) 완전히 제어할 수 있는 것이 얼마나 중요한지는, 특정 RDBMS에 맞게 SQL 쿼리를 세밀하게 튜닝해야 할 때마다 분명해집니다. 이번 경우에는, Oracle과 그 놀라운 스칼라 서브쿼리 캐싱 메커니즘에 대해 살펴봅니다. 보통 SQL에서 PL/SQL로, 그리고 다시 SQL로 돌아오는 컨텍스트 스위치는 일반적인 Oracle 쿼리에서 상당히 비용이 큽니다. 그래서 보통은 저장 함수(stored function)를 필터링, 그룹핑, 정렬 조건에 넣는 것을 지양해야 합니다. 설령 그 함수가 `DETERMINISTIC`으로 선언되어 있더라도 말입니다. 그런데 대규모 결과 집합을 다룰 때는, 쿼리의 프로젝션(projection)에 있는 함수조차도 실행 계획에 악영향을 줄 수 있습니다. 저는 최근 Stack Overflow에 질문을 올렸는데, 구체적인 사용 사례에서 활용할 수 있는 캐싱 메커니즘이 있는지 궁금했기 때문입니다.

https://stackoverflow.com/questions/7270467/is-there-a-pl-sql-pragma-similar-to-deterministic-but-for-the-scope-of-one-singl

그리고 매우 흥미롭게도, 여러 가지 방법이 있었습니다. 채택된 답변은 Ask Tom의 Q&A에 있는 매우 관련성 높은 글을 가리키고 있습니다.

https://www.oracle.com/technetwork/issue-archive/2011/11-sep/o51asktom-453438.html

놀랍게도, 스칼라 서브쿼리에서는 함수 결과가 캐싱될 수 있습니다! 이것은 성능 면에서 매우 유용할 수 있지만, 일관성(consistency) 면에서는 매우 위험할 수도 있습니다. 모든 캐싱 메커니즘이 그렇듯이 말입니다. 게다가 이 캐싱은 매우 암묵적(implicit)입니다! 따라서 다음과 같은 경우에 캐싱이 됩니다:

```sql
-- my_function은 DETERMINISTIC이 아니지만 캐싱됩니다!
select t.x, t.y, (select my_function(t.x) from dual)
from t

-- 논리적으로 위와 동일하지만, 캐싱되지 않습니다
select t.x, t.y, my_function(t.x) from t
```

이것을 알게 된 이상, 꽤 많은 SQL을 다시 검토해야 할 것 같습니다...
