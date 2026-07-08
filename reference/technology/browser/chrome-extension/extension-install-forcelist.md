# Chrome 정책: ExtensionInstallForcelist

> 공식 문서: https://chromeenterprise.google/policies/extension-install-forcelist/

## 1. 개요

`ExtensionInstallForcelist`는 기업/조직이 관리하는 Chrome 브라우저에 특정 확장 프로그램을 자동으로 설치하고, 사용자가 임의로 제거하지 못하도록 강제하는 엔터프라이즈 정책이다. Chrome Enterprise/그룹 정책(Windows GPO), macOS의 관리형 환경설정(managed preferences), 또는 클라우드 기반 Chrome Browser Cloud Management를 통해 배포된다.

---

## 2. 정책 형식

```json
{
  "ExtensionInstallForcelist": [
    "ahfgeienlihckogmohjhadlkjgocpleb;https://clients2.google.com/service/update2/crx",
    "extension_id_2"
  ]
}
```

| 구성 요소 | 설명 |
|-----------|------|
| 확장 프로그램 ID | Chrome Web Store 또는 사설 배포 시스템의 32자 확장 ID |
| 업데이트 URL (선택) | 세미콜론(`;`) 뒤에 명시. 생략하면 기본 Chrome Web Store 업데이트 URL 사용 |

---

## 3. 관련 정책과의 관계

| 정책 | 역할 |
|------|------|
| `ExtensionInstallForcelist` | 강제 설치 + 제거 불가 |
| `ExtensionInstallAllowlist` | 사용자가 설치할 수 있는 확장 화이트리스트 |
| `ExtensionInstallBlocklist` | 설치를 금지할 확장 블랙리스트 (`*`로 전체 차단 후 개별 허용 조합 가능) |
| `ExtensionSettings` | 위 정책들을 세분화해 확장 ID별/업데이트 URL별로 더 정교하게 제어하는 통합 정책 (신규 배포 시 권장) |

```json
{
  "ExtensionSettings": {
    "ahfgeienlihckogmohjhadlkjgocpleb": {
      "installation_mode": "force_installed",
      "update_url": "https://clients2.google.com/service/update2/crx"
    },
    "*": {
      "installation_mode": "blocked"
    }
  }
}
```

---

## 4. 배포 경로

| 플랫폼 | 배포 방식 |
|--------|-----------|
| Windows | 그룹 정책(GPO) 또는 레지스트리 |
| macOS | 관리형 환경설정 프로필(.mobileconfig / MDM) |
| Linux | JSON 정책 파일 (`/etc/opt/chrome/policies/managed/`) |
| ChromeOS | Google Admin Console |
| 클라우드 관리 | Chrome Browser Cloud Management (플랫폼 무관, 사용자 로그인 기반) |

---

## 5. 사설(비공개) 확장 프로그램 배포

Chrome Web Store에 공개하지 않은 사내 전용 확장 프로그램도 자체 호스팅한 update manifest XML을 `update_url`로 지정해 강제 설치할 수 있다. 이는 기업 내부 도구(사내 SSO 확장, 보안 에이전트 확장 등)를 일반 사용자에게 노출하지 않고 배포하는 표준적인 방법이다.

---

## 6. 사용자 경험에 미치는 영향

| 항목 | 동작 |
|------|------|
| 제거 시도 | `chrome://extensions`에서 제거 버튼이 비활성화되거나 재설치됨 |
| 비활성화 시도 | 정책에 따라 토글 자체가 잠기거나, 껐다가도 정책 재적용 시 다시 켜짐 |
| 신뢰 표시 | 관리형 브라우저에서는 확장 목록에 "관리자가 설치함" 배지가 표시됨 |

---

## 7. 보안 관점

기업 입장에서는 필수 보안 도구(엔드포인트 보호 확장, DLP 확장)를 사용자가 임의로 끄지 못하게 강제하는 용도로 쓰이지만, 반대로 이 정책이 침해되면(관리 콘솔 계정 탈취 등) 조직 내 모든 브라우저에 악성 확장을 강제 배포할 수 있는 경로가 되므로, 관리 콘솔 접근 권한 자체의 보호가 중요하다.

---

## 8. 요약

- `ExtensionInstallForcelist`는 관리형 Chrome 환경에서 특정 확장을 강제 설치·제거 불가 상태로 만드는 정책이다.
- 신규 배포에는 더 세분화된 제어가 가능한 `ExtensionSettings` 사용이 권장된다.
- 사설 확장 프로그램도 자체 update URL로 강제 배포할 수 있다.

---

## 참고 자료

- [ExtensionInstallForcelist (Chrome Enterprise 공식)](https://chromeenterprise.google/policies/extension-install-forcelist/)
- [ExtensionSettings (Chrome Enterprise 공식)](https://chromeenterprise.google/policies/#ExtensionSettings)
