# TypeORM Query Builder

## Query Builder란?

Query Builder는 TypeORM의 핵심 기능으로, SQL 쿼리를 우아하고 편리한 문법으로 작성하고 실행할 수 있게 해줍니다. 쿼리 결과는 자동으로 엔티티로 변환됩니다.

## Query Builder 생성

### 방법 1: DataSource 사용

```typescript
const user = await dataSource
    .createQueryBuilder()
    .select("user")
    .from(User, "user")
    .where("user.id = :id", { id: 1 })
    .getOne()
```

### 방법 2: EntityManager 사용

```typescript
const user = await dataSource.manager
    .createQueryBuilder(User, "user")
    .where("user.id = :id", { id: 1 })
    .getOne()
```

### 방법 3: Repository 사용 (권장)

```typescript
const user = await userRepository
    .createQueryBuilder("user")
    .where("user.id = :id", { id: 1 })
    .getOne()
```

## Query Builder 타입

| 타입 | 용도 | 생성 방법 |
|-----|------|---------|
| SelectQueryBuilder | SELECT 쿼리 | `createQueryBuilder()` |
| InsertQueryBuilder | INSERT 쿼리 | `createQueryBuilder().insert()` |
| UpdateQueryBuilder | UPDATE 쿼리 | `createQueryBuilder().update()` |
| DeleteQueryBuilder | DELETE 쿼리 | `createQueryBuilder().delete()` |
| RelationQueryBuilder | 관계 작업 | `createQueryBuilder().relation()` |

## Select Query Builder

### 기본 사용법

```typescript
const users = await userRepository
    .createQueryBuilder("user")
    .getMany()
```

### 결과 조회 메서드

| 메서드 | 반환 타입 | 설명 |
|-------|---------|------|
| `getOne()` | Entity \| null | 단일 엔티티 |
| `getOneOrFail()` | Entity | 단일 엔티티 (없으면 예외) |
| `getMany()` | Entity[] | 엔티티 배열 |
| `getRawOne()` | Object \| null | 변환되지 않은 단일 결과 |
| `getRawMany()` | Object[] | 변환되지 않은 결과 배열 |
| `getCount()` | number | 결과 개수 |
| `stream()` | Stream | 결과 스트림 |

```typescript
// 엔티티로 변환된 결과
const users = await qb.getMany()

// 원시 결과 (JOIN 시 유용)
const rawResults = await qb.getRawMany()
```

### SELECT 절

```typescript
// 전체 선택
qb.select("user")

// 특정 컬럼 선택
qb.select(["user.id", "user.name"])

// 별칭 지정
qb.select("user.firstName", "firstName")

// 추가 선택
qb.select("user.id")
  .addSelect("user.firstName")
  .addSelect("user.lastName")
```

### WHERE 절

#### 기본 조건

```typescript
qb.where("user.name = :name", { name: "John" })
```

#### AND 조건

```typescript
qb.where("user.firstName = :firstName", { firstName: "John" })
  .andWhere("user.lastName = :lastName", { lastName: "Doe" })
```

#### OR 조건

```typescript
qb.where("user.firstName = :firstName", { firstName: "John" })
  .orWhere("user.firstName = :firstName2", { firstName2: "Jane" })
```

#### IN 쿼리

```typescript
qb.where("user.id IN (:...ids)", { ids: [1, 2, 3] })
```

#### 복합 조건 (Brackets)

```typescript
import { Brackets, NotBrackets } from "typeorm"

qb.where("user.isActive = :isActive", { isActive: true })
  .andWhere(new Brackets((subQb) => {
      subQb.where("user.firstName = :firstName", { firstName: "John" })
           .orWhere("user.lastName = :lastName", { lastName: "Doe" })
  }))

// NOT 복합 조건
qb.andWhere(new NotBrackets((subQb) => {
    subQb.where("user.role = :role", { role: "admin" })
}))
```

생성되는 SQL:
```sql
WHERE isActive = true AND (firstName = 'John' OR lastName = 'Doe')
```

### ORDER BY 절

```typescript
// 단일 정렬
qb.orderBy("user.id", "DESC")

// 다중 정렬
qb.orderBy("user.name", "ASC")
  .addOrderBy("user.id", "DESC")

// 객체로 지정
qb.orderBy({
    "user.name": "ASC",
    "user.id": "DESC"
})

// NULL 처리 (PostgreSQL)
qb.orderBy("user.name", "ASC", "NULLS FIRST")
```

### GROUP BY / HAVING 절

```typescript
qb.select("user.role")
  .addSelect("COUNT(user.id)", "count")
  .groupBy("user.role")
  .having("COUNT(user.id) > :minCount", { minCount: 5 })
  .addHaving("COUNT(user.id) < :maxCount", { maxCount: 100 })
```

### DISTINCT

```typescript
qb.select("user.firstName")
  .distinct(true)

// PostgreSQL DISTINCT ON
qb.distinctOn(["user.id"])
  .orderBy("user.id")
```

### LIMIT / OFFSET (페이지네이션)

```typescript
// 페이지네이션 (권장)
qb.skip(10)   // OFFSET 10
  .take(5)    // LIMIT 5

// 직접 지정 (JOIN 시 주의)
qb.limit(5)
  .offset(10)
```

> **주의**: JOIN이 있는 쿼리에서 `limit`/`offset`은 예상과 다르게 동작할 수 있습니다. `take`/`skip`을 사용하세요.

## JOIN

### 관계 로드 포함 JOIN

```typescript
// LEFT JOIN
qb.leftJoinAndSelect("user.photos", "photo")

// INNER JOIN
qb.innerJoinAndSelect("user.profile", "profile")

// 조건부 JOIN
qb.leftJoinAndSelect(
    "user.photos",
    "photo",
    "photo.isPublic = :isPublic",
    { isPublic: true }
)
```

### 관계 로드 제외 JOIN

```typescript
// LEFT JOIN (데이터 로드 X, 조건에만 사용)
qb.leftJoin("user.photos", "photo")
  .where("photo.isPublic = :isPublic", { isPublic: true })

// INNER JOIN
qb.innerJoin("user.profile", "profile")
```

### 임의 테이블 JOIN

```typescript
// 엔티티로 JOIN
qb.leftJoinAndSelect(Photo, "photo", "photo.userId = user.id")

// 테이블명으로 JOIN
qb.leftJoinAndSelect("photos", "photo", "photo.userId = user.id")
```

### 매핑 JOIN

엔티티에 정의되지 않은 속성에 매핑:

```typescript
// 단일 매핑
qb.leftJoinAndMapOne(
    "user.profilePhoto",    // 매핑할 속성
    "user.photos",          // 관계
    "photo",                // 별칭
    "photo.isProfile = TRUE"
)

// 배열 매핑
qb.leftJoinAndMapMany(
    "user.activePhotos",
    "user.photos",
    "photo",
    "photo.isActive = TRUE"
)
```

## 서브쿼리

### WHERE에서 서브쿼리

```typescript
const qb = await postRepository
    .createQueryBuilder("post")
    .where((subQb) => {
        const subQuery = subQb
            .subQuery()
            .select("user.name")
            .from(User, "user")
            .where("user.registered = :registered")
            .getQuery()
        return "post.author IN " + subQuery
    })
    .setParameter("registered", true)
    .getMany()
```

### FROM에서 서브쿼리

```typescript
const qb = await dataSource
    .createQueryBuilder()
    .select("user.name", "name")
    .from((subQuery) => {
        return subQuery
            .select("user.name", "name")
            .from(User, "user")
            .where("user.registered = :registered", { registered: true })
    }, "user")
    .getRawMany()
```

### SELECT에서 서브쿼리

```typescript
qb.addSelect((subQuery) => {
    return subQuery
        .select("COUNT(photo.id)")
        .from(Photo, "photo")
        .where("photo.userId = user.id")
}, "photoCount")
```

## Insert Query Builder

### 기본 삽입

```typescript
await dataSource
    .createQueryBuilder()
    .insert()
    .into(User)
    .values([
        { firstName: "John", lastName: "Doe" },
        { firstName: "Jane", lastName: "Doe" }
    ])
    .execute()
```

### Raw SQL 값

```typescript
await dataSource
    .createQueryBuilder()
    .insert()
    .into(User)
    .values({
        firstName: "John",
        createdAt: () => "NOW()"  // SQL 함수 직접 사용
    })
    .execute()
```

### Upsert (충돌 시 업데이트)

```typescript
await dataSource
    .createQueryBuilder()
    .insert()
    .into(User)
    .values({ id: 1, firstName: "John" })
    .orUpdate(
        ["firstName"],           // 업데이트할 컬럼
        ["id"]                   // 충돌 검사 컬럼
    )
    .execute()
```

### 충돌 무시

```typescript
await dataSource
    .createQueryBuilder()
    .insert()
    .into(User)
    .values({ id: 1, firstName: "John" })
    .orIgnore()
    .execute()
```

## Update Query Builder

### 기본 업데이트

```typescript
await dataSource
    .createQueryBuilder()
    .update(User)
    .set({ firstName: "Updated" })
    .where("id = :id", { id: 1 })
    .execute()
```

### Raw SQL 값

```typescript
await dataSource
    .createQueryBuilder()
    .update(User)
    .set({
        age: () => "age + 1",        // 현재 값에 1 더하기
        updatedAt: () => "NOW()"
    })
    .where("id = :id", { id: 1 })
    .execute()
```

## Delete Query Builder

### 일반 삭제

```typescript
await dataSource
    .createQueryBuilder()
    .delete()
    .from(User)
    .where("id = :id", { id: 1 })
    .execute()
```

### 소프트 삭제

```typescript
await dataSource
    .getRepository(User)
    .createQueryBuilder()
    .softDelete()
    .where("id = :id", { id: 1 })
    .execute()
```

### 소프트 삭제 복구

```typescript
await dataSource
    .getRepository(User)
    .createQueryBuilder()
    .restore()
    .where("id = :id", { id: 1 })
    .execute()
```

## 잠금 (Locking)

### 비관적 잠금 (Pessimistic)

```typescript
// 쓰기 잠금
qb.setLock("pessimistic_write")

// 읽기 잠금
qb.setLock("pessimistic_read")

// Dirty read (커밋되지 않은 데이터 읽기)
qb.setLock("dirty_read")

// 특정 테이블만 잠금
qb.setLock("pessimistic_write", undefined, ["user"])
```

### 낙관적 잠금 (Optimistic)

```typescript
const user = await userRepository.findOneBy({ id: 1 })

qb.setLock("optimistic", user.version)
```

### 잠금 옵션

```typescript
// 대기하지 않음 (잠금 시 즉시 실패)
qb.setLock("pessimistic_write")
  .setOnLocked("nowait")

// 잠긴 행 건너뛰기
qb.setLock("pessimistic_write")
  .setOnLocked("skip_locked")
```

## 숨겨진 컬럼 선택

`select: false`로 설정된 컬럼 조회:

```typescript
@Entity()
class User {
    @Column({ select: false })
    password: string
}

// 명시적으로 선택
const user = await userRepository
    .createQueryBuilder("user")
    .select("user.id")
    .addSelect("user.password")  // 명시적 선택
    .getOne()
```

## 소프트 삭제된 행 조회

```typescript
const users = await userRepository
    .createQueryBuilder("user")
    .withDeleted()  // 삭제된 행 포함
    .getMany()
```

## 쿼리 확인

```typescript
// SQL 쿼리 문자열
const sql = qb.getQuery()

// SQL + 파라미터
const [sql, params] = qb.getQueryAndParameters()

// 콘솔에 출력 후 실행
const users = await qb.printSql().getMany()
```

## Common Table Expressions (CTE)

```typescript
await dataSource
    .createQueryBuilder()
    .select("user")
    .from(User, "user")
    .addCommonTableExpression(
        `SELECT "userId" FROM "post" WHERE "isPublic" = true`,
        "post_users"
    )
    .where(`user.id IN (SELECT "userId" FROM "post_users")`)
    .getMany()
```

## 쿼리 실행 시간 제한

```typescript
qb.maxExecutionTime(1000)  // 1초
```

## 파라미터 이름 주의사항

동일한 파라미터명을 여러 번 사용하면 값이 덮어써집니다:

```typescript
// 잘못된 예
qb.where("user.firstName = :name", { name: "John" })
  .andWhere("user.lastName = :name", { name: "Doe" })
// name이 "Doe"로 덮어써짐

// 올바른 예
qb.where("user.firstName = :firstName", { firstName: "John" })
  .andWhere("user.lastName = :lastName", { lastName: "Doe" })
```

## 참고 자료

- [TypeORM Select Query Builder](https://typeorm.io/select-query-builder)
- [TypeORM Insert Query Builder](https://typeorm.io/insert-query-builder)
- [TypeORM Update Query Builder](https://typeorm.io/update-query-builder)
- [TypeORM Delete Query Builder](https://typeorm.io/delete-query-builder)
