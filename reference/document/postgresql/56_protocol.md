# Chapter 55: Frontend/Backend Protocol

PostgreSQL은 클라이언트(Frontend)와 서버(Backend) 간의 통신을 위해 메시지 기반 프로토콜을 사용한다. 이 장에서는 프로토콜의 구조, 메시지 흐름, 형식 및 오류 처리에 대해 상세히 설명한다.

## 목차

1. [프로토콜 개요 (Protocol Overview)](#1-프로토콜-개요-protocol-overview)
2. [메시지 흐름 (Message Flow)](#2-메시지-흐름-message-flow)
3. [메시지 데이터 타입 (Message Data Types)](#3-메시지-데이터-타입-message-data-types)
4. [메시지 형식 (Message Formats)](#4-메시지-형식-message-formats)
5. [오류 및 경고 메시지 (Error and Notice Messages)](#5-오류-및-경고-메시지-error-and-notice-messages)
6. [확장 쿼리 프로토콜 (Extended Query Protocol)](#6-확장-쿼리-프로토콜-extended-query-protocol)
7. [SASL 인증 (SASL Authentication)](#7-sasl-인증-sasl-authentication)
8. [복제 프로토콜 (Replication Protocol)](#8-복제-프로토콜-replication-protocol)

---

## 1. 프로토콜 개요 (Protocol Overview)

### 1.1 기본 구조

PostgreSQL의 Frontend/Backend 프로토콜은 두 가지 주요 단계로 구성된다:

1. 시작 단계 (Startup Phase): Frontend가 서버에 연결하고 인증을 수행
2. 정상 운영 단계 (Normal Operation): 쿼리 실행 및 결과 반환

```
┌─────────────────────────────────────────────────────────────┐
│                    PostgreSQL Protocol                       │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐                      ┌─────────────┐       │
│  │  Frontend   │  ←─── Messages ───→  │  Backend    │       │
│  │  (Client)   │                      │  (Server)   │       │
│  └─────────────┘                      └─────────────┘       │
│                                                              │
│  Phase 1: Startup (인증, 설정)                               │
│  Phase 2: Normal Operation (쿼리, 결과)                      │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 메시징 개요 (Messaging Overview)

모든 통신은 메시지 스트림을 통해 이루어진다. 각 메시지의 기본 구조:

```
┌──────────────────┬────────────────────┬──────────────────────┐
│ Message Type (1) │ Length (4 bytes)   │ Message Contents     │
│ (1 byte)         │ (includes self)    │ (variable)           │
└──────────────────┴────────────────────┴──────────────────────┘
```

메시지 구조 상세:
- 첫 번째 바이트: 메시지 타입 식별자 (예: 'Q' = Query, 'R' = Authentication)
- 다음 4바이트: 메시지 길이 (자신 포함, 타입 바이트 제외)
- 나머지: 메시지 내용

> 참고: 초기 시작 메시지(StartupMessage)는 예외적으로 메시지 타입 바이트가 없다.

### 1.3 프로토콜 버전 (Protocol Versions)

| 버전 | 지원 PostgreSQL | 설명 |
|------|-----------------|------|
| 3.2 | PostgreSQL 18+ | 최신 버전, 가변 길이 취소 키 지원 |
| 3.1 | - | 예약됨 (pgbouncer 버그로 스킵) |
| 3.0 | PostgreSQL 7.4+ | 표준 버전 |
| 2.0 | PostgreSQL 13까지 | 레거시 (더 이상 권장하지 않음) |

버전 협상:
```
Frontend → Backend: StartupMessage (프로토콜 버전 지정)
                           ↓
Backend 응답:
  - 주 버전 불일치 → 연결 거부
  - 부 버전 불일치 → NegotiateProtocolVersion 메시지
```

### 1.4 형식 코드 (Format Codes)

PostgreSQL 프로토콜은 두 가지 데이터 형식을 지원한다:

| 형식 | 코드 | 설명 |
|------|------|------|
| Text | 0 | 텍스트 표현 (기본값) |
| Binary | 1 | 바이너리 표현 (네트워크 바이트 순서) |

```c
// Text 형식 예시
"12345"          // 정수 12345의 텍스트 표현

// Binary 형식 예시 (네트워크 바이트 순서)
0x00 0x00 0x30 0x39  // 정수 12345의 바이너리 표현
```

---

## 2. 메시지 흐름 (Message Flow)

### 2.1 시작 단계 (Start-up)

연결 초기화 프로세스:

```
Frontend                              Backend
   │                                     │
   │─────── StartupMessage ──────────────>│
   │    (user, database, options)        │
   │                                     │
   │<────── AuthenticationXXX ───────────│
   │    (인증 요청)                       │
   │                                     │
   │─────── PasswordMessage ─────────────>│
   │    (필요시)                          │
   │                                     │
   │<────── AuthenticationOk ────────────│
   │<────── ParameterStatus (여러개) ────│
   │<────── BackendKeyData ──────────────│
   │<────── ReadyForQuery ───────────────│
   │                                     │
```

인증 메시지 종류:

| 메시지 | 설명 |
|--------|------|
| `AuthenticationOk` | 인증 성공 |
| `AuthenticationCleartextPassword` | 평문 비밀번호 요청 |
| `AuthenticationMD5Password` | MD5 암호화 비밀번호 요청 |
| `AuthenticationSASL` | SASL 인증 시작 |
| `AuthenticationGSS` | GSSAPI 인증 |

MD5 비밀번호 계산:
```python
# MD5 비밀번호 계산 공식
import hashlib

def md5_password(password, username, salt):
    # 1단계: password + username의 MD5
    step1 = hashlib.md5((password + username).encode()).hexdigest()
    # 2단계: step1 + salt의 MD5
    step2 = hashlib.md5((step1 + salt.hex()).encode()).hexdigest()
    return 'md5' + step2
```

### 2.2 단순 쿼리 (Simple Query)

가장 기본적인 쿼리 실행 방식:

```
Frontend                              Backend
   │                                     │
   │─────── Query ───────────────────────>│
   │    "SELECT * FROM users"            │
   │                                     │
   │<────── RowDescription ──────────────│
   │    (열 정보)                         │
   │<────── DataRow ─────────────────────│
   │    (행 데이터)                       │
   │<────── DataRow ─────────────────────│
   │<────── CommandComplete ─────────────│
   │    "SELECT 2"                       │
   │<────── ReadyForQuery ───────────────│
   │                                     │
```

응답 메시지 종류:

| 메시지 | 설명 |
|--------|------|
| `RowDescription` | 반환될 열의 구조 설명 |
| `DataRow` | 실제 행 데이터 |
| `CommandComplete` | 명령 완료 (영향받은 행 수 포함) |
| `EmptyQueryResponse` | 빈 쿼리 문자열 |
| `ErrorResponse` | 오류 발생 |
| `ReadyForQuery` | 다음 명령 준비 완료 |

예시: SELECT 쿼리 결과
```
Frontend: Query ("SELECT id, name FROM users LIMIT 2;")
Backend:  RowDescription (id: int4, name: text)
Backend:  DataRow (1, 'Alice')
Backend:  DataRow (2, 'Bob')
Backend:  CommandComplete ("SELECT 2")
Backend:  ReadyForQuery ('I')  // 'I' = Idle
```

### 2.3 다중 문장 처리

단순 쿼리는 세미콜론으로 구분된 여러 SQL 문을 포함할 수 있다:

```sql
-- 암시적 트랜잭션 블록
INSERT INTO mytable VALUES(1);
SELECT 1/0;           -- 에러 발생
INSERT INTO mytable VALUES(2);  -- 실행되지 않음
```

결과: 첫 번째 INSERT도 롤백됨 (암시적 트랜잭션)

```sql
-- 명시적 트랜잭션
BEGIN;
INSERT INTO mytable VALUES(1);
COMMIT;              -- 첫 INSERT 커밋됨
INSERT INTO mytable VALUES(2);
SELECT 1/0;          -- 에러 발생
-- 두 번째 INSERT만 롤백
```

### 2.4 COPY 작업

#### Copy-In (Frontend → Backend)
```
Backend: CopyInResponse
   ↓
Frontend → Backend: CopyData (반복)
Frontend → Backend: CopyDone
   ↓
Backend: CommandComplete
```

#### Copy-Out (Backend → Frontend)
```
Backend: CopyOutResponse
   ↓
Backend → Frontend: CopyData (반복)
Backend → Frontend: CopyDone
   ↓
Backend: CommandComplete
```

### 2.5 비동기 작업 (Asynchronous Operations)

언제든지 Backend에서 전송될 수 있는 메시지:

1. NoticeResponse: 경고 메시지
2. ParameterStatus: 파라미터 변경 알림
3. NotificationResponse: LISTEN/NOTIFY 알림

```sql
-- 알림 예시
LISTEN my_channel;

-- 다른 세션에서
NOTIFY my_channel, 'Hello!';

-- 수신 측에서 NotificationResponse 메시지 수신
```

### 2.6 요청 취소 (Canceling Requests)

장시간 실행되는 쿼리를 취소하려면:

```
Frontend: 새 연결 생성
   ↓
Frontend → Backend: CancelRequest (PID + Secret Key)
   ↓
Backend: 기존 쿼리 취소 시도
Backend: 새 연결 즉시 종료
```

> 주의: 취소 성공 여부는 보장되지 않으며, Frontend는 원래 연결에서 계속 대기해야 한다.

### 2.7 연결 종료 (Termination)

정상 종료:
```
Frontend → Backend: Terminate
Frontend: 연결 닫기
```

비정상 종료:
- Backend 오류 시: ErrorResponse 전송 후 연결 종료
- 진행 중인 트랜잭션은 자동으로 롤백

---

## 3. 메시지 데이터 타입 (Message Data Types)

프로토콜에서 사용되는 기본 데이터 타입:

### 3.1 정수형 (Integer)

| 타입 | 설명 | 예시 |
|------|------|------|
| `Int8` | 8비트 부호 있는 정수 | |
| `Int16` | 16비트 부호 있는 정수 | |
| `Int32` | 32비트 부호 있는 정수 | |
| `Int32(i)` | 특정 값 i를 가진 32비트 정수 | `Int32(0)` = 0 |

바이트 순서: 네트워크 바이트 순서 (빅엔디안, Most Significant Byte First)

```c
// Int32를 네트워크 바이트 순서로 변환
uint32_t value = 12345;
uint8_t bytes[4];
bytes[0] = (value >> 24) & 0xFF;  // 0x00
bytes[1] = (value >> 16) & 0xFF;  // 0x00
bytes[2] = (value >> 8) & 0xFF;   // 0x30
bytes[3] = value & 0xFF;          // 0x39
```

### 3.2 문자열 (String)

- C 스타일 null 종료 문자열
- 길이 제한 없음 (메모리에 맞는 한)
- 내장 null 문자 허용 안 됨

```
String("user") = 'u' 's' 'e' 'r' '\0'
```

### 3.3 바이트 수열 (Byte Sequence)

| 타입 | 설명 |
|------|------|
| `Byte1` | 단일 바이트 |
| `Byte1('c')` | 특정 문자 c |
| `Byten` | n바이트 수열 |
| `Byte[n]` | n개 바이트 배열 |

---

## 4. 메시지 형식 (Message Formats)

### 4.1 시작 메시지

#### StartupMessage (F)
```
┌─────────────────────────────────────────────────────┐
│ Int32     │ 메시지 길이 (자신 포함)                  │
│ Int32     │ 프로토콜 버전 (196610 = 3.2)            │
│ String    │ 파라미터 이름 (예: "user")              │
│ String    │ 파라미터 값                             │
│ ...       │ 추가 파라미터 쌍                        │
│ Byte1(0)  │ 종료자                                  │
└─────────────────────────────────────────────────────┘
```

필수 파라미터:
- `user`: 연결할 사용자명

선택 파라미터:
- `database`: 연결할 데이터베이스 (기본값: user명과 동일)
- `options`: 런타임 파라미터
- `replication`: 복제 모드

```python
# Python 예시: StartupMessage 생성
def create_startup_message(user, database):
    params = f"user\x00{user}\x00database\x00{database}\x00\x00"
    # 프로토콜 버전 3.0 = 196608 (0x00030000)
    version = struct.pack('>I', 196608)
    length = struct.pack('>I', 4 + len(version) + len(params))
    return length + version + params.encode()
```

#### CancelRequest (F)
```
┌─────────────────────────────────────────────────────┐
│ Int32     │ 메시지 길이 (16)                        │
│ Int32     │ 취소 요청 코드 (80877102)               │
│ Int32     │ 대상 Backend 프로세스 ID                │
│ Int32     │ 비밀 키                                  │
└─────────────────────────────────────────────────────┘
```

### 4.2 인증 메시지

#### AuthenticationOk (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('R') │ 메시지 타입                            │
│ Int32(8)   │ 메시지 길이                            │
│ Int32(0)   │ 인증 성공 코드                         │
└─────────────────────────────────────────────────────┘
```

#### AuthenticationMD5Password (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('R') │ 메시지 타입                            │
│ Int32(12)  │ 메시지 길이                            │
│ Int32(5)   │ MD5 인증 코드                          │
│ Byte4      │ Salt (암호화용)                        │
└─────────────────────────────────────────────────────┘
```

#### AuthenticationSASL (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('R') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Int32(10)  │ SASL 인증 코드                         │
│ String[]   │ SASL 메커니즘 목록 (null 종료)         │
└─────────────────────────────────────────────────────┘
```

#### PasswordMessage (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('p') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ 비밀번호 (평문 또는 MD5)               │
└─────────────────────────────────────────────────────┘
```

### 4.3 쿼리 메시지

#### Query (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('Q') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ SQL 쿼리 문자열                        │
└─────────────────────────────────────────────────────┘
```

```python
# Python 예시: Query 메시지 생성
def create_query_message(sql):
    query = sql.encode() + b'\x00'
    length = struct.pack('>I', 4 + len(query))
    return b'Q' + length + query
```

#### Parse (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('P') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ 준비된 문(Prepared Statement) 이름     │
│ String     │ 쿼리 문자열                            │
│ Int16      │ 파라미터 타입 개수                     │
│ Int32[]    │ 각 파라미터의 타입 OID                 │
└─────────────────────────────────────────────────────┘
```

#### Bind (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('B') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ 대상 포털 이름                         │
│ String     │ 소스 준비된 문 이름                    │
│ Int16      │ 파라미터 형식 코드 개수                │
│ Int16[]    │ 파라미터 형식 코드 (0=text, 1=binary)  │
│ Int16      │ 파라미터 값 개수                       │
│ [Int32 + Byte[]]│ 각 파라미터 (길이 + 값)           │
│ Int16      │ 결과 열 형식 코드 개수                 │
│ Int16[]    │ 결과 형식 코드                         │
└─────────────────────────────────────────────────────┘
```

#### Execute (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('E') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ 포털 이름                              │
│ Int32      │ 최대 반환 행 수 (0 = 무제한)           │
└─────────────────────────────────────────────────────┘
```

#### Describe (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('D') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Byte1      │ 'S' = Statement, 'P' = Portal          │
│ String     │ 이름                                   │
└─────────────────────────────────────────────────────┘
```

#### Sync (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('S') │ 메시지 타입                            │
│ Int32(4)   │ 메시지 길이                            │
└─────────────────────────────────────────────────────┘
```

### 4.4 응답 메시지

#### RowDescription (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('T') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Int16      │ 필드 개수                              │
│ [          │ 각 필드에 대해:                        │
│   String   │   필드 이름                            │
│   Int32    │   테이블 OID (0 = 테이블 없음)         │
│   Int16    │   열 번호 (0 = 열 없음)                │
│   Int32    │   데이터 타입 OID                      │
│   Int16    │   데이터 타입 크기                     │
│   Int32    │   타입 수정자 (modifier)               │
│   Int16    │   형식 코드 (0=text, 1=binary)         │
│ ]          │                                        │
└─────────────────────────────────────────────────────┘
```

#### DataRow (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('D') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Int16      │ 열 값 개수                             │
│ [          │ 각 열에 대해:                          │
│   Int32    │   값 길이 (-1 = NULL)                  │
│   Byte[]   │   열 값 (길이가 -1이면 없음)           │
│ ]          │                                        │
└─────────────────────────────────────────────────────┘
```

#### CommandComplete (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('C') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ 명령 태그                              │
└─────────────────────────────────────────────────────┘
```

명령 태그 형식:
- `SELECT rows` - 예: "SELECT 5"
- `INSERT oid rows` - 예: "INSERT 0 3"
- `UPDATE rows` - 예: "UPDATE 10"
- `DELETE rows` - 예: "DELETE 5"

#### ReadyForQuery (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('Z') │ 메시지 타입                            │
│ Int32(5)   │ 메시지 길이                            │
│ Byte1      │ 트랜잭션 상태:                         │
│            │   'I' = Idle (유휴)                    │
│            │   'T' = Transaction (트랜잭션 중)      │
│            │   'E' = Error (실패한 트랜잭션)        │
└─────────────────────────────────────────────────────┘
```

#### BackendKeyData (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('K') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Int32      │ Backend 프로세스 ID                    │
│ Byte[4-256]│ 비밀 키 (프로토콜 버전에 따라 길이 다름)│
└─────────────────────────────────────────────────────┘
```

#### ParameterStatus (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('S') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ 파라미터 이름                          │
│ String     │ 파라미터 값                            │
└─────────────────────────────────────────────────────┘
```

보고되는 주요 파라미터:
- `server_version`
- `client_encoding`
- `server_encoding`
- `DateStyle`
- `TimeZone`
- `integer_datetimes`
- `standard_conforming_strings`

### 4.5 COPY 메시지

#### CopyInResponse (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('G') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Int8       │ 전체 형식 (0=text, 1=binary)           │
│ Int16      │ 열 개수                                │
│ Int16[]    │ 각 열의 형식 코드                      │
└─────────────────────────────────────────────────────┘
```

#### CopyOutResponse (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('H') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Int8       │ 전체 형식 (0=text, 1=binary)           │
│ Int16      │ 열 개수                                │
│ Int16[]    │ 각 열의 형식 코드                      │
└─────────────────────────────────────────────────────┘
```

#### CopyData (F & B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('d') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Byte[]     │ 데이터                                 │
└─────────────────────────────────────────────────────┘
```

#### CopyDone (F & B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('c') │ 메시지 타입                            │
│ Int32(4)   │ 메시지 길이                            │
└─────────────────────────────────────────────────────┘
```

#### CopyFail (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('f') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ 오류 메시지                            │
└─────────────────────────────────────────────────────┘
```

---

## 5. 오류 및 경고 메시지 (Error and Notice Messages)

### 5.1 ErrorResponse (B)

```
┌─────────────────────────────────────────────────────┐
│ Byte1('E') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ [          │ 필드 반복:                             │
│   Byte1    │   필드 타입 코드                       │
│   String   │   필드 값                              │
│ ]          │                                        │
│ Byte1(0)   │ 종료자                                 │
└─────────────────────────────────────────────────────┘
```

### 5.2 NoticeResponse (B)

ErrorResponse와 동일한 구조이지만 메시지 타입이 'N':

```
┌─────────────────────────────────────────────────────┐
│ Byte1('N') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ [필드들]   │ ErrorResponse와 동일                   │
│ Byte1(0)   │ 종료자                                 │
└─────────────────────────────────────────────────────┘
```

### 5.3 필드 타입 코드

| 코드 | 필드명 | 설명 | 항상 존재 |
|------|--------|------|-----------|
| `S` | Severity | 심각도 (ERROR, FATAL, PANIC, WARNING, NOTICE 등) - 지역화됨 | O |
| `V` | Severity (Non-localized) | 심각도 - 지역화되지 않음 (9.6+) | |
| `C` | Code | SQLSTATE 오류 코드 (5자리) | O |
| `M` | Message | 주요 오류 메시지 | O |
| `D` | Detail | 상세 정보 | |
| `H` | Hint | 해결 방법 제안 | |
| `P` | Position | 쿼리 문자열 내 오류 위치 (1부터 시작) | |
| `p` | Internal Position | 내부 명령어 내 오류 위치 | |
| `q` | Internal Query | 실패한 내부 명령어 | |
| `W` | Where | 오류 발생 컨텍스트 (콜 스택) | |
| `s` | Schema Name | 관련 스키마 이름 | |
| `t` | Table Name | 관련 테이블 이름 | |
| `c` | Column Name | 관련 열 이름 | |
| `d` | Data Type Name | 관련 데이터 타입 이름 | |
| `n` | Constraint Name | 관련 제약 조건 이름 | |
| `F` | File | 소스 파일명 | |
| `L` | Line | 소스 라인 번호 | |
| `R` | Routine | 소스 함수명 | |

### 5.4 SQLSTATE 오류 코드 예시

| 코드 | 설명 |
|------|------|
| `00000` | 성공 |
| `23505` | unique_violation (고유 제약 조건 위반) |
| `42601` | syntax_error (구문 오류) |
| `42P01` | undefined_table (테이블 없음) |
| `42703` | undefined_column (열 없음) |
| `22012` | division_by_zero (0으로 나눔) |
| `40001` | serialization_failure (직렬화 실패) |

```python
# Python 예시: 오류 메시지 파싱
def parse_error_response(data):
    fields = {}
    i = 0
    while data[i] != 0:
        field_type = chr(data[i])
        i += 1
        # null 종료 문자열 읽기
        end = data.index(0, i)
        value = data[i:end].decode('utf-8')
        fields[field_type] = value
        i = end + 1
    return fields

# 결과 예시:
# {
#     'S': 'ERROR',
#     'V': 'ERROR',
#     'C': '42P01',
#     'M': 'relation "nonexistent" does not exist',
#     'P': '15',
#     'F': 'parse_relation.c',
#     'L': '1180',
#     'R': 'parserOpenTable'
# }
```

---

## 6. 확장 쿼리 프로토콜 (Extended Query Protocol)

### 6.1 개요

확장 쿼리 프로토콜은 SQL 명령 실행을 여러 단계로 분리하여 더 세밀한 제어와 성능 향상을 제공한다.

```
┌─────────────────────────────────────────────────────────────┐
│              Extended Query Protocol 흐름                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Parse ──→ Bind ──→ Execute                                 │
│    ↓         ↓         ↓                                    │
│  준비된 문  포털     결과                                    │
│  생성      생성     반환                                    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 핵심 개념

#### Prepared Statement (준비된 문)
- 파싱 및 분석된 쿼리
- 파라미터 값이 없는 상태
- 세션 내에서 재사용 가능

#### Portal (포털)
- 준비된 문 + 파라미터 값
- 실행 가능한 상태
- SELECT의 경우 열린 커서와 동등

### 6.3 상세 흐름

```
Frontend                              Backend
   │                                     │
   │─────── Parse ───────────────────────>│
   │    (쿼리, 파라미터 타입)             │
   │<────── ParseComplete ───────────────│
   │                                     │
   │─────── Bind ────────────────────────>│
   │    (준비된 문 → 포털, 파라미터 값)   │
   │<────── BindComplete ────────────────│
   │                                     │
   │─────── Describe ────────────────────>│
   │    (포털 정보 요청)                  │
   │<────── RowDescription ──────────────│
   │                                     │
   │─────── Execute ─────────────────────>│
   │    (포털 실행)                       │
   │<────── DataRow (여러 개) ───────────│
   │<────── CommandComplete ─────────────│
   │                                     │
   │─────── Sync ────────────────────────>│
   │<────── ReadyForQuery ───────────────│
   │                                     │
```

### 6.4 예시: 파라미터화된 쿼리

```python
# Python/psycopg2 스타일의 쿼리
cursor.execute("SELECT * FROM users WHERE id = %s", (42,))
```

프로토콜 레벨:

```
1. Parse 메시지:
   - Statement: ""  (unnamed)
   - Query: "SELECT * FROM users WHERE id = $1"
   - Parameter Types: [INT4 OID]

2. Bind 메시지:
   - Portal: ""  (unnamed)
   - Statement: ""
   - Parameter Values: ["42"]
   - Result Formats: [0]  (text)

3. Execute 메시지:
   - Portal: ""
   - Max Rows: 0  (unlimited)

4. Sync 메시지
```

### 6.5 명명된 vs 비명명 객체

| 특성 | 명명된 (Named) | 비명명 (Unnamed) |
|------|---------------|------------------|
| 수명 | 세션 종료까지 | 다음 Parse/Bind까지 |
| 용도 | 다중 사용 | 1회성 실행 |
| 이름 | 비어있지 않은 문자열 | 빈 문자열 ("") |

### 6.6 부분 실행 (Partial Execution)

Execute 메시지의 `max_rows` 파라미터를 사용하여 부분적으로 결과를 가져올 수 있다:

```
Frontend: Execute (max_rows = 10)
Backend:  DataRow × 10
Backend:  PortalSuspended

Frontend: Execute (max_rows = 10)
Backend:  DataRow × 5
Backend:  CommandComplete ("SELECT 15")

Frontend: Sync
Backend:  ReadyForQuery
```

### 6.7 파이프라이닝 (Pipelining)

여러 쿼리를 응답을 기다리지 않고 연속 전송:

```
Frontend: Parse (Query 1)
Frontend: Bind (Query 1)
Frontend: Execute (Query 1)
Frontend: Parse (Query 2)
Frontend: Bind (Query 2)
Frontend: Execute (Query 2)
Frontend: Sync

Backend:  ParseComplete
Backend:  BindComplete
Backend:  DataRow...
Backend:  CommandComplete
Backend:  ParseComplete
Backend:  BindComplete
Backend:  DataRow...
Backend:  CommandComplete
Backend:  ReadyForQuery
```

오류 처리:
- 오류 발생 시, 다음 Sync까지 모든 메시지 무시
- Sync 이후 ReadyForQuery로 동기화

### 6.8 Close 메시지

#### Close (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('C') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ Byte1      │ 'S' = Statement, 'P' = Portal          │
│ String     │ 이름                                   │
└─────────────────────────────────────────────────────┘
```

#### CloseComplete (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('3') │ 메시지 타입                            │
│ Int32(4)   │ 메시지 길이                            │
└─────────────────────────────────────────────────────┘
```

---

## 7. SASL 인증 (SASL Authentication)

### 7.1 SCRAM-SHA-256

PostgreSQL 10부터 지원되는 가장 안전한 비밀번호 인증 방식.

```
Frontend                              Backend
   │                                     │
   │<────── AuthenticationSASL ──────────│
   │    (메커니즘: SCRAM-SHA-256)        │
   │                                     │
   │─────── SASLInitialResponse ─────────>│
   │    (client-first-message)           │
   │                                     │
   │<────── AuthenticationSASLContinue ──│
   │    (server-first-message)           │
   │                                     │
   │─────── SASLResponse ────────────────>│
   │    (client-final-message)           │
   │                                     │
   │<────── AuthenticationSASLFinal ─────│
   │    (server-final-message)           │
   │                                     │
   │<────── AuthenticationOk ────────────│
   │                                     │
```

### 7.2 SASLInitialResponse (F)

```
┌─────────────────────────────────────────────────────┐
│ Byte1('p') │ 메시지 타입                            │
│ Int32      │ 메시지 길이                            │
│ String     │ SASL 메커니즘 이름                     │
│ Int32      │ 초기 응답 길이 (-1 = 없음)             │
│ Byte[]     │ 초기 응답 데이터                       │
└─────────────────────────────────────────────────────┘
```

### 7.3 SCRAM 메시지 예시

```
# client-first-message
n,,n=user,r=rOprNGfwEbeRWgbNEkqO

# server-first-message
r=rOprNGfwEbeRWgbNEkqO%hvYDpWUa,s=W22ZaJ0SNY7soEsUEjb6gQ==,i=4096

# client-final-message
c=biws,r=rOprNGfwEbeRWgbNEkqO%hvYDpWUa,p=dHzbZapWIk4jUhN+Ute9ytag9zjfMHgsqmmiz7AndVQ=

# server-final-message
v=6rriTRBi23WpRR/wtup+mMhUZUn/dB5nLTJRsjl95G4=
```

---

## 8. 복제 프로토콜 (Replication Protocol)

### 8.1 스트리밍 복제 (Streaming Replication)

물리적 복제를 위한 프로토콜.

연결 설정:
```sql
-- StartupMessage에 replication=true 포함
replication = true
```

주요 명령:
- `IDENTIFY_SYSTEM`: 시스템 정보 요청
- `START_REPLICATION`: WAL 스트리밍 시작

### 8.2 논리적 복제 (Logical Replication)

논리적 변경 스트림을 위한 프로토콜.

```sql
-- 논리적 복제 시작
START_REPLICATION SLOT myslot LOGICAL 0/0
    (proto_version '1', publication_names 'mypub');
```

### 8.3 복제 메시지

#### XLogData (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('w') │ 메시지 타입                            │
│ Int64      │ WAL 시작 위치                          │
│ Int64      │ 현재 WAL 끝 위치                       │
│ Int64      │ 서버 시간                              │
│ Byte[]     │ WAL 데이터                             │
└─────────────────────────────────────────────────────┘
```

#### Primary Keepalive (B)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('k') │ 메시지 타입                            │
│ Int64      │ 현재 WAL 끝 위치                       │
│ Int64      │ 서버 시간                              │
│ Byte1      │ 응답 필요 여부                         │
└─────────────────────────────────────────────────────┘
```

#### Standby Status Update (F)
```
┌─────────────────────────────────────────────────────┐
│ Byte1('r') │ 메시지 타입                            │
│ Int64      │ 마지막으로 받은 WAL                    │
│ Int64      │ 마지막으로 디스크에 플러시한 WAL       │
│ Int64      │ 마지막으로 적용한 WAL                  │
│ Int64      │ 클라이언트 시간                        │
│ Byte1      │ 즉시 응답 요청                         │
└─────────────────────────────────────────────────────┘
```

---

## 9. SSL/TLS 암호화

### 9.1 SSL 연결 설정

```
Frontend                              Backend
   │                                     │
   │─────── SSLRequest ──────────────────>│
   │    (8바이트 메시지)                  │
   │                                     │
   │<────── 'S' 또는 'N' ────────────────│
   │    (1바이트 응답)                    │
   │                                     │
   │  [SSL 핸드셰이크 - 'S'인 경우]       │
   │                                     │
   │─────── StartupMessage ──────────────>│
   │    (암호화된 채널로)                 │
   │                                     │
```

#### SSLRequest
```
┌─────────────────────────────────────────────────────┐
│ Int32(8)       │ 메시지 길이                        │
│ Int32(80877103)│ SSL 요청 코드                      │
└─────────────────────────────────────────────────────┘
```

### 9.2 GSSAPI 암호화

```
Frontend                              Backend
   │                                     │
   │─────── GSSENCRequest ───────────────>│
   │                                     │
   │<────── 'G' 또는 'N' ────────────────│
   │                                     │
   │  [GSSAPI 핸드셰이크 - 'G'인 경우]    │
   │                                     │
```

---

## 10. 프로토콜 구현 예제

### 10.1 간단한 연결 예제 (Python)

```python
import socket
import struct
import hashlib

class PGConnection:
    def __init__(self, host, port, user, database, password):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.connect((host, port))
        self.user = user
        self.database = database
        self.password = password

    def startup(self):
        """StartupMessage 전송"""
        # 파라미터 구성
        params = (
            f"user\x00{self.user}\x00"
            f"database\x00{self.database}\x00"
            "\x00"  # 종료자
        ).encode()

        # 프로토콜 버전 3.0
        version = struct.pack('>I', 196608)
        length = struct.pack('>I', 4 + len(version) + len(params))

        self.sock.sendall(length + version + params)

    def read_message(self):
        """메시지 읽기"""
        msg_type = self.sock.recv(1)
        if not msg_type:
            return None, None

        length_bytes = self.sock.recv(4)
        length = struct.unpack('>I', length_bytes)[0]

        content = self.sock.recv(length - 4)
        return msg_type.decode(), content

    def handle_auth(self, content):
        """인증 처리"""
        auth_type = struct.unpack('>I', content[:4])[0]

        if auth_type == 0:
            print("인증 성공!")
            return True
        elif auth_type == 3:
            # 평문 비밀번호
            self.send_password(self.password)
        elif auth_type == 5:
            # MD5 비밀번호
            salt = content[4:8]
            self.send_md5_password(salt)
        return False

    def send_password(self, password):
        """PasswordMessage 전송"""
        msg = password.encode() + b'\x00'
        length = struct.pack('>I', 4 + len(msg))
        self.sock.sendall(b'p' + length + msg)

    def send_md5_password(self, salt):
        """MD5 암호화 비밀번호 전송"""
        # concat('md5', md5(concat(md5(password+user), salt)))
        step1 = hashlib.md5(
            (self.password + self.user).encode()
        ).hexdigest()
        step2 = hashlib.md5(
            (step1.encode() + salt)
        ).hexdigest()
        self.send_password('md5' + step2)

    def query(self, sql):
        """단순 쿼리 실행"""
        msg = sql.encode() + b'\x00'
        length = struct.pack('>I', 4 + len(msg))
        self.sock.sendall(b'Q' + length + msg)

        results = []
        while True:
            msg_type, content = self.read_message()

            if msg_type == 'T':
                # RowDescription
                columns = self.parse_row_description(content)
                print(f"열: {columns}")
            elif msg_type == 'D':
                # DataRow
                row = self.parse_data_row(content)
                results.append(row)
            elif msg_type == 'C':
                # CommandComplete
                tag = content[:-1].decode()
                print(f"명령 완료: {tag}")
            elif msg_type == 'Z':
                # ReadyForQuery
                status = chr(content[0])
                print(f"준비 완료 (상태: {status})")
                break
            elif msg_type == 'E':
                # ErrorResponse
                error = self.parse_error(content)
                print(f"오류: {error}")

        return results

    def parse_row_description(self, content):
        """RowDescription 파싱"""
        num_fields = struct.unpack('>H', content[:2])[0]
        columns = []
        offset = 2

        for _ in range(num_fields):
            # null 종료 문자열 찾기
            end = content.index(0, offset)
            name = content[offset:end].decode()
            columns.append(name)
            # 나머지 필드 건너뛰기 (18바이트)
            offset = end + 1 + 18

        return columns

    def parse_data_row(self, content):
        """DataRow 파싱"""
        num_columns = struct.unpack('>H', content[:2])[0]
        values = []
        offset = 2

        for _ in range(num_columns):
            length = struct.unpack('>i', content[offset:offset+4])[0]
            offset += 4

            if length == -1:
                values.append(None)
            else:
                value = content[offset:offset+length].decode()
                values.append(value)
                offset += length

        return values

    def parse_error(self, content):
        """ErrorResponse 파싱"""
        fields = {}
        offset = 0

        while content[offset] != 0:
            field_type = chr(content[offset])
            offset += 1
            end = content.index(0, offset)
            value = content[offset:end].decode()
            fields[field_type] = value
            offset = end + 1

        return fields

    def close(self):
        """연결 종료"""
        self.sock.sendall(b'X\x00\x00\x00\x04')
        self.sock.close()


# 사용 예시
if __name__ == '__main__':
    conn = PGConnection(
        host='localhost',
        port=5432,
        user='postgres',
        database='testdb',
        password='password'
    )

    conn.startup()

    # 인증 처리
    while True:
        msg_type, content = conn.read_message()
        if msg_type == 'R':
            if conn.handle_auth(content):
                break
        elif msg_type == 'S':
            # ParameterStatus
            pass
        elif msg_type == 'K':
            # BackendKeyData
            pass
        elif msg_type == 'Z':
            # ReadyForQuery
            break

    # 쿼리 실행
    results = conn.query("SELECT id, name FROM users LIMIT 5;")
    for row in results:
        print(row)

    conn.close()
```

### 10.2 확장 쿼리 예제

```python
def extended_query(self, sql, params):
    """확장 쿼리 프로토콜 사용"""

    # 1. Parse 메시지
    stmt_name = b'\x00'  # unnamed
    query = sql.encode() + b'\x00'
    num_params = struct.pack('>H', 0)  # 타입 추론

    parse_msg = stmt_name + query + num_params
    length = struct.pack('>I', 4 + len(parse_msg))
    self.sock.sendall(b'P' + length + parse_msg)

    # 2. Bind 메시지
    portal_name = b'\x00'  # unnamed
    stmt_name = b'\x00'

    # 파라미터 형식 (모두 텍스트)
    param_formats = struct.pack('>H', 0)

    # 파라미터 값
    num_params = struct.pack('>H', len(params))
    param_values = b''
    for p in params:
        if p is None:
            param_values += struct.pack('>i', -1)
        else:
            val = str(p).encode()
            param_values += struct.pack('>i', len(val)) + val

    # 결과 형식 (모두 텍스트)
    result_formats = struct.pack('>H', 0)

    bind_msg = (portal_name + stmt_name + param_formats +
                num_params + param_values + result_formats)
    length = struct.pack('>I', 4 + len(bind_msg))
    self.sock.sendall(b'B' + length + bind_msg)

    # 3. Describe 메시지 (포털)
    describe_msg = b'P' + b'\x00'
    length = struct.pack('>I', 4 + len(describe_msg))
    self.sock.sendall(b'D' + length + describe_msg)

    # 4. Execute 메시지
    portal_name = b'\x00'
    max_rows = struct.pack('>I', 0)  # 무제한
    execute_msg = portal_name + max_rows
    length = struct.pack('>I', 4 + len(execute_msg))
    self.sock.sendall(b'E' + length + execute_msg)

    # 5. Sync 메시지
    self.sock.sendall(b'S\x00\x00\x00\x04')

    # 응답 처리
    return self._process_response()

# 사용 예시
results = conn.extended_query(
    "SELECT * FROM users WHERE id = $1 AND status = $2",
    [42, 'active']
)
```

---

## 11. 문제 해결 (Troubleshooting)

### 11.1 일반적인 오류

| SQLSTATE | 오류 | 원인 | 해결 방법 |
|----------|------|------|-----------|
| 28000 | invalid_authorization_specification | 인증 실패 | 사용자명/비밀번호 확인 |
| 28P01 | invalid_password | 잘못된 비밀번호 | 비밀번호 확인 |
| 3D000 | invalid_catalog_name | 데이터베이스 없음 | 데이터베이스 이름 확인 |
| 08006 | connection_failure | 연결 실패 | 호스트/포트 확인 |
| 57P03 | cannot_connect_now | 서버 시작 중 | 잠시 후 재시도 |

### 11.2 프로토콜 디버깅

```bash
# PostgreSQL 로그 설정
log_connections = on
log_disconnections = on
log_statement = 'all'

# 네트워크 트래픽 캡처
tcpdump -i lo0 -w pg_traffic.pcap port 5432

# Wireshark에서 분석
# 필터: pgsql
```

### 11.3 성능 최적화

1. Prepared Statements 사용: 반복 쿼리의 파싱 오버헤드 제거
2. 파이프라이닝: 네트워크 왕복 최소화
3. Binary 형식: 큰 데이터의 직렬화/역직렬화 비용 감소
4. 연결 풀링: pgbouncer, pgpool-II 사용

---

## 요약

PostgreSQL Frontend/Backend 프로토콜의 핵심 요소:

1. 메시지 기반 통신: 모든 상호작용은 정형화된 메시지를 통해 이루어짐
2. 두 단계 프로토콜: 시작 단계(인증)와 정상 운영 단계
3. 두 가지 쿼리 모드:
   - 단순 쿼리: 간단하고 직관적
   - 확장 쿼리: 파라미터화, 준비된 문, 성능 최적화
4. 강력한 오류 처리: 상세한 오류 정보와 SQLSTATE 코드
5. 보안: MD5, SCRAM-SHA-256, SSL/TLS, GSSAPI 지원
6. 확장성: 복제 프로토콜, COPY 작업, 비동기 알림

프로토콜을 이해하면 PostgreSQL 클라이언트 라이브러리 개발, 성능 최적화, 문제 해결에 큰 도움이 된다.
