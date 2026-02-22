# PostgreSQL 서버 애플리케이션 (Server Applications)

PostgreSQL 서버 애플리케이션은 데이터베이스 클러스터의 초기화, 시작, 중지, 업그레이드 및 복구 등을 수행하는 명령줄 도구들입니다. 이 문서에서는 주요 서버 애플리케이션인 `initdb`, `pg_ctl`, `postgres`, `pg_upgrade`, `pg_resetwal`, `pg_rewind`에 대해 다룹니다.

---

## 목차

1. [initdb - 데이터베이스 클러스터 초기화](#1-initdb---데이터베이스-클러스터-초기화)
2. [pg_ctl - PostgreSQL 서버 제어](#2-pg_ctl---postgresql-서버-제어)
3. [postgres - PostgreSQL 데이터베이스 서버](#3-postgres---postgresql-데이터베이스-서버)
4. [pg_upgrade - PostgreSQL 버전 업그레이드](#4-pg_upgrade---postgresql-버전-업그레이드)
5. [pg_resetwal - WAL 및 제어 정보 재설정](#5-pg_resetwal---wal-및-제어-정보-재설정)
6. [pg_rewind - 데이터 디렉토리 동기화](#6-pg_rewind---데이터-디렉토리-동기화)

---

## 1. initdb - 데이터베이스 클러스터 초기화

### 개요

`initdb`는 새로운 PostgreSQL 데이터베이스 클러스터(Database Cluster)를 생성하는 도구입니다. 데이터베이스 클러스터는 단일 서버 인스턴스에서 관리하는 데이터베이스들의 모음입니다.

### 기본 문법

```bash
initdb [option...] [--pgdata | -D] directory
```

### 주요 기능

`initdb`는 다음 작업을 수행합니다:

- 클러스터 데이터가 저장될 디렉토리 생성
- 공유 카탈로그 테이블(Shared Catalog Tables) 생성
- 기본 데이터베이스 생성:
  - postgres: 사용자, 유틸리티, 서드파티 애플리케이션용 기본 데이터베이스
  - template1: 이후 `CREATE DATABASE` 명령어로 복사될 소스 데이터베이스
  - template0: 수정되면 안 되는 원본 템플릿 데이터베이스

### 중요 사항

- 소유권(Ownership): `initdb`를 실행하는 OS 사용자가 서버 프로세스 소유자가 됨
- 권한(Permission): root로 실행 불가 (거부됨)
- 보안(Security): 기본적으로 클러스터 소유자만 접근 가능

### 주요 옵션

#### 필수 옵션

| 옵션 | 설명 |
|------|------|
| `-D directory`, `--pgdata=directory` | 데이터베이스 클러스터가 저장될 디렉토리 (`PGDATA` 환경변수로도 설정 가능) |

#### 인증 관련 (Authentication)

| 옵션 | 설명 |
|------|------|
| `-A authmethod`, `--auth=authmethod` | 로컬 사용자의 기본 인증 방법 (`pg_hba.conf`에 사용) |
| `--auth-host=authmethod` | TCP/IP 연결의 인증 방법 |
| `--auth-local=authmethod` | Unix 도메인 소켓 연결의 인증 방법 |

#### 인코딩 및 로케일 (Encoding & Locale)

| 옵션 | 설명 |
|------|------|
| `-E encoding`, `--encoding=encoding` | 템플릿 데이터베이스의 인코딩 지정 |
| `--locale=locale` | 클러스터의 기본 로케일 설정 |
| `--lc-collate=locale` | 정렬 순서 로케일 |
| `--lc-ctype=locale` | 문자 분류 로케일 |
| `--lc-messages=locale` | 메시지 로케일 |
| `--locale-provider={builtin|libc|icu}` | 로케일 제공자 설정 (기본값: libc) |
| `--icu-locale=locale` | ICU 제공자 사용 시 ICU 로케일 지정 |
| `--no-locale` | `--locale=C`와 동일 |

#### 슈퍼유저 설정 (Superuser)

| 옵션 | 설명 |
|------|------|
| `-U username`, `--username=username` | 부트스트랩 슈퍼유저 이름 (기본값: OS 사용자명) |
| `-W`, `--pwprompt` | 슈퍼유저 비밀번호 프롬프트 |
| `--pwfile=filename` | 파일에서 슈퍼유저 비밀번호 읽기 |

#### 데이터 보호 (Data Protection)

| 옵션 | 설명 |
|------|------|
| `-k`, `--data-checksums` | I/O 시스템 손상 감지를 위한 체크섬 활성화 (기본값: 활성화) |
| `--no-data-checksums` | 체크섬 비활성화 |

#### WAL 관련 (Write-Ahead Log)

| 옵션 | 설명 |
|------|------|
| `-X directory`, `--waldir=directory` | WAL 저장 디렉토리 지정 |
| `--wal-segsize=size` | WAL 세그먼트 크기 설정 (1~1024 MB, 2의 거듭제곱, 기본값: 16 MB) |

#### 기타 옵션

| 옵션 | 설명 |
|------|------|
| `-g`, `--allow-group-access` | 클러스터 소유자와 같은 그룹의 사용자가 파일 읽기 가능 |
| `-T config`, `--text-search-config=config` | 기본 텍스트 검색 구성 설정 |
| `-c name=value`, `--set name=value` | 서버 파라미터 강제 설정 |
| `-N`, `--no-sync` | 파일 동기화 대기 생략 (테스트용) |
| `-s`, `--show` | 내부 설정 표시 후 종료 |

### 환경 변수 (Environment Variables)

| 변수 | 설명 |
|------|------|
| `PGDATA` | 데이터베이스 클러스터 저장 디렉토리 (`-D`로 오버라이드 가능) |
| `PG_COLOR` | 진단 메시지 색상 사용 여부 (`always`, `auto`, `never`) |
| `TZ` | 생성된 클러스터의 기본 시간대 |

### 사용 예제

#### 기본 초기화

```bash
initdb -D /var/lib/postgresql/data
```

#### 슈퍼유저 이름 및 비밀번호 지정

```bash
initdb -D /var/lib/postgresql/data -U postgres -W
```

#### 인코딩 및 로케일 설정 (한국어)

```bash
initdb -D /var/lib/postgresql/data -E UTF8 --locale=ko_KR.UTF-8
```

#### ICU 로케일 제공자 사용

```bash
initdb -D /var/lib/postgresql/data --locale-provider=icu --icu-locale=ko
```

#### WAL 디렉토리 별도 지정

```bash
initdb -D /var/lib/postgresql/data -X /var/lib/postgresql/wal
```

#### 서버 파라미터 설정

```bash
initdb -D /var/lib/postgresql/data -c max_connections=200 -c shared_buffers=1GB
```

---

## 2. pg_ctl - PostgreSQL 서버 제어

### 개요

`pg_ctl`은 PostgreSQL 데이터베이스 서버를 초기화, 시작, 중지, 제어 및 상태 확인하는 유틸리티입니다.

### 명령 형식

```bash
pg_ctl init[db] [-D datadir] [-s] [-o initdb-options]
pg_ctl start [-D datadir] [-l filename] [-W] [-t seconds] [-s] [-o options] [-p path] [-c]
pg_ctl stop [-D datadir] [-m s[mart]|f[ast]|i[mmediate]] [-W] [-t seconds] [-s]
pg_ctl restart [-D datadir] [-m s[mart]|f[ast]|i[mmediate]] [-W] [-t seconds] [-s] [-o options] [-c]
pg_ctl reload [-D datadir] [-s]
pg_ctl status [-D datadir]
pg_ctl promote [-D datadir] [-W] [-t seconds] [-s]
pg_ctl logrotate [-D datadir] [-s]
pg_ctl kill signal_name process_id
```

### 모드 설명

| 모드 | 설명 |
|------|------|
| init/initdb | 새 PostgreSQL 데이터베이스 클러스터 생성 |
| start | 백그라운드에서 서버 시작 |
| stop | 서버 종료 (`smart`/`fast`/`immediate` 모드 선택 가능) |
| restart | 서버 재시작 (stop + start) |
| reload | 설정 파일 다시 읽기 (`SIGHUP` 신호 전송) |
| status | 실행 중인 서버 상태 확인 |
| promote | 대기 서버(Standby)를 읽기-쓰기 모드로 전환 |
| logrotate | 서버 로그 파일 회전(Rotation) |
| kill | 프로세스에 신호 전송 (주로 Windows용) |

### 종료 모드 (Shutdown Modes)

| 모드 | 설명 |
|------|------|
| Smart | 새 연결 거부, 모든 기존 클라이언트 연결 대기 후 종료 |
| Fast (기본값) | 클라이언트 대기 없음, 활성 트랜잭션 롤백, 강제 연결 해제 후 종료 |
| Immediate | 모든 서버 프로세스 즉시 중단 (다음 시작 시 복구 필요) |

### 주요 옵션

#### 공통 옵션

| 옵션 | 설명 |
|------|------|
| `-D datadir`, `--pgdata=datadir` | 데이터베이스 설정 파일 위치 |
| `-s`, `--silent` | 에러만 출력, 정보 메시지 없음 |
| `-t seconds`, `--timeout=seconds` | 작업 완료 대기 최대 시간 (기본값: 60초) |
| `-w`, `--wait` | 작업 완료를 기다림 (기본값) |
| `-W`, `--no-wait` | 작업 완료를 기다리지 않음 |

#### start 관련 옵션

| 옵션 | 설명 |
|------|------|
| `-l filename`, `--log=filename` | 서버 로그 출력을 파일에 기록 |
| `-o options`, `--options=options` | `postgres` 명령에 직접 전달할 옵션 |
| `-p path` | `postgres` 실행파일 위치 지정 |
| `-c`, `--core-files` | 서버 크래시 시 코어 파일 생성 허용 |

#### stop 관련 옵션

| 옵션 | 설명 |
|------|------|
| `-m mode`, `--mode=mode` | 종료 모드: `smart`, `fast` (기본값), `immediate` |

### 환경 변수

| 변수 | 설명 |
|------|------|
| `PGCTLTIMEOUT` | 시작/종료 완료 대기 기본 시간 (초, 기본값: 60) |
| `PGDATA` | 기본 데이터 디렉토리 위치 |

### 사용 예제

#### 서버 시작

```bash
pg_ctl start -D /usr/local/pgsql/data
```

#### 서버 시작 (포트 및 옵션 지정)

```bash
pg_ctl start -D /usr/local/pgsql/data -o "-p 5433 -c fsync=off"
```

#### 로그 파일과 함께 서버 시작

```bash
pg_ctl start -D /usr/local/pgsql/data -l /var/log/postgresql/server.log
```

#### 서버 종료

```bash
pg_ctl stop -D /usr/local/pgsql/data
```

#### Smart 모드로 종료

```bash
pg_ctl stop -D /usr/local/pgsql/data -m smart
```

#### Immediate 모드로 종료 (긴급 상황)

```bash
pg_ctl stop -D /usr/local/pgsql/data -m immediate
```

#### 서버 재시작

```bash
pg_ctl restart -D /usr/local/pgsql/data
```

#### 서버 상태 확인

```bash
pg_ctl status -D /usr/local/pgsql/data
```

출력 예:

```
pg_ctl: server is running (PID: 13718)
/usr/local/pgsql/bin/postgres "-D" "/usr/local/pgsql/data"
```

#### 설정 파일 다시 읽기 (Reload)

```bash
pg_ctl reload -D /usr/local/pgsql/data
```

#### 대기 서버를 읽기-쓰기 모드로 승격 (Promote)

```bash
pg_ctl promote -D /usr/local/pgsql/data
```

#### 데이터베이스 초기화

```bash
pg_ctl init -D /usr/local/pgsql/data
```

### 종료 상태 코드 (Exit Status)

| 상태 | 의미 |
|------|------|
| 0 | 정상 완료 |
| 3 | status 모드에서 서버 미실행 |
| 4 | status 모드에서 접근 가능한 데이터 디렉토리 없음 |
| 그 외 | 작업 실패 또는 타임아웃 |

---

## 3. postgres - PostgreSQL 데이터베이스 서버

### 개요

`postgres`는 PostgreSQL 데이터베이스 서버 프로세스입니다. 클라이언트 애플리케이션이 데이터베이스에 접근하려면 실행 중인 `postgres` 인스턴스에 연결해야 합니다.

### 기본 특징

- 데이터베이스 클러스터: 하나의 `postgres` 인스턴스는 정확히 하나의 데이터베이스 클러스터를 관리
- 시작 방법: `-D` 옵션 또는 `PGDATA` 환경 변수로 데이터 디렉토리 위치 지정
- 기본 동작: 기본적으로 foreground에서 실행되며 로그를 stderr로 출력
- 권장 사항: 직접 실행보다 `pg_ctl` 유틸리티 사용 권장

### 주요 옵션

#### 일반 목적 옵션 (General Purpose Options)

| 옵션 | 설명 |
|------|------|
| `-B nbuffers` | 공유 버퍼(Shared Buffers) 수 설정 |
| `-c name=value` | 런타임 파라미터 설정 (여러 번 사용 가능) |
| `-C name` | 파라미터 값 출력 후 종료 |
| `-d debug-level` | 디버그 레벨 설정 (1~5) |
| `-D datadir` | 데이터베이스 디렉토리 경로 지정 |
| `-e` | 유럽식 날짜 형식 설정 (DMY) |
| `-F` | fsync 비활성화 (성능 향상, 데이터 손상 위험) |
| `-h hostname` | TCP/IP 바인드 주소 지정 |
| `-k directory` | Unix 소켓 디렉토리 지정 |
| `-l` | SSL 보안 연결 활성화 |
| `-N max-connections` | 최대 클라이언트 연결 수 |
| `-p port` | TCP/IP 포트 또는 소켓 지정 |
| `-s` | 명령어 실행 시간/통계 출력 |
| `-S work-mem` | 정렬/해시 테이블 메모리 설정 |

#### 디버깅 옵션 (Semi-internal Options)

| 옵션 | 설명 |
|------|------|
| `-f {s\|i\|o\|b\|t\|n\|m\|h}` | 스캔/조인 방법 비활성화 |
| `-O` | 시스템 테이블 구조 수정 허용 |
| `-P` | 손상된 시스템 인덱스 복구 |
| `-t pa[rser]\|pl[anner]\|e[xecutor]` | 타이밍 통계 출력 |
| `-T` | 코어 덤프 파일 생성 (`SIGABRT` 사용) |
| `-W seconds` | 새 서버 프로세스 시작 시 지연 |

### 단일 사용자 모드 (Single-User Mode)

단일 사용자 모드는 부팅, 디버깅, 재해 복구 시 사용합니다.

#### 시작 명령어

```bash
postgres --single -D /usr/local/pgsql/data [other-options] my_database
```

#### 단일 사용자 모드 옵션

| 옵션 | 설명 |
|------|------|
| `--single` | 단일 사용자 모드 선택 (첫 번째 인자) |
| `database` | 접근할 데이터베이스 이름 (마지막 인자) |
| `-E` | 실행 전 모든 명령어 표준 출력 |
| `-j` | 명령어 종료자: 세미콜론 + 2개 개행 |
| `-r filename` | 서버 로그를 파일로 리다이렉트 |

#### 특징

- newline이 명령어 종료자 (기본)
- `-j` 옵션 사용 시: `세미콜론 + 개행 + 개행`이 종료자
- 여러 명령어가 단일 트랜잭션으로 실행
- EOF (`Ctrl+D`)로 종료
- 라인 편집 기능 없음 (명령어 히스토리 불가)

### 신호 처리 (Signal Handling)

| 신호 | 동작 |
|------|------|
| `SIGTERM` | 정상 종료 (모든 클라이언트 대기) - Smart Shutdown |
| `SIGINT` | 강제 종료 (클라이언트 즉시 연결 끊음) - Fast Shutdown |
| `SIGQUIT` | 즉시 종료 (복구 절차 필요) - Immediate Shutdown |
| `SIGHUP` | 설정 파일 재로드 |
| `SIGKILL` | 사용 금지 (리소스 해제 불가) |

### 환경 변수

| 변수 | 설명 |
|------|------|
| `PGCLIENTENCODING` | 기본 문자 인코딩 |
| `PGDATA` | 기본 데이터 디렉토리 위치 |
| `PGDATESTYLE` | DateStyle 파라미터 기본값 (deprecated) |
| `PGPORT` | 기본 포트 번호 |

### 사용 예제

#### 기본 시작

```bash
postgres -D /usr/local/pgsql/data
```

#### 특정 포트 지정

```bash
postgres -p 1234 -D /usr/local/pgsql/data
```

#### 런타임 파라미터 설정

```bash
postgres -c work_mem=256MB -D /usr/local/pgsql/data
postgres --work_mem=256MB -D /usr/local/pgsql/data
```

#### 단일 사용자 모드로 시작

```bash
postgres --single -D /usr/local/pgsql/data postgres
```

---

## 4. pg_upgrade - PostgreSQL 버전 업그레이드

### 개요

`pg_upgrade`는 PostgreSQL 데이터를 메이저 버전(Major Version) 간에 업그레이드하는 도구입니다. 기존의 dump/restore 과정 없이 빠르게 업그레이드할 수 있습니다.

### 지원 범위

- 메이저 버전 업그레이드: PostgreSQL 9.2.X 이상에서 현재 메이저 릴리스로 업그레이드 가능
  - 예: 12.14 → 13.10, 14.9 → 15.5
- 마이너 버전 업그레이드: 불필요 (예: 12.7 → 12.8, 14.1 → 14.5)

### 기본 문법

```bash
pg_upgrade -b oldbindir [-B newbindir] -d oldconfigdir -D newconfigdir [option]...
```

### 주요 옵션

#### 필수 옵션

| 옵션 | 환경변수 | 설명 |
|------|---------|------|
| `-b`, `--old-bindir` | `PGBINOLD` | 이전 PostgreSQL 실행 파일 디렉토리 |
| `-d`, `--old-datadir` | `PGDATAOLD` | 이전 데이터베이스 클러스터 설정 디렉토리 |
| `-D`, `--new-datadir` | `PGDATANEW` | 새 데이터베이스 클러스터 설정 디렉토리 |
| `-B`, `--new-bindir` | `PGBINNEW` | 새 PostgreSQL 실행 파일 디렉토리 (기본값: pg_upgrade 위치) |

#### 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-c`, `--check` | 변경 없이 호환성만 확인 |
| `-j`, `--jobs=njobs` | 병렬 처리 작업 수 |
| `-k`, `--link` | 파일 복사 대신 하드링크 사용 |
| `-N`, `--no-sync` | 동기화 대기 안 함 (테스트용) |
| `-o`, `--old-options options` | 이전 postgres 커맨드 옵션 |
| `-O`, `--new-options options` | 새 postgres 커맨드 옵션 |
| `-p`, `--old-port=port` | 이전 클러스터 포트 |
| `-P`, `--new-port=port` | 새 클러스터 포트 |
| `-r`, `--retain` | 성공 후에도 SQL/로그 파일 유지 |
| `-s`, `--socketdir=dir` | postmaster 소켓 디렉토리 |
| `-U`, `--username=username` | 클러스터 설치 사용자명 |
| `-v`, `--verbose` | 상세 로깅 활성화 |

#### 파일 전송 모드 옵션 (File Transfer Mode)

| 옵션 | 설명 |
|------|------|
| `--copy` | 파일 복사 (기본값) |
| `--link` | 하드링크 사용 (빠르지만 이전 클러스터 사용 불가) |
| `--clone` | 효율적인 파일 클로닝 (Linux Btrfs/XFS, macOS APFS) |
| `--copy-file-range` | `copy_file_range` 시스템 호출 사용 |
| `--swap` | 데이터 디렉토리 이동 (가장 빠름) |

### 업그레이드 단계

#### 1단계: 준비 작업

```bash
# 이전 클러스터 이동 (필요시)
mv /usr/local/pgsql /usr/local/pgsql.old

# 새 버전 설치 (소스 빌드 시)
./configure --prefix=/usr/local/pgsql.new [호환 옵션들]
make && make install
```

#### 2단계: 새 클러스터 초기화

```bash
initdb -D /path/to/new/cluster [호환 옵션들]
```

#### 3단계: 인증 설정 및 서버 중지

```bash
# pg_hba.conf 수정 (peer 인증으로 변경) 또는 ~/.pgpass 파일 생성

# 양쪽 서버 중지
pg_ctl -D /opt/PostgreSQL/12 stop
pg_ctl -D /opt/PostgreSQL/18 stop
```

#### 4단계: pg_upgrade 실행

```bash
# 기본 실행 (복사 모드)
/usr/local/pgsql.new/bin/pg_upgrade \
  -b /usr/local/pgsql/bin \
  -B /usr/local/pgsql.new/bin \
  -d /var/lib/pgsql/data \
  -D /var/lib/pgsql/new/data

# 하드링크 모드 (빠름)
pg_upgrade \
  -b /usr/local/pgsql/bin \
  -B /usr/local/pgsql.new/bin \
  -d /var/lib/pgsql/data \
  -D /var/lib/pgsql/new/data \
  --link

# 병렬 처리 (CPU 코어 수만큼)
pg_upgrade ... --jobs=8

# 미리 확인만 (실제 업그레이드 없음)
pg_upgrade ... --check
```

#### 5단계: 업그레이드 후 처리

```bash
# 설정 파일 복원
cp /usr/local/pgsql.old/data/pg_hba.conf /usr/local/pgsql.new/data/

# 새 서버 시작
pg_ctl -D /opt/PostgreSQL/18 start

# pg_upgrade가 생성한 스크립트 실행
psql --username=postgres --file=script.sql postgres

# 통계 재생성
vacuumdb --all --analyze-in-stages --missing-stats-only
vacuumdb --all --analyze-only
```

#### 6단계: 이전 클러스터 삭제

```bash
# pg_upgrade 완료 시 제공된 스크립트로 삭제
./delete_old_cluster.sh

# 또는 수동으로
rm -rf /usr/local/pgsql.old/data
```

### 사용 예제

#### 완전한 업그레이드 시나리오 (PostgreSQL 14 → 18)

```bash
#!/bin/bash

# PostgreSQL 14 → 18 업그레이드

OLD_BIN=/usr/lib/postgresql/14/bin
NEW_BIN=/usr/lib/postgresql/18/bin
OLD_DATA=/var/lib/postgresql/14/main
NEW_DATA=/var/lib/postgresql/18/main

# 1. 미리 확인
sudo -u postgres $NEW_BIN/pg_upgrade \
  -b $OLD_BIN \
  -B $NEW_BIN \
  -d $OLD_DATA \
  -D $NEW_DATA \
  --check

# 2. 서버 중지
sudo systemctl stop postgresql

# 3. 업그레이드 실행
sudo -u postgres $NEW_BIN/pg_upgrade \
  -b $OLD_BIN \
  -B $NEW_BIN \
  -d $OLD_DATA \
  -D $NEW_DATA \
  --jobs=4 \
  --link

# 4. 새 서버 시작
sudo systemctl start postgresql

# 5. 통계 재생성
sudo -u postgres vacuumdb --all --analyze-only

# 6. 정리
sudo rm -rf /usr/lib/postgresql/14
```

#### Windows에서의 업그레이드

```bash
pg_upgrade.exe ^
  --old-datadir "C:/Program Files/PostgreSQL/12/data" ^
  --new-datadir "C:/Program Files/PostgreSQL/18/data" ^
  --old-bindir "C:/Program Files/PostgreSQL/12/bin" ^
  --new-bindir "C:/Program Files/PostgreSQL/18/bin"
```

### 주요 주의사항

- 경고: 업그레이드 중 이전 슈퍼유저의 임의 코드 실행 가능
- 링크 모드 제약: 이전 클러스터 사용 불가능, 새 클러스터 시작 후 이전 클러스터 손상 위험
- 파일 시스템 요구사항: 링크/클론/스왑 모드에서는 이전/새 클러스터가 같은 파일 시스템에 있어야 함

---

## 5. pg_resetwal - WAL 및 제어 정보 재설정

### 개요

`pg_resetwal`는 PostgreSQL 데이터베이스 클러스터의 Write-Ahead Log(WAL)와 제어 정보(`pg_control`)를 초기화하는 도구입니다. 파일 손상으로 서버가 시작되지 않을 때의 마지막 수단입니다.

### 기본 문법

```bash
pg_resetwal [ -f | --force ] [ -n | --dry-run ] [ option... ] [ -D | --pgdata ] datadir
```

### 중요 주의사항

이 도구는 매우 위험합니다:

- 서버가 비정상 종료되었거나 제어 파일이 손상된 경우 `-f` 옵션 필수
- 복구 후 즉시 데이터 덤프 및 복구 필요
- 데이터 수정 작업 금지 (손상 악화 가능)

안전한 사용:

- 정상 종료된 데이터 디렉토리에서는 부작용 없음
- `--wal-segsize` 같은 옵션으로 안전하게 전역 설정 변경 가능

### 주요 옵션

#### 제어 옵션

| 옵션 | 설명 |
|------|------|
| `-D datadir`, `--pgdata=datadir` | 데이터 디렉토리 위치 (필수) |
| `-f`, `--force` | 위험한 상황에서도 강제 실행 |
| `-n`, `--dry-run` | 실제 수정 없이 복구 값 출력 |
| `-V`, `--version` | 버전 정보 표시 |
| `-?`, `--help` | 도움말 표시 |

#### 트랜잭션 ID 관련 (Transaction ID)

| 옵션 | 설명 |
|------|------|
| `-x xid`, `--next-transaction-id=xid` | 다음 트랜잭션 ID 설정 |
| `-u xid`, `--oldest-transaction-id=xid` | 가장 오래된 미동결 트랜잭션 ID 설정 |
| `-e xid_epoch`, `--epoch=xid_epoch` | 트랜잭션 ID의 에포크 설정 |

#### 멀티트랜잭션 관련 (Multitransaction)

| 옵션 | 설명 |
|------|------|
| `-m mxid,mxid`, `--multixact-ids=mxid,mxid` | 다음 멀티트랜잭션 ID와 가장 오래된 ID 설정 |
| `-O mxoff`, `--multixact-offset=mxoff` | 멀티트랜잭션 오프셋 설정 |

#### WAL 관련 (Write-Ahead Log)

| 옵션 | 설명 |
|------|------|
| `-l walfile`, `--next-wal-file=walfile` | WAL 시작 위치 설정 |
| `--wal-segsize=wal_segment_size` | WAL 세그먼트 크기 설정 (1~1024 MB) |

#### 기타 옵션

| 옵션 | 설명 |
|------|------|
| `-o oid`, `--next-oid=oid` | 다음 OID 설정 |
| `-c xid,xid`, `--commit-timestamp-ids=xid,xid` | 커밋 시간 조회 가능한 트랜잭션 ID 범위 설정 |

### 안전값 결정 방법

#### 다음 트랜잭션 ID (`-x`)

```bash
# pg_xact 디렉토리의 가장 큰 파일명 + 1, × 1048576 (0x100000)
# 예: 0011이 최대값이면 -x 0x1200000 사용
ls -la $PGDATA/pg_xact/
```

#### 가장 오래된 트랜잭션 ID (`-u`)

```bash
# pg_xact 디렉토리의 가장 작은 파일명 × 1048576 (0x100000)
# 예: 0007이 최소값이면 -u 0x700000 사용
ls -la $PGDATA/pg_xact/
```

#### 다음 WAL 파일 (`-l`)

```bash
# pg_wal 디렉토리의 기존 파일보다 큰 값 필요
# 예: 00000001000000320000004A가 최대면 -l 00000001000000320000004B 이상 사용
ls -la $PGDATA/pg_wal/
```

### 실행 조건

| 조건 | 요구사항 |
|------|---------|
| 서버 상태 | 반드시 서버가 종료되어야 함 |
| 사용자 | 서버를 설치한 사용자만 실행 가능 |
| 버전 | 동일 메이저 버전 서버에서만 작동 |

### 사용 예제

#### 기본 사용 (안전한 상황)

```bash
# 드라이 런 (실제 수정 없음)
pg_resetwal -n -D /var/lib/postgresql/14/main

# 실제 실행
pg_resetwal -D /var/lib/postgresql/14/main
```

#### 위험한 상황에서의 사용

```bash
# 비정상 종료된 서버의 WAL 초기화
pg_resetwal -f -D /var/lib/postgresql/14/main
```

#### 수동 값 설정

```bash
# 트랜잭션 ID와 OID 수동 설정
pg_resetwal -x 0x1200000 -u 0x700000 -o 100000 -D /var/lib/postgresql/14/main
```

#### WAL 세그먼트 크기 변경

```bash
# 64MB WAL 세그먼트로 변경
pg_resetwal --wal-segsize=64 -D /var/lib/postgresql/14/main
```

#### 멀티트랜잭션 정보 설정

```bash
pg_resetwal -m 0x20000,0x10000 -O 0x150000 -D /var/lib/postgresql/14/main
```

### 데이터 복구 절차

```bash
# 1. pg_resetwal 실행 (필요시)
pg_resetwal -f -D /var/lib/postgresql/data

# 2. 서버 시작 확인
pg_ctl -D /var/lib/postgresql/data start

# 3. 즉시 데이터 덤프
pg_dumpall > backup.sql

# 4. initdb로 초기화
initdb -D /var/lib/postgresql/new_data

# 5. 새 서버 시작
pg_ctl -D /var/lib/postgresql/new_data start

# 6. 데이터 복구
psql < backup.sql

# 7. 일관성 검증
```

---

## 6. pg_rewind - 데이터 디렉토리 동기화

### 개요

`pg_rewind`는 PostgreSQL 클러스터의 타임라인(Timeline)이 분기된 후, 한 클러스터를 다른 클러스터 사본과 동기화하는 도구입니다.

### 전형적인 사용 사례

- 페일오버(Failover) 후 이전 주(Primary) 서버를 새로운 주 서버를 따르는 대기(Standby)로 복구
- 복제 구성에서 분기된 서버를 다시 동기화

### 주요 특징

- 효율성: 변경된 블록만 복사 (변경되지 않은 관계 블록 비교/복사 불필요)
- 속도: 대규모 데이터베이스에서 블록 변화가 작을 때 rsync 등보다 훨씬 빠름

### 요구사항

- `wal_log_hints` 옵션 활성화 또는 데이터 체크섬 활성화
- `full_page_writes = on` (기본값)

### 기본 문법

```bash
pg_rewind [option...] {-D | --target-pgdata} directory {--source-pgdata=directory | --source-server=connstr}
```

### 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-D directory`, `--target-pgdata=directory` | 필수 동기화할 대상 데이터 디렉토리 (서버 종료 필수) |
| `--source-pgdata=directory` | 소스 서버 데이터 디렉토리 경로 (서버 깔끔한 종료 필수) |
| `--source-server=connstr` | libpq 연결 문자열로 소스 서버 지정 (서버 실행 중) |
| `-R`, `--write-recovery-conf` | `standby.signal` 생성 및 `postgresql.auto.conf`에 연결 설정 추가 |
| `-n`, `--dry-run` | 실제 수정 없이 모든 작업 실행 (테스트용) |
| `-N`, `--no-sync` | 디스크 동기화 대기 생략 (테스트용) |
| `-P`, `--progress` | 진행 상황 보고 활성화 |
| `-c`, `--restore-target-wal` | WAL 아카이브에서 누락된 WAL 파일 검색 |
| `--config-file=filename` | 대상 클러스터 구성 파일 지정 |
| `--debug` | 상세 디버깅 출력 |
| `--no-ensure-shutdown` | 깔끔한 종료 확인 건너뛰기 |

### 동작 방식

#### 1단계: WAL 스캔

- 소스 클러스터의 타임라인이 분기된 지점 이후 마지막 체크포인트부터 대상 클러스터의 WAL 로그 스캔
- 변경된 모든 데이터 블록 기록
- 누락된 WAL 파일이 있으면 `-c` 옵션으로 다시 실행

#### 2단계: 변경 블록 복사

```
--source-pgdata (파일 시스템 접근) 또는
--source-server (SQL 접근)
```

- 모든 변경 블록을 소스에서 대상으로 복사
- 관계 파일이 타임라인 분기 직전의 체크포인트 상태로 동기화

#### 3단계: 기타 파일 복사

- 새로운 관계 파일, WAL 세그먼트, `pg_xact`, 구성 파일 전체 복사
- 제외 디렉토리: `pg_dynshmem/`, `pg_notify/`, `pg_replslot/`, `pg_serial/`, `pg_snapshots/`, `pg_stat_tmp/`, `pg_subtrans/`

#### 4단계: backup_label 생성

- WAL 재생 시작점 구성
- `pg_control` 파일에 최소 일관성 LSN 설정

#### 5단계: WAL 재생

- 대상 서버 시작 시 필요한 WAL 재생
- 일관된 상태의 데이터 디렉토리 완성

### 사용자 권한 설정

슈퍼유저 대신 필요한 권한만 가진 역할 생성:

```sql
CREATE USER rewind_user LOGIN;
GRANT EXECUTE ON function pg_catalog.pg_ls_dir(text, boolean, boolean) TO rewind_user;
GRANT EXECUTE ON function pg_catalog.pg_stat_file(text, boolean) TO rewind_user;
GRANT EXECUTE ON function pg_catalog.pg_read_binary_file(text) TO rewind_user;
GRANT EXECUTE ON function pg_catalog.pg_read_binary_file(text, bigint, bigint, boolean) TO rewind_user;
```

### 사용 예제

#### 파일 시스템 경로로 동기화

```bash
pg_rewind -D /var/lib/postgresql/target_data \
          --source-pgdata=/var/lib/postgresql/source_data
```

#### 라이브 서버로 동기화 (복구 설정 자동 생성)

```bash
pg_rewind -D /var/lib/postgresql/target_data \
          --source-server="host=primary_server dbname=postgres user=rewind_user" \
          -R
```

#### 드라이 런 (테스트)

```bash
pg_rewind -D /var/lib/postgresql/target_data \
          --source-pgdata=/var/lib/postgresql/source_data \
          -n --progress
```

#### WAL 아카이브에서 누락 파일 복구

```bash
pg_rewind -D /var/lib/postgresql/target_data \
          --source-pgdata=/var/lib/postgresql/source_data \
          -c -P
```

### 페일오버 후 복구 시나리오

```bash
# 1. 이전 Primary 서버 종료 확인
pg_ctl -D /var/lib/postgresql/old_primary stop

# 2. pg_rewind 실행 (새 Primary를 소스로)
pg_rewind -D /var/lib/postgresql/old_primary \
          --source-server="host=new_primary dbname=postgres" \
          -R -P

# 3. 이전 Primary를 Standby로 시작
pg_ctl -D /var/lib/postgresql/old_primary start
```

### 중요 주의사항

- 실패 시: 대상 데이터 폴더가 복구 불가능한 상태가 될 수 있음 - 새 백업 필수
- 읽기 전용 파일: SSL 인증서 등 읽기 전용 파일이 있으면 재생성 직전 제거 권장
- 복구 구성: 리와인드 후 복구를 구성하지 않고 서버 재시작하면 주 서버에서 다시 분기될 수 있음

### 환경 변수

- `--source-server` 사용 시 libpq 환경 변수 지원
- `PG_COLOR`: `always`, `auto`, `never` (진단 메시지 색상)

---

## 요약

| 도구 | 주요 용도 |
|------|----------|
| initdb | 새 데이터베이스 클러스터 초기화 |
| pg_ctl | 서버 시작, 중지, 재시작, 상태 확인 |
| postgres | 데이터베이스 서버 프로세스 (직접 또는 pg_ctl 통해 실행) |
| pg_upgrade | 메이저 버전 업그레이드 |
| pg_resetwal | WAL 및 제어 정보 재설정 (긴급 복구용) |
| pg_rewind | 분기된 클러스터 동기화 (페일오버 복구용) |

---

## 참고 문서

- [PostgreSQL 공식 문서 - Server Applications](https://www.postgresql.org/docs/current/reference-server.html)
- [PostgreSQL 공식 문서 - initdb](https://www.postgresql.org/docs/current/app-initdb.html)
- [PostgreSQL 공식 문서 - pg_ctl](https://www.postgresql.org/docs/current/app-pg-ctl.html)
- [PostgreSQL 공식 문서 - postgres](https://www.postgresql.org/docs/current/app-postgres.html)
- [PostgreSQL 공식 문서 - pg_upgrade](https://www.postgresql.org/docs/current/pgupgrade.html)
- [PostgreSQL 공식 문서 - pg_resetwal](https://www.postgresql.org/docs/current/app-pgresetwal.html)
- [PostgreSQL 공식 문서 - pg_rewind](https://www.postgresql.org/docs/current/app-pgrewind.html)
