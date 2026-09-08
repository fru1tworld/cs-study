# Arrow Fx: Functional Domain Modeling with Kotlin

- 공식 사이트 등록일: 2020-06-05.
- 자료 유형: 영상.
- 공식 게시물: [Arrow Fx: Functional Domain Modeling with Kotlin](https://arrow-kt.io/community/blog/2020/06/05/functional-domain-modeling-kotlin/).
- 외부 원문: [글 또는 발표 자료](https://youtu.be/6sw8GAhUJz0).
- 행사: Kotliners 2020.
- 수집일: 2026-09-08.
- 편집: 한국어 소개 추가, 웹 전용 서식과 상대 링크를 일반 Markdown에 맞게 조정함.
- 공식 게시물 본문 라이선스: [Apache-2.0](LICENSE.arrow-website). 외부 링크 대상의 이용 조건은 해당 원문 기준임.

[전체 목록](README.md) | [공식 Learn 정리](../../technology/pl/kotlin/arrow/01_introduction_and_setup.md)

## 한국어 소개

- Optics, Fx, Meta를 조합해 도메인 모델과 동시성, 리소스 수명을 구성하는 강연.
- 발행 당시의 Arrow와 Kotlin을 기준으로 한 자료임. 현재 API로 옮길 때는 [마이그레이션 정리](../../technology/pl/kotlin/arrow/09_design_integrations_and_migration.md) 참고.
- 아래 본문은 Arrow 공식 사이트가 제공하는 소개문임. 외부 글 전체나 영상 자막을 수집한 것은 아님.

## 공식 게시물 본문

Arrow Fx is a purely functional concurrency framework for Kotlin’s suspend system.

In this talk, we will learn how typed functional programming and functional domain modeling powered by Arrow Optics, Fx, and Meta can be applied to assemble powerful applications and architectures from small and simple building blocks.

Simon and Raul will cover important topics and patterns such as optics, union types, refined types, type classes, automatic task cancellation, safe resource handling, and compare how Arrow Fx differs from KotlinX coroutines.
