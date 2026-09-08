# OpenAI: computer-use-preview 모델

> 공식 문서: https://developers.openai.com/api/docs/models/computer-use-preview

## 1. 개요

`computer-use-preview`는 화면 스크린샷을 관찰해 GUI 조작 액션을 계획하는 데 특화된 OpenAI 모델이다. 스크린샷을 보고 좌표 클릭이나 타이핑을 제안한 뒤, 바뀐 화면을 다시 관찰하는 루프를 따른다. 이 구조는 Anthropic [Claude Computer Use](../anthropic/01_computer-use.md), Google [Gemini Computer Use](../gemini/01_computer-use.md)와 같다.

## 2. Responses API와의 통합

OpenAI의 Computer Use 기능은 Responses API의 `computer_use` 도구 타입으로 노출된다. 모델이 액션을 반환하면 클라이언트가 이를 실행하고, 결과 화면을 새 스크린샷으로 전달한다. 애플리케이션은 이 과정을 반복하는 루프를 직접 구성해야 한다.

```
1. 클라이언트: 초기 목표 + 첫 스크린샷을 Responses API에 전달
2. 모델: computer_call 액션(click, type, scroll 등) 반환
3. 클라이언트: 실제 환경에서 액션 실행
4. 클라이언트: 실행 후 스크린샷을 computer_call_output으로 전달
5. 목표 달성 또는 최대 스텝 도달까지 반복
```

## 3. Preview 단계의 의미

이름의 "preview"는 정식 GA(General Availability) 이전의 초기 공개 단계라는 뜻이다. 프리뷰 단계 모델은 다음과 같은 제약이 있을 수 있으므로, API 변경 가능성과 실패 시 재시도·폴백 방식을 함께 고려해야 한다.

- API 형태 변경 가능성: 정식 출시 전까지 요청/응답 스키마가 바뀔 수 있음
- 제한된 가용성: 접근 권한 신청이나 사용량 제한이 걸려 있을 수 있음
- 성능/안정성 변동: 정식 버전 대비 실패율이 더 높을 수 있어 재시도/폴백 로직이 중요
- 프로덕션 의존 지양: 핵심 비즈니스 로직을 프리뷰 모델에만 의존하도록 설계하지 않는 것이 권장됨

## 4. 안전 설계 공통 원칙

Computer Use 기능은 모델이 제안한 액션을 실제 환경에서 실행한다. 프로덕션에 도입할 때는 벤더와 무관하게 실행 범위와 승인 절차를 정해야 한다.

- 샌드박스 실행: 실제 사용자 환경이 아닌 격리된 가상 머신/컨테이너에서 실행
- 위험 동작 확인 단계: 결제, 삭제, 계정 설정 변경 등 되돌리기 어려운 동작 전에는 사람의 승인 단계 삽입
- 스텝 수 제한: 무한 루프/반복 실패를 막기 위한 최대 스텝 카운트 설정
- 로깅/감사: 어떤 화면을 보고 어떤 액션을 취했는지 기록해 사후 검증 가능하게 함

## 5. 다른 벤더 구현과의 위치 비교

- Anthropic
  - 기능명: Computer Use
  - 통합 방식: Tool Use 프레임워크의 특수 도구
- Google
  - 기능명: Computer Use
  - 통합 방식: Gemini API 도구 사용 모드
- OpenAI
  - 기능명: computer-use-preview
  - 통합 방식: Responses API의 `computer_use` 도구 타입

세 벤더 모두 화면을 관찰하고 액션을 실행하는 같은 루프를 각자의 API 규칙에 맞춰 제공한다.

## 참고 자료

- [computer-use-preview (OpenAI 공식)](https://developers.openai.com/api/docs/models/computer-use-preview)
- [Claude Computer Use](../anthropic/01_computer-use.md)
- [Gemini Computer Use](../gemini/01_computer-use.md)
