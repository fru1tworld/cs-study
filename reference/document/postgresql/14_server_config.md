# PostgreSQL 서버 설정 (Server Configuration)

이 문서는 PostgreSQL 18 공식 문서의 "Chapter 19. Server Configuration"을 한국어로 번역한 것입니다.

## 개요

PostgreSQL 서버의 동작을 제어하는 다양한 설정 파라미터가 있습니다. 이 장에서는 이러한 파라미터를 설정하는 방법과 각 파라미터의 의미를 설명합니다.

---

## 목차

1. [파라미터 설정 방법](#1-파라미터-설정-방법)
2. [파일 위치](#2-파일-위치)
3. [연결 및 인증](#3-연결-및-인증)
4. [리소스 소비](#4-리소스-소비)
5. [Write Ahead Log (WAL)](#5-write-ahead-log-wal)
6. [복제](#6-복제)
7. [쿼리 계획](#7-쿼리-계획)
8. [오류 보고 및 로깅](#8-오류-보고-및-로깅)
9. [런타임 통계](#9-런타임-통계)
10. [Vacuum](#10-vacuum)
11. [클라이언트 연결 기본값](#11-클라이언트-연결-기본값)
12. [잠금 관리](#12-잠금-관리)

---

# 1. 파라미터 설정 방법

## 1.1 파라미터 이름과 값

모든 파라미터 이름은 대소문자를 구분하지 않습니다. 파라미터는 다음 5가지 타입 중 하나를 사용합니다:

| 타입 | 설명 | 예시 |
|------|------|------|
| Boolean | 참/거짓 값 | `on`, `off`, `true`, `false`, `yes`, `no`, `1`, `0` |
| String | 문자열 값 | `'value'` (작은따옴표 사용) |
| Numeric (integer/float) | 숫자 값 | `100`, `3.14`, `0x1F` (16진수) |
| Numeric with Unit | 단위가 있는 숫자 | `'128MB'`, `'5min'`, `'500ms'` |
| Enumerated | 열거형 값 | `warning`, `error`, `log` |

### 메모리 단위

| 단위 | 의미 | 승수 |
|------|------|------|
| `B` | 바이트 | 1 |
| `kB` | 킬로바이트 | 1024 |
| `MB` | 메가바이트 | 1024^2 |
| `GB` | 기가바이트 | 1024^3 |
| `TB` | 테라바이트 | 1024^4 |

### 시간 단위

| 단위 | 의미 |
|------|------|
| `us` | 마이크로초 |
| `ms` | 밀리초 |
| `s` | 초 |
| `min` | 분 |
| `h` | 시간 |
| `d` | 일 |

## 1.2 설정 파일을 통한 파라미터 상호작용

### postgresql.conf 파일

주요 설정 파일이며 데이터 디렉터리에 위치합니다.

```ini
# 주석은 #으로 시작합니다
log_connections = all
log_destination = 'syslog'
search_path = '"$user", public'
shared_buffers = 128MB
```

형식 규칙:
- 한 줄에 하나의 파라미터
- 등호(`=`)는 선택 사항
- 공백은 무시됨 (따옴표 내부 제외)
- 빈 줄과 `#` 주석은 무시됨
- 단순하지 않은 값은 작은따옴표로 감싸야 함
- 중복 항목이 있으면 마지막 값이 적용됨

### 설정 다시 로드하기

```bash
# 쉘에서
pg_ctl reload

# SQL에서
SELECT pg_reload_conf();
```

### postgresql.auto.conf 파일

`ALTER SYSTEM` 명령으로 자동 편집되는 파일입니다:

```sql
ALTER SYSTEM SET parameter = value;
```

- `postgresql.conf`와 함께 읽힘
- `postgresql.conf` 설정을 덮어씀

### Include 지시문

```ini
include 'filename'
include_if_exists 'filename'
include_dir 'directory'
```

다중 파일 설정 예시:
```ini
# postgresql.conf 끝에:
include 'shared.conf'
include 'memory.conf'
include 'server.conf'

# 또는 디렉터리 사용:
include_dir 'conf.d'
```

## 1.3 SQL을 통한 파라미터 상호작용

### 현재 값 확인

```sql
-- SHOW 명령 사용
SHOW parameter_name;

-- 함수 사용
SELECT current_setting('parameter_name');
```

### 세션 수준 설정

```sql
-- SET 명령
SET parameter_name = value;

-- 함수 사용
SELECT set_config('parameter_name', 'value', false);
```

### 데이터베이스/역할별 설정

```sql
-- 특정 데이터베이스에 대한 설정
ALTER DATABASE dbname SET parameter = value;

-- 특정 역할에 대한 설정
ALTER ROLE rolename SET parameter = value;
```

## 1.4 쉘을 통한 파라미터 상호작용

### 서버 시작 시

```bash
postgres -c log_connections=all --log-destination='syslog'
```

### PGOPTIONS 환경 변수

```bash
env PGOPTIONS="-c geqo=off --statement-timeout=5min" psql
```

## 1.5 파라미터 컨텍스트

파라미터는 설정할 수 있는 시점에 따라 분류됩니다:

| 컨텍스트 | 설명 |
|----------|------|
| `internal` | 읽기 전용, 변경 불가 |
| `postmaster` | 서버 시작 시에만 설정 가능 |
| `sighup` | postgresql.conf를 통해 설정, SIGHUP으로 다시 로드 |
| `superuser` | 슈퍼유저가 세션 수준에서 설정 가능 |
| `user` | 모든 사용자가 세션 수준에서 설정 가능 |

## 1.6 설정 우선순위 (높은 순서에서 낮은 순서)

1. 서버 명령줄 (`-c`, `--name=value`)
2. `PGOPTIONS` 환경 변수
3. `ALTER DATABASE` / `ALTER ROLE` 설정
4. `ALTER SYSTEM` / `postgresql.auto.conf`
5. `postgresql.conf`
6. 빌트인 기본값

---

# 2. 파일 위치

## 파일 위치 파라미터

| 파라미터 | 타입 | 설명 | 설정 시점 |
|----------|------|------|----------|
| `data_directory` | string | 데이터 저장 디렉터리 | 서버 시작 시에만 |
| `config_file` | string | 주 서버 설정 파일 (postgresql.conf) | 명령줄에서만 |
| `hba_file` | string | 호스트 기반 인증 설정 (pg_hba.conf) | 서버 시작 시에만 |
| `ident_file` | string | 사용자 이름 매핑 설정 (pg_ident.conf) | 서버 시작 시에만 |
| `external_pid_file` | string | 관리 프로그램용 추가 PID 파일 | 서버 시작 시에만 |

## 기본 동작

기본적으로 이러한 파라미터는 명시적으로 설정되지 않습니다. 대신:
- 데이터 디렉터리는 `-D` 명령줄 옵션 또는 `PGDATA` 환경 변수로 지정
- 모든 설정 파일은 데이터 디렉터리 내에 위치

## 사용 예시

### 설정 파일을 데이터 디렉터리와 분리

```bash
# /etc/postgresql에 설정 파일이 있는 상태로 PostgreSQL 시작
postgres -D /etc/postgresql
```

`postgresql.conf`에서:
```ini
data_directory = '/var/lib/postgresql/data'
```

### 개별 설정 파일 경로 지정

```ini
data_directory = '/var/lib/postgresql/data'
config_file = '/etc/postgresql/postgresql.conf'
hba_file = '/etc/postgresql/pg_hba.conf'
ident_file = '/etc/postgresql/pg_ident.conf'
external_pid_file = '/var/run/postgresql/postgresql.pid'
```

---

# 3. 연결 및 인증

## 3.1 연결 설정 (Connection Settings)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `listen_addresses` | string | `localhost` | 서버가 수신하는 TCP/IP 주소. `*`는 모든 인터페이스, `0.0.0.0`은 IPv4, `::`는 IPv6 |
| `port` | integer | `5432` | 서버 연결용 TCP 포트 |
| `max_connections` | integer | `100` | 최대 동시 연결 수. 대기 서버에서는 기본 서버와 같거나 높아야 함 |
| `reserved_connections` | integer | `0` | `pg_use_reserved_connections` 권한이 있는 역할을 위해 예약된 연결 슬롯 |
| `superuser_reserved_connections` | integer | `3` | 슈퍼유저를 위해 예약된 연결 슬롯 |
| `unix_socket_directories` | string | `/tmp` | Unix 도메인 소켓용 디렉터리. 쉼표로 구분하여 여러 개 지정 가능 |
| `unix_socket_group` | string | (빈 문자열) | Unix 도메인 소켓의 소유 그룹 |
| `unix_socket_permissions` | integer | `0777` | 소켓의 Unix 파일 권한 (8진수 형식) |
| `bonjour` | boolean | `off` | Bonjour 서비스 광고 활성화 |
| `bonjour_name` | string | (빈 문자열) | Bonjour 서비스 이름. 비어있으면 컴퓨터 이름 사용 |

### 예시: 연결 설정

```ini
# 모든 IP 주소에서 수신
listen_addresses = '*'

# 기본 포트 사용
port = 5432

# 최대 200개의 동시 연결 허용
max_connections = 200

# 슈퍼유저를 위해 5개 연결 예약
superuser_reserved_connections = 5
```

## 3.2 TCP 설정

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `tcp_keepalives_idle` | integer | `0` | TCP keepalive 전송 전 대기 시간(초). 0은 OS 기본값 사용 |
| `tcp_keepalives_interval` | integer | `0` | 응답 없는 TCP keepalive 재전송 간격(초) |
| `tcp_keepalives_count` | integer | `0` | 연결 종료 전 손실 허용 TCP keepalive 메시지 수 |
| `tcp_user_timeout` | integer | `0` | 응답 없는 데이터가 TCP 연결 종료 전까지 유지되는 시간(ms) |
| `client_connection_check_interval` | integer | `0` | 긴 쿼리 중 클라이언트 연결 확인 폴링 간격(ms) |

## 3.3 인증 (Authentication)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `authentication_timeout` | integer | `1m` | 클라이언트 인증 완료까지 최대 시간 |
| `password_encryption` | enum | `scram-sha-256` | 비밀번호 암호화 알고리즘: `scram-sha-256` 또는 `md5` (비권장) |
| `scram_iterations` | integer | `4096` | SCRAM-SHA-256 암호화를 위한 계산 반복 횟수 |
| `md5_password_warnings` | boolean | `on` | MD5 비밀번호 사용 중단 경고 표시 여부 |
| `krb_server_keyfile` | string | `FILE:/usr/local/pgsql/etc/krb5.keytab` | Kerberos 키 파일 위치 |
| `krb_caseins_users` | boolean | `off` | GSSAPI 사용자 이름 대소문자 구분 여부 |
| `gss_accept_delegation` | boolean | `off` | 클라이언트로부터 위임된 GSSAPI 자격 증명 수락 여부 |

### 예시: 인증 설정

```ini
# SCRAM-SHA-256 비밀번호 암호화 사용
password_encryption = scram-sha-256

# 인증 타임아웃 30초
authentication_timeout = 30s

# SCRAM 반복 횟수 증가 (보안 강화)
scram_iterations = 8192
```

## 3.4 SSL/TLS

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `ssl` | boolean | `off` | SSL/TLS 연결 활성화 |
| `ssl_ca_file` | string | (빈 문자열) | SSL 서버 인증 기관(CA) 파일 |
| `ssl_cert_file` | string | `server.crt` | SSL 서버 인증서 파일 |
| `ssl_key_file` | string | `server.key` | SSL 서버 개인 키 파일 |
| `ssl_crl_file` | string | (빈 문자열) | SSL 클라이언트 인증서 해지 목록(CRL) 파일 |
| `ssl_crl_dir` | string | (빈 문자열) | SSL CRL 파일이 포함된 디렉터리 |
| `ssl_ciphers` | string | `HIGH:MEDIUM:+3DES:!aNULL` | TLS 1.2 이하용 암호 스위트 |
| `ssl_tls13_ciphers` | string | (OpenSSL 기본값) | TLS 1.3 연결용 암호 스위트 |
| `ssl_prefer_server_ciphers` | boolean | `on` | 클라이언트 대신 서버의 SSL 암호 기본 설정 사용 |
| `ssl_groups` | string | `X25519:prime256v1` | 키 교환용 ECDH 곡선 |
| `ssl_min_protocol_version` | enum | `TLSv1.2` | 최소 TLS 버전 |
| `ssl_max_protocol_version` | enum | (빈 문자열, 모든 버전 허용) | 최대 TLS 버전 |
| `ssl_dh_params_file` | string | (빈 문자열) | 임시 DH 암호용 Diffie-Hellman 파라미터 파일 |
| `ssl_passphrase_command` | string | (빈 문자열) | SSL 암호문 획득을 위한 외부 명령 |
| `ssl_passphrase_command_supports_reload` | boolean | `off` | 설정 다시 로드 시 암호문 명령 호출 여부 |

### 예시: SSL 설정

```ini
# SSL 활성화
ssl = on

# 인증서 파일 경로
ssl_cert_file = '/etc/ssl/certs/server.crt'
ssl_key_file = '/etc/ssl/private/server.key'
ssl_ca_file = '/etc/ssl/certs/ca.crt'

# 최소 TLS 1.2 요구
ssl_min_protocol_version = 'TLSv1.2'

# 강력한 암호 스위트만 사용
ssl_ciphers = 'HIGH:!aNULL:!MD5'
```

### DH 파라미터 파일 생성

```bash
openssl dhparam -out dhparams.pem 2048
```

---

# 4. 리소스 소비

## 4.1 메모리 (Memory)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `shared_buffers` | integer | `128MB` | 공유 메모리 버퍼용 메모리 양. RAM이 1GB 이상인 시스템에서는 RAM의 25% 권장, 최대 40% |
| `huge_pages` | enum | `try` | 주 공유 메모리용 huge pages 제어 (`try`, `on`, `off`) |
| `huge_page_size` | integer | `0` | 활성화 시 huge page 크기 |
| `temp_buffers` | integer | `8MB` | 임시 테이블용 세션당 최대 메모리 |
| `max_prepared_transactions` | integer | `0` | 최대 동시 준비된 트랜잭션 수 |
| `work_mem` | integer | `4MB` | 쿼리 작업(정렬, 해시 테이블)용 기본 메모리. 디스크에 쓰기 전 사용 |
| `hash_mem_multiplier` | float | `2.0` | 해시 작업 메모리 제한 승수: `work_mem * hash_mem_multiplier` |
| `maintenance_work_mem` | integer | `64MB` | 유지보수 작업(VACUUM, CREATE INDEX, ALTER TABLE)용 메모리 |
| `autovacuum_work_mem` | integer | `-1` | autovacuum 워커당 메모리. 기본값은 `maintenance_work_mem` 사용 |
| `vacuum_buffer_usage_limit` | integer | `2MB` | VACUUM/ANALYZE용 버퍼 접근 전략 크기. 범위: 128KB-16GB |
| `logical_decoding_work_mem` | integer | `64MB` | 디스크에 쓰기 전 논리적 디코딩용 메모리 |
| `max_stack_depth` | integer | `2MB` | 최대 안전 실행 스택 깊이 |
| `shared_memory_type` | enum | `mmap` | 공유 메모리 구현 방식 (`mmap`, `sysv`, `windows`) |
| `dynamic_shared_memory_type` | enum | `posix` | 동적 공유 메모리 구현 방식 |
| `min_dynamic_shared_memory` | integer | `0` | 병렬 쿼리용으로 시작 시 할당되는 메모리 |

### 예시: 메모리 설정

```ini
# 8GB RAM 시스템에서의 권장 설정
shared_buffers = 2GB
work_mem = 64MB
maintenance_work_mem = 512MB
effective_cache_size = 6GB

# Huge pages 사용 시도
huge_pages = try
```

## 4.2 디스크 (Disk)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `temp_file_limit` | integer | `-1` | 프로세스당 임시 파일 최대 디스크 공간. `-1`은 무제한 |
| `file_copy_method` | enum | `COPY` | 파일 복사 방법: `COPY` (기본) 또는 `CLONE` (OS 지원 시) |
| `max_notify_queue_pages` | integer | `1048576` | NOTIFY/LISTEN 큐용 최대 할당 페이지 |

## 4.3 커널 리소스 사용량

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `max_files_per_process` | integer | `1000` | 서버 서브프로세스당 최대 동시 열린 파일 수 |

## 4.4 백그라운드 라이터 (Background Writer)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `bgwriter_delay` | integer | `200ms` | 백그라운드 라이터 활동 라운드 간 지연 시간 |
| `bgwriter_lru_maxpages` | integer | `100` | 라운드당 기록되는 최대 버퍼 수. `0`은 백그라운드 쓰기 비활성화 |
| `bgwriter_lru_multiplier` | float | `2.0` | 더티 버퍼 추정 승수 |
| `bgwriter_flush_after` | integer | `512kB` (Linux) | 이 양 이후 OS writeback 강제 |

## 4.5 I/O

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `backend_flush_after` | integer | `0` | 백엔드가 이 양을 쓴 후 OS writeback 강제 |
| `effective_io_concurrency` | integer | `16` | 예상되는 동시 I/O 작업 수. 범위: 0-1000 |
| `maintenance_io_concurrency` | integer | `16` | 유지보수 작업용 동시 I/O |
| `io_max_combine_limit` | integer | `128kB` | 결합 작업의 최대 I/O 크기 |
| `io_combine_limit` | integer | `128kB` | 결합 작업의 최대 I/O 크기 (`io_max_combine_limit`로 제한됨) |
| `io_max_concurrency` | integer | `-1` | 프로세스당 최대 동시 I/O. `-1`은 설정 기반 자동 선택 |
| `io_method` | enum | `worker` | 비동기 I/O 방식: `worker`, `io_uring`, `sync` |
| `io_workers` | integer | `3` | I/O 워커 프로세스 수 (`io_method=worker`일 때만) |

### 예시: I/O 설정 (SSD용)

```ini
# SSD 스토리지에 최적화된 설정
effective_io_concurrency = 200
maintenance_io_concurrency = 200
random_page_cost = 1.1
```

## 4.6 워커 프로세스 (Worker Processes)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `max_worker_processes` | integer | `8` | 최대 백그라운드 프로세스 수 |
| `max_parallel_workers_per_gather` | integer | `2` | Gather/Gather Merge 노드당 최대 워커 수. `0`은 병렬 쿼리 비활성화 |
| `max_parallel_maintenance_workers` | integer | `2` | 유틸리티 명령(CREATE INDEX, VACUUM)용 최대 병렬 워커 수 |
| `max_parallel_workers` | integer | `8` | 모든 병렬 작업용 최대 워커 수. `max_worker_processes` 이하여야 함 |
| `parallel_leader_participation` | boolean | `on` | 리더 프로세스가 Gather 노드 아래에서 실행 허용 |

### 예시: 병렬 처리 설정

```ini
# 병렬 쿼리 설정
max_worker_processes = 16
max_parallel_workers = 8
max_parallel_workers_per_gather = 4
max_parallel_maintenance_workers = 4
```

---

# 5. Write Ahead Log (WAL)

## 5.1 설정 (Settings)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `wal_level` | enum | `replica` | WAL에 기록되는 정보량. 값: `minimal`, `replica`, `logical` |
| `fsync` | boolean | `on` | `fsync()` 호출을 통해 업데이트가 물리적으로 디스크에 기록되도록 보장 |
| `synchronous_commit` | enum | `on` | 성공 반환 전 WAL 처리 완료 시점 제어. 값: `remote_apply`, `on`, `remote_write`, `local`, `off` |
| `wal_sync_method` | enum | (플랫폼 종속) | WAL 업데이트를 디스크에 강제하는 방법 |
| `full_page_writes` | boolean | `on` | 체크포인트 후 첫 수정 시 전체 디스크 페이지 내용을 WAL에 기록 |
| `wal_log_hints` | boolean | `off` | 비중요 힌트 비트 수정에 대해 전체 페이지 내용 로깅 |
| `wal_compression` | enum | `off` | WAL 압축 활성화. 값: `pglz`, `lz4`, `zstd` |
| `wal_init_zero` | boolean | `on` | 새 WAL 파일을 0으로 채움 |
| `wal_recycle` | boolean | `on` | 새 파일 생성 대신 WAL 파일 이름 변경으로 재활용 |
| `wal_buffers` | integer | `-1` (자동) | 기록되지 않은 WAL 데이터용 공유 메모리 |
| `wal_writer_delay` | integer | `200ms` | WAL 라이터가 WAL을 플러시하는 빈도 |
| `wal_writer_flush_after` | integer | `1MB` | WAL 라이터 플러시를 위한 볼륨 임계값 |
| `wal_skip_threshold` | integer | `2MB` | `wal_level=minimal`일 때 WAL에 쓸지 파일 fsync할지 결정 |
| `commit_delay` | integer | `0` | 그룹 커밋 처리량 향상을 위한 WAL 플러시 전 시간 지연(마이크로초) |
| `commit_siblings` | integer | `5` | `commit_delay` 적용 전 최소 동시 트랜잭션 수 |

### 예시: WAL 설정

```ini
# WAL 레벨 설정 (복제용)
wal_level = replica

# 동기 커밋 활성화
synchronous_commit = on

# WAL 압축 (CPU 사용량 증가, WAL 크기 감소)
wal_compression = lz4

# WAL 버퍼 크기
wal_buffers = 64MB
```

## 5.2 체크포인트 (Checkpoints)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `checkpoint_timeout` | integer | `5min` | 자동 WAL 체크포인트 간 최대 시간 (30초 - 1일) |
| `checkpoint_completion_target` | float | `0.9` | 체크포인트를 분산시킬 체크포인트 간 시간의 비율 |
| `checkpoint_flush_after` | integer | `256kB` (Linux) | 체크포인트 중 강제 writeback 임계값 |
| `checkpoint_warning` | integer | `30s` | 체크포인트가 이 간격보다 더 자주 발생하면 경고 로깅 |
| `max_wal_size` | integer | `1GB` | 자동 체크포인트 중 최대 WAL 크기 (소프트 제한) |
| `min_wal_size` | integer | `80MB` | 오래된 WAL 파일 재활용 전 최소 WAL 크기 |

### 예시: 체크포인트 설정

```ini
# 체크포인트 설정
checkpoint_timeout = 15min
checkpoint_completion_target = 0.9
max_wal_size = 4GB
min_wal_size = 1GB
```

## 5.3 아카이빙 (Archiving)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `archive_mode` | enum | `off` | WAL 아카이빙 활성화. 값: `off`, `on`, `always` |
| `archive_command` | string | `''` | 완료된 WAL 세그먼트를 아카이브하는 쉘 명령. 플레이스홀더: `%p` (경로), `%f` (파일명) |
| `archive_library` | string | `''` | WAL 세그먼트 아카이빙용 라이브러리 (`archive_command`의 대안) |
| `archive_timeout` | integer | `0` | 지정된 시간 동안 활동이 없으면 WAL 세그먼트 전환 강제 (초) |

### 아카이브 명령 예시

```ini
# Linux/Unix
archive_command = 'cp %p /mnt/server/archivedir/%f'

# Windows
archive_command = 'copy "%p" "C:\\server\\archivedir\\%f"'

# 원격 서버로 복사
archive_command = 'scp %p backup@archiveserver:/archive/%f'
```

## 5.4 복구 (Recovery)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `recovery_prefetch` | enum | `try` | 복구 중 블록 프리페치. 값: `off`, `on`, `try` |
| `wal_decode_buffer_size` | integer | `512kB` | 프리페칭을 위한 WAL lookahead 제한 |

## 5.5 아카이브 복구 (Archive Recovery)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `restore_command` | string | `''` | 아카이브된 WAL 세그먼트를 검색하는 쉘 명령. 플레이스홀더: `%f` (파일명), `%p` (경로), `%r` (재시작 지점 파일) |
| `archive_cleanup_command` | string | `''` | 정리를 위해 모든 restartpoint에서 실행되는 쉘 명령 |
| `recovery_end_command` | string | `''` | 복구 종료 시 한 번 실행되는 쉘 명령 |

### 복원 명령 예시

```ini
restore_command = 'cp /mnt/server/archivedir/%f "%p"'
archive_cleanup_command = 'pg_archivecleanup /mnt/server/archivedir %r'
```

## 5.6 복구 대상 (Recovery Target)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `recovery_target` | string | - | `'immediate'`로 설정하면 가장 빠른 일관된 상태로 복구 |
| `recovery_target_name` | string | - | 복구할 명명된 복원 지점 |
| `recovery_target_time` | timestamp | - | 복구가 진행될 타임스탬프 |
| `recovery_target_xid` | string | - | 복구가 진행될 트랜잭션 ID |
| `recovery_target_lsn` | pg_lsn | - | 복구가 진행될 WAL LSN |
| `recovery_target_inclusive` | boolean | `on` | 복구 대상 포함(`on`) 또는 제외(`off`) |
| `recovery_target_timeline` | string | `latest` | 복구할 타임라인. 값: `current`, `latest`, 또는 16진수 타임라인 ID |
| `recovery_target_action` | enum | `pause` | 복구 대상 도달 후 작업. 값: `pause`, `promote`, `shutdown` |

참고: `recovery_target`, `recovery_target_lsn`, `recovery_target_name`, `recovery_target_time`, `recovery_target_xid` 중 하나만 지정할 수 있습니다.

## 5.7 WAL 요약 (WAL Summarization)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `summarize_wal` | boolean | `off` | WAL 요약기 프로세스 활성화 (증분 백업에 필요) |
| `wal_summary_keep_time` | integer | `10d` | 오래된 WAL 요약이 자동으로 제거되는 시간 |

---

# 6. 복제

## 6.1 송신 서버 (Sending Servers)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `max_wal_senders` | integer | `10` | 대기 서버 또는 스트리밍 베이스 백업 클라이언트의 최대 동시 연결 수 |
| `max_replication_slots` | integer | `10` | 서버가 지원할 수 있는 최대 복제 슬롯 수 |
| `wal_keep_size` | integer | `0` | 대기 스트리밍 복제용으로 `pg_wal` 디렉터리에 유지되는 과거 WAL 파일의 최소 크기(MB) |
| `max_slot_wal_keep_size` | integer | `-1` (무제한) | 복제 슬롯이 `pg_wal` 디렉터리에 유지할 수 있는 WAL 파일의 최대 크기(MB) |
| `idle_replication_slot_timeout` | integer | `0` (비활성화) | 이 기간보다 오래 비활성인 복제 슬롯 무효화 |
| `wal_sender_timeout` | integer | `60s` | 비활성 복제 연결 종료 |
| `track_commit_timestamp` | boolean | `off` | 트랜잭션의 커밋 시간 기록 |
| `synchronized_standby_slots` | string | (빈 문자열) | 논리적 WAL 송신기 프로세스가 대기할 스트리밍 복제 대기 슬롯 이름 목록 |

## 6.2 기본 서버 (Primary Server)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `synchronous_standby_names` | string | (빈 문자열) | 동기 복제를 지원할 수 있는 대기 서버 목록 |

### 동기 복제 구문

```ini
# 첫 번째 3개 대기 서버의 동기 복제
synchronous_standby_names = 'FIRST 3 (s1, s2, s3, s4)'

# 4개 중 아무 3개 대기 서버
synchronous_standby_names = 'ANY 3 (s1, s2, s3, s4)'

# 레거시 구문 (FIRST 1과 동일)
synchronous_standby_names = 's1, s2, s3'
```

## 6.3 대기 서버 (Standby Servers)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `primary_conninfo` | string | (빈 문자열) | 대기 서버가 송신 서버에 연결하기 위한 연결 문자열 |
| `primary_slot_name` | string | (빈 문자열) | 송신 서버에 연결할 때 사용할 기존 복제 슬롯 |
| `hot_standby` | boolean | `on` | 복구 중 연결 및 쿼리 허용 |
| `max_standby_archive_delay` | integer | `30s` | WAL 아카이브 항목과 충돌하는 대기 쿼리를 취소하기 전 최대 대기 시간 |
| `max_standby_streaming_delay` | integer | `30s` | 들어오는 WAL 항목과 충돌하는 대기 쿼리를 취소하기 전 최대 대기 시간 |
| `wal_receiver_create_temp_slot` | boolean | `off` | 영구 슬롯이 설정되지 않은 경우 원격 인스턴스에 임시 복제 슬롯 생성 |
| `wal_receiver_status_interval` | integer | `10s` | WAL 수신기가 기본 서버에 복제 진행 상황을 전송하는 최소 빈도 |
| `hot_standby_feedback` | boolean | `off` | 대기 서버에서 실행 중인 쿼리에 대한 피드백을 기본 서버로 전송 |
| `wal_receiver_timeout` | integer | `60s` | 비활성 복제 연결 종료 |
| `wal_retrieve_retry_interval` | integer | `5s` | WAL 데이터를 사용할 수 없을 때 검색 재시도 전 대기 시간 |
| `recovery_min_apply_delay` | integer | `0` (지연 없음) | 특정 시점 복구를 위해 복구를 지연 |
| `sync_replication_slots` | boolean | `off` | 기본 서버에서 대기 서버로 논리적 장애 조치 슬롯 동기화 |

### 예시: 기본 연결 정보

```ini
primary_conninfo = 'host=primary.example.com port=5432 user=replication password=secret'
```

### 예시: 지연 복제

```ini
recovery_min_apply_delay = '5min'
```

## 6.4 구독자 (Subscribers)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `max_active_replication_origins` | integer | `10` | 동시에 추적할 수 있는 최대 복제 출처 수 |
| `max_logical_replication_workers` | integer | `4` | 최대 논리적 복제 워커 수 |
| `max_sync_workers_per_subscription` | integer | `2` | 구독당 최대 동기화 워커 수 |
| `max_parallel_apply_workers_per_subscription` | integer | `2` | 진행 중인 트랜잭션 스트리밍용 구독당 최대 병렬 적용 워커 수 |

---

# 7. 쿼리 계획

## 7.1 플래너 메서드 설정

이러한 파라미터는 쿼리 옵티마이저가 사용할 수 있는 쿼리 계획 유형을 제어합니다. 달리 명시되지 않는 한 모두 기본값 `on`입니다.

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `enable_async_append` | boolean | `on` | 비동기 인식 append 계획 유형 활성화 |
| `enable_bitmapscan` | boolean | `on` | 비트맵 스캔 계획 유형 활성화 |
| `enable_gathermerge` | boolean | `on` | gather merge 계획 유형 활성화 |
| `enable_hashagg` | boolean | `on` | 해시 집계 계획 유형 활성화 |
| `enable_hashjoin` | boolean | `on` | 해시 조인 계획 유형 활성화 |
| `enable_incremental_sort` | boolean | `on` | 증분 정렬 단계 활성화 |
| `enable_indexscan` | boolean | `on` | 인덱스 스캔 및 인덱스 전용 스캔 계획 유형 활성화 |
| `enable_indexonlyscan` | boolean | `on` | 인덱스 전용 스캔 계획 유형 활성화 |
| `enable_material` | boolean | `on` | 구체화 활성화 (완전히 억제할 수 없음) |
| `enable_memoize` | boolean | `on` | 중첩 루프 조인에서 파라미터화된 스캔 결과 캐시 |
| `enable_mergejoin` | boolean | `on` | 병합 조인 계획 유형 활성화 |
| `enable_nestloop` | boolean | `on` | 중첩 루프 조인 계획 활성화 (완전히 억제할 수 없음) |
| `enable_parallel_append` | boolean | `on` | 병렬 인식 append 계획 유형 활성화 |
| `enable_parallel_hash` | boolean | `on` | 병렬 해시를 사용한 해시 조인 활성화 |
| `enable_partition_pruning` | boolean | `on` | 쿼리 계획에서 파티션 제거 |
| `enable_partitionwise_join` | boolean | `off` | 일치하는 파티션을 조인하여 파티션된 테이블 조인 |
| `enable_partitionwise_aggregate` | boolean | `off` | 파티션된 테이블에서 파티션별로 별도로 그룹화/집계 |
| `enable_seqscan` | boolean | `on` | 순차 스캔 계획 유형 활성화 (완전히 억제할 수 없음) |
| `enable_sort` | boolean | `on` | 명시적 정렬 단계 활성화 (완전히 억제할 수 없음) |
| `enable_tidscan` | boolean | `on` | TID 스캔 계획 유형 활성화 |

### 예시: 특정 계획 유형 비활성화 (문제 해결용)

```sql
-- 해시 조인 비활성화
SET enable_hashjoin = off;

-- 순차 스캔 비활성화 (인덱스 스캔 강제)
SET enable_seqscan = off;
```

## 7.2 플래너 비용 상수

비용 변수는 상대적 값만 중요한 임의의 척도를 사용합니다. 기본 척도: `seq_page_cost = 1.0`

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `seq_page_cost` | float | `1.0` | 순차 디스크 페이지 가져오기 비용 |
| `random_page_cost` | float | `4.0` | 비순차 디스크 페이지 가져오기 비용 |
| `cpu_tuple_cost` | float | `0.01` | 각 행 처리 비용 |
| `cpu_index_tuple_cost` | float | `0.005` | 각 인덱스 항목 처리 비용 |
| `cpu_operator_cost` | float | `0.0025` | 연산자/함수 처리 비용 |
| `parallel_setup_cost` | float | `1000` | 병렬 워커 시작 비용 |
| `parallel_tuple_cost` | float | `0.1` | 병렬 워커에서 튜플 전송 비용 |
| `min_parallel_table_scan_size` | integer | `8MB` | 병렬 스캔을 위한 최소 테이블 데이터 |
| `min_parallel_index_scan_size` | integer | `512kB` | 병렬 스캔을 위한 최소 인덱스 데이터 |
| `effective_cache_size` | integer | `4GB` | 쿼리에 사용 가능한 유효 디스크 캐시 크기 |
| `jit_above_cost` | float | `100000` | JIT 컴파일을 위한 쿼리 비용 임계값 |
| `jit_inline_above_cost` | float | `500000` | JIT 인라이닝을 위한 쿼리 비용 임계값 |
| `jit_optimize_above_cost` | float | `500000` | 비용이 많이 드는 JIT 최적화를 위한 쿼리 비용 임계값 |

### 예시: SSD 스토리지용 비용 조정

```ini
# SSD는 랜덤 읽기가 빠름
random_page_cost = 1.1
seq_page_cost = 1.0

# 캐시 크기 설정 (시스템 RAM의 75%)
effective_cache_size = 12GB
```

## 7.3 유전 쿼리 옵티마이저 (GEQO)

많은 조인이 있는 복잡한 쿼리에 사용되어 최적이 아닌 계획의 잠재적 비용으로 계획 시간을 줄입니다.

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `geqo` | boolean | `on` | 유전 쿼리 최적화 활성화/비활성화 |
| `geqo_threshold` | integer | `12` | GEQO 사용 전 최소 FROM 항목 수 |
| `geqo_effort` | integer | `5` | 계획 시간 대 계획 품질 트레이드오프 (1-10) |
| `geqo_pool_size` | integer | `0` | 유전 집단 크기 (0 = 자동 계산) |
| `geqo_generations` | integer | `0` | 알고리즘 반복 (0 = 자동 계산) |
| `geqo_selection_bias` | float | `2.0` | 선택 압력 (1.50-2.00) |
| `geqo_seed` | float | `0` | 난수 생성기 시드 (0-1) |

## 7.4 기타 플래너 옵션

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `default_statistics_target` | integer | `100` | 테이블 열의 기본 통계 대상 |
| `constraint_exclusion` | enum | `partition` | 최적화에 테이블 제약 조건 사용: `on`, `off`, `partition` |
| `cursor_tuple_fraction` | float | `0.1` | 검색할 커서 행의 비율 |
| `from_collapse_limit` | integer | `8` | 서브쿼리 병합 전 최대 FROM 항목 수 |
| `jit` | boolean | `on` | JIT 컴파일 활성화/비활성화 |
| `join_collapse_limit` | integer | `8` | 명시적 JOIN 재작성 전 최대 항목 수 |
| `plan_cache_mode` | enum | `auto` | 계획 캐싱 모드: `auto`, `force_custom_plan`, `force_generic_plan` |
| `recursive_worktable_factor` | float | `10.0` | 재귀 쿼리용 추정 작업 테이블 크기 승수 |

### 예시: 통계 대상 증가

```sql
-- 더 나은 계획 품질을 위해 통계 대상 증가
SET default_statistics_target = 250;

-- 특정 열에 대한 통계 수집
ALTER TABLE my_table ALTER COLUMN my_column SET STATISTICS 500;
ANALYZE my_table;
```

---

# 8. 오류 보고 및 로깅

## 8.1 로그 위치 (Where to Log)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `log_destination` | string | `stderr` | 로그 대상의 쉼표로 구분된 목록: `stderr`, `csvlog`, `jsonlog`, `syslog`, `eventlog` (Windows만) |
| `logging_collector` | boolean | `off` | stderr를 캡처하고 로그 파일로 리디렉션하는 백그라운드 로깅 수집기 프로세스 활성화 |
| `log_directory` | string | `log` | 로그 파일 디렉터리 (절대 또는 클러스터 데이터 디렉터리 기준 상대) |
| `log_filename` | string | `postgresql-%Y-%m-%d_%H%M%S.log` | `strftime` 형식 코드를 사용한 로그 파일 명명 패턴 |
| `log_file_mode` | integer | `0600` | 로그 파일의 Unix 파일 권한 (8진수 형식) |
| `log_rotation_age` | integer | `1d` | 회전 전 개별 로그 파일 사용 최대 시간 |
| `log_rotation_size` | integer | `10MB` | 회전 전 개별 로그 파일 최대 크기 |
| `log_truncate_on_rotation` | boolean | `off` | 추가 대신 시간 기반 회전 시 기존 로그 파일 자르기 |
| `syslog_facility` | enum | `LOCAL0` | syslog 기능: `LOCAL0`부터 `LOCAL7`까지 |
| `syslog_ident` | string | `postgres` | syslog 식별용 프로그램 이름 |
| `syslog_sequence_numbers` | boolean | `on` | syslog 메시지에 증가하는 시퀀스 번호 접두사 |
| `syslog_split_messages` | boolean | `on` | syslog용으로 줄별로 메시지 분할 (1024바이트 제한) |
| `event_source` | string | `PostgreSQL` | Windows 이벤트 로그 식별용 프로그램 이름 |

### 예시: 로깅 설정

```ini
# 로깅 수집기 활성화
logging_collector = on

# 로그 디렉터리
log_directory = 'pg_log'

# 로그 파일 이름 패턴
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'

# 매일 로그 회전
log_rotation_age = 1d

# 100MB 초과 시 로그 회전
log_rotation_size = 100MB
```

## 8.2 로깅 시점 (When to Log)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `log_min_messages` | enum | `WARNING` | 로깅할 최소 메시지 심각도 |
| `log_min_error_statement` | enum | `ERROR` | 오류를 유발하는 SQL 문 로깅을 위한 최소 심각도 |
| `log_min_duration_statement` | integer | `-1` | 최소 이 시간(ms) 이상 실행되는 문 로깅. `0`은 모든 기간 로깅. `-1`은 비활성화 |
| `log_min_duration_sample` | integer | `-1` | 이 임계값을 초과하는 문 기간 샘플링 (ms) |
| `log_statement_sample_rate` | float | `1.0` | `log_min_duration_sample`을 초과하는 문 로깅 비율 (0.0-1.0) |
| `log_transaction_sample_rate` | float | `0` | 완전히 로깅할 트랜잭션 비율 (0.0-1.0) |
| `log_startup_progress_interval` | integer | `10s` | 오래 실행되는 시작 작업 로깅 간격. `0`은 비활성화 |

### 메시지 심각도 수준

| 심각도 | 용도 | Syslog | Event Log |
|--------|------|--------|-----------|
| `DEBUG1..DEBUG5` | 개발자 디버깅 정보 | `DEBUG` | `INFORMATION` |
| `INFO` | 사용자 요청 정보 (예: VACUUM VERBOSE) | `INFO` | `INFORMATION` |
| `NOTICE` | 유용한 사용자 정보 (예: 식별자 잘림) | `NOTICE` | `INFORMATION` |
| `WARNING` | 경고 (예: 트랜잭션 외부의 COMMIT) | `NOTICE` | `WARNING` |
| `ERROR` | 명령 중단됨 | `WARNING` | `ERROR` |
| `LOG` | 관리자 정보 (예: 체크포인트 활동) | `INFO` | `INFORMATION` |
| `FATAL` | 세션 중단됨 | `ERR` | `ERROR` |
| `PANIC` | 모든 세션 중단됨 | `CRIT` | `ERROR` |

## 8.3 로깅 내용 (What to Log)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `application_name` | string | (빈 문자열) | 애플리케이션 식별자 (최대 63자). 클라이언트가 설정 |
| `debug_print_parse` | boolean | `off` | 각 쿼리의 파스 트리를 `LOG` 수준으로 출력 |
| `debug_print_rewritten` | boolean | `off` | 쿼리 재작성기 출력을 `LOG` 수준으로 출력 |
| `debug_print_plan` | boolean | `off` | 실행 계획을 `LOG` 수준으로 출력 |
| `debug_pretty_print` | boolean | `on` | 가독성을 위해 디버그 출력 들여쓰기 |
| `log_autovacuum_min_duration` | integer | `10min` | 이 기간을 초과하는 autovacuum 작업 로깅. `-1`은 비활성화 |
| `log_checkpoints` | boolean | `on` | 통계와 함께 체크포인트 및 restartpoint 작업 로깅 |
| `log_connections` | string | `''` | 연결 측면 로깅: `receipt`, `authentication`, `authorization`, `setup_durations`, `all` |
| `log_disconnections` | boolean | `off` | 기간과 함께 세션 종료 로깅 |
| `log_duration` | boolean | `off` | 완료된 모든 문의 기간 로깅 |
| `log_error_verbosity` | enum | `DEFAULT` | 오류 세부 수준: `TERSE`, `DEFAULT`, `VERBOSE` |
| `log_hostname` | boolean | `off` | 클라이언트 호스트 이름 로깅 (IP 외에). 성능에 영향을 줄 수 있음 |
| `log_line_prefix` | string | `'%m [%p] '` | 각 로그 줄의 `printf` 스타일 접두사 |
| `log_lock_waits` | boolean | `off` | `deadlock_timeout`보다 오래 잠금을 대기하는 세션 로깅 |
| `log_recovery_conflict_waits` | boolean | `off` | 복구 충돌을 대기하는 시작 프로세스 로깅 |
| `log_parameter_max_length` | integer | `-1` (무제한) | 비오류 로그에서 바인드 파라미터를 이 바이트로 자르기. `0`은 비활성화 |
| `log_parameter_max_length_on_error` | integer | `0` (비활성화) | 오류 메시지에서 바인드 파라미터를 이 바이트로 자르기. `-1`은 전체 허용 |
| `log_statement` | enum | `none` | SQL 문 로깅: `none`, `ddl`, `mod`, `all` |
| `log_replication_commands` | boolean | `off` | 복제 명령 및 walsender 슬롯 작업 로깅 |
| `log_temp_files` | integer | `-1` | 임시 파일 로깅. `0`은 모두 로깅; 양수는 크기 임계값(KB) |
| `log_timezone` | string | `GMT` | 서버 로그 타임스탬프용 시간대 (클러스터 전체) |

### log_line_prefix 이스케이프 시퀀스

| 이스케이프 | 효과 |
|-----------|------|
| `%a` | 애플리케이션 이름 |
| `%u` | 사용자 이름 |
| `%d` | 데이터베이스 이름 |
| `%r` | 원격 호스트/IP 및 포트 |
| `%h` | 원격 호스트/IP |
| `%b` | 백엔드 유형 |
| `%p` | 프로세스 ID |
| `%t` | 타임스탬프 (밀리초 없음) |
| `%m` | 타임스탬프 (밀리초 포함) |
| `%i` | 명령 태그 |
| `%e` | SQLSTATE 오류 코드 |
| `%c` | 세션 ID (16진수 형식) |
| `%l` | 세션당 로그 줄 번호 |
| `%s` | 프로세스 시작 타임스탬프 |
| `%v` | 가상 트랜잭션 ID |
| `%x` | 트랜잭션 ID |
| `%Q` | 쿼리 식별자 |
| `%%` | 리터럴 `%` |

### 예시: 로깅 설정

```ini
# 상세한 로그 접두사
log_line_prefix = '%t [%p]: user=%u,db=%d,app=%a,client=%h '

# 느린 쿼리 로깅 (1초 이상)
log_min_duration_statement = 1000

# DDL 및 데이터 수정 문 로깅
log_statement = 'mod'

# 연결/해제 로깅
log_connections = 'all'
log_disconnections = on

# 잠금 대기 로깅
log_lock_waits = on
```

## 8.4 CSV 형식 로그 출력

CSV 로그에는 다음 열이 포함됩니다 (순서대로):
- timestamp, user_name, database_name, process_id, connection_from
- session_id, session_line_num, command_tag, session_start_time
- virtual_transaction_id, transaction_id, error_severity, sql_state_code
- message, detail, hint, internal_query, internal_query_pos
- context, query, query_pos, location, application_name
- backend_type, leader_pid, query_id

### 예시: CSV 로그용 테이블 정의

```sql
CREATE TABLE postgres_log (
  log_time timestamp(3) with time zone,
  user_name text,
  database_name text,
  process_id integer,
  connection_from text,
  session_id text,
  session_line_num bigint,
  command_tag text,
  session_start_time timestamp with time zone,
  virtual_transaction_id text,
  transaction_id bigint,
  error_severity text,
  sql_state_code text,
  message text,
  detail text,
  hint text,
  internal_query text,
  internal_query_pos integer,
  context text,
  query text,
  query_pos integer,
  location text,
  application_name text,
  backend_type text,
  leader_pid integer,
  query_id bigint,
  PRIMARY KEY (session_id, session_line_num)
);
```

### 로그 파일 가져오기

```sql
COPY postgres_log FROM '/full/path/to/logfile.csv' WITH csv;
```

## 8.5 JSON 형식 로그 출력

JSON 로그 형식에는 다음 필드가 포함됩니다 (null 값 제외):
- timestamp, user, dbname, pid, remote_host, remote_port
- session_id, line_num, ps, session_start, vxid, txid
- error_severity, state_code, message, detail, hint
- internal_query, internal_position, context, statement, cursor_position
- func_name, file_name, file_line_num, application_name
- backend_type, leader_pid, query_id

## 8.6 프로세스 타이틀 (Process Title)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `cluster_name` | string | `''` | 프로세스 타이틀에 나타나는 클러스터 식별자. 최대 63자 |
| `update_process_title` | boolean | `on` (Windows: `off`) | 수신된 각 SQL 명령으로 프로세스 타이틀 업데이트 |

---

# 9. 런타임 통계

## 9.1 누적 쿼리 및 인덱스 통계

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `track_activities` | boolean | `ON` | 세션당 현재 실행 중인 명령에 대한 정보 수집 활성화 |
| `track_activity_query_size` | integer | `1024` | `pg_stat_activity.query` 필드에 현재 실행 중인 명령의 텍스트를 저장하기 위해 예약된 메모리 |
| `track_counts` | boolean | `ON` | 데이터베이스 활동 통계 수집 활성화. autovacuum 데몬에 필요 |
| `track_cost_delay_timing` | boolean | `OFF` | 비용 기반 vacuum 지연 타이밍 활성화 |
| `track_io_timing` | boolean | `OFF` | 데이터베이스 I/O 대기 타이밍 활성화. `pg_stat_database`, `pg_stat_io`, `EXPLAIN` 출력에 표시 |
| `track_wal_io_timing` | boolean | `OFF` | WAL I/O 대기 타이밍 활성화. `pg_stat_io`에 표시 |
| `track_functions` | enum | `none` | 함수 호출 횟수 및 시간 추적 활성화. 값: `none`, `pl` (절차적 언어만), `all` |
| `stats_fetch_consistency` | enum | `cache` | 트랜잭션에서 통계에 여러 번 접근할 때의 동작. 값: `none`, `cache`, `snapshot` |

## 9.2 통계 모니터링

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `compute_query_id` | enum | `auto` | 쿼리 식별자의 코어 내 계산 활성화. 값: `off`, `on`, `auto`, `regress` |
| `log_statement_stats` | boolean | `OFF` | 총 문 성능 통계를 서버 로그에 출력 |
| `log_parser_stats` | boolean | `OFF` | 파서 모듈 통계를 서버 로그에 출력 |
| `log_planner_stats` | boolean | `OFF` | 플래너 모듈 통계를 서버 로그에 출력 |
| `log_executor_stats` | boolean | `OFF` | 실행기 모듈 통계를 서버 로그에 출력 |

### 예시: 통계 설정

```ini
# I/O 타이밍 활성화 (성능 오버헤드 있음)
track_io_timing = on

# 함수 추적 활성화
track_functions = all

# 쿼리 식별자 계산 (pg_stat_statements 사용 시 필요)
compute_query_id = on
```

---

# 10. Vacuum

## 10.1 자동 Vacuuming

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `autovacuum` | boolean | `on` | autovacuum 런처 데몬 실행 여부 제어. `track_counts` 활성화 필요 |
| `autovacuum_worker_slots` | integer | `16` | autovacuum 워커용으로 예약된 백엔드 슬롯 수 |
| `autovacuum_max_workers` | integer | `3` | 최대 동시 autovacuum 프로세스 수 (런처 제외) |
| `autovacuum_naptime` | integer | `1min` | 모든 데이터베이스에서 autovacuum 실행 간 최소 지연 |
| `autovacuum_vacuum_threshold` | integer | `50` | VACUUM 트리거를 위한 최소 업데이트/삭제된 튜플 수 |
| `autovacuum_vacuum_insert_threshold` | integer | `1000` | VACUUM 트리거에 필요한 삽입된 튜플 수. `-1`로 비활성화 |
| `autovacuum_analyze_threshold` | integer | `50` | ANALYZE 트리거를 위한 최소 삽입/업데이트/삭제된 튜플 수 |
| `autovacuum_vacuum_scale_factor` | float | `0.2` | `autovacuum_vacuum_threshold`에 추가되는 테이블 크기의 비율 |
| `autovacuum_vacuum_insert_scale_factor` | float | `0.2` | `autovacuum_vacuum_insert_threshold`에 추가되는 동결되지 않은 페이지의 비율 |
| `autovacuum_analyze_scale_factor` | float | `0.1` | `autovacuum_analyze_threshold`에 추가되는 테이블 크기의 비율 |
| `autovacuum_vacuum_max_threshold` | integer | `100000000` | VACUUM 트리거를 위한 최대 업데이트/삭제된 튜플 수. `-1`은 제한 없음 |
| `autovacuum_freeze_max_age` | integer | `200000000` | 래핑 방지를 위한 강제 VACUUM 전 최대 트랜잭션 나이 |
| `autovacuum_multixact_freeze_max_age` | integer | `400000000` | 강제 VACUUM 전 최대 multixact 나이 |
| `autovacuum_vacuum_cost_delay` | float | `2ms` | 자동 VACUUM의 비용 지연. `-1`은 `vacuum_cost_delay` 사용 |
| `autovacuum_vacuum_cost_limit` | integer | `-1` | 자동 VACUUM의 비용 제한. `-1`은 `vacuum_cost_limit` 사용 |

### 예시: Autovacuum 설정

```ini
# Autovacuum 활성화
autovacuum = on

# 최대 5개의 동시 워커
autovacuum_max_workers = 5

# 더 공격적인 vacuum (기본값보다 낮은 임계값)
autovacuum_vacuum_threshold = 25
autovacuum_vacuum_scale_factor = 0.1
autovacuum_analyze_threshold = 25
autovacuum_analyze_scale_factor = 0.05
```

## 10.2 비용 기반 Vacuum 지연 (Cost-Based Vacuum Delay)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `vacuum_cost_delay` | float | `0` | 비용 제한 초과 시 수면 시간 (ms). `0`은 비용 기반 지연 비활성화 |
| `vacuum_cost_page_hit` | integer | `1` | 공유 캐시의 버퍼 vacuum 추정 비용 |
| `vacuum_cost_page_miss` | integer | `2` | 디스크에서 읽은 버퍼 vacuum 추정 비용 |
| `vacuum_cost_page_dirty` | integer | `20` | VACUUM이 이전에 깨끗한 블록을 수정할 때 추정 비용 |
| `vacuum_cost_limit` | integer | `200` | `vacuum_cost_delay` 수면을 트리거하는 누적 비용 임계값 |

## 10.3 기본 동작

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `vacuum_truncate` | boolean | `true` | VACUUM이 테이블 끝의 빈 페이지를 자르도록 활성화. OS에 디스크 공간 반환. `ACCESS EXCLUSIVE` 잠금 필요 |

## 10.4 동결 (Freezing)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `vacuum_freeze_table_age` | integer | `150000000` | 공격적인 VACUUM 스캔을 트리거하는 트랜잭션 나이 |
| `vacuum_freeze_min_age` | integer | `50000000` | 페이지 동결 결정을 위한 컷오프 나이 |
| `vacuum_failsafe_age` | integer | `1600000000` | VACUUM 페일세이프가 트리거되는 최대 트랜잭션 나이 |
| `vacuum_multixact_freeze_table_age` | integer | `150000000` | 공격적인 VACUUM 스캔을 트리거하는 multixact 나이 |
| `vacuum_multixact_freeze_min_age` | integer | `5000000` | 페이지 동결을 위한 컷오프 multixact 나이 |
| `vacuum_multixact_failsafe_age` | integer | `1600000000` | 페일세이프가 트리거되는 최대 multixact 나이 |
| `vacuum_max_eager_freeze_failure_rate` | float | `0.03` | eager 스캔 비활성화 전 VACUUM이 동결에 실패할 수 있는 페이지의 최대 비율 |

동결의 목적: 충분히 오래된 행을 동결로 표시하여 XID 검사 없이 모든 사람에게 보이도록 하여 트랜잭션 ID 래핑 실패를 방지합니다.

---

# 11. 클라이언트 연결 기본값

## 11.1 문 동작 (Statement Behavior)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `client_min_messages` | enum | `NOTICE` | 클라이언트에 전송되는 메시지 수준 제어 |
| `search_path` | string | `"$user", public` | 객체가 단순 이름으로 참조될 때 스키마가 검색되는 순서 |
| `row_security` | boolean | `on` | 오류 발생 또는 행 보안 정책 적용 제어 |
| `default_table_access_method` | string | `heap` | 테이블/구체화 뷰 생성을 위한 기본 테이블 접근 방법 |
| `default_tablespace` | string | (빈 문자열) | 명시적으로 지정되지 않은 경우 객체 생성을 위한 기본 테이블스페이스 |
| `default_toast_compression` | enum | `pglz` | 기본 TOAST 압축 방법. 지원: `pglz`, `lz4` |
| `temp_tablespaces` | string | (빈 문자열) | 임시 객체용 테이블스페이스 |
| `check_function_bodies` | boolean | `on` | `off`일 때 `CREATE FUNCTION/PROCEDURE` 중 루틴 본문 문자열 유효성 검사 비활성화 |
| `default_transaction_isolation` | enum | `read committed` | 기본 격리 수준. 값: `read uncommitted`, `read committed`, `repeatable read`, `serializable` |
| `default_transaction_read_only` | boolean | `off` | 새 트랜잭션의 기본 읽기 전용 상태 |
| `default_transaction_deferrable` | boolean | `off` | 기본 지연 가능 상태. `serializable` 읽기 전용 트랜잭션에만 영향 |
| `statement_timeout` | integer | `0` | 지정된 밀리초를 초과하는 문 중단. `0`은 비활성화 |
| `transaction_timeout` | integer | `0` | 트랜잭션에서 지정된 밀리초보다 오래 걸리는 세션 종료 |
| `lock_timeout` | integer | `0` | 지정된 밀리초보다 오래 잠금을 대기하는 문 중단 |
| `idle_in_transaction_session_timeout` | integer | `0` | 열린 트랜잭션 내에서 지정된 밀리초보다 오래 유휴인 세션 종료 |
| `idle_session_timeout` | integer | `0` | 지정된 밀리초보다 오래 유휴인 세션 (트랜잭션 내 아님) 종료 |
| `bytea_output` | enum | `hex` | `bytea` 값의 출력 형식. 값: `hex`, `escape` |
| `xmlbinary` | enum | `base64` | XML 바이너리 인코딩 방법. 값: `base64`, `hex` |
| `xmloption` | enum | `CONTENT` | 암시적 XML 변환 모드. 값: `DOCUMENT`, `CONTENT` |
| `gin_pending_list_limit` | integer | `4MB` | GIN 인덱스 보류 목록의 최대 크기 |

### 예시: 문 동작 설정

```ini
# 검색 경로 설정
search_path = '"$user", public, shared_schema'

# 문 타임아웃 30초
statement_timeout = 30000

# 유휴 트랜잭션 타임아웃 5분
idle_in_transaction_session_timeout = 300000

# 기본 트랜잭션 격리 수준
default_transaction_isolation = 'read committed'
```

## 11.2 로케일 및 형식 (Locale and Formatting)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `DateStyle` | string | `ISO, MDY` | 날짜/시간 값의 표시 형식 및 모호한 날짜 입력 해석 |
| `IntervalStyle` | enum | `postgres` | 간격 값의 표시 형식. 값: `sql_standard`, `postgres`, `postgres_verbose`, `iso_8601` |
| `TimeZone` | string | `GMT` | 타임스탬프 표시/해석용 시간대 |
| `timezone_abbreviations` | string | `'Default'` | datetime 입력에 허용되는 시간대 약어 컬렉션 |
| `extra_float_digits` | integer | `1` | 부동 소수점 값의 텍스트 출력 자릿수 조정 |
| `client_encoding` | string | (데이터베이스 인코딩) | 클라이언트 측 문자 집합 인코딩 |
| `lc_messages` | string | (환경에서 상속) | 표시되는 메시지의 언어 |
| `lc_monetary` | string | (환경에서 상속) | 화폐 금액 형식화용 로케일 |
| `lc_numeric` | string | (환경에서 상속) | 숫자 형식화용 로케일 |
| `lc_time` | string | (환경에서 상속) | 날짜/시간 형식화용 로케일 |
| `default_text_search_config` | string | `pg_catalog.simple` | 명시적 인수가 없는 함수용 텍스트 검색 설정 |

### 예시: 로케일 설정

```ini
# 날짜 형식 설정
DateStyle = 'ISO, YMD'

# 시간대 설정
TimeZone = 'Asia/Seoul'

# 클라이언트 인코딩
client_encoding = 'UTF8'

# 로케일 설정
lc_messages = 'ko_KR.UTF-8'
lc_monetary = 'ko_KR.UTF-8'
lc_numeric = 'ko_KR.UTF-8'
lc_time = 'ko_KR.UTF-8'
```

## 11.3 공유 라이브러리 사전 로딩 (Shared Library Preloading)

| 파라미터 | 타입 | 권한 | 설명 |
|----------|------|------|------|
| `local_preload_libraries` | string | 모든 사용자 | 연결 시작 시 사전 로드되는 공유 라이브러리. `plugins` 하위 디렉터리로 제한 |
| `session_preload_libraries` | string | 슈퍼유저만 | 연결 시작 시 사전 로드되는 공유 라이브러리. 디버깅/성능 측정용 |
| `shared_preload_libraries` | string | 서버 시작 시에만 | 서버 시작 시 사전 로드되는 공유 라이브러리. 서버 재시작 필요 |
| `jit_provider` | string | 서버 시작 시에만 | JIT 제공자 라이브러리 이름. 기본값: `llvmjit` |

### 예시: 확장 사전 로딩

```ini
# pg_stat_statements 및 auto_explain 사전 로드
shared_preload_libraries = 'pg_stat_statements, auto_explain'
```

## 11.4 기타 기본값 (Other Defaults)

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `dynamic_library_path` | string | `'$libdir'` | 동적으로 로드 가능한 모듈용 절대 디렉터리 경로의 콜론으로 구분된 목록 |
| `extension_control_path` | string | `'$system'` | 확장 제어 파일을 검색할 경로의 콜론으로 구분된 목록 |
| `gin_fuzzy_search_limit` | integer | (소프트 제한) | GIN 인덱스 스캔이 반환하는 집합 크기의 소프트 상한 |

---

# 12. 잠금 관리

## 잠금 관리 파라미터

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `deadlock_timeout` | integer | `1s` | 교착 상태 조건 확인 전 잠금 대기 시간 |
| `max_locks_per_transaction` | integer | `64` | 공유 잠금 테이블에서 트랜잭션당 평균 객체 잠금 수 제한 |
| `max_pred_locks_per_transaction` | integer | `64` | 공유 술어 잠금 테이블에서 트랜잭션당 술어 잠금 제한 |
| `max_pred_locks_per_relation` | integer | `-2` | 전체 관계 잠금으로 승격하기 전 술어 잠금할 수 있는 페이지/튜플 수 제어 |
| `max_pred_locks_per_page` | integer | `2` | 전체 페이지 잠금으로 승격하기 전 단일 페이지에서 술어 잠금할 수 있는 행 수 제어 |

### deadlock_timeout

- 타입: 정수
- 기본값: `1s` (1초)
- 단위: 밀리초 (단위 미지정 시)

교착 상태 확인은 비용이 많이 들기 때문에 지속적으로 수행되지 않습니다. 이 값을 늘리면 불필요한 교착 상태 확인이 줄어들지만 교착 상태 오류 보고가 지연됩니다. 이상적으로는 일반적인 트랜잭션 시간을 초과하여 교착 상태 확인 전에 잠금이 해제되도록 해야 합니다.

참고: `log_lock_waits`가 활성화된 경우 이 파라미터는 잠금 대기 메시지 로깅 전 시간도 제어합니다.

### max_locks_per_transaction

- 타입: 정수
- 기본값: `64`
- 설정 가능: 서버 시작 시에만

행 잠금 제한이 아닙니다 (행 잠금은 무제한). 단일 트랜잭션에서 많은 다른 테이블을 건드리는 쿼리가 있으면 증가시킵니다.

중요: 대기 서버에서는 기본 서버와 같거나 높은 값으로 설정해야 합니다.

### 예시: 잠금 설정

```ini
# 교착 상태 타임아웃 2초
deadlock_timeout = 2s

# 복잡한 트랜잭션을 위해 잠금 제한 증가
max_locks_per_transaction = 128
max_pred_locks_per_transaction = 128
```

---

# 참고 자료

- [PostgreSQL 18 공식 문서](https://www.postgresql.org/docs/18/)
- [PostgreSQL Server Configuration](https://www.postgresql.org/docs/current/runtime-config.html)
- [PostgreSQL 설정 파일 위치](https://www.postgresql.org/docs/current/runtime-config-file-locations.html)
- [PostgreSQL 연결 및 인증](https://www.postgresql.org/docs/current/runtime-config-connection.html)
- [PostgreSQL 리소스 소비](https://www.postgresql.org/docs/current/runtime-config-resource.html)
- [PostgreSQL WAL](https://www.postgresql.org/docs/current/runtime-config-wal.html)
- [PostgreSQL 복제](https://www.postgresql.org/docs/current/runtime-config-replication.html)
- [PostgreSQL 쿼리 계획](https://www.postgresql.org/docs/current/runtime-config-query.html)
- [PostgreSQL 로깅](https://www.postgresql.org/docs/current/runtime-config-logging.html)
- [PostgreSQL 통계](https://www.postgresql.org/docs/current/runtime-config-statistics.html)
- [PostgreSQL Vacuum](https://www.postgresql.org/docs/current/runtime-config-vacuum.html)
- [PostgreSQL 클라이언트 연결 기본값](https://www.postgresql.org/docs/current/runtime-config-client.html)
- [PostgreSQL 잠금 관리](https://www.postgresql.org/docs/current/runtime-config-locks.html)
