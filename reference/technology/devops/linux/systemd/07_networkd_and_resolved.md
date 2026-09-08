# systemd-networkd와 systemd-resolved

## systemd-networkd와 systemd-resolved

> 원본: https://www.freedesktop.org/software/systemd/man/systemd-networkd.html , https://www.freedesktop.org/software/systemd/man/systemd-resolved.service.html

<a id="네트워크-관리-옵션"></a>
### 네트워크 관리 옵션

Linux에는 여러 네트워크 구성 도구가 있다. 데스크톱의 Wi-Fi 연결부터 서버의 정적 설정까지, 관리할 환경에 따라 선택이 달라진다.

- `NetworkManager`: 데스크탑, 노트북용, GUI 지원, Wi-Fi/VPN 강함 → 워크스테이션, 노트북에 적합
- `systemd-networkd`: 정적, 선언적, 가벼움 → 서버, 컨테이너, IoT에 적합
- `netplan`: Ubuntu의 YAML 추상화, 백엔드는 위 둘 중 하나 사용 → Ubuntu 서버에 적합
- `ifupdown`: 옛날 스타일(`/etc/network/interfaces`) → 레거시 Debian에 적합

여기서는 서버 환경에서 설정을 단순하게 유지하고 멱등성(idempotency)을 확보하기 좋은 `systemd-networkd`를 다룬다.

<a id="systemd-networkd"></a>

### systemd-networkd

먼저 서비스를 활성화한다.

```bash
sudo systemctl enable --now systemd-networkd
```

관리자 설정은 `/etc/systemd/network/`에, 배포판 설정은 `/usr/lib/systemd/network/`에 둔다. 설정 파일은 역할에 따라 다음 세 종류로 나뉜다.

#### 파일 종류

- `.network`: 인터페이스 IP, 라우트, DHCP 설정
- `.netdev`: 가상 디바이스 생성(bridge, bond, vlan, wireguard 등)
- `.link`: 디바이스 이름, MAC 등 udev 시점 속성

파일 이름의 알파벳 순서로 우선순위가 정해지므로, 보통 `10-wired.network`, `20-vlan.network`처럼 숫자 접두사를 붙여 순서를 드러낸다.

<a id="network-파일"></a>

### .network 파일

다음 설정은 `eth0`에 정적 IP, 게이트웨이, DNS 서버를 지정한다. `[Match]`에서 대상을 고르고 `[Network]`에서 적용할 설정을 작성한다.

```ini
# /etc/systemd/network/10-wired.network
[Match]
Name=eth0

[Network]
Address=192.168.1.10/24
Gateway=192.168.1.1
DNS=1.1.1.1
DNS=8.8.8.8
NTP=time.cloudflare.com
IPForward=yes
```

#### [Match] 섹션

`[Match]`는 설정을 적용할 인터페이스를 결정한다. 이름 외에도 MAC 주소, 드라이버 등으로 대상을 지정할 수 있다.

```ini
[Match]
Name=eth*               # glob
MACAddress=aa:bb:cc:dd:ee:ff
Driver=virtio_net
Type=ether
Virtualization=kvm
Host=server01
```

#### [Network] 섹션

```ini
[Network]
DHCP=yes                # IPv4와 IPv6 모두 DHCP
DHCP=ipv4               # IPv4만
Address=10.0.0.5/24     # 정적 IP
Gateway=10.0.0.1
DNS=10.0.0.1
Domains=local.example   # 검색 도메인
LLMNR=no
MulticastDNS=no
DNSOverTLS=opportunistic
DNSSEC=no
IPv6AcceptRA=yes
IPMasquerade=ipv4
IPForward=yes
```

#### [DHCPv4]/[DHCPv6]

```ini
[DHCPv4]
UseDNS=yes
UseNTP=yes
UseRoutes=yes
RouteMetric=100
ClientIdentifier=mac
```

#### [Route]

```ini
[Route]
Gateway=10.0.0.254
Destination=10.10.0.0/16
Metric=200
```

경로가 여러 개라면 `[Route]` 섹션을 반복해서 작성한다.

#### [Address]

주소도 `[Address]` 섹션을 반복해 여러 개 부여할 수 있다.

```ini
[Address]
Address=192.0.2.1/24
Scope=global

[Address]
Address=fd00::1/64
```

<a id="netdev--link-파일"></a>

### .netdev / .link 파일

#### .netdev: 가상 디바이스

- Bridge

  ```ini
  # /etc/systemd/network/10-br0.netdev
  [NetDev]
  Name=br0
  Kind=bridge
  ```

  ```ini
  # /etc/systemd/network/20-eth0.network
  [Match]
  Name=eth0
  [Network]
  Bridge=br0
  ```

  ```ini
  # /etc/systemd/network/30-br0.network
  [Match]
  Name=br0
  [Network]
  Address=192.168.1.10/24
  Gateway=192.168.1.1
  ```

- VLAN

  ```ini
  # /etc/systemd/network/10-vlan100.netdev
  [NetDev]
  Name=vlan100
  Kind=vlan

  [VLAN]
  Id=100
  ```

- Bond

  ```ini
  # /etc/systemd/network/10-bond0.netdev
  [NetDev]
  Name=bond0
  Kind=bond

  [Bond]
  Mode=802.3ad
  LACPTransmitRate=fast
  MIIMonitorSec=100ms
  ```

- WireGuard

  ```ini
  # /etc/systemd/network/10-wg0.netdev
  [NetDev]
  Name=wg0
  Kind=wireguard

  [WireGuard]
  PrivateKey=<base64>
  ListenPort=51820

  [WireGuardPeer]
  PublicKey=<peer-pubkey>
  AllowedIPs=10.0.0.0/24
  Endpoint=peer.example:51820
  PersistentKeepalive=25
  ```

#### .link: udev 시점 속성

```ini
# /etc/systemd/network/10-eth0.link
[Match]
MACAddress=aa:bb:cc:dd:ee:ff

[Link]
Name=wan0
MTUBytes=9000
WakeOnLan=magic
```

`.link` 설정은 udev가 디바이스를 생성할 때 적용된다. 따라서 위 예제처럼 인터페이스 이름을 영구적으로 변경할 때 사용한다(`enp0s3` → `wan0`).

<a id="networkctl"></a>

### networkctl

설정을 적용한 뒤에는 `networkctl`로 인터페이스 상태를 조회한다. 특정 인터페이스의 상세 상태를 보거나 설정을 다시 불러올 때도 같은 도구를 사용한다.

```bash
$ networkctl
IDX LINK   TYPE     OPERATIONAL SETUP
  1 lo     loopback carrier     unmanaged
  2 eth0   ether    routable    configured
  3 wg0    wireguard routable   configured

$ networkctl status eth0
$ networkctl lldp        # LLDP 이웃 (스위치 정보)
$ networkctl reload      # 설정 reload
$ networkctl reconfigure eth0
$ networkctl up eth0     # bring up
$ networkctl down eth0
```

<a id="systemd-resolved"></a>

### systemd-resolved

`systemd-resolved`는 DNS 해석을 통합 관리하는 데몬이다. 서비스를 활성화하고 `/etc/resolv.conf`를 stub 설정에 연결하면 DNS 요청을 resolved가 처리하게 된다.

```bash
sudo systemctl enable --now systemd-resolved
sudo ln -sf /run/systemd/resolve/stub-resolv.conf /etc/resolv.conf
```

#### 기능

- 시스템 전역 DNS 캐시
- DNS over TLS (DoT)
- DNSSEC
- LLMNR / MulticastDNS
- 인터페이스별/도메인별 DNS 라우팅
- `nss-resolve` (NSS 모듈)

#### 설정

- `/etc/systemd/resolved.conf`:

```ini
[Resolve]
DNS=1.1.1.1#cloudflare-dns.com 9.9.9.9#dns.quad9.net
FallbackDNS=8.8.8.8 8.8.4.4
Domains=~.
DNSSEC=allow-downgrade
DNSOverTLS=opportunistic
MulticastDNS=no
LLMNR=no
Cache=yes
DNSStubListener=yes
ReadEtcHosts=yes
```

- `Domains=~.`: 모든 도메인을 이 DNS로 보냄
- `DNS=...#hostname`: DoT TLS hostname 검증
- `DNSStubListener=yes`: `127.0.0.53:53`에서 stub resolver 운영

#### resolv.conf 모드

이때 `/etc/resolv.conf`가 가리키는 대상에 따라 캐시 사용 여부와 DNS 요청 경로가 달라진다.

- `/run/systemd/resolve/stub-resolv.conf`: 127.0.0.53 stub 사용 → 가장 권장
- `/run/systemd/resolve/resolv.conf`: 동적 업스트림 직접 사용(캐시 우회)
- `/usr/lib/systemd/resolv.conf`: 정적(Google DNS 등)
- 직접 작성: resolved 무관

<a id="resolvectl"></a>

### resolvectl

DNS 상태와 질의 결과는 `resolvectl`로 확인한다. 인터페이스별 DNS를 런타임에 바꾸거나 캐시를 비울 때도 사용할 수 있다.

```bash
resolvectl status                # 모든 인터페이스의 DNS 상태
resolvectl status eth0           # 특정 인터페이스
resolvectl query example.com     # DNS 질의 (cache/route 디버깅)
resolvectl statistics            # 캐시 통계
resolvectl flush-caches
resolvectl dns eth0 1.1.1.1 9.9.9.9       # 런타임 DNS 변경
resolvectl domain eth0 ~example.com
resolvectl revert eth0           # 변경 되돌리기
```

#### 디버깅 예제

```bash
$ resolvectl query github.com
github.com: 140.82.114.4
            -- Information acquired via protocol DNS in 12.3ms.
            -- Data is authenticated: no; Data was acquired via local or encrypted transport: yes
            -- Data from: cache
```

<a id="실전-예제"></a>

### 실전 예제

#### 정적 IP + DNS

```ini
# /etc/systemd/network/10-static.network
[Match]
Name=eth0

[Network]
Address=10.0.0.10/24
Gateway=10.0.0.1
DNS=10.0.0.1 1.1.1.1
NTP=time.cloudflare.com
IPv6AcceptRA=no
```

#### Bridge로 VM 호스팅

```ini
# 10-br0.netdev
[NetDev]
Name=br0
Kind=bridge

# 20-eth0.network
[Match]
Name=eth0
[Network]
Bridge=br0

# 30-br0.network
[Match]
Name=br0
[Network]
DHCP=ipv4
```

#### WireGuard 클라이언트

```ini
# 10-wg0.netdev
[NetDev]
Name=wg0
Kind=wireguard

[WireGuard]
PrivateKey=...
ListenPort=51820

[WireGuardPeer]
PublicKey=...
AllowedIPs=0.0.0.0/0
Endpoint=vpn.example.com:51820
PersistentKeepalive=25
```

```ini
# 20-wg0.network
[Match]
Name=wg0

[Network]
Address=10.99.0.2/24
DNS=10.99.0.1
Domains=~.
```

<a id="참고-자료"></a>

### 참고 자료

- [man systemd.network](https://www.freedesktop.org/software/systemd/man/systemd.network.html)
- [man systemd.netdev](https://www.freedesktop.org/software/systemd/man/systemd.netdev.html)
- [man networkctl](https://www.freedesktop.org/software/systemd/man/networkctl.html)
- [man systemd-resolved](https://www.freedesktop.org/software/systemd/man/systemd-resolved.service.html)
- [man resolvectl](https://www.freedesktop.org/software/systemd/man/resolvectl.html)
