# Asynchronisme et hexagone en Kotlin avec ArrowKt

- 공식 사이트 등록일: 2020-06-11.
- 자료 유형: 영상.
- 공식 게시물: [Asynchronisme et hexagone en Kotlin avec ArrowKt](https://arrow-kt.io/community/blog/2020/06/11/asynchronisme-et-hexagone-en-kotlin-avec-Arrow/).
- 외부 원문: [글 또는 발표 자료](https://youtu.be/moJpV-BgezM).
- 행사: Lambda Lille.
- 수집일: 2026-09-08.
- 편집: 한국어 소개 추가, 웹 전용 서식과 상대 링크를 일반 Markdown에 맞게 조정함.
- 공식 게시물 본문 라이선스: [Apache-2.0](LICENSE.arrow-website). 외부 링크 대상의 이용 조건은 해당 원문 기준임.

[전체 목록](README.md) | [공식 Learn 정리](../../technology/pl/kotlin/arrow/01_introduction_and_setup.md)

## 한국어 소개

- 비동기 도메인 동작을 기술 인프라에서 분리하는 헥사고날 아키텍처 강연. 프랑스어 자료임.
- 발행 당시의 Arrow와 Kotlin을 기준으로 한 자료임. 현재 API로 옮길 때는 [마이그레이션 정리](../../technology/pl/kotlin/arrow/09_design_integrations_and_migration.md) 참고.
- 아래 본문은 Arrow 공식 사이트가 제공하는 소개문임. 외부 글 전체나 영상 자막을 수집한 것은 아님.

## 공식 게시물 본문

J'aime bien le DDD et surtout les architectures hexagonales. Avoir un domaine auto-portant et non couplé à des blocs techniques comme Spring (ou autres) apporte beaucoup dans la testabilité et l'évolutivité de l'application.
Les modèles d'asynchronismes (programmation réactive, retardée, coroutines...) empêchent la dissociation stricte de notre modèle métier et de notre code infra dans un langage comme Kotlin.
Obligé d'utiliser une lib de coroutine ou autre programmation reactive.
Deux solutions s'offrent alors :

- Définir que les modèles d'asynchronisme sont des invariants de notre domaine et accepter ce couplage
- Chercher comment modéliser notre domaine comme un ensemble de comportements asynchrones
Dans ce talk nous allons voir comment réaliser la deuxième solution en utilisant la librairie Arrow et son modèle conceptuel d'asynchronisme pour nous permettre de découpler notre domaine de toute logique d'infrastructure.
