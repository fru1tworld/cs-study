# Linux 특수 디렉터리 & 커널 인터페이스

리눅스는 *everything is a file* 철학을 따른다. 디바이스, 프로세스 상태, 커널 객체, 컨테이너의 격리 경계까지 — 사용자 공간에서는 전부 파일/디렉터리 인터페이스로 노출된다. 여기서는 그 인터페이스들을 항목별로 정리한다.

기준 자료:
- `man7.org` man pages: `hier(7)`, `proc(5)`, `sysfs(5)`, `tmpfs(5)`, `cgroups(7)`, `namespaces(7)`, `pid_namespaces(7)`, `user_namespaces(7)`, `null(4)`, `random(4)`
- `docs.kernel.org`: Control Group v2

## 인덱스

| 파일 | 주제 | 백엔드 |
|---|---|---|
| [dev.md](dev.md) | `/dev` — 디바이스 파일 (null, zero, random, tty, shm…) | devtmpfs |
| [proc.md](proc.md) | `/proc` — 프로세스 & 커널 상태 | procfs |
| [sys.md](sys.md) | `/sys` — 커널 객체 모델 (kobject 트리) | sysfs |
| [run.md](run.md) | `/run` — 런타임 상태 | tmpfs |
| [tmp.md](tmp.md) | `/tmp` vs `/var/tmp` vs `/dev/shm` | tmpfs/disk |
| [cgroups.md](cgroups.md) | cgroups v1/v2, `/sys/fs/cgroup` | cgroup2 FS |
| [namespaces.md](namespaces.md) | 8가지 namespace, `/proc/<pid>/ns/` | nsfs |

## hier(7) — FHS 한눈에

`man 7 hier`가 정의하는 표준 루트 디렉터리:

| 경로 | 공식 정의 |
|---|---|
| `/` | the root directory. This is where the whole tree starts. |
| `/bin` | executable programs which are needed in single user mode and to bring the system up or repair it. |
| `/boot` | static files for the boot loader. |
| `/dev` | special or device files, which refer to physical devices. |
| `/etc` | configuration files which are local to the machine. |
| `/home` | home directories for users. |
| `/lib` | shared libraries that are necessary to boot the system and to run the commands in the root filesystem. |
| `/media` | mount points for removable media (CD/DVD/USB). |
| `/mnt` | mount point for a temporarily mounted filesystem. |
| `/opt` | add-on packages that contain static files. |
| `/proc` | mount point for the proc filesystem, which provides information about running processes and the kernel. |
| `/root` | home directory for the root user (optional). |
| `/run` | information which describes the system since it was booted. |
| `/sbin` | commands needed to boot the system, but which are usually not executed by normal users. |
| `/srv` | site-specific data that is served by this system. |
| `/sys` | mount point for the sysfs filesystem, which provides information about the kernel like /proc, but better structured. |
| `/tmp` | temporary files which may be deleted with no notice. |
| `/usr` | shareable, read-only data. |
| `/var` | files which may change in size, such as spool and log files. |

이 중 **`/dev`, `/proc`, `/sys`, `/run`, `/tmp`(부분), `/dev/shm`** 은 디스크가 아니라 커널·메모리가 만들어내는 **가상 파일시스템**이라는 게 핵심. 그 위에 cgroups와 namespaces가 컨테이너의 기반을 이룬다.
