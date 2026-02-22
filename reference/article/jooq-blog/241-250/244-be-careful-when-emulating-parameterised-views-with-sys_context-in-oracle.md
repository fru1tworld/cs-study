> 원문: https://blog.jooq.org/be-careful-when-emulating-parameterised-views-with-sys_context-in-oracle/

# Oracle에서 SYS_CONTEXT로 매개변수화된 뷰를 에뮬레이트할 때 주의할 점

Oracle에서 매개변수화된 뷰(Parameterised Views)를 에뮬레이트하기 위해 `SYS_CONTEXT` 함수를 사용하는 방법과 그에 따른 성능 문제점을 살펴봅니다.

## SYS_CONTEXT란?

`SYS_CONTEXT`는 세션 수준의 전역 변수처럼 동작하여 개발자가 매개변수화된 뷰 동작을 에뮬레이트할 수 있게 해주는 함수입니다. 이것은 "Oracle의 저렴한 매개변수화된 뷰" 방식이라고 할 수 있습니다.

## 매개변수화된 뷰의 개념

만약 Oracle이 매개변수화된 뷰를 지원했다면 다음과 같은 문법을 사용할 수 있었을 것입니다 (가상의 문법):

```sql
CREATE VIEW v_categories_per_actor(
  p_actor_id NUMBER
) AS
SELECT DISTINCT c.name
FROM category c
JOIN film_category fc USING (category_id)
JOIN film_actor fa USING (film_id)
WHERE fa.actor_id = p_actor_id
```

## SYS_CONTEXT를 사용한 뷰 구현

실제로 Oracle에서는 `SYS_CONTEXT`를 사용하여 이를 에뮬레이트할 수 있습니다:

```sql
CREATE VIEW v_categories_per_actor AS
SELECT DISTINCT c.name
FROM category c
JOIN film_category fc USING (category_id)
JOIN film_actor fa USING (film_id)
WHERE fa.actor_id = TO_NUMBER(sys_context('MY_APP', 'ACTOR_ID'))
```

컨텍스트를 설정하는 방법은 다음과 같습니다:

```sql
CREATE CONTEXT my_app USING set_ctx;

CREATE OR REPLACE PROCEDURE set_ctx(
  p_actor_id NUMBER := NULL
) IS
BEGIN
  dbms_session.set_context('MY_APP', 'ACTOR_ID', p_actor_id);
END;
/

EXEC set_ctx(1);
```

그러면 뷰를 다음과 같이 쿼리할 수 있습니다:

```sql
SELECT *
FROM v_categories_per_actor
ORDER BY name;
```

## 성능 문제점

심각한 성능 문제는 `UNION ALL` 문을 뷰 내에서 `SYS_CONTEXT` 조건자와 함께 사용할 때 발생합니다. Oracle의 옵티마이저(Optimizer)는 바인드 변수 피킹(Bind Variable Peeking)과 달리 `SYS_CONTEXT` 값을 "들여다보지" 않기 때문에 심각하게 부정확한 카디널리티(Cardinality) 추정을 생성합니다.

### UNION ALL을 사용한 복잡한 뷰 예제

다음은 사용자 유형에 따라 다른 결과를 반환하는 뷰입니다:

```sql
CREATE OR REPLACE VIEW v_categories_per_actor AS

SELECT DISTINCT actor_id, c.name
FROM category c
JOIN film_category fc USING (category_id)
JOIN film_actor fa USING (film_id)
WHERE fa.actor_id = TO_NUMBER(sys_context('MY_APP', 'ACTOR_ID'))
AND sys_context('MY_APP', 'USER_TYPE') = 'C'

UNION ALL

SELECT DISTINCT actor_id, c.name
FROM category c
JOIN film_category fc USING (category_id)
JOIN film_actor fa USING (film_id)
WHERE sys_context('MY_APP', 'USER_TYPE') = 'O'
```

업데이트된 컨텍스트 프로시저:

```sql
CREATE OR REPLACE PROCEDURE set_ctx(
  p_actor_id NUMBER := NULL
) IS
BEGIN
  dbms_session.set_context('MY_APP', 'ACTOR_ID', p_actor_id);
  dbms_session.set_context('MY_APP', 'USER_TYPE',
    CASE WHEN p_actor_id IS NULL THEN 'O' ELSE 'C' END);
END;
/

EXEC set_ctx(1);
```

### 추정치가 잘못되는 이유

`SYS_CONTEXT` 필터가 전체 `UNION ALL` 분기를 제거하더라도, 옵티마이저는 여전히 두 분기가 모두 실행되는 것처럼 행 수를 추정합니다. 이는 중첩 쿼리를 통해 전파되어, 작은 데이터셋에 대해 해시 조인(Hash Join) 대신 네스티드 루프(Nested Loops)를 선택해야 할 때 점점 더 나쁜 조인 전략을 선택하게 만듭니다.

### 성능 벤치마크

다음과 같은 집계 쿼리를 테스트했습니다:

```sql
SELECT actor_id, name, COUNT(*)
FROM v_categories_per_actor ca
JOIN category c USING (name)
JOIN film_category fc USING (category_id)
JOIN film_actor fa USING (film_id, actor_id)
GROUP BY actor_id, name
ORDER BY actor_id, name;
```

성능 벤치마크 코드:

```sql
SET SERVEROUTPUT ON
DECLARE
  v_ts TIMESTAMP;
  v_repeat CONSTANT NUMBER := 1000;
BEGIN
  v_ts := SYSTIMESTAMP;

  FOR i IN 1..v_repeat LOOP
    FOR rec IN (
      SELECT actor_id, name, COUNT(*)
      FROM v_categories_per_actor ca -- No UNION ALL
      JOIN category c USING (name)
      JOIN film_category fc USING (category_id)
      JOIN film_actor fa USING (film_id, actor_id)
      GROUP BY actor_id, name
      ORDER BY actor_id, name
    ) LOOP
      NULL;
    END LOOP;
  END LOOP;

  dbms_output.put_line('Statement 1 : ' || (SYSTIMESTAMP - v_ts));
  v_ts := SYSTIMESTAMP;

  FOR i IN 1..v_repeat LOOP
    FOR rec IN (
      SELECT actor_id, name, COUNT(*)
      FROM v_categories_per_actor2 ca -- With UNION ALL
      JOIN category c USING (name)
      JOIN film_category fc USING (category_id)
      JOIN film_actor fa USING (film_id, actor_id)
      GROUP BY actor_id, name
      ORDER BY actor_id, name
    ) LOOP
      NULL;
    END LOOP;
  END LOOP;

  dbms_output.put_line('Statement 2 : ' || (SYSTIMESTAMP - v_ts));
END;
/
```

벤치마크 결과, `UNION ALL`이 있는 쿼리는 약 2.923초가 걸린 반면, 없는 쿼리는 1.94초가 걸렸습니다. 이는 잘못된 실행 계획 추정으로 인해 직접적으로 발생한 약 50%의 성능 저하입니다.

## 단일 모드 뷰 (고객 전용)

`UNION ALL` 없이 단일 조건만 사용하는 뷰는 다음과 같이 작성할 수 있습니다:

```sql
CREATE OR REPLACE VIEW v_categories_per_actor AS
SELECT DISTINCT actor_id, c.name
FROM category c
JOIN film_category fc USING (category_id)
JOIN film_actor fa USING (film_id)
WHERE fa.actor_id = TO_NUMBER(sys_context('MY_APP', 'ACTOR_ID'))
AND sys_context('MY_APP', 'USER_TYPE') = 'C'
```

## 권장 사항

`SYS_CONTEXT` 사용 시 주의가 필요합니다:

- 안전한 경우: 행 수준 보안(Row-Level Security)을 위한 단순한 WHERE 절 조건자
- 위험한 경우: `UNION ALL` 분기를 사용한 복잡한 조건부 로직

## 대안

Oracle 12c의 `LATERAL` 조인이나 SQL Server의 인라인 테이블 반환 함수(Inline Table-Valued Functions)와 같은 대안적인 접근 방식이 더 나은 최적화 가능성을 제공합니다.

## 결론

`SYS_CONTEXT`는 Oracle에서 매개변수화된 뷰를 에뮬레이트하는 유용한 기법이지만, 특히 `UNION ALL`과 함께 사용할 때 성능 문제를 일으킬 수 있습니다. 옵티마이저가 `SYS_CONTEXT` 값을 들여다보지 않기 때문에 카디널리티 추정이 부정확해지고, 이로 인해 최적이 아닌 실행 계획이 선택될 수 있습니다. 따라서 이 기법을 사용할 때는 실행 계획을 주의 깊게 검토하고, 필요한 경우 대안적인 접근 방식을 고려해야 합니다.
