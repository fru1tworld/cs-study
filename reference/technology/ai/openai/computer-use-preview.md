# OpenAI: computer-use-preview 모델

> 공식 문서: https://developers.openai.com/api/docs/models/computer-use-preview

## 1. 개요

`computer-use-preview`는 OpenAI가 제공하는, 화면 스크린샷을 관찰해 GUI 조작 액션을 계획하는 데 특화된 모델이다. Anthropic [Claude Computer Use](../anthropic/computer-use.md), Google [Gemini Computer Use](../gemini/computer-use.md)와 마찬가지로 관찰(스크린샷)-행동(좌표 클릭/타이핑)-재관찰 루프 구조를 따른다.

---

## 2. Responses API와의 통합

OpenAI의 Computer Use 기능은 Responses API의 `computer_use` 도구 타입으로 노출되며, 모델이 반환하는 액션을 클라이언트가 실행하고 그 결과(새 스크린샷)를 다시 모델에 전달하는 루프를 애플리케이션이 직접 구성해야 한다.

```
1. 클라이언트: 초기 목표 + 첫 스크린샷을 Responses API에 전달
2. 모델: computer_call 액션(click, type, scroll 등) 반환
3. 클라이언트: 실제 환경에서 액션 실행
4. 클라이언트: 실행 후 스크린샷을 computer_call_output으로 전달
5. 목표 달성 또는 최대 스텝 도달까지 반복
```

---

## 3. Preview 단계의 의미

이름의 "preview"가 나타내듯, 이 모델은 아직 프로덕션 안정성이 완전히 보장된 정식 GA(General Availability) 단계가 아니라 초기 공개 단계에 해당한다. 프리뷰 단계 모델은 다음과 같은 특징을 갖는 경우가 많다.

| 특징 | 실무 영향 |
|------|-----------|
| API 형태 변경 가능성 | 정식 출시 전까지 요청/응답 스키마가 바뀔 수 있음 |
| 제한된 가용성 | 접근 권한 신청이나 사용량 제한이 걸려 있을 수 있음 |
| 성능/안정성 변동 | 정식 버전 대비 실패율이 더 높을 수 있어 재시도/폴백 로직이 중요 |
| 프로덕션 의존 지양 | 핵심 비즈니스 로직을 프리뷰 모델에만 의존하도록 설계하지 않는 것이 권장됨 |

---

## 4. 안전 설계 공통 원칙

Computer Use류 기능을 프로덕션에 도입할 때는 벤더와 무관하게 다음 원칙이 공통적으로 적용된다.

| 원칙 | 설명 |
|------|------|
| 샌드박스 실행 | 실제 사용자 환경이 아닌 격리된 가상 머신/컨테이너에서 실행 |
| 위험 동작 확인 단계 | 결제, 삭제, 계정 설정 변경 등 되돌리기 어려운 동작 전에는 사람의 승인 단계 삽입 |
| 스텝 수 제한 | 무한 루프/반복 실패를 막기 위한 최대 스텝 카운트 설정 |
| 로깅/감사 | 어떤 화면을 보고 어떤 액션을 취했는지 기록해 사후 검증 가능하게 함 |

---

## 5. 다른 벤더 구현과의 위치 비교

| 벤더 | 기능명 | 통합 방식 |
|------|--------|-----------|
| Anthropic | Computer Use | Tool Use 프레임워크의 특수 도구 |
| Google | Computer Use | Gemini API 도구 사용 모드 |
| OpenAI | computer-use-preview | Responses API의 `computer_use` 도구 타입 |

세 벤더 모두 근본적으로 같은 관찰-행동 루프 패러다임을 각자의 API 컨벤션에 맞춰 노출하고 있다.

---

## 6. 요약

- `computer-use-preview`는 OpenAI Responses API에서 스크린샷 기반 GUI 자동화를 담당하는 프리뷰 단계 모델이다.
- 액션 실행 루프(모델 제안 → 클라이언트 실행 → 결과 재전달)를 애플리케이션이 직접 구성해야 한다.
- 프리뷰 단계이므로 API 변경 가능성을 고려해 프로덕션 핵심 경로에는 폴백을 함께 설계해야 한다.

---

## 참고 자료

- [computer-use-preview (OpenAI 공식)](https://developers.openai.com/api/docs/models/computer-use-preview)
- [Claude Computer Use](../anthropic/computer-use.md)
- [Gemini Computer Use](../gemini/computer-use.md)
