# AWS ALB 리스너 개요

## 리스너란?

리스너는 구성된 ** 프로토콜과 포트** 를 사용하여 클라이언트 연결 요청을 확인하는 프로세스입니다. ALB에 트래픽을 받으려면 최소 하나의 리스너가 필요합니다.

## 지원 프로토콜

| 프로토콜 | 포트 범위 | 설명 |
|---------|----------|------|
| HTTP | 1-65535 | 기본 HTTP 트래픽 |
| HTTPS | 1-65535 | SSL/TLS 암호화 트래픽 |

## 리스너 구성 요소

### 기본 구성
```yaml
Listener:
  Protocol: HTTP | HTTPS
  Port: 1-65535
  Default Action: 필수 (forward, redirect, fixed-response 중 택일)
  Rules: 추가 라우팅 규칙 (선택)
```

### 기본 액션 (Default Action)
- 모든 리스너는 ** 기본 규칙** 을 가짐
- 기본 규칙은 삭제할 수 없음
- 다른 규칙에 매칭되지 않으면 기본 액션 실행

## HTTP/2 지원

### 특징
- HTTPS 리스너에서 기본 활성화
- 하나의 연결로 ** 최대 128개의 병렬 요청** 처리
- 헤더 압축 지원
- 타겟으로는 HTTP/1.1로 변환하여 전송

### 비활성화 방법
```bash
aws elbv2 modify-load-balancer-attributes \
    --load-balancer-arn <arn> \
    --attributes Key=routing.http2.enabled,Value=false
```

## WebSocket 지원

### 동작 방식
```
1. 클라이언트 → ALB: HTTP/1.1 Upgrade 요청
2. ALB → 타겟: Upgrade 헤더 전달
3. 타겟 → ALB: 101 Switching Protocols 응답
4. WebSocket 연결 수립 완료
```

### 지원 프로토콜
- `ws://` - HTTP 리스너
- `wss://` - HTTPS 리스너

### 제한사항
- 헬스 체크는 WebSocket을 지원하지 않음
- 요청 추적(X-Amzn-Trace-Id)은 업그레이드 요청까지만 적용

## gRPC 지원

### 요구사항
- HTTPS 리스너 필수
- HTTP/2 활성화 필요
- 타겟 그룹 프로토콜: HTTP/2 또는 gRPC

### 특징
- 단항(Unary) 및 스트리밍 RPC 지원
- gRPC 상태 코드로 헬스 체크 가능

## 리스너 속성

### 응답 헤더 설정
| 속성 | 설명 |
|------|------|
| CORS 헤더 | Access-Control-Allow-* 헤더 자동 삽입 |
| CSP 헤더 | Content-Security-Policy 헤더 |
| HSTS 헤더 | Strict-Transport-Security 헤더 |

### 요청 헤더 수정
| 속성 | 설명 |
|------|------|
| TLS 정보 | X-Amzn-TLS-* 헤더 삽입 |
| mTLS 인증서 | 클라이언트 인증서 정보 헤더 |

## 리스너 생성

### AWS 콘솔
1. EC2 콘솔 → Load Balancers → 대상 ALB 선택
2. **Listeners and rules** 탭
3. **Add listener** 클릭
4. 프로토콜, 포트, 기본 액션 구성

### AWS CLI
```bash
# HTTP 리스너 생성
aws elbv2 create-listener \
    --load-balancer-arn <lb-arn> \
    --protocol HTTP \
    --port 80 \
    --default-actions Type=forward,TargetGroupArn=<tg-arn>
```

## 리스너 삭제

### 삭제 시 영향
- ** 즉시** 해당 포트에서 새 연결 수락 중지
- 활성 연결도 종료됨
- 진행 중인 요청 실패 가능

### AWS 콘솔
1. 리스너 선택 → **Manage listener** → **Delete listener**
2. 확인창에 `confirm` 입력

### AWS CLI
```bash
aws elbv2 delete-listener \
    --listener-arn <listener-arn>
```

## 제한사항

| 항목 | 제한 |
|------|------|
| ALB당 리스너 수 | 50개 (조정 가능) |
| ALB당 규칙 수 | 100개 (조정 가능) |
| 규칙당 조건 수 | 5개 (조정 불가) |

## 참고 자료

- [AWS 공식 문서 - 리스너](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-listeners.html)
