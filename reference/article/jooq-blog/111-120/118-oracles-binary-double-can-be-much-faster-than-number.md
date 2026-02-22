# Oracle의 BINARY_DOUBLE은 NUMBER보다 훨씬 빠를 수 있다

> 원문: https://blog.jooq.org/oracles-binary_double-can-be-much-faster-than-number/

어떤 계산에 올바른 데이터 타입을 사용하는 것은 당연한 조언처럼 들립니다.

예를 들어, 저는 [왜 항상 TIMESTAMP보다 DATETIME을 사용해야 하는지](https://blog.jooq.org/why-you-should-always-store-your-dates-using-datetime-not-timestamp/)에 대해 글을 쓴 적이 있습니다. 그 글은 다른 것들 중에서도 TIMESTAMP가 불일치를 유발할 수 있는 반면, 올바른 시간대 정보가 포함된 DATETIME은 그렇지 않다는 것을 보여주었습니다. 또한 Markus Winand가 쓴 [인덱스된 날짜 열에서 년도를 추출하는 것](https://use-the-index-luke.com/sql/where-clause/obfuscation/dates)에 관한 유사한 글도 있습니다. 그 글은 올바른 타입 사용이 통계 기반 최적화도 허용한다는 것을 보여줍니다.

## 정확한 숫자 대 근사 숫자

`NUMBER` 타입(또는 다른 RDBMS의 `DECIMAL`, `NUMERIC`)은 올바른 정밀도와 반올림이 필요한 모든 십진수에 완벽하게 적합합니다. 예를 들어, 금액에는 정확히 2자리 소수점이 있어야 하며, 은행 계좌를 집계할 때 정확한 십진수 반올림을 사용하기 때문에 그렇습니다.

하지만 정밀도가 중요하지 않은 경우가 많이 있습니다. 이 경우, 저는 숫자 계산을 위한 것이 아니라 "측정값"(물리학에서와 같이)이나 다른 통계적 목적을 위한 데이터에 대해 이야기하고 있습니다.

예를 들어, 로그 법칙은 큰 수의 곱을 큰 수의 합으로 변환하는 데 사용될 수 있습니다. 대략적으로:

- `ln(a * b) = ln(a) + ln(b)`

그러므로:

- `a * b = exp(ln(a) + ln(b))`

### 속도를 원하십니까?

이 벤치마크를 보세요. Oracle에서 `NUMBER(20, 10)`과 `BINARY_DOUBLE`을 비교합니다(그리고 `DOUBLE PRECISION`도 비교에 포함시켰습니다. 왜냐하면 `DOUBLE PRECISION`은 Oracle에서 `NUMBER`의 별칭이기 때문입니다):

```sql
CREATE TABLE data (
  n1 NUMBER(20, 10),
  n2 NUMBER(20, 10),
  d1 DOUBLE PRECISION,
  d2 DOUBLE PRECISION,
  b1 BINARY_DOUBLE,
  b2 BINARY_DOUBLE
);

INSERT INTO data
SELECT level, ln(level), level, ln(level), level, ln(level)
FROM dual
CONNECT BY level <= 100000;
```

따라서 값과 미리 계산된 로그를 포함하는 6개의 열이 있습니다. 실제 값에는 열 이름에 `1`이 붙고, 로그에는 열 이름에 `2`가 붙습니다.

벤치마크 기법은 [여기에 설명되어 있습니다](https://blog.jooq.org/a-nice-way-to-benchmark-sql-in-oracle/). 많은 주의 사항이 있으며 제한된 수의 검증에만 유용하다는 점을 유의하세요.

이제 다음 문장들을 실행합니다:

```sql
-- 문장 1: NUMBER에서 직접 LN 계산
SELECT sum(ln(n1)) FROM data;

-- 문장 2: NUMBER를 BINARY_DOUBLE로 캐스팅 후 LN 계산
SELECT sum(ln(cast(n1 as binary_double))) FROM data;

-- 문장 3: NUMBER를 TO_BINARY_DOUBLE로 변환 후 LN 계산
SELECT sum(ln(to_binary_double(n1))) FROM data;

-- 문장 4: NUMBER에서 미리 계산된 LN 값 합계
SELECT sum(n2) FROM data;

-- 문장 5: DOUBLE PRECISION에서 직접 LN 계산
SELECT sum(ln(d1)) FROM data;

-- 문장 6: DOUBLE PRECISION을 BINARY_DOUBLE로 캐스팅 후 LN 계산
SELECT sum(ln(cast(d1 as binary_double))) FROM data;

-- 문장 7: DOUBLE PRECISION을 TO_BINARY_DOUBLE로 변환 후 LN 계산
SELECT sum(ln(to_binary_double(d1))) FROM data;

-- 문장 8: DOUBLE PRECISION에서 미리 계산된 LN 값 합계
SELECT sum(d2) FROM data;

-- 문장 9: BINARY_DOUBLE에서 직접 LN 계산
SELECT sum(ln(b1)) FROM data;

-- 문장 10: BINARY_DOUBLE에서 미리 계산된 LN 값 합계
SELECT sum(b2) FROM data;
```

### 결과

벤치마크 결과는 다음과 같습니다:

| 문장 | 데이터 타입 | 연산 | 비율 |
|------|-------------|------|------|
| 1 | NUMBER(20,10) | SUM(LN(value)) | 280.75 |
| 2 | NUMBER(20,10) | SUM(LN(CAST)) | 8.04 |
| 3 | NUMBER(20,10) | SUM(LN(TO_BINARY_DOUBLE)) | 7.73 |
| 4 | NUMBER(20,10) | SUM(미리계산) | 1.12 |
| 5 | DOUBLE PRECISION | SUM(LN(value)) | 279.73 |
| 6 | DOUBLE PRECISION | SUM(LN(CAST)) | 8.07 |
| 7 | DOUBLE PRECISION | SUM(LN(TO_BINARY_DOUBLE)) | 7.80 |
| 8 | DOUBLE PRECISION | SUM(미리계산) | 1.54 |
| 9 | BINARY_DOUBLE | SUM(LN(value)) | 2.57 |
| 10 | BINARY_DOUBLE | SUM(미리계산) | 1.00 |

문장 1과 5는 문장 9보다 112배 느립니다!

이 결과에서 몇 가지 사실을 알 수 있습니다:

- `NUMBER`와 `DOUBLE PRECISION`(Oracle에서는 `NUMBER`의 별칭)에서 `LN()` 함수를 직접 실행하면 매우 느립니다(280배).
- `BINARY_DOUBLE`에서 `LN()` 함수를 실행하면 훨씬 빠릅니다(2.57배).
- `NUMBER`나 `DOUBLE PRECISION`을 `BINARY_DOUBLE`로 변환한 후 `LN()`을 계산하면 합리적인 성능을 얻을 수 있습니다(약 8배).
- 미리 계산된 값을 사용하면 가장 빠릅니다.

## 왜 이런 현상이 발생하는가?

`NUMBER` 타입은 임의 정밀도 십진수를 처리하도록 설계되었습니다. 이는 정확한 반올림이 필요한 금융 계산에 적합합니다. 그러나 이러한 정밀도에는 비용이 따릅니다 - CPU 집약적인 수학 연산(예: 로그, 지수, 삼각 함수)은 `NUMBER` 타입에서 훨씬 더 느립니다.

반면에 `BINARY_DOUBLE`은 IEEE 754 부동 소수점 표준을 구현합니다. 이 표준은 바로 이러한 목적, 즉 효율적인 수치 계산을 위해 설계되었습니다. 하드웨어 수준에서 최적화되어 있어 수학 함수를 훨씬 빠르게 실행할 수 있습니다.

이는 Java 개발과도 유사합니다. 우리는 Java에서 `java.math.BigDecimal`로 CPU 집약적인 계산을 하지 않을 것입니다. 금융 데이터에는 `BigDecimal`을 사용하지만, 분석 계산에는 `double`이 적합합니다.

## 실용적인 권장 사항

Java든 데이터베이스든 CPU 집약적인 계산을 수행할 때는 항상 다양한 옵션을 평가해야 합니다:

1. 빠른 수정: 값을 즉시 변환합니다 - `CAST(number_column AS BINARY_DOUBLE)`을 사용하면 약 3배만 느려집니다(112배보다 훨씬 낫습니다).

2. 스키마 마이그레이션: 정확한 십진수 정밀도가 필요하지 않은 열의 경우, 열 타입을 `BINARY_DOUBLE`로 변경하는 것을 고려하세요.

3. 전처리: 자주 사용되는 값을 미리 계산하세요. 위의 예에서 `ln(value)`를 별도의 열에 저장하면 가장 빠른 성능을 얻을 수 있습니다.

## 결론

정확한 십진수 정밀도가 중요하지 않은 숫자 데이터에 대한 분석 작업의 경우, `BINARY_DOUBLE`을 사용하거나 계산 중에 이 타입으로 변환하면 상당한 성능 향상을 얻을 수 있습니다 - 특히 로그와 같은 초월 함수에서 그렇습니다.

핵심 통찰은 하나의 타입이 모든 시나리오를 동등하게 처리한다고 가정하기보다는 데이터 타입을 실제 계산 요구 사항에 맞추는 것입니다.
