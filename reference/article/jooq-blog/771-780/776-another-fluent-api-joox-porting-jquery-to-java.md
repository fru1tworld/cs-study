# 또 다른 Fluent API: jOOX. jQuery를 Java로 포팅하기

> 원문: https://blog.jooq.org/another-fluent-api-joox-porting-jquery-to-java/

2011년 8월 11일, lukaseder

가끔 Java의 DOM API로 XML을 조작하는 것은 도무지 참을 수 없습니다. 정말 믿을 수 없을 정도로 장황합니다. 이것은 Java에서 SQL을 처리하는 것이 마음에 들지 않았던 것과 매우 유사합니다. SQL의 경우 jOOQ를 만들었습니다. 그래서 저는 "XML에서도 이 문제를 해결할 수 없을까?"라고 생각했습니다.

Stack Overflow에 올려 더 나은 XML 유틸리티를 찾아보았지만, JDOM이나 dom4j 같은 기존 솔루션들은 별로 영감을 주지 못했습니다. 기본 DOM API에 비해 크게 개선되지 않았습니다. 이것이 아이디어를 촉발했습니다: "왜 아무도 jQuery를 Java로 포팅하지 않았을까?"

결과는 jOOX입니다 - "jOO-Star 제품군"의 새로운 라이브러리로, jQuery의 유려하고 직관적인 접근 방식을 Java의 XML 처리에 가져오도록 설계되었습니다. 해킹하고 고통받는 대신에, 우아하게 할 수 있습니다:

```java
joox(document).find("orders")
              .children()
              .eq(4)
              .append("<paid>true</paid>");
```

위 코드에서 볼 수 있듯이, jOOX는 2000명 규모의 인도 결혼식 설거지를 하는 것과 같았던 일을 간단하게 만들어 줍니다. jQuery의 개념적 우아함에서 영감을 받아, jOOX는 표준 W3C DOM 모델을 감싸서 작동합니다. Jerry, jsoup, GWTQuery와 같은 다른 Java XML 라이브러리와 달리, jOOX는 자체 파서를 구현하지 않고 대신 Xerces의 성능 최적화를 활용하면서 jQuery 스타일의 구문을 제공합니다.

물론 프로토타입은 아직 기능이 완전하지 않습니다. 표현 언어 탐색 및 선택자와 같은 더 많은 기능이 아직 구현되지 않았습니다. 그러나 순수한 DOM 탐색과 조작은 실제로 Java에서 감싸기가 그렇게 어렵지 않습니다. 이 프로젝트를 진행하면서 jQuery 개발자들에 대한 새로운 존경심을 갖게 되었습니다.

여러분의 피드백을 환영합니다! jOOX 프로젝트 페이지에서 다운로드할 수 있습니다: https://code.google.com/p/joox/
