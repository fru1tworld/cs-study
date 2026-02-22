# Tailscale Fall Update Week 2025 요약

- 원문: [A recap of Tailscale's Fall Update Week 2025](https://tailscale.com/blog/fall-update-week-25-wrap)
- 작성일: 2025년 10월 31일
- 작성자: Kevin Purdy, Technical Content Manager

---

Fall Update Week가 막바지에 접어들었고, Tailscale은 이번 행사를 마치며 다른 제품이 되었습니다. 처음에 밝혔던 대로: 더 간단하고, 더 스마트하고, 더 연결되었습니다.

회사는 이번 주 동안 여러 새로운 기능과 역량을 선보이며, 그 활용법에 대한 안내도 제공했습니다. [Fall Update Week 2025 요약 페이지](https://tailscale.com/fall-update-week-25)에서 추가 세부 정보를 확인할 수 있습니다. Alex Kretzschmar(리드 개발자 애드보케이트)와 Jay Stapleton(솔루션 엔지니어)가 출연한 [웨비나](https://www.youtube.com/watch?v=jEiHwYcKKaQ)에서는 새로운 발표 내용을 탐구하고 다양한 시나리오를 논의합니다.

## 지난 주를 250 단어 이하로 요약

### Services (퍼블릭 베타)

[Services](https://tailscale.com/blog/services-beta)는 특정 머신에 종속되지 않고 테일넷에서 정의된 리소스를 사용할 수 있게 만드는 새로운 방법입니다. Services는 가상의 Tailscale IPv4 및 IPv6 주소 쌍(TailVIP)을 네트워크의 논리적 리소스에 할당할 수 있게 해주며, 해당 리소스가 Tailscale 클라이언트에서 도달 가능하기만 하면 됩니다. Services는 MagicDNS 이름을 받고, 세분화된 접근 제어를 존중하며, 클라우드 로드 밸런싱과 유사한 가용성 규칙에 기반한 지능적 라우팅을 제공합니다. 이 기능은 퍼블릭 베타 상태이며, [문서](https://tailscale.com/kb/1552/tailscale-services)와 동영상이 제공됩니다.

### Tailscale Peer Relays (퍼블릭 베타)

[Tailscale Peer Relays](https://tailscale.com/blog/peer-relays-beta)는 모든 테일넷 노드가 UDP 기반 트래픽 릴레이로 기능할 수 있게 해줍니다. 자신을 피어 릴레이로 광고함으로써, Tailscale 노드는 테일넷의 모든 피어 노드를 위해 트래픽을 릴레이할 수 있으며, 심지어 자신에게 향하는 트래픽도 릴레이할 수 있습니다. 릴레이는 접근 권한이 있는 테일넷 노드에서만 작동합니다. 직접 연결에 필적하는 속도를 달성하면서 클라우드 환경에서 유연성을 제공합니다. 이 기능 역시 퍼블릭 베타 상태이며, 관련 [문서](https://tailscale.com/kb/1591/peer-relays)가 제공됩니다.

### Multiple Tailnets (알파)

[Multiple Tailnets](https://tailscale.com/blog/multiple-tailnets-alpha)는 조직이 동일한 ID 제공자를 사용하여 여러 테일넷을 생성하고 관리할 수 있게 해줍니다. 새로운 기능 테스트, 개발 환경 운영, 고객을 위한 연결 관리 등의 사용 사례를 지원합니다. 새로운 조직이나 ID 시스템 없이도 분리가 가능합니다. 개발자는 API로 생성된 테일넷을 활용하여 고객과 프로젝트에 자체 테일넷을 제공할 수 있습니다. 이 기능은 현재 알파 상태이며, 영업팀 연락을 통해 접근할 수 있고, [문서](https://tailscale.com/kb/1509/multiple-tailnets)가 제공됩니다.

### Workload Identity Federation (퍼블릭 베타)

[Workload Identity Federation](https://tailscale.com/blog/workload-identity-beta)은 인프라와 CI/CD 시스템이 장기 유효 API 키, 인증 키, 또는 OAuth 클라이언트를 관리하지 않고도 Tailscale에 안전하게 인증할 수 있는 더 나은 방법입니다. AWS, Azure, Google Cloud, 또는 GitHub Actions에서 클라우드 호스팅된 인프라는 수명이 짧고 범위가 지정된 OIDC 기반 토큰으로 인증할 수 있습니다. 이 기능은 퍼블릭 베타 상태이며, [문서](https://tailscale.com/kb/1581/workload-identity-federation)가 제공됩니다.

### App Capabilities

[App Capabilities](https://tailscale.com/blog/app-capabilities)는 서드파티 애플리케이션이 표준 HTTP 헤더를 통해 Tailscale 권한 부여(grants)와 ID에 접근할 수 있게 해줍니다. Tailscale의 ID 기반 접근 제어를 통해 완전히 프라이빗한 테일넷에서 강력하고 안전한 애플리케이션을 구축할 수 있습니다. Tailscale의 serve 기능의 최신 버전을 사용하면, 서드파티 애플리케이션이 원하는 프로그래밍 언어로 표준 HTTP 헤더를 통해 권한 부여를 받을 수 있습니다. 불안정(unstable) 빌드에서 먼저 사용 가능하며, 1.92 안정(stable) 릴리스에 포함될 예정입니다. [문서](https://tailscale.com/kb/1312/serve#app-capabilities-header)가 제공됩니다.

## 추가 발표

### Windowed macOS UI (베타)

회사는 베타 상태인 [창 형태의 macOS UI](https://tailscale.com/blog/windowed-macos-ui-beta)도 강조했습니다. 검색, 더 나은 오류 처리, 디버깅, 기능 발견 등을 제공할 수 있는 공간을 확보한 창 형태의 앱입니다. 기존 메뉴 바 앱과 함께 실행되며, Tailscale v1.88 이상에서 사용 가능합니다. CLI만이 아닌 데스크톱에서도 더 쉽게 검색, TailDrop 및 기타 기능에 접근할 수 있게 해줍니다.

### tsidp

회사는 또한 [tsidp](https://tailscale.com/blog/building-tsidp)의 구축에 대해서도 설명했습니다. tsidp는 테일넷 내부와 외부의 애플리케이션 모두에 ID를 전달하는 경량 ID 도구입니다. Tailscale을 인식하는 경량 ID 제공자를 구축했으며, 여러분도 구축할 수 있습니다. tsnet, 애플리케이션 capability grants, 그리고 Funnel을 사용하면, 테일넷 내부와 외부 모두에서 작동하는 안전한 애플리케이션을 빠르게 구축하고 구성할 수 있습니다. [GitHub에서 tsidp](https://github.com/tailscale/tsidp)를 확인해 보세요.

## 모든 것의 출발점

엔지니어링, 제품, 마케팅, 지원, 커뮤니티, 문서화, 영업 팀이 수개월에 걸쳐 협력하여 이번 업데이트를 제공했으며, 네트워킹을 덜 복잡하게 만들기 위해 노력했습니다.

사용자들은 [YouTube 채널](https://www.youtube.com/@Tailscale), 소셜 플랫폼([Bluesky](https://bsky.app/profile/tailscale.com), [Mastodon](https://hachyderm.io/@tailscale), [LinkedIn](https://www.linkedin.com/company/tailscale), [X](https://x.com/tailscale)), [블로그 RSS 피드](https://tailscale.com/blog/index.xml), [Reddit](https://www.reddit.com/r/Tailscale/), 그리고 [Discord](https://discord.com/invite/tailscale)를 통해 최신 정보를 받아볼 수 있습니다.

---

## Fall Update Week 2025 주요 기능 요약

| 기능 | 상태 | 설명 |
|------|------|------|
| Visual Policy Editor | 정식 출시(GA) | HuJSON 정책 파일의 시각적 편집기, 테이블 형태로 정책 파일 섹션 보기 및 편집 |
| Multiple Tailnets | 알파 | 동일한 ID 제공자로 여러 테일넷 생성 및 관리 |
| Tailscale Services | 베타 | 머신에 종속되지 않는 가상 IP 기반 서비스 정의 |
| Workload Identity Federation | 베타 | 장기 유효 키 없이 OIDC 기반 인증 |
| Tailscale Peer Relays | 베타 | 고객 배포 및 관리 트래픽 릴레이 메커니즘 |
| Windowed Desktop Client (macOS) | 베타 | 검색, 오류 처리, 디버깅 기능을 갖춘 창 형태의 앱 |
| tsidp | 사용 가능 | 테일넷 내외부 애플리케이션을 위한 경량 ID 제공자 |
| App Capabilities | 불안정/1.92 안정 | HTTP 헤더를 통한 서드파티 앱의 권한 부여 및 ID 접근 |
