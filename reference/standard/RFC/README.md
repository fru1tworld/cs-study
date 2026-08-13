# RFC 한국어 번역

RFC 문서 한국어 번역/요약 모음.

---

## Network (네트워크)

TCP/IP 4계층 기준으로 분류.

### Link (링크 계층)

- [RFC 826](./Network/Link/RFC826-ARP.md) — ARP (Address Resolution Protocol): IP 주소를 MAC 주소로 변환

### Internet (인터넷 계층)

- [RFC 791](./Network/Internet/RFC791-IPv4.md) — IPv4: 32비트 주소 체계 인터넷 프로토콜
- [RFC 792](./Network/Internet/RFC792-ICMP.md) — ICMP: 네트워크 오류 보고/진단(ping·traceroute)
- [RFC 8200](./Network/Internet/RFC8200-IPv6.md) — IPv6: 128비트 주소 체계 차세대 프로토콜
- [RFC 4291](./Network/Internet/RFC4291-IPv6-Addressing.md) — IPv6 Addressing: IPv6 주소 아키텍처
- [RFC 5952](./Network/Internet/RFC5952-IPv6-Text-Representation.md) — IPv6 Text Representation: IPv6 주소 정규 텍스트 표현
- [RFC 6164](./Network/Internet/RFC6164-IPv6-127bit-Prefixes.md) — IPv6 /127 Prefixes: 라우터 간 P2P 링크 /127 프리픽스 권고
- [RFC 1918](./Network/Internet/RFC1918-Private-Address.md) — Private Address: 사설 IP 주소 대역
- [RFC 3022](./Network/Internet/RFC3022-NAT.md) — NAT: 사설-공인 IP 주소 변환
- [RFC 4632](./Network/Internet/RFC4632-CIDR.md) — CIDR: 클래스리스 주소 체계
- [RFC 9568](./Network/Internet/RFC9568-VRRP.md) — VRRP v3: 가상 라우터 이중화 프로토콜

### Transport (전송 계층)

- [RFC 768](./Network/Transport/RFC768-UDP.md) — UDP: 비연결형 전송 프로토콜
- [RFC 793](./Network/Transport/RFC793-TCP.md) — TCP: 연결 지향 신뢰성 전송 프로토콜
- [RFC 8446](./Network/Transport/RFC8446-TLS1.3.md) — TLS 1.3: 전송 계층 보안

### Application (응용 계층)

#### DNS & DHCP

- [RFC 1034](./Network/Application/DNS/RFC1034-DNS-Concepts.md) — DNS - Concepts: 도메인 네임 시스템 개념
- [RFC 1035](./Network/Application/DNS/RFC1035-DNS-Implementation.md) — DNS - Implementation: DNS 구현 명세
- [RFC 8484](./Network/Application/DNS/RFC8484-DoH.md) — DNS over HTTPS (DoH): HTTPS 통한 암호화된 DNS
- [RFC 9250](./Network/Application/DNS/RFC9250-DoQ.md) — DNS over QUIC (DoQ): QUIC 통한 암호화된 DNS
- [RFC 2131](./Network/Application/RFC2131-DHCP.md) — DHCP: IP 주소 자동 할당(DORA)

#### HTTP

- [RFC 9110](./Network/Application/HTTP/RFC9110-HTTP-Semantics.md) — HTTP Semantics: HTTP 의미론
- [RFC 9112](./Network/Application/HTTP/RFC9112-HTTP1.1.md) — HTTP/1.1: HTTP/1.1 메시지 문법
- [RFC 7540](./Network/Application/HTTP/RFC7540-HTTP2.md) — HTTP/2: 바이너리 프레이밍·멀티플렉싱
- [RFC 9114](./Network/Application/HTTP/RFC9114-HTTP3.md) — HTTP/3: QUIC 기반 HTTP
- [RFC 5789](./Network/Application/HTTP/RFC5789-HTTP-PATCH.md) — HTTP PATCH: 부분 수정 메서드
- [RFC 6585](./Network/Application/HTTP/RFC6585-Additional-HTTP-Status-Codes.md) — Additional HTTP Status Codes: 428·429·431·511 상태 코드
- [RFC 7234](./Network/Application/HTTP/RFC7234-HTTP-Caching.md) — HTTP Caching: Cache-Control·ETag·조건부 요청
- [RFC 7239](./Network/Application/HTTP/RFC7239-Forwarded-Header.md) — Forwarded Header: X-Forwarded-For 표준화
- [RFC 8941](./Network/Application/HTTP/RFC8941-Structured-Field-Values.md) — Structured Field Values: HTTP 필드 값 구조화 표현(List·Dictionary·Item)
- [RFC 9457](./Network/Application/HTTP/RFC9457-Problem-Details.md) — Problem Details: API 에러 응답 표준 형식
- [RFC 6265](./Network/Application/HTTP/RFC6265-HTTP-Cookies.md) — HTTP Cookies: Set-Cookie·쿠키 보안 속성
- [6265bis (초안)](./Network/Application/HTTP/RFC6265bis-SameSite-Draft.md) — SameSite (초안): SameSite/Secure 강제, `__Host-` 접두사
- [RFC 6454](./Network/Application/HTTP/RFC6454-Web-Origin.md) — Web Origin: Same-Origin Policy 기반
- [RFC 6797](./Network/Application/HTTP/RFC6797-HSTS.md) — HSTS: HTTPS 강제 적용
- [RFC 6455](./Network/Application/HTTP/RFC6455-WebSocket.md) — WebSocket: 양방향 실시간 통신
- [RFC 8216](./Network/Application/HTTP/RFC8216-HLS.md) — HLS: HTTP 기반 라이브/온디맨드 스트리밍

#### IMAP & SMTP

- [RFC 9051](./Network/Application/IMAP/RFC9051-IMAP4rev2.md) — IMAP4rev2: 현행 IMAP 표준(UTF-8/NAMESPACE/ID 기본 통합)
- [RFC 3501](./Network/Application/IMAP/RFC3501-IMAP-Obsolete.md) — IMAP4rev1 (Obsolete): 구 IMAP 표준, IMAP4rev2로 대체됨
- [RFC 2177](./Network/Application/IMAP/RFC2177-IMAP-IDLE.md) — IMAP IDLE: 폴링 없는 실시간 메일함 변경 알림 확장
- [RFC 5321](./Network/Application/RFC5321-SMTP.md) — SMTP: 이메일 전송 프로토콜

---

## Auth (인증/인가)

- [RFC 6749](./Auth/RFC6749-OAuth2.md) — OAuth 2.0: 권한 위임 인가 프레임워크
- [RFC 6750](./Auth/RFC6750-OAuth2-Bearer-Token.md) — OAuth 2.0 Bearer Token: Bearer 토큰 사용법
- [RFC 6819](./Auth/RFC6819-OAuth2-Threat-Model.md) — OAuth 2.0 Threat Model: OAuth 2.0 위협 모델 및 보안 고려사항
- [RFC 7636](./Auth/RFC7636-PKCE.md) — PKCE: OAuth 2.0 보안 강화(모바일/SPA)
- [RFC 8628](./Auth/RFC8628-OAuth2-Device-Grant.md) — Device Authorization Grant: 입력 제약 기기(스마트 TV, CLI)용 OAuth 흐름
- [RFC 9700](./Auth/RFC9700-OAuth2-Security-BCP.md) — OAuth 2.0 Security BCP: OAuth 2.0 보안 최선의 현행 관행
- [RFC 7519](./Auth/RFC7519-JWT.md) — JWT: JSON 기반 토큰 형식
- [RFC 8725](./Auth/RFC8725-JWT-BCP.md) — JWT Best Practices: JWT 보안 권장사항
- [RFC 4226](./Auth/RFC4226-HOTP.md) — HOTP: HMAC 기반 일회용 비밀번호 알고리즘
- [RFC 6238](./Auth/RFC6238-TOTP.md) — TOTP: 시간 기반 일회용 비밀번호 알고리즘

---

## Crypto (암호)

- [RFC 5116](./Crypto/RFC5116-AEAD.md) — AEAD Interface: 인증 암호화(AEAD) 인터페이스 및 알고리즘 정의
- [RFC 8439](./Crypto/RFC8439-ChaCha20-Poly1305.md) — ChaCha20-Poly1305: ChaCha20 스트림 암호 및 Poly1305 인증자
- [RFC 9106](./Crypto/RFC9106-Argon2.md) — Argon2: 패스워드 해싱용 메모리 하드 함수
- [RFC 7468](./Crypto/RFC7468-PEM-Textual-Encodings.md) — PEM Textual Encodings: PKIX/PKCS/CMS 구조의 PEM 텍스트 인코딩

---

## Format (데이터 형식)

- [RFC 8259](./Format/RFC8259-JSON.md) — JSON: 텍스트 기반 데이터 교환 형식
- [RFC 6902](./Format/RFC6902-JSON-Patch.md) — JSON Patch: JSON 문서 부분 수정(op, path, value)
- [RFC 7396](./Format/RFC7396-JSON-Merge-Patch.md) — JSON Merge Patch: 간단한 JSON 병합
- [RFC 3986](./Format/RFC3986-URI.md) — URI: URI 구조 및 인코딩 규칙
- [RFC 6570](./Format/RFC6570-URI-Template.md) — URI Template: /users/{id} 같은 URI 템플릿
- [RFC 4122](./Format/RFC4122-UUID.md) — UUID: 128비트 고유 식별자
- [RFC 4180](./Format/RFC4180-CSV.md) — CSV: CSV 파일 공통 형식 및 MIME 타입
- [RFC 3339](./Format/RFC3339-DateTime-Format.md) — Date and Time Format: 인터넷 날짜/시각 표현 형식
- [RFC 4648](./Format/RFC4648-Base-Encodings.md) — Base Encodings: Base64·Base64URL·Base32·Base16 인코딩

---

## Common (공통)

- [RFC 2119](./Common/RFC2119-Key-Words.md) — Key Words: RFC 요구사항 키워드(MUST, SHOULD, MAY)
- [RFC 1122](./Common/RFC1122-Internet-Host-Requirements.md) — Internet Host Requirements: 인터넷 호스트 통신 계층 요구사항

---

## 참고

- [RFC Editor](https://www.rfc-editor.org/) — RFC 공식 저장소
- [IETF Datatracker](https://datatracker.ietf.org/) — IETF 문서 추적
