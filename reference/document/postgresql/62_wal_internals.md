# PostgreSQL WAL 내부 구현 (WAL Internals)

## 개요

이 문서는 PostgreSQL의 Write-Ahead Logging(WAL) 시스템의 내부 구현에 대해 다룬다. WAL은 PostgreSQL의 데이터 무결성과 복구 메커니즘의 핵심이다.

---

## 1. WAL 기본 원리

### 1.1 Write-Ahead Logging 개념

WAL의 핵심 원칙은 다음과 같다:

> 데이터 파일(테이블과 인덱스가 저장되는 곳)에 대한 변경은 해당 변경을 설명하는 WAL 레코드가 영구 저장소에 플러시된 후에만 기록되어야 한다.

이 원칙을 통해 다음과 같은 이점을 얻는다:

- 트랜잭션 커밋 시 데이터 페이지를 플러시할 필요가 없음: WAL 레코드가 먼저 기록되므로 데이터 페이지의 즉각적인 플러시가 불필요
- 충돌 복구(Crash Recovery): 충돌 시 WAL 레코드를 재생하여 적용되지 않은 변경사항을 복구 (roll-forward recovery 또는 REDO)

### 1.2 성능상의 이점

```
[기존 방식]
트랜잭션 커밋 → 모든 수정된 데이터 페이지 디스크에 플러시 → 느림

[WAL 방식]
트랜잭션 커밋 → WAL 파일만 디스크에 플러시 → 빠름
                 (순차 쓰기, 단일 fsync)
```

주요 이점:
- 디스크 쓰기 횟수 대폭 감소
- 순차 쓰기로 sync 작업 속도 향상
- 여러 작은 트랜잭션을 단일 `fsync`로 커밋 가능

---

## 2. WAL 저장 구조

### 2.1 WAL 세그먼트 파일

WAL 파일은 데이터 디렉토리 아래 `pg_wal` 디렉토리에 저장된다.

```
$PGDATA/pg_wal/
├── 000000010000000000000001
├── 000000010000000000000002
├── 000000010000000000000003
└── ...
```

세그먼트 파일 특성:
- 기본 크기: 16MB (initdb의 `--wal-segsize` 옵션으로 구성 가능)
- 순차적으로 번호가 매겨짐
- 번호는 절대 순환하지 않음 (고갈되는 데 매우 오랜 시간이 걸림)

### 2.2 WAL 페이지 구조

각 세그먼트 파일은 페이지로 나뉜다:

```
┌─────────────────────────────────────────────────────────────────┐
│                    WAL 세그먼트 파일 (16MB)                      │
├─────────────────────────────────────────────────────────────────┤
│  페이지 0    │  페이지 1    │  페이지 2    │ ... │  페이지 N    │
│  (8KB)       │  (8KB)       │  (8KB)       │     │  (8KB)       │
└─────────────────────────────────────────────────────────────────┘
```

- 페이지 크기: 기본 8KB (`--with-wal-blocksize` configure 옵션으로 구성)

### 2.3 페이지 헤더 구조

첫 번째 페이지: XLogLongPageHeaderData

```c
typedef struct XLogLongPageHeaderData
{
    XLogPageHeaderData std;        /* 표준 헤더 필드 */
    uint64      xlp_sysid;         /* pg_control의 시스템 식별자 */
    uint32      xlp_seg_size;      /* 교차 확인 값 */
    uint32      xlp_xlog_blcksz;   /* 교차 확인 값 */
} XLogLongPageHeaderData;
```

이후 페이지: XLogPageHeaderData

```c
typedef struct XLogPageHeaderData
{
    uint16      xlp_magic;         /* 매직 값 (0xD113) - 정확성 검증용 */
    uint16      xlp_info;          /* 플래그 비트 */
    TimeLineID  xlp_tli;           /* 페이지의 첫 레코드의 TimeLineID */
    XLogRecPtr  xlp_pageaddr;      /* 현재 페이지의 XLOG 주소 */
    uint32      xlp_rem_len;       /* 이전 페이지에서 이어지는 바이트 수 */
} XLogPageHeaderData;
```

---

## 3. Log Sequence Number (LSN)

### 3.1 LSN 개념

LSN은 WAL의 바이트 오프셋을 나타내는 단조 증가하는 값이다.

```c
typedef uint64 XLogRecPtr;  /* LSN을 나타내는 타입 */
```

LSN 용도:
- WAL 위치 간의 데이터 양 계산
- 복제 진행 상황 측정
- 복구 위치 추적

### 3.2 LSN 데이터 타입

PostgreSQL에서 LSN은 `pg_lsn` 데이터 타입으로 표현된다:

```sql
-- 현재 WAL 위치 확인
SELECT pg_current_wal_lsn();

-- 두 LSN 간의 차이 계산 (바이트 단위)
SELECT pg_wal_lsn_diff('0/1A2B3C4D', '0/1A2B3C00');

-- 페이지의 LSN 확인
SELECT lsn FROM page_header(get_raw_page('mytable', 0));
```

---

## 4. XLOG 레코드 형식 (XLOG Record Format)

### 4.1 전체 레코드 레이아웃

XLOG 레코드의 전체 구조는 `access/xlogrecord.h`에 정의되어 있다:

```
┌─────────────────────────────────────────────────────────────────┐
│                      XLOG 레코드 전체 구조                        │
├─────────────────────────────────────────────────────────────────┤
│  고정 크기 헤더 (XLogRecord)                                     │
├─────────────────────────────────────────────────────────────────┤
│  XLogRecordBlockHeader (0개 이상)                                │
├─────────────────────────────────────────────────────────────────┤
│  XLogRecordDataHeader[Short|Long] (0개 또는 1개)                 │
├─────────────────────────────────────────────────────────────────┤
│  블록 데이터 (block data)                                        │
├─────────────────────────────────────────────────────────────────┤
│  메인 데이터 (main data)                                         │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 XLogRecord 구조체

모든 XLOG 레코드의 헤더를 정의하는 핵심 구조체:

```c
typedef struct XLogRecord
{
    uint32      xl_tot_len;    /* 전체 레코드 길이 */
    TransactionId xl_xid;      /* 트랜잭션 ID */
    XLogRecPtr  xl_prev;       /* 이전 레코드에 대한 포인터 */
    uint8       xl_info;       /* 플래그 비트 (리소스 관리자별 정보) */
    RmgrId      xl_rmid;       /* 리소스 관리자 식별자 */
    /* 2바이트 패딩 */
    pg_crc32c   xl_crc;        /* CRC-32C 체크섬 */
} XLogRecord;
```

필드 설명:

| 필드 | 설명 |
|------|------|
| `xl_tot_len` | 레코드 전체 길이 (헤더 + 데이터) |
| `xl_xid` | 레코드를 생성한 트랜잭션 ID |
| `xl_prev` | 이전 WAL 레코드의 위치 (연결 리스트 형태) |
| `xl_info` | 작업 유형을 나타내는 플래그 비트 |
| `xl_rmid` | 리소스 관리자 식별자 |
| `xl_crc` | 데이터 무결성을 위한 CRC-32C 체크섬 |

### 4.3 XLogRecordBlockHeader 구조체

블록 참조를 설명하는 구조체:

```c
typedef struct XLogRecordBlockHeader
{
    uint8       id;             /* 블록 참조 ID */
    uint8       fork_flags;     /* 포크 번호 및 플래그 */
    uint16      data_length;    /* 페이지 이미지를 제외한 페이로드 크기 */
    /* BKPBLOCK_HAS_IMAGE인 경우 XLogRecordBlockImageHeader가 뒤따름 */
    /* BKPBLOCK_SAME_REL이 아닌 경우 RelFileLocator가 뒤따름 */
    /* BlockNumber가 항상 뒤따름 */
} XLogRecordBlockHeader;
```

플래그 정의:

```c
#define BKPBLOCK_FORK_MASK   0x0F  /* 포크 비트 마스크 */
#define BKPBLOCK_FLAG_MASK   0xF0  /* 플래그 비트 마스크 */
#define BKPBLOCK_HAS_IMAGE   0x10  /* 전체 페이지 이미지 포함 */
#define BKPBLOCK_HAS_DATA    0x20  /* 데이터 포함 */
#define BKPBLOCK_WILL_INIT   0x40  /* 복구 시 페이지 재초기화 */
#define BKPBLOCK_SAME_REL    0x80  /* 이전 블록과 동일한 릴레이션 */
```

### 4.4 XLogRecordBlockImageHeader 구조체

전체 페이지 이미지(Full Page Image, FPI)를 저장할 때 사용:

```c
typedef struct XLogRecordBlockImageHeader
{
    uint16      length;        /* 페이지 이미지 바이트 수 */
    uint16      hole_offset;   /* "hole" 이전의 바이트 수 */
    uint8       bimg_info;     /* 압축 및 상태 플래그 */
} XLogRecordBlockImageHeader;
```

bimg_info 플래그:

```c
#define BKPIMAGE_HAS_HOLE     0x01  /* 페이지에 hole이 있음 */
#define BKPIMAGE_IS_COMPRESSED 0x02  /* 이미지가 압축됨 */
#define BKPIMAGE_APPLY        0x04  /* 복구 시 적용해야 함 */
```

### 4.5 압축 헤더 구조체

압축이 적용된 경우:

```c
typedef struct XLogRecordBlockCompressHeader
{
    uint16      hole_length;   /* 사용되지 않는 섹션의 바이트 수 */
} XLogRecordBlockCompressHeader;
```

지원되는 압축 방식:
- PGLZ (PostgreSQL 기본 압축)
- LZ4
- ZSTD

### 4.6 데이터 헤더 구조체

짧은 형식 (Short):

```c
typedef struct XLogRecordDataHeaderShort
{
    uint8       id;           /* XLR_BLOCK_ID_DATA_SHORT */
    uint8       data_length;  /* 1바이트 길이 */
} XLogRecordDataHeaderShort;
```

긴 형식 (Long):

```c
typedef struct XLogRecordDataHeaderLong
{
    uint8       id;           /* XLR_BLOCK_ID_DATA_LONG */
    /* 4바이트 길이가 뒤따름 */
} XLogRecordDataHeaderLong;
```

---

## 5. WAL 리소스 관리자 (Resource Managers)

### 5.1 리소스 관리자 개념

리소스 관리자(Resource Manager, rmgr)는 WAL 기능과 관련된 작업 집합이다. 각 리소스 관리자는 특정 유형의 WAL 레코드 쓰기와 재생을 담당한다.

### 5.2 내장 리소스 관리자

PostgreSQL에 포함된 주요 리소스 관리자:

| 리소스 관리자 | 설명 |
|--------------|------|
| `RM_XLOG` | XLOG 자체 관리 (체크포인트 등) |
| `RM_XACT` | 트랜잭션 관리 |
| `RM_SMGR` | 스토리지 관리자 |
| `RM_CLOG` | 커밋 로그 |
| `RM_DBASE` | 데이터베이스 작업 |
| `RM_TBLSPC` | 테이블스페이스 |
| `RM_MULTIXACT` | 다중 트랜잭션 |
| `RM_RELMAP` | 릴레이션 맵 |
| `RM_STANDBY` | 스탠바이 관련 |
| `RM_HEAP` | 힙 테이블 작업 (INSERT, UPDATE, DELETE) |
| `RM_HEAP2` | 힙 테이블 추가 작업 (VACUUM 등) |
| `RM_BTREE` | B-tree 인덱스 |
| `RM_HASH` | 해시 인덱스 |
| `RM_GIN` | GIN 인덱스 |
| `RM_GIST` | GiST 인덱스 |
| `RM_SEQ` | 시퀀스 |
| `RM_SPGIST` | SP-GiST 인덱스 |
| `RM_BRIN` | BRIN 인덱스 |

### 5.3 RmgrData 구조체

각 리소스 관리자는 `RmgrData` 구조체로 정의된다:

```c
typedef struct RmgrData
{
    const char *rm_name;                                    /* 리소스 관리자 이름 */
    void        (*rm_redo) (XLogReaderState *record);       /* REDO 콜백 함수 */
    void        (*rm_desc) (StringInfo buf,
                            XLogReaderState *record);       /* 레코드 설명 */
    const char *(*rm_identify) (uint8 info);                /* 레코드 이름 반환 */
    void        (*rm_startup) (void);                       /* 시작 시 호출 */
    void        (*rm_cleanup) (void);                       /* 정리 함수 */
    void        (*rm_mask) (char *pagedata,
                            BlockNumber blkno);             /* 페이지 마스킹 */
    void        (*rm_decode) (struct LogicalDecodingContext *ctx,
                              struct XLogRecordBuffer *buf); /* 논리적 디코딩 */
} RmgrData;
```

주요 콜백 함수:

| 콜백 | 역할 |
|------|------|
| `rm_redo` | 복구 시 WAL 레코드 적용 |
| `rm_desc` | 레코드에 대한 추가 세부 정보 제공 |
| `rm_identify` | xl_info 기반으로 레코드 이름 반환 |
| `rm_startup` | 시작 시 초기화 |
| `rm_cleanup` | 정리 작업 |
| `rm_mask` | `wal_consistency_checking`에서 플래그 제외할 비트 마스킹 |
| `rm_decode` | 사용자 정의 WAL 레코드의 논리적 디코딩 처리 |

### 5.4 사용자 정의 리소스 관리자

PostgreSQL 15부터 확장은 자체 사용자 정의 리소스 관리자를 등록할 수 있다:

```c
/* 사용자 정의 리소스 관리자 등록 */
extern void RegisterCustomRmgr(RmgrId rmid, const RmgrData *rmgr);
```

등록 요구사항:

1. 확장의 `_PG_init()` 함수에서 호출
2. 고유한 리소스 관리자 ID 사용 (개발 시 `RM_EXPERIMENTAL_ID` 사용)
3. `shared_preload_libraries`에 추가하여 조기 로딩

예제 코드:

```c
#include "access/xlog.h"
#include "access/xlog_internal.h"

/* 리소스 관리자 정의 */
static const RmgrData my_rmgr = {
    .rm_name = "my_extension",
    .rm_redo = my_redo,
    .rm_desc = my_desc,
    .rm_identify = my_identify,
    .rm_startup = NULL,
    .rm_cleanup = NULL,
    .rm_mask = NULL,
    .rm_decode = my_decode
};

/* REDO 콜백 구현 */
static void
my_redo(XLogReaderState *record)
{
    uint8 info = XLogRecGetInfo(record) & ~XLR_INFO_MASK;

    switch (info)
    {
        case MY_XLOG_INSERT:
            /* INSERT 작업 재생 */
            break;
        case MY_XLOG_UPDATE:
            /* UPDATE 작업 재생 */
            break;
        /* ... */
    }
}

/* 확장 초기화 */
void
_PG_init(void)
{
    RegisterCustomRmgr(MY_RMGR_ID, &my_rmgr);
}
```

---

## 6. WAL 레코드 작성 (Writing XLOG Records)

### 6.1 XLogInsert 프로세스

WAL 레코드 작성 과정:

```
┌─────────────────────────────────────────────────────────────────┐
│                    WAL 레코드 작성 흐름                           │
├─────────────────────────────────────────────────────────────────┤
│  1. heap_insert() 호출                                           │
│          ↓                                                       │
│  2. XLogBeginInsert() - 레코드 구성 시작                         │
│          ↓                                                       │
│  3. XLogRegisterData() - 메인 데이터 등록                        │
│          ↓                                                       │
│  4. XLogRegisterBuffer() - 버퍼 등록                             │
│          ↓                                                       │
│  5. XLogInsert() - WAL 버퍼에 레코드 작성                        │
│          ↓                                                       │
│  6. 페이지의 pd_lsn 업데이트                                     │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 WAL 버퍼

WAL 레코드는 먼저 WAL 버퍼(공유 메모리)에 기록된다:

```c
/* WAL 버퍼 크기 설정 */
wal_buffers = 64MB  /* 기본값: -1 (자동 계산) */
```

WAL 버퍼 플러시 조건:
- 트랜잭션 커밋 또는 중단
- WAL 버퍼가 가득 참
- WAL writer 프로세스가 주기적으로 실행

### 6.3 XLogFlush 프로세스

```c
/* WAL 플러시 메소드 */
wal_sync_method = fdatasync  /* 기본값 */
```

지원되는 동기화 메소드:

| 메소드 | 설명 |
|--------|------|
| `open_datasync` | O_DSYNC로 WAL 파일 열기 |
| `fdatasync` | 커밋마다 fdatasync() 호출 |
| `fsync` | 커밋마다 fsync() 호출 |
| `fsync_writethrough` | 커밋마다 fsync() 호출 (디스크 캐시 우회) |
| `open_sync` | O_SYNC로 WAL 파일 열기 |

### 6.4 INSERT 예제 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│  INSERT INTO mytable VALUES (1, 'test');                         │
├─────────────────────────────────────────────────────────────────┤
│  1. ExtendCLOG()                                                 │
│     └─ 트랜잭션 상태를 "IN_PROGRESS"로 표시                      │
│                                                                  │
│  2. heap_insert()                                                │
│     └─ XLOG 레코드 생성                                          │
│     └─ XLogInsert()로 WAL 버퍼에 기록 (LSN_1)                    │
│     └─ 페이지의 pd_lsn을 LSN_1으로 업데이트                      │
│                                                                  │
│  3. finish_xact_command()                                        │
│     └─ 커밋 레코드 생성                                          │
│     └─ XLogInsert()로 WAL 버퍼에 기록 (LSN_2)                    │
│                                                                  │
│  4. XLogFlush()                                                  │
│     └─ WAL 버퍼의 모든 레코드를 WAL 세그먼트 파일에 플러시        │
│                                                                  │
│  5. TransactionIdCommitTree()                                    │
│     └─ CLOG에서 트랜잭션 상태를 "COMMITTED"로 변경               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. 전체 페이지 쓰기 (Full Page Writes)

### 7.1 개념

전체 페이지 쓰기(Full Page Writes, FPW)는 각 체크포인트 후 페이지의 첫 번째 변경 시 전체 페이지 이미지를 WAL에 기록하는 것이다.

```c
/* 전체 페이지 쓰기 활성화 (기본값: on) */
full_page_writes = on
```

### 7.2 부분 페이지 쓰기 문제

PostgreSQL은 일반적으로 8KB(16개의 512바이트 섹터) 페이지를 한 번에 쓴다. 전원 손실 시:

```
┌─────────────────────────────────────────────────────────────────┐
│  8KB 페이지 (16 섹터)                                            │
├────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┤
│ S1 │ S2 │ S3 │ S4 │ S5 │ S6 │ S7 │ S8 │ S9 │... │S15 │S16 │    │
├────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┤
│  ✓    ✓    ✓    ✓    ✓    ✗    ✗    ✗    ✗        ✗    ✗       │
│  ^^^^^^^^^^^^^^^^^^^^^^^^^                                       │
│  쓰기 완료                   전원 손실로 쓰기 실패                │
└─────────────────────────────────────────────────────────────────┘
```

해결책: 전체 페이지 이미지를 WAL에 저장하여 부분적으로 기록된 페이지를 복구 시 복원

### 7.3 백업 블록 (Backup Block)

체크포인트 후 첫 번째 변경 시:

```c
/* WAL 레코드에 전체 페이지 이미지 포함 */
typedef struct XLogRecordBlockImageHeader
{
    uint16      length;        /* 페이지 이미지 바이트 수 */
    uint16      hole_offset;   /* "hole" 이전 바이트 수 */
    uint8       bimg_info;     /* 플래그 */
} XLogRecordBlockImageHeader;
```

---

## 8. 체크포인트 처리 (Checkpoint Processing)

### 8.1 체크포인트 발생 조건

체크포인터(checkpointer) 백그라운드 프로세스가 체크포인트를 시작하는 경우:

1. `checkpoint_timeout` 간격 만료 (기본값: 5분)
2. WAL 파일 총 크기가 `max_wal_size` 초과 (기본값: 1GB)
3. 서버가 smart 또는 fast 모드로 중지
4. 슈퍼유저가 수동으로 `CHECKPOINT` 명령 실행

### 8.2 체크포인트 처리 단계

```
┌─────────────────────────────────────────────────────────────────┐
│                     체크포인트 처리 단계                          │
├─────────────────────────────────────────────────────────────────┤
│  1. REDO 포인트 저장                                             │
│     └─ 체크포인트 시작 시점의 XLOG 레코드 위치                    │
│                                                                  │
│  2. 공유 메모리 플러시                                           │
│     └─ CLOG 등 모든 데이터를 스토리지에 전송                      │
│                                                                  │
│  3. 더티 페이지 쓰기                                             │
│     └─ 버퍼의 더티 페이지를 점진적으로 디스크에 기록              │
│                                                                  │
│  4. 체크포인트 레코드 생성                                       │
│     └─ CheckPoint 구조체를 WAL 버퍼에 기록                       │
│                                                                  │
│  5. pg_control 업데이트                                          │
│     └─ 체크포인트 위치 및 기본 정보 기록                          │
└─────────────────────────────────────────────────────────────────┘
```

### 8.3 CheckPoint 구조체

```c
typedef struct CheckPoint
{
    XLogRecPtr  redo;              /* REDO 시작 위치 */
    TimeLineID  ThisTimeLineID;   /* 현재 타임라인 ID */
    TimeLineID  PrevTimeLineID;   /* 이전 타임라인 ID */
    bool        fullPageWrites;   /* 전체 페이지 쓰기 활성화 여부 */
    FullTransactionId nextXid;    /* 다음 트랜잭션 ID */
    Oid         nextOid;          /* 다음 OID */
    MultiXactId nextMulti;        /* 다음 MultiXactId */
    MultiXactOffset nextMultiOffset;
    TransactionId oldestXid;      /* 가장 오래된 트랜잭션 ID */
    Oid         oldestXidDB;      /* oldestXid가 속한 DB */
    MultiXactId oldestMulti;      /* 가장 오래된 MultiXactId */
    Oid         oldestMultiDB;    /* oldestMulti가 속한 DB */
    pg_time_t   time;             /* 체크포인트 시간 */
    TransactionId oldestCommitTsXid;
    TransactionId newestCommitTsXid;
    TransactionId oldestActiveXid;
} CheckPoint;
```

### 8.4 pg_control 파일

`pg_control` 파일은 데이터베이스 복구에 필수적인 정보를 저장:

```bash
# pg_controldata로 확인
$ pg_controldata $PGDATA

pg_control version number:            1300
Catalog version number:               202107181
Database system identifier:           7123456789012345678
Database cluster state:               in production
Latest checkpoint location:           0/1A2B3C4D
Latest checkpoint's REDO location:    0/1A2B3C00
Latest checkpoint's TimeLineID:       1
...
```

---

## 9. 데이터베이스 복구 (Database Recovery)

### 9.1 복구 프로세스 개요

PostgreSQL은 REDO 로그 기반 복구를 구현한다:

```
┌─────────────────────────────────────────────────────────────────┐
│                      복구 프로세스 흐름                           │
├─────────────────────────────────────────────────────────────────┤
│  1. pg_control 파일 읽기                                         │
│     └─ 상태가 "in production"이면 복구 모드 활성화               │
│     └─ 상태가 "shut down"이면 정상 시작                          │
│                                                                  │
│  2. 최신 체크포인트 레코드 읽기                                  │
│     └─ REDO 포인트 위치 추출                                     │
│     └─ 손상된 경우 이전 체크포인트 시도                           │
│                                                                  │
│  3. XLOG 재생                                                    │
│     └─ REDO 포인트부터 순차적으로 XLOG 레코드 읽기 및 재생       │
│     └─ 최신 WAL 세그먼트 끝까지 계속                              │
│                                                                  │
│  4. 복구 완료                                                    │
│     └─ 데이터베이스 일관된 상태로 복원                           │
└─────────────────────────────────────────────────────────────────┘
```

### 9.2 LSN 비교 로직

복구 시 두 가지 레코드 유형을 구분:

백업 블록 (Backup Blocks):
- 해당 테이블 페이지에 무조건 덮어쓰기
- 멱등성(Idempotent) 작업 - 반복 재생 가능

비백업 블록 (Non-Backup Blocks):
- 레코드의 LSN이 페이지의 `pd_lsn`보다 큰 경우에만 재생
- 비멱등성(Non-idempotent) 작업 - 잘못된 재생 시 데이터 불일치 위험

```
┌─────────────────────────────────────────────────────────────────┐
│                      LSN 비교 로직                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  if (레코드가 백업 블록) {                                       │
│      페이지에 무조건 덮어쓰기;                                   │
│  } else {                                                        │
│      if (레코드의 LSN > 페이지의 pd_lsn) {                       │
│          레코드 재생;                                            │
│      } else {                                                    │
│          스킵 (이미 적용됨);                                     │
│      }                                                           │
│  }                                                               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 9.3 복구 예제

```
REDO 포인트: LSN_1

WAL 레코드들:
┌────────┬─────────┬─────────────┬────────────────────────────┐
│ LSN    │ 유형    │ 페이지 LSN  │ 동작                        │
├────────┼─────────┼─────────────┼────────────────────────────┤
│ LSN_1  │ INSERT  │ LSN_0       │ 재생 (LSN_1 > LSN_0)       │
│ LSN_2  │ UPDATE  │ LSN_1       │ 재생 (LSN_2 > LSN_1)       │
│ LSN_3  │ FPI     │ -           │ 무조건 적용 (백업 블록)     │
│ LSN_4  │ DELETE  │ LSN_3       │ 재생 (LSN_4 > LSN_3)       │
└────────┴─────────┴─────────────┴────────────────────────────┘
```

---

## 10. Generic WAL 레코드

### 10.1 개념

Generic WAL은 확장이 페이지 변경을 WAL에 기록할 수 있는 내장 메커니즘이다. `access/generic_xlog.h`에 정의되어 있다.

제한사항: Generic WAL 레코드는 논리적 디코딩(Logical Decoding) 중에 무시된다.

### 10.2 API 함수

```c
/* 1. Generic WAL 레코드 구성 시작 */
GenericXLogState *state = GenericXLogStart(relation);

/* 2. 버퍼 등록 */
Page page = GenericXLogRegisterBuffer(state, buffer, flags);

/* 3. 페이지 수정 */
/* 반환된 페이지 복사본만 수정 */

/* 4. 변경 적용 및 WAL 레코드 발행 */
GenericXLogFinish(state);

/* 또는 변경 취소 */
GenericXLogAbort(state);
```

### 10.3 사용 예제

```c
#include "access/generic_xlog.h"
#include "storage/bufmgr.h"

void
my_extension_insert(Relation rel, ItemPointer tid, Datum value)
{
    Buffer      buffer;
    Page        page;
    GenericXLogState *state;

    /* 버퍼 잠금 획득 */
    buffer = ReadBuffer(rel, P_NEW);
    LockBuffer(buffer, BUFFER_LOCK_EXCLUSIVE);

    /* Generic WAL 시작 */
    state = GenericXLogStart(rel);

    /* 버퍼 등록 (새 페이지이므로 FULL_IMAGE 플래그) */
    page = GenericXLogRegisterBuffer(state, buffer,
                                      GENERIC_XLOG_FULL_IMAGE);

    /* 페이지 초기화 및 데이터 삽입 */
    PageInit(page, BufferGetPageSize(buffer), 0);
    /* ... 데이터 삽입 로직 ... */

    /* WAL 레코드 발행 및 버퍼 해제 */
    GenericXLogFinish(state);
    UnlockReleaseBuffer(buffer);
}
```

### 10.4 플래그

```c
/* 전체 페이지 이미지 포함 (델타 업데이트 대신) */
#define GENERIC_XLOG_FULL_IMAGE    0x01
```

### 10.5 주요 제약사항

| 측면 | 세부사항 |
|------|----------|
| 버퍼 수정 | `GenericXLogRegisterBuffer()`에서 반환된 복사본만 수정 |
| 잠금 | 등록부터 `GenericXLogFinish()` 이후까지 배타적 잠금 유지 |
| 최대 버퍼 | `MAX_GENERIC_XLOG_PAGES` 제한 |
| 페이지 레이아웃 | `pd_lower`와 `pd_upper` 사이에 유용한 데이터가 없다고 가정 |
| 더티 마킹 | `GenericXLogFinish()`가 자동으로 버퍼를 더티로 표시 |

---

## 11. WAL 관련 설정 파라미터

### 11.1 체크포인트 관련

```ini
# 체크포인트 간격 (기본값: 5분)
checkpoint_timeout = 5min

# WAL 파일 최대 크기 (기본값: 1GB)
max_wal_size = 1GB

# WAL 파일 최소 크기
min_wal_size = 80MB

# 체크포인트 완료 목표 (기본값: 0.9)
checkpoint_completion_target = 0.9
```

### 11.2 WAL 버퍼 관련

```ini
# WAL 버퍼 크기 (기본값: -1, 자동 계산)
wal_buffers = -1

# 전체 페이지 쓰기 (기본값: on)
full_page_writes = on

# WAL 압축 (기본값: off)
wal_compression = off
```

### 11.3 커밋 관련

```ini
# 커밋 지연 (마이크로초)
commit_delay = 0

# 커밋 시블링 임계값
commit_siblings = 5

# fsync 활성화 (기본값: on)
fsync = on

# WAL 동기화 메소드
wal_sync_method = fdatasync
```

### 11.4 WAL 레벨

```ini
# WAL 레벨 (기본값: replica)
# minimal: 충돌 복구만 지원
# replica: WAL 아카이빙 및 복제 지원
# logical: 논리적 복제 지원
wal_level = replica
```

---

## 12. 데이터 무결성 메커니즘

### 12.1 체크섬

PostgreSQL은 여러 체크섬 메커니즘을 통해 데이터 무결성을 보호:

| 구성요소 | 보호 방식 |
|---------|----------|
| WAL 레코드 | CRC-32C (32비트) 체크 |
| 데이터 페이지 | 기본적으로 체크섬 적용 |
| 전체 페이지 이미지 | WAL 레코드에서 항상 체크섬 보호 |
| 2단계 파일 | `pg_twophase`의 상태 파일에 CRC-32C 적용 |

### 12.2 스토리지 하드웨어 고려사항

Linux에서 쓰기 캐시 관리:

```bash
# IDE/SATA 드라이브
hdparm -I           # 쓰기 캐시 활성화 여부 확인
hdparm -W 0         # 쓰기 캐시 비활성화

# SCSI 드라이브
sdparm --get=WCE    # 쓰기 캐시 상태 확인
sdparm --clear=WCE  # 쓰기 캐시 비활성화
```

### 12.3 pg_test_fsync

I/O 서브시스템 성능 테스트:

```bash
$ pg_test_fsync

5 seconds per test
O_DIRECT supported on this platform for open_datasync and open_sync.

Compare file sync methods using one 8kB write:
(in wal_sync_method preference order, except fdatasync is Linux's default)
        open_datasync                      4535.775 ops/sec     220 usecs/op
        fdatasync                          4467.020 ops/sec     224 usecs/op
        fsync                              4393.283 ops/sec     228 usecs/op
        fsync_writethrough                              n/a
        open_sync                          4419.012 ops/sec     226 usecs/op
```

---

## 13. 모니터링 및 디버깅

### 13.1 WAL 관련 시스템 뷰

```sql
-- 현재 WAL 상태 확인
SELECT pg_current_wal_lsn(),
       pg_current_wal_insert_lsn(),
       pg_current_wal_flush_lsn();

-- WAL 통계 확인
SELECT * FROM pg_stat_wal;

-- 체크포인터 통계
SELECT * FROM pg_stat_checkpointer;

-- 복제 슬롯 정보
SELECT * FROM pg_replication_slots;
```

### 13.2 pg_waldump 유틸리티

WAL 레코드 분석:

```bash
# 특정 WAL 세그먼트 분석
$ pg_waldump /path/to/pg_wal/000000010000000000000001

# 특정 LSN 범위 분석
$ pg_waldump -s 0/1A2B3C00 -e 0/1A2B3CFF /path/to/pg_wal/*

# 특정 리소스 관리자의 레코드만 표시
$ pg_waldump -r Heap /path/to/pg_wal/*
```

출력 예제:

```
rmgr: Heap        len (rec/tot):     59/    59, tx:        488, lsn: 0/01A2B3C0,
      prev 0/01A2B3A0, desc: INSERT off 2 flags 0x00,
      blkref #0: rel 1663/16384/16385 blk 0

rmgr: Transaction len (rec/tot):     34/    34, tx:        488, lsn: 0/01A2B400,
      prev 0/01A2B3C0, desc: COMMIT 2024-01-15 10:30:00.000000 KST
```

### 13.3 WAL 일관성 검사

```ini
# WAL 일관성 검사 활성화
wal_consistency_checking = all  # 또는 특정 리소스 관리자 지정
```

---

## 14. 참고 자료

### 14.1 PostgreSQL 소스 코드

- `src/include/access/xlogrecord.h` - WAL 레코드 형식 정의
- `src/include/access/xlog_internal.h` - WAL 내부 구조체
- `src/backend/access/transam/xlog.c` - WAL 핵심 구현
- `src/backend/access/transam/generic_xlog.c` - Generic WAL 구현
- `src/test/modules/test_custom_rmgrs/` - 사용자 정의 리소스 관리자 예제

### 14.2 공식 문서

- [PostgreSQL WAL Internals](https://www.postgresql.org/docs/current/wal-internals.html)
- [Custom WAL Resource Managers](https://www.postgresql.org/docs/current/custom-rmgr.html)
- [Generic WAL Records](https://www.postgresql.org/docs/current/generic-wal.html)
- [WAL Configuration](https://www.postgresql.org/docs/current/wal-configuration.html)
- [WAL Reliability](https://www.postgresql.org/docs/current/wal-reliability.html)

### 14.3 추가 참고 자료

- [The Internals of PostgreSQL - Chapter 9: Write Ahead Logging](http://www.interdb.jp/pg/pgsql09.html)
- [Custom WAL Resource Managers Wiki](https://wiki.postgresql.org/wiki/CustomWALResourceManagers)

---

## 요약

PostgreSQL의 WAL 시스템은 데이터 무결성과 복구를 보장하는 핵심 메커니즘이다:

1. WAL 원칙: 데이터 변경 전에 WAL 레코드를 먼저 기록
2. LSN: 모든 WAL 레코드의 고유 위치 식별자
3. 리소스 관리자: 특정 유형의 WAL 레코드 쓰기 및 재생 담당
4. 체크포인트: 복구 시작점을 정의하고 더티 페이지를 디스크에 플러시
5. 복구: REDO 포인트부터 WAL 레코드를 순차적으로 재생하여 일관된 상태 복원
6. 전체 페이지 쓰기: 부분 페이지 쓰기 문제 해결

이러한 구성요소들이 함께 작동하여 PostgreSQL의 신뢰성 있는 트랜잭션 처리와 충돌 복구를 가능하게 한다.
