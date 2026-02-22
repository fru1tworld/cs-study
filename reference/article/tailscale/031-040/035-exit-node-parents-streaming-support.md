# 부모님께 Tailscale 노드를 우편으로 보내야 하는 이유

작성자: Kevin Purdy
게시일: 2025년 11월 4일
읽는 시간: 12분

---

Tailscale은 다양한 플랫폼에 설치할 수 있습니다. 아마 생각해보지 못했을 플랫폼 하나가 있습니다: 바로 우편함입니다.

또는 현관 앞, 택배 보관함, 혹은 친구나 가족이 택배를 받는 어디든 말이죠. Tailscale은 Raspberry Pi나 Apple TV처럼 작은 상자에 들어가는 소형 기기에서도 실행할 수 있습니다. 일단 전원을 연결하면, 미리 Tailscale이 설치된 기기를 통해 고향집의 기기를 원격으로 문제 해결하고, VPN 용도의 출구 노드(exit node)를 제공하며, 오프사이트 백업 위치를 확보하는 등 다양한 멋진 기능을 활용할 수 있습니다.

일부 Tailscale 팬들은 원격 Tailscale 기기를 더욱 중요한 일에 활용해왔습니다. 치매 진단을 받은 가족을 돕고 모니터링하거나, 제한적인 국가 방화벽 안에 살고 있는 부모님과 연결하는 용도로 말이죠.

만약 부모님(또는 이모, 친구, 누구든)이 컴퓨터에 Tailscale을 설치하는 것에 능숙하다면, [기기 공유](https://tailscale.com/kb/1084/sharing) 또는 [사용자 초대](https://tailscale.com/kb/1371/invite-users) 같은 소프트웨어만으로도 해결할 수 있을 것입니다. 하지만 대부분의 사람들은 집에서 항상 켜져 있는 컴퓨터를 가지고 있지 않습니다. 그리고 Tailscale의 [서브넷 라우터(subnet router)](https://tailscale.com/kb/1019/subnets) 기능을 사용하여 라우터나 프린터 같은 원격 네트워크 기기에 접근하려면, 이 기능은 Linux, Apple TV, Android TV, 그리고 [오픈소스 Mac 클라이언트](https://tailscale.com/kb/1065/macos-variants)에서만 사용 가능합니다.

항상 켜져 있는 작은 Tailscale 박스를 연결하면 집에서 떨어진 곳에서도 LAN으로 즉시 안전하게 순간이동할 수 있습니다. 물론 네트워크 존재감의 순간이동이지, 연말연시에 사람 몸을 이동시키는 것은 아직 서비스하지 않습니다.

Tailscale 케어 패키지를 설정하고 세상에 보내봅시다.

## Tailscale 케어 패키지 박스 선택하기

저전력, 항상 켜진 Tailscale 기기를 보내기 위한 일반적인 하드웨어 옵션은 두 가지입니다:

* 지원되는 Linux 배포판을 실행하는 Raspberry Pi 4B나 5 같은 소형 컴퓨터, 또는 "씬 클라이언트(thin client)".
* Apple TV 또는 Android TV 박스.

차이점은 무엇일까요?

* 소형 Linux 컴퓨터는 다재다능하며, 원격 SSH 명령줄에서 거의 완전히 실행할 수 있지만, 더 많은 설정이 필요합니다.
* Apple TV는 작동시키기가 상당히 간단하고, 출구 노드와 서브넷 라우터를 제공할 수 있지만, SSH나 VNC로 원격 접속할 수 없습니다.
* Android TV는 그 중간 정도로, Apple TV보다 조금 더 복잡하고, 일부 원격 접속을 제공하지만, Linux 컴퓨터만큼 강력하지는 않습니다.

Tailscale 사용자들은 몇 달간의 원격 임무 후에도 완벽하게 작동하는 Apple TV를 설정했고, Android TV 출구 노드 제작자들도 장기적인 안정성을 보고했습니다. 다만 집에 방문할 때 가끔 그 TV 박스를 실제 TV나 모니터에 연결하여 시스템 업데이트를 적용하고 보안을 유지할 계획을 세우세요. (Linux 기기는 SSH 연결을 통해 업그레이드하거나, 자동 업데이트를 설정하거나, Tailscale 관리 콘솔의 Machines 페이지에서 한 번의 클릭으로 업데이트할 수 있습니다).

어떤 방법을 선택하든, 이더넷 케이블과 벽면(전원) 플러그와 함께 기기를 보내는 것을 권장합니다. Wi-Fi로 원격 노드를 실행할 수도 있지만, 더 많은 설정이 필요하고, 완전히 다른 종류의 잠재적 문제 해결이 필요해집니다.

### Raspberry Pi (또는 다른 Linux 시스템)

믿음직한 Raspberry Pi로 작업한다면, [Tailscale로 Raspberry Pi 설정하기 완전 YouTube 가이드](https://www.youtube.com/watch?v=Hu9xPMXJPJc)가 있습니다. 이 가이드는 시작에 필요한 모든 것을 Pi에 설정합니다: Tailscale 기반 SSH 접속, 출구 노드, 그리고 서브넷 라우터. 데모 앱과 Docker 설정 같은 다른 부분은 선택 사항입니다(개인 용도로는 꽤 멋지긴 하지만).

(편집, 2026년 1월 7일: Raspberry Pi에서 Tailscale 출구 노드를 설정하는 이 정확한 설정에 대한 매우 상세하고, 업데이트되고, 유용한 가이드가 [johnnyfivepi가 GitHub에 게시](https://github.com/johnnyfivepi/tailscale-exit-node)했습니다.)

Raspberry Pi가 아닌 Linux를 실행하는 컴퓨터를 사용한다면, 비슷한 과정입니다(그 기기에서 Linux를 실행하게 하는 작업을 더해서). 자신의 집에서 모니터, 키보드, 마우스가 연결된 상태로 기기를 가지고 있는 동안:

* Tailscale을 설치하고 Pi를 Tailscale 계정(tailnet)에 추가
* [Tailscale SSH 접속](https://tailscale.com/kb/1193/tailscale-ssh) 설정
* 기기를 출구 노드로 설정([Alex가 YouTube에서 설명](https://www.youtube.com/watch?v=Hu9xPMXJPJc&t=248s))하고 관리 패널에서 승인
* 기기를 [서브넷 라우터로 준비](https://tailscale.com/kb/1019/subnets)하고 [IP 포워딩이 활성화](https://tailscale.com/kb/1104/enable-ip-forwarding)되었는지 확인

멀리 떨어진 네트워크의 다른 기기에 접근할 수 있게 해주는 서브넷 라우터 기능은 기기가 목적지에 연결되고 접근 가능해지면 완전히 설정할 수 있습니다. 연결되면, 부여받은 IP 주소를 확인하고(Tailscale 관리 콘솔에서 볼 수 있음), 관리 콘솔에서 경로를 광고하고 서브넷을 활성화합니다.

### Apple TV

Tailscale로 Apple TV를 설정하는 것은 몇 번의 클릭이면 됩니다(수십 개의 Apple 권한과 EULA에 동의한 후에):

* App Store에서 Tailscale 다운로드
* Tailscale 앱을 설치하고 VPN 추가를 포함하여 필요한 권한 부여
* Apple TV를 tailnet에 승인
* Tailscale Apple TV 앱에서 Exit Node 섹션을 선택하고 Run as Exit Node를 선택
* Apple TV 앱의 Subnet router 섹션에서 Advertise New Route를 선택한 다음 (원격으로 연결된 후) 관리 콘솔에서 설정
* Tailscale 연결이 끊어지지 않도록 잠들지 않게 [홈 허브로 설정](https://support.apple.com/en-us/102557)되어 있는지 확인

원격 Apple TV 노드에 대한 중요한 참고 사항: 원격 네트워크 속도 테스트를 실행하지 마세요. Ookla 속도 테스트 중처럼 Apple TV의 리소스를 너무 많이 사용하면 Tailscale이 지속적인 VPN 연결에 의존하는 네트워크 확장을 종료시킬 수 있습니다. 정해진 한계는 없습니다; Apple TV 세대마다 하드웨어가 다릅니다. 대부분의 사용자는 일반 연결이나 스트리밍 중에는 이 문제를 겪지 않겠지만, 주의할 가치가 있습니다.

### Android TV

원격 Tailscale 노드로 Android TV 기기를 설정하는 단계는 기본적으로 [모든 Android 기기를 설정](https://tailscale.com/kb/1347/tailscale-on-android)하는 것과 동일합니다:

* Play Store나 [Tailscale의 APK 빌드](https://pkgs.tailscale.com/stable/#apks)에서 Tailscale 설치
* 기기를 tailnet에 승인
* 기기를 출구 노드로 활성화
* 기기를 [서브넷 라우터로 준비](https://tailscale.com/kb/1019/subnets)

Android 기기에 Tailscale이 설정되면, Settings > Network & internet > Others로 이동하여, Tailscale의 톱니바퀴 아이콘을 클릭하고, Always-on VPN을 활성화합니다. 이렇게 하면 기기가 부팅될 때 Tailscale이 시작되어 출구 노드나 서브넷 라우터로 기기에 접근할 수 있습니다.

Apple TV와 마찬가지로, Android TV 기기에 원격 SSH 접속은 할 수 없습니다(탈옥과 개발자 모드를 만지작거리지 않는 한—그렇게 했다면 어떻게 했는지 알려주세요!). 재부팅 후에도 지속되는 VNC 접속은 *가능할 것*입니다.

저는 [droidVNC-NG](https://github.com/bk138/droidVNC-NG) 서버를 설정하고, 필요한 권한을 부여하고, Start on Boot를 켜는 데 성공했습니다. 몇 가지 VNC 클라이언트를 시도한 후, Android의 Tailscale IP 주소를 통해 새로 재부팅한 후 droidVNC-NG에 (TigerVNC로) 연결할 수 있었습니다. Tailscale 친화적인 원격 제어 도구인 [RustDesk](https://rustdesk.com/)는 원격 연결을 허용하기 전에 화면 접근 권한이 있는 누군가가 전체 화면 접근에 대한 프롬프트를 승인해야 합니다.

## 보내기 전에 노드-인-어-박스 테스트하기

기기에 Tailscale이 출구 노드 및/또는 서브넷 라우터로 설정되었으면, 예상대로 작동하는지 테스트해야 합니다. 대부분의 사람들에게 가장 쉬운 방법은 휴대폰의 셀룰러 연결(또는 그 휴대폰에 테더링된 컴퓨터)을 사용하는 것입니다:

* 보내려는 박스를 홈 네트워크에 연결합니다.
* 모바일 기기에서 Wi-Fi를 끄고, 셀룰러 신호로 연결되게 합니다.
* iOS 또는 Android 앱을 통해 Tailscale에 연결한 다음, 원격 기기가 출구 노드로 제공되는지 확인합니다.
* 기기를 출구 노드로 활성화한 다음, 휴대폰에서 몇 개의 웹사이트를 탐색하여 접근할 수 있는지 확인합니다.
* 서브넷 라우터를 설정하는 경우, 휴대폰을 통해 네트워크에서 Tailscale을 실행하지 않는 기기—라우터, 프린터, 또는 원격 접속이 활성화된 컴퓨터—에 접근해 봅니다.
* Raspberry Pi 및 Linux 기기의 경우, 다른 기기에서 Tailscale 관리 콘솔에 접속하여 원격 기기 행에서 SSH 버튼을 클릭하여, 터미널 프롬프트가 있는 브라우저 창이 열리고 로그인할 수 있는지 확인합니다.

이더넷으로 원격 기기를 라우터에 연결할 수 없다면, 원격 Wi-Fi 자격 증명으로 기기를 설정해야 합니다. SSH 및 VNC 접속을 활성화하는 Raspberry Pi Imager의 같은 단계에서—기기의 원래 영국식 철자로 "OS customisation settings"—Wi-Fi SSID(네트워크 이름)와 비밀번호를 추가할 수 있습니다.

## 노드 보내기 및 접속하기

의도한 목적지로 기기를 보냅니다. 프로 팁: 대부분의 케이스에 든 Raspberry Pi, Apple TV 유닛, Android TV 기기는 USPS Priority Mail Flat Rate Small Box에 들어갑니다. 당신의 너드함을 수용해준 것에 감사하기 위해 간식 몇 개를 넣을 공간도 남을 수 있습니다. 또는 방문할 때 가져갈 수도 있는데, 관계자들은 더 자주 방문해야 한다고 말합니다.

도착 후, 친구나 가족에게 연결하는 방법을 안내합니다. 대부분의 가정용 인터넷 제공업체가 제공하는 라우터에는 숫자로 표시된 몇 개의 포트(1-4처럼)가 있습니다; 기기를 연결하기 위해 케이블/광섬유 모뎀이나 다른 연결을 뽑지 않도록 확인하세요. Wi-Fi에 의존한다면, Wi-Fi 라우터에 합리적으로 가까운 곳에 꽂아야 합니다. 터미널이나 원격 데스크톱에서 Raspberry Pi의 신호 강도를 확인할 수 있습니다. Apple이나 Android TV 유닛을 보냈다면, 몇 군데 시도하고 테스트해야 할 수 있습니다.

올바르게 설정되었다면, Tailscale 관리 콘솔에서 기기가 온라인(녹색 점으로 표시)인 것을 볼 수 있어야 합니다. 서브넷 라우터를 통해 원격 네트워크의 기기에 접근하거나, 출구 노드로 선택하고 [WhatIsMyIP](https://www.whatismyip.com/) 같은 IP 확인 사이트를 방문할 때 해당 위치를 볼 수 있어야 합니다.

## 여기서 어디로 갈까 (그리고 훌륭한 사용 사례)

기기가 그들의 로컬 네트워크에서 실행되면, 그들의 네트워크에 뛰어들어 네트워크 문제를 진단하고, 원격 데스크톱 지원을 제공하고, 출구 노드로 그들의 위치에서 연결과 서비스를 테스트할 수 있습니다.

[Uptime Kuma](https://github.com/louislam/uptime-kuma) 같은 업타임 모니터링 도구를 사용하여 출구 노드의 IP 주소를 모니터링하고 집에서 모든 것이 괜찮은지 확인할 수 있습니다. LAN 연결에서 더 잘 작동하는 협동 게임을 조카들과 할 수 있습니다.

이 기기를 사용하여 친구나 가족이 Tailscale을 시도하도록 유도할 수도 있습니다. 이미 집을 떠나 있을 때를 위한 편리한 VPN을 제공했으니; 기기에 공유 USB 드라이브를 연결하면 원격 파일 공유를 설정할 수 있습니다. 이기적으로, 하드웨어나 기타 재해에 대비하여 중요한 파일이나 문서를 원격으로(그리고 아마도 암호화하여) 보관할 수 있습니다.

Tailscale 열성 팬들은 이 기기들이 꽤 유용하다는 것을 발견했습니다. [Tailscale 서브레딧](https://www.reddit.com/r/Tailscale/)에서 사용자 caolle는 간병에 Tailscale을 사용한 이야기를 공유했습니다:

> 어머니가 치매를 앓고 계시고 요양원에 계십니다. 언니가 요양원과 어머니의 재정, 그리고 쌓인 서류 처리의 대부분을 담당하고 있습니다.
>
> 저는 두 개 주를 가로질러 몇 시간 떨어져 있습니다. 제가 도울 수 있는 가장 좋은 방법은 기술적 전문 지식을 사용하여 경험을 덜 스트레스 받게 하고 더 쉽게 처리할 수 있게 만드는 것입니다.
>
> (Raspberry Pi 5)와 Radxa Penta SATA HAT으로 nas-pi를 만들었습니다. Paperless-ngx와 몇 가지 다른 것들을 호스팅할 것입니다. Tailscale은 제 위치와 언니 위치 사이를 연결하는 접착제가 되어 양쪽에서 백업을 가능하게 할 것입니다. Taildrive를 사용하여 모든 것을 용이하게 하려고 합니다. 하지만 두고 봐야죠.

사용자 zulcom은 또 다른 흔치 않지만 중요한 사용 사례를 공유했습니다:

> 제 친척들은 최근 WhatsApp까지 차단한 나라에 살고 있어서, 다른 나라에 있는 제 홈 서버에 자체 호스팅 출구 노드를 설정했습니다. 그래서 Tailscale은 우리의 채팅과 화상 통화를 유지할 뿐만 아니라, 요즘 갖추면 매우 감사한 국가 독립 미디어에 대한 접근도 제공합니다.

---

Tailscale로 구동되는 집 기기를 설정해보셨나요? 무엇을 사용하고 있는지, 어떻게 설정했는지 [Reddit](https://www.reddit.com/r/Tailscale/), [Discord](https://discord.com/invite/tailscale), [Bluesky](https://bsky.app/profile/tailscale.com), [Mastodon](https://hachyderm.io/@tailscale), 또는 [LinkedIn](https://www.linkedin.com/company/tailscale/)에서 공유해주세요.
