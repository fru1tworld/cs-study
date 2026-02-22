# Amazon RDS for PostgreSQL 17.4

날짜: 2025-04-28

Jepsen은 Amazon RDS 클러스터에서 테스트를 실행하기 위한 새로운 실험적 라이브러리를 공개했습니다. 해당 분석 결과에 따르면 Amazon RDS for PostgreSQL에서 작은 문제가 발견되었습니다.

## 발견된 문제

"Repeatable Read" 격리 수준에서 PostgreSQL은 일반적으로 스냅샷 격리(Snapshot Isolation)를 의미하지만, Amazon RDS for PostgreSQL 클러스터는 "Long Fork" 현상을 나타내는 것으로 보입니다. 이러한 동작은 건강한 클러스터에서 버전 13.15부터 17.4에 이르기까지 관찰되었습니다.

## 결론

연구팀의 결론에 따르면 Amazon RDS for PostgreSQL은 더 약한 일관성 모델인 Parallel Snapshot Isolation을 지원할 가능성이 있습니다.
