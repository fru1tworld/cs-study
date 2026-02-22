# JOIN을 설명할 때 벤 다이어그램에 NO라고 말하라

> 원문: https://blog.jooq.org/say-no-to-venn-diagrams-when-explaining-joins/

인터넷에서 SQL JOIN을 설명하는 튜토리얼을 검색해 보면, 아래와 유사한 벤 다이어그램으로 설명하는 많은 자료를 찾을 수 있을 것이다:

(벤 다이어그램 이미지: 두 원이 겹치며 INNER JOIN, LEFT JOIN, RIGHT JOIN, FULL OUTER JOIN을 보여줌)

우리 모두 이런 것들을 본 적이 있다. 솔직히, 나도 과거에 이런 다이어그램을 사용한 적이 있다. 직관적으로 보이고, 개념을 아주 간단하게 전달하는 것처럼 보인다. 하지만 이런 다이어그램들은 심각하게 잘못되었다. JOIN은 벤 다이어그램으로 설명할 수 있는 집합 연산이 아니기 때문이다. 벤 다이어그램은 집합 이론과 집합 연산을 설명하기 위한 것이다.

## 벤 다이어그램이 실제로 설명하기에 완벽한 것들

벤 다이어그램은 실제 집합 연산을 설명하기에 완벽하다:

- UNION (합집합)
- UNION ALL (중복을 포함한 합집합)
- INTERSECT (교집합)
- EXCEPT (차집합)

이러한 연산들은 동일한 유형의 튜플 집합에 대해 작동한다. 예를 들어, `AUTHOR` 집합과 `PERSON` 집합이 있다면, 이 둘의 UNION, INTERSECT, EXCEPT를 구할 수 있다. 왜냐하면 두 집합 모두 "사람"을 포함하고 있기 때문이다.

## JOIN은 다르다

반면에 JOIN은 전혀 다르게 작동한다. JOIN은 실제로 필터가 적용된 데카르트 곱(cartesian product, 직교 곱)이다.

### CROSS JOIN

모든 것은 CROSS JOIN에서 시작한다. CROSS JOIN은 왼쪽 테이블의 모든 행을 오른쪽 테이블의 모든 행과 결합한다. 만약 왼쪽에 3개의 행이 있고 오른쪽에 4개의 행이 있다면, 결과는 12개의 행이 된다 (3 × 4 = 12).

```
author 테이블 (3행)      book 테이블 (4행)
+----+--------+         +----+----------+-----------+
| id | name   |         | id | title    | author_id |
+----+--------+         +----+----------+-----------+
| 1  | Alice  |         | 1  | Book A   | 1         |
| 2  | Bob    |         | 2  | Book B   | 1         |
| 3  | Carol  |         | 3  | Book C   | 2         |
+----+--------+         | 4  | Book D   | 3         |
                        +----+----------+-----------+

CROSS JOIN 결과 (12행):
모든 author의 각 행이 모든 book의 각 행과 결합됨
```

이것이 JOIN의 본질이다. 모든 다른 JOIN 유형은 이 CROSS JOIN을 기반으로 구축된다.

### INNER JOIN

INNER JOIN은 단순히 조건자(predicate)로 필터링된 CROSS JOIN이다. 우리는 데카르트 곱을 만든 다음, 조건에 맞지 않는 행들을 제거한다.

```sql
SELECT *
FROM author a
JOIN book b ON a.author_id = b.author_id
```

이 쿼리는 먼저 개념적으로 모든 author와 book의 조합을 만든 다음 (12행), `a.author_id = b.author_id` 조건에 맞는 행만 남긴다.

### OUTER JOIN

OUTER JOIN은 조금 더 복잡하지만, 여전히 데카르트 곱을 기반으로 한다. OUTER JOIN은 INNER JOIN의 결과에 더해서, 매칭되지 않은 행들을 NULL로 패딩하여 추가한다.

- LEFT OUTER JOIN: INNER JOIN 결과 + 매칭되지 않은 왼쪽 테이블의 행들 (오른쪽은 NULL)
- RIGHT OUTER JOIN: INNER JOIN 결과 + 매칭되지 않은 오른쪽 테이블의 행들 (왼쪽은 NULL)
- FULL OUTER JOIN: INNER JOIN 결과 + 양쪽 모두의 매칭되지 않은 행들

LEFT OUTER JOIN을 집합 연산으로 표현하면 다음과 같다:

```sql
-- LEFT OUTER JOIN을 집합 연산으로 표현
SELECT *
FROM author a
JOIN book b USING (author_id)
UNION
SELECT a.*, NULL, NULL, NULL
FROM (
  SELECT a.*
  FROM author a
  EXCEPT
  SELECT a.*
  FROM author a
  JOIN book b USING (author_id)
) a
```

이것은 벤 다이어그램이 보여주려는 것보다 훨씬 더 복잡한 연산이다!

## 벤 다이어그램의 문제점

### 1. JOIN의 본질을 가린다

벤 다이어그램을 사용하면 학습자들이 JOIN의 진정한 본질, 즉 데카르트 곱을 이해하지 못하게 된다. 이것은 실제 문제를 야기한다.

### 2. 실제 실수를 유발한다

내가 SQL 교육을 진행할 때, 참가자들에게 테이블에서 중복된 이름을 찾기 위해 self-join을 작성하라는 과제를 준다. 놀랍게도 약 40%의 참가자들이 실수로 데카르트 곱을 만든다:

```sql
-- 잘못된 쿼리 (데카르트 곱 발생!)
SELECT *
FROM person p1
JOIN person p2 ON p1.first_name = p2.first_name
              AND p1.last_name = p2.last_name

-- 올바른 쿼리 (자기 자신과의 매칭을 제외)
SELECT *
FROM person p1
JOIN person p2 ON p1.first_name = p2.first_name
              AND p1.last_name = p2.last_name
              AND p1.id < p2.id
```

벤 다이어그램으로 JOIN을 이해한 사람들은 이런 실수를 더 자주 저지른다. 왜냐하면 그들은 데카르트 곱이 뒤에서 일어나고 있다는 것을 인식하지 못하기 때문이다.

### 3. 다중 JOIN에서 완전히 실패한다

세 개 이상의 테이블을 JOIN할 때 벤 다이어그램은 어떻게 될까? 표현하기가 매우 어려워지고, 실제로 무슨 일이 일어나는지 전혀 설명하지 못한다.

### 4. 접근성 문제

많은 벤 다이어그램이 색상에 의존하여 JOIN 유형을 구분한다. 이것은 색맹인 사람들에게 접근성 문제를 야기한다.

## 더 나은 대안: JOIN 다이어그램

벤 다이어그램 대신, 행과 매칭 기준을 보여주는 다이어그램을 사용하는 것이 훨씬 낫다. 이러한 다이어그램은:

1. 테이블을 행의 집합으로 보여준다
2. 데카르트 곱이 어떻게 형성되는지 보여준다
3. 조건자가 어떻게 결과를 필터링하는지 보여준다
4. OUTER JOIN에서 NULL이 어디에 나타나는지 보여준다

이런 시각화는 JOIN의 진정한 메커니즘을 이해하는 데 훨씬 더 도움이 된다.

## 결론

벤 다이어그램은 시각적으로 매력적이고 직관적으로 보이지만, JOIN을 설명하는 데는 근본적으로 잘못된 도구다. JOIN은 집합 연산이 아니라 데카르트 곱에 필터를 적용한 것이다.

다음에 누군가에게 JOIN을 설명해야 할 때, 벤 다이어그램에 "NO"라고 말하고, 대신 JOIN의 진정한 본질인 데카르트 곱과 필터링을 설명하라. 이것이 학습자들이 SQL을 더 깊이 이해하고, 실수를 줄이는 데 도움이 될 것이다.

---

*참고: 벤 다이어그램은 UNION, INTERSECT, EXCEPT와 같은 진정한 집합 연산을 설명할 때는 여전히 유용하다. 단지 JOIN에 사용하지 말라는 것이다.*
