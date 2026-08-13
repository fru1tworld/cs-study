# PostgreSQL 서버 관리

## PostgreSQL 서버 설정 (Server Configuration)

### 개요

PostgreSQL 서버의 동작을 제어하는 다양한 설정 파라미터가 존재함. 파라미터를 설정하는 방법과 각 파라미터의 의미를 설명함.

---

### 목차

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

## 1. 파라미터 설정 방법

### 1.1 파라미터 이름과 값

모든 파라미터 이름은 대소문자를 구분하지 않음. 파라미터는 다음 5가지 타입 중 하나를 사용함.

- Boolean: 참/거짓 값 · 예시 `on`, `off`, `true`, `false`, `yes`, `no`, `1`, `0`
- String: 문자열 값 · 예시 `'value'` (작은따옴표 사용)
- Numeric (integer/float): 숫자 값 · 예시 `100`, `3.14`, `0x1F` (16진수)
- Numeric with Unit: 단위가 있는 숫자 · 예시 `'128MB'`, `'5min'`, `'500ms'`
- Enumerated: 열거형 값 · 예시 `warning`, `error`, `log`

#### 메모리 단위

- `B`: 바이트, 승수 1
- `kB`: 킬로바이트, 승수 1024
- `MB`: 메가바이트, 승수 1024^2
- `GB`: 기가바이트, 승수 1024^3
- `TB`: 테라바이트, 승수 1024^4

#### 시간 단위

- `us`: 마이크로초
- `ms`: 밀리초
- `s`: 초
- `min`: 분
- `h`: 시간
- `d`: 일

### 1.2 설정 파일을 통한 파라미터 상호작용

#### postgresql.conf 파일

주요 설정 파일이며 데이터 디렉터리에 위치함.

```ini
# 주석은 #으로 시작합니다
log_connections = all
log_destination = 'syslog'
search_path = '"$user", public'
shared_buffers = 128MB
```

형식 규칙:
- 한 줄에 하나의 파라미터 → 등호(`=`)는 선택 사항
- 공백은 무시됨 (따옴표 내부 제외)
- 빈 줄과 `#` 주석은 무시됨
- 단순하지 않은 값은 작은따옴표로 감싸야 함
- 중복 항목이 있으면 마지막 값이 적용됨

#### 설정 다시 로드하기

```bash
# 쉘에서
pg_ctl reload

# SQL에서
SELECT pg_reload_conf();
```

#### postgresql.auto.conf 파일

`ALTER SYSTEM` 명령으로 자동 편집되는 파일임.

```sql
ALTER SYSTEM SET parameter = value;
```

- `postgresql.conf`와 함께 읽힘 → `postgresql.conf` 설정을 덮어씀

#### Include 지시문

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

### 1.3 SQL을 통한 파라미터 상호작용

#### 현재 값 확인

```sql
-- SHOW 명령 사용
SHOW parameter_name;

-- 함수 사용
SELECT current_setting('parameter_name');
```

#### 세션 수준 설정

```sql
-- SET 명령
SET parameter_name = value;

-- 함수 사용
SELECT set_config('parameter_name', 'value', false);
```

#### 데이터베이스/역할별 설정

```sql
-- 특정 데이터베이스에 대한 설정
ALTER DATABASE dbname SET parameter = value;

-- 특정 역할에 대한 설정
ALTER ROLE rolename SET parameter = value;
```

### 1.4 쉘을 통한 파라미터 상호작용

#### 서버 시작 시

```bash
postgres -c log_connections=all --log-destination='syslog'
```

#### PGOPTIONS 환경 변수

```bash
env PGOPTIONS="-c geqo=off --statement-timeout=5min" psql
```

### 1.5 파라미터 컨텍스트

파라미터는 설정 가능한 시점에 따라 분류됨.

- `internal`: 읽기 전용, 변경 불가
- `postmaster`: 서버 시작 시에만 설정 가능
- `sighup`: postgresql.conf를 통해 설정 → SIGHUP으로 다시 로드
- `superuser`: 슈퍼유저가 세션 수준에서 설정 가능
- `user`: 모든 사용자가 세션 수준에서 설정 가능

### 1.6 설정 우선순위 (높은 것에서 낮은 것 순)

1. 서버 명령줄 (`-c`, `--name=value`)
2. `PGOPTIONS` 환경 변수
3. `ALTER DATABASE` / `ALTER ROLE` 설정
4. `ALTER SYSTEM` / `postgresql.auto.conf`
5. `postgresql.conf`
6. 내장 기본값

---

## 2. 파일 위치

### 파일 위치 파라미터

- `data_directory`: string · 데이터 저장 디렉터리 · 서버 시작 시에만 설정 가능
- `config_file`: string · 주 서버 설정 파일 (postgresql.conf) · 명령줄에서만 설정 가능
- `hba_file`: string · 호스트 기반 인증 설정 (pg_hba.conf) · 서버 시작 시에만 설정 가능
- `ident_file`: string · 사용자 이름 매핑 설정 (pg_ident.conf) · 서버 시작 시에만 설정 가능
- `external_pid_file`: string · 관리 프로그램용 추가 PID 파일 · 서버 시작 시에만 설정 가능

### 기본 동작

기본적으로 이 파라미터들은 명시적으로 설정되지 않음. 대신:
- 데이터 디렉터리는 `-D` 명령줄 옵션 또는 `PGDATA` 환경 변수로 지정
- 모든 설정 파일은 데이터 디렉터리 내에 위치

### 사용 예시

#### 설정 파일을 데이터 디렉터리와 분리

```bash
# /etc/postgresql에 설정 파일이 있는 상태로 PostgreSQL 시작
postgres -D /etc/postgresql
```

`postgresql.conf`에서:
```ini
data_directory = '/var/lib/postgresql/data'
```

#### 개별 설정 파일 경로 지정

```ini
data_directory = '/var/lib/postgresql/data'
config_file = '/etc/postgresql/postgresql.conf'
hba_file = '/etc/postgresql/pg_hba.conf'
ident_file = '/etc/postgresql/pg_ident.conf'
external_pid_file = '/var/run/postgresql/postgresql.pid'
```

---

## 3. 연결 및 인증

### 3.1 연결 설정 (Connection Settings)

- `listen_addresses`: string, 기본값 `localhost` · 서버가 수신하는 TCP/IP 주소 · `*`는 모든 인터페이스, `0.0.0.0`은 IPv4, `::`는 IPv6
- `port`: integer, 기본값 `5432` · 서버 연결용 TCP 포트
- `max_connections`: integer, 기본값 `100` · 최대 동시 연결 수 · 대기 서버에서는 기본 서버와 같거나 높아야 함
- `reserved_connections`: integer, 기본값 `0` · `pg_use_reserved_connections` 권한이 있는 역할을 위해 예약된 연결 슬롯
- `superuser_reserved_connections`: integer, 기본값 `3` · 슈퍼유저를 위해 예약된 연결 슬롯
- `unix_socket_directories`: string, 기본값 `/tmp` · Unix 도메인 소켓용 디렉터리 · 쉼표로 구분하여 여러 개 지정 가능
- `unix_socket_group`: string, 기본값 (빈 문자열) · Unix 도메인 소켓의 소유 그룹
- `unix_socket_permissions`: integer, 기본값 `0777` · 소켓의 Unix 파일 권한 (8진수 형식)
- `bonjour`: boolean, 기본값 `off` · Bonjour 서비스 광고 활성화
- `bonjour_name`: string, 기본값 (빈 문자열) · Bonjour 서비스 이름 · 비어있으면 컴퓨터 이름 사용

#### 예시: 연결 설정

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

### 3.2 TCP 설정

- `tcp_keepalives_idle`: integer, 기본값 `0` · TCP keepalive 전송 전 대기 시간(초) · 0은 OS 기본값 사용
- `tcp_keepalives_interval`: integer, 기본값 `0` · 응답 없는 TCP keepalive 재전송 간격(초)
- `tcp_keepalives_count`: integer, 기본값 `0` · 연결 종료 전 손실 허용 TCP keepalive 메시지 수
- `tcp_user_timeout`: integer, 기본값 `0` · 응답 없는 데이터가 TCP 연결 종료 전까지 유지되는 시간(ms)
- `client_connection_check_interval`: integer, 기본값 `0` · 긴 쿼리 중 클라이언트 연결 확인 폴링 간격(ms)

### 3.3 인증 (Authentication)

- `authentication_timeout`: integer, 기본값 `1m` · 클라이언트 인증 완료까지 최대 시간
- `password_encryption`: enum, 기본값 `scram-sha-256` · 비밀번호 암호화 알고리즘 · `scram-sha-256` 또는 `md5` (비권장)
- `scram_iterations`: integer, 기본값 `4096` · SCRAM-SHA-256 암호화를 위한 계산 반복 횟수
- `md5_password_warnings`: boolean, 기본값 `on` · MD5 비밀번호 사용 중단 경고 표시 여부
- `krb_server_keyfile`: string, 기본값 `FILE:/usr/local/pgsql/etc/krb5.keytab` · Kerberos 키 파일 위치
- `krb_caseins_users`: boolean, 기본값 `off` · GSSAPI 사용자 이름 대소문자 구분 여부
- `gss_accept_delegation`: boolean, 기본값 `off` · 클라이언트로부터 위임된 GSSAPI 자격 증명 수락 여부

#### 예시: 인증 설정

```ini
# SCRAM-SHA-256 비밀번호 암호화 사용
password_encryption = scram-sha-256

# 인증 타임아웃 30초
authentication_timeout = 30s

# SCRAM 반복 횟수 증가 (보안 강화)
scram_iterations = 8192
```

### 3.4 SSL/TLS

- `ssl`: boolean, 기본값 `off` · SSL/TLS 연결 활성화
- `ssl_ca_file`: string, 기본값 (빈 문자열) · SSL 서버 인증 기관(CA) 파일
- `ssl_cert_file`: string, 기본값 `server.crt` · SSL 서버 인증서 파일
- `ssl_key_file`: string, 기본값 `server.key` · SSL 서버 개인 키 파일
- `ssl_crl_file`: string, 기본값 (빈 문자열) · SSL 클라이언트 인증서 해지 목록(CRL) 파일
- `ssl_crl_dir`: string, 기본값 (빈 문자열) · SSL CRL 파일이 포함된 디렉터리
- `ssl_ciphers`: string, 기본값 `HIGH:MEDIUM:+3DES:!aNULL` · TLS 1.2 이하용 암호 스위트
- `ssl_tls13_ciphers`: string, 기본값 (OpenSSL 기본값) · TLS 1.3 연결용 암호 스위트
- `ssl_prefer_server_ciphers`: boolean, 기본값 `on` · 클라이언트 대신 서버의 SSL 암호 기본 설정 사용
- `ssl_groups`: string, 기본값 `X25519:prime256v1` · 키 교환용 ECDH 곡선
- `ssl_min_protocol_version`: enum, 기본값 `TLSv1.2` · 최소 TLS 버전
- `ssl_max_protocol_version`: enum, 기본값 (빈 문자열, 모든 버전 허용) · 최대 TLS 버전
- `ssl_dh_params_file`: string, 기본값 (빈 문자열) · 임시 DH 암호용 Diffie-Hellman 파라미터 파일
- `ssl_passphrase_command`: string, 기본값 (빈 문자열) · SSL 암호문 획득을 위한 외부 명령
- `ssl_passphrase_command_supports_reload`: boolean, 기본값 `off` · 설정 다시 로드 시 암호문 명령 호출 여부

#### 예시: SSL 설정

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

#### DH 파라미터 파일 생성

```bash
openssl dhparam -out dhparams.pem 2048
```

---

## 4. 리소스 소비

### 4.1 메모리 (Memory)

- `shared_buffers`: integer, 기본값 `128MB` · 공유 메모리 버퍼에 할당할 메모리 양 · RAM이 1GB 이상인 시스템에서는 RAM의 25% 권장, 최대 40%
- `huge_pages`: enum, 기본값 `try` · 주 공유 메모리용 huge pages 제어 (`try`, `on`, `off`)
- `huge_page_size`: integer, 기본값 `0` · 활성화 시 huge page 크기
- `temp_buffers`: integer, 기본값 `8MB` · 임시 테이블용 세션당 최대 메모리
- `max_prepared_transactions`: integer, 기본값 `0` · 최대 동시 준비된 트랜잭션 수
- `work_mem`: integer, 기본값 `4MB` · 쿼리 작업(정렬, 해시 테이블)에 사용하는 기본 메모리 · 초과 시 디스크에 씀
- `hash_mem_multiplier`: float, 기본값 `2.0` · 해시 작업 메모리 제한 승수: `work_mem * hash_mem_multiplier`
- `maintenance_work_mem`: integer, 기본값 `64MB` · 유지보수 작업(VACUUM, CREATE INDEX, ALTER TABLE)용 메모리
- `autovacuum_work_mem`: integer, 기본값 `-1` · autovacuum 워커당 메모리 · 기본값은 `maintenance_work_mem` 사용
- `vacuum_buffer_usage_limit`: integer, 기본값 `2MB` · VACUUM/ANALYZE용 버퍼 접근 전략 크기 · 범위 128KB-16GB
- `logical_decoding_work_mem`: integer, 기본값 `64MB` · 논리적 디코딩에 사용하는 메모리 · 초과 시 디스크에 씀
- `max_stack_depth`: integer, 기본값 `2MB` · 최대 안전 실행 스택 깊이
- `shared_memory_type`: enum, 기본값 `mmap` · 공유 메모리 구현 방식 (`mmap`, `sysv`, `windows`)
- `dynamic_shared_memory_type`: enum, 기본값 `posix` · 동적 공유 메모리 구현 방식
- `min_dynamic_shared_memory`: integer, 기본값 `0` · 병렬 쿼리용으로 시작 시 할당되는 메모리

#### 예시: 메모리 설정

```ini
# 8GB RAM 시스템에서의 권장 설정
shared_buffers = 2GB
work_mem = 64MB
maintenance_work_mem = 512MB
effective_cache_size = 6GB

# Huge pages 사용 시도
huge_pages = try
```

### 4.2 디스크 (Disk)

- `temp_file_limit`: integer, 기본값 `-1` · 프로세스당 임시 파일 최대 디스크 공간 · `-1`은 무제한
- `file_copy_method`: enum, 기본값 `COPY` · 파일 복사 방법 · `COPY` (기본) 또는 `CLONE` (OS 지원 시)
- `max_notify_queue_pages`: integer, 기본값 `1048576` · NOTIFY/LISTEN 큐용 최대 할당 페이지

### 4.3 커널 리소스 사용량

- `max_files_per_process`: integer, 기본값 `1000` · 서버 서브프로세스당 최대 동시 열린 파일 수

### 4.4 백그라운드 라이터 (Background Writer)

- `bgwriter_delay`: integer, 기본값 `200ms` · 백그라운드 라이터 활동 라운드 간 지연 시간
- `bgwriter_lru_maxpages`: integer, 기본값 `100` · 라운드당 기록되는 최대 버퍼 수 · `0`은 백그라운드 쓰기 비활성화
- `bgwriter_lru_multiplier`: float, 기본값 `2.0` · 더티 버퍼 추정 승수
- `bgwriter_flush_after`: integer, 기본값 `512kB` (Linux) · 이 양 이후 OS writeback 강제

### 4.5 I/O

- `backend_flush_after`: integer, 기본값 `0` · 백엔드가 이 양을 쓴 후 OS writeback 강제
- `effective_io_concurrency`: integer, 기본값 `16` · 예상되는 동시 I/O 작업 수 · 범위 0-1000
- `maintenance_io_concurrency`: integer, 기본값 `16` · 유지보수 작업용 동시 I/O
- `io_max_combine_limit`: integer, 기본값 `128kB` · 결합 작업의 최대 I/O 크기
- `io_combine_limit`: integer, 기본값 `128kB` · 결합 작업의 최대 I/O 크기 (`io_max_combine_limit`로 제한됨)
- `io_max_concurrency`: integer, 기본값 `-1` · 프로세스당 최대 동시 I/O · `-1`은 설정 기반 자동 선택
- `io_method`: enum, 기본값 `worker` · 비동기 I/O 방식 · `worker`, `io_uring`, `sync`
- `io_workers`: integer, 기본값 `3` · I/O 워커 프로세스 수 (`io_method=worker`일 때만 적용)

#### 예시: I/O 설정 (SSD용)

```ini
# SSD 스토리지에 최적화된 설정
effective_io_concurrency = 200
maintenance_io_concurrency = 200
random_page_cost = 1.1
```

### 4.6 워커 프로세스 (Worker Processes)

- `max_worker_processes`: integer, 기본값 `8` · 최대 백그라운드 프로세스 수
- `max_parallel_workers_per_gather`: integer, 기본값 `2` · Gather/Gather Merge 노드당 최대 워커 수 · `0`은 병렬 쿼리 비활성화
- `max_parallel_maintenance_workers`: integer, 기본값 `2` · 유틸리티 명령(CREATE INDEX, VACUUM)용 최대 병렬 워커 수
- `max_parallel_workers`: integer, 기본값 `8` · 모든 병렬 작업용 최대 워커 수 · `max_worker_processes` 이하여야 함
- `parallel_leader_participation`: boolean, 기본값 `on` · 리더 프로세스가 Gather 노드 아래에서 실행 허용

#### 예시: 병렬 처리 설정

```ini
# 병렬 쿼리 설정
max_worker_processes = 16
max_parallel_workers = 8
max_parallel_workers_per_gather = 4
max_parallel_maintenance_workers = 4
```

---

## 5. Write Ahead Log (WAL)

### 5.1 설정 (Settings)

- `wal_level`: enum, 기본값 `replica` · WAL에 기록되는 정보량 · 값 `minimal`, `replica`, `logical`
- `fsync`: boolean, 기본값 `on` · `fsync()` 호출을 통해 업데이트가 물리적으로 디스크에 기록되도록 보장
- `synchronous_commit`: enum, 기본값 `on` · 성공 반환 전 WAL 처리 완료 시점 제어 · 값 `remote_apply`, `on`, `remote_write`, `local`, `off`
- `wal_sync_method`: enum, 기본값 (플랫폼 종속) · WAL 업데이트를 디스크에 강제하는 방법
- `full_page_writes`: boolean, 기본값 `on` · 체크포인트 후 첫 수정 시 전체 디스크 페이지 내용을 WAL에 기록
- `wal_log_hints`: boolean, 기본값 `off` · 비중요 힌트 비트 수정에 대해 전체 페이지 내용 로깅
- `wal_compression`: enum, 기본값 `off` · WAL 압축 활성화 · 값 `pglz`, `lz4`, `zstd`
- `wal_init_zero`: boolean, 기본값 `on` · 새 WAL 파일을 0으로 채움
- `wal_recycle`: boolean, 기본값 `on` · 새 파일 생성 대신 WAL 파일 이름 변경으로 재활용
- `wal_buffers`: integer, 기본값 `-1` (자동) · 기록되지 않은 WAL 데이터용 공유 메모리
- `wal_writer_delay`: integer, 기본값 `200ms` · WAL 라이터가 WAL을 플러시하는 빈도
- `wal_writer_flush_after`: integer, 기본값 `1MB` · WAL 라이터 플러시를 위한 볼륨 임계값
- `wal_skip_threshold`: integer, 기본값 `2MB` · `wal_level=minimal`일 때 WAL에 쓸지 파일 fsync할지 결정
- `commit_delay`: integer, 기본값 `0` · 그룹 커밋 처리량 향상을 위한 WAL 플러시 전 시간 지연(마이크로초)
- `commit_siblings`: integer, 기본값 `5` · `commit_delay` 적용 전 최소 동시 트랜잭션 수

#### 예시: WAL 설정

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

### 5.2 체크포인트 (Checkpoints)

- `checkpoint_timeout`: integer, 기본값 `5min` · 자동 WAL 체크포인트 간 최대 시간 (30초 - 1일)
- `checkpoint_completion_target`: float, 기본값 `0.9` · 체크포인트를 분산시킬 체크포인트 간 시간의 비율
- `checkpoint_flush_after`: integer, 기본값 `256kB` (Linux) · 체크포인트 중 강제 writeback 임계값
- `checkpoint_warning`: integer, 기본값 `30s` · 체크포인트가 이 간격보다 더 자주 발생하면 경고 로깅
- `max_wal_size`: integer, 기본값 `1GB` · 자동 체크포인트 중 최대 WAL 크기 (소프트 제한)
- `min_wal_size`: integer, 기본값 `80MB` · 오래된 WAL 파일 재활용 전 최소 WAL 크기

#### 예시: 체크포인트 설정

```ini
# 체크포인트 설정
checkpoint_timeout = 15min
checkpoint_completion_target = 0.9
max_wal_size = 4GB
min_wal_size = 1GB
```

### 5.3 아카이빙 (Archiving)

- `archive_mode`: enum, 기본값 `off` · WAL 아카이빙 활성화 · 값 `off`, `on`, `always`
- `archive_command`: string, 기본값 `''` · 완료된 WAL 세그먼트를 아카이브하는 쉘 명령 · 플레이스홀더 `%p` (경로), `%f` (파일명)
- `archive_library`: string, 기본값 `''` · WAL 세그먼트 아카이빙용 라이브러리 (`archive_command`의 대안)
- `archive_timeout`: integer, 기본값 `0` · 지정된 시간 동안 활동이 없으면 WAL 세그먼트 전환 강제 (초)

#### 아카이브 명령 예시

```ini
# Linux/Unix
archive_command = 'cp %p /mnt/server/archivedir/%f'

# Windows
archive_command = 'copy "%p" "C:\\server\\archivedir\\%f"'

# 원격 서버로 복사
archive_command = 'scp %p backup@archiveserver:/archive/%f'
```

### 5.4 복구 (Recovery)

- `recovery_prefetch`: enum, 기본값 `try` · 복구 중 블록 프리페치 · 값 `off`, `on`, `try`
- `wal_decode_buffer_size`: integer, 기본값 `512kB` · 프리페칭을 위한 WAL lookahead 제한

### 5.5 아카이브 복구 (Archive Recovery)

- `restore_command`: string, 기본값 `''` · 아카이브된 WAL 세그먼트를 검색하는 쉘 명령 · 플레이스홀더 `%f` (파일명), `%p` (경로), `%r` (재시작 지점 파일)
- `archive_cleanup_command`: string, 기본값 `''` · 정리를 위해 모든 restartpoint에서 실행되는 쉘 명령
- `recovery_end_command`: string, 기본값 `''` · 복구 종료 시 한 번 실행되는 쉘 명령

#### 복원 명령 예시

```ini
restore_command = 'cp /mnt/server/archivedir/%f "%p"'
archive_cleanup_command = 'pg_archivecleanup /mnt/server/archivedir %r'
```

### 5.6 복구 대상 (Recovery Target)

- `recovery_target`: string · `'immediate'`로 설정하면 가장 빠른 일관된 상태로 복구
- `recovery_target_name`: string · 복구할 명명된 복원 지점
- `recovery_target_time`: timestamp · 복구가 진행될 타임스탬프
- `recovery_target_xid`: string · 복구가 진행될 트랜잭션 ID
- `recovery_target_lsn`: pg_lsn · 복구가 진행될 WAL LSN
- `recovery_target_inclusive`: boolean, 기본값 `on` · 복구 대상 포함(`on`) 또는 제외(`off`)
- `recovery_target_timeline`: string, 기본값 `latest` · 복구할 타임라인 · 값 `current`, `latest`, 또는 16진수 타임라인 ID
- `recovery_target_action`: enum, 기본값 `pause` · 복구 대상 도달 후 작업 · 값 `pause`, `promote`, `shutdown`

참고: `recovery_target`, `recovery_target_lsn`, `recovery_target_name`, `recovery_target_time`, `recovery_target_xid` 중 하나만 지정 가능.

### 5.7 WAL 요약 (WAL Summarization)

- `summarize_wal`: boolean, 기본값 `off` · WAL 요약기 프로세스 활성화 (증분 백업에 필요)
- `wal_summary_keep_time`: integer, 기본값 `10d` · 오래된 WAL 요약이 자동으로 제거되는 시간

---

## 6. 복제

### 6.1 송신 서버 (Sending Servers)

- `max_wal_senders`: integer, 기본값 `10` · 대기 서버 또는 스트리밍 베이스 백업 클라이언트의 최대 동시 연결 수
- `max_replication_slots`: integer, 기본값 `10` · 서버가 지원할 수 있는 최대 복제 슬롯 수
- `wal_keep_size`: integer, 기본값 `0` · 대기 스트리밍 복제용으로 `pg_wal` 디렉터리에 유지되는 과거 WAL 파일의 최소 크기(MB)
- `max_slot_wal_keep_size`: integer, 기본값 `-1` (무제한) · 복제 슬롯이 `pg_wal` 디렉터리에 유지할 수 있는 WAL 파일의 최대 크기(MB)
- `idle_replication_slot_timeout`: integer, 기본값 `0` (비활성화) · 이 기간보다 오래 비활성인 복제 슬롯 무효화
- `wal_sender_timeout`: integer, 기본값 `60s` · 비활성 복제 연결을 이 시간 후 종료
- `track_commit_timestamp`: boolean, 기본값 `off` · 트랜잭션의 커밋 시간 기록
- `synchronized_standby_slots`: string, 기본값 (빈 문자열) · 논리적 WAL 송신기 프로세스가 대기할 스트리밍 복제 대기 슬롯 이름 목록

### 6.2 기본 서버 (Primary Server)

- `synchronous_standby_names`: string, 기본값 (빈 문자열) · 동기 복제를 지원할 수 있는 대기 서버 목록

#### 동기 복제 구문

```ini
# 첫 번째 3개 대기 서버의 동기 복제
synchronous_standby_names = 'FIRST 3 (s1, s2, s3, s4)'

# 4개 중 아무 3개 대기 서버
synchronous_standby_names = 'ANY 3 (s1, s2, s3, s4)'

# 레거시 구문 (FIRST 1과 동일)
synchronous_standby_names = 's1, s2, s3'
```

### 6.3 대기 서버 (Standby Servers)

- `primary_conninfo`: string, 기본값 (빈 문자열) · 대기 서버가 송신 서버에 연결하기 위한 연결 문자열
- `primary_slot_name`: string, 기본값 (빈 문자열) · 송신 서버에 연결할 때 사용할 기존 복제 슬롯
- `hot_standby`: boolean, 기본값 `on` · 복구 중 연결 및 쿼리 허용
- `max_standby_archive_delay`: integer, 기본값 `30s` · WAL 아카이브 항목과 충돌하는 대기 쿼리를 취소하기 전 최대 대기 시간
- `max_standby_streaming_delay`: integer, 기본값 `30s` · 들어오는 WAL 항목과 충돌하는 대기 쿼리를 취소하기 전 최대 대기 시간
- `wal_receiver_create_temp_slot`: boolean, 기본값 `off` · 영구 슬롯이 설정되지 않은 경우 원격 인스턴스에 임시 복제 슬롯 생성
- `wal_receiver_status_interval`: integer, 기본값 `10s` · WAL 수신기가 기본 서버에 복제 진행 상황을 전송하는 최소 빈도
- `hot_standby_feedback`: boolean, 기본값 `off` · 대기 서버에서 실행 중인 쿼리에 대한 피드백을 기본 서버로 전송
- `wal_receiver_timeout`: integer, 기본값 `60s` · 비활성 복제 연결을 이 시간 후 종료
- `wal_retrieve_retry_interval`: integer, 기본값 `5s` · WAL 데이터를 사용할 수 없을 때 검색 재시도 전 대기 시간
- `recovery_min_apply_delay`: integer, 기본값 `0` (지연 없음) · 특정 시점 복구를 위해 복구를 지연
- `sync_replication_slots`: boolean, 기본값 `off` · 기본 서버에서 대기 서버로 논리적 장애 조치 슬롯 동기화

#### 예시: 기본 연결 정보

```ini
primary_conninfo = 'host=primary.example.com port=5432 user=replication password=secret'
```

#### 예시: 지연 복제

```ini
recovery_min_apply_delay = '5min'
```

### 6.4 구독자 (Subscribers)

- `max_active_replication_origins`: integer, 기본값 `10` · 동시에 추적할 수 있는 최대 복제 출처 수
- `max_logical_replication_workers`: integer, 기본값 `4` · 최대 논리적 복제 워커 수
- `max_sync_workers_per_subscription`: integer, 기본값 `2` · 구독당 최대 동기화 워커 수
- `max_parallel_apply_workers_per_subscription`: integer, 기본값 `2` · 진행 중인 트랜잭션 스트리밍용 구독당 최대 병렬 적용 워커 수

---

## 7. 쿼리 계획

### 7.1 플래너 메서드 설정

이 파라미터들은 쿼리 옵티마이저가 사용할 수 있는 쿼리 계획 유형을 제어함. 달리 명시되지 않는 한 모두 기본값 `on`.

- `enable_async_append`: boolean, 기본값 `on` · 비동기 인식 append 계획 유형 활성화
- `enable_bitmapscan`: boolean, 기본값 `on` · 비트맵 스캔 계획 유형 활성화
- `enable_gathermerge`: boolean, 기본값 `on` · gather merge 계획 유형 활성화
- `enable_hashagg`: boolean, 기본값 `on` · 해시 집계 계획 유형 활성화
- `enable_hashjoin`: boolean, 기본값 `on` · 해시 조인 계획 유형 활성화
- `enable_incremental_sort`: boolean, 기본값 `on` · 증분 정렬 단계 활성화
- `enable_indexscan`: boolean, 기본값 `on` · 인덱스 스캔 및 인덱스 전용 스캔 계획 유형 활성화
- `enable_indexonlyscan`: boolean, 기본값 `on` · 인덱스 전용 스캔 계획 유형 활성화
- `enable_material`: boolean, 기본값 `on` · 구체화 활성화 (완전히 억제할 수 없음)
- `enable_memoize`: boolean, 기본값 `on` · 중첩 루프 조인에서 파라미터화된 스캔 결과 캐시
- `enable_mergejoin`: boolean, 기본값 `on` · 병합 조인 계획 유형 활성화
- `enable_nestloop`: boolean, 기본값 `on` · 중첩 루프 조인 계획 활성화 (완전히 억제할 수 없음)
- `enable_parallel_append`: boolean, 기본값 `on` · 병렬 인식 append 계획 유형 활성화
- `enable_parallel_hash`: boolean, 기본값 `on` · 병렬 해시를 사용한 해시 조인 활성화
- `enable_partition_pruning`: boolean, 기본값 `on` · 쿼리 계획에서 파티션 제거
- `enable_partitionwise_join`: boolean, 기본값 `off` · 일치하는 파티션을 조인하여 파티션된 테이블 조인
- `enable_partitionwise_aggregate`: boolean, 기본값 `off` · 파티션된 테이블에서 파티션별로 별도로 그룹화/집계
- `enable_seqscan`: boolean, 기본값 `on` · 순차 스캔 계획 유형 활성화 (완전히 억제할 수 없음)
- `enable_sort`: boolean, 기본값 `on` · 명시적 정렬 단계 활성화 (완전히 억제할 수 없음)
- `enable_tidscan`: boolean, 기본값 `on` · TID 스캔 계획 유형 활성화

#### 예시: 특정 계획 유형 비활성화 (문제 해결용)

```sql
-- 해시 조인 비활성화
SET enable_hashjoin = off;

-- 순차 스캔 비활성화 (인덱스 스캔 강제)
SET enable_seqscan = off;
```

### 7.2 플래너 비용 상수

비용 변수는 절대값이 아닌 상대적 비율만 의미를 가지는 임의의 척도를 사용함. 기준: `seq_page_cost = 1.0`

- `seq_page_cost`: float, 기본값 `1.0` · 순차 디스크 페이지 가져오기 비용
- `random_page_cost`: float, 기본값 `4.0` · 비순차 디스크 페이지 가져오기 비용
- `cpu_tuple_cost`: float, 기본값 `0.01` · 각 행 처리 비용
- `cpu_index_tuple_cost`: float, 기본값 `0.005` · 각 인덱스 항목 처리 비용
- `cpu_operator_cost`: float, 기본값 `0.0025` · 연산자/함수 처리 비용
- `parallel_setup_cost`: float, 기본값 `1000` · 병렬 워커 시작 비용
- `parallel_tuple_cost`: float, 기본값 `0.1` · 병렬 워커에서 튜플 전송 비용
- `min_parallel_table_scan_size`: integer, 기본값 `8MB` · 병렬 스캔을 위한 최소 테이블 데이터
- `min_parallel_index_scan_size`: integer, 기본값 `512kB` · 병렬 스캔을 위한 최소 인덱스 데이터
- `effective_cache_size`: integer, 기본값 `4GB` · 쿼리에 사용 가능한 유효 디스크 캐시 크기
- `jit_above_cost`: float, 기본값 `100000` · JIT 컴파일을 위한 쿼리 비용 임계값
- `jit_inline_above_cost`: float, 기본값 `500000` · JIT 인라이닝을 위한 쿼리 비용 임계값
- `jit_optimize_above_cost`: float, 기본값 `500000` · 비용이 많이 드는 JIT 최적화를 위한 쿼리 비용 임계값

#### 예시: SSD 스토리지용 비용 조정

```ini
# SSD는 랜덤 읽기가 빠름
random_page_cost = 1.1
seq_page_cost = 1.0

# 캐시 크기 설정 (시스템 RAM의 75%)
effective_cache_size = 12GB
```

### 7.3 유전 쿼리 옵티마이저 (GEQO)

조인이 많은 복잡한 쿼리에 적용되며, 최적이 아닌 계획이 나올 수 있는 대신 계획 시간을 줄임.

- `geqo`: boolean, 기본값 `on` · 유전 쿼리 최적화 활성화/비활성화
- `geqo_threshold`: integer, 기본값 `12` · GEQO 사용 전 최소 FROM 항목 수
- `geqo_effort`: integer, 기본값 `5` · 계획 시간 대 계획 품질 트레이드오프 (1-10)
- `geqo_pool_size`: integer, 기본값 `0` · 유전 집단 크기 (0 = 자동 계산)
- `geqo_generations`: integer, 기본값 `0` · 알고리즘 반복 (0 = 자동 계산)
- `geqo_selection_bias`: float, 기본값 `2.0` · 선택 압력 (1.50-2.00)
- `geqo_seed`: float, 기본값 `0` · 난수 생성기 시드 (0-1)

### 7.4 기타 플래너 옵션

- `default_statistics_target`: integer, 기본값 `100` · 테이블 열의 기본 통계 대상
- `constraint_exclusion`: enum, 기본값 `partition` · 최적화에 테이블 제약 조건 사용 · `on`, `off`, `partition`
- `cursor_tuple_fraction`: float, 기본값 `0.1` · 검색할 커서 행의 비율
- `from_collapse_limit`: integer, 기본값 `8` · 서브쿼리 병합 전 최대 FROM 항목 수
- `jit`: boolean, 기본값 `on` · JIT 컴파일 활성화/비활성화
- `join_collapse_limit`: integer, 기본값 `8` · 명시적 JOIN 재작성 전 최대 항목 수
- `plan_cache_mode`: enum, 기본값 `auto` · 계획 캐싱 모드 · `auto`, `force_custom_plan`, `force_generic_plan`
- `recursive_worktable_factor`: float, 기본값 `10.0` · 재귀 쿼리용 추정 작업 테이블 크기 승수

#### 예시: 통계 대상 증가

```sql
-- 더 나은 계획 품질을 위해 통계 대상 증가
SET default_statistics_target = 250;

-- 특정 열에 대한 통계 수집
ALTER TABLE my_table ALTER COLUMN my_column SET STATISTICS 500;
ANALYZE my_table;
```

---

## 8. 오류 보고 및 로깅

### 8.1 로그 위치 (Where to Log)

- `log_destination`: string, 기본값 `stderr` · 로그 대상의 쉼표로 구분된 목록 · `stderr`, `csvlog`, `jsonlog`, `syslog`, `eventlog` (Windows만)
- `logging_collector`: boolean, 기본값 `off` · stderr를 캡처해 로그 파일로 리디렉션하는 백그라운드 로깅 수집기 프로세스 활성화
- `log_directory`: string, 기본값 `log` · 로그 파일 디렉터리 (절대 또는 클러스터 데이터 디렉터리 기준 상대)
- `log_filename`: string, 기본값 `postgresql-%Y-%m-%d_%H%M%S.log` · `strftime` 형식 코드를 사용한 로그 파일 명명 패턴
- `log_file_mode`: integer, 기본값 `0600` · 로그 파일의 Unix 파일 권한 (8진수 형식)
- `log_rotation_age`: integer, 기본값 `1d` · 회전 전 개별 로그 파일 사용 최대 시간
- `log_rotation_size`: integer, 기본값 `10MB` · 회전 전 개별 로그 파일 최대 크기
- `log_truncate_on_rotation`: boolean, 기본값 `off` · 추가 대신 시간 기반 회전 시 기존 로그 파일 자르기
- `syslog_facility`: enum, 기본값 `LOCAL0` · syslog 기능 · `LOCAL0`부터 `LOCAL7`까지
- `syslog_ident`: string, 기본값 `postgres` · syslog 식별용 프로그램 이름
- `syslog_sequence_numbers`: boolean, 기본값 `on` · syslog 메시지에 증가하는 시퀀스 번호 접두사
- `syslog_split_messages`: boolean, 기본값 `on` · syslog용으로 줄별로 메시지 분할 (1024바이트 제한)
- `event_source`: string, 기본값 `PostgreSQL` · Windows 이벤트 로그 식별용 프로그램 이름

#### 예시: 로깅 설정

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

### 8.2 로깅 시점 (When to Log)

- `log_min_messages`: enum, 기본값 `WARNING` · 로깅할 최소 메시지 심각도
- `log_min_error_statement`: enum, 기본값 `ERROR` · 오류를 유발하는 SQL 문 로깅을 위한 최소 심각도
- `log_min_duration_statement`: integer, 기본값 `-1` · 최소 이 시간(ms) 이상 실행되는 문 로깅 · `0`은 모든 기간 로깅, `-1`은 비활성화
- `log_min_duration_sample`: integer, 기본값 `-1` · 이 임계값을 초과하는 문 기간 샘플링 (ms)
- `log_statement_sample_rate`: float, 기본값 `1.0` · `log_min_duration_sample`을 초과하는 문 로깅 비율 (0.0-1.0)
- `log_transaction_sample_rate`: float, 기본값 `0` · 완전히 로깅할 트랜잭션 비율 (0.0-1.0)
- `log_startup_progress_interval`: integer, 기본값 `10s` · 오래 실행되는 시작 작업 로깅 간격 · `0`은 비활성화

#### 메시지 심각도 수준

- `DEBUG1..DEBUG5`: 개발자 디버깅 정보 · Syslog `DEBUG` · Event Log `INFORMATION`
- `INFO`: 사용자 요청 정보 (예: VACUUM VERBOSE) · Syslog `INFO` · Event Log `INFORMATION`
- `NOTICE`: 유용한 사용자 정보 (예: 식별자 잘림) · Syslog `NOTICE` · Event Log `INFORMATION`
- `WARNING`: 경고 (예: 트랜잭션 외부의 COMMIT) · Syslog `NOTICE` · Event Log `WARNING`
- `ERROR`: 명령 중단됨 · Syslog `WARNING` · Event Log `ERROR`
- `LOG`: 관리자 정보 (예: 체크포인트 활동) · Syslog `INFO` · Event Log `INFORMATION`
- `FATAL`: 세션 중단됨 · Syslog `ERR` · Event Log `ERROR`
- `PANIC`: 모든 세션 중단됨 · Syslog `CRIT` · Event Log `ERROR`

### 8.3 로깅 내용 (What to Log)

- `application_name`: string, 기본값 (빈 문자열) · 애플리케이션 식별자 (최대 63자) · 클라이언트가 설정
- `debug_print_parse`: boolean, 기본값 `off` · 각 쿼리의 파스 트리를 `LOG` 수준으로 출력
- `debug_print_rewritten`: boolean, 기본값 `off` · 쿼리 재작성기 출력을 `LOG` 수준으로 출력
- `debug_print_plan`: boolean, 기본값 `off` · 실행 계획을 `LOG` 수준으로 출력
- `debug_pretty_print`: boolean, 기본값 `on` · 가독성을 위해 디버그 출력 들여쓰기
- `log_autovacuum_min_duration`: integer, 기본값 `10min` · 이 시간을 초과하는 autovacuum 작업을 로깅 · `-1`은 비활성화
- `log_checkpoints`: boolean, 기본값 `on` · 통계와 함께 체크포인트 및 restartpoint 작업 로깅
- `log_connections`: string, 기본값 `''` · 연결 측면 로깅 · `receipt`, `authentication`, `authorization`, `setup_durations`, `all`
- `log_disconnections`: boolean, 기본값 `off` · 기간과 함께 세션 종료 로깅
- `log_duration`: boolean, 기본값 `off` · 완료된 모든 문의 기간 로깅
- `log_error_verbosity`: enum, 기본값 `DEFAULT` · 오류 세부 수준 · `TERSE`, `DEFAULT`, `VERBOSE`
- `log_hostname`: boolean, 기본값 `off` · 클라이언트 호스트 이름 로깅 (IP 외에) · 성능에 영향을 줄 수 있음
- `log_line_prefix`: string, 기본값 `'%m [%p] '` · 각 로그 줄의 `printf` 스타일 접두사
- `log_lock_waits`: boolean, 기본값 `off` · `deadlock_timeout`보다 오래 잠금을 대기하는 세션 로깅
- `log_recovery_conflict_waits`: boolean, 기본값 `off` · 복구 충돌을 대기하는 시작 프로세스 로깅
- `log_parameter_max_length`: integer, 기본값 `-1` (무제한) · 비오류 로그에서 바인드 파라미터를 이 바이트로 자르기 · `0`은 비활성화
- `log_parameter_max_length_on_error`: integer, 기본값 `0` (비활성화) · 오류 메시지에서 바인드 파라미터를 이 바이트로 자르기 · `-1`은 전체 허용
- `log_statement`: enum, 기본값 `none` · SQL 문 로깅 · `none`, `ddl`, `mod`, `all`
- `log_replication_commands`: boolean, 기본값 `off` · 복제 명령 및 walsender 슬롯 작업 로깅
- `log_temp_files`: integer, 기본값 `-1` · 임시 파일 로깅 · `0`은 모두 로깅, 양수는 크기 임계값(KB)
- `log_timezone`: string, 기본값 `GMT` · 서버 로그 타임스탬프용 시간대 (클러스터 전체)

#### log_line_prefix 이스케이프 시퀀스

- `%a`: 애플리케이션 이름
- `%u`: 사용자 이름
- `%d`: 데이터베이스 이름
- `%r`: 원격 호스트/IP 및 포트
- `%h`: 원격 호스트/IP
- `%b`: 백엔드 유형
- `%p`: 프로세스 ID
- `%t`: 타임스탬프 (밀리초 없음)
- `%m`: 타임스탬프 (밀리초 포함)
- `%i`: 명령 태그
- `%e`: SQLSTATE 오류 코드
- `%c`: 세션 ID (16진수 형식)
- `%l`: 세션당 로그 줄 번호
- `%s`: 프로세스 시작 타임스탬프
- `%v`: 가상 트랜잭션 ID
- `%x`: 트랜잭션 ID
- `%Q`: 쿼리 식별자
- `%%`: 리터럴 `%`

#### 예시: 로깅 설정

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

### 8.4 CSV 형식 로그 출력

CSV 로그에는 다음 열이 포함됨 (순서대로):
- timestamp, user_name, database_name, process_id, connection_from
- session_id, session_line_num, command_tag, session_start_time
- virtual_transaction_id, transaction_id, error_severity, sql_state_code
- message, detail, hint, internal_query, internal_query_pos
- context, query, query_pos, location, application_name
- backend_type, leader_pid, query_id

#### 예시: CSV 로그용 테이블 정의

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

#### 로그 파일 가져오기

```sql
COPY postgres_log FROM '/full/path/to/logfile.csv' WITH csv;
```

### 8.5 JSON 형식 로그 출력

JSON 로그 형식에는 다음 필드가 포함됨 (null 값 제외):
- timestamp, user, dbname, pid, remote_host, remote_port
- session_id, line_num, ps, session_start, vxid, txid
- error_severity, state_code, message, detail, hint
- internal_query, internal_position, context, statement, cursor_position
- func_name, file_name, file_line_num, application_name
- backend_type, leader_pid, query_id

### 8.6 프로세스 타이틀 (Process Title)

- `cluster_name`: string, 기본값 `''` · 프로세스 타이틀에 나타나는 클러스터 식별자 · 최대 63자
- `update_process_title`: boolean, 기본값 `on` (Windows: `off`) · 수신된 각 SQL 명령으로 프로세스 타이틀 업데이트

---

## 9. 런타임 통계

### 9.1 누적 쿼리 및 인덱스 통계

- `track_activities`: boolean, 기본값 `ON` · 세션당 현재 실행 중인 명령에 대한 정보 수집 활성화
- `track_activity_query_size`: integer, 기본값 `1024` · `pg_stat_activity.query` 필드에 현재 실행 중인 명령의 텍스트를 저장하기 위해 예약된 메모리
- `track_counts`: boolean, 기본값 `ON` · 데이터베이스 활동 통계 수집 활성화 · autovacuum 데몬에 필요
- `track_cost_delay_timing`: boolean, 기본값 `OFF` · 비용 기반 vacuum 지연 타이밍 활성화
- `track_io_timing`: boolean, 기본값 `OFF` · 데이터베이스 I/O 대기 타이밍 활성화 · `pg_stat_database`, `pg_stat_io`, `EXPLAIN` 출력에 표시
- `track_wal_io_timing`: boolean, 기본값 `OFF` · WAL I/O 대기 타이밍 활성화 · `pg_stat_io`에 표시
- `track_functions`: enum, 기본값 `none` · 함수 호출 횟수 및 시간 추적 활성화 · 값 `none`, `pl` (절차적 언어만), `all`
- `stats_fetch_consistency`: enum, 기본값 `cache` · 트랜잭션에서 통계에 여러 번 접근할 때의 동작 · 값 `none`, `cache`, `snapshot`

### 9.2 통계 모니터링

- `compute_query_id`: enum, 기본값 `auto` · 쿼리 식별자의 코어 내 계산 활성화 · 값 `off`, `on`, `auto`, `regress`
- `log_statement_stats`: boolean, 기본값 `OFF` · 총 문 성능 통계를 서버 로그에 출력
- `log_parser_stats`: boolean, 기본값 `OFF` · 파서 모듈 통계를 서버 로그에 출력
- `log_planner_stats`: boolean, 기본값 `OFF` · 플래너 모듈 통계를 서버 로그에 출력
- `log_executor_stats`: boolean, 기본값 `OFF` · 실행기 모듈 통계를 서버 로그에 출력

#### 예시: 통계 설정

```ini
# I/O 타이밍 활성화 (성능 오버헤드 있음)
track_io_timing = on

# 함수 추적 활성화
track_functions = all

# 쿼리 식별자 계산 (pg_stat_statements 사용 시 필요)
compute_query_id = on
```

---

## 10. Vacuum

### 10.1 자동 Vacuuming

- `autovacuum`: boolean, 기본값 `on` · autovacuum 런처 데몬 실행 여부 제어 · `track_counts` 활성화 필요
- `autovacuum_worker_slots`: integer, 기본값 `16` · autovacuum 워커용으로 예약된 백엔드 슬롯 수
- `autovacuum_max_workers`: integer, 기본값 `3` · 최대 동시 autovacuum 프로세스 수 (런처 제외)
- `autovacuum_naptime`: integer, 기본값 `1min` · 모든 데이터베이스에서 autovacuum 실행 간 최소 지연
- `autovacuum_vacuum_threshold`: integer, 기본값 `50` · VACUUM 트리거를 위한 최소 업데이트/삭제된 튜플 수
- `autovacuum_vacuum_insert_threshold`: integer, 기본값 `1000` · VACUUM 트리거에 필요한 삽입된 튜플 수 · `-1`로 비활성화
- `autovacuum_analyze_threshold`: integer, 기본값 `50` · ANALYZE 트리거를 위한 최소 삽입/업데이트/삭제된 튜플 수
- `autovacuum_vacuum_scale_factor`: float, 기본값 `0.2` · `autovacuum_vacuum_threshold`에 추가되는 테이블 크기의 비율
- `autovacuum_vacuum_insert_scale_factor`: float, 기본값 `0.2` · `autovacuum_vacuum_insert_threshold`에 추가되는 동결되지 않은 페이지의 비율
- `autovacuum_analyze_scale_factor`: float, 기본값 `0.1` · `autovacuum_analyze_threshold`에 추가되는 테이블 크기의 비율
- `autovacuum_vacuum_max_threshold`: integer, 기본값 `100000000` · VACUUM 트리거를 위한 최대 업데이트/삭제된 튜플 수 · `-1`은 제한 없음
- `autovacuum_freeze_max_age`: integer, 기본값 `200000000` · 래핑 방지를 위한 강제 VACUUM 전 최대 트랜잭션 나이
- `autovacuum_multixact_freeze_max_age`: integer, 기본값 `400000000` · 강제 VACUUM 전 최대 multixact 나이
- `autovacuum_vacuum_cost_delay`: float, 기본값 `2ms` · 자동 VACUUM의 비용 지연 · `-1`은 `vacuum_cost_delay` 사용
- `autovacuum_vacuum_cost_limit`: integer, 기본값 `-1` · 자동 VACUUM의 비용 제한 · `-1`은 `vacuum_cost_limit` 사용

#### 예시: Autovacuum 설정

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

### 10.2 비용 기반 Vacuum 지연 (Cost-Based Vacuum Delay)

- `vacuum_cost_delay`: float, 기본값 `0` · 비용 제한 초과 시 수면 시간 (ms) · `0`은 비용 기반 지연 비활성화
- `vacuum_cost_page_hit`: integer, 기본값 `1` · 공유 캐시의 버퍼 vacuum 추정 비용
- `vacuum_cost_page_miss`: integer, 기본값 `2` · 디스크에서 읽은 버퍼 vacuum 추정 비용
- `vacuum_cost_page_dirty`: integer, 기본값 `20` · VACUUM이 이전에 깨끗한 블록을 수정할 때 추정 비용
- `vacuum_cost_limit`: integer, 기본값 `200` · `vacuum_cost_delay` 수면을 트리거하는 누적 비용 임계값

### 10.3 기본 동작

- `vacuum_truncate`: boolean, 기본값 `true` · VACUUM이 테이블 끝의 빈 페이지를 자르도록 활성화 · OS에 디스크 공간 반환 · `ACCESS EXCLUSIVE` 잠금 필요

### 10.4 동결 (Freezing)

- `vacuum_freeze_table_age`: integer, 기본값 `150000000` · 공격적인 VACUUM 스캔을 트리거하는 트랜잭션 나이
- `vacuum_freeze_min_age`: integer, 기본값 `50000000` · 페이지 동결 결정을 위한 컷오프 나이
- `vacuum_failsafe_age`: integer, 기본값 `1600000000` · VACUUM 페일세이프가 트리거되는 최대 트랜잭션 나이
- `vacuum_multixact_freeze_table_age`: integer, 기본값 `150000000` · 공격적인 VACUUM 스캔을 트리거하는 multixact 나이
- `vacuum_multixact_freeze_min_age`: integer, 기본값 `5000000` · 페이지 동결을 위한 컷오프 multixact 나이
- `vacuum_multixact_failsafe_age`: integer, 기본값 `1600000000` · 페일세이프가 트리거되는 최대 multixact 나이
- `vacuum_max_eager_freeze_failure_rate`: float, 기본값 `0.03` · eager 스캔 비활성화 전 VACUUM이 동결에 실패할 수 있는 페이지의 최대 비율

동결의 목적: 충분히 오래된 행을 동결 상태로 표시 → XID 검사 없이 모든 트랜잭션에서 보이도록 함 → 트랜잭션 ID 래핑(wraparound) 장애 방지.

---

## 11. 클라이언트 연결 기본값

### 11.1 문 동작 (Statement Behavior)

- `client_min_messages`: enum, 기본값 `NOTICE` · 클라이언트에 전송되는 메시지 수준 제어
- `search_path`: string, 기본값 `"$user", public` · 객체가 단순 이름으로 참조될 때 스키마가 검색되는 순서
- `row_security`: boolean, 기본값 `on` · 오류 발생 또는 행 보안 정책 적용 제어
- `default_table_access_method`: string, 기본값 `heap` · 테이블/구체화 뷰 생성을 위한 기본 테이블 접근 방법
- `default_tablespace`: string, 기본값 (빈 문자열) · 명시적으로 지정되지 않은 경우 객체 생성을 위한 기본 테이블스페이스
- `default_toast_compression`: enum, 기본값 `pglz` · 기본 TOAST 압축 방법 · 지원 `pglz`, `lz4`
- `temp_tablespaces`: string, 기본값 (빈 문자열) · 임시 객체용 테이블스페이스
- `check_function_bodies`: boolean, 기본값 `on` · `off`로 설정하면 `CREATE FUNCTION/PROCEDURE` 실행 중 루틴 본문 문자열 유효성 검사를 비활성화
- `default_transaction_isolation`: enum, 기본값 `read committed` · 기본 격리 수준 · 값 `read uncommitted`, `read committed`, `repeatable read`, `serializable`
- `default_transaction_read_only`: boolean, 기본값 `off` · 새 트랜잭션의 기본 읽기 전용 상태
- `default_transaction_deferrable`: boolean, 기본값 `off` · 기본 지연 가능 상태 · `serializable` 읽기 전용 트랜잭션에만 영향
- `statement_timeout`: integer, 기본값 `0` · 지정된 밀리초를 초과하는 문 중단 · `0`은 비활성화
- `transaction_timeout`: integer, 기본값 `0` · 트랜잭션에서 지정된 밀리초보다 오래 걸리는 세션 종료
- `lock_timeout`: integer, 기본값 `0` · 지정된 밀리초보다 오래 잠금을 대기하는 문 중단
- `idle_in_transaction_session_timeout`: integer, 기본값 `0` · 열린 트랜잭션 내에서 지정된 밀리초보다 오래 유휴인 세션 종료
- `idle_session_timeout`: integer, 기본값 `0` · 지정된 밀리초보다 오래 유휴인 세션 (트랜잭션 내 아님) 종료
- `bytea_output`: enum, 기본값 `hex` · `bytea` 값의 출력 형식 · 값 `hex`, `escape`
- `xmlbinary`: enum, 기본값 `base64` · XML 바이너리 인코딩 방법 · 값 `base64`, `hex`
- `xmloption`: enum, 기본값 `CONTENT` · 암시적 XML 변환 모드 · 값 `DOCUMENT`, `CONTENT`
- `gin_pending_list_limit`: integer, 기본값 `4MB` · GIN 인덱스 보류 목록의 최대 크기

#### 예시: 문 동작 설정

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

### 11.2 로케일 및 형식 (Locale and Formatting)

- `DateStyle`: string, 기본값 `ISO, MDY` · 날짜/시간 값의 표시 형식 및 모호한 날짜 입력 해석
- `IntervalStyle`: enum, 기본값 `postgres` · 간격 값의 표시 형식 · 값 `sql_standard`, `postgres`, `postgres_verbose`, `iso_8601`
- `TimeZone`: string, 기본값 `GMT` · 타임스탬프 표시/해석용 시간대
- `timezone_abbreviations`: string, 기본값 `'Default'` · datetime 입력에 허용되는 시간대 약어 컬렉션
- `extra_float_digits`: integer, 기본값 `1` · 부동 소수점 값의 텍스트 출력 자릿수 조정
- `client_encoding`: string, 기본값 (데이터베이스 인코딩) · 클라이언트 측 문자 집합 인코딩
- `lc_messages`: string, 기본값 (환경에서 상속) · 표시되는 메시지의 언어
- `lc_monetary`: string, 기본값 (환경에서 상속) · 화폐 금액 형식화용 로케일
- `lc_numeric`: string, 기본값 (환경에서 상속) · 숫자 형식화용 로케일
- `lc_time`: string, 기본값 (환경에서 상속) · 날짜/시간 형식화용 로케일
- `default_text_search_config`: string, 기본값 `pg_catalog.simple` · 명시적 인수가 없는 함수용 텍스트 검색 설정

#### 예시: 로케일 설정

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

### 11.3 공유 라이브러리 사전 로딩 (Shared Library Preloading)

- `local_preload_libraries`: string, 권한 모든 사용자 · 연결 시작 시 사전 로드되는 공유 라이브러리 · `plugins` 하위 디렉터리로 제한
- `session_preload_libraries`: string, 권한 슈퍼유저만 · 연결 시작 시 사전 로드되는 공유 라이브러리 · 디버깅/성능 측정용
- `shared_preload_libraries`: string, 권한 서버 시작 시에만 · 서버 시작 시 사전 로드되는 공유 라이브러리 · 서버 재시작 필요
- `jit_provider`: string, 권한 서버 시작 시에만 · JIT 제공자 라이브러리 이름 · 기본값 `llvmjit`

#### 예시: 확장 사전 로딩

```ini
# pg_stat_statements 및 auto_explain 사전 로드
shared_preload_libraries = 'pg_stat_statements, auto_explain'
```

### 11.4 기타 기본값 (Other Defaults)

- `dynamic_library_path`: string, 기본값 `'$libdir'` · 동적으로 로드 가능한 모듈용 절대 디렉터리 경로의 콜론으로 구분된 목록
- `extension_control_path`: string, 기본값 `'$system'` · 확장 제어 파일을 검색할 경로의 콜론으로 구분된 목록
- `gin_fuzzy_search_limit`: integer, 기본값 (소프트 제한) · GIN 인덱스 스캔이 반환하는 집합 크기의 소프트 상한

---

## 12. 잠금 관리

### 잠금 관리 파라미터

- `deadlock_timeout`: integer, 기본값 `1s` · 교착 상태 조건 확인 전 잠금 대기 시간
- `max_locks_per_transaction`: integer, 기본값 `64` · 공유 잠금 테이블에서 트랜잭션당 평균 객체 잠금 수 제한
- `max_pred_locks_per_transaction`: integer, 기본값 `64` · 공유 술어 잠금 테이블에서 트랜잭션당 술어 잠금 제한
- `max_pred_locks_per_relation`: integer, 기본값 `-2` · 전체 관계 잠금으로 승격하기 전 술어 잠금할 수 있는 페이지/튜플 수 제어
- `max_pred_locks_per_page`: integer, 기본값 `2` · 전체 페이지 잠금으로 승격하기 전 단일 페이지에서 술어 잠금할 수 있는 행 수 제어

#### deadlock_timeout

- 타입: 정수
- 기본값: `1s` (1초)
- 단위: 밀리초 (단위 미지정 시)

교착 상태 확인은 비용이 크기 때문에 항상 수행하지는 않음. 이 값을 늘리면 불필요한 교착 상태 확인은 줄어들지만 교착 상태 오류 보고가 지연됨. 일반적인 트랜잭션 시간보다 크게 설정하면 교착 상태 확인 전에 잠금이 해제될 가능성이 높아짐.

참고: `log_lock_waits`가 활성화된 경우 이 파라미터는 잠금 대기 메시지 로깅 전 시간도 제어함.

#### max_locks_per_transaction

- 타입: 정수
- 기본값: `64`
- 설정 가능 시점: 서버 시작 시에만

행 잠금에 대한 제한이 아님(행 잠금은 무제한). 단일 트랜잭션에서 많은 테이블에 접근하는 쿼리가 있다면 이 값을 늘려야 함.

중요: 대기 서버에서는 기본 서버와 같거나 더 높은 값으로 설정 필요.

#### 예시: 잠금 설정

```ini
# 교착 상태 타임아웃 2초
deadlock_timeout = 2s

# 복잡한 트랜잭션을 위해 잠금 제한 증가
max_locks_per_transaction = 128
max_pred_locks_per_transaction = 128
```

---

## 참고 자료

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

---
## Chapter 20. 클라이언트 인증 (Client Authentication)

---

### 목차

- [20.1 pg_hba.conf 파일](#201-pg_hbaconf-파일)
- [20.2 사용자 이름 맵](#202-사용자-이름-맵)
- [20.3 인증 방법](#203-인증-방법)
- [20.4 Trust 인증](#204-trust-인증)
- [20.5 패스워드 인증](#205-패스워드-인증)
- [20.6 GSSAPI 인증](#206-gssapi-인증)
- [20.7 SSPI 인증](#207-sspi-인증)
- [20.8 Ident 인증](#208-ident-인증)
- [20.9 Peer 인증](#209-peer-인증)
- [20.10 LDAP 인증](#2010-ldap-인증)
- [20.11 RADIUS 인증](#2011-radius-인증)
- [20.12 인증서 인증](#2012-인증서-인증)
- [20.13 PAM 인증](#2013-pam-인증)
- [20.14 BSD 인증](#2014-bsd-인증)
- [20.15 OAuth 인증/권한 부여](#2015-oauth-인증권한-부여)
- [20.16 인증 문제 해결](#2016-인증-문제-해결)

---

### 개요

클라이언트 애플리케이션이 데이터베이스 서버에 연결할 때 연결 요청에 사용할 PostgreSQL 데이터베이스 사용자 이름을 지정 → 운영체제 로그인 시 특정 사용자로 연결을 요청하는 방식과 유사함.

인증(Authentication)은 데이터베이스 서버가 클라이언트의 신원을 확인 → 궁극적으로 해당 클라이언트 애플리케이션(또는 클라이언트 애플리케이션을 실행하는 사용자)이 요청한 사용자 이름으로 연결할 수 있는지 결정하는 프로세스임.

PostgreSQL은 클라이언트를 인증하기 위한 다양한 방법을 제공함. 특정 클라이언트 연결을 인증하는 데 사용되는 방법은 호스트 주소·데이터베이스·사용자를 기반으로 선택 가능.

PostgreSQL 데이터베이스 사용자 이름은 서버가 실행되는 운영체제의 사용자 이름과 논리적으로 분리됨. 특정 서버의 모든 사용자가 서버 머신에 계정을 갖고 있다면 운영체제 사용자 이름과 일치하는 데이터베이스 사용자 이름을 할당하는 것이 합리적. 그러나 원격 연결을 허용하는 서버는 로컬 운영체제 계정이 없는 많은 데이터베이스 사용자를 가질 수 있음 → 이 경우 데이터베이스 사용자 이름과 운영체제 사용자 이름 간의 연결은 불필요.

`LOGIN` 권한을 가진 역할(role)을 "데이터베이스 사용자"라고 지칭. 데이터베이스 객체에 대한 접근 권한은 활성 데이터베이스 사용자 이름에 의해 결정됨.

---

### 20.1 pg_hba.conf 파일

`pg_hba.conf` 파일은 PostgreSQL에서 클라이언트 인증을 제어하는 기본 설정 파일임. HBA는 "Host-Based Authentication(호스트 기반 인증)"을 의미. 이 파일은 데이터베이스 클러스터의 데이터 디렉토리에 저장되며, 서버 시작 시와 메인 서버 프로세스가 SIGHUP 신호를 수신할 때 읽힘.

#### 파일 다시 읽기

- Linux/Unix: 변경 사항을 적용하려면 다음 방법으로 postmaster에 신호 전달 필요
  - `pg_ctl reload`
  - SQL 함수 `pg_reload_conf()`
  - `kill -HUP`

- Windows: 변경 사항은 이후의 새 연결에 즉시 적용됨

#### 일반 형식

- 레코드는 한 줄에 하나씩 작성
- 빈 줄과 주석(`#`으로 시작)은 무시됨
- 백슬래시(`\`)를 사용하여 다음 줄로 레코드 계속 가능
- 필드는 공백 및/또는 탭으로 구분
- 공백을 포함하는 필드는 큰따옴표로 묶어야 함
- 특수 키워드(`all`, `replication` 등)를 따옴표로 묶으면 리터럴 데이터베이스/사용자/호스트 이름으로 처리됨

#### 레코드 형식

인증 레코드는 다음과 같은 형식을 가짐.

```
local               database  user  auth-method [auth-options]
host                database  user  address     auth-method [auth-options]
hostssl             database  user  address     auth-method [auth-options]
hostnossl           database  user  address     auth-method [auth-options]
hostgssenc          database  user  address     auth-method [auth-options]
hostnogssenc        database  user  address     auth-method [auth-options]
host                database  user  IP-address  IP-mask  auth-method [auth-options]
hostssl             database  user  IP-address  IP-mask  auth-method [auth-options]
hostnossl           database  user  IP-address  IP-mask  auth-method [auth-options]
hostgssenc          database  user  IP-address  IP-mask  auth-method [auth-options]
hostnogssenc        database  user  IP-address  IP-mask  auth-method [auth-options]
include             file
include_if_exists   file
include_dir         directory
```

#### 연결 유형 필드

- `local`: Unix 도메인 소켓 연결
- `host`: TCP/IP 연결 (SSL 또는 비-SSL, GSSAPI 암호화 여부 무관)
- `hostssl`: SSL 암호화를 사용하는 TCP/IP 연결만 해당
- `hostnossl`: SSL을 사용하지 않는 TCP/IP 연결
- `hostgssenc`: GSSAPI 암호화를 사용하는 TCP/IP 연결만 해당
- `hostnogssenc`: GSSAPI 암호화를 사용하지 않는 TCP/IP 연결

참고: 원격 TCP/IP 연결은 적절한 `listen_addresses` 설정으로 서버가 시작되어야 함 (기본값은 localhost만).

#### 데이터베이스 필드

레코드가 일치하는 데이터베이스를 지정.

- `all`: 모든 데이터베이스와 일치
- `sameuser`: 데이터베이스 이름이 사용자 이름과 같으면 일치
- `samerole`: 사용자가 데이터베이스와 같은 이름의 역할의 멤버이면 일치
- `samegroup`: `samerole`의 더 이상 사용되지 않는 표기
- `replication`: 물리적 복제 연결과 일치 (논리적 복제는 해당하지 않음)
- 특정 데이터베이스 이름
- 정규 표현식 (`/`로 시작하는 경우)
- 쉼표로 구분된 여러 값
- 외부 파일 (`@`로 시작)

정규 표현식 예시:
```
/^db\d{2,4}$
```

#### 사용자 필드

레코드가 일치하는 데이터베이스 사용자를 지정.

- `all`: 모든 사용자와 일치
- 특정 사용자 이름
- 정규 표현식 (`/`로 시작하는 경우)
- 그룹 이름 (`+`로 시작하면 해당 역할과 모든 직접/간접 멤버와 일치)
- 쉼표로 구분된 여러 값
- 외부 파일 (`@`로 시작)

#### 주소 필드

클라이언트 머신 주소를 지정.

##### IP 주소 범위 (CIDR 표기법)
```
172.20.143.89/32        # 단일 IPv4 호스트
172.20.143.0/24         # 소규모 IPv4 네트워크
10.6.0.0/16             # 대규모 IPv4 네트워크
::1/128                 # 단일 IPv6 호스트 (루프백)
fe80::7a31:c1ff:0000:0000/96  # IPv6 네트워크
0.0.0.0/0               # 모든 IPv4 주소
::0/0                   # 모든 IPv6 주소
```

- IPv4 마스크 길이: 단일 호스트의 경우 32
- IPv6 마스크 길이: 단일 호스트의 경우 128

##### 대체 넷마스크 형식
```
IP-address  IP-mask
127.0.0.1   255.255.255.255  # 단일 호스트
172.20.143.0 255.255.255.0   # 네트워크 /24
```

##### 특수 키워드
- `all`: 모든 IP 주소
- `samehost`: 서버 자체의 모든 IP 주소
- `samenet`: 직접 연결된 서브넷의 모든 주소

##### 호스트 이름
- 대소문자 구분 없이 처리
- 정방향 및 역방향 DNS 해석 수행
- `.`으로 시작하는 호스트 이름은 접미사와 일치 (예: `.example.com`은 `foo.example.com`과 일치)
- 호스트 이름 데이터베이스는 역방향 DNS 조회 결과와 일치해야 함

호스트 이름 해석 프로세스:
1. 클라이언트 IP의 역방향 DNS 조회
2. 결과 호스트 이름의 정방향 DNS 조회
3. 둘 다 클라이언트 IP와 일치해야 항목이 일치함

#### 인증 방법

- `trust`: 무조건 연결 허용 (패스워드 불필요)
- `reject`: 무조건 연결 거부
- `scram-sha-256`: SCRAM-SHA-256 패스워드 인증
- `md5`: SCRAM-SHA-256 또는 MD5 패스워드 인증 (더 이상 권장하지 않음)
- `password`: 암호화되지 않은 패스워드 (신뢰할 수 없는 네트워크에서는 비권장)
- `gss`: GSSAPI 인증 (TCP/IP만 해당)
- `sspi`: SSPI 인증 (Windows만 해당)
- `ident`: ident 서버를 통한 운영체제 사용자 이름 (TCP/IP 또는 로컬)
- `peer`: 운영체제 사용자 이름 (로컬 연결만 해당)
- `ldap`: LDAP 서버 인증
- `radius`: RADIUS 서버 인증
- `cert`: SSL 클라이언트 인증서 인증
- `pam`: 플러그형 인증 모듈
- `bsd`: BSD 인증 서비스
- `oauth`: OAuth 2.0 ID 제공자

참고: 모든 방법 이름은 소문자이며 대소문자를 구분함.

#### 인증 옵션

##### 방법별 옵션
옵션은 auth-method 필드 뒤에 `name=value` 쌍으로 지정.

##### 방법에 독립적인 옵션

###### `clientcert` (hostssl 레코드만)
사용 가능한 값:
- `verify-ca`: 클라이언트가 유효한 SSL 인증서를 제시하는지 확인
- `verify-full`: 인증서를 확인하고 CN(Common Name)이 사용자 이름과 일치하는지 확인

###### `clientname` (클라이언트 인증서 인증)
사용 가능한 값:
- `CN` (기본값): 인증서의 Common Name과 사용자 이름 일치 확인
- `DN`: 전체 Distinguished Name과 사용자 이름 일치 확인 (RFC 2253 형식)

클라이언트 인증서 DN 확인:
```bash
openssl x509 -in myclient.crt -noout -subject -nameopt RFC2253 | sed "s/^subject=//"
```

#### Include 지시문

##### `include`
해당 줄을 주어진 파일의 내용으로 대체.

##### `include_if_exists`
파일이 존재하면 파일 내용으로 대체하고, 그렇지 않으면 메시지를 로그에 기록.

##### `include_dir`
해당 줄을 `.`으로 시작하지 않고 `.conf`로 끝나는 디렉토리의 모든 파일로 대체. 파일 이름 순서대로 처리 (C 로케일: 숫자가 문자보다 먼저, 대문자가 소문자보다 먼저).

##### 외부 이름 목록 (@)
`@`로 참조되는 파일은 공백이나 쉼표로 구분된 이름을 포함.
```
@admins       # 외부 파일 참조
@demodbs      # 데이터베이스용
```

- 주석은 `#` 사용
- 중첩된 `@` 구문 허용
- 상대 경로는 참조하는 파일 기준

#### 레코드 처리

- 각 연결 시도마다 레코드가 순차적으로 검사됨
- 첫 번째 일치하는 레코드가 사용됨 (폴스루나 백업 없음)
- 일치하는 레코드에서 인증이 실패하면 후속 레코드는 무시됨
- 일치하는 레코드가 없으면 접근이 거부됨
- 순서가 중요함: 일반적으로 인증 방법이 약할수록 일치 기준을 엄격하게 설정하여 앞에 배치하고, 인증 방법이 강할수록 기준을 느슨하게 하여 뒤에 배치

#### 설정 예제

##### 기본 로컬 Trust
```
# Unix 도메인 소켓 - 모든 로컬 사용자 신뢰
local   all             all                                     trust

# TCP/IP localhost - 신뢰
host    all             all             127.0.0.1/32            trust
host    all             all             ::1/128                 trust
```

##### 패스워드 보호 원격 접근
```
# 로컬 네트워크는 패스워드 필요
host    postgres        all             192.168.12.10/32        scram-sha-256

# 도메인은 패스워드 필요
host    all             all             .example.com            scram-sha-256
```

##### 혼합 인증
```
# 구형 클라이언트 예외
host    all             mike            .example.com            md5
host    all             all             .example.com            scram-sha-256
```

##### GSSAPI 설정
```
# 특정 호스트 거부
host    all             all             192.168.54.1/32         reject

# 어디서든 GSSAPI 암호화
hostgssenc all          all             0.0.0.0/0               gss

# 특정 호스트에서 GSSAPI 비암호화
host    all             all             192.168.12.10/32        gss
```

##### 데이터베이스 및 사용자 제한
```
# 사용자는 동일한 이름의 데이터베이스만
local   sameuser        all                                     md5

# 헬프데스크 사용자는 모든 데이터베이스
local   all             /^.*helpdesk$                           md5

# 관리자 파일
local   all             @admins                                 md5

# 지원 역할 멤버
local   all             +support                                md5

# 결합
local   all             @admins,+support                        md5
```

##### 정규 표현식
```
# 데이터베이스 이름 패턴
host    "/^db\d{2,4}$"  all             localhost               trust
```

##### 맵핑과 함께 Ident 인증
```
host    all             all             192.168.0.0/16          ident map=omicron
```

#### 중요 참고 사항

##### 연결 권한
사용자는 pg_hba.conf 검사를 통과하고 데이터베이스에 대한 `CONNECT` 권한도 가져야 함. 세분화된 제어가 필요하면 pg_hba.conf보다 데이터베이스 권한 사용 권장.

##### 호스트 이름 해석 성능
- 로컬 이름 캐시 사용 (예: `nscd`)
- IP 주소 대신 호스트 이름을 로그에 기록하려면 `log_hostname` 활성화
- 접미사 일치 (`.example.com`)에는 역방향 DNS 필요

##### 슈퍼유저 멤버십
슈퍼유저는 명시적으로 할당된 경우에만 역할의 멤버로 간주되며, 슈퍼유저라는 이유만으로는 멤버가 아님.

#### 디버깅을 위한 뷰

시스템 뷰 `pg_hba_file_rules`는 변경 사항 테스트 또는 문제 진단에 도움이 됨. null이 아닌 `error` 필드가 있는 행은 해당 줄의 문제를 나타냄.

---

### 20.2 사용자 이름 맵

사용자 이름 맵을 사용하면 Ident나 GSSAPI와 같은 외부 인증 시스템을 사용할 때 운영체제 사용자 이름을 데이터베이스 사용자(역할) 이름에 매핑 가능. 연결을 시작하는 OS 사용자와 의도한 데이터베이스 사용자가 다를 때 필요.

#### 설정

##### 사용자 이름 맵 활성화
사용자 이름 매핑을 사용하려면 `pg_hba.conf`의 옵션 필드에 `map=map-name` 지정 필요. 이 옵션은 외부 사용자 이름을 받는 모든 인증 방법에서 작동함.

##### 맵 파일 위치
사용자 이름 맵은 ident 맵 파일에 정의되며, 기본적으로 `pg_ident.conf`라는 이름으로 클러스터의 데이터 디렉토리에 저장됨. 위치는 `ident_file` 설정 매개변수로 사용자 정의 가능.

#### 파일 문법

`pg_ident.conf` 파일은 다음 형식의 줄을 포함.

```
map-name       system-username       database-username
include        file
include_if_exists   file
include_dir    directory
```

필드 설명:
- `map-name`: `pg_hba.conf`에서 참조되는 임의의 식별자
- `system-username`: 운영체제 사용자 이름
- `database-username`: PostgreSQL 데이터베이스 사용자 이름

주석, 공백, 줄 계속은 `pg_hba.conf`와 동일한 규칙을 따름.

#### 다시 읽기 메커니즘

`pg_ident.conf` 파일은 다음 시점에 읽힘.
- 서버 시작 시
- 메인 서버 프로세스가 SIGHUP 신호를 수신할 때

활성 시스템에서 다시 읽기:
- `pg_ctl reload`
- SQL 함수 `pg_reload_conf()`
- `kill -HUP` 명령

#### 고급 기능

##### 특수 문자 및 키워드

`all` 키워드: 시스템 사용자가 모든 기존 데이터베이스 사용자로 연결할 수 있도록 `database-username`으로 사용.
```
map-name    system-username    all
```
`"all"`을 따옴표로 묶으면 리터럴 사용자 이름으로 처리됨.

`+` 접두사: 해당 역할에 속하는 모든 사용자로 로그인 허용.
```
map-name    system-username    +role-name
```
- `+role-name`: 지정된 역할의 직접 또는 간접 멤버인 모든 역할과 일치
- `role-name` (`+` 없이): 해당 특정 역할만 일치

특수 의미를 제거하려면 사용자 이름을 따옴표로 묶어야 함 (예: `"+admin"`).

##### 정규 표현식

시스템 사용자 이름에 정규 표현식: `system-username`이 `/`로 시작하면 POSIX 정규 표현식으로 처리됨.
```
map-name    /^(.*)@mydomain\.com$    \1
```

기능:
- 단일 캡처 그룹(괄호로 묶인 하위 표현식) 포함 가능
- `database-username`에서 `\1` (백슬래시-1)을 사용하여 캡처된 내용 참조
- 모범 사례: 전체 문자열을 일치시키려면 `^` 및 `$` 앵커 사용

데이터베이스 사용자 이름에 정규 표현식: `database-username`이 `/`로 시작하면 정규 표현식으로 처리됨.
```
map-name    system-username    /^(allowed_users)$/
```

제한 사항: 데이터베이스 사용자 이름 정규 표현식 패턴 내에서 `\1` 참조 사용 불가.

#### 실제 예제

##### 예제 20.2: 샘플 `pg_ident.conf` 파일

```
# MAPNAME       SYSTEM-USERNAME         PG-USERNAME

omicron         bryanh                  bryanh
omicron         ann                     ann
# bob는 이 머신들에서 사용자 이름이 robert
omicron         robert                  bob
# bryanh는 guest1으로도 연결 가능
omicron         bryanh                  guest1
```

동작:
- `bryanh`: `bryanh` 또는 `guest1`로 연결 가능
- `ann`: `ann`으로만 연결 가능
- `robert`: `bob`으로만 연결 가능
- 다른 모든 사용자: 접근 거부

##### 고급 정규 표현식 예제

```
mymap   /^(.*)@mydomain\.com$      \1
mymap   /^(.*)@otherdomain\.com$   guest
```

- 첫 번째 줄: `@mydomain.com` 사용자의 도메인 접미사 제거
- 두 번째 줄: 모든 `@otherdomain.com` 사용자가 `guest`로 연결하도록 허용

#### 진단 및 문제 해결

##### 시스템 뷰: `pg_ident_file_mappings`
시스템 뷰 `pg_ident_file_mappings`는 다음에 도움이 됨.
- `pg_ident.conf` 변경 사항 사전 테스트
- 로딩 문제 진단
- 설정 문제 식별 (문제를 나타내는 null이 아닌 값에 대한 `error` 열 확인)

#### 핵심 개념

1. 카디널리티 제한 없음: 하나의 OS 사용자에서 여러 데이터베이스 사용자로 매핑 가능하며, 그 반대도 가능
2. 권한 모델: 맵 항목은 "이 OS 사용자가 이 데이터베이스 사용자로 연결하도록 허용됨"을 의미 (동등성이 아님)
3. 연결 로직: 맵 항목이 외부 인증 시스템 사용자 이름과 요청된 데이터베이스 사용자 이름을 연결하면 연결이 허용됨

---

### 20.3 인증 방법

PostgreSQL은 사용자를 인증하기 위한 13가지 인증 방법을 제공. 연결 유형과 보안 인프라 가용성에 따라 적절한 방법을 선택 권장.

#### 사용 가능한 인증 방법

##### 1. Trust 인증
- 사용자가 주장하는 대로 신뢰
- 패스워드 불필요
- 자세한 내용: [20.4 Trust 인증](#204-trust-인증)

##### 2. 패스워드 인증
- 사용자가 패스워드를 보내야 함
- 대부분의 시나리오에 적합한 표준 접근 방식
- 자세한 내용: [20.5 패스워드 인증](#205-패스워드-인증)

##### 3. GSSAPI 인증
- GSSAPI 호환 보안 라이브러리 사용
- 일반적으로 인증 서버 (Kerberos, Microsoft Active Directory)에 사용
- 자세한 내용: [20.6 GSSAPI 인증](#206-gssapi-인증)

##### 4. SSPI 인증
- GSSAPI와 유사한 Windows 전용 프로토콜
- 플랫폼별 구현
- 자세한 내용: [20.7 SSPI 인증](#207-sspi-인증)

##### 5. Ident 인증
- "Identification Protocol" (RFC 1413) 서비스 사용
- 로컬 Unix 소켓 연결의 경우 peer 인증으로 처리됨
- 자세한 내용: [20.8 Ident 인증](#208-ident-인증)

##### 6. Peer 인증
- 운영체제 기능을 사용하여 연결 끝점의 프로세스 식별
- 로컬 연결만 지원 (원격은 미지원)
- 자세한 내용: [20.9 Peer 인증](#209-peer-인증)

##### 7. LDAP 인증
- LDAP 인증 서버 사용
- 자세한 내용: [20.10 LDAP 인증](#2010-ldap-인증)

##### 8. RADIUS 인증
- RADIUS 인증 서버 사용
- 자세한 내용: [20.11 RADIUS 인증](#2011-radius-인증)

##### 9. 인증서 인증
- SSL 연결 필요
- SSL 인증서 검증을 통해 사용자 인증
- 자세한 내용: [20.12 인증서 인증](#2012-인증서-인증)

##### 10. PAM 인증
- PAM (Pluggable Authentication Modules) 라이브러리 사용
- 자세한 내용: [20.13 PAM 인증](#2013-pam-인증)

##### 11. BSD 인증
- BSD Authentication 프레임워크 사용
- OpenBSD에서만 사용 가능
- 자세한 내용: [20.14 BSD 인증](#2014-bsd-인증)

##### 12. OAuth 인증/권한 부여
- 외부 OAuth 2.0 ID 제공자 사용
- 자세한 내용: [20.15 OAuth 인증/권한 부여](#2015-oauth-인증권한-부여)

#### 연결 유형별 권장 사항

- 로컬 연결: Peer 인증 (선호), Trust 인증 (충분한 경우)
- 원격 연결: 패스워드 인증 (가장 쉬운 선택)
- 기업 환경: GSSAPI, LDAP, RADIUS, 인증서, OAuth
- Windows 환경: SSPI 인증

#### 주요 고려 사항

- 외부 인프라 필요: GSSAPI, SSPI, LDAP, RADIUS, 인증서, PAM, BSD, OAuth 방법은 인증 서버나 인증 기관 필요
- 플랫폼별: SSPI (Windows), BSD 인증 (OpenBSD)
- 보안 대 단순성 트레이드오프: Trust 인증이 가장 간단하지만 보안 수준이 가장 낮음 · 다른 방법은 추가 인프라가 필요하지만 더 높은 보안을 제공

---

### 20.4 Trust 인증

Trust 인증은 서버가 연결할 수 있는 모든 사람이 지정한 모든 사용자 이름(슈퍼유저 이름 포함)으로 데이터베이스에 접근할 권한이 있다고 가정하는 PostgreSQL 인증 방법임. `pg_hba.conf`의 `database` 및 `user` 열 제한은 여전히 적용됨.

#### 주요 특성

##### 사용 시기
- 적절한 경우: 단일 사용자 워크스테이션에서의 로컬 연결
- 적절하지 않은 경우: 다중 사용자 머신에서 단독 사용

#### 보안 고려 사항

Unix 도메인 소켓 연결의 경우:
- 파일 시스템 권한을 사용하여 Unix 도메인 소켓 파일에 대한 접근을 제한하면 다중 사용자 머신에서 사용 가능
- 다음 매개변수 설정 필요
  - `unix_socket_permissions`: 소켓 파일 권한 설정
  - `unix_socket_group`: 선택적으로 소켓 그룹 소유권 설정
  - `unix_socket_directories`: 소켓을 제한된 디렉토리에 배치

TCP/IP 연결의 경우:
- 다음 경우에만 적합: `pg_hba.conf`가 연결을 허용하는 모든 머신의 모든 사용자를 신뢰하는 경우
- 보안 위험: 로컬 TCP/IP 연결은 파일 시스템 권한으로 제한되지 않음
- 모범 사례: localhost (127.0.0.1)에서의 TCP/IP 연결에만 `trust` 사용
- 중요: 로컬 보안을 위해 파일 시스템 권한에 의존하는 경우 `pg_hba.conf`에서 `host ... 127.0.0.1 ...` 줄을 `trust`에서 다른 인증 방법으로 제거하거나 변경 필요

#### 중요 경고
Trust 인증은 서버 연결에 적절한 운영체제 수준 보호가 갖춰진 경우에만 사용 필요. 추가 접근 제어 없이는 격리된 단일 사용자 환경에만 적합하며 프로덕션 다중 사용자 시스템에는 부적합.

---

### 20.5 패스워드 인증

PostgreSQL의 패스워드 기반 인증 방법은 사용자 패스워드가 서버에 저장되는 방식과 연결을 통해 패스워드가 전송되는 방식에 따라 구분됨.

#### 인증 방법

##### 1. scram-sha-256 (권장)
- 표준: RFC 7677 준수
- 유형: 챌린지-응답 스킴
- 보안 수준: 현재 사용 가능한 가장 안전한 방법
- 기능:
  - 신뢰할 수 없는 연결에서 패스워드 스니핑 방지
  - 암호화 해시된 패스워드 저장 지원
  - 패스워드가 공격에 안전한 것으로 간주됨
- 제한 사항: 구형 클라이언트 라이브러리에서 미지원

##### 2. md5 (더 이상 권장하지 않음)
- 유형: 사용자 정의 챌린지-응답 메커니즘
- 보안 수준: 덜 안전함
- 기능:
  - 패스워드 스니핑 방지
  - 서버에 평문 패스워드 저장 방지
- 제한 사항:
  - 서버에서 패스워드 해시가 도난당한 경우 보호 없음
  - MD5 알고리즘은 더 이상 결정적인 공격에 안전하지 않은 것으로 간주됨
  - MD5 방식은 더 이상 권장되지 않으며 향후 PostgreSQL 릴리스에서 제거 예정

자동 업그레이드 기능: `pg_hba.conf`에 `md5`가 지정되어 있지만 사용자의 패스워드가 SCRAM으로 암호화된 경우, SCRAM 기반 인증이 자동으로 선택됨.

##### 3. password (가장 안전하지 않음)
- 유형: 평문 전송
- 보안 수준: 패스워드 스니핑 공격에 취약
- 권장 사항: 연결이 SSL로 암호화되지 않는 한 사용 금지
- 대안: SSL 인증서 인증이 더 나을 수 있음

#### 패스워드 관리

저장 위치: `pg_authid` 시스템 카탈로그

관리 명령:
```sql
CREATE ROLE foo WITH LOGIN PASSWORD 'secret';
ALTER ROLE username PASSWORD 'new_password';
```

대안: psql의 `\password` 명령

기본 상태: 패스워드가 설정되지 않으면 저장된 패스워드는 null이고 인증은 항상 실패함.

#### 패스워드 암호화 설정

`password_encryption` 매개변수는 패스워드가 설정될 때 사용되는 암호화 방법을 제어함.

##### 암호화 및 방법 호환성

- `scram-sha-256` 암호화 방식: `scram-sha-256`, `password` (평문), `md5` (자동으로 SCRAM으로 전환) 인증 방법 사용 가능
- `md5` 암호화 방식: `md5`, `password` (평문) 인증 방법 사용 가능

#### 마이그레이션 가이드: MD5에서 SCRAM-SHA-256으로

기존 설치를 업그레이드하려면 다음 절차 필요.

1. 클라이언트 호환성 확인: 모든 클라이언트 라이브러리가 SCRAM을 지원하는지 확인
2. 설정 업데이트:
   ```
   password_encryption = 'scram-sha-256'
   ```
   (`postgresql.conf`에서)
3. 사용자 패스워드 재설정: 모든 사용자가 새 패스워드를 설정해야 함
4. 인증 방법 업데이트:
   ```
   # pg_hba.conf에서
   # 변경 전: md5
   # 변경 후: scram-sha-256
   ```

#### 중요 참고 사항

- PostgreSQL 데이터베이스 패스워드는 운영체제 사용자 패스워드와 별개임
- 이전 PostgreSQL 릴리스는 평문 패스워드 저장을 지원했으나 현재는 미지원
- 현재 저장된 패스워드 해시를 확인하려면 `pg_authid` 시스템 카탈로그 쿼리 필요

---

### 20.6 GSSAPI 인증

GSSAPI (Generic Security Service Application Program Interface)는 RFC 2743에 정의된 안전한 인증을 위한 산업 표준 프로토콜임. PostgreSQL은 다음 목적으로 GSSAPI를 지원함.
- 인증
- 통신 암호화
- 인증과 암호화 모두

주요 기능: 지원되는 시스템에서 자동 인증(싱글 사인온)을 제공함.

#### 전제 조건
- PostgreSQL 빌드 시 GSSAPI 지원이 활성화되어야 함 (설치 세부 사항은 Chapter 17 참조)
- GSSAPI 암호화 또는 SSL 암호화가 사용되면 데이터가 암호화됨 · 그렇지 않으면 암호화되지 않음

#### 서비스 주체 이름 형식

GSSAPI가 Kerberos를 사용할 때 표준 형식을 사용함.
```
servicename/hostname@realm
```

##### 주체 구성 요소:
- servicename: 일반적으로 `postgres`이지만 libpq의 `krbsrvname` 연결 매개변수로 사용자 정의 가능
- hostname: libpq가 연결하는 정규화된 호스트 이름
- realm: 서버가 접근할 수 있는 Kerberos 설정 파일에서 선호하는 realm

주체 이름은 PostgreSQL 서버에 인코딩되지 않음 → 대신 서버가 읽는 keytab 파일에 지정됨. keytab에 여러 주체가 있으면 서버는 그중 하나를 수락함.

#### 클라이언트 요구 사항

클라이언트는 다음 조건을 충족해야 함.
1. 연결하려는 서버의 주체 이름을 알아야 함
2. 유효한 티켓이 있는 자체 주체 이름을 가져야 함
3. 클라이언트 주체가 PostgreSQL 데이터베이스 사용자 이름과 연관되어야 함

##### 사용자 이름 매핑
사용자 이름 매핑은 `pg_ident.conf`를 통해 설정 가능. 예시:
- `pgusername@realm` → `pgusername`
- 또는 전체 `username@realm` 주체를 PostgreSQL 역할 이름으로 사용

중요: PostgreSQL은 realm을 제거하여 매핑하는 것도 지원하지만 이는 이전 버전과의 호환성만을 위한 것으로 강력히 비권장. 같은 이름을 가진 다른 realm의 사용자를 구별할 수 없기 때문.

#### 설정 매개변수

##### 항목별 옵션 (`pg_hba.conf`에서)

`include_realm`
- 값: 0 또는 1 (기본값: 1)
- 0으로 설정하면: 사용자 이름 매핑 전에 인증된 주체에서 realm 이름 제거
- 상태: 비권장, 주로 이전 버전과의 호환성 용도
- 보안 참고: `krb_realm`도 함께 사용하지 않으면 다중 realm 환경에서 위험
- 권장 사항: 기본값 (1)을 유지하고 `pg_ident.conf`에서 명시적 매핑 사용

`map`
- 클라이언트 주체에서 데이터베이스 사용자 이름으로 매핑 허용
- 자세한 내용은 Section 20.2 참조
- 매핑을 위한 주체 형식:
  - `username@EXAMPLE.COM` (또는 `username/hostbased@EXAMPLE.COM`)
  - `include_realm` = 0인 경우: `username` (또는 `username/hostbased`) 사용

`krb_realm`
- 사용자 주체 이름과 일치시킬 realm 설정
- 설정된 경우: 해당 realm의 사용자만 수락됨
- 설정되지 않은 경우: 모든 realm의 사용자가 연결 가능 (사용자 이름 매핑에 따라)

##### 서버 전체 옵션

`krb_caseins_users`
- 유형: 불리언
- 효과: true로 설정하면 클라이언트 주체가 사용자 맵 항목에 대소문자를 구분하지 않고 일치됨
- `krb_realm`의 대소문자를 구분하지 않는 일치에도 영향을 줌

`krb_server_keyfile`
- 서버의 keytab 파일 위치 지정
- 보안 권장 사항: PostgreSQL 서버 전용 별도 keytab 사용 (시스템 keytab 아님)
- 권한: keytab은 PostgreSQL 서버 계정만 읽을 수 있어야 함 (읽기 전용 권장)

#### Keytab 파일 생성

keytab 파일은 Kerberos 소프트웨어를 사용하여 생성됨. 문서에서는 MIT Kerberos의 `kadmin` 도구를 참조 (자세한 지침은 Kerberos 문서 참조).

#### 보안 고려 사항

1. 별도 Keytab: 시스템 keytab이 아닌 PostgreSQL 전용 keytab 사용
2. 파일 권한: keytab이 PostgreSQL 서버 계정만 읽을 수 있도록 보장
3. 다중 Realm 환경: realm 제거에 의존하지 않고 명시적 `pg_ident.conf` 매핑 사용
4. Realm 일치: 단일 realm 설치에서 추가 보안을 위해 `krb_realm` 매개변수 사용

---

### 20.7 SSPI 인증

SSPI (Security Support Provider Interface)는 PostgreSQL에서 싱글 사인온을 통한 안전한 인증을 위한 Windows 기술임. 주요 특성은 다음과 같음.

- 작동 모드: `negotiate` 모드를 사용하며 먼저 Kerberos를 시도하고 필요한 경우 자동으로 NTLM으로 폴백
- 상호 운용성: SSPI와 GSSAPI 클라이언트 및 서버가 상호 운용됨 (예: SSPI 클라이언트가 GSSAPI 서버에 인증 가능)
- 플랫폼 권장 사항: Windows 플랫폼에서는 SSPI 사용, 비-Windows 플랫폼에서는 GSSAPI 사용

#### Kerberos 인증

Kerberos 인증을 사용할 때 SSPI는 GSSAPI와 동일하게 작동함. 자세한 정보는 Section 20.6 (GSSAPI 인증) 참조.

#### 설정 옵션

##### 1. include_realm
- 기본값: 1 (활성화)
- 기능: 인증된 사용자 주체의 realm 이름을 포함할지 여부 제어
- 0으로 설정하면: 사용자 이름 매핑을 통과하기 전에 realm 이름 제거
- 보안 참고: 비활성화는 비권장이며 주로 이전 버전과의 호환성 용도 · `krb_realm`도 함께 사용하지 않으면 다중 realm 환경에서 위험
- 권장 사항: 기본값 (1)을 유지하고 `pg_ident.conf`에서 명시적 매핑 제공

##### 2. compat_realm
- 기본값: 1 (활성화)
- 기능: `include_realm` 옵션에 사용되는 realm 이름 형식 결정
  - 활성화 시: 도메인의 SAM 호환 이름 (NetBIOS 이름) 사용
  - 비활성화 (0) 시: Kerberos 사용자 주체 이름의 실제 realm 이름 사용
- 중요 경고: 다음 조건이 충족되지 않으면 비활성화 금지
  - 서버가 도메인 계정 (가상 서비스 계정 포함)으로 실행됨
  - 그리고 모든 SSPI 클라이언트도 도메인 계정 사용
  - 그렇지 않으면 인증이 실패함

##### 3. upn_username
- 기본값: 비활성화 (0)
- 기능: 인증에 사용되는 사용자 이름 형식 제어
  - `compat_realm`과 함께 활성화하면: Kerberos UPN (User Principal Name) 사용
  - 비활성화하면: SAM 호환 사용자 이름 사용
- 참고: 기본적으로 새 사용자 계정의 경우 이 두 이름은 동일함
- libpq 관련 중요 사항: libpq는 명시적 사용자 이름이 지정되지 않으면 SAM 호환 이름 사용 → 이 옵션을 비활성화된 상태로 두거나 연결 문자열에서 명시적으로 사용자 이름 지정 필요

##### 4. map
- 기능: 시스템과 데이터베이스 사용자 이름 간의 매핑 허용
- 참조: 자세한 내용은 Section 20.2 (사용자 이름 맵) 참조
- 매핑 동작:
  - `username@EXAMPLE.COM` (또는 `username/hostbased@EXAMPLE.COM`)과 같은 SSPI/Kerberos 주체의 경우 매핑된 사용자 이름은 기본적으로 전체 주체
  - `include_realm`이 0으로 설정되면 `username` (또는 `username/hostbased`)만 매핑에 사용됨

##### 5. krb_realm
- 기능: 사용자 주체 이름과 일치시킬 realm 설정
- 설정된 경우: 지정된 realm의 사용자만 수락됨
- 설정되지 않은 경우: 모든 realm의 사용자가 연결 가능 (사용자 이름 매핑 규칙에 따라)

#### 모범 사례 요약

1. `pg_ident.conf`에서 명시적 매핑과 함께 `include_realm`을 기본값 (1)으로 유지
2. 모든 클라이언트와 서버가 도메인 계정을 사용하는 경우에만 `compat_realm` 비활성화
3. libpq와 함께 연결 문자열에서 명시적 사용자 이름 사용
4. 필요시 특정 realm으로 인증을 제한하려면 `krb_realm` 사용

---

### 20.8 Ident 인증

#### Ident 인증이란?

Ident 인증은 ident 서버에서 클라이언트의 운영체제 사용자 이름을 가져와 허용된 데이터베이스 사용자 이름으로 사용하는 방법임 (선택적 사용자 이름 매핑 사용). TCP/IP 연결에서만 지원됨.

##### 중요 참고
로컬 (비-TCP/IP) 연결에 ident가 지정되면 대신 peer 인증이 사용됨 (Section 20.9 참조).

#### 설정 옵션

##### `map`
시스템과 데이터베이스 사용자 이름 간의 매핑을 허용. 자세한 내용은 Section 20.2 (사용자 이름 맵) 참조.

#### 기술 세부 사항

##### 프로토콜 기반
- RFC 1413에 설명된 Identification Protocol 기반
- 거의 모든 Unix 계열 운영체제에는 기본적으로 TCP 포트 113에서 수신하는 ident 서버 포함

##### 작동 방식
ident 서버는 다음과 같은 쿼리에 응답함: "포트 X에서 출발하여 포트 Y로 연결하는 연결을 시작한 사용자가 누구입니까?"

PostgreSQL은 물리적 연결이 설정될 때 포트 X (클라이언트)와 포트 Y (서버) 모두를 알고 있으므로 다음이 가능함.
1. 클라이언트 호스트의 ident 서버에 질의 가능
2. 이론적으로 주어진 연결에 대한 운영체제 사용자 결정 가능

#### 보안 고려 사항

##### 중요한 단점
이 방법은 전적으로 클라이언트 머신 무결성에 의존함. 신뢰할 수 없거나 손상된 클라이언트 머신에서 공격자는 다음이 가능함.
- 포트 113에서 모든 프로그램을 실행할 수 있음
- 원하는 모든 사용자 이름을 반환할 수 있음

##### 적절한 사용 사례
Ident 인증은 다음 경우에만 적합함.
- 클라이언트 머신에 대한 엄격한 제어가 있는 폐쇄형 네트워크
- 데이터베이스 및 시스템 관리자가 긴밀하게 협력하는 환경
- ident 서버를 실행하는 머신을 신뢰해야 하는 조건 충족 시

##### RFC 1413 경고
"Identification Protocol은 권한 부여 또는 접근 제어 프로토콜로 의도되지 않았음."

#### 중요 보안 경고

##### 암호화 옵션 위험
일부 ident 서버에는 원래 머신 관리자만 아는 키를 사용하여 반환된 사용자 이름을 암호화하는 비표준 옵션이 있음.

이 옵션은 PostgreSQL과 함께 사용 금지.
- PostgreSQL은 반환된 문자열을 해독할 방법이 없음
- 실제 사용자 이름을 결정할 수 없음

---

### 20.9 Peer 인증

#### 개요
Peer 인증은 커널에서 클라이언트의 운영체제 사용자 이름을 가져와 허용된 데이터베이스 사용자 이름으로 사용하는 PostgreSQL 인증 방법임.

#### 주요 특성

적용 가능성:
- 로컬 연결에서만 지원됨 (원격/TCP 연결은 해당하지 않음)
- 다음에 대한 운영체제 지원 필요
  - `getpeereid()` 함수
  - `SO_PEERCRED` 소켓 매개변수
  - 또는 유사한 메커니즘

지원되는 운영체제:
- Linux
- 대부분의 BSD 변형 (macOS 포함)
- Solaris

#### 설정 옵션

peer 인증 방법은 다음 설정 옵션을 지원함.

- `map`: 시스템과 데이터베이스 사용자 이름 간의 매핑 허용. 자세한 내용은 Section 20.2 (사용자 이름 맵) 참조

#### 작동 방식

1. 인증 방법이 커널에서 클라이언트의 운영체제 사용자 이름 검색
2. 이 OS 사용자 이름이 데이터베이스 사용자 이름과 일치됨
3. 선택적 사용자 이름 매핑을 적용하여 시스템 사용자와 데이터베이스 사용자 간 변환 가능

#### 맥락

Peer 인증은 OS 사용자 신원을 운영체제 수준에서 신뢰할 수 있게 얻을 수 있는 로컬 시스템 연결에 특히 유용함.

---

### 20.10 LDAP 인증

#### 개요

PostgreSQL의 LDAP 인증은 패스워드 인증과 유사하게 작동하지만 LDAP를 사용하여 패스워드를 확인함. 중요: LDAP 인증을 사용하려면 사용자가 PostgreSQL 데이터베이스에 미리 존재해야 함.

#### 두 가지 작동 모드

##### 1. 단순 바인드 모드
서버가 prefix + username + suffix로 구성된 DN에 바인딩함.

- 사용 사례: `cn=` 접두사 또는 `DOMAIN\` (Active Directory)
- 장점: 더 간단한 설정
- 단점: 디렉토리 구조의 유연성이 낮음

##### 2. 검색+바인드 모드
서버가 2단계 프로세스를 수행함.
1. 고정 자격 증명(또는 익명)으로 디렉토리에 바인딩
2. 사용자를 검색한 다음 클라이언트의 패스워드로 다시 바인딩

- 장점: 디렉토리에서 더 유연한 사용자 위치
- 단점: 두 개의 추가 LDAP 서버 요청

#### 설정 옵션 (두 모드 공통)

- `ldapserver`: LDAP 서버 이름/IP (공백으로 구분하여 여러 개 지정 가능)
- `ldapport`: 포트 번호 (생략 시 LDAP 라이브러리 기본값 사용)
- `ldapscheme`: SSL을 통한 LDAP의 경우 `ldaps`로 설정
- `ldaptls`: StartTLS를 사용한 TLS 암호화의 경우 `1`로 설정 (RFC 4513)

보안 참고: `ldapscheme` 또는 `ldaptls`는 PostgreSQL과 LDAP 간 트래픽만 암호화하며 PostgreSQL 클라이언트와 서버 간 트래픽은 암호화하지 않음.

#### 단순 바인드 모드 옵션

- `ldapprefix`: DN 형성을 위해 사용자 이름 앞에 추가되는 문자열
- `ldapsuffix`: DN 형성을 위해 사용자 이름 뒤에 추가되는 문자열

##### 예제: 단순 바인드

```
host ... ldap ldapserver=ldap.example.net ldapprefix="cn=" ldapsuffix=", dc=example, dc=net"
```

결과: 사용자 `someuser`는 DN: `cn=someuser, dc=example, dc=net`으로 바인딩 시도

##### 예제: 사용자 정의 포트를 사용한 LDAPS

```
host ... ldap ldapurl="ldaps://ldap.example.net:49151" ldapprefix="cn=" ldapsuffix=", dc=example, dc=net"
```

#### 검색+바인드 모드 옵션

- `ldapbasedn`: 사용자 검색을 위한 루트 DN
- `ldapbinddn`: 검색을 위해 바인딩할 사용자의 DN (익명의 경우 생략)
- `ldapbindpasswd`: 검색 바인드를 위한 패스워드
- `ldapsearchattribute`: 사용자 이름과 일치시킬 속성 (기본값: `uid`)
- `ldapsearchfilter`: `$username` 자리 표시자를 사용한 사용자 정의 검색 필터

##### 예제: 익명 검색+바인드

```
host ... ldap ldapserver=ldap.example.net ldapbasedn="dc=example, dc=net" ldapsearchattribute=uid
```

결과: `dc=example, dc=net` 아래에서 `(uid=someuser)`를 익명으로 검색한 다음 찾은 DN과 클라이언트 패스워드로 바인딩함.

##### 예제: URL로서의 검색+바인드

```
host ... ldap ldapurl="ldap://ldap.example.net/dc=example,dc=net?uid?sub"
```

##### 예제: 사용자 정의 검색 필터 (여러 속성)

```
host ... ldap ldapserver=ldap.example.net ldapbasedn="dc=example, dc=net" ldapsearchfilter="(|(uid=$username)(mail=$username))"
```

결과: `uid` 또는 `mail` 속성으로 사용자 인증함.

#### LDAP URL 형식

RFC 4516 준수 형식.

```
ldap[s]://host[:port]/basedn[?[attribute][?[scope][?[filter]]]]
```

- scope: `base`, `one`, 또는 `sub` (기본값: `base`)
- attribute: 지정된 경우 `ldapsearchattribute`로 사용됨
- filter: 속성이 비어 있으면 `ldapsearchfilter`로 사용됨
- 스킴 `ldaps`: `ldapscheme=ldaps`와 동일

참고: LDAP URL은 OpenLDAP에서만 지원됨 (Windows는 해당하지 않음).

#### 고급 기능

##### DNS SRV 검색 (OpenLDAP만)

PostgreSQL이 OpenLDAP로 컴파일되고 `ldapserver`가 생략되면 시스템은 RFC 2782 DNS SRV 조회를 수행함. `_ldap._tcp.DOMAIN`에서 DOMAIN은 `ldapbasedn`에서 추출됨.

예시:
```
host ... ldap ldapbasedn="dc=example,dc=net"
```

#### 설정 규칙

1. 모드 혼합 금지: 단순 바인드와 검색+바인드 옵션을 결합할 수 없음
2. 검색+바인드 기본값: `ldapsearchattribute`도 `ldapsearchfilter`도 지정되지 않으면 기본값은 `ldapsearchattribute=uid`
3. 등가성: `ldapsearchattribute=foo`는 `ldapsearchfilter="(foo=$username)"`과 동일
4. DN 따옴표 처리: 쉼표/공백을 포함하는 DN에는 큰따옴표 사용

#### 다른 모드와의 주요 차이점

`password` 모드와 달리 LDAP는 인증을 외부 LDAP 디렉토리에 위임하지만 여전히 데이터베이스 사용자가 PostgreSQL의 사용자 데이터베이스에 존재해야 함.

---

### 20.11 RADIUS 인증

#### 개요
PostgreSQL의 RADIUS 인증은 패스워드 인증과 유사하게 작동하지만 RADIUS 서버를 사용하여 패스워드를 확인함. RADIUS 인증을 사용하려면 사용자가 데이터베이스에 미리 존재해야 함.

#### 작동 방식

RADIUS 인증이 활성화되면 다음 절차로 처리됨.
1. Access Request 메시지 (`Authenticate Only` 유형)가 설정된 RADIUS 서버로 전송됨
2. 요청에 포함되는 내용:
   - 사용자 이름
   - 패스워드 (암호화됨)
   - NAS 식별자
3. 요청이 서버와의 공유 비밀을 사용하여 암호화됨
4. RADIUS 서버가 `Access Accept` 또는 `Access Reject`로 응답함
5. RADIUS 계정은 지원되지 않음

#### 다중 RADIUS 서버

여러 RADIUS 서버를 지정할 수 있으며 순차적으로 시도됨.
- 부정적인 응답을 받으면 → 인증 실패
- 응답이 없으면 → 목록의 다음 서버 시도
- 서버를 큰따옴표로 둘러싼 쉼표 구분 목록으로 지정
- 다른 RADIUS 옵션도 쉼표로 구분 가능 (서버별 개별 값) 또는 단일 값 (모든 서버에 적용)

#### 설정 옵션

##### `radiusservers` (필수)
- 연결할 RADIUS 서버의 DNS 이름 또는 IP 주소

##### `radiussecrets` (필수)
- RADIUS 서버와의 안전한 통신을 위한 공유 비밀
- PostgreSQL과 RADIUS 서버에서 정확히 일치해야 함
- 권장 최소 길이: 16자
- 참고: 암호화 강도는 OpenSSL 지원 여부에 따라 다름. OpenSSL 없이 빌드된 경우 전송은 난독화되지만 안전하게 보호되지는 않으므로 필요시 외부 보안 조치 적용 필요

##### `radiusports` (선택)
- RADIUS 서버에 연결할 포트 번호
- 기본값: `1812` (표준 RADIUS 포트)

##### `radiusidentifiers` (선택)
- RADIUS 요청에서 `NAS Identifier`로 사용되는 문자열
- 사용 사례: 사용자가 연결하는 데이터베이스 클러스터 식별 (RADIUS 서버 정책 일치에 유용)
- 기본값: `postgresql`

#### 설정 예제

쉼표나 공백을 포함하는 값의 경우 큰따옴표 사용 (이중 따옴표 처리 필요).

```
host ... radius radiusservers="server1,server2" radiussecrets="""secret one"",""secret two"""
```

이 예시는 공백을 포함하는 별도의 비밀을 가진 두 RADIUS 서버를 설정함.

---

### 20.12 인증서 인증

#### 개요
인증서 인증은 클라이언트 인증서를 사용하여 사용자 신원을 확인하는 SSL 기반 인증 방법임. SSL 연결에서만 사용 가능.

#### 주요 특성

- 패스워드 프롬프트 없음: 클라이언트가 유효하고 신뢰할 수 있는 인증서를 제공해야 함
- 인증 메커니즘: 인증서의 `cn` (Common Name) 속성이 요청된 데이터베이스 사용자 이름과 비교됨
- 일치 요구 사항: 인증서의 CN이 데이터베이스 사용자 이름과 일치하면 로그인 허용
- 유연성: 사용자 이름 매핑을 사용하여 CN이 데이터베이스 사용자 이름과 다를 수 있음

#### 설정 옵션

##### `map`
- 목적: 시스템과 데이터베이스 사용자 이름 간의 매핑 허용
- 참조: 자세한 설정 지침은 Section 20.2 (사용자 이름 맵) 참조

#### 중요 참고 사항

##### `clientcert` 옵션과의 관계
`cert` 인증과 함께 `clientcert` 옵션을 사용하는 것은 중복임. 이유:
- `cert` 인증은 효과적으로 `clientcert=verify-full`을 사용한 `trust` 인증과 동일함

#### 전제 조건
- 서버에서 SSL이 적절히 설정되어야 함
- 클라이언트가 유효하고 신뢰할 수 있는 인증서를 제공해야 함
- SSL 설정 지침은 Section 18.9.2 (OpenSSL 설정) 참조

---

### 20.13 PAM 인증

#### 개요

PostgreSQL의 PAM (Pluggable Authentication Modules) 인증은 패스워드 인증과 유사하게 작동하지만 PostgreSQL의 네이티브 패스워드 처리 대신 PAM을 기본 인증 메커니즘으로 사용함.

#### 주요 특성:

- 기본 PAM 서비스 이름: `postgresql`
- 목적: 사용자 이름/패스워드 쌍을 검증하고 선택적으로 연결된 원격 호스트 이름 또는 IP 주소를 검증
- 전제 조건: PAM 인증을 사용하려면 사용자가 PostgreSQL 데이터베이스에 미리 존재해야 함
- 사용자 검증: PAM은 자격 증명 검증에만 사용되며 사용자 생성에는 사용되지 않음

#### 설정 옵션

##### 1. pamservice
- 설명: PAM 서비스 이름 지정
- 기본값: `postgresql`
- 사용법: 사용할 사용자 정의 PAM 서비스 설정 지정 가능

##### 2. pam_use_hostname
- 설명: 원격 IP 주소 또는 호스트 이름을 PAM 모듈에 제공할지 결정
- 유효한 값: 0 (기본값) 또는 1
- 기본 동작 (0): IP 주소 사용
- 대안 (1): 대신 해석된 호스트 이름 사용
- 중요 참고: 호스트 이름 해석은 로그인 지연을 초래할 수 있음
- 고려 사항: 대부분의 PAM 설정은 이 정보를 사용하지 않으므로 PAM 설정이 특별히 필요로 하는 경우에만 활성화 권장

#### 중요한 제한 사항

##### Shadow 파일 접근 문제

중요 참고: PAM이 `/etc/shadow`를 읽도록 설정되면 PostgreSQL 서버가 비-root 사용자에 의해 시작되어 shadow 패스워드 파일에 접근할 수 없으므로 인증이 실패함.

해결 방법: PAM이 다음을 사용하도록 설정된 경우에는 문제없음.
- LDAP 인증
- 기타 비-shadow 인증 방법

---

### 20.14 BSD 인증

#### 개요

BSD 인증은 패스워드 인증과 유사하게 작동하지만 BSD Authentication을 사용하여 사용자 자격 증명을 확인하는 PostgreSQL의 인증 방법임. 이 인증 메커니즘은 현재 OpenBSD에서만 사용 가능.

#### 주요 특성

##### 작동 방식
- BSD Authentication 프레임워크를 통해 사용자 이름/패스워드 쌍 인증
- BSD Authentication을 사용하려면 사용자의 역할이 데이터베이스에 미리 존재해야 함
- 데이터베이스 역할을 생성하지 않음, 자격 증명만 검증

##### 로그인 클래스 설정
PostgreSQL은 `auth-postgresql` 로그인 유형을 사용하고 `login.conf`에 정의된 경우 `postgresql` 로그인 클래스로 인증을 시도함.
- `postgresql` 로그인 클래스가 `login.conf`에 정의되어 있으면 사용됨
- 정의되어 있지 않으면 PostgreSQL은 기본 로그인 클래스로 기본 설정됨

#### 전제 조건 및 설정

##### 중요 요구 사항
BSD 인증을 사용하려면 PostgreSQL 사용자 계정 (서버를 실행하는 운영체제 사용자)이 `auth` 그룹에 추가되어야 함.

- `auth` 그룹은 OpenBSD 시스템에 기본적으로 존재함
- 이 그룹의 멤버십 없이는 BSD 인증이 작동하지 않음

#### 플랫폼 가용성

- 지원되는 플랫폼: OpenBSD만 해당
- 가용성: 다른 운영체제에서는 사용 불가

#### 중요 참고 사항

1. BSD 인증은 자격 증명 검증만을 위해 설계됨
2. 데이터베이스 사용자 역할은 인증 전에 미리 생성되어야 함
3. 설정은 OpenBSD의 `login.conf` 파일을 통해 처리됨
4. PostgreSQL 서버 프로세스는 작동하기 위해 적절한 시스템 그룹 멤버십 필요

---

### 20.15 OAuth 인증/권한 부여

#### 개요

OAuth 2.0은 제3자 애플리케이션이 보호된 리소스에 대한 제한된 접근을 얻을 수 있게 하는 산업 표준 프레임워크임 (RFC 6749). PostgreSQL OAuth 클라이언트 지원은 빌드 프로세스 중에 활성화되어야 함.

#### 주요 용어

- 리소스 소유자/최종 사용자: 보호된 리소스를 소유하고 접근을 부여하는 사용자 또는 시스템. OAuth와 함께 psql을 사용할 때 사용자가 리소스 소유자
- 클라이언트: 접근 토큰을 사용하여 보호된 리소스에 접근하는 시스템 (예: psql, libpq 애플리케이션)
- 리소스 서버: 보호된 리소스를 호스팅하는 시스템. 연결하는 PostgreSQL 클러스터
- 제공자: OAuth 권한 부여 서버와 클라이언트를 개발하고 관리하는 조직 또는 엔티티
- 권한 부여 서버: 리소스 소유자가 승인을 부여한 후 클라이언트에 접근 토큰 발급. PostgreSQL은 이것을 제공하지 않음
- 발급자: OAuth 클라이언트에 대한 신뢰할 수 있는 네임스페이스를 제공하는 권한 부여 서버에 대한 HTTPS URL 식별자

#### Bearer 토큰

PostgreSQL은 bearer 토큰 (RFC 6750)을 지원함. OAuth 2.0과 함께 사용되는 불투명 문자열임. 토큰 형식은 구현에 따라 다르며 각 권한 부여 서버에 의해 선택됨.

#### 설정 옵션

##### `issuer` (필수)
- 다음 중 하나인 HTTPS URL:
  - 권한 부여 서버의 정확한 발급자 식별자 (검색 문서에서), 또는
  - 검색 문서를 직접 가리키는 잘 알려진 URI
- 검색 URL 구성:
  - 기본값: 발급자에 `/.well-known/openid-configuration` 추가
  - 발급자가 `/.well-known/` 경로 세그먼트를 포함하면 해당 URL이 그대로 사용됨

경고: 발급자 설정은 검색 문서의 발급자 식별자 및 클라이언트의 `oauth_issuer` 설정과 정확히 일치해야 함. 대소문자나 형식 변형은 허용되지 않음.

##### `scope` (필수)
- 다음에 필요한 OAuth 스코프의 공백으로 구분된 목록:
  - 클라이언트의 서버 권한 부여
  - 사용자 인증
- 값은 권한 부여 서버와 OAuth 검증 모듈에 의해 결정됨 (Chapter 50 참조)

##### `validator` (선택/조건부)
- bearer 토큰 검증을 위한 라이브러리 지정
- `oauth_validator_libraries`의 라이브러리와 정확히 일치해야 함
- `oauth_validator_libraries`에 둘 이상의 라이브러리가 포함된 경우 필수
- 그렇지 않으면 선택 사항

##### `map` (선택)
- OAuth ID 제공자와 데이터베이스 사용자 이름 간의 매핑
- 사용자 이름 매핑에 대한 자세한 내용은 Section 20.2 참조
- 지정되지 않으면 토큰의 연결된 사용자 이름이 요청된 역할 이름과 정확히 일치해야 함

##### `delegate_ident_mapping` (고급/선택)
- 표준 `pg_ident.conf` 사용자 매핑을 건너뛰려면 `1`로 설정
- OAuth 검증기가 최종 사용자 신원을 데이터베이스 역할에 매핑하는 전체 책임을 짐
- `map` 옵션과 호환되지 않음

경고: 토큰 권한을 확인하기 위해 신중한 검증기 구현이 필요하므로 주의해서 사용 필요.

#### 중요 참고 사항

- 소규모 배포의 경우 "제공자", "권한 부여 서버", "발급자"가 동의어일 수 있음
- 복잡한 설정에서는 일대다 또는 다대다 관계가 있을 수 있음
- PostgreSQL 구현은 OpenID Connect/OIDC와 상호 운용 가능하도록 의도되었지만 그 자체가 OIDC 클라이언트는 아니며 그 사용을 요구하지도 않음

---

### 20.16 인증 문제 해결

#### 개요
이 문서 섹션은 PostgreSQL 서버에 연결할 때 일반적인 인증 실패 및 관련 문제를 다룸.

#### 일반적인 인증 오류 메시지

##### 1. pg_hba.conf 항목 없음
```
FATAL:  no pg_hba.conf entry for host "123.123.123.123", user "andym", database "testdb"
```
의미: 서버에 성공적으로 접속했지만 `pg_hba.conf` 설정 파일에 일치하는 항목이 없어 연결 요청이 거부됨.

해결 방법: 호스트, 사용자, 데이터베이스 조합을 허용하도록 `pg_hba.conf`에 적절한 항목 설정 필요.

---

##### 2. 패스워드 인증 실패
```
FATAL:  password authentication failed for user "andym"
```
의미: 서버가 연결을 수락하지만 `pg_hba.conf`에 지정된 권한 부여 방법 (이 경우 패스워드 인증)을 통과해야 함.

해결 방법:
- 제공하는 패스워드가 올바른지 확인
- 오류에서 해당 인증 유형이 언급된 경우 Kerberos 또는 ident 소프트웨어 확인

---

##### 3. 사용자가 존재하지 않음
```
FATAL:  user "andym" does not exist
```
의미: 지정된 데이터베이스 사용자 이름이 시스템에서 확인되지 않음.

해결 방법: 사용자를 생성하거나 올바른 사용자 이름 확인 필요.

---

##### 4. 데이터베이스가 존재하지 않음
```
FATAL:  database "testdb" does not exist
```
의미: 연결하려는 데이터베이스가 존재하지 않음.

참고: 데이터베이스 이름을 지정하지 않으면 데이터베이스 사용자 이름이 기본값으로 사용됨.

---

#### 문제 해결 팁

서버 로그에는 클라이언트에 보고되는 것보다 인증 실패에 관한 더 많은 정보가 담겨 있을 수 있음. 실패 원인이 불분명하면 서버 로그 확인 필요.

---

### 요약

PostgreSQL의 클라이언트 인증 시스템은 매우 유연하고 다양한 환경에 적응할 수 있도록 설계됨. 주요 포인트는 다음과 같음.

1. pg_hba.conf는 누가 어떤 방법으로 연결할 수 있는지를 제어하는 핵심 설정 파일임
2. 사용자 이름 맵을 통해 외부 시스템 사용자를 데이터베이스 사용자로 매핑 가능
3. 13가지 인증 방법 중에서 환경과 요구 사항에 맞는 것을 선택 가능
4. 보안과 편의성 사이의 균형을 고려하여 적절한 인증 방법 선택 필요
5. 문제가 발생하면 서버 로그를 확인하여 자세한 오류 정보 확인 필요

---

---
## Chapter 21. 데이터베이스 역할 (Database Roles)

PostgreSQL은 역할(roles) 개념을 사용해 데이터베이스 접근 권한을 관리함. 역할은 설정 방식에 따라 데이터베이스 사용자(database user) 또는 데이터베이스 사용자 그룹(group of database users)으로 간주 가능.

역할은 데이터베이스 객체(예: 테이블, 함수)를 소유 가능하며, 다른 역할에 해당 객체에 대한 권한을 부여해 누가 어떤 객체에 접근할 수 있는지 제어 가능. 또한 역할의 멤버십을 다른 역할에 부여할 수 있어, 멤버 역할이 다른 역할에 할당된 권한을 사용 가능.

역할의 개념은 "사용자(users)"와 "그룹(groups)"의 개념을 포괄함. PostgreSQL 8.1 이전 버전에서는 사용자와 그룹이 별개의 개체 유형이었으나, 현재는 역할만 존재. 모든 역할은 사용자·그룹·둘 다로 작동 가능.

이 장에서는 역할을 생성하고 관리하는 방법을 설명함. 다양한 데이터베이스 객체에 대한 역할 권한의 효과에 대한 자세한 정보는 5.8 참고.

---

### 목차

- [21.1. 데이터베이스 역할](#211-데이터베이스-역할-database-roles)
- [21.2. 역할 속성](#212-역할-속성-role-attributes)
- [21.3. 역할 멤버십](#213-역할-멤버십-role-membership)
- [21.4. 역할 삭제](#214-역할-삭제-dropping-roles)
- [21.5. 사전 정의된 역할](#215-사전-정의된-역할-predefined-roles)
- [21.6. 함수 보안](#216-함수-보안-function-security)

---

### 21.1. 데이터베이스 역할 (Database Roles)

데이터베이스 역할은 운영 체제 사용자와 개념적으로 완전히 분리됨. 실제로는 대응 관계를 유지하는 편이 편리할 수 있으나, 필수 사항은 아님. 데이터베이스 역할은 데이터베이스 클러스터 설치 전체에서 전역적으로 적용됨(개별 데이터베이스별 적용 아님).

#### 역할 생성

역할을 생성하려면 `CREATE ROLE` SQL 명령을 사용함.

```sql
CREATE ROLE name;
```

`name`은 SQL 식별자 규칙을 따름: 특수 문자가 없는 순수한 이름이거나 큰따옴표로 둘러싸인 이름이어야 함. (실제로는 보통 `LOGIN`과 같은 추가 옵션을 명령에 추가함. 자세한 내용은 아래에서 설명.)

#### 역할 삭제

기존 역할을 제거하려면 유사한 `DROP ROLE` 명령을 사용함.

```sql
DROP ROLE name;
```

#### 셸 명령어 래퍼

편의를 위해 `createuser`와 `dropuser` 프로그램 제공. 이 프로그램들은 셸 명령줄에서 호출 가능한 SQL 명령의 래퍼임.

```bash
createuser name
dropuser name
```

#### 기존 역할 확인

기존 역할 집합을 확인하려면 `pg_roles` 시스템 카탈로그를 조회함.

```sql
SELECT rolname FROM pg_roles;
```

로그인 가능한 역할만 확인하려면 다음을 사용.

```sql
SELECT rolname FROM pg_roles WHERE rolcanlogin;
```

psql 프로그램의 `\du` 메타 명령도 기존 역할을 나열하는 데 유용함.

```
\du
```

#### 초기 슈퍼유저 역할

새로 초기화된 시스템에는 항상 하나의 사전 정의된 로그인 가능 역할이 포함됨. 이 역할은 항상 "슈퍼유저"이며, 기본적으로(특별히 지정하지 않는 한) 데이터베이스 클러스터를 초기화하기 위해 `initdb`를 실행한 운영 체제 사용자와 동일한 이름을 가짐. 일반적으로 이 역할의 이름은 `postgres`. 더 많은 역할을 생성하려면 먼저 이 초기 역할로 연결 필요.

#### 데이터베이스 연결

데이터베이스 서버에 대한 모든 연결은 특정 역할의 이름으로 이루어지며, 이 역할이 해당 연결로 시작된 세션의 초기 접근 권한을 결정함. 특정 데이터베이스 연결에 사용할 역할 이름은 연결 요청을 시작하는 클라이언트가 애플리케이션별 방식으로 나타냄. 예를 들어 `psql` 프로그램은 `-U` 명령줄 옵션을 사용해 연결할 역할을 나타냄.

```bash
psql -U rolename
```

많은 애플리케이션은 현재 운영 체제 사용자의 이름을 기본값으로 가정함(`createuser`와 `psql` 포함). 따라서 역할과 운영 체제 사용자 간에 명명 대응을 유지하는 편이 편리한 경우가 많음.

#### 보안 고려사항

주어진 클라이언트 연결이 연결할 수 있는 역할 집합은 Chapter 20에서 설명하는 클라이언트 인증 설정에 의해 결정됨. (따라서 클라이언트가 반드시 운영 체제 사용자 이름과 일치하는 역할로 연결할 필요는 없음. 마치 사람의 로그인 이름이 반드시 실제 이름과 일치할 필요가 없는 것과 같음.) 역할 신원이 연결된 클라이언트에 사용 가능한 데이터베이스 권한 집합을 결정하므로, 다중 사용자 환경에서는 권한을 신중하게 구성 필요.

---

### 21.2. 역할 속성 (Role Attributes)

데이터베이스 역할은 권한을 정의하고 클라이언트 인증 시스템과 상호 작용하는 여러 속성을 가질 수 있음.

#### 1. 로그인 권한 (Login Privilege)

`LOGIN` 속성을 가진 역할만 데이터베이스 연결의 초기 역할 이름으로 사용 가능. `LOGIN` 속성을 가진 역할은 "데이터베이스 사용자"와 동일한 것으로 간주 가능. 로그인 권한이 있는 역할을 생성하려면 다음 중 하나를 사용.

```sql
CREATE ROLE name LOGIN;
CREATE USER name;
```

(`CREATE USER`는 기본적으로 `LOGIN`을 부여한다는 점을 제외하고 `CREATE ROLE`과 동일함. 반면 `CREATE ROLE`은 기본적으로 부여하지 않음.)

#### 2. 슈퍼유저 상태 (Superuser Status)

데이터베이스 슈퍼유저는 로그인 권한 검사를 제외한 모든 권한 검사를 우회함. 위험한 권한이므로 남용 금지, 대부분의 작업은 비슈퍼유저로 수행 권장. 새 데이터베이스 슈퍼유저를 생성하려면 `CREATE ROLE name SUPERUSER`를 사용함. 이미 슈퍼유저인 역할로 실행 필요.

```sql
CREATE ROLE name SUPERUSER;
```

#### 3. 데이터베이스 생성 (Database Creation)

역할이 데이터베이스를 생성하려면 명시적인 권한 부여 필요(슈퍼유저는 어차피 모든 것을 우회하므로 제외). 그러한 역할을 생성하려면 `CREATE ROLE name CREATEDB`를 사용함.

```sql
CREATE ROLE name CREATEDB;
```

#### 4. 역할 생성 (Role Creation)

역할이 더 많은 역할을 생성하려면 명시적인 권한 부여 필요(슈퍼유저는 어차피 모든 것을 우회하므로 제외). 그러한 역할을 생성하려면 `CREATE ROLE name CREATEROLE`을 사용함.

```sql
CREATE ROLE name CREATEROLE;
```

`CREATEROLE` 권한의 제약 사항:

`CREATEROLE` 권한을 가진 역할은 다른 역할에 대해 `ALTER ROLE`도 수행 가능하고, 어떤 역할에도 `COMMENT`와 `SECURITY LABEL` 사용 가능함. 그러나 이러한 방식으로 슈퍼유저 역할을 변경하는 것은 불가능.

또한 `CREATEROLE`은 슈퍼유저가 아닌 역할에 대해 `ALTER ROLE ... SET`과 `ALTER ROLE ... RENAME`을 허용하며, `REPLICATION` 역할에 대해서도 이 두 명령 실행 가능.

그러나 `CREATEROLE`은 `REPLICATION` 역할을 생성하거나, `REPLICATION` 또는 `BYPASSRLS` 권한을 부여·취소할 수 있는 능력은 부여하지 않음.

#### 5. 복제 시작 (Initiating Replication)

역할이 스트리밍 복제를 시작하려면 명시적인 권한 부여 필요(슈퍼유저는 어차피 모든 것을 우회하므로 제외). 스트리밍 복제에 사용되는 역할은 `LOGIN` 권한도 필요. 그러한 역할을 생성하려면 `CREATE ROLE name REPLICATION LOGIN`을 사용함.

```sql
CREATE ROLE name REPLICATION LOGIN;
```

#### 6. 비밀번호 (Password)

비밀번호는 클라이언트 인증 방법이 데이터베이스 연결 시 비밀번호 입력을 요구하는 경우에만 의미 있음. `password`와 `md5` 인증 방법이 비밀번호를 사용함. 데이터베이스 비밀번호는 운영 체제 비밀번호와 별개. 역할 생성 시 비밀번호 지정 가능.

```sql
CREATE ROLE name PASSWORD 'string';
```

#### 7. 권한 상속 (Inheritance of Privileges)

역할은 기본적으로 자신이 멤버인 역할의 권한을 상속함. 상속 없이 역할을 생성하려면 `CREATE ROLE name NOINHERIT`를 사용함.

```sql
CREATE ROLE name NOINHERIT;
```

역할 멤버십 그래프의 특정 지점에서만 상속을 활성화·비활성화하려면 `GRANT` 시 `WITH INHERIT TRUE` 또는 `WITH INHERIT FALSE`를 사용함.

```sql
WITH INHERIT TRUE
WITH INHERIT FALSE
```

#### 8. 행 수준 보안 우회 (Bypassing Row-Level Security)

역할이 모든 행 수준 보안(RLS) 정책을 우회하려면 명시적인 권한 부여 필요(슈퍼유저는 어차피 모든 것을 우회하므로 제외). 그러한 역할을 생성하려면 `CREATE ROLE name BYPASSRLS`를 사용함. 슈퍼유저로 수행 필요.

```sql
CREATE ROLE name BYPASSRLS;
```

#### 9. 연결 제한 (Connection Limit)

연결 제한은 역할이 만들 수 있는 동시 연결 수를 지정 가능. -1(기본값)은 제한 없음을 의미함. 역할 생성 시 연결 제한 지정 가능.

```sql
CREATE ROLE name CONNECTION LIMIT 'integer';
```

#### 역할 속성 수정

역할의 속성은 `ALTER ROLE`로 생성 후 수정 가능. 자세한 내용은 `CREATE ROLE`과 `ALTER ROLE` 참조 페이지 참고.

역할은 Chapter 19에서 설명하는 많은 런타임 구성 설정에 대해 역할별 기본값을 가질 수도 있음. 예를 들어, 연결할 때마다 인덱스 스캔을 비활성화하려면(어떤 이유로든) 다음을 사용 가능.

```sql
ALTER ROLE myname SET enable_indexscan TO off;
```

이렇게 하면 설정이 저장됨(즉시 설정되지는 않음). 후속 연결에서 이 역할이 세션을 시작할 때마다 `SET enable_indexscan TO off`가 실행된 것처럼 됨. 세션 중에 설정 변경도 가능함. 이것은 기본값일 뿐임. 역할별 기본 설정을 제거하려면 `ALTER ROLE rolename RESET varname`을 사용함.

```sql
ALTER ROLE rolename RESET varname;
```

`LOGIN` 권한이 없는 역할에 연결된 역할별 기본값은 실제로 쓸모없음. 절대 호출되지 않기 때문.

#### CREATEROLE 사용자의 자동 권한 부여

`CREATEROLE` 권한을 가진 사용자가 새 역할을 생성하면, 자동으로 다음과 같이 해당 역할에 대한 권한이 부여됨.

```sql
GRANT created_user TO creating_user WITH ADMIN TRUE, SET FALSE, INHERIT FALSE;
```

생성하는 역할은 `ADMIN OPTION`을 통해 생성된 역할을 관리하고 다른 사용자에게 멤버십 부여 가능. 그러나 생성하는 역할은 기본적으로 생성된 역할의 권한을 상속하지 않으며(`INHERIT FALSE`), `SET ROLE`을 통해 생성된 역할에 접근 불가(`SET FALSE`). 다만 `ADMIN OPTION`이 있으므로 자신에게 역할을 재부여해 원하는 대로 `INHERIT` 및/또는 `SET` 옵션 변경 가능.

---

### 21.3. 역할 멤버십 (Role Membership)

권한 관리를 편리하게 하기 위해 사용자를 그룹화 가능. 이렇게 하면 전체 그룹에 권한을 부여·취소 가능. PostgreSQL에서는 권한 그룹을 나타내는 역할을 생성한 다음, 개별 사용자 역할에 해당 그룹 역할의 멤버십을 부여하는 방식으로 이를 구현함.

#### 그룹 역할 설정

그룹 역할을 설정하려면 먼저 역할을 생성함.

```sql
CREATE ROLE name;
```

일반적으로 그룹으로 사용되는 역할은 `LOGIN` 속성이 없으나, 원하면 설정 가능.

#### 멤버십 부여 및 취소

역할이 존재하면 `GRANT`와 `REVOKE` 명령을 사용해 멤버를 추가·제거 가능.

```sql
GRANT group_role TO role1, ... ;
REVOKE group_role FROM role1, ... ;
```

다른 그룹 역할에도 멤버십 부여 가능(역할 멤버와 역할 그룹 간에 실제 구분이 없기 때문). 데이터베이스는 순환 멤버십 루프 설정을 허용하지 않음. 또한 역할 멤버십을 `PUBLIC`에 부여하는 것은 금지.

#### 권한 사용 방식

그룹 역할의 멤버는 다음 두 가지 방식으로 해당 역할의 권한 사용 가능.

##### 방식 1: SET ROLE (명시적)

멤버십이 `SET TRUE` 옵션으로 부여된 경우, 멤버는 명시적으로 `SET ROLE`을 실행해 일시적으로 그룹 역할로 전환 가능. 이 상태에서 데이터베이스 세션은 원래 로그인 역할이 아닌 그룹 역할의 권한을 사용하며, 새로 생성되는 모든 데이터베이스 객체는 로그인 역할이 아닌 그룹 역할이 소유한 것으로 처리됨.

```sql
SET ROLE group_role;
```

##### 방식 2: INHERIT (자동 상속)

`INHERIT TRUE` 옵션으로 부여된 멤버십을 가진 역할은 `SET ROLE`을 먼저 실행하지 않아도 직접 또는 간접적으로 멤버인 역할의 권한을 자동으로 사용 가능. 예를 들어, 다음과 같이 설정했다고 가정.

```sql
CREATE ROLE joe LOGIN;
CREATE ROLE admin;
CREATE ROLE wheel;
CREATE ROLE island;

GRANT admin TO joe WITH INHERIT TRUE;
GRANT wheel TO admin WITH INHERIT FALSE;
GRANT island TO joe WITH INHERIT TRUE, SET FALSE;
```

`joe`로 연결하면 데이터베이스 세션은 `joe`에 직접 부여된 권한과 `admin`에 부여된 권한을 모두 사용 가능. `joe`가 `INHERIT TRUE` 옵션으로 `admin`의 멤버십을 부여받았기 때문. 반면 `wheel`에 부여된 권한은 사용 불가. `joe`는 `admin`을 통해 간접적으로 `wheel`의 멤버이지만, 그 멤버십이 `INHERIT FALSE`로 부여되었기 때문. `island`에 부여된 권한은 `joe`가 `INHERIT TRUE`로 해당 멤버십을 부여받았으므로 사용 가능.

joe 역할의 권한 상황:

- 초기 로그인: joe + admin + island(INHERIT로 받음) 접근 가능
- `SET ROLE admin;` 후: admin만 가능
- `SET ROLE wheel;` 후: wheel만 가능
- `SET ROLE island;`: 불가능(SET FALSE로 설정)

#### SET ROLE의 동작

`SET ROLE admin;` 후에는 `admin`에 직접 부여된 권한만 사용 가능하며, `joe`에 부여된 권한은 사용 불가. `SET ROLE wheel;` 후에는 `wheel`에 직접 부여된 권한만 사용 가능하며, `joe`나 `admin`에 부여된 권한은 사용 불가. 원래 권한 상태는 다음 중 하나로 복원 가능.

```sql
SET ROLE joe;
SET ROLE NONE;
RESET ROLE;
```

#### SET ROLE 선택 규칙

`SET ROLE` 명령으로는 로그인 역할이 직접 또는 간접적으로 멤버인 모든 역할로 전환 가능함. 단, 해당 역할로 이어지는 멤버십 연쇄 전체가 `SET TRUE`(기본값)여야 함. 따라서 위의 예에서 `joe`가 `wheel`로 전환하기 전에 먼저 `admin`이 될 필요 없이, `SET ROLE wheel`을 직접 사용 가능. 반면 `island`의 경우 `joe`에서 `island`로 이어지는 멤버십에 `SET FALSE`가 설정되어 있으므로 `SET ROLE island`는 불가능.

#### 특수 속성의 비상속

`LOGIN`, `SUPERUSER`, `CREATEDB`, `CREATEROLE` 속성은 일반 권한으로 상속되지 않는 특수 속성으로 취급됨. 이러한 속성을 가진 역할로 실제로 `SET ROLE`을 해야 해당 속성 사용 가능. 계속되는 예에서 `admin` 역할에 `CREATEDB`와 `CREATEROLE`을 부여할 수도 있음. 그러면 `joe`로 연결하는 세션은 이러한 권한을 즉시 갖지 않고, `SET ROLE admin`을 수행한 후에만 갖게 됨.

#### 그룹 역할 삭제

그룹 역할을 삭제하려면 `DROP ROLE`을 사용함.

```sql
DROP ROLE name;
```

그룹 역할에 대한 모든 멤버십은 자동으로 취소됨(멤버 역할 자체는 영향받지 않음).

#### SQL 표준과의 차이

하위 호환성을 위해 `CREATE ROLE`은 기본적으로 `INHERIT` 속성을 부여하는 반면, `GRANT` 명령에서는 기본값이 `INHERIT FALSE`임. SQL 표준에서는 기본값이 `INHERIT FALSE`. SQL 표준과 일치하는 동작을 원한다면 사용자 역할에는 `NOINHERIT` 속성을, 그룹 역할에는 `INHERIT` 속성 사용 권장.

---

### 21.4. 역할 삭제 (Dropping Roles)

데이터베이스 역할은 데이터베이스 객체를 소유할 수 있고 다른 객체에 대한 권한을 가질 수 있기 때문에, 역할을 삭제하는 것은 단순히 `DROP ROLE`을 실행하는 것만으로는 부족한 경우가 많음. 역할이 소유한 모든 객체는 먼저 삭제되거나 다른 소유자에게 이전되어야 하며, 역할에 부여된 모든 권한도 취소 필요.

#### 객체 소유권 이전

객체의 소유권은 `ALTER` 명령을 사용해 한 번에 하나씩 이전 가능.

```sql
ALTER TABLE bobs_table OWNER TO alice;
```

#### 일괄 소유권 이전 (REASSIGN OWNED)

또는 `REASSIGN OWNED` 명령을 사용해 삭제할 역할이 소유한 모든 객체의 소유권을 다른 역할 하나로 일괄 이전 가능.

```sql
REASSIGN OWNED BY doomed_role TO successor_role;
```

주의사항:
- `REASSIGN OWNED`는 다른 데이터베이스의 객체에 접근 불가하므로, 역할이 여러 데이터베이스에 객체를 소유하고 있다면 클러스터의 각 데이터베이스에서 실행 필요
- `REASSIGN OWNED`는 공유 객체(데이터베이스, 테이블스페이스)의 소유권도 변경함

#### 남은 객체 삭제 (DROP OWNED)

`REASSIGN OWNED`로 소유 객체를 처리한 후에는 `DROP OWNED`를 사용해 해당 역할이 소유한 나머지 객체를 삭제 가능.

```sql
DROP OWNED BY doomed_role;
```

이 명령은 대상 역할에 부여된 모든 권한도 함께 취소함. `REASSIGN OWNED`로 이전된 객체에 대한 권한도 포함됨. `REASSIGN OWNED`는 소유권만 이전할 뿐, 이전 소유자에게 부여된 권한은 취소하지 않기 때문.

주의사항:
- `REASSIGN OWNED`와 마찬가지로 `DROP OWNED`는 다른 데이터베이스의 객체에 접근 불가
- `DROP OWNED`는 전체 데이터베이스나 테이블스페이스를 삭제하지 않으므로, 역할이 현재 데이터베이스에 없는 데이터베이스나 테이블스페이스를 소유하고 있다면 수동 처리 필요

#### 역할 삭제

객체 소유권과 권한이 정리되면 역할 삭제 가능.

```sql
DROP ROLE doomed_role;
```

#### 완전한 삭제 절차

```sql
REASSIGN OWNED BY doomed_role TO successor_role;
DROP OWNED BY doomed_role;
-- 클러스터의 각 데이터베이스에서 위 명령 반복 실행
DROP ROLE doomed_role;
```

중요: 순서 준수 필수(REASSIGN OWNED → DROP OWNED → DROP ROLE)

#### 오류 메시지

종속 객체가 남아있는 경우 `DROP ROLE`이 깨끗하게 삭제하지 못하면 어떤 객체를 재할당하거나 삭제해야 하는지 식별하는 메시지가 발행됨.

---

### 21.5. 사전 정의된 역할 (Predefined Roles)

PostgreSQL은 자주 필요한 특권 기능 및 정보에 대한 접근을 제공하는 사전 정의된 역할 집합을 제공함. 관리자(슈퍼유저 권한을 가진 역할 포함)는 이러한 역할을 사용자에게 `GRANT`해 특정 기능에 대한 접근을 부여 가능.

```sql
GRANT pg_signal_backend TO admin_user;
```

#### 사전 정의된 역할 목록

- `pg_checkpoint`: `CHECKPOINT` 명령 실행 허용
- `pg_create_subscription`: 데이터베이스에 대한 `CREATE` 권한이 있는 사용자가 `CREATE SUBSCRIPTION` 실행 허용
- `pg_database_owner`: 별도 허용 권한 없음. 이 역할의 멤버십은 자동으로 현재 데이터베이스 소유자에게 부여됨
- `pg_execute_server_program`: 데이터베이스를 실행하는 사용자로 `COPY` 및 기타 서버 측 프로그램 실행 함수를 사용해 서버의 프로그램 실행 허용
- `pg_maintain`: 모든 관계에서 `VACUUM`, `ANALYZE`, `CLUSTER`, `REFRESH MATERIALIZED VIEW`, `REINDEX`, `LOCK TABLE` 실행 허용
- `pg_monitor`: 다양한 모니터링 뷰와 함수를 읽고·실행함
  - `pg_read_all_settings`, `pg_read_all_stats`, `pg_stat_scan_tables`의 멤버임
- `pg_read_all_data`: 모든 테이블, 뷰, 시퀀스에서 `SELECT` 권한이 있고 모든 스키마에서 `USAGE` 권한이 있는 것처럼 모든 데이터(테이블, 뷰, 시퀀스) 읽기 허용
- `pg_read_all_settings`: 슈퍼유저에게만 표시되도록 제한된 구성 변수를 포함한 모든 구성 변수 읽기 허용
- `pg_read_all_stats`: 슈퍼유저에게만 표시되도록 제한된 정보를 포함한 모든 `pg_stat_*` 뷰 읽기 허용 및 슈퍼유저만 볼 수 있는 통계 관련 확장 사용 허용
- `pg_read_server_files`: 데이터베이스가 서버에서 접근 가능한 모든 위치에서 `COPY` 및 기타 파일 접근 함수를 사용해 파일 읽기 허용
- `pg_signal_autovacuum_worker`: autovacuum 워커에 신호 전송 허용(현재 테이블에서 vacuum 취소 또는 종료)
- `pg_signal_backend`: 다른 백엔드에 신호 전송 허용(예: 쿼리 취소 또는 세션 종료)
- `pg_stat_scan_tables`: `ACCESS SHARE` 잠금을 필요로 하는 모니터링 함수 실행 허용(예: `pgrowlocks(text)`)
- `pg_use_reserved_connections`: `reserved_connections`를 통해 예약된 연결 슬롯 사용 허용
- `pg_write_all_data`: 모든 테이블, 뷰, 시퀀스에서 `INSERT`, `UPDATE`, `DELETE` 권한이 있고 모든 스키마에서 `USAGE` 권한이 있는 것처럼 모든 데이터(테이블, 뷰, 시퀀스) 쓰기 허용
- `pg_write_server_files`: 데이터베이스가 서버에서 접근 가능한 모든 위치에서 `COPY` 및 기타 파일 접근 함수를 사용해 파일 쓰기 허용

#### 상세 설명

##### pg_database_owner

`pg_database_owner` 역할은 현재 데이터베이스 소유자를 암묵적 멤버로 가지며, 따라서 이 역할에 부여된 권한을 가짐. `pg_database_owner`는 다른 역할의 멤버가 될 수 없으며, 다른 역할을 이 역할의 멤버로 추가하는 것도 불가. 다른 역할과 마찬가지로 이 역할이 객체를 소유하거나 기본 접근 권한의 대상이 될 수 있음.

특히 `pg_database_owner` 역할은 `public` 스키마를 소유함. 이를 통해 데이터베이스 소유자가 해당 데이터베이스의 로컬 사용을 제어함. 기본적으로 모든 역할은 이 역할을 통해 `public` 스키마에 객체 생성 가능.

##### pg_read_all_data 및 pg_write_all_data

`pg_read_all_data`와 `pg_write_all_data` 역할은 각각 데이터에 대한 읽기 또는 쓰기 권한만 제공하며, 행 수준 보안(RLS) 정책을 우회하지 않음. 따라서 RLS가 활성화된 경우 해당 정책이 여전히 접근을 제한함.

##### pg_signal_backend

`pg_signal_backend` 역할은 신뢰할 수 있는 비슈퍼유저가 다른 백엔드에 신호를 보낼 수 있도록 함. 다른 세션의 쿼리를 취소하거나 세션을 종료하는 신호 전송 가능. 단, 슈퍼유저가 소유한 백엔드에는 신호 전송 불가.

#### 서버 파일 접근 역할의 보안 주의사항

`pg_read_server_files`, `pg_write_server_files`, `pg_execute_server_program` 역할은 데이터베이스를 실행하는 운영 체제 사용자 권한으로 서버 파일에 접근하거나 서버에서 프로그램을 실행할 수 있게 함. 이 역할들은 모든 데이터베이스 수준 권한 검사를 우회하므로, 부여 시 운영 체제 파일 시스템 접근 권한도 사실상 함께 부여된다는 점 고려 필요. 파일 시스템 접근이 제한되어 있더라도, 사용자는 서버에서 클라이언트로 전송되는 데이터에 민감한 정보를 포함시킬 수 있음.

경고: 이러한 역할을 부여할 때는 매우 신중한 판단 필요.

#### 버전 변경 사항

향후 버전에서 특권 기능이 추가되면 이러한 역할의 권한이 변경될 수 있음. 관리자는 릴리스 노트에서 변경 사항 확인 필요.

---

### 21.6. 함수 보안 (Function Security)

함수, 트리거, 행 수준 보안 정책은 사용자가 자신도 모르게 다른 사용자가 삽입한 코드를 실행하게 만들 수 있음. 이러한 메커니즘은 "트로이 목마" 유형의 공격, 즉 공격자가 백엔드 서버에 코드를 삽입해 다른 사용자가 의도치 않게 실행하도록 유도하는 공격에 노출될 수 있음.

#### 보안 대책

가장 강력한 보호는 객체를 정의할 수 있는 사람을 엄격하게 통제하는 것임. 이것이 불가능한 경우, 신뢰할 수 있는 소유자가 정의한 객체만 참조하는 쿼리 작성 권장. `search_path`에서 신뢰할 수 없는 사용자가 객체를 생성할 수 있는 스키마 제거 권장.

- 엄격한 접근 제어: 객체를 정의할 수 있는 사람에 대한 통제
- 신뢰할 수 있는 소유자의 객체만 참조: 신뢰할 수 있는 소유자가 정의한 객체만 사용
- search_path 관리: 신뢰할 수 없는 사용자가 객체를 생성할 수 있는 스키마를 `search_path`에서 제거

#### 함수 실행 환경

함수는 데이터베이스 서버 데몬의 운영 체제 권한으로 백엔드 서버 프로세스 내에서 실행됨. 함수에 사용되는 프로그래밍 언어가 비검증 메모리 접근을 허용하는 경우, 서버의 내부 데이터 구조 변경 가능.

#### 신뢰할 수 없는 언어

결과적으로 이러한 함수는 시스템 접근 제어를 우회할 수 있음. 이러한 접근을 허용하는 함수 언어는 "신뢰할 수 없음(untrusted)"으로 간주되며, PostgreSQL은 슈퍼유저만 해당 언어로 작성된 함수를 생성할 수 있도록 제한함.

주요 사항:
- 메모리 접근 제어가 없는 언어: 서버의 내부 데이터 구조 변경 가능
- 시스템 접근 제어 우회: 이론적으로 모든 시스템 접근 제어 우회 가능
- 제한 사항: PostgreSQL에서는 이러한 언어로 작성된 함수는 슈퍼유저만 생성 가능

---

### 참고 자료

- [PostgreSQL 18 공식 문서 - Chapter 21. Database Roles](https://www.postgresql.org/docs/18/user-manag.html)
- [CREATE ROLE](https://www.postgresql.org/docs/18/sql-createrole.html)
- [ALTER ROLE](https://www.postgresql.org/docs/18/sql-alterrole.html)
- [DROP ROLE](https://www.postgresql.org/docs/18/sql-droprole.html)
- [GRANT](https://www.postgresql.org/docs/18/sql-grant.html)
- [REVOKE](https://www.postgresql.org/docs/18/sql-revoke.html)

---

## Chapter 22. 데이터베이스 관리 (Managing Databases)

PostgreSQL 서버는 하나 이상의 데이터베이스를 관리 가능. 일반적으로 각 프로젝트나 각 사용자에 대해 별도의 데이터베이스 사용.

목차
- [22.1. 개요](#221-개요)
- [22.2. 데이터베이스 생성](#222-데이터베이스-생성)
- [22.3. 템플릿 데이터베이스](#223-템플릿-데이터베이스)
- [22.4. 데이터베이스 구성](#224-데이터베이스-구성)
- [22.5. 데이터베이스 삭제](#225-데이터베이스-삭제)
- [22.6. 테이블스페이스](#226-테이블스페이스)

---

### 22.1. 개요

데이터베이스 객체의 작은 집합인 역할(role), 데이터베이스, 테이블스페이스는 클러스터 수준에서 정의되며 `pg_global` 테이블스페이스에 저장됨. 클러스터 내부에는 여러 데이터베이스가 존재하며, 이들은 서로 격리되어 있지만 클러스터 수준 객체에는 접근 가능. 각 데이터베이스 내부에는 여러 스키마가 있으며, 스키마에는 테이블이나 함수 같은 객체가 포함됨. 따라서 전체 계층 구조는 클러스터 → 데이터베이스 → 스키마 → 테이블(또는 함수 같은 다른 종류의 객체) 순서.

서버에 연결할 때, 연결 요청은 클러스터 내의 정확히 하나의 데이터베이스를 지정해야 함. 연결당 여러 데이터베이스에 접근하는 것은 금지. 그러나 애플리케이션이 동일한 데이터베이스 또는 다른 데이터베이스에 대해 동시에 여러 연결을 여는 것을 막는 제한은 없음. 데이터베이스는 물리적으로 분리되어 있으며 접근 제어는 연결 수준에서 관리됨. 하나의 클러스터 내에 여러 데이터베이스가 있고 단일 연결로 여러 데이터베이스의 데이터에 동시 접근이 필요한 경우, PostgreSQL의 Foreign Data Wrapper 기능(`postgres_fdw`) 사용 가능. 자세한 내용은 [postgres_fdw](https://www.postgresql.org/docs/18/postgres-fdw.html) 참고. 다른 접근법으로 `dblink` 모듈 사용도 가능. 자세한 내용은 [dblink](https://www.postgresql.org/docs/18/dblink.html) 참고. `postgres_fdw`는 다른 클러스터의 데이터베이스에서 객체에 대한 프록시를 생성하는 데도 사용 가능.

만약 동일한 PostgreSQL 서버 인스턴스가 서로 독립적인 프로젝트나 사용자를 호스팅하는 경우, 이들을 별도의 데이터베이스에 넣고 그에 맞게 접근 제어 및 권한 부여를 구성하는 것이 권장됨. 프로젝트나 사용자들이 상호 관련되어 있고 서로의 리소스를 사용해야 하는 경우, 동일한 데이터베이스에 넣되 네임스페이스 격리 및 권한 부여를 통한 모듈화 구조를 위해 별도의 스키마에 넣을 수 있음.

클러스터 내에서 여러 데이터베이스를 사용하면 공유 WAL(Write-Ahead Log)로 인해 백업 및 복구에 대한 유연성이 감소함. 클러스터에 포함된 여러 데이터베이스가 사용자 관점에서는 격리되어 있어도 관리자 관점에서는 밀접하게 결합되어 있음.

데이터베이스는 `CREATE DATABASE` 명령(22.2 참고)으로 생성되고 `DROP DATABASE` 명령(22.5 참고)으로 삭제됨. 기존 데이터베이스 집합을 확인하려면 다음과 같이 `pg_database` 시스템 카탈로그를 조회함.

```sql
SELECT datname FROM pg_database;
```

`psql` 프로그램의 `\l` 메타 명령과 `-l` 명령줄 옵션도 기존 데이터베이스를 나열하는 데 유용함.

> 참고: SQL 표준에서는 데이터베이스를 "카탈로그(catalog)"라고 부르나, 실질적인 차이는 없음.

---

### 22.2. 데이터베이스 생성

데이터베이스를 생성하려면 PostgreSQL 서버가 실행 중이어야 함(18.3 참고).

데이터베이스는 SQL 명령 `CREATE DATABASE`로 생성됨.

```sql
CREATE DATABASE name;
```

여기서 `name`은 SQL 식별자의 일반적인 규칙을 따름. 현재 역할(role)이 자동으로 새 데이터베이스의 소유자가 됨. 나중에 데이터베이스를 삭제하는 것은 소유자의 특권임(이는 데이터베이스에 포함된 모든 객체도 삭제하며, 소유자가 아닌 경우에도 삭제됨).

데이터베이스 생성은 제한된 작업임. 권한을 부여하는 방법은 21.2 참고.

데이터베이스를 생성하려면 데이터베이스 서버에 연결되어 있어야 하므로, 주어진 클러스터에서 첫 번째 데이터베이스를 어떻게 만들 수 있는지에 대한 의문이 생길 수 있음. 첫 번째 데이터베이스는 항상 `initdb` 명령으로 데이터 저장 영역을 초기화할 때 생성됨(18.2 참고). 이 데이터베이스는 `postgres`라고 함. 따라서 첫 번째 "일반" 데이터베이스를 생성하려면 `postgres`에 연결하면 됨.

데이터베이스 클러스터 초기화 중에 `template1`(22.3 참고)과 `template0`이라는 두 개의 추가 데이터베이스도 생성됨. 클러스터 내에서 새 데이터베이스가 생성될 때마다 `template1`이 본질적으로 복제됨. 이는 `template1`에서 수행한 모든 수정 사항이 이후에 생성되는 모든 데이터베이스에 나타난다는 의미임. 이 때문에 `template1`에 객체를 추가하는 것은 적절한 고려 없이 수행 금지. `template0`은 `template1`의 원시 복사본으로, 사용자가 추가한 어떠한 사이트 지역 사항도 포함하지 않는 순수한 데이터베이스가 필요할 때 복제 가능. 자세한 내용은 22.3 참고.

편의상, 셸에서 실행할 수 있는 `createdb` 프로그램도 있음.

```bash
createdb dbname
```

`createdb`는 마법이 아님. `postgres` 데이터베이스에 연결하고 `CREATE DATABASE` 명령을 실행함(위에서 설명한 것과 정확히 같음). [createdb](https://www.postgresql.org/docs/18/app-createdb.html) 참조 페이지에 호출 세부 정보 포함. 인자 없이 호출된 `createdb`는 현재 사용자 이름으로 데이터베이스를 생성함.

> 팁: Chapter 20에서는 특정 데이터베이스에 연결할 수 있는 사람을 제한하는 방법에 대한 정보 제공.

때때로 다른 사람을 위해 데이터베이스를 생성하고 그 새 데이터베이스의 소유자로 만들어 스스로 구성하고 관리할 수 있도록 하고 싶을 때가 있음. 이를 수행하려면 SQL 환경에서 다음 명령 중 하나를 사용함.

```sql
CREATE DATABASE dbname OWNER rolename;
```

또는 셸에서 다음을 사용.

```bash
createdb -O rolename dbname
```

슈퍼유저만 자신이 멤버가 아닌 역할을 위해 데이터베이스 생성 가능.

---

### 22.3. 템플릿 데이터베이스

`CREATE DATABASE`는 실제로 기존 데이터베이스를 복사하여 작동함. 기본적으로 `template1`이라는 표준 시스템 데이터베이스를 복사함. 따라서 해당 데이터베이스는 새 데이터베이스를 만드는 "템플릿"임. `template1`에 객체를 추가하면 이러한 객체는 이후에 생성되는 사용자 데이터베이스로 복사됨. 이 동작은 데이터베이스에 대한 표준 객체 집합의 사이트 지역 수정을 허용함. 예를 들어, `template1`에 절차적 언어 PL/Perl을 설치하면 추가 조치 없이 사용자 데이터베이스에서 자동으로 사용 가능해짐.

그러나 `template1`에서 데이터베이스 수준의 `GRANT` 권한은 새 데이터베이스에 복사되지 않음. 새 데이터베이스는 해당 데이터베이스 수준 권한에 대한 기본 설정을 가짐.

클러스터 초기화 중에 `template0`이라는 두 번째 표준 시스템 데이터베이스가 생성됨. 이 데이터베이스는 `template1`의 초기 내용과 동일한 데이터를 포함하며, 즉 PostgreSQL 버전에서 미리 정의한 표준 객체만 포함함. `template0`은 데이터베이스 클러스터 초기화 후에 변경 금지. `CREATE DATABASE`가 `template1` 대신 `template0`을 복사하도록 지시함으로써, `template1`에 추가된 사이트 지역 추가 사항이 포함되지 않은 "순수한" 사용자 데이터베이스(어떤 사용자 정의 객체도 존재하지 않고 시스템 객체가 변경되지 않은 상태)를 생성할 수 있음.

`template0`을 템플릿으로 사용해야 하는 또 다른 일반적인 이유는 `template0`을 복사할 때 `template1`과 달리 새로운 인코딩 및 로케일 설정을 지정할 수 있기 때문. `template0`에는 인코딩 또는 로케일에 종속되는 데이터가 포함되지 않은 것으로 알려져 있기 때문.

`template0`을 템플릿 데이터베이스로 사용해 데이터베이스를 생성하려면 다음을 사용함.

```sql
CREATE DATABASE dbname TEMPLATE template0;
```

셸에서는 다음과 같음.

```bash
createdb -T template0 dbname
```

이름을 지정해 다른 데이터베이스를 템플릿으로 복사하는 추가적인 데이터베이스 템플릿 생성 가능. 그러나 이 기능은 (아직) 범용 "COPY DATABASE" 기능으로 설계된 것이 아님에 주의 필요. 주요 제한 사항은 소스 데이터베이스가 복사되는 동안 다른 세션이 연결되어 있으면 안 된다는 것. 시작 시점에 다른 연결이 존재하면 `CREATE DATABASE`는 실패함.

`pg_database`의 각 데이터베이스에 대해 두 가지 유용한 플래그가 있음: `datistemplate` 및 `datallowconn` 열. `datistemplate`은 데이터베이스가 `CREATE DATABASE`의 템플릿으로 의도되었음을 나타내기 위해 설정 가능. 이 플래그가 설정되면 `CREATEDB` 권한이 있는 모든 사용자가 데이터베이스 복제 가능. 설정되지 않은 경우 슈퍼유저와 데이터베이스 소유자만 복제 가능. `datallowconn`이 false이면 데이터베이스에 대한 새 연결이 허용되지 않음(그러나 단순히 플래그를 false로 설정해도 기존 세션은 종료되지 않음). `template0` 데이터베이스는 일반적으로 수정을 방지하기 위해 `datallowconn = false`로 표시됨. `template0`과 `template1` 모두 항상 `datistemplate = true`로 표시 필요.

> 참고: `template1`과 `template0`에는 특별한 하드코딩된 상태가 없으며, 이름만 다름. 예를 들어, `template1`을 삭제하고 `template0`에서 다시 생성 가능. 신중하지 않게 `template1`에 쓰레기를 추가한 경우 이 작업 과정이 권장될 수 있음. (`template1`을 삭제하려면 `pg_database.datistemplate = false`가 있어야 함.)

`postgres` 데이터베이스도 데이터베이스 클러스터 초기화 중에 생성됨. 이 데이터베이스는 사용자와 애플리케이션이 연결하기 위한 기본 데이터베이스로 의도됨. 이것은 단순히 `template1`의 복사본이며 필요한 경우 삭제하고 다시 생성 가능.

---

### 22.4. 데이터베이스 구성

Chapter 19에서 설명한 것처럼 PostgreSQL 서버는 런타임 구성 매개변수를 상당수 제공함. 이러한 설정 중 많은 것에 대해 데이터베이스별 기본값 설정 가능.

예를 들어, 어떤 이유로 특정 데이터베이스에 대해 GEQO 옵티마이저를 비활성화하려면 일반적으로 모든 데이터베이스에 대해 비활성화하거나 모든 연결 클라이언트가 `SET geqo TO off;`를 실행하도록 해야 함. 해당 데이터베이스 내에서 이 설정을 기본값으로 만들려면 다음 명령을 실행 가능.

```sql
ALTER DATABASE mydb SET geqo TO off;
```

이렇게 하면 설정이 저장됨(그러나 즉시 설정되지는 않음). 이후 이 데이터베이스에 대한 연결에서 세션이 시작되기 직전에 `SET geqo TO off;`가 실행된 것처럼 나타남. 이것은 여전히 기본값일 뿐이므로 세션 내에서 설정 변경 가능. 이 설정을 실행 취소하려면 `ALTER DATABASE dbname RESET varname`을 사용함.

데이터베이스별 기본 설정을 취소하려면 다음을 사용함.

```sql
ALTER DATABASE dbname RESET varname;
```

---

### 22.5. 데이터베이스 삭제

데이터베이스는 `DROP DATABASE` 명령으로 삭제됨.

```sql
DROP DATABASE name;
```

데이터베이스의 소유자 또는 슈퍼유저만 데이터베이스 삭제 가능. 데이터베이스를 삭제하면 데이터베이스에 포함된 모든 객체가 제거됨. 데이터베이스 삭제는 취소 불가.

삭제할 데이터베이스에 연결되어 있는 동안에는 `DROP DATABASE` 실행 불가. 그러나 클러스터의 다른 데이터베이스를 포함한 다른 데이터베이스에는 연결 가능. `template1`은 클러스터의 마지막 사용자 데이터베이스를 삭제하는 경우 유일한 옵션이 됨.

편의상, 셸에서 데이터베이스를 삭제하기 위한 셸 프로그램 `dropdb`도 있음.

```bash
dropdb dbname
```

(`createdb`와 달리 현재 사용자 이름의 데이터베이스를 삭제하는 것은 기본 동작 아님.)

---

### 22.6. 테이블스페이스

PostgreSQL의 테이블스페이스를 사용하면 데이터베이스 관리자가 데이터베이스 객체 파일이 저장될 파일 시스템 위치를 정의 가능. 한 번 생성되면 테이블스페이스는 데이터베이스 객체를 생성할 때 이름으로 참조 가능.

테이블스페이스를 사용하면 관리자는 PostgreSQL 설치의 디스크 레이아웃을 제어 가능. 이는 적어도 두 가지 이유로 유용함. 첫째, 클러스터가 초기화된 파티션 또는 볼륨의 공간이 부족하고 확장할 수 없는 경우, 다른 파티션에 테이블스페이스를 생성하고 시스템이 재구성될 때까지 사용 가능. 둘째, 테이블스페이스를 사용하면 관리자가 데이터베이스 객체의 사용 패턴에 대한 지식을 사용해 성능 최적화 가능. 예를 들어, 매우 많이 사용되는 인덱스는 매우 빠르고 가용성이 높은 SSD와 같은 솔리드 스테이트 드라이브에 배치 가능. 동시에 거의 사용되지 않거나 성능이 중요하지 않은 아카이브 데이터를 저장하는 테이블은 더 저렴하고 느린 디스크 시스템에 저장 가능.

> 경고: 테이블스페이스가 메인 PostgreSQL 데이터 디렉토리 외부에 있더라도 데이터베이스 클러스터의 필수적인 부분이며 데이터 파일의 자율적인 컬렉션으로 취급 불가. 테이블스페이스는 메인 데이터 디렉토리에 포함된 메타데이터에 종속되므로 다른 데이터베이스 클러스터에 연결하거나 개별적으로 백업 불가. 마찬가지로 테이블스페이스를 잃으면(파일 삭제, 디스크 장애 등) 데이터베이스 클러스터가 읽을 수 없거나 시작할 수 없게 될 수 있음. 램 디스크와 같은 임시 파일 시스템에 테이블스페이스를 배치하면 전체 클러스터의 신뢰성이 위험에 처할 수 있음.

테이블스페이스를 정의하려면 `CREATE TABLESPACE` 명령을 사용함. 예:

```sql
CREATE TABLESPACE fastspace LOCATION '/ssd1/postgresql/data';
```

위치는 빈 기존 디렉토리여야 하며 PostgreSQL 운영 체제 사용자가 소유해야 함. 이후 테이블스페이스 내에서 생성된 모든 객체는 이 디렉토리 아래의 파일에 저장됨. 위치는 제거 가능하거나 일시적인 저장소에 있으면 안 됨. 그렇지 않으면 테이블스페이스가 누락되거나 손실되어 클러스터가 작동하지 않을 수 있음.

> 참고: 논리적으로 클러스터당 하나의 테이블스페이스에 있는 여러 논리적 파일 시스템을 사용하는 것을 방해하는 것은 일반적으로 없음. 그러나 관리자는 그러한 설정에서 복잡성을 인식하고 있어야 함.

테이블스페이스 생성 자체는 데이터베이스 슈퍼유저로 수행해야 하지만, 그 후에 의도된 일반 데이터베이스 사용자에게 사용 권한 부여 가능. 이를 수행하려면 테이블스페이스에 대한 `CREATE` 권한을 부여함.

테이블, 인덱스 및 전체 데이터베이스를 특정 테이블스페이스에 할당 가능. 이를 수행하려면 주어진 테이블스페이스에 대한 `CREATE` 권한이 있는 사용자가 테이블스페이스 이름을 관련 명령에 매개변수로 전달해야 함. 예를 들어, 다음은 `space1` 테이블스페이스에 테이블을 생성함.

```sql
CREATE TABLE foo(i int) TABLESPACE space1;
```

또는 `default_tablespace` 매개변수를 사용함.

```sql
SET default_tablespace = space1;
CREATE TABLE foo(i int);
```

`default_tablespace`가 빈 문자열 이외의 값으로 설정되면, 명시적인 `TABLESPACE` 절이 없는 `CREATE TABLE` 및 `CREATE INDEX` 명령에 암묵적인 `TABLESPACE` 절이 적용됨.

임시 테이블 및 인덱스의 배치와 대규모 데이터 집합을 정렬하는 것과 같은 목적으로 내부적으로 생성되는 임시 파일에 대해 지정된 테이블스페이스 집합을 결정하는 `temp_tablespaces`라는 매개변수도 있음. 이것은 하나의 테이블스페이스 이름이 아닌 테이블스페이스 이름 목록이 될 수 있으므로, 임시 객체와 관련된 로드가 여러 테이블스페이스에 분산될 수 있음. 임시 객체가 생성될 때마다 목록의 무작위 멤버가 선택됨.

데이터베이스와 연관된 테이블스페이스는 데이터베이스의 시스템 카탈로그를 저장하는 데 사용됨. 또한, `TABLESPACE` 절이 주어지지 않고 `default_tablespace` 또는 `temp_tablespaces`(해당되는 대로)에 의해 다른 선택이 지정되지 않은 경우 데이터베이스 내에서 생성된 테이블, 인덱스 및 임시 파일에 사용되는 기본 테이블스페이스임. 테이블스페이스를 지정하지 않고 생성된 데이터베이스는 복사된 템플릿 데이터베이스와 동일한 테이블스페이스를 사용함.

데이터베이스 클러스터가 초기화될 때 두 개의 테이블스페이스가 자동으로 생성됨. `pg_global` 테이블스페이스는 공유 시스템 카탈로그에 사용됨. `pg_default` 테이블스페이스는 `template1` 및 `template0` 데이터베이스의 기본 테이블스페이스임(따라서 `TABLESPACE` 절로 재정의되지 않는 한 다른 데이터베이스의 기본 테이블스페이스이기도 함).

테이블스페이스가 생성되면 원하는 사용자에게 권한이 부여된 경우 모든 데이터베이스에서 사용 가능. 이는 테이블스페이스를 사용하는 모든 데이터베이스에서 모든 객체가 제거될 때까지 테이블스페이스 삭제 불가함을 의미함.

빈 테이블스페이스를 제거하려면 `DROP TABLESPACE` 명령을 사용함.

```sql
DROP TABLESPACE tablespace_name;
```

기존 테이블스페이스 집합을 검사하려면 `pg_tablespace` 시스템 카탈로그를 사용함. 예:

```sql
SELECT spcname FROM pg_tablespace;
```

`psql` 프로그램의 `\db` 메타 명령도 기존 테이블스페이스를 나열하는 데 유용함.

테이블스페이스의 파일 시스템 위치를 포함한 더 자세한 정보를 보려면 다음을 사용함.

```sql
SELECT spcname, spcowner::regrole, pg_tablespace_location(oid)
FROM pg_tablespace;
```

`pg_tablespace_location` 함수는 테이블스페이스의 파일 시스템 경로를 반환함.

테이블스페이스 구현을 단순화하기 위해 `$PGDATA/pg_tblspc` 디렉토리에는 클러스터에 정의된 비기본 테이블스페이스를 가리키는 심볼릭 링크가 생성됨. 심볼릭 링크를 지원하지 않는 시스템에서는 권장되지 않으나, `pg_tblspc` 링크가 가리키는 위치에 직접 디렉토리를 두는 것도 가능함.

> 경고: 서버가 실행 중일 때 이러한 링크를 수동으로 이동하거나 수정하면 데이터 무결성 오류가 발생할 수 있음.

---

### 참고 문서

- [PostgreSQL 18 공식 문서 - Managing Databases](https://www.postgresql.org/docs/18/managing-databases.html)
- [CREATE DATABASE](https://www.postgresql.org/docs/18/sql-createdatabase.html)
- [DROP DATABASE](https://www.postgresql.org/docs/18/sql-dropdatabase.html)
- [CREATE TABLESPACE](https://www.postgresql.org/docs/18/sql-createtablespace.html)
- [DROP TABLESPACE](https://www.postgresql.org/docs/18/sql-droptablespace.html)
- [ALTER DATABASE](https://www.postgresql.org/docs/18/sql-alterdatabase.html)
## PostgreSQL 18 백업 및 복원 (Backup and Restore)

### 세 가지 기본 백업 방식

PostgreSQL에서는 세 가지 기본적인 백업 방식을 제공함.

1. SQL 덤프 (SQL Dump)
2. 파일 시스템 레벨 백업 (File System Level Backup)
3. 연속 아카이빙 (Continuous Archiving)

각 방식마다 고유한 장단점이 있으며, 이후 섹션에서 자세히 설명함.

---

### 25.1. SQL 덤프

#### 목차
- 25.1.1. 덤프 복원
- 25.1.2. pg_dumpall 사용
- 25.1.3. 대용량 데이터베이스 처리

#### 개요

덤프 방식은 데이터베이스를 이전 상태로 재생성하는 SQL 명령이 포함된 파일을 생성함. PostgreSQL은 이를 위해 `pg_dump` 유틸리티를 제공함.

기본 사용법:
```bash
pg_dump dbname > dumpfile
```

주요 특징:
- `pg_dump`는 표준 출력으로 결과를 출력함
- 여러 파일 형식을 생성 가능 (병렬 처리 및 세밀한 객체 복원 허용)
- 일반적인 PostgreSQL 클라이언트 애플리케이션임 (원격 호스트에서 실행 가능)
- 백업하려는 모든 테이블에 대한 읽기 접근 권한 필요
- 전체 데이터베이스 백업을 위해서는 일반적으로 슈퍼유저 권한 필요
- 선택적으로 `-n schema` 또는 `-t table` 옵션으로 부분 백업 가능

연결 옵션:
```
-h host       (기본값: PGHOST 환경 변수 또는 로컬 호스트)
-p port       (기본값: PGPORT 환경 변수 또는 컴파일 시 설정된 기본값)
-U username   (기본값: 현재 OS 사용자 또는 PGUSER 환경 변수)
```

장점:
- 출력 결과는 최신 PostgreSQL 버전으로 다시 로드 가능
- 다른 머신 아키텍처로의 데이터베이스 전송에 사용 가능 (예: 32비트에서 64비트로)
- 아키텍처 전송에 사용할 수 있는 유일한 방식임
- 내부적으로 일관성 있음 (덤프 시작 시점의 스냅샷)
- 다른 데이터베이스 작업을 차단하지 않음 (대부분의 `ALTER TABLE`과 같이 배타적 잠금이 필요한 작업 제외)

---

#### 25.1.1. 덤프 복원

텍스트 덤프 복원:
```bash
psql -X dbname < dumpfile
```

중요 참고사항:
- `dbname` 데이터베이스는 미리 `template0`에서 생성되어 있어야 함
- 명령 예시:
```bash
createdb -T template0 dbname
```
- `-X` (`--no-psqlrc`) 옵션 사용 → psql이 기본 설정으로 실행됨
- `psql`은 `pg_dump`와 동일한 서버 및 사용자 옵션을 지원함

비텍스트 형식 복원:
```bash
pg_restore
```
(커스텀 및 디렉토리 형식 덤프에 사용)

복원 전 요구사항:
- 객체를 소유하거나 권한이 부여된 모든 사용자가 이미 존재해야 함
- 그렇지 않으면 복원 시 원래 소유권·권한으로 객체를 재생성하는 데 실패함

오류 처리 - 옵션 1 (오류 발생 시 계속):
기본 동작 - `psql`은 SQL 오류 발생 후에도 계속 진행 → 부분 복원이 됨

오류 처리 - 옵션 2 (오류 발생 시 중지):
```bash
psql -X --set ON_ERROR_STOP=on dbname < dumpfile
```
- psql은 SQL 오류 시 종료 코드 3으로 종료함
- 여전히 부분 복원 상태가 됨

오류 처리 - 옵션 3 (단일 트랜잭션):
```bash
psql -X -1 dbname < dumpfile
```
또는
```bash
psql -X --single-transaction dbname < dumpfile
```

특징:
- 전체 덤프가 단일 트랜잭션으로 복원됨
- 전체 완료 또는 전체 롤백 (all-or-nothing) 복원
- 주의: 사소한 오류로 인해 여러 시간의 복원이 롤백될 수 있음
- 복잡한 데이터베이스의 수동 정리보다 나을 수 있음

서버 간 직접 전송:
```bash
pg_dump -h host1 dbname | psql -X -h host2 dbname
```

#### template0 요구사항에 대한 중요 참고

덤프는 `template0`을 기준으로 함. `template1`을 통해 추가된 언어·프로시저 등도 함께 덤프됨. 사용자 정의된 `template1`로 복원할 때는 `template0`에서 빈 데이터베이스를 생성해야 함.
```bash
createdb -T template0 dbname
```

복원 후 단계:
- 각 데이터베이스에서 `ANALYZE` 실행 (쿼리 최적화기가 유용한 통계를 수집할 수 있도록)
- 참조: 섹션 24.1.3 (플래너 통계 업데이트) 및 섹션 24.1.6 (Autovacuum 데몬)
- 대용량 데이터 로드의 경우: 섹션 14.4 (데이터베이스 채우기) 참조

---

#### 25.1.2. pg_dumpall 사용

특징:
- `pg_dump`는 한 번에 하나의 데이터베이스만 백업함
- 역할(role)이나 테이블스페이스는 덤프하지 않음 (클러스터 전역 객체이며 데이터베이스별이 아니기 때문)
- `pg_dumpall`은 전체 데이터베이스 클러스터를 백업함
- 클러스터 전역 데이터(역할 및 테이블스페이스 정의)를 보존함

기본 사용법:
```bash
pg_dumpall > dumpfile
```

복원:
```bash
psql -X -f dumpfile postgres
```

참고:
- 기존 데이터베이스 이름 지정 가능 (빈 클러스터의 경우 `postgres` 사용)
- 복원을 위해 데이터베이스 슈퍼유저 접근 권한 필요
- 테이블스페이스 경로가 새 설치에 적합한지 확인 필요
- 각 데이터베이스는 내부적으로 일관성 있으나, 서로 다른 데이터베이스의 스냅샷은 동기화되지 않음

전역 객체만 백업:
```bash
pg_dumpall --globals-only
```
- 개별 데이터베이스에 `pg_dump`를 실행할 때 전체 클러스터 백업에 필요함

---

#### 25.1.3. 대용량 데이터베이스 처리

일부 운영 체제의 파일 크기 제한 문제를 `pg_dump`의 파이프 기능으로 해결함.

방법 1: 압축된 덤프 (gzip)

```bash
pg_dump dbname | gzip > filename.gz
```

복원 옵션:
```bash
gunzip -c filename.gz | psql dbname
```
또는
```bash
cat filename.gz | gunzip | psql dbname
```

방법 2: `split` 명령 사용

2 기가바이트 청크로 생성:
```bash
pg_dump dbname | split -b 2G - filename
```

복원:
```bash
cat filename* | psql dbname
```

GNU split과 gzip 함께 사용:
```bash
pg_dump dbname | split -b 2G --filter='gzip > $FILE.gz'
```

`zcat`으로 복원

방법 3: 커스텀 덤프 형식

요구사항: zlib 압축 라이브러리로 빌드된 PostgreSQL

```bash
pg_dump -Fc dbname > filename
```

특징:
- 작성 시 자동으로 데이터를 압축함
- gzip과 유사한 크기의 덤프를 생성함
- 선택적 테이블 복원을 허용함
- psql 스크립트가 아님

복원:
```bash
pg_restore -d dbname filename
```

방법 4: 병렬 덤프 기능

대용량 데이터베이스의 경우 병렬 처리로 덤프 속도 향상.

```bash
pg_dump -j num -F d -f out.dir dbname
```

매개변수:
- `-j num`: 병렬 처리 정도 제어
- `-F d`: 디렉토리 아카이브 형식 (병렬 덤프를 지원하는 유일한 형식)
- `-f out.dir`: 출력 디렉토리

병렬 복원:
```bash
pg_restore -j num -d dbname out.dir
```

참고:
- 커스텀 또는 디렉토리 아카이브 모드에서 작동함
- `pg_dump -j`로 생성되었는지 여부와 관계없이 작동함

복합 접근 방식:
- 매우 대용량 데이터베이스의 경우 `split`과 압축 또는 커스텀 형식을 결합해야 할 수 있음

---

### 25.2. 파일 시스템 레벨 백업

#### 개요
PostgreSQL이 데이터베이스 데이터를 저장하는 데 사용하는 파일을 직접 복사하는 대안적인 백업 전략임. 위치 정보는 섹션 18.2 (데이터베이스 클러스터 생성)에서 확인 가능.

#### 예시 명령
```bash
tar -cf backup.tar /usr/local/pgsql/data
```

#### 두 가지 중요한 제한사항

##### 1. 서버가 종료되어 있어야 함
- 사용 가능한 백업을 얻으려면 데이터베이스 서버가 반드시 종료되어 있어야 함
- 모든 연결을 금지하는 것과 같은 절반의 조치는 작동하지 않음
- 이유: `tar` 및 유사 도구는 파일 시스템 상태의 원자적 스냅샷을 찍지 않으며, 서버 내부 버퍼링도 존재하기 때문
- 서버 종료 정보: 섹션 18.5 (서버 종료) 참고
- 데이터 복원 전에도 서버가 종료되어 있어야 함

##### 2. 개별 테이블이나 데이터베이스를 백업할 수 없음
- 특정 개별 테이블이나 데이터베이스만 백업하거나 복원하는 것은 불가능
- 이유: 파일은 커밋 로그 파일(`pg_xact/*`) 없이는 사용 불가
- `pg_xact/*` 파일에는 모든 트랜잭션의 커밋 상태가 포함됨
- 테이블 파일은 이 정보가 있어야만 사용 가능
- 테이블과 관련된 `pg_xact` 데이터만 복원 → 데이터베이스 클러스터의 다른 모든 테이블이 사용 불가 상태가 됨
- 파일 시스템 백업은 전체 데이터베이스 클러스터의 완전한 백업 및 복원에서만 작동함

#### 대안: 일관된 스냅샷 접근 방식

##### 절차
- 데이터베이스를 포함하는 볼륨의 "고정된 스냅샷" 생성 (파일 시스템이 지원하는 경우)
- 스냅샷에서 전체 데이터 디렉토리를 백업 장치로 복사
- 고정된 스냅샷 해제
- 데이터베이스 서버가 실행 중일 때도 작동 가능

##### 중요 고려사항
- 백업은 서버가 제대로 종료되지 않은 상태의 데이터베이스 파일을 저장함
- 서버 재시작 시 이전 인스턴스가 충돌한 것으로 인식 → WAL 로그를 재생함
- 이것은 문제가 아니며, 단지 인식하고 있으면 됨
- 백업에 WAL 파일을 포함해야 함
- 스냅샷을 찍기 전에 `CHECKPOINT`를 수행하면 복구 시간 단축 가능

##### 다중 파일 시스템 제한
- 데이터베이스가 여러 파일 시스템에 분산된 경우 정확히 동시적인 고정 스냅샷을 얻지 못할 수 있음
- 다음 경우 스냅샷이 동시에 이루어져야 함
  - 데이터 파일과 WAL 로그가 다른 디스크에 있는 경우
  - 테이블스페이스가 다른 파일 시스템에 있는 경우
- 이러한 상황에서는 스냅샷 백업을 신뢰하기 전에 파일 시스템 문서를 주의 깊게 읽을 필요

#### 다중 파일 시스템 문제 해결책

##### 옵션 1: 스냅샷 중 종료
- 모든 고정 스냅샷을 설정하기에 충분한 시간 동안 데이터베이스 서버를 종료

##### 옵션 2: 연속 아카이빙 베이스 백업
- 연속 아카이빙 베이스 백업 수행 (섹션 25.3.2 참고)
- 백업 중 파일 시스템 변경에 영향받지 않음
- 백업 프로세스 중 연속 아카이빙 활성화 필요
- 연속 아카이브 복구를 사용하여 복원 (섹션 25.3.5 참고)

##### 옵션 3: rsync 방법
- 데이터베이스 서버가 실행 중일 때 `rsync` 실행 (첫 번째 실행)
- `rsync --checksum`을 수행하기에 충분한 시간 동안 데이터베이스 서버 종료
- 참고: `rsync`는 파일 수정 시간 정밀도가 1초뿐이므로 `--checksum` 필요
- 두 번째 rsync는 더 빠름 (전송할 데이터가 상대적으로 적기 때문)
- 서버가 다운되어 있었으므로 결과가 일관됨
- 최소한의 다운타임으로 파일 시스템 백업 가능

#### 성능 비교

##### 크기
- 파일 시스템 백업은 일반적으로 SQL 덤프보다 큼
- `pg_dump`는 인덱스 내용을 덤프하지 않고 재생성 명령만 덤프함

##### 속도
- 파일 시스템 백업 수행이 더 빠를 수 있음

---

### 25.3. 연속 아카이빙 및 특정 시점 복구 (PITR)

#### 개요

PostgreSQL은 항상 클러스터 데이터 디렉토리의 `pg_wal/` 하위 디렉토리에 _Write Ahead Log_ (WAL)를 유지함. 로그는 데이터베이스 데이터 파일에 대한 모든 변경 사항을 기록함. 이 로그는 주로 충돌 안전성을 위해 존재함: 시스템 충돌 시 마지막 체크포인트 이후에 만들어진 로그 항목을 "재생"함 → 데이터베이스를 일관성 있는 상태로 복원 가능. 그런데 로그의 존재는 데이터베이스 백업을 위한 세 번째 전략을 가능하게 함: 파일 시스템 수준 백업과 WAL 파일의 백업을 결합 가능. 복구가 필요한 경우 파일 시스템 백업을 복원한 다음 백업된 WAL 파일에서 재생 → 시스템을 현재 상태로 가져옴. 이 접근 방식은 이전 접근 방식보다 관리가 더 복잡하지만 몇 가지 중요한 이점이 있음.

- 시작점으로 완벽하게 일관된 파일 시스템 백업이 필요 없음. 백업의 내부 불일치는 로그 재생으로 수정됨 (충돌 복구 중에 일어나는 것과 크게 다르지 않음). 따라서 파일 시스템 스냅샷 기능이 필요 없고 tar 또는 유사한 아카이빙 도구만 있으면 됨.

- 무한히 긴 WAL 파일 시퀀스를 재생용으로 결합 가능하므로 WAL 파일을 계속 아카이빙하기만 하면 연속 백업 달성 가능. 이는 전체 백업을 자주 수행하기 불편할 수 있는 대용량 데이터베이스에 특히 유용함.

- WAL 항목을 끝까지 모두 재생할 필요 없음. 어느 시점에서든 재생을 중지하면 해당 시점의 데이터베이스 일관된 스냅샷을 확보 가능. 따라서 이 기술은 _특정 시점 복구(point-in-time recovery)_를 지원함: 베이스 백업이 수행된 이후 어느 시점으로든 데이터베이스 복원 가능.

- 동일한 베이스 백업 파일이 로드된 다른 머신에 WAL 파일 시리즈를 지속적으로 공급 → _웜 스탠바이(warm standby)_ 시스템 확보. 언제든지 두 번째 머신을 가동하면 거의 최신 복사본의 데이터베이스를 확보하게 됨.

#### 참고

pg_dump와 pg_dumpall은 파일 시스템 수준 백업을 생성하지 않으며 연속 아카이빙 솔루션의 일부로 사용 불가. 이러한 덤프는 _논리적_이며 WAL 재생에 사용될 수 있는 충분한 정보를 포함하지 않음.

일반 파일 시스템 백업 기술과 마찬가지로 이 방법은 전체 데이터베이스 클러스터의 복원만 지원 가능하며 하위 집합은 지원 불가. 또한 많은 아카이브 저장소가 필요함: 베이스 백업이 방대할 수 있고 바쁜 시스템은 아카이빙해야 할 많은 메가바이트의 WAL 트래픽을 생성함. 그럼에도 불구하고 높은 신뢰성이 필요한 많은 상황에서 선호되는 백업 기술임.

연속 아카이빙(많은 데이터베이스 벤더가 "온라인 백업"이라고도 함)을 사용하여 성공적으로 복구하려면 적어도 백업 시작 시간까지 거슬러 올라가는 연속적인 아카이브된 WAL 파일 시퀀스가 필요함. 따라서 시작하려면 첫 번째 베이스 백업을 수행하기 _전에_ WAL 파일 아카이빙 절차를 설정하고 테스트해야 함. 이에 따라 먼저 WAL 파일 아카이빙의 메커니즘부터 설명함.

---

#### 25.3.1. WAL 아카이빙 설정

추상적인 의미에서 실행 중인 PostgreSQL 시스템은 무한히 긴 WAL 레코드 시퀀스를 생성함. 시스템은 이 시퀀스를 WAL _세그먼트 파일_로 물리적으로 나눔. 세그먼트 파일은 일반적으로 각각 16MB임 (세그먼트 크기는 initdb 중에 변경 가능). 세그먼트 파일에는 추상적인 WAL 시퀀스에서의 위치를 반영하는 숫자 이름이 지정됨. WAL 아카이빙을 사용하지 않을 때 시스템은 일반적으로 몇 개의 세그먼트 파일만 생성한 다음 더 이상 필요하지 않은 세그먼트 파일의 이름을 더 높은 세그먼트 번호로 변경하여 "재활용"함. 마지막 체크포인트 이전의 내용을 가진 세그먼트 파일은 더 이상 관심 대상이 아니며 재활용 가능하다고 간주함.

WAL 데이터를 아카이빙할 때는 각 세그먼트 파일이 채워지면 해당 내용을 캡처하고 세그먼트 파일이 재사용을 위해 재활용되기 전에 해당 데이터를 어딘가에 저장해야 함. 애플리케이션 및 사용 가능한 하드웨어에 따라 "데이터를 어딘가에 저장"하는 방법은 다양함: 세그먼트 파일을 다른 머신의 NFS 마운트 디렉토리로 복사·테이프 드라이브에 기록(각 파일의 원래 이름을 식별하는 방법이 있는지 확인 필요)·함께 묶어 CD에 굽기·완전히 다른 방식 등. 데이터베이스 관리자에게 유연성을 제공하기 위해 PostgreSQL은 아카이빙 방법에 대해 어떤 가정도 하지 않으려 함. 대신 PostgreSQL은 관리자가 완료된 세그먼트 파일을 필요한 곳으로 복사하기 위해 실행할 셸 명령 또는 아카이브 라이브러리를 지정할 수 있도록 함. 이것은 `cp`를 사용하는 셸 명령처럼 간단할 수도 있고 복잡한 C 함수를 호출할 수도 있음 - 모두 사용자에게 달린 선택임.

WAL 아카이빙을 활성화하려면 [wal_level](runtime-config-wal.html#GUC-WAL-LEVEL) 구성 매개변수를 `replica` 이상으로 설정하고 [archive_mode](runtime-config-wal.html#GUC-ARCHIVE-MODE)를 `on`으로 설정한 다음 [archive_command](runtime-config-wal.html#GUC-ARCHIVE-COMMAND) 구성 매개변수에 사용할 셸 명령을 지정하거나 [archive_library](runtime-config-wal.html#GUC-ARCHIVE-LIBRARY) 구성 매개변수에 사용할 라이브러리를 지정함. 실제로 이러한 설정은 항상 `postgresql.conf` 파일에 배치됨.

`archive_command`에서 `%p`는 아카이빙할 파일의 경로 이름으로 대체되고 `%f`는 파일 이름만으로 대체됨. (경로 이름은 현재 작업 디렉토리, 즉 클러스터의 데이터 디렉토리를 기준으로 함.) 명령에 실제 `%` 문자를 포함해야 하는 경우 `%%`를 사용함. 가장 간단한 유용한 명령은 다음과 같음.

```bash
archive_command = 'test ! -f /mnt/server/archivedir/%f && cp %p /mnt/server/archivedir/%f'  # Unix
archive_command = 'copy "%p" "C:\\server\\archivedir\\%f"'  # Windows
```

이것은 아카이빙 가능한 WAL 세그먼트를 `/mnt/server/archivedir` 디렉토리로 복사함. (이것은 예시이며 권장 사항이 아니고 모든 플랫폼에서 작동하지 않을 수 있음.) `%p` 및 `%f` 매개변수가 대체된 후 실제로 실행되는 명령은 다음과 같을 수 있음.

```bash
test ! -f /mnt/server/archivedir/00000001000000A900000065 && cp pg_wal/00000001000000A900000065 /mnt/server/archivedir/00000001000000A900000065
```

아카이빙할 각 새 파일에 대해 유사한 명령이 생성됨.

아카이브 명령은 PostgreSQL 서버가 실행되는 것과 동일한 사용자 권한으로 실행됨. 아카이빙되는 WAL 파일 시리즈에는 데이터베이스의 모든 내용이 포함되므로 아카이브된 데이터가 외부에 노출되지 않도록 주의 필요. 예를 들어 그룹 또는 전체 사용자에게 읽기 권한이 없는 디렉토리에 아카이브함.

아카이브 명령이 성공한 경우에만 0 종료 상태를 반환하는 것이 중요함. 0 결과를 받으면 PostgreSQL은 파일이 성공적으로 아카이브되었다고 간주 → 이를 제거하거나 재활용함. 그러나 0이 아닌 상태는 PostgreSQL에게 파일이 아카이브되지 않았음을 알림 → 성공할 때까지 주기적으로 재시도함.

아카이빙의 또 다른 방법은 `archive_library`에 사용자 정의 아카이브 모듈을 지정하는 것임. 이러한 모듈은 `C`로 작성되므로 직접 구현하면 셸 명령보다 훨씬 많은 작업이 필요할 수 있음. 그러나 아카이브 모듈은 셸 방식보다 성능이 우수하고 다양한 서버 리소스에 접근 가능함. 아카이브 모듈에 대한 자세한 내용은 [49장](archive-modules.html "49장. 아카이브 모듈") 참조.

아카이브 명령이 신호(서버 종료의 일부로 사용되는 SIGTERM 제외)에 의해 종료되거나 125보다 큰 종료 상태로 셸에 의해 오류가 발생하거나(예: 명령을 찾을 수 없음), 아카이브 함수가 `ERROR` 또는 `FATAL`을 발생시키면 아카이버 프로세스가 중단되고 postmaster에 의해 다시 시작됨. 이러한 경우 실패는 [pg_stat_archiver](monitoring-stats.html#PG-STAT-ARCHIVER-VIEW "표 27.22. pg_stat_archiver 뷰")에 보고되지 않음.

아카이브 명령과 라이브러리는 일반적으로 기존 아카이브 파일을 덮어쓰지 않도록 설계해야 함. 이것은 관리자 오류(예: 두 개의 다른 서버의 출력을 동일한 아카이브 디렉토리로 보내는 것)의 경우 아카이브의 무결성을 보존하기 위한 중요한 안전 기능임. 제안된 아카이브 라이브러리가 기존 파일을 덮어쓰지 않는지 테스트할 필요.

드문 경우 PostgreSQL은 이전에 아카이브된 WAL 파일을 다시 아카이브하려고 시도할 수 있음. 예를 들어 서버가 아카이빙 성공에 대한 지속적인 기록을 만들기 전에 시스템이 충돌하면 서버는 재시작 후 파일을 다시 아카이브하려고 시도함 (아카이빙이 여전히 활성화되어 있는 경우). 아카이브 명령이나 라이브러리가 기존 파일을 만나면 WAL 파일이 기존 아카이브와 내용이 동일하고 기존 아카이브가 스토리지에 완전히 지속된 경우 각각 0 상태 또는 `true`를 반환해야 함. 기존 파일이 아카이빙 중인 WAL 파일과 다른 내용을 포함하면 아카이브 명령이나 라이브러리는 _반드시_ 각각 0이 아닌 상태 또는 `false`를 반환해야 함.

위의 Unix용 예제 명령은 별도의 `test` 단계를 포함하여 기존 아카이브 덮어쓰기를 회피함. 일부 Unix 플랫폼에서는 `cp`에 `-i`와 같은 스위치가 있어 같은 작업을 덜 장황하게 수행 가능하지만 올바른 종료 상태가 반환되는지 확인하지 않고 이에 의존하는 것은 금지. (특히 GNU `cp`는 `-i`를 사용하고 대상 파일이 이미 존재하면 상태 0을 반환하는데, 이것은 원하는 동작이 _아님_.)

아카이빙 설정을 설계할 때 일부 측면에서 운영자 개입이 필요하거나 아카이브 공간이 부족하여 아카이브 명령이나 라이브러리가 반복적으로 실패할 경우 어떻게 대처할지 미리 고려할 필요. 예를 들어 오토체인저 없이 테이프에 쓰는 경우 이런 일이 발생할 수 있음. 테이프가 가득 차면 테이프를 교체할 때까지 더 이상 아카이브 불가. 상황을 합리적으로 빠르게 해결할 수 있도록 모든 오류 조건이나 운영자에 대한 요청이 적절하게 보고되도록 해야 함. `pg_wal/` 디렉토리는 상황이 해결될 때까지 WAL 세그먼트 파일로 계속 채워짐. (`pg_wal/`을 포함하는 파일 시스템이 가득 차면 PostgreSQL은 PANIC 종료를 수행함. 커밋된 트랜잭션은 손실되지 않지만 공간을 확보할 때까지 데이터베이스는 오프라인 상태로 유지됨.)

아카이브 명령이나 라이브러리의 속도는 서버가 WAL 데이터를 생성하는 평균 속도를 따라갈 수 있는 한 중요하지 않음. 아카이빙 프로세스가 약간 뒤처지더라도 정상 작업은 계속됨. 아카이빙이 크게 뒤처지면 재해 발생 시 손실될 데이터의 양이 증가함. 또한 `pg_wal/` 디렉토리에 아직 아카이브되지 않은 많은 수의 세그먼트 파일이 포함되어 결국 사용 가능한 디스크 공간을 초과할 수 있음. 아카이빙 프로세스가 의도한 대로 작동하는지 모니터링할 필요.

아카이브 명령이나 라이브러리를 작성할 때 아카이빙할 파일 이름이 최대 64자 길이이고 ASCII 문자, 숫자 및 점의 조합을 포함할 수 있다고 가정해야 함. 원래 상대 경로(`%p`)를 보존할 필요는 없지만 파일 이름(`%f`)은 보존해야 함.

WAL 아카이빙을 통해 PostgreSQL 데이터베이스의 데이터에 대한 모든 수정 사항을 복원 가능하지만 구성 파일(즉, `postgresql.conf`, `pg_hba.conf` 및 `pg_ident.conf`)에 대한 변경 사항은 복원되지 않음. 이러한 파일은 SQL 작업이 아닌 수동으로 편집되기 때문. 정기적인 파일 시스템 백업 절차로 백업될 위치에 구성 파일을 보관할 것을 권장. 구성 파일을 재배치하는 방법은 [섹션 19.2](runtime-config-file-locations.html "19.2. 파일 위치") 참조.

아카이브 명령이나 함수는 완료된 WAL 세그먼트에서만 호출됨. 따라서 서버가 WAL 트래픽을 거의 생성하지 않거나 (또는 그렇게 하는 여유 기간이 있는 경우) 트랜잭션 완료와 아카이브 스토리지에 안전하게 기록되는 사이에 긴 지연이 있을 수 있음. 아카이브되지 않은 데이터가 얼마나 오래된 것인지에 대한 제한을 두려면 [archive_timeout](runtime-config-wal.html#GUC-ARCHIVE-TIMEOUT)을 설정 → 서버가 최소한 그 정도 자주 새 WAL 세그먼트 파일로 전환하도록 강제 가능. 강제 전환으로 인해 조기에 아카이브된 파일은 여전히 완전히 채워진 파일과 동일한 길이임. 따라서 매우 짧은 `archive_timeout` 설정은 권장하지 않음 - 아카이브 스토리지가 팽창함. 1분 정도의 `archive_timeout` 설정이 일반적으로 합리적임.

또한 방금 완료된 트랜잭션이 가능한 빨리 아카이브되도록 하려면 `pg_switch_wal`을 사용하여 수동으로 세그먼트 전환을 강제 가능. WAL 관리와 관련된 다른 유틸리티 함수는 [표 9.97](functions-admin.html#FUNCTIONS-ADMIN-BACKUP-TABLE "표 9.97. 백업 제어 함수")에 나열됨.

`wal_level`이 `minimal`이면 [섹션 14.4.7](populate.html#POPULATE-PITR "14.4.7. WAL 아카이빙 및 스트리밍 복제 비활성화")에 설명된 대로 일부 SQL 명령이 WAL 로깅을 피하도록 최적화됨. 이러한 문 중 하나를 실행하는 동안 아카이빙이나 스트리밍 복제가 켜져 있으면 WAL에 아카이브 복구에 충분한 정보가 포함되지 않음. (충돌 복구는 영향을 받지 않음.) 이러한 이유로 `wal_level`은 서버 시작 시에만 변경 가능. 그러나 `archive_command`와 `archive_library`는 구성 파일 다시 로드로 변경 가능. 셸을 통해 아카이빙하고 일시적으로 아카이빙을 중지하려면 한 가지 방법은 `archive_command`를 빈 문자열(`''`)로 설정하는 것임. 이렇게 하면 작동하는 `archive_command`가 다시 설정될 때까지 WAL 파일이 `pg_wal/`에 축적됨.

---

#### 25.3.2. 베이스 백업 만들기

베이스 백업을 수행하는 가장 쉬운 방법은 [pg_basebackup](app-pgbasebackup.html "pg_basebackup") 도구 사용임. 일반 파일 또는 tar 아카이브로 베이스 백업 생성 가능. [pg_basebackup](app-pgbasebackup.html "pg_basebackup")이 제공할 수 있는 것보다 더 많은 유연성이 필요한 경우 저수준 API를 사용하여 베이스 백업 생성도 가능 ([섹션 25.3.4](continuous-archiving.html#BACKUP-LOWLEVEL-BASE-BACKUP "25.3.4. 저수준 API를 사용하여 베이스 백업 만들기") 참조).

베이스 백업을 수행하는 데 걸리는 시간에 대해 걱정할 필요 없음. 그러나 일반적으로 `full_page_writes`를 비활성화한 상태로 서버를 실행하는 경우 백업 모드 중에 `full_page_writes`가 효과적으로 강제 적용되므로 백업 실행 중 성능 저하를 느낄 수 있음.

백업을 사용하려면 파일 시스템 백업 중 및 이후에 생성된 모든 WAL 세그먼트 파일을 보관해야 함. 이를 지원하기 위해 베이스 백업 프로세스는 WAL 아카이브 영역에 즉시 저장되는 _백업 이력 파일_을 생성함. 이 파일은 파일 시스템 백업에 필요한 첫 번째 WAL 세그먼트 파일의 이름을 따서 명명됨. 예를 들어 시작 WAL 파일이 `0000000100001234000055CD`이면 백업 이력 파일은 `0000000100001234000055CD.007C9330.backup`과 같은 이름이 됨. (파일 이름의 두 번째 부분은 WAL 파일 내의 정확한 위치를 나타내며 일반적으로 무시 가능.) 파일 시스템 백업과 백업 중 사용된 WAL 세그먼트 파일(백업 이력 파일에 지정된 대로)을 안전하게 아카이브하면 숫자상으로 더 작은 이름을 가진 모든 아카이브된 WAL 세그먼트는 파일 시스템 백업을 복구하는 데 더 이상 필요하지 않으며 삭제 가능. 그러나 데이터를 복구할 수 있음을 절대적으로 확신하기 위해 여러 백업 세트를 유지할 것을 권장.

백업 이력 파일은 작은 텍스트 파일에 불과함. [pg_basebackup](app-pgbasebackup.html "pg_basebackup")에 지정한 레이블 문자열과 백업의 시작 및 종료 시간 및 WAL 세그먼트가 포함됨. 레이블을 사용하여 연관된 덤프 파일을 식별했다면 아카이브된 이력 파일만으로도 어떤 덤프 파일을 복원해야 하는지 파악 가능.

마지막 베이스 백업까지 모든 아카이브된 WAL 파일을 보관해야 하므로 베이스 백업 간의 간격은 일반적으로 아카이브된 WAL 파일에 사용할 스토리지 양을 기준으로 선택해야 함. 복구가 필요한 경우 복구에 얼마나 많은 시간을 할애할 수 있는지도 고려 필요 - 시스템은 모든 WAL 세그먼트를 재생해야 하며 마지막 베이스 백업 이후 오랜 시간이 경과했다면 상당한 시간이 걸릴 수 있음.

---

#### 25.3.3. 증분 백업 만들기

`--incremental` 옵션을 지정하여 [pg_basebackup](app-pgbasebackup.html "pg_basebackup")을 사용하면 증분 백업 수행 가능. `--incremental`에 대한 인수로 동일한 서버의 이전 백업에 대한 백업 매니페스트를 제공해야 함. 결과 백업에서 비관계 파일은 전체적으로 포함되지만 일부 관계 파일은 이전 백업 이후 변경된 블록과 파일의 현재 버전을 재구성하기에 충분한 메타데이터만 포함하는 더 작은 증분 파일로 대체될 수 있음.

어떤 블록을 백업해야 하는지 파악하기 위해 서버는 데이터 디렉토리의 `pg_wal/summaries` 디렉토리에 저장된 WAL 요약을 사용함. 필요한 요약 파일이 없으면 증분 백업 시도가 실패함. 이 디렉토리에 있는 요약은 이전 백업의 시작 LSN부터 현재 백업의 시작 LSN까지 모든 LSN을 포함해야 함. 서버가 현재 백업의 시작 LSN을 설정한 직후 WAL 요약을 찾기 때문에 필요한 요약 파일이 디스크에 즉시 존재하지 않을 수 있지만 서버는 누락된 파일이 나타날 때까지 대기함. 이것은 WAL 요약 프로세스가 뒤처진 경우에도 도움이 됨. 그러나 필요한 파일이 이미 제거되었거나 WAL 요약기가 충분히 빨리 따라잡지 못하면 증분 백업이 실패함.

증분 백업을 복원할 때 증분 백업 자체뿐만 아니라 증분 백업에서 생략된 블록을 제공하는 데 필요한 모든 이전 백업도 필요함. 이 요구 사항에 대한 자세한 내용은 [pg_combinebackup](app-pgcombinebackup.html "pg_combinebackup") 참조. 클러스터의 체크섬 상태가 변경된 경우 `pg_combinebackup` 사용에 제한이 있음. [pg_combinebackup 제한 사항](app-pgcombinebackup.html#APP-PGCOMBINEBACKUP-LIMITATIONS "제한 사항") 참조.

전체 백업을 사용하기 위한 모든 요구 사항은 증분 백업에도 동일하게 적용됨. 예를 들어 파일 시스템 백업 중 및 이후에 생성된 모든 WAL 세그먼트 파일과 관련 WAL 이력 파일이 여전히 필요함. 그리고 [섹션 25.3.5](continuous-archiving.html#BACKUP-PITR-RECOVERY "25.3.5. 연속 아카이브 백업을 사용한 복구")에 설명된 대로 `recovery.signal`(또는 `standby.signal`)을 생성하고 복구를 수행해야 함. 복원 시 이전 백업을 사용할 수 있어야 하고 `pg_combinebackup`을 사용해야 하는 요구 사항은 다른 모든 것 위에 추가되는 요구 사항임. PostgreSQL에는 나중에 증분 백업을 복원하기 위한 기초로 어떤 백업이 여전히 필요한지 파악하는 내장 메커니즘이 없음. 전체 및 증분 백업 간의 관계를 직접 추적하고 나중에 증분 백업을 복원할 때 필요할 수 있는 이전 백업을 제거하지 않도록 주의 필요.

증분 백업은 일반적으로 데이터의 상당 부분이 변경되지 않거나 천천히 변경되는 비교적 대용량 데이터베이스에만 의미가 있음. 소규모 데이터베이스의 경우 증분 백업의 존재를 무시하고 관리하기 더 간단한 전체 백업을 수행하는 편이 간단함. 모두 크게 수정되는 대용량 데이터베이스의 경우 증분 백업은 전체 백업보다 훨씬 작지 않음.

증분 백업은 재생이 이전 백업보다 나중 체크포인트에서 시작하는 경우에만 가능함. 프라이머리에서 증분 백업을 수행하면 각 백업이 새 체크포인트를 트리거하므로 이 조건은 항상 충족됨. 스탠바이에서는 가장 최근의 재시작 지점에서 재생이 시작됨. 따라서 이전 백업 이후 활동이 거의 없었다면 새 재시작 지점이 생성되지 않았을 수 있으므로 스탠바이 서버의 증분 백업이 실패할 수 있음.

---

#### 25.3.4. 저수준 API를 사용하여 베이스 백업 만들기

[pg_basebackup](app-pgbasebackup.html "pg_basebackup")을 사용하여 전체 또는 증분 베이스 백업을 수행하는 대신 저수준 API를 사용하여 베이스 백업 수행 가능. 이 절차는 pg_basebackup 방법보다 몇 단계가 더 있지만 비교적 간단함. 이러한 단계가 순서대로 실행되고 다음 단계로 진행하기 전에 단계의 성공 여부를 확인하는 것이 매우 중요함.

여러 백업을 동시에 실행 가능함 (이 백업 API를 사용하여 시작된 것과 [pg_basebackup](app-pgbasebackup.html "pg_basebackup")을 사용하여 시작된 것 모두).

##### 단계별 절차

1. WAL 아카이빙이 활성화되어 있고 작동하는지 확인.

2. 서버에 연결함 (어떤 데이터베이스인지는 중요하지 않음) `pg_backup_start`를 실행할 권한이 있는 사용자(슈퍼유저 또는 함수에 대한 `EXECUTE` 권한이 부여된 사용자)로 다음 명령을 실행함.

```sql
SELECT pg_backup_start(label => 'label', fast => false);
```

여기서 `label`은 이 백업 작업을 고유하게 식별하는 데 사용할 문자열임. `pg_backup_start`를 호출하는 연결은 백업 끝까지 유지되어야 하며, 그렇지 않으면 백업이 자동으로 중단됨.

온라인 백업은 항상 체크포인트 시작 시 시작됨. 기본적으로 `pg_backup_start`는 다음 정기 예약된 체크포인트가 완료될 때까지 대기하며 오랜 시간이 걸릴 수 있음 (구성 매개변수 [checkpoint_timeout](runtime-config-wal.html#GUC-CHECKPOINT-TIMEOUT) 및 [checkpoint_completion_target](runtime-config-wal.html#GUC-CHECKPOINT-COMPLETION-TARGET) 참조). 이것은 일반적으로 실행 중인 시스템에 미치는 영향을 최소화하므로 선호됨. 가능한 빨리 백업을 시작하려면 두 번째 매개변수로 `true`를 `pg_backup_start`에 전달 → 가능한 한 많은 I/O를 사용하여 가능한 빨리 완료되는 즉시 체크포인트를 요청함.

3. 백업을 수행함, tar 또는 cpio와 같은 편리한 파일 시스템 백업 도구를 사용함 (pg_dump나 pg_dumpall은 금지). 이 작업을 수행하는 동안 데이터베이스의 정상 작업을 중지할 필요도 없고 바람직하지도 않음. 이 백업 중에 고려해야 할 사항은 [섹션 25.3.4.1](continuous-archiving.html#BACKUP-LOWLEVEL-BASE-BACKUP-DATA "25.3.4.1. 데이터 디렉토리 백업") 참조.

4. 이전과 동일한 연결에서 다음 명령을 실행함.

```sql
SELECT * FROM pg_backup_stop(wait_for_archive => true);
```

이것은 백업 모드를 종료함. 프라이머리에서는 다음 WAL 세그먼트로 자동 전환도 수행함. 스탠바이에서는 WAL 세그먼트를 자동으로 전환할 수 없으므로 수동 전환을 수행하기 위해 프라이머리에서 `pg_switch_wal` 실행 가능. 전환의 이유는 백업 간격 동안 작성된 마지막 WAL 세그먼트 파일이 아카이브될 준비가 되도록 정리하기 위함임.

`pg_backup_stop`은 세 가지 값이 있는 하나의 행을 반환함. 이러한 필드 중 두 번째는 백업 루트 디렉토리의 `backup_label`이라는 파일에 작성되어야 함. 세 번째 필드는 필드가 비어 있지 않은 한 `tablespace_map`이라는 파일에 작성되어야 함. 이러한 파일은 백업이 작동하는 데 필수적이며 수정 없이 바이트 단위로 작성되어야 하며 바이너리 모드로 파일을 열어야 할 수 있음.

5. 백업 중에 활성화된 WAL 세그먼트 파일이 아카이브되면 완료됨. `pg_backup_stop`의 첫 번째 반환 값으로 식별된 파일은 완전한 백업 파일 세트를 구성하는 데 필요한 마지막 세그먼트임. 프라이머리에서 `archive_mode`가 활성화되어 있고 `wait_for_archive` 매개변수가 `true`이면 `pg_backup_stop`은 마지막 세그먼트가 아카이브될 때까지 반환하지 않음. 스탠바이에서는 `pg_backup_stop`이 대기하려면 `archive_mode`가 `always`여야 함. `archive_command` 또는 `archive_library`를 이미 구성했으므로 이러한 파일의 아카이빙은 자동으로 발생함. 대부분의 경우 이것은 빠르게 발생하지만 지연이 없는지 확인하기 위해 아카이브 시스템을 모니터링할 것을 권장. 아카이브 명령이나 라이브러리의 실패로 인해 아카이브 프로세스가 뒤처진 경우 아카이브가 성공하고 백업이 완료될 때까지 계속 재시도함. `pg_backup_stop` 실행에 시간 제한을 두려면 적절한 `statement_timeout` 값을 설정하되 이로 인해 `pg_backup_stop`이 종료되면 백업이 유효하지 않을 수 있음.

백업 프로세스가 백업에 필요한 모든 WAL 세그먼트 파일이 성공적으로 아카이브되었는지 모니터링하고 확인하는 경우 `wait_for_archive` 매개변수(기본값 true)를 false로 설정 → `pg_backup_stop`이 중지 백업 레코드가 WAL에 작성되자마자 반환되도록 설정 가능. 기본적으로 `pg_backup_stop`은 모든 WAL이 아카이브될 때까지 대기하며 시간이 걸릴 수 있음. 이 옵션은 주의해서 사용해야 함: WAL 아카이빙이 올바르게 모니터링되지 않으면 백업에 모든 WAL 파일이 포함되지 않아 불완전하여 복원 불가.

##### 25.3.4.1. 데이터 디렉토리 백업

일부 파일 시스템 백업 도구는 복사 중에 파일이 변경되면 경고나 오류를 발생시킴. 활성 데이터베이스의 베이스 백업을 수행할 때 이 상황은 정상이며 오류가 아님. 그러나 이러한 종류의 불만을 실제 오류와 구별할 수 있어야 함. 예를 들어 일부 버전의 rsync는 "사라진 소스 파일"에 대해 별도의 종료 코드를 반환하며 이 종료 코드를 비오류 사례로 허용하는 드라이버 스크립트 작성이 가능함. 또한 일부 버전의 GNU tar는 tar가 파일을 복사하는 동안 파일이 잘린 경우 치명적인 오류와 구별할 수 없는 오류 코드를 반환함. 다행히 GNU tar 버전 1.16 이상에서는 백업 중에 파일이 변경되면 1로 종료하고 다른 오류의 경우 2로 종료함. GNU tar 버전 1.23 이상에서는 경고 옵션 `--warning=no-file-changed --warning=no-file-removed`를 사용하여 관련 경고 메시지를 숨길 수 있음.

백업에 데이터베이스 클러스터 디렉토리(예: `/usr/local/pgsql/data`) 아래의 모든 파일이 포함되어 있는지 확인 필요. 이 디렉토리 아래에 있지 않은 테이블스페이스를 사용하는 경우 해당 테이블스페이스도 포함하도록 주의 (그리고 백업 아카이브가 심볼릭 링크를 링크로 아카이브하는지 확인 - 그렇지 않으면 복원 시 테이블스페이스가 손상됨).

그러나 클러스터의 `pg_wal/` 하위 디렉토리 내의 파일은 백업에서 제외해야 함. 이 약간의 조정은 복원 시 실수의 위험을 줄이기 때문에 가치가 있음. `pg_wal/`이 클러스터 디렉토리 외부 어딘가를 가리키는 심볼릭 링크인 경우 정리하기 쉬우며 이것은 어차피 성능상의 이유로 일반적인 설정임. 또한 실행 중인 postmaster에 대한 정보를 기록하는 `postmaster.pid` 및 `postmaster.opts`도 제외 가능. 이 백업을 결국 사용할 postmaster가 아니기 때문. (이러한 파일은 pg_ctl을 혼란스럽게 할 수 있음.)

프라이머리에 존재하는 복제 슬롯이 백업의 일부가 되지 않도록 클러스터의 `pg_replslot/` 디렉토리 내의 파일도 백업에서 제외할 것을 권장. 그렇지 않으면 백업을 사용하여 스탠바이를 생성하면 스탠바이에서 WAL 파일이 무기한 보존될 수 있으며 핫 스탠바이 피드백이 활성화된 경우 프라이머리에서 팽창이 발생할 수 있음. 이러한 복제 슬롯을 사용하는 클라이언트가 여전히 스탠바이가 아닌 프라이머리에 연결하고 슬롯을 업데이트하기 때문. 백업이 새 프라이머리 생성에만 사용되는 경우에도 해당 슬롯의 내용은 새 프라이머리가 온라인 상태가 될 때까지 심하게 구식이 될 가능성이 높으므로 복제 슬롯을 복사하는 것은 특히 유용하지 않음.

`pg_dynshmem/`, `pg_notify/`, `pg_serial/`, `pg_snapshots/`, `pg_stat_tmp/` 및 `pg_subtrans/` 디렉토리의 내용(디렉토리 자체는 제외)은 postmaster 시작 시 초기화되므로 백업에서 생략 가능.

`pgsql_tmp`로 시작하는 모든 파일이나 디렉토리는 백업에서 생략 가능. 이러한 파일은 postmaster 시작 시 제거되고 필요에 따라 디렉토리가 다시 생성됨.

`pg_internal.init` 파일은 해당 이름의 파일이 발견될 때마다 백업에서 생략 가능. 이러한 파일에는 복구 시 항상 다시 빌드되는 관계 캐시 데이터가 포함됨.

백업 레이블 파일에는 `pg_backup_start`에 지정한 레이블 문자열과 `pg_backup_start`가 실행된 시간 및 시작 WAL 파일의 이름이 포함됨. 혼란이 있는 경우 백업 파일 내부를 살펴보고 덤프 파일이 어떤 백업 세션에서 온 것인지 정확히 확인 가능. 테이블스페이스 맵 파일에는 `pg_tblspc/` 디렉토리에 존재하는 심볼릭 링크 이름과 각 심볼릭 링크의 전체 경로가 포함됨. 이러한 파일은 단순히 정보용이 아니라 시스템의 복구 프로세스가 올바르게 작동하는 데 필수적임.

서버가 중지된 상태에서 백업을 수행하는 것도 가능함. 이 경우 당연히 `pg_backup_start` 또는 `pg_backup_stop` 사용은 불가하며 따라서 어떤 백업이 무엇인지 그리고 연관된 WAL 파일이 얼마나 뒤로 가는지 추적하는 것은 사용자의 몫임. 일반적으로 위의 연속 아카이빙 절차를 따를 것을 권장.

---

#### 25.3.5. 연속 아카이브 백업을 사용한 복구

최악의 상황이 발생해 백업에서 복구해야 하는 경우 다음 절차를 따름.

1. 서버가 실행 중이면 중지함.

2. 공간이 있다면, 나중에 필요할 경우를 대비하여 전체 클러스터 데이터 디렉토리와 테이블스페이스를 임시 위치에 복사함. 이 예방 조치를 위해서는 시스템에 기존 데이터베이스의 두 복사본을 보관할 충분한 여유 공간이 필요함. 충분한 공간이 없으면 시스템이 다운되기 전에 아카이브되지 않은 WAL 파일을 포함할 수 있으므로 최소한 클러스터의 `pg_wal` 하위 디렉토리의 내용은 저장해야 함.

3. 클러스터 데이터 디렉토리 아래의 모든 기존 파일과 하위 디렉토리 및 사용 중인 테이블스페이스의 루트 디렉토리 아래의 모든 파일과 하위 디렉토리를 제거함.

4. 전체 백업을 복원하는 경우 데이터베이스 파일을 대상 디렉토리로 직접 복원 가능. 올바른 소유권(`root`가 아닌 데이터베이스 시스템 사용자!)과 올바른 권한으로 복원되었는지 확인 필요. 테이블스페이스를 사용하는 경우 `pg_tblspc/`의 심볼릭 링크가 올바르게 복원되었는지 확인 필요.

5. 증분 백업을 복원하는 경우 복원을 수행하는 머신으로 증분 백업과 직접 또는 간접적으로 의존하는 모든 이전 백업을 복원해야 함. 이러한 백업은 실행 중인 서버가 결국 위치할 대상 디렉토리가 아닌 별도의 디렉토리에 배치해야 함. 이 작업이 완료되면 [pg_combinebackup](app-pgcombinebackup.html "pg_combinebackup")을 사용하여 전체 백업과 모든 후속 증분 백업에서 데이터를 가져와 대상 디렉토리에 합성 전체 백업을 작성함. 위와 같이 권한과 테이블스페이스 링크가 올바른지 확인함.

6. `pg_wal/`에 있는 모든 파일을 제거함; 이것들은 파일 시스템 백업에서 가져온 것이므로 현재보다는 아마도 구식일 것임. `pg_wal/`을 전혀 아카이브하지 않았다면 적절한 권한으로 다시 생성함. 이전에 심볼릭 링크로 설정했다면 다시 심볼릭 링크로 설정하도록 주의함.

7. 2단계에서 저장한 아카이브되지 않은 WAL 세그먼트 파일이 있으면 `pg_wal/`에 복사함. (문제가 발생하여 다시 시작해야 하는 경우 수정되지 않은 파일이 남도록 이동보다는 복사가 나음.)

8. `postgresql.conf`에 복구 구성 설정을 하고 ([섹션 19.5.5](runtime-config-wal.html#RUNTIME-CONFIG-WAL-ARCHIVE-RECOVERY "19.5.5. 아카이브 복구") 참조) 클러스터 데이터 디렉토리에 `recovery.signal` 파일을 생성함. 복구가 성공했는지 확신할 때까지 일반 사용자가 연결하지 못하도록 `pg_hba.conf`를 일시적으로 수정할 수도 있음.

9. 서버를 시작함. 서버는 복구 모드로 들어가 필요한 아카이브된 WAL 파일을 읽기 시작함. 외부 오류로 인해 복구가 종료되면 서버를 다시 시작하기만 하면 복구가 계속됨. 복구 프로세스가 완료되면 서버는 `recovery.signal`을 제거하고 (나중에 실수로 복구 모드로 다시 들어가는 것을 방지) 정상 데이터베이스 작업을 시작함.

10. 데이터베이스의 내용을 검사하여 원하는 상태로 복구되었는지 확인함. 그렇지 않으면 1단계로 돌아감. 모든 것이 정상이면 `pg_hba.conf`를 정상으로 복원하여 사용자가 연결할 수 있도록 함.

##### 복구 구성

이 모든 것의 핵심 부분은 복구 방법과 복구가 얼마나 진행되어야 하는지 설명하는 복구 구성을 설정하는 것임. 반드시 지정해야 하는 것은 `restore_command`로, PostgreSQL에게 아카이브된 WAL 파일 세그먼트를 검색하는 방법을 알려줌. `archive_command`와 마찬가지로 이것은 셸 명령 문자열임. `%f`는 원하는 WAL 파일의 이름으로 대체되고 `%p`는 WAL 파일을 복사할 경로 이름으로 대체됨. (경로 이름은 현재 작업 디렉토리, 즉 클러스터의 데이터 디렉토리를 기준으로 함.) 명령에 실제 `%` 문자를 포함해야 하는 경우 `%%`를 작성함. 가장 간단한 유용한 명령은 다음과 같음.

```bash
restore_command = 'cp /mnt/server/archivedir/%f %p'
```

이것은 `/mnt/server/archivedir` 디렉토리에서 이전에 아카이브된 WAL 세그먼트를 복사함. 물론 훨씬 더 복잡한 것을 사용 가능하며 운영자에게 적절한 테이프를 마운트하도록 요청하는 셸 스크립트일 수도 있음.

명령이 실패 시 0이 아닌 종료 상태를 반환하는 것이 중요함. 명령은 아카이브에 없는 파일을 요청하면서 _호출될 것_임; 그렇게 요청받으면 0이 아닌 값을 반환해야 함. 이것은 오류 조건이 아님. 예외적으로 명령이 신호(데이터베이스 서버 종료의 일부로 사용되는 SIGTERM 제외)에 의해 종료되거나 셸에 의한 오류(예: 명령을 찾을 수 없음)가 발생하면 복구가 중단되고 서버가 시작되지 않음.

요청된 파일 중 일부는 WAL 세그먼트 파일이 아님; `.history` 접미사가 있는 파일에 대한 요청도 예상해야 함. 또한 `%p` 경로의 기본 이름이 `%f`와 다를 것이라는 점에 유의. 서로 바꿔서 사용할 수 있다고 기대하는 것은 금지.

아카이브에서 찾을 수 없는 WAL 세그먼트는 `pg_wal/`에서 검색됨; 이를 통해 최근에 아카이브되지 않은 세그먼트 사용 가능. 그러나 아카이브에서 사용할 수 있는 세그먼트는 `pg_wal/`의 파일보다 우선적으로 사용됨.

일반적으로 복구는 사용 가능한 모든 WAL 세그먼트를 통해 진행되어 데이터베이스를 현재 시점(또는 사용 가능한 WAL 세그먼트가 허용하는 한 가까운 시점)으로 복원함. 따라서 정상적인 복구는 "파일을 찾을 수 없음" 메시지로 끝나며 `restore_command` 선택에 따라 정확한 오류 메시지 텍스트가 달라짐. 복구 시작 시 `00000001.history`와 같은 이름의 파일에 대한 오류 메시지도 볼 수 있음. 이것도 정상이며 단순한 복구 상황에서 문제를 나타내지 않음; 논의는 [섹션 25.3.6](continuous-archiving.html#BACKUP-TIMELINES "25.3.6. 타임라인") 참조.

이전 시점으로 복구하려면 (예: 후배 DBA가 메인 트랜잭션 테이블을 삭제하기 직전) 필요한 [중지 지점](runtime-config-wal.html#RUNTIME-CONFIG-WAL-RECOVERY-TARGET "19.5.6. 복구 대상")을 지정하면 됨. "복구 대상"으로 알려진 중지 지점을 날짜/시간, 명명된 복원 지점 또는 특정 트랜잭션 ID의 완료로 지정 가능. 어떤 트랜잭션 ID를 사용해야 하는지 정확히 식별하는 데 도움이 되는 도구가 없으므로, 실질적으로 날짜/시간 또는 명명된 복원 지점 옵션 사용이 일반적임.

#### 참고

중지 지점은 베이스 백업의 종료 시간 이후여야 함. 즉, `pg_backup_stop`의 종료 시간임. 베이스 백업을 사용하여 해당 백업이 진행 중이던 시간으로 복구하는 것은 불가. (그러한 시간으로 복구하려면 이전 베이스 백업으로 돌아가서 거기에서 앞으로 롤링해야 함.)

복구가 손상된 WAL 데이터를 발견하면 해당 지점에서 복구가 중단되고 서버가 시작되지 않음. 이 경우 복구가 정상적으로 완료될 수 있도록 손상 지점 이전의 "복구 대상"을 지정하여 복구 프로세스를 처음부터 다시 실행 가능. 시스템 충돌이나 WAL 아카이브에 액세스할 수 없게 되는 것과 같은 외부 이유로 복구가 실패하면 복구를 다시 시작하기만 하면 실패한 곳에서 거의 다시 시작됨. 복구 다시 시작은 정상 작업에서의 체크포인팅과 매우 유사하게 작동함: 서버는 주기적으로 모든 상태를 디스크에 강제하고 `pg_control` 파일을 업데이트하여 이미 처리된 WAL 데이터를 다시 스캔할 필요가 없음을 나타냄.

---

#### 25.3.6. 타임라인

데이터베이스를 이전 시점으로 복원하는 기능은 시간 여행이나 평행 우주처럼 몇 가지 복잡한 상황을 만들어냄. 예를 들어 데이터베이스의 원래 이력에서 화요일 저녁 5시 15분에 중요한 테이블을 삭제했지만 수요일 정오까지 실수를 깨닫지 못했다고 가정함. 당황하지 않고 백업을 꺼내 화요일 저녁 5시 14분 시점으로 복원하고 다시 가동함. 데이터베이스 우주의 _이_ 이력에서는 테이블을 삭제하지 않음. 그러나 나중에 이것이 그다지 좋은 생각이 아니었음을 깨닫고 원래 이력의 수요일 아침 어느 시점으로 돌아가고 싶다고 가정함. 데이터베이스가 가동되는 동안 지금 돌아가고 싶은 시간까지 이어지는 일부 WAL 세그먼트 파일을 덮어썼다면 돌아갈 수 없음. 따라서 이를 피하려면 특정 시점 복구 후 생성된 WAL 레코드 시리즈를 원래 데이터베이스 이력에서 생성된 것과 구별해야 함.

이 문제를 처리하기 위해 PostgreSQL에는 _타임라인_이라는 개념이 있음. 아카이브 복구가 완료될 때마다 해당 복구 후 생성된 WAL 레코드 시리즈를 식별하기 위해 새 타임라인이 생성됨. 타임라인 ID 번호는 WAL 세그먼트 파일 이름의 일부이므로 새 타임라인은 이전 타임라인에서 생성된 WAL 데이터를 덮어쓰지 않음. 예를 들어 WAL 파일 이름 `0000000100001234000055CD`에서 앞의 `00000001`은 16진수의 타임라인 ID임. (서버 로그 메시지와 같은 다른 컨텍스트에서는 타임라인 ID가 일반적으로 10진수로 인쇄됨.)

실제로 많은 다른 타임라인을 아카이브할 수 있음. 이것이 쓸모없는 기능처럼 보일 수 있지만 종종 생명의 은인이 됨. 어느 시점으로 복구해야 하는지 정확히 확실하지 않아 여러 번 시행착오로 특정 시점 복구를 수행하여 이전 이력에서 분기할 최적의 위치를 찾아야 하는 상황을 고려함. 타임라인이 없으면 이 프로세스는 곧 관리할 수 없는 혼란을 만들어냄. 타임라인을 사용하면 이전에 포기한 타임라인 분기의 상태를 포함하여 _이전의 모든_ 상태로 복구 가능.

새 타임라인이 생성될 때마다 PostgreSQL은 어떤 타임라인에서 분기되었는지와 언제 분기되었는지를 보여주는 "타임라인 이력" 파일을 생성함. 이러한 이력 파일은 여러 타임라인이 포함된 아카이브에서 복구할 때 시스템이 올바른 WAL 세그먼트 파일을 선택할 수 있도록 하는 데 필요함. 따라서 WAL 세그먼트 파일과 마찬가지로 WAL 아카이브 영역에 아카이브됨. 이력 파일은 작은 텍스트 파일에 불과하므로 (크기가 큰 세그먼트 파일과 달리) 무기한 보관하는 것이 저렴하고 적절함. 원하는 경우 이력 파일에 특정 타임라인이 어떻게 그리고 왜 생성되었는지에 대한 자신의 메모를 기록하는 주석 추가 가능. 이러한 주석은 실험의 결과로 서로 다른 타임라인의 덤불이 있을 때 특히 유용함.

복구의 기본 동작은 아카이브에서 찾은 최신 타임라인으로 복구하는 것임. 베이스 백업이 수행되었을 때 현재였던 타임라인이나 특정 자식 타임라인으로 복구하려면 (즉, 복구 시도 후 자체적으로 생성된 일부 상태로 돌아가려면) [recovery_target_timeline](runtime-config-wal.html#GUC-RECOVERY-TARGET-TIMELINE)에 `current` 또는 대상 타임라인 ID를 지정해야 함. 베이스 백업보다 더 일찍 분기된 타임라인으로는 복구 불가.

---

#### 25.3.7. 팁과 예제

연속 아카이빙 구성에 대한 몇 가지 팁을 소개함.

##### 25.3.7.1. 독립형 핫 백업

PostgreSQL의 백업 기능을 사용하여 독립형 핫 백업 생성 가능. 이러한 백업은 특정 시점 복구에 사용할 수 없지만 일반적으로 pg_dump 덤프보다 백업 및 복원이 훨씬 빠름. (또한 pg_dump 덤프보다 훨씬 크므로 일부 경우 속도 이점이 상쇄될 수 있음.)

베이스 백업과 마찬가지로 독립형 핫 백업을 생성하는 가장 쉬운 방법은 [pg_basebackup](app-pgbasebackup.html "pg_basebackup") 도구 사용임. 호출할 때 `-X` 매개변수를 포함하면 백업을 사용하는 데 필요한 모든 write-ahead 로그가 백업에 자동으로 포함되며 백업을 복원하기 위해 특별한 조치가 필요 없음.

##### 25.3.7.2. 압축된 아카이브 로그

아카이브 스토리지 크기가 우려되는 경우 gzip을 사용하여 아카이브 파일 압축 가능.

```bash
archive_command = 'gzip < %p > /mnt/server/archivedir/%f.gz'
```

그런 다음 복구 중에 gunzip을 사용해야 함.

```bash
restore_command = 'gunzip < /mnt/server/archivedir/%f.gz > %p'
```

##### 25.3.7.3. `archive_command` 스크립트

많은 사람들이 `archive_command`를 정의하기 위해 스크립트를 사용하므로 `postgresql.conf` 항목이 매우 간단해 보임.

```bash
archive_command = 'local_backup_script.sh "%p" "%f"'
```

아카이빙 프로세스에서 둘 이상의 명령을 사용하려는 경우 별도의 스크립트 파일을 사용할 것을 권장. 이를 통해 bash나 perl과 같은 인기 있는 스크립팅 언어로 작성된 스크립트 내에서 모든 복잡성 관리 가능.

스크립트 내에서 해결될 수 있는 요구 사항의 예:

- 안전한 원격 데이터 스토리지에 데이터 복사
- 한 번에 하나씩이 아닌 3시간마다 전송되도록 WAL 파일 일괄 처리
- 다른 백업 및 복구 소프트웨어와 인터페이스
- 오류를 보고하기 위해 모니터링 소프트웨어와 인터페이스

##### 팁

`archive_command` 스크립트를 사용할 때 [logging_collector](runtime-config-logging.html#GUC-LOGGING-COLLECTOR)를 활성화할 것을 권장. 그러면 스크립트에서 stderr에 작성된 모든 메시지가 데이터베이스 서버 로그에 나타나므로 복잡한 구성이 실패할 경우 쉽게 진단 가능.

---

#### 25.3.8. 주의사항

연속 아카이빙 기술에는 다음과 같은 제한 사항이 있으며, 향후 릴리스에서 개선될 수 있음.

- 베이스 백업이 수행되는 동안 [`CREATE DATABASE`](sql-createdatabase.html "CREATE DATABASE") 명령이 실행되고 `CREATE DATABASE`가 복사한 템플릿 데이터베이스가 베이스 백업이 아직 진행 중인 동안 수정되면 해당 수정 사항이 생성된 데이터베이스에도 전파될 수 있음. 이것은 물론 바람직하지 않음. 이 위험을 피하려면 베이스 백업을 수행하는 동안 템플릿 데이터베이스를 수정하지 않을 것을 권장.

- [`CREATE TABLESPACE`](sql-createtablespace.html "CREATE TABLESPACE") 명령은 리터럴 절대 경로로 WAL 로깅되므로 동일한 절대 경로로 테이블스페이스 생성으로 재생됨. WAL이 다른 머신에서 재생되는 경우 이것은 바람직하지 않을 수 있음. WAL이 동일한 머신에서 재생되지만 새 데이터 디렉토리로 재생되는 경우에도 위험할 수 있음: 재생이 여전히 원래 테이블스페이스의 내용을 덮어씀. 이러한 종류의 잠재적 문제를 피하기 위한 가장 좋은 방법은 테이블스페이스를 생성하거나 삭제한 후 새 베이스 백업을 수행하는 것임.

기본 WAL 형식은 많은 디스크 페이지 스냅샷을 포함하므로 상당히 방대함. 이러한 페이지 스냅샷은 부분적으로 작성된 디스크 페이지를 수정해야 할 수 있으므로 충돌 복구를 지원하도록 설계됨. 시스템 하드웨어 및 소프트웨어에 따라 부분 쓰기의 위험이 무시할 수 있을 정도로 작을 수 있으며, 이 경우 [full_page_writes](runtime-config-wal.html#GUC-FULL-PAGE-WRITES) 매개변수를 사용하여 페이지 스냅샷을 끄면 아카이브된 WAL 파일의 총 볼륨을 크게 줄일 수 있음. (그렇게 하기 전에 [28장](wal.html "28장. 신뢰성 및 Write-Ahead 로그")의 참고 사항과 경고를 읽을 것.) 페이지 스냅샷을 끄는 것은 PITR 작업에 WAL 사용을 방해하지 않음. 향후 개발 영역은 `full_page_writes`가 켜져 있는 경우에도 불필요한 페이지 복사를 제거하여 아카이브된 WAL 데이터를 압축하는 것임. 그동안 관리자는 체크포인트 간격 매개변수를 가능한 한 늘려 WAL에 포함된 페이지 스냅샷 수를 줄일 수 있음.

---
## Chapter 26. 고가용성, 로드 밸런싱 및 복제

PostgreSQL 18 공식 문서 - High Availability, Load Balancing, and Replication 번역

---

### 개요

데이터베이스 서버는 함께 작동하도록 구성 가능 → 특정 서버가 장애를 겪어도 다른 서버가 작업을 인계받아 여러 서버가 동일한 데이터를 제공 가능. 로드 밸런싱을 통해 여러 서버가 동일한 데이터를 제공하도록 구성하는 것도 가능. 이상적으로는 데이터베이스 서버들이 원활하게 협력해야 함. 정적 웹 페이지를 제공하는 웹 서버는 각 서버에 페이지를 한 번만 배치하면 되므로 쉽게 묶어서 운영 가능. 읽기 전용 데이터베이스 서버도 마찬가지로 쉽게 구성 가능. 그러나 대부분의 데이터베이스 서버는 읽기/쓰기 요청이 혼재 · 읽기/쓰기 서버는 모든 서버 간 일관성 유지를 위해 쓰기를 조정해야 하므로 훨씬 어려움.

이 동기화 문제는 데이터베이스 서버 협력의 근본적인 난제임. 모든 사용 사례에서 쓰기 동기화 문제를 해결하는 단일 솔루션은 존재하지 않으므로 다양한 솔루션 제공. 각 솔루션은 특정 워크로드에 적합한 방식으로 이 문제를 다루며, 각각 특정 영역에서의 성능 저하를 최소화함.

#### 주요 용어

- Primary/Master: 데이터를 수정할 수 있는 읽기/쓰기 서버
- Standby/Secondary: Primary의 변경 사항을 추적하는 서버
  - Warm Standby: 승격(Promoted)될 때까지 연결 불가
  - Hot Standby: 읽기 전용 쿼리 수용 가능

#### 동기화 방식

- 동기(Synchronous): 모든 서버가 커밋할 때까지 대기 · 데이터 손실 없음
- 비동기(Asynchronous): 지연 허용 · 성능 우수하나 데이터 손실 가능

일부 솔루션은 서버 간 통신이 필요한 동기식 솔루션을 사용하는 반면, 다른 솔루션은 비동기식으로 서버 간 동기화가 느슨하게 연결되어 자연스러운 지연이 발생함.

---

### 26.1. 다양한 솔루션 비교

#### 1. 공유 디스크 장애조치 (Shared Disk Failover)

공유 디스크 장애조치는 여러 서버가 공유하는 단일 디스크 배열을 사용해 단일 복사본만 유지 → 동기화 오버헤드 회피. 주 데이터베이스 서버가 실패하면 대기 서버가 마치 데이터베이스 충돌 복구 후처럼 데이터베이스를 마운트하고 시작 가능 → 빠른 장애조치 가능 · 데이터 손실 없음.

특징:
- 데이터베이스 사본 1개만 유지
- 주 서버 실패 시 대기 서버가 디스크 마운트
- 빠른 장애조치, 데이터 손실 없음

단점:
- 공유 디스크 배열 장애 시 모두 영향
- 대기 서버가 공유 스토리지에 동시에 접근 불가

공유 하드웨어 기능은 네트워크 스토리지 장치에서 일반적임. 주의할 점은 대기 서버가 공유 스토리지에 접근하면 안 된다는 것 → 접근 시 데이터 손상 가능.

#### 2. 파일 시스템(블록 장치) 복제 (File System/Block Device Replication)

공유 하드웨어 기능의 대안은 파일 시스템 복제 사용 → 모든 파일 시스템 변경 사항을 다른 컴퓨터에 미러링. 유일한 제한 사항은 대기 서버에서 주 서버와 동일한 순서로 미러링해야 한다는 것.

특징:
- 파일 시스템 변경사항을 다른 컴퓨터에 미러링
- 미러링 순서 일치 필수

예시:
- DRBD (Linux용 파일 시스템 복제 솔루션)

#### 3. Write-Ahead Log 배송 (Write-Ahead Log Shipping)

웜 및 핫 대기 서버는 WAL(Write-Ahead Log) 레코드 스트림을 읽어 최신 상태 유지 가능. 주 서버가 실패하면 대기 서버에 주 서버의 거의 모든 데이터가 포함되어 새로운 주 서버로 전환 가능. 동기식 또는 비동기식으로 수행 가능하며 전체 데이터베이스 서버에 대해서만 수행 가능.

방식:
- WAL 레코드 스트림 읽기로 대기 서버 유지
- 동기/비동기 모두 가능
- 전체 데이터베이스에만 적용 가능

구현:
- 파일 기반 로그 배송 (26.2절)
- 스트리밍 복제 (26.2.5절)
- 두 가지 조합 사용 가능

대기 서버는 파일 기반 로그 배송(26.2 참고) · 스트리밍 복제(26.2.5 참고) 또는 두 가지의 조합으로 구현 가능.

핫 대기 관련 정보는 26.4 참고.

#### 4. 논리적 복제 (Logical Replication)

논리적 복제를 사용하면 데이터베이스 서버가 데이터 변경 사항의 논리적 스트림을 다른 서버로 전송 가능. PostgreSQL 논리적 복제는 WAL에서 논리적 데이터 수정 스트림을 구성함. 논리적 복제는 테이블 단위로 데이터 복제 가능. 또한 변경 사항을 보내는 서버(발행 서버)가 다른 서버의 변경 사항을 구독할 수도 있어 양방향 복제 가능.

특징:
- 테이블별 단위로 복제 가능
- WAL에서 논리적 데이터 수정 스트림 구성
- 양방향 복제 가능 (발행 서버가 다른 서버의 변경사항 구독 가능)
- Logical decoding 인터페이스(47장)로 타사 확장 지원

#### 5. 트리거 기반 Primary-Standby 복제 (Trigger-Based Primary-Standby Replication)

트리거 기반 복제 설정에서는 일반적으로 주 서버에서 하나의 지정된 대기 서버로 모든 데이터 수정을 비동기적으로 전송함. 대기 서버는 쿼리 응답 가능 · 로컬에서 데이터 변경 가능(예: 분석 데이터) → 대규모 분석 또는 데이터 웨어하우스 쿼리 분산에 유용.

특징:
- 테이블 단위 복제
- 비동기 업데이트
- 대기 서버가 쿼리 응답 가능
- 로컬 데이터 변경 허용

예시:
- Slony-I

단점:
- 장애조치 시 데이터 손실 가능

#### 6. SQL 기반 복제 미들웨어 (SQL-Based Replication Middleware)

SQL 기반 복제 미들웨어 프로그램을 사용하면 프로그램이 모든 SQL 쿼리를 가로채 하나 또는 모든 서버로 전송 가능. 각 서버는 독립적으로 작동함. 읽기-쓰기 쿼리는 모든 서버로 전송되어 모든 서버에서 변경 사항 발생. 반면 읽기 전용 쿼리는 하나의 서버에만 전송되어 읽기 작업 부하 분산 가능.

동작:
- 모든 SQL 쿼리 인터셉트 후 서버로 전송
- 읽기-쓰기 쿼리: 모든 서버 전송
- 읽기 전용 쿼리: 1개 서버만 전송 가능

주의사항:
- `random()`, `CURRENT_TIMESTAMP`, 시퀀스 등 비결정적 함수값 일관성 문제

예시:
- Pgpool-II
- Continuent Tungsten

해결방안:
- 미들웨어/애플리케이션에서 단일 소스 결정
- 2단계 커밋(Two-Phase Commit) 사용

#### 7. 비동기 다중 마스터 복제 (Asynchronous Multimaster Replication)

비동기 다중 마스터 복제는 각 서버가 독립적으로 작동하고 충돌 해결을 위해 주기적으로 통신하는 방식 → 불규칙한 연결이나 느린 통신 링크(예: 노트북, 원격 서버) 환경에 이상적.

특징:
- 불규칙한 연결, 느린 통신 링크 환경에 적합 (노트북, 원격 서버)
- 각 서버 독립 작동
- 주기적 동기화로 충돌 식별
- 충돌 해결 유연성

예시:
- Bucardo

#### 8. 동기 다중 마스터 복제 (Synchronous Multimaster Replication)

동기 다중 마스터 복제에서는 각 서버가 쓰기 요청 수용 가능 · 수정된 데이터는 트랜잭션 커밋 전에 각 서버에서 모든 서버로 전송됨. 과도한 쓰기 활동은 트랜잭션이 어디서든 커밋될 수 있도록 서버 전체에 잠금이 필요하므로 과도한 잠금과 커밋 지연 유발 가능 → 쓰기 성능이 종종 단일 서버보다 나쁨. 읽기 요청은 모든 서버로 분산 가능해 읽기 집약적인 워크로드에 유리.

특징:
- 모든 서버가 쓰기 요청 수용
- 트랜잭션 커밋 전 모든 서버에 전송
- 비결정적 함수 문제 없음

단점:
- 과도한 잠금
- 커밋 지연
- 성능 저하

최적 사용 사례:
- 대부분 읽기 작업인 워크로드

PostgreSQL:
- PostgreSQL은 이 유형의 복제를 제공하지 않음
- 2단계 커밋(PREPARE TRANSACTION 및 COMMIT PREPARED)으로 구현 가능하지만 애플리케이션 수준 코드에서 원자적 커밋 관리 필요

#### 고가용성, 로드 밸런싱 및 복제 기능 매트릭스 (Table 26.1)

- 공유 디스크
  - 인기 예시: NAS
  - 특수 하드웨어 불필요: 미해당
  - 여러 주 서버 허용: 미해당
  - 주 서버 오버헤드 없음: 해당
  - 다중 서버 대기 없음: 해당
  - 주 서버 장애 시 데이터 손실 없음: 해당
  - 복제본 읽기 전용 쿼리: 미해당
  - 테이블 단위 세분성: 미해당
  - 충돌 해결 불필요: 해당
- 파일 시스템 복제
  - 인기 예시: DRBD
  - 특수 하드웨어 불필요: 해당
  - 여러 주 서버 허용: 미해당
  - 주 서버 오버헤드 없음: 미해당
  - 다중 서버 대기 없음: 미해당
  - 주 서버 장애 시 데이터 손실 없음: 해당
  - 복제본 읽기 전용 쿼리: 미해당
  - 테이블 단위 세분성: 미해당
  - 충돌 해결 불필요: 해당
- WAL 로그 배송
  - 인기 예시: 내장 스트리밍
  - 특수 하드웨어 불필요: 해당
  - 여러 주 서버 허용: 미해당
  - 주 서버 오버헤드 없음: 해당
  - 다중 서버 대기 없음: sync off일 때 해당
  - 주 서버 장애 시 데이터 손실 없음: sync on일 때 해당
  - 복제본 읽기 전용 쿼리: hot standby일 때 가능
  - 테이블 단위 세분성: 미해당
  - 충돌 해결 불필요: 해당
- 논리적 복제
  - 인기 예시: 내장 논리적, pglogical
  - 특수 하드웨어 불필요: 해당
  - 여러 주 서버 허용: 해당
  - 주 서버 오버헤드 없음: 해당
  - 다중 서버 대기 없음: sync off일 때 해당
  - 주 서버 장애 시 데이터 손실 없음: sync on일 때 해당
  - 복제본 읽기 전용 쿼리: 해당
  - 테이블 단위 세분성: 해당
  - 충돌 해결 불필요: 미해당
- 트리거 기반
  - 인기 예시: Londiste, Slony
  - 특수 하드웨어 불필요: 해당
  - 여러 주 서버 허용: 미해당
  - 주 서버 오버헤드 없음: 미해당
  - 다중 서버 대기 없음: 해당
  - 주 서버 장애 시 데이터 손실 없음: 미해당
  - 복제본 읽기 전용 쿼리: 해당
  - 테이블 단위 세분성: 해당
  - 충돌 해결 불필요: 해당
- SQL 복제
  - 인기 예시: pgpool-II
  - 특수 하드웨어 불필요: 해당
  - 여러 주 서버 허용: 해당
  - 주 서버 오버헤드 없음: 해당
  - 다중 서버 대기 없음: 미해당
  - 주 서버 장애 시 데이터 손실 없음: 해당
  - 복제본 읽기 전용 쿼리: 해당
  - 테이블 단위 세분성: 미해당
  - 충돌 해결 불필요: 해당
- 비동기 MM
  - 인기 예시: Bucardo
  - 특수 하드웨어 불필요: 해당
  - 여러 주 서버 허용: 해당
  - 주 서버 오버헤드 없음: 미해당
  - 다중 서버 대기 없음: 해당
  - 주 서버 장애 시 데이터 손실 없음: 미해당
  - 복제본 읽기 전용 쿼리: 해당
  - 테이블 단위 세분성: 해당
  - 충돌 해결 불필요: 미해당
- 동기 MM
  - 특수 하드웨어 불필요: 해당
  - 여러 주 서버 허용: 해당
  - 주 서버 오버헤드 없음: 미해당
  - 다중 서버 대기 없음: 미해당
  - 주 서버 장애 시 데이터 손실 없음: 해당
  - 복제본 읽기 전용 쿼리: 해당
  - 테이블 단위 세분성: 해당
  - 충돌 해결 불필요: 해당

#### 범주 외 솔루션

##### 데이터 파티셔닝 (Data Partitioning)

데이터 파티셔닝은 테이블을 데이터셋으로 분할 → 각 세트는 하나의 서버만 수정 가능. 예를 들어 런던 사무실과 파리 사무실에 서버가 있고 각 사무실이 자체 판매 데이터만 수정 가능하며 데이터 상호 조회가 가능한 구성 존재. 이러한 설정은 애플리케이션 수준 코드로 쉽게 구현 가능하며, 분할 기능은 PL/Proxy 도구셋 참고.

##### 다중 서버 병렬 쿼리 실행 (Multiple-Server Parallel Query Execution)

위의 많은 솔루션은 여러 서버가 여러 쿼리를 처리하도록 함. 그러나 어떤 솔루션도 단일 쿼리가 여러 서버를 사용해 더 빠르게 완료되도록 허용하지 않음. 이 솔루션은 여러 서버에 데이터를 분할하고 각 서버가 쿼리의 일부를 실행한 후 결과를 중앙 서버로 반환해 사용자에게 전달 → PL/Proxy 도구셋으로 구현 가능.

PostgreSQL 오픈 소스의 특성으로 인해 여러 상용 솔루션이 존재하며, 독자적인 장애조치 · 복제 · 로드 밸런싱 기능 제공.

---

### 26.2. 로그 배송 대기 서버 (Log-Shipping Standby Servers)

연속 아카이빙(Continuous Archiving)을 사용해 하나 이상의 대기 서버가 있는 고가용성(HA) 클러스터 구성 가능 → 이를 웜 대기(Warm Standby) 또는 로그 배송(Log Shipping)이라고 함.

주 서버와 대기 서버가 함께 작동해 이 높은 수준의 기능을 제공하지만 서버는 느슨하게 결합됨. 주 서버는 연속 아카이빙 모드에서 작동하는 반면, 각 대기 서버는 주 서버에서 WAL 파일을 읽는 연속 복구 모드에서 작동함. 데이터베이스 테이블 변경이 필요하지 않으므로 다른 복제 솔루션 대비 관리 오버헤드가 낮음. 또한 주 서버 성능에 미치는 영향이 상대적으로 적음.

주 서버에서 대기 서버로 WAL 레코드를 직접 전달하는 방식을 일반적으로 로그 배송이라고 함. PostgreSQL은 한 번에 하나의 WAL 파일(WAL 세그먼트) 단위로 전송하는 파일 기반 로그 배송을 구현함. WAL 파일(16MB)은 로컬 또는 원격으로 쉽고 효율적으로 전송 가능(예: scp, rsync, ftp). 스트리밍 복제를 사용하면 레코드 단위 로그 배송도 가능.

로그 배송은 비동기식이므로 WAL 레코드는 트랜잭션 커밋 이후에 배송됨. 따라서 주 서버가 심각한 장애를 겪으면 아직 대기 서버에 복제되지 않은 커밋된 트랜잭션으로 인해 데이터 손실 가능. 파일 기반 로그 배송에서는 `archive_timeout`으로 이 데이터 손실 구간을 제한 가능하며 몇 초 단위로 설정도 가능하나, 낮게 설정하면 주 서버의 네트워크 대역폭 사용량이 크게 증가함. 스트리밍 복제(26.2.5 참고)는 훨씬 작은 데이터 손실 구간 제공.

복구 성능이 충분히 빠르므로 대기 서버는 일반적으로 활성화 후 짧은 시간 안에 완전히 운영 가능한 상태가 됨 → 높은 가용성을 제공하는 웜 대기 구성이라고 함. 아카이브된 베이스 백업에서 서버를 복원하고 롤포워드하는 작업은 시간이 오래 걸리므로 이 기술은 고가용성보다는 재해 복구 용도에 적합. 대기 서버는 재해 복구 · 데이터 보호 또는 두 목적 모두에 활용 가능.

#### 26.2.1. 계획 (Planning)

일반적으로 주 서버와 대기 서버를 가능한 한 비슷하게 설계하는 것이 바람직함. 특히 테이블스페이스 관련 경로 이름은 동일한 경로로 전달되어야 함 → 주 서버에서 `CREATE TABLESPACE`를 실행하면 대기 서버의 복구 시작 전에 필요한 새 마운트 포인트가 생성되어 있어야 함. 주 서버에서 다른 테이블스페이스 레이아웃을 사용하려면 복구 중 `tablespace_map` 파일을 수정하거나, 테이블스페이스를 사용하지 않고 대기 서버의 테이블스페이스 레이아웃을 조정하는 방법도 가능.

하드웨어가 꼭 동일할 필요는 없으나, 경험상 아키텍처 간 차이를 마이그레이션하는 것은 지원되지 않음 → 32비트에서 64비트로 또는 그 반대로 이동은 불가. 동일한 메이저 버전에서 다른 마이너 버전을 실행하는 것은 일반적으로 작동하나 공식 지원 대상은 아님. 릴리스 노트 정독 필요.

PostgreSQL 버전 호환성:
- 다른 메이저 버전 간 로그 배송은 불가능
- 다른 마이너 버전 간 가능 (공식 지원 없음)
- 권장: 주 서버와 대기 서버를 동일 버전으로 유지
- 업그레이드 전략: 대기 서버를 먼저 업그레이드

#### 26.2.2. 대기 서버 운영 (Standby Server Operation)

대기 모드에서 서버는 아카이브된 위치에서 WAL을 지속적으로 적용하거나, 주 서버 연결을 통해 직접 적용함.

대기 서버 시작 시 `standby.signal` 파일이 데이터 디렉토리에 존재하면 대기 모드로 진입함. 대기 서버에서 `restore_command` 설정 필요하며, `archive_cleanup_command`로 더 이상 필요 없는 WAL 세그먼트 파일 제거 가능. `pg_archivecleanup` 유틸리티는 이 목적으로 설계되어 일반적인 단일 대기 구성에서 `archive_cleanup_command`로 사용 가능.

##### WAL 복구 순서 (우선순위)

1. `restore_command`를 통한 WAL 아카이브 복구
2. `pg_wal` 디렉토리의 WAL 복구
3. 스트리밍 복제를 통한 주 서버 연결
4. 위의 모든 방법 재시도 (루프)

대기 모드는 `pg_ctl stop`으로 종료하거나 서버가 장애조치를 요청받을 때까지 유지됨. 대기 모드 종료 전 아카이브 또는 `pg_wal`에서 즉시 재생 가능한 WAL은 완전히 재생하나, 주 서버 재연결 시도는 없음.

##### 대기 서버 승격 (Promotion)

```bash
# 대기 모드 종료 및 일반 모드로 변환:
pg_ctl promote

# 또는 SQL 함수:
SELECT pg_promote();
```

#### 26.2.3. 대기 서버를 위한 주 서버 준비 (Preparing the Primary for Standby Servers)

연속 아카이빙을 25.3.1(참고)에 설명된 대로 주 서버에 설정함. 아카이브 디렉토리는 주 서버가 다운되어도 대기 서버에서 접근 가능해야 함 → 즉 주 서버가 아닌 대기 서버 자체 또는 다른 신뢰할 수 있는 서버에 위치해야 함.

대기 서버 연결을 위해 스트리밍 복제를 설정하려면(26.2.5 참고) `max_wal_senders`를 필요한 대기 서버 수를 지원하기에 충분한 값으로 설정 필요. 복제 슬롯을 사용하려면 적어도 슬롯 수만큼 `max_replication_slots`가 설정되어 있는지 확인 필요.

##### 주 서버 설정 예제

```ini
# postgresql.conf (Primary)

# 복제 연결을 위한 설정
max_wal_senders = 10  # 대기 서버 수 이상으로 설정
max_replication_slots = 10  # 복제 슬롯 사용 시 설정
```

```
# pg_hba.conf (Primary)
# TYPE  DATABASE        USER            ADDRESS            METHOD
host    replication     foo             192.168.1.100/32   md5
```

#### 26.2.4. 대기 서버 설정 (Setting Up a Standby Server)

대기 서버를 설정하려면 주 서버에서 베이스 백업을 복원함(25.3.5 참고). 대기 서버의 클러스터 데이터 디렉토리에 `standby.signal` 파일을 만들고, `restore_command`를 아카이브된 WAL을 검색하는 간단한 명령으로 설정.

##### 1. 베이스 백업 복구

```bash
# 주 서버에서 생성한 베이스 백업을 대기 서버로 복구
# (Section 25.3.5 참조)
```

##### 2. standby.signal 파일 생성

```bash
# 대기 서버의 클러스터 데이터 디렉토리에 생성
touch /path/to/data/directory/standby.signal
```

##### 3. 기본 설정

```ini
# postgresql.conf (Standby)

# WAL 복구 명령
restore_command = 'cp /path/to/archive/%f %p'

# 스트리밍 복제 설정 (선택사항)
primary_conninfo = 'host=192.168.1.50 port=5432 user=foo password=foopass options=''-c wal_sender_timeout=5000'''

# 타임라인 설정 (다중 대기 HA의 경우)
recovery_target_timeline = 'latest'  # 기본값

# WAL 아카이브 정리 (선택사항)
archive_cleanup_command = 'pg_archivecleanup /path/to/archive %r'
```

##### 4. 완전 설정 예제

```ini
# 대기 서버의 postgresql.conf
primary_conninfo = 'host=192.168.1.50 port=5432 user=foo password=foopass options=''-c wal_sender_timeout=5000'''
restore_command = 'cp /path/to/archive/%f %p'
archive_cleanup_command = 'pg_archivecleanup /path/to/archive %r'
```

##### 5. HA용 추가 설정

대기 서버가 장애조치 후 새로운 주 서버가 되는 경우, 주 서버와 동일하게 WAL 아카이빙 · 연결 · 인증 설정 필요.

#### 26.2.5. 스트리밍 복제 (Streaming Replication)

스트리밍 복제를 사용하면 대기 서버가 파일 기반 로그 배송 사용 시보다 더 최신 상태 유지 가능. 대기 서버는 주 서버에 연결하고, 주 서버는 생성 즉시 WAL 레코드를 대기 서버로 스트리밍 → WAL 파일이 채워지기를 기다리지 않음.

스트리밍 복제는 기본적으로 비동기식(26.2.8 참고)이며, 주 서버에서 트랜잭션이 커밋된 후 대기 서버에 변경 사항이 나타나기까지 작은 지연 존재. 다만 이 지연은 파일 기반 로그 배송보다 훨씬 작아 대기 서버가 로드를 유지할 수 있다면 일반적으로 1초 미만. 스트리밍 복제에서는 `archive_timeout`이 데이터 손실 창 크기 축소에 불필요.

##### 스트리밍 복제 특징

- 대기 서버가 주 서버에 연결
- 주 서버는 생성되는 즉시 WAL 레코드 스트림
- WAL 파일이 완성될 때까지 대기하지 않음
- 기본값: 비동기 모드
- 지연 시간: 일반적으로 1초 이내 (대기 서버가 부하를 감당할 수 있을 때)

##### 필수 설정 단계

```ini
# 1. 파일 기반 로그 배송 대기 서버를 먼저 설정 (26.2.4 참조)

# 2. primary_conninfo 설정으로 스트리밍 복제 활성화
primary_conninfo = 'host=192.168.1.50 port=5432 user=foo password=foopass'

# 3. 주 서버에서 listen_addresses 설정
listen_addresses = '*'  # 또는 대기 서버 IP

# 4. 주 서버에서 max_wal_senders 충분히 설정
max_wal_senders = 10
```

스트리밍 복제만 사용하는 경우(WAL 아카이브 없이) 대기 서버가 다시 연결할 때까지 어떤 WAL 세그먼트가 필요할지 알 수 없음. `wal_keep_size`를 지정하면 이전 WAL 세그먼트가 너무 빨리 재활용되지 않도록 방지 가능. 또는 복제 슬롯(26.2.6 참고)을 설정해 필요한 모든 세그먼트를 유지하는 방법도 가능.

##### WAL 세그먼트 재활용 관리

```ini
# 옵션 1: wal_keep_size 사용
wal_keep_size = 1GB

# 옵션 2: 복제 슬롯 사용 (권장)
# 26.2.6 참조

# 옵션 3: 아카이브 사용
archive_command = '...'
```

##### 대기 서버 연결 확인

대기 서버 시작 후:
- 대기 서버에서 `walreceiver` 프로세스 확인
- 주 서버에서 `walsender` 프로세스 확인

##### 26.2.5.1. 인증 (Authentication)

클라이언트 인증 설정 시 `replication` 권한을 부여해 대기 서버가 `walsender`에 연결하도록 하는 것이 중요함. `pg_hba.conf`의 데이터베이스 필드에 `replication`을 지정해 이 접근을 설정함. 일반 사용자가 `walsender`에 연결하지 못하도록 막으려면 `REPLICATION` 권한을 가진 전용 사용자를 만들어 해당 사용자만 허용 권장.

```
# pg_hba.conf (Primary)
# TYPE  DATABASE        USER            ADDRESS                 METHOD
host    replication     foo             192.168.1.100/32        md5
```

예제 설명:
- 대기 서버 IP: 192.168.1.100
- 복제 계정: foo
- 인증 방식: md5

```ini
# postgresql.conf (Standby)
# 주 서버 연결 정보
primary_conninfo = 'host=192.168.1.50 port=5432 user=foo password=foopass'
```

##### ~/.pgpass 파일 사용

```
# ~/.pgpass (Standby)
192.168.1.50:5432:replication:foo:foopass
```

```bash
chmod 600 ~/.pgpass
```

##### 복제 사용자 생성

```sql
-- 주 서버에서 복제 사용자 생성
CREATE ROLE foo WITH LOGIN REPLICATION PASSWORD 'foopass';
```

##### 권한 설명

- `REPLICATION`: 매우 높은 권한 · 데이터 수정 불가
- `SUPERUSER`: `REPLICATION`보다 높은 권한 · 데이터 수정 가능
- `LOGIN`: 로그인 권한

##### 26.2.5.2. 모니터링 (Monitoring)

스트리밍 복제에서 중요한 것은 주 서버에서 복제 지연을 모니터링하는 것. 복제 지연은 대기 서버가 주 서버를 따라잡는 데 걸리는 시간을 나타내며, 대기 서버 과부하 시 증가 가능.

###### 지연 시간 계산

```sql
-- 주 서버에서:
SELECT pg_current_wal_lsn();

-- 대기 서버에서:
SELECT pg_last_wal_receive_lsn();

-- 지연 시간 = 주 서버의 WAL LSN - 대기 서버의 Receive LSN
```

###### 주 서버 모니터링

```sql
-- WAL Sender 프로세스 확인
SELECT * FROM pg_stat_replication;
```

주요 컬럼:
- `sent_lsn`: 주 서버에서 전송한 위치
- 주 서버 현재 WAL 위치와 `sent_lsn`의 큰 차이 → 주 서버 과부하
- `sent_lsn`과 대기 서버의 `receive_lsn`의 큰 차이 → 네트워크 지연 또는 대기 서버 과부하

###### 대기 서버 모니터링

```sql
-- WAL Receiver 상태 확인
SELECT * FROM pg_stat_wal_receiver;

-- pg_last_wal_replay_lsn과 flushed_lsn의 큰 차이
-- -> WAL을 받는 속도가 재생 속도보다 빠름
```

###### 프로세스 상태 확인

```bash
# 대기 서버의 walreceiver 프로세스 상태
ps aux | grep walreceiver

# 주 서버의 walsender 프로세스 상태
ps aux | grep walsender
```

#### 26.2.6. 복제 슬롯 (Replication Slots)

복제 슬롯은 주 서버가 대기 서버에 필요한 모든 정보를 유지하도록 하는 자동화된 방법 제공.

##### 개념

- 주 서버가 대기 서버에서 받을 때까지 WAL 세그먼트 제거 방지
- 주 서버가 대기 서버 연결 해제 시에도 행 제거 방지 (복구 충돌 회피)

##### 대체 방법 비교

- 복제 슬롯
  - 장점: 필요한 세그먼트만 보유
  - 단점: WAL이 `pg_wal`을 채울 수 있음
- `wal_keep_size`
  - 장점: 설정이 간단
  - 단점: 필요 이상으로 많은 세그먼트 보유
- `archive_command`
  - 장점: 유연함
  - 단점: 많은 세그먼트 보유 가능
- `hot_standby_feedback`
  - 장점: Vacuum 행 보호
  - 단점: 대기 서버 연결 해제 시 효과 없음

##### 주의사항

복제 슬롯이 `pg_wal`을 가득 채우지 않도록 주의 필요 → `max_slot_wal_keep_size`로 제한 가능.

##### 26.2.6.1. 복제 슬롯 조회 및 조작

###### 복제 슬롯 조회

```sql
-- 모든 복제 슬롯 확인
SELECT * FROM pg_replication_slots;

-- 슬롯 정보 확인
SELECT slot_name, slot_type, active FROM pg_replication_slots;
```

##### 26.2.6.2. 설정 예제

###### 1. 물리 복제 슬롯 생성

```sql
postgres=# SELECT * FROM pg_create_physical_replication_slot('node_a_slot');
  slot_name  | lsn
-------------+-----
 node_a_slot |

postgres=# SELECT slot_name, slot_type, active FROM pg_replication_slots;
  slot_name  | slot_type | active
-------------+-----------+--------
 node_a_slot | physical  | f
(1 row)
```

###### 2. 대기 서버 설정

```ini
# postgresql.conf (Standby)
primary_conninfo = 'host=192.168.1.50 port=5432 user=foo password=foopass'
primary_slot_name = 'node_a_slot'
```

#### 26.2.7. 캐스케이딩 복제 (Cascading Replication)

캐스케이딩 복제 기능을 사용하면 대기 서버가 WAL 레코드를 주 서버에서 직접 받지 않고 다른 대기 서버에서 수신 가능 → 이를 캐스케이딩 복제라고 함.

##### 개념

```
대기 서버가 다른 대기 서버에게 WAL 레코드를 스트림
Primary <- Cascading Standby <- Downstream Standby
```

##### 용어

- Cascading Standby: Receiver이면서 Sender인 대기 서버
- Upstream Server: 주 서버에 더 가까운 대기 서버
- Downstream Server: 주 서버에서 더 먼 대기 서버

##### 특징

1. 주 서버 직접 연결 수 감소
2. 사이트 간 대역폭 오버헤드 최소화
3. 각 대기 서버는 단 하나의 업스트림 서버에만 연결
4. 제한 없는 다운스트림 서버 개수
5. 현재 비동기만 가능 (동기 복제 미지원)
6. Hot Standby Feedback 업스트림 전파
7. `recovery_target_timeline = 'latest'`로 승격 후 새 주 서버 추적

##### 캐스케이딩 대기 서버 설정

```ini
# postgresql.conf (Cascading Standby)

# 다른 대기 서버로부터 WAL 수신
primary_conninfo = 'host=upstream_standby_ip port=5432 user=foo password=foopass'

# 다른 대기 서버에게 WAL 스트림 전송
max_wal_senders = 10
hot_standby = on

# 인증 설정: Section 20.1 (pg_hba.conf)
```

#### 26.2.8. 동기 복제 (Synchronous Replication)

PostgreSQL 스트리밍 복제는 기본적으로 비동기식임. 주 서버가 충돌하면 비동기 대기 서버에 복제되지 않은 일부 트랜잭션이 손실될 수 있음. 데이터 손실량은 장애 시 복제 지연에 비례하며, 분배되지 않은 WAL 세그먼트가 즉시 생성되지 않으므로 낮은 지연이 보장됨.

동기 복제는 트랜잭션의 모든 변경 사항이 하나 이상의 동기 대기 서버로 전달되었음을 확인하는 기능 제공 → 데이터베이스 서버가 보장할 수 있는 내구성 수준 향상.

##### 개념 비교

- 비동기
  - 데이터 손실: 주 서버 크래시 시 손실 가능
  - 응답 시간: 빠름
  - 사용 사례: 대부분의 용도
- 동기
  - 데이터 손실: 주 서버와 대기 서버가 동시에 크래시한 경우만 손실
  - 응답 시간: 느림
  - 사용 사례: 높은 내구성 필요
- 동기(`remote_write`)
  - 데이터 손실: OS 크래시(PostgreSQL 크래시 아님) 시에만 손실
  - 응답 시간: 중간
  - 사용 사례: 균형 잡힌 설정
- 동기(`remote_apply`)
  - 데이터 손실: 대기 서버 재생 이후에만 손실
  - 응답 시간: 가장 느림
  - 사용 사례: 로드 밸런싱

##### 이론 용어

- 2-safe replication: 주 서버 + 대기 서버 모두 디스크에 저장 확인
- group-1-safe: `synchronous_commit = 'remote_write'`일 때

##### 동작 방식

1. 트랜잭션 커밋 시 주 서버에서 대기
2. WAL을 대기 서버 디스크에 기록 확인 대기
3. 대기 서버 응답 확인 후 트랜잭션 완료
4. 최소 대기 시간: 주 서버 <-> 대기 서버 왕복 시간

##### 대기하지 않는 경우

- 읽기 전용 트랜잭션
- 트랜잭션 롤백
- 서브트랜잭션 커밋 (최상위 레벨만 대기)
- 데이터 로딩, 인덱스 빌드 등 긴 작업 중간
- 모든 2단계 커밋 작업

##### 26.2.8.1. 기본 설정 (Basic Configuration)

###### 1. 스트리밍 복제 먼저 설정

```ini
# postgresql.conf (Primary)
primary_conninfo = 'host=192.168.1.100 port=5432 ...'
```

###### 2. 동기 복제 활성화

```ini
# postgresql.conf (Primary)

# 핵심 설정
synchronous_standby_names = 's1'

# 일반적으로 이미 기본값: on
synchronous_commit = on
```

###### 3. WAL Receiver 상태 보고

```ini
# postgresql.conf (Standby)

# WAL을 디스크에 쓸 때마다 상태 보고 (기본값: 10초)
wal_receiver_status_interval = 10s
# 0으로 설정하면 비활성화
```

###### 4. 동작 확인

대기 서버에서 응답 메시지:
- `synchronous_commit = on`: WAL 디스크 저장 시
- `synchronous_commit = remote_write`: 디스크 쓰기(fsync 제외) 후
- `synchronous_commit = remote_apply`: 트랜잭션 재생 후

##### 26.2.8.2. 다중 동기 대기 서버 (Multiple Synchronous Standbys)

###### FIRST 방식 (우선순위 기반)

```ini
synchronous_standby_names = 'FIRST 2 (s1, s2, s3)'
```

동작:
- s1, s2가 동기 (우선순위 순)
- s3는 잠재적 동기 (s1 또는 s2 장애 시 대체)
- s4 (리스트 밖)는 비동기

특징:
- 트랜잭션이 2개 대기 서버 응답 대기
- 하나 장애 시 자동 다음 대기 서버로 변경

###### ANY 방식 (쿼럼 기반)

```ini
synchronous_standby_names = 'ANY 2 (s1, s2, s3)'
```

동작:
- s1, s2, s3 중 최소 2개 응답 필요
- s4 (리스트 밖)는 비동기

특징:
- 더 유연한 구성
- 쿼럼 개념 사용

###### 상태 확인

```sql
SELECT * FROM pg_stat_replication;
-- sync_state: 'sync', 'potential', 'async' 확인
```

##### 26.2.8.3. 성능 계획 (Planning for Performance)

###### 성능 영향

- 대기 서버 응답 대기 시간 증가
- 트랜잭션 락 계속 유지 (리소스 미활용)
- 응답 시간 증가 → 경합도 상승

###### 권장 설정: 애플리케이션 수준 차별화

```ini
# 예: 중요도별 설정

# 중요 데이터 (10%)
synchronous_commit = on

# 덜 중요한 데이터 (90%)
synchronous_commit = off
```

###### 애플리케이션 코드

```sql
-- 트랜잭션별 동기 복제 설정
BEGIN;
SET synchronous_commit = on;
-- 중요한 변경사항
COMMIT;

BEGIN;
SET synchronous_commit = off;
-- 덜 중요한 데이터
COMMIT;
```

###### 네트워크 대역폭

필수: 네트워크 대역폭 > WAL 생성 속도

##### 26.2.8.4. 고가용성 계획 (Planning for High Availability)

###### 문제점

동기 대기 서버 크래시 → 트랜잭션 커밋 완료 불가능.

###### 해결책: 다중 대기 서버

```ini
# FIRST 방식
synchronous_standby_names = 'FIRST 2 (s1, s2, s3)'
```

이점:
- s1, s2 중 하나 장애 → s3로 자동 전환
- 계속 2개 동기 상태 유지

###### 대기 서버 상태 전이

```
catchup 모드
    |
    v (lag이 0이 되면)
streaming 모드 (동기 대기 서버 가능)
```

모니터링:
```sql
SELECT * FROM pg_stat_replication;
-- state: 'catchup' or 'streaming' 확인
```

###### 주 서버 재시작 중 커밋 대기

주 서버가 재시작되면:
- 대기 중인 트랜잭션 → 완전히 커밋된 것으로 표시
- 모든 대기 서버가 WAL을 받았다는 보장 없음
- 일부 대기 서버에는 커밋이 안 보일 수 있음
- 보장: 커밋 승인 전까지 모든 동기 대기 서버 수신 확인

###### 대기 서버 부족 시 대응

```ini
# 수를 줄이거나 비활성화
synchronous_standby_names = ''

# 설정 리로드
SELECT pg_reload_conf();
```

###### 대기 서버 재생성 중 커밋 대기 방지

```sql
-- synchronous_commit = off에서 실행
SET synchronous_commit = off;
SELECT pg_backup_start('label');
-- ... backup 작업 ...
SELECT pg_backup_stop();
```

#### 26.2.9. 대기 상태에서의 연속 아카이빙 (Continuous Archiving in Standby)

대기 상태에서 연속 아카이빙 사용 시 두 가지 시나리오 존재: WAL 아카이브를 주 서버와 대기 서버 간에 공유하는 경우, 대기 서버가 자체 WAL 아카이브를 갖는 경우.

##### 2가지 시나리오

- 공유 아카이브: 주 서버와 대기 서버가 같은 아카이브 사용
  - 설정: 복잡한 테스트 필요
- 독립 아카이브: 대기 서버가 자신의 아카이브 사용
  - 설정: `archive_mode = always`

##### 1. 독립 아카이브 설정

```ini
# postgresql.conf (Standby)
archive_mode = always
archive_command = 'cp %p /path/to/archive/%f'
```

동작:
- 아카이브 복구 또는 스트리밍 복제로 받은 모든 WAL 아카이브
- 간단하며 권장

##### 2. 공유 아카이브 설정 (복잡함)

```ini
# archive_command 또는 archive_library에서:
# 1. 파일이 이미 존재하는지 확인
# 2. 내용이 동일한지 확인
# 3. 경쟁 조건 방지 (2개 서버가 동시에 같은 파일 아카이브)
```

문제점:
- 주 서버와 대기 서버가 동시에 같은 파일 아카이브 시도
- 파일 덮어쓰기 또는 컨텐츠 불일치 위험

##### 3. archive_mode = on 설정

```ini
archive_mode = on  # 기본값
```

동작:
- 복구 또는 대기 모드에서는 아카이버 비활성화
- 대기 서버 승격 후 활성화
- 대기 서버가 자신이 생성하지 않은 WAL/Timeline은 아카이브하지 않음

해결책:
대기 서버에 도달하기 전에 모든 WAL을 아카이브하도록 보장
- 파일 기반 로그 배송: 자동 보장 (대기 서버는 아카이브된 파일만 복구)
- 스트리밍 복제: 수동으로 보장 필요

#### 요약 비교표

- 파일 기반
  - 데이터 손실: 중간
  - 지연 시간: 높음
  - 구성 복잡도: 낮음
  - 성능 영향: 낮음
  - 적용 시기: 언제든
- 스트리밍
  - 데이터 손실: 낮음
  - 지연 시간: 낮음
  - 구성 복잡도: 중간
  - 성능 영향: 낮음
  - 적용 시기: 실시간
- 동기
  - 데이터 손실: 최소
  - 지연 시간: 높음
  - 구성 복잡도: 높음
  - 성능 영향: 중간
  - 적용 시기: 실시간
- 캐스케이딩
  - 데이터 손실: 낮음
  - 지연 시간: 낮음
  - 구성 복잡도: 높음
  - 성능 영향: 낮음
  - 적용 시기: 계층구조

---

### 26.3. 장애 조치 (Failover)

주 서버가 실패하면 대기 서버가 장애 조치 절차를 시작해야 함.

#### 주 서버 실패 시

대기 서버가 장애 조치 절차를 시작해야 함.

#### 대기 서버 실패 시

- 별도의 장애 조치 불필요
- 대기 서버 재시작 가능 시 → 복구 프로세스 즉시 재시작 (재시작 가능한 복구 활용)
- 대기 서버 재시작 불가 시 → 새로운 대기 서버 인스턴스 생성 필요

#### STONITH (Shoot The Other Node In The Head)

주요 위험 상황:
- 주 서버 실패 → 대기 서버가 새로운 주 서버 승격
- 이후 구 주 서버 재시작 시 반드시 메커니즘 필요
- 두 시스템이 모두 주 서버라고 생각하면 → 데이터 손실 위험

이를 split brain이라고도 하며, 구 주 서버가 자신이 더 이상 주 서버가 아님을 인식하도록 하는 메커니즘 필요.

#### 장애 조치 아키텍처

##### 2-System 모델 (주요)

```
Primary <-> Heartbeat <-> Standby
```
- 연결성과 주 서버 생존 여부 지속 확인

##### 3-System 모델 (선택적)

```
Primary <-> Heartbeat <-> Standby
    |                        |
    +-> Witness Server <-----+
```
- 부적절한 장애 조치 방지
- 추가 복잡성 있으므로 신중한 설정 및 테스트 필요

#### PostgreSQL의 제한사항

PostgreSQL은 다음을 제공하지 않음:
- 주 서버 실패 감지 시스템 소프트웨어
- 대기 데이터베이스 서버에 알림 메커니즘

필요 도구:
- 외부 도구 사용 필수
- OS 수준의 기능 통합 (IP 주소 마이그레이션 등)

#### 장애 조치 후 절차

##### "Degenerate State" (축소된 상태)

- 구 대기 서버 = 새로운 주 서버 (단독 운영)
- 구 주 서버 = 다운 상태

##### 정상 운영 복구

1. 옵션 1: 구 주 서버 복구 시 대기 서버 재구성
2. 옵션 2: 새로운 제3 시스템에 대기 서버 구성
3. pg_rewind 활용: 대형 클러스터에서 복구 속도 향상

```
pg_rewind 유틸리티 사용 가능
-> 역할 전환 기간 단축
```

#### Primary <-> Standby 전환

##### 장점

- 빠른 전환
- 정기적인 각 시스템의 유지보수 다운타임 가능
- 장애 조치 메커니즘 테스트 (정상 작동 확인)

##### 권장사항

- 정기적인 전환 수행
- 서면 운영 절차 작성

#### Logical Replication 고려사항

Logical Replication Slot 동기화 사용 시:

```
Primary -> Standby로 전환 전:
1. Logical slots 동기화 상태 확인 필요
2. Section 47.2.3 참조: Replication Slot Synchronization
3. Section 29.3 참조: Logical Replication Failover
   -> 정확한 절차 따라야 함
```

#### 장애 조치 트리거 명령어

##### Log-Shipping Standby Server 장애 조치 시작

```bash
# 방법 1: pg_ctl 사용
pg_ctl promote

# 방법 2: SQL 함수 사용
SELECT pg_promote();
```

##### 예외사항

보고 서버 설정 시 (읽기 전용, 고가용성 아님):
- 승격(Promote) 불필요

#### 체크리스트

- [ ] STONITH 메커니즘 구현
- [ ] Heartbeat 감시 시스템 구성
- [ ] 외부 장애 조치 도구 선택 및 통합
- [ ] `pg_ctl promote` 또는 `pg_promote()` 명령어 테스트
- [ ] pg_rewind 유틸리티 숙지 (대형 클러스터)
- [ ] 정기적 Primary-Standby 전환 테스트
- [ ] 서면 운영 절차 작성
- [ ] Logical replication slot 동기화 상태 확인

---

### 26.4. 핫 대기 (Hot Standby)

핫 대기(Hot Standby)라는 용어는 서버가 아카이브 복구 또는 대기 모드에 있는 동안 클라이언트 연결과 읽기 전용 쿼리 실행이 가능한 기능을 가리킴 → 복제 목적과 원하는 정밀도로 백업을 복원하는 데 유용.

#### 26.4.1. 사용자 개요 (User's Overview)

`hot_standby` 파라미터가 대기 서버에서 `true`로 설정되면 `recovery_min_apply_delay`에 설정된 지연 시간 경과 후 클라이언트 연결 수락 시작. 모든 연결은 엄격히 읽기 전용이며 임시 테이블 생성도 불가.

핫 대기의 데이터는 약간의 시간 지연이 있어 주 서버에서 커밋된 최신 레코드가 즉시 반영되지 않을 수 있음. 단, 동일한 대기 서버에 연결된 사용자끼리는 동시에 동일한 데이터 조회 가능 → 최종적 일관성(Eventually Consistent) 상태에 해당.

##### 허용되는 명령어

###### 쿼리 접근
```sql
SELECT
COPY TO
```

###### 커서 명령
```sql
DECLARE, FETCH, CLOSE
```

###### 설정 명령
```sql
SHOW, SET, RESET
```

###### 트랜잭션 관리
```sql
BEGIN, END, ABORT, START TRANSACTION
SAVEPOINT, RELEASE, ROLLBACK TO SAVEPOINT
EXCEPTION 블록 및 내부 서브트랜잭션
```

###### 락 명령
```sql
LOCK TABLE (ACCESS SHARE, ROW SHARE, ROW EXCLUSIVE 모드만)
```

###### 플랜 및 리소스
```sql
PREPARE, EXECUTE, DEALLOCATE, DISCARD
```

###### 플러그인 및 확장
```sql
LOAD
```

###### 기타
```sql
UNLISTEN
```

##### 금지되는 명령어

###### DML (Data Manipulation Language)
```sql
INSERT, UPDATE, DELETE, MERGE, COPY FROM, TRUNCATE
```

###### DDL (Data Definition Language)
```sql
CREATE, DROP, ALTER, COMMENT
```

###### 락 관련
```sql
SELECT ... FOR SHARE | UPDATE
LOCK (ACCESS EXCLUSIVE MODE 이상)
```

###### 트랜잭션 설정
```sql
BEGIN READ WRITE
START TRANSACTION READ WRITE
SET TRANSACTION READ WRITE
SET transaction_read_only = off
```

###### 2단계 커밋
```sql
PREPARE TRANSACTION, COMMIT PREPARED, ROLLBACK PREPARED
```

###### 시퀀스 업데이트
```sql
nextval(), setval()
```

###### 리스너
```sql
LISTEN, NOTIFY
```

##### Hot Standby 상태 확인

```sql
-- PostgreSQL 14 이상
SHOW in_hot_standby;

-- 이전 버전
SHOW transaction_read_only;
```

#### 26.4.2. 쿼리 충돌 처리 (Handling Query Conflicts)

주 서버와 대기 서버가 동일한 데이터에 대해 동시에 작동하면 두 종류의 작업 사이에 잠재적 충돌 발생 → 핫 대기에서 충돌이 가장 많이 발생하는 원인은 배타적 잠금(exclusive lock).

##### 충돌 유형

###### 1. 접근 제외 잠금 (Access Exclusive Locks)

- 주 서버의 `LOCK` 명령과 DDL 작업이 대기 서버 쿼리와 충돌

###### 2. 테이블스페이스 삭제

```sql
DROP TABLESPACE -- 대기 서버의 임시 파일과 충돌
```

###### 3. 데이터베이스 삭제

```sql
DROP DATABASE -- 대기 서버 세션과 충돌
```

###### 4. Vacuum 정리

- WAL의 vacuum 정리 레코드가 대기 서버 트랜잭션과 충돌
- 행 버전 정리 시 MVCC 가시성 문제 발생

##### 충돌 해결 메커니즘

관련 파라미터:
```
max_standby_archive_delay     -- 아카이브 WAL 읽기 시 최대 지연
max_standby_streaming_delay   -- 스트리밍 복제 시 최대 지연
```

동작 원리:
1. 지정된 시간 초과 시 충돌 쿼리 취소
2. 쿼리 취소 오류 발생
3. DROP DATABASE의 경우 전체 세션 종료

##### 해결 방안

###### 1. hot_standby_feedback 활성화

```
hot_standby_feedback = on
```

장점:
- VACUUM이 최근 삭제된 행 제거 방지
- 충돌 감소

단점:
- 주 서버의 데드 행 정리 지연
- 테이블 팽창 가능성

###### 2. 파라미터 조정

```ini
# 고가용성 서버 (권장 예시)
max_standby_archive_delay = 10s
max_standby_streaming_delay = 10s

# 분석용 서버
max_standby_archive_delay = -1      # 무한 대기
max_standby_streaming_delay = -1    # 무한 대기
```

###### 3. 모니터링

```sql
-- 충돌 통계 확인
SELECT * FROM pg_stat_database_conflicts;
SELECT * FROM pg_stat_database;
```

###### 4. 로깅 설정

```ini
log_recovery_conflict_waits = on    -- deadlock_timeout 초과 시 로그
```

#### 26.4.3. 관리자 개요 (Administrator's Overview)

`hot_standby`가 `postgresql.conf`에서 `on`으로 설정되고 `standby.signal` 파일이 존재하면, 서버는 복구 모드에서 연결을 허용하는 핫 대기 모드로 동작함.

##### Hot Standby 시작 확인

필수 조건:
- `hot_standby = on` (postgresql.conf)
- `standby.signal` 파일 존재

로그 메시지:
```
LOG: entering standby mode
... 시간 경과 ...
LOG: consistent recovery state reached
LOG: database system is ready to accept read-only connections
```

##### 공유 메모리 파라미터 설정

대기 서버의 다음 파라미터는 주 서버 이상으로 설정 필요:

```
max_connections
max_prepared_transactions
max_locks_per_transaction
max_wal_senders
max_worker_processes
```

권장사항:
1. 증가할 때: 대기 서버 먼저 → 주 서버
2. 감소할 때: 주 서버 먼저 → 대기 서버

##### 파라미터 불일치 경고

```
WARNING: hot standby is not possible because of insufficient parameter settings
DETAIL: max_connections = 80 is a lower setting than on the primary server, where its value was 100.
LOG: recovery has paused
DETAIL: If recovery is unpaused, the server will shut down.
```

##### 복구 중 금지 명령어

```sql
CREATE INDEX              -- DDL
GRANT, REVOKE            -- 권한
ANALYZE, VACUUM, CLUSTER -- 유지보수
```

##### 시스템 뷰 동작

###### pg_stat_activity
```sql
-- 복구 트랜잭션은 활성 상태로 표시되지 않음
SELECT * FROM pg_stat_activity;
```

###### pg_locks
```sql
-- 백업 프로세스가 보유한 AccessExclusiveLocks 표시
SELECT * FROM pg_locks;
```

###### pg_prepared_xacts
```sql
-- 복구 중에는 항상 비어있음
SELECT * FROM pg_prepared_xacts;
```

##### 특별 고려사항

###### 1. Hint Bit 업데이트
```
-- 주 서버의 hint bit는 WAL 로깅되지 않음
-- 대기 서버가 다시 쓰기 수행 (읽기 전용이지만 쓰기 발생)
```

###### 2. 임시 파일
```
-- 사용자가 대용량 정렬 임시 파일 생성 가능
-- relcache 정보 파일 재생성 가능
```

###### 3. 외부 데이터베이스 접근
```sql
-- 읽기 전용이지만 dblink로 원격 DB 쓰기 가능
-- PL 함수로 외부 작업 수행 가능
```

###### 4. Advisory Locks
```sql
-- 정상 작동
-- WAL 로깅되지 않음
-- 데드락 감지 지원
SELECT pg_advisory_lock(1);
```

##### 프로세스 활동

```
Checkpointer: 활성 (restartpoint 수행)
Background Writer: 활성 (블록 정리)
Autovacuum: 비활성 (복구 종료 후 시작)
```

#### 26.4.4. 핫 대기 매개변수 참조 (Hot Standby Parameter Reference)

##### 주 서버 파라미터

```ini
wal_level = replica 또는 logical    -- 필수
```

##### 대기 서버 파라미터

```ini
hot_standby = on                              -- Hot Standby 활성화
max_standby_archive_delay = 30s               -- 아카이브 읽기 최대 지연 (기본값)
max_standby_streaming_delay = 30s             -- 스트리밍 최대 지연 (기본값)
hot_standby_feedback = off                    -- VACUUM 정리 피드백
log_recovery_conflict_waits = off             -- 충돌 대기 로깅
```

#### 26.4.5. 주의사항 (Caveats)

핫 대기 모드에는 몇 가지 제한 사항 존재.

##### 1. 대형 서브트랜잭션

```
-- 64개 초과 서브트랜잭션
-- 읽기 전용 연결 시작 지연
-- 장기 실행 쓰기 트랜잭션 완료까지 대기
```

로그 메시지:
```
LOG: delaying hot standby connections until all transactions are finished
```

##### 2. 체크포인트 필요성

```
-- 주 서버 체크포인트 시 유효한 시작점 생성
-- 주 서버 종료 시 대기 서버가 hot standby 재진입 불가능할 수 있음
```

##### 3. AccessExclusiveLocks와 Prepared Transactions

```
-- 복구 종료 시 준비된 트랜잭션의 AccessExclusiveLocks
-- 일반적인 2배 lock table 항목 필요
-- max_locks_per_transaction을 2배로 설정 권장
```

설정 예:
```ini
-- 주 서버
max_locks_per_transaction = 64

-- 대기 서버 (권장)
max_locks_per_transaction = 128
```

##### 4. 직렬화 격리 수준 미지원

```sql
-- 불가능
BEGIN ISOLATION LEVEL SERIALIZABLE;

-- 오류 발생
ERROR: cannot use SERIALIZABLE isolation level in hot standby mode
```

#### 실제 구성 예

```ini
# postgresql.conf (주 서버)
wal_level = replica
archive_mode = on
archive_command = 'test ! -f /backup/wal/%f && cp %p /backup/wal/%f'

# postgresql.conf (대기 서버)
hot_standby = on
max_standby_archive_delay = 30s
max_standby_streaming_delay = 30s
hot_standby_feedback = on
max_connections = 200          # 주 서버 이상
max_locks_per_transaction = 64
```

---

### 참고

- Section 25.3: Continuous Archiving and Point-in-Time Recovery
- Section 26.2.5: Streaming Replication (상세)
- Section 26.3: Failover
- Section 26.4: Hot Standby
- Section 29.3: Logical Replication Failover
- Section 47.2.3: Replication Slot Synchronization

---

### 요약

PostgreSQL의 고가용성, 로드 밸런싱 및 복제는 다양한 솔루션으로 구현 가능 → 각 솔루션은 특정 요구사항과 환경에 맞게 선택 필요.

#### 주요 솔루션 요약

- 공유 디스크 장애조치
  - 특징: 단일 데이터 복사본 · 빠른 장애조치
  - 적합한 사용 사례: 공유 스토리지 환경
- 파일 시스템 복제
  - 특징: 블록 수준 미러링
  - 적합한 사용 사례: DRBD 등 사용 환경
- WAL 로그 배송
  - 특징: 내장 기능 · 전체 DB 복제
  - 적합한 사용 사례: 일반적인 HA 구성
- 논리적 복제
  - 특징: 테이블 단위 복제 · 양방향 가능
  - 적합한 사용 사례: 선택적 데이터 복제
- 트리거 기반 복제
  - 특징: 테이블 단위 · 비동기
  - 적합한 사용 사례: 분석/웨어하우스 분산
- SQL 미들웨어
  - 특징: 쿼리 분산
  - 적합한 사용 사례: 로드 밸런싱 중심
- 비동기 다중 마스터
  - 특징: 독립 서버 · 주기적 동기화
  - 적합한 사용 사례: 불규칙한 연결 환경
- 동기 다중 마스터
  - 특징: 모든 서버 쓰기 가능
  - 적합한 사용 사례: 읽기 중심 워크로드

#### 핵심 개념

1. WAL (Write-Ahead Log): PostgreSQL의 복제 핵심 메커니즘
2. 스트리밍 복제: 실시간에 가까운 데이터 동기화
3. 동기/비동기: 데이터 내구성과 성능 간의 트레이드오프
4. 핫 대기: 복구 중에도 읽기 전용 쿼리 가능
5. 복제 슬롯: WAL 세그먼트 자동 관리
6. 캐스케이딩: 계층적 복제 구조로 확장성 향상

#### 권장 사항

- 중요한 데이터는 동기 복제 사용
- 성능이 중요한 경우 비동기 복제 고려
- 정기적인 장애조치 테스트 수행
- 서면 운영 절차 작성 및 유지
- 모니터링 시스템 구축
## 제23장. 지역화 (Localization)

> PostgreSQL 18 공식 문서 번역
>
> 원문: https://www.postgresql.org/docs/current/charset.html

이 장에서는 관리자가 사용할 수 있는 지역화 기능을 설명함.

목차
- [23.1. 로케일 지원](#231-로케일-지원)
  - [23.1.1. 개요](#2311-개요)
  - [23.1.2. 동작](#2312-동작)
  - [23.1.3. 로케일 선택](#2313-로케일-선택)
  - [23.1.4. 로케일 제공자](#2314-로케일-제공자)
  - [23.1.5. ICU 로케일](#2315-icu-로케일)
  - [23.1.6. 문제 해결](#2316-문제-해결)
- [23.2. 콜레이션 지원](#232-콜레이션-지원)
  - [23.2.1. 개념](#2321-개념)
  - [23.2.2. 콜레이션 관리](#2322-콜레이션-관리)
  - [23.2.3. ICU 사용자 정의 콜레이션](#2323-icu-사용자-정의-콜레이션)
- [23.3. 문자 집합 지원](#233-문자-집합-지원)
  - [23.3.1. 지원되는 문자 집합](#2331-지원되는-문자-집합)
  - [23.3.2. 문자 집합 설정](#2332-문자-집합-설정)
  - [23.3.3. 서버와 클라이언트 간 자동 문자 집합 변환](#2333-서버와-클라이언트-간-자동-문자-집합-변환)
  - [23.3.4. 사용 가능한 문자 집합 변환](#2334-사용-가능한-문자-집합-변환)
  - [23.3.5. 추가 자료](#2335-추가-자료)

---

### 23.1. 로케일 지원

로케일 지원은 알파벳, 정렬, 숫자 형식 등에 관한 문화적 선호도를 애플리케이션이 준수하도록 하는 기능. PostgreSQL은 서버 운영 체제에서 제공하는 표준 ISO C 및 POSIX 로케일 기능을 사용함.

#### 23.1.1. 개요

로케일 지원은 `initdb`를 사용하여 데이터베이스 클러스터를 생성할 때 자동으로 초기화됨. 기본 로케일은 실행 환경에서 상속됨.

##### 기본 로케일 선택

초기화 시 로케일을 지정하려면:

```bash
initdb --locale=sv_SE
```

이 예제는 로케일을 스웨덴(SE)에서 사용되는 스웨덴어(sv)로 설정함. 다른 예시:
- `en_US` (미국 영어)
- `fr_CA` (캐나다 프랑스어)
- `fr_BE.UTF-8` (UTF-8 인코딩을 사용하는 벨기에 프랑스어)

##### 로케일 하위 카테고리

로케일 규칙은 하위 카테고리별로 혼합하여 지정 가능:

- `LC_COLLATE`: 문자열 정렬 순서
- `LC_CTYPE`: 문자 분류 (문자란 무엇인가·대문자 등가는 무엇인가)
- `LC_MESSAGES`: 메시지 언어
- `LC_MONETARY`: 통화 금액 형식
- `LC_NUMERIC`: 숫자 형식
- `LC_TIME`: 날짜 및 시간 형식

##### 예제: 혼합 로케일

```bash
initdb --locale=fr_CA --lc-monetary=en_US
```

캐나다 프랑스어 로케일을 사용하되, 통화 형식에는 미국 규칙을 적용함.

##### 특수 로케일 이름

로케일 지원을 비활성화하려면:

```bash
initdb --locale=C
# 또는
initdb --locale=POSIX
```

##### 고정 로케일 카테고리

`LC_COLLATE`와 `LC_CTYPE`은 데이터베이스 생성 시 고정 → 이후 변경 불가. 인덱스의 정렬 순서에 영향을 미치기 때문. 다른 카테고리는 서버 구성 매개변수를 통해 동적으로 변경 가능.

##### 환경 변수 상속

실행 환경에서 로케일을 상속할 때, 각 카테고리에 대해 다음 변수들이 순서대로 참조됨:
1. `LC_ALL`
2. `LC_<CATEGORY>` (예: `LC_COLLATE`)
3. `LANG`
4. 설정된 것이 없으면 기본값 `C`

참고: `LANGUAGE` 환경 변수는 메시지 언어에 대해 다른 설정을 재정의함.

##### NLS 지원

메시지 번역에는 빌드 시 NLS 활성화 필요:

```bash
configure --enable-nls
```

---

#### 23.1.2. 동작

로케일 설정은 다음 SQL 기능에 영향을 미침:

1. 텍스트 데이터에 대한 `ORDER BY` 쿼리 및 비교 연산자의 정렬 순서
2. 문자열 함수: `upper`, `lower`, `initcap`
3. 패턴 매칭: `LIKE`, `SIMILAR TO`, POSIX 스타일 정규 표현식
4. 형식 지정 함수: `to_char` 함수 계열
5. `LIKE` 절에서의 인덱스 사용

##### 성능 고려사항

`C` 또는 `POSIX` 이외의 로케일을 사용하면 성능 저하 발생:
- 문자 처리가 느려짐
- `LIKE`에서 일반 인덱스 사용이 방지됨

##### 성능 해결 방법

1. `LIKE` 인덱스에 사용자 정의 연산자 클래스 사용 (Section 11.10 참조)
2. `C` 콜레이션을 사용하여 인덱스 생성 (Section 23.2 참조)

---

#### 23.1.3. 로케일 선택

로케일은 여러 범위에서 선택 가능하며, 각각 더 세밀한 세분화를 제공함:

1. 운영 체제 환경: 새로 초기화된 클러스터의 기본값
2. `initdb` 명령줄 옵션: 클러스터 전체 로케일 설정 지정
3. 데이터베이스별 설정: `CREATE DATABASE` 또는 `createdb` 명령 사용
4. 열별 설정: SQL 콜레이션 객체 사용 (Section 23.2)
5. 쿼리별 설정: 임시 변경을 위한 SQL 콜레이션 객체 사용

---

#### 23.1.4. 로케일 제공자

로케일 제공자는 콜레이션 및 문자 분류 동작을 정의하는 데 사용할 라이브러리를 지정함.

##### 예제: ICU 제공자로 초기화

```bash
initdb --locale-provider=icu --icu-locale=en
```

로케일 제공자는 다양한 세분화 수준에서 혼합 가능(예: 클러스터에는 `libc`를 사용하지만 개별 데이터베이스에는 `icu`를 사용).

##### 사용 가능한 로케일 제공자

###### `builtin`

내장 연산을 사용함. `C`, `C.UTF-8`, `PG_UNICODE_FAST` 로케일만 지원.

`C` 로케일:
- libc 제공자의 `C` 로케일과 동일
- 동작은 데이터베이스 인코딩에 따라 달라질 수 있음

`C.UTF-8` 로케일:
- UTF-8 데이터베이스 인코딩에서만 사용 가능
- 유니코드 기반 동작
- 콜레이션에 코드 포인트 값 사용
- "POSIX 호환" 의미론을 기반으로 한 정규 표현식 문자 클래스
- "단순" 변형 대소문자 매핑

`PG_UNICODE_FAST` 로케일:
- UTF-8 데이터베이스 인코딩에서만 사용 가능
- 유니코드 기반 동작
- 콜레이션에 코드 포인트 값 사용
- "표준" 의미론을 기반으로 한 정규 표현식 문자 클래스
- "전체" 변형 대소문자 매핑

###### `icu`

외부 ICU 라이브러리를 사용함(PostgreSQL이 지원을 포함하여 구성되어야 함).

장점:
- 콜레이션과 문자 분류가 운영 체제 및 데이터베이스 인코딩과 독립적
- 다른 플랫폼으로 전환할 때 일관된 결과
- `LC_COLLATE`와 `LC_CTYPE`을 독립적으로 설정 가능

참고: 결과는 ICU 라이브러리 버전에 따라 달라질 수 있음 → 자연어의 변경 사항을 반영하여 업데이트되기 때문.

###### `libc`

운영 체제의 C 라이브러리를 사용함.

- 콜레이션과 문자 분류가 `LC_COLLATE`와 `LC_CTYPE`에 의해 제어됨
- 이러한 설정은 독립적으로 설정 불가

참고: 동일한 로케일 이름이 다른 플랫폼에서 다른 동작을 할 수 있음.

---

#### 23.1.5. ICU 로케일

##### 23.1.5.1. ICU 로케일 이름

ICU는 언어 태그 형식을 사용함 (23.1.5.3 참고).

```sql
CREATE COLLATION mycollation1 (provider = icu, locale = 'ja-JP');
CREATE COLLATION mycollation2 (provider = icu, locale = 'fr');
```

##### 23.1.5.2. 로케일 정규화 및 검증

새 ICU 콜레이션이나 데이터베이스를 정의할 때, 로케일 이름은 표준 언어 태그 형식으로 변환됨:

```sql
CREATE COLLATION mycollation3 (provider = icu, locale = 'en-US-u-kn-true');
NOTICE:  using standard form "en-US-u-kn" for locale "en-US-u-kn-true"

CREATE COLLATION mycollation4 (provider = icu, locale = 'de_DE.utf8');
NOTICE:  using standard form "de-DE" for locale "de_DE.utf8"
```

###### 검증 경고

```sql
CREATE COLLATION nonsense (provider = icu, locale = 'nonsense');
WARNING:  ICU locale "nonsense" has unknown language "nonsense"
HINT:  To disable ICU locale validation, set parameter icu_validation_level to DISABLED.
```

`icu_validation_level` 매개변수는 검증 메시지 보고 방식을 제어함. `ERROR`로 설정되지 않은 한 콜레이션은 여전히 생성되지만 동작이 의도한 대로 되지 않을 수 있음.

##### 23.1.5.3. 언어 태그

언어 태그 (BCP 47 표준)는 언어, 지역 및 로케일 정보에 대한 표준화된 식별자.

###### 기본 형식

```
language[-region][-script]
```

예시:
- `ja-JP` (일본의 일본어)
- `de` (독일어)
- `fr-CA` (캐나다의 프랑스어)

###### 언어 태그의 콜레이션 설정

콜레이션 사용자 정의를 위해 `-u` 뒤에 `-key-value` 쌍을 추가함:

```
language-region-u-key1-value1-key2-value2
```

부울 설정은 값을 생략 가능(암시적 `true`).

###### 예제: 사용자 정의 콜레이션 언어 태그

```
en-US-u-kn-ks-level2
```

의미:
- 미국 지역의 영어
- `kn` (숫자 정렬) = `true` (숫자 시퀀스를 숫자로 처리)
- `ks` (대소문자 구분) = `level2` (대소문자 구분 안함)

###### 사용 예제

```sql
CREATE COLLATION mycollation5 (provider = icu, deterministic = false, locale = 'en-US-u-kn-ks-level2');

SELECT 'aB' = 'Ab' COLLATE mycollation5 as result;
 result
--------
 t
(1 row)

SELECT 'N-45' < 'N-123' COLLATE mycollation5 as result;
 result
--------
 t
(1 row)
```

---

#### 23.1.6. 문제 해결

##### 문제 해결 단계

1. OS 로케일 구성 확인:
   ```bash
   locale -a
   ```
   시스템에 설치된 로케일을 나열함.

2. 활성 로케일 설정 확인:
   ```sql
   SHOW LC_COLLATE;
   SHOW LC_CTYPE;
   SHOW LC_MESSAGES;
   SHOW LC_MONETARY;
   ```

3. 유의사항: `LC_COLLATE`와 `LC_CTYPE`은 데이터베이스 생성 시 결정되며 기존 데이터베이스에서는 변경 불가.

##### 테스트 스위트

소스 배포판의 `src/test/locale` 디렉토리에는 PostgreSQL의 로케일 지원에 대한 테스트 스위트가 있음.

##### 클라이언트 애플리케이션 고려사항

오류 메시지 텍스트를 파싱하는 클라이언트 애플리케이션은 서버 메시지가 다른 언어로 출력될 때 문제가 발생함. 메시지 텍스트 대신 오류 코드 사용 권장.

##### 번역 기여

메시지 번역 유지에는 자원 봉사자의 노력이 필요함. 해당 언어의 번역 품질 개선에 기여하고 싶다면 Chapter 56(네이티브 언어 지원)을 참조하거나 개발자 메일링 리스트에 문의.

---

### 23.2. 콜레이션 지원

콜레이션 기능을 사용하면 열 단위 또는 연산 단위로 정렬 순서와 문자 분류 동작을 지정 가능 → 데이터베이스 생성 후 `LC_COLLATE`와 `LC_CTYPE`을 변경할 수 없다는 제약을 일부 완화함.

#### 23.2.1. 개념

##### 콜레이션 기본 개념

콜레이션을 지원하는 데이터 타입의 모든 표현식(기본 제공: `text`, `varchar`, `char`)에는 콜레이션이 적용됨. 열 참조의 경우 해당 열에 정의된 콜레이션이 사용되고, 리터럴 상수의 경우 데이터 타입의 기본 콜레이션이 사용됨.

복합 표현식의 콜레이션은 입력 콜레이션에서 파생됨. 표현식의 콜레이션은:
- "기본" 콜레이션(데이터베이스 로케일 설정)
- 불확정(정렬 연산 실패 원인)

##### 콜레이션 파생 규칙

표현식에서 여러 콜레이션을 결합할 때:

1. 명시적 콜레이션 재정의: 입력에 `COLLATE` 절을 통한 명시적 콜레이션 파생이 있는 경우, 모든 명시적 콜레이션이 일치해야 하며 그렇지 않으면 오류 발생.

2. 암시적 콜레이션 결합: 모든 입력이 동일한 암시적 콜레이션 또는 기본값을 가져야 함. 비기본 콜레이션이 우선함.

3. 충돌하는 암시적 콜레이션: 불확정 콜레이션이 됨 → 연산이 콜레이션을 알아야 하는 경우에만 런타임 오류 발생.

##### 예제: 콜레이션 충돌 해결

```sql
CREATE TABLE test1 (
    a text COLLATE "de_DE",
    b text COLLATE "es_ES",
    ...
);

-- 작동: 암시적과 기본을 결합
SELECT a < 'foo' FROM test1;

-- 작동: 명시적 콜레이션이 재정의
SELECT a < ('foo' COLLATE "fr_FR") FROM test1;

-- 오류: 충돌하는 암시적 콜레이션
SELECT a < b FROM test1;

-- 수정: 명시적 콜레이션 지정자
SELECT a < b COLLATE "de_DE" FROM test1;
SELECT a COLLATE "de_DE" < b FROM test1;

-- 작동: || 연산자는 콜레이션에 관심이 없음
SELECT a || b FROM test1;

-- 오류: ORDER BY는 콜레이션이 필요하지만 || 결과는 불확정
SELECT * FROM test1 ORDER BY a || b;

-- 수정: ORDER BY에 명시적 콜레이션
SELECT * FROM test1 ORDER BY a || b COLLATE "fr_FR";
```

---

#### 23.2.2. 콜레이션 관리

콜레이션은 SQL 이름을 OS 로케일에 매핑하는 SQL 스키마 객체. 두 가지 표준 제공자 존재:

- `libc`: OS C 라이브러리 로케일 사용 (대부분의 도구가 이것을 사용)
- `icu`: 외부 ICU 라이브러리 사용 (빌드 시 구성된 경우에만 사용 가능)

##### 제공자 차이점

libc 콜레이션:
- `setlocale()`을 통해 `LC_COLLATE` 및 `LC_CTYPE` 설정에 매핑
- 문자 집합 인코딩에 연결됨
- 동일한 콜레이션 이름이 다른 인코딩에 존재할 수 있음

icu 콜레이션:
- 명명된 ICU 콜레이터에 매핑
- 별도의 collate/ctype 설정 미지원
- 인코딩과 독립적 (데이터베이스에서 이름당 하나의 콜레이션만)

---

##### 23.2.2.1. 표준 콜레이션

모든 플랫폼에서 사용 가능한 표준 콜레이션:

- `unicode`: 기본 유니코드 콜레이션 요소 테이블을 사용한 유니코드 콜레이션 알고리즘, ICU 지원 필요
- `ucs_basic`: 유니코드 코드 포인트 정렬, ASCII A-Z만, UTF8만
- `pg_unicode_fast`: 유니코드 코드 포인트 정렬, 전체 대소문자 매핑, UTF8만
- `pg_c_utf8`: 유니코드 코드 포인트 정렬, 단순 대소문자 매핑, UTF8만
- `C`/`POSIX`: 전통적인 C 동작, 바이트 값 정렬, ASCII A-Z만
- `default`: 데이터베이스 생성 로케일 사용

---

##### 23.2.2.2. 사전 정의된 콜레이션

데이터베이스 클러스터가 초기화될 때(OS가 여러 로케일을 지원하거나 ICU가 구성된 경우), `initdb`는 사용 가능한 로케일로 `pg_collation`을 채움.

사용 가능한 콜레이션 보기:
```sql
SELECT * FROM pg_collation;
-- 또는 psql에서:
\dOS+
```

###### 23.2.2.2.1. libc 콜레이션

예: OS가 `de_DE.utf8` 로케일을 제공 → PostgreSQL이 생성:
- UTF8 인코딩용 `de_DE.utf8` 콜레이션
- `de_DE` 콜레이션 (인코딩 독립적 이름)

새 libc 콜레이션 생성:
```sql
CREATE COLLATION german (provider = libc, locale = 'de_DE');
```

OS 로케일 일괄 가져오기:
```sql
SELECT pg_import_system_collations('pg_catalog');
```

###### 23.2.2.2.2. ICU 콜레이션

ICU는 `-x-icu` 접미사가 있는 BCP 47 언어 태그 형식을 사용함.

예제 ICU 콜레이션:
```
de-x-icu           -- 독일어, 기본 변형
de-AT-x-icu        -- 독일어, 오스트리아
und-x-icu          -- ICU 루트 콜레이션 (언어에 구애받지 않음)
```

---

##### 23.2.2.3. 새 콜레이션 객체 생성

###### 23.2.2.3.1. libc 콜레이션

```sql
CREATE COLLATION german (provider = libc, locale = 'de_DE');
```

`locale -a`로 사용 가능한 로케일 확인 가능.

###### 23.2.2.3.2. ICU 콜레이션

```sql
CREATE COLLATION german (provider = icu, locale = 'de-DE');
```

ICU는 BCP 47 언어 태그와 libc 스타일 이름을 수용함(자동으로 변환됨).

###### 23.2.2.3.3. 콜레이션 복사

기존 콜레이션에서 새 콜레이션 생성:

```sql
CREATE COLLATION german FROM "de_DE";
CREATE COLLATION french FROM "fr-x-icu";
```

---

##### 23.2.2.4. 비결정적 콜레이션

콜레이션은 결정적(동일한 문자열이 동일한 바이트 시퀀스를 가짐)이거나 비결정적(동일한 문자열이 바이트에서 다를 수 있음, 예: 대소문자 구분 안함, 악센트 구분 안함)일 수 있음.

비결정적 콜레이션 생성:
```sql
CREATE COLLATION ndcoll (provider = icu, locale = 'und', deterministic = false);

-- 악센트와 대소문자 무시
CREATE COLLATION case_insensitive (provider = icu, locale = 'und-u-ks-level2', deterministic = false);

-- 악센트만 무시
CREATE COLLATION ignore_accents (provider = icu, locale = 'und-u-ks-level1-kc-true', deterministic = false);
```

장단점:
- 장점: 유니코드에서 더 정확한 동작
- 단점: 성능 저하
- 단점: B-트리 중복 제거 비활성화
- 단점: 일부 패턴 매칭 사용 불가

대안으로 `normalize()` 및 `is_normalized()` 함수 사용 가능.

---

#### 23.2.3. ICU 사용자 정의 콜레이션

ICU는 언어 태그의 콜레이션 설정을 통해 광범위한 사용자 정의를 허용함.

예제:
```sql
-- 악센트와 대소문자 차이 무시
CREATE COLLATION ignore_accent_case (provider = icu, deterministic = false, locale = 'und-u-ks-level1');
SELECT 'Å' = 'A' COLLATE ignore_accent_case;  -- true
SELECT 'z' = 'Z' COLLATE ignore_accent_case;  -- true

-- 대문자가 소문자보다 먼저 정렬
CREATE COLLATION upper_first (provider = icu, locale = 'und-u-kf-upper');
SELECT 'B' < 'b' COLLATE upper_first;  -- true

-- 숫자 콜레이션, 구두점 무시
CREATE COLLATION num_ignore_punct (provider = icu, deterministic = false, locale = 'und-u-ka-shifted-kn');
SELECT 'id-45' < 'id-123' COLLATE num_ignore_punct;  -- true
SELECT 'w;x*y-z' = 'wxyz' COLLATE num_ignore_punct;  -- true
```

---

##### 23.2.3.1. ICU 비교 수준

ICU 콜레이션 수준별 비교 결과(표 23.1 대응):

- level1 (기본 문자): `'f'='f'` true · `'ab'=U&'a\2063b'` true · `'x-y'='x_y'` true · `'g'='G'` true · `'n'='ñ'` true · `'y'='z'` false
- level2 (악센트): `'f'='f'` true · `'ab'=U&'a\2063b'` true · `'x-y'='x_y'` true · `'g'='G'` true · `'n'='ñ'` false · `'y'='z'` false
- level3 (대소문자/변형): `'f'='f'` true · `'ab'=U&'a\2063b'` true · `'x-y'='x_y'` true · `'g'='G'` false · `'n'='ñ'` false · `'y'='z'` false
- level4 (구두점): `'f'='f'` true · `'ab'=U&'a\2063b'` true · `'x-y'='x_y'` false · `'g'='G'` false · `'n'='ñ'` false · `'y'='z'` false
- identic (모두): `'f'='f'` true · `'ab'=U&'a\2063b'` false · `'x-y'='x_y'` false · `'g'='G'` false · `'n'='ñ'` false · `'y'='z'` false

예제:
```sql
CREATE COLLATION level3 (provider = icu, deterministic = false, locale = 'und-u-ka-shifted-ks-level3');
CREATE COLLATION level4 (provider = icu, deterministic = false, locale = 'und-u-ka-shifted-ks-level4');
CREATE COLLATION identic (provider = icu, deterministic = false, locale = 'und-u-ka-shifted-ks-identic');

-- 보이지 않는 구분자는 identic을 제외한 모든 수준에서 무시됨
SELECT 'ab' = U&'a\\2063b' COLLATE level4;  -- true
SELECT 'ab' = U&'a\\2063b' COLLATE identic;  -- false

-- 구두점은 level3에서는 무시되지만 level4에서는 아님
SELECT 'x-y' = 'x_y' COLLATE level3;  -- true
SELECT 'x-y' = 'x_y' COLLATE level4;  -- false
```

---

##### 23.2.3.2. ICU 로케일의 콜레이션 설정

ICU 콜레이션 설정 키(표 23.2 대응):

- `co` (콜레이션 유형): 값 `emoji`, `phonebk`, `standard` 등, 기본값 `standard`
- `ka` (구두점/공백 무시, `ks` ≤ level3일 때): 값 `noignore`, `shifted`, 기본값 `noignore`
- `kb` (역방향 레벨 2 비교): 값 `true`/`false`, 기본값 `false`
- `kc` (대소문자를 "레벨 2.5"로 분리): 값 `true`/`false`, 기본값 `false`
- `kf` (대/소문자 정렬 순서): 값 `upper`/`lower`/`false`, 기본값 `false`
- `kn` (숫자를 숫자 값으로 처리): 값 `true`/`false`, 기본값 `false`
- `kk` (전체 정규화 활성화): 값 `true`/`false`, 기본값 `false`
- `kr` (문자 클래스 순서 재정의): 값 `space`, `punct`, `symbol`, `currency`, `digit`, script-id, 기본값 없음
- `ks` (민감도/강도): 값 `level1`, `level2`, `level3`, `level4`, `identic`, 기본값 `level3`
- `kv` (레벨 3에서 무시할 문자 클래스): 값 `space`, `punct`, `symbol`, `currency`, 기본값 `punct`

---

##### 23.2.3.3. 콜레이션 설정 예제

```sql
-- 독일어 전화번호부 콜레이션
CREATE COLLATION "de-u-co-phonebk-x-icu" (provider = icu, locale = 'de-u-co-phonebk');

-- 이모지 콜레이션
CREATE COLLATION "und-u-co-emoji-x-icu" (provider = icu, locale = 'und-u-co-emoji');

-- 그리스 문자가 라틴 문자보다 먼저
CREATE COLLATION latinlast (provider = icu, locale = 'en-u-kr-grek-latn');

-- 대문자가 소문자보다 먼저
CREATE COLLATION upperfirst (provider = icu, locale = 'en-u-kf-upper');

-- 결합된 옵션
CREATE COLLATION special (provider = icu, locale = 'en-u-kf-upper-kr-grek-latn');
```

---

##### 23.2.3.4. ICU 맞춤 규칙

맞춤 규칙을 사용한 사용자 정의 콜레이션 요소 순서:

```sql
-- 간단한 예: W가 V 뒤에 정렬
CREATE COLLATION custom (provider = icu, locale = 'und', rules = '&V << w <<< W');

-- 복잡한 예: EBCDIC 문자 순서
CREATE COLLATION ebcdic (provider = icu, locale = 'und',
rules = $$
& ' ' < '.' < '<' < '(' < '+' < \|
< '&' < '!' < '$' < '*' < ')' < ';'
< '-' < '/' < ',' < '%' < '_' < '>' < '?'
< '`' < ':' < '#' < '@' < \' < '=' < '"'
<*a-r < '~' <*s-z < '^' < '[' < ']'
< '{' <*A-I < '}' <*J-R < '\' <*S-Z <*0-9
$$);

SELECT c
FROM (VALUES ('a'), ('b'), ('A'), ('B'), ('1'), ('2'), ('!'), ('^')) AS x(c)
ORDER BY c COLLATE ebcdic;
 c
---
 !
 a
 b
 ^
 A
 B
 1
 2
```

---

##### 23.2.3.5. ICU 외부 참조

- [Unicode Technical Standard #35](https://www.unicode.org/reports/tr35/tr35-collation.html)
- [BCP 47](https://www.rfc-editor.org/info/bcp47)
- [CLDR 저장소](https://github.com/unicode-org/cldr/blob/master/common/bcp47/collation.xml)
- [ICU 로케일 가이드](https://unicode-org.github.io/icu/userguide/locale/)
- [ICU 콜레이션 가이드](https://unicode-org.github.io/icu/userguide/collation/)
- [ICU 맞춤 규칙](https://unicode-org.github.io/icu/userguide/collation/customization/)

---

### 23.3. 문자 집합 지원

PostgreSQL의 문자 집합 지원을 통해 다양한 문자 집합(인코딩)으로 텍스트 저장 가능:
- 단일 바이트 문자 집합: ISO 8859 시리즈
- 다중 바이트 문자 집합: EUC (Extended Unix Code), UTF-8, Mule internal code

지원되는 모든 문자 집합은 클라이언트에서 투명하게 사용 가능하지만, 일부는 서버 측 사용에 지원되지 않음.

#### 주요 제약

각 데이터베이스의 문자 집합은 데이터베이스의 `LC_CTYPE`(문자 분류) 및 `LC_COLLATE`(문자열 정렬 순서) 로케일 설정과 호환되어야 함:
- `C` 또는 `POSIX` 로케일의 경우: 모든 문자 집합 허용
- 다른 libc 제공 로케일의 경우: 하나의 문자 집합만 올바르게 작동
- Windows에서: UTF-8 인코딩을 모든 로케일과 함께 사용 가능
- ICU 지원이 있는 경우: ICU 제공 로케일은 대부분의(전부는 아닌) 서버 측 인코딩과 작동

---

#### 23.3.1. 지원되는 문자 집합

PostgreSQL 문자 집합 목록(표 23.3 대응, 각 항목은 이름 / 설명 / 언어 / 서버 지원 여부 / ICU 지원 여부 / 바이트-문자 / 별칭 순):

- `BIG5` (Big Five): 번체 중국어, 서버 미지원, ICU 미지원, 1-2바이트, 별칭 `WIN950`·`Windows950`
- `EUC_CN` (Extended UNIX Code-CN): 간체 중국어, 서버 지원, ICU 지원, 1-3바이트
- `EUC_JP` (Extended UNIX Code-JP): 일본어, 서버 지원, ICU 지원, 1-3바이트
- `EUC_JIS_2004` (Extended UNIX Code-JP, JIS X 0213): 일본어, 서버 지원, ICU 미지원, 1-3바이트
- `EUC_KR` (Extended UNIX Code-KR): 한국어, 서버 지원, ICU 지원, 1-3바이트
- `EUC_TW` (Extended UNIX Code-TW): 번체 중국어·대만어, 서버 지원, ICU 지원, 1-4바이트
- `GB18030` (국가 표준): 중국어, 서버 미지원, ICU 미지원, 1-4바이트
- `GBK` (확장 국가 표준): 간체 중국어, 서버 미지원, ICU 미지원, 1-2바이트, 별칭 `WIN936`·`Windows936`
- `ISO_8859_5` (ISO 8859-5, ECMA 113): 라틴/키릴, 서버 지원, ICU 지원, 1바이트
- `ISO_8859_6` (ISO 8859-6, ECMA 114): 라틴/아랍어, 서버 지원, ICU 지원, 1바이트
- `ISO_8859_7` (ISO 8859-7, ECMA 118): 라틴/그리스어, 서버 지원, ICU 지원, 1바이트
- `ISO_8859_8` (ISO 8859-8, ECMA 121): 라틴/히브리어, 서버 지원, ICU 지원, 1바이트
- `JOHAB` (JOHAB): 한국어(한글), 서버 미지원, ICU 미지원, 1-3바이트
- `KOI8R` (KOI8-R): 키릴(러시아어), 서버 지원, ICU 지원, 1바이트, 별칭 `KOI8`
- `KOI8U` (KOI8-U): 키릴(우크라이나어), 서버 지원, ICU 지원, 1바이트
- `LATIN1` (ISO 8859-1, ECMA 94): 서유럽어, 서버 지원, ICU 지원, 1바이트, 별칭 `ISO88591`
- `LATIN2` (ISO 8859-2, ECMA 94): 중앙 유럽어, 서버 지원, ICU 지원, 1바이트, 별칭 `ISO88592`
- `LATIN3` (ISO 8859-3, ECMA 94): 남유럽어, 서버 지원, ICU 지원, 1바이트, 별칭 `ISO88593`
- `LATIN4` (ISO 8859-4, ECMA 94): 북유럽어, 서버 지원, ICU 지원, 1바이트, 별칭 `ISO88594`
- `LATIN5` (ISO 8859-9, ECMA 128): 터키어, 서버 지원, ICU 지원, 1바이트, 별칭 `ISO88599`
- `LATIN6` (ISO 8859-10, ECMA 144): 노르딕, 서버 지원, ICU 지원, 1바이트, 별칭 `ISO885910`
- `LATIN7` (ISO 8859-13): 발트어, 서버 지원, ICU 지원, 1바이트, 별칭 `ISO885913`
- `LATIN8` (ISO 8859-14): 켈트어, 서버 지원, ICU 지원, 1바이트, 별칭 `ISO885914`
- `LATIN9` (ISO 8859-15): 유로 및 악센트가 포함된 LATIN1, 서버 지원, ICU 지원, 1바이트, 별칭 `ISO885915`
- `LATIN10` (ISO 8859-16, ASRO SR 14111): 루마니아어, 서버 지원, ICU 미지원, 1바이트, 별칭 `ISO885916`
- `MULE_INTERNAL` (Mule internal code): 다국어 Emacs, 서버 지원, ICU 미지원, 1-4바이트
- `SJIS` (Shift JIS): 일본어, 서버 미지원, ICU 미지원, 1-2바이트, 별칭 `Mskanji`·`ShiftJIS`·`WIN932`·`Windows932`
- `SHIFT_JIS_2004` (Shift JIS, JIS X 0213): 일본어, 서버 미지원, ICU 미지원, 1-2바이트
- `SQL_ASCII` (미지정, 텍스트 참조): 모두, 서버 지원, ICU 미지원, 1바이트
- `UHC` (Unified Hangul Code): 한국어, 서버 미지원, ICU 미지원, 1-2바이트, 별칭 `WIN949`·`Windows949`
- `UTF8` (Unicode, 8비트): 모두, 서버 지원, ICU 지원, 1-4바이트, 별칭 `Unicode`
- `WIN866` (Windows CP866): 키릴, 서버 지원, ICU 지원, 1바이트, 별칭 `ALT`
- `WIN874` (Windows CP874): 태국어, 서버 지원, ICU 미지원, 1바이트
- `WIN1250` (Windows CP1250): 중앙 유럽어, 서버 지원, ICU 지원, 1바이트
- `WIN1251` (Windows CP1251): 키릴, 서버 지원, ICU 지원, 1바이트, 별칭 `WIN`
- `WIN1252` (Windows CP1252): 서유럽어, 서버 지원, ICU 지원, 1바이트
- `WIN1253` (Windows CP1253): 그리스어, 서버 지원, ICU 지원, 1바이트
- `WIN1254` (Windows CP1254): 터키어, 서버 지원, ICU 지원, 1바이트
- `WIN1255` (Windows CP1255): 히브리어, 서버 지원, ICU 지원, 1바이트
- `WIN1256` (Windows CP1256): 아랍어, 서버 지원, ICU 지원, 1바이트
- `WIN1257` (Windows CP1257): 발트어, 서버 지원, ICU 지원, 1바이트
- `WIN1258` (Windows CP1258): 베트남어, 서버 지원, ICU 미지원, 1바이트, 별칭 `ABC`·`TCVN`·`TCVN5712`·`VSCII`

##### SQL_ASCII에 대한 중요 참고사항

`SQL_ASCII` 설정은 다른 설정과 다르게 동작함:
- 서버는 바이트 값 0-127을 ASCII 표준에 따라 해석함
- 바이트 값 128-255는 해석되지 않은 문자로 취급됨
- 인코딩 변환이 발생하지 않음
- ASCII를 사용한다는 선언이 아니라 인코딩에 대한 무지의 선언
- 비ASCII 데이터에 `SQL_ASCII`를 사용하는 것은 권장하지 않음. PostgreSQL이 비ASCII 문자를 변환하거나 검증할 수 없기 때문.

---

#### 23.3.2. 문자 집합 설정

##### initdb 사용

PostgreSQL 클러스터의 기본 문자 집합 정의:

```bash
initdb -E EUC_JP
```

또는 긴 형식 사용:

```bash
initdb --encoding EUC_JP
```

`-E` 또는 `--encoding` 옵션을 생략하면 `initdb`가 지정된 로케일 또는 기본 로케일을 기반으로 적절한 인코딩을 자동으로 결정함.

##### 비기본 인코딩으로 데이터베이스 생성

특정 인코딩과 로케일로 데이터베이스 생성(호환되어야 함):

```bash
createdb -E EUC_KR -T template0 --lc-collate=ko_KR.euckr --lc-ctype=ko_KR.euckr korean
```

또는 SQL 사용:

```sql
CREATE DATABASE korean WITH ENCODING 'EUC_KR'
  LC_COLLATE='ko_KR.euckr'
  LC_CTYPE='ko_KR.euckr'
  TEMPLATE=template0;
```

##### 중요한 제약

비기본 인코딩이나 로케일을 지정할 때는 반드시 `template0`을 템플릿으로 지정 필요. 다른 데이터베이스를 복사할 경우 소스 데이터베이스의 인코딩 및 로케일 설정을 변경할 수 없으며, 무리하게 변경하면 데이터 손상 가능.

##### 데이터베이스 인코딩 보기

`psql`을 사용하여 데이터베이스의 인코딩 확인:

```bash
$ psql -l
```

출력 예:

```
                                     List of databases
   Name    |  Owner   | Encoding  |  Collation  |    Ctype    |          Access Privileges
-----------+----------+-----------+-------------+-------------+-------------------------------------
 clocaledb | hlinnaka | SQL_ASCII | C           | C           |
 englishdb | hlinnaka | UTF8      | en_GB.UTF8  | en_GB.UTF8  |
 japanese  | hlinnaka | UTF8      | ja_JP.UTF8  | ja_JP.UTF8  |
 korean    | hlinnaka | EUC_KR    | ko_KR.euckr | ko_KR.euckr |
 postgres  | hlinnaka | UTF8      | fi_FI.UTF8  | fi_FI.UTF8  |
 template0 | hlinnaka | UTF8      | fi_FI.UTF8  | fi_FI.UTF8  | {=c/hlinnaka,hlinnaka=CTc/hlinnaka}
 template1 | hlinnaka | UTF8      | fi_FI.UTF8  | fi_FI.UTF8  | {=c/hlinnaka,hlinnaka=CTc/hlinnaka}
(7 rows)
```

또는 psql에서:

```
\l
```

##### 중요 경고

1. LC_CTYPE/인코딩 일치: 대부분의 현대 운영 체제에서 PostgreSQL은 `LC_CTYPE` 설정에 의해 암시되는 문자 집합을 결정하고 일치하는 데이터베이스 인코딩만 사용되도록 강제함. 오래된 시스템에서는 호환성 확인 필요.

2. SQL_ASCII 위험: PostgreSQL은 `LC_CTYPE`이 `C` 또는 `POSIX`가 아닌 경우에도 슈퍼유저가 `SQL_ASCII` 인코딩으로 데이터베이스를 생성하도록 허용함. 그러나 `SQL_ASCII`는 특정 인코딩을 강제하지 않으므로 로케일 종속 오동작 발생 가능 → 사용 비권장.

---

#### 23.3.3. 서버와 클라이언트 간 자동 문자 집합 변환

PostgreSQL은 많은 조합에 대해 서버와 클라이언트 간 자동 문자 집합 변환을 지원함 (Section 23.3.4 참조).

##### 자동 변환 활성화 방법

###### 1. psql `\encoding` 명령 사용

클라이언트 인코딩을 즉시 변경:

```
\encoding SJIS
```

###### 2. libpq 함수 사용

libpq (Section 32.11)는 클라이언트 인코딩을 제어하는 함수를 제공함.

###### 3. SQL 명령 사용

SQL을 사용하여 클라이언트 인코딩 설정:

```sql
SET CLIENT_ENCODING TO 'value';
```

또는 표준 SQL 구문 사용:

```sql
SET NAMES 'value';
```

현재 클라이언트 인코딩 쿼리:

```sql
SHOW client_encoding;
```

기본 인코딩으로 복귀:

```sql
RESET client_encoding;
```

###### 4. PGCLIENTENCODING 환경 변수 사용

연결 시 자동으로 인코딩을 선택하도록 클라이언트 환경에서 정의:

```bash
export PGCLIENTENCODING=SJIS
```

###### 5. client_encoding 구성 변수 사용

연결 시 자동으로 인코딩을 선택하도록 설정함. 다른 방법으로 재정의 가능.

##### 오류 처리

특정 문자의 변환이 불가능한 경우 오류 발생. 예:
- 서버 인코딩: `EUC_JP`
- 클라이언트 인코딩: `LATIN1`
- 문제: `LATIN1` 표현이 없는 일본어 문자가 오류를 발생시킴

##### SQL_ASCII 클라이언트 동작

클라이언트 문자 집합이 `SQL_ASCII`인 경우:
- 서버의 문자 집합에 관계없이 인코딩 변환이 비활성화됨
- 서버의 문자 집합이 `SQL_ASCII`가 아닌 경우, 서버는 여전히 들어오는 데이터가 해당 인코딩에 유효한지 확인함
- 순 효과: 클라이언트 문자 집합이 서버의 것과 일치하는 것처럼 동작
- 용도: 모든 ASCII 데이터로 작업할 때만 적합

---

#### 23.3.4. 사용 가능한 문자 집합 변환

PostgreSQL은 `pg_conversion` 시스템 카탈로그에 변환 함수가 등록된 문자 집합 간 변환을 지원하며, 다양한 변환이 사전에 내장되어 있음.

##### 내장 클라이언트/서버 문자 집합 변환

서버 문자 집합별 사용 가능한 클라이언트 문자 집합(표 23.4 대응):

- `BIG5`: 서버 인코딩으로 지원되지 않음
- `EUC_CN`: EUC_CN, MULE_INTERNAL, UTF8
- `EUC_JP`: EUC_JP, MULE_INTERNAL, SJIS, UTF8
- `EUC_JIS_2004`: EUC_JIS_2004, SHIFT_JIS_2004, UTF8
- `EUC_KR`: EUC_KR, MULE_INTERNAL, UTF8
- `EUC_TW`: EUC_TW, BIG5, MULE_INTERNAL, UTF8
- `GB18030`: 서버 인코딩으로 지원되지 않음
- `GBK`: 서버 인코딩으로 지원되지 않음
- `ISO_8859_5`: ISO_8859_5, KOI8R, MULE_INTERNAL, UTF8, WIN866, WIN1251
- `ISO_8859_6`: ISO_8859_6, UTF8
- `ISO_8859_7`: ISO_8859_7, UTF8
- `ISO_8859_8`: ISO_8859_8, UTF8
- `JOHAB`: 서버 인코딩으로 지원되지 않음
- `KOI8R`: KOI8R, ISO_8859_5, MULE_INTERNAL, UTF8, WIN866, WIN1251
- `KOI8U`: KOI8U, UTF8
- `LATIN1`: LATIN1, MULE_INTERNAL, UTF8
- `LATIN2`: LATIN2, MULE_INTERNAL, UTF8, WIN1250
- `LATIN3`: LATIN3, MULE_INTERNAL, UTF8
- `LATIN4`: LATIN4, MULE_INTERNAL, UTF8
- `LATIN5`: LATIN5, UTF8
- `LATIN6`: LATIN6, UTF8
- `LATIN7`: LATIN7, UTF8
- `LATIN8`: LATIN8, UTF8
- `LATIN9`: LATIN9, UTF8
- `LATIN10`: LATIN10, UTF8
- `MULE_INTERNAL`: MULE_INTERNAL, BIG5, EUC_CN, EUC_JP, EUC_KR, EUC_TW, ISO_8859_5, KOI8R, LATIN1-4, SJIS, WIN866, WIN1250, WIN1251
- `SJIS`: 서버 인코딩으로 지원되지 않음
- `SHIFT_JIS_2004`: 서버 인코딩으로 지원되지 않음
- `SQL_ASCII`: 모두 (변환이 수행되지 않음)
- `UHC`: 서버 인코딩으로 지원되지 않음
- `UTF8`: 지원되는 모든 인코딩
- `WIN866`: WIN866, ISO_8859_5, KOI8R, MULE_INTERNAL, UTF8, WIN1251
- `WIN874`: WIN874, UTF8
- `WIN1250`: WIN1250, LATIN2, MULE_INTERNAL, UTF8
- `WIN1251`: WIN1251, ISO_8859_5, KOI8R, MULE_INTERNAL, UTF8, WIN866
- `WIN1252`: WIN1252, UTF8
- `WIN1253`: WIN1253, UTF8
- `WIN1254`: WIN1254, UTF8
- `WIN1255`: WIN1255, UTF8
- `WIN1256`: WIN1256, UTF8
- `WIN1257`: WIN1257, UTF8
- `WIN1258`: WIN1258, UTF8

##### 사용자 정의 변환 생성

다음을 사용하여 새 변환 생성:

```sql
CREATE CONVERSION
```

클라이언트/서버 자동 변환에 사용하려면 해당 문자 집합 쌍에 대해 변환이 "기본(default)"으로 표시되어야 함.

##### 모든 내장 문자 집합 변환

내장 문자 집합 변환 예시(표 23.5 대응, 변환 이름 → 소스 인코딩 / 대상 인코딩):

- `big5_to_euc_tw`: BIG5 → EUC_TW
- `euc_jp_to_sjis`: EUC_JP → SJIS
- `euc_jp_to_utf8`: EUC_JP → UTF8
- `utf8_to_euc_jp`: UTF8 → EUC_JP
- `sjis_to_utf8`: SJIS → UTF8
- `utf8_to_sjis`: UTF8 → SJIS
- `iso_8859_1_to_utf8`: LATIN1 → UTF8
- `utf8_to_iso_8859_1`: UTF8 → LATIN1
- `windows_1250_to_utf8`: WIN1250 → UTF8
- `utf8_to_windows_1250`: UTF8 → WIN1250

공식 문서에서 100개 이상 변환의 전체 목록 확인 가능.

---

#### 23.3.5. 추가 자료

##### 권장 자료

1. CJKV Information Processing: Chinese, Japanese, Korean & Vietnamese Computing
   - `EUC_JP`, `EUC_CN`, `EUC_KR`, `EUC_TW`에 대한 자세한 설명 포함

2. Unicode Consortium
   - 웹사이트: https://www.unicode.org/

3. RFC 3629
   - UTF-8 (8비트 UCS/유니코드 변환 형식) 정의
   - URL: https://datatracker.ietf.org/doc/html/rfc3629

---

### 요약 표: 핵심 사항

- 기본 설정: `initdb -E`로 지정
- 데이터베이스별: 생성 시 데이터베이스별로 재정의 가능
- 로케일 호환성: LC_CTYPE 및 LC_COLLATE와 일치 필요
- 서버 인코딩: 서버 측 사용에 23개 지원
- 클라이언트 인코딩: 클라이언트 측에서 39개 이상의 인코딩 지원
- 자동 변환: SET NAMES 또는 \encoding을 통해 활성화
- 변환 방법: 100개 이상의 내장 변환 함수
- UTF-8 장점: 지원되는 모든 인코딩으로/에서 변환

---

### 참고 문서

- [PostgreSQL 18 공식 문서 - Localization](https://www.postgresql.org/docs/current/charset.html)
- [PostgreSQL 18 공식 문서 - Locale Support](https://www.postgresql.org/docs/current/locale.html)
- [PostgreSQL 18 공식 문서 - Collation Support](https://www.postgresql.org/docs/current/collation.html)
- [PostgreSQL 18 공식 문서 - Character Set Support](https://www.postgresql.org/docs/current/multibyte.html)
## 제24장. 정기적인 데이터베이스 유지보수 작업 (Routine Database Maintenance Tasks)

> PostgreSQL 18 공식 문서 번역
>
> 원문: https://www.postgresql.org/docs/current/maintenance.html

PostgreSQL 데이터베이스는 최적의 성능을 유지하기 위해 정기적인 유지보수 작업이 필요함. 여기서 다루는 작업들은 필수적이지만 반복적인 성격을 가지므로 cron 스크립트나 Windows 작업 스케줄러 같은 표준 도구로 쉽게 자동화 가능. 적절한 스크립트를 설정하고 정상적으로 실행되는지 확인하는 것은 데이터베이스 관리자의 책임임.

명백한 유지보수 작업 중 하나는 정기적인 백업 복사본 생성. 최신 백업이 없으면 재해(디스크 장애, 화재, 실수로 인한 테이블 삭제 등) 후 복구 불가. PostgreSQL에서 사용 가능한 백업 및 복구 메커니즘은 Chapter 25에서 자세히 논의함.

또 다른 주요 유지보수 작업은 정기적인 데이터베이스 "vacuuming". 이 작업은 Section 24.1에서 다룸. 밀접하게 관련된 작업으로 쿼리 플래너가 사용하는 통계 업데이트가 있으며, Section 24.1.3에서 설명함.

정기적인 관리가 필요할 수 있는 또 다른 작업은 로그 파일 관리. 이는 Section 24.3에서 논의함.

[check_postgres](https://bucardo.org/check_postgres/)는 데이터베이스 상태를 모니터링하고 비정상적인 상황을 보고하는 데 사용 가능. check_postgres는 Nagios 및 MRTG와 통합되지만 독립적으로도 실행 가능.

PostgreSQL은 다른 데이터베이스 관리 시스템에 비해 유지보수 부담이 적음. 그럼에도 이러한 작업들에 적절히 관심을 기울이면 안정적이고 생산적인 운영 환경을 갖추는 데 크게 도움이 됨.

목차
- [24.1. 정기적인 Vacuuming](#241-정기적인-vacuuming)
  - [24.1.1. Vacuuming 기초](#2411-vacuuming-기초)
  - [24.1.2. 디스크 공간 회수](#2412-디스크-공간-회수)
  - [24.1.3. 플래너 통계 업데이트](#2413-플래너-통계-업데이트)
  - [24.1.4. 가시성 맵 업데이트](#2414-가시성-맵-업데이트)
  - [24.1.5. 트랜잭션 ID Wraparound 장애 방지](#2415-트랜잭션-id-wraparound-장애-방지)
  - [24.1.6. Autovacuum 데몬](#2416-autovacuum-데몬)
- [24.2. 정기적인 재인덱싱](#242-정기적인-재인덱싱)
- [24.3. 로그 파일 유지보수](#243-로그-파일-유지보수)

---

### 24.1. 정기적인 Vacuuming

PostgreSQL 데이터베이스는 vacuuming이라고 알려진 정기적인 유지보수가 필요함. 대부분의 환경에서는 autovacuum 데몬(24.1.6 참고)이 vacuuming을 처리하도록 두는 것으로 충분함. 상황에 따라 최적의 결과를 위해 autovacuum 매개변수를 조정해야 할 수도 있음. 일부 데이터베이스 관리자는 cron이나 작업 스케줄러 스크립트를 통해 수동 `VACUUM` 명령을 실행하여 데몬의 동작을 보완하거나 대체하기도 함.

---

#### 24.1.1. Vacuuming 기초

PostgreSQL의 `VACUUM` 명령은 여러 가지 이유로 각 테이블을 정기적으로 처리해야 함.

- 업데이트되거나 삭제된 행이 차지하는 디스크 공간을 회수·재사용
- PostgreSQL 쿼리 플래너가 사용하는 데이터 통계 업데이트
- 인덱스 전용 스캔(index-only scan)의 속도를 높이는 가시성 맵 업데이트
- 트랜잭션 ID wraparound 또는 multixact ID wraparound로 인한 매우 오래된 데이터의 손실 방지

VACUUM 변형:

`VACUUM`에는 표준 `VACUUM`과 `VACUUM FULL`의 두 가지 변형이 있음.

- `VACUUM FULL`: 더 많은 디스크 공간 회수 가능하나 훨씬 더 느리게 실행됨. 테이블에 대한 `ACCESS EXCLUSIVE` 잠금을 요구하여 병렬 작업 방지함
- 표준 `VACUUM`: 프로덕션 데이터베이스 작업(SELECT, INSERT, UPDATE, DELETE가 정상적으로 계속됨)과 병렬 실행 가능. 표준 vacuuming 중에는 `ALTER TABLE`로 테이블 정의 수정 불가

성능 고려사항:

`VACUUM`은 상당한 I/O 트래픽을 생성하며, 이는 다른 활성 세션의 성능 저하를 유발할 수 있음. 백그라운드 vacuuming의 성능 영향을 줄이기 위해 구성 매개변수 조정 가능(Section 19.10.2 참고).

---

#### 24.1.2. 디스크 공간 회수

PostgreSQL에서 행을 `UPDATE`하거나 `DELETE`하면 이전 버전의 행이 즉시 제거되지 않음. 이는 다중 버전 동시성 제어(MVCC)의 이점을 활용하기 위함으로, 다른 트랜잭션에서 여전히 보일 수 있는 행 버전은 삭제 금지. 결국 오래되거나 삭제된 행 버전이 더 이상 어떤 트랜잭션에서도 필요하지 않게 되면 그 공간을 회수해야 함.

표준 VACUUM vs. VACUUM FULL:

- 표준 `VACUUM`: 테이블과 인덱스에서 죽은 행 버전을 제거하고 향후 재사용을 위해 공간을 사용 가능으로 표시함. 테이블 끝의 하나 이상의 페이지가 완전히 비어 있고 독점적인 테이블 잠금을 쉽게 얻을 수 있는 경우를 제외하고는 운영 체제에 공간을 반환하지 않음
- `VACUUM FULL`: 죽은 공간 없이 완전히 새 버전의 테이블 파일을 작성하여 테이블을 적극적으로 압축함. 테이블 크기를 최소화하지만 오랜 시간이 걸리고 작업이 완료될 때까지 새 복사본을 위한 추가 디스크 공간이 필요함

모범 사례:

정기적인 vacuuming의 기본 목표는 `VACUUM FULL`이 필요하지 않도록 표준 `VACUUM`을 충분히 자주 실행하는 것임. autovacuum 데몬도 이 방식으로 동작하며 `VACUUM FULL`을 실행하지 않음. 핵심은 테이블을 최소 크기로 유지하는 것이 아니라 디스크 공간을 안정적인 수준으로 유지하는 것임. 각 테이블은 최소 크기에 vacuum 실행 사이에 사용되는 공간을 더한 만큼의 공간을 차지하게 됨.

`VACUUM FULL`이 테이블을 최소 크기로 줄이고 디스크 공간을 운영 체제에 반환할 수 있지만, 테이블이 이후에 다시 커질 경우에는 큰 의미가 없음. 적당한 주기로 실행하는 표준 `VACUUM`이 많이 업데이트되는 테이블을 관리하는 데 더 나은 방법임.

스케줄링 접근 방식:

일부 관리자는 직접 vacuuming 일정을 관리하는 쪽을 선호함(예: 부하가 낮은 야간에 실행). 다만 테이블에 예상치 못한 업데이트가 급증하면 `VACUUM FULL`이 필요한 수준의 블로팅이 발생할 수 있다는 문제가 있음. autovacuum 데몬은 업데이트 활동에 따라 vacuuming을 동적으로 조율하여 이 문제를 완화함. 워크로드가 극도로 예측 가능한 경우가 아니라면 데몬을 완전히 비활성화하는 것은 비권장.

autovacuum을 사용하지 않는 설치의 경우 일반적인 접근 방식:
- 사용량이 낮은 시간에 하루에 한 번 데이터베이스 전체 `VACUUM` 스케줄링
- 필요에 따라 많이 업데이트되는 테이블에 대해 더 자주 vacuuming으로 보완
- 업데이트 빈도가 매우 높은 일부 설치에서는 바쁜 테이블을 몇 분마다 vacuum
- 클러스터의 여러 데이터베이스에 `vacuumdb` 프로그램 사용

> 팁: 대안 명령
>
> 테이블에 대규모 업데이트 또는 삭제 활동으로 인한 많은 수의 죽은 행 버전이 포함된 경우 `VACUUM FULL`, `CLUSTER` 또는 `ALTER TABLE`의 테이블 재작성 변형이 필요할 수 있음. 이러한 명령은 테이블의 완전히 새로운 복사본을 다시 작성하고 새 인덱스를 빌드함. 모두 `ACCESS EXCLUSIVE` 잠금이 필요하고 테이블 크기와 거의 동일한 추가 디스크 공간을 일시적으로 사용함.

> 팁: TRUNCATE 대안
>
> 테이블의 전체 내용이 주기적으로 삭제되는 경우 `DELETE` 다음에 `VACUUM`을 사용하는 것보다 `TRUNCATE` 사용을 고려할 것. `TRUNCATE`는 전체 테이블 내용을 즉시 제거하며, 후속 `VACUUM` 또는 `VACUUM FULL`이 불필요함. 단점은 엄격한 MVCC 의미론이 위반된다는 점.

---

#### 24.1.3. 플래너 통계 업데이트

PostgreSQL 쿼리 플래너는 효율적인 쿼리 계획을 생성하기 위해 테이블 내용에 대한 통계 정보에 의존함. 이 통계는 `ANALYZE` 명령으로 수집하며, 단독으로 실행하거나 `VACUUM`의 선택적 단계로 실행 가능. 통계가 충분히 정확하지 않으면 잘못된 계획이 선택되어 데이터베이스 성능이 저하될 수 있으므로 통계를 최신 상태로 유지하는 것이 중요함.

Autovacuum 동작:

autovacuum 데몬이 활성화된 경우 테이블 내용이 충분히 변경될 때마다 `ANALYZE` 명령을 자동으로 실행함. 그러나 특히 테이블의 업데이트 활동이 플래너가 관심을 갖는 열의 통계에 영향을 주지 않는 경우, 관리자가 수동으로 `ANALYZE` 일정을 관리하는 쪽을 선호할 수도 있음. 데몬은 삽입되거나 업데이트된 행 수를 기준으로 `ANALYZE`를 기계적으로 스케줄링하며, 그것이 의미 있는 통계 변화로 이어지는지는 판단하지 않음.

특수 케이스:

파티션이나 상속 자식 테이블에서 변경된 튜플은 부모 테이블에 대한 분석을 트리거하지 않음. 부모 테이블 자체에 변경이 거의 없으면 autovacuum이 이를 처리하지 않아 전체 상속 트리에 대한 통계가 수집되지 않을 수 있음. 통계를 최신 상태로 유지하려면 부모 테이블에서 `ANALYZE`를 수동으로 실행 필요.

빈도 고려사항:

자주 통계 업데이트는 거의 업데이트되지 않는 테이블보다 많이 업데이트되는 테이블에 더 유용함. 많이 업데이트되는 테이블의 경우에도 데이터의 통계적 분포가 많이 변경되지 않는 경우 통계 업데이트가 불필요할 수 있음.

경험 법칙: 테이블 열의 최소값과 최대값이 얼마나 변경되는지 생각해 볼 것. 예를 들어:
- 행 업데이트 시간을 포함하는 `timestamp` 열은 행이 추가되고 업데이트됨에 따라 지속적으로 증가하는 최대값을 가짐. 이러한 열은 아마도 더 자주 통계 업데이트가 필요함
- 웹사이트 페이지의 URL을 포함하는 열은 마찬가지로 자주 변경될 수 있지만, 값의 통계적 분포는 상대적으로 천천히 변함

유연성:

특정 테이블 또는 특정 열에 대해서만 `ANALYZE`를 실행할 수 있으므로, 일부 통계를 다른 것보다 더 자주 업데이트하는 유연성 확보 가능. 실제로는 전체 데이터베이스를 분석하는 것이 대개 가장 좋음. `ANALYZE`는 모든 행을 읽는 대신 통계적으로 무작위한 샘플링을 사용하므로 빠르게 완료됨.

> 팁: 열별 조정
>
> 열별로 `ANALYZE` 빈도를 조정하는 것은 크게 생산적이지 않을 수 있지만, `ANALYZE`가 수집하는 통계의 상세 수준을 열별로 조정하는 것은 가치가 있을 수 있음. `WHERE` 절에서 많이 사용되고 매우 불규칙한 데이터 분포를 가진 열은 다른 열보다 더 세밀한 데이터 히스토그램이 필요할 수 있음. `ALTER TABLE SET STATISTICS`를 참고하거나 `default_statistics_target` 구성 매개변수를 사용하여 데이터베이스 전체 기본값 변경할 것.

추가 통계:

기본적으로 함수 선택도에 대한 정보는 제한적임. 그러나 함수 호출을 포함하는 통계 객체나 표현식 인덱스를 생성하면 해당 함수에 대한 유용한 통계가 수집되며, 이는 표현식 인덱스를 사용하는 쿼리 계획을 크게 개선할 수 있음.

> 팁: 외부 테이블
>
> autovacuum 데몬은 외부 테이블에 대해 `ANALYZE` 명령을 발행하지 않음. 적절한 계획을 위해 쿼리가 외부 테이블의 통계를 필요로 하는 경우 적절한 일정에 따라 해당 테이블에서 수동으로 관리되는 `ANALYZE` 명령을 실행할 것.

> 팁: 파티션된 테이블
>
> autovacuum 데몬은 파티션된 테이블에 대해 `ANALYZE` 명령을 발행하지 않음. 상속 부모는 부모 자체가 변경된 경우에만 분석됨. 자식 테이블의 변경은 부모 테이블에 대한 자동 분석을 트리거하지 않음. 적절한 계획을 위해 쿼리가 부모 테이블의 통계를 필요로 하는 경우 주기적으로 해당 테이블에서 수동 `ANALYZE`를 실행하여 통계를 최신 상태로 유지할 것.

---

#### 24.1.4. 가시성 맵 업데이트

Vacuum은 각 테이블에 대해 가시성 맵(visibility map)을 유지하여, 모든 활성 트랜잭션(및 페이지가 다시 수정될 때까지 모든 미래 트랜잭션)에 보이는 튜플만 포함하는 페이지를 추적함.

두 가지 목적:

- Vacuum 최적화: Vacuum 자체가 다음 실행에서 정리할 것이 없으므로 그러한 페이지를 건너뛸 수 있음
- 인덱스 전용 스캔: PostgreSQL이 기본 테이블을 참조하지 않고 인덱스만 사용하여 일부 쿼리에 응답 가능하게 함

인덱스 전용 스캔:

PostgreSQL 인덱스에는 튜플 가시성 정보가 포함되지 않으므로 일반 인덱스 스캔은 일치하는 각 인덱스 항목에 대해 힙 튜플을 가져와 현재 트랜잭션에서 보이는지 확인함. 반면 인덱스 전용 스캔은 먼저 가시성 맵을 확인함. 페이지의 모든 튜플이 가시적인 것으로 알려진 경우 힙 페치를 건너뛸 수 있음. 이 기능은 가시성 맵이 디스크 접근을 줄여 주는 대규모 데이터셋에서 가장 유용함. 가시성 맵은 힙보다 훨씬 작으므로 힙이 매우 큰 경우에도 쉽게 캐시 가능.

---

#### 24.1.5. 트랜잭션 ID Wraparound 장애 방지

PostgreSQL의 MVCC 트랜잭션 의미론은 트랜잭션 ID(XID) 번호를 비교할 수 있다는 점에 의존함. 삽입 XID가 현재 트랜잭션의 XID보다 큰 행 버전은 "미래"에 속하며 현재 트랜잭션에 보여서는 안 됨. 트랜잭션 ID는 크기가 제한(32비트)되어 있으므로 오랫동안 실행된 클러스터(40억 개 이상의 트랜잭션)는 트랜잭션 ID wraparound를 겪게 됨. XID 카운터가 0으로 다시 돌아가면 과거에 속하던 트랜잭션이 갑자기 미래에 있는 것처럼 보이게 되고, 해당 데이터는 보이지 않게 됨. 데이터 자체는 남아 있지만 접근 불가능해져 치명적인 데이터 손실이 발생함.

예방:

이를 방지하려면 최소한 20억 트랜잭션마다 모든 데이터베이스의 모든 테이블을 vacuum 필요.

VACUUM이 문제를 해결하는 방법:

정기적인 vacuuming은 `VACUUM`이 행을 frozen으로 표시함으로써 이 문제를 해결함. frozen 표시는 해당 행이 모든 현재 및 미래 트랜잭션에서 확실히 볼 수 있을 만큼 충분히 오래전에 커밋된 트랜잭션에 의해 삽입되었음을 나타냄.

일반 XID 비교:

일반 XID는 모듈로-2^32 산술로 비교됨. 모든 일반 XID에는 "더 오래된" 20억 개의 XID와 "더 새로운" 20억 개의 XID가 존재함. 일반 XID 공간은 끝점이 없는 원형 구조임. 특정 일반 XID로 생성된 행 버전은 이후 20억 트랜잭션 동안 "과거"에 속하는 것으로 간주됨. 행 버전이 20억 트랜잭션을 넘어 유지되면 갑자기 미래에 있는 것처럼 보이게 됨.

FrozenTransactionId:

이를 방지하기 위해 PostgreSQL은 일반 XID 비교 규칙을 따르지 않고 항상 모든 일반 XID보다 오래된 것으로 처리되는 특수 XID인 `FrozenTransactionId`를 예약함. Frozen 행 버전은 삽입 XID가 `FrozenTransactionId`인 것처럼 취급되므로 wraparound 여부와 관계없이 모든 일반 트랜잭션에서 "과거"에 속하는 것으로 보임. 이러한 행 버전은 삭제될 때까지 얼마나 오래 유지되더라도 유효함.

> 참고: Freezing 구현
>
> PostgreSQL 9.4 이전 버전에서 freezing은 실제로 행의 삽입 XID를 `FrozenTransactionId`로 대체하여 구현되었으며, 이는 행의 `xmin` 시스템 열에서 볼 수 있었음. 새 버전에서는 플래그 비트만 설정하여 포렌식 사용을 위해 행의 원래 `xmin`을 보존함. 그러나 `xmin`이 `FrozenTransactionId`(2)와 같은 행은 9.4 이전 버전에서 pg_upgrade된 데이터베이스에서 여전히 찾을 수 있음.
>
> 시스템 카탈로그에도 `xmin`이 `BootstrapTransactionId`(1)와 같은 행이 포함될 수 있으며, 이는 initdb의 첫 번째 단계 중에 삽입되었음을 나타냄. `FrozenTransactionId`와 마찬가지로 이 특수 XID는 모든 일반 XID보다 오래된 것으로 처리됨.

구성 매개변수:

`vacuum_freeze_min_age`는 행이 frozen되기 전에 XID 값이 최소한 얼마나 오래되어야 하는지를 제어함. 이 값을 높이면 곧 다시 수정될 행을 불필요하게 frozen하는 작업을 줄일 수 있지만, 낮추면 테이블을 다시 vacuum해야 하기 전까지 허용되는 트랜잭션 수가 늘어남.

가시성 맵 사용:

`VACUUM`은 가시성 맵을 사용하여 테이블의 어떤 페이지를 스캔해야 하는지 결정함. 일반적으로 죽은 행 버전이 없는 페이지는 해당 페이지에 오래된 XID 값을 가진 행 버전이 있더라도 건너뜀. 따라서 일반 `VACUUM`은 테이블의 모든 오래된 행 버전을 항상 frozen하지는 않음.

공격적인 Vacuum:

일반 `VACUUM`이 모든 오래된 버전을 frozen하지 않을 경우, 결국 공격적인 vacuum을 수행해야 함. 공격적인 vacuum은 all-visible이지만 all-frozen이 아닌 페이지를 포함하여 모든 적격한 unfrozen XID 및 MXID 값을 frozen함.

테이블에 all-visible이지만 all-frozen이 아닌 페이지가 쌓이는 경우, 일반 vacuum은 그렇지 않으면 건너뛸 수 있는 페이지를 frozen 시도를 위해 스캔하도록 선택할 수 있음. 이를 통해 다음 공격적인 vacuum이 스캔해야 하는 페이지 수를 줄임. 이렇게 스캔된 페이지를 eager scanning된 페이지라고 함. eager scanning은 `vacuum_max_eager_freeze_failure_rate`를 높여 더 많은 all-visible 페이지를 frozen하도록 조정 가능. eager scanning이 all-visible이지만 all-frozen이 아닌 페이지 수를 최소화하더라도, 대부분의 테이블은 여전히 주기적인 공격적인 vacuuming이 필요함. 그러나 eager freezing된 페이지는 공격적인 vacuum 중에 건너뛸 수 있으므로, eager freezing은 공격적인 vacuum의 오버헤드를 최소화하는 데 도움이 됨.

공격적인 Vacuum 타이밍:

`vacuum_freeze_table_age`는 테이블에 대해 공격적인 vacuum을 수행하는 시점을 제어함. 마지막 스캔 이후 경과한 트랜잭션 수가 `vacuum_freeze_table_age`에서 `vacuum_freeze_min_age`를 뺀 값보다 크면 all-visible이지만 all-frozen이 아닌 모든 페이지가 스캔됨. `vacuum_freeze_table_age`를 0으로 설정하면 `VACUUM`이 항상 공격적인 전략을 사용하도록 강제함.

최대 Vacuum되지 않은 시간:

테이블이 vacuum되지 않아도 되는 최대 기간은 20억 트랜잭션에서 마지막 공격적인 vacuum 당시의 `vacuum_freeze_min_age` 값을 뺀 것임. 이 기간을 초과하면 데이터 손실이 발생할 수 있음. 이를 방지하기 위해 `autovacuum_freeze_max_age` 설정값보다 오래된 XID를 가진 unfrozen 행이 있는 모든 테이블에서 autovacuum이 강제 호출됨(autovacuum이 비활성화된 경우에도 발생함).

즉, 테이블이 그 외에 vacuum되지 않는다면 대략 `autovacuum_freeze_max_age`에서 `vacuum_freeze_min_age`를 뺀 트랜잭션 수마다 autovacuum이 호출됨. 공간 회수를 위해 정기적으로 vacuum되는 테이블에서는 이 부분이 큰 문제가 되지 않음. 그러나 정적 테이블(삽입만 발생하고 업데이트나 삭제가 없는 테이블 포함)은 공간 회수를 위한 vacuum이 불필요하므로, 매우 큰 정적 테이블에서는 강제 autovacuum 간격을 최대화하는 것이 유용할 수 있음. `autovacuum_freeze_max_age`를 높이거나 `vacuum_freeze_min_age`를 낮춰 조정 가능.

vacuum_freeze_table_age 유효 최대값:

`vacuum_freeze_table_age`의 유효 최대값은 0.95 × `autovacuum_freeze_max_age`임. 이보다 높은 설정은 최대값으로 제한됨. `autovacuum_freeze_max_age`보다 높은 값은 의미가 없는데, 어차피 그 시점에서 anti-wraparound autovacuum이 트리거되기 때문임. 0.95 배수는 그 전에 수동 `VACUUM`을 실행할 수 있는 여유를 남기기 위함.

경험 법칙:

`vacuum_freeze_table_age`는 `autovacuum_freeze_max_age`보다 다소 낮은 값으로 설정하여, 정기적으로 예약된 `VACUUM` 또는 일반 삭제·업데이트 활동으로 트리거된 autovacuum이 그 범위 안에서 실행될 수 있도록 충분한 여유를 남겨야 함. 너무 근접하게 설정하면 테이블이 최근에 공간 회수를 위해 vacuum되었음에도 anti-wraparound autovacuum이 발생할 수 있으며, 너무 낮은 값은 공격적인 vacuuming이 더 자주 발생하게 함.

스토리지 영향:

`autovacuum_freeze_max_age`(그리고 `vacuum_freeze_table_age`)를 높이는 유일한 단점은 데이터베이스 클러스터의 `pg_xact` 및 `pg_commit_ts` 하위 디렉토리가 더 많은 공간을 차지한다는 점임. `autovacuum_freeze_max_age` 한계까지 모든 트랜잭션의 커밋 상태와(`track_commit_timestamp`가 활성화된 경우 타임스탬프도) 저장해야 하기 때문임. 커밋 상태는 트랜잭션당 2비트를 사용하므로:
- `autovacuum_freeze_max_age`가 최대 허용값인 20억으로 설정된 경우 `pg_xact`는 약 0.5GB까지, `pg_commit_ts`는 약 20GB까지 커질 것으로 예상됨
- 이것이 총 데이터베이스 크기에 비해 미미하다면 `autovacuum_freeze_max_age`를 최대 허용 값으로 설정하는 것이 좋음
- 그렇지 않으면 `pg_xact` 및 `pg_commit_ts` 스토리지에 허용할 수 있는 것에 따라 설정 필요
- 기본값인 2억 트랜잭션은 약 50MB의 `pg_xact` 스토리지와 약 2GB의 `pg_commit_ts` 스토리지로 환산됨

vacuum_freeze_min_age 감소의 단점:

`vacuum_freeze_min_age`를 낮추면 `VACUUM`이 불필요한 작업을 수행하게 될 수 있음. 곧 수정되어 새 XID를 부여받을 행을 frozen하는 것은 시간 낭비이기 때문임. 따라서 이 값은 행이 더 이상 변경될 가능성이 없을 때까지 frozen되지 않도록 충분히 크게 설정해야 함.

XID 나이 추적:

데이터베이스에서 가장 오래된 unfrozen XID의 나이를 추적하기 위해 `VACUUM`은 시스템 테이블 `pg_class` 및 `pg_database`에 XID 통계를 저장함. 구체적으로:
- 테이블의 `pg_class` 행의 `relfrozenxid` 열은 `relfrozenxid`를 성공적으로 진행시킨 가장 최근 `VACUUM`(일반적으로 가장 최근의 공격적인 VACUUM) 끝에 남아 있는 가장 오래된 unfrozen XID를 포함함
- 데이터베이스의 `pg_database` 행의 `datfrozenxid` 열은 해당 데이터베이스에 나타나는 unfrozen XID의 하한임. 이것은 단순히 데이터베이스 내의 테이블별 `relfrozenxid` 값의 최소값임

예시 쿼리:

이 정보를 검사하는 편리한 방법:

```sql
SELECT c.oid::regclass as table_name,
       greatest(age(c.relfrozenxid),age(t.relfrozenxid)) as age
FROM pg_class c
LEFT JOIN pg_class t ON c.reltoastrelid = t.oid
WHERE c.relkind IN ('r', 'm');

SELECT datname, age(datfrozenxid) FROM pg_database;
```

`age` 열은 컷오프 XID에서 현재 트랜잭션의 XID까지의 트랜잭션 수를 측정함.

> 팁: VERBOSE 매개변수
>
> `VACUUM` 명령의 `VERBOSE` 매개변수가 지정되면 `VACUUM`은 테이블에 대한 다양한 통계를 출력함. 여기에는 `relfrozenxid` 및 `relminmxid`가 어떻게 진행되었는지와 새로 frozen된 페이지 수에 대한 정보가 포함됨. autovacuum 로깅(`log_autovacuum_min_duration`으로 제어됨)이 autovacuum이 실행한 `VACUUM` 작업에 대해 보고할 때 동일한 세부 정보가 서버 로그에 나타남.

relfrozenxid 진행:

`VACUUM`이 마지막 vacuum 이후 수정된 페이지를 주로 스캔하는 동안 frozen하려는 시도로 all-visible이지만 all-frozen이 아닌 일부 페이지를 eager 스캔할 수도 있음. 그러나 `relfrozenxid`는 unfrozen XID를 포함할 수 있는 테이블의 모든 페이지가 스캔될 때만 진행됨. 이것은 다음과 같은 경우에 발생함:
- `relfrozenxid`가 `vacuum_freeze_table_age` 트랜잭션보다 오래된 경우
- `VACUUM`의 `FREEZE` 옵션이 사용된 경우
- 아직 all-frozen이 아닌 모든 페이지가 죽은 행 버전을 제거하기 위해 vacuuming이 필요한 경우

`VACUUM`이 아직 all-frozen이 아닌 테이블의 모든 페이지를 스캔할 때 `age(relfrozenxid)`를 사용된 `vacuum_freeze_min_age` 설정보다 약간 더 큰 값으로 설정하게 됨(`VACUUM`이 시작된 이후 시작된 트랜잭션 수만큼 더). `VACUUM`은 `relfrozenxid`를 테이블에 남아 있는 가장 오래된 XID로 설정하므로 최종 값이 엄격하게 필요한 것보다 훨씬 더 최근일 수 있음.

`autovacuum_freeze_max_age`에 도달할 때까지 테이블에서 `relfrozenxid`를 진행시키는 `VACUUM`이 발행되지 않으면 곧 테이블에 대해 autovacuum이 강제됨.

경고 메시지:

어떤 이유로 autovacuum이 테이블에서 오래된 XID를 지우지 못하면 데이터베이스의 가장 오래된 XID가 wraparound 지점에서 4천만 트랜잭션에 도달할 때 시스템이 다음과 같은 경고 메시지를 내보내기 시작함:

```
WARNING:  database "mydb" must be vacuumed within 39985967 transactions
HINT:  To avoid XID assignment failures, execute a database-wide VACUUM in that database.
```

힌트에서 제안하는 대로 수동 `VACUUM`을 실행하면 문제 해결 가능. 단, `VACUUM`은 슈퍼유저가 실행해야 함. 그렇지 않으면 시스템 카탈로그를 처리하지 못해 데이터베이스의 `datfrozenxid`를 진행시킬 수 없음.

치명적 오류:

이러한 경고가 무시되면 wraparound까지 3백만 트랜잭션 미만이 남았을 때 시스템은 새 XID 할당을 거부함:

```
ERROR:  database is not accepting commands that assign new XIDs to avoid wraparound data loss in database "mydb"
HINT:  Execute a database-wide VACUUM in that database.
```

이 상태에서 이미 진행 중인 트랜잭션은 계속 실행 가능하지만 읽기 전용 트랜잭션만 새로 시작 가능. 데이터베이스 레코드를 수정하거나 관계를 truncate하는 작업은 실패함. `VACUUM` 명령은 여전히 정상적으로 실행 가능.

> 참고: 이전 릴리스에서 때때로 권장되었던 것과 달리, 정상 작동을 복원하기 위해 postmaster를 중지하거나 단일 사용자 모드로 들어갈 필요는 없으며 바람직하지도 않음.

복구 단계:

대신 다음 단계를 따를 것.

1. 오래된 준비 완료 트랜잭션(prepared transaction)을 처리함. `pg_prepared_xacts`에서 `age(transactionid)`가 큰 행을 확인하여 찾을 수 있음. 이러한 트랜잭션은 커밋하거나 롤백 필요.

2. 오래 실행 중인 열린 트랜잭션을 종료함. `pg_stat_activity`에서 `age(backend_xid)` 또는 `age(backend_xmin)`이 큰 행을 확인하여 찾을 수 있음. 해당 트랜잭션을 커밋·롤백하거나, `pg_terminate_backend`로 세션 종료 가능.

3. 오래된 복제 슬롯을 삭제함. `pg_stat_replication`에서 `age(xmin)` 또는 `age(catalog_xmin)`이 큰 슬롯을 찾음. 대부분의 경우 이러한 슬롯은 더 이상 존재하지 않거나 오랫동안 다운된 서버를 위해 생성된 것임. 아직 연결을 시도할 수 있는 서버에 해당하는 슬롯을 삭제하면 해당 복제본을 다시 구성해야 할 수 있음.

4. 대상 데이터베이스에서 `VACUUM`을 실행함. 데이터베이스 전체에 대한 `VACUUM`이 가장 간단함. 필요한 시간을 줄이려면 `relminxid`가 가장 오래된 테이블부터 수동으로 `VACUUM` 명령을 실행하는 방법도 있음. 이 상황에서 `VACUUM FULL`은 사용 금지. XID가 필요하므로 실패하거나, 슈퍼유저 모드에서는 XID를 소비하여 wraparound 위험을 증가시킴. `VACUUM FREEZE`도 필요한 것보다 더 많은 작업을 수행하므로 사용 금지.

5. 정상 작동이 복원되면 향후 문제를 피하기 위해 대상 데이터베이스에서 autovacuum이 적절하게 구성되어 있는지 확인할 것.

> 참고: 단일 사용자 모드
>
> 이전 버전에서는 때때로 postmaster를 중지하고 단일 사용자 모드에서 데이터베이스를 `VACUUM`해야 했음. 일반적인 시나리오에서는 더 이상 필요하지 않으며 가능하면 피해야 함. 시스템을 다운시키는 것을 포함하기 때문임. 또한 데이터 손실을 방지하기 위해 설계된 트랜잭션 ID wraparound 보호 장치를 비활성화하므로 더 위험함. 이 시나리오에서 단일 사용자 모드를 사용하는 유일한 이유는 필요하지 않은 테이블을 `TRUNCATE`하거나 `DROP`하여 vacuum할 필요가 없도록 하려는 경우임. 3백만 트랜잭션 안전 마진은 관리자가 이를 수행할 수 있도록 함. 단일 사용자 모드 사용에 대한 자세한 내용은 `postgres` 참조 페이지를 참고할 것.

##### 24.1.5.1. Multixact와 Wraparound

Multixact ID 정의:

Multixact ID는 여러 트랜잭션에 의한 행 잠금을 지원하는 데 사용됨. 튜플 헤더에 잠금 정보를 저장할 공간이 제한되어 있으므로, 행을 동시에 잠그는 트랜잭션이 둘 이상인 경우 해당 정보는 "다중 트랜잭션 ID" 또는 줄여서 multixact ID로 인코딩됨. 특정 multixact ID에 포함된 트랜잭션 ID 정보는 `pg_multixact` 하위 디렉토리에 별도로 저장되며, 튜플 헤더의 `xmax` 필드에는 multixact ID만 나타남.

스토리지 및 관리:

트랜잭션 ID와 마찬가지로 multixact ID는 32비트 카운터와 해당 스토리지로 구현되며, 신중한 나이 관리, 스토리지 정리, wraparound 처리가 필요함. 각 multixact의 멤버 목록을 저장하는 별도의 스토리지 영역도 있으며, 이 영역 역시 32비트 카운터를 사용하여 관리해야 함. 시스템 함수 `pg_get_multixact_members()`(Table 9.84 참고)를 사용하여 multixact ID와 연결된 트랜잭션 ID 조회 가능.

Multixact Freezing:

`VACUUM`이 테이블의 일부를 스캔할 때마다 `vacuum_multixact_freeze_min_age`보다 오래된 multixact ID를 다른 값으로 대체함. 이 값은 다음 중 하나일 수 있음:
- 0 값
- 단일 트랜잭션 ID
- 더 새로운 multixact ID

각 테이블에 대해 `pg_class.relminmxid`는 해당 테이블의 모든 튜플에 여전히 나타날 수 있는 가장 오래된 가능한 multixact ID를 저장함. 이 값이 `vacuum_multixact_freeze_table_age`보다 오래되면 공격적인 vacuum이 강제됨. 이전 섹션에서 논의한 바와 같이 공격적인 vacuum은 all-frozen으로 알려진 페이지만 건너뜀. `mxid_age()`를 `pg_class.relminmxid`에서 사용하여 나이를 찾을 수 있음.

공격적인 Vacuum 보장:

공격적인 `VACUUM`은 원인에 관계없이 테이블의 `relminmxid`를 진행시키도록 보장됨. 결국 모든 데이터베이스의 모든 테이블이 스캔되고 가장 오래된 multixact 값이 진행되면 오래된 multixact 관련 디스크 스토리지 회수 가능.

Autovacuum 안전 장치:

안전 장치로, multixact-age가 `autovacuum_multixact_freeze_max_age`를 초과하는 모든 테이블에 대해 공격적인 vacuum 스캔이 수행됨. 또한 multixact 멤버가 차지하는 스토리지가 약 10GB를 초과하면 multixact-age가 가장 오래된 테이블부터 시작하여 모든 테이블에 대해 더 자주 공격적인 vacuum 스캔이 수행됨. 이 두 가지 공격적인 스캔은 autovacuum이 비활성화된 경우에도 발생함. 멤버 스토리지 영역은 wraparound에 도달하기 전까지 약 20GB까지 커질 수 있음.

경고 및 오류:

XID의 경우와 마찬가지로 autovacuum이 테이블에서 오래된 MXID를 지우지 못하면 데이터베이스의 가장 오래된 MXID가 wraparound 지점에서 4천만 트랜잭션에 도달할 때 시스템이 경고 메시지를 내보내기 시작함. 그리고 XID의 경우와 마찬가지로 이러한 경고가 무시되면 wraparound까지 3백만 미만이 남았을 때 시스템은 새 MXID 생성을 거부함.

복원 프로세스:

MXID가 소진된 경우 정상 작동은 XID가 소진된 경우와 거의 동일한 방법으로 복원 가능. 이전 섹션의 단계를 따르되 다음과 같은 차이점이 있음.

1. 실행 중인 트랜잭션 및 준비 완료 트랜잭션은 multixact에 나타날 가능성이 없는 경우 무시 가능함.

2. MXID 정보는 `pg_stat_activity` 같은 시스템 뷰에서 직접 확인 불가. 그러나 오래된 XID를 찾는 것이 MXID wraparound 문제를 일으키는 트랜잭션을 파악하는 데 여전히 좋은 방법임.

3. XID 소진은 모든 쓰기 트랜잭션을 차단하지만, MXID 소진은 쓰기 트랜잭션 중 MXID가 필요한 행 잠금을 포함하는 일부 트랜잭션만 차단함.

---

#### 24.1.6. Autovacuum 데몬

PostgreSQL에는 autovacuum이라는 선택적이지만 강력히 권장되는 기능이 있으며, `VACUUM` 및 `ANALYZE` 명령 실행을 자동화하는 것이 목적임. 활성화하면 autovacuum은 삽입, 업데이트 또는 삭제된 튜플 수가 많은 테이블을 감지함. 이 감지는 통계 수집 기능을 사용하므로 `track_counts`가 `true`로 설정되지 않으면 autovacuum 사용 불가. 기본 구성에서는 autovacuum이 활성화되고 관련 구성 매개변수도 적절히 설정됨.

Autovacuum 프로세스:

"autovacuum 데몬"은 실제로 여러 프로세스로 구성됨.

- Autovacuum launcher: 모든 데이터베이스에 대해 autovacuum worker 프로세스를 시작하는 영구 데몬 프로세스
- Autovacuum worker: 개별 데이터베이스에 대해 launcher에 의해 시작됨

launcher는 `autovacuum_naptime` 초마다 각 데이터베이스에서 worker 하나를 시작하려 시도하며, 시간에 걸쳐 작업을 분배함. 따라서 데이터베이스가 N개 있으면 `autovacuum_naptime`/N 초마다 새 worker가 시작됨.

동시에 실행될 수 있는 worker 프로세스의 최대 수는 `autovacuum_max_workers`로 제한됨. 처리해야 할 데이터베이스가 `autovacuum_max_workers`보다 많은 경우 첫 번째 worker가 완료되는 즉시 다음 데이터베이스가 처리됨.

Worker 처리:

각 worker 프로세스는 데이터베이스 내의 각 테이블을 확인하고 필요에 따라 `VACUUM` 및·또는 `ANALYZE`를 실행함. `log_autovacuum_min_duration`을 설정하여 autovacuum worker의 활동 모니터링 가능.

작업 분배:

짧은 시간 안에 여러 대형 테이블이 동시에 vacuuming 대상이 되면 모든 autovacuum worker가 오랫동안 해당 테이블을 처리하는 데 묶일 수 있음. 이 경우 worker가 사용 가능해질 때까지 다른 테이블과 데이터베이스는 vacuum되지 않음. 단일 데이터베이스에 할당될 수 있는 worker 수에는 제한이 없지만, worker들은 다른 worker가 이미 수행한 작업을 반복하지 않으려 함.

연결 제한:

실행 중인 worker 수는 `max_connections` 또는 `superuser_reserved_connections` 제한에 포함되지 않음.

Vacuum 결정 로직:

`relfrozenxid` 값이 `autovacuum_freeze_max_age`보다 오래된 테이블은 항상 vacuum됨(스토리지 매개변수를 통해 freeze max age가 재정의된 테이블에도 적용됨). 그 외의 경우 마지막 `VACUUM` 이후 obsolete된 튜플 수가 "vacuum 임계값"을 초과하면 테이블이 vacuum됨.

Vacuum 임계값 공식:

```
vacuum threshold = Minimum(vacuum max threshold,
                           vacuum base threshold +
                           vacuum scale factor * number of tuples)
```

여기서:
- `vacuum max threshold` = `autovacuum_vacuum_max_threshold`
- `vacuum base threshold` = `autovacuum_vacuum_threshold`
- `vacuum scale factor` = `autovacuum_vacuum_scale_factor`
- `number of tuples` = `pg_class.reltuples`

삽입 임계값:

마지막 vacuum 이후 삽입된 튜플 수가 정의된 삽입 임계값을 초과한 경우에도 테이블이 vacuum됨:

```
vacuum insert threshold = vacuum base insert threshold +
                          vacuum insert scale factor * number of tuples
```

여기서:
- `vacuum insert base threshold` = `autovacuum_vacuum_insert_threshold`
- `vacuum insert scale factor` = `autovacuum_vacuum_insert_scale_factor`

이러한 vacuum은 테이블의 일부 페이지를 all-visible로 표시하고 튜플을 frozen할 수 있으므로 이후 vacuum에서 필요한 작업량을 줄일 수 있음. `INSERT`는 발생하지만 `UPDATE`·`DELETE`가 없거나 거의 없는 테이블의 경우, 테이블의 `autovacuum_freeze_min_age`를 낮추면 더 이른 vacuum에서 튜플을 frozen할 수 있어 유용할 수 있음.

튜플 수 추적:

obsolete된 튜플 수와 삽입된 튜플 수는 누적 통계 시스템에서 가져옴. 이는 각 `UPDATE`, `DELETE`, `INSERT` 작업에 의해 업데이트되는 eventually-consistent 카운트임.

공격적인 Vacuum 트리거:

테이블의 `relfrozenxid` 값이 `vacuum_freeze_table_age` 트랜잭션보다 오래되면 오래된 튜플을 frozen하고 `relfrozenxid`를 진행시키기 위해 공격적인 vacuum이 수행됨.

Analyze 임계값:

analyze에도 유사한 조건이 사용됨. 임계값은 다음과 같이 정의됨:

```
analyze threshold = analyze base threshold +
                    analyze scale factor * number of tuples
```

이것은 마지막 `ANALYZE` 이후 삽입, 업데이트 또는 삭제된 총 튜플 수와 비교됨.

파티션된 테이블 제한:

파티션된 테이블은 튜플을 직접 저장하지 않으므로 autovacuum의 처리 대상이 아님(개별 파티션은 일반 테이블과 동일하게 처리됨). 이로 인해 autovacuum이 파티션된 테이블에서 `ANALYZE`를 실행하지 않으며, 이는 파티션된 테이블 통계를 참조하는 쿼리에 대해 최적이 아닌 계획을 초래할 수 있음. 파티션된 테이블을 처음 채울 때와 파티션의 데이터 분포가 크게 변경될 때마다 수동으로 `ANALYZE`를 실행하여 이 문제 해결 가능.

임시 테이블 제한:

임시 테이블은 autovacuum이 접근 불가. 따라서 vacuum 및 analyze 작업은 세션 내 SQL 명령으로 직접 수행 필요.

테이블별 구성:

기본 임계값 및 스케일 팩터는 `postgresql.conf`에서 가져오지만 테이블별로 재정의 가능(기타 많은 autovacuum 제어 매개변수도 마찬가지). 자세한 내용은 Storage Parameters를 참고. 테이블의 스토리지 매개변수를 통해 설정이 변경된 경우 해당 테이블을 처리할 때 그 값이 사용되며, 그렇지 않으면 전역 설정이 사용됨. 전역 설정에 대한 자세한 내용은 Section 19.10.1을 참고.

비용 지연 균형:

여러 worker가 실행 중일 때 autovacuum 비용 지연 매개변수(Section 19.10.2 참고)는 모든 실행 중인 worker 간에 "균형"을 맞추므로, 실행 중인 worker 수에 관계없이 시스템에 대한 총 I/O 영향은 동일하게 유지됨. 단, 테이블별 `autovacuum_vacuum_cost_delay` 또는 `autovacuum_vacuum_cost_limit` 스토리지 매개변수가 설정된 테이블을 처리하는 worker는 이 균형 알고리즘에서 제외됨.

잠금 동작:

Autovacuum worker는 일반적으로 다른 명령을 차단하지 않음. 다른 프로세스가 autovacuum이 보유한 `SHARE UPDATE EXCLUSIVE` 잠금과 충돌하는 잠금을 획득하려 하면 잠금 획득 시 autovacuum이 중단됨. 충돌하는 잠금 모드는 Table 13.2를 참고. 단, autovacuum이 트랜잭션 ID wraparound 방지 목적으로 실행 중인 경우(즉, `pg_stat_activity` 뷰의 autovacuum 쿼리 이름이 `(to prevent wraparound)`로 끝나는 경우)에는 자동으로 중단되지 않음.

> 경고:
>
> `SHARE UPDATE EXCLUSIVE` 잠금과 충돌하는 잠금을 획득하는 명령(예: ANALYZE)을 정기적으로 실행하면 autovacuum이 완료되지 않을 수 있음.

---

### 24.2. 정기적인 재인덱싱

일부 상황에서는 `REINDEX` 명령이나 일련의 개별 재구축 단계를 통해 인덱스를 주기적으로 재구축하는 것이 유용함.

#### 공간 효율성 문제

B-tree 인덱스:
- 완전히 비어 있는 B-tree 인덱스 페이지는 재사용을 위해 회수됨
- 그러나 페이지의 인덱스 키 대부분(전부는 아님)이 삭제된 경우 비효율적인 공간 사용이 여전히 발생할 수 있음. 페이지는 할당된 상태로 유지됨
- 각 범위의 대부분(전부는 아님)의 키가 결국 삭제되는 사용 패턴은 공간 활용도가 낮음
- 권장사항: 이러한 사용 패턴에는 주기적인 재인덱싱 권장

비-B-tree 인덱스:
- 비-B-tree 인덱스의 블로트 가능성은 잘 연구되지 않음
- 권장사항: 비-B-tree 인덱스 유형을 사용할 때는 인덱스의 물리적 크기를 주기적으로 모니터링 필요

#### 성능 고려사항

B-tree 인덱스의 경우:
- 새로 구축된 인덱스는 여러 번 업데이트된 인덱스보다 접근 속도가 약간 빠름
- 새로 구축된 인덱스에서는 논리적으로 인접한 페이지가 일반적으로 물리적으로도 인접하기 때문임
- 참고: 이 고려사항은 비-B-tree 인덱스에는 미적용
- 접근 속도를 향상시키기 위해 주기적으로 재인덱싱하는 것이 가치 있을 수 있음

#### 사용법 및 잠금

`REINDEX`는 모든 경우에 안전하고 쉽게 사용 가능:
- 기본적으로 명령은 `ACCESS EXCLUSIVE` 잠금을 요구함
- `CONCURRENTLY` 옵션으로 실행하는 것이 종종 바람직하며, 이 경우 `SHARE UPDATE EXCLUSIVE` 잠금만 필요함

---

### 24.3. 로그 파일 유지보수

데이터베이스 서버의 로그 출력을 `/dev/null`로 버리는 것보다는 어딘가에 저장하는 것이 좋음. 로그 출력은 문제 진단 시 매우 유용함.

#### 보안 고려사항

> 중요: 서버 로그에는 다음과 같은 민감한 정보가 포함될 수 있음:
> - DDL 문의 평문 암호
> - 애플리케이션의 SQL 소스 코드
> - ERROR 수준 로그의 데이터 행 일부

로그는 적절하게 권한이 부여된 사람만 볼 수 있도록 보호 필요.

#### 로그 순환 접근 방식

##### 1. 내장 로그 순환 기능 (권장)

`postgresql.conf`에서 구성 매개변수 `logging_collector`를 `true`로 설정함:

```
logging_collector = true
```

제어 매개변수는 Section 19.8.1에 설명되어 있음. 이 접근 방식은 기계 판독 가능한 CSV 형식 캡처도 지원함.

##### 2. 외부 로그 순환 프로그램

stderr 출력을 외부 프로그램으로 파이프함. Apache의 `rotatelogs`를 사용한 예:

```bash
pg_ctl start | rotatelogs /var/log/pgsql_log 86400
```

##### 3. logrotate와 결합된 접근 방식

PostgreSQL의 내장 로깅 수집기가 생성한 파일을 수집하도록 `logrotate`를 설정함. 로그를 순환할 때 로그 파일을 다시 열도록 `SIGHUP` 신호를 보냄:

```bash
pg_ctl logrotate
```

서버는 로깅 구성에 따라 새 로그 파일로 전환하거나 기존 파일을 다시 염.

정적 로그 파일 이름에 대한 참고: 최대 열린 파일 제한에 도달하면 서버가 로그 파일을 다시 열지 못할 수 있음. 이를 방지하려면 동적 로그 파일 이름을 구성하고 `prerotate` 스크립트를 사용하여 열린 로그 파일을 무시할 것.

##### 4. Syslog 접근 방식

`postgresql.conf`에서 `log_destination`을 `syslog`로 설정함:

```
log_destination = syslog
```

주의사항:
- Syslog는 큰 메시지에서 항상 신뢰할 수 없음
- 메시지를 자르거나 삭제할 수 있음
- Linux에서는 각 메시지를 디스크에 플러시함(성능 저하)
- syslog 구성에서 "`-`" 접두사로 동기화 비활성화 가능

#### 로그 파일 정리

위의 모든 방법은 구성 가능한 간격으로 새 로그 파일을 생성하지만 이전 파일을 자동으로 삭제하지는 않음. 다음 중 하나를 수행 필요:
- 주기적으로 이전 로그 파일을 삭제하는 배치 작업 설정
- 로그 파일을 순환적으로 덮어쓰도록 순환 프로그램 구성

#### 추가 도구

- [pgBadger](https://pgbadger.darold.net/) - 정교한 로그 파일 분석
- [check_postgres](https://bucardo.org/check_postgres/) - 중요한 로그 메시지 및 이상 탐지에 대한 Nagios 경고
## 제27장. 데이터베이스 활동 모니터링 (Monitoring Database Activity)

> PostgreSQL 18 공식 문서 번역
>
> 원문: https://www.postgresql.org/docs/current/monitoring.html

데이터베이스 관리자는 종종 "시스템이 지금 무엇을 하고 있는가?"라는 질문의 답을 찾아야 함. 이 장에서는 데이터베이스 활동을 모니터링하고 시스템 동작을 파악하는 방법을 설명함.

---

### 목차

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

### 27.1 표준 유닉스 도구

대부분의 유닉스 플랫폼에서 PostgreSQL은 `ps`가 표시하는 명령 제목을 수정해 개별 서버 프로세스를 쉽게 식별할 수 있게 함.

샘플 `ps` 출력:

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

#### 프로세스 유형

1. 주 서버 프로세스: 원래 명령 인수와 함께 나열된 첫 번째 프로세스
2. 백그라운드 워커 프로세스: 주 프로세스가 자동으로 시작
   - Background writer
   - Checkpointer
   - WAL writer
   - Autovacuum launcher (활성화된 경우)
3. 클라이언트 연결 프로세스: 개별 클라이언트 연결을 처리

#### 클라이언트 프로세스 형식

각 클라이언트 연결 프로세스는 다음을 표시함:

```
postgres: user database host activity
```

- user, database, host: 클라이언트 연결 수명 동안 동일하게 유지됨
- activity: 변경되며 다음 값을 가질 수 있음
  - `idle`: 클라이언트 명령 대기 중
  - `idle in transaction`: BEGIN 블록 내에서 클라이언트 대기 중
  - 명령 유형 이름 (예: `SELECT`)
  - `waiting`: 다른 세션이 보유한 잠금 대기 중인 경우 추가됨

#### 클러스터 이름 표시

`cluster_name`이 구성된 경우 `ps` 출력에 표시됨:

```
$ ps aux|grep server1
postgres   27093  0.0  0.0  30096  2752 ?        Ss   11:34   0:00 postgres: server1: background writer
```

#### 구성 옵션

- update_process_title: 비활성화하면 활동 표시기가 갱신되지 않으며, 새 프로세스가 시작될 때만 프로세스 제목이 설정됨 → 일부 플랫폼에서 명령당 발생하는 측정 가능한 오버헤드 감소 가능

#### Solaris 특수 처리

Solaris 시스템의 경우:
- `/bin/ps` 대신 `/usr/ucb/ps` 사용
- `w` 플래그를 하나가 아닌 두 개 사용
- 원래 postgres 명령 호출은 서버 프로세스보다 더 짧은 `ps` 상태 표시를 가져야 함

---

### 27.2 누적 통계 시스템

PostgreSQL의 누적 통계 시스템(cumulative statistics system)은 서버 활동 정보를 수집·보고함. 다음을 추적함:

- 테이블 및 인덱스 액세스 (디스크 블록 및 행 수준)
- 테이블당 행 수
- Vacuum 및 Analyze 작업
- 사용자 정의 함수 호출 및 실행 시간 (활성화된 경우)
- 실시간 시스템 활동 (누적 통계와 독립적)

#### 27.2.1 통계 수집 구성

통계 수집은 쿼리 실행에 오버헤드를 추가하며, 아래 매개변수로 제어함 (`postgresql.conf`에서 설정):

- `track_activities`: 서버 프로세스의 현재 명령 실행 모니터링
- `track_cost_delay_timing`: 비용 기반 vacuum 지연 모니터링
- `track_counts`: 테이블·인덱스 액세스에 대한 누적 통계 수집
- `track_functions`: 사용자 정의 함수 사용 추적
- `track_io_timing`: 블록 읽기·쓰기·확장·fsync 시간 모니터링
- `track_wal_io_timing`: WAL 읽기·쓰기·fsync 시간 모니터링

주요 사항:

- `SET` 명령으로 세션별 설정 가능 (슈퍼유저만 해당)
- 각 PostgreSQL 프로세스가 공유 메모리에 통계를 누적
- 프로세스는 적절한 간격으로 통계를 공유 메모리에 플러시
- 정상 종료 시: 통계가 `pg_stat` 하위 디렉터리에 저장됨 (재시작 후에도 유지)
- 비정상 종료 시: 모든 카운터가 재설정됨

#### 27.2.2 통계 보기

##### 중요 고려사항

1. 업데이트 지연: 통계는 즉시 반영되지 않음
   - 프로세스는 유휴 상태가 되기 직전에 공유 메모리로 플러시
   - `PGSTAT_MIN_INTERVAL`(기본값 1초)보다 자주 플러시하지 않음
   - `track_activities`의 현재 쿼리 정보는 항상 최신 상태 유지

2. 스냅샷/캐시 동작:
   - 기본값(`cache`)에서는 처음 조회한 통계 값이 트랜잭션 끝까지 캐시됨
   - `stats_fetch_consistency` 매개변수로 동작 변경 가능
     - `snapshot`: 트랜잭션 시작 시 전체 스냅샷 생성 → 왜곡 최소화 (메모리 사용량은 증가)
     - `none`: 통계에 한 번만 접근하는 경우 캐싱 비활성화
   - `pg_stat_clear_snapshot()`으로 캐시된 값 초기화 가능

3. 트랜잭션별 뷰:
   - `pg_stat_xact_all_tables`, `pg_stat_xact_sys_tables`, `pg_stat_xact_user_tables`
   - `pg_stat_xact_user_functions`
   - 트랜잭션 전체에서 지속적으로 업데이트됨 (아직 공유 메모리로 플러시되지 않음)

4. 보안 제한:
   - 일반 사용자는 자신의 세션 정보만 조회 가능
   - 슈퍼유저와 `pg_read_all_stats` 역할은 모든 세션 조회 가능
   - 세션 존재 여부 및 일반 속성은 모든 사용자에게 표시됨

##### 동적 통계 뷰

- `pg_stat_activity`: 서버 프로세스당 하나의 행과 현재 활동
- `pg_stat_replication`: 대기 서버에 대한 WAL 발신자 통계
- `pg_stat_wal_receiver`: WAL 수신자 통계
- `pg_stat_recovery_prefetch`: 복구 중 미리 가져온 블록
- `pg_stat_subscription`: 구독 워커 정보
- `pg_stat_ssl`: 연결당 SSL 사용
- `pg_stat_gssapi`: GSSAPI 인증/암호화 정보
- `pg_stat_progress_*`: ANALYZE·CREATE INDEX·VACUUM·CLUSTER·BASEBACKUP·COPY에 대한 진행 상황 보고

##### 수집된 통계 뷰

- `pg_stat_archiver`: WAL 아카이버 프로세스 통계
- `pg_stat_bgwriter`: 백그라운드 라이터 활동
- `pg_stat_checkpointer`: 체크포인트 프로세스 활동
- `pg_stat_database`: 데이터베이스 전체 통계
- `pg_stat_database_conflicts`: 대기 복구로 인한 쿼리 취소
- `pg_stat_io`: 클러스터 전체 I/O 통계
- `pg_stat_replication_slots`: 복제 슬롯 사용 통계
- `pg_stat_slru`: SLRU 캐시 작업
- `pg_stat_subscription_stats`: 구독 오류 및 충돌
- `pg_stat_wal`: WAL 활동 통계
- `pg_stat_all_tables` / `pg_stat_sys_tables` / `pg_stat_user_tables`: 테이블 액세스 통계
- `pg_stat_xact_*`: 트랜잭션별 테이블 통계
- `pg_stat_all_indexes` / `pg_stat_sys_indexes` / `pg_stat_user_indexes`: 인덱스 액세스 통계
- `pg_stat_user_functions` / `pg_stat_xact_user_functions`: 함수 실행 통계
- `pg_statio_all_tables` / `pg_statio_sys_tables` / `pg_statio_user_tables`: 테이블 I/O 통계
- `pg_statio_all_indexes` / `pg_statio_sys_indexes` / `pg_statio_user_indexes`: 인덱스 I/O 통계
- `pg_statio_all_sequences`: 시퀀스 I/O 통계

#### 27.2.3 pg_stat_activity

##### pg_stat_activity 컬럼

- `datid` (oid): 연결된 데이터베이스의 OID
- `datname` (name): 연결된 데이터베이스 이름
- `pid` (integer): 백엔드의 프로세스 ID
- `leader_pid` (integer): 병렬 그룹 리더의 PID (리더인 경우 NULL)
- `usesysid` (oid): 로그인한 사용자의 OID
- `usename` (name): 로그인한 사용자 이름
- `application_name` (text): 연결된 애플리케이션 이름
- `client_addr` (inet): 클라이언트 IP 주소 (유닉스 소켓의 경우 NULL)
- `client_hostname` (text): 역방향 DNS 호스트 이름 (`log_hostname` 활성화 필요)
- `client_port` (integer): 클라이언트 TCP 포트 (유닉스 소켓의 경우 -1)
- `backend_start` (timestamp): 프로세스 시작 시간
- `xact_start` (timestamp): 현재 트랜잭션 시작 (활성 상태가 아니면 NULL)
- `query_start` (timestamp): 활성 쿼리 시작 시간
- `state_change` (timestamp): 마지막 상태 변경 시간
- `wait_event_type` (text): 대기 이벤트 유형 (아래 대기 이벤트 유형 참고)
- `wait_event` (text): 특정 대기 이벤트 이름
- `state` (text): 백엔드 상태
- `backend_xid` (xid): 최상위 트랜잭션 ID
- `backend_xmin` (xid): 현재 백엔드의 xmin 수평선
- `query_id` (bigint): 쿼리 식별자 (`compute_query_id` 활성화 시)
- `query` (text): 가장 최근 쿼리 텍스트 (기본적으로 1024바이트에서 잘림)
- `backend_type` (text): 백엔드 유형 (autovacuum launcher/worker, logical replication, parallel worker, background writer, client backend, checkpointer, archiver 등)

##### 백엔드 상태

- `starting`: 초기 시작/인증 단계
- `active`: 쿼리 실행 중
- `idle`: 클라이언트 명령 대기 중
- `idle in transaction`: 트랜잭션 내에서 쿼리를 실행하지 않는 상태
- `idle in transaction (aborted)`: 오류 상태의 트랜잭션
- `fastpath function call`: 빠른 경로 함수 실행 중
- `disabled`: `track_activities` 비활성화됨

##### 대기 이벤트 유형

- `Activity`: 주 처리 루프에서 유휴 대기
- `BufferPin`: 배타적 버퍼 액세스 대기
- `Client`: 클라이언트 소켓 활동 대기
- `Extension`: 확장 정의 대기 조건
- `InjectionPoint`: 테스트 주입 지점 조건
- `IO`: I/O 작업 완료 대기
- `IPC`: 프로세스 간 통신
- `Lock`: 중량 잠금 대기
- `LWLock`: 경량 잠금 대기
- `Timeout`: 타임아웃 만료 대기

##### Activity 대기 이벤트 예시

- `ArchiverMain`: 아카이버 프로세스 주 루프
- `AutovacuumMain`: Autovacuum 런처 주 루프
- `BgwriterMain`: 백그라운드 라이터 주 루프
- `CheckpointerMain`: 체크포인터 주 루프
- `WalReceiverMain`: WAL 수신자 주 루프
- `WalSenderMain`: WAL 발신자 주 루프
- `LogicalApplyMain`: 논리 복제 적용 주 루프

##### I/O 대기 이벤트 예시

- `DataFileRead`: 관계 데이터 파일 읽기
- `DataFileWrite`: 관계 데이터 파일 쓰기
- `DataFileExtend`: 관계 데이터 파일 확장
- `DataFileSync`: 관계 데이터 파일 fsync
- `ControlFileRead`: pg_control 파일 읽기
- `ControlFileWrite`: pg_control 파일 쓰기
- `WalRead`: WAL 파일 읽기
- `WalWrite`: WAL 파일 쓰기
- `WalSync`: WAL 파일 fsync

##### Lock 대기 이벤트

- `advisory`: 권고 사용자 잠금
- `extend`: 관계 확장
- `frozenid`: pg_database.datfrozenxid 업데이트
- `object`: 비관계 데이터베이스 객체
- `page`: 관계 페이지 잠금
- `relation`: 관계 잠금
- `transactionid`: 트랜잭션 완료 대기
- `tuple`: 튜플 잠금
- `virtualxid`: 가상 트랜잭션 ID 잠금

##### 대기 이벤트 세부 정보 쿼리 예시 (`pg_wait_events`와 조인)

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

#### 27.2.4 pg_stat_replication

연결된 각 대기 서버(standby server)에 대한 통계를 표시함.

##### pg_stat_replication 컬럼

- `pid` (integer): WAL 발신자 프로세스 ID
- `usesysid` (oid): 사용자 OID
- `usename` (name): 사용자 이름
- `application_name` (text): 애플리케이션 이름
- `client_addr` (inet): 클라이언트 IP 주소
- `client_hostname` (text): 클라이언트 호스트 이름
- `client_port` (integer): 클라이언트 포트
- `backend_start` (timestamp): 연결 시작 시간
- `backend_xmin` (xid): 대기 서버의 xmin 수평선
- `state` (text): WAL 발신자 상태 (startup, catchup, streaming, backup, stopping)
- `sent_lsn` (pg_lsn): 마지막으로 전송된 WAL 위치
- `write_lsn` (pg_lsn): 마지막으로 작성된 WAL 위치 (아직 플러시되지 않음)
- `flush_lsn` (pg_lsn): 마지막으로 플러시된 WAL 위치
- `replay_lsn` (pg_lsn): 마지막으로 재생된 WAL 위치
- `write_lag` (interval): `synchronous_commit` 수준 `remote_write`에 대한 지연
- `flush_lag` (interval): `synchronous_commit` 수준 `on`에 대한 지연
- `replay_lag` (interval): `synchronous_commit` 수준 `remote_apply`에 대한 지연
- `sync_priority` (integer): 동기식 대기 선택의 우선순위
- `sync_state` (text): 동기화 상태 (async, potential, sync, quorum)
- `reply_time` (timestamp): 마지막 응답 메시지 시간

#### 27.2.5 pg_stat_replication_slots

논리적 복제 슬롯 사용을 추적함.

##### pg_stat_replication_slots 컬럼

- `slot_name` (text): 클러스터 전체에서 고유한 슬롯 식별자
- `spill_txns` (bigint): 디스크로 스필된 트랜잭션
- `spill_count` (bigint): 스필 이벤트 수
- `spill_bytes` (bigint): 스필된 디코딩된 트랜잭션의 바이트
- `stream_txns` (bigint): 스트리밍된 진행 중인 트랜잭션
- `stream_count` (bigint): 스트림 이벤트 수
- `stream_bytes` (bigint): 스트리밍된 트랜잭션의 바이트
- `total_txns` (bigint): 전송된 총 디코딩된 트랜잭션
- `total_bytes` (bigint): 디코딩된 총 트랜잭션 데이터
- `stats_reset` (timestamp): 마지막 재설정 시간

#### 27.2.6 pg_stat_wal_receiver

대기 서버의 WAL 수신자 통계.

##### pg_stat_wal_receiver 컬럼

- `pid` (integer): WAL 수신자 프로세스 ID
- `status` (text): 활동 상태
- `receive_start_lsn` (pg_lsn): 수신자가 시작할 때 첫 번째 WAL 위치
- `receive_start_tli` (integer): 시작 타임라인 번호
- `written_lsn` (pg_lsn): 마지막으로 작성된 (플러시되지 않은) WAL 위치
- `flushed_lsn` (pg_lsn): 마지막으로 플러시된 WAL 위치
- `received_tli` (integer): 마지막으로 플러시된 WAL 위치의 타임라인
- `last_msg_send_time` (timestamp): 발신자로부터 마지막 메시지 전송 시간
- `last_msg_receipt_time` (timestamp): 마지막 메시지 수신 시간
- `latest_end_lsn` (pg_lsn): 발신자에게 보고된 마지막 WAL 위치
- `latest_end_time` (timestamp): 마지막 보고된 WAL 위치 시간
- `slot_name` (text): 복제 슬롯 이름
- `sender_host` (text): 주 서버 호스트/경로
- `sender_port` (integer): 주 서버 포트
- `conninfo` (text): 연결 문자열 (보안에 민감한 필드는 난독화됨)

#### 27.2.7 pg_stat_recovery_prefetch

복구 미리 가져오기 통계 (단일 행).

##### 표 27.17: pg_stat_recovery_prefetch 컬럼

- `stats_reset` (timestamp): 마지막 재설정 시간
- `prefetch` (bigint): 미리 가져온 블록 (버퍼 풀에 없음)
- `hit` (bigint): 이미 버퍼 풀에 있는 블록
- `skip_init` (bigint): 미리 가져오지 않은 0으로 초기화된 블록
- `skip_new` (bigint): 미리 가져오지 않은 존재하지 않는 블록
- `skip_fpw` (bigint): 미리 가져오지 않은 전체 페이지 이미지가 있는 블록
- `skip_rep` (bigint): 미리 가져오지 않은 최근에 미리 가져온 블록
- `wal_distance` (int): 미리 가져오기가 앞서 보고 있는 바이트
- `block_distance` (int): 미리 가져오기가 앞서 보고 있는 블록
- `io_depth` (int): 완료되지 않은 것으로 알려진 비행 중인 미리 가져오기

#### 27.2.8 pg_stat_subscription

##### 표 27.18: pg_stat_subscription 컬럼

- `subid` (oid): 구독 OID
- `subname` (name): 구독 이름
- `worker_type` (text): 워커 유형 (apply, parallel apply, table synchronization)
- `pid` (integer): 워커 프로세스 ID
- `leader_pid` (integer): 리더 적용 워커 PID (리더 또는 동기화 워커인 경우 NULL)
- `relid` (oid): 동기화 중인 관계 OID (적용 워커의 경우 NULL)
- `received_lsn` (pg_lsn): 마지막으로 수신된 WAL 위치 (병렬 적용의 경우 NULL)
- `last_msg_send_time` (timestamp): 마지막 발신자 메시지 전송 시간
- `last_msg_receipt_time` (timestamp): 마지막 발신자 메시지 수신 시간
- `latest_end_lsn` (pg_lsn): 발신자에게 보고된 마지막 WAL 위치
- `latest_end_time` (timestamp): 마지막 보고된 위치 시간

#### 27.2.9 pg_stat_subscription_stats

구독 오류 및 충돌 통계.

##### pg_stat_subscription_stats 컬럼

- `subid` (oid): 구독 OID
- `subname` (name): 구독 이름
- `apply_error_count` (bigint): 적용 오류 (충돌 카운터에도 계산됨)
- `sync_error_count` (bigint): 초기 동기화 오류
- `confl_insert_exists` (bigint): 삽입 시 고유 제약 조건 위반
- `confl_update_origin_differs` (bigint): 이전에 수정된 행에 대한 업데이트
- `confl_update_exists` (bigint): 업데이트 시 고유 제약 조건 위반
- `confl_update_missing` (bigint): 업데이트 대상 행을 찾을 수 없음
- `confl_delete_origin_differs` (bigint): 이전에 수정된 행의 삭제
- `confl_delete_missing` (bigint): 삭제 대상 행을 찾을 수 없음
- `confl_multiple_unique_conflicts` (bigint): 여러 고유 제약 조건 위반
- `stats_reset` (timestamp): 마지막 재설정 시간

#### 27.2.10 pg_stat_ssl

연결당 SSL 사용 통계.

##### pg_stat_ssl 컬럼

- `pid` (integer): 백엔드/WAL 발신자 프로세스 ID
- `ssl` (boolean): SSL 사용 중 여부
- `version` (text): SSL 버전
- `cipher` (text): SSL 암호 이름
- `bits` (integer): 암호화 알고리즘 비트
- `client_dn` (text): 클라이언트 인증서 DN (64자로 잘림)
- `client_serial` (numeric): 클라이언트 인증서 일련 번호
- `issuer_dn` (text): 발급자 DN (64자로 잘림)

#### 27.2.11 pg_stat_gssapi

GSSAPI 인증/암호화 통계.

##### pg_stat_gssapi 컬럼

- `pid` (integer): 백엔드 프로세스 ID
- `gss_authenticated` (boolean): GSSAPI 인증 사용 여부
- `principal` (text): 사용된 주체 (64자로 잘림)
- `encrypted` (boolean): GSSAPI 암호화 사용 중 여부
- `credentials_delegated` (boolean): 자격 증명 위임 여부

#### 27.2.12 pg_stat_archiver

WAL 아카이버 프로세스 통계 (단일 행).

##### pg_stat_archiver 컬럼

- `archived_count` (bigint): 성공적으로 아카이빙된 WAL 파일
- `last_archived_wal` (text): 가장 최근에 성공적으로 아카이빙된 파일
- `last_archived_time` (timestamp): 가장 최근 성공적인 아카이브 시간
- `failed_count` (bigint): 실패한 아카이빙 시도
- `last_failed_wal` (text): 가장 최근 실패한 아카이빙 파일
- `last_failed_time` (timestamp): 가장 최근 실패 시간
- `stats_reset` (timestamp): 마지막 재설정 시간

참고: WAL 파일이 순서대로 아카이빙된다는 보장이 없으므로, `last_archived_wal`보다 오래된 파일이 모두 아카이빙되었다고 가정하면 안 됨.

#### 27.2.13 pg_stat_io

백엔드 유형·객체·컨텍스트별 클러스터 전체 I/O 통계.

##### pg_stat_io 컬럼

- `backend_type` (text): 백엔드 유형 (background worker, autovacuum 등)
- `object` (text): 대상 객체 (relation, temp relation, wal)
- `context` (text): I/O 컨텍스트 (normal, init, vacuum, bulkread, bulkwrite)
- `reads` (bigint): 읽기 작업 수
- `read_bytes` (numeric): 총 읽은 바이트
- `read_time` (double): 읽기 대기 시간 (ms, `track_io_timing` 활성화 시)
- `writes` (bigint): 쓰기 작업 수
- `write_bytes` (numeric): 총 쓴 바이트
- `write_time` (double): 쓰기 대기 시간 (ms)
- `writebacks` (bigint): 쓰기 되돌림 단위 (8kB 블록)
- `writeback_time` (double): 쓰기 되돌림 대기 시간 (ms)
- `extends` (bigint): 관계 확장 작업
- `extend_bytes` (numeric): 확장 작업의 바이트
- `extend_time` (double): 확장 대기 시간 (ms)
- `hits` (bigint): 버퍼에서 찾은 원하는 블록
- `evictions` (bigint): 버퍼에서 제거된 블록
- `reuses` (bigint): 링 버퍼에서 재사용된 버퍼
- `fsyncs` (bigint): fsync() 호출
- `fsync_time` (double): fsync 대기 시간 (ms)
- `stats_reset` (timestamp): 마지막 재설정 시간

##### I/O 컨텍스트

- normal: 기본 I/O 컨텍스트 (공유 버퍼)
- init: WAL 세그먼트 생성
- vacuum: VACUUM/ANALYZE 중 공유 버퍼 외부 I/O
- bulkread: 공유 버퍼 외부의 대규모 순차 읽기
- bulkwrite: 공유 버퍼 외부의 대규모 쓰기 (예: COPY)

##### 튜닝 참고사항

- `evictions` 수치가 높음 → 공유 버퍼 크기 증가 검토
- 클라이언트의 `fsyncs` 수치가 높음 → 공유 버퍼 또는 체크포인터 설정 확인
- 클라이언트의 `writes` 수치가 높음 → 버퍼 또는 체크포인터 설정 확인

#### 27.2.14 pg_stat_bgwriter

백그라운드 라이터 통계 (단일 행).

##### pg_stat_bgwriter 컬럼

- `buffers_clean` (bigint): 백그라운드 라이터가 쓴 버퍼
- `maxwritten_clean` (bigint): 너무 많은 쓰기로 인해 중지된 청소 스캔
- `buffers_alloc` (bigint): 할당된 버퍼
- `stats_reset` (timestamp): 마지막 재설정 시간

#### 27.2.15 pg_stat_checkpointer

체크포인트 프로세스 통계 (단일 행).

##### pg_stat_checkpointer 컬럼

- `num_timed` (bigint): 예약된 체크포인트 (완료 + 건너뜀)
- `num_requested` (bigint): 요청된 체크포인트 (완료 + 건너뜀)
- `num_done` (bigint): 완료된 체크포인트
- `restartpoints_timed` (bigint): 예약된 재시작점 (완료 + 건너뜀)
- `restartpoints_req` (bigint): 요청된 재시작점 (완료 + 건너뜀)
- `restartpoints_done` (bigint): 완료된 재시작점
- `write_time` (double): 파일 쓰기 시간 (ms)
- `sync_time` (double): 파일 동기화 시간 (ms)
- `buffers_written` (bigint): 쓴 공유 버퍼
- `slru_written` (bigint): 쓴 SLRU 버퍼
- `stats_reset` (timestamp): 마지막 재설정 시간

#### 27.2.16 pg_stat_wal

WAL 활동 통계 (단일 행).

##### pg_stat_wal 컬럼

- `wal_records` (bigint): 생성된 총 WAL 레코드
- `wal_fpi` (bigint): 생성된 총 전체 페이지 이미지
- `wal_bytes` (numeric): 생성된 총 WAL 바이트
- `wal_buffers_full` (bigint): 버퍼가 가득 차서 WAL을 쓴 횟수
- `stats_reset` (timestamp): 마지막 재설정 시간

#### 27.2.17 pg_stat_database

데이터베이스 전체 통계 (데이터베이스당 하나의 행 + 공유 객체에 대해 하나).

##### pg_stat_database 컬럼

- `datid` (oid): 데이터베이스 OID (공유 객체의 경우 0)
- `datname` (name): 데이터베이스 이름 (공유 객체의 경우 NULL)
- `numbackends` (integer): 현재 연결된 백엔드 (현재 상태 전용 컬럼)
- `xact_commit` (bigint): 커밋된 트랜잭션
- `xact_rollback` (bigint): 롤백된 트랜잭션
- `blks_read` (bigint): 읽은 디스크 블록
- `blks_hit` (bigint): 버퍼 캐시 히트
- `tup_returned` (bigint): 순차/인덱스 스캔에서 라이브 행
- `tup_fetched` (bigint): 인덱스 스캔에서 라이브 행
- `tup_inserted` (bigint): 삽입된 행
- `tup_updated` (bigint): 업데이트된 행 (HOT 및 newpage 업데이트 포함)
- `tup_deleted` (bigint): 삭제된 행
- `conflicts` (bigint): 복구 충돌 (대기 전용)
- `temp_files` (bigint): 생성된 임시 파일
- `temp_bytes` (bigint): 쓴 임시 파일 데이터
- `deadlocks` (bigint): 감지된 교착 상태
- `checksum_failures` (bigint): 데이터 페이지 체크섬 실패 (비활성화된 경우 NULL)
- `checksum_last_failure` (timestamp): 마지막 체크섬 실패 시간
- `blk_read_time` (double): 데이터 파일 읽기 시간 (ms, `track_io_timing` 활성화 시)
- `blk_write_time` (double): 데이터 파일 쓰기 시간 (ms)
- `session_time` (double): 총 세션 시간 (ms)
- `active_time` (double): 활성 SQL 실행 시간 (ms)
- `idle_in_transaction_time` (double): 트랜잭션 내 유휴 시간 (ms)
- `sessions` (bigint): 설정된 총 세션
- `sessions_abandoned` (bigint): 클라이언트 연결 끊김으로 인해 손실된 세션
- `sessions_fatal` (bigint): 치명적 오류로 종료된 세션
- `sessions_killed` (bigint): 운영자에 의해 종료된 세션
- `parallel_workers_to_launch` (bigint): 계획된 병렬 워커
- `parallel_workers_launched` (bigint): 시작된 병렬 워커
- `stats_reset` (timestamp): 마지막 재설정 시간

#### 27.2.18 pg_stat_database_conflicts

대기 복구 충돌 통계 (데이터베이스당 하나의 행, 대기 전용).

##### pg_stat_database_conflicts 컬럼

- `datid` (oid): 데이터베이스 OID
- `datname` (name): 데이터베이스 이름
- `confl_tablespace` (bigint): 삭제된 테이블스페이스에서 취소된 쿼리
- `confl_lock` (bigint): 잠금 타임아웃에서 취소된 쿼리
- `confl_snapshot` (bigint): 오래된 스냅샷에서 취소된 쿼리
- `confl_bufferpin` (bigint): 고정된 버퍼에서 취소된 쿼리
- `confl_deadlock` (bigint): 교착 상태에서 취소된 쿼리
- `confl_active_logicalslot` (bigint): 오래된 스냅샷/낮은 wal_level에서 취소된 논리 슬롯 사용

#### 27.2.19 pg_stat_all_tables

테이블별 액세스 통계 (TOAST 포함 테이블당 하나의 행).

##### pg_stat_all_tables 컬럼

- `relid` (oid): 테이블 OID
- `schemaname` (name): 스키마 이름
- `relname` (name): 테이블 이름
- `seq_scan` (bigint): 시작된 순차 스캔
- `last_seq_scan` (timestamp): 마지막 순차 스캔 시간
- `seq_tup_read` (bigint): 순차 스캔으로 가져온 라이브 행
- `idx_scan` (bigint): 시작된 인덱스 스캔
- `last_idx_scan` (timestamp): 마지막 인덱스 스캔 시간
- `idx_tup_fetch` (bigint): 인덱스 스캔으로 가져온 라이브 행
- `n_tup_ins` (bigint): 삽입된 행
- `n_tup_upd` (bigint): 업데이트된 행 (HOT 및 newpage 업데이트 포함)
- `n_tup_del` (bigint): 삭제된 행
- `n_tup_hot_upd` (bigint): HOT 업데이트된 행 (인덱스 업데이트 불필요)
- `n_tup_newpage_upd` (bigint): 새 힙 페이지로 이동된 업데이트 행
- `n_live_tup` (bigint): 추정 라이브 행
- `n_dead_tup` (bigint): 추정 데드 행
- `n_mod_since_analyze` (bigint): ANALYZE 이후 수정된 추정 행
- `n_ins_since_vacuum` (bigint): VACUUM 이후 삽입된 추정 행
- `last_vacuum` (timestamp): 마지막 수동 VACUUM (FULL 아님)
- `last_autovacuum` (timestamp): autovacuum 데몬의 마지막 VACUUM
- `last_analyze` (timestamp): 마지막 수동 ANALYZE
- `last_autoanalyze` (timestamp): autovacuum 데몬의 마지막 ANALYZE
- `vacuum_count` (bigint): 수동 VACUUM 횟수
- `autovacuum_count` (bigint): Autovacuum 데몬 횟수
- `analyze_count` (bigint): 수동 ANALYZE 횟수
- `autoanalyze_count` (bigint): Autovacuum ANALYZE 횟수
- `total_vacuum_time` (double): 총 수동 vacuum 시간 (ms, 대기 시간 포함)
- `total_autovacuum_time` (double): 총 autovacuum 시간 (ms)
- `total_analyze_time` (double): 총 수동 analyze 시간 (ms)
- `total_autoanalyze_time` (double): 총 autovacuum analyze 시간 (ms)

관련 뷰:
- `pg_stat_user_tables`: 사용자 테이블만
- `pg_stat_sys_tables`: 시스템 테이블만
- `pg_stat_xact_all_tables`: 현재 트랜잭션 활동 (데드 행, vacuum 또는 analyze 컬럼 없음)
- `pg_stat_xact_user_tables`: 현재 트랜잭션의 사용자 테이블
- `pg_stat_xact_sys_tables`: 현재 트랜잭션의 시스템 테이블

#### 27.2.20 pg_stat_all_indexes

인덱스별 액세스 통계 (인덱스당 하나의 행).

##### pg_stat_all_indexes 컬럼

- `relid` (oid): 테이블 OID
- `indexrelid` (oid): 인덱스 OID
- `schemaname` (name): 스키마 이름
- `relname` (name): 테이블 이름
- `indexrelname` (name): 인덱스 이름
- `idx_scan` (bigint): 시작된 인덱스 스캔
- `last_idx_scan` (timestamp): 마지막 인덱스 스캔 시간
- `idx_tup_read` (bigint): 스캔에서 반환된 인덱스 항목
- `idx_tup_fetch` (bigint): 단순 인덱스 스캔으로 가져온 라이브 테이블 행

##### 중요 참고사항

- 비트맵 스캔: 인덱스에 대해 `idx_tup_read`를 증가시키지만 `idx_tup_fetch`는 증가시키지 않음 (테이블에서 계산됨)
- 옵티마이저: 상수 값 검증을 위해 인덱스에 액세스할 수 있음
- `idx_tup_read` >= `idx_tup_fetch` (데드 행, 커밋되지 않은 행, 인덱스 전용 스캔으로 인해)
- 인덱스 스캔은 실행자 노드 실행을 초과할 수 있음 (다중 값 검색, 스킵 스캔)

관련 뷰:
- `pg_stat_user_indexes`: 사용자 테이블 인덱스만
- `pg_stat_sys_indexes`: 시스템 테이블 인덱스만

#### 27.2.21 pg_statio_all_tables

테이블 I/O 통계 (TOAST 포함 테이블당 하나의 행).

##### pg_statio_all_tables 컬럼

- `relid` (oid): 테이블 OID
- `schemaname` (name): 스키마 이름
- `relname` (name): 테이블 이름
- `heap_blks_read` (bigint): 읽은 디스크 블록
- `heap_blks_hit` (bigint): 버퍼 히트
- `idx_blks_read` (bigint): 읽은 인덱스 디스크 블록
- `idx_blks_hit` (bigint): 인덱스 버퍼 히트
- `toast_blks_read` (bigint): 읽은 TOAST 테이블 디스크 블록
- `toast_blks_hit` (bigint): TOAST 테이블 버퍼 히트
- `tidx_blks_read` (bigint): 읽은 TOAST 인덱스 디스크 블록
- `tidx_blks_hit` (bigint): TOAST 인덱스 버퍼 히트

관련 뷰:
- `pg_statio_user_tables`: 사용자 테이블만
- `pg_statio_sys_tables`: 시스템 테이블만

#### 27.2.22 pg_statio_all_indexes

인덱스 I/O 통계 (인덱스당 하나의 행).

##### pg_statio_all_indexes 컬럼

- `relid` (oid): 테이블 OID
- `indexrelid` (oid): 인덱스 OID
- `schemaname` (name): 스키마 이름
- `relname` (name): 테이블 이름
- `indexrelname` (name): 인덱스 이름
- `idx_blks_read` (bigint): 읽은 디스크 블록
- `idx_blks_hit` (bigint): 버퍼 히트

관련 뷰:
- `pg_statio_user_indexes`: 사용자 테이블 인덱스만
- `pg_statio_sys_indexes`: 시스템 테이블 인덱스만

#### 27.2.23 pg_statio_all_sequences

시퀀스 I/O 통계 (시퀀스당 하나의 행).

##### pg_statio_all_sequences 컬럼

- `relid` (oid): 시퀀스 OID
- `schemaname` (name): 스키마 이름
- `relname` (name): 시퀀스 이름
- `blks_read` (bigint): 읽은 디스크 블록
- `blks_hit` (bigint): 버퍼 히트

관련 뷰:
- `pg_statio_user_sequences`: 사용자 시퀀스만
- `pg_statio_sys_sequences`: 시스템 시퀀스만 (현재 항상 비어 있음)

#### 27.2.24 pg_stat_user_functions

사용자 정의 함수 실행 통계 (`track_functions` 매개변수로 제어됨).

##### pg_stat_user_functions 컬럼

- `funcid` (oid): 함수 OID
- `schemaname` (name): 스키마 이름
- `funcname` (name): 함수 이름
- `calls` (bigint): 함수 호출 횟수
- `total_time` (double): 함수 및 호출자의 총 시간 (ms)
- `self_time` (double): 함수 자체의 시간 (ms)

관련 뷰:
- `pg_stat_xact_user_functions`: 현재 트랜잭션 함수 호출만

#### 27.2.25 pg_stat_slru

SLRU (Simple Least-Recently-Used) 캐시 통계.

##### pg_stat_slru 컬럼

- `name` (text): SLRU 캐시 이름
- `blks_zeroed` (bigint): 초기화 중 0으로 설정된 블록
- `blks_hit` (bigint): 캐시 히트 (OS 캐시 포함하지 않음)
- `blks_read` (bigint): 읽은 디스크 블록
- `blks_written` (bigint): 쓴 디스크 블록
- `blks_exists` (bigint): 존재 확인된 블록
- `flushes` (bigint): 더티 데이터 플러시
- `truncates` (bigint): 캐시 잘라내기
- `stats_reset` (timestamp): 마지막 재설정 시간

##### 핵심 SLRU 캐시

- `commit_timestamp`: 커밋 타임스탬프 추적
- `multixact_member`: Multixact 멤버 정보
- `multixact_offset`: Multixact 오프셋 정보
- `notify`: NOTIFY 메시지 저장
- `serializable`: 직렬화 가능 트랜잭션 충돌
- `subtransaction`: 하위 트랜잭션 정보
- `transaction`: 트랜잭션 상태

#### 27.2.26 통계 함수

##### 표 27.36: 추가 통계 함수

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

##### 표 27.37: 백엔드별 통계 함수

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

##### 예시: 모든 활성 백엔드의 PID 및 쿼리 가져오기

```sql
SELECT pg_stat_get_backend_pid(backendid) AS pid,
       pg_stat_get_backend_activity(backendid) AS query
FROM pg_stat_get_backend_idset() AS backendid;
```

##### 중요 경고

`pg_stat_reset()` 사용 시 autovacuum 타이밍 카운터도 함께 재설정되어 필요한 유지 관리가 지연될 수 있음. 통계 재설정 후에는 데이터베이스 전체에 `ANALYZE`를 실행하는 것 권장.

---

### 27.3 잠금 보기

`pg_locks` 시스템 뷰는 잠금 관리자의 미해결 잠금 정보를 조회하는 데 유용한 도구임.

#### 주요 기능

`pg_locks` 뷰를 통해 데이터베이스 관리자는 다음을 수행 가능:

1. 잠금 정보 조회
   - 모든 미해결 잠금
   - 특정 데이터베이스의 관계에 설정된 모든 잠금
   - 특정 관계에 설정된 모든 잠금
   - 특정 PostgreSQL 세션이 보유한 모든 잠금

2. 경합 원인 식별
   - 가장 많은 미승인 잠금을 보유한 관계 파악 → 클라이언트 간 경합의 잠재적 원인 식별

3. 성능 영향 분석
   - 잠금 경합이 전체 데이터베이스 성능에 미치는 영향 파악
   - 데이터베이스 트래픽 변화에 따른 경합 추이 평가

#### 관련 문서

- `pg_locks` 뷰 세부 정보: 53.13 참고
- 잠금 및 동시성 관리: 13장 (동시성 제어) 참고

---

### 27.4 진행 상황 보고

PostgreSQL은 여러 장기 실행 명령에 대해 진행 상황 보고 기능을 제공함. 진행 상황 보고를 지원하는 명령은 다음과 같음:

- `ANALYZE`
- `CLUSTER`
- `CREATE INDEX`
- `VACUUM`
- `COPY`
- `BASE_BACKUP` (복제 명령)

진행 상황 정보는 명령 실행의 실시간 지표를 표시하는 전용 시스템 뷰를 통해 조회 가능.

#### 27.4.1 ANALYZE 진행 상황 보고

뷰: `pg_stat_progress_analyze`

`ANALYZE`가 실행 중일 때 이 뷰는 해당 명령을 실행하는 백엔드당 하나의 행을 포함함.

##### 주요 컬럼

- `pid` - 프로세스 ID
- `datid`, `datname` - 데이터베이스 OID 및 이름
- `relid` - 분석 중인 테이블의 OID
- `phase` - 현재 처리 단계
- `sample_blks_total`, `sample_blks_scanned` - 힙 블록 샘플링 진행 상황
- `ext_stats_total`, `ext_stats_computed` - 확장 통계 진행 상황
- `child_tables_total`, `child_tables_done` - 자식 테이블 진행 상황
- `current_child_table_relid` - 현재 스캔 중인 자식 테이블의 OID
- `delay_time` - 비용 기반 지연의 총 대기 시간 (밀리초)

##### ANALYZE 단계

- `initializing`: 힙 스캔 시작 준비 중 (짧음)
- `acquiring sample rows`: 샘플 행에 대해 테이블 스캔 중
- `acquiring inherited sample rows`: 샘플 행에 대해 자식 테이블 스캔 중
- `computing statistics`: 샘플에서 통계 계산 중
- `computing extended statistics`: 확장 통계 계산 중
- `finalizing analyze`: `pg_class` 업데이트 중

참고: `ANALYZE`를 `ONLY` 없이 파티션된 테이블에 실행하면 모든 파티션이 재귀적으로 분석됨. 진행 상황은 부모 테이블을 먼저, 이후 각 파티션 순서로 보고됨.

#### 27.4.2 CLUSTER 진행 상황 보고

뷰: `pg_stat_progress_cluster`

이 뷰는 테이블을 재작성하는 `CLUSTER` 및 `VACUUM FULL` 작업의 진행 상황 정보를 포함함.

##### 주요 컬럼

- `pid` - 프로세스 ID
- `datid`, `datname` - 데이터베이스 OID 및 이름
- `relid` - 클러스터링 중인 테이블의 OID
- `command` - `CLUSTER` 또는 `VACUUM FULL`
- `phase` - 현재 처리 단계
- `cluster_index_relid` - 스캔에 사용된 인덱스의 OID (있는 경우)
- `heap_tuples_scanned`, `heap_tuples_written` - 튜플 진행 상황
- `heap_blks_total`, `heap_blks_scanned` - 블록 진행 상황
- `index_rebuild_count` - 다시 빌드된 인덱스 수

##### CLUSTER 및 VACUUM FULL 단계

- `initializing`: 힙 스캔 준비 중
- `seq scanning heap`: 테이블 순차 스캔 중
- `index scanning heap`: 테이블 인덱스 스캔 중 (CLUSTER만 해당)
- `sorting tuples`: 튜플 정렬 중 (CLUSTER만 해당)
- `writing new heap`: 새 힙 작성 중
- `swapping relation files`: 새로 빌드된 파일을 제자리에 교체 중
- `rebuilding index`: 인덱스 다시 빌드 중
- `performing final cleanup`: 최종 정리 작업 중

#### 27.4.3 COPY 진행 상황 보고

뷰: `pg_stat_progress_copy`

이 뷰는 `COPY` 명령의 진행 상황을 추적함.

##### 주요 컬럼

- `pid` - 프로세스 ID
- `datid`, `datname` - 데이터베이스 OID 및 이름
- `relid` - 테이블의 OID (SELECT 쿼리의 경우 0)
- `command` - `COPY FROM` 또는 `COPY TO`
- `type` - I/O 유형: `FILE`, `PROGRAM`, `PIPE`, 또는 `CALLBACK`
- `bytes_processed`, `bytes_total` - 바이트 진행 상황 (사용할 수 없는 경우 0)
- `tuples_processed` - 처리된 튜플
- `tuples_excluded` - WHERE 절로 제외된 튜플
- `tuples_skipped` - 잘못된 데이터로 인해 건너뛴 튜플 (`ON_ERROR` 사용 시)

#### 27.4.4 CREATE INDEX 진행 상황 보고

뷰: `pg_stat_progress_create_index`

`CREATE INDEX` 및 `REINDEX` 작업의 진행 상황을 추적함.

##### 주요 컬럼

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

##### CREATE INDEX 단계

- `initializing`: 인덱스 생성 준비 중
- `waiting for writers before build`: 쓰기 잠금 대기 중 (CONCURRENTLY만 해당)
- `building index`: 액세스 방법별 코드로 인덱스 빌드 중
- `waiting for writers before validation`: 쓰기 잠금 대기 중 (CONCURRENTLY만 해당)
- `index validation: scanning index`: 검증할 튜플에 대해 인덱스 스캔 중
- `index validation: sorting tuples`: 인덱스 출력 정렬 중
- `index validation: scanning table`: 인덱스 튜플을 검증하기 위해 테이블 스캔 중
- `waiting for old snapshots`: 오래된 스냅샷 대기 중 (CONCURRENTLY만 해당)
- `waiting for readers before marking dead`: 읽기 잠금 대기 중 (REINDEX CONCURRENTLY만 해당)
- `waiting for readers before dropping`: 삭제 전 읽기 잠금 대기 중 (REINDEX CONCURRENTLY만 해당)

#### 27.4.5 VACUUM 진행 상황 보고

뷰: `pg_stat_progress_vacuum`

일반 `VACUUM` 작업의 진행 상황을 추적함 (`VACUUM FULL`은 `pg_stat_progress_cluster` 사용).

##### 주요 컬럼

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

##### VACUUM 단계

- `initializing`: 힙 스캔 준비 중
- `scanning heap`: 힙 스캔·정리·조각 모음 및 동결 중
- `vacuuming indexes`: 인덱스 vacuum 중 (힙이 완전히 스캔된 후 발생)
- `vacuuming heap`: 힙 vacuum 중 (인덱스 vacuum 후)
- `cleaning up indexes`: 인덱스 정리 중
- `truncating heap`: OS에 빈 페이지를 반환하기 위해 힙 잘라내기 중
- `performing final cleanup`: 여유 공간 맵 및 통계를 포함한 최종 정리 중

#### 27.4.6 Base Backup 진행 상황 보고

뷰: `pg_stat_progress_basebackup`

`pg_basebackup`이 사용하는 `BASE_BACKUP` 복제 명령의 진행 상황을 추적함.

##### 주요 컬럼

- `pid` - WAL 발신자 프로세스의 프로세스 ID
- `phase` - 현재 처리 단계
- `backup_total` - 스트리밍할 총 데이터 (추정, `--no-estimate-size`인 경우 NULL)
- `backup_streamed` - 스트리밍된 데이터 양
- `tablespaces_total` - 스트리밍할 총 테이블스페이스
- `tablespaces_streamed` - 스트리밍된 테이블스페이스 수

##### Base Backup 단계

- `initializing`: 백업 시작 준비 중
- `waiting for checkpoint to finish`: 백업 시작 체크포인트 대기 중
- `estimating backup size`: 총 데이터베이스 파일 크기 추정 중
- `streaming database files`: 데이터베이스 파일 스트리밍 중
- `waiting for wal archiving to finish`: WAL 아카이빙 대기 중 (`pg_backup_stop` 중)
- `transferring wal files`: WAL 로그 전송 중 (`--wal-method=fetch` 사용 시)

##### 사용 예시

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

### 27.5 동적 추적

PostgreSQL은 코드의 특정 지점에서 외부 유틸리티를 호출해 실행을 추적하는 동적 추적 기능을 제공함. 프로브는 기본적으로 컴파일되지 않으며, 빌드 시 명시적으로 활성화 필요.

지원 도구:
- DTrace (Solaris, macOS, FreeBSD, NetBSD, Oracle Linux)
- SystemTap (Linux 동등물)

#### 27.5.1 동적 추적을 위한 컴파일

DTrace 지원을 활성화하려면 configure 스크립트 실행 시 `--enable-dtrace` 플래그 지정:

```bash
./configure --enable-dtrace
```

이후 컴파일된 PostgreSQL 바이너리에서 프로브 사용 가능.

#### 27.5.2 내장 프로브

PostgreSQL에는 범주별로 구성된 수많은 표준 프로브가 포함되어 있음.

##### 트랜잭션 프로브

- `transaction-start` `(LocalTransactionId)`: 새 트랜잭션 시작 시 발화, arg0은 트랜잭션 ID
- `transaction-commit` `(LocalTransactionId)`: 트랜잭션이 성공적으로 완료되면 발화, arg0은 트랜잭션 ID
- `transaction-abort` `(LocalTransactionId)`: 트랜잭션이 실패로 완료되면 발화, arg0은 트랜잭션 ID

##### 쿼리 프로브

- `query-start` `(const char *)`: 쿼리 처리 시작 시 발화, arg0은 쿼리 문자열
- `query-done` `(const char *)`: 쿼리 처리 완료 시 발화, arg0은 쿼리 문자열
- `query-parse-start` `(const char *)`: 쿼리 파싱 시작 시 발화, arg0은 쿼리 문자열
- `query-parse-done` `(const char *)`: 쿼리 파싱 완료 시 발화, arg0은 쿼리 문자열
- `query-rewrite-start` `(const char *)`: 쿼리 재작성 시작 시 발화, arg0은 쿼리 문자열
- `query-rewrite-done` `(const char *)`: 쿼리 재작성 완료 시 발화, arg0은 쿼리 문자열
- `query-plan-start` `()`: 쿼리 계획 시작 시 발화
- `query-plan-done` `()`: 쿼리 계획 완료 시 발화
- `query-execute-start` `()`: 쿼리 실행 시작 시 발화
- `query-execute-done` `()`: 쿼리 실행 완료 시 발화

##### 명령문 상태

- `statement-status` `(const char *)`: 서버가 `pg_stat_activity`.`status`를 업데이트할 때 발화, arg0은 새 상태 문자열

##### 체크포인트 프로브

- `checkpoint-start` `(int)`: 체크포인트 시작 시 발화
  - arg0: 비트 플래그 (shutdown, immediate, force)
- `checkpoint-done` `(int, int, int, int, int)`: 체크포인트 완료 시 발화
  - arg0: 쓴 버퍼, arg1: 총 버퍼, arg2-arg4: 추가/제거/재활용된 WAL 파일
- `clog-checkpoint-start` `(bool)`: CLOG 체크포인트 부분 시작 시 발화
  - arg0: 정상이면 true, 종료이면 false
- `clog-checkpoint-done` `(bool)`: CLOG 체크포인트 부분 완료 시 발화
- `subtrans-checkpoint-start` `(bool)`: SUBTRANS 체크포인트 부분 시작 시 발화
- `subtrans-checkpoint-done` `(bool)`: SUBTRANS 체크포인트 부분 완료 시 발화
- `multixact-checkpoint-start` `(bool)`: MultiXact 체크포인트 부분 시작 시 발화
- `multixact-checkpoint-done` `(bool)`: MultiXact 체크포인트 부분 완료 시 발화
- `buffer-checkpoint-start` `(int)`: 버퍼 쓰기 부분 시작 시 발화
  - arg0: 비트 플래그
- `buffer-sync-start` `(int, int)`: 더티 버퍼 쓰기 시작 시 발화
  - arg0: 총 버퍼, arg1: 더티 버퍼
- `buffer-sync-written` `(int)`: 각 버퍼 쓰기 후 발화
  - arg0: 버퍼 ID
- `buffer-sync-done` `(int, int, int)`: 모든 더티 버퍼 쓰기 완료 시 발화
  - arg0: 총, arg1: 쓴, arg2: 예상
- `buffer-checkpoint-sync-start` `()`: 더티 버퍼 쓰기 후, fsync 요청 전 발화
- `buffer-checkpoint-done` `()`: 디스크로 버퍼 동기화 완료 시 발화
- `twophase-checkpoint-start` `()`: 2단계 체크포인트 부분 시작 시 발화
- `twophase-checkpoint-done` `()`: 2단계 체크포인트 부분 완료 시 발화

##### 버퍼 프로브

- `buffer-extend-start` `(ForkNumber, BlockNumber, Oid, Oid, Oid, int, unsigned int)`: 관계 확장 시작 시 발화
  - arg0: 포크, arg1: 블록, arg2-4: 테이블스페이스/데이터베이스/관계 OID, arg5: 백엔드 ID 또는 -1, arg6: 요청된 블록
- `buffer-extend-done` `(ForkNumber, BlockNumber, Oid, Oid, Oid, int, unsigned int, BlockNumber)`: 관계 확장 완료 시 발화
  - 추가 arg6: 실제 확장된 블록, arg7: 첫 번째 새 블록 번호
- `buffer-read-start` `(ForkNumber, BlockNumber, Oid, Oid, Oid, int)`: 버퍼 읽기 시작 시 발화
- `buffer-read-done` `(ForkNumber, BlockNumber, Oid, Oid, Oid, int, bool)`: 버퍼 읽기 완료 시 발화
  - arg6: 풀에서 찾으면 true, 그렇지 않으면 false
- `buffer-flush-start` `(ForkNumber, BlockNumber, Oid, Oid, Oid)`: 공유 버퍼에 대한 쓰기 요청 발행 전 발화
- `buffer-flush-done` `(ForkNumber, BlockNumber, Oid, Oid, Oid)`: 쓰기 요청 완료 시 발화 (커널 수준, 디스크 수준 아님)

##### WAL 프로브

- `wal-buffer-write-dirty-start` `()`: 서버가 더티 WAL 버퍼 쓰기 시작 시 발화 (`wal_buffers`가 너무 작을 수 있음을 나타냄)
- `wal-buffer-write-dirty-done` `()`: 더티 WAL 버퍼 쓰기 완료 시 발화
- `wal-insert` `(unsigned char, unsigned char)`: WAL 레코드 삽입 시 발화
  - arg0: 리소스 관리자 (rmid), arg1: 정보 플래그
- `wal-switch` `()`: WAL 세그먼트 전환 요청 시 발화

##### 스토리지 관리자 프로브

- `smgr-md-read-start` `(ForkNumber, BlockNumber, Oid, Oid, Oid, int)`: 관계에서 블록 읽기 시작 시 발화
- `smgr-md-read-done` `(ForkNumber, BlockNumber, Oid, Oid, Oid, int, int, int)`: 블록 읽기 완료 시 발화
  - arg6: 읽은 바이트, arg7: 요청된 바이트
- `smgr-md-write-start` `(ForkNumber, BlockNumber, Oid, Oid, Oid, int)`: 관계에 블록 쓰기 시작 시 발화
- `smgr-md-write-done` `(ForkNumber, BlockNumber, Oid, Oid, Oid, int, int, int)`: 블록 쓰기 완료 시 발화
  - arg6: 쓴 바이트, arg7: 요청된 바이트

##### 정렬 프로브

- `sort-start` `(int, bool, int, int, bool, int)`: 정렬 작업 시작 시 발화
  - arg0: heap/index/datum, arg1: 고유 적용, arg2: 키 컬럼, arg3: 작업 메모리 (KB), arg4: 랜덤 액세스 필요, arg5: 0=serial/1=worker/2=leader
- `sort-done` `(bool, long)`: 정렬 완료 시 발화
  - arg0: 외부이면 true/내부이면 false, arg1: 블록 (외부) 또는 KB (내부)

##### 잠금 프로브

- `lwlock-acquire` `(char *, LWLockMode)`: LWLock 획득 시 발화
  - arg0: tranche, arg1: 잠금 모드 (배타적/공유)
- `lwlock-release` `(char *)`: LWLock 해제 시 발화
  - arg0: tranche
- `lwlock-wait-start` `(char *, LWLockMode)`: LWLock 사용 불가, 프로세스 대기 시 발화
  - arg0: tranche, arg1: 잠금 모드
- `lwlock-wait-done` `(char *, LWLockMode)`: LWLock 대기에서 프로세스 해제 시 발화
  - arg0: tranche, arg1: 잠금 모드
- `lwlock-condacquire` `(char *, LWLockMode)`: 대기 없이 LWLock 성공적으로 획득 시 발화
- `lwlock-condacquire-fail` `(char *, LWLockMode)`: 대기 없이 LWLock 획득 실패 시 발화
- `lock-wait-start` `(unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, LOCKMODE)`: 중량 잠금 (lmgr) 대기 시작 시 발화
  - arg0-3: 태그 필드, arg4: 객체 유형, arg5: 잠금 유형
- `lock-wait-done` `(unsigned int, unsigned int, unsigned int, unsigned int, unsigned int, LOCKMODE)`: 중량 잠금 대기 완료 시 발화 (잠금 획득)
  - 인수는 `lock-wait-start`와 동일

##### 교착 상태 프로브

- `deadlock-found` `()`: 교착 상태 감지기가 교착 상태를 발견할 때 발화

##### 정의된 유형

- `LocalTransactionId` → `unsigned int`
- `LWLockMode` → `int`
- `LOCKMODE` → `int`
- `BlockNumber` → `unsigned int`
- `Oid` → `unsigned int`
- `ForkNumber` → `int`
- `bool` → `unsigned char`

#### 27.5.3 프로브 사용하기

##### DTrace 예시 스크립트

다음 DTrace 스크립트는 트랜잭션 수를 분석함:

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

##### 스크립트 실행

```bash
./txn_count.d `pgrep -n postgres` or ./txn_count.d <PID>
^C

Start                                          71
Commit                                         70
Total time (ns)                        2312105013
```

##### SystemTap 표기법

SystemTap은 DTrace와 다른 표기법을 사용함. SystemTap 스크립트에서는 하이픈 대신 이중 밑줄로 프로브 이름을 참조해야 함 (예: `transaction-start` 대신 `transaction__start`). 이 동작은 향후 SystemTap 릴리스에서 수정될 예정.

##### 모범 사례

- DTrace 스크립트는 신중하게 작성하고 디버그 필요 (계측 오류가 자주 발생함)
- 동적 추적 결과를 공유할 때는 사용한 스크립트를 항상 함께 첨부하고 검증
- 비용이 큰 작업은 `ENABLED()` 매크로로 보호

#### 27.5.4 새 프로브 정의하기

##### 새 프로브 추가 단계

1. 프로브 이름 및 매개변수 결정
2. `src/backend/utils/probes.d`에 프로브 정의 추가
3. `pg_trace.h` 포함하고 원하는 위치에 `TRACE_POSTGRESQL` 매크로 삽입
4. 재컴파일하고 확인

##### 예시: 트랜잭션 프로브 추가

1단계: 프로브 이름을 `transaction-start`로, 매개변수를 `LocalTransactionId`로 결정.

2단계: `src/backend/utils/probes.d`에 추가:

```
probe transaction__start(LocalTransactionId);
```

정의에서 이중 밑줄 주의.

3단계: `pg_trace.h`를 포함하고 매크로 호출 추가:

```c
TRACE_POSTGRESQL_TRANSACTION_START(vxid.localTransactionId);
```

매크로 이름은 단일 밑줄을 사용하며 다음과 같이 프로브 정의에서 파생됨:
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

##### 중요 고려사항

1. 유형 일치
   - 프로브 매개변수에 지정된 데이터 유형은 매크로의 변수 유형과 일치 필요
   - 유형 불일치는 컴파일 오류 발생

2. 성능
   - 대부분의 플랫폼에서 `--enable-dtrace`를 사용하면 추적이 비활성화되어 있어도 추적 매크로 인수가 평가됨
   - 비용이 많이 드는 함수 호출의 경우 `ENABLED()` 매크로로 보호 필요

```c
if (TRACE_POSTGRESQL_TRANSACTION_START_ENABLED())
    TRACE_POSTGRESQL_TRANSACTION_START(some_function(...));
```

각 추적 매크로에는 해당하는 `ENABLED()` 변형 존재.

3. 최소 오버헤드
   - 로컬 변수 값 보고는 최소 오버헤드만 발생
   - 보호 없이 매크로 인수에서 비용이 많이 드는 함수 호출은 회피 필요

---

### 27.6 디스크 사용량 모니터링

이 절에서는 PostgreSQL 데이터베이스 시스템의 디스크 사용량을 모니터링하는 방법을 설명함.

#### 27.6.1 디스크 사용량 확인

##### 저장소 구조

- 각 테이블에는 데이터를 저장하는 기본 힙 디스크 파일 존재
- 넓은 값 컬럼이 있는 테이블은 메인 테이블에 저장하기 너무 큰 값을 위한 TOAST 파일을 가질 수 있음
- TOAST 테이블이 있으면 하나의 연관 인덱스 존재
- 기본 테이블에는 연관된 인덱스가 있을 수 있음
- 각 테이블과 인덱스는 별도의 디스크 파일에 저장됨 (1기가바이트를 초과할 수 있음)

##### 모니터링 방법

세 가지 방법 사용 가능:

1. SQL 함수 (권장): 9.102 데이터베이스 객체 크기 함수 참고
2. oid2name 모듈
3. 시스템 카탈로그 직접 조회

##### 쿼리 예시

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

참고: 각 페이지는 일반적으로 8킬로바이트임. `relpages` 값은 `VACUUM`, `ANALYZE`, `CREATE INDEX` 등 특정 DDL 명령 실행 시에만 갱신됨.

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

#### 27.6.2 디스크 가득 참 장애

##### 주요 고려사항

- 데이터 디스크가 가득 차도 데이터 손상은 발생하지 않지만, 유용한 데이터베이스 작업이 불가해질 수 있음
- WAL (Write-Ahead Log) 디스크가 가득 차면 데이터베이스 서버가 패닉 상태로 종료될 수 있음
- 일부 파일 시스템은 거의 가득 찼을 때 성능이 저하되므로, 완전히 가득 차기 전에 조치 필요

##### 해결책

1. 불필요한 파일 삭제로 공간 확보
2. 테이블스페이스를 사용해 데이터베이스 파일을 다른 파일 시스템으로 이동 (22.6 참고)
3. 사용자별 디스크 할당량 모니터링 - 할당량 초과는 디스크 공간 부족과 동일한 효과를 가짐

---

### 주요 구성 매개변수 요약

- `track_activities`
  - 기본값: on
  - 용도: 현재 명령 추적 활성화
- `track_counts`
  - 기본값: on
  - 용도: 테이블/인덱스 통계 수집
- `track_functions`
  - 기본값: none
  - 용도: 함수 호출 추적 (none, pl, all)
- `track_io_timing`
  - 기본값: off
  - 용도: I/O 타이밍 측정 활성화
- `track_wal_io_timing`
  - 기본값: off
  - 용도: WAL I/O 타이밍 활성화
- `track_cost_delay_timing`
  - 기본값: off
  - 용도: vacuum 지연 타이밍 추적
- `track_activity_query_size`
  - 기본값: 1024
  - 용도: 쿼리 텍스트 잘라내기 길이 (바이트)
- `stats_fetch_consistency`
  - 기본값: cache
  - 용도: 스냅샷/캐시 모드 (snapshot/cache/none)
- `PGSTAT_MIN_INTERVAL`
  - 기본값: 1000
  - 용도: 최소 플러시 간격 (밀리초)

---

### 요약

1. 누적 통계는 테이블/인덱스 액세스, 함수 호출, 일반 데이터베이스 활동을 추적함
2. 동적 뷰는 실시간 세션 및 프로세스 정보를 표시함 (누적 통계와 독립적)
3. 구성 매개변수로 수집 통계를 제어함 (오버헤드와 세부 정보 수준 간 트레이드오프)
4. 보안 모델: 일반 사용자는 자신의 세션만 조회 가능, 슈퍼유저는 모든 세션 조회 가능
5. 캐싱 동작: 통계는 기본적으로 트랜잭션 끝까지 캐시됨 (비용이 큰 쿼리 실행 시 왜곡 발생 가능)
6. 재설정 함수: 통계를 재설정하면 autovacuum 카운터도 함께 재설정되므로, 이후 ANALYZE 실행 권장
7. I/O 추적: OS 유틸리티와 결합해 완전한 I/O 성능 파악 가능
8. 대기 이벤트: 세부적인 대기 분류를 통해 성능 병목 지점 식별에 도움
## 제28장. 안정성과 Write-Ahead Log (Reliability and the Write-Ahead Log)

> PostgreSQL 18 공식 문서 번역

원문: https://www.postgresql.org/docs/current/wal.html

이 장에서는 Write-Ahead Log에 대한 세부 사항을 포함하여 PostgreSQL의 안정성을 제어하는 방법을 설명함.

---

### 목차

- [28.1. 안정성 (Reliability)](#281-안정성-reliability)
- [28.2. 데이터 체크섬 (Data Checksums)](#282-데이터-체크섬-data-checksums)
  - [28.2.1. 오프라인 체크섬 활성화](#2821-오프라인-체크섬-활성화)
- [28.3. Write-Ahead Logging (WAL)](#283-write-ahead-logging-wal)
- [28.4. 비동기 커밋 (Asynchronous Commit)](#284-비동기-커밋-asynchronous-commit)
- [28.5. WAL 구성 (WAL Configuration)](#285-wal-구성-wal-configuration)
- [28.6. WAL 내부 구조 (WAL Internals)](#286-wal-내부-구조-wal-internals)

---

### 28.1. 안정성 (Reliability)

안정성은 모든 심각한 데이터베이스 시스템의 중요한 속성이며, PostgreSQL은 모든 커밋된 트랜잭션의 데이터가 정전·운영 체제 장애·하드웨어 장애(저장 영역 자체의 장애는 제외)로부터 안전한 비휘발성 영역에 저장되도록 보장함. 이 보장을 위해서는 데이터를 영구적으로 저장하기 위한 컴퓨터의 영구 저장소(디스크 드라이브 또는 동등한 장치)에 쓰기가 성공적으로 수행되어야 함.

#### 캐싱 아키텍처의 도전

PostgreSQL은 메인 메모리와 디스크 플래터 사이의 여러 캐시 레이어를 탐색해야 함.

##### 운영 체제 버퍼 캐시

- 자주 요청되는 디스크 블록을 캐시하고 쓰기를 결합
- PostgreSQL은 운영 체제 기능을 사용하여 버퍼 캐시에서 디스크로 쓰기를 강제
- `wal_sync_method` 파라미터로 제어됨

##### 디스크 컨트롤러 캐시

RAID 컨트롤러에서 일반적임.

- Write-through: 쓰기를 즉시 드라이브로 전송
- Write-back: 나중에 데이터를 드라이브로 전송 (휘발성 메모리이므로 안정성 위험 존재)
- 배터리 백업 장치(BBU): 시스템 장애 시 캐시 전원을 유지하여 데이터 손실 방지

##### 디스크 드라이브 캐시

- 일부는 write-through, 다른 일부는 write-back
- 소비자용 IDE 및 SATA 드라이브는 정전 시 생존하지 못하는 write-back 캐시를 가지는 경향
- 많은 SSD에는 휘발성 write-back 캐시 존재

#### 운영 체제별 쓰기 캐시 비활성화

##### Linux

```bash
# IDE/SATA 드라이브 - 쓰기 캐싱 상태 확인
hdparm -I /dev/sdX

# 쓰기 캐싱 비활성화
hdparm -W 0 /dev/sdX

# SCSI 드라이브 - 상태 확인
sdparm --get=WCE /dev/sdX

# SCSI에서 쓰기 캐시 비활성화
sdparm --clear=WCE /dev/sdX
```

##### FreeBSD

- IDE: `camcontrol identify` (쿼리) · `/boot/loader.conf`에서 `hw.ata.wc=0` (비활성화)
- SCSI: `camcontrol identify` (쿼리) · 사용 가능한 경우 `sdparm` (쿼리/변경)

##### Solaris

```bash
format -e
```

참고: ZFS 파일 시스템은 자체 캐시 플러시 명령을 사용하므로 디스크 쓰기 캐시가 활성화되어도 안전함.

##### Windows

- `wal_sync_method`가 `open_datasync`(기본값)인 경우: `내 컴퓨터\디스크 드라이브 열기\속성\하드웨어\속성\정책\디스크에 쓰기 캐싱 사용` 체크 해제
- 또는 `wal_sync_method`를 `fdatasync`(NTFS 전용) 또는 `fsync`로 설정하여 쓰기 캐싱 방지

##### macOS

```
wal_sync_method를 fsync_writethrough로 설정
```

#### 드라이브 캐시 플러시 명령

- SATA 드라이브 (ATAPI-6 이상): `FLUSH CACHE EXT` 명령 지원
- SCSI 드라이브: `SYNCHRONIZE CACHE` 명령 지원
- 이러한 명령은 PostgreSQL에서 직접 접근할 수 없지만 일부 파일 시스템(ZFS, ext4)에서 사용함
- 주의: BBU 컨트롤러와는 비최적 동작 → 동기화 명령이 컨트롤러 캐시의 모든 데이터를 디스크로 강제하여 BBU의 이점을 제거함

##### 테스트 도구

```bash
pg_test_fsync
```

이 프로그램을 사용하여 시스템이 BBU/쓰기 장벽 문제의 영향을 받는지 확인 필요.

#### 관리자의 책임

1. 모든 저장 구성 요소가 데이터 및 파일 시스템 메타데이터의 무결성을 보장하는지 확인
2. 배터리 백업이 없는 쓰기 캐시가 있는 디스크 컨트롤러 사용 자제
3. 드라이브 수준에서는 드라이브가 종료 전 쓰기를 보장할 수 없는 경우 write-back 캐싱 비활성화
4. SSD의 경우 많은 SSD가 기본적으로 캐시 플러시 명령을 준수하지 않음에 유의
5. I/O 서브시스템 안정성 테스트: [`diskchecker.pl`](https://brad.livejournal.com/2116715.html) 활용

#### 부분 페이지 쓰기 보호

##### 문제점

- 디스크 플래터는 512바이트 섹터로 나뉨
- 쓰기 요청은 여러 섹터에 걸칠 수 있음 (PostgreSQL은 일반적으로 8192바이트 = 16섹터 쓰기)
- 쓰기 중 정전으로 작업이 부분적으로 완료될 수 있음

##### PostgreSQL 솔루션

- 실제 디스크 페이지를 수정하기 전에 주기적으로 전체 페이지 이미지를 WAL에 기록
- 충돌 복구 중 부분적으로 쓰인 페이지는 WAL에서 복원됨
- `full_page_writes` 파라미터로 제어됨

참고: ZFS 또는 부분 페이지 쓰기를 방지하는 유사한 파일 시스템에서는 비활성화 가능. BBU 컨트롤러는 데이터가 전체 8kB 페이지로 쓰이도록 보장하지 않는 한 부분 페이지 쓰기를 방지하지 못함.

#### 데이터 손상 보호

##### WAL 레코드 보호

- 각 WAL 파일 레코드는 CRC-32C (32비트) 체크섬으로 보호됨
- CRC는 충돌 복구·아카이브 복구·복제 중에 검증됨

##### 데이터 페이지 보호

- 데이터 페이지는 기본적으로 체크섬됨
- WAL 레코드의 전체 페이지 이미지는 항상 체크섬으로 보호됨

##### 내부 데이터 구조 커버리지

- 직접 체크섬되지 않는 대상: `pg_xact`, `pg_subtrans`, `pg_multixact`, `pg_serial`, `pg_notify`, `pg_stat`, `pg_snapshots`
- 전체 페이지 쓰기로도 보호되지 않는 대상: 동일한 내부 구조
- 보호 방법: WAL 레코드를 통해 충돌 복구 시 정확한 재구축을 허용함 (WAL 레코드 자체는 보호됨)

##### 2단계 커밋 파일

- `pg_twophase`의 개별 상태 파일은 CRC-32C로 보호됨

##### 임시 데이터 파일

- 대규모 SQL 쿼리(정렬·구체화·중간 결과)에서 생성
- 현재 체크섬되지 않으며 WAL 레코드도 기록되지 않음

#### 메모리 오류 보호

PostgreSQL은 수정 가능한 메모리 오류에 대해 보호하지 않음. 시스템이 다음을 사용할 것으로 가정함.

- 산업 표준 오류 수정 코드(ECC) RAM
- 또는 더 나은 보호 표준

---

### 28.2. 데이터 체크섬 (Data Checksums)

기본적으로 데이터 페이지는 체크섬으로 보호되지만, 이 보호는 클러스터에 대해 선택적으로 비활성화 가능. 활성화된 경우:

- 각 데이터 페이지에는 페이지가 쓰일 때 업데이트되는 체크섬이 포함됨
- 페이지가 읽힐 때마다 체크섬이 검증됨
- 데이터 페이지만 보호되며 내부 데이터 구조와 임시 파일은 보호되지 않음

#### 체크섬 활성화/비활성화

##### 클러스터 초기화 중

`--data-checksums` 플래그와 함께 [`initdb`](https://www.postgresql.org/docs/current/app-initdb.html#APP-INITDB-DATA-CHECKSUMS) 명령을 사용하여 클러스터 초기화 시 체크섬 활성화/비활성화 가능.

##### 오프라인 작업

나중에 [`pg_checksums`](https://www.postgresql.org/docs/current/app-pgchecksums.html) 애플리케이션을 사용하여 오프라인 작업으로 체크섬 활성화 또는 비활성화 가능.

중요한 제한 사항:

- 데이터 체크섬은 전체 클러스터 수준에서 활성화 또는 비활성화됨
- 개별 데이터베이스나 테이블에 대해서는 지정 불가

#### 체크섬 상태 확인

읽기 전용 구성 변수를 확인하여 클러스터의 현재 체크섬 상태 확인 가능.

```sql
SHOW data_checksums;
```

#### 구성 파라미터

##### `data_checksums`

- 타입: 읽기 전용 부울 구성 변수
- 목적: 클러스터에 대해 데이터 체크섬이 활성화되어 있는지 표시

##### `ignore_checksum_failure`

- 타입: 구성 파라미터
- 목적: 페이지 손상에서 복구를 시도할 때 체크섬 보호를 우회하기 위해 임시로 설정하는 용도

#### 28.2.1. 오프라인 체크섬 활성화

`pg_checksums` 도구를 사용하여 오프라인 클러스터에서 체크섬을 활성화·비활성화·검증 가능.

```bash
# 체크섬 활성화
pg_checksums --enable -D /path/to/data

# 체크섬 비활성화
pg_checksums --disable -D /path/to/data

# 체크섬 검증
pg_checksums --check -D /path/to/data
```

---

### 28.3. Write-Ahead Logging (WAL)

Write-Ahead Logging (WAL)은 데이터 무결성을 보장하기 위한 표준 방법임.

#### 핵심 개념

WAL의 핵심 원칙은 데이터 파일에 대한 변경은 해당 변경 사항이 로그에 기록된 후에만 이루어져야 한다는 것임. 구체적으로:

- 변경 사항을 설명하는 WAL 레코드가 먼저 영구 저장소로 플러시되어야 함
- 모든 트랜잭션 커밋 시 데이터 페이지를 디스크로 플러시할 필요는 없음
- 충돌 시 로그를 사용하여 데이터베이스 복구 가능
- 커밋된 변경 사항은 WAL 레코드를 통해 다시 실행됨 (롤포워드 복구, REDO 라고도 함)

#### 주요 이점

##### 1. 디스크 I/O 감소

- 트랜잭션 커밋을 보장하기 위해 WAL 파일만 디스크로 플러시하면 됨
- WAL 파일은 순차적으로 쓰이므로 동기화 작업이 여러 데이터 페이지를 플러시하는 것보다 훨씬 저렴함
- 여러 개의 작은 동시 트랜잭션이 WAL 파일의 단일 `fsync`로 커밋 가능

##### 2. 온라인 백업 및 특정 시점 복구 (PITR)

- 온라인 백업 기능을 활성화함
- 사용 가능한 WAL 데이터가 커버하는 모든 시간 순간으로 되돌리기를 지원함
- 물리적 백업은 일정 기간에 걸쳐 수행 가능하며, WAL 재생을 통해 내부 불일치를 수정함
- 프로세스: 이전 물리적 백업 설치 → 원하는 시간까지 WAL 재생

#### 파일 시스템 고려 사항

##### 중요한 팁

- 안정적인 WAL/데이터 파일 저장소를 위해 저널링 파일 시스템이 필수는 아님
- 저널링 오버헤드는 성능을 저하시킬 수 있음 (특히 데이터 플러싱 시)
- 저널링 중 데이터 플러싱은 마운트 옵션으로 비활성화 가능한 경우가 많음
  - 예: Linux ext3에서 `data=writeback`
- 저널링 파일 시스템은 충돌 후 부팅 속도를 향상시킴

---

### 28.4. 비동기 커밋 (Asynchronous Commit)

비동기 커밋은 WAL(Write-Ahead Log) 레코드가 디스크로 플러시되기 전에 성공을 반환하여 트랜잭션을 더 빨리 완료할 수 있게 하는 선택적 모드임. 데이터베이스가 충돌할 경우 최근 트랜잭션의 데이터가 손실될 수 있음.

#### 주요 특성

##### 일반(동기) 커밋

- 서버는 WAL 레코드가 영구 저장소로 플러시될 때까지 대기함
- 클라이언트는 데이터가 내구적으로 쓰인 후에만 성공 확인을 받음
- 즉시 서버 충돌이 발생해도 트랜잭션 보존을 보장함
- 특히 짧은 트랜잭션의 경우 지연 시간이 발생함

##### 비동기 커밋

- 서버는 논리적 트랜잭션 완료 직후 성공을 반환함
- WAL 레코드가 아직 디스크에 도달하지 않은 상태
- 작은 트랜잭션에 대해 상당한 처리량 향상을 제공함
- 데이터 손실에 대한 위험 창을 만듦

#### 데이터 손실 vs 손상

- 위험 요소는 데이터 손실이며 손상은 아님
- 데이터베이스 복구는 마지막으로 플러시된 레코드까지 WAL을 재생함
- 자체적으로 일관된 상태로 복원되지만, 플러시되지 않은 트랜잭션은 손실됨
- 트랜잭션은 커밋 순서대로 재생되므로 불일치는 발생하지 않음

#### 구성

##### 기본 파라미터

`synchronous_commit`: 커밋 모드를 제어하는 사용자 설정 가능 파라미터

- 다른 구성 파라미터와 동일한 방식으로 변경 가능
- 변경 후 시작되는 트랜잭션부터 적용됨
- 동기 트랜잭션과 비동기 트랜잭션을 동시에 혼합 가능

```sql
-- 현재 세션에서 비동기 커밋 활성화
SET synchronous_commit = off;

-- 기본 동기 커밋으로 되돌리기
SET synchronous_commit = on;
```

##### 위험 창 기간

- 백그라운드 WAL 작성자 프로세스에 의해 제한됨
- `wal_writer_delay` 밀리초마다 쓰이지 않은 레코드를 플러시함
- 최대 위험 창: 3 × `wal_writer_delay` (바쁜 기간 동안 페이지를 일괄 쓰기 때문)

#### 중요한 예외

`synchronous_commit` 설정과 관계없이 다음 명령은 항상 동기적으로 커밋됨.

- 유틸리티 명령 (예: `DROP TABLE`)
- 2단계 커밋 명령 (예: `PREPARE TRANSACTION`)

이는 파일 시스템과 데이터베이스 논리적 상태 간의 일관성을 보장하기 위함.

#### 경고

주의: 즉시 모드 종료는 서버 충돌과 동일하며 플러시되지 않은 비동기 커밋의 손실을 초래함.

#### 다른 설정과의 비교

##### vs `fsync = off`

- 범위
  - `synchronous_commit = off`: 트랜잭션별
  - `fsync = off`: 서버 전체
- 손상 위험
  - `synchronous_commit = off`: 없음
  - `fsync = off`: 있음
- 데이터 손실 위험
  - `synchronous_commit = off`: 최근 트랜잭션
  - `fsync = off`: 전체 데이터베이스
- 복구
  - `synchronous_commit = off`: 자체 일관성 상태
  - `fsync = off`: 임의의 손상 가능

##### vs `commit_delay`

- `commit_delay`는 동기 커밋 방식임 (비동기 커밋 시에는 무시됨)
- WAL 플러시 전에 지연을 추가하여 여러 트랜잭션을 묶어 처리함
- 단일 플러시에 트랜잭션을 그룹화하여 플러시 비용을 분산함

#### 사용 사례

##### 비동기 커밋에 적합한 경우:

- 이벤트 로깅
- 중요하지 않은 데이터
- 최근 트랜잭션 손실을 허용하는 애플리케이션

##### 적합하지 않은 경우:

- 금융 트랜잭션 (예: ATM 현금 인출)
- 강력한 내구성 보장이 필요한 모든 시나리오
- 클라이언트가 커밋 확인을 기반으로 외부 작업을 수행하는 경우

---

### 28.5. WAL 구성 (WAL Configuration)

WAL(Write-Ahead Log) 구성은 데이터베이스 성능과 안정성에 영향을 미치는 파라미터를 제공함. 이 섹션에서는 체크포인트 관리·WAL 버퍼 처리 및 복구 최적화를 다룸.

#### 핵심 개념

##### 체크포인트

정의: 해당 체크포인트 이전에 기록된 모든 정보가 힙 및 인덱스 데이터 파일에 반영됨이 보장되는 트랜잭션 시퀀스상의 지점.

체크포인트 프로세스:

1. 모든 더티 데이터 페이지가 디스크로 플러시됨
2. 특수 체크포인트 레코드가 WAL 파일에 기록됨
3. 충돌 후 복구는 최신 체크포인트 레코드(redo 레코드)에서 시작됨
4. redo 레코드 이전의 WAL 세그먼트는 재활용되거나 제거 가능

성능 영향: 모든 더티 데이터 페이지를 플러시하면 상당한 I/O 부하가 발생하므로, 체크포인트 활동은 체크포인트 간격 전반에 걸쳐 I/O를 분산하도록 조절됨.

#### 구성 파라미터

##### 체크포인트 타이밍 파라미터

- `checkpoint_timeout` (기본값 5분): N초마다 체크포인트 시작
- `max_wal_size` (기본값 1 GB): 초과할 것 같으면 체크포인트 시작
- `checkpoint_warning` (기본값 설정 안 됨): 체크포인트가 N초보다 가깝게 발생하면 경고 로깅
- `checkpoint_completion_target` (기본값 0.9): I/O를 분산할 체크포인트 간격의 비율 (0-1.0)
- `checkpoint_flush_after` (기본값 다양): N바이트 후 OS 페이지 플러시 강제 (Linux/POSIX 전용)

참고: 이전 체크포인트 이후 WAL이 기록되지 않았다면 `checkpoint_timeout`이 지나도 체크포인트는 건너뛰어짐.

##### 체크포인트 로직

체크포인트는 다음 중 먼저 발생하는 것에 의해 트리거됨.

- `checkpoint_timeout` 초가 경과
- `max_wal_size`를 초과할 것 같음
- 수동 `CHECKPOINT` SQL 명령

##### WAL 파일 관리 파라미터

- `min_wal_size`: 향후 사용을 위해 재활용되는 최소 WAL 파일 양
- `max_wal_size`: 이전 세그먼트가 제거되기 전 최대 WAL 크기
- `wal_keep_size`: 최근 WAL 파일을 최소 N 메가바이트와 하나의 추가 파일로 유지

WAL 세그먼트 재활용 로직:

- 시스템은 다음 체크포인트까지 예상 필요량을 커버할 충분한 WAL 파일을 재활용함
- 이전 체크포인트 사이클에서 사용된 WAL 파일의 이동 평균을 기반으로 함
- `max_wal_size`를 초과하면 불필요한 세그먼트가 제거됨
- `min_wal_size`는 유휴 상태에서도 최소 재활용 WAL을 보장함

##### WAL 버퍼 파라미터

- `wal_buffers` (기본값 약 16 MB): 공유 메모리의 WAL 버퍼용 디스크 페이지 수
- `full_page_writes` (기본값 on): 체크포인트 후 첫 번째 수정 시 전체 페이지 내용 로깅

튜닝 권장 사항: WAL 출력이 많은 시스템에서는 `wal_buffers`를 늘려 배타적 잠금을 보유하는 동안 `XLogInsertRecord`가 버퍼 쓰기를 강제하는 상황을 방지 필요.

##### 그룹 커밋 파라미터

- `commit_delay` (기본값 0 마이크로초): XLogFlush에서 잠금 획득 후 리더 대기 시간, 팔로워가 큐에 들어갈 수 있도록 함
- `commit_siblings` (기본값 5): commit_delay 대기를 트리거하는 데 필요한 최소 동시 트랜잭션
- `fsync` (기본값 on): WAL 플러싱을 위한 fsync 활성화

최적화 참고:

- `commit_delay`는 `pg_test_fsync`가 8kB 쓰기에 대해 보고하는 평균 시간의 약 절반으로 설정
- 고지연 회전 디스크에서 유용하며 SSD/RAID 어레이에서도 이점이 있을 수 있음
- 너무 높게 설정하면 트랜잭션 지연 시간이 늘어남
- 0으로 설정해도 클라이언트 수가 충분히 많으면 자연스럽게 그룹 커밋 발생

##### WAL 동기화 파라미터

- `wal_sync_method`: PostgreSQL이 WAL 업데이트를 디스크에 강제하는 방법 (fsync, fdatasync, open_sync, open_datasync, fsync_writethrough)
- `track_wal_io_timing`: pg_stat_io에서 쓰기 및 fsync 시간 추적

테스트: `pg_test_fsync` 프로그램을 사용하여 플러시 작업 속도를 측정하고 다른 `wal_sync_method` 옵션 비교 가능.

##### 복구 파라미터

- `recovery_prefetch` (기본값 try): 복구 중 디스크 블록 미리 읽기 활성화
- `maintenance_io_concurrency` (기본값 다양): 프리페치 동시성 제한
- `wal_decode_buffer_size` (기본값 다양): 프리페치 거리 제한
- `wal_debug` (기본값 off): 각 XLogInsertRecord 및 XLogFlush 호출 로깅 (개발 빌드 전용)

#### 재시작점 (아카이브/대기 모드)

정의: 체크포인트와 유사하지만 아카이브 복구 또는 대기 모드 중에 발생함.

특성:

- 모든 상태를 디스크에 강제함
- `pg_control` 파일을 업데이트함
- 이전 WAL 세그먼트 파일을 재활용함
- 체크포인트 레코드에서만 수행 가능
- 한 체크포인트 사이클 분량의 WAL만큼 `max_wal_size`를 초과할 수 있음

모니터링 뷰: `pg_stat_checkpointer` 뷰 카운터.

- `restartpoints_timed`: 스케줄 트리거된 재시작점
- `restartpoints_req`: 요청 트리거된 재시작점
- `restartpoints_done`: 실제로 수행된 재시작점

#### 성능 권장 사항

##### 체크포인트 간격 튜닝

- 빠른 복구가 필요하면 `checkpoint_timeout` 및/또는 `max_wal_size` 감소 (더 자주 체크포인트 발생)
- 트레이드오프: 더 자주 체크포인트하면 전체 페이지 쓰기로 인해 I/O 및 WAL 볼륨 증가
- 건전성 확인: `checkpoint_warning`을 사용하여 체크포인트가 너무 자주 발생하는지 확인 가능

##### 체크포인트 I/O 분산

- 모범 사례: `checkpoint_completion_target`을 0.9(기본값)로 유지
- 권장하지 않음: 1.0으로 설정하면 체크포인트가 늦게 완료되고 가변적인 WAL 세그먼트 필요
- 대안: 복구 시간이 우려된다면 더 자주 체크포인트하도록 `checkpoint_timeout` 감소

##### 고지연 저장소

- `commit_delay`를 `pg_test_fsync`가 보고하는 시간의 약 절반으로 설정
- 더 높은 `commit_siblings` 값 사용
- 두 명의 클라이언트만으로도 상당한 처리량 이점 가능

##### 디스크 공간 관리

- 항상 충분한 여유 공간 확보 필요; `max_wal_size`는 하드 리밋이 아님
- WAL 아카이빙 또는 복제 슬롯이 따라가지 못하는지 모니터링 필요
- WAL 쓰기 및 fsync 통계는 `pg_stat_io`에서 확인 가능

#### WAL 아카이빙 고려 사항

WAL 아카이빙이 활성화된 경우:

- 이전 세그먼트는 아카이빙될 때까지 제거되거나 재활용될 수 없음
- 아카이빙이 뒤처지면 WAL 파일이 `pg_wal`에 축적됨
- 복제 슬롯을 사용하는 느리거나 실패한 대기 서버도 비슷한 영향을 미침
- WAL 요약화(활성화된 경우)도 요약될 때까지 이전 세그먼트를 유지함
- 아카이브 빈도에 하한을 두려면 `archive_timeout` 사용 (체크포인트 파라미터가 아님에 유의)

---

### 28.6. WAL 내부 구조 (WAL Internals)

WAL은 PostgreSQL에서 자동으로 활성화됨. 관리자는 WAL 파일을 위한 적절한 디스크 공간을 확보하고 필요한 튜닝만 수행하면 됨 (28.5 참고).

#### 핵심 개념

##### 로그 시퀀스 번호 (LSN)

- WAL 레코드는 WAL 파일에 순차적으로 추가됨
- 삽입 위치는 로그 시퀀스 번호(LSN)로 나타냄 → WAL에서의 바이트 오프셋
- LSN은 새 레코드마다 단조 증가함
- 데이터 타입: `pg_lsn`
- 용도: 복제 및 복구 진행 측정, 지점 간 WAL 데이터 볼륨 비교

#### WAL 파일 저장 구조

위치: 데이터 디렉토리 아래 `pg_wal` 디렉토리

세그먼트 구성:

- 세그먼트 파일: 일반적으로 각 16 MB (`initdb`의 `--wal-segsize` 옵션으로 구성 가능)
- 각 세그먼트는 페이지로 나뉨: 일반적으로 각 8 kB (`--with-wal-blocksize` configure 옵션으로 구성 가능)
- 명명: `000000010000000000000001`부터 시작하는 계속 증가하는 숫자
- 숫자는 순환하지 않음; 사용 가능한 숫자를 소진하는 데 극도로 오랜 시간 소요

레코드 헤더:

- `access/xlogrecord.h`에 설명됨
- 내용은 로깅되는 이벤트 유형에 따라 다름

#### 성능 최적화

디스크 배치:

WAL은 이상적으로 메인 데이터베이스 파일과 다른 디스크에 위치해야 함. 다음과 같은 방법으로 달성 가능.

1. 서버 종료
2. `pg_wal` 디렉토리를 다른 위치로 이동
3. 원래 위치에서 새 위치로 심볼릭 링크 생성

```bash
# 예시
pg_ctl stop -D /path/to/data
mv /path/to/data/pg_wal /new/disk/pg_wal
ln -s /new/disk/pg_wal /path/to/data/pg_wal
pg_ctl start -D /path/to/data
```

#### 안정성 고려 사항

##### 데이터 무결성 위험

- WAL은 데이터베이스 레코드가 변경되기 전에 로그가 기록되도록 하는 것을 목표로 함
- 위험 요소: 디스크 드라이브가 데이터를 캐시에만 기록하고 실제로 지속되지 않은 상태에서 쓰기 성공을 허위로 보고할 수 있음
- 이러한 경우 정전은 복구 불가능한 데이터 손상을 유발할 수 있음
- 권장 사항: PostgreSQL WAL 파일을 보유하는 디스크가 거짓 쓰기 보고를 하지 않도록 보장 필요 (28.1 참고)

##### 복구 프로세스

1. 서버가 시작 시 `pg_control` 파일을 읽음
2. `pg_control`에서 체크포인트 레코드를 읽음
3. 체크포인트 레코드의 WAL 위치에서 앞으로 스캔하여 REDO 작업 수행
4. 체크포인트 이후 변경된 모든 페이지를 일관된 상태로 복원 (`full_page_writes`가 활성화된 경우)

##### pg_control 보호 장치

- 체크포인트 위치는 체크포인트 및 WAL 플러시 후 `pg_control`에 저장됨
- 파일 크기: 하나의 디스크 페이지 미만 (부분 쓰기 문제의 대상이 아님)
- 참고: `pg_control` 손상 시 최신 체크포인트를 찾기 위해 WAL 세그먼트를 역방향으로 스캔하는 기능은 구현되어 있지 않음
- 실제로는 `pg_control` 자체를 읽지 못해 데이터베이스 장애가 발생한 사례는 보고되지 않음
- 이론적으로는 약점이지만 실제 문제로 이어진 사례는 없음

#### 내부 WAL 함수

- `XLogInsertRecord`: WAL 버퍼에 새 레코드 배치 (배타적 잠금을 가진 모든 저수준 수정에서)
- `XLogFlush`: WAL 버퍼 쓰기 및 플러시 (일반적으로 트랜잭션 커밋 시)
- `XLogWrite`: WAL 버퍼를 디스크에 쓰기
- `issue_xlog_fsync`: WAL 파일을 디스크에 동기화

---

### 요약

#### WAL의 핵심 이점

1. 데이터 무결성: 충돌 시에도 커밋된 트랜잭션 보존
2. 성능 향상: 순차적 WAL 쓰기로 랜덤 I/O 감소
3. 복구 능력: 특정 시점 복구 (PITR) 지원
4. 복제 기반: 스트리밍 복제 및 논리적 복제의 기초

#### 주요 구성 고려 사항

- 빠른 복구가 목표: 낮은 `checkpoint_timeout`, 낮은 `max_wal_size` 권장
- 높은 처리량이 목표: 높은 `wal_buffers`, 적절한 `commit_delay` 권장
- 데이터 안정성이 목표: `synchronous_commit = on`, `fsync = on` 권장
- 성능 우선이 목표: `synchronous_commit = off` 권장 (중요하지 않은 데이터 한정)

#### 모니터링 뷰

```sql
-- 체크포인트 통계
SELECT * FROM pg_stat_checkpointer;

-- WAL I/O 통계
SELECT * FROM pg_stat_io WHERE backend_type = 'walwriter';

-- 복제 상태
SELECT * FROM pg_stat_replication;
```

---

### 참고

- Section 25.3: 연속 아카이빙 및 특정 시점 복구
- Section 26.2.5: 스트리밍 복제
- Section 28.1: 안정성
- Section 28.5: WAL 구성
- pg_test_fsync: WAL 동기화 성능 테스트 도구
- pg_checksums: 오프라인 체크섬 관리 도구

---
## 제29장. 논리적 복제 (Logical Replication)

> PostgreSQL 18 공식 문서 번역

원문: [https://www.postgresql.org/docs/current/logical-replication.html](https://www.postgresql.org/docs/current/logical-replication.html)

---

### 목차

- [개요](#개요)
- [29.1. 발행 (Publication)](#291-발행-publication)
  - [29.1.1. 복제 식별자 (Replica Identity)](#2911-복제-식별자-replica-identity)
- [29.2. 구독 (Subscription)](#292-구독-subscription)
  - [29.2.1. 복제 슬롯 관리](#2921-복제-슬롯-관리)
  - [29.2.2. 예제: 논리적 복제 설정](#2922-예제-논리적-복제-설정)
  - [29.2.3. 예제: 지연된 복제 슬롯 생성](#2923-예제-지연된-복제-슬롯-생성)
- [29.3. 논리적 복제 장애 조치](#293-논리적-복제-장애-조치)
- [29.4. 행 필터 (Row Filters)](#294-행-필터-row-filters)
  - [29.4.1. 행 필터 규칙](#2941-행-필터-규칙)
  - [29.4.2. 표현식 제한사항](#2942-표현식-제한사항)
  - [29.4.3. UPDATE 변환](#2943-update-변환)
  - [29.4.4. 파티션 테이블](#2944-파티션-테이블)
  - [29.4.5. 초기 데이터 동기화](#2945-초기-데이터-동기화)
  - [29.4.6. 다중 행 필터 결합](#2946-다중-행-필터-결합)
  - [29.4.7. 예제](#2947-예제)
- [29.5. 컬럼 목록 (Column Lists)](#295-컬럼-목록-column-lists)
  - [29.5.1. 예제](#2951-예제)
- [29.6. 생성된 컬럼 복제](#296-생성된-컬럼-복제)
- [29.7. 충돌 (Conflicts)](#297-충돌-conflicts)
- [29.8. 제한사항 (Restrictions)](#298-제한사항-restrictions)
- [29.9. 아키텍처 (Architecture)](#299-아키텍처-architecture)
  - [29.9.1. 초기 스냅샷](#2991-초기-스냅샷)
- [29.10. 모니터링 (Monitoring)](#2910-모니터링-monitoring)
- [29.11. 보안 (Security)](#2911-보안-security)
- [29.12. 구성 설정](#2912-구성-설정)
  - [29.12.1. 발행자](#29121-발행자)
  - [29.12.2. 구독자](#29122-구독자)
- [29.13. 업그레이드](#2913-업그레이드)
  - [29.13.1. 발행자 업그레이드 준비](#29131-발행자-업그레이드-준비)
  - [29.13.2. 구독자 업그레이드 준비](#29132-구독자-업그레이드-준비)
  - [29.13.3. 논리적 복제 클러스터 업그레이드](#29133-논리적-복제-클러스터-업그레이드)
- [29.14. 빠른 설정](#2914-빠른-설정)

---

### 개요

논리적 복제(Logical Replication)는 복제 식별자(일반적으로 기본 키)를 기반으로 데이터 객체와 해당 변경 사항을 복제하는 방법. 정확한 블록 주소와 바이트 단위 복제를 사용하는 물리적 복제와 대조됨.

논리적 복제는 발행 및 구독 모델(publish and subscribe model)을 사용함. 하나 이상의 구독자(subscriber)가 발행자(publisher) 노드의 하나 이상의 발행(publication)을 구독함 → 구독자는 발행에서 데이터를 가져오며, 캐스케이딩 복제를 위해 데이터를 다시 발행할 수도 있음.

논리적 복제는 테이블 데이터의 초기 스냅샷을 가져온 다음 후속 변경 사항을 전송함 → 단일 구독 내에서 트랜잭션 일관성 보장.

#### 주요 사용 사례

논리적 복제의 주요 사용 사례:

- 데이터베이스의 증분 변경 사항을 수신하는 즉시 구독자에게 전송
- 변경 사항이 구독자에 도착할 때 트리거 실행
- 여러 데이터베이스를 단일 데이터베이스로 통합 (예: 분석 목적)
- 서로 다른 PostgreSQL 메이저 버전 간 복제
- 서로 다른 플랫폼 간 복제 (예: Linux에서 Windows로)
- 서로 다른 사용자 그룹에게 복제된 데이터에 대한 접근 권한 부여
- 데이터베이스의 하위 집합을 다른 서버와 공유

---

### 29.1. 발행 (Publication)

발행(Publication)은 테이블 또는 테이블 그룹에서 생성된 변경 사항의 집합이며, 변경 집합(change set) 또는 복제 집합(replication set)이라고도 함. 발행은 모든 물리적 복제 주 서버에서 정의 가능하며, 발행이 정의된 노드를 발행자(publisher)라고 함.

#### 주요 특성

- 데이터베이스 범위: 각 발행은 하나의 데이터베이스에만 존재
- 스키마와 독립적: 발행은 스키마와 다르며 테이블 접근에 영향을 주지 않음
- 다중 발행: 각 테이블은 여러 발행에 추가 가능
- 콘텐츠 유형: 테이블과 스키마의 모든 테이블을 포함 가능
- 명시적 추가: `ALL TABLES`로 생성된 발행을 제외하고 객체는 명시적으로 추가 필요

#### 작업 유형

발행은 다음 변경 사항의 조합으로 제한 가능:

- `INSERT`
- `UPDATE`
- `DELETE`
- `TRUNCATE`

기본값: 모든 작업 유형이 복제됨.

참고: 이 설정은 DML 작업에만 적용되며 초기 데이터 동기화에는 영향을 미치지 않음. 행 필터는 `TRUNCATE` 명령에는 적용되지 않음.

#### 발행 관리

발행은 다음 명령으로 관리됨:

- 생성: `CREATE PUBLICATION` 명령
- 수정: `ALTER PUBLICATION` 명령
- 삭제: `DROP PUBLICATION` 명령

`ALTER PUBLICATION`을 사용하여 테이블을 동적으로 추가·제거 가능:

- `ADD TABLE` - 트랜잭션 작업
- `DROP TABLE` - 트랜잭션 작업

두 작업 모두 트랜잭션이 커밋되면 올바른 스냅샷에서 복제를 시작/중지함.

모든 발행은 여러 구독자를 가질 수 있음.

#### 29.1.1. 복제 식별자 (Replica Identity)

발행된 테이블은 `UPDATE` 및 `DELETE` 작업을 복제하기 위해 복제 식별자(replica identity)가 구성되어 있어야 함 → 구독자 측에서 적절한 행을 식별 가능.

##### 복제 식별자 옵션

###### 1. 기본 키 (기본값)
기본 키가 존재하는 경우 기본적으로 사용됨.

###### 2. 고유 인덱스
다른 고유 인덱스를 복제 식별자로 설정 가능(특정 추가 요구 사항 있음).

###### 3. FULL
- 전체 행이 키가 됨.
- 적합한 키가 없을 때 사용됨.
- 구독자 측의 인덱스가 행 검색을 지원할 수 있음.
- 후보 인덱스 요구 사항:
  - btree 또는 hash여야 함.
  - 부분 인덱스가 아니어야 함.
  - 인덱스의 가장 왼쪽 필드는 발행된 테이블 컬럼을 참조하는 컬럼(표현식이 아님)이어야 함.

경고: 적합한 인덱스가 없으면 `FULL` 복제 식별자는 매우 비효율적이며 대체 수단으로만 사용 권장.

###### 4. NOTHING / 기본 키 없는 DEFAULT / 삭제된 인덱스의 USING INDEX
이러한 작업을 복제하는 발행에서 `UPDATE` 또는 `DELETE` 작업 지원 불가. 이러한 작업을 시도하면 발행자에서 오류 발생.

##### 중요 규칙

- INSERT 작업: 복제 식별자에 관계없이 진행됨.
- 구독자 구성: 발행자에 `FULL` 이외의 복제 식별자가 설정된 경우 구독자 측에 동일하거나 더 적은 컬럼이 설정되어야 함.

##### 구성

복제 식별자 설정에 대한 자세한 내용은 `ALTER TABLE...REPLICA IDENTITY` 참고.

---

### 29.2. 구독 (Subscription)

구독(Subscription)은 논리적 복제의 다운스트림 측. 구독이 정의된 노드를 구독자(subscriber)라고 함. 구독은 다른 데이터베이스에 대한 연결과 구독하려는 발행 집합을 정의함.

#### 주요 특성

- 구독자 데이터베이스는 다른 PostgreSQL 인스턴스처럼 동작하며 다른 데이터베이스의 발행자가 될 수 있음.
- 구독자 노드는 여러 구독을 가질 수 있음.
- 단일 발행자-구독자 쌍 사이에 여러 구독이 존재할 수 있음(발행 객체 중복에 주의 필요).
- 각 구독은 하나의 복제 슬롯을 통해 변경 사항을 수신함.
- 논리적 복제 구독은 동기 복제를 위한 대기 서버가 될 수 있음(대기 이름은 기본적으로 구독 이름).
- 구독은 현재 사용자가 슈퍼유저인 경우에만 `pg_dump`로 덤프됨.

#### 관리 명령

구독은 세 가지 주요 명령으로 관리됨:

- CREATE SUBSCRIPTION - 구독 추가
- ALTER SUBSCRIPTION - 구독 중지/재개
- DROP SUBSCRIPTION - 구독 제거

중요 참고: 구독이 삭제되고 다시 생성되면 동기화 정보가 손실되어 데이터를 다시 동기화해야 함.

#### 스키마 및 테이블 매칭

- 스키마 정의는 복제되지 않음. 발행된 테이블이 구독자에 존재해야 함.
- 일반 테이블만 복제 대상 가능(뷰 불가).
- 테이블은 정규화된 테이블 이름으로 매칭됨(다른 이름의 테이블로의 복제는 지원되지 않음).
- 컬럼은 이름으로 매칭됨(순서는 일치할 필요 없음).
- 데이터 유형은 텍스트 표현이 대상 유형으로 변환될 수 있으면 일치할 필요 없음(예: `integer` -> `bigint`).
- 대상 테이블은 추가 컬럼을 가질 수 있음(기본값으로 채워짐).
- 바이너리 복제는 컬럼 매칭에 대해 더 제한적임.

#### 29.2.1. 복제 슬롯 관리

각 활성 구독은 원격(발행) 측의 복제 슬롯에서 변경 사항을 수신함.

##### 테이블 동기화 슬롯

테이블 동기화 슬롯은 일시적이며, 초기 테이블 동기화를 위해 내부적으로 생성되고 더 이상 필요하지 않으면 자동으로 삭제됨. 명명 패턴: `pg_%u_sync_%u_%llu` (구독 OID, 테이블 relid, 시스템 식별자)

##### 슬롯 관리 시나리오

###### 1. 복제 슬롯이 이미 존재하는 경우

`create_slot = false` 옵션을 사용하여 기존 슬롯과 연결:

```sql
CREATE SUBSCRIPTION sub1
  CONNECTION '...'
  PUBLICATION pub1
  WITH (create_slot = false);
```

###### 2. 구독 생성 시 원격 호스트에 연결할 수 없는 경우

`connect = false` 옵션 사용(`pg_dump`에서 사용):

```sql
CREATE SUBSCRIPTION sub1
  CONNECTION '...'
  PUBLICATION pub1
  WITH (connect = false);
```

구독 활성화 전에 원격 복제 슬롯을 수동으로 생성해야 함.

###### 3. 구독 삭제 시 복제 슬롯 유지

구독자 데이터베이스를 다른 호스트로 이동할 때 유용함:

```sql
ALTER SUBSCRIPTION sub1 DISABLE;
ALTER SUBSCRIPTION sub1 SET (slot_name = NONE);
DROP SUBSCRIPTION sub1;
```

###### 4. 구독 삭제 시 원격 호스트에 연결할 수 없는 경우

삭제 전에 슬롯을 분리:

```sql
ALTER SUBSCRIPTION sub1 SET (slot_name = NONE);
DROP SUBSCRIPTION sub1;
```

원격 인스턴스가 더 이상 존재하지 않는 경우에는 추가 조치 불필요. 그렇지 않은 경우에는 남은 슬롯을 수동으로 삭제하여 WAL 예약 및 디스크 공간 문제 방지 필요.

#### 29.2.2. 예제: 논리적 복제 설정

##### 단계 1: 발행자에 테스트 테이블 생성

```sql
/* pub # */ CREATE TABLE t1(a int, b text, PRIMARY KEY(a));
/* pub # */ CREATE TABLE t2(c int, d text, PRIMARY KEY(c));
/* pub # */ CREATE TABLE t3(e int, f text, PRIMARY KEY(e));
```

##### 단계 2: 구독자에 동일한 테이블 생성

```sql
/* sub # */ CREATE TABLE t1(a int, b text, PRIMARY KEY(a));
/* sub # */ CREATE TABLE t2(c int, d text, PRIMARY KEY(c));
/* sub # */ CREATE TABLE t3(e int, f text, PRIMARY KEY(e));
```

##### 단계 3: 발행자 측에 데이터 삽입

```sql
/* pub # */ INSERT INTO t1 VALUES (1, 'one'), (2, 'two'), (3, 'three');
/* pub # */ INSERT INTO t2 VALUES (1, 'A'), (2, 'B'), (3, 'C');
/* pub # */ INSERT INTO t3 VALUES (1, 'i'), (2, 'ii'), (3, 'iii');
```

##### 단계 4: 발행 생성

```sql
/* pub # */ CREATE PUBLICATION pub1 FOR TABLE t1;
/* pub # */ CREATE PUBLICATION pub2 FOR TABLE t2 WITH (publish = 'truncate');
/* pub # */ CREATE PUBLICATION pub3a FOR TABLE t3 WITH (publish = 'truncate');
/* pub # */ CREATE PUBLICATION pub3b FOR TABLE t3 WHERE (e > 5);
```

발행 세부사항:
- `pub1`: t1의 모든 작업을 발행
- `pub2`: t2의 TRUNCATE 작업만 발행
- `pub3a`: t3의 TRUNCATE 작업만 발행
- `pub3b`: 행 필터(e > 5)가 있는 t3 발행

##### 단계 5: 구독 생성

```sql
/* sub # */ CREATE SUBSCRIPTION sub1
/* sub - */ CONNECTION 'host=localhost dbname=test_pub application_name=sub1'
/* sub - */ PUBLICATION pub1;

/* sub # */ CREATE SUBSCRIPTION sub2
/* sub - */ CONNECTION 'host=localhost dbname=test_pub application_name=sub2'
/* sub - */ PUBLICATION pub2;

/* sub # */ CREATE SUBSCRIPTION sub3
/* sub - */ CONNECTION 'host=localhost dbname=test_pub application_name=sub3'
/* sub - */ PUBLICATION pub3a, pub3b;
```

##### 단계 6: 초기 데이터 복사 확인

발행의 `publish` 작업에 관계없이 초기 테이블 데이터가 복사됨.

초기 동기화 후 구독자:

```sql
/* sub # */ SELECT * FROM t1;
 a |   b
---+-------
 1 | one
 2 | two
 3 | three
(3 rows)

/* sub # */ SELECT * FROM t2;
 c | d
---+---
 1 | A
 2 | B
 3 | C
(3 rows)

/* sub # */ SELECT * FROM t3;
 e |  f
---+-----
 1 | i
 2 | ii
 3 | iii
(3 rows)
```

참고: `pub3b`에 행 필터(e > 5)가 있지만 `pub3a`에 행 필터가 없으므로 초기 데이터 복사에는 모든 행이 포함됨.

##### 단계 7: 발행자에 추가 데이터 삽입

```sql
/* pub # */ INSERT INTO t1 VALUES (4, 'four'), (5, 'five'), (6, 'six');
/* pub # */ INSERT INTO t2 VALUES (4, 'D'), (5, 'E'), (6, 'F');
/* pub # */ INSERT INTO t3 VALUES (4, 'iv'), (5, 'v'), (6, 'vi');
```

삽입 후 발행자 데이터:

```sql
/* pub # */ SELECT * FROM t1;
 a |   b
---+-------
 1 | one
 2 | two
 3 | three
 4 | four
 5 | five
 6 | six
(6 rows)

/* pub # */ SELECT * FROM t2;
 c | d
---+---
 1 | A
 2 | B
 3 | C
 4 | D
 5 | E
 6 | F
(6 rows)

/* pub # */ SELECT * FROM t3;
 e |  f
---+-----
 1 | i
 2 | ii
 3 | iii
 4 | iv
 5 | v
 6 | vi
(6 rows)
```

##### 단계 8: 일반 복제가 발행 설정을 준수하는지 확인

일반 복제 중에는 적절한 `publish` 작업이 적용됨:
- `pub2`와 `pub3a`는 INSERT 작업을 복제하지 않음.
- `pub3b`는 행 필터(e > 5)에 맞는 데이터만 복제함.

일반 복제 후 구독자 데이터:

```sql
/* sub # */ SELECT * FROM t1;
 a |   b
---+-------
 1 | one
 2 | two
 3 | three
 4 | four
 5 | five
 6 | six
(6 rows)

/* sub # */ SELECT * FROM t2;
 c | d
---+---
 1 | A
 2 | B
 3 | C
(3 rows)

/* sub # */ SELECT * FROM t3;
 e |  f
---+-----
 1 | i
 2 | ii
 3 | iii
 6 | vi
(4 rows)
```

결과 설명:
- `t1`: 모든 6행 (pub1이 모든 작업을 발행)
- `t2`: 3행만 (pub2는 INSERT를 복제하지 않으므로 새 행이 추가되지 않음)
- `t3`: 4행 (초기 동기화에서 3행 · pub3b의 행 필터로 인해 e=6인 행만 추가)

#### 29.2.3. 예제: 지연된 복제 슬롯 생성

원격 복제 슬롯이 자동으로 생성되지 않는 경우 구독 활성화 전에 수동으로 생성해야 함. 이 예제들은 표준 논리적 디코딩 출력 플러그인(`pgoutput`)을 사용함.

##### 초기 설정

예제를 위한 발행 생성:

```sql
/* pub # */ CREATE PUBLICATION pub1 FOR ALL TABLES;
```

##### 예제 1: `connect = false` (슬롯 이름이 기본적으로 구독 이름)

구독자에서 - 구독 생성:

```sql
/* sub # */ CREATE SUBSCRIPTION sub1
/* sub - */ CONNECTION 'host=localhost dbname=test_pub'
/* sub - */ PUBLICATION pub1
/* sub - */ WITH (connect=false);
WARNING:  subscription was created, but is not connected
HINT:  To initiate replication, you must manually create the replication slot, enable the subscription, and refresh the subscription.
```

발행자에서 - 슬롯 수동 생성:

```sql
/* pub # */ SELECT * FROM pg_create_logical_replication_slot('sub1', 'pgoutput');
 slot_name |    lsn
-----------+-----------
 sub1      | 0/19404D0
(1 row)
```

구독자에서 - 활성화 완료:

```sql
/* sub # */ ALTER SUBSCRIPTION sub1 ENABLE;
/* sub # */ ALTER SUBSCRIPTION sub1 REFRESH PUBLICATION;
```

##### 예제 2: `connect = false`와 명시적 `slot_name`

구독자에서 - 구독 생성:

```sql
/* sub # */ CREATE SUBSCRIPTION sub1
/* sub - */ CONNECTION 'host=localhost dbname=test_pub'
/* sub - */ PUBLICATION pub1
/* sub - */ WITH (connect=false, slot_name='myslot');
WARNING:  subscription was created, but is not connected
HINT:  To initiate replication, you must manually create the replication slot, enable the subscription, and refresh the subscription.
```

발행자에서 - 지정된 이름으로 슬롯 수동 생성:

```sql
/* pub # */ SELECT * FROM pg_create_logical_replication_slot('myslot', 'pgoutput');
 slot_name |    lsn
-----------+-----------
 myslot    | 0/19059A0
(1 row)
```

구독자에서 - 활성화 완료:

```sql
/* sub # */ ALTER SUBSCRIPTION sub1 ENABLE;
/* sub # */ ALTER SUBSCRIPTION sub1 REFRESH PUBLICATION;
```

##### 예제 3: `slot_name = NONE` (완전 지연된 슬롯 생성)

구독자에서 - 구독 생성:

```sql
/* sub # */ CREATE SUBSCRIPTION sub1
/* sub - */ CONNECTION 'host=localhost dbname=test_pub'
/* sub - */ PUBLICATION pub1
/* sub - */ WITH (slot_name=NONE, enabled=false, create_slot=false);
```

발행자에서 - 임의의 이름으로 슬롯 수동 생성:

```sql
/* pub # */ SELECT * FROM pg_create_logical_replication_slot('myslot', 'pgoutput');
 slot_name |    lsn
-----------+-----------
 myslot    | 0/1905930
(1 row)
```

구독자에서 - 구독과 슬롯 연결:

```sql
/* sub # */ ALTER SUBSCRIPTION sub1 SET (slot_name='myslot');
```

구독자에서 - 활성화 완료:

```sql
/* sub # */ ALTER SUBSCRIPTION sub1 ENABLE;
/* sub # */ ALTER SUBSCRIPTION sub1 REFRESH PUBLICATION;
```

##### 구독 옵션 요약 표

- `create_slot`: 복제 슬롯 자동 생성 여부
  - 예: `WITH (create_slot = false)`
- `connect`: 발행자에 즉시 연결 여부
  - 예: `WITH (connect = false)`
- `slot_name`: 사용할 복제 슬롯 이름
  - 예: `WITH (slot_name = 'myslot')` 또는 `WITH (slot_name = NONE)`
- `enabled`: 구독 초기 활성화 여부
  - 예: `WITH (enabled = false)`
- `application_name`: 동기 복제용 애플리케이션 이름
  - 예: `CONNECTION '...' application_name=sub1'`

---

### 29.3. 논리적 복제 장애 조치

논리적 복제 장애 조치를 통해 주 발행자 노드가 다운되더라도 구독자 노드가 발행자로부터 데이터 복제를 계속할 수 있음. 이를 위해 다음이 필요:

1. 발행자 노드에 해당하는 물리적 대기 서버
2. `failover = true` 매개변수를 사용하여 대기 서버에 동기화된 논리적 슬롯
3. 적절한 동기화 구성

#### 주요 개념

장애 조치 매개변수: 구독 생성 시 `failover = true`를 지정하여 논리적 슬롯을 대기 서버에 동기화함 → 대기 서버 승격 후 원활한 전환 가능.

동기화 요구사항:
- 슬롯 동기화는 비동기적으로 수행됨.
- 대기 서버가 구독자보다 앞서 유지되도록 `synchronized_standby_slots`를 구성함.
- 장애 조치 전에 대기 서버가 준비되었는지 확인 필요.

#### 장애 조치 준비 상태 확인 단계

##### 단계 1: 구독자에서 복제 슬롯 식별

동기화해야 할 슬롯을 찾기 위해 구독자 노드에서 실행:

```sql
/* sub # */ SELECT
               array_agg(quote_literal(s.subslotname)) AS slots
           FROM  pg_subscription s
           WHERE s.subfailover AND
                 s.subslotname IS NOT NULL;
```

결과 예시:

```
 slots
-------
 {'sub1','sub2','sub3'}
(1 row)
```

##### 단계 2: 구독자에서 테이블 동기화 슬롯 식별

장애 조치가 활성화된 구독이 포함된 각 데이터베이스에서 실행:

```sql
/* sub # */ SELECT
               array_agg(quote_literal(slot_name)) AS slots
           FROM
           (
               SELECT CONCAT('pg_', srsubid, '_sync_', srrelid, '_', ctl.system_identifier) AS slot_name
               FROM pg_control_system() ctl, pg_subscription_rel r, pg_subscription s
               WHERE r.srsubstate = 'f' AND s.oid = r.srsubid AND s.subfailover
           );
```

결과 예시:

```
 slots
-------
 {'pg_16394_sync_16385_7394666715149055164'}
(1 row)
```

참고: 테이블 동기화 슬롯은 테이블 복사가 완료된 경우에만 동기화 필요(52.55절 참고).

##### 단계 3: 대기 서버에서 슬롯 확인

식별된 슬롯이 대기 서버에 존재하고 장애 조치 준비가 되었는지 확인:

```sql
/* standby # */ SELECT slot_name, (synced AND NOT temporary AND invalidation_reason IS NULL) AS failover_ready
               FROM pg_replication_slots
               WHERE slot_name IN
                   ('sub1','sub2','sub3', 'pg_16394_sync_16385_7394666715149055164');
```

결과 예시:

```
  slot_name                                 | failover_ready
--------------------------------------------+----------------
  sub1                                      | t
  sub2                                      | t
  sub3                                      | t
  pg_16394_sync_16385_7394666715149055164   | t
(4 rows)
```

성공 기준: 모든 슬롯이 `failover_ready = true`로 존재하면 장애 조치 후 새 주 서버에서 구독 계속 가능.

#### 계획된 장애 조치를 위한 대체 방법

계획된 장애 조치 또는 모든 구독자(PostgreSQL 및 비PostgreSQL) 확인을 위해 주 서버에서 직접 쿼리:

```sql
/* primary # */ SELECT array_agg(quote_literal(r.slot_name)) AS slots
               FROM pg_replication_slots r
               WHERE r.failover AND NOT r.temporary;
```

결과 예시:

```
 slots
-------
 {'sub1','sub2','sub3', 'pg_16394_sync_16385_7394666715149055164'}
(1 row)
```

#### 중요 고려사항

- 장애 조치 후, 지정된 대기 서버가 담당할 각 구독자 노드에서 단계 1-2를 실행함.
- 비PostgreSQL 구독자는 복제 슬롯을 식별하기 위해 자체 방법을 사용할 수 있음.
- 장애 조치를 진행하기 전에 확인 목록 완료 필요.
- 모든 슬롯이 대기 서버에 존재하고 `failover_ready = true`를 표시해야 함.

---

### 29.4. 행 필터 (Row Filters)

행 필터를 사용하면 발행된 테이블에서 구독자로 데이터를 선택적으로 복제 가능. 기본적으로 발행된 테이블의 모든 데이터가 복제되지만 행 필터는 `WHERE` 절을 사용하여 복제할 행을 지정해 이를 줄임.

#### 29.4.1. 행 필터 규칙

주요 규칙:
- 행 필터는 변경 사항을 발행하기 전에 적용됨.
- 행 필터가 `false` 또는 `NULL`로 평가되면 해당 행은 복제되지 않음.
- `WHERE` 절은 복제 연결 역할을 사용하여 평가됨.
- 행 필터는 `TRUNCATE` 명령에 영향을 미치지 않음.

#### 29.4.2. 표현식 제한사항

허용되는 표현식:
- 단순 표현식만 허용
- 포함할 수 없는 것:
  - 사용자 정의 함수·연산자·유형·콜레이션
  - 시스템 컬럼 참조
  - 불변(immutable)이 아닌 내장 함수

컬럼 제한:
- `UPDATE` 또는 `DELETE` 작업의 경우: 행 필터는 복제 식별자가 포함하는 컬럼만 포함해야 함.
- `INSERT` 작업만 해당: 행 필터는 모든 컬럼을 사용 가능.

#### 29.4.3. UPDATE 변환

`UPDATE`가 처리될 때 행 필터는 이전 행과 새 행 모두에 대해 평가됨.

변환 규칙:

- 이전 행이 일치 안 함 · 새 행이 일치 안 함 → 복제 안 함
- 이전 행이 일치 안 함 · 새 행이 일치 → `INSERT`
- 이전 행이 일치 · 새 행이 일치 안 함 → `DELETE`
- 이전 행이 일치 · 새 행이 일치 → `UPDATE`

설명:
- 이전 행이 필터를 만족하지만 새 행이 만족하지 않으면 → `DELETE` (일관성 유지)
- 이전 행이 필터를 만족하지 않지만 새 행이 만족하면 → `INSERT` (일관성 유지)
- 둘 다 일치하거나 둘 다 일치하지 않으면 → 일반 `UPDATE` 또는 복제 없음

#### 29.4.4. 파티션 테이블

`publish_via_partition_root` 매개변수가 사용할 행 필터를 결정함:

- `true`인 경우: 루트 파티션 테이블의 행 필터가 사용됨.
- `false`(기본값)인 경우: 각 파티션의 행 필터가 사용됨.

#### 29.4.5. 초기 데이터 동기화

- 행 필터 표현식을 만족하는 데이터만 구독자에 복사됨.
- 테이블이 서로 다른 `WHERE` 절을 가진 여러 발행에 있는 경우 모든 표현식을 만족하는 행이 복사됨(OR 논리).

경고: 초기 데이터 동기화는 `publish` 매개변수를 고려하지 않으므로 일반적으로 DML을 통해 복제되지 않는 일부 행이 복사될 수 있음.

참고: 15 이전 릴리스의 구독자는 초기 복사 중 행 필터를 사용하지 않음(전체 테이블 데이터가 복사됨).

#### 29.4.6. 다중 행 필터 결합

동일한 테이블이 서로 다른 행 필터를 가진 여러 발행에 나타나면 표현식이 OR로 결합됨. 다음 경우 다른 모든 필터는 중복됨:

1. 하나의 발행에 행 필터가 없는 경우
2. 하나의 발행이 `FOR ALL TABLES`를 사용하는 경우
3. 하나의 발행이 `FOR TABLES IN SCHEMA`를 사용하고 해당 테이블을 포함하는 경우

#### 29.4.7. 예제

##### 설정: 테이블 생성

```sql
CREATE TABLE t1(a int, b int, c text, PRIMARY KEY(a,c));
CREATE TABLE t2(d int, e int, f int, PRIMARY KEY(d));
CREATE TABLE t3(g int, h int, i int, PRIMARY KEY(g));
```

##### 행 필터가 있는 발행 생성

```sql
CREATE PUBLICATION p1 FOR TABLE t1 WHERE (a > 5 AND c = 'NSW');
CREATE PUBLICATION p2 FOR TABLE t1, t2 WHERE (e = 99);
CREATE PUBLICATION p3 FOR TABLE t2 WHERE (d = 10), t3 WHERE (g = 10);
```

##### 행 필터 보기

```sql
\dRp+   -- 행 필터가 있는 발행 세부 정보 표시
\d t1   -- 발행 및 필터 세부 정보가 있는 테이블 표시
```

##### 구독자 설정

```sql
CREATE TABLE t1(a int, b int, c text, PRIMARY KEY(a,c));
CREATE SUBSCRIPTION s1
  CONNECTION 'host=localhost dbname=test_pub application_name=s1'
  PUBLICATION p1;
```

##### 예제 1: 행 필터가 있는 INSERT

```sql
-- 발행자
INSERT INTO t1 VALUES
  (2, 102, 'NSW'),   -- 일치하지 않음 (a > 5)
  (6, 106, 'NSW'),   -- 필터 일치
  (9, 109, 'NSW');   -- 필터 일치

-- 구독자는 일치하는 행만 봄
SELECT * FROM t1;
-- a | b   | c
-- 6 | 106 | NSW
-- 9 | 109 | NSW
```

##### 예제 2: 두 행이 모두 일치하는 UPDATE

```sql
-- 이전 행과 새 행 모두 필터를 만족 -> 일반 UPDATE
UPDATE t1 SET b = 999 WHERE a = 6;

-- 구독자는 변경 사항을 반영
SELECT * FROM t1;
-- a | b   | c
-- 6 | 999 | NSW
-- 9 | 109 | NSW
```

##### 예제 3: INSERT로 변환된 UPDATE

```sql
-- 이전 행 (a=2, NSW)는 일치하지 않았고, 새 행 (a=555, NSW)는 일치
UPDATE t1 SET a = 555 WHERE a = 2;

-- 구독자는 INSERT를 봄
SELECT * FROM t1;
-- a   | b   | c
-- 6   | 999 | NSW
-- 9   | 109 | NSW
-- 555 | 102 | NSW  -- 삽입됨
```

##### 예제 4: DELETE로 변환된 UPDATE

```sql
-- 이전 행 (a=9, NSW)는 일치했고, 새 행 (a=9, VIC)는 일치하지 않음
UPDATE t1 SET c = 'VIC' WHERE a = 9;

-- 구독자는 행을 삭제
SELECT * FROM t1;
-- a   | b   | c
-- 6   | 999 | NSW
-- 555 | 102 | NSW  -- a=9 행이 삭제됨
```

##### 파티션 테이블 예제

```sql
-- 발행자: 파티션 테이블 생성
CREATE TABLE parent(a int PRIMARY KEY) PARTITION BY RANGE(a);
CREATE TABLE child PARTITION OF parent DEFAULT;

-- 구독자: 동일한 구조
CREATE TABLE parent(a int PRIMARY KEY) PARTITION BY RANGE(a);
CREATE TABLE child PARTITION OF parent DEFAULT;

-- publish_via_partition_root=true로 발행
CREATE PUBLICATION p4
  FOR TABLE parent WHERE (a < 5),
           child WHERE (a >= 5)
  WITH (publish_via_partition_root=true);

CREATE SUBSCRIPTION s4
  CONNECTION 'host=localhost dbname=test_pub application_name=s4'
  PUBLICATION p4;

-- 데이터 삽입
INSERT INTO parent VALUES (2), (4), (6);
INSERT INTO child VALUES (3), (5), (7);

-- 구독자는 5 미만인 행만 봄 (부모의 필터 사용)
SELECT * FROM parent ORDER BY a;
-- a
-- 2
-- 3
-- 4
```

`publish_via_partition_root=false`일 때는 자식 파티션의 필터가 대신 사용됨:

```sql
DROP PUBLICATION p4;
CREATE PUBLICATION p4
  FOR TABLE parent,
           child WHERE (a >= 5)
  WITH (publish_via_partition_root=false);

-- 구독자는 이제 5 이상인 행을 봄 (자식의 필터 사용)
SELECT * FROM child ORDER BY a;
-- a
-- 5
-- 6
-- 7
```

---

### 29.5. 컬럼 목록 (Column Lists)

각 발행은 선택적으로 각 테이블의 어떤 컬럼이 구독자에 복제되는지 지정 가능. 구독자 측의 테이블은 발행된 모든 컬럼을 최소한 포함해야 함. 컬럼 목록이 지정되지 않으면 발행자의 모든 컬럼이 복제됨.

#### 주요 사항

##### 컬럼 목록 동작
- 선택적 지정: 발행은 어떤 컬럼이 복제되는지 정의 가능.
- 새 컬럼 자동 복제: 컬럼 목록이 지정되지 않으면 나중에 테이블에 추가된 모든 컬럼이 자동으로 복제됨.
- 동등하지 않음: 모든 컬럼을 나열하는 컬럼 목록을 갖는 것은 컬럼 목록이 전혀 없는 것과 동일하지 않음.
- 컬럼 순서: 목록의 컬럼 순서는 보존되지 않음.
- 단순 참조만: 컬럼 목록은 단순 컬럼 참조만 포함 가능.

##### 보안 고려사항

경고: 보안을 위해 컬럼 목록에 의존 금지. 악의적인 구독자는 특별히 발행되지 않은 컬럼에서 데이터를 얻을 수 있음. 보안이 중요한 경우 발행자 측에서 보호를 적용 필요.

##### 생성된 컬럼
생성된 컬럼은 컬럼 목록에 지정 가능하며, `publish_generated_columns` 발행 매개변수와 관계없이 발행 가능(29.6절 참고).

##### 스키마 수준 발행
발행이 `FOR TABLES IN SCHEMA`를 발행할 때 컬럼 목록 지정은 지원되지 않음.

##### 파티션 테이블
파티션 테이블의 경우 `publish_via_partition_root` 매개변수가 어떤 컬럼 목록이 사용되는지 결정함:
- `true`인 경우: 루트 파티션 테이블의 컬럼 목록이 사용됨.
- `false`(기본값)인 경우: 각 파티션의 컬럼 목록이 사용됨.

##### UPDATE/DELETE 작업
발행이 `UPDATE` 또는 `DELETE` 작업을 발행하는 경우 모든 컬럼 목록은 테이블의 복제 식별자 컬럼(`REPLICA IDENTITY`로 정의됨)을 포함해야 함. `INSERT` 전용 발행의 경우 복제 식별자 컬럼 생략 가능.

##### TRUNCATE 명령
컬럼 목록은 `TRUNCATE` 명령에 영향을 미치지 않음.

##### 초기 데이터 동기화
- 발행된 컬럼만 초기 동기화 중에 복사됨.
- 예외: 구독자가 15 이전 릴리스인 경우 모든 컬럼이 복사됨(컬럼 목록 무시).
- 생성된 컬럼 예외: 구독자가 18 이전 릴리스인 경우 발행자에 정의되어 있어도 생성된 컬럼이 복사되지 않음.

##### 경고: 다중 발행의 컬럼 목록 결합

동일한 테이블에 서로 다른 컬럼 목록이 있는 여러 발행을 포함하는 구독은 지원되지 않음.

문제점:
- `CREATE SUBSCRIPTION`은 처음에 이러한 구독 생성을 방지함.
- 구독 생성 후 컬럼 목록을 변경하면 이 상황에 빠질 수 있음.
- 구독된 발행의 컬럼 목록을 변경하면 구독자 측에서 오류가 발생할 수 있음.

해결책:
1. 발행 측의 컬럼 목록이 모두 일치하도록 조정
2. 구독을 다시 생성
3. `ALTER SUBSCRIPTION ... DROP PUBLICATION`을 사용하여 문제가 되는 발행을 제거하고 다시 추가

#### 29.5.1. 예제

##### 단계 1: 발행자에 테이블 생성

```sql
/* pub # */ CREATE TABLE t1(id int, a text, b text, c text, d text, e text, PRIMARY KEY(id));
```

##### 단계 2: 컬럼 목록이 있는 발행 생성

```sql
/* pub # */ CREATE PUBLICATION p1 FOR TABLE t1 (id, b, a, d);
```

참고: 목록의 컬럼 순서는 중요하지 않음(id, b, a, d는 id, a, b, d와 동등).

##### 단계 3: 발행 컬럼 목록 보기

`psql` 사용:

```sql
/* pub # */ \dRp+
```
출력:

```
                                     Publication p1
Owner   | All tables | Inserts | Updates | Deletes | Truncates | Generated columns | Via root
--------|------------|---------|---------|---------|-----------|-------------------|----------
postgres| f          | t       | t       | t       | t         | none              | f

Tables:
    "public.t1" (id, a, b, d)
```

##### 단계 4: 테이블별 컬럼 목록 보기

```sql
/* pub # */ \d t1
```

출력:

```
             Table "public.t1"
Column |  Type   | Collation | Nullable | Default
-------|---------|-----------|----------|----------
id     | integer |           | not null |
a      | text    |           |          |
b      | text    |           |          |
c      | text    |           |          |
d      | text    |           |          |
e      | text    |           |          |

Indexes:
    "t1_pkey" PRIMARY KEY, btree (id)

Publications:
    "p1" (id, a, b, d)
```

##### 단계 5: 구독자 테이블 및 구독 생성

```sql
/* sub # */ CREATE TABLE t1(id int, b text, a text, d text, PRIMARY KEY(id));
/* sub # */ CREATE SUBSCRIPTION s1
/* sub - */ CONNECTION 'host=localhost dbname=test_pub application_name=s1'
/* sub - */ PUBLICATION p1;
```

> 참고: 구독자 테이블은 발행된 컬럼만 필요함

##### 단계 6: 발행자에 데이터 삽입

```sql
/* pub # */ INSERT INTO t1 VALUES(1, 'a-1', 'b-1', 'c-1', 'd-1', 'e-1');
/* pub # */ INSERT INTO t1 VALUES(2, 'a-2', 'b-2', 'c-2', 'd-2', 'e-2');
/* pub # */ INSERT INTO t1 VALUES(3, 'a-3', 'b-3', 'c-3', 'd-3', 'e-3');

/* pub # */ SELECT * FROM t1 ORDER BY id;
```

출력:

```
id |  a  |  b  |  c  |  d  |  e
---|-----|-----|-----|-----|-----
 1 | a-1 | b-1 | c-1 | d-1 | e-1
 2 | a-2 | b-2 | c-2 | d-2 | e-2
 3 | a-3 | b-3 | c-3 | d-3 | e-3
(3 rows)
```

##### 단계 7: 구독자에서 복제 확인

```sql
/* sub # */ SELECT * FROM t1 ORDER BY id;
```

출력 (발행된 컬럼만 복제됨):

```
id |  b  |  a  |  d
---|-----|-----|-----
 1 | b-1 | a-1 | d-1
 2 | b-2 | a-2 | d-2
 3 | b-3 | a-3 | d-3
(3 rows)
```

> 참고: 발행자의 컬럼 `c`와 `e`는 컬럼 목록에 없어서 복제되지 않음

---

### 29.6. 생성된 컬럼 복제

구독자 테이블의 생성된 컬럼은 일반적으로 발행자 테이블과 동일하게 정의됨. 두 테이블 모두 `GENERATED` 컬럼이 있는 경우 구독자 테이블의 생성된 컬럼 값이 항상 사용되며, 구독자의 정의에 따라 계산됨.

#### 주요 동작

PostgreSQL 18.0 이전: 논리적 복제는 `GENERATED` 컬럼을 전혀 발행하지 않음.

PostgreSQL 18.0 이상: 사용자는 저장된 생성 컬럼을 일반 컬럼처럼 발행하도록 선택 가능.

#### 생성된 컬럼 발행 방법

두 가지 방법 사용 가능:

1. `publish_generated_columns` 매개변수를 `stored`로 설정
   - PostgreSQL에 현재 및 미래의 저장된 생성 컬럼을 발행하도록 지시함
   - 발행의 모든 테이블에 적용됨

2. 테이블 컬럼 목록 지정
   - 발행할 저장된 생성 컬럼을 명시적으로 지정함
   - `publish_generated_columns` 매개변수보다 우선함

#### 예제

발행자 설정:

```sql
/* pub # */ CREATE TABLE tab_gen_to_gen (a int, b int GENERATED ALWAYS AS (a + 1) STORED);
/* pub # */ INSERT INTO tab_gen_to_gen VALUES (1),(2),(3);
/* pub # */ CREATE PUBLICATION pub1 FOR TABLE tab_gen_to_gen;
/* pub # */ SELECT * FROM tab_gen_to_gen;
 a | b
---+---
 1 | 2
 2 | 3
 3 | 4
```

구독자 설정:

```sql
/* sub # */ CREATE TABLE tab_gen_to_gen (a int, b int GENERATED ALWAYS AS (a * 100) STORED);
/* sub # */ CREATE SUBSCRIPTION sub1 CONNECTION 'dbname=test_pub' PUBLICATION pub1;
/* sub # */ SELECT * from tab_gen_to_gen;
 a | b
---+----
 1 | 100
 2 | 200
 3 | 300
```

구독자는 발행자의 계산(a + 1)이 아닌 자체 생성 컬럼 계산(a * 100)을 사용함.

#### 복제 결과 요약

- 생성 컬럼 미발행 시
  - 발행자 GENERATED · 구독자 GENERATED → 발행자 컬럼이 복제되지 않음 · 구독자의 생성 값 사용
  - 발행자 GENERATED · 구독자 일반 컬럼 → 발행자 컬럼이 복제되지 않음 · 구독자의 기본값 사용
  - 발행자 GENERATED · 구독자 컬럼 없음 → 발행자 컬럼이 복제되지 않음 · 아무 일도 발생하지 않음
- 생성 컬럼 발행 시
  - 발행자 GENERATED · 구독자 GENERATED → 오류(지원되지 않음)
  - 발행자 GENERATED · 구독자 일반 컬럼 → 발행자 컬럼 값이 구독자에 복제됨
  - 발행자 GENERATED · 구독자 컬럼 없음 → 오류(구독자에 컬럼 없음)

#### 중요 경고

1. 단일 구독 내에서 동일한 테이블에 대해 서로 다른 컬럼 목록을 가진 여러 발행을 지원하지 않음
2. 충돌 시나리오: 하나의 발행은 생성된 컬럼을 발행하고 다른 발행은 동일한 테이블에 대해 발행하지 않는 경우
3. 18 이전 구독자: 발행자에 정의되어 있어도 초기 테이블 동기화에서 생성된 컬럼을 복사하지 않음

#### 사용 사례

이 기능은 출력 플러그인을 통해 비PostgreSQL 데이터베이스로 데이터를 복제할 때 유용함 · 특히 대상 데이터베이스가 생성된 컬럼을 지원하지 않는 경우에 활용 가능.

---

### 29.7. 충돌 (Conflicts)

논리적 복제는 일반 DML 작업과 유사하게 동작함. 구독자 노드에서 로컬로 변경된 데이터도 업데이트 대상이 됨. 수신된 데이터가 제약 조건을 위반하면 복제가 중지됨(충돌). `UPDATE` 또는 `DELETE` 작업에서 대상 행이 없는 경우도 충돌로 간주되지만 오류는 발생하지 않으며 해당 작업은 단순히 건너뜀.

#### 충돌 유형

##### `insert_exists`
- `NOT DEFERRABLE` 고유 제약 조건을 위반하는 행 삽입
- 원본 및 커밋 타임스탬프 세부 정보를 기록하려면 구독자에서 `track_commit_timestamp` 활성화 필요
- 결과: 수동으로 해결될 때까지 오류 발생

##### `update_origin_differs`
- 이전에 다른 원본에서 수정한 행 업데이트
- 구독자에서 `track_commit_timestamp`가 활성화된 경우에만 감지됨
- 결과: 로컬 행 원본에 관계없이 업데이트가 항상 적용됨

##### `update_exists`
- 업데이트된 행 값이 `NOT DEFERRABLE` 고유 제약 조건을 위반
- 충돌 세부 정보를 기록하려면 `track_commit_timestamp` 활성화 필요
- 파티션 테이블의 경우: 행이 새 파티션으로 이동하면 `insert_exists`가 발생할 수 있음
- 결과: 수동으로 해결될 때까지 오류 발생

##### `update_missing`
- 업데이트할 행을 찾을 수 없음
- 결과: 업데이트가 자동으로 건너뜀

##### `delete_origin_differs`
- 이전에 다른 원본에서 수정한 행 삭제
- `track_commit_timestamp`가 활성화된 경우에만 감지됨
- 결과: 로컬 행 원본에 관계없이 삭제가 항상 적용됨

##### `delete_missing`
- 삭제할 행을 찾을 수 없음
- 결과: 삭제가 자동으로 건너뜀

##### `multiple_unique_conflicts`
- 삽입 또는 업데이트하는 행이 여러 `NOT DEFERRABLE` 고유 제약 조건을 위반
- 충돌 세부 정보를 위해 `track_commit_timestamp` 활성화 필요
- 결과: 수동으로 해결될 때까지 오류 발생

#### 로그 형식

```
LOG:  conflict detected on relation "_schemaname_._tablename_": conflict=_conflict_type_
DETAIL:  _detailed_explanation_.
{_detail_values_ [; ... ]}.
```

##### 세부 값 형식

```
Key (column_name [, ...])=(column_value [, ...])
existing local row [(column_name [, ...])=](column_value [, ...])
remote row [(column_name [, ...])=](column_value [, ...])
replica identity {(column_name [, ...])=(column_value [, ...]) | full [(column_name [, ...])=](column_value [, ...])}
```

#### 로그 정보 세부사항

LOG 섹션:
- `_schemaname_._tablename_`: 관련된 로컬 릴레이션 식별
- `_conflict_type_`: 충돌 유형 (예: `insert_exists`, `update_exists`)

DETAIL 섹션:
- `_detailed_explanation_`: 가능한 경우 원본, 트랜잭션 ID, 커밋 타임스탬프 포함
- Key 섹션: 고유 제약 조건을 위반한 키 값 (`insert_exists`, `update_exists`, `multiple_unique_conflicts`의 경우)
- existing local row: 원본이 다르거나 키가 충돌할 때 로컬 행 세부 정보
- remote row: 충돌을 일으킨 원격 삽입/업데이트의 새 행 (업데이트에서 변경되지 않은/토스트된 컬럼의 경우 null 값)
- replica identity: 기존 행을 검색하는 데 사용된 복제 식별자 키 값; `REPLICA IDENTITY FULL`로 표시된 경우 전체 행 포함
- `_column_name_`: 사용자가 모든 테이블 컬럼에 접근할 권한이 없는 경우에만 기록됨
- `_column_value_`: 큰 값은 64바이트로 잘림

#### 예제 오류 로그

```
ERROR:  conflict detected on relation "public.test": conflict=insert_exists
DETAIL:  Key already exists in unique index "t_pkey", which was modified locally in transaction 740 at 2024-06-26 10:47:04.727375+08.
Key (c)=(1); existing local row (1, 'local'); remote row (1, 'remote').
CONTEXT:  processing remote data for replication origin "pg_16395" during "INSERT" for replication target relation "public.test" in transaction 725 finished at 0/14C0378
```

#### 권한 및 행 수준 보안

논리적 복제 작업은 구독 소유자 역할의 권한으로 실행됨. 다음으로 인해 충돌이 발생할 수 있음:
- 대상 테이블에 대한 권한 실패
- `INSERT`, `UPDATE`, `DELETE` 또는 `TRUNCATE` 작업을 거부하는 행 수준 보안(RLS) 정책

> 참고: RLS 제한은 향후 PostgreSQL 버전에서 해제될 수 있음

#### 충돌 해결

##### 수동 해결 단계

1. 충돌 식별 - 다음을 포함하는 오류 메시지에서:
   - LSN (Log Sequence Number): 예: `0/14C0378`
   - 복제 원본 이름: 예: `pg_16395`

2. 해결 방법 선택:

   옵션 A: ALTER SUBSCRIPTION SKIP 사용
   ```sql
   ALTER SUBSCRIPTION subscription_name SKIP (LSN '0/14C0378');
   ```

   옵션 B: pg_replication_origin_advance() 사용
   ```sql
   -- 먼저 구독 비활성화
   ALTER SUBSCRIPTION subscription_name DISABLE;

   -- 또는 구독 생성 시 disable_on_error 옵션 사용

   -- 그런 다음 다음 LSN으로 전진
   SELECT pg_replication_origin_advance('pg_16395', '0/14C0379');

   -- 현재 위치 확인
   SELECT * FROM pg_replication_origin_status;

   -- 구독 다시 활성화
   ALTER SUBSCRIPTION subscription_name ENABLE;
   ```

3. 근본 문제 해결:
   - 들어오는 변경과 충돌하지 않도록 구독자의 데이터 변경
   - 대상 테이블의 권한 수정
   - 또는 충돌하는 트랜잭션 건너뛰기

##### 중요 고려사항

- 트랜잭션 건너뛰기는 충돌하는 작업뿐만 아니라 해당 트랜잭션의 모든 변경 사항을 건너뜀 → 구독자 데이터 불일치 발생 가능
- 구독자의 `track_commit_timestamp` 설정을 활성화하면 원본 및 커밋 타임스탬프 세부 정보가 로깅됨 → 로컬 변경과 원격 변경 중 어느 것을 유지할지 판단하는 데 도움
- 병렬 스트리밍 모드의 경우 완료 LSN이 기록되지 않을 수 있음 → 완료 LSN을 얻으려면 스트리밍 모드를 `on` 또는 `off`로 변경 필요

#### 관련 뷰 및 함수

- `pg_stat_subscription_stats`: 충돌 통계 보기
- `pg_replication_origin_status`: 복제 원본의 현재 위치 보기
- `pg_replication_origin_advance()`: 복제 원본 위치 전진
- `ALTER SUBSCRIPTION ... SKIP`: 충돌하는 트랜잭션 건너뛰기
- `ALTER SUBSCRIPTION ... DISABLE`: 구독 비활성화

---

### 29.8. 제한사항 (Restrictions)

#### 제한사항 전체 목록

##### 1. 데이터베이스 스키마 및 DDL 명령이 복제되지 않음
- 데이터베이스 스키마와 DDL(Data Definition Language) 명령은 발행자와 구독자 간에 복제되지 않음
- 초기 스키마는 다음을 사용하여 수동으로 복사 필요:
  ```bash
  pg_dump --schema-only
  ```
- 후속 스키마 변경은 수동으로 동기화 유지 필요
- 스키마는 양쪽에서 완전히 동일할 필요는 없음
- 발행자에서 스키마가 변경되고 복제된 데이터가 구독자에 도착했지만 테이블 스키마에 맞지 않으면 스키마가 업데이트될 때까지 복제에 오류 발생
- 모범 사례: 간헐적인 오류를 방지하기 위해 추가적인 스키마 변경을 구독자에 먼저 적용

##### 2. 시퀀스 데이터가 복제되지 않음
- 시퀀스 데이터 자체는 복제되지 않음
- 시리얼 또는 ID 컬럼 데이터는 테이블 데이터의 일부로 복제됨
- 구독자의 시퀀스 객체는 여전히 시작 값을 표시함
- 읽기 전용 구독자의 경우: 일반적으로 문제되지 않음
- 전환/장애 조치 시나리오의 경우: 다음을 사용하여 시퀀스를 최신 값으로 수동 업데이트 필요:
  - 발행자에서 복사한 데이터 (예: `pg_dump`)
  - 실제 테이블에서 결정된 값

##### 3. TRUNCATE 명령 복제 제한
- `TRUNCATE` 명령은 복제가 지원됨
- 외래 키로 연결된 테이블에 주의 필요
- 구독자는 발행자와 동일한 테이블 그룹을 잘라냄(캐스케이딩 테이블 포함) · 구독에 없는 테이블은 제외
- 영향을 받는 모든 테이블이 동일한 구독의 일부인 경우 올바르게 작동함
- 구독자에서 잘린 테이블이 동일한(또는 어떤) 구독에 없는 테이블에 외래 키 링크가 있는 경우 실패함

##### 4. 대형 객체가 복제되지 않음
- 대형 객체(PostgreSQL 문서 33장)는 복제되지 않음
- 해결 방법: 대신 일반 테이블에 데이터 저장

##### 5. 테이블만 복제 지원
- 복제는 파티션 테이블을 포함한 테이블에만 지원됨
- 다른 릴레이션 유형은 오류 발생:
  - 뷰
  - 구체화된 뷰
  - 외부 테이블

##### 6. 파티션 테이블 복제 제약
- 복제는 기본적으로 발행자의 리프 파티션에서 시작됨
- 파티션은 유효한 대상 테이블로 구독자에 존재 필요
- 대상 파티션은 다음일 수 있음:
  - 리프 파티션 자체
  - 추가로 하위 파티션됨
  - 독립적인 테이블
- 대체 접근 방식: `CREATE PUBLICATION`에서 `publish_via_partition_root` 매개변수를 사용하여 개별 리프 파티션 대신 파티션된 루트 테이블의 ID와 스키마를 사용하여 복제함

##### 7. REPLICA IDENTITY FULL 제한
- 발행된 테이블에서 `REPLICA IDENTITY FULL`을 사용할 때:
  - 기본 B-tree 또는 Hash 연산자 클래스가 없는 데이터 유형(예: `point`, `box`)의 속성을 포함하는 테이블에는 `UPDATE` 및 `DELETE` 작업을 구독자에 적용할 수 없음
  - 해결 방법: 테이블에 기본 키 또는 복제 식별자가 정의되어 있는지 확인 필요

#### 요약

이러한 제한을 고려할 때 PostgreSQL의 논리적 복제는 다음 시나리오에 가장 적합함:
- 테이블 데이터만 복제
- 읽기 전용 복제본 또는 수동 준비가 있는 예정된 전환
- 수동으로 동기화된 스키마
- 복잡한 기하학적 또는 사용자 정의 데이터 유형이 없는 테이블(기본 키/복제 식별자 사용 시 제외)

---

### 29.9. 아키텍처 (Architecture)

논리적 복제는 물리적 스트리밍 복제와 유사한 아키텍처로 구현됨. 두 가지 주요 프로세스로 구성됨:

1. walsender 프로세스 - WAL의 논리적 디코딩을 시작하고 표준 논리적 디코딩 출력 플러그인(`pgoutput`)을 로드함
2. apply 프로세스 - 데이터를 로컬 테이블에 매핑하고 올바른 트랜잭션 순서로 수신된 대로 개별 변경 사항을 적용함

#### 주요 프로세스 흐름

- `pgoutput` 플러그인은 WAL에서 읽은 변경 사항을 논리적 복제 프로토콜로 변환함(54.5절 참고)
- 데이터는 발행 사양에 따라 필터링됨
- 데이터는 스트리밍 복제 프로토콜을 사용하여 apply 워커로 지속적으로 전송됨

#### 세션 복제 역할

구독자 데이터베이스의 apply 프로세스는 항상 `session_replication_role`이 `replica`로 설정된 상태로 실행됨. 따라서:

- 기본적으로 트리거와 규칙이 구독자에서 실행되지 않음
- 필요한 경우 특정 테이블에서 트리거와 규칙을 활성화 가능:
  ```sql
  ALTER TABLE table_name ENABLE TRIGGER trigger_name;
  ALTER TABLE table_name ENABLE RULE rule_name;
  ```

#### 트리거 동작

- 논리적 복제 apply 프로세스는 행 트리거만 실행하며 문 트리거는 실행하지 않음
- 초기 테이블 동기화는 `COPY` 명령처럼 구현되며, `INSERT`에 대해 행 트리거와 문 트리거를 모두 실행함

#### 29.9.1. 초기 스냅샷

##### 동기화 프로세스

1. 초기 데이터 스냅샷: 기존 테이블 데이터가 스냅샷되고 특별한 apply 프로세스의 병렬 인스턴스에서 복사됨
2. 테이블 동기화 워커: 동기화할 각 테이블에 대해 전용 워커가 생성됨
3. 각 워커:
   - 자체 복제 슬롯을 생성함
   - 기존 데이터를 복사함
   - 복사가 완료되면 데이터가 다른 백엔드에 표시됨

##### 동기화 단계

초기 복사가 완료되면 워커는 동기화 모드로 진입함:
- 테이블이 메인 apply 프로세스와 동기화된 상태가 되도록 함
- 표준 논리적 복제를 사용하여 초기 데이터 복사 중 발생한 모든 변경 사항을 스트리밍함
- 발행자에서 발생한 순서대로 변경 사항을 적용하고 커밋함
- 완료되면 메인 apply 프로세스로 제어를 반환하고 정상적으로 복제를 계속함

#### 중요 참고 사항

##### 발행 매개변수

발행의 `publish` 매개변수는 어떤 DML 작업을 복제할지에만 영향을 미침. 초기 데이터 동기화 시 기존 테이블 데이터를 복사할 때는 이 매개변수를 고려하지 않음.

##### 오류 처리

테이블 동기화 워커가 복사 중 실패하면:
- apply 워커가 실패를 감지함
- 테이블 동기화 워커가 동기화 프로세스를 계속하기 위해 다시 생성됨
- 이 동작은 일시적인 오류가 복제 설정을 영구적으로 방해하지 않도록 함
- 관련 구성: `wal_retrieve_retry_interval`

---

### 29.10. 모니터링 (Monitoring)

논리적 복제 모니터링은 물리적 스트리밍 복제와 유사한 아키텍처를 공유하므로 모니터링 방식도 비슷함. 발행 노드 모니터링 정보는 26.2.5.2절 모니터링 참고.

#### 구독 모니터링

구독에 대한 모니터링 정보는 `pg_stat_subscription` 뷰를 통해 확인 가능 · 각 구독 워커에 대해 하나의 행이 포함됨.

#### 주요 사항

- 워커 상태: 구독은 상태에 따라 0개 이상의 활성 구독 워커를 가질 수 있음
- 정상 작동: 일반적으로 활성화된 구독에 대해 단일 apply 프로세스가 실행됨
- 비활성화/충돌된 구독: 뷰에 0개의 행이 있음
- 초기 동기화: 초기 데이터 동기화가 진행 중일 때 동기화 중인 테이블에 대한 추가 워커가 나타남
- 병렬 스트리밍: `streaming` 트랜잭션이 병렬로 적용되는 경우 추가 병렬 apply 워커가 있을 수 있음

---

### 29.11. 보안 (Security)

#### 발행자 보안 요구 사항

##### 복제 역할 속성
- `REPLICATION` 속성 필요(또는 슈퍼유저)
- `LOGIN` 속성 필요
- `pg_hba.conf`에서 접근 구성 필요

##### 행 보안 정책
- 역할에 `SUPERUSER` 및 `BYPASSRLS`가 없으면 발행자 행 보안 정책이 실행될 수 있음
- 행 보안 정책 실행을 방지하려면 연결 문자열에 다음을 포함:
  ```
  options=-crow_security=off
  ```
- 활성화된 경우 테이블 소유자가 행 보안 정책을 추가하면 안전하지 않은 정책을 실행하지 않고 복제가 중단됨

##### 권한 요구 사항
- 초기 테이블 데이터를 복사하려면 발행된 테이블에 대한 `SELECT` 권한 필요(또는 슈퍼유저)
- 발행을 생성하려면 데이터베이스에 `CREATE` 권한 필요

#### 발행 권한

##### 발행에 테이블 추가
- 일반 테이블: 사용자가 테이블에 대한 소유권 필요
- 스키마의 모든 테이블: 사용자가 슈퍼유저여야 함
- 모든 테이블 또는 스키마의 자동 발행: 사용자가 슈퍼유저여야 함

##### 발행 접근 제어
- 현재 발행에 대한 권한이 없음
- 연결할 수 있는 모든 구독은 모든 발행에 접근할 수 있음
- 행 필터, 컬럼 목록 또는 선택적 테이블 포함을 통해 정보를 숨기더라도 동일한 데이터베이스의 다른 발행이 동일한 정보를 노출할 수 있음에 유의 필요
- 발행 권한은 향후 PostgreSQL 버전에서 추가될 수 있음

#### 구독 보안 요구 사항

##### 생성 권한
- 사용자가 `pg_create_subscription` 역할의 권한 보유 필요
- 데이터베이스에 `CREATE` 권한 필요

##### Apply 프로세스 실행

###### 기본 동작 (run_as_owner = false)
- 세션 수준: 구독 소유자의 권한으로 실행
- 작업별: INSERT, UPDATE, DELETE 또는 TRUNCATE 작업에 대해 테이블 소유자로 역할 전환
- 요구 사항: 구독 소유자는 복제된 테이블을 소유한 각 역할로 `SET ROLE` 가능해야 함

###### run_as_owner = true인 경우
- 역할 전환이 발생하지 않음
- 모든 작업이 구독 소유자의 권한으로 수행됨
- 권한 요구 사항 감소: 구독 소유자는 대상 테이블에 대한 `SELECT`, `INSERT`, `UPDATE` 및 `DELETE` 권한만 필요
- 보안 위험: 테이블 소유자가 구독 소유자의 권한으로 임의 코드를 실행할 수 있음(예: 트리거를 통해)
- 권장 사항: 데이터베이스 내 사용자 보안이 중요하지 않은 경우가 아니면 사용을 피할 것

#### 권한 재확인

##### 발행자
- 권한은 복제 연결 시작 시 한 번 확인됨
- 각 변경 레코드를 읽을 때 재확인되지 않음

##### 구독자
- 구독 소유자의 권한은 적용될 때 각 트랜잭션마다 재확인됨
- 트랜잭션 적용 중 구독 소유권이 변경되면 현재 트랜잭션은 이전 소유자의 권한으로 계속됨

#### 주요 보안 고려사항

1. 행 보안 정책: 모든 테이블 소유자를 신뢰하지 않는 경우 `crow_security=off` 사용 권장
2. 발행 가시성: 발행은 인증된 구독자에게 전역적으로 접근 가능함
3. 역할 전환 위험: `run_as_owner = true` 옵션은 트리거를 통해 보안 취약점을 도입할 수 있음
4. 권한 모니터링: 각 트랜잭션에 대한 구독자 권한을 정기적으로 재확인 필요

---

### 29.12. 구성 설정

논리적 복제에는 여러 구성 옵션 설정 필요. 각 옵션은 발행자 또는 구독자 중 한쪽에만 해당됨.

#### 29.12.1. 발행자

다음 설정은 발행자 측에서 구성 필요:

##### 필수 설정

1. `wal_level`
   - `logical`로 설정 필요

2. `max_replication_slots`
   - 예상 구독 수 이상으로 설정 필요
   - 테이블 동기화용 여유분 포함 필요

3. `max_wal_senders`
   - 최소한 `max_replication_slots` 이상으로 설정 필요
   - 동시에 연결된 물리적 복제본 수를 추가로 더함

##### 영향을 받는 설정

- `idle_replication_slot_timeout` - 논리적 복제 슬롯에 영향
- `wal_sender_timeout` - 논리적 복제 walsender에 영향

#### 29.12.2. 구독자

다음 설정은 구독자 측에서 구성 필요:

##### 필수 설정

1. `max_active_replication_origins`
   - 추가할 구독 수 이상으로 설정 필요
   - 테이블 동기화용 여유분 포함 필요

2. `max_logical_replication_workers`
   - 최소한 구독 수(리더 apply 워커용) 이상으로 설정 필요
   - 테이블 동기화 워커 및 병렬 apply 워커용 여유분을 추가로 더함

3. `max_worker_processes`
   - 복제 워커를 수용할 수 있도록 조정 필요할 수 있음
   - 최소: `max_logical_replication_workers + 1`
   - 참고: 확장 및 병렬 쿼리도 워커 슬롯을 소비함

4. `max_sync_workers_per_subscription`
   - 구독 초기화 중 초기 데이터 복사의 병렬 처리를 제어함
   - 새 테이블 추가 시 병렬 처리를 제어함

5. `max_parallel_apply_workers_per_subscription`
   - 진행 중인 트랜잭션 스트리밍의 병렬 처리를 제어함
   - 구독 매개변수 `streaming = parallel`과 함께 사용됨

##### 영향을 받는 설정

- `wal_receiver_timeout` - 논리적 복제 워커에 영향
- `wal_receiver_status_interval` - 논리적 복제 워커에 영향
- `wal_retrieve_retry_interval` - 논리적 복제 워커에 영향

---

### 29.13. 업그레이드

논리적 복제 클러스터 업그레이드는 클러스터의 모든 구성원이 버전 17.0 이상인 경우에만 지원됨.

#### 29.13.1. 발행자 업그레이드 준비

##### 주요 사항
- `pg_upgrade`는 논리적 슬롯 마이그레이션을 시도하므로 수동으로 재생성할 필요 없음
- 버전 17.0 이상의 클러스터에만 지원됨
- 17.0 이전 클러스터의 슬롯은 자동으로 무시됨

##### 업그레이드 전 필수 조건

1. 구독자에서 구독 비활성화:
   ```sql
   ALTER SUBSCRIPTION ... DISABLE;
   ```

2. 새 클러스터의 구성 요구 사항:
   - `wal_level = logical`
   - `max_replication_slots >=` 이전 클러스터의 슬롯 수

3. 플러그인 요구 사항:
   - 슬롯에서 참조하는 출력 플러그인이 새 PostgreSQL 실행 파일 디렉토리에 설치되어 있어야 함

4. 복제 상태:
   - 이전 클러스터가 모든 트랜잭션과 논리적 디코딩 메시지를 구독자에 복제한 상태여야 함
   - 모든 슬롯이 사용 가능해야 함(`pg_replication_slots.conflicting = true`인 충돌 슬롯 없음)
   - 새 클러스터에 영구 논리적 슬롯이 없어야 함(`pg_replication_slots.temporary = false`인 슬롯 없음)

#### 29.13.2. 구독자 업그레이드 준비

##### 구독자 구성
업그레이드 전에 새 구독자에서 구독 관련 구성 설정 필요.

##### pg_upgrade가 마이그레이션하는 항목
- `pg_subscription_rel` 시스템 카탈로그의 구독 종속성
- 구독의 테이블 정보
- 구독의 복제 원본(이전 구독자가 중단한 위치에서 복제를 계속할 수 있음)

참고: 버전 17.0 이상의 클러스터에만 마이그레이션이 지원됨.

##### 필수 조건

1. 구독 테이블 상태가 다음 중 하나여야 함:
   - `i` (초기화) 또는 `r` (준비됨)
   - 확인: `pg_subscription_rel.srsubstate`

2. 각 구독에 대한 복제 원본 항목이 존재해야 함:
   - `pg_subscription` 및 `pg_replication_origin` 테이블 모두 확인

3. 구성 요구 사항:
   - `max_active_replication_origins >=` 이전 클러스터의 구독 수

#### 29.13.3. 논리적 복제 클러스터 업그레이드

##### 중요 참고 사항

- 쓰기 작업 허용됨: 구독자를 업그레이드하는 동안 발행자에서 쓰기 작업 수행 가능. 변경 사항은 구독자 업그레이드가 완료되면 복제됨.

- 제한 적용: 논리적 복제 제한이 클러스터 업그레이드에 적용됨(29.8 참고).

- 필수 조건 적용: 발행자와 구독자 업그레이드 필수 조건이 모두 적용됨.

> 경고: 논리적 복제 클러스터 업그레이드에는 다양한 노드에 걸쳐 여러 단계 필요. 모든 작업이 트랜잭션은 아님. 항상 25.3.2절에 설명된 대로 백업 수행 필요.

##### 29.13.3.1. 2노드 논리적 복제 클러스터 업그레이드 단계

설정: `node1`에 발행자, `node2`에 구독자, 구독 `sub1_node1_node2`

###### 전체 업그레이드 단계

1. node2에서 구독 비활성화:
   ```sql
   /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 DISABLE;
   ```

2. 발행자(node1) 중지:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data1 stop
   ```

3. 업그레이드된 인스턴스 초기화:
   ```bash
   (새 버전으로 data1_upgraded 초기화)
   ```

4. pg_upgrade로 발행자 업그레이드:
   ```bash
   pg_upgrade \
       --old-datadir "/opt/PostgreSQL/postgres/17/data1" \
       --new-datadir "/opt/PostgreSQL/postgres/18/data1_upgraded" \
       --old-bindir "/opt/PostgreSQL/postgres/17/bin" \
       --new-bindir "/opt/PostgreSQL/postgres/18/bin"
   ```

5. 업그레이드된 발행자 시작:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data1_upgraded start -l logfile
   ```

6. 구독자(node2) 중지:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data2 stop
   ```

7. 업그레이드된 구독자 인스턴스 초기화:
   ```bash
   (새 버전으로 data2_upgraded 초기화)
   ```

8. pg_upgrade로 구독자 업그레이드:
   ```bash
   pg_upgrade \
       --old-datadir "/opt/PostgreSQL/postgres/17/data2" \
       --new-datadir "/opt/PostgreSQL/postgres/18/data2_upgraded" \
       --old-bindir "/opt/PostgreSQL/postgres/17/bin" \
       --new-bindir "/opt/PostgreSQL/postgres/18/bin"
   ```

9. 업그레이드된 구독자 시작:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data2_upgraded start -l logfile
   ```

10. node2에 누락된 테이블 생성 (다운타임 중 node1에서 생성된 것):
    ```sql
    /* node2 # */ CREATE TABLE distributors (did integer PRIMARY KEY, name varchar(40));
    ```

11. node2에서 구독 활성화:
    ```sql
    /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 ENABLE;
    ```

12. 구독 발행 새로고침:
    ```sql
    /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 REFRESH PUBLICATION;
    ```

참고: 발행자를 먼저 업그레이드하거나 구독자를 먼저 업그레이드할 수 있음. 역순으로도 유사한 단계 적용됨.

##### 29.13.3.2. 캐스케이드 논리적 복제 클러스터 업그레이드 단계

설정: `node1` -> `node2` -> `node3` (캐스케이드 체인)
- `node2`는 `node1`을 구독 (구독: `sub1_node1_node2`)
- `node3`는 `node2`를 구독 (구독: `sub1_node2_node3`)

###### 업그레이드 순서 (20단계)

1. node2의 node1 구독 비활성화:
   ```sql
   /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 DISABLE;
   ```

2. node1 중지:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data1 stop
   ```

3. `data1_upgraded` 초기화

4. pg_upgrade로 node1 업그레이드

5. 업그레이드된 node1 시작:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data1_upgraded start -l logfile
   ```

6. node3의 node2 구독 비활성화:
   ```sql
   /* node3 # */ ALTER SUBSCRIPTION sub1_node2_node3 DISABLE;
   ```

7. node2 중지:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data2 stop
   ```

8. `data2_upgraded` 초기화

9. pg_upgrade로 node2 업그레이드

10. 업그레이드된 node2 시작:
    ```bash
    pg_ctl -D /opt/PostgreSQL/data2_upgraded start -l logfile
    ```

11. node2에 누락된 테이블 생성:
    ```sql
    /* node2 # */ CREATE TABLE distributors (did integer PRIMARY KEY, name varchar(40));
    ```

12. node2 구독 활성화:
    ```sql
    /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 ENABLE;
    ```

13. node2 구독 새로고침:
    ```sql
    /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 REFRESH PUBLICATION;
    ```

14. node3 중지:
    ```bash
    pg_ctl -D /opt/PostgreSQL/data3 stop
    ```

15. `data3_upgraded` 초기화

16. pg_upgrade로 node3 업그레이드

17. 업그레이드된 node3 시작:
    ```bash
    pg_ctl -D /opt/PostgreSQL/data3_upgraded start -l logfile
    ```

18. node3에 누락된 테이블 생성:
    ```sql
    /* node3 # */ CREATE TABLE distributors (did integer PRIMARY KEY, name varchar(40));
    ```

19. node3 구독 활성화:
    ```sql
    /* node3 # */ ALTER SUBSCRIPTION sub1_node2_node3 ENABLE;
    ```

20. node3 구독 새로고침:
    ```sql
    /* node3 # */ ALTER SUBSCRIPTION sub1_node2_node3 REFRESH PUBLICATION;
    ```

##### 29.13.3.3. 2노드 순환 논리적 복제 클러스터 업그레이드 단계

설정: 양방향 복제 `node1` <-> `node2`
- `node1`은 구독 `sub1_node2_node1`을 가짐 (node2에서)
- `node2`는 구독 `sub1_node1_node2`를 가짐 (node1에서)

###### 업그레이드 순서 (16단계)

1. node2의 node1 구독 비활성화:
   ```sql
   /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 DISABLE;
   ```

2. node1 중지:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data1 stop
   ```

3. `data1_upgraded` 초기화

4. pg_upgrade로 node1 업그레이드

5. 업그레이드된 node1 시작:
   ```bash
   pg_ctl -D /opt/PostgreSQL/data1_upgraded start -l logfile
   ```

6. node2 구독 활성화:
   ```sql
   /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 ENABLE;
   ```

7. node1에 누락된 테이블 생성:
   ```sql
   /* node1 # */ CREATE TABLE distributors (did integer PRIMARY KEY, name varchar(40));
   ```

8. 데이터 복사를 위한 node1 구독 새로고침:
   ```sql
   /* node1 # */ ALTER SUBSCRIPTION sub1_node2_node1 REFRESH PUBLICATION;
   ```

9. node1의 node2 구독 비활성화:
   ```sql
   /* node1 # */ ALTER SUBSCRIPTION sub1_node2_node1 DISABLE;
   ```

10. node2 중지:
    ```bash
    pg_ctl -D /opt/PostgreSQL/data2 stop
    ```

11. `data2_upgraded` 초기화

12. pg_upgrade로 node2 업그레이드

13. 업그레이드된 node2 시작:
    ```bash
    pg_ctl -D /opt/PostgreSQL/data2_upgraded start -l logfile
    ```

14. node1 구독 활성화:
    ```sql
    /* node1 # */ ALTER SUBSCRIPTION sub1_node2_node1 ENABLE;
    ```

15. node2에 누락된 테이블 생성:
    ```sql
    /* node2 # */ CREATE TABLE distributors (did integer PRIMARY KEY, name varchar(40));
    ```

16. 데이터 복사를 위한 node2 구독 새로고침:
    ```sql
    /* node2 # */ ALTER SUBSCRIPTION sub1_node1_node2 REFRESH PUBLICATION;
    ```

#### 주요 요점

- 버전 요구 사항: 모든 노드가 17.0 이상이어야 함
- 백업 중요: 업그레이드 전 항상 백업 필요
- 구독 관리: 업그레이드 전 비활성화, 후 활성화
- 테이블 동기화: 다운타임 중 생성된 테이블을 수동으로 생성
- 발행 새로고침: 상태 동기화를 위해 항상 새로고침
- 순서 중요: 지정된 노드 업그레이드 순서 준수 필요

---

### 29.14. 빠른 설정

#### 단계 1: postgresql.conf 구성

발행자 데이터베이스에서 다음 구성 옵션 설정:

```
wal_level = logical
```

참고: 다른 필수 설정은 기본값으로 충분함.

#### 단계 2: pg_hba.conf 구성

복제 연결을 허용하도록 `pg_hba.conf` 조정 필요(네트워크 구성 및 사용자에 따라 값 조정):

```
host     all     repuser     0.0.0.0/0     md5
```

#### 단계 3: 발행 생성 (발행자 데이터베이스에서)

```sql
CREATE PUBLICATION mypub FOR TABLE users, departments;
```

이렇게 하면 `users` 및 `departments` 테이블을 포함하는 `mypub`라는 발행이 생성됨.

#### 단계 4: 구독 생성 (구독자 데이터베이스에서)

```sql
CREATE SUBSCRIPTION mysub CONNECTION 'dbname=foo host=bar user=repuser' PUBLICATION mypub;
```

이렇게 하면 다음을 수행하는 `mysub`라는 구독이 생성됨:
- 지정된 연결 문자열을 사용하여 발행자에 연결
- `mypub` 발행을 구독

#### 결과

구독이 생성되면 복제 프로세스가 자동으로:
1. `users` 및 `departments`의 초기 테이블 내용을 동기화함
2. 해당 테이블에 대한 증분 변경 사항을 복제하기 시작함

이것이 PostgreSQL에서 기본 논리적 복제에 필요한 최소 구성임.

---

### 참고 자료

- [PostgreSQL 18 공식 문서 - Logical Replication](https://www.postgresql.org/docs/current/logical-replication.html)
- [CREATE PUBLICATION](https://www.postgresql.org/docs/current/sql-createpublication.html)
- [CREATE SUBSCRIPTION](https://www.postgresql.org/docs/current/sql-createsubscription.html)
- [ALTER SUBSCRIPTION](https://www.postgresql.org/docs/current/sql-altersubscription.html)
