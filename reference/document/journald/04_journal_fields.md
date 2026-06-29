# Journal Fields 레퍼런스

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.journal-fields.html

---

## 목차

1. [필드 분류](#필드-분류)
2. [User Journal 필드](#user-journal-필드)
3. [Trusted Journal 필드 (`_` 접두)](#trusted-journal-필드-_-접두)
4. [Kernel 필드](#kernel-필드)
5. [감사(audit) 필드](#감사audit-필드)
6. [Address 필드 (`__`)](#address-필드-__)
7. [Code 필드](#code-필드)
8. [필드 찾기와 활용](#필드-찾기와-활용)
9. [참고 자료](#참고-자료)

---

## 필드 분류

journal 엔트리의 필드는 접두사로 분류됩니다:

| 접두사 | 종류 | 설명 |
| --- | --- | --- |
| (없음) | User | 애플리케이션이 보낸 값. 위조 가능 |
| `_` | Trusted | journald가 커널에서 검증. 신뢰 가능 |
| `__` | Address | 메시지 자체의 주소·시각 |

---

## User Journal 필드

애플리케이션이 자유롭게 설정 가능한 필드. 신뢰할 수는 없지만 가장 풍부한 정보.

### MESSAGE

로그 본문. 사람이 읽는 메시지.

```
MESSAGE=Server started on port 8080
```

### MESSAGE_ID

128비트 UUID. 메시지의 **종류** 를 식별. 같은 종류의 메시지는 같은 UUID를 갖습니다. catalog와 결합해 다국어 설명·해결책을 제공할 수 있음.

```
MESSAGE_ID=abc12345-...
```

systemd가 발생시키는 표준 메시지 ID는 [systemd 카탈로그](https://github.com/systemd/systemd/tree/main/catalog)에 정의되어 있습니다.

### PRIORITY

syslog 레벨. 0(emerg) ~ 7(debug).

```
PRIORITY=6     # info
```

### CODE_FILE / CODE_LINE / CODE_FUNC

소스 위치 정보. `sd_journal_send` 매크로가 자동 채움.

```
CODE_FILE=server.c
CODE_LINE=123
CODE_FUNC=handle_request
```

### ERRNO

C errno 값.

```
ERRNO=2     # ENOENT
```

### SYSLOG_FACILITY / SYSLOG_IDENTIFIER / SYSLOG_PID

syslog 호환 필드.

```
SYSLOG_FACILITY=3       # daemon
SYSLOG_IDENTIFIER=nginx
SYSLOG_PID=1234
SYSLOG_TIMESTAMP=...
```

### SYSLOG_RAW

원본 syslog 메시지 (가공 전).

### DOCUMENTATION

이 메시지에 관련된 문서 URL.

### TID

스레드 ID (`gettid()`).

---

## Trusted Journal 필드 (`_` 접두)

journald가 메시지 수신 시 커널에서 직접 가져와 추가. 애플리케이션이 위조 불가.

### 프로세스 식별

| 필드 | 의미 |
| --- | --- |
| `_PID` | 프로세스 ID |
| `_UID` | 사용자 UID |
| `_GID` | 그룹 GID |
| `_COMM` | 명령어 이름 (`/proc/<pid>/comm`) |
| `_EXE` | 실행 파일 경로 (`/proc/<pid>/exe` 의 readlink) |
| `_CMDLINE` | 명령행 (`/proc/<pid>/cmdline`) |
| `_CAP_EFFECTIVE` | 실효 capability |

### systemd 컨텍스트

| 필드 | 의미 |
| --- | --- |
| `_SYSTEMD_CGROUP` | cgroup 경로 |
| `_SYSTEMD_SLICE` | 가장 가까운 slice |
| `_SYSTEMD_UNIT` | 시스템 unit |
| `_SYSTEMD_USER_UNIT` | 사용자 unit |
| `_SYSTEMD_USER_SLICE` | 사용자 slice |
| `_SYSTEMD_SESSION` | 로그인 세션 ID |
| `_SYSTEMD_OWNER_UID` | 사용자 매니저의 UID |
| `_SYSTEMD_INVOCATION_ID` | unit의 이번 invocation 고유 ID |

### 호스트와 부팅

| 필드 | 의미 |
| --- | --- |
| `_HOSTNAME` | 호스트명 |
| `_MACHINE_ID` | `/etc/machine-id` |
| `_BOOT_ID` | `/proc/sys/kernel/random/boot_id` (재부팅마다 변경) |

### 보안

| 필드 | 의미 |
| --- | --- |
| `_SELINUX_CONTEXT` | SELinux 컨텍스트 |
| `_AUDIT_SESSION` | audit 세션 ID |
| `_AUDIT_LOGINUID` | 로그인 UID (sudo 후에도 원래 사용자) |

### 전송

| 필드 | 의미 |
| --- | --- |
| `_TRANSPORT` | `kernel`, `journal`, `stdout`, `syslog`, `audit`, `driver` |
| `_STREAM_ID` | stdout/stderr 스트림의 고유 ID |
| `_LINE_BREAK` | stdout 줄이 어떻게 잘렸는지 (`nul`, `line-max`, `eof`, `pid-change`) |

### Namespace

| 필드 | 의미 |
| --- | --- |
| `_NAMESPACE` | journal namespace 이름 |

---

## Kernel 필드

`_TRANSPORT=kernel` 인 메시지의 추가 필드.

| 필드 | 의미 |
| --- | --- |
| `_KERNEL_DEVICE` | 디바이스 노드 (예: `b8:0` block 8:0) |
| `_KERNEL_SUBSYSTEM` | 서브시스템 (예: `block`, `net`) |
| `_UDEV_SYSNAME` | sysfs 이름 |
| `_UDEV_DEVNODE` | 디바이스 파일 |
| `_UDEV_DEVLINK` | 심볼릭 링크 |

---

## 감사(audit) 필드

`_TRANSPORT=audit` 메시지의 필드.

| 필드 | 의미 |
| --- | --- |
| `_AUDIT_TYPE` | audit 이벤트 타입 |
| `_AUDIT_TYPE_NAME` | 사람이 읽는 이름 |
| `_AUDIT_ID` | 이벤트 ID |
| `_AUDIT_FIELD_*` | audit이 보낸 추가 필드들 |

---

## Address 필드 (`__`)

엔트리 자체의 메타 정보.

### __REALTIME_TIMESTAMP

UNIX epoch 마이크로초.

```
__REALTIME_TIMESTAMP=1715161234000000
```

### __MONOTONIC_TIMESTAMP

`CLOCK_MONOTONIC` 기반 마이크로초. 부팅 후 경과 시간.

### __CURSOR

엔트리의 고유 cursor 문자열. 다음에 이 위치로 점프할 때 사용.

```bash
journalctl --cursor="s=abc;i=123;b=def;m=456;t=ghi;x=jkl"
journalctl --after-cursor=...
journalctl --since-cursor=...
journalctl --show-cursor
```

cursor를 파일에 저장해 뒀다가 재시작 후 그 시점부터 이어서 읽는 방식으로 활용할 수 있습니다.

---

## Code 필드

C/C++ 외 언어에서 호환을 위해 사용:

| 필드 | 의미 |
| --- | --- |
| `OBJECT_PID` | 메시지가 다른 프로세스에 관한 것일 때 그 PID |
| `OBJECT_UID` | 그 프로세스의 UID |
| `OBJECT_*` | 다른 프로세스의 trusted 필드들 |

예: 로그인 데몬이 사용자 X의 활동을 기록할 때 OBJECT_UID=X.

---

## 필드 찾기와 활용

### 가능한 필드 목록

```bash
journalctl --fields
```

### 특정 필드의 모든 값

```bash
$ journalctl -F _SYSTEMD_UNIT
nginx.service
postgresql.service
sshd.service
...

$ journalctl -F PRIORITY
3
4
6
```

### JSON으로 모든 필드 보기

```bash
journalctl -n 1 -o json-pretty
```

### 필터 결합

```bash
# nginx의 warning 이상, 어제 이후
journalctl _SYSTEMD_UNIT=nginx.service PRIORITY=4 --since yesterday

# 특정 cgroup 안의 모든 로그
journalctl _SYSTEMD_CGROUP=/system.slice/docker.service

# 특정 사용자의 활동
journalctl _UID=1000

# python 인터프리터로 실행된 모든 프로세스의 로그
journalctl _COMM=python3
```

### 사용자 정의 필드 보내기

C에서:

```c
sd_journal_send(
    "MESSAGE=order placed",
    "PRIORITY=6",
    "ORDER_ID=%s", order_id,
    "USER_ID=%d", user_id,
    "AMOUNT=%lld", amount_cents,
    NULL);
```

조회:

```bash
journalctl ORDER_ID=ORD-12345
journalctl USER_ID=42 AMOUNT=10000
```

`MESSAGE_ID` 까지 부여하면 카탈로그와 연계 가능.

---

## 참고 자료

- [man systemd.journal-fields](https://www.freedesktop.org/software/systemd/man/systemd.journal-fields.html)
- [man sd_journal_send](https://www.freedesktop.org/software/systemd/man/sd_journal_send.html)
- [systemd Catalog](https://github.com/systemd/systemd/tree/main/catalog)
