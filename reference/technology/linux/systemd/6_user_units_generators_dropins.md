# 사용자 Unit과 세션, Generators와 Drop-in

## 사용자 Unit과 세션

> 원본: https://www.freedesktop.org/software/systemd/man/user@.service.html , https://www.freedesktop.org/software/systemd/man/logind.conf.html

---

### 목차

1. [User instance란?](#user-instance란)
2. [디렉터리 구조](#디렉터리-구조)
3. [기본 명령](#기본-명령)
4. [Linger](#linger)
5. [환경 변수](#환경-변수)
6. [Logind 세션](#logind-세션)
7. [실전 예제](#실전-예제)
8. [참고 자료](#참고-자료)

---

### User instance란?

systemd는 시스템 매니저(PID 1) 외에 **각 사용자별 매니저**도 실행합니다. 사용자가 처음 로그인하면 `user@<UID>.service`가 시작되고, 그 안에서 별도의 systemd 인스턴스가 사용자 unit을 관리합니다.

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

### 디렉터리 구조

사용자 unit 검색 경로 (우선순위 순):

| 경로 | 용도 |
| --- | --- |
| `~/.config/systemd/user/` | 사용자 자신의 정의 (가장 높음) |
| `/etc/systemd/user/` | 관리자가 모든 사용자에게 배포 |
| `/run/systemd/user/` | 런타임 |
| `/usr/lib/systemd/user/` | 패키지 설치 |

---

### 기본 명령

모든 systemctl 명령에 `--user` 플래그를 붙여 사용합니다.

```bash
systemctl --user start mybackup.service
systemctl --user enable mybackup.timer
systemctl --user status mybackup.service
systemctl --user list-units
systemctl --user list-timers
systemctl --user daemon-reload
```

#### 사용자 매니저 자체 다루기

```bash
systemctl --user        # 활성 unit 목록
loginctl                 # 로그인 세션 정보
loginctl show-user $USER
```

#### journal

```bash
journalctl --user                     # 자기 unit의 로그
journalctl --user -u mybackup.timer
journalctl --user-unit=mybackup       # 동일
```

---

### Linger

기본적으로 사용자 매니저는 **사용자가 로그인해 있는 동안만** 실행됩니다. SSH 세션이 종료되면 매니저도 함께 종료되어 모든 사용자 unit이 중지됩니다.

부팅부터 종료까지 사용자 unit을 계속 실행하려면 **linger**를 활성화합니다:

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

### 환경 변수

사용자 매니저는 시스템 매니저와 별도의 환경을 가집니다. 일반적인 셸 rc 파일(`.bashrc` 등)은 systemd가 unit을 실행할 때 source되지 않으므로, 필요한 환경 변수는 명시적으로 전달해야 합니다.

#### unit 안에서 직접

```ini
[Service]
Environment="PATH=/usr/local/bin:/usr/bin:/bin"
EnvironmentFile=%h/.config/myapp/env
```

`%h` 는 사용자 홈 디렉터리.

#### 매니저 환경 설정

`~/.config/environment.d/*.conf` 파일에 다음 형식:

```
PATH=/usr/local/bin:/usr/bin:/bin
EDITOR=vim
GOPATH=%h/go
```

이 변수들은 사용자 매니저와 모든 자식 unit에 자동 적용됩니다.

#### 런타임 추가/제거

```bash
systemctl --user set-environment FOO=bar
systemctl --user import-environment DISPLAY XAUTHORITY
systemctl --user unset-environment FOO
```

데스크탑 세션에서 GUI 앱을 systemd로 실행할 때 `DISPLAY`, `XAUTHORITY`, `WAYLAND_DISPLAY` 등을 import하는 패턴이 흔히 쓰입니다.

---

### Logind 세션

`systemd-logind` 는 사용자 로그인 세션을 관리합니다.

#### 세션 정보

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

#### 세션 종료

```bash
loginctl terminate-session 3
loginctl terminate-user alice
```

#### 좌석 (Seat)

물리적인 입력/출력 장치 그룹. 보통 `seat0` 하나지만, 멀티-시트 데스크탑(키보드/모니터 여러 세트)에서 의미가 있습니다.

```bash
loginctl list-seats
loginctl seat-status seat0
```

#### logind 설정

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

`KillUserProcesses=yes` (Debian/Ubuntu 기본값)이면 로그아웃 시 사용자의 모든 프로세스를 종료합니다. `nohup`이나 `screen` 같은 도구도 무력화됩니다. linger를 활성화하면 예외 처리됩니다.

#### Inhibit

상태 전환을 일시적으로 차단하는 메커니즘입니다. 백업 작업 중 절전 방지 등에 활용합니다.

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

### 실전 예제

#### 1. 사용자 백업 타이머

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

#### 2. 데스크탑 알림 데몬

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

#### 3. SSH 에이전트

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

### 참고 자료

- [man user@.service](https://www.freedesktop.org/software/systemd/man/user@.service.html)
- [man logind.conf](https://www.freedesktop.org/software/systemd/man/logind.conf.html)
- [man loginctl](https://www.freedesktop.org/software/systemd/man/loginctl.html)
- [man systemd-inhibit](https://www.freedesktop.org/software/systemd/man/systemd-inhibit.html)

---

## Generators와 Drop-in

> 원본: https://www.freedesktop.org/software/systemd/man/systemd.generator.html

---

### 목차

1. [Generator란?](#generator란)
2. [표준 generator](#표준-generator)
3. [generator 작성](#generator-작성)
4. [Drop-in 디렉터리](#drop-in-디렉터리)
5. [Drop-in 우선순위와 누적](#drop-in-우선순위와-누적)
6. [전역 default 변경](#전역-default-변경)
7. [Preset](#preset)
8. [systemd-run으로 즉석 unit](#systemd-run으로-즉석-unit)
9. [참고 자료](#참고-자료)

---

### Generator란?

generator는 **부팅 초기에 실행되어 unit 파일을 동적으로 생성**하는 작은 바이너리입니다. systemd가 unit을 본격적으로 처리하기 전에 호출되며, 결과를 임시 디렉터리에 기록합니다.

#### 실행 시점

systemd 재시작/부팅 직후, sysinit 이전:

```
PID 1 시작
  ↓
모든 generator 병렬 실행
  ↓
임시 unit 디렉터리에 생성된 파일들 로드
  ↓
unit 의존성 그래프 구축
  ↓
default.target으로 진행
```

#### 설치 위치

| 경로 | 종류 |
| --- | --- |
| `/usr/lib/systemd/system-generators/` | 시스템 generator (배포판) |
| `/etc/systemd/system-generators/` | 관리자 추가 |
| `/usr/lib/systemd/user-generators/` | 사용자 generator |

#### 생성 위치

| 경로 | 우선순위 |
| --- | --- |
| `/run/systemd/generator.early/` | `/etc` 보다 높음 |
| `/run/systemd/generator/` | `/etc/systemd/system/` 과 동급 |
| `/run/systemd/generator.late/` | `/usr/lib` 보다 낮음 |

generator는 인자로 위 세 경로를 받습니다.

---

### 표준 generator

배포판이 기본 제공하는 주요 generator.

#### systemd-fstab-generator

`/etc/fstab` 을 읽어 `.mount` 와 `.swap` unit으로 변환. 가장 흔히 사용되는 generator.

#### systemd-getty-generator

가상 콘솔(`tty1`, `tty2`, ...)에 자동으로 `getty@ttyN.service` 인스턴스 생성.

#### systemd-rc-local-generator

`/etc/rc.local` 이 있으면 `rc-local.service` 를 만들어 부팅 마지막에 실행. SysV 호환성 layer.

#### systemd-system-update-generator

`/system-update` 심볼릭 링크가 있으면 `system-update.target` 으로 isolate. OS 업그레이드용.

#### systemd-cryptsetup-generator

`/etc/crypttab` 을 읽어 암호화된 디스크의 unlock unit 생성.

#### systemd-gpt-auto-generator

GPT 파티션 타입 GUID를 보고 root, /home, swap 등을 자동 마운트.

#### systemd-debug-generator

커널 부트 파라미터 `systemd.mask=`, `systemd.wants=`, `systemd.debug_shell` 등을 unit으로 변환.

#### systemd-veritysetup-generator

dm-verity 기반 무결성 검증 디바이스 셋업.

#### environment-generators

환경 변수 동적 생성. `/usr/lib/systemd/system-environment-generators/` 등에 위치.

---

### generator 작성

```bash
#!/bin/bash
# /etc/systemd/system-generators/my-generator

# generator는 세 인자를 받음:
NORMAL_DIR="$1"
EARLY_DIR="$2"
LATE_DIR="$3"

# 조건 체크 후 unit 생성
if [ -f /etc/myapp/enabled ]; then
    cat > "$NORMAL_DIR/myapp.service" <<EOF
[Unit]
Description=Auto-generated MyApp service
Wants=network.target
After=network.target

[Service]
ExecStart=/usr/local/bin/myapp
EOF

    # multi-user.target.wants/ 에 심볼릭 링크
    mkdir -p "$NORMAL_DIR/multi-user.target.wants"
    ln -s ../myapp.service "$NORMAL_DIR/multi-user.target.wants/myapp.service"
fi

exit 0
```

권한:
```bash
sudo chmod +x /etc/systemd/system-generators/my-generator
sudo systemctl daemon-reload
```

#### 제약

- 짧고 빨라야 함 (부팅 초기에 실행)
- D-Bus, journal, 네트워크 사용 금지
- shell이 가능하지만 최소화 권장
- 절대 systemctl을 호출하면 안 됨 (데드락 위험)

---

### Drop-in 디렉터리

기존 unit 파일을 직접 수정하지 않고 일부 옵션만 추가하거나 변경하는 메커니즘.

#### 위치

unit 이름 뒤에 `.d/` 디렉터리를 만들고 그 안에 `.conf` 파일을 둡니다.

```
/etc/systemd/system/
└── nginx.service.d/
    ├── 10-restart.conf
    ├── 20-resource.conf
    └── 50-env.conf
```

`/etc/`, `/run/`, `/usr/lib/` 의 모든 `.d/` 디렉터리가 검색됩니다.

#### 가장 쉬운 작성법

```bash
sudo systemctl edit nginx.service
```

`/etc/systemd/system/nginx.service.d/override.conf` 파일을 자동으로 생성하고 편집기를 엽니다.

```bash
sudo systemctl edit --full nginx.service    # 전체 unit 편집
```

#### 예시

```ini
# /etc/systemd/system/nginx.service.d/10-restart.conf
[Service]
Restart=always
RestartSec=2
StartLimitIntervalSec=0
```

원본 nginx.service의 `[Service]` 에 위 옵션이 추가됩니다.

---

### Drop-in 우선순위와 누적

#### 알파벳 순 적용

같은 디렉터리 안의 `.conf` 파일은 **알파벳 순** 으로 적용됩니다. 따라서 `10-`, `20-`, `90-` 같은 숫자 접두사를 붙이는 관습이 일반적.

#### 경로별 우선순위

`/etc/` > `/run/` > `/usr/lib/`

#### 리스트 누적 vs 초기화

대부분의 리스트형 옵션(`ExecStart=`, `Environment=`, `After=` 등)은 **누적** 됩니다.

```ini
# 원본
[Service]
ExecStart=/usr/sbin/nginx -g 'daemon off;'

# drop-in (의도: 교체)
[Service]
ExecStart=/usr/local/bin/my-nginx
```

이렇게 하면 ExecStart가 **두 개** 가 됩니다 (Type=oneshot이 아닌 한 오류). 교체하려면 빈 값으로 한 번 초기화해야 합니다:

```ini
[Service]
ExecStart=
ExecStart=/usr/local/bin/my-nginx
```

`Environment=` 등 다른 옵션도 동일.

#### 검증

drop-in이 어떻게 합쳐졌는지 확인:

```bash
systemctl cat nginx.service
```

```
# /usr/lib/systemd/system/nginx.service
[Unit]
...

# /etc/systemd/system/nginx.service.d/10-restart.conf
[Service]
Restart=always
```

---

### 전역 default 변경

특정 옵션의 기본값을 시스템 전체에 적용하고 싶을 때.

#### 모든 service에 기본 옵션

```ini
# /etc/systemd/system/service.d/50-defaults.conf
[Service]
TimeoutStartSec=120
TimeoutStopSec=30
```

`service.d` 디렉터리는 모든 `.service` unit에 적용되는 특별한 drop-in.

#### 모든 unit 종류 공통

```
/etc/systemd/system.conf       # 시스템 매니저 자체 설정
/etc/systemd/user.conf         # 사용자 매니저 설정
```

```ini
# /etc/systemd/system.conf
[Manager]
DefaultTimeoutStartSec=120
DefaultTimeoutStopSec=30
DefaultRestartSec=10
DefaultLimitNOFILE=65536
DefaultMemoryAccounting=yes
DefaultCPUAccounting=yes
DefaultIOAccounting=yes
```

`Default*=` 접두사가 붙은 옵션은 unit이 별도 지정하지 않을 때의 기본값.

---

### Preset

배포판이 패키지 설치 시 자동으로 enable 또는 disable할지를 정하는 정책.

#### 위치

`/usr/lib/systemd/system-preset/*.preset`

#### 형식

```
# /usr/lib/systemd/system-preset/90-default.preset
enable nginx.service
disable httpd.service
disable telnet.socket

# glob 가능
disable *
enable network*.service
```

#### 적용

```bash
sudo systemctl preset nginx.service     # 단일
sudo systemctl preset-all                # 전체
```

`preset-all`은 기존 설정을 일괄 변경하므로 신중하게 사용해야 합니다. 주로 새 시스템 초기 설정이나 컨테이너 이미지 구성 시에 활용합니다.

---

### systemd-run으로 즉석 unit

unit 파일을 별도로 작성하지 않고 명령어를 즉석에서 unit으로 감싸 실행합니다.

#### 임시 service

```bash
sudo systemd-run --unit=batch-job /usr/local/bin/process-data
```

이름이 지정되므로 이후 systemctl 명령으로 관리할 수 있습니다:

```bash
systemctl status batch-job
journalctl -u batch-job
```

#### 리소스 제한

```bash
sudo systemd-run --slice=batch.slice \
  -p MemoryMax=2G -p CPUQuota=50% -p Nice=10 \
  /usr/local/bin/heavy-task
```

#### scope vs service

- `--scope`: 외부에서 시작된 프로세스를 cgroup으로 감쌈 (프로세스의 부모는 호출자)
- 기본 (service): systemd가 직접 자식 프로세스로 fork

```bash
# 현재 셸에서 실행하면서 cgroup만 적용
systemd-run --scope --user -p MemoryMax=4G stress
```

#### 일회성 timer

```bash
sudo systemd-run --on-calendar='*-*-* 03:00:00' \
  --unit=midnight-cleanup \
  /usr/local/bin/cleanup
```

#### 지연 실행

```bash
sudo systemd-run --on-active=10s /bin/touch /tmp/marker
sudo systemd-run --on-boot=1h /usr/local/bin/post-boot-task
```

---

### 참고 자료

- [man systemd.generator](https://www.freedesktop.org/software/systemd/man/systemd.generator.html)
- [man systemd-fstab-generator](https://www.freedesktop.org/software/systemd/man/systemd-fstab-generator.html)
- [man systemd.preset](https://www.freedesktop.org/software/systemd/man/systemd.preset.html)
- [man systemd-run](https://www.freedesktop.org/software/systemd/man/systemd-run.html)
- [man systemd-system.conf](https://www.freedesktop.org/software/systemd/man/systemd-system.conf.html)
