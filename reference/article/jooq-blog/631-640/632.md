# Java 하위 호환성의 아쉬운 측면

> 원문: https://blog.jooq.org/the-lame-side-of-javas-backwards-compatibility/

Java는 뛰어난 하위 호환성을 유지하면서, `java.util.Date`와 `java.util.Calendar` API처럼 JDK 1.1 시절의 deprecated 코드를 여전히 살려두고 있습니다. 그러나 새 버전이 등장할수록 이러한 헌신은 문제를 야기합니다.

JDBC 4.2 명세는 레거시 API의 설계 실수를 해결하기 위한 메서드들을 도입했습니다:

- `Statement.executeLargeBatch()`
- `Statement.executeLargeUpdate(String)`
- `Statement.executeLargeUpdate(String, int)`
- `Statement.executeLargeUpdate(String, int[])`
- `Statement.executeLargeUpdate(String, String[])`
- `Statement.getLargeMaxRows()`
- `Statement.getLargeUpdateCount()`
- `Statement.setLargeMaxRows(long)`

"_large_" 접두사가 붙은 이 메서드들은 `int` 대신 `long`을 반환합니다. 이는 32비트 프로세서가 주류였던 시절에 내려진 원래의 설계 결정을 수정한 것입니다. Java 8의 디펜더 메서드(defender methods) 덕분에 하위 호환성을 유지하면서 이런 추가가 가능해졌습니다.

## 핵심 문제

저자는 성급한 `int` 선택으로 인해 JDK의 얼마나 많은 다른 곳에서 이와 같은 "_large_" 메서드 복제가 필요할지 의문을 제기합니다. 그는 유머러스하게, 2139년경 64비트 공간이 부족해지면 미래 Java 버전에서 `executeHugeUpdate()`와 같은 메서드가 필요해질 것이라고 예측합니다.

## 댓글에서 논의된 사항

댓글 작성자들은 `Date`와 `Calendar` API에 수많은 함정이 있다고 지적했습니다. 한 응답자는 실제로 프로덕션 분석 데이터베이스에서 20억 행 제한에 부딪힌 경험을 언급했는데, 대량의 INSERT와 UPDATE 작업을 수행하는 ETL 작업이 정수 오버플로우로 인해 실패했다고 합니다.
