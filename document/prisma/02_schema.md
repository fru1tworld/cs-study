# Prisma Schema

> 공식 문서: https://www.prisma.io/docs/orm/prisma-schema

## 개요

Prisma Schema는 Prisma ORM의 중앙 구성 파일로, **Prisma Schema Language(PSL)** 를 사용합니다.

---

## 스키마 구조

Prisma Schema는 세 가지 주요 구성 요소로 이루어집니다:

```prisma
// 1. 데이터 소스 (Data Source)
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

// 2. 제너레이터 (Generator)
generator client {
  provider = "prisma-client"
  output   = "../src/generated/prisma"
}

// 3. 데이터 모델 (Data Model)
model User {
  id    Int     @id @default(autoincrement())
  email String  @unique
  name  String?
  posts Post[]
}
```

---

## 1. 데이터 소스 (Data Source)

데이터베이스 연결을 정의합니다.

### 기본 구문

```prisma
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}
```

### 지원되는 Provider

| Provider | 설명 |
|----------|------|
| `postgresql` | PostgreSQL |
| `mysql` | MySQL |
| `sqlite` | SQLite |
| `sqlserver` | Microsoft SQL Server |
| `mongodb` | MongoDB |
| `cockroachdb` | CockroachDB |

### 환경 변수 사용

```prisma
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}
```

```env
# .env
DATABASE_URL="postgresql://user:password@localhost:5432/mydb?schema=public"
```

### Connection URL 형식

```
postgresql://USER:PASSWORD@HOST:PORT/DATABASE?schema=SCHEMA
```

**PostgreSQL 예시:**
```
postgresql://johndoe:mypassword@localhost:5432/mydb?schema=public
```

**MySQL 예시:**
```
mysql://johndoe:mypassword@localhost:3306/mydb
```

### SSL 설정

```prisma
datasource db {
  provider = "postgresql"
  url      = "postgresql://user:pass@localhost:5432/mydb?sslmode=require&sslcert=./certs/client-cert.pem"
}
```

### 주의사항

- ** 단일 데이터 소스만 허용**: 하나의 스키마에는 하나의 `datasource`만 정의 가능
- **Prisma v7 변경사항**: `url`, `directUrl`, `shadowDatabaseUrl` 필드가 deprecated됨

---

## 2. 제너레이터 (Generator)

Prisma Client 생성을 구성합니다.

### prisma-client (v6.16.0+, v7 기본값)

```prisma
generator client {
  provider = "prisma-client"
  output   = "../src/generated/prisma"
}
```

### 옵션

| 옵션 | 설명 | 기본값 |
|-----|------|-------|
| `provider` | 제너레이터 종류 | 필수 |
| `output` | 생성 경로 | 필수 (prisma-client) |
| `runtime` | 런타임 환경 | `nodejs` |
| `moduleFormat` | 모듈 형식 | `esm` |
| `generatedFileExtension` | 파일 확장자 | `ts` |

### 런타임 옵션

```prisma
generator client {
  provider = "prisma-client"
  output   = "../src/generated/prisma"
  runtime  = "nodejs"  // nodejs, deno, bun, workerd, vercel-edge, react-native
}
```

### 모듈 형식

```prisma
generator client {
  provider     = "prisma-client"
  output       = "../src/generated/prisma"
  moduleFormat = "esm"  // esm 또는 cjs
}
```

### 생성되는 파일 구조

```
src/generated/prisma/
├── client.ts       # PrismaClient 및 타입
├── browser.ts      # 브라우저용 타입 (Node.js 의존성 없음)
├── enums.ts        # Enum 정의
├── models.ts       # 모든 모델 타입
└── models/
    ├── User.ts     # 개별 모델 파일
    └── Post.ts
```

### 레거시 제너레이터 (prisma-client-js)

```prisma
// v6.x 이전 방식 (deprecated)
generator client {
  provider        = "prisma-client-js"
  previewFeatures = ["fullTextSearch"]
  binaryTargets   = ["native", "linux-musl"]
}
```

---

## 3. 데이터 모델 (Data Model)

### 모델 정의

```prisma
model User {
  id        Int      @id @default(autoincrement())
  email     String   @unique
  name      String?
  role      Role     @default(USER)
  posts     Post[]
  profile   Profile?
  createdAt DateTime @default(now())
  updatedAt DateTime @updatedAt
}
```

### 스칼라 타입

| Prisma 타입 | PostgreSQL | MySQL | SQLite | MongoDB |
|------------|------------|-------|--------|---------|
| `String` | text/varchar | varchar | TEXT | String |
| `Int` | integer | INT | INTEGER | Int |
| `BigInt` | bigint | BIGINT | INTEGER | Long |
| `Float` | double precision | DOUBLE | REAL | Double |
| `Decimal` | decimal | DECIMAL | DECIMAL | Decimal128 |
| `Boolean` | boolean | TINYINT(1) | INTEGER | Bool |
| `DateTime` | timestamp | DATETIME | - | Date |
| `Json` | jsonb | JSON | - | Object |
| `Bytes` | bytea | LONGBLOB | BLOB | BinData |

### 타입 수정자

| 수정자 | 설명 |
|-------|------|
| `?` | 선택적(Optional) 필드 |
| `[]` | 리스트(배열) 필드 |

```prisma
model User {
  name     String?   // 선택적
  roles    String[]  // 리스트
  tags     String[]  // 배열 (PostgreSQL만 지원)
}
```

---

## 필드 속성 (Attributes)

### @id

기본 키를 정의합니다.

```prisma
model User {
  id Int @id @default(autoincrement())
}

// UUID 사용
model Post {
  id String @id @default(uuid())
}

// CUID 사용
model Comment {
  id String @id @default(cuid())
}
```

### @@id (복합 기본 키)

```prisma
model PostTag {
  postId Int
  tagId  Int

  @@id([postId, tagId])
}
```

### @unique / @@unique

유일성 제약 조건을 정의합니다.

```prisma
model User {
  email String @unique
}

// 복합 유니크
model Post {
  authorId Int
  title    String

  @@unique([authorId, title])
}
```

### @default

기본값을 설정합니다.

```prisma
model User {
  role      Role     @default(USER)
  active    Boolean  @default(true)
  createdAt DateTime @default(now())
}
```

** 사용 가능한 함수:**

| 함수 | 설명 |
|-----|------|
| `autoincrement()` | 자동 증가 정수 |
| `uuid()` | UUID v4 생성 |
| `cuid()` | CUID 생성 |
| `now()` | 현재 타임스탬프 |
| `dbgenerated()` | 데이터베이스 기본값 사용 |

### @updatedAt

레코드 수정 시 자동으로 타임스탬프 갱신

```prisma
model User {
  updatedAt DateTime @updatedAt
}
```

### @relation

관계를 정의합니다.

```prisma
model Post {
  author   User @relation(fields: [authorId], references: [id])
  authorId Int
}
```

### @@index

인덱스를 생성합니다.

```prisma
model Post {
  title   String
  content String

  @@index([title])
  @@index([title, content])
}
```

### @map / @@map

데이터베이스 이름 매핑

```prisma
model User {
  id        Int    @id @map("user_id")
  firstName String @map("first_name")

  @@map("users")
}
```

### @db (네이티브 타입)

데이터베이스 특정 타입 지정

```prisma
model User {
  id       Int     @id
  name     String  @db.VarChar(100)
  age      Int     @db.SmallInt
  balance  Decimal @db.Decimal(10, 2)
  metadata Json    @db.JsonB
}
```

---

## 인덱스 상세 설정

### 기본 인덱스

```prisma
model Post {
  id      Int    @id
  title   String
  content String

  @@index([title])
}
```

### 정렬 순서

```prisma
model Post {
  id        Int      @id
  createdAt DateTime

  @@index([createdAt(sort: Desc)])
}
```

### 인덱스 타입 (PostgreSQL)

```prisma
model Post {
  id      Int    @id
  content String

  // Hash 인덱스
  @@index([id], type: Hash)

  // GIN 인덱스 (JSON, 배열용)
  @@index([content], type: Gin)

  // GiST 인덱스
  @@index([content], type: Gist)
}
```

### 전문 검색 인덱스 (Full-Text)

```prisma
generator client {
  provider        = "prisma-client"
  output          = "../src/generated/prisma"
  previewFeatures = ["fullTextIndex"]
}

model Post {
  id      Int    @id
  title   String
  content String

  @@fulltext([title, content])
}
```

### 길이 제한 (MySQL)

```prisma
model Post {
  id      Int    @id
  content String @db.VarChar(3000)

  @@index([content(length: 100)])
}
```

---

## Enum 정의

```prisma
enum Role {
  USER
  ADMIN
  MODERATOR
}

model User {
  id   Int  @id
  role Role @default(USER)
}
```

### Enum 매핑

```prisma
enum Status {
  ACTIVE    @map("active")
  INACTIVE  @map("inactive")

  @@map("user_status")
}
```

---

## 복합 타입 (MongoDB 전용)

```prisma
type Address {
  street  String
  city    String
  zipCode String
}

model User {
  id      String  @id @map("_id") @db.ObjectId
  address Address
}
```

---

## 멀티 파일 스키마

Prisma 5.15.0부터 여러 `.prisma` 파일로 스키마 분할 가능

```prisma
// schema.prisma
generator client {
  provider        = "prisma-client"
  output          = "../src/generated/prisma"
  previewFeatures = ["prismaSchemaFolder"]
}

datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}
```

```prisma
// models/user.prisma
model User {
  id    Int    @id
  email String @unique
}
```

```prisma
// models/post.prisma
model Post {
  id       Int  @id
  authorId Int
}
```

---

## 주석

```prisma
// 일반 주석 (무시됨)
/// 문서 주석 (AST에 포함됨)
/* 블록 주석 */

/// User 모델에 대한 설명
model User {
  /// 사용자 고유 ID
  id Int @id
}
```

---

## 스키마 유효성 검사 및 포맷팅

```bash
# 스키마 유효성 검사
npx prisma validate

# 스키마 포맷팅
npx prisma format
```

---

## 관련 문서

- [관계 정의](./05_relations.md)
- [CLI 명령어](./07_cli.md)
- [Prisma Migrate](./04_migrate.md)
