# 서브넷 라우터: 어떻게 작동하나요?

원문: [Subnet routers: how do they work?](https://tailscale.com/blog/subnet-router-video)

게시일: 2024년 6월 20일

작성자: Alex Kretzschmar

---

## 개요

Tailscale의 가장 일반적인 사용 사례는 디바이스에 클라이언트를 직접 설치하는 것입니다. 하지만 이것이 항상 가능하지는 않습니다. 예를 들어, 임베디드 시스템, IoT 디바이스, 또는 기존 대규모 인프라에서는 모든 디바이스에 클라이언트를 설치하는 것이 현실적이지 않을 수 있습니다.

이런 상황에서 서브넷 라우터(Subnet Router)가 해결책이 됩니다. 서브넷 라우터를 사용하면 Tailscale을 실행하든 실행하지 않든 관계없이 디바이스들이 강력한 NAT 순회(NAT traversal) 기술을 사용하여 통신할 수 있습니다.

## 영상 튜토리얼

<iframe width="560" height="315" src="https://www.youtube.com/embed/UmVMaymH1-s" frameborder="0" allowfullscreen></iframe>

[YouTube에서 보기](https://www.youtube.com/watch?v=UmVMaymH1-s)

이 콘텐츠는 "Tailscale 설명(Tailscale Explained)" 비디오 시리즈의 일부입니다.

## 서브넷 라우터란?

서브넷 라우터는 Tailscale 네트워크(tailnet이라고 함)를 Tailscale 클라이언트를 실행하지 않거나 실행할 수 없는 디바이스로 확장할 수 있게 해줍니다. 서브넷 라우터는 Tailscale 네트워크와 물리적 서브넷을 연결하는 게이트웨이 역할을 합니다.

## 주요 사용 사례

### 1. 마이그레이션 테스트
대규모 네트워크 마이그레이션 중에 모든 디바이스에 클라이언트를 설치하지 않고도 Tailscale을 테스트할 수 있습니다.

### 2. AWS VPC 연결
전체 AWS VPC를 개별 디바이스 설치 없이 연결할 수 있습니다.

### 3. 임베디드 디바이스 및 레거시 시스템
프린터, 카메라, 센서 등 직접 클라이언트 설치가 불가능한 디바이스에 접근할 수 있습니다.

### 4. 빠른 배포
광범위한 클라이언트 설치 없이 신속하게 배포할 수 있습니다.

## Linux에서 서브넷 라우터 설정

### 1단계: IP 포워딩 활성화

시스템에 따라 다음 명령어를 사용합니다:

```bash
echo 'net.ipv4.ip_forward = 1' | sudo tee -a /etc/sysctl.d/99-tailscale.conf
echo 'net.ipv6.conf.all.forwarding = 1' | sudo tee -a /etc/sysctl.d/99-tailscale.conf
sudo sysctl -p /etc/sysctl.d/99-tailscale.conf
```

### 2단계: 라우트 광고(Advertise Routes)

```bash
sudo tailscale set --advertise-routes=192.0.2.0/24,198.51.100.0/24
```

### firewalld 사용 시

```bash
firewall-cmd --permanent --add-masquerade
```

## Windows에서 서브넷 라우터 설정

### 라우트 광고

```powershell
tailscale set --advertise-routes=192.0.2.0/24,198.51.100.0/24
```

참고: Windows에서는 라우트 광고 전에 IP 포워딩을 활성화해야 합니다.

## 관리 콘솔에서 라우트 승인

1. [관리 콘솔](https://login.tailscale.com/admin/machines)의 Machines 페이지로 이동합니다
2. `property:subnet` 필터를 사용하여 서브넷 라우터 디바이스를 찾습니다
3. 디바이스를 선택하고 Subnets 섹션으로 이동합니다
4. Edit를 선택하고 승인할 라우트를 선택한 후 Save를 클릭합니다

## 고급 기능

### DNS 라우팅
스플릿 DNS(Split DNS) 구성을 통해 내부 DNS 서버로 라우팅할 수 있습니다.

### 고가용성(High Availability)
이중화 및 장애 조치를 위한 고가용성 설정이 가능합니다.

### 중복 라우트(Overlapping Routes)
세밀한 제어를 위해 최장 접두사 매칭(Longest Prefix Matching, LPM)을 사용한 중복 라우트를 구성할 수 있습니다.

### SNAT 비활성화
원본 소스 IP를 보존하려면 `--snat-subnet-routes=false` 옵션을 사용합니다 (Linux 전용):

```bash
sudo tailscale set --snat-subnet-routes=false
```

## 중요 참고사항

- 커넥터의 키가 만료되면 광고된 라우트는 구성된 상태로 유지되지만 접근할 수 없게 됩니다
- 중단을 방지하려면 커넥터에서 키 만료를 비활성화하거나 고가용성을 구성하세요

## 가격 정책

Tailscale은 개인 사용 시 무료이며, 서브넷 라우터 뒤에 있는 디바이스는 요금제의 디바이스 제한에 포함되지 않습니다. 최대 100대의 디바이스와 3명의 사용자를 무료로 연결할 수 있습니다.

---

## 시작하기

서브넷 라우터를 사용하여 Tailscale 네트워크를 확장해 보세요.

- [Tailscale 다운로드](https://tailscale.com/download)
- [서브넷 라우터 문서](https://tailscale.com/kb/1019/subnets)
- [영업팀 문의](https://tailscale.com/contact/sales)

---

## 관련 링크

- [Reddit 커뮤니티](https://www.reddit.com/r/Tailscale/)
- [X (구 Twitter)](https://twitter.com/tailscale)
- [Mastodon](https://hachyderm.io/@tailscale)
- [Bluesky](https://bsky.app/profile/tailscale.com)
