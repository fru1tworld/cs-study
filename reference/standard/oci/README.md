# OCI (Open Container Initiative)

## 목차

1. [개요](#1-개요)
2. [역사](#2-역사)
3. [OCI의 3가지 핵심 명세](#3-oci의-3가지-핵심-명세)
4. [Runtime Specification 상세](#4-runtime-specification-상세)
5. [Image Specification 상세](#5-image-specification-상세)
6. [Distribution Specification 상세](#6-distribution-specification-상세)
7. [주요 구현체](#7-주요-구현체)
8. [OCI와 Docker, Kubernetes의 관계](#8-oci와-docker-kubernetes의-관계)
9. [장점과 단점](#9-장점과-단점)
10. [실제 활용 사례](#10-실제-활용-사례)

---

## 1. 개요

### OCI란 무엇인가

OCI(Open Container Initiative) 는 컨테이너 기술에 대한 개방형 업계 표준을 만들기 위해 설립된 오픈소스 프로젝트이다. Linux Foundation 산하의 거버넌스 구조 아래 운영되며, 컨테이너의 런타임(Runtime), 이미지(Image), 배포(Distribution) 에 대한 명세(Specification)를 정의한다.

### 왜 만들어졌는가

컨테이너 기술이 급속히 성장하면서, Docker가 사실상의 표준(de facto standard)으로 자리잡았지만 다음과 같은 문제가 발생했다.

- 벤더 종속(Vendor Lock-in): Docker 하나의 구현에 전체 생태계가 의존하는 구조는 건강하지 않았다.
- 상호운용성 부재: 서로 다른 컨테이너 런타임, 이미지 포맷, 레지스트리 간에 호환성이 보장되지 않았다.
- 경쟁 표준의 등장: CoreOS가 자체적으로 appc(App Container) 명세를 발표하면서 표준 분열의 위험이 생겼다.
- 업계 합의의 필요성: 클라우드 네이티브 생태계가 성숙하려면, 특정 벤더에 종속되지 않는 개방형 표준이 필수적이었다.

OCI는 이러한 문제를 해결하기 위해, 컨테이너 기술의 핵심 요소들을 표준화하여 이식성(Portability) 과 상호운용성(Interoperability) 을 보장하는 것을 목표로 한다.

---

## 2. 역사

### Docker의 등장과 컨테이너 혁명

- 2013년: Docker가 PyCon에서 처음 공개되었다. Linux 커널의 cgroups와 namespace 기술을 활용한 컨테이너화를 쉽게 사용할 수 있게 만들어, 개발 및 배포 방식에 혁명을 일으켰다.
- 2013~2015년: Docker는 폭발적으로 성장하며, 컨테이너 이미지 포맷과 런타임의 사실상 표준이 되었다. 하지만 이는 동시에 Docker라는 단일 벤더에 대한 의존도를 높였다.

### CoreOS와 appc (App Container Specification)

- 2014년 12월: CoreOS는 Docker의 독점적 지위에 대한 우려를 표명하며, appc(App Container Specification) 라는 대안 컨테이너 명세와 이를 구현한 rkt(로켓) 런타임을 발표했다.
- appc는 컨테이너 이미지 포맷(ACI, App Container Image), 런타임 환경, 이미지 검증 및 서명 등을 정의했다.
- 이로 인해 컨테이너 업계에 표준 분열(Fragmentation) 의 위기가 찾아왔다. 두 개의 경쟁 표준이 존재하면 생태계 전체에 혼란이 올 수 있었다.

### OCI 설립 (2015년)

- 2015년 6월: Docker, CoreOS, Google, Microsoft, Amazon, IBM, Red Hat 등 주요 기업들이 모여 Open Container Initiative(OCI) 를 Linux Foundation 산하에 설립했다.
- Docker는 자사의 컨테이너 런타임인 libcontainer 의 코드를 기부하여 runc 프로젝트의 기반으로 삼았다. 이것이 OCI 런타임 명세의 참조 구현(Reference Implementation)이 되었다.
- CoreOS는 appc의 아이디어와 경험을 OCI에 반영하기로 합의하고, 별도 표준 추진을 중단했다. (이후 rkt도 CNCF에 기부되었다가 2020년에 아카이브되었다.)
- 설립 당시 목표는 크게 두 가지였다:
  1. 컨테이너 런타임 표준 (Runtime Specification)
  2. 컨테이너 이미지 포맷 표준 (Image Specification)

### 주요 마일스톤

| 시점 | 사건 |
|------|------|
| 2015년 6월 | OCI 설립, runc 초기 코드 기부 |
| 2017년 7월 | Runtime Spec v1.0 및 Image Spec v1.0 공식 릴리스 |
| 2018년 | Distribution Spec 작업 시작 |
| 2021년 | Image Spec v1.1 작업 시작 (아티팩트 지원 등) |
| 2023년 | Distribution Spec v1.1 및 Image Spec v1.1 릴리스 |
| 2024년 | Runtime Spec v1.2 릴리스 |

---

## 3. OCI의 3가지 핵심 명세

OCI는 컨테이너 생태계의 핵심 영역을 세 가지 명세로 표준화한다.

```
+---------------------+     +---------------------+     +---------------------+
|   Image Spec        |     |  Distribution Spec  |     |   Runtime Spec      |
|   (image-spec)      |     |  (distribution-spec)|     |   (runtime-spec)    |
|                     |     |                     |     |                     |
|  컨테이너 이미지의   |     |  이미지를 저장하고   |     |  컨테이너를 실행하는 |
|  포맷과 구조를 정의  |---->|  배포하는 방법을 정의 |---->|  방법을 정의         |
|                     |     |                     |     |                     |
+---------------------+     +---------------------+     +---------------------+

  "이미지를 어떻게        "이미지를 어떻게          "컨테이너를 어떻게
   만들 것인가?"           주고받을 것인가?"          실행할 것인가?"
```

### 3.1 Runtime Specification (runtime-spec)

- GitHub: `opencontainers/runtime-spec`
- 목적: 컨테이너의 설정(configuration), 실행 환경(execution environment), 생명주기(lifecycle)를 표준화한다.
- 핵심 내용: 컨테이너가 어떤 파일시스템 번들(filesystem bundle)을 사용하고, 어떤 격리 기술(namespace, cgroups 등)을 적용하며, 어떤 상태 전이를 거치는지를 정의한다.
- 참조 구현: runc

### 3.2 Image Specification (image-spec)

- GitHub: `opencontainers/image-spec`
- 목적: 컨테이너 이미지의 포맷을 표준화하여, 어떤 빌드 도구로 만들든, 어떤 런타임에서든 실행 가능하도록 한다.
- 핵심 내용: 이미지 매니페스트(Manifest), 이미지 인덱스(Index), 파일시스템 레이어(Layer), 이미지 설정(Configuration)의 구조를 정의한다.
- 설계 원칙: 콘텐츠 주소 지정 방식(Content-addressable) -- 모든 구성 요소는 SHA-256 해시로 식별된다.

### 3.3 Distribution Specification (distribution-spec)

- GitHub: `opencontainers/distribution-spec`
- 목적: 컨테이너 이미지를 레지스트리(Registry)에 저장하고, 가져오는(push/pull) API를 표준화한다.
- 핵심 내용: HTTP 기반의 RESTful API를 정의하며, Docker Registry HTTP API V2를 기반으로 발전시켰다.
- 배경: 원래 Docker Distribution 프로젝트에서 비롯된 것으로, OCI가 이를 공식 명세로 채택했다.

---

## 4. Runtime Specification 상세

Runtime Spec은 컨테이너를 실행하기 위한 표준을 정의한다. 핵심은 파일시스템 번들(Filesystem Bundle) 과 config.json 이다.

### 4.1 파일시스템 번들 (Filesystem Bundle)

OCI 런타임이 컨테이너를 생성하려면, 다음으로 구성된 파일시스템 번들이 필요하다.

```
container-bundle/
├── config.json          # 컨테이너 설정 파일 (필수)
└── rootfs/              # 컨테이너의 루트 파일시스템 (필수)
    ├── bin/
    ├── etc/
    ├── lib/
    ├── usr/
    └── ...
```

- `config.json`: 컨테이너의 모든 설정을 담고 있는 JSON 파일
- `rootfs/`: 컨테이너 내부에서 `/`로 마운트될 루트 파일시스템 디렉터리

### 4.2 config.json 구조

`config.json`은 컨테이너의 실행 환경을 완전히 기술하는 파일이다. 주요 필드는 다음과 같다.

```json
{
    "ociVersion": "1.0.2",
    "process": {
        "terminal": true,
        "user": {
            "uid": 0,
            "gid": 0
        },
        "args": ["/bin/sh"],
        "env": [
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "TERM=xterm"
        ],
        "cwd": "/",
        "capabilities": {
            "bounding": ["CAP_AUDIT_WRITE", "CAP_KILL", "CAP_NET_BIND_SERVICE"],
            "effective": ["CAP_AUDIT_WRITE", "CAP_KILL", "CAP_NET_BIND_SERVICE"],
            "permitted": ["CAP_AUDIT_WRITE", "CAP_KILL", "CAP_NET_BIND_SERVICE"]
        },
        "rlimits": [
            {
                "type": "RLIMIT_NOFILE",
                "hard": 1024,
                "soft": 1024
            }
        ],
        "noNewPrivileges": true
    },
    "root": {
        "path": "rootfs",
        "readonly": false
    },
    "hostname": "my-container",
    "mounts": [
        {
            "destination": "/proc",
            "type": "proc",
            "source": "proc"
        },
        {
            "destination": "/dev",
            "type": "tmpfs",
            "source": "tmpfs",
            "options": ["nosuid", "strictatime", "mode=755", "size=65536k"]
        },
        {
            "destination": "/sys",
            "type": "sysfs",
            "source": "sysfs",
            "options": ["nosuid", "noexec", "nodev", "ro"]
        }
    ],
    "linux": {
        "namespaces": [
            {"type": "pid"},
            {"type": "network"},
            {"type": "ipc"},
            {"type": "uts"},
            {"type": "mount"},
            {"type": "user"},
            {"type": "cgroup"}
        ],
        "resources": {
            "memory": {
                "limit": 536870912
            },
            "cpu": {
                "shares": 1024,
                "quota": 100000,
                "period": 100000
            },
            "pids": {
                "limit": 100
            }
        },
        "seccomp": {
            "defaultAction": "SCMP_ACT_ERRNO",
            "architectures": ["SCMP_ARCH_X86_64"],
            "syscalls": [
                {
                    "names": ["read", "write", "exit", "exit_group", "mmap"],
                    "action": "SCMP_ACT_ALLOW"
                }
            ]
        },
        "maskedPaths": [
            "/proc/kcore",
            "/proc/latency_stats",
            "/proc/timer_list"
        ],
        "readonlyPaths": [
            "/proc/asound",
            "/proc/bus",
            "/proc/fs"
        ]
    }
}
```

#### 주요 필드 설명

| 필드 | 설명 |
|------|------|
| `ociVersion` | 준수하는 OCI Runtime Spec 버전 |
| `process` | 컨테이너 내에서 실행할 프로세스 정보 (명령어, 환경변수, 사용자, 권한 등) |
| `root` | 컨테이너의 루트 파일시스템 경로와 읽기 전용 여부 |
| `hostname` | 컨테이너의 호스트 이름 |
| `mounts` | 컨테이너에 마운트할 파일시스템 목록 (proc, sysfs, tmpfs 등) |
| `linux` | Linux 특화 설정 (namespace, cgroups, seccomp, apparmor 등) |
| `linux.namespaces` | 적용할 Linux 네임스페이스 목록 |
| `linux.resources` | cgroups를 통한 리소스 제한 (CPU, 메모리, PID 등) |
| `linux.seccomp` | 시스템 콜 필터링 정책 |

### 4.3 컨테이너 생명주기 (Lifecycle)

OCI Runtime Spec은 컨테이너의 상태 전이를 다음과 같이 정의한다.

```
                  create
  (없음) ─────────────────> creating ──────> created
                                                │
                                          start │
                                                v
                                             running
                                                │
                                    (프로세스 종료 또는 kill)
                                                │
                                                v
                                             stopped
                                                │
                                         delete │
                                                v
                                             (없음)
```

#### 상태(State)

| 상태 | 설명 |
|------|------|
| `creating` | 컨테이너가 생성 중인 상태 |
| `created` | 컨테이너가 생성되었지만, 사용자 프로세스가 아직 실행되지 않은 상태 |
| `running` | 사용자 프로세스가 실행 중인 상태 |
| `stopped` | 사용자 프로세스가 종료된 상태 |

#### 연산(Operations)

OCI 호환 런타임은 다음의 연산을 반드시 지원해야 한다.

| 연산 | 설명 |
|------|------|
| `create <container-id> <bundle-path>` | 파일시스템 번들로부터 컨테이너를 생성한다. `config.json`을 읽어 환경을 설정하되, 사용자 프로세스는 아직 시작하지 않는다. |
| `start <container-id>` | 생성된 컨테이너의 사용자 프로세스를 시작한다. |
| `state <container-id>` | 컨테이너의 현재 상태를 JSON으로 반환한다. |
| `kill <container-id> <signal>` | 컨테이너 프로세스에 시그널을 보낸다. |
| `delete <container-id>` | 중지된 컨테이너의 리소스를 정리하고 삭제한다. |

#### Hooks (생명주기 후크)

컨테이너 생명주기의 특정 시점에 사용자 정의 스크립트를 실행할 수 있다.

| 후크 | 실행 시점 |
|------|----------|
| `prestart` | `start` 이후, 사용자 프로세스 시작 전 (deprecated, createRuntime으로 대체) |
| `createRuntime` | 런타임 환경이 생성된 직후 |
| `createContainer` | 컨테이너 네임스페이스 생성 후, pivot_root 전 |
| `startContainer` | 사용자 프로세스 시작 직전 |
| `poststart` | 사용자 프로세스 시작 직후 |
| `poststop` | 컨테이너 삭제 직후 |

### 4.4 Linux 격리 기술

#### Linux Namespaces

네임스페이스는 프로세스가 볼 수 있는 시스템 리소스의 범위를 격리한다.

| 네임스페이스 | 설정 값 | 격리 대상 |
|-------------|---------|----------|
| Mount | `mount` | 파일시스템 마운트 포인트. 컨테이너 자체의 마운트 트리를 갖는다. |
| PID | `pid` | 프로세스 ID. 컨테이너 내부에서 PID 1부터 시작한다. |
| Network | `network` | 네트워크 인터페이스, IP 주소, 라우팅 테이블, 포트 등을 격리한다. |
| IPC | `ipc` | System V IPC, POSIX 메시지 큐 등 프로세스 간 통신을 격리한다. |
| UTS | `uts` | 호스트명과 도메인명을 격리한다. |
| User | `user` | UID/GID 매핑. 컨테이너 내부의 root를 호스트의 비특권 사용자로 매핑 가능하다. |
| Cgroup | `cgroup` | cgroup 루트 디렉터리를 격리한다. |

#### Cgroups (Control Groups)

cgroups는 프로세스 그룹의 리소스 사용을 제한하고 모니터링한다.

```json
"resources": {
    "memory": {
        "limit": 536870912,
        "reservation": 268435456,
        "swap": 536870912,
        "disableOOMKiller": false
    },
    "cpu": {
        "shares": 1024,
        "quota": 50000,
        "period": 100000,
        "cpus": "0-3"
    },
    "pids": {
        "limit": 512
    },
    "blockIO": {
        "weight": 500,
        "throttleReadBpsDevice": [
            {
                "major": 8,
                "minor": 0,
                "rate": 104857600
            }
        ]
    }
}
```

| 리소스 | 설명 |
|--------|------|
| `memory.limit` | 최대 메모리 사용량 (바이트 단위) |
| `memory.reservation` | 소프트 리밋. 시스템 메모리가 부족할 때 적용된다. |
| `cpu.shares` | CPU 상대적 가중치 (다른 컨테이너와의 비율) |
| `cpu.quota` / `cpu.period` | CPU 시간 할당. quota/period 비율만큼 CPU를 사용한다. |
| `pids.limit` | 최대 프로세스 수. 포크 밤(Fork Bomb)을 방지한다. |
| `blockIO` | 블록 I/O 대역폭 제한 |

> 참고: Linux 커널 4.5 이후 cgroups v2가 도입되었으며, OCI Runtime Spec도 cgroups v2를 지원한다. cgroups v2는 통합된 계층 구조와 더 세밀한 리소스 제어를 제공한다.

#### Seccomp (Secure Computing Mode)

seccomp는 컨테이너 프로세스가 호출할 수 있는 시스템 콜을 제한하여 공격 표면을 줄인다.

```json
"seccomp": {
    "defaultAction": "SCMP_ACT_ERRNO",
    "architectures": ["SCMP_ARCH_X86_64", "SCMP_ARCH_X86"],
    "syscalls": [
        {
            "names": [
                "accept", "bind", "clone", "close", "connect",
                "execve", "exit", "exit_group", "fork",
                "read", "write", "openat", "mmap", "mprotect"
            ],
            "action": "SCMP_ACT_ALLOW"
        },
        {
            "names": ["mount", "umount2", "reboot", "swapon"],
            "action": "SCMP_ACT_ERRNO",
            "errnoRet": 1
        }
    ]
}
```

| 액션 | 설명 |
|------|------|
| `SCMP_ACT_ALLOW` | 시스템 콜 허용 |
| `SCMP_ACT_ERRNO` | 에러 코드 반환으로 거부 |
| `SCMP_ACT_KILL` | 해당 시스템 콜 호출 시 프로세스를 종료 |
| `SCMP_ACT_TRAP` | SIGSYS 시그널 발생 |
| `SCMP_ACT_LOG` | 허용하되 로그 기록 |

기본적으로 Docker/containerd는 약 300여 개의 시스템 콜 중 위험한 약 40~50개를 차단하는 기본 seccomp 프로파일을 제공한다.

---

## 5. Image Specification 상세

OCI Image Spec은 컨테이너 이미지의 포맷과 구조를 정의한다. 핵심 원칙은 콘텐츠 주소 지정(Content Addressability) 으로, 모든 구성 요소가 그 내용의 SHA-256 해시로 고유하게 식별된다.

### 5.1 전체 구조

```
OCI Image Layout
├── oci-layout              # OCI 이미지 레이아웃 버전 정보
├── index.json              # 이미지 인덱스 (진입점)
└── blobs/
    └── sha256/
        ├── <manifest-digest>     # 이미지 매니페스트
        ├── <config-digest>       # 이미지 설정 (config)
        ├── <layer1-digest>       # 파일시스템 레이어 1
        ├── <layer2-digest>       # 파일시스템 레이어 2
        └── ...
```

### 5.2 이미지 매니페스트 (Manifest)

매니페스트는 하나의 컨테이너 이미지를 구성하는 요소들(설정 + 레이어들)을 기술하는 JSON 문서이다.

```json
{
    "schemaVersion": 2,
    "mediaType": "application/vnd.oci.image.manifest.v1+json",
    "config": {
        "mediaType": "application/vnd.oci.image.config.v1+json",
        "digest": "sha256:b5b2b2c507a0944348e0303114d8d93aaaa081732b86451d9bce1f432a537bc7",
        "size": 7023
    },
    "layers": [
        {
            "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
            "digest": "sha256:9834876dcfb05cb167a5c24953eba58c4ac89b1adf57f28f2f9d09af107ee8f0",
            "size": 32654
        },
        {
            "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
            "digest": "sha256:3c3a4604a545cdc127456d94e421cd355bca5b528f4a9c1905b15da2eb4a4c6b",
            "size": 16724
        },
        {
            "mediaType": "application/vnd.oci.image.layer.v1.tar+gzip",
            "digest": "sha256:ec4b8955958665577945c89419d1af06b5f7636b4ac3da7f12184802ad867736",
            "size": 73109
        }
    ],
    "annotations": {
        "com.example.key1": "value1",
        "org.opencontainers.image.created": "2024-01-01T00:00:00Z"
    }
}
```

| 필드 | 설명 |
|------|------|
| `schemaVersion` | 스키마 버전. 현재 `2`이다. |
| `mediaType` | 이 문서의 미디어 타입 |
| `config` | 이미지 설정 파일의 디스크립터 (digest, size, mediaType) |
| `layers` | 파일시스템 레이어 배열. 순서가 중요하며, 아래에서 위로 겹쳐진다. |
| `annotations` | 선택적 메타데이터 (키-값 쌍) |

### 5.3 이미지 인덱스 (Image Index / Manifest List)

이미지 인덱스는 멀티 플랫폼 이미지 를 지원하기 위한 상위 수준의 매니페스트이다. 하나의 이미지 태그가 여러 아키텍처/OS 조합을 가리킬 수 있게 해준다.

```json
{
    "schemaVersion": 2,
    "mediaType": "application/vnd.oci.image.index.v1+json",
    "manifests": [
        {
            "mediaType": "application/vnd.oci.image.manifest.v1+json",
            "digest": "sha256:e692418e4cbaf90ca69d05a66403747baa33ee08806650b51fab815ad7fc331f",
            "size": 7143,
            "platform": {
                "architecture": "amd64",
                "os": "linux"
            }
        },
        {
            "mediaType": "application/vnd.oci.image.manifest.v1+json",
            "digest": "sha256:5b0bcabd1ed22e9fb1310cf6c2dec7cdef19f0ad69efa1f392e94a4333501270",
            "size": 7682,
            "platform": {
                "architecture": "arm64",
                "os": "linux",
                "variant": "v8"
            }
        },
        {
            "mediaType": "application/vnd.oci.image.manifest.v1+json",
            "digest": "sha256:aabbccdd...",
            "size": 7320,
            "platform": {
                "architecture": "amd64",
                "os": "windows",
                "os.version": "10.0.17763.0"
            }
        }
    ]
}
```

클라이언트가 `docker pull nginx:latest`를 실행하면, 레지스트리는 먼저 이미지 인덱스를 반환한다. 클라이언트는 자신의 플랫폼(예: `linux/amd64`)에 맞는 매니페스트를 선택하여 해당 레이어들을 다운로드한다.

### 5.4 레이어 (Layer) 구조

레이어는 컨테이너 이미지의 파일시스템 변경분을 나타내는 tar 아카이브이다.

```
Base Layer (layer 0):    ubuntu:22.04 기본 파일시스템
    │
    ├── /bin, /lib, /usr, /etc, ...
    │
Layer 1:                 apt-get install python3
    │
    ├── /usr/bin/python3 (추가)
    ├── /usr/lib/python3/ (추가)
    │
Layer 2:                 COPY app.py /app/
    │
    ├── /app/app.py (추가)
    │
Layer 3:                 RUN pip install flask
    │
    ├── /usr/lib/python3/dist-packages/flask/ (추가)
```

#### 레이어 미디어 타입

| 미디어 타입 | 설명 |
|------------|------|
| `application/vnd.oci.image.layer.v1.tar` | 비압축 tar |
| `application/vnd.oci.image.layer.v1.tar+gzip` | gzip 압축 (가장 일반적) |
| `application/vnd.oci.image.layer.v1.tar+zstd` | zstd 압축 (더 빠른 압축/해제) |
| `application/vnd.oci.image.layer.nondistributable.v1.tar+gzip` | 재배포 불가 레이어 (라이선스 제한) |

#### Union Filesystem과 Whiteout

레이어는 유니온 파일시스템(Union Filesystem, 예: OverlayFS) 을 통해 겹쳐진다. 상위 레이어에서 하위 레이어의 파일을 "삭제"하려면 whiteout 파일 을 사용한다.

```
# 파일 삭제: .wh.<filename> 이라는 빈 파일을 생성
.wh.old-config.txt     # old-config.txt 파일이 삭제된 것으로 처리

# 디렉터리 전체 삭제: .wh..wh..opq 파일을 해당 디렉터리에 생성
tmp/.wh..wh..opq       # /tmp 디렉터리의 모든 하위 내용이 삭제(불투명 처리)
```

### 5.5 이미지 설정 (Image Configuration)

이미지 설정은 컨테이너를 실행할 때 적용할 기본 파라미터와 이미지 메타데이터를 담는다.

```json
{
    "created": "2024-01-15T10:30:00Z",
    "author": "developer@example.com",
    "architecture": "amd64",
    "os": "linux",
    "config": {
        "User": "appuser",
        "ExposedPorts": {
            "8080/tcp": {}
        },
        "Env": [
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "APP_ENV=production"
        ],
        "Entrypoint": ["/usr/bin/python3"],
        "Cmd": ["app.py"],
        "WorkingDir": "/app",
        "Labels": {
            "maintainer": "team@example.com",
            "version": "1.0.0"
        },
        "StopSignal": "SIGTERM"
    },
    "rootfs": {
        "type": "layers",
        "diff_ids": [
            "sha256:abc123...",
            "sha256:def456...",
            "sha256:ghi789..."
        ]
    },
    "history": [
        {
            "created": "2024-01-15T10:00:00Z",
            "created_by": "/bin/sh -c #(nop) ADD file:abc... in /"
        },
        {
            "created": "2024-01-15T10:10:00Z",
            "created_by": "/bin/sh -c apt-get update && apt-get install -y python3"
        },
        {
            "created": "2024-01-15T10:20:00Z",
            "created_by": "/bin/sh -c #(nop) COPY file:def... in /app/app.py",
            "empty_layer": false
        }
    ]
}
```

| 필드 | 설명 |
|------|------|
| `architecture` | 이미지 대상 CPU 아키텍처 (amd64, arm64 등) |
| `os` | 이미지 대상 운영체제 |
| `config` | 런타임 기본 설정 (Dockerfile의 `CMD`, `ENTRYPOINT`, `ENV`, `EXPOSE` 등에 해당) |
| `rootfs.diff_ids` | 각 레이어의 비압축 상태 해시 (레이어 순서 및 무결성 검증용) |
| `history` | 이미지 빌드 히스토리 (각 레이어가 어떤 명령으로 만들어졌는지) |

---

## 6. Distribution Specification 상세

Distribution Spec은 OCI 이미지를 레지스트리에 저장하고 가져오는 HTTP API를 정의한다.

### 6.1 레지스트리 API 엔드포인트

OCI Distribution Spec이 정의하는 핵심 API 엔드포인트는 다음과 같다.

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| `GET` | `/v2/` | API 버전 확인 및 인증 체크 |
| `HEAD` | `/v2/<name>/manifests/<reference>` | 매니페스트 존재 여부 확인 |
| `GET` | `/v2/<name>/manifests/<reference>` | 매니페스트 가져오기 |
| `PUT` | `/v2/<name>/manifests/<reference>` | 매니페스트 업로드 |
| `DELETE` | `/v2/<name>/manifests/<reference>` | 매니페스트 삭제 |
| `HEAD` | `/v2/<name>/blobs/<digest>` | Blob 존재 여부 확인 |
| `GET` | `/v2/<name>/blobs/<digest>` | Blob 다운로드 |
| `DELETE` | `/v2/<name>/blobs/<digest>` | Blob 삭제 |
| `POST` | `/v2/<name>/blobs/uploads/` | Blob 업로드 시작 |
| `PATCH` | `/v2/<name>/blobs/uploads/<session-id>` | Blob 청크 업로드 |
| `PUT` | `/v2/<name>/blobs/uploads/<session-id>` | Blob 업로드 완료 |
| `GET` | `/v2/<name>/tags/list` | 태그 목록 조회 |
| `GET` | `/v2/<name>/referrers/<digest>` | 참조자 목록 조회 (v1.1+) |

여기서 `<name>`은 리포지토리 이름(예: `library/nginx`), `<reference>`는 태그(예: `latest`) 또는 다이제스트(예: `sha256:abc123...`)이다.

### 6.2 Pull 프로토콜 (이미지 가져오기)

이미지를 가져오는 과정은 다음과 같다.

```
클라이언트                                    레지스트리
    │                                            │
    │  1. GET /v2/                               │
    │ ─────────────────────────────────────────> │
    │  200 OK (API 지원 확인)                     │
    │ <───────────────────────────────────────── │
    │                                            │
    │  2. GET /v2/library/nginx/manifests/latest │
    │     Accept: application/vnd.oci.image.index.v1+json,
    │             application/vnd.oci.image.manifest.v1+json
    │ ─────────────────────────────────────────> │
    │  200 OK + Image Index (JSON)               │
    │ <───────────────────────────────────────── │
    │                                            │
    │  3. (플랫폼에 맞는 매니페스트 선택)          │
    │  GET /v2/library/nginx/manifests/sha256:e69...
    │ ─────────────────────────────────────────> │
    │  200 OK + Image Manifest (JSON)            │
    │ <───────────────────────────────────────── │
    │                                            │
    │  4. GET /v2/library/nginx/blobs/sha256:b5b... │
    │     (이미지 설정 다운로드)                    │
    │ ─────────────────────────────────────────> │
    │  200 OK + Config JSON                      │
    │ <───────────────────────────────────────── │
    │                                            │
    │  5. HEAD /v2/library/nginx/blobs/sha256:983...│
    │     (레이어가 로컬에 있는지 확인)             │
    │ ─────────────────────────────────────────> │
    │                                            │
    │  6. GET /v2/library/nginx/blobs/sha256:983... │
    │     (없는 레이어만 다운로드)                  │
    │ ─────────────────────────────────────────> │
    │  200 OK + Layer tar.gz                     │
    │ <───────────────────────────────────────── │
```

### 6.3 Push 프로토콜 (이미지 업로드)

이미지를 업로드하는 과정은 다음과 같다.

```
클라이언트                                    레지스트리
    │                                            │
    │  1. HEAD /v2/myapp/blobs/sha256:983...     │
    │     (레이어가 이미 존재하는지 확인)           │
    │ ─────────────────────────────────────────> │
    │  404 Not Found (존재하지 않음)               │
    │ <───────────────────────────────────────── │
    │                                            │
    │  2. POST /v2/myapp/blobs/uploads/          │
    │     (Blob 업로드 세션 시작)                  │
    │ ─────────────────────────────────────────> │
    │  202 Accepted                              │
    │  Location: /v2/myapp/blobs/uploads/<uuid>  │
    │ <───────────────────────────────────────── │
    │                                            │
    │  3. PATCH /v2/myapp/blobs/uploads/<uuid>   │
    │     Content-Type: application/octet-stream │
    │     (레이어 데이터 청크 전송)                │
    │ ─────────────────────────────────────────> │
    │  202 Accepted                              │
    │ <───────────────────────────────────────── │
    │                                            │
    │  4. PUT /v2/myapp/blobs/uploads/<uuid>     │
    │     ?digest=sha256:983...                  │
    │     (업로드 완료 및 다이제스트 검증)          │
    │ ─────────────────────────────────────────> │
    │  201 Created                               │
    │ <───────────────────────────────────────── │
    │                                            │
    │  5. (모든 레이어 업로드 후)                  │
    │  PUT /v2/myapp/manifests/v1.0.0            │
    │  Content-Type: application/vnd.oci.image.manifest.v1+json
    │  (매니페스트 업로드)                         │
    │ ─────────────────────────────────────────> │
    │  201 Created                               │
    │ <───────────────────────────────────────── │
```

#### 업로드 방식

| 방식 | 설명 |
|------|------|
| Monolithic Upload | `POST` 한 번으로 전체 Blob을 업로드. 작은 Blob에 적합하다. |
| Chunked Upload | `POST`로 세션을 시작하고, `PATCH`로 청크를 나누어 전송한 뒤, `PUT`으로 완료. 대용량 레이어에 적합하다. |
| Cross-Repository Mount | 같은 레지스트리 내 다른 리포지토리에 이미 존재하는 Blob을 복사 없이 마운트. `POST /v2/<name>/blobs/uploads/?mount=<digest>&from=<other-repo>` |

### 6.4 인증 (Authentication)

OCI Distribution Spec은 Docker Registry의 인증 체계를 따른다.

```
1. 클라이언트가 레지스트리에 요청
2. 레지스트리가 401 Unauthorized + WWW-Authenticate 헤더 반환
   WWW-Authenticate: Bearer realm="https://auth.example.com/token",
                     service="registry.example.com",
                     scope="repository:library/nginx:pull"
3. 클라이언트가 토큰 서버에 인증 요청
4. 토큰 서버가 Bearer 토큰 발급
5. 클라이언트가 Authorization: Bearer <token> 헤더로 재요청
```

### 6.5 Referrers API (v1.1+)

Distribution Spec v1.1에서 추가된 Referrers API는 특정 매니페스트를 참조하는 아티팩트들(서명, SBOM, 취약점 스캔 결과 등)을 조회할 수 있게 한다.

```
GET /v2/<name>/referrers/<digest>?artifactType=<type>

# 응답: 해당 매니페스트를 참조하는 아티팩트 목록 (Image Index 형태)
{
    "schemaVersion": 2,
    "mediaType": "application/vnd.oci.image.index.v1+json",
    "manifests": [
        {
            "mediaType": "application/vnd.oci.image.manifest.v1+json",
            "digest": "sha256:signature-digest...",
            "size": 1234,
            "artifactType": "application/vnd.cncf.notary.signature",
            "annotations": {
                "org.opencontainers.image.created": "2024-01-15T10:30:00Z"
            }
        }
    ]
}
```

---

## 7. 주요 구현체

### 7.1 런타임 구현체

#### runc

- 역할: OCI Runtime Spec의 참조 구현(Reference Implementation)
- 개발: Docker가 libcontainer를 기부하여 시작, 현재 OCI 산하 프로젝트
- 특징: Go로 작성된 저수준(low-level) 컨테이너 런타임. Linux namespace, cgroups, seccomp 등을 직접 다루어 컨테이너를 생성하고 실행한다.
- 사용처: containerd, CRI-O 등 고수준 런타임의 기본 하위 런타임으로 사용된다.

```bash
# runc를 직접 사용하는 예시
runc spec                          # 기본 config.json 생성
runc create my-container           # 컨테이너 생성
runc start my-container            # 컨테이너 시작
runc state my-container            # 컨테이너 상태 조회
runc kill my-container SIGTERM     # 컨테이너에 시그널 전송
runc delete my-container           # 컨테이너 삭제
```

#### crun

- 특징: C로 작성된 경량 OCI 런타임. runc보다 빠르고 메모리 사용량이 적다.
- 사용처: Podman, CRI-O에서 runc 대안으로 사용 가능

#### youki

- 특징: Rust로 작성된 OCI 런타임. 메모리 안전성을 강조한다.

#### gVisor (runsc)

- 특징: Google이 개발한 샌드박스 런타임. 사용자 공간에서 Linux 커널 시스템 콜을 가로채어 추가적인 격리 계층을 제공한다.
- 사용처: 멀티테넌트 환경에서 보안이 중요한 경우

#### Kata Containers

- 특징: 경량 가상 머신(Lightweight VM) 기반 런타임. 각 컨테이너가 전용 커널을 갖는다.
- 사용처: 강력한 격리가 필요한 워크로드

### 7.2 고수준 런타임 및 도구

#### containerd

- 역할: 산업 표준 컨테이너 런타임. CNCF 졸업(Graduated) 프로젝트.
- 특징: 이미지 관리, 컨테이너 생명주기, 스토리지, 네트워킹 등 전반적인 컨테이너 관리를 담당한다. 하위 런타임으로 runc(또는 다른 OCI 런타임)를 호출한다.
- 사용처: Docker Engine의 핵심 런타임, Kubernetes의 CRI(Container Runtime Interface) 구현체

```
Docker CLI / Kubernetes kubelet
        │
        v
    containerd (고수준 런타임)
        │
        v
    containerd-shim
        │
        v
    runc (저수준 OCI 런타임)
        │
        v
    컨테이너 프로세스
```

#### CRI-O

- 역할: Kubernetes 전용으로 설계된 경량 컨테이너 런타임
- 특징: Kubernetes CRI를 직접 구현하며, OCI 이미지와 런타임을 사용한다. Docker나 containerd 없이도 Kubernetes에서 컨테이너를 실행할 수 있다.
- 설계 철학: "Kubernetes에 필요한 것만 구현한다"

#### Podman

- 역할: Docker CLI 호환의 컨테이너 관리 도구
- 특징:
  - 데몬이 없는(daemonless) 아키텍처. 각 명령이 독립적인 프로세스로 실행된다.
  - Rootless 컨테이너를 기본 지원한다.
  - OCI 이미지와 OCI 런타임을 직접 사용한다.
  - Pod 개념을 기본 지원한다 (Kubernetes Pod와 유사).

```bash
# Docker와 거의 동일한 CLI
podman pull docker.io/library/nginx:latest
podman run -d -p 8080:80 nginx
podman build -t myapp .
podman push myapp registry.example.com/myapp:v1
```

#### Buildah

- 역할: OCI 컨테이너 이미지 빌드 전용 도구
- 특징:
  - Dockerfile 없이도 스크립트로 이미지를 빌드할 수 있다.
  - 데몬이 필요 없다.
  - OCI 이미지와 Docker 이미지 포맷 모두 생성 가능하다.
  - CI/CD 파이프라인에 적합하다.

```bash
# Buildah로 이미지 빌드하기
container=$(buildah from ubuntu:22.04)
buildah run $container apt-get update
buildah run $container apt-get install -y python3
buildah copy $container app.py /app/app.py
buildah config --cmd "python3 /app/app.py" $container
buildah commit $container myapp:latest
```

#### Skopeo

- 역할: 컨테이너 이미지 복사/검사/서명 도구
- 특징: 레지스트리 간 이미지를 직접 복사할 수 있으며, 로컬에 이미지를 풀(pull)하지 않고도 레지스트리의 이미지를 검사할 수 있다.

```bash
# 레지스트리 간 이미지 직접 복사
skopeo copy docker://docker.io/nginx:latest docker://registry.example.com/nginx:latest

# 이미지 메타데이터 검사 (pull 없이)
skopeo inspect docker://docker.io/library/nginx:latest
```

---

## 8. OCI와 Docker, Kubernetes의 관계

### 8.1 OCI와 Docker

```
[ Docker 생태계의 진화 ]

Before OCI (~ 2015):
┌─────────────────────────────────────────┐
│            Docker (모놀리식)              │
│  CLI + 데몬 + 런타임 + 이미지 + 빌드     │
│  모든 것이 하나의 바이너리에              │
└─────────────────────────────────────────┘

After OCI (2015 ~):
┌──────────┐  ┌──────────────┐  ┌──────────────┐  ┌───────┐
│ Docker   │  │  containerd  │  │ containerd-  │  │ runc  │
│ CLI      │->│  (고수준     │->│ shim         │->│ (OCI  │
│ (사용자  │  │   런타임)    │  │              │  │  런타임│
│  인터페이스)│  │              │  │              │  │       │
└──────────┘  └──────────────┘  └──────────────┘  └───────┘
                    │
                    v
              OCI Image Spec
              (이미지 포맷)
```

- Docker는 OCI 설립의 주도적 역할을 했으며, libcontainer 코드를 기부하여 runc의 기반을 마련했다.
- Docker는 자사의 모놀리식 아키텍처를 분해하여, containerd(고수준 런타임)와 runc(저수준 런타임)를 분리했다.
- containerd는 CNCF에 기부되어 독립 프로젝트가 되었다.
- Docker 이미지 포맷(Docker Image Manifest V2, Schema 2)은 OCI Image Spec의 직접적인 기반이 되었다. 두 포맷은 거의 동일하며 상호 변환이 가능하다.

### 8.2 OCI와 Kubernetes

```
[ Kubernetes 컨테이너 런타임 진화 ]

Phase 1: Docker 직접 사용
┌──────────┐     ┌──────────────────────────────┐
│ kubelet  │ --> │ dockershim --> Docker Engine  │
│          │     │   --> containerd --> runc     │
└──────────┘     └──────────────────────────────┘
  (비효율적: 불필요한 레이어가 많음)

Phase 2: CRI 도입 (현재)
┌──────────┐     ┌──────────────────────┐
│ kubelet  │ --> │ CRI (표준 인터페이스) │
│          │     └──────────┬────────────┘
└──────────┘                │
                 ┌──────────┼──────────┐
                 v          v          v
            containerd    CRI-O     기타 CRI
                 │          │       구현체
                 v          v
               runc       runc
             (OCI RT)   (OCI RT)
```

- Kubernetes 1.5 (2016): CRI(Container Runtime Interface) 도입. Docker 외에 다른 런타임도 사용 가능해짐.
- Kubernetes 1.20 (2020): dockershim 지원 중단 예고(deprecation).
- Kubernetes 1.24 (2022): dockershim 완전 제거. containerd 또는 CRI-O를 직접 사용.
- Kubernetes는 OCI 이미지를 표준으로 사용한다. `kubectl`로 배포하는 모든 컨테이너 이미지는 OCI 또는 Docker 이미지 포맷을 따른다.
- CRI를 통해 OCI 호환 런타임이면 어떤 것이든 Kubernetes에서 사용할 수 있다.

### 8.3 관계 요약

| 기술 | OCI와의 관계 |
|------|-------------|
| Docker | OCI의 설립자이자 핵심 기여자. Docker 이미지와 런타임이 OCI 명세의 기반. |
| containerd | OCI 이미지 관리 및 OCI 런타임(runc) 호출을 담당하는 고수준 런타임 |
| Kubernetes | OCI 이미지를 표준 포맷으로 사용. CRI를 통해 OCI 런타임과 연동 |
| 레지스트리 | Docker Hub, GHCR, ECR, GCR, ACR 등 주요 레지스트리가 OCI Distribution Spec 지원 |

---

## 9. 장점과 단점

### 장점

| 장점 | 설명 |
|------|------|
| 벤더 중립성 | 특정 벤더에 종속되지 않는 개방형 표준이다. Docker, Podman, containerd 등 어떤 도구를 사용하든 동일한 이미지를 실행할 수 있다. |
| 이식성 | OCI 호환 이미지는 어디서든 실행 가능하다. 로컬 개발 환경, CI/CD, 클라우드 등 환경을 가리지 않는다. |
| 상호운용성 | 서로 다른 빌드 도구(Docker, Buildah, kaniko), 런타임(runc, crun, kata), 레지스트리(Docker Hub, GHCR, ECR) 간 호환이 보장된다. |
| 생태계 다양성 | 표준이 있으므로, 각 레이어에서 다양한 구현체가 경쟁하고 혁신할 수 있다. 보안(gVisor, Kata), 성능(crun), 편의성(Podman) 등 다양한 가치를 추구하는 도구가 공존한다. |
| 확장성 | Image Spec v1.1의 아티팩트 지원, 커스텀 미디어 타입 등으로 컨테이너 이미지 외의 아티팩트(Helm 차트, Wasm 모듈, SBOM 등)도 OCI 레지스트리에 저장 가능하다. |
| 보안 | 콘텐츠 주소 지정(Content Addressable)으로 이미지 무결성이 보장된다. Referrers API로 서명, SBOM 등의 보안 메타데이터를 체계적으로 관리할 수 있다. |

### 단점

| 단점 | 설명 |
|------|------|
| 표준화의 느린 속도 | 합의 기반 프로세스이므로, 새로운 기능의 표준화가 느리다. 예를 들어 Distribution Spec은 수년간 작업되었다. |
| 복잡성 | 세 가지 명세가 별도로 존재하며, 각각의 버전이 다르게 진행된다. 전체 스택을 이해하려면 상당한 학습이 필요하다. |
| 최소 공통분모 문제 | 다양한 벤더의 합의가 필요하므로, 혁신적이거나 실험적인 기능이 명세에 포함되기 어렵다. |
| 사실상 표준과의 간극 | Docker 이미지 포맷이 여전히 가장 널리 사용되며, OCI 이미지와 Docker 이미지가 사실상 거의 동일함에도 두 가지가 공존한다. |
| 플랫폼 의존성 | Runtime Spec은 Linux에 가장 최적화되어 있으며, Windows나 다른 OS 지원은 상대적으로 제한적이다. |
| 구현의 일관성 부재 | 명세를 해석하는 방식이 구현체마다 미묘하게 다를 수 있어, "OCI 호환"이라 하더라도 완벽한 호환을 기대하기 어려운 경우가 있다. |

---

## 10. 실제 활용 사례

### 10.1 멀티 플랫폼 이미지 배포

```bash
# Docker Buildx로 멀티 플랫폼 OCI 이미지 빌드
docker buildx create --name multiplatform --use
docker buildx build \
  --platform linux/amd64,linux/arm64,linux/arm/v7 \
  --tag registry.example.com/myapp:v1.0.0 \
  --push .
```

OCI Image Index를 활용하면, 하나의 이미지 태그로 x86 서버, ARM 기반 AWS Graviton, Raspberry Pi 등 다양한 플랫폼을 모두 지원할 수 있다.

### 10.2 OCI 레지스트리를 범용 아티팩트 저장소로 활용

OCI Image Spec v1.1부터 컨테이너 이미지가 아닌 범용 아티팩트도 저장할 수 있다.

```bash
# Helm 차트를 OCI 레지스트리에 저장
helm push mychart-0.1.0.tgz oci://registry.example.com/charts

# Helm 차트를 OCI 레지스트리에서 가져오기
helm pull oci://registry.example.com/charts/mychart --version 0.1.0

# ORAS(OCI Registry As Storage)로 임의의 파일을 OCI 아티팩트로 저장
oras push registry.example.com/myfiles:latest ./report.pdf:application/pdf

# Wasm 모듈 저장
oras push registry.example.com/wasm/mymodule:v1 ./app.wasm:application/wasm
```

### 10.3 소프트웨어 공급망 보안 (Supply Chain Security)

```bash
# Cosign으로 OCI 이미지에 서명
cosign sign --key cosign.key registry.example.com/myapp:v1.0.0

# 서명 검증
cosign verify --key cosign.pub registry.example.com/myapp:v1.0.0

# SBOM(Software Bill of Materials)을 이미지에 첨부
syft registry.example.com/myapp:v1.0.0 -o spdx-json > sbom.json
oras attach registry.example.com/myapp:v1.0.0 \
  --artifact-type application/spdx+json sbom.json

# Referrers API로 이미지에 연결된 아티팩트 조회
oras discover registry.example.com/myapp:v1.0.0
```

OCI Referrers API 덕분에 이미지와 서명, SBOM, 취약점 스캔 결과 등을 체계적으로 연결하고 관리할 수 있다.

### 10.4 Kubernetes 환경에서의 런타임 선택

```yaml
# Kubernetes RuntimeClass를 사용한 런타임 선택
apiVersion: node.k8s.io/v1
kind: RuntimeClass
metadata:
  name: gvisor
handler: runsc
---
apiVersion: node.k8s.io/v1
kind: RuntimeClass
metadata:
  name: kata
handler: kata
---
# Pod에서 특정 런타임 사용
apiVersion: v1
kind: Pod
metadata:
  name: secure-workload
spec:
  runtimeClassName: gvisor    # gVisor 런타임으로 실행
  containers:
  - name: app
    image: registry.example.com/myapp:v1.0.0
```

OCI Runtime Spec 덕분에 워크로드의 특성에 따라 런타임을 선택할 수 있다. 일반 워크로드는 runc, 보안이 중요한 워크로드는 gVisor나 Kata Containers를 사용하는 식이다.

### 10.5 CI/CD 파이프라인에서의 데몬리스 빌드

```yaml
# GitHub Actions에서 Buildah를 사용한 OCI 이미지 빌드 (데몬 불필요)
name: Build and Push
on: push
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Build image with Buildah
        run: |
          buildah bud -t myapp:${{ github.sha }} .

      - name: Push to GHCR
        run: |
          buildah login -u ${{ github.actor }} -p ${{ secrets.GITHUB_TOKEN }} ghcr.io
          buildah push myapp:${{ github.sha }} ghcr.io/${{ github.repository }}/myapp:${{ github.sha }}
```

Docker 데몬 없이 Buildah로 OCI 이미지를 빌드하고, Skopeo나 Buildah로 직접 레지스트리에 푸시할 수 있다. CI/CD 환경에서 Docker-in-Docker(DinD)의 복잡성과 보안 문제를 피할 수 있다.

### 10.6 에어갭(Air-Gapped) 환경에서의 이미지 전송

```bash
# 인터넷 환경에서 이미지를 OCI 레이아웃으로 저장
skopeo copy docker://docker.io/nginx:latest oci:nginx-oci:latest

# 또는 tar로 아카이브
skopeo copy docker://docker.io/nginx:latest oci-archive:nginx.tar:latest

# (물리적 매체로 전송)

# 에어갭 환경에서 로컬 레지스트리에 로드
skopeo copy oci-archive:nginx.tar:latest docker://internal-registry.local/nginx:latest
```

OCI 이미지 레이아웃은 파일시스템에 직접 저장할 수 있는 표준 구조를 정의하므로, 인터넷이 차단된 보안 환경에서도 이미지를 안전하게 전송할 수 있다.

---

## 참고 자료

- OCI 공식 사이트: https://opencontainers.org/
- Runtime Spec: https://github.com/opencontainers/runtime-spec
- Image Spec: https://github.com/opencontainers/image-spec
- Distribution Spec: https://github.com/opencontainers/distribution-spec
- runc: https://github.com/opencontainers/runc
- containerd: https://github.com/containerd/containerd
- CRI-O: https://github.com/cri-o/cri-o
- Podman: https://github.com/containers/podman
- Buildah: https://github.com/containers/buildah
- Skopeo: https://github.com/containers/skopeo
- ORAS (OCI Registry As Storage): https://oras.land/
