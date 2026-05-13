# `/tmp` vs `/var/tmp` vs `/dev/shm`

세 개 다 "임시 파일 둘 곳"인데 **수명·백엔드·용도가 다르다**. `hier(7)`과 `tmpfs(5)` 기준 정리.

## 한눈에

| 경로 | 수명 | 백엔드 | 용도 |
|---|---|---|---|
| `/tmp` | 재부팅 시 정리 (또는 더 짧게) | 보통 tmpfs (RAM), 또는 디스크 | 일반 임시 파일 |
| `/var/tmp` | **재부팅 후에도 유지** | 디스크 | 오래 살아야 하는 임시 파일 |
| `/dev/shm` | 재부팅 시 사라짐 | tmpfs (RAM) | POSIX 공유 메모리 |
| `/run` | 재부팅 시 사라짐 | tmpfs (RAM) | 런타임 상태 ([run.md](run.md)) |

## `/tmp`

> "contains temporary files which may be deleted with no notice, such as by a regular job or at system boot up" — `hier(7)`

특징:
- **누가 언제든 지워도 정상**. 프로그램은 절대 `/tmp`의 영속성을 가정하면 안 됨.
- 현대 배포판(systemd 기본)에서 `/tmp`는 보통 **tmpfs** — `tmpfs /tmp tmpfs defaults,nosuid,nodev`
- systemd가 제공하는 `tmp.mount` 유닛이 이걸 관리. 비활성화하면 디스크 `/tmp`로 폴백.
- `systemd-tmpfiles`의 기본 정책: **10일** 이상 안 건드린 파일 자동 삭제 (`/usr/lib/tmpfiles.d/tmp.conf`).
- 권한: `1777` (sticky bit). 누구나 쓸 수 있지만 **자기 파일만 지울 수 있음**.

보안 주의:
- 다른 사용자가 같이 쓰는 디렉터리이므로 **race condition 취약**. 
- 안전한 임시 파일은 `mkstemp(3)` / `mkdtemp(3)` (커널이 atomic하게 unique 파일/디렉터리 생성).
- `O_TMPFILE` 플래그(Linux 3.11+): 이름 없는 임시 파일 — `open(dir, O_TMPFILE | O_RDWR, ...)`.
- 또는 **private `/tmp`** — systemd 유닛의 `PrivateTmp=yes`가 mount namespace로 분리.

## `/var/tmp`

> (FHS) "Temporary files preserved between system reboots."

특징:
- **재부팅 후에도 살아남는** 임시 파일.
- 디스크 기반 (tmpfs 아님).
- `systemd-tmpfiles` 기본: **30일** 자동 삭제.
- 권한 `1777` (sticky bit).
- 용도: 컴파일 캐시, 큰 다운로드 중간 파일, 재부팅 거쳐 이어가야 하는 작업.

`/tmp` vs `/var/tmp` 선택 기준:
- 빨라야 하고 재부팅 후 살아남을 필요 없음 → `/tmp` (tmpfs라면 RAM)
- 크거나, RAM에 안 들어가거나, 재부팅 거쳐 살아남아야 함 → `/var/tmp`

## `/dev/shm`

> `tmpfs(5)`에 따르면, 표준 배치 중 하나가 "`/dev/shm` for POSIX shared memory and semaphores"

특징:
- **tmpfs** 마운트, 권한 `1777`.
- **POSIX 공유 메모리**(`shm_open(3)`, `shm_unlink(3)`, `sem_open(3)`) 백엔드.
- 프로세스 간 빠른 데이터 공유 — `mmap`으로 공유 영역 만들고 여러 프로세스가 동시에 접근.
- 일반 파일을 두는 곳은 아님 (관습적으로 `/tmp`나 `/run` 사용).
- 컨테이너에서는 보통 작은 크기 제한이 걸려 있어 (`docker run --shm-size=...`로 조정), 큰 데이터 공유 시 부족할 수 있음.

`shm_open` 동작:
```
shm_open("/myshm", O_CREAT|O_RDWR, 0666)
  → /dev/shm/myshm 파일을 만들고 fd 반환
mmap(NULL, len, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0)
  → 가상 메모리에 매핑
```

## tmpfs 자체 정리 (`tmpfs(5)`)

> "a virtual memory filesystem"

| 성질 | 내용 |
|---|---|
| 백엔드 | RAM + 필요시 swap |
| 메모리 사용 | **현재 내용만큼만** 동적 사용 |
| 크기 변경 | remount로 변경 가능, 데이터 손실 없음 |
| 영속성 | umount/재부팅 시 모두 소실 |
| 적용 | `/tmp` (선택적), `/run`, `/dev/shm`, 사용자별 `/run/user/<uid>` |

### 주요 마운트 옵션

| 옵션 | 의미 |
|---|---|
| `size=` | 상한 (바이트, `k`/`m`/`g` 접미사, 또는 `%` — 기본 RAM의 50%) |
| `nr_blocks=` | PAGE_CACHE_SIZE 블록 수 |
| `nr_inodes=` | 최대 inode 수 (기본 RAM 페이지 수의 절반) |
| `mode=` | 루트 디렉터리 권한 |
| `uid=`, `gid=` | 루트 디렉터리 소유 |
| `huge=` | HugeTLB 정책 (`never`/`always`/`within_size`/`advise`/`deny`/`force`) |
| `noswap` | 스왑 비활성 (6.4+) |

## 자주 쓰는 패턴

```bash
# 어떤 파일시스템인지 확인
findmnt /tmp /var/tmp /dev/shm /run

# /tmp 사용량
df -h /tmp

# tmpfs 크기 변경 (예: /tmp를 8G로)
mount -o remount,size=8G /tmp

# 안전한 임시 파일/디렉터리
TMPFILE=$(mktemp)             # /tmp/tmp.XXXXXXXX
TMPDIR=$(mktemp -d)
trap "rm -rf $TMPDIR" EXIT

# Bash: 환경변수 TMPDIR 우선
export TMPDIR=/var/tmp        # 큰 작업은 디스크로
```

## systemd 유닛에서의 격리

서비스가 `/tmp`를 다른 서비스와 공유하지 않게 하려면 유닛 파일에:

```ini
[Service]
PrivateTmp=yes
```

이렇게 하면 그 서비스는 **자기만의 mount namespace 안에서 새 tmpfs를 `/tmp`와 `/var/tmp`에 마운트**한다. 서비스가 종료되면 그 tmpfs는 사라진다. (mount namespace에 대한 자세한 내용은 [namespaces.md](namespaces.md))
