# Amazon RDS for PostgreSQL

## 개요

Amazon RDS for PostgreSQL은 완전 관리형 PostgreSQL 데이터베이스 서비스입니다. DB 인스턴스, 스냅샷, 특정 시점 복구, 자동 백업을 지원하며, Multi-AZ 배포와 읽기 복제본을 통해 고가용성을 제공합니다.

## 지원 버전

| 버전 | 지원 상태 |
|-----|----------|
| PostgreSQL 16 | ✅ 최신 |
| PostgreSQL 15 | ✅ 권장 |
| PostgreSQL 14 | ✅ 지원 |
| PostgreSQL 13 | ✅ 지원 |
| PostgreSQL 12 | ✅ 지원 (EOL 예정) |

**버전 확인**:
```bash
aws rds describe-db-engine-versions --engine postgres
```

## 주요 기능

### RDS 관리 기능
- ✅ 자동 백업 및 스냅샷
- ✅ 자동 마이너 버전 업그레이드
- ✅ Multi-AZ 배포
- ✅ 읽기 전용 복제본 (최대 15개)
- ✅ 스토리지 자동 확장

### PostgreSQL 고유 기능
- ✅ 풍부한 데이터 타입 (JSON, Array, hstore 등)
- ✅ 전체 텍스트 검색
- ✅ 파티셔닝
- ✅ 논리 복제
- ✅ 확장(Extensions) 지원

## 연결

### 기본 포트
- **5432** (기본값)

### 연결 문자열
```bash
psql -h <endpoint> -p 5432 -U <username> -d <database>
```

### 애플리케이션 연결 (JDBC)
```
jdbc:postgresql://<endpoint>:5432/<database>?ssl=true&sslmode=verify-full
```

### SSL 연결
```bash
psql "host=mydb.xxx.rds.amazonaws.com \
      port=5432 \
      dbname=mydb \
      user=admin \
      sslmode=verify-full \
      sslrootcert=global-bundle.pem"
```

## 확장(Extensions)

### 주요 지원 확장

| 확장 | 용도 |
|-----|------|
| `pg_stat_statements` | 쿼리 통계 |
| `PostGIS` | 지리공간 데이터 |
| `pgcrypto` | 암호화 함수 |
| `pg_trgm` | 유사성 검색 |
| `hstore` | 키-값 저장 |
| `uuid-ossp` | UUID 생성 |

### 확장 설치
```sql
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
CREATE EXTENSION IF NOT EXISTS postgis;
```

### 사용 가능한 확장 확인
```sql
SELECT * FROM pg_available_extensions ORDER BY name;
```

## 주요 파라미터

### 성능 관련

| 파라미터 | 설명 | 권장 값 |
|---------|------|--------|
| `shared_buffers` | 공유 버퍼 | 메모리의 25% |
| `effective_cache_size` | 예상 캐시 크기 | 메모리의 75% |
| `work_mem` | 작업 메모리 | 쿼리에 따라 |
| `maintenance_work_mem` | 유지보수 메모리 | 512MB-2GB |
| `random_page_cost` | 랜덤 I/O 비용 | SSD: 1.1-1.5 |

### 로깅 관련

| 파라미터 | 설명 |
|---------|------|
| `log_statement` | 로깅할 SQL 유형 |
| `log_min_duration_statement` | 슬로우 쿼리 기준 (ms) |
| `log_connections` | 연결 로깅 |
| `log_disconnections` | 연결 해제 로깅 |

### SSL 강제
```
rds.force_ssl = 1
```

## 복제

### 읽기 복제본
- 스트리밍 복제 사용
- 비동기식 복제
- 최대 15개 복제본

```bash
aws rds create-db-instance-read-replica \
    --db-instance-identifier mydb-replica \
    --source-db-instance-identifier mydb
```

### 논리 복제
PostgreSQL 10+에서 논리 복제 지원:
```sql
-- 퍼블리케이션 생성 (소스)
CREATE PUBLICATION my_publication FOR TABLE my_table;

-- 구독 생성 (대상)
CREATE SUBSCRIPTION my_subscription
    CONNECTION 'host=source port=5432 dbname=mydb'
    PUBLICATION my_publication;
```

## 데이터 마이그레이션

### pg_dump 사용
```bash
# 내보내기
pg_dump -h source-host -U user -d database -F c -f backup.dump

# 가져오기
pg_restore -h rds-endpoint -U admin -d database backup.dump
```

### AWS DMS 사용
- 전체 로드 + CDC 지원
- 이기종 마이그레이션 가능

## S3 통합

### S3로 데이터 내보내기
```sql
SELECT aws_s3.query_export_to_s3(
    'SELECT * FROM my_table',
    aws_commons.create_s3_uri('my-bucket', 'export/data.csv', 'us-east-1'),
    options :='format csv, header true'
);
```

### S3에서 데이터 가져오기
```sql
SELECT aws_s3.table_import_from_s3(
    'my_table',
    '',
    '(format csv, header true)',
    aws_commons.create_s3_uri('my-bucket', 'import/data.csv', 'us-east-1')
);
```

## Lambda 함수 호출

```sql
SELECT * FROM aws_lambda.invoke(
    aws_commons.create_lambda_function_arn('my-function', 'us-east-1'),
    '{"key": "value"}'::json
);
```

## IAM 데이터베이스 인증

### 활성화
```bash
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --enable-iam-database-authentication
```

### 사용자 생성
```sql
CREATE USER my_iam_user WITH LOGIN;
GRANT rds_iam TO my_iam_user;
```

### 연결
```bash
PGPASSWORD=$(aws rds generate-db-auth-token \
    --hostname mydb.xxx.rds.amazonaws.com \
    --port 5432 \
    --username my_iam_user) \
psql -h mydb.xxx.rds.amazonaws.com -U my_iam_user -d mydb
```

## 제한사항

### RDS 특유 제한
- ❌ 슈퍼유저 권한 없음
- ❌ 호스트 직접 접근 불가
- ❌ 일부 확장 설치 제한
- ❌ 물리적 복제 슬롯 직접 관리 불가

### 대안
| 제한 | 대안 |
|-----|-----|
| 슈퍼유저 | rds_superuser 역할 사용 |
| 확장 | 지원되는 확장 목록 확인 |

## 규정 준수

- ✅ HIPAA 적격
- ✅ PCI DSS 준수
- ✅ SOC 1, 2, 3
- ✅ FedRAMP (GovCloud)

## 모범 사례

### 성능
```
✅ 적절한 shared_buffers 설정
✅ 쿼리 실행 계획 분석 (EXPLAIN ANALYZE)
✅ 정기적인 VACUUM 및 ANALYZE
✅ 적절한 인덱스 설계
```

### 보안
```
✅ SSL 연결 강제 (rds.force_ssl = 1)
✅ IAM 데이터베이스 인증 사용
✅ 저장 데이터 암호화 활성화
✅ 최소 권한 원칙 적용
```

### 모니터링
```
✅ pg_stat_statements 확장 활성화
✅ 슬로우 쿼리 로깅
✅ Performance Insights 활용
```

## 관련 문서

- [DB 인스턴스 생성](../../03-db-instances/creating-instance.md)
- [파라미터 그룹](../../10-management/parameter-groups.md)
- [읽기 복제본](../../07-high-availability/read-replicas.md)
- [IAM 인증](../../05-security/iam-authentication.md)
