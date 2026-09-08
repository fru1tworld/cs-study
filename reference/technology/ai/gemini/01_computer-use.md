# Google Gemini: Computer Use

> 공식 문서: https://ai.google.dev/gemini-api/docs/computer-use

## 1. 개요

Gemini API의 Computer Use는 화면 스크린샷을 입력받아 클릭, 타이핑, 스크롤 같은 GUI 조작 액션을 계획하는 도구 사용 모드다. 모델이 계획을 반환하면 호출하는 애플리케이션이 이를 실행하고, 바뀐 화면을 다시 전달한다. Anthropic의 [Claude Computer Use](../anthropic/01_computer-use.md)도 같은 구조를 따른다.

```
클라이언트: 현재 화면 스크린샷 + 목표 지시 전달
     ▼
Gemini: 스크린샷 분석 후 다음 액션(예: 좌표 클릭, 텍스트 입력) 제안
     ▼
클라이언트: 실제 환경에서 액션 실행 후 새 스크린샷 캡처
     ▼
(목표 달성까지 반복)
```

## 2. 다른 벤더 구현과의 공통 패턴

- 관찰-행동 루프: 스크린샷(관찰) → 액션 제안 → 실행 → 재관찰의 반복 구조
- 클라이언트 책임 분리: 모델은 "무엇을 할지"만 결정, 실제 입력 이벤트 생성은 클라이언트 인프라의 몫
- 좌표 기반 액션: 특정 브라우저/OS API에 종속되지 않는 범용 좌표 기반 인터페이스
- 안전장치 필요성: 파괴적이거나 결제 등 민감한 액션 전에는 사람의 확인 단계를 두는 것이 권장됨

Anthropic, Google, OpenAI의 Computer Use는 세부 API 형식이 다르더라도 화면을 관찰하고 좌표 기반 액션을 계획한다는 구조를 공유한다. 따라서 벤더를 선택한 뒤에도 실제 입력을 실행하고 다음 화면을 전달하는 클라이언트가 필요하다.

## 3. 실무에서 고려할 점

- 실행 환경 격리: 가상 머신/샌드박스 브라우저에서 실행해 실제 시스템 피해 방지
- 지연 시간: 매 스텝마다 스크린샷 캡처 + 모델 추론 왕복이 필요 → 실시간성이 요구되는 작업에는 부적합할 수 있음
- 반복 실패 처리: 같은 화면 상태에서 반복적으로 실패하는 루프를 감지하고 종료하는 로직 필요
- 대안 검토: 대상 서비스가 구조화된 API나 [MCP](../../../standard/mcp/specification.md) 도구를 제공한다면, Computer Use보다 그쪽이 더 정확하고 효율적

## 참고 자료

- [Gemini API Computer Use (공식)](https://ai.google.dev/gemini-api/docs/computer-use)
- [Claude Computer Use](../anthropic/01_computer-use.md)
- [OpenAI computer-use-preview](../openai/01_computer-use-preview.md)
