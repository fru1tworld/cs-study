# 놀라운 SQL 기능: 정량화된 비교 술어 (ANY, ALL)

> 원문: https://blog.jooq.org/a-wonderful-sql-feature-quantified-comparison-predicates-any-all/

SQL의 `ANY`(또는 `SOME`)와 `ALL` 키워드의 사용 사례에 대해 궁금해 본 적이 있으신가요? 아마 실무에서 이 키워드들을 접해본 적이 없을 것입니다. 하지만 이것들은 매우 유용할 수 있습니다. 먼저 SQL 표준에서 어떻게 정의되어 있는지 살펴보겠습니다. 쉬운 부분부터:

8.7 <정량화된 비교 술어>

기능

정량화된 비교를 지정합니다.

형식

```
<quantified comparison predicate> ::=
    <row value constructor> <comp op>
        <quantifier> <table subquery>

<quantifier> ::= <all> | <some>
<all> ::= ALL
<some> ::= SOME | ANY
```

직관적으로, 이러한 정량화된 비교 술어는 다음과 같이 사용할 수 있습니다:

```sql
-- 나이가 42인 사람이 있는가?
42 = ANY (SELECT age FROM person)

-- 모든 사람이 42세보다 어린가?
42 > ALL (SELECT age FROM person)
```

유용한 것들만 살펴보겠습니다. 여러분은 아마 위의 쿼리들을 다른 문법으로 작성해 왔을 것입니다:

```sql
-- 나이가 42인 사람이 있는가?
42 IN (SELECT age FROM person)

-- 모든 사람이 42세보다 어린가?
42 > (SELECT MAX(age) FROM person)
```

실제로 여러분은 `<in 술어>`를 사용하거나, 집계 함수가 포함된 `<스칼라 서브쿼리>`와 함께 보다 큼 술어를 사용해 왔습니다.

## IN 술어

위의 `ANY`를 사용한 `<정량화된 비교 술어>`처럼 `<in 술어>`를 사용해 왔다면 그것은 우연이 아닙니다. 실제로 `<in 술어>`는 정확히 그렇게 정의되어 있습니다:

8.4 <in 술어>

구문 규칙

2) RVC를 `<row value constructor>`라 하고, IPV를 `<in predicate value>`라 하자.

3) 표현식

```
RVC NOT IN IPV
```

은 다음과 동등합니다

```
NOT ( RVC IN IPV )
```

4) 표현식

```
RVC IN IPV
```

은 다음과 동등합니다

```
RVC = ANY IPV
```

정확합니다! SQL이 아름답지 않나요? 참고로, 3)의 암묵적인 결과는 `NULL`에 대해 `NOT IN` 술어가 매우 특이한 동작을 하게 되는데, 이를 알고 있는 개발자는 거의 없습니다.

## 이제 정말 멋진 부분입니다

지금까지는 이 `<정량화된 비교 술어>`에 특별한 것이 없었습니다. 이전의 모든 예제들은 "더 관용적인", 또는 "더 일상적인" SQL로 에뮬레이션할 수 있습니다. 하지만 `<정량화된 비교 술어>`의 진정한 멋짐은 차수/항수가 1보다 큰 행을 가진 `<행 값 표현식>`과 함께 사용될 때 나타납니다:

```sql
-- "John"이라는 이름의 42세인 사람이 있는가?
(42, 'John') = ANY (SELECT age, first_name FROM person)

-- 모든 사람이 55세보다 어린가?
-- 또는 55세라면, 모두 150,000.00 미만을 버는가?
(55, 150000.00) > ALL (SELECT age, wage FROM person)
```

위 쿼리들이 PostgreSQL에서 작동하는 것을 이 SQLFiddle에서 확인해 보세요. 이 시점에서, 실제로 다음을 지원하는 데이터베이스가 거의 없다는 점을 언급할 가치가 있습니다...

- 행 값 표현식, 또는...
- 행 값 표현식을 사용한 정량화된 비교 술어

SQL-92에서 지정되었음에도 불구하고, 대부분의 데이터베이스가 22년이 지난 지금까지도 이 기능을 구현하는 데 시간이 걸리는 것 같습니다.

## jOOQ로 이 술어들 에뮬레이션하기

하지만 다행히도 jOOQ가 이러한 기능들을 에뮬레이션해 줍니다. 프로젝트에서 jOOQ를 사용하지 않더라도, 위의 술어들을 표현하고 싶다면 다음의 SQL 변환 단계가 유용할 수 있습니다. MySQL에서 어떻게 할 수 있는지 살펴보겠습니다:

```sql
-- 이 술어는
(42, 'John') = ANY (SELECT age, first_name FROM person)

-- 다음과 동일합니다:
EXISTS (
  SELECT 1 FROM person
  WHERE age = 42 AND first_name = 'John'
)
```

다른 술어는 어떨까요?

```sql
-- 이 술어는
(55, 150000.00) > ALL (SELECT age, wage FROM person)

-- 다음과 동일합니다:
-- 행 값 표현식을 사용한 정량화된 비교 술어가
-- 사용 불가능한 경우
(55, 150000.00) > (
  SELECT age, wage FROM person
  ORDER BY 1 DESC, 2 DESC
  LIMIT 1
)

-- 행 값 표현식이 전혀 사용 불가능한 경우
NOT EXISTS (
  SELECT 1 FROM person
  WHERE (55 < age)
  OR    (55 = age AND 150000.00 <= wage)
)
```

분명히, `EXISTS` 술어는 거의 모든 데이터베이스에서 이전에 본 것을 에뮬레이션하는 데 사용할 수 있습니다. 일회성 에뮬레이션만 필요하다면 위의 예제로 충분할 것입니다. 그러나 `<행 값 표현식>`과 `<정량화된 비교 술어>`를 더 형식적으로 사용하고 싶다면, SQL 변환을 제대로 하는 것이 좋습니다. 이 글에서 SQL 변환에 대해 더 읽어보세요.
