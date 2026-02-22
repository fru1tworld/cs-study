> 원문: https://blog.jooq.org/how-to-emulate-partial-indexes-in-oracle/

# Oracle에서 부분 인덱스(Partial Index)를 에뮬레이션하는 방법

부분 인덱스(Partial Index, 필터링된 인덱스라고도 함)는 SQL Server와 PostgreSQL에서 기본적으로 지원되지만, Oracle에서는 사용할 수 없는 기능입니다. 하지만 Oracle의 특성을 활용하면 이를 에뮬레이션할 수 있습니다.

## 핵심 개념

Oracle은 모든 인덱스에서 NULL 값을 자동으로 제외합니다. 개발자들은 이 동작을 활용하여 함수 기반 표현식(Function-Based Expression)으로 부분 인덱스를 생성할 수 있습니다:

```sql
CREATE INDEX i ON message (
  CASE WHEN deleted > 0 THEN deleted END
);
```

이 방식은 CASE 표현식이 NULL이 아닌 값을 반환하는 행만 인덱싱하여 사실상 부분 인덱스를 생성합니다.

## 성능상의 이점

이 기법은 상당한 이점을 제공합니다:

- 저장 공간: 값이 0인 99,999개의 행과 값이 1인 1개의 행이 있는 테스트 테이블에서, 인덱스는 테이블의 152개 블록에 비해 단 1개의 리프 블록(Leaf Block)만 사용했습니다.
- 삽입 속도: CASE 표현식에 의해 변환된 100만 개의 NULL 값을 삽입하는 데 1.5초가 걸린 반면, 인덱싱되는 100만 개의 값을 삽입하는 데는 8.2초가 걸렸습니다.
- 쿼리 효율성: 부분 인덱스는 스캔 오버헤드를 크게 줄여줍니다.

## 중요한 주의사항

인덱스를 활용하려면 쿼리에서 정확히 동일한 함수 기반 표현식을 재현해야 합니다:

```sql
-- 풀 테이블 스캔(Full Table Scan) 사용
SELECT * FROM message WHERE deleted = 1;

-- 부분 인덱스 사용
SELECT * FROM message
WHERE CASE WHEN deleted > 0 THEN deleted END = 1;
```

표현식 구문이 일치하지 않으면, 인덱스가 존재하더라도 옵티마이저(Optimizer)는 풀 테이블 스캔을 수행합니다.

## 결론

이 기법은 데이터 분포가 크게 편향된 경우에 효과적이지만, 개발자는 인덱스 정의와 WHERE 절 모두에서 동일한 표현식을 일관되게 사용해야 합니다. 이로 인해 쿼리 유지보수가 복잡해질 수 있습니다.
