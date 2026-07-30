# systemctl과 systemd-analyze

## systemctl 사용법

> 원본: https://www.freedesktop.org/software/systemd/man/systemctl.html

---

### 목차

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

### systemctl이란?

systemd와 통신하는 메인 CLI 도구. 내부적으로 D-Bus(`org.freedesktop.systemd1`)를 통해 PID 1과 통신합니다.

기본 형식:
```
systemctl [OPTIONS] COMMAND [UNIT...]
```

#### 시스템 vs 사용자

- 시스템 manager: `systemctl ...` (root 권한 필요한 경우 다수)
- 사용자 manager: `systemctl --user ...`

---

### Unit 라이프사이클

#### 시작·중지

```bash
sudo systemctl start nginx.service       # 시작
sudo systemctl stop nginx.service        # 중지
sudo systemctl restart nginx.service     # 재시작 (stop+start)
sudo systemctl reload nginx.service      # config reload (SIGHUP 등)
sudo systemctl reload-or-restart nginx.service   # reload 지원하면 reload, 아니면 restart
sudo systemctl try-restart nginx.service # 실행 중일 때만 restart
```

확장자 `.service` 는 생략 가능 (다른 종류와 충돌하지 않을 때).

#### 다중 unit

```bash
sudo systemctl start nginx postgresql redis
```

#### 패턴 매칭

```bash
sudo systemctl restart 'sshd-*.service'
```

---

### 상태 조회

#### 단일 unit 상태

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

#### 활성/실패 unit 목록

```bash
systemctl list-units                     # 활성화된 모든 unit
systemctl list-units --failed            # 실패한 것만
systemctl list-units --type=service      # service만
systemctl list-units --state=active      # 상태별 필터
```

#### 모든 unit 파일 (활성/비활성 무관)

```bash
systemctl list-unit-files
systemctl list-unit-files --type=timer
```

#### 부팅 시 자동 시작될 unit

```bash
systemctl list-unit-files --state=enabled
```

#### 의존성 트리

```bash
systemctl list-dependencies nginx.service
systemctl list-dependencies --reverse nginx.service     # 누가 의존하는지
systemctl list-dependencies --before nginx.service      # before/after
```

#### unit 속성 조회

```bash
systemctl show nginx.service                            # 모든 속성
systemctl show nginx.service -p MainPID -p ActiveState  # 일부만
systemctl show -p Environment nginx.service             # 환경 변수
```

---

### Enable/Disable

부팅 시 자동 시작 여부 제어. `[Install]` 섹션의 심볼릭 링크를 만들거나 제거.

```bash
sudo systemctl enable nginx                 # 부팅 시 시작 등록 (당장은 안 켬)
sudo systemctl enable --now nginx           # 등록 + 즉시 시작
sudo systemctl disable nginx                # 등록 해제 (실행 중이면 그대로)
sudo systemctl disable --now nginx          # 해제 + 즉시 중지
```

#### 활성화 여부 확인

```bash
$ systemctl is-enabled nginx
enabled

$ systemctl is-active nginx
active

$ systemctl is-failed nginx
inactive
```

각각 종료 코드도 셸 스크립트에서 활용 가능.

#### Preset

배포판이 정한 기본 enable/disable 정책. `/usr/lib/systemd/system-preset/` 확인.

```bash
systemctl preset nginx     # preset 정책 적용
systemctl preset-all       # 모든 unit에 적용
```

---

### 편집

#### Drop-in 편집 (권장)

```bash
sudo systemctl edit nginx.service
```

`/etc/systemd/system/nginx.service.d/override.conf` 파일을 편집기로 띄움. 패키지 unit을 직접 건드리지 않으므로 업그레이드에도 살아남음.

#### 전체 unit 편집

```bash
sudo systemctl edit --full nginx.service
```

`/etc/systemd/system/nginx.service` 에 사본을 만들고 편집. 패키지 unit을 완전히 덮어씀.

#### 변경 후 reload

```bash
sudo systemctl daemon-reload
```

unit 파일을 직접 수정한 경우 반드시 호출. `systemctl edit` 은 자동으로 처리.

---

### Mask와 Unmask

unit을 **완전히 비활성화**. `disable` 보다 강함 — 다른 unit이 의존성으로 끌어와도 시작되지 않음.

```bash
sudo systemctl mask cups.service       # /etc/systemd/system/cups.service → /dev/null
sudo systemctl unmask cups.service
```

내부적으로 `/etc/systemd/system/cups.service` 를 `/dev/null` 로 향하는 심볼릭 링크로 만들어, 어떤 수단으로도 해당 unit을 시작할 수 없게 합니다.

언제 쓰나:
- 절대 시작되면 안 되는 서비스
- 디스크 풀, 보안 등 이유로 막아야 할 때

---

### 시스템 제어

#### 종료/재부팅

```bash
sudo systemctl reboot
sudo systemctl poweroff
sudo systemctl halt
sudo systemctl kexec       # kexec 새 커널로 재부팅
sudo systemctl suspend
sudo systemctl hibernate
sudo systemctl hybrid-sleep
```

#### 메시지 동봉

```bash
sudo systemctl reboot -i --message="Kernel update"
```

#### Default target 변경

```bash
systemctl get-default
sudo systemctl set-default multi-user.target
```

#### Isolate

```bash
sudo systemctl isolate multi-user.target
sudo systemctl isolate rescue.target
sudo systemctl isolate emergency.target
```

---

### 원격 제어

```bash
systemctl --host=user@server status nginx
```

내부적으로 SSH를 사용하며, systemctl에 SSH 클라이언트 기능이 내장되어 있습니다.

```bash
systemctl --machine=container-name status nginx    # 컨테이너 안의 systemd
```

---

### 자주 쓰는 패턴

#### 부팅 후 시작 못한 서비스 찾기

```bash
systemctl --failed
systemctl list-units --state=failed
```

#### 서비스가 무엇을 사용 중인지

```bash
systemctl status nginx --no-pager
systemctl show nginx -p CGroup -p MainPID
systemd-cgls /system.slice/nginx.service
```

#### 환경 변수 임시 변경 후 시작

```bash
sudo systemctl set-environment LOG_LEVEL=debug
sudo systemctl restart nginx
sudo systemctl unset-environment LOG_LEVEL
```

#### 일회성 작업 (transient unit)

```bash
sudo systemd-run --unit=oneshot-task --scope --slice=batch.slice \
  -p MemoryMax=1G -p CPUQuota=50% \
  /usr/local/bin/heavy-task.sh
```

서비스 파일 없이 즉석에서 cgroup·격리를 적용하여 명령어를 실행합니다. 백그라운드 작업 처리에 유용합니다.

#### 부팅 분석

```bash
systemd-analyze blame                  # 시간 많이 쓴 unit
systemd-analyze critical-chain         # 부팅 의존성 critical path
systemd-analyze plot > boot.svg
```

#### unit 파일 검증

```bash
systemd-analyze verify /etc/systemd/system/myapp.service
```

문법 오류, 잘못된 의존성 등을 찾아냄.

#### Cat — 모든 fragment 통합 보기

```bash
$ systemctl cat nginx.service
# /usr/lib/systemd/system/nginx.service
[Unit]
...

# /etc/systemd/system/nginx.service.d/override.conf
[Service]
Restart=always
```

원본 unit과 모든 drop-in을 합쳐서 보여줍니다.

---

### 참고 자료

- [man systemctl](https://www.freedesktop.org/software/systemd/man/systemctl.html)
- [man systemd-run](https://www.freedesktop.org/software/systemd/man/systemd-run.html)
- [man systemd-cgls](https://www.freedesktop.org/software/systemd/man/systemd-cgls.html)

---

## systemd-analyze (부팅 분석과 진단)

> 원본: https://www.freedesktop.org/software/systemd/man/systemd-analyze.html

---

### 목차

1. [개요](#개요)
2. [부팅 시간 분석](#부팅-시간-분석)
3. [Critical chain](#critical-chain)
4. [Plot — Gantt 차트](#plot--gantt-차트)
5. [Unit 검증](#unit-검증)
6. [보안 점수](#보안-점수)
7. [캘린더 표현식](#캘린더-표현식)
8. [Cat-config](#cat-config)
9. [기타 유용한 서브커맨드](#기타-유용한-서브커맨드)
10. [참고 자료](#참고-자료)

---

### 개요

`systemd-analyze` 는 systemd 동작을 진단하고 분석하는 도구입니다. 부팅 시간 분석부터 unit 파일 검증, 보안 점수 측정까지 다양한 기능을 제공합니다.

기본 형식:
```
systemd-analyze [SUBCOMMAND] [OPTIONS]
```

서브커맨드 없이 실행하면 `time` 으로 동작.

---

### 부팅 시간 분석

#### time (기본)

```bash
$ systemd-analyze
Startup finished in 1.123s (kernel) + 2.345s (initrd) + 4.567s (userspace) = 8.035s
graphical.target reached after 4.567s in userspace.
```

각 단계의 의미:
- **kernel**: 커널 시작부터 init 실행까지
- **initrd**: 초기 RAM 디스크 처리 (가능한 경우)
- **userspace**: PID 1 시작부터 default target 도달까지

#### blame — unit별 소요 시간

```bash
$ systemd-analyze blame
3.502s NetworkManager-wait-online.service
1.234s docker.service
  856ms postgresql.service
  423ms apparmor.service
  ...
```

가장 오래 걸린 unit부터 정렬. 부팅 시간 단축 작업의 출발점으로 활용할 수 있습니다.

> 주의: 병렬로 실행되는 unit이 많으므로 blame 시간을 단순히 합치면 실제 부팅 시간과 다릅니다. 진짜 critical path는 `critical-chain` 에서 봐야 합니다.

---

### Critical chain

```bash
$ systemd-analyze critical-chain
The time when unit became active or started is printed after the "@" character.
The time the unit took to start is printed after the "+" character.

graphical.target @4.567s
└─multi-user.target @4.567s
  └─docker.service @3.333s +1.234s
    └─containerd.service @3.300s +33ms
      └─basic.target @3.299s
        └─sockets.target @3.299s
          └─dbus.socket @3.299s
            └─sysinit.target @3.298s
              └─...
```

각 노드는 `@시점 +지속시간` 형식. 진짜 부팅 지연의 critical path를 보여줍니다. 이 경로 위의 unit을 최적화하지 않으면 부팅이 빨라지지 않습니다.

특정 unit의 critical-chain만 보기:
```bash
systemd-analyze critical-chain nginx.service
```

---

### Plot — Gantt 차트

```bash
systemd-analyze plot > boot.svg
xdg-open boot.svg
```

부팅 과정의 시각적 Gantt 차트를 SVG로 출력. 각 unit이 언제 시작되어 언제 활성화됐는지 한눈에 보입니다. 병렬 부팅 패턴을 이해하는 데 가장 좋은 도구.

---

### Unit 검증

#### verify

작성한 unit 파일의 문법과 의존성을 검사.

```bash
$ systemd-analyze verify /etc/systemd/system/myapp.service
/etc/systemd/system/myapp.service:5: Unknown section 'Servic'. Ignoring.
myapp.service: Service has no ExecStart=, ExecStop=, or SuccessAction=. Refusing.
```

CI 파이프라인에서 unit 파일을 머지 전에 검증하기에 적합.

#### 환경 변수 영향 확인

```bash
systemd-analyze verify --root=/path/to/test myapp.service
```

---

### 보안 점수

#### security

특정 unit의 보안 노출 정도를 0~10점으로 평가 (낮을수록 안전).

```bash
$ systemd-analyze security nginx.service
  NAME                                                  DESCRIPTION                                                       EXPOSURE
✗ User=/DynamicUser=                                    Service runs as root user                                              0.4
✓ SupplementaryGroups=                                  Service has no supplementary groups
✗ PrivateDevices=                                       Service potentially has access to hardware devices                      0.2
✗ PrivateNetwork=                                       Service has access to the host's network                                0.5
✓ ProtectClock=                                         Service cannot write to the hardware clock or system clock
✗ ProtectHome=                                          Service has full access to home directories                             0.2
✗ ProtectSystem=                                        Service has full access to the OS file hierarchy                        0.2
...
→ Overall exposure level for nginx.service: 6.5 MEDIUM 🙂
```

#### 모든 서비스 점수

```bash
$ systemd-analyze security
UNIT                            EXPOSURE PREDICATE HAPPY
nginx.service                        6.5 MEDIUM    🙂
postgresql.service                   8.7 EXPOSED   😨
sshd.service                         9.6 UNSAFE    😨
my-hardened-app.service              1.4 OK        😀
```

서비스 하드닝의 좋은 출발점.

#### 항목별 차이 보여주기

```bash
systemd-analyze security --no-pager nginx.service | less
```

각 옵션을 켰을 때 점수가 어떻게 바뀌는지 비교하면서 점진적으로 개선 가능.

---

### 캘린더 표현식

`OnCalendar=` 표현식이 다음 실행 시점을 어떻게 해석하는지 검증.

```bash
$ systemd-analyze calendar "Mon..Fri 09:00"
  Original form: Mon..Fri 09:00
Normalized form: Mon..Fri *-*-* 09:00:00
    Next elapse: Mon 2026-05-12 09:00:00 KST
       From now: 4 days left
```

여러 번의 실행 시점을 미리 보기:
```bash
$ systemd-analyze calendar --iterations=5 "*-*-* 03:00:00"
  Original form: *-*-* 03:00:00
Normalized form: *-*-* 03:00:00
    Next elapse: Sat 2026-05-09 03:00:00 KST
                 (next 5 iterations)
                 Sun 2026-05-10 03:00:00 KST
                 Mon 2026-05-11 03:00:00 KST
                 Tue 2026-05-12 03:00:00 KST
                 Wed 2026-05-13 03:00:00 KST
```

#### 시간 단위 검증

```bash
$ systemd-analyze timespan "1h 30min 45s"
Original: 1h 30min 45s
      μs: 5445000000
   Human: 1h 30min 45s
```

---

### Cat-config

여러 위치에 흩어진 설정 파일을 한꺼번에 보기.

```bash
systemd-analyze cat-config systemd/system.conf
systemd-analyze cat-config systemd/journald.conf
systemd-analyze cat-config systemd/network/eth0.network
```

기본 설정 + drop-in을 모두 합쳐 출력. 디버깅에 유용.

---

### 기타 유용한 서브커맨드

#### dot — 의존성 그래프

```bash
systemd-analyze dot --to-pattern='*.target' --from-pattern='*.target' | dot -Tsvg > targets.svg
```

unit 의존성을 Graphviz dot 형식으로 출력. 파이프해서 시각화.

#### dump

systemd 내부 상태 전체 덤프.

```bash
systemd-analyze dump > systemd-state.txt
```

매우 큰 출력. 이슈 리포트에 첨부할 때 사용.

#### exit-status

종료 코드 의미 설명.

```bash
$ systemd-analyze exit-status 217
NAME                       STATUS  CLASS
EXIT_USER                  217     systemd
```

서비스가 알 수 없는 종료 코드로 실패했을 때 사용.

#### syscall-filter

syscall 필터 그룹의 내용 확인.

```bash
systemd-analyze syscall-filter @system-service
```

`SystemCallFilter=` 옵션을 작성할 때 어떤 syscall이 포함되는지 확인 가능.

#### condition

조건식 평가.

```bash
$ systemd-analyze condition 'ConditionACPower=true'
test.service: Conditions succeeded.
```

#### unit-files

unit 검색 경로와 파일 목록 표시.

```bash
systemd-analyze unit-files
systemd-analyze unit-paths
```

#### service-watchdogs

워치독 사용 여부 확인.

```bash
systemd-analyze service-watchdogs
```

---

### 참고 자료

- [man systemd-analyze](https://www.freedesktop.org/software/systemd/man/systemd-analyze.html)
- [Boot time optimization with systemd](http://0pointer.de/blog/projects/blame-game.html)
