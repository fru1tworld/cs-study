# PostgreSQL 내부 함수와 시스템 관리 함수 (Internal Functions and System Administration Functions)

이 문서는 PostgreSQL 공식 문서의 시스템 관리 함수(System Administration Functions)와 시스템 정보 함수(System Information Functions)를 한국어로 번역한 것입니다.

## 개요

PostgreSQL은 데이터베이스 서버의 관리, 모니터링, 백업, 복제 등을 위한 다양한 시스템 함수를 제공합니다. 이러한 함수들은 DBA(Database Administrator)와 시스템 운영자가 PostgreSQL 클러스터를 효과적으로 관리하는 데 필수적입니다.

---

# 1. 서버 신호 함수 (Server Signaling Functions)

서버 신호 함수는 다른 서버 프로세스의 동작을 제어하는 데 사용됩니다. 모든 함수는 `boolean`을 반환하며, 신호 전송 성공 시 `true`, 실패 시 `false`를 반환합니다.

## pg_cancel_backend()

```sql
pg_cancel_backend(pid integer) → boolean
```

설명: 지정된 백엔드 프로세스의 현재 쿼리를 취소합니다.

권한: 해당 역할의 멤버이거나 `pg_signal_backend` 권한을 가진 사용자만 사용 가능. 슈퍼유저 백엔드는 슈퍼유저만 취소 가능.

예제:
```sql
-- PID가 12345인 프로세스의 쿼리 취소
SELECT pg_cancel_backend(12345);

-- 특정 사용자의 모든 쿼리 취소
SELECT pg_cancel_backend(pid)
FROM pg_stat_activity
WHERE usename = 'slow_user' AND state = 'active';
```

## pg_terminate_backend()

```sql
pg_terminate_backend(pid integer, timeout bigint DEFAULT 0) → boolean
```

설명: 지정된 백엔드 프로세스의 세션을 종료합니다.

매개변수:
- `pid`: 종료할 프로세스 ID
- `timeout`: 대기 시간(밀리초). 0이면 즉시 반환(기본값). 양수이면 프로세스 종료 또는 타임아웃까지 대기

반환값: 성공 시 `true`, 타임아웃 시 `false`

예제:
```sql
-- 즉시 종료 요청 (결과 대기 안 함)
SELECT pg_terminate_backend(12345);

-- 5초 대기하며 종료
SELECT pg_terminate_backend(12345, 5000);

-- 1시간 이상 실행 중인 쿼리 종료
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'active'
  AND query_start < now() - interval '1 hour';
```

## pg_log_backend_memory_contexts()

```sql
pg_log_backend_memory_contexts(pid integer) → boolean
```

설명: 지정된 백엔드의 메모리 컨텍스트 정보를 서버 로그에 기록하도록 요청합니다.

권한: 슈퍼유저만 사용 가능

예제:
```sql
-- 특정 백엔드의 메모리 사용량 로깅
SELECT pg_log_backend_memory_contexts(pg_backend_pid());
```

## pg_reload_conf()

```sql
pg_reload_conf() → boolean
```

설명: 모든 PostgreSQL 프로세스가 설정 파일을 다시 로드하도록 합니다. postmaster에 SIGHUP 신호를 보내고, postmaster는 자식 프로세스들에게 전달합니다.

참고: 리로드 전에 `pg_file_settings`, `pg_hba_file_rules`, `pg_ident_file_mappings` 뷰로 설정 파일을 확인하는 것이 좋습니다.

예제:
```sql
-- 설정 파일 변경 후 리로드
SELECT pg_reload_conf();

-- 리로드 전 설정 확인
SELECT * FROM pg_file_settings WHERE error IS NOT NULL;
```

## pg_rotate_logfile()

```sql
pg_rotate_logfile() → boolean
```

설명: 로그 파일 관리자에게 새 출력 파일로 전환하도록 신호를 보냅니다.

참고: 내장 로그 수집기(built-in log collector)가 실행 중일 때만 작동합니다.

예제:
```sql
-- 로그 파일 즉시 교체
SELECT pg_rotate_logfile();
```

---

# 2. 백업 제어 함수 (Backup Control Functions)

백업 제어 함수는 온라인 백업 수행에 필요합니다. 복구(recovery) 중에는 실행할 수 없습니다(일부 제외).

## pg_backup_start()

```sql
pg_backup_start(label text [, fast boolean]) → pg_lsn
```

설명: 서버를 온라인 백업 수행 준비 상태로 만듭니다.

매개변수:
- `label`: 사용자 정의 백업 레이블 (일반적으로 백업 덤프 파일명)
- `fast`: `true`이면 즉시 체크포인트 실행 (I/O 스파이크 발생 가능)

반환값: WAL 위치 (pg_lsn)

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- 일반 백업 시작
SELECT pg_backup_start('daily_backup_2024');

-- 빠른 백업 시작 (즉시 체크포인트)
SELECT pg_backup_start('emergency_backup', true);
```

## pg_backup_stop()

```sql
pg_backup_stop([wait_for_archive boolean]) → record
  (lsn pg_lsn, labelfile text, spcmapfile text)
```

설명: 온라인 백업을 완료합니다.

매개변수:
- `wait_for_archive`: `true`(기본값)이면 WAL 아카이빙 완료까지 대기. `false`이면 즉시 반환 (권장하지 않음)

반환값:
- `lsn`: 백업 종료 LSN
- `labelfile`: 백업 레이블 파일 내용
- `spcmapfile`: 테이블스페이스 맵 파일 내용

중요: 반환된 파일 내용은 반드시 백업 영역에 기록해야 합니다. 라이브 데이터 디렉터리에 기록하면 안 됩니다.

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- 백업 종료 및 결과 확인
SELECT * FROM pg_backup_stop();

-- 백업 레이블 파일만 가져오기
SELECT labelfile FROM pg_backup_stop();
```

## pg_create_restore_point()

```sql
pg_create_restore_point(name text) → pg_lsn
```

설명: WAL에 복구 대상으로 사용할 수 있는 명명된 마커를 생성합니다.

매개변수:
- `name`: 복구 지점 이름 (`recovery_target_name` 매개변수와 함께 사용)

주의: 중복 이름 사용을 피하세요. 복구는 첫 번째 일치 지점에서 중단됩니다.

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- 복구 지점 생성
SELECT pg_create_restore_point('before_schema_change');
SELECT pg_create_restore_point('after_data_migration');
```

## WAL 위치 함수

### pg_current_wal_lsn()

```sql
pg_current_wal_lsn() → pg_lsn
```

설명: 현재 WAL 쓰기 위치를 반환합니다. 아카이빙에 가장 적합합니다.

예제:
```sql
SELECT pg_current_wal_lsn();
-- 결과: 0/1A2B3C4D
```

### pg_current_wal_insert_lsn()

```sql
pg_current_wal_insert_lsn() → pg_lsn
```

설명: 현재 WAL 삽입 위치(논리적 끝)를 반환합니다. 주로 디버깅용입니다.

### pg_current_wal_flush_lsn()

```sql
pg_current_wal_flush_lsn() → pg_lsn
```

설명: 영구 저장소에 기록된 것으로 알려진 마지막 WAL 위치를 반환합니다.

### pg_switch_wal()

```sql
pg_switch_wal() → pg_lsn
```

설명: 아카이빙을 위해 새 WAL 파일로 강제 전환합니다.

반환값: 완료된 파일의 끝 위치 + 1. 마지막 전환 이후 WAL 활동이 없으면 아무것도 반환하지 않습니다.

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- WAL 파일 강제 전환
SELECT pg_switch_wal();
```

### pg_walfile_name()

```sql
pg_walfile_name(lsn pg_lsn) → text
```

설명: WAL 위치를 WAL 파일명으로 변환합니다.

예제:
```sql
SELECT pg_walfile_name('0/1A2B3C4D');
-- 결과: 000000010000000000000001
```

### pg_walfile_name_offset()

```sql
pg_walfile_name_offset(lsn pg_lsn) → record
  (file_name text, file_offset integer)
```

설명: LSN을 파일명과 파일 내 바이트 오프셋으로 변환합니다.

예제:
```sql
SELECT * FROM pg_walfile_name_offset(pg_current_wal_lsn());
--        file_name         | file_offset
-- --------------------------+-------------
--  00000001000000000000000D |     4039624
```

### pg_split_walfile_name()

```sql
pg_split_walfile_name(file_name text) → record
  (segment_number numeric, timeline_id bigint)
```

설명: WAL 파일명에서 시퀀스 번호와 타임라인 ID를 추출합니다.

### pg_wal_lsn_diff()

```sql
pg_wal_lsn_diff(lsn1 pg_lsn, lsn2 pg_lsn) → numeric
```

설명: 두 WAL 위치 간의 바이트 차이를 계산합니다 (`lsn1 - lsn2`).

용도: 복제 지연(replication lag) 계산에 유용합니다.

예제:
```sql
-- 복제 지연 바이트 계산
SELECT pg_wal_lsn_diff(
    pg_current_wal_lsn(),
    replay_lsn
) AS replication_lag_bytes
FROM pg_stat_replication;
```

---

# 3. 복구 제어 함수 (Recovery Control Functions)

복구 제어 함수는 복구(recovery) 상태를 확인하고 제어하는 데 사용됩니다.

## 복구 정보 함수 (Recovery Information Functions)

복구 중이나 정상 실행 중 모두 사용 가능합니다.

### pg_is_in_recovery()

```sql
pg_is_in_recovery() → boolean
```

설명: 복구가 아직 진행 중인지 여부를 반환합니다.

예제:
```sql
SELECT pg_is_in_recovery();
-- Primary: false
-- Standby: true
```

### pg_last_wal_receive_lsn()

```sql
pg_last_wal_receive_lsn() → pg_lsn
```

설명: 스트리밍 복제로 수신 및 동기화된 마지막 WAL 위치를 반환합니다.

반환값: 스트리밍 비활성화 또는 미시작 시 `NULL`

### pg_last_wal_replay_lsn()

```sql
pg_last_wal_replay_lsn() → pg_lsn
```

설명: 복구 중 재생된 마지막 WAL 위치를 반환합니다.

반환값: 복구 없이 정상 시작된 경우 `NULL`

### pg_last_xact_replay_timestamp()

```sql
pg_last_xact_replay_timestamp() → timestamp with time zone
```

설명: 복구 중 재생된 마지막 트랜잭션의 타임스탬프를 반환합니다.

참고: 프라이머리에서 커밋/중단 WAL 레코드가 생성된 시간입니다.

예제:
```sql
-- 복제 지연 시간 확인
SELECT now() - pg_last_xact_replay_timestamp() AS replication_lag;
```

### pg_get_wal_resource_managers()

```sql
pg_get_wal_resource_managers() → setof record
  (rm_id integer, rm_name text, rm_builtin boolean)
```

설명: 현재 로드된 WAL 리소스 관리자 목록을 반환합니다.

반환 필드:
- `rm_id`: 리소스 관리자 ID
- `rm_name`: 리소스 관리자 이름
- `rm_builtin`: 내장 여부 (`true`: 내장, `false`: 확장으로 로드됨)

## 복구 제어 함수 (Recovery Control Functions)

복구 중에만 실행 가능합니다.

### pg_is_wal_replay_paused()

```sql
pg_is_wal_replay_paused() → boolean
```

설명: 복구 일시정지가 요청되었는지 여부를 반환합니다.

### pg_get_wal_replay_pause_state()

```sql
pg_get_wal_replay_pause_state() → text
```

설명: 현재 복구 일시정지 상태를 반환합니다.

반환값:
- `not paused`: 일시정지 요청 없음
- `pause requested`: 일시정지 요청됨, 아직 일시정지 안 됨
- `paused`: 복구가 실제로 일시정지됨

### pg_wal_replay_pause()

```sql
pg_wal_replay_pause() → void
```

설명: 복구 일시정지를 요청합니다.

참고: 일시정지 중에는 데이터베이스 변경이 적용되지 않으며, 쿼리는 일관된 스냅샷을 봅니다.

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- 복구 일시정지
SELECT pg_wal_replay_pause();

-- 상태 확인
SELECT pg_get_wal_replay_pause_state();
```

### pg_wal_replay_resume()

```sql
pg_wal_replay_resume() → void
```

설명: 일시정지된 복구를 재개합니다.

권한: 슈퍼유저 (권한 부여 가능)

### pg_promote()

```sql
pg_promote([wait boolean DEFAULT true, wait_seconds integer DEFAULT 60]) → boolean
```

설명: 스탠바이를 프라이머리로 승격합니다.

매개변수:
- `wait`: `true`이면 승격 완료 또는 타임아웃까지 대기 (기본값). `false`이면 SIGUSR1 신호 전송 후 즉시 반환
- `wait_seconds`: 대기 시간(초). 기본값 60초

반환값: 승격 성공 시 `true`, 타임아웃 시 `false`

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- 스탠바이 승격 (60초 대기)
SELECT pg_promote();

-- 즉시 승격 요청 (대기 안 함)
SELECT pg_promote(false);

-- 120초 대기하며 승격
SELECT pg_promote(true, 120);
```

주의사항:
- 승격 중에는 `pg_wal_replay_pause()` 또는 `pg_wal_replay_resume()` 실행 불가
- 일시정지 중 승격이 트리거되면 일시정지 상태 종료
- 스트리밍이 활성화된 상태에서 무기한 일시정지하면 디스크 공간이 가득 찰 수 있음

---

# 4. 스냅샷 동기화 함수 (Snapshot Synchronization Functions)

스냅샷 동기화 함수는 여러 세션 간에 동일한 데이터베이스 뷰를 공유하는 데 사용됩니다.

## pg_export_snapshot()

```sql
pg_export_snapshot() → text
```

설명: 트랜잭션의 현재 스냅샷을 저장하고 식별 문자열을 반환합니다.

용도: 반환된 문자열을 다른 클라이언트에 전달하여 동일한 스냅샷을 가져올 수 있습니다.

주의사항:
- 내보내는 트랜잭션이 종료될 때까지만 유효
- 트랜잭션당 여러 번 내보내기 가능 (`READ COMMITTED`에서만 유용)
- 스냅샷 내보내기 후 `PREPARE TRANSACTION` 사용 불가
- 동기화된 트랜잭션은 기존 데이터에 대해 동일한 뷰를 가지지만, 서로의 미커밋 변경 사항은 볼 수 없음

예제:
```sql
-- 세션 1: 스냅샷 내보내기
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT pg_export_snapshot();
-- 결과: '00000003-0000001B-1'

-- 세션 2: 스냅샷 가져오기
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SET TRANSACTION SNAPSHOT '00000003-0000001B-1';
-- 이제 세션 1과 동일한 데이터를 볼 수 있음
```

## pg_log_standby_snapshot()

```sql
pg_log_standby_snapshot() → pg_lsn
```

설명: 실행 중인 트랜잭션의 스냅샷을 WAL에 기록합니다.

용도: 스탠바이에서의 논리적 디코딩(logical decoding)에 유용합니다. bgwriter나 checkpointer를 기다리지 않아도 됩니다.

---

# 5. 복제 관리 함수 (Replication Management Functions)

복제 관리 함수는 복제 슬롯, 논리적 디코딩, 복제 원점(replication origin)을 관리합니다.

## 5.1 물리적 복제 슬롯 (Physical Replication Slots)

### pg_create_physical_replication_slot()

```sql
pg_create_physical_replication_slot(
  slot_name name,
  [immediately_reserve boolean, temporary boolean]
) → record (slot_name name, lsn pg_lsn)
```

설명: 새 물리적 복제 슬롯을 생성합니다.

매개변수:
- `slot_name`: 슬롯 이름
- `immediately_reserve`: `true`이면 즉시 LSN 예약 (기본값: 첫 연결 시)
- `temporary`: `true`이면 세션 전용, 에러 시 자동 해제

예제:
```sql
-- 물리적 복제 슬롯 생성
SELECT * FROM pg_create_physical_replication_slot('standby_slot');

-- 즉시 예약으로 임시 슬롯 생성
SELECT * FROM pg_create_physical_replication_slot('temp_slot', true, true);
```

### pg_copy_physical_replication_slot()

```sql
pg_copy_physical_replication_slot(
  src_slot_name name,
  dst_slot_name name,
  [temporary boolean]
) → record (slot_name name, lsn pg_lsn)
```

설명: 기존 물리적 슬롯을 복사합니다.

참고: 복사된 슬롯은 소스 슬롯의 LSN부터 WAL을 예약합니다. 무효화된 슬롯은 복사할 수 없습니다.

### pg_drop_replication_slot()

```sql
pg_drop_replication_slot(slot_name name) → void
```

설명: 물리적 또는 논리적 복제 슬롯을 삭제합니다.

예제:
```sql
-- 복제 슬롯 삭제
SELECT pg_drop_replication_slot('standby_slot');
```

## 5.2 논리적 복제 슬롯 (Logical Replication Slots)

### pg_create_logical_replication_slot()

```sql
pg_create_logical_replication_slot(
  slot_name name,
  plugin name,
  [temporary boolean, twophase boolean, failover boolean]
) → record (slot_name name, lsn pg_lsn)
```

설명: 새 논리적(디코딩) 복제 슬롯을 생성합니다.

매개변수:
- `slot_name`: 슬롯 이름
- `plugin`: 출력 플러그인 이름 (예: `pgoutput`, `test_decoding`)
- `temporary`: `true`이면 세션 전용
- `twophase`: `true`이면 준비된 트랜잭션(prepared transactions) 디코딩 활성화
- `failover`: `true`이면 페일오버 재개를 위해 스탠바이에 동기화

예제:
```sql
-- 논리적 복제 슬롯 생성
SELECT * FROM pg_create_logical_replication_slot('my_slot', 'pgoutput');

-- 2단계 커밋 지원 슬롯 생성
SELECT * FROM pg_create_logical_replication_slot(
    'two_phase_slot',
    'pgoutput',
    false,  -- temporary
    true    -- twophase
);
```

### pg_copy_logical_replication_slot()

```sql
pg_copy_logical_replication_slot(
  src_slot_name name,
  dst_slot_name name,
  [temporary boolean, plugin name]
) → record (slot_name name, lsn pg_lsn)
```

설명: 기존 논리적 복제 슬롯을 복사합니다.

참고: 출력 플러그인과 지속성을 변경할 수 있습니다. `failover`는 복사되지 않습니다(기본값 false).

## 5.3 논리적 디코딩 (Logical Decoding)

### pg_logical_slot_get_changes()

```sql
pg_logical_slot_get_changes(
  slot_name name,
  upto_lsn pg_lsn,
  upto_nchanges integer,
  VARIADIC options text[]
) → setof record (lsn pg_lsn, xid xid, data text)
```

설명: 마지막 소비 이후의 변경 사항을 반환하고 소비합니다.

매개변수:
- `slot_name`: 슬롯 이름
- `upto_lsn`: `NULL`이면 WAL 끝까지, 아니면 해당 LSN 이전에 커밋된 트랜잭션만 포함
- `upto_nchanges`: `NULL`이면 제한 없음, 양수이면 해당 행 수 초과 시 중단 (소프트 제한, 트랜잭션 단위로 확인)
- `options`: 출력 플러그인에 전달할 옵션

예제:
```sql
-- 모든 변경 사항 가져오기
SELECT * FROM pg_logical_slot_get_changes('my_slot', NULL, NULL);

-- 최대 100개 변경 사항만 가져오기
SELECT * FROM pg_logical_slot_get_changes('my_slot', NULL, 100);

-- 옵션과 함께 가져오기
SELECT * FROM pg_logical_slot_get_changes(
    'my_slot',
    NULL,
    NULL,
    'include-xids', 'true',
    'include-timestamp', 'true'
);
```

### pg_logical_slot_peek_changes()

```sql
pg_logical_slot_peek_changes(
  slot_name name,
  upto_lsn pg_lsn,
  upto_nchanges integer,
  VARIADIC options text[]
) → setof record (lsn pg_lsn, xid xid, data text)
```

설명: `pg_logical_slot_get_changes()`와 동일하지만 변경 사항을 소비하지 않습니다. 다음 호출 시 다시 반환됩니다.

### pg_logical_slot_get_binary_changes()

```sql
pg_logical_slot_get_binary_changes(
  slot_name name,
  upto_lsn pg_lsn,
  upto_nchanges integer,
  VARIADIC options text[]
) → setof record (lsn pg_lsn, xid xid, data bytea)
```

설명: `pg_logical_slot_get_changes()`와 같지만 `bytea`로 반환합니다.

### pg_logical_slot_peek_binary_changes()

```sql
pg_logical_slot_peek_binary_changes(
  slot_name name,
  upto_lsn pg_lsn,
  upto_nchanges integer,
  VARIADIC options text[]
) → setof record (lsn pg_lsn, xid xid, data bytea)
```

설명: `pg_logical_slot_peek_changes()`와 같지만 `bytea`로 반환합니다.

### pg_replication_slot_advance()

```sql
pg_replication_slot_advance(
  slot_name name,
  upto_lsn pg_lsn
) → record (slot_name name, end_lsn pg_lsn)
```

설명: 복제 슬롯의 확인된 위치를 앞으로 이동합니다.

주의: 슬롯은 뒤로 이동하거나 삽입 위치를 초과하여 이동하지 않습니다. 위치는 다음 체크포인트에서 기록됩니다.

예제:
```sql
-- 슬롯 위치 앞으로 이동
SELECT * FROM pg_replication_slot_advance('my_slot', '0/1A2B3C4D');
```

## 5.4 복제 원점 (Replication Origins)

복제 원점은 복제된 데이터의 출처를 추적하는 데 사용됩니다.

### pg_replication_origin_create()

```sql
pg_replication_origin_create(node_name text) → oid
```

설명: 외부 이름으로 복제 원점을 생성합니다.

반환값: 내부 ID

주의: 최대 이름 길이 512바이트

예제:
```sql
-- 복제 원점 생성
SELECT pg_replication_origin_create('remote_cluster_1');
```

### pg_replication_origin_drop()

```sql
pg_replication_origin_drop(node_name text) → void
```

설명: 복제 원점과 연관된 재생 진행 상황을 삭제합니다.

### pg_replication_origin_oid()

```sql
pg_replication_origin_oid(node_name text) → oid
```

설명: 이름으로 복제 원점을 찾아 내부 ID를 반환합니다. 없으면 `NULL` 반환.

### pg_replication_origin_session_setup()

```sql
pg_replication_origin_session_setup(node_name text) → void
```

설명: 현재 세션을 지정된 원점에서 재생하는 것으로 표시합니다.

주의: 이미 선택된 원점이 없을 때만 호출 가능. `pg_replication_origin_session_reset()`으로 취소.

### pg_replication_origin_session_reset()

```sql
pg_replication_origin_session_reset() → void
```

설명: `pg_replication_origin_session_setup()` 효과를 취소합니다.

### pg_replication_origin_session_is_setup()

```sql
pg_replication_origin_session_is_setup() → boolean
```

설명: 현재 세션에서 복제 원점이 선택되었는지 여부를 반환합니다.

### pg_replication_origin_session_progress()

```sql
pg_replication_origin_session_progress(flush boolean) → pg_lsn
```

설명: 선택된 원점의 재생 위치를 반환합니다.

매개변수:
- `flush`: 로컬 트랜잭션이 디스크에 플러시되었음을 보장할지 여부

### pg_replication_origin_xact_setup()

```sql
pg_replication_origin_xact_setup(
  origin_lsn pg_lsn,
  origin_timestamp timestamp with time zone
) → void
```

설명: 현재 트랜잭션을 원점에서 재생하는 트랜잭션으로 표시합니다.

주의: `pg_replication_origin_session_setup()`으로 원점이 선택된 경우에만 호출 가능.

### pg_replication_origin_xact_reset()

```sql
pg_replication_origin_xact_reset() → void
```

설명: `pg_replication_origin_xact_setup()` 효과를 취소합니다.

### pg_replication_origin_advance()

```sql
pg_replication_origin_advance(node_name text, lsn pg_lsn) → void
```

설명: 원점 노드의 복제 진행 상황을 설정합니다.

용도: 초기 설정 또는 구성 변경 후 유용합니다.

경고: 부주의하게 사용하면 일관성 없는 복제로 이어질 수 있습니다.

### pg_replication_origin_progress()

```sql
pg_replication_origin_progress(node_name text, flush boolean) → pg_lsn
```

설명: 지정된 원점의 재생 위치를 반환합니다.

매개변수:
- `flush`: 트랜잭션이 디스크에 플러시되었음을 보장할지 여부

## 5.5 논리적 디코딩 메시지 (Logical Decoding Messages)

### pg_logical_emit_message()

```sql
pg_logical_emit_message(
  transactional boolean,
  prefix text,
  content text | bytea,
  [flush boolean DEFAULT false]
) → pg_lsn
```

설명: 플러그인용 논리적 디코딩 메시지를 발행합니다.

매개변수:
- `transactional`: `true`이면 현재 트랜잭션의 일부, `false`이면 즉시 기록
- `prefix`: 플러그인 인식을 위한 텍스트 접두사
- `content`: 메시지 내용 (텍스트 또는 바이너리)
- `flush`: `false`(기본값)이면 WAL 쓰기 지연 가능 (transactional이면 무시됨)

예제:
```sql
-- 트랜잭션 메시지 발행
SELECT pg_logical_emit_message(true, 'myapp', 'data migration started');

-- 비트랜잭션 메시지 즉시 발행
SELECT pg_logical_emit_message(false, 'myapp', 'checkpoint marker', true);
```

## 5.6 복제 슬롯 동기화 (Replication Slot Synchronization)

### pg_sync_replication_slots()

```sql
pg_sync_replication_slots() → void
```

설명: 프라이머리에서 스탠바이로 논리적 페일오버 복제 슬롯을 동기화합니다.

주의사항:
- 스탠바이에서만 실행
- 승격 후 임시 동기화 슬롯을 삭제해야 함
- `sync_replication_slots`가 활성화되고 slotsync 워커가 실행 중이면 실행 불가
- `hot_standby_feedback` 비활성화 후 동기화된 슬롯이 무효화될 수 있음

---

# 6. 통계 함수 (Statistics Functions)

## 6.1 데이터베이스 객체 크기 함수

모든 크기 결과는 바이트 단위입니다. 존재하지 않는 OID에 대해서는 `NULL`을 반환합니다.

### pg_column_size()

```sql
pg_column_size("any") → integer
```

설명: 개별 데이터 값을 저장하는 데 사용된 바이트 수를 반환합니다. 압축이 적용된 경우 이를 반영합니다.

예제:
```sql
SELECT pg_column_size('Hello World');
-- 결과: 12

SELECT pg_column_size(repeat('x', 10000));
-- 압축으로 인해 실제 크기보다 작을 수 있음
```

### pg_column_compression()

```sql
pg_column_compression("any") → text
```

설명: 가변 길이 값에 사용된 압축 알고리즘을 반환합니다. 압축되지 않은 경우 `NULL` 반환.

### pg_database_size()

```sql
pg_database_size(name | oid) → bigint
```

설명: 데이터베이스가 사용하는 총 디스크 공간을 반환합니다.

권한: `CONNECT` 권한 또는 `pg_read_all_stats` 역할 필요

예제:
```sql
-- 데이터베이스 크기 확인
SELECT pg_size_pretty(pg_database_size('mydb'));
-- 결과: '1234 MB'

-- 모든 데이터베이스 크기 확인
SELECT datname, pg_size_pretty(pg_database_size(datname))
FROM pg_database
ORDER BY pg_database_size(datname) DESC;
```

### pg_table_size()

```sql
pg_table_size(regclass) → bigint
```

설명: 테이블이 사용하는 디스크 공간을 반환합니다 (인덱스 제외). TOAST 테이블, 자유 공간 맵(FSM), 가시성 맵(VM)을 포함합니다.

### pg_indexes_size()

```sql
pg_indexes_size(regclass) → bigint
```

설명: 테이블 인덱스가 사용하는 총 디스크 공간을 반환합니다.

### pg_relation_size()

```sql
pg_relation_size(relation regclass [, fork text]) → bigint
```

설명: 릴레이션 "포크"가 사용하는 디스크 공간을 반환합니다.

fork 옵션:
- `main`: 메인 데이터 포크 (기본값)
- `fsm`: 자유 공간 맵 (Free Space Map)
- `vm`: 가시성 맵 (Visibility Map)
- `init`: 초기화 포크

예제:
```sql
-- 테이블의 메인 데이터 크기
SELECT pg_size_pretty(pg_relation_size('mytable'));

-- FSM 크기
SELECT pg_size_pretty(pg_relation_size('mytable', 'fsm'));
```

### pg_total_relation_size()

```sql
pg_total_relation_size(regclass) → bigint
```

설명: 총 디스크 공간: `pg_table_size() + pg_indexes_size()`

예제:
```sql
-- 테이블 총 크기 (인덱스 포함)
SELECT pg_size_pretty(pg_total_relation_size('mytable'));
```

### pg_tablespace_size()

```sql
pg_tablespace_size(name | oid) → bigint
```

설명: 테이블스페이스의 총 디스크 공간을 반환합니다.

권한: `CREATE` 권한 또는 `pg_read_all_stats` 역할 필요 (기본 테이블스페이스 제외)

### pg_size_pretty()

```sql
pg_size_pretty(bigint | numeric) → text
```

설명: 바이트를 사람이 읽기 쉬운 형식으로 변환합니다.

단위: bytes, kB, MB, GB, TB, PB (2의 거듭제곱)
- 1kB = 1024 bytes
- 1MB = 1048576 bytes

예제:
```sql
SELECT pg_size_pretty(1073741824);
-- 결과: '1024 MB'

SELECT pg_size_pretty(pg_database_size(current_database()));
```

### pg_size_bytes()

```sql
pg_size_bytes(text) → bigint
```

설명: 사람이 읽기 쉬운 형식을 바이트로 변환합니다.

유효한 단위: bytes, B, kB, MB, GB, TB, PB

예제:
```sql
SELECT pg_size_bytes('1 GB');
-- 결과: 1073741824
```

## 6.2 위치 함수 (Location Functions)

### pg_relation_filenode()

```sql
pg_relation_filenode(relation regclass) → oid
```

설명: 릴레이션의 파일노드 번호를 반환합니다. 파일 이름의 기본 구성 요소입니다.

참고: 보통 `pg_class.relfilenode`와 동일합니다. 저장소가 없는 릴레이션(뷰)은 `NULL` 반환.

### pg_relation_filepath()

```sql
pg_relation_filepath(relation regclass) → text
```

설명: PGDATA를 기준으로 한 릴레이션의 전체 파일 경로를 반환합니다.

예제:
```sql
SELECT pg_relation_filepath('mytable');
-- 결과: 'base/16384/16385'
```

### pg_filenode_relation()

```sql
pg_filenode_relation(tablespace oid, filenode oid) → regclass
```

설명: `pg_relation_filepath()`의 역함수입니다.

매개변수:
- `tablespace`: 테이블스페이스 OID. 기본 테이블스페이스는 `0`
- `filenode`: 파일노드 번호

반환값: 릴레이션이 없거나 임시 릴레이션이면 `NULL`

## 6.3 통계 조작 함수 (Statistics Manipulation Functions)

경고: 변경 사항은 autovacuum이나 VACUUM/ANALYZE에 의해 덮어씌워질 가능성이 높습니다. 임시 용도로만 사용하세요.

복구 중에는 실행할 수 없습니다.

### pg_restore_relation_stats()

```sql
pg_restore_relation_stats(VARIADIC kwargs "any") → boolean
```

설명: 테이블 수준 통계를 업데이트합니다.

필수 인자:
- `schemaname` (text): 스키마 이름
- `relname` (text): 테이블 이름

선택적 통계:
- `relpages` (integer): 페이지 수
- `reltuples` (real): 튜플 수
- `relallvisible` (integer): 모두 가시 페이지 수
- `relallfrozen` (integer): 모두 동결 페이지 수
- `version` (integer): 소스 버전

권한: `MAINTAIN` 권한 또는 데이터베이스 소유자

예제:
```sql
SELECT pg_restore_relation_stats(
    'schemaname', 'myschema',
    'relname',    'mytable',
    'relpages',   173::integer,
    'reltuples',  10000::real
);
```

### pg_clear_relation_stats()

```sql
pg_clear_relation_stats(schemaname text, relname text) → void
```

설명: 테이블 수준 통계를 새로 생성된 것처럼 지웁니다.

권한: `MAINTAIN` 권한 또는 데이터베이스 소유자

### pg_restore_attribute_stats()

```sql
pg_restore_attribute_stats(VARIADIC kwargs "any") → boolean
```

설명: 컬럼 수준 통계를 생성/업데이트합니다.

필수 인자:
- `schemaname` (text): 스키마 이름
- `relname` (text): 테이블 이름
- `attname` (text) 또는 `attnum` (smallint): 컬럼 이름 또는 번호
- `inherited` (boolean): 상속 여부

선택적 통계 (`pg_stats`에서):
- `avg_width` (integer): 평균 너비
- `null_frac` (real): NULL 비율
- `version` (integer): 소스 버전

권한: `MAINTAIN` 권한 또는 데이터베이스 소유자

예제:
```sql
SELECT pg_restore_attribute_stats(
    'schemaname', 'myschema',
    'relname',    'mytable',
    'attname',    'col1',
    'inherited',  false,
    'avg_width',  125::integer,
    'null_frac',  0.5::real
);
```

### pg_clear_attribute_stats()

```sql
pg_clear_attribute_stats(
  schemaname text,
  relname text,
  attname text,
  inherited boolean
) → void
```

설명: 컬럼 수준 통계를 새로 생성된 것처럼 지웁니다.

권한: `MAINTAIN` 권한 또는 데이터베이스 소유자

## 6.4 파티셔닝 정보 함수 (Partitioning Information Functions)

### pg_partition_tree()

```sql
pg_partition_tree(regclass) → setof record
  (relid regclass, parentrelid regclass, isleaf boolean, level integer)
```

설명: 파티션 트리의 테이블/인덱스 목록을 반환합니다.

반환 필드:
- `relid`: 파티션 OID
- `parentrelid`: 부모 OID
- `isleaf`: 리프 파티션 여부
- `level`: 입력에서 0, 직접 자식은 1, 등

예제:
```sql
-- 파티션 트리 전체 크기
SELECT pg_size_pretty(sum(pg_relation_size(relid))) AS total_size
FROM pg_partition_tree('measurement');

-- 파티션 트리 구조 확인
SELECT * FROM pg_partition_tree('orders');
```

### pg_partition_ancestors()

```sql
pg_partition_ancestors(regclass) → setof regclass
```

설명: 자기 자신을 포함한 조상 릴레이션 목록을 반환합니다.

### pg_partition_root()

```sql
pg_partition_root(regclass) → regclass
```

설명: 파티션 트리의 최상위 부모를 반환합니다.

반환값: 파티션/파티션 테이블이 아니면 `NULL`

---

# 7. 인덱스 유지보수 함수 (Index Maintenance Functions)

복구 중에는 실행할 수 없습니다. 슈퍼유저와 인덱스 소유자로 제한됩니다.

## BRIN 인덱스 함수

### brin_summarize_new_values()

```sql
brin_summarize_new_values(index regclass) → integer
```

설명: BRIN 인덱스에서 요약되지 않은 페이지 범위를 스캔하고 테이블 페이지를 스캔하여 요약을 생성합니다.

반환값: 삽입된 새 요약 수

예제:
```sql
-- 새 값 요약
SELECT brin_summarize_new_values('mytable_brin_idx');
```

### brin_summarize_range()

```sql
brin_summarize_range(index regclass, blockNumber bigint) → integer
```

설명: 지정된 블록을 포함하는 페이지 범위를 요약합니다. 해당 특정 범위만 처리합니다.

### brin_desummarize_range()

```sql
brin_desummarize_range(index regclass, blockNumber bigint) → void
```

설명: 페이지 범위를 요약하는 BRIN 인덱스 튜플을 제거합니다.

## GIN 인덱스 함수

### gin_clean_pending_list()

```sql
gin_clean_pending_list(index regclass) → bigint
```

설명: GIN 인덱스의 "대기 중(pending)" 목록을 정리합니다. 항목을 메인 GIN 구조로 이동합니다.

반환값: 대기 목록에서 제거된 페이지 수. `fastupdate = false`이면 0 반환 (대기 목록 없음)

예제:
```sql
-- 대기 목록 정리
SELECT gin_clean_pending_list('mytable_gin_idx');
```

---

# 8. 일반 파일 접근 함수 (Generic File Access Functions)

클러스터 디렉터리와 `log_directory`로 접근이 제한됩니다. 슈퍼유저 또는 `pg_read_server_files` 역할을 가진 사용자만 사용 가능합니다.

중요: 이러한 함수는 데이터베이스 내 권한 검사를 우회합니다. 권한 부여에 주의하세요.

## 디렉터리 목록 함수

### pg_ls_dir()

```sql
pg_ls_dir(
  dirname text,
  [missing_ok boolean, include_dot_dirs boolean]
) → setof text
```

설명: 디렉터리의 파일, 디렉터리, 특수 파일 목록을 반환합니다.

매개변수:
- `missing_ok`: `true`이면 없을 때 `NULL` 반환 (기본값: 에러)
- `include_dot_dirs`: `false`(기본값)이면 "."과 ".." 제외

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- 데이터 디렉터리 목록
SELECT pg_ls_dir('base');

-- 없어도 에러 안 남
SELECT pg_ls_dir('nonexistent', true, false);
```

### pg_ls_logdir()

```sql
pg_ls_logdir() → setof record
  (name text, size bigint, modification timestamp with time zone)
```

설명: 서버 로그 디렉터리의 파일 목록을 반환합니다.

참고: 점으로 시작하는 파일, 디렉터리, 특수 파일은 제외됩니다.

권한: 슈퍼유저 및 `pg_monitor` 역할 (권한 부여 가능)

### pg_ls_waldir()

```sql
pg_ls_waldir() → setof record
  (name text, size bigint, modification timestamp with time zone)
```

설명: WAL 디렉터리의 파일 목록을 반환합니다.

권한: 슈퍼유저 및 `pg_monitor` 역할 (권한 부여 가능)

예제:
```sql
-- WAL 파일 목록 및 크기
SELECT name, pg_size_pretty(size) FROM pg_ls_waldir() ORDER BY modification DESC;
```

### pg_ls_logicalmapdir()

```sql
pg_ls_logicalmapdir() → setof record
  (name text, size bigint, modification timestamp with time zone)
```

설명: `pg_logical/mappings` 디렉터리의 파일 목록을 반환합니다.

### pg_ls_logicalsnapdir()

```sql
pg_ls_logicalsnapdir() → setof record
  (name text, size bigint, modification timestamp with time zone)
```

설명: `pg_logical/snapshots` 디렉터리의 파일 목록을 반환합니다.

### pg_ls_replslotdir()

```sql
pg_ls_replslotdir(slot_name text) → setof record
  (name text, size bigint, modification timestamp with time zone)
```

설명: 복제 슬롯 디렉터리의 파일 목록을 반환합니다.

### pg_ls_tmpdir()

```sql
pg_ls_tmpdir([tablespace oid]) → setof record
  (name text, size bigint, modification timestamp with time zone)
```

설명: 테이블스페이스의 임시 디렉터리 파일 목록을 반환합니다.

매개변수:
- `tablespace`: 지정하지 않으면 기본값 `pg_default`

## 파일 읽기 함수

### pg_read_file()

```sql
pg_read_file(
  filename text,
  [offset bigint, length bigint],
  [missing_ok boolean]
) → text
```

설명: 파일 내용 또는 파일의 일부를 반환합니다.

매개변수:
- `offset`: 음수이면 파일 끝에서부터의 상대 위치
- `missing_ok`: `true`이면 없을 때 `NULL` 반환

참고: 데이터베이스 인코딩 문자열로 해석합니다. 유효하지 않은 인코딩이면 에러 발생.

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- 전체 파일 읽기
SELECT pg_read_file('postgresql.conf');

-- 처음 100바이트만 읽기
SELECT pg_read_file('postgresql.conf', 0, 100);
```

### pg_read_binary_file()

```sql
pg_read_binary_file(
  filename text,
  [offset bigint, length bigint],
  [missing_ok boolean]
) → bytea
```

설명: `pg_read_file()`과 같지만 `bytea`로 반환합니다. 인코딩 검사 없음.

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
-- UTF-8 파일을 데이터베이스 인코딩으로 읽기
SELECT convert_from(pg_read_binary_file('file_in_utf8.txt'), 'UTF8');
```

### pg_stat_file()

```sql
pg_stat_file(filename text [, missing_ok boolean]) → record
  (size bigint, access timestamp with time zone,
   modification timestamp with time zone, change timestamp with time zone,
   creation timestamp with time zone, isdir boolean)
```

설명: 파일 메타데이터를 반환합니다.

반환 필드:
- `size`: 파일 크기
- `access`: 접근 시간
- `modification`: 수정 시간
- `change`: 변경 시간 (Unix)
- `creation`: 생성 시간 (Windows)
- `isdir`: 디렉터리 여부

권한: 슈퍼유저 (권한 부여 가능)

예제:
```sql
SELECT * FROM pg_stat_file('postgresql.conf');
```

---

# 9. 권고 잠금 함수 (Advisory Lock Functions)

권고 잠금(Advisory Lock)은 애플리케이션 정의 리소스 잠금을 관리합니다. 64비트 키 또는 두 개의 32비트 키(겹치지 않는 공간)로 식별됩니다.

## 잠금 유형

- 배타적 잠금 (Exclusive Lock): 다른 배타적 잠금과 충돌
- 공유 잠금 (Shared Lock): 다른 공유 잠금과 충돌하지 않음, 배타적 잠금만 충돌

## 잠금 수준

- 세션 수준 (Session-level): 해제하거나 세션이 종료될 때까지 유지. 스태킹 지원
- 트랜잭션 수준 (Transaction-level): 트랜잭션 종료 시 자동 해제. 수동 해제 불가

## 배타적 세션 수준 잠금

### pg_advisory_lock()

```sql
pg_advisory_lock(key bigint | key1 integer, key2 integer) → void
```

설명: 배타적 세션 수준 잠금을 획득합니다. 필요시 대기합니다.

예제:
```sql
-- 64비트 키로 잠금
SELECT pg_advisory_lock(12345);

-- 두 개의 32비트 키로 잠금
SELECT pg_advisory_lock(1, 2);
```

### pg_try_advisory_lock()

```sql
pg_try_advisory_lock(key bigint | key1 integer, key2 integer) → boolean
```

설명: 가능하면 배타적 세션 수준 잠금을 획득합니다.

반환값: 획득 시 즉시 `true`, 불가능 시 대기 없이 `false`

예제:
```sql
-- 잠금 시도
IF pg_try_advisory_lock(12345) THEN
    -- 잠금 획득 성공, 작업 수행
END IF;
```

## 공유 세션 수준 잠금

### pg_advisory_lock_shared()

```sql
pg_advisory_lock_shared(key bigint | key1 integer, key2 integer) → void
```

설명: 공유 세션 수준 잠금을 획득합니다. 필요시 대기합니다.

### pg_try_advisory_lock_shared()

```sql
pg_try_advisory_lock_shared(key bigint | key1 integer, key2 integer) → boolean
```

설명: 가능하면 공유 세션 수준 잠금을 획득합니다.

반환값: 획득 시 즉시 `true`, 불가능 시 대기 없이 `false`

## 세션 수준 잠금 해제

### pg_advisory_unlock()

```sql
pg_advisory_unlock(key bigint | key1 integer, key2 integer) → boolean
```

설명: 배타적 세션 수준 잠금을 해제합니다.

반환값: 성공적으로 해제되면 `true`, 잠금이 없으면 `false` (SQL 경고 보고)

### pg_advisory_unlock_shared()

```sql
pg_advisory_unlock_shared(key bigint | key1 integer, key2 integer) → boolean
```

설명: 공유 세션 수준 잠금을 해제합니다.

반환값: 성공적으로 해제되면 `true`, 잠금이 없으면 `false` (SQL 경고 보고)

### pg_advisory_unlock_all()

```sql
pg_advisory_unlock_all() → void
```

설명: 현재 세션이 보유한 모든 세션 수준 잠금을 해제합니다.

참고: 세션 종료 시 자동으로 호출됩니다 (비정상 종료 포함).

## 트랜잭션 수준 잠금

### pg_advisory_xact_lock()

```sql
pg_advisory_xact_lock(key bigint | key1 integer, key2 integer) → void
```

설명: 배타적 트랜잭션 수준 잠금을 획득합니다. 필요시 대기합니다. 트랜잭션 종료 시 해제됩니다.

예제:
```sql
BEGIN;
SELECT pg_advisory_xact_lock(12345);
-- 작업 수행
COMMIT; -- 자동으로 잠금 해제
```

### pg_try_advisory_xact_lock()

```sql
pg_try_advisory_xact_lock(key bigint | key1 integer, key2 integer) → boolean
```

설명: 가능하면 배타적 트랜잭션 수준 잠금을 획득합니다.

반환값: 획득 시 즉시 `true`, 불가능 시 대기 없이 `false`

### pg_advisory_xact_lock_shared()

```sql
pg_advisory_xact_lock_shared(key bigint | key1 integer, key2 integer) → void
```

설명: 공유 트랜잭션 수준 잠금을 획득합니다. 필요시 대기합니다. 트랜잭션 종료 시 해제됩니다.

### pg_try_advisory_xact_lock_shared()

```sql
pg_try_advisory_xact_lock_shared(key bigint | key1 integer, key2 integer) → boolean
```

설명: 가능하면 공유 트랜잭션 수준 잠금을 획득합니다.

반환값: 획득 시 즉시 `true`, 불가능 시 대기 없이 `false`

## 권고 잠금 사용 예제

```sql
-- 배치 작업에서 중복 실행 방지
DO $$
DECLARE
    lock_id CONSTANT bigint := 999999;
BEGIN
    IF NOT pg_try_advisory_lock(lock_id) THEN
        RAISE NOTICE '다른 프로세스가 이미 실행 중입니다';
        RETURN;
    END IF;

    -- 배치 작업 수행
    PERFORM process_batch();

    -- 잠금 해제
    PERFORM pg_advisory_unlock(lock_id);
END;
$$;

-- 트랜잭션 수준 잠금으로 동시성 제어
BEGIN;
SELECT pg_advisory_xact_lock(
    ('x' || substr(md5('customer_' || customer_id::text), 1, 16))::bit(64)::bigint
)
FROM customer
WHERE customer_id = 12345;

-- 고객 데이터 처리
UPDATE customer SET ... WHERE customer_id = 12345;

COMMIT; -- 잠금 자동 해제
```

---

# 10. 세션 정보 함수 (Session Information Functions)

세션 및 시스템 정보를 추출하는 함수들입니다.

## 기본 세션 정보

### current_database() / current_catalog

```sql
current_database() → name
current_catalog → name
```

설명: 현재 데이터베이스 이름을 반환합니다.

### current_user / session_user / user

```sql
current_user → name
session_user → name
user → name
```

설명:
- `current_user` / `user`: 현재 실행 컨텍스트의 사용자 이름
- `session_user`: 세션 사용자 이름

참고: `current_role`은 `current_user`와 동일합니다.

### current_schema / current_schemas()

```sql
current_schema → name
current_schema() → name
current_schemas(boolean) → name[]
```

설명:
- `current_schema`: 검색 경로의 첫 번째 스키마
- `current_schemas(true)`: 암시적 스키마 포함
- `current_schemas(false)`: 명시적 스키마만

예제:
```sql
SELECT current_schema();
-- 결과: 'public'

SELECT current_schemas(true);
-- 결과: '{pg_catalog,public}'
```

## 네트워크 정보

### inet_client_addr() / inet_client_port()

```sql
inet_client_addr() → inet
inet_client_port() → integer
```

설명: 현재 클라이언트의 IP 주소와 포트를 반환합니다.

### inet_server_addr() / inet_server_port()

```sql
inet_server_addr() → inet
inet_server_port() → integer
```

설명: 현재 연결에서 서버의 IP 주소와 포트를 반환합니다.

예제:
```sql
SELECT inet_client_addr(), inet_client_port();
-- 결과: '192.168.1.100', 54321

SELECT inet_server_addr(), inet_server_port();
-- 결과: '192.168.1.1', 5432
```

## 프로세스 및 시스템 정보

### pg_backend_pid()

```sql
pg_backend_pid() → integer
```

설명: 세션에 연결된 서버 프로세스의 PID를 반환합니다.

### pg_blocking_pids()

```sql
pg_blocking_pids(pid integer) → integer[]
```

설명: 지정된 프로세스를 차단하는 프로세스 ID 배열을 반환합니다.

예제:
```sql
-- 차단된 프로세스와 차단하는 프로세스 확인
SELECT pid, pg_blocking_pids(pid), query
FROM pg_stat_activity
WHERE cardinality(pg_blocking_pids(pid)) > 0;
```

### pg_conf_load_time()

```sql
pg_conf_load_time() → timestamp with time zone
```

설명: 설정 파일이 마지막으로 로드된 시간을 반환합니다.

### pg_postmaster_start_time()

```sql
pg_postmaster_start_time() → timestamp with time zone
```

설명: 서버 시작 시간을 반환합니다.

예제:
```sql
-- 서버 가동 시간
SELECT now() - pg_postmaster_start_time() AS uptime;
```

### pg_current_logfile()

```sql
pg_current_logfile([text]) → text
```

설명: 현재 로그 파일의 경로를 반환합니다.

### pg_listening_channels()

```sql
pg_listening_channels() → setof text
```

설명: 세션이 수신 대기 중인 알림 채널 집합을 반환합니다.

### pg_notification_queue_usage()

```sql
pg_notification_queue_usage() → double precision
```

설명: 알림 큐가 차지하는 비율(0-1)을 반환합니다.

---

# 11. 트랜잭션 ID와 스냅샷 (Transaction IDs and Snapshots)

## 트랜잭션 ID 함수

### pg_current_xact_id()

```sql
pg_current_xact_id() → xid8
```

설명: 현재 트랜잭션 ID를 반환합니다.

### pg_current_xact_id_if_assigned()

```sql
pg_current_xact_id_if_assigned() → xid8
```

설명: 할당된 경우 트랜잭션 ID를 반환하고, 없으면 `NULL`을 반환합니다.

### pg_xact_status()

```sql
pg_xact_status(xid8) → text
```

설명: 커밋 상태를 반환합니다.

반환값: `'in progress'`, `'committed'`, `'aborted'`, 또는 `NULL`

예제:
```sql
SELECT pg_xact_status(pg_current_xact_id());
-- 결과: 'in progress'
```

### age()

```sql
age(xid) → integer
```

설명: 주어진 XID 이후의 트랜잭션 수를 반환합니다.

예제:
```sql
-- 테이블의 나이 확인 (동결 필요성 판단)
SELECT relname, age(relfrozenxid)
FROM pg_class
WHERE relkind = 'r'
ORDER BY age(relfrozenxid) DESC
LIMIT 10;
```

## 스냅샷 함수

### pg_current_snapshot()

```sql
pg_current_snapshot() → pg_snapshot
```

설명: 현재 스냅샷을 반환합니다 (`xmin:xmax:xip_list` 형식).

스냅샷 구성 요소:
- `xmin`: 가장 낮은 활성 트랜잭션 ID
- `xmax`: 가장 높은 완료된 트랜잭션 ID + 1
- `xip_list`: 진행 중인 트랜잭션 목록 (형식: "10:20:10,14,15")

### pg_snapshot_xip()

```sql
pg_snapshot_xip(pg_snapshot) → setof xid8
```

설명: 스냅샷에서 진행 중인 XID들을 반환합니다.

### pg_snapshot_xmax()

```sql
pg_snapshot_xmax(pg_snapshot) → xid8
```

설명: 스냅샷에서 xmax를 반환합니다.

### pg_snapshot_xmin()

```sql
pg_snapshot_xmin(pg_snapshot) → xid8
```

설명: 스냅샷에서 xmin을 반환합니다.

### pg_visible_in_snapshot()

```sql
pg_visible_in_snapshot(xid8, pg_snapshot) → boolean
```

설명: 트랜잭션이 스냅샷에서 보이는지 여부를 반환합니다.

---

# 12. 제어 데이터 함수 (Control Data Functions)

클러스터 전체 정보를 반환합니다 (`pg_controldata` 애플리케이션과 유사).

## pg_control_checkpoint()

```sql
pg_control_checkpoint() → record
```

설명: 체크포인트 상태를 반환합니다.

반환 필드:
- `checkpoint_lsn`, `redo_lsn` (pg_lsn)
- `redo_wal_file` (text)
- `timeline_id`, `prev_timeline_id` (integer)
- `full_page_writes` (boolean)
- `next_xid`, `next_oid`, `next_multixact_id`, `next_multi_offset`
- `oldest_xid`, `oldest_xid_dbid`, `oldest_active_xid`
- `oldest_multi_xid`, `oldest_multi_dbid`
- `oldest_commit_ts_xid`, `newest_commit_ts_xid`
- `checkpoint_time` (timestamp with time zone)

예제:
```sql
SELECT * FROM pg_control_checkpoint();
```

## pg_control_system()

```sql
pg_control_system() → record
```

설명: 제어 파일 상태를 반환합니다.

반환 필드:
- `pg_control_version`, `catalog_version_no` (integer)
- `system_identifier` (bigint)
- `pg_control_last_modified` (timestamp with time zone)

## pg_control_init()

```sql
pg_control_init() → record
```

설명: 클러스터 초기화 상태를 반환합니다.

반환 필드:
- `max_data_alignment`, `database_block_size`, `blocks_per_segment` (integer)
- `wal_block_size`, `bytes_per_wal_segment` (integer)
- `max_identifier_length`, `max_index_columns` (integer)
- `max_toast_chunk_size`, `large_object_chunk_size` (integer)
- `float8_pass_by_value` (boolean)
- `data_page_checksum_version` (integer)

## pg_control_recovery()

```sql
pg_control_recovery() → record
```

설명: 복구 상태를 반환합니다.

반환 필드:
- `min_recovery_end_lsn`, `backup_start_lsn`, `backup_end_lsn` (pg_lsn)
- `min_recovery_end_timeline` (integer)
- `end_of_backup_record_required` (boolean)

---

# 13. 버전 정보 함수 (Version Information Functions)

## version()

```sql
version() → text
```

설명: PostgreSQL 서버 버전 문자열을 반환합니다.

예제:
```sql
SELECT version();
-- 결과: 'PostgreSQL 16.0 on x86_64-pc-linux-gnu, compiled by gcc ...'
```

## unicode_version()

```sql
unicode_version() → text
```

설명: PostgreSQL에서 사용하는 Unicode 버전을 반환합니다.

## icu_unicode_version()

```sql
icu_unicode_version() → text
```

설명: ICU Unicode 버전을 반환합니다. ICU 지원이 없으면 `NULL` 반환.

---

# 요약 (Summary)

| 카테고리 | 주요 함수 | 개수 |
|----------|----------|------|
| 서버 신호 (Server Signaling) | `pg_cancel_backend`, `pg_terminate_backend`, `pg_log_backend_memory_contexts`, `pg_reload_conf`, `pg_rotate_logfile` | 5 |
| 백업 제어 (Backup Control) | `pg_backup_start`, `pg_backup_stop`, `pg_create_restore_point`, WAL 위치 함수 | 12 |
| 복구 제어 (Recovery Control) | 복구 정보 (5) + 제어 (5) | 10 |
| 스냅샷 (Snapshots) | `pg_export_snapshot`, `pg_log_standby_snapshot` | 2 |
| 복제 (Replication) | 물리적 슬롯 (3) + 논리적 슬롯 (4) + 디코딩 (4) + 원점 (9) + 메시지 (1) + 동기화 (1) | 22 |
| DB 객체 (DB Objects) | 크기 (9) + 위치 (3) + 통계 (4) + 파티셔닝 (3) | 19 |
| 인덱스 유지보수 (Index Maintenance) | BRIN (3) + GIN (1) | 4 |
| 파일 접근 (File Access) | 디렉터리 (9) + 읽기 (3) | 12 |
| 권고 잠금 (Advisory Locks) | 세션 배타 (2) + 세션 공유 (2) + 해제 (3) + 트랜잭션 배타 (2) + 트랜잭션 공유 (2) | 11 |
| 세션 정보 (Session Info) | 기본 (10) + 네트워크 (4) + 프로세스 (6) | 20 |
| 트랜잭션/스냅샷 (Txn/Snapshot) | 트랜잭션 ID (4) + 스냅샷 (6) | 10 |
| 제어 데이터 (Control Data) | `pg_control_*` 함수들 | 4 |
| 버전 정보 (Version) | `version`, `unicode_version`, `icu_unicode_version` | 3 |
| 총계 | | 134 |

---

# 참고 자료 (References)

- PostgreSQL 공식 문서: [System Administration Functions](https://www.postgresql.org/docs/current/functions-admin.html)
- PostgreSQL 공식 문서: [System Information Functions](https://www.postgresql.org/docs/current/functions-info.html)
- PostgreSQL 공식 문서: [Backup and Restore](https://www.postgresql.org/docs/current/backup.html)
- PostgreSQL 공식 문서: [High Availability, Load Balancing, and Replication](https://www.postgresql.org/docs/current/high-availability.html)
