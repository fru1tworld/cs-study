# SQL에서 백분위수를 계산하여 데이터 집합 편향 파악하기

> 원문: https://blog.jooq.org/calculate-percentiles-to-learn-about-data-set-skew-in-sql/

B-Tree 인덱스는 데이터가 상당히 균일하게 분포되어 있을 때 매우 유용합니다. 데이터가 편향되어 있을 때는 그다지 유용하지 않습니다. 여러분의 RDBMS 통계는 일단 계산되면 이 정보를 포함하고 있지만, 백분위수를 사용하여 임의 쿼리에서 이러한 "편향"을 수동으로 감지할 수도 있습니다. 백분위수는 SQL 표준에 정의되어 있으며 다양한 데이터베이스에서 일반 집계 함수로 지원됩니다.

## 편향이란 무엇인가?

"편향(Skew)"은 정규 분포가 대칭이 아닐 때를 나타내는 통계학 용어입니다. 우리가 의미하는 편향은 데이터베이스 컨텍스트에서 일부 값이 다른 값보다 훨씬 더 자주 나타나는 것을 의미합니다. 즉, 데이터가 균일하게 분포되어 있지 않다는 것입니다.

RDBMS에서 지원하는 SQL 표준 `PERCENTILE_DISC` 및 `PERCENTILE_CONT` 집계 함수를 사용하여 데이터의 분포를 확인할 수 있습니다. 여기에는 Oracle, PostgreSQL, 그리고 SQL Server가 포함됩니다(다만 SQL Server는 윈도우 함수 형태로만 지원하며 집계 함수로는 지원하지 않습니다).

## 균일 분포

균일하게 분포된 열의 예시로 Sakila 데이터베이스의 FILM 테이블에 있는 FILM_ID 열을 살펴보겠습니다. 아래 쿼리는 다양한 백분위수에서 얼마나 많은 FILM_ID 값이 존재하는지 보여줍니다:

```sql
SELECT
  percentile_disc(0.0) WITHIN GROUP (ORDER BY film_id) AS "0%",
  percentile_disc(0.1) WITHIN GROUP (ORDER BY film_id) AS "10%",
  percentile_disc(0.2) WITHIN GROUP (ORDER BY film_id) AS "20%",
  percentile_disc(0.3) WITHIN GROUP (ORDER BY film_id) AS "30%",
  percentile_disc(0.4) WITHIN GROUP (ORDER BY film_id) AS "40%",
  percentile_disc(0.5) WITHIN GROUP (ORDER BY film_id) AS "50%",
  percentile_disc(0.6) WITHIN GROUP (ORDER BY film_id) AS "60%",
  percentile_disc(0.7) WITHIN GROUP (ORDER BY film_id) AS "70%",
  percentile_disc(0.8) WITHIN GROUP (ORDER BY film_id) AS "80%",
  percentile_disc(0.9) WITHIN GROUP (ORDER BY film_id) AS "90%",
  percentile_disc(1.0) WITHIN GROUP (ORDER BY film_id) AS "100%"
FROM film;
```

결과는 다음과 같습니다:

| 0% | 10% | 20% | 30% | 40% | 50% | 60% | 70% | 80% | 90% | 100% |
|----|-----|-----|-----|-----|-----|-----|-----|-----|-----|------|
| 1  | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900 | 1000 |

보시다시피 데이터는 완벽하게 균일하게 분포되어 있습니다. 값이 1에서 1000까지이고, 백분위수 간격이 10%씩 증가하면 대략 100씩 증가합니다. 이것을 차트로 그리면 직선을 얻게 됩니다:

```
100% |                                                          *
 90% |                                                     *
 80% |                                                *
 70% |                                           *
 60% |                                      *
 50% |                                 *
 40% |                            *
 30% |                       *
 20% |                  *
 10% |             *
  0% |        *
     +------------------------------------------------------------
         1   100  200  300  400  500  600  700  800  900  1000
```

균형 잡힌 트리(balanced tree) 인덱스는 데이터가 상당히 균일하게 분포되어 있을 때 매우 유용합니다. 그 이유는 ID = 1인 레코드를 찾든 ID = 832인 레코드를 찾든 동일한 복잡도 O(log N)으로 해당 값을 찾아갈 수 있기 때문입니다(물론 루트 노드에 더 가까운 곳에 있거나 같은 블록 또는 같은 리프에 있는지에 따라 약간의 편차가 있을 수 있습니다).

이것은 FILM_ID 값을 찾는 쿼리에 매우 유용한 속성입니다:

```sql
SELECT *
FROM film
WHERE film_id = 1
```

## "편향된" 분포

이제 AMOUNT(금액) 열에 대해 PAYMENT 테이블에서 동일한 작업을 해보겠습니다:

```sql
SELECT
  percentile_disc(0.0) WITHIN GROUP (ORDER BY amount) AS "0%",
  percentile_disc(0.1) WITHIN GROUP (ORDER BY amount) AS "10%",
  percentile_disc(0.2) WITHIN GROUP (ORDER BY amount) AS "20%",
  percentile_disc(0.3) WITHIN GROUP (ORDER BY amount) AS "30%",
  percentile_disc(0.4) WITHIN GROUP (ORDER BY amount) AS "40%",
  percentile_disc(0.5) WITHIN GROUP (ORDER BY amount) AS "50%",
  percentile_disc(0.6) WITHIN GROUP (ORDER BY amount) AS "60%",
  percentile_disc(0.7) WITHIN GROUP (ORDER BY amount) AS "70%",
  percentile_disc(0.8) WITHIN GROUP (ORDER BY amount) AS "80%",
  percentile_disc(0.9) WITHIN GROUP (ORDER BY amount) AS "90%",
  percentile_disc(1.0) WITHIN GROUP (ORDER BY amount) AS "100%"
FROM payment;
```

결과는 다음과 같습니다:

| 0%   | 10%  | 20%  | 30%  | 40%  | 50%  | 60%  | 70%  | 80%  | 90%  | 100%  |
|------|------|------|------|------|------|------|------|------|------|-------|
| 0.00 | 0.99 | 1.99 | 2.99 | 2.99 | 3.99 | 4.99 | 4.99 | 5.99 | 6.99 | 11.99 |

데이터의 분포가 더 이상 균일하지 않습니다. 낮은 범위에 훨씬 더 많은 레코드가 집중되어 있고, 높은 범위에는 레코드가 거의 없습니다. 이를 막대 차트로 그려보겠습니다:

```
100% |                                                          *
 90% |                            *
 80% |                       *
 70% |                   *
 60% |                *
 50% |             *
 40% |          *
 30% |       *
 20% |     *
 10% |   *
  0% | *
     +------------------------------------------------------------
       0.00  1.99  2.99  3.99  4.99  5.99  6.99        ...  11.99
```

차트의 선이 초반에 거의 평평합니다(낮은 값에 많은 항목이 집중되어 있음). 실제 분포를 보기 위해 개수를 집계할 수도 있습니다:

```sql
SELECT amount, count(*)
FROM (
  SELECT trunc(amount) AS amount
  FROM payment
) t
GROUP BY amount
ORDER BY amount;
```

결과:

| amount | count |
|--------|-------|
| 0      | 3003  |
| 1      | 641   |
| 2      | 3542  |
| 3      | 1117  |
| 4      | 3789  |
| 5      | 1306  |
| 6      | 1119  |
| 7      | 675   |
| 8      | 486   |
| 9      | 257   |
| 10     | 104   |
| 11     | 10    |

보시다시피 0-2 범위와 4-5 범위에 많은 레코드가 있고, 높은 값으로 갈수록 급격히 줄어듭니다.

## 상관관계

데이터의 분포가 다른 열과 상관관계가 있는지도 분석할 수 있습니다. 예를 들어, FILM 테이블의 LENGTH(상영 시간) 열이 RATING(등급)과 어떤 관계가 있는지 살펴보겠습니다. `GROUP BY ROLLUP`을 사용하면 각 등급별 분포와 전체 분포를 함께 볼 수 있습니다:

```sql
SELECT
  rating,
  count(*),
  percentile_disc(0.0) WITHIN GROUP (ORDER BY length) AS "0%",
  percentile_disc(0.1) WITHIN GROUP (ORDER BY length) AS "10%",
  percentile_disc(0.2) WITHIN GROUP (ORDER BY length) AS "20%",
  percentile_disc(0.3) WITHIN GROUP (ORDER BY length) AS "30%",
  percentile_disc(0.4) WITHIN GROUP (ORDER BY length) AS "40%",
  percentile_disc(0.5) WITHIN GROUP (ORDER BY length) AS "50%",
  percentile_disc(0.6) WITHIN GROUP (ORDER BY length) AS "60%",
  percentile_disc(0.7) WITHIN GROUP (ORDER BY length) AS "70%",
  percentile_disc(0.8) WITHIN GROUP (ORDER BY length) AS "80%",
  percentile_disc(0.9) WITHIN GROUP (ORDER BY length) AS "90%",
  percentile_disc(1.0) WITHIN GROUP (ORDER BY length) AS "100%"
FROM film
GROUP BY ROLLUP(rating);
```

결과:

| rating | count | 0% | 10% | 20% | 30% | 40% | 50% | 60% | 70% | 80% | 90% | 100% |
|--------|-------|----|-----|-----|-----|-----|-----|-----|-----|-----|-----|------|
| G      | 178   | 47 | 57  | 67  | 80  | 93  | 107 | 121 | 138 | 156 | 176 | 185  |
| PG     | 194   | 46 | 58  | 72  | 85  | 99  | 113 | 122 | 137 | 151 | 168 | 185  |
| PG-13  | 223   | 46 | 61  | 76  | 92  | 110 | 125 | 138 | 150 | 162 | 176 | 185  |
| R      | 195   | 49 | 68  | 82  | 90  | 104 | 115 | 129 | 145 | 160 | 173 | 185  |
| NC-17  | 210   | 46 | 58  | 74  | 84  | 97  | 112 | 125 | 138 | 153 | 174 | 184  |
| (전체) | 1000  | 46 | 60  | 74  | 86  | 102 | 114 | 128 | 142 | 156 | 173 | 185  |

이 경우 상영 시간은 등급과 상관관계가 없습니다. 모든 등급에서 상영 시간이 거의 동일하게 분포되어 있습니다. 하지만 일부 데이터 집합에서는 이러한 상관관계가 성능 튜닝에 중요한 통찰을 제공할 수 있습니다.

## 이것이 성능에 어떻게 도움이 되는가?

"편향된" 데이터에 접근할 때, 일부 값은 다른 값보다 "더 동등"합니다(조지 오웰의 "동물농장"을 인용하자면). 이것은 예를 들어 payment 테이블에서 금액을 찾을 때 쿼리가 동일하지 않다는 것을 의미합니다. amount 열에 대한 인덱스가 일부 쿼리에는 유용했을 수 있지만 다른 쿼리에는 그렇지 않을 수 있습니다:

```sql
-- 많은 행이 반환됨 (3644개)
SELECT * FROM payment WHERE amount BETWEEN 0 AND 2;

-- 적은 행이 반환됨 (361개)
SELECT * FROM payment WHERE amount BETWEEN 9 AND 11;
```

두 쿼리는 같은 범위 크기(3)를 검색하지만, 반환되는 행의 수는 10배 차이가 납니다. 옵티마이저가 이를 인식하지 못하면 잘못된 실행 계획을 선택할 수 있습니다.

데이터베이스의 통계(히스토그램)가 이 정보를 포함하고 있으면, 옵티마이저는 더 나은 결정을 내릴 수 있습니다. 하지만 때로는 바인드 변수 피킹(bind variable peeking)이나 적응형 커서 공유(adaptive cursor sharing) 같은 기능이 제대로 작동하지 않을 수 있습니다. 이런 경우 데이터의 편향을 미리 파악하고 있으면 쿼리 튜닝이나 인덱스 전략을 세우는 데 도움이 됩니다.

## 결론

간단한 SQL 집계 함수인 백분위수를 사용하여 데이터의 "편향"을 계산하고 시각화할 수 있습니다. 이는 데이터 분포를 이해하고 성능 최적화 전략을 수립하는 데 유용합니다.

일반적으로 다음과 같은 데이터 유형별 분포 특성을 예상할 수 있습니다:

균일한 분포:
- 시퀀스로 생성된 대리 키(surrogate key)
- UUID
- 일대일 관계의 외래 키

약간의 편향:
- 날짜/타임스탬프 (데이터가 시간에 따라 균등하게 입력된 경우)
- 이름
- 전화번호

상당한 편향:
- 다대일(to-many) 관계의 외래 키 (예: 일부 고객이 다른 고객보다 더 많은 자산을 보유)
- 숫자 값 (예: 금액)
- 코드 및 기타 이산 값 (예: 영화 등급, 결제 정산 코드 등)

데이터의 편향을 이해하면 인덱스 전략, 쿼리 최적화, 그리고 전반적인 데이터베이스 성능 튜닝에 큰 도움이 됩니다.
