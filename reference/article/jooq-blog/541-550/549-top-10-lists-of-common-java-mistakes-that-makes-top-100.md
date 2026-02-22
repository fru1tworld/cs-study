# 흔한 Java 실수 Top 10 리스트 (Top 100을 만드는!)

> 원문: https://blog.jooq.org/top-10-lists-of-common-java-mistakes-that-makes-top-100/

우리는 항상 특정 주제에 대한 Top 10 리스트를 찾고 있습니다. 자, 여기에 여러분을 위한 Java 실수 Top 100이 있습니다:

```sql
SELECT TOP 10 mistake FROM source1
UNION ALL
SELECT TOP 10 mistake FROM source2
UNION ALL
SELECT TOP 10 mistake FROM source3
...
```

저는 의도적으로 Java 초보자들을 위한 Top 10 리스트들은 제외했습니다. 왜냐하면 수천 가지의 초보자 함정들이 있고, 그것들은 대부분 모든 언어에서 공유되기 때문입니다. 저는 *미묘한* 실수들에 집중하고 싶었습니다. 여기 제가 정말 좋아하는 10가지 리스트들이 있습니다:

## 1. ZeroTurnaround의 경험 많은 Java 개발자 & 아키텍트들의 10가지 흔한 함정

JRebel 팀이 최근에 매우 멋진 글을 발표했습니다. 이것이 바로 이 블로그 글에 영감을 준 것입니다. 그들의 촌철살인적이고 괴짜스러운 접근 방식은 정말 읽기 좋습니다.

URL: zeroturnaround.com/rebellabs/watch-out-for-these-10-common-pitfalls-of-experienced-java-developers-architects/

## 2. jOOQ의 Java 코딩 시 10가지 미묘한 모범 사례

네, 저도 이 주제로 글을 썼습니다. 우리 모두 마케팅에 능해야 하니까요, 그렇죠? 이 글은 Java 개발에서 미묘한 코딩 문제들을 다룹니다.

URL: blog.jooq.org/10-subtle-best-practices-when-coding-java/

## 3. AppDynamics의 Top 10 Java 성능 문제

이 eBook을 다운로드하려면 연락처 정보를 제공해야 합니다. 하지만 그만한 가치가 있습니다. 잘 작성된 기술적인 내용을 담고 있습니다.

URL: info.appdynamics.com/Top10JavaPerformanceProblems_eBook.html

## 4. AmiableAPI의 Java API 설계 체크리스트

이것은 깔끔하고 잘 설계된 API를 작성하기 위한 스타일 가이드입니다. 엄밀히 말하면 top 10 형식은 아니지만, 그래도 여기에 포함시킵니다.

URL: theamiableapi.com/2012/01/16/java-api-design-checklist/

## 5. Josh Bloch의 강연: 좋은 API를 설계하는 방법과 그것이 중요한 이유

권위 있는 전문가의 비디오 프레젠테이션입니다. API 설계 기본 원칙을 다룹니다.

URL: youtube.com/watch?v=heh4OeB9A-c

## 6. Pierre-Hugues Charbonneau의 Java EE 엔터프라이즈 성능 문제의 Top 10 원인

이것은 극도로 잘 작성되었습니다. Java 아키텍트들을 대상으로 합니다.

URL: java.dzone.com/articles/top-10-causes-java-ee

## 7. Adam Bien의 Java EE 6에 대한 10가지 흥미로운 발언

이것은 Kai Waehner가 정리한 Adam Bien의 의견들을 요약한 것입니다. 저는 Bien의 관점에 항상 동의하지는 않지만, 그의 블로그는 감사하게 생각합니다.

URL: kai-waehner.de/blog/2010/09/10/10-interesting-statements-of-adam-bien-about-the-java-enterprise-edition-6-jee-6/

## 8. Top 15 최악의 컴퓨터 소프트웨어 실수들

이것은 Java에 국한된 내용보다 더 넓은 범위를 다룹니다. 나쁜 관행의 결과를 보여줍니다.

URL: intertech.com/Blog/15-worst-computer-software-blunders/

## 9. 당신이 알아야 할 Top 10 Java 인물들

Java 커뮤니티에서 영향력 있는 인물들의 목록입니다.

URL: javastoreroom.blogspot.ch/2013/05/top-10-java-people-you-should-know.html

## 10. 최고의 Java 관련 Top 10 리스트들의 Top 10 리스트

네, 바로 이 글입니다. 자기 참조적인 재귀 항목입니다. StackOverflowError에 주의하세요!

URL: blog.jooq.org/top-10-lists-of-common-java-mistakes-that-makes-top-100/

---

## 댓글 섹션

Peter Verhas가 재귀적 특성을 지적했습니다 - 이 리스트가 자기 자신을 포함해야 한다고요. 자기 참조적인 유머를 캐치한 것입니다.

lukaseder (저자)가 이 관찰을 확인하고 그에 따라 업데이트했습니다.

Eric S는 처음의 SQL 쿼리에서 UNION ALL 대신 UNION을 사용하면 중복을 제거할 수 있다고 지적했습니다.

---

*태그: 흔한 실수, java, 실수, 함정, Top 10*
*작성자: lukaseder (jOOQ 창시자)*
*게시일: 2013년 11월 1일*
