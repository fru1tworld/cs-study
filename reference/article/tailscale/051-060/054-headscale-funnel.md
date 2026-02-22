# Tailscale 없이 Tailscale 사용하기

저자: Xe Iaso

게시일: 2023년 4월 1일

---

이 게시물은 원래 Tailscale 커뮤니티 사이트인 tailscale.dev에 게시되었습니다. 서식 변경을 제외하고 원래 게시된 그대로 여기에 다시 게시되었습니다. tailscale.dev 게시물의 코드 및 제품 정보는 최신 상태가 아닐 수 있습니다.

> 주의: 이것은 만우절 게시물입니다. 아직 존재하지 않는 것들을 참조할 것입니다. 여기서 언급된 황당한 행위를 복제하려고 시도하기 전에 경고를 받으시기 바랍니다.

## 본문

철학자로서, 저는 최신 기술 트렌드를 파악하는 것이 유용하다고 생각합니다. 특히 최근 그 기술이 우리의 일상생활을 얼마나 많이 형성하는지를 고려하면 더욱 그렇습니다. 제가 이를 위해 사용하는 최고의 웹사이트 중 하나는 Hacker News라는 사이트입니다. 제 친구들과 동료들은 제가 이 웹사이트를 사용하는 방식에 대해 걱정합니다. 왜냐하면 그곳의 의견들이... 특별할 수 있고, 저는 그것을 일종의 초현실주의 코미디로 보는 경향이 있기 때문입니다. 그러나 제 친애하는 철학자 친구 xeonmc가 영감의 원천이 된 질문을 했습니다. 그들이 물었습니다:

> "NAT 뒤에서 Headscale 서버를 호스팅하기 위해 [Funnel]을 사용하는 것이 가능할까요?"

---

Aoi의 대화:

"잠깐, 뭐라고요? 그 사람이 Tailscale을 사용하지 않는 방식으로 Tailscale을 사용하는 방법을 물어보는 건가요? 그건 마치 자동차를 사용하지 않고 자동차를 사용하는 방법을 묻는 것과 같아요."

---

오 그렇습니다, 친애하는 여우님, 맞습니다. 그리고 오늘 저는 그러한 저주받은 광경을 어떻게 만들 수 있는지 보여드리겠습니다. 안전벨트를 매세요, 이것은 험난한 여정이 될 것입니다.

Headscale은 Tailscale 컨트롤 플레인(control plane)의 자체 호스팅 가능한 버전입니다. 훌륭한 프로젝트이며, 팬데믹 초기에 찾아온 지루함에서 비롯된 순수한 리버스 엔지니어링(reverse engineering)을 통해 그들이 성취할 수 있었던 것은 매우 놀랍습니다. Headscale 서버를 설정하면 Tailscale SaaS 제품을 사용할 필요성을 완전히 우회할 수 있습니다. 이를 통해 SaaS 컨트롤 플레인을 사용하고 싶지 않거나 사용할 수 없는 사람들이 Tailscale을 사용할 수 있게 됩니다.

그러나 이것을 호스팅하려면 무언가를 인터넷에 노출해야 합니다. 이렇게 하지 않으면, 클라이언트가 네트워크에 연결되어 있지 않은 상태에서 네트워크에 있는 것에 접근하려고 시도하지만 전혀 작동하지 않는 캐치-22 상황(Catch-22, 진퇴양난)이 발생합니다. 이때 Funnel이 등장합니다.

Funnel은 네트워크의 서비스를 인터넷에 노출할 수 있게 해주는 Tailscale의 기능입니다. 이것이 이 방정식의 빠진 부분이며, 나머지 네트워크를 위해 Tailscale(SaaS 컨트롤 플레인)을 사용하지 않으면서 Tailscale(장치들을 연결하는 서비스)을 사용할 수 있게 해주는 것입니다.

이 튜토리얼에 필요한 것들:

- SaaS 컨트롤 플레인의 Tailscale 계정 (이를 위해 일회용 Gmail 주소를 사용할 수 있습니다).
- 가상 머신을 실행할 곳 (저는 waifud라고 만든 것을 사용합니다).
- headscalenet에 참여시킬 머신들 (이를 위해 더 많은 일회용 Ubuntu를 만들 수 있습니다).
- headscalenet에 사용할 가상의 도메인 이름. 저는 이를 위해 `ts.plex-each`를 사용합니다.

---

Mara의 대화:

"`plex each`는 Talon으로 'xe'를 철자하는 방법입니다."

---

[이미지: Waifu Diffusion v1.4로 생성됨, 시골 배경에서 조깅하는 녹색 머리 소녀를 보여줍니다]

## 설정

먼저, waifud 클러스터에 새 NixOS VM을 생성합니다:

```
waifuctl create -m 4096 -c 4 -H pneuma -s 25 -d nixos-unstable -z arsene/vms
```

---

Aoi의 대화:

"잠깐, 뭐라고요. waifud는 아직 개발 중이지 않나요? libvirtd가 어떻게 작동하는지에 대한 광범위한 경험이 필요하지 않나요? 이 블로그의 무작위 독자들이 이것을 따라가기 위해 필요한 조금의 도메인 경험이라도 있을 것이라고 어떻게 기대할 수 있나요?

그리고, 제가 왜 여기 있는 거죠? 저는 xeiaso.net 블로그를 위해 만들어지지 않았나요?"

---

Mara의 대화:

"네, waifud는 아직 깊은 개발 중입니다. 로컬 waifud 클러스터가 없다면, Proxmox, ESXi, 또는 yolo-qemu와 같은 좋아하는 VM 호스팅 플랫폼을 사용할 수 있습니다. AWS, GCP, 또는 Azure와 같은 클라우드 제공업체를 사용할 수도 있습니다. 베어메탈 서버를 사용할 수도 있지만, 그것은 조금 더 복잡하고 여기서 다루고 싶지 않습니다.

그리고, 당신은 여기 없어요, 당신도 VM 안에 있어요."

---

Aoi의 대화:

"뭐라고요. 좋아요. 묻지도 않을게요."

---

> 주의: `nixos-unstable-within` 이미지를 사용하는 경우 root로 SSH 키를 설정해야 합니다. 이것은 해당 이미지, cloud-init, 그리고 NixOS가 사용자 생성 방식에서 충돌하는 알려진 문제입니다.

root로 SSH 접속하여 들어갈 수 있는지 확인합니다:

```
$ ssh root@10.77.131.232
Warning: Permanently added '10.77.131.232' (ED25519) to the list of known hosts.
Last login: Fri Mar 31 00:02:08 2023 from 10.77.131.1

[root@baelzeb-weedle:~]#
```

완벽합니다! 이제 새 터미널 창을 열고 TRAMP 모드에서 Emacs 세션에서 `/etc/nixos/configuration.nix`를 엽니다:

```
$ e /ssh:root@10.77.131.232:/etc/nixos/configuration.nix
```

파일이 존재하지 않는 경우

해당 파일이 존재하지 않는 경우 (`nixos-unstable-within` 이미지를 사용하는 경우), 이 템플릿으로 생성합니다:

```nix
{ lib, pkgs, config, ... }:

{
  boot.initrd.availableKernelModules =
    [ "ata_piix" "uhci_hcd" "virtio_pci" "sr_mod" "virtio_blk" ];
  boot.initrd.kernelModules = [ ];
  boot.kernelModules = [ ];
  boot.extraModulePackages = [ ];
  boot.growPartition = true;
  boot.kernelParams = [ "console=ttyS0" ];
  boot.loader.grub.device = "/dev/vda";
  boot.loader.timeout = 0;

  fileSystems."/" = {
    device = "/dev/disk/by-label/nixos";
    fsType = "ext4";
    autoResize = true;
  };

  networking.hostName = "baelzeb-weedle";

  systemd.services.cloud-init.requires = lib.mkForce [ "network.target" ];

  services.tailscale.enable = true;
  services.openssh.enable = true;

  services.cloud-init = {
    enable = true;
    ext4.enable = true;
  };

  networking.firewall = {
    checkReversePath = "loose";
    trustedInterfaces = [ "tailscale0" ];
    allowedUDPPorts = [ config.services.tailscale.port ];
  };
}
```

호스트명을 Territorial Rotbart의 무서운 힘을 통해 waifud가 할당한 것으로 교체합니다.

다음 설정이 활성화되어 있는지 확인합니다:

```nix
services.tailscale.enable = true;
services.openssh.enable = true;
```

SaaS 컨트롤 플레인과 함께 Funnel에 연결하려면 머신에서 Tailscale이 활성화되어 있어야 합니다. 또한 독자에게 연습으로 남겨진 이유로 머신에 연결할 수 있도록 SSH가 활성화되어 있어야 합니다.

파일을 저장하고 재빌드를 트리거합니다:

```sh
sudo nixos-rebuild switch
```

그런 다음 확실히 하기 위해 재부팅합니다:

```sh
sudo reboot
```

---

Mara의 대화:

"이 재부팅은 필수가 아니지만, 머신을 재부팅할 때 모든 것이 다시 올라온다는 것을 보여주는 것이 재미있습니다."

---

이제 NixOS 머신에 Funnel을 설정해 봅시다. 먼저, Tailscale로 인증합니다:

```sh
sudo tailscale up
```

이것은 인증 URL을 출력합니다. 포인팅 장치로 힘을 가해 클릭한 다음 좋아하는 브라우저(예: Luakit)에서 엽니다. Tailscale로 인증하라는 메시지가 표시됩니다. 인증하면 "성공! 이제 이 창을 닫을 수 있습니다"라고 적힌 페이지로 리디렉션됩니다. 이제 그 창을 닫을 수 있습니다.

그런 다음 https://login.tailscale.com/admin/machines 에서 Tailscale 관리 패널을 열 수 있으며 새 머신이 목록에 표시되어야 합니다. 액세스 제어 탭을 클릭한 다음 funnel ACL을 작성합니다.

이제 NixOS 머신에 Headscale을 설치할 수 있습니다. 먼저, NixOS 구성에 Headscale 모듈을 추가해야 합니다:

```nix
services.headscale = {
  enable = true;
  address = "0.0.0.0";
  port = 8080;
  serverUrl = "https://baelzeb-weedle.shark-harmonic.ts.net";
  dns.baseDomain = "ts.plex-each";
  settings.logtail.enabled = false;
};
```

---

Mara의 대화:

"`serverUrl`은 머신의 호스트명과 tailnet 도메인의 조합과 같아야 합니다. `shark-harmonic.ts.net` 부분이 tailnet 도메인입니다. `baelzeb-weedle` 부분은 NixOS 머신의 호스트명입니다."

---

이제 NixOS를 재빌드하고 포트 8080에서 Headscale이 실행되는 것을 확인합니다:

```
# nixos-rebuild switch
```

```
# netstat -tnlp
Active Internet connections (only servers)
Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program name
tcp        0      0 100.114.219.37:44479    0.0.0.0:*               LISTEN      867/tailscaled
tcp        0      0 0.0.0.0:22              0.0.0.0:*               LISTEN      1066/sshd: /nix/sto
tcp6       0      0 :::8080                 :::*                    LISTEN      4146/headscale
tcp6       0      0 fd7a:115c:a1e0:ab:44479 :::*                    LISTEN      867/tailscaled
tcp6       0      0 :::22                   :::*                    LISTEN      1066/sshd: /nix/sto
tcp6       0      0 :::37247                :::*                    LISTEN      4146/headscale
```

만세! 실행되고 있습니다! 이제 `tailscale serve` 명령을 사용하여 Funnel을 가리킬 수 있습니다:

```
tailscale serve tls-terminated-tcp:443 tcp://localhost:8080
```

---

Mara의 대화:

"네, 정말로 TLS-terminated TCP를 사용해야 합니다. 이 글을 쓰는 시점에서, Tailscale의 HTTP 리버스 프록시가 tailscaled가 컨트롤 플레인에 연결하는 데 사용하는 HTTP 롱 폴링(long-polling)과 협력하지 않는 것 같습니다. TLS-terminated TCP를 사용하면 이 우스꽝스러운 임시 구조물이 작동할 수 있도록 이 문제를 해결합니다."

---

그런 다음 노드에서 Funnel을 활성화합니다:

```
tailscale funnel 443 on
```

그런 다음 DNS 신들이 당신의 얼굴에 미소 짓기를 1-2분 기다린 후 좋아하는 브라우저(예: GNOME Web)에서 URL을 엽니다. 404 페이지가 반환되어야 합니다. 이것은 예상된 것입니다.

이제 노드를 위한 Headscale 네임스페이스를 생성해 봅시다:

```
headscale namespaces create casa
```

이제 waifud에서 Amazon Linux 2 인스턴스와 같은 다른 Linux VM을 생성합니다:

```
waifuctl create -m 1024 -c 2 -d amazon-linux-2 -s 25 -H ontos
```

---

Aoi의 대화:

"서점을 위해 만들어진 Linux 배포판을 사용하는 건가요??? 왜 그런 게 존재하는 거죠? 왜? 어떻게? 뭐라고요?"

---

Mara의 대화:

"이 터무니없는 모험에 뭘 또 쓰겠어요?"

---

그런 다음 연결하고 Tailscale을 설치합니다:

```
$ ssh xe@10.77.130.5
Last login: Fri Mar 31 14:48:06 2023 from 10.77.130.1

       __|  __|_  )
       _|  (     /   Amazon Linux 2 AMI
      ___|\___|___|

https://aws.amazon.com/amazon-linux-2/
[xe@geordi-coral-bits ~]$ curl -fsSL https://tailscale.com/install.sh | sh
```

다음으로, `--login-server` 플래그를 사용하여 Headscale 서버에 인증합니다:

```
[xe@geordi-coral-bits ~]$ sudo tailscale up --login-server https://baelzeb-weedle.shark-harmonic.ts.net
To authenticate, visit:

        https://baelzeb-weedle.shark-harmonic.ts.net/register/nodekey:67e57f6cf6b11be04f66a30b389672cd6355081c15b5c3eae2739eed9c6ce41a
```

그런 다음 NixOS 머신 창을 열고 노드를 인증합니다:

```
headscale --user casa nodes register --key nodekey:67e57f6cf6b11be04f66a30b389672cd6355081c15b5c3eae2739eed9c6ce41a
```

만세! 이제 네트워크의 모든 행복한 노드를 볼 수 있습니다:

```
[xe@geordi-coral-bits ~]$ tailscale status
100.64.0.1 geordi-coral-bits casa linux -
```

하나 더 추가해 봅시다, Ubuntu는 어떨까요:

```
waifuctl create -m 1024 -c 2 -d ubuntu-22.04 -s 25 -H kos-mos
```

그런 다음 연결하고 Tailscale을 설치합니다. 그런 다음 이전처럼 인증합니다.

머신들이 서로 ping할 수 있어야 합니다. 할 수 없다면, 그건 나쁜 겁니다. ping이 작동할 때까지 머신 중 하나 또는 둘 다를 재부팅해 보세요.

---

Aoi의 대화:

"저는 여전히 할 말을 잃었어요. 이것에 대해 뭐라고 말해야 할지 모르겠어요. Tailscale을 사용하지 않기 위해 Tailscale을 사용하고 있잖아요. 이 카드로 만든 탑이 무너지는 걸 기다릴 수가 없네요. 누구도 이걸 프로덕션에서 사용하지 않기를 신께 빕니다."

---

Mara의 대화:

"걱정 마세요, 그들은 사용할 거예요! 이것은 무언가를 문서화한 자연스러운 결과입니다. 누군가 이것을 사용할 것이고 그것이 망가질 때 제가 그들 근처에 없기를 바랍니다."

---

Aoi의 대화:

"휴가가 필요해요."

---

## 결론

이것들은 Funnel로 할 수 있는 많은 것들 중 일부입니다. 이것이 전혀 작동하는 것 이상으로 테스트하지 않았으므로 이것이 얼마나 안정적인지 모른다는 점을 참고하세요.

이 아이디어에 대해 Hacker News의 xeonmc에게 많은 감사를 드립니다. 이것은 당신의 잘못입니다. 당신이 책임자입니다. 행복하시길 바랍니다.

다음 분들께 사과드립니다:

- apenwarr님, 제 터무니없는 모험을 가능하게 해주셔서
- Claire님, 이 기사의 전제에 대해 완전히 어안이 벙벙한 반응으로 Aoi의 대사에 영감을 주셔서
- Deidra님, 이 전제의 초현실성에 추가로 영감을 주셔서
- iliana님, 서점이 자체 Linux 배포판을 갖게 해주셔서
- Kristoffer님, headscale 디버깅 과정에서 도와주셔서

이게 작동한다니 믿을 수가 없네요.

---

원문 링크: https://tailscale.com/blog/headscale-funnel
