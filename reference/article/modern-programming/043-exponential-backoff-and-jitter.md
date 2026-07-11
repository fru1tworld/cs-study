# 지수 백오프와 지터

> 원문: [Exponential Backoff And Jitter](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/)
> 저자: Marc Brooker (AWS Architecture Blog, 2015-03-04) | 번역일: 2026-07-10

업데이트(2023년 5월): 8년이 지난 지금도 이 해법은 Amazon이 회복력 있는 시스템을 위한 원격 클라이언트 라이브러리를 만드는 방식의 기둥 역할을 하고 있다. 이제 대부분의 AWS SDK는 standard 또는 adaptive 모드를 쓸 때 재시도 동작의 일부로 지수 백오프와 지터를 지원한다. 따라서 AWS 서비스에 보내는 AWS SDK 요청에 직접 로직을 짜 넣지 않고도 이 패턴을 활용할 수 있다. 이 기능을 지원하는 AWS SDK 목록은 AWS SDK 문서에서 확인할 수 있다.

타임아웃, 재시도, 지터를 더한 백오프에 대해서는 Amazon Builders' Library에서 더 깊이 살펴볼 수 있다.

# OCC 소개

낙관적 동시성 제어(Optimistic concurrency control, OCC)는 여러 작성자가 쓰기를 잃지 않으면서 하나의 객체를 안전하게 수정하는, 오래도록 검증된 방법이다. OCC에는 세 가지 좋은 성질이 있다. 기반 저장소가 가용한 한 항상 진행을 보장하고, 이해하기 쉽고, 구현하기 쉽다. DynamoDB의 조건부 쓰기 덕분에 OCC는 DynamoDB 사용자에게 자연스러운 선택이며, DynamoDBMapper 클라이언트가 기본으로 지원한다.

OCC는 진행을 보장하지만, 경합이 심하면 성능이 상당히 나빠질 수 있다. 이런 경합 상황 중 가장 단순한 경우는, 아주 많은 클라이언트가 동시에 시작해서 같은 데이터베이스 행을 갱신하려는 것이다. 매 라운드마다 정확히 한 클라이언트만 성공이 보장되므로, 모든 갱신을 마치는 데 걸리는 시간은 경합에 비례해 선형으로 늘어난다.

이 글의 그래프들을 위해, 나는 지연(그리고 지연의 분산)이 있는 네트워크에서 원격 데이터베이스를 상대로 한 OCC의 동작을 작은 시뮬레이터로 모델링했다. 이 시뮬레이션에서 네트워크는 평균 10ms, 분산 4ms의 지연을 넣는다. 첫 번째 시뮬레이션은 완료 시간이 경합에 비례해 선형으로 늘어나는 모습을 보여준다. 매 라운드에 한 클라이언트만 성공하므로, N개의 클라이언트가 모두 성공하려면 N 라운드가 걸리기 때문이다.

![](https://d2908q01vomqb2.cloudfront.net/fc074d501302eb2b93e2554793fcaf50b3bf7291/2017/10/03/exponential-backoff-and-jitter-blog-figure-1.png)

안타깝게도 이게 전부가 아니다. N개의 클라이언트가 경합하면, 시스템이 수행하는 전체 작업량은 N²에 비례해 늘어난다.

![](https://d2908q01vomqb2.cloudfront.net/fc074d501302eb2b93e2554793fcaf50b3bf7291/2017/10/03/exponential-backoff-and-jitter-blog-figure-2.png)

# 백오프 추가하기

여기서 문제는 첫 라운드에 N개의 클라이언트가 경합하고, 두 번째 라운드에 N-1개가 경합하는 식으로 이어진다는 점이다. 매 라운드마다 모든 클라이언트가 경합하는 것은 낭비다. 클라이언트를 느리게 만들면 도움이 될 수 있는데, 클라이언트를 느리게 만드는 고전적인 방법이 상한이 있는 지수 백오프(capped exponential backoff)다. 상한이 있는 지수 백오프란, 클라이언트가 매 시도 후 백오프에 상수를 곱하되 어떤 최댓값까지만 늘리는 것이다. 우리 경우, 클라이언트는 시도가 실패할 때마다 다음 시간만큼 잠든다.

![](https://d2908q01vomqb2.cloudfront.net/fc074d501302eb2b93e2554793fcaf50b3bf7291/2017/10/03/exponential-backoff-and-jitter-blog-figure-3.png)

시뮬레이션을 다시 돌려보면 백오프가 약간 도움이 되긴 하지만 문제를 해결하지는 못한다는 것을 알 수 있다. 클라이언트 작업량은 조금 줄었을 뿐이다.

![](https://d2908q01vomqb2.cloudfront.net/fc074d501302eb2b93e2554793fcaf50b3bf7291/2017/10/03/exponential-backoff-and-jitter-blog-figure-4.png)

문제를 가장 잘 보는 방법은 지수 백오프된 호출들이 일어나는 시각을 살펴보는 것이다.

![](https://d2908q01vomqb2.cloudfront.net/fc074d501302eb2b93e2554793fcaf50b3bf7291/2017/10/03/exponential-backoff-and-jitter-blog-figure-5.png)

호출이 점점 드물게 일어난다는 점에서 지수 백오프가 작동하고 있는 것은 분명하다. 문제도 눈에 확 띈다. 여전히 호출이 뭉쳐 있는 것이다. 매 라운드에 경합하는 클라이언트 수를 줄인 게 아니라, 어떤 클라이언트도 경합하지 않는 시간대를 만들어 넣었을 뿐이다. 네트워크 지연의 자연스러운 분산이 어느 정도 퍼뜨려 주기는 했지만, 경합 자체는 별로 줄지 않았다.

# 지터 추가하기

해법은 백오프를 없애는 것이 아니다. 지터를 더하는 것이다. 처음에는 지터가 직관에 반하는 아이디어로 보일 수 있다. 무작위성을 더해서 시스템 성능을 개선하겠다는 것이니까. 하지만 위의 시계열이 지터의 근거를 훌륭하게 보여준다. 우리는 저 스파이크들을 대략 일정한 속도로 펴고 싶은 것이다. 지터를 더하는 것은 sleep 함수의 작은 수정이다.

![](https://d2908q01vomqb2.cloudfront.net/fc074d501302eb2b93e2554793fcaf50b3bf7291/2017/10/03/exponential-backoff-and-jitter-blog-figure-6.png)

![](https://d2908q01vomqb2.cloudfront.net/fc074d501302eb2b93e2554793fcaf50b3bf7291/2017/10/03/exponential-backoff-and-jitter-blog-figure-7.png)

이 시계열은 훨씬 좋아 보인다. 공백이 사라졌고, 초반의 스파이크 이후에는 호출 속도가 거의 일정하다. 전체 호출 수에도 큰 효과가 있었다.

![](https://d2908q01vomqb2.cloudfront.net/fc074d501302eb2b93e2554793fcaf50b3bf7291/2017/10/03/exponential-backoff-and-jitter-blog-figure-8.png)

100개의 클라이언트가 경합하는 경우, 호출 수를 절반 이상 줄였다. 지터 없는 지수 백오프와 비교하면 완료까지 걸리는 시간도 크게 개선됐다.

![](https://d2908q01vomqb2.cloudfront.net/fc074d501302eb2b93e2554793fcaf50b3bf7291/2017/10/03/exponential-backoff-and-jitter-blog-figure-9.png)

이런 시간 기반 백오프 루프를 구현하는 방법은 몇 가지가 있다. 위의 알고리즘을 "Full Jitter"라고 부르고, 두 가지 대안을 살펴보자. 첫 번째 대안은 "Equal Jitter"로, 백오프의 일부를 항상 유지하고 더 작은 폭으로만 지터를 준다.

![](https://d2908q01vomqb2.cloudfront.net/fc074d501302eb2b93e2554793fcaf50b3bf7291/2017/10/03/exponential-backoff-and-jitter-blog-figure-10.png)

이 방식의 직관은 아주 짧은 sleep을 막고, 백오프가 주는 감속 효과를 항상 일부 유지한다는 것이다. 두 번째 대안은 "Decorrelated Jitter"로, "Full Jitter"와 비슷하지만 직전의 무작위 값을 기준으로 지터의 최댓값도 함께 늘린다.

![](https://d2908q01vomqb2.cloudfront.net/fc074d501302eb2b93e2554793fcaf50b3bf7291/2017/10/03/exponential-backoff-and-jitter-blog-figure-11.png)

어느 접근이 가장 좋을 것 같은가?

클라이언트 작업량을 보면, "Full" 지터와 "Equal" 지터의 호출 수는 거의 같고 "Decorrelated"는 더 많다. 셋 모두 지터 없는 두 접근에 비하면 작업량을 크게 줄인다.

![](https://d2908q01vomqb2.cloudfront.net/fc074d501302eb2b93e2554793fcaf50b3bf7291/2017/10/03/exponential-backoff-and-jitter-blog-figure-12.png)

지터 없는 지수 백오프는 명백한 패배자다. 작업량이 더 많을 뿐 아니라 지터를 준 접근들보다 시간도 더 오래 걸린다. 사실 시간이 너무 오래 걸려서, 나머지 방법들을 제대로 비교하려면 그래프에서 빼야 할 정도다.

![](https://d2908q01vomqb2.cloudfront.net/fc074d501302eb2b93e2554793fcaf50b3bf7291/2017/10/03/exponential-backoff-and-jitter-blog-figure-13.png)

지터를 준 접근들 중에서는 "Equal Jitter"가 패배자다. "Full Jitter"보다 작업이 약간 더 많고 시간은 훨씬 오래 걸린다. "Decorrelated Jitter"와 "Full Jitter" 사이의 선택은 덜 분명하다. "Full Jitter"는 작업량이 더 적지만 시간은 약간 더 걸린다. 그래도 두 접근 모두 클라이언트 작업과 서버 부하를 크게 줄여준다.

이 접근들 중 어느 것도 해야 할 작업의 N² 성질을 근본적으로 바꾸지는 못하지만, 합리적인 수준의 경합에서는 작업을 크게 줄여준다는 점을 짚어둘 만하다. 지터를 준 백오프는 구현 복잡도 대비 수익이 어마어마하며, 원격 클라이언트의 표준 접근법으로 삼아야 한다.

이 글의 모든 그래프와 수치는 OCC 동작의 간단한 시뮬레이션으로 생성했다. 시뮬레이터 코드는 GitHub의 aws-arch-backoff-simulator 프로젝트에서 받을 수 있다.

## 저자 소개

![Marc Brooker](https://d2908q01vomqb2.cloudfront.net/fc074d501302eb2b93e2554793fcaf50b3bf7291/2019/09/19/marc-brooker.png)

Marc Brooker는 Amazon Web Services의 VP Technology of Software Development다. 2008년부터 AWS에서 EC2, EBS, IoT 등 여러 서비스에 참여했다. 현재는 스케일링과 가상화 작업을 포함해 AWS Lambda에 집중하고 있다. COE와 포스트모템 읽기를 무척 즐긴다. 전기공학 박사 학위를 갖고 있다.
