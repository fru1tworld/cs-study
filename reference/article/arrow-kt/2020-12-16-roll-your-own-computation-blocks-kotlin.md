# Roll your own Computation blocks in Kotlin

- 공식 사이트 등록일: 2020-12-16.
- 자료 유형: 영상.
- 공식 게시물: [Roll your own Computation blocks in Kotlin](https://arrow-kt.io/community/blog/2020/12/16/roll-your-own-computation-blocks-kotlin/).
- 외부 원문: [글 또는 발표 자료](https://youtu.be/0_zatebXMDU).
- 행사: Lambda Lille.
- 수집일: 2026-09-08.
- 편집: 한국어 소개 추가, 웹 전용 서식과 상대 링크를 일반 Markdown에 맞게 조정함.
- 공식 게시물 본문 라이선스: [Apache-2.0](LICENSE.arrow-website). 외부 링크 대상의 이용 조건은 해당 원문 기준임.

[전체 목록](README.md) | [공식 Learn 정리](../../technology/pl/kotlin/arrow/01_introduction_and_setup.md)

## 한국어 소개

- Kotlin suspend와 Arrow Continuations의 reset, shift로 계산 블록을 만드는 강연.
- 발행 당시의 Arrow와 Kotlin을 기준으로 한 자료임. 현재 API로 옮길 때는 [마이그레이션 정리](../../technology/pl/kotlin/arrow/09_design_integrations_and_migration.md) 참고.
- 아래 본문은 Arrow 공식 사이트가 제공하는 소개문임. 외부 글 전체나 영상 자막을 수집한 것은 아님.

## 공식 게시물 본문

Computation blocks empower library authors and users to build ad-hoc operators and DSLs over any data-type getting rid of API complexity and simplifying composition. In this talk, we will learn how we can build Computation blocks over Kotlin suspend functions & the Arrow Continuations library's `reset` / `shift` capabilities. We will demonstrate the composition of well known JVM data-types and patterns such as lists, futures, streams, and IOs, where callback chains can be simply replaced by a single
suspended operator. The Kotlin suspension system provides enough capabilities to implement delimited continuations allowing us to ignore methods such as `map` & `flatMap` on your favorite data-type in favor of direct imperative syntax. Leveraging Kotlin suspension & thinking of Continuations as "The Mother of all Monads", we will embark on this journey where we'll build and roll our own computation blocks with Arrow Continuations.
