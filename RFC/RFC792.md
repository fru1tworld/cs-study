# RFC 792 - Internet Control Message Protocol (ICMP)

## 문서 정보

- **RFC 번호**: 792
- **제목**: Internet Control Message Protocol
- **저자**: J. Postel (USC/Information Sciences Institute)
- **발행일**: 1981년 9월
- **목적**: IP 네트워크에서 제어 메시지와 오류 보고를 위한 프로토콜

---

## 1. 개요

### 1.1 ICMP란?

ICMP(Internet Control Message Protocol)는 IP 네트워크에서 게이트웨이(라우터)와 호스트 간에 제어 메시지를 전달하는 프로토콜입니다.

> "ICMP는 IP의 상위 계층 프로토콜처럼 작동하지만, 실제로는 IP의 필수 구성 요소이며 모든 IP 모듈에서 구현되어야 한다"

### 1.2 ICMP의 역할

- **오류 보고**: 데이터그램 전달 실패 시 송신자에게 알림
- **진단 도구**: ping, traceroute 등의 네트워크 진단 기능 제공
- **경로 최적화**: 더 좋은 경로가 있을 때 알림 (Redirect)
- **흐름 제어**: 네트워크 혼잡 시 송신 속도 조절 요청

### 1.3 ICMP의 특징

| 특징 | 설명 |
|------|------|
| **신뢰성 없음** | IP를 사용하므로 ICMP 메시지도 손실될 수 있음 |
| **피드백 전용** | 전달 성공을 보장하지 않고 오류만 보고 |
| **ICMP에 대한 ICMP 없음** | ICMP 메시지 오류에 대해 ICMP를 생성하지 않음 |

---

## 2. ICMP 메시지 형식

### 2.1 기본 구조

```
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |     Type      |     Code      |          Checksum             |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                         Message Body                          |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### 2.2 공통 헤더 필드

| 필드 | 크기 | 설명 |
|------|------|------|
| **Type** | 8비트 | 메시지 유형 |
| **Code** | 8비트 | 메시지 세부 유형 |
| **Checksum** | 16비트 | 오류 검출용 체크섬 |

---

## 3. ICMP 메시지 타입

### 3.1 메시지 타입 요약

| Type | 이름 | 카테고리 |
|------|------|----------|
| 0 | Echo Reply | 쿼리 |
| 3 | Destination Unreachable | 오류 |
| 4 | Source Quench | 흐름 제어 |
| 5 | Redirect | 라우팅 |
| 8 | Echo Request | 쿼리 |
| 11 | Time Exceeded | 오류 |
| 12 | Parameter Problem | 오류 |
| 13 | Timestamp Request | 쿼리 |
| 14 | Timestamp Reply | 쿼리 |
| 15 | Information Request | 쿼리 (구식) |
| 16 | Information Reply | 쿼리 (구식) |

---

## 4. 주요 메시지 상세

### 4.1 Echo Request / Echo Reply (Type 8 / Type 0)

**용도**: 네트워크 연결 테스트 (ping 명령어)

```
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |     Type      |     Code      |          Checksum             |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |           Identifier          |        Sequence Number        |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |     Data ...
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-
```

| 필드 | 설명 |
|------|------|
| Type | 8 (Request) 또는 0 (Reply) |
| Code | 0 |
| Identifier | 프로세스 식별자 (요청/응답 매칭용) |
| Sequence Number | 순서 번호 |
| Data | 임의의 데이터 (응답에서 그대로 반환) |

**ping 동작 예시**:
```
Host A                                    Host B
   |                                         |
   |  Echo Request (Type=8)                  |
   |---------------------------------------->|
   |                                         |
   |  Echo Reply (Type=0)                    |
   |<----------------------------------------|
   |                                         |

RTT = 응답 도착 시간 - 요청 전송 시간
```

---

### 4.2 Destination Unreachable (Type 3)

**용도**: 목적지에 도달할 수 없음을 알림

```
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |     Type      |     Code      |          Checksum             |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                             unused                            |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |      Internet Header + 64 bits of Original Data Datagram     |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

#### Code 값 상세

| Code | 이름 | 설명 |
|------|------|------|
| 0 | Network Unreachable | 목적지 네트워크에 도달 불가 |
| 1 | Host Unreachable | 목적지 호스트에 도달 불가 |
| 2 | Protocol Unreachable | 프로토콜을 지원하지 않음 |
| 3 | Port Unreachable | 포트가 열려있지 않음 (UDP) |
| 4 | Fragmentation Needed | 분할 필요하나 DF 비트 설정됨 |
| 5 | Source Route Failed | 소스 라우팅 실패 |

**실제 예시**:
```bash
$ ping 192.168.1.254
From 192.168.1.1 icmp_seq=1 Destination Host Unreachable
# Code 1: Host Unreachable
```

---

### 4.3 Time Exceeded (Type 11)

**용도**: TTL 초과 또는 재조립 시간 초과

```
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |     Type      |     Code      |          Checksum             |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                             unused                            |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |      Internet Header + 64 bits of Original Data Datagram     |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

| Code | 이름 | 설명 |
|------|------|------|
| 0 | TTL Exceeded in Transit | TTL이 0이 되어 패킷 폐기 |
| 1 | Fragment Reassembly Time Exceeded | 재조립 타임아웃 |

**traceroute 동작 원리**:
```
TTL=1 패킷 전송 → 첫 번째 라우터에서 Time Exceeded 응답
TTL=2 패킷 전송 → 두 번째 라우터에서 Time Exceeded 응답
TTL=3 패킷 전송 → 세 번째 라우터에서 Time Exceeded 응답
...
TTL=N 패킷 전송 → 목적지 도달, Echo Reply 또는 Port Unreachable
```

---

### 4.4 Redirect (Type 5)

**용도**: 더 좋은 경로가 있음을 알림

```
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |     Type      |     Code      |          Checksum             |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                 Gateway Internet Address                      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |      Internet Header + 64 bits of Original Data Datagram     |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

| Code | 이름 | 설명 |
|------|------|------|
| 0 | Redirect for Network | 네트워크에 대한 더 좋은 경로 |
| 1 | Redirect for Host | 호스트에 대한 더 좋은 경로 |
| 2 | Redirect for TOS and Network | TOS와 네트워크 |
| 3 | Redirect for TOS and Host | TOS와 호스트 |

**동작 시나리오**:
```
Host A -----> Router 1 -----> Router 2 -----> Host B
              (기본 GW)        (더 좋은 경로)

1. Host A가 Router 1로 패킷 전송
2. Router 1이 Router 2가 더 좋은 경로임을 인식
3. Router 1이 Host A에게 Redirect 메시지 전송
4. Host A가 라우팅 테이블 업데이트
5. 이후 Host A → Router 2 → Host B로 직접 전송
```

---

### 4.5 Source Quench (Type 4)

**용도**: 흐름 제어 - 송신 속도 낮추라는 요청

> **참고**: 현재는 사용되지 않음 (RFC 6633에서 폐지). TCP 혼잡 제어 메커니즘으로 대체됨.

```
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |     Type      |     Code      |          Checksum             |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                             unused                            |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |      Internet Header + 64 bits of Original Data Datagram     |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

---

### 4.6 Parameter Problem (Type 12)

**용도**: IP 헤더의 파라미터에 문제가 있음

```
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |     Type      |     Code      |          Checksum             |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |    Pointer    |                   unused                      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |      Internet Header + 64 bits of Original Data Datagram     |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

- **Pointer**: 문제가 있는 옥텟을 가리킴

---

### 4.7 Timestamp Request / Reply (Type 13 / Type 14)

**용도**: 시간 동기화 및 RTT 측정

```
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |     Type      |      Code     |          Checksum             |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |           Identifier          |        Sequence Number        |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                       Originate Timestamp                     |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                        Receive Timestamp                      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                        Transmit Timestamp                     |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

| 타임스탬프 | 설명 |
|------------|------|
| Originate | 송신자가 요청을 보낸 시간 |
| Receive | 수신자가 요청을 받은 시간 |
| Transmit | 수신자가 응답을 보낸 시간 |

- 시간 단위: 자정(UTC) 이후 밀리초
- Code 값: 0

---

### 4.8 Information Request / Reply (Type 15 / Type 16)

**용도**: 호스트가 자신이 속한 네트워크 번호를 알아내기 위한 메시지

> **참고**: 이 메시지 타입은 현재 사용되지 않음 (obsolete). RARP, BOOTP, DHCP로 대체됨.

```
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |     Type      |      Code     |          Checksum             |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |           Identifier          |        Sequence Number        |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

| 필드 | 설명 |
|------|------|
| Type | 15 (Request) 또는 16 (Reply) |
| Code | 0 |
| Identifier | 요청/응답 매칭을 위한 식별자 |
| Sequence Number | 순서 번호 |

**동작 방식**:
1. 호스트가 네트워크에 연결될 때 IP 헤더의 소스 주소를 0으로 설정
2. 목적지 주소도 0으로 설정하여 Information Request 전송
3. 게이트웨이 또는 다른 호스트가 해당 네트워크 주소 정보를 포함한 Reply 전송
4. Reply의 IP 헤더에서 소스/목적지 주소가 완전한 네트워크 주소로 채워짐

**역사적 배경**:
- 디스크 없는 워크스테이션이 부팅 시 자신의 네트워크 주소를 알아내기 위해 사용
- RARP (Reverse ARP), BOOTP, 그리고 현대의 DHCP가 이 기능을 대체함

---

## 5. 모든 Type별 Code 값 목록

### 5.1 완전한 ICMP Type/Code 참조 테이블

#### Type 0 - Echo Reply
| Code | 설명 |
|------|------|
| 0 | Echo Reply |

#### Type 3 - Destination Unreachable
| Code | 설명 | 생성 주체 |
|------|------|----------|
| 0 | Network Unreachable | 게이트웨이 |
| 1 | Host Unreachable | 게이트웨이 |
| 2 | Protocol Unreachable | 목적지 호스트 |
| 3 | Port Unreachable | 목적지 호스트 |
| 4 | Fragmentation Needed and DF Set | 게이트웨이 |
| 5 | Source Route Failed | 게이트웨이 |

#### Type 4 - Source Quench
| Code | 설명 |
|------|------|
| 0 | Source Quench (송신 속도 감소 요청) |

#### Type 5 - Redirect
| Code | 설명 |
|------|------|
| 0 | Redirect for Network (네트워크 단위 리다이렉트) |
| 1 | Redirect for Host (호스트 단위 리다이렉트) |
| 2 | Redirect for Type of Service and Network |
| 3 | Redirect for Type of Service and Host |

#### Type 8 - Echo Request
| Code | 설명 |
|------|------|
| 0 | Echo Request |

#### Type 11 - Time Exceeded
| Code | 설명 | 생성 주체 |
|------|------|----------|
| 0 | TTL Exceeded in Transit | 게이트웨이 |
| 1 | Fragment Reassembly Time Exceeded | 호스트 |

#### Type 12 - Parameter Problem
| Code | 설명 |
|------|------|
| 0 | Pointer indicates the error (문제 위치를 Pointer가 가리킴) |

#### Type 13 - Timestamp Request
| Code | 설명 |
|------|------|
| 0 | Timestamp Request |

#### Type 14 - Timestamp Reply
| Code | 설명 |
|------|------|
| 0 | Timestamp Reply |

#### Type 15 - Information Request (Obsolete)
| Code | 설명 |
|------|------|
| 0 | Information Request |

#### Type 16 - Information Reply (Obsolete)
| Code | 설명 |
|------|------|
| 0 | Information Reply |

---

## 6. 체크섬 계산 방법

### 6.1 체크섬 알고리즘 개요

ICMP 체크섬은 ICMP 메시지 전체의 무결성을 검증하기 위해 사용됩니다. IP 헤더 체크섬과 동일한 알고리즘을 사용합니다.

### 6.2 체크섬 계산 절차

**1단계: 체크섬 필드 초기화**
```
체크섬 필드를 0으로 설정
```

**2단계: 16비트 단위로 분할**
```
ICMP 메시지를 16비트(2바이트) 단위로 분할
메시지 길이가 홀수인 경우, 마지막에 0x00을 패딩
```

**3단계: 1의 보수 합 계산**
```
모든 16비트 워드를 더함 (1의 보수 덧셈)
오버플로우 발생 시 캐리를 다시 더함
```

**4단계: 1의 보수 취하기**
```
최종 합의 1의 보수(비트 반전)가 체크섬
```

### 6.3 의사 코드 (Pseudocode)

```
function calculate_checksum(icmp_message):
    // 1. 체크섬 필드를 0으로 설정
    icmp_message.checksum = 0

    // 2. 메시지를 16비트 워드 배열로 변환
    words = split_into_16bit_words(icmp_message)

    // 3. 홀수 길이인 경우 패딩
    if length(icmp_message) is odd:
        append 0x00 to icmp_message

    // 4. 모든 16비트 워드의 합 계산
    sum = 0
    for each word in words:
        sum = sum + word

        // 캐리 처리 (32비트 오버플로우)
        if sum > 0xFFFF:
            sum = (sum & 0xFFFF) + (sum >> 16)

    // 5. 1의 보수 반환
    checksum = ~sum & 0xFFFF
    return checksum
```

### 6.4 실제 계산 예시

Echo Request 메시지 예시:
```
Type: 08 (Echo Request)
Code: 00
Checksum: 00 00 (계산 전)
Identifier: 00 01
Sequence: 00 01
Data: 61 62 63 64 ("abcd")
```

**계산 과정**:
```
1. 16비트 워드로 분할:
   0x0800, 0x0000, 0x0001, 0x0001, 0x6162, 0x6364

2. 합산:
   0x0800 + 0x0000 + 0x0001 + 0x0001 + 0x6162 + 0x6364
   = 0x12AC8

3. 캐리 처리:
   0x2AC8 + 0x0001 = 0x2AC9

4. 1의 보수:
   ~0x2AC9 = 0xD536

5. 최종 체크섬: 0xD536
```

### 6.5 체크섬 검증

수신 측에서의 검증:
```
1. 수신된 메시지 전체(체크섬 포함)에 대해 동일한 합산 수행
2. 결과가 0xFFFF이면 체크섬 유효
3. 그렇지 않으면 메시지 손상됨 - 폐기
```

---

## 7. ICMP 오류 메시지 전송 금지 조건

### 7.1 ICMP 오류 메시지를 생성하지 않는 경우

ICMP 무한 루프와 네트워크 폭주를 방지하기 위해 다음 상황에서는 ICMP 오류 메시지를 생성하지 않습니다:

#### 7.1.1 ICMP 오류 메시지에 대한 응답

| 조건 | 이유 |
|------|------|
| ICMP 오류 메시지에 대해 ICMP 오류 생성 금지 | 무한 루프 방지 |

**예외**: ICMP 쿼리 메시지(Echo, Timestamp, Information)에 대해서는 ICMP 오류 생성 가능

**오류 메시지 타입 목록 (ICMP 오류 생성 금지 대상)**:
- Type 3: Destination Unreachable
- Type 4: Source Quench
- Type 5: Redirect
- Type 11: Time Exceeded
- Type 12: Parameter Problem

#### 7.1.2 브로드캐스트/멀티캐스트 관련

| 조건 | 이유 |
|------|------|
| IP 목적지 주소가 브로드캐스트 주소인 경우 | 브로드캐스트 폭풍 방지 |
| IP 목적지 주소가 멀티캐스트 주소인 경우 | 동일 |
| 링크 계층 브로드캐스트로 전송된 데이터그램 | 동일 |

**예시 - Smurf Attack 방지**:
```
공격자 → 브로드캐스트 주소로 Echo Request (소스 주소 위조)
         ↓
모든 호스트가 위조된 소스로 Echo Reply 전송 → 피해자 폭주

ICMP 생성 금지 규칙이 이 공격을 완화함
```

#### 7.1.3 IP 프래그먼트 관련

| 조건 | 이유 |
|------|------|
| 첫 번째 프래그먼트가 아닌 경우 (Fragment Offset != 0) | 중복 ICMP 방지 |

**이유**:
- 프래그먼트된 데이터그램의 경우, 첫 번째 프래그먼트만 상위 프로토콜 헤더(TCP/UDP 포트 등)를 포함
- 두 번째 이후 프래그먼트에 대해 ICMP를 보내면 불완전한 정보 전달

#### 7.1.4 소스 주소 관련

| 조건 | 이유 |
|------|------|
| 소스 주소가 0.0.0.0인 경우 | 응답 대상 없음 |
| 소스 주소가 127.x.x.x (루프백)인 경우 | 내부 통신 |
| 소스 주소가 브로드캐스트 주소인 경우 | 증폭 공격 방지 |
| 소스 주소가 멀티캐스트 주소인 경우 | 유효하지 않은 소스 |

### 7.2 ICMP 오류 메시지 전송 금지 규칙 요약 표

| 번호 | 조건 | 적용 대상 |
|------|------|----------|
| 1 | ICMP 오류 메시지에 대한 응답 | 모든 ICMP 오류 타입 |
| 2 | 목적지가 브로드캐스트 주소 | 모든 데이터그램 |
| 3 | 목적지가 멀티캐스트 주소 | 모든 데이터그램 |
| 4 | 링크 계층 브로드캐스트 | 모든 데이터그램 |
| 5 | 첫 번째가 아닌 IP 프래그먼트 | Fragment Offset > 0 |
| 6 | 소스 주소가 유효하지 않음 | 0.0.0.0, 브로드캐스트, 멀티캐스트 |

### 7.3 ICMP 전송 빈도 제한

네트워크 폭주 방지를 위한 추가 권장 사항:

```
- 동일한 목적지에 대해 일정 시간 내 ICMP 오류 메시지 수 제한
- 일반적으로 초당 1개 또는 몇 초에 1개로 제한
- 예: Linux 기본값 - net.ipv4.icmp_ratelimit = 1000 (1초에 1000개 제한)
```

---

## 8. 게이트웨이 vs 호스트 역할 구분

### 8.1 역할 정의

| 구분 | 게이트웨이 (라우터) | 호스트 |
|------|-------------------|--------|
| **주요 기능** | 패킷 포워딩, 라우팅 | 데이터 송수신 (종단점) |
| **IP 포워딩** | 활성화 | 비활성화 (일반적) |
| **위치** | 네트워크 경계/중간 | 네트워크 종단 |

### 8.2 ICMP 메시지별 역할 구분

#### 8.2.1 게이트웨이만 생성하는 메시지

| 메시지 타입 | Code | 설명 |
|------------|------|------|
| **Destination Unreachable (Type 3)** | 0 | Network Unreachable |
| **Destination Unreachable (Type 3)** | 1 | Host Unreachable |
| **Destination Unreachable (Type 3)** | 4 | Fragmentation Needed and DF Set |
| **Destination Unreachable (Type 3)** | 5 | Source Route Failed |
| **Redirect (Type 5)** | 0-3 | 모든 Redirect 메시지 |
| **Time Exceeded (Type 11)** | 0 | TTL Exceeded in Transit |
| **Source Quench (Type 4)** | 0 | 혼잡 시 (게이트웨이에서 주로 발생) |

**이유**: 게이트웨이는 패킷을 포워딩하는 과정에서 경로 문제, TTL 만료, 더 나은 경로 발견 등의 상황을 인지함

#### 8.2.2 호스트만 생성하는 메시지

| 메시지 타입 | Code | 설명 |
|------------|------|------|
| **Destination Unreachable (Type 3)** | 2 | Protocol Unreachable |
| **Destination Unreachable (Type 3)** | 3 | Port Unreachable |
| **Time Exceeded (Type 11)** | 1 | Fragment Reassembly Time Exceeded |

**이유**:
- Protocol/Port Unreachable: 목적지 호스트만 해당 프로토콜/포트 지원 여부 판단 가능
- Fragment Reassembly: 목적지 호스트에서 프래그먼트 재조립 수행

#### 8.2.3 게이트웨이와 호스트 모두 생성 가능한 메시지

| 메시지 타입 | 설명 |
|------------|------|
| **Echo Request/Reply (Type 8/0)** | 연결 테스트 |
| **Timestamp Request/Reply (Type 13/14)** | 시간 정보 교환 |
| **Information Request/Reply (Type 15/16)** | 네트워크 정보 (obsolete) |
| **Parameter Problem (Type 12)** | IP 헤더 오류 발견 시 |
| **Source Quench (Type 4)** | 버퍼 오버플로우 시 (드물게 호스트도 생성) |

### 8.3 Redirect 메시지의 특별 규칙

Redirect 메시지는 **게이트웨이만** 생성할 수 있습니다:

```
조건:
1. 게이트웨이가 패킷을 수신한 인터페이스와 동일한 인터페이스로 포워딩할 때
2. 해당 목적지에 대해 더 나은 게이트웨이가 존재할 때
3. 소스 호스트가 직접 더 나은 게이트웨이에 도달 가능할 때

동작:
- 게이트웨이는 Redirect를 소스 호스트에게 전송
- 소스 호스트는 라우팅 테이블 업데이트
- 호스트는 Redirect를 수신만 하고 생성하지 않음
```

**시나리오 다이어그램**:
```
      Host A                Router 1              Router 2              Host B
         |                     |                     |                     |
         |---패킷(Host B로)---->|                     |                     |
         |                     |--Redirect(Router2)->|                     |
         |<-Redirect(Router2)--|                     |                     |
         |                     |                     |                     |
         |---직접 Router 2로-------------------------------->|             |
         |                                                   |---패킷--->  |
```

### 8.4 역할에 따른 ICMP 처리 요약

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        ICMP 메시지 생성 주체                              │
├────────────────────┬────────────────────┬───────────────────────────────┤
│   게이트웨이 전용    │     호스트 전용     │          공통 (둘 다)          │
├────────────────────┼────────────────────┼───────────────────────────────┤
│ Type 3, Code 0     │ Type 3, Code 2     │ Type 0 (Echo Reply)          │
│ (Net Unreachable)  │ (Proto Unreachable)│                              │
├────────────────────┼────────────────────┼───────────────────────────────┤
│ Type 3, Code 1     │ Type 3, Code 3     │ Type 8 (Echo Request)        │
│ (Host Unreachable) │ (Port Unreachable) │                              │
├────────────────────┼────────────────────┼───────────────────────────────┤
│ Type 3, Code 4     │ Type 11, Code 1    │ Type 12 (Parameter Problem)  │
│ (Frag Needed)      │ (Reassembly Time)  │                              │
├────────────────────┼────────────────────┼───────────────────────────────┤
│ Type 3, Code 5     │                    │ Type 13/14 (Timestamp)       │
│ (Source Route)     │                    │                              │
├────────────────────┼────────────────────┼───────────────────────────────┤
│ Type 5 (Redirect)  │                    │ Type 15/16 (Information)     │
├────────────────────┼────────────────────┼───────────────────────────────┤
│ Type 11, Code 0    │                    │ Type 4 (Source Quench)       │
│ (TTL Exceeded)     │                    │                              │
└────────────────────┴────────────────────┴───────────────────────────────┘
```

### 8.5 구현 참고사항

**게이트웨이 구현 시**:
- 패킷 포워딩 로직에서 ICMP 생성 필요성 판단
- TTL 감소 및 만료 처리
- 라우팅 테이블 기반 Redirect 결정
- 네트워크/호스트 도달 가능성 판단

**호스트 구현 시**:
- 수신된 Redirect 메시지 처리 및 라우팅 테이블 업데이트
- 상위 프로토콜 가용성(Protocol/Port) 판단
- 프래그먼트 재조립 타임아웃 처리
- ICMP 쿼리 메시지 응답

---

## 9. ICMP 메시지에 포함되는 정보

오류 메시지에는 항상 다음이 포함됩니다:
- 원본 IP 헤더
- 원본 데이터의 처음 64비트 (8바이트)

이를 통해 송신자가:
- 어떤 패킷이 문제인지 식별
- 어떤 상위 프로토콜/포트가 관련되었는지 확인

---

## 10. 실용적 활용

### 10.1 ping 명령어

```bash
$ ping google.com
PING google.com (142.250.196.110): 56 data bytes
64 bytes from 142.250.196.110: icmp_seq=0 ttl=116 time=31.5 ms
64 bytes from 142.250.196.110: icmp_seq=1 ttl=116 time=32.1 ms
64 bytes from 142.250.196.110: icmp_seq=2 ttl=116 time=31.8 ms
```

| 정보 | 설명 |
|------|------|
| icmp_seq | ICMP 순서 번호 |
| ttl | Time To Live (홉 수 추정 가능) |
| time | Round Trip Time (RTT) |

### 10.2 traceroute 명령어

```bash
$ traceroute google.com
 1  192.168.1.1 (192.168.1.1)  1.234 ms
 2  10.0.0.1 (10.0.0.1)  5.678 ms
 3  72.14.215.85 (72.14.215.85)  15.234 ms
 4  142.250.196.110 (142.250.196.110)  31.567 ms
```

**동작 원리**:
1. TTL=1인 UDP 패킷 (또는 ICMP Echo) 전송
2. 첫 번째 라우터에서 TTL=0 → Time Exceeded 반환
3. TTL을 1씩 증가시키며 반복
4. 목적지 도달 시 Port Unreachable 또는 Echo Reply

---

## 11. 보안 고려사항

### 11.1 ICMP 관련 공격

| 공격 유형 | 설명 |
|----------|------|
| **Ping Flood** | 대량의 Echo Request로 서비스 거부 |
| **Ping of Death** | 비정상적으로 큰 패킷으로 시스템 충돌 |
| **Smurf Attack** | 브로드캐스트 주소를 이용한 증폭 공격 |
| **ICMP Redirect 공격** | 가짜 Redirect로 트래픽 가로채기 |

### 11.2 방어 전략

```bash
# ICMP Echo 차단 예시 (Linux iptables)
iptables -A INPUT -p icmp --icmp-type echo-request -j DROP

# 또는 커널 파라미터로 비활성화
echo 1 > /proc/sys/net/ipv4/icmp_echo_ignore_all
```

---

## 12. IP 헤더와의 관계

ICMP 메시지는 IP 데이터그램 안에 캡슐화됩니다:

```
+------------------+
|    IP Header     |  ← Protocol = 1 (ICMP)
+------------------+
|   ICMP Message   |
+------------------+
```

- **IP Protocol 번호**: 1
- **IP 헤더의 Protocol 필드**가 1이면 페이로드는 ICMP

---

## 13. ICMPv6

IPv6에서는 ICMPv6 (RFC 4443)가 사용됩니다:

| 기능 | ICMPv4 | ICMPv6 |
|------|--------|--------|
| 기본 오류 보고 | O | O |
| Echo/Echo Reply | O | O |
| Router Discovery | 별도 프로토콜 | 내장 (NDP) |
| Neighbor Discovery | ARP 사용 | NDP로 통합 |
| Multicast 관리 | IGMP | MLD (내장) |

---

## 14. 요약

| 항목 | 내용 |
|------|------|
| **목적** | IP 네트워크의 제어 및 오류 보고 |
| **주요 용도** | ping, traceroute, 오류 알림 |
| **메시지 분류** | 쿼리 메시지, 오류 메시지 |
| **특징** | IP의 필수 구성 요소, 신뢰성 없음 |
| **IP Protocol** | 1 |

---

## 참고 자료

- RFC 792: https://www.rfc-editor.org/rfc/rfc792
- RFC 4443: ICMPv6
- RFC 6633: Source Quench 폐지
- RFC 1122: Host Requirements
