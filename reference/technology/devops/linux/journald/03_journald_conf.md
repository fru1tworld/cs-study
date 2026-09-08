# journald.conf 설정

> 원본: https://www.freedesktop.org/software/systemd/man/journald.conf.html

<a id="설정-파일-위치"></a>
## 설정 파일 위치

- journald 설정은 다음 우선순위로 적용됨

```
/etc/systemd/journald.conf
/etc/systemd/journald.conf.d/*.conf
/run/systemd/journald.conf.d/*.conf
/usr/lib/systemd/journald.conf.d/*.conf
```

패키지가 제공하는 설정과 충돌하지 않도록 drop-in 디렉터리(`journald.conf.d/`)에 별도 파일을 두는 편이 좋다. 설정을 바꾼 뒤에는 journald를 다시 시작한다.

```bash
sudo systemctl restart systemd-journald
```

- 전체 설정 확인:

```bash
systemd-analyze cat-config systemd/journald.conf
```

<a id="storage"></a>

## Storage

```ini
[Journal]
Storage=auto
```

- 값별 동작
  - `volatile`: 항상 RAM (`/run/log/journal/`)에 저장하며 재부팅 시 사라짐
  - `persistent`: 항상 디스크 (`/var/log/journal/`)에 저장하며 디렉터리 자동 생성
  - `auto` (기본): `/var/log/journal/`이 존재하면 디스크, 아니면 RAM
  - `none`: 저장 안 함
    - forward만 (예: rsyslog가 받음)

- 영구 저장을 원하면:

```bash
sudo mkdir -p /var/log/journal
sudo systemd-tmpfiles --create --prefix /var/log/journal
sudo systemctl restart systemd-journald
```

<a id="압축과-sealing"></a>

## 압축과 sealing

### Compress

```ini
Compress=yes
```

- 기본적으로 512바이트 이상의 데이터 객체는 자동으로 압축됨
- 텍스트 로그는 일반적으로 압축률 70% 이상

- 크기 임계값 지정:

```ini
Compress=512    # 512바이트 이상 메시지만 압축
```

### Seal (FSS)

- Forward Secure Sealing: journal 파일을 암호학적으로 봉인해 사후 변조를 탐지할 수 있게 만드는 기능

```ini
Seal=yes
```

- 설정 후 sealing key 생성:

```bash
sudo journalctl --setup-keys
sudo journalctl --setup-keys --interval=1h
```

이 명령은 seed key를 journal 디렉터리에 저장하고 verification key를 출력한다. 출력된 verification key는 텍스트나 QR로 보관했다가 다음과 같이 검증에 사용한다.

```bash
sudo journalctl --verify
sudo journalctl --verify-key=<verification-key>
```

- 검증 결과:

```
PASS: /var/log/journal/abc/system.journal
```

- 변조가 있으면 `FAIL`로 어디부터 무결성이 깨졌는지 표시

- Sealing은 디스크가 외부에 압류되거나 공격받았을 때 특정 시점 이후의 로그가 위조됐는지 탐지하는 용도 → 실시간 보호는 아님

<a id="크기와-보존-한도"></a>

## 크기와 보존 한도

### 영구 저장 (/var/log/journal)

```ini
SystemMaxUse=4G
SystemKeepFree=1G
SystemMaxFileSize=128M
SystemMaxFiles=100
```

- 옵션별 의미
  - `SystemMaxUse=`: journal이 사용할 최대 용량
  - `SystemKeepFree=`: 디스크에 항상 비워둘 공간
  - `SystemMaxFileSize=`: 개별 journal 파일 최대 크기 (이 값 도달 시 회전)
  - `SystemMaxFiles=`: 최대 파일 개수

- 기본값
  - `SystemMaxUse=`는 디스크 크기의 10%까지, 단 4G 이하
  - `SystemKeepFree=`는 15% 또는 4G

### 휘발성 저장 (/run/log/journal)

```ini
RuntimeMaxUse=128M
RuntimeKeepFree=128M
RuntimeMaxFileSize=64M
RuntimeMaxFiles=20
```

- RAM 사용 → 기본값이 더 작음

### 시간 기반 보존

```ini
MaxRetentionSec=2week
MaxFileSec=1month
```

- `MaxRetentionSec=`: 이 시간이 지난 항목은 자동 삭제
- `MaxFileSec=`: 한 파일이 이 시간을 초과하면 회전

- 크기 한도와 시간 한도가 함께 적용됨 (둘 중 더 빨리 도달하는 쪽 기준)

### 즉시 적용

```bash
sudo journalctl --vacuum-size=2G
sudo journalctl --vacuum-time=1month
```

<a id="rate-limit"></a>

## Rate Limit

- 로그 폭주로 디스크와 CPU가 점유되는 것을 방지하는 기능

```ini
RateLimitIntervalSec=30s
RateLimitBurst=10000
```

이 설정에서는 30초 안에 한 서비스의 로그가 10000건을 넘으면 이후 메시지를 일시 차단한다. 차단 중 누락된 메시지 건수는 별도로 기록해 얼마나 생략됐는지 알 수 있게 한다.

- 서비스별로 unit 파일에서 재정의 가능:

```ini
[Service]
LogRateLimitIntervalSec=10s
LogRateLimitBurst=1000
```

### 비활성화

```ini
RateLimitBurst=0
```

- 진단 용도로만 권장, 프로덕션에서는 적절한 값 유지 필요

<a id="forward-옵션"></a>

## Forward 옵션

- journald가 수신한 로그를 다른 채널로 전달할지 제어하는 옵션

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

- `yes`면 `/run/systemd/journal/syslog` 소켓을 통해 syslog 데몬(rsyslog/syslog-ng)에 전달
- rsyslog가 `imjournal` 대신 이 소켓을 사용할 때 활용

### ForwardToKMsg

- 커널 ring buffer로 전달
- 일반적으로 비권장 (커널 dmesg가 애플리케이션 로그로 오염됨)

### ForwardToConsole

- 지정한 TTY에 출력, 디버깅용

### ForwardToWall

- 긴급 메시지를 모든 로그인 사용자의 터미널에 표시 (`wall(1)` 유사), 기본값 yes

### MaxLevel*

- 각 채널의 최대 레벨 지정
- 예: `MaxLevelKMsg=notice`면 notice 이하만 kmsg로 전달

<a id="기타-옵션"></a>

## 기타 옵션

### LineMax

```ini
LineMax=48K
```

`LineMax`는 스트림 로그를 레코드 로그로 변환할 때 허용하는 최대 줄 길이다. 한 줄이 이 길이를 넘으면 여러 레코드로 나눠 저장한다.

### ReadKMsg

```ini
ReadKMsg=yes
```

- `/dev/kmsg`에서 커널 메시지를 읽을지 결정
- 컨테이너 안에서는 보통 `no`

### Audit

```ini
Audit=yes
```

- 커널 audit 메시지 수신 여부
- systemd 240 이상 기본값 yes

### SplitMode

```ini
SplitMode=uid
```

- 값별 의미
  - `uid` (기본): 사용자별 별도 파일 (`user-1000.journal`)
  - `none`: 모두 system.journal에 기록

- `uid` 모드는 사용자별 권한 분리에 좋지만 파일 수가 많아짐

<a id="추천-설정-예시"></a>

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

- rsyslog 측 설정:

```
# /etc/rsyslog.d/forward.conf
*.* @@central-log-server:514
```

<a id="참고-자료"></a>

## 참고 자료

- [man journald.conf](https://www.freedesktop.org/software/systemd/man/journald.conf.html)
- [man systemd-journald](https://www.freedesktop.org/software/systemd/man/systemd-journald.service.html)
