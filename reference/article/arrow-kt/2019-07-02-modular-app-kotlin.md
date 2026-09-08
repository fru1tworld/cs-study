# Modular functional programming with Kotlin

- 공식 사이트 등록일: 2019-07-02.
- 자료 유형: 글.
- 공식 게시물: [Modular functional programming with Kotlin](https://arrow-kt.io/community/blog/2019/07/02/modular-app-kotlin/).
- 외부 원문: [글 또는 발표 자료](https://www.msec.it/blog/modular-functional-programming-composition-with-kotlin/).
- 수집일: 2026-09-08.
- 편집: 한국어 소개 추가, 웹 전용 서식과 상대 링크를 일반 Markdown에 맞게 조정함.
- 공식 게시물 본문 라이선스: [Apache-2.0](LICENSE.arrow-website). 외부 링크 대상의 이용 조건은 해당 원문 기준임.

[전체 목록](README.md) | [공식 Learn 정리](../../technology/pl/kotlin/arrow/01_introduction_and_setup.md)

## 한국어 소개

- 함수형 Kotlin 모듈의 조합, 테스트와 컴파일 시점 의존성 관리를 다룸.
- 발행 당시의 Arrow와 Kotlin을 기준으로 한 자료임. 현재 API로 옮길 때는 [마이그레이션 정리](../../technology/pl/kotlin/arrow/09_design_integrations_and_migration.md) 참고.
- 아래 본문은 Arrow 공식 사이트가 제공하는 소개문임. 외부 글 전체나 영상 자막을 수집한 것은 아님.

## 공식 게시물 본문

This post proposes a possible solution in order to structure and compose a pure functional Kotlin application, in order to better manage and decouple modules, get simpler tests and manage the Dependency Injection at compile time.
