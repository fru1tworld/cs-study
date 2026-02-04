# ElSql: Java를 위한 새로운 외부 SQL DSL

> 원문: https://blog.jooq.org/elsql-a-new-external-sql-dsl-for-java/

lambda-dev를 포함한 Java 8 메일링 리스트의 빈번한 기고자인 Stephen Colebourne이 최근 ElSql을 발표했습니다. 이것은 그가 개발해온 Java를 위한 외부 SQL 도메인 특화 언어(Domain-Specific Language)입니다. 이 프로젝트는 GitHub에서 확인할 수 있습니다.

## 구현 예제

ElSql 접근 방식은 특별한 마크업 어노테이션과 함께 SQL을 제시합니다:

```
@NAME(SelectBlogs)
  @PAGING(:paging_offset,:paging_fetch)
    SELECT @INCLUDE(CommonFields)
    FROM blogs
    WHERE id = :id
      @AND(:date)
        date > :date
      @AND(:active)
        active = :active
    ORDER BY title, author
@NAME(CommonFields)
  title, author, content
```

## 주요 관찰 사항

이 구현은 Java 통합을 위한 "훅(hooks)"을 도입하면서도 SQL을 대체로 인식 가능한 상태로 유지합니다. Colebourne의 설계 철학은 jOOQ와 같은 대안들에 비해 단순성을 강조하면서도, 표준 SQL 구문과 밀접하게 유사한 외부 DSL의 유용한 기능들을 보여줍니다.

이 접근 방식은 내부 DSL 구성과 대조되며, 여러 플랫폼에 걸친 DSL 저작을 향한 더 넓은 업계 트렌드를 반영합니다. Eclipse의 Xtext와 Scala의 매크로 시스템은 이 영역에서 병행 발전을 대표하지만, DSL-Java 통합에 관해서는 서로 다른 철학을 가지고 있습니다.

## 참고 자료

추가 세부 사항은 다음을 참조하세요:
- Stephen Colebourne의 ElSql에 관한 블로그 포스트
- OpenGamma의 라이브러리 기술 개요
