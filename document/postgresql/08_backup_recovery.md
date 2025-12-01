# PostgreSQL 백업 및 복구

> 공식 문서: https://www.postgresql.org/docs/current/backup.html

PostgreSQL 데이터베이스는 정기적으로 백업해야 합니다. 세 가지 주요 백업 방식이 있습니다.

---

## 백업 방식 개요

| 방식 | 설명 | 장점 | 단점 |
|------|------|------|------|
| SQL 덤프 | SQL 명령어로 추출 | 이식성, 유연성 | 대용량에서 느림 |
| 파일 시스템 백업 | 데이터 파일 복사 | 빠름 | 전체 클러스터만 |
| 지속적 아카이빙 | WAL 파일 아카이빙 | PITR, 증분 백업 | 설정 복잡 |

---

## 1. SQL 덤프 (pg_dump)

### 데이터베이스 덤프

```bash
# 기본 덤프
pg_dump dbname > backup.sql

# 사용자 지정
pg_dump -U username -h hostname dbname > backup.sql

# 특정 포맷
pg_dump -Fc dbname > backup.dump      # 커스텀 포맷 (압축)
pg_dump -Fd dbname -f backup_dir       # 디렉토리 포맷 (병렬)
pg_dump -Ft dbname > backup.tar        # tar 포맷
```

### 덤프 옵션

```bash
# 스키마만
pg_dump --schema-only dbname > schema.sql

# 데이터만
pg_dump --data-only dbname > data.sql

# 특정 테이블만
pg_dump -t table_name dbname > table.sql

# 특정 테이블 제외
pg_dump --exclude-table=logs dbname > backup.sql

# 특정 스키마만
pg_dump -n schema_name dbname > schema_backup.sql

# 압축
pg_dump -Z 9 dbname > backup.sql.gz

# 병렬 덤프 (디렉토리 포맷)
pg_dump -Fd -j 4 dbname -f backup_dir
```

### 전체 클러스터 덤프

```bash
# 모든 데이터베이스와 글로벌 객체
pg_dumpall > full_backup.sql

# 역할과 테이블스페이스만
pg_dumpall --roles-only > roles.sql
pg_dumpall --tablespaces-only > tablespaces.sql

# 글로벌 객체만
pg_dumpall --globals-only > globals.sql
```

---

## SQL 덤프 복구 (pg_restore)

### 텍스트 덤프 복구

```bash
# SQL 파일 복구
psql dbname < backup.sql

# 새 데이터베이스에 복구
createdb newdb
psql newdb < backup.sql
```

### 커스텀/디렉토리 포맷 복구

```bash
# 기본 복구
pg_restore -d dbname backup.dump

# 새 데이터베이스 생성 후 복구
pg_restore -C -d postgres backup.dump

# 병렬 복구
pg_restore -j 4 -d dbname backup_dir

# 특정 테이블만 복구
pg_restore -t table_name -d dbname backup.dump

# 스키마만 복구
pg_restore --schema-only -d dbname backup.dump

# 데이터만 복구
pg_restore --data-only -d dbname backup.dump
```

### 복구 옵션

```bash
# 오류 시 계속 진행
pg_restore --if-exists --clean -d dbname backup.dump

# 상세 출력
pg_restore -v -d dbname backup.dump

# 단일 트랜잭션으로 복구
pg_restore --single-transaction -d dbname backup.dump
```

---

## 2. 파일 시스템 레벨 백업

### 콜드 백업

서버를 중지하고 데이터 디렉토리를 복사합니다.

```bash
# 서버 중지
pg_ctl stop -D /var/lib/postgresql/data

# 데이터 디렉토리 복사
cp -r /var/lib/postgresql/data /backup/pg_data_backup

# 서버 시작
pg_ctl start -D /var/lib/postgresql/data
```

### 제한사항

- 서버 중지 필요
- 전체 클러스터만 백업/복구 가능
- 다른 버전으로 복구 불가

---

## 3. 지속적 아카이빙 (WAL 아카이빙)

PITR(Point-In-Time Recovery)을 위한 WAL 파일 아카이빙입니다.

### WAL 아카이빙 설정

**postgresql.conf:**

```ini
# WAL 아카이빙 활성화
wal_level = replica
archive_mode = on
archive_command = 'cp %p /backup/wal_archive/%f'

# 또는 rsync로 원격 백업
# archive_command = 'rsync -a %p backup_server:/backup/wal_archive/%f'
```

### 베이스 백업 생성

```bash
# pg_basebackup 사용
pg_basebackup -D /backup/base_backup -Ft -z -P

# 옵션
# -D: 대상 디렉토리
# -Ft: tar 포맷
# -z: gzip 압축
# -P: 진행률 표시
# -X stream: WAL도 함께 스트리밍

# 체크포인트 방식 지정
pg_basebackup -D /backup/base_backup --checkpoint=fast
```

### PITR 복구

1. **기존 데이터 디렉토리 비우기**

```bash
rm -rf /var/lib/postgresql/data/*
```

2. **베이스 백업 복원**

```bash
tar -xzf /backup/base_backup/base.tar.gz -C /var/lib/postgresql/data/
```

3. **복구 설정**

**postgresql.conf 또는 postgresql.auto.conf:**

```ini
restore_command = 'cp /backup/wal_archive/%f %p'

# 특정 시점으로 복구
recovery_target_time = '2024-12-25 14:30:00'

# 또는 특정 트랜잭션으로
# recovery_target_xid = '12345'

# 또는 특정 복구 지점으로
# recovery_target_name = 'my_restore_point'

# 복구 완료 후 동작
recovery_target_action = 'promote'
```

4. **복구 시그널 파일 생성**

```bash
touch /var/lib/postgresql/data/recovery.signal
```

5. **서버 시작**

```bash
pg_ctl start -D /var/lib/postgresql/data
```

---

## pg_basebackup 고급 사용

### 스트리밍 복제를 위한 백업

```bash
pg_basebackup \
    -h primary_host \
    -D /var/lib/postgresql/data \
    -U replication_user \
    -P \
    -R \
    --wal-method=stream
```

**옵션 설명:**
- `-R`: 스탠바이 설정 자동 생성
- `--wal-method=stream`: WAL을 스트리밍으로 수집

### 증분 백업 (PostgreSQL 17+)

```bash
# 전체 백업
pg_basebackup -D /backup/full

# 증분 백업
pg_basebackup -D /backup/incremental --incremental=/backup/full/backup_manifest
```

---

## 복구 포인트

### 명명된 복구 포인트 생성

```sql
SELECT pg_create_restore_point('before_migration');
```

### 복구 포인트로 복구

```ini
recovery_target_name = 'before_migration'
recovery_target_action = 'promote'
```

---

## 백업 스크립트 예제

### 일일 덤프 스크립트

```bash
#!/bin/bash
DATE=$(date +%Y%m%d)
BACKUP_DIR="/backup/daily"
DB_NAME="mydb"

# 오래된 백업 삭제 (7일 이상)
find $BACKUP_DIR -name "*.dump" -mtime +7 -delete

# 백업 실행
pg_dump -Fc $DB_NAME > $BACKUP_DIR/${DB_NAME}_${DATE}.dump

# 결과 확인
if [ $? -eq 0 ]; then
    echo "Backup successful: ${DB_NAME}_${DATE}.dump"
else
    echo "Backup failed!" | mail -s "Backup Alert" admin@example.com
fi
```

### WAL 아카이빙 스크립트

```bash
#!/bin/bash
# archive_command에서 사용
WAL_FILE=$1
WAL_PATH=$2
ARCHIVE_DIR="/backup/wal_archive"

# WAL 파일 복사
cp $WAL_PATH $ARCHIVE_DIR/$WAL_FILE

# 원격 서버에도 복사
rsync -a $WAL_PATH backup_server:$ARCHIVE_DIR/$WAL_FILE

# 오래된 WAL 파일 정리 (30일 이상)
find $ARCHIVE_DIR -name "*.gz" -mtime +30 -delete
```

---

## 백업 검증

### 덤프 파일 확인

```bash
# 덤프 내용 확인 (복구 없이)
pg_restore -l backup.dump

# 테이블 목록 확인
pg_restore -l backup.dump | grep "TABLE"
```

### 복구 테스트

```bash
# 테스트 데이터베이스에 복구
createdb test_restore
pg_restore -d test_restore backup.dump

# 데이터 검증
psql -d test_restore -c "SELECT COUNT(*) FROM important_table;"

# 테스트 데이터베이스 삭제
dropdb test_restore
```

---

## 대용량 데이터베이스 백업

### 병렬 덤프

```bash
# 4개 프로세스로 병렬 덤프
pg_dump -Fd -j 4 dbname -f backup_dir
```

### 테이블별 분리 백업

```bash
# 테이블 목록 추출
psql -d dbname -t -c "SELECT tablename FROM pg_tables WHERE schemaname='public'" > tables.txt

# 테이블별 백업
while read table; do
    pg_dump -t "$table" dbname > "backup_${table}.sql"
done < tables.txt
```

### 파티션 테이블 백업

```sql
-- 특정 파티션만 백업
pg_dump -t 'orders_2024' dbname > orders_2024.sql
```

---

## 클라우드 백업

### AWS S3

```bash
# pg_dump를 S3로 직접 업로드
pg_dump -Fc dbname | aws s3 cp - s3://bucket/backup.dump

# S3에서 복구
aws s3 cp s3://bucket/backup.dump - | pg_restore -d dbname
```

### Google Cloud Storage

```bash
pg_dump -Fc dbname | gsutil cp - gs://bucket/backup.dump
```

---

## 백업 모범 사례

### 백업 전략

1. **일일 논리 백업** (pg_dump)
   - 빠른 복구, 특정 테이블 복원

2. **주간 베이스 백업** (pg_basebackup)
   - PITR 기반

3. **지속적 WAL 아카이빙**
   - 최소 데이터 손실

### 체크리스트

- [ ] 백업 자동화 스크립트 구성
- [ ] 백업 파일 암호화 (필요 시)
- [ ] 오프사이트 백업 저장
- [ ] 정기적인 복구 테스트
- [ ] 백업 모니터링 및 알림
- [ ] 오래된 백업 정리 정책

---

## 참고 문서

- [백업 및 복구](https://www.postgresql.org/docs/current/backup.html)
- [SQL 덤프](https://www.postgresql.org/docs/current/backup-dump.html)
- [지속적 아카이빙](https://www.postgresql.org/docs/current/continuous-archiving.html)
- [pg_basebackup](https://www.postgresql.org/docs/current/app-pgbasebackup.html)
