# Forwarding과 Remote 전송

> 원본: https://www.freedesktop.org/software/systemd/man/systemd-journal-remote.service.html , https://www.freedesktop.org/software/systemd/man/systemd-journal-upload.service.html

---

## 목차

1. [Forward 옵션 복습](#forward-옵션-복습)
2. [syslog 데몬과 결합](#syslog-데몬과-결합)
3. [systemd-journal-remote](#systemd-journal-remote)
4. [systemd-journal-upload](#systemd-journal-upload)
5. [systemd-journal-gatewayd](#systemd-journal-gatewayd)
6. [중앙 수집 아키텍처](#중앙-수집-아키텍처)
7. [Loki/Vector 등으로 전송](#lokivector-등으로-전송)
8. [참고 자료](#참고-자료)

---

## Forward 옵션 복습

`journald.conf` 의 `ForwardTo*=` 옵션은 **로컬에서** 다른 채널로 복사 전송합니다.

```ini
[Journal]
ForwardToSyslog=yes        # /run/systemd/journal/syslog 소켓
ForwardToKMsg=no
ForwardToConsole=no
ForwardToWall=yes

MaxLevelStore=debug
MaxLevelSyslog=info        # syslog로 보낼 때 info 이하만
MaxLevelKMsg=notice
MaxLevelConsole=info
MaxLevelWall=emerg
```

원격 전송은 ForwardTo로는 안 되고, 별도 도구(`journal-remote`, `journal-upload`, syslog 데몬, vector 등)가 필요합니다.

---

## syslog 데몬과 결합

가장 흔한 패턴 — journald가 받고, syslog 데몬(rsyslog/syslog-ng)이 추가 가공·전송.

### rsyslog

`imjournal` 모듈로 journal에서 직접 읽거나 `imuxsock` + journald의 `ForwardToSyslog=yes` 로 받습니다.

```conf
# /etc/rsyslog.conf

module(load="imjournal"
       StateFile="/var/lib/rsyslog/imjournal.state"
       Ratelimit.Interval="600"
       Ratelimit.Burst="20000")

# 원격 전송 (TCP, TLS는 별도 설정)
*.* @@logserver.example.com:514

# 또는 파일로
*.info /var/log/messages
```

`StateFile=` 은 journal cursor를 저장해 재시작 후 누락 없이 이어서 읽기 위함.

### syslog-ng

```conf
source s_journal { systemd-journal(); };
destination d_central { syslog("logserver.example.com" port(514) transport("tls")); };
log { source(s_journal); destination(d_central); };
```

### 직접 forward만 사용 (단순 케이스)

```ini
# /etc/systemd/journald.conf
[Journal]
ForwardToSyslog=yes
```

```conf
# /etc/rsyslog.conf
$ModLoad imuxsock                # 시스템 로그 소켓
*.* @@logserver:514
```

---

## systemd-journal-remote

journal 형식을 그대로 다른 머신에 전송하는 systemd 자체 도구. journal 바이너리 인덱스가 보존되므로 검색 성능이 좋습니다.

### 수신 측 (서버)

```bash
sudo apt install systemd-journal-remote        # Debian/Ubuntu
sudo dnf install systemd-journal-remote        # RHEL/Fedora

sudo systemctl enable --now systemd-journal-remote.socket
```

기본 listen: `https://0.0.0.0:19532`

설정: `/etc/systemd/journal-remote.conf`

```ini
[Remote]
Seal=true
SplitMode=host
ServerKeyFile=/etc/ssl/private/journal-remote.pem
ServerCertificateFile=/etc/ssl/certs/journal-remote.pem
TrustedCertificateFile=/etc/ssl/certs/journal-trusted.pem
```

수신된 로그는:
```
/var/log/journal/remote/
├── remote-host01.journal
├── remote-host02.journal
└── ...
```

`SplitMode=host` 면 호스트별 분리 파일.

### 송신 측

`journal-upload` 를 사용 (다음 절).

또는 직접 push:
```bash
journalctl -o export | curl -k --data-binary @- https://server:19532/upload
```

---

## systemd-journal-upload

송신 전용 데몬. 로컬 journal을 읽어 원격 `journal-remote` 로 전송합니다.

### 설정

```ini
# /etc/systemd/journal-upload.conf
[Upload]
URL=https://logserver.example.com:19532
ServerKeyFile=/etc/ssl/private/client.key
ServerCertificateFile=/etc/ssl/certs/client.crt
TrustedCertificateFile=/etc/ssl/certs/ca.crt
```

활성화:
```bash
sudo systemctl enable --now systemd-journal-upload
```

특징:
- cursor를 저장해 재시작 후 이어서 전송
- 네트워크 끊김 시 재시도
- TLS 클라이언트 인증서 지원

### 상태 확인

```bash
journalctl -u systemd-journal-upload
systemctl status systemd-journal-upload
```

---

## systemd-journal-gatewayd

journal을 HTTP API로 노출하는 게이트웨이. 웹 UI나 다른 도구에서 조회할 수 있도록 합니다.

### 시작

```bash
sudo systemctl enable --now systemd-journal-gatewayd.socket
```

기본 listen: `http://0.0.0.0:19531`

### API 사용 예

```bash
# 모든 엔트리 (JSON)
curl http://localhost:19531/entries -H "Accept: application/json"

# 필터
curl 'http://localhost:19531/entries?_SYSTEMD_UNIT=nginx.service'

# 부팅 단위
curl 'http://localhost:19531/entries?boot'

# 머신 정보
curl http://localhost:19531/machine
```

내장 웹 UI는 `http://localhost:19531/browse` 접속.

### TLS

```bash
sudo systemctl edit systemd-journal-gatewayd
```

```ini
[Service]
ExecStart=
ExecStart=/usr/lib/systemd/systemd-journal-gatewayd \
  --cert=/etc/ssl/certs/server.crt \
  --key=/etc/ssl/private/server.key \
  --trust=/etc/ssl/certs/ca.crt
```

---

## 중앙 수집 아키텍처

### 패턴 1: journal-remote 클러스터

```
[host1] systemd-journal-upload --→ [logserver] systemd-journal-remote → /var/log/journal/remote/
[host2] systemd-journal-upload --→
[host3] systemd-journal-upload --→
```

장점: 바이너리 형식 그대로, journalctl로 검색 가능
단점: ELK/Loki 같은 도구와 직접 호환 안 됨

### 패턴 2: rsyslog 중앙 + 변환

```
[host1] journald → rsyslog → TCP/TLS → [logserver] rsyslog → 파일/Elasticsearch
```

장점: 텍스트, 광범위한 도구 호환
단점: 구조화된 필드 일부 손실 가능

### 패턴 3: Loki/Vector 등 푸시

```
[host1] journald → vector → Loki/Elastic/S3
```

장점: 풍부한 변환 파이프라인, 라벨링
단점: 추가 에이전트 필요

---

## Loki/Vector 등으로 전송

### Promtail (Loki agent)

`/etc/promtail/config.yml`:

```yaml
clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: journal
    journal:
      json: true
      max_age: 12h
      labels:
        job: systemd-journal
    relabel_configs:
      - source_labels: ['__journal__systemd_unit']
        target_label: 'unit'
      - source_labels: ['__journal__hostname']
        target_label: 'host'
```

### Vector

`/etc/vector/vector.toml`:

```toml
[sources.journal]
type = "journald"
since_now = false

[transforms.parse]
type = "remap"
inputs = ["journal"]
source = '''
  .severity = .PRIORITY
  del(.PRIORITY)
'''

[sinks.loki]
type = "loki"
inputs = ["parse"]
endpoint = "http://loki:3100"
labels.unit = "{{ _SYSTEMD_UNIT }}"
labels.host = "{{ _HOSTNAME }}"
encoding.codec = "json"
```

### Fluent Bit

```ini
[INPUT]
    Name systemd
    Tag  host.*
    Read_From_Tail On

[OUTPUT]
    Name  forward
    Host  log-aggregator
    Port  24224
```

각 도구는 cursor 또는 마지막 처리 시각을 저장하므로, 재시작 후에도 누락 없이 이어서 처리합니다.

---

## 참고 자료

- [man systemd-journal-remote](https://www.freedesktop.org/software/systemd/man/systemd-journal-remote.service.html)
- [man systemd-journal-upload](https://www.freedesktop.org/software/systemd/man/systemd-journal-upload.service.html)
- [man systemd-journal-gatewayd](https://www.freedesktop.org/software/systemd/man/systemd-journal-gatewayd.service.html)
- [Promtail systemd config](https://grafana.com/docs/loki/latest/clients/promtail/configuration/#journal)
- [Vector journald source](https://vector.dev/docs/reference/configuration/sources/journald/)
