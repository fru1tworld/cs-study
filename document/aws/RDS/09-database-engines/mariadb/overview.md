# Amazon RDS for MariaDB

## 개요

Amazon RDS for MariaDB는 완전 관리형 MariaDB 데이터베이스 서비스입니다. MariaDB는 MySQL과 호환되는 오픈소스 관계형 데이터베이스로, MySQL 개발자들이 만든 포크입니다.

## 지원 버전

| 버전 | 지원 상태 |
|-----|----------|
| MariaDB 10.11 | ✅ 최신 LTS |
| MariaDB 10.6 | ✅ LTS |
| MariaDB 10.5 | ✅ 지원 |
| MariaDB 10.4 | ✅ 지원 (EOL 예정) |

**버전 확인**:
```bash
aws rds describe-db-engine-versions --engine mariadb
```

## MySQL과의 차이점

### MariaDB 고유 기능
| 기능 | 설명 |
|-----|------|
| Aria 스토리지 엔진 | 크래시 안전 MyISAM 대체 |
| ColumnStore | 분석 워크로드용 |
| Galera 클러스터 | 동기식 멀티 마스터 복제 |
| 시퀀스 | Oracle 호환 시퀀스 객체 |
| 가상 컬럼 | 계산된 컬럼 |

### 성능 개선
- 서브쿼리 최적화
- 쿼리 캐시 스레드 풀
- 향상된 통계 수집

## 주요 기능

### RDS 관리 기능
- ✅ 자동 백업 및 스냅샷
- ✅ Multi-AZ 배포
- ✅ 읽기 전용 복제본 (최대 15개)
- ✅ 스토리지 자동 확장
- ✅ 자동 마이너 버전 업그레이드

## 연결

### 기본 포트
- **3306** (기본값)

### 연결 문자열
```bash
mysql -h <endpoint> -P 3306 -u <username> -p
```

### 애플리케이션 연결 (JDBC)
```
jdbc:mariadb://<endpoint>:3306/<database>?useSSL=true
```

### SSL 연결
```bash
mysql -h mydb.xxx.rds.amazonaws.com -u admin -p \
    --ssl-ca=global-bundle.pem \
    --ssl-mode=VERIFY_IDENTITY
```

## 주요 파라미터

### 성능 관련

| 파라미터 | 설명 | 권장 값 |
|---------|------|--------|
| `innodb_buffer_pool_size` | InnoDB 버퍼 풀 | 메모리의 75% |
| `max_connections` | 최대 연결 수 | 워크로드에 따라 |
| `thread_cache_size` | 스레드 캐시 | 연결 패턴에 따라 |
| `table_open_cache` | 테이블 캐시 | 테이블 수에 따라 |

### MariaDB 특화 파라미터

| 파라미터 | 설명 |
|---------|------|
| `aria_pagecache_buffer_size` | Aria 페이지 캐시 |
| `optimizer_switch` | 옵티마이저 기능 토글 |
| `use_stat_tables` | 통계 테이블 사용 |

## 복제

### 읽기 복제본
```bash
aws rds create-db-instance-read-replica \
    --db-instance-identifier mydb-replica \
    --source-db-instance-identifier mydb
```

### GTID 기반 복제
MariaDB 10.0.2+에서 GTID 지원

### 병렬 복제
슬레이브에서 병렬 SQL 스레드 지원:
```
slave_parallel_threads = 4
slave_parallel_mode = optimistic
```

## 스토리지 엔진

### 지원 엔진

| 엔진 | 용도 | 권장 |
|-----|------|-----|
| InnoDB | 트랜잭션 워크로드 | ✅ 기본 권장 |
| Aria | MyISAM 대체 | 임시 테이블 |
| MyISAM | 읽기 전용 | ⚠️ 제한적 사용 |

## 데이터 마이그레이션

### mysqldump 사용
```bash
# 내보내기
mysqldump -h source-host -u user -p database > backup.sql

# 가져오기
mysql -h rds-endpoint -u admin -p database < backup.sql
```

### AWS DMS
- MySQL에서 MariaDB로 마이그레이션
- MariaDB에서 MariaDB로 마이그레이션

## MySQL에서 마이그레이션

### 호환성 고려사항
- 대부분의 MySQL 5.x 애플리케이션 호환
- 일부 MySQL 8.0 기능 미지원
- 스토어드 프로시저/함수 검증 필요

### 마이그레이션 단계
1. MariaDB 버전 호환성 확인
2. 테스트 환경에서 검증
3. 데이터 마이그레이션
4. 애플리케이션 테스트
5. 프로덕션 전환

## 제한사항

### RDS 특유 제한
- ❌ 호스트 직접 접근 불가
- ❌ SUPER 권한 없음
- ❌ 일부 스토리지 엔진 미지원 (TokuDB, Spider 등)
- ❌ Galera 클러스터 미지원 (RDS 자체 Multi-AZ 사용)

## 규정 준수

- ✅ HIPAA 적격 (BAA 필요)
- ✅ PCI DSS 준수
- ✅ SOC 1, 2, 3

## 모범 사례

### 성능
```
✅ InnoDB 버퍼 풀 최적화
✅ 슬로우 쿼리 로그 활성화 및 분석
✅ 쿼리 옵티마이저 힌트 활용
✅ 적절한 인덱스 설계
```

### 보안
```
✅ SSL/TLS 연결 사용
✅ 최소 권한 사용자 생성
✅ 저장 데이터 암호화 활성화
✅ VPC 프라이빗 서브넷 사용
```

### 마이그레이션
```
✅ 버전 호환성 철저히 검토
✅ 테스트 환경에서 충분한 검증
✅ 롤백 계획 수립
```

## 관련 문서

- [DB 인스턴스 생성](../../03-db-instances/creating-instance.md)
- [파라미터 그룹](../../10-management/parameter-groups.md)
- [읽기 복제본](../../07-high-availability/read-replicas.md)
