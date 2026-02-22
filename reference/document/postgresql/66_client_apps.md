# PostgreSQL 클라이언트 애플리케이션 (Client Applications)

이 문서는 PostgreSQL 공식 문서를 기반으로 클라이언트 애플리케이션에 대해 설명합니다. 클라이언트 애플리케이션은 데이터베이스 서버가 설치된 위치와 관계없이 모든 호스트에서 실행할 수 있습니다.

---

## 목차

1. [클라이언트 애플리케이션 개요](#1-클라이언트-애플리케이션-개요)
2. [psql - 대화형 터미널](#2-psql---대화형-터미널)
3. [pg_dump - 데이터베이스 백업](#3-pg_dump---데이터베이스-백업)
4. [pg_restore - 데이터베이스 복원](#4-pg_restore---데이터베이스-복원)
5. [pg_dumpall - 클러스터 전체 백업](#5-pg_dumpall---클러스터-전체-백업)
6. [pg_basebackup - 기본 백업](#6-pg_basebackup---기본-백업)
7. [createdb - 데이터베이스 생성](#7-createdb---데이터베이스-생성)
8. [dropdb - 데이터베이스 삭제](#8-dropdb---데이터베이스-삭제)
9. [createuser - 사용자 생성](#9-createuser---사용자-생성)
10. [dropuser - 사용자 삭제](#10-dropuser---사용자-삭제)
11. [vacuumdb - 가비지 컬렉션 및 분석](#11-vacuumdb---가비지-컬렉션-및-분석)
12. [reindexdb - 인덱스 재구축](#12-reindexdb---인덱스-재구축)
13. [clusterdb - 테이블 클러스터링](#13-clusterdb---테이블-클러스터링)
14. [기타 유틸리티](#14-기타-유틸리티)

---

## 1. 클라이언트 애플리케이션 개요

PostgreSQL은 다양한 클라이언트 애플리케이션을 제공하며, 이들은 데이터베이스 관리, 백업/복원, 성능 테스트 등 다양한 작업을 수행할 수 있습니다.

### 1.1 클라이언트 애플리케이션 목록

| 애플리케이션 | 설명 |
|-------------|------|
| psql | PostgreSQL 대화형 터미널 |
| pg_dump | 데이터베이스를 SQL 스크립트 또는 아카이브 형식으로 내보내기 |
| pg_dumpall | 클러스터 전체를 스크립트 파일로 추출 |
| pg_restore | pg_dump로 생성된 아카이브에서 데이터베이스 복원 |
| pg_basebackup | PostgreSQL 클러스터의 기본 백업 수행 |
| createdb | 새 PostgreSQL 데이터베이스 생성 |
| dropdb | PostgreSQL 데이터베이스 삭제 |
| createuser | 새 PostgreSQL 사용자 계정 생성 |
| dropuser | PostgreSQL 사용자 계정 삭제 |
| vacuumdb | 가비지 컬렉션 및 분석 수행 |
| reindexdb | 데이터베이스 인덱스 재구축 |
| clusterdb | PostgreSQL 데이터베이스 클러스터링 |
| pg_isready | PostgreSQL 서버 연결 상태 확인 |
| pgbench | PostgreSQL 벤치마크 테스트 실행 |
| pg_config | 설치된 PostgreSQL 버전 정보 조회 |
| pg_receivewal | PostgreSQL 서버에서 WAL 스트리밍 |
| pg_recvlogical | PostgreSQL 논리적 디코딩 스트림 제어 |
| pg_verifybackup | 기본 백업의 무결성 검증 |
| pg_amcheck | 데이터베이스 손상 검사 |
| ecpg | 임베디드 SQL C 전처리기 |

### 1.2 공통 연결 옵션

대부분의 클라이언트 애플리케이션은 다음과 같은 공통 연결 옵션을 지원합니다:

| 옵션 | 설명 |
|------|------|
| `-h, --host=HOST` | 데이터베이스 서버 호스트명 |
| `-p, --port=PORT` | 데이터베이스 서버 포트 (기본값: 5432) |
| `-U, --username=USER` | 연결할 사용자명 |
| `-d, --dbname=DBNAME` | 데이터베이스 이름 |
| `-W, --password` | 비밀번호 프롬프트 강제 |
| `-w, --no-password` | 비밀번호 프롬프트 표시 안 함 |

### 1.3 환경 변수 (Environment Variables)

다음 환경 변수를 사용하여 기본 연결 매개변수를 설정할 수 있습니다:

```bash
# 기본 연결 매개변수
export PGHOST=localhost
export PGPORT=5432
export PGDATABASE=mydb
export PGUSER=postgres
export PGPASSWORD=secret  # 보안상 권장하지 않음

# 비밀번호 파일 사용 (권장)
export PGPASSFILE=~/.pgpass
```

---

## 2. psql - 대화형 터미널

psql은 PostgreSQL의 터미널 기반 프론트엔드로, SQL 쿼리를 대화형으로 입력하고 실행 결과를 확인할 수 있습니다.

### 2.1 기본 구문 (Syntax)

```bash
psql [option]... [dbname [username]]
```

### 2.2 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-d dbname` | 연결할 데이터베이스 이름 |
| `-h hostname` | 서버 호스트명 |
| `-p port` | 서버 포트 (기본값: 5432) |
| `-U username` | 데이터베이스 사용자 |
| `-c command` | 명령 문자열 실행 |
| `-f filename` | 파일에서 명령 실행 |
| `-l, --list` | 모든 데이터베이스 목록 표시 후 종료 |
| `-a, --echo-all` | 모든 입력 라인을 stdout으로 출력 |
| `-e, --echo-queries` | 서버로 전송된 SQL 명령 에코 |
| `-q, --quiet` | 정보 메시지 억제 |
| `-t, --tuples-only` | 테이블 데이터만 출력 (헤더 제외) |
| `-A, --no-align` | 정렬되지 않은 출력 모드 |
| `-H, --html` | HTML 출력 형식 |
| `-x, --expanded` | 확장된 테이블 포맷 |

### 2.3 연결 예제

```bash
# 로컬 데이터베이스에 연결
psql -d mydb

# 특정 호스트와 포트로 연결
psql -h localhost -p 5432 -U postgres -d mydb

# 연결 문자열 사용
psql "host=localhost port=5432 dbname=mydb user=postgres"

# URI 형식 연결 문자열
psql postgresql://user:password@host:5432/dbname?sslmode=require

# 서비스 파일 사용
psql "service=myservice sslmode=require"
```

### 2.4 SQL 명령 실행

```bash
# 단일 명령 실행
psql -c "SELECT * FROM users;"

# 여러 명령 실행
psql -c '\x' -c 'SELECT * FROM users;'

# 파일에서 명령 실행
psql -f script.sql

# 표준 입력에서 명령 실행
psql < commands.sql

# 모든 데이터베이스 목록 표시
psql -l
```

### 2.5 메타 명령 (Meta-Commands)

psql에서 백슬래시(`\`)로 시작하는 메타 명령을 사용하여 다양한 작업을 수행할 수 있습니다:

#### 2.5.1 정보 조회 명령

| 명령 | 설명 |
|------|------|
| `\l` | 데이터베이스 목록 |
| `\dt` | 테이블 목록 |
| `\dt+` | 테이블 목록 (상세) |
| `\d tablename` | 테이블 구조 설명 |
| `\d+ tablename` | 테이블 구조 설명 (상세) |
| `\di` | 인덱스 목록 |
| `\dv` | 뷰 목록 |
| `\df` | 함수 목록 |
| `\dn` | 스키마 목록 |
| `\du` | 역할/사용자 목록 |
| `\dp` | 테이블 권한 목록 |
| `\dx` | 설치된 확장 목록 |

#### 2.5.2 연결 및 실행 명령

| 명령 | 설명 |
|------|------|
| `\c dbname` | 다른 데이터베이스에 연결 |
| `\conninfo` | 현재 연결 정보 표시 |
| `\e` | 외부 편집기에서 쿼리 편집 |
| `\i filename` | 파일에서 명령 실행 |
| `\ir filename` | 상대 경로로 파일에서 명령 실행 |
| `\o filename` | 출력을 파일로 리다이렉션 |
| `\copy` | 클라이언트 측 COPY 명령 |
| `\! command` | 쉘 명령 실행 |
| `\q` | psql 종료 |

#### 2.5.3 출력 형식 명령

| 명령 | 설명 |
|------|------|
| `\x` | 확장 모드 토글 |
| `\x on` | 확장 모드 켜기 |
| `\x off` | 확장 모드 끄기 |
| `\x auto` | 확장 모드 자동 |
| `\pset format FORMAT` | 출력 형식 설정 |
| `\pset border N` | 테두리 스타일 설정 |
| `\pset null 'NULL'` | NULL 값 표시 문자열 설정 |
| `\timing` | 쿼리 실행 시간 표시 토글 |

### 2.6 출력 형식 설정

```sql
-- 정렬된 출력 (기본값)
\pset format aligned

-- CSV 형식
\pset format csv

-- 정렬되지 않은 형식
\pset format unaligned
\pset fieldsep ','

-- HTML 형식
\pset format html

-- 확장 모드 (세로 표시)
\x on

-- Markdown 형식
\pset format markdown

-- LaTeX 형식
\pset format latex
```

### 2.7 변수와 보간 (Variables and Interpolation)

```sql
-- 변수 설정
\set foo 'my_table'

-- 변수 사용 (인용 없음)
SELECT * FROM :foo;

-- 식별자로 인용
SELECT * FROM :"foo";

-- SQL 리터럴로 인용
SELECT * FROM :'foo';

-- 조건문 사용
\if :myvar
    SELECT 'Variable is set';
\else
    SELECT 'Variable is not set';
\endif

-- 현재 설정된 모든 변수 표시
\set
```

### 2.8 COPY 명령 사용

```sql
-- 테이블을 CSV 파일로 내보내기
\copy users TO '/tmp/users.csv' WITH CSV HEADER;

-- CSV 파일에서 테이블로 가져오기
\copy users FROM '/tmp/users.csv' WITH CSV HEADER;

-- 특정 컬럼만 내보내기
\copy users(id, name, email) TO '/tmp/users.csv' WITH CSV HEADER;

-- 쿼리 결과 내보내기
\copy (SELECT * FROM users WHERE active = true) TO '/tmp/active_users.csv' WITH CSV HEADER;
```

### 2.9 종료 코드 (Exit Codes)

| 코드 | 설명 |
|------|------|
| `0` | 성공적 완료 |
| `1` | 치명적 오류 |
| `2` | 연결 실패 (비대화형 모드) |
| `3` | `ON_ERROR_STOP` 설정 시 스크립트 오류 |

### 2.10 환경 변수

| 변수 | 설명 |
|------|------|
| `PGDATABASE` | 기본 데이터베이스 이름 |
| `PGHOST` | 기본 호스트명 |
| `PGPORT` | 기본 포트 |
| `PGUSER` | 기본 사용자명 |
| `PSQL_EDITOR` | `\e` 명령에 사용할 편집기 |
| `PAGER` | 출력을 위한 페이저 프로그램 |
| `PSQLRC` | 사용자 .psqlrc 파일 위치 |

### 2.11 실용적인 예제

```bash
# 데이터베이스 정보 빠르게 확인
psql -d mydb -c "\dt" -c "\du"

# 쿼리 결과를 CSV로 저장
psql -d mydb -A -F',' -c "SELECT * FROM users" > users.csv

# 스크립트 실행 및 오류 시 중단
psql -d mydb -v ON_ERROR_STOP=1 -f migration.sql

# 특정 테이블 구조 확인
psql -d mydb -c "\d+ users"

# 대용량 쿼리 결과를 파일로 저장
psql -d mydb -o result.txt -c "SELECT * FROM large_table"
```

---

## 3. pg_dump - 데이터베이스 백업

pg_dump는 PostgreSQL 데이터베이스를 SQL 스크립트 또는 아카이브 파일로 내보내는 유틸리티입니다. 데이터베이스가 활발히 사용되는 중에도 일관된 백업을 생성하며, 다른 사용자의 접근을 차단하지 않습니다.

### 3.1 기본 구문 (Syntax)

```bash
pg_dump [connection-option...] [option...] [dbname]
```

### 3.2 주요 기능

- 논블로킹 (Non-blocking): 동시 읽기/쓰기 접근을 방해하지 않음
- 다양한 형식: 일반 텍스트 SQL, 커스텀 아카이브, 디렉토리, tar 형식 지원
- 유연한 복원: 데이터베이스 객체를 선택적으로 복원 가능
- 병렬 덤프: 디렉토리 형식에서 병렬 처리 지원

### 3.3 출력 형식 옵션

| 형식 | 옵션 | 설명 |
|------|------|------|
| Plain | `-Fp` | SQL 텍스트 파일, 이식성이 좋지만 복원이 느림 |
| Custom | `-Fc` | 압축됨, 선택적 복원, 재정렬 가능 |
| Directory | `-Fd` | 병렬 덤프 및 복원 지원 |
| Tar | `-Ft` | 디렉토리 형식과 호환, 압축 없음 |

### 3.4 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-a, --data-only` | 데이터만 덤프, 스키마 제외 |
| `-s, --schema-only` | 스키마만 덤프, 데이터 제외 |
| `-c, --clean` | CREATE 문 전에 DROP 명령 추가 |
| `-C, --create` | CREATE DATABASE 명령 포함 |
| `-F format` | 출력 형식: p (plain), c (custom), d (directory), t (tar) |
| `-j njobs` | 병렬 덤프 (디렉토리 형식만) |
| `-t pattern` | 특정 테이블 덤프 |
| `-T pattern` | 특정 테이블 제외 |
| `-n pattern` | 특정 스키마 덤프 |
| `-N pattern` | 특정 스키마 제외 |
| `-v, --verbose` | 상세 출력 |
| `-Z level` | 압축 수준 (0-9) |
| `--inserts` | COPY 대신 INSERT 명령 사용 |
| `--column-inserts` | 컬럼 이름이 포함된 INSERT 명령 사용 |
| `--if-exists` | DROP 명령에 IF EXISTS 추가 |
| `--no-owner` | 소유권 복원 명령 생략 |
| `--no-privileges` | 권한 복원 명령 생략 |

### 3.5 기본 사용 예제

```bash
# 간단한 SQL 스크립트 덤프
pg_dump mydb > mydb_backup.sql

# 커스텀 아카이브 형식 (압축)
pg_dump -Fc mydb > mydb_backup.dump

# 디렉토리 형식 (병렬 덤프)
pg_dump -Fd -j 4 mydb -f mydb_backup_dir

# tar 형식
pg_dump -Ft mydb > mydb_backup.tar
```

### 3.6 선택적 덤프 예제

```bash
# 특정 테이블만 덤프
pg_dump -t users mydb > users.sql

# 특정 테이블 제외하고 덤프
pg_dump -T logs mydb > mydb_without_logs.sql

# 패턴과 일치하는 테이블 덤프
pg_dump -t 'emp*' mydb > emp_tables.sql

# 여러 테이블 덤프
pg_dump -t users -t orders -t products mydb > selected_tables.sql

# 특정 스키마 덤프
pg_dump -n public mydb > public_schema.sql

# 여러 스키마 덤프
pg_dump -n 'east*' -n 'west*' mydb > multi_schema.sql

# 데이터만 덤프 (스키마 제외)
pg_dump -a mydb > data_only.sql

# 스키마만 덤프 (데이터 제외)
pg_dump -s mydb > schema_only.sql
```

### 3.7 고급 사용 예제

```bash
# 압축된 덤프
pg_dump -Z 9 mydb > mydb_compressed.sql.gz

# 병렬 덤프 (5개 작업)
pg_dump -Fd -j 5 mydb -f /backup/mydb

# INSERT 문으로 덤프 (다른 DBMS와 호환성)
pg_dump --inserts mydb > mydb_inserts.sql

# 컬럼 이름 포함 INSERT
pg_dump --column-inserts mydb > mydb_column_inserts.sql

# 소유권 및 권한 제외
pg_dump --no-owner --no-privileges mydb > mydb_portable.sql

# DROP IF EXISTS 포함
pg_dump -c --if-exists mydb > mydb_with_drop.sql

# 필터 파일 사용
pg_dump --filter=filter.txt mydb > filtered.sql
```

### 3.8 필터 파일 예제

`filter.txt` 파일:
```
include table users*
include table orders
exclude table user_sessions
exclude table user_logs
```

### 3.9 복원 예제

```bash
# SQL 스크립트에서 복원
psql newdb < mydb_backup.sql

# 커스텀 아카이브에서 복원
pg_restore -d newdb mydb_backup.dump

# 디렉토리 형식에서 복원
pg_restore -d newdb mydb_backup_dir

# tar 형식에서 복원
pg_restore -d newdb mydb_backup.tar
```

### 3.10 중요 참고사항

- 보안: 일반 텍스트 덤프는 임의의 슈퍼유저 코드를 실행할 수 있습니다. 신뢰할 수 없는 소스의 덤프는 복원 전에 검사하세요.
- 프로덕션 백업: 정기 프로덕션 백업에는 pg_basebackup 또는 WAL 아카이빙을 권장합니다.
- 전역 객체: 역할, 테이블스페이스 등 클러스터 전체 객체는 pg_dumpall을 사용하세요.

---

## 4. pg_restore - 데이터베이스 복원

pg_restore는 pg_dump로 생성된 아카이브 파일(커스텀, 디렉토리, tar 형식)에서 PostgreSQL 데이터베이스를 복원하는 유틸리티입니다.

### 4.1 기본 구문 (Syntax)

```bash
pg_restore [connection-option...] [option...] [filename]
```

### 4.2 주요 기능

- 선택적 복원: 특정 스키마, 테이블, 함수, 트리거 선택 가능
- 병렬 처리: `-j` 플래그로 여러 작업 동시 실행
- 형식 지원: 커스텀, 디렉토리, tar 아카이브 형식
- 스크립트 생성: 직접 복원 대신 SQL 스크립트 생성 가능
- 유연한 출력: 내용 목록 표시, 항목 재정렬, 필터링

### 4.3 핵심 옵션

| 옵션 | 설명 |
|------|------|
| `-d, --dbname=dbname` | 복원할 대상 데이터베이스 |
| `-a, --data-only` | 데이터만 복원, 스키마 제외 |
| `-s, --schema-only` | 스키마만 복원, 데이터 제외 |
| `-c, --clean` | 재생성 전에 객체 삭제 |
| `-C, --create` | 복원 전 데이터베이스 생성 |
| `-1, --single-transaction` | 모든 명령을 BEGIN/COMMIT으로 감싸기 |
| `-j, --jobs=N` | N개의 동시 작업 실행 |
| `-f, --file=filename` | 스크립트를 위한 출력 파일 |
| `-l, --list` | 아카이브 내용 목록 표시 |
| `-L, --use-list=file` | 목록 파일에서 항목 복원 |
| `-v, --verbose` | 상세 출력 |

### 4.4 필터링 옵션

| 옵션 | 설명 |
|------|------|
| `-n, --schema=schema` | 특정 스키마 복원 |
| `-N, --exclude-schema=schema` | 특정 스키마 제외 |
| `-t, --table=table` | 특정 테이블 복원 |
| `-I, --index=index` | 특정 인덱스 복원 |
| `-P, --function=func(args)` | 특정 함수 복원 |
| `-T, --trigger=trigger` | 특정 트리거 복원 |
| `--filter=filename` | 파일에서 필터 패턴 읽기 |

### 4.5 기본 복원 예제

```bash
# 커스텀 아카이브에서 복원
pg_restore -d newdb mydb_backup.dump

# 디렉토리 형식에서 복원
pg_restore -d newdb mydb_backup_dir

# tar 형식에서 복원
pg_restore -d newdb mydb_backup.tar
```

### 4.6 데이터베이스 삭제 후 재생성

```bash
# 데이터베이스 삭제 후 재생성하며 복원
pg_restore -c -C -d postgres mydb_backup.dump
```

### 4.7 새 데이터베이스로 복원

```bash
# 빈 템플릿에서 새 데이터베이스 생성
createdb -T template0 newdb

# 복원 수행
pg_restore -d newdb mydb_backup.dump
```

### 4.8 선택적 복원 예제

```bash
# 특정 테이블만 복원
pg_restore -d mydb -t users mydb_backup.dump

# 특정 스키마만 복원
pg_restore -d mydb -n public mydb_backup.dump

# 데이터만 복원 (스키마 제외)
pg_restore -a -d mydb mydb_backup.dump

# 스키마만 복원 (데이터 제외)
pg_restore -s -d mydb mydb_backup.dump
```

### 4.9 병렬 복원

```bash
# 4개의 병렬 작업으로 복원
pg_restore -j 4 -d mydb mydb_backup.dump

# 디렉토리 형식에서 8개의 병렬 작업
pg_restore -j 8 -d mydb mydb_backup_dir
```

### 4.10 아카이브 내용 확인 및 재정렬

```bash
# 아카이브 내용 목록 표시
pg_restore -l mydb_backup.dump > contents.txt

# 목록 파일 편집 (주석 처리하거나 순서 변경)
# 편집된 목록으로 복원
pg_restore -L contents.txt -d mydb mydb_backup.dump
```

### 4.11 SQL 스크립트로 변환

```bash
# 아카이브를 SQL 스크립트로 변환
pg_restore mydb_backup.dump > restore_script.sql

# 특정 파일로 출력
pg_restore -f restore_script.sql mydb_backup.dump
```

### 4.12 트랜잭션 옵션

```bash
# 단일 트랜잭션으로 복원 (실패 시 전체 롤백)
pg_restore -1 -d mydb mydb_backup.dump

# 트랜잭션 크기 지정 (대용량 복원 시)
pg_restore --transaction-size=1000 -d mydb mydb_backup.dump
```

### 4.13 모범 사례 (Best Practices)

1. 새 데이터베이스는 `template0`에서 생성 (빈 상태 보장)
2. 원자성을 위해 `--single-transaction` 사용 (대용량에서는 잠금 제한 주의)
3. 대용량 복원 시 `--transaction-size=N` 사용
4. 복원 후 통계가 완전히 복원되지 않았으면 `ANALYZE` 실행
5. 멀티프로세서 시스템에서 복원 속도를 높이려면 `--jobs=N` 사용

### 4.14 보안 경고

복원은 임의의 코드를 실행합니다. 신뢰할 수 없는 소스의 슈퍼유저가 작성한 덤프라면 `pg_restore --file`을 사용하여 SQL 문을 먼저 검사하세요.

---

## 5. pg_dumpall - 클러스터 전체 백업

pg_dumpall은 PostgreSQL 데이터베이스 클러스터 전체를 SQL 스크립트 파일로 추출합니다. 모든 데이터베이스, 역할, 테이블스페이스 등 클러스터 수준의 객체를 포함합니다.

### 5.1 기본 구문 (Syntax)

```bash
pg_dumpall [connection-option...] [option...]
```

### 5.2 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-g, --globals-only` | 전역 객체만 덤프 (역할, 테이블스페이스) |
| `-r, --roles-only` | 역할만 덤프 |
| `-t, --tablespaces-only` | 테이블스페이스만 덤프 |
| `-c, --clean` | 재생성 전 데이터베이스 객체 삭제 |
| `--if-exists` | DROP 명령에 IF EXISTS 추가 |
| `-s, --schema-only` | 스키마만 덤프, 데이터 제외 |
| `-a, --data-only` | 데이터만 덤프, 스키마 제외 |
| `--no-role-passwords` | 역할 비밀번호 제외 |
| `-v, --verbose` | 상세 출력 |

### 5.3 사용 예제

```bash
# 전체 클러스터 백업
pg_dumpall > full_backup.sql

# 전역 객체만 백업 (역할, 테이블스페이스)
pg_dumpall -g > globals.sql

# 역할만 백업
pg_dumpall -r > roles.sql

# DROP 문 포함
pg_dumpall -c > full_backup_with_drop.sql

# 원격 서버에서 백업
pg_dumpall -h remotehost -U postgres > remote_backup.sql
```

### 5.4 복원 예제

```bash
# 전체 복원
psql -f full_backup.sql postgres

# 또는
psql postgres < full_backup.sql
```

### 5.5 pg_dump와의 차이점

| 항목 | pg_dump | pg_dumpall |
|------|---------|------------|
| 범위 | 단일 데이터베이스 | 전체 클러스터 |
| 역할 백업 | 불가 | 가능 |
| 테이블스페이스 | 불가 | 가능 |
| 출력 형식 | 다양 (plain, custom, directory, tar) | plain만 가능 |
| 병렬 덤프 | 가능 (directory 형식) | 불가 |
| 선택적 복원 | 가능 | 제한적 |

---

## 6. pg_basebackup - 기본 백업

pg_basebackup은 실행 중인 PostgreSQL 데이터베이스 클러스터의 기본 백업을 수행하는 유틸리티입니다. PITR(Point-In-Time Recovery) 및 스탠바이 서버 설정에 필수적입니다.

### 6.1 기본 구문 (Syntax)

```bash
pg_basebackup [option...]
```

### 6.2 주요 기능

- 전체 또는 증분 백업: 데이터베이스 클러스터의 정확한 복사본 또는 수정된 블록만 포함하는 증분 버전 생성
- 논블로킹 (Non-disruptive): 다른 데이터베이스 클라이언트에 영향을 주지 않음
- 복제 프로토콜: 복제 권한이 있는 일반 PostgreSQL 연결 사용
- 다양한 형식: 일반 파일 또는 tar 형식 출력 지원

### 6.3 요구사항

- 사용자는 `REPLICATION` 권한 또는 슈퍼유저여야 함
- `pg_hba.conf`에서 복제 연결을 허용해야 함
- 서버의 `max_wal_senders`가 백업과 WAL 스트리밍을 위해 충분히 설정되어야 함

### 6.4 주요 옵션

#### 6.4.1 출력 옵션

| 옵션 | 설명 |
|------|------|
| `-D directory` | 대상 디렉토리 (필수) |
| `-F format` | 출력 형식: 'p' (plain) 또는 't' (tar) |
| `-T olddir=newdir` | 테이블스페이스 재배치 |

#### 6.4.2 WAL 처리 옵션

| 옵션 | 설명 |
|------|------|
| `-X method` | WAL 수집: 'none', 'fetch', 또는 'stream' (기본값) |
| `-S slotname` | 특정 복제 슬롯 사용 |
| `--waldir=path` | WAL 디렉토리 위치 지정 |

#### 6.4.3 압축 옵션

| 옵션 | 설명 |
|------|------|
| `-z, --gzip` | gzip 압축 활성화 |
| `-Z method` | 압축 방식 지정: gzip, lz4, zstd, none |

#### 6.4.4 성능 및 보고 옵션

| 옵션 | 설명 |
|------|------|
| `-c {fast\|spread}` | 체크포인트 모드 (기본값: spread) |
| `-P, --progress` | 진행 상황 보고 활성화 |
| `-r rate` | 최대 전송 속도 (KB/s) |
| `-N, --no-sync` | 파일 동기화 건너뛰기 (빠르지만 위험) |

### 6.5 사용 예제

```bash
# 로컬 디렉토리로 기본 백업
pg_basebackup -D /usr/local/pgsql/backup

# 진행 상황 표시와 함께 압축된 tar 백업
pg_basebackup -D backup -F t -z -P

# 테이블스페이스 재배치와 함께 백업
pg_basebackup -D /backup -T /opt/ts=./backup/ts

# 원격 서버에서 gzip 압축으로 백업
pg_basebackup -h mydbserver -D backup -F t -z

# 빠른 체크포인트로 백업
pg_basebackup -D backup -c fast -P

# 전송 속도 제한
pg_basebackup -D backup -r 10240 -P

# 복제 슬롯 사용
pg_basebackup -D backup -S myslot -P
```

### 6.6 스탠바이 서버 설정

```bash
# 스탠바이 서버용 백업
pg_basebackup -D /var/lib/postgresql/standby \
    -h primary.example.com \
    -U replicator \
    -P \
    -R  # standby.signal 파일 및 연결 정보 자동 생성
```

### 6.7 중요 참고사항

- 시작 시 체크포인트가 필요함 (시간이 걸릴 수 있음)
- PostgreSQL 9.1+ 이상에서 동작 (일부 기능은 더 최신 버전 필요)
- WAL 스트리밍 (`-X stream`)은 서버 9.3+ 필요
- tar 형식은 서버 9.5+ 필요
- 증분 백업은 서버 17+ 필요
- 백업 매니페스트가 기본적으로 생성되어 `pg_verifybackup`으로 무결성 검증 가능

---

## 7. createdb - 데이터베이스 생성

createdb는 새 PostgreSQL 데이터베이스를 생성하는 유틸리티입니다. SQL `CREATE DATABASE` 명령의 래퍼입니다.

### 7.1 기본 구문 (Syntax)

```bash
createdb [connection-option...] [option...] [dbname [description]]
```

### 7.2 주요 옵션

#### 7.2.1 데이터베이스 구성 옵션

| 옵션 | 설명 |
|------|------|
| `-D, --tablespace=tablespace` | 데이터베이스의 기본 테이블스페이스 설정 |
| `-E, --encoding=encoding` | 문자 인코딩 지정 |
| `-l, --locale=locale` | 로케일 설정 (collate, ctype, icu-locale) |
| `-O, --owner=owner` | 데이터베이스 소유자 지정 |
| `-T, --template=template` | 템플릿 데이터베이스 지정 |
| `-S, --strategy=strategy` | 데이터베이스 생성 전략 설정 |

#### 7.2.2 로케일 옵션

| 옵션 | 설명 |
|------|------|
| `--lc-collate=locale` | LC_COLLATE 설정 |
| `--lc-ctype=locale` | LC_CTYPE 설정 |
| `--builtin-locale=locale` | 내장 프로바이더 로케일 |
| `--icu-locale=locale` | ICU 로케일 ID |
| `--locale-provider={builtin\|libc\|icu}` | 로케일 프로바이더 |

#### 7.2.3 기타 옵션

| 옵션 | 설명 |
|------|------|
| `-e, --echo` | 생성된 SQL 명령 에코 |
| `-V, --version` | 버전 출력 후 종료 |
| `-?, --help` | 도움말 표시 |

### 7.3 사용 예제

```bash
# 기본 설정으로 데이터베이스 생성
createdb mydb

# 설명과 함께 생성
createdb mydb "My test database"

# 특정 소유자 지정
createdb -O myuser mydb

# 특정 인코딩 지정
createdb -E UTF8 mydb

# 템플릿 지정
createdb -T template0 mydb

# 원격 서버에 생성
createdb -h remotehost -p 5432 -U postgres mydb

# 로케일 지정
createdb -l ko_KR.UTF-8 mydb

# 모든 옵션 함께 사용
createdb -h localhost -p 5432 -U postgres -O myuser -E UTF8 -T template0 mydb "Description"
```

### 7.4 SQL 동등 명령

```sql
-- createdb mydb와 동등
CREATE DATABASE mydb;

-- createdb -O myuser -E UTF8 mydb와 동등
CREATE DATABASE mydb
    OWNER = myuser
    ENCODING = 'UTF8';

-- 템플릿 지정
CREATE DATABASE mydb
    TEMPLATE = template0
    ENCODING = 'UTF8'
    LC_COLLATE = 'ko_KR.UTF-8'
    LC_CTYPE = 'ko_KR.UTF-8';
```

---

## 8. dropdb - 데이터베이스 삭제

dropdb는 기존 PostgreSQL 데이터베이스를 삭제하는 유틸리티입니다. SQL `DROP DATABASE` 명령의 래퍼입니다.

### 8.1 기본 구문 (Syntax)

```bash
dropdb [connection-option...] [option...] dbname
```

### 8.2 요구사항

실행 사용자는 데이터베이스 슈퍼유저이거나 데이터베이스 소유자여야 합니다.

### 8.3 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-e, --echo` | 서버로 전송되는 명령 에코 |
| `-f, --force` | 삭제 전 모든 기존 연결 종료 |
| `-i, --interactive` | 삭제 전 확인 프롬프트 |
| `--if-exists` | 데이터베이스가 없어도 오류 발생하지 않음 |
| `-V, --version` | 버전 정보 표시 |
| `-?, --help` | 도움말 표시 |
| `--maintenance-db=dbname` | 삭제 시 연결할 데이터베이스 (기본값: postgres 또는 template1) |

### 8.4 사용 예제

```bash
# 간단한 데이터베이스 삭제
dropdb mydb

# 확인 프롬프트와 함께 삭제
dropdb -i mydb

# 에코 및 원격 연결
dropdb -e -h remotehost -p 5432 -U postgres mydb

# 기존 연결 강제 종료 후 삭제
dropdb -f mydb

# 존재하지 않아도 오류 없음
dropdb --if-exists mydb
```

### 8.5 SQL 동등 명령

```sql
-- dropdb mydb와 동등
DROP DATABASE mydb;

-- dropdb -f mydb와 동등
DROP DATABASE mydb WITH (FORCE);

-- dropdb --if-exists mydb와 동등
DROP DATABASE IF EXISTS mydb;
```

### 8.6 주의사항

- 삭제된 데이터베이스는 복구할 수 없습니다. 삭제 전 백업을 확인하세요.
- 현재 연결된 데이터베이스는 삭제할 수 없습니다.
- `-f` 옵션은 활성 연결을 강제로 종료합니다.

---

## 9. createuser - 사용자 생성

createuser는 새 PostgreSQL 사용자 계정(역할)을 생성하는 유틸리티입니다. SQL `CREATE ROLE` 명령의 래퍼입니다.

### 9.1 기본 구문 (Syntax)

```bash
createuser [connection-option...] [option...] [username]
```

### 9.2 요구사항

- 슈퍼유저 또는 `CREATEROLE` 권한이 있는 사용자만 새 사용자를 생성할 수 있습니다.
- `SUPERUSER`, `REPLICATION`, `BYPASSRLS` 권한을 가진 사용자를 생성하려면 슈퍼유저로 연결해야 합니다.

### 9.3 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-d, --createdb` | 사용자가 데이터베이스를 생성할 수 있도록 허용 |
| `-D, --no-createdb` | 데이터베이스 생성 금지 (기본값) |
| `-s, --superuser` | 슈퍼유저 생성 |
| `-S, --no-superuser` | 일반 사용자 생성 (기본값) |
| `-r, --createrole` | 역할 생성 허용 |
| `-R, --no-createrole` | 역할 생성 금지 (기본값) |
| `-l, --login` | 로그인 허용 (기본값) |
| `-L, --no-login` | 로그인 금지 |
| `-P, --pwprompt` | 비밀번호 프롬프트 |
| `-c, --connection-limit=N` | 최대 연결 수 설정 |
| `-v, --valid-until=TIMESTAMP` | 비밀번호 만료 날짜 설정 |
| `-e, --echo` | 생성된 SQL 명령 에코 |
| `--replication` | 복제 권한 부여 |
| `--no-replication` | 복제 권한 제외 (기본값) |

### 9.4 사용 예제

```bash
# 기본 사용자 생성
createuser joe

# 비밀번호 프롬프트와 함께 슈퍼유저 생성
createuser -s -P admin

# 데이터베이스 생성 권한을 가진 사용자
createuser -d dbcreator

# 연결 제한이 있는 사용자
createuser -c 10 limited_user

# 로그인이 불가능한 역할 (그룹용)
createuser -L mygroup

# 비밀번호 만료일 설정
createuser -P -v "2025-12-31" temp_user

# 원격 서버에 사용자 생성
createuser -h remotehost -p 5432 -U postgres -P newuser

# 복제 권한이 있는 사용자
createuser --replication replicator
```

### 9.5 SQL 동등 명령

```sql
-- createuser joe와 동등
CREATE ROLE joe LOGIN;

-- createuser -s -P admin과 동등
CREATE ROLE admin SUPERUSER LOGIN PASSWORD 'password';

-- createuser -d dbcreator와 동등
CREATE ROLE dbcreator CREATEDB LOGIN;

-- createuser -L mygroup과 동등
CREATE ROLE mygroup NOLOGIN;

-- createuser --replication replicator와 동등
CREATE ROLE replicator REPLICATION LOGIN;
```

---

## 10. dropuser - 사용자 삭제

dropuser는 기존 PostgreSQL 사용자 계정을 삭제하는 유틸리티입니다. SQL `DROP ROLE` 명령의 래퍼입니다.

### 10.1 기본 구문 (Syntax)

```bash
dropuser [connection-option...] [option...] [username]
```

### 10.2 권한 요구사항

- 슈퍼유저는 모든 역할을 삭제할 수 있습니다.
- 슈퍼유저가 아닌 역할은 `CREATEROLE` 권한 또는 대상 역할에 대한 `ADMIN OPTION`을 가진 사용자만 삭제할 수 있습니다.

### 10.3 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-e, --echo` | 서버로 전송되는 명령 에코 |
| `-i, --interactive` | 삭제 전 확인 프롬프트 |
| `--if-exists` | 사용자가 없어도 오류 발생하지 않음 |
| `-V, --version` | 버전 정보 표시 |
| `-?, --help` | 도움말 표시 |

### 10.4 사용 예제

```bash
# 기본 사용자 삭제
dropuser joe

# 확인 프롬프트와 함께 삭제
dropuser -i joe

# 에코 및 원격 연결
dropuser -e -h remotehost -p 5432 -U postgres joe

# 존재하지 않아도 오류 없음
dropuser --if-exists joe
```

### 10.5 SQL 동등 명령

```sql
-- dropuser joe와 동등
DROP ROLE joe;

-- dropuser --if-exists joe와 동등
DROP ROLE IF EXISTS joe;
```

### 10.6 주의사항

- 객체를 소유한 사용자는 먼저 객체의 소유권을 변경하거나 객체를 삭제해야 삭제할 수 있습니다.
- 권한이 부여된 사용자는 먼저 권한을 취소해야 합니다.

```sql
-- 소유권 변경
REASSIGN OWNED BY joe TO postgres;

-- 소유한 객체 삭제
DROP OWNED BY joe;

-- 사용자 삭제
DROP ROLE joe;
```

---

## 11. vacuumdb - 가비지 컬렉션 및 분석

vacuumdb는 PostgreSQL 데이터베이스에서 가비지 컬렉션을 수행하고 쿼리 최적화를 위한 내부 통계를 생성하는 유틸리티입니다. SQL `VACUUM` 명령의 래퍼입니다.

### 11.1 기본 구문 (Syntax)

```bash
vacuumdb [connection-option...] [option...] [-t | --table table [(column [...])]] ... [dbname | -a | --all]
vacuumdb [connection-option...] [option...] [-n | --schema schema] ... [dbname | -a | --all]
vacuumdb [connection-option...] [option...] [-N | --exclude-schema schema] ... [dbname | -a | --all]
```

### 11.2 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-a, --all` | 모든 데이터베이스 처리 |
| `-d, --dbname=dbname` | 데이터베이스 이름 |
| `-z, --analyze` | 옵티마이저 통계 계산 |
| `-Z, --analyze-only` | 분석만 수행 (VACUUM 없음) |
| `-f, --full` | 전체 VACUUM 수행 |
| `-F, --freeze` | 튜플을 적극적으로 프리징 |
| `-t, --table=table` | 특정 테이블 처리 |
| `-n, --schema=schema` | 특정 스키마 처리 |
| `-N, --exclude-schema=schema` | 특정 스키마 제외 |
| `-j, --jobs=njobs` | 병렬로 VACUUM/ANALYZE 실행 |
| `-P, --parallel=workers` | VACUUM용 병렬 워커 수 |
| `-v, --verbose` | 상세 정보 출력 |
| `-q, --quiet` | 진행 메시지 억제 |
| `-e, --echo` | 명령 에코 |

### 11.3 사용 예제

```bash
# 단일 데이터베이스 정리
vacuumdb mydb

# 모든 데이터베이스 정리
vacuumdb --all

# 정리 및 분석
vacuumdb --analyze mydb

# 분석만 수행
vacuumdb --analyze-only mydb

# 전체 VACUUM
vacuumdb --full mydb

# 특정 테이블 처리
vacuumdb -t users mydb

# 특정 컬럼 분석
vacuumdb --analyze-only -t "users(name, email)" mydb

# 특정 스키마 처리
vacuumdb -n public mydb

# 병렬 처리
vacuumdb --jobs=4 mydb

# 여러 스키마 처리
vacuumdb -n foo -n bar mydb
```

### 11.4 SQL 동등 명령

```sql
-- vacuumdb mydb와 동등
VACUUM;

-- vacuumdb --analyze mydb와 동등
VACUUM ANALYZE;

-- vacuumdb --full mydb와 동등
VACUUM FULL;

-- vacuumdb -t users mydb와 동등
VACUUM users;

-- vacuumdb --analyze-only -t users mydb와 동등
ANALYZE users;
```

### 11.5 정기적인 유지보수

```bash
# 크론잡으로 야간 VACUUM ANALYZE
0 2 * * * vacuumdb --all --analyze --quiet

# 주간 전체 VACUUM (시스템 다운타임 필요)
0 4 * * 0 vacuumdb --all --full --quiet
```

---

## 12. reindexdb - 인덱스 재구축

reindexdb는 PostgreSQL 데이터베이스의 인덱스를 재구축하는 유틸리티입니다. SQL `REINDEX` 명령의 래퍼입니다.

### 12.1 기본 구문 (Syntax)

```bash
reindexdb [connection-option...] [option...] [dbname | -a | --all]
```

### 12.2 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-a, --all` | 모든 데이터베이스 재인덱스 |
| `-d, --dbname=dbname` | 재인덱스할 데이터베이스 |
| `-S, --schema=schema` | 특정 스키마 재인덱스 |
| `-t, --table=table` | 특정 테이블 재인덱스 |
| `-i, --index=index` | 특정 인덱스 재인덱스 |
| `-s, --system` | 시스템 카탈로그만 재인덱스 |
| `--concurrently` | 동시 읽기를 허용하며 재인덱스 |
| `-j, --jobs=njobs` | 병렬로 재인덱스 명령 실행 |
| `--tablespace=tablespace` | 특정 테이블스페이스에 인덱스 재구축 |
| `-e, --echo` | 생성된 SQL 명령 에코 |
| `-v, --verbose` | 상세 처리 정보 출력 |
| `-q, --quiet` | 진행 메시지 억제 |

### 12.3 사용 예제

```bash
# 단일 데이터베이스 재인덱스
reindexdb mydb

# 모든 데이터베이스 재인덱스
reindexdb --all

# 특정 테이블 재인덱스
reindexdb -t users mydb

# 특정 인덱스 재인덱스
reindexdb -i users_pkey mydb

# 특정 테이블과 인덱스 함께
reindexdb -t users -i orders_idx mydb

# 동시 접근 허용하며 재인덱스
reindexdb --concurrently mydb

# 병렬 재인덱스
reindexdb --all --jobs=4

# 시스템 카탈로그만 재인덱스
reindexdb --system mydb
```

### 12.4 SQL 동등 명령

```sql
-- reindexdb mydb와 동등
REINDEX DATABASE mydb;

-- reindexdb -t users mydb와 동등
REINDEX TABLE users;

-- reindexdb -i users_pkey mydb와 동등
REINDEX INDEX users_pkey;

-- reindexdb --concurrently mydb와 동등
REINDEX DATABASE CONCURRENTLY mydb;

-- reindexdb --system mydb와 동등
REINDEX SYSTEM mydb;
```

### 12.5 중요 참고사항

- `-j/--jobs` 옵션은 `--system`과 호환되지 않습니다.
- 병렬 작업에는 충분한 `max_connections` 설정이 필요합니다.
- `--concurrently` 옵션은 잠금 없이 재인덱스하지만 더 많은 리소스를 사용합니다.

---

## 13. clusterdb - 테이블 클러스터링

clusterdb는 PostgreSQL 데이터베이스의 테이블을 재클러스터링하는 유틸리티입니다. 이전에 클러스터링된 테이블을 찾아 마지막으로 사용된 인덱스로 다시 클러스터링합니다. SQL `CLUSTER` 명령의 래퍼입니다.

### 13.1 기본 구문 (Syntax)

```bash
clusterdb [connection-option...] [option...] [--table | -t table] ... [dbname | -a | --all]
```

### 13.2 주요 옵션

| 옵션 | 설명 |
|------|------|
| `-a, --all` | 모든 데이터베이스 클러스터링 |
| `-d dbname, --dbname=dbname` | 클러스터링할 데이터베이스 |
| `-t table, --table=table` | 특정 테이블만 클러스터링 |
| `-e, --echo` | 생성된 SQL 명령 에코 |
| `-q, --quiet` | 진행 메시지 억제 |
| `-v, --verbose` | 상세 정보 출력 |
| `-V, --version` | 버전 표시 후 종료 |
| `-?, --help` | 도움말 표시 |
| `--maintenance-db=dbname` | 모든 데이터베이스 클러스터링 시 사용할 데이터베이스 |

### 13.3 사용 예제

```bash
# 데이터베이스의 모든 클러스터링된 테이블 재클러스터링
clusterdb mydb

# 특정 테이블만 클러스터링
clusterdb -t users mydb

# 모든 데이터베이스 클러스터링
clusterdb --all

# 상세 정보와 함께
clusterdb -v mydb
```

### 13.4 SQL 동등 명령

```sql
-- clusterdb mydb와 동등
CLUSTER;

-- clusterdb -t users mydb와 동등
CLUSTER users;

-- 인덱스 지정 클러스터링
CLUSTER users USING users_pkey;
```

### 13.5 참고사항

- 클러스터링은 테이블을 인덱스 순서대로 물리적으로 재정렬합니다.
- 범위 쿼리 성능을 크게 향상시킬 수 있습니다.
- 클러스터링 중에는 테이블에 배타적 잠금이 설정됩니다.
- 클러스터링되지 않은 테이블은 영향을 받지 않습니다.

---

## 14. 기타 유틸리티

### 14.1 pg_isready - 서버 연결 상태 확인

서버의 연결 상태를 확인하는 유틸리티입니다.

```bash
# 기본 연결 확인
pg_isready

# 특정 호스트 확인
pg_isready -h localhost -p 5432

# 출력 형식 지정
pg_isready -h localhost -p 5432 -d mydb -U postgres
```

종료 코드:
- `0`: 서버가 연결을 수락 중
- `1`: 서버가 연결을 거부 중
- `2`: 응답 없음
- `3`: 연결 시도 없음 (잘못된 매개변수)

### 14.2 pgbench - 벤치마크 테스트

PostgreSQL에서 벤치마크 테스트를 실행하는 유틸리티입니다.

```bash
# 벤치마크 데이터베이스 초기화
pgbench -i -s 10 mydb

# 기본 벤치마크 실행
pgbench -c 10 -j 2 -t 1000 mydb

# 60초 동안 실행
pgbench -c 10 -j 2 -T 60 mydb

# 사용자 정의 스크립트 사용
pgbench -f custom_script.sql mydb
```

주요 옵션:
- `-i`: 초기화 모드
- `-s scale`: 스케일 팩터
- `-c clients`: 클라이언트 수
- `-j threads`: 스레드 수
- `-t transactions`: 트랜잭션 수
- `-T seconds`: 실행 시간 (초)
- `-f script`: 사용자 정의 스크립트 파일

### 14.3 pg_config - 설치 정보 조회

설치된 PostgreSQL 버전 정보를 조회합니다.

```bash
# 모든 정보 표시
pg_config

# 특정 정보만 표시
pg_config --bindir
pg_config --includedir
pg_config --libdir
pg_config --version
```

### 14.4 pg_receivewal - WAL 스트리밍

서버에서 WAL을 스트리밍하여 저장합니다.

```bash
# WAL 수신 및 저장
pg_receivewal -D /path/to/wal_archive -h primary_host

# 복제 슬롯 사용
pg_receivewal -D /path/to/wal_archive -S myslot -h primary_host
```

### 14.5 pg_verifybackup - 백업 무결성 검증

pg_basebackup으로 생성된 백업의 무결성을 검증합니다.

```bash
# 백업 검증
pg_verifybackup /path/to/backup

# 상세 출력
pg_verifybackup -e /path/to/backup
```

### 14.6 pg_amcheck - 데이터베이스 손상 검사

데이터베이스의 손상을 검사합니다.

```bash
# 전체 데이터베이스 검사
pg_amcheck mydb

# 특정 테이블 검사
pg_amcheck -t users mydb

# 상세 출력
pg_amcheck -v mydb
```

---

## 요약

PostgreSQL 클라이언트 애플리케이션은 데이터베이스 관리, 백업/복원, 성능 테스트 등 다양한 작업을 수행할 수 있는 강력한 도구들입니다.

### 자주 사용되는 명령 요약

| 작업 | 명령 |
|------|------|
| 대화형 SQL 실행 | `psql -d dbname` |
| 데이터베이스 백업 | `pg_dump -Fc dbname > backup.dump` |
| 데이터베이스 복원 | `pg_restore -d dbname backup.dump` |
| 전체 클러스터 백업 | `pg_dumpall > full_backup.sql` |
| 기본 백업 | `pg_basebackup -D /backup -P` |
| 데이터베이스 생성 | `createdb dbname` |
| 데이터베이스 삭제 | `dropdb dbname` |
| 사용자 생성 | `createuser username` |
| 사용자 삭제 | `dropuser username` |
| 가비지 컬렉션 | `vacuumdb --analyze dbname` |
| 인덱스 재구축 | `reindexdb dbname` |
| 테이블 클러스터링 | `clusterdb dbname` |

### 모범 사례

1. 정기 백업: pg_dump 또는 pg_basebackup을 사용하여 정기적으로 백업
2. 유지보수: vacuumdb와 reindexdb를 정기적으로 실행
3. 모니터링: pg_isready로 서버 상태 확인
4. 보안: 비밀번호 파일(.pgpass) 사용, 환경 변수에 비밀번호 저장 지양
5. 테스트: pgbench로 성능 테스트 수행

---

## 참고 자료

- [PostgreSQL 공식 문서 - Client Applications](https://www.postgresql.org/docs/current/reference-client.html)
- [PostgreSQL 공식 문서 - psql](https://www.postgresql.org/docs/current/app-psql.html)
- [PostgreSQL 공식 문서 - pg_dump](https://www.postgresql.org/docs/current/app-pgdump.html)
- [PostgreSQL 공식 문서 - pg_restore](https://www.postgresql.org/docs/current/app-pgrestore.html)
- [PostgreSQL 공식 문서 - pg_basebackup](https://www.postgresql.org/docs/current/app-pgbasebackup.html)
