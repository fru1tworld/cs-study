# systemctl 사용법

> 이 문서는 `man systemctl` 의 내용을 한국어로 정리한 것입니다.
> 원본: https://www.freedesktop.org/software/systemd/man/systemctl.html

---

## 목차

1. [systemctl이란?](#systemctl이란)
2. [Unit 라이프사이클](#unit-라이프사이클)
3. [상태 조회](#상태-조회)
4. [Enable/Disable](#enabledisable)
5. [편집](#편집)
6. [Mask와 Unmask](#mask와-unmask)
7. [시스템 제어](#시스템-제어)
8. [원격 제어](#원격-제어)
9. [자주 쓰는 패턴](#자주-쓰는-패턴)
10. [참고 자료](#참고-자료)

---

## systemctl이란?

systemd와 통신하는 메인 CLI 도구. 내부적으로 D-Bus(`org.freedesktop.systemd1`)를 통해 PID 1과 통신합니다.

기본 형식:
```
systemctl [OPTIONS] COMMAND [UNIT...]
```

### 시스템 vs 사용자

- 시스템 manager: `systemctl ...` (root 권한 필요한 경우 다수)
- 사용자 manager: `systemctl --user ...`

---

## Unit 라이프사이클

### 시작·중지

```bash
sudo systemctl start nginx.service       # 시작
sudo systemctl stop nginx.service        # 중지
sudo systemctl restart nginx.service     # 재시작 (stop+start)
sudo systemctl reload nginx.service      # config reload (SIGHUP 등)
sudo systemctl reload-or-restart nginx.service   # reload 지원하면 reload, 아니면 restart
sudo systemctl try-restart nginx.service # 실행 중일 때만 restart
```

확장자 `.service` 는 생략 가능 (다른 종류와 충돌하지 않을 때).

### 다중 unit

```bash
sudo systemctl start nginx postgresql redis
```

### 패턴 매칭

```bash
sudo systemctl restart 'sshd-*.service'
```

---

## 상태 조회

### 단일 unit 상태

```bash
$ systemctl status nginx
● nginx.service - A high performance web server
     Loaded: loaded (/usr/lib/systemd/system/nginx.service; enabled; preset: enabled)
     Active: active (running) since Fri 2026-05-08 10:23:11 KST; 2h ago
   Main PID: 1234 (nginx)
      Tasks: 5 (limit: 4915)
     Memory: 12.5M
        CPU: 234ms
     CGroup: /system.slice/nginx.service
             ├─1234 nginx: master process /usr/sbin/nginx
             └─1235 nginx: worker process

May 08 10:23:11 host systemd[1]: Started A high performance web server.
May 08 10:23:11 host nginx[1234]: nginx: started.
```

핵심 정보:
- **Loaded**: unit 파일 위치, enable 여부, vendor preset
- **Active**: 현재 상태
- **Main PID**: 메인 프로세스
- **CGroup**: 이 unit의 cgroup과 모든 자식 프로세스
- **로그 마지막 줄**: journalctl에서 가져옴

### 활성/실패 unit 목록

```bash
systemctl list-units                     # 활성화된 모든 unit
systemctl list-units --failed            # 실패한 것만
systemctl list-units --type=service      # service만
systemctl list-units --state=active      # 상태별 필터
```

### 모든 unit 파일 (활성/비활성 무관)

```bash
systemctl list-unit-files
systemctl list-unit-files --type=timer
```

### 부팅 시 자동 시작될 unit

```bash
systemctl list-unit-files --state=enabled
```

### 의존성 트리

```bash
systemctl list-dependencies nginx.service
systemctl list-dependencies --reverse nginx.service     # 누가 의존하는지
systemctl list-dependencies --before nginx.service      # before/after
```

### unit 속성 조회

```bash
systemctl show nginx.service                            # 모든 속성
systemctl show nginx.service -p MainPID -p ActiveState  # 일부만
systemctl show -p Environment nginx.service             # 환경 변수
```

---

## Enable/Disable

부팅 시 자동 시작 여부 제어. `[Install]` 섹션의 심볼릭 링크를 만들거나 제거.

```bash
sudo systemctl enable nginx                 # 부팅 시 시작 등록 (당장은 안 켬)
sudo systemctl enable --now nginx           # 등록 + 즉시 시작
sudo systemctl disable nginx                # 등록 해제 (실행 중이면 그대로)
sudo systemctl disable --now nginx          # 해제 + 즉시 중지
```

### 활성화 여부 확인

```bash
$ systemctl is-enabled nginx
enabled

$ systemctl is-active nginx
active

$ systemctl is-failed nginx
inactive
```

각각 종료 코드도 셸 스크립트에서 활용 가능.

### Preset

배포판이 정한 기본 enable/disable 정책. `/usr/lib/systemd/system-preset/` 확인.

```bash
systemctl preset nginx     # preset 정책 적용
systemctl preset-all       # 모든 unit에 적용
```

---

## 편집

### Drop-in 편집 (권장)

```bash
sudo systemctl edit nginx.service
```

`/etc/systemd/system/nginx.service.d/override.conf` 파일을 편집기로 띄움. 패키지 unit을 직접 건드리지 않으므로 업그레이드에도 살아남음.

### 전체 unit 편집

```bash
sudo systemctl edit --full nginx.service
```

`/etc/systemd/system/nginx.service` 에 사본을 만들고 편집. 패키지 unit을 완전히 덮어씀.

### 변경 후 reload

```bash
sudo systemctl daemon-reload
```

unit 파일을 직접 수정한 경우 반드시 호출. `systemctl edit` 은 자동으로 처리.

---

## Mask와 Unmask

unit을 **완전히 비활성화**. `disable` 보다 강함 — 다른 unit이 의존성으로 끌어와도 시작되지 않음.

```bash
sudo systemctl mask cups.service       # /etc/systemd/system/cups.service → /dev/null
sudo systemctl unmask cups.service
```

내부적으로 `/etc/systemd/system/cups.service` 를 `/dev/null` 로 향하는 심볼릭 링크로 만듭니다. 따라서 그 어떤 ExecStart도 실행되지 않습니다.

언제 쓰나:
- 절대 시작되면 안 되는 서비스
- 디스크 풀, 보안 등 이유로 막아야 할 때

---

## 시스템 제어

### 종료/재부팅

```bash
sudo systemctl reboot
sudo systemctl poweroff
sudo systemctl halt
sudo systemctl kexec       # kexec 새 커널로 재부팅
sudo systemctl suspend
sudo systemctl hibernate
sudo systemctl hybrid-sleep
```

### 메시지 동봉

```bash
sudo systemctl reboot -i --message="Kernel update"
```

### Default target 변경

```bash
systemctl get-default
sudo systemctl set-default multi-user.target
```

### Isolate

```bash
sudo systemctl isolate multi-user.target
sudo systemctl isolate rescue.target
sudo systemctl isolate emergency.target
```

---

## 원격 제어

```bash
systemctl --host=user@server status nginx
```

내부적으로 SSH 사용. systemctl 자체에 SSH 클라이언트 기능 내장.

```bash
systemctl --machine=container-name status nginx    # 컨테이너 안의 systemd
```

---

## 자주 쓰는 패턴

### 부팅 후 시작 못한 서비스 찾기

```bash
systemctl --failed
systemctl list-units --state=failed
```

### 서비스가 무엇을 사용 중인지

```bash
systemctl status nginx --no-pager
systemctl show nginx -p CGroup -p MainPID
systemd-cgls /system.slice/nginx.service
```

### 환경 변수 임시 변경 후 시작

```bash
sudo systemctl set-environment LOG_LEVEL=debug
sudo systemctl restart nginx
sudo systemctl unset-environment LOG_LEVEL
```

### 일회성 작업 (transient unit)

```bash
sudo systemd-run --unit=oneshot-task --scope --slice=batch.slice \
  -p MemoryMax=1G -p CPUQuota=50% \
  /usr/local/bin/heavy-task.sh
```

서비스 파일 없이 즉석에서 cgroup·격리를 적용해 명령어 실행. 백그라운드 작업 처리에 유용.

### 부팅 분석

```bash
systemd-analyze blame                  # 시간 많이 쓴 unit
systemd-analyze critical-chain         # 부팅 의존성 critical path
systemd-analyze plot > boot.svg
```

### unit 파일 검증

```bash
systemd-analyze verify /etc/systemd/system/myapp.service
```

문법 오류, 잘못된 의존성 등을 찾아냄.

### Cat — 모든 fragment 통합 보기

```bash
$ systemctl cat nginx.service
# /usr/lib/systemd/system/nginx.service
[Unit]
...

# /etc/systemd/system/nginx.service.d/override.conf
[Service]
Restart=always
```

원본 unit과 모든 drop-in을 합쳐서 보여줌.

---

## 참고 자료

- [man systemctl](https://www.freedesktop.org/software/systemd/man/systemctl.html)
- [man systemd-run](https://www.freedesktop.org/software/systemd/man/systemd-run.html)
- [man systemd-cgls](https://www.freedesktop.org/software/systemd/man/systemd-cgls.html)
