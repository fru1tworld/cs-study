# journald.conf 설정

> 원본: https://www.freedesktop.org/software/systemd/man/journald.conf.html

---

## 목차

1. [설정 파일 위치](#설정-파일-위치)
2. [Storage](#storage)
3. [압축과 sealing](#압축과-sealing)
4. [크기와 보존 한도](#크기와-보존-한도)
5. [Rate Limit](#rate-limit)
6. [Forward 옵션](#forward-옵션)
7. [기타 옵션](#기타-옵션)
8. [추천 설정 예시](#추천-설정-예시)
9. [참고 자료](#참고-자료)

---

## 설정 파일 위치

journald 설정은 다음 우선순위로 적용됩니다:

```
/etc/systemd/journald.conf
/etc/systemd/journald.conf.d/*.conf
/run/systemd/journald.conf.d/*.conf
/usr/lib/systemd/journald.conf.d/*.conf
```

패키지 충돌을 방지하려면 `drop-in` 디렉터리(`journald.conf.d/`)에 별도 파일을 두는 것이 좋습니다.

설정 변경 후:

```bash
sudo systemctl restart systemd-journald
```

전체 설정 확인:

```bash
systemd-analyze cat-config systemd/journald.conf
```

---

## Storage

```ini
[Journal]
Storage=auto
```

| 값 | 동작 |
| --- | --- |
| `volatile` | 항상 RAM (`/run/log/journal/`) — 재부팅 시 사라짐 |
| `persistent` | 항상 디스크 (`/var/log/journal/`) — 디렉터리 자동 생성 |
| `auto` (기본) | `/var/log/journal/` 이 존재하면 디스크, 아니면 RAM |
| `none` | 저장 안 함. forward만 (예: rsyslog가 받음) |

영구 저장을 원하면:

```bash
sudo mkdir -p /var/log/journal
sudo systemd-tmpfiles --create --prefix /var/log/journal
sudo systemctl restart systemd-journald
```

---

## 압축과 sealing

### Compress

```ini
Compress=yes
```

기본적으로 512바이트 이상의 데이터 객체는 자동으로 압축됩니다. 텍스트 로그는 일반적으로 70% 이상 압축률.

크기 임계값 지정:
```ini
Compress=512    # 512바이트 이상 메시지만 압축
```

### Seal (FSS)

**Forward Secure Sealing**. journal 파일을 암호학적으로 봉인해 사후 변조를 탐지할 수 있게 만드는 기능.

```ini
Seal=yes
```

설정 후 sealing key 생성:

```bash
sudo journalctl --setup-keys
sudo journalctl --setup-keys --interval=1h
```

이 명령은:
1. seed key를 생성해 journal 디렉터리에 저장
2. verification key를 출력 (사람이 읽거나 QR로 보관)

검증:

```bash
sudo journalctl --verify
sudo journalctl --verify-key=<verification-key>
```

검증 결과:
```
PASS: /var/log/journal/abc/system.journal
```

또는 변조가 있으면 `FAIL` 로 어디부터 무결성이 깨졌는지 표시.

> Sealing은 디스크가 외부에 압류되거나 공격받았을 때 특정 시점 이후의 로그가 위조됐는지 탐지하는 데 쓰입니다. 실시간 보호는 아닙니다.

---

## 크기와 보존 한도

### 영구 저장 (/var/log/journal)

```ini
SystemMaxUse=4G
SystemKeepFree=1G
SystemMaxFileSize=128M
SystemMaxFiles=100
```

| 옵션 | 의미 |
| --- | --- |
| `SystemMaxUse=` | journal이 사용할 최대 용량 |
| `SystemKeepFree=` | 디스크에 항상 비워둘 공간 |
| `SystemMaxFileSize=` | 개별 journal 파일 최대 크기 (이 값 도달 시 회전) |
| `SystemMaxFiles=` | 최대 파일 개수 |

기본값:
- `SystemMaxUse=` 는 디스크 크기의 10%까지, 단 4G 이하
- `SystemKeepFree=` 는 15% 또는 4G

### 휘발성 저장 (/run/log/journal)

```ini
RuntimeMaxUse=128M
RuntimeKeepFree=128M
RuntimeMaxFileSize=64M
RuntimeMaxFiles=20
```

RAM을 사용하므로 기본값이 더 작습니다.

### 시간 기반 보존

```ini
MaxRetentionSec=2week
MaxFileSec=1month
```

- `MaxRetentionSec=`: 이 시간이 지난 항목은 자동 삭제
- `MaxFileSec=`: 한 파일이 이 시간을 초과하면 회전

크기 한도와 시간 한도가 함께 적용됩니다 (둘 중 더 빨리 도달하는 쪽).

### 즉시 적용

```bash
sudo journalctl --vacuum-size=2G
sudo journalctl --vacuum-time=1month
```

---

## Rate Limit

로그 폭주로 디스크와 CPU가 점유되는 것을 방지하는 기능.

```ini
RateLimitIntervalSec=30s
RateLimitBurst=10000
```

- 30초 안에 한 서비스에서 10000건이 넘으면 그 이후 메시지 일시 차단
- 차단 동안 누락된 건수는 별도로 기록됨

서비스별로 unit 파일에서 재정의 가능:

```ini
[Service]
LogRateLimitIntervalSec=10s
LogRateLimitBurst=1000
```

### 비활성화

```ini
RateLimitBurst=0
```

진단 용도로만 권장. 프로덕션에서는 적절한 값을 유지.

---

## Forward 옵션

journald가 수신한 로그를 다른 채널로 전달할지 제어.

```ini
ForwardToSyslog=yes
ForwardToKMsg=no
ForwardToConsole=no
ForwardToWall=yes

MaxLevelStore=debug
MaxLevelSyslog=debug
MaxLevelKMsg=notice
MaxLevelConsole=info
MaxLevelWall=emerg

TTYPath=/dev/console
```

### ForwardToSyslog

`yes` 면 `/run/systemd/journal/syslog` 소켓을 통해 syslog 데몬(rsyslog/syslog-ng)에 전달. rsyslog가 `imjournal` 대신 이 소켓을 사용할 때 활용.

### ForwardToKMsg

커널 ring buffer로 보냄. 일반적으로 비권장 (커널 dmesg가 어플리케이션 로그로 오염됨).

### ForwardToConsole

지정한 TTY에 출력. 디버깅용.

### ForwardToWall

긴급 메시지를 모든 로그인 사용자의 터미널에 표시 (`wall(1)` 비슷). 기본 `yes`.

### MaxLevel*

각 채널의 최대 레벨. 예: `MaxLevelKMsg=notice` 면 notice 이하만 kmsg로 전달.

---

## 기타 옵션

### LineMax

```ini
LineMax=48K
```

스트림 로그를 레코드 로그로 변환할 때 허용하는 최대 줄 길이. 이 길이를 초과하면 분할됩니다.

### ReadKMsg

```ini
ReadKMsg=yes
```

`/dev/kmsg` 에서 커널 메시지를 읽을지. 컨테이너 안에서는 `no` 가 보통.

### Audit

```ini
Audit=yes
```

커널 audit 메시지를 받을지. systemd 240+ 기본값 yes.

### SplitMode

```ini
SplitMode=uid
```

| 값 | 의미 |
| --- | --- |
| `uid` (기본) | 사용자별 별도 파일 (`user-1000.journal`) |
| `none` | 모두 system.journal에 |

`uid` 모드는 사용자별 권한 분리에 좋지만 파일 수가 많아짐.

---

## 추천 설정 예시

### 프로덕션 서버 (디스크 여유 있음)

```ini
# /etc/systemd/journald.conf.d/production.conf
[Journal]
Storage=persistent
Compress=yes
Seal=yes

SystemMaxUse=8G
SystemKeepFree=5G
SystemMaxFileSize=256M
MaxRetentionSec=1month

RateLimitIntervalSec=30s
RateLimitBurst=20000

ForwardToSyslog=no
ForwardToWall=yes
MaxLevelStore=info
```

### 컨테이너/임베디드 (메모리 적음)

```ini
[Journal]
Storage=volatile
Compress=yes

RuntimeMaxUse=64M
RuntimeMaxFileSize=16M
RuntimeMaxFiles=5

RateLimitIntervalSec=10s
RateLimitBurst=1000

ForwardToSyslog=no
ReadKMsg=no
```

### syslog 중앙 수집과 공존

```ini
[Journal]
Storage=persistent
Compress=yes
SystemMaxUse=2G
MaxRetentionSec=1week

ForwardToSyslog=yes
MaxLevelSyslog=info
```

rsyslog 측에서:

```
# /etc/rsyslog.d/forward.conf
*.* @@central-log-server:514
```

---

## 참고 자료

- [man journald.conf](https://www.freedesktop.org/software/systemd/man/journald.conf.html)
- [man systemd-journald](https://www.freedesktop.org/software/systemd/man/systemd-journald.service.html)
