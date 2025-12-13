# TypeORM Repository와 EntityManager

## EntityManager란?

EntityManager는 모든 엔티티를 관리(삽입, 업데이트, 삭제, 로드 등)할 수 있는 중앙 집중식 관리자입니다. 단일 위치의 모든 엔티티 Repository 모음과 같습니다.

```typescript
import { AppDataSource } from "./data-source"
import { User } from "./entity/User"

// EntityManager 접근
const manager = AppDataSource.manager

// 사용 예시
const user = await manager.findOneBy(User, { id: 1 })
user.name = "Updated"
await manager.save(user)
```

## Repository란?

Repository는 특정 엔티티에 대한 작업을 캡슐화한 클래스입니다. EntityManager보다 더 명확한 타입 지정이 가능합니다.

```typescript
import { AppDataSource } from "./data-source"
import { User } from "./entity/User"

// Repository 가져오기
const userRepository = AppDataSource.getRepository(User)

// 사용 예시
const user = await userRepository.findOneBy({ id: 1 })
user.name = "Updated"
await userRepository.save(user)
```

## EntityManager API

### 속성

| 속성 | 설명 |
|------|------|
| `dataSource` | EntityManager가 사용하는 DataSource |
| `queryRunner` | 트랜잭션에서 사용되는 QueryRunner |

### 트랜잭션 및 쿼리 실행

#### transaction

여러 데이터베이스 요청을 단일 트랜잭션으로 실행:

```typescript
await manager.transaction(async (transactionalManager) => {
    await transactionalManager.save(user1)
    await transactionalManager.save(user2)
})
```

#### query

원시 SQL 쿼리 실행 (파라미터 바인딩으로 SQL 주입 방지):

```typescript
const users = await manager.query(
    "SELECT * FROM user WHERE name = $1",
    ["John"]
)
```

#### createQueryBuilder

SQL 쿼리 빌더 생성:

```typescript
const users = await manager
    .createQueryBuilder(User, "user")
    .where("user.name = :name", { name: "John" })
    .getMany()
```

### 엔티티 생성 및 조작

#### create

새 엔티티 인스턴스 생성 (데이터베이스에 저장하지 않음):

```typescript
const user = manager.create(User, {
    firstName: "John",
    lastName: "Doe"
})
```

#### merge

여러 엔티티를 단일 엔티티로 병합:

```typescript
const user = manager.create(User, { firstName: "John" })
const mergedUser = manager.merge(User, user, { lastName: "Doe" })
```

#### preload

데이터베이스에서 엔티티 로드 후 새 값으로 대체:

```typescript
const partialUser = { id: 1, firstName: "Updated" }
const user = await manager.preload(User, partialUser)
// user.lastName은 데이터베이스 값 유지
```

### CRUD 작업

#### save

엔티티 저장 (INSERT 또는 UPDATE):

```typescript
// 단일 저장
const user = await manager.save(user)

// 배열 저장
const users = await manager.save([user1, user2])
```

#### insert

새 엔티티 삽입:

```typescript
await manager.insert(User, {
    firstName: "John",
    lastName: "Doe"
})

// 배열 삽입
await manager.insert(User, [user1, user2])
```

#### update

조건에 맞는 엔티티 업데이트:

```typescript
await manager.update(User, { id: 1 }, { firstName: "Updated" })

// 여러 조건
await manager.update(User, { isActive: false }, { isActive: true })
```

#### upsert

삽입 또는 업데이트 (충돌 시):

```typescript
await manager.upsert(
    User,
    { externalId: "abc123", firstName: "John" },
    ["externalId"]  // 충돌 검사 컬럼
)
```

#### delete

조건에 맞는 엔티티 삭제:

```typescript
await manager.delete(User, { id: 1 })
await manager.delete(User, [1, 2, 3])  // ID 배열
```

#### remove

엔티티 객체 삭제:

```typescript
await manager.remove(user)
await manager.remove([user1, user2])
```

#### increment / decrement

컬럼 값 증가/감소:

```typescript
await manager.increment(User, { id: 1 }, "age", 1)
await manager.decrement(User, { id: 1 }, "points", 10)
```

#### softDelete / restore

소프트 삭제 및 복구:

```typescript
await manager.softDelete(User, { id: 1 })
await manager.restore(User, { id: 1 })
```

#### clear

테이블 완전 초기화 (TRUNCATE):

```typescript
await manager.clear(User)
```

### 조회 작업

#### find / findBy

조건에 맞는 엔티티 조회:

```typescript
const users = await manager.find(User)
const users = await manager.find(User, {
    where: { isActive: true },
    order: { name: "ASC" }
})
const users = await manager.findBy(User, { isActive: true })
```

#### findOne / findOneBy

첫 번째 엔티티 조회:

```typescript
const user = await manager.findOne(User, {
    where: { id: 1 }
})
const user = await manager.findOneBy(User, { id: 1 })
```

#### findOneOrFail / findOneByOrFail

조회 실패 시 예외 발생:

```typescript
const user = await manager.findOneOrFail(User, {
    where: { id: 1 }
})
// 결과 없으면 EntityNotFoundError 발생
```

#### findAndCount / findAndCountBy

엔티티와 총 개수 반환 (페이지네이션):

```typescript
const [users, total] = await manager.findAndCount(User, {
    where: { isActive: true },
    take: 10,
    skip: 0
})
```

#### count / countBy

엔티티 개수 조회:

```typescript
const count = await manager.count(User)
const count = await manager.countBy(User, { isActive: true })
```

#### exists / existsBy

엔티티 존재 여부 확인:

```typescript
const exists = await manager.exists(User, {
    where: { email: "test@test.com" }
})
const exists = await manager.existsBy(User, { email: "test@test.com" })
```

### Repository 접근

```typescript
const userRepository = manager.getRepository(User)
const treeRepository = manager.getTreeRepository(Category)
const mongoRepository = manager.getMongoRepository(User)
```

## Repository API

Repository는 EntityManager의 대부분의 메서드를 동일하게 제공하며, 특정 엔티티에 특화되어 있습니다.

### 기본 속성

```typescript
const userRepository = dataSource.getRepository(User)

userRepository.manager      // EntityManager
userRepository.metadata     // 엔티티 메타데이터
userRepository.target       // User 클래스
userRepository.queryRunner  // QueryRunner (트랜잭션에서)
```

### 특수 메서드

#### hasId / getId

기본 키 확인 및 조회:

```typescript
const hasId = userRepository.hasId(user)  // true/false
const id = userRepository.getId(user)     // id 값
```

#### sum / average / minimum / maximum

집계 함수:

```typescript
const sum = await userRepository.sum("salary", { isActive: true })
const avg = await userRepository.average("age")
const min = await userRepository.minimum("age")
const max = await userRepository.maximum("salary")
```

## Find Options

### 기본 옵션

```typescript
const users = await userRepository.find({
    select: {
        firstName: true,
        lastName: true
    },
    relations: {
        profile: true,
        photos: true
    },
    where: {
        firstName: "John",
        isActive: true
    },
    order: {
        id: "DESC"
    },
    skip: 0,
    take: 10,
    cache: true
})
```

### select - 선택할 컬럼

```typescript
// 특정 컬럼만 선택
userRepository.find({
    select: {
        firstName: true,
        lastName: true
    }
})
```

### relations - 관계 로드

```typescript
// 중첩 관계 로드
userRepository.find({
    relations: {
        profile: true,
        photos: {
            photoMetadata: true
        }
    }
})
```

### where - 조건

```typescript
// AND 조건
userRepository.find({
    where: {
        firstName: "John",
        lastName: "Doe"
    }
})

// OR 조건
userRepository.find({
    where: [
        { firstName: "John" },
        { firstName: "Jane" }
    ]
})
```

### order - 정렬

```typescript
userRepository.find({
    order: {
        name: "ASC",
        id: "DESC"
    }
})
```

### skip / take - 페이지네이션

```typescript
userRepository.find({
    skip: 10,   // 10개 건너뛰기
    take: 5     // 5개 가져오기
})
```

### withDeleted - 소프트 삭제 포함

```typescript
userRepository.find({
    withDeleted: true
})
```

### lock - 잠금

```typescript
userRepository.findOne({
    where: { id: 1 },
    lock: { mode: "pessimistic_write" }
})
```

## 고급 Find 연산자

TypeORM은 복잡한 조건을 위한 연산자를 제공합니다:

```typescript
import {
    Not, Equal, LessThan, LessThanOrEqual,
    MoreThan, MoreThanOrEqual, Like, ILike,
    Between, In, Any, IsNull, ArrayContains,
    ArrayContainedBy, ArrayOverlap, Raw,
    And, Or
} from "typeorm"
```

### 비교 연산자

```typescript
// Not: 부정
userRepository.find({ where: { name: Not("John") } })

// Equal: 동일
userRepository.find({ where: { name: Equal("John") } })

// LessThan / MoreThan: 비교
userRepository.find({ where: { age: LessThan(30) } })
userRepository.find({ where: { age: MoreThan(18) } })

// LessThanOrEqual / MoreThanOrEqual
userRepository.find({ where: { age: LessThanOrEqual(30) } })
```

### 문자열 검색

```typescript
// Like: 패턴 매칭 (대소문자 구분)
userRepository.find({ where: { name: Like("%John%") } })

// ILike: 패턴 매칭 (대소문자 무시) - PostgreSQL
userRepository.find({ where: { name: ILike("%john%") } })
```

### 범위 및 목록

```typescript
// Between: 범위
userRepository.find({ where: { age: Between(18, 30) } })

// In: 목록
userRepository.find({ where: { id: In([1, 2, 3]) } })
```

### Null 검사

```typescript
// IsNull: NULL 체크
userRepository.find({ where: { deletedAt: IsNull() } })
```

### 배열 연산자 (PostgreSQL)

```typescript
// ArrayContains: 배열 포함
userRepository.find({ where: { tags: ArrayContains(["TypeScript"]) } })

// ArrayOverlap: 배열 겹침
userRepository.find({ where: { tags: ArrayOverlap(["JS", "TS"]) } })
```

### Raw: 원본 SQL

```typescript
userRepository.find({
    where: {
        createdAt: Raw((alias) => `${alias} > NOW() - INTERVAL '1 day'`)
    }
})

// 파라미터 바인딩
userRepository.find({
    where: {
        age: Raw((alias) => `${alias} > :minAge`, { minAge: 18 })
    }
})
```

### 연산자 조합

```typescript
// And: AND 조건
userRepository.find({
    where: {
        age: And(MoreThan(18), LessThan(65))
    }
})

// Or: OR 조건
userRepository.find({
    where: {
        name: Or(Equal("John"), Like("J%"))
    }
})

// Not과 조합
userRepository.find({
    where: {
        age: Not(LessThan(18))  // age >= 18
    }
})
```

## Custom Repository

커스텀 메서드를 가진 Repository 생성:

```typescript
// 커스텀 Repository 정의
export const UserRepository = dataSource.getRepository(User).extend({
    findByName(firstName: string, lastName: string) {
        return this.createQueryBuilder("user")
            .where("user.firstName = :firstName", { firstName })
            .andWhere("user.lastName = :lastName", { lastName })
            .getMany()
    },

    findActiveUsers() {
        return this.findBy({ isActive: true })
    }
})

// 사용
const users = await UserRepository.findByName("John", "Doe")
```

## Active Record 패턴

Entity가 직접 데이터베이스 작업 수행:

```typescript
import { BaseEntity, Entity, Column, PrimaryGeneratedColumn } from "typeorm"

@Entity()
export class User extends BaseEntity {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    firstName: string

    @Column()
    lastName: string

    @Column()
    isActive: boolean

    // 커스텀 메서드
    static findByName(firstName: string, lastName: string) {
        return this.createQueryBuilder("user")
            .where("user.firstName = :firstName", { firstName })
            .andWhere("user.lastName = :lastName", { lastName })
            .getMany()
    }
}

// 사용
const user = new User()
user.firstName = "John"
user.lastName = "Doe"
await user.save()

const users = await User.find({ where: { isActive: true } })
const john = await User.findByName("John", "Doe")
```

## 참고 자료

- [TypeORM EntityManager API](https://typeorm.io/entity-manager-api)
- [TypeORM Repository API](https://typeorm.io/repository-api)
- [TypeORM Find Options](https://typeorm.io/find-options)
