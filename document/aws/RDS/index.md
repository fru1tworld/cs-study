# Amazon RDS 문서

> Amazon Relational Database Service(Amazon RDS)는 AWS 클라우드에서 관계형 데이터베이스를 쉽게 설정, 운영 및 확장할 수 있게 해주는 완전 관리형 서비스입니다.

## 목차

### 1. 개요
- [Amazon RDS란?](./01-overview/what-is-rds.md)
  - RDS 소개 및 주요 이점
  - 지원 데이터베이스 엔진
  - 공유 책임 모델
  - 비용 및 프리 티어

### 2. 시작하기
- [Amazon RDS 시작하기](./02-getting-started/getting-started.md)
  - 사전 요구사항
  - DB 인스턴스 생성
  - 데이터베이스 연결

### 3. DB 인스턴스
- [DB 인스턴스 개요](./03-db-instances/overview.md)
  - DB 인스턴스 개념
  - 인스턴스 클래스
  - 인스턴스 상태
- [DB 인스턴스 생성](./03-db-instances/creating-instance.md)
  - 콘솔/CLI/API를 통한 생성
  - 필수 파라미터
- [DB 인스턴스 수정](./03-db-instances/modifying-instance.md)
  - 설정 변경
  - 인스턴스 클래스 변경
  - 스토리지 수정
- [DB 인스턴스 삭제](./03-db-instances/deleting-instance.md)
  - 삭제 절차
  - 최종 스냅샷
  - 자동 백업 보존

### 4. 스토리지
- [스토리지 유형](./04-storage/storage-types.md)
  - General Purpose SSD (gp3, gp2)
  - Provisioned IOPS SSD (io2, io1)
  - Magnetic (레거시)
  - 스토리지 자동 확장

### 5. 보안
- [보안 개요](./05-security/overview.md)
  - 보안 계층
  - 규정 준수
  - 모범 사례
- [VPC 구성](./05-security/vpc-configuration.md)
  - VPC 및 서브넷
  - DB 서브넷 그룹
  - 보안 그룹
  - 네트워크 시나리오
- [암호화](./05-security/encryption.md)
  - 저장 데이터 암호화
  - 전송 데이터 암호화 (SSL/TLS)
  - KMS 키 관리

### 6. 백업 및 복구
- [자동 백업](./06-backup-recovery/automated-backups.md)
  - 백업 보존 기간
  - 백업 기간
  - 특정 시점 복구 (PITR)
- [DB 스냅샷](./06-backup-recovery/snapshots.md)
  - 수동 스냅샷 생성
  - 스냅샷 복사
  - 스냅샷 공유
  - 스냅샷에서 복원

### 7. 고가용성
- [Multi-AZ 배포](./07-high-availability/multi-az.md)
  - Multi-AZ DB 인스턴스
  - Multi-AZ DB 클러스터
  - 자동 장애 조치
- [읽기 복제본](./07-high-availability/read-replicas.md)
  - 읽기 확장
  - 크로스 리전 복제
  - 복제본 승격

### 8. 모니터링
- [모니터링 개요](./08-monitoring/overview.md)
  - CloudWatch 메트릭
  - Enhanced Monitoring
  - Performance Insights
  - 이벤트 알림
  - 로깅

### 9. 데이터베이스 엔진

#### MySQL
- [RDS for MySQL 개요](./09-database-engines/mysql/overview.md)
  - 지원 버전 및 기능
  - 연결 및 보안
  - 복제
  - 마이그레이션

#### PostgreSQL
- [RDS for PostgreSQL 개요](./09-database-engines/postgresql/overview.md)
  - 지원 버전 및 기능
  - 확장(Extensions)
  - S3/Lambda 통합
  - IAM 데이터베이스 인증

#### MariaDB
- [RDS for MariaDB 개요](./09-database-engines/mariadb/overview.md)
  - MySQL과의 차이점
  - 지원 기능
  - 마이그레이션

#### Oracle
- [RDS for Oracle 개요](./09-database-engines/oracle/overview.md)
  - 에디션 및 라이선스
  - 옵션 그룹
  - Active Data Guard
  - RDS Custom

#### SQL Server
- [RDS for SQL Server 개요](./09-database-engines/sqlserver/overview.md)
  - 에디션 및 기능
  - Windows 인증
  - Native Backup/Restore
  - Always On AG

#### Db2
- [RDS for Db2 개요](./09-database-engines/db2/overview.md)
  - 지원 기능
  - 페더레이션
  - 마이그레이션

### 10. 관리
- [파라미터 그룹](./10-management/parameter-groups.md)
  - 파라미터 그룹 생성
  - 파라미터 수정
  - 동적/정적 파라미터

---

## 빠른 참조

### 기본 포트

| 데이터베이스 | 기본 포트 |
|------------|----------|
| MySQL/MariaDB | 3306 |
| PostgreSQL | 5432 |
| Oracle | 1521 |
| SQL Server | 1433 |
| Db2 | 50000 |

### 인스턴스 제한 (기본값)

| 엔진 | 최대 인스턴스 |
|-----|-------------|
| MySQL, MariaDB, PostgreSQL, Db2 | 40개 |
| Oracle (라이선스 포함) | 10개 |
| SQL Server | 10개 |

### 스토리지 제한

| 스토리지 유형 | 최대 용량 |
|-------------|----------|
| gp3, io1, io2 | 64 TiB |
| gp2 | 64 TiB |
| Magnetic | 3 TiB |

---

## 공식 문서

- [AWS RDS 공식 문서](https://docs.aws.amazon.com/rds/)
- [AWS RDS 사용자 가이드](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/)
- [AWS RDS API 레퍼런스](https://docs.aws.amazon.com/AmazonRDS/latest/APIReference/)
- [AWS RDS 가격](https://aws.amazon.com/rds/pricing/)

---

*이 문서는 AWS RDS 공식 문서를 기반으로 정리되었습니다.*
