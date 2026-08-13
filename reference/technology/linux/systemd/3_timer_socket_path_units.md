# Timer, Socket, Path Unit

## Timer Unit

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.timer.html

---

### 목차

1. [Timer란?](#timer란)
2. [Timer와 cron의 차이](#timer와-cron의-차이)
3. [Monotonic 타이머](#monotonic-타이머)
4. [Realtime 타이머 (OnCalendar)](#realtime-타이머-oncalendar)
5. [Timer 옵션](#timer-옵션)
6. [Persistent와 Accuracy](#persistent와-accuracy)
7. [실전 예제](#실전-예제)
8. [관리 명령](#관리-명령)
9. [참고 자료](#참고-자료)

---

### Timer란?

`.timer` unit은 다른 unit(보통 service)을 일정 시점·주기에 활성화하는 unit → cron 작업을 systemd 방식으로 표현하는 도구.

기본 패턴:
- `backup.timer` 가 `backup.service` 를 실행
- 두 파일은 보통 같은 이름의 짝(pair)
- `Unit=` 으로 다른 이름의 service를 지정 가능

```ini
# /etc/systemd/system/backup.timer
[Unit]
Description=Daily Backup Timer

[Timer]
OnCalendar=daily
Persistent=true
Unit=backup.service

[Install]
WantedBy=timers.target
```

```ini
# /etc/systemd/system/backup.service
[Unit]
Description=Daily Backup Job

[Service]
Type=oneshot
ExecStart=/usr/local/bin/backup.sh
```

`backup.timer` 만 enable하면 됨 → `backup.service` 는 enable 불필요.

---

### Timer와 cron의 차이

- 표현 형식: cron은 crontab 5필드 · systemd timer는 OnCalendar 자연어
- 로깅: cron은 별도 메일/로그 · systemd timer는 journald 통합
- 의존성: cron은 없음 · systemd timer는 unit 의존성 활용
- 실패 처리: cron은 사용자 책임 · systemd timer는 Restart, OnFailure
- 부팅 후 누락 작업: cron은 없음 · systemd timer는 Persistent 옵션
- 리소스 제어: cron은 없음 · systemd timer는 cgroup으로 제한 가능
- 보안 격리: cron은 없음 · systemd timer는 systemd.exec 옵션 전부 사용 가능
- 사용자 단위: cron은 `crontab -e` · systemd timer는 `systemctl --user`

---

### Monotonic 타이머

시스템 부팅 시점이나 unit 활성화 시점 등을 기준으로 동작.

```ini
[Timer]
OnBootSec=15min
OnUnitActiveSec=1h
```

#### 종류

- `OnActiveSec=`: timer가 active가 된 시점부터
- `OnBootSec=`: 시스템 부팅 시점부터
- `OnStartupSec=`: systemd 시작 시점부터
- `OnUnitActiveSec=`: 연관 unit이 마지막으로 active가 된 시점부터
- `OnUnitInactiveSec=`: 연관 unit이 마지막으로 inactive가 된 시점부터

#### 시간 표기

- `30s`, `5min`, `2h`, `1d`, `1week`, `1month`, `1year`
- 조합 가능: `1h 30min`

여러 개를 지정하면 각 조건이 OR로 결합.

```ini
[Timer]
OnBootSec=15min          # 부팅 후 15분
OnUnitActiveSec=1h        # 그 다음부터 1시간마다
```

---

### Realtime 타이머 (OnCalendar)

벽시계 시간 기준의 캘린더 표현식 사용.

```ini
OnCalendar=Mon..Fri 09:00
OnCalendar=*-*-01 03:00:00
OnCalendar=hourly
```

#### 형식

```
DayOfWeek Year-Month-Day Hour:Minute:Second
```

- `*`: 모든 값
- `..`: 범위 (`Mon..Fri`)
- `,`: 목록 (`Mon,Wed,Fri`)
- `/`: 간격 (`*/5` = 5의 배수)

#### 자주 쓰는 단축어

- `minutely`: 매분
- `hourly`: 매 시 정각 (`*-*-* *:00:00`)
- `daily`: 매일 자정
- `weekly`: 매주 월요일 자정
- `monthly`: 매월 1일 자정
- `yearly` / `annually`: 매년 1월 1일
- `quarterly`: 분기마다
- `semiannually`: 6개월마다

#### 예시

```
*-*-* *:00:00              매시간 정각
*-*-* 02:30:00             매일 02:30
Mon *-*-* 03:00:00         월요일 03:00
*-*-01 00:00:00            매월 1일
*-01,07-01 00:00:00        1월 1일과 7월 1일
*-*-* *:00/15:00           0,15,30,45분마다 (15분 간격)
2025-12-25 09:00:00        2025년 12월 25일 09:00 (한 번)
```

#### 검증

```bash
$ systemd-analyze calendar "Mon..Fri 09:00"
  Original form: Mon..Fri 09:00
Normalized form: Mon..Fri *-*-* 09:00:00
    Next elapse: Mon 2026-05-12 09:00:00 KST
       From now: 4 days left
```

---

### Timer 옵션

#### Persistent

```ini
Persistent=true
```

`OnCalendar` 와 함께 사용 → 시스템이 꺼져 있어 타이머를 놓친 경우, 부팅 후 즉시 한 번 실행 (cron의 `anacron` 동작과 유사).

`/var/lib/systemd/timers/` 에 마지막 실행 시각 저장.

#### Accuracy

```ini
AccuracySec=1min
```

타이머 정확도 지정 → 기본값 1분. 값을 크게 설정하면(예: `1h`) 여러 타이머를 같은 시점에 묶어 발화시켜 절전 효과 있음 → 데스크탑·IoT 환경에 유용.

매우 정확한 트리거가 필요한 경우:
```ini
AccuracySec=1us
```

#### RandomizedDelaySec

```ini
RandomizedDelaySec=5min
```

발화 시점에 0부터 지정 시간 사이의 무작위 지연 추가 → 여러 머신에서 같은 타이머를 실행할 때 부하 집중 방지 용도.

#### FixedRandomDelay

```ini
FixedRandomDelay=true
```

무작위 지연을 호스트별로 고정된 값으로 적용(호스트 ID와 unit 이름 기반) → 동일한 머신은 항상 같은 지연값 사용.

#### WakeSystem

```ini
WakeSystem=true
```

타이머 발화 시 슬립 상태의 시스템을 깨움(RTC 사용) → 노트북 백업 같은 시나리오에 유용.

#### OnClockChange / OnTimezoneChange

```ini
OnClockChange=true
OnTimezoneChange=true
```

시스템 시간이 점프하거나 타임존이 변경되면 즉시 발화.

#### RemainAfterElapse

```ini
RemainAfterElapse=no
```

마지막 발화 후 타이머를 즉시 정리할지 여부 지정 → 대부분의 경우 기본값 `yes`가 적절.

---

### Persistent와 Accuracy

타이머를 다룰 때 가장 흔한 실수 두 가지.

#### 누락 작업이 처리되지 않음

cron에서 systemd timer로 전환할 때 가장 자주 겪는 문제 → 다음 옵션 추가 필요:

```ini
[Timer]
OnCalendar=daily
Persistent=true
```

`Persistent=true` 가 없으면 시스템이 꺼져 있던 동안의 작업은 실행 안 됨.

#### 여러 머신이 동시에 실행

```ini
[Timer]
OnCalendar=hourly
RandomizedDelaySec=10min
```

이렇게 하면 매 시 정각이 아니라 정각~정각+10분 사이에 분산되어 실행됨.

---

### 실전 예제

#### 1. 매일 새벽 3시 백업

```ini
# backup.timer
[Unit]
Description=Daily Database Backup

[Timer]
OnCalendar=*-*-* 03:00:00
Persistent=true
RandomizedDelaySec=15min

[Install]
WantedBy=timers.target
```

#### 2. 부팅 5분 후 실행, 그 후 1시간마다

```ini
# health-check.timer
[Unit]
Description=Periodic Health Check

[Timer]
OnBootSec=5min
OnUnitActiveSec=1h
Unit=health-check.service

[Install]
WantedBy=timers.target
```

#### 3. 평일 업무시간만 실행

```ini
[Timer]
OnCalendar=Mon..Fri 09:00,12:00,15:00
Persistent=false
```

#### 4. 사용자 timer

`~/.config/systemd/user/sync.timer` 에 작성 후 다음 실행:

```bash
systemctl --user enable --now sync.timer
loginctl enable-linger $USER   # 사용자 로그아웃 후에도 동작
```

`enable-linger` 가 없으면 사용자가 로그인해 있는 동안만 timer 동작.

---

### 관리 명령

#### 모든 타이머 보기

```bash
systemctl list-timers --all
```

```
NEXT                        LEFT          LAST                        PASSED       UNIT
Sat 2026-05-09 03:00:00 KST 14h left      Fri 2026-05-08 03:00:00 KST 9h ago       backup.timer
Fri 2026-05-08 13:30:15 KST 30min left    Fri 2026-05-08 12:30:15 KST 29min ago    health-check.timer
```

#### 캘린더 식 검증

```bash
systemd-analyze calendar "Mon..Fri 09:00"
systemd-analyze calendar --iterations=5 "*-*-* 03:00:00"
```

#### 타이머 즉시 실행 (테스트)

```bash
systemctl start backup.service    # 타이머가 아닌 service를 직접 시작
```

타이머 자체를 시작하면 다음 발화 시점만 계산할 뿐 즉시 실행되지 않음 → 즉시 실행이 필요하면 service를 직접 시작.

#### 마지막 실행 결과

```bash
journalctl -u backup.service -n 50
systemctl status backup.timer
```

---

### 참고 자료

- [man systemd.timer](https://www.freedesktop.org/software/systemd/man/systemd.timer.html)
- [man systemd.time](https://www.freedesktop.org/software/systemd/man/systemd.time.html) — 시간/캘린더 표기 레퍼런스
- [systemd-analyze calendar](https://www.freedesktop.org/software/systemd/man/systemd-analyze.html)

---

## Socket Unit과 소켓 활성화

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.socket.html

---

### 목차

1. [소켓 활성화란?](#소켓-활성화란)
2. [장점](#장점)
3. [기본 구조](#기본-구조)
4. [Listen 옵션](#listen-옵션)
5. [Accept= 모드](#accept-모드)
6. [소켓 옵션](#소켓-옵션)
7. [sd_listen_fds 프로토콜](#sd_listen_fds-프로토콜)
8. [실전 예제](#실전-예제)
9. [참고 자료](#참고-자료)

---

### 소켓 활성화란?

전통적인 `inetd`의 systemd 버전 → systemd가 포트(또는 유닉스 소켓)를 미리 열어 두고, 첫 연결이 들어오면 서비스를 시작해 해당 소켓을 넘겨줌.

```
[부팅]
  ├─ ssh.socket → 22번 포트 LISTEN (서비스는 아직 안 떠있음)
  ├─ ...
  └─ multi-user.target 도달

[첫 SSH 접속]
  ├─ 커널이 패킷 수신
  ├─ systemd가 ssh.service 시작
  ├─ ssh.service가 22번 포트 fd를 상속
  └─ ssh.service가 클라이언트 응답
```

---

### 장점

#### 1. 부팅 가속

서비스 자체를 시작하지 않고 소켓만 열어두면 되므로 부팅이 빠름 → systemd가 모든 서비스의 소켓을 동시에 열어두면, 의존 관계가 있는 서비스들도 병렬로 기동 가능.

#### 2. 의존성 단순화

A 서비스가 B 서비스에 연결하는 경우 가정. 일반적으로는 A가 B의 시작을 기다려야 함. 그러나 B의 소켓만 열려 있으면 A는 즉시 연결을 시도할 수 있고, 커널 버퍼에 쌓인 데이터는 B가 시작된 뒤 처리 → 순서 의존성이 사라짐.

#### 3. 서비스 재시작에도 연결 유지

서비스를 재시작해도 소켓은 systemd가 들고 있으므로 클라이언트 연결이 끊기지 않음.

#### 4. 권한 분리

systemd가 소켓을 열고 권한 있는 fd를 비특권 서비스에 넘겨주면, 서비스 자체는 root 권한 없이도 동작(1024 미만 포트 포함).

#### 5. 온디맨드 시작

거의 사용되지 않는 서비스는 평소에 메모리를 점유하지 않다가, 필요할 때만 시작됨.

---

### 기본 구조

소켓 유닛과 서비스 유닛은 같은 이름을 가져야 자동으로 연결됨.

```ini
# /etc/systemd/system/echo.socket
[Unit]
Description=Echo Socket

[Socket]
ListenStream=12345
Accept=no

[Install]
WantedBy=sockets.target
```

```ini
# /etc/systemd/system/echo.service
[Unit]
Description=Echo Service
Requires=echo.socket

[Service]
ExecStart=/usr/local/bin/echo-server
StandardInput=socket
```

`Service=` 로 다른 이름의 서비스를 지정 가능.

---

### Listen 옵션

#### ListenStream — TCP 또는 Unix stream

```ini
ListenStream=8080                # IPv6/IPv4 dual, 모든 인터페이스
ListenStream=127.0.0.1:8080      # IPv4 localhost
ListenStream=[::1]:8080          # IPv6 localhost
ListenStream=/run/myapp.sock     # Unix domain socket
ListenStream=@abstract           # abstract namespace (Linux 전용)
```

여러 개 지정 가능 → 각각 별도 fd로 서비스에 전달됨.

#### ListenDatagram — UDP 또는 Unix dgram

```ini
ListenDatagram=53
ListenDatagram=/run/log.sock
```

#### ListenSequentialPacket — Unix SEQPACKET

`SOCK_SEQPACKET` 형 → 메시지 경계를 유지하는 신뢰성 있는 양방향 소켓.

#### ListenFIFO — 명명된 파이프

```ini
ListenFIFO=/run/myapp.fifo
```

#### ListenSpecial — 디바이스/특수 파일

```ini
ListenSpecial=/dev/input/event0
```

#### ListenNetlink — Netlink 소켓

```ini
ListenNetlink=audit 1
```

#### ListenUSBFunction — USB Gadget

USB Gadget 모드의 functionfs 인스턴스.

---

### Accept= 모드

#### Accept=no (기본, 권장)

소켓 fd 자체를 서비스에 넘김 → 서비스는 단일 인스턴스로 실행되며, 직접 `accept(2)`를 호출해 연결 처리 → nginx·sshd처럼 자체 멀티플렉싱이 가능한 데몬에 적합.

#### Accept=yes

연결마다 서비스를 새 인스턴스로 fork → 서비스는 stdin/stdout이 클라이언트와 연결된 단순한 프로그램이면 충분 → 옛 `inetd` 스타일로, 간단한 echo 서버나 ftp 같은 용도에 적합.

```ini
[Socket]
ListenStream=2323
Accept=yes
```

이때 서비스는 템플릿이어야 함:

```ini
# echo@.service
[Service]
ExecStart=/usr/local/bin/echo
StandardInput=socket
```

---

### 소켓 옵션

#### 권한

```ini
SocketUser=nginx
SocketGroup=nginx
SocketMode=0660
```

Unix 소켓의 소유자 및 권한 설정.

#### Backlog

```ini
Backlog=128
```

`listen(2)` backlog 큐 크기 설정.

#### KeepAlive

```ini
KeepAlive=yes
KeepAliveTimeSec=300
KeepAliveIntervalSec=75
KeepAliveProbes=9
```

TCP keepalive 설정.

#### NoDelay (TCP_NODELAY)

```ini
NoDelay=yes
```

Nagle 알고리즘 비활성화 → 작은 패킷의 전송 지연 감소.

#### FreeBind

```ini
FreeBind=yes
```

존재하지 않거나 아직 구성되지 않은 IP 주소에도 바인딩 허용 → floating IP 환경에 유용.

#### Transparent

```ini
Transparent=yes
```

`IP_TRANSPARENT` → 투명 프록시 구현에 사용.

#### ReusePort

```ini
ReusePort=yes
```

`SO_REUSEPORT` → 여러 프로세스가 동일한 포트에 바인딩 가능(커널 수준 로드 밸런싱).

#### MaxConnections

```ini
MaxConnections=100
MaxConnectionsPerSource=10
```

`Accept=yes` 모드에서 동시 연결 수 제한.

#### TriggerLimit

```ini
TriggerLimitIntervalSec=2s
TriggerLimitBurst=200
```

소켓 활성화 트리거 빈도 제한(DoS 방지).

---

### sd_listen_fds 프로토콜

소켓 활성화를 지원하는 서비스는 fd를 다음과 같이 전달받음.

systemd는 다음 환경 변수를 설정한 뒤 서비스를 실행:
- `LISTEN_FDS=N`: 전달된 fd 개수
- `LISTEN_PID=<pid>`: 현재 PID(다른 프로세스에서 받지 않도록 검증용)
- `LISTEN_FDNAMES=name1:name2`: fd 이름(선택적)

fd는 `3`부터 시작하는 정수로 전달(3, 4, 5, ...).

#### libsystemd C API

```c
#include <systemd/sd-daemon.h>

int n = sd_listen_fds(0);
for (int fd = SD_LISTEN_FDS_START; fd < SD_LISTEN_FDS_START + n; fd++) {
    // 이 fd에서 accept() 또는 recvmsg() 등 수행
}
```

#### Go

```go
listeners, err := activation.Listeners()  // github.com/coreos/go-systemd/activation
```

#### 셸/Python

환경 변수를 직접 읽고 fd 3부터 처리.

#### FileDescriptorName

```ini
[Socket]
ListenStream=80
ListenStream=443
FileDescriptorName=http
FileDescriptorName=https
```

여러 소켓을 이름으로 구분할 때 사용.

---

### 실전 예제

#### 1. SSH 소켓 활성화

```ini
# ssh.socket
[Unit]
Description=OpenSSH Server Socket
Conflicts=ssh.service
Before=ssh.service

[Socket]
ListenStream=22
Accept=no

[Install]
WantedBy=sockets.target
```

#### 2. nginx 권한 분리

systemd가 80/443 소켓을 열고 nginx에 넘기면, nginx는 root 권한 없이 실행됨.

```ini
# nginx.socket
[Socket]
ListenStream=80
ListenStream=443
NoDelay=yes
```

#### 3. 컨테이너 헬스체크용 Unix 소켓

```ini
# myapp.socket
[Unit]
Description=MyApp Control Socket

[Socket]
ListenStream=/run/myapp/control.sock
SocketUser=myapp
SocketGroup=adm
SocketMode=0660
RemoveOnStop=yes

[Install]
WantedBy=sockets.target
```

#### 4. inetd 스타일 echo

```ini
# echo.socket
[Socket]
ListenStream=2323
Accept=yes
```

```ini
# echo@.service
[Service]
ExecStart=/usr/bin/cat
StandardInput=socket
StandardOutput=socket
```

---

### 참고 자료

- [man systemd.socket](https://www.freedesktop.org/software/systemd/man/systemd.socket.html)
- [man sd_listen_fds](https://www.freedesktop.org/software/systemd/man/sd_listen_fds.html)
- [Lennart: Socket Activation](http://0pointer.de/blog/projects/socket-activation.html)

---

## Path Unit

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.path.html

---

### 목차

1. [Path란?](#path란)
2. [감시 옵션](#감시-옵션)
3. [Path와 Service의 짝](#path와-service의-짝)
4. [동작 방식](#동작-방식)
5. [실전 예제](#실전-예제)
6. [주의사항](#주의사항)
7. [참고 자료](#참고-자료)

---

### Path란?

`.path` unit은 파일이나 디렉터리의 변경을 감시하다가 특정 조건이 만족되면 다른 unit(보통 service)을 활성화 → 내부적으로 `inotify` 사용.

용도:
- 디렉터리에 새 파일이 들어오면 처리 (incoming 큐)
- 설정 파일이 변경되면 데몬에 알림
- lock 파일이 사라지면 후속 작업 실행

```ini
# /etc/systemd/system/incoming.path
[Unit]
Description=Watch /var/incoming for new files

[Path]
PathChanged=/var/incoming
Unit=incoming.service

[Install]
WantedBy=multi-user.target
```

---

### 감시 옵션

#### PathExists

```ini
PathExists=/var/lib/myapp/trigger
```

경로가 존재하면 연관 unit 활성화 → 파일이 사라지면 unit도 비활성화.

#### PathExistsGlob

```ini
PathExistsGlob=/var/spool/jobs/*.job
```

glob 패턴 사용 → 패턴에 매칭하는 파일이 하나라도 있으면 활성화됨.

#### PathChanged

```ini
PathChanged=/etc/myapp.conf
```

파일이 닫힐 때(write 후 close) 트리거됨 → 가장 자주 사용하는 옵션이며, 디렉터리를 지정하면 그 안의 파일 변경을 감지.

#### PathModified

```ini
PathModified=/var/log/access.log
```

`PathChanged`와 비슷하지만 close 없이 write 시점에도 트리거됨 → 더 민감하지만 같은 변경에 여러 번 발화 가능.

#### DirectoryNotEmpty

```ini
DirectoryNotEmpty=/var/incoming
```

디렉터리에 파일이 하나라도 있으면 트리거됨 → 처리 큐 패턴에 적합.

모든 옵션은 절대 경로여야 하며, 여러 번 지정해 여러 경로 감시 가능.

---

### Path와 Service의 짝

기본적으로 `foo.path` 는 `foo.service` 를 활성화 → 다른 이름을 쓰려면 `Unit=` 으로 지정.

```ini
[Path]
PathChanged=/etc/myapp.conf
Unit=myapp-reload.service
```

---

### 동작 방식

1. `.path` unit이 시작되면 systemd가 inotify watch를 등록
2. 변경 이벤트가 오면 연관 unit을 활성화
3. 연관 unit이 종료되면 다시 감시 모드로 복귀

`PathExists` / `DirectoryNotEmpty` 같은 "상태" 기반 옵션은 unit 시작 시 즉시 평가됨 → 이미 조건이 맞으면 바로 활성화.

`PathChanged` / `PathModified` 는 "이벤트" 기반이라 unit 시작 후 발생한 변경만 감지.

#### MakeDirectory

```ini
[Path]
PathChanged=/var/incoming
MakeDirectory=yes
DirectoryMode=0755
```

감시 대상 디렉터리가 없으면 자동으로 생성.

---

### 실전 예제

#### 1. 새 파일이 들어오면 처리

```ini
# /etc/systemd/system/process-incoming.path
[Unit]
Description=Watch incoming directory

[Path]
DirectoryNotEmpty=/var/incoming
MakeDirectory=yes

[Install]
WantedBy=multi-user.target
```

```ini
# /etc/systemd/system/process-incoming.service
[Unit]
Description=Process incoming files

[Service]
Type=oneshot
ExecStart=/usr/local/bin/process-files /var/incoming
```

#### 2. 설정 변경 시 자동 reload

```ini
# /etc/systemd/system/myapp-config.path
[Unit]
Description=Watch myapp config for changes

[Path]
PathChanged=/etc/myapp/myapp.conf
Unit=myapp-reload.service

[Install]
WantedBy=multi-user.target
```

```ini
# /etc/systemd/system/myapp-reload.service
[Unit]
Description=Reload myapp on config change

[Service]
Type=oneshot
ExecStart=/bin/systemctl reload myapp.service
```

#### 3. lock 파일 패턴

```ini
# /etc/systemd/system/maintenance.path
[Unit]
Description=Run maintenance when lock is released

[Path]
PathExists=!/var/lock/maintenance.lock
Unit=maintenance.service
```

참고: `!` 부정 연산자는 systemd 252+ 에서 일부 옵션에 사용 가능 → 옛 버전에서는 두 개의 path unit으로 표현 필요.

#### 4. 인스턴스 템플릿과 결합

`PathChanged=/var/incoming` 에 새 파일이 들어올 때마다 그 파일 이름으로 인스턴스 service를 시작하는 방법:

```ini
# process@.service
[Service]
Type=oneshot
ExecStart=/usr/local/bin/process %i
```

`.path` unit 자체로는 파일 이름을 service에 전달할 수 없지만, 트리거된 service에서 `inotifywait` 등으로 파일을 직접 가져가는 방식으로 우회 가능.

---

### 주의사항

#### inotify 큐 한도

파일 변경이 빠르게 대량으로 발생하면 inotify 이벤트가 누락될 수 있음.

```bash
sysctl fs.inotify.max_user_watches
sysctl fs.inotify.max_queued_events
```

#### 단발성 이벤트

`PathChanged`는 close 이벤트마다 unit을 한 번씩 트리거 → 짧은 시간에 변경이 여러 번 발생하면 service가 여러 번 호출될 수 있으므로, service 자체는 idempotent하게 작성 필요.

#### 디렉터리 vs 파일

디렉터리를 `PathChanged`로 감시하면 그 안의 파일 close 이벤트가 트리거됨. 단, 하위 디렉터리는 재귀적으로 감시되지 않음 → 깊은 디렉터리 트리를 감시하려면 별도의 inotify 도구 필요.

#### NFS와 가상 파일시스템

inotify는 로컬 파일시스템의 변경만 감지 → NFS를 사용하는 경우 다른 클라이언트의 변경은 감지되지 않음.

---

### 참고 자료

- [man systemd.path](https://www.freedesktop.org/software/systemd/man/systemd.path.html)
- [man inotify](https://man7.org/linux/man-pages/man7/inotify.7.html)
