> 원문: https://blog.jooq.org/do-you-really-have-to-name-everything-in-software/

# 소프트웨어에서 모든 것에 이름을 붙여야 할까?

## 개요

이 글은 소프트웨어 개발에서 명목적 타이핑(Nominal Typing)과 구조적 타이핑(Structural Typing) 사이의 긴장 관계를 살펴보며, 모든 것에 이름을 붙이는 것이 항상 더 좋다는 주장에 대해 반박합니다.

## 핵심 개념

구조적 타이핑(Structural Typing): 명시적인 이름이 아닌 구조에 의해 암묵적으로 정의되는 타입입니다. SQL에서 SELECT 문을 통해 생성되는 행 타입(Row Type)이 이를 잘 보여줍니다:

```sql
SELECT first_name, last_name FROM customer
```

이 쿼리는 `(first_name VARCHAR, last_name VARCHAR)` 구조를 가진 이름 없는 타입을 생성합니다.

명목적 타이핑(Nominal Typing): 파생 테이블(Derived Table), CTE, 또는 뷰(View)를 통해 구조적 타입에 명시적인 이름을 부여하는 방식입니다:

```sql
WITH people AS (
  SELECT first_name, last_name FROM customer
)
SELECT * FROM people
```

## 핵심 논점

저자는 사소한 조건문을 이름이 있는 함수로 추출해야 한다고 주장하는 글을 비판합니다. 원래 예제는 다음과 같습니다:

```java
if(barrier.value() > LIMIT && barrier.value() > 0)
```

이것을 다음과 같이 리팩토링하자고 제안되었습니다:

```java
if(barrierHasPositiveLimitBreach())
```

## 저자의 반론

저자는 모든 것에 이름을 붙이는 것의 여러 문제점을 지적합니다:

- 장황한 이름은 인지적 복잡성을 줄이기보다 오히려 증가시킵니다
- 제안된 함수 이름은 정의되지 않은 개념("breach", "positive limit")을 도입합니다
- 독자는 결국 함수 구현을 살펴봐야 합니다
- 전역 상태(Global State) 의존성이 숨겨지게 됩니다
- 간접 참조로 인한 잠재적인 성능 저하가 발생할 수 있습니다
- 로직이 변경되면 이름이 쓸모없게 될 수 있습니다

더 간단한 대안이 있습니다:

```sql
SELECT * FROM orders WHERE barrier > GREATEST(:limit, 0)
```

## 결론

"컴퓨터 과학에서 어려운 것은 딱 두 가지뿐이다: 캐시 무효화와 이름 짓기. 그리고 오프 바이 원 에러(Off-by-one Error)." 저자는 실용적인 적용을 옹호합니다: "정말 도움이 되는 곳에서는 이름을 붙이고, 도움이 되지 않는 곳에서는 이름을 붙이지 마라."

이 글은 교조적인 규칙보다 상황에 따른 의사결정을 강조합니다.
