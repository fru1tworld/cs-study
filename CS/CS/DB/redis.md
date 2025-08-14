# Redis

- KV Store
- 컬렉션 지원
- Pub/Sub 지원
- RDB, AOF와 같은 영속성 기능이 제공됨
- 복제
  - Redis는 Master, Slave Replication을 지원

# Redis와 Memcached 차이

- 속도: 둘 다 초당 100,000 QPS(Query Per Second) 이상의 속도
- 자료구조: K-V만 지원하는 Memcached와 다르게 List, Hash, Set, Sorted Set을 지원

## Sorted Set

- 확률 기반
- Skip List
