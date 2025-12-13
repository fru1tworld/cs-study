# Prisma Client

> 공식 문서: https://www.prisma.io/docs/orm/prisma-client

## 개요

Prisma Client는 Prisma Schema를 기반으로 자동 생성되는 타입 안전한 데이터베이스 클라이언트입니다.

---

## 설치 및 설정

### 설치

```bash
npm install @prisma/client
```

### Prisma Client 생성

```bash
npx prisma generate
```

### 기본 사용법

```typescript
import { PrismaClient } from './generated/prisma';

const prisma = new PrismaClient();

async function main() {
  const users = await prisma.user.findMany();
  console.log(users);
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

## PrismaClient 인스턴스

### 싱글톤 패턴

```typescript
// lib/prisma.ts
import { PrismaClient } from './generated/prisma';

const globalForPrisma = globalThis as unknown as {
  prisma: PrismaClient | undefined;
};

export const prisma =
  globalForPrisma.prisma ??
  new PrismaClient({
    log: ['query'],
  });

if (process.env.NODE_ENV !== 'production') {
  globalForPrisma.prisma = prisma;
}
```

### 옵션

```typescript
const prisma = new PrismaClient({
  // 로깅 설정
  log: ['query', 'info', 'warn', 'error'],

  // 에러 포맷팅
  errorFormat: 'pretty', // 'colorless' | 'minimal'

  // 데이터 소스 URL 오버라이드
  datasources: {
    db: {
      url: process.env.DATABASE_URL,
    },
  },
});
```

---

## 연결 관리

### 명시적 연결

```typescript
// 연결
await prisma.$connect();

// 연결 해제
await prisma.$disconnect();
```

### 연결 풀 설정

Connection URL 파라미터로 설정:

```env
DATABASE_URL="postgresql://user:pass@localhost:5432/db?connection_limit=5&pool_timeout=10"
```

| 파라미터 | 설명 | 기본값 |
|---------|------|-------|
| `connection_limit` | 최대 연결 수 | `num_cpus * 2 + 1` |
| `pool_timeout` | 연결 대기 시간(초) | 10 |
| `connect_timeout` | 연결 타임아웃(초) | 5 |

### 서버리스 환경

```typescript
const prisma = new PrismaClient({
  datasources: {
    db: {
      url: process.env.DATABASE_URL + '?connection_limit=1',
    },
  },
});
```

---

## 로깅

### 기본 로깅

```typescript
const prisma = new PrismaClient({
  log: ['query', 'info', 'warn', 'error'],
});
```

### 이벤트 기반 로깅

```typescript
const prisma = new PrismaClient({
  log: [
    { emit: 'event', level: 'query' },
    { emit: 'stdout', level: 'error' },
    { emit: 'stdout', level: 'info' },
    { emit: 'stdout', level: 'warn' },
  ],
});

prisma.$on('query', (e) => {
  console.log('Query: ' + e.query);
  console.log('Params: ' + e.params);
  console.log('Duration: ' + e.duration + 'ms');
});
```

### 로그 레벨

| 레벨 | 설명 |
|-----|------|
| `query` | 실행된 SQL 쿼리 |
| `info` | 일반 정보 |
| `warn` | 경고 |
| `error` | 에러 |

---

## 에러 처리

### Prisma 에러 타입

```typescript
import { Prisma } from './generated/prisma';

try {
  await prisma.user.create({
    data: { email: 'existing@example.com' },
  });
} catch (e) {
  if (e instanceof Prisma.PrismaClientKnownRequestError) {
    // 알려진 에러
    if (e.code === 'P2002') {
      console.log('Unique constraint violation');
    }
  } else if (e instanceof Prisma.PrismaClientUnknownRequestError) {
    // 알 수 없는 에러
  } else if (e instanceof Prisma.PrismaClientValidationError) {
    // 유효성 검사 에러
  }
}
```

### 주요 에러 코드

| 코드 | 설명 |
|-----|------|
| `P2000` | 값이 너무 김 |
| `P2001` | 레코드를 찾을 수 없음 |
| `P2002` | 유니크 제약 조건 위반 |
| `P2003` | 외래 키 제약 조건 위반 |
| `P2025` | 레코드가 존재하지 않음 |

---

## Select와 Include

### Select - 특정 필드 선택

```typescript
const user = await prisma.user.findUnique({
  where: { id: 1 },
  select: {
    email: true,
    name: true,
  },
});
// 결과: { email: '...', name: '...' }
```

### Include - 관계 포함

```typescript
const user = await prisma.user.findUnique({
  where: { id: 1 },
  include: {
    posts: true,
    profile: true,
  },
});
// 결과: { id, email, name, ..., posts: [...], profile: {...} }
```

### 중첩 Select

```typescript
const user = await prisma.user.findUnique({
  where: { id: 1 },
  select: {
    email: true,
    posts: {
      select: {
        title: true,
      },
    },
  },
});
```

### Include 내부의 Select

```typescript
const user = await prisma.user.findUnique({
  where: { id: 1 },
  include: {
    posts: {
      select: {
        title: true,
        content: true,
      },
    },
  },
});
```

### Omit - 특정 필드 제외

```typescript
const user = await prisma.user.findUnique({
  where: { id: 1 },
  omit: {
    password: true,
  },
});
```

---

## 필터링 (where)

### 기본 필터

```typescript
const users = await prisma.user.findMany({
  where: {
    email: 'alice@example.com',
  },
});
```

### 비교 연산자

```typescript
const users = await prisma.user.findMany({
  where: {
    age: { gt: 18 },        // greater than
    // age: { gte: 18 },    // greater than or equal
    // age: { lt: 65 },     // less than
    // age: { lte: 65 },    // less than or equal
    // age: { not: 25 },    // not equal
  },
});
```

### 문자열 필터

```typescript
const users = await prisma.user.findMany({
  where: {
    email: { contains: 'example' },
    // email: { startsWith: 'alice' },
    // email: { endsWith: '.com' },
  },
});
```

### 대소문자 무시 (PostgreSQL, MongoDB)

```typescript
const users = await prisma.user.findMany({
  where: {
    email: {
      contains: 'ALICE',
      mode: 'insensitive',
    },
  },
});
```

### 리스트 필터

```typescript
const users = await prisma.user.findMany({
  where: {
    id: { in: [1, 2, 3] },
    // id: { notIn: [4, 5] },
  },
});
```

### NULL 체크

```typescript
const users = await prisma.user.findMany({
  where: {
    name: null,           // name이 NULL
    // name: { not: null }, // name이 NULL이 아님
  },
});
```

### 논리 연산자

```typescript
// AND
const users = await prisma.user.findMany({
  where: {
    AND: [
      { email: { contains: 'example' } },
      { name: { not: null } },
    ],
  },
});

// OR
const users = await prisma.user.findMany({
  where: {
    OR: [
      { email: { contains: 'gmail' } },
      { email: { contains: 'yahoo' } },
    ],
  },
});

// NOT
const users = await prisma.user.findMany({
  where: {
    NOT: {
      email: { contains: 'test' },
    },
  },
});
```

### 관계 필터

```typescript
// 1:N 관계 필터
const users = await prisma.user.findMany({
  where: {
    posts: {
      some: { published: true },   // 하나 이상
      // every: { published: true }, // 모두
      // none: { published: false }, // 하나도 없음
    },
  },
});

// 1:1 관계 필터
const users = await prisma.user.findMany({
  where: {
    profile: {
      is: { bio: { contains: 'developer' } },
      // isNot: { bio: null },
    },
  },
});
```

---

## 정렬 (orderBy)

### 기본 정렬

```typescript
const users = await prisma.user.findMany({
  orderBy: {
    createdAt: 'desc',
  },
});
```

### 다중 정렬

```typescript
const users = await prisma.user.findMany({
  orderBy: [
    { role: 'asc' },
    { name: 'asc' },
  ],
});
```

### 관계 기준 정렬

```typescript
const posts = await prisma.post.findMany({
  orderBy: {
    author: {
      name: 'asc',
    },
  },
});
```

### 관계 개수 기준 정렬

```typescript
const users = await prisma.user.findMany({
  orderBy: {
    posts: {
      _count: 'desc',
    },
  },
});
```

### NULL 순서 제어

```typescript
const users = await prisma.user.findMany({
  orderBy: {
    name: { sort: 'asc', nulls: 'last' },
  },
});
```

---

## 페이지네이션

### Offset 페이지네이션

```typescript
const users = await prisma.user.findMany({
  skip: 10,  // 건너뛸 레코드 수
  take: 10,  // 가져올 레코드 수
});
```

### Cursor 기반 페이지네이션

```typescript
const users = await prisma.user.findMany({
  take: 10,
  skip: 1,  // 커서 제외
  cursor: {
    id: lastUserId,
  },
  orderBy: {
    id: 'asc',
  },
});
```

### 페이지네이션 비교

| 방식 | 장점 | 단점 |
|-----|-----|-----|
| **Offset** | 특정 페이지로 바로 이동 가능 | 대량 데이터에서 느림 |
| **Cursor** | 대량 데이터에서 효율적 | 특정 페이지로 바로 이동 불가 |

---

## Distinct

### 중복 제거

```typescript
const cities = await prisma.user.findMany({
  distinct: ['city'],
  select: {
    city: true,
  },
});
```

### 다중 필드 Distinct

```typescript
const users = await prisma.user.findMany({
  distinct: ['city', 'country'],
});
```

---

## 타입 안전성

### 생성된 타입 사용

```typescript
import { User, Post, Prisma } from './generated/prisma';

// 모델 타입
const user: User = {
  id: 1,
  email: 'alice@example.com',
  name: 'Alice',
  createdAt: new Date(),
  updatedAt: new Date(),
};

// Input 타입
const createInput: Prisma.UserCreateInput = {
  email: 'bob@example.com',
  name: 'Bob',
};

// Select 결과 타입
type UserWithPosts = Prisma.UserGetPayload<{
  include: { posts: true };
}>;
```

### 동적 쿼리 타입

```typescript
function getUsers<T extends Prisma.UserFindManyArgs>(
  args: Prisma.SelectSubset<T, Prisma.UserFindManyArgs>
) {
  return prisma.user.findMany(args);
}

// 사용
const users = await getUsers({
  select: { email: true },
});
```

---

## 관련 문서

- [CRUD 작업](./06_crud.md)
- [관계 정의](./05_relations.md)
- [고급 기능](./08_advanced.md)
