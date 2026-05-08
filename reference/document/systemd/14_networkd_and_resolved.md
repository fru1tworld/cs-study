# systemd-networkd와 systemd-resolved

> 이 문서는 `man systemd-networkd`, `man systemd.network`, `man systemd-resolved` 의 내용을 한국어로 정리한 것입니다.
> 원본: https://www.freedesktop.org/software/systemd/man/systemd-networkd.html , https://www.freedesktop.org/software/systemd/man/systemd-resolved.service.html

---

## 목차

1. [네트워크 관리 옵션](#네트워크-관리-옵션)
2. [systemd-networkd](#systemd-networkd)
3. [.network 파일](#network-파일)
4. [.netdev / .link 파일](#netdev--link-파일)
5. [networkctl](#networkctl)
6. [systemd-resolved](#systemd-resolved)
7. [resolvectl](#resolvectl)
8. [실전 예제](#실전-예제)
9. [참고 자료](#참고-자료)

---

## 네트워크 관리 옵션

Linux의 네트워크 구성 도구는 여러 갈래입니다.

| 도구 | 특징 | 적합한 환경 |
| --- | --- | --- |
| `NetworkManager` | 데스크탑/노트북, GUI, Wi-Fi/VPN 강함 | 워크스테이션, 노트북 |
| `systemd-networkd` | 정적, 선언적, 가벼움 | 서버, 컨테이너, IoT |
| `netplan` | Ubuntu의 YAML 추상화 (백엔드는 위 둘 중 하나) | Ubuntu 서버 |
| `ifupdown` | 옛날 스타일 (`/etc/network/interfaces`) | 레거시 Debian |

systemd-networkd는 단순함과 idempotency 면에서 서버 환경에 매우 적합합니다.

---

## systemd-networkd

활성화:

```bash
sudo systemctl enable --now systemd-networkd
```

설정 파일은 `/etc/systemd/network/` (관리자) 와 `/usr/lib/systemd/network/` (배포판) 두 곳에 검색.

### 파일 종류

| 확장자 | 용도 |
| --- | --- |
| `.network` | 인터페이스 IP/라우트/DHCP 설정 |
| `.netdev` | 가상 디바이스 생성 (bridge, bond, vlan, wireguard 등) |
| `.link` | 디바이스 이름·MAC 등 udev 시점 속성 |

이름 우선순위는 알파벳 순. 보통 `10-wired.network`, `20-vlan.network` 처럼 숫자 접두사를 붙입니다.

---

## .network 파일

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

### [Match] 섹션

어느 인터페이스에 적용할지 결정.

```ini
[Match]
Name=eth*               # glob
MACAddress=aa:bb:cc:dd:ee:ff
Driver=virtio_net
Type=ether
Virtualization=kvm
Host=server01
```

### [Network] 섹션

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

### [DHCPv4]/[DHCPv6]

```ini
[DHCPv4]
UseDNS=yes
UseNTP=yes
UseRoutes=yes
RouteMetric=100
ClientIdentifier=mac
```

### [Route]

```ini
[Route]
Gateway=10.0.0.254
Destination=10.10.0.0/16
Metric=200
```

여러 [Route] 섹션을 가질 수 있습니다.

### [Address]

여러 IP 부여:

```ini
[Address]
Address=192.0.2.1/24
Scope=global

[Address]
Address=fd00::1/64
```

---

## .netdev / .link 파일

### .netdev — 가상 디바이스

#### Bridge

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

#### VLAN

```ini
# /etc/systemd/network/10-vlan100.netdev
[NetDev]
Name=vlan100
Kind=vlan

[VLAN]
Id=100
```

#### Bond

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

#### WireGuard

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

### .link — udev 시점 속성

```ini
# /etc/systemd/network/10-eth0.link
[Match]
MACAddress=aa:bb:cc:dd:ee:ff

[Link]
Name=wan0
MTUBytes=9000
WakeOnLan=magic
```

`.link` 는 udev가 디바이스를 만들 때 적용되므로 이름을 영구적으로 바꿀 때 사용 (`enp0s3` → `wan0`).

---

## networkctl

상태 조회 도구.

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

---

## systemd-resolved

DNS 해석을 통합 관리하는 데몬. `/etc/resolv.conf` 의 systemd 버전.

활성화:

```bash
sudo systemctl enable --now systemd-resolved
sudo ln -sf /run/systemd/resolve/stub-resolv.conf /etc/resolv.conf
```

### 기능

- 시스템 전역 DNS 캐시
- DNS over TLS (DoT)
- DNSSEC
- LLMNR / MulticastDNS
- 인터페이스별/도메인별 DNS 라우팅
- `nss-resolve` (NSS 모듈)

### 설정

`/etc/systemd/resolved.conf`:

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

- `Domains=~.` : 모든 도메인을 이 DNS로 보냄
- `DNS=...#hostname` : DoT TLS hostname 검증
- `DNSStubListener=yes` : `127.0.0.53:53` 에서 stub resolver 운영

### resolv.conf 모드

`/etc/resolv.conf` 를 어디로 향하게 하느냐에 따라 동작이 달라집니다.

| 심볼릭 링크 대상 | 동작 |
| --- | --- |
| `/run/systemd/resolve/stub-resolv.conf` | 127.0.0.53 stub. 가장 권장 |
| `/run/systemd/resolve/resolv.conf` | 동적 업스트림 직접 사용 (캐시 우회) |
| `/usr/lib/systemd/resolv.conf` | 정적 (Google DNS 등) |
| 직접 작성 | resolved 무관 |

---

## resolvectl

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

### 디버깅 예제

```bash
$ resolvectl query github.com
github.com: 140.82.114.4
            -- Information acquired via protocol DNS in 12.3ms.
            -- Data is authenticated: no; Data was acquired via local or encrypted transport: yes
            -- Data from: cache
```

---

## 실전 예제

### 정적 IP + DNS

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

### Bridge로 VM 호스팅

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

### WireGuard 클라이언트

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

---

## 참고 자료

- [man systemd.network](https://www.freedesktop.org/software/systemd/man/systemd.network.html)
- [man systemd.netdev](https://www.freedesktop.org/software/systemd/man/systemd.netdev.html)
- [man networkctl](https://www.freedesktop.org/software/systemd/man/networkctl.html)
- [man systemd-resolved](https://www.freedesktop.org/software/systemd/man/systemd-resolved.service.html)
- [man resolvectl](https://www.freedesktop.org/software/systemd/man/resolvectl.html)
