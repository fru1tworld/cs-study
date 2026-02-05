# MySQL 나쁜 아이디어 #666

> 원문: https://blog.jooq.org/mysql-bad-idea-666/

MySQL... 우리는 이전에 MySQL에 대해 블로그에 글을 쓴 적이 있습니다. 여러 번이요. MySQL에 구현된 나쁜 아이디어들을 여기서 보여드렸습니다:

- [MySQL 나쁜 아이디어 #384](https://blog.jooq.org/mysql-bad-idea-384/ "MySQL Bad Idea #384")
- [MySQL 나쁜 아이디어 #573](https://blog.jooq.org/mysql-bad-idea-573/ "MySQL Bad Idea #573")

하지만 이건 모든 것을 능가합니다. 이 [Stack Overflow 질문](https://stackoverflow.com/q/20796143/521799)을 확인해 보세요. 질문 내용은 이렇습니다: "왜 Oracle은 'group by 1,2,3'을 지원하지 않나요?". 처음에 저는 이 사용자가 SQL이 `ORDER BY` 절에서 (1부터 시작하는!) 인덱스로 컬럼을 참조하는 것을 허용하기 때문에 혼란스러워한 것이라고 생각했습니다:

```sql
SELECT first_name, last_name
FROM customers
ORDER BY 1, 2
```

위 쿼리는 `ORDER BY first_name, last_name`과 동일합니다. 인덱스 `1, 2`는 프로젝션(SELECT 절)의 컬럼들을 참조합니다. 이것은 복잡한 컬럼 표현식을 반복하지 않으려 할 때 가끔 유용할 수 있지만, `SELECT` 절에 컬럼을 추가할 때 정렬 의미가 바뀔 수 있어서 약간 위험할 수도 있습니다.

하지만 이 사용자는 `GROUP BY` 절에서 같은 문법을 사용하고 싶어했습니다. 그리고 이것이 실제로 MySQL에서 작동합니다! 다음 쿼리를 확인해 보세요:

```sql
SELECT a, b
FROM (
  SELECT 'a' a, 'b' b, 'c' c UNION ALL
  SELECT 'a'  , 'c'  , 'c'   UNION ALL
  SELECT 'a'  , 'b'  , 'd'
) t
GROUP BY 1, 2
ORDER BY 2
```

위 쿼리의 결과는...

| A | B |
|---|---|
| a | b |
| a | c |

하지만 이것이 도대체 무슨 의미일까요? [SQL 절에 대한 심층 설명](https://blog.jooq.org/10-easy-steps-to-a-complete-understanding-of-sql/)에 따르면, 프로젝션(`SELECT` 절)은 논리적으로 `GROUP BY` 절 _이후에_ 평가됩니다. 다시 말해서, `SELECT` 절에서 정의된 컬럼들은 아직 `GROUP BY` 절의 스코프에 들어있지 않습니다. 따라서 컬럼 인덱스의 유일하게 합리적인 의미는 테이블 소스 `t`의 인덱스여야 합니다. 하지만 그렇지 않습니다. 다음 대안 쿼리를 확인해 보세요:

```sql
SELECT a, b
FROM (
  SELECT 'a' a, 'b' b, 'c' c UNION ALL
  SELECT 'a'  , 'c'  , 'c'   UNION ALL
  SELECT 'a'  , 'b'  , 'd'
) t
GROUP BY 1, 2
ORDER BY 2
```

이제 결과는 다음과 같습니다:

| B | C |
|---|---|
| b | c |
| c | c |
| b | d |

그리고 이것은 [문서에 명시된 대로](https://dev.mysql.com/doc/refman/5.7/en/select.html) MySQL에서 (아마도?) 예상된 동작입니다:

> 출력을 위해 선택된 컬럼들은 컬럼 이름, 컬럼 별칭, 또는 컬럼 위치를 사용하여 ORDER BY 및 GROUP BY 절에서 참조될 수 있습니다. 컬럼 위치는 정수이며 1부터 시작합니다.

문서는 실제로 `GROUP BY`를 `ORDER BY`와 매우 유사한 방식으로 다루고 있습니다. 예를 들어, `GROUP BY`만 사용하여 정렬 방향을 지정하는 것이 가능합니다:

> MySQL은 GROUP BY 절을 확장하여 절에 명명된 컬럼 뒤에 ASC와 DESC를 지정할 수 있도록 합니다:
>
> `SELECT a, COUNT(b) FROM test_table GROUP BY a DESC;`

이 기능을 망가뜨리는 합리적인 엣지 케이스를 찾을 수는 없지만, 우리는 여전히 뭔가 수상한 점이 있다고 생각합니다. `SELECT` 절이 논리적으로 테이블 소스(`FROM, WHERE, GROUP BY, HAVING`) _이후에_ 평가된다는 사실을 고려할 때, `GROUP BY` 절이 그것을 참조할 수 있도록 허용하는 것은 SQL에 대한 이상한 이해로 이어지는 것 같습니다.

반면에, SQL은 공통 테이블 표현식(CTE)과 [`WINDOW` 절](https://blog.jooq.org/probably-the-coolest-sql-feature-window-functions/ "Probably the Coolest SQL Feature: Window Functions")을 제외하면 재사용 가능한 객체를 선언하는 것에 대한 지원이 거의 없는 매우 장황한 언어입니다. SQL 표준 담당자들이 훨씬 더 유용한 "공통 컬럼 표현식"을 도입하기 _전에_ 재사용 가능한 윈도우 프레임을 선언하기 위한 이 `WINDOW` 절을 지원할 것이라는 점은 사실 조금 놀랍습니다. 예를 들어:

```sql
-- 공통 컬럼/테이블 표현식:
WITH x AS CASE t1.a
          WHEN 1 THEN 'a'
          WHEN 2 THEN 'b'
                 ELSE 'c'
          END,
     y AS SOME_FUNCTION(t2.a, t2.b)
SELECT x, NVL(y, x)
FROM t1 JOIN t2 ON t1.id = t2.id
GROUP BY x, y
ORDER BY x DESC, y ASC
```

공통 컬럼 표현식을 사용하면, 컬럼 표현식의 재사용이 `SELECT` 절 자체와 독립적입니다. 다시 말해서, 컬럼 표현식을 실제로 `SELECT`하지 않고도 `JOIN` 절, `WHERE` 절, `GROUP BY` 절, `HAVING` 절 등에서 재사용할 수 있습니다.

그래서, MySQL에 공정하게 말하자면, 이 기능은 현재 형태로는 비(非)기능이지만, SQL의 장황함에 대한 우회책을 제공합니다.
