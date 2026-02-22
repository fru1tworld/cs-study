# Spring Data JPA DTO 프로젝션을 가져오는 두 번째로 좋은 방법

> 원문: https://blog.jooq.org/the-second-best-way-to-fetch-a-spring-data-jpa-dto-projection/

Vlad Mihalcea가 작성한 "[Spring Data JPA DTO 프로젝션을 가져오는 가장 좋은 방법](https://vladmihalcea.com/the-best-way-to-fetch-a-spring-data-jpa-dto-projection/)"이라는 훌륭한 글을 방금 발견했습니다. 이 글은 [reddit](https://www.reddit.com/)에서도 꽤 많은 관심을 받았습니다. 이것은 매우 훌륭한 사용 사례이자 적절한 해결책이라, 이번에는 jOOQ를 사용하여 동일한 작업을 수행하는 두 번째로 좋은 방법을 빠르게 보여드리고 싶었습니다.

팁: Spring Data JPA와 함께 jOOQ를 쉽게 사용할 수 있습니다. Spring Boot의 jOOQ 스타터를 사용하고, jOOQ에 `DataSource`를 주입한 다음, 모든 리포지토리 쿼리를 jOOQ에 위임하면 됩니다.

해당 글에서 소개된 계층적 DTO 프로젝션으로 바로 넘어가겠습니다. 이 프로젝션은 다음과 같은 타입 계층 구조로 데이터를 투영합니다:

```java
public record PostCommentDTO (
    Long id,
    String review
) {}

public record PostDTO (
    Long id,
    String title,
    List<PostCommentDTO> comments
) {}
```

그래서 `MULTISET` 값 생성자를 사용하여 다음과 같이 jOOQ를 사용할 것입니다:

```java
List<PostDTO> result =
ctx.select(
        POST.ID,
        POST.TITLE,
        multiset(
            select(POST_COMMENT.ID, POST_COMMENT.REVIEW)
            .from(POST_COMMENT)
            .where(POST_COMMENT.POST_ID.eq(POST.ID))
        ).convertFrom(r -> r.map(mapping(PostCommentDTO::new)))
   )
   .from(POST)
   .where(POST.TITLE.like(postTitle))
   .fetch(mapping(PostDTO::new));
```

또는, 그 방식이 더 마음에 드신다면 (그리고 컬렉션을 1단계 이상 중첩하지 않는 경우) `MULTISET_AGG` 집계 함수를 사용할 수도 있습니다:

```java
List<PostDTO> result =
ctx.select(
        POST_COMMENT.post().ID,
        POST_COMMENT.post().TITLE,
        multisetAgg(POST_COMMENT.ID, POST_COMMENT.REVIEW)
            .convertFrom(r -> r.map(mapping(PostCommentDTO::new)))
   .from(POST_COMMENT)
   .where(POST_COMMENT.post().TITLE.like(postTitle))
   .fetch(mapping(PostDTO::new));
```

두 솔루션 모두 임시 레코드 변환(ad-hoc record conversion)을 사용하여 완전한 타입 안전성을 보장합니다. 스키마를 변경하고 코드를 재생성하면, 기존 코드가 더 이상 컴파일되지 않습니다.

쿼리 자체 외에는 추가적인 인프라 로직을 작성할 필요가 없습니다.

멋지지 않나요? :)
