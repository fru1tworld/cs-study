# 포괄적인 SQL 비트 연산 호환성 목록

> 원문: https://blog.jooq.org/the-comprehensive-sql-bitwise-operations-compatibility-list/

SQL 비표준 영역에서 다소 까다롭고 잘 다뤄지지 않는 전쟁터 중 하나가 바로 비트 연산이다. 내가 아는 한, 비트 연산은 어떤 SQL 표준에도 포함되어 있지 않지만(SQL:2008 초안을 확인해 보았다), 거의 모든 데이터베이스가 어떤 형태로든 비트 연산을 지원한다.

우리가 이야기하는 연산은 다음과 같다: `bit_count()`, and (`&`), or (`|`), xor (`^`), not (`~`), left shift (`<<`), right shift (`>>`)

물론 데이터베이스에서 가장 중요한 기능은 아니다. 하지만 이 연산들은 때때로 유용하게 쓰일 수 있다.

그렇다면 다가오는 jOOQ 지원과 관련하여 비트 연산 지원에 대한 순위를 매겨보자:

## 순위

### 1위: "세상을 더 나은 곳으로 만드는 자들"

이 데이터베이스들은 비트의 친구들이다. 아마도 개발팀에 C / 어셈블러 개발자들이 있기 때문일 것이다.

- MySQL: 만점! `bit_count()`를 지원하는 유일한 데이터베이스이다.
- Postgres

### 2위: "자기 본분을 다하는 자들"

이 데이터베이스들은 아무 문제 없다. 중요한 연산들을 지원한다: and (`&`), or (`|`), xor (`^`), not (`~`)

- DB2: `BITANDNOT`까지 지원한다! 연산 이름은 `BITAND`, `BITOR`, `BITXOR`, `BITNOT`이다.
- SQLite: 다만 xor (`^`) 연산자가 빠져 있다.
- SQL Server
- Sybase Adaptive Server
- Sybase SQL Anywhere

### 3위: "잠깐만"

이 데이터베이스들에서 not (`~`) 연산은 어디로 간 걸까? 흠... 하지만 아직 and (`&`), or (`|`), xor (`^`)는 있다.

- H2: 연산 이름은 `BITAND`, `BITOR`, `BITXOR`이다.
- HSQLDB: 연산 이름은 `BITAND`, `BITOR`, `BITXOR`이다.

### 4위: "한번은 좀 실망스러운"

보통 이런 비교에서는 승자인데, 여기서는 패자다. and (`&`) 연산만 지원한다:

- Oracle: 연산 이름은 `BITAND`이다.

### 5위: "게임에서 탈락"

그리고 마지막으로, 비트 연산 기능이 완전히 없는 늘 그런 용의자들이다.

- Ingres: 사용하기 어려운 `BIT_AND`와 `BIT_OR` 지원이 있다. 입출력 타입을 여러 번 변환해야 하므로 이건 해당 사항에 포함되지 않는다.
- Derby: 비트 연산이 전혀 없다.

## jOOQ에서의 시뮬레이션

늘 그렇듯이 jOOQ는 가능한 한 이러한 비호환성 사실들을 개발자에게 숨겨준다. API는 간단하다. 아무 `Field<?>`에서 호출하면 된다:

```java
Field<Integer> bitCount();
Field<T> bitNot();

// 아래 메서드들은 Field 파라미터도 지원하도록 오버로딩되어 있다
Field<T> bitAnd(Number value);
Field<T> bitNand(Number value);
Field<T> bitOr(Number value);
Field<T> bitNor(Number value);
Field<T> bitXor(Number value);
Field<T> bitXNor(Number value);
Field<T> shl(Number value);
Field<T> shr(Number value);
```

### bit_count()

MySQL의 `bit_count()` 함수는 다음과 같은 알고리즘을 사용하여 대부분의 데이터베이스에서 시뮬레이션할 수 있다. 이 경우는 `TINYINT` 데이터 타입 기준이다. `BIGINT`의 경우 꽤 복잡해질 것이다:

```sql
SELECT (my_field &   1)       +
       (my_field &   2) >> 1  +
       (my_field &   4) >> 2  +
       (my_field &   8) >> 3  +
       (my_field &  16) >> 4  +
        ...
       (my_field & 128) >> 7
FROM my_table
```

Java의 `Integer.bitCount(int)` 및 `Long.bitCount(long)` 메서드에도 꽤 기이한 방법이 있다. 나로서는 이해하기 너무 기이해서, MySQL이 하는 것과 같은 것인지 확인하지 않았다:

```java
public static int bitCount(int i) {
    // HD, Figure 5-2
    i = i - ((i >>> 1) & 0x55555555);
    i = (i & 0x33333333) + ((i >>> 2) & 0x33333333);
    i = (i + (i >>> 4)) & 0x0f0f0f0f;
    i = i + (i >>> 8);
    i = i + (i >>> 16);
    return i & 0x3f;
}
```

더 좋은 아이디어가 있다면, 여기에 올려서 평판 점수를 얻을 수 있다: https://stackoverflow.com/questions/7946349/how-to-simulate-the-mysql-bit-count-function-in-sybase-sql-anywhere

### Left / Right Shift (좌/우 시프트)

DB2, H2, HSQLDB, Ingres, Oracle, SQL Server, Sybase ASE, Sybase SQL Anywhere에서 시뮬레이션된다. 이것은 당연히 곱셈 / 나눗셈으로 수행할 수 있다(오버플로우 위험이 있다):

- `a << b` 또는 `shl(a, b)`는 `a * power(2, b)`가 된다.
- `a >> b` 또는 `shr(a, b)`는 `a / power(2, b)`가 된다.

`power` 함수를 사용할 수 없는 경우, `power(a, b)`는 `exp(ln(a) * b)`로 시뮬레이션된다.

### Not / 비트 반전

H2, HSQLDB, Ingres, Oracle에서 시뮬레이션된다. 이것은 산술적으로 수행할 수 있는데, `~a` 또는 `bitnot(a)`를 `-a - 1`로 계산하면 된다.

### 나머지 전부

Oracle에서 시뮬레이션된다. 그 경우에 대해서도 방법을 찾았다:

- `a | b` 또는 `bitor(a, b)`는 `a - (a & b) + b`가 된다.
- `a ^ b` 또는 `bitxor(a, b)`는 `(a | b) - (a & b)` 또는 대안으로 `~(a & b) & (a | b)`가 된다.

## 결론

다시 한번, 얇은 SQL 추상화 계층이 고도로 반복적인 작업으로부터 여러분을 지켜주는 데 매우 강력하다는 것이 입증되었다. 아직 하지 않았다면 지금 바로 jOOQ를 다운로드하라! https://www.jooq.org
