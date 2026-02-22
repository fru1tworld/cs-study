# POSIX (Portable Operating System Interface)

## 목차

1. [개요](#1-개요)
2. [역사](#2-역사)
3. [POSIX가 정의하는 주요 영역](#3-posix가-정의하는-주요-영역)
4. [주요 API 및 시스템 콜](#4-주요-api-및-시스템-콜)
5. [파일 시스템 규약](#5-파일-시스템-규약)
6. [셸 스크립트 표준](#6-셸-스크립트-표준)
7. [정규 표현식](#7-정규-표현식)
8. [POSIX 호환 운영체제 목록](#8-posix-호환-운영체제-목록)
9. [POSIX vs SUS](#9-posix-vs-sus-single-unix-specification-관계)
10. [장점과 단점](#10-장점과-단점)
11. [실제 활용 사례 및 개발자가 알아야 할 점](#11-실제-활용-사례-및-개발자가-알아야-할-점)

---

## 1. 개요

### POSIX란 무엇인가

POSIX는 Portable Operating System Interface의 약자로, 운영체제 간의 호환성을 보장하기 위해 IEEE(Institute of Electrical and Electronics Engineers)가 제정한 표준 규격이다. 정식 명칭은 IEEE Std 1003이며, 국제 표준으로는 ISO/IEC 9945로도 등록되어 있다.

POSIX라는 이름은 리처드 스톨만(Richard Stallman)이 제안한 것으로, 원래 IEEE 표준 번호만으로 불리던 이 규격에 기억하기 쉬운 이름을 붙인 것이다. 마지막 글자 X는 UNIX 계열 운영체제의 전통적인 명명 관례에서 따온 것이다.

### 왜 만들어졌는가

1980년대, UNIX 운영체제는 수많은 벤더(vendor)에 의해 각기 다른 방향으로 분화되고 있었다. AT&T의 System V, BSD(Berkeley Software Distribution), Sun의 SunOS, HP의 HP-UX, IBM의 AIX 등 각 벤더가 독자적인 UNIX 변종을 만들면서, 동일한 소스 코드가 서로 다른 UNIX 시스템에서 컴파일되지 않는 이식성(portability) 문제가 심각해졌다.

이러한 배경에서 POSIX는 다음과 같은 목적으로 탄생했다:

- 소프트웨어 이식성 확보: 하나의 소스 코드를 여러 운영체제에서 컴파일하고 실행할 수 있도록 표준 인터페이스를 정의한다.
- 벤더 종속(vendor lock-in) 방지: 특정 운영체제에만 동작하는 코드를 줄이고, 개발자가 특정 벤더에 종속되지 않도록 한다.
- 개발 비용 절감: 플랫폼마다 별도의 코드를 작성할 필요 없이, 표준 API를 사용하면 한 번의 개발로 다수의 플랫폼을 지원할 수 있다.

### 핵심 철학

POSIX는 구현(implementation)이 아닌 인터페이스(interface)를 표준화한다. 즉, 운영체제 내부가 어떻게 동작하는지는 규정하지 않으며, 오직 프로그래머가 사용하는 API의 동작 방식만을 정의한다. 이를 통해 각 운영체제는 자유롭게 내부 구현을 최적화하면서도 동일한 인터페이스를 외부에 제공할 수 있다.

---

## 2. 역사

### UNIX 전쟁 (Unix Wars)

POSIX의 탄생을 이해하려면 1980년대의 유닉스 전쟁(Unix Wars)을 알아야 한다.

| 시기 | 사건 |
|------|------|
| 1969 | AT&T 벨 연구소에서 UNIX 탄생 (켄 톰프슨, 데니스 리치) |
| 1977 | BSD(Berkeley Software Distribution) 등장 |
| 1983 | AT&T가 System V를 상업적으로 출시 |
| 1984~1990 | 유닉스 전쟁 본격화 - AT&T System V 진영 vs BSD 진영 |
| 1987 | AT&T와 Sun Microsystems가 협력하여 SVR4(System V Release 4) 개발 발표 |
| 1988 | 이에 대항하여 IBM, DEC, HP 등이 OSF(Open Software Foundation) 결성 |

이 시기에 UNIX의 파편화가 극심해지면서, 소프트웨어 업계 전반에서 표준화에 대한 요구가 거세졌다. 이것이 POSIX 표준화의 직접적인 동기가 되었다.

### IEEE Std 1003 시리즈와 버전 이력

| 표준 번호 | 연도 | 내용 |
|-----------|------|------|
| IEEE Std 1003.1-1988 | 1988 | 최초의 POSIX 표준. 기본 시스템 인터페이스(C API) 정의. `POSIX.1`이라고도 불림 |
| IEEE Std 1003.2-1992 | 1992 | 셸과 유틸리티 표준. `POSIX.2`라고 불림. sh, awk, sed 등 정의 |
| IEEE Std 1003.1b-1993 | 1993 | 실시간 확장(Realtime Extensions). 세마포어, 공유 메모리, 메시지 큐 등 |
| IEEE Std 1003.1c-1995 | 1995 | 스레드 확장(Threads Extensions). POSIX Threads(pthread) 정의 |
| IEEE Std 1003.1g-2000 | 2000 | 프로토콜 독립적 인터페이스(소켓 API 등) |
| IEEE Std 1003.1-2001 | 2001 | 이전의 여러 표준을 통합한 대규모 개정판. SUSv3와 동일 |
| IEEE Std 1003.1-2008 | 2008 | 추가 개정. SUSv4와 동일. 일부 레거시 인터페이스 제거 |
| IEEE Std 1003.1-2017 | 2017 | 기술적 정오표(Technical Corrigenda) 반영 |
| IEEE Std 1003.1-2024 | 2024 | 최신 개정판. Issue 8로 불림 |

### 표준화의 통합 흐름

초기에는 POSIX.1, POSIX.2 등으로 분리되어 있던 표준이 2001년 개정(IEEE Std 1003.1-2001)에서 하나의 문서로 통합되었다. 이 통합본은 The Open Group의 SUSv3(Single UNIX Specification Version 3)과 동일한 내용이며, 이후로 POSIX와 SUS는 사실상 하나의 표준으로 공동 관리되고 있다.

---

## 3. POSIX가 정의하는 주요 영역

POSIX 표준 문서는 크게 네 개의 볼륨으로 구성된다:

- Base Definitions (XBD): 기본 용어, 헤더 파일, 환경 변수 등의 정의
- System Interfaces (XSH): C 언어 기반 시스템 인터페이스(함수) 정의
- Shell & Utilities (XCU): 셸 명령어 및 유틸리티 정의
- Rationale (XRAT): 표준의 설계 근거 및 해설 (규범적 내용 아님)

### 3.1 시스템 인터페이스 (System Interfaces)

POSIX 시스템 인터페이스는 C 언어를 기반으로 운영체제가 제공해야 하는 함수(시스템 콜 및 라이브러리 함수)를 정의한다.

주요 범주:

| 범주 | 설명 | 대표 함수 |
|------|------|-----------|
| 프로세스 관리 | 프로세스 생성, 종료, 대기 | `fork()`, `exec()`, `wait()`, `exit()` |
| 파일 I/O | 파일 열기, 읽기, 쓰기, 닫기 | `open()`, `read()`, `write()`, `close()` |
| 파일 시스템 | 디렉터리 조작, 링크, 상태 조회 | `mkdir()`, `stat()`, `link()`, `unlink()` |
| 시그널 처리 | 비동기 이벤트 통지 | `signal()`, `sigaction()`, `kill()` |
| IPC(프로세스 간 통신) | 파이프, FIFO, 메시지 큐, 공유 메모리, 세마포어 | `pipe()`, `mmap()`, `sem_open()` |
| 네트워킹 | 소켓 기반 네트워크 통신 | `socket()`, `bind()`, `connect()`, `accept()` |
| 시간/타이머 | 시간 조회 및 타이머 설정 | `time()`, `clock_gettime()`, `nanosleep()` |
| 사용자/그룹 | 사용자 및 그룹 정보 조회 | `getuid()`, `getgid()`, `getpwnam()` |

### 3.2 셸과 유틸리티 (Shell & Utilities)

POSIX는 sh(Bourne Shell) 호환 셸의 동작 방식과 함께, 운영체제가 반드시 제공해야 하는 유틸리티(명령어) 목록을 정의한다.

필수 유틸리티 예시:

```
awk, basename, cat, cd, chmod, chown, cmp, cp, cut, date, dd, diff,
dirname, echo, env, expr, false, find, grep, head, id, join, kill,
ln, ls, mkdir, mv, od, paste, printf, ps, pwd, rm, rmdir, sed,
sh, sleep, sort, tail, tar, tee, test, touch, tr, true, umask,
uname, uniq, wc, xargs
```

이 유틸리티들은 POSIX 호환 시스템이라면 어디서든 동일한 옵션과 동작을 보장해야 한다. 단, `bash`, `zsh`, `vim`, `gcc` 등은 POSIX 필수 유틸리티가 아니다.

### 3.3 실시간 확장 (Realtime Extensions, POSIX.1b)

실시간 시스템에서 필요한 결정론적(deterministic) 동작을 지원하기 위해 추가된 확장이다.

주요 기능:

| 기능 | 설명 | 관련 API |
|------|------|----------|
| 실시간 시그널 | 큐잉 가능한 시그널, 추가 데이터 전달 | `sigqueue()`, `sigwaitinfo()` |
| 세마포어 | 이름 있는/이름 없는 세마포어 | `sem_open()`, `sem_wait()`, `sem_post()` |
| 메시지 큐 | 우선순위 기반 메시지 전달 | `mq_open()`, `mq_send()`, `mq_receive()` |
| 공유 메모리 | 프로세스 간 메모리 공유 | `shm_open()`, `mmap()` |
| 메모리 잠금 | 페이지를 물리 메모리에 고정 | `mlock()`, `mlockall()` |
| 클록/타이머 | 고해상도 시간 측정, 타이머 | `clock_gettime()`, `timer_create()` |
| 비동기 I/O | 논블로킹 I/O 연산 | `aio_read()`, `aio_write()` |
| 스케줄링 | 실시간 스케줄링 정책 | `sched_setscheduler()`, `sched_get_priority_max()` |

### 3.4 스레드 확장 (Threads Extensions, POSIX.1c)

POSIX Threads(pthread)는 멀티스레드 프로그래밍을 위한 표준 API이다. 이 확장은 운영체제와 무관하게 동일한 방식으로 스레드를 생성하고 관리할 수 있게 한다.

주요 기능:

| 범주 | 설명 | 관련 API |
|------|------|----------|
| 스레드 관리 | 스레드 생성, 종료, 조인 | `pthread_create()`, `pthread_exit()`, `pthread_join()` |
| 뮤텍스 | 상호 배제를 위한 잠금 | `pthread_mutex_init()`, `pthread_mutex_lock()`, `pthread_mutex_unlock()` |
| 조건 변수 | 스레드 간 이벤트 통지 | `pthread_cond_wait()`, `pthread_cond_signal()`, `pthread_cond_broadcast()` |
| 읽기-쓰기 잠금 | 다수의 독자와 단일 기록자 | `pthread_rwlock_rdlock()`, `pthread_rwlock_wrlock()` |
| 스레드 로컬 저장소 | 스레드별 고유 데이터 | `pthread_key_create()`, `pthread_setspecific()`, `pthread_getspecific()` |
| 스레드 속성 | 스택 크기, 분리 상태 등 설정 | `pthread_attr_init()`, `pthread_attr_setdetachstate()` |
| 배리어 | 여러 스레드의 동기화 지점 | `pthread_barrier_init()`, `pthread_barrier_wait()` |
| 스핀락 | 짧은 대기 시간을 위한 바쁜 대기 잠금 | `pthread_spin_lock()`, `pthread_spin_unlock()` |

---

## 4. 주요 API 및 시스템 콜

### 4.1 프로세스 생성: fork()

```c
#include <unistd.h>

pid_t fork(void);
```

현재 프로세스의 복사본을 만들어 자식 프로세스(child process)를 생성한다. 호출 후 부모 프로세스에서는 자식의 PID를 반환하고, 자식 프로세스에서는 0을 반환한다. 실패 시 -1을 반환한다.

```c
pid_t pid = fork();
if (pid == 0) {
    // 자식 프로세스
    printf("I am the child, PID=%d\n", getpid());
} else if (pid > 0) {
    // 부모 프로세스
    printf("I am the parent, child PID=%d\n", pid);
    wait(NULL); // 자식 종료 대기
} else {
    perror("fork failed");
}
```

### 4.2 프로그램 실행: exec 계열

```c
#include <unistd.h>

int execl(const char *path, const char *arg, ... /* (char *)NULL */);
int execv(const char *path, char *const argv[]);
int execle(const char *path, const char *arg, ... /*, (char *)NULL, char *const envp[] */);
int execve(const char *path, char *const argv[], char *const envp[]);
int execlp(const char *file, const char *arg, ... /* (char *)NULL */);
int execvp(const char *file, char *const argv[]);
```

현재 프로세스의 이미지(코드, 데이터)를 새로운 프로그램으로 대체(replace)한다. `fork()` 후 `exec()`를 호출하는 것이 UNIX/POSIX에서 새로운 프로그램을 실행하는 전통적인 패턴이다.

```c
// fork-exec 패턴
pid_t pid = fork();
if (pid == 0) {
    // 자식 프로세스에서 ls 실행
    execlp("ls", "ls", "-la", NULL);
    // exec가 성공하면 이 줄은 실행되지 않음
    perror("exec failed");
    _exit(1);
}
```

### 4.3 파이프: pipe()

```c
#include <unistd.h>

int pipe(int fildes[2]);
```

단방향 통신 채널을 생성한다. `fildes[0]`은 읽기용, `fildes[1]`은 쓰기용 파일 디스크립터이다. 보통 `fork()`와 함께 사용하여 부모-자식 프로세스 간 통신에 활용한다.

```c
int fd[2];
pipe(fd);

pid_t pid = fork();
if (pid == 0) {
    // 자식: 쓰기
    close(fd[0]);
    write(fd[1], "hello", 5);
    close(fd[1]);
} else {
    // 부모: 읽기
    close(fd[1]);
    char buf[128];
    int n = read(fd[0], buf, sizeof(buf));
    buf[n] = '\0';
    printf("Received: %s\n", buf);
    close(fd[0]);
    wait(NULL);
}
```

### 4.4 시그널: signal() / sigaction()

```c
#include <signal.h>

void (*signal(int sig, void (*func)(int)))(int);     // 간단하지만 이식성 문제 있음
int sigaction(int sig, const struct sigaction *act,
              struct sigaction *oact);                 // 권장되는 방식
```

시그널은 프로세스에 비동기적으로 전달되는 소프트웨어 인터럽트이다. `signal()`은 오래된 인터페이스로 플랫폼마다 동작이 미묘하게 다를 수 있어, POSIX에서는 `sigaction()`을 권장한다.

주요 시그널:

| 시그널 | 번호 (일반적) | 설명 |
|--------|--------------|------|
| `SIGHUP` | 1 | 터미널 연결 끊김 |
| `SIGINT` | 2 | 인터럽트 (Ctrl+C) |
| `SIGQUIT` | 3 | 종료 + 코어 덤프 (Ctrl+\\) |
| `SIGKILL` | 9 | 강제 종료 (포착/무시 불가) |
| `SIGSEGV` | 11 | 잘못된 메모리 접근 |
| `SIGPIPE` | 13 | 끊어진 파이프에 쓰기 |
| `SIGTERM` | 15 | 정상 종료 요청 |
| `SIGCHLD` | 17/20 | 자식 프로세스 상태 변경 |
| `SIGSTOP` | 19/17 | 프로세스 일시 중지 (포착/무시 불가) |
| `SIGCONT` | 18/19 | 중지된 프로세스 재개 |

```c
#include <signal.h>
#include <stdio.h>

void handler(int sig) {
    printf("Caught signal %d\n", sig);
}

int main(void) {
    struct sigaction sa;
    sa.sa_handler = handler;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = 0;

    sigaction(SIGINT, &sa, NULL);

    while (1) {
        pause(); // 시그널 대기
    }
    return 0;
}
```

### 4.5 POSIX Threads: pthread

```c
#include <pthread.h>

int pthread_create(pthread_t *thread, const pthread_attr_t *attr,
                   void *(*start_routine)(void *), void *arg);
int pthread_join(pthread_t thread, void retval);
int pthread_mutex_lock(pthread_mutex_t *mutex);
int pthread_mutex_unlock(pthread_mutex_t *mutex);
```

POSIX 스레드 예제:

```c
#include <pthread.h>
#include <stdio.h>

pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
int counter = 0;

void *worker(void *arg) {
    for (int i = 0; i < 100000; i++) {
        pthread_mutex_lock(&mutex);
        counter++;
        pthread_mutex_unlock(&mutex);
    }
    return NULL;
}

int main(void) {
    pthread_t t1, t2;

    pthread_create(&t1, NULL, worker, NULL);
    pthread_create(&t2, NULL, worker, NULL);

    pthread_join(t1, NULL);
    pthread_join(t2, NULL);

    printf("Counter = %d\n", counter); // 200000
    return 0;
}
```

컴파일 시 `-lpthread` 플래그가 필요하다:

```bash
gcc -o example example.c -lpthread
```

### 4.6 기타 중요 시스템 콜

| 함수 | 헤더 | 설명 |
|------|------|------|
| `open()` | `<fcntl.h>` | 파일 열기/생성 |
| `close()` | `<unistd.h>` | 파일 디스크립터 닫기 |
| `read()` | `<unistd.h>` | 파일 디스크립터에서 읽기 |
| `write()` | `<unistd.h>` | 파일 디스크립터에 쓰기 |
| `lseek()` | `<unistd.h>` | 파일 오프셋 이동 |
| `dup()` / `dup2()` | `<unistd.h>` | 파일 디스크립터 복제 |
| `mmap()` | `<sys/mman.h>` | 메모리 매핑 |
| `select()` / `poll()` | `<sys/select.h>` / `<poll.h>` | I/O 멀티플렉싱 |
| `getenv()` | `<stdlib.h>` | 환경 변수 조회 |
| `setenv()` | `<stdlib.h>` | 환경 변수 설정 |

---

## 5. 파일 시스템 규약

### 5.1 경로 규약

POSIX 파일 시스템은 단일 루트(/) 트리 구조를 사용한다.

| 규칙 | 설명 |
|------|------|
| 경로 구분자 | `/` (슬래시) |
| 루트 디렉터리 | `/` |
| 현재 디렉터리 | `.` |
| 상위 디렉터리 | `..` |
| 절대 경로 | `/`로 시작 (예: `/usr/bin/sh`) |
| 상대 경로 | `/`로 시작하지 않음 (예: `../lib/libfoo.a`) |
| 파일명 최대 길이 | `NAME_MAX` (최소 14자, 일반적으로 255자) |
| 경로 최대 길이 | `PATH_MAX` (일반적으로 4096자) |
| 허용 문자 | NUL(`\0`)과 `/`을 제외한 모든 바이트 |
| 숨김 파일 관례 | `.`으로 시작하는 파일명 (POSIX 표준은 아니지만 사실상 표준) |

### 5.2 파일 종류

POSIX는 다음 파일 종류를 정의한다:

| 종류 | `ls -l` 표시 | 설명 |
|------|-------------|------|
| 일반 파일 (regular file) | `-` | 데이터를 담는 일반 파일 |
| 디렉터리 (directory) | `d` | 파일 목록을 담는 특수 파일 |
| 심볼릭 링크 (symbolic link) | `l` | 다른 파일을 가리키는 참조 |
| 블록 장치 (block device) | `b` | 블록 단위 I/O 장치 (디스크 등) |
| 문자 장치 (character device) | `c` | 문자 단위 I/O 장치 (터미널 등) |
| FIFO (named pipe) | `p` | 프로세스 간 통신용 파이프 |
| 소켓 (socket) | `s` | 프로세스 간 통신용 소켓 |

### 5.3 권한 모델

POSIX는 소유자(owner) / 그룹(group) / 기타(others) 3단계 권한 모델을 사용한다.

```
-rwxr-xr--  1 user group 4096 Jan 15 10:30 example.sh
 |||||||||||
 |||||||||+-- others: read (r--)
 ||||||++++-- group:  read + execute (r-x)
 |||+++++---- owner:  read + write + execute (rwx)
 |+---------- 파일 종류 (- = 일반 파일)
```

권한 비트:

| 권한 | 8진수 값 | 파일에 대한 의미 | 디렉터리에 대한 의미 |
|------|---------|-----------------|---------------------|
| 읽기 (r) | 4 | 파일 내용 읽기 | 디렉터리 목록 조회 |
| 쓰기 (w) | 2 | 파일 내용 수정 | 파일 생성/삭제 |
| 실행 (x) | 1 | 파일 실행 | 디렉터리 진입(cd) |

특수 권한 비트:

| 비트 | 8진수 | 설명 |
|------|-------|------|
| Set-UID (SUID) | 4000 | 파일 실행 시 소유자 권한으로 실행 |
| Set-GID (SGID) | 2000 | 파일 실행 시 그룹 권한으로 실행 / 디렉터리에서 새 파일이 부모 그룹 상속 |
| Sticky bit | 1000 | 디렉터리에서 파일 소유자만 삭제 가능 (`/tmp` 등에 사용) |

`umask` 메커니즘:

`umask`는 새로 생성되는 파일의 기본 권한에서 제거할 비트를 지정한다.

```bash
$ umask 022
# 새 파일: 0666 & ~022 = 0644 (rw-r--r--)
# 새 디렉터리: 0777 & ~022 = 0755 (rwxr-xr-x)
```

### 5.4 파일 디스크립터

POSIX에서 모든 I/O는 파일 디스크립터(file descriptor)를 통해 이루어진다. 파일 디스크립터는 0 이상의 정수이며, 다음 세 가지가 미리 예약되어 있다:

| 번호 | 이름 | 매크로 | 설명 |
|------|------|--------|------|
| 0 | 표준 입력 | `STDIN_FILENO` | 키보드 입력 |
| 1 | 표준 출력 | `STDOUT_FILENO` | 화면 출력 |
| 2 | 표준 오류 | `STDERR_FILENO` | 오류 출력 |

---

## 6. 셸 스크립트 표준 (sh 호환)

### 6.1 POSIX sh란

POSIX 셸은 Bourne Shell(sh)의 동작을 표준화한 것이다. `bash`, `zsh`, `ksh`, `dash` 등은 모두 POSIX sh의 상위 집합(superset)이다. POSIX 호환 셸 스크립트를 작성하면 이들 셸 어디에서든 동작한다.

### 6.2 POSIX sh에서 사용 가능한 문법

```sh
#!/bin/sh
# POSIX 호환 셸 스크립트 예시

# 변수
name="World"
echo "Hello, ${name}"

# 조건문
if [ "$name" = "World" ]; then
    echo "Standard greeting"
elif [ "$name" = "POSIX" ]; then
    echo "POSIX greeting"
else
    echo "Custom greeting"
fi

# 반복문
for item in a b c; do
    echo "$item"
done

i=0
while [ "$i" -lt 5 ]; do
    echo "$i"
    i=$((i + 1))
done

# 함수
greet() {
    echo "Hello, $1"
}
greet "Developer"

# 명령어 치환
today=$(date +%Y-%m-%d)
echo "Today is $today"

# 산술 확장
result=$((3 + 4 * 2))
echo "Result: $result"

# here document
cat <<EOF
This is
a here document
EOF

# 종료 상태
if command -v git > /dev/null 2>&1; then
    echo "git is installed"
fi
```

### 6.3 POSIX sh에서 사용 불가능한 문법 (bash 확장)

다음 문법은 bash/zsh 등의 확장 기능으로, POSIX sh에서는 사용할 수 없다:

| bash 확장 | POSIX 대안 |
|-----------|-----------|
| `[[ ... ]]` (이중 대괄호) | `[ ... ]` 또는 `test` |
| `array=(a b c)` (배열) | 대안 없음 (변수나 파일 활용) |
| `${var//old/new}` (전역 치환) | `echo "$var" \| sed 's/old/new/g'` |
| `$'...'` (ANSI-C 인용) | `printf '\n'` 등 사용 |
| `function name { ... }` (function 키워드) | `name() { ... }` |
| `source file` | `. file` |
| `<<<` (here string) | `echo "string" \| command` |
| `{1..10}` (브레이스 확장) | `seq 1 10` 또는 반복문 |
| `let` / `((...))` (산술문) | `$((...))` (산술 확장) |
| `local` 키워드 (일부 셸에서 미지원) | 서브셸 또는 변수 관리로 대체 |

### 6.4 이식성 높은 스크립트 작성 팁

1. 셔뱅(shebang)은 `#!/bin/sh`를 사용한다.
2. `echo`보다 `printf`를 선호한다. `echo`의 동작은 플랫폼마다 다를 수 있다 (예: `-n` 옵션, 이스케이프 시퀀스 해석 여부).
3. 명령어 존재 여부는 `command -v`로 확인한다. `which`는 POSIX 표준이 아니다.
4. 변수를 항상 큰따옴표로 감싼다. 단어 분리(word splitting)와 글로빙(globbing) 방지.
5. `test` 또는 `[ ]`을 사용하고, `[[ ]]`는 피한다.

---

## 7. 정규 표현식

POSIX는 두 가지 정규 표현식 문법을 정의한다.

### 7.1 BRE (Basic Regular Expressions)

`grep`, `sed`(기본 모드) 등에서 사용된다.

| 메타 문자 | 의미 |
|-----------|------|
| `.` | 임의의 한 문자 |
| `*` | 앞의 요소 0회 이상 반복 |
| `^` | 줄의 시작 |
| `$` | 줄의 끝 |
| `[...]` | 문자 클래스 |
| `[^...]` | 부정 문자 클래스 |
| `\(...\)` | 그룹 캡처 (역슬래시 필요) |
| `\{m,n\}` | 반복 횟수 지정 (역슬래시 필요) |
| `\1` ~ `\9` | 역참조 |

BRE에서는 `(`, `)`, `{`, `}`를 리터럴로 취급하며, 메타 문자로 사용하려면 역슬래시(`\`)를 붙여야 한다.

```bash
# BRE 예시 (grep)
grep '^#include' file.c            # #include로 시작하는 줄
grep 'err\(or\)\?' file.txt        # err 또는 error (BRE에서 \?는 비표준)
grep 'a\{3,5\}' file.txt           # aaa, aaaa, aaaaa
```

### 7.2 ERE (Extended Regular Expressions)

`grep -E` (또는 `egrep`), `awk`, `sed -E` 등에서 사용된다.

| 메타 문자 | 의미 |
|-----------|------|
| `.` | 임의의 한 문자 |
| `*` | 앞의 요소 0회 이상 반복 |
| `+` | 앞의 요소 1회 이상 반복 |
| `?` | 앞의 요소 0회 또는 1회 |
| `^` | 줄의 시작 |
| `$` | 줄의 끝 |
| `[...]` | 문자 클래스 |
| `(...)` | 그룹 (역슬래시 불필요) |
| `\|` | 택일 (OR) - BRE에서는 미지원 |
| `{m,n}` | 반복 횟수 지정 (역슬래시 불필요) |

ERE에서는 `(`, `)`, `{`, `}`, `+`, `?`, `|`가 역슬래시 없이 메타 문자로 동작한다.

```bash
# ERE 예시 (grep -E)
grep -E '^[0-9]{3}-[0-9]{4}$' file.txt    # 000-0000 형태
grep -E '(error|warning):' log.txt          # error: 또는 warning:
grep -E 'https?://[^ ]+' file.txt           # http:// 또는 https:// URL
```

### 7.3 POSIX 문자 클래스

로캘(locale)에 독립적인 문자 매칭을 위해 POSIX는 다음과 같은 문자 클래스를 정의한다:

| 클래스 | 의미 | 대략적 등가 표현 |
|--------|------|-----------------|
| `[:alnum:]` | 알파벳 + 숫자 | `[a-zA-Z0-9]` |
| `[:alpha:]` | 알파벳 | `[a-zA-Z]` |
| `[:digit:]` | 숫자 | `[0-9]` |
| `[:lower:]` | 소문자 | `[a-z]` |
| `[:upper:]` | 대문자 | `[A-Z]` |
| `[:space:]` | 공백 문자 | `[ \t\n\r\f\v]` |
| `[:blank:]` | 스페이스와 탭 | `[ \t]` |
| `[:print:]` | 인쇄 가능 문자 | 공백 포함 |
| `[:graph:]` | 인쇄 가능 문자 | 공백 제외 |
| `[:punct:]` | 구두점 | 알파벳/숫자/공백 제외 인쇄 가능 문자 |
| `[:cntrl:]` | 제어 문자 | |
| `[:xdigit:]` | 16진수 문자 | `[0-9a-fA-F]` |

사용 시 대괄호 안에 넣어야 한다: `[[:digit:]]`

```bash
grep '[[:upper:]][[:lower:]]*' file.txt   # 대문자로 시작하는 단어
```

---

## 8. POSIX 호환 운영체제 목록

### 8.1 공식 인증된 POSIX 호환 운영체제

The Open Group으로부터 공식 UNIX 인증(UNIX 03 또는 UNIX V7)을 받은 운영체제는 POSIX를 완전히 준수한다:

| 운영체제 | 벤더 | 인증 수준 | 비고 |
|---------|------|-----------|------|
| macOS | Apple | UNIX 03 | 10.5(Leopard)부터 공식 인증 |
| AIX | IBM | UNIX 03 | IBM 메인프레임/서버용 |
| HP-UX | Hewlett-Packard | UNIX 03 | HP 서버용 (사실상 종료) |
| Solaris / illumos | Oracle (이전 Sun) | UNIX 03 | illumos는 오픈소스 파생 |
| z/OS (USS) | IBM | UNIX 03 | 메인프레임용 UNIX 서비스 |
| EulerOS | Huawei | UNIX 03 | Linux 기반 |
| Inspur K-UX | Inspur | UNIX 03 | Linux 기반 |

### 8.2 대부분 호환되는 운영체제 (공식 인증 없음)

| 운영체제 | 호환 수준 | 비고 |
|---------|-----------|------|
| Linux (모든 배포판) | 대부분 호환 | 공식 인증을 받지 않았지만 사실상 거의 완벽히 호환. LSB(Linux Standard Base)를 통해 별도 표준화 |
| FreeBSD | 대부분 호환 | BSD 계열의 대표적 오픈소스 OS |
| OpenBSD | 대부분 호환 | 보안 중심 BSD |
| NetBSD | 대부분 호환 | 이식성 중심 BSD |
| DragonFly BSD | 대부분 호환 | FreeBSD에서 파생 |
| Cygwin | 부분 호환 | Windows 위에서 POSIX API를 에뮬레이션 |
| WSL (Windows Subsystem for Linux) | 대부분 호환 | Windows에서 Linux 커널 실행 (WSL2) |
| MINIX 3 | 부분 호환 | 교육/연구용 마이크로커널 OS |
| QNX | 대부분 호환 | 실시간 운영체제 (자동차, 임베디드) |

### 8.3 POSIX를 지원하지 않는 운영체제

| 운영체제 | 비고 |
|---------|------|
| Windows (네이티브) | Win32 API는 POSIX와 완전히 다른 체계. 과거 Windows NT에 POSIX 서브시스템이 있었으나 제거됨 |
| MS-DOS | 단일 프로세스, 단일 사용자 |

---

## 9. POSIX vs SUS (Single UNIX Specification) 관계

### 9.1 역사적 배경

| 표준 | 관리 주체 | 성격 |
|------|-----------|------|
| POSIX | IEEE (후에 The Open Group과 공동) | 운영체제 인터페이스 표준 |
| SUS | The Open Group (이전 X/Open) | UNIX 상표 사용을 위한 인증 규격 |

### 9.2 관계 정리

```
POSIX (IEEE Std 1003)
  └── 기본 시스템 인터페이스와 셸/유틸리티 정의

SUS (Single UNIX Specification)
  └── POSIX를 포함하고 + 추가 기능(XSI 확장) 정의
  └── UNIX 인증의 기준

                    ┌──────────────────────────┐
                    │        SUS (전체)          │
                    │  ┌────────────────────┐   │
                    │  │  POSIX (기본 표준)   │   │
                    │  │                    │   │
                    │  └────────────────────┘   │
                    │  + XSI 확장 (추가 기능)    │
                    │  + 네트워킹 서비스         │
                    │  + X/Open Curses 등       │
                    └──────────────────────────┘
```

XSI(X/Open System Interface) 확장은 SUS가 POSIX에 추가로 요구하는 기능들이다:

- 국제화(i18n) 관련 추가 함수
- System V IPC (`msgget`, `semget`, `shmget`)
- 추가 유틸리티 (`crontab`, `lp`, `at` 등)
- 더 넓은 범위의 헤더 파일

### 9.3 통합

2001년 이후 POSIX(IEEE Std 1003.1)와 SUS는 공동 개정(joint revision)으로 관리되고 있어, 사실상 같은 문서의 두 가지 이름이다. 다만 SUS는 "UNIX" 상표 인증의 기준이 되고, POSIX는 IEEE/ISO 표준으로서의 역할을 한다.

| SUS 버전 | POSIX 버전 | 연도 |
|---------|-----------|------|
| SUSv3 | IEEE Std 1003.1-2001 | 2001 |
| SUSv4 | IEEE Std 1003.1-2008 | 2008 |
| SUSv4 (2016 Edition) | IEEE Std 1003.1-2017 | 2017 |
| SUSv5 (Issue 8) | IEEE Std 1003.1-2024 | 2024 |

---

## 10. 장점과 단점

### 10.1 장점

| 장점 | 설명 |
|------|------|
| 이식성 (Portability) | POSIX 호환 코드는 Linux, macOS, BSD, AIX, Solaris 등 다양한 운영체제에서 수정 없이 또는 최소한의 수정으로 동작한다. |
| 안정성과 성숙도 | 30년 이상의 역사를 가진 표준으로, 인터페이스가 매우 안정적이다. 한 번 표준화된 API는 쉽게 제거되지 않는다. |
| 벤더 독립성 | 특정 운영체제 벤더에 종속되지 않아, 기술 선택의 자유도가 높다. |
| 풍부한 생태계 | 거의 모든 UNIX 계열 운영체제가 지원하므로, 학습 투자 대비 활용 범위가 넓다. |
| 표준 셸 스크립트 | POSIX sh 호환 스크립트는 어떤 UNIX 계열 시스템에서든 실행 가능하여 인프라 자동화에 유리하다. |
| 보안 모델 | 사용자/그룹 기반 권한 모델이 명확하고 검증되어 있다. |

### 10.2 단점

| 단점 | 설명 |
|------|------|
| 최저 공통 분모 | 모든 OS가 지원할 수 있는 수준에서 표준이 정해지므로, 특정 OS의 고유한 고성능 기능(예: Linux의 `epoll`, BSD의 `kqueue`)을 활용할 수 없다. |
| 느린 표준화 속도 | 새로운 기술(예: 비동기 I/O, 파일 시스템 이벤트 감시)이 표준에 반영되기까지 오랜 시간이 걸린다. |
| 레거시 인터페이스 | `signal()`, `gets()` 등 안전하지 않거나 사용이 권장되지 않는 인터페이스가 호환성을 위해 유지된다. |
| 네트워킹 기능 제한 | 현대 네트워킹에서 필요한 많은 기능(고급 소켓 옵션, 비동기 DNS 등)이 POSIX 범위 밖이다. |
| GUI 미포함 | POSIX는 그래픽 사용자 인터페이스를 전혀 정의하지 않는다. X11, Wayland 등은 별도 표준이다. |
| 인증 비용 | 공식 POSIX/UNIX 인증을 받으려면 The Open Group에 상당한 비용을 지불해야 하므로, Linux 같은 오픈소스 프로젝트는 인증을 받지 않는다. |
| Windows 비호환 | Windows는 완전히 다른 API 체계(Win32)를 사용하므로, POSIX 코드는 Windows에서 직접 실행되지 않는다. |

---

## 11. 실제 활용 사례 및 개발자가 알아야 할 점

### 11.1 실제 활용 사례

#### 크로스 플랫폼 소프트웨어 개발

POSIX API만 사용하면 하나의 코드베이스로 Linux, macOS, BSD를 동시에 지원할 수 있다.

```c
// POSIX API만 사용한 파일 복사 예시
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>

int copy_file(const char *src, const char *dst) {
    int fd_src = open(src, O_RDONLY);
    if (fd_src < 0) return -1;

    int fd_dst = open(dst, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (fd_dst < 0) { close(fd_src); return -1; }

    char buf[8192];
    ssize_t n;
    while ((n = read(fd_src, buf, sizeof(buf))) > 0) {
        write(fd_dst, buf, n);
    }

    close(fd_src);
    close(fd_dst);
    return 0;
}
```

#### CI/CD 파이프라인 및 인프라 자동화

POSIX sh 호환 스크립트는 Docker 컨테이너, CI/CD 서버, 클라우드 인스턴스 등 어디서든 실행할 수 있어 배포 자동화에 널리 사용된다.

```sh
#!/bin/sh
# POSIX 호환 배포 스크립트
set -eu

DEPLOY_DIR="/opt/app"
BACKUP_DIR="/opt/backup/$(date +%Y%m%d_%H%M%S)"

# 백업
if [ -d "$DEPLOY_DIR" ]; then
    mkdir -p "$BACKUP_DIR"
    cp -r "$DEPLOY_DIR" "$BACKUP_DIR"
    printf "Backup created at %s\n" "$BACKUP_DIR"
fi

# 배포
tar -xzf release.tar.gz -C "$DEPLOY_DIR"
printf "Deployment completed\n"
```

#### 임베디드 시스템 및 IoT

QNX, VxWorks 등 실시간 운영체제가 POSIX를 지원하므로, 자동차 소프트웨어, 의료 기기, 항공 시스템 등에서 POSIX API로 개발하면 운영체제 변경 시에도 코드 재사용이 가능하다.

#### 컨테이너 기술

Docker, containerd 등 컨테이너 런타임은 POSIX의 프로세스 관리(`fork`, `exec`), 파일 시스템, 사용자/권한 모델 위에 구축되어 있다. Linux의 네임스페이스와 cgroup은 POSIX를 넘어서는 기능이지만, 기반이 되는 프로세스 모델은 POSIX에 뿌리를 두고 있다.

### 11.2 개발자가 알아야 할 점

#### (1) POSIX 호환 여부를 확인하는 방법

컴파일 시 기능 테스트 매크로(feature test macro)를 사용한다:

```c
#define _POSIX_C_SOURCE 200809L  // POSIX.1-2008 기능 사용 선언
#include <unistd.h>
#include <stdio.h>

int main(void) {
    // _POSIX_VERSION 매크로로 시스템의 POSIX 지원 수준 확인
    printf("POSIX version: %ld\n", _POSIX_VERSION);

    // sysconf()로 런타임에 옵션 지원 여부 확인
    long threads = sysconf(_SC_THREADS);
    if (threads > 0) {
        printf("POSIX Threads supported\n");
    }
    return 0;
}
```

#### (2) 플랫폼별 차이에 대응하기

POSIX 호환이라도 미묘한 차이가 있을 수 있다. 조건부 컴파일로 대응한다:

```c
#ifdef __linux__
    #include <sys/epoll.h>    // Linux 전용 고성능 I/O
#elif defined(__APPLE__)
    #include <sys/event.h>    // macOS/BSD 전용 kqueue
#else
    #include <poll.h>         // POSIX 표준 poll (범용)
#endif
```

#### (3) errno 확인을 습관화하기

POSIX 함수는 실패 시 대부분 -1을 반환하고 `errno`를 설정한다. 반드시 에러를 확인해야 한다:

```c
#include <errno.h>
#include <string.h>

int fd = open("file.txt", O_RDONLY);
if (fd == -1) {
    fprintf(stderr, "open failed: %s (errno=%d)\n",
            strerror(errno), errno);
    // errno 값에 따라 분기 처리
    if (errno == ENOENT) {
        fprintf(stderr, "File not found\n");
    } else if (errno == EACCES) {
        fprintf(stderr, "Permission denied\n");
    }
}
```

#### (4) 시그널 안전 함수 (Async-Signal-Safe Functions)

시그널 핸들러 내에서 호출할 수 있는 함수는 제한되어 있다. `printf()`, `malloc()` 등은 시그널 핸들러에서 호출하면 안 된다. POSIX는 시그널 안전 함수 목록을 명시하고 있다:

```c
// 시그널 핸들러에서 안전한 함수 예시
// write(), _exit(), signal(), kill(), open(), close(), read() 등

void handler(int sig) {
    const char msg[] = "Signal received\n";
    write(STDERR_FILENO, msg, sizeof(msg) - 1);  // 안전
    // printf("Signal received\n");               // 안전하지 않음!
}
```

#### (5) 셸 스크립트의 이식성 검증 도구

- `shellcheck`: 셸 스크립트의 POSIX 호환성 및 일반적인 오류를 검사하는 정적 분석 도구
- `checkbashisms` (Debian 패키지): bash 전용 문법이 POSIX sh 스크립트에 사용되었는지 검사

```bash
# shellcheck으로 POSIX 호환성 검사
shellcheck --shell=sh script.sh
```

#### (6) POSIX 표준 문서 접근 방법

- 공식 온라인 문서: [https://pubs.opengroup.org/onlinepubs/9799919799/](https://pubs.opengroup.org/onlinepubs/9799919799/)
- `man` 페이지에서 POSIX 관련 정보 확인:

```bash
man 3p open    # POSIX 매뉴얼 페이지 (시스템에 따라 설치 필요)
```

#### (7) 실무 권장 사항 요약

| 상황 | 권장 사항 |
|------|-----------|
| 크로스 플랫폼 C/C++ 개발 | POSIX API를 기본으로 사용하고, 플랫폼 전용 기능은 추상화 레이어로 분리 |
| 셸 스크립트 작성 | `#!/bin/sh`로 시작하고, bash 확장 문법 사용을 피함 |
| 고성능 서버 개발 | POSIX `poll()`로 기본 구현 후, 플랫폼별 최적화(`epoll`, `kqueue`) 추가 |
| 빌드 시스템 | `Makefile`에서 POSIX 유틸리티만 사용하면 이식성 확보 |
| 파일 경로 처리 | `/` 구분자 사용, `PATH_MAX` 상수 활용, 절대 경로/상대 경로 명확히 구분 |
| 에러 처리 | 모든 시스템 콜 반환값 확인, `errno` 기반 에러 처리 |
| 스레드 프로그래밍 | `pthread` API 사용, 뮤텍스/조건 변수로 동기화 |

---

## 참고 자료

- [The Open Group - POSIX.1-2024 (Issue 8)](https://pubs.opengroup.org/onlinepubs/9799919799/)
- [IEEE Std 1003.1](https://standards.ieee.org/standard/1003_1-2017.html)
- [POSIX - Wikipedia](https://en.wikipedia.org/wiki/POSIX)
- Stevens, W. Richard. *Advanced Programming in the UNIX Environment*, 3rd Edition. Addison-Wesley, 2013.
- Kerrisk, Michael. *The Linux Programming Interface*. No Starch Press, 2010.
