# 파라미터 그룹

## 개요

파라미터 그룹은 데이터베이스 구성을 관리하는 핵심 도구입니다. 데이터베이스 파라미터는 메모리 할당, 캐시 크기 등 데이터베이스 동작 방식을 결정합니다.

## 파라미터 그룹 유형

### DB 파라미터 그룹
- 개별 DB 인스턴스에 적용
- 엔진별 기본 파라미터 그룹 제공
- 사용자 정의 파라미터 그룹 생성 가능

### DB 클러스터 파라미터 그룹
- Multi-AZ DB 클러스터 전체에 적용
- 클러스터 수준 설정 관리

## 기본 파라미터 그룹

### 특징
- 각 엔진 버전별로 제공
- 수정 불가 (읽기 전용)
- 새 DB 인스턴스 생성 시 자동 연결

### 기본 파라미터 그룹 이름 형식
```
default.<engine><major-version>
예: default.mysql8.0
    default.postgres15
```

## 사용자 정의 파라미터 그룹

### 생성

```bash
aws rds create-db-parameter-group \
    --db-parameter-group-name my-mysql-params \
    --db-parameter-group-family mysql8.0 \
    --description "Custom MySQL 8.0 parameters"
```

### 파라미터 수정

```bash
aws rds modify-db-parameter-group \
    --db-parameter-group-name my-mysql-params \
    --parameters \
        "ParameterName=max_connections,ParameterValue=500,ApplyMethod=pending-reboot" \
        "ParameterName=innodb_buffer_pool_size,ParameterValue={DBInstanceClassMemory*3/4},ApplyMethod=pending-reboot"
```

### DB 인스턴스에 연결

```bash
aws rds modify-db-instance \
    --db-instance-identifier mydb \
    --db-parameter-group-name my-mysql-params \
    --apply-immediately
```

## 파라미터 적용 방법

### 동적 파라미터 (Dynamic)
- 즉시 적용 가능
- 재부팅 불필요
- `ApplyMethod=immediate`

### 정적 파라미터 (Static)
- 재부팅 후 적용
- 데이터베이스 재시작 필요
- `ApplyMethod=pending-reboot`

## 파라미터 값 표현식

### 수식 사용

| 표현식 | 설명 |
|-------|------|
| `{DBInstanceClassMemory}` | 인스턴스 메모리 (바이트) |
| `{DBInstanceClassMemory/12345678}` | 계산된 값 |
| `{DBInstanceClassMemory*3/4}` | 메모리의 75% |

### 예시

```bash
# innodb_buffer_pool_size를 메모리의 75%로 설정
--parameters "ParameterName=innodb_buffer_pool_size,ParameterValue={DBInstanceClassMemory*3/4}"
```

## 파라미터 조회

### 파라미터 그룹 목록

```bash
aws rds describe-db-parameter-groups
```

### 특정 그룹의 파라미터

```bash
aws rds describe-db-parameters \
    --db-parameter-group-name my-mysql-params
```

### 수정된 파라미터만 조회

```bash
aws rds describe-db-parameters \
    --db-parameter-group-name my-mysql-params \
    --source user
```

## 파라미터 그룹 비교

```bash
# 두 파라미터 그룹 비교 (콘솔에서)
# 또는 describe-db-parameters 결과를 비교
```

## 주요 파라미터 예시

### MySQL

| 파라미터 | 설명 | 권장 값 |
|---------|------|--------|
| `max_connections` | 최대 연결 수 | 워크로드에 따라 |
| `innodb_buffer_pool_size` | 버퍼 풀 크기 | 메모리의 75% |
| `slow_query_log` | 슬로우 쿼리 로그 | 1 (활성화) |
| `long_query_time` | 슬로우 쿼리 기준 (초) | 1-2 |

### PostgreSQL

| 파라미터 | 설명 | 권장 값 |
|---------|------|--------|
| `shared_buffers` | 공유 버퍼 | 메모리의 25% |
| `effective_cache_size` | 예상 캐시 크기 | 메모리의 75% |
| `work_mem` | 작업 메모리 | 쿼리 복잡도에 따라 |
| `log_min_duration_statement` | 로그 기록 기준 (ms) | 1000 |

## 파라미터 그룹 삭제

```bash
aws rds delete-db-parameter-group \
    --db-parameter-group-name my-mysql-params
```

### 삭제 조건
- 어떤 DB 인스턴스에도 연결되지 않아야 함
- 기본 파라미터 그룹은 삭제 불가

## 모범 사례

### 설계
```
✅ 환경별(개발/스테이징/프로덕션) 별도 파라미터 그룹 생성
✅ 명확한 명명 규칙 사용 (예: project-env-engine-params)
✅ 변경 전 기본값 문서화
```

### 변경 관리
```
✅ 테스트 환경에서 먼저 검증
✅ 변경 사항 문서화
✅ 점진적 변경 (한 번에 많은 파라미터 변경 지양)
✅ 성능 영향 모니터링
```

### 운영
```
✅ 정기적인 파라미터 검토
✅ 불필요한 사용자 정의 파라미터 그룹 정리
✅ 버전 업그레이드 시 호환성 확인
```

## 관련 문서

- [옵션 그룹](./option-groups.md)
- [DB 인스턴스 수정](../03-db-instances/modifying-instance.md)
- [데이터베이스 엔진별 가이드](../09-database-engines/)
