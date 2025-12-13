# TypeORM Migration

## Migration이란?

Migration은 데이터베이스 스키마를 업데이트하고 기존 데이터베이스에 새로운 변경 사항을 적용하기 위한 SQL 쿼리가 담긴 파일입니다. 프로덕션 환경에서는 `synchronize: true` 대신 마이그레이션을 사용해야 합니다.

## 왜 Migration을 사용해야 하는가?

### synchronize의 위험성
- `synchronize: true`는 자동으로 스키마를 동기화하지만 데이터 손실 위험이 있습니다
- 프로덕션 환경에서는 절대 사용하면 안 됩니다

### Migration의 장점
- 스키마 변경 이력 추적 가능
- 변경 사항 롤백 가능
- 팀원 간 스키마 동기화
- CI/CD 파이프라인에서 안전하게 배포

## DataSource 설정

```typescript
// data-source.ts
import { DataSource } from "typeorm"

export const AppDataSource = new DataSource({
    type: "postgres",
    host: "localhost",
    port: 5432,
    username: "root",
    password: "password",
    database: "mydb",
    entities: ["src/entity/**/*.ts"],
    migrations: ["src/migration/**/*.ts"],  // 마이그레이션 파일 경로
    synchronize: false,  // 반드시 false
    migrationsRun: false,  // 자동 실행 여부
    migrationsTableName: "migrations",  // 마이그레이션 테이블명
})
```

### 주요 설정 옵션

| 옵션 | 설명 | 기본값 |
|------|------|-------|
| `migrations` | 마이그레이션 파일 경로 | - |
| `migrationsRun` | 앱 시작 시 자동 실행 | `false` |
| `migrationsTableName` | 마이그레이션 정보 테이블명 | `"migrations"` |
| `migrationsTransactionMode` | 트랜잭션 모드 | `"all"` |

### migrationsTransactionMode 옵션

| 값 | 설명 |
|------|------|
| `"all"` | 모든 마이그레이션을 단일 트랜잭션으로 |
| `"each"` | 각 마이그레이션을 개별 트랜잭션으로 |
| `"none"` | 트랜잭션 없이 실행 |

## Migration CLI

TypeORM CLI를 사용하여 마이그레이션을 관리합니다.

### 패키지 설정

```json
// package.json
{
  "scripts": {
    "typeorm": "typeorm-ts-node-commonjs",
    "migration:create": "npm run typeorm migration:create",
    "migration:generate": "npm run typeorm migration:generate -- -d ./src/data-source.ts",
    "migration:run": "npm run typeorm migration:run -- -d ./src/data-source.ts",
    "migration:revert": "npm run typeorm migration:revert -- -d ./src/data-source.ts"
  }
}
```

### ESM 프로젝트

```json
{
  "scripts": {
    "typeorm": "typeorm-ts-node-esm"
  }
}
```

## Migration 생성

### 1. 수동 생성 (빈 파일)

```bash
npx typeorm migration:create src/migration/CreateUserTable
```

생성되는 파일: `{TIMESTAMP}-CreateUserTable.ts`

```typescript
import { MigrationInterface, QueryRunner } from "typeorm"

export class CreateUserTable1234567890123 implements MigrationInterface {

    public async up(queryRunner: QueryRunner): Promise<void> {
        // 마이그레이션 적용 시 실행할 코드
    }

    public async down(queryRunner: QueryRunner): Promise<void> {
        // 마이그레이션 되돌릴 때 실행할 코드
    }
}
```

#### 마이그레이션 코드 작성 예시

```typescript
import { MigrationInterface, QueryRunner } from "typeorm"

export class CreateUserTable1234567890123 implements MigrationInterface {

    public async up(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`
            CREATE TABLE "user" (
                "id" SERIAL PRIMARY KEY,
                "firstName" VARCHAR NOT NULL,
                "lastName" VARCHAR NOT NULL,
                "age" INTEGER
            )
        `)
    }

    public async down(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(`DROP TABLE "user"`)
    }
}
```

### 2. 자동 생성 (엔티티 변경 감지)

TypeORM은 엔티티 변경 사항을 감지하여 마이그레이션을 자동 생성합니다.

```bash
npx typeorm migration:generate src/migration/AddEmailToUser -d ./src/data-source.ts
```

#### 동작 원리
1. 현재 엔티티 정의 분석
2. 데이터베이스 스키마와 비교
3. 차이점에 대한 SQL 자동 생성

#### 예시

엔티티에 email 컬럼 추가:

```typescript
@Entity()
export class User {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    firstName: string

    @Column()  // 새로 추가
    email: string
}
```

자동 생성되는 마이그레이션:

```typescript
export class AddEmailToUser1234567890123 implements MigrationInterface {

    public async up(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(
            `ALTER TABLE "user" ADD "email" VARCHAR NOT NULL`
        )
    }

    public async down(queryRunner: QueryRunner): Promise<void> {
        await queryRunner.query(
            `ALTER TABLE "user" DROP COLUMN "email"`
        )
    }
}
```

### 생성 옵션

```bash
# Pretty 포맷팅 (가독성 향상)
npx typeorm migration:generate src/migration/AddEmailToUser -d ./src/data-source.ts -p

# JavaScript 출력 (CommonJS)
npx typeorm migration:generate src/migration/AddEmailToUser -d ./src/data-source.ts -o

# ESM 출력
npx typeorm migration:generate src/migration/AddEmailToUser -d ./src/data-source.ts --esm
```

## Migration 실행

### 대기 중인 모든 마이그레이션 실행

```bash
npx typeorm migration:run -d ./src/data-source.ts
```

실행 순서: 타임스탬프 순서대로 실행

### TypeScript 파일 직접 실행

```bash
# CommonJS
npx typeorm-ts-node-commonjs migration:run -d ./src/data-source.ts

# ESM
npx typeorm-ts-node-esm migration:run -d ./src/data-source.ts
```

## Migration 되돌리기

### 가장 최근 마이그레이션 되돌리기

```bash
npx typeorm migration:revert -d ./src/data-source.ts
```

### 여러 마이그레이션 되돌리기

명령어를 반복 실행:

```bash
npx typeorm migration:revert -d ./src/data-source.ts
npx typeorm migration:revert -d ./src/data-source.ts
npx typeorm migration:revert -d ./src/data-source.ts
```

## Migration 상태 확인

```bash
npx typeorm migration:show -d ./src/data-source.ts
```

출력 예시:
```
[X] CreateUserTable1234567890123
[X] AddEmailToUser1234567890124
[ ] AddAgeToUser1234567890125    <- 아직 실행되지 않음
```

## QueryRunner API

마이그레이션에서 사용하는 QueryRunner의 주요 메서드:

### 쿼리 실행

```typescript
// 원시 SQL 실행
await queryRunner.query(`SELECT * FROM "user"`)

// 파라미터 바인딩
await queryRunner.query(
    `INSERT INTO "user" ("firstName") VALUES ($1)`,
    ["John"]
)
```

### 테이블 관리

```typescript
// 테이블 생성
await queryRunner.createTable(new Table({
    name: "user",
    columns: [
        {
            name: "id",
            type: "int",
            isPrimary: true,
            isGenerated: true,
            generationStrategy: "increment"
        },
        {
            name: "firstName",
            type: "varchar"
        }
    ]
}))

// 테이블 삭제
await queryRunner.dropTable("user")

// 테이블 이름 변경
await queryRunner.renameTable("user", "users")

// 테이블 존재 여부 확인
const hasTable = await queryRunner.hasTable("user")
```

### 컬럼 관리

```typescript
// 컬럼 추가
await queryRunner.addColumn("user", new TableColumn({
    name: "email",
    type: "varchar",
    isNullable: true
}))

// 컬럼 삭제
await queryRunner.dropColumn("user", "email")

// 컬럼 변경
await queryRunner.changeColumn("user", "firstName", new TableColumn({
    name: "first_name",
    type: "varchar"
}))
```

### 인덱스 관리

```typescript
// 인덱스 생성
await queryRunner.createIndex("user", new TableIndex({
    name: "IDX_USER_EMAIL",
    columnNames: ["email"]
}))

// 유니크 인덱스
await queryRunner.createIndex("user", new TableIndex({
    name: "IDX_USER_EMAIL_UNIQUE",
    columnNames: ["email"],
    isUnique: true
}))

// 인덱스 삭제
await queryRunner.dropIndex("user", "IDX_USER_EMAIL")
```

### 외래 키 관리

```typescript
// 외래 키 생성
await queryRunner.createForeignKey("post", new TableForeignKey({
    columnNames: ["userId"],
    referencedTableName: "user",
    referencedColumnNames: ["id"],
    onDelete: "CASCADE"
}))

// 외래 키 삭제
await queryRunner.dropForeignKey("post", "FK_POST_USER")
```

### 트랜잭션 제어

```typescript
// 트랜잭션 시작
await queryRunner.startTransaction()

try {
    await queryRunner.query(`...`)
    await queryRunner.query(`...`)
    await queryRunner.commitTransaction()
} catch (error) {
    await queryRunner.rollbackTransaction()
    throw error
}
```

## 모범 사례

### 1. 작은 단위로 마이그레이션

```typescript
// 좋은 예: 하나의 변경 사항
export class AddEmailToUser implements MigrationInterface {
    async up(queryRunner: QueryRunner) {
        await queryRunner.addColumn("user", new TableColumn({
            name: "email",
            type: "varchar"
        }))
    }
}

// 나쁜 예: 여러 변경 사항 혼합
export class BigMigration implements MigrationInterface {
    async up(queryRunner: QueryRunner) {
        // 테이블 생성, 컬럼 추가, 인덱스 생성...
    }
}
```

### 2. 항상 down 메서드 구현

```typescript
export class AddEmailToUser implements MigrationInterface {
    async up(queryRunner: QueryRunner) {
        await queryRunner.addColumn("user", new TableColumn({
            name: "email",
            type: "varchar"
        }))
    }

    async down(queryRunner: QueryRunner) {
        await queryRunner.dropColumn("user", "email")
    }
}
```

### 3. 엔티티 변경마다 마이그레이션 생성

엔티티를 변경할 때마다 즉시 마이그레이션을 생성하세요:

```bash
# 엔티티 수정 후
npx typeorm migration:generate src/migration/DescriptiveName -d ./src/data-source.ts
```

### 4. 데이터 마이그레이션 분리

스키마 변경과 데이터 마이그레이션을 분리:

```typescript
// 1. 스키마 변경 (새 컬럼 추가, nullable)
export class AddStatusColumn implements MigrationInterface {
    async up(queryRunner: QueryRunner) {
        await queryRunner.addColumn("user", new TableColumn({
            name: "status",
            type: "varchar",
            isNullable: true  // 먼저 nullable로
        }))
    }
}

// 2. 데이터 마이그레이션
export class PopulateStatusColumn implements MigrationInterface {
    async up(queryRunner: QueryRunner) {
        await queryRunner.query(`
            UPDATE "user" SET "status" = 'active' WHERE "status" IS NULL
        `)
    }
}

// 3. 스키마 변경 (NOT NULL 제약)
export class MakeStatusNotNull implements MigrationInterface {
    async up(queryRunner: QueryRunner) {
        await queryRunner.changeColumn("user", "status", new TableColumn({
            name: "status",
            type: "varchar",
            isNullable: false
        }))
    }
}
```

## 프로덕션 배포

### CI/CD에서 마이그레이션 실행

```yaml
# GitHub Actions 예시
- name: Run Migrations
  run: npm run migration:run
  env:
    DATABASE_URL: ${{ secrets.DATABASE_URL }}
```

### 마이그레이션 실행 확인

```typescript
// 앱 시작 시 마이그레이션 상태 확인
const pendingMigrations = await dataSource.showMigrations()
if (pendingMigrations) {
    console.warn("There are pending migrations!")
}
```

## 참고 자료

- [TypeORM Migrations 문서](https://typeorm.io/migrations)
- [TypeORM CLI](https://typeorm.io/using-cli)
