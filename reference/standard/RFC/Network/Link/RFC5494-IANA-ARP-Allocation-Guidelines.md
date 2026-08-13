# RFC 5494 - 주소 해석 프로토콜(ARP)에 대한 IANA 할당 지침

```
Network Working Group                                           J. Arkko
Request for Comments: 5494                                      Ericsson
Updates: 826, 951, 1044, 1329, 2131,                        C. Pignataro
         2132, 2176, 2225, 2834, 2835,                     Cisco Systems
         3315, 4338, 4361, 4701                               April 2009
Category: Standards Track
```

### Abstract

- 주소 해석 프로토콜(ARP)의 새로운 값 할당을 위한 IANA 지침을 규정
- 실험 목적으로 일부 번호를 예약
- 이 변경 사항은 ARP 이름 공간의 값을 사용하는 다른 프로토콜에도 영향
- 이 RFC는 RFC 826·951·1044·1329·2131·2132·2176·2225·2834·2835·3315·4338·4361·4701을 갱신(Updates)

## 1. 서론

- ARP [RFC0826]의 다양한 필드에 새로운 값을 할당하기 위한 IANA 지침 [RFC5226]을 규정
- 같은 메시지 형식을 사용하는 ARP의 확장에도 적용 → [RFC0903]·[RFC1931]·[RFC2390] 등
- ARP 이름 공간의 값을 사용하는 다른 프로토콜에도 영향
  - ARP 하드웨어 주소 타입(`ar$hrd`) 번호 공간은 Bootstrap Protocol(BOOTP) [RFC0951]과 Dynamic Host Configuration Protocol(DHCP) [RFC2131]의 "htype"(하드웨어 주소 타입) 필드에서도 사용
  - DHCPv6 [RFC3315]의 DHCP Unique Identifier "hardware type" 필드에서도 사용
  - → 이 프로토콜들도 IANA 규칙 갱신의 영향을 받음
- 영향을 받는 다른 명세: 특수 주소 해석 메커니즘 포함
  - HYPERchannel [RFC1044]
  - DHCP 옵션 [RFC2132] [RFC4361]
  - ATM(Asynchronous Transfer Mode) ARP [RFC2225]
  - HARP(High-Performance Parallel Interface ARP) [RFC2834] [RFC2835]
  - Dual MAC(Media Access Control) FDDI(Fiber Distributed Data Interface) ARP [RFC1329]
  - MAPOS(Multiple Access Protocol over Synchronous Optical Network/Synchronous Digital Hierarchy) ARP [RFC2176]
  - FC(Fibre Channel) ARP [RFC4338]
  - DNS DHCID Resource Record [RFC4701]
- IANA 지침은 2절에 제시 → 이전에는 이러한 할당에 대한 IANA 안내가 존재하지 않았음
  - 목적: IANA가 이 지침에 따라 번호 할당을 일관되게 관리
- 실험 목적으로 일부 번호를 예약 → 3절에 제시

## 2. IANA 고려사항

다음 규칙이 ARP의 필드에 적용됨.

- `ar$hrd` (16비트) 하드웨어 주소 공간
  - 256 미만의 `ar$hrd` 값이나 하나 이상의 새 값을 일괄 요청하는 경우 → 전문가 검토(Expert Review) [RFC5226]를 통해 이루어짐
  - BOOTP나 DHCPv4 같은 특정 프로토콜은 8비트 필드 내에서 이 값을 사용
    - 전문가는 새 값 할당의 필요성 존재 여부, 기존 값이 새 하드웨어 주소 타입을 표현하기에 불충분한지 여부를 판단해야 함
    - 전문가는 요청의 적용 가능성을 판단하고, BOOTP/DHCPv4에 적용되지 않는 요청에는 255보다 큰 값을 할당해야 함
    - 전문가는 BOOTP/DHCPv4에 적용되는 요청에는 1옥텟 값을 할당해야 함 → 예: 값 31의 "IPsec tunnel" [RFC3456]
    - BOOTP/DHCPv4에서 같은 값을 사용할 예상되는 이유가 없는 ARP 전용 사용은 2옥텟 값을 선호해야 함
  - 값을 지정하지 않거나 요청된 값이 255보다 큰 개별 `ar$hrd` 값 요청 → 선착순(First Come First Served) [RFC5226]으로 이루어짐
  - 할당은 항상 2옥텟 값으로 이루어짐
- `ar$pro` (16비트) 프로토콜 주소 공간
  - 이 번호들은 Ethertype 공간을 공유
  - Ethertype 공간은 [RFC5342]에 기술된 대로 관리
- `ar$op` (16비트) Opcode
  - 새로운 `ar$op` 값 요청은 IETF 검토 또는 IESG 승인 [RFC5226]을 통해 이루어짐

## 3. 이 문서에서 정의된 할당

- 새로운 프로토콜 확장 아이디어를 테스트할 때, 폐쇄된 환경에서 테스트하더라도 새 기능을 사용하기 위해 실제 상수를 사용해야 하는 경우가 많음
- 이 문서는 ARP에서 실험 목적으로 다음 번호를 예약
  - 실험 목적으로 두 개의 새로운 `ar$hrd` 값을 할당: HW_EXP1 (36)과 HW_EXP2 (256)
    - 하나는 256 미만, 다른 하나는 255 초과
    - 최하위 옥텟과 최상위 옥텟에서 서로 다른 값을 갖도록 의도적으로 선택됨
  - 실험 목적으로 `ar$op`의 두 개의 새로운 값을 할당: OP_EXP1 (24)과 OP_EXP2 (25)
- [RFC5342]의 부록 B.2는 실험 목적으로 사용할 수 있는 두 개의 Ethertype을 나열
- `ar$hrd`와 `ar$op` 모두에서 값 0과 65535는 예약됨으로 표시 → 할당에 사용할 수 없음을 뜻함

## 4. 보안 고려사항

- 이 명세는 영향을 받는 프로토콜의 보안 속성을 변경하지 않음
- 3절에 정의된 실험 코드 포인트의 사용에 대해 언급 필요
  - 실험 값 사용으로 인한 해로울 수 있는 부작용 → 실험의 소유자가 전적으로 제어하지 않는 네트워크에 실험을 배포하기 전에 주의 깊게 평가해야 함
  - 실험 값 사용에 대한 [RFC3692]의 지침을 따라야 함

## 부록 A. 원본 RFC로부터의 변경사항

- 이 문서는 ARP의 다양한 필드와 관련된 IANA 규칙만을 규정
- 이 규칙의 규정은 레지스트리를 공유하는, 1절에 나열된 프로토콜의 해당 필드 할당에도 영향
- 이 문서는 이 프로토콜들 자체의 동작에 어떤 변경도 가하지 않음
