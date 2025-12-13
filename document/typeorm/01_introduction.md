# TypeORM 소개 및 시작하기

## TypeORM이란?

TypeORM은 Node.js, Browser, Cordova, Ionic, React Native, NativeScript, Expo, Electron 플랫폼에서 실행할 수 있는 ORM(Object-Relational Mapping) 라이브러리입니다. TypeScript와 JavaScript(ES5, ES6, ES7, ES8)를 지원하며, 최신 JavaScript 기능을 활용하면서도 기존 데이터베이스와의 호환성을 제공합니다.

## 주요 특징

### 지원 데이터베이스
- MySQL / MariaDB
- PostgreSQL
- SQLite
- Microsoft SQL Server
- Oracle
- MongoDB
- CockroachDB
- SAP HANA

### 지원 패턴
- **Active Record 패턴**: 모델 내부에서 직접 데이터베이스 작업 수행
- **Data Mapper 패턴**: Repository를 통한 데이터베이스 작업 분리

## 설치

### npm을 통한 설치

```bash
npm install typeorm reflect-metadata
```

### 데이터베이스 드라이버 설치

사용하는 데이터베이스에 맞는 드라이버를 설치해야 합니다:

```bash
# MySQL / MariaDB
npm install mysql2

# PostgreSQL
npm install pg

# SQLite
npm install better-sqlite3

# Microsoft SQL Server
npm install mssql

# Oracle
npm install oracledb

# MongoDB
npm install mongodb
```

### TypeScript 설정

`tsconfig.json`에 다음 옵션을 추가해야 합니다:

```json
{
  "compilerOptions": {
    "emitDecoratorMetadata": true,
    "experimentalDecorators": true
  }
}
```

### reflect-metadata 임포트

애플리케이션 진입점에서 `reflect-metadata`를 임포트해야 합니다:

```typescript
import "reflect-metadata"
```

## 빠른 시작

### CLI로 프로젝트 생성

```bash
npx typeorm init --name MyProject --database postgres
```

### 기본 프로젝트 구조

```
MyProject
├── src
│   ├── entity          # 엔티티 클래스
│   │   └── User.ts
│   ├── migration       # 마이그레이션 파일
│   ├── data-source.ts  # DataSource 설정
│   └── index.ts        # 애플리케이션 진입점
├── package.json
└── tsconfig.json
```

## DataSource 설정

### 기본 설정

```typescript
import { DataSource } from "typeorm"
import { User } from "./entity/User"

export const AppDataSource = new DataSource({
    type: "mysql",
    host: "localhost",
    port: 3306,
    username: "root",
    password: "password",
    database: "test",
    synchronize: true,  // 개발 환경에서만 사용
    logging: true,
    entities: [User],
    migrations: [],
    subscribers: [],
})
```

### DataSource 초기화

```typescript
import { AppDataSource } from "./data-source"

AppDataSource.initialize()
    .then(() => {
        console.log("Data Source has been initialized!")
    })
    .catch((error) => {
        console.error("Error during Data Source initialization", error)
    })
```

### 주요 설정 옵션

| 옵션 | 설명 |
|------|------|
| `type` | 데이터베이스 종류 (필수) |
| `host` | 데이터베이스 호스트 |
| `port` | 데이터베이스 포트 |
| `username` | 데이터베이스 사용자명 |
| `password` | 데이터베이스 비밀번호 |
| `database` | 데이터베이스 이름 |
| `entities` | 로드할 엔티티 목록 |
| `synchronize` | 스키마 자동 동기화 (프로덕션에서 false) |
| `logging` | 쿼리 로깅 활성화 |
| `migrations` | 마이그레이션 파일 경로 |
| `subscribers` | 구독자 목록 |

## 다중 DataSource 설정

서로 다른 데이터베이스에 연결해야 할 경우:

```typescript
const MysqlDataSource = new DataSource({
    type: "mysql",
    host: "localhost",
    port: 3306,
    username: "root",
    password: "admin",
    database: "db1",
    entities: [/* entities */],
})

const PostgresDataSource = new DataSource({
    type: "postgres",
    host: "localhost",
    port: 5432,
    username: "root",
    password: "admin",
    database: "db2",
    entities: [/* entities */],
})
```

## 주의사항

### synchronize 옵션
- `synchronize: true`는 개발 환경에서만 사용해야 합니다
- 프로덕션 환경에서는 마이그레이션을 사용해야 합니다
- 데이터 손실 위험이 있으므로 절대 프로덕션에서 사용하지 마세요

### dropSchema 옵션
- 연결할 때마다 스키마를 삭제합니다
- 개발/테스트 환경에서만 사용해야 합니다

## 참고 자료

- [TypeORM 공식 문서](https://typeorm.io)
- [TypeORM GitHub](https://github.com/typeorm/typeorm)
