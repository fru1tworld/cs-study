# 인기 있는 ORM은 SQL을 하지 않는다

> 원문: https://blog.jooq.org/popular-orms-dont-do-sql/

지난 15년간 ISO / IEC SQL 표준에서 무슨 일이 있었는지 곰곰이 생각해 봅니다. 우리가 사랑하는 SQL 언어에 꽤 많은 새로운 기능들이 추가되었습니다. 이것을 보세요:

- ISO/IEC SQL:1999 표준에서는 그룹핑 셋(grouping sets)과 (재귀적) 공통 테이블 표현식(common table expressions)을 활용할 수 있게 되었습니다.
- ISO/IEC SQL:2003 표준에서는 매우 정교한 윈도우 함수(window functions)와 MERGE 문을 갖게 되었습니다.
- ISO/IEC SQL:2008 표준에서는 파티션드 JOIN(partitioned JOINs)을 수행할 수 있게 되었습니다.
- ISO/IEC SQL:2011 표준에서는 이제 임시 데이터베이스(temporal databases)와 상호 운용할 수 있습니다(현재까지 IBM DB2와 Oracle에서 구현됨).

그리고 분명히, 거의 읽을 수 없는 1423페이지 분량의 문서에는 훨씬 더 많은 좋은 것들이 숨어 있습니다. 하지만 JPA는... 자, 이런 멋진 기능들 중 어느 것이라도 JPA에 나타났나요? 아니요. 다음 SQL 표준에서 새로운 멋진 기능들이 도입될까요? 분명히 그럴 것입니다! Oracle / CUBRID CONNECT BY 절이나 Oracle / SQL Server PIVOT / UNPIVOT 절들이 표준화의 좋은 후보가 될 것이라고 상상할 수 있습니다. Oracle의 미친 MODEL 절이 포함된다면 저는 정말 열광할 것입니다. 이런 흥미로운 일들이 일어나는 동안, ORM 임피던스 불일치는 더욱 깊어지고 Charles Humble이 QCon에서 최근 발표한 내용을 확인시켜 줄 것입니다. 그는 인기 있는 ORM들의 끊임없이 증가하는 복잡성에 불만족하는 사람들이 점점 더 많아지고 있다는 것을 관찰했습니다. 복잡성의 예: NamedEntityGraph!

```java
@NamedEntityGraph(
    name="ExecutiveProjects"
    attributeNodes={
        @NamedAttributeNode("address"),
        @NamedAttributeNode(
            value="projects",
            subgraph="projects"
        )
    },
    subgraphs={
        @NamedSubgraph(
            name="projects",
            attributeNodes={
                @NamedAttributeNode("properties")
            }
        ),
        @NamedSubgraph(
            name="projects",
            type=LargeProject.class,
            attributeNodes={
                @NamedAttributeNode("executive")
            }
        )
    }
)
```

이런, 이것이 정말로 JPA에 추가되어야 했을까요? Stack Overflow는 한 화면에 그렇게 많은 어노테이션을 표시할 수 없습니다! 글쎄요, 이것이 SQL의 최근 발전에 대한 JEE의 답변이라면, 요즘 JEE를 많이 하지 않아서 다행입니다. 저는 SQL을 합니다. SQL은 자유롭게 놓아두면 멋진 언어입니다.
