# journald 개요

> 원본: https://www.freedesktop.org/software/systemd/man/systemd-journald.service.html

<a id="journald란"></a>
## journald란?

`systemd-journald`는 systemd가 제공하는 로그 수집 데몬이다. 부팅부터 종료까지 커널, init, 서비스, syslog, stdout/stderr의 로그를 한곳에 모아 구조화된 바이너리 형식으로 저장한다.

전통적인 syslog와 비교하면 로그 본문 외에 구조와 메타데이터를 함께 관리한다는 차이가 있다.

- 구조화된 필드(key=value)
- 신뢰할 수 있는 메타데이터(PID, UID, cgroup 등 커널 검증)
- 무결성 보호(sealing, FSS)
- 빠른 검색(인덱스 기반)
- 순환 보관(디스크 사용량 자동 관리)

<a id="핵심-특징"></a>

## 핵심 특징

### 통합 수집

journald는 다음 소스에서 받은 메시지를 단일 로그 스트림으로 모은다.

- 커널 메시지(`/dev/kmsg`)
- 부팅 초기 메시지(initrd, early userspace)
- 서비스의 stdout/stderr
- syslog 호환 소켓(`/dev/log`, `journal/syslog`)
- 네이티브 journal API(`/run/systemd/journal/socket`)
- 사용자 audit 메시지(`AUDIT_*`)

### 기본 활성화

- systemd 기반 시스템에서 journald는 PID 1과 함께 자동으로 시작 → 별도 활성화 불필요

### journalctl

- journal에 저장된 데이터를 조회하는 클라이언트 도구
  - 별도 챕터에서 다룸

<a id="로그-소스"></a>

## 로그 소스

### 커널 로그

- `/dev/kmsg`에서 읽음
  - `dmesg`와 같은 데이터지만 journal에서는 시간 동기화, 메타데이터가 더 풍부함

```bash
journalctl -k             # 커널 로그만
journalctl --dmesg        # 동일
```

### 서비스 stdout/stderr

systemd가 unit을 시작할 때 stdout/stderr 파이프를 만들면 journald가 여기서 로그를 수집한다. 따라서 별도 logger 호출 없이 `printf`나 `println`으로 출력한 메시지도 수집할 수 있다.

```ini
[Service]
StandardOutput=journal       # 기본값
StandardError=journal
```

### syslog 호환

journald는 `/dev/log` 유닉스 소켓에서 메시지를 받는다. 그래서 기존 syslog API(`syslog(3)`, `logger`)를 사용하는 프로그램의 로그도 journald로 모인다.

```bash
logger -t myapp "Hello from CLI"
# journalctl -t myapp 로 확인 가능
```

### 네이티브 API

- 구조화된 필드를 직접 보내는 API:

```c
#include <systemd/sd-journal.h>

sd_journal_send(
    "MESSAGE=User logged in",
    "PRIORITY=6",
    "USER_ID=%d", uid,
    "REQUEST_ID=%s", request_id,
    NULL);
```

- 또는 셸:

```bash
echo "MESSAGE=event" | systemd-cat -t myapp
```

<a id="저장-형식"></a>

## 저장 형식

### 디스크 위치

- `/var/log/journal/<machine-id>/`: 영구(persistent) 저장 용도
- `/run/log/journal/<machine-id>/`: 휘발성(volatile, RAM) 저장 용도

- `/var/log/journal/` 디렉터리가 존재하면 영구 저장 → 없으면 RAM에만 저장

- 영구 저장 활성화:

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

- 각 파일은 자체 인덱스를 가진 바이너리 형식이며 mmap 기반으로 빠르게 접근 가능

### 압축

- 저장 시 자동 압축(LZ4 또는 zstd). 텍스트 로그라 압축률이 매우 높음

<a id="구조화된-로그"></a>

## 구조화된 로그

journal은 로그 엔트리를 여러 필드(key=value)의 묶음으로 저장한다. 메시지 본문뿐 아니라 프로세스와 서비스 정보도 필드로 구분해 다룰 수 있다는 점이 특징이다.

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

이처럼 프로세스 ID와 unit 이름이 별도 필드에 저장되므로, `_PID=1234` 또는 `_SYSTEMD_UNIT=nginx.service`로 원하는 로그를 정확하게 걸러낼 수 있다.

<a id="신뢰할-수-있는-메타데이터"></a>

## 신뢰할 수 있는 메타데이터

- `_`로 시작하는 필드는 journald가 커널과 검증한 신뢰 가능한 메타데이터임
  - 애플리케이션이 위조 불가

### 신뢰 메타데이터

- `_PID`: 프로세스 ID
- `_UID`, `_GID`: UID, GID
- `_COMM`: 프로세스 이름(`/proc/<pid>/comm`)
- `_EXE`: 실행 파일 경로
- `_CMDLINE`: 명령행
- `_SYSTEMD_UNIT`: 어느 unit에서 발생했는지
- `_SYSTEMD_USER_UNIT`: 사용자 unit
- `_SYSTEMD_CGROUP`: cgroup 경로
- `_SYSTEMD_SLICE`: slice
- `_SELINUX_CONTEXT`: SELinux 컨텍스트
- `_AUDIT_LOGINUID`: 로그인 UID
- `_BOOT_ID`: 부팅 인스턴스
- `_MACHINE_ID`: 머신 ID
- `_HOSTNAME`: 호스트명
- `_TRANSPORT`: journal/stdout/syslog/kernel/audit

### 애플리케이션 필드

- `_`가 없는 필드는 애플리케이션이 보낸 값:
- `MESSAGE`: 로그 메시지 본문
- `PRIORITY`: syslog 레벨(0~7)
- `SYSLOG_FACILITY`: syslog facility
- `SYSLOG_IDENTIFIER`: tag
- `CODE_FILE`, `CODE_LINE`, `CODE_FUNC`: 소스 위치
- `MESSAGE_ID`: 메시지 종류를 식별하는 UUID
- 사용자 정의(`REQUEST_ID`, `USER_ID` 등): 자유롭게 추가 가능

### Priority 값

- syslog 표준과 동일:
- 0: emerg (단축 emerg)
- 1: alert (단축 alert)
- 2: crit (단축 crit)
- 3: err (단축 err)
- 4: warning (단축 warning)
- 5: notice (단축 notice)
- 6: info (단축 info)
- 7: debug (단축 debug)

<a id="syslog와의-관계"></a>

## syslog와의 관계

journald만으로 로그를 관리할 수도 있고, 기존 syslog 데몬과 함께 사용할 수도 있다. 중앙 수집이나 기존 도구와의 연동이 필요한지에 따라 구성을 선택한다.

### journald만

- 대부분의 현대 systemd 시스템 기본 구성 → rsyslog/syslog-ng를 별도로 설치하지 않은 경우

### 공존 모드

- journald는 모든 로그를 받고, 일부를 syslog 데몬에 전달:

```ini
# /etc/systemd/journald.conf
[Journal]
ForwardToSyslog=yes
```

- rsyslog/syslog-ng는 `imjournal` 모듈로 journal에서 직접 읽거나 `/run/systemd/journal/syslog` 소켓에서 받음

- 용도:
- 중앙 로그 수집(rsyslog → 원격 서버)
- 기존 syslog 기반 SIEM 연동
- 텍스트 로그 파일(`/var/log/messages` 같은) 유지

### 전달 옵션

```ini
[Journal]
ForwardToSyslog=yes        # syslog 소켓으로
ForwardToKMsg=no           # 커널 ring buffer로
ForwardToConsole=no        # 콘솔로
ForwardToWall=yes          # 모든 터미널로(긴급 메시지)
TTYPath=/dev/console
MaxLevelConsole=info
MaxLevelWall=emerg
```

<a id="참고-자료"></a>

## 참고 자료

- [man systemd-journald.service](https://www.freedesktop.org/software/systemd/man/systemd-journald.service.html)
- [man systemd.journal-fields](https://www.freedesktop.org/software/systemd/man/systemd.journal-fields.html)
- [Lennart: The Journal](http://0pointer.de/blog/projects/journalctl.html)
