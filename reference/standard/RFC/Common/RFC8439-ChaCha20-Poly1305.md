# RFC 8439 - IETF 프로토콜을 위한 ChaCha20 및 Poly1305

> 원제: ChaCha20 and Poly1305 for IETF Protocols
> 저자: Y. Nir (Check Point), A. Langley (Google Inc.)
> 분류: Informational
> 발행일: 2018년 6월
> 대체 문서: RFC 7539

## 초록

이 문서는 ChaCha20 스트림 암호와 Poly1305 인증자를 개별적으로, 그리고 AEAD 알고리즘으로 결합된 형태로 정의한다. 이 문서는 이 알고리즘을 참조하는 IETF 프로토콜에 안정적인 참조를 제공하기 위해 RFC 7539를 대체한다.

---

## 1. 서론

이 RFC는 AES 기반 암호화의 한계를 해결하기 위해 설계된 세 가지 암호 알고리즘을 정의한다. AES가 현대 암호학을 지배하고 있지만, 전용 하드웨어가 없는 플랫폼에서 성능 단점이 있고 캐시 충돌 타이밍 공격에 대한 취약점이 있다.

세 가지 핵심 알고리즘:

1. **ChaCha20 암호**: 소프트웨어 전용 구현에서 AES보다 약 3배 빠른 고속 스트림 암호
2. **Poly1305 인증자**: 구현이 간단한 고속 메시지 인증 코드
3. **CHACHA20-POLY1305 AEAD**: 연관 데이터를 포함한 인증 암호화 구성

이 문서는 RFC 7539를 대체하여 정오표를 병합하고 보안 고려사항을 확장함으로써 IETF 프로토콜에 안정적인 참조를 제공한다. 이 알고리즘들은 D.J. Bernstein의 학술 논문에서 유래했으며 엄격한 암호 분석을 거쳤다.

### 관례

- RFC 2119 키워드("MUST", "SHOULD" 등)가 대문자로 사용될 때 적용된다.
- ChaCha 상태는 16 요소 벡터 또는 시각화를 위한 4×4 행렬로 표현된다.
- 모든 요소는 32비트 부호 없는 정수이다.

---

## 2. 알고리즘

### 2.1 ChaCha 쿼터 라운드

기본 연산은 네 개의 32비트 부호 없는 정수(a, b, c, d)를 다음 시퀀스로 처리한다:

```
a += b; d ^= a; d <<<= 16;
c += d; b ^= c; b <<<= 12;
a += b; d ^= a; d <<<= 8;
c += d; b ^= c; b <<<= 7;
```

여기서 "+"는 모듈러 2^32 덧셈, "^"는 비트별 XOR, "<<<"는 왼쪽 회전을 나타낸다.

**테스트 벡터 예시:**
- 입력: a=0x11111111, b=0x01020304, c=0x9b8d6f43, d=0x01234567
- 출력: a=0xea2a92f4, b=0xcb1cf8ce, c=0x4581472e, d=0x5881c4bb

### 2.2 ChaCha 상태에 대한 쿼터 라운드

상태는 16개의 위치에서 동작한다. 연산은 QUARTERROUND(x, y, z, w) 표기법으로 미리 결정된 네 개의 인덱스를 대상으로 한다. "컬럼 라운드"는 위치 (1, 5, 9, 13)을, "대각선 라운드"는 (2, 7, 8, 13)과 같은 위치를 대상으로 한다.

### 2.3 ChaCha20 블록 함수

**입력:**
- 256비트 키 (8개의 32비트 리틀 엔디안 정수)
- 96비트 논스 (3개의 32비트 리틀 엔디안 정수)
- 32비트 블록 카운터

**출력:** 64바이트의 랜덤하게 보이는 바이트

**상태 초기화:**

```
상수       상수       상수       상수
키         키         키         키
키         키         키         키
블록카운터  논스       논스       논스
```

- 워드 0~3: 상수 (0x61707865, 0x3320646e, 0x79622d32, 0x6b206574)
  - ASCII로 "expand 32-byte k"
- 워드 4~11: 256비트 키를 리틀 엔디안 청크로
- 워드 12: 블록 카운터
- 워드 13~15: 논스

**라운드 구조:** 총 20라운드 (컬럼/대각선 쌍의 10회 반복):

```
컬럼 라운드:                    대각선 라운드:
QUARTERROUND(0, 4,  8, 12)     QUARTERROUND(0, 5, 10, 15)
QUARTERROUND(1, 5,  9, 13)     QUARTERROUND(1, 6, 11, 12)
QUARTERROUND(2, 6, 10, 14)     QUARTERROUND(2, 7,  8, 13)
QUARTERROUND(3, 7, 11, 15)     QUARTERROUND(3, 4,  9, 14)
```

**의사 코드:**

```
chacha20_block(key, counter, nonce):
   state = constants | key | counter | nonce
   initial_state = state
   for i = 1 upto 10
      inner_block(state)    // 컬럼 라운드 + 대각선 라운드
   state += initial_state   // 모듈러 2^32 워드별 덧셈
   return serialize(state)  // 리틀 엔디안 직렬화
```

마지막 단계에서 원래 상태를 출력에 더한 후(모듈러 2^32), 리틀 엔디안으로 직렬화한다.

### 2.4 ChaCha20 암호화 알고리즘

**입력:**
- 256비트 키
- 32비트 초기 카운터
- 96비트 논스
- 임의 길이 평문

**동작:** 연속적인 카운터 값으로 블록 함수를 호출하여 키스트림 블록을 생성한 후, 평문과 키스트림을 XOR한다. 복호화도 동일한 과정을 사용한다.

**의사 코드:**

```
chacha20_encrypt(key, counter, nonce, plaintext):
   for j = 0 upto floor(len(plaintext)/64) - 1
      key_stream = chacha20_block(key, counter+j, nonce)
      block = plaintext[(j*64)..(j*64+63)]
      encrypted_message += block ^ key_stream
   if ((len(plaintext) % 64) != 0)
      j = floor(len(plaintext)/64)
      key_stream = chacha20_block(key, counter+j, nonce)
      block = plaintext[(j*64)..len(plaintext)-1]
      encrypted_message += (block ^ key_stream)[0..len(plaintext)%64]
   return encrypted_message
```

### 2.5 Poly1305 알고리즘

Poly1305는 32바이트 키와 임의 길이 메시지를 받아 16바이트 인증 태그를 생성하는 일회용 인증자이다.

**키 구조:** 키는 두 개의 128비트 부분으로 나뉜다: "r"과 "s". "r" 값은 사용 전에 클램핑이 필요하다:

- r[3], r[7], r[11], r[15]: 상위 4비트가 0이어야 한다
- r[4], r[8], r[12]: 하위 2비트가 0이어야 한다

**클램핑 코드:**

```c
void poly1305_clamp(unsigned char r[16]) {
   r[3]  &= 15;
   r[7]  &= 15;
   r[11] &= 15;
   r[15] &= 15;
   r[4]  &= 252;
   r[8]  &= 252;
   r[12] &= 252;
}
```

**처리 과정:**

1. 메시지를 16바이트 블록으로 나눈다
2. 각 블록에 대해: 리틀 엔디안 숫자로 읽고, 블록 길이가 k일 때 2^(8k)를 더하고, 누산기에 더한다
3. 누산기에 r을 곱하고, p(p = 2^130 - 5)로 모듈러 감소한다
4. 최종 누산기에 s를 더한다
5. 최하위 128비트를 리틀 엔디안으로 직렬화한다

**의사 코드:**

```
clamp(r): r &= 0x0ffffffc0ffffffc0ffffffc0fffffff

poly1305_mac(msg, key):
   r = le_bytes_to_num(key[0..15])
   clamp(r)
   s = le_bytes_to_num(key[16..31])
   a = 0  /* 누산기 */
   p = (1<<130) - 5
   for i = 1 upto ceil(msg length in bytes / 16)
      n = le_bytes_to_num(msg[((i-1)*16)..(i*16)] | [0x01])
      a += n
      a = (r * a) % p
   a += s
   return num_to_16_le_bytes(a)
```

### 2.6 ChaCha20을 사용한 Poly1305 키 생성

일회용 Poly1305 키를 의사 랜덤하게 생성하는 방법:

1. ChaCha20 블록 함수를 다음과 함께 호출한다:
   - 세션 무결성 키를 ChaCha20 키로
   - 블록 카운터 = 0
   - 96비트 논스 (호출마다 고유해야 한다)

2. 직렬화된 상태의 처음 256비트를 추출한다
3. 처음 128비트가 클램핑된 "r"이 되고, 다음 128비트가 "s"가 된다

**의사 코드:**

```
poly1305_key_gen(key, nonce):
   counter = 0
   block = chacha20_block(key, counter, nonce)
   return block[0..31]
```

### 2.7 의사 랜덤 함수

이 RFC는 Poly1305가 키 재사용을 금지하고 키 도출에 부적합한 편향을 보이므로 PRF로 사용할 수 없음을 명시한다. ChaCha20은 이론적으로 PRF 목적의 키스트림을 생성할 수 있지만, IKEv2와 같은 프로토콜의 표준 PRF 요구사항과 일치하지 않는다. 따라서 "이 문서는 PRF를 명시하지 않는다."

### 2.8 AEAD 구성

**AEAD_CHACHA20_POLY1305**는 두 프리미티브를 연관 데이터를 포함한 인증 암호화로 결합한다.

**입력:**
- 256비트 키
- 96비트 논스 (호출마다 고유해야 한다)
- 임의 길이 평문
- 임의 길이 추가 인증 데이터(AAD)

**암호화 과정:**

1. 섹션 2.6 방법으로 Poly1305 일회용 키를 생성한다
2. 카운터를 1로 초기화하여 ChaCha20으로 평문을 암호화한다
3. Poly1305 입력을 다음과 같이 구성한다:
   - AAD
   - padding1 (AAD를 16바이트 경계에 맞추기 위한 0~15 영 바이트)
   - 암호문
   - padding2 (암호문을 16바이트 경계에 맞추기 위한 0~15 영 바이트)
   - 64비트 리틀 엔디안 AAD 길이
   - 64비트 리틀 엔디안 암호문 길이
4. Poly1305 태그를 계산한다

**출력:** 암호문에 128비트 인증 태그를 연결한 것

**복호화:** 과정을 역순으로 수행한다 — 암호문에 ChaCha20을 적용하고, AAD와 수신된 암호문에 Poly1305를 실행하고, 계산된 태그와 수신된 태그를 상수 시간 비교로 검증한다.

**의사 코드:**

```
pad16(x):
   if (len(x) % 16) == 0
      then return NULL
      else return copies(0, 16 - (len(x) % 16))

chacha20_aead_encrypt(aad, key, iv, constant, plaintext):
   nonce = constant | iv
   otk = poly1305_key_gen(key, nonce)
   ciphertext = chacha20_encrypt(key, 1, nonce, plaintext)
   mac_data = aad | pad16(aad)
   mac_data |= ciphertext | pad16(ciphertext)
   mac_data |= num_to_8_le_bytes(aad.length)
   mac_data |= num_to_8_le_bytes(ciphertext.length)
   tag = poly1305_mac(mac_data, otk)
   return (ciphertext, tag)
```

**RFC 5116 파라미터:**

| 파라미터 | 값 |
|---------|-----|
| K_LEN | 32 옥텟 |
| P_MAX | 274,877,906,880 바이트 (~256 GiB) |
| A_MAX | 2^64 - 1 옥텟 |
| N_MIN = N_MAX | 12 옥텟 |
| C_MAX | 274,877,906,896 옥텟 |

---

## 3. 구현 조언

### ChaCha20 최적화

효율성을 위해, 구현자는 연속 블록에 대해 반복적으로 상태를 재생성하지 않고 상태를 한 번 복사해야 한다. 이는 약 5.5%의 연산을 절약한다.

### Poly1305 최적화

범용 큰 수 라이브러리 대신, Poly1305 구현은 타이밍 부채널을 방지하기 위해 전용 상수 시간 산술 라이브러리를 사용해야 한다. 단순한 288비트 정수 구현도 충분하며, 곱셈 연산 중 2^288을 초과하지 않는다.

---

## 4. 보안 고려사항

### ChaCha20 보안

검증된 스트림 암호 구성을 통해 256비트 보안 강도를 제공하도록 설계되었다.

### Poly1305 보안

강한 위조 불가능성(SUF-CMA)을 달성한다. 16n 바이트 메시지에 대해, 2^64개의 합법적인 메시지 후에도 1-(n/2^102)의 확률로 위조된 메시지를 거부한다.

### 논스 요구사항 (중요)

논스는 동일한 키에 대해 각 암호화마다 고유해야 한다(MUST). 논스 반복은 동일한 키스트림이 서로 다른 암호문과 XOR되므로 평문의 XOR을 노출한다. 허용 가능한 논스 생성 방법:
- 카운터
- LFSR
- 64비트 블록의 DES 암호화 카운터

### Poly1305 키의 예측 불가능성

일회용 키는 공격자에게 예측 불가능해야 한다. 비밀 키를 사용한 카운터 암호화 또는 ChaCha20을 통한 의사 랜덤 생성이 이 요구사항을 충족한다.

### 상수 시간 구현

두 알고리즘 모두 상수 시간 구현이 가능하다:

- **ChaCha20**: 덧셈, XOR, 고정 회전만 사용하므로 본질적으로 상수 시간이다.
- **Poly1305**: 128비트 이상의 수에 대한 산술(덧셈, 곱셈, 모듈러)은 데이터 종속 분기와 캐시 효과를 피하는 신중한 구현이 필요하다.

### 태그 비교

인증 검증은 타이밍 기반 위조 공격을 방지하기 위해 상수 시간 비교를 사용해야 한다(MUST). memcmp()와 같은 표준 라이브러리 함수는 안전하지 않다. **"태그 절단(tag truncation)은 수행해서는 안 된다(MUST NOT)."**

---

## 5. IANA 고려사항

IANA는 "Authenticated Encryption with Associated Data (AEAD) Parameters" 레지스트리에 "AEAD_CHACHA20_POLY1305" 항목을 유지하며, 숫자 ID 29와 이 RFC를 참조한다.

---

## 주요 상수 및 매직 넘버

**ChaCha20 상태 상수 (리틀 엔디안):**

| 워드 | 16진수 값 | ASCII |
|------|----------|-------|
| 0 | 0x61707865 | "expa" |
| 1 | 0x3320646e | "nd 3" |
| 2 | 0x79622d32 | "2-by" |
| 3 | 0x6b206574 | "te k" |

결합하면 "expand 32-byte k"

**Poly1305 소수:** p = 2^130 - 5 = 0x3fffffffffffffffffffffffffffffffb

---

## 테스트 벡터

RFC는 섹션 2 전체와 부록 A에서 포괄적인 테스트 벡터를 제공하며, 다음을 포함한다:

- 쿼터 라운드 연산
- 전체 ChaCha20 블록 함수 출력
- ChaCha20 암호화 (유명한 "Ladies and Gentlemen" 평문 포함)
- 다양한 엣지 케이스를 포함한 Poly1305 인증
- Poly1305 키 생성
- 완전한 AEAD 암호화 및 복호화 예제

이 벡터들은 참조 출력에 대한 구현 검증을 가능하게 한다.

---

## 참조

### 규범적 참조

| 참조 | 문서 |
|------|------|
| [RFC2119] | RFC 요구사항 키워드 |
| [RFC5116] | 인증 암호화를 위한 인터페이스 및 알고리즘 |
| [ChaCha] | Bernstein, D., "ChaCha, a variant of Salsa20" (2008) |
| [Poly1305] | Bernstein, D., "The Poly1305-AES message-authentication code" (2005) |

### 정보적 참조

| 참조 | 문서 |
|------|------|
| [RFC4303] | IP ESP |
| [RFC4106] | IPsec ESP에서의 GCM 사용 |
| [RFC5297] | SIV-AES |
| [RFC7539] | IETF 프로토콜을 위한 ChaCha20 및 Poly1305 (이 RFC로 대체됨) |
| [RFC7634] | IPsec ESP에서의 ChaCha20-Poly1305 |
| [RFC7905] | TLS 및 DTLS에서의 ChaCha20-Poly1305 암호 스위트 |
| [AE] | Bellare & Namprempre, "인증 암호화" |
| [FIPS-197] | AES (FIPS 197) |
| [SP800-38D] | NIST SP 800-38D: GCM |
