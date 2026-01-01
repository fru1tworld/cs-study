# RFC 4122 - A Universally Unique IDentifier (UUID) URN Namespace

> **발행일**: 2005년 7월
> **상태**: Standards Track

## 1. 개요

UUID(Universally Unique Identifier)는 분산 시스템에서 **중앙 조정 기관 없이 고유한 식별자를 생성**할 수 있는 128비트 식별자입니다. GUID(Globally Unique Identifier)라고도 불립니다.

### 핵심 특징

| 특징 | 설명 |
|------|------|
| **크기** | 128비트 (16바이트) |
| **고유성** | 전역적으로 고유함 (충돌 확률 극히 낮음) |
| **분산 생성** | 중앙 서버 없이 독립적으로 생성 가능 |
| **표준 형식** | `xxxxxxxx-xxxx-Mxxx-Nxxx-xxxxxxxxxxxx` |

---

## 2. UUID 형식

### 2.1 표준 문자열 표현

```
xxxxxxxx-xxxx-Mxxx-Nxxx-xxxxxxxxxxxx
```

- **32개의 16진수 문자** + **4개의 하이픈**
- 총 36자
- M: 버전 (1-5)
- N: 변형 (variant)

**예시**:
```
550e8400-e29b-41d4-a716-446655440000
f47ac10b-58cc-4372-a567-0e02b2c3d479
```

### 2.2 필드 구조

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                          time_low                             |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|       time_mid                |         time_hi_and_version   |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|clk_seq_hi_res |  clk_seq_low  |         node (0-1)            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                         node (2-5)                            |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

| 필드 | 크기 | 설명 |
|------|------|------|
| time_low | 32비트 | 타임스탬프 하위 32비트 |
| time_mid | 16비트 | 타임스탬프 중간 16비트 |
| time_hi_and_version | 16비트 | 타임스탬프 상위 + 버전(4비트) |
| clk_seq_hi_res | 8비트 | 클럭 시퀀스 상위 + variant(2비트) |
| clk_seq_low | 8비트 | 클럭 시퀀스 하위 |
| node | 48비트 | 노드 식별자 (MAC 주소 등) |

### 2.3 Variant 필드

```
MSB 비트 패턴    Variant
0xx             NCS 호환 (레거시)
10x             RFC 4122 표준
110             Microsoft 호환 (레거시)
111             미래용 예약
```

**RFC 4122 UUID의 특징**: N 위치의 첫 글자가 `8`, `9`, `a`, `b` 중 하나

### 2.4 Nil UUID

**Nil UUID**는 모든 128비트가 0으로 설정된 특별한 UUID입니다:

```
00000000-0000-0000-0000-000000000000
```

**용도**:
- UUID가 아직 할당되지 않았음을 나타냄
- 초기화되지 않은 UUID 필드의 기본값
- "값 없음"을 명시적으로 표현

**특징**:
- 유효한 UUID로 취급됨
- 버전과 variant 비트도 0
- 어떤 UUID 버전에도 속하지 않음

**프로그래밍 예시**:
```python
import uuid

nil_uuid = uuid.UUID('00000000-0000-0000-0000-000000000000')
# 또는
nil_uuid = uuid.UUID(int=0)

# Nil UUID 확인
if str(my_uuid) == '00000000-0000-0000-0000-000000000000':
    print("UUID가 할당되지 않음")
```

### 2.5 바이트 순서 (네트워크 바이트 오더)

UUID는 **네트워크 바이트 오더 (Big-Endian)**를 사용합니다:

```
바이트 0                                                바이트 15
+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
| time_low        | time_mid | time_hi | clk | clk |       node                |
|                 |          | +ver    | hi  | lo  |                           |
+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
  0    1    2    3   4    5    6    7    8    9   10   11   12   13   14   15
```

**바이트 순서 규칙**:

| 필드 | 옥텟 범위 | 바이트 순서 |
|------|-----------|-------------|
| time_low | 0-3 | Big-Endian (MSB first) |
| time_mid | 4-5 | Big-Endian (MSB first) |
| time_hi_and_version | 6-7 | Big-Endian (MSB first) |
| clk_seq_hi_res | 8 | 단일 바이트 |
| clk_seq_low | 9 | 단일 바이트 |
| node | 10-15 | Big-Endian (MSB first) |

**중요 사항**:
- 네트워크 전송 시 바이트 순서 변환 불필요
- 문자열 표현과 바이너리 표현의 바이트 순서가 일치
- Little-Endian 시스템에서는 변환 주의 필요

**바이트 순서 변환 예시**:
```python
import uuid
import struct

# UUID 생성
u = uuid.uuid4()

# 바이트 표현 (Big-Endian)
uuid_bytes = u.bytes  # 16바이트, 네트워크 바이트 오더

# 필드별 추출
time_low = struct.unpack('>I', uuid_bytes[0:4])[0]   # Big-Endian unsigned int
time_mid = struct.unpack('>H', uuid_bytes[4:6])[0]   # Big-Endian unsigned short
```

---

## 3. UUID 버전

### 3.1 버전 개요

| 버전 | 이름 | 기반 | 용도 |
|------|------|------|------|
| **1** | Time-based | 타임스탬프 + MAC | 시간 순서가 필요할 때 |
| **2** | DCE Security | 타임스탬프 + POSIX UID | DCE 보안용 (거의 사용 안 함) |
| **3** | Name-based (MD5) | 네임스페이스 + 이름 | 동일 이름에서 동일 UUID |
| **4** | Random | 랜덤 비트 | 가장 일반적 사용 |
| **5** | Name-based (SHA-1) | 네임스페이스 + 이름 | v3의 보안 강화 버전 |

### 3.2 Version 1 (Time-based)

타임스탬프와 노드 ID(MAC 주소) 기반:

```
UUID v1 = timestamp(60bit) + clock_seq(14bit) + node(48bit) + version(4bit) + variant(2bit)
```

**구조**:
```
550e8400-e29b-11d4-a716-446655440000
                ^
                버전 1
```

**특징**:
- 생성 시간 추출 가능
- MAC 주소 노출 (프라이버시 문제)
- 시간 순서대로 정렬 가능

### 3.3 Version 3 (Name-based MD5)

네임스페이스와 이름의 MD5 해시:

```
UUID v3 = MD5(namespace_uuid + name)
```

**미리 정의된 네임스페이스**:

| 네임스페이스 | UUID | 용도 |
|--------------|------|------|
| DNS | `6ba7b810-9dad-11d1-80b4-00c04fd430c8` | 도메인 이름 |
| URL | `6ba7b811-9dad-11d1-80b4-00c04fd430c8` | URL |
| OID | `6ba7b812-9dad-11d1-80b4-00c04fd430c8` | ISO OID |
| X500 | `6ba7b814-9dad-11d1-80b4-00c04fd430c8` | X.500 DN |

**예시**:
```python
# Python 예시
import uuid
namespace = uuid.NAMESPACE_DNS
name = "example.com"
uuid3 = uuid.uuid3(namespace, name)
# 항상 동일한 결과: 9073926b-929f-31c2-abc9-fad77ae3e8eb
```

### 3.4 Version 4 (Random)

122비트의 랜덤 데이터 + 6비트 고정(버전/variant):

```
UUID v4 = random(122bit) + version(4bit) + variant(2bit)
```

**구조**:
```
f47ac10b-58cc-4372-a567-0e02b2c3d479
              ^
              버전 4
```

**특징**:
- 가장 널리 사용됨
- 충돌 확률: 약 2^122 분의 1
- 암호학적으로 안전한 난수 생성기 권장

### 3.5 Version 5 (Name-based SHA-1)

네임스페이스와 이름의 SHA-1 해시:

```
UUID v5 = SHA-1(namespace_uuid + name)[0:128]
```

**v3 vs v5**:
| 특성 | v3 | v5 |
|------|----|----|
| 해시 알고리즘 | MD5 | SHA-1 |
| 보안성 | 낮음 | 높음 |
| 권장 | 레거시 호환 | 신규 개발 |

---

## 4. UUID 생성 알고리즘

### 4.1 Version 1 알고리즘

```
1. 현재 시간을 UTC로 가져옴 (100나노초 단위, 1582년 10월 15일 기준)
2. clock_seq를 이전 값과 비교하여 필요시 증가
3. node 값으로 MAC 주소 또는 랜덤 값 사용
4. 필드 조합하여 UUID 생성
```

### 4.2 Version 4 알고리즘

```
1. 128비트 랜덤 값 생성
2. version 필드를 4로 설정 (비트 76-79)
3. variant 필드를 10으로 설정 (비트 64-65)
```

**의사 코드**:
```python
def generate_uuid4():
    # 128비트 랜덤 생성
    uuid = secure_random(128)

    # 버전 설정 (상위 4비트를 0100으로)
    uuid[6] = (uuid[6] & 0x0f) | 0x40

    # variant 설정 (상위 2비트를 10으로)
    uuid[8] = (uuid[8] & 0x3f) | 0x80

    return uuid
```

---

## 5. 충돌 확률

### 5.1 Version 4 충돌 확률

122비트 랜덤 공간에서의 충돌 확률:

| 생성 개수 | 충돌 확률 |
|-----------|-----------|
| 2.71 × 10^18 | 50% |
| 10억 개 | 약 10^-26 |
| 1조 개 | 약 10^-14 |

**Birthday Paradox 공식**:
```
p ≈ 1 - e^(-n²/2N)
N = 2^122 (가능한 UUID 수)
n = 생성된 UUID 수
```

### 5.2 실용적 관점

> 매초 10억 개의 UUID를 100년간 생성해도 충돌 확률은 50%에 도달하지 않음

---

## 6. URN 네임스페이스

### 6.1 UUID URN 형식

```
urn:uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6
```

### 6.2 URN 문법

```
UUID-URN = "urn:uuid:" UUID-string
UUID-string = time-low "-" time-mid "-" time-high-and-version "-"
              clock-seq-and-reserved "-" node
```

---

## 7. 프로그래밍 언어별 예시

### 7.1 Python

```python
import uuid

# Version 1 (시간 기반)
uuid1 = uuid.uuid1()

# Version 3 (MD5 이름 기반)
uuid3 = uuid.uuid3(uuid.NAMESPACE_DNS, "example.com")

# Version 4 (랜덤)
uuid4 = uuid.uuid4()

# Version 5 (SHA-1 이름 기반)
uuid5 = uuid.uuid5(uuid.NAMESPACE_DNS, "example.com")

# 문자열에서 생성
uuid_from_str = uuid.UUID("550e8400-e29b-41d4-a716-446655440000")
```

### 7.2 JavaScript

```javascript
// Node.js (crypto 모듈)
const crypto = require('crypto');
const uuid = crypto.randomUUID();

// 브라우저 (Web Crypto API)
const uuid = crypto.randomUUID();

// uuid 패키지
const { v1, v4, v5 } = require('uuid');
const id1 = v1();    // Time-based
const id4 = v4();    // Random
const id5 = v5('example.com', v5.DNS);  // Name-based
```

### 7.3 Java

```java
import java.util.UUID;

// Version 4 (랜덤)
UUID uuid4 = UUID.randomUUID();

// Version 3 (이름 기반)
UUID uuid3 = UUID.nameUUIDFromBytes("example.com".getBytes());

// 문자열에서 생성
UUID fromStr = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

// 필드 추출
long mostSig = uuid4.getMostSignificantBits();
long leastSig = uuid4.getLeastSignificantBits();
int version = uuid4.version();
```

### 7.4 Go

```go
import "github.com/google/uuid"

// Version 4 (랜덤)
id := uuid.New()

// Version 1 (시간 기반)
id1, _ := uuid.NewUUID()

// 문자열에서 파싱
parsed, _ := uuid.Parse("550e8400-e29b-41d4-a716-446655440000")

// 버전 확인
version := id.Version()
```

---

## 8. 활용 사례

### 8.1 데이터베이스 기본키

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE
);
```

**장점**:
- 분산 환경에서 ID 충돌 없음
- 샤딩/복제 용이
- 보안 (auto-increment보다 예측 어려움)

**단점**:
- 128비트로 정수보다 큼
- 인덱스 성능 영향 가능 (v4의 경우)

### 8.2 파일/리소스 식별

```
/uploads/f47ac10b-58cc-4372-a567-0e02b2c3d479.jpg
/api/sessions/550e8400-e29b-41d4-a716-446655440000
```

### 8.3 분산 시스템 트랜잭션 ID

```json
{
  "transactionId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "timestamp": "2023-12-01T10:30:00Z",
  "operations": [...]
}
```

### 8.4 세션 식별자

```http
Set-Cookie: session_id=f81d4fae-7dec-11d0-a765-00a0c91e6bf6; HttpOnly; Secure
```

---

## 9. 보안 고려사항

### 9.1 Version 1의 프라이버시 문제

- MAC 주소가 노출됨
- 생성 시간이 추론 가능
- **권장**: v4 또는 v5 사용

### 9.2 난수 생성기 품질

```
Version 4는 암호학적으로 안전한 난수 생성기(CSPRNG)를 사용해야 합니다.
```

**안전한 방법**:
- `/dev/urandom` (Linux)
- `CryptGenRandom` (Windows)
- `crypto.randomUUID()` (브라우저)

**위험한 방법**:
- `Math.random()` (JavaScript)
- `rand()` (C)

---

## 10. 요약

RFC 4122는 UUID 형식과 생성 알고리즘을 정의합니다:

- **128비트 식별자**: 전역적으로 고유
- **5가지 버전**: 시간 기반(v1), 보안(v2), MD5(v3), 랜덤(v4), SHA-1(v5)
- **표준 형식**: `xxxxxxxx-xxxx-Mxxx-Nxxx-xxxxxxxxxxxx`
- **분산 생성**: 중앙 조정 없이 독립적으로 생성
- **권장 버전**: v4(랜덤) 또는 v5(이름 기반)

UUID는 **데이터베이스 기본키, 세션 ID, 파일 식별** 등 다양한 용도로 활용되는 핵심 식별 체계입니다.

---

## 참고 자료

- [RFC 4122 원문](https://www.rfc-editor.org/rfc/rfc4122)
- [UUID Wikipedia](https://en.wikipedia.org/wiki/Universally_unique_identifier)
- [New UUID Formats (RFC 9562)](https://www.rfc-editor.org/rfc/rfc9562) - UUIDv6, v7, v8 정의
