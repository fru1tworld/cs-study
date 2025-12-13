# TypeORM 고급 기능

## 트랜잭션 (Transactions)

트랜잭션은 여러 데이터베이스 작업을 하나의 원자적 단위로 실행합니다.

### 콜백 방식

```typescript
// DataSource 사용
await dataSource.transaction(async (transactionalEntityManager) => {
  await transactionalEntityManager.save(user);
  await transactionalEntityManager.save(photo);
});

// EntityManager 사용
await dataSource.manager.transaction(async (transactionalEntityManager) => {
  await transactionalEntityManager.save(user);
  await transactionalEntityManager.save(photo);
});
```

> ** 중요**: 트랜잭션 내에서는 반드시 전달받은 `transactionalEntityManager`만 사용해야 합니다. 글로벌 EntityManager를 사용하면 안 됩니다.

### 격리 수준 (Isolation Level)

```typescript
await dataSource.manager.transaction(
  "SERIALIZABLE", // 격리 수준 지정
  async (transactionalEntityManager) => {
    await transactionalEntityManager.save(user);
  }
);
```

| 격리 수준          | 설명                            |
| ------------------ | ------------------------------- |
| `READ UNCOMMITTED` | 커밋되지 않은 데이터 읽기 가능  |
| `READ COMMITTED`   | 커밋된 데이터만 읽기            |
| `REPEATABLE READ`  | 트랜잭션 내 동일 쿼리 동일 결과 |
| `SERIALIZABLE`     | 완전한 격리 (가장 엄격)         |

### QueryRunner를 사용한 수동 제어

더 세밀한 제어가 필요할 때:

```typescript
const queryRunner = dataSource.createQueryRunner();

// 연결 획득
await queryRunner.connect();

// 트랜잭션 시작
await queryRunner.startTransaction();

try {
  await queryRunner.manager.save(user);
  await queryRunner.manager.save(photo);

  // 성공 시 커밋
  await queryRunner.commitTransaction();
} catch (error) {
  // 실패 시 롤백
  await queryRunner.rollbackTransaction();
  throw error;
} finally {
  // 연결 해제
  await queryRunner.release();
}
```

## 리스너와 구독자 (Listeners & Subscribers)

### Entity Listener

엔티티 클래스 내에 정의되는 이벤트 핸들러:

```typescript
@Entity()
export class User {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  firstName: string;

  @Column()
  lastName: string;

  @Column()
  fullName: string;

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;

  // 삽입 전
  @BeforeInsert()
  setFullName() {
    this.fullName = `${this.firstName} ${this.lastName}`;
  }

  // 삽입 후
  @AfterInsert()
  logInsert() {
    console.log(`User ${this.id} has been inserted`);
  }

  // 업데이트 전
  @BeforeUpdate()
  updateFullName() {
    this.fullName = `${this.firstName} ${this.lastName}`;
  }

  // 업데이트 후
  @AfterUpdate()
  logUpdate() {
    console.log(`User ${this.id} has been updated`);
  }

  // 삭제 전
  @BeforeRemove()
  logRemove() {
    console.log(`User ${this.id} will be removed`);
  }

  // 로드 후
  @AfterLoad()
  updateCounters() {
    // 엔티티 로드 후 처리
  }
}
```

### Listener 데코레이터 종류

| 데코레이터          | 실행 시점      |
| ------------------- | -------------- |
| `@AfterLoad`        | 엔티티 로드 후 |
| `@BeforeInsert`     | 삽입 전        |
| `@AfterInsert`      | 삽입 후        |
| `@BeforeUpdate`     | 업데이트 전    |
| `@AfterUpdate`      | 업데이트 후    |
| `@BeforeRemove`     | 삭제 전        |
| `@AfterRemove`      | 삭제 후        |
| `@BeforeSoftRemove` | 소프트 삭제 전 |
| `@AfterSoftRemove`  | 소프트 삭제 후 |
| `@BeforeRecover`    | 복구 전        |
| `@AfterRecover`     | 복구 후        |

> ** 주의**: 리스너 내에서는 데이터베이스 작업을 하지 마세요. 대신 Subscriber를 사용하세요.

### Subscriber

별도 클래스로 정의되는 이벤트 핸들러:

```typescript
import {
  EntitySubscriberInterface,
  EventSubscriber,
  InsertEvent,
  UpdateEvent,
  RemoveEvent,
} from "typeorm";
import { User } from "./entity/User";

@EventSubscriber()
export class UserSubscriber implements EntitySubscriberInterface<User> {
  // 특정 엔티티만 수신
  listenTo() {
    return User;
  }

  // 삽입 전
  beforeInsert(event: InsertEvent<User>) {
    console.log("BEFORE INSERT: ", event.entity);
  }

  // 삽입 후
  afterInsert(event: InsertEvent<User>) {
    console.log("AFTER INSERT: ", event.entity);
  }

  // 업데이트 전
  beforeUpdate(event: UpdateEvent<User>) {
    console.log("BEFORE UPDATE: ", event.entity);
  }

  // 업데이트 후
  afterUpdate(event: UpdateEvent<User>) {
    console.log("AFTER UPDATE: ", event.entity);
  }

  // 삭제 전
  beforeRemove(event: RemoveEvent<User>) {
    console.log("BEFORE REMOVE: ", event.entity);
  }

  // 삭제 후
  afterRemove(event: RemoveEvent<User>) {
    console.log("AFTER REMOVE: ", event.entity);
  }
}
```

### Subscriber에서 데이터베이스 작업

```typescript
@EventSubscriber()
export class AuditSubscriber implements EntitySubscriberInterface {
  async afterInsert(event: InsertEvent<any>) {
    // event.queryRunner 또는 event.manager 사용
    await event.manager.save(AuditLog, {
      action: "INSERT",
      entityId: event.entity.id,
      timestamp: new Date(),
    });
  }
}
```

### Event 객체 속성

| 속성          | 설명                          |
| ------------- | ----------------------------- |
| `dataSource`  | DataSource 인스턴스           |
| `queryRunner` | 트랜잭션에 사용된 QueryRunner |
| `manager`     | EntityManager                 |
| `entity`      | 대상 엔티티                   |

### DataSource에 Subscriber 등록

```typescript
const dataSource = new DataSource({
  // ...
  subscribers: [UserSubscriber, AuditSubscriber],
});
```

## 캐싱 (Caching)

### 캐싱 활성화

```typescript
const dataSource = new DataSource({
  // ...
  cache: true,
});
```

### 쿼리 캐싱

```typescript
// QueryBuilder에서
const users = await userRepository
  .createQueryBuilder("user")
  .where("user.isAdmin = :isAdmin", { isAdmin: true })
  .cache(true)
  .getMany();

// Repository에서
const users = await userRepository.find({
  where: { isAdmin: true },
  cache: true,
});
```

### 캐시 기간 설정

```typescript
// 밀리초 단위
.cache(60000)  // 60초

// ID와 함께
.cache("users_admins", 60000)
```

### 전역 캐시 기간

```typescript
const dataSource = new DataSource({
  // ...
  cache: {
    duration: 30000, // 30초
  },
});
```

### 캐시 저장소

#### 데이터베이스 (기본값)

```typescript
const dataSource = new DataSource({
  cache: true, // query-result-cache 테이블 사용
});
```

#### Redis

```typescript
const dataSource = new DataSource({
  cache: {
    type: "redis",
    options: {
      host: "localhost",
      port: 6379,
    },
  },
});
```

#### ioredis

```typescript
const dataSource = new DataSource({
  cache: {
    type: "ioredis",
    options: {
      host: "localhost",
      port: 6379,
    },
  },
});
```

### 캐시 무효화

```typescript
// 특정 캐시 제거
await dataSource.queryResultCache.remove(["users_admins"]);

// 전체 캐시 제거
await dataSource.queryResultCache.clear();
```

### CLI로 캐시 초기화

```bash
typeorm cache:clear
```

## 로깅 (Logging)

### 기본 로깅 활성화

```typescript
const dataSource = new DataSource({
  logging: true,
});
```

### 로깅 옵션

```typescript
const dataSource = new DataSource({
  logging: ["query", "error"], // 배열로 지정
});
```

| 옵션     | 설명               |
| -------- | ------------------ |
| `query`  | 모든 쿼리 로깅     |
| `error`  | 실패한 쿼리와 에러 |
| `schema` | 스키마 빌드 과정   |
| `warn`   | 내부 경고          |
| `info`   | 정보 메시지        |
| `log`    | 로그 메시지        |
| `"all"`  | 모든 로깅 활성화   |

### 느린 쿼리 로깅

```typescript
const dataSource = new DataSource({
  logging: true,
  maxQueryExecutionTime: 1000, // 1초 이상 걸리는 쿼리 로깅
});
```

### 로거 타입

```typescript
const dataSource = new DataSource({
  logger: "advanced-console", // 색상 및 SQL 하이라이트
  // logger: "simple-console"  // 기본
  // logger: "file"  // ormlogs.log 파일에 저장
});
```

### 커스텀 로거

```typescript
import { Logger, QueryRunner } from "typeorm";

class CustomLogger implements Logger {
  logQuery(query: string, parameters?: any[], queryRunner?: QueryRunner) {
    console.log("Query: ", query);
  }

  logQueryError(
    error: string,
    query: string,
    parameters?: any[],
    queryRunner?: QueryRunner
  ) {
    console.error("Error: ", error);
  }

  logQuerySlow(
    time: number,
    query: string,
    parameters?: any[],
    queryRunner?: QueryRunner
  ) {
    console.warn(`Slow query (${time}ms): `, query);
  }

  logSchemaBuild(message: string, queryRunner?: QueryRunner) {
    console.log("Schema: ", message);
  }

  logMigration(message: string, queryRunner?: QueryRunner) {
    console.log("Migration: ", message);
  }

  log(level: "log" | "info" | "warn", message: any, queryRunner?: QueryRunner) {
    console.log(`[${level}] ${message}`);
  }
}

const dataSource = new DataSource({
  logger: new CustomLogger(),
});
```

## 인덱스 (Indices)

### 컬럼 인덱스

```typescript
@Entity()
export class User {
  @Index()
  @Column()
  firstName: string;

  @Index("IDX_USER_EMAIL") // 이름 지정
  @Column()
  email: string;
}
```

### 유니크 인덱스

```typescript
@Entity()
export class User {
  @Index({ unique: true })
  @Column()
  email: string;
}
```

### 복합 인덱스 (엔티티 레벨)

```typescript
@Entity()
@Index(["firstName", "lastName"])
@Index("IDX_NAME_UNIQUE", ["firstName", "lastName"], { unique: true })
export class User {
  @Column()
  firstName: string;

  @Column()
  lastName: string;
}
```

### 공간 인덱스 (Spatial Index)

```typescript
@Entity()
export class Place {
  @Index({ spatial: true })
  @Column({
    type: "geometry",
    spatialFeatureType: "Point",
    srid: 4326,
  })
  location: string;
}
```

### 동시 생성 (PostgreSQL)

```typescript
@Entity()
@Index("IDX_NAME", ["name"], { concurrent: true })
export class User {
  @Column()
  name: string;
}
```

> ** 주의**: 동시 생성 시 `migrationsTransactionMode: "none"` 설정 필요

## Active Record vs Data Mapper

### Active Record 패턴

모델이 직접 데이터베이스 작업 수행:

```typescript
import { BaseEntity, Entity, Column, PrimaryGeneratedColumn } from "typeorm";

@Entity()
export class User extends BaseEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  firstName: string;

  @Column()
  lastName: string;

  // 커스텀 메서드
  static findByName(firstName: string, lastName: string) {
    return this.createQueryBuilder("user")
      .where("user.firstName = :firstName", { firstName })
      .andWhere("user.lastName = :lastName", { lastName })
      .getMany();
  }
}

// 사용
const user = new User();
user.firstName = "John";
user.lastName = "Doe";
await user.save();

const users = await User.find();
const john = await User.findByName("John", "Doe");
await user.remove();
```

#### 장점

- 간단하고 직관적
- 코드가 짧음
- 소규모 프로젝트에 적합

#### 단점

- 테스트하기 어려움
- 모델과 데이터 접근 로직 결합
- 대규모 프로젝트에서 복잡해짐

### Data Mapper 패턴

Repository를 통해 데이터베이스 작업 분리:

```typescript
@Entity()
export class User {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  firstName: string;

  @Column()
  lastName: string;
}

// 사용
const userRepository = dataSource.getRepository(User);

const user = userRepository.create({
  firstName: "John",
  lastName: "Doe",
});
await userRepository.save(user);

const users = await userRepository.find();
await userRepository.remove(user);
```

#### 장점

- 관심사 분리
- 테스트 용이
- 유지보수성 우수
- 대규모 프로젝트에 적합

#### 단점

- 코드가 더 장황함
- Repository 주입 필요

### 선택 기준

| 상황              | 권장 패턴     |
| ----------------- | ------------- |
| 소규모 프로젝트   | Active Record |
| 대규모 프로젝트   | Data Mapper   |
| 단위 테스트 중요  | Data Mapper   |
| 빠른 프로토타이핑 | Active Record |

## Soft Delete

### 엔티티 설정

```typescript
@Entity()
export class User {
  @PrimaryGeneratedColumn()
  id: number;

  @Column()
  name: string;

  @DeleteDateColumn()
  deletedAt?: Date;
}
```

### 소프트 삭제 실행

```typescript
// Repository 사용
await userRepository.softDelete({ id: 1 });
await userRepository.softRemove(user);

// QueryBuilder 사용
await userRepository
  .createQueryBuilder()
  .softDelete()
  .where("id = :id", { id: 1 })
  .execute();
```

### 소프트 삭제된 데이터 조회

```typescript
// 소프트 삭제 포함 조회
const users = await userRepository.find({
  withDeleted: true,
});

// QueryBuilder
const users = await userRepository
  .createQueryBuilder("user")
  .withDeleted()
  .getMany();
```

### 복구

```typescript
await userRepository.restore({ id: 1 });
await userRepository.recover(user);

// QueryBuilder
await userRepository
  .createQueryBuilder()
  .restore()
  .where("id = :id", { id: 1 })
  .execute();
```

## 참고 자료

- [TypeORM Transactions](https://typeorm.io/transactions)
- [TypeORM Listeners and Subscribers](https://typeorm.io/listeners-and-subscribers)
- [TypeORM Caching](https://typeorm.io/caching)
- [TypeORM Logging](https://typeorm.io/logging)
- [TypeORM Indices](https://typeorm.io/indices)
