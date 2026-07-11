# 스레드는 없다

> 원문: [There Is No Thread](https://blog.stephencleary.com/2013/11/there-is-no-thread.html)
> 저자: Stephen Cleary | 번역일: 2026-07-10

이것이 가장 순수한 형태의 async가 지닌 본질적 진리다: **스레드는 없다.**

이 진리에 반대하는 이들은 수없이 많다. "아니다," 그들은 외친다. "어떤 연산을 await하고 있다면, 그 대기를 수행하는 스레드가 *반드시* 있어야 한다! 아마 스레드 풀 스레드일 것이다. 아니면 OS 스레드! 아니면 디바이스 드라이버의 무언가…"

그 외침에 귀 기울이지 말라. 비동기 연산이 순수하다면, 스레드는 없다.

회의론자들은 납득하지 못한다. 그들의 청을 들어주자.

비동기 연산 하나를 하드웨어까지 끝까지 추적해 보겠다. .NET 부분과 디바이스 드라이버 부분에 특히 주목한다. 중간 계층의 세부 사항 일부는 생략해서 설명을 단순화하겠지만, 진실에서 벗어나지는 않을 것이다.

일반적인 "쓰기" 연산을 생각해 보자(파일이든, 네트워크 스트림이든, USB 토스터든 무엇이든). 코드는 단순하다:

```csharp
private async void Button_Click(object sender, RoutedEventArgs e)
{
  byte[] data = ...
  await myDevice.WriteAsync(data, 0, data.Length);
}
```

`await` 동안 UI 스레드가 블록되지 않는다는 건 이미 알고 있다. 질문: UI 스레드가 살 수 있도록 블로킹의 제단에 자신을 바쳐야 하는 *다른 스레드*가 있는가?

내 손을 잡아라. 깊이 잠수해 보자.

첫 번째 정거장: 라이브러리(즉 BCL 코드로 진입). `WriteAsync`가 [.NET의 표준 P/Invoke 비동기 I/O 시스템](http://msdn.microsoft.com/en-us/library/system.threading.overlapped.aspx?WT.mc_id=DT-MVP-5000058)으로 구현되어 있다고 가정하자. 이 시스템은 overlapped I/O에 기반한다. 그래서 이 호출은 디바이스의 밑단 `HANDLE`에 대해 Win32 overlapped I/O 연산을 시작한다.

그다음 OS는 디바이스 드라이버를 향해 쓰기 연산을 시작하라고 요청한다. 먼저 쓰기 요청을 나타내는 객체를 만드는데, 이를 I/O 요청 패킷(IRP, I/O Request Packet)이라 부른다.

디바이스 드라이버는 IRP를 받아 디바이스에 데이터를 써내라는 명령을 내린다. 디바이스가 DMA(Direct Memory Access)를 지원하면 버퍼 주소를 디바이스 레지스터에 쓰는 것만으로 끝날 수도 있다. 디바이스 드라이버가 할 수 있는 건 그게 전부다. 드라이버는 IRP를 "대기 중(pending)"으로 표시하고 OS로 반환한다.

[![](https://blog.stephencleary.com/assets/Os1.png)](https://blog.stephencleary.com/assets/Os1.png)

진리의 핵심이 바로 여기에 있다: 디바이스 드라이버는 IRP를 처리하는 동안 블록하는 것이 허용되지 않는다. 즉 IRP를 *즉시* 완료할 수 없다면 **반드시** *비동기적으로* 처리해야 한다. 동기 API에 대해서도 마찬가지다! 디바이스 드라이버 수준에서는 (단순하지 않은) 모든 요청이 비동기다.

> [지식의](https://www.amazon.com/gp/product/0735648735/ref=as_li_ss_tl?ie=UTF8&camp=1789&creative=390957&creativeASIN=0735648735&linkCode=as2&tag=stepheclearys-20) [보고](https://www.amazon.com/gp/product/0735665877/ref=as_li_ss_tl?ie=UTF8&camp=1789&creative=390957&creativeASIN=0735665877&linkCode=as2&tag=stepheclearys-20)를 인용하면, "I/O 요청의 종류와 무관하게, 애플리케이션을 대신해 드라이버에 발행되는 I/O 연산은 내부적으로 비동기적으로 수행된다".

IRP가 "대기 중"인 상태로, OS는 라이브러리로 반환하고, 라이브러리는 완료되지 않은 태스크를 버튼 클릭 이벤트 핸들러에 반환하고, 그러면 async 메서드가 일시 중단되고, UI 스레드는 계속 실행된다.

우리는 요청을 따라 시스템의 심연 속으로, 물리 디바이스에까지 내려왔다.

쓰기 연산은 이제 "비행 중(in flight)"이다. 몇 개의 스레드가 이 연산을 처리하고 있는가?

0개다.

그 쓰기 연산을 처리하고 있는 디바이스 드라이버 스레드도, OS 스레드도, BCL 스레드도, 스레드 풀 스레드도 없다. **스레드는 없다.**

이제 커널 악령들의 땅에서 필멸자들의 세계로 돌아오는 응답을 따라가 보자.

쓰기 요청이 시작되고 얼마 뒤, 디바이스가 쓰기를 마친다. 디바이스는 인터럽트로 CPU에 알린다.

디바이스 드라이버의 인터럽트 서비스 루틴(ISR)이 인터럽트에 응답한다. 인터럽트는 CPU 수준의 이벤트로, 실행 중이던 스레드가 무엇이든 그로부터 CPU의 제어권을 일시적으로 빼앗는다. ISR이 현재 실행 중인 스레드를 "빌린다"고 생각할 *수도* 있지만, 나는 ISR이 "스레드"라는 개념 자체가 *존재하지* 않을 만큼 낮은 수준에서 실행된다고 보는 편이다. 말하자면 모든 스레드 "아래에서" 들어오는 것이다.

어쨌든 이 ISR은 제대로 작성되어 있어서, 하는 일이라곤 디바이스에게 "인터럽트 고맙다"라고 말하고 지연 프로시저 호출(DPC, Deferred Procedure Call)을 큐에 넣는 것뿐이다.

CPU는 인터럽트에 시달리는 일이 끝나면 DPC들을 처리한다. DPC 역시 "스레드"를 논하기엔 적절치 않을 만큼 낮은 수준에서 실행된다. ISR처럼 DPC도 스레딩 시스템 "아래에서" CPU 위에 직접 실행된다.

DPC는 쓰기 요청을 나타내는 IRP를 가져다 "완료"로 표시한다. 그러나 그 "완료" 상태는 OS 수준에만 존재한다. 프로세스에는 자기만의 메모리 공간이 있으므로 별도로 통지받아야 한다. 그래서 OS는 `HANDLE`을 소유한 스레드에 특수 커널 모드 비동기 프로시저 호출(APC, Asynchronous Procedure Call)을 큐잉한다.

라이브러리/BCL은 표준 P/Invoke overlapped I/O 시스템을 쓰고 있으므로, 이미 그 핸들을 [I/O 완료 포트(IOCP)에 등록](http://msdn.microsoft.com/en-us/library/system.threading.threadpool.bindhandle.aspx?WT.mc_id=DT-MVP-5000058)해 두었다. IOCP는 스레드 풀의 일부다. 그래서 I/O 스레드 풀 스레드 하나가 잠깐 빌려져 APC를 실행하고, APC가 태스크에 완료를 통지한다.

그 태스크는 UI 컨텍스트를 캡처해 두었으므로, 스레드 풀 스레드에서 곧바로 `async` 메서드를 재개하지 않는다. 대신 그 메서드의 연속(continuation)을 UI 컨텍스트에 큐잉하고, UI 스레드가 여유가 될 때 그 메서드의 실행을 재개한다.

이렇게, 요청이 비행 중인 동안 스레드는 없었음을 보았다. 요청이 완료되자 여러 스레드가 잠깐 "빌려지거나" 짧게 작업을 큐잉받았다. 이 작업은 보통 1밀리초 정도(예: 스레드 풀에서 실행되는 APC)에서 1마이크로초 정도(예: ISR) 수준이다. 하지만 그 요청이 완료되기만을 기다리며 블록된 스레드는 없다.

[![](https://blog.stephencleary.com/assets/Os2.png)](https://blog.stephencleary.com/assets/Os2.png)

지금 우리가 따라간 경로는 다소 단순화한 "표준" 경로다. 무수한 변형이 있지만, 핵심 진리는 같다.

"어딘가에서 비동기 연산을 *처리하는* 스레드가 반드시 있을 것이다"라는 생각은 진리가 아니다.

마음을 해방하라. 그 "async 스레드"를 찾으려 하지 말라 — 그건 불가능하다. 대신, 오직 진리를 깨달으려 하라:

**스레드는 없다.**
