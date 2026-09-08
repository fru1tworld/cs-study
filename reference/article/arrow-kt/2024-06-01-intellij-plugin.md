# Arrow plug-in for IntelliJ 0.1 is here!

- 공식 사이트 등록일: 2024-06-01.
- 자료 유형: 글.
- 공식 게시물: [Arrow plug-in for IntelliJ 0.1 is here!](https://arrow-kt.io/community/blog/2024/06/01/intellij-plugin/).
- 수집일: 2026-09-08.
- 편집: 한국어 소개 추가, 웹 전용 서식과 상대 링크를 일반 Markdown에 맞게 조정함.
- 공식 게시물 본문 라이선스: [Apache-2.0](LICENSE.arrow-website). 외부 링크 대상의 이용 조건은 해당 원문 기준임.

[전체 목록](README.md) | [공식 Learn 정리](../../technology/pl/kotlin/arrow/01_introduction_and_setup.md)

## 한국어 소개

- 누락한 bind, Raise 스코프 오용과 개선 가능한 문법을 탐지하는 IntelliJ 플러그인 0.1 소개.

## 공식 게시물 본문

### Arrow plug-in for IntelliJ 0.1 is here!

One of the main goals of the Arrow project is to produce libraries
that follow well-known Kotlin idioms, and we strive to make them
as discoverable as possible. Nevertheless, the surface of some
components, like [typed errors](https://arrow-kt.io/learn/typed-errors/),
is quite large.
For that reason, we have been busy in the last weeks preparing
the first release of the
[Arrow plug-in for IntelliJ-based IDEs](https://plugins.jetbrains.com/plugin/24550-arrow).

This first version already focuses on three different aspects of
Arrow usage where we found that an additional companion can make
a big difference. The first aspect is the usage of typed errors:
the IDE will now suggest missing `.bind()` or `.bindAll()`,
mapping of error using `withError`, and promoting idioms like
`ensure` whenever possible.

The second aspect is warning about wrong usages of Arrow APIs
which cannot be prevented by Kotlin's type system alone. This includes
escaping of `Raise` contexts -- for example, using `sequence` or
`flow` inside `either` --, using `Atomic` with primitive types
-- where `AtomicInt` or `AtomicBoolean` should be used instead --,
or matching on `Eval` instances directly instead of using the
provided API -- which can easily lead to broken invariants.

The third aspect is applying some known recipes which may be hard
to know upfront. The first release includes a suggestion to add
the corresponding [serializer](https://arrow-kt.io/learn/quickstart/setup/serialization/)
when a type marked as `@Serializable` includes an Arrow Core type.
This is an area which we would like to explore more, helping with
the difficulties raised by the community.

The plug-in lives in a [separate repository](https://github.com/arrow-kt/arrow-intellij).
Please let us know your experience, and don't be shy to open issues
with suggestions for more features. They would help not only you
but potentially every user of the Arrow library.
