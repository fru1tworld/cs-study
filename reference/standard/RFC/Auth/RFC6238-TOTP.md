# RFC 6238 - TOTP: Time-Based One-Time Password Algorithm

> 발행일: 2011년 5월
> 상태: Informational

## 1. 개요

TOTP(Time-based One-Time Password)는 [RFC 4226 HOTP](./RFC4226-HOTP.md)의 이동 인자(moving factor)를 카운터 대신 현재 시각으로 바꾼 알고리즘이다.
 서버와 클라이언트가 카운터 값을 동기화할 필요 없이, 둘 다 같은 시계(clock)를 기준으로 같은 시간 구간에 있으면 동일한 OTP 값을 계산할 수 있다.

| 구분 | HOTP | TOTP |
|------|------|------|
| 이동 인자 | 증가하는 카운터 C | 현재 UNIX 시각을 구간으로 나눈 값 T |
| 동기화 대상 | 카운터 | 시계 |
| 값 변경 시점 | 사용할 때마다 | 시간 구간(보통 30초)마다 |
| 대표 사용처 | 하드웨어 토큰, 이벤트 기반 인증 | Google Authenticator, Authy 등 OTP 앱 |

---

## 2. 알고리즘

```
TOTP(K) = HOTP(K, T)

T = floor((현재 UNIX 시각 - T0) / X)
```

| 기호 | 의미 | 기본값 |
|------|------|--------|
| K | 공유 비밀 키 | - |
| T0 | 카운터 시작 시각(UNIX epoch) | 0 |
| X | 시간 구간 길이(초) | 30 |
| T | 시간 구간 카운터 값 (HOTP의 C 자리에 대입) | - |

HOTP와 마찬가지로 HMAC 결과를 동적 절단(dynamic truncation)하여 지정한 자릿수(보통 6자리)로 축약한다.
 HMAC 해시 함수로는 SHA-1(기본), SHA-256, SHA-512를 사용할 수 있다.

### 2.1 계산 예시 (의사코드)

```python
def totp(key: bytes, digits=6, period=30, t0=0, algo="sha1"):
    t = int((time.time() - t0) // period)
    counter = t.to_bytes(8, "big")
    return hotp(key, counter, digits, algo)
```

30초 구간을 쓰면, 같은 30초 안에 여러 번 조회해도 값이 바뀌지 않고, 구간이 넘어가는 순간 값이 갱신된다.

---

## 3. 시간 동기화와 허용 오차

클라이언트(토큰/앱)와 서버의 시계가 완벽히 일치하기 어렵기 때문에, 검증 서버는 보통 앞뒤로 한두 구간의 오차를 허용한다.

```
검증 시각 T에 대해 T-1, T, T+1 세 구간의 값을 모두 계산해
클라이언트가 보낸 값과 비교
```

| 항목 | 권장 사항 |
|------|-----------|
| 허용 윈도우 | 앞뒤 1구간(±30초) 정도로 제한 |
| 큰 오차 감지 시 | 서버가 클라이언트 시계 오차를 추정해 다음 검증부터 보정 |
| 재사용 방지 | 이미 성공적으로 검증된 T 값은 재사용 금지 (재전송 공격 방지) |

---

## 4. 보안 고려사항

| 항목 | 설명 |
|------|------|
| 공유 비밀 길이 | HOTP와 동일하게 최소 128비트, 160비트 이상 권장 |
| 시간 구간 길이 | 짧을수록 공격 윈도우가 줄지만 사용성이 나빠짐. 30초가 일반적 |
| 재전송 공격 | 같은 시간 구간 내에서 탈취한 OTP 값 재사용 가능 → 재사용 여부를 서버가 기록해 차단 |
| 시계 신뢰 | 서버와 클라이언트 모두 신뢰할 수 있는 시각 동기화(NTP 등) 필요 |
| 브루트포스 | HOTP와 동일하게 검증 시도 횟수 제한(스로틀링) 필요 |

---

## 5. 프로비저닝: `otpauth://` URI

Google Authenticator 등에서 사실상 표준으로 쓰이는 QR 코드 형식(RFC는 아니지만 TOTP 생태계의 관례):

```
otpauth://totp/{issuer}:{account}?secret={base32비밀}&issuer={issuer}&algorithm=SHA1&digits=6&period=30
```

| 파라미터 | 의미 |
|----------|------|
| secret | Base32로 인코딩한 공유 비밀 키 |
| issuer | 서비스 제공자 이름 |
| algorithm | SHA1(기본) / SHA256 / SHA512 |
| digits | OTP 자릿수 (6 또는 8) |
| period | 시간 구간(초), 기본 30 |

---

## 6. 요약

- TOTP는 HOTP의 카운터를 시간 구간으로 대체한 알고리즘이다.
- 서버-클라이언트 간 카운터 동기화 문제를 시계 동기화 문제로 바꾼다.
- 30초 구간 + ±1구간 허용 오차가 일반적인 구현 관례다.
- MFA(다중 인증)에서 가장 널리 쓰이는 OTP 방식이며, [RFC 4226 HOTP](./RFC4226-HOTP.md), [RFC 6749 OAuth2](./RFC6749-OAuth2.md) 기반 인증 흐름과 함께 2단계 인증에 사용된다.

---

## 참고 자료

- [RFC 6238 원문](https://www.rfc-editor.org/rfc/rfc6238)
- [RFC 4226 HOTP](./RFC4226-HOTP.md)
- [Key Uri Format (Google Authenticator wiki)](https://github.com/google/google-authenticator/wiki/Key-Uri-Format)
