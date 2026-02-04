# Java 8 금요일 선물: 람다와 XML

> 원문: https://blog.jooq.org/java-8-friday-goodies-lambdas-and-xml/

Data Geekery에서는 Java 8에 대해 매우 열광하고 있습니다. 이것이 우리의 주요 제품인 jOOQ 쿼리 DSL 주변의 전체 생태계에 큰 영향을 미칠 것이기 때문입니다. 우리는 Java 8의 우수함에 대해 블로그에 글을 올려왔고, 몇 주 전에는 우리의 무료 오픈 소스 라이브러리인 jOOL과 jOOX가 Java 8에서 어떻게 작동하는지에 대한 예시를 제공하면서 몇 가지 흥미로운 Java 8 기능에 대해 자세히 살펴보았습니다.

## Java 8 금요일

매주 금요일마다 여러분에게 새로운 Java 8 기능에 대한 몇 가지 훌륭한 튜토리얼 스타일의 선물을 제공하는 새로운 블로그 시리즈를 시작합니다. 짧고 간결한 튜토리얼 스타일로요. 시작해 봅시다!

## 람다와 XML

XML 친화적인 Java 람다를 어떻게 결합할 수 있을까요? SAX와 DOM은 람다 표현식을 위해 특별히 설계되지 않았습니다. SAX ContentHandler는 일반적으로 람다와 함께 사용하기에는 너무 많은 추상 메서드를 가지고 있고, DOM은 W3C 기반이어서 상당히 장황합니다.

jOOX를 사용하면 그것을 개선할 수 있습니다. jOOX는 표준 W3C DOM을 래핑하고 콘텐츠 탐색 및 조작을 위한 jQuery와 유사한 API를 제공하는 작은 오픈 소스 라이브러리입니다.

다음은 Maven pom.xml 파일에서 모든 Maven 아티팩트 좌표를 추출하는 방법입니다:

```java
$(new File("./pom.xml")).find("groupId")
                        .each(ctx -> {
    System.out.println(
        $(ctx).text() + ":" +
        $(ctx).siblings("artifactId").text() + ":" +
        $(ctx).siblings("version").text());
});
```

(XML 네임스페이스에 대한 자세한 내용은 생략)

위 코드의 출력은 대략 다음과 같습니다:

```
org.jooq:jooq:3.2.0
org.jooq:jooq-codegen:3.2.0
org.jooq:jooq-meta:3.2.0
```

보시다시피, jOOX는 jQuery에서 알려진 편리한 `$()` 표기법을 지원하여 DOM 트리를 탐색하고, 래핑된 요소 집합에 대해 람다 표현식을 실행할 수 있습니다.

## 필터링

이제 모든 SNAPSHOT 아티팩트를 필터링하고 싶습니다. 명시적인 버전이 없는 아티팩트도 필터링됩니다:

```java
$(new File("./pom.xml"))
    .find("groupId")
    .filter(ctx -> $(ctx).siblings("version")
                         .matchText(".*-SNAPSHOT")
                         .isEmpty())
    .each(ctx -> {
        System.out.println(
            $(ctx).text() + ":" +
            $(ctx).siblings("artifactId").text() + ":" +
            $(ctx).siblings("version").text());
    });
```

## 변환

jOOX를 사용하여 XML을 변환할 수도 있습니다. 예를 들어, 각 `groupId` 요소를 결합된 Maven 표기법이 포함된 `artifact` 요소로 바꾸고 싶다면:

```java
$(new File("./pom.xml"))
    .find("groupId")
    .filter(ctx -> $(ctx).siblings("version")
                         .matchText(".*-SNAPSHOT")
                         .isEmpty())
    .content(ctx ->
        $(ctx).text() + ":" +
        $(ctx).siblings("artifactId").text() + ":" +
        $(ctx).siblings("version").text()
    )
    .rename("artifact")
    .each(ctx -> {
        System.out.println(ctx);
    });
```

이것은 다음과 같은 내용을 출력할 것입니다:

```xml
<artifact>org.jooq:jooq:3.2.0</artifact>
<artifact>org.jooq:jooq-codegen:3.2.0</artifact>
<artifact>org.jooq:jooq-meta:3.2.0</artifact>
```

## 핵심 통찰

좋은 점은 람다 전문가 그룹이 모든 SAM(Single Abstract Method, 단일 추상 메서드) 인터페이스를 람다 표현식과 함께 사용할 수 있도록 한 선택이 jOOX처럼 오래 전에 작성된 기존 API에도 큰 가치를 추가한다는 것입니다.

## 결론

이 게시물은 람다 표현식이 XML 처리를 어떻게 더 간결하고 읽기 쉽게 만드는지 보여줍니다. jOOX 라이브러리를 사용하면 전통적인 DOM 조작에 비해 훨씬 더 표현력 있는 코드를 작성할 수 있습니다. Java 8의 람다는 기존 API를 완전히 다시 작성할 필요 없이 함수형 프로그래밍 패턴을 활용할 수 있게 해줍니다.

다음 주에는 java.util.stream API와 함께 사용하는 java.nio에 대해 살펴보겠습니다!
