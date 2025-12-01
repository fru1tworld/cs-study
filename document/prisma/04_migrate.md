# Prisma Migrate

> 공식 문서: https://www.prisma.io/docs/orm/prisma-migrate

## 개요

Prisma Migrate는 Prisma Schema 변경을 데이터베이스에 적용하는 마이그레이션 도구입니다.

---

## 워크플로우 개요

```
Schema 변경 → migrate dev → SQL 생성 → 데이터베이스 적용 → Client 생성
```

---

## 개발 환경 마이그레이션

### 초기 마이그레이션

```bash
npx prisma migrate dev --name init
```

이 명령은:
1. 새 마이그레이션 SQL 파일 생성
2. 데이터베이스에 마이그레이션 적용
3. Prisma Client 재생성

### 마이그레이션 파일 구조

```
prisma/
└── migrations/
    ├── 20231128000000_init/
    │   └── migration.sql
    ├── 20231129000000_add_profile/
    │   └── migration.sql
    └── migration_lock.toml
```

### 예시: migration.sql

```sql
-- CreateTable
CREATE TABLE "User" (
    "id" SERIAL NOT NULL,
    "email" TEXT NOT NULL,
    "name" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "User_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "User_email_key" ON "User"("email");
```

---

## 스키마 변경 예시

### 1. 필드 추가

```prisma
model User {
  id        Int      @id @default(autoincrement())
  email     String   @unique
  name      String?
+ bio       String?  // 새 필드 추가
}
```

```bash
npx prisma migrate dev --name add_bio
```

### 2. 필드 삭제

```prisma
model User {
  id        Int      @id @default(autoincrement())
  email     String   @unique
- name      String?  // 필드 삭제
}
```

```bash
npx prisma migrate dev --name remove_name
```

### 3. 테이블 추가

```prisma
+ model Post {
+   id        Int      @id @default(autoincrement())
+   title     String
+   content   String?
+   authorId  Int
+   author    User     @relation(fields: [authorId], references: [id])
+ }
```

```bash
npx prisma migrate dev --name add_post
```

---

## 프로덕션 마이그레이션

### 마이그레이션 배포

```bash
npx prisma migrate deploy
```

이 명령은:
- 대기 중인 마이그레이션만 적용
- 새 마이그레이션 생성하지 않음
- Shadow Database 사용하지 않음
- CI/CD 파이프라인에 적합

### 마이그레이션 상태 확인

```bash
npx prisma migrate status
```

---

## Shadow Database

### 개념

Shadow Database는 `migrate dev` 실행 시 마이그레이션을 검증하기 위해 사용되는 임시 데이터베이스입니다.

### 동작 방식

1. Shadow Database 생성
2. 모든 마이그레이션 순차 적용
3. 현재 스키마와 비교
4. 새 마이그레이션 생성
5. Shadow Database 삭제

### 설정 (필요시)

```prisma
datasource db {
  provider          = "postgresql"
  url               = env("DATABASE_URL")
  shadowDatabaseUrl = env("SHADOW_DATABASE_URL")
}
```

```env
DATABASE_URL="postgresql://user:pass@localhost:5432/mydb"
SHADOW_DATABASE_URL="postgresql://user:pass@localhost:5432/mydb_shadow"
```

---

## db push (프로토타이핑)

마이그레이션 파일 없이 스키마를 데이터베이스에 직접 푸시

```bash
npx prisma db push
```

### 사용 케이스

- 초기 프로토타이핑
- 로컬 개발 빠른 반복
- 마이그레이션 히스토리가 필요 없는 경우

### 주의사항

```bash
# 데이터 손실 경고 무시 (주의!)
npx prisma db push --accept-data-loss
```

### migrate dev vs db push

| 특성 | migrate dev | db push |
|-----|-------------|---------|
| 마이그레이션 파일 | 생성 | 생성 안 함 |
| 히스토리 관리 | O | X |
| 프로덕션 사용 | O | X |
| 데이터 손실 | 경고 | 자동 처리 |
| 용도 | 운영 | 프로토타이핑 |

---

## 마이그레이션 리셋

### 개발 환경 리셋

```bash
npx prisma migrate reset
```

이 명령은:
1. 데이터베이스 삭제 (또는 스키마 삭제)
2. 데이터베이스 재생성
3. 모든 마이그레이션 재적용
4. 시드 데이터 실행 (있는 경우)

### 시드 스크립트 설정

```json
// package.json
{
  "prisma": {
    "seed": "ts-node prisma/seed.ts"
  }
}
```

```typescript
// prisma/seed.ts
import { PrismaClient } from './generated/prisma';

const prisma = new PrismaClient();

async function main() {
  await prisma.user.create({
    data: {
      email: 'admin@example.com',
      name: 'Admin',
    },
  });
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
```

### 시드 실행

```bash
npx prisma db seed
```

---

## 기존 데이터베이스에 적용

### 1. 스키마 가져오기

```bash
npx prisma db pull
```

### 2. 베이스라인 마이그레이션 생성

```bash
# 마이그레이션 폴더 생성만
npx prisma migrate dev --name baseline --create-only
```

### 3. 마이그레이션 적용됨으로 표시

```bash
npx prisma migrate resolve --applied 20231128000000_baseline
```

---

## 마이그레이션 수정

### SQL 직접 수정

1. 마이그레이션 생성만 (적용 안 함)
```bash
npx prisma migrate dev --name custom_migration --create-only
```

2. SQL 파일 수정
```sql
-- prisma/migrations/20231128000000_custom_migration/migration.sql
-- 커스텀 SQL 추가
CREATE INDEX CONCURRENTLY "User_email_idx" ON "User"("email");
```

3. 마이그레이션 적용
```bash
npx prisma migrate dev
```

---

## 마이그레이션 문제 해결

### 드리프트 감지

스키마와 데이터베이스가 불일치할 때:

```bash
npx prisma migrate dev
# 경고: Drift detected
```

### 해결 방법

```bash
# 1. 마이그레이션 리셋 (개발 환경)
npx prisma migrate reset

# 2. 또는 베이스라인 생성
npx prisma migrate resolve --rolled-back <migration_name>
```

### 실패한 마이그레이션 처리

```bash
# 실패로 표시
npx prisma migrate resolve --rolled-back 20231128000000_failed

# 수동 적용 후 성공으로 표시
npx prisma migrate resolve --applied 20231128000000_manual
```

---

## 마이그레이션 비교

### diff 명령어

```bash
# 스키마와 데이터베이스 비교
npx prisma migrate diff \
  --from-schema-datamodel prisma/schema.prisma \
  --to-schema-datasource prisma/schema.prisma

# SQL 출력
npx prisma migrate diff \
  --from-schema-datamodel prisma/schema.prisma \
  --to-schema-datasource prisma/schema.prisma \
  --script
```

---

## 데이터베이스별 주의사항

### PostgreSQL

- Shadow Database에 `CREATE DATABASE` 권한 필요
- 또는 `shadowDatabaseUrl` 수동 지정

### MySQL

- 외래 키 제약 조건 순서 주의
- `utf8mb4` 인코딩 권장

### SQLite

- 컬럼 수정에 제한 있음
- 테이블 재생성 방식 사용

### MongoDB

- Prisma Migrate 지원하지 않음
- `db push`만 사용 가능

---

## CI/CD 통합

### GitHub Actions 예시

```yaml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'

      - name: Install dependencies
        run: npm ci

      - name: Run migrations
        run: npx prisma migrate deploy
        env:
          DATABASE_URL: ${{ secrets.DATABASE_URL }}

      - name: Generate Prisma Client
        run: npx prisma generate
```

---

## 관련 문서

- [Prisma Schema](./02_schema.md)
- [CLI 명령어](./07_cli.md)
- [고급 기능](./08_advanced.md)
