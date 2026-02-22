# RFC 5321: Simple Mail Transfer Protocol (SMTP)

> 단순 메일 전송 프로토콜 - 인터넷 전자 메일 전송의 기본 표준

## 문서 정보

| 항목 | 내용 |
|------|------|
| RFC 번호 | 5321 |
| 분류 | Standards Track (표준 추적) |
| 작성자 | J. Klensin |
| 발행일 | 2008년 10월 |
| 상태 | Draft Standard |
| 폐기 대상 | RFC 821, RFC 974, RFC 1869, RFC 2821 |
| 갱신 대상 | RFC 1123 |

---

## 목차

1. [개요](#1-개요)
2. [SMTP 모델](#2-smtp-모델)
3. [주소 지정](#3-주소-지정)
4. [SMTP 명세](#4-smtp-명세)
5. [주소 해석과 메일 처리](#5-주소-해석과-메일-처리)
6. [문제 탐지와 처리](#6-문제-탐지와-처리)
7. [보안 고려사항](#7-보안-고려사항)
8. [IANA 고려사항](#8-iana-고려사항)
9. [부록](#9-부록)

---

## 1. 개요

### 1.1 SMTP의 목적

SMTP(Simple Mail Transfer Protocol)의 목적은 메일을 신뢰성 있고 효율적으로 전송하는 것입니다.

SMTP는 특정 전송 서브시스템과 독립적이며, 신뢰성 있는 순서화된 데이터 스트림 채널만 필요로 합니다. TCP(Transmission Control Protocol)가 주요 전송 수단이지만, 다른 전송 서비스도 사용될 수 있습니다.

### 1.2 역사와 배경

- RFC 821 (1982년): 원본 SMTP 명세
- RFC 1123 (1989년): 호스트 요구사항 명세
- RFC 2821 (2001년): SMTP 갱신 명세
- RFC 5321 (2008년): 현재 표준 명세

### 1.3 용어 정의

이 문서에서 사용되는 키워드 "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", "OPTIONAL"은 RFC 2119에 기술된 대로 해석되어야 합니다.

| 용어 | 정의 |
|------|------|
| Client | 메일 전송을 시작하는 호스트 |
| Server | 메일을 수신하는 호스트 |
| Session | 클라이언트와 서버 간의 연결 |
| Transaction | MAIL 명령부터 DATA 완료까지의 단위 |
| Forward-path | 수신자 주소 (RCPT TO에서 사용) |
| Reverse-path | 발신자 주소 (MAIL FROM에서 사용) |
| Envelope | MAIL/RCPT 명령으로 전달되는 주소 정보 |
| Message | 헤더와 본문으로 구성된 실제 메일 내용 |

---

## 2. SMTP 모델

### 2.1 기본 구조

```
+----------+                +----------+
|          |                |          |
|   User   |<-------------->|   User   |
|          |    Commands    |          |
+-----+----+    Replies     +----+-----+
      |                          |
      V                          V
+----------+                +----------+
|          |   Connections   |          |
|   SMTP   |<--------------->|   SMTP   |
|  Client  |                 |  Server  |
|          |                 |          |
+----------+                +----------+
                DNS
         (MX Records)
```

SMTP 설계는 다음과 같은 전송 모델을 기반으로 합니다:

1. 사용자가 메일 발송을 요청하면, SMTP 클라이언트가 SMTP 서버와 양방향 전송 채널을 설정합니다.
2. SMTP 클라이언트는 명령을 생성하고 서버의 응답을 처리합니다.
3. 연결이 설정되면 클라이언트는 MAIL 명령으로 발신자를 지정하고, RCPT 명령으로 수신자를 지정합니다.
4. 수신자가 협상되면 DATA 명령으로 메시지를 전송합니다.

### 2.2 메일 전송 에이전트 유형

```
[MUA] --> [MSA] --> [MTA] --> [MTA] --> ... --> [MDA] --> [MUA]
 발신       제출      중계      중계              배달      수신
```

| 에이전트 | 역할 | 설명 |
|----------|------|------|
| MUA (Mail User Agent) | 사용자 에이전트 | 최종 사용자의 메일 클라이언트 |
| MSA (Mail Submission Agent) | 제출 에이전트 | 사용자로부터 메일 수신 (포트 587) |
| MTA (Mail Transfer Agent) | 전송 에이전트 | 메일 중계 및 라우팅 (포트 25) |
| MDA (Mail Delivery Agent) | 배달 에이전트 | 최종 수신자 메일박스에 배달 |

### 2.3 SMTP 세션의 역할

#### 2.3.1 Originator (발신자)
메일을 처음 생성하고 SMTP 시스템에 주입하는 시스템입니다.

#### 2.3.2 Relay (중계)
메일을 다른 SMTP 서버로 전달하는 중간 시스템입니다. Received 헤더를 추가하는 것 외에 메시지를 수정하지 않습니다.

#### 2.3.3 Gateway (게이트웨이)
서로 다른 메일 시스템 간에 메시지를 변환하는 시스템입니다. 메시지 형식 변환이 필요할 수 있습니다.

#### 2.3.4 Final Delivery (최종 배달)
메시지를 최종 목적지(사용자 메일박스)에 전달하는 시스템입니다.

### 2.4 책임 이전

> 중요: SMTP 서버가 메시지에 대해 성공 응답(250)을 보내면, 그 메시지를 배달하거나 배달 실패를 적절히 보고할 책임을 정식으로 수락한 것입니다.

```
클라이언트                              서버
    |                                    |
    |  DATA                              |
    |  메시지 내용...                      |
    |  .                                 |
    |----------------------------------->|
    |                                    |
    |  250 OK                            |
    |<-----------------------------------|
    |                                    |
    |  (책임이 클라이언트에서 서버로 이전됨)   |
```

---

## 3. 주소 지정

### 3.1 메일 객체

SMTP에서 전송되는 메일 객체는 두 부분으로 구성됩니다:

```
+----------------------------------+
|          ENVELOPE                |
|  (MAIL FROM, RCPT TO 명령들)       |
+----------------------------------+
|          MESSAGE                 |
|  +----------------------------+  |
|  |         HEADER             |  |
|  |  From: sender@example.com  |  |
|  |  To: recipient@example.org |  |
|  |  Subject: Test             |  |
|  |  Date: Mon, 1 Jan 2024...  |  |
|  +----------------------------+  |
|  |         BODY               |  |
|  |  메시지 본문 내용...          |  |
|  +----------------------------+  |
+----------------------------------+
```

### 3.2 이메일 주소 형식

이메일 주소는 local-part@domain 형식을 따릅니다.

```
예: john.doe@example.com
    ^^^^^^^^ ^^^^^^^^^^^
    local-part   domain
```

#### 3.2.1 Local-part 규칙

- 대소문자 구분: local-part는 대소문자를 구분합니다(SHOULD)
- 허용 문자: 영숫자, 점(.), 하이픈(-), 밑줄(_) 등
- 따옴표 문자열: 특수 문자를 포함하려면 따옴표로 묶음

```
유효한 예:
  user@example.com
  user.name@example.com
  user+tag@example.com
  "user name"@example.com
  mailbox@example.com
```

#### 3.2.2 Domain 규칙

- 대소문자 구분 없음: 도메인은 대소문자를 구분하지 않습니다
- 형식: DNS 도메인 이름 또는 주소 리터럴
- 주소 리터럴: [IPv4 주소] 또는 [IPv6:IPv6주소]

```
예:
  user@example.com          (DNS 도메인)
  user@[192.168.1.1]        (IPv4 리터럴)
  user@[IPv6:2001:db8::1]   (IPv6 리터럴)
```

### 3.3 특수 주소

| 주소 | 용도 |
|------|------|
| `postmaster@domain` | 관리자 주소 (필수 지원) |
| `<>` (null) | 반송 메일의 발신자 (bounce) |
| `abuse@domain` | 남용 신고 주소 (권장) |

---

## 4. SMTP 명세

### 4.1 SMTP 명령어

SMTP 명령어는 클라이언트가 서버에 보내는 텍스트 기반 명령입니다.

```
명령 형식: COMMAND [Parameters] CRLF
         ^^^^^^^ ^^^^^^^^^^^^^ ^^^^
         동사    인자(선택적)    줄바꿈
```

#### 4.1.1 EHLO / HELO (세션 시작)

목적: 클라이언트가 자신을 식별하고 세션을 시작합니다.

```
구문:
  EHLO domain
  HELO domain
```

EHLO vs HELO 차이점:

| 명령 | 확장 지원 | 현대 사용 |
|------|-----------|-----------|
| EHLO | 지원 (ESMTP) | 권장 |
| HELO | 미지원 (원본 SMTP) | 폴백용 |

예시:
```
C: EHLO mail.example.com
S: 250-server.example.org Hello mail.example.com
S: 250-SIZE 14680064
S: 250-8BITMIME
S: 250-STARTTLS
S: 250-ENHANCEDSTATUSCODES
S: 250-PIPELINING
S: 250-CHUNKING
S: 250 SMTPUTF8
```

EHLO 응답에서 광고되는 확장 기능:

| 확장 | 설명 |
|------|------|
| `SIZE` | 최대 메시지 크기 |
| `8BITMIME` | 8비트 MIME 지원 |
| `STARTTLS` | TLS 암호화 지원 |
| `ENHANCEDSTATUSCODES` | 향상된 상태 코드 |
| `PIPELINING` | 명령 파이프라이닝 |
| `CHUNKING` | BDAT 명령 지원 |
| `SMTPUTF8` | UTF-8 주소 지원 |
| `AUTH` | 인증 메커니즘 |
| `DSN` | 배달 상태 알림 |

#### 4.1.2 MAIL FROM (발신자 지정)

목적: 메일 트랜잭션을 시작하고 발신자(reverse-path)를 지정합니다.

```
구문:
  MAIL FROM:<reverse-path> [parameters]
```

예시:
```
C: MAIL FROM:<sender@example.com>
S: 250 2.1.0 Sender OK

C: MAIL FROM:<sender@example.com> SIZE=1234567
S: 250 2.1.0 Sender OK

C: MAIL FROM:<> (반송 메일용 null 발신자)
S: 250 2.1.0 Sender OK
```

매개변수:

| 매개변수 | 확장 | 설명 |
|----------|------|------|
| `SIZE=n` | SIZE | 메시지 크기 (바이트) |
| `BODY=8BITMIME` | 8BITMIME | 8비트 본문 |
| `BODY=BINARYMIME` | BINARYMIME | 바이너리 본문 |
| `RET=FULL|HDRS` | DSN | 반환 유형 |
| `ENVID=value` | DSN | 봉투 ID |
| `AUTH=<addr>` | AUTH | 인증된 발신자 |
| `SMTPUTF8` | SMTPUTF8 | UTF-8 주소 |

#### 4.1.3 RCPT TO (수신자 지정)

목적: 수신자(forward-path)를 지정합니다. 여러 수신자에게 보내려면 여러 번 사용합니다.

```
구문:
  RCPT TO:<forward-path> [parameters]
```

예시:
```
C: RCPT TO:<recipient@example.org>
S: 250 2.1.5 Recipient OK

C: RCPT TO:<another@example.org>
S: 250 2.1.5 Recipient OK

C: RCPT TO:<invalid@example.org>
S: 550 5.1.1 User unknown
```

매개변수:

| 매개변수 | 확장 | 설명 |
|----------|------|------|
| `NOTIFY=value` | DSN | 알림 조건 (NEVER, SUCCESS, FAILURE, DELAY) |
| `ORCPT=type;addr` | DSN | 원본 수신자 |

#### 4.1.4 DATA (메시지 전송)

목적: 메시지 내용(헤더와 본문)을 전송합니다.

```
구문:
  DATA
  <message content>
  .
```

중요: 메시지 끝은 단독 점(.)만 있는 줄로 표시됩니다.

예시:
```
C: DATA
S: 354 Start mail input; end with <CRLF>.<CRLF>
C: From: sender@example.com
C: To: recipient@example.org
C: Subject: Test Message
C: Date: Mon, 1 Jan 2024 12:00:00 +0000
C:
C: This is the message body.
C: It can span multiple lines.
C: .
S: 250 2.0.0 Message accepted for delivery
```

점 스터핑 (Dot Stuffing):

본문에 줄 시작이 점(.)인 경우, 추가 점을 앞에 붙여야 합니다:

```
원본 메시지:                  전송 시:
Hello                        Hello
.This starts with a dot      ..This starts with a dot
End                          End
```

#### 4.1.5 QUIT (세션 종료)

목적: SMTP 세션을 정상적으로 종료합니다.

```
구문:
  QUIT
```

예시:
```
C: QUIT
S: 221 2.0.0 Bye
(연결 종료)
```

#### 4.1.6 RSET (트랜잭션 재설정)

목적: 현재 메일 트랜잭션을 취소하고 상태를 초기화합니다.

```
구문:
  RSET
```

예시:
```
C: MAIL FROM:<sender@example.com>
S: 250 OK
C: RCPT TO:<recipient@example.org>
S: 250 OK
C: RSET
S: 250 2.0.0 Reset OK
(트랜잭션 취소됨, 새 트랜잭션 시작 가능)
```

RSET 후 상태:
- reverse-path 버퍼 초기화
- forward-path 버퍼 초기화
- 메시지 데이터 버퍼 초기화
- EHLO/HELO 상태는 유지

#### 4.1.7 VRFY (주소 확인)

목적: 메일박스가 존재하는지 확인합니다.

```
구문:
  VRFY <string>
```

예시:
```
C: VRFY Smith
S: 250 Fred Smith <fsmith@example.com>

C: VRFY Jones
S: 251 User not local; will forward to <jones@other.example>

C: VRFY Nobody
S: 550 5.1.1 User unknown
```

보안 참고: 많은 서버가 보안상의 이유로 VRFY를 비활성화합니다.

#### 4.1.8 EXPN (메일링 리스트 확장)

목적: 메일링 리스트의 멤버십을 확장합니다.

```
구문:
  EXPN <string>
```

예시:
```
C: EXPN staff
S: 250-Fred Smith <fsmith@example.com>
S: 250-Jane Doe <jdoe@example.com>
S: 250 John Public <jpublic@example.com>
```

보안 참고: VRFY와 마찬가지로 보안상의 이유로 비활성화되는 경우가 많습니다.

#### 4.1.9 HELP (도움말)

목적: 서버로부터 도움말 정보를 요청합니다.

```
구문:
  HELP [command]
```

예시:
```
C: HELP
S: 214-Commands supported:
S: 214-HELO EHLO MAIL RCPT DATA
S: 214-RSET NOOP QUIT HELP VRFY EXPN
S: 214 For more info use "HELP <topic>"

C: HELP MAIL
S: 214 MAIL FROM:<sender> [parameters]
```

#### 4.1.10 NOOP (무동작)

목적: 아무 동작도 하지 않고 연결을 유지합니다.

```
구문:
  NOOP
```

예시:
```
C: NOOP
S: 250 2.0.0 OK
```

용도:
- 연결 유지 (keep-alive)
- 서버 응답 확인
- 타임아웃 방지

---

### 4.2 SMTP 응답 코드

SMTP 응답은 3자리 숫자 코드와 텍스트 설명으로 구성됩니다.

```
응답 형식: XYZ text
          ^^^ ^^^^
          코드 설명
```

#### 4.2.1 응답 코드 구조

```
첫 번째 숫자 (X): 응답 범주
  2 - 성공
  3 - 중간 (추가 입력 필요)
  4 - 일시적 실패 (재시도 가능)
  5 - 영구적 실패

두 번째 숫자 (Y): 응답 유형
  0 - 구문
  1 - 정보
  2 - 연결
  3 - 지정되지 않음
  4 - 지정되지 않음
  5 - 메일 시스템

세 번째 숫자 (Z): 세부 구분
```

#### 4.2.2 주요 응답 코드 목록

2XX - 성공 응답

| 코드 | 설명 |
|------|------|
| 211 | 시스템 상태 또는 시스템 도움말 응답 |
| 214 | 도움말 메시지 |
| 220 | 서비스 준비 완료 (서버 인사) |
| 221 | 서비스 종료 (연결 닫기) |
| 250 | 요청된 메일 작업 완료 |
| 251 | 사용자가 로컬이 아님; 전달될 것임 |
| 252 | 사용자 확인 불가; 수락하고 배달 시도 |

3XX - 중간 응답

| 코드 | 설명 |
|------|------|
| 354 | 메일 입력 시작; <CRLF>.<CRLF>로 종료 |

4XX - 일시적 실패 (재시도 가능)

| 코드 | 설명 |
|------|------|
| 421 | 서비스를 사용할 수 없음; 연결 닫기 |
| 450 | 요청된 메일 작업 미완료: 메일박스 사용 불가 |
| 451 | 요청된 작업 중단: 로컬 오류 처리 중 |
| 452 | 요청된 작업 미완료: 시스템 저장 공간 부족 |
| 455 | 서버가 매개변수를 수용할 수 없음 |

5XX - 영구적 실패

| 코드 | 설명 |
|------|------|
| 500 | 구문 오류, 명령을 인식할 수 없음 |
| 501 | 매개변수 또는 인수의 구문 오류 |
| 502 | 명령이 구현되지 않음 |
| 503 | 잘못된 명령 순서 |
| 504 | 명령 매개변수가 구현되지 않음 |
| 550 | 요청된 작업 미완료: 메일박스 사용 불가 |
| 551 | 사용자가 로컬이 아님; <forward-path> 시도 |
| 552 | 요청된 메일 작업 중단: 저장 공간 할당 초과 |
| 553 | 요청된 작업 미완료: 메일박스 이름 허용 안 됨 |
| 554 | 트랜잭션 실패 (다른 응답이 해당하지 않을 때 사용) |
| 555 | MAIL FROM/RCPT TO 매개변수를 인식하지 못하거나 구현하지 않음 |

#### 4.2.3 향상된 상태 코드 (Enhanced Status Codes)

RFC 3463에서 정의된 향상된 상태 코드는 더 세부적인 정보를 제공합니다:

```
형식: X.Y.Z
      ^ ^ ^
      | | +-- 세부 상태 (0-999)
      | +---- 주제 (0-9)
      +------ 클래스 (2, 4, 5)

예: 250 2.1.5 Recipient OK
    ^^^ ^^^^^
    기본 향상된
    코드 코드
```

향상된 상태 코드 예시:

| 코드 | 의미 |
|------|------|
| `2.0.0` | 성공 |
| `2.1.0` | 발신 주소 유효 |
| `2.1.5` | 수신 주소 유효 |
| `2.6.0` | 메시지 내용 전송 성공 |
| `4.0.0` | 일시적 주소 실패 |
| `4.2.2` | 메일박스 가득 참 |
| `4.7.0` | 보안 또는 정책 상태 |
| `5.0.0` | 영구 주소 실패 |
| `5.1.0` | 잘못된 발신 주소 |
| `5.1.1` | 잘못된 수신 메일박스 주소 |
| `5.1.2` | 잘못된 수신 시스템 주소 |
| `5.2.0` | 메일박스 상태 문제 |
| `5.2.1` | 메일박스 비활성화 |
| `5.2.2` | 메일박스 가득 참 |
| `5.3.0` | 메일 시스템 상태 문제 |
| `5.3.4` | 메시지가 너무 큼 |
| `5.5.0` | 프로토콜 상태 |
| `5.7.0` | 보안 또는 정책 상태 |
| `5.7.1` | 메시지 거부됨 |

---

### 4.3 SMTP 세션 예제

#### 4.3.1 기본 메일 전송

```
S: 220 mail.example.org ESMTP Service Ready
C: EHLO mail.example.com
S: 250-mail.example.org Hello mail.example.com
S: 250-SIZE 14680064
S: 250-8BITMIME
S: 250-ENHANCEDSTATUSCODES
S: 250-PIPELINING
S: 250 HELP
C: MAIL FROM:<alice@example.com>
S: 250 2.1.0 Sender OK
C: RCPT TO:<bob@example.org>
S: 250 2.1.5 Recipient OK
C: DATA
S: 354 Start mail input; end with <CRLF>.<CRLF>
C: From: Alice <alice@example.com>
C: To: Bob <bob@example.org>
C: Subject: Hello
C: Date: Mon, 1 Jan 2024 12:00:00 +0000
C: Message-ID: <123456@example.com>
C:
C: Hi Bob,
C:
C: This is a test message.
C:
C: Best regards,
C: Alice
C: .
S: 250 2.0.0 Message accepted for delivery
C: QUIT
S: 221 2.0.0 Bye
```

#### 4.3.2 여러 수신자에게 전송

```
S: 220 mail.example.org ESMTP
C: EHLO sender.example.com
S: 250-mail.example.org
S: 250 OK
C: MAIL FROM:<newsletter@example.com>
S: 250 OK
C: RCPT TO:<user1@example.org>
S: 250 OK
C: RCPT TO:<user2@example.org>
S: 250 OK
C: RCPT TO:<user3@example.org>
S: 550 5.1.1 User unknown
C: RCPT TO:<user4@example.org>
S: 250 OK
C: DATA
S: 354 Enter message
C: Subject: Newsletter
C:
C: Newsletter content...
C: .
S: 250 OK
C: QUIT
S: 221 Bye
```

#### 4.3.3 오류 처리 및 재시도

```
S: 220 mail.example.org ESMTP
C: EHLO client.example.com
S: 250 OK
C: MAIL FROM:<sender@example.com>
S: 250 OK
C: RCPT TO:<recipient@example.org>
S: 452 4.2.2 Mailbox full - try again later
C: RSET
S: 250 OK
C: QUIT
S: 221 Bye
(나중에 재시도)
```

#### 4.3.4 TLS 암호화 사용 (STARTTLS)

```
S: 220 mail.example.org ESMTP
C: EHLO client.example.com
S: 250-mail.example.org
S: 250-STARTTLS
S: 250 OK
C: STARTTLS
S: 220 2.0.0 Ready to start TLS
(TLS 핸드셰이크 수행)
C: EHLO client.example.com
S: 250-mail.example.org
S: 250-AUTH PLAIN LOGIN
S: 250 OK
C: AUTH PLAIN AGFsaWNlAHNlY3JldA==
S: 235 2.7.0 Authentication successful
C: MAIL FROM:<alice@example.com>
...
```

---

### 4.4 메시지 형식

#### 4.4.1 메시지 구조

SMTP로 전송되는 메시지는 RFC 5322 형식을 따릅니다:

```
+---------------------------------+
|           HEADER                |
|  필드이름: 필드값               |
|  필드이름: 필드값               |
|  ...                           |
+---------------------------------+
|        (빈 줄 - CRLF)           |
+---------------------------------+
|           BODY                  |
|  메시지 본문 내용               |
|  ...                           |
+---------------------------------+
```

#### 4.4.2 필수 헤더 필드

| 필드 | 설명 | 예시 |
|------|------|------|
| Date | 메시지 작성 날짜/시간 | `Date: Mon, 1 Jan 2024 12:00:00 +0000` |
| From | 메시지 작성자 | `From: Alice <alice@example.com>` |

#### 4.4.3 권장 헤더 필드

| 필드 | 설명 | 예시 |
|------|------|------|
| Message-ID | 고유 메시지 식별자 | `Message-ID: <unique-id@example.com>` |
| To | 기본 수신자 | `To: Bob <bob@example.org>` |
| Subject | 메시지 제목 | `Subject: Meeting Tomorrow` |

#### 4.4.4 선택적 헤더 필드

| 필드 | 설명 |
|------|------|
| Cc | 참조 수신자 |
| Bcc | 숨은 참조 수신자 |
| Reply-To | 회신 주소 |
| In-Reply-To | 원본 메시지 ID |
| References | 관련 메시지 ID들 |
| MIME-Version | MIME 버전 |
| Content-Type | 내용 유형 |
| Content-Transfer-Encoding | 전송 인코딩 |

#### 4.4.5 추적 헤더 필드

```
Received: from mail.example.com (mail.example.com [192.0.2.1])
        by mail.example.org (Postfix) with ESMTPS id ABC123
        for <bob@example.org>; Mon, 1 Jan 2024 12:00:00 +0000

Return-Path: <alice@example.com>
```

---

### 4.5 명령 순서 규칙

SMTP 명령은 특정 순서를 따라야 합니다:

```
세션 시작
    |
    v
+--------+
|  EHLO  | <----+
+--------+      |
    |           |
    v           |
+--------+      |
|  MAIL  | -----+
+--------+      |
    |           |
    v           |
+--------+      |
|  RCPT  | (여러 번 가능)
+--------+      |
    |           |
    v           |
+--------+      |
|  DATA  | -----+
+--------+      |
    |           |
    v           |
+--------+      |
|  RSET  | -----+ (트랜잭션 취소 시)
+--------+
    |
    v
+--------+
|  QUIT  |
+--------+
    |
    v
세션 종료
```

명령 순서 요구사항:

| 순서 | 요구사항 |
|------|----------|
| EHLO/HELO | 세션에서 첫 번째 명령이어야 함 |
| MAIL | EHLO/HELO 이후에만 가능 |
| RCPT | MAIL 이후에만 가능 |
| DATA | 적어도 하나의 RCPT 이후에만 가능 |
| RSET | 언제든지 가능 |
| QUIT | 언제든지 가능 |
| NOOP | 언제든지 가능 |
| VRFY/EXPN | 언제든지 가능 |
| HELP | 언제든지 가능 |

---

## 5. 주소 해석과 메일 처리

### 5.1 MX 레코드 조회

SMTP 클라이언트는 목적지 도메인의 MX(Mail Exchanger) 레코드를 조회하여 메일 서버를 찾습니다:

```
1. 도메인의 MX 레코드 조회
2. MX 레코드가 없으면 A/AAAA 레코드 조회
3. 우선순위(낮은 값이 높은 우선순위)에 따라 정렬
4. 가장 높은 우선순위 서버부터 연결 시도
```

MX 레코드 예시:
```
example.org.    IN MX 10 mail1.example.org.
example.org.    IN MX 20 mail2.example.org.
example.org.    IN MX 30 backup.example.org.
```

조회 알고리즘:

```
function lookup_mail_server(domain):
    mx_records = dns_query(domain, "MX")

    if mx_records is not empty:
        sort mx_records by preference (ascending)
        return mx_records

    # MX가 없으면 A/AAAA 레코드 사용
    a_records = dns_query(domain, "A") + dns_query(domain, "AAAA")

    if a_records is not empty:
        return [(0, domain)]  # 도메인 자체가 메일 서버

    return error("No mail server found")
```

### 5.2 메일 라우팅

```
발신자 -> [DNS 조회] -> MX 서버 1 (실패) -> MX 서버 2 (성공) -> 수신자
```

라우팅 규칙:

1. 직접 배달: 클라이언트가 최종 목적지 서버에 직접 연결
2. 스마트 호스트: 모든 발신 메일을 특정 서버로 전송
3. 중계: 중간 서버를 통한 전달

### 5.3 주소 리터럴

도메인 이름 대신 IP 주소를 직접 사용할 수 있습니다:

```
user@[192.168.1.1]           (IPv4)
user@[IPv6:2001:db8::1]      (IPv6)
```

주의: 주소 리터럴 사용은 일반적으로 권장되지 않습니다.

---

## 6. 문제 탐지와 처리

### 6.1 타임아웃

SMTP 클라이언트와 서버는 적절한 타임아웃을 구현해야 합니다:

| 작업 | 권장 타임아웃 |
|------|---------------|
| 초기 연결 | 5분 |
| EHLO/HELO 응답 | 5분 |
| MAIL 명령 응답 | 5분 |
| RCPT 명령 응답 | 5분 |
| DATA 시작 응답 | 2분 |
| DATA 블록 전송 | 3분 |
| DATA 종료 응답 | 10분 |
| 서버 유휴 | 5분 |

### 6.2 재시도 전략

일시적 실패(4XX)에 대한 재시도 전략:

```
재시도 전략:
  - 초기 지연: 30분
  - 최대 재시도 기간: 4-5일
  - 지연 증가: 지수 백오프
  - 알림: 지연 경고 (4-6시간 후)
```

예시 재시도 스케줄:
```
실패 시: 즉시 재시도
1차 실패 후: 30분 대기
2차 실패 후: 1시간 대기
3차 실패 후: 2시간 대기
4차 실패 후: 4시간 대기
...
(4-5일 후 최종 실패로 처리)
```

### 6.3 반송 메시지 (Bounce Messages)

배달 실패 시 발신자에게 알림을 보냅니다:

```
From: Mail Delivery System <MAILER-DAEMON@example.org>
To: <alice@example.com>
Subject: Undelivered Mail Returned to Sender
Date: Mon, 1 Jan 2024 12:30:00 +0000

This is the mail system at host mail.example.org.

I'm sorry to have to inform you that your message could not
be delivered to one or more recipients.

<bob@example.org>: delivery temporarily suspended:
    connect to mail.example.org[192.0.2.1]:25:
    Connection timed out

----- Original message follows -----
(원본 메시지 또는 헤더)
```

반송 메일 특성:
- MAIL FROM은 null (<>)을 사용
- 원본 메시지 정보 포함
- 반송 메일의 반송은 생성하지 않음

### 6.4 루프 탐지

메일 루프를 방지하기 위한 메커니즘:

```
Received 헤더 분석:
  - 동일 호스트가 여러 번 나타나면 루프 가능성
  - 일반적으로 25-100개 이상의 Received 헤더는 루프로 간주
```

---

## 7. 보안 고려사항

### 7.1 메일 릴레이 제어

오픈 릴레이 문제:
- 오픈 릴레이는 스팸 전송에 악용될 수 있음
- 서버는 인가된 사용자/도메인만 릴레이해야 함

릴레이 제어 방법:

| 방법 | 설명 |
|------|------|
| IP 기반 | 알려진 IP 주소에서만 릴레이 허용 |
| 인증 기반 | SMTP AUTH로 인증된 사용자만 릴레이 |
| POP-before-SMTP | POP3 인증 후 일정 시간 릴레이 허용 |
| 도메인 기반 | 로컬 도메인으로 향하는 메일만 수신 |

### 7.2 SMTP 인증 (SMTP AUTH)

RFC 4954에서 정의된 SMTP 인증 확장:

```
C: EHLO client.example.com
S: 250-server.example.org
S: 250-AUTH PLAIN LOGIN CRAM-MD5
S: 250 OK
C: AUTH PLAIN AGFsaWNlAHNlY3JldA==
S: 235 2.7.0 Authentication successful
```

인증 메커니즘:

| 메커니즘 | 설명 | 보안 |
|----------|------|------|
| PLAIN | 평문 사용자명/비밀번호 | TLS 필수 |
| LOGIN | 레거시 평문 인증 | TLS 필수 |
| CRAM-MD5 | 챌린지-응답 | 중간 |
| SCRAM-SHA-256 | 현대적 챌린지-응답 | 높음 |
| XOAUTH2 | OAuth 2.0 기반 | 높음 |

### 7.3 STARTTLS

전송 계층 보안(TLS) 활성화:

```
C: EHLO client.example.com
S: 250-server.example.org
S: 250-STARTTLS
S: 250 OK
C: STARTTLS
S: 220 2.0.0 Ready to start TLS
(TLS 핸드셰이크)
C: EHLO client.example.com
(암호화된 세션 계속)
```

TLS 고려사항:

| 항목 | 권장사항 |
|------|----------|
| 프로토콜 버전 | TLS 1.2 이상 |
| 암호 스위트 | 강력한 암호만 사용 |
| 인증서 검증 | 서버 인증서 검증 권장 |
| 기회적 TLS | STARTTLS 사용 가능 시 사용 |

### 7.4 SPF (Sender Policy Framework)

발신자 도메인 인증을 위한 DNS 기반 메커니즘:

```
예시 DNS 레코드:
example.com.  IN TXT "v=spf1 mx ip4:192.0.2.0/24 -all"
```

SPF 한정자:

| 한정자 | 의미 |
|--------|------|
| `+` | Pass (기본값) |
| `-` | Fail |
| `~` | SoftFail |
| `?` | Neutral |

### 7.5 DKIM (DomainKeys Identified Mail)

암호화 서명을 통한 메시지 인증:

```
DKIM-Signature: v=1; a=rsa-sha256; c=relaxed/relaxed;
    d=example.com; s=selector1;
    h=from:to:subject:date;
    bh=47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=;
    b=dzdVyOfAKCdLXdJOc9G2q8LoXSlE...
```

### 7.6 DMARC (Domain-based Message Authentication)

SPF와 DKIM을 결합한 정책 프레임워크:

```
예시 DNS 레코드:
_dmarc.example.com. IN TXT "v=DMARC1; p=reject; rua=mailto:dmarc@example.com"
```

### 7.7 발신자 주소 위조 방지

주의해야 할 공격:

| 공격 유형 | 설명 | 대응 |
|-----------|------|------|
| 주소 위조 | From 헤더 조작 | SPF, DKIM, DMARC |
| 오픈 릴레이 악용 | 제3자 서버 이용 | 릴레이 제어 |
| 메일 폭탄 | 대량 메일 발송 | 속도 제한 |
| 디렉토리 수확 | 유효 주소 수집 | VRFY 비활성화 |

### 7.8 개인정보 보호

Received 헤더의 정보 노출:
- 내부 네트워크 구조
- 서버 소프트웨어 버전
- 클라이언트 IP 주소

완화 방법:
- 내부 Received 헤더 제거/수정
- 익명화 서비스 사용

---

## 8. IANA 고려사항

### 8.1 서비스 이름과 포트

| 서비스 | 포트 | 설명 |
|--------|------|------|
| smtp | 25 | 메일 전송 (MTA 간) |
| submission | 587 | 메일 제출 (MUA에서 MSA로) |
| submissions | 465 | 암시적 TLS 메일 제출 |

### 8.2 SMTP 확장 레지스트리

IANA는 SMTP 서비스 확장 레지스트리를 유지합니다:

| 확장 키워드 | RFC | 설명 |
|-------------|-----|------|
| SIZE | 1870 | 메시지 크기 선언 |
| 8BITMIME | 6152 | 8비트 MIME 전송 |
| STARTTLS | 3207 | TLS 암호화 |
| AUTH | 4954 | 인증 |
| PIPELINING | 2920 | 명령 파이프라이닝 |
| DSN | 3461 | 배달 상태 알림 |
| ENHANCEDSTATUSCODES | 2034 | 향상된 상태 코드 |
| SMTPUTF8 | 6531 | 국제화 이메일 |
| CHUNKING | 3030 | 청크 전송 |
| BINARYMIME | 3030 | 바이너리 MIME |

---

## 9. 부록

### 9.1 SMTP 명령어 요약표

| 명령 | 구문 | 설명 |
|------|------|------|
| EHLO | `EHLO domain` | 확장 SMTP 인사 |
| HELO | `HELO domain` | 기본 SMTP 인사 |
| MAIL | `MAIL FROM:<addr> [params]` | 발신자 지정 |
| RCPT | `RCPT TO:<addr> [params]` | 수신자 지정 |
| DATA | `DATA` | 메시지 데이터 시작 |
| RSET | `RSET` | 트랜잭션 재설정 |
| VRFY | `VRFY string` | 주소 확인 |
| EXPN | `EXPN string` | 리스트 확장 |
| HELP | `HELP [topic]` | 도움말 요청 |
| NOOP | `NOOP` | 무동작 |
| QUIT | `QUIT` | 세션 종료 |

### 9.2 응답 코드 요약표

| 코드 | 의미 |
|------|------|
| 2XX | 긍정 완료 |
| 3XX | 긍정 중간 (추가 입력 필요) |
| 4XX | 일시적 부정 완료 |
| 5XX | 영구적 부정 완료 |

### 9.3 ABNF 문법

```abnf
; 기본 구조
ehlo           = "EHLO" SP ( Domain / address-literal ) CRLF
helo           = "HELO" SP Domain CRLF
mail           = "MAIL FROM:" Reverse-path [SP Mail-parameters] CRLF
rcpt           = "RCPT TO:" ( "<Postmaster@" Domain ">" /
                             "<Postmaster>" /
                             Forward-path ) [SP Rcpt-parameters] CRLF
data           = "DATA" CRLF
rset           = "RSET" CRLF
vrfy           = "VRFY" SP String CRLF
expn           = "EXPN" SP String CRLF
help           = "HELP" [ SP String ] CRLF
noop           = "NOOP" [ SP String ] CRLF
quit           = "QUIT" CRLF

; 경로
Reverse-path   = Path / "<>"
Forward-path   = Path
Path           = "<" [ A-d-l ":" ] Mailbox ">"
A-d-l          = At-domain *( "," At-domain )
At-domain      = "@" Domain
Mailbox        = Local-part "@" ( Domain / address-literal )
Local-part     = Dot-string / Quoted-string
Domain         = sub-domain *("." sub-domain)
address-literal = "[" ( IPv4-address-literal /
                       IPv6-address-literal /
                       General-address-literal ) "]"
```

### 9.4 SMTP 포트 사용 가이드

```
┌─────────────────────────────────────────────────────────────┐
│                      SMTP 포트 사용                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  포트 25 (SMTP)                                             │
│  ├─ 용도: 서버 간 메일 전송 (MTA to MTA)                     │
│  ├─ 암호화: STARTTLS (기회적)                                │
│  └─ 참고: ISP에서 차단하는 경우 있음                         │
│                                                             │
│  포트 587 (Submission)                                      │
│  ├─ 용도: 클라이언트에서 서버로 메일 제출 (MUA to MSA)       │
│  ├─ 암호화: STARTTLS (권장)                                  │
│  └─ 인증: 필수 (SMTP AUTH)                                   │
│                                                             │
│  포트 465 (Submissions)                                     │
│  ├─ 용도: 암시적 TLS 메일 제출                               │
│  ├─ 암호화: 암시적 TLS (연결 시 즉시)                        │
│  └─ 인증: 필수 (SMTP AUTH)                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 9.5 일반적인 문제 해결

| 문제 | 가능한 원인 | 해결 방법 |
|------|-------------|-----------|
| 연결 거부 | 방화벽, 포트 차단 | 포트 587 또는 465 사용 |
| 인증 실패 | 잘못된 자격 증명 | 사용자명/비밀번호 확인 |
| 550 오류 | 수신자 불명 | 주소 확인 |
| 552 오류 | 메일박스 가득 참 | 수신자에게 연락 |
| 554 오류 | 스팸으로 거부됨 | SPF/DKIM/DMARC 설정 확인 |
| TLS 오류 | 인증서 문제 | 인증서 유효성 확인 |

---

## 참고 자료

### 관련 RFC

| RFC | 제목 | 설명 |
|-----|------|------|
| RFC 821 | SMTP | 원본 SMTP 명세 |
| RFC 1123 | Host Requirements | 호스트 요구사항 |
| RFC 2034 | Enhanced Status Codes | 향상된 상태 코드 확장 |
| RFC 2821 | SMTP | SMTP 갱신 (RFC 5321로 대체) |
| RFC 2920 | PIPELINING | SMTP 파이프라이닝 |
| RFC 3030 | CHUNKING | 청크 전송 |
| RFC 3207 | STARTTLS | SMTP TLS 확장 |
| RFC 3461 | DSN | 배달 상태 알림 |
| RFC 3463 | Enhanced Status Codes | 향상된 상태 코드 |
| RFC 4954 | AUTH | SMTP 인증 |
| RFC 5322 | IMF | 인터넷 메시지 형식 |
| RFC 6152 | 8BITMIME | 8비트 MIME 전송 |
| RFC 6531 | SMTPUTF8 | 국제화 이메일 주소 |
| RFC 7208 | SPF | 발신자 정책 프레임워크 |
| RFC 6376 | DKIM | DomainKeys Identified Mail |
| RFC 7489 | DMARC | 도메인 기반 메시지 인증 |

### 외부 링크

- [RFC 5321 원문 (IETF)](https://datatracker.ietf.org/doc/html/rfc5321)
- [RFC 5321 (RFC Editor)](https://www.rfc-editor.org/rfc/rfc5321)
- [IANA SMTP Service Extension Registry](https://www.iana.org/assignments/mail-parameters/)

---

*이 문서는 RFC 5321의 한국어 번역 및 정리본입니다.*
