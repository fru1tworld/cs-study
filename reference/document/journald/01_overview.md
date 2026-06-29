# journald 개요

> 원본: https://www.freedesktop.org/software/systemd/man/systemd-journald.service.html

---

## 목차

1. [journald란?](#journald란)
2. [핵심 특징](#핵심-특징)
3. [로그 소스](#로그-소스)
4. [저장 형식](#저장-형식)
5. [구조화된 로그](#구조화된-로그)
6. [신뢰할 수 있는 메타데이터](#신뢰할-수-있는-메타데이터)
7. [syslog와의 관계](#syslog와의-관계)
8. [참고 자료](#참고-자료)

---

## journald란?

`systemd-journald` 는 systemd가 제공하는 **로그 수집 데몬** 입니다. 시스템 부팅부터 종료까지의 모든 로그(커널, init, 서비스, syslog, stdout/stderr)를 한 곳에 모아 구조화된 바이너리 형식으로 저장합니다.

전통적인 syslog 대비 가장 큰 차이:
- **구조화된 필드** (key=value)
- **신뢰할 수 있는 메타데이터** (PID, UID, cgroup 등 커널 검증)
- **무결성 보호** (sealing, FSS)
- **빠른 검색** (인덱스 기반)
- **순환 보관** (디스크 사용량 자동 관리)

---

## 핵심 특징

### 통합 수집

journald는 다음 모든 소스를 단일 로그 스트림에 통합합니다:
- 커널 메시지 (`/dev/kmsg`)
- 부팅 초기 메시지 (initrd, early userspace)
- 서비스의 stdout/stderr
- syslog 호환 소켓 (`/dev/log`, `journal/syslog`)
- 네이티브 journal API (`/run/systemd/journal/socket`)
- 사용자 audit 메시지 (`AUDIT_*`)

### 기본 활성화

systemd 기반 시스템에서 journald는 PID 1과 함께 자동으로 시작되며 별도 활성화가 필요 없습니다.

### journalctl

journal에 저장된 데이터를 조회하는 클라이언트 도구. 별도 챕터에서 다룹니다.

---

## 로그 소스

### 커널 로그

`/dev/kmsg` 에서 읽습니다. `dmesg` 와 같은 데이터지만 journal에서는 시간 동기화·메타데이터가 더 풍부합니다.

```bash
journalctl -k             # 커널 로그만
journalctl --dmesg        # 동일
```

### 서비스 stdout/stderr

systemd가 unit을 시작할 때 stdout/stderr 파이프를 만들고 journald가 수집합니다. 별도 logger 호출 없이도 `printf` / `println` 만으로 로그가 자동 수집됩니다.

```ini
[Service]
StandardOutput=journal       # 기본값
StandardError=journal
```

### syslog 호환

`/dev/log` 유닉스 소켓을 journald가 listen합니다. 기존 syslog API(`syslog(3)`, `logger`)를 사용하는 모든 프로그램이 자동으로 journald로 흘러듭니다.

```bash
logger -t myapp "Hello from CLI"
# journalctl -t myapp 로 확인 가능
```

### 네이티브 API

구조화된 필드를 직접 보내는 API:

```c
#include <systemd/sd-journal.h>

sd_journal_send(
    "MESSAGE=User logged in",
    "PRIORITY=6",
    "USER_ID=%d", uid,
    "REQUEST_ID=%s", request_id,
    NULL);
```

또는 셸:

```bash
echo "MESSAGE=event" | systemd-cat -t myapp
```

---

## 저장 형식

### 디스크 위치

| 경로 | 용도 |
| --- | --- |
| `/var/log/journal/<machine-id>/` | 영구 (persistent) |
| `/run/log/journal/<machine-id>/` | 휘발성 (volatile, RAM) |

`/var/log/journal/` 디렉터리가 존재하면 영구 저장, 없으면 RAM에만 저장됩니다.

영구 저장 활성화:

```bash
sudo mkdir -p /var/log/journal
sudo systemd-tmpfiles --create --prefix /var/log/journal
sudo systemctl restart systemd-journald
```

### 파일 종류

```
/var/log/journal/abc123def456/
├── system.journal              # 시스템 활성 파일
├── user-1000.journal           # 사용자별 활성 파일
├── system@xxx-yyy.journal      # 회전된 파일
└── ...
```

각 파일은 자체 인덱스를 가진 바이너리 형식이며 mmap 기반으로 빠르게 접근할 수 있습니다.

### 압축

저장 시 자동 압축 (LZ4 또는 zstd). 텍스트 로그라 압축률이 매우 높습니다.

---

## 구조화된 로그

journal의 핵심 차별점. 각 로그 엔트리는 단순 텍스트 한 줄이 아니라 **여러 필드(key=value)** 의 묶음입니다.

```
$ journalctl -o json-pretty -n 1
{
    "_TRANSPORT" : "stdout",
    "PRIORITY" : "6",
    "SYSLOG_FACILITY" : "3",
    "SYSLOG_IDENTIFIER" : "nginx",
    "MESSAGE" : "Server started on port 80",
    "_PID" : "1234",
    "_UID" : "33",
    "_GID" : "33",
    "_COMM" : "nginx",
    "_EXE" : "/usr/sbin/nginx",
    "_CMDLINE" : "nginx: master process /usr/sbin/nginx",
    "_SYSTEMD_UNIT" : "nginx.service",
    "_SYSTEMD_CGROUP" : "/system.slice/nginx.service",
    "_BOOT_ID" : "abc123...",
    "_MACHINE_ID" : "xyz789...",
    "_HOSTNAME" : "server01",
    "__REALTIME_TIMESTAMP" : "1715161234000000",
    ...
}
```

이 덕분에 `_PID=1234` 또는 `_SYSTEMD_UNIT=nginx.service` 같은 정확한 필터가 가능합니다.

---

## 신뢰할 수 있는 메타데이터

`_` 로 시작하는 필드는 **journald가 커널과 검증한 신뢰 가능한 메타데이터** 입니다. 애플리케이션이 위조할 수 없습니다.

### 신뢰 메타데이터

| 필드 | 의미 |
| --- | --- |
| `_PID` | 프로세스 ID |
| `_UID`, `_GID` | UID, GID |
| `_COMM` | 프로세스 이름 (`/proc/<pid>/comm`) |
| `_EXE` | 실행 파일 경로 |
| `_CMDLINE` | 명령행 |
| `_SYSTEMD_UNIT` | 어느 unit에서 발생했는지 |
| `_SYSTEMD_USER_UNIT` | 사용자 unit |
| `_SYSTEMD_CGROUP` | cgroup 경로 |
| `_SYSTEMD_SLICE` | slice |
| `_SELINUX_CONTEXT` | SELinux 컨텍스트 |
| `_AUDIT_LOGINUID` | 로그인 UID |
| `_BOOT_ID` | 부팅 인스턴스 |
| `_MACHINE_ID` | 머신 ID |
| `_HOSTNAME` | 호스트명 |
| `_TRANSPORT` | journal/stdout/syslog/kernel/audit |

### 애플리케이션 필드

`_` 가 없는 필드는 애플리케이션이 보낸 값:

| 필드 | 의미 |
| --- | --- |
| `MESSAGE` | 로그 메시지 본문 |
| `PRIORITY` | syslog 레벨 (0~7) |
| `SYSLOG_FACILITY` | syslog facility |
| `SYSLOG_IDENTIFIER` | tag |
| `CODE_FILE`, `CODE_LINE`, `CODE_FUNC` | 소스 위치 |
| `MESSAGE_ID` | UUID — 메시지 종류 식별 |
| 사용자 정의 (`REQUEST_ID`, `USER_ID` 등) | 자유롭게 추가 가능 |

### Priority 값

syslog 표준과 동일:

| 값 | 이름 | 단축 |
| --- | --- | --- |
| 0 | emerg | emerg |
| 1 | alert | alert |
| 2 | crit | crit |
| 3 | err | err |
| 4 | warning | warning |
| 5 | notice | notice |
| 6 | info | info |
| 7 | debug | debug |

---

## syslog와의 관계

journald는 syslog를 완전히 대체할 수도 있고, 공존할 수도 있습니다.

### journald만

대부분의 현대 systemd 시스템 기본 구성으로, rsyslog/syslog-ng를 별도로 설치하지 않은 경우입니다.

### 공존 모드

journald는 모든 로그를 받고, 일부를 syslog 데몬에 전달:

```ini
# /etc/systemd/journald.conf
[Journal]
ForwardToSyslog=yes
```

rsyslog/syslog-ng는 `imjournal` 모듈로 journal에서 직접 읽거나 `/run/systemd/journal/syslog` 소켓에서 받습니다.

용도:
- 중앙 로그 수집 (rsyslog → 원격 서버)
- 기존 syslog 기반 SIEM 연동
- 텍스트 로그 파일 (`/var/log/messages` 같은) 유지

### 전달 옵션

```ini
[Journal]
ForwardToSyslog=yes        # syslog 소켓으로
ForwardToKMsg=no           # 커널 ring buffer로
ForwardToConsole=no        # 콘솔로
ForwardToWall=yes          # 모든 터미널로 (긴급 메시지)
TTYPath=/dev/console
MaxLevelConsole=info
MaxLevelWall=emerg
```

---

## 참고 자료

- [man systemd-journald.service](https://www.freedesktop.org/software/systemd/man/systemd-journald.service.html)
- [man systemd.journal-fields](https://www.freedesktop.org/software/systemd/man/systemd.journal-fields.html)
- [Lennart: The Journal](http://0pointer.de/blog/projects/journalctl.html)
