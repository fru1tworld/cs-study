# 테이블 별칭을 써야 할까 말아야 할까?

> 원문: https://blog.jooq.org/should-i-put-that-table-alias-or-not/

SQL을 자주 사용하지 않는 개발자들은 파생 테이블(derived table)에 괄호를 언제 써야 하는지, 별칭(alias)을 언제 붙여야 하는지 종종 혼란스러워합니다. Reddit에서 Elmhurstlol이라는 사용자가 자신의 쿼리에서 파생 테이블(UNION이 포함된 서브셀렉트)에 왜 별칭을 제공해야 하는지 질문하는 토론이 있었습니다.

해당 사용자가 제공한 쿼리는 다음과 같습니다:

```sql
SELECT AVG(price) AS AVG_PRICE
FROM (
  SELECT price from product a JOIN pc b
  ON a.model=b.model AND maker='A'
  UNION ALL
  SELECT price from product a JOIN laptop c
  ON a.model=c.model and maker='A'
) hello
```

이 질문은 `"hello"` 테이블 별칭이 왜 필요한지에 초점을 맞추고 있었는데, 실제로는 필수가 아닌 것처럼 보이는 경우가 많기 때문입니다.

## SQL 표준에서 명시하는 내용

무료로 제공되는 SQL 1992 표준 텍스트를 참조하여 문법 요소를 설명하겠습니다. 관련 문법 정의는 다음과 같습니다:

```
<table reference> ::=
    <table name> [ [ AS ] <correlation name>
        [ <left paren> <derived column list>
          <right paren> ] ]
  | <derived table> [ AS ] <correlation name>
        [ <left paren> <derived column list>
          <right paren> ]
  | <joined table>

<derived table> ::= <table subquery>

<table subquery> ::= <subquery>

<subquery> ::= <left paren> <query expression>
               <right paren>
```

## 표준에서 도출되는 핵심 요점

문법 명세에서 세 가지 핵심 요점을 정리할 수 있습니다:

1. 파생 테이블은 반드시 항상 별칭을 가져야 합니다
2. `AS` 키워드는 가독성을 위해 선택적으로 사용할 수 있습니다
3. 괄호는 반드시 항상 서브쿼리를 감싸야 합니다

## 주의해야 할 예외 사항

일부 SQL 방언(dialect)에서는 별칭이 없는 파생 테이블을 허용하지만, 이는 권장되지 않습니다. 별칭이 없으면 해당 파생 테이블의 컬럼을 명확하게 한정(qualify)할 수 없게 되기 때문입니다.

## 핵심 정리

결론은 간단합니다: 항상 파생 테이블에 의미 있는 별칭을 제공하세요. 그게 전부입니다.

이 글은 다양한 데이터베이스 시스템 간의 최대 호환성과 코드 명확성을 위해 SQL 표준 관례를 준수하는 것을 강조합니다.
