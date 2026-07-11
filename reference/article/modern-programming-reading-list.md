# 모던 프로그래밍 리딩 리스트

함수형 프로그래밍, 타입 설계, 동시성, 분산 시스템, 소프트웨어 설계 등 한 번은 읽어볼 만한 아티클 모음.
형식: `[제목](링크) — 요약`

## 1. 함수형 프로그래밍 — 철학과 기초

- [Why Functional Programming Matters (John Hughes)](https://www.cs.kent.ac.uk/people/staff/dat/miranda/whyfp90.pdf) — FP의 존재 이유를 "고차 함수와 지연 평가가 주는 모듈화 능력"으로 설명한 고전 논문. FP를 진지하게 공부한다면 출발점.
- [Out of the Tar Pit (Moseley & Marks)](https://curtclifton.net/papers/MoseleyMarks06a.pdf) — 소프트웨어 복잡성을 본질적 복잡성과 우연적 복잡성으로 나누고, 우연적 복잡성의 주범이 가변 상태와 제어 흐름이라고 진단한다. 함수형-관계형 프로그래밍을 대안으로 제시.
- [Monads for Functional Programming (Philip Wadler)](https://homepages.inf.ed.ac.uk/wadler/papers/marktoberdorf/baastad.pdf) — 모나드를 실용적 관점(예외, 상태, 출력)에서 소개한 원조 논문. 모나드 튜토리얼 100개보다 이 원문이 낫다.
- [Typeclassopedia](https://wiki.haskell.org/Typeclassopedia) — Functor, Applicative, Monad, Traversable 등 표준 타입클래스의 관계를 한 장의 지도로 정리한 문서. Haskell 기준이지만 Scala/Kotlin의 Cats, Arrow를 이해하는 데도 그대로 통한다.
- [Parse, don't validate (Alexis King)](https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/) — 검증(boolean 반환) 대신 파싱(더 정밀한 타입 반환)을 하라. 검증된 사실을 타입에 남겨 재검증과 방어 코드를 없애는 타입 주도 설계의 핵심 에세이.
- [Names are not type safety (Alexis King)](https://lexi-lambda.github.io/blog/2020/11/01/names-are-not-type-safety/) — newtype으로 이름만 바꾸는 것은 타입 안전성이 아니다. 생성자를 은닉하고 스마트 생성자로 불변식을 강제해야 진짜 안전성이 생긴다는 후속편.
- [No, dynamic type systems are not inherently more open (Alexis King)](https://lexi-lambda.github.io/blog/2020/01/19/no-dynamic-type-systems-are-not-inherently-more-open/) — "동적 타입이 더 유연하다"는 통념에 대한 반박. 정적 타입 시스템에서도 개방성을 표현할 수 있음을 보인다.
- [Boolean Blindness (Robert Harper)](https://existentialtype.wordpress.com/2011/03/15/boolean-blindness/) — bool은 "왜 참인지"의 증거를 버린다. 판별 결과 대신 증거를 담는 타입(Option, Either)을 쓰라는 CMU 교수의 짧고 강한 에세이.
- [Railway Oriented Programming (Scott Wlaschin)](https://fsharpforfunandprofit.com/rop/) — 에러 처리를 성공/실패 두 레일의 파이프라인으로 모델링하는 방법. Either/Result 기반 에러 처리를 비유로 쉽게 설명한다.
- [Designing with types 시리즈 (Scott Wlaschin)](https://fsharpforfunandprofit.com/series/designing-with-types/) — "잘못된 상태를 표현 불가능하게 만들기"를 단계별 예제로 보여주는 시리즈. F#이지만 Scala 3 enum, Kotlin sealed class로 그대로 옮겨진다.
- [An introduction to property-based testing (Scott Wlaschin)](https://fsharpforfunandprofit.com/posts/property-based-testing/) — 예제 기반 테스트의 한계를 게으른 개발자 이야기로 풀어내며 속성 기반 테스트를 소개한다.
- [Thirteen ways of looking at a turtle (Scott Wlaschin)](https://fsharpforfunandprofit.com/posts/13-ways-of-looking-at-a-turtle/) — 같은 turtle graphics API를 13가지 스타일(OOP, FP, 인터프리터, capability 등)로 구현하며 설계 스펙트럼을 비교한다.
- [Effective ML Revisited (Yaron Minsky)](https://blog.janestreet.com/effective-ml-revisited/) — Jane Street의 OCaml 실무 원칙. "Make illegal states unrepresentable"이라는 문구의 출처.
- [The Curse of the Excluded Middle (Erik Meijer)](https://queue.acm.org/detail.cfm?id=2611829) — "90% 함수형"은 안 통한다는 도발적 주장. 부수효과가 조금이라도 섞이면 등식 추론이 무너지는 사례들을 보여준다.
- [Functional Core, Imperative Shell (Gary Bernhardt)](https://www.destroyallsoftware.com/screencasts/catalog/functional-core-imperative-shell) — 순수한 결정 로직(코어)과 부수효과(셸)를 분리하는 아키텍처 패턴을 소개한 스크린캐스트. 실무 FP 적용의 가장 현실적인 출발점.
- [Boundaries (Gary Bernhardt)](https://www.destroyallsoftware.com/talks/boundaries) — 값(value)을 컴포넌트 경계로 삼으면 테스트와 동시성이 쉬워진다는 강연. Functional Core, Imperative Shell의 확장판.
- [Simple Made Easy (Rich Hickey)](https://www.infoq.com/presentations/Simple-Made-Easy/) — simple(엮이지 않음)과 easy(익숙함)를 구분한 전설적 강연. "복잡성은 엮임(complecting)에서 온다"는 프레임을 제공한다.
- [The Value of Values (Rich Hickey)](https://www.infoq.com/presentations/Value-Values/) — 불변 값 중심 설계가 시스템(캐시, 로그, 분산)까지 단순하게 만드는 이유를 설명하는 강연.
- [Making Impossible States Impossible (Richard Feldman)](https://www.youtube.com/watch?v=IcgmSRJHu_8) — Elm 컨퍼런스 강연. 유니언 타입으로 불가능한 상태 조합 자체를 컴파일 타임에 제거하는 실전 리팩터링을 보여준다.
- [Domain Modeling Made Functional (Scott Wlaschin, 강연)](https://www.youtube.com/watch?v=2JB1_e5wZmU) — DDD를 대수적 데이터 타입으로 하는 방법. 같은 제목의 책 내용을 1시간으로 압축한 강연.

## 2. Scala 3 / Scala 생태계

- [Scala 3 Book (공식)](https://docs.scala-lang.org/scala3/book/introduction.html) — Scala 3 문법과 관용구를 처음부터 다시 정리한 공식 책. 기존 `technology/pl/scala` 문서의 원전.
- [Scala 3 Macros Tutorial (공식)](https://docs.scala-lang.org/scala3/guides/macros/) — inline, quote/splice 기반의 새 매크로 시스템 공식 가이드.
- [Strategic Scala Style: Principle of Least Power (Li Haoyi)](https://www.lihaoyi.com/post/StrategicScalaStylePrincipleofLeastPower.html) — "표현 가능한 것 중 가장 약한 기능을 써라". Scala의 방대한 기능 중 무엇을 언제 쓸지에 대한 실무 지침.
- [Strategic Scala Style: Practical Type Safety (Li Haoyi)](https://www.lihaoyi.com/post/StrategicScalaStylePracticalTypeSafety.html) — 실무에서 비용 대비 효과가 좋은 타입 안전성 기법의 우선순위를 매긴다.
- [Strategic Scala Style: Designing Datatypes (Li Haoyi)](https://www.lihaoyi.com/post/StrategicScalaStyleDesigningDatatypes.html) — case class와 sealed trait로 데이터 타입을 설계할 때의 구체적 원칙들.
- [Scala with Cats (무료 온라인 책)](https://www.scalawithcats.com/) — 모노이드부터 모나드 변환자까지 Cats 라이브러리로 배우는 함수형 추상화. Scala FP 입문 표준.
- [Cats Effect: Concepts (공식)](https://typelevel.org/cats-effect/docs/concepts) — IO 모나드, 파이버, 취소 등 이펙트 시스템의 핵심 개념 정리. Typelevel 스택을 쓴다면 필독.
- [ZIO 공식 문서](https://zio.dev/) — ZIO 이펙트 시스템의 공식 문서. 에러 채널과 환경(R)을 타입에 인코딩하는 접근이 Cats Effect와 어떻게 다른지 비교하며 읽기 좋다.
- [Rock the JVM Articles](https://rockthejvm.com/articles) — 기존 `article/rockthejvm-scala3` 컬렉션의 출처. Scala 3, Cats Effect, ZIO 실전 튜토리얼이 계속 올라온다.

## 3. Kotlin / 코루틴

- [Structured Concurrency (Roman Elizarov)](https://elizarov.medium.com/structured-concurrency-722d765aa952) — 코루틴 설계자가 직접 쓴 구조적 동시성 선언문. "동시성에도 goto 금지 같은 구조화가 필요하다"는 주장.
- [Blocking threads, suspending coroutines (Roman Elizarov)](https://elizarov.medium.com/blocking-threads-suspending-coroutines-d33e11bf4761) — 블로킹과 서스펜션의 차이, 그리고 블로킹 코드를 코루틴 세계로 안전하게 감싸는 방법.
- [Explicit concurrency (Roman Elizarov)](https://elizarov.medium.com/explicit-concurrency-67a8e8fd9b25) — suspend 함수는 기본적으로 순차 실행이고, 동시성은 launch/async로 명시적으로만 도입된다는 Kotlin의 설계 철학.
- [Kotlin Coroutines Guide (공식)](https://kotlinlang.org/docs/coroutines-guide.html) — 코루틴 공식 가이드 전체. 취소, 예외 전파, Flow까지.
- [KEEP: Kotlin Coroutines 설계 문서](https://github.com/Kotlin/KEEP/blob/master/proposals/coroutines.md) — suspend 함수가 컴파일러 수준에서 CPS 변환으로 어떻게 구현되는지 설명하는 설계 제안서. 코루틴 내부가 궁금할 때.
- [Kotlin Idioms (공식)](https://kotlinlang.org/docs/idioms.html) — 관용적 Kotlin 코드 모음. Java 스타일 습관을 벗는 체크리스트로 좋다.
- [Kotlin Coding Conventions (공식)](https://kotlinlang.org/docs/coding-conventions.html) — 공식 코딩 컨벤션. 팀 스타일 가이드의 기준선.
- [Arrow 공식 문서](https://arrow-kt.io/) — 기존 `article/arrow-kt` 컬렉션의 원전. Kotlin에서 typed error, Resource, Optics 등 함수형 패턴을 제공하는 라이브러리.

## 4. 동시성과 메모리 모델

- [Notes on structured concurrency, or: Go statement considered harmful (Nathaniel J. Smith)](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/) — 구조적 동시성 개념을 대중화한 에세이. go/spawn 문을 goto에 비유하며 nursery(스코프) 모델을 제안한다. Kotlin coroutineScope의 사상적 배경.
- [What Color is Your Function? (Bob Nystrom)](https://journal.stuffwithstuff.com/2015/02/01/what-color-is-your-function/) — async/await가 만드는 "빨간 함수/파란 함수" 분열 문제를 다룬 유명 에세이. 코루틴과 가상 스레드 논쟁의 필수 배경지식.
- [There Is No Thread (Stephen Cleary)](https://blog.stephencleary.com/2013/11/there-is-no-thread.html) — 비동기 I/O가 진행되는 동안 스레드는 존재하지 않는다는 사실을 하드웨어 인터럽트 수준까지 내려가 설명한다.
- [The Problem with Threads (Edward A. Lee)](https://www2.eecs.berkeley.edu/Pubs/TechRpts/2006/EECS-2006-1.pdf) — 스레드는 비결정성을 기본값으로 만들기 때문에 근본적으로 결함 있는 모델이라는 버클리 논문.
- [JSR-133 (Java Memory Model) FAQ](https://www.cs.umd.edu/~pugh/java/memoryModel/jsr-133-faq.html) — happens-before, volatile, final 필드 의미론 등 JVM 메모리 모델의 표준 해설. JVM 언어(Scala/Kotlin) 사용자 모두에게 해당된다.
- [An Introduction to Lock-Free Programming (Jeff Preshing)](https://preshing.com/20120612/an-introduction-to-lock-free-programming/) — CAS, 메모리 오더링 등 락프리 프로그래밍의 지형도를 그려주는 입문 글.
- [Memory Barriers Are Like Source Control Operations (Jeff Preshing)](https://preshing.com/20120710/memory-barriers-are-like-source-control-operations/) — 메모리 배리어 4종류를 소스 컨트롤의 pull/push에 비유해 설명하는 명문.

## 5. 분산 시스템

- [Notes on Distributed Systems for Young Bloods (Jeff Hodges)](https://www.somethingsimilar.com/2013/01/14/notes-on-distributed-systems-for-young-bloods/) — 분산 시스템 실무자가 신입에게 전하는 교훈 모음. "네트워크는 실패한다, 좌표는 합의보다 비싸다" 같은 격언의 출처.
- [The Log: What every software engineer should know about real-time data's unifying abstraction (Jay Kreps)](https://web.archive.org/web/20240105095933/https://engineering.linkedin.com/distributed-systems/log-what-every-software-engineer-should-know-about-real-time-datas-unifying) — 로그를 분산 시스템의 통합 추상화로 제시한 글. Kafka의 사상적 기반이며, 기존 `article/confluent` 컬렉션과 이어진다.
- [Turning the database inside-out (Martin Kleppmann)](https://martin.kleppmann.com/2015/03/04/turning-the-database-inside-out.html) — 데이터베이스를 이벤트 스트림 중심으로 뒤집어 보는 강연 정리. 이벤트 소싱/스트림 처리의 관점 전환.
- [Life beyond Distributed Transactions (Pat Helland)](https://queue.acm.org/detail.cfm?id=3025012) — 분산 트랜잭션 없이 엔티티와 멱등 메시지로 설계하는 방법. 사가 패턴과 결과적 일관성 논의의 원류.
- [You Can't Sacrifice Partition Tolerance (Coda Hale)](https://web.archive.org/web/20231224104704/https://codahale.com/you-cant-sacrifice-partition-tolerance/) — CAP에서 P는 선택지가 아니라는 것을 짚은 글. CAP를 잘못 인용하는 습관을 교정해 준다.
- [Jepsen: Consistency Models](https://jepsen.io/consistency) — 선형성부터 read-committed까지 일관성 모델의 계층 지도. 기존 `article/jepsen` 컬렉션의 기준 문서.
- [A Distributed Systems Class (aphyr/distsys-class)](https://github.com/aphyr/distsys-class) — Jepsen 저자가 만든 분산 시스템 강의 노트. 한 파일로 읽는 분산 시스템 개론.
- [In Search of an Understandable Consensus Algorithm (Raft 논문)](https://raft.github.io/raft.pdf) — 이해 가능성을 최우선으로 설계한 합의 알고리즘. Paxos보다 먼저 읽는 것이 정석.
- [Fallacies of distributed computing](https://en.wikipedia.org/wiki/Fallacies_of_distributed_computing) — "네트워크는 신뢰할 수 있다" 등 분산 시스템 초심자가 저지르는 8가지 오류 목록.
- [Timeouts, retries, and backoff with jitter (Amazon Builders' Library)](https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/) — 타임아웃/재시도/지터를 AWS 운영 경험 기반으로 정리한 실무 가이드.
- [Avoiding fallback in distributed systems (Amazon Builders' Library)](https://aws.amazon.com/builders-library/avoiding-fallback-in-distributed-systems/) — 폴백 로직이 오히려 장애를 증폭시키는 이유와 대안.
- [Exponential Backoff And Jitter (AWS Architecture Blog)](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/) — 재시도 폭주를 지터로 해결하는 방법을 시뮬레이션으로 비교한 글.
- [Designing Data-Intensive Applications (책 사이트)](https://dataintensive.net/) — 데이터 시스템 설계의 사실상 표준 교과서. 아티클은 아니지만 이 리스트의 절반이 이 책 참고문헌에 들어 있다.

## 6. 소프트웨어 설계 에세이

- [The Grug Brained Developer](https://grugbrain.dev/) — "복잡성은 악마"라는 메시지를 원시인 말투로 전달하는 컬트적 에세이. 유머 속에 실무 지혜가 압축돼 있다.
- [Choose Boring Technology (Dan McKinley)](https://mcfunley.com/choose-boring-technology) — 혁신 토큰은 3개뿐이니 검증된 기술을 기본값으로 선택하라는 기술 선택론의 고전.
- [The Wrong Abstraction (Sandi Metz)](https://sandimetz.com/blog/2016/1/20/the-wrong-abstraction) — 잘못된 추상화보다 중복이 싸다. 추상화를 되돌리는(inline) 리팩터링을 정당화해 주는 글.
- [Write code that is easy to delete, not easy to extend (tef)](https://programmingisterrible.com/post/139222674273/write-code-that-is-easy-to-delete-not-easy-to) — 확장성 대신 삭제 가능성을 최적화하라. 레이어링과 결합에 대한 관점을 뒤집는 에세이.
- [Goodbye, Clean Code (Dan Abramov)](https://overreacted.io/goodbye-clean-code/) — 중복 제거 리팩터링이 오히려 코드를 나쁘게 만든 경험담. "clean code는 목표가 아니라 수단"이라는 교훈.
- [The Law of Leaky Abstractions (Joel Spolsky)](https://www.joelonsoftware.com/2002/11/11/the-law-of-leaky-abstractions/) — 모든 비자명한 추상화는 샌다. 추상화 아래 계층을 알아야 하는 이유.
- [Things You Should Never Do, Part I (Joel Spolsky)](https://www.joelonsoftware.com/2000/04/06/things-you-should-never-do-part-i/) — "처음부터 다시 짜기"가 최악의 전략적 실수인 이유. Netscape 사례.
- [Big Ball of Mud (Foote & Yoder)](http://www.laputan.org/mud/) — 가장 흔한 실제 아키텍처는 진흙 덩어리라는 것을 인정하고 분석한 논문.
- [The Rise of Worse is Better (Richard Gabriel)](https://www.dreamsongs.com/RiseOfWorseIsBetter.html) — 단순한 구현이 올바른 설계를 이기는 이유. Unix/C의 성공을 설명하는 고전.
- [Programming as Theory Building (Peter Naur)](https://pages.cs.wisc.edu/~remzi/Naur.pdf) — 프로그램의 진짜 산출물은 코드가 아니라 팀이 가진 이론(멘탈 모델)이라는 1985년 논문. 문서화와 인수인계 논의에서 계속 소환된다.
- [On the criteria to be used in decomposing systems into modules (David Parnas)](https://www.win.tue.nl/~wstomv/edu/2ip30/references/criteria_for_modularization.pdf) — 정보 은닉 개념을 만든 1972년 논문. 모듈 경계는 "바뀔 가능성이 있는 결정"을 숨기도록 그으라는 원칙.
- [Semantic Compression (Casey Muratori)](https://caseymuratori.com/blog_0015) — 추상화를 미리 설계하지 말고, 중복이 실제로 나타난 뒤 압축하듯 뽑아내라는 상향식 코딩론.
- [John Carmack on Inlined Code](http://number-none.com/blow/john_carmack_on_inlined_code.html) — Carmack이 함수 분리 대신 인라인 스타일을 옹호하게 된 이유를 쓴 이메일. 상태와 실행 순서가 명확해지는 효과를 논한다.
- [Hyrum's Law](https://www.hyrumslaw.com/) — 사용자가 충분히 많으면 API의 모든 관찰 가능한 동작이 누군가의 의존 대상이 된다는 법칙.
- [Cognitive Load is what matters (zakirullin)](https://github.com/zakirullin/cognitive-load) — 코드 품질 논쟁을 "읽는 사람의 인지 부하"라는 단일 기준으로 정리한 글. 최근 몇 년 사이 가장 널리 공유된 설계 에세이 중 하나.
- [A Philosophy of Software Design (John Ousterhout, Google 강연)](https://www.youtube.com/watch?v=bmSAYlu0NcY) — "깊은 모듈(작은 인터페이스, 큰 기능)"과 "복잡성은 점진적으로 쌓인다"는 동명 책의 핵심을 1시간에 요약한 강연.
- [Systems design explains the world (apenwarr)](https://apenwarr.ca/log/20201227) — 시스템 설계 감각(피드백 루프, 청킹, 캐패시티)이 조직과 세상 문제까지 설명한다는 장문 에세이.
- [Simplicity is An Advantage but Sadly Complexity Sells Better (Eugene Yan)](https://eugeneyan.com/writing/simplicity/) — 단순한 해법이 저평가되는 조직적 이유와 그럼에도 단순함을 팔아야 하는 이유.

## 7. 성능과 시스템 기초

- [Latency Numbers Every Programmer Should Know](https://gist.github.com/jboner/2841832) — L1 캐시부터 대륙 간 왕복까지 지연 시간 수치표. 성능 어림 계산의 기본 상수들.
- [What Every Programmer Should Know About Memory (Ulrich Drepper)](https://people.freebsd.org/~lstewart/articles/cpumemory.pdf) — 캐시 계층, NUMA, 프리페칭까지 메모리 하드웨어를 소프트웨어 개발자 관점에서 해부한 100페이지 논문.
- [The USE Method (Brendan Gregg)](https://www.brendangregg.com/usemethod.html) — 모든 리소스에 대해 Utilization/Saturation/Errors를 점검하는 성능 분석 방법론.
- [Flame Graphs (Brendan Gregg)](https://www.brendangregg.com/flamegraphs.html) — CPU 프로파일을 한눈에 보는 플레임 그래프의 원조 문서.
- [Napkin Math (Simon Eskildsen)](https://github.com/sirupsen/napkin-math) — 시스템 성능을 냅킨 계산으로 어림하기 위한 수치와 연습문제 모음.

## 8. API·실무 설계

- [Google Cloud API Design Guide](https://cloud.google.com/apis/design) — 리소스 지향 API 설계의 표준 참고서. gRPC/REST 공통 원칙.
- [Semantic Versioning](https://semver.org/) — SemVer 명세 원문. "무엇이 breaking change인가"를 논할 때의 기준 문서.
- [Keep a Changelog](https://keepachangelog.com/) — 사람이 읽는 체인지로그 작성 규약.
- [The Twelve-Factor App](https://12factor.net/) — 클라우드 네이티브 애플리케이션 설계 원칙 12개. 낡은 부분도 있지만 여전히 공용어.
- [Google Engineering Practices: Code Review](https://google.github.io/eng-practices/review/) — Google의 코드 리뷰 가이드 전문. "완벽보다 개선"이라는 리뷰 기준선을 제시한다.
- [Conventional Commits](https://www.conventionalcommits.org/) — 커밋 메시지 규약. 체인지로그 자동화와 SemVer 연동의 사실상 표준.

## 9. 데이터베이스와 SQL

- [Use The Index, Luke!](https://use-the-index-luke.com/) — 개발자를 위한 SQL 인덱싱 무료 책. B-tree부터 인덱스만으로 커버되는 쿼리까지. jOOQ 컬렉션과 짝으로 읽기 좋다.
- [How does a relational database work (Coding Geek)](https://web.archive.org/web/20150821021559/http://coding-geek.com/how-databases-work/) — 파서, 옵티마이저, 버퍼 풀, 트랜잭션 관리자까지 RDBMS 내부를 한 편으로 훑는 장문 글.
- [The Internals of PostgreSQL](https://www.interdb.jp/pg/) — PostgreSQL 내부 구조(프로세스, 스토리지, VACUUM, WAL)를 다룬 무료 온라인 책.
- [Don't Do This (PostgreSQL Wiki)](https://wiki.postgresql.org/wiki/Don%27t_Do_This) — PostgreSQL에서 하지 말아야 할 것들의 공식 위키. `timestamp(without time zone)` 금지 같은 실무 함정 모음.

## 10. 테스팅

- [The Practical Test Pyramid (Ham Vocke, martinfowler.com)](https://martinfowler.com/articles/practical-test-pyramid.html) — 테스트 피라미드를 현대적 마이크로서비스 맥락에서 다시 쓴 결정판 가이드.
- [Mocks Aren't Stubs (Martin Fowler)](https://martinfowler.com/articles/mocksArentStubs.html) — 테스트 더블 분류와 고전파/모의객체파 TDD의 차이를 정리한 표준 문서.
- [Test Desiderata (Kent Beck)](https://kentbeck.github.io/TestDesiderata/) — 좋은 테스트가 가져야 할 12가지 속성(격리, 결정성, 행위 민감성 등)과 그 트레이드오프.
- [Unit Testing is Overrated (Oleksii Holub)](https://tyrrrz.me/blog/unit-testing-is-overrated) — 목 범벅 단위 테스트 대신 통합 테스트에 투자하라는 반론. 테스트 전략 논쟁의 대표 글.
- [Google Testing Blog](https://testing.googleblog.com/) — Testing on the Toilet 시리즈를 포함한 Google의 테스트 문화 아카이브.

## 11. 모든 개발자의 기초 소양

- [What Every Computer Scientist Should Know About Floating-Point Arithmetic](https://docs.oracle.com/cd/E19957-01/806-3568/ncg_goldberg.html) — 부동소수점의 반올림, 오차 전파, 비교 함정을 다룬 표준 문서. `0.1 + 0.2 != 0.3`의 모든 것.
- [The Absolute Minimum Every Software Developer Must Know About Unicode in 2023 (Nikita Prokopov)](https://tonsky.me/blog/unicode/) — Joel의 2003년 유니코드 글을 UTF-8, 그래핌 클러스터, 정규화까지 현대적으로 갱신한 버전.
- [UTF-8 Everywhere](https://utf8everywhere.org/) — 문자열 인코딩은 어디서나 UTF-8이어야 한다는 선언문. UTF-16의 역사적 부채를 상세히 설명한다.
- [Falsehoods Programmers Believe About Names (Patrick McKenzie)](https://www.kalzumeus.com/2010/06/17/falsehoods-programmers-believe-about-names/) — "이름에는 성과 이름이 있다" 같은 잘못된 가정 40개. falsehoods 장르의 원조.
- [Falsehoods Programmers Believe About Time](https://infiniteundo.com/post/25326999628/falsehoods-programmers-believe-about-time) — 시간에 대한 잘못된 가정 모음. 타임존/DST 버그를 만들기 전에 읽을 것.

## 12. Go / Rust — 모던 언어 설계 감각

- [Errors are values (Rob Pike, Go Blog)](https://go.dev/blog/errors-are-values) — 에러를 예외가 아니라 값으로 다루면 프로그래밍 가능해진다는 Go 에러 철학의 핵심 글.
- [Share Memory By Communicating (Go Blog)](https://go.dev/blog/codelab-share) — "메모리 공유로 통신하지 말고 통신으로 메모리를 공유하라"는 Go 동시성 슬로건의 해설.
- [Go Proverbs](https://go-proverbs.github.io/) — Rob Pike가 정리한 Go 격언 모음. 언어 설계 철학이 한 줄씩 압축돼 있다.
- [Error Handling in Rust (Andrew Gallant / BurntSushi)](https://blog.burntsushi.net/rust-error-handling/) — Option/Result 합성부터 커스텀 에러 타입까지 Rust 에러 처리를 집대성한 장문 가이드.
- [Rust API Guidelines](https://rust-lang.github.io/api-guidelines/) — Rust 라이브러리 API 설계 체크리스트. 네이밍, 타입 안전성, future-proofing 관례의 공식 정리.
- [Learn Rust With Entirely Too Many Linked Lists](https://rust-unofficial.github.io/too-many-lists/) — 연결 리스트를 6번 구현하며 소유권과 빌림(borrow)을 몸으로 배우는 무료 책. 소유권 모델 이해에 가장 효과적인 자료 중 하나.

## 13. TypeScript — 타입 레벨 프로그래밍

- [Effective TypeScript Blog (Dan Vanderkam)](https://effectivetypescript.com/) — 동명 책 저자의 블로그. 구조적 타이핑, 타입 좁히기 등 심화 주제가 아티클로 계속 올라온다.
- [Total TypeScript Articles (Matt Pocock)](https://www.totaltypescript.com/articles) — 제네릭, 조건부 타입, 브랜디드 타입 등 타입 레벨 테크닉 실전 아티클 모음.
- [type-challenges](https://github.com/type-challenges/type-challenges) — 타입 시스템만으로 푸는 문제집. 조건부 타입과 재귀 타입 근육을 기르는 데 최적.

## 14. 일하는 방식

- [How to ask good questions (Julia Evans)](https://jvns.ca/blog/good-questions/) — 좋은 질문의 구조(상황, 시도, 최소화)를 정리한 글. 주니어 온보딩 자료로 자주 쓰인다.
- [Kalzumeus Greatest Hits (Patrick McKenzie)](https://www.kalzumeus.com/greatest-hits/) — 연봉 협상, 커리어, 소프트웨어 비즈니스에 대한 대표 에세이 모음. 엔지니어의 비즈니스 감각을 기르는 데 좋다.
- [Boring Technology Club (Dan McKinley)](https://boringtechnology.club/) — Choose Boring Technology의 슬라이드+스크립트 강연판. 글보다 강연 흐름으로 읽고 싶을 때.
