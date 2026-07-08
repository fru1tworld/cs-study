# RFC 9051 - IMAP4rev2 (Internet Message Access Protocol)

> 발행일: 2021년 8월
> 상태: Proposed Standard (RFC 3501을 대체)

## 1. 개요

IMAP(Internet Message Access Protocol)은 메일 서버에 저장된 메시지를 클라이언트가 원격에서 조회·검색·조작할 수 있게 하는 프로토콜이다. POP3와 달리 메시지를 로컬로 다운로드해 삭제하는 방식이 아니라, 서버에 메일을 둔 채로 여러 클라이언트(웹메일, 데스크톱, 모바일)가 같은 메일함 상태를 공유하도록 설계됐다.

IMAP4rev2(RFC 9051)는 1996년 나온 [RFC 3501 IMAP4rev1](./RFC3501-IMAP-Obsolete.md)을 대체하는 개정판으로, 그동안 확장(extension)으로 흩어져 있던 기능 다수를 본문에 통합했다.

| 구분 | IMAP4rev1 (RFC 3501) | IMAP4rev2 (RFC 9051) |
|------|----------------------|----------------------|
| 상태 | Obsolete | 현재 표준 |
| UTF-8 | 확장(IMAP4rev1 + UTF8=ACCEPT) 필요 | 기본 내장 |
| 네임스페이스 | 확장 필요 | 기본 내장 |
| ID 명령 | 확장 필요 | 기본 내장 |
| ESEARCH/CONDSTORE 등 | 별도 RFC 확장 | 다수 통합 |
| LOGIN 평문 | 허용 | 기본 비활성(암호화 채널 전제) |

---

## 2. 연결과 상태 모델

IMAP 연결은 4가지 상태를 오간다.

```
[연결 수립]
     │
     ▼
Not Authenticated ──LOGIN/AUTHENTICATE──▶ Authenticated
                                              │
                                          SELECT/EXAMINE
                                              │
                                              ▼
                                           Selected
                                              │
                                            LOGOUT
                                              ▼
                                            Logout
```

| 상태 | 설명 |
|------|------|
| Not Authenticated | 연결은 됐지만 인증 전 |
| Authenticated | 인증 완료, 특정 메일함 미선택 |
| Selected | 특정 메일함(mailbox)을 선택해 메시지 조작 가능 |
| Logout | 연결 종료 절차 중 |

---

## 3. 명령어 구조

IMAP 명령은 클라이언트가 붙이는 태그(tag)로 요청-응답을 짝짓는다.

```
C: a001 LOGIN alice password
S: a001 OK LOGIN completed

C: a002 SELECT INBOX
S: * 172 EXISTS
S: * 1 RECENT
S: * OK [UNSEEN 12] Message 12 is first unseen
S: * OK [UIDVALIDITY 3857529045] UIDs valid
S: * FLAGS (\Answered \Flagged \Deleted \Seen \Draft)
S: a002 OK [READ-WRITE] SELECT completed
```

| 응답 종류 | 표기 | 의미 |
|-----------|------|------|
| 태그 있는 응답 | `a001 OK ...` | 해당 명령의 최종 결과 |
| 상태 응답 | `* OK`, `* NO`, `* BAD` | 서버가 임의 시점에 보내는 상태 정보 |
| 데이터 응답 | `* 172 EXISTS` | 메일함 상태 변경 통지 |

### 3.1 주요 명령어

| 명령 | 설명 |
|------|------|
| `LOGIN` / `AUTHENTICATE` | 인증 |
| `SELECT` / `EXAMINE` | 메일함 열기 (쓰기/읽기 전용) |
| `FETCH` | 메시지 본문·헤더·플래그 조회 |
| `STORE` | 플래그(읽음, 삭제 표시 등) 변경 |
| `SEARCH` | 조건 검색 |
| `COPY` / `MOVE` | 메시지 이동/복사 |
| `EXPUNGE` | `\Deleted` 플래그가 붙은 메시지 영구 삭제 |
| `NAMESPACE` | 개인/공유/기타 사용자 네임스페이스 조회 |
| `IDLE` | 실시간 변경 통지 대기 ([RFC 2177](./RFC2177-IMAP-IDLE.md)) |

---

## 4. IMAP4rev2에서 통합된 주요 확장

| 기능 | 이전에는 | IMAP4rev2에서 |
|------|----------|----------------|
| UTF-8 지원 | RFC 6855 확장 | 기본 |
| NAMESPACE | RFC 2342 확장 | 기본 |
| ID | RFC 2971 확장 | 기본 |
| ESEARCH (확장 검색) | RFC 4731 | 기본 |
| SEARCHRES | RFC 5182 | 기본 |
| ENABLE | RFC 5161 | 기본 |
| LIST-EXTENDED | RFC 5258 | 기본 |
| SPECIAL-USE 메일함(\Sent, \Trash 등) | RFC 6154 | 기본 |
| MOVE 명령 | RFC 6851 | 기본 |
| STATUS=SIZE | RFC 8438 | 기본 |
| LIST-STATUS | RFC 5819 | 기본 |

---

## 5. UID와 메시지 시퀀스 번호

| 식별자 | 특징 |
|--------|------|
| Sequence Number | 현재 세션에서 메일함 내 순번. `EXPUNGE` 발생 시 재배치됨 |
| UID (Unique Identifier) | 메일함 내에서 영속적으로 유지되는 고유 번호. `UIDVALIDITY`가 바뀌지 않는 한 재사용되지 않음 |

클라이언트가 로컬 캐시와 서버 상태를 동기화할 때는 시퀀스 번호가 아니라 UID + UIDVALIDITY 조합을 사용해야 한다.

---

## 6. 요약

- IMAP4rev2는 그간 파편화된 확장들을 표준 본문에 통합한 현행 IMAP 표준이다.
- 상태 기반(Not Authenticated → Authenticated → Selected) 프로토콜이며, 태그로 요청-응답을 매칭한다.
- 실시간 알림은 [RFC 2177 IDLE](./RFC2177-IMAP-IDLE.md) 확장을 사용한다.
- 구형 클라이언트/서버는 여전히 [RFC 3501 IMAP4rev1](./RFC3501-IMAP-Obsolete.md) 기반으로 동작하는 경우가 많다.

---

## 참고 자료

- [RFC 9051 원문](https://datatracker.ietf.org/doc/rfc9051/)
- [RFC 3501 IMAP4rev1 (Obsolete)](./RFC3501-IMAP-Obsolete.md)
- [RFC 2177 IMAP IDLE](./RFC2177-IMAP-IDLE.md)
