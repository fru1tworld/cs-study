# Apache Xalan의 XPath 프로세서를 10배 빠르게 하는 방법

> 원문: https://blog.jooq.org/how-to-speed-up-apache-xalans-xpath-processor-by-factor-10x/

2013년 8월 25일 Lukas Eder 작성

## 핵심 문제

Apache Xalan에는 심각한 성능 버그가 있습니다(XALANJ-2540). 이 버그로 인해 XPath 표현식 평가마다 내부 SPI 설정 파일이 수천 번씩 로드됩니다.

## 성능 비교

단순한 DOM 요소 접근으로 문제를 확인할 수 있습니다:

```java
Element e = (Element)
  document.getElementsByTagName("SomeElementName")
          .item(0);
String result = ((Element) e).getTextContent();
```

이 방식은 XPath를 사용하는 것보다 약 100배 빠릅니다:

```java
// 전체 시간의 30%를 차지하며, 캐시할 수 있음
XPathFactory factory = XPathFactory.newInstance();

// 무시할 수 있는 수준
XPath xpath = factory.newXPath();

// 무시할 수 있는 수준
XPathExpression expression =
  xpath.compile("//SomeElementName");

// 전체 시간의 70%를 차지함
String result = (String) expression
  .evaluate(document, XPathConstants.STRING);
```

## 근본 원인

성능 저하는 모든 XPath 평가가 클래스로더를 통해 기본 설정으로 `DTMManager` 인스턴스를 조회하도록 트리거하기 때문에 발생합니다. 이 접근은 `ObjectFactory.class`에 대한 락으로 보호됩니다. 접근이 실패하면(기본적으로 실패함), `xalan.jar` 파일 내의 `META-INF/service/org.apache.xml.dtm.DTMManager`에서 설정을 로드합니다 - 매번 말입니다.

## 해결책

JVM 파라미터를 사용하여 동작을 오버라이드합니다:

```
-Dorg.apache.xml.dtm.DTMManager=
  org.apache.xml.dtm.ref.DTMManagerDefault
```

또는:

```
-Dcom.sun.org.apache.xml.internal.dtm.DTMManager=
  com.sun.org.apache.xml.internal.dtm.ref.DTMManagerDefault
```

## 작동 원리

시스템 프로퍼티를 설정하면 `lookUpFactoryClassName()` 메서드에서 조기에 반환할 수 있습니다:

```java
// c.s.o.a.xml.internal.dtm.ObjectFactory의 코드
static String lookUpFactoryClassName(
       String factoryId,
       String propertiesFilename,
       String fallbackClassName) {
  SecuritySupport ss = SecuritySupport
    .getInstance();

  try {
    String systemProp = ss
      .getSystemProperty(factoryId);
    if (systemProp != null) {
      // 메서드에서 조기 반환
      return systemProp;
    }
  } catch (SecurityException se) {
  }
  // [..] "무거운" 작업들이 이후에 발생함
}
```

## 핵심 요점

이 우회 방법은 기본 팩토리 클래스 이름을 미리 지정함으로써 비용이 많이 드는 작업을 우회합니다. 이를 통해 시스템 프로퍼티 조회가 설정 파일에 반복적으로 접근하는 대신 즉시 반환할 수 있게 됩니다.
