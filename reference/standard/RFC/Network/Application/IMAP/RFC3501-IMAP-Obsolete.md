# RFC 3501 - IMAP4rev1 (Obsolete)

- 발행일: 2003년 3월
- 상태: Obsolete ([RFC 9051 IMAP4rev2](./RFC9051-IMAP4rev2.md)로 대체됨)

## 1. 개요

- RFC 3501: 1996년 첫 발행된 IMAP4rev1을 정리한 개정판
  - 20년 넘게 사실상 표준 IMAP 구현체 대부분(Dovecot·Cyrus·Courier 등 초기 버전)의 기반
- 2021년 [RFC 9051 IMAP4rev2](./RFC9051-IMAP4rev2.md)가 공식 대체 → 그러나 여전히 많은 서버/클라이언트가 IMAP4rev1 기준으로 동작하거나 두 버전을 함께 지원
- 이 문서: IMAP4rev1 고유 사항과 IMAP4rev2로의 주요 변경점 중심으로 정리
  - 공통 개념(상태 모델, 명령 구조, UID)은 [RFC 9051](./RFC9051-IMAP4rev2.md) 문서 참고

---

## 2. IMAP4rev1과 IMAP4rev2의 차이

- 문자 인코딩
  - IMAP4rev1: 기본 ASCII, UTF-8은 별도 확장(RFC 6855) 필요
  - IMAP4rev2: UTF-8 기본 지원
- 메일함 이름 인코딩
  - IMAP4rev1: Modified UTF-7
  - IMAP4rev2: UTF-8
- NAMESPACE
  - IMAP4rev1: 확장(RFC 2342)
  - IMAP4rev2: 기본 명령
- ID 명령
  - IMAP4rev1: 확장(RFC 2971)
  - IMAP4rev2: 기본 명령
- SEARCH 응답
  - IMAP4rev1: 시퀀스 번호만
  - IMAP4rev2: ESEARCH 확장 형식 기본 지원
- LSUB
  - IMAP4rev1: 존재(구독 목록 조회)
  - IMAP4rev2: 폐지 → LIST의 `(SUBSCRIBED)` 옵션으로 대체
- RECENT 응답
  - IMAP4rev1: 존재
  - IMAP4rev2: 폐지
- STATUS 항목
  - IMAP4rev1: MESSAGES·RECENT·UIDNEXT·UIDVALIDITY·UNSEEN
  - IMAP4rev2: RECENT 제거, SIZE 추가

---

## 3. IMAP4rev1 고유 명령/응답

### 3.1 LSUB / SUBSCRIBE / UNSUBSCRIBE

```
C: a1 SUBSCRIBE "INBOX.Work"
S: a1 OK SUBSCRIBE completed

C: a2 LSUB "" "*"
S: * LSUB () "." "INBOX.Work"
S: a2 OK LSUB completed
```

- 구독한 메일함만 별도로 관리하는 방식
- IMAP4rev2에서는 `LIST (SUBSCRIBED) ""`로 통합

### 3.2 RECENT 응답

```
S: * 5 RECENT
```

- 마지막 세션 이후 새로 도착한 메시지 수를 나타냄
- 여러 클라이언트가 동시 접속할 때 의미가 모호해지는 문제 → IMAP4rev2에서 제거, `UNSEEN` 상태로 대체

### 3.3 Modified UTF-7 메일함 이름

- 비-ASCII 메일함 이름은 Modified UTF-7로 인코딩 필요

```
"받은편지함" → "&TttjBg-" (Modified UTF-7 예시 형태)
```

- IMAP4rev2는 이 복잡한 인코딩 규칙 없이 UTF-8을 직접 사용

---

## 4. 마이그레이션 시 유의점

- 서버 구현: 두 버전을 동시에 지원하며 클라이언트가 `CAPABILITY`로 `IMAP4rev1` / `IMAP4rev2`를 확인하도록 설계
- 클라이언트 구현: `LOGIN` 전 `CAPABILITY` 응답을 확인해 지원 버전에 맞는 명령 집합 사용
- 메일함 이름: UTF-7/UTF-8 혼용 환경에서는 항상 인코딩을 명시적으로 확인
- RECENT 의존 로직: IMAP4rev2 서버에서는 동작하지 않음 → UNSEEN/UID 기반 로직으로 전환 필요

---

## 5. 요약

- RFC 3501(IMAP4rev1): 오랫동안 사실상 표준이었으나 현재는 Obsolete 상태
- IMAP4rev2: UTF-8·NAMESPACE·ID 등 흩어져 있던 확장을 통합, RECENT/LSUB처럼 모호했던 기능 정리
- 레거시 서버/클라이언트와의 호환을 위해 `CAPABILITY` 응답 기반 분기 처리 여전히 필요

---

## 참고 자료

- [RFC 3501 원문](https://www.rfc-editor.org/rfc/rfc3501)
- [RFC 9051 IMAP4rev2](./RFC9051-IMAP4rev2.md)
- [RFC 2177 IMAP IDLE](./RFC2177-IMAP-IDLE.md)
