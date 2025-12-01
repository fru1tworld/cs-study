# Prisma CLI

> 공식 문서: https://www.prisma.io/docs/orm/reference/prisma-cli-reference

## 개요

Prisma CLI는 Prisma 프로젝트 관리를 위한 명령줄 도구입니다.

---

## 설치

```bash
# 개발 의존성으로 설치
npm install prisma --save-dev

# 글로벌 설치
npm install -g prisma
```

---

## 기본 명령어

### prisma init

새 Prisma 프로젝트를 초기화합니다.

```bash
# 기본 초기화
npx prisma init

# 데이터베이스 프로바이더 지정
npx prisma init --datasource-provider postgresql
npx prisma init --datasource-provider mysql
npx prisma init --datasource-provider sqlite
npx prisma init --datasource-provider sqlserver
npx prisma init --datasource-provider mongodb
npx prisma init --datasource-provider cockroachdb

# 출력 디렉토리 지정
npx prisma init --output ./src/prisma

# Prisma Postgres 생성
npx prisma init --db
```

** 생성되는 파일:**
- `prisma/schema.prisma` - Prisma 스키마 파일
- `.env` - 환경 변수 파일

---

### prisma generate

Prisma Client를 생성합니다.

```bash
# 기본 생성
npx prisma generate

# 감시 모드 (스키마 변경 시 자동 재생성)
npx prisma generate --watch

# 데이터 프록시 모드
npx prisma generate --data-proxy

# Accelerate용 생성
npx prisma generate --accelerate

# 스키마 경로 지정
npx prisma generate --schema=./custom/schema.prisma
```

---

### prisma validate

스키마 파일의 유효성을 검사합니다.

```bash
npx prisma validate

# 특정 스키마 파일
npx prisma validate --schema=./custom/schema.prisma
```

---

### prisma format

스키마 파일을 포맷팅합니다.

```bash
npx prisma format

# 특정 스키마 파일
npx prisma format --schema=./custom/schema.prisma
```

---

## 데이터베이스 명령어

### prisma db push

스키마를 데이터베이스에 직접 푸시합니다 (마이그레이션 파일 생성 안 함).

```bash
# 기본 푸시
npx prisma db push

# 데이터 손실 수락
npx prisma db push --accept-data-loss

# 시드 건너뛰기
npx prisma db push --skip-generate

# 강제 리셋
npx prisma db push --force-reset
```

** 사용 케이스:**
- 프로토타이핑
- 로컬 개발
- MongoDB (Migrate 미지원)

---

### prisma db pull

데이터베이스 스키마를 Prisma 스키마로 가져옵니다.

```bash
# 기본 인트로스펙션
npx prisma db pull

# 스키마 파일 지정
npx prisma db pull --schema=./custom/schema.prisma

# 강제 덮어쓰기
npx prisma db pull --force

# JSON 출력
npx prisma db pull --print
```

---

### prisma db seed

시드 스크립트를 실행합니다.

```bash
npx prisma db seed
```

** 설정 (package.json):**
```json
{
  "prisma": {
    "seed": "ts-node prisma/seed.ts"
  }
}
```

---

### prisma db execute

SQL 스크립트를 실행합니다.

```bash
# 파일에서 실행
npx prisma db execute --file ./script.sql --schema ./prisma/schema.prisma

# stdin에서 실행
echo "SELECT 1" | npx prisma db execute --stdin --schema ./prisma/schema.prisma
```

---

## 마이그레이션 명령어

### prisma migrate dev

개발 환경에서 마이그레이션을 생성하고 적용합니다.

```bash
# 마이그레이션 생성 및 적용
npx prisma migrate dev --name init

# 마이그레이션 생성만 (적용 안 함)
npx prisma migrate dev --name add_column --create-only

# 시드 건너뛰기
npx prisma migrate dev --skip-seed

# 클라이언트 생성 건너뛰기
npx prisma migrate dev --skip-generate
```

** 동작:**
1. Shadow Database에서 마이그레이션 검증
2. 새 마이그레이션 파일 생성
3. 마이그레이션 적용
4. Prisma Client 재생성
5. 시드 실행 (있는 경우)

---

### prisma migrate deploy

프로덕션 환경에 마이그레이션을 적용합니다.

```bash
npx prisma migrate deploy
```

** 특징:**
- 대기 중인 마이그레이션만 적용
- 새 마이그레이션 생성 안 함
- Shadow Database 사용 안 함
- CI/CD에 적합

---

### prisma migrate reset

데이터베이스를 리셋합니다 (개발용).

```bash
# 확인 프롬프트와 함께 리셋
npx prisma migrate reset

# 확인 없이 강제 리셋
npx prisma migrate reset --force

# 시드 건너뛰기
npx prisma migrate reset --skip-seed
```

** 동작:**
1. 데이터베이스 삭제 (또는 스키마)
2. 데이터베이스 재생성
3. 모든 마이그레이션 적용
4. 시드 실행

---

### prisma migrate status

마이그레이션 상태를 확인합니다.

```bash
npx prisma migrate status
```

** 출력 정보:**
- 적용된 마이그레이션
- 대기 중인 마이그레이션
- 실패한 마이그레이션
- 드리프트 감지

---

### prisma migrate resolve

마이그레이션 문제를 해결합니다.

```bash
# 마이그레이션을 적용됨으로 표시
npx prisma migrate resolve --applied 20231128000000_migration_name

# 마이그레이션을 롤백됨으로 표시
npx prisma migrate resolve --rolled-back 20231128000000_migration_name
```

** 사용 케이스:**
- 베이스라인 마이그레이션 설정
- 실패한 마이그레이션 처리
- 수동 마이그레이션 후 상태 동기화

---

### prisma migrate diff

두 스키마 상태의 차이를 비교합니다.

```bash
# 스키마 파일과 데이터베이스 비교
npx prisma migrate diff \
  --from-schema-datamodel prisma/schema.prisma \
  --to-schema-datasource prisma/schema.prisma

# SQL 스크립트 출력
npx prisma migrate diff \
  --from-schema-datamodel prisma/schema.prisma \
  --to-schema-datasource prisma/schema.prisma \
  --script

# 마이그레이션 디렉토리와 비교
npx prisma migrate diff \
  --from-migrations prisma/migrations \
  --to-schema-datamodel prisma/schema.prisma
```

---

## 기타 명령어

### prisma studio

브라우저 기반 데이터베이스 GUI를 실행합니다.

```bash
# 기본 실행 (포트 5555)
npx prisma studio

# 포트 지정
npx prisma studio --port 5000

# 브라우저 자동 열기 비활성화
npx prisma studio --browser none
```

---

### prisma version

버전 정보를 표시합니다.

```bash
npx prisma version
# 또는
npx prisma -v
```

---

### prisma debug

디버그 정보를 표시합니다.

```bash
npx prisma debug
```

---

## 환경 변수

### DATABASE_URL

데이터베이스 연결 URL

```env
DATABASE_URL="postgresql://user:password@localhost:5432/mydb?schema=public"
```

### SHADOW_DATABASE_URL

Shadow Database URL (마이그레이션용)

```env
SHADOW_DATABASE_URL="postgresql://user:password@localhost:5432/mydb_shadow"
```

### PRISMA_HIDE_UPDATE_MESSAGE

업데이트 메시지 숨기기

```env
PRISMA_HIDE_UPDATE_MESSAGE=true
```

### DEBUG

디버그 로깅 활성화

```env
DEBUG="prisma:*"
DEBUG="prisma:client"
DEBUG="prisma:engine"
```

---

## CLI 옵션

### 공통 옵션

| 옵션 | 설명 |
|-----|------|
| `--help`, `-h` | 도움말 표시 |
| `--version`, `-v` | 버전 표시 |
| `--schema` | 스키마 파일 경로 지정 |

---

## 명령어 요약

| 명령어 | 설명 | 환경 |
|-------|------|-----|
| `init` | 프로젝트 초기화 | 개발 |
| `generate` | Client 생성 | 개발/프로덕션 |
| `validate` | 스키마 검증 | 개발 |
| `format` | 스키마 포맷팅 | 개발 |
| `db push` | 스키마 푸시 | 개발 |
| `db pull` | 스키마 가져오기 | 개발 |
| `db seed` | 시드 실행 | 개발 |
| `db execute` | SQL 실행 | 개발/프로덕션 |
| `migrate dev` | 마이그레이션 (개발) | 개발 |
| `migrate deploy` | 마이그레이션 (배포) | 프로덕션 |
| `migrate reset` | 데이터베이스 리셋 | 개발 |
| `migrate status` | 상태 확인 | 개발/프로덕션 |
| `migrate resolve` | 문제 해결 | 개발/프로덕션 |
| `migrate diff` | 스키마 비교 | 개발 |
| `studio` | GUI 실행 | 개발 |

---

## 관련 문서

- [Prisma Migrate](./04_migrate.md)
- [Prisma Schema](./02_schema.md)
- [Prisma Client](./03_client.md)
