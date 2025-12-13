# Prisma 개요

> 최종 업데이트: 2025-11-28
> 공식 문서: https://www.prisma.io/docs

## Prisma란?

Prisma는 차세대 Node.js 및 TypeScript ORM(Object-Relational Mapping)으로, 직관적인 데이터 모델, 자동 마이그레이션, 타입 안전성, 자동 완성을 제공합니다.

---

## 핵심 구성 요소

### 1. Prisma Schema

Prisma 설정의 핵심 파일로, 데이터 모델과 데이터베이스 연결을 정의합니다.

- 파일 위치: `prisma/schema.prisma`
- Prisma Schema Language(PSL) 사용
- 데이터 소스, 제너레이터, 모델 정의 포함

### 2. Prisma Client

타입 안전한 데이터베이스 클라이언트입니다.

- 스키마 기반 자동 생성
- 완전한 TypeScript 타입 지원
- CRUD 작업, 관계 쿼리, 트랜잭션 지원

### 3. Prisma Migrate

데이터베이스 스키마 마이그레이션 도구입니다.

- 스키마 변경 추적
- SQL 마이그레이션 파일 자동 생성
- 개발/프로덕션 환경 별도 워크플로우

### 4. Prisma Studio

데이터베이스 GUI 도구입니다.

- 브라우저 기반 데이터 뷰어/편집기
- 관계 데이터 시각화

---

## 지원 데이터베이스

| 데이터베이스 | 지원 여부 |
|------------|----------|
| PostgreSQL | ✅ |
| MySQL | ✅ |
| MariaDB | ✅ |
| SQLite | ✅ |
| SQL Server | ✅ |
| MongoDB | ✅ |
| CockroachDB | ✅ |
| PlanetScale | ✅ |

---

## 빠른 시작

### 1. 설치

```bash
# 프로젝트 초기화
npm init -y

# Prisma 설치
npm install prisma --save-dev

# Prisma 초기화
npx prisma init --datasource-provider postgresql
```

### 2. 스키마 정의

```prisma
// prisma/schema.prisma

datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

generator client {
  provider = "prisma-client"
  output   = "../src/generated/prisma"
}

model User {
  id        Int      @id @default(autoincrement())
  email     String   @unique
  name      String?
  posts     Post[]
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
```

### 3. 마이그레이션 실행

```bash
# 개발 환경 마이그레이션
npx prisma migrate dev --name init
```

### 4. Prisma Client 사용

```typescript
import { PrismaClient } from './generated/prisma';

const prisma = new PrismaClient();

async function main() {
  // 사용자 생성
  const user = await prisma.user.create({
    data: {
      email: 'alice@example.com',
      name: 'Alice',
    },
  });

  console.log(user);
}

main()
  .catch((e) => {
    throw e;
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
```

---

## 프로젝트 구조

```
project/
├── prisma/
│   ├── schema.prisma        # 스키마 정의
│   └── migrations/          # 마이그레이션 히스토리
│       └── 20231128000000_init/
│           └── migration.sql
├── src/
│   ├── generated/
│   │   └── prisma/          # 생성된 클라이언트
│   └── index.ts
├── .env                     # 환경 변수
└── package.json
```

---

## Prisma의 장점

### 1. 타입 안전성

```typescript
// 타입 오류가 컴파일 타임에 감지됨
const user = await prisma.user.findUnique({
  where: { email: 'alice@example.com' },
});

// user.email - OK
// user.invalidField - 컴파일 에러!
```

### 2. 자동 완성

IDE에서 모든 필드와 메서드에 대한 자동 완성 지원

### 3. 관계 처리

```typescript
// 중첩 생성
const userWithPosts = await prisma.user.create({
  data: {
    email: 'bob@example.com',
    posts: {
      create: [
        { title: 'First Post' },
        { title: 'Second Post' },
      ],
    },
  },
  include: { posts: true },
});
```

### 4. 마이그레이션 자동화

스키마 변경 시 SQL 마이그레이션 자동 생성

---

## Prisma 에코시스템

| 제품 | 설명 |
|-----|------|
| **Prisma ORM** | 핵심 ORM 도구 |
| **Prisma Studio** | 데이터베이스 GUI |
| **Prisma Accelerate** | 글로벌 데이터베이스 캐시 |
| **Prisma Optimize** | 쿼리 분석 및 최적화 |
| **Prisma Postgres** | 관리형 PostgreSQL |

---

## 다음 단계

- [Prisma Schema](./02_schema.md)
- [Prisma Client](./03_client.md)
- [Prisma Migrate](./04_migrate.md)
- [관계 정의](./05_relations.md)
- [CRUD 작업](./06_crud.md)
- [CLI 명령어](./07_cli.md)
- [고급 기능](./08_advanced.md)
