# 스레드의 문제점

> 원문: [The Problem with Threads](https://www2.eecs.berkeley.edu/Pubs/TechRpts/2006/EECS-2006-1.pdf)
> 저자: Edward A. Lee (UC Berkeley, Technical Report No. UCB/EECS-2006-1, 2006년 1월 10일) | 번역일: 2026-07-10

## 초록

스레드는 지배적인 순차 계산 모델을 동시성 시스템에 겉보기에 단순하게 적응시킨 것이다. 언어는 스레드를 지원하기 위해 문법을 거의 또는 전혀 바꿀 필요가 없고, 운영체제와 아키텍처는 스레드를 효율적으로 지원하도록 진화해 왔다. 많은 기술 전문가가 컴퓨터 아키텍처에서 예측되는 병렬성 증가를 활용하려고 소프트웨어에서 멀티스레딩을 더 많이 쓰자고 밀어붙이고 있다. 이 논문에서 나는 그것이 좋은 생각이 아니라고 주장한다. 스레드는 순차 계산에서 작은 한 걸음처럼 보이지만, 실제로는 거대한 도약이다. 스레드는 순차 계산이 가진 가장 본질적이고 매력적인 성질, 곧 이해 가능성, 예측 가능성, 결정론을 내다 버린다. 계산 모델로서의 스레드는 극도로 비결정적이며, 프로그래머의 일은 그 비결정성을 가지치기하는 것이 된다. 많은 연구 기법이 더 효과적인 가지치기를 제공해 모델을 개선하지만, 나는 이것이 문제를 거꾸로 접근하는 것이라고 주장한다. 비결정성을 가지치기하는 대신, 본질적으로 결정적이고 합성 가능한 컴포넌트에서 출발해야 한다. 비결정성은 필요 없는 곳에서 제거하는 것이 아니라, 필요한 곳에 명시적이고 신중하게 도입해야 한다. 이 원칙의 결과는 심대하다. 나는 견고하고 합성 가능한 형식론에 기반한 동시성 코디네이션 언어의 개발을 주장한다. 그런 언어가 훨씬 더 신뢰할 수 있고 더 동시적인 프로그램을 낳으리라고 믿는다.

## 1 서론

동시성 프로그래밍이 어렵다는 것은 널리 인정되는 사실이다. 그런데 동시성 프로그래밍의 필요성은 점점 더 절박해지고 있다. 많은 기술 전문가가 무어의 법칙의 종말은 점점 더 병렬화되는 컴퓨터 아키텍처(멀티코어 또는 칩 멀티프로세서, CMP)로 응답될 것이라고 예측한다 [15]. 컴퓨팅에서 성능 향상을 계속 얻으려면, 프로그램이 이 병렬성을 활용할 수 있어야 한다.

가능한 기술적 해법 하나는 순차 프로그램에서 병렬성을 자동으로 뽑아내는 것이다. 동적 디스패치 같은 컴퓨터 아키텍처 기법을 쓰거나, 순차 프로그램의 자동 병렬화를 쓰는 방법이다 [6]. 그러나 많은 연구자가 이러한 자동 기법은 이미 갈 수 있는 데까지 갔고, 뽑아낼 수 있는 병렬성이 크지 않다는 데 동의한다. 자연스러운 결론은 프로그램 자체가 더 동시적이 되어야 한다는 것이다.

동시성 프로그래밍이 왜 그렇게 어려운지 이해한다면, 문제를 해결할 가능성도 커진다. Sutter와 Larus는 이렇게 말한다 [47].

> "인간은 동시성에 금방 압도되며, 동시적 코드는 순차적 코드보다 추론하기가 훨씬 어렵다고 느낀다. 부분 순서만 있는 연산들의 단순한 모음에서조차, 신중한 사람도 가능한 인터리빙을 놓친다."

그런데 인간은 사실 동시성 시스템을 꽤 잘 추론한다. 물리적 세계는 고도로 동시적이며, 우리의 생존 자체가 동시적 동역학을 추론하는 능력에 달려 있다. 문제는 우리가 물리적 세계의 동시성과 조금도 닮지 않은 동시성 추상화를 골랐다는 데 있다. 우리는 이 계산적 추상화에 너무 익숙해진 나머지, 그것이 불변의 진리가 아니라는 사실을 잊어버렸다. 이 논문에서 나는 동시성 프로그래밍의 어려움이 추상화의 결과이며, 그 추상화를 기꺼이 놓아준다면 문제는 고칠 수 있다고 주장한다.

Barrosso [7]는 낙관적인 견해를 내놓는다. 기술적 힘이 서버와 데스크톱 컴퓨팅을 모두 CMP로 몰아갈 것이고, 그 기술이 주류가 되면 프로그래밍 문제도 어떻게든 해결되리라는 것이다. 그러나 우리는 그 도전을 과소평가해서는 안 된다. 필요한 것은, Stein의 표현을 빌리면, "일련의 단계라는 관습적 은유를 상호작용하는 개체들의 공동체라는 개념으로 대체하는 것"이다 [46]. 이 논문이 그 논거를 제시한다.

## 2 스레드

범용 소프트웨어 공학 실무에서, 동시성 프로그래밍의 한 가지 접근이 다른 모든 것을 압도하는 지점에 이르렀다. 바로 스레드다. 스레드는 메모리를 공유하는 순차적 프로세스다. 스레드는 현대의 컴퓨터, 프로그래밍 언어, 운영체제가 지원하는 핵심 동시성 모델이다. 오늘날 쓰이는 많은 범용 병렬 아키텍처(대칭형 멀티프로세서 SMP 등)는 스레드 추상화를 하드웨어로 직접 구현한 것이다.

일부 애플리케이션은 스레드를 매우 효과적으로 쓸 수 있다. 이른바 "처치 곤란 병렬(embarrassingly parallel)" 애플리케이션(예를 들어 PVM gmake 같은 빌드 도구나 웹 서버처럼, 본질적으로 여러 독립 프로세스를 생성하는 애플리케이션)이 그렇다. 이런 애플리케이션은 서로 독립적이므로 프로그래밍이 비교적 쉽고, 쓰이는 추상화도 스레드(메모리 공유)라기보다 프로세스(메모리 비공유)에 가깝다. 이런 애플리케이션이 데이터를 공유해야 할 때는 데이터베이스 추상화를 통해 공유하며, 데이터베이스는 트랜잭션 같은 메커니즘으로 동시성을 관리한다. 그러나 클라이언트 측 애플리케이션은 그렇게 단순하지 않다. Sutter와 Larus를 다시 인용한다 [47].

> "클라이언트 애플리케이션의 세계는 그만큼 잘 구조화되어 있지도, 규칙적이지도 않다. 전형적인 클라이언트 애플리케이션은 단일 사용자를 위해 비교적 작은 계산을 수행하므로, 동시성은 계산을 더 잘게 나누는 데서 나온다. 이 조각들, 이를테면 사용자 인터페이스와 프로그램의 계산은 무수한 방식으로 상호작용하고 데이터를 공유한다. 비균질적인 코드, 세밀하고 복잡한 상호작용, 포인터 기반 자료구조 때문에 이런 종류의 프로그램은 동시적으로 실행하기 어렵다."

물론 스레드가 동시성 프로그래밍의 유일한 선택지는 아니다. 성능 요구 때문에 오래전부터 동시성 프로그래밍이 필수였던 과학 계산 분야에서는, 데이터 병렬 언어 확장과 메시지 전달 라이브러리(PVM [23], MPI [39], OpenMP¹ 등)가 스레드보다 우세하다. 실제로 과학 계산용 컴퓨터 아키텍처는 이른바 "범용" 아키텍처와 상당히 다른 경우가 많다. 예를 들어 벡터와 스트림을 하드웨어에서 지원하는 것이 보통이다. 그러나 이 분야에서조차 동시적 프로그램을 작성하는 일은 여전히 지루하다. 훨씬 나은 데이터 병렬 언어들의 오랜 역사에도 불구하고 C와 FORTRAN이 지배적이다.

분산 컴퓨팅에서 스레드는 실용적인 추상화가 아닌 경우가 많다. 공유 메모리라는 환상을 만들어내는 비용이 너무 크기 때문이다. 그럼에도 우리는 멀티스레드 프로그래밍을 흉내 내는 분산 컴퓨팅 메커니즘을 만드는 데 상당한 공을 들여 왔다. 예를 들어 CORBA와 .NET은 분산 객체지향 기법에 뿌리를 두는데, 소프트웨어 컴포넌트가 마치 메모리를 공유하는 로컬 객체처럼 동작하는 프록시와 상호작용한다. 객체지향의 데이터 추상화는 공유 메모리 환상을 유지해야 하는 범위를 제한하므로, 이런 기법은 비용 대비 효과가 그런대로 괜찮다. 이 기법들은 분산 프로그래밍을 멀티스레드 프로그래밍과 매우 비슷하게 보이도록 만든다.

임베디드 컴퓨팅도 스레드 이외의 동시성 모델을 활용한다. 프로그래머블 DSP 아키텍처는 흔히 VLIW 머신이다. 비디오 신호 프로세서는 SIMD와 VLIW, 스트림 처리를 결합하는 경우가 많다. 네트워크 프로세서는 스트리밍 데이터를 위한 명시적 하드웨어 지원을 제공한다. 그러나 상당한 혁신적 연구에도 불구하고, 실무에서 이 분야의 프로그래밍 모델은 여전히 원시적이다. 설계자는 특정 하드웨어 기능을 활용하는 저수준 어셈블리 코드를 작성하고, 성능이 그리 중요하지 않은 곳에서만 이 어셈블리 코드를 C 코드와 결합한다.

많은 임베디드 애플리케이션의 흥미로운 성질 하나는, 신뢰성과 예측 가능성이 표현력이나 성능보다 훨씬 중요하다는 점이다. 범용 컴퓨팅에서도 그래야 한다고 주장할 수 있지만, 그것은 곁가지 논쟁이다. 나는 많은 애플리케이션에서 스레드로 신뢰성과 예측 가능성을 달성하는 것이 본질적으로 불가능하다고 주장하겠다.

¹ http://www.openmp.org 참조

## 3 계산으로서의 스레드

이 절에서는 특정 스레드 라이브러리나 언어를 언급하지 않고 근본적인 관점에서 스레드를 검토하여, 계산 모델로서 스레드에 심각한 결함이 있음을 보이겠다. 다음 절에서는 제안된 여러 수정안을 살펴본다.

ℕ = {0, 1, 2, ...}이 자연수를 나타낸다고 하자. B = {0, 1}을 이진 숫자의 집합이라 하자. B*를 모든 유한 비트 시퀀스의 집합이라 하고,

B^ω = (ℕ → B)

를 모든 무한 비트 시퀀스의 집합(각각은 ℕ을 B로 사상하는 함수)이라 하자. [17]을 따라 B** = B* ∪ B^ω라 하자. B**로 계산 기계의 상태, (잠재적으로 무한한) 입력, (잠재적으로 무한한) 출력을 표현하겠다.

Q = (B** ⇀ B**)

를 정의역과 공역이 B**인 모든 부분 함수의 집합이라 하자.²

명령형 기계 (A, c)는 원자적 동작의 유한 집합 A ⊂ Q와 제어 함수 c: B** → ℕ이다. 집합 A는 기계의 원자적 동작(보통 명령어)을 나타내고, 함수 c는 명령어들이 어떻게 순서화되는지를 나타낸다. A가 다음 성질을 가진 정지 명령어 h ∈ A를 하나 포함한다고 가정한다.

∀ b ∈ B**,   h(b) = b.

즉, 정지 명령어는 상태를 바꾸지 않는다.

길이 m ∈ ℕ의 순차 프로그램은 함수

p: ℕ → A

이며, 여기서

∀ n ≥ m,   p(n) = h.

즉, 순차 프로그램은 명령어의 유한 시퀀스 뒤에 정지 명령어의 무한 시퀀스가 붙은 것이다. 모든 순차 프로그램의 집합(P라 표기)은 가산 무한 집합임에 유의하라.

이 프로그램의 실행이 스레드다. 실행은 기계의 초기 상태와 (잠재적으로 무한한) 입력을 나타내는 초기 b₀ ∈ B**에서 시작하며, 모든 n ∈ ℕ에 대해

b_{n+1} = p(c(b_n))(b_n).   (1)

여기서 c(b_n)은 다음 명령어 p(c(b_n))에 대한 프로그램 p 내 인덱스를 제공한다. 그 명령어를 상태 b_n에 적용해 다음 상태 b_{n+1}을 얻는다. 어떤 n ∈ ℕ에 대해 c(b_n) ≥ m이면 p(c(b_n)) = h이고 프로그램은 상태 b_n에서 정지한다(즉, 이후 상태는 영원히 변하지 않는다). 모든 초기 상태 b₀ ∈ B에 대해 프로그램 p가 정지하면, p는 Q의 전함수를 정의한다. 어떤 b₀ ∈ B에 대해 정지하면 Q의 부분 함수를 정의한다.³

이제 순차 프로그램이 가진 핵심 매력에 다다른다. 프로그램과 초기 상태가 주어지면, (1)이 주는 시퀀스가 정의된다. 시퀀스가 정지하면, 프로그램이 계산하는 함수가 정의된다. 임의의 두 프로그램 p와 p′는 비교할 수 있다. 같은 부분 함수를 계산하면 두 프로그램은 동치다. 즉, 같은 초기 상태에서 정지하고, 그런 초기 상태에 대해 최종 상태가 같으면 동치다.⁴ 이런 동치 이론은 어떤 유용한 형식론에서든 필수적이다.

여러 스레드를 합성하면 프로그램의 이 본질적이고 매력적인 성질이 사라진다. 멀티스레드 방식으로 동시에 실행되는 두 프로그램 p₁과 p₂를 생각해 보자. 이것이 뜻하는 바는 (1)이 다음으로 대체된다는 것이다.

b_{n+1} = p_i(c(b_n))(b_n)   i ∈ {1, 2}.   (2)

각 단계 n에서, 두 프로그램 중 어느 쪽이든 다음 (원자적) 동작을 제공할 수 있다. 이제 유용한 동치 이론이 있는지 생각해 보자. 즉, 멀티스레드 프로그램 쌍 (p₁, p₂)와 또 다른 쌍 (p₁′, p₂′)가 주어졌을 때, 두 쌍은 언제 동치인가? 기본 이론의 합리적 확장은, 같은 초기 상태에서 모든 인터리빙이 정지하고 같은 최종 상태를 내면 동치라고 정의한다. 가능한 인터리빙의 수가 어마어마해서, 자명한 경우(예를 들어 상태 B**가 분할되어 두 프로그램이 서로의 분할에 영향을 주지 않는 경우)를 제외하면 이런 동치를 추론하기가 극도로 어렵다.

더 나쁜 것은, (1)에 따라 실행할 때 동치인 두 프로그램 p와 p′가 있어도, 멀티스레드 환경에서 실행되면 더 이상 동치라고 결론지을 수 없다는 점이다. 사실 실행될 수 있는 다른 모든 스레드를 알아야 하고(이 자체가 잘 정의되지 않을 수 있다), 가능한 모든 인터리빙을 분석해야 한다. 결론은, 스레드에는 유용한 동치 이론이 없다는 것이다.

더더욱 나쁜 것은, 멀티스레드 계산 모델을 구현하는 일이 극도로 어렵다는 점이다. 예를 들어 Java 메모리 모델의 깊은 미묘함이 그 증거다([41]과 [24] 참조). 놀랄 만큼 자명한 프로그램조차 가능한 동작을 두고 상당한 논쟁을 낳는다.

널리 쓰이는 모든 프로그래밍 언어가 기반을 두는 (1)의 핵심 계산 추상화는, 결정적 컴포넌트의 결정적 합성을 강조한다. 동작은 결정적이고 그 순차 합성도 결정적이다. 순차 실행은 의미론적으로 함수 합성이다. 결정적 컴포넌트가 결정적 결과로 합성되는 깔끔하고 단순한 모델이다.

반면 스레드는 극도로 비결정적이다. 프로그래머의 일은 그 비결정성을 가지치기하는 것이다. 물론 우리는 가지치기를 돕는 도구를 개발해 왔다. 세마포어, 모니터, 그리고 스레드 위에 얹는 더 현대적인 장치들(다음 절에서 논의)은 점점 더 효과적인 가지치기를 제공한다. 하지만 마구 자란 가시덤불을 가지치기해서 만족스러운 울타리가 나오는 일은 드물다.

또 다른 비유를 들자면, 기계 공학자에게 열운동에 따라 무작위로 움직이는 철, 탄화수소, 산소 분자가 든 솥에서 시작해 내연기관을 설계하라고 요청한다고 상상해 보자. 공학자의 일은 이 운동을 제약하여 결과가 내연기관이 되도록 만드는 것이다. 열역학과 화학은 이것이 설계 문제를 생각하는 이론적으로 타당한 방법이라고 말해 준다. 하지만 실용적인가?

세 번째 비유로, 광기의 속설적 정의는 같은 일을 반복하면서 다른 결과를 기대하는 것이다. 이 정의에 따르면, 우리는 사실상 멀티스레드 시스템의 프로그래머에게 미쳐 있기를 요구하는 셈이다. 제정신이라면 자기 프로그램을 이해할 수 없을 테니까.

나는 우리가 훨씬 더 결정적인 동시성 계산 모델을 만들어야 하고(만들 수 있고), 필요한 곳에 비결정성을 신중하고 조심스럽게 도입해야 한다고 주장하겠다. 비결정성은 순차 프로그래밍에서 그렇듯이, 필요한 곳에만 명시적으로 프로그램에 추가되어야 한다. 스레드는 정반대 접근을 취한다. 스레드는 프로그램을 터무니없이 비결정적으로 만들고, 결정적 목표를 달성하기 위해 그 비결정성을 제약하는 일을 프로그래밍 스타일에 떠넘긴다.

² 부분 함수는 정의역의 각 원소에서 정의될 수도 있고 아닐 수도 있는 함수다.
³ 컴퓨팅의 고전적 결과 하나가 이제 자명해졌음에 유의하라. Q가 가산 집합이 아님은 쉽게 보일 수 있다. (B** 자체가 비가산이므로, 상수 함수만 모은 Q의 부분집합조차 비가산이다. 칸토어의 대각선 논법으로 쉽게 증명할 수 있다.) 모든 유한 프로그램의 집합 P는 가산이므로, Q의 모든 함수를 유한 프로그램으로 표현할 수는 없다는 결론이 나온다. 즉, 어떤 순차 기계도 표현력에 한계가 있다. 튜링과 처치 [48]는 순차 기계 (A, c)의 여러 선택이 정확히 Q의 같은 부분집합을 표현하는 프로그램 P를 낳는다는 것을 증명했다. 이 부분집합을 실효적 계산 가능 함수라 부른다.
⁴ 이 고전적 이론에서 정지하지 않는 프로그램은 모두 동치다. 이는 계산 이론을 임베디드 소프트웨어에 적용할 때 심각한 문제를 일으키는데, 임베디드 소프트웨어에서 유용한 프로그램은 정지하지 않기 때문이다 [34].

## 4 실무에서는 얼마나 나쁜가?

우리는 스레드가 계산의 핵심 추상화를 절망적으로 쓸 수 없게 확장한 것이라고 주장했다. 그런데 실무에서는 오늘날 많은 프로그래머가 동작하는 멀티스레드 프로그램을 작성한다. 모순 아닌가? 실무에서 프로그래머에게는 비결정성 대부분을 가지치기하는 도구가 주어진다. 예를 들어 객체지향 프로그래밍은 프로그램의 특정 부분이 상태의 특정 부분에 접근할 수 있는 가시성을 제한한다. 이는 사실상 상태 공간 B**를 서로 소인 구역으로 분할한다. 프로그램이 이 상태 공간의 공유 부분에서 동작하는 곳에서는 세마포어, 상호 배제 락, 모니터(상호 배타적인 메서드를 가진 객체)가 비결정성을 더 가지치기할 수 있는 메커니즘을 제공한다. 그러나 실무에서 이 기법들은 아주 단순한 상호작용에서만 이해 가능한 프로그램을 낳는다.

옵저버 패턴 [22]이라는 흔히 쓰이는 디자인 패턴을 생각해 보자. 이것은 매우 단순하고 널리 쓰이는 디자인 패턴이다. 단일 스레드에서 유효한 Java 구현이 그림 1에 나와 있다.⁵ 이 클래스는 두 메서드를 보여 준다. `setValue()` 메서드를 호출하면 `addListener()` 호출로 등록된 모든 객체의 `valueChanged()` 메서드를 호출해 새 값을 통지한다.

```java
public class ValueHolder {
    private List listeners = new LinkedList();
    private int value;
    public interface Listener {
        public void valueChanged(int newValue);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void setValue(int newValue) {
        value = newValue;
        Iterator i = listeners.iterator();
        while(i.hasNext()) {
            ((Listener)i.next()).valueChanged(newValue);
        }
    }
}
```

그림 1: 단일 스레드에서 유효한 옵저버 패턴의 Java 구현.

그러나 그림 1의 코드는 스레드 안전하지 않다. 즉, 여러 스레드가 `setValue()`나 `addListener()`를 호출할 수 있으면, 이터레이터가 리스트를 순회하는 도중에 `listeners` 리스트가 수정될 수 있다. 이는 예외를 발생시키고, 아마 프로그램을 종료시킬 것이다.

가장 단순한 해법은 `setValue()`와 `addListener()` 메서드 정의 각각에 Java 키워드 `synchronized`를 붙이는 것이다. Java의 `synchronized` 키워드는 상호 배제를 구현하여 이 `ValueHolder` 클래스의 인스턴스를 모니터로 바꾸고, 두 스레드가 동시에 이 메서드들 안에 있지 못하게 막는다. synchronized 메서드가 호출되면 호출 스레드는 객체에 대한 배타적 락을 얻으려 시도한다. 다른 스레드가 그 락을 쥐고 있으면, 호출 스레드는 락이 해제될 때까지 멈춘다.

그러나 이 해법은 현명하지 않다. 데드락으로 이어질 수 있기 때문이다. 구체적으로, `ValueHolder`의 인스턴스 a와 `Listener` 인터페이스를 구현하는 다른 클래스의 인스턴스 b가 있다고 하자. 그 다른 클래스는 `valueChanged()` 메서드에서 무엇이든 할 수 있는데, 다른 모니터의 락을 획득하는 것도 포함된다. 그 락 획득에서 멈춰 버리면, 멈춰 있는 동안에도 이 `ValueHolder` 객체의 락은 계속 쥐고 있게 된다. 그사이 획득하려는 락을 쥔 스레드가 a에 대해 `addListener()`를 호출할 수 있다. 이제 두 스레드는 풀려날 희망 없이 블록된다. 이런 잠재적 데드락은 모니터를 쓰는 많은 프로그램에 잠복해 있다.

이미 이 단순한 디자인 패턴조차 올바르게 구현하기 어렵다는 것이 드러나고 있다. 그림 2에 나온 개선된 구현을 생각해 보자. `setValue()` 메서드는 락을 쥔 상태에서 리스너 리스트의 사본을 만든다. `addListeners()` 메서드가 synchronized이므로, 그림 1의 코드에서 발생할 수 있는 동시 수정 예외를 피한다. 나아가 데드락을 피하기 위해 `valueChanged()`를 synchronized 블록 바깥에서 호출한다.

```java
public class ValueHolder {
    private List listeners = new LinkedList();
    private int value;
    public interface Listener {
        public void valueChanged(int newValue);
    }

    public synchronized void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void setValue(int newValue) {
        List copyOfListeners;
        synchronized(this) {
            value = newValue;
            copyOfListeners = new LinkedList(listeners);
        }
        Iterator i = copyOfListeners.iterator();
        while(i.hasNext()) {
            ((Listener)i.next()).valueChanged(newValue);
        }
    }
}
```

그림 2: 스레드 안전을 시도한 옵저버 패턴의 Java 구현.

문제를 해결했다고 생각하기 쉽지만, 사실 이 코드도 여전히 올바르지 않다. 두 스레드가 setValue()를 호출한다고 하자. 둘 중 하나가 값을 마지막에 설정하고, 그 값이 객체에 남는다. 그러나 리스너들은 값 변경 통지를 반대 순서로 받을 수 있다. 리스너들은 `ValueHolder` 객체의 최종 값이 잘못된 값이라고 결론짓게 된다!

물론 이 패턴을 Java에서 견고하게 동작하도록 만들 수는 있다(독자를 위한 연습 문제로 남긴다). 내 요점은, 이토록 단순하고 흔히 쓰이는 디자인 패턴조차 가능한 인터리빙에 대한 꽤나 복잡한 사고를 요구한다는 것이다. 나는 대부분의 멀티스레드 프로그램에 이런 버그가 있다고 추측한다. 나아가 이 버그들이 큰 장애물이 되지 않은 이유는, 오늘날의 아키텍처와 운영체제가 병렬성을 제한적으로만 제공하기 때문이라고 추측한다. 컨텍스트 스위칭 비용이 높아서, 스레드 명령어의 가능한 인터리빙 중 극히 일부만 실제로 발생한다. 나는 대부분의 멀티스레드 범용 애플리케이션이 사실 동시성 버그로 가득 차 있어서, 멀티코어 아키텍처가 흔해지면 이 버그들이 시스템 장애로 나타나기 시작할 것이라고 추측한다. 이 시나리오는 컴퓨터 벤더에게 암울하다. 그들의 차세대 기계는 많은 프로그램이 크래시하는 기계로 널리 알려지게 될 것이다.

바로 그 컴퓨터 벤더들이, 팔고 싶어 하는 병렬성을 활용할 동시성이 생기도록 더 많은 멀티스레드 프로그래밍을 옹호하고 있다. 예를 들어 Intel은 유수의 컴퓨터과학 학과들이 멀티스레드 프로그래밍을 더 강조하도록 만드는 적극적인 캠페인에 착수했다. 그들이 성공해서 차세대 프로그래머들이 멀티스레딩을 더 집중적으로 쓰게 된다면, 차세대 컴퓨터는 거의 쓸 수 없는 물건이 될 것이다.

⁵ 이 예제를 제안해 준 Mark S. Miller에게 감사한다.

## 5 더 공격적인 가지치기로 스레드 고치기

이 절에서는 이 문제를 해결하려는 여러 접근을 논의한다. 이들은 공통점을 하나 공유한다. 프로그래머에게 스레드라는 본질적 계산 모델은 유지하되, 어마어마하게 비결정적인 동작을 가지치기하는 더 공격적인 메커니즘을 제공한다는 점이다.

첫 번째 기법은 더 나은 소프트웨어 공학 프로세스다. 이는 사실 신뢰할 수 있는 멀티스레드 프로그램을 얻는 데 필수적이다. 그러나 충분하지는 않다. Ptolemy 프로젝트⁶의 일화가 시사적(그리고 섬뜩)하다. 2000년 초, 우리 그룹은 동시성 계산 모델을 지원하는 모델링 환경인 Ptolemy II [20]의 커널을 개발하기 시작했다. 초기 목표 중 하나는 동시적 프로그램이 실행되는 동안 그래픽 사용자 인터페이스로 그 프로그램을 수정할 수 있게 하는 것이었다. 과제는 어떤 스레드도 프로그램 구조의 일관성 없는 뷰를 절대 볼 수 없게 보장하는 것이었다. 전략은 Java 스레드와 모니터를 쓰는 것이었다.

Ptolemy 프로젝트 실험의 일부는 학술 연구 환경에서 효과적인 소프트웨어 공학 관행을 개발할 수 있는지 확인하는 것이었다. 우리는 코드 성숙도 등급 시스템(빨강, 노랑, 초록, 파랑 4단계), 설계 리뷰, 코드 리뷰, 야간 빌드, 회귀 테스트, 자동화된 코드 커버리지 지표 [43]를 포함하는 프로세스를 개발했다. 프로그램 구조의 일관된 뷰를 보장하는 커널 부분은 2000년 초에 작성되어, 노랑으로 설계 리뷰를 받고 초록으로 코드 리뷰를 받았다. 리뷰어에는 경험 없는 대학원생이 아니라 동시성 전문가가 포함되었다(Christopher Hylands(현재 Brooks), Bart Kienhuis, John Reekie, 그리고 나 자신이 모두 리뷰어였다). 우리는 100퍼센트 코드 커버리지를 달성한 회귀 테스트를 작성했다. 야간 빌드와 회귀 테스트는 2프로세서 SMP 머신에서 돌았는데, 이 머신은 모두 단일 프로세서였던 개발 머신들과 다른 스레드 동작을 보였다. Ptolemy II 시스템 자체가 널리 쓰이기 시작했고, 시스템을 쓸 때마다 이 코드가 실행되었다. 4년 후인 2004년 4월 26일 코드가 데드락에 빠질 때까지 아무 문제도 관찰되지 않았다.

우리의 비교적 엄격한 소프트웨어 공학 관행이 많은 동시성 버그를 찾아 고친 것은 분명한 사실이다. 그러나 시스템을 통째로 잠가 버리는 데드락만큼 심각한 문제가 이런 관행에도 불구하고 4년 동안 발견되지 않을 수 있었다는 사실은 섬뜩하다. 이런 문제가 얼마나 더 남아 있을까? 이런 문제를 모두 발견했다고 확신하려면 얼마나 오래 테스트해야 할까? 유감스럽게도, 나는 테스트가 복잡한 멀티스레드 코드의 문제를 결코 전부 드러내지 못할 수 있다고 결론지을 수밖에 없다.

물론 데드락을 피하는 솔깃하게 단순한 규칙들이 있다. 예를 들어, 항상 같은 순서로 락을 획득하라 [32]. 그러나 이 규칙은 실무에서 적용하기가 매우 어렵다. 널리 쓰이는 어떤 프로그래밍 언어에서도 메서드 시그니처는 그 메서드가 어떤 락을 획득하는지 알려 주지 않기 때문이다. 어떤 메서드를 확신을 갖고 호출하려면, 호출하는 모든 메서드의 소스 코드와 그 메서드들이 호출하는 모든 메서드를 검토해야 한다. 락을 메서드 시그니처의 일부로 만들어 이 언어 문제를 고친다 해도, 이 규칙은 대칭적 접근(상호작용이 어느 쪽에서든 시작될 수 있는 경우)의 구현을 극도로 어렵게 만든다. 그리고 어떤 수정도 상호 배제 락에 대한 추론이 극도로 어렵다는 문제를 피해 가지 못한다. 프로그래머가 자기 코드를 이해할 수 없다면, 그 코드는 신뢰할 수 없다.

문제가 Java가 스레드를 구현하는 방식에 있다고 결론지을 수도 있다. synchronized 키워드가 최선의 가지치기 도구가 아닐 수도 있다. 실제로 2005년에 나온 Java 5.0은 스레드 동기화를 위한 여러 다른 메커니즘을 추가했다. 이들은 분명 프로그래머가 비결정성을 가지치기할 도구 상자를 풍부하게 해 준다. 그러나 이 메커니즘들(세마포어 등)은 여전히 상당한 숙련을 요구하며, 십중팔구 미묘한 버그가 잠복한 이해 불가능한 프로그램을 낳을 것이다.

소프트웨어 공학 프로세스 개선만으로는 부족하다. 도움이 될 수 있는 또 다른 접근은 [32]와 [44]처럼 검증된 동시성 계산 디자인 패턴을 쓰는 것이다. 실제로 프로그래머의 과제가 패턴 중 하나와 명확히 일치할 때 이 패턴들은 엄청난 도움이 된다. 그러나 두 가지 어려움이 있다. 하나는 패턴의 구현이 신중한 지침이 있어도 여전히 미묘하고 까다롭다는 것이다. 프로그래머는 실수를 저지를 것이고, 구현이 패턴을 준수하는지 자동으로 확인하는 확장 가능한 기법은 없다. 더 중요한 것은, 패턴들을 결합하기 어려울 수 있다는 점이다. 패턴의 성질은 보통 합성 가능하지 않으므로, 둘 이상의 패턴을 쓰는 복잡한 프로그램은 이해 가능할 가능성이 낮다.

동시성 계산에서 패턴의 매우 흔한 용례는 데이터베이스, 특히 트랜잭션 개념에서 발견된다. 트랜잭션은 데이터 사본에 대한 추측성 비동기화 계산 뒤에 커밋 또는 중단이 따르는 방식을 지원한다. 커밋은 충돌이 없었음을 보일 수 있을 때 일어난다. 트랜잭션은 분산 하드웨어에서 지원될 수도 있고(데이터베이스에서 흔하다), 공유 메모리 머신에서 소프트웨어로 [45], 또는 가장 흥미롭게도 공유 메모리 머신에서 하드웨어로 [38] 지원될 수 있다. 후자의 경우 이 기법은 그런 머신에 어차피 필요한 캐시 일관성 프로토콜과 잘 맞물린다. 트랜잭션은 의도하지 않은 데드락을 없애지만, 합성 가능성을 위한 최근 확장 [26]에도 불구하고 여전히 고도로 비결정적인 상호작용 메커니즘이다. 트랜잭션은 본질적으로 비결정적인 상황, 예를 들어 여러 행위자가 자원을 놓고 비결정적으로 경쟁하는 상황에 잘 맞는다. 그러나 결정적인 동시적 상호작용을 만드는 데는 잘 맞지 않는다.

패턴의 특히 흥미로운 용례는 Dean과 Ghemawat이 보고한 MapReduce다 [19]. 이 패턴은 Google에서 거대한 데이터셋의 대규모 분산 처리에 쓰였다. 대부분의 패턴이 동기화를 곁들인 세밀한 공유 자료구조를 제공하는 반면, MapReduce는 대규모 분산 프로그램을 구성하는 프레임워크를 제공한다. 이 패턴은 Lisp를 비롯한 함수형 언어의 고차 함수에서 영감을 받았다. 패턴의 매개변수는 데이터 조각이 아니라 코드로 표현되는 기능 조각이다.

전문가는 패턴을 라이브러리로 캡슐화할 수 있다. Google의 MapReduce, Java 5.0의 동시성 자료구조, C++의 STAPL [1]이 그렇게 만들어졌다. 이는 구현의 신뢰성을 크게 높이지만, 모든 동시적 상호작용이 이 라이브러리를 통해서만 일어나도록 프로그래머의 규율을 요구한다. 이 라이브러리들의 기능을 문법과 의미론이 그 제약을 강제하는 언어로 접어 넣는다면, 결국 훨씬 쉽게 만들 수 있는 동시적 프로그램으로 이어질 수 있다.

MapReduce 같은 고차 패턴은 언어 설계자에게 특히 흥미로운 도전과 기회를 제공한다. 이 패턴들은 더 전통적인 프로그래밍 언어라기보다 코디네이션 언어의 수준에서 기능한다. 기존 프로그래밍 언어(Java, C++ 등)와 호환되는 새 코디네이션 언어는, 기존 언어를 대체하는 새 프로그래밍 언어보다 받아들여질 가능성이 훨씬 크다.

흔한 절충안 하나는 기존 프로그래밍 언어를 어노테이션이나 몇 개의 선택된 키워드로 확장해 동시성 계산을 지원하는 것이다. 이 절충안은 동시성이 문제가 아닌 곳에서는 기존 코드의 상당 부분을 재사용할 수 있게 하지만, 동시성을 드러내려면 재작성이 필요하다. 예를 들어 Split-C [16]와 Cilk [13]가 이 전략을 따르는데, 둘 다 멀티스레딩을 지원하는 C 계열 언어다. Cilk에서 프로그래머는 본질적으로 C 프로그램인 코드에 "cilk", "spawn", "sync" 키워드를 삽입할 수 있다. Cilk의 목표는 공유 메모리 맥락에서 동적 멀티스레딩을 지원하되, 병렬성이 실제로 활용될 때만 오버헤드가 발생하도록 하는 것이다(즉, 단일 프로세서에서는 오버헤드가 낮다).

관련된 접근으로, 더 일관되고 예측 가능한 동작을 얻기 위해 언어 확장과 기존 언어의 표현력을 제한하는 제약을 결합하는 것이 있다. 예를 들어 Guava 언어 [5]는 동기화되지 않은 객체를 여러 스레드에서 접근할 수 없도록 Java를 제약한다. 나아가 읽은 데이터의 무결성을 보장하는 락(읽기 락)과 데이터의 안전한 수정을 가능하게 하는 락(쓰기 락)의 구분을 명시적으로 만든다. 이런 언어 변경은 성능을 크게 희생하지 않으면서 상당한 비결정성을 가지치기하지만, 여전히 데드락 위험이 있다.

데드락 회피에 더 무게를 두는 또 다른 접근은 프라미스(promise)다. 예를 들어 Mark Miller가 E 프로그래밍 언어⁷에서 구현했다. 퓨처(future)라고도 불리며, 원래는 Baker와 Hewitt [27]에게서 나왔다. 여기서 프로그램은 공유 데이터에 접근하려고 블록하는 대신, 결국 얻게 될 것으로 기대하는 데이터의 프록시를 가지고 진행하며, 그 프록시를 마치 데이터 자체인 것처럼 사용한다.

또 다른 접근은 프로그래밍 언어와 동시성 표현 메커니즘은 그대로 두고, 멀티스레드 프로그램의 잠재적 동시성 버그를 식별하는 형식적 프로그램 분석을 도입한다. Blast [28]와 Intel 스레드 체커⁸가 그 예다. 이 접근은 인간이 발견하기 어려운 프로그램 동작을 드러내어 상당한 도움을 줄 수 있다. Valgrind⁹ 같은 성능 디버거처럼 덜 형식적인 기법도 비슷하게 도움이 되어, 프로그래머가 프로그램 동작의 방대한 비결정성을 정리하기 쉽게 해 준다. 형식적 기법과 비형식적 기법 모두 유망하지만, 여전히 적용에 상당한 전문성이 필요하고 확장성 한계에 시달린다.

위의 모든 기법은 스레드의 비결정성 일부를 가지치기한다. 그러나 모두 여전히 고도로 비결정적인 프로그램을 낳는다. 서버, 동시적 데이터베이스 접근, 자원 경쟁처럼 본질적으로 비결정적인 애플리케이션에는 이것이 적절하다. 그러나 비결정적 수단으로 결정적 목표를 달성하는 일은 여전히 어렵다. 결정적인 동시성 계산을 달성하려면 문제에 다르게 접근해야 한다. 스레드처럼 고도로 비결정적인 메커니즘에서 시작해 프로그래머가 그 비결정성을 가지치기하도록 의존하는 대신, 결정적이고 합성 가능한 메커니즘에서 시작해 필요한 곳에만 비결정성을 도입해야 한다. 다음으로 이 접근을 탐구한다.

⁶ http://ptolemy.eecs.berkeley.edu 참조
⁷ http://www.erights.org/ 참조
⁸ http://developer.intel.com/software/products/threading/tcwin 참조
⁹ http://valgrind.org/ 참조

## 6 스레드의 대안

그림 1과 2에 나온 옵저버 패턴을 다시 생각해 보자. 이것은 단순한(자명하다고 해도 좋은) 디자인 패턴이다 [22]. 구현도 단순해야 마땅하다. 유감스럽게도 위에서 보았듯, 스레드로 구현하기는 그리 쉽지 않다.

Ptolemy II [20]의 "Rendezvous 도메인"으로 구현한 옵저버 패턴을 보여 주는 그림 3을 보자. 왼쪽 위 "Rendezvous Director"라고 적힌 상자는 이 다이어그램이 CSP류 [29] 동시성 프로그램을 나타낸다는 어노테이션이다. 각 컴포넌트(아이콘으로 표현)는 프로세스이고, 통신은 랑데부(rendezvous)로 이루어진다. 프로세스 자체는 평범한 Java 코드로 명세되므로, 이 프레임워크는 (마침 시각적 문법을 가진) 코디네이션 언어로 보는 것이 옳다. 이 랑데부 도메인의 Ptolemy II 구현은 Reo [2]에서 영감을 받았으며, 조건부 랑데부를 명세하는 "Merge" 블록을 포함한다. 다이어그램에서 Merge 블록은 두 Value Producer 중 어느 쪽이든 Value Consumer 및 Observer와 랑데부할 수 있음을 명세한다. 즉, 발생할 수 있는 3자 랑데부 상호작용이 두 가지 있다. 이 상호작용들은 비결정적 순서로 반복해서 일어날 수 있다.

그림 3: 시각적 문법을 가진 랑데부 기반 코디네이션 언어로 구현한 옵저버 패턴.

첫째, 아이콘의 의미를 이해하고 나면 다이어그램이 옵저버 패턴을 매우 명확하게 표현한다는 점에 주목하라. 둘째, Merge 블록이 명세하는 명시적으로 비결정적인 상호작용을 제외하면 프로그램의 모든 것이 결정적임에 주목하라. 그 블록이 없다면 프로그램은 결정적 프로세스들 사이의 결정적 상호작용을 명세하게 된다. 셋째, 데드락이 없음이 증명 가능하다는 점에 주목하라(이 경우 다이어그램에 순환이 없다는 사실이 데드락 없음을 보장한다). 넷째, 다자간 랑데부는 Value Consumer와 Observer가 새 값을 같은 순서로 본다는 것을 보장한다. 옵저버 패턴이 마땅히 그래야 하듯 자명해진다.

자명한 프로그래밍 문제를 자명하게 만들었으니, 이제 흥미로운 발전을 고려할 수 있다. 그림 4는 Kahn 프로세스 네트워크(PN) 동시성 모델 [31]을 구현하는 "PN Director"로 구현한 옵저버 패턴을 보여 준다. 이 모델에서 각 아이콘은 역시 프로세스를 나타내지만, 랑데부 기반 상호작용 대신 프로세스들은 개념적으로 무한한 FIFO 큐와 블로킹 읽기(스트림)를 통한 메시지 전달로 통신한다. Kahn과 MacQueen의 원래 PN 모델 [31]에서 블로킹 읽기는 모든 네트워크가 결정적 계산을 정의하도록 보장한다. 이 경우 PN 모델은 스트림을 비결정적으로 병합하는 "NondeterministicMerge" 프리미티브로 확장되었다. PN 모델을 이런 명시적 비결정성으로 확장하는 것은 임베디드 소프트웨어 애플리케이션에서 흔하다는 점에 유의하라 [18].

그림 4: 프로세스 네트워크 동시성 모델로 구현한 옵저버 패턴.

그림 4의 새 구현은 그림 3의 구현이 가진 앞서 언급한 장점을 모두 가지면서, Observer가 Value Consumer를 따라잡을 필요가 없다는 성질이 추가된다. 통지를 큐에 쌓아 두었다가 나중에 처리할 수 있다. 스레드 기반 구현에서는 이런 질문을 던지는 지점까지 갈 가능성조차 희박하다. 옵저버 패턴을 어떤 형태로든 올바르게 만드는 데 드는 프로그래머의 노력이 이미 과도하기 때문이다.

세 번째 구현은 비결정적 병합이 나타내는 비결정성의 본질을 더 정교하게 다듬을 수 있다. 동기 언어(synchronous languages) [8]의 원칙을 써서 공정성을 보장할 수 있다. Ptolemy II에서 같은 모델을 "SR director"(synchronous/reactive)로 구현할 수 있는데, 이는 Esterel [12], SIGNAL [10], Lustre [25]와 관련된 동기적 모델을 구현한다. 이 중 마지막 것은 항공기 제어 애플리케이션을 위한 고도로 동시적이고 안전이 결정적으로 중요한 소프트웨어 설계에 성공적으로 쓰였다 [11]. 이런 애플리케이션에 스레드를 쓰는 것은 현명하지 않을 것이다.

네 번째 구현은 비결정적 이벤트의 타이밍에 초점을 맞출 수 있다. Ptolemy II에서 "DE director"(discrete events, 이산 이벤트)를 쓰는 비슷한 모델은 VHDL, Verilog 같은 하드웨어 기술 언어나 Opnet Modeler 같은 네트워크 모델링 프레임워크의 의미론과 관련된 [33] 엄밀한 의미론을 가진 시간 명세를 제공한다.

네 경우(랑데부, PN, SR, DE) 모두, 우리는 근본적으로 결정적인 (그리고 동시적인) 상호작용 메커니즘에서 출발했다(수행되는 계산이라는 의미에서 결정적이며, 처음 세 경우는 타이밍이라는 의미에서는 결정적이지 않다). 비결정성은 정확히 필요한 곳에 신중하게 도입되었다. 이 설계 스타일은 극도로 비결정적인 상호작용 메커니즘에서 출발해 원치 않는 비결정성을 가지치기하려 드는 스레드 스타일과 매우 다르다.

그림 3과 4에 나온 구현은 둘 다 Java 스레드를 사용한다. 그러나 프로그래머의 모델은 스레드와 전혀 무관하다. 앞 절에서 설명한 모든 기법과 비교하면, 계산을 통해 데이터를 흘려보낸다는 비슷한 성격을 가진 MapReduce에 가장 가깝다. 그러나 MapReduce와 달리, 광범위한 상호작용을 기술하기에 충분히 표현력 있는 엄밀한 코디네이션 언어의 지원을 받는다. 실제로 여기에는 두 개의 서로 다른 코디네이션 언어가 제시되었다. 하나는 랑데부 의미론, 다른 하나는 PN 의미론이다.

이런 스타일의 동시성은 물론 새로운 것이 아니다. 데이터가 (제어가 아니라) 컴포넌트를 통해 흐르는 컴포넌트 아키텍처는 "액터 지향(actor-oriented)" [35]이라 불려 왔다. 여러 형태가 있다. Unix 파이프는 PN과 비슷하지만, 순환 그래프를 지원하지 않는다는 점에서 더 제한적이다. MPI와 OpenMP 같은 메시지 전달 패키지는 랑데부와 PN을 구현하는 기능을 포함하지만, 결정성보다 표현력을 강조하는 덜 구조화된 맥락에서다. 이런 패키지의 순진한 사용자는 예상치 못한 비결정성에 쉽게 물릴 수 있다. Erlang 같은 언어 [4]는 메시지 전달 동시성을 범용 언어의 필수 요소로 만든다. Ada 같은 언어는 랑데부를 필수 요소로 만든다. 함수형 언어 [30]와 단일 대입(single-assignment) 언어도 결정적 계산을 강조하지만, 명시적으로 동시적이지는 않으므로 동시성을 제어하고 활용하기가 더 어려울 수 있다. 데이터 병렬 언어도 결정적 상호작용을 강조하지만, 소프트웨어의 저수준 재작성을 요구한다.

이 접근들은 모두 해법의 조각을 제공한다. 그러나 어느 하나가 주류가 될 것 같지는 않다. 다음에서 그 이유를 검토한다.

## 7 도전과 기회

사실 스레드의 대안은 오래전부터 있었지만, 여전히 스레드가 동시성 프로그래밍 언어의 지형을 지배한다. 이 대안들이 뿌리내리는 데는 실제로 많은 장애물이 있다. 아마 가장 중요한 것은 프로그래밍이라는 개념 자체와 계산의 핵심 추상화가 순차 패러다임에 깊이 뿌리박고 있다는 점이다. 가장 널리 쓰이는 프로그래밍 언어들은 (압도적으로) 이 패러다임을 고수한다. 문법적으로 스레드는 이 언어들의 사소한 확장이거나(Java처럼) 그저 외부 라이브러리다. 물론 의미론적으로는 언어의 본질적 결정론을 완전히 무너뜨린다. 유감스럽게도 프로그래머는 의미론보다 문법에 더 이끌리는 듯하다. 뿌리내린 스레드 대안들, 이를테면 MPI와 OpenMP는 바로 이 핵심 특징을 공유한다. 언어에 문법적 변화를 가하지 않는다. Erlang이나 Ada처럼 완전히 새로운 문법으로 언어를 대체하는 대안들은 뿌리내리지 못했고, 아마 앞으로도 못할 것이다. Split-C나 Cilk처럼 기존 언어에 사소한 문법 수정만 가한 언어들조차 여전히 소수 취향으로 남아 있다.

메시지는 분명하다. 기존 언어를 대체하지 말아야 한다. 그 위에 쌓아 올려야 한다. 그러나 라이브러리만으로 쌓아 올리는 것은 만족스럽지 않다. 라이브러리는 구조를 거의 제공하지 않고, 패턴을 강제하지 않으며, 합성 가능한 성질이 거의 없다.

나는 올바른 답이 코디네이션 언어라고 믿는다. 코디네이션 언어도 새 문법을 도입하지만, 그 문법은 기존 프로그래밍 언어의 목적과 직교하는 목적에 봉사한다. Erlang이나 Ada 같은 범용 동시성 언어는 산술 표현식 같은 일상적인 연산을 위한 문법까지 포함해야 하지만, 코디네이션 언어는 코디네이션 이외의 어떤 것도 명세할 필요가 없다. 그렇기에 문법이 뚜렷이 구별될 수 있다. 그림 3과 4에 나온 "프로그램"들은 액터 지향 코디네이션(데이터가 제어가 아니라 컴포넌트를 통해 흐르는)을 명세하기 위해 시각적 문법을 쓴다. 여기서 시각적 문법은 교육 목적으로만 쓰였지만, UML의 특정 부분이 객체지향 프로그래밍에 대해 그랬듯이, 언젠가 이런 시각적 문법이 확장 가능하고 효과적으로 만들어질 수 있다고 생각할 수 있다. 그렇게 되지 않더라도, 같은 구조를 명세하는 확장 가능한 텍스트 문법을 상상하기는 쉽다.

물론 코디네이션 언어조차 오래전부터 있었다 [40]. 이들 역시 뿌리내리지 못했다. 그 이유 중 하나는, 코디네이션 언어를 받아들이는 것이 핵심 전선 하나에서의 항복을 뜻하기 때문이다. 바로 동질성(homogeneity)이다. 프로그래밍 언어 연구에 흐르는 지배적 저류는, 가치 있는 프로그래밍 언어라면 범용이어야 한다는 것이다. 최소한 자기 자신의 컴파일러를 표현할 만큼은 표현력이 있어야 한다. 그리고 그 언어의 신봉자가 다른 언어를 쓰는 데 굴복하면 배신자로 간주된다. 언어 전쟁은 종교 전쟁이며, 이 종교들 가운데 다신교는 드물다.

그러나 핵심적인 발전 하나가 얼음을 깼다. 각각 시각적 문법을 가진 언어들의 가족으로 보는 것이 옳은 UML은 C++ 및 Java와 일상적으로 결합된다. 프로그래머들은 상보적인 기능을 서로 다른 언어가 제공하는, 둘 이상의 언어 사용에 익숙해지기 시작했다. 그림 3과 4의 프로그램은 이 정신을 따른다. 이들은 세밀한 계산과는 상당히 직교하는 대규모 구조를 다이어그램으로 명세한다.

Kahn 프로세스 네트워크, CSP, 데이터플로처럼 스레드보다 강한 결정론을 가진 동시성 모델도 오래전부터 있었다. 일부는 Occam [21](CSP 기반) 같은 프로그래밍 언어로 이어졌고, 일부는 YAPI [18] 같은 도메인 특화 프레임워크로 이어졌다. 그러나 대부분은 주로 정교한 프로세스 대수를 만드는 데 쓰였고, 주류 프로그래밍에는 큰 영향을 주지 못했다. 나는 이 동시성 모델들이 대체 언어가 아니라 코디네이션 언어를 정의하는 데 쓰인다면 상황이 달라질 수 있다고 믿는다.

이 길에는 많은 도전이 있다. 좋은 코디네이션 언어를 설계하는 일은 좋은 범용 언어를 설계하는 일만큼 어렵고, 함정으로 가득하다. 예를 들어 소수의 간결한 프리미티브가 주는 거짓 우아함에 빠지기 쉽다. 범용 언어에서는 일곱 개의 프리미티브면 충분하다는 것이 잘 알려져 있지만 [48], 이 프리미티브들 위에 만들어진 진지한 프로그래밍 언어는 없다.

그림 5는 단순한 동시성 계산의 두 구현을 보여 준다. 위쪽 프로그램은 [3]의 예제를 각색한 것으로, Data Source 1과 Data Source 2의 연속 출력이 결정적으로 인터리빙되어 Display 블록에 번갈아 나타난다. 그러나 이 꽤 단순한 기능이 상당히 복잡한 방식으로 달성된다. 사실 어떻게 동작하는지 이해하는 것 자체가 약간의 퍼즐이다.¹⁰ 반면 아래쪽 프로그램은 쉽게 이해된다. Commutator 블록이 입력 각각과 위에서 아래 순서로 랑데부를 수행하여 같은 인터리빙을 달성한다. "언어" 프리미티브의 신중한 선택은 결정적 목표의 단순하고 직접적이며 결정적인 표현을 가능하게 한다. 위쪽 모델은 더 표현력 있긴 하지만 비결정적인 메커니즘으로 결정적 목표를 달성한다. 그래서 훨씬 더 난해하다.

그림 5: 랑데부로 결정적 인터리빙을 달성하는 두 가지 방법.

물론 코디네이션 언어는 기존 언어에 준하는 확장성과 모듈성 기능을 발전시켜야 한다. 이는 가능하다. 예를 들어 Ptolemy II는 코디네이션 언어 수준에서 정교하고 현대적인 타입 시스템을 제공한다 [49]. 나아가 객체지향 기법에서 각색한 상속과 다형성의 예비적 형태도 제공한다 [35]. 고차 함수 개념을 코디네이션 언어에 각색하여 코디네이션 언어 수준에서 MapReduce 같은 구성물을 제공하는 데 거대한 기회가 있다. 이 방향의 매우 유망한 초기 연구가 Reekie [42]에게서 나왔다.

더 도전적인 장기적 기회는, 동시성 계산에 더 나은 기초를 제공하도록 계산 이론을 각색하는 것이다. 이 방향으로 상당한 진전이 있었지만, 나는 할 일이 훨씬 더 많다고 믿는다. 3절에서 순차 계산은 비트 시퀀스를 비트 시퀀스로 사상하는 함수로 모델링되었다. 대응하는 동시성 모델이 [36]에서 탐구되었는데, 함수

f: B** → B**

대신 동시성 계산은 함수

f: (T → B**) → (T → B**)

로 주어진다. 위에서 T는 부분 순서 또는 전 순서가 부여된 태그의 집합이며, 그 순서는 시간, 인과성, 또는 더 추상적인 의존 관계를 나타낼 수 있다. 이렇게 바라본 계산은 진화하는 비트 패턴을 진화하는 비트 패턴으로 사상한다. 이 기본 정식화는 많은 동시성 계산 모델에 적용 가능함이 입증되었다 [9, 14, 37].

¹⁰ Barrier 액터는 자신의 모든 입력이 같은 랑데부 상호작용에 참여하도록 보장한다. Merge 액터는 앞에서 설명했다. 용량이 1인 Buffer 액터는 처음에 비어 있어서 입력과 기꺼이 랑데부한다. 가득 차면 출력과만 랑데부하려 한다. 종합하면, 이 모델은 두 다자간 랑데부 상호작용 중 하나가 번갈아 일어나도록 허용한다. 첫 번째는 두 Data Source 액터, Buffer, Display가 참여한다. 두 번째는 Buffer와 Display만 참여한다.

## 8 결론

소프트웨어의 동시성은 어렵다. 그러나 이 어려움의 많은 부분은 우리가 선택한 동시성 추상화의 결과다. 오늘날 범용 컴퓨팅에서 지배적으로 쓰이는 것은 스레드다. 그러나 복잡한 멀티스레드 프로그램은 인간이 이해할 수 없다. 디자인 패턴, 더 나은 원자성 단위(예: 트랜잭션), 개선된 언어, 형식적 방법으로 프로그래밍 모델을 개선할 수 있는 것은 사실이다. 그러나 이 기법들은 스레딩 모델의 불필요하게 거대한 비결정성을 조금씩 깎아낼 뿐이다. 모델은 본질적으로 다루기 힘든 채로 남는다.

동시성 프로그래밍이 주류가 되기를 기대하고 프로그램에 신뢰성과 예측 가능성을 요구한다면, 프로그래밍 모델로서의 스레드를 폐기해야 한다. 스레드보다 훨씬 예측 가능하고 이해 가능한 동시성 프로그래밍 모델을 만들 수 있다. 그 모델들은 매우 단순한 원칙에 기반한다. 결정적 목적은 결정적 수단으로 달성해야 한다는 것이다. 비결정성은 필요한 곳에 신중하고 조심스럽게 도입해야 하며, 프로그램에서 명시적이어야 한다. 이 원칙은 자명해 보이지만, 스레드는 이를 달성하지 못한다. 스레드는 컴퓨팅의 기관실로 밀려나, 전문 기술 제공자만 감내하는 존재가 되어야 한다.

## 참고문헌

1. P. An, A. Jula, S. Rus, S. Saunders, T. Smith, G. Tanase, N. Thomas, N. Amato, and L. Rauchwerger. STAPL: An adaptive, generic parallel C++ library. In Wkshp. on Lang. and Comp. for Par. Comp. (LCPC), pages 193–208, Cumberland Falls, Kentucky, 2001.
2. F. Arbab. Reo: A channel-based coordination model for component composition. Mathematical Structures in Computer Science, 14(3):329–366, 2004.
3. F. Arbab. A behavioral model for composition of software components. L'Object, to appear, 2006.
4. J. Armstrong, R. Virding, C. Wikstrom, and M. Williams. Concurrent programming in Erlang. Prentice Hall, second edition, 1996.
5. D. F. Bacon, R. E. Strom, and A. Tarafdar. Guava: a dialect of Java without data races. In ACM SIGPLAN conference on Object-oriented programming, systems, languages, and applications, volume 35 of ACM SIGPLAN Notices, pages 382–400, 2000.
6. U. Banerjee, R. Eigenmann, A. Nicolau, and D. Padua. Automatic program parallelization. Proceedings of the IEEE, 81(2):211–243, 1993.
7. L. A. Barroso. The price of performance. ACM Queue, 3(7), 2005.
8. A. Benveniste and G. Berry. The synchronous approach to reactive and real-time systems. Proceedings of the IEEE, 79(9):1270–1282, 1991.
9. A. Benveniste, L. Carloni, P. Caspi, and A. Sangiovanni-Vincentelli. Heterogeneous reactive systems modeling and correct-by-construction deployment. In EMSOFT. Springer, 2003.
10. A. Benveniste and P. L. Guernic. Hybrid dynamical systems theory and the signal language. IEEE Tr. on Automatic Control, 35(5):525–546, 1990.
11. G. Berry. The effectiveness of synchronous languages for the development of safety-critical systems. White paper, Esterel Technologies, 2003.
12. G. Berry and G. Gonthier. The esterel synchronous programming language: Design, semantics, implementation. Science of Computer Programming, 19(2):87–152, 1992.
13. R. D. Blumofe, C. F. Joerg, B. C. Kuszmaul, C. E. Leiserson, K. H. Randall, and Y. Zhou. Cilk: an efficient multithreaded runtime system. In ACM SIGPLAN symposium on Principles and Practice of Parallel Programming (PPoPP), ACM SIGPLAN Notices, 1995.
14. J. R. Burch, R. Passerone, and A. L. Sangiovanni-Vincentelli. Notes on agent algebras. Technical Report UCB/ERL M03/38, University of California, November 2003.
15. M. Creeger. Multicore CPUs for the masses. ACM Queue, 3(7), 2005.
16. D. E. Culler, A. Dusseau, S. C. Goldstein, A. Krishnamurthy, S. Lumetta, T. v. Eicken, and K. Yelick. Parallel programming in Split-C. In Supercomputing, Portland, OR, 1993.
17. B. A. Davey and H. A. Priestly. Introduction to Lattices and Order. Cambridge University Press, 1990.
18. E. A. de Kock, G. Essink, W. J. M. Smits, P. van der Wolf, J.-Y. Brunel, W. Kruijtzer, P. Lieverse, and K. A. Vissers. Yapi: Application modeling for signal processing systems. In 37th Design Automation Conference (DAC'00), pages 402–405, Los Angeles, CA, 2000.
19. J. Dean and S. Ghemawat. MapReduce: Simplified data processing on large clusters. In Sixth Symposium on Operating System Design and Implementation (OSDI), San Francisco, CA, 2004.
20. J. Eker, J. W. Janneck, E. A. Lee, J. Liu, X. Liu, J. Ludvig, S. Neuendorffer, S. Sachs, and Y. Xiong. Taming heterogeneity—the Ptolemy approach. Proceedings of the IEEE, 91(2), 2003.
21. J. Galletly. Occam-2. University College London Press, 2nd edition, 1996.
22. E. Gamma, R. Helm, R. Johnson, and J. Vlissides. Design Patterns: Elements of Reusable Object-Oriented Software. Addison Wesley, 1994.
23. A. Geist, A. Beguelin, J. Dongarra, W. Jiang, B. Manchek, and V. Sunderam. PVM: Parallel Virtual Machine — A Users Guide and Tutorial for Network Parallel Computing. MIT Press, Cambridge, MA, 1994.
24. A. Gontmakher and A. Schuster. Java consistency: nonoperational characterizations for Java memory behavior. ACM Trans. Comput. Syst., 18(4):333–386, 2000.
25. N. Halbwachs, P. Caspi, P. Raymond, and D. Pilaud. The synchronous data flow programming language lustre. Proceedings of the IEEE, 79(9):1305–1319, 1991.
26. T. Harris, S. Marlow, S. P. Jones, and M. Herlihy. Composable memory transactions. In ACM Conference on Principles and Practice of Parallel Programming (PPoPP), Chicago, IL, 2005.
27. J. Henry G. Baker and C. Hewitt. The incremental garbage collection of processes. In Proceedings of the Symposium on AI and Programming Languages, volume 12 of ACM SIGPLAN Notices, pages 55–59, 1977.
28. T. A. Henzinger, R. Jhala, R. Majumdar, and S. Qadeer. Thread-modular abstraction refinement. In 15th International Conference on Computer-Aided Verification (CAV), volume 2725 of Lecture Notes in Computer Science, pages 262–274. Springer-Verlag, 2003.
29. C. A. R. Hoare. Communicating sequential processes. Communications of the ACM, 21(8), 1978.
30. P. Hudak. Conception, evolution, and application of functional programming languages. ACM Computing Surveys, 21(3), 1989.
31. G. Kahn and D. B. MacQueen. Coroutines and networks of parallel processes. In B. Gilchrist, editor, Information Processing. North-Holland Publishing Co., 1977.
32. D. Lea. Concurrent Programming in Java: Design Principles and Patterns. Addison-Wesley, Reading MA, 1997.
33. E. A. Lee. Modeling concurrent real-time processes using discrete events. Annals of Software Engineering, 7:25–45, 1999.
34. E. A. Lee. What's ahead for embedded software? IEEE Computer Magazine, pages 18–26, 2000.
35. E. A. Lee and S. Neuendorffer. Classes and subclasses in actor-oriented design. In Conference on Formal Methods and Models for Codesign (MEMOCODE), San Diego, CA, USA, 2004.
36. E. A. Lee and A. Sangiovanni-Vincentelli. A framework for comparing models of computation. IEEE Transactions on CAD, 17(12), 1998.
37. X. Liu. Semantic foundation of the tagged signal model. Phd thesis, EECS Department, University of California, December 20 2005.
38. H. Maurice and J. E. B. Moss. Transactional memory: architectural support for lock-free data structures. In Proceedings of the 20th annual international symposium on Computer architecture, pages 289–300, San Diego, California, United States, 1993. ACM Press.
39. Message Passing Interface Forum. MPI2: A message passing interface standard. International Journal of High Performance Computing Applications, 12(1-2):1–299, 1998.
40. G. Papadopoulos and F. Arbab. Coordination models and languages. In M. Zelkowitz, editor, Advances in Computers - The Engineering of Large Systems, volume 46, pages 329–400. Academic Press, 1998.
41. W. Pugh. Fixing the Java memory model. In Proceedings of the ACM 1999 conference on Java Grande, pages 89–98, San Francisco, California, United States, 1999. ACM Press.
42. H. J. Reekie. Toward effective programming for parallel digital signal processing. Ph.D. Thesis Research Report 92.1, University of Technology, Sydney, 1992.
43. H. J. Reekie, S. Neuendorffer, C. Hylands, and E. A. Lee. Software practice in the Ptolemy project. Technical Report Series GSRC-TR-1999-01, Gigascale Semiconductor Research Center, University of California, Berkeley, April 1999.
44. D. C. Schmidt, M. Stal, H. Rohnert, and F. Buschmann. Pattern-Oriented Software Architecture - Patterns for Concurrent and Networked Objects. Wiley, 2000.
45. N. Shavit and D. Touitou. Software transactional memory. In ACM symposium on Principles of Distributed Computing, pages 204–213, Ottowa, Ontario, Canada, 1995. ACM Press.
46. L. A. Stein. Challenging the computational metaphor: Implications for how we think. Cybernetics and Systems, 30(6), 1999.
47. H. Sutter and J. Larus. Software and the concurrency revolution. ACM Queue, 3(7), 2005.
48. A. M. Turing. Computability and λ-definability. Journal of Symbolic Logic, 2:153–163, 1937.
49. Y. Xiong. An extensible type system for component-based design. Ph.D. Thesis Technical Memorandum UCB/ERL M02/13, University of California, Berkeley, CA 94720, May 1 2002.
