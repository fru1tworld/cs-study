# systemctl과 systemd-analyze

## systemctl 사용법

> 원본: https://www.freedesktop.org/software/systemd/man/systemctl.html

<a id="systemctl이란"></a>
### systemctl이란?

`systemctl`은 systemd를 제어하는 기본 CLI 도구다. 내부적으로 D-Bus(`org.freedesktop.systemd1`)를 통해 PID 1과 통신한다.

- 기본 형식:

```
systemctl [OPTIONS] COMMAND [UNIT...]
```

#### 시스템 vs 사용자

- 시스템 manager: `systemctl ...` (root 권한 필요한 경우 다수)
- 사용자 manager: `systemctl --user ...`

<a id="unit-라이프사이클"></a>

### Unit 라이프사이클

#### 시작, 중지

```bash
sudo systemctl start nginx.service       # 시작
sudo systemctl stop nginx.service        # 중지
sudo systemctl restart nginx.service     # 재시작 (stop+start)
sudo systemctl reload nginx.service      # config reload (SIGHUP 등)
sudo systemctl reload-or-restart nginx.service   # reload 지원하면 reload, 아니면 restart
sudo systemctl try-restart nginx.service # 실행 중일 때만 restart
```

- 확장자 `.service` 는 생략 가능 (다른 종류와 충돌하지 않을 때).

#### 다중 unit

```bash
sudo systemctl start nginx postgresql redis
```

#### 패턴 매칭

```bash
sudo systemctl restart 'sshd-*.service'
```

<a id="상태-조회"></a>

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

- 핵심 정보:
- Loaded: unit 파일 위치, enable 여부, vendor preset
- Active: 현재 상태
- Main PID: 메인 프로세스
- CGroup: 이 unit의 cgroup과 모든 자식 프로세스
- 로그 마지막 줄: journalctl에서 가져옴

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

<a id="enabledisable"></a>

### Enable/Disable

`enable`과 `disable`은 `[Install]` 섹션에 따라 심볼릭 링크를 만들거나 제거해 부팅 시 자동 시작 여부를 제어한다. 아래 예제에서 `--now`를 함께 쓰면 현재 실행 상태도 바꾼다.

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

- 각각 종료 코드도 셸 스크립트에서 활용 가능

#### Preset

- 배포판이 정한 기본 enable/disable 정책
  - `/usr/lib/systemd/system-preset/` 확인

```bash
systemctl preset nginx     # preset 정책 적용
systemctl preset-all       # 모든 unit에 적용
```

<a id="편집"></a>

### 편집

#### Drop-in 편집 (권장)

```bash
sudo systemctl edit nginx.service
```

이 명령은 `/etc/systemd/system/nginx.service.d/override.conf` 파일을 편집기로 연다. 패키지의 unit 파일을 직접 수정하지 않으므로, 패키지를 업그레이드해도 변경 사항이 남는다.

#### 전체 unit 편집

```bash
sudo systemctl edit --full nginx.service
```

- `/etc/systemd/system/nginx.service` 에 사본을 만들고 편집
  - 패키지 unit을 완전히 덮어씀

#### 변경 후 reload

```bash
sudo systemctl daemon-reload
```

- unit 파일을 직접 수정한 경우 반드시 호출
  - `systemctl edit` 은 자동으로 처리

<a id="mask와-unmask"></a>

### Mask와 Unmask

`mask`는 다른 unit이 의존성으로 요청하더라도 해당 unit이 시작되지 않게 한다. 자동 시작 등록만 해제하는 `disable`보다 강한 제한이다.

```bash
sudo systemctl mask cups.service       # /etc/systemd/system/cups.service → /dev/null
sudo systemctl unmask cups.service
```

- 내부적으로 `/etc/systemd/system/cups.service` 를 `/dev/null` 로 향하는 심볼릭 링크로 만듦 → 어떤 수단으로도 해당 unit을 시작할 수 없음

- 언제 쓰나:
- 절대 시작되면 안 되는 서비스
- 디스크 풀, 보안 등 이유로 막아야 할 때

<a id="시스템-제어"></a>

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

<a id="원격-제어"></a>

### 원격 제어

```bash
systemctl --host=user@server status nginx
```

- 내부적으로 SSH를 사용 → systemctl에 SSH 클라이언트 기능이 내장

```bash
systemctl --machine=container-name status nginx    # 컨테이너 안의 systemd
```

<a id="자주-쓰는-패턴"></a>

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

- 서비스 파일 없이 즉석에서 cgroup, 격리를 적용해 명령어를 실행 → 백그라운드 작업 처리에 유용

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

- 문법 오류, 잘못된 의존성 등을 찾아냄

#### Cat: 모든 fragment 통합 보기

```bash
$ systemctl cat nginx.service
# /usr/lib/systemd/system/nginx.service
[Unit]
...

# /etc/systemd/system/nginx.service.d/override.conf
[Service]
Restart=always
```

- 원본 unit과 모든 drop-in을 합쳐서 보여줌

<a id="참고-자료"></a>

### 참고 자료

- [man systemctl](https://www.freedesktop.org/software/systemd/man/systemctl.html)
- [man systemd-run](https://www.freedesktop.org/software/systemd/man/systemd-run.html)
- [man systemd-cgls](https://www.freedesktop.org/software/systemd/man/systemd-cgls.html)

## systemd-analyze (부팅 분석과 진단)

> 원본: https://www.freedesktop.org/software/systemd/man/systemd-analyze.html

<a id="개요"></a>
### 개요

`systemd-analyze`는 systemd의 동작을 진단하고 분석하는 도구다. 부팅에 걸린 시간을 조사하거나 unit 파일을 검증하고 서비스의 보안 점수를 확인할 때 사용한다.

- 기본 형식:

```
systemd-analyze [SUBCOMMAND] [OPTIONS]
```

- 서브커맨드 없이 실행하면 `time` 으로 동작

<a id="부팅-시간-분석"></a>

### 부팅 시간 분석

#### time (기본)

```bash
$ systemd-analyze
Startup finished in 1.123s (kernel) + 2.345s (initrd) + 4.567s (userspace) = 8.035s
graphical.target reached after 4.567s in userspace.
```

- 각 단계의 의미:
- kernel: 커널 시작부터 init 실행까지
- initrd: 초기 RAM 디스크 처리 (가능한 경우)
- userspace: PID 1 시작부터 default target 도달까지

#### blame: unit별 소요 시간

```bash
$ systemd-analyze blame
3.502s NetworkManager-wait-online.service
1.234s docker.service
  856ms postgresql.service
  423ms apparmor.service
  ...
```

`blame`은 가장 오래 걸린 unit부터 출력하므로 부팅 시간을 줄일 때 조사할 대상을 찾기 좋다. 다만 여러 unit이 병렬로 실행되기 때문에 여기에 나온 시간을 합쳐도 실제 부팅 시간이 되지는 않는다. 부팅 지연에 영향을 주는 의존성 경로는 다음의 `critical-chain`으로 확인한다.

<a id="critical-chain"></a>

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

- 각 노드는 `@시점 +지속시간` 형식 → 진짜 부팅 지연의 critical path를 보여줌 → 이 경로 위의 unit을 최적화하지 않으면 부팅 속도 개선 불가

- 특정 unit의 critical-chain만 보기:

```bash
systemd-analyze critical-chain nginx.service
```

<a id="plot--gantt-차트"></a>

### Plot: Gantt 차트

```bash
systemd-analyze plot > boot.svg
xdg-open boot.svg
```

`plot`은 부팅 과정을 Gantt 차트로 그려 SVG로 출력한다. 각 unit의 시작 시점과 활성화 시점을 한눈에 볼 수 있어 병렬 부팅 패턴을 이해하기에 가장 좋은 도구다.

<a id="unit-검증"></a>

### Unit 검증

#### verify

- 작성한 unit 파일의 문법과 의존성을 검사

```bash
$ systemd-analyze verify /etc/systemd/system/myapp.service
/etc/systemd/system/myapp.service:5: Unknown section 'Servic'. Ignoring.
myapp.service: Service has no ExecStart=, ExecStop=, or SuccessAction=. Refusing.
```

- CI 파이프라인에서 unit 파일을 머지 전에 검증하기에 적합

#### 환경 변수 영향 확인

```bash
systemd-analyze verify --root=/path/to/test myapp.service
```

<a id="보안-점수"></a>

### 보안 점수

#### security

- 특정 unit의 보안 노출 정도를 0~10점으로 평가 (낮을수록 안전).

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

- 서비스 하드닝의 좋은 출발점

#### 항목별 차이 보여주기

```bash
systemd-analyze security --no-pager nginx.service | less
```

- 각 옵션을 켰을 때 점수가 어떻게 바뀌는지 비교하면서 점진적으로 개선 가능

<a id="캘린더-표현식"></a>

### 캘린더 표현식

- `OnCalendar=` 표현식이 다음 실행 시점을 어떻게 해석하는지 검증

```bash
$ systemd-analyze calendar "Mon..Fri 09:00"
  Original form: Mon..Fri 09:00
Normalized form: Mon..Fri *-*-* 09:00:00
    Next elapse: Mon 2026-05-12 09:00:00 KST
       From now: 4 days left
```

- 여러 번의 실행 시점을 미리 보기:

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

<a id="cat-config"></a>

### Cat-config

- 여러 위치에 흩어진 설정 파일을 한꺼번에 보기

```bash
systemd-analyze cat-config systemd/system.conf
systemd-analyze cat-config systemd/journald.conf
systemd-analyze cat-config systemd/network/eth0.network
```

- 기본 설정 + drop-in을 모두 합쳐 출력
  - 디버깅에 유용

<a id="기타-유용한-서브커맨드"></a>

### 기타 유용한 서브커맨드

#### dot: 의존성 그래프

```bash
systemd-analyze dot --to-pattern='*.target' --from-pattern='*.target' | dot -Tsvg > targets.svg
```

`dot`은 unit 의존성을 Graphviz dot 형식으로 출력한다. 위 예제처럼 Graphviz에 파이프로 넘기면 그래프를 시각화할 수 있다.

#### dump

- systemd 내부 상태 전체 덤프

```bash
systemd-analyze dump > systemd-state.txt
```

- 매우 큰 출력
  - 이슈 리포트에 첨부할 때 사용

#### exit-status

- 종료 코드 의미 설명

```bash
$ systemd-analyze exit-status 217
NAME                       STATUS  CLASS
EXIT_USER                  217     systemd
```

- 서비스가 알 수 없는 종료 코드로 실패했을 때 사용

#### syscall-filter

- syscall 필터 그룹의 내용 확인

```bash
systemd-analyze syscall-filter @system-service
```

- `SystemCallFilter=` 옵션을 작성할 때 어떤 syscall이 포함되는지 확인 가능

#### condition

- 조건식 평가

```bash
$ systemd-analyze condition 'ConditionACPower=true'
test.service: Conditions succeeded.
```

#### unit-files

- unit 검색 경로와 파일 목록 표시

```bash
systemd-analyze unit-files
systemd-analyze unit-paths
```

#### service-watchdogs

- 워치독 사용 여부 확인

```bash
systemd-analyze service-watchdogs
```

### 참고 자료

- [man systemd-analyze](https://www.freedesktop.org/software/systemd/man/systemd-analyze.html)
- [Boot time optimization with systemd](http://0pointer.de/blog/projects/blame-game.html)
