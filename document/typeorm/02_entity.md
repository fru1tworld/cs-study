# TypeORM Entity

## Entity란?

Entity는 데이터베이스 테이블(MongoDB에서는 컬렉션)에 매핑되는 클래스입니다. `@Entity()` 데코레이터를 사용하여 정의하며, 각 엔티티는 최소 하나의 기본 키(Primary Key) 컬럼을 포함해야 합니다.

## 기본 Entity 정의

```typescript
import { Entity, Column, PrimaryGeneratedColumn } from "typeorm"

@Entity()
export class User {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    firstName: string

    @Column()
    lastName: string

    @Column()
    isActive: boolean
}
```

이 코드는 다음과 같은 테이블을 생성합니다:

```sql
CREATE TABLE "user" (
    "id" SERIAL PRIMARY KEY,
    "firstName" VARCHAR NOT NULL,
    "lastName" VARCHAR NOT NULL,
    "isActive" BOOLEAN NOT NULL
);
```

## 기본 키(Primary Key) 컬럼

### @PrimaryColumn

수동으로 값을 할당해야 하는 기본 키:

```typescript
@Entity()
export class User {
    @PrimaryColumn()
    id: number
}

// 사용 시
const user = new User()
user.id = 1  // 직접 값 할당 필요
```

### @PrimaryGeneratedColumn

자동 증가(auto-increment) 기본 키:

```typescript
@Entity()
export class User {
    @PrimaryGeneratedColumn()
    id: number
}
```

### @PrimaryGeneratedColumn("uuid")

UUID 자동 생성 기본 키:

```typescript
@Entity()
export class User {
    @PrimaryGeneratedColumn("uuid")
    id: string
}
```

### 복합 기본 키

여러 컬럼을 조합한 기본 키:

```typescript
@Entity()
export class User {
    @PrimaryColumn()
    firstName: string

    @PrimaryColumn()
    lastName: string
}
```

## 컬럼 타입

### 기본 타입

TypeORM은 TypeScript 타입을 자동으로 데이터베이스 타입으로 변환합니다:

```typescript
@Column()
name: string  // varchar

@Column()
age: number  // integer

@Column()
isActive: boolean  // boolean
```

### 명시적 타입 지정

```typescript
@Column({ type: "varchar", length: 200 })
name: string

@Column({ type: "int" })
age: number

@Column({ type: "decimal", precision: 10, scale: 2 })
price: number

@Column({ type: "text" })
description: string

@Column({ type: "datetime" })
createdAt: Date
```

### 특수 컬럼 타입

#### enum 타입

```typescript
export enum UserRole {
    ADMIN = "admin",
    EDITOR = "editor",
    VIEWER = "viewer"
}

@Entity()
export class User {
    @Column({
        type: "enum",
        enum: UserRole,
        default: UserRole.VIEWER
    })
    role: UserRole
}
```

#### simple-array 타입

쉼표로 구분된 문자열로 저장:

```typescript
@Entity()
export class User {
    @Column("simple-array")
    names: string[]  // "Tim,Joe,Deb" 형태로 저장
}
```

#### simple-json 타입

JSON 문자열로 객체 저장:

```typescript
@Entity()
export class User {
    @Column("simple-json")
    profile: { name: string; age: number }
}
```

## 특수 컬럼 데코레이터

### @CreateDateColumn

엔티티 삽입 시 자동으로 현재 시간 설정:

```typescript
@Entity()
export class User {
    @CreateDateColumn()
    createdAt: Date
}
```

### @UpdateDateColumn

엔티티 업데이트 시 자동으로 현재 시간 설정:

```typescript
@Entity()
export class User {
    @UpdateDateColumn()
    updatedAt: Date
}
```

### @DeleteDateColumn

소프트 삭제(Soft Delete) 시간 기록:

```typescript
@Entity()
export class User {
    @DeleteDateColumn()
    deletedAt: Date
}
```

### @VersionColumn

엔티티 버전 관리 (낙관적 잠금):

```typescript
@Entity()
export class User {
    @VersionColumn()
    version: number
}
```

### @Generated

자동 생성 값:

```typescript
@Entity()
export class User {
    @Column()
    @Generated("uuid")
    uuid: string

    @Column()
    @Generated("increment")
    number: number
}
```

## 컬럼 옵션

```typescript
@Column({
    type: "varchar",
    name: "user_name",        // 데이터베이스 컬럼명
    length: 150,              // 문자열 길이
    nullable: false,          // NULL 허용 여부
    unique: true,             // 유니크 제약
    default: "guest",         // 기본값
    select: false,            // 기본 조회에서 제외
    insert: true,             // 삽입 허용 여부
    update: true,             // 업데이트 허용 여부
    comment: "사용자 이름",    // 컬럼 주석
    precision: 10,            // 숫자 정밀도
    scale: 2,                 // 소수점 자릿수
})
name: string
```

### select: false 옵션

민감한 정보를 기본 조회에서 제외:

```typescript
@Entity()
export class User {
    @Column()
    username: string

    @Column({ select: false })
    password: string
}

// 조회 시 password는 포함되지 않음
const user = await userRepository.findOne({ where: { id: 1 } })
// user.password는 undefined

// 명시적으로 선택해야 함
const userWithPassword = await userRepository
    .createQueryBuilder("user")
    .addSelect("user.password")
    .where("user.id = :id", { id: 1 })
    .getOne()
```

## 컬럼 Transformer

데이터베이스와 엔티티 간 값 변환:

```typescript
@Entity()
export class User {
    @Column({
        type: "varchar",
        transformer: {
            to(value: string): string {
                return value.toLowerCase()  // 저장 시 소문자 변환
            },
            from(value: string): string {
                return value.toUpperCase()  // 조회 시 대문자 변환
            }
        }
    })
    email: string
}
```

## Entity 상속

### 구체적 테이블 상속 (Concrete Table Inheritance)

각 자식 클래스가 독립적인 테이블을 가짐:

```typescript
// 부모 클래스 (테이블 없음)
export abstract class Content {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    title: string

    @Column()
    description: string
}

// 자식 클래스들 (각각 테이블 생성)
@Entity()
export class Photo extends Content {
    @Column()
    size: number
}

@Entity()
export class Question extends Content {
    @Column()
    answersCount: number
}
```

### 단일 테이블 상속 (Single Table Inheritance)

모든 자식 클래스가 하나의 테이블 공유:

```typescript
@Entity()
@TableInheritance({ column: { type: "varchar", name: "type" } })
export class Content {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    title: string
}

@ChildEntity()
export class Photo extends Content {
    @Column()
    size: number
}

@ChildEntity()
export class Question extends Content {
    @Column()
    answersCount: number
}
```

## Embedded Entity

코드 중복을 줄이기 위한 합성(Composition) 패턴:

```typescript
// Embedded 클래스 (테이블 없음)
export class Name {
    @Column()
    first: string

    @Column()
    last: string
}

@Entity()
export class User {
    @PrimaryGeneratedColumn()
    id: number

    @Column(() => Name)
    name: Name
}

@Entity()
export class Employee {
    @PrimaryGeneratedColumn()
    id: number

    @Column(() => Name)
    name: Name

    @Column()
    salary: number
}
```

결과 테이블:
- `user`: id, nameFirst, nameLast
- `employee`: id, nameFirst, nameLast, salary

## View Entity

데이터베이스 뷰에 매핑:

```typescript
@ViewEntity({
    expression: `
        SELECT "post"."id" AS "id",
               "post"."name" AS "name",
               "category"."name" AS "categoryName"
        FROM "post" "post"
        LEFT JOIN "category" "category" ON "post"."categoryId" = "category"."id"
    `
})
export class PostCategory {
    @ViewColumn()
    id: number

    @ViewColumn()
    name: string

    @ViewColumn()
    categoryName: string
}
```

### QueryBuilder로 뷰 정의

```typescript
@ViewEntity({
    expression: (dataSource: DataSource) => dataSource
        .createQueryBuilder()
        .select("post.id", "id")
        .addSelect("post.name", "name")
        .addSelect("category.name", "categoryName")
        .from(Post, "post")
        .leftJoin(Category, "category", "category.id = post.categoryId")
})
export class PostCategory {
    @ViewColumn()
    id: number

    @ViewColumn()
    name: string

    @ViewColumn()
    categoryName: string
}
```

## Entity 옵션

```typescript
@Entity({
    name: "users",              // 테이블명
    database: "secondary_db",   // 데이터베이스명
    schema: "public",           // 스키마명
    engine: "InnoDB",           // MySQL 엔진
    synchronize: true,          // 동기화 여부
    orderBy: {                  // 기본 정렬
        name: "ASC",
        id: "DESC"
    }
})
export class User {
    // ...
}
```

## 참고 자료

- [TypeORM Entity 문서](https://typeorm.io/entities)
- [TypeORM Embedded Entities](https://typeorm.io/embedded-entities)
- [TypeORM View Entities](https://typeorm.io/view-entities)
