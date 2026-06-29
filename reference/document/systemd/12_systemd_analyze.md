# systemd-analyze (부팅 분석과 진단)

> 원본: https://www.freedesktop.org/software/systemd/man/systemd-analyze.html

---

## 목차

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

## 개요

`systemd-analyze` 는 systemd 동작을 진단하고 분석하는 도구입니다. 부팅 시간 분석부터 unit 파일 검증, 보안 점수 측정까지 다양한 기능을 제공합니다.

기본 형식:
```
systemd-analyze [SUBCOMMAND] [OPTIONS]
```

서브커맨드 없이 실행하면 `time` 으로 동작.

---

## 부팅 시간 분석

### time (기본)

```bash
$ systemd-analyze
Startup finished in 1.123s (kernel) + 2.345s (initrd) + 4.567s (userspace) = 8.035s
graphical.target reached after 4.567s in userspace.
```

각 단계의 의미:
- **kernel**: 커널 시작부터 init 실행까지
- **initrd**: 초기 RAM 디스크 처리 (가능한 경우)
- **userspace**: PID 1 시작부터 default target 도달까지

### blame — unit별 소요 시간

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

## Critical chain

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

## Plot — Gantt 차트

```bash
systemd-analyze plot > boot.svg
xdg-open boot.svg
```

부팅 과정의 시각적 Gantt 차트를 SVG로 출력. 각 unit이 언제 시작되어 언제 활성화됐는지 한눈에 보입니다. 병렬 부팅 패턴을 이해하는 데 가장 좋은 도구.

---

## Unit 검증

### verify

작성한 unit 파일의 문법과 의존성을 검사.

```bash
$ systemd-analyze verify /etc/systemd/system/myapp.service
/etc/systemd/system/myapp.service:5: Unknown section 'Servic'. Ignoring.
myapp.service: Service has no ExecStart=, ExecStop=, or SuccessAction=. Refusing.
```

CI 파이프라인에서 unit 파일을 머지 전에 검증하기에 적합.

### 환경 변수 영향 확인

```bash
systemd-analyze verify --root=/path/to/test myapp.service
```

---

## 보안 점수

### security

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

### 모든 서비스 점수

```bash
$ systemd-analyze security
UNIT                            EXPOSURE PREDICATE HAPPY
nginx.service                        6.5 MEDIUM    🙂
postgresql.service                   8.7 EXPOSED   😨
sshd.service                         9.6 UNSAFE    😨
my-hardened-app.service              1.4 OK        😀
```

서비스 하드닝의 좋은 출발점.

### 항목별 차이 보여주기

```bash
systemd-analyze security --no-pager nginx.service | less
```

각 옵션을 켰을 때 점수가 어떻게 바뀌는지 비교하면서 점진적으로 개선 가능.

---

## 캘린더 표현식

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

### 시간 단위 검증

```bash
$ systemd-analyze timespan "1h 30min 45s"
Original: 1h 30min 45s
      μs: 5445000000
   Human: 1h 30min 45s
```

---

## Cat-config

여러 위치에 흩어진 설정 파일을 한꺼번에 보기.

```bash
systemd-analyze cat-config systemd/system.conf
systemd-analyze cat-config systemd/journald.conf
systemd-analyze cat-config systemd/network/eth0.network
```

기본 설정 + drop-in을 모두 합쳐 출력. 디버깅에 유용.

---

## 기타 유용한 서브커맨드

### dot — 의존성 그래프

```bash
systemd-analyze dot --to-pattern='*.target' --from-pattern='*.target' | dot -Tsvg > targets.svg
```

unit 의존성을 Graphviz dot 형식으로 출력. 파이프해서 시각화.

### dump

systemd 내부 상태 전체 덤프.

```bash
systemd-analyze dump > systemd-state.txt
```

매우 큰 출력. 이슈 리포트에 첨부할 때 사용.

### exit-status

종료 코드 의미 설명.

```bash
$ systemd-analyze exit-status 217
NAME                       STATUS  CLASS
EXIT_USER                  217     systemd
```

서비스가 알 수 없는 종료 코드로 실패했을 때 사용.

### syscall-filter

syscall 필터 그룹의 내용 확인.

```bash
systemd-analyze syscall-filter @system-service
```

`SystemCallFilter=` 옵션을 작성할 때 어떤 syscall이 포함되는지 확인 가능.

### condition

조건식 평가.

```bash
$ systemd-analyze condition 'ConditionACPower=true'
test.service: Conditions succeeded.
```

### unit-files

unit 검색 경로와 파일 목록 표시.

```bash
systemd-analyze unit-files
systemd-analyze unit-paths
```

### service-watchdogs

워치독 사용 여부 확인.

```bash
systemd-analyze service-watchdogs
```

---

## 참고 자료

- [man systemd-analyze](https://www.freedesktop.org/software/systemd/man/systemd-analyze.html)
- [Boot time optimization with systemd](http://0pointer.de/blog/projects/blame-game.html)
