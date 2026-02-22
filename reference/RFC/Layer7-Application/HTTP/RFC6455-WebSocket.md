# RFC 6455: The WebSocket Protocol

## 개요

RFC 6455는 2011년 12월에 발행되었으며, WebSocket 프로토콜 을 정의합니다. 이 프로토콜은 브라우저 기반 애플리케이션이 여러 HTTP 연결에 의존하지 않고 서버와 양방향 통신 을 할 수 있는 메커니즘을 제공합니다.

### 핵심 특징
- 단일 TCP 연결을 통한 전이중(full-duplex) 양방향 통신
- HTTP의 요청-응답 모델에서 벗어난 지속적인 연결
- 기존 HTTP 인프라와의 하위 호환성 유지

### 배경
기존 웹 애플리케이션에서 실시간 업데이트가 필요한 경우 여러 HTTP 연결을 사용하는 폴링(polling) 방식에 의존했습니다. WebSocket은 이러한 비효율적인 우회 방법을 대체하여 진정한 실시간 통신 패턴을 가능하게 합니다.

---

## 1. URI 스킴

WebSocket은 두 가지 URI 스킴을 정의합니다:

| 스킴 | 설명 | 기본 포트 |
|------|------|-----------|
| `ws://` | 암호화되지 않은 연결 | 80 |
| `wss://` | TLS로 암호화된 연결 | 443 |

---

## 2. 오프닝 핸드셰이크 (Opening Handshake)

WebSocket 연결은 HTTP Upgrade 요청으로 시작됩니다.

### 2.1 클라이언트 요청

클라이언트는 다음과 같은 HTTP Upgrade 요청을 전송합니다:

```http
GET /chat HTTP/1.1
Host: server.example.com
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13
Origin: http://example.com
Sec-WebSocket-Protocol: chat, superchat
Sec-WebSocket-Extensions: permessage-deflate
```

#### 필수 헤더

| 헤더 | 설명 |
|------|------|
| `Host` | 서버 식별 |
| `Upgrade` | `websocket` 키워드 |
| `Connection` | `Upgrade` 토큰 |
| `Sec-WebSocket-Key` | Base64 인코딩된 16바이트 랜덤 논스(nonce) |
| `Sec-WebSocket-Version` | 프로토콜 버전 (13) |

#### 선택적 헤더

| 헤더 | 설명 |
|------|------|
| `Origin` | 요청의 출처 |
| `Sec-WebSocket-Protocol` | 서브프로토콜 협상 |
| `Sec-WebSocket-Extensions` | 확장 협상 |

### 2.2 서버 응답

서버는 HTTP 101 상태 코드로 응답합니다:

```http
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

#### Sec-WebSocket-Accept 계산

서버는 클라이언트의 `Sec-WebSocket-Key`와 고정 GUID를 연결한 후 SHA-1 해시를 계산합니다:

```
Sec-WebSocket-Accept = Base64(SHA-1(Sec-WebSocket-Key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"))
```

#### Sec-WebSocket-Key 생성 및 검증 상세

클라이언트 측 (Key 생성):
1. 16바이트(128비트)의 암호학적으로 안전한 랜덤 값을 생성합니다
2. 이 값을 Base64로 인코딩하여 24문자의 문자열을 생성합니다
3. 이 문자열을 `Sec-WebSocket-Key` 헤더로 전송합니다

```python
# Python 예시
import os
import base64

nonce = os.urandom(16)  # 16바이트 랜덤 값
sec_websocket_key = base64.b64encode(nonce).decode('utf-8')
# 결과: "dGhlIHNhbXBsZSBub25jZQ==" (예시)
```

서버 측 (Accept 계산):
1. 클라이언트로부터 받은 `Sec-WebSocket-Key` 값을 가져옵니다
2. 고정 GUID `258EAFA5-E914-47DA-95CA-C5AB0DC85B11`을 연결합니다
3. 연결된 문자열의 SHA-1 해시(20바이트)를 계산합니다
4. 해시 결과를 Base64로 인코딩합니다

```python
# Python 예시
import hashlib
import base64

GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
sec_websocket_key = "dGhlIHNhbXBsZSBub25jZQ=="

concat = sec_websocket_key + GUID
sha1_hash = hashlib.sha1(concat.encode('utf-8')).digest()
sec_websocket_accept = base64.b64encode(sha1_hash).decode('utf-8')
# 결과: "s3pPLMBiTxaQ9kYGzzhZRbK+xOo="
```

검증 단계:
| 단계 | 설명 |
|------|------|
| 1 | 클라이언트가 16바이트 논스를 Base64 인코딩하여 전송 |
| 2 | 서버가 Key + GUID를 SHA-1 해싱 후 Base64 인코딩하여 응답 |
| 3 | 클라이언트가 동일한 계산을 수행하여 서버 응답 검증 |
| 4 | 불일치 시 연결 실패 처리 |

> GUID의 목적: 이 고정 GUID는 WebSocket 프로토콜을 명확히 식별하기 위한 것으로, WebSocket을 이해하지 못하는 서버가 우연히 올바른 응답을 생성하는 것을 방지합니다.

### 2.3 TLS 핸드셰이크 (wss의 경우)

보안 연결(`wss`)의 경우, 클라이언트는 WebSocket 핸드셰이크 데이터를 전송하기 전에 Server Name Indication(SNI) 을 사용하여 TLS 협상을 수행합니다.

---

## 3. 데이터 프레이밍 (Data Framing)

### 3.1 프레임 구조

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-------+-+-------------+-------------------------------+
|F|R|R|R| opcode|M| Payload len |    Extended payload length    |
|I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
|N|V|V|V|       |S|             |   (if payload len==126/127)   |
| |1|2|3|       |K|             |                               |
+-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
|     Extended payload length continued, if payload len == 127  |
+ - - - - - - - - - - - - - - - +-------------------------------+
|                               |Masking-key, if MASK set to 1  |
+-------------------------------+-------------------------------+
| Masking-key (continued)       |          Payload Data         |
+-------------------------------- - - - - - - - - - - - - - - - +
:                     Payload Data continued ...                :
+ - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
|                     Payload Data continued ...                |
+---------------------------------------------------------------+
```

### 3.2 프레임 필드 설명

| 필드 | 비트 | 설명 |
|------|------|------|
| FIN | 1 | 메시지의 마지막 프레임 여부 |
| RSV1-3 | 3 | 확장을 위해 예약됨 |
| Opcode | 4 | 페이로드 해석 방법 |
| MASK | 1 | 페이로드 마스킹 여부 |
| Payload length | 7/16/64 | 페이로드 길이 |
| Masking-key | 32 | 마스킹 키 (클라이언트→서버만) |
| Payload Data | 가변 | 실제 데이터 |

### 3.3 Opcode 값

| Opcode | 타입 | 설명 |
|--------|------|------|
| `0x0` | 연속 | 연속 프레임 |
| `0x1` | 텍스트 | UTF-8 텍스트 데이터 |
| `0x2` | 바이너리 | 바이너리 데이터 |
| `0x8` | 닫기 | 연결 종료 |
| `0x9` | Ping | 연결 상태 확인 |
| `0xA` | Pong | Ping에 대한 응답 |

### 3.4 클라이언트-서버 마스킹

클라이언트는 서버로 전송하는 모든 프레임을 반드시 마스킹해야 합니다.

마스킹 알고리즘:
```
transformed-octet-i = original-octet-i XOR masking-key-octet-(i MOD 4)
```

- 마스킹 키는 32비트 랜덤 값 으로, 강력한 엔트로피 소스에서 생성해야 합니다.
- 서버는 마스킹되지 않은 클라이언트 프레임을 거부하고 연결을 종료해야 합니다.
- 서버는 프레임을 마스킹하지 않습니다.

#### 마스킹이 필요한 이유

마스킹은 중간 프록시가 WebSocket 프레임을 HTTP 응답으로 잘못 해석하는 것을 방지하여 캐시 오염(cache poisoning) 및 기타 인프라 공격을 차단합니다.

---

## 4. 프래그멘테이션 (Fragmentation)

메시지는 여러 프레임으로 분할될 수 있습니다:

1. 첫 번째 프래그먼트: 데이터 opcode(0x1 또는 0x2) + FIN=0
2. 연속 프래그먼트: opcode=0x0 + FIN=0
3. 마지막 프래그먼트: opcode=0x0 + FIN=1

### 규칙
- 컨트롤 프레임은 프래그멘테이션할 수 없습니다
- 컨트롤 프레임은 메시지 프래그먼트 사이에 삽입될 수 있습니다
- 프래그멘테이션을 통해 전체 페이로드를 버퍼링하지 않고 대용량 메시지를 스트리밍할 수 있습니다

---

## 5. 컨트롤 프레임

### 5.1 Close 프레임 (0x8)

연결 종료를 시작합니다.

| 필드 | 설명 |
|------|------|
| 상태 코드 | 2바이트 정수 (선택) |
| 이유 | UTF-8 문자열 (선택) |

### 5.2 Ping 프레임 (0x9)

연결 상태 확인(keepalive) 용도로 사용됩니다. 수신자는 Pong으로 응답해야 합니다.

### 5.3 Pong 프레임 (0xA)

Ping에 대한 응답입니다. 수신된 Ping 데이터를 그대로 에코합니다.

---

## 6. 연결 종료 (Closing the Connection)

### 6.1 종료 핸드셰이크

1. 시작측이 Close 프레임 전송: 상태 코드와 선택적 이유 포함
2. 응답측이 Close 프레임 전송: 아직 전송하지 않았다면 Close 프레임으로 응답
3. 시작측이 연결 종료: 응답을 받은 후 TCP 연결 종료

### 6.2 상태 코드

| 코드 | 이름 | 설명 |
|------|------|------|
| 1000 | Normal Closure | 정상 종료 |
| 1001 | Going Away | 서버 종료 또는 브라우저 이탈 |
| 1002 | Protocol Error | 프로토콜 오류 |
| 1003 | Unsupported Data | 지원하지 않는 데이터 타입 수신 |
| 1006 | Abnormal Closure | 비정상 종료 (Close 프레임 없이) |
| 1007 | Invalid Payload | 유효하지 않은 페이로드 데이터 |
| 1008 | Policy Violation | 정책 위반 |
| 1009 | Message Too Big | 메시지가 너무 큼 |
| 1010 | Missing Extension | 필수 확장 누락 |
| 1011 | Internal Error | 서버 내부 오류 |
| 1015 | TLS Handshake | TLS 핸드셰이크 실패 |

---

## 7. 데이터 송수신

### 7.1 데이터 전송

엔드포인트는 텍스트(UTF-8) 또는 바이너리 콘텐츠를 포함하는 메시지 프레임을 통해 데이터를 전송합니다.

- 적절한 opcode 설정
- FIN 비트 설정
- 클라이언트는 모든 프레임을 마스킹
- 서버는 프레임을 마스킹하지 않음

### 7.2 데이터 수신

수신자는 다음을 수행해야 합니다:
- 프래그멘트된 메시지를 전송 순서대로 재조합
- 텍스트 프레임의 UTF-8 인코딩 유효성 검사
- 프래그멘트된 메시지 수신 중에도 컨트롤 프레임을 즉시 처리
- 가변 길이 프레임을 올바르게 처리

---

## 8. 서브프로토콜과 확장

### 8.1 서브프로토콜

`Sec-WebSocket-Protocol` 헤더를 통해 애플리케이션 수준의 기능을 레이어링합니다.

```http
Sec-WebSocket-Protocol: chat.example.com
```

권장사항: 도메인 기반 네이밍 사용 (예: `chat.example.com`)

### 8.2 확장

`Sec-WebSocket-Extensions` 헤더를 통해 핸드셰이크 중에 협상됩니다.

```http
Sec-WebSocket-Extensions: permessage-deflate
```

---

## 9. 보안 고려사항

### 9.1 Origin 기반 보안

- WebSocket은 브라우저 모델과 일치하는 Origin 기반 보안 을 구현합니다
- 서버는 Origin 헤더를 검증하여 권한 없는 스크립트 컨텍스트로부터의 연결을 제한해야 합니다
- 연결을 수락하지 않으려면 적절한 HTTP 오류 코드를 반환해야 합니다

### 9.2 비브라우저 클라이언트

비브라우저 클라이언트가 WebSocket 서버에 접근할 때는 Origin 기반 보호를 우회합니다. 서버는 이러한 연결에 대해 대체 인증 메커니즘 을 구현해야 합니다.

### 9.3 마스킹의 중요성

- 클라이언트 측 마스킹은 중간 프록시가 프레임을 HTTP 응답으로 해석하는 것을 방지
- TLS 암호화 여부와 관계없이 마스킹 요구사항 적용
- 캐시 오염 및 기타 인프라 공격 방지

### 9.4 구현 제한

- 서버는 메모리 고갈 공격을 방지하기 위해 합리적인 페이로드 길이 제한을 적용해야 합니다
- 고부하 시 호스트/IP 주소당 연결 제한 구현 권장

### 9.5 UTF-8 유효성 검사

- 텍스트 프레임은 유효한 UTF-8 시퀀스를 포함해야 합니다
- 구현체는 인코딩의 유효성을 검사하고 잘못된 시퀀스가 감지되면 연결을 종료해야 합니다

### 9.6 인증서 유효성 검사

보안 WebSocket 연결(`wss`)은 엄격한 TLS 인증서 유효성 검사가 필요합니다:
- 서버 인증서 확인
- 호스트 이름 매칭 확인

---

## 10. 핵심 요약

| 항목 | 내용 |
|------|------|
| 목적 | 브라우저와 서버 간 실시간 양방향 통신 |
| 연결 방식 | HTTP Upgrade를 통한 핸드셰이크 후 단일 TCP 연결 유지 |
| 데이터 전송 | 프레임 기반, 텍스트/바이너리 지원 |
| 마스킹 | 클라이언트→서버 방향만 필수 |
| 보안 | Origin 기반 + TLS(wss) |
| 종료 | Close 프레임을 통한 깔끔한 종료 |

---

## 참고

- [RFC 6455 원문](https://www.rfc-editor.org/rfc/rfc6455)
- 발행일: 2011년 12월
- 상태: Proposed Standard
