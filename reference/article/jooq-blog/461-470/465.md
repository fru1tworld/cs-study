# SQL에서 정렬 간접 참조를 구현하는 방법

> 원문: https://blog.jooq.org/how-to-implement-sort-indirection-in-sql/

## 문제

최근 Stack Overflow에서 흥미로운 질문을 발견했습니다. 사용자가 다음과 같은 쿼리를 작성했습니다:

```sql
SELECT name
FROM product
WHERE name IN ('CE367FAACDHCANPH-151556',
               'CE367FAACEX9ANPH-153877',
               'NI564FAACJSFANPH-162605',
               'GE526OTACCD3ANPH-149839')
```

데이터베이스는 결과를 알파벳순으로 반환했지만, 사용자가 원했던 것은 `IN` 절에 지정된 순서대로 결과를 받는 것이었습니다. 본질적으로 사용자는 결과 레코드가 잘 정의된 순서로 전달되기를 원했던 것입니다.

## 해결책: 정렬 간접 참조(Sort Indirection)

정렬 간접 참조는 값을 숫자 위치에 매핑하여 자연 정렬 규칙과 무관한 사용자 정의 순서를 만드는 기법입니다.

### 방법 1: CASE 표현식

가장 일반적인 해결책은 `ORDER BY` 절에서 `CASE` 문을 사용하여 열거형 매핑을 만드는 것입니다:

```sql
SELECT name
FROM product
WHERE name IN ('CE367FAACDHCANPH-151556',
               'CE367FAACEX9ANPH-153877',
               'NI564FAACJSFANPH-162605',
               'GE526OTACCD3ANPH-149839')
ORDER BY
  CASE WHEN name = 'CE367FAACDHCANPH-151556' THEN 1
       WHEN name = 'CE367FAACEX9ANPH-153877' THEN 2
       WHEN name = 'NI564FAACJSFANPH-162605' THEN 3
       WHEN name = 'GE526OTACCD3ANPH-149839' THEN 4
  END
```

이 기법은 모든 SQL 방언에서 작동합니다. `CASE` 표현식은 각 이름 값을 정렬 순서를 나타내는 숫자에 매핑합니다.

### 방법 2: VALUES 절을 사용한 INNER JOIN

대안적인 방법은 이름-정렬 순서 매핑을 포함하는 파생 테이블(derived table)을 사용하는 것입니다:

```sql
SELECT product.name
FROM product
JOIN (
  VALUES('CE367FAACDHCANPH-151556', 1),
        ('CE367FAACEX9ANPH-153877', 2),
        ('NI564FAACJSFANPH-162605', 3),
        ('GE526OTACCD3ANPH-149839', 4)
) AS sort (name, sort)
ON product.name = sort.name
ORDER BY sort.sort
```

이 접근 방식은 PostgreSQL 문법을 사용하며, `WHERE` 절과 `ORDER BY` 절 양쪽에서 값을 반복할 필요 없이 사용자 정의 순서를 적용할 수 있습니다. `JOIN`이 암묵적으로 필터링 역할도 수행합니다.

## jOOQ 구현

jOOQ는 정렬 간접 참조를 위한 전용 API를 제공하여 `CASE` 표현식을 수동으로 작성할 필요를 없애줍니다:

```java
DSL.using(configuration)
   .select(PRODUCT.NAME)
   .from(PRODUCT)
   .where(NAME.in(
      "CE367FAACDHCANPH-151556",
      "CE367FAACEX9ANPH-153877",
      "NI564FAACJSFANPH-162605",
      "GE526OTACCD3ANPH-149839"
   ))
   .orderBy(PRODUCT.NAME.sortAsc(
      "CE367FAACDHCANPH-151556",
      "CE367FAACEX9ANPH-153877",
      "NI564FAACJSFANPH-162605",
      "GE526OTACCD3ANPH-149839"
   ))
   .fetch();
```

`sortAsc()` 메서드는 제공된 값들의 순서에 따라 적절한 `CASE` 표현식을 자동으로 생성합니다.

또는 사용자 정의 간접 참조 값을 사용할 수도 있습니다:

```java
.orderBy(PRODUCT.NAME.sort(
   new HashMap<String, Integer>() {{
     put("CE367FAACDHCANPH-151556", 2);
     put("CE367FAACEX9ANPH-153877", 3);
     put("NI564FAACJSFANPH-162605", 5);
     put("GE526OTACCD3ANPH-149839", 8);
   }}
))
```

`sort()` 메서드를 사용하면 연속적인 정수가 아닌 임의의 정렬 값을 지정할 수 있어 더 큰 유연성을 제공합니다.

## 핵심 요점

정렬 간접 참조는 때때로 유용하게 사용할 수 있는 좋은 기법입니다. SQL 문의 `ORDER BY` 절에는 거의 모든 임의의 컬럼 표현식을 넣을 수 있다는 것을 잊지 마세요.

이 기법은 비즈니스 로직이 비표준 정렬을 요구하고, 이를 애플리케이션 코드에서 처리하는 것이 비실용적일 때 특히 유용합니다. 정렬 로직을 데이터베이스 계층에서 실행함으로써 성능상의 이점을 얻을 수 있습니다.
