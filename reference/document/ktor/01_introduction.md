# Ktor 소개

> 원본 참고: https://ktor.io/docs/

---

## Ktor란

Ktor는 Kotlin과 코루틴을 기반으로 한 비동기 프레임워크로, **연결된 애플리케이션(connected applications)** — 즉 HTTP/WebSocket 서버 및 클라이언트 — 을 만들기 위해 JetBrains에서 개발합니다. 가벼움, 비파괴적인 DSL, 플러그인 기반 아키텍처가 특징입니다.

핵심 컨셉:

- **embeddedServer / EngineMain** — JVM Servlet 컨테이너 없이도 단독 실행 가능.
- **엔진 분리** — Netty / Jetty / Tomcat / CIO 등 교체 가능한 엔진 위에서 동작.
- **모듈(Application 확장 함수)** — 라우트·설정·플러그인을 모아서 한 단위로 등록.
- **플러그인(파이프라인 인터셉터)** — 직렬화, 인증, 세션, 로깅 등 횡단 관심사를 install 한 번으로 끼움.
- **Routing DSL** — `routing { get("/x") { ... } }`로 표현되는 선언적 라우트.

---

## 그 외 자료

- [Ktor 공식 사이트](https://ktor.io/)
- [Server 문서 홈](https://ktor.io/docs/server-create-a-new-project.html)
- [프로젝트 생성기](https://start.ktor.io/)
- [GitHub 리포지토리](https://github.com/ktorio/ktor)
- [샘플 모음](https://github.com/ktorio/ktor-samples)

---

## 구성

| 번호 | 파일 | 주제 |
| --- | --- | --- |
| 01 | `01_introduction.md` | 소개 / 인덱스 |
| 02 | `02_create_project.md` | 새 프로젝트 만들기 (generator / CLI / IntelliJ) |
| 03 | `03_engines.md` | 엔진 (Netty / Jetty / CIO / Tomcat / Servlet) |
| 04 | `04_configuration_and_modules.md` | 설정 파일과 Application 모듈 |
| 05 | `05_routing.md` | 라우팅 DSL |
| 06 | `06_requests_and_responses.md` | 요청 / 응답 처리 |
| 07 | `07_plugins.md` | 플러그인 (내장 + 커스텀) |
| 08 | `08_content_negotiation.md` | Content Negotiation 및 직렬화 |
| 09 | `09_authentication.md` | 인증 (Basic / JWT / OAuth / Session) |
| 10 | `10_sessions.md` | 세션 관리 |
| 11 | `11_websockets.md` | WebSocket |
| 12 | `12_static_content.md` | 정적 콘텐츠 / 리소스 서빙 |
| 13 | `13_status_pages.md` | StatusPages — 예외/상태 처리 |
| 14 | `14_testing.md` | testApplication 기반 통합 테스트 |
| 15 | `15_deployment.md` | Fat JAR / 컨테이너 / Native 배포 |
