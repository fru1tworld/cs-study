# PostgreSQL 문서 색인 (Document Index)

PostgreSQL 18 공식 문서 번역 색인입니다. 이 색인을 통해 원하는 주제를 빠르게 찾을 수 있습니다.

---

## 문서 구성 개요

| 분류 | 문서 번호 | 내용 |
|------|----------|------|
| 기초 | 01-05 | SQL 기본, 데이터 정의/조작, 쿼리 |
| 데이터 타입 및 함수 | 06-08 | 데이터 타입, 함수/연산자, 타입 변환 |
| 인덱스 및 검색 | 09-10 | 인덱스, 전문 검색 |
| 동시성 및 성능 | 11-13 | 동시성 제어, 성능 튜닝, 병렬 쿼리 |
| 서버 관리 | 14-17 | 서버 설정, 인증, 역할, DB 관리 |
| 백업 및 복제 | 18-19, 24 | 백업/복원, 고가용성, 논리적 복제 |
| 유지보수 | 20-23 | 지역화, 유지보수, 모니터링, WAL |
| 클라이언트 인터페이스 | 25-28 | libpq, 대용량 객체, ECPG, 정보 스키마 |
| 확장성 | 29-40 | SQL 확장, 트리거, 절차적 언어 등 |
| 인덱스 내부 | 47-52 | 인덱스 접근 메서드, GiST, SP-GiST, GIN, BRIN, Hash |
| 내부 구조 | 53-62 | 저장소, 시스템 카탈로그, 프로토콜, 소스 코드 |
| 참조 | 63-66 | B-Tree, contrib 모듈, SQL 명령어, 클라이언트 앱 |

---

## I. 기초 (Tutorial & SQL Basics)

### [01. 튜토리얼 (Tutorial)](./01_tutorial.md)
PostgreSQL 입문을 위한 기본 가이드입니다.
- PostgreSQL 소개 및 아키텍처 개념
- SQL 언어 기초
- 고급 기능 개요

### [02. SQL 구문 (SQL Syntax)](./02_sql_syntax.md)
SQL 구문의 기본 규칙을 다룹니다.
- 어휘 구조 (Lexical Structure)
- 값 표현식 (Value Expressions)
- 함수 호출 방법

### [03. 데이터 정의 (Data Definition)](./03_data_definition.md)
데이터베이스 객체를 정의하는 방법입니다.
- 테이블 생성 및 수정
- 제약 조건 (PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK)
- 스키마 및 상속
- 테이블 파티셔닝

### [04. 데이터 조작 (Data Manipulation)](./04_data_manipulation.md)
데이터를 삽입, 수정, 삭제하는 방법입니다.
- INSERT 문
- UPDATE 문
- DELETE 문
- RETURNING 절

### [05. 쿼리 (Queries)](./05_queries.md)
SELECT 문을 활용한 데이터 조회입니다.
- SELECT 기본 구문
- 테이블 표현식 (FROM 절, JOIN)
- 집합 연산 (UNION, INTERSECT, EXCEPT)
- WITH 쿼리 (CTE)
- LIMIT/OFFSET

---

## II. 데이터 타입 및 함수 (Data Types & Functions)

### [06. 데이터 타입 (Data Types)](./06_data_types.md)
PostgreSQL에서 지원하는 데이터 타입입니다.
- 숫자 타입 (integer, numeric, float 등)
- 문자 타입 (char, varchar, text)
- 날짜/시간 타입
- 불리언, 열거형
- 기하학 타입, 네트워크 타입
- 배열, JSON/JSONB, XML
- 범위 타입, 복합 타입

### [07. 함수와 연산자 (Functions and Operators)](./07_functions_operators.md)
내장 함수 및 연산자 레퍼런스입니다.
- 논리, 비교, 수학 연산자
- 문자열 함수
- 패턴 매칭 (LIKE, 정규식)
- 날짜/시간 함수
- 배열, JSON 함수
- 집계 함수, 윈도우 함수
- 서브쿼리 표현식

### [08. 타입 변환 (Type Conversion)](./08_type_conversion.md)
데이터 타입 간 변환 규칙입니다.
- 암시적/명시적 타입 변환
- 연산자 타입 해석
- 함수 타입 해석
- 값 저장 시 타입 변환
- UNION/CASE 구문의 타입 해결

---

## III. 인덱스 및 검색 (Indexes & Search)

### [09. 인덱스 (Indexes)](./09_indexes.md)
데이터베이스 성능 최적화를 위한 인덱스입니다.
- B-Tree, Hash, GiST, SP-GiST, GIN, BRIN 인덱스
- 다중 컬럼 인덱스
- 표현식 인덱스
- 부분 인덱스
- 커버링 인덱스 (INCLUDE)
- 인덱스 전용 스캔

### [10. 전문 검색 (Full Text Search)](./10_full_text_search.md)
텍스트 검색 기능입니다.
- 문서와 쿼리 (tsvector, tsquery)
- 텍스트 검색 연산자
- 검색 결과 순위 지정
- 하이라이팅
- GIN/GiST 인덱스 활용

---

## IV. 동시성 및 성능 (Concurrency & Performance)

### [11. 동시성 제어 (Concurrency Control)](./11_concurrency.md)
다중 사용자 환경에서의 동시성 관리입니다.
- MVCC (다중 버전 동시성 제어)
- 트랜잭션 격리 수준
  - Read Committed (기본값)
  - Repeatable Read
  - Serializable
- 명시적 잠금 (테이블/행 수준)
- 권고 잠금 (Advisory Locks)
- 교착 상태 (Deadlock)

### [12. 성능 팁 (Performance Tips)](./12_performance.md)
쿼리 성능 최적화 방법입니다.
- EXPLAIN 사용법
  - EXPLAIN 기초
  - EXPLAIN ANALYZE
- 플래너 통계
- 명시적 JOIN 절로 플래너 제어
- 데이터베이스 채우기 최적화
- 비영속적 설정

### [13. 병렬 쿼리 (Parallel Query)](./13_parallel_query.md)
다중 CPU를 활용한 병렬 처리입니다.
- 병렬 쿼리 작동 방식
- Gather/Gather Merge 노드
- 병렬 스캔, 병렬 조인, 병렬 집계
- 병렬 안전성 (Parallel Safety)
- 관련 설정 파라미터

---

## V. 서버 관리 (Server Administration)

### [14. 서버 설정 (Server Configuration)](./14_server_config.md)
PostgreSQL 서버 설정 파라미터입니다.
- postgresql.conf 파일
- 파라미터 타입 (Boolean, String, Numeric 등)
- 연결 및 인증 설정
- 리소스 소비 (메모리, 디스크)
- Write Ahead Log (WAL)
- 복제 설정
- 쿼리 계획 설정
- 오류 보고 및 로깅

### [15. 클라이언트 인증 (Client Authentication)](./15_client_auth.md)
클라이언트 연결 인증 방법입니다.
- pg_hba.conf 파일
- 인증 방법
  - Trust, Password (md5, scram-sha-256)
  - GSSAPI, SSPI
  - Ident, Peer
  - LDAP, RADIUS
  - 인증서 인증
  - PAM, OAuth
- 사용자 이름 맵

### [16. 데이터베이스 역할 (Database Roles)](./16_database_roles.md)
사용자 및 권한 관리입니다.
- 역할 생성/삭제
- 역할 속성 (LOGIN, SUPERUSER, CREATEDB 등)
- 역할 멤버십
- 사전 정의된 역할
- 함수 보안 (SECURITY DEFINER)

### [17. 데이터베이스 관리 (Managing Databases)](./17_managing_databases.md)
데이터베이스 생성 및 관리입니다.
- 데이터베이스 생성/삭제
- 템플릿 데이터베이스 (template0, template1)
- 데이터베이스 구성
- 테이블스페이스

---

## VI. 백업 및 복제 (Backup & Replication)

### [18. 백업 및 복원 (Backup and Restore)](./18_backup_restore.md)
데이터 백업 및 복구 방법입니다.
- SQL 덤프 (pg_dump, pg_dumpall)
- 파일 시스템 레벨 백업
- 연속 아카이빙 (WAL 아카이빙)
- Point-in-Time Recovery (PITR)

### [19. 고가용성, 로드 밸런싱, 복제 (High Availability)](./19_high_availability.md)
서버 가용성 및 복제 구성입니다.
- 솔루션 비교 (공유 디스크, 파일 시스템 복제, WAL 배송)
- 로그 배송 스탠바이 서버
- 스트리밍 복제
- 핫 스탠바이 (읽기 전용 쿼리)
- 동기/비동기 복제

### [24. 논리적 복제 (Logical Replication)](./24_logical_replication.md)
테이블 단위 논리적 복제입니다.
- 발행 (Publication)
- 구독 (Subscription)
- 행 필터, 컬럼 목록
- 충돌 처리
- 장애 조치
- 모니터링

---

## VII. 유지보수 (Maintenance)

### [20. 지역화 (Localization)](./20_localization.md)
다국어 및 문자셋 지원입니다.
- 로케일 지원 (LC_COLLATE, LC_CTYPE 등)
- 콜레이션 (Collation)
- 문자 집합 (Character Set) 지원
- 인코딩 변환

### [21. 정기적 유지보수 (Routine Maintenance)](./21_maintenance.md)
데이터베이스 유지보수 작업입니다.
- VACUUM
  - 디스크 공간 회수
  - 플래너 통계 업데이트
  - 가시성 맵 업데이트
  - 트랜잭션 ID Wraparound 방지
- Autovacuum 데몬
- 재인덱싱 (REINDEX)
- 로그 파일 유지보수

### [22. 모니터링 (Monitoring)](./22_monitoring.md)
데이터베이스 활동 모니터링입니다.
- 표준 유닉스 도구
- 누적 통계 시스템
  - pg_stat_activity
  - pg_stat_replication
  - pg_stat_database
  - pg_stat_user_tables
- 잠금 보기 (pg_locks)
- 진행 상황 보고
- 동적 추적

### [23. WAL (Write-Ahead Log)](./23_wal.md)
WAL의 안정성 및 구성입니다.
- 안정성 원칙
- 데이터 체크섬
- WAL 구성
- 비동기 커밋
- WAL 내부 구조

---

## VIII. 클라이언트 인터페이스 (Client Interfaces)

### [25. libpq - C 라이브러리](./25_libpq.md)
PostgreSQL C API입니다.
- 연결 제어 함수
- 명령 실행 함수
- 비동기 명령 처리
- 커서를 사용한 결과 검색
- 대용량 객체 함수

### [26. 대용량 객체 (Large Objects)](./26_large_objects.md)
대용량 바이너리 데이터 처리입니다.
- 대용량 객체 소개
- 클라이언트 인터페이스
- 서버측 함수 (lo_create, lo_import, lo_export 등)

### [27. ECPG - 내장 SQL](./27_ecpg.md)
C 언어 내장 SQL 프로그래밍입니다.
- ECPG 개념
- 데이터베이스 연결 관리
- SQL 명령 실행
- 호스트 변수
- 동적 SQL
- 오류 처리

### [28. 정보 스키마 (Information Schema)](./28_information_schema.md)
SQL 표준 메타데이터 뷰입니다.
- 정보 스키마 개요
- 주요 뷰
  - tables, columns
  - table_constraints
  - routines
  - views

---

## IX. 확장성 (Extensibility)

### [29. SQL 확장하기 (Extending SQL)](./29_extending_sql.md)
PostgreSQL 확장 방법입니다.
- 확장성 작동 원리
- PostgreSQL 타입 시스템
- 사용자 정의 함수
- 사용자 정의 타입
- 사용자 정의 연산자
- 확장 (Extensions)

### [30. 트리거 (Triggers)](./30_triggers.md)
자동 실행 함수입니다.
- 트리거 개요 및 용도
- 트리거 동작 (BEFORE/AFTER, ROW/STATEMENT)
- 트리거 함수 작성
- 데이터 변경의 가시성

### [31. 이벤트 트리거 (Event Triggers)](./31_event_triggers.md)
DDL 이벤트 트리거입니다.
- 이벤트 종류 (ddl_command_start, ddl_command_end 등)
- 이벤트 트리거 함수
- DDL 명령 캡처

### [32. 규칙 시스템 (Rule System)](./32_rule_system.md)
쿼리 재작성 규칙입니다.
- 쿼리 트리 구조
- 뷰와 규칙 시스템
- INSERT/UPDATE/DELETE 규칙
- 규칙 vs 트리거

### [33. 절차적 언어 (Procedural Languages)](./33_procedural_languages.md)
절차적 언어 개요입니다.
- 절차적 언어란?
- 핸들러 작동 방식
- 신뢰/비신뢰 언어
- 언어 설치

### [34. PL/pgSQL](./34_plpgsql.md)
PostgreSQL 기본 절차적 언어입니다.
- 변수 및 상수 선언
- 제어 구조 (IF, CASE, LOOP)
- 커서 사용
- 오류 및 예외 처리
- 트리거 함수
- 트랜잭션 제어

### [35. PL/Tcl](./35_pltcl.md)
Tcl 절차적 언어입니다.
- PL/Tcl 함수와 인수
- 데이터베이스 접근
- 트리거 함수
- 오류 처리

### [36. PL/Perl](./36_plperl.md)
Perl 절차적 언어입니다.
- PL/Perl 함수
- 내장 함수
- 신뢰/비신뢰 PL/Perl
- 트리거

### [37. PL/Python](./37_plpython.md)
Python 절차적 언어입니다.
- PL/Python 함수
- 데이터 타입 매핑
- 데이터베이스 접근
- 트리거 함수
- 트랜잭션 제어

### [38. SPI (Server Programming Interface)](./38_spi.md)
서버 프로그래밍 인터페이스입니다.
- 연결 관리
- 쿼리 실행
- 준비된 구문
- 커서 관리
- 메모리 관리

### [39. 백그라운드 워커 (Background Workers)](./39_background_workers.md)
백그라운드 프로세스 확장입니다.
- 백그라운드 워커 등록
- 시그널 처리
- 공유 메모리 접근

### [40. 논리적 디코딩 (Logical Decoding)](./40_logical_decoding.md)
변경 데이터 캡처입니다.
- 논리적 디코딩 개념
- 복제 슬롯
- 출력 플러그인

---

## X. 고급 확장 기능 (Advanced Extension Features)

### [41. 복제 진행 추적 (Replication Progress)](./41_replication_progress.md)
복제 원본 및 진행 상태 추적입니다.

### [42. 아카이브 모듈 (Archive Modules)](./42_archive_modules.md)
커스텀 WAL 아카이브 모듈입니다.

### [43. 외래 데이터 래퍼 (FDW)](./43_fdw.md)
외래 테이블 접근 래퍼 작성입니다.

### [44. 테이블 샘플링 메서드 (Table Sampling)](./44_tablesample.md)
커스텀 샘플링 메서드입니다.

### [45. 커스텀 스캔 프로바이더 (Custom Scan)](./45_custom_scan.md)
커스텀 스캔 방법 구현입니다.

### [46. 일반 WAL 레코드 (Generic WAL)](./46_generic_wal.md)
확장을 위한 일반 WAL 레코드입니다.

---

## XI. 인덱스 내부 구조 (Index Internals)

### [47. 인덱스 접근 메서드 (Index Access Method)](./47_index_am.md)
새로운 인덱스 유형 개발을 위한 인터페이스입니다.
- 기본 API 구조
- 인덱스 스캔
- 인덱스 잠금 고려사항

### [48. GiST 인덱스](./48_gist.md)
일반화 검색 트리입니다.
- GiST 소개 및 확장성
- 내장 연산자 클래스
- 구현 세부사항

### [49. SP-GiST 인덱스](./49_spgist.md)
공간 분할 일반화 검색 트리입니다.
- 쿼드 트리, k-d 트리, 기수 트리
- 확장성

### [50. GIN 인덱스](./50_gin.md)
일반화 역 인덱스입니다.
- 복합 값 인덱싱
- 전문 검색, 배열 검색
- 성능 팁

### [51. BRIN 인덱스](./51_brin.md)
블록 범위 인덱스입니다.
- 블록 범위 개념
- 연산자 클래스 (Minmax, Inclusion, Bloom)

### [52. 해시 인덱스](./52_hash.md)
해시 인덱스 내부입니다.
- 해시 코드 저장
- 동등 비교 전용

---

## XII. 내부 구조 (Internals)

### [53. 데이터베이스 물리적 저장소 (Storage)](./53_storage.md)
물리적 저장소 구조입니다.
- 파일 레이아웃
- TOAST
- 프리 스페이스 맵
- 가시성 맵
- 페이지 레이아웃
- HOT (Heap-Only Tuples)

### [54. 시스템 카탈로그 (System Catalogs)](./54_system_catalogs.md)
시스템 메타데이터 테이블입니다.
- pg_class, pg_attribute
- pg_type, pg_proc
- pg_namespace, pg_index
- pg_constraint, pg_database

### [55. 시스템 뷰 (System Views)](./55_system_views.md)
시스템 상태 뷰입니다.
- pg_stat_activity
- pg_locks
- pg_settings
- pg_tables, pg_indexes

### [56. Frontend/Backend 프로토콜](./56_protocol.md)
클라이언트-서버 통신 프로토콜입니다.
- 메시지 흐름
- 메시지 형식
- 확장 쿼리 프로토콜
- SASL 인증
- 복제 프로토콜

### [57. PostgreSQL 소스 코드](./57_source_code.md)
소스 코드 구조입니다.
- 디렉토리 구조
- backend, bin, common 등
- 빌드 시스템

### [58. 네이티브 언어 지원 (NLS)](./58_nls.md)
메시지 번역 시스템입니다.
- GNU gettext 기반
- 번역자/프로그래머 가이드

### [59. 플래너 통계 활용 (Planner Statistics)](./59_planner_stats.md)
쿼리 플래너의 통계 활용입니다.
- 단일/확장 통계
- 행 추정 예제

### [60. 유전 쿼리 최적화기 (GEQO)](./60_geqo.md)
복잡한 조인 최적화입니다.
- 유전 알고리즘 기반
- 설정 파라미터

### [61. 테이블 접근 메서드 (Table AM)](./61_table_am.md)
테이블 저장 방법 인터페이스입니다.

### [62. WAL 내부 구현 (WAL Internals)](./62_wal_internals.md)
WAL 시스템 내부입니다.
- WAL 기본 원리
- 레코드 구조

---

## XIII. 참조 (Reference)

### [63. B-Tree 인덱스](./63_btree.md)
B-Tree 인덱스 상세입니다.
- 연산자 클래스 동작
- 지원 함수
- 구현 세부사항

### [64. 추가 제공 모듈 (Contrib)](./64_contrib.md)
contrib 확장 모듈입니다.
- pg_stat_statements
- postgres_fdw
- pg_trgm, hstore, ltree

### [65. SQL 명령어 참조 (SQL Commands)](./65_sql_commands.md)
SQL 명령어 레퍼런스입니다.
- DML (SELECT, INSERT, UPDATE, DELETE)
- DDL (CREATE, ALTER, DROP)
- DCL (GRANT, REVOKE)
- TCL (BEGIN, COMMIT, ROLLBACK)

### [66. 클라이언트 애플리케이션 (Client Apps)](./66_client_apps.md)
클라이언트 도구입니다.
- psql (대화형 터미널)
- pg_dump, pg_restore
- pg_dumpall
- pg_basebackup
- createdb, dropdb
- createuser, dropuser
- vacuumdb, reindexdb

---

## 주제별 빠른 참조

### 초보자용
1. [01. 튜토리얼](./01_tutorial.md) - PostgreSQL 시작하기
2. [02. SQL 구문](./02_sql_syntax.md) - SQL 기본 문법
3. [03. 데이터 정의](./03_data_definition.md) - 테이블 생성
4. [04. 데이터 조작](./04_data_manipulation.md) - 데이터 CRUD
5. [05. 쿼리](./05_queries.md) - SELECT 문

### 성능 최적화
1. [09. 인덱스](./09_indexes.md) - 인덱스 활용
2. [12. 성능 팁](./12_performance.md) - EXPLAIN 및 튜닝
3. [13. 병렬 쿼리](./13_parallel_query.md) - 병렬 처리
4. [21. 유지보수](./21_maintenance.md) - VACUUM, ANALYZE

### 운영/관리
1. [14. 서버 설정](./14_server_config.md) - 설정 파라미터
2. [15. 클라이언트 인증](./15_client_auth.md) - 인증 설정
3. [16. 역할](./16_database_roles.md) - 권한 관리
4. [18. 백업/복원](./18_backup_restore.md) - 백업 전략
5. [22. 모니터링](./22_monitoring.md) - 상태 모니터링

### 고가용성/복제
1. [19. 고가용성](./19_high_availability.md) - HA 구성
2. [24. 논리적 복제](./24_logical_replication.md) - 논리적 복제
3. [23. WAL](./23_wal.md) - WAL 이해

### 개발자용
1. [06. 데이터 타입](./06_data_types.md) - 타입 선택
2. [07. 함수와 연산자](./07_functions_operators.md) - 내장 함수
3. [34. PL/pgSQL](./34_plpgsql.md) - 저장 프로시저
4. [30. 트리거](./30_triggers.md) - 자동화

### 확장 개발자용
1. [29. SQL 확장](./29_extending_sql.md) - 확장 개발
2. [47. 인덱스 접근 메서드](./47_index_am.md) - 커스텀 인덱스
3. [43. FDW](./43_fdw.md) - 외래 데이터 래퍼

---

## 문서 정보

- 원본: PostgreSQL 18 공식 문서
- 형식: 한국어 번역
- 최종 업데이트: 2026-01-15

---

## 참고 자료

- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/18/)
- [PostgreSQL 위키](https://wiki.postgresql.org/)
