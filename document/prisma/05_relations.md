# Prisma 관계 (Relations)

> 공식 문서: https://www.prisma.io/docs/orm/prisma-schema/data-model/relations

## 개요

Prisma에서 관계(Relations)는 두 모델 간의 연결을 정의합니다. 관계 필드는 스키마 레벨에서만 존재하며, 실제 데이터베이스에는 저장되지 않습니다.

---

## 관계 유형

| 유형 | 설명 | 예시 |
|-----|------|-----|
| **1:1** | 일대일 | User ↔ Profile |
| **1:N** | 일대다 | User ↔ Post[] |
| **M:N** | 다대다 | Post[] ↔ Category[] |

---

## 관계 핵심 개념

### 관계 필드 vs 스칼라 필드

```prisma
model Post {
  id       Int  @id @default(autoincrement())
  author   User @relation(fields: [authorId], references: [id])  // 관계 필드
  authorId Int  // 스칼라 필드 (외래 키)
}
```

- ** 관계 필드**: 다른 모델을 참조, 데이터베이스에 저장 안 됨
- ** 스칼라 필드**: 실제 외래 키, 데이터베이스에 저장됨

### @relation 속성

```prisma
@relation(fields: [localField], references: [foreignField])
```

---

## 1:1 (일대일) 관계

### 기본 구조

```prisma
model User {
  id      Int      @id @default(autoincrement())
  email   String   @unique
  profile Profile?
}

model Profile {
  id     Int    @id @default(autoincrement())
  bio    String
  user   User   @relation(fields: [userId], references: [id])
  userId Int    @unique  // @unique가 1:1 관계를 보장
}
```

### 외래 키 위치

외래 키는 양쪽 모델 어디에든 위치할 수 있습니다:

```prisma
// Profile에 외래 키
model Profile {
  userId Int  @unique
  user   User @relation(fields: [userId], references: [id])
}

// 또는 User에 외래 키
model User {
  profileId Int?     @unique
  profile   Profile? @relation(fields: [profileId], references: [id])
}
```

### 필수 vs 선택적

```prisma
// 선택적 관계 (User는 Profile이 없어도 됨)
model User {
  profile Profile?
}

model Profile {
  user   User @relation(fields: [userId], references: [id])
  userId Int  @unique
}

// 필수 관계 (양쪽 다 필수)
model User {
  profile Profile
}

model Profile {
  user   User @relation(fields: [userId], references: [id])
  userId Int  @unique
}
```

---

## 1:N (일대다) 관계

### 기본 구조

```prisma
model User {
  id    Int    @id @default(autoincrement())
  email String @unique
  posts Post[]  // 여러 Post를 가질 수 있음
}

model Post {
  id       Int  @id @default(autoincrement())
  title    String
  author   User @relation(fields: [authorId], references: [id])
  authorId Int
}
```

### 필수 vs 선택적

```prisma
// 필수: Post는 반드시 author가 있어야 함
model Post {
  author   User @relation(fields: [authorId], references: [id])
  authorId Int
}

// 선택적: Post는 author 없이 생성 가능
model Post {
  author   User? @relation(fields: [authorId], references: [id])
  authorId Int?
}
```

---

## M:N (다대다) 관계

### 암시적 다대다

Prisma가 중간 테이블을 자동 관리합니다.

```prisma
model Post {
  id         Int        @id @default(autoincrement())
  title      String
  categories Category[]
}

model Category {
  id    Int    @id @default(autoincrement())
  name  String
  posts Post[]
}
```

** 자동 생성되는 중간 테이블:**
- 테이블명: `_CategoryToPost`
- 컬럼: `A` (Category ID), `B` (Post ID)

### 명시적 다대다

추가 필드가 필요할 때 직접 중간 테이블을 정의합니다.

```prisma
model Post {
  id         Int                 @id @default(autoincrement())
  title      String
  categories CategoriesOnPosts[]
}

model Category {
  id    Int                 @id @default(autoincrement())
  name  String
  posts CategoriesOnPosts[]
}

model CategoriesOnPosts {
  post       Post     @relation(fields: [postId], references: [id])
  postId     Int
  category   Category @relation(fields: [categoryId], references: [id])
  categoryId Int
  assignedAt DateTime @default(now())  // 추가 필드
  assignedBy String                     // 추가 필드

  @@id([postId, categoryId])  // 복합 기본 키
}
```

---

## 자기 참조 관계 (Self-relation)

### 일대다 자기 참조

```prisma
model Employee {
  id        Int        @id @default(autoincrement())
  name      String
  managerId Int?
  manager   Employee?  @relation("ManagerSubordinates", fields: [managerId], references: [id])
  subordinates Employee[] @relation("ManagerSubordinates")
}
```

### 다대다 자기 참조

```prisma
model User {
  id         Int    @id @default(autoincrement())
  name       String
  followedBy User[] @relation("UserFollows")
  following  User[] @relation("UserFollows")
}
```

---

## 동일 모델 간 다중 관계

```prisma
model User {
  id           Int     @id @default(autoincrement())
  name         String
  writtenPosts Post[]  @relation("WrittenPosts")
  pinnedPost   Post?   @relation("PinnedPost")
}

model Post {
  id         Int   @id @default(autoincrement())
  title      String
  author     User  @relation("WrittenPosts", fields: [authorId], references: [id])
  authorId   Int
  pinnedBy   User? @relation("PinnedPost", fields: [pinnedById], references: [id])
  pinnedById Int?  @unique
}
```

---

## 참조 액션 (Referential Actions)

관련 레코드 삭제/수정 시 동작을 정의합니다.

### 옵션

| 액션 | 설명 |
|-----|------|
| `Cascade` | 참조된 레코드 삭제 시 함께 삭제 |
| `Restrict` | 참조하는 레코드가 있으면 삭제 차단 |
| `NoAction` | Restrict와 유사, DB에 따라 다름 |
| `SetNull` | 참조 필드를 NULL로 설정 |
| `SetDefault` | 참조 필드를 기본값으로 설정 |

### 사용법

```prisma
model Post {
  id       Int  @id @default(autoincrement())
  author   User @relation(fields: [authorId], references: [id], onDelete: Cascade, onUpdate: Cascade)
  authorId Int
}
```

### 기본값

| 관계 유형 | onDelete | onUpdate |
|---------|----------|----------|
| 선택적 | SetNull | Cascade |
| 필수 | Restrict | Cascade |

### 예시 시나리오

```prisma
// User 삭제 시 모든 Post도 삭제
model Post {
  author   User @relation(fields: [authorId], references: [id], onDelete: Cascade)
  authorId Int
}

// User 삭제 시 Post의 authorId를 NULL로 설정
model Post {
  author   User? @relation(fields: [authorId], references: [id], onDelete: SetNull)
  authorId Int?
}

// User 삭제 차단 (Post가 있으면)
model Post {
  author   User @relation(fields: [authorId], references: [id], onDelete: Restrict)
  authorId Int
}
```

### 데이터베이스별 지원

| DB | Cascade | Restrict | NoAction | SetNull | SetDefault |
|---|---|---|---|---|---|
| PostgreSQL | O | O | O | O | O |
| MySQL | O | O | O | O | X |
| SQLite | O | O | O | O | O |
| SQL Server | O | X | O | O | O |
| MongoDB | O | O | O | O | X |

---

## 관계 쿼리

### 중첩 읽기

```typescript
// include로 관계 포함
const user = await prisma.user.findUnique({
  where: { id: 1 },
  include: {
    posts: true,
    profile: true,
  },
});

// select로 특정 필드만
const user = await prisma.user.findUnique({
  where: { id: 1 },
  select: {
    name: true,
    posts: {
      select: { title: true },
    },
  },
});
```

### 관계 필터

```typescript
// some: 하나 이상 조건 충족
const users = await prisma.user.findMany({
  where: {
    posts: {
      some: { published: true },
    },
  },
});

// every: 모든 레코드가 조건 충족
const users = await prisma.user.findMany({
  where: {
    posts: {
      every: { published: true },
    },
  },
});

// none: 조건 충족하는 레코드 없음
const users = await prisma.user.findMany({
  where: {
    posts: {
      none: { published: false },
    },
  },
});
```

### 중첩 쓰기

```typescript
// 관계와 함께 생성
const user = await prisma.user.create({
  data: {
    email: 'alice@example.com',
    posts: {
      create: [
        { title: 'Post 1' },
        { title: 'Post 2' },
      ],
    },
  },
});

// 기존 레코드 연결
const post = await prisma.post.update({
  where: { id: 1 },
  data: {
    author: {
      connect: { id: 1 },
    },
  },
});

// 연결 또는 생성
const post = await prisma.post.update({
  where: { id: 1 },
  data: {
    author: {
      connectOrCreate: {
        where: { email: 'bob@example.com' },
        create: { email: 'bob@example.com', name: 'Bob' },
      },
    },
  },
});

// 연결 해제
const post = await prisma.post.update({
  where: { id: 1 },
  data: {
    author: {
      disconnect: true,
    },
  },
});
```

---

## 관계 개수 조회

```typescript
const users = await prisma.user.findMany({
  include: {
    _count: {
      select: { posts: true },
    },
  },
});

// 결과: { id, name, ..., _count: { posts: 5 } }
```

---

## Fluent API

관계를 연쇄적으로 탐색합니다.

```typescript
// User의 posts의 categories 조회
const categories = await prisma.user
  .findUnique({ where: { id: 1 } })
  .posts()
  .then(posts =>
    Promise.all(posts.map(post =>
      prisma.post.findUnique({ where: { id: post.id } }).categories()
    ))
  );

// 단순화된 버전
const user = await prisma.user.findUnique({
  where: { id: 1 },
  include: {
    posts: {
      include: {
        categories: true,
      },
    },
  },
});
```

---

## 관련 문서

- [Prisma Schema](./02_schema.md)
- [CRUD 작업](./06_crud.md)
- [Prisma Client](./03_client.md)
