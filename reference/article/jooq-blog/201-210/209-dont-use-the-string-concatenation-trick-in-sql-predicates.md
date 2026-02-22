> 원문: https://blog.jooq.org/dont-use-the-string-concatenation-trick-in-sql-predicates/

# SQL 조건절에서 문자열 연결 트릭을 사용하지 마세요

## 요약

이 글에서는 SQL 조건절(Predicate)에서 여러 필드를 비교할 때 문자열 연결(String Concatenation)을 사용하는 기법에 대해 경고합니다. 이 방식에는 두 가지 심각한 문제가 있습니다: 정확성(Correctness)과 성능(Performance)입니다.

## 핵심 문제

문자열 연결을 사용한 비교는 미묘한 버그를 만들어냅니다. 배우 이름과 일치하는 고객을 검색할 때 다음과 같이 작성하면:

```sql
WHERE first_name || last_name IN (
  SELECT first_name || last_name FROM actor
)
```

이 쿼리는 의도치 않은 레코드를 잘못 매칭합니다. "JENNI FERDAVIS"라는 고객을 추가하면, 쿼리는 "JENNIFER DAVIS"와 "JENNI FERDAVIS" 두 건 모두를 반환합니다. 두 이름 모두 연결하면 "JENNIFERDAVIS"가 되기 때문입니다. 원래 의도한 "JENNIFER" + "DAVIS" 조합이 아닌 것이죠.

## 구분자로도 해결되지 않는 이유

`'###'`과 같은 구분자(Separator)를 추가하면 일시적으로 안전해 보이지만, 이 접근 방식은 두 가지 이유로 실패합니다:

1. 진정한 해결책이 아닙니다: "불가능한" 구분자(이모지 포함)라도 이론적으로 데이터에 나타날 수 있습니다
2. 심각한 성능 저하: 문자열 연결은 적절한 인덱스 사용을 방해하여, 인덱스 범위 스캔(Index Range Scan) 대신 전체 스캔(Full Scan)을 강제합니다

## 벤치마크 증거

Oracle에서 테스트한 결과, 문자열 연결을 사용하면 적절한 대안에 비해 약 5~6배 느린 실행 성능 저하가 나타났습니다.

## 권장 해결책

지원되는 경우 행 생성자(Row Constructor)를 사용하세요:

```sql
WHERE (first_name, last_name) IN (
  SELECT first_name, last_name FROM actor
)
```

또는 더 넓은 호환성을 위해 `EXISTS` 절을 사용하세요:

```sql
WHERE EXISTS (
  SELECT 1 FROM actor a
  WHERE c.first_name = a.first_name
  AND c.last_name = a.last_name
)
```

두 접근 방식 모두 기존 인덱스를 적절히 활용하고 논리적 정확성을 유지합니다.
