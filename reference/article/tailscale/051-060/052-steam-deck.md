# Steam Deck에 Tailscale 설치하기

작성자: Xe Iaso
작성일: 2022년 7월 18일

---

이 글은 설치 스크립트로 대체되었습니다. [Steam Deck에 Tailscale 설치하기](https://github.com/tailscale-dev/deck-tailscale)

Tailscale은 컴퓨터들 간의 보안 연결을 가능하게 합니다. Steam Deck—Valve의 휴대용 Linux 컴퓨터—은 이러한 네트워크 접근 방식에 아주 적합한 기기입니다. SteamOS라는 Arch Linux의 변형을 실행하며, Nintendo Switch처럼 데스크톱급 게임을 휴대하며 플레이할 수 있습니다.

Steam Deck에 Tailscale을 설치하면 NAS와 같은 내부 서비스에 접근하고 원격으로 홈 네트워크에 도달할 수 있습니다. Taildrop은 SSH 인증 없이 파일을 전송합니다—수동으로 키를 관리할 필요 없이 모드를 추가하기에 편리합니다.

Tailscale은 표준 네트워크 기능에 의해서만 제한되는 가능성을 제공하는 네트워킹 기반으로 기능합니다. 이러한 유연성은 수많은 옵션이 주어졌을 때 결정 마비를 일으킬 수 있습니다. 여기서의 목표는 전통적인 네트워킹의 복잡성에 짓눌려 있다면 결코 고려하지 않았을 네트워킹 가능성에 대한 실험을 장려하는 실용적인 접근 방식을 보여주는 것입니다.

## 그냥 PC일 뿐

가장 간단한 설치 방법은 SteamOS를 다른 Linux 배포판으로 교체하는 것입니다. Valve는 Windows 설치를 포함한 완전한 OS 교체를 허용합니다. 그러나 이 접근 방식은 SteamOS 기능을 포기하게 되며, SteamOS에서 직접 Tailscale을 실행하는 흥미로운 도전을 놓치게 됩니다.

Steam Deck은 SteamOS를 실행하는 싱글 보드 컴퓨터와 게임 컨트롤러를 결합합니다. SteamOS가 Arch Linux에서 파생되고 Tailscale이 Arch 패키지로 제공되므로, 순진한 접근 방식은 간단해 보입니다: 셸에 접근하여 `pacman -S tailscale`을 실행하면 됩니다.

## 그냥 PC가 아님

이 방법은 간단해 보이지만 심각한 문제를 일으킵니다. 처음에는 작동하지만, Steam Deck의 A/B 파티션 시스템이 이 접근 방식을 망가뜨립니다.

Steam Deck은 ChromeOS와 유사한 이중 파티션을 사용합니다. 파티션 A가 부팅되면 업데이트는 파티션 B에 설치됩니다. 파티션 B가 부팅되면 업데이트는 파티션 A에 설치됩니다. 이 설계는 업데이트가 부팅 실패를 일으킬 경우 이전 파티션으로 되돌릴 수 있게 합니다—NixOS나 ZFS 부트 환경을 가진 FreeBSD와 비슷합니다.

읽기 전용 씰(readonly seal)을 직접 수정하면 하나의 대체 가능한 OS 파티션을 편집하게 됩니다. 파티션 A에 대한 변경 사항은 업데이트 중에 파티션 B로 전송되지 않습니다. 업데이트 후 파티션 B로 재부팅하면 수정 사항이 사라집니다. 그것들은 여전히 파티션 A에 존재하지만 접근할 수 없게 됩니다. 이후 업데이트는 결국 이러한 변경 사항을 완전히 제거합니다.

이 방법으로 Tailscale을 설치하면 "시한폭탄"을 만들게 됩니다—Deck이 시스템 업데이트 후 무작위로 작동을 멈춥니다. Tailscale의 평판은 "마법"같고 "그냥 작동"하는 것에 달려 있습니다. 무작위로 미래에 실패를 일으키는 설치 방법은 이 핵심 정체성과 모순됩니다.

## 선 안에서 그리기

선 밖에서 그리면 확실히 실패합니다. 대신 시스템 제약 조건 내에서 작업하세요.

`mount` 명령은 쓰기 가능한 디스크 위치를 보여줍니다. 결과를 필터링하면 `/var`, `/home`, `/opt`, `/root`, `/srv`, `/var/cache/pacman`, `/var/lib/docker`, `/var/lib/flatpak`, `/var/lib/systemd/coredump`, `/var/log`, `/var/tmp`와 같은 접근 가능한 폴더를 볼 수 있습니다.

Tailscale은 표준 패키지를 실행할 수 없는 시스템을 위해 정적 바이너리를 배포하여 ABI 비호환성 문제를 제거합니다. `~/.local/share`에 폴더를 만들고 Tailscale을 다운로드하세요:

```bash
$ mkdir -p ~/.local/share/tailscale/steamos
$ cd ~/.local/share/tailscale/steamos
$ wget https://pkgs.tailscale.com/stable/tailscale_1.24.2_amd64.tgz
$ tar xzf tailscale_1.24.2_amd64.tgz
$ cd tailscale_1.24.2_amd64
```

바이너리를 테스트하세요:

```bash
$ ./tailscale version
1.24.2
  tailscale commit: 9d6867fb0ab30a33cbdfc8e583f5d39169dbb2e6
  other commit: 2d0f7ddc35aa4149e67e27d11ea317669cccdd94
  go version: go1.18.1-ts710a0d8610
```

성공하면 CLI가 작동하는 것을 확인한 것입니다. 이제 `tailscaled` 노드 에이전트를 실행해야 합니다. 여러 접근 방식이 있지만, 프로그램을 기본 환경에서 실행하는 것이 바람직합니다.

Tailscale을 Flatpak으로 배포하는 것은 불가능합니다—Tailscale은 Flatpak 샌드박스가 차단하는 저수준 커널 네트워킹 프리미티브를 필요로 합니다. 또한 Tailscale은 GUI 애플리케이션이 아닌 시스템 서비스로 기능하며, Flatpak은 시스템 서비스를 방지합니다.

더 많은 창의성이 필요합니다. Arch Linux를 기반으로 하는 SteamOS는 systemd를 광범위하게 사용합니다. 각 Steam Deck은 동일한 systemd 구성 요소를 포함하여 문서화를 단순화합니다. 광범위한 연구 끝에 Steam Deck에서 Tailscale을 실행하기 위한 두 가지 실행 가능한 접근 방식을 식별했습니다.

서비스를 실행하기 위한 여러 방법이 중복되어 보일 수 있지만, 각각은 다른 요구를 해결하고 특정 장점을 제공합니다. Steam Deck과 같은 제약된 환경—전통적인 PC보다 게임 콘솔에 더 가까운—에서 이러한 옵션은 필수적입니다. Deck의 제한은 제약을 극복하기보다 그 안에서 작업하는 것을 의미합니다.

두 가지 systemd 옵션이 있습니다:

- `systemd-run`을 사용하여 `tailscaled`를 백그라운드에서 실행
- 시스템 확장 이미지를 만들어 SteamOS에 레이어링한 다음 `tailscaled`를 정상적으로 관리

Tailscale을 포터블 서비스로 실행할 수도 있지만, 이렇게 하면 `tailscaled`가 커널 네트워킹 모드를 사용하는 것을 방지합니다. 포터블 서비스는 Flatpak과 유사한 제약을 부과하여 Steam Deck에 적합하지 않습니다. 게임 콘솔에서 모든 것을 SOCKS 프록시 서버를 사용하도록 구성하고 싶지 않을 것입니다!

## systemd-run

`systemd-run`은 가장 간단한 접근 방식입니다. 이 도구는 systemd의 DBus API를 통해 일회성 systemd 작업을 생성하여 서비스, 소켓 활성화 서비스, 타이머 작업을 마음대로 생성할 수 있습니다. 화면 잠금이나 SSH 연결 해제 후에도 지속되며, 메모리 제한, 시스템 격리, 상세한 저널 로깅, 제어 그룹 관리를 포함한 systemd 서비스의 장점을 얻습니다.

예시—systemd가 관리하는 셸 세션 생성:

```bash
(deck@taildeck ~)$ sudo systemd-run -S
[sudo] password for deck:
Running as unit: run-u97.service
Press ^] three times within 1s to disconnect TTY.
(A+)(root@taildeck deck)#
```

다른 터미널에서 임시 세션을 확인하세요:

```bash
(A+)(root@taildeck deck)# systemctl status run-u97.service
● run-u97.service - /bin/bash
     Loaded: loaded (/run/systemd/transient/run-u97.service; transient)
  Transient: yes
     Active: active (running) since Tue 2022-05-03 13:28:31 EDT; 50s ago
   Main PID: 8274 (bash)
      Tasks: 3 (limit: 17718)
     Memory: 3.6M
        CPU: 58ms
     CGroup: /system.slice/run-u97.service
             ├─8274 /bin/bash
             ├─8290 systemctl status run-u97.service
             └─8291 less

May 03 13:28:31 taildeck systemd[1]: Starting /bin/bash...
May 03 13:28:31 taildeck systemd[1]: Started /bin/bash.
```

`tailscaled`를 위한 `systemd-run` 명령은 다음과 같습니다:

```sh
sudo systemd-run \
    --service-type=notify \
    --description="Tailscale node agent" \
    -u tailscaled.service \
    -p ExecStartPre="/home/deck/.local/share/tailscale/steamos/tailscale_1.24.2_amd64/tailscaled --cleanup" \
    -p ExecStopPost="/home/deck/.local/share/tailscale/steamos/tailscale_1.24.2_amd64/tailscaled --cleanup" \
    -p Restart=on-failure \
    -p RuntimeDirectory=tailscale \
    -p RuntimeDirectoryMode=0755 \
    -p StateDirectory=tailscale \
    -p StateDirectoryMode=0700 \
    -p CacheDirectory=tailscale \
    -p CacheDirectoryMode=0750 \
    "/home/deck/.local/share/tailscale/steamos/tailscale_1.24.2_amd64/tailscaled" \
    "--state=/var/lib/tailscale/tailscaled.state" \
    "--socket=/run/tailscale/tailscaled.sock"
```

`tailscaled`를 실행한 후 다음으로 로그인하세요:

```bash
$ sudo /home/deck/.local/share/tailscale/steamos/tailscale_1.24.2_amd64/tailscale up --operator=deck --qr
```

휴대폰으로 QR 코드를 스캔하여 tailnet에 인증하고 네트워크 리소스에 접근하세요.

시스템 관리자들이 알듯이, `systemd-run`은 일회성 작업을 슈퍼 데몬화합니다—셸 세션 종료로 인한 중단 위험 없이 패키지 업그레이드에 매우 유용합니다.

이 접근 방식은 읽기 전용 씰을 깨는 것보다 장점을 제공합니다: Tailscale이 시스템 이미지를 수정하지 않고 설치되며, `tailscaled.service`가 시작될 때 systemd가 자동으로 쓰기 가능한 위치에 상태, 캐시, 런타임 디렉터리를 생성합니다. Steam Deck 업데이트가 VPN 중단 없이 안전하게 진행됩니다.

주요 단점: Tailscale이 부팅 시 자동으로 시작되지 않습니다. 해결 방법이 있습니다—`systemd/tailscaled.service`를 편집하여 `/home/deck/.local/share/tailscale/steamos` 위치를 참조하거나, 위 명령을 실행한 후 `systemctl cat tailscaled.service`를 사용하고 결과를 `/etc/systemd/system`에 배치—하지만 기본 시작 방식은 Tailscale을 시스템 전체에 통합하지 않습니다.

셸 별칭이나 `$PATH` 수정으로 이를 단순화할 수 있지만, `tailscale` 명령이 `/usr/bin`에 있으면 사용성이 상당히 향상됩니다.

이 설정은 작동하며 대부분의 요구를 처리합니다. 그러나 더 인체공학적인 옵션이 있습니다: 시스템 확장(system extensions)입니다.

## systemd-sysext

systemd 프로젝트는 최근 [시스템 확장 이미지](https://www.freedesktop.org/software/systemd/man/systemd-sysext.html)를 도입했습니다. 이것들은 패키지 설치와 유사하게 런타임에 시스템에 임의의 파일을 추가합니다. 핵심 차이점: 시스템 확장은 기본 시스템 파티션을 오버레이하여 쓰기 접근 권한이 필요하지 않습니다. 이것이 아마도 Linux가 하위 호환성 심볼릭 링크와 함께 `/usr`로 마이그레이션한 동기일 것입니다. 자세한 내용은 Lennard Pottering의 [이 블로그 포스트](https://0pointer.net/blog/testing-my-system-code-in-usr-without-modifying-usr.html)를 참조하세요.

시스템 확장은 여기서 완벽하게 작동하며, 읽기 전용 씰을 깨거나 미래의 지뢰를 피하면서 pacman으로 설치한 것처럼 Tailscale을 설치합니다. `tailscale` 명령이 다른 시스템 명령처럼 실행되어 디버깅이 용이합니다.

Tailscale의 정적 바이너리 tarball은 이미 시스템 확장 이미지에 접근합니다. 약간의 차이가 필요합니다:

- 바이너리를 `usr/bin/tailscale`과 `usr/sbin/tailscaled`에 배치
- systemd 유닛을 `usr/lib/systemd/system/tailscaled.service`에 배치
- OS 호환성을 정의하는 `extension-release` 파일 추가
- 이 단계를 건너뛰지 마세요—이것 없이는 확장 이미지가 인식되지 않습니다

다음과 같은 셸 스크립트를 실행하세요:

```sh
#!/usr/bin/env bash

set -euo pipefail

dir="$(mktemp -d)"
pushd .
cd "${dir}"

tarball="$(curl 'https://pkgs.tailscale.com/stable/?mode=json' | jq -r .Tarballs.amd64)"
version="$(echo ${tarball} | cut -d_ -f2)"

curl "https://pkgs.tailscale.com/stable/${tarball}" -o tailscale.tgz

mkdir -p tailscale/usr/{bin,sbin,lib/{systemd/system,extension-release.d}}
tar xzf tailscale.tgz

cp -vrf "tailscale_${version}_amd64"/tailscale tailscale/usr/bin/tailscale
cp -vrf "tailscale_${version}_amd64"/tailscaled tailscale/usr/sbin/tailscaled
cp -vrf "tailscale_${version}_amd64"/systemd/tailscaled.service tailscale/usr/lib/systemd/system/tailscaled.service

sed -i 's/--port.*//g' tailscale/usr/lib/systemd/system/tailscaled.service

source /etc/os-release
echo -e "SYSEXT_LEVEL=1.0\nID=steamos\nVERSION_ID=${VERSION_ID}" >> tailscale/usr/lib/extension-release.d/extension-release.tailscale

mkdir -p /var/lib/extensions
rm -rf /var/lib/extensions/tailscale
cp -vrf tailscale /var/lib/extensions/

mkdir -p /etc/default
touch /etc/default/tailscaled

popd
rm -rf "${dir}"
```

이 스크립트를 Steam Deck에 복사하고 실행하세요:

```console
$ sudo bash ./tailscale.sh
```

완료 후 시스템 확장을 병합하세요:

```console
$ sudo systemd-sysext merge
```

이것은 Tailscale을 Deck의 파일 시스템에 추가합니다. `tailscaled`를 정상적으로 시작하고 로그인하세요:

```console
$ systemctl start tailscaled.service
$ sudo tailscale up --qr --operator=deck --ssh
```

큰 QR 코드가 Deck에 나타납니다. 휴대폰으로 스캔하고 로그인하여 모든 tailnet 기능에 정상적으로 접근하세요.

## Steam Deck 플러그인 미리보기

Steam Deck 플러그인 UI가 이것을 크게 단순화할 수 있습니다. 개발 중인 플러그인이 설정을 자동화하지만 아직 출시 준비 상태에 도달하지 않았습니다. 미리보기는 다음과 같습니다:

![Steam Deck 플러그인 UI 미리보기](https://cdn.sanity.io/images/w77i7m8x/production/0243373c5240229acd3f947320a993e5b7c91d4e-1280x800.jpg)

개발이 진행됨에 따라 더 많은 세부 사항이 나옵니다.

## 재미있게 할 수 있는 것들

실험은 영감을 주는 결과를 낳았습니다. 가장 뛰어난 성과: Tailscale을 통해 NAS를 원격 마운트하고 NAS에 저장된 게임을 플레이하는 것입니다. 이것은 NAS가 오타와에 있는 동안 몬트리올의 Steam Deck에서 장시간 플레이할 수 있을 만큼 안정적임이 입증되었습니다. 간단한 게임은 약간 더 긴 로드 시간 외에는 문제가 없었습니다. 2005년 이전의 게임이 일반적으로 더 잘 작동합니다; Dead Space 2와 같이 지속적인 디스크 스트리밍이 있는 현대 타이틀은 성능이 더 떨어집니다. 적절한 Wi-Fi 조건에서 네트워크 로딩은 오버헤드 레이어에도 불구하고 DVD 드라이브 속도를 초과합니다.

테스트가 필수입니다—게임마다 스토리지 지연을 다르게 허용합니다. 일반적으로 더 간단한 게임이 더 잘 작동합니다.

### 전용 서버가 있는 게임

테스트 중 거의 모든 자체 호스팅 가능한 전용 서버 게임이 Tailscale을 통해 작동했습니다:

- Factorio
- Minecraft
- Valheim
- Starbound
- Terraria

Assetto Corsa Competizione(레이싱 시뮬레이터)는 서버 검색이 Tailscale이 지원하지 않는 브로드캐스트/멀티캐스트 패킷에 의존하기 때문에 문제가 있었습니다. 그 외 모든 것은 원활하게 작동했습니다—로컬 네트워크나 공용 인터넷 호스팅과 동일합니다. 노드 공유를 통해 친구들이 복잡한 설정 없이 서버에 참여할 수 있었습니다.

### Moonlight 게임 스트리밍

Steam Deck은 기본 게임 스트리밍 기능이 있지만 이중 NAT와 복잡한 네트워크 상황에서 어려움을 겪습니다. Moonlight는 오픈 소스 스트리밍 프로토콜로, 게임을 게이밍 타워에서 실행하고 비디오를 Steam Deck과 같은 장치로 전송합니다. KDE 데스크톱 모드 내의 Discover 스토어에서 사용할 수 있습니다.

AMD 그래픽으로 인해 이 기능을 테스트하지 못했습니다—NVIDIA 하드웨어가 이를 지원합니다. 보고서에 따르면 호환되는 기계에서 원활한 성능을 보입니다.

---

친구들과 어떤 게임을 즐기시나요? Steam Deck을 받으셨나요? 네트워킹 보안이 부담되지 않았다면 어떤 서버를 호스팅하시겠습니까? @Tailscale에 연락하거나 포럼을 방문하여 경험을 공유하세요.
