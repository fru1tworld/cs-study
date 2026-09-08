# Linux 특수 디렉터리 & 커널 인터페이스

리눅스는 everything is a file 철학에 따라 디바이스, 프로세스 상태, 커널 객체, 컨테이너의 격리 경계를 사용자 공간에 파일과 디렉터리 인터페이스로 노출한다. 여기서는 각 인터페이스가 어떤 정보를 제공하는지 정리한다.

다음 자료를 기준으로 정리했다.

- `man7.org` man pages: `hier(7)`, `proc(5)`, `sysfs(5)`, `tmpfs(5)`, `cgroups(7)`, `namespaces(7)`, `pid_namespaces(7)`, `user_namespaces(7)`, `null(4)`, `random(4)`
- `docs.kernel.org`: Control Group v2

## 인덱스

- [01_dev.md](01_dev.md): `/dev`, 디바이스 파일(null, zero, random, tty, shm 등), 백엔드 devtmpfs
- [02_proc.md](02_proc.md): `/proc`, 프로세스와 커널 상태, 백엔드 procfs
- [03_sys.md](03_sys.md): `/sys`, 커널 객체 모델(kobject 트리), 백엔드 sysfs
- [04_run.md](04_run.md): `/run`, 런타임 상태, 백엔드 tmpfs
- [05_tmp.md](05_tmp.md): `/tmp` vs `/var/tmp` vs `/dev/shm`, 백엔드 tmpfs/disk
- [06_cgroups.md](06_cgroups.md): cgroups v1/v2, `/sys/fs/cgroup`, 백엔드 cgroup2 FS
- [07_namespaces.md](07_namespaces.md): 8가지 namespace, `/proc/<pid>/ns/`, 백엔드 nsfs

## hier(7): FHS 한눈에

`man 7 hier`는 표준 루트 디렉터리를 다음과 같이 설명한다.

- `/`: the root directory. This is where the whole tree starts.
- `/bin`: executable programs which are needed in single user mode and to bring the system up or repair it.
- `/boot`: static files for the boot loader.
- `/dev`: special or device files, which refer to physical devices.
- `/etc`: configuration files which are local to the machine.
- `/home`: home directories for users.
- `/lib`: shared libraries that are necessary to boot the system and to run the commands in the root filesystem.
- `/media`: mount points for removable media (CD/DVD/USB).
- `/mnt`: mount point for a temporarily mounted filesystem.
- `/opt`: add-on packages that contain static files.
- `/proc`: mount point for the proc filesystem, which provides information about running processes and the kernel.
- `/root`: home directory for the root user (optional).
- `/run`: information which describes the system since it was booted.
- `/sbin`: commands needed to boot the system, but which are usually not executed by normal users.
- `/srv`: site-specific data that is served by this system.
- `/sys`: mount point for the sysfs filesystem, which provides information about the kernel like /proc, but better structured.
- `/tmp`: temporary files which may be deleted with no notice.
- `/usr`: shareable, read-only data.
- `/var`: files which may change in size, such as spool and log files.

이 중 `/dev`, `/proc`, `/sys`, `/run`, `/tmp`(부분), `/dev/shm`은 커널과 메모리가 제공하는 가상 파일시스템이다. 여기에 cgroups와 namespaces를 함께 사용해 컨테이너의 실행 환경을 구성한다.
