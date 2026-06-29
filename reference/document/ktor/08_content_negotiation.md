# 08. Content Negotiation 과 직렬화

> 출처: https://ktor.io/docs/server-serialization.html

---

## 무엇을 하는 플러그인인가

`ContentNegotiation`은 두 가지를 동시에 처리합니다.

1. **콘텐츠 협상**: 클라이언트의 `Accept` 헤더와 서버가 지원하는 포맷을 매칭.
2. **직렬화/역직렬화**: JSON / XML / CBOR / ProtoBuf 등을 객체 ↔ 본문으로 자동 변환.

이게 없으면 `call.receive<MyDto>()`나 `call.respond(myDto)`가 동작하지 않습니다.

---

## 의존성

- 코어: `io.ktor:ktor-server-content-negotiation`
- 포맷별 컨버터: `ktor-serialization-kotlinx-json`, `-xml`, `-cbor`, `-protobuf`, `ktor-serialization-jackson`, `ktor-serialization-gson` 등

`build.gradle.kts` 예:

```kotlin
dependencies {
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
}
```

---

## 설치

```kotlin
install(ContentNegotiation) {
    json()       // kotlinx.serialization
    // xml()
    // cbor()
}
```

설정을 전달하는 경우:

```kotlin
install(ContentNegotiation) {
    json(Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = false
    })
}
```

여러 포맷을 동시에 등록해 두면 `Accept` 헤더에 따라 자동 선택됩니다.

---

## 사용 흐름

### 응답 직렬화

```kotlin
@Serializable
data class User(val id: Long, val name: String)

get("/users/{id}") {
    val user = service.find(call.parameters["id"]!!)
    call.respond(user)        // Accept: application/json → JSON
}
```

### 요청 역직렬화

```kotlin
@Serializable
data class CreateUser(val name: String, val email: String)

post("/users") {
    val req = call.receive<CreateUser>()
    val created = service.create(req)
    call.respond(HttpStatusCode.Created, created)
}
```

---

## 라이브러리별 비교

| 라이브러리 | 강점 | 비고 |
| --- | --- | --- |
| **kotlinx.serialization** | Kotlin-native, 멀티플랫폼, `@Serializable` | Ktor 공식 권장 |
| **Jackson** | 풍부한 모듈 / 어노테이션 생태계 | 리플렉션 기반 |
| **Gson** | 단순함 | 코틀린 null/default 처리에서 가끔 함정 |

---

## 커스텀 컨버터

`ContentConverter` 인터페이스를 직접 구현해 임의의 미디어 타입을 처리할 수 있습니다.

```kotlin
class CsvConverter : ContentConverter {
    override suspend fun serialize(
        contentType: ContentType, charset: Charset, typeInfo: TypeInfo, value: Any
    ): OutgoingContent? = TODO()

    override suspend fun deserialize(
        charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel
    ): Any? = TODO()
}

install(ContentNegotiation) {
    register(ContentType.Text.CSV, CsvConverter())
}
```

---

## 주의할 점

- `@Serializable`이 없는 일반 클래스는 kotlinx.serialization에서 처리하지 못합니다.
- 클라이언트가 `Accept: */*`인 경우 등록 순서대로 첫 컨버터가 쓰입니다.
- `respondNullable`이 아닌 `respond`에 `null`을 넘기면 예외가 납니다.
