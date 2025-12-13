# AWS ALB 스티키 세션

## 개요

스티키 세션(세션 친화도)은 ** 동일한 클라이언트의 요청을 동일한 타겟으로 라우팅** 하는 기능입니다. 세션 상태를 유지해야 하는 애플리케이션에 유용합니다.

## 스티키 세션 유형

### 1. Duration-Based (기간 기반)

ALB가 생성한 쿠키(`AWSALB`)를 사용합니다.

```yaml
쿠키 이름: AWSALB
생성 주체: ALB
만료: 설정된 기간 후
특징:
  - 타겟 그룹 단위로 설정
  - 1초 ~ 7일 범위
  - 암호화되어 클라이언트가 수정 불가
```

### 2. Application-Based (애플리케이션 기반)

애플리케이션이 생성한 쿠키를 사용합니다.

```yaml
쿠키 이름: 사용자 정의
생성 주체: 애플리케이션
만료: 애플리케이션에서 설정
특징:
  - 기존 세션 쿠키 활용 가능
  - AWSALBAPP 쿠키도 함께 설정
  - 더 세밀한 제어 가능
```

## Duration-Based 스티키 세션 설정

### AWS 콘솔
1. 타겟 그룹 선택 → **Attributes** 탭
2. **Edit**
3. **Stickiness** 활성화
4. **Stickiness type**: Load balancer generated cookie
5. **Stickiness duration**: 기간 설정
6. **Save changes**

### AWS CLI
```bash
aws elbv2 modify-target-group-attributes \
    --target-group-arn <arn> \
    --attributes \
        Key=stickiness.enabled,Value=true \
        Key=stickiness.type,Value=lb_cookie \
        Key=stickiness.lb_cookie.duration_seconds,Value=86400
```

### 기간 설정

| 설정 | 범위 |
|------|------|
| 최소 | 1초 |
| 최대 | 604800초 (7일) |
| 권장 | 애플리케이션 세션 타임아웃과 동일하게 |

## Application-Based 스티키 세션 설정

### AWS 콘솔
1. 타겟 그룹 선택 → **Attributes** 탭
2. **Edit**
3. **Stickiness** 활성화
4. **Stickiness type**: Application-based cookie
5. **App cookie name**: 애플리케이션 쿠키 이름 입력
6. **Stickiness duration**: 기간 설정
7. **Save changes**

### AWS CLI
```bash
aws elbv2 modify-target-group-attributes \
    --target-group-arn <arn> \
    --attributes \
        Key=stickiness.enabled,Value=true \
        Key=stickiness.type,Value=app_cookie \
        Key=stickiness.app_cookie.cookie_name,Value=MY_SESSION_ID \
        Key=stickiness.app_cookie.duration_seconds,Value=86400
```

## 동작 방식

### Duration-Based 흐름
```
1. 클라이언트 → ALB: 첫 요청 (쿠키 없음)
2. ALB: 라우팅 알고리즘으로 타겟 선택
3. ALB → 클라이언트: AWSALB 쿠키 설정
4. 클라이언트 → ALB: 후속 요청 (AWSALB 쿠키 포함)
5. ALB: 쿠키의 타겟 정보로 동일 타겟 선택
```

### Application-Based 흐름
```
1. 클라이언트 → ALB: 첫 요청
2. ALB: 라우팅 알고리즘으로 타겟 선택
3. 타겟 → 클라이언트: 애플리케이션 쿠키 설정 (MY_SESSION_ID)
4. ALB → 클라이언트: AWSALBAPP 쿠키 추가 설정
5. 클라이언트 → ALB: 후속 요청 (두 쿠키 모두 포함)
6. ALB: AWSALBAPP 쿠키의 타겟 정보로 동일 타겟 선택
```

## 스티키 세션과 타겟 장애

### 타겟이 unhealthy가 되면

```yaml
동작:
  - 해당 타겟으로 더 이상 라우팅하지 않음
  - 새로운 타겟 선택
  - 새로운 스티키 세션 쿠키 발급
주의: 기존 세션 데이터 손실 가능
```

### 타겟이 해제(deregister)되면

```yaml
동작:
  - draining 기간 동안 기존 연결 유지
  - draining 완료 후 새 타겟 선택
  - 새로운 스티키 세션 쿠키 발급
```

## 가중치 기반 타겟 그룹과 스티키 세션

가중치 기반 라우팅에서 스티키 세션을 활성화하면:

```yaml
활성화 조건:
  - 그룹 수준 스티키 세션 활성화 필요
  - 타겟 그룹별 스티키 세션 설정은 무시됨

동작:
  - 첫 요청: 가중치에 따라 타겟 그룹 선택
  - 후속 요청: 동일한 타겟 그룹으로 라우팅
  - 쿠키: AWSALB로 타겟 그룹 정보 저장
```

### 설정
```bash
aws elbv2 modify-rule \
    --rule-arn <rule-arn> \
    --actions \
        Type=forward,\
        ForwardConfig='{
            "TargetGroups": [
                {"TargetGroupArn": "<tg1-arn>", "Weight": 80},
                {"TargetGroupArn": "<tg2-arn>", "Weight": 20}
            ],
            "TargetGroupStickinessConfig": {
                "Enabled": true,
                "DurationSeconds": 86400
            }
        }'
```

## 스티키 세션 쿠키 속성

### AWSALB (Duration-Based)
```yaml
속성:
  - HttpOnly: true (JavaScript 접근 불가)
  - Secure: HTTPS 리스너에서만
  - SameSite: None
  - Path: /
  - Expires: 설정된 기간
```

### AWSALBAPP (Application-Based)
```yaml
속성:
  - HttpOnly: true
  - Secure: HTTPS 리스너에서만
  - SameSite: None
  - Path: /
  - Expires: 설정된 기간
```

## 제한사항 및 고려사항

### 1. 부하 불균형
```yaml
문제: 스티키 세션으로 인해 특정 타겟에 부하 집중 가능
해결:
  - 세션 기간을 가능한 짧게 설정
  - 세션 저장소 외부화 고려 (Redis, DynamoDB)
```

### 2. WebSocket과의 호환성
```yaml
상태: WebSocket 연결은 스티키 세션과 무관
이유: WebSocket은 연결 기반이므로 자동으로 동일 타겟 유지
```

### 3. 크로스 존 로드 밸런싱
```yaml
동작: 스티키 세션은 크로스 존 설정과 독립적
결과: 다른 가용 영역의 타겟에도 스티키 세션 적용 가능
```

### 4. 예약된 쿠키 이름
```yaml
사용 금지:
  - AWSALB
  - AWSALBAPP
  - AWSALBTG
이유: ALB 내부 사용 쿠키
```

## 비활성화

```bash
aws elbv2 modify-target-group-attributes \
    --target-group-arn <arn> \
    --attributes Key=stickiness.enabled,Value=false
```

## 참고 자료

- [AWS 공식 문서 - 스티키 세션](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/sticky-sessions.html)
