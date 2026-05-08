# 사용자 Unit과 세션

> 이 문서는 `man systemd` 의 사용자 인스턴스 절과 `man logind.conf` 의 내용을 한국어로 정리한 것입니다.
> 원본: https://www.freedesktop.org/software/systemd/man/user@.service.html , https://www.freedesktop.org/software/systemd/man/logind.conf.html

---

## 목차

1. [User instance란?](#user-instance란)
2. [디렉터리 구조](#디렉터리-구조)
3. [기본 명령](#기본-명령)
4. [Linger](#linger)
5. [환경 변수](#환경-변수)
6. [Logind 세션](#logind-세션)
7. [실전 예제](#실전-예제)
8. [참고 자료](#참고-자료)

---

## User instance란?

systemd는 시스템 매니저(PID 1) 외에 **각 사용자별 매니저** 도 실행합니다. 사용자가 처음 로그인하면 `user@<UID>.service` 가 시작되고, 그 안에서 별도의 systemd 인스턴스가 사용자 unit을 관리합니다.

```
PID 1 systemd (시스템)
  └─ user@1000.service (alice의 매니저)
       └─ user 1000의 unit들...
  └─ user@1001.service (bob의 매니저)
       └─ user 1001의 unit들...
```

장점:
- 시스템 권한 없이 자기 데몬을 등록 가능
- 데스크탑 자동화, 백그라운드 sync 등에 적합
- cgroup으로 사용자 리소스 격리

---

## 디렉터리 구조

사용자 unit 검색 경로 (우선순위 순):

| 경로 | 용도 |
| --- | --- |
| `~/.config/systemd/user/` | 사용자 자신의 정의 (가장 높음) |
| `/etc/systemd/user/` | 관리자가 모든 사용자에게 배포 |
| `/run/systemd/user/` | 런타임 |
| `/usr/lib/systemd/user/` | 패키지 설치 |

---

## 기본 명령

모든 systemctl 명령에 `--user` 플래그를 붙이면 됩니다.

```bash
systemctl --user start mybackup.service
systemctl --user enable mybackup.timer
systemctl --user status mybackup.service
systemctl --user list-units
systemctl --user list-timers
systemctl --user daemon-reload
```

### 사용자 매니저 자체 다루기

```bash
systemctl --user        # 활성 unit 목록
loginctl                 # 로그인 세션 정보
loginctl show-user $USER
```

### journal

```bash
journalctl --user                     # 자기 unit의 로그
journalctl --user -u mybackup.timer
journalctl --user-unit=mybackup       # 동일
```

---

## Linger

기본적으로 사용자 매니저는 **사용자가 로그인해 있는 동안만** 실행됩니다. SSH 세션 종료 시 매니저도 함께 종료되어 모든 사용자 unit이 중지됩니다.

이를 막고 부팅부터 종료까지 사용자 unit을 계속 실행하려면 **linger** 를 활성화:

```bash
sudo loginctl enable-linger alice
sudo loginctl disable-linger alice
loginctl show-user alice | grep Linger
```

linger가 켜진 사용자는:
- 부팅 시 자동으로 매니저 시작
- 로그아웃해도 매니저 유지
- `~/.config/systemd/user/default.target.wants/` 의 unit이 부팅 시 시작

홈 서버에서 사용자 권한으로 백그라운드 작업을 돌릴 때 필수.

---

## 환경 변수

사용자 매니저는 시스템 매니저와 별도의 환경을 가집니다. 일반적인 셸 rc 파일(`.bashrc` 등)은 systemd가 unit을 실행할 때 source되지 않으므로 환경을 명시적으로 전달해야 합니다.

### unit 안에서 직접

```ini
[Service]
Environment="PATH=/usr/local/bin:/usr/bin:/bin"
EnvironmentFile=%h/.config/myapp/env
```

`%h` 는 사용자 홈 디렉터리.

### 매니저 환경 설정

`~/.config/environment.d/*.conf` 파일에 다음 형식:

```
PATH=/usr/local/bin:/usr/bin:/bin
EDITOR=vim
GOPATH=%h/go
```

이 변수들은 사용자 매니저와 모든 자식 unit에 자동 적용됩니다.

### 런타임 추가/제거

```bash
systemctl --user set-environment FOO=bar
systemctl --user import-environment DISPLAY XAUTHORITY
systemctl --user unset-environment FOO
```

데스크탑 세션에서 GUI 앱을 systemd로 띄울 때 `DISPLAY`, `XAUTHORITY`, `WAYLAND_DISPLAY` 등을 import하는 패턴이 흔합니다.

---

## Logind 세션

`systemd-logind` 는 사용자 로그인 세션을 관리합니다.

### 세션 정보

```bash
$ loginctl
SESSION  UID USER  SEAT  TTY
      3 1000 alice seat0 tty2
      5 1000 alice         pts/0

$ loginctl show-session 3
$ loginctl session-status 3
```

각 세션은:
- 고유 ID
- TTY 또는 graphical 디스플레이
- 좌석(seat)
- 잠금/잠금해제 상태

### 세션 종료

```bash
loginctl terminate-session 3
loginctl terminate-user alice
```

### 좌석 (Seat)

물리적인 입력/출력 장치 그룹. 보통 `seat0` 하나지만, 멀티-시트 데스크탑(키보드/모니터 여러 세트)에서 의미가 있습니다.

```bash
loginctl list-seats
loginctl seat-status seat0
```

### logind 설정

`/etc/systemd/logind.conf`:

```ini
[Login]
HandleLidSwitch=suspend           # 노트북 덮개 닫으면 절전
HandlePowerKey=poweroff           # 전원 버튼
HandleSuspendKey=suspend
HandleHibernateKey=hibernate
IdleAction=ignore                 # 유휴 시 동작
IdleActionSec=30min
KillUserProcesses=no              # 로그아웃 시 프로세스 죽일지
RuntimeDirectorySize=10%          # /run/user/<UID> 크기
InhibitDelayMaxSec=5
RemoveIPC=yes
```

`KillUserProcesses=yes` (Debian/Ubuntu 기본) 이면 로그아웃 시 사용자의 모든 프로세스를 죽입니다 — `nohup` 이나 `screen` 같은 도구도 무력화됨. linger를 켜면 예외 처리.

### Inhibit

상태 전환을 임시로 막는 메커니즘. 백업 작업 중 절전 방지 등.

```bash
systemd-inhibit --what=sleep --who=backup --why="Backup running" /usr/local/bin/backup.sh
```

`--what` 가능 값: `sleep`, `idle`, `shutdown`, `handle-power-key`, `handle-suspend-key`, `handle-hibernate-key`, `handle-lid-switch`.

```bash
$ systemd-inhibit --list
WHO   UID USER PID COMM            WHAT             WHY              MODE
NetworkManager 0 root 752 NetworkManager sleep            NetworkManager  delay
```

---

## 실전 예제

### 1. 사용자 백업 타이머

```ini
# ~/.config/systemd/user/backup.service
[Unit]
Description=My Backup

[Service]
Type=oneshot
ExecStart=%h/bin/backup.sh
```

```ini
# ~/.config/systemd/user/backup.timer
[Unit]
Description=Daily Backup

[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target
```

```bash
systemctl --user daemon-reload
systemctl --user enable --now backup.timer
loginctl enable-linger $USER     # 로그아웃해도 동작하도록
```

### 2. 데스크탑 알림 데몬

```ini
# ~/.config/systemd/user/notify-on-mail.service
[Unit]
Description=Mail notification daemon
After=graphical-session.target
PartOf=graphical-session.target

[Service]
ExecStart=%h/bin/mail-notifier
Restart=on-failure

[Install]
WantedBy=graphical-session.target
```

`graphical-session.target` 은 GUI 세션이 활성일 때만 도달하는 target.

```bash
systemctl --user import-environment DISPLAY XAUTHORITY
systemctl --user enable --now notify-on-mail.service
```

### 3. SSH 에이전트

```ini
# ~/.config/systemd/user/ssh-agent.service
[Unit]
Description=SSH key agent

[Service]
Type=simple
Environment=SSH_AUTH_SOCK=%t/ssh-agent.socket
ExecStart=/usr/bin/ssh-agent -D -a $SSH_AUTH_SOCK

[Install]
WantedBy=default.target
```

`%t` 는 `$XDG_RUNTIME_DIR` (보통 `/run/user/1000`).

`~/.bashrc` 에 추가:
```bash
export SSH_AUTH_SOCK="$XDG_RUNTIME_DIR/ssh-agent.socket"
```

---

## 참고 자료

- [man user@.service](https://www.freedesktop.org/software/systemd/man/user@.service.html)
- [man logind.conf](https://www.freedesktop.org/software/systemd/man/logind.conf.html)
- [man loginctl](https://www.freedesktop.org/software/systemd/man/loginctl.html)
- [man systemd-inhibit](https://www.freedesktop.org/software/systemd/man/systemd-inhibit.html)
