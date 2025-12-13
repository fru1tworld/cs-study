# TypeORM 문서 모음

TypeORM 공식 문서를 기반으로 정리한 학습 자료입니다.

> 최종 업데이트: 2025-11-28
> 공식 문서: https://typeorm.io/

## 목차

### 1. [소개 및 시작하기](./01_introduction.md)
- TypeORM이란?
- 주요 특징 및 지원 데이터베이스
- 설치 및 설정
- DataSource 설정
- 다중 DataSource

### 2. [Entity](./02_entity.md)
- Entity 정의
- 기본 키 (Primary Key)
- 컬럼 타입 및 옵션
- 특수 컬럼 데코레이터
- 컬럼 Transformer
- Entity 상속
- Embedded Entity
- View Entity

### 3. [Relations (관계)](./03_relations.md)
- One-to-One (일대일)
- Many-to-One / One-to-Many (다대일 / 일대다)
- Many-to-Many (다대다)
- Relation 옵션 (cascade, onDelete 등)
- @JoinColumn / @JoinTable 옵션
- Eager Loading vs Lazy Loading
- Self-referencing Relations

### 4. [Repository와 EntityManager](./04_repository_entity_manager.md)
- EntityManager API
- Repository API
- Find Options
- 고급 Find 연산자
- Custom Repository
- Active Record 패턴

### 5. [Query Builder](./05_query_builder.md)
- Query Builder 생성
- Select Query Builder
- WHERE, ORDER BY, GROUP BY
- JOIN
- 서브쿼리
- Insert / Update / Delete Query Builder
- 잠금 (Locking)
- 숨겨진 컬럼 선택

### 6. [Migration](./06_migration.md)
- Migration이란?
- DataSource 설정
- Migration CLI
- Migration 생성 (수동/자동)
- Migration 실행 및 되돌리기
- QueryRunner API
- 모범 사례

### 7. [고급 기능](./07_advanced_features.md)
- 트랜잭션 (Transactions)
- 리스너와 구독자 (Listeners & Subscribers)
- 캐싱 (Caching)
- 로깅 (Logging)
- 인덱스 (Indices)
- Active Record vs Data Mapper
- Soft Delete

## 참고 자료

- [TypeORM 공식 문서](https://typeorm.io)
- [TypeORM GitHub](https://github.com/typeorm/typeorm)
- [TypeORM Discord](https://discord.gg/typeorm)
