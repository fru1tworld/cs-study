# Java 개발자가 SQL 작성 시 범하는 10가지 추가 흔한 실수

> 원문: https://blog.jooq.org/10-more-common-mistakes-java-developers-make-when-writing-sql/

이 글은 Lukas Eder가 작성한 시리즈의 후속편으로, Java 개발자들이 SQL을 작성할 때 흔히 범하는 추가적인 실수 10가지를 다룹니다.

## 1. PreparedStatement를 사용하지 않는 것

PreparedStatement는 세 가지 이유로 항상 사용해야 합니다:

- 구문 오류 방지: 문자열 연결로 SQL을 작성하면 따옴표 처리 등에서 오류가 발생하기 쉽습니다.
- SQL 인젝션 방지: 바인드 변수를 사용하면 악의적인 입력으로부터 보호됩니다.
- 바인드 변수 엿보기(bind variable peeking): 데이터베이스가 실행 계획을 최적화할 수 있습니다.

기본적으로 모든 쿼리에서 PreparedStatement를 사용하고, 값을 인라인으로 넣지 마세요.

## 2. 너무 많은 컬럼을 반환하는 것

`SELECT *`를 사용하거나 필요하지 않은 컬럼을 선택하면 다음과 같은 문제가 발생합니다:

- IO 낭비: 불필요한 데이터를 디스크에서 읽습니다.
- 메모리 낭비: 사용하지 않을 데이터를 메모리에 적재합니다.
- 최적화 불가: 데이터베이스가 복잡한 뷰 내의 "숨겨진" 조인을 제거하지 못합니다.

해결책은 실제로 필요한 데이터만 선택하는 것입니다.

## 3. JOIN이 SELECT의 일부라고 오해하는 것

SQL 표준에 따르면, JOIN은 SELECT 절의 일부가 아니라 FROM 절 내 테이블 참조의 일부입니다.

```sql
-- 개념적으로 올바른 이해
SELECT ...
FROM (table_reference JOIN table_reference ON ...)
WHERE ...
```

이는 성능 이슈보다는 의미론적 이해에 관한 것입니다.

## 4. ANSI 92 이전의 JOIN 구문 사용

오래된 쉼표로 구분된 테이블 구문은 가독성을 떨어뜨립니다:

```sql
-- 피해야 할 방식 (ANSI 89)
SELECT *
FROM author a, book b
WHERE a.id = b.author_id
AND b.title LIKE '%SQL%'

-- 권장하는 방식 (ANSI 92)
SELECT *
FROM author a
JOIN book b ON a.id = b.author_id
WHERE b.title LIKE '%SQL%'
```

ANSI 구문은 조인 조건과 필터링 조건을 명확하게 분리합니다.

## 5. LIKE 술어에서 이스케이프를 잊는 것

사용자 입력이 LIKE 절에 포함될 때는 와일드카드 문자(`%`, `_`)를 올바르게 이스케이프해야 합니다:

```sql
-- 올바른 이스케이프 사용
SELECT *
FROM t
WHERE x LIKE '%25\%%' ESCAPE '\'
```

특히 언더스코어(`_`)는 자주 간과되지만, 단일 문자 와일드카드로 해석됩니다.

## 6. NOT IN과 NOT(IN)을 혼동하는 것

NULL 값이 관련되면 이 둘은 동등하지 않습니다:

```sql
-- A IN (X, Y)가 TRUE 또는 FALSE를 반환할 때
-- NOT(A IN (X, Y))는 여전히 UNKNOWN을 반환할 수 있음
```

SQL의 3값 논리(TRUE, FALSE, UNKNOWN)로 인해 NULL이 포함되면 예상치 못한 결과가 발생합니다.

## 7. 행 값(Row Value)에서 NULL 술어를 잘못 사용하는 것

다중 컬럼 표현식에서 `NOT (A IS NULL)`과 `A IS NOT NULL`은 다릅니다:

```sql
-- 단일 값에서는 동등함
NOT (x IS NULL) = x IS NOT NULL

-- 행 값에서는 다름
NOT ((x, y) IS NULL) != (x, y) IS NOT NULL
```

3값 논리에서 이 구분은 중요합니다.

## 8. 행 값 표현식(Row Value Expression)을 무시하는 것

이 SQL 기능을 사용하면 복합 키 비교가 더 간결하고 빨라질 수 있습니다:

```sql
-- 개별 비교 대신
WHERE a.x = b.x AND a.y = b.y

-- 행 값 표현식 사용
WHERE (a.x, a.y) = (b.x, b.y)
```

특히 페이지네이션에서 유용합니다:

```sql
-- 키셋 페이지네이션
WHERE (created_at, id) > (?, ?)
ORDER BY created_at, id
```

## 9. 데이터베이스 제약조건을 건너뛰는 것

제약조건은 데이터 무결성뿐만 아니라 쿼리 최적화에도 도움이 됩니다:

- 외래 키: 조인 제거(join elimination) 최적화 가능
- NOT NULL: 불필요한 NULL 검사 제거
- CHECK 제약조건: 쿼리 변환 및 계획 계산 개선

```sql
-- 외래 키가 있으면 데이터베이스가 이 조인을 제거할 수 있음
SELECT a.name
FROM author a
JOIN book b ON a.id = b.author_id
-- book의 컬럼을 사용하지 않으면 조인 불필요
```

## 10. 느린 쿼리 실행을 당연하게 받아들이는 것

현대 데이터베이스는 복잡한 쿼리도 매우 빠르게 실행합니다. 50ms의 실행 시간도 조사가 필요할 수 있습니다.

느린 성능은 보통 데이터베이스의 한계가 아니라 다음을 의미합니다:

- 잘못된 인덱스 설계
- 비효율적인 쿼리 작성
- 부적절한 데이터 모델
- 누락된 통계 정보

해결책: 실행 계획을 분석하고, 인덱스를 검토하며, 쿼리를 튜닝하세요.

---

## 결론

이러한 실수들은 대부분 SQL과 관계형 데이터베이스에 대한 깊은 이해 부족에서 비롯됩니다. Java 개발자로서 SQL을 "그냥 작동하는" 수준이 아닌, 최적화되고 안전한 수준으로 작성하려면 다음을 기억하세요:

1. 항상 PreparedStatement 사용
2. 필요한 컬럼만 선택
3. ANSI JOIN 구문 사용
4. NULL 처리에 주의
5. 제약조건 활용
6. 성능 문제를 당연시하지 않기

jOOQ와 같은 타입 안전한 SQL 빌더를 사용하면 이러한 실수 중 상당수를 컴파일 타임에 방지할 수 있습니다.
