# Vue 3 공식 기술 문서 한국어판

공식 기술 문서 107개를 주제별로 묶은 한국어 문서다. 시작하기부터 API 참조까지 본문과 코드 예제를 수록하며, 각 절에 영어 원문과 한국어 번역 기반의 고정 커밋 링크를 제공한다.

## 목차

- [01. Vue 3 시작하기와 튜토리얼](01_getting_started_and_tutorial.md)
- [02. Vue 3 템플릿과 반응성 기초](02_essentials.md)
- [03. Vue 3 컴포넌트와 로직 재사용](03_components_and_reusability.md)
- [04. Vue 3 내장 컴포넌트와 애니메이션](04_built_ins_and_animation.md)
- [05. Vue 3 애플리케이션 확장과 운영](05_scaling_typescript_and_best_practices.md)
- [06. Vue 3 반응성과 렌더링 심화](06_reactivity_and_rendering_in_depth.md)
- [07. Vue 3 애플리케이션, 컴포지션 및 반응성 API](07_composition_and_reactivity_apis.md)
- [08. Vue 3 컴포넌트와 고급 API](08_component_and_advanced_apis.md)
- [09. Vue 3 스타일 가이드, 예제와 참고 자료](09_style_guide_examples_and_reference.md)

## 읽는 방법

처음 학습한다면 01부터 06까지 읽고, 구체적인 API가 필요할 때 07과 08을 찾아보면 된다. 스타일 규칙, 예제 20종, FAQ, 용어집과 에러 코드 표는 09에 있다. 튜토리얼은 01에 15단계 전체를 수록했다.

컴포지션 API와 옵션 API 설명은 해당 예제 앞에 구분을 표시했다. 일반 Markdown에서 읽을 수 있도록 문서의 안내 상자를 일반 본문으로 옮겼다. 실행형 데모는 코드나 소스 링크로 제공하며, 브라우저에서의 동작은 공식 사이트에서 확인할 수 있다.

예제와 이미지, 가이드의 Vue 데모 소스는 `assets/`에 있다. 튜토리얼의 `App/composition.js`와 `App/options.js`는 서로 다른 API 방식의 예제이며 `template.html`을 공유한다. `_hint/`는 정답에서 달라지는 파일만 담으므로 나머지 파일은 시작 코드와 함께 읽는다. 이 폴더는 독립적으로 실행하는 Vue 앱이나 문서 사이트 프로젝트가 아니다.

범위는 Vue 3 본체의 가이드, API, 튜토리얼과 기술 참고 자료다. Vue Router, Pinia, Nuxt의 별도 문서와 파트너, 후원, 테마 소개 등 사이트 운영 페이지는 포함하지 않는다.

## 출처와 이용 조건

원저작자: Yuxi (Evan) You 및 Vue documentation contributors.
한국어 번역: vuejs-translations/docs-ko 기여자.

영문 기준 커밋(2026-09-15): https://github.com/vuejs/docs/tree/40aa88af0094f7bab4aaf786e55c748a6a251d88

한국어 번역 기준 커밋(2026-08-21): https://github.com/vuejs-translations/docs-ko/tree/6e646acb5687510feda533281faa546b26ea243c

공식 영어 문서: https://vuejs.org/

공식 한국어 문서: https://ko.vuejs.org/

이미지를 제외한 원본 콘텐츠는 CC BY 4.0이다. 저작권 및 라이선스 전문은 [LICENSE](LICENSE)에 보존했다. 이미지는 각 원저작자의 저작권과 이용 조건을 따르며 CC BY 4.0에 일괄 포함되지 않는다. 이미지 출처는 한국어 기준 저장소의 `src/` 아래에서 이 사본의 `assets/` 이하와 같은 상대 경로에 있다.

라이선스: https://creativecommons.org/licenses/by/4.0/

이 사본은 원문의 후속 변경을 번역하고, 한국어 표현과 문단 연결을 다듬었으며, 장 구성과 내부 링크를 로컬 열람에 맞게 수정했다. API 목차와 에러 코드 표는 정적 문서로 구성하고 에러 메시지에 한국어 설명을 병기했다. 공식 프로젝트가 배포하는 번역본과 구분되는 수정본이다.
