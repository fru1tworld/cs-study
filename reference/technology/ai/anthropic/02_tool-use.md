# Anthropic: Tool Use (Claude)

> 공식 문서: https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview

## 1. 개요

Tool Use(함수 호출)를 사용하려면 Claude에게 도구(함수) 목록과 각 도구의 입력 스키마를 전달한다. 모델은 사용자 요청에 필요한 도구와 인자를 골라 구조화된 호출 요청으로 응답한다. 이 요청을 실제로 실행하는 쪽은 호출한 애플리케이션이며, 모델이 직접 부수효과를 일으키지는 않는다.

```
1. 애플리케이션이 도구 목록(name, description, input_schema)과 함께 메시지 전송
2. Claude가 도구 호출이 필요하다고 판단하면 tool_use 블록으로 응답
3. 애플리케이션이 실제로 해당 함수를 실행
4. 실행 결과를 tool_result로 다시 Claude에 전달
5. Claude가 결과를 바탕으로 최종 답변 생성 (또는 추가 도구 호출)
```

## 2. 도구 정의 형식

다음 정의는 도시 이름을 받아 날씨를 조회하는 도구다. `description`은 도구의 용도를, `input_schema`는 호출할 때 필요한 입력을 설명한다.

```json
{
  "name": "get_weather",
  "description": "지정한 도시의 현재 날씨를 조회한다",
  "input_schema": {
    "type": "object",
    "properties": {
      "city": { "type": "string", "description": "도시 이름" }
    },
    "required": ["city"]
  }
}
```

- `name`: 도구 식별자
- `description`: 모델이 언제 이 도구를 써야 할지 판단하는 핵심 근거
- `input_schema`: JSON Schema 형식의 입력 파라미터 정의

이처럼 이름, 설명, JSON Schema로 도구를 기술하는 방식은 [MCP](../../../standard/mcp/specification.md)의 Tool 정의와도 공통적이다.

## 3. 응답 형식

```json
{
  "role": "assistant",
  "content": [
    { "type": "text", "text": "날씨를 확인해볼게요." },
    {
      "type": "tool_use",
      "id": "toolu_01A",
      "name": "get_weather",
      "input": { "city": "서울" }
    }
  ]
}
```

이 응답을 받은 애플리케이션은 `tool_use` 블록을 파싱해 실제 함수를 실행한다. 이어서 결과를 `tool_result` 콘텐츠 블록에 담아 다음 요청으로 전달하고, `tool_use_id`로 앞의 호출과 연결한다.

```json
{
  "role": "user",
  "content": [
    {
      "type": "tool_result",
      "tool_use_id": "toolu_01A",
      "content": "서울, 맑음, 15도"
    }
  ]
}
```

## 4. 도구 선택 제어

`tool_choice` 옵션으로 도구 사용 여부와 사용할 도구를 제어한다.

- `auto`(기본값): 모델이 도구 사용 여부와 어떤 도구를 쓸지 자율적으로 판단
- `any`: 반드시 도구 중 하나를 호출하도록 강제
- `tool`(특정 이름 지정): 지정한 특정 도구만 호출하도록 강제
- 병렬 도구 호출: 한 응답에서 여러 도구를 동시에 호출하도록 요청 가능

## 5. 정확도를 높이는 설계 팁

- `description`을 구체적으로 작성: 모델이 도구 선택 여부와 인자 값을 판단하는 유일한 단서이기 때문
- 도구 개수를 필요한 만큼만 제공: 도구가 많을수록 잘못된 도구를 선택할 확률 상승
- 입력 스키마에 열거형(`enum`) 활용: 자유 형식 문자열보다 값 오류 감소
- 에러 결과도 `tool_result`로 명확히 전달: 모델이 실패를 인지하고 대안을 시도할 수 있게 함

## 6. Computer Use와의 관계

[Computer Use](./01_computer-use.md)는 스크린샷, 클릭, 타이핑처럼 미리 정의된 도구를 사용하는 Tool Use의 한 형태로 볼 수 있다. 구조화된 API가 있는 시스템에서는 일반 Tool Use로 함수를 직접 노출하는 편이 더 정확하다. 그래서 GUI 외에 접근 수단이 없는 경우에 Computer Use를 선택하는 것이 일반적이다.

## 참고 자료

- [Tool use (Anthropic 공식)](https://platform.claude.com/docs/en/agents-and-tools/tool-use/overview)
- [Computer use](./01_computer-use.md)
- [MCP Specification](../../../standard/mcp/specification.md)
