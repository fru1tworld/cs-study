# 좋은 API 설계

> 원문: https://blog.jooq.org/the-good-api-design/

API 설계 가이드라인을 잘 정리한 체크리스트를 발견했습니다. 발췌 내용은 다음과 같습니다:

1. 권장 - API와 구현을 별도의 패키지에 배치하라
2. 권장 - API는 상위 레벨 패키지에, 구현은 하위 레벨 패키지에 배치하라
3. 고려 - 대규모 API는 여러 패키지로 분리하는 것을 고려하라
4. 고려 - API 패키지와 구현 패키지를 별도의 Java 아카이브에 넣는 것을 고려하라
5. 지양 (최소화) - API 내에서 구현 클래스에 대한 내부 의존성을 최소화하라
6. 지양 - 불필요한 API 파편화를 피하라
7. 금지 - API 패키지에 공개(public) 구현 클래스를 배치하지 마라
8. 금지 - 호출자(caller)와 구현 클래스 사이에 의존성을 만들지 마라
9. 금지 - 관련 없는 API들을 같은 패키지에 배치하지 마라
10. 금지 - API와 SPI(Service Provider Interface)를 같은 패키지에 배치하지 마라
11. 금지 - 이미 릴리스된 공개 API의 패키지를 이동하거나 이름을 변경하지 마라

전체 체크리스트는 여기에서 확인할 수 있습니다:

[Java API Design Checklist](http://theamiableapi.com/2012/01/16/java-api-design-checklist/)
