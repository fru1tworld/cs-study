# 추적과 프로파일링을 피하기 위한 자체 메타검색 엔진 호스팅

원문: [Host your own metasearch engine to avoid tracking and profiling](https://tailscale.com/blog/video-searxng)

게시일: 2024년 11월 18일

작성자: Alex Kretzschmar (개발자 관계 책임자)

기고자: Parker Higgins

---

Tailscale을 사용하면 공개적으로 노출하지 않고도 어디서든 온라인으로 비공개 서비스에 접근할 수 있습니다. 이 튜토리얼에서는 예상치 못한 리소스인 검색 엔진과 함께 Tailscale을 사용하는 방법을 보여드립니다.

SearXNG는 웹 결과를 결합하면서 개인정보를 보호하는 "완전한 오픈소스 검색 엔진 애그리게이터(aggregator)"입니다. 이 튜토리얼에서는 SearXNG를 컨테이너에 설치하고 Tailscale을 통해 어디서든 접근할 수 있도록 하는 방법을 다룹니다.

저자는 Google 서비스 사용을 최소화하려는 개인적인 도전 과정에서 SearXNG를 발견했습니다. 커뮤니티에는 이 소프트웨어를 자체 호스팅하는 다양한 동기를 가진 열정적인 사용자들이 있습니다.

## 영상 튜토리얼

<iframe width="560" height="315" src="https://www.youtube.com/embed/cg9d87PuanE" frameborder="0" allowfullscreen></iframe>

[YouTube에서 보기](https://www.youtube.com/watch?v=cg9d87PuanE)

## 자체 호스팅 메타검색 엔진을 운영하는 세 가지 주요 이유

### 1. 개인정보 보호 (Privacy)

SearXNG는 주요 검색 제공업체에서 흔히 볼 수 있는 지속적인 백그라운드 추적 없이 결과를 제공합니다. Tailscale을 통해서만 접근 가능한 자체 호스팅 인스턴스는 추가적인 보안 계층을 제공합니다.

### 2. 구성 가능성 (Configurability)

오픈소스이며 해킹 가능(hackable)하기 때문에 사용자는 데이터 소스와 결과 표시 방법을 완전히 제어할 수 있습니다. 필요에 따라 수정이 가능합니다.

### 3. 제어권 (Control)

검색 엔진 레이아웃 변경에 좌절한 사용자들은 자신이 선호하는 인터페이스를 영구적으로 유지할 수 있습니다. 상업적 제공업체가 강제하는 빈번한 변경을 받아들이는 대신 자신만의 검색 경험을 자율적으로 관리할 수 있습니다.

## 기술적 구현

비디오 튜토리얼에서는 다음을 다룹니다:

- Docker 컨테이너화: SearXNG를 Docker를 사용하여 컨테이너화합니다
- LXC 컨테이너 배포: Proxmox 인프라에서 LXC 컨테이너를 배포합니다
- Tailscale Serve 구성: tailnet(Tailscale 네트워크 용어)에서 컨테이너에 접근할 수 있도록 Tailscale Serve를 구성합니다
- 자동 TLS 인증서 프로비저닝: 브라우저 호환성을 위해 TLS 인증서가 자동으로 구성됩니다

## 커뮤니티 참여

이 게시물은 다양한 플랫폼을 통한 지속적인 참여를 권장합니다:

- YouTube 댓글
- [Reddit 커뮤니티](https://www.reddit.com/r/Tailscale/)
- [X (구 Twitter)](https://twitter.com/tailscale)
- [Mastodon](https://hachyderm.io/@tailscale)
- [Bluesky](https://bsky.app/profile/tailscale.com)

Tailscale은 커뮤니티 피드백을 중요하게 생각합니다.

---

## 시작하기

Tailscale과 SearXNG를 함께 사용하여 개인정보를 보호하면서 검색 기능을 활용해 보세요. [시작하기](https://tailscale.com/download) 또는 [영업팀 문의](https://tailscale.com/contact/sales)를 통해 자세히 알아볼 수 있습니다.
