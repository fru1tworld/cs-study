# 2026년 1월 Tailscale 이번 달 업데이트

게시일: 2026년 1월 23일

저자: Kevin Purdy

원문: [This month at Tailscale for January 2026](https://tailscale.com/blog/january-26-product-update)

---

우리는 여러분의 네트워크를 더 안정적이고, 관리하기 쉬우며, 안전하게 만들기 위한 업데이트를 지속적으로 제공하고 있습니다. 매월, 클라이언트, 관리자 도구, 통합 기능, 인프라 전반에 걸쳐 가장 영향력 있는 변경 사항들을 강조하여 새로운 기능과 개선된 기능을 파악할 수 있도록 합니다.

최근 Tailscale 소프트웨어에서 변경된 사항들을 정리했습니다. 클라이언트 변경 사항, API 개선 사항 및 기타 업데이트가 있습니다. [업데이트 가이드](https://tailscale.com/kb/1067/update)를 참조하세요.

## 변경 사항

### 워크로드 아이덴티티 페더레이션 API (Workload Identity Federation API)

[페더레이션 아이덴티티(Federated identities)](https://tailscale.com/kb/1581/workload-identity-federation)가 이제 Tailscale의 더 많은 부분과 통합되었습니다:

- [Tailscale API](https://tailscale.com/kb/1101/api) (생성, 읽기, 업데이트, 삭제)
- `tailscale-client-go-v2` (구성)
- [Tailscale Terraform 프로바이더](https://tailscale.com/kb/1210/terraform-provider) (구성)

### 인도 DERP 리전 도시명 업데이트

인도에서 호스팅되는 [DERP 서버](https://tailscale.com/kb/1232/derp-servers)의 도시명이 공식 명칭인 벵갈루루(Bengaluru)를 반영하여 업데이트되었습니다. 호스팅 제공업체와 IP 주소는 변경되지 않았습니다.

## 클라이언트 업데이트

### v1.92.5

Tailscale 1.92.5부터 Windows 및 Linux 클라이언트는 더 이상 기본적으로 상태 파일 암호화(state file encryption)와 하드웨어 증명 키(hardware attestation keys)를 활성화하지 않습니다. [Hacker News 토론](https://news.ycombinator.com/item?id=46531925)에서 자세한 내용을 확인할 수 있습니다. Apple 기기 및 Android 클라이언트는 기본적으로 보안 노드 상태 저장소 암호화가 계속 활성화됩니다.

## GitHub Action

### v4.1.1

[Tailscale GitHub Action](https://tailscale.com/kb/1276/tailscale-github-action)이 이제 macOS 기반 GitHub 러너에서 캐시 저장 및 검색에 올바른 아키텍처를 사용합니다.

## 컨테이너, Kubernetes 및 `tsrecorder` 업데이트

### 컨테이너 이미지 v1.92.5

- 하드웨어 증명 키(hardware attestation keys)가 더 이상 Kubernetes 상태 `Secrets`에 추가되지 않아 노드 변경이 가능해졌습니다.

### Kubernetes 오퍼레이터 v1.92.5

- ACME 계정 키가 재생성될 경우 갱신 실패를 방지하기 위해 인증서 갱신이 더 이상 기본적으로 ARI 주문으로 수행되지 않습니다.
- 하드웨어 증명 키(hardware attestation keys)가 더 이상 Kubernetes 상태 `Secrets`에 추가되지 않습니다.

### tsrecorder v1.92.5

이 버전은 라이브러리 업데이트 외에 다른 변경 사항이 없습니다.

---

최근 몇 주간의 주요 내용을 정리했습니다. 질문이나 피드백이 있으시면 언제든지 도움을 드리겠습니다.
