# AWS ALB 로드 밸런서 구성

## 로드 밸런서 기본 역할

로드 밸런서는 **클라이언트의 단일 연결 지점**으로 작동하며, 들어오는 요청을 여러 가용 영역의 대상으로 분산합니다.

## 로드 밸런서 상태

| 상태 | 설명 |
|------|------|
| `provisioning` | 로드 밸런서 설정 중 |
| `active` | 트래픽 라우팅 준비 완료 |
| `active_impaired` | 라우팅 중이나 리소스 부족 |
| `failed` | 설정 실패 |

## 스킴 (Scheme)

### Internet-facing
```yaml
특징:
  - 퍼블릭 IP 주소 보유
  - 인터넷에서 접근 가능
  - 퍼블릭 서브넷에 배치

사용 사례:
  - 웹 애플리케이션
  - 외부 API 서비스
```

### Internal
```yaml
특징:
  - 프라이빗 IP 주소만 보유
  - VPC 내부에서만 접근 가능
  - 프라이빗 서브넷에 배치

사용 사례:
  - 내부 마이크로서비스
  - 백엔드 API
  - 다중 계층 아키텍처의 중간 계층
```

## 서브넷 요구사항

### 기본 요구사항
- **최소 2개의 가용 영역** 필요
- 각 서브넷: 최소 `/27` CIDR 블록
- **8개 이상의 여유 IP 주소** 필요

### 지원되는 서브넷 유형
| 유형 | 설명 |
|------|------|
| 일반 서브넷 | 표준 AZ 서브넷 |
| 로컬 존 서브넷 | AWS Local Zones |
| 아웃포스트 서브넷 | AWS Outposts |

## IP 주소 유형

### IPv4
```yaml
설명: 클라이언트가 IPv4 주소로만 연결
DNS 레코드: A 레코드 반환
```

### Dualstack
```yaml
설명: IPv4 및 IPv6 모두 지원
DNS 레코드: A 및 AAAA 레코드 반환
요구사항: 연결된 서브넷이 IPv6 CIDR 블록 필요
```

### Dualstack-without-public-ipv4
```yaml
설명: IPv6만 지원 (퍼블릭 IPv4 없음)
사용 사례: IPv6 전용 환경
```

## 보안 그룹 구성

### 필수 인바운드 규칙
```yaml
# HTTP 트래픽
- Protocol: TCP
  Port: 80
  Source: 0.0.0.0/0 (또는 특정 CIDR)

# HTTPS 트래픽
- Protocol: TCP
  Port: 443
  Source: 0.0.0.0/0 (또는 특정 CIDR)
```

### 필수 아웃바운드 규칙
```yaml
# 타겟으로의 트래픽
- Protocol: TCP
  Port: <타겟 포트>
  Destination: <타겟 보안 그룹 또는 CIDR>

# 헬스 체크 트래픽
- Protocol: TCP
  Port: <헬스 체크 포트>
  Destination: <타겟 보안 그룹 또는 CIDR>
```

## 주요 속성

### 삭제 보호 (Deletion Protection)
```yaml
설명: 실수로 인한 삭제 방지
기본값: 비활성화
설정 방법:
  - 콘솔: Actions → Edit load balancer attributes
  - CLI: modify-load-balancer-attributes
```

### 유휴 타임아웃 (Idle Timeout)
```yaml
설명: 연결이 유휴 상태로 유지될 수 있는 시간
기본값: 60초
범위: 1-4000초
참고: 타겟의 keep-alive 설정보다 작게 설정 권장
```

### HTTP/2 지원
```yaml
설명: HTTPS 리스너에서 HTTP/2 프로토콜 지원
기본값: 활성화
특징:
  - 하나의 연결로 최대 128개 병렬 요청
  - 헤더 압축
  - 서버 푸시 지원
```

### 크로스 존 로드 밸런싱
```yaml
설명: 모든 가용 영역의 타겟에 균등하게 트래픽 분산
기본값: 활성화
참고: 비활성화 시 각 AZ 내에서만 트래픽 분산
```

### Desync 완화 모드
```yaml
옵션:
  - defensive: 의심스러운 요청 거부 (기본값)
  - strictest: 모든 비정상 요청 거부
  - monitor: 모니터링만, 요청은 허용

설명: HTTP 요청 밀수 공격 방어
```

## 로드 밸런서 수정

### 서브넷 변경
```bash
aws elbv2 set-subnets \
    --load-balancer-arn <arn> \
    --subnets subnet-111 subnet-222
```

### 보안 그룹 변경
```bash
aws elbv2 set-security-groups \
    --load-balancer-arn <arn> \
    --security-groups sg-111 sg-222
```

### 속성 수정
```bash
aws elbv2 modify-load-balancer-attributes \
    --load-balancer-arn <arn> \
    --attributes Key=idle_timeout.timeout_seconds,Value=120
```

## 태그 관리

```bash
# 태그 추가
aws elbv2 add-tags \
    --resource-arns <arn> \
    --tags Key=Environment,Value=Production

# 태그 제거
aws elbv2 remove-tags \
    --resource-arns <arn> \
    --tag-keys Environment
```

## DNS 및 접근

### ALB DNS 이름 형식
```
<name>-<id>.<region>.elb.amazonaws.com
```

### 커스텀 도메인 연결
1. **Route 53 Alias 레코드**: ALB를 직접 타겟으로 지정
2. **CNAME 레코드**: ALB DNS 이름을 값으로 설정

**주의사항:**
- ALB 기본 DNS 이름으로는 HTTPS 인증서 발급 불가
- 퍼블릭 인증서는 `*.amazonaws.com` 도메인에 발급되지 않음

## 참고 자료

- [AWS 공식 문서 - ALB 구성](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/application-load-balancers.html)
