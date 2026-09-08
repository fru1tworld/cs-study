# Domain Model Validation In Kotlin: Part 3

- 공식 사이트 등록일: 2022-03-10.
- 자료 유형: 글.
- 공식 게시물: [Domain Model Validation In Kotlin: Part 3](https://arrow-kt.io/community/blog/2022/03/10/domain-model-validation-in-kotlin-part-3/).
- 외부 원문: [글 또는 발표 자료](https://tibtof.medium.com/domain-model-validation-in-kotlin-part-3-96c3fd4af342).
- 수집일: 2026-09-08.
- 편집: 한국어 소개 추가, 웹 전용 서식과 상대 링크를 일반 Markdown에 맞게 조정함.
- 공식 게시물 본문 라이선스: [Apache-2.0](LICENSE.arrow-website). 외부 링크 대상의 이용 조건은 해당 원문 기준임.

[전체 목록](README.md) | [공식 Learn 정리](../../technology/pl/kotlin/arrow/01_introduction_and_setup.md)

## 한국어 소개

- 도메인 모델 검증 연재 3편. 검증 문맥을 전달하고 테스트에서 교체하는 방법을 다룸.
- 발행 당시의 Arrow와 Kotlin을 기준으로 한 자료임. 현재 API로 옮길 때는 [마이그레이션 정리](../../technology/pl/kotlin/arrow/09_design_integrations_and_migration.md) 참고.
- 아래 본문은 Arrow 공식 사이트가 제공하는 소개문임. 외부 글 전체나 영상 자막을 수집한 것은 아님.

## 공식 게시물 본문

In the third part of the series, Tiberiu Tofan explores multiple techniques of using a context when doing validations
and how the context can be changed in the tests to simulate success or failure. All using just Kotlin standard library.
