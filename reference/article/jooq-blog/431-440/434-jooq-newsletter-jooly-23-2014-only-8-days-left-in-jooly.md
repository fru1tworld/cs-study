# jOOQ 뉴스레터: jOOLY 2014년 7월 23일 - jOOLY가 8일밖에 안 남았습니다!

> 원문: https://blog.jooq.org/jooq-newsletter-jooly-23-2014-only-8-days-left-in-jooly/

게시일: 2014년 7월 23일 | 작성자: lukaseder

jOOLY(7월)가 거의 끝나갑니다. 2014년 7월에 jOOQ를 구매하시면 20% 할인 혜택과 함께 Markus Winand의 "SQL Performance Explained" e-book을 무료로 받으실 수 있습니다. 이 프로모션이 끝나기 전에 서둘러 이 할인을 활용하시기 바랍니다!

## 오늘의 트윗

우리 고객분들이 jOOQ에 대해 말씀해 주신 내용을 공유합니다:

Alvaro Hernandez Tortosa:
> "@JavaOOQ 항상 SQL을 직접 제어하세요. SQL을 숨기는 프레임워크는 잊어버리세요. SQL은 정말 강력합니다 :)"

Calvin Thomas:
> "드디어 원하는 스택을 찾은 것 같아요! #angularjs #bootstrap #playframework #scala #JOOQ"

Adam Bien:
Adam Bien은 Java EE 프로젝트에서 jOOQ 통합에 대해 설명해 주셨습니다.

## 단계별 가격 정책 모델

최근 워크스테이션 기반 가격 정책 방식에 대한 논의와 팀 구성원 변동을 처리해야 하는 대규모 조직의 우려 사항을 인지하고 있습니다. 이러한 문제를 해결하기 위해 10개 워크스테이션을 초과하는 구독에 적용 가능한 단계별 가격 정책 모델을 공식적으로 도입합니다.

이 대안은 가격이 부가 가치에 비례하여 조정된다는 우리의 철학을 유지하면서도 대량 구매 조직의 관리 복잡성을 줄여줍니다. 관심 있으신 분들은 업데이트된 라이선스 계약서(17페이지에 가격 세부 정보 포함)를 검토하시거나 영업팀에 직접 문의하시기 바랍니다.

참고로 이 혜택은 jOOLY 프로모션 할인과 결합하여 사용할 수 있습니다.

## jOOQ 3.5: Oracle AQ 지원

곧 출시될 jOOQ 3.5 버전에는 Oracle Advanced Queueing을 위한 코드 생성기 및 API 확장이 포함됩니다. 이 기능은 Oracle AQ의 강력한 데이터 변경 알림 기능 작업을 단순화합니다.

CallableStatement를 통해 복잡한 OBJECT 타입 바인딩을 관리하는 대신, jOOQ 접근 방식은 타입 안전 참조를 생성하여 전체 타입 정보와 함께 객체를 인큐(enqueue) 및 디큐(dequeue)하는 간단한 작업을 허용합니다.

```java
// 인큐
DBMS_AQ.enqueue(conf, QUEUE_NAME, object);

// 디큐
MyObjectType object = DBMS_AQ.dequeue(conf, QUEUE_NAME);
```

## 커뮤니티 존

커뮤니티에서 주목할 만한 세 가지 기여를 소개합니다:

- gradle-jooq-plugin - Etienne Studer가 만든 완전한 기능을 갖춘 Gradle용 jOOQ 플러그인입니다. Maven/Gradle 사용자들에게 유용합니다.

- Bert van Langen의 튜토리얼 - 그의 블로그에서 jOOQ에 대한 훌륭한 입문 가이드를 제공합니다.

- Marco Behler의 기사 - "Java persistence ghetto(그리고 jOOQ가 이를 어떻게 바꿀 수 있는지)"에 대해 작성했습니다. JPA와 SQL이 같은 프로젝트에서 공존할 수 있음을 보여줍니다.

## 피드백 존

콘텐츠 품질과 개선 제안에 관한 독자 여러분의 피드백을 환영합니다. 의견, 질문 또는 주제 추천이 있으시면 contact@datageekery.com으로 연락해 주세요.
