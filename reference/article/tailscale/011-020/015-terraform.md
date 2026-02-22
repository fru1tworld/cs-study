# Terraform으로 Tailscale 리소스 관리하기

작성자: Denton Gentry, Andrew Dunham
게시일: 2022년 8월 30일

---

인프라를 배포할 때, 테스트를 위해 환경을 자주 재배포하거나 수요 증가에 대응하여 서버를 확장해야 할 수 있습니다. 인프라 프로비저닝을 자동화하는 일반적인 도구가 바로 Terraform입니다. Terraform을 사용하면 인프라를 코드로 정의한 다음, 해당 인프라의 배포를 스크립트로 작성할 수 있습니다. Tailscale을 통해 접근하려는 서버를 배포하는 경우, 태그가 지정된 인증 키(tagged auth key)를 사용하여 적절한 권한으로 디바이스를 tailnet에 자동으로 연결함으로써 설정을 간소화할 수 있습니다. 하지만 Tailscale 배포 자체를 관리하려면 어떻게 해야 할까요?

Terraform을 사용하여 Tailscale의 ACL, DNS 설정, 인증 키 등을 정의하고 배포하는 방식으로 Tailscale 사용을 관리할 수도 있습니다. Tailscale은 Tailscale Terraform 프로바이더를 인수하여 지속적인 지원과 개발에 대한 책임을 맡게 되었습니다. 커뮤니티, 특히 David Bond가 Tailscale Terraform 프로바이더를 처음 개발했으며, Tailscale은 다른 사용자들에게 이 가치 있는 도구를 제공하기 위해 수행한 그들의 작업에 매우 감사드립니다.

## Tailscale Terraform 프로바이더가 이제 Tailscale에서 배포됩니다

누구나 Terraform 레지스트리에 프로바이더를 기여할 수 있으며, 자연스럽게 이런 질문이 생길 수 있습니다: 현재 프로바이더가 잘 작동하고 있다면, 왜 변경하는 걸까요? 이것을 커뮤니티 관리에서 Tailscale 관리로 전환함으로써, 커뮤니티 기여에 대한 접근 방식은 변하지 않습니다. 오히려 Tailscale은 이 프로바이더가 사용자들에게 얼마나 중요한지 인식하고 있기 때문에 이를 유지 관리하기로 약속합니다.

Tailscale이 이제 프로바이더를 배포합니다. 하지만 그것이 실제로 무엇을 의미할까요? 기술적으로는 프로바이더의 소스 코드를 Tailscale 조직으로 이전하고, Terraform 레지스트리에서 Tailscale의 네임스페이스로 프로바이더를 배포하며, Tailscale이 관리하는 키로 새 버전의 프로바이더에 서명하는 것을 의미합니다. 실질적으로는 Tailscale이 새로운 기능을 추가함에 따라 Terraform 프로바이더를 계속 개발하고, 새로운 기여를 검토하며, 추가 기능 요청에 응답한다는 것을 의미합니다.

Tailscale은 Tailscale Terraform 프로바이더 개발에 기여한 모든 분들, 특히 David Bond에게 감사드립니다. 회사는 Tailscale에 대한 커뮤니티 기여를 소중히 여기며, 커뮤니티가 구축하는 것에 감사드립니다.

## Tailscale의 Terraform 프로바이더로 ACL, DNS 설정, 인증 키 및 디바이스 관리 구성 가능

Tailscale과 함께 Terraform을 사용하려면, Tailscale용 API 키와 tailnet 이름으로 Tailscale Terraform 프로바이더를 구성하세요. 그러면 Terraform 프로바이더를 사용하여 프로그래밍 방식으로 다음을 수행할 수 있습니다:

- tailnet 정책 파일 정의 (`tailscale_acl` 리소스 사용)
- 글로벌 네임서버(`tailscale_dns_nameservers`), 분할 DNS(Split DNS)를 위한 제한된 네임서버(`tailscale_dns_search_paths`), MagicDNS 활성화 또는 비활성화(`tailscale_dns_preferences`)를 포함한 DNS 설정
- 재사용 가능 여부, 임시(ephemeral) 여부, 사전 승인 여부, 태그 지정 여부를 설정하는 인증 키 생성 (`tailscale_key`)
- 디바이스 승인(`tailscale_device_authorization`), 키 만료 비활성화(`tailscale_device_key`), 태그 설정(`tailscale_device_tags`), 서브넷 라우트 광고(`tailscale_device_subnet_routes`)를 포함한 디바이스 속성 관리

## 기존 Terraform 프로바이더에서 업데이트하기

이미 커뮤니티 기여 Terraform 프로바이더를 사용하고 있다면, Terraform 구성을 수정하고 `source`를 `tailscale/tailscale`로 설정하여 새 프로바이더로 업데이트할 수 있습니다:

```json
terraform {
  required_providers {
    tailscale = {
      source = "tailscale/tailscale" // 이전에는 davidsbond/tailscale
      version = "0.13.5"
    }
  }
}

provider "tailscale" {
  api_key = "tskey-1234567CNTRL-abcdefghijklmnopqrstu" // 권장하지 않음. 대신 환경 변수 `TAILSCALE_API_KEY` 사용
  tailnet = "example.com"
}
```

Tailscale API 키와 같은 민감한 정보를 소스 제어에 저장하는 것은 권장하지 않습니다. 대신 Terraform에서 이를 설정하려면 환경 변수 `TAILSCALE_API_KEY`를 사용하세요.

그런 다음 `terraform init`을 실행하세요.

이 새 프로바이더는 Terraform 레지스트리의 새 네임스페이스에서 사용할 수 있으며 새 키로 서명됩니다. `davidsbond/tailscale`에 대한 참조는 여전히 작동하지만, 더 이상 업데이트를 받지 않습니다. 다음에 Terraform 구성을 업데이트할 때 `tailscale/tailscale`로 전환하는 것이 권장됩니다.

Tailscale은 Tailscale API를 업데이트함에 따라 Terraform 프로바이더를 업데이트할 계획입니다. 피드백을 제공하거나 기능을 요청하려면 GitHub의 tailscale/terraform-provider-tailscale에 이슈를 제출해 주세요.

## Tailscale에는 Pulumi 프로바이더도 있습니다

Pulumi를 사용하고 있다면, Tailscale Pulumi 프로바이더는 Pulumi에서 유지 관리하며 Terraform 프로바이더에서 생성되므로, Pulumi 프로바이더도 계속 작동합니다.

Pulumi를 사용하여 Tailscale 리소스를 관리하려면, Tailscale API 키와 tailnet 이름으로 Pulumi를 구성한 다음, Pulumi를 사용하여 tailnet 정책 파일, DNS 설정, 인증 키 및 디바이스 속성을 관리하세요.

## 부록: Terraform 프로바이더를 이전하는 방법

사용자를 중단시키지 않고 Terraform 프로바이더를 한 조직에서 다른 조직으로 이전하는 방법은 명확하지 않습니다. Tailscale과 같은 상황에 있고 방법을 알고 싶다면, Tailscale이 수행한 단계를 기반으로 한 모범 접근 방식이 있습니다:

1. Terraform 레지스트리 계정에 가입합니다. GitHub로 로그인하면 기존 GitHub 조직에 연결할 수 있습니다. 그런 다음 Terraform 레지스트리에서 조직의 네임스페이스를 설정합니다.

2. GitHub에서 프로바이더의 소스 코드가 있는 저장소를 커뮤니티 기여자로부터 조직의 직원에게 이전합니다. 조직에 직접 이전할 수 없습니다. 그렇게 하려면 기여자에게 GitHub에서 조직의 공개 저장소를 생성할 수 있는 권한을 부여해야 하기 때문입니다. 직원에게 이전한 후, 다시 이번에는 조직으로 이전합니다.

3. 적절한 경우, 새 저장소에서 원래 기여자의 권한을 변경합니다. 예를 들어 Write에서 Read로 변경합니다. 지금은 행동 강령을 검토하고 조직에서 사용할 수 있는 기본 브랜치 이름과 같은 다른 규칙에 맞추기 좋은 시기이기도 합니다.

4. Terraform에 기존에 게시된 프로바이더를 서명에 사용된 기존 공개 키를 포함하여 조직의 네임스페이스로 이전해 달라고 정중히 요청합니다.

5. 릴리스에 서명하기 위해 조직의 새 공개-개인 키 쌍을 생성하고, 새 공개 키를 Terraform 레지스트리에 추가합니다.

6. Terraform 프로바이더의 새 버전을 빌드하고, 서명하고, 게시합니다.

GitHub는 원래 저장소에서 이전된 위치로의 참조(종속성 등)를 자동으로 업데이트합니다. Terraform은 CLI에서 프로바이더가 업데이트되었으며 새 프로바이더를 참조해야 한다고 사용자에게 경고하지만, 이전에 서명된 프로바이더의 사용은 여전히 허용합니다.

Tailscale 리소스를 프로그래밍 방식으로 관리하려면, Tailscale Terraform 프로바이더를 설치하고 문서를 확인하거나, Tailscale Pulumi 프로바이더를 설치하고 문서를 확인하세요.

---

원문: [https://tailscale.com/blog/terraform](https://tailscale.com/blog/terraform)
