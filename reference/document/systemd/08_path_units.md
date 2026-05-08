# Path Unit

> 이 문서는 `man systemd.path` 의 내용을 한국어로 정리한 것입니다.
> 원본: https://www.freedesktop.org/software/systemd/man/systemd.path.html

---

## 목차

1. [Path란?](#path란)
2. [감시 옵션](#감시-옵션)
3. [Path와 Service의 짝](#path와-service의-짝)
4. [동작 방식](#동작-방식)
5. [실전 예제](#실전-예제)
6. [주의사항](#주의사항)
7. [참고 자료](#참고-자료)

---

## Path란?

`.path` unit은 **파일이나 디렉터리의 변경을 감시**하다가 특정 조건이 만족되면 다른 unit(보통 service)을 활성화합니다. 내부적으로 `inotify` 를 사용합니다.

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

## 감시 옵션

### PathExists

```ini
PathExists=/var/lib/myapp/trigger
```

**경로가 존재하는 동안** 연관 unit을 활성 상태로 유지. 파일이 사라지면 unit도 비활성화.

### PathExistsGlob

```ini
PathExistsGlob=/var/spool/jobs/*.job
```

glob 패턴 매칭. 매칭하는 파일이 하나라도 있으면 활성화.

### PathChanged

```ini
PathChanged=/etc/myapp.conf
```

파일이 **닫힐 때(write 후 close)** 트리거. 가장 자주 쓰는 옵션. 디렉터리를 지정하면 그 안의 파일 변경을 감지.

### PathModified

```ini
PathModified=/var/log/access.log
```

`PathChanged` 와 비슷하지만 close 없이 **write 시점에도** 트리거. 더 빠르지만 같은 변경에 여러 번 발화할 수 있음.

### DirectoryNotEmpty

```ini
DirectoryNotEmpty=/var/incoming
```

디렉터리에 무언가가 있으면 트리거. 처리 큐 패턴에 적합.

> 모든 옵션은 절대 경로여야 하며, 여러 번 지정해 여러 경로를 감시할 수 있습니다.

---

## Path와 Service의 짝

기본적으로 `foo.path` 는 `foo.service` 를 활성화합니다. 다른 이름을 쓰려면 `Unit=` 으로 지정.

```ini
[Path]
PathChanged=/etc/myapp.conf
Unit=myapp-reload.service
```

---

## 동작 방식

1. `.path` unit이 시작되면 systemd가 inotify watch를 등록
2. 변경 이벤트가 오면 연관 unit을 활성화
3. 연관 unit이 종료되면 다시 감시 모드로 복귀

`PathExists` / `DirectoryNotEmpty` 같은 "상태" 기반 옵션은 unit 시작 시 즉시 평가됩니다 — 이미 조건이 맞으면 바로 활성화.

`PathChanged` / `PathModified` 는 "이벤트" 기반이라 unit 시작 후 발생한 변경만 감지합니다.

### MakeDirectory

```ini
[Path]
PathChanged=/var/incoming
MakeDirectory=yes
DirectoryMode=0755
```

감시 대상 디렉터리가 없으면 자동 생성.

---

## 실전 예제

### 1. 새 파일이 들어오면 처리

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

### 2. 설정 변경 시 자동 reload

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

### 3. lock 파일 패턴

```ini
# /etc/systemd/system/maintenance.path
[Unit]
Description=Run maintenance when lock is released

[Path]
PathExists=!/var/lock/maintenance.lock
Unit=maintenance.service
```

> 참고: `!` 부정 연산자는 systemd 252+ 에서 일부 옵션에 사용 가능. 옛 버전에서는 두 개의 path unit으로 표현해야 합니다.

### 4. 인스턴스 템플릿과 결합

`PathChanged=/var/incoming` 에 새 파일이 들어올 때마다 그 파일 이름으로 인스턴스 service를 시작:

```ini
# process@.service
[Service]
Type=oneshot
ExecStart=/usr/local/bin/process %i
```

`.path` unit 자체로는 파일 이름을 service에 전달할 수 없지만, 트리거된 service에서 `inotifywait` 등으로 파일을 직접 가져갈 수 있습니다.

---

## 주의사항

### inotify 큐 한도

너무 많은 파일이 빠르게 변경되면 inotify 이벤트가 누락될 수 있습니다.

```bash
sysctl fs.inotify.max_user_watches
sysctl fs.inotify.max_queued_events
```

### 단발성 이벤트

`PathChanged` 는 하나의 close 이벤트마다 한 번씩 unit을 트리거합니다. 짧은 시간에 여러 변경이 있으면 service가 여러 번 호출될 수 있으므로 service 자체는 idempotent해야 합니다.

### 디렉터리 vs 파일

디렉터리를 `PathChanged` 로 감시하면 그 안의 파일 close가 트리거합니다. 그러나 **하위 디렉터리는 재귀적으로 감시되지 않습니다**. 깊은 트리를 감시하려면 별도의 inotify 도구가 필요합니다.

### NFS와 가상 파일시스템

inotify는 로컬 파일시스템 변경만 감지합니다. NFS 클라이언트에서 다른 클라이언트의 변경은 감지되지 않습니다.

---

## 참고 자료

- [man systemd.path](https://www.freedesktop.org/software/systemd/man/systemd.path.html)
- [man inotify](https://man7.org/linux/man-pages/man7/inotify.7.html)
