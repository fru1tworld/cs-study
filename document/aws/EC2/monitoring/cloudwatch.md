# EC2 CloudWatch 모니터링

## 개요

Amazon CloudWatch는 EC2 인스턴스와 AWS 리소스에 대한 모니터링 및 관찰 기능을 제공합니다. 지표 수집, 알람 설정, 대시보드 생성이 가능합니다.

## CloudWatch 지표

### 기본 모니터링 vs 세부 모니터링

| 특성 | 기본 모니터링 | 세부 모니터링 |
|------|-------------|--------------|
| 간격 | 5분 | 1분 |
| 비용 | 무료 | 유료 |
| 기본값 | 활성화 | 비활성화 |

```bash
# 세부 모니터링 활성화
aws ec2 monitor-instances --instance-ids i-xxxxxxxxx

# 세부 모니터링 비활성화
aws ec2 unmonitor-instances --instance-ids i-xxxxxxxxx
```

### 기본 EC2 지표

| 지표 | 설명 | 단위 |
|------|------|------|
| `CPUUtilization` | CPU 사용률 | 퍼센트 |
| `DiskReadOps` | 디스크 읽기 작업 수 | 카운트 |
| `DiskWriteOps` | 디스크 쓰기 작업 수 | 카운트 |
| `DiskReadBytes` | 디스크에서 읽은 바이트 | 바이트 |
| `DiskWriteBytes` | 디스크에 쓴 바이트 | 바이트 |
| `NetworkIn` | 수신된 네트워크 바이트 | 바이트 |
| `NetworkOut` | 송신된 네트워크 바이트 | 바이트 |
| `NetworkPacketsIn` | 수신된 패킷 수 | 카운트 |
| `NetworkPacketsOut` | 송신된 패킷 수 | 카운트 |
| `StatusCheckFailed` | 상태 검사 실패 | 카운트 |
| `StatusCheckFailed_Instance` | 인스턴스 상태 검사 실패 | 카운트 |
| `StatusCheckFailed_System` | 시스템 상태 검사 실패 | 카운트 |

### 지표 조회

```bash
# 특정 인스턴스의 CPU 사용률 조회
aws cloudwatch get-metric-statistics \
    --namespace AWS/EC2 \
    --metric-name CPUUtilization \
    --dimensions Name=InstanceId,Value=i-xxxxxxxxx \
    --start-time 2024-01-15T00:00:00Z \
    --end-time 2024-01-15T12:00:00Z \
    --period 300 \
    --statistics Average
```

## CloudWatch 에이전트

OS 수준의 상세 지표와 로그를 수집합니다.

### 설치 (Amazon Linux 2/2023)

```bash
# 에이전트 설치
sudo yum install -y amazon-cloudwatch-agent

# 설정 마법사 실행
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-config-wizard
```

### 설정 파일 예시

```json
{
    "agent": {
        "metrics_collection_interval": 60,
        "run_as_user": "root"
    },
    "metrics": {
        "namespace": "MyApp/EC2",
        "append_dimensions": {
            "InstanceId": "${aws:InstanceId}",
            "AutoScalingGroupName": "${aws:AutoScalingGroupName}"
        },
        "metrics_collected": {
            "cpu": {
                "measurement": [
                    "cpu_usage_idle",
                    "cpu_usage_user",
                    "cpu_usage_system"
                ],
                "totalcpu": true
            },
            "mem": {
                "measurement": [
                    "mem_used_percent",
                    "mem_available_percent"
                ]
            },
            "disk": {
                "measurement": [
                    "disk_used_percent",
                    "disk_free"
                ],
                "resources": ["/", "/data"]
            },
            "diskio": {
                "measurement": [
                    "diskio_reads",
                    "diskio_writes"
                ]
            }
        }
    },
    "logs": {
        "logs_collected": {
            "files": {
                "collect_list": [
                    {
                        "file_path": "/var/log/messages",
                        "log_group_name": "/ec2/var/log/messages",
                        "log_stream_name": "{instance_id}"
                    },
                    {
                        "file_path": "/var/log/httpd/access_log",
                        "log_group_name": "/ec2/httpd/access",
                        "log_stream_name": "{instance_id}"
                    }
                ]
            }
        }
    }
}
```

### 에이전트 시작

```bash
# 설정 파일 적용 및 시작
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
    -a fetch-config \
    -m ec2 \
    -s \
    -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json

# 상태 확인
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
    -a status
```

## CloudWatch 알람

### 알람 생성

```bash
# CPU 사용률 80% 초과 시 알람
aws cloudwatch put-metric-alarm \
    --alarm-name "High-CPU-Utilization" \
    --alarm-description "CPU exceeds 80%" \
    --metric-name CPUUtilization \
    --namespace AWS/EC2 \
    --statistic Average \
    --period 300 \
    --threshold 80 \
    --comparison-operator GreaterThanThreshold \
    --dimensions Name=InstanceId,Value=i-xxxxxxxxx \
    --evaluation-periods 2 \
    --alarm-actions arn:aws:sns:ap-northeast-2:xxx:my-topic
```

### 일반적인 알람 패턴

```
┌─────────────────────────────────────────────────────────────┐
│                    권장 알람 설정                            │
├─────────────────────────────────────────────────────────────┤
│  CPU 사용률                                                  │
│  - 경고: 70% 초과 5분                                        │
│  - 심각: 90% 초과 5분                                        │
│                                                              │
│  메모리 사용률 (에이전트 필요)                                │
│  - 경고: 80% 초과 5분                                        │
│  - 심각: 95% 초과 5분                                        │
│                                                              │
│  디스크 사용률 (에이전트 필요)                                │
│  - 경고: 80% 초과                                            │
│  - 심각: 90% 초과                                            │
│                                                              │
│  상태 검사                                                   │
│  - 심각: StatusCheckFailed > 0                              │
└─────────────────────────────────────────────────────────────┘
```

### 복합 알람

```bash
# 복합 알람 생성
aws cloudwatch put-composite-alarm \
    --alarm-name "EC2-Health-Check" \
    --alarm-rule "ALARM(High-CPU) OR ALARM(High-Memory)" \
    --alarm-actions arn:aws:sns:ap-northeast-2:xxx:my-topic
```

### 알람 액션

| 액션 유형 | 설명 |
|-----------|------|
| **SNS** | 알림 전송 |
| **Auto Scaling** | 스케일링 정책 실행 |
| **EC2** | 인스턴스 중지/종료/복구 |
| **Systems Manager** | 자동화 실행 |

## CloudWatch 대시보드

### 대시보드 생성

```bash
aws cloudwatch put-dashboard \
    --dashboard-name "EC2-Monitoring" \
    --dashboard-body '{
        "widgets": [
            {
                "type": "metric",
                "x": 0,
                "y": 0,
                "width": 12,
                "height": 6,
                "properties": {
                    "title": "CPU Utilization",
                    "view": "timeSeries",
                    "metrics": [
                        ["AWS/EC2", "CPUUtilization", "InstanceId", "i-xxxxxxxxx"]
                    ],
                    "region": "ap-northeast-2",
                    "period": 300
                }
            }
        ]
    }'
```

## 상태 검사

### 상태 검사 유형

| 유형 | 검사 대상 | 예시 |
|------|----------|------|
| ** 시스템 상태 검사** | AWS 인프라 | 네트워크, 전원, 하드웨어 |
| ** 인스턴스 상태 검사** | 인스턴스 OS | 커널 패닉, 메모리 부족 |
| **EBS 상태 검사** | EBS 볼륨 | 볼륨 손상, I/O 문제 |

### 상태 검사 확인

```bash
# 인스턴스 상태 확인
aws ec2 describe-instance-status \
    --instance-ids i-xxxxxxxxx \
    --include-all-instances
```

### 자동 복구 설정

```bash
# 상태 검사 실패 시 자동 복구
aws cloudwatch put-metric-alarm \
    --alarm-name "EC2-Auto-Recovery" \
    --namespace AWS/EC2 \
    --metric-name StatusCheckFailed_System \
    --dimensions Name=InstanceId,Value=i-xxxxxxxxx \
    --statistic Maximum \
    --period 60 \
    --evaluation-periods 2 \
    --threshold 1 \
    --comparison-operator GreaterThanOrEqualToThreshold \
    --alarm-actions arn:aws:automate:ap-northeast-2:ec2:recover
```

## CloudWatch Logs

### 로그 그룹 생성

```bash
aws logs create-log-group \
    --log-group-name /ec2/application/logs
```

### 로그 보존 기간 설정

```bash
aws logs put-retention-policy \
    --log-group-name /ec2/application/logs \
    --retention-in-days 30
```

### 로그 조회

```bash
# 최근 로그 이벤트 조회
aws logs get-log-events \
    --log-group-name /ec2/application/logs \
    --log-stream-name i-xxxxxxxxx \
    --limit 100
```

### 지표 필터

```bash
# ERROR 패턴을 지표로 변환
aws logs put-metric-filter \
    --log-group-name /ec2/application/logs \
    --filter-name ErrorCount \
    --filter-pattern "ERROR" \
    --metric-transformations \
        metricName=ErrorCount,metricNamespace=MyApp,metricValue=1
```

## CloudWatch Logs Insights

### 쿼리 예시

```sql
-- 최근 에러 로그
fields @timestamp, @message
| filter @message like /ERROR/
| sort @timestamp desc
| limit 100

-- 시간별 에러 카운트
filter @message like /ERROR/
| stats count(*) as error_count by bin(1h)

-- 응답 시간 분석
fields @timestamp, @message
| parse @message /response_time=(?<response_time>\d+)/
| stats avg(response_time) as avg_response, max(response_time) as max_response by bin(5m)
```

## EventBridge (CloudWatch Events)

### EC2 상태 변경 이벤트

```json
{
    "source": ["aws.ec2"],
    "detail-type": ["EC2 Instance State-change Notification"],
    "detail": {
        "state": ["stopped", "terminated"]
    }
}
```

### 이벤트 규칙 생성

```bash
aws events put-rule \
    --name "EC2-State-Change" \
    --event-pattern '{
        "source": ["aws.ec2"],
        "detail-type": ["EC2 Instance State-change Notification"]
    }' \
    --state ENABLED
```

## 모범 사례

### 1. 필수 모니터링 항목

```
┌─────────────────────────────────────────────────────────────┐
│  인프라 지표           │  애플리케이션 지표                   │
├─────────────────────────────────────────────────────────────┤
│  - CPU 사용률          │  - 응답 시간                        │
│  - 메모리 사용률       │  - 에러율                           │
│  - 디스크 사용률       │  - 요청 수                          │
│  - 네트워크 트래픽     │  - 처리량                           │
│  - 상태 검사           │  - 큐 길이                          │
└─────────────────────────────────────────────────────────────┘
```

### 2. 알람 설계

- 액션 가능한 알람만 생성
- 적절한 임계값 설정 (너무 민감하지 않게)
- 알람 설명에 대응 절차 포함

### 3. 비용 최적화

- 필요한 지표만 수집
- 로그 보존 기간 적절히 설정
- 오래된 대시보드 정리

## 참고 자료

- [CloudWatch 사용자 가이드](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/)
- [CloudWatch 에이전트](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Install-CloudWatch-Agent.html)
- [EC2 모니터링](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/monitoring_ec2.html)
