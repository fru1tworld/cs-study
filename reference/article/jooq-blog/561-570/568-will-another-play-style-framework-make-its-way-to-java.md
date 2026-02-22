# 또 다른 Play 스타일 프레임워크가 Java에 진출할 것인가?

> 원문: https://blog.jooq.org/will-another-play-style-framework-make-its-way-to-java/

게시일: 2013년 9월 27일 | 저자: lukaseder

---

최근 매우 인기 있는 Play Framework의 아이디어를 기반으로 한 Ninja Web Framework를 발견했습니다. Zenexity와 Typesafe가 주로 Scala 생태계에서 Play를 지원하기 위해 동맹을 맺은 이후로, Ninja는 Java를 "이등 시민"으로 만들고 싶지 않은 사람들에게 대체재가 될 수 있습니다.

베를린에 본사를 둔 FinalFrontierLabs에서 관리하는 이 프레임워크는 그들이 Java에서 Play를 사용할 때 Play의 열렬한 팬이었기 때문에 만들어졌습니다.

*사진 저작권 (c) 2013 FinalFrontierLabs*

주목할 가치가 있는 프레임워크입니다!

---

## 댓글

### Samuel Rutishauser

Ninja가 Guice를 통한 의존성 주입을 사용하고 Play 1에서 사용했던 일반 텍스트 라우트 파일을 제거한 것과 같은 긍정적인 설계 선택들을 보니 유망해 보입니다. 문서는 개선될 수 있을 것 같습니다.

---

### Rob Diana

Play가 Java를 완전히 버리고 Scala만 지원한다는 주장은 정확하지 않은 것 같습니다.

---

### lukaseder (저자)

네, 맞습니다. Play가 Java를 완전히 포기한 것은 아니지만, 주로 Scala 지원에 초점을 맞추고 있다는 의미였습니다.

---

### Johan Andrén

Typesafe는 Akka에서와 마찬가지로 Play 2에서도 Java와 Scala를 병행 지원하고 있습니다. Java 지원이 중단된 것은 아닙니다.

---

### Stéphane C

저는 이전 Play 1.x 사용자로서 Play 2의 Scala 템플릿 요구 사항에 불만을 가지고 있었습니다. Scala를 좋아하지 않는 사람으로서 Ninja는 템플릿 엔진 대안(예: Rythm)과 함께 사용할 수 있어 매우 진지한 Play 대안이 될 수 있습니다.

---

### Steph CL

이 블로그 포스트의 논의로 인해 Play 커뮤니티에서 Java 지원에 대한 대화가 촉발되었습니다. Play 팀이 특히 Java 8 지원을 중심으로 Java 개발에 더 집중할 것이라는 소식이 있습니다.

---

## 역자 주

이 글은 2013년에 작성된 것으로, Play Framework와 Ninja Web Framework에 대한 당시의 논의를 담고 있습니다. 현재 Play Framework는 Java와 Scala 모두를 계속해서 지원하고 있으며, Java 생태계도 Spring Boot, Quarkus, Micronaut 등 다양한 현대적 프레임워크들이 등장하여 크게 발전했습니다.
