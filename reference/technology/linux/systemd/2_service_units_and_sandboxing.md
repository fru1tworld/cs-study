# Service Unit, 실행 환경과 샌드박스, 리소스 제어

## Service Unit

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.service.html

---

### 목차

1. [Service란?](#service란)
2. [Type=의 종류](#type의-종류)
3. [Exec 지시자](#exec-지시자)
4. [Restart 정책](#restart-정책)
5. [타임아웃](#타임아웃)
6. [환경 변수](#환경-변수)
7. [PID 파일과 알림](#pid-파일과-알림)
8. [실전 예제](#실전-예제)
9. [참고 자료](#참고-자료)

---

### Service란?

`.service` unit은 데몬 프로세스를 정의하는 가장 흔한 unit 종류입니다. systemd가 시작·중지·감시하는 프로세스의 라이프사이클을 기술합니다.

기본 구조:

```ini
[Unit]
Description=My Service
After=network.target

[Service]
Type=simple
ExecStart=/usr/local/bin/my-server

[Install]
WantedBy=multi-user.target
```

---

### Type=의 종류

`Type=` 은 systemd가 서비스의 시작 완료 시점을 어떻게 판단할지 결정합니다. 의존하는 다른 unit이 언제 시작되어야 하는지를 정하는 핵심 옵션입니다.

#### simple (기본값, 권장하지 않음)

`ExecStart` 의 프로세스가 fork하지 않고 포그라운드로 실행됩니다. systemd는 fork() 직후 즉시 unit을 active로 간주합니다.

문제: 프로세스가 실제로 준비됐는지(포트 바인딩 완료 등) 확인할 수 없어 의존하는 unit이 너무 빨리 시작될 수 있습니다.

#### exec (권장)

`simple` 과 비슷하지만, systemd는 **`execve()` 가 성공할 때까지** 대기합니다. 즉 바이너리가 실제로 로드된 시점을 확인합니다. 대부분의 경우 `simple` 대신 이 옵션을 써야 합니다.

#### forking

전통적인 데몬 패턴 — 프로세스가 fork하고 부모는 종료, 자식이 백그라운드에서 실행됩니다. `PIDFile=` 을 함께 지정해 systemd가 자식 PID를 추적할 수 있게 해야 합니다.

```ini
[Service]
Type=forking
ExecStart=/usr/sbin/old-style-daemon
PIDFile=/run/old-style-daemon.pid
```

레거시 데몬에만 사용. 새 서비스는 simple/exec/notify를 권장합니다.

#### oneshot

한 번 실행하고 끝나는 작업에 사용 (예: 부팅 시 설정 적용). systemd는 프로세스가 **종료될 때까지** 대기합니다.

```ini
[Service]
Type=oneshot
ExecStart=/usr/local/bin/setup-once.sh
RemainAfterExit=yes
```

`RemainAfterExit=yes` 를 주면 프로세스 종료 후에도 unit을 active로 표시합니다.

#### notify

서비스가 `sd_notify(3)` API로 시작 완료를 직접 알립니다. 가장 정확한 방식이며, systemd 자체와 nginx, openvpn 등이 이를 지원합니다.

```ini
[Service]
Type=notify
ExecStart=/usr/sbin/my-aware-service
NotifyAccess=main
```

서비스 코드 안에서:

```c
sd_notify(0, "READY=1");
```

또는 셸에서 `systemd-notify --ready` 호출.

#### notify-reload

`Type=notify` 와 같지만 `systemctl reload` 시 SIGHUP 후 `RELOADING=1` / `READY=1` 알림을 기다립니다 (systemd 253+).

#### dbus

`BusName=` 으로 지정한 D-Bus 이름을 가져올 때 unit이 active가 됩니다.

#### idle

다른 작업이 끝날 때까지 시작을 지연시킵니다. 부팅 메시지 충돌 방지용 — 거의 쓰이지 않습니다.

---

### Exec 지시자

#### ExecStart

서비스를 시작하는 명령. **절대 경로** 여야 합니다. shell 메타문자(`>`, `|`, `*`)는 직접 처리되지 않으므로 셸이 필요하면 `/bin/sh -c "..."` 로 감싸야 합니다.

```ini
ExecStart=/usr/local/bin/server --port 8080 --config /etc/myapp.conf
```

`Type=oneshot` 일 때만 여러 번 지정 가능.

#### ExecStartPre / ExecStartPost

`ExecStart` 직전/직후에 실행됩니다. 권한 설정, 디렉터리 생성, 헬스체크 등에 사용.

```ini
ExecStartPre=/bin/mkdir -p /var/lib/myapp
ExecStartPre=/bin/chown myapp:myapp /var/lib/myapp
ExecStart=/usr/local/bin/myapp
ExecStartPost=/usr/local/bin/notify-startup.sh
```

#### ExecReload

`systemctl reload` 시 실행할 명령. 보통 SIGHUP을 보냅니다.

```ini
ExecReload=/bin/kill -HUP $MAINPID
```

`$MAINPID` 는 systemd가 자동 제공하는 변수.

#### ExecStop / ExecStopPost

서비스를 깔끔하게 종료할 명령. 미지정 시 systemd는 SIGTERM → SIGKILL 순으로 보냅니다.

```ini
ExecStop=/usr/local/bin/myapp-shutdown
ExecStopPost=/bin/rm -f /run/myapp.lock
```

#### Exec 접두사

명령 앞에 특수 문자를 붙여 동작을 변경할 수 있습니다.

| 접두사 | 의미 |
| --- | --- |
| `-` | 명령 실패를 무시 |
| `@` | argv[0] 별도 지정 |
| `:` | 환경 변수 치환 비활성화 |
| `+` | 권한 제한 무시 (root로 실행) |
| `!` | User=/Group=/SupplementaryGroups= 만 적용하고 그 외 권한 옵션 무시 |
| `\|` | User= 의 기본 셸을 사용하거나, 접두사로 사용 시 셸(-c)을 통해 실행 |

```ini
ExecStartPre=-/bin/rm -f /tmp/old.sock
```

---

### Restart 정책

서비스가 종료됐을 때 자동 재시작 여부.

```ini
[Service]
Restart=on-failure
RestartSec=5
StartLimitIntervalSec=60
StartLimitBurst=3
```

#### Restart= 값

| 값 | 동작 |
| --- | --- |
| `no` | 재시작 안 함 (기본값) |
| `on-success` | 정상 종료(exit 0, SIGHUP/SIGINT/SIGTERM/SIGPIPE 등)일 때만 재시작 |
| `on-failure` | exit≠0, 시그널, 타임아웃, 코어덤프 시 재시작 |
| `on-abnormal` | 시그널/타임아웃/코어덤프 시 재시작 (exit code 무시) |
| `on-watchdog` | 워치독 타임아웃 시에만 재시작 |
| `on-abort` | 의도치 않은 시그널 (SIGABRT 등) 시 |
| `always` | 항상 재시작 (오류든 정상이든) |

#### 재시작 제어

- `RestartSec=5`: 재시작 전 대기 시간 (기본 100ms)
- `StartLimitIntervalSec=60` + `StartLimitBurst=3`: 60초 안에 3번 이상 시작 실패하면 더 시도하지 않음
- `StartLimitAction=`: 한도 초과 시 `reboot`, `poweroff` 등 가능

---

### 타임아웃

- `TimeoutStartSec=90s`: 시작이 이 시간 안에 완료되지 않으면 실패 처리. 기본 90초
- `TimeoutStopSec=90s`: SIGTERM 후 이 시간이 지나면 SIGKILL
- `TimeoutAbortSec=`: WatchdogSec 트리거 시 abort 시간
- `TimeoutSec=`: 위 두 개를 동시에 설정
- `RuntimeMaxSec=1h`: 서비스 최대 실행 시간 (이후 자동 종료)

`infinity` 를 주면 타임아웃이 비활성화됩니다.

#### 워치독

```ini
[Service]
Type=notify
WatchdogSec=30s
```

서비스가 30초마다 `sd_notify(0, "WATCHDOG=1")` 을 호출하지 않으면 systemd가 서비스를 SIGABRT(또는 `WatchdogSignal=` 에 지정된 시그널)로 종료하고 failed 상태로 전환합니다. `Restart=on-failure` 등을 설정하면 이후 자동 재시작됩니다. 데몬이 무한 루프에 빠진 경우를 잡아내는 데 유용.

---

### 환경 변수

```ini
[Service]
Environment="LOG_LEVEL=info"
Environment="DB_HOST=localhost" "DB_PORT=5432"
EnvironmentFile=-/etc/default/myapp
```

- `Environment=`: 직접 지정 (KEY=VALUE)
- `EnvironmentFile=`: 파일에서 읽기. `-` 접두사를 붙이면 파일이 없어도 무시
- `PassEnvironment=`: systemd 자체 환경에서 전달 (보통 컨테이너에서)

EnvironmentFile 형식:
```
LOG_LEVEL=debug
DB_HOST=localhost
# 주석 가능
```

셸 스크립트 형식이 아니므로 `export`, 변수 치환, 따옴표 처리 등은 지원되지 않거나 제한적입니다.

---

### PID 파일과 알림

`Type=forking` 일 때만 `PIDFile=` 이 의미 있습니다. systemd는 이 파일에서 PID를 읽어 메인 프로세스를 추적합니다.

```ini
PIDFile=/run/nginx.pid
```

PID 파일은 보안상 **시스템 런타임 디렉터리** (`/run`)에 두는 것이 좋습니다.

`NotifyAccess=` 는 `Type=notify` 일 때 어느 프로세스의 알림을 수락할지 정합니다:
- `none` (기본): 알림 비활성화
- `main`: 메인 프로세스만
- `exec`: ExecStart로 실행된 프로세스 모두
- `all`: 자식 프로세스 모두

---

### 실전 예제

#### 1. 단순 웹 서비스

```ini
[Unit]
Description=Hello Web Server
After=network-online.target
Wants=network-online.target

[Service]
Type=exec
User=hello
Group=hello
WorkingDirectory=/opt/hello
ExecStart=/opt/hello/server
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

#### 2. 환경 파일과 워치독을 쓰는 서비스

```ini
[Unit]
Description=Worker
After=network.target postgresql.service
Requires=postgresql.service

[Service]
Type=notify
EnvironmentFile=/etc/worker/env
ExecStart=/usr/local/bin/worker
WatchdogSec=30s
Restart=on-failure
RestartSec=10
TimeoutStartSec=120

[Install]
WantedBy=multi-user.target
```

#### 3. oneshot 초기화

```ini
[Unit]
Description=Apply iptables rules
DefaultDependencies=no
Before=network-pre.target
Wants=network-pre.target

[Service]
Type=oneshot
RemainAfterExit=yes
ExecStart=/usr/sbin/iptables-restore /etc/iptables/rules.v4
ExecStop=/usr/sbin/iptables -F

[Install]
WantedBy=multi-user.target
```

---

### 참고 자료

- [man systemd.service](https://www.freedesktop.org/software/systemd/man/systemd.service.html)
- [man sd_notify](https://www.freedesktop.org/software/systemd/man/sd_notify.html)
- [systemd by example](https://systemd-by-example.com/)

---

## 실행 환경과 샌드박스

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.exec.html

---

### 목차

1. [개요](#개요)
2. [사용자와 그룹](#사용자와-그룹)
3. [작업 디렉터리와 루트](#작업-디렉터리와-루트)
4. [Capabilities](#capabilities)
5. [네임스페이스 격리](#네임스페이스-격리)
6. [파일시스템 보호](#파일시스템-보호)
7. [시스템 콜 필터링](#시스템-콜-필터링)
8. [기타 보안 옵션](#기타-보안-옵션)
9. [systemd-analyze security](#systemd-analyze-security)
10. [실전 하드닝 예제](#실전-하드닝-예제)
11. [참고 자료](#참고-자료)

---

### 개요

`systemd.exec` 는 서비스(`.service`), 소켓(`.socket`), 마운트(`.mount`), 스왑(`.swap`) 등 **프로세스를 실제로 실행하는 unit** 의 실행 환경을 통제하는 옵션들을 정의합니다.

이 옵션들의 핵심 가치는 **서비스 하드닝(hardening)** 입니다. 별도의 SELinux/AppArmor 정책 없이도 systemd만으로 강력한 샌드박싱을 구성할 수 있습니다. 내부적으로는 Linux 커널의 capabilities, namespaces, seccomp, mount propagation 등을 조합해 동작합니다.

---

### 사용자와 그룹

```ini
[Service]
User=myapp
Group=myapp
SupplementaryGroups=video render
DynamicUser=yes
```

- `User=`, `Group=`: 서비스를 실행할 UID/GID. 미지정 시 root.
- `SupplementaryGroups=`: 추가 그룹 (디바이스 접근 등)
- `DynamicUser=yes`: **동적 사용자**. 서비스 실행 시 임시 UID 할당, 종료 시 회수. `/etc/passwd` 를 더럽히지 않음. 강력 권장.
- `UMask=0077`: 파일 생성 마스크

`DynamicUser=yes` 는 다음 설정을 자동으로 적용합니다:
- `RemoveIPC=yes`
- `ProtectSystem=strict`
- `ProtectHome=read-only`
- `NoNewPrivileges=yes`
- `RestrictSUIDSGID=yes`

---

### 작업 디렉터리와 루트

```ini
WorkingDirectory=/var/lib/myapp
RootDirectory=/var/empty
RootImage=/usr/share/myapp/myapp.raw
```

- `WorkingDirectory=`: chdir
- `RootDirectory=`: chroot. 서비스가 이 경로를 root(/)로 보게 됨
- `RootImage=`: 디스크 이미지(raw, GPT)를 루트로 사용
- `RootImageOptions=`: 이미지 마운트 옵션
- `RootHash=`: dm-verity 무결성 검증

#### State/Cache/Logs/Configuration 디렉터리

systemd가 자동으로 디렉터리를 만들고 권한을 설정해주는 옵션:

```ini
StateDirectory=myapp
CacheDirectory=myapp
LogsDirectory=myapp
ConfigurationDirectory=myapp
RuntimeDirectory=myapp
```

각각 `/var/lib/myapp`, `/var/cache/myapp`, `/var/log/myapp`, `/etc/myapp`, `/run/myapp` 디렉터리가 자동으로 만들어지고 `User=` 권한이 적용됩니다.

`RuntimeDirectoryPreserve=yes` 를 주면 서비스 재시작 사이에 `/run/myapp` 이 유지됩니다.

---

### Capabilities

Linux capabilities는 root의 권한을 세밀하게 쪼갠 것입니다. systemd는 서비스가 가질 수 있는 capability를 제한할 수 있습니다.

```ini
CapabilityBoundingSet=CAP_NET_BIND_SERVICE
AmbientCapabilities=CAP_NET_BIND_SERVICE
NoNewPrivileges=yes
```

- `CapabilityBoundingSet=`: 이 capability **이외에는 절대 가질 수 없음**. 빈 문자열을 할당하면 bounding set을 완전히 비울 수 있음
- `AmbientCapabilities=`: 비-root 프로세스에 부여할 capability
- `NoNewPrivileges=yes`: setuid 바이너리·파일 capability·SELinux transition으로도 권한 상승 불가. **거의 모든 서비스에 권장**

#### 자주 쓰는 capability

| capability | 용도 |
| --- | --- |
| `CAP_NET_BIND_SERVICE` | 1024 미만 포트 바인딩 |
| `CAP_NET_RAW` | raw 소켓 (ping, tcpdump) |
| `CAP_NET_ADMIN` | 네트워크 인터페이스 설정 |
| `CAP_SYS_ADMIN` | 광범위 (마운트, namespace 등) |
| `CAP_SYS_PTRACE` | strace, gdb 등 |
| `CAP_DAC_READ_SEARCH` | 모든 파일 읽기 권한 |
| `CAP_CHOWN` | 파일 소유권 변경 |

#### 모든 capability 제거 (가장 안전)

```ini
CapabilityBoundingSet=
AmbientCapabilities=
```

---

### 네임스페이스 격리

systemd는 Linux namespaces를 활용해 서비스를 격리할 수 있습니다.

#### Mount namespace

```ini
PrivateTmp=yes
PrivateDevices=yes
ProtectHome=yes
ProtectSystem=strict
```

- `PrivateTmp=yes`: 서비스 전용 `/tmp`, `/var/tmp` 제공. 다른 서비스와 격리
- `PrivateDevices=yes`: `/dev` 에 기본 디바이스(null, zero, random, ...)만 노출. 디스크/네트워크 디바이스 차단
- `ProtectHome=yes`: `/home`, `/root`, `/run/user` 를 빈 디렉터리로 마운트. `read-only` 또는 `tmpfs` 도 가능
- `ProtectSystem=`:
  - `true`: `/usr`, `/boot`, `/efi` 읽기 전용
  - `full`: 위 + `/etc` 읽기 전용
  - `strict`: 거의 모든 파일시스템 읽기 전용

#### Network namespace

```ini
PrivateNetwork=yes
```

서비스가 자체 네트워크 namespace를 가져 호스트 네트워크에 접근 불가. 루프백만 사용 가능. 외부 통신이 필요 없는 서비스에 매우 강력.

#### User namespace

```ini
PrivateUsers=yes
```

서비스가 자체 user namespace를 가지며 UID/GID 매핑이 격리됩니다.

#### IPC namespace

```ini
PrivateIPC=yes
RemoveIPC=yes
```

System V IPC와 POSIX 메시지 큐를 격리.

#### PID namespace

```ini
PrivatePIDs=yes
```

(systemd 256+) 서비스가 자체 PID namespace를 가짐.

---

### 파일시스템 보호

#### 읽기/쓰기 경로 명시

```ini
ProtectSystem=strict
ReadWritePaths=/var/lib/myapp /var/log/myapp
ReadOnlyPaths=/etc/myapp
InaccessiblePaths=/srv
```

`ProtectSystem=strict`으로 거의 모든 경로를 읽기 전용으로 잠근 뒤, `ReadWritePaths=`로 필요한 경로만 열어주는 패턴이 안전합니다.

#### 커널 보호

```ini
ProtectKernelTunables=yes
ProtectKernelModules=yes
ProtectKernelLogs=yes
ProtectControlGroups=yes
ProtectClock=yes
ProtectHostname=yes
ProtectProc=invisible
ProcSubset=pid
```

- `ProtectKernelTunables=yes`: `/proc/sys`, `/sys` 읽기 전용
- `ProtectKernelModules=yes`: 커널 모듈 로드/언로드 차단
- `ProtectKernelLogs=yes`: kmsg 접근 차단
- `ProtectControlGroups=yes`: cgroup 파일시스템 읽기 전용
- `ProtectProc=invisible`: 다른 프로세스 정보(`/proc/<pid>`) 숨김
- `ProcSubset=pid`: `/proc` 의 시스템 정보(`/proc/cpuinfo` 등) 숨기고 PID 항목만 노출

---

### 시스템 콜 필터링

seccomp 기반 syscall 허용 목록/차단 목록입니다.

```ini
SystemCallFilter=@system-service
SystemCallFilter=~@privileged @resources
SystemCallErrorNumber=EPERM
SystemCallArchitectures=native
```

- `SystemCallFilter=@system-service`: 화이트리스트 그룹 (대부분의 일반 서비스에 적합)
- `~@privileged @resources`: 이미 허용된 것 중에서 privileged/resource 그룹은 빼기
- `SystemCallErrorNumber=`: 차단된 syscall이 어떤 에러로 실패할지
- `SystemCallArchitectures=native`: 네이티브 아키텍처만 허용 (x86_32 호환 모드 차단)

#### 주요 syscall 그룹

| 그룹 | 설명 |
| --- | --- |
| `@system-service` | 일반 서비스에 필요한 syscall (좋은 시작점) |
| `@privileged` | mount, reboot 등 권한 필요 syscall |
| `@resources` | 우선순위 변경 등 |
| `@debug` | ptrace, perf 등 |
| `@module` | 커널 모듈 |
| `@mount` | mount, umount |
| `@network-io` | 소켓 연산 |
| `@raw-io` | iopl, ioperm |

---

### 기타 보안 옵션

#### Memory와 실행

```ini
MemoryDenyWriteExecute=yes
LockPersonality=yes
RestrictRealtime=yes
RestrictSUIDSGID=yes
```

- `MemoryDenyWriteExecute=yes`: W^X를 강제합니다. JIT 컴파일러를 사용하는 언어(Java, Node.js 등)에서는 비활성화가 필요합니다
- `LockPersonality=yes`: `personality()` syscall 차단 (32/64비트 전환 등)
- `RestrictRealtime=yes`: 실시간 스케줄링 차단
- `RestrictSUIDSGID=yes`: setuid/setgid 비트 설정 차단

#### 주소 패밀리 제한

```ini
RestrictAddressFamilies=AF_UNIX AF_INET AF_INET6
```

서비스가 사용할 수 있는 소켓 패밀리를 제한합니다. AF_NETLINK 등을 차단해 공격 표면을 줄일 수 있습니다.

#### Namespace 제한

```ini
RestrictNamespaces=yes
```

서비스가 새 namespace를 생성하는 것을 차단합니다. 컨테이너 런타임이 아닌 서비스에는 거의 항상 활성화하는 것이 좋습니다.

#### KeyringMode

```ini
KeyringMode=private
```

커널 키링 격리.

---

### systemd-analyze security

작성한 unit의 보안 점수를 자동으로 평가해주는 도구.

```bash
$ systemd-analyze security nginx.service
  NAME                                                 DESCRIPTION                                                       EXPOSURE
✗ RootDirectory=/RootImage=                            Service runs within the host's root directory                          0.1
  SupplementaryGroups=                                 Service has no supplementary groups
✗ PrivateDevices=                                      Service potentially has access to hardware devices                      0.2
✗ PrivateNetwork=                                      Service has access to the host's network                                0.5
...
→ Overall exposure level for nginx.service: 6.5 MEDIUM 🙂
```

0~10점 (낮을수록 안전). 옵션을 하나씩 켜면서 점수를 낮추는 것이 실전 하드닝의 좋은 출발점입니다.

---

### 실전 하드닝 예제

```ini
[Unit]
Description=Hardened App
After=network.target

[Service]
Type=exec
ExecStart=/usr/local/bin/myapp

# 사용자
DynamicUser=yes

# 디렉터리
StateDirectory=myapp
LogsDirectory=myapp
RuntimeDirectory=myapp

# 권한
NoNewPrivileges=yes
CapabilityBoundingSet=
AmbientCapabilities=

# 파일시스템
ProtectSystem=strict
ProtectHome=yes
PrivateTmp=yes
PrivateDevices=yes

# 커널
ProtectKernelTunables=yes
ProtectKernelModules=yes
ProtectKernelLogs=yes
ProtectControlGroups=yes
ProtectClock=yes
ProtectHostname=yes
ProtectProc=invisible
ProcSubset=pid

# 네임스페이스
PrivateNetwork=no
PrivateUsers=yes
RestrictNamespaces=yes
RestrictRealtime=yes
RestrictSUIDSGID=yes
LockPersonality=yes
MemoryDenyWriteExecute=yes
RemoveIPC=yes

# 주소 패밀리
RestrictAddressFamilies=AF_UNIX AF_INET AF_INET6

# Syscall
SystemCallFilter=@system-service
SystemCallFilter=~@privileged @resources
SystemCallArchitectures=native

[Install]
WantedBy=multi-user.target
```

이 unit은 `systemd-analyze security` 에서 1~2점대(매우 안전) 가 나옵니다.

---

### 참고 자료

- [man systemd.exec](https://www.freedesktop.org/software/systemd/man/systemd.exec.html)
- [systemd-analyze(1)](https://www.freedesktop.org/software/systemd/man/systemd-analyze.html)
- [Lennart's Hardening Guide](http://0pointer.de/blog/projects/security.html)

---

## 리소스 제어와 cgroups

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.resource-control.html , https://docs.kernel.org/admin-guide/cgroup-v2.html

---

### 목차

1. [cgroups란?](#cgroups란)
2. [cgroups v1 vs v2](#cgroups-v1-vs-v2)
3. [systemd와 cgroups](#systemd와-cgroups)
4. [Slice 계층](#slice-계층)
5. [CPU 제어](#cpu-제어)
6. [메모리 제어](#메모리-제어)
7. [IO 제어](#io-제어)
8. [태스크 수와 기타](#태스크-수와-기타)
9. [Delegate](#delegate)
10. [관찰과 디버깅](#관찰과-디버깅)
11. [참고 자료](#참고-자료)

---

### cgroups란?

**Control Groups(cgroups)** 는 Linux 커널의 기능으로, 프로세스 그룹의 리소스(CPU, 메모리, IO, 네트워크 등) 사용을 측정·제한·격리합니다. Docker, Kubernetes, systemd 등 모든 현대 컨테이너/init 시스템의 기반 기술입니다.

핵심 아이디어:
- 프로세스를 트리 구조의 그룹에 배치
- 각 그룹에 컨트롤러(controller)를 부착해 자원 정책 적용
- 자식 그룹은 부모 정책의 영향을 받음

---

### cgroups v1 vs v2

#### v1 (레거시)

- 컨트롤러별로 **독립된 계층** (CPU, 메모리, IO 각각 별도 트리)
- 같은 프로세스가 컨트롤러마다 다른 그룹에 속할 수 있음
- 일관성 부족, 복잡함

#### v2 (통합)

- 모든 컨트롤러가 **단일 계층** 공유
- 한 프로세스는 정확히 한 cgroup에 속함
- 더 단순하고 강력한 정책 표현 가능
- Linux 4.5에서 안정화, 5.x에서 성숙
- systemd는 가능하면 항상 v2를 선호

#### 현재 시스템 확인

```bash
$ stat -fc %T /sys/fs/cgroup/
cgroup2fs    # v2 단독 (권장)
tmpfs        # 하이브리드 또는 v1
```

부팅 시 `systemd.unified_cgroup_hierarchy=1` 커널 파라미터로 v2 강제 가능.

---

### systemd와 cgroups

systemd는 PID 1로서 cgroup 트리의 루트를 관리합니다. 모든 unit은 자동으로 cgroup에 배치됩니다.

```
/sys/fs/cgroup/
├── system.slice/
│   ├── nginx.service/
│   ├── postgresql.service/
│   └── ...
├── user.slice/
│   └── user-1000.slice/
│       └── user@1000.service/
│           └── app.slice/
└── machine.slice/
    └── ... (컨테이너)
```

각 서비스는 `<name>.service` 이름의 cgroup을 가지며, 해당 cgroup 안의 모든 프로세스를 systemd가 추적합니다. PID 파일 기반의 SysV init보다 훨씬 안정적인 프로세스 추적이 가능합니다.

---

### Slice 계층

`.slice` unit은 cgroup 트리의 중간 노드 역할을 합니다. 리소스 정책을 그룹화할 때 유용합니다.

#### 표준 slice

| Slice | 용도 |
| --- | --- |
| `-.slice` | 루트 |
| `system.slice` | 시스템 서비스 |
| `user.slice` | 사용자 세션 |
| `machine.slice` | 컨테이너/VM |

#### 사용자 정의 slice

```ini
# /etc/systemd/system/databases.slice
[Unit]
Description=Database Services Slice

[Slice]
CPUWeight=200
MemoryMax=8G
```

서비스에서 사용:

```ini
[Service]
Slice=databases.slice
```

이렇게 하면 모든 DB 서비스가 한 slice에 묶여 합산 메모리 8GB, CPU 가중치 200으로 제한됩니다.

---

### CPU 제어

#### CPUWeight (상대 비중)

기본값 100. 다른 unit과의 **상대적인 CPU 시간 분배** 비율.

```ini
CPUWeight=200      # 일반보다 2배
StartupCPUWeight=500   # 부팅 시 임시 가중치
```

- 범위: 1~10000
- v1의 `CPUShares=` 를 대체

#### CPUQuota (절대 한도)

특정 CPU 사용률을 넘지 못하게 함.

```ini
CPUQuota=50%      # CPU 한 코어의 50%
CPUQuota=200%     # 코어 2개분
```

내부적으로 `cpu.max` (period/quota)를 설정. 초과 시 throttling 발생.

#### CPUAffinity (코어 핀)

```ini
CPUAffinity=0-3 8
```

지정한 CPU 코어에서만 실행되도록 제한.

#### AllowedCPUs / AllowedMemoryNodes

NUMA 시스템에서 cgroup이 사용할 CPU/메모리 노드.

```ini
AllowedCPUs=0-15
AllowedMemoryNodes=0
```

---

### 메모리 제어

cgroups v2는 4단계의 메모리 한도를 지원합니다.

#### MemoryMin (보호)

```ini
MemoryMin=512M
```

이 unit이 사용하는 메모리는 reclaim에서 **절대로 회수되지 않음**. 다른 곳에서 메모리 압박이 와도 보호됨.

#### MemoryLow (소프트 보호)

```ini
MemoryLow=1G
```

가능한 한 reclaim에서 보호. `MemoryMin` 보다 약함.

#### MemoryHigh (소프트 한도)

```ini
MemoryHigh=2G
```

이 한도를 넘으면 active reclaim이 시작되고 프로세스가 throttling됨. **OOM은 발생하지 않음**.

#### MemoryMax (하드 한도)

```ini
MemoryMax=4G
```

절대 한도. 넘으면 cgroup OOM killer가 프로세스를 죽임. v1의 `MemoryLimit=` 대체.

#### MemorySwapMax

```ini
MemorySwapMax=0
```

swap 사용 한도. 0으로 두면 swap 사용 금지.

#### 추천 패턴

```ini
[Service]
MemoryAccounting=yes      # 사용량 추적 (대부분 자동)
MemoryHigh=3G             # 부드러운 경고
MemoryMax=4G              # 절대 한도
```

---

### IO 제어

블록 디바이스 IO 대역폭 제어. cgroups v2에서 큰 폭으로 개선되었습니다.

#### IOWeight (상대 비중)

```ini
IOWeight=200
StartupIOWeight=500
```

기본 100, 범위 1~10000.

#### IODevice 제어

```ini
IOReadBandwidthMax=/dev/sda 50M
IOWriteBandwidthMax=/dev/sda 30M
IOReadIOPSMax=/dev/sda 1000
IOWriteIOPSMax=/dev/sda 500
```

특정 디바이스에 대한 절대 한도.

#### IODeviceWeight

```ini
IODeviceWeight=/dev/sda 500
```

특정 디바이스에 대한 가중치.

> 참고: IO 컨트롤러는 파일시스템 종류와 IO 스케줄러에 따라 동작이 달라집니다. ext4/xfs + bfq/mq-deadline 조합이 가장 잘 동작합니다.

---

### 태스크 수와 기타

#### TasksMax

```ini
TasksMax=1024
```

cgroup이 가질 수 있는 최대 태스크(PID) 수. fork 폭탄 방어에 효과적.

#### LimitNOFILE 등 (rlimit)

cgroup 컨트롤러는 아니지만 비슷한 역할.

```ini
LimitNOFILE=65536
LimitNPROC=2048
LimitCORE=infinity
LimitMEMLOCK=64K
```

각각 `setrlimit(2)` 의 RLIMIT_NOFILE, RLIMIT_NPROC, RLIMIT_CORE, RLIMIT_MEMLOCK에 매핑.

#### OOMScoreAdjust

```ini
OOMScoreAdjust=-500
```

`/proc/<pid>/oom_score_adj` 에 해당. -1000 ~ 1000. 낮을수록 OOM killer가 죽일 가능성 낮음.

#### OOMPolicy

```ini
OOMPolicy=stop
```

- `continue` (기본): 프로세스만 죽이고 unit은 계속
- `stop`: cgroup 안 프로세스가 OOM으로 죽으면 unit 전체 중지
- `kill`: cgroup 전체를 죽임

---

### Delegate

```ini
[Service]
Delegate=cpu memory io
```

해당 unit이 자식 cgroup을 직접 관리할 수 있도록 권한을 위임합니다. 컨테이너 런타임(systemd-nspawn, Docker 등)에서 사용하며, 일반 서비스에는 필요하지 않습니다.

---

### 관찰과 디버깅

#### 실시간 리소스 사용량

```bash
systemd-cgtop
```

cgroup별 CPU/메모리/IO 사용량을 `top` 처럼 실시간으로 표시합니다.

#### 특정 unit의 cgroup

```bash
$ systemctl status nginx.service
...
   CGroup: /system.slice/nginx.service
           ├─1234 nginx: master process /usr/sbin/nginx
           └─1235 nginx: worker process
```

#### cgroup 파일 직접 보기

```bash
cd /sys/fs/cgroup/system.slice/nginx.service
cat memory.current        # 현재 메모리 사용량
cat memory.max            # 메모리 한도
cat cpu.stat              # CPU 통계 (usage_usec, throttled_usec)
cat io.stat               # IO 통계
cat cgroup.procs          # 이 cgroup의 PID 목록
```

#### 임시 unit으로 실험

```bash
sudo systemd-run --unit=test-mem --scope -p MemoryMax=100M stress --vm 1 --vm-bytes 200M
```

`--scope` 는 외부 명령을 새 cgroup으로 감싸서 실행합니다. 즉석에서 리소스 한도를 테스트할 때 유용합니다.

#### 누가 OOM에 죽었는지

```bash
journalctl -k | grep -i oom
journalctl -u nginx.service | grep -i oom
```

---

### 참고 자료

- [man systemd.resource-control](https://www.freedesktop.org/software/systemd/man/systemd.resource-control.html)
- [man systemd-cgtop](https://www.freedesktop.org/software/systemd/man/systemd-cgtop.html)
- [Kernel cgroup-v2 docs](https://docs.kernel.org/admin-guide/cgroup-v2.html)
- [Chris Down: Linux Memory Management at Scale](https://chrisdown.name/talks.html)
