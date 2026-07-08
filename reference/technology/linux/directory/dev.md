# `/dev` — 디바이스 파일

> "special or device files, which refer to physical devices" — `hier(7)`

커널이 노출하는 디바이스를 파일처럼 다루는 디렉터리. 실제 디스크 블록이 아니라 **`devtmpfs`** — 커널이 부팅 시 자동으로 만드는 가상 파일시스템이다. `udev`(또는 systemd-udevd)가 그 위에 디바이스 노드를 동적으로 추가/이름 부여한다.

## 핵심 가상 디바이스

### `/dev/null` — bit bucket

`null(4)` 공식 정의:
> "Data written to the `/dev/null` and `/dev/zero` special files is discarded."
> "Reads from `/dev/null` always return end of file (i.e., `read(2)` returns 0)."

- 쓰면 → 버려짐 (write가 즉시 성공)
- 읽으면 → 즉시 EOF
- 용도: 출력 폐기 (`cmd > /dev/null 2>&1`)

### `/dev/zero` — 영원한 0 공급원

`null(4)`:
> "Reads from `/dev/zero` always return bytes containing zero (`'\0'` characters)."

- 쓰면 → 버려짐
- 읽으면 → 무한히 `0x00`
- 용도: 파일 0으로 초기화, 메모리 매핑 (`mmap`), 스왑 파일 생성

```bash
dd if=/dev/zero of=disk.img bs=1M count=1024
```

### `/dev/full` — 항상 디스크 풀

- 쓰면 → 항상 `ENOSPC` 반환
- 읽으면 → 0
- 용도: "디스크 가득 찼을 때" 시나리오 테스트

### `/dev/random` & `/dev/urandom`

`random(4)` 공식 표현:

**`/dev/random`**: "a legacy interface" — "will return random bytes only within the estimated number of bits of fresh noise in the entropy pool, blocking if necessary."

**`/dev/urandom`**: "returns random bytes using a pseudorandom number generator seeded from the entropy pool" — "reads from this device do not block."

핵심 차이:
- `/dev/random`은 엔트로피 부족 시 **블록**. Linux 5.6부터는 초기 부팅 이후엔 사실상 블로킹 안 함.
- `/dev/urandom`은 절대 블록하지 않음.

**공식 권고**:
> "The `/dev/random` interface is considered a legacy interface, and `/dev/urandom` is preferred and sufficient in all use cases, with the exception of applications which require randomness during early boot time."

초기 부팅 시 난수가 필요하면 → **`getrandom(2)` syscall** (Linux 3.17+). 엔트로피 풀 초기화까지 블록하므로 안전.

### 터미널 디바이스

- `/dev/tty` — 호출한 프로세스의 제어 터미널
- `/dev/console` — 시스템 콘솔
- `/dev/pts/*` — 의사 터미널 (pseudo terminal). SSH/tmux 세션마다 하나씩.
- `/dev/ptmx` — pty 마스터 멀티플렉서

### 블록 디바이스

- `/dev/sda`, `/dev/sda1` — SCSI/SATA 디스크와 파티션
- `/dev/nvme0n1`, `/dev/nvme0n1p1` — NVMe
- `/dev/vda` — virtio (KVM/QEMU 게스트)
- `/dev/mapper/*` — LVM, dm-crypt 등 device-mapper

### 루프 디바이스

- `/dev/loop0`, `/dev/loop1`… — **파일을 블록 디바이스처럼** 마운트할 수 있게 해줌
- `losetup -f --show disk.img` → `/dev/loopN` 할당

### 공유 메모리

- `/dev/shm` — **tmpfs** 마운트 포인트. POSIX 공유 메모리(`shm_open(3)`)의 백엔드.
- 프로세스 간 빠른 데이터 공유에 사용. RAM 기반이라 빠르지만 재부팅 시 사라짐.

### 표준 입출력 링크

- `/dev/stdin` → `/proc/self/fd/0`
- `/dev/stdout` → `/proc/self/fd/1`
- `/dev/stderr` → `/proc/self/fd/2`
- `/dev/fd` → `/proc/self/fd` — Bash 프로세스 치환 `<(cmd)`이 이걸 활용

## 디바이스 파일의 정체

`ls -l`로 보면 일반 파일과 다른 타입 식별자가 붙는다:

```
crw-rw-rw- 1 root root 1, 3 May 13 12:00 /dev/null    # c = character
brw-rw---- 1 root disk 8, 0 May 13 12:00 /dev/sda     # b = block
```

- **major number** (앞 숫자): 어떤 드라이버가 다룰지
- **minor number** (뒤 숫자): 같은 드라이버 내 인스턴스
- `mknod(1)`로 직접 만들 수도 있음 (보통은 udev가 자동 처리)
- `stat(2)`의 `st_rdev`로 major:minor 추출 가능

## udev와의 관계

- 부팅 시 커널이 devtmpfs를 `/dev`에 마운트하고 최소한의 노드를 만듦
- `systemd-udevd`가 `/etc/udev/rules.d/`, `/usr/lib/udev/rules.d/` 룰을 읽어 영구 심볼릭 링크 생성
  - 예: `/dev/disk/by-uuid/<UUID>` → `/dev/sda1`
  - 예: `/dev/disk/by-partlabel/<label>` → 해당 파티션
- 핫플러그(USB 꽂기 등) 이벤트도 udev가 처리

## 컨테이너 / namespace 측면

- 컨테이너는 보통 **자체** `/dev`를 마운트한다 (devtmpfs를 새로 또는 일부만 bind-mount).
- 보안을 위해 `/dev/mem`, `/dev/kmsg`, 디스크 노드는 보통 노출 안 함.
- `--device` 옵션(Docker/Podman)은 특정 노드만 컨테이너 안으로 통과시킨다.
