# Caddy를 사용하여 Tailscale HTTPS 인증서 관리하기

게시일: 2022년 3월 15일

저자: Brad Fitzpatrick
- 직책: 수석 엔지니어 (Chief Engineer)
- 소개: "Brad는 LiveJournal, memcached, OpenID를 만든 것으로 유명한 선구적인 소프트웨어 엔지니어입니다..."

---

## 본문

테일넷(tailnet)에서 일반 HTTP를 통해 웹 애플리케이션에 연결하면 브라우저에서 보안 경고가 표시될 수 있습니다. 테일넷 연결은 네트워크 계층에서 종단 간 암호화(end-to-end encryption)를 제공하는 WireGuard를 사용하지만, 브라우저는 이러한 암호화를 인식하지 못합니다. 따라서 브라우저는 해당 도메인에 대한 유효한 TLS 인증서를 찾게 됩니다. 내부 웹 앱의 경우 이로 인해 사용자들이 혼란스러워할 수 있으므로, Tailscale은 이미 `tailscale cert` 명령을 통해 내부 웹 애플리케이션을 위한 Let's Encrypt HTTPS 인증서를 프로비저닝할 수 있도록 지원하고 있습니다.

하지만 공개 웹 서버를 운영하는 경우, 테일넷에서 HTTPS를 통해 사이트를 제공하려면 Tailscale에서 인증서를 가져와야 합니다. Caddy는 오픈 소스 웹 서버로, 대부분의 웹 서버와 달리 HTTPS 인증서를 자동으로 프로비저닝하고 관리해줍니다. (우리는 Caddy가 기본적으로 HTTPS를 사용하기 때문에 좋아합니다!) Caddy는 또한 이러한 인증서의 갱신도 자동으로 관리합니다.

**Caddy 2.5 베타 버전이 출시되면서, Caddy는 Tailscale 네트워크(`*.ts.net`)용 인증서를 자동으로 인식하고 사용할 수 있게 되었으며, 새 서비스를 시작할 때 Tailscale의 HTTPS 인증서 프로비저닝 기능을 활용할 수 있습니다.**

Tailscale 네트워크에서 Caddy를 사용하려면, 먼저 테일넷에서 HTTPS 인증서가 활성화되어 있는지 확인하세요. 그런 다음 Caddy를 root로 실행하거나, Caddy 사용자가 Tailscale 소켓에 접근할 수 있도록 설정해야 합니다.

그 외에 별도로 해야 할 것은 없습니다. Caddy는 특별한 설정 없이도 `*.ts.net` 도메인용 인증서를 Tailscale에서 자동으로 가져옵니다. 더 자세한 내용은 문서를 참조하세요.

다음은 최소한의 Caddyfile 예제입니다:

```
machine-name.domain-alias.ts.net

root * /var/www
file_server
```

Tailscale에서 웹 서버를 실행하려면 Caddy를 시작해 보세요.

---

관련 링크:
- [Caddy 문서](https://caddyserver.com/docs/getting-started)
- [Tailscale HTTPS 인증서 가이드](/kb/1190/caddy-certificates/)

---

*원문: [https://tailscale.com/blog/caddy](https://tailscale.com/blog/caddy)*
