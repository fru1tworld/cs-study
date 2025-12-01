# AWS ALB 로드 밸런서 삭제

## 삭제 전 확인사항

### 1. 삭제 보호 확인
삭제 보호가 활성화되어 있으면 먼저 비활성화해야 합니다.

```bash
# 삭제 보호 비활성화
aws elbv2 modify-load-balancer-attributes \
    --load-balancer-arn <arn> \
    --attributes Key=deletion_protection.enabled,Value=false
```

### 2. DNS 레코드 업데이트

도메인의 DNS 레코드가 로드 밸런서를 가리키고 있다면, ** 삭제 전에 새 위치로 변경** 해야 합니다.

| 레코드 유형 | 대기 시간 |
|------------|----------|
| CNAME (TTL 300초) | 최소 300초 |
| Route 53 Alias (A) | 최소 60초 |
| Route 53 전파 시간 | TTL + 60초 |

### 3. 삭제 영향 범위

- ** 로드 밸런서 삭제 시:**
  - 즉시 요금 청구 중단
  - 모든 리스너 자동 삭제
  - 연결된 타겟 그룹은 ** 삭제되지 않음**
  - EC2 인스턴스는 계속 실행됨

## 삭제 방법

### AWS 콘솔

1. EC2 콘솔 → **Load Balancers** 선택
2. 삭제할 로드 밸런서 선택
3. **Actions** → **Delete load balancer**
4. 확인창에 `confirm` 입력
5. **Delete** 클릭

### AWS CLI

```bash
# 로드 밸런서 삭제
aws elbv2 delete-load-balancer \
    --load-balancer-arn arn:aws:elasticloadbalancing:us-east-1:123456789012:loadbalancer/app/my-alb/50dc6c495c0c9188
```

### CloudFormation

스택에서 `AWS::ElasticLoadBalancingV2::LoadBalancer` 리소스를 제거하거나 스택 전체를 삭제합니다.

## 삭제 후 정리

### 타겟 그룹 삭제 (선택)

로드 밸런서 삭제 후 타겟 그룹도 정리하려면:

```bash
# 타겟 그룹 삭제
aws elbv2 delete-target-group \
    --target-group-arn <target-group-arn>
```

### 보안 그룹 정리 (선택)

ALB 전용으로 생성한 보안 그룹이 있다면:

```bash
# 보안 그룹 삭제
aws ec2 delete-security-group \
    --group-id sg-xxxxxxxx
```

## 주의사항

1. ** 되돌릴 수 없음**: 삭제된 로드 밸런서는 복구할 수 없습니다
2. ** 진행 중인 요청**: 삭제 시점에 진행 중인 요청은 완료되지 않을 수 있습니다
3. **DNS 캐시**: DNS 레코드 변경 후 충분한 전파 시간을 확보하세요
4. **Auto Scaling 연동**: Auto Scaling 그룹에서 ALB를 먼저 분리해야 합니다

## 참고 자료

- [AWS 공식 문서 - ALB 삭제](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-delete.html)
