# PostgreSQL 12.3 Jepsen 분석

## 주요 발견사항

Jepsen이 PostgreSQL 12.3을 테스트한 결과, 두 가지 중요한 문제를 발견했습니다.

### Repeatable Read 격리 수준
PostgreSQL의 "repeatable read"는 실제로 스냅샷 격리(Snapshot Isolation)입니다. 보고서는 "The Repeatable Read isolation level only sees data committed before the transaction began" 명시하고 있으나, G2-item 이상현상이 발생합니다. 이는 ANSI SQL 표준의 모호성으로 인해 해석에 따라 허용될 수 있습니다.

### Serializable 격리 수준의 심각한 버그
더 심각한 문제로, PostgreSQL의 serializable 모드도 G2-item 이상현상을 보였습니다. 이는 "충돌 감지 메커니즘이 세 개의 동시 트랜잭션이 주어졌을 때 트랜잭션 ID를 잘못 식별할 수 있는" 문제 때문입니다.

## 기술적 원인

PostgreSQL 기여자들이 확인한 바에 따르면, 새로 삽입된 행에 대한 읽기-쓰기 충돌 감지 메커니즘의 결함입니다. 이 코드는 2011년 이후 거의 수정되지 않았습니다.

## 권고사항

- PostgreSQL 팀은 패치를 커밋했으며 8월 13일 다음 마이너 릴리스에서 해결될 예정
- 문서에 "snapshot isolation" 용어 명시 권고
- 사용자는 영향을 받을 수 있는 트랜잭션에 명시적 잠금 추가 고려
