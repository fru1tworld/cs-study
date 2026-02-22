# 2016년은 Java가 드디어 윈도우 함수를 갖게 된 해로 기억될 것이다!

> 원문: https://blog.jooq.org/2016-will-be-the-year-remembered-as-when-java-finally-had-window-functions/

맞습니다, 제대로 들으셨습니다. 지금까지 멋진 윈도우 함수는 SQL에서만 사용할 수 있는 기능이었습니다. 심지어 정교한 함수형 프로그래밍 언어들조차 아직 이 아름다운 기능을 갖추지 못한 것 같습니다 (제가 틀렸다면 Haskell 분들이 지적해 주세요). 우리는 윈도우 함수에 대해 수많은 블로그 게시물을 작성하며 독자들에게 전파해 왔습니다. 예를 들면:

- "아마도 가장 멋진 SQL 기능: 윈도우 함수"
- "이 멋진 윈도우 함수 트릭을 사용하여 시계열에서 시간 차이를 계산하는 방법"
- "SQL에서 가장 긴 연속 이벤트 시리즈를 찾는 방법"
- "FIRST_VALUE(), LAST_VALUE(), LEAD(), LAG()의 놀라운 SQL 파워를 놓치지 마세요"
- "ROW_NUMBER(), RANK(), DENSE_RANK()의 차이점"
- "NoSQL? No SQL – 누적 합계 계산하는 방법"

윈도우 함수의 제가 가장 좋아하는 사용 사례 중 하나는 누적 합계(running total)입니다. 즉, 다음과 같은 은행 계좌 거래 테이블에서 계산된 잔액이 있는 테이블로 변환하는 것입니다.

| ID   | VALUE_DATE | AMOUNT  | BALANCE  |
|------|------------|---------|----------|
| 9997 | 2014-03-18 | 99.17   | 19985.81 |
| 9981 | 2014-03-16 | 71.44   | 19886.64 |
| 9979 | 2014-03-16 | -94.60  | 19815.20 |
| 9977 | 2014-03-16 | -6.96   | 19909.80 |
| 9971 | 2014-03-15 | -65.95  | 19916.76 |

SQL로는 이것이 아주 쉽습니다. `SUM(t.amount) OVER(...)`의 사용법을 살펴보세요:

```sql
SELECT
  t.*,
  t.current_balance - NVL(
    SUM(t.amount) OVER (
      PARTITION BY t.account_id
      ORDER BY     t.value_date DESC,
                   t.id         DESC
      ROWS BETWEEN UNBOUNDED PRECEDING
           AND     1         PRECEDING
    ),
  0) AS balance
FROM     v_transactions t
WHERE    t.account_id = 1
ORDER BY t.value_date DESC,
         t.id         DESC
```

## 윈도우 함수는 어떻게 작동할까요?

(윈도우 함수와 그 외 많은 것들을 배우려면 우리의 SQL 마스터클래스를 예약하는 것을 잊지 마세요!) 때때로 조금 무서워 보이는 문법에도 불구하고, 윈도우 함수는 정말 이해하기 쉽습니다. 윈도우는 `FROM / WHERE / GROUP BY / HAVING` 절에서 생성된 데이터의 "뷰(view)"입니다. 윈도우 함수를 사용하면 `SELECT` 절(또는 드물게 `ORDER BY` 절)에서 무언가를 계산하는 동안 현재 행을 기준으로 다른 모든 행에 접근할 수 있습니다. 위의 문장이 실제로 하는 일은 다음과 같습니다:

즉, 주어진 잔액에 대해 현재 잔액에서 현재 행과 같은 파티션(같은 은행 계좌)에 있고, 현재 행보다 엄격히 "위에" 있는 모든 행의 윈도우 "`OVER()`"에 대한 `SUM()`을 뺍니다. 자세히 말하면:

- `PARTITION BY`는 윈도우가 "`OVER()`" 어떤 행들을 포함하는지 지정합니다
- `ORDER BY`는 윈도우가 어떻게 정렬되는지 지정합니다
- `ROWS`는 어떤 정렬된 행 인덱스를 고려해야 하는지 지정합니다

## Java 컬렉션으로도 이것을 할 수 있을까요?

네, 할 수 있습니다! jOOλ를 사용한다면요: JDK 8 Stream과 Collector API가 충분하지 않다고 생각해서 우리가 설계한 완전히 무료이고 오픈 소스인 Apache 2.0 라이센스 라이브러리입니다. Java 8이 설계될 때, 병렬 스트림 지원에 많은 초점이 맞춰졌습니다. 그것은 좋지만, 함수형 프로그래밍이 적용될 수 있는 유일하게 유용한 영역은 분명 아닙니다. 우리는 이 갭을 메우기 위해 jOOλ를 만들었습니다 – vavr이나 functional java처럼 완전히 새로운 대체 컬렉션 API를 구현하지 않으면서요. jOOλ는 이미 다음을 제공합니다:

1. 튜플 타입
2. 정렬된, 순차 전용 스트림을 위한 더 유용한 것들

최근 출시된 jOOλ 0.9.9에서 우리는 두 가지 주요 새 기능을 추가했습니다:

1. 수많은 새로운 Collector들
2. 윈도우 함수

## JDK에서 누락된 많은 Collector들

JDK는 몇 가지 collector를 제공하지만, 이것들은 어색하고 장황해 보이며, 이 Stack Overflow 질문(그리고 다른 많은 질문들)에서 노출된 것과 같은 collector를 작성하는 것을 좋아하는 사람은 아무도 없습니다. 하지만 링크된 질문에서 노출된 사용 사례는 매우 유효한 것입니다. 사람들의 목록에서 여러 가지를 집계하고 싶습니다:

이 목록이 있다고 가정하면, 이제 다음과 같은 집계를 얻고 싶습니다:

- 사람 수
- 최대 나이
- 최소 키
- 평균 몸무게

SQL을 작성하는 데 익숙한 사람이라면 이것은 너무나 쉬운 문제입니다:

끝. Java에서는 얼마나 어려울 수 있을까요? 바닐라 JDK 8 API로는 많은 글루 코드를 작성해야 한다는 것이 밝혀졌습니다.

jOOλ 0.9.9를 사용하면, 이 문제를 해결하는 것이 다시 터무니없이 간단해지고, 거의 SQL처럼 읽힙니다:

```java
Tuple result =
Seq.seq(personsList)
   .collect(
       count(),
       max(Person::getAge),
       min(Person::getHeight),
       avg(Person::getWeight)
   );

System.out.println(result);
```

이것은 SQL 데이터베이스에 대해 쿼리를 실행하는 것이 아닙니다(그것은 jOOQ가 하는 일입니다). 우리는 인메모리 Java 컬렉션에 대해 이 "쿼리"를 실행하고 있습니다.

## 좋아요, 이것은 이미 굉장합니다. 이제 윈도우 함수는 어떨까요?

맞습니다, 이 글의 제목은 사소한 집계 기능을 약속한 것이 아닙니다. 멋진 윈도우 함수를 약속했습니다. 하지만 윈도우 함수는 데이터 스트림의 부분 집합에 대한 집계(또는 순위)일 뿐입니다. 스트림(또는 테이블) 전체를 단일 레코드로 집계하는 대신, 원본 레코드를 유지하면서 각 개별 레코드에 직접 집계를 제공하고 싶은 것입니다. 윈도우 함수에 대한 좋은 입문 예제는 ROW_NUMBER(), RANK(), DENSE_RANK()의 차이를 설명하는 이 글에서 제공됩니다. 다음 PostgreSQL 쿼리를 살펴보세요:

```sql
SELECT
  v,
  ROW_NUMBER() OVER(w),
  RANK()       OVER(w),
  DENSE_RANK() OVER(w)
FROM (
  VALUES('a'),('a'),('a'),('b'),
        ('c'),('c'),('d'),('e')
) t(v)
WINDOW w AS (ORDER BY v);
```

jOOλ 0.9.9를 사용하면 Java 8에서도 동일하게 할 수 있습니다:

```java
System.out.println(
    Seq.of("a", "a", "a", "b", "c", "c", "d", "e")
       .window(naturalOrder())
       .map(w -> tuple(
            w.value(),
            w.rowNumber(),
            w.rank(),
            w.denseRank()
       ))
       .format()
);
```

결과는...

| v0 | v1 | v2 | v3 |
|----|----|----|-----|
| a  | 0  | 0  | 0   |
| a  | 1  | 0  | 0   |
| a  | 2  | 0  | 0   |
| b  | 3  | 3  | 1   |
| c  | 4  | 4  | 2   |
| c  | 5  | 4  | 2   |
| d  | 6  | 6  | 3   |
| e  | 7  | 7  | 4   |

다시 한 번 참고하세요, 우리는 데이터베이스에 대해 어떤 쿼리도 실행하지 않습니다. 모든 것이 메모리에서 이루어집니다. 두 가지를 주목하세요:

- jOOλ의 윈도우 함수는 0 기반 순위를 반환합니다. 이는 모두 1 기반인 SQL과 달리 Java API에서 예상되는 것입니다.
- Java에서는 명명된 열이 있는 임시 레코드를 생성하는 것이 불가능합니다. 이것은 안타깝고, 미래의 Java가 이러한 언어 기능을 지원하기를 바랍니다.

코드에서 정확히 무슨 일이 일어나는지 살펴보겠습니다:

`Seq.of("a", "a", "a", "b", "c", "c", "d", "e")` – 이것은 단지 값을 나열하는 것입니다

`.window(naturalOrder())` – 여기서 스트림의 값 T에 대해 자연순으로 정렬되는 단일 윈도우를 지정합니다

위의 윈도우 절은 `Window<T>` 객체(여기서는 `w`)를 생성하며, 이것은 다음을 노출합니다...

`w.value()` – String 타입의 현재 값 자체...

`w.rowNumber()`, `w.rank()`, `w.denseRank()` – 위 윈도우에 대한 다양한 순위나 집계.

`.format()` – 테이블을 생성하기 위한 멋진 포매팅

그게 전부입니다! 쉽지 않나요? 더 많은 것을 할 수 있습니다! 이것을 확인해 보세요:

```java
System.out.println(
    Seq.of("a", "a", "a", "b", "c", "c", "d", "e")
       .window(naturalOrder())
       .map(w -> tuple(
            w.value(),   // v0
            w.count(),   // v1
            w.median(),  // v2
            w.lead(),    // v3
            w.lag(),     // v4
            w.toString() // v5
       ))
       .format()
);
```

위의 코드는 무엇을 출력할까요?

| v0 | v1 | v2 | v3      | v4      | v5       |
|----|----|----|---------|---------|----------|
| a  | 1  | a  | a       | {empty} | a        |
| a  | 2  | a  | a       | a       | aa       |
| a  | 3  | a  | b       | a       | aaa      |
| b  | 4  | a  | c       | a       | aaab     |
| c  | 5  | a  | c       | b       | aaabc    |
| c  | 6  | a  | d       | c       | aaabcc   |
| d  | 7  | b  | e       | c       | aaabccd  |
| e  | 8  | b  | {empty} | d       | aaabccde |

여러분의 분석가 마음이 뛰고 있을 것입니다.

## 잠깐만요. SQL처럼 프레임도 할 수 있나요?

네, 할 수 있습니다. SQL에서와 마찬가지로, 윈도우 정의에서 프레임 절을 생략하면(`ORDER BY` 절은 지정하면서) 다음이 기본적으로 적용됩니다:

```
ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
```

우리는 이전 예제에서 이것을 했습니다. v5 열에서 볼 수 있는데, 여기서 문자열을 맨 첫 번째 값부터 현재 값까지 집계합니다. 그러면 프레임을 지정해 봅시다:

```java
System.out.println(
    Seq.of("a", "a", "a", "b", "c", "c", "d", "e")
       .window(naturalOrder(), -1, 1) // 여기서 프레임 지정
       .map(w -> tuple(
            w.value(),   // v0
            w.count(),   // v1
            w.median(),  // v2
            w.lead(),    // v3
            w.lag(),     // v4
            w.toString() // v5
       ))
       .format()
);
```

결과는 당연히:

| v0 | v1 | v2 | v3      | v4      | v5  |
|----|----|----|---------|---------|-----|
| a  | 2  | a  | a       | {empty} | aa  |
| a  | 3  | a  | a       | a       | aaa |
| a  | 3  | a  | b       | a       | aab |
| b  | 3  | b  | c       | a       | abc |
| c  | 3  | c  | c       | b       | bcc |
| c  | 3  | c  | d       | c       | ccd |
| d  | 3  | d  | e       | c       | cde |
| e  | 2  | d  | {empty} | d       | de  |

예상대로, `lead()`와 `lag()`는 영향을 받지 않지만, `count()`, `median()`, `toString()`는 영향을 받습니다.

## 멋지네요! 이제 누적 합계를 검토해 봅시다.

종종, 스트림 자체의 스칼라 값에 대해 윈도우 함수를 계산하지 않습니다. 왜냐하면 그 값은 보통 스칼라 값이 아니라 튜플(또는 Java 용어로 POJO)이기 때문입니다. 대신, 튜플(또는 POJO)에서 값을 추출하고 그것에 대해 집계를 수행합니다. 따라서 다시, `BALANCE`를 계산할 때 먼저 `AMOUNT`를 추출해야 합니다.

다음은 Java 8과 jOOλ 0.9.9로 누적 합계를 작성하는 방법입니다:

```java
BigDecimal currentBalance = new BigDecimal("19985.81");

Seq.of(
    tuple(9997, "2014-03-18", new BigDecimal("99.17")),
    tuple(9981, "2014-03-16", new BigDecimal("71.44")),
    tuple(9979, "2014-03-16", new BigDecimal("-94.60")),
    tuple(9977, "2014-03-16", new BigDecimal("-6.96")),
    tuple(9971, "2014-03-15", new BigDecimal("-65.95")))
.window(Comparator
    .comparing((Tuple3<Integer, String, BigDecimal> t)
        -> t.v1, reverseOrder())
    .thenComparing(t -> t.v2), Long.MIN_VALUE, -1)
.map(w -> w.value().concat(
     currentBalance.subtract(w.sum(t -> t.v3)
                              .orElse(BigDecimal.ZERO))
));
```

결과는:

| v0   | v1         | v2     | v3       |
|------|------------|--------|----------|
| 9997 | 2014-03-18 | 99.17  | 19985.81 |
| 9981 | 2014-03-16 | 71.44  | 19886.64 |
| 9979 | 2014-03-16 | -94.60 | 19815.20 |
| 9977 | 2014-03-16 | -6.96  | 19909.80 |
| 9971 | 2014-03-15 | -65.95 | 19916.76 |

여기서 몇 가지가 바뀌었습니다:

- 비교자는 이제 두 가지 비교를 고려합니다. 안타깝게도 JEP-101이 완전히 구현되지 않아서, 여기서 타입 추론을 위해 컴파일러를 도와줘야 합니다.
- `Window.value()`는 이제 단일 값이 아니라 튜플입니다. 그래서 거기서 관심 있는 열인 `AMOUNT`(`t -> t.v3`를 통해)를 추출해야 합니다. 반면에, 단순히 그 추가 값을 튜플에 `concat()`할 수 있습니다.

하지만 그게 전부입니다. 비교자의 장황함을 제외하면(미래의 jOOλ 버전에서 분명히 해결할 것입니다), 윈도우 함수를 작성하는 것은 아주 쉽습니다.

## 또 무엇을 할 수 있을까요?

이 글은 새 API로 할 수 있는 모든 것에 대한 완전한 설명이 아닙니다. 곧 추가 예제와 함께 후속 블로그 게시물을 작성할 것입니다. 예를 들면:

- partition by 절은 설명되지 않았지만, 역시 사용 가능합니다
- 여기서 노출된 단일 윈도우보다 더 많은 윈도우를 지정할 수 있으며, 각각 개별 `PARTITION BY`, `ORDER BY` 및 프레임 사양을 가집니다

또한, 현재 구현은 다소 정규적(canonical)입니다. 즉, 아직 집계를 캐시하지 않습니다:

- 정렬되지 않은 / 프레임이 없는 윈도우의 경우 (파티션 전체에 대해 동일한 값)
- 엄격하게 오름차순으로 프레임된 윈도우의 경우 (집계는 `SUM()`이나 `toString()`과 같은 결합적 collector의 경우 이전 값을 기반으로 할 수 있음)

여기까지입니다. jOOλ를 다운로드하고, 이것저것 해보고, 가장 멋진 SQL 기능이 이제 모든 Java 8 개발자에게 제공된다는 사실을 즐기세요!
