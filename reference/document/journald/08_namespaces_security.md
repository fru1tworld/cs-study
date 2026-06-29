# Journal Namespace와 보안

> 원본: https://www.freedesktop.org/software/systemd/man/systemd-journald.service.html#Journal%20Namespaces

---

## 목차

1. [Journal Namespace란?](#journal-namespace란)
2. [Namespace 생성과 사용](#namespace-생성과-사용)
3. [용도와 한계](#용도와-한계)
4. [FSS (Forward Secure Sealing)](#fss-forward-secure-sealing)
5. [무결성 검증](#무결성-검증)
6. [읽기 권한과 ACL](#읽기-권한과-acl)
7. [권한 분리 패턴](#권한-분리-패턴)
8. [참고 자료](#참고-자료)

---

## Journal Namespace란?

systemd 245+ 에서 도입된 기능. **여러 개의 독립된 journald 인스턴스** 를 운영할 수 있게 해줍니다.

기본 namespace는 이름이 비어있고 `/var/log/journal/<machine-id>/` 에 저장. 추가 namespace는:
- 별도 데몬 인스턴스 (`systemd-journald@<name>.service`)
- 별도 디렉터리 (`/var/log/journal/<machine-id>.<name>/`)
- 별도 소켓 (`/run/systemd/journal.<name>/`)

용도:
- 특정 서비스의 로그를 격리해 다른 로그와 섞이지 않게
- namespace별 다른 보존 정책
- 컨테이너의 로그 격리

---

## Namespace 생성과 사용

### 1. 설정 파일

```ini
# /etc/systemd/journald@myapp.conf
[Journal]
Storage=persistent
SystemMaxUse=2G
MaxRetentionSec=1month
```

파일 이름이 `journald@<namespace>.conf` 형식이어야 합니다.

### 2. unit에 namespace 지정

```ini
# /etc/systemd/system/myapp.service
[Service]
ExecStart=/usr/local/bin/myapp
LogNamespace=myapp
```

`LogNamespace=` 가 지정된 unit은:
- 그 namespace의 journal로만 stdout/stderr 전송
- 자동으로 `systemd-journald@myapp.service` 가 활성화

### 3. 조회

```bash
journalctl --namespace=myapp
journalctl --namespace=myapp -u myapp.service -f
```

namespace를 지정하지 않으면 기본 namespace의 로그만 표시됩니다.

### 4. 모든 namespace 동시 조회

```bash
journalctl --namespace='*'
```

---

## 용도와 한계

### 적합한 경우

- **로그 폭주 격리**: 특정 서비스의 폭주 로그가 다른 서비스 로그를 회전시키지 않게
- **다른 보존 정책**: 감사 로그는 6개월, 애플리케이션은 2주 등
- **권한 분리**: namespace별로 다른 그룹이 읽도록
- **컨테이너 호스트**: 호스트와 컨테이너 로그 분리

### 한계

- namespace 간 cursor가 공유되지 않음 → 시간순 정렬에 주의
- 도구(rsyslog, vector 등)가 namespace를 모를 수 있음
- 메모리/디스크 오버헤드: 데몬 인스턴스마다 메모리 사용
- 사용자 namespace는 별도 — `systemd --user` 영역과 다른 개념

---

## FSS (Forward Secure Sealing)

journal 파일의 **무결성을 사후에 검증**할 수 있는 암호학적 기능. 디스크가 공격받거나 백업이 변조되었을 때 변조가 시작된 지점을 탐지할 수 있습니다.

### 작동 원리

1. 서버가 seed key를 생성해 디스크에 저장
2. 이와 쌍을 이루는 verification key를 생성 → 별도 보관 (다른 머신, 종이, 비밀번호 매니저)
3. journald가 일정 간격마다 journal 파일에 sealing tag를 추가
4. tag는 그 시점까지의 모든 엔트리를 해시한 값
5. 시간이 흐르면 seed key가 forward-evolve하여 과거 시점의 tag를 위조할 수 없게 됨
6. 검증 시 verification key로 모든 tag를 재계산해 일치 여부를 확인

> 핵심: 공격자가 서버에 침투해 seed key를 훔쳐도 **이미 sealing된 과거 데이터** 는 변조할 수 없습니다 (forward security).

### 활성화

```ini
# /etc/systemd/journald.conf
[Journal]
Seal=yes
```

### 키 생성

```bash
sudo journalctl --setup-keys
```

출력:
```
The new key pair has been generated. The verify key is:

       abc123-def456-ghi789-jkl012/01a2b3-c4d5e6

The sealing key is automatically changed every 15min.

The keys are stored in /var/log/journal/<machine-id>/fss

Please save the verify key. To watch for journal inconsistencies, run
"journalctl --verify --verify-key=abc123-..."
```

verification key를 안전한 곳(다른 머신, 비밀번호 매니저, 종이)에 보관하세요.

간격 변경:
```bash
sudo journalctl --setup-keys --interval=1h
```

---

## 무결성 검증

### 기본 검증

```bash
sudo journalctl --verify
```

```
PASS: /var/log/journal/abc/system.journal
PASS: /var/log/journal/abc/user-1000.journal
```

### sealing key로 강한 검증

```bash
sudo journalctl --verify --verify-key=abc123-def456-...
```

변조가 있을 경우:
```
FAIL: /var/log/journal/abc/system.journal (Tag/Seal Failure)
```

어느 epoch에서 검증이 실패했는지 표시되어 변조 시점을 좁혀 파악할 수 있습니다.

### 정기 검증

```ini
# /etc/systemd/system/journal-verify.service
[Unit]
Description=Verify journal integrity

[Service]
Type=oneshot
ExecStart=/usr/bin/journalctl --verify --verify-key=...
```

```ini
# /etc/systemd/system/journal-verify.timer
[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target
```

매일 검증을 실행하고 실패 시 알림을 전송하도록 구성할 수 있습니다.

---

## 읽기 권한과 ACL

### 그룹

journald가 사용하는 그룹:

| 그룹 | 권한 |
| --- | --- |
| `systemd-journal` | 모든 journal 읽기 |
| `adm` | ACL로 모든 journal 읽기 (배포판 따라) |
| `wheel` | 일부 배포판에서 |

```bash
$ getfacl /var/log/journal/<machine-id>
# group:adm:r-x
# group:wheel:r-x
```

### 사용자 추가

```bash
sudo usermod -aG systemd-journal alice
# alice 다시 로그인
```

### 자기 user unit 로그

`SplitMode=uid` (기본)면 사용자는 자동으로 자기 user unit의 로그를 읽을 수 있습니다.

### 격리된 namespace 권한

namespace의 journal 디렉터리에 별도 ACL을 적용해 특정 그룹만 읽도록 할 수 있습니다.

```bash
sudo setfacl -m g:audit-readers:rx /var/log/journal.audit
```

---

## 권한 분리 패턴

### 패턴 1: 감사 namespace + 별도 그룹

```ini
# /etc/systemd/journald@audit.conf
[Journal]
Storage=persistent
Seal=yes
MaxRetentionSec=6month
SystemMaxUse=10G
```

```ini
# /etc/systemd/system/audit-collector.service
[Service]
ExecStart=/usr/local/bin/audit-collector
LogNamespace=audit
User=audit
```

```bash
sudo groupadd audit-readers
sudo setfacl -m g:audit-readers:rx /var/log/journal.audit
sudo setfacl -d -m g:audit-readers:rx /var/log/journal.audit  # 새 파일에도 적용
```

`audit-readers` 그룹만 감사 로그를 읽을 수 있으며, 다른 사용자는 일반 journal에만 접근할 수 있습니다.

### 패턴 2: 컨테이너 namespace

각 컨테이너 단위 service에:

```ini
[Service]
LogNamespace=%i
```

(템플릿 unit의 인스턴스 이름을 namespace 식별자로 활용)

### 패턴 3: tampering 탐지

- `Seal=yes`
- 매일 `--verify` 타이머
- verification key는 다른 머신에 보관
- 검증 실패 시 이메일/PagerDuty

---

## 참고 자료

- [man systemd-journald.service — Journal Namespaces](https://www.freedesktop.org/software/systemd/man/systemd-journald.service.html#Journal%20Namespaces)
- [Forward Secure Sealing 페이퍼](https://www.usenix.org/conference/lisa13/technical-sessions/papers/erickson)
- [man journalctl --verify](https://www.freedesktop.org/software/systemd/man/journalctl.html)
