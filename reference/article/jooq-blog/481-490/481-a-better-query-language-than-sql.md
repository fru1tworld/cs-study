# SQL보다 더 나은 쿼리 언어

> 원문: https://blog.jooq.org/a-better-query-language-than-sql/

게시일: 2014년 3월 19일, 작성자: lukaseder

Tech.Pro의 창립자인 Leland Richardson이 최근 BQL에 대한 매우 흥미로운 글을 발표했습니다. BQL은 그가 생각하는 (SQL보다) 더 나은 쿼리 언어에 대한 비전입니다. 그의 새로운 언어 제안에서 결정적인 특징은 이것이 실제로 SQL 자체의 상위 집합(superset)이라는 사실입니다. SQL은 관계형 데이터베이스를 쿼리하기 위한 매우 풍부하고 표현력 있는 언어입니다. 하지만 여러 측면에서 어색하며, 많은 사람들이 SQL이 느리게만 발전하고 있다고 인식합니다 - SQL 표준의 발전 속도를 고려하면 사실이 아님에도 불구하고 말입니다. 그러나 표준은 하나의 문제이고, 구현은 또 다른 문제입니다 - 특히 엔터프라이즈 환경에서는 더욱 그렇습니다. SQL에 대해 블로그를 작성할 때, 우리 자신도 PostgreSQL 방언이 얼마나 훌륭한지에 대해 끊임없이 놀라곤 합니다. 하지만 종종 PostgreSQL은 실제로 표준을 구현하고 있을 뿐입니다. 그래서 우리가 어딘가에 도달하고 있다는 희망이 있습니다. 그럼에도 불구하고, Leland의 글에는 주목할 만한 몇 가지 아이디어가 있습니다. 우리의 관점에서, 주요 아이디어는 다음과 같습니다:

## SELECT 절과 테이블 표현식의 순서 유연성

SQL에서 `SELECT`는 항상 첫 번째 키워드입니다. 테이블 표현식보다 먼저 작성해야 합니다. 이전 글에서 이것이 많은 SQL 사용자들에게 상당히 혼란스럽다는 것을 보여드린 바 있습니다. 기존 문법은 계속 유지되어야 하지만, `SELECT` 절과 테이블 표현식의 순서를 뒤집을 수 있다면 좋을 것입니다.

```sql
FROM table
WHERE predicate
GROUP BY columns
SELECT columns
```

기억하세요, 테이블 표현식에는 `FROM`, `WHERE`, `GROUP BY` 절뿐만 아니라 벤더별 `CONNECT BY` 절 등이 포함됩니다:

```
<query specification> ::=
  SELECT [ <set quantifier> ]
    <select list> <table expression>
```

그런데 이 언어 기능은 이미 LINQ에서 사용 가능합니다.

## 암시적 KEY JOIN

이 기능은 jOOQ에서도 `ON KEY` 절을 사용하여 사용할 수 있습니다. Sybase도 `ON KEY` 조인을 지원한다는 점에 주목하세요:

```sql
from post
key join user
key join comment
select *
```

## 이름이 있는 프로젝션(Named Projections)

이것은 SQL 언어에 정말 있었으면 하는 기능 중 하나입니다. 그러나 전용 문법으로 프로젝션을 지정하는 것에 기대를 걸지는 않을 것입니다. 우리는 오히려 테이블 표현식 문법의 확장을 사용하여 테이블이 다음과 같은 "부가 테이블(side-tables)"을 생성할 수 있도록 하고 싶습니다:

```sql
from dbo.users
with projection as (
  firstName, lastName, phoneNumber, email
)
select projection.*
```

위의 예제에서 `projection`은 실제로 `users` 테이블에서 파생된 또 다른 테이블 표현식에 불과합니다. SQL 문법 의미론의 관점에서, 이것은 매우 강력할 것입니다. 왜냐하면 이러한 프로젝션은 일반 테이블의 모든 문법적 기능을 상속받기 때문입니다. 우리는 이 기능을 "공통 컬럼 표현식(common column expressions)"이라고 불렀을 때 이에 대해 블로그에 글을 쓴 적이 있습니다.

## 결론

Leland에게는 다른 많은 아이디어들이 있습니다. 그는 아직 많은 개선이 필요한 프로젝트의 초기 단계에 있습니다. 그러나 그가 reddit에서 받은 피드백은 상당히 좋습니다. 분명히 SQL을 위한 "BQL"을 만드는 데 많은 잠재력이 있습니다. 다음과 같은 관계처럼 말입니다:

- less가 CSS를 위한 것처럼
- Groovy가 Java를 위한 것처럼
- Xtend가 Java를 위한 것처럼
- jQuery가 JavaScript를 위한 것처럼

이 시도가 어디로 이어질지 지켜봅시다. 우리는 확실히 BQL의 다음 단계를 계속 주시할 것입니다.
