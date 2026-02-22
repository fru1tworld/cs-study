# Redis-Raft 1b3fbf6

날짜: 2020-06-22

Jepsen은 Redis Labs의 Redis-Raft 초기 개발 빌드를 검증하는 데 지원했습니다. 분석 결과 21개의 문제를 발견했는데, 이는 다음을 포함합니다:
- 충돌(crashes)
- 분열 뇌(split-brain)
- 무한 루프(infinite loops)
- 중단된 읽기(aborted reads)
- 데이터 손상(data corruption)
- 모든 장애 조치 시 전체 데이터 손실(total data loss on any failover)

Redis Labs는 이들 문제 중 하나를 제외한 모든 문제를 최근 버전에서 해결했으며, 2021년 일반 공개를 목표로 계속 작업하고 있습니다.
