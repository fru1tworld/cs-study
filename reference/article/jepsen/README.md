# Jepsen 분산 시스템 분석 모음

Jepsen(https://jepsen.io)의 분산 시스템 테스트 및 분석 보고서 한국어 번역 모음입니다.

## 디렉터리 구조

```
jepsen/
├── blog/          # 블로그 글
├── analyses/      # 상세 분석 보고서
└── README.md      # 이 파일
```

## 블로그 글 (blog/)

| 날짜 | 제목 | 파일명 |
|------|------|--------|
| 2025-12-07 | NATS 2.12.1 | 2025-12-08-nats-2.12.1.md |
| 2025-12-01 | Jepsen 0.3.10 | 2025-12-02-jepsen-0.3.10.md |
| 2025-10-19 | 분산 시스템 신뢰성 용어집 | 2025-10-20-distsys-glossary.md |
| 2025-08-17 | Jepsen 18: Serializable Mom | 2025-08-18-jepsen-18-serializable-mom.md |
| 2025-08-06 | Capela dda5892 | 2025-08-07-capela-dda5892.md |
| 2025-08-05 | The GeekNarrator 팟캐스트 | 2025-08-06-geeknarrator-podcast.md |
| 2025-08-04 | Jepsen 17: ACID Jazz | 2025-08-05-jepsen-17-talk.md |
| 2025-07-14 | 분산 시스템 용어집 | 2025-07-15-distributed-systems-glossary.md |
| 2025-06-05 | TigerBeetle 0.16.11 | 2025-06-06-tigerbeetle-0.16.11.md |
| 2025-04-28 | Amazon RDS for PostgreSQL 17.4 | 2025-04-29-amazon-rds-for-postgresql-17.4.md |
| 2025-03-03 | 예정된 이벤트 | 2025-03-04-upcoming-events.md |
| 2024-11-26 | Antithesis & Bufstream 웨비나 | 2024-11-27-antithesis-bufstream-webinar.md |
| 2024-10-24 | Bufstream 0.1.0 | 2024-11-12-bufstream-0.1.0.md |
| 2024-10-24 | MariaDB 스냅샷 격리 | 2024-11-07-mariadb-snapshot-isolation.md |
| 2024-10-24 | 분산 시스템 기초 - 공개 세션 | 2024-10-25-distributed-systems-class.md |
| 2024-10-17 | Jepsen 0.3.6 | 2024-10-18-jepsen-0.3.6.md |
| 2024-08-28 | Jepsen 16 - Systems Distributed 2024 | 2024-08-29-jepsen-16.md |
| 2024-08-07 | jetcd 0.8.2 | 2024-08-08-jetcd.md |
| 2024-07-22 | GOTO Chicago 2024 | 2024-07-23-goto-chicago.md |
| 2024-07-16 | 윤리 정책 업데이트 | 2024-07-17-ethics-update.md |
| 2024-05-14 | Datomic Pro 1.0.7075 | 2024-05-15-datomic-pro-1.0.7075.md |
| 2024-01-30 | RavenDB 6.0.2 | 2024-01-31-ravendb-6.0.2.md |
| 2023-12-18 | MySQL 8.0.34 | 2023-12-19-mysql-8.0.34.md |
| 2022-04-28 | Redpanda 21.10.1 | 2022-04-29-redpanda-21.10.1.md |
| 2022-02-04 | Radix DLT 1.0-beta.35.1 | 2022-02-05-radix-dlt-1.0-beta.35.1.md |
| 2020-12-22 | Scylla 4.2-rc3 | 2020-12-23-scylla-4.2-rc3.md |
| 2020-06-22 | Redis-Raft 1b3fbf6 | 2020-06-23-redis-raft-1b3fbf6.md |
| 2020-06-11 | PostgreSQL 12.3 | 2020-06-12-postgresql-12.3.md |
| 2020-05-14 | MongoDB 4.2.6 | 2020-05-15-mongodb-4.2.6.md |
| 2020-04-29 | Dgraph 1.1.1 | 2020-04-30-dgraph-1.1.1.md |
| 2020-01-29 | etcd 3.4.3 | 2020-01-30-etcd-3.4.3.md |
| 2019-09-04 | YugaByte DB 1.3.1 | 2019-09-05-yugabyte-db-1.3.1.md |
| 2019-06-11 | TiDB 2.1.7 | 2019-06-12-tidb-2.1.7.md |
| 2019-03-25 | YugaByte DB 1.1.9 | 2019-03-26-yugabyte-db-1.1.9.md |
| 2019-03-04 | FaunaDB 2.5.4 | 2019-03-05-faunadb-2.5.4.md |
| 2018-10-22 | MongoDB 3.6.4 | 2018-10-23-mongodb-3.6.4.md |
| 2018-08-22 | Dgraph 1.0.2 | 2018-08-23-dgraph-1-0-2.md |
| 2018-03-06 | Aerospike 3.99.0.3 | 2018-03-07-aerospike-3-99-0-3.md |
| 2017-10-05 | Hazelcast 3.8.3 | 2017-10-06-hazelcast-3.8.3.md |
| 2017-09-04 | Tendermint 0.10.2 | 2017-09-05-tendermint-0-10-2.md |
| 2017-02-15 | CockroachDB beta-20160829 | 2017-02-16-cockroachdb-beta-20160829.md |
| 2017-02-06 | MongoDB 3.4.0-rc3 | 2017-02-07-mongodb-3-4-0-rc3.md |
| 2016-08-16 | ElasticSearch 발산 문제 | 2016-08-17-elasticsearch-divergence.md |

## 상세 분석 보고서 (analyses/)

| 시스템 | 버전 | 파일명 |
|--------|------|--------|
| NATS | 2.12.1 | nats-2.12.1.md |
| Bufstream | 0.1.0 | bufstream-0.1.0.md |
| Amazon RDS PostgreSQL | 17.4 | amazon-rds-for-postgresql-17.4.md |
| TigerBeetle | 0.16.11 | tigerbeetle-0.16.11.md |
| Capela | dda5892 | capela-dda5892.md |
| Datomic Pro | 1.0.7075 | datomic-pro-1.0.7075.md |
| jetcd | 0.8.2 | jetcd-0.8.2.md |
| RavenDB | 6.0.2 | ravendb-6.0.2.md |
| MySQL | 8.0.34 | mysql-8.0.34.md |
| PostgreSQL | 12.3 | postgresql-12.3.md |
| MongoDB | 4.2.6 | mongodb-4.2.6.md |
| MongoDB | 3.6.4 | mongodb-3.6.4.md |
| MongoDB | 3.4.0-rc3 | mongodb-3.4.0-rc3.md |
| etcd | 3.4.3 | etcd-3.4.3.md |
| Redis-Raft | 1b3fbf6 | redis-raft-1b3fbf6.md |
| Redpanda | 21.10.1 | redpanda-21.10.1.md |
| Scylla | 4.2-rc3 | scylla-4.2-rc3.md |
| Dgraph | 1.1.1 | dgraph-1.1.1.md |
| CockroachDB | beta-20160829 | cockroachdb-beta-20160829.md |
| TiDB | 2.1.7 | tidb-2.1.7.md |
| YugaByte DB | 1.3.1 | yugabyte-db-1.3.1.md |
| FaunaDB | 2.5.4 | faunadb-2.5.4.md |
| Radix DLT | 1.0-beta.35.1 | radix-dlt-1.0-beta.35.1.md |
| Hazelcast | 3.8.3 | hazelcast-3.8.3.md |
| Tendermint | 0.10.2 | tendermint-0.10.2.md |
| Aerospike | 3.99.0.3 | aerospike-3.99.0.3.md |
| VoltDB | 6.3 | voltdb-6.3.md |

## Jepsen 소개

Jepsen은 Kyle Kingsbury(aphyr)가 설립한 분산 시스템 테스트 및 컨설팅 회사입니다. 주요 활동:

- 분산 시스템 안전성 테스트: 데이터베이스, 메시지 큐, 합의 시스템 등의 일관성 보장 검증
- 교육: 분산 시스템 기초 클래스 제공
- 도구 개발: Jepsen 테스팅 프레임워크, Elle 일관성 검사기 등

## 주요 개념

### 일관성 모델
- Linearizability (선형화 가능성): 가장 강력한 일관성, 모든 작업이 원자적으로 순간에 발생하는 것처럼 보임
- Serializability (직렬화 가능성): 트랜잭션이 직렬로 실행된 것처럼 보임
- Snapshot Isolation (스냅샷 격리): 각 트랜잭션이 일관된 스냅샷을 봄

### 일반적인 문제
- Split-brain (분열 뇌): 네트워크 분할 시 여러 마스터가 동시에 존재
- Lost Update (손실된 업데이트): 동시 쓰기로 인해 일부 업데이트가 손실
- Stale Read (부실 읽기): 이전 버전의 데이터를 읽음
- Dirty Read (더티 읽기): 커밋되지 않은 데이터를 읽음

## 참고 자료

- [Jepsen 공식 웹사이트](https://jepsen.io)
- [Jepsen GitHub](https://github.com/jepsen-io)
- [분산 시스템 신뢰성 용어집](https://jepsen.io/consistency)

---

원문 저작권: © Jepsen, LLC
번역: 학습 목적으로 번역됨
