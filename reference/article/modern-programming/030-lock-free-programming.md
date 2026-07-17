# 락프리 프로그래밍 입문

> 원문: [An Introduction to Lock-Free Programming](https://preshing.com/20120612/an-introduction-to-lock-free-programming/)
> 저자: Jeff Preshing | 번역일: 2026-07-10

락프리 프로그래밍은 도전적인 분야다. 작업 자체의 복잡성 때문이기도 하지만, 애초에 이 주제에 발을 들이는 것부터가 어렵기 때문이다.

나는 운이 좋게도 락프리(lockless라고도 부른다) 프로그래밍을 Bruce Dawson의 훌륭하고 포괄적인 백서 [Lockless Programming Considerations](http://msdn.microsoft.com/en-us/library/windows/desktop/ee418650(v=vs.85).aspx)로 처음 접했다. 그리고 많은 이들처럼, Xbox 360 같은 플랫폼에서 락프리 코드를 개발하고 디버깅하며 Bruce의 조언을 실전에 적용할 기회가 있었다.

그 뒤로 추상적인 이론과 정확성 증명부터 실용적인 예제와 하드웨어 세부 사항까지 좋은 자료가 많이 나왔다. 참고 문헌 목록은 각주에 남기겠다. 자료들의 내용이 서로 어긋나 보일 때도 있다. 예를 들어 어떤 자료는 [순차 일관성(sequential consistency)](http://en.wikipedia.org/wiki/Sequential_consistency)을 전제하여, 락프리 C/C++ 코드를 흔히 괴롭히는 메모리 순서(memory ordering) 문제를 비켜 간다. 새로운 [C++11 원자적 라이브러리 표준](http://en.cppreference.com/w/cpp/atomic)은 여기에 또 하나의 변수를 던져, 많은 이들이 락프리 알고리즘을 표현해 온 방식에 도전한다.

이 글에서는 락프리 프로그래밍을 다시 소개하려 한다. 먼저 정의를 내리고, 대부분의 정보를 몇 가지 핵심 개념으로 추려낸다. 그 개념들이 서로 어떻게 연결되는지 순서도로 보여준 뒤, 세부 사항에도 발끝을 살짝 담가 보겠다. 최소한, 락프리 프로그래밍에 뛰어드는 프로그래머라면 뮤텍스와 세마포어, 이벤트 같은 고수준 동기화 객체로 올바른 멀티스레드 코드를 작성하는 법은 이미 알고 있어야 한다.

## 락프리란 무엇인가?

사람들은 흔히 락프리 프로그래밍을 뮤텍스([락](http://preshing.com/20111118/locks-arent-slow-lock-contention-is)이라고도 부른다) 없이 하는 프로그래밍이라고 설명한다. 맞는 말이지만 이야기의 일부일 뿐이다. 학술 문헌에 기반한 일반적으로 받아들여지는 정의는 조금 더 넓다. 본질적으로 락프리는 어떤 코드가 실제로 어떻게 작성되었는지에 대해 많은 것을 말하지 않으면서, 그 코드를 기술하는 데 쓰이는 하나의 속성이다.

기본적으로, 프로그램의 어떤 부분이 다음 조건들을 만족하면 그 부분은 락프리라고 부를 자격이 있다. 반대로 어떤 부분이 이 조건들을 만족하지 않으면 그 부분은 락프리가 아니다.

![](https://preshing.com/images/its-lock-free.png)

이런 의미에서 락프리의 *락*은 뮤텍스를 직접 가리키는 게 아니라, 데드락이든 라이브락이든 — 심지어 당신의 최악의 적이 내리는 가상의 스레드 스케줄링 결정 때문이든 — 애플리케이션 전체가 어떤 식으로든 "잠겨 버릴(locking up)" 가능성을 가리킨다. 마지막 항목은 우습게 들리지만 핵심이다. 공유 뮤텍스는 이 기준에서 곧바로 탈락한다. 한 스레드가 뮤텍스를 획득하는 순간, 당신의 최악의 적이 그 스레드를 다시는 스케줄하지 않으면 그만이기 때문이다. 물론 실제 운영체제는 그렇게 동작하지 않는다. 우리는 단지 용어를 정의하고 있을 뿐이다.

뮤텍스는 전혀 쓰지 않지만 그래도 락프리가 아닌 연산의 간단한 예를 보자. 처음에 X = 0이다. 독자를 위한 연습 문제로, 두 스레드가 어떻게 스케줄되면 어느 스레드도 루프를 빠져나가지 못하는지 생각해 보라.

```cpp
while (X == 0)
{
    X = 1 - X;
}
```

큰 애플리케이션 전체가 락프리이기를 기대하는 사람은 없다. 보통은 전체 코드베이스에서 특정 락프리 연산의 집합을 골라낸다. 예를 들어 락프리 큐라면 `push`, `pop`, 어쩌면 `isEmpty` 같은 소수의 락프리 연산이 있을 것이다.

[![](https://preshing.com/images/art-of-multiprocessor.png)](http://www.amazon.com/gp/product/0123973376/ref=as_li_ss_tl?ie=UTF8&tag=preshonprogr-20&linkCode=as2&camp=1789&creative=390957&creativeASIN=0123973376)[The Art of Multiprocessor Programming](http://www.amazon.com/gp/product/0123973376/ref=as_li_ss_tl?ie=UTF8&tag=preshonprogr-20&linkCode=as2&camp=1789&creative=390957&creativeASIN=0123973376)의 저자 Herlihy와 Shavit은 이런 연산들을 클래스 메서드로 표현하는 편이며, 락프리에 대한 다음과 같은 간결한 정의를 제시한다([슬라이드 150](https://docs.google.com/viewer?a=v&q=cache:HaWgz4g5e7QJ:www.elsevierdirect.com/companions/9780123705914/Lecture%2520Slides/05~Chapter_05.ppt+&hl=en&gl=ca&pid=bl&srcid=ADGEESghbD6JBTSkCnlPP8ZjPwxS2kM6bbvEGUJaHozCN1CGYW0hnR0WkwmG7LvVj5BUOYZTfTXUClM7uXmr-nXPYlOvZulPJMgYXHaXqqo_m9qkn38gw8qMn01tFoxTmTkvjalHzQOB&sig=AHIEtbRChU00kpYARLAr5Cv5Z5aB2NLo5w) 참조): "무한한 실행에서, 무한히 자주 어떤 메서드 호출이 완료된다." 다시 말해, 프로그램이 그 락프리 연산들을 계속 *호출*할 수만 있다면, *완료된* 호출의 수는 무슨 일이 있어도 계속 늘어난다. 그 연산들이 진행되는 동안 시스템이 잠겨 버리는 것은 알고리즘적으로 불가능하다.

락프리 프로그래밍의 중요한 귀결 하나는, 스레드 하나를 일시 정지시켜도 나머지 스레드들이 각자의 락프리 연산을 통해 집단으로서 전진하는 것을 결코 막지 못한다는 점이다. 이는 인터럽트 핸들러와 실시간 시스템처럼, 프로그램의 나머지 부분이 어떤 상태이든 특정 작업이 정해진 시간 안에 완료되어야 하는 곳에서 락프리 프로그래밍이 갖는 가치를 시사한다.

마지막으로 하나 짚어 두자면, 블록하도록 *설계된* 연산이 있다고 해서 알고리즘이 자격을 잃는 것은 아니다. 예를 들어 큐의 pop 연산은 큐가 비어 있을 때 의도적으로 블록할 수 있다. 나머지 코드 경로는 여전히 락프리로 간주할 수 있다.

## 락프리 프로그래밍 기법

락프리 프로그래밍의 논블로킹 조건을 만족시키려고 하다 보면 원자적 연산, 메모리 배리어, ABA 문제 회피 등 일련의 기법들이 자연스럽게 따라 나온다. 여기서부터 상황이 급격히 악마적으로 변한다.

그렇다면 이 기법들은 서로 어떻게 연결될까? 이를 보여주기 위해 다음 순서도를 만들었다. 각각을 아래에서 자세히 설명하겠다.

![](https://preshing.com/images/techniques.png)

### 원자적 읽기-수정-쓰기 연산

원자적 연산이란 나눌 수 없는 것처럼 보이는 방식으로 메모리를 조작하는 연산이다. 어떤 스레드도 그 연산이 절반만 완료된 상태를 관찰할 수 없다. 현대 프로세서에서는 많은 연산이 이미 원자적이다. 예를 들어 단순 타입의 정렬된(aligned) 읽기와 쓰기는 보통 원자적이다.

![](https://preshing.com/images/rmw-turnstile-2.png)[읽기-수정-쓰기(read-modify-write)](http://en.wikipedia.org/wiki/Read-modify-write)(RMW) 연산은 한 걸음 더 나아가, 더 복잡한 트랜잭션을 원자적으로 수행할 수 있게 해준다. 락프리 알고리즘이 여러 기록자(writer)를 지원해야 할 때 특히 유용한데, 여러 스레드가 같은 주소에 RMW를 시도하면 사실상 한 줄로 서서 그 연산들을 하나씩 차례로 실행하기 때문이다. 이 블로그에서 이미 RMW 연산을 다룬 적이 있다. [경량 뮤텍스](http://preshing.com/20120226/roll-your-own-lightweight-mutex), [재귀 뮤텍스](http://preshing.com/20120305/implementing-a-recursive-mutex), [경량 로깅 시스템](http://preshing.com/20120522/lightweight-in-memory-logging)을 구현할 때였다.

RMW 연산의 예로는 Win32의 [`_InterlockedIncrement`](http://msdn.microsoft.com/en-us/library/2ddez55b(v=vs.90).aspx), iOS의 [`OSAtomicAdd32`](http://developer.apple.com/library/ios/#DOCUMENTATION/System/Conceptual/ManPages_iPhoneOS/man3/OSAtomicAdd32.3.html), C++11의 [`std::atomic<int>::fetch_add`](http://www.stdthread.co.uk/doc/headers/atomic/atomic/specializations/integral/fetch_add.html)가 있다. 주의할 점은 C++11 원자적 표준이 모든 플랫폼에서 구현이 락프리라고 보장하지는 않는다는 것이다. 따라서 자신의 플랫폼과 툴체인의 능력을 알아두는 것이 좋다. [`std::atomic<>::is_lock_free`](http://www.stdthread.co.uk/doc/headers/atomic/atomic/specializations/integral/is_lock_free.html)를 호출해 확인할 수 있다.

CPU 계열마다 [RMW를 지원하는 방식이 다르다](http://jfdube.wordpress.com/2011/11/30/understanding-atomic-operations/). PowerPC와 ARM 같은 프로세서는 [load-link/store-conditional](http://en.wikipedia.org/wiki/Load-link/store-conditional) 명령어를 노출하는데, 이를 이용하면 저수준에서 자신만의 RMW 프리미티브를 구현할 수 있다. 다만 이렇게까지 하는 경우는 흔치 않다. 일반적인 RMW 연산으로 대개 충분하다.

순서도가 보여주듯, 원자적 RMW는 단일 프로세서 시스템에서도 락프리 프로그래밍의 필수 요소다. 원자성이 없으면 스레드가 트랜잭션 도중에 중단될 수 있고, 이는 일관성 없는 상태로 이어질 수 있다.

### Compare-And-Swap 루프

아마 가장 자주 논의되는 RMW 연산은 [compare-and-swap](http://en.wikipedia.org/wiki/Compare-and-swap)(CAS)일 것이다. Win32에서 CAS는 [`_InterlockedCompareExchange`](http://msdn.microsoft.com/en-us/library/ttk2z1ws.aspx) 같은 인트린식 계열로 제공된다. 프로그래머는 흔히 트랜잭션을 반복해서 시도하기 위해 compare-and-swap을 루프 안에서 수행한다. 이 패턴은 보통 공유 변수를 지역 변수로 복사하고, 어떤 투기적(speculative) 작업을 수행한 뒤, CAS로 변경 사항 게시를 시도하는 식이다:

```cpp
void LockFreeQueue::push(Node* newHead)
{
    for (;;)
    {
        // 공유 변수(m_Head)를 지역 변수로 복사한다.
        Node* oldHead = m_Head;

        // 투기적 작업을 수행한다. 아직 다른 스레드에게는 보이지 않는다.
        newHead->next = oldHead;

        // 다음으로, 변경 사항을 공유 변수에 게시하려고 시도한다.
        // 공유 변수가 바뀌지 않았다면 CAS가 성공하고 반환한다.
        // 그렇지 않으면 반복한다.
        if (_InterlockedCompareExchange(&m_Head, newHead, oldHead) == oldHead)
            return;
    }
}
```

이런 루프도 여전히 락프리 자격이 있다. 한 스레드에서 검사가 실패했다는 것은 다른 스레드에서는 성공했음이 틀림없다는 뜻이기 때문이다 — 다만 일부 아키텍처는 그것이 반드시 참은 아닌 [더 약한 변종의 CAS](http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2008/n2748.html)를 제공한다. CAS 루프를 구현할 때는 언제나 [ABA 문제](http://en.wikipedia.org/wiki/ABA_problem)를 피하도록 각별히 주의해야 한다.

### 순차 일관성

순차 일관성이란 모든 스레드가 메모리 연산이 일어난 순서에 합의하며, 그 순서가 프로그램 소스 코드의 연산 순서와 일치한다는 뜻이다. 순차 일관성 아래에서는 [이전 글에서 시연했던 것](http://preshing.com/20120515/memory-reordering-caught-in-the-act) 같은 메모리 재배열 장난을 경험하는 것이 불가능하다.

순차 일관성을 달성하는 간단한(하지만 명백히 비실용적인) 방법 하나는 컴파일러 최적화를 끄고 모든 스레드를 단일 프로세서에서 강제로 실행하는 것이다. 프로세서는 스레드가 임의의 시점에 선점되고 스케줄되더라도 자기 자신의 메모리 효과를 순서가 뒤바뀐 채로 보는 일이 결코 없다.

일부 프로그래밍 언어는 멀티프로세서 환경에서 실행되는 최적화된 코드에 대해서도 순차 일관성을 제공한다. C++11에서는 모든 공유 변수를 기본 메모리 순서 제약을 가진 C++11 원자적 타입으로 선언하면 된다. Java에서는 모든 공유 변수를 `volatile`로 표시하면 된다. 다음은 [이전 글](http://preshing.com/20120515/memory-reordering-caught-in-the-act)의 예제를 C++11 스타일로 다시 쓴 것이다:

```cpp
std::atomic<int> X(0), Y(0);
int r1, r2;

void thread1()
{
    X.store(1);
    r1 = Y.load();
}

void thread2()
{
    Y.store(1);
    r2 = X.load();
}
```

C++11 원자적 타입은 순차 일관성을 보장하므로 r1 = r2 = 0이라는 결과는 불가능하다. 이를 달성하기 위해 컴파일러는 보이지 않는 곳에서 추가 명령어를 출력한다 — 보통 메모리 펜스나 RMW 연산이다. 이 추가 명령어 때문에, 프로그래머가 메모리 순서를 직접 다룬 구현에 비해 덜 효율적일 수 있다.

### 메모리 순서

순서도가 시사하듯, 멀티코어(또는 어떤 [대칭형 멀티프로세서](http://en.wikipedia.org/wiki/Symmetric_multiprocessing)든)를 대상으로 락프리 프로그래밍을 하는데 환경이 순차 일관성을 보장하지 않는다면, 언제나 [메모리 재배열](http://preshing.com/20120515/memory-reordering-caught-in-the-act)을 어떻게 막을지 고민해야 한다.

오늘날의 아키텍처에서 올바른 메모리 순서를 강제하는 도구는 대체로 세 범주로 나뉘며, 이들은 [컴파일러 재배열](http://preshing.com/20120625/memory-ordering-at-compile-time)과 [프로세서 재배열](http://preshing.com/20120710/memory-barriers-are-like-source-control-operations)을 모두 막는다:

- 경량 sync 또는 fence 명령어. [이후 글](http://preshing.com/20120913/acquire-and-release-semantics)에서 다루겠다;
- 전체 메모리 펜스 명령어. [이전에 시연한 적](http://preshing.com/20120522/lightweight-in-memory-logging)이 있다;
- acquire 또는 release 시맨틱을 제공하는 메모리 연산.

acquire 시맨틱은 프로그램 순서상 그 뒤에 오는 연산들의 메모리 재배열을 막고, release 시맨틱은 그 앞에 오는 연산들의 메모리 재배열을 막는다. 이 시맨틱들은 한 스레드가 정보를 게시하고 다른 스레드가 그것을 읽는 생산자/소비자 관계가 있을 때 특히 적합하다. 이 역시 [이후 글](http://preshing.com/20120913/acquire-and-release-semantics)에서 더 이야기하겠다.

### 프로세서마다 메모리 모델이 다르다

메모리 재배열에 관해서는 [CPU 계열마다 습성이 다르다](http://www.linuxjournal.com/node/8212/print). 규칙은 각 CPU 벤더가 문서화하며 하드웨어가 엄격히 따른다. 예를 들어 PowerPC와 ARM 프로세서는 메모리 저장(store)의 순서를 명령어 순서와 다르게 바꿀 수 있지만, Intel과 AMD의 x86/64 계열 프로세서는 보통 그러지 않는다. 앞의 프로세서들이 더 [느슨한(relaxed) 메모리 모델](http://preshing.com/20120930/weak-vs-strong-memory-models)을 갖는다고 말한다.

이런 플랫폼별 세부 사항을 추상화하고 싶은 유혹이 있다. 특히 C++11이 이식 가능한 락프리 코드를 작성하는 표준적인 방법을 제공하는 지금은 더 그렇다. 하지만 현재로서는 대부분의 락프리 프로그래머가 플랫폼 간 차이를 어느 정도는 체감하고 있다고 생각한다. 기억해야 할 핵심 차이가 하나 있다면, x86/64 명령어 수준에서는 메모리에서의 모든 load가 acquire 시맨틱을 동반하고 메모리로의 모든 store가 release 시맨틱을 제공한다는 것이다 — 적어도 SSE가 아닌 명령어와 write-combined가 아닌 메모리에 한해서는. 그 결과, 과거에는 x86/64에서는 동작하지만 [다른 프로세서에서는 실패하는](http://www.drdobbs.com/parallel/208801974) 락프리 코드를 작성하는 일이 흔했다.

프로세서가 어떻게, 왜 메모리 재배열을 수행하는지 하드웨어 세부 사항에 관심이 있다면 [Is Parallel Programming Hard](http://kernel.org/pub/linux/kernel/people/paulmck/perfbook/perfbook.2011.01.02a.pdf)의 부록 C를 추천한다. 어떤 경우든, 메모리 재배열은 컴파일러의 명령어 재배열 때문에도 일어날 수 있다는 점을 기억하라.

이 글에서는 락프리 프로그래밍의 실용적인 측면, 이를테면 언제 하는가? 얼마나 필요한가? 같은 문제는 별로 이야기하지 않았다. 락프리 알고리즘 검증의 중요성도 언급하지 않았다. 그래도 일부 독자에게는 이 입문이 락프리 개념에 대한 기본적인 친숙함을 제공해서, 추가 자료를 읽을 때 너무 어리둥절하지 않고 나아갈 수 있기를 바란다. 늘 그렇듯, 부정확한 부분을 발견하면 댓글로 알려 달라.

*[이 글은 [Hacker Monthly](http://hackermonthly.com/issue-29.html) 29호에 실렸다.]*

## 추가 참고 자료

- [![](https://preshing.com/images/concurrency-in-action.png)](http://www.amazon.com/gp/product/1933988770/ref=as_li_ss_tl?ie=UTF8&tag=preshonprogr-20&linkCode=as2&camp=1789&creative=390957&creativeASIN=1933988770)[Anthony Williams의 블로그](http://www.justsoftwaresolutions.co.uk/blog/)와 그의 책 [C++ Concurrency in Action](http://www.amazon.com/gp/product/1933988770/ref=as_li_ss_tl?ie=UTF8&tag=preshonprogr-20&linkCode=as2&camp=1789&creative=390957&creativeASIN=1933988770)
- [Dmitriy V'jukov의 웹사이트](http://www.1024cores.net/)와 여러 [포럼 토론](https://groups.google.com/forum/?fromgroups#!forum/lock-free)
- [Bartosz Milewski의 블로그](http://bartoszmilewski.com/)
- Charles Bloom의 블로그에 있는 [Low-Level Threading 시리즈](http://cbloomrants.blogspot.ca/2012/06/06-12-12-another-threading-post-index.html)
- Doug Lea의 [JSR-133 Cookbook](http://g.oswego.edu/dl/jmm/cookbook.html)
- Howells와 McKenney의 [memory-barriers.txt](http://www.kernel.org/doc/Documentation/memory-barriers.txt) 문서
- Hans Boehm의 C++11 메모리 모델 관련 [링크 모음](http://www.hpl.hp.com/personal/Hans_Boehm/c++mm/)
- Herb Sutter의 [Effective Concurrency](http://www.gotw.ca/publications/) 시리즈
