# DB 인스턴스 삭제

## 개요

Amazon RDS에서 더 이상 필요하지 않은 DB 인스턴스를 삭제할 수 있습니다. 삭제 시 데이터 보존 옵션을 신중하게 선택해야 합니다.

## 삭제 전 필수 조건

### 삭제 보호 확인
- AWS 콘솔에서 생성된 인스턴스는 기본적으로 삭제 보호 활성화
- 삭제 전 삭제 보호를 비활성화해야 함

```bash
aws rds modify-db-instance \
    --db-instance-identifier mydbinstance \
    --no-deletion-protection \
    --apply-immediately
```

### 읽기 복제본 확인
- 삭제하려는 인스턴스에 연결된 읽기 복제본 확인
- 원본 인스턴스 삭제 시 읽기 복제본은 독립 인스턴스로 승격

## 삭제 시 옵션

### 최종 스냅샷 (Final Snapshot)
- ** 생성 권장**: 삭제 후에도 데이터 복구 가능
- 스냅샷 식별자 지정 필요
- 수동 스냅샷으로 저장되어 자동 삭제되지 않음

### 자동 백업 보존
- ** 보존 선택 시**: 지정된 보존 기간 동안 유지
- ** 보존 미선택 시**: 자동 백업 함께 삭제 (복구 불가)
- 보존 시 추가 스토리지 비용 발생

## 삭제 절차

### AWS 콘솔

1. **RDS 콘솔 접속**
   - AWS Management Console에서 RDS 선택

2. ** 인스턴스 선택**
   - 탐색 창에서 **Databases** 선택
   - 삭제할 DB 인스턴스 선택

3. ** 삭제 시작**
   - **Actions** → **Delete** 클릭

4. ** 옵션 선택**
   - **Create final snapshot?**: 최종 스냅샷 생성 여부
   - **Retain automated backups?**: 자동 백업 보존 여부
   - 스냅샷 식별자 입력 (생성 시)

5. ** 확인**
   - "delete me" 입력하여 확인
   - **Delete** 클릭

### AWS CLI

#### 최종 스냅샷 생성하며 삭제
```bash
aws rds delete-db-instance \
    --db-instance-identifier mydbinstance \
    --final-db-snapshot-identifier mydbinstance-final-snapshot \
    --delete-automated-backups
```

#### 최종 스냅샷 없이 삭제
```bash
aws rds delete-db-instance \
    --db-instance-identifier mydbinstance \
    --skip-final-snapshot \
    --delete-automated-backups
```

#### 자동 백업 보존하며 삭제
```bash
aws rds delete-db-instance \
    --db-instance-identifier mydbinstance \
    --skip-final-snapshot \
    --no-delete-automated-backups
```

### RDS API

```python
import boto3

rds = boto3.client('rds')

response = rds.delete_db_instance(
    DBInstanceIdentifier='mydbinstance',
    SkipFinalSnapshot=False,
    FinalDBSnapshotIdentifier='mydbinstance-final-snapshot',
    DeleteAutomatedBackups=True
)

print("Deleting:", response['DBInstance']['DBInstanceIdentifier'])
```

## 삭제 후 영향

### 즉시 영향
| 항목 | 상태 |
|-----|------|
| DB 인스턴스 | 삭제됨 |
| 엔드포인트 | 사용 불가 |
| 연결 | 모두 종료 |

### 데이터 관련
| 항목 | 최종 스냅샷 생성 | 스냅샷 미생성 |
|-----|----------------|-------------|
| 데이터 | 스냅샷에서 복구 가능 | 영구 삭제 |
| 수동 스냅샷 | 유지됨 | 유지됨 |
| 자동 백업 | 옵션에 따라 다름 | 옵션에 따라 다름 |

### 읽기 복제본
- 원본 인스턴스 삭제 시 독립 인스턴스로 자동 승격
- 복제 관계 해제
- 읽기/쓰기 모두 가능해짐

## 삭제 소요 시간

삭제 시간에 영향을 주는 요소:
- 최종 스냅샷 생성 여부
- 데이터베이스 크기
- 백업 설정

일반적으로 수 분에서 수십 분 소요

## 삭제 취소

⚠️ ** 삭제 시작 후에는 취소 불가**

삭제가 완료되면:
- 인스턴스 복구 불가
- 최종 스냅샷에서 새 인스턴스 생성 가능
- 자동 백업에서 특정 시점 복구 가능 (보존 시)

## 삭제 전 체크리스트

### 필수 확인 사항
- [ ] 삭제 보호 비활성화
- [ ] 최종 스냅샷 필요 여부 결정
- [ ] 자동 백업 보존 필요 여부 결정
- [ ] 연결된 애플리케이션 확인
- [ ] 읽기 복제본 처리 계획

### 권장 조치
- [ ] 데이터 내보내기 완료
- [ ] 관련 팀에 통지
- [ ] 비용 청구 종료 확인

## 모범 사례

1. ** 프로덕션 인스턴스**: 항상 최종 스냅샷 생성
2. ** 자동 백업**: 일정 기간 보존 권장
3. ** 문서화**: 삭제 이유 및 날짜 기록
4. ** 정리**: 사용하지 않는 수동 스냅샷 정기 정리

## 관련 문서

- [DB 인스턴스 개요](./overview.md)
- [백업 및 복구](../06-backup-recovery/automated-backups.md)
- [스냅샷 관리](../06-backup-recovery/snapshots.md)
