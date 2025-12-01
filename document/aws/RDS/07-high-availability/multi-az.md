# Multi-AZ 배포

## 개요

Multi-AZ 배포는 Amazon RDS에서 고가용성과 내구성을 제공하는 핵심 기능입니다. 주 DB 인스턴스에 장애가 발생하면 자동으로 대기 인스턴스로 장애 조치가 수행됩니다.

## Multi-AZ 배포 유형

### 1. Multi-AZ DB 인스턴스 배포

```
┌─────────────────────────────────────────────────────────┐
│                         VPC                              │
│  ┌───────────────────┐     ┌───────────────────┐        │
│  │ Availability Zone A│     │ Availability Zone B│        │
│  │                   │     │                   │        │
│  │  ┌─────────────┐  │     │  ┌─────────────┐  │        │
│  │  │   Primary   │  │ ──▶ │  │   Standby   │  │        │
│  │  │ (읽기/쓰기) │  │동기화│  │  (대기)     │  │        │
│  │  └─────────────┘  │     │  └─────────────┘  │        │
│  └───────────────────┘     └───────────────────┘        │
└─────────────────────────────────────────────────────────┘
```

** 특징:**
- 단일 대기 DB 인스턴스
- 장애 조치 지원만 제공 (읽기 트래픽 처리 안 함)
- 동기식 복제
- 역할: Primary 또는 Instance

### 2. Multi-AZ DB 클러스터 배포

```
┌─────────────────────────────────────────────────────────────────┐
│                              VPC                                 │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐        │
│  │    AZ A     │     │    AZ B     │     │    AZ C     │        │
│  │             │     │             │     │             │        │
│  │ ┌─────────┐ │     │ ┌─────────┐ │     │ ┌─────────┐ │        │
│  │ │ Writer  │ │ ──▶ │ │ Reader  │ │ ──▶ │ │ Reader  │ │        │
│  │ │(읽기/쓰기)│ │동기화│ │(읽기)   │ │동기화│ │(읽기)   │ │        │
│  │ └─────────┘ │     │ └─────────┘ │     │ └─────────┘ │        │
│  └─────────────┘     └─────────────┘     └─────────────┘        │
└─────────────────────────────────────────────────────────────────┘
```

** 특징:**
- 2개의 대기(Reader) DB 인스턴스
- 장애 조치 지원 + 읽기 트래픽 처리
- 3개 가용 영역에 분산
- 역할: Writer instance 또는 Reader instance
- ** 지원 엔진**: MySQL, PostgreSQL만

## 동기식 복제

### 작동 방식
1. 주 인스턴스에 쓰기 트랜잭션 발생
2. 변경 사항이 대기 인스턴스로 동기 복제
3. 대기 인스턴스에서 커밋 확인
4. 애플리케이션에 완료 응답

### 장점
- 데이터 무손실 (RPO = 0)
- 일관된 데이터 보장

### 고려사항
- 약간의 쓰기 지연 발생
- 네트워크 대역폭 사용

## 자동 장애 조치

### 장애 조치 트리거
- 주 인스턴스 장애
- 가용 영역 장애
- DB 인스턴스 유형 변경
- 소프트웨어 패치
- 수동 장애 조치 요청

### 장애 조치 프로세스
```
1. 장애 감지 (보통 수십 초 내)
2. DNS 레코드 업데이트
3. 대기 인스턴스가 주 역할 승격
4. 새 대기 인스턴스 프로비저닝
```

### 장애 조치 시간
- ** 일반적**: 60-120초
- 대규모 트랜잭션/복구 프로세스 시 더 길어질 수 있음

### 수동 장애 조치

```bash
# 장애 조치 시작
aws rds reboot-db-instance \
    --db-instance-identifier mydb \
    --force-failover
```

## Multi-AZ 활성화

### 생성 시 활성화

```bash
aws rds create-db-instance \
    --db-instance-identifier mydb \
    --multi-az \
    --db-instance-class db.m6g.large \
    --engine mysql \
    ...
```

### 기존 인스턴스에서 활성화

```bash
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --multi-az \
    --apply-immediately
```

### 변환 프로세스
1. 주 인스턴스의 스냅샷 생성
2. 다른 AZ에서 스냅샷 복원
3. 동기 복제 설정
4. 약간의 I/O 일시 중단 발생 가능

## Multi-AZ DB 클러스터 생성

```bash
aws rds create-db-cluster \
    --db-cluster-identifier my-multi-az-cluster \
    --engine mysql \
    --engine-version 8.0.32 \
    --master-username admin \
    --manage-master-user-password \
    --db-cluster-instance-class db.r6gd.xlarge \
    --allocated-storage 100 \
    --storage-type io1 \
    --iops 1000
```

## 엔드포인트

### Multi-AZ DB 인스턴스
- ** 단일 엔드포인트**: 장애 조치 시 자동으로 대기 인스턴스로 연결

### Multi-AZ DB 클러스터
- **Writer 엔드포인트**: 쓰기/읽기 작업
- **Reader 엔드포인트**: 읽기 작업 (부하 분산)
- ** 인스턴스 엔드포인트**: 특정 인스턴스 직접 연결

## 백업

### Multi-AZ에서 백업
- 대기 인스턴스에서 백업 수행
- 주 인스턴스의 I/O 영향 최소화
- 프로덕션 워크로드에 영향 없음

### 백업 기간 권장사항
```
✅ 저부하 시간대 선택
✅ 정기 유지 관리 기간과 분리
```

## 모니터링

### 주요 이벤트
- `RDS-EVENT-0025`: Multi-AZ 장애 조치 완료
- `RDS-EVENT-0026`: Multi-AZ 장애 조치 시작

### CloudWatch 메트릭
- `ReplicaLag`: 복제 지연 (DB 클러스터)

### 이벤트 구독

```bash
aws rds create-event-subscription \
    --subscription-name my-failover-alerts \
    --sns-topic-arn arn:aws:sns:us-east-1:123456789012:my-topic \
    --source-type db-instance \
    --event-categories "failover"
```

## 비용

| 항목 | 비용 |
|-----|-----|
| Multi-AZ DB 인스턴스 | 단일 AZ의 약 2배 |
| Multi-AZ DB 클러스터 | 인스턴스 수에 따름 |
| 데이터 전송 | AZ 간 전송 무료 |

## 비교: Multi-AZ vs Read Replica

| 특성 | Multi-AZ | Read Replica |
|-----|---------|--------------|
| 목적 | 고가용성 | 읽기 확장 |
| 복제 | 동기식 | 비동기식 |
| 장애 조치 | 자동 | 수동 승격 |
| 읽기 트래픽 | 처리 안 함* | 처리 가능 |
| 다른 리전 | 불가 | 가능 |

*Multi-AZ DB 클러스터의 Reader는 읽기 트래픽 처리 가능

## 모범 사례

### 설계
```
✅ 프로덕션 환경에서 Multi-AZ 사용
✅ 애플리케이션에서 재연결 로직 구현
✅ 연결 풀링 사용으로 장애 조치 영향 최소화
✅ 정기적으로 수동 장애 조치 테스트
```

### 모니터링
```
✅ 장애 조치 이벤트 알림 설정
✅ 복제 상태 모니터링
✅ 장애 조치 후 성능 검증
```

## 관련 문서

- [읽기 복제본](./read-replicas.md)
- [백업 및 복구](../06-backup-recovery/automated-backups.md)
- [모니터링](../08-monitoring/overview.md)
