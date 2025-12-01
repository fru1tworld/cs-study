# Amazon RDS for Db2

## 개요

Amazon RDS for Db2는 IBM Db2 데이터베이스의 완전 관리형 서비스입니다. 엔터프라이즈급 Db2 워크로드를 AWS 클라우드에서 실행할 수 있습니다.

## 지원 버전

| 버전 | 지원 상태 |
|-----|----------|
| Db2 11.5 | ✅ 현재 지원 |

## 에디션

| 에디션 | 용도 |
|-------|-----|
| Standard Edition | 일반 워크로드 |
| Advanced Edition | 고급 기능 (압축, 보안 등) |

## 주요 기능

### RDS 관리 기능
- ✅ 자동 백업 및 스냅샷
- ✅ Multi-AZ 배포
- ✅ 스토리지 자동 확장
- ✅ 자동 마이너 버전 업그레이드
- ✅ 읽기 복제본 (HADR 기반)

### Db2 고유 기능
- ✅ 고급 압축 (Advanced Edition)
- ✅ 행 및 컬럼 접근 제어
- ✅ 네이티브 암호화
- ✅ 페더레이션

## 연결

### 기본 포트
- **50000** (기본값)

### IBM Data Studio 연결
```
호스트: mydb.xxx.rds.amazonaws.com
포트: 50000
데이터베이스: MYDB
사용자: admin
```

### JDBC 연결
```
jdbc:db2://mydb.xxx.rds.amazonaws.com:50000/MYDB
```

### CLI 연결
```bash
db2 connect to MYDB user admin using password
```

## 사용자 권한

### 마스터 사용자 권한
- DBADM 권한 보유
- 데이터베이스 관리 작업 수행 가능

### 제한된 권한
- ❌ SYSADM
- ❌ SYSCTRL
- ❌ SYSMAINT

이러한 인스턴스 수준 권한은 RDS가 관리합니다.

## Multi-AZ 배포

### 고가용성
- 동기식 복제
- 자동 장애 조치
- HADR (High Availability Disaster Recovery) 기반

## 읽기 복제본

### 특징
- HADR 기반
- 최대 1개 복제본
- 읽기 전용 쿼리 지원

### 생성
```bash
aws rds create-db-instance-read-replica \
    --db-instance-identifier mydb-replica \
    --source-db-instance-identifier mydb
```

## 데이터 마이그레이션

### AWS DMS
- 온프레미스 Db2에서 RDS Db2로 마이그레이션
- CDC(Change Data Capture) 지원

### Db2 Native Tools
- `db2move` 유틸리티
- `EXPORT`/`IMPORT` 명령
- `LOAD` 명령

## S3 통합

### 데이터 내보내기
S3로 데이터 내보내기 지원

### 데이터 가져오기
S3에서 데이터 로드 지원

## 파라미터 그룹

### 주요 파라미터

| 파라미터 | 설명 |
|---------|------|
| `db2_workload` | 워크로드 유형 |
| `logbufsz` | 로그 버퍼 크기 |
| `locklist` | 락 리스트 크기 |
| `maxlocks` | 최대 락 비율 |

## 페더레이션

### 지원되는 데이터 소스
- 다른 Db2 데이터베이스
- Oracle
- SQL Server
- PostgreSQL

### 설정 단계
1. 페더레이션 활성화
2. 외부 데이터 소스 래퍼 생성
3. 서버 정의
4. 닉네임 생성

## 제한사항

### RDS 특유 제한
- ❌ 인스턴스 수준 관리 권한 없음
- ❌ 호스트 직접 접근 불가
- ❌ 일부 Db2 기능 제한

### 우회 방법
| 제한 | 대안 |
|-----|-----|
| 인스턴스 관리 | RDS API/콘솔 사용 |
| 파일 시스템 접근 | S3 통합 사용 |

## 모범 사례

### 성능
```
✅ 버퍼 풀 크기 최적화
✅ 정기적인 RUNSTATS 실행
✅ 인덱스 설계 최적화
✅ 쿼리 튜닝
```

### 보안
```
✅ SSL/TLS 연결 사용
✅ 저장 데이터 암호화 활성화
✅ 행 수준 보안 활용 (Advanced Edition)
✅ 최소 권한 사용자 생성
```

### 마이그레이션
```
✅ 버전 호환성 확인
✅ 테스트 환경에서 충분한 검증
✅ 애플리케이션 호환성 테스트
```

## 관련 문서

- [DB 인스턴스 생성](../../03-db-instances/creating-instance.md)
- [파라미터 그룹](../../10-management/parameter-groups.md)
- [Multi-AZ 배포](../../07-high-availability/multi-az.md)
