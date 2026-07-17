# 구조적 동시성에 관한 노트, 혹은: go 문의 해로움

> 원문: [Notes on structured concurrency, or: Go statement considered harmful](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/)
> 저자: Nathaniel J. Smith | 번역일: 2026-07-10

모든 동시성 API에는 코드를 동시에 실행하는 방법이 필요하다. 서로 다른 API에서 그것이 어떤 모습인지 몇 가지 예를 보자.

```
go myfunc();                                // Golang

pthread_create(&thread_id, NULL, &myfunc);  /* C + POSIX 스레드 */

spawn(modulename, myfuncname, [])           % Erlang

threading.Thread(target=myfunc).start()     # Python + 스레드

asyncio.create_task(myfunc())               # Python + asyncio
```

표기법과 용어는 제각각이지만 의미론은 같다. 전부 `myfunc`가 프로그램의 나머지 부분과 동시에 실행되도록 준비해 놓고 즉시 반환해서, 부모가 다른 일을 할 수 있게 한다.

또 다른 선택지는 콜백이다.

```
QObject::connect(&emitter, SIGNAL(event()),        // C++ + Qt
                 &receiver, SLOT(myfunc()))

g_signal_connect(emitter, "event", myfunc, NULL)   /* C + GObject */

document.getElementById("myid").onclick = myfunc;  // Javascript

promise.then(myfunc, errorhandler)                 // Javascript + Promise

deferred.addCallback(myfunc)                       # Python + Twisted

future.add_done_callback(myfunc)                   # Python + asyncio
```

역시 표기법은 다르지만 하는 일은 모두 같다. 지금부터 특정 이벤트가 발생하면 그때 `myfunc`가 실행되도록 준비해 두고, 그 설정을 마치면 즉시 반환해서 호출자가 다른 일을 할 수 있게 한다. (콜백은 [promise](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all) [컴비네이터](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/race)나 [Twisted 스타일 프로토콜/트랜스포트](https://twistedmatrix.com/documents/current/core/howto/servers.html) 같은 화려한 헬퍼로 치장되기도 하지만, 핵심 아이디어는 같다.)

그리고... 그게 전부다. 실제로 쓰이는 범용 동시성 API를 아무거나 골라 보면, 아마 이 두 부류 중 하나에 속할 것이다(asyncio처럼 둘 다인 경우도 있다).

그런데 내가 새로 만든 라이브러리 [Trio](https://trio.readthedocs.io)는 특이하다. 어느 쪽 접근도 쓰지 않는다. 대신 `myfunc`와 `anotherfunc`를 동시에 실행하고 싶으면 이렇게 쓴다.

```
async with trio.open_nursery() as nursery:
    nursery.start_soon(myfunc)
    nursery.start_soon(anotherfunc)
```

이 "nursery" 구문을 처음 접한 사람들은 대개 혼란스러워한다. 왜 들여쓰기 블록이 있지? 이 `nursery` 객체는 뭐고, 왜 태스크를 하나 띄우기 전에 이걸 먼저 만들어야 하지? 그러다 다른 프레임워크에서 익숙해진 패턴을 못 쓰게 막는다는 걸 깨닫고는 정말로 짜증을 낸다. 기본 프리미티브라기엔 별나고 독특하고 너무 고수준으로 느껴진다. 충분히 이해할 만한 반응이다! 하지만 조금만 참고 들어 보시라.

**이 글에서 나는 nursery가 별나거나 독특한 것이 전혀 아니며, for 루프나 함수 호출만큼이나 근본적인 새로운 제어 흐름 프리미티브라는 것을 설득하려 한다. 나아가 위에서 본 다른 접근들 – 스레드 생성과 콜백 등록 – 은 완전히 제거하고 nursery로 대체해야 한다고 주장한다.**

그럴 리가 없다고? 사실 비슷한 일이 전에도 있었다. `goto` 문은 한때 제어 흐름의 왕이었다. 지금은 [웃음거리](https://xkcd.com/292/)다. 몇몇 언어에는 아직 `goto`라 부르는 무언가가 남아 있지만, 원래의 `goto`와는 다르고 훨씬 약하다. 그리고 대부분의 언어에는 그것조차 없다. 무슨 일이 있었던 걸까? 너무 오래전 일이라 이제는 그 이야기를 아는 사람이 드물지만, 알고 보면 놀라울 정도로 지금과 맞닿아 있다. 그러니 먼저 `goto`가 정확히 무엇이었는지 되짚어 보고, 그것이 동시성 API에 관해 무엇을 가르쳐 줄 수 있는지 살펴보자.

**목차:**

  * 대체 `goto` 문이란 무엇인가?
  * 대체 `go` 문이란 무엇인가?
  * `goto`에는 무슨 일이 일어났나?
    * `goto`: 추상화의 파괴자
    * 뜻밖의 이득: `goto` 문을 없애자 새로운 기능이 가능해졌다
    * `goto` 문: 단 한 번도 안 된다
  * `go` 문의 해로움
    * `go` 문: 단 한 번도 안 된다
  * Nursery: `go` 문의 구조적 대체물
    * nursery는 함수 추상화를 보존한다.
    * nursery는 동적 태스크 생성을 지원한다.
    * 탈출구는 있다.
    * nursery처럼 동작하는 새 타입을 정의할 수 있다.
    * 아니, 정말로, nursery는 내부 태스크가 종료될 때까지 _항상_ 기다린다.
    * 자동 리소스 정리가 동작한다.
    * 자동 오류 전파가 동작한다.
    * 뜻밖의 이득: `go` 문을 없애자 새로운 기능이 가능해졌다
  * 실전에서의 nursery
  * 결론
  * 댓글
  * 감사의 말
  * 각주

## 대체 `goto` 문이란 무엇인가?

역사를 좀 복습해 보자. 초기 컴퓨터는 [어셈블리 언어](https://en.wikipedia.org/wiki/Assembly_language)나 그보다 더 원시적인 방식으로 프로그래밍했다. 이건 꽤나 고역이었다. 그래서 1950년대에 IBM의 [존 배커스](https://en.wikipedia.org/wiki/John_Backus)나 Remington Rand의 [그레이스 호퍼](https://en.wikipedia.org/wiki/Grace_Hopper) 같은 사람들이 [FORTRAN](https://en.wikipedia.org/wiki/Fortran)이나 [FLOW-MATIC](https://en.wikipedia.org/wiki/FLOW-MATIC)(직계 후손인 [COBOL](https://en.wikipedia.org/wiki/COBOL)로 더 잘 알려져 있다) 같은 언어를 개발하기 시작했다.

FLOW-MATIC은 당대 기준으로 매우 야심 찬 언어였다. Python의 고조할머니의 고조할머니쯤 되는 존재라고 생각하면 된다. 컴퓨터보다 인간을 먼저 생각하고 설계된 최초의 언어였다. 어떤 모습이었는지 맛보기로 FLOW-MATIC 코드를 조금 보자.

![FLOW-MATIC 코드 샘플](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/flow-matic-1.svg)

현대 언어와 달리 `if` 블록도, 루프 블록도, 함수 호출도 없다는 게 눈에 띌 것이다. 사실 블록 구분자나 들여쓰기 자체가 아예 없다. 그저 문장들이 평평하게 나열되어 있을 뿐이다. 이 프로그램이 우연히 너무 짧아서 화려한 제어 문법을 안 쓴 게 아니다. 블록 문법이 아직 발명되지 않았기 때문이다!

![순차 흐름은 아래를 향하는 세로 화살표로, goto 흐름은 아래로 향하다가 옆으로 튀어나가는 화살표로 표현한 그림](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/sequential-and-go-to-schematic.svg)

대신 FLOW-MATIC에는 흐름 제어 방법이 두 가지 있었다. 평소에는 예상대로 순차적이다. 맨 위에서 시작해 한 문장씩 아래로 내려간다. 하지만 `JUMP TO` 같은 특수한 문장을 실행하면 제어를 다른 곳으로 곧장 옮길 수 있었다. 예를 들어 문장 (13)은 문장 (2)로 되돌아간다.

![문장 13에서 문장 2로 되돌아가는 점프를 표시한 FLOW-MATIC 코드](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/flow-matic-2.svg)

글 첫머리의 동시성 프리미티브들처럼, 이 "일방향 점프" 연산을 뭐라고 부를지에 대해서도 의견이 갈렸다. 여기서는 `JUMP TO`지만 결국 살아남은 이름은 `goto`("go to", 감이 오시는지?)였으니, 이 글에서도 그렇게 부르겠다.

이 작은 프로그램에 들어 있는 `goto` 점프 전체는 다음과 같다.

![프로그램의 모든 goto 점프를 화살표로 표시한 FLOW-MATIC 코드](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/flow-matic-4.svg)

이게 혼란스러워 보인다면, 당신만 그런 게 아니다! 이런 점프 기반 프로그래밍 스타일은 FLOW-MATIC이 어셈블리 언어에서 거의 그대로 물려받은 것이다. 강력하고 컴퓨터 하드웨어가 실제로 동작하는 방식에 잘 들어맞지만, 직접 다루기에는 지독하게 혼란스럽다. 저 화살표 뭉치야말로 "스파게티 코드"라는 말이 생겨난 이유다. 분명히 더 나은 무언가가 필요했다.

그런데... `goto`의 어떤 점이 이 모든 문제를 일으키는 걸까? 왜 어떤 제어 구조는 괜찮고 어떤 것은 안 되는 걸까? 좋은 것은 어떻게 골라내야 할까? 당시에는 이게 정말 불분명했고, 문제를 이해하지 못하면 고치기도 어려운 법이다.

## 대체 `go` 문이란 무엇인가?

역사 이야기는 잠시 멈추자. `goto`가 나빴다는 건 누구나 안다. 이게 동시성과 무슨 상관일까? 자, Golang의 그 유명한 `go` 문을 보자. 새 "고루틴"(경량 스레드)을 만들 때 쓴다.

```
// Golang
go myfunc();
```

이것의 제어 흐름 다이어그램을 그릴 수 있을까? 위에서 본 두 가지와는 조금 다르다. 제어가 실제로 갈라지기 때문이다. 이렇게 그릴 수 있겠다.

![go 흐름을 두 개의 화살표로 표현한 그림: 아래로 향하는 초록 화살표와, 아래로 향하다가 옆으로 튀어나가는 연보라 화살표](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/go-schematic-unlabeled.svg)

여기서 색은 _두_ 경로가 모두 실행된다는 뜻이다. 부모 고루틴(초록 선)의 관점에서 제어는 순차적으로 흐른다. 위로 들어와서 곧바로 아래로 나간다. 한편 자식(연보라 선)의 관점에서 제어는 위로 들어와서 `myfunc`의 본문으로 점프한다. 일반 함수 호출과 달리 이 점프는 일방향이다. `myfunc`를 실행할 때 완전히 새로운 스택으로 전환하며, 런타임은 어디서 왔는지 즉시 잊어버린다.

이건 Golang에만 해당하는 얘기가 아니다. 이 글 첫머리에 나열한 프리미티브 _전부_ 의 제어 흐름 다이어그램이 이것이다.

  * 스레딩 라이브러리는 보통 나중에 스레드를 `join`할 수 있는 핸들 객체 같은 것을 제공한다. 하지만 그건 언어가 전혀 알지 못하는 독립적인 연산일 뿐이고, 실제 스레드 생성 프리미티브의 제어 흐름은 위 그림 그대로다.
  * 콜백 등록은 의미론적으로 (a) 어떤 이벤트가 발생할 때까지 블록했다가 (b) 콜백을 실행하는 백그라운드 스레드를 시작하는 것과 동등하다. (물론 구현은 다르다.) 그러니 고수준 제어 흐름 관점에서 콜백 등록은 본질적으로 `go` 문이다.
  * Future와 promise도 마찬가지다. 함수를 호출했더니 promise를 반환한다면, 작업을 백그라운드에서 실행되도록 예약해 놓고 (원한다면) 나중에 그 작업에 join할 수 있는 핸들 객체를 건네줬다는 뜻이다. 제어 흐름 의미론으로 보면 스레드를 띄우는 것과 똑같다. 거기다 promise에는 콜백을 등록하니, 앞 항목도 참고하시라.

똑같은 패턴이 정말 다양한 형태로 나타난다. 핵심 공통점은, 이 모든 경우에 제어 흐름이 갈라지면서 한쪽은 일방향 점프를 하고 다른 쪽은 호출자에게 돌아간다는 것이다. 뭘 찾아야 하는지 알고 나면 여기저기서 보이기 시작한다. 꽤 재미있는 놀이다! [1]

그런데 성가시게도 이 부류의 제어 흐름 구조를 가리키는 표준 명칭이 없다. 그래서 "`goto` 문"이 온갖 `goto`류 구조의 총칭이 된 것처럼, 나는 이것들의 총칭으로 "`go` 문"을 쓰겠다. 왜 `go`냐고? 하나는 Golang이 이 형태의 특히 순수한 예를 보여 주기 때문이다. 그리고 다른 하나는... 뭐, 내가 어디로 가려는지 이미 짐작했을 것이다. 이 두 다이어그램을 보라. 비슷한 점이 보이지 않는가?

![앞서 본 다이어그램의 반복: 아래로 향하다 옆으로 튀어나가는 화살표로 표현한 goto 흐름과, 초록 화살표 하나에 옆으로 튀어나가는 연보라 화살표가 더해진 go 흐름](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/go-schematic-and-go-to-schematic.svg)

그렇다. **go 문은 goto 문의 한 형태다.**

동시성 프로그램은 작성하기도 추론하기도 어렵기로 악명 높다. `goto` 기반 프로그램도 그렇다. 혹시 그 이유 중 일부가 같은 건 아닐까? 현대 언어에서 `goto`가 일으키던 문제는 대부분 해결됐다. 그들이 `goto`를 어떻게 고쳤는지 연구하면, 더 쓰기 좋은 동시성 API를 만드는 법도 배울 수 있지 않을까? 알아보자.

## `goto`에는 무슨 일이 일어났나?

그래서 `goto`의 어떤 점이 그토록 많은 문제를 일으키는 걸까? 1960년대 후반, [에츠허르 W. 데이크스트라](https://en.wikipedia.org/wiki/Edsger_W._Dijkstra)가 이제는 유명해진 두 편의 논문을 써서 이를 훨씬 명확하게 밝혔다. [Go to statement considered harmful](https://scholar.google.com/scholar?cluster=15335993203437612903&hl=en&as_sdt=0,5)과 [Notes on structured programming](https://www.cs.utexas.edu/~EWD/ewd02xx/EWD249.PDF)(PDF)이다.

### `goto`: 추상화의 파괴자

이 논문들에서 데이크스트라가 고민한 문제는 사소하지 않은 소프트웨어를 어떻게 작성하고 올바르게 만들 것인가였다. 여기서 그 진가를 다 전할 수는 없다. 온갖 매혹적인 통찰이 들어 있다. 예를 들어 이 인용문을 들어 봤을지도 모르겠다.

![테스트는 버그의 존재를 보여 줄 수는 있어도, 버그가 없음을 보여 줄 수는 결코 없다!](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/testing.png)

그렇다, _Notes on structured programming_ 에 나오는 말이다. 하지만 그의 가장 큰 관심사는 _추상화_ 였다. 그는 머릿속에 한꺼번에 담기에는 너무 큰 프로그램을 작성하고 싶어 했다. 그러려면 프로그램의 일부를 블랙박스처럼 다룰 수 있어야 한다. Python 프로그램이 이렇게 하는 걸 볼 때처럼 말이다.

```
print("Hello world!")
```

`print`가 어떻게 구현되어 있는지(문자열 포매팅, 버퍼링, 플랫폼별 차이, ...) 시시콜콜 알 필요가 없다. 어떻게든 건네준 텍스트를 출력해 준다는 것만 알면 되고, 그러면 코드의 이 지점에서 그게 정말 원하는 동작인지 생각하는 데 에너지를 쓸 수 있다. 데이크스트라는 언어가 이런 종류의 추상화를 지원하기를 원했다.

이 무렵에는 블록 문법이 발명되어 있었고, ALGOL 같은 언어에는 5가지 정도의 서로 다른 제어 구조가 쌓여 있었다. 여전히 순차 흐름과 `goto`가 있었고,

![앞서 본 것과 같은 순차 흐름과 goto 흐름 그림](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/sequential-and-go-to-schematic.svg)

if/else, 루프, 함수 호출의 변형들도 갖추게 되었다.

![if 문, 루프, 함수 호출의 제어 흐름을 화살표로 보여 주는 다이어그램들](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/control-schematics.svg)

이 고수준 구조들은 `goto`로 구현할 수 있고, 초기에는 사람들도 그렇게 생각했다. 편리한 축약 표기라고 말이다. 하지만 데이크스트라가 지적한 것은, 이 다이어그램들을 보면 `goto`와 나머지 사이에 큰 차이가 있다는 점이다. `goto`를 제외한 모든 것에서 제어 흐름은 위로 들어오고 → [무언가가 일어나고] → 아래로 나간다. 이걸 "블랙박스 규칙"이라 부를 수 있겠다. 제어 구조가 이런 모양이라면, 내부에서 무슨 일이 일어나는지 신경 쓸 필요가 없는 맥락에서는 [무언가가 일어난다] 부분을 무시하고 전체를 평범한 순차 흐름으로 취급할 수 있다. 더 좋은 것은, 이 조각들로 _조합된_ 코드에 대해서도 똑같이 성립한다는 점이다. 이 코드를 볼 때

```
print("Hello world!")
```

제어 흐름이 어떻게 되는지 알아내려고 `print`의 정의와 그 모든 전이적 의존성까지 읽으러 갈 필요가 없다. `print` 안에 루프가 있을 수도 있고, 루프 안에 if/else가 있고, 그 안에 또 다른 함수 호출이 있을 수도 있다... 아니면 전혀 다른 무언가일 수도 있다. 그건 별로 중요하지 않다. 제어가 `print`로 흘러 들어가고, 함수가 제 할 일을 하고, 결국 제어가 내가 읽고 있는 코드로 돌아온다는 것만 알면 된다.

당연한 얘기처럼 들릴지 모른다. 하지만 언어에 `goto`가 있다면 – 함수를 비롯한 모든 것이 `goto` 위에 세워져 있고, `goto`가 언제든 어디로든 점프할 수 있는 언어라면 – 이 제어 구조들은 전혀 블랙박스가 아니다! 함수가 있고, 함수 안에 루프가 있고, 루프 안에 if/else가 있고, if/else 안에 `goto`가 있다면... 그 `goto`는 제어를 어디로든 보내 버릴 수 있다. 어쩌면 제어가 아직 호출하지도 않은 전혀 다른 함수에서 갑자기 반환되어 나올지도 모른다. 알 수가 없다!

그리고 이것이 추상화를 깨뜨린다. _모든 함수 호출이 잠재적으로 변장한 `goto` 문이며, 그걸 알아내는 유일한 방법은 시스템의 전체 소스 코드를 한꺼번에 머릿속에 담아 두는 것뿐_ 이라는 뜻이기 때문이다. `goto`가 언어에 들어오는 순간, 제어 흐름에 대한 지역적 추론이 불가능해진다. 그것이 `goto`가 스파게티 코드로 이어지는 _이유_ 다.

문제를 이해하고 나자 데이크스트라는 그것을 해결할 수 있었다. 그의 혁명적인 제안은 이렇다. if/루프/함수 호출을 `goto`의 축약 표기로 생각하기를 멈추고, 그 자체로 근본적인 프리미티브로 여기자. 그리고 `goto`는 언어에서 완전히 제거하자.

2018년의 시점에서 보면 이건 충분히 자명해 보인다. 하지만 안전하게 쓸 만큼 똑똑하지 못하다는 이유로 장난감을 빼앗으려 할 때 프로그래머들이 어떻게 반응하는지 본 적 있는가? 그렇다, 어떤 것들은 결코 변하지 않는다. 1969년에 이 제안은 _엄청나게 논쟁적_ 이었다. [도널드 커누스](https://en.wikipedia.org/wiki/Donald_Knuth)는 `goto`를 [옹호했다](https://scholar.google.com/scholar?cluster=17147143327681396418&hl=en&as_sdt=0,5). `goto`로 코드를 쓰는 데 전문가가 된 사람들은, 더 새롭고 더 제약이 많은 구조로 자기 생각을 표현하기 위해 프로그래밍을 사실상 처음부터 다시 배워야 한다는 데 당연히 분개했다. 그리고 물론 완전히 새로운 언어들을 만들어야 했다.

![왼쪽에는 으르렁대는 늑대 사진, 오른쪽에는 뚱한 불도그 사진](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/wolf-and-bulldog.jpg)

왼쪽: 전통적인 `goto`. 오른쪽: C, C#, Golang 등에서 볼 수 있는 길들여진 `goto`. 함수 경계를 넘지 못하니, 신발에 오줌을 쌀 수는 있어도 얼굴을 물어뜯지는 못할 것이다.

결과적으로 현대 언어들은 데이크스트라의 원래 정식화보다는 조금 덜 엄격하다. `break`, `continue`, `return` 같은 구조로 여러 겹의 중첩 구조를 한 번에 빠져나가는 것을 허용한다. 하지만 근본적으로는 모두 데이크스트라의 아이디어를 중심으로 설계되어 있다. 경계를 밀어붙이는 이런 구조들조차 엄격히 제한된 방식으로만 그렇게 한다. 특히 제어 흐름을 블랙박스 안에 감싸는 근본 도구인 함수는 불가침으로 여겨진다. 한 함수를 `break`로 빠져나가 다른 함수로 들어갈 수는 없고, `return`은 현재 함수 밖으로 데려다줄 수는 있어도 그 이상은 안 된다. 함수가 내부에서 제어 흐름으로 무슨 곡예를 부리든, 다른 함수들은 신경 쓸 필요가 없다.

이는 `goto` 자체에도 적용된다. C, C#, Golang처럼 아직 `goto`라 부르는 무언가를 가진 언어가 몇 있지만, 무거운 제약이 붙어 있다. 최소한 한 함수 본문에서 다른 함수 본문으로 점프하는 것은 허용하지 않는다. 어셈블리로 작업하는 게 아니라면 [2], 그 고전적인 무제한 `goto`는 사라졌다. 데이크스트라가 이겼다.

### 뜻밖의 이득: `goto` 문을 없애자 새로운 기능이 가능해졌다

그리고 `goto`가 사라지자 흥미로운 일이 벌어졌다. 언어 설계자들이 제어 흐름의 구조성에 의존하는 기능들을 추가할 수 있게 된 것이다.

예를 들어 Python에는 리소스 정리를 위한 근사한 문법인 `with` 문이 있다. 이렇게 쓸 수 있다.

```
# Python
with open("my-file") as file_handle:
    ...
```

그러면 `...` 코드가 실행되는 동안에는 파일이 열려 있고, 그 직후에는 닫힌다는 것이 보장된다. 대부분의 현대 언어에 이에 상응하는 것이 있다(RAII, `using`, try-with-resource, `defer`, ...). 그리고 이것들은 전부 제어가 질서 있게, 구조적으로 흐른다고 가정한다. `goto`로 `with` 블록 한가운데로 점프해 들어간다면... 대체 무슨 일이 벌어지는 걸까? 파일은 열려 있는 걸까 아닐까? 정상적으로 빠져나가는 대신 다시 점프해 나간다면? 파일은 닫힐까? 언어에 `goto`가 있으면 이 기능은 어떤 일관된 방식으로도 동작할 수 없다.

오류 처리에도 비슷한 문제가 있다. 뭔가 잘못됐을 때 코드는 무엇을 해야 할까? 흔한 답은 스택 위쪽의 호출자에게 책임을 넘기고 알아서 처리하게 하는 것이다. 현대 언어에는 이를 쉽게 해 주는 구조가 따로 있다. 예외라든가, 다른 형태의 [자동 오류 전파](https://doc.rust-lang.org/std/result/index.html#the-question-mark-operator-) 같은 것들이다. 하지만 언어가 이런 도움을 줄 수 있으려면 스택이 _있어야_ 하고, "호출자"라는 개념이 신뢰할 수 있어야 한다. 우리의 FLOW-MATIC 프로그램의 제어 흐름 스파게티를 다시 보면서, 그 한복판에서 예외를 던지려 한다고 상상해 보라. 대체 그게 어디로 가겠는가?

### `goto` 문: 단 한 번도 안 된다

그러니까 함수 경계를 무시하는 전통적인 `goto`는, 올바르게 쓰기 어려운 그저 그런 나쁜 기능이 아니다. 그 정도였다면 살아남았을지도 모른다. 나쁜 기능은 얼마든지 살아남았으니까. 하지만 `goto`는 그보다 훨씬 나쁘다.

> 직접 `goto`를 쓰지 않더라도, 언어에 선택지로 존재한다는 것만으로 _모든 것_ 이 쓰기 어려워진다. 서드파티 라이브러리를 쓰기 시작할 때마다 그것을 블랙박스로 취급할 수 없다. 어떤 함수가 평범한 함수이고 어떤 것이 변장한 독자적 제어 흐름 구조인지 알아내려면 전부 읽어 봐야 한다. 이는 지역적 추론을 가로막는 심각한 장애물이다. 게다가 신뢰할 수 있는 리소스 정리나 자동 오류 전파 같은 강력한 언어 기능도 잃는다. `goto`를 통째로 제거하고 "블랙박스" 규칙을 따르는 제어 흐름 구조를 택하는 편이 낫다.

## `go` 문의 해로움

여기까지가 `goto`의 역사다. 자, 이 중 얼마나 많은 부분이 `go` 문에 적용될까? 음... 기본적으로 전부 다! 이 유비는 충격적일 만큼 정확하게 들어맞는다.

**Go 문은 추상화를 깨뜨린다.** 언어가 `goto`를 허용하면 어떤 함수든 변장한 `goto`일 수 있다고 했던 것을 기억하는가? 대부분의 동시성 프레임워크에서 `go` 문은 정확히 같은 문제를 일으킨다. 함수를 호출할 때마다, 그 함수가 백그라운드 태스크를 띄울 수도 있고 아닐 수도 있다. 함수가 반환된 것처럼 보이는데, 백그라운드에서 아직 실행 중인 걸까? 소스 코드를 전이적으로 전부 읽어 보지 않고는 알 방법이 없다. 언제 끝날까? 말하기 어렵다. `go` 문이 있으면 함수는 더 이상 제어 흐름에 관한 블랙박스가 아니다. [동시성 API에 관한 내 첫 글](https://vorpus.org/blog/some-thoughts-on-asynchronous-api-design-in-a-post-asyncawait-world/)에서 나는 이를 "인과성 위반"이라 불렀고, asyncio와 Twisted를 쓰는 프로그램에서 흔히 벌어지는 실제 문제들 – 백프레셔 문제, 올바른 종료 문제 등 – 의 근본 원인이 이것임을 확인했다.

**Go 문은 자동 리소스 정리를 깨뜨린다.** 그 `with` 문 예제를 다시 보자.

```
# Python
with open("my-file") as file_handle:
    ...
```

앞서 우리는 `...` 코드가 실행되는 동안 파일이 열려 있고 그 후에 닫힌다는 것이 "보장"된다고 말했다. 하지만 `...` 코드가 백그라운드 태스크를 띄운다면? 보장은 사라진다. `with` 블록 _안에 있는 것처럼 보이는_ 연산들이 실제로는 `with` 블록이 끝난 _뒤에도_ 계속 실행되다가, 아직 사용 중인 파일이 닫히는 바람에 죽어 버릴 수 있다. 그리고 역시, 지역적으로 살펴봐서는 알 수 없다. 이런 일이 벌어지는지 알려면 `...` 코드 안에서 호출되는 모든 함수의 소스 코드를 읽으러 가야 한다.

이 코드가 제대로 동작하길 원한다면, 어떻게든 모든 백그라운드 태스크를 추적하고, 그것들이 끝났을 때만 파일이 닫히도록 손수 조율해야 한다. 가능은 하다. 다만 태스크가 끝났을 때 알림을 받을 방법을 전혀 제공하지 않는 라이브러리를 쓰고 있다면 얘기가 다른데, 이런 경우가 괴로울 만큼 흔하다(예를 들어 join할 수 있는 태스크 핸들을 노출하지 않아서). 하지만 최선의 경우라 해도, 비구조적 제어 흐름 때문에 언어는 우리를 도울 수 없다. 옛날 그 나쁜 시절처럼 리소스 정리를 손으로 구현하는 처지로 돌아간 것이다.

**Go 문은 오류 처리를 깨뜨린다.** 위에서 논의했듯이, 현대 언어는 오류가 감지되어 올바른 곳으로 전파되도록 예외 같은 강력한 도구를 제공한다. 하지만 이 도구들은 "현재 코드의 호출자"라는 개념이 신뢰할 수 있다는 데 의존한다. 태스크를 띄우거나 콜백을 등록하는 순간 그 개념은 깨진다. 그 결과, 내가 아는 모든 주류 동시성 프레임워크는 그냥 포기한다. 백그라운드 태스크에서 오류가 발생했는데 수동으로 처리하지 않으면, 런타임은 그냥... 바닥에 떨어뜨리고는 별로 중요한 게 아니었기를 빌며 손가락을 꼰다. 운이 좋으면 콘솔에 뭔가 찍어 줄지도 모른다. (내가 써 본 소프트웨어 중 "뭔가 출력하고 계속 진행"이 좋은 오류 처리 전략이라고 생각하는 다른 것은 낡아빠진 Fortran 라이브러리들뿐인데, 지금 우리가 그 수준에 와 있다.) 고등학교 졸업앨범에서 "스레딩 정확성에 가장 집착하는 언어"로 뽑혔을 Rust조차 이 죄에서 자유롭지 않다. 백그라운드 스레드가 패닉하면 Rust는 [오류를 버리고 잘되기를 바란다](https://doc.rust-lang.org/std/thread/).

물론 이런 시스템에서도 오류를 제대로 처리 _할 수는_ 있다. 모든 스레드를 빠짐없이 join하거나, [Twisted의 errback](https://twistedmatrix.com/documents/current/core/howto/defer.html#visual-explanation)이나 [Javascript의 Promise.catch](https://hackernoon.com/promises-and-error-handling-4a11af37cb0e)처럼 자체 오류 전파 메커니즘을 만들면 된다. 하지만 그러면 언어에 이미 있는 기능을 임시방편으로, 깨지기 쉽게 재구현하고 있는 셈이다. "트레이스백"이나 "디버거" 같은 유용한 것들도 잃는다. `Promise.catch` 호출을 딱 한 번 잊어버리는 것만으로 심각한 오류가 자기도 모르게 바닥에 버려진다. 그리고 이 모든 문제를 어떻게든 해결하더라도, 같은 일을 하는 중복 시스템 두 개를 떠안게 된다.

### `go` 문: 단 한 번도 안 된다

`goto`가 최초의 실용적인 고수준 언어들에게 자명한 프리미티브였던 것처럼, `go`는 최초의 실용적인 동시성 프레임워크들에게 자명한 프리미티브였다. 하부 스케줄러가 실제로 동작하는 방식과 일치하고, 다른 어떤 동시적 흐름 패턴이든 구현할 수 있을 만큼 강력하다. 하지만 역시 `goto`처럼 제어 흐름 추상화를 깨뜨리기 때문에, 언어에 선택지로 존재하는 것만으로 모든 것이 쓰기 어려워진다.

다행인 것은 이 문제들이 전부 해결 가능하다는 점이다. 데이크스트라가 방법을 보여 줬다! 우리가 할 일은:

  * 비슷한 힘을 가지면서 "블랙박스 규칙"을 따르는 `go` 문의 대체물을 찾고,
  * 그 새 구조를 동시성 프레임워크의 프리미티브로 내장하되, 어떤 형태의 `go` 문도 포함하지 않는 것이다.

그리고 그것이 Trio가 한 일이다.

## Nursery: `go` 문의 구조적 대체물

핵심 아이디어는 이렇다. 제어가 여러 동시 경로로 갈라질 때마다, 반드시 다시 합류하도록 만들자. 예를 들어 세 가지 일을 동시에 하고 싶다면 제어 흐름은 이런 모양이어야 한다.

![위에서 화살표 하나가 들어와 세 갈래로 갈라졌다가 아래에서 다시 하나로 합쳐지는 그림](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/nursery-schematic-unlabeled.svg)

위로 들어가는 화살표가 하나, 아래로 나오는 화살표가 하나뿐이라는 점에 주목하라. 데이크스트라의 블랙박스 규칙을 따른다. 그럼 이 스케치를 어떻게 구체적인 언어 구조로 바꿀 수 있을까? 이 제약을 만족하는 기존 구조들이 몇 가지 있긴 하다. 하지만 (a) 내 제안은 내가 아는 어떤 것과도 조금씩 다르고 그것들보다 나은 점이 있으며(특히 이걸 독립적인 프리미티브로 삼으려는 맥락에서), (b) 동시성 문헌은 방대하고 복잡해서 그 모든 역사와 트레이드오프를 파헤치다 보면 논의가 완전히 옆길로 샐 테니, 그건 별도의 글로 미루겠다. 여기서는 내 해법을 설명하는 데만 집중한다. 다만 내가 동시성이라는 개념을 발명했다고 주장하는 게 아니라는 점은 알아 두시라. 이 아이디어는 많은 원천에서 영감을 얻었고, 나는 거인들의 어깨 위에 서 있다. [3]

어쨌든 이렇게 하기로 하자. 먼저, 부모 태스크는 자식들이 살 곳, 즉 _nursery_ 를 먼저 만들지 않고서는 어떤 자식 태스크도 시작할 수 없다고 선언한다. 이는 _nursery 블록_ 을 여는 것으로 이루어진다. Trio에서는 Python의 `async with` 문법을 쓴다.

![async with trio.open_nursery() as nursery: 블록을 여는 코드 다이어그램](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/nursery-1-pathified.svg)

nursery 블록을 열면 이 nursery를 나타내는 객체가 자동으로 만들어지고, `as nursery` 문법이 이 객체를 `nursery`라는 변수에 할당한다. 그러면 nursery 객체의 `start_soon` 메서드로 동시 태스크를 시작할 수 있다. 이 경우 한 태스크는 함수 `myfunc`를, 다른 태스크는 `anotherfunc`를 호출한다. 개념적으로 이 태스크들은 nursery 블록 _안에서_ 실행된다. 사실 nursery 블록 안에 쓰인 코드 자체를, 블록이 만들어질 때 자동으로 시작되는 최초의 태스크라고 생각하면 편할 때가 많다.

![nursery 블록 안에서 start_soon으로 태스크를 시작하는 코드 다이어그램](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/nursery-2-pathified.svg)

결정적으로, nursery 블록은 그 안의 모든 태스크가 종료될 때까지 끝나지 않는다. 자식들이 다 끝나기 전에 부모 태스크가 블록 끝에 도달하면, 부모는 거기서 멈춰 자식들을 기다린다. nursery는 자식들을 담을 수 있도록 자동으로 늘어난다.

제어 흐름은 이렇다. 이 절의 첫머리에서 보여 준 기본 패턴과 일치하는 것을 확인할 수 있다.

![nursery 코드와 그 제어 흐름을 나란히 놓은 다이어그램: 제어가 갈라졌다가 블록 끝에서 다시 합류한다](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/nursery-3-pathified.svg)

이 설계에는 여러 귀결이 있고, 그중 일부는 자명하지 않다. 몇 가지를 짚어 보자.

### nursery는 함수 추상화를 보존한다.

`go` 문의 근본 문제는, 함수를 호출했을 때 그 함수가 자기가 끝난 뒤에도 계속 실행되는 백그라운드 태스크를 띄우는지 알 수 없다는 것이다. nursery에서는 이 걱정을 할 필요가 없다. 어떤 함수든 nursery를 열어 여러 동시 태스크를 실행할 수 있지만, 그것들이 전부 끝나기 전에는 함수가 반환할 수 없다. 그러니 함수가 반환했다면, 정말로 끝난 것이다.

### nursery는 동적 태스크 생성을 지원한다.

위의 제어 흐름 다이어그램을 만족하는 더 단순한 프리미티브도 있다. thunk의 리스트를 받아 전부 동시에 실행하는 것이다.

```
run_concurrently([myfunc, anotherfunc])
```

하지만 이것의 문제는 실행할 태스크의 전체 목록을 미리 알아야 한다는 점인데, 항상 그럴 수 있는 건 아니다. 예를 들어 서버 프로그램에는 대개 `accept` 루프가 있어서, 들어오는 연결을 받아 각각을 처리할 새 태스크를 시작한다. Trio의 최소한의 `accept` 루프는 이렇다.

```
async with trio.open_nursery() as nursery:
    while True:
        incoming_connection = await server_socket.accept()
        nursery.start_soon(connection_handler, incoming_connection)
```

nursery로는 이게 아무것도 아니지만, `run_concurrently`로 구현하려면 _훨씬_ 어색해진다. 그리고 원한다면 nursery 위에 `run_concurrently`를 구현하는 것은 쉽다. 다만 굳이 그럴 필요도 없는 게, `run_concurrently`가 감당할 수 있는 단순한 경우에는 nursery 표기도 그만큼 읽기 좋기 때문이다.

### 탈출구는 있다.

nursery 객체는 탈출구도 제공한다. 백그라운드 태스크를 띄우는 함수, 그것도 태스크가 함수 자체보다 오래 사는 함수를 정말로 써야 한다면? 간단하다. 함수에 nursery 객체를 넘기면 된다. `async with open_nursery()` 블록 바로 안쪽의 코드만 `nursery.start_soon`을 호출할 수 있다는 규칙은 없다. nursery 블록이 열려 있는 한 [4], nursery 객체에 대한 참조를 얻은 누구든 그 nursery 안으로 태스크를 띄울 능력을 얻는다. 함수 인자로 넘겨도 되고, 큐로 보내도 되고, 뭐든 좋다.

실질적으로 이는 "규칙을 어기는" 함수를 쓸 수는 있되, 한계 안에서만 가능하다는 뜻이다.

  * nursery 객체는 명시적으로 전달해야 하므로, 호출 지점만 봐도 어떤 함수가 통상적인 제어 흐름을 벗어나는지 즉시 식별할 수 있다. 지역적 추론이 여전히 가능하다.
  * 함수가 띄우는 태스크는 여전히 넘겨받은 nursery의 수명에 묶여 있다.
  * 그리고 호출하는 쪽은 자기가 접근할 수 있는 nursery 객체만 넘길 수 있다.

그러니 이것은 아무 코드나 아무 때나 수명 제한 없는 백그라운드 태스크를 띄울 수 있는 전통적 모델과는 여전히 크게 다르다.

이것이 유용한 곳 중 하나가 nursery가 `go` 문과 동등한 표현력을 갖는다는 증명인데, 이 글이 이미 충분히 길어서 그건 다음 기회로 남겨 둔다.

### nursery처럼 동작하는 새 타입을 정의할 수 있다.

표준 nursery 의미론은 튼튼한 토대를 제공하지만, 때로는 다른 것이 필요하다. Erlang의 supervisor가 부러워서, 자식 태스크를 재시작하는 방식으로 예외를 처리하는 nursery 비슷한 클래스를 정의하고 싶을 수도 있다. 얼마든지 가능하고, 사용자에게는 평범한 nursery와 똑같아 보일 것이다.

```
async with my_supervisor_library.open_supervisor() as nursery_alike:
    nursery_alike.start_soon(...)
```

nursery를 인자로 받는 함수가 있다면, 대신 이런 것을 넘겨서 그 함수가 띄우는 태스크들의 오류 처리 정책을 제어할 수 있다. 꽤 근사하다. 다만 여기에는 Trio를 asyncio나 다른 라이브러리들과 다른 관례로 이끄는 미묘한 지점이 하나 있다. `start_soon`이 코루틴 객체나 `Future`가 아니라 함수를 받아야 한다는 것이다. (함수는 여러 번 호출할 수 있지만, 코루틴 객체나 `Future`를 재시작할 방법은 없다.) 여러 이유로 어차피 이쪽이 더 나은 관례라고 생각하지만(특히 Trio에는 `Future` 자체가 없다!), 그래도 언급할 가치는 있다.

### 아니, 정말로, nursery는 내부 태스크가 종료될 때까지 _항상_ 기다린다.

태스크 취소와 태스크 join이 어떻게 상호작용하는지도 짚고 넘어갈 만하다. 잘못 다루면 nursery의 불변식을 깨뜨릴 수 있는 미묘한 지점들이 있기 때문이다.

Trio에서는 코드가 언제든 취소 요청을 받을 수 있다. 취소가 요청된 뒤, 코드가 다음번 "체크포인트" 연산을 실행할 때([자세한 내용](https://trio.readthedocs.io/en/latest/reference-core.html#checkpoints)) `Cancelled` 예외가 발생한다. 즉 취소가 _요청된_ 시점과 실제로 _일어나는_ 시점 사이에는 간격이 있다. 태스크가 체크포인트를 실행하기까지 시간이 걸릴 수 있고, 그 뒤에도 예외가 스택을 되감고 정리 핸들러를 실행하는 등의 과정이 남아 있다. 이런 일이 벌어질 때 nursery는 항상 정리가 완전히 끝나기를 기다린다. 정리 핸들러를 실행할 기회를 주지 않고 태스크를 종료시키는 일은 _결코_ 없고, 설령 취소되는 중이라도 태스크를 nursery 바깥에 감독 없이 방치하는 일도 _결코_ 없다.

### 자동 리소스 정리가 동작한다.

nursery는 블랙박스 규칙을 따르기 때문에 `with` 블록이 다시 제대로 동작하게 만든다. 이를테면 `with` 블록 끝에서 파일을 닫는 바람에, 그 파일을 아직 쓰고 있던 백그라운드 태스크가 갑자기 망가질 일이 없다.

### 자동 오류 전파가 동작한다.

위에서 말했듯이, 대부분의 동시성 시스템에서 백그라운드 태스크의 처리되지 않은 오류는 그냥 버려진다. 말 그대로 달리 어쩔 도리가 없다.

Trio에서는 모든 태스크가 nursery 안에 살고, 모든 nursery는 부모 태스크의 일부이며, 부모 태스크는 nursery 안의 태스크들을 기다려야 하므로... 처리되지 않은 오류를 가지고 _할 수 있는 일_ 이 생긴다. 백그라운드 태스크가 예외와 함께 종료되면, 그 예외를 부모 태스크에서 다시 던질 수 있다. 직관은 이렇다. nursery는 일종의 "동시 호출" 프리미티브다. 위의 예제를 `myfunc`와 `anotherfunc`를 동시에 호출하는 것으로 생각할 수 있으니, 우리의 호출 스택은 트리가 된 셈이다. 그리고 예외는 일반 호출 스택을 타고 올라가듯이, 이 호출 트리의 루트를 향해 위로 전파된다.

다만 여기에도 미묘한 점이 하나 있다. 부모 태스크에서 예외를 다시 던지면, 예외는 부모 태스크 안에서 전파되기 시작한다. 일반적으로 그것은 부모 태스크가 nursery 블록을 빠져나간다는 뜻이다. 하지만 자식 태스크가 아직 실행 중인 동안에는 부모가 nursery 블록을 떠날 수 없다고 이미 말했다. 그럼 어떻게 해야 할까?

답은 이렇다. 자식에서 처리되지 않은 예외가 발생하면, Trio는 같은 nursery 안의 다른 모든 태스크를 즉시 취소하고, 그것들이 끝나기를 기다린 뒤에 예외를 다시 던진다. 직관은, 예외는 스택을 되감는 것이고, 스택 트리의 분기점을 지나 되감으려면 다른 가지들도 취소해서 함께 되감아야 한다는 것이다.

이는 곧, 당신의 언어에서 nursery를 구현하려면 nursery 코드와 취소 시스템 사이에 어떤 통합이 필요할 수 있다는 뜻이기도 하다. C#이나 Golang처럼 취소가 보통 수동적인 객체 전달과 관례로 관리되는 언어라면 까다로울 수 있고, 범용 취소 메커니즘이 아예 없는 언어라면 (더 나쁘게도) 더욱 그렇다.

### 뜻밖의 이득: `go` 문을 없애자 새로운 기능이 가능해졌다

`goto`를 제거하자 과거의 언어 설계자들이 프로그램 구조에 대해 더 강한 가정을 할 수 있게 되었고, 그 덕에 `with` 블록이나 예외 같은 새 기능이 가능해졌다. `go` 문을 제거하는 것도 비슷한 효과를 낸다. 예를 들면:

  * Trio의 취소 시스템은 태스크들이 규칙적인 트리 구조로 중첩되어 있다고 가정할 수 있기 때문에 경쟁자들보다 쓰기 쉽고 신뢰할 수 있다. 자세한 논의는 [Timeouts and cancellation for humans](https://vorpus.org/blog/timeouts-and-cancellation-for-humans/)를 보라.
  * Trio는 Ctrl-C가 Python 개발자들이 기대하는 대로 동작하는 유일한 Python 동시성 라이브러리다([자세한 내용](https://vorpus.org/blog/control-c-handling-in-python-and-trio/)). nursery가 예외 전파를 위한 신뢰할 수 있는 메커니즘을 제공하지 않았다면 불가능했을 일이다.

## 실전에서의 nursery

여기까지가 이론이다. 실전에서는 어떨까?

음... 그건 경험적인 질문이다. 직접 써 보고 확인해 보시라! 진지하게 말하자면, 많은 사람이 두들겨 보기 전까지는 확실히 알 수 없다. 현시점에서 나는 토대가 견고하다고 꽤 확신하지만, 초기 구조적 프로그래밍 옹호자들이 결국 `break`와 `continue` 제거에서 한발 물러섰던 것처럼, 우리도 손볼 곳이 있다는 걸 깨닫게 될지 모른다.

그리고 당신이 Trio를 이제 막 배우는 숙련된 동시성 프로그래머라면, 때때로 험난하게 느껴질 각오를 해야 한다. [새로운 방식들을 배워야 할 것이다](https://stackoverflow.com/questions/48282841/in-trio-how-can-i-have-a-background-task-that-lives-as-long-as-my-object-does). 1970년대의 프로그래머들이 `goto` 없이 코드 쓰는 법을 배우느라 애먹었던 것처럼.

하지만 물론 그게 요점이다. 커누스가 썼듯이([Knuth, 1974](https://scholar.google.com/scholar?cluster=17147143327681396418&hl=en&as_sdt=0,5), p. 275):

> **go to** 문이라는 주제에 관해 누군가가 저지를 수 있는 최악의 실수는 아마도, 프로그램을 늘 하던 대로 짠 다음 **go to** 를 제거하는 것으로 "구조적 프로그래밍"이 달성된다고 가정하는 것이다. 대부분의 **go to** 는 애초에 거기 있지 말았어야 한다! 우리가 정말 원하는 것은 **go to** 문을 거의 _생각조차_ 하지 않는 방식으로 프로그램을 구상하는 것이다. 그것이 정말로 필요한 경우가 거의 없기 때문이다. 우리가 생각을 표현하는 언어는 사고 과정에 강한 영향을 미친다. 그래서 데이크스트라는 **go to** 가 복잡함으로 우리를 유혹하는 것을 피하기 위해, 더 많은 새로운 언어 기능 – 명료한 사고를 북돋는 구조들 – 을 요구하는 것이다.

그리고 지금까지 nursery를 쓰면서 내가 겪은 것이 바로 그것이다. nursery는 명료한 사고를 북돋는다. 더 견고하고, 더 쓰기 쉽고, 전반적으로 그냥 더 나은 설계로 이어진다. 그리고 그 제약들은 오히려 문제 해결을 쉽게 만든다. 불필요한 복잡함으로 유혹당하는 시간이 줄어들기 때문이다. Trio를 쓰는 것은 아주 실질적인 의미에서 나를 더 나은 프로그래머로 만들어 주었다.

예를 들어 Happy Eyeballs 알고리즘([RFC 8305](https://tools.ietf.org/html/rfc8305))을 보자. TCP 연결 수립을 빠르게 하기 위한 단순한 동시 알고리즘이다. 개념적으로는 복잡하지 않다. 여러 연결 시도를 서로 경주시키되, 네트워크 과부하를 피하려고 시작 시점을 엇갈리게 하는 것뿐이다. 그런데 [Twisted의 가장 잘 만든 구현](https://github.com/twisted/twisted/compare/trunk...glyph:statemachine-hostnameendpoint)을 보면 Python 코드로 거의 600줄인 데다, 여전히 [최소한 하나의 논리 버그](https://twistedmatrix.com/trac/ticket/9345)가 있다. Trio의 동등한 구현은 **15배** 이상 짧다. 더 중요한 것은, Trio를 써서 나는 몇 달이 아니라 몇 분 만에 그것을 작성할 수 있었고, 첫 시도에 로직을 올바르게 짰다는 점이다. 다른 어떤 프레임워크에서도, 심지어 내가 훨씬 경험이 많은 프레임워크에서도 이건 불가능했을 것이다. 자세한 내용은 [지난달 Pyninsula에서 한 내 발표](https://www.youtube.com/watch?v=i-R704I8ySE)를 보면 된다. 이게 일반적인 경우일까? 시간이 말해 줄 것이다. 하지만 확실히 유망하다.

## 결론

인기 있는 동시성 프리미티브들 – `go` 문, 스레드 생성 함수, 콜백, future, promise, ... 이것들은 이론적으로도 실제로도 전부 `goto`의 변종이다. 그것도 현대의 길들여진 `goto`가 아니라, 함수 경계를 뛰어넘을 수 있었던 구약 시대의 유황불 `goto` 말이다. 이 프리미티브들은 우리가 직접 쓰지 않더라도 위험하다. 제어 흐름을 추론하고 추상적이고 모듈화된 부품으로 복잡한 시스템을 조립하는 우리의 능력을 잠식하고, 자동 리소스 정리나 오류 전파 같은 유용한 언어 기능을 방해하기 때문이다. 그러므로 `goto`와 마찬가지로, 현대적인 고수준 언어에 이것들이 있을 자리는 없다.

nursery는 언어의 온전한 힘을 보존하는 안전하고 편리한 대안이며, 강력한 새 기능들을 가능하게 하고(Trio의 취소 스코프와 Ctrl-C 처리가 보여 주듯이), 가독성과 생산성과 정확성에서 극적인 개선을 이끌어 낼 수 있다.

안타깝게도 이 이점을 온전히 누리려면 옛 프리미티브들을 완전히 제거해야 하고, 그러자면 아마 새로운 동시성 프레임워크를 밑바닥부터 만들어야 할 것이다. `goto`를 없애는 데 새 언어들을 설계해야 했던 것처럼 말이다. 하지만 FLOW-MATIC이 당대에 아무리 인상적이었다 해도, 우리 대부분은 더 나은 것으로 갈아탄 것을 다행으로 여긴다. nursery로 갈아타는 것도 후회하지 않으리라 생각하고, Trio는 이것이 실용적인 범용 동시성 프레임워크로 성립 가능한 설계임을 보여 준다.

## 댓글

이 글은 [Trio 포럼에서 토론할 수 있다](https://trio.discourse.group/t/discussion-thread-notes-on-structured-concurrency-or-go-statement-considered-harmful/25).

## 감사의 말

이 글의 초안에 의견을 준 Graydon Hoare, Quentin Pradet, Hynek Schlawack에게 깊이 감사한다. 남아 있는 오류는 물론 전부 내 잘못이다.

크레딧: FLOW-MATIC 샘플 코드는 [이 브로슈어](http://archive.computerhistory.org/resources/text/Remington_Rand/Univac.Flowmatic.1957.102646140.pdf)(PDF)에서 가져왔으며, [Computer History Museum이 보존하고 있다](http://www.computerhistory.org/collections/catalog/102646140). [Wolves in Action](https://www.flickr.com/photos/iam_photo/478178221)은 i:am. photography / Martin Pannier의 작품으로 [CC-BY-SA 2.0](https://creativecommons.org/licenses/by-nc-sa/2.0/) 라이선스이며 잘라서 사용했다. [French Bulldog Pet Dog](https://pixabay.com/en/french-bulldog-pet-dog-funny-2427629/)는 Daniel Borker의 작품으로 [CC0 퍼블릭 도메인 헌정](https://creativecommons.org/publicdomain/zero/1.0/)으로 공개되었다.

## 각주

[1] 적어도 어떤 부류의 사람에게는 그렇다.

[2] 그리고 WebAssembly는 `goto` 없는 저수준 어셈블리 언어가 가능하며 어느 정도는 바람직하기까지 하다는 것을 보여 준다: [레퍼런스](https://www.w3.org/TR/wasm-core-1/#control-instructions%E2%91%A0), [설계 근거](https://github.com/WebAssembly/design/blob/master/Rationale.md#control-flow)

[3] 자기가 좋아하는 논문을 내가 아는지부터 확인해야 글에 집중할 수 있는 분들을 위해, 나중에 쓸 리뷰에 포함할 주제들의 현재 목록을 밝혀 둔다: Cooperating/Communicating Sequential Processes와 Occam의 "병렬 합성" 연산자, fork/join 모델, Erlang supervisor, Martin Sústrik의 [Structured concurrency](http://250bpm.com/blog:71) 글과 [libdill](https://github.com/sustrik/libdill) 작업, 그리고 Rust의 [crossbeam::scope](https://docs.rs/crossbeam/0.3.2/crossbeam/struct.Scope.html) / [rayon::scope](https://docs.rs/rayon/1.0.1/rayon/fn.scope.html). [수정: Golang의 [golang.org/x/sync/errgroup](https://godoc.org/golang.org/x/sync/errgroup)과 [github.com/oklog/run](https://godoc.org/github.com/oklog/run)도 매우 관련이 깊다고 제보받았다.] 중요한 것이 빠졌다면 [알려 달라](mailto:njs@pobox.com).

[4] nursery 블록이 끝난 _뒤에_ `start_soon`을 호출하면 `start_soon`이 오류를 던지고, 반대로 오류를 던지지 않았다면 태스크가 끝날 때까지 nursery 블록이 열려 있음이 보장된다. 직접 nursery 시스템을 구현한다면 이 부분의 동기화를 신중하게 다뤄야 할 것이다.
