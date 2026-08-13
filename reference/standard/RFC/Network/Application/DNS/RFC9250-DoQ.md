# RFC 9250 - DNS over Dedicated QUIC Connections (DoQ)

```
Internet Engineering Task Force (IETF)                        C. Huitema
Request for Comments: 9250                          Private Octopus Inc.
Category: Standards Track                                   S. Dickinson
ISSN: 2070-1721                                               Sinodun IT
                                                               A. Mankin
                                                              Salesforce
                                                                May 2022
```

### Abstract

- DNS에 대한 전송 기밀성 제공을 위한 QUIC 사용 기술
- QUIC 암호화: TLS와 유사한 속성 → TCP의 head-of-line blocking 제거 + UDP보다 효율적인 패킷 손실 복구 제공
- DNS over QUIC(DoQ): RFC 7858의 DNS over TLS(DoT)와 유사한 프라이버시 속성, 기존 DNS over UDP와 유사한 지연 특성
- DNS를 위한 범용 전송으로서 DoQ 사용 기술 → stub-recursive · recursive-authoritative · 영역 전송 시나리오 포함

## 이 메모의 상태

- Internet Standards Track 문서
- IETF의 산출물, IETF 커뮤니티의 합의 대표
- 공개 검토를 거쳐 IESG가 발행 승인
- Internet Standards에 대한 추가 정보 → RFC 7841 Section 2 참고
- 현재 상태 · 정오표 · 피드백 제공 방법: https://www.rfc-editor.org/info/rfc9250

## 저작권 고지

- Copyright (c) 2022 IETF Trust 및 문서 저자로 식별된 사람들. 모든 권리 보유
- BCP 78과 발행일 기준 유효한 IETF Trust의 IETF 문서 관련 법적 조항(https://trustee.ietf.org/license-info) 적용 → 권리와 제한 확인을 위한 검토 필요
- 문서에서 추출된 코드 구성요소는 Trust Legal Provisions Section 4.e 명시대로 Revised BSD License 텍스트 포함 필요, Revised BSD License 명시대로 보증 없이 제공

## 목차

```
1.  서론
2.  핵심 용어
3.  설계 고려사항
  3.1.  DNS 프라이버시 제공
  3.2.  최소 지연을 위한 설계
  3.3.  중간 장치 고려사항
  3.4.  서버 시작 트랜잭션 없음
4.  명세
  4.1.  연결 설정
    4.1.1.  포트 선택
  4.2.  스트림 매핑 및 사용
    4.2.1.  DNS Message ID
  4.3.  DoQ 오류 코드
    4.3.1.  트랜잭션 취소
    4.3.2.  트랜잭션 오류
    4.3.3.  프로토콜 오류
    4.3.4.  대체 오류 코드
  4.4.  연결 관리
  4.5.  세션 재개 및 0-RTT
  4.6.  메시지 크기
5.  구현 요구사항
  5.1.  인증
  5.2.  연결 실패 시 다른 프로토콜로의 폴백
  5.3.  주소 검증
  5.4.  패딩
  5.5.  연결 처리
    5.5.1.  연결 재사용
    5.5.2.  리소스 관리
    5.5.3.  0-RTT 및 세션 재개 사용
    5.5.4.  프라이버시를 위한 연결 마이그레이션 제어
  5.6.  쿼리 병렬 처리
  5.7.  영역 전송
  5.8.  흐름 제어 메커니즘
6.  보안 고려사항
7.  프라이버시 고려사항
  7.1.  0-RTT 데이터의 프라이버시 문제
  7.2.  세션 재개의 프라이버시 문제
  7.3.  주소 검증 토큰의 프라이버시 문제
  7.4.  장기 세션의 프라이버시 문제
  7.5.  트래픽 분석
8.  IANA 고려사항
  8.1.  DoQ 식별 문자열 등록
  8.2.  전용 포트 예약
  8.3.  Extended DNS Error Code: Too Early 예약
  8.4.  DNS-over-QUIC Error Codes 레지스트리
9.  참고문헌
  9.1.  규범적 참고문헌
  9.2.  참고적 참고문헌
부록 A.  NOTIFY 서비스
저자 주소
```

## 1. 서론

- DNS 개념: "Domain names - concepts and facilities" [RFC1034]에 명시
- UDP · TCP를 통한 DNS 쿼리 및 응답 전송: "Domain names - implementation and specification" [RFC1035]에 명시
- 이 문서: QUIC 전송 [RFC9000] [RFC9001]을 통한 DNS 프로토콜 매핑 제시
- DNS over QUIC → "DNS Terminology" [DNS-TERMS]에 따라 DoQ로 지칭

DoQ 매핑의 목표:

1. DoT [RFC7858]와 동일한 DNS 프라이버시 보호 제공
   - "Usage Profiles for DNS over TLS and DNS over DTLS" [RFC8310]대로 인증 도메인 이름을 통한 클라이언트의 서버 인증 옵션 포함
2. 기존 DNS over UDP 대비 향상된 수준의 출발지 주소 검증 제공
3. 전송 가능한 DNS 응답 크기에 경로 MTU 제한을 부과하지 않는 전송 제공

목표 달성과 DNS 암호화 진행 작업 지원을 위한 이 문서의 범위:

- "stub에서 recursive resolver로"의 시나리오 ("stub에서 recursive로")
- "recursive resolver에서 authoritative nameserver로"의 시나리오 ("recursive에서 authoritative로")
- "nameserver에서 nameserver로"의 시나리오 (주로 영역 전송(XFR) [RFC1995] [RFC5936]에 사용)

→ 이 문서는 DNS를 위한 범용 전송으로서 QUIC을 명시

이 문서의 구체적 비목표:

1. 중간 장치에 의한 DoQ 트래픽의 잠재적 차단 회피 시도 안 함
2. DNS Stateful Operations(DSO) [RFC8490]에서만 사용되는 서버 시작 트랜잭션 지원 시도 안 함

- QUIC을 통한 애플리케이션 전송 명시 시 필요 사항: 애플리케이션 메시지의 QUIC 스트림 매핑 방식 + 애플리케이션의 QUIC 사용 방식 정의
  - HTTP의 경우 "Hypertext Transfer Protocol Version 3 (HTTP/3)" [HTTP/3]에서 수행
- 이 문서의 목적: DNS 메시지가 QUIC을 통해 전송되는 방법 정의

- DNS over HTTPS(DoH) [RFC8484]: HTTP/3과 함께 사용 시 QUIC의 일부 이점 획득 가능
- DoQ를 위한 경량 직접 매핑 → 중개자가 거의 관여하지 않는 recursive-authoritative 시나리오와 영역 전송 시나리오 모두에 더 자연스러운 적합
  - 이런 시나리오에서는 HTTP의 추가 오버헤드가 HTTP 프록싱 · 캐싱 동작의 이점으로 상쇄되지 않음

- Section 3: 제안된 설계를 안내한 추론 제시
- Section 4: DoQ의 실제 매핑 명시
- Section 5: DoQ의 구현 · 사용 · 배포 지침 제시

## 2. 핵심 용어

- "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "NOT RECOMMENDED", "MAY", "OPTIONAL" 키워드: 모두 대문자로 나타나는 경우에만 BCP 14 [RFC2119] [RFC8174]에 기술된 대로 해석

## 3. 설계 고려사항

- 이 섹션과 하위 섹션: DoQ에 사용된 설계 지침 제시
- 이 문서의 다른 모든 섹션은 규범적, 이 섹션은 본질적으로 정보 제공적

### 3.1. DNS 프라이버시 제공

- DoT [RFC7858]: TLS를 통한 DNS 전송 명시 → "DNS Privacy Considerations" [RFC9076]에 기술된 문제 완화
- "Usage Profiles for DNS over TLS and DNS over DTLS" [RFC8310]: stub resolver 인증 메커니즘 포함한 DoT 사용 프로필 명시
- QUIC 연결 설정: "Using TLS to Secure QUIC" [RFC9001]에 따라 TLS를 사용한 보안 매개변수 협상 포함 → QUIC 전송의 암호화 가능
- QUIC을 통한 DNS 메시지 전송 → 사용 프로필 [RFC8310]을 포함해 DoT [RFC7858]와 본질적으로 동일한 프라이버시 보호 제공 예상
- 추가 논의: Section 7

### 3.2. 최소 지연을 위한 설계

QUIC의 프로토콜 지연 감소를 위한 설계 기능:

1. 세션 재개 중 0-RTT 데이터 지원
2. "QUIC Loss Detection and Congestion Control" [RFC9002]에 명시된 고급 패킷 손실 복구 절차 지원
3. 여러 스트림에서 데이터의 병렬 전달 허용 → head-of-line blocking 완화

DNS의 QUIC 매핑이 이 기능들을 활용하는 세 가지 방법:

1. 세션 재개 중 0-RTT 데이터 전송의 선택적 지원 (보안 · 프라이버시 영향은 이후 섹션에서 논의)
2. 여러 DNS 트랜잭션이 수행되는 장기 QUIC 연결 → 고급 복구 기능의 이점을 얻는 데 필요한 지속적 트래픽 생성
3. head-of-line blocking 완화를 위해 각 DNS 쿼리/응답 트랜잭션을 별도 스트림에 매핑
   - 서버가 쿼리에 순서와 관계없이 응답 가능
   - 클라이언트가 응답이 도착하는 대로 처리 가능(이전 응답의 순서대로 전달 대기 불필요)

- 이러한 고려사항은 Section 4.2의 DNS 트래픽 → QUIC 스트림 매핑에 반영

### 3.3. 중간 장치 고려사항

- QUIC 사용 시 암호화 · 패딩 · 트래픽 페이싱 · 트래픽 셰이핑 등 트래픽 분석 저항 기법으로 프로토콜의 목적을 경로상 장치로부터 위장 가능
- 이 명세는 그러한 분류 회피를 위한 조치는 미포함
  - Section 5.4의 패딩 메커니즘: DNS 쿼리 · 응답에 포함된 특정 레코드 난독화 용도, DNS 트래픽이라는 사실 자체의 난독화 용도 아님
- → 방화벽 등 중간 장치는 DoQ를 HTTP 등 QUIC을 사용하는 다른 프로토콜과 구별해 다른 처리 적용 가능

- 이 명세에서 프로토콜 분류 회피 조치의 부재는 그러한 관행의 지지를 의미하지 않음

### 3.4. 서버 시작 트랜잭션 없음

- Section 1에 명시된 대로 이 문서는 설정된 DoQ 연결 내 서버 시작 트랜잭션 지원을 명시하지 않음
  - 즉 DoQ 연결의 개시자만이 해당 연결을 통해 쿼리 전송 가능
- DSO: 기존 연결 내 서버 시작 트랜잭션 지원
  - 여기서 정의된 DoQ는 메시지의 순서대로 전달을 보장하지 않음 → DSO의 적용 가능한 전송 기준 미충족 ([RFC8490] Section 4.2 참조)

## 4. 명세

- DoQ 연결: QUIC 전송 명세 [RFC9000]에 기술된 대로 설정
- 연결 설정 중 DoQ 지원 표시: 암호화 핸드셰이크에서 Application-Layer Protocol Negotiation(ALPN) 토큰 "doq" 선택

### 4.1. 연결 설정

- DoQ 연결: QUIC 전송 명세 [RFC9000]에 기술된 대로 설정
- 연결 설정 중 DoQ 지원 표시: 암호화 핸드셰이크에서 Application-Layer Protocol Negotiation(ALPN) 토큰 "doq" 선택

#### 4.1.1. 포트 선택

- 기본적으로 DoQ를 지원하는 DNS 서버는 다른 포트 사용 상호 합의가 없는 한 전용 UDP 포트 853(Section 8)에서 QUIC 연결을 수신 대기하고 수락 필요(MUST)
- 기본적으로 특정 서버와 DoQ를 사용하려는 DNS 클라이언트는 다른 포트 사용 상호 합의가 없는 한 서버의 UDP 포트 853에 QUIC 연결 설정 필요(MUST)
- DoQ 연결은 UDP 포트 53 사용 금지(MUST NOT)
  - 이유: DoQ와 DNS over UDP [RFC1035] 사용 사이의 혼동 방지 → 두 당사자가 포트 53에 합의해도 그 합의를 모르는 다른 당사자가 여전히 해당 포트 사용을 시도할 수 있어 혼동 위험 존재
- stub-recursive 시나리오: 상호 합의된 대체 포트로서 포트 443 사용이 운영상 유익 가능
  - 이유: 포트 443은 QUIC 및 HTTP-3을 사용하는 많은 서비스에 사용되어 다른 포트보다 차단될 가능성이 적음
  - stub이 사용자 정의 포트 사용을 포함해 암호화된 전송을 제공하는 recursive를 발견하기 위한 여러 메커니즘은 진행 중인 작업의 주제

### 4.2. 스트림 매핑 및 사용

- QUIC 스트림을 통한 DNS 트래픽 매핑: QUIC 전송 명세 [RFC9000] Section 2에 상세 기술된 QUIC 스트림 기능 활용
- DNS 쿼리/응답 트래픽 [RFC1034] [RFC1035]: 클라이언트가 쿼리를 보내고 서버가 하나 이상의 응답을 제공(여러 응답은 영역 전송에서 발생 가능)하는 단순 패턴

매핑 요구사항:

- 클라이언트는 각 쿼리에 대해 별도 QUIC 스트림 선택 필요
  - 서버는 해당 쿼리에 대한 모든 응답 메시지 제공에 동일 스트림 사용
  - 여러 응답이 파싱될 수 있도록 DNS over TCP [RFC1035]에 정의된 것과 정확히 동일한 방식으로 2옥텟 길이 필드 사용
  - 결과: 각 QUIC 스트림의 내용 = 정확히 하나의 쿼리를 관리하는 TCP 연결의 내용과 동일

- DoQ 연결을 통해 전송되는 모든 DNS 메시지(쿼리 및 응답)는 [RFC1035]에 명시된 대로 2옥텟 길이 필드 다음 메시지 내용으로 인코딩 필요(MUST)

- 클라이언트는 QUIC 전송 명세 [RFC9000]에 부합해 QUIC 연결에서 각 후속 쿼리에 대해 다음으로 사용 가능한 클라이언트 시작 양방향 스트림 선택 필요(MUST)
  - 패킷 손실 등 네트워크 이벤트로 쿼리가 다른 순서로 도착 가능
  - 서버는 쿼리가 도착하는 대로 처리 필요(SHOULD) → 그렇지 않으면 불필요한 지연 발생

- 클라이언트는 선택한 스트림을 통해 DNS 쿼리 전송 필요(MUST), STREAM FIN 메커니즘으로 해당 스트림에서 더 이상 데이터가 전송되지 않음을 표시 필요(MUST)
- 서버는 동일 스트림에서 응답(들) 전송 필요(MUST), 마지막 응답 후 STREAM FIN 메커니즘으로 더 이상 데이터가 전송되지 않음을 표시 필요(MUST)

- → 단일 DNS 트랜잭션은 단일 양방향 클라이언트 시작 스트림을 소비
  - 클라이언트의 첫 번째 쿼리는 QUIC 스트림 0에서, 두 번째는 4에서, 이후 계속 발생 ([RFC9000] Section 2.1 참조)

- 서버는 클라이언트가 선택한 스트림에서 STREAM FIN이 표시될 때까지 쿼리 처리를 연기 가능(MAY)

- 서버와 클라이언트는 "매달려 있는(dangling)" 스트림의 수를 모니터링 가능(MAY)
  - dangling 스트림: 구현 정의 타임아웃 후 다음 이벤트가 발생하지 않은 열린 스트림
    - 예상되는 쿼리 또는 응답이 수신되지 않은 경우, 또는
    - 예상되는 쿼리 또는 응답이 수신되었지만 STREAM FIN이 수신되지 않은 경우

- 구현은 그러한 dangling 스트림 수에 제한 부과 가능(MAY) → 제한 도달 시 연결 종료 가능(MAY)

#### 4.2.1. DNS Message ID

- QUIC 연결을 통해 쿼리를 보낼 때 DNS Message ID는 0으로 설정 필요(MUST)
  - DoQ의 스트림 매핑이 쿼리와 응답의 모호하지 않은 상관관계를 허용 → Message ID 필드 불필요

영향:

- DoQ 메시지를 다른 전송으로/에서 프록싱하는 데 영향
  - 예: 프록시는 DoQ가 DNS over TCP보다 단일 연결에서 더 많은 미완료 쿼리를 지원할 수 있음을 관리해야 할 수 있음 (DoQ는 Message ID 공간에 의해 제한되지 않기 때문)
  - 이 문제는 Message ID를 0으로 설정하는 것이 권장되는 DoH에 이미 존재

- DoQ에서 다른 전송을 통해 DNS 메시지를 전달할 때: 사용 중인 프로토콜의 규칙에 따라 DNS Message ID 생성 필요(MUST)
- 다른 전송에서 DoQ를 통해 DNS 메시지를 전달할 때: Message ID는 0으로 설정 필요(MUST)

### 4.3. DoQ 오류 코드

다음 오류 코드는 스트림을 급작스럽게 종료할 때, 스트림 읽기를 중단할 때 애플리케이션 프로토콜 오류 코드로 사용하거나, 연결을 즉시 닫을 때 사용하기 위해 정의:

- DOQ_NO_ERROR (0x0): 오류 없음 → 연결이나 스트림을 닫아야 하지만 알릴 오류가 없을 때 사용
- DOQ_INTERNAL_ERROR (0x1): DoQ 구현이 내부 오류를 만나 트랜잭션이나 연결을 계속할 수 없음
- DOQ_PROTOCOL_ERROR (0x2): DoQ 구현이 프로토콜 오류를 만나 연결을 강제로 중단
- DOQ_REQUEST_CANCELLED (0x3): DoQ 클라이언트가 미완료 트랜잭션을 취소하고자 할 때 사용
- DOQ_EXCESSIVE_LOAD (0x4): DoQ 구현이 과도한 부하로 인해 연결을 닫을 때 사용
- DOQ_UNSPECIFIED_ERROR (0x5): DoQ 구현이 더 구체적인 오류 코드가 없을 때 사용
- DOQ_ERROR_RESERVED (0xd098ea5e): 테스트에 사용되는 대체 오류 코드

- 새로운 오류 코드 등록 세부사항: Section 8.4 참조

#### 4.3.1. 트랜잭션 취소

- QUIC에서 STOP_SENDING 전송 → 피어에게 스트림에서의 전송 중단 요청
- DoQ 클라이언트가 미완료 요청을 취소하고자 할 경우: QUIC STOP_SENDING 발행 필요(MUST), 오류 코드 DOQ_REQUEST_CANCELLED 사용 필요(SHOULD)
  - Section 8.4에 따라 등록된 더 구체적인 오류 코드 사용 가능(MAY)
  - STOP_SENDING 요청은 언제든지 전송 가능하나 서버 응답이 이미 전송된 경우 효과 없음 → 이 경우 클라이언트는 수신되는 응답을 단순히 폐기
  - 해당 DNS 트랜잭션은 포기 필요(MUST)

- STOP_SENDING을 수신한 서버: [RFC9000] Section 3.5에 따라 행동
  - 서버는 STOP_SENDING 수신 시 DNS 트랜잭션 처리 계속 금지(SHOULD NOT)

- 서버는 취소 요청의 총 수 또는 비율에 대한 구현 제한 부과 가능(MAY) → 제한 도달 시 연결 종료 가능(MAY)
  - 클라이언트 디버깅을 돕고자 하는 경우 오류 코드 DOQ_EXCESSIVE_LOAD 사용 가능(MAY)
  - 선의 클라이언트의 디버깅 지원과 서비스 거부 공격자의 서버 방어 테스트 허용 사이에는 항상 절충 존재 → 상황에 따라 서버는 다른 오류 코드 선택 가능

- 참고: 이 메커니즘은 보조 서버가 QUIC 연결을 닫지 않고도 주어진 스트림에서 발생하는 단일 영역 전송을 취소하는 방법 제공

- 서버는 클라이언트가 STREAM FIN을 표시하기 전에 클라이언트로부터 RESET_STREAM 요청을 수신한 경우 DNS 트랜잭션 처리 계속 금지(MUST NOT)
- 서버는 다음 경우를 제외하고 트랜잭션이 포기되었음을 표시하기 위해 RESET_STREAM 발행 필요(MUST):
  - 다른 이유로 이미 그렇게 한 경우, 또는
  - 이미 응답을 보내고 STREAM FIN을 표시한 경우

#### 4.3.2. 트랜잭션 오류

- 서버는 일반적으로 트랜잭션의 스트림에서 DNS 응답(또는 응답들)을 보냄으로써 트랜잭션 완료 (DNS 응답이 DNS 오류를 나타내는 경우 포함)
  - 예: 클라이언트는 Response Code가 SERVFAIL로 설정된 응답을 통해 Server Failure(SERVFAIL, [RFC1035])에 대해 알림받아야 함(SHOULD)

- 서버가 내부 오류로 DNS 응답을 보낼 수 없는 경우: QUIC RESET_STREAM 프레임 발행 필요(SHOULD)
  - 오류 코드는 DOQ_INTERNAL_ERROR로 설정 필요(SHOULD)
  - 해당 DNS 트랜잭션은 포기 필요(MUST)
  - 클라이언트는 연결을 닫기로 선택하기 전에 연결에서 수신되는 요청되지 않은 QUIC RESET_STREAM 프레임의 수 제한 가능(MAY)

- 참고: 이 메커니즘은 주 서버가 QUIC 연결을 닫지 않고도 주어진 스트림에서 발생하는 단일 영역 전송을 중단하는 방법 제공

#### 4.3.3. 프로토콜 오류

트랜잭션 중 잘못된 형식의, 불완전한, 또는 예상치 못한 메시지로 인해 발생 가능한 오류 시나리오 (다음 포함, 국한되지 않음):

- 클라이언트 또는 서버가 0이 아닌 Message ID를 가진 메시지를 수신한 경우
- 클라이언트 또는 서버가 2옥텟 길이 필드에 표시된 메시지의 모든 바이트를 수신하기 전에 STREAM FIN을 수신한 경우
- 클라이언트가 예상되는 모든 응답을 수신하기 전에 STREAM FIN을 수신한 경우
- 서버가 하나의 스트림에서 둘 이상의 쿼리를 수신한 경우
- 클라이언트가 스트림에서 예상과 다른 수의 응답을 수신한 경우 (예: A 레코드에 대한 쿼리에 대해 여러 응답)
- 클라이언트가 STOP_SENDING 요청을 수신한 경우
- 클라이언트 또는 서버가 요청 또는 응답을 보낸 후 예상되는 STREAM FIN을 표시하지 않은 경우 (Section 4.2 참조)
- 구현이 edns-tcp-keepalive EDNS(0) Option [RFC7828]을 포함하는 메시지를 수신한 경우 (Section 5.5.2 참조)
- 클라이언트 또는 서버가 단방향 QUIC 스트림을 열려고 시도한 경우
- 서버가 서버 시작 양방향 QUIC 스트림을 열려고 시도한 경우
- 서버가 0-RTT 데이터에서 "재생 가능한" 트랜잭션을 수신한 경우 (이 경우를 처리하지 않으려는 서버는 Section 4.5 참조)

- 피어가 그러한 오류 조건을 만나면 치명적 오류로 간주
  - QUIC의 CONNECTION_CLOSE 메커니즘으로 연결 강제 중단 필요(SHOULD), DoQ 오류 코드 DOQ_PROTOCOL_ERROR 사용 필요(SHOULD)
  - 일부 경우 대신 연결을 조용히 포기 가능(MAY) → 로컬 리소스는 더 적게 사용하나 문제 있는 노드에서의 디버깅은 더 어려워짐

- 참고: 위 EDNS(0) 옵션 사용에 대한 제한은 TCP/DoT/DoH를 통한 메시지를 DoQ를 통해 프록싱하는 데 영향을 미침

#### 4.3.4. 대체 오류 코드

- 이 명세는 Section 4.3.1, 4.3.2, 4.3.3에서 구체적인 오류 코드 기술 → 실패 및 기타 사고의 조사 용이화 목적
- 새로운 오류 코드는 DoQ의 향후 버전에서 정의되거나 Section 8.4에 명시된 대로 등록 가능

- 새로운 오류 코드는 협상 없이 정의될 수 있음 → 예상치 못한 컨텍스트에서 오류 코드를 사용하거나 알 수 없는 오류 코드를 수신하는 것은 DOQ_UNSPECIFIED_ERROR와 동등하게 처리 필요(MUST)

- 구현은 이 문서에 나열되지 않은 오류 코드를 사용해 오류 코드 확장 메커니즘 지원을 테스트하고자 할 수 있음(MAY), DOQ_ERROR_RESERVED 사용 가능(MAY)

### 4.4. 연결 관리

QUIC 전송 명세 [RFC9000] Section 10: 연결이 닫힐 수 있는 세 가지 방법

- 유휴 타임아웃
- 즉시 닫기
- 상태 비저장 리셋

- DoQ를 구현하는 클라이언트와 서버는 유휴 타임아웃의 사용을 협상 필요(SHOULD)
  - 유휴 타임아웃에 의한 닫기: 패킷 교환 없이 수행 → 프로토콜 오버헤드 최소화
  - QUIC 전송 명세 [RFC9000] Section 10.1에 따라 유휴 타임아웃의 유효값 = 두 엔드포인트가 광고한 값의 최솟값
  - 유휴 타임아웃 설정의 실제적 고려사항: Section 5.5.2에서 논의

- 클라이언트는 서버로부터 마지막 패킷이 수신된 이후 경과한 시간(연결의 유휴 시간)을 모니터링 필요(SHOULD)
  - 새로운 DNS 쿼리를 보내려고 준비할 때 유휴 시간이 유휴 타이머보다 충분히 낮은지 확인 필요(SHOULD)
    - 낮은 경우: 기존 연결을 통해 DNS 쿼리를 보내야 함(SHOULD)
    - 낮지 않은 경우: 새로운 연결을 설정하고 그 연결을 통해 쿼리를 보내야 함(SHOULD)

- 클라이언트는 유휴 타임아웃이 만료되기 전에 서버와의 연결을 폐기 가능(MAY)
  - 미완료 쿼리가 있는 클라이언트는 QUIC의 CONNECTION_CLOSE 메커니즘과 DoQ 오류 코드 DOQ_NO_ERROR를 사용해 명시적으로 연결을 닫아야 함(SHOULD)

- 클라이언트와 서버는 QUIC의 CONNECTION_CLOSE로 표시되는 다양한 다른 이유로 연결 종료 가능(MAY)
  - 피어가 폐기한 연결을 통해 패킷을 보내는 클라이언트와 서버는 상태 비저장 리셋 표시를 받을 수 있음
  - 연결이 실패하면 해당 연결에서 진행 중인 모든 트랜잭션은 포기 필요(MUST)

### 4.5. 세션 재개 및 0-RTT

- 서버가 지원하는 경우 클라이언트는 QUIC 전송 [RFC9000] 및 QUIC TLS [RFC9001]에 의해 지원되는 세션 재개 및 0-RTT 메커니즘 활용 가능(MAY)
  - 사용 결정 전 세션 재개와 관련된 잠재적 프라이버시 문제를 고려하고, 특히 이 문서의 다양한 섹션에 제시된 절충을 평가 필요(SHOULD)
  - 프라이버시 문제 상세: Section 7.1과 7.2, 구현 고려사항: Section 5.5.3

- 0-RTT 메커니즘은 "재생 가능한" 트랜잭션이 아닌 DNS 요청을 보내는 데 사용 금지(MUST NOT)
  - 이 명세에서 재생 가능한 트랜잭션: OPCODE가 QUERY 또는 NOTIFY인 경우만
  - 따라서 다른 OPCODE는 0-RTT 데이터로 전송 금지(MUST NOT)
  - NOTIFY가 여기 포함된 이유의 상세 논의: 부록 A 참조

- 서버는 세션 재개 지원 가능(MAY), [RFC9001] Section 4.6.1에 기술된 메커니즘을 사용해 0-RTT 지원 여부와 관계없이 그렇게 할 수 있음(MAY)
  - 0-RTT를 지원하는 서버는 0-RTT 데이터에서 수신된 재생 불가능한 트랜잭션을 즉시 처리 금지(MUST NOT), 대신 다음 동작 중 하나를 채택 필요(MUST):
    - 문제가 되는 트랜잭션을 대기열에 넣고 [RFC9001] Section 4.1.1에 정의된 대로 QUIC 핸드셰이크가 완료된 후에만 처리
    - 문제가 되는 트랜잭션에 [RFC6891]에 정의된 확장 RCODE 메커니즘과 [RFC8914]에 정의된 확장 DNS 오류를 사용해 응답 코드 REFUSED와 Extended DNS Error Code(EDE) "Too Early"로 응답 (Section 8.3 참조)
    - 오류 코드 DOQ_PROTOCOL_ERROR로 연결을 닫음

### 4.6. 메시지 크기

- DoQ 쿼리 및 응답: 이론적으로 최대 2^62 바이트를 운반할 수 있는 QUIC 스트림에서 전송
- 그러나 DNS 메시지는 실제로 최대 65535 바이트로 제한
  - 근거: DNS over TCP [RFC1035] 및 DoT [RFC7858]에서의 2옥텟 메시지 길이 필드 사용, DoH [RFC8484]를 위한 "application/dns-message"의 정의
  - DoQ는 동일한 제한 적용

- Extension Mechanisms for DNS(EDNS(0)) [RFC6891]: 피어가 UDP 메시지 크기를 지정할 수 있도록 함
  - 이 매개변수는 DoQ에 의해 무시됨
  - DoQ 구현은 항상 최대 메시지 크기가 65535 바이트라고 가정

## 5. 구현 요구사항

### 5.1. 인증

- stub-recursive 시나리오: 인증 요구사항은 DoT [RFC7858] 및 "Usage Profiles for DNS over TLS and DNS over DTLS" [RFC8310]에 기술된 것과 동일
  - [RFC8932]: DNS 프라이버시 서비스는 클라이언트가 서버를 인증하는 데 사용할 수 있는 자격 증명을 제공해야 함(SHOULD)
  - → DoH의 인증 모델과 일치시키기 위해 DoQ stub은 Strict 사용 프로필 사용 필요(SHOULD)
  - 암호화된 stub-recursive 시나리오에 대한 클라이언트 인증은 어떤 DNS RFC에도 기술되어 있지 않음

- 영역 전송의 경우: 인증 요구사항은 [RFC9103]에 기술된 것과 동일

- recursive-authoritative 시나리오의 경우: 인증 요구사항은 작성 시점에 명시되지 않았으며 DPRIVE WG에서 진행 중인 작업의 주제

### 5.2. 연결 실패 시 다른 프로토콜로의 폴백

- DoQ 연결의 설정이 실패하면: 클라이언트는 사용 프로필에 따라 DoT [RFC7858] 및 "Usage Profiles for DNS over TLS and DNS over DTLS" [RFC8310]에 명시된 대로 DoT로 폴백하고 잠재적으로 평문으로 폴백 시도 가능(MAY)

- DNS 클라이언트는 DoQ를 지원하지 않는 서버 IP 주소를 기억 필요(SHOULD)
  - 모바일 클라이언트는 주어진 IP 주소에 의한 DoQ 지원 부재를 컨텍스트별로(예: 네트워크 또는 프로비저닝 도메인별로) 기억 가능

- 타임아웃, 연결 거부, QUIC 핸드셰이크 실패: 서버가 DoQ를 지원하지 않음을 나타내는 지표
  - 클라이언트는 합리적인 기간(예: 서버당 1시간) 동안 DoQ를 지원하지 않는 서버에 DoQ 쿼리를 시도 금지(SHOULD NOT)
  - 대역 외 키 고정 사용 프로필 [RFC7858]을 따르는 DNS 클라이언트는 DoQ 연결 실패 후 재시도에 대해 더 공격적일 수 있음(MAY)

### 5.3. 주소 검증

- QUIC 전송 명세 [RFC9000] Section 8: 서버가 주소 증폭 공격에 사용되는 것을 방지하기 위한 주소 검증 절차 정의
  - DoQ 구현은 이 명세를 준수 필요(MUST) → 최악의 경우 증폭을 3배로 제한

- DoQ 구현은 QUIC 전송 명세 [RFC9000] Section 8.1.2에 정의된 Retry Packets를 사용한 주소 검증 절차를 사용하도록 서버를 구성하는 것을 고려 필요(SHOULD)
  - 이 절차: DNS Cookies 메커니즘 [RFC7873]과 유사하게 클라이언트의 출발지 주소 반환 경로 확인을 위해 1-RTT 지연 부과

- Retry Packets를 사용한 주소 검증을 구성하는 DoQ 구현은 QUIC 전송 명세 [RFC9000] Section 8.1.3에 정의된 향후 연결을 위한 주소 검증 절차 구현 필요(SHOULD)
  - 목적: 클라이언트 주소가 검증된 후 서버가 클라이언트에게 NEW_TOKEN 프레임을 보낼 수 있도록 함 → 동일 주소에서의 후속 연결 중 1-RTT 패널티 회피

### 5.4. 패딩

- 구현은 패딩의 신중한 삽입을 통해 Section 7.5에 기술된 트래픽 분석 공격으로부터 보호 필요(MUST)
  - 방법: EDNS(0) Padding Option [RFC7830]을 사용해 개별 DNS 메시지를 패딩, 또는 QUIC 패킷을 패딩([RFC9000] Section 19.1 참조)

- QUIC 패킷 수준에서의 패딩: 동등한 보호에 대해 이론적으로 더 나은 성능 가능
  - 이유: 패딩의 양이 확인 응답이나 흐름 제어 업데이트 같은 비DNS 프레임을 고려할 수 있고, QUIC 패킷이 여러 DNS 메시지를 운반할 수 있기 때문
  - 다만 애플리케이션은 QUIC의 구현이 적절한 API를 노출하는 경우에만 QUIC 패킷의 패딩 양을 제어 가능

권고:

- QUIC의 구현이 패딩 정책을 설정하는 API를 노출하는 경우: DoQ는 패킷 길이를 소수의 고정 크기 집합에 맞추기 위해 해당 API를 사용 필요(SHOULD)
- QUIC 패킷 수준에서의 패딩이 불가능하거나 사용되지 않는 경우: DoQ는 [RFC7830]에 명시된 EDNS(0) padding 확장을 사용해 모든 DNS 쿼리 및 응답이 소수의 고정 크기 집합으로 패딩되도록 보장 필요(MUST)

- 구현은 다른 암호화된 전송에 적용되는 기존 DNS 메시지 패딩 로직을 재사용하는 것이 상당히 더 간단한 경우 패딩을 위한 QUIC API를 사용하지 않기로 선택 가능

- 패딩 크기에 대한 표준 정책이 없는 경우: 구현은 Experimental 상태의 "Padding Policies for Extension Mechanisms for DNS (EDNS(0))" [RFC8467]의 권고를 따라야 함(SHOULD)
  - Experimental이지만, 이 권고는 DoT에 대해 구현되고 배포되어 있으며 구현이 이 명세를 완전히 준수하는 방법을 제공하므로 참조

### 5.5. 연결 처리

- "DNS Transport over TCP - Implementation Requirements" [RFC7766]: DNS over TCP에 대한 갱신된 지침 제공, 그 중 일부는 DoQ에 적용 가능
- 이 섹션: DoQ에 대한 연결 처리에 대해 유사한 조언 제공

#### 5.5.1. 연결 재사용

- DNS 클라이언트의 역사적 구현: 각 DNS 쿼리마다 TCP 연결을 열고 닫음
- 연결 설정 비용 분산을 위해: 클라이언트와 서버 모두 단일 지속 QUIC 연결을 통해 여러 쿼리와 응답을 보내는 연결 재사용 지원 필요(SHOULD)

- UDP와 동등한 성능 달성을 위해: DNS 클라이언트는 QUIC 연결의 QUIC 스트림을 통해 쿼리를 동시에 전송 필요(SHOULD)
  - 즉 여러 쿼리를 보낼 때 다음 쿼리를 보내기 전에 미완료 응답을 기다리는 것 금지(SHOULD NOT)

#### 5.5.2. 리소스 관리

- 설정된 연결과 유휴 연결의 적절한 관리는 DNS 서버의 건전한 운영에 중요

- DoQ의 구현은 DNS over TCP [RFC7766]에 명시된 것과 유사한 모범 사례를 따라야 함(SHOULD), 특히 다음과 관련해:
  - 동시 연결 ([RFC7766] Section 6.2.2, [RFC9103] Section 6.4에 의해 갱신)
  - 보안 고려사항 ([RFC7766] Section 10)
  - 미준수 시 리소스 고갈과 서비스 거부로 이어질 수 있음

- 장기 DoQ 연결을 유지하고자 하는 클라이언트는 QUIC 전송 명세 [RFC9000] Section 10.1에 정의된 유휴 타임아웃 메커니즘 사용 필요(SHOULD)
- 클라이언트와 서버는 DoQ 연결에서 전송되는 어떤 메시지에서도 edns-tcp-keepalive EDNS(0) Option [RFC7828]을 보내는 것 금지(MUST NOT) (전송으로서 TCP/TLS 사용에 특화된 옵션이기 때문)

- 이 문서는 유휴 연결에 대한 타임아웃 값에 구체적인 권고를 하지 않음
  - 클라이언트와 서버는 사용 가능한 리소스의 수준에 따라 연결을 재사용하거나 닫아야 함
  - 타임아웃은 활동이 적은 기간에는 더 길고 활동이 많은 기간에는 더 짧을 수 있음

#### 5.5.3. 0-RTT 및 세션 재개 사용

- DoQ에 0-RTT를 사용하는 것: 여러 매력적인 이점
  - 클라이언트는 연결 지연 없이 연결을 설정하고 쿼리를 보낼 수 있음
  - → 서버는 연결 타이머의 낮은 값을 협상 가능 → 관리해야 하는 총 연결 수 감소 (0-RTT를 사용하는 클라이언트는 새 연결이 필요한 경우에도 지연 패널티를 받지 않기 때문)

- 세션 재개 및 0-RTT 데이터 전송: Section 7.1과 7.2에 상세 기술된 프라이버시 위험 생성
  - 다음 권고: Section 4.5에 명시된 제한에 따라 0-RTT 데이터의 성능 이점을 누리면서 프라이버시 위험을 줄이기 위한 것

- 클라이언트는 [RFC8446] Appendix C.4에 명시된 대로 재개 티켓을 한 번만 사용 필요(SHOULD)
  - 기본적으로 클라이언트의 연결 상태가 변경된 경우 세션 재개 사용 금지(SHOULD NOT)

- 클라이언트는 NEW_TOKEN 메커니즘을 사용해 서버로부터 주소 검증 토큰을 받을 수 있음 ([RFC9000] Section 8 참조)
  - 관련된 추적 위험: Section 7.3
  - 클라이언트는 추가적인 추적 위험을 피하기 위해 세션 재개도 사용할 때만 주소 검증 토큰 사용 필요(SHOULD)

- 서버는 충분히 긴 수명(예: 6시간)의 세션 재개 티켓을 발행 필요(SHOULD)
  - 목적: 클라이언트가 연결을 살려두거나 세션 재개 티켓을 갱신하기 위해 서버를 자주 폴링하려는 유혹을 받지 않도록 함
  - 서버는 [RFC8446] Section 8에 명시된 안티 리플레이 메커니즘 구현 필요(SHOULD)

#### 5.5.4. 프라이버시를 위한 연결 마이그레이션 제어

- DoQ 구현은 [RFC9000] Section 9에 정의된 연결 마이그레이션 기능의 사용을 고려 가능
  - 이 기능: 클라이언트의 연결 상태가 변경되어도 연결이 계속 작동할 수 있게 함
  - Section 7.4에 상세 기술된 대로 이 기능은 프라이버시를 지연과 교환

- 기본적으로 클라이언트는 연결 상태가 변경되면 프라이버시를 우선시하고 새 세션을 시작하도록 구성 필요(SHOULD)

### 5.6. 쿼리 병렬 처리

- "DNS Transport over TCP - Implementation Requirements" [RFC7766] Section 7에 명시된 대로 resolver는 응답의 준비를 병렬로 지원하고 순서와 관계없이 보내는 것이 권장됨(RECOMMENDED)
- DoQ에서의 수행 방식: 이전에 열린 스트림에 대한 응답의 가용성을 기다리지 않고 가능한 한 빨리 해당 특정 스트림에서 응답을 전송

### 5.7. 영역 전송

- [RFC9103]: TLS를 통한 영역 전송(XoT) 명시, [RFC1995](IXFR) · [RFC5936](AXFR) · [RFC7766]에 대한 갱신 포함
  - 거기 기술된 XoT 연결 재사용 관련 고려사항은 DoQ 연결을 사용하는 영역 전송에도 유사하게 적용
  - 재진술 이유: 오늘날 기존 TCP/TLS 영역 전송 구현에서 효과적인 연결 재사용이 부족하기 때문

다음 권고 적용:

- DoQ 서버는 단일 QUIC 연결에서 여러 동시 IXFR 요청을 처리 필요(MUST)
- DoQ 서버는 단일 QUIC 연결에서 여러 동시 AXFR 요청을 처리 필요(MUST)
- DoQ 구현은 다음을 수행 필요(SHOULD):
  - 동일한 주 서버에 대한 AXFR 및 IXFR 요청 모두에 동일한 QUIC 연결 사용
  - 대기열에 들어오는 대로 해당 요청을 병렬로 전송, 즉 연결에서 다음 쿼리를 보내기 전에 응답을 기다리지 않음 (TCP/TLS 연결에서 요청을 파이프라이닝하는 것과 유사)
  - 각 요청에 대한 응답(들)을 사용 가능해지는 대로 전송, 즉 응답 스트림이 서로 섞여서 전송될 수 있음(MAY)

### 5.8. 흐름 제어 메커니즘

- 서버와 클라이언트는 [RFC9000] Section 4에 정의된 메커니즘을 사용해 흐름 제어 관리
  - 이 메커니즘: 클라이언트와 서버가 생성할 수 있는 스트림의 수, 스트림에서 보낼 수 있는 데이터의 양, 모든 스트림의 합집합에서 보낼 수 있는 데이터의 양을 지정할 수 있게 함
  - DoQ의 경우: 생성되는 스트림의 수를 제어하면 서버가 클라이언트가 주어진 연결에서 보낼 수 있는 새로운 요청의 수를 제어 가능

- 흐름 제어의 목적: 엔드포인트 리소스 보호
  - 서버의 경우: 전역 및 스트림별 흐름 제어 한계가 클라이언트가 보낼 수 있는 데이터의 양을 제어
  - 동일 메커니즘: 클라이언트가 서버가 보낼 수 있는 데이터의 양을 제어 가능
  - 너무 작은 값은 불필요하게 성능을 제한, 너무 큰 값은 엔드포인트를 과부하나 메모리 고갈에 노출시킬 수 있음
  - 구현 또는 배포는 이 균형을 맞추기 위해 흐름 제어 한계 조정 필요
  - 특히 영역 전송 구현은 대규모 및 동시 영역 전송이 잘 관리되도록 이 한계를 주의 깊게 제어 필요

- 매개변수의 초기 값: 연결 시작 시 클라이언트와 서버가 보낼 수 있는 요청의 수와 데이터의 양을 제어
  - 이 값은 연결 핸드셰이크 중에 교환되는 전송 매개변수에 명시
  - 초기 연결에서 수신된 매개변수 값은 재개된 연결에서 클라이언트가 0-RTT 데이터를 사용해 보낼 수 있는 요청의 수와 데이터의 양도 제어
  - 이러한 초기 매개변수의 너무 작은 값을 사용하면 0-RTT 데이터 허용의 유용성이 제한됨

## 6. 보안 고려사항

- Domain Name System의 위협 분석: [RFC3833]에 있음
  - DoT, DoH, DoQ의 개발 이전에 작성 → 갱신이 필요할 것으로 보임

- DoQ의 보안 고려사항: DoT [RFC7858]의 것과 비교할 만해야 함
  - [RFC7858]에 명시된 DoT는 stub-recursive 시나리오만 다루지만, 중간자 공격 · 중간 장치 · 평문 연결의 데이터 캐싱에 대한 고려사항은 resolver-authoritative server 시나리오에서도 DoQ에 적용
  - Section 5.1에 명시된 대로 DoQ를 사용한 영역 전송 보안을 위한 인증 요구사항은 DoT를 통한 영역 전송에 대한 것과 동일 → 일반적인 보안 고려사항은 [RFC9103]에 기술된 것과 전적으로 유사

- DoQ는 그 자체가 TLS 1.3에 의존하는 QUIC에 의존 → [BCP195]에 기술된 다운그레이드 공격에 대한 보호를 기본적으로 지원
  - QUIC 특유의 문제와 완화: [RFC9000] Section 21에 기술

## 7. 프라이버시 고려사항

- "DNS Privacy Considerations" [RFC9076]에 제공된 암호화된 전송에 대한 일반적 고려사항은 DoQ에 적용
  - 거기 제공된 구체적 고려사항은 DoT와 DoQ 사이에 차이가 없어 여기서 재논의하지 않음
  - "Recommendations for DNS Privacy Service Operators" [RFC8932] (DNS 프라이버시 서비스의 운영 · 정책 · 보안 고려사항)도 DoQ 서비스에 적용 가능

- QUIC은 TLS 1.3 [RFC8446]의 메커니즘을 통합 → QUIC의 "0-RTT" 데이터 전송 가능
  - 흥미로운 지연 이득을 제공할 수 있으나 두 가지 우려 제기:
    1. 공격자가 0-RTT 데이터를 재생 → 수신 서버의 동작에서 그 내용 추론 가능
    2. 0-RTT 메커니즘은 연속적인 클라이언트 세션 간의 연결 가능성을 제공할 수 있는 TLS 세션 재개에 의존

- 이러한 문제는 Section 7.1과 7.2에서 전개

### 7.1. 0-RTT 데이터의 프라이버시 문제

- 0-RTT 데이터는 공격자에 의해 재생 가능
  - 해당 데이터는 recursive resolver에 의한 authoritative resolver로의 쿼리 트리거 가능
  - 공격자는 recursive resolver의 발신 트래픽이 관찰 가능한 시간을 선택해 0-RTT 데이터에서 어떤 이름이 쿼리되었는지 알아낼 수 있음

- 이 위험: "DNS Privacy Considerations" [RFC9076]에서 논의된 recursive resolver의 동작을 관찰하는 일반적 문제의 부분집합
  - 공격은 이 트래픽의 관찰 가능성을 줄임으로써 부분적으로 완화
  - TLS 1.3 [RFC8446]의 필수적 리플레이 보호 메커니즘: 리플레이의 위험을 제한하나 제거하지는 않음 → 0-RTT 패킷은 클럭 스큐와 네트워크 전송의 변동을 고려한 좁은 창 내에서만 재생 가능

- TLS 1.3 [RFC8446]에 대한 권고: 0-RTT 데이터를 사용하는 기능은 기본적으로 꺼져 있어야 하고 사용자가 관련 위험을 명확히 이해하는 경우에만 활성화 필요
  - DoQ의 경우: 0-RTT 데이터를 허용하는 것은 상당한 성능 향상을 제공 → 사용하지 말라는 권고는 단순히 무시될 것이라는 우려
  - 대신 Section 4.5와 5.5.3에서 일련의 실용적 권고 제공

- Section 4.5의 명세: 서버의 장기 상태를 변경하지 않을 트랜잭션만 허용 → 리플레이 공격의 가장 명백한 위험 차단

- 위 공격: stub-recursive 시나리오에 적용, recursive-authoritative 시나리오에서도 유사한 공격을 구상할 수 있으며 동일한 완화 적용

### 7.2. 세션 재개의 프라이버시 문제

- QUIC 세션 재개 메커니즘: 세션 재설정 비용 감소, 0-RTT 데이터 가능화
  - 동일한 재개 토큰이 여러 번 사용되는 경우 연결 가능성 문제 존재
  - 클라이언트-서버 경로상 공격자는 토큰의 반복적 사용을 관찰해 시간에 걸쳐 또는 여러 위치에 걸쳐 클라이언트 추적 가능

- 세션 재개 메커니즘: 서버가 재개된 세션을 초기 세션과 상관시켜 클라이언트를 추적할 수 있게 함
  - 가상의 장기 세션 생성 → 해당 세션의 일련의 쿼리는 서버가 클라이언트를 식별하는 데 사용 가능
  - 클라이언트 주소가 일정하게 유지되면 서버는 이미 그렇게 할 수 있으나, 세션 재개 티켓은 클라이언트의 주소 변경 후에도 추적을 가능하게 함

- Section 5.5.3의 권고: 이러한 위험 완화 목적
  - 세션 티켓을 한 번만 사용 → 제3자에 의한 추적 위험 완화
  - 주소 변경 시 세션 재개 거부 → 서버에 의한 추적의 증분적 위험 완화 (IP 주소에 의한 추적 위험은 남아 있음)

- 프라이버시 절충은 컨텍스트에 따라 다름
  - stub resolver: 종종 위치를 변경 → 지연보다 프라이버시를 선호하는 강한 동기
  - 소수의 정적 IP 주소 집합을 사용하는 recursive resolver: 세션 재개가 제공하는 감소된 지연을 선호할 가능성이 더 높으며, 세션 간 IP 주소가 변경되더라도 재개 티켓을 사용하는 유효한 이유로 고려 가능

- 암호화된 영역 전송([RFC9103]): 전송에 관련된 당사자의 신원을 숨기려는 명시적 시도 없음, 특별히 지연에 민감하지도 않음
  - → 영역 전송을 지원하는 애플리케이션은 stub-recursive 애플리케이션과 동일한 보호를 적용하기로 결정 가능

### 7.3. 주소 검증 토큰의 프라이버시 문제

- QUIC: [RFC9000] Section 8에서 주소 검증 메커니즘 명시
  - 주소 검증 토큰 사용: QUIC 서버가 새 연결에 대해 추가 RTT를 피할 수 있게 함
  - 토큰은 일반적으로 IP 주소에 연결
  - QUIC 클라이언트는 일반적으로 이전에 사용한 주소에서 새 연결을 설정할 때만 토큰 사용
  - 다만 클라이언트가 새 주소를 사용하고 있다는 것을 항상 인식하는 것은 아님 (NAT, 또는 IP 주소 변경 확인 API 부재가 원인 - IPv6의 경우 매우 빈번할 수 있음)
  - → 클라이언트가 모르는 사이에 새 위치로 이동한 후 주소 검증 토큰을 잘못 사용하면 연결 가능성 위험 발생

- Section 5.5.3의 권고: NEW_TOKEN 사용을 세션 재개 사용에 연결해 이 위험 완화
  - 다만 클라이언트가 주소 변경을 인식하지 못하는 경우는 다루지 않음

### 7.4. 장기 세션의 프라이버시 문제

- 세션 재개의 잠재적 대안: 장기 세션 사용 (세션이 오랜 시간 열려 있으면 연결 설정 지연 없이 새 쿼리 전송 가능)
  - 두 솔루션은 유사한 프라이버시 특성
  - 세션 재개는 서버가 클라이언트의 IP 주소를 추적할 수 있게 할 수 있는데, 장기 세션도 동일한 효과

- DoQ 구현은 클라이언트의 연결 상태가 변경되어도 세션을 유지하기 위해 QUIC의 연결 마이그레이션 기능 활용 가능
  - 예: 클라이언트가 Wi-Fi 연결에서 셀룰러 네트워크 연결로, 그리고 다른 Wi-Fi 연결로 마이그레이션하는 경우
  - 서버는 장기 연결이 사용하는 IP 주소의 변천을 모니터링해 클라이언트 위치를 추적할 수 있음

- Section 5.5.4의 권고: 여러 클라이언트 주소를 사용하는 장기 세션과 관련된 프라이버시 우려 완화

### 7.5. 트래픽 분석

- QUIC 패킷이 암호화되어 있어도 공격자는 쿼리와 응답 모두에서 패킷 길이를 관찰하고 패킷 타이밍을 관찰해 정보 획득 가능
  - 많은 DNS 요청은 웹 브라우저에 의해 발생
  - 특정 웹 페이지를 로드하려면 수십 개의 DNS 이름 해석이 필요할 수 있음
  - 애플리케이션이 패킷당 하나의 쿼리 또는 응답, 즉 "패킷당 하나의 QUIC STREAM 프레임"의 단순한 매핑을 채택하면 패킷 길이의 연속이 요청된 사이트를 식별하기에 충분한 정보를 제공할 수 있음

- 구현은 이 공격을 완화하기 위해 Section 5.4에 정의된 메커니즘 사용 필요(SHOULD)

## 8. IANA 고려사항

### 8.1. DoQ 식별 문자열 등록

- 이 문서는 "TLS Application-Layer Protocol Negotiation (ALPN) Protocol IDs" 레지스트리 [RFC7301]에서 DoQ 식별을 위한 새로운 등록 생성

"doq" 문자열이 DoQ를 식별:

- Protocol: DoQ
- Identification Sequence: 0x64 0x6F 0x71 ("doq")
- Specification: 이 문서

### 8.2. 전용 포트 예약

- TCP와 UDP 모두에서 포트 853은 현재 "DNS query-response protocol run over TLS/DTLS" [RFC7858]를 위해 예약

- DNS over DTLS(DoD) [RFC8094]의 명세: 실험적, stub에서 resolver로 제한, 저자들이 아는 한 현재 어떤 구현이나 배포도 존재하지 않음 (명세 발행 후 몇 년이 지났음에도)

- 이 명세는 추가로 DoQ를 위한 UDP 포트 853의 사용 예약
  - QUIC 버전 1: DTLS를 포함해 동일한 포트에서 다른 프로토콜과 공존할 수 있도록 설계 ([RFC9000] Section 17.2 참조)
  - → 동일한 포트에서 DoD와 DoQ(QUIC 버전 1)를 제공하는 배포는 각 UDP 페이로드의 두 번째로 유의미한 비트로 둘을 역다중화 가능
  - 그러한 배포는 동일한 포트에서 DNS를 제공하기 위해 배포하기 전에 QUIC 및 DTLS의 향후 버전 또는 확장(예: [GREASING-QUIC])의 서명을 확인 필요

IANA는 System 범위의 "Service Name and Transport Protocol Port Number Registry"에서 다음 값을 갱신 (해당 범위의 레지스트리는 IETF Review 또는 IESG Approval [RFC6335] 요구):

- Service Name: domain-s
- Port Number: 853
- Transport Protocol(s): UDP
- Assignee: IESG
- Contact: IETF Chair
- Description: DNS query-response protocol run over DTLS or QUIC
- Reference: [RFC7858][RFC8094] 이 문서

- 추가로 IANA는 일관성과 명확성을 위해 TCP 포트 853 할당의 Description 필드를 "DNS query-response protocol run over TLS"로 갱신하고 TCP 할당의 Reference 필드에서 [RFC8094] 제거

### 8.3. Extended DNS Error Code: Too Early 예약

IANA는 "Extended DNS Error Codes" 레지스트리 [RFC8914]에 다음 값 등록:

- INFO-CODE: 26
- Purpose: Too Early
- Reference: 이 문서

### 8.4. DNS-over-QUIC Error Codes 레지스트리

- IANA는 "Domain Name System (DNS) Parameters" 웹 페이지에 "DNS-over-QUIC Error Codes"에 대한 레지스트리 추가

"DNS-over-QUIC Error Codes" 레지스트리는 62비트 공간을 관리, 서로 다른 정책에 의해 관리되는 세 영역으로 분할:

- 0x00에서 0x3f 사이의 값(16진수, 포함)에 대한 영구 등록: [RFC8126] Section 4.9 및 4.10에 정의된 Standards Action 또는 IESG Approval을 사용해 할당
- 0x3f보다 큰 값에 대한 영구 등록: Specification Required 정책 ([RFC8126])을 사용해 할당
- 0x3f보다 큰 값에 대한 임시 등록: [RFC8126] Section 4.5에 정의된 Expert Review 요구

- 임시 예약은 일부 영구 등록과 0x3f보다 큰 값의 범위를 공유
  - 목적: 배포된 시스템의 변경을 요구하지 않고 임시 등록을 영구 등록으로 전환할 수 있도록 의도적으로 설계 ([RFC9000] Section 22에 설정된 원칙에 부합)

이 레지스트리의 등록은 다음 필드를 포함 필요(MUST):

- Value: 할당된 코드포인트
- Status: "Permanent" 또는 "Provisional"
- Contact: 등록자에 대한 연락처 정보

또한 영구 등록은 다음을 포함 필요(MUST):

- Error: 매개변수에 대한 짧은 니모닉
- Specification: 해당 값에 대한 공개적으로 이용 가능한 명세에 대한 참조 (임시 등록의 경우 선택사항)
- Description: 오류 코드 의미론에 대한 간략한 설명, 명세 참조가 제공되는 경우 요약일 수 있음(MAY)

- 코드포인트의 임시 등록: DoQ에 대한 확장의 개인적 사용 및 실험을 허용하기 위한 것
  - 다만 임시 등록은 다른 목적을 위해 회수되고 재할당될 수 있음
  - 위에 나열된 매개변수 외에도 임시 등록은 다음을 포함 필요(MUST): Date - 등록의 마지막 업데이트 날짜
  - 임시 등록의 날짜를 갱신하는 요청은 지정된 전문가의 검토 없이 가능

이 레지스트리의 초기 내용 (모든 항목 공유 필드: Status - Permanent, Contact - DPRIVE WG, Specification - Section 4.3):

- 0x0: DOQ_NO_ERROR - No error
- 0x1: DOQ_INTERNAL_ERROR - Implementation error
- 0x2: DOQ_PROTOCOL_ERROR - Generic protocol violation
- 0x3: DOQ_REQUEST_CANCELLED - Request cancelled by client
- 0x4: DOQ_EXCESSIVE_LOAD - Closing a connection for excessive load
- 0x5: DOQ_UNSPECIFIED_ERROR - No error reason specified
- 0xd098ea5e: DOQ_ERROR_RESERVED - Alternative error code used for tests

## 9. 참고문헌

### 9.1. 규범적 참고문헌

```
[RFC1034]  Mockapetris, P., "Domain names - concepts and
           facilities", STD 13, RFC 1034, DOI 10.17487/RFC1034,
           November 1987,
           <https://www.rfc-editor.org/info/rfc1034>.

[RFC1035]  Mockapetris, P., "Domain names - implementation and
           specification", STD 13, RFC 1035,
           DOI 10.17487/RFC1035, November 1987,
           <https://www.rfc-editor.org/info/rfc1035>.

[RFC1995]  Ohta, M., "Incremental Zone Transfer in DNS", RFC 1995,
           DOI 10.17487/RFC1995, August 1996,
           <https://www.rfc-editor.org/info/rfc1995>.

[RFC2119]  Bradner, S., "Key words for use in RFCs to Indicate
           Requirement Levels", BCP 14, RFC 2119,
           DOI 10.17487/RFC2119, March 1997,
           <https://www.rfc-editor.org/info/rfc2119>.

[RFC5936]  Lewis, E. and A. Hoenes, Ed., "DNS Zone Transfer
           Protocol (AXFR)", RFC 5936, DOI 10.17487/RFC5936,
           June 2010,
           <https://www.rfc-editor.org/info/rfc5936>.

[RFC6891]  Damas, J., Graff, M., and P. Vixie, "Extension
           Mechanisms for DNS (EDNS(0))", STD 75, RFC 6891,
           DOI 10.17487/RFC6891, April 2013,
           <https://www.rfc-editor.org/info/rfc6891>.

[RFC7301]  Friedl, S., Popov, A., Langley, A., and E. Stephan,
           "Transport Layer Security (TLS) Application-Layer
           Protocol Negotiation Extension", RFC 7301,
           DOI 10.17487/RFC7301, July 2014,
           <https://www.rfc-editor.org/info/rfc7301>.

[RFC7766]  Dickinson, J., Dickinson, S., Bellis, R., Mankin, A.,
           and D. Wessels, "DNS Transport over TCP -
           Implementation Requirements", RFC 7766,
           DOI 10.17487/RFC7766, March 2016,
           <https://www.rfc-editor.org/info/rfc7766>.

[RFC7830]  Mayrhofer, A., "The EDNS(0) Padding Option", RFC 7830,
           DOI 10.17487/RFC7830, May 2016,
           <https://www.rfc-editor.org/info/rfc7830>.

[RFC7858]  Hu, Z., Zhu, L., Heidemann, J., Mankin, A., Wessels,
           D., and P. Hoffman, "Specification for DNS over
           Transport Layer Security (TLS)", RFC 7858,
           DOI 10.17487/RFC7858, May 2016,
           <https://www.rfc-editor.org/info/rfc7858>.

[RFC8126]  Cotton, M., Leiba, B., and T. Narten, "Guidelines for
           Writing an IANA Considerations Section in RFCs", BCP 26,
           RFC 8126, DOI 10.17487/RFC8126, June 2017,
           <https://www.rfc-editor.org/info/rfc8126>.

[RFC8174]  Leiba, B., "Ambiguity of Uppercase vs Lowercase in RFC
           2119 Key Words", BCP 14, RFC 8174,
           DOI 10.17487/RFC8174, May 2017,
           <https://www.rfc-editor.org/info/rfc8174>.

[RFC8310]  Dickinson, S., Gillmor, D., and T. Reddy, "Usage
           Profiles for DNS over TLS and DNS over DTLS", RFC 8310,
           DOI 10.17487/RFC8310, March 2018,
           <https://www.rfc-editor.org/info/rfc8310>.

[RFC8446]  Rescorla, E., "The Transport Layer Security (TLS)
           Protocol Version 1.3", RFC 8446,
           DOI 10.17487/RFC8446, August 2018,
           <https://www.rfc-editor.org/info/rfc8446>.

[RFC8467]  Mayrhofer, A., "Padding Policies for Extension
           Mechanisms for DNS (EDNS(0))", RFC 8467,
           DOI 10.17487/RFC8467, October 2018,
           <https://www.rfc-editor.org/info/rfc8467>.

[RFC8914]  Kumari, W., Hunt, E., Arends, R., Hardaker, W., and D.
           Lawrence, "Extended DNS Errors", RFC 8914,
           DOI 10.17487/RFC8914, October 2020,
           <https://www.rfc-editor.org/info/rfc8914>.

[RFC9000]  Iyengar, J., Ed. and M. Thomson, Ed., "QUIC: A
           UDP-Based Multiplexed and Secure Transport", RFC 9000,
           DOI 10.17487/RFC9000, May 2021,
           <https://www.rfc-editor.org/info/rfc9000>.

[RFC9001]  Thomson, M., Ed. and S. Turner, Ed., "Using TLS to
           Secure QUIC", RFC 9001, DOI 10.17487/RFC9001, May 2021,
           <https://www.rfc-editor.org/info/rfc9001>.

[RFC9103]  Toorop, W., Dickinson, S., Sahib, S., Aras, P., and A.
           Mankin, "DNS Zone Transfer over TLS", RFC 9103,
           DOI 10.17487/RFC9103, August 2021,
           <https://www.rfc-editor.org/info/rfc9103>.
```

### 9.2. 참고적 참고문헌

```
[BCP195]   Sheffer, Y., Holz, R., and P. Saint-Andre,
           "Recommendations for Secure Use of Transport Layer
           Security (TLS) and Datagram Transport Layer Security
           (DTLS)", BCP 195, RFC 7525, May 2015.

           Moriarty, K. and S. Farrell, "Deprecating TLS 1.0 and
           TLS 1.1", BCP 195, RFC 8996, March 2021.

           <https://www.rfc-editor.org/info/bcp195>

[DNS-TERMS]
           Hoffman, P. and K. Fujiwara, "DNS Terminology", Work in
           Progress, Internet-Draft,
           draft-ietf-dnsop-rfc8499bis-03, 28 September 2021,
           <https://datatracker.ietf.org/doc/html/
           draft-ietf-dnsop-rfc8499bis-03>.

[DNS0RTT]  Kahn Gillmor, D., "DNS + 0-RTT", Message to DNS-Privacy
           WG mailing list, 6 April 2016,
           <https://www.ietf.org/mail-archive/web/dns-privacy/
           current/msg01276.html>.

[GREASING-QUIC]
           Thomson, M., "Greasing the QUIC Bit", Work in Progress,
           Internet-Draft, draft-ietf-quic-bit-grease-02,
           10 November 2021,
           <https://datatracker.ietf.org/doc/html/draft-ietf-
           quic-bit-grease-02>.

[HTTP/3]   Bishop, M., Ed., "Hypertext Transfer Protocol Version 3
           (HTTP/3)", Work in Progress, Internet-Draft, draft-ietf-
           quic-http-34, 2 February 2021,
           <https://datatracker.ietf.org/doc/html/draft-ietf-quic-
           http-34>.

[RFC1996]  Vixie, P., "A Mechanism for Prompt Notification of Zone
           Changes (DNS NOTIFY)", RFC 1996, DOI 10.17487/RFC1996,
           August 1996,
           <https://www.rfc-editor.org/info/rfc1996>.

[RFC3833]  Atkins, D. and R. Austein, "Threat Analysis of the
           Domain Name System (DNS)", RFC 3833,
           DOI 10.17487/RFC3833, August 2004,
           <https://www.rfc-editor.org/info/rfc3833>.

[RFC6335]  Cotton, M., Eggert, L., Touch, J., Westerlund, M., and
           S. Cheshire, "Internet Assigned Numbers Authority (IANA)
           Procedures for the Management of the Service Name and
           Transport Protocol Port Number Registry", BCP 165,
           RFC 6335, DOI 10.17487/RFC6335, August 2011,
           <https://www.rfc-editor.org/info/rfc6335>.

[RFC7828]  Wouters, P., Abley, J., Dickinson, S., and R. Bellis,
           "The edns-tcp-keepalive EDNS0 Option", RFC 7828,
           DOI 10.17487/RFC7828, April 2016,
           <https://www.rfc-editor.org/info/rfc7828>.

[RFC7873]  Eastlake 3rd, D. and M. Andrews, "Domain Name System
           (DNS) Cookies", RFC 7873, DOI 10.17487/RFC7873, May
           2016, <https://www.rfc-editor.org/info/rfc7873>.

[RFC8094]  Reddy, T., Wing, D., and P. Patil, "DNS over Datagram
           Transport Layer Security (DTLS)", RFC 8094,
           DOI 10.17487/RFC8094, February 2017,
           <https://www.rfc-editor.org/info/rfc8094>.

[RFC8484]  Hoffman, P. and P. McManus, "DNS Queries over HTTPS
           (DoH)", RFC 8484, DOI 10.17487/RFC8484, October 2018,
           <https://www.rfc-editor.org/info/rfc8484>.

[RFC8490]  Bellis, R., Cheshire, S., Dickinson, J., Dickinson, S.,
           Lemon, T., and T. Pusateri, "DNS Stateful Operations",
           RFC 8490, DOI 10.17487/RFC8490, March 2019,
           <https://www.rfc-editor.org/info/rfc8490>.

[RFC8932]  Dickinson, S., Overeinder, B., van Rijswijk-Deij, R.,
           and A. Mankin, "Recommendations for DNS Privacy Service
           Operators", BCP 232, RFC 8932, DOI 10.17487/RFC8932,
           October 2020,
           <https://www.rfc-editor.org/info/rfc8932>.

[RFC9002]  Iyengar, J., Ed. and I. Swett, Ed., "QUIC Loss
           Detection and Congestion Control", RFC 9002,
           DOI 10.17487/RFC9002, May 2021,
           <https://www.rfc-editor.org/info/rfc9002>.

[RFC9076]  Wicinski, T., Ed., "DNS Privacy Considerations",
           RFC 9076, DOI 10.17487/RFC9076, July 2021,
           <https://www.rfc-editor.org/info/rfc9076>.
```

## 부록 A. NOTIFY 서비스

- 이 부록: 0-RTT 데이터로 NOTIFY([RFC1996] 참조)를 보내는 것이 수용 가능하다고 간주되는 이유 논의

- Section 4.5: "0-RTT 메커니즘은 '재생 가능한' 트랜잭션이 아닌 DNS 요청을 보내는 데 사용 금지(MUST NOT)"
  - 이 명세가 0-RTT 데이터로 NOTIFY 전송을 지원하는 이유: NOTIFY는 기술적으로 수신 서버의 상태를 변경하지만, NOTIFY를 재생하는 효과가 실제로는 무시할 만한 영향

- NOTIFY 메시지: 보조 서버에게 더 새로운 버전의 영역이 사용 가능하다는 근거로 SOA 쿼리 또는 XFR 요청을 주 서버에 보내도록 촉구
  - NOTIFY는 위조될 수 있으며, 이론적으로 보조 서버가 주 서버에 반복적으로 불필요한 요청을 보내도록 하는 데 사용될 수 있다는 것은 오래전부터 인정되어 온 사실
  - → 대부분의 구현은 하나 이상의 NOTIFY 수신에 의해 트리거되는 SOA/XFR 쿼리에 대한 어떤 형태의 제한(throttling)을 보유

- [RFC9103]: NOTIFY 및 SOA 쿼리와 관련된 프라이버시 위험 기술, 영역 전송의 암호화 범위 내에서 이러한 위험을 해결하는 것은 미포함
  - → NOTIFY에 DoQ를 사용하는 프라이버시 이점은 명확하지 않으나, 같은 이유로 0-RTT 데이터로 NOTIFY를 보내는 것은 평문 DNS를 사용해 보내는 것 이상의 프라이버시 위험을 가지지 않음

## 저자 주소

```
Christian Huitema
Private Octopus Inc.
427 Golfcourse Rd
Friday Harbor, WA 98250
United States of America
Email: huitema@huitema.net

Sara Dickinson
Sinodun IT
Oxford Science Park
Oxford
OX4 4GA
United Kingdom
Email: sara@sinodun.com

Allison Mankin
Salesforce
Email: allison.mankin@gmail.com
```
