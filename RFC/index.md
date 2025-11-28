# RFC 문서 정리

> RFC(Request for Comments)는 인터넷 기술 표준을 정의하는 공식 문서입니다.
> 이 디렉터리는 네트워크 및 웹 개발에서 핵심적인 RFC 문서들을 한국어로 정리한 자료입니다.

---

## 목차

### 기초 문서
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 2119](./RFC2119.md) | Key Words for RFCs | RFC 문서에서 사용되는 요구사항 키워드(MUST, SHOULD, MAY 등) 정의 |

---

### 링크 계층 (Link Layer)
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 826](./RFC826.md) | ARP (Address Resolution Protocol) | IP 주소를 MAC 주소로 변환하는 프로토콜 |

---

### 인터넷 계층 (Internet Layer)
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 791](./RFC791.md) | IPv4 (Internet Protocol version 4) | 32비트 주소 체계를 사용하는 인터넷 프로토콜 |
| [RFC 792](./RFC792.md) | ICMP (Internet Control Message Protocol) | 네트워크 오류 보고 및 진단 프로토콜 (ping, traceroute) |
| [RFC 8200](./RFC8200.md) | IPv6 (Internet Protocol version 6) | 128비트 주소 체계의 차세대 인터넷 프로토콜 |

---

### 전송 계층 (Transport Layer)
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 768](./RFC768.md) | UDP (User Datagram Protocol) | 비연결형, 비신뢰성 전송 프로토콜 |
| [RFC 793](./RFC793.md) | TCP (Transmission Control Protocol) | 연결 지향, 신뢰성 있는 전송 프로토콜 |

---

### 응용 계층 (Application Layer)
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 1034](./RFC1034.md) | DNS - Concepts and Facilities | 도메인 네임 시스템의 개념과 기능 |
| [RFC 1035](./RFC1035.md) | DNS - Implementation and Specification | DNS 구현 및 명세 상세 |
| [RFC 2131](./RFC2131.md) | DHCP (Dynamic Host Configuration Protocol) | IP 주소 자동 할당 프로토콜 (DORA 과정) |

---

### HTTP 프로토콜
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 9110](./RFC9110.md) | HTTP Semantics | HTTP의 핵심 의미론 (메서드, 상태 코드, 헤더) |
| [RFC 9112](./RFC9112.md) | HTTP/1.1 | HTTP/1.1 메시지 문법 및 파싱 |
| [RFC 7540](./RFC7540.md) | HTTP/2 | 바이너리 프레이밍, 멀티플렉싱, 서버 푸시 |

---

### 웹 보안 (Web Security)
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 8446](./RFC8446.md) | TLS 1.3 | 전송 계층 보안 프로토콜 최신 버전 |
| [RFC 6797](./RFC6797.md) | HSTS (HTTP Strict Transport Security) | HTTPS 강제 적용 메커니즘 |
| [RFC 6454](./RFC6454.md) | Web Origin Concept | Same-Origin Policy와 CORS의 기반이 되는 Origin 개념 |

---

### 인증/인가 (Authentication/Authorization)
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 6749](./RFC6749.md) | OAuth 2.0 | 권한 위임을 위한 인가 프레임워크 |
| [RFC 7519](./RFC7519.md) | JWT (JSON Web Token) | 클레임 기반의 컴팩트한 토큰 형식 |

---

### 실시간 통신 (Real-time Communication)
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 6455](./RFC6455.md) | WebSocket Protocol | 브라우저-서버 간 양방향 실시간 통신 프로토콜 |

---

### 데이터 형식 (Data Formats)
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 8259](./RFC8259.md) | JSON (JavaScript Object Notation) | 경량의 텍스트 기반 데이터 교환 형식 |
| [RFC 4122](./RFC4122.md) | UUID (Universally Unique Identifier) | 분산 환경에서 고유한 128비트 식별자 |

---

### 네트워크 인프라 (Network Infrastructure)
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 1122](./RFC1122.md) | Requirements for Internet Hosts | 인터넷 호스트의 통신 계층 요구사항 |
| [RFC 1918](./RFC1918.md) | Private Address Space | 사설 IP 주소 대역 (10.x.x.x, 172.16.x.x, 192.168.x.x) |
| [RFC 3022](./RFC3022.md) | NAT (Network Address Translation) | 사설-공인 IP 주소 변환 기술 |
| [RFC 4632](./RFC4632.md) | CIDR (Classless Inter-Domain Routing) | 클래스리스 주소 체계 및 슈퍼넷팅 |

---

## RFC 번호순 전체 목록

| RFC | 제목 | 분류 |
|-----|------|------|
| [RFC 768](./RFC768.md) | UDP | 전송 계층 |
| [RFC 791](./RFC791.md) | IPv4 | 인터넷 계층 |
| [RFC 792](./RFC792.md) | ICMP | 인터넷 계층 |
| [RFC 793](./RFC793.md) | TCP | 전송 계층 |
| [RFC 826](./RFC826.md) | ARP | 링크 계층 |
| [RFC 1034](./RFC1034.md) | DNS - Concepts | 응용 계층 |
| [RFC 1035](./RFC1035.md) | DNS - Implementation | 응용 계층 |
| [RFC 1122](./RFC1122.md) | Internet Host Requirements | 네트워크 인프라 |
| [RFC 1918](./RFC1918.md) | Private Address | 네트워크 인프라 |
| [RFC 2119](./RFC2119.md) | RFC Key Words | 기초 문서 |
| [RFC 2131](./RFC2131.md) | DHCP | 응용 계층 |
| [RFC 3022](./RFC3022.md) | NAT | 네트워크 인프라 |
| [RFC 4122](./RFC4122.md) | UUID | 데이터 형식 |
| [RFC 4632](./RFC4632.md) | CIDR | 네트워크 인프라 |
| [RFC 6454](./RFC6454.md) | Web Origin | 웹 보안 |
| [RFC 6455](./RFC6455.md) | WebSocket | 실시간 통신 |
| [RFC 6749](./RFC6749.md) | OAuth 2.0 | 인증/인가 |
| [RFC 6797](./RFC6797.md) | HSTS | 웹 보안 |
| [RFC 7519](./RFC7519.md) | JWT | 인증/인가 |
| [RFC 7540](./RFC7540.md) | HTTP/2 | HTTP 프로토콜 |
| [RFC 8200](./RFC8200.md) | IPv6 | 인터넷 계층 |
| [RFC 8259](./RFC8259.md) | JSON | 데이터 형식 |
| [RFC 8446](./RFC8446.md) | TLS 1.3 | 웹 보안 |
| [RFC 9110](./RFC9110.md) | HTTP Semantics | HTTP 프로토콜 |
| [RFC 9112](./RFC9112.md) | HTTP/1.1 | HTTP 프로토콜 |

---

## OSI 7계층 기준 분류

```
┌─────────────────────────────────────────────────────────────────┐
│  7. 응용 계층 (Application Layer)                               │
│     - RFC 1034, 1035 (DNS)                                      │
│     - RFC 2131 (DHCP)                                           │
│     - RFC 9110, 9112 (HTTP)                                     │
│     - RFC 7540 (HTTP/2)                                         │
│     - RFC 6455 (WebSocket)                                      │
│     - RFC 6749 (OAuth 2.0)                                      │
│     - RFC 7519 (JWT)                                            │
├─────────────────────────────────────────────────────────────────┤
│  6. 표현 계층 (Presentation Layer)                              │
│     - RFC 8446 (TLS 1.3)                                        │
│     - RFC 8259 (JSON)                                           │
├─────────────────────────────────────────────────────────────────┤
│  5. 세션 계층 (Session Layer)                                   │
│     - RFC 8446 (TLS 1.3)                                        │
├─────────────────────────────────────────────────────────────────┤
│  4. 전송 계층 (Transport Layer)                                 │
│     - RFC 768 (UDP)                                             │
│     - RFC 793 (TCP)                                             │
├─────────────────────────────────────────────────────────────────┤
│  3. 네트워크 계층 (Network Layer)                               │
│     - RFC 791 (IPv4)                                            │
│     - RFC 792 (ICMP)                                            │
│     - RFC 8200 (IPv6)                                           │
│     - RFC 1918 (Private Address)                                │
│     - RFC 3022 (NAT)                                            │
│     - RFC 4632 (CIDR)                                           │
├─────────────────────────────────────────────────────────────────┤
│  2. 데이터링크 계층 (Data Link Layer)                           │
│     - RFC 826 (ARP)                                             │
├─────────────────────────────────────────────────────────────────┤
│  1. 물리 계층 (Physical Layer)                                  │
│     - (해당 RFC 없음)                                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 주제별 학습 가이드

### 네트워크 기초
```
1. RFC 791 (IPv4) → RFC 8200 (IPv6)
2. RFC 768 (UDP) → RFC 793 (TCP)
3. RFC 792 (ICMP)
4. RFC 826 (ARP)
5. RFC 1122 (Host Requirements)
```

### 웹 개발
```
1. RFC 9110 (HTTP Semantics)
2. RFC 9112 (HTTP/1.1) → RFC 7540 (HTTP/2)
3. RFC 8446 (TLS 1.3)
4. RFC 6455 (WebSocket)
5. RFC 8259 (JSON)
```

### 인증/보안
```
1. RFC 6454 (Web Origin) - Same-Origin Policy의 기반
2. RFC 6797 (HSTS)
3. RFC 6749 (OAuth 2.0)
4. RFC 7519 (JWT)
5. RFC 8446 (TLS 1.3)
```

### 네트워크 설정
```
1. RFC 2131 (DHCP)
2. RFC 1034, 1035 (DNS)
3. RFC 1918 (Private Address)
4. RFC 3022 (NAT)
5. RFC 4632 (CIDR)
```

---

## 참고 자료

- [RFC Editor](https://www.rfc-editor.org/) - RFC 공식 저장소
- [IETF Datatracker](https://datatracker.ietf.org/) - IETF 문서 추적
- [RFC 2119](./RFC2119.md) - RFC 문서의 요구사항 키워드 정의

---

*이 문서들은 CS 학습 목적으로 정리한 한국어 번역/요약본입니다.*
