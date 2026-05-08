# journalctl 사용법

> 이 문서는 `man journalctl` 의 내용을 한국어로 정리한 것입니다.
> 원본: https://www.freedesktop.org/software/systemd/man/journalctl.html

---

## 목차

1. [기본 사용](#기본-사용)
2. [시간 기반 필터](#시간-기반-필터)
3. [Unit 필터](#unit-필터)
4. [Priority와 부팅 필터](#priority와-부팅-필터)
5. [필드 필터](#필드-필터)
6. [출력 형식](#출력-형식)
7. [팔로우와 페이저](#팔로우와-페이저)
8. [관리 명령](#관리-명령)
9. [실전 패턴](#실전-패턴)
10. [참고 자료](#참고-자료)

---

## 기본 사용

```bash
journalctl                  # 모든 로그 (가장 오래된 것부터)
journalctl -r               # 최신 순 (reverse)
journalctl -e               # 페이저를 끝부분에서 시작
journalctl -n 50            # 최신 50줄
journalctl -n               # 기본 10줄
journalctl --no-pager       # 페이저 비활성화 (스크립트용)
```

### 권한

- root 또는 `systemd-journal`, `adm`, `wheel` 그룹에 속한 사용자: 모든 로그
- 일반 사용자: 자기 user unit과 자기가 시작한 프로세스 로그

```bash
sudo usermod -aG systemd-journal $USER       # 일반 사용자에게 전체 로그 읽기 권한
```

---

## 시간 기반 필터

### --since / --until

```bash
journalctl --since "2026-05-08 09:00:00"
journalctl --until "2026-05-08 12:00:00"
journalctl --since "2 hours ago"
journalctl --since "yesterday"
journalctl --since "today" --until "now"
journalctl --since "10 min ago"
```

날짜 형식:
- `YYYY-MM-DD HH:MM:SS`
- `today`, `yesterday`, `tomorrow`
- `now`, `N min ago`, `N hour ago`, `N day ago`
- `+N min`, `-N hour`

### 부팅 단위

```bash
journalctl -b               # 현재 부팅
journalctl -b 0             # 동일 (현재 부팅)
journalctl -b -1            # 직전 부팅
journalctl -b -2            # 두 번 전 부팅
journalctl --list-boots     # 모든 부팅 목록
```

```
$ journalctl --list-boots
IDX BOOT ID                          FIRST ENTRY                  LAST ENTRY
 -2 abc123...                        2026-05-06 09:00:00          2026-05-06 23:30:00
 -1 def456...                        2026-05-07 08:00:00          2026-05-08 02:15:00
  0 ghi789...                        2026-05-08 09:00:00          2026-05-08 13:45:00
```

---

## Unit 필터

### -u / --unit

```bash
journalctl -u nginx.service
journalctl -u nginx                            # 확장자 생략
journalctl -u nginx -u postgresql              # 여러 unit
journalctl -u 'docker-*.service'               # glob
```

### --user-unit (사용자 unit)

```bash
journalctl --user                              # 자기 user 매니저의 모든 로그
journalctl --user-unit backup.service
```

### unit + 시간 결합

```bash
journalctl -u nginx --since "1 hour ago" -n 100
journalctl -u nginx -b -1                      # 직전 부팅의 nginx 로그
```

---

## Priority와 부팅 필터

### -p / --priority

```bash
journalctl -p err                              # err 이상 (err, crit, alert, emerg)
journalctl -p warning..err                     # 범위
journalctl -p 3                                # 숫자도 가능
journalctl -p info -u nginx
```

| 이름 | 숫자 |
| --- | --- |
| emerg | 0 |
| alert | 1 |
| crit | 2 |
| err | 3 |
| warning | 4 |
| notice | 5 |
| info | 6 |
| debug | 7 |

### -k / --dmesg (커널만)

```bash
journalctl -k -b                               # 현재 부팅 커널 로그
journalctl -k -p err                           # 커널 에러
```

---

## 필드 필터

journal 레코드의 모든 필드로 필터링 가능. `KEY=VALUE` 형식:

```bash
journalctl _PID=1234
journalctl _UID=1000
journalctl _SYSTEMD_UNIT=nginx.service
journalctl SYSLOG_IDENTIFIER=cron
journalctl _COMM=sshd
journalctl _EXE=/usr/bin/python3
```

### 여러 필터

같은 키를 여러 번 쓰면 **OR**, 다른 키를 함께 쓰면 **AND**:

```bash
journalctl _SYSTEMD_UNIT=nginx _SYSTEMD_UNIT=apache2     # nginx OR apache2
journalctl _SYSTEMD_UNIT=nginx _PID=1234                  # nginx AND PID 1234
```

### + 로 묶음 (OR 그룹화)

```bash
journalctl _SYSTEMD_UNIT=nginx + _SYSTEMD_UNIT=apache2 _PID=1234
# (nginx) OR (apache2 AND PID 1234)
```

### 사용 가능한 필드 보기

```bash
journalctl --fields                            # 모든 필드 이름
journalctl -F _SYSTEMD_UNIT                    # 특정 필드의 모든 값
journalctl -F _COMM                            # 모든 명령어
journalctl -F PRIORITY                         # 사용된 priority들
```

### grep (정규식 본문 검색)

```bash
journalctl -g "ERROR|FATAL"
journalctl --case-sensitive=false -g "out of memory"
```

---

## 출력 형식

`-o` 또는 `--output` 으로 형식 지정.

| 형식 | 설명 |
| --- | --- |
| `short` | 기본. syslog 비슷한 한 줄 |
| `short-iso` | ISO 8601 타임스탬프 |
| `short-iso-precise` | 마이크로초 정확도 |
| `short-precise` | 기본 + 마이크로초 |
| `short-monotonic` | 부팅 후 경과 시간 |
| `short-unix` | Unix 타임스탬프 |
| `verbose` | 모든 필드 출력 |
| `export` | 바이너리 export 형식 (다른 journal로 import 가능) |
| `json` | 한 줄 JSON |
| `json-pretty` | 보기 좋은 JSON |
| `json-sse` | Server-Sent Events |
| `cat` | MESSAGE 필드만 |

```bash
journalctl -o short-iso -u nginx -n 5
journalctl -o json-pretty -n 1
journalctl -o cat -u myapp                    # 깔끔한 메시지만 (스크립트용)
```

---

## 팔로우와 페이저

### --follow / -f (tail -f 처럼)

```bash
journalctl -f                                  # 모든 새 로그
journalctl -fu nginx                           # nginx 새 로그
journalctl -fu nginx -p warning                # nginx 경고 이상
```

### 페이저 제어

```bash
journalctl --no-pager
SYSTEMD_PAGER=less journalctl
SYSTEMD_PAGERSECURE=1 journalctl               # secure 모드
```

### 출력 줄 수 제한

```bash
journalctl -n 100                              # 최신 100줄
journalctl --lines 100                         # 동일
```

---

## 관리 명령

### 디스크 사용량

```bash
$ journalctl --disk-usage
Archived and active journals take up 512.0M in the file system.
```

### 보존 정책 즉시 적용

```bash
sudo journalctl --vacuum-size=200M             # 200MB 이하로
sudo journalctl --vacuum-time=2weeks           # 2주 이상 된 것 삭제
sudo journalctl --vacuum-files=5               # 최신 5개 파일만
```

### 무결성 검사

```bash
journalctl --verify
```

### Header 정보

```bash
journalctl --header
```

각 journal 파일의 메타데이터 (생성 시각, 시퀀스 등).

### 다른 호스트의 journal 보기

```bash
journalctl -D /path/to/journal-dir
journalctl --machine=container-name
journalctl --root=/mnt/recovered-disk
journalctl --file=/var/log/journal/abc/system.journal
```

복구 디스크나 컨테이너의 journal을 직접 볼 때 유용.

---

## 실전 패턴

### 부팅 실패 디버깅

```bash
journalctl -b -1 -p err                        # 직전 부팅의 에러
journalctl -b -1 -k -p warning                 # 직전 부팅 커널 경고
journalctl -b -1 -u systemd-networkd            # 직전 부팅 네트워크
```

### 서비스 크래시 추적

```bash
journalctl -u myapp.service -p err --since today
journalctl -u myapp.service -g "panic|segfault|abort"
journalctl _SYSTEMD_UNIT=myapp.service _PID=1234
```

### 연속 모니터링

```bash
journalctl -fu myapp.service -p warning
```

다른 터미널에서 trigger 하면서 실시간으로 확인.

### 보안 감사

```bash
journalctl -u sshd.service --since "1 day ago" -g "Failed password"
journalctl _COMM=sudo --since today
journalctl _AUDIT_LOGINUID=1000 --since "1 hour ago"
```

### 성능 측정

```bash
journalctl -b _SYSTEMD_UNIT=systemd-journald.service -o short-monotonic
```

부팅 후 경과 시간 형식으로 출력.

### CSV/TSV로 export

```bash
journalctl -u nginx -o json --since today \
  | jq -r '[.__REALTIME_TIMESTAMP, ._PID, .MESSAGE] | @tsv' \
  > nginx-today.tsv
```

### Catalog (사람이 읽는 설명)

```bash
journalctl -x                                  # MESSAGE_ID 기반 설명 추가
journalctl -xeu nginx.service                  # 자주 쓰는 조합
```

`-x` 는 잘 알려진 MESSAGE_ID에 대해 추가 설명·해결 힌트를 출력합니다.

---

## 참고 자료

- [man journalctl](https://www.freedesktop.org/software/systemd/man/journalctl.html)
- [man systemd.journal-fields](https://www.freedesktop.org/software/systemd/man/systemd.journal-fields.html)
