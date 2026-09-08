# Arrow plug-in for IntelliJ 0.3

- 공식 사이트 등록일: 2025-04-22.
- 자료 유형: 글.
- 공식 게시물: [Arrow plug-in for IntelliJ 0.3](https://arrow-kt.io/community/blog/2025/04/22/arrow-intellij-0-3/).
- 수집일: 2026-09-08.
- 편집: 한국어 소개 추가, 웹 전용 서식과 상대 링크를 일반 Markdown에 맞게 조정함.
- 공식 게시물 본문 라이선스: [Apache-2.0](LICENSE.arrow-website). 외부 링크 대상의 이용 조건은 해당 원문 기준임.

[전체 목록](README.md) | [공식 Learn 정리](../../technology/pl/kotlin/arrow/01_introduction_and_setup.md)

## 한국어 소개

- IntelliJ 2025.1 호환성과 Raise, Eval gutter 표시를 추가한 Arrow IDE 플러그인 0.3 소개.

## 공식 게시물 본문

### Arrow plug-in for IntelliJ 0.3

The new version of the [Arrow plug-in for IntelliJ](https://plugins.jetbrains.com/plugin/24550-arrow) (and compatible IDEs) is out! This release fixes some problems in inspections related to `Raise`, and brings **compatibility with 2025.1**. The plug-in is compatible with **both K1 and K2 mode** of the Kotlin plug-in.

This release also brings new **gutter icons** inspired by the "suspended function" and "recursive function" icons in the Kotlin plug-in. These gutter icons highlight uses of `Raise` functions, and delayed computations with `Eval`. Our goal is to make a bit more explicit what the surface syntax of Kotlin keeps implicit.

![Gutter icon for Raise](https://arrow-kt.io/img/blog/gutter-raise.png) <br /> _Gutter icon for `Raise`_

![Gutter icons for Eval](https://arrow-kt.io/img/blog/gutter-eval.png) <br /> _Gutter icons for `Eval.later` and `Eval.always`_
