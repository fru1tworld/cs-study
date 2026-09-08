# What could possibly go wrong? - A safer programming with Arrow

- 공식 사이트 등록일: 2020-02-26.
- 자료 유형: 영상.
- 공식 게시물: [What could possibly go wrong? - A safer programming with Arrow](https://arrow-kt.io/community/blog/2020/02/26/safer-programming-with-arrow/).
- 외부 원문: [글 또는 발표 자료](https://youtu.be/C9SmleSSeGk).
- 수집일: 2026-09-08.
- 편집: 한국어 소개 추가, 웹 전용 서식과 상대 링크를 일반 Markdown에 맞게 조정함.
- 공식 게시물 본문 라이선스: [Apache-2.0](LICENSE.arrow-website). 외부 링크 대상의 이용 조건은 해당 원문 기준임.

[전체 목록](README.md) | [공식 Learn 정리](../../technology/pl/kotlin/arrow/01_introduction_and_setup.md)

## 한국어 소개

- 외부 API, 변환, 저장 과정의 실패를 Arrow 데이터 타입과 효과로 다루는 강연.
- 발행 당시의 Arrow와 Kotlin을 기준으로 한 자료임. 현재 API로 옮길 때는 [마이그레이션 정리](../../technology/pl/kotlin/arrow/09_design_integrations_and_migration.md) 참고.
- 아래 본문은 Arrow 공식 사이트가 제공하는 소개문임. 외부 글 전체나 영상 자막을 수집한 것은 아님.

## 공식 게시물 본문

Your Kotlin app grabs data from an API, transforms it and saves the processed data in a database. However, there are so many things that could go wrong at runtime: the API might be inaccessible, the data is not what you expected or the data can't be persisted in the database. You can start adding try catch blocks to your function in your objects, but there is a better way to do it: treat your impure functions as computations with context, pass them around just like other values, and make the necessary unsafe invocation from a single point of your app, your main function.

This talk will walk you through the core functional concepts of Arrow, you will learn how Some, Either and even IO are functor, applicative and monad. You can use the code example from this talk as a starting point for your Arrow-learning, to write safer, simpler and more elegant functional code in Kotlin.
