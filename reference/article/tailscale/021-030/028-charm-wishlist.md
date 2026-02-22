# Charm Wishlist를 사용한 Tailnet의 SSH 엔드포인트 탐색

게시일: 2023년 7월 19일
작성자: Parker Higgins

---

Wishlist는 터미널에서 실행하여 여러 SSH 서비스를 탐색하고 연결할 수 있는 놀랍도록 재미있는 개인화된 디렉토리입니다. 이 도구는 커맨드 라인 도구 회사인 Charm에서 만들었습니다. Wishlist를 SSH 앱과 서버를 위한 홈페이지처럼 생각하시면 됩니다. 그리고 오늘부터 Wishlist를 Tailscale과 연동할 수 있게 되어, Wishlist가 tailnet의 노드에서 사용 가능한 SSH 엔드포인트를 자동으로 탐색하고 쉽게 탐색할 수 있게 해줍니다.

Wishlist는 Wish와 bubbletea 같은 다른 인기 있는 Charm 도구들과 함께 제공됩니다. Wish는 프로토콜을 통해 터미널 애플리케이션을 사용할 수 있게 해주는 특수한 SSH 서버이고, bubbletea는 TUI(터미널 사용자 인터페이스) 애플리케이션을 만들기 위한 프레임워크입니다. 이러한 Charm 도구들과 다른 도구들을 함께 사용하면 커맨드 라인 소프트웨어의 활발한 생태계를 지원하는 데 도움이 됩니다. Wishlist는 물론 Wish 앱과 호환되지만, 다른 SSH 엔드포인트와도 호환됩니다.

Tailscale과 함께 Wishlist의 새로운 엔드포인트 탐색 기능을 사용하려면, 최신 Wishlist 릴리스를 사용하고 있는지 확인한 후 `wishlist`를 `--tailscale.key` 플래그(또는 환경 변수 `TAILSCALE_KEY`)와 함께 실행하세요. 이 값은 Tailscale API 키로 설정해야 하며, 관리자 콘솔의 Keys 페이지에서 생성할 수 있습니다. 또한 `--tailscale.net` 플래그를 tailnet의 이름으로 설정하세요. "Tailscale API 키는 장기간 사용하도록 설계되지 않았으므로, OAuth 클라이언트를 생성"하고 새 키 생성을 스크립트로 자동화하는 것이 좋습니다.

그러면 tailnet에서 사용 가능한 SSH 애플리케이션을 탐색할 수 있습니다. 해당 목록을 확장하고 싶다면, Charm에서 제공하는 CLI 애플리케이션들을 확인해 보세요.

새로 제공되는 옵션에 대한 자세한 내용은 Charm의 블로그에서 읽을 수 있으며, 여기서는 새로 지원되는 다른 엔드포인트 탐색 메커니즘과 추가 연결 규칙을 설정하기 위한 `hints` 구성에 대해 설명합니다.
