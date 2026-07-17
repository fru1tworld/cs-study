# 메모리 배리어는 소스 컨트롤 작업과 같다

> 원문: [Memory Barriers Are Like Source Control Operations](https://preshing.com/20120710/memory-barriers-are-like-source-control-operations/)
> 저자: Jeff Preshing | 번역일: 2026-07-10

소스 컨트롤을 쓰고 있다면, 당신은 이미 메모리 순서(memory ordering)를 이해하는 길에 들어선 셈이다. 메모리 순서는 C, C++ 및 다른 언어로 락프리 코드를 작성할 때 중요하게 고려해야 할 사항이다.

지난 글에서는 메모리 순서 퍼즐의 절반에 해당하는 [컴파일 시점의 메모리 순서](http://preshing.com/20120625/memory-ordering-at-compile-time)를 다뤘다. 이번 글은 나머지 절반, 즉 런타임에 프로세서 자체에서 일어나는 메모리 순서에 관한 것이다. 컴파일러 재배열과 마찬가지로, 프로세서 재배열은 단일 스레드 프로그램에서는 보이지 않는다. [락프리 기법](http://preshing.com/20120612/an-introduction-to-lock-free-programming)을 쓸 때 — 즉 스레드 간 상호 배제 없이 공유 메모리를 조작할 때 — 비로소 드러난다. 다만 컴파일러 재배열과 달리 프로세서 재배열의 효과는 [멀티코어와 멀티프로세서 시스템에서만 보인다](http://preshing.com/20120515/memory-reordering-caught-in-the-act).

**메모리 배리어(memory barrier)** 역할을 하는 명령어를 발행하면 프로세서에서 올바른 메모리 순서를 강제할 수 있다. 어떤 면에서는 이것이 알아야 할 유일한 기법이다. 그런 명령어를 쓰면 컴파일러 순서는 자동으로 처리되기 때문이다. 메모리 배리어 역할을 하는 명령어의 예로는 다음이 있다(이게 전부는 아니다):

- GCC의 특정 인라인 어셈블리 지시문, 예: PowerPC 전용 `asm volatile("lwsync" ::: "memory")`
- 모든 [Win32 Interlocked 연산](http://msdn.microsoft.com/en-us/library/windows/desktop/ms684122.aspx) (Xbox 360 제외)
- [C++11 원자적 타입](http://en.cppreference.com/w/cpp/atomic/atomic)의 많은 연산, 예: `load(std::memory_order_acquire)`
- POSIX 뮤텍스 연산, 예: [`pthread_mutex_lock`](http://linux.die.net/man/3/pthread_mutex_lock)

메모리 배리어 역할을 하는 명령어가 많은 것처럼, 알아야 할 메모리 배리어의 종류도 많다. 실제로 위 명령어들이 전부 같은 종류의 메모리 배리어를 만드는 것은 아니다 — 락프리 코드를 작성할 때 혼란을 일으킬 수 있는 또 하나의 지점이다. 이를 어느 정도 정리해 보고자, 가능한 메모리 배리어 유형의 대다수(전부는 아니다)를 이해하는 데 도움이 됐던 비유 하나를 제시하려 한다.

먼저 전형적인 멀티코어 시스템의 아키텍처를 생각해 보자. 코어가 두 개인 장치가 있고, 각 코어는 32 KiB의 전용 L1 데이터 캐시를 갖는다. 두 코어가 공유하는 1 MiB의 L2 캐시가 있고, 512 MiB의 메인 메모리가 있다.

![](https://preshing.com/images/cpu-diagram.png)

멀티코어 시스템은 기묘한 소스 컨트롤 전략을 쓰며 한 프로젝트에서 협업하는 프로그래머 집단과 약간 비슷하다. 예를 들어 위의 듀얼코어 시스템은 프로그래머가 딱 두 명인 시나리오에 대응한다. 이름을 Larry와 Sergey라고 하자.

![](https://preshing.com/images/source-control-analogy.png)

오른쪽에는 공유되는 중앙 저장소가 있다. 이것은 메인 메모리와 공유 L2 캐시를 합친 것을 나타낸다. Larry는 자기 로컬 머신에 저장소의 완전한 작업 사본(working copy)을 갖고 있고, Sergey도 마찬가지다. 이것들은 (사실상) 각 CPU 코어에 붙어 있는 L1 캐시를 나타낸다. 각 머신에는 레지스터나 지역 변수를 개인적으로 관리하는 스크래치 영역도 있다. 두 프로그래머는 거기 앉아 작업 사본과 스크래치 영역을 정신없이 편집하면서, 눈에 보이는 데이터를 근거로 다음에 뭘 할지 결정한다 — 그 코어에서 돌아가는 실행 스레드와 똑 닮았다.

이제 소스 컨트롤 전략 차례다. 이 비유에서 소스 컨트롤 전략은 정말이지 아주 이상하다. Larry와 Sergey가 저장소의 작업 사본을 수정하면, 그 수정 사항이 백그라운드에서 완전히 무작위한 시점에 중앙 저장소로, 그리고 중앙 저장소로부터 끊임없이 **새어 나간다(leaking)**. Larry가 파일 X를 편집하면 그 변경은 중앙 저장소로 새어 나가겠지만, 언제 그럴지는 아무 보장이 없다. 즉시일 수도 있고, 한참, 아주 한참 뒤일 수도 있다. 그가 이어서 다른 파일 Y와 Z를 편집하면, 그 수정들이 X보다 *먼저* 저장소로 새어 들어갈 수도 있다. 이런 식으로 store는 저장소로 가는 길에 사실상 재배열된다.

마찬가지로 Sergey의 머신에서도, 그 변경들이 저장소에서 *그의* 작업 사본으로 새어 *돌아오는* 시점이나 순서에 아무 보장이 없다. 이런 식으로 load는 저장소에서 나오는 길에 사실상 재배열된다.

자, 두 프로그래머가 저장소의 완전히 다른 부분에서 작업한다면, 어느 쪽도 이런 백그라운드 누출이 일어나고 있다는 것도, 심지어 상대 프로그래머의 존재조차도 알아차리지 못할 것이다. 이는 독립된 단일 스레드 프로세스 두 개를 실행하는 것에 해당한다. 이 경우 [메모리 순서의 제1 규칙](http://preshing.com/20120625/memory-ordering-at-compile-time)은 지켜진다.

이 비유는 두 프로그래머가 저장소의 같은 부분에서 작업하기 시작할 때 더 쓸모 있어진다. [이전 글](http://preshing.com/20120515/memory-reordering-caught-in-the-act)에서 들었던 예를 다시 보자. X와 Y는 전역 변수이고 둘 다 처음에는 0이다:

![](https://preshing.com/images/marked-example2-2.png)

X와 Y를 Larry의 작업 사본, Sergey의 작업 사본, 그리고 중앙 저장소 자체에 존재하는 파일이라고 생각하라. Larry가 자기 작업 사본의 X에 1을 쓰고, 거의 같은 시각에 Sergey가 자기 작업 사본의 Y에 1을 쓴다. 각 프로그래머가 자기 작업 사본에서 *상대편* 파일을 확인하기 전에 어느 수정도 저장소로 새어 나갔다가 돌아올 시간이 없었다면, 결과는 r1 = 0이면서 r2 = 0이 된다. 처음에는 직관에 어긋나 보였을 이 결과가, 소스 컨트롤 비유에서는 오히려 꽤 당연해진다.

![](https://preshing.com/images/iriw-state.png)

## 메모리 배리어의 종류

다행히 Larry와 Sergey가 백그라운드에서 일어나는 이 무작위하고 예측 불가능한 누출에 전적으로 휘둘리기만 하는 것은 아니다. 그들에게는 펜스(fence) 명령어라 불리는 특수 명령어를 발행할 능력이 있고, 이 명령어들이 메모리 배리어 역할을 한다. 이 비유에서는 네 가지 유형의 메모리 배리어, 즉 네 가지 펜스 명령어를 정의하면 충분하다. 각 메모리 배리어 유형의 이름은 그것이 막도록 설계된 메모리 재배열의 유형에서 따왔다. 예를 들어 `#StoreLoad`는 store 뒤에 오는 load의 재배열을 막도록 설계되었다.

![](https://preshing.com/images/barrier-types.png)

[Doug Lea가 지적하듯](http://g.oswego.edu/dl/jmm/cookbook.html), 이 네 범주는 실제 CPU의 특정 명령어들과 꽤 잘 대응한다 — 정확히 일치하지는 않지만. 대부분의 경우 실제 CPU 명령어는 위 배리어 유형들의 어떤 조합으로 동작하며, 다른 효과가 더해지기도 한다. 어쨌든 소스 컨트롤 비유 안에서 이 네 가지 메모리 배리어를 이해하고 나면, 실제 CPU의 수많은 명령어뿐 아니라 여러 고수준 프로그래밍 언어 구성 요소도 이해할 좋은 위치에 서게 된다.

### #LoadLoad

LoadLoad 배리어는 배리어 앞에서 수행된 load와 배리어 뒤에서 수행된 load의 재배열을 사실상 막는다.

우리 비유에서 `#LoadLoad` 펜스 명령어는 기본적으로 중앙 저장소로부터의 **pull**에 해당한다. `git pull`, `hg pull`, `p4 sync`, `svn update`, `cvs update`를 떠올리되, 전부 저장소 전체에 작용한다고 생각하라. 로컬 변경과 병합 충돌이 나면, 뭐, 무작위로 해결된다고 해두자.

![](https://preshing.com/images/loadload.png)

한 가지 유의할 점: `#LoadLoad`가 저장소 전체의 최신(head) 리비전을 pull한다는 보장은 없다! head보다 오래된 리비전을 pull할 수도 있다. 단, 그 리비전이 *중앙 저장소에서 자신의 로컬 머신으로 새어 들어온 가장 새로운 값 이상으로 새것*이기만 하면 된다.

약한 보장처럼 들릴 수 있지만, 오래된(stale) 데이터를 보는 것을 막는 데는 여전히 더할 나위 없이 좋은 방법이다. 고전적인 예를 보자. Sergey가 Larry가 어떤 데이터를 게시했는지 공유 플래그로 확인한다. 플래그가 참이면, 게시된 값을 읽기 전에 `#LoadLoad` 배리어를 발행한다:

```cpp
if (IsPublished)                   // 공유 플래그를 load하여 확인
{
    LOADLOAD_FENCE();              // load의 재배열을 방지
    return Value;                  // 게시된 값을 load
}
```

물론 이 예는 `IsPublished` 플래그가 저절로 Sergey의 작업 사본으로 새어 들어오는 것에 의존한다. 그게 정확히 언제 일어나는지는 중요하지 않다. 새어 들어온 플래그가 관찰되고 나면, 그는 `#LoadLoad` 펜스를 발행해 플래그 자체보다 오래된 `Value`의 값을 읽는 것을 막는다.

### #StoreStore

StoreStore 배리어는 배리어 앞에서 수행된 store와 배리어 뒤에서 수행된 store의 재배열을 사실상 막는다.

우리 비유에서 `#StoreStore` 펜스 명령어는 중앙 저장소로의 **push**에 해당한다. `git push`, `hg push`, `p4 submit`, `svn commit`, `cvs commit`을 떠올리되, 전부 저장소 전체에 작용한다고 생각하라.

![](https://preshing.com/images/storestore.png)

한 가지 반전을 더하자면, `#StoreStore` 명령어는 **즉각적이지 않다**고 가정하자. 지연된, 비동기적인 방식으로 수행된다. 그래서 Larry가 `#StoreStore`를 실행하더라도, 그의 이전 store들이 마침내 중앙 저장소에서 보이게 되는 시점에 대해서는 아무것도 가정할 수 없다.

이 역시 약한 보장처럼 들릴 수 있지만, 다시 말하건대 Sergey가 Larry가 게시한 오래된 데이터를 보는 것을 막는 데는 완벽히 충분하다. 위와 같은 예로 돌아가면, Larry는 공유 메모리에 데이터를 게시하고, `#StoreStore` 배리어를 발행한 뒤, 공유 플래그를 참으로 설정하기만 하면 된다:

```cpp
Value = x;                         // 데이터를 게시
STORESTORE_FENCE();
IsPublished = 1;                   // 데이터가 준비됐음을 알리는 공유 플래그 설정
```

여기서도 우리는 `IsPublished`의 값이 저절로 Larry의 작업 사본에서 Sergey의 작업 사본으로 새어 가기를 기대하고 있다. Sergey가 그것을 감지하면, 그는 올바른 `Value` 값을 보게 되리라 확신할 수 있다. 흥미로운 점은, 이 패턴이 동작하기 위해 `Value`가 원자적 타입일 필요조차 없다는 것이다. 원소가 잔뜩 든 거대한 구조체여도 상관없다.

### #LoadStore

![](https://preshing.com/images/get-back-to-later.png)`#LoadLoad`나 `#StoreStore`와 달리, `#LoadStore`에는 소스 컨트롤 작업으로 표현할 기발한 은유가 없다. `#LoadStore` 배리어를 이해하는 가장 좋은 방법은, 아주 단순하게, 명령어 재배열의 관점에서 보는 것이다.

Larry에게 따라야 할 명령어 목록이 있다고 상상하자. 어떤 명령어는 그의 개인 작업 사본에서 레지스터로 데이터를 load하게 하고, 어떤 명령어는 레지스터의 데이터를 작업 사본으로 store하게 한다. Larry는 명령어를 뒤섞을 수 있는데, 특정한 경우에만 가능하다. load를 만나면 그 뒤에 오는 store들을 미리 살펴본다. 그 store들이 현재 load와 *완전히 무관*하다면, 먼저 건너뛰어 store들을 처리하고 나서 돌아와 load를 마무리하는 것이 허용된다. 이런 경우에도 메모리 순서의 제1 규칙 — 단일 스레드 프로그램의 동작을 절대 바꾸지 않는다 — 은 여전히 지켜진다.

실제 CPU에서는, 이를테면 load에서 캐시 미스가 나고 뒤이은 store에서 캐시 히트가 나는 경우 일부 프로세서에서 이런 명령어 재배열이 일어날 수 있다. 하지만 비유를 이해하는 데 그런 하드웨어 세부 사항은 별로 중요하지 않다. 그냥 Larry의 일이 지루한데, 이때가 그에게 창의력 발휘가 허락되는 몇 안 되는 순간이라고 해두자. 그가 그렇게 할지 말지는 전혀 예측할 수 없다. 다행히 이것은 막는 비용이 비교적 저렴한 유형의 재배열이다. Larry는 `#LoadStore` 배리어를 만나면 그 배리어 주위에서 이런 재배열을 삼가기만 하면 된다.

우리 비유에서는 load와 store 사이에 `#LoadLoad`나 `#StoreStore` 배리어가 있어도 Larry가 이런 LoadStore 재배열을 수행하는 것이 유효하다. 하지만 실제 CPU에서 `#LoadStore` 배리어 역할을 하는 명령어는 보통 그 두 배리어 유형 중 적어도 하나의 역할도 겸한다.

### #StoreLoad

StoreLoad 배리어는 배리어 앞에서 수행된 모든 store가 다른 프로세서들에게 보이게 하고, 배리어 뒤에서 수행된 모든 load가 배리어 시점에 보이는 최신 값을 받도록 보장한다. 다시 말해, [순차 일관성](http://preshing.com/20120612/an-introduction-to-lock-free-programming#sequential-consistency)을 갖춘 멀티프로세서가 그 연산들을 수행하는 방식을 존중하면서, 배리어 앞의 모든 store와 배리어 뒤의 모든 load 사이의 재배열을 사실상 막는다.

`#StoreLoad`는 유일무이하다. [Memory Reordering Caught in the Act](http://preshing.com/20120515/memory-reordering-caught-in-the-act)의 예제 — 이 글 앞부분에서 반복한 바로 그 예제 — 에서 r1 = r2 = 0이라는 결과를 막을 수 있는 유일한 유형의 메모리 배리어다.

여기까지 잘 따라왔다면 궁금할 것이다. `#StoreLoad`는 `#StoreStore` 뒤에 `#LoadLoad`를 붙인 것과 뭐가 다른가? 어쨌든 `#StoreStore`는 변경을 중앙 저장소로 push하고 `#LoadLoad`는 원격 변경을 pull해 오지 않는가. 하지만 그 두 배리어 유형만으로는 불충분하다. 기억하라, push 연산은 임의 개수의 명령어만큼 지연될 수 있고, pull 연산은 head 리비전을 pull하지 않을 수 있다. 이것이 PowerPC의 `lwsync` 명령어 — `#LoadLoad`, `#LoadStore`, `#StoreStore` 세 가지 배리어 역할은 전부 하지만 `#StoreLoad`는 아닌 — 가 그 예제에서 r1 = r2 = 0을 막기에 불충분한 이유를 시사한다.

비유의 관점에서 `#StoreLoad` 배리어는, 모든 로컬 변경을 중앙 저장소에 push하고, 그 작업이 완료되기를 기다린 다음, 저장소의 절대적으로 최신인 head 리비전을 pull하는 것으로 달성할 수 있다. 대부분의 프로세서에서 `#StoreLoad` 배리어 역할을 하는 명령어는 다른 배리어 유형 역할을 하는 명령어보다 비싼 경향이 있다.

![](https://preshing.com/images/storeload.png)

여기에 `#LoadStore` 배리어를 하나 더 얹으면 — 별로 대단한 일도 아니다 — 우리가 얻는 것은 전체 메모리 펜스(full memory fence), 즉 네 가지 배리어 유형 모두를 한꺼번에 수행하는 것이다. [역시 Doug Lea가 지적하듯](http://g.oswego.edu/dl/jmm/cookbook.html), 공교롭게도 현존하는 모든 프로세서에서 `#StoreLoad` 배리어 역할을 하는 명령어는 전부 전체 메모리 펜스 역할도 한다.

## 이 비유는 어디까지 통할까?

앞서 언급했듯, 메모리 순서에 관해서는 [프로세서마다 습성이 다르다](http://preshing.com/20120612/an-introduction-to-lock-free-programming#different-processors-have). 특히 x86/64 계열은 강한 메모리 모델을 갖는다. 메모리 재배열을 최소한으로 유지하는 것으로 알려져 있다. PowerPC와 ARM은 더 약한 메모리 모델을 갖고, Alpha는 독보적인 존재로 유명하다. 다행히 이 글에서 제시한 비유는 [약한 메모리 모델](http://preshing.com/20120930/weak-vs-strong-memory-models)에 대응한다. 이 비유를 머릿속에 담아두고 여기서 소개한 펜스 명령어로 올바른 메모리 순서를 강제할 수 있다면, 대부분의 CPU를 다룰 수 있을 것이다.

이 비유는 C++11(예전 이름 C++0x)과 C11이 대상으로 삼는 추상 기계와도 꽤 잘 대응한다. 따라서 이 비유를 염두에 두고 그 언어들의 표준 라이브러리로 락프리 코드를 작성하면, 어떤 플랫폼에서든 올바르게 동작할 가능성이 더 높다.

이 비유에서 나는 각 프로그래머가 별도의 코어에서 실행되는 단일 실행 스레드를 나타낸다고 말했다. 실제 운영체제에서 스레드는 살아 있는 동안 여러 코어 사이를 옮겨 다니는 경향이 있지만, 비유는 여전히 유효하다. 또 기계어 예제와 C/C++ 예제를 번갈아 사용했다. 당연히 우리는 C/C++이나 다른 고수준 언어를 쓰는 편을 선호할 것이다. 이것이 가능한 이유는, 다시 말하지만, 메모리 배리어 역할을 하는 모든 연산은 [컴파일러 재배열](http://preshing.com/20120625/memory-ordering-at-compile-time)도 막기 때문이다.

모든 종류의 메모리 배리어를 다 다룬 것은 아니다. 예를 들어 [데이터 의존성 배리어](http://www.mjmwired.net/kernel/Documentation/memory-barriers.txt#305)도 있다. 이후 글에서 더 설명하겠다. 그래도 여기서 소개한 네 가지가 큰 축이다.

CPU가 내부에서 어떻게 동작하는지 — 스토어 버퍼, 캐시 일관성 프로토콜 같은 하드웨어 구현 세부 사항 — 그리고 애초에 왜 메모리 재배열을 수행하는지 궁금하다면, Paul McKenney와 David Howells의 [훌륭한](http://www.rdrop.com/users/paulmck/scalability/paper/whymb.2010.07.23a.pdf) [작업물](http://www.kernel.org/doc/Documentation/memory-barriers.txt)을 추천한다. 실제로 락프리 코드를 성공적으로 작성해 본 프로그래머라면 대부분 그런 하드웨어 세부 사항에 최소한 스치듯이라도 익숙하리라 짐작한다.
