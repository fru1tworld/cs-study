# Amazon RDS for SQL Server

## 개요

Amazon RDS for SQL Server는 완전 관리형 Microsoft SQL Server 데이터베이스 서비스입니다. SQL Server의 기능을 활용하면서 관리 작업을 AWS에 위임할 수 있습니다.

## 지원 버전

| 버전 | 최신 빌드 |
|-----|----------|
| SQL Server 2022 | ✅ 최신 |
| SQL Server 2019 | ✅ 권장 |
| SQL Server 2017 | ✅ 지원 |
| SQL Server 2016 SP3 | ✅ 지원 |

## 에디션

| 에디션 | 용도 | DB 수 제한 |
|-------|-----|-----------|
| Express | 개발/테스트 | 100개 |
| Web | 웹 애플리케이션 | 100개 |
| Standard | 일반 워크로드 | 100개 |
| Enterprise | 미션 크리티컬 | 100개 |

## 라이선스 모델

| 모델 | 설명 |
|-----|------|
| License Included | AWS가 라이선스 제공 (시간당 요금에 포함) |
| BYOL | 기존 Microsoft 라이선스 사용 (Enterprise만) |

## 연결

### 기본 포트
- **1433** (기본값)

### 연결 문자열 (sqlcmd)
```bash
sqlcmd -S mydb.xxx.rds.amazonaws.com,1433 -U admin -P password
```

### JDBC 연결
```
jdbc:sqlserver://mydb.xxx.rds.amazonaws.com:1433;
databaseName=mydb;
encrypt=true;
trustServerCertificate=false;
```

### .NET 연결
```
Server=mydb.xxx.rds.amazonaws.com,1433;
Database=mydb;
User Id=admin;
Password=password;
Encrypt=true;
```

### 예약 포트
다음 포트는 사용 불가:
- 1234, 1434, 3260, 3343, 3389
- 47001, 49152-49156

## 스토리지 제한

| 스토리지 유형 | 최소 | 최대 |
|-------------|-----|-----|
| General Purpose (gp2/gp3) | 20 GiB | 16 TiB |
| Provisioned IOPS | 20 GiB | 64 TiB |

## Multi-AZ 배포

### 지원 방식

| 방식 | 에디션 | 특징 |
|-----|-------|-----|
| Database Mirroring | Standard, Enterprise | 동기식 미러링 |
| Always On AG | Enterprise | 가용성 그룹 |

### Always On 가용성 그룹 (Enterprise)
```
- 자동 장애 조치
- 읽기 전용 복제본 지원
- 더 빠른 장애 조치 시간
```

## 읽기 복제본

### 제한사항
- Enterprise 에디션만 지원
- 최대 5개 복제본
- Always On AG 기반

### 생성
```bash
aws rds create-db-instance-read-replica \
    --db-instance-identifier mydb-replica \
    --source-db-instance-identifier mydb
```

## 옵션 그룹

### 주요 옵션

| 옵션 | 설명 |
|-----|------|
| TDE | 투명 데이터 암호화 |
| Native Backup/Restore | S3와 네이티브 백업 통합 |
| SQL Server Audit | 감사 로깅 |
| SSRS | Reporting Services |
| SSIS | Integration Services |
| SSAS | Analysis Services |

### Native Backup 활성화

```bash
aws rds add-option-to-option-group \
    --option-group-name my-sqlserver-options \
    --options OptionName=SQLSERVER_BACKUP_RESTORE,\
OptionSettings=[{Name=IAM_ROLE_ARN,Value=arn:aws:iam::123456789012:role/rds-backup-role}]
```

## S3 통합 (Native Backup/Restore)

### 백업
```sql
exec msdb.dbo.rds_backup_database
    @source_db_name='mydb',
    @s3_arn_to_backup_to='arn:aws:s3:::my-bucket/backup.bak',
    @overwrite_s3_backup_file=1;
```

### 복원
```sql
exec msdb.dbo.rds_restore_database
    @restore_db_name='mydb_restored',
    @s3_arn_to_restore_from='arn:aws:s3:::my-bucket/backup.bak';
```

### 작업 상태 확인
```sql
exec msdb.dbo.rds_task_status @db_name='mydb';
```

## Windows 인증

### Active Directory 통합
1. AWS Directory Service에 디렉터리 생성
2. DB 인스턴스를 디렉터리에 조인
3. Windows 인증 사용자 생성

```bash
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --domain d-1234567890 \
    --domain-iam-role-name rds-ad-role
```

### Windows 로그인 생성
```sql
USE [master]
CREATE LOGIN [MYDOMAIN\myuser] FROM WINDOWS;
```

## 주요 파라미터

| 파라미터 | 설명 |
|---------|------|
| `max server memory` | 최대 메모리 (RDS가 관리) |
| `cost threshold for parallelism` | 병렬 처리 임계값 |
| `max degree of parallelism` | 최대 병렬 처리 수준 |
| `optimize for ad hoc workloads` | 임시 쿼리 최적화 |

## 데이터 마이그레이션

### Native Backup/Restore
온프레미스에서 백업 → S3 업로드 → RDS에서 복원

### AWS DMS
- 최소 다운타임 마이그레이션
- CDC 지원

### Import/Export Wizard
SQL Server Management Studio에서 사용

## 제한사항

### RDS 특유 제한
- ❌ sysadmin 역할 없음
- ❌ Windows Server 직접 접근 불가
- ❌ SQL Server Agent 제한적 지원
- ❌ 일부 확장 저장 프로시저 불가

### SQL Server Agent 제한
지원되는 작업:
- T-SQL
- SSIS 패키지

지원되지 않는 작업:
- PowerShell
- Replication
- Log Reader

## 규정 준수

- ✅ HIPAA 적격 (Enterprise, Standard, Web)
- ✅ PCI DSS 준수
- ✅ SOC 1, 2, 3
- ✅ FedRAMP (GovCloud)

## 모범 사례

### 성능
```
✅ 적절한 인덱스 설계
✅ 쿼리 저장소(Query Store) 활용
✅ 실행 계획 분석
✅ 통계 자동 업데이트 활성화
```

### 보안
```
✅ TDE 활성화 (Enterprise)
✅ SSL/TLS 연결 강제
✅ 감사 로깅 구성
✅ 최소 권한 원칙 적용
```

### 백업
```
✅ Native Backup으로 S3에 추가 백업
✅ 정기적인 복원 테스트
✅ 트랜잭션 로그 백업 빈도 설정
```

## 관련 문서

- [DB 인스턴스 생성](../../03-db-instances/creating-instance.md)
- [옵션 그룹](../../10-management/option-groups.md)
- [Multi-AZ 배포](../../07-high-availability/multi-az.md)
