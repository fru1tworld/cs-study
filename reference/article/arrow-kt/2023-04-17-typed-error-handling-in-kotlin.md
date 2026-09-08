# Typed Error Handling in Kotlin

- 공식 사이트 등록일: 2023-04-17.
- 자료 유형: 글.
- 공식 게시물: [Typed Error Handling in Kotlin](https://arrow-kt.io/community/blog/2023/04/17/typed-error-handling-in-kotlin/).
- 외부 원문: [글 또는 발표 자료](https://medium.com/@mitchellyuwono/typed-error-handling-in-kotlin-11ff25882880).
- 수집일: 2026-09-08.
- 편집: 한국어 소개 추가, 웹 전용 서식과 상대 링크를 일반 Markdown에 맞게 조정함.
- 공식 게시물 본문 라이선스: [Apache-2.0](LICENSE.arrow-website). 외부 링크 대상의 이용 조건은 해당 원문 기준임.

[전체 목록](README.md) | [공식 Learn 정리](../../technology/pl/kotlin/arrow/01_introduction_and_setup.md)

## 한국어 소개

- Kotlin의 typed error 처리 방식을 코드 복잡도와 생산성 관점에서 비교한 글 소개.
- 아래 본문은 Arrow 공식 사이트가 제공하는 소개문임. 외부 글 전체나 영상 자막을 수집한 것은 아님.

## 공식 게시물 본문

### Typed Error Handling in Kotlin

A comparative study about several typed-error handling practices in Kotlin.

There are various approaches to error handling in the Kotlin community.
In this article we’ve explored a small subset of typed error handling practices in the community.

From the approaches explored, there were three patterns that aligns with Kotlin recommendation with
relatively low cognitive complexity including: Sealed class matching with early returns, Arrow's `either { }` builder,
and Arrow's `context(Raise<E>)` with context-receivers.

Arrow's `context(Raise<E>)` achieved the most optimized score on all aspects of
developer productivity. This includes having the lowest cognitive complexity, the lowest cyclomatic complexity
as well as the most succinct with the least lines of codes.

Read the full article: [Typed Error Handling in Kotlin](https://medium.com/@mitchellyuwono/typed-error-handling-in-kotlin-11ff25882880).
