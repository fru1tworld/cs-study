# Amazon RDS for Oracle

## 개요

Amazon RDS for Oracle은 완전 관리형 Oracle 데이터베이스 서비스입니다. Oracle Database의 기능을 활용하면서 관리 부담을 줄일 수 있습니다.

## 지원 버전

| 버전 | 지원 상태 |
|-----|----------|
| Oracle 21c (21.0.0.0) | ✅ 최신 |
| Oracle 19c (19.0.0.0) | ✅ 장기 지원 (권장) |

**⚠️ 레거시 버전**: Oracle 11g, 12c, 18c는 더 이상 지원되지 않음

## 에디션

### 지원 에디션

| 에디션 | 용도 | 기능 |
|-------|-----|------|
| Standard Edition Two (SE2) | 중소규모 워크로드 | 기본 기능 |
| Enterprise Edition (EE) | 대규모 미션 크리티컬 | 고급 기능 포함 |

### 라이선스 모델

| 모델 | 설명 | 적합한 경우 |
|-----|------|-----------|
| License Included | AWS에서 라이선스 포함 | 새로운 도입, 유연성 필요 |
| BYOL (Bring Your Own License) | 기존 라이선스 사용 | 이미 라이선스 보유 |

## 주요 기능

### RDS 관리 기능
- ✅ 자동 백업 및 스냅샷
- ✅ Multi-AZ 배포
- ✅ 읽기 전용 복제본 (Active Data Guard)
- ✅ 자동 마이너 버전 업그레이드

### Oracle 고급 기능 (EE)
- ✅ Oracle Partitioning
- ✅ Oracle Advanced Compression
- ✅ Oracle Spatial
- ✅ Oracle OLAP
- ✅ Oracle Label Security

## 연결

### 기본 포트
- **1521** (기본값)

### 연결 문자열
```bash
sqlplus admin/password@//mydb.xxx.rds.amazonaws.com:1521/ORCL
```

### Easy Connect
```
admin/password@mydb.xxx.rds.amazonaws.com:1521/ORCL
```

### TNS 연결
```
(DESCRIPTION=
  (ADDRESS=(PROTOCOL=TCP)(HOST=mydb.xxx.rds.amazonaws.com)(PORT=1521))
  (CONNECT_DATA=(SID=ORCL))
)
```

## 옵션 그룹

### 주요 옵션

| 옵션 | 설명 | 에디션 |
|-----|------|-------|
| Oracle Statspack | 성능 분석 | 모든 에디션 |
| Oracle Enterprise Manager (OEM) | DB 관리 | EE |
| Oracle Spatial | 지리공간 데이터 | SE2, EE |
| Oracle APEX | 웹 애플리케이션 개발 | 모든 에디션 |
| Oracle Native Network Encryption | 네이티브 암호화 | 모든 에디션 |
| TDE (Transparent Data Encryption) | 투명 데이터 암호화 | EE |

### 옵션 그룹 생성

```bash
aws rds create-option-group \
    --option-group-name my-oracle-options \
    --engine-name oracle-ee \
    --major-engine-version 19 \
    --option-group-description "My Oracle options"
```

### 옵션 추가

```bash
aws rds add-option-to-option-group \
    --option-group-name my-oracle-options \
    --options OptionName=STATSPACK
```

## 주요 파라미터

### 성능 관련

| 파라미터 | 설명 |
|---------|------|
| `processes` | 최대 프로세스 수 |
| `sessions` | 최대 세션 수 |
| `sga_target` | SGA 자동 관리 크기 |
| `pga_aggregate_target` | PGA 총 크기 |
| `cursor_sharing` | 커서 공유 방식 |

### 메모리 관리

```
# 권장 설정 비율
SGA: 인스턴스 메모리의 75%
PGA: 인스턴스 메모리의 25%
```

## Character Set

### 지원 문자셋
- AL32UTF8 (권장, 유니코드)
- WE8ISO8859P1
- 기타 Oracle 지원 문자셋

### National Character Set
- AL16UTF16 (기본)
- UTF8

## 읽기 복제본

### Active Data Guard
Enterprise Edition에서 읽기 복제본 생성:

```bash
aws rds create-db-instance-read-replica \
    --db-instance-identifier mydb-replica \
    --source-db-instance-identifier mydb
```

### 특징
- 물리적 대기 데이터베이스
- 읽기 전용 쿼리 지원
- 장애 조치 가능

## 데이터 마이그레이션

### Oracle Data Pump
```bash
# 내보내기 (소스)
expdp admin/password@source directory=DATA_PUMP_DIR dumpfile=export.dmp

# 가져오기 (RDS)
impdp admin/password@rds-endpoint directory=DATA_PUMP_DIR dumpfile=export.dmp
```

### AWS DMS
- 온프레미스 Oracle에서 RDS Oracle로
- 최소 다운타임 마이그레이션

### Oracle GoldenGate
- 실시간 복제
- 이기종 마이그레이션

## RDS Custom for Oracle

### 개요
호스트 및 운영 체제에 대한 더 많은 제어가 필요한 경우 RDS Custom 사용

### 특징
- SSH 접근 가능
- 사용자 정의 소프트웨어 설치
- 특정 패치 적용

### 적합한 경우
- 타사 소프트웨어 에이전트 필요
- 사용자 정의 패치 요구
- 레거시 애플리케이션 호환성

## 제한사항

### RDS 특유 제한
- ❌ SYS, SYSTEM 계정 직접 접근 불가
- ❌ OS 직접 접근 불가 (RDS Custom 제외)
- ❌ 일부 SYSDBA 권한 제한
- ❌ 특정 초기화 파라미터 수정 불가

### 대안
| 제한 | 대안 |
|-----|-----|
| SYS 접근 | RDS 제공 프로시저 사용 |
| OS 접근 | RDS Custom 사용 |

## 규정 준수

- ✅ HIPAA 적격
- ✅ PCI DSS 준수
- ✅ SOC 1, 2, 3
- ✅ FedRAMP (GovCloud)

## 모범 사례

### 성능
```
✅ AWR/Statspack을 통한 성능 분석
✅ 적절한 SGA/PGA 크기 설정
✅ 옵티마이저 통계 최신 유지
✅ 적절한 인덱스 설계
```

### 보안
```
✅ TDE로 저장 데이터 암호화 (EE)
✅ Native Network Encryption 사용
✅ 최소 권한 사용자 생성
✅ 감사 로깅 활성화
```

### 라이선스
```
✅ BYOL 사용 시 라이선스 규정 준수 확인
✅ CPU 코어 수에 따른 라이선스 계산
✅ 옵션 사용 시 추가 라이선스 확인
```

## 관련 문서

- [DB 인스턴스 생성](../../03-db-instances/creating-instance.md)
- [옵션 그룹](../../10-management/option-groups.md)
- [파라미터 그룹](../../10-management/parameter-groups.md)
- [읽기 복제본](../../07-high-availability/read-replicas.md)
