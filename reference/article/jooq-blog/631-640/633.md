# jOOX 첫 사용 경험 기사

> 원문: https://blog.jooq.org/a-joox-first-time-experience-article/

jOOX에 대한 첫 사용자 경험 기사가 있어 소개합니다: http://www.kubrynski.com/2013/03/as-developer-i-want-to-use-xml.html

## jOOX 소개

해당 기사에서는 jOOX를 다음과 같이 정의합니다:

> "jOOX는 Java Object Oriented XML의 약자입니다. 이것은 org.w3c.dom 패키지를 위한 간단한 래퍼로, DOM이 필요하지만 너무 장황한 경우에 유연한 XML 문서 생성과 조작을 가능하게 합니다."

이 도구는 DOM을 대체하기보다는 사용성을 향상시키기 위해 기반 문서를 래핑합니다. jQuery를 에뮬레이트하는 유사한 도구들(jsoup, jerry, gwtquery 등)과 달리, jOOX는 Xerces의 고성능 구현과 함께 표준 W3C DOM을 활용합니다.

## 코드 예제

기사에서는 jOOX 사용법을 보여주는 샘플 구문을 제공합니다:

```java
// 인덱스 4의 주문을 찾아서
// "paid" 요소를 추가
$(document).find("orders")
           .children().eq(4)
           .append("<paid>true</paid>");

// 결제 완료된 주문을 찾아서
// "settled"로 표시
$(document).find("orders")
           .children().find("paid")
           .after("<settled>true</settled>");
```
