# TypeORM Relations (관계)

## Relations란?

TypeORM의 Relations는 엔티티 간의 관계를 정의하는 기능입니다. 데이터베이스의 외래 키 관계를 객체 지향적으로 표현할 수 있습니다.

## 관계 종류

| 관계 타입 | 데코레이터 | 설명 |
|----------|-----------|------|
| 일대일 | `@OneToOne` | A는 B 하나만, B도 A 하나만 가짐 |
| 다대일 | `@ManyToOne` | A 여러 개가 B 하나를 참조 |
| 일대다 | `@OneToMany` | A 하나가 B 여러 개를 소유 |
| 다대다 | `@ManyToMany` | A 여러 개와 B 여러 개가 서로 연결 |

## One-to-One (일대일)

### 단방향 관계

한 쪽에서만 관계를 참조:

```typescript
// Profile 엔티티
@Entity()
export class Profile {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    gender: string

    @Column()
    photo: string
}

// User 엔티티
@Entity()
export class User {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    name: string

    @OneToOne(() => Profile)
    @JoinColumn()  // 외래 키를 가지는 쪽에 필수
    profile: Profile
}
```

생성되는 테이블:
```sql
-- profile 테이블
id | gender | photo

-- user 테이블
id | name | profileId (FK)
```

### 양방향 관계

양쪽 모두에서 관계 참조 가능:

```typescript
@Entity()
export class Profile {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    gender: string

    @OneToOne(() => User, user => user.profile)
    user: User
}

@Entity()
export class User {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    name: string

    @OneToOne(() => Profile, profile => profile.user)
    @JoinColumn()  // 한쪽에만 지정
    profile: Profile
}
```

### 데이터 저장

```typescript
// cascade 없이 저장
const profile = new Profile()
profile.gender = "male"
profile.photo = "me.jpg"
await dataSource.manager.save(profile)

const user = new User()
user.name = "Joe"
user.profile = profile
await dataSource.manager.save(user)

// cascade와 함께 저장
const user = new User()
user.name = "Joe"
user.profile = new Profile()
user.profile.gender = "male"
await dataSource.manager.save(user)  // profile도 함께 저장됨
```

### 데이터 조회

```typescript
// find 옵션 사용
const users = await userRepository.find({
    relations: {
        profile: true
    }
})

// QueryBuilder 사용
const users = await userRepository
    .createQueryBuilder("user")
    .leftJoinAndSelect("user.profile", "profile")
    .getMany()
```

## Many-to-One / One-to-Many (다대일 / 일대다)

User가 여러 Photo를 가지는 관계:

```typescript
@Entity()
export class Photo {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    url: string

    @ManyToOne(() => User, user => user.photos)
    user: User  // 외래 키가 자동 생성됨
}

@Entity()
export class User {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    name: string

    @OneToMany(() => Photo, photo => photo.user)
    photos: Photo[]
}
```

> **중요**: `@ManyToOne`은 독립적으로 사용 가능하지만, `@OneToMany`는 반드시 `@ManyToOne`과 함께 사용해야 합니다.

### 외래 키만 있는 관계

`@OneToMany`를 생략할 수 있습니다:

```typescript
@Entity()
export class Photo {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    url: string

    @ManyToOne(() => User)
    user: User
}

@Entity()
export class User {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    name: string
    // photos 관계 없음
}
```

### 데이터 저장 및 조회

```typescript
// 저장
const photo1 = new Photo()
photo1.url = "photo1.jpg"

const photo2 = new Photo()
photo2.url = "photo2.jpg"

const user = new User()
user.name = "John"
user.photos = [photo1, photo2]
await dataSource.manager.save(user)

// 조회
const users = await userRepository.find({
    relations: {
        photos: true
    }
})
```

## Many-to-Many (다대다)

Category와 Question이 서로 여러 개씩 연결:

```typescript
@Entity()
export class Category {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    name: string

    @ManyToMany(() => Question, question => question.categories)
    questions: Question[]
}

@Entity()
export class Question {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    title: string

    @ManyToMany(() => Category, category => category.questions)
    @JoinTable()  // 한쪽에만 필요
    categories: Category[]
}
```

생성되는 테이블:
```sql
-- category 테이블
id | name

-- question 테이블
id | title

-- question_categories_category (조인 테이블)
questionId | categoryId
```

### 데이터 저장

```typescript
const category1 = new Category()
category1.name = "TypeScript"

const category2 = new Category()
category2.name = "Programming"

const question = new Question()
question.title = "How to use TypeORM?"
question.categories = [category1, category2]
await dataSource.manager.save(question)
```

### 관계 삭제

조인 테이블의 레코드만 삭제 (원본 엔티티는 유지):

```typescript
const question = await questionRepository.findOne({
    where: { id: 1 },
    relations: { categories: true }
})

// 특정 카테고리만 제거
question.categories = question.categories.filter(
    category => category.id !== removeId
)
await questionRepository.save(question)
```

### 추가 프로퍼티가 있는 다대다

조인 테이블에 추가 컬럼이 필요한 경우, 중간 엔티티 생성:

```typescript
@Entity()
export class Category {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    name: string

    @OneToMany(() => QuestionToCategory, qtc => qtc.category)
    questionToCategories: QuestionToCategory[]
}

@Entity()
export class Question {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    title: string

    @OneToMany(() => QuestionToCategory, qtc => qtc.question)
    questionToCategories: QuestionToCategory[]
}

// 중간 엔티티
@Entity()
export class QuestionToCategory {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    order: number  // 추가 프로퍼티

    @ManyToOne(() => Question, question => question.questionToCategories)
    question: Question

    @ManyToOne(() => Category, category => category.questionToCategories)
    category: Category
}
```

## Relation 옵션

```typescript
@ManyToOne(() => Category, category => category.posts, {
    eager: false,           // 자동 로딩 여부
    cascade: true,          // 연쇄 저장/삭제
    onDelete: "CASCADE",    // 참조 삭제 시 동작
    nullable: true,         // NULL 허용
    orphanedRowAction: "delete"  // 고아 레코드 처리
})
category: Category
```

### cascade 옵션

```typescript
// boolean 또는 배열로 설정
cascade: true
cascade: ["insert"]
cascade: ["update"]
cascade: ["remove"]
cascade: ["soft-remove"]
cascade: ["recover"]
cascade: ["insert", "update"]
```

**주의**: cascade는 강력하지만 보안 위험과 예상치 못한 버그를 유발할 수 있습니다.

### onDelete 옵션

| 옵션 | 설명 |
|------|------|
| `RESTRICT` | 참조된 행 삭제 방지 |
| `CASCADE` | 참조하는 행도 함께 삭제 |
| `SET NULL` | 외래 키를 NULL로 설정 |
| `NO ACTION` | 아무 작업도 하지 않음 |

### orphanedRowAction 옵션

부모에서 자식 관계가 제거되었을 때:

| 옵션 | 설명 |
|------|------|
| `delete` | 완전 삭제 |
| `soft-delete` | 소프트 삭제 |
| `nullify` | 외래 키 NULL 설정 |
| `disable` | 아무 작업 안 함 |

## @JoinColumn 옵션

외래 키 컬럼 커스터마이징:

```typescript
@OneToOne(() => Profile)
@JoinColumn({
    name: "profile_id",           // 외래 키 컬럼명
    referencedColumnName: "id"    // 참조할 컬럼명
})
profile: Profile

// 복합 외래 키
@JoinColumn([
    { name: "category_id", referencedColumnName: "id" },
    { name: "locale_code", referencedColumnName: "locale" }
])
```

## @JoinTable 옵션

다대다 조인 테이블 커스터마이징:

```typescript
@ManyToMany(() => Category)
@JoinTable({
    name: "question_categories",  // 조인 테이블명
    joinColumn: {
        name: "question_id",
        referencedColumnName: "id"
    },
    inverseJoinColumn: {
        name: "category_id",
        referencedColumnName: "id"
    }
})
categories: Category[]
```

## Eager Loading vs Lazy Loading

### Eager Loading (즉시 로딩)

find 메서드 사용 시 자동으로 관계 로드:

```typescript
@Entity()
export class Question {
    @ManyToMany(() => Category, {
        eager: true  // 항상 자동 로드
    })
    @JoinTable()
    categories: Category[]
}

// 자동으로 categories 포함
const questions = await questionRepository.find()
```

> **주의**: eager는 관계의 한 쪽에만 설정 가능합니다. QueryBuilder에서는 작동하지 않습니다.

### Lazy Loading (지연 로딩)

접근 시점에 데이터 로드 (Promise 타입 사용):

```typescript
@Entity()
export class Question {
    @ManyToMany(() => Category)
    @JoinTable()
    categories: Promise<Category[]>  // Promise 타입
}

// 사용
const question = await questionRepository.findOne({
    where: { id: 1 }
})
const categories = await question.categories  // 이 시점에 로드
```

**주의**: Lazy loading은 실험적 기능입니다.

## Self-referencing Relations

엔티티가 자기 자신을 참조:

```typescript
@Entity()
export class Category {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    name: string

    // 부모 카테고리
    @ManyToOne(() => Category, category => category.children)
    parent: Category

    // 자식 카테고리들
    @OneToMany(() => Category, category => category.parent)
    children: Category[]
}
```

## 참고 자료

- [TypeORM Relations 문서](https://typeorm.io/relations)
- [TypeORM Eager/Lazy Relations](https://typeorm.io/eager-and-lazy-relations)
