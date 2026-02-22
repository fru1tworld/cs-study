# Oracle 12c 선물: { CROSS | OUTER } APPLY

> 원문: https://blog.jooq.org/oracle-12c-goodies-cross-outer-apply/

Oracle이 이 기능을 공개적으로 알렸던 적이 있나요? 저는 이 주제에 대한 블로그 글을 많이 본 적이 없습니다.

Oracle 12c에서 SQL 표준 `OFFSET .. FETCH` 절(SQL Server 2012에서도 사용 가능했던)을 추가한 것 외에도, Oracle은 이제 `{ CROSS | OUTER } APPLY` 구문도 지원합니다!

공식 문서에서 자세한 내용을 확인할 수 있습니다:
- [Oracle 12c APPLY 절 문서](http://docs.oracle.com/cd/E16655_01/server.121/e17209/statements_10002.htm#CHDIJFDJ)

이제 jOOQ에서도 이 기능들을 지원할 때가 되었습니다. 가능한 경우 에뮬레이션도 함께 지원할 예정입니다:
- GitHub 이슈 #846: CROSS APPLY 지원
- GitHub 이슈 #845: OUTER APPLY 지원

## CROSS APPLY와 OUTER APPLY란?

`APPLY` 연산자는 원래 SQL Server에서 도입된 기능으로, 테이블 값 함수(table-valued function)나 서브쿼리를 외부 쿼리의 각 행에 대해 실행할 수 있게 해줍니다.

- CROSS APPLY: `INNER JOIN`과 유사하게 동작합니다. 오른쪽 테이블 표현식이 결과를 반환하는 행만 결과에 포함됩니다.
- OUTER APPLY: `LEFT OUTER JOIN`과 유사하게 동작합니다. 오른쪽 테이블 표현식이 결과를 반환하지 않더라도 왼쪽 테이블의 모든 행이 결과에 포함됩니다.

### 사용 예시

```sql
-- CROSS APPLY 예시
SELECT d.department_name, e.employee_name
FROM departments d
CROSS APPLY (
    SELECT employee_name
    FROM employees
    WHERE employees.department_id = d.department_id
    AND rownum <= 3
) e;

-- OUTER APPLY 예시
SELECT d.department_name, e.employee_name
FROM departments d
OUTER APPLY (
    SELECT employee_name
    FROM employees
    WHERE employees.department_id = d.department_id
    AND rownum <= 3
) e;
```

이 기능의 핵심은 서브쿼리 내에서 외부 쿼리의 컬럼을 참조할 수 있다는 것입니다(lateral correlation). 이는 일반적인 `JOIN`에서는 불가능한 기능입니다.

## SQL 표준과의 관계

SQL 표준에서는 이와 동등한 기능을 `LATERAL` 키워드로 제공합니다:

```sql
-- SQL 표준 LATERAL 구문
SELECT d.department_name, e.employee_name
FROM departments d
LEFT JOIN LATERAL (
    SELECT employee_name
    FROM employees
    WHERE employees.department_id = d.department_id
    FETCH FIRST 3 ROWS ONLY
) e ON 1=1;
```

Oracle 12c는 `LATERAL` 구문도 지원하므로, 이제 개발자들은 선호하는 구문을 선택할 수 있습니다.

---

태그: APPLY, CROSS APPLY, Oracle, Oracle 12c, OUTER APPLY, SQL, T-SQL
