# FP with Kotlin/Arrow: Monad Comprehensions & Parallel Processing

- 공식 사이트 등록일: 2020-02-26.
- 자료 유형: 영상.
- 공식 게시물: [FP with Kotlin/Arrow: Monad Comprehensions & Parallel Processing](https://arrow-kt.io/community/blog/2020/02/26/fp-with-kotlin-arrow/).
- 외부 원문: [글 또는 발표 자료](https://youtu.be/nAtzuIRryuE).
- 수집일: 2026-09-08.
- 편집: 한국어 소개 추가, 웹 전용 서식과 상대 링크를 일반 Markdown에 맞게 조정함.
- 공식 게시물 본문 라이선스: [Apache-2.0](LICENSE.arrow-website). 외부 링크 대상의 이용 조건은 해당 원문 기준임.

[전체 목록](README.md) | [공식 Learn 정리](../../technology/pl/kotlin/arrow/01_introduction_and_setup.md)

## 한국어 소개

- 모나드 합성, 컴프리헨션과 병렬 map을 Arrow Fx로 설명하는 강연.
- 발행 당시의 Arrow와 Kotlin을 기준으로 한 자료임. 현재 API로 옮길 때는 [마이그레이션 정리](../../technology/pl/kotlin/arrow/09_design_integrations_and_migration.md) 참고.
- 아래 본문은 Arrow 공식 사이트가 제공하는 소개문임. 외부 글 전체나 영상 자막을 수집한 것은 아님.

## 공식 게시물 본문

Arrow has multiple libraries available for functional programming. In this talk we'll focus on Arrow FX and learn how to handle IO in a functional way with an introduction to monadic composition. Then we'll examine how to compose monads in a cleaner fashion with Arrow FX's monad comprehensions. Finally, we'll take a look at how to parallelize IO monads with parallel map strategies.
