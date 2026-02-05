# SQLite에서의 기발한 문자열 함수 시뮬레이션

> 원문: https://blog.jooq.org/funky-string-function-simulation-in-sqlite/

SQLite는 매우 가볍기 때문에 유용한 문자열 함수가 거의 없습니다. 다른 데이터베이스에서 찾을 수 있는 `ASCII()`, `LPAD()`, `RPAD()`, `REPEAT()`, `POSITION()` 같은 일반적인 함수들을 찾아볼 수 없습니다. 하지만 `RANDOMBLOB()`과 `ZEROBLOB()`이라는 매우 유용한 함수가 있습니다.

## REPEAT() 구현

`ZEROBLOB()`과 `QUOTE()` 함수를 조합하여 `REPEAT()` 함수를 시뮬레이션할 수 있습니다.

```sql
replace(substr(quote(zeroblob((Y + 1) / 2)), 3, Y), '0', X)
```

여기서 X는 반복할 문자열이고 Y는 반복 횟수입니다. `QUOTE()` 함수는 blob을 16진수 형식(`X'000000'`)으로 변환하여 필요한 문자 패딩을 생성합니다.

`quote(zeroblob(3))`은 `X'000000'`을 생성합니다. 이 결과를 `SUBSTR()`과 `REPLACE()`로 조작하면 반복을 시뮬레이션할 수 있습니다.

## RPAD() 구현

```sql
Z || replace(
  replace(
    substr(
      quote(zeroblob((X + 1) / 2)),
      3, (X - length(Z))
    ), '''', ''
  ), '0', Y
)
```

매개변수: X(목표 길이), Y(패딩 문자), Z(원본 문자열)

## LPAD() 구현

```sql
replace(
  replace(
    substr(
      quote(zeroblob((X + 1) / 2)),
      3, (X - length(Z))
    ), '''', ''
  ), '0', Y
) || Z
```

## 참고 사항

LPAD/RPAD 솔루션은 Stack Overflow 커뮤니티 멤버들의 기여를 인정합니다. 이러한 "이상한 문제들"을 해결하는 데 외부의 기여가 있었습니다.

이러한 시뮬레이션들은 jOOQ에 포함되어, 수동으로 구현할 필요 없이 SQLite의 제한된 기능을 우회하는 창의적인 접근 방식을 제공합니다.
