# Nicer data transformation with KopyKat and Optics

- 공식 사이트 등록일: 2023-05-04.
- 자료 유형: 영상.
- 공식 게시물: [Nicer data transformation with KopyKat and Optics](https://arrow-kt.io/community/blog/2023/05/04/data-transformation-kotlinconf/).
- 외부 원문: [글 또는 발표 자료](https://youtu.be/atV8liVgd3w).
- 행사: KotlinConf.
- 수집일: 2026-09-08.
- 편집: 한국어 소개 추가, 웹 전용 서식과 상대 링크를 일반 Markdown에 맞게 조정함.
- 공식 게시물 본문 라이선스: [Apache-2.0](LICENSE.arrow-website). 외부 링크 대상의 이용 조건은 해당 원문 기준임.

[전체 목록](README.md) | [공식 Learn 정리](../../technology/pl/kotlin/arrow/01_introduction_and_setup.md)

## 한국어 소개

- KopyKat과 Arrow Optics로 중첩된 불변 데이터의 변환을 간결하게 만드는 강연.
- 아래 본문은 Arrow 공식 사이트가 제공하는 소개문임. 외부 글 전체나 영상 자막을 수집한 것은 아님.

## 공식 게시물 본문

### Nicer Data Transformation With KopyKat and Optics

Watch [Alejandro Serrano](https://twitter.com/trupill)'s presentation from KotlinConf 2023 about data transformation.

Data classes are incredibly useful when modeling our domain in an immutable way. The Kotlin compiler gives us many niceties, including 'copy' to create a new value based on a previous one. However, this 'copy' often falls short. This talk explores two alternatives: KopyKat, a plug-in to generate additional variations of 'copy', and Arrow Optics, a whole framework to transform this immutable data.
