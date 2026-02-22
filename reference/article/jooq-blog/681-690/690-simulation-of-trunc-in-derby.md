# Derby에서 TRUNC() 함수 시뮬레이션

> 원문: https://blog.jooq.org/simulation-of-trunc-in-derby/

게시일: 2012년 5월 11일, lukaseder

## 개요

Derby 데이터베이스는 다른 데이터베이스에서 사용할 수 있는 많은 표준 SQL 함수가 부족합니다. 여기에는 `TRUNC(value, decimals)` 함수도 포함됩니다. 이 글에서는 Derby에서 사용 가능한 수학 함수를 사용하여 이 절삭 함수를 시뮬레이션하는 방법을 설명합니다.

## 문제

Derby는 숫자를 지정된 소수점 자리로 절삭하는 `TRUNC()` 함수를 기본적으로 지원하지 않습니다.

## 해결 방법

절삭 원리에 기반한 수학적 접근 방식을 제공합니다. 기본 공식은 floor와 ceiling 함수를 사용한 조건부 로직을 사용합니다:

```sql
CASE WHEN x > 0
THEN
  floor(power(10, n) * x) / power(10, n)
ELSE
  ceil(power(10, n) * x) / power(10, n)
END
```

Derby는 `POWER()` 함수도 없기 때문에, 지수 공식 `power(b, x) = exp(x * ln(b))`를 대체하여 사용합니다:

```sql
CASE WHEN x >= 0
THEN
  floor(exp(n * ln(10)) * x) / exp(n * ln(10))
ELSE
  ceil(exp(n * ln(10)) * x) / exp(n * ln(10))
END
```

## 테스트 결과

이 공식은 값을 성공적으로 절삭합니다:
- 11.111을 n=0으로 절삭하면 11이 됩니다
- 11.111을 n=1로 절삭하면 11.1이 됩니다
- 11.111을 n=2로 절삭하면 11.11이 됩니다
- 11.111을 n=-1로 절삭하면 10이 됩니다

## 결론

다소 장황하고 잠재적으로 비효율적일 수 있지만, 이 시뮬레이션은 Derby에서 사용 가능한 수학 함수만을 사용하여 절삭 기능을 효과적으로 복제합니다.
