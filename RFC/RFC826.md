# RFC 826 - Ethernet Address Resolution Protocol (ARP)

## 문서 정보

- **RFC 번호**: 826
- **제목**: An Ethernet Address Resolution Protocol (이더넷 주소 해석 프로토콜)
- **저자**: David C. Plummer (MIT)
- **발행일**: 1982년 11월
- **목적**: 프로토콜 주소(예: IP 주소)를 하드웨어 주소(예: MAC 주소)로 동적으로 변환

---

## 1. 개요

### 1.1 ARP란?

ARP(Address Resolution Protocol)는 네트워크 계층(L3)의 프로토콜 주소를 데이터 링크 계층(L2)의 하드웨어 주소로 변환하는 프로토콜입니다.

### 1.2 왜 ARP가 필요한가?

| 계층 | 주소 체계 | 예시 |
|------|----------|------|
| 네트워크 계층 (L3) | 프로토콜 주소 | IP 주소 (32비트) |
| 데이터 링크 계층 (L2) | 하드웨어 주소 | MAC 주소 (48비트) |

이더넷은 48비트 MAC 주소를 사용하지만, 상위 프로토콜들은 각각 다른 주소 체계를 사용합니다:
- **Internet (IP)**: 32비트
- **CHAOS**: 16비트
- **Xerox PUP**: 8비트

이 두 계층 간의 주소 매핑을 동적으로 수행하기 위해 ARP가 필요합니다.

---

## 2. 핵심 문제

> "프로토콜 타입과 프로토콜 주소 쌍을 48비트 이더넷 주소로 동적으로 매핑하는 표준화된 방식이 필요하다"

### 문제 상황 예시

호스트 A가 호스트 B에게 IP 패킷을 보내려고 할 때:
1. A는 B의 IP 주소(192.168.1.100)를 알고 있음
2. 하지만 이더넷 프레임을 전송하려면 B의 MAC 주소가 필요함
3. IP 주소만으로는 MAC 주소를 알 수 없음

→ **ARP가 이 문제를 해결**

---

## 3. ARP 패킷 형식

```
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |         Hardware Type         |         Protocol Type         |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |  HW Addr Len  | Proto Addr Len|           Operation           |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                    Sender Hardware Address                    |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                    Sender Protocol Address                    |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                    Target Hardware Address                    |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                    Target Protocol Address                    |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### 필드 상세 설명

| 필드 | 크기 | 설명 |
|------|------|------|
| **ar$hrd** (Hardware Type) | 16비트 | 하드웨어 주소 공간 (이더넷 = 1) |
| **ar$pro** (Protocol Type) | 16비트 | 프로토콜 주소 공간 (IPv4 = 0x0800) |
| **ar$hln** (Hardware Length) | 8비트 | 하드웨어 주소 길이 (이더넷 = 6) |
| **ar$pln** (Protocol Length) | 8비트 | 프로토콜 주소 길이 (IPv4 = 4) |
| **ar$op** (Operation) | 16비트 | 연산 코드 |
| **ar$sha** (Sender HW Addr) | 가변 | 송신자 하드웨어 주소 |
| **ar$spa** (Sender Proto Addr) | 가변 | 송신자 프로토콜 주소 |
| **ar$tha** (Target HW Addr) | 가변 | 대상 하드웨어 주소 |
| **ar$tpa** (Target Proto Addr) | 가변 | 대상 프로토콜 주소 |

### Operation 코드

| 코드 | 의미 |
|------|------|
| 1 | ARP Request (요청) |
| 2 | ARP Reply (응답) |

---

## 4. ARP 동작 알고리즘

### 4.1 ARP Request (요청) 생성 과정

```
1. 호스트가 특정 IP 주소로 패킷을 전송하려 함
2. 주소 해석 모듈이 ARP 캐시(변환 테이블) 확인
3. 매핑이 존재하면 → 해당 MAC 주소 사용
4. 매핑이 없으면 → ARP Request 브로드캐스트
```

### 4.2 ARP Reply (응답) 처리 과정

```
1. ARP 패킷 수신
2. 하드웨어 타입과 프로토콜 지원 여부 확인
3. 송신자 정보를 ARP 캐시에 병합 (기존 항목 갱신 허용)
4. 자신이 대상인지 확인
5. REQUEST인 경우:
   - 필드를 교환하여 REPLY 생성
   - 유니캐스트로 송신자에게 전송
```

### 4.3 상세 알고리즘 (의사 코드)

```python
def process_arp_packet(packet):
    # 1. 기본 검증
    if not support_hardware_type(packet.hrd):
        return  # 무시
    if not support_protocol_type(packet.pro):
        return  # 무시

    # 2. 송신자 정보 확인 및 캐시 업데이트
    merge_flag = False
    if packet.spa in arp_cache:
        arp_cache[packet.spa] = packet.sha
        merge_flag = True

    # 3. 대상 확인
    if packet.tpa == my_protocol_address:
        if not merge_flag:
            arp_cache[packet.spa] = packet.sha

        # 4. Request인 경우 Reply 전송
        if packet.op == ARP_REQUEST:
            reply = ARP_Packet()
            reply.op = ARP_REPLY
            reply.sha = my_hardware_address
            reply.spa = my_protocol_address
            reply.tha = packet.sha
            reply.tpa = packet.spa
            send_unicast(reply, packet.sha)
```

---

## 5. 통신 시나리오 예시

### 호스트 X와 Y 간의 첫 통신

```
초기 상태:
- 호스트 X: IP=192.168.1.10, MAC=AA:AA:AA:AA:AA:AA
- 호스트 Y: IP=192.168.1.20, MAC=BB:BB:BB:BB:BB:BB
- X의 ARP 캐시: 비어있음
- Y의 ARP 캐시: 비어있음

Step 1: X가 Y에게 패킷을 보내려 함
        → ARP 캐시에 192.168.1.20 없음
        → ARP Request 브로드캐스트

Step 2: ARP Request 전송
        [Broadcast: FF:FF:FF:FF:FF:FF]
        - 송신자 MAC: AA:AA:AA:AA:AA:AA
        - 송신자 IP:  192.168.1.10
        - 대상 MAC:   00:00:00:00:00:00 (모름)
        - 대상 IP:    192.168.1.20

Step 3: Y가 ARP Request 수신
        → "나의 IP네!" 확인
        → X의 정보를 캐시에 저장
        → ARP Reply 전송

Step 4: ARP Reply 전송
        [Unicast: AA:AA:AA:AA:AA:AA]
        - 송신자 MAC: BB:BB:BB:BB:BB:BB
        - 송신자 IP:  192.168.1.20
        - 대상 MAC:   AA:AA:AA:AA:AA:AA
        - 대상 IP:    192.168.1.10

Step 5: X가 ARP Reply 수신
        → Y의 정보를 캐시에 저장

최종 상태:
- X의 ARP 캐시: {192.168.1.20 → BB:BB:BB:BB:BB:BB}
- Y의 ARP 캐시: {192.168.1.10 → AA:AA:AA:AA:AA:AA}
- 양방향 통신 준비 완료!
```

---

## 6. ARP 캐시 관리

### 6.1 캐시 타임아웃

ARP 캐시 항목은 영구적이지 않습니다. RFC 826에서 제안하는 관리 방식:

- **시간 기반 만료**: 일정 시간이 지나면 캐시 항목 삭제
- **재검증**: 주기적으로 ARP Request를 보내 유효성 확인

### 6.2 고려사항

| 상황 | 해결 방안 |
|------|----------|
| 호스트 이동 | 타임아웃 후 재요청 |
| IP 주소 변경 | 새로운 ARP Request로 갱신 |
| 네트워크 장애 | 불완전한 라우팅 정보 처리 |

---

## 7. 설계 철학

### 7.1 효율성

> "패킷은 필요할 때만 전송되며, 통상적으로 부팅당 한 번만 발생한다"

- **주기적 브로드캐스트 없음**: 네트워크 트래픽 최소화
- **동적 요청-응답 모델**: 필요할 때만 주소 해석 수행
- **캐싱**: 한 번 해석된 주소는 재사용

### 7.2 확장성

ARP는 특정 프로토콜에 종속되지 않도록 설계되었습니다:
- 다양한 하드웨어 타입 지원 (이더넷 외에도)
- 다양한 프로토콜 주소 지원 (IP 외에도)
- 가변 길이 주소 필드

---

## 8. 보안 고려사항

### 8.1 ARP 스푸핑 (ARP Spoofing)

ARP는 인증 메커니즘이 없어 다음과 같은 공격에 취약합니다:

```
공격 시나리오:
1. 공격자가 가짜 ARP Reply 전송
2. "192.168.1.1의 MAC은 [공격자 MAC]이다"
3. 피해자의 ARP 캐시 오염
4. 모든 트래픽이 공격자에게 전달됨 (중간자 공격)
```

### 8.2 대응 방안

- **정적 ARP 항목**: 중요한 호스트에 대해 수동 설정
- **ARP 감시**: 비정상적인 ARP 트래픽 모니터링
- **Dynamic ARP Inspection (DAI)**: 스위치 레벨에서 ARP 검증

---

## 9. 관련 프로토콜

| 프로토콜 | 설명 |
|----------|------|
| **RARP** | Reverse ARP - MAC → IP 변환 (구식) |
| **Proxy ARP** | 다른 네트워크의 호스트를 대신하여 응답 |
| **Gratuitous ARP** | IP 충돌 감지 및 캐시 갱신용 |
| **NDP** | IPv6용 Neighbor Discovery Protocol |

---

## 10. 요약

| 항목 | 내용 |
|------|------|
| **목적** | IP 주소 → MAC 주소 변환 |
| **동작 방식** | Request(브로드캐스트) → Reply(유니캐스트) |
| **캐싱** | ARP 테이블에 저장, 타임아웃 관리 |
| **장점** | 동적, 효율적, 확장 가능 |
| **단점** | 보안 취약점 (스푸핑) |

---

## 참고 자료

- RFC 826: https://www.rfc-editor.org/rfc/rfc826
- RFC 903: RARP (Reverse Address Resolution Protocol)
- RFC 4861: IPv6 Neighbor Discovery
