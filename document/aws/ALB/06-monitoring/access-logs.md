# AWS ALB 액세스 로그

## 개요

ALB 액세스 로그는 로드 밸런서로 전송된 ** 모든 요청에 대한 상세 정보** 를 캡처합니다. 트래픽 패턴 분석, 문제 해결, 보안 감사에 활용됩니다.

## 특징

```yaml
기본 상태: 비활성화
생성 주기: 5분마다
저장 위치: Amazon S3
압축: gzip
비용: S3 저장소 비용만 발생 (대역폭 비용 없음)
```

## 활성화

### AWS 콘솔
1. EC2 콘솔 → Load Balancers → ALB 선택
2. **Attributes** 탭 → **Edit**
3. **Access logs** 섹션 활성화
4. S3 버킷 위치 입력
5. (선택) 프리픽스 입력
6. **Save changes**

### AWS CLI
```bash
aws elbv2 modify-load-balancer-attributes \
    --load-balancer-arn <alb-arn> \
    --attributes \
        Key=access_logs.s3.enabled,Value=true \
        Key=access_logs.s3.bucket,Value=my-log-bucket \
        Key=access_logs.s3.prefix,Value=alb-logs
```

## S3 버킷 정책

ALB가 로그를 저장하려면 S3 버킷에 적절한 권한이 필요합니다:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::<elb-account-id>:root"
            },
            "Action": "s3:PutObject",
            "Resource": "arn:aws:s3:::my-log-bucket/alb-logs/AWSLogs/<your-account-id>/*"
        }
    ]
}
```

### 리전별 ELB 계정 ID

| 리전 | 계정 ID |
|------|---------|
| us-east-1 | 127311923021 |
| us-east-2 | 033677994240 |
| us-west-1 | 027434742980 |
| us-west-2 | 797873946194 |
| eu-west-1 | 156460612806 |
| ap-northeast-1 | 582318560864 |
| ap-northeast-2 | 600734575887 |

## 로그 파일 위치

### 경로 형식
```
bucket/prefix/AWSLogs/account-id/elasticloadbalancing/region/yyyy/mm/dd/
```

### 파일명 형식
```
account-id_elasticloadbalancing_region_app.load-balancer-id_end-time_ip-address_random-string.log.gz
```

### 예시
```
my-bucket/alb-logs/AWSLogs/123456789012/elasticloadbalancing/us-east-1/2024/01/15/
123456789012_elasticloadbalancing_us-east-1_app.my-alb-1234567890abcdef_20240115T1200Z_10.0.0.1_abcdef12.log.gz
```

## 로그 엔트리 형식

### 필드 목록 (총 33개)

| 인덱스 | 필드 | 설명 |
|-------|------|------|
| 1 | type | 요청 유형 (http, https, h2, grpcs, ws, wss) |
| 2 | time | 응답 시간 (ISO 8601) |
| 3 | elb | ALB 리소스 ID |
| 4 | client:port | 클라이언트 IP:포트 |
| 5 | target:port | 타겟 IP:포트 |
| 6 | request_processing_time | 요청 수신 ~ 타겟 전송 시간 |
| 7 | target_processing_time | 타겟에서 처리 시간 |
| 8 | response_processing_time | 타겟 응답 ~ 클라이언트 전송 시간 |
| 9 | elb_status_code | ALB 응답 상태 코드 |
| 10 | target_status_code | 타겟 응답 상태 코드 |
| 11 | received_bytes | 수신 바이트 |
| 12 | sent_bytes | 전송 바이트 |
| 13 | request | 요청 라인 (메서드 URL 프로토콜) |
| 14 | user_agent | User-Agent 헤더 |
| 15 | ssl_cipher | SSL 암호화 제품군 (HTTPS만) |
| 16 | ssl_protocol | SSL 프로토콜 (HTTPS만) |
| 17 | target_group_arn | 타겟 그룹 ARN |
| 18 | trace_id | X-Amzn-Trace-Id 값 |
| 19 | domain_name | SNI 도메인 |
| 20 | chosen_cert_arn | 선택된 인증서 ARN |
| 21 | matched_rule_priority | 매칭된 규칙 우선순위 |
| 22 | request_creation_time | 요청 수신 시간 |
| 23 | actions_executed | 실행된 액션 |
| 24 | redirect_url | 리다이렉트 URL |
| 25 | error_reason | 에러 사유 |
| 26 | target:port_list | 다중 타겟 목록 |
| 27 | target_status_code_list | 다중 타겟 상태 코드 |
| 28 | classification | 분류 |
| 29 | classification_reason | 분류 사유 |
| 30 | conn_trace_id | 연결 추적 ID |
| 31 | target_processing_time_list | 다중 타겟 처리 시간 |
| 32 | request_header_fields | 요청 헤더 필드 |
| 33 | response_header_fields | 응답 헤더 필드 |

### 로그 예시

```
https 2024-01-15T12:00:00.123456Z app/my-alb/1234567890abcdef 203.0.113.1:54321 10.0.1.10:80 0.001 0.015 0.000 200 200 125 528 "GET https://www.example.com:443/api/users HTTP/2.0" "Mozilla/5.0..." ECDHE-RSA-AES128-GCM-SHA256 TLSv1.2 arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/my-tg/1234567890abcdef "Root=1-67891233-abcdef012345678912345678" "www.example.com" "arn:aws:acm:us-east-1:123456789012:certificate/abcd1234-ef56-gh78-ij90-klmnop123456" 0 2024-01-15T12:00:00.107456Z "forward" "-" "-" "10.0.1.10:80" "200" "-" "-" "-" "-" "-"
```

## 로그 분석

### Amazon Athena 활용

** 테이블 생성:**
```sql
CREATE EXTERNAL TABLE IF NOT EXISTS alb_logs (
    type string,
    time string,
    elb string,
    client_ip string,
    target_ip string,
    request_processing_time double,
    target_processing_time double,
    response_processing_time double,
    elb_status_code int,
    target_status_code string,
    received_bytes bigint,
    sent_bytes bigint,
    request_verb string,
    request_url string,
    request_proto string,
    user_agent string,
    ssl_cipher string,
    ssl_protocol string,
    target_group_arn string,
    trace_id string,
    domain_name string,
    chosen_cert_arn string,
    matched_rule_priority string,
    request_creation_time string,
    actions_executed string,
    redirect_url string,
    lambda_error_reason string,
    target_port_list string,
    target_status_code_list string,
    classification string,
    classification_reason string
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.RegexSerDe'
WITH SERDEPROPERTIES (
 'serialization.format' = '1',
 'input.regex' = '([^ ]*) ([^ ]*) ([^ ]*) ([^ ]*):([0-9]*) ([^ ]*)[:-]([0-9]*) ([-.0-9]*) ([-.0-9]*) ([-.0-9]*) (|[-0-9]*) (-|[-0-9]*) ([-0-9]*) ([-0-9]*) \"([^ ]*) ([^ ]*) (- |[^ ]*)\" \"([^\"]*)\" ([A-Z0-9-]+) ([A-Za-z0-9.-]*) ([^ ]*) \"([^\"]*)\" \"([^\"]*)\" \"([^\"]*)\" ([-.0-9]*) ([^ ]*) \"([^\"]*)\" \"([^\"]*)\" \"([^ ]*)\" \"([^\s]+?)\" \"([^\s]+)\" \"([^ ]*)\" \"([^ ]*)\"')
LOCATION 's3://my-log-bucket/alb-logs/AWSLogs/123456789012/elasticloadbalancing/us-east-1/';
```

** 쿼리 예시:**

```sql
-- 상위 10개 클라이언트 IP
SELECT client_ip, COUNT(*) as requests
FROM alb_logs
GROUP BY client_ip
ORDER BY requests DESC
LIMIT 10;

-- 5xx 에러 분석
SELECT time, client_ip, request_url, elb_status_code, target_status_code
FROM alb_logs
WHERE elb_status_code >= 500
ORDER BY time DESC
LIMIT 100;

-- 평균 응답 시간
SELECT
    DATE(from_iso8601_timestamp(time)) as date,
    AVG(target_processing_time) as avg_response_time
FROM alb_logs
GROUP BY DATE(from_iso8601_timestamp(time))
ORDER BY date;
```

## 처리 시간 분석

### 지연 구간 이해

```yaml
request_processing_time:
  시작: ALB가 요청 수신
  종료: 타겟으로 요청 전송
  높은 값 원인: 큐 대기, 처리 지연

target_processing_time:
  시작: 타겟으로 요청 전송
  종료: 타겟에서 응답 수신
  높은 값 원인: 애플리케이션 지연, DB 지연

response_processing_time:
  시작: 타겟 응답 수신
  종료: 클라이언트로 응답 전송
  높은 값 원인: 응답 크기, 네트워크 지연
```

### 값이 -1인 경우

```yaml
-1 의미: 해당 단계가 완료되지 않음
원인:
  - 연결 실패
  - 타임아웃
  - 클라이언트 연결 종료
```

## 주의사항

```yaml
베스트 에포트:
  - 모든 요청이 기록된다고 보장하지 않음
  - 트래픽 패턴 분석 목적으로 사용
  - 정확한 회계 목적으로는 부적합

헬스 체크:
  - 헬스 체크 요청은 로그에 포함되지 않음
```

## 참고 자료

- [AWS 공식 문서 - 액세스 로그](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-access-logs.html)
