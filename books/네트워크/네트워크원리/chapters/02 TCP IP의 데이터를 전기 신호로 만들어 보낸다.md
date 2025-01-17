# 1. 소켓을 작성한다

## 1.1 프로토콜 스택의 내부 구성

- OS에 내장된 네트워크 제어 소프트웨어인 프로토콜 스택과 네트워크용 하드웨어(LAN 어댑터)를 알아본다.
- TCP/IP 소프트웨어는 계층 구조로 되어 있으며, 상위 계층이 하위 계층에 작업을 의뢰하는 방식이다.

**구성 요소:**

- 네트워크 애플리케이션: 웹 브라우저, 메일 클라이언트, 웹 서버, 메일 서버 등
- Socket 라이브러리: 리졸버 내장
- 프로토콜 스택: TCP, UDP, IP
- LAN 드라이버
- LAN 어댑터

**프로토콜 스택 역할:**

- TCP는 데이터를 안정적으로 송수신하고, UDP는 짧은 제어용 데이터를 송수신한다.
- IP는 패킷을 운반하며, 패킷 운반 중 오류는 **ICMP**가 처리하고, MAC 주소는 **ARP**로 확인한다.

## 1.2 소켓의 실체는 통신 제어용 정보

- 프로토콜 스택은 제어 정보를 기록하는 메모리 영역을 가지고 있으며, 여기서 소켓이 제어된다.
- 소켓의 실체는 제어 정보이며, IP 주소, 포트 번호 등의 통신 상태 정보를 포함한다.

## 1.3 Socket을 호출했을 때의 동작

- 소켓 호출 시 메모리 영역이 확보되고, 디스크립터가 애플리케이션에 반환된다.
- 디스크립터는 이후 통신 시 소켓을 식별하는 데 사용된다.

# 2. 서버에 접속한다

## 2.1 접속의 의미

- 소켓 생성 후, **connect**를 호출하여 클라이언트의 IP 주소, 포트 번호를 서버에 전달한다.
- 서버 측에서도 클라이언트와 마찬가지로 IP 주소와 포트 번호를 설정하여 통신 준비를 한다.
- 데이터 송수신을 위한 버퍼 메모리를 확보하며, 접속 동작을 통해 통신이 가능해진다.

## 2.2 헤더를 통한 제어 정보 기록

- 통신 시 제어 정보는 TCP, IP, 이더넷 등의 헤더에 기록되어 송수신된다.
- 제어 정보는 소켓에 기록되어 통신 상태를 관리하지만, 상대측은 헤더로만 제어 정보를 받는다.

## 2.3 접속 동작의 실제

- **connect** 호출 시, TCP 담당 부분에서 송수신처의 포트 번호를 포함한 **SYN 비트**가 설정된 헤더가 생성된다.
- 패킷이 서버에 도착하면 서버 측에서도 **SYN** 및 **ACK** 비트를 사용해 클라이언트에 응답을 보낸다.
- 클라이언트는 응답을 받고 서버와의 연결이 완료되었음을 확인한다.

# 3. 데이터를 송수신한다

## 3.1 프로토콜 스택에 HTTP 리퀘스트 메시지를 넘긴다

- **write**를 호출하여 HTTP 리퀘스트 메시지를 프로토콜 스택에 전달하면, 데이터는 송신 버퍼에 저장된다.
- 데이터를 일정 크기 이상 버퍼에 저장하거나, 시간이 경과하면 송신 동작이 이루어진다.

## 3.2 데이터가 클 때는 분할하여 보낸다

- 데이터가 한 패킷에 들어가지 않는 경우 **MSS**(최대 세그먼트 크기)에 맞춰 데이터를 분할해 전송한다.
- TCP 헤더를 추가하여 송신하고, IP 담당 부분을 통해 송신 동작을 실행한다.

## 3.3 ACK 번호로 패킷 도착 확인

- TCP는 **시퀀스 번호**와 **ACK 번호**를 통해 패킷이 올바르게 도착했는지 확인한다.
- 수신한 데이터가 누락되지 않았다면, 수신 측에서 ACK 번호를 송신 측에 돌려보낸다.

## 3.4 패킷 왕복 시간에 따른 ACK 대기 시간 조정

- ACK 번호가 돌아올 때까지 기다리는 시간을 **타임아웃 값**으로 설정하며, 네트워크 상태에 따라 조정한다.
- 혼잡한 네트워크에서는 대기 시간이 길어질 수 있으며, 이 시간을 적절히 설정해야 효율을 높일 수 있다.

## 3.5 윈도우 제어 방식을 이용한 ACK 번호 관리

- TCP는 **윈도우 제어** 방식으로, ACK 번호를 기다리지 않고 복수의 패킷을 송신할 수 있다.
- 수신 가능한 데이터 양을 미리 통지하여 송신 측이 이를 초과하지 않도록 제어한다.

## 3.6 ACK 번호와 윈도우 통지를 함께 보낸다

- 수신 측은 ACK 번호와 윈도우 통지를 가능한 한 합쳐서 한 번에 송신한다.
- 여러 ACK 번호가 발생한 경우, 최종 ACK 번호만 보내고 중간 값은 생략한다.

## 3.7 HTTP 응답 메시지를 수신한다

- 응답 메시지가 돌아오면, 수신 버퍼에 저장된 데이터를 애플리케이션에 전달한다.
- 데이터가 도착하지 않으면 잠시 대기하며, 응답이 도착하면 즉시 처리한다.

# 4. 서버에서 연결을 끊어 소켓을 말소한다

## 4.1 데이터 송신 완료 후 연결을 끊는다

- 데이터 송수신이 완료되면 **close**를 호출하여 연결을 끊는다.
- 서버 측에서 먼저 close를 호출하고, 클라이언트는 이를 수신한 후 응답을 보내며 연결이 종료된다.

## 4.2 소켓을 말소한다

- 클라이언트와 서버의 데이터 송수신이 모두 완료되면, 소켓이 말소된다.

# 5. IP와 - IP 담당 부분은 패킷을 생성하고, 이더넷을 통해 신호를 송수신한다.

## 5.1 패킷의 기본

- 패킷은 헤더와 데이터의 부분으로 구성
  (a) 패킷의 기본형 헤더, 데이터
  (b)TCP/IP의 패킷 MAC 헤더, IP 헤더 , TCP 헤더, 데이터 조각
  MAC 헤더: 이더넷의 제어 정보
  IP 헤더: IP의 제어 정보
  TCP 헤더:보통 TCP 헤더와 데이터 조각이 패킷의 내용이 된다.

- 송신처와 수신처를 명확하게 구별하지 않는 쪽이 편리할 수 있다.
  엔드노드
  서브넷은 라우터와 허브라는 두 종류의 패킷 중계 장치에서 다음과 같은 역할을 분담하면서 패킷을 운반한다.

1. 라우터가 목적지를 확인하여 다음 라우터를 나타낸다.
2. 허브가 서브넷 안에서 패킷을 운반하여 다음 라우터에 도착한다.
   허브는 이더넷의 규칙에 따라 패킷을 운반하고, 라우터는 IP의 규칙에 따라 패킷을 운반하기 때문에 위의 설명을 다음과 같이 바꾸어 말할 수 있다.
3. IP가 목적지를 확인하여 다음 IP의 중계 장치를 나타낸다.
4. 서브넷 안에 있는 이더넷이 중계 장치까지 패킷을 운반한다.

## 5.1 패킷의 기본

- 패킷은 헤더와 데이터로 구성되며, 각각 제어 정보와 실제 데이터를 담고 있다.
  - (a) 패킷의 기본형: 헤더와 데이터로 구성
  - (b) TCP/IP 패킷 구조: MAC 헤더, IP 헤더, TCP 헤더, 데이터 조각으로 구성

**각 헤더의 역할:**

- **MAC 헤더**: 이더넷의 제어 정보를 포함하며, 물리적 네트워크 주소(MAC 주소)를 기반으로 데이터를 송수신한다.
- **IP 헤더**: IP 계층의 제어 정보를 포함하여 송신처와 수신처 IP 주소를 기록한다.
- **TCP 헤더**: TCP 계층에서 신뢰성 있는 연결을 위해 사용되며, 시퀀스 번호와 ACK 번호, 포트 번호 등의 정보가 포함된다.
- **데이터 조각**: 실제 전송할 데이터의 내용이 담긴 부분이다.

- 패킷 송수신 시 송신처와 수신처의 네트워크 주소를 명확히 구별하여 통신이 이루어지지만, 내부 동작을 단순화하기 위해 송수신처의 구분 없이 엔드노드 간의 데이터 교환이 이루어질 수 있다.

### 엔드노드와 서브넷의 역할

- **엔드노드**: 통신의 출발지 또는 도착지를 의미하며, 주로 컴퓨터나 서버가 해당한다.
- **서브넷**: 라우터와 허브의 도움을 받아 패킷을 중계하는 네트워크 내부의 구조이다. 라우터와 허브는 패킷 중계 장치로서 각각의 역할을 수행한다:
  1. **라우터**는 목적지 IP 주소를 기반으로 패킷이 향할 다음 라우터를 결정한다.
  2. **허브**는 이더넷 규칙에 따라 서브넷 내에서 패킷을 전달하여 라우터에 도착하게 한다.

패킷 중계 과정은 아래와 같이 설명할 수 있다:

1. IP 계층이 목적지 IP 주소를 확인하고, 다음에 중계할 IP 장치를 결정한다.
2. 이더넷 계층에서 해당 장치까지 패킷을 운반한다.

## 5.2 패킷 송수신 동작의 개요

패킷 송수신 과정은 여러 단계로 이루어지며, 각각의 계층에서 제어 정보가 추가되고 데이터가 처리된다. 데이터는 송신처에서 수신처까지 물리적 네트워크를 통해 전달된다.

## 5.3 수신처 IP 주소를 기록한 IP 헤더를 만든다.

IP 계층에서는 패킷의 수신처 IP 주소를 IP 헤더에 기록한다. 이를 통해 데이터가 정확한 목적지에 도착할 수 있다. IP 주소는 라우팅 과정에서 매우 중요한 역할을 한다.

## 5.4 이더넷용 MAC 헤더를 만든다.

이더넷 계층에서는 수신처의 MAC 주소를 기반으로 이더넷 헤더를 생성한다. MAC 헤더는 패킷이 로컬 네트워크 상에서 이동할 때 사용되며, 이더넷 규칙을 따른다.

## 5.5 ARP로 수신처 라우터의 MAC 주소를 조사한다.

수신처 라우터의 MAC 주소를 알아내기 위해 **ARP**(Address Resolution Protocol)를 사용한다. ARP는 IP 주소를 MAC 주소로 변환하는 역할을 하며, 로컬 네트워크 상의 장치 간 통신을 가능하게 한다.

## 5.6 이더넷의 기본

이더넷은 네트워크 상에서 데이터를 전송하기 위한 규칙(프로토콜)을 정의한다. 이더넷 프레임은 MAC 주소와 이더넷 헤더를 포함하며, 네트워크 상에서의 데이터 전송을 담당한다.

## 5.7 IP 패킷을 전기나 빛의 신호로 변환하여 송신한다.

패킷은 물리 계층에서 전기적 신호나 광신호로 변환되어 케이블이나 광섬유를 통해 전송된다. 이 과정에서 데이터는 신호로 변환되며, 네트워크 장치를 통해 전송된다.

## 5.8 패킷에 3개의 제어용 데이터를 추가한다.

송신 전에 패킷에는 TCP, IP, MAC 헤더 등 3개의 제어용 데이터가 추가된다. 이 정보는 각 계층에서 패킷을 처리하는 데 필요한 정보이며, 각 계층은 다음 계층으로 데이터를 넘기면서 헤더를 추가하게 된다.

## 5.9 허브를 향해 패킷을 송신한다.

송신된 패킷은 네트워크의 허브 장치로 전달된다. 허브는 패킷을 받아들이고, 이를 서브넷 내에서 다른 장치로 전송한다. 허브는 이더넷 규칙에 따라 패킷을 중계한다.

## 5.10 돌아온 패킷을 받는다.

패킷이 수신처에서 응답을 보내면, 송신처는 돌아온 패킷을 받는다. 이 패킷은 수신된 데이터를 포함하며, 이를 기반으로 통신 상태를 확인하고 다음 작업을 진행한다.

## 5.11 서버의 응답 패킷을 IP에서 TCP로 넘긴다.

서버로부터 응답 패킷을 수신한 후, IP 계층에서 TCP 계층으로 데이터를 전달한다. TCP 계층은 응답 패킷을 확인하고, 이를 애플리케이션에 전달한다.

# 6. UDP 프로토콜을 이용한 송수신 동작

## 6.1 수정 송신이 필요없는 데이터의 송신은 UDP가 효율적이다

UDP는 TCP와 달리 신뢰성 있는 연결을 보장하지 않으며, 오류 검출 및 재전송 기능이 없다. 그러나 짧은 데이터나 수정이 필요 없는 데이터를 전송할 때는 TCP보다 더 효율적이다. 예를 들어, 실시간 통신이나 멀티캐스트 통신에서 UDP가 자주 사용된다.

## 6.2 제어용 짧은 데이터

UDP는 짧은 제어용 데이터를 전송하는 데 적합하다. 예를 들어, DNS 요청이나 간단한 제어 메시지 등을 보낼 때 UDP를 사용하면 빠른 통신이 가능하다.

## 6.3 음성 및 동영상 데이터

음성 및 동영상 스트리밍에서는 실시간으로 데이터를 전송해야 하기 때문에, 오류 발생 시 재전송하는 것이 비효율적이다. 이때는 UDP를 사용하여 실시간 데이터를 전송함으로써 지연을 최소화할 수 있다. 실시간 미디어 스트리밍, VoIP(인터넷 전화), 온라인 게임 등에서도 UDP가 많이 사용된다.
