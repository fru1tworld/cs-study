# 관계형 데이터베이스 접근

## 개요

Go를 사용하면 다양한 데이터베이스와 데이터 접근 방식을 애플리케이션에 통합할 수 있습니다. 표준 라이브러리의 [`database/sql`](https://pkg.go.dev/database/sql) 패키지는 관계형 데이터베이스에 접근하는 데 사용됩니다.

소개 튜토리얼은 [튜토리얼: 관계형 데이터베이스 접근](/doc/tutorial/database-access)을 참조하세요.

## 대체 데이터 접근 기술

Go는 표준 `database/sql` 패키지 외에도 추가적인 데이터 접근 기술을 지원합니다:

### 객체-관계 매핑(ORM) 라이브러리
`database/sql`이 저수준 데이터 접근 로직을 제공하는 반면, 더 높은 수준의 추상화도 사용할 수 있습니다:
- **[GORM](https://gorm.io/index.html)** - ([패키지 레퍼런스](https://pkg.go.dev/gorm.io/gorm))
- **[ent](https://entgo.io/)** - ([패키지 레퍼런스](https://pkg.go.dev/entgo.io/ent))

### NoSQL 데이터 저장소
Go 커뮤니티는 대부분의 NoSQL 데이터 저장소를 위한 드라이버를 개발했습니다:
- **[MongoDB](https://docs.mongodb.com/drivers/go/)**
- **[Couchbase](https://docs.couchbase.com/go-sdk/current/hello-world/overview.html)**

추가 드라이버는 [pkg.go.dev](https://pkg.go.dev/)에서 찾을 수 있습니다.

## 지원되는 데이터베이스 관리 시스템

Go는 모든 일반적인 관계형 데이터베이스 관리 시스템을 지원합니다:
- MySQL
- Oracle
- PostgreSQL
- SQL Server
- SQLite
- 기타

전체 드라이버 목록은 [SQLDrivers](/wiki/SQLDrivers) 페이지에서 확인할 수 있습니다.

## 주요 기능 및 주제

### 쿼리 실행 또는 데이터베이스 변경을 위한 함수

`database/sql` 패키지는 특수화된 함수를 제공합니다:
- **`Query`** / **`QueryRow`** - 쿼리 실행
  - `QueryRow`는 단일 행 결과에 최적화되어 오버헤드를 줄임
- **`Exec`** - `INSERT`, `UPDATE`, 또는 `DELETE` 문으로 데이터베이스 변경

관련 문서:
- [데이터를 반환하지 않는 SQL 문 실행](/doc/database/change-data)
- [데이터 쿼리](/doc/database/querying)

### 트랜잭션

`sql.Tx`를 통해 트랜잭션으로 데이터베이스 작업을 실행할 수 있습니다:
- 여러 작업이 함께 실행됨
- **커밋**(모든 변경 사항을 원자적으로 적용) 또는 **롤백**(변경 사항 폐기)으로 종료

자세한 내용은 [트랜잭션 실행](/doc/database/execute-transactions)을 참조하세요.

### 쿼리 취소

`context.Context`를 사용하여 데이터베이스 작업을 취소합니다:
- 클라이언트 연결이 닫힐 때 취소
- 작업이 원하는 시간을 초과하면 취소
- database/sql 함수는 `Context`를 인수로 받음
- 작업에 대한 타임아웃 또는 마감 시간 지정
- 리소스를 해제하기 위해 취소 요청을 전파

[진행 중인 작업 취소](/doc/database/cancel-operations)를 참조하세요.

### 관리되는 연결 풀

`sql.DB` 데이터베이스 핸들에는 필요에 따라 자동으로 연결을 생성하고 폐기하는 내장 연결 풀이 포함되어 있습니다.

**기본 사용:**
- `sql.DB`는 Go로 데이터베이스에 접근하는 가장 일반적인 방법
- [데이터베이스 핸들 열기](/doc/database/open-handle) 참조

**고급 구성:**
- 연결 풀 속성을 수동으로 구성
- [연결 풀 속성 설정](/doc/database/manage-connections#connection_pool_properties) 참조

**전용 단일 연결:**
- 트랜잭션이 부적절할 때 [`sql.Conn`](https://pkg.go.dev/database/sql#Conn) 사용

**전용 연결 사용 사례:**
- 커스텀 트랜잭션 시맨틱으로 DDL을 통한 스키마 변경
- 임시 테이블을 생성하는 쿼리 잠금 작업 수행

자세한 내용은 [전용 연결 사용](/doc/database/manage-connections#dedicated_connections)을 참조하세요.
