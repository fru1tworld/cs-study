# 값 집합을 동적 SQL UNION 쿼리로 Map Reduce하기

> 원문: https://blog.jooq.org/map-reducing-a-set-of-values-into-a-dynamic-sql-union-query/

Stack Overflow에서 [jOOQ 관련 재미있는 질문](https://stackoverflow.com/q/44952508/521799)을 발견했습니다. 값 집합을 동적으로 아래와 같은 UNION 쿼리로 변환하는 방법에 대한 질문이었습니다:

```sql
SELECT T.COL1 FROM T WHERE T.COL2 = 'V1'
UNION
SELECT T.COL1 FROM T WHERE T.COL2 = 'V2'
...
UNION
SELECT T.COL1 FROM T WHERE T.COL2 = 'VN'
```

Stack Overflow 질문자와 저 모두 IN 조건자를 사용할 수 있다는 것을 잘 알고 있습니다 :-), 하지만 해당 사용자의 특정 MySQL 버전에서 UNION 쿼리가 IN 조건자보다 성능이 더 좋다고 가정해 봅시다.

자바의 함수형 프로그래밍을 사용하면 정말 우아하게 해결할 수 있습니다:

```java
import static org.jooq.impl.DSL.*;
import java.util.*;
import org.jooq.*;

public class Unions {
    public static void main(String[] args) {
        List<String> list = Arrays.asList("V1", "V2", "V3", "V4");

        System.out.println(
            list.stream()
                .map(Unions::query)
                .reduce(Select::union));
    }

    // 입력 문자열로부터 동적으로 쿼리를 생성합니다
    private static Select<Record1<String>> query(String s) {
        return select(T.COL1).from(T).where(T.COL2.eq(s));
    }
}
```

출력 결과:

```
Optional[(
  select T.COL1
  from T
  where T.COL2 = 'V1'
)
union (
  select T.COL1
  from T
  where T.COL2 = 'V2'
)
union (
  select T.COL1
  from T
  where T.COL2 = 'V3'
)
union (
  select T.COL1
  from T
  where T.COL2 = 'V4'
)]
```

정말 멋지지 않나요?

JDK 9+를 사용하면 `Optional.stream()`을 활용하여 더 나아갈 수 있습니다:

```java
List<String> list = Arrays.asList("V1", "V2", "V3", "V4");

try (Stream<Record1<String>> stream = list.stream()
    .map(Unions::query)
    .reduce(Select::union)
    .stream() // Optional.stream()!
    .flatMap(Select::fetchStream)) {
    ...
}
```

빈 리스트를 전달하면 Optional도 비어있게 되므로, flatMap이 빈 스트림을 반환하여 불필요한 데이터베이스 쿼리를 실행하지 않게 됩니다. 정말 우아하지 않나요?
