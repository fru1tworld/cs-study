# perf probe

> 동적 트레이스포인트 생성 도구

## 개요

`perf probe`는 커널 함수(kprobes)나 유저 스페이스 함수(uprobes)에 동적으로 트레이스포인트를 추가합니다. 소스 코드 수정이나 재컴파일 없이 런타임에 프로브를 설치할 수 있습니다.

---

## 기본 사용법

```bash
# 커널 함수에 프로브 추가
sudo perf probe --add tcp_sendmsg

# 프로브 목록 확인
sudo perf probe -l

# 프로브로 데이터 수집
sudo perf record -e probe:tcp_sendmsg -ag sleep 10

# 프로브 삭제
sudo perf probe --del tcp_sendmsg
```

---

## 주요 옵션

### 프로브 관리

| 옵션 | 설명 |
|------|------|
| `-a, --add` | 프로브 추가 |
| `-d, --del` | 프로브 삭제 |
| `-l, --list` | 프로브 목록 |
| `-D, --definition` | 프로브 정의만 출력 (추가 안 함) |

### 타겟 지정

| 옵션 | 설명 |
|------|------|
| `-x <exec>` | 유저 스페이스 바이너리 |
| `-m <module>` | 커널 모듈 |
| `-k <vmlinux>` | vmlinux 파일 |
| `-s <dir>` | 소스 코드 디렉토리 |

### 정보 조회

| 옵션 | 설명 |
|------|------|
| `-V <func>` | 함수의 사용 가능한 변수 목록 |
| `-L <func>` | 함수의 소스 라인 목록 |
| `--vars` | 변수 정보 표시 |
| `--externs` | extern 변수 포함 |

### 기타

| 옵션 | 설명 |
|------|------|
| `-n, --dry-run` | 실제 추가 없이 테스트 |
| `-v, --verbose` | 상세 출력 |
| `-f, --force` | 강제 실행 |
| `-q, --quiet` | 조용한 모드 |

---

## 커널 프로브 (kprobes)

### 함수 진입점 프로브

```bash
# 함수 진입 시점 프로브
sudo perf probe --add tcp_sendmsg

# 여러 함수
sudo perf probe --add tcp_sendmsg --add tcp_recvmsg
```

### 함수 반환점 프로브

```bash
# 함수 반환 시점 프로브
sudo perf probe --add 'tcp_sendmsg%return'
```

### 특정 라인 프로브

```bash
# 소스 코드 특정 라인
sudo perf probe --add 'tcp_sendmsg:15'

# 상대 라인 (함수 시작부터)
sudo perf probe --add 'tcp_sendmsg+15'
```

### 변수 캡처

```bash
# 함수 인자 캡처
sudo perf probe --add 'tcp_sendmsg size'

# 여러 변수
sudo perf probe --add 'tcp_sendmsg size sk'

# 구조체 멤버
sudo perf probe --add 'tcp_sendmsg size sk->__sk_common.skc_state'

# 특정 라인의 변수
sudo perf probe --add 'tcp_sendmsg:81 seglen'
```

### 문자열 캡처

```bash
# 문자열 인자
sudo perf probe --add 'do_sys_open filename:string'
```

### 반환값 캡처

```bash
# 함수 반환값
sudo perf probe --add 'tcp_sendmsg%return $retval'
```

---

## 유저 스페이스 프로브 (uprobes)

### 바이너리 함수 프로브

```bash
# 공유 라이브러리 함수
sudo perf probe -x /lib/x86_64-linux-gnu/libc.so.6 --add malloc

# 실행 파일 함수
sudo perf probe -x /usr/bin/my_program --add my_function
```

### 변수 캡처

```bash
# 함수 인자
sudo perf probe -x /lib64/libc.so.6 --add 'malloc size'
```

### 반환값 캡처

```bash
# 반환값
sudo perf probe -x /lib64/libc.so.6 --add 'malloc%return $retval'
```

---

## 정보 조회

### 사용 가능한 변수 확인

```bash
# 함수의 변수 목록
sudo perf probe -V tcp_sendmsg

# extern 변수 포함
sudo perf probe -V tcp_sendmsg --externs

# 특정 라인의 변수
sudo perf probe -V tcp_sendmsg:81
```

### 소스 코드 라인 확인

```bash
# 함수의 소스 라인
sudo perf probe -L tcp_sendmsg

# 라인 범위 지정
sudo perf probe -L tcp_sendmsg:1-100
```

### Dry-run (테스트)

```bash
# 실제 추가 없이 확인
sudo perf probe -nv 'tcp_sendmsg size sk->__sk_common.skc_state'
```

---

## 프로브 관리

### 프로브 목록

```bash
# 모든 perf 프로브
sudo perf probe -l

# 시스템의 모든 kprobes
cat /sys/kernel/debug/tracing/kprobe_events
```

### 프로브 삭제

```bash
# 특정 프로브 삭제
sudo perf probe --del tcp_sendmsg

# 모든 프로브 삭제
sudo perf probe --del '*'

# 패턴으로 삭제
sudo perf probe --del 'tcp_*'
```

---

## 프로브로 데이터 수집

### 기본 수집

```bash
# 프로브 추가
sudo perf probe --add tcp_sendmsg

# 데이터 수집
sudo perf record -e probe:tcp_sendmsg -ag sleep 10

# 결과 확인
sudo perf report
```

### 필터와 함께 사용

```bash
# 조건 필터
sudo perf record -e probe:tcp_sendmsg --filter 'size > 1000' -a sleep 10
```

### 스크립트 출력

```bash
sudo perf script -F time,event,trace
```

---

## 실용적인 예시

### TCP 연결 분석

```bash
# TCP 송신 프로브
sudo perf probe --add 'tcp_sendmsg size'

# 수집
sudo perf record -e probe:tcp_sendmsg -ag sleep 30

# 분석
sudo perf report
sudo perf script
```

### 파일 열기 추적

```bash
# do_sys_open 프로브
sudo perf probe --add 'do_sys_open filename:string'

# 수집
sudo perf record -e probe:do_sys_open -a sleep 10

# 출력
sudo perf script
```

### 메모리 할당 추적

```bash
# kmalloc 프로브
sudo perf probe --add 'kmalloc size'

# 수집
sudo perf record -e probe:kmalloc -a sleep 10

# 분석
sudo perf report
```

### 유저 스페이스 malloc 추적

```bash
# malloc 프로브
sudo perf probe -x /lib/x86_64-linux-gnu/libc.so.6 --add 'malloc size'

# 수집
sudo perf record -e probe_libc:malloc -p <pid> sleep 10

# 분석
sudo perf script
```

### 함수 레이턴시 측정

```bash
# 진입/반환 프로브
sudo perf probe --add tcp_sendmsg
sudo perf probe --add 'tcp_sendmsg%return'

# 수집
sudo perf record -e probe:tcp_sendmsg -e probe:tcp_sendmsg__return -a sleep 10

# 분석 (진입-반환 시간 차이 계산)
sudo perf script
```

---

## 커널 요구사항

### 커널 설정

```bash
# 필요한 커널 설정
CONFIG_KPROBES=y
CONFIG_KPROBE_EVENTS=y
CONFIG_UPROBES=y
CONFIG_UPROBE_EVENTS=y
CONFIG_DEBUG_INFO=y
```

### 디버그 정보

```bash
# 커널 디버그 정보 패키지 설치
# Ubuntu/Debian
sudo apt install linux-image-$(uname -r)-dbgsym

# CentOS/RHEL
sudo debuginfo-install kernel
```

### vmlinux 파일

```bash
# vmlinux 위치 확인
ls /boot/vmlinux-$(uname -r)
ls /usr/lib/debug/boot/vmlinux-$(uname -r)

# vmlinux 지정
sudo perf probe -k /boot/vmlinux-$(uname -r) --add tcp_sendmsg
```

---

## 고급 사용

### 복잡한 표현식

```bash
# 포인터 역참조
sudo perf probe --add 'tcp_sendmsg sk->sk_rcvbuf'

# 배열 요소
sudo perf probe --add 'my_function arr[0]'

# 비트 필드
sudo perf probe --add 'my_function flags->bit1'
```

### 조건부 프로브 (필터)

```bash
# 프로브 추가
sudo perf probe --add 'tcp_sendmsg size'

# 필터와 함께 수집
sudo perf record -e probe:tcp_sendmsg \
  --filter 'size > 0 && skc_state != 1' -a sleep 10
```

### 스택 트레이스 포함

```bash
sudo perf record -e probe:tcp_sendmsg -ag sleep 10
```

---

## 트러블슈팅

### "Failed to find symbol" 오류

```bash
# 디버그 정보 확인
file /boot/vmlinux-$(uname -r)

# 디버그 패키지 설치
sudo apt install linux-image-$(uname -r)-dbgsym
```

### "Failed to write event" 오류

```bash
# kprobe 권한 확인
ls -la /sys/kernel/debug/tracing/

# debugfs 마운트
sudo mount -t debugfs none /sys/kernel/debug

# 기존 프로브 정리
sudo perf probe --del '*'
```

### 변수를 찾을 수 없음

```bash
# 사용 가능한 변수 확인
sudo perf probe -V tcp_sendmsg

# 특정 라인의 변수
sudo perf probe -V tcp_sendmsg:50

# 최적화로 인해 변수가 제거될 수 있음
# -O0 빌드 또는 다른 라인 시도
```

### uprobe 실패

```bash
# 바이너리에 디버그 심볼 확인
file /path/to/binary

# 디버그 버전 설치
sudo apt install libc6-dbg
```

---

## 성능 고려사항

- ** 오버헤드**: 프로브는 약간의 오버헤드를 추가
- ** 빈번한 함수**: 자주 호출되는 함수에 프로브 설치 시 주의
- ** 필터 사용**: 필터로 수집 범위 제한 권장
- ** 수집 시간**: 짧은 시간으로 시작하여 점진적 확장

---

## 참고 자료

- [perf-probe(1) man page](https://man7.org/linux/man-pages/man1/perf-probe.1.html)
- [Linux Kernel Kprobes](https://www.kernel.org/doc/Documentation/kprobes.txt)
- [Linux Kernel Uprobes](https://www.kernel.org/doc/Documentation/trace/uprobetracer.txt)
- [Brendan Gregg's perf Examples](https://www.brendangregg.com/perf.html)
