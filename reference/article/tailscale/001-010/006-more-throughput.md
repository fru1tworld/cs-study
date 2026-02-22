# Tailscale로 10Gb/s 돌파: Linux에서의 성능 향상

- 원문: [Surpassing 10Gb/s with Tailscale](https://tailscale.com/blog/more-throughput)
- 작성일: 2023년 4월 13일
- 작성자: Jordan Whited

---

Tailscale은 VPN 서비스에 대한 중요한 성능 개선을 발표했으며, 베어 메탈 Linux 시스템에서 초당 10기가비트를 초과하는 처리량을 가능하게 했습니다. 이러한 개선은 Tailscale의 데이터 플레인을 구동하는 유저스페이스 WireGuard 구현인 wireguard-go에 대한 최적화에서 비롯되었습니다.

## 배경

Tailscale 아키텍처는 wireguard-go를 통해 패킷을 라우팅하며, TUN 인터페이스를 통해 데이터를 수신하고, 페이로드를 암호화하고, UDP 소켓을 통해 전송합니다. 버전 1.36의 이전 개선 사항은 개별 패킷을 처리하는 대신 패킷 배칭을 가능하게 하여 시스템 호출 오버헤드를 줄였습니다.

## 기술적 최적화

최신 개선 사항은 세 가지 핵심 기술을 구현합니다:

UDP Generic Segmentation Offload (GSO): Linux v4.18에서 도입된 이 커널 기능은 네트워킹 스택 순회 비용을 줄이기 위해 UDP 데이터그램 분할을 지연시킵니다. 구현은 전송을 위해 여러 데이터그램을 배치하기 위해 소켓 옵션을 활용합니다.

UDP Generic Receive Offload (GRO): Linux v5.0에서 추가된 이 기능은 처리를 위해 여러 UDP 패킷을 더 큰 단위로 합칩니다. 이는 GSO가 전송 측에서 제공하는 이점을 미러링합니다.

체크섬 최적화: 팀은 인터넷 체크섬 계산 함수에서 루프를 풀어 런타임을 약 57% 줄였습니다. 이 최적화는 각 합산 후 반복하는 대신 128바이트 청크로 데이터를 처리하며, RFC 1071에 설명된 원칙을 따릅니다.

## 성능 결과

AWS c6i.8xlarge 인스턴스에서의 테스트는 5.34 Gbits/sec에서 7.32 Gbits/sec로 처리량 개선을 보여주었습니다. 25Gb/s NIC를 갖춘 베어 메탈 i5-12400 시스템에서 성능은 8.36 Gbits/sec에서 13.0 Gbits/sec로 개선되어 해당 하드웨어에서 인커널 WireGuard 성능을 능가했습니다.

하드웨어 기반 UDP 세그멘테이션 오프로드(tx-udp-segmentation)는 구형 프로세서에서 추가적인 이점을 제공하여, 활성화 시 E3-1230-V2 시스템에서 35%의 처리량 증가를 가져왔습니다.

## 가용성

이러한 개선 사항은 Tailscale의 불안정 클라이언트 릴리스에서 사용할 수 있으며 버전 1.40에 예정되어 있습니다. 변경 사항은 이전 성능 개선 패턴에 따라 WireGuard 프로젝트에 업스트림 기여하기 위한 것입니다.
