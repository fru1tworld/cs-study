# jOOX와 XSLT. XML 러브 스토리, 계속

> 원문: https://blog.jooq.org/joox-and-xslt-an-xml-love-story-continued/

jOOX의 [XML](https://en.wikipedia.org/wiki/XML) 조작에 관련된 다소 함수적인 사고방식은 단순히 XSLT를 지원하는 추가적인 API 향상을 간절히 요구합니다. [XSL 변환](https://en.wikipedia.org/wiki/XSLT)은 일반적인 DOM 조작(또는 jOOX 조작)이 너무 지루해지는 경우, 대량의 XML을 다른 구조로 변환하는 꽤 표준적인 방법이 되었습니다. 표준 Java에서 이러한 작업이 어떻게 수행되는지 살펴보겠습니다.

### 입력 예제:

```xml
<books>
  <book id="1"/>
  <book id="2"/>
</books>
```

### XSL 예제:

```xml
<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!-- 모든 book을 매칭하고 ID를 증가시킨다 -->
    <xsl:template match="book">
        <book id="{@id + 1}">
            <xsl:apply-templates/>
        </book>
    </xsl:template>

    <!-- 다른 모든 요소와 속성은 항등 변환한다 -->
    <xsl:template match="@*|*">
        <xsl:copy>
            <xsl:apply-templates select="*|@*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
```

### Java에서 XSL 변환의 장황함

Java에서 XSL 변환을 수행하는 표준적인 방법은 꽤 장황합니다 - 표준 Java에서 XML과 관련된 거의 모든 것이 그렇듯이 말입니다. 위의 변환을 적용하는 예제를 살펴보세요:

```java
Source source = new StreamSource(new File("increment.xsl"));
TransformerFactory factory = TransformerFactory.newInstance();
Transformer transformer = factory.newTransformer(source);
DOMResult result = new DOMResult();
transformer.transform(new DOMSource(document), result);

Node output = result.getNode();
```

### jOOX로 장황함을 획기적으로 줄이기

jOOX를 사용하면 훨씬 적은 코드로 정확히 동일한 작업을 수행할 수 있습니다:

```java
// document 요소에 변환을 적용한다:
$(document).transform("increment.xsl");

// 모든 book 요소에 변환을 적용한다:
$(document).find("book").transform("increment.xsl");
```

### 두 경우 모두 결과는 다음과 같습니다:

```xml
<books>
  <book id="2"/>
  <book id="3"/>
</books>
```
