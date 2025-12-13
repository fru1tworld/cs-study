# PostgreSQL 복제 및 고가용성

> 공식 문서: https://www.postgresql.org/docs/current/high-availability.html

---

## 복제 개요

### 용어 정리

| 용어 | 설명 |
|------|------|
| Primary (Master) | 데이터 수정이 가능한 주 서버 |
| Standby (Replica) | 주 서버의 변경사항을 추적하는 서버 |
| Warm Standby | 연결 불가, 복구 대기 |
| Hot Standby | 읽기 쿼리 허용 |

### 복제 방식

| 방식 | 설명 |
|------|------|
| 스트리밍 복제 | WAL 레코드를 실시간 전송 |
| 로그 배송 | WAL 파일을 주기적으로 전송 |
| 논리 복제 | 데이터 변경사항만 전송 |

### 동기화 방식

| 방식 | 장점 | 단점 |
|------|------|------|
| 비동기 | 성능 영향 최소 | 데이터 손실 가능 |
| 동기 | 데이터 손실 방지 | 지연 발생 |

---

## 스트리밍 복제 설정

### Primary 서버 설정

**postgresql.conf:**

```ini
# 복제 설정
wal_level = replica
max_wal_senders = 10
max_replication_slots = 10

# 아카이빙 (선택)
archive_mode = on
archive_command = 'cp %p /backup/wal_archive/%f'

# Hot Standby 허용
hot_standby = on
```

**pg_hba.conf:**

```
# 복제 연결 허용
host    replication     repl_user       192.168.1.0/24    scram-sha-256
```

** 복제 사용자 생성:**

```sql
CREATE ROLE repl_user WITH REPLICATION LOGIN PASSWORD 'password';
```

### Standby 서버 설정

** 베이스 백업 생성:**

```bash
pg_basebackup -h primary_host -U repl_user -D /var/lib/postgresql/data -Fp -Xs -P -R
```

** 옵션 설명:**
- `-R`: standby.signal 및 연결 정보 자동 생성
- `-Xs`: WAL 스트리밍
- `-Fp`: plain 포맷

**postgresql.auto.conf (자동 생성):**

```ini
primary_conninfo = 'host=primary_host port=5432 user=repl_user password=password'
```

**standby.signal 파일:**

```bash
# 파일이 존재하면 standby 모드
touch /var/lib/postgresql/data/standby.signal
```

### 복제 상태 확인

**Primary에서:**

```sql
-- WAL 송신자 상태
SELECT * FROM pg_stat_replication;

-- 복제 슬롯
SELECT * FROM pg_replication_slots;
```

**Standby에서:**

```sql
-- 복구 상태
SELECT pg_is_in_recovery();

-- 수신자 상태
SELECT * FROM pg_stat_wal_receiver;

-- 복제 지연
SELECT now() - pg_last_xact_replay_timestamp() AS replication_lag;
```

---

## 복제 슬롯

### 물리 복제 슬롯

```sql
-- 슬롯 생성
SELECT pg_create_physical_replication_slot('standby1_slot');

-- 슬롯 삭제
SELECT pg_drop_replication_slot('standby1_slot');
```

**Standby에서 슬롯 사용:**

```ini
primary_slot_name = 'standby1_slot'
```

### 논리 복제 슬롯

```sql
-- 슬롯 생성
SELECT pg_create_logical_replication_slot('my_slot', 'pgoutput');

-- 변경사항 읽기
SELECT * FROM pg_logical_slot_get_changes('my_slot', NULL, NULL);
```

---

## 동기식 복제

### Primary 설정

**postgresql.conf:**

```ini
synchronous_commit = on
synchronous_standby_names = 'FIRST 1 (standby1, standby2)'
```

** 동기화 옵션:**

| 옵션 | 설명 |
|------|------|
| `FIRST n (list)` | 목록에서 처음 n개 대기 |
| `ANY n (list)` | 목록에서 아무 n개 대기 |
| `*` | 모든 standby |

### 동기화 수준

```sql
-- 트랜잭션별 설정
SET synchronous_commit = 'off';       -- 비동기
SET synchronous_commit = 'local';     -- 로컬만 보장
SET synchronous_commit = 'remote_write'; -- 원격 쓰기 버퍼
SET synchronous_commit = 'on';        -- 원격 플러시
SET synchronous_commit = 'remote_apply'; -- 원격 적용
```

---

## 논리 복제

### 개요

논리 복제는 테이블 수준에서 데이터 변경사항을 복제합니다.

** 특징:**
- 특정 테이블만 복제 가능
- 다른 버전 간 복제 가능
- 양방향 복제 가능

### Publisher 설정

**postgresql.conf:**

```ini
wal_level = logical
```

**Publication 생성:**

```sql
-- 특정 테이블
CREATE PUBLICATION my_pub FOR TABLE users, orders;

-- 모든 테이블
CREATE PUBLICATION all_pub FOR ALL TABLES;

-- 필터 조건 (PostgreSQL 15+)
CREATE PUBLICATION filtered_pub FOR TABLE orders
WHERE (status = 'completed');
```

### Subscriber 설정

**Subscription 생성:**

```sql
CREATE SUBSCRIPTION my_sub
CONNECTION 'host=primary port=5432 dbname=mydb user=repl_user password=password'
PUBLICATION my_pub;
```

**Subscription 관리:**

```sql
-- 일시 중지
ALTER SUBSCRIPTION my_sub DISABLE;

-- 재개
ALTER SUBSCRIPTION my_sub ENABLE;

-- 새로고침 (테이블 추가 시)
ALTER SUBSCRIPTION my_sub REFRESH PUBLICATION;

-- 삭제
DROP SUBSCRIPTION my_sub;
```

### 상태 확인

```sql
-- Publisher
SELECT * FROM pg_publication;
SELECT * FROM pg_publication_tables;

-- Subscriber
SELECT * FROM pg_subscription;
SELECT * FROM pg_stat_subscription;
```

---

## 장애 조치 (Failover)

### 수동 Failover

**Standby를 Primary로 승격:**

```bash
# pg_ctl 사용
pg_ctl promote -D /var/lib/postgresql/data

# 또는 트리거 파일
touch /var/lib/postgresql/data/promote
```

**SQL 사용:**

```sql
SELECT pg_promote();
```

### 자동 Failover

자동 Failover를 위해서는 외부 도구가 필요합니다:

- **Patroni**: etcd/Consul/ZooKeeper 기반
- **repmgr**: PostgreSQL 전용 클러스터 관리
- **pg_auto_failover**: Citus 제공

### Patroni 설정 예시

**patroni.yml:**

```yaml
scope: postgres-cluster
name: node1

restapi:
  listen: 0.0.0.0:8008
  connect_address: node1:8008

etcd:
  hosts: etcd1:2379,etcd2:2379,etcd3:2379

bootstrap:
  dcs:
    ttl: 30
    loop_wait: 10
    retry_timeout: 10
    maximum_lag_on_failover: 1048576
    postgresql:
      use_pg_rewind: true
      parameters:
        wal_level: replica
        hot_standby: on

  initdb:
    - encoding: UTF8
    - data-checksums

postgresql:
  listen: 0.0.0.0:5432
  connect_address: node1:5432
  data_dir: /var/lib/postgresql/data
  authentication:
    replication:
      username: replicator
      password: rep-pass
    superuser:
      username: postgres
      password: postgres-pass
```

---

## 캐스케이딩 복제

Standby 서버가 다른 Standby의 소스가 됩니다.

** 구조:**
```
Primary -> Standby1 -> Standby2
                   -> Standby3
```

**Standby1 설정:**

```ini
# Primary에서 복제
primary_conninfo = 'host=primary ...'
```

**Standby2 설정:**

```ini
# Standby1에서 복제
primary_conninfo = 'host=standby1 ...'
```

---

## 지연 복제 (Delayed Replication)

인위적으로 복제 지연을 설정합니다 (실수 복구용).

**Standby 설정:**

```ini
recovery_min_apply_delay = '1h'  -- 1시간 지연
```

---

## 읽기 부하 분산

### pgpool-II 설정

```
backend_hostname0 = 'primary'
backend_port0 = 5432
backend_weight0 = 1
backend_flag0 = 'ALLOW_TO_FAILOVER'

backend_hostname1 = 'standby1'
backend_port1 = 5432
backend_weight1 = 1
backend_flag1 = 'ALLOW_TO_FAILOVER'

load_balance_mode = on
```

### HAProxy 설정

```
frontend pgsql
    bind *:5432
    default_backend pgsql_backend

backend pgsql_backend
    option httpchk GET /primary
    http-check expect status 200
    default-server inter 3s fall 3 rise 2
    server primary primary:5432 check
    server standby1 standby1:5432 check backup
```

---

## 모니터링

### 복제 지연 모니터링

```sql
-- 바이트 단위 지연
SELECT
    client_addr,
    state,
    sent_lsn,
    write_lsn,
    flush_lsn,
    replay_lsn,
    pg_wal_lsn_diff(sent_lsn, replay_lsn) AS lag_bytes
FROM pg_stat_replication;

-- 시간 기반 지연 (Standby에서)
SELECT
    CASE
        WHEN pg_last_wal_receive_lsn() = pg_last_wal_replay_lsn()
        THEN 0
        ELSE EXTRACT(EPOCH FROM now() - pg_last_xact_replay_timestamp())
    END AS lag_seconds;
```

### 슬롯 모니터링

```sql
-- 슬롯이 WAL 보유량
SELECT
    slot_name,
    slot_type,
    active,
    pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) AS retained_bytes
FROM pg_replication_slots;
```

---

## 고가용성 체크리스트

- [ ] 복제 구성 완료
- [ ] 동기식/비동기식 결정
- [ ] 복제 슬롯 설정
- [ ] 장애 조치 절차 문서화
- [ ] 자동 Failover 설정 (옵션)
- [ ] 복제 지연 모니터링
- [ ] 정기적인 Failover 테스트
- [ ] 복구 절차 테스트

---

## 참고 문서

- [고가용성](https://www.postgresql.org/docs/current/high-availability.html)
- [스트리밍 복제](https://www.postgresql.org/docs/current/warm-standby.html)
- [논리 복제](https://www.postgresql.org/docs/current/logical-replication.html)
- [복제 슬롯](https://www.postgresql.org/docs/current/warm-standby.html#STREAMING-REPLICATION-SLOTS)
