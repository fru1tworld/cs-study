# 제27장. 데이터베이스 활동 모니터링 (Monitoring Database Activity)

> PostgreSQL 18 공식 문서 번역
>
> 원문: https://www.postgresql.org/docs/current/monitoring.html

데이터베이스 관리자는 종종 "시스템이 지금 무엇을 하고 있는가?"라는 질문에 대한 답을 찾아야 합니다. 이 장에서는 데이터베이스 활동을 모니터링하고 시스템이 수행하는 작업을 이해하는 방법에 대해 설명합니다.

---

## 목차

- [27.1 표준 유닉스 도구](#271-표준-유닉스-도구)
- [27.2 누적 통계 시스템](#272-누적-통계-시스템)
  - [27.2.1 통계 수집 구성](#2721-통계-수집-구성)
  - [27.2.2 통계 보기](#2722-통계-보기)
  - [27.2.3 pg_stat_activity](#2723-pg_stat_activity)
  - [27.2.4 pg_stat_replication](#2724-pg_stat_replication)
  - [27.2.5 pg_stat_replication_slots](#2725-pg_stat_replication_slots)
  - [27.2.6 pg_stat_wal_receiver](#2726-pg_stat_wal_receiver)
  - [27.2.7 pg_stat_recovery_prefetch](#2727-pg_stat_recovery_prefetch)
  - [27.2.8 pg_stat_subscription](#2728-pg_stat_subscription)
  - [27.2.9 pg_stat_subscription_stats](#2729-pg_stat_subscription_stats)
  - [27.2.10 pg_stat_ssl](#27210-pg_stat_ssl)
  - [27.2.11 pg_stat_gssapi](#27211-pg_stat_gssapi)
  - [27.2.12 pg_stat_archiver](#27212-pg_stat_archiver)
  - [27.2.13 pg_stat_io](#27213-pg_stat_io)
  - [27.2.14 pg_stat_bgwriter](#27214-pg_stat_bgwriter)
  - [27.2.15 pg_stat_checkpointer](#27215-pg_stat_checkpointer)
  - [27.2.16 pg_stat_wal](#27216-pg_stat_wal)
  - [27.2.17 pg_stat_database](#27217-pg_stat_database)
  - [27.2.18 pg_stat_database_conflicts](#27218-pg_stat_database_conflicts)
  - [27.2.19 pg_stat_all_tables](#27219-pg_stat_all_tables)
  - [27.2.20 pg_stat_all_indexes](#27220-pg_stat_all_indexes)
  - [27.2.21 pg_statio_all_tables](#27221-pg_statio_all_tables)
  - [27.2.22 pg_statio_all_indexes](#27222-pg_statio_all_indexes)
  - [27.2.23 pg_statio_all_sequences](#27223-pg_statio_all_sequences)
  - [27.2.24 pg_stat_user_functions](#27224-pg_stat_user_functions)
  - [27.2.25 pg_stat_slru](#27225-pg_stat_slru)
  - [27.2.26 통계 함수](#27226-통계-함수)
- [27.3 잠금 보기](#273-잠금-보기)
- [27.4 진행 상황 보고](#274-진행-상황-보고)
  - [27.4.1 ANALYZE 진행 상황 보고](#2741-analyze-진행-상황-보고)
  - [27.4.2 CLUSTER 진행 상황 보고](#2742-cluster-진행-상황-보고)
  - [27.4.3 COPY 진행 상황 보고](#2743-copy-진행-상황-보고)
  - [27.4.4 CREATE INDEX 진행 상황 보고](#2744-create-index-진행-상황-보고)
  - [27.4.5 VACUUM 진행 상황 보고](#2745-vacuum-진행-상황-보고)
  - [27.4.6 Base Backup 진행 상황 보고](#2746-base-backup-진행-상황-보고)
- [27.5 동적 추적](#275-동적-추적)
  - [27.5.1 동적 추적을 위한 컴파일](#2751-동적-추적을-위한-컴파일)
  - [27.5.2 내장 프로브](#2752-내장-프로브)
  - [27.5.3 프로브 사용하기](#2753-프로브-사용하기)
  - [27.5.4 새 프로브 정의하기](#2754-새-프로브-정의하기)
- [27.6 디스크 사용량 모니터링](#276-디스크-사용량-모니터링)
  - [27.6.1 디스크 사용량 확인](#2761-디스크-사용량-확인)
  - [27.6.2 디스크 가득 참 장애](#2762-디스크-가득-참-장애)

---

## 27.1 표준 유닉스 도구

대부분의 유닉스 플랫폼에서 PostgreSQL은 `ps`에 의해 보고되는 명령 제목을 수정하여 개별 서버 프로세스를 쉽게 식별할 수 있도록 합니다.

샘플 `ps` 출력은 다음과 같습니다:

```
$ ps auxww | grep ^postgres
postgres  15551  0.0  0.1  57536  7132 pts/0    S    18:02   0:00 postgres -i
postgres  15554  0.0  0.0  57536  1184 ?        Ss   18:02   0:00 postgres: background writer
postgres  15555  0.0  0.0  57536   916 ?        Ss   18:02   0:00 postgres: checkpointer
postgres  15556  0.0  0.0  57536   916 ?        Ss   18:02   0:00 postgres: walwriter
postgres  15557  0.0  0.0  58504  2244 ?        Ss   18:02   0:00 postgres: autovacuum launcher
postgres  15582  0.0  0.0  58772  3080 ?        Ss   18:04   0:00 postgres: joe runbug 127.0.0.1 idle
postgres  15606  0.0  0.0  58772  3052 ?        Ss   18:07   0:00 postgres: tgl regression [local] SELECT waiting
postgres  15610  0.0  0.0  58772  3056 ?        Ss   18:07   0:00 postgres: tgl regression [local] idle in transaction
```

### 프로세스 유형

1. 주 서버 프로세스: 원래 명령 인수와 함께 나열된 첫 번째 프로세스
2. 백그라운드 워커 프로세스: 주 프로세스에 의해 자동으로 시작됨
   - Background writer
   - Checkpointer
   - WAL writer
   - Autovacuum launcher (활성화된 경우)
3. 클라이언트 연결 프로세스: 개별 클라이언트 연결을 처리

### 클라이언트 프로세스 형식

각 클라이언트 연결 프로세스는 다음을 표시합니다:

```
postgres: user database host activity
```

- user, database, host: 클라이언트 연결 수명 동안 동일하게 유지
- activity: 변경되며 다음 값을 가질 수 있음:
  - `idle`: 클라이언트 명령 대기 중
  - `idle in transaction`: BEGIN 블록 내에서 클라이언트 대기 중
  - 명령 유형 이름 (예: `SELECT`)
  - `waiting`: 다른 세션이 보유한 잠금 대기 중인 경우 추가됨

### 클러스터 이름 표시

`cluster_name`이 구성된 경우 `ps` 출력에 표시됩니다:

```
$ ps aux|grep server1
postgres   27093  0.0  0.0  30096  2752 ?        Ss   11:34   0:00 postgres: server1: background writer
```

### 구성 옵션

- update_process_title: 끄면 활동 표시기가 업데이트되지 않습니다. 새 프로세스가 시작될 때만 프로세스 제목이 설정됩니다. 이는 일부 플랫폼에서 명령당 측정 가능한 오버헤드를 절약할 수 있습니다.

### Solaris 특수 처리

Solaris 시스템의 경우:
- `/bin/ps` 대신 `/usr/ucb/ps` 사용
- `w` 플래그를 하나가 아닌 두 개 사용
- 원래 postgres 명령 호출은 서버 프로세스보다 더 짧은 `ps` 상태 표시를 가져야 함

---

## 27.2 누적 통계 시스템

PostgreSQL의 누적 통계 시스템(cumulative statistics system) 은 서버 활동에 대한 정보를 수집하고 보고합니다. 다음을 추적합니다:

- 테이블 및 인덱스 액세스 (디스크 블록 및 행 수준)
- 테이블당 행 수
- Vacuum 및 Analyze 작업
- 사용자 정의 함수 호출 및 실행 시간 (활성화된 경우)
- 실시간 시스템 활동 (누적 통계와 독립적)

### 27.2.1 통계 수집 구성

통계 수집은 쿼리 실행에 오버헤드를 추가하며 다음 매개변수로 제어됩니다 (`postgresql.conf`에서 설정):

| 매개변수 | 용도 |
|---------|-----|
| `track_activities` | 서버 프로세스의 현재 명령 실행 모니터링 |
| `track_cost_delay_timing` | 비용 기반 vacuum 지연 모니터링 |
| `track_counts` | 테이블/인덱스 액세스에 대한 누적 통계 수집 |
| `track_functions` | 사용자 정의 함수 사용 추적 |
| `track_io_timing` | 블록 읽기, 쓰기, 확장 및 fsync 시간 모니터링 |
| `track_wal_io_timing` | WAL 읽기, 쓰기 및 fsync 시간 모니터링 |

주요 사항:

- 매개변수는 `SET` 명령을 사용하여 세션별로 설정할 수 있음 (슈퍼유저만 해당)
- 각 PostgreSQL 프로세스가 공유 메모리에 통계 수집
- 프로세스는 적절한 간격으로 통계를 플러시
- 정상 종료 시: 통계가 `pg_stat` 하위 디렉토리에 저장됨 (재시작 시에도 유지)
- 비정상 종료 시: 모든 카운터 재설정

### 27.2.2 통계 보기

#### 중요 고려사항

1. 업데이트 지연: 통계는 즉시 업데이트되지 않음
   - 프로세스는 유휴 상태가 되기 직전에 공유 메모리로 플러시
   - `PGSTAT_MIN_INTERVAL`(기본값 1초)보다 자주 플러시하지 않음
   - `track_activities`의 현재 쿼리 정보는 항상 최신 상태

2. 스냅샷/캐시 동작:
   - 액세스된 값은 트랜잭션 끝까지 캐시됨 (기본 구성)
   - `stats_fetch_consistency` 매개변수 사용:
     - `snapshot`: 왜곡 최소화 (메모리 사용량 증가)
     - `none`: 통계에 한 번만 액세스하는 경우 캐싱 비활성화
   - `pg_stat_clear_snapshot()`으로 스냅샷 지우기

3. 트랜잭션별 뷰:
   - `pg_stat_xact_all_tables`, `pg_stat_xact_sys_tables`, `pg_stat_xact_user_tables`
   - `pg_stat_xact_user_functions`
   - 트랜잭션 전체에서 지속적으로 업데이트 (아직 공유 메모리로 플러시되지 않음)

4. 보안 제한:
   - 일반 사용자는 자신의 세션 정보만 볼 수 있음
   - 슈퍼유저와 `pg_read_all_stats` 역할은 모든 세션을 볼 수 있음
   - 세션 존재 및 일반 속성은 모든 사용자에게 표시됨

#### 표 27.1: 동적 통계 뷰

| 뷰 이름 | 설명 |
|--------|-----|
| `pg_stat_activity` | 서버 프로세스당 하나의 행과 현재 활동 |
| `pg_stat_replication` | 대기 서버에 대한 WAL 발신자 통계 |
| `pg_stat_wal_receiver` | WAL 수신자 통계 |
| `pg_stat_recovery_prefetch` | 복구 중 미리 가져온 블록 |
| `pg_stat_subscription` | 구독 워커 정보 |
| `pg_stat_ssl` | 연결당 SSL 사용 |
| `pg_stat_gssapi` | GSSAPI 인증/암호화 정보 |
| `pg_stat_progress_*` | ANALYZE, CREATE INDEX, VACUUM, CLUSTER, BASEBACKUP, COPY에 대한 진행 상황 보고 |

#### 표 27.2: 수집된 통계 뷰

| 뷰 이름 | 설명 |
|--------|-----|
| `pg_stat_archiver` | WAL 아카이버 프로세스 통계 |
| `pg_stat_bgwriter` | 백그라운드 라이터 활동 |
| `pg_stat_checkpointer` | 체크포인트 프로세스 활동 |
| `pg_stat_database` | 데이터베이스 전체 통계 |
| `pg_stat_database_conflicts` | 대기 복구로 인한 쿼리 취소 |
| `pg_stat_io` | 클러스터 전체 I/O 통계 |
| `pg_stat_replication_slots` | 복제 슬롯 사용 통계 |
| `pg_stat_slru` | SLRU 캐시 작업 |
| `pg_stat_subscription_stats` | 구독 오류 및 충돌 |
| `pg_stat_wal` | WAL 활동 통계 |
| `pg_stat_all_tables` / `pg_stat_sys_tables` / `pg_stat_user_tables` | 테이블 액세스 통계 |
| `pg_stat_xact_*` | 트랜잭션별 테이블 통계 |
| `pg_stat_all_indexes` / `pg_stat_sys_indexes` / `pg_stat_user_indexes` | 인덱스 액세스 통계 |
| `pg_stat_user_functions` / `pg_stat_xact_user_functions` | 함수 실행 통계 |
| `pg_statio_all_tables` / `pg_statio_sys_tables` / `pg_statio_user_tables` | 테이블 I/O 통계 |
| `pg_statio_all_indexes` / `pg_statio_sys_indexes` / `pg_statio_user_indexes` | 인덱스 I/O 통계 |
| `pg_statio_all_sequences` | 시퀀스 I/O 통계 |

### 27.2.3 pg_stat_activity

#### 표 27.3: pg_stat_activity 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `datid` | oid | 연결된 데이터베이스의 OID |
| `datname` | name | 연결된 데이터베이스 이름 |
| `pid` | integer | 백엔드의 프로세스 ID |
| `leader_pid` | integer | 병렬 그룹 리더의 PID (리더인 경우 NULL) |
| `usesysid` | oid | 로그인한 사용자의 OID |
| `usename` | name | 로그인한 사용자 이름 |
| `application_name` | text | 연결된 애플리케이션 이름 |
| `client_addr` | inet | 클라이언트 IP 주소 (유닉스 소켓의 경우 NULL) |
| `client_hostname` | text | 역방향 DNS 호스트 이름 (`log_hostname` 활성화 필요) |
| `client_port` | integer | 클라이언트 TCP 포트 (유닉스 소켓의 경우 -1) |
| `backend_start` | timestamp | 프로세스 시작 시간 |
| `xact_start` | timestamp | 현재 트랜잭션 시작 (활성 상태가 아니면 NULL) |
| `query_start` | timestamp | 활성 쿼리 시작 시간 |
| `state_change` | timestamp | 마지막 상태 변경 시간 |
| `wait_event_type` | text | 대기 이벤트 유형 (표 27.4 참조) |
| `wait_event` | text | 특정 대기 이벤트 이름 |
| `state` | text | 백엔드 상태 |
| `backend_xid` | xid | 최상위 트랜잭션 ID |
| `backend_xmin` | xid | 현재 백엔드의 xmin 수평선 |
| `query_id` | bigint | 쿼리 식별자 (`compute_query_id` 활성화 시) |
| `query` | text | 가장 최근 쿼리 텍스트 (기본적으로 1024바이트에서 잘림) |
| `backend_type` | text | 백엔드 유형 (autovacuum launcher/worker, logical replication, parallel worker, background writer, client backend, checkpointer, archiver 등) |

#### 백엔드 상태

- `starting`: 초기 시작/인증 단계
- `active`: 쿼리 실행 중
- `idle`: 클라이언트 명령 대기 중
- `idle in transaction`: 트랜잭션 내에서 쿼리 실행하지 않음
- `idle in transaction (aborted)`: 오류 상태의 트랜잭션
- `fastpath function call`: 빠른 경로 함수 실행 중
- `disabled`: `track_activities` 비활성화됨

#### 대기 이벤트 유형 (표 27.4)

| 유형 | 설명 |
|-----|-----|
| `Activity` | 주 처리 루프에서 유휴 대기 |
| `BufferPin` | 배타적 버퍼 액세스 대기 |
| `Client` | 클라이언트 소켓 활동 대기 |
| `Extension` | 확장 정의 대기 조건 |
| `InjectionPoint` | 테스트 주입 지점 조건 |
| `IO` | I/O 작업 완료 |
| `IPC` | 프로세스 간 통신 |
| `Lock` | 중량 잠금 대기 |
| `LWLock` | 경량 잠금 대기 |
| `Timeout` | 타임아웃 만료 |

#### Activity 대기 이벤트 예시 (표 27.5)

| 이벤트 | 설명 |
|-------|-----|
| `ArchiverMain` | 아카이버 프로세스 주 루프 |
| `AutovacuumMain` | Autovacuum 런처 주 루프 |
| `BgwriterMain` | 백그라운드 라이터 주 루프 |
| `CheckpointerMain` | 체크포인터 주 루프 |
| `WalReceiverMain` | WAL 수신자 주 루프 |
| `WalSenderMain` | WAL 발신자 주 루프 |
| `LogicalApplyMain` | 논리 복제 적용 주 루프 |

#### I/O 대기 이벤트 예시 (표 27.9)

| 이벤트 | 설명 |
|-------|-----|
| `DataFileRead` | 관계 데이터 파일 읽기 |
| `DataFileWrite` | 관계 데이터 파일 쓰기 |
| `DataFileExtend` | 관계 데이터 파일 확장 |
| `DataFileSync` | 관계 데이터 파일 fsync |
| `ControlFileRead` | pg_control 파일 읽기 |
| `ControlFileWrite` | pg_control 파일 쓰기 |
| `WalRead` | WAL 파일 읽기 |
| `WalWrite` | WAL 파일 쓰기 |
| `WalSync` | WAL 파일 fsync |

#### Lock 대기 이벤트 (표 27.11)

| 이벤트 | 설명 |
|-------|-----|
| `advisory` | 권고 사용자 잠금 |
| `extend` | 관계 확장 |
| `frozenid` | pg_database.datfrozenxid 업데이트 |
| `object` | 비관계 데이터베이스 객체 |
| `page` | 관계 페이지 잠금 |
| `relation` | 관계 잠금 |
| `transactionid` | 트랜잭션 완료 대기 |
| `tuple` | 튜플 잠금 |
| `virtualxid` | 가상 트랜잭션 ID 잠금 |

#### 대기 이벤트 세부 정보 쿼리 예시

```sql
SELECT pid, wait_event_type, wait_event
FROM pg_stat_activity
WHERE wait_event is NOT NULL;

-- 대기 이벤트 설명과 조인
SELECT a.pid, a.wait_event, w.description
FROM pg_stat_activity a
JOIN pg_wait_events w ON (a.wait_event_type = w.type
                          AND a.wait_event = w.name)
WHERE a.wait_event is NOT NULL
  AND a.state = 'active';
```

### 27.2.4 pg_stat_replication

연결된 각 대기 서버에 대한 통계를 표시합니다.

#### 표 27.14: pg_stat_replication 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `pid` | integer | WAL 발신자 프로세스 ID |
| `usesysid` | oid | 사용자 OID |
| `usename` | name | 사용자 이름 |
| `application_name` | text | 애플리케이션 이름 |
| `client_addr` | inet | 클라이언트 IP 주소 |
| `client_hostname` | text | 클라이언트 호스트 이름 |
| `client_port` | integer | 클라이언트 포트 |
| `backend_start` | timestamp | 연결 시작 시간 |
| `backend_xmin` | xid | 대기 서버의 xmin 수평선 |
| `state` | text | WAL 발신자 상태 (startup, catchup, streaming, backup, stopping) |
| `sent_lsn` | pg_lsn | 마지막으로 전송된 WAL 위치 |
| `write_lsn` | pg_lsn | 마지막으로 작성된 WAL 위치 (아직 플러시되지 않음) |
| `flush_lsn` | pg_lsn | 마지막으로 플러시된 WAL 위치 |
| `replay_lsn` | pg_lsn | 마지막으로 재생된 WAL 위치 |
| `write_lag` | interval | `synchronous_commit` 수준 `remote_write`에 대한 지연 |
| `flush_lag` | interval | `synchronous_commit` 수준 `on`에 대한 지연 |
| `replay_lag` | interval | `synchronous_commit` 수준 `remote_apply`에 대한 지연 |
| `sync_priority` | integer | 동기식 대기 선택의 우선 순위 |
| `sync_state` | text | 동기화 상태 (async, potential, sync, quorum) |
| `reply_time` | timestamp | 마지막 응답 메시지 시간 |

### 27.2.5 pg_stat_replication_slots

논리적 복제 슬롯 사용을 추적합니다.

#### 표 27.15: pg_stat_replication_slots 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `slot_name` | text | 클러스터 전체에서 고유한 슬롯 식별자 |
| `spill_txns` | bigint | 디스크로 스필된 트랜잭션 |
| `spill_count` | bigint | 스필 이벤트 수 |
| `spill_bytes` | bigint | 스필된 디코딩된 트랜잭션의 바이트 |
| `stream_txns` | bigint | 스트리밍된 진행 중인 트랜잭션 |
| `stream_count` | bigint | 스트림 이벤트 수 |
| `stream_bytes` | bigint | 스트리밍된 트랜잭션의 바이트 |
| `total_txns` | bigint | 전송된 총 디코딩된 트랜잭션 |
| `total_bytes` | bigint | 디코딩된 총 트랜잭션 데이터 |
| `stats_reset` | timestamp | 마지막 재설정 시간 |

### 27.2.6 pg_stat_wal_receiver

대기 서버의 WAL 수신자 통계입니다.

#### 표 27.16: pg_stat_wal_receiver 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `pid` | integer | WAL 수신자 프로세스 ID |
| `status` | text | 활동 상태 |
| `receive_start_lsn` | pg_lsn | 수신자가 시작할 때 첫 번째 WAL 위치 |
| `receive_start_tli` | integer | 시작 타임라인 번호 |
| `written_lsn` | pg_lsn | 마지막으로 작성된 (플러시되지 않은) WAL 위치 |
| `flushed_lsn` | pg_lsn | 마지막으로 플러시된 WAL 위치 |
| `received_tli` | integer | 마지막으로 플러시된 WAL 위치의 타임라인 |
| `last_msg_send_time` | timestamp | 발신자로부터 마지막 메시지 전송 시간 |
| `last_msg_receipt_time` | timestamp | 마지막 메시지 수신 시간 |
| `latest_end_lsn` | pg_lsn | 발신자에게 보고된 마지막 WAL 위치 |
| `latest_end_time` | timestamp | 마지막 보고된 WAL 위치 시간 |
| `slot_name` | text | 복제 슬롯 이름 |
| `sender_host` | text | 주 서버 호스트/경로 |
| `sender_port` | integer | 주 서버 포트 |
| `conninfo` | text | 연결 문자열 (보안에 민감한 필드는 난독화됨) |

### 27.2.7 pg_stat_recovery_prefetch

복구 미리 가져오기 통계입니다 (단일 행).

#### 표 27.17: pg_stat_recovery_prefetch 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `stats_reset` | timestamp | 마지막 재설정 시간 |
| `prefetch` | bigint | 미리 가져온 블록 (버퍼 풀에 없음) |
| `hit` | bigint | 이미 버퍼 풀에 있는 블록 |
| `skip_init` | bigint | 미리 가져오지 않은 0으로 초기화된 블록 |
| `skip_new` | bigint | 미리 가져오지 않은 존재하지 않는 블록 |
| `skip_fpw` | bigint | 미리 가져오지 않은 전체 페이지 이미지가 있는 블록 |
| `skip_rep` | bigint | 미리 가져오지 않은 최근에 미리 가져온 블록 |
| `wal_distance` | int | 미리 가져오기가 앞서 보고 있는 바이트 |
| `block_distance` | int | 미리 가져오기가 앞서 보고 있는 블록 |
| `io_depth` | int | 완료되지 않은 것으로 알려진 비행 중인 미리 가져오기 |

### 27.2.8 pg_stat_subscription

#### 표 27.18: pg_stat_subscription 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `subid` | oid | 구독 OID |
| `subname` | name | 구독 이름 |
| `worker_type` | text | 워커 유형 (apply, parallel apply, table synchronization) |
| `pid` | integer | 워커 프로세스 ID |
| `leader_pid` | integer | 리더 적용 워커 PID (리더 또는 동기화 워커인 경우 NULL) |
| `relid` | oid | 동기화 중인 관계 OID (적용 워커의 경우 NULL) |
| `received_lsn` | pg_lsn | 마지막으로 수신된 WAL 위치 (병렬 적용의 경우 NULL) |
| `last_msg_send_time` | timestamp | 마지막 발신자 메시지 전송 시간 |
| `last_msg_receipt_time` | timestamp | 마지막 발신자 메시지 수신 시간 |
| `latest_end_lsn` | pg_lsn | 발신자에게 보고된 마지막 WAL 위치 |
| `latest_end_time` | timestamp | 마지막 보고된 위치 시간 |

### 27.2.9 pg_stat_subscription_stats

구독 오류 및 충돌 통계입니다.

#### 표 27.19: pg_stat_subscription_stats 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `subid` | oid | 구독 OID |
| `subname` | name | 구독 이름 |
| `apply_error_count` | bigint | 적용 오류 (충돌 카운터에도 계산됨) |
| `sync_error_count` | bigint | 초기 동기화 오류 |
| `confl_insert_exists` | bigint | 삽입 시 고유 제약 조건 위반 |
| `confl_update_origin_differs` | bigint | 이전에 수정된 행에 대한 업데이트 |
| `confl_update_exists` | bigint | 업데이트 시 고유 제약 조건 위반 |
| `confl_update_missing` | bigint | 업데이트 대상 행을 찾을 수 없음 |
| `confl_delete_origin_differs` | bigint | 이전에 수정된 행의 삭제 |
| `confl_delete_missing` | bigint | 삭제 대상 행을 찾을 수 없음 |
| `confl_multiple_unique_conflicts` | bigint | 여러 고유 제약 조건 위반 |
| `stats_reset` | timestamp | 마지막 재설정 시간 |

### 27.2.10 pg_stat_ssl

연결당 SSL 사용 통계입니다.

#### 표 27.20: pg_stat_ssl 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `pid` | integer | 백엔드/WAL 발신자 프로세스 ID |
| `ssl` | boolean | SSL 사용 중 |
| `version` | text | SSL 버전 |
| `cipher` | text | SSL 암호 이름 |
| `bits` | integer | 암호화 알고리즘 비트 |
| `client_dn` | text | 클라이언트 인증서 DN (64자로 잘림) |
| `client_serial` | numeric | 클라이언트 인증서 일련 번호 |
| `issuer_dn` | text | 발급자 DN (64자로 잘림) |

### 27.2.11 pg_stat_gssapi

GSSAPI 인증/암호화 통계입니다.

#### 표 27.21: pg_stat_gssapi 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `pid` | integer | 백엔드 프로세스 ID |
| `gss_authenticated` | boolean | GSSAPI 인증 사용됨 |
| `principal` | text | 사용된 주체 (64자로 잘림) |
| `encrypted` | boolean | GSSAPI 암호화 사용 중 |
| `credentials_delegated` | boolean | 자격 증명 위임됨 |

### 27.2.12 pg_stat_archiver

WAL 아카이버 프로세스 통계입니다 (단일 행).

#### 표 27.22: pg_stat_archiver 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `archived_count` | bigint | 성공적으로 아카이빙된 WAL 파일 |
| `last_archived_wal` | text | 가장 최근에 성공적으로 아카이빙된 파일 |
| `last_archived_time` | timestamp | 가장 최근 성공적인 아카이브 시간 |
| `failed_count` | bigint | 실패한 아카이빙 시도 |
| `last_failed_wal` | text | 가장 최근 실패한 아카이빙 파일 |
| `last_failed_time` | timestamp | 가장 최근 실패 시간 |
| `stats_reset` | timestamp | 마지막 재설정 시간 |

참고: WAL 파일이 순서대로 아카이빙되는 것이 보장되지 않으므로 `last_archived_wal`보다 오래된 모든 파일이 아카이빙되었다고 가정할 수 없습니다.

### 27.2.13 pg_stat_io

백엔드 유형, 객체 및 컨텍스트별 클러스터 전체 I/O 통계입니다.

#### 표 27.23: pg_stat_io 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `backend_type` | text | 백엔드 유형 (background worker, autovacuum 등) |
| `object` | text | 대상 객체 (relation, temp relation, wal) |
| `context` | text | I/O 컨텍스트 (normal, init, vacuum, bulkread, bulkwrite) |
| `reads` | bigint | 읽기 작업 수 |
| `read_bytes` | numeric | 총 읽은 바이트 |
| `read_time` | double | 읽기 대기 시간 (ms, `track_io_timing` 활성화 시) |
| `writes` | bigint | 쓰기 작업 수 |
| `write_bytes` | numeric | 총 쓴 바이트 |
| `write_time` | double | 쓰기 대기 시간 (ms) |
| `writebacks` | bigint | 쓰기 되돌림 단위 (8kB 블록) |
| `writeback_time` | double | 쓰기 되돌림 대기 시간 (ms) |
| `extends` | bigint | 관계 확장 작업 |
| `extend_bytes` | numeric | 확장 작업의 바이트 |
| `extend_time` | double | 확장 대기 시간 (ms) |
| `hits` | bigint | 버퍼에서 찾은 원하는 블록 |
| `evictions` | bigint | 버퍼에서 제거된 블록 |
| `reuses` | bigint | 링 버퍼에서 재사용된 버퍼 |
| `fsyncs` | bigint | fsync() 호출 |
| `fsync_time` | double | fsync 대기 시간 (ms) |
| `stats_reset` | timestamp | 마지막 재설정 시간 |

#### I/O 컨텍스트

- normal: 기본 I/O 컨텍스트 (공유 버퍼)
- init: WAL 세그먼트 생성
- vacuum: VACUUM/ANALYZE 중 공유 버퍼 외부 I/O
- bulkread: 공유 버퍼 외부의 대규모 순차 읽기
- bulkwrite: 공유 버퍼 외부의 대규모 쓰기 (예: COPY)

#### 튜닝 인사이트

- 높은 `evictions`: 공유 버퍼 증가
- 클라이언트의 높은 `fsyncs`: 잘못 구성된 공유 버퍼 또는 체크포인터
- 클라이언트의 높은 `writes`: 잘못 구성된 버퍼 또는 체크포인터

### 27.2.14 pg_stat_bgwriter

백그라운드 라이터 통계입니다 (단일 행).

#### 표 27.24: pg_stat_bgwriter 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `buffers_clean` | bigint | 백그라운드 라이터가 쓴 버퍼 |
| `maxwritten_clean` | bigint | 너무 많은 쓰기로 인해 중지된 청소 스캔 |
| `buffers_alloc` | bigint | 할당된 버퍼 |
| `stats_reset` | timestamp | 마지막 재설정 시간 |

### 27.2.15 pg_stat_checkpointer

체크포인트 프로세스 통계입니다 (단일 행).

#### 표 27.25: pg_stat_checkpointer 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `num_timed` | bigint | 예약된 체크포인트 (완료 + 건너뜀) |
| `num_requested` | bigint | 요청된 체크포인트 (완료 + 건너뜀) |
| `num_done` | bigint | 완료된 체크포인트 |
| `restartpoints_timed` | bigint | 예약된 재시작점 (완료 + 건너뜀) |
| `restartpoints_req` | bigint | 요청된 재시작점 (완료 + 건너뜀) |
| `restartpoints_done` | bigint | 완료된 재시작점 |
| `write_time` | double | 파일 쓰기 시간 (ms) |
| `sync_time` | double | 파일 동기화 시간 (ms) |
| `buffers_written` | bigint | 쓴 공유 버퍼 |
| `slru_written` | bigint | 쓴 SLRU 버퍼 |
| `stats_reset` | timestamp | 마지막 재설정 시간 |

### 27.2.16 pg_stat_wal

WAL 활동 통계입니다 (단일 행).

#### 표 27.26: pg_stat_wal 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `wal_records` | bigint | 생성된 총 WAL 레코드 |
| `wal_fpi` | bigint | 생성된 총 전체 페이지 이미지 |
| `wal_bytes` | numeric | 생성된 총 WAL 바이트 |
| `wal_buffers_full` | bigint | 버퍼가 가득 차서 WAL을 쓴 횟수 |
| `stats_reset` | timestamp | 마지막 재설정 시간 |

### 27.2.17 pg_stat_database

데이터베이스 전체 통계입니다 (데이터베이스당 하나의 행 + 공유 객체에 대해 하나).

#### 표 27.27: pg_stat_database 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `datid` | oid | 데이터베이스 OID (공유 객체의 경우 0) |
| `datname` | name | 데이터베이스 이름 (공유 객체의 경우 NULL) |
| `numbackends` | integer | 현재 연결된 백엔드 (현재 상태 전용 컬럼) |
| `xact_commit` | bigint | 커밋된 트랜잭션 |
| `xact_rollback` | bigint | 롤백된 트랜잭션 |
| `blks_read` | bigint | 읽은 디스크 블록 |
| `blks_hit` | bigint | 버퍼 캐시 히트 |
| `tup_returned` | bigint | 순차/인덱스 스캔에서 라이브 행 |
| `tup_fetched` | bigint | 인덱스 스캔에서 라이브 행 |
| `tup_inserted` | bigint | 삽입된 행 |
| `tup_updated` | bigint | 업데이트된 행 (HOT 및 newpage 업데이트 포함) |
| `tup_deleted` | bigint | 삭제된 행 |
| `conflicts` | bigint | 복구 충돌 (대기 전용) |
| `temp_files` | bigint | 생성된 임시 파일 |
| `temp_bytes` | bigint | 쓴 임시 파일 데이터 |
| `deadlocks` | bigint | 감지된 교착 상태 |
| `checksum_failures` | bigint | 데이터 페이지 체크섬 실패 (비활성화된 경우 NULL) |
| `checksum_last_failure` | timestamp | 마지막 체크섬 실패 시간 |
| `blk_read_time` | double | 데이터 파일 읽기 시간 (ms, `track_io_timing` 활성화 시) |
| `blk_write_time` | double | 데이터 파일 쓰기 시간 (ms) |
| `session_time` | double | 총 세션 시간 (ms) |
| `active_time` | double | 활성 SQL 실행 시간 (ms) |
| `idle_in_transaction_time` | double | 트랜잭션 내 유휴 시간 (ms) |
| `sessions` | bigint | 설정된 총 세션 |
| `sessions_abandoned` | bigint | 클라이언트 연결 끊김으로 인해 손실된 세션 |
| `sessions_fatal` | bigint | 치명적 오류로 종료된 세션 |
| `sessions_killed` | bigint | 운영자에 의해 종료된 세션 |
| `parallel_workers_to_launch` | bigint | 계획된 병렬 워커 |
| `parallel_workers_launched` | bigint | 시작된 병렬 워커 |
| `stats_reset` | timestamp | 마지막 재설정 시간 |

### 27.2.18 pg_stat_database_conflicts

대기 복구 충돌 통계입니다 (데이터베이스당 하나의 행, 대기 전용).

#### 표 27.28: pg_stat_database_conflicts 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `datid` | oid | 데이터베이스 OID |
| `datname` | name | 데이터베이스 이름 |
| `confl_tablespace` | bigint | 삭제된 테이블스페이스에서 취소된 쿼리 |
| `confl_lock` | bigint | 잠금 타임아웃에서 취소된 쿼리 |
| `confl_snapshot` | bigint | 오래된 스냅샷에서 취소된 쿼리 |
| `confl_bufferpin` | bigint | 고정된 버퍼에서 취소된 쿼리 |
| `confl_deadlock` | bigint | 교착 상태에서 취소된 쿼리 |
| `confl_active_logicalslot` | bigint | 오래된 스냅샷/낮은 wal_level에서 취소된 논리 슬롯 사용 |

### 27.2.19 pg_stat_all_tables

테이블별 액세스 통계입니다 (TOAST 포함 테이블당 하나의 행).

#### 표 27.29: pg_stat_all_tables 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `relid` | oid | 테이블 OID |
| `schemaname` | name | 스키마 이름 |
| `relname` | name | 테이블 이름 |
| `seq_scan` | bigint | 시작된 순차 스캔 |
| `last_seq_scan` | timestamp | 마지막 순차 스캔 시간 |
| `seq_tup_read` | bigint | 순차 스캔으로 가져온 라이브 행 |
| `idx_scan` | bigint | 시작된 인덱스 스캔 |
| `last_idx_scan` | timestamp | 마지막 인덱스 스캔 시간 |
| `idx_tup_fetch` | bigint | 인덱스 스캔으로 가져온 라이브 행 |
| `n_tup_ins` | bigint | 삽입된 행 |
| `n_tup_upd` | bigint | 업데이트된 행 (HOT 및 newpage 업데이트 포함) |
| `n_tup_del` | bigint | 삭제된 행 |
| `n_tup_hot_upd` | bigint | HOT 업데이트된 행 (인덱스 업데이트 필요 없음) |
| `n_tup_newpage_upd` | bigint | 새 힙 페이지로 이동된 업데이트 행 |
| `n_live_tup` | bigint | 추정 라이브 행 |
| `n_dead_tup` | bigint | 추정 데드 행 |
| `n_mod_since_analyze` | bigint | ANALYZE 이후 수정된 추정 행 |
| `n_ins_since_vacuum` | bigint | VACUUM 이후 삽입된 추정 행 |
| `last_vacuum` | timestamp | 마지막 수동 VACUUM (FULL 아님) |
| `last_autovacuum` | timestamp | autovacuum 데몬의 마지막 VACUUM |
| `last_analyze` | timestamp | 마지막 수동 ANALYZE |
| `last_autoanalyze` | timestamp | autovacuum 데몬의 마지막 ANALYZE |
| `vacuum_count` | bigint | 수동 VACUUM 횟수 |
| `autovacuum_count` | bigint | Autovacuum 데몬 횟수 |
| `analyze_count` | bigint | 수동 ANALYZE 횟수 |
| `autoanalyze_count` | bigint | Autovacuum ANALYZE 횟수 |
| `total_vacuum_time` | double | 총 수동 vacuum 시간 (ms, 대기 시간 포함) |
| `total_autovacuum_time` | double | 총 autovacuum 시간 (ms) |
| `total_analyze_time` | double | 총 수동 analyze 시간 (ms) |
| `total_autoanalyze_time` | double | 총 autovacuum analyze 시간 (ms) |

관련 뷰:
- `pg_stat_user_tables`: 사용자 테이블만
- `pg_stat_sys_tables`: 시스템 테이블만
- `pg_stat_xact_all_tables`: 현재 트랜잭션 활동 (데드 행, vacuum 또는 analyze 컬럼 없음)
- `pg_stat_xact_user_tables`: 현재 트랜잭션의 사용자 테이블
- `pg_stat_xact_sys_tables`: 현재 트랜잭션의 시스템 테이블

### 27.2.20 pg_stat_all_indexes

인덱스별 액세스 통계입니다 (인덱스당 하나의 행).

#### 표 27.30: pg_stat_all_indexes 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `relid` | oid | 테이블 OID |
| `indexrelid` | oid | 인덱스 OID |
| `schemaname` | name | 스키마 이름 |
| `relname` | name | 테이블 이름 |
| `indexrelname` | name | 인덱스 이름 |
| `idx_scan` | bigint | 시작된 인덱스 스캔 |
| `last_idx_scan` | timestamp | 마지막 인덱스 스캔 시간 |
| `idx_tup_read` | bigint | 스캔에서 반환된 인덱스 항목 |
| `idx_tup_fetch` | bigint | 단순 인덱스 스캔으로 가져온 라이브 테이블 행 |

#### 중요 참고사항

- 비트맵 스캔: 인덱스에 대해 `idx_tup_read`를 증가시키지만 `idx_tup_fetch`는 증가시키지 않음 (테이블에서 계산됨)
- 옵티마이저: 상수 값 검증을 위해 인덱스에 액세스할 수 있음
- `idx_tup_read` >= `idx_tup_fetch` (데드 행, 커밋되지 않은 행, 인덱스 전용 스캔으로 인해)
- 인덱스 스캔은 실행자 노드 실행을 초과할 수 있음 (다중 값 검색, 스킵 스캔)

관련 뷰:
- `pg_stat_user_indexes`: 사용자 테이블 인덱스만
- `pg_stat_sys_indexes`: 시스템 테이블 인덱스만

### 27.2.21 pg_statio_all_tables

테이블 I/O 통계입니다 (TOAST 포함 테이블당 하나의 행).

#### 표 27.31: pg_statio_all_tables 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `relid` | oid | 테이블 OID |
| `schemaname` | name | 스키마 이름 |
| `relname` | name | 테이블 이름 |
| `heap_blks_read` | bigint | 읽은 디스크 블록 |
| `heap_blks_hit` | bigint | 버퍼 히트 |
| `idx_blks_read` | bigint | 읽은 인덱스 디스크 블록 |
| `idx_blks_hit` | bigint | 인덱스 버퍼 히트 |
| `toast_blks_read` | bigint | 읽은 TOAST 테이블 디스크 블록 |
| `toast_blks_hit` | bigint | TOAST 테이블 버퍼 히트 |
| `tidx_blks_read` | bigint | 읽은 TOAST 인덱스 디스크 블록 |
| `tidx_blks_hit` | bigint | TOAST 인덱스 버퍼 히트 |

관련 뷰:
- `pg_statio_user_tables`: 사용자 테이블만
- `pg_statio_sys_tables`: 시스템 테이블만

### 27.2.22 pg_statio_all_indexes

인덱스 I/O 통계입니다 (인덱스당 하나의 행).

#### 표 27.32: pg_statio_all_indexes 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `relid` | oid | 테이블 OID |
| `indexrelid` | oid | 인덱스 OID |
| `schemaname` | name | 스키마 이름 |
| `relname` | name | 테이블 이름 |
| `indexrelname` | name | 인덱스 이름 |
| `idx_blks_read` | bigint | 읽은 디스크 블록 |
| `idx_blks_hit` | bigint | 버퍼 히트 |

관련 뷰:
- `pg_statio_user_indexes`: 사용자 테이블 인덱스만
- `pg_statio_sys_indexes`: 시스템 테이블 인덱스만

### 27.2.23 pg_statio_all_sequences

시퀀스 I/O 통계입니다 (시퀀스당 하나의 행).

#### 표 27.33: pg_statio_all_sequences 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `relid` | oid | 시퀀스 OID |
| `schemaname` | name | 스키마 이름 |
| `relname` | name | 시퀀스 이름 |
| `blks_read` | bigint | 읽은 디스크 블록 |
| `blks_hit` | bigint | 버퍼 히트 |

관련 뷰:
- `pg_statio_user_sequences`: 사용자 시퀀스만
- `pg_statio_sys_sequences`: 시스템 시퀀스만 (현재 항상 비어 있음)

### 27.2.24 pg_stat_user_functions

사용자 정의 함수 실행 통계입니다 (`track_functions` 매개변수에 의해 제어됨).

#### 표 27.34: pg_stat_user_functions 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `funcid` | oid | 함수 OID |
| `schemaname` | name | 스키마 이름 |
| `funcname` | name | 함수 이름 |
| `calls` | bigint | 함수 호출 횟수 |
| `total_time` | double | 함수 및 호출자의 총 시간 (ms) |
| `self_time` | double | 함수 자체의 시간 (ms) |

관련 뷰:
- `pg_stat_xact_user_functions`: 현재 트랜잭션 함수 호출만

### 27.2.25 pg_stat_slru

SLRU (Simple Least-Recently-Used) 캐시 통계입니다.

#### 표 27.35: pg_stat_slru 컬럼

| 컬럼 | 유형 | 설명 |
|-----|-----|-----|
| `name` | text | SLRU 캐시 이름 |
| `blks_zeroed` | bigint | 초기화 중 0으로 설정된 블록 |
| `blks_hit` | bigint | 캐시 히트 (OS 캐시 포함하지 않음) |
| `blks_read` | bigint | 읽은 디스크 블록 |
| `blks_written` | bigint | 쓴 디스크 블록 |
| `blks_exists` | bigint | 존재 확인된 블록 |
| `flushes` | bigint | 더티 데이터 플러시 |
| `truncates` | bigint | 캐시 잘라내기 |
| `stats_reset` | timestamp | 마지막 재설정 시간 |

#### 핵심 SLRU 캐시

- `commit_timestamp`: 커밋 타임스탬프 추적
- `multixact_member`: Multixact 멤버 정보
- `multixact_offset`: Multixact 오프셋 정보
- `notify`: NOTIFY 메시지 저장
- `serializable`: 직렬화 가능 트랜잭션 충돌
- `subtransaction`: 하위 트랜잭션 정보
- `transaction`: 트랜잭션 상태

### 27.2.26 통계 함수

#### 표 27.36: 추가 통계 함수

```sql
-- 스냅샷 및 백엔드 정보
pg_backend_pid() → integer
  -- 현재 세션 백엔드 PID 반환

pg_stat_get_snapshot_timestamp() → timestamp with time zone
  -- 현재 통계 스냅샷 타임스탬프 반환 (또는 NULL)

pg_stat_clear_snapshot() → void
  -- 현재 트랜잭션의 통계 스냅샷 삭제

-- 재설정 함수 (슈퍼유저 제한)
pg_stat_reset() → void
  -- 현재 데이터베이스의 모든 카운터 재설정
  -- 경고: autovacuum 타이밍도 재설정됨

pg_stat_reset_shared([target text DEFAULT NULL]) → void
  -- 클러스터 전체 카운터 재설정
  -- target: 'archiver', 'bgwriter', 'checkpointer', 'io',
  --         'recovery_prefetch', 'slru', 'wal', 또는 NULL (전체)

pg_stat_reset_single_table_counters(oid) → void
  -- 단일 테이블/인덱스의 카운터 재설정

pg_stat_reset_backend_stats(integer) → void
  -- 백엔드 PID의 카운터 재설정

pg_stat_reset_single_function_counters(oid) → void
  -- 단일 함수의 카운터 재설정

pg_stat_reset_slru([target text DEFAULT NULL]) → void
  -- SLRU 통계 재설정
  -- target: 'commit_timestamp', 'multixact_member', 'multixact_offset',
  --         'notify', 'serializable', 'subtransaction', 'transaction',
  --         'other', 또는 NULL (전체)

pg_stat_reset_replication_slot(text) → void
  -- 복제 슬롯 통계 재설정 (전체의 경우 NULL)

pg_stat_reset_subscription_stats(oid) → void
  -- 구독 통계 재설정 (전체의 경우 NULL)

-- I/O 및 활동 통계
pg_stat_get_backend_io(integer) → setof record
  -- 백엔드 PID의 I/O 통계 (pg_stat_io와 동일한 컬럼)

pg_stat_get_activity(integer) → setof record
  -- 백엔드 PID의 활동 레코드 (전체의 경우 NULL)

pg_stat_get_backend_wal(integer) → record
  -- 백엔드 PID의 WAL 통계

pg_stat_get_xact_blocks_fetched(oid) → bigint
  -- 현재 트랜잭션의 블록 읽기 요청

pg_stat_get_xact_blocks_hit(oid) → bigint
  -- 캐시에서 충족된 블록 읽기 요청 (현재 트랜잭션)
```

#### 표 27.37: 백엔드별 통계 함수

```sql
pg_stat_get_backend_activity(integer) → text
  -- 가장 최근 쿼리 텍스트

pg_stat_get_backend_activity_start(integer) → timestamp with time zone
  -- 가장 최근 쿼리 시작 시간

pg_stat_get_backend_client_addr(integer) → inet
  -- 클라이언트 IP 주소

pg_stat_get_backend_client_port(integer) → integer
  -- 클라이언트 TCP 포트 번호

pg_stat_get_backend_dbid(integer) → oid
  -- 연결된 데이터베이스 OID

pg_stat_get_backend_idset() → setof integer
  -- 활성 백엔드 ID 집합 (반복에 유용)

pg_stat_get_backend_pid(integer) → integer
  -- 백엔드의 프로세스 ID

pg_stat_get_backend_start(integer) → timestamp with time zone
  -- 프로세스 시작 시간

pg_stat_get_backend_subxact(integer) → record
  -- 하위 트랜잭션 정보
  -- 반환: subxact_count, subxact_overflow

pg_stat_get_backend_userid(integer) → oid
  -- 로그인한 사용자의 OID

pg_stat_get_backend_wait_event(integer) → text
  -- 대기 이벤트 이름 (대기하지 않는 경우 NULL)

pg_stat_get_backend_wait_event_type(integer) → text
  -- 대기 이벤트 유형 (대기하지 않는 경우 NULL)

pg_stat_get_backend_xact_start(integer) → timestamp with time zone
  -- 현재 트랜잭션 시작 시간
```

#### 예시: 모든 활성 백엔드의 PID 및 쿼리 가져오기

```sql
SELECT pg_stat_get_backend_pid(backendid) AS pid,
       pg_stat_get_backend_activity(backendid) AS query
FROM pg_stat_get_backend_idset() AS backendid;
```

#### 중요 경고

`pg_stat_reset()` 사용 시 autovacuum 타이밍 카운터가 재설정되어 필요한 유지 관리가 방지될 수 있습니다. 권장 사항 : 통계 재설정 후 데이터베이스 전체 `ANALYZE` 실행.

---

## 27.3 잠금 보기

`pg_locks` 시스템 뷰는 잠금 관리자의 미해결 잠금에 대한 정보를 보기 위한 유용한 도구입니다.

### 주요 기능

`pg_locks` 시스템 테이블을 통해 데이터베이스 관리자는 다음을 수행할 수 있습니다:

1. 잠금 정보 보기:
   - 모든 미해결 잠금
   - 특정 데이터베이스의 관계에 대한 모든 잠금
   - 특정 관계에 대한 모든 잠금
   - 특정 PostgreSQL 세션이 보유한 모든 잠금

2. 경합 원인 식별:
   - 데이터베이스 클라이언트 간 경합의 잠재적 원인인 가장 많은 비승인 잠금을 가진 관계 결정

3. 성능 영향 분석:
   - 전체 데이터베이스 성능에 대한 잠금 경합의 영향 결정
   - 전체 데이터베이스 트래픽에 따라 경합이 어떻게 변하는지 평가

### 관련 문서

- `pg_locks` 뷰 세부 정보: 섹션 53.13
- 잠금 및 동시성 관리: 13장 (동시성 제어)

---

## 27.4 진행 상황 보고

PostgreSQL은 여러 장기 실행 명령에 대한 진행 상황 보고 기능을 제공합니다. 진행 상황 보고를 지원하는 명령은 다음과 같습니다:

- `ANALYZE`
- `CLUSTER`
- `CREATE INDEX`
- `VACUUM`
- `COPY`
- `BASE_BACKUP` (복제 명령)

진행 상황 정보는 명령 실행에 대한 실시간 지표를 표시하는 전용 시스템 뷰를 통해 액세스됩니다.

### 27.4.1 ANALYZE 진행 상황 보고

뷰: `pg_stat_progress_analyze`

`ANALYZE`가 실행 중일 때 이 뷰는 명령을 실행하는 백엔드당 하나의 행을 포함합니다.

#### 주요 컬럼

- `pid` - 프로세스 ID
- `datid`, `datname` - 데이터베이스 OID 및 이름
- `relid` - 분석 중인 테이블의 OID
- `phase` - 현재 처리 단계
- `sample_blks_total`, `sample_blks_scanned` - 힙 블록 샘플링 진행 상황
- `ext_stats_total`, `ext_stats_computed` - 확장 통계 진행 상황
- `child_tables_total`, `child_tables_done` - 자식 테이블 진행 상황
- `current_child_table_relid` - 현재 스캔 중인 자식 테이블의 OID
- `delay_time` - 비용 기반 지연의 총 대기 시간 (밀리초)

#### ANALYZE 단계

| 단계 | 설명 |
|-----|-----|
| `initializing` | 힙 스캔 시작 준비 중 (짧음) |
| `acquiring sample rows` | 샘플 행에 대해 테이블 스캔 중 |
| `acquiring inherited sample rows` | 샘플 행에 대해 자식 테이블 스캔 중 |
| `computing statistics` | 샘플에서 통계 계산 중 |
| `computing extended statistics` | 확장 통계 계산 중 |
| `finalizing analyze` | `pg_class` 업데이트 중 |

참고: `ANALYZE`가 `ONLY` 없이 파티션된 테이블에서 실행되면 모든 파티션이 재귀적으로 분석됩니다. 진행 상황은 먼저 부모 테이블에 대해, 그 다음 각 파티션에 대해 보고됩니다.

### 27.4.2 CLUSTER 진행 상황 보고

뷰: `pg_stat_progress_cluster`

이 뷰는 테이블을 다시 작성하는 `CLUSTER` 및 `VACUUM FULL` 작업에 대한 진행 상황 정보를 포함합니다.

#### 주요 컬럼

- `pid` - 프로세스 ID
- `datid`, `datname` - 데이터베이스 OID 및 이름
- `relid` - 클러스터링 중인 테이블의 OID
- `command` - `CLUSTER` 또는 `VACUUM FULL`
- `phase` - 현재 처리 단계
- `cluster_index_relid` - 스캔에 사용된 인덱스의 OID (있는 경우)
- `heap_tuples_scanned`, `heap_tuples_written` - 튜플 진행 상황
- `heap_blks_total`, `heap_blks_scanned` - 블록 진행 상황
- `index_rebuild_count` - 다시 빌드된 인덱스 수

#### CLUSTER 및 VACUUM FULL 단계

| 단계 | 설명 |
|-----|-----|
| `initializing` | 힙 스캔 준비 중 |
| `seq scanning heap` | 테이블 순차 스캔 중 |
| `index scanning heap` | 테이블 인덱스 스캔 중 (CLUSTER만) |
| `sorting tuples` | 튜플 정렬 중 (CLUSTER만) |
| `writing new heap` | 새 힙 작성 중 |
| `swapping relation files` | 새로 빌드된 파일을 제자리에 교체 중 |
| `rebuilding index` | 인덱스 다시 빌드 중 |
| `performing final cleanup` | 최종 정리 작업 |

### 27.4.3 COPY 진행 상황 보고

뷰: `pg_stat_progress_copy`

이 뷰는 `COPY` 명령의 진행 상황을 추적합니다.

#### 주요 컬럼

- `pid` - 프로세스 ID
- `datid`, `datname` - 데이터베이스 OID 및 이름
- `relid` - 테이블의 OID (SELECT 쿼리의 경우 0)
- `command` - `COPY FROM` 또는 `COPY TO`
- `type` - I/O 유형: `FILE`, `PROGRAM`, `PIPE`, 또는 `CALLBACK`
- `bytes_processed`, `bytes_total` - 바이트 진행 상황 (사용할 수 없는 경우 0)
- `tuples_processed` - 처리된 튜플
- `tuples_excluded` - WHERE 절로 제외된 튜플
- `tuples_skipped` - 잘못된 데이터로 인해 건너뛴 튜플 (`ON_ERROR` 사용 시)

### 27.4.4 CREATE INDEX 진행 상황 보고

뷰: `pg_stat_progress_create_index`

`CREATE INDEX` 및 `REINDEX` 작업의 진행 상황을 추적합니다.

#### 주요 컬럼

- `pid` - 프로세스 ID
- `datid`, `datname` - 데이터베이스 OID 및 이름
- `relid` - 테이블의 OID
- `index_relid` - 인덱스의 OID (비동시 CREATE INDEX 중 0)
- `command` - `CREATE INDEX`, `CREATE INDEX CONCURRENTLY`, `REINDEX`, 또는 `REINDEX CONCURRENTLY`
- `phase` - 현재 처리 단계
- `lockers_total`, `lockers_done`, `current_locker_pid` - 잠금 대기 진행 상황
- `blocks_total`, `blocks_done` - 블록 진행 상황
- `tuples_total`, `tuples_done` - 튜플 진행 상황
- `partitions_total`, `partitions_done` - 파티션 진행 상황

#### CREATE INDEX 단계

| 단계 | 설명 |
|-----|-----|
| `initializing` | 인덱스 생성 준비 중 |
| `waiting for writers before build` | 쓰기 잠금 대기 중 (CONCURRENTLY만) |
| `building index` | 액세스 방법별 코드로 인덱스 빌드 중 |
| `waiting for writers before validation` | 쓰기 잠금 대기 중 (CONCURRENTLY만) |
| `index validation: scanning index` | 검증할 튜플에 대해 인덱스 스캔 중 |
| `index validation: sorting tuples` | 인덱스 출력 정렬 중 |
| `index validation: scanning table` | 인덱스 튜플을 검증하기 위해 테이블 스캔 중 |
| `waiting for old snapshots` | 오래된 스냅샷 대기 중 (CONCURRENTLY만) |
| `waiting for readers before marking dead` | 읽기 잠금 대기 중 (REINDEX CONCURRENTLY만) |
| `waiting for readers before dropping` | 삭제 전 읽기 잠금 대기 중 (REINDEX CONCURRENTLY만) |

### 27.4.5 VACUUM 진행 상황 보고

뷰: `pg_stat_progress_vacuum`

일반 `VACUUM` 작업의 진행 상황을 추적합니다 (`VACUUM FULL`은 `pg_stat_progress_cluster`를 사용하므로 제외).

#### 주요 컬럼

- `pid` - 프로세스 ID
- `datid`, `datname` - 데이터베이스 OID 및 이름
- `relid` - vacuum 중인 테이블의 OID
- `phase` - 현재 처리 단계
- `heap_blks_total` - 총 힙 블록 (스캔 시작 시점 기준)
- `heap_blks_scanned` - 스캔된 힙 블록
- `heap_blks_vacuumed` - vacuum된 힙 블록
- `index_vacuum_count` - 완료된 인덱스 vacuum 주기 수
- `max_dead_tuple_bytes` - 인덱스 vacuum 전 최대 데드 튜플 데이터
- `dead_tuple_bytes` - 마지막 인덱스 vacuum 이후 수집된 데드 튜플 데이터
- `num_dead_item_ids` - 수집된 데드 아이템 식별자
- `indexes_total`, `indexes_processed` - 인덱스 진행 상황
- `delay_time` - 비용 기반 지연의 총 대기 시간

#### VACUUM 단계

| 단계 | 설명 |
|-----|-----|
| `initializing` | 힙 스캔 준비 중 |
| `scanning heap` | 힙 스캔, 정리, 조각 모음 및 동결 중 |
| `vacuuming indexes` | 인덱스 vacuum 중 (힙이 완전히 스캔된 후 발생) |
| `vacuuming heap` | 힙 vacuum 중 (인덱스 vacuum 후) |
| `cleaning up indexes` | 인덱스 정리 중 |
| `truncating heap` | OS에 빈 페이지를 반환하기 위해 힙 잘라내기 중 |
| `performing final cleanup` | 여유 공간 맵 및 통계를 포함한 최종 정리 |

### 27.4.6 Base Backup 진행 상황 보고

뷰: `pg_stat_progress_basebackup`

`pg_basebackup`에서 사용하는 `BASE_BACKUP` 복제 명령의 진행 상황을 추적합니다.

#### 주요 컬럼

- `pid` - WAL 발신자 프로세스의 프로세스 ID
- `phase` - 현재 처리 단계
- `backup_total` - 스트리밍할 총 데이터 (추정, `--no-estimate-size`인 경우 NULL)
- `backup_streamed` - 스트리밍된 데이터 양
- `tablespaces_total` - 스트리밍할 총 테이블스페이스
- `tablespaces_streamed` - 스트리밍된 테이블스페이스 수

#### Base Backup 단계

| 단계 | 설명 |
|-----|-----|
| `initializing` | 백업 시작 준비 중 |
| `waiting for checkpoint to finish` | 백업 시작 체크포인트 대기 중 |
| `estimating backup size` | 총 데이터베이스 파일 크기 추정 중 |
| `streaming database files` | 데이터베이스 파일 스트리밍 중 |
| `waiting for wal archiving to finish` | WAL 아카이빙 대기 중 (`pg_backup_stop` 중) |
| `transferring wal files` | WAL 로그 전송 중 (`--wal-method=fetch` 사용 시) |

#### 사용 예시

VACUUM 진행 상황 모니터링:

```sql
SELECT pid, datname, relid, phase, heap_blks_scanned, heap_blks_total
FROM pg_stat_progress_vacuum;
```

CREATE INDEX 진행 상황 모니터링:

```sql
SELECT pid, command, phase, blocks_done, blocks_total
FROM pg_stat_progress_create_index;
```

---

## 27.5 동적 추적

PostgreSQL은 코드의 특정 지점에서 외부 유틸리티를 호출하여 실행을 추적할 수 있는 동적 추적 기능을 제공합니다. 기본적으로 프로브는 PostgreSQL에 컴파일되지 않으며, 사용자는 구성 중에 명시적으로 활성화해야 합니다.

지원 도구:
- DTrace (Solaris, macOS, FreeBSD, NetBSD, Oracle Linux)
- SystemTap (Linux 동등물)

### 27.5.1 동적 추적을 위한 컴파일

DTrace 지원을 활성화하려면 configure 스크립트를 실행할 때 `--enable-dtrace` 플래그를 지정합니다:

```bash
./configure --enable-dtrace
```

이렇게 하면 컴파일된 PostgreSQL 바이너리에서 프로브를 사용할 수 있습니다.

### 27.5.2 내장 프로브

PostgreSQL에는 범주별로 구성된 수많은 표준 프로브가 포함되어 있습니다.

#### 트랜잭션 프로브

| 프로브 | 매개변수 | 설명 |
|-------|---------|-----|
| `transaction-start` | `(LocalTransactionId)` | 새 트랜잭션 시작 시 발화. arg0은 트랜잭션 ID. |
| `transaction-commit` | `(LocalTransactionId)` | 트랜잭션이 성공적으로 완료되면 발화. arg0은 트랜잭션 ID. |
| `transaction-abort` | `(LocalTransactionId)` | 트랜잭션이 실패로 완료되면 발화. arg0은 트랜잭션 ID. |

#### 쿼리 프로브

| 프로브 | 매개변수 | 설명 |
|-------|---------|-----|
| `query-start` | `(const char *)` | 쿼리 처리 시작 시 발화. arg0은 쿼리 문자열. |
| `query-done` | `(const char *)` | 쿼리 처리 완료 시 발화. arg0은 쿼리 문자열. |
| `query-parse-start` | `(const char *)` | 쿼리 파싱 시작 시 발화. arg0은 쿼리 문자열. |
| `query-parse-done` | `(const char *)` | 쿼리 파싱 완료 시 발화. arg0은 쿼리 문자열. |
| `query-rewrite-start` | `(const char *)` | 쿼리 재작성 시작 시 발화. arg0은 쿼리 문자열. |
| `query-rewrite-done` | `(const char *)` | 쿼리 재작성 완료 시 발화. arg0은 쿼리 문자열. |
| `query-plan-start` | `()` | 쿼리 계획 시작 시 발화. |
| `query-plan-done` | `()` | 쿼리 계획 완료 시 발화. |
| `query-execute-start` | `()` | 쿼리 실행 시작 시 발화. |
| `query-execute-done` | `()` | 쿼리 실행 완료 시 발화. |

#### 명령문 상태

| 프로브 | 매개변수 | 설명 |
|-------|---------|-----|
| `statement-status` | `(const char *)` | 서버가 `pg_stat_activity`.`status`를 업데이트할 때 발화. arg0은 새 상태 문자열. |

#### 체크포인트 프로브

| 프로브 | 매개변수 | 설명 |
|-------|---------|-----|
| `checkpoint-start` | `(int)` | 체크포인트 시작 시 발화. arg0은 비트 플래그 (shutdown, immediate, force). |
| `checkpoint-done` | `(int, int, int, int, int)` | 체크포인트 완료 시 발화. arg0: 쓴 버퍼, arg1: 총 버퍼, arg2-arg4: 추가/제거/재활용된 WAL 파일. |
| `clog-checkpoint-start` | `(bool)` | CLOG 체크포인트 부분 시작 시 발화. arg0: 정상이면 true, 종료이면 false. |
| `clog-checkpoint-done` | `(bool)` | CLOG 체크포인트 부분 완료 시 발화. |
| `subtrans-checkpoint-start` | `(bool)` | SUBTRANS 체크포인트 부분 시작 시 발화. |
| `subtrans-checkpoint-done` | `(bool)` | SUBTRANS 체크포인트 부분 완료 시 발화. |
| `multixact-checkpoint-start` | `(bool)` | MultiXact 체크포인트 부분 시작 시 발화. |
| `multixact-checkpoint-done` | `(bool)` | MultiXact 체크포인트 부분 완료 시 발화. |
| `buffer-checkpoint-start` | `(int)` | 버퍼 쓰기 부분 시작 시 발화. arg0: 비트 플래그. |
| `buffer-sync-start` | `(int, int)` | 더티 버퍼 쓰기 시작 시 발화. arg0: 총 버퍼, arg1: 더티 버퍼. |
| `buffer-sync-written` | `(int)` | 각 버퍼 쓰기 후 발화. arg0: 버퍼 ID. |
| `buffer-sync-done` | `(int, int, int)` | 모든 더티 버퍼 쓰기 완료 시 발화. arg0: 총, arg1: 쓴, arg2: 예상. |
| `buffer-checkpoint-sync-start` | `()` | 더티 버퍼 쓰기 후, fsync 요청 전 발화. |
| `buffer-checkpoint-done` | `()` | 디스크로 버퍼 동기화 완료 시 발화. |
| `twophase-checkpoint-start` | `()` | 2단계 체크포인트 부분 시작 시 발화. |
| `twophase-checkpoint-done` | `()` | 2단계 체크포인트 부분 완료 시 발화. |

#### 버퍼 프로브

| 프로브 | 매개변수 | 설명 |
|-------|---------|-----|
| `buffer-extend-start` | `(ForkNumber, BlockNumber, Oid, Oid, Oid, int, unsigned int)` | 관계 확장 시작 시 발화. arg0: 포크, arg1: 블록, arg2-4: 테이블스페이스/데이터베이스/관계 OID, arg5: 백엔드 ID 또는 -1, arg6: 요청된 블록. |
| `buffer-extend-done` | `(ForkNumber, BlockNumber, Oid, Oid, Oid, int, unsigned int, BlockNumber)` | 관계 확장 완료 시 발화. 추가 arg6: 실제 확장된 블록, arg7: 첫 번째 새 블록 번호. |
| `buffer-read-start` | `(ForkNumber, BlockNumber, Oid, Oid, Oid, int)` | 버퍼 읽기 시작 시 발화. |
| `buffer-read-done` | `(ForkNumber, BlockNumber, Oid, Oid, Oid, int, bool)` | 버퍼 읽기 완료 시 발화. arg6: 풀에서 찾으면 true, 그렇지 않으면 false. |
| `buffer-flush-start` | `(ForkNumber, BlockNumber, Oid, Oid, Oid)` | 공유 버퍼에 대한 쓰기 요청 발행 전 발화. |
| `buffer-flush-done` | `(ForkNumber, BlockNumber, Oid, Oid, Oid)` | 쓰기 요청 완료 시 발화 (커널 수준, 디스크 수준 아님). |

#### WAL 프로브

| 프로브 | 매개변수 | 설명 |
|-------|---------|-----|
| `wal-buffer-write-dirty-start` | `()` | 서버가 더티 WAL 버퍼 쓰기 시작 시 발화 (`wal_buffers`가 너무 작을 수 있음을 나타냄). |
| `wal-buffer-write-dirty-done` | `()` | 더티 WAL 버퍼 쓰기 완료 시 발화. |
| `wal-insert` | `(unsigned char, unsigned char)` | WAL 레코드 삽입 시 발화. arg0: 리소스 관리자 (rmid), arg1: 정보 플래그. |
| `wal-switch` | `()` | WAL 세그먼트 전환 요청 시 발화. |

#### 스토리지 관리자 프로브

| 프로브 | 매개변수 | 설명 |
|-------|---------|-----|
| `smgr-md-read-start` | `(ForkNumber, BlockNumber, Oid, Oid, Oid, int)` | 관계에서 블록 읽기 시작 시 발화. |
| `smgr-md-read-done` | `(ForkNumber, BlockNumber, Oid, Oid, Oid, int, int, int)` | 블록 읽기 완료 시 발화. arg6: 읽은 바이트, arg7: 요청된 바이트. |
| `smgr-md-write-start` | `(ForkNumber, BlockNumber, Oid, Oid, Oid, int)` | 관계에 블록 쓰기 시작 시 발화. |
| `smgr-md-write-done` | `(ForkNumber, BlockNumber, Oid, Oid, Oid, int, int, int)` | 블록 쓰기 완료 시 발화. arg6: 쓴 바이트, arg7: 요청된 바이트. |

#### 정렬 프로브

| 프로브 | 매개변수 | 설명 |
|-------|---------|-----|
| `sort-start` | `(int, bool, int, int, bool, int)` | 정렬 작업 시작 시 발화. arg0: heap/index/datum, arg1: 고유 적용, arg2: 키 컬럼, arg3: 작업 메모리 (KB), arg4: 랜덤 액세스 필요, arg5: 0=serial/1=worker/2=leader. |
| `sort-done` | `(bool, long)` | 정렬 완료 시 발화. arg0: 외부이면 true/내부이면 false, arg1: 블록 (외부) 또는 KB (내부). |

#### 잠금 프로브

| 프로브 | 매개변수 | 설명 |
|-------|---------|-----|
| `lwlock-acquire` | `(char *, LWLockMode)` | LWLock 획득 시 발화. arg0: tranche, arg1: 잠금 모드 (배타적/공유). |
| `lwlock-release` | `(char *)` | LWLock 해제 시 발화. arg0: tranche. |
| `lwlock-wait-start` | `(char *, LWLockMode)` | LWLock 사용 불가, 프로세스 대기 시 발화. arg0: tranche, arg1: 잠금 모드. |
| `lwlock-wait-done` | `(char *, LWLockMode)` | LWLock 대기에서 프로세스 해제 시 발화. arg0: tranche, arg1: 잠금 모드. |
| `lwlock-condacquire` | `(char *, LWLockMode)` | 대기 없이 LWLock 성공적으로 획득 시 발화. |
| `lwlock-condacquire-fail` | `(char *, LWLockMode)` | 대기 없이 LWLock 획득 실패 시 발화. |
| `lock-wait-start` | `(unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, LOCKMODE)` | 중량 잠금 (lmgr) 대기 시작 시 발화. arg0-3: 태그 필드, arg4: 객체 유형, arg5: 잠금 유형. |
| `lock-wait-done` | `(unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, LOCKMODE)` | 중량 잠금 대기 완료 시 발화 (잠금 획득). 인수는 `lock-wait-start`와 동일. |

#### 교착 상태 프로브

| 프로브 | 매개변수 | 설명 |
|-------|---------|-----|
| `deadlock-found` | `()` | 교착 상태 감지기가 교착 상태를 발견할 때 발화. |

#### 정의된 유형

| 유형 | 정의 |
|-----|-----|
| `LocalTransactionId` | `unsigned int` |
| `LWLockMode` | `int` |
| `LOCKMODE` | `int` |
| `BlockNumber` | `unsigned int` |
| `Oid` | `unsigned int` |
| `ForkNumber` | `int` |
| `bool` | `unsigned char` |

### 27.5.3 프로브 사용하기

#### DTrace 예시 스크립트

다음 DTrace 스크립트는 트랜잭션 수를 분석합니다:

```dtrace
#!/usr/sbin/dtrace -qs

postgresql$1:::transaction-start
{
      @start["Start"] = count();
      self->ts  = timestamp;
}

postgresql$1:::transaction-abort
{
      @abort["Abort"] = count();
}

postgresql$1:::transaction-commit
/self->ts/
{
      @commit["Commit"] = count();
      @time["Total time (ns)"] = sum(timestamp - self->ts);
      self->ts=0;
}
```

#### 스크립트 실행

```bash
./txn_count.d `pgrep -n postgres` or ./txn_count.d <PID>
^C

Start                                          71
Commit                                         70
Total time (ns)                        2312105013
```

#### SystemTap 표기법

SystemTap은 DTrace와 다른 표기법을 사용합니다. 중요: SystemTap 스크립트는 하이픈 대신 이중 밑줄 을 사용하여 프로브 이름을 참조해야 합니다 (예: `transaction-start` 대신 `transaction__start`). 이것은 향후 SystemTap 릴리스에서 수정될 예정입니다.

#### 모범 사례

- DTrace 스크립트는 신중하게 작성하고 디버그해야 함; 계측 오류가 흔함
- 동적 추적 결과를 논의할 때는 항상 사용된 스크립트를 포함하고 확인
- 비용이 많이 드는 작업을 보호하려면 `ENABLED()` 매크로 사용

### 27.5.4 새 프로브 정의하기

#### 새 프로브 추가 단계

1. 프로브 이름 및 매개변수 결정

2. `src/backend/utils/probes.d`에 프로브 정의 추가

3. `pg_trace.h` 포함하고 원하는 위치에 `TRACE_POSTGRESQL` 매크로 삽입

4. 재컴파일하고 확인

#### 예시: 트랜잭션 프로브 추가

1단계: 프로브 이름을 `transaction-start`로, 매개변수를 `LocalTransactionId`로 결정합니다.

2단계: `src/backend/utils/probes.d`에 추가:

```
probe transaction__start(LocalTransactionId);
```

정의에서 이중 밑줄에 주의하세요.

3단계: `pg_trace.h`를 포함하고 매크로 호출 추가:

```c
TRACE_POSTGRESQL_TRANSACTION_START(vxid.localTransactionId);
```

매크로 이름은 단일 밑줄을 사용하며 다음과 같이 프로브 정의에서 파생됩니다:
- 이중 밑줄을 단일 밑줄로 변환
- 대문자로 변환
- `TRACE_POSTGRESQL_` 접두사 추가

4단계: 재컴파일 후 프로브 확인:

```bash
dtrace -ln transaction-start
   ID    PROVIDER          MODULE           FUNCTION NAME
18705 postgresql49878     postgres     StartTransactionCommand transaction-start
18755 postgresql49877     postgres     StartTransactionCommand transaction-start
18805 postgresql49876     postgres     StartTransactionCommand transaction-start
18855 postgresql49875     postgres     StartTransactionCommand transaction-start
18986 postgresql49873     postgres     StartTransactionCommand transaction-start
```

#### 중요 고려사항

1. 유형 일치
- 프로브 매개변수에 지정된 데이터 유형은 매크로의 변수 유형과 일치해야 함
- 유형 불일치는 컴파일 오류를 발생시킴

2. 성능
- 대부분의 플랫폼에서 `--enable-dtrace`를 사용하면 추적이 비활성화되어 있어도 추적 매크로 인수가 평가됨
- 비용이 많이 드는 함수 호출의 경우 `ENABLED()` 매크로로 보호:

```c
if (TRACE_POSTGRESQL_TRANSACTION_START_ENABLED())
    TRACE_POSTGRESQL_TRANSACTION_START(some_function(...));
```

각 추적 매크로에는 해당하는 `ENABLED()` 변형이 있습니다.

3. 최소 오버헤드
- 로컬 변수 값 보고는 최소 오버헤드 발생
- 보호 없이 매크로 인수에서 비용이 많이 드는 함수 호출 피하기

---

## 27.6 디스크 사용량 모니터링

이 섹션에서는 PostgreSQL 데이터베이스 시스템에서 디스크 사용량을 모니터링하는 방법에 대해 설명합니다.

### 27.6.1 디스크 사용량 확인

#### 저장소 구조

- 각 테이블에는 데이터 저장을 위한 기본 힙 디스크 파일이 있음
- 넓은 값 컬럼이 있는 테이블은 메인 테이블에 비해 너무 큰 값을 위한 연관된 TOAST 파일을 가질 수 있음
- TOAST 테이블이 있으면 하나의 유효한 인덱스가 존재
- 기본 테이블에는 연관된 인덱스가 있을 수 있음
- 각 테이블과 인덱스는 별도의 디스크 파일에 저장됨 (1기가바이트를 초과할 수 있음)

#### 모니터링 방법

세 가지 접근법 사용 가능:

1. SQL 함수 (권장) - 표 9.102 데이터베이스 객체 크기 함수에 나열됨
2. oid2name 모듈
3. 수동 시스템 카탈로그 검사

#### 쿼리 예시

테이블 디스크 사용량 확인:

```sql
SELECT pg_relation_filepath(oid), relpages FROM pg_class WHERE relname = 'customer';
```

결과 예시:

```
 pg_relation_filepath | relpages
----------------------+----------
 base/16384/16806     |       60
(1 row)
```

참고: 각 페이지는 일반적으로 8킬로바이트입니다. `relpages` 값은 `VACUUM`, `ANALYZE` 및 `CREATE INDEX`와 같은 특정 DDL 명령에 의해서만 업데이트됩니다.

TOAST 테이블 공간 사용량 표시:

```sql
SELECT relname, relpages
FROM pg_class,
     (SELECT reltoastrelid
      FROM pg_class
      WHERE relname = 'customer') AS ss
WHERE oid = ss.reltoastrelid OR
      oid = (SELECT indexrelid
             FROM pg_index
             WHERE indrelid = ss.reltoastrelid)
ORDER BY relname;
```

인덱스 크기 표시:

```sql
SELECT c2.relname, c2.relpages
FROM pg_class c, pg_class c2, pg_index i
WHERE c.relname = 'customer' AND
      c.oid = i.indrelid AND
      c2.oid = i.indexrelid
ORDER BY c2.relname;
```

가장 큰 테이블과 인덱스 찾기:

```sql
SELECT relname, relpages
FROM pg_class
ORDER BY relpages DESC;
```

### 27.6.2 디스크 가득 참 장애

#### 주요 고려사항

- 가득 찬 데이터 디스크는 데이터 손상을 일으키지 않지만 유용한 데이터베이스 활동을 방지할 수 있음
- WAL (Write-Ahead Log) 디스크가 가득 차면 데이터베이스 서버가 패닉하고 종료될 수 있음
- 일부 파일 시스템은 거의 가득 찼을 때 성능이 저하됨 - 완전히 가득 차기 전에 조치 취하기

#### 해결책

1. 불필요한 파일 삭제 로 공간 확보
2. 테이블스페이스 사용 으로 데이터베이스 파일을 다른 파일 시스템으로 이동 (섹션 22.6 참조)
3. 시스템이 사용자별 할당량을 지원하는 경우 디스크 할당량 모니터링 - 할당량 초과는 디스크 공간 부족과 동일한 효과

---

## 주요 구성 매개변수 요약

| 매개변수 | 기본값 | 용도 |
|---------|-------|-----|
| `track_activities` | on | 현재 명령 추적 활성화 |
| `track_counts` | on | 테이블/인덱스 통계 수집 |
| `track_functions` | none | 함수 호출 추적 (off, pl, all) |
| `track_io_timing` | off | I/O 타이밍 측정 활성화 |
| `track_wal_io_timing` | off | WAL I/O 타이밍 활성화 |
| `track_cost_delay_timing` | off | vacuum 지연 타이밍 추적 |
| `track_activity_query_size` | 1024 | 쿼리 텍스트 잘라내기 길이 (바이트) |
| `stats_fetch_consistency` | cache | 스냅샷/캐시 모드 (snapshot/cache/none) |
| `PGSTAT_MIN_INTERVAL` | 1000 | 최소 플러시 간격 (밀리초) |

---

## 요약

1. 누적 통계 는 테이블/인덱스 액세스, 함수 호출 및 일반 데이터베이스 활동을 추적
2. 동적 뷰 는 실시간 세션 및 프로세스 정보를 표시 (누적 통계와 독립적)
3. 구성 매개변수 는 수집되는 통계를 제어 (트레이드오프: 오버헤드 vs 세부 정보)
4. 보안 모델: 일반 사용자는 자신의 세션만 볼 수 있음; 슈퍼유저는 모든 것을 볼 수 있음
5. 캐싱 동작: 통계는 트랜잭션 끝까지 캐시됨 (비용이 많이 드는 쿼리로 왜곡 발생 가능)
6. 재설정 함수: 통계 재설정 (하지만 autovacuum 카운터도 재설정됨 - 이후 ANALYZE 실행)
7. I/O 추적: OS 유틸리티와 결합하여 완전한 I/O 성능 그림 제공
8. 대기 이벤트: 자세한 대기 분류는 성능 병목 현상 식별에 도움
