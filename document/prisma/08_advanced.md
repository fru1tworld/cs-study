# Prisma 고급 기능

> 공식 문서: https://www.prisma.io/docs/orm/prisma-client

## 트랜잭션 상세

### 중첩 쓰기 (Nested Writes)

관련 레코드를 하나의 트랜잭션으로 처리합니다.

```typescript
const user = await prisma.user.create({
  data: {
    email: 'alice@example.com',
    profile: {
      create: { bio: 'Developer' },
    },
    posts: {
      create: [
        { title: 'Post 1' },
        { title: 'Post 2' },
      ],
    },
  },
});
```

### 벌크 작업 (Bulk Operations)

```typescript
// 모두 하나의 트랜잭션으로 실행
await prisma.user.createMany({
  data: users,
});

await prisma.post.updateMany({
  where: { authorId: 1 },
  data: { published: true },
});

await prisma.post.deleteMany({
  where: { authorId: 1 },
});
```

### 순차적 트랜잭션

```typescript
const [users, posts, count] = await prisma.$transaction([
  prisma.user.findMany(),
  prisma.post.findMany(),
  prisma.user.count(),
]);
```

### 인터랙티브 트랜잭션

```typescript
const transfer = await prisma.$transaction(async (tx) => {
  // 1. 잔액 확인
  const sender = await tx.account.findUnique({
    where: { id: senderId },
  });

  if (sender.balance < amount) {
    throw new Error('Insufficient funds');
  }

  // 2. 송금자 잔액 차감
  await tx.account.update({
    where: { id: senderId },
    data: { balance: { decrement: amount } },
  });

  // 3. 수신자 잔액 증가
  await tx.account.update({
    where: { id: receiverId },
    data: { balance: { increment: amount } },
  });

  return { success: true };
});
```

### 트랜잭션 옵션

```typescript
await prisma.$transaction(
  async (tx) => { /* ... */ },
  {
    maxWait: 5000,    // 트랜잭션 획득 대기 시간 (기본: 2000ms)
    timeout: 10000,   // 트랜잭션 타임아웃 (기본: 5000ms)
    isolationLevel: Prisma.TransactionIsolationLevel.Serializable,
  }
);
```

### 격리 수준 (Isolation Level)

```typescript
import { Prisma } from './generated/prisma';

// 사용 가능한 격리 수준
Prisma.TransactionIsolationLevel.ReadUncommitted
Prisma.TransactionIsolationLevel.ReadCommitted
Prisma.TransactionIsolationLevel.RepeatableRead
Prisma.TransactionIsolationLevel.Snapshot        // SQL Server만
Prisma.TransactionIsolationLevel.Serializable
```

---

## Raw SQL 쿼리

### $queryRaw

SELECT 쿼리 실행

```typescript
// 태그드 템플릿 (안전)
const users = await prisma.$queryRaw`
  SELECT * FROM "User" WHERE id = ${id}
`;

// 타입 지정
interface UserResult {
  id: number;
  email: string;
}

const users = await prisma.$queryRaw<UserResult[]>`
  SELECT id, email FROM "User"
`;
```

### $executeRaw

INSERT, UPDATE, DELETE 실행

```typescript
const affectedRows = await prisma.$executeRaw`
  UPDATE "User" SET name = ${name} WHERE id = ${id}
`;

console.log(`${affectedRows} rows affected`);
```

### Prisma.sql 헬퍼

```typescript
import { Prisma } from './generated/prisma';

// 동적 쿼리 구성
const column = 'email';
const value = 'alice@example.com';

const result = await prisma.$queryRaw`
  SELECT * FROM "User" WHERE ${Prisma.raw(column)} = ${value}
`;

// 배열을 IN 절에 사용
const ids = [1, 2, 3];
const users = await prisma.$queryRaw`
  SELECT * FROM "User" WHERE id IN (${Prisma.join(ids)})
`;
```

### TypedSQL (v5.19.0+)

```sql
-- prisma/sql/findUsersByAge.sql
-- @param {Int} $1:minAge
-- @param {Int} $2:maxAge
SELECT id, name, email, age
FROM "User"
WHERE age >= $1 AND age <= $2
ORDER BY age
```

```typescript
import { findUsersByAge } from '@prisma/client/sql';

const users = await prisma.$queryRawTyped(
  findUsersByAge(18, 65)
);
```

---

## 미들웨어

### 미들웨어 정의

```typescript
prisma.$use(async (params, next) => {
  console.log('Before:', params);

  const result = await next(params);

  console.log('After:', result);

  return result;
});
```

### 실행 시간 측정

```typescript
prisma.$use(async (params, next) => {
  const before = Date.now();
  const result = await next(params);
  const after = Date.now();

  console.log(`${params.model}.${params.action}: ${after - before}ms`);

  return result;
});
```

### Soft Delete 구현

```typescript
prisma.$use(async (params, next) => {
  // Delete를 Update로 변환
  if (params.model === 'Post') {
    if (params.action === 'delete') {
      params.action = 'update';
      params.args['data'] = { deleted: true };
    }

    if (params.action === 'deleteMany') {
      params.action = 'updateMany';
      if (params.args.data) {
        params.args.data['deleted'] = true;
      } else {
        params.args['data'] = { deleted: true };
      }
    }
  }

  return next(params);
});

// 조회 시 삭제된 항목 제외
prisma.$use(async (params, next) => {
  if (params.model === 'Post') {
    if (params.action === 'findMany' || params.action === 'findFirst') {
      params.args.where = {
        ...params.args.where,
        deleted: false,
      };
    }
  }

  return next(params);
});
```

---

## Client Extensions (v4.16.0+)

### 기본 사용

```typescript
const prisma = new PrismaClient().$extends({
  name: 'myExtension',

  // 모델 메서드 추가
  model: {
    user: {
      async signUp(email: string) {
        return prisma.user.create({
          data: { email },
        });
      },
    },
  },

  // 결과 변환
  result: {
    user: {
      fullName: {
        needs: { firstName: true, lastName: true },
        compute(user) {
          return `${user.firstName} ${user.lastName}`;
        },
      },
    },
  },

  // 쿼리 확장
  query: {
    user: {
      async findMany({ model, operation, args, query }) {
        // 기본 조건 추가
        args.where = { ...args.where, active: true };
        return query(args);
      },
    },
  },

  // 클라이언트 메서드 추가
  client: {
    async $log(message: string) {
      console.log(message);
    },
  },
});

// 사용
await prisma.user.signUp('alice@example.com');
const user = await prisma.user.findFirst();
console.log(user.fullName);  // 계산된 필드
```

### 결과 확장

```typescript
const prisma = new PrismaClient().$extends({
  result: {
    post: {
      // 계산된 필드
      summary: {
        needs: { content: true },
        compute(post) {
          return post.content.substring(0, 100) + '...';
        },
      },
    },
  },
});
```

### 쿼리 확장

```typescript
const prisma = new PrismaClient().$extends({
  query: {
    $allModels: {
      // 모든 모델의 모든 쿼리에 적용
      async $allOperations({ model, operation, args, query }) {
        const start = performance.now();
        const result = await query(args);
        const end = performance.now();

        console.log(`${model}.${operation}: ${end - start}ms`);

        return result;
      },
    },
  },
});
```

---

## 전문 검색 (Full-Text Search)

### 스키마 설정

```prisma
generator client {
  provider        = "prisma-client"
  output          = "../src/generated/prisma"
  previewFeatures = ["fullTextSearch"]  // PostgreSQL
  // previewFeatures = ["fullTextSearch", "fullTextIndex"]  // MySQL
}

model Post {
  id      Int    @id @default(autoincrement())
  title   String
  content String

  @@fulltext([title, content])  // MySQL만
}
```

### PostgreSQL 전문 검색

```typescript
const posts = await prisma.post.findMany({
  where: {
    title: {
      search: 'database & prisma',  // AND
    },
  },
  orderBy: {
    _relevance: {
      fields: ['title'],
      search: 'database',
      sort: 'desc',
    },
  },
});
```

### MySQL 전문 검색

```typescript
const posts = await prisma.post.findMany({
  where: {
    title: {
      search: 'database prisma',
    },
  },
});
```

---

## 관계 로드 전략 (v5.8.0+)

### Join 전략 (기본값)

```typescript
const users = await prisma.user.findMany({
  include: {
    posts: true,
  },
  relationLoadStrategy: 'join',  // 단일 쿼리
});
```

### Query 전략

```typescript
const users = await prisma.user.findMany({
  include: {
    posts: true,
  },
  relationLoadStrategy: 'query',  // 테이블당 별도 쿼리
});
```

---

## 데이터베이스 연결 최적화

### 연결 풀 설정

```env
DATABASE_URL="postgresql://user:pass@host:5432/db?connection_limit=20&pool_timeout=30"
```

### 서버리스 환경

```typescript
// 연결 제한
const prisma = new PrismaClient({
  datasources: {
    db: {
      url: process.env.DATABASE_URL + '?connection_limit=1',
    },
  },
});

// 함수 외부에서 인스턴스화 (재사용)
export const prisma = new PrismaClient();

export async function handler(event) {
  // prisma 사용
  const users = await prisma.user.findMany();

  // $disconnect 호출하지 않음
  return users;
}
```

### 외부 연결 풀러

**PgBouncer 사용:**

```env
# 풀링된 연결 (쿼리용)
DATABASE_URL="postgresql://user:pass@pgbouncer:6432/db?pgbouncer=true"

# 직접 연결 (마이그레이션용)
DIRECT_URL="postgresql://user:pass@database:5432/db"
```

```prisma
datasource db {
  provider  = "postgresql"
  url       = env("DATABASE_URL")
  directUrl = env("DIRECT_URL")
}
```

---

## 로깅 및 모니터링

### 상세 로깅

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
  console.log('Query:', e.query);
  console.log('Params:', e.params);
  console.log('Duration:', e.duration, 'ms');
});
```

### OpenTelemetry 통합

```typescript
import { PrismaClient } from './generated/prisma';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { PrismaInstrumentation } from '@prisma/instrumentation';

registerInstrumentations({
  instrumentations: [new PrismaInstrumentation()],
});

const prisma = new PrismaClient();
```

---

## 메트릭 (v4.10.0+)

```typescript
const prisma = new PrismaClient({
  log: [{ emit: 'event', level: 'query' }],
});

// JSON 메트릭
const metrics = await prisma.$metrics.json();

// Prometheus 형식
const prometheus = await prisma.$metrics.prometheus();
```

---

## 배치 처리 최적화

### 대량 데이터 처리

```typescript
// 청크 단위로 처리
const BATCH_SIZE = 1000;

async function processLargeDataset(data: any[]) {
  for (let i = 0; i < data.length; i += BATCH_SIZE) {
    const chunk = data.slice(i, i + BATCH_SIZE);

    await prisma.$transaction(
      chunk.map(item =>
        prisma.user.create({ data: item })
      )
    );

    console.log(`Processed ${Math.min(i + BATCH_SIZE, data.length)}/${data.length}`);
  }
}
```

### Cursor 기반 페이지네이션

```typescript
async function* iterateUsers() {
  let cursor: number | undefined;

  while (true) {
    const users = await prisma.user.findMany({
      take: 100,
      skip: cursor ? 1 : 0,
      cursor: cursor ? { id: cursor } : undefined,
      orderBy: { id: 'asc' },
    });

    if (users.length === 0) break;

    yield users;

    cursor = users[users.length - 1].id;
  }
}

// 사용
for await (const batch of iterateUsers()) {
  // 배치 처리
}
```

---

## 테스트

### 단위 테스트 (모킹)

```typescript
import { PrismaClient } from './generated/prisma';
import { mockDeep, DeepMockProxy } from 'jest-mock-extended';

export type MockPrismaClient = DeepMockProxy<PrismaClient>;

export const createMockPrismaClient = (): MockPrismaClient => {
  return mockDeep<PrismaClient>();
};

// 테스트
describe('UserService', () => {
  let prisma: MockPrismaClient;

  beforeEach(() => {
    prisma = createMockPrismaClient();
  });

  it('should create user', async () => {
    const mockUser = { id: 1, email: 'test@example.com' };
    prisma.user.create.mockResolvedValue(mockUser);

    const result = await prisma.user.create({
      data: { email: 'test@example.com' },
    });

    expect(result).toEqual(mockUser);
  });
});
```

### 통합 테스트

```typescript
import { PrismaClient } from './generated/prisma';

const prisma = new PrismaClient();

beforeAll(async () => {
  // 테스트 데이터베이스 설정
});

afterAll(async () => {
  await prisma.$disconnect();
});

beforeEach(async () => {
  // 각 테스트 전 데이터 정리
  await prisma.user.deleteMany();
});

describe('User integration tests', () => {
  it('should create and retrieve user', async () => {
    const user = await prisma.user.create({
      data: { email: 'test@example.com' },
    });

    const found = await prisma.user.findUnique({
      where: { id: user.id },
    });

    expect(found).toEqual(user);
  });
});
```

---

## 관련 문서

- [CRUD 작업](./06_crud.md)
- [Prisma Client](./03_client.md)
- [관계 정의](./05_relations.md)
