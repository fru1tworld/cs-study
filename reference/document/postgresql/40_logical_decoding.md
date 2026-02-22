# 제49장. 논리적 디코딩 (Logical Decoding)

> PostgreSQL 18 공식 문서 번역

원문: [https://www.postgresql.org/docs/current/logicaldecoding.html](https://www.postgresql.org/docs/current/logicaldecoding.html)

---

## 목차

- [개요](#개요)
- [49.1. 논리적 디코딩 예제](#491-논리적-디코딩-예제)
  - [49.1.1. SQL 인터페이스 예제](#4911-sql-인터페이스-예제)
  - [49.1.2. 스트리밍 복제 프로토콜 예제](#4912-스트리밍-복제-프로토콜-예제)
  - [49.1.3. 2단계 커밋 예제](#4913-2단계-커밋-예제)
- [49.2. 논리적 디코딩 개념](#492-논리적-디코딩-개념)
  - [49.2.1. 논리적 디코딩](#4921-논리적-디코딩)
  - [49.2.2. 복제 슬롯 (Replication Slots)](#4922-복제-슬롯-replication-slots)
  - [49.2.3. 복제 슬롯 동기화](#4923-복제-슬롯-동기화)
  - [49.2.4. 출력 플러그인 (Output Plugins)](#4924-출력-플러그인-output-plugins)
  - [49.2.5. 내보낸 스냅샷 (Exported Snapshots)](#4925-내보낸-스냅샷-exported-snapshots)
- [49.3. 스트리밍 복제 프로토콜 인터페이스](#493-스트리밍-복제-프로토콜-인터페이스)
- [49.4. SQL 인터페이스](#494-sql-인터페이스)
- [49.5. 시스템 카탈로그](#495-시스템-카탈로그)
- [49.6. 출력 플러그인](#496-출력-플러그인)
  - [49.6.1. 초기화 함수](#4961-초기화-함수)
  - [49.6.2. 콜백 함수](#4962-콜백-함수)
  - [49.6.3. 출력 모드](#4963-출력-모드)
  - [49.6.4. 출력 생성 함수](#4964-출력-생성-함수)
- [49.7. 출력 라이터 (Output Writers)](#497-출력-라이터-output-writers)
- [49.8. 동기식 복제 지원](#498-동기식-복제-지원)
- [49.9. 대규모 트랜잭션 스트리밍](#499-대규모-트랜잭션-스트리밍)
- [49.10. 2단계 커밋 지원](#4910-2단계-커밋-지원)

---

## 개요

논리적 디코딩(Logical Decoding) 은 PostgreSQL이 SQL을 통해 수행된 데이터베이스 수정 사항을 외부 소비자에게 스트리밍할 수 있게 해주는 인프라입니다. 이 기능은 다음을 지원합니다:

- 복제 솔루션: 데이터베이스 간 동기화
- 감사(Auditing): 변경 사항 추적
- 커스텀 데이터 통합 파이프라인: ETL 프로세스 등

변경 사항은 논리적 복제 슬롯(Logical Replication Slots) 으로 식별되는 스트림으로 전송됩니다.

### 핵심 구성 요소

| 구성 요소 | 설명 |
|-----------|------|
| 복제 슬롯 (Replication Slot) | 변경 사항 스트림을 식별하고 소비자가 처리한 수정 사항을 추적 |
| 출력 플러그인 (Output Plugin) | 변경 사항이 스트리밍되는 형식을 결정 |
| 변경 소비 방법 | 스트리밍 복제 프로토콜, SQL 인터페이스, 커스텀 출력 라이터 |

### 출력 플러그인 접근 가능 데이터

모든 출력 플러그인은 다음에 접근할 수 있습니다:

- `INSERT`로 생성된 새 행
- `UPDATE`로 생성된 새 행 버전
- `UPDATE` 및 `DELETE`의 이전 행 버전 (구성된 `REPLICA IDENTITY`에 따라)

---

## 49.1. 논리적 디코딩 예제

이 섹션에서는 SQL 인터페이스와 스트리밍 복제 프로토콜을 사용하여 논리적 디코딩을 제어하는 방법을 보여줍니다.

### 사전 요구 사항

논리적 디코딩을 사용하기 전에 다음 설정이 필요합니다:

```sql
-- postgresql.conf 설정
wal_level = logical
max_replication_slots = 1  -- 최소 1 이상

-- 2단계 트랜잭션 사용 시
max_prepared_transactions = 1  -- 최소 1 이상
```

> 참고: 슈퍼유저 권한으로 연결해야 합니다.

### 49.1.1. SQL 인터페이스 예제

#### 논리적 복제 슬롯 생성

```sql
-- 논리적 복제 슬롯 생성
SELECT * FROM pg_create_logical_replication_slot('regression_slot', 'test_decoding', false, true);

-- 결과
    slot_name    |    lsn
-----------------+-----------
 regression_slot | 0/16B1970
(1 row)
```

#### 복제 슬롯 정보 조회

```sql
SELECT slot_name, plugin, slot_type, database, active, restart_lsn, confirmed_flush_lsn
FROM pg_replication_slots;
```

#### 테스트 데이터 생성

```sql
-- 테이블 생성 및 데이터 삽입
CREATE TABLE data(id serial primary key, data text);
INSERT INTO data(data) VALUES('1');
INSERT INTO data(data) VALUES('2');
UPDATE data SET data = 'updated' WHERE id = 1;
DELETE FROM data WHERE id = 2;
```

#### 변경 사항 조회 (소비)

```sql
-- 변경 사항 가져오기 (한 번 읽으면 소비됨)
SELECT * FROM pg_logical_slot_get_changes('regression_slot', NULL, NULL);

-- 결과
    lsn    | xid |                          data
-----------+-----+--------------------------------------------------------
 0/16B19A8 | 688 | BEGIN 688
 0/16B19A8 | 688 | table public.data: INSERT: id[integer]:1 data[text]:'1'
 0/16B1A38 | 688 | COMMIT 688
 0/16B1A38 | 689 | BEGIN 689
 0/16B1A38 | 689 | table public.data: INSERT: id[integer]:2 data[text]:'2'
 0/16B1AD0 | 689 | COMMIT 689
 ...
```

#### 변경 사항 미리보기 (소비하지 않음)

```sql
-- 변경 사항 미리보기 (소비되지 않음)
SELECT * FROM pg_logical_slot_peek_changes('regression_slot', NULL, NULL);
```

#### 출력 플러그인 옵션 사용

```sql
-- 타임스탬프 포함 옵션
SELECT * FROM pg_logical_slot_peek_changes('regression_slot', NULL, NULL, 'include-timestamp', 'on');

-- 결과 (타임스탬프 포함)
COMMIT 10299 (at 2017-05-10 12:07:21.272494-04)
```

#### 복제 슬롯 삭제

```sql
-- 더 이상 사용하지 않는 슬롯 삭제
SELECT pg_drop_replication_slot('regression_slot');
```

### 49.1.2. 스트리밍 복제 프로토콜 예제

`pg_recvlogical` 유틸리티를 사용한 예제입니다:

```bash
# 슬롯 생성
$ pg_recvlogical -d postgres --slot=test --create-slot

# 변경 사항 스트리밍 시작
$ pg_recvlogical -d postgres --slot=test --start -f -
# Ctrl+Z로 백그라운드 전환

# 다른 터미널에서 데이터 삽입
$ psql -d postgres -c "INSERT INTO data(data) VALUES('4');"

# fg로 포그라운드 복귀
$ fg
BEGIN 693
table public.data: INSERT: id[integer]:4 data[text]:'4'
COMMIT 693
# Ctrl+C로 종료

# 슬롯 삭제
$ pg_recvlogical -d postgres --slot=test --drop-slot
```

### 49.1.3. 2단계 커밋 예제

#### 스트리밍 프로토콜 사용

```bash
# 2단계 커밋 활성화하여 슬롯 생성
$ pg_recvlogical -d postgres --slot=test --create-slot --enable-two-phase

# 스트리밍 시작
$ pg_recvlogical -d postgres --slot=test --start -f -
# Ctrl+Z

# PREPARE TRANSACTION 실행
$ psql -d postgres -c "BEGIN;INSERT INTO data(data) VALUES('5');PREPARE TRANSACTION 'test';"

$ fg
BEGIN 694
table public.data: INSERT: id[integer]:5 data[text]:'5'
PREPARE TRANSACTION 'test', txid 694
# Ctrl+Z

# COMMIT PREPARED 실행
$ psql -d postgres -c "COMMIT PREPARED 'test';"

$ fg
COMMIT PREPARED 'test', txid 694
# Ctrl+C

# 슬롯 삭제
$ pg_recvlogical -d postgres --slot=test --drop-slot
```

#### SQL 인터페이스 사용

```sql
-- PREPARE TRANSACTION
BEGIN;
INSERT INTO data(data) VALUES('5');
PREPARE TRANSACTION 'test_prepared1';

-- 변경 사항 확인
SELECT * FROM pg_logical_slot_get_changes('regression_slot', NULL, NULL);
-- 결과: PREPARE TRANSACTION 정보 출력

-- COMMIT PREPARED
COMMIT PREPARED 'test_prepared1';
SELECT * FROM pg_logical_slot_get_changes('regression_slot', NULL, NULL);
-- 결과: COMMIT PREPARED 정보 출력

-- ROLLBACK PREPARED 예제
BEGIN;
INSERT INTO data(data) VALUES('6');
PREPARE TRANSACTION 'test_prepared2';

ROLLBACK PREPARED 'test_prepared2';
SELECT * FROM pg_logical_slot_get_changes('regression_slot', NULL, NULL);
-- 결과: ROLLBACK PREPARED 정보 출력
```

> 중요: `pg_logical_slot_get_changes()`로 읽은 변경 사항은 소비되어 후속 호출에서 나타나지 않습니다. 더 이상 사용하지 않는 슬롯은 삭제하여 서버 리소스를 확보하세요.

---

## 49.2. 논리적 디코딩 개념

### 49.2.1. 논리적 디코딩

논리적 디코딩(Logical Decoding) 은 데이터베이스 테이블에 대한 모든 영구적 변경 사항을 데이터베이스의 내부 상태에 대한 자세한 지식 없이도 해석할 수 있는 일관되고 이해하기 쉬운 형식으로 추출하는 과정입니다.

PostgreSQL은 저장소 수준에서 변경 사항을 설명하는 Write-Ahead Log(WAL) 의 내용을 디코딩하여 다음과 같은 애플리케이션별 형식으로 변환합니다:

- 튜플 스트림: 행 단위 데이터
- SQL 문: 재실행 가능한 명령어

### 49.2.2. 복제 슬롯 (Replication Slots)

복제 슬롯(Replication Slot) 은 원본 서버에서 생성된 순서대로 클라이언트에게 재생할 수 있는 변경 사항 스트림을 나타냅니다. 각 슬롯은 단일 데이터베이스에서 변경 사항 시퀀스를 스트리밍합니다.

#### 주요 특성

| 특성 | 설명 |
|------|------|
| 고유 식별자 | PostgreSQL 클러스터 내 모든 데이터베이스에서 고유 |
| 영속성 | 연결과 독립적으로 유지되며 충돌에 안전 |
| 정확히 한 번 전송 | 정상 운영 시 각 변경 사항을 정확히 한 번만 전송 |
| 위치 저장 | 현재 위치는 체크포인트 시에만 저장됨 |

#### 중요 고려 사항

- 다중 슬롯: 단일 데이터베이스에 대해 각각 고유한 상태를 가진 여러 독립 슬롯이 존재할 수 있음
- 단일 소비자: 주어진 시점에 하나의 수신자만 슬롯의 변경 사항을 소비할 수 있음
- Hot Standby 지원: Hot Standby에서 논리적 복제 슬롯을 생성할 수 있지만 추가 구성 필요

#### Hot Standby 요구 사항

Hot Standby에서 논리적 슬롯을 사용하려면:

- 스탠바이에서 `hot_standby_feedback` 활성화
- 프라이머리와 스탠바이 간 물리적 슬롯 권장
- 프라이머리에서 `wal_level`을 `logical`로 설정

> 주의: 복제 슬롯은 필요한 WAL과 시스템 카탈로그 행의 제거를 방지합니다. 이로 인해 상당한 저장 공간이 소비될 수 있으며, 극단적인 경우 트랜잭션 ID 랩어라운드를 방지하기 위해 데이터베이스가 종료될 수 있습니다. 더 이상 필요하지 않은 슬롯은 반드시 삭제하세요.

### 49.2.3. 복제 슬롯 동기화

프라이머리의 논리적 복제 슬롯을 Hot Standby에 동기화하여 장애 조치 기능을 제공할 수 있습니다.

#### 구성 요구 사항

```sql
-- 장애 조치 가능한 슬롯 생성
SELECT pg_create_logical_replication_slot('my_slot', 'test_decoding', false, true, 'failover');

-- 또는 CREATE SUBSCRIPTION의 failover 옵션 사용
CREATE SUBSCRIPTION my_sub CONNECTION '...' PUBLICATION my_pub WITH (failover = true);
```

스탠바이에서 필요한 설정:

- `sync_replication_slots` 활성화
- `primary_slot_name` 구성 (물리적 복제 슬롯)
- `hot_standby_feedback` 활성화
- `primary_conninfo`에 유효한 `dbname` 지정

#### 동기화 방법

| 방법 | 설명 |
|------|------|
| 자동 (권장) | `sync_replication_slots`를 통해 slotsync worker가 주기적으로 업데이트 |
| 수동 | `pg_sync_replication_slots()` 함수 사용 (테스트/디버깅용) |

#### 장애 조치 후 재개

```sql
-- 구독의 연결 정보를 새 프라이머리로 업데이트
ALTER SUBSCRIPTION my_sub CONNECTION 'host=new_primary ...';
```

> 주의: 스탠바이를 승격하기 전에 구독을 비활성화하세요. 그렇지 않으면 논리적 구독자가 이전 프라이머리에서 계속 데이터를 받아 데이터 불일치가 발생할 수 있습니다.

### 49.2.4. 출력 플러그인 (Output Plugins)

출력 플러그인(Output Plugin) 은 Write-Ahead Log의 내부 표현에서 복제 슬롯 소비자가 원하는 형식으로 데이터를 변환합니다.

출력 플러그인은 공유 라이브러리로 구현되며, 코어 코드를 수정하지 않고도 확장할 수 있습니다. 예제 플러그인은 `contrib/test_decoding`에서 확인할 수 있습니다.

### 49.2.5. 내보낸 스냅샷 (Exported Snapshots)

스트리밍 복제 인터페이스를 사용하여 새 복제 슬롯이 생성되면, 모든 변경 사항이 포함되는 시점 이후의 정확한 데이터베이스 상태를 보여주는 스냅샷이 내보내집니다.

#### 사용법

```sql
-- 슬롯 생성 시점의 데이터베이스 상태 읽기
SET TRANSACTION SNAPSHOT 'exported_snapshot_id';
```

이를 통해 다음이 가능합니다:

- 해당 시점의 데이터베이스 상태 덤프
- 변경 사항 손실 없이 슬롯 내용을 사용한 상태 업데이트

#### 스냅샷 내보내기 억제

스냅샷 내보내기가 필요 없는 애플리케이션의 경우:

```sql
SNAPSHOT 'nothing'
```

---

## 49.3. 스트리밍 복제 프로토콜 인터페이스

스트리밍 복제 프로토콜 인터페이스는 복제 연결을 통해서만 사용할 수 있는 논리적 디코딩 관리 명령을 제공합니다. 이러한 명령은 표준 SQL을 통해 사용할 수 없습니다.

### 핵심 명령어

#### CREATE_REPLICATION_SLOT

```
CREATE_REPLICATION_SLOT slot_name LOGICAL output_plugin
```

논리적 디코딩을 위한 새 복제 슬롯을 생성합니다. 변경 사항을 디코딩할 출력 플러그인을 지정해야 합니다.

#### DROP_REPLICATION_SLOT

```
DROP_REPLICATION_SLOT slot_name [WAIT]
```

기존 복제 슬롯을 제거합니다. 선택적 `WAIT` 절로 블로킹 동작을 제어할 수 있습니다.

#### START_REPLICATION

```
START_REPLICATION SLOT slot_name LOGICAL ...
```

복제 슬롯에서 변경 사항 스트리밍을 시작합니다. 디코딩된 논리적 변경 사항을 클라이언트로 스트리밍합니다.

### pg_recvlogical 유틸리티

`pg_recvlogical`은 스트리밍 복제 연결을 통해 논리적 디코딩을 제어하는 주요 명령줄 유틸리티입니다. 위 명령을 내부적으로 사용하여 복제 슬롯을 관리하고 변경 사항을 수신합니다.

```bash
# 주요 옵션
pg_recvlogical -d database --slot=slot_name --create-slot [--enable-two-phase]
pg_recvlogical -d database --slot=slot_name --start -f output_file
pg_recvlogical -d database --slot=slot_name --drop-slot
```

---

## 49.4. SQL 인터페이스

PostgreSQL은 논리적 디코딩과 상호작용하기 위한 SQL 수준 API를 제공합니다. 자세한 함수 정보는 섹션 9.28.6 "복제 관리 함수"를 참조하세요.

### 주요 함수

| 함수 | 설명 |
|------|------|
| `pg_create_logical_replication_slot()` | 논리적 복제 슬롯 생성 |
| `pg_drop_replication_slot()` | 복제 슬롯 삭제 |
| `pg_logical_slot_get_changes()` | 변경 사항 가져오기 (소비) |
| `pg_logical_slot_peek_changes()` | 변경 사항 미리보기 (비소비) |
| `pg_logical_slot_get_binary_changes()` | 바이너리 형식으로 변경 사항 가져오기 |
| `pg_logical_slot_peek_binary_changes()` | 바이너리 형식으로 변경 사항 미리보기 |
| `pg_replication_slot_advance()` | 슬롯 위치 전진 |

### 제한 사항

> 중요: 동기식 복제(섹션 26.2.8 참조)는 스트리밍 복제 인터페이스를 통해 사용되는 복제 슬롯에서만 지원됩니다. 함수 인터페이스 및 추가 비코어 인터페이스는 동기식 복제를 지원하지 않습니다.

---

## 49.5. 시스템 카탈로그

논리적 디코딩에 관련된 정보를 제공하는 시스템 카탈로그와 뷰입니다.

### 주요 뷰

| 뷰 | 설명 |
|----|------|
| `pg_replication_slots` | 복제 슬롯의 현재 상태 표시 (물리적 및 논리적) |
| `pg_stat_replication` | 스트리밍 복제 연결 정보 표시 |
| `pg_stat_replication_slots` | 논리적 복제 슬롯의 통계 정보 제공 |

### 예제 쿼리

```sql
-- 모든 복제 슬롯 정보 조회
SELECT
    slot_name,
    plugin,
    slot_type,
    database,
    active,
    restart_lsn,
    confirmed_flush_lsn
FROM pg_replication_slots;

-- 활성 복제 연결 조회
SELECT
    pid,
    usename,
    application_name,
    client_addr,
    state,
    sent_lsn,
    write_lsn,
    flush_lsn,
    replay_lsn
FROM pg_stat_replication;

-- 논리적 슬롯 통계 조회
SELECT
    slot_name,
    spill_txns,
    spill_count,
    spill_bytes,
    stream_txns,
    stream_count,
    stream_bytes
FROM pg_stat_replication_slots;
```

---

## 49.6. 출력 플러그인

출력 플러그인은 데이터베이스 변경 사항을 임의의 형식으로 디코딩할 수 있게 해주는 공유 라이브러리입니다.

### 49.6.1. 초기화 함수

출력 플러그인은 `_PG_output_plugin_init()` 함수를 제공해야 하며, 이 함수는 `OutputPluginCallbacks` 구조체를 채웁니다:

```c
typedef struct OutputPluginCallbacks {
    LogicalDecodeStartupCB startup_cb;
    LogicalDecodeBeginCB begin_cb;
    LogicalDecodeChangeCB change_cb;
    LogicalDecodeTruncateCB truncate_cb;
    LogicalDecodeCommitCB commit_cb;
    LogicalDecodeMessageCB message_cb;
    LogicalDecodeFilterByOriginCB filter_by_origin_cb;
    LogicalDecodeShutdownCB shutdown_cb;

    /* 2단계 커밋 콜백 */
    LogicalDecodeFilterPrepareCB filter_prepare_cb;
    LogicalDecodeBeginPrepareCB begin_prepare_cb;
    LogicalDecodePrepareCB prepare_cb;
    LogicalDecodeCommitPreparedCB commit_prepared_cb;
    LogicalDecodeRollbackPreparedCB rollback_prepared_cb;

    /* 스트리밍 콜백 */
    LogicalDecodeStreamStartCB stream_start_cb;
    LogicalDecodeStreamStopCB stream_stop_cb;
    LogicalDecodeStreamAbortCB stream_abort_cb;
    LogicalDecodeStreamPrepareCB stream_prepare_cb;
    LogicalDecodeStreamCommitCB stream_commit_cb;
    LogicalDecodeStreamChangeCB stream_change_cb;
    LogicalDecodeStreamMessageCB stream_message_cb;
    LogicalDecodeStreamTruncateCB stream_truncate_cb;
} OutputPluginCallbacks;
```

### 49.6.2. 콜백 함수

#### 필수 콜백

| 콜백 | 설명 |
|------|------|
| `begin_cb` | 트랜잭션 시작 시 호출 |
| `change_cb` | 행 수정 시 호출 |
| `commit_cb` | 트랜잭션 종료 시 호출 |

#### 선택적 콜백

| 콜백 | 설명 |
|------|------|
| `startup_cb` | 복제 슬롯 생성 또는 스트리밍 시작 시 호출 |
| `shutdown_cb` | 종료 시 호출 |
| `truncate_cb` | TRUNCATE 시 호출 |
| `message_cb` | 논리적 디코딩 메시지 수신 시 호출 |
| `filter_by_origin_cb` | 원본 기반 필터링 |

#### 스트리밍 지원 콜백 (활성화 시 필수)

- `stream_start_cb`, `stream_stop_cb` - 스트림 블록 경계
- `stream_abort_cb` - 스트리밍 트랜잭션 중단
- `stream_commit_cb` - 스트리밍 트랜잭션 커밋
- `stream_change_cb` - 스트리밍 변경 사항

#### 콜백 함수 시그니처

```c
// 시작 콜백
typedef void (*LogicalDecodeStartupCB) (
    struct LogicalDecodingContext *ctx,
    OutputPluginOptions *options,
    bool is_init
);

// 트랜잭션 시작 콜백
typedef void (*LogicalDecodeBeginCB) (
    struct LogicalDecodingContext *ctx,
    ReorderBufferTXN *txn
);

// 변경 콜백
typedef void (*LogicalDecodeChangeCB) (
    struct LogicalDecodingContext *ctx,
    ReorderBufferTXN *txn,
    Relation relation,
    ReorderBufferChange *change
);

// 커밋 콜백
typedef void (*LogicalDecodeCommitCB) (
    struct LogicalDecodingContext *ctx,
    ReorderBufferTXN *txn,
    XLogRecPtr commit_lsn
);

// TRUNCATE 콜백
typedef void (*LogicalDecodeTruncateCB) (
    struct LogicalDecodingContext *ctx,
    ReorderBufferTXN *txn,
    int nrelations,
    Relation relations[],
    ReorderBufferChange *change
);
```

### 49.6.3. 출력 모드

플러그인은 `OutputPluginOptions`를 통해 출력 형식을 선언합니다:

```c
typedef struct OutputPluginOptions {
    OutputPluginOutputType output_type;
    bool receive_rewrites;
} OutputPluginOptions;
```

| 출력 유형 | 설명 |
|-----------|------|
| `OUTPUT_PLUGIN_TEXTUAL_OUTPUT` | 서버 인코딩의 텍스트 데이터 |
| `OUTPUT_PLUGIN_BINARY_OUTPUT` | 바이너리 데이터 (기본값) |

`receive_rewrites`가 true이면 DDL 중 힙 재작성으로 인한 변경 사항을 수신합니다.

### 49.6.4. 출력 생성 함수

콜백 내에서 출력 버퍼에 쓰기:

```c
// 출력 작성 예제
OutputPluginPrepareWrite(ctx, last_write);
appendStringInfo(ctx->out, "BEGIN %u", txn->xid);
OutputPluginWrite(ctx, last_write);
```

`last_write` 매개변수는 이것이 콜백의 마지막 쓰기인지 나타냅니다.

### 플러그인 기능 및 제한

#### 허용됨

- 백엔드 출력 함수 호출
- `pg_catalog` 테이블 및 사용자 정의 카탈로그 테이블에 대한 읽기 전용 접근
- `systable_*` API를 통한 카탈로그 테이블 접근

#### 금지됨

- 트랜잭션 ID 할당
- 테이블에 쓰기
- DDL 변경
- `pg_current_xact_id()` 호출

### 트랜잭션 의미론

- 동시 트랜잭션은 커밋 순서로 디코딩됨
- 중단된 트랜잭션은 디코딩되지 않음
- 세이브포인트는 부모 트랜잭션에 통합됨
- 디스크에 플러시된 트랜잭션만 디코딩됨 (`synchronous_commit`의 영향을 받음)

---

## 49.7. 출력 라이터 (Output Writers)

출력 라이터를 사용하면 논리적 디코딩에 대한 추가 출력 방법을 추가할 수 있습니다.

### 필요한 함수

커스텀 출력 라이터를 구현하려면 세 가지 함수가 필요합니다:

1. WAL 읽기 함수 - Write-Ahead Log에서 읽기
2. 출력 준비 함수 - 출력 형식 준비
3. 출력 쓰기 함수 - 실제로 디코딩된 변경 사항 쓰기

구현 세부 사항은 `src/backend/replication/logical/logicalfuncs.c`를 참조하세요.

---

## 49.8. 동기식 복제 지원

논리적 디코딩을 사용하여 스트리밍 복제와 동일한 사용자 인터페이스로 동기식 복제 솔루션을 구축할 수 있습니다.

### 구현 요구 사항

1. 데이터를 스트리밍하기 위해 스트리밍 복제 인터페이스 (섹션 49.3)를 사용해야 합니다
2. 클라이언트는 스트리밍 복제 클라이언트와 마찬가지로 `Standby status update (F)` 메시지(섹션 54.4)를 보내야 합니다

### 중요 제한 사항

논리적 디코딩을 통해 변경 사항을 수신하는 동기식 복제본은 단일 데이터베이스 범위 내에서 작동합니다. `synchronous_standby_names`는 서버 전체 설정이므로, 둘 이상의 데이터베이스가 활발하게 사용되는 경우 이 기술은 제대로 작동하지 않습니다.

### 주의 사항: 카탈로그 테이블 데드락

동기식 복제 설정에서 트랜잭션이 사용자 카탈로그 테이블을 배타적으로 잠글 때 데드락이 발생할 수 있습니다.

#### 피해야 할 작업

데드락을 방지하려면 다음을 통해 사용자 카탈로그 테이블에 배타적 잠금을 설정하지 마세요:

| 작업 | 설명 |
|------|------|
| `LOCK` | 트랜잭션에서 `pg_class`에 대한 `LOCK` 발행 |
| `CLUSTER` | 트랜잭션에서 `pg_class`에 대한 `CLUSTER` 수행 |
| 2단계 커밋 | 2단계 트랜잭션 논리적 디코딩이 활성화된 상태에서 `pg_class`에 대한 `LOCK` 후 `PREPARE TRANSACTION` |
| 트리거 | 발행된 테이블에 트리거가 있는 경우 `pg_trigger`에 대한 `CLUSTER` 후 `PREPARE TRANSACTION` |
| `TRUNCATE` | 트랜잭션에서 사용자 카탈로그 테이블에 대한 `TRUNCATE` 실행 |

> 참고: 이러한 명령은 나열된 시스템 카탈로그 테이블뿐만 아니라 다른 카탈로그 테이블에서도 데드락을 유발할 수 있습니다.

---

## 49.9. 대규모 트랜잭션 스트리밍

PostgreSQL의 논리적 디코딩은 적용 지연을 줄이기 위해 대규모 트랜잭션의 스트리밍을 지원합니다.

### 기본 동작 vs 스트리밍

스트리밍 없이는 트랜잭션의 모든 디코딩된 변경 사항이 트랜잭션이 커밋될 때만 전송되어 대규모 트랜잭션의 복제가 크게 지연될 수 있습니다.

#### 기본 콜백 (커밋 시에만 호출)

- `begin_cb`
- `change_cb`
- `commit_cb`
- `message_cb`

#### 스트리밍 콜백 (점진적으로 호출)

필수:
- `stream_start_cb`, `stream_stop_cb`
- `stream_abort_cb`, `stream_commit_cb`
- `stream_change_cb`

선택적:
- `stream_message_cb`, `stream_truncate_cb`

2단계 커밋용:
- `stream_prepare_cb`, `commit_prepared_cb`, `rollback_prepared_cb`

### 스트리밍 작동 방식

변경 사항은 `stream_start_cb`와 `stream_stop_cb` 콜백으로 구분된 블록 단위로 스트리밍됩니다:

```
stream_start_cb(...);         // 첫 번째 블록 시작
  stream_change_cb(...);
  stream_change_cb(...);
  stream_message_cb(...);
  ...
stream_stop_cb(...);          // 첫 번째 블록 끝

stream_start_cb(...);         // 두 번째 블록 시작
  stream_change_cb(...);
  ...
stream_stop_cb(...);          // 두 번째 블록 끝

// 트랜잭션 커밋
stream_commit_cb(...);        // 일반 커밋
// 또는
stream_prepare_cb(...);       // 2단계 커밋 준비
commit_prepared_cb(...);      // 2단계 커밋 완료
```

### 스트리밍 트리거 조건

WAL에서 디코딩된 변경 사항의 총량(모든 진행 중인 트랜잭션에 걸쳐)이 `logical_decoding_work_mem` 제한을 초과하면 스트리밍이 자동으로 트리거됩니다. 그러면 가장 큰 최상위 트랜잭션이 스트리밍 대상으로 선택됩니다.

> 참고: 스트리밍이 활성화되어 있어도 불완전한 튜플(예: 해당 메인 테이블 삽입 없이 TOAST 테이블 삽입)이 발생하면 디스크로의 스필이 여전히 발생할 수 있습니다.

### 주요 보장 사항

- 변경 사항은 커밋 순서로 적용되어 비스트리밍 모드와 동일한 보장 유지
- 일관성을 희생하지 않고 대규모 트랜잭션의 적용 지연 감소

---

## 49.10. 2단계 커밋 지원

PostgreSQL의 논리적 디코딩은 추가 출력 플러그인 콜백을 통해 2단계 커밋 명령(`PREPARE TRANSACTION`, `COMMIT PREPARED`, `ROLLBACK PREPARED`)을 지원합니다.

### 기본 vs 2단계 디코딩

#### 2단계 지원 없이 (기본 콜백)

- `PREPARE TRANSACTION`은 무시됨
- `COMMIT PREPARED`는 일반 `COMMIT`으로 디코딩됨
- `ROLLBACK PREPARED`는 일반 `ROLLBACK`으로 디코딩됨

#### 2단계 지원 포함

- 변경 사항은 `PREPARE TRANSACTION` 시점에 디코딩되어 출력 플러그인에 전달됨
- 이는 트랜잭션이 커밋될 때만 변경 사항이 전달되는 기본 디코딩과 다름

### 필수 콜백

2단계 커밋 디코딩을 지원하려면 출력 플러그인이 다음 콜백을 제공해야 합니다:

| 콜백 | 설명 |
|------|------|
| `begin_prepare_cb` | 준비된 트랜잭션 시작 표시 |
| `prepare_cb` | 트랜잭션이 준비될 때 호출 |
| `commit_prepared_cb` | 준비된 트랜잭션이 커밋될 때 호출 |
| `rollback_prepared_cb` | 준비된 트랜잭션이 롤백될 때 호출 |
| `stream_prepare_cb` | 스트리밍되는 준비된 트랜잭션용 |

### 선택적 콜백

| 콜백 | 설명 |
|------|------|
| `filter_prepare_cb` | `gid`(전역 ID) 패턴 매칭 또는 `xid`(트랜잭션 ID) 조회를 통해 특정 트랜잭션의 2단계 디코딩을 필터링 |

### 중요 주의 사항

1. 블로킹 위험: 준비된 트랜잭션이 카탈로그 테이블을 배타적으로 잠근 경우, 주 트랜잭션이 커밋될 때까지 디코딩 준비 작업이 블로킹될 수 있습니다.

2. 데드락 위험: 이 기능을 기반으로 구축된 분산 2단계 커밋 솔루션은 준비된 트랜잭션이 카탈로그 테이블을 잠그면 데드락이 발생할 수 있습니다. 사용자는 이러한 트랜잭션에서 카탈로그 테이블에 대한 명시적 `LOCK` 명령을 피해야 합니다.

---

## 부록: 빠른 참조

### 구성 매개변수

| 매개변수 | 설명 | 기본값 |
|----------|------|--------|
| `wal_level` | WAL 기록 수준 (논리적 디코딩에는 `logical` 필요) | `replica` |
| `max_replication_slots` | 최대 복제 슬롯 수 | `10` |
| `max_prepared_transactions` | 최대 준비된 트랜잭션 수 (2단계 커밋용) | `0` |
| `logical_decoding_work_mem` | 논리적 디코딩 작업 메모리 | `64MB` |

### 주요 SQL 함수

```sql
-- 슬롯 관리
pg_create_logical_replication_slot(slot_name, plugin [, temporary, twophase, failover])
pg_drop_replication_slot(slot_name)

-- 변경 사항 조회
pg_logical_slot_get_changes(slot_name, upto_lsn, upto_nchanges [, options])
pg_logical_slot_peek_changes(slot_name, upto_lsn, upto_nchanges [, options])
pg_logical_slot_get_binary_changes(slot_name, upto_lsn, upto_nchanges [, options])
pg_logical_slot_peek_binary_changes(slot_name, upto_lsn, upto_nchanges [, options])

-- 슬롯 제어
pg_replication_slot_advance(slot_name, upto_lsn)
```

### pg_recvlogical 주요 옵션

```bash
# 슬롯 생성
--create-slot          # 슬롯 생성
--enable-two-phase     # 2단계 커밋 활성화

# 스트리밍
--start                # 스트리밍 시작
-f filename            # 출력 파일 (-f - 는 stdout)

# 슬롯 관리
--drop-slot            # 슬롯 삭제

# 연결
-d database            # 데이터베이스 지정
--slot=name            # 슬롯 이름
```

---

## 참고 자료

- [PostgreSQL 공식 문서 - Logical Decoding](https://www.postgresql.org/docs/current/logicaldecoding.html)
- [PostgreSQL 공식 문서 - Streaming Replication Protocol](https://www.postgresql.org/docs/current/protocol-replication.html)
- [PostgreSQL 공식 문서 - Replication Management Functions](https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-REPLICATION)
- [contrib/test_decoding](https://www.postgresql.org/docs/current/test-decoding.html) - 예제 출력 플러그인
