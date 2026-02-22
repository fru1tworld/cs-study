# jOOX에서 Xalan의 확장 함수를 네이티브로 사용하기

> 원문: https://blog.jooq.org/use-xalans-extension-functions-natively-in-joox/

jOOX는 Java의 다소 복잡한 XML API를 다룰 때 사용 편의성을 높이는 것을 목표로 합니다.

이러한 복잡한 API의 한 가지 예가 Xalan입니다. Xalan은 확장 네임스페이스와 같은 유용한 기능을 많이 가지고 있습니다. Xalan을 사용한다면, 다음 문서에서 설명하는 확장 기능에 대해 들어본 적이 있을 것입니다: [http://exslt.org](http://exslt.org)

이러한 확장 기능은 일반적으로 XSLT에서 사용할 수 있습니다. 다음 XML 예제를 살펴보겠습니다:

```xml
<values>
   <value>7</value>
   <value>11</value>
   <value>8</value>
   <value>4</value>
</values>
```

하지만 사실 `math:max`는 Java에서 직접 생성하는 XPath 표현식을 포함하여, 모든 유형의 XPath 표현식에서 사용할 수 있습니다. 다음은 그 장황한 방식입니다:

```java
Document document = // ... DOM 문서

// XPath 객체 생성
XPathFactory factory = XPathFactory.newInstance();
XPath xpath = factory.newXPath();

// XPath 객체에 Xalan 확장 초기화
xpath.setNamespaceContext(
  new org.apache.xalan.extensions.ExtensionNamespaceContext());
xpath.setXPathFunctionResolver(
  new org.apache.xalan.extensions.XPathFunctionResolverImpl());

// 확장 함수를 사용하여 표현식 평가
XPathExpression expression = xpath.compile(
  "//value[number(.) = math:max(//value)]");
NodeList result = (NodeList) expression.evaluate(
  document, XPathConstants.NODESET);

// 결과 반복 처리
for (int i = 0; i < result.getLength(); i++) {
  System.out.println(result.item(i).getTextContent());
}
```

## jOOX가 훨씬 더 편리합니다

jOOX의 xpath 메서드는 이미 Xalan 확장을 지원합니다. 위의 코드를 jOOX로 작성하면 다음과 같습니다:

```java
Document document = // ... DOM 문서

// jOOX의 xpath 메서드는 이미 Xalan 확장을 지원합니다
for (Match value : $(document).xpath(
    "//value[number(.) = math:max(//value)]").each()) {
  System.out.println(value.text());
}
```
