# Model Context Protocol (MCP) Specification 개요

> 공식 스펙: https://modelcontextprotocol.io/specification

## 1. 개요

MCP(Model Context Protocol)는 LLM 애플리케이션(호스트)이 외부 데이터 소스와 도구를 표준화된 방식으로 연결할 수 있게 하는 프로토콜이다. Anthropic이 처음 제안했으며, 이후 여러 AI 벤더와 도구 생태계가 채택하는 개방형 표준으로 확장됐다. 핵심 목표는 "M개의 LLM 애플리케이션 × N개의 데이터 소스/도구" 조합마다 커스텀 통합을 만들어야 했던 문제를, 공통 프로토콜 하나로 M+N 문제로 줄이는 것이다.

```
기존 방식 (M×N 통합)                MCP 방식 (M+N 통합)
App A ─┬─ Tool X                    App A ─┐
App B ─┼─ Tool Y     (각자 커스텀)    App B ─┼─ MCP ─┬─ Server X
App C ─┴─ Tool Z                    App C ─┘        ├─ Server Y
                                                      └─ Server Z
```

---

## 2. 아키텍처: Host / Client / Server

| 구성 요소 | 역할 |
|-----------|------|
| Host | 사용자가 상호작용하는 LLM 애플리케이션 (예: Claude Desktop, IDE 플러그인) |
| Client | Host 내부에서 하나의 MCP Server와 1:1로 연결을 관리하는 컴포넌트 |
| Server | 도구/데이터/프롬프트를 제공하는 별도 프로세스 또는 서비스 |

```
Host (LLM 애플리케이션)
 ├── Client 1 ──── Server A (파일시스템 접근)
 ├── Client 2 ──── Server B (데이터베이스 조회)
 └── Client 3 ──── Server C (외부 API 연동)
```

Host는 여러 Client를 관리하고, 각 Client는 하나의 Server와 독립된 세션을 유지한다.

---

## 3. 전송 계층(Transport)

| 방식 | 사용 시나리오 |
|------|----------------|
| stdio | 로컬 프로세스로 Server를 실행 (표준 입출력으로 JSON-RPC 메시지 교환) |
| Streamable HTTP | 원격 Server와 HTTP(+ SSE 스트리밍)로 통신 |

메시지 포맷은 두 전송 방식 모두 [JSON-RPC 2.0](https://www.jsonrpc.org/specification)을 사용한다.

---

## 4. 핵심 프리미티브

MCP는 Server가 Host에 노출할 수 있는 세 가지 핵심 개념을 정의한다.

| 프리미티브 | 설명 | 제어 주체 |
|------------|------|-----------|
| Tools | LLM이 호출해 부수효과(파일 쓰기, API 호출 등)를 일으킬 수 있는 함수 | 모델(에이전트)이 호출 여부 결정 |
| Resources | LLM에 컨텍스트로 제공되는 읽기 전용 데이터(파일, DB 레코드 등) | 애플리케이션이 언제 포함할지 결정 |
| Prompts | 재사용 가능한 프롬프트 템플릿 | 사용자가 명시적으로 선택해 사용 |

```json
// tools/list 응답 예시
{
  "tools": [
    {
      "name": "search_files",
      "description": "지정한 디렉터리에서 파일을 검색한다",
      "inputSchema": {
        "type": "object",
        "properties": { "query": { "type": "string" } },
        "required": ["query"]
      }
    }
  ]
}
```

---

## 5. 생명주기

```
Client → Server: initialize (프로토콜 버전, capabilities 교환)
Server → Client: initialize 응답 (지원하는 기능 목록)
Client → Server: initialized 알림

  ... 정상 운영 (tools/list, tools/call, resources/read 등) ...

Client/Server: 연결 종료
```

`initialize` 단계에서 양측은 서로 지원하는 capability(예: `tools`, `resources`, `prompts`, `sampling`)를 교환해, 이후 호출 가능한 기능 범위를 확정한다.

---

## 6. Sampling: Server가 LLM을 역으로 호출

MCP는 Server가 필요할 때 Host의 LLM에게 추론을 요청할 수 있는 `sampling/createMessage` 기능도 정의한다. 이는 Server가 별도로 자체 LLM API 키를 관리하지 않고도, 이미 Host가 연결된 모델의 추론 능력을 빌려 쓸 수 있게 해준다 (예: 에이전트형 Server가 하위 작업을 위해 보조 LLM 호출이 필요한 경우).

---

## 7. 권한과 보안 모델

| 원칙 | 설명 |
|------|------|
| 사용자 동의 | Tool 호출, Resource 접근은 원칙적으로 사용자가 인지/승인할 수 있어야 함 |
| 명시적 승인 | Host는 Server가 제공하는 기능을 사용자에게 명확히 보여주고 승인받는 UX를 구현해야 함(스펙 권고) |
| 최소 권한 | Server는 필요한 기능만 노출하고, Host는 필요한 Server에만 연결 |
| 격리 | Server는 Host 프로세스와 분리된 프로세스/권한 경계에서 동작하는 것이 일반적 |

---

## 8. WebMCP와의 관계

MCP는 원래 로컬 프로세스 간(Host-Server) 프로토콜로 설계됐지만, 이 "도구 노출" 개념을 브라우저 웹 페이지 수준으로 가져오려는 시도가 [WebMCP](../w3c/webmcp.md)다. WebMCP는 MCP의 도구 스키마 철학을 웹 콘텐츠 보안 모델 안에서 재구현하는 실험적 확장으로 볼 수 있다.

---

## 9. 요약

- MCP는 LLM 애플리케이션과 외부 도구/데이터를 표준 프로토콜로 연결해 M×N 통합 문제를 M+N으로 줄인다.
- Host-Client-Server 구조이며, JSON-RPC 2.0 기반 stdio/HTTP 전송을 지원한다.
- Tools(호출), Resources(컨텍스트), Prompts(템플릿) 세 프리미티브로 기능을 노출한다.
- Sampling으로 Server가 Host의 LLM 추론 능력을 역으로 활용할 수 있다.

---

## 참고 자료

- [MCP Specification (공식)](https://modelcontextprotocol.io/specification)
- [WebMCP Explainer](../w3c/webmcp.md)
- [chrome-devtools-mcp 개요](../../technology/browser/automation-internals/chrome-devtools-mcp.md)
