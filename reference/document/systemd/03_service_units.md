# Service Unit

> 이 문서는 `man systemd.service` 의 내용을 한국어로 정리한 것입니다.
> 원본: https://www.freedesktop.org/software/systemd/man/systemd.service.html

---

## 목차

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

## Service란?

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

## Type=의 종류

`Type=` 은 systemd가 서비스의 시작 완료 시점을 어떻게 판단할지 결정합니다. 의존하는 다른 unit이 언제 시작되어야 하는지를 정하는 핵심 옵션입니다.

### simple (기본값, 권장하지 않음)

`ExecStart` 의 프로세스가 fork하지 않고 포그라운드로 실행됩니다. systemd는 fork() 직후 즉시 unit을 active로 간주합니다.

문제: 프로세스가 실제로 준비됐는지(포트 바인딩 완료 등) 확인할 수 없어 의존하는 unit이 너무 빨리 시작될 수 있습니다.

### exec (권장)

`simple` 과 비슷하지만, systemd는 **`execve()` 가 성공할 때까지** 대기합니다. 즉 바이너리가 실제로 로드된 시점을 확인합니다. 대부분의 경우 `simple` 대신 이 옵션을 써야 합니다.

### forking

전통적인 데몬 패턴 — 프로세스가 fork하고 부모는 종료, 자식이 백그라운드에서 실행됩니다. `PIDFile=` 을 함께 지정해 systemd가 자식 PID를 추적할 수 있게 해야 합니다.

```ini
[Service]
Type=forking
ExecStart=/usr/sbin/old-style-daemon
PIDFile=/run/old-style-daemon.pid
```

레거시 데몬에만 사용. 새 서비스는 simple/exec/notify를 권장합니다.

### oneshot

한 번 실행하고 끝나는 작업에 사용 (예: 부팅 시 설정 적용). systemd는 프로세스가 **종료될 때까지** 대기합니다.

```ini
[Service]
Type=oneshot
ExecStart=/usr/local/bin/setup-once.sh
RemainAfterExit=yes
```

`RemainAfterExit=yes` 를 주면 프로세스 종료 후에도 unit을 active로 표시합니다.

### notify

서비스가 `sd_notify(3)` API를 통해 시작 완료를 알립니다. 가장 정확한 방식이며, systemd 자체나 nginx, openvpn 등이 지원합니다.

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

### notify-reload

`Type=notify` 와 같지만 `systemctl reload` 시 SIGHUP 후 `RELOADING=1` / `READY=1` 알림을 기다립니다 (systemd 253+).

### dbus

`BusName=` 으로 지정한 D-Bus 이름을 가져올 때 unit이 active가 됩니다.

### idle

다른 작업이 끝날 때까지 시작을 지연시킵니다. 부팅 메시지 충돌 방지용 — 거의 쓰이지 않습니다.

---

## Exec 지시자

### ExecStart

서비스를 시작하는 명령. **절대 경로** 여야 합니다. shell 메타문자(`>`, `|`, `*`)는 직접 처리되지 않으므로 셸이 필요하면 `/bin/sh -c "..."` 로 감싸야 합니다.

```ini
ExecStart=/usr/local/bin/server --port 8080 --config /etc/myapp.conf
```

`Type=oneshot` 일 때만 여러 번 지정 가능.

### ExecStartPre / ExecStartPost

`ExecStart` 직전/직후에 실행됩니다. 권한 설정, 디렉터리 생성, 헬스체크 등에 사용.

```ini
ExecStartPre=/bin/mkdir -p /var/lib/myapp
ExecStartPre=/bin/chown myapp:myapp /var/lib/myapp
ExecStart=/usr/local/bin/myapp
ExecStartPost=/usr/local/bin/notify-startup.sh
```

### ExecReload

`systemctl reload` 시 실행할 명령. 보통 SIGHUP을 보냅니다.

```ini
ExecReload=/bin/kill -HUP $MAINPID
```

`$MAINPID` 는 systemd가 자동 제공하는 변수.

### ExecStop / ExecStopPost

서비스를 깔끔하게 종료할 명령. 미지정 시 systemd는 SIGTERM → SIGKILL 순으로 보냅니다.

```ini
ExecStop=/usr/local/bin/myapp-shutdown
ExecStopPost=/bin/rm -f /run/myapp.lock
```

### Exec 접두사

명령 앞에 특수 문자를 붙여 동작을 변경할 수 있습니다.

| 접두사 | 의미 |
| --- | --- |
| `-` | 명령 실패를 무시 |
| `@` | argv[0] 별도 지정 |
| `:` | 환경 변수 치환 비활성화 |
| `+` | 권한 제한 무시 (root로 실행) |
| `!` | User=/Group= 적용하지만 그 외 권한 옵션 무시 |
| `!!` | 위와 같지만 ambient capability를 사용 |

```ini
ExecStartPre=-/bin/rm -f /tmp/old.sock
```

---

## Restart 정책

서비스가 종료됐을 때 자동 재시작 여부.

```ini
[Service]
Restart=on-failure
RestartSec=5
StartLimitIntervalSec=60
StartLimitBurst=3
```

### Restart= 값

| 값 | 동작 |
| --- | --- |
| `no` | 재시작 안 함 (기본값) |
| `on-success` | exit 0일 때만 재시작 |
| `on-failure` | exit≠0, 시그널, 타임아웃, 코어덤프 시 재시작 |
| `on-abnormal` | 시그널/타임아웃/코어덤프 시 재시작 (exit code 무시) |
| `on-watchdog` | 워치독 타임아웃 시에만 재시작 |
| `on-abort` | 의도치 않은 시그널 (SIGABRT 등) 시 |
| `always` | 항상 재시작 (오류든 정상이든) |

### 재시작 제어

- `RestartSec=5`: 재시작 전 대기 시간 (기본 100ms)
- `StartLimitIntervalSec=60` + `StartLimitBurst=3`: 60초 안에 3번 이상 시작 실패하면 더 시도하지 않음
- `StartLimitAction=`: 한도 초과 시 `reboot`, `poweroff` 등 가능

---

## 타임아웃

- `TimeoutStartSec=90s`: 시작이 이 시간 안에 완료되지 않으면 실패 처리. 기본 90초
- `TimeoutStopSec=90s`: SIGTERM 후 이 시간이 지나면 SIGKILL
- `TimeoutAbortSec=`: WatchdogSec 트리거 시 abort 시간
- `TimeoutSec=`: 위 두 개를 동시에 설정
- `RuntimeMaxSec=1h`: 서비스 최대 실행 시간 (이후 자동 종료)

`infinity` 를 주면 타임아웃이 비활성화됩니다.

### 워치독

```ini
[Service]
Type=notify
WatchdogSec=30s
```

서비스가 30초마다 `sd_notify(0, "WATCHDOG=1")` 을 호출하지 않으면 systemd가 서비스를 재시작합니다. 데몬이 무한 루프에 빠진 경우를 잡아내는 데 유용.

---

## 환경 변수

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

shell 형식이 아니므로 `export`, 변수 치환, 따옴표 처리 등은 제한적입니다.

---

## PID 파일과 알림

`Type=forking` 일 때만 `PIDFile=` 이 의미 있습니다. systemd는 이 파일에서 PID를 읽어 메인 프로세스를 추적합니다.

```ini
PIDFile=/run/nginx.pid
```

PID 파일은 보안상 **읽기 전용 디렉터리** (`/run`)에 두는 것이 좋습니다.

`NotifyAccess=` 는 `Type=notify` 일 때 어느 프로세스의 알림을 수락할지 정합니다:
- `none` (기본): 알림 비활성화
- `main`: 메인 프로세스만
- `exec`: ExecStart로 실행된 프로세스 모두
- `all`: 자식 프로세스 모두

---

## 실전 예제

### 1. 단순 웹 서비스

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

### 2. 환경 파일과 워치독을 쓰는 서비스

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

### 3. oneshot 초기화

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

## 참고 자료

- [man systemd.service](https://www.freedesktop.org/software/systemd/man/systemd.service.html)
- [man sd_notify](https://www.freedesktop.org/software/systemd/man/sd_notify.html)
- [systemd by example](https://systemd-by-example.com/)
