# 필터링과 쿼리 패턴

> 이 문서는 `journalctl` 의 다양한 쿼리 패턴을 실전 예제 위주로 정리한 것입니다.
> 원본: https://www.freedesktop.org/software/systemd/man/journalctl.html

---

## 목차

1. [Boolean 로직](#boolean-로직)
2. [부팅·시간 결합](#부팅시간-결합)
3. [Cursor 사용](#cursor-사용)
4. [정규식 본문 검색](#정규식-본문-검색)
5. [복합 시나리오](#복합-시나리오)
6. [scripting 패턴](#scripting-패턴)
7. [성능 팁](#성능-팁)
8. [참고 자료](#참고-자료)

---

## Boolean 로직

### 같은 키 여러 번 = OR

```bash
journalctl _SYSTEMD_UNIT=nginx.service _SYSTEMD_UNIT=apache2.service
# nginx OR apache2
```

### 다른 키 함께 = AND

```bash
journalctl _SYSTEMD_UNIT=nginx.service _PID=1234
# nginx AND PID 1234
```

### + 로 OR 그룹

```bash
journalctl _SYSTEMD_UNIT=nginx.service + _SYSTEMD_UNIT=apache2.service _PID=1234
# (nginx) OR (apache2 AND PID 1234)
```

### NOT은 직접 지원 안 함

journalctl 자체에 NOT 연산자는 없습니다. `grep -v` 로 후처리하거나 `--grep` 의 부정 정규식을 사용:

```bash
journalctl -u nginx | grep -v "GET /health"
journalctl -u nginx --grep '^(?!.*GET /health)'
```

---

## 부팅·시간 결합

### 직전 부팅에서 발생한 모든 에러

```bash
journalctl -b -1 -p err
```

### 어제부터 오늘 9시까지의 nginx 로그

```bash
journalctl -u nginx --since yesterday --until "today 09:00:00"
```

### 부팅 후 처음 30초의 모든 로그

```bash
journalctl -b -o short-monotonic | awk '$1 < 30'
```

`short-monotonic` 출력은 첫 컬럼이 부팅 후 경과 초.

### 마지막 5분 + 따라가기

```bash
journalctl --since "5 min ago" -fu myapp
```

---

## Cursor 사용

cursor는 journal 엔트리의 고유 식별자. 위치를 저장해뒀다가 그 시점부터 이어서 읽는 패턴에 유용.

### 현재 마지막 엔트리의 cursor 저장

```bash
journalctl --show-cursor -n 1 | tail -1
# -- cursor: s=abc...;i=123;b=def...
```

또는:

```bash
journalctl -n 1 --output=export | grep '^__CURSOR='
```

### 특정 cursor부터 읽기

```bash
journalctl --after-cursor='s=abc;i=123;b=def;m=456;t=ghi;x=jkl'
```

### 스크립트 패턴

```bash
CURSOR_FILE=/var/lib/myparser/cursor

if [ -f "$CURSOR_FILE" ]; then
    CURSOR=$(cat "$CURSOR_FILE")
    journalctl --after-cursor="$CURSOR" -u myapp -o json
else
    journalctl -u myapp -o json
fi

# 처리 후 마지막 cursor 저장
journalctl -u myapp -n 1 --show-cursor | tail -1 | sed 's/^-- cursor: //' > "$CURSOR_FILE"
```

이런 식으로 외부 시스템에 누락 없이 로그를 전송할 수 있습니다.

---

## 정규식 본문 검색

### --grep / -g

```bash
journalctl -u nginx -g "5\d\d"           # 5xx 응답
journalctl -g "panic|oops|BUG"
journalctl --case-sensitive=false -g "out of memory"
```

PCRE2 문법. `--case-sensitive=false` 면 대소문자 무시.

### grep과의 차이

`grep` 은 한 줄 단위지만 `--grep` 은 `MESSAGE` 필드 단위로 매칭. 멀티라인 메시지에서도 정확.

---

## 복합 시나리오

### OOM 킬러 추적

```bash
journalctl -k -g "oom-kill|out of memory|killed process"
journalctl -b -1 _COMM=systemd-oomd
```

### 디스크 IO 에러

```bash
journalctl -k -g "I/O error|EXT4-fs error|XFS.*error"
```

### sudo 사용 내역

```bash
journalctl _COMM=sudo --since "1 day ago" -o short
journalctl SYSLOG_IDENTIFIER=sudo -g "session opened"
```

### SSH 실패 로그인

```bash
journalctl -u sshd --since "1 day ago" -g "Failed password|Invalid user"
```

### 특정 컨테이너의 로그 (Docker가 systemd cgroup 사용 시)

```bash
journalctl _SYSTEMD_CGROUP=/system.slice/docker-abc123.scope
journalctl CONTAINER_NAME=mycontainer
```

### Service 재시작 횟수 카운트

```bash
journalctl -u myapp -g "Started" --since today -o short \
  | wc -l
```

### Critical chain 부팅 순서 따라가기

```bash
journalctl -b -o short-monotonic _PID=1 | head -50
```

PID=1(systemd) 의 메시지로 부팅 진행을 추적.

### 메모리 누수 의심 서비스의 OOM 직전 로그

```bash
# OOM이 발생한 시각 찾기
OOM_TIME=$(journalctl -k -g "oom-kill" -o short-iso | tail -1 | awk '{print $1, $2}')

# 그 시각 직전 30초의 myapp 로그
journalctl -u myapp --since "$OOM_TIME -30s" --until "$OOM_TIME"
```

---

## scripting 패턴

### JSON으로 파이프

```bash
journalctl -u nginx -o json --since today \
  | jq -r 'select(.PRIORITY|tonumber < 4) | [._SYSTEMD_INVOCATION_ID, .MESSAGE] | @tsv'
```

### export 형식 (raw)

```bash
journalctl -u nginx --since today -o export > nginx.export
# 다른 시스템에서 import:
systemd-cat -p info < nginx.export   # 단순 본문만 다시 흘리기
```

### CSV 변환

```bash
journalctl -u myapp --since today -o json \
  | jq -r '[
      (.__REALTIME_TIMESTAMP | tonumber / 1000000 | strftime("%Y-%m-%dT%H:%M:%S")),
      .PRIORITY,
      ._PID,
      .MESSAGE
    ] | @csv'
```

### 알림 데몬

```bash
journalctl -fu myapp --output=cat | while read line; do
    if echo "$line" | grep -q "FATAL"; then
        # 슬랙/PagerDuty 호출
        curl -X POST "$SLACK_WEBHOOK" --data "{\"text\":\"$line\"}"
    fi
done
```

### 부팅마다 실행되는 분석

```ini
# /etc/systemd/system/boot-analyzer.service
[Unit]
Description=Analyze previous boot
After=multi-user.target

[Service]
Type=oneshot
ExecStart=/usr/local/bin/boot-analyzer.sh

[Install]
WantedBy=multi-user.target
```

```bash
#!/bin/bash
# 직전 부팅의 critical 로그를 슬랙에 전송
journalctl -b -1 -p err -o json | python3 /usr/local/lib/notify-errors.py
```

---

## 성능 팁

### 인덱스 활용

journalctl은 cursor와 시간 기반 필터에 인덱스를 사용. 느려질 때:
- 시간 범위(`--since`)를 좁히기
- 너무 많은 부팅 데이터 누적 → vacuum
- 패턴 검색(`--grep`) 은 풀 스캔이라 느림. 필드 필터 우선

### 빠른 vs 느린 패턴

```bash
# 빠름 (필드 인덱스)
journalctl _SYSTEMD_UNIT=nginx --since "1 hour ago"

# 느림 (전체 스캔 + 본문 정규식)
journalctl --since today --grep "complex.*regex"

# 권장: 필드로 좁히고 grep으로 마무리
journalctl _SYSTEMD_UNIT=nginx --since today --grep "complex.*regex"
```

### 페이저 끄기

스크립트에서:

```bash
journalctl --no-pager -u nginx -n 100
SYSTEMD_PAGER=cat journalctl ...
```

### Disk usage 줄이기

```bash
sudo journalctl --vacuum-size=500M
sudo journalctl --vacuum-time=1week
```

또는 `journald.conf` 에서 `SystemMaxUse=` 설정.

---

## 참고 자료

- [man journalctl](https://www.freedesktop.org/software/systemd/man/journalctl.html)
- [man systemd.journal-fields](https://www.freedesktop.org/software/systemd/man/systemd.journal-fields.html)
- [PCRE2 Regex Syntax](https://www.pcre.org/current/doc/html/pcre2syntax.html)
