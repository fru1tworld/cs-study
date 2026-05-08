# Timer Unit

> 이 문서는 `man systemd.timer` 의 내용을 한국어로 정리한 것입니다.
> 원본: https://www.freedesktop.org/software/systemd/man/systemd.timer.html

---

## 목차

1. [Timer란?](#timer란)
2. [Timer와 cron의 차이](#timer와-cron의-차이)
3. [Monotonic 타이머](#monotonic-타이머)
4. [Realtime 타이머 (OnCalendar)](#realtime-타이머-oncalendar)
5. [Timer 옵션](#timer-옵션)
6. [Persistent와 Accuracy](#persistent와-accuracy)
7. [실전 예제](#실전-예제)
8. [관리 명령](#관리-명령)
9. [참고 자료](#참고-자료)

---

## Timer란?

`.timer` unit은 **다른 unit(보통 service)을 일정 시점/주기에 활성화**하는 unit입니다. cron 작업을 systemd 방식으로 표현하는 도구입니다.

기본 패턴:
- `backup.timer` 가 `backup.service` 를 실행
- 두 파일은 보통 **같은 이름의 짝(pair)**
- `Unit=` 으로 다른 이름의 service를 지정할 수도 있음

```ini
# /etc/systemd/system/backup.timer
[Unit]
Description=Daily Backup Timer

[Timer]
OnCalendar=daily
Persistent=true
Unit=backup.service

[Install]
WantedBy=timers.target
```

```ini
# /etc/systemd/system/backup.service
[Unit]
Description=Daily Backup Job

[Service]
Type=oneshot
ExecStart=/usr/local/bin/backup.sh
```

`backup.timer` 만 enable하면 됩니다 — `backup.service` 는 enable 불필요.

---

## Timer와 cron의 차이

| 항목 | cron | systemd timer |
| --- | --- | --- |
| 표현 형식 | crontab 5필드 | OnCalendar 자연어 |
| 로깅 | 별도 메일/로그 | journald 통합 |
| 의존성 | 없음 | unit 의존성 활용 |
| 실패 처리 | 사용자 책임 | Restart, OnFailure |
| 부팅 후 누락 작업 | 없음 | Persistent 옵션 |
| 리소스 제어 | 없음 | cgroup으로 제한 가능 |
| 보안 격리 | 없음 | systemd.exec 옵션 모두 사용 |
| 사용자 단위 | crontab -e | `systemctl --user` |

---

## Monotonic 타이머

시스템이 켜진 시점이나 unit이 활성화된 시점 등을 기준으로 동작.

```ini
[Timer]
OnBootSec=15min
OnUnitActiveSec=1h
```

### 종류

| 옵션 | 기준 |
| --- | --- |
| `OnActiveSec=` | timer가 active가 된 시점부터 |
| `OnBootSec=` | 시스템 부팅 시점부터 |
| `OnStartupSec=` | systemd 시작 시점부터 |
| `OnUnitActiveSec=` | 연관 unit이 마지막으로 active가 된 시점부터 |
| `OnUnitInactiveSec=` | 연관 unit이 마지막으로 inactive가 된 시점부터 |

### 시간 표기

- `30s`, `5min`, `2h`, `1d`, `1week`, `1month`, `1year`
- 조합 가능: `1h 30min`

여러 개를 지정하면 모든 조건이 OR로 결합됩니다.

```ini
[Timer]
OnBootSec=15min          # 부팅 후 15분
OnUnitActiveSec=1h        # 그 다음부터 1시간마다
```

---

## Realtime 타이머 (OnCalendar)

벽시계 시간 기준의 캘린더 표현식.

```ini
OnCalendar=Mon..Fri 09:00
OnCalendar=*-*-01 03:00:00
OnCalendar=hourly
```

### 형식

```
DayOfWeek Year-Month-Day Hour:Minute:Second
```

- `*` 은 모든 값
- `..` 은 범위 (`Mon..Fri`)
- `,` 은 목록 (`Mon,Wed,Fri`)
- `/` 은 간격 (`*/5` = 5의 배수)

### 자주 쓰는 단축어

| 단축어 | 의미 |
| --- | --- |
| `minutely` | 매분 |
| `hourly` | 매 시 정각 (`*-*-* *:00:00`) |
| `daily` | 매일 자정 |
| `weekly` | 매주 월요일 자정 |
| `monthly` | 매월 1일 자정 |
| `yearly` / `annually` | 매년 1월 1일 |
| `quarterly` | 분기마다 |
| `semiannually` | 6개월마다 |

### 예시

```
*-*-* *:00:00              매시간 정각
*-*-* 02:30:00             매일 02:30
Mon *-*-* 03:00:00         월요일 03:00
*-*-01 00:00:00            매월 1일
*-01,07-01 00:00:00        1월 1일과 7월 1일
*-*-* 00/15:00             0,15,30,45분마다 (15분 간격)
2025-12-25 09:00:00        2025년 12월 25일 09:00 (한 번)
```

### 검증

```bash
$ systemd-analyze calendar "Mon..Fri 09:00"
  Original form: Mon..Fri 09:00
Normalized form: Mon..Fri *-*-* 09:00:00
    Next elapse: Mon 2026-05-12 09:00:00 KST
       From now: 4 days left
```

---

## Timer 옵션

### Persistent

```ini
Persistent=true
```

`OnCalendar` 와 함께 사용. 시스템이 꺼져 있어 타이머를 놓친 경우, 부팅 후 즉시 한 번 실행. cron의 `anacron` 동작과 비슷.

`/var/lib/systemd/timers/` 에 마지막 실행 시각을 저장합니다.

### Accuracy

```ini
AccuracySec=1min
```

타이머의 정확도. 기본 1분. 정확도를 낮추면(예: `1h`) 여러 타이머가 같은 시점에 발화하도록 systemd가 묶어주어 절전 효과가 있음. 데스크탑이나 IoT에서 유용.

매우 정확한 트리거가 필요하면:
```ini
AccuracySec=1us
```

### RandomizedDelaySec

```ini
RandomizedDelaySec=5min
```

발화 시점에 0~지정 시간 사이의 랜덤 지연 추가. 여러 머신에서 같은 cron을 돌릴 때 부하가 한 번에 몰리는 것을 방지.

### FixedRandomDelay

```ini
FixedRandomDelay=true
```

랜덤 지연을 호스트마다 고정값으로 (호스트 ID와 unit 이름 기반). 같은 머신은 항상 같은 지연을 받음.

### WakeSystem

```ini
WakeSystem=true
```

타이머가 발화할 때 시스템을 슬립에서 깨움 (RTC 사용). 노트북 백업 같은 시나리오.

### OnClockChange / OnTimezoneChange

```ini
OnClockChange=true
OnTimezoneChange=true
```

시간이 점프하거나 타임존이 바뀌면 즉시 발화.

### RemainAfterElapse

```ini
RemainAfterElapse=no
```

타이머가 마지막으로 발화한 후 즉시 정리할지. 대부분 기본값 `yes` 가 적절.

---

## Persistent와 Accuracy

타이머를 다룰 때 가장 흔한 실수가 두 가지입니다.

### 누락 작업이 처리되지 않음

cron 사용자가 systemd timer로 옮길 때 가장 자주 부딪치는 문제. 다음 옵션을 추가하세요:

```ini
[Timer]
OnCalendar=daily
Persistent=true
```

`Persistent=true` 가 없으면 시스템이 꺼져 있던 동안의 작업은 실행되지 않습니다.

### 여러 머신이 동시에 실행

```ini
[Timer]
OnCalendar=hourly
RandomizedDelaySec=10min
```

이렇게 하면 매 시 정각이 아니라 정각~정각+10분 사이에 분산되어 실행됩니다.

---

## 실전 예제

### 1. 매일 새벽 3시 백업

```ini
# backup.timer
[Unit]
Description=Daily Database Backup

[Timer]
OnCalendar=*-*-* 03:00:00
Persistent=true
RandomizedDelaySec=15min

[Install]
WantedBy=timers.target
```

### 2. 부팅 5분 후 실행, 그 후 1시간마다

```ini
# health-check.timer
[Unit]
Description=Periodic Health Check

[Timer]
OnBootSec=5min
OnUnitActiveSec=1h
Unit=health-check.service

[Install]
WantedBy=timers.target
```

### 3. 평일 업무시간만 실행

```ini
[Timer]
OnCalendar=Mon..Fri 09:00,12:00,15:00
Persistent=false
```

### 4. 사용자 timer

`~/.config/systemd/user/sync.timer` 에 작성 후:

```bash
systemctl --user enable --now sync.timer
loginctl enable-linger $USER   # 사용자 로그아웃 후에도 동작
```

`enable-linger` 가 없으면 사용자가 로그인해 있는 동안만 timer가 동작합니다.

---

## 관리 명령

### 모든 타이머 보기

```bash
systemctl list-timers --all
```

```
NEXT                        LEFT          LAST                        PASSED       UNIT
Sat 2026-05-09 03:00:00 KST 14h left      Fri 2026-05-08 03:00:00 KST 9h ago       backup.timer
Fri 2026-05-08 13:30:15 KST 30min left    Fri 2026-05-08 12:30:15 KST 29min ago    health-check.timer
```

### 캘린더 식 검증

```bash
systemd-analyze calendar "Mon..Fri 09:00"
systemd-analyze calendar --iterations=5 "*-*-* 03:00:00"
```

### 타이머 즉시 실행 (테스트)

```bash
systemctl start backup.service    # 타이머가 아닌 service를 직접 시작
```

타이머 자체는 시작하면 다음 발화 시점만 계산할 뿐 즉시 실행하지 않습니다. 즉시 실행은 service를 직접 호출.

### 마지막 실행 결과

```bash
journalctl -u backup.service -n 50
systemctl status backup.timer
```

---

## 참고 자료

- [man systemd.timer](https://www.freedesktop.org/software/systemd/man/systemd.timer.html)
- [man systemd.time](https://www.freedesktop.org/software/systemd/man/systemd.time.html) — 시간/캘린더 표기 레퍼런스
- [systemd-analyze calendar](https://www.freedesktop.org/software/systemd/man/systemd-analyze.html)
