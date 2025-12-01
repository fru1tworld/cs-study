# AWS ALB 요청 추적

## 개요

Application Load Balancer는 `X-Amzn-Trace-Id` 헤더를 사용하여 **요청을 추적**합니다. 이를 통해 분산 시스템에서 요청의 흐름을 파악하고 문제를 진단할 수 있습니다.

## X-Amzn-Trace-Id 헤더

### 헤더 형식
```
X-Amzn-Trace-Id: Field=version-time-id
```

### 구성 요소

| 구성 요소 | 설명 | 예시 |
|----------|------|------|
| Field | 필드 유형 (Root, Self) | Root, Self |
| version | 버전 (항상 1) | 1 |
| time | 요청 시간 (epoch, 16진수 8자리) | 67891233 |
| id | 추적 식별자 (16진수 24자리) | abcdef012345678912345678 |

### 예시
```
X-Amzn-Trace-Id: Root=1-67891233-abcdef012345678912345678
```

## ALB의 헤더 처리 동작

### 시나리오 1: 헤더가 없는 경우

클라이언트가 헤더 없이 요청하면 ALB가 새로 생성합니다.

```yaml
클라이언트 요청:
  - 헤더 없음

ALB 처리:
  - Root 필드 생성

타겟으로 전달:
  X-Amzn-Trace-Id: Root=1-67891233-abcdef012345678912345678
```

### 시나리오 2: Root 헤더가 있는 경우

클라이언트가 Root 헤더를 포함하면 ALB가 Self 필드를 추가합니다.

```yaml
클라이언트 요청:
  X-Amzn-Trace-Id: Root=1-67891233-abcdef012345678912345678

ALB 처리:
  - 기존 Root 유지
  - Self 필드 추가

타겟으로 전달:
  X-Amzn-Trace-Id: Self=1-67891234-zyxwvutsrqponmlkjihgfedc;Root=1-67891233-abcdef012345678912345678
```

### 시나리오 3: 커스텀 필드가 있는 경우

애플리케이션이 추가한 커스텀 필드는 유지됩니다.

```yaml
클라이언트 요청:
  X-Amzn-Trace-Id: Root=1-67891233-abcdef012345678912345678;CustomField=value

ALB 처리:
  - 기존 필드 유지
  - Self 필드 추가

타겟으로 전달:
  X-Amzn-Trace-Id: Self=1-67891234-zyxwvutsrqponmlkjihgfedc;Root=1-67891233-abcdef012345678912345678;CustomField=value
```

## 헤더 크기 제한

### 최대 크기
```yaml
최대 헤더 크기: 7KB

초과 시 동작:
  - 기존 헤더 삭제
  - 새 Root 필드만 생성
```

## AWS X-Ray 통합

### X-Ray와 함께 사용

ALB의 추적 ID는 AWS X-Ray와 호환됩니다.

```yaml
X-Ray 트레이스 ID 형식:
  Root=1-<timestamp>-<identifier>

통합 방법:
  1. 애플리케이션에 X-Ray SDK 설치
  2. X-Amzn-Trace-Id 헤더에서 Root ID 추출
  3. X-Ray에 트레이스 정보 전송
```

### 애플리케이션 코드 예시 (Python)

```python
from aws_xray_sdk.core import xray_recorder
from aws_xray_sdk.core import patch_all

# 모든 라이브러리 패치
patch_all()

def handler(request):
    # X-Amzn-Trace-Id 헤더에서 추적 정보 추출
    trace_header = request.headers.get('X-Amzn-Trace-Id')

    if trace_header:
        # 추적 컨텍스트 설정
        xray_recorder.put_trace_header(trace_header)

    # 비즈니스 로직 수행
    result = process_request(request)

    return result
```

### 애플리케이션 코드 예시 (Node.js)

```javascript
const AWSXRay = require('aws-xray-sdk');

app.use((req, res, next) => {
    const traceHeader = req.headers['x-amzn-trace-id'];

    if (traceHeader) {
        // 추적 ID 파싱
        const traceData = AWSXRay.utils.processTraceData(traceHeader);
        // 세그먼트에 연결
        AWSXRay.setTraceData(traceData);
    }

    next();
});
```

## WebSocket 추적

### 제한사항
```yaml
WebSocket 추적:
  - Upgrade 요청까지만 추적 가능
  - WebSocket 연결 수립 후 추적 종료
  - 양방향 메시지는 추적되지 않음
```

## 로그에서 추적 ID 활용

### 액세스 로그

액세스 로그의 `trace_id` 필드에 기록됩니다:
```
... "Root=1-67891233-abcdef012345678912345678" ...
```

### 애플리케이션 로그 연결

```python
import logging

def handler(request):
    trace_id = request.headers.get('X-Amzn-Trace-Id', 'unknown')

    # 로그에 추적 ID 포함
    logging.info(f"[{trace_id}] Processing request...")
```

### CloudWatch Logs Insights 쿼리

```sql
-- 특정 추적 ID로 로그 검색
fields @timestamp, @message
| filter @message like /Root=1-67891233-abcdef012345678912345678/
| sort @timestamp desc
| limit 100
```

## 분산 추적 구현

### 아키텍처 예시

```
클라이언트
    ↓
    ↓ X-Amzn-Trace-Id: Root=1-xxx
    ↓
[ALB]
    ↓
    ↓ X-Amzn-Trace-Id: Self=1-yyy;Root=1-xxx
    ↓
[서비스 A]
    ↓
    ↓ 동일한 헤더 전파
    ↓
[서비스 B]
    ↓
    ↓ 동일한 헤더 전파
    ↓
[데이터베이스]
```

### 헤더 전파 코드

```python
import requests

def call_downstream_service(request, url):
    # ALB에서 받은 추적 헤더 전파
    trace_header = request.headers.get('X-Amzn-Trace-Id')

    headers = {}
    if trace_header:
        headers['X-Amzn-Trace-Id'] = trace_header

    response = requests.get(url, headers=headers)
    return response
```

## 추적 비활성화

### 비활성화 방법

ALB의 요청 추적은 비활성화할 수 없습니다. 하지만 애플리케이션에서 무시할 수 있습니다.

```yaml
주의:
  - ALB는 항상 X-Amzn-Trace-Id 헤더를 추가/수정
  - 응답에서는 헤더가 수정되지 않음
  - 애플리케이션에서 헤더를 제거해도 다음 요청에서 다시 추가됨
```

## 모범 사례

### 1. 모든 서비스에서 헤더 전파

```yaml
이유:
  - 완전한 요청 경로 추적 가능
  - 문제 발생 시 빠른 원인 파악
  - 성능 병목 지점 식별
```

### 2. 로그에 추적 ID 포함

```yaml
형식: [trace-id] 메시지
예시: [Root=1-67891233-xxx] User login successful
```

### 3. X-Ray와 통합

```yaml
장점:
  - 시각적 서비스 맵
  - 자동 지연 분석
  - 오류 추적 및 알림
```

## 참고 자료

- [AWS 공식 문서 - 요청 추적](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-request-tracing.html)
- [AWS X-Ray 개발자 가이드](https://docs.aws.amazon.com/xray/latest/devguide/)
