# Prisma CRUD 작업

> 공식 문서: https://www.prisma.io/docs/orm/prisma-client/queries/crud

## 개요

Prisma Client는 타입 안전한 CRUD(Create, Read, Update, Delete) 작업을 제공합니다.

---

## Create (생성)

### create - 단일 레코드 생성

```typescript
const user = await prisma.user.create({
  data: {
    email: 'alice@example.com',
    name: 'Alice',
  },
});
```

### 관계와 함께 생성

```typescript
const user = await prisma.user.create({
  data: {
    email: 'bob@example.com',
    name: 'Bob',
    posts: {
      create: [
        { title: 'First Post' },
        { title: 'Second Post' },
      ],
    },
    profile: {
      create: { bio: 'Developer' },
    },
  },
  include: {
    posts: true,
    profile: true,
  },
});
```

### createMany - 다중 레코드 생성

```typescript
const result = await prisma.user.createMany({
  data: [
    { email: 'user1@example.com', name: 'User 1' },
    { email: 'user2@example.com', name: 'User 2' },
    { email: 'user3@example.com', name: 'User 3' },
  ],
  skipDuplicates: true,  // 중복 무시 (PostgreSQL, SQLite)
});

console.log(result.count);  // 생성된 레코드 수
```

### createManyAndReturn - 생성 후 반환 (v5.14.0+)

```typescript
// PostgreSQL, CockroachDB, SQLite에서만 지원
const users = await prisma.user.createManyAndReturn({
  data: [
    { email: 'user1@example.com' },
    { email: 'user2@example.com' },
  ],
});
```

---

## Read (조회)

### findUnique - 고유 레코드 조회

```typescript
// ID로 조회
const user = await prisma.user.findUnique({
  where: { id: 1 },
});

// 유니크 필드로 조회
const user = await prisma.user.findUnique({
  where: { email: 'alice@example.com' },
});

// 복합 유니크로 조회
const postTag = await prisma.postTag.findUnique({
  where: {
    postId_tagId: {
      postId: 1,
      tagId: 2,
    },
  },
});
```

### findUniqueOrThrow - 없으면 에러

```typescript
const user = await prisma.user.findUniqueOrThrow({
  where: { id: 1 },
});
// 레코드가 없으면 PrismaClientKnownRequestError 발생
```

### findFirst - 첫 번째 레코드 조회

```typescript
const post = await prisma.post.findFirst({
  where: { published: true },
  orderBy: { createdAt: 'desc' },
});
```

### findFirstOrThrow - 없으면 에러

```typescript
const post = await prisma.post.findFirstOrThrow({
  where: { published: true },
});
```

### findMany - 다중 레코드 조회

```typescript
// 모든 레코드
const allUsers = await prisma.user.findMany();

// 필터링
const users = await prisma.user.findMany({
  where: {
    email: { contains: 'example.com' },
  },
  orderBy: { createdAt: 'desc' },
  skip: 0,
  take: 10,
});
```

### 관계 포함 조회

```typescript
const user = await prisma.user.findUnique({
  where: { id: 1 },
  include: {
    posts: true,
    profile: true,
  },
});

// 중첩 포함
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

### 필드 선택

```typescript
const user = await prisma.user.findUnique({
  where: { id: 1 },
  select: {
    id: true,
    email: true,
    posts: {
      select: { title: true },
    },
  },
});
```

---

## Update (수정)

### update - 단일 레코드 수정

```typescript
const user = await prisma.user.update({
  where: { id: 1 },
  data: {
    name: 'Alice Updated',
  },
});
```

### 관계 수정

```typescript
// 관계 연결
const user = await prisma.user.update({
  where: { id: 1 },
  data: {
    posts: {
      connect: { id: 10 },  // 기존 Post 연결
    },
  },
});

// 관계 생성
const user = await prisma.user.update({
  where: { id: 1 },
  data: {
    posts: {
      create: { title: 'New Post' },  // 새 Post 생성
    },
  },
});

// 관계 해제
const user = await prisma.user.update({
  where: { id: 1 },
  data: {
    posts: {
      disconnect: { id: 10 },  // Post 연결 해제
    },
  },
});

// 관계 재설정
const user = await prisma.user.update({
  where: { id: 1 },
  data: {
    posts: {
      set: [{ id: 1 }, { id: 2 }],  // 특정 Post들만 연결
    },
  },
});
```

### updateMany - 다중 레코드 수정

```typescript
const result = await prisma.post.updateMany({
  where: {
    authorId: 1,
    published: false,
  },
  data: {
    published: true,
  },
});

console.log(result.count);  // 수정된 레코드 수
```

### updateManyAndReturn - 수정 후 반환 (v6.2.0+)

```typescript
// PostgreSQL, CockroachDB, SQLite에서만 지원
const posts = await prisma.post.updateManyAndReturn({
  where: { authorId: 1 },
  data: { published: true },
});
```

### 원자적 연산 (숫자 필드)

```typescript
const user = await prisma.user.update({
  where: { id: 1 },
  data: {
    viewCount: { increment: 1 },    // +1
    // viewCount: { decrement: 1 }, // -1
    // balance: { multiply: 2 },    // *2
    // balance: { divide: 2 },      // /2
    // age: { set: 30 },            // 직접 설정
  },
});
```

---

## Upsert (생성 또는 수정)

```typescript
const user = await prisma.user.upsert({
  where: { email: 'alice@example.com' },
  update: {
    name: 'Alice Updated',
  },
  create: {
    email: 'alice@example.com',
    name: 'Alice',
  },
});
```

### 관계와 함께 Upsert

```typescript
const user = await prisma.user.upsert({
  where: { email: 'bob@example.com' },
  update: {
    name: 'Bob Updated',
    posts: {
      upsert: {
        where: { id: 1 },
        update: { title: 'Updated Title' },
        create: { title: 'New Title' },
      },
    },
  },
  create: {
    email: 'bob@example.com',
    name: 'Bob',
    posts: {
      create: { title: 'First Post' },
    },
  },
});
```

---

## Delete (삭제)

### delete - 단일 레코드 삭제

```typescript
const user = await prisma.user.delete({
  where: { id: 1 },
});
```

### deleteMany - 다중 레코드 삭제

```typescript
// 조건에 맞는 레코드 삭제
const result = await prisma.post.deleteMany({
  where: {
    authorId: 1,
    published: false,
  },
});

console.log(result.count);  // 삭제된 레코드 수

// 모든 레코드 삭제
const result = await prisma.post.deleteMany();
```

### 관계 레코드 삭제 주의사항

외래 키 제약이 있는 경우:

```typescript
// 에러: Post가 있는 User는 삭제 불가
await prisma.user.delete({ where: { id: 1 } });

// 해결 방법 1: 관계 먼저 삭제
await prisma.post.deleteMany({ where: { authorId: 1 } });
await prisma.user.delete({ where: { id: 1 } });

// 해결 방법 2: 트랜잭션 사용
await prisma.$transaction([
  prisma.post.deleteMany({ where: { authorId: 1 } }),
  prisma.user.delete({ where: { id: 1 } }),
]);

// 해결 방법 3: Cascade 삭제 설정 (스키마에서)
// onDelete: Cascade
```

---

## 집계 (Aggregation)

### count - 개수

```typescript
const count = await prisma.user.count();

// 조건부 카운트
const count = await prisma.user.count({
  where: {
    posts: {
      some: { published: true },
    },
  },
});

// 관계 개수 포함
const users = await prisma.user.findMany({
  include: {
    _count: {
      select: { posts: true },
    },
  },
});
```

### aggregate - 집계 함수

```typescript
const result = await prisma.product.aggregate({
  _count: { id: true },
  _sum: { price: true },
  _avg: { price: true },
  _min: { price: true },
  _max: { price: true },
  where: {
    available: true,
  },
});

console.log(result._avg.price);
```

### groupBy - 그룹화

```typescript
const result = await prisma.post.groupBy({
  by: ['authorId'],
  _count: { id: true },
  _sum: { viewCount: true },
  having: {
    id: {
      _count: { gt: 5 },  // 5개 이상 포스트를 가진 그룹만
    },
  },
  orderBy: {
    _count: {
      id: 'desc',
    },
  },
});
```

---

## 트랜잭션

### 순차적 트랜잭션

```typescript
const [user, post] = await prisma.$transaction([
  prisma.user.create({ data: { email: 'test@example.com' } }),
  prisma.post.create({ data: { title: 'Hello', authorId: 1 } }),
]);
```

### 인터랙티브 트랜잭션

```typescript
const result = await prisma.$transaction(async (tx) => {
  // 잔액 확인
  const user = await tx.user.findUnique({
    where: { id: 1 },
    select: { balance: true },
  });

  if (user.balance < 100) {
    throw new Error('Insufficient balance');
  }

  // 잔액 차감
  const updatedUser = await tx.user.update({
    where: { id: 1 },
    data: { balance: { decrement: 100 } },
  });

  return updatedUser;
});
```

### 트랜잭션 옵션

```typescript
await prisma.$transaction(
  async (tx) => {
    // ...
  },
  {
    maxWait: 5000,     // 트랜잭션 시작 대기 (ms)
    timeout: 10000,    // 트랜잭션 타임아웃 (ms)
    isolationLevel: 'Serializable',  // 격리 수준
  }
);
```

### 격리 수준

| 레벨 | 설명 |
|-----|------|
| `ReadUncommitted` | 커밋되지 않은 데이터 읽기 가능 |
| `ReadCommitted` | 커밋된 데이터만 읽기 (PostgreSQL 기본) |
| `RepeatableRead` | 트랜잭션 내 일관된 읽기 (MySQL 기본) |
| `Serializable` | 가장 엄격한 격리 |

---

## Raw SQL

### $queryRaw - SELECT

```typescript
const users = await prisma.$queryRaw`
  SELECT * FROM "User" WHERE email = ${email}
`;

// 타입 지정
const users = await prisma.$queryRaw<User[]>`
  SELECT * FROM "User" WHERE id = ${id}
`;
```

### $executeRaw - INSERT, UPDATE, DELETE

```typescript
const result = await prisma.$executeRaw`
  UPDATE "User" SET name = ${name} WHERE id = ${id}
`;

console.log(result);  // 영향받은 행 수
```

### 주의사항

```typescript
// 안전: 파라미터 바인딩 사용
const users = await prisma.$queryRaw`
  SELECT * FROM "User" WHERE id = ${id}
`;

// 위험: SQL Injection 가능
const users = await prisma.$queryRawUnsafe(
  `SELECT * FROM "User" WHERE id = ${id}`  // 사용 금지!
);
```

---

## TypedSQL (v5.19.0+)

### SQL 파일 작성

```sql
-- prisma/sql/getUserById.sql
SELECT id, email, name
FROM "User"
WHERE id = $1
```

### 사용

```typescript
import { getUserById } from '@prisma/client/sql';

const user = await prisma.$queryRawTyped(getUserById(1));
```

---

## 관련 문서

- [Prisma Client](./03_client.md)
- [관계 정의](./05_relations.md)
- [고급 기능](./08_advanced.md)
