# RFC 8446 - The Transport Layer Security (TLS) Protocol Version 1.3

> 발행일: 2018년 8월
> 대체 문서: RFC 5077, RFC 5246 (TLS 1.2) 등
> 상태: Standards Track

## 1. 개요

RFC 8446은 TLS(Transport Layer Security) 프로토콜의 최신 버전인 TLS 1.3 을 정의합니다. HTTPS의 핵심 보안 프로토콜로, 클라이언트-서버 간 통신의 기밀성(confidentiality) 과 무결성(integrity) 을 보장합니다.

### TLS가 제공하는 세 가지 보안 보장

| 보장 | 설명 |
|------|------|
| 서버 인증 | 클라이언트가 서버의 신원을 확인 (클라이언트 인증은 선택) |
| 데이터 기밀성 | 전송 데이터의 도청 방지 |
| 메시지 무결성 | 데이터 변조 및 위조 감지 |

### TLS 1.3의 주요 개선사항

```
TLS 1.2 대비 개선:
- 핸드셰이크 간소화 (1-RTT, 0-RTT 지원)
- 레거시 알고리즘 제거
- 모든 핸드셰이크 암호화 강화
- Forward Secrecy 필수화
```

## 2. 핸드셰이크 프로토콜

### 2.1 핸드셰이크 개요

TLS 1.3 핸드셰이크는 세 단계 로 구성됩니다:

```
       Client                                           Server

Key    ClientHello
Exch   + key_share*
       + signature_algorithms*
       + psk_key_exchange_modes*
       + pre_shared_key*       -------->
                                                   ServerHello
                                                   + key_share*
                                                   + pre_shared_key*
                                         {EncryptedExtensions}
                                         {CertificateRequest*}
                                                {Certificate*}
                                          {CertificateVerify*}
                                                    {Finished}
                               <--------  [Application Data*]
       {Certificate*}
       {CertificateVerify*}
       {Finished}              -------->
       [Application Data]      <------->  [Application Data]
```

범례:
- `+` : 메시지 내 확장(extension)
- `*` : 선택적/상황별 전송
- `{}` : 핸드셰이크 키로 암호화
- `[]` : 애플리케이션 데이터 키로 암호화

### 2.2 세 단계 상세

#### 1단계: Key Exchange (키 교환)

```
Client → Server:
- ClientHello
  - 지원하는 암호 스위트 목록
  - 지원하는 그룹 (ECDHE 곡선 등)
  - key_share (DH 공개 키)
  - 서명 알고리즘 목록
  - PSK 정보 (선택)

Server → Client:
- ServerHello
  - 선택된 암호 스위트
  - 선택된 그룹
  - key_share (서버 DH 공개 키)
```

> 중요: 이 단계 이후 모든 것이 암호화 됩니다.

#### 2단계: Server Parameters (서버 파라미터)

```
Server → Client (암호화됨):
- EncryptedExtensions: 추가 확장 정보
- CertificateRequest: 클라이언트 인증서 요청 (선택)
```

#### 3단계: Authentication (인증)

```
Server → Client (암호화됨):
- Certificate: 서버 인증서
- CertificateVerify: 개인키로 서명된 검증값
- Finished: 핸드셰이크 완료 MAC

Client → Server (암호화됨):
- Certificate: 클라이언트 인증서 (요청된 경우)
- CertificateVerify: 클라이언트 서명 (인증서 전송 시)
- Finished: 핸드셰이크 완료 MAC
```

### 2.3 1-RTT 핸드셰이크

TLS 1.3의 표준 핸드셰이크는 1-RTT (1 Round Trip Time) 으로 완료됩니다:

```
TLS 1.2: 2-RTT
┌────────┐         ┌────────┐
│ Client │         │ Server │
└───┬────┘         └───┬────┘
    │ ClientHello       │
    │──────────────────>│
    │      ServerHello  │
    │<──────────────────│  1-RTT
    │      Certificate  │
    │<──────────────────│
    │        Finished   │
    │<──────────────────│
    │ ClientKeyExchange │
    │──────────────────>│  2-RTT
    │        Finished   │
    │──────────────────>│
    │   Application     │
    │<────────────────>│

TLS 1.3: 1-RTT
┌────────┐         ┌────────┐
│ Client │         │ Server │
└───┬────┘         └───┬────┘
    │ ClientHello      │
    │ + key_share      │
    │──────────────────>│
    │ ServerHello      │
    │ + key_share      │
    │ {EncryptedExt}   │  1-RTT
    │ {Certificate}    │
    │ {CertVerify}     │
    │ {Finished}       │
    │<──────────────────│
    │ {Finished}       │
    │──────────────────>│
    │   Application    │
    │<────────────────>│
```

### 2.4 0-RTT 모드 (Early Data)

이전 연결에서 공유된 PSK를 사용하여 첫 메시지와 함께 데이터 전송:

```
Client                                           Server

ClientHello
+ early_data
+ key_share*
+ psk_key_exchange_modes
+ pre_shared_key
(Application Data*)     -------->
                                            ServerHello
                                           + pre_shared_key
                                               + key_share*
                                      {EncryptedExtensions}
                                              + early_data*
                                                 {Finished}
                        <--------       [Application Data*]
(EndOfEarlyData)
{Finished}              -------->
[Application Data]      <------->        [Application Data]
```

0-RTT 주의사항:
> - Early data는 Forward Secret이 아님
> - 재전송 공격 보호가 없음 (연결 간 non-replay 보장 없음)
> - 멱등(idempotent) 요청에만 사용 권장

## 3. 암호화 스위트 (Cipher Suites)

### 3.1 TLS 1.3의 변화

TLS 1.3은 레거시 알고리즘을 모두 제거 했습니다:

```
제거된 알고리즘:
- RSA 키 교환 (Forward Secrecy 없음)
- 정적 DH (Forward Secrecy 없음)
- CBC 모드 암호화
- RC4, DES, 3DES
- MD5, SHA-1 (핸드셰이크)
- 압축
```

### 3.2 암호 스위트 구성

TLS 1.3에서 암호 스위트는 다음만 정의:
- AEAD 알고리즘 (레코드 보호)
- 해시 함수 (키 유도, MAC)

키 교환과 인증은 별도 협상됩니다.

### 3.3 필수 및 권장 스위트

| 스위트 | 설명 |
|--------|------|
| TLS_AES_128_GCM_SHA256 | 필수 구현 |
| TLS_AES_256_GCM_SHA384 | 권장 |
| TLS_CHACHA20_POLY1305_SHA256 | 권장 |
| TLS_AES_128_CCM_SHA256 | IoT 환경용 |
| TLS_AES_128_CCM_8_SHA256 | IoT 환경용 (태그 축소) |

### 3.4 AEAD 알고리즘

| 알고리즘 | 키 길이 | Nonce | 태그 |
|----------|---------|-------|------|
| AES-128-GCM | 128비트 | 96비트 | 128비트 |
| AES-256-GCM | 256비트 | 96비트 | 128비트 |
| ChaCha20-Poly1305 | 256비트 | 96비트 | 128비트 |
| AES-128-CCM | 128비트 | 96비트 | 128비트 |

## 4. 키 교환 메커니즘

### 4.1 세 가지 모드

TLS 1.3은 세 가지 키 교환 모드를 지원합니다:

| 모드 | Forward Secrecy | 설명 |
|------|:---------------:|------|
| (EC)DHE | ✓ | Diffie-Hellman (타원 곡선 또는 유한 필드) |
| PSK-only | ✗ | 사전 공유 키만 사용 |
| PSK + (EC)DHE | ✓ | 사전 공유 키 + DH |

### 4.2 Forward Secrecy

```
Forward Secrecy (전방 비밀성):
서버의 장기 비밀키가 유출되어도
과거 세션의 암호화된 데이터는 복호화 불가

TLS 1.2: RSA 키 교환 시 Forward Secrecy 없음
TLS 1.3: 모든 공개키 기반 교환이 (EC)DHE → Forward Secrecy 보장
```

### 4.3 지원 그룹

#### ECDHE (타원 곡선)

| 그룹 | 보안 수준 | 비고 |
|------|-----------|------|
| x25519 | 128비트 | 권장, 빠름 |
| x448 | 224비트 | 고보안 |
| secp256r1 (P-256) | 128비트 | 널리 지원 |
| secp384r1 (P-384) | 192비트 | |
| secp521r1 (P-521) | 256비트 | |

#### FFDHE (유한 필드)

| 그룹 | 크기 | 보안 수준 |
|------|------|-----------|
| ffdhe2048 | 2048비트 | ~112비트 |
| ffdhe3072 | 3072비트 | ~128비트 |
| ffdhe4096 | 4096비트 | ~152비트 |
| ffdhe6144 | 6144비트 | ~176비트 |
| ffdhe8192 | 8192비트 | ~200비트 |

### 4.4 키 유도 (Key Derivation)

TLS 1.3은 HKDF (HMAC-based Extract-and-Expand Key Derivation Function) 를 사용합니다:

```
                              0
                              |
                              v
                    PSK ->  HKDF-Extract = Early Secret
                              |
                              +-----> Derive-Secret(., "ext binder" | "res binder", "")
                              |                     = binder_key
                              |
                              +-----> Derive-Secret(., "c e traffic", ClientHello)
                              |                     = client_early_traffic_secret
                              |
                              +-----> Derive-Secret(., "e exp master", ClientHello)
                              |                     = early_exporter_master_secret
                              v
                        Derive-Secret(., "derived", "")
                              |
                              v
               (EC)DHE ->  HKDF-Extract = Handshake Secret
                              |
                              +-----> Derive-Secret(., "c hs traffic",
                              |                     ClientHello...ServerHello)
                              |                     = client_handshake_traffic_secret
                              |
                              +-----> Derive-Secret(., "s hs traffic",
                              |                     ClientHello...ServerHello)
                              |                     = server_handshake_traffic_secret
                              v
                        Derive-Secret(., "derived", "")
                              |
                              v
                     0 ->  HKDF-Extract = Master Secret
                              |
                              +-----> Derive-Secret(., "c ap traffic",
                              |                     ClientHello...server Finished)
                              |                     = client_application_traffic_secret_0
                              |
                              +-----> Derive-Secret(., "s ap traffic",
                              |                     ClientHello...server Finished)
                              |                     = server_application_traffic_secret_0
                              |
                              +-----> Derive-Secret(., "exp master",
                              |                     ClientHello...server Finished)
                              |                     = exporter_master_secret
                              |
                              +-----> Derive-Secret(., "res master",
                                                    ClientHello...client Finished)
                                                    = resumption_master_secret
```

## 5. 인증서 처리

### 5.1 Certificate 메시지

서버(및 선택적으로 클라이언트)의 인증서 체인을 전송합니다:

```
struct {
    opaque certificate_request_context<0..2^8-1>;
    CertificateEntry certificate_list<0..2^24-1>;
} Certificate;

struct {
    select (certificate_type) {
        case RawPublicKey:
            opaque ASN1_subjectPublicKeyInfo<1..2^24-1>;
        case X509:
            opaque cert_data<1..2^24-1>;
    };
    Extension extensions<0..2^16-1>;
} CertificateEntry;
```

인증서 유형:
- X.509 인증서 (기본)
- Raw Public Key
- 캐시된 정보

### 5.2 CertificateVerify 메시지

인증서의 개인키로 핸드셰이크 전체를 서명 합니다:

```
struct {
    SignatureScheme algorithm;
    opaque signature<0..2^16-1>;
} CertificateVerify;
```

서명 대상:
```
Transcript-Hash(Handshake Context, Certificate)
```

### 5.3 서명 알고리즘

`signature_algorithms` 확장으로 지원 알고리즘을 협상합니다:

| 알고리즘 | 코드 | 상태 |
|----------|------|------|
| rsa_pkcs1_sha256 | 0x0401 | 레거시 |
| rsa_pkcs1_sha384 | 0x0501 | 레거시 |
| rsa_pkcs1_sha512 | 0x0601 | 레거시 |
| ecdsa_secp256r1_sha256 | 0x0403 | 권장 |
| ecdsa_secp384r1_sha384 | 0x0503 | 권장 |
| ecdsa_secp521r1_sha512 | 0x0603 | 권장 |
| rsa_pss_rsae_sha256 | 0x0804 | 권장 |
| rsa_pss_rsae_sha384 | 0x0805 | 권장 |
| rsa_pss_rsae_sha512 | 0x0806 | 권장 |
| ed25519 | 0x0807 | 권장 |
| ed448 | 0x0808 | 권장 |
| rsa_pss_pss_sha256 | 0x0809 | 권장 |
| rsa_pss_pss_sha384 | 0x080a | 권장 |
| rsa_pss_pss_sha512 | 0x080b | 권장 |
| rsa_pkcs1_sha1 | 0x0201 | 사용 중단 |
| ecdsa_sha1 | 0x0203 | 사용 중단 |

> 참고: 인증서 인증이 필요할 때 `signature_algorithms` 확장을 생략하면 연결 중단

## 6. 레코드 프로토콜

### 6.1 레코드 구조

```
struct {
    ContentType type;
    ProtocolVersion legacy_record_version;
    uint16 length;
    opaque fragment[TLSPlaintext.length];
} TLSPlaintext;

struct {
    opaque content[TLSPlaintext.length];
    ContentType type;
    uint8 zeros[length_of_padding];
} TLSInnerPlaintext;

struct {
    ContentType opaque_type = application_data; /* 23 */
    ProtocolVersion legacy_record_version = 0x0303; /* TLS v1.2 */
    uint16 length;
    opaque encrypted_record[TLSCiphertext.length];
} TLSCiphertext;
```

### 6.2 암호화 프로세스

```
평문 레코드
    ↓
TLSInnerPlaintext (실제 타입 + 패딩)
    ↓
AEAD 암호화 (nonce + additional_data)
    ↓
TLSCiphertext (타입 = application_data로 위장)
```

### 6.3 Nonce 생성

```
per-record nonce:
    nonce = write_iv XOR (0^(n-8) || sequence_number)

sequence_number: 0부터 시작, 레코드마다 1씩 증가
write_iv: 키 유도에서 생성된 IV
```

### 6.4 패딩

```
┌─────────────────┬──────┬───────────────┐
│    Content      │ Type │   Padding     │
│   (가변 길이)    │ (1B) │  (0+개의 0)   │
└─────────────────┴──────┴───────────────┘
```

패딩 목적:
- 콘텐츠 길이 노출 방지
- 트래픽 분석 방해

### 6.5 키 사용 제한

암호화 키는 일정 횟수 이상 사용하면 갱신해야 합니다:

| 알고리즘 | 권장 한계 |
|----------|-----------|
| AES-GCM | 2^24.5 레코드 |
| ChaCha20-Poly1305 | 무제한 (실용적으로) |

## 7. 보안 고려사항

### 7.1 핸드셰이크 보안

| 속성 | 설명 |
|------|------|
| HKDF 사용 | 키 유도에 HMAC 기반 함수 사용 |
| 키 분리 | 핸드셰이크/애플리케이션/재개용 키 분리 |
| 후손상 보안 | 키 업데이트로 손상된 키 영향 제한 |

### 7.2 0-RTT 제한사항

```
⚠️ 0-RTT (Early Data) 경고:

1. Forward Secret 아님
   - PSK가 노출되면 early data 복호화 가능

2. 재전송 공격 취약
   - 연결 간 동일 early data가 재전송될 수 있음
   - 서버가 0-RTT 데이터를 여러 번 처리할 수 있음

권장사항:
- 멱등(idempotent) 작업만 0-RTT로 전송
- 상태 변경 작업은 1-RTT 완료 후 전송
```

### 7.3 트래픽 분석 방어

```
TLS는 전송 데이터의 길이를 숨기지 않음

방어 수단:
- 레코드 패딩 사용
- 균일한 크기의 레코드 전송
- 더미 트래픽 추가
```

### 7.4 버전 다운그레이드 방지

ServerHello의 random 필드에 다운그레이드 표시자를 포함:

```
TLS 1.3 서버가 TLS 1.2로 협상 시:
random[24..31] = 44 4F 57 4E 47 52 44 01
                  "DOWNGRD" + 0x01

TLS 1.3 서버가 TLS 1.1 이하로 협상 시:
random[24..31] = 44 4F 57 4E 47 52 44 00
                  "DOWNGRD" + 0x00
```

클라이언트는 이 값을 확인하여 다운그레이드 공격 탐지

### 7.5 레코드 계층 보안

| 보호 | 설명 |
|------|------|
| 재전송 방지 | 시퀀스 번호로 중복 레코드 탐지 |
| 인증 암호화 | AEAD로 기밀성 + 무결성 동시 보장 |
| 순서 보장 | 시퀀스 번호로 순서 변조 탐지 |

## 8. TLS 1.2 vs TLS 1.3 비교

| 특성 | TLS 1.2 | TLS 1.3 |
|------|---------|---------|
| 핸드셰이크 RTT | 2-RTT | 1-RTT (0-RTT 가능) |
| 키 교환 | RSA, DHE, ECDHE | (EC)DHE만 (Forward Secrecy 필수) |
| 암호화 | CBC, GCM, CCM | AEAD만 (GCM, CCM, ChaCha20) |
| 해시 | MD5, SHA-1 사용 가능 | SHA-256+ 만 |
| 인증서 암호화 | 평문 | 암호화됨 |
| 세션 재개 | Session ID, Ticket | PSK 기반 |
| 압축 | 지원 | 제거됨 (CRIME 공격 방어) |
| 재협상 | 지원 | 제거됨 |

## 요약

RFC 8446 TLS 1.3은 다음을 제공합니다:

- 1-RTT 핸드셰이크: 지연시간 단축 (0-RTT도 가능)
- 필수 Forward Secrecy: (EC)DHE로만 키 교환
- AEAD 전용: 레거시 암호 제거
- 핸드셰이크 암호화: 인증서 등 민감 정보 보호
- HKDF 키 유도: 안전한 키 분리
- 다운그레이드 방지: 버전 협상 공격 차단

TLS 1.3은 보안을 강화하면서 성능을 개선 한 현대적인 보안 프로토콜입니다.
