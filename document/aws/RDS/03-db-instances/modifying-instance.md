# DB 인스턴스 수정

## 개요

Amazon RDS에서는 스토리지 추가, DB 인스턴스 클래스 변경 등 다양한 설정을 수정할 수 있습니다. 일부 수정 사항은 즉시 적용되며, 일부는 다음 유지 관리 기간에 적용됩니다.

## 수정 가능한 설정

### 즉시 적용 가능 (다운타임 없음)
- 백업 보존 기간
- 백업 기간
- 모니터링 간격
- 태그 수정
- 보안 그룹 연결

### 유지 관리 기간에 적용 (다운타임 발생 가능)
- DB 인스턴스 클래스 변경
- 스토리지 유형 변경
- 엔진 버전 업그레이드
- Multi-AZ 배포 활성화/비활성화
- 파라미터 그룹 변경 (일부)

## 수정 절차

### AWS 콘솔

1. **RDS 콘솔 접속**
   - AWS Management Console에서 RDS 선택

2. **인스턴스 선택**
   - 탐색 창에서 **Databases** 선택
   - 수정할 DB 인스턴스 선택

3. **수정 시작**
   - **Modify** 버튼 클릭

4. **설정 변경**
   - 필요한 설정 변경
   - 각 항목의 현재 값과 새 값 확인

5. **적용 방식 선택**
   - **Apply immediately**: 즉시 적용 (다운타임 발생 가능)
   - **Apply during the next scheduled maintenance window**: 다음 유지 관리 기간에 적용

6. **확인 및 완료**
   - 변경 사항 요약 검토
   - **Modify DB instance** 클릭

### AWS CLI

```bash
aws rds modify-db-instance \
    --db-instance-identifier mydbinstance \
    --db-instance-class db.m6g.large \
    --apply-immediately
```

#### 주요 파라미터

| 파라미터 | 설명 |
|---------|------|
| `--db-instance-identifier` | 수정할 인스턴스 식별자 |
| `--db-instance-class` | 새 인스턴스 클래스 |
| `--allocated-storage` | 새 스토리지 용량 |
| `--engine-version` | 새 엔진 버전 |
| `--apply-immediately` | 즉시 적용 여부 |

### RDS API

```python
import boto3

rds = boto3.client('rds')

response = rds.modify_db_instance(
    DBInstanceIdentifier='mydbinstance',
    DBInstanceClass='db.m6g.large',
    ApplyImmediately=True
)

print(response['DBInstance']['PendingModifiedValues'])
```

## 인스턴스 클래스 변경

### 주의사항
- 다운타임 발생: 수 분에서 수십 분
- Multi-AZ 배포 시 장애 조치 발생
- 테스트 환경에서 먼저 검증 권장

### 권장 절차
1. 현재 성능 메트릭 수집
2. 테스트 인스턴스에서 변경 테스트
3. 유지 관리 기간 중 변경 수행
4. 변경 후 성능 모니터링

## 스토리지 수정

### 스토리지 확장
- 용량은 증가만 가능 (감소 불가)
- 확장 중에도 데이터베이스 사용 가능
- 6시간마다 한 번 확장 가능

```bash
aws rds modify-db-instance \
    --db-instance-identifier mydbinstance \
    --allocated-storage 100 \
    --apply-immediately
```

### 스토리지 유형 변경
- gp2 → gp3: 온라인 변경 가능
- gp3 → io1/io2: 온라인 변경 가능
- 마그네틱 → SSD: 다운타임 발생

### 스토리지 자동 확장
```bash
aws rds modify-db-instance \
    --db-instance-identifier mydbinstance \
    --max-allocated-storage 500 \
    --apply-immediately
```

## Multi-AZ 배포 변경

### 단일 AZ → Multi-AZ
```bash
aws rds modify-db-instance \
    --db-instance-identifier mydbinstance \
    --multi-az \
    --apply-immediately
```

- 스냅샷 생성 후 동기 복제본 생성
- 약간의 성능 영향 (I/O 일시 중단)
- 다운타임 최소화

### Multi-AZ → 단일 AZ
```bash
aws rds modify-db-instance \
    --db-instance-identifier mydbinstance \
    --no-multi-az \
    --apply-immediately
```

## 파라미터 그룹 변경

```bash
aws rds modify-db-instance \
    --db-instance-identifier mydbinstance \
    --db-parameter-group-name my-new-param-group \
    --apply-immediately
```

### 적용 방식
- **동적 파라미터**: 즉시 적용
- **정적 파라미터**: 재부팅 필요

## 보류 중인 수정 사항 확인

```bash
aws rds describe-db-instances \
    --db-instance-identifier mydbinstance \
    --query 'DBInstances[0].PendingModifiedValues'
```

## 모범 사례

### 수정 전
1. ✅ 스냅샷 생성
2. ✅ 테스트 환경에서 먼저 검증
3. ✅ 유지 관리 기간 또는 저부하 시간대 선택
4. ✅ 롤백 계획 수립

### 수정 중
1. ✅ CloudWatch 메트릭 모니터링
2. ✅ 이벤트 로그 확인
3. ✅ 애플리케이션 연결 상태 확인

### 수정 후
1. ✅ 데이터베이스 연결 테스트
2. ✅ 애플리케이션 정상 작동 확인
3. ✅ 성능 메트릭 비교

## 관련 문서

- [DB 인스턴스 개요](./overview.md)
- [인스턴스 클래스 상세](./instance-classes.md)
- [스토리지 유형](../04-storage/storage-types.md)
