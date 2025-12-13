# AWS ALB 리스너 규칙

## 규칙 개요

리스너 규칙은 ** 조건(Conditions)** 과 ** 액션(Actions)** 으로 구성되며, 들어오는 요청을 어떻게 처리할지 결정합니다.

## 규칙 구조

```yaml
Rule:
  Priority: 1-50000 (숫자가 낮을수록 높은 우선순위)
  Conditions: 매칭 조건 (최대 5개)
  Actions: 수행할 작업 (최대 5개)
```

## 규칙 우선순위

- **1 ~ 50000** 범위의 숫자
- 숫자가 ** 낮을수록 우선순위 높음**
- 기본 규칙은 항상 마지막에 평가 (우선순위 없음)
- 여러 규칙 중 ** 첫 번째 매칭된 규칙** 만 실행

## 조건 유형 (Conditions)

### 호스트 헤더 (host-header)
```yaml
조건: 요청의 Host 헤더 값
예시:
  - *.example.com
  - api.example.com
  - www.example.com
와일드카드: * (단일 문자 이상), ? (단일 문자)
```

### 경로 패턴 (path-pattern)
```yaml
조건: 요청 URL의 경로
예시:
  - /images/*
  - /api/v1/*
  - /users/*/profile
와일드카드: * (단일 문자 이상), ? (단일 문자)
```

### HTTP 헤더 (http-header)
```yaml
조건: 특정 HTTP 헤더의 값
예시:
  헤더명: X-Custom-Header
  값: custom-value
대소문자: 헤더명은 대소문자 구분 없음, 값은 구분함
```

### HTTP 요청 메서드 (http-request-method)
```yaml
조건: HTTP 메서드
예시:
  - GET
  - POST
  - PUT
  - DELETE
```

### 쿼리 문자열 (query-string)
```yaml
조건: URL 쿼리 파라미터
예시:
  - version=v1
  - feature=enabled&user=admin
```

### 소스 IP (source-ip)
```yaml
조건: 클라이언트 IP 주소
예시:
  - 192.168.1.0/24
  - 10.0.0.0/8
형식: CIDR 블록
```

## 조건 조합

### AND 조합
하나의 규칙 내 여러 조건은 **AND** 로 결합됩니다.

```yaml
Rule:
  Conditions:
    - host-header: api.example.com  # AND
    - path-pattern: /v1/*           # AND
    - http-request-method: GET
```

### OR 조합
동일 유형의 조건 내 여러 값은 **OR** 로 결합됩니다.

```yaml
Condition:
  path-pattern:
    - /api/*    # OR
    - /v1/*     # OR
    - /v2/*
```

## 액션 유형 (Actions)

### Forward (전달)
```yaml
액션: forward
설명: 요청을 타겟 그룹으로 전달
옵션:
  - 단일 타겟 그룹
  - 가중치 기반 다중 타겟 그룹
  - 스티키 세션 설정
```

** 가중치 기반 라우팅:**
```yaml
Actions:
  - Type: forward
    ForwardConfig:
      TargetGroups:
        - TargetGroupArn: <tg-blue-arn>
          Weight: 80
        - TargetGroupArn: <tg-green-arn>
          Weight: 20
```

### Redirect (리다이렉트)
```yaml
액션: redirect
설명: 클라이언트를 다른 URL로 리다이렉트
상태 코드: 301 (영구) 또는 302 (임시)
```

**HTTP → HTTPS 리다이렉트:**
```yaml
Actions:
  - Type: redirect
    RedirectConfig:
      Protocol: HTTPS
      Port: "443"
      StatusCode: HTTP_301
```

**URL 리다이렉트:**
```yaml
Actions:
  - Type: redirect
    RedirectConfig:
      Host: "#{host}"
      Path: "/new-path/#{path}"
      Query: "#{query}"
      StatusCode: HTTP_302
```

### Fixed Response (고정 응답)
```yaml
액션: fixed-response
설명: 로드 밸런서가 직접 응답 반환
용도: 유지보수 페이지, 에러 페이지
```

** 예시:**
```yaml
Actions:
  - Type: fixed-response
    FixedResponseConfig:
      StatusCode: "503"
      ContentType: "text/html"
      MessageBody: "<h1>Service Unavailable</h1>"
```

### Authenticate (인증)
```yaml
액션: authenticate-oidc 또는 authenticate-cognito
설명: 요청 전에 사용자 인증 수행
위치: 반드시 forward 액션 전에 배치
```

## 규칙 생성

### AWS CLI
```bash
aws elbv2 create-rule \
    --listener-arn <listener-arn> \
    --priority 10 \
    --conditions Field=path-pattern,Values='/api/*' \
    --actions Type=forward,TargetGroupArn=<tg-arn>
```

### 호스트 기반 + 경로 기반 조합
```bash
aws elbv2 create-rule \
    --listener-arn <listener-arn> \
    --priority 20 \
    --conditions \
        Field=host-header,Values='api.example.com' \
        Field=path-pattern,Values='/v1/*' \
    --actions Type=forward,TargetGroupArn=<tg-arn>
```

## 규칙 수정

```bash
# 우선순위 변경
aws elbv2 set-rule-priorities \
    --rule-priorities RuleArn=<rule-arn>,Priority=5

# 규칙 조건/액션 수정
aws elbv2 modify-rule \
    --rule-arn <rule-arn> \
    --conditions Field=path-pattern,Values='/new-path/*' \
    --actions Type=forward,TargetGroupArn=<new-tg-arn>
```

## 규칙 삭제

```bash
aws elbv2 delete-rule \
    --rule-arn <rule-arn>
```

** 주의:** 기본 규칙은 삭제할 수 없습니다.

## 규칙 평가 순서

```
1. 요청 수신
2. 우선순위 순서대로 규칙 평가 (낮은 숫자부터)
3. 조건이 모두 매칭되는 첫 번째 규칙의 액션 실행
4. 매칭되는 규칙이 없으면 기본 규칙 실행
```

## 제한사항

| 항목 | 제한 |
|------|------|
| 규칙당 조건 수 | 5개 |
| 규칙당 조건 값 수 | 5개 |
| 규칙당 와일드카드 수 | 6개 |
| 호스트 조건 문자 수 | 128자 |
| 경로 조건 문자 수 | 128자 |

## 참고 자료

- [AWS 공식 문서 - 리스너 규칙](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/listener-update-rules.html)
