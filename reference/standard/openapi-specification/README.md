# OpenAPI Specification (OAS) 상세 정리

## 목차

1. [개요](#1-개요)
2. [역사](#2-역사)
3. [핵심 개념 및 구조](#3-핵심-개념-및-구조)
4. [버전별 차이](#4-버전별-차이)
5. [주요 키워드 및 필드 설명](#5-주요-키워드-및-필드-설명)
6. [사용 사례 및 활용 도구](#6-사용-사례-및-활용-도구)
7. [장점과 단점](#7-장점과-단점)
8. [실제 예시](#8-실제-예시)
9. [관련 생태계 및 커뮤니티](#9-관련-생태계-및-커뮤니티)

---

## 1. 개요

### OpenAPI Specification이란

OpenAPI Specification(OAS)은 RESTful API를 기술하기 위한 표준 인터페이스 정의 언어이다. 프로그래밍 언어에 종속되지 않으며, 사람과 기계 모두가 이해할 수 있는 형식으로 HTTP API의 기능을 정의한다. JSON 또는 YAML 형식으로 작성되며, API의 엔드포인트, 요청/응답 형식, 인증 방식, 파라미터 등 API의 모든 측면을 선언적으로 기술할 수 있다.

### 왜 만들어졌는가

API 생태계가 급격히 성장하면서 다음과 같은 문제들이 대두되었다.

- 문서화의 일관성 부재: 각 조직마다 API를 기술하는 방식이 달라, API 소비자가 매번 새로운 형식에 적응해야 했다.
- 클라이언트-서버 간 계약(Contract)의 모호함: 구두나 비정형 문서로 API 명세를 공유하면 해석 차이로 인한 통합 오류가 빈번했다.
- 자동화의 어려움: 표준화된 명세가 없으면 코드 생성, 테스트 자동화, 모킹(mocking) 등을 체계적으로 수행하기 어려웠다.
- API 수명주기 관리의 비효율: 설계, 개발, 테스트, 배포, 운영 전 과정에서 통일된 기준이 필요했다.

OAS는 이러한 문제를 해결하기 위해 API의 단일 진실 공급원(Single Source of Truth) 역할을 수행하도록 설계되었다. 하나의 명세 파일로부터 문서 생성, 코드 스캐폴딩, 테스트, 검증 등 다양한 작업을 자동화할 수 있다.

### 핵심 철학

| 원칙 | 설명 |
|------|------|
| 언어 중립성 | 특정 프로그래밍 언어나 프레임워크에 종속되지 않는다 |
| 사람과 기계 모두를 위한 설계 | 개발자가 읽을 수 있으면서도 도구가 파싱할 수 있는 형식이다 |
| 선언적 기술 | API가 "무엇을" 하는지를 기술하며, "어떻게" 구현하는지는 다루지 않는다 |
| API-First 설계 지원 | 코드 작성 전에 API 명세를 먼저 정의하는 워크플로를 지원한다 |

---

## 2. 역사

### Swagger의 탄생 (2010-2014)

- 2010년: Wordnik의 엔지니어 Tony Tam이 자사 API를 문서화하기 위해 Swagger라는 프로젝트를 시작했다. 초기에는 단순한 API 문서화 도구였으나, API 명세 형식 자체가 핵심 가치로 부각되었다.
- 2011년: Swagger 1.0이 공개되었다. JSON 기반으로 API를 기술하는 최초의 체계적인 시도였다.
- 2012년: Swagger 1.1, 1.2가 순차적으로 릴리스되며 커뮤니티가 성장하기 시작했다.
- 2014년 9월: Swagger 2.0이 발표되었다. 명세 구조가 대폭 개선되어 단일 파일로 API 전체를 기술할 수 있게 되었으며, 업계에서 사실상의 표준(de facto standard)으로 자리 잡았다.

### SmartBear 인수와 OpenAPI Initiative (2015-2016)

- 2015년 3월: API 개발 도구 회사인 SmartBear Software가 오픈 소스 Swagger 프로젝트를 인수했다.
- 2015년 11월: SmartBear는 Linux Foundation 산하에 OpenAPI Initiative (OAI) 를 설립하고, Swagger Specification을 기부했다. 구글, IBM, Microsoft, PayPal, Apigee 등이 창립 멤버로 참여했다.
- 2016년 1월: Swagger Specification은 공식적으로 OpenAPI Specification으로 이름이 변경되었다. 이때부터 "Swagger"는 SmartBear가 제공하는 도구 브랜드명으로만 사용되고, 명세 자체는 "OpenAPI"로 불리게 되었다.

### OpenAPI 3.0 및 이후 (2017-현재)

- 2017년 7월: OpenAPI 3.0.0이 발표되었다. 2.0 대비 구조가 대폭 개편되어 `components` 객체 도입, `requestBody` 분리, 다중 서버 지원, 콜백(callback) 등 현대적 API 패턴을 지원하게 되었다.
- 2021년 2월: OpenAPI 3.1.0이 발표되었다. JSON Schema Draft 2020-12와 완전히 호환되도록 변경되어, 기존의 커스텀 스키마 방언(dialect) 문제를 해결했다. Webhooks 지원도 추가되었다.
- 2024년 이후: OpenAPI 3.1.1 패치와 함께, 차기 메이저 버전에 대한 논의가 진행 중이다.

### 타임라인 요약

```
2010  Swagger 프로젝트 시작 (Tony Tam / Wordnik)
2011  Swagger 1.0 공개
2014  Swagger 2.0 발표
2015  SmartBear 인수 -> OpenAPI Initiative 설립 (Linux Foundation)
2016  명칭 변경: Swagger Specification -> OpenAPI Specification
2017  OpenAPI 3.0.0 발표
2021  OpenAPI 3.1.0 발표 (JSON Schema 완전 호환)
```

---

## 3. 핵심 개념 및 구조

OpenAPI 문서는 계층적 구조를 가진다. 최상위에는 메타 정보가 위치하고, 그 아래로 API의 구체적인 정의가 펼쳐진다. 아래에서는 OpenAPI 3.1 기준으로 각 구성 요소를 설명한다.

### 3.1 최상위 구조

```yaml
openapi: "3.1.0"          # 필수. OAS 버전
info: { ... }              # 필수. API 메타데이터
servers: [ ... ]           # API 서버 목록
paths: { ... }             # API 엔드포인트 정의
webhooks: { ... }          # Webhook 정의 (3.1+)
components: { ... }        # 재사용 가능한 컴포넌트
security: [ ... ]          # 전역 보안 요구사항
tags: [ ... ]              # 엔드포인트 그룹화용 태그
externalDocs: { ... }      # 외부 문서 링크
```

### 3.2 Info Object

API에 대한 기본 메타데이터를 정의한다.

```yaml
info:
  title: "사용자 관리 API"           # 필수. API 이름
  version: "1.0.0"                   # 필수. API 버전
  description: "사용자 CRUD 기능을 제공하는 API"
  termsOfService: "https://example.com/terms"
  contact:
    name: "API 지원팀"
    url: "https://example.com/support"
    email: "api@example.com"
  license:
    name: "Apache 2.0"
    url: "https://www.apache.org/licenses/LICENSE-2.0.html"
  summary: "사용자 관리를 위한 RESTful API"  # 3.1에서 추가
```

### 3.3 Servers

API가 호스팅되는 서버 정보를 배열로 정의한다. 2.0에서는 `host`, `basePath`, `schemes`로 나뉘어 있던 것이 3.0부터 `servers`로 통합되었다.

```yaml
servers:
  - url: "https://api.example.com/v1"
    description: "운영 서버"
  - url: "https://staging-api.example.com/v1"
    description: "스테이징 서버"
  - url: "http://localhost:8080/v1"
    description: "로컬 개발 서버"
```

서버 URL에 변수를 사용할 수도 있다.

```yaml
servers:
  - url: "https://{environment}.example.com/{version}"
    variables:
      environment:
        default: "api"
        enum: ["api", "staging", "sandbox"]
        description: "서버 환경"
      version:
        default: "v1"
```

### 3.4 Paths

API의 개별 엔드포인트(경로)와 해당 경로에서 지원하는 HTTP 메서드를 정의한다. OAS의 가장 핵심적인 부분이다.

```yaml
paths:
  /users:
    get:
      summary: "전체 사용자 목록 조회"
      operationId: "listUsers"
      tags:
        - users
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 1
        - name: limit
          in: query
          schema:
            type: integer
            default: 20
            maximum: 100
      responses:
        '200':
          description: "성공"
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/User'
        '401':
          $ref: '#/components/responses/Unauthorized'
    post:
      summary: "새 사용자 생성"
      operationId: "createUser"
      tags:
        - users
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateUserRequest'
      responses:
        '201':
          description: "사용자 생성 완료"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/User'

  /users/{userId}:
    get:
      summary: "특정 사용자 조회"
      operationId: "getUser"
      tags:
        - users
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: "성공"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/User'
        '404':
          $ref: '#/components/responses/NotFound'
```

Path Item Object에서 사용 가능한 HTTP 메서드는 다음과 같다.

| 메서드 | 설명 |
|--------|------|
| `get` | 리소스 조회 |
| `post` | 리소스 생성 |
| `put` | 리소스 전체 교체 |
| `patch` | 리소스 부분 수정 |
| `delete` | 리소스 삭제 |
| `options` | 통신 옵션 확인 |
| `head` | GET과 동일하되 본문 없이 헤더만 반환 |
| `trace` | 루프백 테스트 |

### 3.5 Components

재사용 가능한 객체들을 정의하는 컨테이너이다. `$ref`를 통해 문서 내 어디서든 참조할 수 있다. 코드의 함수나 클래스처럼, 반복되는 정의를 한 곳에 모아 관리함으로써 명세의 일관성과 유지보수성을 높인다.

```yaml
components:
  schemas: { ... }          # 데이터 모델 (JSON Schema)
  responses: { ... }        # 재사용 가능한 응답 정의
  parameters: { ... }       # 재사용 가능한 파라미터
  examples: { ... }         # 예시 값
  requestBodies: { ... }    # 재사용 가능한 요청 본문
  headers: { ... }          # 재사용 가능한 헤더
  securitySchemes: { ... }  # 보안 스킴 정의
  links: { ... }            # 응답 간 관계 정의
  callbacks: { ... }        # 콜백 정의
  pathItems: { ... }        # 재사용 가능한 경로 항목 (3.1+)
```

### 3.6 Schemas

데이터 모델을 JSON Schema 형식으로 정의한다. 요청/응답의 본문 구조, 파라미터 타입 등을 기술하는 데 사용된다.

```yaml
components:
  schemas:
    User:
      type: object
      required:
        - id
        - email
        - name
      properties:
        id:
          type: string
          format: uuid
          description: "사용자 고유 식별자"
          readOnly: true
        email:
          type: string
          format: email
          description: "이메일 주소"
        name:
          type: string
          minLength: 1
          maxLength: 100
          description: "사용자 이름"
        role:
          type: string
          enum:
            - admin
            - user
            - guest
          default: user
        createdAt:
          type: string
          format: date-time
          readOnly: true

    CreateUserRequest:
      type: object
      required:
        - email
        - name
      properties:
        email:
          type: string
          format: email
        name:
          type: string
          minLength: 1
          maxLength: 100
        role:
          type: string
          enum: [admin, user, guest]
          default: user

    Error:
      type: object
      required:
        - code
        - message
      properties:
        code:
          type: integer
        message:
          type: string
        details:
          type: string
```

스키마 조합(Schema Composition) 키워드를 사용하면 복잡한 데이터 모델을 표현할 수 있다.

```yaml
# allOf: 모든 스키마를 동시에 만족해야 함 (상속/확장에 활용)
AdminUser:
  allOf:
    - $ref: '#/components/schemas/User'
    - type: object
      properties:
        permissions:
          type: array
          items:
            type: string

# oneOf: 정확히 하나의 스키마만 만족해야 함 (배타적 선택)
Pet:
  oneOf:
    - $ref: '#/components/schemas/Cat'
    - $ref: '#/components/schemas/Dog'
  discriminator:
    propertyName: petType

# anyOf: 하나 이상의 스키마를 만족하면 됨
SearchResult:
  anyOf:
    - $ref: '#/components/schemas/User'
    - $ref: '#/components/schemas/Post'
```

### 3.7 Parameters

API 요청에 포함될 수 있는 파라미터를 정의한다. 위치(`in`)에 따라 네 가지로 분류된다.

| 위치 | 설명 | 예시 |
|------|------|------|
| `path` | URL 경로의 일부 | `/users/{userId}` |
| `query` | URL 쿼리 문자열 | `?page=1&limit=20` |
| `header` | HTTP 요청 헤더 | `X-Request-ID: abc123` |
| `cookie` | HTTP 쿠키 | `session_id=xyz` |

```yaml
components:
  parameters:
    PageParam:
      name: page
      in: query
      description: "페이지 번호"
      required: false
      schema:
        type: integer
        minimum: 1
        default: 1
    LimitParam:
      name: limit
      in: query
      description: "페이지당 항목 수"
      required: false
      schema:
        type: integer
        minimum: 1
        maximum: 100
        default: 20
    UserIdParam:
      name: userId
      in: path
      description: "사용자 ID"
      required: true
      schema:
        type: string
        format: uuid
```

### 3.8 Responses

API 응답을 정의한다. HTTP 상태 코드별로 구분하며, 응답 본문, 헤더, 링크 등을 포함할 수 있다.

```yaml
components:
  responses:
    NotFound:
      description: "요청한 리소스를 찾을 수 없습니다"
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
          example:
            code: 404
            message: "리소스를 찾을 수 없습니다"
    Unauthorized:
      description: "인증이 필요합니다"
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
          example:
            code: 401
            message: "유효한 인증 토큰이 필요합니다"
    BadRequest:
      description: "잘못된 요청입니다"
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    InternalServerError:
      description: "서버 내부 오류가 발생했습니다"
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
```

### 3.9 Request Body

3.0부터 파라미터에서 분리되어 독립적인 객체가 되었다. `content` 필드를 통해 다양한 미디어 타입별로 스키마를 정의할 수 있다.

```yaml
requestBody:
  description: "사용자 생성 요청"
  required: true
  content:
    application/json:
      schema:
        $ref: '#/components/schemas/CreateUserRequest'
      example:
        email: "user@example.com"
        name: "홍길동"
        role: "user"
    multipart/form-data:
      schema:
        type: object
        properties:
          name:
            type: string
          avatar:
            type: string
            format: binary
```

### 3.10 Security

API의 인증/인가 방식을 정의한다. `securitySchemes`에서 스킴을 정의하고, `security`에서 적용한다.

지원하는 보안 스킴 유형:

| 유형 | 설명 |
|------|------|
| `apiKey` | API 키 (헤더, 쿼리, 쿠키) |
| `http` | HTTP 인증 (Basic, Bearer 등) |
| `oauth2` | OAuth 2.0 플로우 |
| `openIdConnect` | OpenID Connect Discovery |
| `mutualTLS` | 상호 TLS 인증 (3.1+) |

```yaml
components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: "JWT Bearer 토큰 인증"
    ApiKeyAuth:
      type: apiKey
      in: header
      name: X-API-Key
      description: "API 키 인증"
    OAuth2:
      type: oauth2
      flows:
        authorizationCode:
          authorizationUrl: "https://auth.example.com/authorize"
          tokenUrl: "https://auth.example.com/token"
          refreshUrl: "https://auth.example.com/refresh"
          scopes:
            users:read: "사용자 정보 읽기"
            users:write: "사용자 정보 쓰기"
            admin: "관리자 권한"

# 전역 보안 적용 (모든 엔드포인트에 기본 적용)
security:
  - BearerAuth: []
  - ApiKeyAuth: []
```

특정 엔드포인트에서 전역 보안을 오버라이드할 수 있다.

```yaml
paths:
  /public/health:
    get:
      summary: "헬스 체크"
      security: []          # 보안 비활성화
      responses:
        '200':
          description: "서버 정상"
  /admin/settings:
    put:
      summary: "관리자 설정 변경"
      security:
        - OAuth2: [admin]   # admin 스코프 필요
```

### 3.11 Tags

엔드포인트를 논리적으로 그룹화한다. 문서 생성 시 섹션 구분에 사용된다.

```yaml
tags:
  - name: users
    description: "사용자 관리"
    externalDocs:
      description: "사용자 관리 상세 문서"
      url: "https://docs.example.com/users"
  - name: auth
    description: "인증 관련"
  - name: admin
    description: "관리자 기능"
```

### 3.12 Links (3.0+)

응답 값을 사용하여 후속 API 호출 관계를 표현한다. HATEOAS 개념과 유사하다.

```yaml
paths:
  /users:
    post:
      operationId: createUser
      responses:
        '201':
          description: "사용자 생성 완료"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/User'
          links:
            GetUser:
              operationId: getUser
              parameters:
                userId: '$response.body#/id'
              description: "생성된 사용자 조회 링크"
```

### 3.13 Callbacks (3.0+)

API 서버가 클라이언트에게 비동기적으로 요청을 보내는 Webhook 패턴을 표현한다.

```yaml
paths:
  /webhooks/subscribe:
    post:
      summary: "웹훅 구독"
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                callbackUrl:
                  type: string
                  format: uri
      callbacks:
        onUserCreated:
          '{$request.body#/callbackUrl}':
            post:
              summary: "사용자 생성 이벤트 수신"
              requestBody:
                content:
                  application/json:
                    schema:
                      $ref: '#/components/schemas/User'
              responses:
                '200':
                  description: "콜백 수신 확인"
```

### 3.14 Discriminator

다형성(Polymorphism)을 표현할 때 사용한다. `oneOf`/`anyOf`와 함께 사용되어 어떤 스키마가 적용되는지를 특정 프로퍼티 값으로 결정한다.

```yaml
components:
  schemas:
    Pet:
      oneOf:
        - $ref: '#/components/schemas/Cat'
        - $ref: '#/components/schemas/Dog'
      discriminator:
        propertyName: petType
        mapping:
          cat: '#/components/schemas/Cat'
          dog: '#/components/schemas/Dog'
    Cat:
      type: object
      required: [petType, name]
      properties:
        petType:
          type: string
        name:
          type: string
        indoor:
          type: boolean
    Dog:
      type: object
      required: [petType, name]
      properties:
        petType:
          type: string
        name:
          type: string
        breed:
          type: string
```

---

## 4. 버전별 차이

### 4.1 Swagger 2.0 vs OpenAPI 3.0 vs OpenAPI 3.1

| 항목 | Swagger 2.0 | OpenAPI 3.0 | OpenAPI 3.1 |
|------|-------------|-------------|-------------|
| 최상위 키 | `swagger: "2.0"` | `openapi: "3.0.x"` | `openapi: "3.1.x"` |
| 서버 정의 | `host` + `basePath` + `schemes` | `servers` 배열 | `servers` 배열 |
| 요청 본문 | `parameters`의 `in: body` | 독립 `requestBody` 객체 | 독립 `requestBody` 객체 |
| 파일 업로드 | `type: file` | `format: binary` in `content` | `format: binary` in `content` / `contentMediaType` |
| 재사용 컴포넌트 | `definitions`, `parameters`, `responses` 분산 | `components` 하위 통합 | `components` 하위 통합 |
| JSON Schema 호환 | 자체 확장 서브셋 | 확장된 서브셋 (OpenAPI Schema Object) | JSON Schema Draft 2020-12 완전 호환 |
| Nullable | `x-nullable` (비공식) | `nullable: true` 별도 키워드 | `type: ["string", "null"]` (JSON Schema 방식) |
| Webhooks | 미지원 | 미지원 (Callbacks만 가능) | `webhooks` 최상위 키 지원 |
| 콜백 | 미지원 | `callbacks` 지원 | `callbacks` 지원 |
| 링크 | 미지원 | `links` 지원 | `links` 지원 |
| 예시 | `example` (단수) | `example` + `examples` (복수) | `example` + `examples` + JSON Schema `examples` |
| Content Negotiation | `produces`, `consumes` 전역 | `content` 미디어 타입 맵 (경로별) | `content` 미디어 타입 맵 (경로별) |
| 상호 TLS | 미지원 | 미지원 | `mutualTLS` 보안 스킴 |
| 식별자 | 미지원 | 미지원 | `$id`, `$anchor`, `$dynamicRef` 지원 |
| PathItems 재사용 | 미지원 | 미지원 | `components/pathItems` 지원 |

### 4.2 Swagger 2.0에서 3.0으로의 주요 변경점

구조 변경:
- `definitions` -> `components/schemas`
- `parameters` (전역) -> `components/parameters`
- `responses` (전역) -> `components/responses`
- `securityDefinitions` -> `components/securitySchemes`
- `host` + `basePath` + `schemes` -> `servers`
- `produces` / `consumes` -> `content` 미디어 타입 맵

새로운 기능:
- `requestBody`: 요청 본문이 파라미터에서 분리되어 미디어 타입별 스키마를 정의할 수 있게 되었다
- `callbacks`: 비동기 Webhook 패턴을 지원한다
- `links`: 응답 간 관계를 정의할 수 있다
- `servers` 변수: URL 템플릿 변수를 사용할 수 있다
- `oneOf`, `anyOf`: 스키마 조합 키워드가 공식 지원되었다
- Cookie 파라미터: `in: cookie`가 추가되었다

### 4.3 OpenAPI 3.0에서 3.1로의 주요 변경점

JSON Schema 완전 호환:
- 3.0에서는 JSON Schema의 확장 서브셋(superset/subset 혼합)을 사용했으나, 3.1은 JSON Schema Draft 2020-12와 100% 호환된다
- `nullable: true` 키워드가 제거되고, JSON Schema의 타입 배열 방식 `type: ["string", "null"]`을 사용한다
- `exclusiveMinimum`/`exclusiveMaximum`이 boolean에서 숫자 값으로 변경되었다

새로운 기능:
- `webhooks`: 최상위에서 Webhook을 정의할 수 있다
- `pathItems` in components: Path Item 자체를 재사용 컴포넌트로 정의할 수 있다
- `$id`, `$anchor`: JSON Schema 식별자를 사용할 수 있다
- `contentMediaType`, `contentEncoding`: 문자열 콘텐츠의 미디어 타입과 인코딩을 명시할 수 있다
- Info Object에 `summary` 필드가 추가되었다

---

## 5. 주요 키워드 및 필드 설명

### 5.1 기본 데이터 타입

OpenAPI에서 지원하는 기본 타입과 형식(format) 조합은 다음과 같다.

| type | format | 설명 |
|------|--------|------|
| `integer` | `int32` | 32비트 정수 |
| `integer` | `int64` | 64비트 정수 (long) |
| `number` | `float` | 단정밀도 부동소수점 |
| `number` | `double` | 배정밀도 부동소수점 |
| `string` | - | 일반 문자열 |
| `string` | `byte` | Base64 인코딩된 문자열 |
| `string` | `binary` | 바이너리 데이터 (파일 등) |
| `string` | `date` | RFC 3339 날짜 (`2024-01-15`) |
| `string` | `date-time` | RFC 3339 날짜시간 (`2024-01-15T09:30:00Z`) |
| `string` | `password` | UI에서 마스킹 표시 힌트 |
| `string` | `email` | 이메일 형식 (3.1 JSON Schema) |
| `string` | `uri` | URI 형식 (3.1 JSON Schema) |
| `string` | `uuid` | UUID 형식 (3.1 JSON Schema) |
| `boolean` | - | 참/거짓 |
| `array` | - | 배열 (`items` 필수) |
| `object` | - | 객체 (`properties` 정의) |
| `null` | - | null 값 (3.1에서 타입으로 사용 가능) |

### 5.2 스키마 검증 키워드

문자열 검증:

| 키워드 | 설명 |
|--------|------|
| `minLength` | 최소 문자열 길이 |
| `maxLength` | 최대 문자열 길이 |
| `pattern` | 정규표현식 패턴 |
| `enum` | 허용 값 목록 |
| `const` | 고정 값 (3.1+) |

숫자 검증:

| 키워드 | 설명 |
|--------|------|
| `minimum` | 최솟값 (이상) |
| `maximum` | 최댓값 (이하) |
| `exclusiveMinimum` | 최솟값 미포함 (3.0: boolean, 3.1: number) |
| `exclusiveMaximum` | 최댓값 미포함 (3.0: boolean, 3.1: number) |
| `multipleOf` | 배수 조건 |

배열 검증:

| 키워드 | 설명 |
|--------|------|
| `items` | 배열 요소의 스키마 |
| `minItems` | 최소 요소 수 |
| `maxItems` | 최대 요소 수 |
| `uniqueItems` | 요소 중복 불허 |
| `prefixItems` | 튜플 검증 (3.1+, JSON Schema) |

객체 검증:

| 키워드 | 설명 |
|--------|------|
| `properties` | 프로퍼티 정의 |
| `required` | 필수 프로퍼티 목록 |
| `additionalProperties` | 정의되지 않은 프로퍼티 허용 여부/스키마 |
| `minProperties` | 최소 프로퍼티 수 |
| `maxProperties` | 최대 프로퍼티 수 |
| `patternProperties` | 정규식 패턴으로 프로퍼티명 매칭 (3.1+) |

### 5.3 참조 및 조합

| 키워드 | 설명 |
|--------|------|
| `$ref` | 다른 스키마/컴포넌트 참조. 예: `$ref: '#/components/schemas/User'` |
| `allOf` | 모든 스키마를 동시에 만족 (교집합, 상속에 활용) |
| `oneOf` | 정확히 하나의 스키마만 만족 (배타적 합집합) |
| `anyOf` | 하나 이상의 스키마를 만족 (합집합) |
| `not` | 지정된 스키마를 만족하지 않아야 함 (부정) |
| `discriminator` | 다형성 구분자 |
| `if`/`then`/`else` | 조건부 스키마 (3.1+) |

### 5.4 OpenAPI 전용 키워드

JSON Schema에는 없고 OpenAPI에서 추가한 키워드들이다.

| 키워드 | 설명 |
|--------|------|
| `readOnly` | 응답에서만 포함 (요청 시 무시) |
| `writeOnly` | 요청에서만 포함 (응답 시 무시). 예: 비밀번호 |
| `deprecated` | 해당 요소가 더 이상 사용되지 않음을 표시 |
| `xml` | XML 표현 방식 커스터마이징 |
| `externalDocs` | 외부 문서 링크 |
| `example` | 단일 예시 값 |
| `examples` | 복수 예시 값 (Operation/Media Type 레벨) |

### 5.5 Operation Object 주요 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `tags` | [string] | 해당 오퍼레이션의 태그 목록 |
| `summary` | string | 짧은 설명 (한 줄) |
| `description` | string | 상세 설명 (Markdown 지원) |
| `operationId` | string | 오퍼레이션 고유 식별자 (코드 생성 시 함수명으로 사용) |
| `parameters` | [Parameter] | 파라미터 목록 |
| `requestBody` | RequestBody | 요청 본문 정의 |
| `responses` | Responses | 응답 정의 (필수) |
| `callbacks` | Map[Callback] | 콜백 정의 |
| `deprecated` | boolean | 사용 중지 여부 |
| `security` | [SecurityRequirement] | 보안 요구사항 (전역 오버라이드) |
| `servers` | [Server] | 서버 목록 (전역 오버라이드) |

---

## 6. 사용 사례 및 활용 도구

### 6.1 주요 사용 사례

API-First 설계 (Design-First)

코드 작성 전에 OpenAPI 명세를 먼저 작성하는 접근 방식이다. 프론트엔드와 백엔드 팀이 API 계약에 합의한 후 병렬로 개발할 수 있어, 대규모 프로젝트에서 특히 유리하다.

```
명세 작성 -> 리뷰/합의 -> 코드 생성 -> 구현 -> 검증
```

Code-First 설계

기존 코드에서 어노테이션이나 데코레이터를 사용하여 OpenAPI 명세를 자동 생성하는 접근 방식이다. 기존 프로젝트에 OAS를 도입할 때 유리하다.

```
코드 작성 -> 어노테이션 추가 -> 명세 자동 생성 -> 문서/SDK 생성
```

API 게이트웨이 구성

OpenAPI 명세를 기반으로 API 게이트웨이의 라우팅, 요청 검증, 속도 제한 등을 자동으로 구성할 수 있다.

계약 테스트 (Contract Testing)

명세를 기준으로 실제 API의 요청/응답이 계약을 준수하는지 자동으로 검증한다.

모킹 (Mock Server)

명세만으로 가상의 API 서버를 구동하여, 백엔드 개발 완료 전에 프론트엔드 개발이나 통합 테스트를 진행할 수 있다.

### 6.2 SmartBear Swagger 도구 생태계

| 도구 | 설명 |
|------|------|
| Swagger Editor | 브라우저 기반 OpenAPI 명세 편집기. 실시간 문법 검증과 미리보기를 제공한다 |
| Swagger UI | OpenAPI 명세를 인터랙티브한 API 문서로 렌더링한다. API를 직접 호출해볼 수 있다 |
| Swagger Codegen | 명세에서 서버 스텁과 클라이언트 SDK를 40개 이상의 언어로 생성한다 |
| SwaggerHub | 팀 협업을 위한 SaaS 플랫폼. 명세 호스팅, 버전 관리, 협업 기능을 제공한다 |

### 6.3 코드 생성 도구

| 도구 | 설명 |
|------|------|
| OpenAPI Generator | Swagger Codegen의 커뮤니티 포크. 더 활발하게 유지보수되며 50개 이상의 언어를 지원한다 |
| openapi-typescript | TypeScript 타입 정의를 생성한다 |
| oapi-codegen | Go 언어용 코드 생성기 |
| NSwag | .NET/C# 클라이언트 및 컨트롤러를 생성한다 |
| Kiota (Microsoft) | 다국어 API 클라이언트를 생성한다 |

### 6.4 문서화 도구

| 도구 | 설명 |
|------|------|
| Redoc | Swagger UI의 대안으로 깔끔한 3열 레이아웃의 API 문서를 생성한다 |
| Stoplight Elements | 웹 컴포넌트 기반의 API 문서 UI를 제공한다 |
| RapiDoc | 커스터마이징이 용이한 웹 컴포넌트 기반 문서 뷰어이다 |
| Scalar | 모던한 디자인의 API 문서 생성 도구이다 |

### 6.5 검증 및 린팅 도구

| 도구 | 설명 |
|------|------|
| Spectral (Stoplight) | OpenAPI 명세의 린팅 도구. 커스텀 규칙 정의가 가능하다 |
| openapi-spec-validator | Python 기반 명세 검증 도구이다 |
| vacuum | 고성능 OpenAPI 린터이다 |
| Redocly CLI | 명세 검증, 번들링, 미리보기 기능을 제공한다 |

### 6.6 모킹 및 테스트 도구

| 도구 | 설명 |
|------|------|
| Prism (Stoplight) | OpenAPI 명세 기반 Mock 서버 및 검증 프록시이다 |
| Microcks | API 모킹 및 계약 테스트 플랫폼이다 |
| Schemathesis | 명세 기반 자동 API 퍼징(Fuzzing) 테스트 도구이다 |
| Dredd | API 명세와 실제 구현의 일치 여부를 검증한다 |
| Postman | OpenAPI 명세를 가져와 컬렉션으로 변환하여 테스트할 수 있다 |

### 6.7 프레임워크 통합

| 프레임워크/언어 | 도구 | 방식 |
|----------------|------|------|
| Spring Boot (Java) | springdoc-openapi | Code-First (어노테이션에서 명세 생성) |
| FastAPI (Python) | 내장 지원 | Code-First (타입 힌트에서 자동 생성) |
| NestJS (TypeScript) | @nestjs/swagger | Code-First (데코레이터에서 명세 생성) |
| Express (Node.js) | swagger-jsdoc | Code-First (JSDoc 주석에서 생성) |
| ASP.NET Core (.NET) | Swashbuckle / NSwag | Code-First |
| Go | swag (swaggo) | Code-First (주석에서 생성) |
| Ruby on Rails | rswag | Code-First (RSpec에서 생성) |

---

## 7. 장점과 단점

### 7.1 장점

표준화된 계약
- 프론트엔드, 백엔드, QA, 기획 등 모든 이해관계자가 동일한 명세를 기준으로 소통할 수 있다
- API의 단일 진실 공급원(Single Source of Truth)으로 기능한다

자동화 생태계
- 하나의 명세 파일로부터 문서, 클라이언트 SDK, 서버 스텁, 테스트, Mock 서버 등을 자동 생성할 수 있다
- 수동 작업으로 인한 불일치 문제를 줄인다

풍부한 도구 지원
- 수백 개의 오픈 소스 및 상용 도구가 OAS를 지원한다
- 대부분의 프로그래밍 언어와 프레임워크에 대한 통합이 존재한다

API 거버넌스
- 린팅 도구를 통해 조직 차원의 API 설계 표준을 강제할 수 있다
- 버전 관리와 변경 이력 추적이 용이하다

테스트 용이성
- 명세 기반 계약 테스트로 API의 하위 호환성을 검증할 수 있다
- Mock 서버를 통해 의존성 없이 테스트할 수 있다

탐색 및 이해 용이성
- 인터랙티브 문서를 통해 API를 쉽게 탐색하고 직접 호출해볼 수 있다
- 신규 개발자의 온보딩 시간을 단축한다

언어 및 플랫폼 독립성
- JSON/YAML이라는 범용 형식을 사용하여 어떤 기술 스택에서든 활용할 수 있다

### 7.2 단점

학습 곡선
- 명세 작성 문법이 처음에는 복잡하게 느껴질 수 있다
- 특히 고급 기능(discriminator, callbacks, links 등)은 이해하기 어렵다

명세 유지보수 비용
- API가 변경될 때마다 명세도 함께 업데이트해야 한다
- Code-First가 아닌 경우, 명세와 실제 구현이 불일치할 위험이 있다

REST API에 한정
- RESTful HTTP API만을 대상으로 한다
- GraphQL, gRPC, WebSocket, 이벤트 기반 API 등은 별도의 명세(GraphQL SDL, Protocol Buffers, AsyncAPI 등)가 필요하다

표현의 한계
- 복잡한 비즈니스 로직이나 워크플로를 표현하기 어렵다
- 요청 간의 의존 관계나 순서를 명시적으로 기술하기 어렵다
- 동적으로 구조가 변하는 응답을 기술하기 까다롭다

대규모 명세의 관리 어려움
- 수백 개의 엔드포인트를 가진 API는 단일 파일로 관리하기 어려워진다
- 파일 분할 시 `$ref`가 복잡해지며, 번들링 도구가 필요하다

버전 간 마이그레이션 비용
- 2.0에서 3.0, 3.0에서 3.1로의 마이그레이션에 상당한 노력이 필요하다
- 일부 도구가 최신 버전을 완전히 지원하지 않을 수 있다

코드 생성의 품질
- 자동 생성된 코드가 항상 프로덕션 품질은 아니다
- 생성기마다 결과물의 스타일과 품질 차이가 크다

---

## 8. 실제 예시

아래는 "도서 관리 API"를 정의하는 완전한 OpenAPI 3.1 명세 예시이다.

```yaml
openapi: "3.1.0"

info:
  title: "도서 관리 API"
  version: "1.0.0"
  description: |
    도서의 CRUD 기능을 제공하는 RESTful API이다.

    ## 인증
    모든 API 호출에는 Bearer 토큰이 필요하다.
    `/auth/login` 엔드포인트에서 토큰을 발급받을 수 있다.
  contact:
    name: "API 지원팀"
    email: "api-support@example.com"
  license:
    name: "MIT"
    identifier: "MIT"

servers:
  - url: "https://api.example.com/v1"
    description: "운영 서버"
  - url: "https://staging-api.example.com/v1"
    description: "스테이징 서버"

tags:
  - name: books
    description: "도서 관리"
  - name: auth
    description: "인증"

security:
  - BearerAuth: []

paths:
  # === 인증 ===
  /auth/login:
    post:
      tags: [auth]
      summary: "로그인"
      operationId: login
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [username, password]
              properties:
                username:
                  type: string
                  examples: ["admin"]
                password:
                  type: string
                  format: password
      responses:
        '200':
          description: "로그인 성공"
          content:
            application/json:
              schema:
                type: object
                properties:
                  token:
                    type: string
                    description: "JWT 액세스 토큰"
                  expiresIn:
                    type: integer
                    description: "토큰 만료 시간(초)"
        '401':
          description: "인증 실패"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Error'

  # === 도서 목록 ===
  /books:
    get:
      tags: [books]
      summary: "도서 목록 조회"
      description: "페이지네이션을 지원하며, 제목과 저자로 검색할 수 있다."
      operationId: listBooks
      parameters:
        - $ref: '#/components/parameters/PageParam'
        - $ref: '#/components/parameters/LimitParam'
        - name: search
          in: query
          description: "제목 또는 저자 검색어"
          schema:
            type: string
        - name: genre
          in: query
          description: "장르 필터"
          schema:
            type: string
            enum: [fiction, non-fiction, science, history, technology]
        - name: sort
          in: query
          description: "정렬 기준"
          schema:
            type: string
            enum: [title, author, publishedDate, rating]
            default: title
        - name: order
          in: query
          description: "정렬 순서"
          schema:
            type: string
            enum: [asc, desc]
            default: asc
      responses:
        '200':
          description: "조회 성공"
          headers:
            X-Total-Count:
              description: "전체 도서 수"
              schema:
                type: integer
            X-Total-Pages:
              description: "전체 페이지 수"
              schema:
                type: integer
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    type: array
                    items:
                      $ref: '#/components/schemas/Book'
                  pagination:
                    $ref: '#/components/schemas/Pagination'
              example:
                data:
                  - id: "550e8400-e29b-41d4-a716-446655440000"
                    title: "클린 코드"
                    author: "로버트 C. 마틴"
                    isbn: "978-0132350884"
                    genre: "technology"
                    publishedDate: "2008-08-01"
                    rating: 4.5
                    createdAt: "2024-01-15T09:30:00Z"
                pagination:
                  page: 1
                  limit: 20
                  totalItems: 150
                  totalPages: 8
        '401':
          $ref: '#/components/responses/Unauthorized'

    post:
      tags: [books]
      summary: "새 도서 등록"
      operationId: createBook
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateBookRequest'
            example:
              title: "클린 아키텍처"
              author: "로버트 C. 마틴"
              isbn: "978-0134494166"
              genre: "technology"
              publishedDate: "2017-09-10"
              description: "소프트웨어 구조와 설계의 보편적 원칙"
      responses:
        '201':
          description: "도서 등록 완료"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Book'
          links:
            GetBook:
              operationId: getBook
              parameters:
                bookId: '$response.body#/id'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '409':
          description: "ISBN 중복"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Error'

  # === 개별 도서 ===
  /books/{bookId}:
    parameters:
      - $ref: '#/components/parameters/BookIdParam'

    get:
      tags: [books]
      summary: "도서 상세 조회"
      operationId: getBook
      responses:
        '200':
          description: "조회 성공"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Book'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'

    put:
      tags: [books]
      summary: "도서 정보 전체 수정"
      operationId: updateBook
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateBookRequest'
      responses:
        '200':
          description: "수정 완료"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Book'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'

    patch:
      tags: [books]
      summary: "도서 정보 부분 수정"
      operationId: patchBook
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PatchBookRequest'
      responses:
        '200':
          description: "수정 완료"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Book'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'

    delete:
      tags: [books]
      summary: "도서 삭제"
      operationId: deleteBook
      responses:
        '204':
          description: "삭제 완료"
        '401':
          $ref: '#/components/responses/Unauthorized'
        '404':
          $ref: '#/components/responses/NotFound'

# ==========================================
# 재사용 컴포넌트
# ==========================================
components:

  # --- 스키마 ---
  schemas:
    Book:
      type: object
      required: [id, title, author, isbn, genre, publishedDate]
      properties:
        id:
          type: string
          format: uuid
          readOnly: true
          description: "도서 고유 식별자"
        title:
          type: string
          minLength: 1
          maxLength: 500
          description: "도서 제목"
        author:
          type: string
          minLength: 1
          maxLength: 200
          description: "저자"
        isbn:
          type: string
          pattern: "^978-\\d{10}$"
          description: "ISBN-13 (하이픈 포함)"
        genre:
          type: string
          enum: [fiction, non-fiction, science, history, technology]
          description: "장르"
        publishedDate:
          type: string
          format: date
          description: "출판일"
        description:
          type: ["string", "null"]
          maxLength: 2000
          description: "도서 설명"
        rating:
          type: ["number", "null"]
          format: float
          minimum: 0
          maximum: 5
          description: "평균 평점 (0-5)"
        createdAt:
          type: string
          format: date-time
          readOnly: true
        updatedAt:
          type: string
          format: date-time
          readOnly: true

    CreateBookRequest:
      type: object
      required: [title, author, isbn, genre, publishedDate]
      properties:
        title:
          type: string
          minLength: 1
          maxLength: 500
        author:
          type: string
          minLength: 1
          maxLength: 200
        isbn:
          type: string
          pattern: "^978-\\d{10}$"
        genre:
          type: string
          enum: [fiction, non-fiction, science, history, technology]
        publishedDate:
          type: string
          format: date
        description:
          type: ["string", "null"]
          maxLength: 2000

    PatchBookRequest:
      type: object
      minProperties: 1
      properties:
        title:
          type: string
          minLength: 1
          maxLength: 500
        author:
          type: string
          minLength: 1
          maxLength: 200
        genre:
          type: string
          enum: [fiction, non-fiction, science, history, technology]
        publishedDate:
          type: string
          format: date
        description:
          type: ["string", "null"]
          maxLength: 2000

    Pagination:
      type: object
      properties:
        page:
          type: integer
          description: "현재 페이지"
        limit:
          type: integer
          description: "페이지당 항목 수"
        totalItems:
          type: integer
          description: "전체 항목 수"
        totalPages:
          type: integer
          description: "전체 페이지 수"

    Error:
      type: object
      required: [code, message]
      properties:
        code:
          type: integer
          description: "HTTP 상태 코드"
        message:
          type: string
          description: "오류 메시지"
        details:
          type: ["string", "null"]
          description: "상세 오류 정보"
        timestamp:
          type: string
          format: date-time
          description: "오류 발생 시각"

  # --- 파라미터 ---
  parameters:
    BookIdParam:
      name: bookId
      in: path
      required: true
      description: "도서 ID"
      schema:
        type: string
        format: uuid
    PageParam:
      name: page
      in: query
      description: "페이지 번호"
      schema:
        type: integer
        minimum: 1
        default: 1
    LimitParam:
      name: limit
      in: query
      description: "페이지당 항목 수"
      schema:
        type: integer
        minimum: 1
        maximum: 100
        default: 20

  # --- 응답 ---
  responses:
    BadRequest:
      description: "잘못된 요청"
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
          example:
            code: 400
            message: "요청 형식이 올바르지 않습니다"
            timestamp: "2024-01-15T09:30:00Z"
    Unauthorized:
      description: "인증 필요"
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
          example:
            code: 401
            message: "유효한 인증 토큰이 필요합니다"
            timestamp: "2024-01-15T09:30:00Z"
    NotFound:
      description: "리소스를 찾을 수 없음"
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
          example:
            code: 404
            message: "요청한 리소스를 찾을 수 없습니다"
            timestamp: "2024-01-15T09:30:00Z"

  # --- 보안 스킴 ---
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: "JWT Bearer 토큰. `/auth/login`에서 발급받을 수 있다."
```

---

## 9. 관련 생태계 및 커뮤니티

### 9.1 관련 명세 및 표준

| 명세 | 관계 | 설명 |
|------|------|------|
| JSON Schema | 기반 표준 | OAS 3.1은 JSON Schema Draft 2020-12를 완전히 채택한다. 데이터 모델 정의의 근간이다 |
| AsyncAPI | 자매 명세 | 이벤트 기반/비동기 API(Kafka, WebSocket, MQTT 등)를 위한 명세. OAS의 구조와 유사하게 설계되었다 |
| JSON:API | 보완 표준 | REST API의 요청/응답 형식에 대한 규약. OAS와 함께 사용할 수 있다 |
| RAML | 경쟁 명세 | MuleSoft가 만든 API 명세 형식. OAS에 비해 채택률이 낮다 |
| API Blueprint | 경쟁 명세 | Markdown 기반의 API 명세 형식. Apiary에서 개발했다 |
| gRPC / Protocol Buffers | 대안 기술 | RPC 기반 API를 위한 명세. REST가 아닌 gRPC 프로토콜에 사용된다 |
| GraphQL SDL | 대안 기술 | GraphQL API의 스키마 정의 언어이다 |

### 9.2 OpenAPI Initiative (OAI)

역할:
- Linux Foundation 산하의 오픈 거버넌스 조직으로, OpenAPI Specification의 개발과 발전을 주도한다
- 명세의 로드맵, 릴리스 주기, 거버넌스 정책을 결정한다

주요 멤버사:
Google, Microsoft, IBM, SAP, SmartBear, Postman, Amazon 등 주요 기술 기업들이 참여하고 있다.

기술 운영 위원회(Technical Steering Committee, TSC):
- 명세의 기술적 방향을 결정하는 핵심 그룹이다
- GitHub에서 RFC(Request for Comments) 프로세스를 통해 변경 사항을 논의한다

### 9.3 커뮤니티 채널

| 채널 | URL / 설명 |
|------|------------|
| GitHub | [github.com/OAI/OpenAPI-Specification](https://github.com/OAI/OpenAPI-Specification) - 명세 원본 및 이슈 트래킹 |
| 공식 웹사이트 | [openapis.org](https://www.openapis.org/) - OAI 공식 사이트 |
| Slack | OAI 커뮤니티 Slack 워크스페이스 |
| Stack Overflow | `openapi`, `swagger` 태그 |
| 공식 블로그 | [openapis.org/blog](https://www.openapis.org/blog) |

### 9.4 연례 행사 및 컨퍼런스

- ASC (API Specifications Conference): OAI가 주최하는 연례 컨퍼런스로, API 명세 생태계 전반의 발전을 논의한다
- API World: API 관련 대규모 컨퍼런스로, OAS 관련 세션이 다수 포함된다
- APIDays: 유럽 중심의 API 컨퍼런스 시리즈이다

### 9.5 OpenAPI의 미래

- Overlays: 기존 명세에 추가 정보를 덧씌우는 별도의 문서 형식이 표준화 진행 중이다. 환경별 설정, 문서 번역 등에 활용할 수 있다
- Workflows: API 간의 호출 순서와 의존 관계를 정의하는 명세가 논의 중이다. 복잡한 비즈니스 프로세스를 표현하는 것이 목표이다
- Moonwalk (다음 메이저 버전): 현재 구조의 근본적인 개선을 목표로 하는 차기 버전에 대한 논의가 진행 중이다
- AI/LLM과의 통합: API 명세를 기반으로 AI 에이전트가 자동으로 API를 탐색하고 호출하는 패턴이 부상하고 있다. OpenAPI 명세는 LLM의 함수 호출(function calling) 인터페이스 정의에도 활용된다

---

## 참고 자료

- [OpenAPI Specification 공식 문서](https://spec.openapis.org/oas/latest.html)
- [OpenAPI Initiative GitHub](https://github.com/OAI/OpenAPI-Specification)
- [Swagger 공식 사이트](https://swagger.io/)
- [JSON Schema 공식 사이트](https://json-schema.org/)
- [OpenAPI.Tools - 도구 디렉터리](https://openapi.tools/)
