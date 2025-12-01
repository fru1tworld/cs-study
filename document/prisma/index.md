# Prisma 공식 문서 정리

> 최종 업데이트: 2025-11-28
> 공식 문서: https://www.prisma.io/docs

## 개요

Prisma는 차세대 Node.js 및 TypeScript ORM으로, 직관적인 데이터 모델, 자동 마이그레이션, 타입 안전성, 자동 완성을 제공합니다.

---

## 문서 목차

### 기본 개념

1. **[개요 (Overview)](./01_overview.md)**
   - Prisma 소개
   - 핵심 구성 요소
   - 지원 데이터베이스
   - 빠른 시작 가이드

2. **[Prisma Schema](./02_schema.md)**
   - 데이터 소스 설정
   - 제너레이터 구성
   - 모델 정의
   - 필드 타입 및 속성
   - 인덱스 설정
   - Enum 정의

3. **[Prisma Client](./03_client.md)**
   - 설치 및 설정
   - 연결 관리
   - 로깅 설정
   - 에러 처리
   - Select와 Include
   - 필터링과 정렬
   - 페이지네이션

### 데이터 관리

4. **[Prisma Migrate](./04_migrate.md)**
   - 개발 환경 마이그레이션
   - 프로덕션 배포
   - Shadow Database
   - db push (프로토타이핑)
   - 마이그레이션 리셋
   - 기존 데이터베이스 적용

5. **[관계 정의 (Relations)](./05_relations.md)**
   - 일대일 (1:1) 관계
   - 일대다 (1:N) 관계
   - 다대다 (M:N) 관계
   - 자기 참조 관계
   - 참조 액션
   - 관계 쿼리

6. **[CRUD 작업](./06_crud.md)**
   - Create (생성)
   - Read (조회)
   - Update (수정)
   - Delete (삭제)
   - Upsert
   - 집계 함수
   - 트랜잭션
   - Raw SQL

### 도구 및 고급 기능

7. **[CLI 명령어](./07_cli.md)**
   - prisma init
   - prisma generate
   - prisma migrate
   - prisma db (push, pull, seed)
   - prisma studio
   - 환경 변수

8. **[고급 기능](./08_advanced.md)**
   - 트랜잭션 상세
   - Raw SQL 쿼리
   - 미들웨어
   - Client Extensions
   - 전문 검색
   - 연결 최적화
   - 로깅 및 모니터링
   - 테스트

---

## 핵심 구성 요소

### 1. Prisma Schema

Prisma 설정의 핵심 파일입니다. (`prisma/schema.prisma`)

```prisma
// 데이터 소스 정의
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

// 클라이언트 생성기
generator client {
  provider = "prisma-client"
  output   = "../src/generated/prisma"
}

// 모델 정의
model User {
  id        Int      @id @default(autoincrement())
  email     String   @unique
  name      String?
  posts     Post[]
  profile   Profile?
  createdAt DateTime @default(now())
  updatedAt DateTime @updatedAt
}

model Post {
  id        Int      @id @default(autoincrement())
  title     String
  content   String?
  published Boolean  @default(false)
  author    User     @relation(fields: [authorId], references: [id])
  authorId  Int
}

model Profile {
  id     Int    @id @default(autoincrement())
  bio    String
  user   User   @relation(fields: [userId], references: [id])
  userId Int    @unique
}
```

### 2. Prisma Client

타입 안전한 데이터베이스 클라이언트입니다.

```typescript
import { PrismaClient } from './generated/prisma';

const prisma = new PrismaClient();

// CRUD 작업
async function main() {
  // Create
  const user = await prisma.user.create({
    data: {
      email: 'alice@example.com',
      name: 'Alice',
      posts: {
        create: { title: 'Hello World' },
      },
    },
    include: { posts: true },
  });

  // Read
  const users = await prisma.user.findMany({
    where: { email: { contains: 'example.com' } },
    include: { posts: true },
  });

  // Update
  const updatedUser = await prisma.user.update({
    where: { id: 1 },
    data: { name: 'Alice Updated' },
  });

  // Delete
  await prisma.user.delete({
    where: { id: 1 },
  });
}
```

### 3. Prisma Migrate

데이터베이스 스키마 마이그레이션 도구입니다.

```bash
# 개발 환경 마이그레이션
npx prisma migrate dev --name init

# 프로덕션 마이그레이션 적용
npx prisma migrate deploy

# 마이그레이션 상태 확인
npx prisma migrate status
```

---

## CLI 명령어 요약

```bash
# Prisma Client 생성
npx prisma generate

# 스키마를 데이터베이스에 푸시 (프로토타이핑용)
npx prisma db push

# 데이터베이스에서 스키마 가져오기
npx prisma db pull

# Prisma Studio 실행 (GUI)
npx prisma studio

# 스키마 유효성 검사
npx prisma validate

# 스키마 포맷팅
npx prisma format
```

---

## 프로젝트 구조

```
project/
├── prisma/
│   ├── schema.prisma      # 스키마 정의
│   └── migrations/        # 마이그레이션 히스토리
│       └── 20231128000000_init/
│           └── migration.sql
├── src/
│   ├── generated/
│   │   └── prisma/        # 생성된 클라이언트
│   └── index.ts
├── .env                   # 환경 변수
└── package.json
```

---

## 참고 링크

- [Prisma 공식 문서](https://www.prisma.io/docs)
- [Prisma Schema Reference](https://www.prisma.io/docs/orm/prisma-schema/overview)
- [Prisma Client API](https://www.prisma.io/docs/orm/prisma-client)
- [Prisma Migrate](https://www.prisma.io/docs/orm/prisma-migrate)
- [CLI Reference](https://www.prisma.io/docs/orm/reference/prisma-cli-reference)
- [Prisma GitHub](https://github.com/prisma/prisma)
