# jOOX가 많은 Stack Overflow 질문에 답하다

> 원문: https://blog.jooq.org/joox-answers-many-stack-overflow-questions/

Stack Overflow에서 XML, DOM, XPath, JAXB 등에 관한 질문들을 검색해보면, jOOX를 사용한 예제 하나만으로도 간단하게 답할 수 있는 경우가 매우 많습니다.

예를 들어 다음 질문을 살펴보겠습니다:

### 목표

다음과 같은 XML 문서가 있을 때:

```xml
<root>
    <elemA>one</elemA>
    <elemA attribute1='first' attribute2='second'>two</elemA>
    <elemB>three</elemB>
    <elemA>four</elemA>
    <elemC>
        <elemB>five</elemB>
    </elemC>
</root>
```

### jOOX의 답변

위 문제는 jOOX를 사용하면 (XSLT, SAX, DOM 등을 활용한 다른 답변들에 비해) 상당히 간단하게 해결할 수 있습니다:

```java
List<String> list = $(document).xpath("//*[not(*)]").map(new Mapper<String>() {
  public String map(Context context) {
    return $(context).xpath() + "='" + $(context).text() + "'";
  }
});
```

이 코드는 다음과 같은 결과를 출력합니다:

```
/root[1]/elemA[1]='one'
/root[1]/elemA[2]='two'
/root[1]/elemB[1]='three'
/root[1]/elemA[3]='four'
/root[1]/elemC[1]/elemB[1]='five'
```

이것은 원래 질문자의 문제에 대한 "거의 완벽한" 해결책입니다. jOOX는 아직 속성(attribute)의 매칭/매핑을 지원하지 않습니다. 따라서 속성은 어떤 출력도 생성하지 않습니다. 하지만 이 기능은 가까운 시일 내에 구현될 예정입니다.

전체 답변과 질문은 여기에서 확인하세요: https://stackoverflow.com/a/8943144/521799
