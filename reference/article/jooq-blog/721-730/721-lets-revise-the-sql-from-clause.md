# SQL FROM 절을 다시 살펴보자

> 원문: https://blog.jooq.org/lets-revise-the-sql-from-clause/

SQL은 직관적인 언어입니다. 자연어처럼 작동하도록 설계되었기 때문에 대부분의 경우 잘 읽힙니다. 그래서 대부분의 SQL 개발자들은 직관에 의존하여 SQL을 작성하며, 공식적인 언어 명세를 자세히 살펴보지 않습니다. 하지만 때때로 공식 명세를 되돌아보면 놀라운 점을 발견할 수 있습니다.

다음은 간략화된 T-SQL 정의입니다:

```
<query_specification> ::=
SELECT [ ALL | DISTINCT ] < select_list >
    [ FROM { <table_source> } [ ,...n ] ]
    [ WHERE <search_condition> ]
    [ <GROUP BY> ]
    [ HAVING < search_condition > ]
```

여기서 table_source는 다음과 같이 정의됩니다:

```
<table_source> ::=
{
    table_or_view_name [ [ AS ] table_alias ]
    | user_defined_function [ [ AS ] table_alias ]
    | derived_table [ AS ] table_alias [ ( column_alias [ ,...n ] ) ]
    | <joined_table>
}
<joined_table> ::=
{
    <table_source> <join_type> <table_source> ON <search_condition>
    | <table_source> CROSS JOIN <table_source>
    | [ ( ] <joined_table> [ ) ]
}
```

여기서 핵심적인 관찰 사항은 다음과 같습니다: SELECT 문에는 명시적인 JOIN 절이 없습니다, 비록 우리가 그렇게 되는 것처럼 SQL을 작성하는 경향이 있더라도 말입니다. 직관적으로 다음 SQL 문장을 살펴보면:

```sql
SELECT *
FROM my_table
JOIN my_other_table ON id1 = id2
JOIN my_third_table ON id2 = id3
WHERE ...
```

위 문장은 공식적으로 다음과 같이 파싱됩니다:

```sql
SELECT *
FROM ((my_table JOIN my_other_table ON id1 = id2)
                JOIN my_third_table ON id2 = id3)
WHERE ...
```

즉, 가장 안쪽의 JOIN은 `<table_source>`를 생성하고, 이 `<table_source>`가 다시 외부 JOIN의 왼쪽 피연산자가 됩니다. 이것이 의미하는 바는 다음과 같은 것도 유효하다는 것입니다:

```sql
SELECT *
FROM t1 JOIN t2 ON id1 = id2,
     another_table,
     t3 JOIN t4 ON id3 = id4,
     yet_another_table,
     t5 JOIN t6 ON id5 = id6
WHERE ...
```

여기서는 5개의 테이블 소스의 전체 크로스 곱(cross product)에서 선택하고 있으며, 이 중 일부는 실제 테이블이고 일부는 조인된 테이블로부터 생성된 것입니다.

## jOOQ에서의 의미

이는 jOOQ에서도 가능해야 합니다. jOOQ 2.0.3에서는 향상된 Table API를 통해 이 구조를 지원할 것입니다:

```java
// 특정 유형의 테이블 소스를 생성
Table<Record> tableSource =
  MY_TABLE.join(MY_OTHER_TABLE).on(ID1.equal(ID2))
          .join(MY_THIRD_TABLE).on(ID2.equal(ID3));

// 위의 테이블 소스를 select 문에서 사용
create.select()
      .from(tableSource)
      .where(...);

// 여러 개의 "복합" 테이블 소스를 사용
create.select()
      .from(T1.join(T2).on(ID1.equal(ID2)),
            ANOTHER_TABLE,
            T3.join(T4).on(ID3.equal(ID4)),
            YET_ANOTHER_TABLE,
            T5.join(T6).on(ID5.equal(ID6)))
      .where(...);
```

물론, 이전의 편의 메서드들은 여전히 SELECT DSL API에서 사용할 수 있습니다. 하지만 이제 더 복잡한 테이블 소스를 생성하는 것이 가능합니다. 다음에 살펴볼 내용은 Oracle과 SQL Server의 PIVOT/UNPIVOT 연산이며, 이것도 또 다른 유형의 테이블 소스입니다.
