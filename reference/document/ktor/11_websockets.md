# 11. WebSocket

> 출처: https://ktor.io/docs/server-websockets.html

---

## 의존성과 설치

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

| 옵션 | 의미 |
| --- | --- |
| `pingPeriod` | 서버가 ping을 보내는 주기 |
| `timeout` | pong을 못 받았을 때 끊을 한계 |
| `maxFrameSize` | 한 프레임의 최대 바이트 |
| `masking` | 송신 프레임 마스킹 여부 |

---

## 엔드포인트 정의

`routing` 블록 안에서 `webSocket("/path") { ... }`로 선언합니다. 람다는 `DefaultWebSocketServerSession`을 컨텍스트로 받습니다.

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

- `incoming: ReceiveChannel<Frame>` — 들어오는 프레임 채널.
- `outgoing: SendChannel<Frame>` — 송신 채널. `send("...")`는 헬퍼 함수.
- `close(CloseReason)` — 정상 종료.

---

## 프레임 타입

| 타입 | 용도 |
| --- | --- |
| `Frame.Text` | UTF-8 텍스트. `readText()`로 추출. |
| `Frame.Binary` | 바이너리 데이터. `readBytes()`. |
| `Frame.Close` | 종료 프레임. `readReason()`로 코드 확인. `incoming`에 포함되지 않으며, 직접 다루려면 `webSocketRaw` 사용. |
| `Frame.Ping` / `Frame.Pong` | keep-alive — `incoming`에 포함되지 않으며, 직접 다루려면 `webSocketRaw` 사용. |

---

## 직렬화를 통한 객체 송수신

WebSockets 플러그인에 `contentConverter`를 설정하면 `sendSerialized` / `receiveDeserialized`로 객체를 그대로 주고받을 수 있습니다.

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

## 다중 클라이언트 (브로드캐스트) 패턴

세션을 `ConcurrentSet` 등에 모아 두고 메시지가 올 때마다 모두에게 보내는 게 보편적인 방식입니다.

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

스레드 안전성과 backpressure가 중요한 대규모 채팅 시스템에서는 `SharedFlow`로 채널을 분리하는 패턴을 자주 씁니다.

---

## 예외 처리

`incoming` 루프 바깥에서 발생한 예외는 자동으로 채널 종료 및 비정상 close로 이어집니다. 의도적으로 종료할 때는 `close(CloseReason(code, message))`를 사용합니다.

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
