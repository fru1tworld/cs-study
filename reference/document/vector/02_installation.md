# Vector 설치 가이드

이 문서는 Vector 공식 문서의 설치(Installation) 섹션을 한국어로 번역한 것입니다.

원본 문서: https://vector.dev/docs/setup/installation/

---

## 목차

1. [개요](#개요)
2. [빠른 설치](#빠른-설치)
3. [패키지 관리자를 통한 설치](#패키지-관리자를-통한-설치)
   - [APT](#apt)
   - [dpkg](#dpkg)
   - [Helm](#helm)
   - [Homebrew](#homebrew)
   - [MSI](#msi)
   - [Nix](#nix)
   - [pacman](#pacman)
   - [RPM](#rpm)
   - [YUM](#yum)
4. [플랫폼별 설치](#플랫폼별-설치)
   - [Docker](#docker)
   - [Kubernetes](#kubernetes)
5. [운영체제별 설치](#운영체제별-설치)
   - [Amazon Linux](#amazon-linux)
   - [Arch Linux](#arch-linux)
   - [CentOS](#centos)
   - [Debian](#debian)
   - [Fedora](#fedora)
   - [macOS](#macos)
   - [NixOS](#nixos)
   - [Raspbian](#raspbian)
   - [RHEL](#rhel)
   - [Ubuntu](#ubuntu)
   - [Windows](#windows)
6. [수동 설치](#수동-설치)
   - [Vector 설치 스크립트](#vector-설치-스크립트)
   - [아카이브에서 설치](#아카이브에서-설치)
   - [소스에서 빌드](#소스에서-빌드)
7. [설치 후 설정](#설치-후-설정)
8. [참고 사항](#참고-사항)

---

## 개요

Vector는 관측성(observability) 파이프라인을 구축하기 위한 경량의 초고속 도구입니다. 다양한 패키지 관리자와 여러 운영체제 및 플랫폼에서 Vector를 설치할 수 있습니다. 사용자 정의 빌드가 필요한 경우 수동으로 설치할 수도 있습니다.

### 지원되는 설치 방법

- 패키지 관리자: APT, dpkg, Helm, Homebrew, MSI, Nix, pacman, RPM, YUM
- 플랫폼: Docker, Kubernetes
- 운영체제: Amazon Linux, Arch Linux, CentOS, Debian, Fedora, macOS, NixOS, Raspbian, RHEL, Ubuntu, Windows

### 시스템 요구 사항

Vector는 단일 정적 바이너리로 컴파일되어 설치가 매우 간단합니다. *nix 시스템에서 Vector의 유일한 의존성은 libc이며, 이는 일반적으로 운영체제에서 제공됩니다.

Vector는 또한 musl을 사용하여 libc 구현을 정적으로 링크한 아티팩트를 릴리스합니다. 이로 인해 의존성이 없는 정적 바이너리가 생성됩니다. 이러한 의존성 없는 아티팩트는 내장 libc 구현을 제공하지 않는 최소화된 환경에서 유용할 수 있습니다.

> 참고: musl은 Vector가 여러 스레드에서 실행될 때 glibc보다 성능이 상당히 떨어집니다. 단일 CPU에서 Vector를 실행하지 않는 한, 가능하면 glibc를 사용하는 것이 좋습니다.

---

## 빠른 설치

Vector를 설치하는 가장 간단한 방법은 공식 설치 스크립트를 사용하는 것입니다:

```bash
curl --proto '=https' --tlsv1.2 -sSfL https://sh.vector.dev | bash
```

특정 버전을 설치하려면 `VECTOR_VERSION` 환경 변수를 사용하세요:

```bash
curl --proto '=https' --tlsv1.2 -sSfL https://sh.vector.dev | VECTOR_VERSION=0.52.0 bash
```

---

## 패키지 관리자를 통한 설치

### APT

APT(Advanced Package Tool)는 Debian, Ubuntu 및 기타 Linux 배포판에서 소프트웨어 설치 및 제거를 처리하는 무료 패키지 관리자입니다.

> 참고: APT 저장소는 Datadog에서 제공합니다.

#### 설치

1단계: Vector 저장소 추가

```bash
bash -c "$(curl -L https://setup.vector.dev)"
```

2단계: Vector 설치

```bash
sudo apt-get install vector
```

#### 기존 저장소에서 마이그레이션

기존 저장소(repositories.timber.io)에서 마이그레이션하는 경우:

```bash
CSM_MIGRATE=true bash -c "$(curl -L https://setup.vector.dev)"
```

또는 기존 저장소 제거를 직접 처리하려면 `CSM_MIGRATE`를 설정하지 않아도 됩니다.

#### 수동 저장소 설정

수동으로 설정하려면 기존 저장소를 제거하고 HTTPS 다운로드를 위한 APT를 설정하세요:

```bash
rm "/etc/apt/sources.list.d/timber-vector.list"
sudo apt-get update
sudo apt-get install apt-transport-https curl gnupg
```

#### 관리 명령어

Vector 업그레이드:
```bash
sudo apt-get upgrade vector
```

Vector 제거:
```bash
sudo apt remove vector
```

Vector 시작:
```bash
sudo systemctl start vector
```

Vector 상태 확인:
```bash
sudo systemctl status vector
```

---

### dpkg

dpkg는 Debian 운영체제 및 그 파생 제품에서 패키지 관리 시스템을 구동하는 소프트웨어입니다. dpkg는 .deb 패키지를 통해 소프트웨어를 설치하고 관리하는 데 사용됩니다.

#### 설치

1단계: .deb 패키지 다운로드

`{arch}`를 시스템 아키텍처(amd64, arm64, armhf)로 교체하세요:

```bash
curl \
  --proto '=https' \
  --tlsv1.2 -O \
  https://apt.vector.dev/pool/v/ve/vector_0.52.0-1_{arch}.deb
```

2단계: dpkg로 설치

```bash
sudo dpkg -i vector_0.52.0-1_{arch}.deb
```

#### 의존성 처리

설치 중 의존성 누락 오류가 발생하면 다음 명령으로 해결할 수 있습니다:

```bash
sudo apt install -f
```

#### 제거

```bash
dpkg -r vector
```

> 참고: dpkg 명령은 sudo/슈퍼유저 권한이 필요합니다. sudo 없이 실행하면 "dpkg: error: requested operation requires superuser privilege" 오류가 발생합니다.

---

### Helm

Helm은 Kubernetes 클러스터에서 애플리케이션 및 서비스의 배포와 관리를 용이하게 하는 Kubernetes용 패키지 관리자입니다.

#### 설치

1단계: Vector 저장소 추가

```bash
helm repo add vector https://helm.vector.dev
helm repo update
```

2단계: Vector 설치

릴리스 이름 `<RELEASE_NAME>`으로 차트를 설치합니다:

```bash
helm install <RELEASE_NAME> vector/vector
```

Vector Aggregator를 네임스페이스 생성과 함께 설치:

```bash
helm install vector vector/vector \
  --namespace vector \
  --create-namespace
```

#### 배포 역할

기본적으로 Vector는 "Aggregator" 역할의 StatefulSet으로 실행됩니다. 대안으로:
- "Stateless-Aggregator" 역할의 Deployment로 실행
- "Agent" 역할의 DaemonSet으로 실행

#### 사용자 정의 설정으로 업그레이드

```bash
helm upgrade -f values.yaml <RELEASE_NAME> vector/vector
```

> 권장사항: values.yaml에는 재정의해야 하는 값만 포함하세요. 이렇게 하면 차트 버전 업그레이드 시 더 원활한 경험을 할 수 있습니다.

#### 제거

```bash
helm delete <RELEASE_NAME>
```

이 명령은 차트와 연결된 모든 Kubernetes 구성 요소를 제거하고 릴리스를 삭제합니다.

#### 요구 사항

차트는 Kubernetes 버전 >=1.15.0-0이 필요합니다.

> 참고: 별도의 `vector/vector-agent` 및 `vector/vector-aggregator` 차트는 이제 폐기(DEPRECATED) 되었습니다. 대신 통합된 `vector/vector` 차트를 사용하세요.

---

### Homebrew

Homebrew는 Apple의 macOS 운영체제 및 일부 지원되는 Linux 시스템을 위한 무료 오픈 소스 패키지 관리 시스템입니다.

> 참고: Homebrew를 통한 Vector 설치는 macOS에서만 사용 가능합니다(Linux는 지원되지 않음).

#### 설치

1단계: Vector tap 추가

```bash
brew tap vectordotdev/brew
```

2단계: Vector 설치

```bash
brew install vector
```

#### 설정 파일 위치

brew 설치 시 설정 파일은 다음 위치에 저장됩니다:
- Intel Mac: `/usr/local/etc/vector/vector.yaml`
- Apple Silicon Mac: `/opt/homebrew/etc/vector/vector.yaml`

---

### MSI

MSI는 Windows Installer의 파일 형식 및 명령줄 유틸리티입니다. Windows Installer(이전의 Microsoft Installer)는 Windows 시스템에서 소프트웨어를 설치하고 관리하는 데 사용되는 Microsoft Windows용 인터페이스입니다.

#### 설치

PowerShell에서 다음 명령을 실행하세요:

```powershell
Invoke-WebRequest https://packages.timber.io/vector/0.52.0/vector-x64.msi -OutFile vector-0.52.0-x64.msi
msiexec /i vector-0.52.0-x64.msi
```

#### 주요 이점

Rust의 Windows에 대한 tier 1 지원 외에도, Vector는 어떤 의존성 설치도 필요하지 않습니다. 이로 인해 설치가 Vector 바이너리를 머신에 복사하는 것만큼 간단합니다. 추가 DLL 파일을 설치하거나 환경 변경이 필요하지 않습니다.

---

### Nix

Nix는 선언적 빌드 및 배포를 지원하는 패키지 관리자입니다.

> 참고: Vector의 Nix 릴리스는 수동으로 업데이트해야 하므로, 공식 Vector 릴리스와 Nix 패키지 릴리스 사이에 지연이 있을 수 있습니다. 새로운 Vector 패키지는 일반적으로 며칠 내에 Nix에서 사용 가능합니다.

#### 설치

```bash
nix-env --install \
  --file https://github.com/NixOS/nixpkgs/archive/master.tar.gz \
  --attr vector
```

#### 업그레이드

```bash
nix-env --upgrade vector \
  --file https://github.com/NixOS/nixpkgs/archive/master.tar.gz
```

#### 제거

```bash
nix-env --uninstall vector
```

---

### pacman

pacman은 주로 Arch Linux 및 그 파생 제품에서 Linux의 소프트웨어 패키지를 관리하는 유틸리티입니다.

#### 설치

Vector는 Arch Linux extra 패키지 저장소를 통해 설치할 수 있습니다:

```bash
sudo pacman -Syu vector
```

---

### RPM

RPM Package Manager는 Fedora, CentOS, OpenSUSE, OpenMandriva, Red Hat Enterprise Linux 및 관련 Linux 기반 시스템에서 소프트웨어를 설치하고 관리하기 위한 무료 오픈 소스 패키지 관리 시스템입니다.

#### 설치

`{arch}`를 다음 중 하나로 교체하세요: `x86_64`, `aarch64`, 또는 `armv7hl`:

```bash
sudo rpm -i https://yum.vector.dev/stable/vector-0/{arch}/vector-0.52.0-1.{arch}.rpm
```

---

### YUM

Yellowdog Updater, Modified(YUM)는 RPM Package Manager를 사용하는 Linux 운영체제를 위한 무료 오픈 소스 명령줄 패키지 관리자입니다.

> 참고: YUM 저장소는 Datadog에서 제공합니다.

#### 설치

1단계: 저장소 추가

```bash
bash -c "$(curl -L https://setup.vector.dev)"
```

2단계: Vector 설치

```bash
sudo yum install vector
```

---

## 플랫폼별 설치

### Docker

Vector는 Docker Hub의 `timberio/vector` 이미지를 통해 컨테이너로 실행할 수 있습니다.

#### Docker 이미지 가져오기

```bash
docker pull timberio/vector:0.52.0-debian
```

사용 가능한 배포판:
- `debian` - Debian slim 기반
- `alpine` - Alpine Linux 기반 (권장, 크기가 작고 안정적)
- `distroless-libc` - 동적 링크 빌드
- `distroless-static` - 정적 링크된 musl x86 빌드

#### Vector 실행

사용자 정의 설정 파일로 Vector 실행:

```bash
docker run \
  -d \
  -v $PWD/vector.yaml:/etc/vector/vector.yaml:ro \
  -p 8686:8686 \
  --name vector \
  timberio/vector:0.52.0-debian
```

다른 배포판을 사용하는 경우 `debian`을 해당 배포판으로 교체하세요.

#### Docker alias 설정

Vector 명령을 더 쉽게 실행하려면 alias를 설정할 수 있습니다:

```bash
alias vector='docker run -i -v $(pwd)/:/etc/vector/ --rm timberio/vector:0.52.0-debian'
```

#### 이미지 변형

| 이미지 | 설명 |
|--------|------|
| Alpine | musl libc와 BusyBox를 기반으로 구축된 Linux 배포판. 다른 Docker 이미지보다 크기가 상당히 작고 라이브러리를 정적으로 링크합니다. 작은 크기와 신뢰성으로 인해 권장됩니다. |
| Debian | debian-slim 이미지 기반으로, debian 이미지의 더 작고 컴팩트한 변형입니다. |
| Distroless | OS를 최소화하거나 핵심 부분을 처음부터 구축하여 만든 기본 Docker 이미지. 정적 또는 동적 링크 바이너리를 실행하는 데 필수적인 것만 포함합니다. |

#### 아키텍처 지원

Vector의 이미지는 멀티 아키텍처를 지원하며 x86_64, ARM64, ARMv7 아키텍처를 지원합니다. Docker가 이를 투명하게 처리합니다.

---

### Kubernetes

Kubernetes에서 Vector를 Helm, kubectl 또는 Vector Operator를 사용하여 설치할 수 있습니다.

#### Helm을 사용한 설치

위의 [Helm](#helm) 섹션을 참조하세요.

#### kubectl을 사용한 설치

Vector Agent와 Aggregator 역할로 설치하는 방법입니다. Vector를 자체 Kubernetes 네임스페이스에서 실행하는 것이 좋으며, 일반적으로 'vector'를 네임스페이스로 사용합니다.

Agent로 설치:

Vector Agent는 소스에서 데이터를 수집한 다음 싱크를 사용하여 다양한 대상으로 전달할 수 있습니다.

RBAC 구성:

최신 Kubernetes 클러스터는 역할 기반 액세스 제어(RBAC) 체계로 실행됩니다. RBAC가 활성화된 클러스터는 Vector가 Kubernetes API 엔드포인트에 액세스할 수 있도록 권한을 부여하기 위한 일부 구성이 필요합니다. RBAC는 현재 Kubernetes API에 대한 액세스를 제어하는 표준 방법이므로 필요한 구성이 기본 제공됩니다.

kubectl YAML 설정의 ClusterRole, ClusterRoleBinding 및 ServiceAccount와 Helm 차트의 rbac 구성을 참조하세요.

#### 권장 리소스 제한

Kubernetes에서 Vector를 Agent로 실행할 때 권장되는 리소스 제한:

```yaml
resources:
  requests:
    memory: "64Mi"
    cpu: "500m"
  limits:
    memory: "1024Mi"
    cpu: "6000m"
```

#### Pod 제외 설정

기본적으로 `kubernetes_logs` 소스는 `vector.dev/exclude: "true"` 레이블이 있는 Pod의 로그를 건너뜁니다. 레이블 또는 필드 선택기를 통해 추가 제외 규칙을 구성할 수 있습니다.

#### Vector Operator

Vector Operator(kaasops에서 제공)는 Helm을 통해 설치할 수 있습니다:

```bash
helm install vector-operator vector-operator/vector-operator
```

Operator는 노드 파일 시스템에서 컨테이너 및 애플리케이션 로그를 수집하기 위해 모든 노드에 vector agent daemonset을 배포하고 구성합니다.

---

## 운영체제별 설치

### Amazon Linux

Amazon Linux에 Vector를 설치하려면 [YUM](#yum) 또는 [RPM](#rpm) 패키지 관리자를 사용하세요.

### Arch Linux

Arch Linux에 Vector를 설치하려면 [pacman](#pacman)을 사용하세요.

### CentOS

CentOS에 Vector를 설치하려면 [YUM](#yum) 또는 [RPM](#rpm) 패키지 관리자를 사용하세요.

### Debian

Debian에 Vector를 설치하려면 [APT](#apt) 또는 [dpkg](#dpkg) 패키지 관리자를 사용하세요.

### Fedora

Fedora에 Vector를 설치하려면 [YUM](#yum) 또는 [RPM](#rpm) 패키지 관리자를 사용하세요.

### macOS

macOS에 Vector를 설치하는 방법:

#### Homebrew 사용 (권장)

```bash
brew tap vectordotdev/brew
brew install vector
```

#### Vector 설치 스크립트 사용

```bash
curl --proto '=https' --tlsv1.2 -sSfL https://sh.vector.dev | bash
```

#### 아카이브에서 설치

```bash
mkdir -p vector && \
curl -sSfL --proto '=https' --tlsv1.2 https://packages.timber.io/vector/0.52.0/vector-0.52.0-x86_64-apple-darwin.tar.gz | \
tar xzf - -C vector --strip-components=2
```

### NixOS

NixOS는 Nix 패키지 관리자 위에 구축된 Linux 배포판입니다.

Nixpkgs에는 Vector에 대한 커뮤니티 유지 관리 모듈이 있으며, 옵션은 NixOS Search에서 볼 수 있습니다. 이를 사용하여 NixOS 시스템에서 Vector를 배포하고 구성할 수 있으며, 모듈은 변경 사항을 활성화하기 전에 Vector 구성이 유효한지 확인합니다.

[Nix](#nix) 섹션을 참조하세요.

### Raspbian

Raspbian에 Vector를 설치하려면 [APT](#apt) 또는 [dpkg](#dpkg) 패키지 관리자를 사용하세요.

### RHEL

RHEL에 Vector를 설치하려면 [YUM](#yum) 또는 [RPM](#rpm) 패키지 관리자를 사용하세요.

### Ubuntu

Ubuntu에 Vector를 설치하려면 [APT](#apt) 또는 [dpkg](#dpkg) 패키지 관리자를 사용하세요.

### Windows

Windows에 Vector를 설치하는 방법:

#### MSI 설치 프로그램 사용 (권장)

PowerShell에서:

```powershell
Invoke-WebRequest https://packages.timber.io/vector/0.52.0/vector-x64.msi -OutFile vector-0.52.0-x64.msi
msiexec /i vector-0.52.0-x64.msi
```

#### 아카이브에서 설치

PowerShell에서:

```powershell
Invoke-WebRequest https://packages.timber.io/vector/0.52.0/vector-0.52.0-x86_64-pc-windows-msvc.zip -OutFile vector.zip
Expand-Archive vector.zip -DestinationPath .
```

Vector 시작:

```powershell
.\bin\vector --config config\vector.yaml
```

---

## 수동 설치

### Vector 설치 스크립트

Vector 설치 스크립트를 사용하면 플랫폼에 구애받지 않는 설치 스크립트로 Vector를 설치할 수 있습니다.

#### 기본 설치

```bash
curl --proto '=https' --tlsv1.2 -sSfL https://sh.vector.dev | bash
```

#### 특정 버전 설치

```bash
curl --proto '=https' --tlsv1.2 -sSfL https://sh.vector.dev | VECTOR_VERSION=0.52.0 bash
```

---

### 아카이브에서 설치

이 설치 방법은 사전 빌드된 아카이브에서 Vector를 설치하는 방법을 다룹니다. 이러한 아카이브에는 vector 바이너리와 지원 구성 파일이 포함되어 있습니다.

> 권장사항: 가능하면 지원되는 플랫폼 또는 패키지 관리자를 통해 설치하는 것이 좋습니다. 이들은 권한, 디렉토리 생성 및 기타 복잡한 사항을 처리합니다.

#### Linux (x86_64, GNU libc)

```bash
mkdir -p vector && \
curl -sSfL --proto '=https' --tlsv1.2 https://packages.timber.io/vector/0.52.0/vector-0.52.0-x86_64-unknown-linux-gnu.tar.gz | \
tar xzf - -C vector --strip-components=2
```

#### Linux (x86_64, musl)

```bash
mkdir -p vector && \
curl -sSfL --proto '=https' --tlsv1.2 https://packages.timber.io/vector/0.52.0/vector-0.52.0-x86_64-unknown-linux-musl.tar.gz | \
tar xzf - -C vector --strip-components=2
```

#### Linux (ARM64)

```bash
mkdir -p vector && \
curl -sSfL --proto '=https' --tlsv1.2 https://packages.timber.io/vector/0.52.0/vector-0.52.0-aarch64-unknown-linux-musl.tar.gz | \
tar xzf - -C vector --strip-components=2
```

#### Linux (ARMv7)

```bash
mkdir -p vector && \
curl -sSfL --proto '=https' --tlsv1.2 https://packages.timber.io/vector/0.52.0/vector-0.52.0-armv7-unknown-linux-gnueabihf.tar.gz | \
tar xzf - -C vector --strip-components=2
```

#### macOS (x86_64)

```bash
mkdir -p vector && \
curl -sSfL --proto '=https' --tlsv1.2 https://packages.timber.io/vector/0.52.0/vector-0.52.0-x86_64-apple-darwin.tar.gz | \
tar xzf - -C vector --strip-components=2
```

#### Windows (x86_64)

PowerShell에서:

```powershell
Invoke-WebRequest https://packages.timber.io/vector/0.52.0/vector-0.52.0-x86_64-pc-windows-msvc.zip -OutFile vector.zip
Expand-Archive vector.zip -DestinationPath .
```

#### 설치 후 단계 (Linux/macOS)

1. vector 디렉토리로 이동:
   ```bash
   cd vector
   ```

2. Vector를 $PATH에 추가:
   ```bash
   echo "export PATH=\"$(pwd)/vector/bin:\$PATH\"" >> $HOME/.profile
   source $HOME/.profile
   ```

3. Vector 시작:
   ```bash
   vector --config config/vector.yaml
   ```

#### 다운로드 URL 형식

일반적인 다운로드 URL 형식:
```
https://packages.timber.io/vector/{version}/vector-{version}-{arch}.tar.gz
```

---

### 소스에서 빌드

Vector는 호스트의 네이티브 툴체인을 사용하여 소스에서 설치할 수 있습니다. 또한 x86_64, ARM64 및 ARMv7 아키텍처용 Linux를 위한 정적 바이너리로 컴파일할 수도 있습니다.

#### 필수 조건

1. Rust 설치

```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --default-toolchain stable
```

2. Protoc 설치

```bash
./scripts/environment/install-protoc.sh
```

3. 기타 의존성 설치 (Ubuntu/Debian 예시)

```bash
sudo apt-get update
sudo apt-get install -y build-essential cmake curl git
```

#### 빌드

저장소를 복제하고 빌드:

```bash
git clone https://github.com/vectordotdev/vector
cd vector
make build
```

`FEATURES` 환경 변수를 사용하여 기본 기능을 재정의할 수 있습니다:

```bash
FEATURES="<flag1>,<flag2>,..." make build
```

빌드 후 바이너리가 생성됩니다:
- Windows: `vector.exe`
- Linux/macOS: `target/release/vector`

다음 명령으로 시작할 수 있습니다:

```bash
./target/release/vector --config config/vector.yaml
```

#### 사용자 정의 기능 빌드

Vector는 빌드에 포함할 기능을 사용자 정의할 수 있는 많은 기능 플래그를 지원합니다. 기본적으로 모든 소스, 변환 및 싱크가 활성화됩니다. 전체 기능 목록은 Cargo.toml의 "[features]" 아래에 나열되어 있습니다.

특정 구성 요소만으로 빌드하는 예:

```bash
FEATURES="api,sources-file,transforms-remap,sinks-console" make build
```

#### Windows 빌드

Windows에서는 rustup, VC++ 빌드 도구(프롬프트가 표시되면), CMake, Protoc, Perl을 설치합니다. Perl을 PATH에 추가합니다.

Windows 빌드의 경우:

```bash
git clone https://github.com/vectordotdev/vector
git checkout v0.52.0
cd vector
set RUSTFLAGS=-Ctarget-feature=+crt-static
cargo build --no-default-features --features default-msvc --release
```

#### Docker 빌드

이러한 명령은 대상 아키텍처용 Rust 툴체인이 포함된 Docker 이미지를 빌드합니다. musl 대상은 완전히 정적인 바이너리를 생성하고 gnu 대상은 glibc에 대해 링크합니다. 컴파일된 패키지는 `target/artifacts/`에 위치합니다.

#### 프로파일 가이드 최적화 (PGO)

최적화된 빌드를 위해 Vector 소스 디렉토리로 이동하여 `cargo pgo build`를 실행합니다. 이렇게 하면 계측된 Vector 버전이 빌드됩니다.

```bash
cargo pgo run -- -- -c vector.toml
```

충분한 정보를 수집하기 위해 테스트 로드에서 계측된 Vector를 실행하고 잠시 기다립니다.

그런 다음 PGO 최적화로 Vector를 빌드합니다:

```bash
cargo pgo optimize
```

---

## 설치 후 설정

### 구성 파일 위치

설치 방법에 따른 Vector 구성 파일 위치:

| 플랫폼 | 위치 |
|--------|------|
| Linux (APT, dpkg, RPM, YUM) | `/etc/vector/vector.yaml` |
| macOS (Homebrew) | `/opt/homebrew/etc/vector/vector.yaml` 또는 `/usr/local/etc/vector/vector.yaml` |
| Windows | `C:\Program Files\Vector\config\vector.yaml` |
| Docker | `/etc/vector/vector.yaml` |

> 참고: 기본 구성 위치 `/etc/vector/vector.toml`은 이제 폐기되었습니다. 이 위치는 v0.33.0에서 존재하는 경우 계속 사용되지만, v0.34.0부터 Vector는 먼저 `/etc/vector/vector.yaml`을 찾습니다. 사용자는 TOML 구성을 이 새 경로의 YAML로 변환하는 것이 좋습니다.

### 지원되는 구성 형식

Vector는 TOML, YAML, JSON 파일 형식을 지원합니다. 파일 확장자(.yaml, .toml, .json)에서 해석할 형식이 결정됩니다. 지원되는 형식을 감지할 수 없는 경우 Vector는 YAML로 대체합니다.

### 여러 구성 파일

Vector를 시작할 때 여러 구성 파일을 전달할 수 있습니다:

```bash
vector --config vector1.yaml --config vector2.yaml
```

`VECTOR_CONFIG_DIR` 설정을 사용하여 네임스페이스가 지정된 멀티파일 기능을 배포할 수 있습니다.

### systemctl을 사용한 Vector 관리

APT, dpkg, RPM, YUM 또는 pacman을 사용하여 Vector를 설치한 경우 systemctl을 사용하여 관리할 수 있습니다.

Vector 시작:
```bash
sudo systemctl start vector
```

Vector 중지:
```bash
sudo systemctl stop vector
```

Vector 재시작 (구성 변경 후):
```bash
sudo systemctl restart vector
```

Vector 상태 확인:
```bash
sudo systemctl status vector
```

부팅 시 자동 시작 활성화:
```bash
sudo systemctl enable vector
```

### 버전 확인

```bash
vector --version
```

---

## 참고 사항

### 패키지 저장소 변경

패키지가 `apt.vector.dev` 및 `yum.vector.dev`로 이동되었습니다. `repositories.timber.io`에 있던 저장소는 2024년 2월 28일에 폐기되었습니다.

### musl vs glibc 성능

musl은 Vector가 여러 스레드에서 실행될 때 glibc보다 성능 프로파일이 상당히 낮습니다. 단일 CPU에서 Vector를 실행하지 않는 한 glibc를 사용하는 것이 좋습니다.

### 다운로드 페이지

모든 파일은 공식 다운로드 페이지에서 찾을 수 있습니다: https://vector.dev/download/

### GitHub 저장소

Vector 소스 코드 및 이슈: https://github.com/vectordotdev/vector

---

## 참조 링크

- [Vector 공식 문서](https://vector.dev/docs/)
- [Vector 설치 가이드](https://vector.dev/docs/setup/installation/)
- [Vector 빠른 시작](https://vector.dev/docs/setup/quickstart/)
- [Vector 구성 가이드](https://vector.dev/docs/reference/configuration/)
- [Vector GitHub](https://github.com/vectordotdev/vector)
- [Vector Docker Hub](https://hub.docker.com/r/timberio/vector)
- [Vector Helm Charts](https://github.com/vectordotdev/helm-charts)
