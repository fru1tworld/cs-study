# Generators와 Drop-in

> 이 문서는 `man systemd.generator` 와 drop-in 관련 매뉴얼을 정리한 것입니다.
> 원본: https://www.freedesktop.org/software/systemd/man/systemd.generator.html

---

## 목차

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

## Generator란?

generator는 **부팅 초기에 실행되어 unit 파일을 동적으로 생성** 하는 작은 바이너리입니다. systemd가 unit을 본격적으로 처리하기 전에 호출되어 결과를 임시 디렉터리에 작성합니다.

### 실행 시점

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

### 설치 위치

| 경로 | 종류 |
| --- | --- |
| `/usr/lib/systemd/system-generators/` | 시스템 generator (배포판) |
| `/etc/systemd/system-generators/` | 관리자 추가 |
| `/usr/lib/systemd/user-generators/` | 사용자 generator |

### 생성 위치

| 경로 | 우선순위 |
| --- | --- |
| `/run/systemd/generator.early/` | `/etc` 보다 높음 |
| `/run/systemd/generator/` | `/etc/systemd/system/` 과 동급 |
| `/run/systemd/generator.late/` | `/usr/lib` 보다 낮음 |

generator는 인자로 위 세 경로를 받습니다.

---

## 표준 generator

배포판이 기본 제공하는 주요 generator.

### systemd-fstab-generator

`/etc/fstab` 을 읽어 `.mount` 와 `.swap` unit으로 변환. 가장 흔히 사용되는 generator.

### systemd-getty-generator

가상 콘솔(`tty1`, `tty2`, ...)에 자동으로 `getty@ttyN.service` 인스턴스 생성.

### systemd-rc-local-generator

`/etc/rc.local` 이 있으면 `rc-local.service` 를 만들어 부팅 마지막에 실행. SysV 호환성 layer.

### systemd-system-update-generator

`/system-update` 심볼릭 링크가 있으면 `system-update.target` 으로 isolate. OS 업그레이드용.

### systemd-cryptsetup-generator

`/etc/crypttab` 을 읽어 암호화된 디스크의 unlock unit 생성.

### systemd-gpt-auto-generator

GPT 파티션 타입 GUID를 보고 root, /home, swap 등을 자동 마운트.

### systemd-debug-generator

커널 부트 파라미터 `systemd.mask=`, `systemd.wants=`, `systemd.debug-shell` 등을 unit으로 변환.

### systemd-veritysetup-generator

dm-verity 기반 무결성 검증 디바이스 셋업.

### environment-generators

환경 변수 동적 생성. `/usr/lib/systemd/system-environment-generators/` 등에 위치.

---

## generator 작성

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

### 제약

- 짧고 빨라야 함 (부팅 초기에 실행)
- D-Bus, journal, 네트워크 사용 금지
- shell이 가능하지만 최소화 권장
- 절대 systemctl을 호출하면 안 됨 (데드락 위험)

---

## Drop-in 디렉터리

기존 unit을 직접 수정하지 않고 일부 옵션만 추가/변경하는 메커니즘.

### 위치

unit 이름 뒤에 `.d/` 디렉터리를 만들고 그 안에 `.conf` 파일을 둡니다.

```
/etc/systemd/system/
└── nginx.service.d/
    ├── 10-restart.conf
    ├── 20-resource.conf
    └── 50-env.conf
```

`/etc/`, `/run/`, `/usr/lib/` 의 모든 `.d/` 디렉터리가 검색됩니다.

### 가장 쉬운 작성법

```bash
sudo systemctl edit nginx.service
```

자동으로 `/etc/systemd/system/nginx.service.d/override.conf` 를 만들고 편집기를 띄웁니다.

```bash
sudo systemctl edit --full nginx.service    # 전체 unit 편집
```

### 예시

```ini
# /etc/systemd/system/nginx.service.d/10-restart.conf
[Service]
Restart=always
RestartSec=2
StartLimitIntervalSec=0
```

원본 nginx.service의 `[Service]` 에 위 옵션이 추가됩니다.

---

## Drop-in 우선순위와 누적

### 알파벳 순 적용

같은 디렉터리 안의 `.conf` 파일은 **알파벳 순** 으로 적용됩니다. 따라서 `10-`, `20-`, `90-` 같은 숫자 접두사를 붙이는 관습이 일반적.

### 경로별 우선순위

`/etc/` > `/run/` > `/usr/lib/`

### 리스트 누적 vs 초기화

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

### 검증

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

## 전역 default 변경

특정 옵션의 기본값을 시스템 전체에 적용하고 싶을 때.

### 모든 service에 기본 옵션

```ini
# /etc/systemd/system/service.d/50-defaults.conf
[Service]
TimeoutStartSec=120
TimeoutStopSec=30
```

`service.d` 디렉터리는 모든 `.service` unit에 적용되는 특별한 drop-in.

### 모든 unit 종류 공통

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

## Preset

배포판이 패키지 설치 시 자동으로 enable 또는 disable할지를 정하는 정책.

### 위치

`/usr/lib/systemd/system-preset/*.preset`

### 형식

```
# /usr/lib/systemd/system-preset/90-default.preset
enable nginx.service
disable httpd.service
disable telnet.socket

# glob 가능
disable *
enable network*.service
```

### 적용

```bash
sudo systemctl preset nginx.service     # 단일
sudo systemctl preset-all                # 전체
```

`preset-all` 은 위험할 수 있으니 신중히. 새 시스템 셋업이나 컨테이너 이미지에서 주로 사용.

---

## systemd-run으로 즉석 unit

unit 파일을 만들지 않고 명령어를 즉석에서 unit으로 감싸 실행.

### 임시 service

```bash
sudo systemd-run --unit=batch-job /usr/local/bin/process-data
```

이름이 부여되어 다른 systemctl 명령으로 다룰 수 있습니다:

```bash
systemctl status batch-job
journalctl -u batch-job
```

### 리소스 제한

```bash
sudo systemd-run --slice=batch.slice \
  -p MemoryMax=2G -p CPUQuota=50% -p Nice=10 \
  /usr/local/bin/heavy-task
```

### scope vs service

- `--scope`: 외부에서 시작된 프로세스를 cgroup으로 감쌈 (PID는 호출자)
- 기본 (service): systemd가 자식 프로세스로 fork

```bash
# 현재 셸에서 실행하면서 cgroup만 적용
systemd-run --scope --user -p MemoryMax=4G stress
```

### 일회성 timer

```bash
sudo systemd-run --on-calendar='*-*-* 03:00:00' \
  --unit=midnight-cleanup \
  /usr/local/bin/cleanup
```

### immediate 실행 (지연)

```bash
sudo systemd-run --on-active=10s /bin/touch /tmp/marker
sudo systemd-run --on-boot=1h /usr/local/bin/post-boot-task
```

---

## 참고 자료

- [man systemd.generator](https://www.freedesktop.org/software/systemd/man/systemd.generator.html)
- [man systemd-fstab-generator](https://www.freedesktop.org/software/systemd/man/systemd-fstab-generator.html)
- [man systemd.preset](https://www.freedesktop.org/software/systemd/man/systemd.preset.html)
- [man systemd-run](https://www.freedesktop.org/software/systemd/man/systemd-run.html)
- [man systemd-system.conf](https://www.freedesktop.org/software/systemd/man/systemd-system.conf.html)
