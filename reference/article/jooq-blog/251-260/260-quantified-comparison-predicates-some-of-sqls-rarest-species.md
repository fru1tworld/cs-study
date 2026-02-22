# 정량화된 비교 술어 - SQL의 가장 희귀한 종 중 일부

> 원문: https://blog.jooq.org/quantified-comparison-predicates-some-of-sqls-rarest-species/

---

## 소개

Aaron Bertrand(Stack Overflow에서 분명히 만나보셨을 분)의 최근 트윗이 제 관심을 끌었습니다:

> "아니면 ANY / ALL 구문에 대해 많은 질문을 하는 사람? Celko가 아직 대학에 있을 때 이후로 아무도 사용하지 않는 구문 말이야?"
>
> — Aaron Bertrand (@AaronBertrand) 2016년 7월 11일

실제로 제 SQL 마스터클래스를 방문했던 사람들 중 SQL에서 사용할 수 있는 `ANY`와 `ALL` 정량자에 대해 들어본 사람은 거의 없었고, 정기적으로 사용하는 사람은 더더욱 없었습니다(아예 한 번도 사용해본 적 없는 경우도 많았습니다).

---

## 이것들은 무엇인가?

형식적 정의는 상당히 간단합니다. 다음과 같이 정의합니다:

R <비교 연산자> <정량자> S

…이것이 정량화된 비교 술어이며, 여기서

- `R`은 행(row)입니다
- `<비교 연산자>`는 비교 연산자입니다 (=, != 등)
- `<정량자>`는 `ANY` 또는 `ALL`입니다
- `S`는 서브쿼리입니다

이제 다음과 같이 작성할 수 있습니다:

```sql
SELECT *
FROM author AS a
WHERE a.author_id = ANY (
  SELECT b.author_id
  FROM book AS b
)
```

위 쿼리는 상당히 직관적으로 읽힙니다("직관적"에 대한 여러분의 이해에 따라 다르겠지만):

> `book`에 포함된 `author_id` 값 중 "어느 것이든(ANY)" `author_id`가 같은 모든 `authors`를 원합니다

사실, SQL 표준은 `IN` 술어를 `= ANY()` 정량화된 비교 술어의 문법적 설탕(syntax sugar)으로 정의하고 있습니다.

8.4 <in predicate>

RVC를 <row value predicand>로, IPV를 <in predicate value>로 합니다.

다음 표현식은

```
RVC IN IPV
```

다음과 동등합니다

```
RVC = ANY IPV
```

---

## 왜 이 이상한 연산자들을 사용할까요?

이 연산자들의 흥미로운 사용 사례는 비교 연산자가 동등("=") 또는 비동등("!=" 또는 "<>")인 경우가 아닙니다. 흥미로운 경우는 다음과 같이 미만(less-than) 또는 초과(greater-than) 연산자로 비교할 때입니다:

```sql
SELECT *
FROM author AS a
WHERE a.age > ALL (
  SELECT c.age
  FROM customer AS c
)
```

또는 평범한 한국어로:

> 모든 `customers`의 `age`보다 `age`가 큰 모든 `authors`를 원합니다

멋지지 않나요? 물론, 같은 쿼리를 처리하는 (최소한) 두 가지 대안적인 방법이 있습니다:

### 대안 1: MAX()와만 비교하기

```sql
SELECT *
FROM author AS a
WHERE a.age > (
  SELECT MAX(c.age)
  FROM customer AS c
)
```

이 솔루션은 저자의 나이를 최대 고객 나이와만 비교하며, 이는 명백히 같은 결과를 산출합니다. 가장 나이 많은 고객보다 더 나이 많은 저자를 찾으면, 그 저자는 _모든_ 고객보다 나이가 많은 것입니다.

### 대안 2: "첫 번째" 값과만 비교하기

```sql
SELECT *
FROM author AS a
WHERE a.age > (
  SELECT c.age
  FROM customer AS c
  ORDER BY c.age DESC
  LIMIT 1
)
```

저는 PostgreSQL 구문(LIMIT 1)을 사용하고 있지만, 대부분의 데이터베이스에는 쿼리에서 하나의 행만 반환하는 방법이 있습니다. 이것은 `MAX()`를 사용하는 것과 같습니다. 일부 데이터베이스는 단일 행만 반환한다는 사실을 인식하고, 먼저 전체 결과 집합을 `O(N log N)`으로 정렬하지 않고 `O(N)`으로 최대값만 산출하며, 저자당 한 번이 아니라 모든 저자에 대해 한 번만 수행합니다. 이 최적화가 여러분의 데이터베이스에서 수행되는지는 말씀드릴 수 없습니다. 직접 측정하고 실행 계획을 확인해야 합니다.

---

## 그래서 다시, 왜 정량자를 사용할까요?

트위터 대화의 Aaron은 이 "비직관적인" 구문 사용을 권장하지 않습니다:

> "솔직히 저는 그런 것을 더 직관적인 방식으로 작성할 것 같습니다."
>
> — Aaron Bertrand (@AaronBertrand) 2016년 7월 11일

아마도요. 궁극적으로 항상 성능을 먼저 선택해야 하고, 그 다음에는 - 확실히 - 직관성을 두 번째로 선택해야 합니다(어떤 불쌍한 영혼이 여러분의 쿼리를 유지보수해야 할 수도 있으니까요). 하지만 개인적으로 저는 세 가지 이유로 이 정량자들이 꽤 우아하다고 생각합니다:

1. 정량화를 적절한 위치에서 표현합니다. 비교 연산자와 함께요. 이것을 LIMIT를 사용하는 솔루션과 비교해보세요. LIMIT는 시각적으로 greater-than 연산자에서 멀리 떨어져 있을 수 있습니다. 정량자는 MAX()를 사용할 때보다 훨씬 더 간결합니다(제 의견으로는).

2. 매우 집합 지향적입니다. 저는 SQL로 작업할 때 집합 관점에서 생각하는 것을 좋아합니다. `ORDER BY` 절을 생략할 수 있을 때마다 그렇게 합니다. 잠재적으로 느린 연산을 피하기 위해서라도요(데이터베이스가 최적화하지 않아 전체 `O(N log N)` 정렬 연산이 호출되는 경우).

3. 정량화된 비교 술어는 단일 값뿐만 아니라 행에서도 작동합니다.

이것을 확인해보세요:

```sql
SELECT (c1.last_name, c1.first_name) >= ALL (
  SELECT c2.last_name, c2.first_name
  FROM customer AS c2
)
FROM customer AS c1
WHERE c1.id = 1
```

위 쿼리는 "TRUE" 또는 "FALSE" 값을 포함하는 단일 열을 산출합니다(예: PostgreSQL에서, 이 정확한 구문을 지원합니다). 아이디어는 `id = 1`인 `customer`에 대해 쿼리를 실행하고 그 고객의 `(last_name, first_name)` 튜플이 다른 모든 고객들보다 _"뒤에"_ 있는지 확인하는 것입니다. 또는 평범한 한국어로:

> 나는 전화번호부의 끝에 있는가?

다시 말하지만, LIMIT로 이것을 할 수 있습니다:

```sql
SELECT (c1.last_name, c1.first_name) >= (
  SELECT c2.last_name, c2.first_name
  FROM customer AS c2
  ORDER BY c2.last_name DESC, c2.first_name DESC
  LIMIT 1
)
FROM customer AS c1
WHERE c1.id = 1
```

…하지만 저는 이것이 훨씬 덜 우아하다고 생각합니다. 안타깝게도, 이번에는 `MAX()`로 이 문제를 해결할 방법이 없습니다. (제가 아는 한) 어떤 데이터베이스도 `MAX()`와 같은 집계 함수에 행 값 표현식(튜플)을 사용하는 것을 지원하지 않습니다. 이런 것이 가능하면 좋을 것입니다:

```sql
SELECT (c1.last_name, c1.first_name) >= (
  SELECT MAX((c2.last_name, c2.first_name))
  FROM customer AS c2
)
FROM customer AS c1
WHERE c1.id = 1
```

또는 이것:

```sql
SELECT (c1.last_name, c1.first_name) >= (
  SELECT MAX(ROW(c2.last_name, c2.first_name))
  FROM customer AS c2
)
FROM customer AS c1
WHERE c1.id = 1
```

물론, 위의 것은 구성된 예시일 뿐입니다. 네, 대신 윈도우 함수를 사용할 수도 있습니다(예: LEAD()).

---

## 결론

정량화된 비교 술어는 실제로 매우 드물게 볼 수 있습니다. 한 가지 이유는 이에 대해 아는 사람이 적거나, 문제를 해결할 때 이것을 떠올리지 못하기 때문입니다. 또 다른 이유는 일부 데이터베이스에서 잘 최적화되지 않아서 실제로 사용하는 것이 좋지 않은 아이디어일 수 있기 때문입니다. 그럼에도 불구하고, 저는 이것들이 알아두면 매우 흥미로운 SQL 지식이라고 생각합니다. 그리고 누가 알겠습니까, 아마도 언젠가 정량화된 비교 술어로 가장 잘 해결되는 문제에 부딪힐 수도 있습니다. 그때 여러분은 가장 최적의 솔루션으로 빛날 것입니다.

### 추가 읽기

- A Wonderful SQL Feature: Quantified Comparison Predicates (ANY, ALL)
- The Awesome PostgreSQL 9.4 / SQL:2003 FILTER Clause for Aggregate Functions
- Don't Miss out on Awesome SQL Power with FIRST_VALUE(), LAST_VALUE(), LEAD(), and LAG()
- 10 SQL Tricks That You Didn't Think Were Possible
- 10 Easy Steps to a Complete Understanding of SQL
