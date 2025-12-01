# DB 인스턴스 개요

## DB 인스턴스란?

DB 인스턴스는 클라우드에서 실행되는 격리된 데이터베이스 환경입니다. Amazon RDS의 기본 구성 요소로, AWS Management Console이나 CLI 도구를 사용하여 간편하게 생성하고 수정할 수 있습니다.

## DB 인스턴스 식별자

각 DB 인스턴스는 고유한 식별자를 가지며, 이는 DNS 엔드포인트에 포함됩니다.

** 엔드포인트 형식:**
```
<db-instance-identifier>.<random-string>.<region>.rds.amazonaws.com
```

** 예시:**
```
mydb.abcdefghijkl.us-east-1.rds.amazonaws.com
```

### 명명 규칙
- 1-63자의 영숫자 또는 하이픈
- 첫 글자는 알파벳
- 하이픈 연속 사용 불가
- 하이픈으로 끝날 수 없음
- 리전 내에서 고유해야 함

## 인스턴스 제한

| 데이터베이스 엔진 | 기본 제한 |
|-----------------|---------|
| MySQL | 40개 |
| MariaDB | 40개 |
| PostgreSQL | 40개 |
| Db2 | 40개 |
| Oracle (라이선스 포함) | 10개 |
| Oracle (BYOL) | 40개 |
| SQL Server | 10개 (라이선스 포함) |

추가 인스턴스가 필요한 경우 AWS 서비스 한도 증가 요청을 제출할 수 있습니다.

## DB 인스턴스 클래스

DB 인스턴스 클래스는 인스턴스의 컴퓨팅 및 메모리 용량을 결정합니다.

### 클래스 유형

#### 범용 (Standard)
- **db.m7g, db.m6g, db.m6i, db.m5**: 균형 잡힌 컴퓨팅, 메모리, 네트워킹
- 대부분의 데이터베이스 워크로드에 적합

#### 메모리 최적화 (Memory Optimized)
- **db.r7g, db.r6g, db.r6i, db.r5**: 메모리 집약적 워크로드
- 대용량 데이터셋을 처리하는 데이터베이스에 적합

#### 버스터블 (Burstable)
- **db.t4g, db.t3, db.t3.micro**: 기본 CPU 성능과 버스트 기능
- 개발/테스트 환경 또는 간헐적 워크로드에 적합

### 클래스 변경
- 인스턴스 클래스는 수정 작업을 통해 변경 가능
- 변경 시 다운타임 발생 가능
- 테스트 환경에서 먼저 검증 권장

## DB 인스턴스 상태

| 상태 | 설명 |
|-----|------|
| **available** | 정상 사용 가능 상태 |
| **creating** | 인스턴스 생성 중 |
| **modifying** | 인스턴스 수정 중 |
| **backing-up** | 백업 진행 중 |
| **rebooting** | 재부팅 중 |
| **stopping** | 중지 중 |
| **stopped** | 중지됨 |
| **starting** | 시작 중 |
| **deleting** | 삭제 중 |
| **failed** | 실패 상태 |
| **storage-full** | 스토리지 용량 초과 |

## 마스터 사용자

DB 인스턴스 생성 시 마스터 사용자 계정이 자동으로 생성됩니다.

### 마스터 사용자 권한
- 데이터베이스 생성 및 삭제
- 테이블 생성, 수정, 삭제
- 사용자 생성 및 권한 부여
- 저장 프로시저 생성

### 주의사항
- 마스터 비밀번호는 생성 시 확인 후 안전하게 보관
- AWS Secrets Manager를 통한 자동 관리 권장
- 마스터 사용자로 일상적인 작업 수행 지양 (별도 사용자 생성 권장)

## 호스트 액세스 제한

Amazon RDS는 관리형 서비스로, 보안상의 이유로 다음을 제한합니다:

- ❌ SSH/Telnet을 통한 호스트 직접 접근 불가
- ❌ 운영 체제 수준 접근 불가
- ❌ 일부 시스템 프로시저 및 테이블 접근 제한

### 허용되는 접근 방법
- ✅ 표준 SQL 클라이언트 (mysql, psql, sqlcmd 등)
- ✅ 데이터베이스 관리 도구 (DBeaver, pgAdmin 등)
- ✅ 애플리케이션 드라이버를 통한 연결

## 관련 문서

- [DB 인스턴스 생성](./creating-instance.md)
- [DB 인스턴스 수정](./modifying-instance.md)
- [DB 인스턴스 삭제](./deleting-instance.md)
- [인스턴스 클래스 상세](./instance-classes.md)
