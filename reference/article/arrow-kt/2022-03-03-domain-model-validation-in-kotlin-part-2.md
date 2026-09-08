# Domain Model Validation In Kotlin: Part 2

- 공식 사이트 등록일: 2022-03-03.
- 자료 유형: 글.
- 공식 게시물: [Domain Model Validation In Kotlin: Part 2](https://arrow-kt.io/community/blog/2022/03/03/domain-model-validation-in-kotlin-part-2/).
- 외부 원문: [글 또는 발표 자료](https://medium.com/@tibtof/domain-model-validation-in-kotlin-part-2-fb4726ef8f8d).
- 수집일: 2026-09-08.
- 편집: 한국어 소개 추가, 웹 전용 서식과 상대 링크를 일반 Markdown에 맞게 조정함.
- 공식 게시물 본문 라이선스: [Apache-2.0](LICENSE.arrow-website). 외부 링크 대상의 이용 조건은 해당 원문 기준임.

[전체 목록](README.md) | [공식 Learn 정리](../../technology/pl/kotlin/arrow/01_introduction_and_setup.md)

## 한국어 소개

- 도메인 모델 검증 연재 2편. Validated로 다중 속성, 리스트 검증과 오류 누적을 처리함.
- 발행 당시의 Arrow와 Kotlin을 기준으로 한 자료임. 현재 API로 옮길 때는 [마이그레이션 정리](../../technology/pl/kotlin/arrow/09_design_integrations_and_migration.md) 참고.
- 아래 본문은 Arrow 공식 사이트가 제공하는 소개문임. 외부 글 전체나 영상 자막을 수집한 것은 아님.

## 공식 게시물 본문

In the second article in this series, Tiberiu Tofan writes how Validated type can be
used to validate multiple properties, accumulate the errors, apply individual
element validations to lists of elements, and create rules that
depend on numerous properties.
