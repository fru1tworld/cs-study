# Amazon RDS for MySQL

## 개요

Amazon RDS for MySQL은 완전 관리형 MySQL 데이터베이스 서비스입니다. MySQL의 기능을 활용하면서 하드웨어 프로비저닝, 데이터베이스 설정, 패치 및 백업 등의 관리 작업을 AWS가 처리합니다.

## 지원 버전

| 버전 | 지원 상태 |
|-----|----------|
| MySQL 8.0 | ✅ 현재 권장 |
| MySQL 5.7 | ✅ 지원 (EOL 예정) |

**참고**: 정확한 마이너 버전 목록은 AWS CLI로 확인:
```bash
aws rds describe-db-engine-versions --engine mysql
```

## 주요 기능

### 관리 기능
- ✅ 자동 백업 및 스냅샷
- ✅ 자동 소프트웨어 패치
- ✅ Multi-AZ 배포
- ✅ 읽기 전용 복제본 (최대 15개)
- ✅ 스토리지 자동 확장

### MySQL 고유 기능
- ✅ InnoDB 스토리지 엔진
- ✅ 풀텍스트 검색
- ✅ 저장 프로시저 및 함수
- ✅ 트리거
- ✅ 뷰

## 인스턴스 클래스

### 권장 인스턴스 클래스

| 유형 | 클래스 예시 | 사용 사례 |
|-----|-----------|----------|
| 범용 | db.m6g, db.m5 | 일반 워크로드 |
| 메모리 최적화 | db.r6g, db.r5 | 메모리 집약적 |
| 버스터블 | db.t3, db.t4g | 개발/테스트 |

## 스토리지 옵션

| 유형 | 최대 용량 | 최대 IOPS | 권장 사용 |
|-----|---------|----------|---------|
| gp3 | 64 TiB | 16,000 | 일반 워크로드 |
| io1/io2 | 64 TiB | 256,000 | I/O 집약적 |

## 연결

### 기본 포트
- **3306** (기본값)

### 연결 문자열
```
mysql -h <endpoint> -P 3306 -u <username> -p
```

### 애플리케이션 연결 (JDBC)
```
jdbc:mysql://<endpoint>:3306/<database>?useSSL=true
```

## 보안

### SSL/TLS 연결
```bash
mysql -h mydb.xxx.rds.amazonaws.com -u admin -p \
    --ssl-ca=global-bundle.pem \
    --ssl-mode=VERIFY_IDENTITY
```

### SSL 강제
```sql
-- 사용자별 SSL 필수
ALTER USER 'myuser'@'%' REQUIRE SSL;
```

### 파라미터 그룹에서 설정
```bash
aws rds modify-db-parameter-group \
    --db-parameter-group-name my-mysql-params \
    --parameters "ParameterName=require_secure_transport,ParameterValue=1"
```

## 주요 파라미터

### 성능 관련

| 파라미터 | 설명 | 권장 값 |
|---------|------|--------|
| `innodb_buffer_pool_size` | InnoDB 버퍼 풀 | 메모리의 75% |
| `max_connections` | 최대 연결 수 | 워크로드에 따라 |
| `query_cache_size` | 쿼리 캐시 (5.7) | 비활성화 권장 |
| `innodb_log_file_size` | 리두 로그 크기 | 기본값 유지 |

### 로깅 관련

| 파라미터 | 설명 |
|---------|------|
| `general_log` | 일반 로그 활성화 |
| `slow_query_log` | 슬로우 쿼리 로그 |
| `long_query_time` | 슬로우 쿼리 기준 (초) |
| `log_output` | 로그 출력 대상 |

## 복제

### 읽기 복제본 생성
```bash
aws rds create-db-instance-read-replica \
    --db-instance-identifier mydb-replica \
    --source-db-instance-identifier mydb
```

### GTID 기반 복제
MySQL 5.7+ 에서 GTID(Global Transaction Identifier) 기반 복제 지원

### 외부 복제
- 온프레미스 MySQL에서 RDS로 복제 가능
- RDS에서 외부 MySQL로 복제 가능 (binlog 활용)

## 데이터 마이그레이션

### AWS DMS 사용
```
소스: 온프레미스 MySQL
대상: RDS for MySQL
복제 유형: 전체 로드 + CDC
```

### mysqldump 사용
```bash
# 내보내기
mysqldump -h source-host -u user -p database > backup.sql

# 가져오기
mysql -h rds-endpoint -u admin -p database < backup.sql
```

### Percona XtraBackup
대용량 데이터베이스의 빠른 마이그레이션

## 제한사항

### RDS 특유 제한
- ❌ 호스트(OS) 직접 접근 불가
- ❌ SUPER 권한 없음
- ❌ 일부 플러그인 설치 제한
- ❌ 파일 시스템 직접 접근 불가

### 우회 방법
| 제한 | 대안 |
|-----|-----|
| SUPER 권한 | RDS 제공 저장 프로시저 사용 |
| 파일 접근 | S3 통합 사용 |
| 플러그인 | 옵션 그룹으로 지원되는 것만 사용 |

## 규정 준수

- ✅ HIPAA 적격
- ✅ PCI DSS 준수
- ✅ SOC 1, 2, 3
- ✅ FedRAMP (GovCloud)

## 모범 사례

### 성능
```
✅ innodb_buffer_pool_size 최적화
✅ 적절한 인덱스 설계
✅ 슬로우 쿼리 모니터링 및 최적화
✅ 읽기 복제본으로 읽기 부하 분산
```

### 보안
```
✅ SSL/TLS 연결 강제
✅ 최소 권한 사용자 생성
✅ 저장 데이터 암호화 활성화
✅ VPC 프라이빗 서브넷 사용
```

### 가용성
```
✅ Multi-AZ 배포 사용
✅ 자동 백업 활성화
✅ 읽기 복제본 크로스 리전 배포 (DR)
```

## 관련 문서

- [DB 인스턴스 생성](../../03-db-instances/creating-instance.md)
- [파라미터 그룹](../../10-management/parameter-groups.md)
- [읽기 복제본](../../07-high-availability/read-replicas.md)
- [백업 및 복구](../../06-backup-recovery/automated-backups.md)
