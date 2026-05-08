# Mount와 Automount Unit

> 이 문서는 `man systemd.mount` , `man systemd.automount` 의 내용을 한국어로 정리한 것입니다.
> 원본: https://www.freedesktop.org/software/systemd/man/systemd.mount.html , https://www.freedesktop.org/software/systemd/man/systemd.automount.html

---

## 목차

1. [Mount unit](#mount-unit)
2. [fstab과의 관계](#fstab과의-관계)
3. [Mount 옵션](#mount-옵션)
4. [Automount unit](#automount-unit)
5. [Idle Timeout](#idle-timeout)
6. [실전 예제](#실전-예제)
7. [참고 자료</](#참고-자료)

---

## Mount unit

`.mount` unit은 파일시스템 마운트를 표현합니다. unit 이름은 마운트 지점을 이스케이프한 것 — 슬래시는 `-` 로, 다른 특수 문자는 `\xNN` 으로.

| 마운트 지점 | unit 이름 |
| --- | --- |
| `/` | `-.mount` |
| `/home` | `home.mount` |
| `/var/lib/docker` | `var-lib-docker.mount` |
| `/mnt/data 1` | `mnt-data\x201.mount` |

이스케이프는 손으로 하기 어려우니 `systemd-escape` 를 씁니다:

```bash
$ systemd-escape --path /var/lib/docker
var-lib-docker
$ systemd-escape --path --suffix=mount /var/lib/docker
var-lib-docker.mount
```

---

## fstab과의 관계

systemd는 부팅 시 `/etc/fstab` 을 읽어 **자동으로 mount unit을 생성**합니다. 즉 대부분의 경우 `.mount` 파일을 직접 작성할 필요는 없고 fstab만 쓰면 됩니다.

```fstab
# <device>      <mountpoint>    <fstype>  <options>                          <dump> <pass>
UUID=abc-123    /data           xfs       defaults,nofail,x-systemd.device-timeout=30  0      2
//srv/share     /mnt/share      cifs      _netdev,credentials=/etc/smbcred   0      0
```

### x-systemd.* 옵션

fstab의 옵션 필드에 systemd 전용 옵션을 넣을 수 있습니다.

| 옵션 | 의미 |
| --- | --- |
| `x-systemd.device-timeout=30` | 디바이스 대기 타임아웃 |
| `x-systemd.requires=foo.service` | 이 mount의 Requires= |
| `x-systemd.before=bar.service` | 이 mount Before= |
| `x-systemd.after=baz.service` | 이 mount After= |
| `x-systemd.automount` | 자동으로 automount unit 생성 |
| `x-systemd.idle-timeout=600` | automount idle timeout |
| `x-systemd.mount-timeout=10` | mount() 타임아웃 |
| `_netdev` | 네트워크 마운트 (네트워크 이후) |
| `nofail` | 실패해도 부팅 계속 |
| `noauto` | 부팅 시 자동 마운트 안 함 |

`fstab` 변경 후 즉시 반영하려면:

```bash
sudo systemctl daemon-reload
sudo systemctl restart local-fs.target   # 주의: 마운트 재배치가 발생
```

---

## Mount 옵션

직접 `.mount` 파일을 쓸 때:

```ini
# /etc/systemd/system/srv-data.mount
[Unit]
Description=Data Volume
Requires=network-online.target
After=network-online.target

[Mount]
What=//fileserver/data
Where=/srv/data
Type=cifs
Options=credentials=/etc/cifs.cred,_netdev
TimeoutSec=30

[Install]
WantedBy=multi-user.target
```

### 핵심 지시자

- `What=`: 디바이스나 원격 위치 (예: `/dev/sda1`, `UUID=...`, `LABEL=...`, `//host/share`)
- `Where=`: 마운트 지점 (unit 이름과 일치해야 함)
- `Type=`: 파일시스템 종류 (`ext4`, `xfs`, `nfs`, `cifs`, `tmpfs`, ...)
- `Options=`: 마운트 옵션 (콤마 구분)
- `SloppyOptions=yes`: 알 수 없는 옵션 무시
- `LazyUnmount=yes`: `umount -l` 사용
- `ForceUnmount=yes`: `umount -f` 사용
- `ReadWriteOnly=yes`: read-only 자동 fallback 비활성화
- `TimeoutSec=`: mount 타임아웃

### 의존성 자동 추가

systemd는 다음을 자동으로 추가합니다:
- `Before=local-fs.target` (또는 네트워크 마운트는 `remote-fs.target`)
- `After=` 디바이스가 등장한 시점
- `Requires=` 디바이스 unit

---

## Automount unit

`.automount` unit은 마운트 지점을 **참조하는 순간** 마운트를 트리거합니다. 사용하지 않을 때는 마운트 해제 — NFS나 CIFS처럼 사용 빈도가 낮은 마운트에 유용.

작동 방식:
- automount만 부팅 시 활성화
- 누군가 마운트 지점에 접근 (`ls /srv/data`)
- 커널이 systemd에 알림
- systemd가 mount unit 활성화
- 마운트 완료 후 원래 호출 진행

```ini
# /etc/systemd/system/srv-data.automount
[Unit]
Description=Automount for /srv/data

[Automount]
Where=/srv/data
TimeoutIdleSec=600

[Install]
WantedBy=multi-user.target
```

이 automount는 같은 이름의 `srv-data.mount` 와 짝이 됩니다.

> 주의: automount unit만 enable하면 됩니다. mount unit은 enable하지 마세요.

---

## Idle Timeout

```ini
TimeoutIdleSec=10min
```

마지막 접근 후 이 시간이 지나면 자동 unmount. 0 이면 비활성화 (수동 unmount만).

NFS 마운트가 많은 서버에서 idle timeout을 짧게 두면 메모리/소켓 부담이 줄어듭니다.

---

## 실전 예제

### 1. fstab 한 줄로 NFS automount

```fstab
nfsserver:/data  /mnt/data  nfs  _netdev,x-systemd.automount,x-systemd.idle-timeout=300  0  0
```

이 한 줄로 systemd가 mount + automount 두 unit을 자동 생성합니다.

### 2. tmpfs 마운트

```ini
# /etc/systemd/system/var-cache-myapp.mount
[Unit]
Description=tmpfs for myapp cache
DefaultDependencies=no
Conflicts=umount.target
Before=local-fs.target umount.target

[Mount]
What=tmpfs
Where=/var/cache/myapp
Type=tmpfs
Options=size=512M,mode=0755,uid=myapp,gid=myapp

[Install]
WantedBy=local-fs.target
```

### 3. 의존성 있는 마운트

```ini
[Unit]
Requires=vpn.service
After=vpn.service

[Mount]
What=//office.local/share
Where=/mnt/office
Type=cifs
Options=credentials=/etc/cifs.cred
```

VPN이 올라간 뒤에만 마운트 시도.

### 4. 네트워크 디스크 자동 마운트 (사용자별)

`~/.config/systemd/user/mnt-cloud.mount` 와 `mnt-cloud.automount` 를 만들고:

```bash
systemctl --user enable --now mnt-cloud.automount
```

---

## 관리 명령

### 현재 마운트 보기

```bash
systemctl list-units --type=mount
findmnt
```

### 강제 unmount

```bash
sudo systemctl stop srv-data.mount
```

### 디버깅

```bash
journalctl -u srv-data.mount
journalctl -u srv-data.automount
```

### fstab 변경 즉시 반영

```bash
sudo systemctl daemon-reload
sudo mount -a   # fstab 기반 마운트만
```

---

## 참고 자료

- [man systemd.mount](https://www.freedesktop.org/software/systemd/man/systemd.mount.html)
- [man systemd.automount](https://www.freedesktop.org/software/systemd/man/systemd.automount.html)
- [man systemd-fstab-generator](https://www.freedesktop.org/software/systemd/man/systemd-fstab-generator.html)
