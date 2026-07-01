# GraphQL Spec 상세 정리

## 목차

1. [개요](#1-개요)
2. [역사](#2-역사)
3. [핵심 개념](#3-핵심-개념)
4. [타입 시스템 상세](#4-타입-시스템-상세)
5. [인트로스펙션(Introspection)](#5-인트로스펙션introspection)
6. [실행 모델](#6-실행-모델)
7. [N+1 문제와 DataLoader](#7-n1-문제와-dataloader)
8. [보안 고려사항](#8-보안-고려사항)
9. [주요 구현체 및 도구](#9-주요-구현체-및-도구)
10. [장점과 단점](#10-장점과-단점)
11. [실제 예시](#11-실제-예시)

---

## 1. 개요

### GraphQL이란?

GraphQL은 API를 위한 쿼리 언어(Query Language) 이자, 서버 측에서 데이터를 제공하기 위한 런타임(Runtime) 이다. 클라이언트가 필요한 데이터의 구조를 직접 명시하면, 서버는 정확히 그 구조에 맞는 데이터만 반환한다. 이름에서 알 수 있듯이 "Graph" + "Query Language"의 합성어로, 데이터를 그래프 구조로 바라보고 질의한다는 철학을 담고 있다.

### 왜 만들어졌는가?

Facebook은 2012년 모바일 앱을 개발하면서 기존 REST API의 한계에 직면했다. 모바일 환경에서는 네트워크 대역폭이 제한적이고, 하나의 화면을 그리기 위해 여러 엔드포인트를 호출해야 하는 REST의 구조가 성능 병목을 일으켰다. 이 문제를 해결하기 위해 "클라이언트가 원하는 데이터를 한 번의 요청으로 정확히 받아올 수 있는 방법"을 고민한 결과 GraphQL이 탄생했다.

### REST와의 핵심 차이

| 비교 항목 | REST | GraphQL |
|---|---|---|
| 엔드포인트 | 리소스마다 별도 URL (`/users`, `/posts`) | 단일 엔드포인트 (`/graphql`) |
| 데이터 결정 주체 | 서버가 응답 구조를 결정 | 클라이언트가 필요한 필드를 명시 |
| Over-fetching | 불필요한 필드까지 전부 반환 | 요청한 필드만 정확히 반환 |
| Under-fetching | 한 화면에 여러 API 호출 필요 | 한 번의 쿼리로 연관 데이터 모두 조회 |
| 버전 관리 | URL 버전닝 (`/v1/`, `/v2/`) | 스키마 진화(evolution)로 버전 없이 운영 |
| 타입 시스템 | 별도 명세(OpenAPI 등) 필요 | 스키마 자체가 강력한 타입 시스템 |
| 실시간 통신 | 별도 프로토콜(WebSocket 등) 필요 | Subscription으로 스펙에 내장 |

REST에서는 `/users/1`을 호출하면 서버가 정해놓은 모든 필드가 반환되지만, GraphQL에서는 클라이언트가 `{ user(id: 1) { name email } }`처럼 name과 email만 요청하면 그것만 돌아온다.

---

## 2. 역사

### Facebook에서의 탄생 (2012)

2012년, Facebook은 모바일 앱(iOS)의 뉴스피드를 재구축하면서 기존 REST API의 구조적 한계를 절감했다. 뉴스피드 하나를 렌더링하기 위해 게시글, 작성자, 댓글, 좋아요 등 수많은 엔드포인트를 호출해야 했고, 이는 느린 모바일 네트워크에서 치명적인 성능 저하를 야기했다. Lee Byron, Dan Schafer, Nick Schrock 세 명의 엔지니어가 중심이 되어 내부 프로젝트로 GraphQL을 설계하고, Facebook 모바일 앱에 적용하기 시작했다.

### 오픈소스화 (2015)

2015년 React.js Conf에서 GraphQL이 처음으로 외부에 공개되었다. 같은 해 7월, Facebook은 GraphQL 스펙 초안과 참조 구현체인 `graphql-js`를 오픈소스로 공개했다. 이후 GitHub, Shopify, Twitter 등 주요 기업들이 GraphQL을 도입하면서 커뮤니티가 급속도로 성장했다.

### GraphQL Foundation 설립 (2018~2019)

2018년 11월, Facebook은 GraphQL의 중립적 거버넌스를 위해 Linux Foundation 산하에 GraphQL Foundation을 설립했다. 이를 통해 GraphQL은 특정 기업에 종속되지 않는 오픈 표준으로 자리매김했다. 스펙 관리, 참조 구현체 유지보수, 상표권 관리 등이 Foundation을 통해 이루어진다.

### 주요 타임라인

| 연도 | 사건 |
|---|---|
| 2012 | Facebook 내부에서 GraphQL 개발 시작 |
| 2015 | GraphQL 스펙 초안 및 graphql-js 오픈소스 공개 |
| 2016 | GitHub API v4를 GraphQL로 공개 |
| 2017 | Apollo, Prisma 등 생태계 도구 폭발적 성장 |
| 2018 | GraphQL Foundation 설립 발표 |
| 2019 | GraphQL Foundation 공식 출범 (Linux Foundation 산하) |
| 2021 | GraphQL 스펙 October 2021 Edition 발표 |

---

## 3. 핵심 개념

### 3.1 스키마(Schema)와 타입 시스템

GraphQL의 근간은 스키마(Schema) 이다. 스키마는 API가 제공하는 데이터의 형태를 타입으로 정의하며, 클라이언트와 서버 사이의 계약(Contract) 역할을 한다. 모든 GraphQL 서비스는 반드시 스키마를 가져야 하며, 스키마는 SDL(Schema Definition Language)로 작성된다.

#### Scalar 타입

GraphQL에 내장된 기본 스칼라 타입은 다음 5가지이다.

| 타입 | 설명 | 예시 |
|---|---|---|
| `Int` | 부호 있는 32비트 정수 | `42` |
| `Float` | 부호 있는 배정밀도 부동소수점 | `3.14` |
| `String` | UTF-8 문자열 | `"hello"` |
| `Boolean` | 참/거짓 | `true` |
| `ID` | 고유 식별자 (문자열로 직렬화) | `"abc123"` |

커스텀 스칼라도 정의할 수 있다.

```graphql
scalar DateTime
scalar JSON
scalar URL
```

#### Object 타입

GraphQL에서 가장 기본적이고 중요한 타입이다. 이름이 있는 필드들의 집합으로, 각 필드는 다른 타입을 참조할 수 있다.

```graphql
type User {
  id: ID!
  name: String!
  email: String
  age: Int
  posts: [Post!]!
}

type Post {
  id: ID!
  title: String!
  content: String!
  author: User!
  createdAt: DateTime!
}
```

#### Enum 타입

허용되는 값의 집합을 정의한다. 서버와 클라이언트 양쪽에서 유효한 값만 사용하도록 강제할 수 있다.

```graphql
enum Role {
  ADMIN
  EDITOR
  VIEWER
}

enum PostStatus {
  DRAFT
  PUBLISHED
  ARCHIVED
}
```

#### Interface 타입

여러 Object 타입이 공유하는 필드 집합을 정의한다. 인터페이스를 구현하는 Object 타입은 인터페이스에 정의된 모든 필드를 반드시 포함해야 한다.

```graphql
interface Node {
  id: ID!
}

interface Timestamped {
  createdAt: DateTime!
  updatedAt: DateTime!
}

type User implements Node & Timestamped {
  id: ID!
  name: String!
  createdAt: DateTime!
  updatedAt: DateTime!
}

type Post implements Node & Timestamped {
  id: ID!
  title: String!
  createdAt: DateTime!
  updatedAt: DateTime!
}
```

#### Union 타입

여러 Object 타입 중 하나가 될 수 있는 타입을 정의한다. Interface와 달리 공통 필드를 요구하지 않는다.

```graphql
union SearchResult = User | Post | Comment

type Query {
  search(term: String!): [SearchResult!]!
}
```

Union 타입을 쿼리할 때는 인라인 프래그먼트를 사용하여 각 타입별 필드를 지정해야 한다.

```graphql
query {
  search(term: "GraphQL") {
    ... on User {
      name
      email
    }
    ... on Post {
      title
      content
    }
    ... on Comment {
      body
    }
  }
}
```

#### Input 타입

뮤테이션이나 쿼리의 인자로 복잡한 객체를 전달할 때 사용한다. 일반 Object 타입과 달리 `input` 키워드를 사용하며, 리졸버를 가질 수 없다.

```graphql
input CreateUserInput {
  name: String!
  email: String!
  age: Int
  role: Role = VIEWER
}

input UpdateUserInput {
  name: String
  email: String
  age: Int
}

type Mutation {
  createUser(input: CreateUserInput!): User!
  updateUser(id: ID!, input: UpdateUserInput!): User!
}
```

### 3.2 쿼리(Query), 뮤테이션(Mutation), 서브스크립션(Subscription)

GraphQL 스키마에는 세 가지 특별한 루트 타입이 존재한다.

#### Query (조회)

데이터를 읽어오는 작업을 정의한다. REST의 GET에 해당하며, 모든 GraphQL 스키마에 반드시 존재해야 하는 유일한 루트 타입이다.

```graphql
type Query {
  user(id: ID!): User
  users(limit: Int, offset: Int): [User!]!
  post(id: ID!): Post
  searchPosts(keyword: String!): [Post!]!
}
```

```graphql
# 클라이언트 쿼리 예시
query GetUser {
  user(id: "1") {
    name
    email
    posts {
      title
      createdAt
    }
  }
}
```

Query의 필드들은 병렬(parallel) 로 실행될 수 있다.

#### Mutation (변경)

데이터를 생성, 수정, 삭제하는 작업을 정의한다. REST의 POST, PUT, PATCH, DELETE에 해당한다.

```graphql
type Mutation {
  createUser(input: CreateUserInput!): User!
  updateUser(id: ID!, input: UpdateUserInput!): User!
  deleteUser(id: ID!): Boolean!
  publishPost(id: ID!): Post!
}
```

```graphql
# 클라이언트 뮤테이션 예시
mutation CreateNewUser {
  createUser(input: {
    name: "홍길동"
    email: "hong@example.com"
    age: 30
  }) {
    id
    name
    email
  }
}
```

Mutation의 최상위 필드들은 순차적(serial) 으로 실행된다. 이는 side effect가 있는 작업의 예측 가능한 실행 순서를 보장하기 위함이다.

#### Subscription (구독)

서버에서 클라이언트로 실시간 데이터 푸시를 정의한다. 주로 WebSocket을 통해 구현되며, 이벤트 기반으로 동작한다.

```graphql
type Subscription {
  postCreated: Post!
  commentAdded(postId: ID!): Comment!
  userStatusChanged(userId: ID!): UserStatus!
}
```

```graphql
# 클라이언트 서브스크립션 예시
subscription OnNewPost {
  postCreated {
    id
    title
    author {
      name
    }
  }
}
```

### 3.3 리졸버(Resolver)

리졸버는 스키마의 각 필드에 대해 실제 데이터를 반환하는 함수이다. GraphQL 서버의 실행 엔진은 쿼리를 파싱하고 검증한 후, 각 필드에 매핑된 리졸버를 호출하여 데이터를 수집한다.

리졸버 함수는 일반적으로 4개의 인자를 받는다.

```javascript
// JavaScript(graphql-js) 기준 리졸버 시그니처
const resolvers = {
  Query: {
    user: (parent, args, context, info) => {
      // parent  : 부모 필드의 리졸버가 반환한 값
      // args    : 클라이언트가 전달한 인자 (예: { id: "1" })
      // context : 모든 리졸버가 공유하는 컨텍스트 (DB 커넥션, 인증 정보 등)
      // info    : 쿼리의 AST, 스키마 정보 등 실행 메타데이터
      return database.users.findById(args.id);
    },
  },

  User: {
    posts: (parent, args, context, info) => {
      // parent는 상위 user 리졸버가 반환한 User 객체
      return database.posts.findByAuthorId(parent.id);
    },
  },
};
```

리졸버 체인: GraphQL은 쿼리의 각 필드를 재귀적으로 순회하면서 리졸버를 호출한다. 부모 리졸버의 반환값이 자식 리졸버의 `parent` 인자로 전달된다. 스칼라 타입에 도달하면 재귀가 종료된다.

기본 리졸버(Default Resolver): 리졸버를 명시적으로 정의하지 않으면, `parent[fieldName]`을 반환하는 기본 리졸버가 적용된다. 따라서 데이터 구조가 스키마와 동일하면 별도의 리졸버가 필요 없다.

### 3.4 필드(Field)와 인자(Arguments)

#### 필드(Field)

GraphQL 쿼리에서 요청하는 데이터의 최소 단위이다. 각 필드는 특정 타입을 반환한다.

```graphql
query {
  user(id: "1") {     # user 필드 -> User 타입 반환
    name               # name 필드 -> String 타입 반환
    email              # email 필드 -> String 타입 반환
    posts {            # posts 필드 -> [Post!]! 타입 반환
      title            # title 필드 -> String 타입 반환
    }
  }
}
```

별칭(Alias): 같은 필드를 다른 인자로 여러 번 요청할 때 사용한다.

```graphql
query {
  admin: user(id: "1") {
    name
  }
  guest: user(id: "2") {
    name
  }
}
```

응답:

```json
{
  "data": {
    "admin": { "name": "관리자" },
    "guest": { "name": "게스트" }
  }
}
```

#### 인자(Arguments)

필드에 전달되는 키-값 쌍이다. 모든 필드와 중첩 객체는 인자를 가질 수 있으며, 이를 통해 데이터 필터링, 정렬, 페이지네이션 등을 구현한다.

```graphql
type Query {
  users(
    limit: Int = 10        # 기본값 지정 가능
    offset: Int = 0
    role: Role
    sortBy: String
    order: SortOrder = ASC
  ): [User!]!

  post(id: ID!): Post      # !는 필수 인자
}
```

### 3.5 프래그먼트(Fragment)

프래그먼트는 재사용 가능한 필드 집합이다. 여러 쿼리에서 동일한 필드 세트를 반복해서 작성하는 것을 방지하고, 쿼리의 가독성을 높여준다.

#### 이름 있는 프래그먼트(Named Fragment)

```graphql
# 프래그먼트 정의
fragment UserBasicInfo on User {
  id
  name
  email
  avatar
}

fragment PostSummary on Post {
  id
  title
  createdAt
  author {
    ...UserBasicInfo
  }
}

# 프래그먼트 사용
query GetDashboard {
  currentUser {
    ...UserBasicInfo
    role
  }
  recentPosts {
    ...PostSummary
  }
  popularPosts {
    ...PostSummary
    viewCount
  }
}
```

#### 인라인 프래그먼트(Inline Fragment)

Union이나 Interface 타입에서 특정 타입의 필드에 접근할 때 사용한다.

```graphql
query {
  search(term: "GraphQL") {
    ... on User {
      name
      email
    }
    ... on Post {
      title
      content
    }
  }
}
```

### 3.6 변수(Variables)

쿼리 문자열에 값을 직접 하드코딩하는 대신, 변수를 사용하여 동적으로 값을 전달할 수 있다. 이를 통해 쿼리 문자열을 재사용하고, 클라이언트 측에서 안전하게 값을 주입할 수 있다.

```graphql
# 쿼리 정의 (변수 선언)
query GetUser($userId: ID!, $includeEmail: Boolean = false) {
  user(id: $userId) {
    name
    email @include(if: $includeEmail)
    posts {
      title
    }
  }
}
```

```json
// 변수 값 (별도의 JSON으로 전달)
{
  "userId": "123",
  "includeEmail": true
}
```

변수 타입 뒤에 `!`가 붙으면 필수, `= defaultValue` 형태로 기본값을 지정할 수 있다.

### 3.7 디렉티브(Directives)

디렉티브는 쿼리의 실행 방식을 동적으로 변경하는 데코레이터이다. `@` 기호로 시작한다.

#### 내장 디렉티브

`@skip(if: Boolean!)` : 조건이 `true`이면 해당 필드를 건너뛴다.

```graphql
query GetUser($skipEmail: Boolean!) {
  user(id: "1") {
    name
    email @skip(if: $skipEmail)
  }
}
```

`@include(if: Boolean!)` : 조건이 `true`일 때만 해당 필드를 포함한다.

```graphql
query GetUser($showPosts: Boolean!) {
  user(id: "1") {
    name
    posts @include(if: $showPosts) {
      title
    }
  }
}
```

`@deprecated(reason: String)` : 스키마에서 특정 필드나 Enum 값이 더 이상 사용되지 않음을 표시한다. 인트로스펙션을 통해 클라이언트에 전달된다.

```graphql
type User {
  id: ID!
  name: String!
  username: String @deprecated(reason: "name 필드를 사용하세요.")
  email: String!
}

enum Status {
  ACTIVE
  INACTIVE @deprecated(reason: "DISABLED를 사용하세요.")
  DISABLED
}
```

`@specifiedBy(url: String!)` : 커스텀 스칼라 타입의 스펙 URL을 지정한다 (2021 스펙에서 추가).

```graphql
scalar DateTime @specifiedBy(url: "https://scalars.graphql.org/andimarek/date-time")
```

#### 커스텀 디렉티브

서버 구현체에 따라 커스텀 디렉티브를 정의할 수 있다 (스펙 자체가 아닌 구현체 확장).

```graphql
# 스키마 디렉티브 정의
directive @auth(requires: Role!) on FIELD_DEFINITION

type Query {
  publicPosts: [Post!]!
  adminDashboard: Dashboard! @auth(requires: ADMIN)
}

# 캐시 디렉티브
directive @cacheControl(maxAge: Int!) on FIELD_DEFINITION

type Post {
  id: ID!
  title: String! @cacheControl(maxAge: 3600)
  viewCount: Int! @cacheControl(maxAge: 60)
}
```

---

## 4. 타입 시스템 상세

GraphQL의 타입 시스템은 타입 수식어(Type Modifier) 를 통해 각 필드의 null 허용 여부와 리스트 여부를 세밀하게 제어한다.

### Non-Null (`!`)

기본적으로 GraphQL의 모든 필드는 nullable이다. 타입 뒤에 `!`를 붙이면 해당 필드는 절대 `null`을 반환할 수 없다.

```graphql
type User {
  id: ID!           # null 불가 - 반드시 ID 값이 존재
  name: String!     # null 불가 - 반드시 문자열 반환
  email: String     # null 허용 - null이 반환될 수 있음
  bio: String       # null 허용
}
```

Non-Null 필드의 리졸버가 `null`을 반환하면 GraphQL 실행 엔진은 에러를 발생시키고, 부모 필드로 에러를 전파(propagation) 한다. 부모도 Non-Null이면 그 위로 계속 전파되어 최악의 경우 전체 `data`가 `null`이 될 수 있다.

### List (`[]`)

배열을 나타낸다. Non-Null과 조합하여 다양한 의미를 표현할 수 있다.

| 타입 표기 | null 허용 | 빈 배열 | 배열 내 null |
|---|---|---|---|
| `[String]` | 필드 자체가 null 가능 | 가능 | 가능 |
| `[String]!` | 필드 자체는 null 불가 | 가능 | 가능 |
| `[String!]` | 필드 자체가 null 가능 | 가능 | 불가 |
| `[String!]!` | 필드 자체는 null 불가 | 가능 | 불가 |

구체적인 유효/무효 예시:

```
[String]   -> null, [], ["a", null, "b"] 모두 유효
[String]!  -> null은 무효, [], ["a", null] 유효
[String!]  -> null 유효, [] 유효, ["a", null] 무효
[String!]! -> null 무효, [] 유효, ["a", "b"] 유효, ["a", null] 무효
```

### 루트 타입과 `schema` 키워드

스키마의 진입점을 명시적으로 정의할 수 있다.

```graphql
schema {
  query: Query
  mutation: Mutation
  subscription: Subscription
}
```

타입 이름이 `Query`, `Mutation`, `Subscription`과 정확히 일치하면 `schema` 블록은 생략할 수 있다 (대부분의 구현체가 이를 지원).

### 타입 확장 (Type Extension)

기존에 정의된 타입을 확장할 수 있다. 모듈화된 스키마 설계에 유용하다.

```graphql
# 기본 스키마
type Query {
  users: [User!]!
}

# 다른 모듈에서 확장
extend type Query {
  posts: [Post!]!
}

extend type User {
  followers: [User!]!
}
```

---

## 5. 인트로스펙션(Introspection)

GraphQL의 가장 강력한 기능 중 하나로, 스키마 자체를 쿼리로 조회할 수 있는 시스템이다. 이를 통해 클라이언트는 서버가 어떤 타입, 필드, 인자를 지원하는지 런타임에 알아낼 수 있다.

### 인트로스펙션 쿼리 예시

```graphql
# 스키마의 모든 타입 조회
query IntrospectionQuery {
  __schema {
    types {
      name
      kind
      description
    }
    queryType {
      name
    }
    mutationType {
      name
    }
    subscriptionType {
      name
    }
  }
}
```

```graphql
# 특정 타입의 상세 정보 조회
query TypeDetail {
  __type(name: "User") {
    name
    kind
    fields {
      name
      type {
        name
        kind
        ofType {
          name
          kind
        }
      }
      args {
        name
        type {
          name
        }
        defaultValue
      }
      isDeprecated
      deprecationReason
    }
  }
}
```

### 주요 인트로스펙션 타입

| 타입 | 설명 |
|---|---|
| `__Schema` | 스키마 전체 정보 (루트 타입, 모든 타입, 디렉티브) |
| `__Type` | 개별 타입 정보 (이름, 종류, 필드, 인터페이스 등) |
| `__Field` | 필드 정보 (이름, 타입, 인자, deprecated 여부) |
| `__InputValue` | 입력 값 정보 (필드 인자, Input 타입 필드) |
| `__EnumValue` | Enum 값 정보 |
| `__Directive` | 디렉티브 정보 |

### `__typename` 메타 필드

모든 Object 타입에서 사용할 수 있는 특별한 필드로, 해당 객체의 타입 이름을 문자열로 반환한다. Union/Interface에서 실제 타입을 식별할 때 유용하다.

```graphql
query {
  search(term: "GraphQL") {
    __typename
    ... on User {
      name
    }
    ... on Post {
      title
    }
  }
}
```

응답:

```json
{
  "data": {
    "search": [
      { "__typename": "User", "name": "홍길동" },
      { "__typename": "Post", "title": "GraphQL 입문" }
    ]
  }
}
```

GraphiQL, Apollo Studio 같은 개발 도구들은 인트로스펙션을 활용하여 자동 완성, 문서 생성 등의 기능을 제공한다. 프로덕션 환경에서는 보안을 위해 인트로스펙션을 비활성화하는 것이 권장된다.

---

## 6. 실행 모델

GraphQL 서버가 클라이언트의 요청을 처리하는 과정은 크게 3단계로 나뉜다.

### 6.1 파싱 (Parsing)

클라이언트가 보낸 쿼리 문자열을 AST(Abstract Syntax Tree) 로 변환한다. 이 과정에서 GraphQL 문법에 맞지 않는 쿼리는 문법 에러(Syntax Error)로 거부된다.

```
쿼리 문자열: "{ user(id: 1) { name } }"
      ↓ Lexer (토큰화)
토큰 스트림: ["{", "user", "(", "id", ":", "1", ")", "{", "name", "}", "}"]
      ↓ Parser (구문 분석)
AST (추상 구문 트리)
```

### 6.2 검증 (Validation)

파싱된 AST를 스키마와 대조하여 유효성을 검증한다. 다음과 같은 사항이 검사된다.

- 필드 존재 여부: 요청한 필드가 해당 타입에 존재하는가?
- 인자 타입: 전달된 인자의 타입이 스키마 정의와 일치하는가?
- 필수 인자: Non-Null 인자가 모두 제공되었는가?
- 프래그먼트 유효성: 프래그먼트가 올바른 타입에 적용되었는가?
- 변수 타입: 변수의 타입이 사용되는 위치의 기대 타입과 호환되는가?
- 디렉티브 위치: 디렉티브가 허용된 위치에서 사용되었는가?
- 순환 프래그먼트 방지: 프래그먼트가 자기 자신을 참조하는 순환이 없는가?

검증에 실패하면 쿼리는 실행되지 않고, 상세한 에러 메시지가 반환된다.

```json
{
  "errors": [
    {
      "message": "Cannot query field 'fullName' on type 'User'. Did you mean 'name'?",
      "locations": [{ "line": 3, "column": 5 }]
    }
  ]
}
```

### 6.3 실행 (Execution)

검증을 통과한 쿼리를 실제로 실행한다. AST를 DFS(깊이 우선 탐색)로 순회하면서 각 필드의 리졸버를 호출한다.

```
실행 흐름 예시: query { user(id: "1") { name posts { title } } }

1. Query.user 리졸버 호출 (args: { id: "1" })
   ├── 2. User.name 리졸버 호출 (기본 리졸버: parent.name)
   └── 3. User.posts 리졸버 호출
       ├── 4. Post.title 리졸버 호출 (각 post에 대해)
       └── ...
```

실행 결과는 쿼리와 동일한 형태(shape)의 JSON으로 반환된다.

```json
{
  "data": {
    "user": {
      "name": "홍길동",
      "posts": [
        { "title": "GraphQL 기초" },
        { "title": "타입 시스템 이해하기" }
      ]
    }
  }
}
```

### 에러 처리

GraphQL은 부분 실행(Partial Execution)을 지원한다. 일부 필드에서 에러가 발생하더라도 나머지 필드는 정상적으로 반환될 수 있다.

```json
{
  "data": {
    "user": {
      "name": "홍길동",
      "posts": null
    }
  },
  "errors": [
    {
      "message": "게시글을 불러오는 중 오류가 발생했습니다.",
      "locations": [{ "line": 4, "column": 5 }],
      "path": ["user", "posts"]
    }
  ]
}
```

---

## 7. N+1 문제와 DataLoader

### N+1 문제란?

GraphQL의 필드별 리졸버 실행 방식은 자연스럽게 N+1 문제를 유발한다. 예를 들어 사용자 목록을 조회하면서 각 사용자의 게시글을 가져오는 경우를 보자.

```graphql
query {
  users {       # 1번의 쿼리: SELECT * FROM users
    name
    posts {     # N번의 쿼리: 각 사용자마다 SELECT * FROM posts WHERE author_id = ?
      title
    }
  }
}
```

사용자가 100명이라면, 1(사용자 목록) + 100(각 사용자의 게시글) = 총 101번의 데이터베이스 쿼리가 발생한다.

### DataLoader를 이용한 해결

Facebook이 개발한 DataLoader는 이 문제를 배칭(Batching) 과 캐싱(Caching) 으로 해결한다.

#### 동작 원리

1. 요청 수집: 같은 이벤트 루프 틱(tick) 내에 발생하는 개별 load 요청을 모아둔다.
2. 배치 실행: 수집된 key들을 하나의 배치 함수에 전달하여 한 번에 처리한다.
3. 결과 분배: 배치 결과를 각 요청자에게 분배한다.
4. 캐싱: 같은 요청 내에서 동일한 key는 캐시에서 반환한다.

```javascript
const DataLoader = require('dataloader');

// 배치 함수 정의: key 배열을 받아 한 번에 조회
const postsByAuthorLoader = new DataLoader(async (authorIds) => {
  // 1번의 쿼리: SELECT * FROM posts WHERE author_id IN (1, 2, 3, ...)
  const posts = await db.query(
    'SELECT * FROM posts WHERE author_id IN (?)',
    [authorIds]
  );

  // key 순서에 맞게 결과 매핑
  const postsByAuthor = {};
  posts.forEach(post => {
    if (!postsByAuthor[post.author_id]) {
      postsByAuthor[post.author_id] = [];
    }
    postsByAuthor[post.author_id].push(post);
  });

  return authorIds.map(id => postsByAuthor[id] || []);
});

// 리졸버에서 사용
const resolvers = {
  User: {
    posts: (parent, args, context) => {
      // 개별 호출처럼 보이지만, 내부적으로 배치 처리됨
      return context.loaders.postsByAuthor.load(parent.id);
    },
  },
};
```

결과: 101번의 쿼리가 2번 (사용자 목록 1번 + 게시글 배치 1번)으로 줄어든다.

#### DataLoader 사용 시 주의사항

- DataLoader 인스턴스는 요청(request)마다 새로 생성해야 한다. 요청 간 캐시가 공유되면 데이터 누출이 발생할 수 있다.
- 배치 함수는 반드시 입력 key 배열과 동일한 길이와 순서의 결과 배열을 반환해야 한다.
- 결과가 없는 key에는 `null` 또는 빈 배열을 반환한다.

```javascript
// Express 미들웨어에서 요청별 DataLoader 생성
app.use('/graphql', (req, res) => {
  const context = {
    loaders: {
      postsByAuthor: new DataLoader(batchPostsByAuthor),
      userById: new DataLoader(batchUsersById),
    },
    currentUser: req.user,
  };
  // ...
});
```

---

## 8. 보안 고려사항

GraphQL의 유연함은 동시에 보안 취약점이 될 수 있다. 클라이언트가 임의의 쿼리를 작성할 수 있기 때문에, 적절한 방어책이 필요하다.

### 8.1 쿼리 깊이 제한 (Query Depth Limiting)

중첩이 깊은 쿼리는 서버에 과도한 부하를 줄 수 있다.

```graphql
# 악의적인 깊은 중첩 쿼리
query Malicious {
  user(id: "1") {
    posts {
      author {
        posts {
          author {
            posts {
              author {
                # ... 무한 중첩 가능
              }
            }
          }
        }
      }
    }
  }
}
```

대응: 허용 최대 깊이를 설정한다 (예: 최대 10레벨).

```javascript
// graphql-depth-limit 라이브러리 사용 예시
const depthLimit = require('graphql-depth-limit');

const server = new ApolloServer({
  typeDefs,
  resolvers,
  validationRules: [depthLimit(10)],
});
```

### 8.2 쿼리 복잡도 분석 (Query Complexity Analysis)

각 필드에 비용(cost)을 할당하고, 쿼리의 전체 비용이 임계값을 초과하면 거부한다.

```graphql
type Query {
  # cost: 1
  user(id: ID!): User

  # cost: 10 (목록 조회는 비용이 높음)
  users(limit: Int = 10): [User!]!
}

type User {
  # cost: 1
  name: String!

  # cost: limit * 5 (중첩 목록)
  posts(limit: Int = 10): [Post!]!
}
```

```javascript
// graphql-query-complexity 라이브러리 사용
const { createComplexityRule, simpleEstimator } = require('graphql-query-complexity');

const complexityRule = createComplexityRule({
  maximumComplexity: 1000,
  estimators: [
    simpleEstimator({ defaultComplexity: 1 }),
  ],
  onComplete: (complexity) => {
    console.log('Query complexity:', complexity);
  },
});
```

### 8.3 속도 제한 (Rate Limiting)

단위 시간당 허용하는 쿼리 수 또는 총 비용을 제한한다.

### 8.4 인트로스펙션 비활성화

프로덕션 환경에서는 인트로스펙션을 비활성화하여 스키마 구조 노출을 방지한다.

```javascript
const server = new ApolloServer({
  typeDefs,
  resolvers,
  introspection: process.env.NODE_ENV !== 'production',
});
```

### 8.5 Persisted Queries (영속 쿼리)

클라이언트가 임의의 쿼리를 보내는 대신, 서버에 미리 등록된 쿼리의 해시값만 전달하도록 한다. 이를 통해 허용되지 않은 쿼리를 원천 차단할 수 있다.

```json
// 클라이언트 요청
{
  "extensions": {
    "persistedQuery": {
      "version": 1,
      "sha256Hash": "ecf4edb46db40b5132295c0291d62fb65d6759a9eedfa4d5d612dd5ec54a6b38"
    }
  },
  "variables": { "id": "1" }
}
```

### 8.6 타임아웃 설정

장시간 실행되는 쿼리를 방지하기 위해 리졸버 또는 요청 수준에서 타임아웃을 설정한다.

### 8.7 인증(Authentication) 및 인가(Authorization)

GraphQL 자체는 인증/인가 메커니즘을 포함하지 않으므로 별도로 구현해야 한다. 일반적인 패턴은 다음과 같다.

```javascript
// context에 인증 정보 전달
const server = new ApolloServer({
  context: ({ req }) => {
    const token = req.headers.authorization || '';
    const user = verifyToken(token);
    return { user };
  },
});

// 리졸버에서 인가 처리
const resolvers = {
  Query: {
    adminDashboard: (parent, args, context) => {
      if (!context.user || context.user.role !== 'ADMIN') {
        throw new ForbiddenError('관리자 권한이 필요합니다.');
      }
      return getAdminDashboard();
    },
  },
};
```

---

## 9. 주요 구현체 및 도구

### 서버 라이브러리

| 이름 | 언어 | 특징 |
|---|---|---|
| graphql-js | JavaScript | Facebook 공식 참조 구현체. 다른 많은 도구의 기반 |
| Apollo Server | JavaScript/TypeScript | 가장 널리 사용되는 GraphQL 서버. 미들웨어, 플러그인 생태계 풍부 |
| graphql-yoga | JavaScript/TypeScript | The Guild에서 개발. 간결한 설정, Envelop 플러그인 시스템 |
| Mercurius | JavaScript | Fastify 기반의 고성능 GraphQL 서버 |
| graphql-java | Java | JVM 생태계의 대표 GraphQL 구현체 |
| graphene | Python | Python의 대표 GraphQL 라이브러리 |
| Strawberry | Python | Python 타입 힌트 기반의 현대적 GraphQL 라이브러리 |
| gqlgen | Go | 코드 생성 기반의 Go GraphQL 서버 |
| Juniper | Rust | Rust의 대표 GraphQL 라이브러리 |
| Sangria | Scala | Scala의 GraphQL 구현체 |
| Hot Chocolate | C# | .NET 생태계의 GraphQL 서버 |

### 클라이언트 라이브러리

| 이름 | 특징 |
|---|---|
| Apollo Client | 가장 인기 있는 GraphQL 클라이언트. React, Vue, Angular, iOS, Android 지원. 정규화된 캐시, 낙관적 업데이트 |
| Relay | Facebook 공식 클라이언트. React 전용. 컴파일러 기반 최적화, 프래그먼트 기반 데이터 요구사항 선언 |
| urql | 가벼운 GraphQL 클라이언트. 확장 가능한 교환(exchange) 시스템 |
| graphql-request | 최소한의 GraphQL 클라이언트. 단순한 요청에 적합 |
| URQL | Formidable에서 개발한 경량 클라이언트 |

### 개발 도구

| 이름 | 역할 |
|---|---|
| GraphiQL | 브라우저 기반 GraphQL IDE. 인트로스펙션을 활용한 자동 완성과 문서 탐색 |
| Apollo Studio (Explorer) | 클라우드 기반 GraphQL IDE. 스키마 레지스트리, 성능 모니터링 |
| Altair GraphQL Client | 데스크톱/브라우저 GraphQL 클라이언트. 파일 업로드, 환경 변수 지원 |
| GraphQL Voyager | 스키마를 인터랙티브 그래프로 시각화 |
| GraphQL Code Generator | 스키마로부터 TypeScript 타입, React Hooks 등 자동 생성 |
| Prisma | 데이터베이스 ORM으로, GraphQL과 함께 자주 사용 |

### 스키마 관리 도구

| 이름 | 역할 |
|---|---|
| Apollo Federation | 마이크로서비스 환경에서 여러 GraphQL 서비스를 하나의 통합 스키마로 결합 |
| Schema Stitching | 여러 GraphQL 스키마를 하나로 합치는 방법 |
| GraphQL Mesh | REST, gRPC, SOAP 등 다양한 데이터 소스를 GraphQL로 통합 |

---

## 10. 장점과 단점

### 장점

1. Over-fetching / Under-fetching 해결: 클라이언트가 필요한 데이터만 정확히 요청하여 불필요한 데이터 전송을 제거한다. 한 번의 쿼리로 연관된 데이터를 모두 가져와 다중 API 호출을 방지한다.

2. 강력한 타입 시스템: 스키마가 곧 문서이자 계약이다. 컴파일 타임에 쿼리의 유효성을 검증할 수 있어 런타임 에러를 줄인다.

3. 개발자 경험(DX): GraphiQL, Apollo Studio 등의 도구를 통해 자동 완성, 실시간 문서, 쿼리 테스트가 가능하다. 프론트엔드와 백엔드가 스키마를 기반으로 독립적으로 개발할 수 있다.

4. 버전 없는 API 진화: 새 필드를 추가해도 기존 클라이언트에 영향이 없다. `@deprecated` 디렉티브로 점진적 마이그레이션이 가능하다.

5. 프론트엔드 자율성: 백엔드 변경 없이 프론트엔드가 필요한 데이터를 자유롭게 조합할 수 있다. 각 화면에 최적화된 쿼리를 작성할 수 있다.

6. 에코시스템: Apollo, Relay 등 성숙한 클라이언트 라이브러리가 캐싱, 상태 관리, 낙관적 업데이트 등을 기본 지원한다.

7. 다중 데이터 소스 통합: 하나의 GraphQL 레이어 뒤에 DB, REST API, 마이크로서비스 등 다양한 소스를 통합할 수 있다.

### 단점

1. 학습 곡선: SDL 문법, 리졸버 패턴, 타입 시스템, DataLoader 등 새로운 개념이 많다. REST에 익숙한 팀에게는 초기 도입 비용이 크다.

2. 캐싱 복잡성: REST는 HTTP 캐싱(ETag, Cache-Control 등)을 자연스럽게 활용할 수 있지만, GraphQL은 단일 엔드포인트에 POST를 사용하므로 HTTP 수준 캐싱이 어렵다. 별도의 캐싱 전략(Apollo Client의 정규화된 캐시, Persisted Queries 등)이 필요하다.

3. N+1 문제: 필드별 리졸버 방식이 자연스럽게 N+1 문제를 유발한다. DataLoader 같은 도구를 반드시 함께 사용해야 한다.

4. 파일 업로드: GraphQL 스펙에 파일 업로드가 포함되어 있지 않다. `graphql-upload` 같은 별도의 멀티파트 스펙 확장이나, 프리사인드 URL 방식이 필요하다.

5. 보안 취약점: 클라이언트가 임의의 쿼리를 보낼 수 있어 악의적인 깊은 중첩, 과도한 복잡도의 쿼리로 서버 자원이 고갈될 수 있다. 별도의 보안 계층이 필수이다.

6. 에러 처리의 모호함: GraphQL은 항상 HTTP 200을 반환하고, 에러를 응답 body의 `errors` 필드에 담는다. REST의 HTTP 상태 코드 기반 에러 처리에 비해 직관적이지 않을 수 있다.

7. 단순한 API에는 과도한 설계: CRUD 위주의 단순한 API라면 REST가 더 적합할 수 있다. GraphQL의 스키마 정의, 리졸버 구현, 보안 설정 등이 오버헤드가 될 수 있다.

8. 속도 제한의 어려움: REST에서는 엔드포인트별로 속도를 제한하면 되지만, GraphQL은 단일 엔드포인트이므로 쿼리 복잡도 기반의 제한이 필요하다.

---

## 11. 실제 예시

### 11.1 완전한 스키마 정의

```graphql
# ----- 커스텀 스칼라 -----
scalar DateTime

# ----- Enum -----
enum Role {
  ADMIN
  EDITOR
  VIEWER
}

enum PostStatus {
  DRAFT
  PUBLISHED
  ARCHIVED
}

enum SortOrder {
  ASC
  DESC
}

# ----- Interface -----
interface Node {
  id: ID!
}

interface Timestamped {
  createdAt: DateTime!
  updatedAt: DateTime!
}

# ----- Object 타입 -----
type User implements Node & Timestamped {
  id: ID!
  name: String!
  email: String!
  bio: String
  role: Role!
  avatar: String
  posts(limit: Int = 10, offset: Int = 0): [Post!]!
  followers: [User!]!
  followingCount: Int!
  createdAt: DateTime!
  updatedAt: DateTime!
}

type Post implements Node & Timestamped {
  id: ID!
  title: String!
  content: String!
  status: PostStatus!
  author: User!
  tags: [Tag!]!
  comments(limit: Int = 20): [Comment!]!
  viewCount: Int!
  likeCount: Int!
  isLikedByMe: Boolean!
  createdAt: DateTime!
  updatedAt: DateTime!
}

type Comment implements Node & Timestamped {
  id: ID!
  body: String!
  author: User!
  post: Post!
  createdAt: DateTime!
  updatedAt: DateTime!
}

type Tag {
  id: ID!
  name: String!
  posts: [Post!]!
}

# ----- Union -----
union SearchResult = User | Post | Comment

# ----- Input 타입 -----
input CreatePostInput {
  title: String!
  content: String!
  tags: [String!]
  status: PostStatus = DRAFT
}

input UpdatePostInput {
  title: String
  content: String
  tags: [String!]
  status: PostStatus
}

input PostFilterInput {
  status: PostStatus
  authorId: ID
  tag: String
  search: String
}

input PaginationInput {
  limit: Int = 10
  offset: Int = 0
  sortBy: String = "createdAt"
  order: SortOrder = DESC
}

# ----- 페이지네이션 (Relay 스타일 커서 기반) -----
type PageInfo {
  hasNextPage: Boolean!
  hasPreviousPage: Boolean!
  startCursor: String
  endCursor: String
}

type PostEdge {
  node: Post!
  cursor: String!
}

type PostConnection {
  edges: [PostEdge!]!
  pageInfo: PageInfo!
  totalCount: Int!
}

# ----- 루트 타입 -----
type Query {
  # 단일 조회
  node(id: ID!): Node
  user(id: ID!): User
  post(id: ID!): Post

  # 목록 조회
  users(limit: Int = 10, offset: Int = 0): [User!]!
  posts(filter: PostFilterInput, pagination: PaginationInput): PostConnection!

  # 검색
  search(term: String!, limit: Int = 10): [SearchResult!]!

  # 현재 사용자
  me: User
}

type Mutation {
  # 인증
  signUp(name: String!, email: String!, password: String!): AuthPayload!
  signIn(email: String!, password: String!): AuthPayload!

  # 게시글
  createPost(input: CreatePostInput!): Post!
  updatePost(id: ID!, input: UpdatePostInput!): Post!
  deletePost(id: ID!): Boolean!
  publishPost(id: ID!): Post!

  # 댓글
  addComment(postId: ID!, body: String!): Comment!
  deleteComment(id: ID!): Boolean!

  # 좋아요
  likePost(postId: ID!): Post!
  unlikePost(postId: ID!): Post!

  # 팔로우
  followUser(userId: ID!): User!
  unfollowUser(userId: ID!): User!
}

type Subscription {
  postPublished: Post!
  commentAdded(postId: ID!): Comment!
  userFollowed(userId: ID!): User!
}

type AuthPayload {
  token: String!
  user: User!
}
```

### 11.2 쿼리 예시

#### 기본 쿼리: 사용자와 게시글 조회

```graphql
query GetUserProfile($userId: ID!) {
  user(id: $userId) {
    name
    email
    bio
    role
    posts(limit: 5) {
      title
      status
      createdAt
      likeCount
      tags {
        name
      }
    }
    followers {
      name
      avatar
    }
    followingCount
  }
}
```

변수:

```json
{
  "userId": "user-123"
}
```

응답:

```json
{
  "data": {
    "user": {
      "name": "김개발",
      "email": "kim@example.com",
      "bio": "풀스택 개발자",
      "role": "EDITOR",
      "posts": [
        {
          "title": "GraphQL 완벽 가이드",
          "status": "PUBLISHED",
          "createdAt": "2025-01-15T09:00:00Z",
          "likeCount": 42,
          "tags": [
            { "name": "GraphQL" },
            { "name": "API" }
          ]
        }
      ],
      "followers": [
        { "name": "이코딩", "avatar": "https://example.com/avatars/lee.png" }
      ],
      "followingCount": 15
    }
  }
}
```

#### 프래그먼트 활용 쿼리

```graphql
fragment UserCard on User {
  id
  name
  avatar
  role
}

fragment PostPreview on Post {
  id
  title
  status
  createdAt
  likeCount
  author {
    ...UserCard
  }
}

query Dashboard($showDrafts: Boolean!) {
  me {
    ...UserCard
    email
  }
  posts(
    filter: { status: PUBLISHED }
    pagination: { limit: 10, sortBy: "createdAt", order: DESC }
  ) {
    edges {
      node {
        ...PostPreview
        comments(limit: 3) {
          body
          author {
            ...UserCard
          }
        }
      }
      cursor
    }
    pageInfo {
      hasNextPage
      endCursor
    }
    totalCount
  }
  drafts: posts(filter: { status: DRAFT }) @include(if: $showDrafts) {
    edges {
      node {
        ...PostPreview
      }
    }
    totalCount
  }
}
```

#### 뮤테이션 예시

```graphql
mutation CreateAndPublishPost($input: CreatePostInput!) {
  createPost(input: $input) {
    id
    title
    content
    status
    author {
      name
    }
    createdAt
  }
}
```

변수:

```json
{
  "input": {
    "title": "GraphQL Subscription 활용하기",
    "content": "이 글에서는 실시간 기능을 구현하는 방법을 알아봅니다...",
    "tags": ["GraphQL", "실시간", "WebSocket"],
    "status": "PUBLISHED"
  }
}
```

#### 서브스크립션 예시

```graphql
subscription WatchNewComments($postId: ID!) {
  commentAdded(postId: $postId) {
    id
    body
    author {
      name
      avatar
    }
    createdAt
  }
}
```

#### 검색 쿼리 (Union 타입 활용)

```graphql
query GlobalSearch($term: String!) {
  search(term: $term, limit: 20) {
    __typename
    ... on User {
      id
      name
      email
      avatar
    }
    ... on Post {
      id
      title
      content
      author {
        name
      }
    }
    ... on Comment {
      id
      body
      post {
        title
      }
    }
  }
}
```

#### 인트로스펙션 쿼리

```graphql
query FullIntrospection {
  __schema {
    queryType { name }
    mutationType { name }
    subscriptionType { name }
    types {
      name
      kind
      description
      fields(includeDeprecated: true) {
        name
        description
        type {
          name
          kind
          ofType {
            name
            kind
          }
        }
        isDeprecated
        deprecationReason
      }
    }
    directives {
      name
      description
      locations
      args {
        name
        type {
          name
          kind
        }
      }
    }
  }
}
```

---

## 참고 자료

- [GraphQL 공식 스펙](https://spec.graphql.org/)
- [GraphQL 공식 사이트](https://graphql.org/)
- [GraphQL Foundation](https://graphql.org/foundation/)
- [Apollo GraphQL 문서](https://www.apollographql.com/docs/)
- [Relay 문서](https://relay.dev/)
- [graphql-js GitHub](https://github.com/graphql/graphql-js)
- [DataLoader GitHub](https://github.com/graphql/dataloader)
