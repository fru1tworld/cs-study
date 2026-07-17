# 플레임 그래프(Flame Graphs)

> 원문: [Flame Graphs](https://www.brendangregg.com/flamegraphs.html)
> 저자: Brendan Gregg | 정리일: 2026-07-10
>
> 원문 사이트에 별도의 허용적 라이선스가 없어, 원문의 섹션 구조를 빠짐없이 따라가는 상세 정리 노트로 작성했다. 수치·링크·용어는 원문 그대로다.

![CPU Flame Graph 예시](https://www.brendangregg.com/FlameGraphs/cpu-mysql-crop-500.png)

플레임 그래프는 계층적 데이터를 시각화하는 방법으로, Brendan Gregg가 프로파일링된 소프트웨어의 스택 트레이스를 시각화해 가장 빈번한 코드 경로를 빠르고 정확하게 식별하기 위해 만들었다. 이 페이지는 플레임 그래프의 공식 사이트다. [github.com/brendangregg/FlameGraph](https://github.com/brendangregg/FlameGraph)의 오픈소스 프로그램으로 인터랙티브 SVG를 생성할 수 있다. 지금은 100개가 넘는 다른 구현체가 있으며 그중 다수가 오픈소스이고(아래 "업데이트" 절 참고), 대부분의 상용 프로파일러에서도 지원한다. Netflix 성능 엔지니어링 팀의 동료 Martin Spier는 더 인터랙티브한 d3 버전인 [d3-flame-graph](https://github.com/spiermar/d3-flame-graph)를 만들었으며 이 역시 널리 쓰인다.

원문 사이트는 플레임 그래프의 여러 종류를 각각 별도 페이지(또는 포스트)로 소개한다.

- [CPU](https://www.brendangregg.com/FlameGraphs/cpuflamegraphs.html)
- [AI/GPU](https://www.brendangregg.com/blog/2024-10-29/ai-flame-graphs.html)
- [Memory](https://www.brendangregg.com/FlameGraphs/memoryflamegraphs.html)
- [Off-CPU](https://www.brendangregg.com/FlameGraphs/offcpuflamegraphs.html)
- [Hot/Cold](https://www.brendangregg.com/FlameGraphs/hotcoldflamegraphs.html)
- [Differential](https://www.brendangregg.com/blog/2014-11-09/differential-flame-graphs.html)

위 예시 이미지는 CPU 플레임 그래프의 일부로, MySQL의 어떤 코드 경로가 얼마나 많은 CPU 사이클을 소비하는지 보여준다.

플레임 그래프는 계층 구조를 가진 데이터라면 무엇에든 쓸 수 있다. 예를 들어 파일 시스템 콘텐츠도 가능하다([방법](https://www.brendangregg.com/blog/2017-02-05/file-system-flame-graph.html), [트리맵·선버스트와의 비교](https://www.brendangregg.com/blog/2017-02-06/flamegraphs-vs-treemaps-vs-sunburst.html)). 변동·교란·주기적 활동을 연구하기 위해 커스텀 시간 범위별로 플레임 그래프를 생성하려면 [FlameScope](https://www.brendangregg.com/flamescope.html)를 참고하라.

## 요약

x축은 스택 프로파일 표본의 모집단을 알파벳순으로 정렬해 보여준다(시간의 흐름이 아니다). y축은 스택 깊이를 아래에서 0부터 세어 나타낸다. 각 사각형은 하나의 스택 프레임을 나타내며, 프레임이 넓을수록 스택에 더 자주 등장했다는 뜻이다. 맨 위 가장자리는 현재 CPU에서 실행 중인 것을 보여주고, 그 아래는 그 프레임의 조상(호출자) 체인이다. 원조 플레임 그래프는 인접한 프레임을 시각적으로 구분하기 쉽도록 무작위 색을 쓴다. y축을 뒤집는 변형("icicle graph"), 코드 종류를 나타내도록 색조를 바꾸는 변형, 추가 차원을 전달하기 위해 색 스펙트럼을 쓰는 변형 등이 있다.

플레임 그래프는 정적 시각화이자 동적 시각화다. 정적 시각화로서는 이미지로 저장해 인쇄물(책)에 실을 수 있으며, 가장 빈번한 프레임만 라벨을 붙일 만큼 넓기 때문에 이미지만으로도 "큰 그림"이 전달된다. 동적 시각화로서는 탐색과 이해를 돕는 인터랙티브 기능을 제공한다.

- 마우스를 올리면 상태 바에 프레임의 추가 정보가 표시된다.
- 마우스로 클릭하면 시각화가 수평으로 확대(zoom)되어, 가려져 있던 함수 이름이 드러난다.
- 검색하면 주어진 검색어와 일치하는 프레임을 강조하고, 검색어를 포함한 스택의 "누적 백분율"을 보여준다.

이 시각화는 저자의 ACMQ 아티클 [The Flame Graph](http://queue.acm.org/detail.cfm?id=2927301)에서 자세히 설명한다. 이 글은 [Communications of the ACM, Vol. 59 No. 6](http://cacm.acm.org/magazines/2016/6/202665-the-flame-graph/abstract)에도 게재되었다.

[CPU Flame Graphs](https://www.brendangregg.com/FlameGraphs/cpuflamegraphs.html) 페이지와 아래 "발표" 절도 참고하라.

## 발표

저자는 USENIX ATC 2017에서 플레임 그래프를 설명하는 갱신된 발표 [Visualizing Performance with Flame Graphs](https://www.usenix.org/conference/atc17/program/presentation/gregg-flame)를 했다. 이 발표는 [유튜브](https://www.youtube.com/watch?v=D53T1Ejig1Q)와 [슬라이드셰어](https://www.slideshare.net/brendangregg/usenix-atc-2017-visualizing-performance-with-flame-graphs)([PDF](https://www.brendangregg.com/Slides/USENIX_ATC2017_flamegraphs.pdf))에서 볼 수 있다.

플레임 그래프에 관한 첫 발표는 [USENIX LISA 2013](https://www.usenix.org/conference/lisa13/technical-sessions/plenary/gregg)이었고, 결국 본회의(plenary) 발표가 되었다([유튜브](http://www.youtube.com/watch?v=nZfNehCzGdw), [슬라이드셰어](http://www.slideshare.net/brendangregg/blazing-performance-with-flame-graphs), [PDF](https://www.brendangregg.com/Slides/LISA13_Flame_Graphs.pdf)).

## 운영체제

이제 일부 OS 프로파일러는 플레임 그래프를 내장 지원한다.

- Linux: perf (`perf script report flamegraph`)
- Windows: WPA, PerfView

스택 트레이스가 담긴 프로파일 데이터라면 어디서든 플레임 그래프를 생성할 수 있다. 다음 프로파일링 도구가 그 예다.

- Linux: perf, eBPF, SystemTap, ktap
- FreeBSD: DTrace
- Mac OS X: Instruments
- Windows: Xperf.exe

의미 있는 스택을 생성할 수 있는 프로파일러만 있으면 이를 플레임 그래프로 변환하는 것은 보통 쉬운 단계다.

플레임 그래프를 지원하는 프로파일링 제품과 회사도 이제 많다. 자세한 내용은 아래 "업데이트" 절을 참고하라.

## 변형

**Icicle chart**는 플레임 그래프를 위아래로 뒤집은 것이다. 이 방식을 선호하는 사람도 있다. 저자의 `flamegraph.pl`은 `--inverted` 옵션으로 이를 생성한다. 저자는 y축이 아래쪽 0부터 위로 스택 깊이를 세는 표준 "flame" 레이아웃을 선호하며, 위에서 아래로 훑으며 평평한 영역(plateau)을 찾는 데 익숙하다. 다만 매우 깊은 스택에서는 (맨 위부터 시작하는 GUI에서) 플레임 그래프 레이아웃의 초기 화면이 대부분 비어 있을 수 있어(가느다란 인터럽트 스택 몇 개뿐), 개발자가 프로파일의 대부분을 보려면 아래로 스크롤해야 하는 문제가 있다. root-to-leaf 순으로 읽는 것을 선호하는 개발자에게는 icicle 레이아웃이 스크롤 없이 항상 시작점을 화면에 보여준다는 장점이 있다. 이런 이유로 많은 플레임 그래프 구현체가 기본값으로 icicle 레이아웃을 쓴다. 다른 구현체는 flame 레이아웃을 쓰되 루트 프레임이 화면에 보이도록 아래쪽부터 표시하기 시작한다. 저자는 이에 대해 강한 의견은 없으며, 선호하는 쪽을 쓰면 된다고 말한다. 가능하면 최종 사용자가 원하는 레이아웃을 고를 수 있는 토글을 포함하는 것이 좋다.

**Flame chart**는 구글 크롬의 WebKit Web Inspector가 처음 추가했다([버그 트래커](https://bugs.webkit.org/show_bug.cgi?id=111162)). 플레임 그래프에서 영감을 받았지만, flame chart는 x축에 알파벳 대신 시간의 흐름을 놓는다. 이 덕분에 시간 기반 패턴을 연구할 수 있다. 플레임 그래프는 x축 샘플을 알파벳순으로 재정렬해 프레임 병합을 최대화하고, 프로파일의 큰 그림을 더 잘 보여준다. 멀티스레드 애플리케이션은 단일 flame chart로는 제대로 표시할 수 없지만 플레임 그래프로는 가능하다(flame chart는 원래 단일 스레드 자바스크립트 분석용으로 쓰였기에 이 문제를 다룰 필요가 없었다). 두 시각화 모두 유용하며, 가능하면 도구가 둘 다 제공해야 한다(예: TraceCompass는 실제로 둘 다 지원한다). 일부 분석 도구는 flame chart를 구현해 놓고 플레임 그래프라고 잘못 부르기도 한다.

**Sunburst 레이아웃**은 x축에 방사형(radial) 좌표를 써서 플레임 그래프를 계층적 파이 차트로 바꾼 것이다. 구글 Web Inspector 팀이 이를 [프로토타입](https://bugs.chromium.org/p/chromium/issues/detail?id=452624)으로 만들었다. 저자도 플레임 그래프와의 비교를 [별도 포스트](https://www.brendangregg.com/blog/2017-02-06/flamegraphs-vs-treemaps-vs-sunburst.html)에서 다뤘다.

## 기원

저자는 MySQL 성능 문제를 다루다가 CPU 사용량을 빠르고 깊이 있게 파악해야 했을 때 플레임 그래프를 발명했다. 기존 프로파일러/트레이서는 텍스트 벽만 만들어 냈기에 시각화를 탐색하기 시작했다. 처음에는 CPU 함수 호출을 트레이싱한 뒤 Neelakanth Nadgir의 콜스택 시간순 시각화 기법으로 그렸는데, 이 기법 자체는 Roch Bourbonnais의 CallStackAnalyzer와 Jan Boerhout의 vftrace에서 영감을 받은 것이었다. 이들은 플레임 그래프와 비슷해 보이지만 x축에 시간의 흐름을 둔다. 여기에는 두 가지 문제가 있었다. 함수 트레이싱의 오버헤드가 너무 커서 대상 시스템을 교란시켰고, 여러 초에 걸친 데이터를 표시하면 최종 시각화가 너무 밀도가 높아 읽기 어려웠다.

저자는 오버헤드 문제를 해결하기 위해 타임드 샘플링(프로파일링)으로 전환했다. 다만 샘플링은 사이에 공백이 있어 함수 흐름을 더 이상 알 수 없으므로, x축에서 시간을 버리고 프레임 병합을 최대화하도록 샘플을 재정렬했다. 이 방법이 통했고 최종 시각화는 훨씬 읽기 쉬워졌다. Neelakanth와 Roch의 시각화는 프레임을 구분하기 위해 완전히 무작위인 색을 썼다. 저자는 색 팔레트를 좁히는 편이 더 보기 좋다고 생각했고, 처음에는 CPU가 왜 "뜨거운(hot, 즉 바쁜)" 상태인지 설명하기 위해 따뜻한(warm) 색만 골랐다. 불꽃을 닮았다는 이유로 빠르게 "플레임 그래프"로 알려지게 되었다.

저자는 플레임 그래프로 이어진 원래 성능 문제를 위의 ACMQ/CACM 아티클에서 더 자세히 설명한다. 플레임 그래프 시각화는 사실 프로파일링된 스택 트레이스를 시각화하기 위해 저자가 사용한, 뒤집힌 icicle 레이아웃을 가진 인접(adjacency) 다이어그램이다.

## 업데이트

플레임 그래프는 2011년 12월에 공개되었다. 원문은 그 이후 있었던 소식을 연도별로 정리해 나열한다. 주요 흐름은 다음과 같다.

- **초기(2012년경)**: Alan Coopersmith가 X server의 플레임 그래프를 생성했고, Dave Pacheco가 node.js 함수용으로 만들었으며(이후 npm 패키지 stackvis로 발전), Max Bruning이 IP 스케일링 문제 해결에 활용했다. Zoltan Farkas는 플레임 그래프에서 영감을 받아 Java용 Spf4j를 만들었다.
- **2013년**: Linux perf/SystemTap 기반 커널 플레임 그래프 문서화, Mac OS X Instruments용 플레임 그래프(Mark Probst), Ruby MiniProfiler의 플레임 그래프(Sam Saffron), Windows Xperf 요약(Bruce Dawson), 구글 크롬의 Flame Charts 도입, Perl Devel::NYTProf에 플레임 그래프 통합(Tim Bunce) 등이 이어졌다.
- **2013년 하반기**: 메모리 누수/증가용 플레임 그래프 문서화(저자, 초록색으로 구분), Off-CPU Time 플레임 그래프(Yichun Zhang)로 블로킹 시간 문제 해결, node.js 대상 작성법 정리(Igor Soarez), V8 Flame Charts 데모(Paul Irish, Umar Hansa, Addy Osmani).
- **2014년**: USENIX/LISA13 본회의 발표 "Blazing Performance with Flame Graphs", Off-CPU 플레임 그래프와 Hot/Cold 플레임 그래프 문서화, Java lightweight-java-profiler 활용법, Erlang용 eflame(Vladimir Kirillov), Linux Node.js 플레임 그래프 생성법(Trevor Norris), Oracle 데이터베이스용 예시(Luca Canali), FreeBSD TCP 스택 락 경합 분석(Julien Charbon) 등.
- **2014년 말**: Facebook Strobelight의 뒤집힌 플레임 그래프 활용, 원본 FlameGraph 소프트웨어에 Click to Zoom 기능 추가(Adrien Mahieux), Differential 플레임 그래프 발표(저자), 이를 응용한 CPI 플레임 그래프(메모리 스톨 사이클 강조), FreeBSD 개발자 서밋에서의 발표, Node.js 플레임 그래프 문서화, Netflix의 "Node.js in Flames" 사례(Yunong Xiao).
- **2015년**: Erlang, GHC(Haskell), ODI(Oracle Data Integrator) 등 다양한 언어·도구로 확장. Google Chrome과 Firefox에 플레임 그래프/차트 토글 기능 요청 제출. mixed-mode Java 플레임 그래프 프리뷰(저자, `-XX:+PreserveFramePointer` 옵션으로 이어짐, JDK9와 JDK8u60에 반영). FlameGraphdiff(Cor-Paul Bezemer)로 A-B 차이 시각화. SpeedScope(Jamie Wong)가 time-order·left-heavy·sandwich 세 가지 뷰를 한 GUI에서 지원.
- **2016년**: Netflix Java in Flames(저자와 Martin Spier), 다양한 언어(Go, Python, GHC 등)의 플레임 그래프 도구 확산, ACMQ 아티클 "The Flame Graph" 발표(정의·기원·해석법·향후 발전 방향 정리), 0x(David Mark Clements)로 Node.js 인터랙티브 프로파일러 등장, Qt Creator 4.0.0에 통합.
- **2016년 하반기~2017년**: Go GC 지연 분석, eBay의 원클릭 프로덕션 플레임 그래프, Spark/Cassandra/PowerDNS 등 실제 성능 개선 사례 다수, ETW 플레임 그래프(Windows Performance Analyzer 지원), Linux 4.9의 효율적인 BPF 기반 프로파일러 소개.
- **2017년**: USENIX ATC 2017 발표 갱신, "Coloring Flame Graphs: Code Hues" 포스트, Oracle Developer Studio Performance Analyzer에 통합, LinkedIn의 Common Issue Detection for CPU Profiling, 파일 시스템용 플레임 그래프와 트리맵/선버스트 비교 포스트, Java Package Flame Graph 소개.
- **2017년 말~2018년**: FlameScope 공개(저자와 Martin Spier, Netflix, GitHub 오픈소스), Java Heap Allocation Flamegraphs, Windows PerfView·pprof 웹 UI에 병합, async-profiler로 Clojure 지원.
- **2019년**: SAP HANA, AMD uProf, YourKit, IntelliJ IDE, Percona(MySQL) 등 상용·오픈소스 도구에 폭넓게 통합.
- **2020년**: AWS CodeGuru Profiler, Google Cloud Profiler(largest-frame-left 정렬 기본값 사용)에 플레임 그래프 도입.
- **2021년**: Intel vTune에 플레임 그래프 추가.

저자는 이 페이지 마지막에 "플레임 그래프에 대해 글을 쓰고, 더 발전시키고, 결과를 공유해 준 모든 사람에게 감사하며, 앞으로도 이 페이지를 새 소식으로 계속 갱신하겠다"고 적었다.

원문 최종 수정: 2025-01-23
