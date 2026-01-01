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

### 각 필드 상세 분석

#### Hardware Type (ar$hrd) - 16비트

네트워크 인터페이스의 하드웨어 타입을 식별합니다.

| 값 | 하드웨어 타입 |
|----|--------------|
| 1 | Ethernet (10Mb) |
| 6 | IEEE 802 Networks |
| 15 | Frame Relay |
| 20 | Serial Line |

이 필드를 통해 ARP가 다양한 데이터 링크 계층 기술과 함께 동작할 수 있습니다.

#### Protocol Type (ar$pro) - 16비트

상위 계층 프로토콜의 타입을 식별합니다. 이더넷 프레임의 EtherType 값과 동일한 값을 사용합니다.

| 값 | 프로토콜 |
|----|----------|
| 0x0800 | IPv4 |
| 0x0806 | ARP |
| 0x86DD | IPv6 |

#### Hardware Address Length (ar$hln) - 8비트

하드웨어 주소의 바이트 단위 길이입니다.

- **이더넷**: 6바이트 (48비트 MAC 주소)
- **Token Ring**: 6바이트
- **FDDI**: 6바이트

이 필드가 있어서 ARP는 다양한 길이의 하드웨어 주소를 처리할 수 있습니다.

#### Protocol Address Length (ar$pln) - 8비트

프로토콜 주소의 바이트 단위 길이입니다.

- **IPv4**: 4바이트 (32비트)
- **CHAOS**: 2바이트 (16비트)

#### Operation (ar$op) - 16비트

ARP 패킷의 유형을 나타냅니다. 요청(Request)과 응답(Reply)을 구분합니다.

#### Sender Hardware Address (ar$sha) - 가변 길이

ARP 패킷을 전송하는 호스트의 하드웨어(MAC) 주소입니다.
- 길이는 ar$hln 필드에 의해 결정
- ARP Request에서: 요청자의 MAC 주소
- ARP Reply에서: 응답자의 MAC 주소

#### Sender Protocol Address (ar$spa) - 가변 길이

ARP 패킷을 전송하는 호스트의 프로토콜(IP) 주소입니다.
- 길이는 ar$pln 필드에 의해 결정
- 수신자가 송신자 정보를 캐시에 저장하는 데 사용

#### Target Hardware Address (ar$tha) - 가변 길이

대상 호스트의 하드웨어(MAC) 주소입니다.
- ARP Request에서: 0으로 채워짐 (알 수 없는 값)
- ARP Reply에서: 원래 요청자의 MAC 주소

#### Target Protocol Address (ar$tpa) - 가변 길이

대상 호스트의 프로토콜(IP) 주소입니다.
- ARP Request에서: 찾고자 하는 IP 주소
- ARP Reply에서: 원래 요청자의 IP 주소

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

### 6.2 운영체제별 기본 타임아웃 값

| 운영체제 | 완료 항목 타임아웃 | 불완전 항목 타임아웃 |
|---------|------------------|-------------------|
| Linux | 기본 60초 (조정 가능) | 3초 |
| Windows | 15-45초 (동적) | 3분 |
| macOS | 20분 | 60초 |
| Cisco IOS | 4시간 | 60초 |

### 6.3 캐시 항목 유형

#### 동적 항목 (Dynamic Entries)

ARP 프로토콜을 통해 자동으로 학습된 항목입니다.

```
특성:
- 타임아웃에 의해 자동 만료
- 새로운 ARP Reply로 갱신 가능
- 대부분의 캐시 항목이 이 유형
```

#### 정적 항목 (Static Entries)

관리자가 수동으로 설정한 항목입니다.

```
특성:
- 타임아웃으로 만료되지 않음
- 수동으로만 삭제 가능
- ARP 스푸핑 방지에 사용
```

#### 불완전 항목 (Incomplete Entries)

ARP Request를 보냈지만 아직 Reply를 받지 못한 항목입니다.

```
특성:
- 짧은 타임아웃 (일반적으로 3초)
- Reply 수신 시 완료 항목으로 전환
- 재시도 횟수 제한 있음 (일반적으로 3회)
```

### 6.4 캐시 관리 명령어

#### Linux

```bash
# ARP 캐시 확인
arp -a
ip neigh show

# 캐시 항목 추가 (정적)
sudo arp -s 192.168.1.100 AA:BB:CC:DD:EE:FF

# 캐시 항목 삭제
sudo arp -d 192.168.1.100

# 전체 캐시 비우기
sudo ip neigh flush all

# 타임아웃 설정 확인/변경
cat /proc/sys/net/ipv4/neigh/default/gc_stale_time
echo 120 > /proc/sys/net/ipv4/neigh/default/gc_stale_time
```

#### Windows

```cmd
# ARP 캐시 확인
arp -a

# 캐시 항목 추가 (정적)
arp -s 192.168.1.100 AA-BB-CC-DD-EE-FF

# 캐시 항목 삭제
arp -d 192.168.1.100

# 전체 캐시 비우기
arp -d *
```

#### macOS

```bash
# ARP 캐시 확인
arp -a

# 캐시 항목 추가 (정적)
sudo arp -s 192.168.1.100 AA:BB:CC:DD:EE:FF

# 캐시 항목 삭제
sudo arp -d 192.168.1.100
```

### 6.5 캐시 관련 고려사항

| 상황 | 문제점 | 해결 방안 |
|------|--------|----------|
| 호스트 이동 | 이전 MAC 주소가 캐시에 남아있음 | 타임아웃 후 재요청 또는 Gratuitous ARP |
| IP 주소 변경 | 잘못된 매핑 정보 | 새로운 ARP Request로 갱신 |
| NIC 교체 | MAC 주소 변경됨 | Gratuitous ARP로 즉시 갱신 |
| 네트워크 장애 | 불완전한 라우팅 정보 | 불완전 항목 타임아웃 후 재시도 |
| 캐시 오버플로우 | 메모리 부족 | 오래된 항목 우선 삭제 (LRU) |

### 6.6 캐시 최적화 전략

```
1. 적절한 타임아웃 설정
   - 너무 짧으면: ARP 트래픽 증가
   - 너무 길면: 오래된 정보 유지

2. 정적 항목 활용
   - 게이트웨이, 서버 등 핵심 장비
   - 보안이 중요한 시스템

3. 캐시 크기 모니터링
   - 대규모 네트워크에서 중요
   - 필요시 캐시 크기 조정
```

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

## 10. Gratuitous ARP 상세

### 10.1 정의

Gratuitous ARP는 호스트가 자신의 IP 주소에 대한 ARP Request를 브로드캐스트하는 특수한 형태의 ARP입니다. "요청하지 않은(gratuitous)" ARP라는 이름은 실제로 주소 해석이 필요하지 않은 상황에서 전송되기 때문입니다.

### 10.2 패킷 구조

```
Gratuitous ARP Request:
- Sender IP = Target IP (자신의 IP)
- Sender MAC = 자신의 MAC
- Target MAC = 00:00:00:00:00:00 또는 FF:FF:FF:FF:FF:FF
- Operation = 1 (Request)
- 이더넷 목적지 = FF:FF:FF:FF:FF:FF (브로드캐스트)
```

### 10.3 주요 용도

#### IP 주소 충돌 감지 (Duplicate Address Detection)

```
동작 방식:
1. 호스트가 IP 주소를 설정하려 할 때
2. 해당 IP로 Gratuitous ARP Request 전송
3. 응답이 오면 → IP 충돌 발생!
4. 응답이 없으면 → IP 사용 가능

결과 처리:
- 충돌 감지 시: IP 설정 거부 또는 경고 표시
- 충돌 없음: 정상적으로 IP 설정 완료
```

#### ARP 캐시 갱신 (Cache Update)

```
시나리오: NIC 교체로 MAC 주소 변경

1. 호스트 A의 NIC 교체
   - 기존 MAC: AA:AA:AA:AA:AA:AA
   - 새 MAC: BB:BB:BB:BB:BB:BB

2. 네트워크의 다른 호스트들은 이전 MAC을 캐시에 보유

3. A가 Gratuitous ARP 전송
   - "192.168.1.10은 BB:BB:BB:BB:BB:BB입니다"

4. 모든 호스트가 캐시 갱신
   - 통신 중단 없이 MAC 주소 변경 완료
```

#### VRRP/HSRP 페일오버

```
가상 IP를 사용하는 고가용성 환경:

1. 마스터 라우터 장애 발생
2. 백업 라우터가 마스터로 전환
3. 백업 라우터가 Gratuitous ARP 전송
   - 가상 IP에 대해 자신의 MAC 광고
4. 네트워크 장비들이 캐시 갱신
5. 트래픽이 새 마스터로 전환
```

### 10.4 Gratuitous ARP의 두 가지 형태

| 형태 | Operation | 용도 |
|------|-----------|------|
| Gratuitous ARP Request | 1 (Request) | IP 충돌 감지 |
| Gratuitous ARP Reply | 2 (Reply) | 캐시 갱신 (일부 시스템) |

### 10.5 보안 고려사항

```
위험:
- Gratuitous ARP를 이용한 ARP 스푸핑 가능
- 악의적인 호스트가 가짜 Gratuitous ARP 전송
- 네트워크 전체의 캐시 오염

대응:
- Dynamic ARP Inspection (DAI) 활성화
- 포트 보안 설정
- Gratuitous ARP 필터링 (일부 환경)
```

---

## 11. Proxy ARP 상세

### 11.1 정의

Proxy ARP는 라우터나 게이트웨이가 다른 네트워크에 있는 호스트를 대신하여 ARP Request에 응답하는 기술입니다. RFC 1027에서 표준화되었습니다.

### 11.2 동작 원리

```
네트워크 구성:
- 서브넷 A: 192.168.1.0/24 (호스트 X가 위치)
- 서브넷 B: 192.168.2.0/24 (호스트 Y가 위치)
- 라우터 R: 두 서브넷을 연결

호스트 X가 호스트 Y와 통신하려 할 때:

일반적인 경우 (Proxy ARP 없음):
1. X가 Y의 IP가 다른 서브넷임을 인식
2. X가 게이트웨이(R)로 패킷 전송
3. R이 Y에게 패킷 전달

Proxy ARP 사용 시:
1. X가 Y의 MAC을 알기 위해 ARP Request 브로드캐스트
2. 라우터 R이 이 요청을 수신
3. R이 Y에게 도달 가능함을 알고 있음
4. R이 자신의 MAC 주소로 ARP Reply 전송
5. X가 Y의 IP → R의 MAC으로 캐시
6. X가 패킷을 R의 MAC으로 전송
7. R이 패킷을 Y에게 전달
```

### 11.3 상세 시나리오

```
환경:
- 호스트 X: IP=192.168.1.10, MAC=AA:AA:AA:AA:AA:AA
- 호스트 Y: IP=192.168.2.20, MAC=BB:BB:BB:BB:BB:BB
- 라우터 R:
  - eth0: IP=192.168.1.1, MAC=CC:CC:CC:CC:CC:C1
  - eth1: IP=192.168.2.1, MAC=CC:CC:CC:CC:CC:C2

Step 1: X가 ARP Request 전송
        "192.168.2.20의 MAC은 무엇인가요?"
        (X는 Y가 로컬 네트워크에 있다고 생각)

Step 2: 라우터 R이 ARP Request 수신
        - R은 192.168.2.20으로 라우팅 가능함을 알고 있음
        - Proxy ARP 활성화 상태

Step 3: 라우터 R이 ARP Reply 전송
        "192.168.2.20의 MAC은 CC:CC:CC:CC:CC:C1입니다"
        (자신의 eth0 MAC 주소를 응답)

Step 4: X의 ARP 캐시 상태
        {192.168.2.20 → CC:CC:CC:CC:CC:C1}

Step 5: X가 Y에게 패킷 전송
        - 이더넷 목적지: CC:CC:CC:CC:CC:C1 (R의 MAC)
        - IP 목적지: 192.168.2.20 (Y의 IP)

Step 6: 라우터 R이 패킷 수신 및 라우팅
        - IP 목적지 확인 → 192.168.2.0/24 네트워크
        - eth1을 통해 Y에게 전달
```

### 11.4 Proxy ARP 사용 사례

| 사용 사례 | 설명 |
|----------|------|
| 서브넷 확장 | 물리적으로 분리된 네트워크를 하나의 서브넷처럼 사용 |
| 레거시 호스트 지원 | 서브넷 마스크를 이해하지 못하는 오래된 장비 |
| VPN 연결 | VPN을 통해 연결된 원격 호스트 지원 |
| NAT 환경 | 내부 호스트를 대신하여 응답 |

### 11.5 장단점

#### 장점

```
1. 호스트 설정 불필요
   - 게이트웨이 설정 없이 통신 가능
   - 레거시 시스템 호환성

2. 투명한 네트워크 확장
   - 서브넷 분할 없이 네트워크 확장
   - 호스트에게 투명하게 동작

3. 유연한 네트워크 구성
   - 복잡한 라우팅 설정 간소화
```

#### 단점

```
1. ARP 캐시 비효율
   - 원격 호스트마다 별도 캐시 항목
   - 캐시 오버플로우 가능성

2. 보안 취약점
   - ARP 스푸핑과 구분 어려움
   - 비인가 라우팅 가능

3. 브로드캐스트 도메인 확장
   - ARP 트래픽 증가
   - 네트워크 성능 저하 가능

4. 문제 해결 복잡성
   - 네트워크 토폴로지 파악 어려움
   - 디버깅 복잡
```

### 11.6 Proxy ARP 설정

#### Cisco 라우터

```
# Proxy ARP 활성화 (기본값)
Router(config-if)# ip proxy-arp

# Proxy ARP 비활성화
Router(config-if)# no ip proxy-arp
```

#### Linux

```bash
# Proxy ARP 확인
cat /proc/sys/net/ipv4/conf/eth0/proxy_arp

# Proxy ARP 활성화
echo 1 > /proc/sys/net/ipv4/conf/eth0/proxy_arp

# Proxy ARP 비활성화
echo 0 > /proc/sys/net/ipv4/conf/eth0/proxy_arp

# 특정 IP에 대해서만 Proxy ARP
ip neigh add proxy 192.168.2.20 dev eth0
```

---

## 12. RARP (Reverse ARP)

### 12.1 개요

RARP(Reverse Address Resolution Protocol)는 RFC 903에서 정의된 프로토콜로, ARP의 역방향 동작을 수행합니다. MAC 주소를 알고 있을 때 해당 IP 주소를 알아내는 데 사용됩니다.

| 프로토콜 | 방향 | 용도 |
|----------|------|------|
| ARP | IP → MAC | 목적지 하드웨어 주소 확인 |
| RARP | MAC → IP | 자신의 IP 주소 확인 |

### 12.2 탄생 배경

```
문제 상황:
- 디스크 없는 워크스테이션(Diskless Workstation)
- 부팅 시 자신의 IP 주소를 모름
- 네트워크를 통해 IP 주소를 알아내야 함

해결책:
- 자신의 MAC 주소는 NIC에서 읽을 수 있음
- RARP 서버에 MAC 주소를 보내고 IP 주소를 받음
```

### 12.3 RARP 패킷 형식

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

ARP와 동일한 형식, Operation 코드만 다름
```

### 12.4 Operation 코드

| 코드 | 의미 |
|------|------|
| 1 | ARP Request |
| 2 | ARP Reply |
| 3 | RARP Request |
| 4 | RARP Reply |

### 12.5 RARP 동작 과정

```
환경:
- 디스크 없는 클라이언트: MAC=AA:AA:AA:AA:AA:AA, IP=???
- RARP 서버: MAC=BB:BB:BB:BB:BB:BB, IP=192.168.1.1
- 서버의 RARP 테이블: {AA:AA:AA:AA:AA:AA → 192.168.1.100}

Step 1: 클라이언트 부팅
        - NIC에서 자신의 MAC 주소 읽음
        - IP 주소는 알 수 없음

Step 2: RARP Request 브로드캐스트
        [Ethernet Frame]
        - 출발지 MAC: AA:AA:AA:AA:AA:AA
        - 목적지 MAC: FF:FF:FF:FF:FF:FF
        - EtherType: 0x8035 (RARP)

        [RARP Packet]
        - Operation: 3 (RARP Request)
        - Sender MAC: AA:AA:AA:AA:AA:AA
        - Sender IP: 0.0.0.0 (모름)
        - Target MAC: AA:AA:AA:AA:AA:AA
        - Target IP: 0.0.0.0 (알고 싶음)

Step 3: RARP 서버가 요청 수신
        - Target MAC을 RARP 테이블에서 조회
        - 매핑 발견: AA:AA:AA:AA:AA:AA → 192.168.1.100

Step 4: RARP Reply 유니캐스트
        [Ethernet Frame]
        - 출발지 MAC: BB:BB:BB:BB:BB:BB
        - 목적지 MAC: AA:AA:AA:AA:AA:AA

        [RARP Packet]
        - Operation: 4 (RARP Reply)
        - Sender MAC: BB:BB:BB:BB:BB:BB
        - Sender IP: 192.168.1.1
        - Target MAC: AA:AA:AA:AA:AA:AA
        - Target IP: 192.168.1.100 (할당된 IP)

Step 5: 클라이언트가 IP 주소 획득
        - 자신의 IP 주소: 192.168.1.100
        - 부팅 과정 계속 진행
```

### 12.6 RARP의 한계

| 한계점 | 설명 |
|--------|------|
| 서버 필요 | 각 네트워크 세그먼트마다 RARP 서버 필요 |
| IP 주소만 제공 | 서브넷 마스크, 게이트웨이 등 추가 정보 없음 |
| 브로드캐스트 의존 | 라우터를 넘어 동작하지 않음 |
| 수동 테이블 관리 | MAC-IP 매핑을 수동으로 관리해야 함 |

### 12.7 RARP의 대체 기술

```
발전 순서:
RARP (1984) → BOOTP (1985) → DHCP (1993)

BOOTP 개선점:
- UDP/IP 기반으로 라우터 통과 가능
- 서브넷 마스크, 게이트웨이 정보 제공
- 부트 서버 주소 제공

DHCP 추가 개선점:
- 동적 IP 주소 할당
- 임대(Lease) 개념
- 다양한 옵션 지원
- IP 주소 풀 관리
```

### 12.8 현재 상태

```
RARP는 현재 거의 사용되지 않습니다.

이유:
1. DHCP가 모든 기능을 대체
2. 디스크 없는 워크스테이션 감소
3. PXE 부팅이 RARP 기능 대체
4. 네트워크 부팅 환경의 변화

역사적 의의:
- 네트워크 부팅의 초기 솔루션
- BOOTP와 DHCP 발전의 기반
- 주소 역해석 개념의 시작
```

---

## 13. 요약

### 13.1 ARP 기본

| 항목 | 내용 |
|------|------|
| **목적** | IP 주소 → MAC 주소 변환 |
| **동작 방식** | Request(브로드캐스트) → Reply(유니캐스트) |
| **캐싱** | ARP 테이블에 저장, 타임아웃 관리 |
| **장점** | 동적, 효율적, 확장 가능 |
| **단점** | 보안 취약점 (스푸핑) |

### 13.2 ARP 변형 프로토콜 비교

| 프로토콜 | 방향 | 주요 용도 | 현재 상태 |
|----------|------|----------|----------|
| **ARP** | IP → MAC | 목적지 MAC 확인 | 활발히 사용 |
| **RARP** | MAC → IP | 디스크리스 부팅 | DHCP로 대체됨 |
| **Gratuitous ARP** | 자기 광고 | IP 충돌 감지, 캐시 갱신 | 활발히 사용 |
| **Proxy ARP** | 대리 응답 | 서브넷 확장, 레거시 지원 | 제한적 사용 |

### 13.3 캐시 관리 핵심 사항

| 항목 | 내용 |
|------|------|
| **동적 항목** | 자동 학습, 타임아웃 적용 |
| **정적 항목** | 수동 설정, 보안 강화 |
| **불완전 항목** | Reply 대기 중, 짧은 타임아웃 |
| **갱신 방법** | 새 ARP Reply, Gratuitous ARP |

---

## 참고 자료

- RFC 826: https://www.rfc-editor.org/rfc/rfc826 - ARP (Address Resolution Protocol)
- RFC 903: https://www.rfc-editor.org/rfc/rfc903 - RARP (Reverse Address Resolution Protocol)
- RFC 1027: https://www.rfc-editor.org/rfc/rfc1027 - Proxy ARP
- RFC 2131: https://www.rfc-editor.org/rfc/rfc2131 - DHCP (Dynamic Host Configuration Protocol)
- RFC 4861: https://www.rfc-editor.org/rfc/rfc4861 - IPv6 Neighbor Discovery
- RFC 5227: https://www.rfc-editor.org/rfc/rfc5227 - IPv4 Address Conflict Detection
