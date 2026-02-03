# 표준 SQL/JSON - 냉정한 현실

> 원문: https://blog.jooq.org/standard-sql-json-the-sobering-parts/

이 글은 Lukas Eder가 2021년 7월 27일에 작성한 것으로, 다양한 데이터베이스 벤더들의 SQL/JSON 구현에서 발생하는 실제 문제점들을 살펴봅니다.

## JSON 타입인가, 문자열인가?

JSON을 적절한 타입이 아닌 문자열로 처리하면 의미론적 문제가 발생합니다.

### MariaDB/MySQL 문제점

- 중첩된 JSON 배열이 파생 테이블에서 타입 어노테이션을 잃어버림
- MariaDB 10.5는 중첩 배열을 따옴표가 붙은 문자열로 변환: `[[1, 2], 3]` 대신 `["[1, 2]", 3]`
- 우회 방법 필요:

```sql
json_array(json_merge_preserve('[]', nested), 3)
```

### Oracle 제한사항

- Oracle 21c 이전에는 네이티브 JSON 타입이 없음
- JSON 기본값이 `VARCHAR2`(4000바이트 제한)
- `RETURNING CLOB` 절을 반복적으로 사용해야 함
- `json_arrayagg(coalesce(json_object(), json_object()))`가 `[{}]` 대신 `["{}"]`를 생성
- 해결책: 모든 곳에 `FORMAT JSON` 절 추가

## 잘림(Truncation) 문제

### Oracle

- `json_arrayagg(owner) from all_objects` 쿼리가 "ORA-40478: output value too large" 오류 발생
- JSON을 최대 4000자로 제한
- 실제 사용을 위해 `RETURNING CLOB` 필요

### MySQL

- `JSON_ARRAYAGG()`의 역사적으로 제한된 지원
- `GROUP_CONCAT`이 기본적으로 결과를 잘라냄
- 해결책:

```sql
SET @@group_concat_max_len = 4294967295;
```

- 집계를 위한 우회 방법:

```sql
concat('[', group_concat(json_quote(title)), ']')
```

## 데이터 타입 지원

### MySQL 불리언 처리

- `json_array(true, false)`는 리터럴에서는 작동하지만 표현식에서는 `[1, 0]`을 생성
- 필요한 처리:

```sql
json_extract(case when t = 1 then 'true' when t = 0 then 'false' end, '$')
```

### Oracle 불리언 인코딩

- 네이티브 불리언 타입 없음
- 필요한 처리:

```sql
case when t = 1 then 'true' when t = 0 then 'false' end format json
```

## NULL 처리 차이점

### PostgreSQL/MySQL 접근 방식

SQL `NULL`과 JSON `null`을 올바르게 구분:
- SQL `NULL`: 빈 셀
- JSON `null`: 값 `null`

### Oracle 문제점

- JSON `null`을 제대로 표현할 수 없음
- SQL `NULL`과 JSON `null`이 동일하게 나타남
- 발견된 우회 방법 없음

### Oracle의 장점

집계에 대해 `ABSENT ON NULL`과 `NULL ON NULL` 절 제공:

```sql
json_arrayagg(a)                    -- null 무시: [1]
json_arrayagg(a absent on null)     -- null 무시: [1]
json_arrayagg(a null on null)       -- null 포함: [1, null]
```

## 기타 데이터베이스 참고사항

### Db2

- 문법은 표준을 준수하지만 심각한 버그가 있음
- 현재 실질적으로 사용 불가

### SQL Server

- 완전히 다른 JSON 구현 방식
- 스칼라 값으로 `JSON_ARRAY`를 쉽게 만들 수 없음
- 제한된 구성 기능
- 결과 집합 변환을 위해 `FOR JSON PATH` 사용

## 핵심 요점

저자는 다음과 같이 말합니다: "SQL/JSON은 비교적 늦게, 주로 Oracle에 의해 표준화되었습니다. 표준 자체는 매우 건전합니다. 하지만 안타깝게도 많은 방언들이 문법, 동작에서 서로 다르며, 일부는 여전히 버그가 상당히 많습니다."

## 권장사항

네이티브 SQL/JSON을 작성하는 대신 jOOQ와 같은 추상화 라이브러리를 사용하여 벤더 간 표준화를 달성하세요. 이렇게 하면 "미묘하고 지루한 차이점들"을 자동으로 처리할 수 있습니다.
