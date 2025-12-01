# Amazon RDS 모니터링 개요

## 모니터링의 중요성

효과적인 모니터링 계획에는 다음 항목이 포함되어야 합니다:
- 모니터링 목표
- 모니터링 대상 리소스
- 모니터링 빈도
- 사용할 모니터링 도구
- 담당자 및 알림 대상

## 성능 기준선 수립

다양한 부하 조건에서 성능을 측정하여 기준선을 설정해야 합니다.

### 주요 모니터링 메트릭
| 메트릭 | 설명 | 권장 기준 |
|-------|------|----------|
| 네트워크 처리량 | 네트워크 I/O 대역폭 | 예상 트래픽과 비교 |
| 클라이언트 연결 수 | 동시 연결 수 | 최대 연결 수 대비 |
| 읽기/쓰기 IOPS | 초당 I/O 작업 수 | 기준선 대비 변화 |
| 디스크 사용률 | 스토리지 사용량 | 85% 이하 유지 |
| 버스트 크레딧 | gp2 버스트 잔액 | 고갈 방지 |

## 성능 문제 지표

### 1. 높은 CPU/RAM 사용률
- 애플리케이션 목표와 일치하면 정상
- 예상치 않은 높은 사용률은 조사 필요

### 2. 디스크 사용률
```
⚠️ 85% 이상: 데이터 삭제 또는 아카이빙 검토
🔴 90% 이상: 즉시 스토리지 확장 필요
```

### 3. 네트워크 트래픽
- 예상 처리량과 비교 분석
- 비정상적인 트래픽 패턴 감지

### 4. DB 연결 수
- 성능 저하 시 연결 제한 고려
- 연결 풀링 활용

### 5. IOPS 메트릭
- 기준선과 비교하여 이상 감지
- 스토리지 유형 적합성 검토

## 모니터링 도구

### Amazon CloudWatch

#### 기본 모니터링 (무료)
- 5분 간격 메트릭 수집
- 주요 인스턴스 메트릭

#### 상세 모니터링 (Enhanced Monitoring)
- 1분 간격 OS 수준 메트릭
- 추가 비용 발생

```bash
# Enhanced Monitoring 활성화
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --monitoring-interval 60 \
    --monitoring-role-arn arn:aws:iam::123456789012:role/rds-monitoring-role
```

### 주요 CloudWatch 메트릭

| 메트릭 | 설명 |
|-------|------|
| `CPUUtilization` | CPU 사용률 (%) |
| `DatabaseConnections` | 활성 연결 수 |
| `FreeableMemory` | 사용 가능한 RAM |
| `FreeStorageSpace` | 사용 가능한 스토리지 |
| `ReadIOPS` / `WriteIOPS` | 초당 I/O 작업 |
| `ReadLatency` / `WriteLatency` | I/O 지연 시간 |
| `DiskQueueDepth` | 대기 중인 I/O |
| `NetworkReceiveThroughput` | 수신 네트워크 처리량 |
| `NetworkTransmitThroughput` | 송신 네트워크 처리량 |
| `ReplicaLag` | 복제 지연 (초) |

### CloudWatch 경보 설정

```bash
aws cloudwatch put-metric-alarm \
    --alarm-name "RDS-High-CPU" \
    --alarm-description "CPU 사용률 80% 초과" \
    --metric-name CPUUtilization \
    --namespace AWS/RDS \
    --statistic Average \
    --period 300 \
    --threshold 80 \
    --comparison-operator GreaterThanThreshold \
    --dimensions Name=DBInstanceIdentifier,Value=mydb \
    --evaluation-periods 3 \
    --alarm-actions arn:aws:sns:us-east-1:123456789012:my-topic
```

### 권장 경보

| 조건 | 임계값 | 심각도 |
|-----|-------|-------|
| CPU 사용률 | > 80% (5분, 3회) | 경고 |
| 메모리 부족 | < 256MB | 경고 |
| 스토리지 부족 | < 10% | 위험 |
| 연결 수 | > 최대의 80% | 경고 |
| 복제 지연 | > 60초 | 경고 |

## Performance Insights

### 개요
데이터베이스 로드를 시각화하고 대기, SQL 문, 호스트 또는 사용자별로 필터링할 수 있습니다.

### 주요 기능
- ** 데이터베이스 로드 차트**: 시간에 따른 로드 시각화
- ** 대기 이벤트 분석**: 병목 현상 식별
- **Top SQL**: 가장 많은 리소스를 사용하는 쿼리
- **Top Hosts/Users**: 부하를 유발하는 클라이언트

### 활성화

```bash
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --enable-performance-insights \
    --performance-insights-retention-period 7
```

⚠️ ** 중요**: 2026년 6월 30일 이후 Performance Insights는 CloudWatch Database Insights로 대체될 예정

## Enhanced Monitoring

### OS 수준 메트릭
- 프로세스 목록
- CPU 코어별 사용률
- 파일 시스템 사용량
- 메모리 상세 정보

### 활성화

```bash
# IAM 역할 생성 필요
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --monitoring-interval 60 \
    --monitoring-role-arn arn:aws:iam::123456789012:role/rds-enhanced-monitoring-role
```

### 모니터링 간격
- 1, 5, 10, 15, 30, 60초
- 짧은 간격 = 더 상세한 데이터 = 더 높은 비용

## 이벤트 및 로그

### RDS 이벤트

```bash
# 최근 이벤트 조회
aws rds describe-events \
    --source-type db-instance \
    --source-identifier mydb \
    --duration 1440
```

### 이벤트 구독

```bash
aws rds create-event-subscription \
    --subscription-name my-db-events \
    --sns-topic-arn arn:aws:sns:us-east-1:123456789012:my-topic \
    --source-type db-instance \
    --source-ids mydb \
    --event-categories "availability" "backup" "failure"
```

### 데이터베이스 로그

| 엔진 | 주요 로그 |
|-----|----------|
| MySQL | 에러 로그, 슬로우 쿼리 로그, 일반 로그 |
| PostgreSQL | postgresql.log |
| Oracle | 경보 로그, 트레이스 파일 |
| SQL Server | 에러 로그, 에이전트 로그 |

### CloudWatch Logs로 내보내기

```bash
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --cloudwatch-logs-export-configuration \
        '{"EnableLogTypes":["error","slowquery","general"]}'
```

## 모니터링 대시보드

### CloudWatch 대시보드 생성

```bash
aws cloudwatch put-dashboard \
    --dashboard-name RDS-Monitoring \
    --dashboard-body file://dashboard.json
```

### 권장 위젯
- CPU/메모리 사용률 그래프
- 연결 수 추이
- IOPS 및 처리량
- 스토리지 사용량
- 복제 지연 (해당 시)

## 모니터링 모범 사례

### 설정
```
✅ 기준선 메트릭 수집 및 문서화
✅ 적절한 경보 임계값 설정
✅ 중요 이벤트에 대한 알림 구성
✅ 로그를 CloudWatch Logs로 내보내기
```

### 운영
```
✅ 정기적인 성능 검토
✅ 경보 발생 시 조치 프로세스 정의
✅ 용량 계획을 위한 추세 분석
✅ 비정상 패턴 조사
```

### 비용 최적화
```
✅ 필요한 메트릭만 상세 모니터링
✅ 로그 보존 기간 최적화
✅ 사용하지 않는 경보 정리
```

## 관련 문서

- [CloudWatch 메트릭 상세](./cloudwatch-metrics.md)
- [Performance Insights](./performance-insights.md)
- [로깅](./logging.md)
- [이벤트 알림](./events.md)
