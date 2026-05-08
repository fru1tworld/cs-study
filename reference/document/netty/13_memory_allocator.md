# 메모리 할당자 동작 분석

> 이 문서는 Netty 공식 Wiki의 "Analyzing Memory Allocator Behavior" 페이지를 한국어로 번역한 것입니다.
> 원본: https://netty.io/wiki/analyzing-memory-allocator-behavior.html

---

Netty 4.2는 `ByteBuf` 인스턴스를 만드는 데 사용되는 세 가지 서로 다른 메모리 할당자를 포함합니다.

1. `unpooled` 할당자는 호출할 때마다 시스템 메모리에서 할당하고, `ByteBuf`가 더 이상 사용되지 않으면 즉시 시스템에 반환합니다.
2. `pooled` 할당자는 시스템 메모리의 _chunk_ 단위로 할당하고, 여러 `ByteBuf` 인스턴스 사이에서 공유·재사용합니다. 메모리는 arena, chunk, size-class로 조직되며, 이는 `jemalloc` 할당자의 설계를 따릅니다. 이 할당자는 Netty 4.1의 기본값이었습니다.
3. `adaptive` 할당자도 `pooled`처럼 시스템 메모리의 _chunk_를 할당합니다. chunk는 일반적으로 더 작고 더 많으며, magazine이라는 그룹에 모입니다. magazine은 size class당 하나씩 magazine group으로 조직됩니다. 그룹은 멀티스레드 경쟁(contention)에 반응해 커집니다. 이 할당자는 Netty 4.2의 **기본값**입니다.

각 할당자에는 장단점이 있어, 어떤 애플리케이션에는 더 적합하고 어떤 애플리케이션에는 덜 적합합니다.

Netty가 기본으로 사용할 할당자는 `io.netty.allocator.type` 시스템 프로퍼티로 제어하며, 위에 나온 `name` 중 하나로 설정합니다.

## FlightRecorder 이벤트 기록

`pooled`와 `adaptive` 할당자는 버퍼나 chunk를 할당/해제할 때 Java FlightRecorder 이벤트를 발사할 수 있습니다.

이 이벤트들은 성능 오버헤드가 꽤 크기 때문에 기본적으로 비활성화되어 있습니다. 대신 기록에 사용되는 FlightRecorder 프로파일에서 명시적으로 활성화해야 합니다.

Netty 팀은 _오직_ Netty 할당자 이벤트만 포함하고 다른 모든 것은 비활성화하는 FlightRecorder 프로파일을 함께 만들어 두었습니다. 이 프로파일은 다음 위치에서 다운로드할 수 있습니다: https://github.com/netty/netty/blob/4.2/microbench/src/main/resources/Netty%20Allocator%20Events.jfc

이 프로파일을 사용하면 결과 기록에 개인 정보나 사적 정보, 지적 재산이 포함되지 않으므로 결과를 공개적으로 공유할 수 있다는 장점도 있습니다.

프로파일을 Netty 애플리케이션이 실행 중인 시스템에 다운로드한 뒤, 다음 명령으로 할당 프로파일을 얻을 수 있습니다.

* 프로파일링하려는 Netty 애플리케이션의 PID 얻기:
  * `$ jps`
* 다음과 비슷한 명령으로 프로파일링 시작:
  * `$ jcmd <PID> JFR.start name=netty-allocator-profiling duration=30s filename=netty-allocator.jfr settings=/path/to/netty.jfc maxsize=200m`
* 주기적으로 상태를 확인하면서 프로파일링이 끝나기를 기다립니다:
  * `$ jcmd <PID> JFR.check`
* 기록이 끝나면 `.jfr` 파일을 다운로드해서 분석할 수 있습니다.

## 할당자 Flight Recording 분석

`netty-microbench` 모듈에는 `AllocationPatternSimulator`라는 프로그램이 있습니다. 이 프로그램은 할당 프로파일을 분석하고 `pooled` 및 `adaptive` 할당자에 동시에 적용해 그 할당 패턴을 시뮬레이션하며, 두 할당자의 메모리 사용량을 비교 차트로 보여줍니다.

이 프로그램을 사용하려면 Netty 소스 코드를 체크아웃하고 컴파일해야 합니다. 그런 다음 `AllocationPatternSimulator` 클래스를 실행하면서 첫 번째 프로그램 인자로 할당 프로파일 `.jfr` 파일을 전달하면 됩니다.
