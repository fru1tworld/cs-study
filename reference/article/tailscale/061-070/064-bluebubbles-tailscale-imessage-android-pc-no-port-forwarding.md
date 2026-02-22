# Tailscale + BlueBubbles: Windows, Android 또는 어디서든 iMessage 사용하기

게시일: 2026년 1월 22일
저자: Kevin Purdy

---

## 소개

Apple 경영진이 iMessage가 사용자 유지(lock-in) 도구로서 얼마나 효과적인지 잘 알고 있다는 것은 널리 알려진 사실입니다. "초록 말풍선"에 대한 이상한 문화적 낙인, 편리한 멀티 디바이스 접근, 종단간 암호화(end-to-end encryption)—이것들을 이기기 어렵고, 우회하기는 더욱 어렵습니다. Android, Windows, 또는 Linux에서는 iMessage를 사용할 수 없습니다—Mac에서 BlueBubbles를 실행할 준비가 되어 있지 않다면 말이죠.

BlueBubbles는 기본적으로 모든 주요 컴퓨팅 장치에서 iMessage 채팅(그리고 iPhone 사용자라면 SMS 문자)을 읽고, 보내고, 알림을 받을 수 있게 해주는 오픈소스 Mac 서버 앱입니다. BlueBubbles는 제 책상 아래에 Mac Mini를 볼트로 고정해 둔 주된 이유입니다. 저는 BlueBubbles 클라이언트를 사용해 Linux 노트북, Chromebook, Windows 게이밍 데스크톱, 그리고 웹앱을 통해 메시지를 주고받았습니다. BlueBubbles는 읽음/안읽음 상태, "탭백(tapbacks)"과 이모지 반응, 그룹 채팅 기능, 메시지 편집을 포함하여 거의 완전한 iMessage 경험을 제공합니다. 그리고 이름이 암시하듯이, 모든 말풍선이 "파란색"입니다.

![Android 폰에서 BlueBubbles를 통한 대화, Tony Tran과의 대화: "내일 봐", "우리 옆에 새벽 1시까지 하는 웬디스에서 역대 최대 고객이 될 거야", "BlueBubbles Android 스크린샷 텍스트."]

캡션: 저자가 Android 폰에서 iPhone을 사용하는 동료와 다시 연락하는 모습.

BlueBubbles를 설정하려면 쉽게 연결할 수 있는 Mac이 필요하고, 알려진 IP 주소에서 접근 가능한 포트가 있어야 합니다. 이것은 개인 메시지를 보안해야 한다는 것을 고려하기 전에도 다소 부담스럽습니다. Tailscale에서 우리는 이에 대한 훌륭한 솔루션을 제공한다고 생각합니다. 그리고 중요한 것은? BlueBubbles를 포함한 다른 사람들도 동의하는 경향이 있습니다.

BlueBubbles의 주요 개발자 중 한 명인 Zach는 이메일 교환에서 "설정할 기술적 노하우가 있다면 항상 Tailscale 사용을 권장합니다(설정하지 않기에는 거의 너무 쉬운데도 말이죠)"라고 썼습니다.

---

## Tailscale이 BlueBubbles 설정에서 대체하는 것들

BlueBubbles는 점심시간에 설정할 수 있는 것이 아닙니다(이것은 도전이 아닙니다). 초기 설정은 상당한 작업입니다—광범위한 시스템 권한 부여, 시스템 무결성 보호(System Integrity Protection) 비활성화, 알림 방식 선택—연결 부분에 도달하기 전에 말이죠. 다행히 연결은 Tailscale이 빛나는 부분입니다.

Tailscale이 BlueBubbles 경험에 가져오는 업그레이드는 다음과 같습니다:

- 동적 DNS(Dynamic DNS) 설정 대신 안정적인 IP 주소 또는 URL 제공
- 라우터에서 열린 포트나 포트 포워딩(port-forwarding) 불필요
- 인증서, 터널, 프록시 설정 대신 완전히 암호화된 트래픽과 선택적 HTTPS 연결
- 머신 공유(machine sharing)로 동일한 Mac에서 두 번째 BlueBubbles 사용자 설정이 더 쉬워짐

---

## Tailscale과 BlueBubbles를 사용하는 세 가지 방법

BlueBubbles 자체의 Tailscale 사용 가이드는 Tailscale의 Funnel 사용을 제안합니다. 이것은 암호화 인증서(HTTPS)가 있는 공개적으로 접근 가능한 URL을 생성합니다. 작동하지만, 가장 보안이 낮은 경로입니다. Tailscale을 통해 BlueBubbles를 몇 가지 방법으로 실행할 수 있으며, 저는 이를 "바닐라(Vanilla)", "서브(Serve)", "퍼널(Funnel)" 설정이라고 부르겠습니다:

- 바닐라(Vanilla): Tailscale을 실행하는 데스크톱이나 노트북에서 BlueBubbles를 사용하기에 가장 적합
- 서브(Serve): 바닐라와 비슷하지만 HTTPS URL이 있어 웹앱 접근에 유용
- 퍼널(Funnel): 모든 브라우저나 BlueBubbles를 실행하는 장치에서 웹앱 접근이 필요하지만 Tailscale은 실행하지 않는 경우

설정 간에 확고한 선택을 할 필요는 없습니다. BlueBubbles-via-Tailscale의 세 가지 "버전" 모두 대략 한 번의 클릭과 하나의 터미널 명령어 차이이며, 쉽게 롤백할 수 있습니다. 시작해 봅시다.

---

## Tailscale로 BlueBubbles 설정하기

이것은 완전한 BlueBubbles 설정 가이드가 아닙니다. 표준 지침에서 파생된 BlueBubbles 설정의 요점과 개발자 Zach의 참고 사항, 그리고 Tailscale을 사용하여 어디서든 안정적으로 접근할 수 있게 만드는 가이드입니다.

Zach 참고 사항의 요점은 "알림 및 Firebase" 화면이 보이는 것보다 훨씬 선택적이라는 것입니다. Android와 Chromebook에서 새 메시지 알림을 받으려면 대안으로 ntfy를 설정하거나, Android에서 BlueBubbles 앱을 포그라운드 서비스로 계속 실행할 수 있습니다(Windows와 Linux의 데스크톱 클라이언트는 활성 백그라운드 연결을 유지하므로 별도의 알림이 필요하지 않습니다).

Firebase 클라우드 서버가 하는 또 다른 일은 BlueBubbles 클라이언트에 URL 변경 사항을 보내는 것입니다; Tailscale의 전용 IP 주소와 MagicDNS 이름은 이것을 불필요하게 만듭니다.

살펴봅시다.

### 설치 단계:

1. iMessage를 제공하기 위해 계속 실행할 Mac에 BlueBubbles 서버의 최신 DMG를 다운로드하고 설치합니다
2. 설치를 진행하면서 BlueBubbles가 요청하는 권한을 부여합니다
3. Google Firebase 생성 단계를 설정하거나 건너뜁니다(위 참조)
4. 아직 하지 않았다면, 메시지 제공 Mac에 Tailscale을 설치합니다. 더 쉬운 명령줄 접근을 위해 스탠드얼론(standalone) 버전을 권장합니다
5. Mac 터미널에서 `alias` 명령어를 한 번 실행하여 Tailscale의 명령줄 인터페이스(CLI) 접근을 훨씬 쉽게 만듭니다(문서에 자세히 설명되어 있음)
6. iMessage 접근이 필요한 장치에 BlueBubbles 클라이언트(필요한 경우 ntfy도)를 설치합니다

![BlueBubbles 설정 화면, "Dynamic DNS / Custom URL"이 활성화되어 있고, Tailscale IP 주소와 BlueBubbles 포트(http://100.124.xx.yz:1234)가 입력되어 있음.]

"바닐라" 경로(모든 장치가 Tailscale 사용)를 선택하는 경우 다음 단계는 간단합니다.

1. 테일넷(tailnet)에 연결된 모든 장치에서 `tailscale status`를 실행하거나 웹 관리 콘솔에서 확인하여 이 Mac의 Tailscale IP 주소(`100.xxx.yyy.zzz`)를 얻습니다
2. 사용하는 모든 BlueBubbles 클라이언트에서, 설정 중 또는 설정에서, (http) Tailscale IP 주소와 포트 1234를 다음과 같이 입력합니다: `http://100.xxx.yyy.zzz:1234`

"서브" 또는 "퍼널" 설정을 더 깊이 파기 전에, 클라이언트 장치에서 BlueBubbles 서버에 연결할 수 있는지 확인하는 것을 권장합니다.

Tailscale에 연결된 클라이언트에 웹앱 접근을 부여하는 "서브" 버전의 경우, 몇 가지 추가 단계가 있습니다.

1. Tailscale DNS 설정에서 HTTPS 인증서를 활성화합니다
2. BlueBubbles를 실행하는 Mac에서 명령줄을 열고 실행합니다: `tailscale serve --bg --https=443 1234` (포트 443이 기본값이지만, 8080, 10000 또는 다른 포트도 작동할 수 있습니다)
3. 반환되는 URL을 기록합니다, 예: `https://mac.tailnet-name.ts.net:443`
4. 해당 전체 URL(https와 포트 포함)을 사용하여 Mac의 BlueBubbles 서버(설정 > 프록시 설정 > Dynamic DNS / Custom URL 아래)와 BlueBubbles 클라이언트를 설정합니다

모든 컴퓨터나 웹앱이 비밀번호로(강력하고 다른 곳에서 사용하지 않은 것을 사용하세요) BlueBubbles 서버에 접근할 수 있게 하는 "퍼널" 설정은 상당히 유사합니다:

1. 테일넷에서 HTTPS 인증서, MagicDNS, Funnel이 활성화되어 있는지 확인합니다
2. BlueBubbles를 실행하는 Mac에서 실행합니다: `tailscale funnel --bg --https=443 1234`, 필요한 경우 443 포트를 대체합니다
3. 제공되는 URL(`https://mac.tailnet-name.ts.net:443`)을 기록한 다음 BlueBubbles 서버 설정과 클라이언트 모두에서 사용합니다

---

## BlueBubbles 서버 운영에 대한 몇 가지 팁

이제 iMessage가 가득한 Mac에 도달할 수 있게 만들었으니, Mac을 항상 실행하고 쉽게 접근할 수 있도록 유지하고 싶을 것입니다.

키보드, 마우스, 모니터가 연결된 데스크톱으로 이미 사용 중이라면, 절전 모드로 들어가지 않도록 설정하세요: 시스템 설정 > 에너지/배터리 > 옵션(오른쪽 하단의 작은 버튼)으로 이동하여 "디스플레이가 꺼져 있을 때 자동 절전 모드 방지"를 선택합니다. 또한 해당 섹션에서 전원 장애 후 자동 시작을 찾아 가능하면 활성화하세요.

Mac(특히 Mini)을 모니터 없이 "헤드리스(headless)"로 운영하려는 경우, 몇 가지 추가 단계를 권장합니다:

- BlueBubbles 설정의 기능 아래에서 자동 시작 방법을 찾아 Launch Agent (충돌 지속형)을 선택합니다
- Mac의 시스템 설정에서 "원격 관리"를 검색하고 원격 관리(원격 화면 제어용)와 원격 로그인(SSH용)을 활성화합니다
- FileVault(디스크 암호화)를 끄는 것이 괜찮다면, 비활성화하고 자동 로그인을 활성화할 수 있습니다(시스템 설정 > 사용자 및 그룹 > 자동으로 다음으로 로그인...)
- 원격 화면 접근을 위해 RustDesk/Tailscale 조합 사용을 고려하세요
- 집에서 서브넷 라우터(subnet router) 역할을 하는 다른 Tailscale 장치가 있으면, 정전 후에도 명령줄을 통해 Mac을 잠금 해제할 수 있습니다

![Pop!_OS(Linux) 데스크톱에서 찍은 스크린샷, 왼쪽에 앱 트레이가 있고 오른쪽에 창이 있으며, "메시지" 열과 사워도우 스타터 문제를 논의하는 iMessage 스레드가 표시됨.]

캡션: BlueBubbles가 가장 네이티브하지 않은 환경 중 하나인 Pop!_OS 데스크톱에서 iMessage를 처리하는 모습.

---

## 암호화된 iMessage, 원하는 어디서든

모든 장치에 iMessage를 개방하는 것은 분명 모든 사람을 위한 것은 아닙니다. 하지만 이 작업을 수행하려는 사람들에게 Tailscale 사용은 몇 가지 복잡한 레이어를 제거해야 합니다. 인기 있는 암호화된 채팅 시스템을 가져와 메시지를 암호화된 상태로 유지하고, 유용한 Mac 서버를 보호하며, 가지고 있는 무엇이든 연락을 유지할 수 있게 합니다.

이 프로젝트가 어떻게 작동했는지 알려주시거나, Reddit, Discord, Bluesky, Mastodon, 또는 LinkedIn에서 자신만의 잠금 해제 Tailscale 프로젝트를 공유해 주세요.

---

## 각주

[1]: BlueBubbles 웹앱에 대해: 이 글을 쓰는 시점에서 작동하지만, BlueBubbles 개발자인 Zach는 "한동안" 업데이트를 받지 못했으며 "본질적으로 지원되지 않음"이라고 언급합니다. 언젠가 전면 개편될 예정이지만 아직 일정이 정해지지 않았습니다. 따라서 필요하다면 있지만, Zach는 대부분의 사람과 장치(Chromebook 포함)가 전용 앱을 고수해야 한다고 제안합니다.
