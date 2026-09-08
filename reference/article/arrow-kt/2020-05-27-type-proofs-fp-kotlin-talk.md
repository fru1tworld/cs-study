# Type Proofs and FP for the Kotlin Type System

- 공식 사이트 등록일: 2020-05-27.
- 자료 유형: 영상.
- 공식 게시물: [Type Proofs and FP for the Kotlin Type System](https://arrow-kt.io/community/blog/2020/05/27/type-proofs-fp-kotlin-talk/).
- 외부 원문: [글 또는 발표 자료](https://www.youtube.com/watch?v=lK80dPcsNUg&t=353s).
- 행사: Chicago Kotlin User Group Meetup.
- 수집일: 2026-09-08.
- 편집: 한국어 소개 추가, 웹 전용 서식과 상대 링크를 일반 Markdown에 맞게 조정함.
- 공식 게시물 본문 라이선스: [Apache-2.0](LICENSE.arrow-website). 외부 링크 대상의 이용 조건은 해당 원문 기준임.

[전체 목록](README.md) | [공식 Learn 정리](../../technology/pl/kotlin/arrow/01_introduction_and_setup.md)

## 한국어 소개

- Type Proofs로 타입 클래스, 유니언 타입, 정제 타입을 실험하는 Chicago 밋업 강연.
- 발행 당시의 Arrow와 Kotlin을 기준으로 한 자료임. 현재 API로 옮길 때는 [마이그레이션 정리](../../technology/pl/kotlin/arrow/09_design_integrations_and_migration.md) 참고.
- 아래 본문은 Arrow 공식 사이트가 제공하는 소개문임. 외부 글 전체나 영상 자막을 수집한 것은 아님.

## 공식 게시물 본문

Type Proofs is a new compiler plugin built on Arrow Meta enabling new features in the Kotlin type system, such as Type Classes, Union Types, Type Refinements, and many other extensions that make Functional Programming easier in Kotlin.

Type Proofs propositions are expressed as extension functions that unlock new relationships between types ad-hoc whilst remaining fully compatible with subtype polymorphism and the existing inheritance type system.

This talk demonstrates some of the new features the Arrow team is introducing in Arrow at the type level and IDE and how others can benefit from them when building libraries and applications.
