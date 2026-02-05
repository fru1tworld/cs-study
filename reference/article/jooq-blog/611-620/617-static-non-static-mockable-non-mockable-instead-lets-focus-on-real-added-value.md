# 정적, 비정적. 목킹 가능, 목킹 불가능... 대신, 진정한 부가 가치에 집중합시다...

> 원문: https://blog.jooq.org/static-non-static-mockable-non-mockable-instead-lets-focus-on-real-added-value/

테스트 가능성에 대한 끝나지 않는 주제... 정적이냐 비정적이냐에 대한 독단적인 논쟁들. 목킹 가능이냐 목킹 불가능이냐. 테스트 가능이냐 테스트 불가능이냐. 다음은 최근 DZone에 신디케이트된 기사로, 정적인 것을 만드는 것이 얼마나 악한지에 대한 내용입니다:

[http://java.dzone.com/articles/why-static-bad-and-how-avoid](http://java.dzone.com/articles/why-static-bad-and-how-avoid)

기사 자체는 의존성 주입을 통해 목킹 가능하게 만드는 단순한 방법에 여전히 어느 정도 초점을 맞추고 있지만, 수많은 댓글과 불평은 그저 놀라울 뿐입니다. 댓글을 자세히 살펴보면, 성별 중립적인 "she"가 더 나은지 단수형 "they"가 더 나은지에 대한 횡설수설까지 읽게 될 것입니다. 주제에서 벗어난 트롤 경보!

아무도 코드가 테스트 가능해야 한다는 일반적인 유용성을 의심하지 않습니다. 합리적인 노력으로 자동화된 테스트를 추가하는 것이 가능하다면, 정신이 멀쩡한 사람 중 누구도 그런 테스트에 의문을 제기하지 않을 것입니다. 하지만 이 _정적 반대_ 독단은 어디서 오는 걸까요? 모든 프로젝트 관리자는 [80/20 법칙](http://management.about.com/cs/generalmanagement/a/Pareto081202.htm)을 따르는 엔지니어를 좋아할 것입니다. 결국, 좋은 소프트웨어는 모든 이해관계자에게 제공하는 부가 가치로 정의됩니다. 옳고 그름이 없습니다. 대신, _"목킹의 50가지 그림자"_ 가 있습니다. 그리고 약간의 유머와 함께 프로젝트 1일차와 238일차 사이 어딘가의 결과물을 얻게 될 것입니다:

[Reddit 이미지 참조: http://www.reddit.com/r/webdev/comments/1i0vwh/my_reaction_when_someone_offers_to_contribute/]

직시합시다. _정적_ 은 다른 도구들과 마찬가지로 하나의 도구입니다. 장점이 있습니다. 그리고 단점도 있습니다. 적합한 곳에서 도구를 선택하고 필요한 곳에서 지나치게 엄격한 규칙을 검토하세요. 독단적이 되면 결국 실용적인 것보다 더 큰 혼란을 초래할 것입니다. "악"과 싸우기보다는 효율적이 되려고 노력하세요. 목은 통합 테스트와 마찬가지로 각자의 자리가 있습니다.

더 많은 불평과 트롤링 댓글을 찾고 있는 분들을 위해, 데이터베이스 맥락에서 더 많은 목킹을 권장하는 이 기사에서 볼 수 있습니다:
[http://architects.dzone.com/articles/easy-mocking-your-database-0](http://architects.dzone.com/articles/easy-mocking-your-database-0)

그리고 그 후에. 다시 일로 돌아가서 가치를 추가하는 것에 집중하는 무언가를 만들어 봅시다!
