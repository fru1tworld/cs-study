# ICMP

```
Network Working Group                                          J. Postel
Request for Comments: 792                                            ISI
                                                          September 1981
Updates:  RFCs 777, 760
Updates:  IENs 109, 128
```

INTERNET CONTROL MESSAGE PROTOCOL — ICMP 사양(Specification)

## 소개(Introduction)

- Internet Protocol(IP): Catenet이라 불리는 상호 연결된 네트워크 시스템에서 호스트 간 데이터그램 서비스를 위해 사용 [1]
- 네트워크 연결 장치 → 게이트웨이(Gateway)라 부름
  - 게이트웨이들은 제어 목적으로 Gateway to Gateway Protocol(GGP) [3]을 통해 상호 통신
- 게이트웨이나 목적지 호스트가 데이터그램 처리 중 오류를 보고하기 위해 출발지 호스트와 통신해야 하는 경우 존재 → 이 목적을 위해 Internet Control Message Protocol(ICMP) 사용
  - ICMP는 상위 계층 프로토콜인 것처럼 IP의 기본 지원을 사용하지만, 실제로는 IP의 필수 구성 요소 → 모든 IP 모듈에서 구현 필요
- ICMP 메시지가 전송되는 상황 예시
  - 데이터그램이 목적지에 도달할 수 없을 때
  - 게이트웨이가 데이터그램을 전달하기 위한 버퍼링 용량이 부족할 때
  - 게이트웨이가 호스트에게 더 짧은 경로로 트래픽을 보내도록 지시할 때
- Internet Protocol은 절대적으로 신뢰성 있게 설계되지 않음
  - ICMP 제어 메시지의 목적: 통신 환경의 문제에 대한 피드백 제공 (IP 자체를 신뢰성 있게 만드는 것이 아님)
  - 데이터그램 전달이나 제어 메시지 반환에 대한 보장 없음 → 일부 데이터그램은 알림 없이 폐기될 수 있음
  - 신뢰성 있는 통신이 필요한 상위 계층 프로토콜은 자체 신뢰성 절차 구현 필요
- ICMP 메시지는 일반적으로 데이터그램 처리 시의 오류를 보고
  - 메시지에 대한 메시지에 대한 메시지 등의 무한 반복 방지 → ICMP 메시지에 대한 ICMP 메시지는 전송하지 않음
  - 첫 번째 프래그먼트(프래그먼트 0)가 아닌 다른 프래그먼트에 도착한 오류에 대해서는 ICMP 메시지 미전송

## 메시지 형식(Message Formats)

- ICMP 메시지: 기본 IP 헤더를 사용하여 전송
- 데이터그램 데이터 부분의 첫 번째 옥텟 = ICMP type 필드 → 이 값에 따라 나머지 데이터의 형식 결정
- "unused"로 표시된 필드
  - 향후 확장을 위해 예약 → 전송 시 0이어야 함
  - 수신자는 이 필드를 사용해서는 안 됨(단, 체크섬 계산 시에는 포함)
- 별도 언급이 없는 한 인터넷 헤더 필드는 다음과 같음
  - Version: 4
  - IHL: Internet Header Length(워드 단위)
  - Type of Service: 0
  - Total Length: 헤더와 데이터의 길이(옥텟 단위)
  - Identification, Flags, Fragment Offset: 프래그먼트화에 사용 (참조: [1])
  - Time to Live
    - 수명(time to live, 초 단위)
    - 이 필드가 0이 되면 데이터그램은 폐기되며, 이 경우에도 ICMP 메시지의 대상이 될 수 있음
  - Protocol: ICMP = 1
  - Header Checksum
    - 헤더의 모든 16비트 워드의 1의 보수 합에 대한 16비트 1의 보수
    - 체크섬 계산 시 체크섬 필드는 0이어야 함
  - Source Address: 출발지 게이트웨이 또는 호스트의 주소
  - Destination Address: 목적지 게이트웨이 또는 호스트의 주소

## Destination Unreachable 메시지

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|     Type      |     Code      |          Checksum             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                             unused                            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|      Internet Header + 64 bits of Original Data Datagram      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

- IP 필드
  - Destination Address: 원본 데이터그램의 출발지 네트워크 및 주소
- ICMP 필드
  - Type: 3
  - Code
    - 0 = net unreachable
    - 1 = host unreachable
    - 2 = protocol unreachable
    - 3 = port unreachable
    - 4 = fragmentation needed and DF set
    - 5 = source route failed
  - Checksum
    - ICMP 메시지의 체크섬, type 필드에서 시작
    - 헤더의 모든 16비트 워드의 1의 보수 합에 대한 16비트 1의 보수
    - 체크섬 계산 시 체크섬 필드는 0이어야 함
  - Internet Header + 64 bits of Data Datagram
    - 인터넷 헤더에 원본 데이터그램의 옵션 포함
    - 오류와 데이터그램을 적절한 프로세스에 매칭하기 위해 호스트가 사용
    - 상위 계층 프로토콜이 포트 번호를 사용하는 경우, 원본 데이터그램 데이터의 처음 64비트에 해당 정보가 있다고 가정
- 설명
  - 게이트웨이의 라우팅 테이블 정보상 데이터그램의 인터넷 목적지 필드에 지정된 네트워크에 도달할 수 없는 경우(예: 해당 네트워크까지의 거리가 무한인 경우) → 게이트웨이는 출발지 호스트에게 destination unreachable 메시지 전송 가능
  - 일부 네트워크에서는 게이트웨이가 목적지 호스트 도달 가능 여부 판단 가능 → 도달 불가 시 출발지 호스트에게 destination unreachable 메시지 전송 가능
  - 목적지 호스트의 IP 모듈이 지정된 프로토콜 모듈이나 프로세스 포트가 비활성 상태여서 데이터그램을 전달할 수 없는 경우 → 목적지 호스트가 출발지 호스트에게 destination unreachable 메시지 전송 가능
  - 데이터그램이 게이트웨이에 의해 전달되기 위해 프래그먼트화되어야 하지만 Don't Fragment 플래그가 설정된 경우 → 게이트웨이는 해당 데이터그램을 폐기해야 하며 destination unreachable 메시지 반환 가능
  - Code 0, 1, 4, 5는 게이트웨이에서 수신 가능 · Code 2, 3은 호스트에서 수신 가능

## Time Exceeded 메시지

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|     Type      |     Code      |          Checksum             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                             unused                            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|      Internet Header + 64 bits of Original Data Datagram      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

- IP 필드
  - Destination Address: 원본 데이터그램의 출발지 네트워크 및 주소
- ICMP 필드
  - Type: 11
  - Code
    - 0 = time to live exceeded in transit
    - 1 = fragment reassembly time exceeded
  - Checksum
    - ICMP 메시지의 체크섬, type 필드에서 시작
    - 헤더의 모든 16비트 워드의 1의 보수 합에 대한 16비트 1의 보수
    - 체크섬 계산 시 체크섬 필드는 0이어야 함
  - Internet Header + 64 bits of Data Datagram
    - 인터넷 헤더에 원본 데이터그램의 옵션 포함
    - 오류와 데이터그램을 적절한 프로세스에 매칭하기 위해 호스트가 사용
    - 상위 계층 프로토콜이 포트 번호를 사용하는 경우, 원본 데이터그램 데이터의 처음 64비트에 해당 정보가 있다고 가정
- 설명
  - 데이터그램을 처리하는 게이트웨이가 time to live 필드가 0인 것을 발견하면 해당 데이터그램을 폐기해야 함 → time exceeded 메시지로 출발지 호스트에게 알림 가능
  - 프래그먼트화된 데이터그램을 재조립하는 호스트가 시간 제한 내에 누락된 프래그먼트로 재조립을 완료할 수 없는 경우 → 해당 데이터그램을 폐기하고 time exceeded 메시지 전송 가능
  - 프래그먼트 0을 사용할 수 없는 경우에는 time exceeded를 전혀 보낼 필요 없음
  - Code 0은 게이트웨이에서, Code 1은 호스트에서 수신 가능

## Parameter Problem 메시지

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|     Type      |     Code      |          Checksum             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|    Pointer    |                   unused                      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|      Internet Header + 64 bits of Original Data Datagram      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

- IP 필드
  - Destination Address: 원본 데이터그램의 출발지 네트워크 및 주소
- ICMP 필드
  - Type: 12
  - Code: 0 = pointer indicates the error
  - Checksum
    - ICMP 메시지의 체크섬, type 필드에서 시작
    - 헤더의 모든 16비트 워드의 1의 보수 합에 대한 16비트 1의 보수
    - 체크섬 계산 시 체크섬 필드는 0이어야 함
  - Pointer: code 0인 경우, 오류가 발견된 옥텟을 식별(옵션의 중간일 수도 있음)
  - Internet Header + 64 bits of Data Datagram
    - 인터넷 헤더에 원본 데이터그램의 옵션 포함
    - 오류와 데이터그램을 적절한 프로세스에 매칭하기 위해 호스트가 사용
    - 상위 계층 프로토콜이 포트 번호를 사용하는 경우, 원본 데이터그램 데이터의 처음 64비트에 해당 정보가 있다고 가정
- 설명
  - 게이트웨이 또는 호스트가 데이터그램 처리 중 헤더 매개변수 문제로 처리를 완료할 수 없는 경우 → 해당 데이터그램 폐기 필요
    - 잠재적 원인 중 하나: 옵션의 잘못된 인수
    - 게이트웨이 또는 호스트는 parameter problem 메시지로 출발지 호스트에게 알림 가능 (오류로 데이터그램이 폐기된 경우에만 전송)
  - Pointer: 오류가 감지된 원본 데이터그램 헤더의 옥텟을 식별(옵션의 중간일 수도 있음)
    - 예: 1은 Type of Service에 문제, 20은(옵션이 있는 경우) 첫 번째 옵션의 type code에 문제
  - Code 0은 게이트웨이 또는 호스트에서 수신 가능

## Source Quench 메시지

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|     Type      |     Code      |          Checksum             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                             unused                            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|      Internet Header + 64 bits of Original Data Datagram      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

- IP 필드
  - Destination Address: 원본 데이터그램의 출발지 네트워크 및 주소
- ICMP 필드
  - Type: 4
  - Code: 0
  - Checksum
    - ICMP 메시지의 체크섬, type 필드에서 시작
    - 헤더의 모든 16비트 워드의 1의 보수 합에 대한 16비트 1의 보수
    - 체크섬 계산 시 체크섬 필드는 0이어야 함
  - Internet Header + 64 bits of Data Datagram
    - 인터넷 헤더에 원본 데이터그램의 옵션 포함
    - 오류와 데이터그램을 적절한 프로세스에 매칭하기 위해 호스트가 사용
    - 상위 계층 프로토콜이 포트 번호를 사용하는 경우, 원본 데이터그램 데이터의 처음 64비트에 해당 정보가 있다고 가정
- 설명
  - 게이트웨이는 목적지 네트워크로의 경로상 다음 네트워크로 출력하기 위해 데이터그램을 큐에 넣을 버퍼 공간이 없는 경우 인터넷 데이터그램 폐기 가능
    - 폐기 시 해당 데이터그램의 출발지 호스트에게 source quench 메시지 전송 가능
  - 목적지 호스트도 데이터그램이 너무 빠르게 도착하여 처리할 수 없는 경우 source quench 메시지 전송 가능
  - source quench 메시지: 호스트에게 인터넷 목적지로 보내는 트래픽 속도를 줄이라는 요청
    - 게이트웨이는 폐기하는 모든 메시지에 대해 source quench 메시지 전송 가능
    - 수신 시 출발지 호스트는 게이트웨이로부터 더 이상 source quench 메시지를 수신하지 않을 때까지 해당 목적지로 보내는 트래픽 속도를 줄여야 함
    - 이후 출발지 호스트는 다시 source quench 메시지를 수신할 때까지 트래픽 전송 속도를 점진적으로 증가시킬 수 있음
  - 게이트웨이 또는 호스트는 용량 초과를 기다리지 않고 용량 한계에 접근할 때 source quench 메시지 전송 가능 → 이 경우 메시지를 유발한 데이터 데이터그램 자체는 전달될 수도 있음
  - Code 0은 게이트웨이 또는 호스트에서 수신 가능
  - 참고: Source Quench(Type 4)는 혼잡 제어 메커니즘으로서 효과가 없고 악용될 수 있다는 이유로 RFC 6633에 의해 공식 폐기(deprecated) → 라우터와 호스트는 이 메시지를 생성하지 않아야 함(SHOULD NOT)

## Redirect 메시지

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|     Type      |     Code      |          Checksum             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                 Gateway Internet Address                      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|      Internet Header + 64 bits of Original Data Datagram      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

- IP 필드
  - Destination Address: 원본 데이터그램의 출발지 네트워크 및 주소
- ICMP 필드
  - Type: 5
  - Code
    - 0 = Redirect datagrams for the Network
    - 1 = Redirect datagrams for the Host
    - 2 = Redirect datagrams for the Type of Service and Network
    - 3 = Redirect datagrams for the Type of Service and Host
  - Checksum
    - ICMP 메시지의 체크섬, type 필드에서 시작
    - 헤더의 모든 16비트 워드의 1의 보수 합에 대한 16비트 1의 보수
    - 체크섬 계산 시 체크섬 필드는 0이어야 함
  - Gateway Internet Address: redirect에서 지정된 네트워크에 대한 트래픽이 전송되어야 하는 게이트웨이의 주소
  - Internet Header + 64 bits of Data Datagram
    - 인터넷 헤더에 원본 데이터그램의 옵션 포함
    - 오류와 데이터그램을 적절한 프로세스에 매칭하기 위해 호스트가 사용
    - 상위 계층 프로토콜이 포트 번호를 사용하는 경우, 원본 데이터그램 데이터의 처음 64비트에 해당 정보가 있다고 가정
- 설명
  - 게이트웨이가 호스트에게 redirect 메시지를 전송하는 상황
    - 게이트웨이 G1이 자신이 연결된 네트워크상의 호스트로부터 인터넷 데이터그램 수신
    - G1이 라우팅 테이블을 확인해 데이터그램의 인터넷 목적지 네트워크 X로의 경로상 다음 게이트웨이 G2의 주소 획득
    - G2와 데이터그램의 출발지 주소로 식별되는 호스트가 같은 네트워크에 있는 경우 → redirect 메시지가 해당 호스트에게 전송됨
    - redirect 메시지: 호스트에게 네트워크 X로의 트래픽을 게이트웨이 G2로 직접 보내라고 안내 (더 짧은 경로이므로)
    - 게이트웨이는 원본 데이터그램의 데이터를 인터넷 목적지로 전달
  - IP source route 옵션이 있고 목적지 주소 필드에 게이트웨이 주소가 있는 데이터그램의 경우, source route의 다음 주소보다 최종 목적지까지 더 나은 경로가 있더라도 redirect 메시지는 전송되지 않음
  - Code 0, 1, 2, 3은 게이트웨이에서 수신 가능

## Echo 또는 Echo Reply 메시지

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|     Type      |     Code      |          Checksum             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|           Identifier          |        Sequence Number        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|     Data ...
+-+-+-+-+-
```

- IP 필드
  - Addresses: echo 메시지의 출발지 주소가 echo reply 메시지의 목적지가 됨
    - echo reply 메시지 구성: 출발지·목적지 주소를 서로 바꾸고, type code를 0으로 변경하고, 체크섬 재계산
  - Type: echo 메시지 8, echo reply 메시지 0
  - Code: 0
  - Checksum
    - ICMP 메시지의 체크섬, type 필드에서 시작
    - 헤더의 모든 16비트 워드의 1의 보수 합에 대한 16비트 1의 보수
    - 체크섬 계산 시 체크섬 필드는 0이어야 함
  - Identifier: echo 발신자가 응답을 요청과 매칭하는 데 사용 가능(값은 0일 수 있음)
  - Sequence Number: echo 발신자가 응답을 요청과 매칭하는 데 사용 가능(값은 0일 수 있음)
- 설명
  - echo 메시지에서 수신된 데이터는 echo reply 메시지에서 반환되어야 함
  - identifier와 sequence number는 echo 발신자가 응답을 요청과 매칭하는 데 사용
    - 예: identifier는 TCP·UDP의 포트처럼 세션 식별에 사용, sequence number는 각 echo 요청 전송마다 증가 가능
    - 응답자(echoer)는 echo reply에서 이 값들을 동일하게 반환
  - Code 0은 게이트웨이 또는 호스트에서 수신 가능

## Timestamp 또는 Timestamp Reply 메시지

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|     Type      |      Code     |          Checksum             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|           Identifier          |        Sequence Number        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|     Originate Timestamp                                       |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|     Receive Timestamp                                         |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|     Transmit Timestamp                                        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

- IP 필드
  - Addresses: timestamp 메시지의 출발지 주소가 timestamp reply 메시지의 목적지가 됨
    - timestamp reply 메시지 구성: 출발지·목적지 주소를 서로 바꾸고, type code를 14로 변경하고, 체크섬 재계산
  - Type: timestamp 메시지 13, timestamp reply 메시지 14
  - Code: 0
  - Checksum
    - ICMP 메시지의 체크섬, type 필드에서 시작
    - 헤더의 모든 16비트 워드의 1의 보수 합에 대한 16비트 1의 보수
    - 체크섬 계산 시 체크섬 필드는 0이어야 함
  - Identifier: timestamp 발신자가 응답을 요청과 매칭하는 데 사용 가능(값은 0일 수 있음)
  - Sequence Number: timestamp 발신자가 응답을 요청과 매칭하는 데 사용 가능(값은 0일 수 있음)
- 설명
  - 메시지에서 수신된 timestamp는 추가 timestamp와 함께 응답에서 반환
    - timestamp: 자정 UT 이후의 밀리초를 나타내는 32비트 값
  - Originate Timestamp: 발신자가 메시지 전송 전 마지막으로 처리한 시간
  - Receive Timestamp: 응답자(echoer)가 수신 시 처음으로 처리한 시간
  - Transmit Timestamp: 응답자가 메시지 전송 전 마지막으로 처리한 시간
  - 시간을 밀리초 단위 또는 자정 UT 기준으로 제공할 수 없는 경우 → timestamp의 최상위 비트를 설정해 비표준 값임을 나타내는 한, 임의의 시간을 timestamp에 삽입 가능
  - identifier와 sequence number는 echo 발신자가 응답을 요청과 매칭하는 데 사용
    - 예: identifier는 TCP·UDP의 포트처럼 세션 식별에 사용, sequence number는 각 요청 전송마다 증가 가능
    - 목적지는 응답에서 이 값들을 동일하게 반환
  - Code 0은 게이트웨이 또는 호스트에서 수신 가능

## Information Request 또는 Information Reply 메시지

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|     Type      |      Code     |          Checksum             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|           Identifier          |        Sequence Number        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

- IP 필드
  - Addresses: information request 메시지의 출발지 주소가 information reply 메시지의 목적지가 됨
    - information reply 메시지 구성: 출발지·목적지 주소를 서로 바꾸고, type code를 16으로 변경하고, 체크섬 재계산
  - Type: information request 메시지 15, information reply 메시지 16
  - Code: 0
  - Checksum
    - ICMP 메시지의 체크섬, type 필드에서 시작
    - 헤더의 모든 16비트 워드의 1의 보수 합에 대한 16비트 1의 보수
    - 체크섬 계산 시 체크섬 필드는 0이어야 함
  - Identifier: information 발신자가 응답을 요청과 매칭하는 데 사용 가능(값은 0일 수 있음)
  - Sequence Number: information 발신자가 응답을 요청과 매칭하는 데 사용 가능(값은 0일 수 있음)
- 설명
  - 이 메시지는 IP 헤더의 출발지·목적지 주소 필드에서 출발지 네트워크를 0("this" 네트워크를 의미)으로 설정해 전송 가능
    - 응답하는 IP 모듈은 주소가 완전히 지정된 응답을 보내야 함
  - 이 메시지: 호스트가 자신이 속한 네트워크의 번호를 알아내기 위한 방법
  - identifier와 sequence number는 echo 발신자가 응답을 요청과 매칭하는 데 사용
    - 예: identifier는 TCP·UDP의 포트처럼 세션 식별에 사용, sequence number는 각 요청 전송마다 증가 가능
    - 목적지는 응답에서 이 값들을 동일하게 반환
  - Code 0은 게이트웨이 또는 호스트에서 수신 가능
  - 참고: Information Request/Reply(Type 15/16)는 실질적으로 사용되지 않게 되어 RFC 6918에 의해 공식 폐기(deprecated) → 호스트는 이 메시지를 생성하지 않아야 함(SHOULD NOT)

## 메시지 타입 요약(Summary of Message Types)

- 0: Echo Reply
- 3: Destination Unreachable
- 4: Source Quench
- 5: Redirect
- 8: Echo
- 11: Time Exceeded
- 12: Parameter Problem
- 13: Timestamp
- 14: Timestamp Reply
- 15: Information Request
- 16: Information Reply

## 참고 문헌(References)

- [1] Postel, J. (ed.), "Internet Protocol - DARPA Internet Program Protocol Specification," RFC 791, USC/Information Sciences Institute, September 1981.
- [2] Postel, J. (ed.), "Internet Protocol - DARPA Internet Program Protocol Specification," RFC 791, USC/Information Sciences Institute, September 1981.
- [3] Hinden, R., and A. Sheltzer, "The DARPA Internet Gateway," RFC 823, BBN, September 1982.
