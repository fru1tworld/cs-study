# RFC 한국어 번역

RFC 문서 한국어 번역/요약 모음입니다.

---

## OSI 7계층 기준 분류

### Layer 2 - Data Link (데이터링크 계층)
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 826](./Layer2-DataLink/RFC826-ARP.md) | ARP (Address Resolution Protocol) | IP 주소를 MAC 주소로 변환 |

### Layer 3 - Network (네트워크 계층)
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 791](./Layer3-Network/RFC791-IPv4.md) | IPv4 | 32비트 주소 체계 인터넷 프로토콜 |
| [RFC 792](./Layer3-Network/RFC792-ICMP.md) | ICMP | 네트워크 오류 보고/진단 (ping, traceroute) |
| [RFC 8200](./Layer3-Network/RFC8200-IPv6.md) | IPv6 | 128비트 주소 체계 차세대 프로토콜 |
| [RFC 1918](./Layer3-Network/RFC1918-Private-Address.md) | Private Address | 사설 IP 주소 대역 |
| [RFC 3022](./Layer3-Network/RFC3022-NAT.md) | NAT | 사설-공인 IP 주소 변환 |
| [RFC 4632](./Layer3-Network/RFC4632-CIDR.md) | CIDR | 클래스리스 주소 체계 |
| [RFC 9568](./Layer3-Network/RFC9568-VRRP.md) | VRRP v3 | 가상 라우터 이중화 프로토콜 |

### Layer 4 - Transport (전송 계층)
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 768](./Layer4-Transport/RFC768-UDP.md) | UDP | 비연결형 전송 프로토콜 |
| [RFC 793](./Layer4-Transport/RFC793-TCP.md) | TCP | 연결 지향 신뢰성 전송 프로토콜 |

### Layer 7 - Application (응용 계층)
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 1034](./Layer7-Application/RFC1034-DNS-Concepts.md) | DNS - Concepts | 도메인 네임 시스템 개념 |
| [RFC 1035](./Layer7-Application/RFC1035-DNS-Implementation.md) | DNS - Implementation | DNS 구현 명세 |
| [RFC 8484](./Layer7-Application/RFC8484-DoH.md) | DNS over HTTPS (DoH) | HTTPS 통한 암호화된 DNS |
| [RFC 9250](./Layer7-Application/RFC9250-DoQ.md) | DNS over QUIC (DoQ) | QUIC 통한 암호화된 DNS |
| [RFC 2131](./Layer7-Application/RFC2131-DHCP.md) | DHCP | IP 주소 자동 할당 (DORA) |
| [RFC 9110](./Layer7-Application/RFC9110-HTTP-Semantics.md) | HTTP Semantics | HTTP 의미론 |
| [RFC 9112](./Layer7-Application/RFC9112-HTTP1.1.md) | HTTP/1.1 | HTTP/1.1 메시지 문법 |
| [RFC 7540](./Layer7-Application/RFC7540-HTTP2.md) | HTTP/2 | 바이너리 프레이밍, 멀티플렉싱 |
| [RFC 6455](./Layer7-Application/RFC6455-WebSocket.md) | WebSocket | 양방향 실시간 통신 |
| [RFC 6749](./Layer7-Application/RFC6749-OAuth2.md) | OAuth 2.0 | 권한 위임 인가 프레임워크 |
| [RFC 7519](./Layer7-Application/RFC7519-JWT.md) | JWT | JSON 기반 토큰 형식 |
| [RFC 6454](./Layer7-Application/RFC6454-Web-Origin.md) | Web Origin | Same-Origin Policy 기반 |
| [RFC 6797](./Layer7-Application/RFC6797-HSTS.md) | HSTS | HTTPS 강제 적용 |
| [RFC 8446](./Layer7-Application/RFC8446-TLS1.3.md) | TLS 1.3 | 전송 계층 보안 |
| [RFC 8259](./Layer7-Application/RFC8259-JSON.md) | JSON | 텍스트 기반 데이터 교환 형식 |
| [RFC 4122](./Layer7-Application/RFC4122-UUID.md) | UUID | 128비트 고유 식별자 |

### Common (공통)
| RFC | 제목 | 설명 |
|-----|------|------|
| [RFC 2119](./Common/RFC2119-Key-Words.md) | Key Words | RFC 요구사항 키워드 (MUST, SHOULD, MAY) |
| [RFC 1122](./Common/RFC1122-Internet-Host-Requirements.md) | Internet Host Requirements | 인터넷 호스트 통신 계층 요구사항 |
| [RFC 9106](./Common/RFC9106-Argon2.md) | Argon2 | 패스워드 해싱용 메모리 하드 함수 |

---

## 참고

- [RFC Editor](https://www.rfc-editor.org/) - RFC 공식 저장소
- [IETF Datatracker](https://datatracker.ietf.org/) - IETF 문서 추적
