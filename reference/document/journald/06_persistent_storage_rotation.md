# 영구 저장과 로테이션

---

## 목차

1. [저장 모드 선택](#저장-모드-선택)
2. [영구 저장 활성화](#영구-저장-활성화)
3. [디렉터리 구조](#디렉터리-구조)
4. [로테이션이 일어나는 조건](#로테이션이-일어나는-조건)
5. [Vacuum](#vacuum)
6. [용량 산정](#용량-산정)
7. [모니터링](#모니터링)
8. [트러블슈팅](#트러블슈팅)
9. [참고 자료](#참고-자료)

---

## 저장 모드 선택

`journald.conf` 의 `Storage=` 설정:

| 값 | 위치 | 재부팅 시 |
| --- | --- | --- |
| `volatile` | `/run/log/journal/` (tmpfs) | 사라짐 |
| `persistent` | `/var/log/journal/` | 유지 |
| `auto` (기본) | 디렉터리 존재 시 disk, 없으면 RAM | 디렉터리 유지 시 |
| `none` | 저장 안 함 | — |

대부분의 서버는 `persistent` 또는 `auto` + `/var/log/journal/` 생성.

---

## 영구 저장 활성화

### 방법 1: 디렉터리 생성 (auto 모드)

```bash
sudo mkdir -p /var/log/journal
sudo systemd-tmpfiles --create --prefix /var/log/journal
sudo systemctl restart systemd-journald
```

`systemd-tmpfiles` 가 적절한 권한(`2755`, `root:systemd-journal`)을 자동으로 설정합니다.

### 방법 2: 명시적 설정

```ini
# /etc/systemd/journald.conf.d/storage.conf
[Journal]
Storage=persistent
```

```bash
sudo systemctl restart systemd-journald
```

### 권한

- `/var/log/journal/` : `2755` (setgid, group=`systemd-journal`)
- 안의 파일: `0640`, owner=root, group=`systemd-journal`
- `adm` 그룹은 ACL로 추가 읽기 권한

```bash
$ ls -ld /var/log/journal
drwxr-sr-x+ 3 root systemd-journal 4096 May  8 09:00 /var/log/journal

$ getfacl /var/log/journal
# file: var/log/journal
# owner: root
# group: systemd-journal
user::rwx
group::r-x
group:adm:r-x
other::r-x
```

---

## 디렉터리 구조

```
/var/log/journal/
└── <machine-id>/
    ├── system.journal               # 활성 시스템 파일
    ├── user-1000.journal            # 활성 사용자별 파일 (SplitMode=uid)
    ├── system@<seqnum-realtime>.journal     # 회전된 (archived) 파일
    ├── user-1000@<seqnum-realtime>.journal
    └── ...
```

### machine-id

`/etc/machine-id` 에 저장된 32자리 hex 값으로, systemd 최초 부팅 시 생성됩니다. 같은 머신의 모든 journal은 이 ID로 묶입니다.

```bash
cat /etc/machine-id
hostnamectl status
```

### 활성 vs Archived 파일

| 종류 | 이름 | 동작 |
| --- | --- | --- |
| 활성 (active) | `system.journal` 등 | 현재 쓰기 중 |
| Archived | `system@xxx.journal` 등 | 회전돼 더 이상 쓰지 않음, 읽기 전용 |

`journalctl` 은 두 종류 모두 자동으로 읽습니다.

---

## 로테이션이 일어나는 조건

다음 중 하나라도 해당되면 활성 파일이 archived로 전환되고 새 활성 파일이 생성됩니다:

### 1. 파일 크기

```ini
SystemMaxFileSize=128M
RuntimeMaxFileSize=64M
```

기본은 `SystemMaxUse` 의 1/8.

### 2. 시간

```ini
MaxFileSec=1month
```

### 3. 매니저 재시작

```bash
sudo systemctl restart systemd-journald
sudo journalctl --rotate
```

### 4. 시스템 재부팅

새 부팅 ID에서는 보통 새 파일이 만들어집니다.

### 5. 파일 손상 감지

journald가 손상을 감지하면 즉시 회전.

---

## Vacuum

오래된/큰 archived 파일을 삭제하는 작업.

### 자동 vacuum

한도를 초과하면 journald가 자동으로 가장 오래된 archived 파일부터 삭제합니다. 활성 파일은 삭제되지 않습니다.

한도:
- `SystemMaxUse=`: 총 사용량
- `SystemKeepFree=`: 비워둘 디스크 공간
- `MaxRetentionSec=`: 보존 기간
- `SystemMaxFiles=`: 파일 개수

### 수동 vacuum

```bash
sudo journalctl --vacuum-size=500M     # 합계 500MB 이하로
sudo journalctl --vacuum-time=2weeks   # 2주 넘은 것 삭제
sudo journalctl --vacuum-files=10      # 최신 10개 파일만
```

수동 vacuum은 활성 파일에 영향을 주지 않습니다. 즉시 효과를 보려면 먼저 회전을 수행합니다:

```bash
sudo journalctl --rotate
sudo journalctl --vacuum-size=500M
```

---

## 용량 산정

### 기본값

설정을 명시하지 않으면 systemd가 다음과 같이 자동으로 결정합니다:

- `SystemMaxUse=` = `min(디스크 용량의 10%, 4G)`
- `SystemKeepFree=` = `min(디스크 용량의 15%, 4G)`
- `SystemMaxFileSize=` = `SystemMaxUse / 8`

### 실제 사용량 확인

```bash
$ journalctl --disk-usage
Archived and active journals take up 1.2G in the file system.
```

### 디렉터리 크기

```bash
du -sh /var/log/journal/
du -sh /run/log/journal/
```

### 압축률 보기

journal은 LZ4로 압축되며, 일반적으로 텍스트 크기의 1/3~1/5로 줄어듭니다. 반복성이 높은 syslog는 압축률이 더 좋습니다.

### 권장 설정

서버 규모별 가이드라인:

```ini
# 작은 서버 (single-app, < 1GB/day)
[Journal]
SystemMaxUse=1G
SystemMaxFileSize=64M
MaxRetentionSec=2week

# 중간 (~10GB/day, 일주일 보존)
[Journal]
SystemMaxUse=10G
SystemMaxFileSize=256M
MaxRetentionSec=1week

# 대형 (~100GB/day, 별도 수집기로 forward)
[Journal]
Storage=volatile        # RAM만 (전송 후 잊음)
RuntimeMaxUse=512M
ForwardToSyslog=yes     # rsyslog → 중앙 수집기
```

---

## 모니터링

### 디스크 사용량

```bash
journalctl --disk-usage
df -h /var/log
```

### 회전 발생 여부

```bash
ls -laht /var/log/journal/*/
journalctl --header | grep -A2 "File: "
```

### 마지막 vacuum 시각

journald가 자체 로그로 남깁니다:

```bash
journalctl _SYSTEMD_UNIT=systemd-journald.service -g "Vacuum"
```

### Lost messages

rate limit이나 디스크 풀로 손실된 메시지 확인:

```bash
journalctl _SYSTEMD_UNIT=systemd-journald.service -g "Suppressed|missed"
```

---

## 트러블슈팅

### "No journal files were found"

```bash
$ journalctl
No journal files were found.
```

원인:
- `Storage=none`
- `/var/log/journal/` 권한 문제
- journald가 실행 중이 아님

확인:
```bash
sudo systemctl status systemd-journald
ls -ld /var/log/journal
sudo journalctl --verify
```

### Journal corrupted

```bash
$ journalctl
Failed to open ...: File corrupted
```

대응:
```bash
sudo journalctl --verify
sudo systemctl stop systemd-journald
sudo mv /var/log/journal/<machine-id>/system.journal /tmp/corrupt.journal
sudo systemctl start systemd-journald
```

손상된 파일은 백업해 두고 새 파일로 시작합니다.

### 디스크 풀

journald는 `SystemKeepFree=` 에 지정된 공간을 항상 확보하려 하지만, 다른 프로세스가 디스크를 채우면 한계가 있습니다.

```bash
sudo journalctl --vacuum-size=100M
df -h /var/log
```

근본적인 해결은 `SystemMaxUse=` 를 작게 설정하고 외부 수집기로 forward하는 방식을 사용합니다.

### 권한 거부

```bash
$ journalctl
Hint: You are currently not seeing messages from other users and the system.
      Users in the 'systemd-journal' group can see all messages.
```

해결:
```bash
sudo usermod -aG systemd-journal $USER
# 다시 로그인
```

### 너무 빨리 회전

`SystemMaxFileSize` 가 너무 작아 매분 회전이 발생한다면 값을 늘립니다:

```ini
[Journal]
SystemMaxFileSize=256M
```

### archived 파일이 안 지워짐

`SystemMaxUse=` 를 설정하지 않고 `SystemKeepFree=` 만 설정한 경우, 디스크 여유 공간이 충분하면 vacuum이 발생하지 않습니다. 명시적인 상한을 설정하세요.

---

## 참고 자료

- [man journald.conf](https://www.freedesktop.org/software/systemd/man/journald.conf.html)
- [man systemd-tmpfiles](https://www.freedesktop.org/software/systemd/man/systemd-tmpfiles.html)
