# Arrow Optics for Gradle in beta

- 공식 사이트 등록일: 2025-11-01.
- 자료 유형: 글.
- 공식 게시물: [Arrow Optics for Gradle in beta](https://arrow-kt.io/community/blog/2025/11/01/arrow-optics-gradle/).
- 수집일: 2026-09-08.
- 편집: 한국어 소개 추가, 웹 전용 서식과 상대 링크를 일반 Markdown에 맞게 조정함.
- 공식 게시물 본문 라이선스: [Apache-2.0](LICENSE.arrow-website). 외부 링크 대상의 이용 조건은 해당 원문 기준임.

[전체 목록](README.md) | [공식 Learn 정리](../../technology/pl/kotlin/arrow/01_introduction_and_setup.md)

## 한국어 소개

- KSP 설정과 companion 생성을 자동화하는 Arrow Optics Gradle 플러그인 베타 소개.

## 공식 게시물 본문

### Introducing Arrow Optics for Gradle

In order to unlock the full power of Arrow Optics, lenses and prisms for your
own types must be defined. This is a tedious task, that can be automate with
the help of the `@optics` annotation. Alas, [setting up](https://arrow-kt.io/learn/quickstart/setup/#additional-setup-for-optics)
the plug-in is not easy (especially in Multiplatform projects), and the
architecture of the plugin means that you need to write a `companion object`
explicitly on every one of your types.

We've been working on making this process easier, and the result is the
new [Arrow Optics plugin for Gradle](https://plugins.gradle.org/plugin/io.arrow-kt.optics).
This plugin configures your project to process `@optics` annotations,
taking care of all the peculiarities of Kotlin JVM and Multiplatform plugins.
Furthermore, it sets up the Kotlin compiler to generate `companion object`
automatically (if they are not already present), saving time and boilerplate.

This new plugin is in **beta**, the KSP plugin still remains the officially
supported way to generate optics. But we would also love feedback on the
new approach, with the goal of making this simpler option the default.
If you want to try the new plugin, you need to:

- Remove any previous configuration of the Arrow Optics KSP plugin,
- Add `id("io.arrow-kt.optics") version "2.2.3"` to your `plugins` block,
- Call `arrowOptics()` at the **end** of your `kotlin` block.

```kotlin
plugins {
    id("io.arrow-kt.optics") version "2.2.3"
}

kotlin {
    // compiler, target, and source set configuration
    // ...
    arrowOptics()
}
```

Please let us know of any issues you encounter or any feedback on how to
make the process even more approachable, in either our
[issues page](https://github.com/arrow-kt/arrow/issues)
or the `#arrow` channel on [Kotlin Slack](https://slack-chats.kotlinlang.org/c/arrow).
