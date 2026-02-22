# Tailscale 로그를 Google Cloud Storage로 스트리밍

- 원문: [Stream Tailscale logs to Google Cloud Storage](https://tailscale.com/blog/gcs-log-streaming)
- 작성일: 2026년 2월 18일
- 작성자: Jillian Murphy
- 기여자: Samuel Jinadu, Brad Kouchi, Eric Weinstein, Clare Adrien, Joseph Tsai, Jim Scott, Danni Popova

---

Tailscale 로그 스트리밍을 사용하면 테일넷의 로그를 보존, 조사 및 규정 준수를 위해 이미 사용 중인 시스템으로 내보낼 수 있습니다. 이 서비스는 이전에 SIEM 플랫폼과 S3 호환 스토리지를 포함한 외부 대상을 지원했습니다. 이제 Google Cloud Storage(GCS) 지원이 추가되었습니다.

주로 Google Cloud에서 운영하는 조직의 경우, 이는 기존 스토리지 및 접근 제어를 사용하여 Tailscale 로그를 운영 및 보안 텔레메트리와 함께 보관하는 직접적인 방법을 제공합니다.

## 암호화를 깨지 않는 네트워크 가시성

Tailscale의 피어 투 피어 아키텍처를 기반으로 구축된 네트워크를 운영하면 속도와 암호화를 얻습니다. 그러나 더 이상 중앙 게이트웨이를 통해 유입되지 않는 트래픽에 대한 질문이 생깁니다: 무엇이 무엇에 연결되었나? 언제 발생했나? 테일넷을 통해 접근이 어떻게 흘러갔나?

레거시 네트워크에서 감독은 병목 현상의 결과였습니다; 트래픽은 느린 "초크 포인트"를 통해 강제로 통과시켜 모니터링되었습니다. Tailscale은 트래픽이 가장 짧은 경로를 따르도록 이러한 장애물을 제거합니다. 암호화를 깨는 무거운 프록시를 다시 도입하지 않으면서도 네트워크 활동에 대한 신뢰할 수 있는 기록을 얻으면서 성능을 유지합니다.

네트워크 플로우 및 구성 감사 로그는 Tailscale의 조정 레이어를 활용하여 감독을 복원합니다. 모든 연결이 인증되므로, 이벤트는 일시적인 IP 주소가 아닌 특정 사용자 또는 디바이스 ID와 연결됩니다. 조직은 실제 트래픽 내용에 접근하지 않고도 네트워크를 이해하고 인시던트에 대응하는 데 필요한 컨텍스트를 얻습니다.

GCS로의 로그 스트리밍은 두 가지 범주의 테일넷 로그를 지원합니다:

- 네트워크 플로우 로그, 노드 간 연결 메타데이터 캡처(트래픽 내용 제외)
- 구성 감사 로그, 관리 작업 및 정책 변경 기록

감사 및 구성 로그 스트리밍은 Personal, Personal Plus 및 Enterprise 플랜에서 사용할 수 있습니다. 네트워크 플로우 로그 스트리밍은 Enterprise 플랜에서만 사용할 수 있습니다.

## GCS 환경으로 로그 가져오기

GCS가 지원되는 로그 스트리밍 대상이 됨에 따라, Tailscale 로그는 기존 Google Cloud 제어를 통해 관리되는 고객 소유 버킷으로 직접 전송됩니다. 거기서 조직은 IAM 정책 및 보존 규칙을 적용하고, 규정 준수를 위해 로그를 보관하거나, BigQuery 또는 Chronicle과 같은 다운스트림 시스템으로 데이터를 라우팅할 수 있습니다.

관리 콘솔의 Logs > Log streaming에서 Google Cloud Storage를 로그 스트리밍 엔드포인트로 구성하세요.

GCS로 로그 스트리밍을 시작하려면 로그 스트리밍 문서를 참조하세요.
