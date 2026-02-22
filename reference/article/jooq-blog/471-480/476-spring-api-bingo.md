# Spring API 빙고

> 원문: https://blog.jooq.org/spring-api-bingo/

오늘 날짜를 기념하여, 재미있는 게임을 하나 발명했습니다. 바로 Spring API 빙고입니다!

실제 API(예: Spring)와의 유사성은 완전히 우연의 일치입니다.

다음은 이 게임의 핵심입니다:

```java
public class SpringAPIBingo {
    public static void main(String[] args) {
        List<String> terms = Arrays.asList(
            "Abstract", "Adapter", "Adaptor", "Advisor", "Aware",
            "Bean", "Class", "Container", "Data", "Definition",
            "Delegate", "Delegating", "Destination", "Detecting",
            "Disposable", "Entity", "Exception", "Factory", "Handler",
            "Info", "Initializer", "Initializing", "Local", "Loader",
            "Manager", "Mapping", "Persistence", "Post", "Pre",
            "Resolver", "Source", "Target", "Translation", "Translator"
        );

        System.out.println("<table>");
        System.out.println("<tr>");

        for (int i = 0; i < 25; i++) {
            if (i > 0 && i % 5 == 0)
                System.out.println("</tr><tr>");

            System.out.print("<td>");
            Collections.shuffle(terms);
            System.out.print(
                terms.stream()
                     .limit((long)(2 + Math.random() * 4))
                     .collect(Collectors.joining())
            );
            System.out.println("</td>");
        }

        System.out.println("</tr>");
        System.out.println("</table>");
    }
}
```

위 프로그램을 실행하면 아래와 같은 빙고 테이블이 생성됩니다:

| | | | | |
|---|---|---|---|---|
| ClassContainerPost | AdaptorExceptionDefinitionPreMapping | DetectingDelegatingAdaptor | PreMappingDetectingClassAdapter | ExceptionLocal |
| FactoryAdvisorAdapterHandlerLoader | TranslatorLoader | ContainerLocalTranslation | ManagerResolverExceptionBeanAware | InfoPreSourceBeanFactory |
| AdvisorMapping | ContainerPreTranslatorInfoDisposable | DetectingClass | BeanFactoryDestinationResolver | AbstractBeanDefinition |
| ResolverAdaptorTranslatorEntity | TranslatorPostFactory | DefinitionManagerDisposableAbstract | TranslationBean | PersistencePre |
| LocalEntity | PreClassResolver | MappingDelegatingPersistenceAbstractHandler | LocalPersistenceManagerFactoryBean | DisposableBean |

이제 [Spring Framework 4.0 Javadoc의 모든 클래스 목록](http://docs.spring.io/spring/docs/4.0.x/javadoc-api/allclasses-noframe.html)을 열고 실제로 존재하는 클래스들을 체크하세요. 5개가 연속으로 일치하면 빙고입니다!

저의 결과는 다음과 같습니다 (굵은 글씨로 표시):

- BeanFactoryDestinationResolver - 실제 존재하는 클래스
- LocalPersistenceManagerFactoryBean - 실제 존재하는 클래스
- AbstractBeanDefinition - 실제 존재하는 클래스
- DisposableBean - 실제 존재하는 클래스

4개만 맞았네요. 다음에 더 좋은 결과를 기대해봅시다!

참고로, 저는 이미 "Facebook 빙고" 카드를 가지고 있는데, Facebook이 기업을 인수할 때마다 체크하고 있습니다...
