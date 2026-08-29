# Ktor WebSocket과 정적 콘텐츠

## 11. WebSocket

> 출처: https://ktor.io/docs/server-websockets.html

---

### 의존성과 설치

```kotlin
// build.gradle.kts
implementation("io.ktor:ktor-server-websockets:$ktor_version")
```

```kotlin
install(WebSockets) {
    pingPeriod = 15.seconds
    timeout    = 15.seconds
    maxFrameSize = Long.MAX_VALUE
    masking      = false
}
```

설치 옵션:

- `pingPeriod`: 서버가 ping을 보내는 주기
- `timeout`: pong을 못 받았을 때 끊을 한계
- `maxFrameSize`: 한 프레임의 최대 바이트
- `masking`: 송신 프레임 마스킹 여부

---

### 엔드포인트 정의

`routing` 블록 안에서 `webSocket("/path") { ... }`로 선언함. 람다는 `DefaultWebSocketServerSession`을 컨텍스트로 받음.

```kotlin
routing {
    webSocket("/echo") {
        send("Connected")
        for (frame in incoming) {
            when (frame) {
                is Frame.Text -> {
                    val text = frame.readText()
                    if (text.equals("bye", ignoreCase = true)) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "bye"))
                    } else {
                        send("ECHO: $text")
                    }
                }
                is Frame.Binary -> { /* frame.readBytes() */ }
                is Frame.Close  -> {}
                else -> {}
            }
        }
    }
}
```

핵심 API:

- `incoming: ReceiveChannel<Frame>` — 들어오는 프레임 채널
- `outgoing: SendChannel<Frame>` — 송신 채널. `send("...")`는 헬퍼 함수
- `close(CloseReason)` — 정상 종료

---

### 표준 WebSocket 이벤트와의 대응

브라우저 WebSocket API의 `onopen`/`onmessage`/`onclose`/`onerror` 콜백에 익숙하다면, Ktor의 `webSocket { }` 블록은 다음과 같이 대응됨.

- onConnect: 블록이 시작되는 시점 자체가 연결 성사 시점임 → 별도 콜백 없이 블록 맨 위 코드가 그 역할을 함
- onMessage: `incoming` 채널에서 프레임을 성공적으로 읽어온 직후 (`for (frame in incoming)` 또는 `incoming.receive()`)
- onClose: `incoming` 채널이 닫히는 시점 → 보통 `ClosedReceiveChannelException`으로 루프가 빠져나옴
- onError: 그 외의 예외가 발생하는 시점 → 다른 예외 처리와 동일하게 취급됨

onClose·onError 두 경우 모두 세션의 `closeReason` 프로퍼티가 채워지므로, 정상 종료인지 비정상 종료인지와 종료 코드를 그 값으로 구분할 수 있음.

```kotlin
webSocket("/events") {
    try {
        for (frame in incoming) {
            // onMessage에 해당
        }
    } catch (e: ClosedReceiveChannelException) {
        // onClose에 해당
        println("Session closed: ${closeReason.await()}")
    } catch (e: Throwable) {
        // onError에 해당
        println("Error: ${closeReason.await()}")
    }
}
```

---

### 프레임 타입

- `Frame.Text`: UTF-8 텍스트 → `readText()`로 추출
- `Frame.Binary`: 바이너리 데이터 → `readBytes()`로 추출
- `Frame.Close`: 종료 프레임 → `readReason()`로 코드 확인, `incoming`에 포함되지 않으며 직접 다루려면 `webSocketRaw` 사용
- `Frame.Ping` / `Frame.Pong`: keep-alive용 → `incoming`에 포함되지 않으며 직접 다루려면 `webSocketRaw` 사용

---

### 직렬화를 통한 객체 송수신

WebSockets 플러그인에 `contentConverter`를 설정하면 `sendSerialized` / `receiveDeserialized`로 객체를 그대로 주고받을 수 있음.

```kotlin
@Serializable
data class ChatMsg(val from: String, val body: String)

webSocket("/chat") {
    for (frame in incoming) {
        if (frame is Frame.Text) {
            val msg = converter!!.deserialize<ChatMsg>(frame)
            sendSerialized(msg.copy(body = msg.body.uppercase()))
        }
    }
}
```

---

### 다중 클라이언트 (브로드캐스트) 패턴

세션을 `ConcurrentSet` 등에 모아 두고 메시지가 올 때마다 모두에게 보내는 방식이 보편적임.

```kotlin
val sessions = Collections.synchronizedSet<DefaultWebSocketServerSession>(mutableSetOf())

webSocket("/room") {
    sessions += this
    try {
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                sessions.forEach { it.send(text) }
            }
        }
    } finally {
        sessions -= this
    }
}
```

스레드 안전성과 backpressure가 중요한 대규모 채팅 시스템에서는 `SharedFlow`로 채널을 분리하는 패턴을 자주 씀.

---

### 예외 처리

`incoming` 루프 바깥에서 발생한 예외는 자동으로 채널 종료 및 비정상 close로 이어짐. 의도적으로 종료할 때는 `close(CloseReason(code, message))`를 사용함.

```kotlin
webSocket("/notify") {
    try {
        for (frame in incoming) { /* ... */ }
    } catch (e: ClosedReceiveChannelException) {
        // 정상 종료
    } catch (e: Throwable) {
        application.log.error("ws error", e)
    }
}
```

---

## 12. 정적 콘텐츠 (Static Content)

> 출처: https://ktor.io/docs/server-static-content.html

---

### 두 가지 소스

- `staticResources(remotePath, basePackage)`: 클래스패스 리소스에서 읽음 (`src/main/resources/...`, JAR 내부)
- `staticFiles(remotePath, dir)`: 로컬 파일시스템에서 읽음 (`File("...")`)

```kotlin
routing {
    staticResources("/assets", "static")          // /assets/* → resources/static/*
    staticFiles("/uploads", File("data/uploads")) // /uploads/* → ./data/uploads/*
}
```

`staticResources`는 jar로 패키징해도 그대로 동작하지만 변경 시 재배포 필요. `staticFiles`는 외부에서 갈아끼울 수 있어 사용자 업로드 등에 적합.

---

### 인덱스 페이지

기본은 `index.html`. 다른 파일로 바꾸려면 다음과 같이 설정함.

```kotlin
staticResources("/", "static", index = "home.html")
```

인덱스가 필요 없으면 `index = null`로 설정.

---

### 사전 압축 (gzip / brotli)

같은 디렉터리에 `app.js`, `app.js.gz`, `app.js.br`이 모두 있을 때, 클라이언트의 `Accept-Encoding`에 맞는 파일을 골라 응답함.

```kotlin
staticFiles("/", File("public")) {
    preCompressed(CompressedFileType.BROTLI, CompressedFileType.GZIP)
}
```

---

### 캐시 헤더

```kotlin
install(ConditionalHeaders)

staticFiles("/files", File("textFiles")) {
    cacheControl { file ->
        when (file.extension) {
            "html" -> listOf(CacheControl.NoCache(null))
            "js", "css" -> listOf(CacheControl.MaxAge(60 * 60 * 24 * 30))
            else -> emptyList()
        }
    }
}
```

`ConditionalHeaders`를 같이 깔면 `ETag` / `Last-Modified` 기반 304 응답이 동작함.

---

### 일부 파일 차단 / 폴백 / HEAD

```kotlin
staticFiles("/site", File("public")) {
    exclude { file -> file.name.startsWith(".") }     // dotfile 차단
    enableAutoHeadResponse()                          // HEAD 지원
    default("index.html")                             // SPA 폴백
}
```

`default("index.html")`은 SPA에서 `/some/deep/route` 같은 경로도 `index.html`로 응답하는 일반적인 패턴임.

---

### 그 외 옵션

- `staticZip(remotePath, basePath, zip)`: ZIP 아카이브에서 직접 서빙
- `modify { file, call -> ... }`: 응답 전 후크

---

### 권한이 있는 정적 파일

`staticFiles`에도 라우트 스코프 플러그인을 적용 가능. 인증이 필요하면 `authenticate {}` 블록으로 감싸면 됨.

```kotlin
authenticate("auth-session") {
    staticFiles("/private", File("private"))
}
```
