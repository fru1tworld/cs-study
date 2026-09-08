# Model Context Protocol (MCP) Specification 개요

> 공식 스펙: https://modelcontextprotocol.io/specification

## 1. 개요

MCP(Model Context Protocol)는 LLM 애플리케이션(호스트)을 외부 데이터 소스나 도구와 연결하는 표준 프로토콜이다. Anthropic이 처음 제안한 뒤 여러 AI 벤더와 도구 생태계가 채택하는 개방형 표준으로 확장되었다.

애플리케이션과 도구마다 별도로 연동하면 M개의 애플리케이션과 N개의 도구 사이에 M×N개의 통합이 필요하다. MCP는 양쪽이 같은 프로토콜을 구현하게 해 이 작업을 M+N으로 줄이는 것을 목표로 한다.

```
기존 방식 (M×N 통합)                MCP 방식 (M+N 통합)
App A ─┬─ Tool X                    App A ─┐
App B ─┼─ Tool Y     (각자 커스텀)    App B ─┼─ MCP ─┬─ Server X
App C ─┴─ Tool Z                    App C ─┘        ├─ Server Y
                                                      └─ Server Z
```

---

## 2. 아키텍처: Host / Client / Server

- Host: 사용자가 상호작용하는 LLM 애플리케이션 (예: Claude Desktop, IDE 플러그인)
- Client: Host 내부에서 하나의 MCP Server와 1:1로 연결을 관리하는 컴포넌트
- Server: 도구, 데이터, 프롬프트를 제공하는 별도 프로세스 또는 서비스

```
Host (LLM 애플리케이션)
 ├── Client 1 ──── Server A (파일시스템 접근)
 ├── Client 2 ──── Server B (데이터베이스 조회)
 └── Client 3 ──── Server C (외부 API 연동)
```

Host는 여러 Client를 관리하며, 각 Client는 하나의 Server와 독립된 세션을 유지한다.

---

## 3. 전송 계층(Transport)

- stdio: 로컬 프로세스로 Server를 실행 (표준 입출력으로 JSON-RPC 메시지 교환)
- Streamable HTTP: 원격 Server와 HTTP(+ SSE 스트리밍)로 통신

두 전송 방식 모두 메시지 형식으로 [JSON-RPC 2.0](https://www.jsonrpc.org/specification)을 사용한다.

---

## 4. 핵심 프리미티브

MCP는 Server가 Host에 제공할 수 있는 기능을 다음 세 가지로 정의한다.

- Tools: LLM이 호출해 부수효과(파일 쓰기, API 호출 등)를 일으킬 수 있는 함수
  - 제어 주체: 모델(에이전트)이 호출 여부 결정
- Resources: LLM에 컨텍스트로 제공되는 읽기 전용 데이터(파일, DB 레코드 등)
  - 제어 주체: 애플리케이션이 언제 포함할지 결정
- Prompts: 재사용 가능한 프롬프트 템플릿
  - 제어 주체: 사용자가 명시적으로 선택해 사용

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

`initialize` 단계에서 양측은 `tools`, `resources`, `prompts`, `sampling` 등 자신이 지원하는 capability를 교환한다. 이 정보로 이후 호출할 수 있는 기능의 범위를 정한다.

---

## 6. Sampling: Server가 LLM을 역으로 호출

MCP는 Server가 Host의 LLM에 추론을 요청하는 `sampling/createMessage`도 정의한다. Server가 자체 LLM API 키를 관리하는 대신 Host에 연결된 모델을 활용할 수 있다. 예를 들어 에이전트형 Server가 하위 작업을 처리하기 위해 보조 LLM 호출을 요청하는 경우에 사용할 수 있다.

---

## 7. 권한과 보안 모델

- 사용자 동의: Tool 호출과 Resource 접근을 사용자가 인지하고 승인할 수 있어야 함
- 명시적 승인: Host는 Server가 제공하는 기능을 사용자에게 명확히 보여주고 승인받는 UX를 구현해야 함(스펙 권고)
- 최소 권한: Server는 필요한 기능만 노출, Host는 필요한 Server에만 연결
- 격리: Server는 일반적으로 Host와 프로세스 및 권한을 분리해 동작

---

## 8. WebMCP와의 관계

MCP는 원래 로컬 프로세스 간(Host-Server) 프로토콜로 설계되었다. 이 도구 노출 개념을 브라우저의 웹 페이지로 가져오려는 시도가 [WebMCP](../w3c/webmcp.md)다. MCP의 도구 스키마 철학을 웹 콘텐츠 보안 모델 안에서 재구현하는 실험적 확장으로 볼 수 있다.

---

## 9. 요약

- MCP는 LLM 애플리케이션과 외부 도구, 데이터를 표준 프로토콜로 연결해 M×N 통합 문제를 M+N으로 축소함
- Host-Client-Server 구조, JSON-RPC 2.0 기반 stdio/HTTP 전송 지원
- Tools(호출), Resources(컨텍스트), Prompts(템플릿) 세 프리미티브로 기능을 노출
- Sampling으로 Server가 Host의 LLM 추론 능력을 역으로 활용 가능

---

## 참고 자료

- [MCP Specification (공식)](https://modelcontextprotocol.io/specification)
- [WebMCP Explainer](../w3c/webmcp.md)
- [chrome-devtools-mcp 개요](../../technology/frontend/browser/automation-internals/06_chrome-devtools-mcp.md)
