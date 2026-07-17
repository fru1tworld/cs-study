# 분산 컴퓨팅의 오류들

> 원문: [Fallacies of distributed computing](https://en.wikipedia.org/wiki/Fallacies_of_distributed_computing)
> 저자: Wikipedia 기여자 (CC BY-SA 4.0) | 번역일: 2026-07-10

분산 컴퓨팅의 오류들(fallacies of distributed computing)은 [Sun Microsystems](https://en.wikipedia.org/wiki/Sun_Microsystems)의 [L. 피터 도이치(L. Peter Deutsch)](https://en.wikipedia.org/wiki/L._Peter_Deutsch)와 동료들이 정리한 일련의 명제로, [분산](https://en.wikipedia.org/wiki/Distributed_computing) [애플리케이션](https://en.wikipedia.org/wiki/Application_software)이 처음인 [프로그래머](https://en.wikipedia.org/wiki/Programmer)가 어김없이 하게 되는 잘못된 가정들을 서술한다.

## 오류 목록

원래 목록에 오른 [오류](https://en.wikipedia.org/wiki/Fallacy)들은 다음과 같다.[^1]

1. [네트워크](https://en.wikipedia.org/wiki/Computer_network)는 신뢰할 수 있다.
2. [지연 시간(latency)](https://en.wikipedia.org/wiki/Latency_(engineering))은 0이다.
3. [대역폭](https://en.wikipedia.org/wiki/Throughput)은 무한하다.
4. 네트워크는 [안전하다](https://en.wikipedia.org/wiki/Computer_security).
5. [토폴로지](https://en.wikipedia.org/wiki/Network_topology)는 변하지 않는다.
6. [관리자](https://en.wikipedia.org/wiki/Network_administrator)는 한 명이다.
7. 전송 비용은 0이다.
8. 네트워크는 균질하다.

## 오류가 낳는 결과

1. 소프트웨어 애플리케이션은 네트워크 오류에 대한 에러 처리가 거의 없이 작성된다. 네트워크 장애가 나면 그런 애플리케이션은 멈추거나 응답 패킷을 무한정 기다리면서 메모리나 다른 리소스를 영구히 점유할 수 있다. 장애가 났던 네트워크가 복구되어도, 멈췄던 작업을 재시도하지 못하거나 (수동) 재시작이 필요할 수 있다.
2. 네트워크 지연 시간과 그것이 유발할 수 있는 [패킷 손실](https://en.wikipedia.org/wiki/Packet_loss)을 무시하면, 애플리케이션 계층과 전송 계층 개발자가 트래픽을 무제한으로 허용하게 되어 패킷 드롭이 크게 늘고 대역폭이 낭비된다.
3. 트래픽 송신자가 대역폭 한계를 무시하면 병목이 생길 수 있다.
4. 네트워크 보안에 안일하면, 보안 조치에 끊임없이 적응하는 악의적인 사용자와 프로그램에 허를 찔리게 된다.
5. [네트워크 토폴로지](https://en.wikipedia.org/wiki/Network_topology)의 변화는 대역폭과 지연 시간 문제 모두에 영향을 줄 수 있고, 따라서 비슷한 문제를 일으킬 수 있다.
6. 경쟁 회사들의 [서브넷](https://en.wikipedia.org/wiki/Subnetwork)처럼 관리자가 여럿이면, 서로 충돌하는 정책이 시행될 수 있다. 네트워크 트래픽의 송신자는 원하는 경로를 완주하기 위해 이런 정책을 알고 있어야 한다.
7. 네트워크나 서브넷을 구축하고 유지하는 "숨은" 비용은 무시할 수 없는 수준이므로, 큰 예산 부족을 피하려면 반드시 예산에 반영해야 한다.
8. 시스템이 균질한 네트워크를 가정하면, 앞의 세 가지 오류에서 비롯되는 것과 같은 문제로 이어질 수 있다.

## 역사

이 오류 목록은 [Sun Microsystems](https://en.wikipedia.org/wiki/Sun_Microsystems)에서 시작되었다. 초창기 Sun "[펠로우](https://en.wikipedia.org/wiki/Fellow)" 중 한 명인 [L. 피터 도이치](https://en.wikipedia.org/wiki/L._Peter_Deutsch)가 1994년에 처음으로 일곱 가지 오류 목록을 만들었는데, 여기에는 [빌 조이(Bill Joy)](https://en.wikipedia.org/wiki/Bill_Joy)와 데이브 라이언(Dave Lyon)이 "The Fallacies of Networked Computing"에서 이미 짚어낸 네 가지 오류가 반영되었다.[^2] 1997년 무렵, 역시 Sun 펠로우이자 [Java](https://en.wikipedia.org/wiki/Java_(programming_language))의 창시자인 [제임스 고슬링(James Gosling)](https://en.wikipedia.org/wiki/James_Gosling)이 여덟 번째 오류를 추가했다.[^2]

"Software Engineering Radio"의 한 에피소드에서[^3] 피터 도이치는 아홉 번째 오류를 추가했다. "이건 사실 4번의 확장입니다. 물리적 네트워크의 경계를 넘어서죠. ... 당신이 통신하는 상대는 신뢰할 수 있다."

이후 2020년에 마크 리처즈(Mark Richards)와 닐 포드(Neal Ford)가 분산 시스템의 동시대적 과제를 다루기 위해 세 가지 오류를 추가하며 원래의 "분산 컴퓨팅의 오류들"을 확장했다.[^4]

- 버저닝은 단순하다
- 보상 업데이트(compensating update)는 항상 동작한다
- 관측 가능성(observability)은 선택 사항이다

## 같이 보기

- [CAP 정리](https://en.wikipedia.org/wiki/CAP_theorem)
- [PACELC 설계 원칙](https://en.wikipedia.org/wiki/PACELC_design_principle)
- [분산 컴퓨팅](https://en.wikipedia.org/wiki/Distributed_computing)
- [세밀한 SOA 대 거친 SOA](https://en.wikipedia.org/wiki/Microservices#Criticism_and_concerns)

## 참고 문헌

[^1]: Wilson, Gareth (2015-02-06). ["The Eight Fallacies of Distributed Computing - Tech Talk"](https://web.archive.org/web/20171107014323/http://blog.fogcreek.com/eight-fallacies-of-distributed-computing-tech-talk/). [원본](http://blog.fogcreek.com/eight-fallacies-of-distributed-computing-tech-talk/)에서 2017-11-07에 보존. 2017-06-18에 확인. "여덟 가지 오류는 오래전 Java One 컨퍼런스에서 제임스 고슬링이라는 사람에게 들은 것이다. 그는 이를 피터 도이치라는 사람에게 돌렸고, 기본적으로 Sun의 여러 사람이 함께 이 오류 목록을 만들어냈다고 했다."

[^2]: Van Den Hoogen, Ingrid (2004-01-08). ["Deutsch's Fallacies, 10 Years After"](https://web.archive.org/web/20070811082651/http://java.sys-con.com/read/38665.htm). [원본](http://java.sys-con.com/read/38665.htm)에서 2007-08-11에 보존. 2005-12-03에 확인.

[^3]: [L. Peter Deutsch on the Fallacies of Distributed Computing](https://se-radio.net/2021/07/episode-470-l-peter-deutsch-on-the-fallacies-of-distributed-computing/). 2021-07-27. 해당 발언은 57:10에 나온다.

[^4]: Richards, Mark. Fundamentals of Software Architecture: An Engineering Approach. O'Reilly Media. [ISBN](https://en.wikipedia.org/wiki/ISBN_(identifier)) [978-1492043454](https://en.wikipedia.org/wiki/Special:BookSources/978-1492043454).

## 외부 링크

- Deutsch, Peter. ["The Eight Fallacies of Distributed Computing"](https://nighthacks.com/jag/res/Fallacies.html). nighthacks.com. 2024-07-24에 확인.
- Yoavi, Neli (2008년 1월). ["Fallacies of Distributed Computing Explained"](https://www.researchgate.net/publication/322500050_Fallacies_of_Distributed_Computing_Explained). 2024-07-24에 확인 – [ResearchGate](https://en.wikipedia.org/wiki/ResearchGate) 경유.
