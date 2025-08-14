# SSL/TLS

**SSL(Secure Sockets Layer)**는 과거에 사용되던 보안 프로토콜로, 현재는 보안상의 이유로 더 이상 사용되지 않지만 관용적으로 'SSL'이라는 용어가 TLS와 혼용되어 사용됨

현재는 **TLS(Transport Layer Security)**가 표준으로 사용되며, TLS 1.2와 TLS 1.3이 널리 사용됨

TLS 1.2 → TLS 1.3 업그레이드 시 주요 차이점 중 하나는 핸드셰이크 과정의 간소화:

- TLS 1.2는 핸드셰이크 과정에서 왕복(RTT)이 2회 필요함.
- TLS 1.3은 핸드셰이크 과정을 간소화해 1-RTT 또는 0-RTT 통신이 가능함.
- 그 결과 연결 설정 시간이 줄어들고, 보안성 또한 강화됨.

### 과정 1.3 기준

1. ClientHello

- 클라이언트가 다음 정보를 포함한 ClientHello 메시지를 서버에 보낸다

- 클라이언 지원 암호 스위트 목록
- 키 공유(공개키) 클라이언트가 선택한 키 교환 알고리즘의 공개 키 제공
- 지원 가능한 버전
- Server Name Indacation(SNI) 접속하려는 호스트 이름

2. ServerHello

- 서버가 선택한 암호 스위트
- 서버의 공개 키
- 선택된 TLS 버전

- 이후 서버는 이를 순차적으로 보낸다.

  - Encrypted Extensions: 추가 확장 정보 (예: ALPN, SNI 응답 등)
  - Certificate: 서버의 인증서 (서버 신원을 보장)
  - Certificate Verify: 서버가 해당 인증서를 소유하고 있다는 증거

- Finished: 서버 측 핸드셰이크 완료 메시지 (서명 포함)

3. Client Finished

- 클라이언트는 서버의 인증서를 검증하고 Finished 메시지를 보낸다.
- 이 메시지도 암호화되어 전송하며, 이제 암호화된 통신으로 통신함

```bash
   Client                                             Server
     │                                                  │
     │ --- ClientHello (암호 스위트, Key Share, 등) ---> │
     │                                                  │
     │                             <--- ServerHello (암호 스위트, Key Share, 버전 선택)
     │                             <--- Encrypted Extensions (ALPN, SNI 응답 등)
     │                             <--- Certificate (서버 인증서)
     │                             <--- Certificate Verify (인증서 소유 증명)
     │                             <--- Finished (서버 핸드셰이크 완료)
     │                                                  │
     │ --- Finished (클라이언트 핸드셰이크 완료) ------> │
     │                                                  │
     │             이후, 암호화된 Application Data 전송              │

```
