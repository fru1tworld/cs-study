# AWS ALB와 Lambda 통합

## 개요

Application Load Balancer는 Lambda 함수를 타겟으로 등록하여 **HTTP 요청을 직접 Lambda 함수로 라우팅** 할 수 있습니다. 서버리스 아키텍처에서 API Gateway의 대안으로 사용됩니다.

## 특징

### 장점
```yaml
비용:
  - 요청 기반 요금
  - 유휴 시간에 비용 없음
  - API Gateway 대비 저렴할 수 있음

통합:
  - 기존 ALB 인프라 활용
  - 다른 타겟 유형과 혼합 사용 가능
  - 리스너 규칙으로 라우팅 제어
```

### ALB와 API Gateway 비교

| 기능 | ALB + Lambda | API Gateway |
|------|-------------|-------------|
| WebSocket | 미지원 | 지원 |
| 요청 변환 | 제한적 | 풍부함 |
| 캐싱 | 미지원 | 지원 |
| 스로틀링 | 미지원 | 지원 |
| 인증 | ALB 인증 사용 | 다양한 옵션 |
| 비용 | 요청당 낮음 | 요청당 높음 |

## 제한사항

| 항목 | 제한 |
|------|------|
| 요청 본문 최대 크기 | 1MB |
| 응답 JSON 최대 크기 | 1MB |
| 타겟 그룹당 Lambda 함수 | 1개 |
| WebSocket | 미지원 |
| Local Zones | 미지원 |
| 자동 타겟 가중치(ATW) | 미지원 |

## Lambda 함수 준비

### 요청 형식

ALB가 Lambda 함수에 전달하는 이벤트 형식:

```json
{
    "requestContext": {
        "elb": {
            "targetGroupArn": "arn:aws:elasticloadbalancing:..."
        }
    },
    "httpMethod": "GET",
    "path": "/api/users",
    "queryStringParameters": {
        "id": "123"
    },
    "headers": {
        "host": "example.com",
        "user-agent": "Mozilla/5.0...",
        "accept": "application/json"
    },
    "body": null,
    "isBase64Encoded": false
}
```

### 응답 형식

Lambda 함수가 반환해야 하는 응답 형식:

```json
{
    "statusCode": 200,
    "statusDescription": "200 OK",
    "headers": {
        "Content-Type": "application/json"
    },
    "isBase64Encoded": false,
    "body": "{\"message\": \"Hello, World!\"}"
}
```

### 응답 필드 설명

| 필드 | 필수 | 설명 |
|------|------|------|
| `statusCode` | Yes | HTTP 상태 코드 (200, 404 등) |
| `statusDescription` | No | 상태 설명 |
| `headers` | No | 응답 헤더 |
| `multiValueHeaders` | No | 다중 값 헤더 |
| `body` | No | 응답 본문 |
| `isBase64Encoded` | No | 본문 인코딩 여부 |

### Lambda 함수 예시 (Python)

```python
import json

def lambda_handler(event, context):
    # 요청 정보 추출
    http_method = event.get('httpMethod')
    path = event.get('path')
    query_params = event.get('queryStringParameters', {})
    body = event.get('body')

    # 비즈니스 로직 처리
    response_body = {
        'message': 'Hello from Lambda!',
        'method': http_method,
        'path': path
    }

    # 응답 반환
    return {
        'statusCode': 200,
        'headers': {
            'Content-Type': 'application/json'
        },
        'body': json.dumps(response_body),
        'isBase64Encoded': False
    }
```

### Lambda 함수 예시 (Node.js)

```javascript
exports.handler = async (event) => {
    const response = {
        statusCode: 200,
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            message: 'Hello from Lambda!',
            path: event.path
        }),
        isBase64Encoded: false
    };
    return response;
};
```

## 타겟 그룹 설정

### 1. 타겟 그룹 생성

**AWS 콘솔:**
1. EC2 콘솔 → **Target Groups** → **Create target group**
2. **Target type**: Lambda function
3. 이름 입력
4. **Health checks** 구성 (선택)
5. **Next**

**AWS CLI:**
```bash
aws elbv2 create-target-group \
    --name my-lambda-tg \
    --target-type lambda
```

### 2. Lambda 권한 부여

ALB가 Lambda 함수를 호출할 수 있도록 권한 추가:

```bash
aws lambda add-permission \
    --function-name my-function \
    --statement-id elb-invoke \
    --action lambda:InvokeFunction \
    --principal elasticloadbalancing.amazonaws.com \
    --source-arn <target-group-arn>
```

### 3. Lambda 함수 등록

**AWS CLI:**
```bash
aws elbv2 register-targets \
    --target-group-arn <tg-arn> \
    --targets Id=arn:aws:lambda:us-east-1:123456789012:function:my-function
```

### 함수 버전/별칭 사용

특정 버전이나 별칭을 타겟으로 등록:

```bash
# 특정 버전
aws elbv2 register-targets \
    --target-group-arn <tg-arn> \
    --targets Id=arn:aws:lambda:us-east-1:123456789012:function:my-function:1

# 별칭 (권장)
aws elbv2 register-targets \
    --target-group-arn <tg-arn> \
    --targets Id=arn:aws:lambda:us-east-1:123456789012:function:my-function:prod
```

## 헬스 체크

### 설정

Lambda 타겟에 대한 헬스 체크는 ** 선택 사항** 입니다.

```bash
aws elbv2 modify-target-group \
    --target-group-arn <tg-arn> \
    --health-check-enabled \
    --health-check-path /health \
    --health-check-interval-seconds 35 \
    --health-check-timeout-seconds 30 \
    --healthy-threshold-count 5 \
    --unhealthy-threshold-count 2 \
    --matcher HttpCode=200
```

### 기본값

| 설정 | Lambda 기본값 |
|------|-------------|
| 간격 | 35초 |
| 타임아웃 | 30초 |
| 정상 임계값 | 5 |
| 비정상 임계값 | 2 |

## Multi-Value Headers

### 활성화

다중 값 헤더를 지원하려면:

```bash
aws elbv2 modify-target-group-attributes \
    --target-group-arn <tg-arn> \
    --attributes Key=lambda.multi_value_headers.enabled,Value=true
```

### 요청 형식 변경

활성화 후 요청 형식:
```json
{
    "multiValueQueryStringParameters": {
        "ids": ["1", "2", "3"]
    },
    "multiValueHeaders": {
        "accept": ["application/json", "text/html"]
    }
}
```

### 응답 형식 변경

```json
{
    "statusCode": 200,
    "multiValueHeaders": {
        "Set-Cookie": ["cookie1=value1", "cookie2=value2"]
    },
    "body": "response body"
}
```

## ALB가 무시하는 헤더

Lambda 응답의 다음 헤더는 ALB가 무시합니다:

- `Connection`
- `Transfer-Encoding`
- `Upgrade`
- `Expect`
- `Proxy-Connection`
- `TE`
- `Keep-Alive`

이러한 hop-by-hop 헤더는 ALB가 자체적으로 관리합니다.

## 에러 처리

### Lambda 에러 시 ALB 동작

| Lambda 상태 | ALB 응답 |
|------------|---------|
| 함수 에러 | 502 Bad Gateway |
| 타임아웃 | 504 Gateway Timeout |
| 권한 오류 | 502 Bad Gateway |
| 잘못된 응답 형식 | 502 Bad Gateway |

### 에러 응답 커스터마이징

Lambda 함수에서 에러 응답을 직접 반환:

```python
def lambda_handler(event, context):
    try:
        # 비즈니스 로직
        result = process_request(event)
        return {
            'statusCode': 200,
            'body': json.dumps(result)
        }
    except ValueError as e:
        return {
            'statusCode': 400,
            'body': json.dumps({'error': str(e)})
        }
    except Exception as e:
        return {
            'statusCode': 500,
            'body': json.dumps({'error': 'Internal Server Error'})
        }
```

## 참고 자료

- [AWS 공식 문서 - Lambda 타겟](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/lambda-functions.html)
