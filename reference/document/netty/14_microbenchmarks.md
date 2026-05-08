# Microbenchmarks

> 이 문서는 Netty 공식 Wiki의 "Microbenchmarks" 페이지를 한국어로 번역한 것입니다.
> 원본: https://netty.io/wiki/microbenchmarks.html

---

Netty에는 일련의 마이크로벤치마크 테스트를 수행하는 'netty-microbench'라는 모듈이 있습니다. 이 모듈은 HotSpot의 표준 마이크로벤치마킹 솔루션인 [OpenJDK JMH](http://openjdk.java.net/projects/code-tools/jmh/) 위에서 동작합니다. "건전지 포함" 방식이라 별도의 의존성을 추가할 필요 없이 바로 시작할 수 있습니다.

## 벤치마크 실행하기

명령줄에서 maven으로 실행하거나 IDE에서 직접 실행할 수 있습니다. 모든 테스트를 기본 설정으로 실행하려면 `mvn -DskipTests=false test`를 사용합니다. `skipTests=false`를 명시적으로 설정해야 하는 이유는, 일반 테스트 실행에서 (시간이 꽤 걸릴 수 있는) 마이크로벤치마크가 단위 테스트처럼 실행되는 것을 원치 않기 때문입니다.

순조롭게 진행되면 JMH가 지정된 fork 수만큼 워밍업과 측정 반복을 수행하면서 깔끔한 요약을 보여줍니다. 일반적인 벤치마크 실행은 다음과 같이 보입니다(출력에서 이런 결과를 여러 번 보게 됩니다).

```
# Fork: 2 of 2
# Warmup: 10 iterations, 1 s each
# Measurement: 10 iterations, 1 s each
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Throughput, ops/time
# Running: io.netty.microbench.buffer.ByteBufAllocatorBenchmark.pooledDirectAllocAndFree_1_0
# Warmup Iteration   1: 8454.103 ops/ms
# Warmup Iteration   2: 11551.524 ops/ms
# Warmup Iteration   3: 11677.575 ops/ms
# Warmup Iteration   4: 11404.954 ops/ms
# Warmup Iteration   5: 11553.299 ops/ms
# Warmup Iteration   6: 11514.766 ops/ms
# Warmup Iteration   7: 11661.768 ops/ms
# Warmup Iteration   8: 11667.577 ops/ms
# Warmup Iteration   9: 11551.240 ops/ms
# Warmup Iteration  10: 11692.991 ops/ms
Iteration   1: 11633.877 ops/ms
Iteration   2: 11740.063 ops/ms
Iteration   3: 11751.798 ops/ms
Iteration   4: 11260.071 ops/ms
Iteration   5: 11461.010 ops/ms
Iteration   6: 11642.912 ops/ms
Iteration   7: 11808.595 ops/ms
Iteration   8: 11683.780 ops/ms
Iteration   9: 11750.292 ops/ms
Iteration  10: 11769.986 ops/ms

Result : 11650.238 ±(99.9%) 229.698 ops/ms
  Statistics: (min, avg, max) = (11260.071, 11650.238, 11808.595), stdev = 169.080
  Confidence interval (99.9%): [11420.540, 11879.937]
```

마지막으로 테스트 출력은 다음과 비슷한 모습이 됩니다(시스템 설정과 구성에 따라 다름).

```
Benchmark                                                                Mode   Samples         Mean   Mean error    Units
i.n.m.b.ByteBufAllocatorBenchmark.pooledDirectAllocAndFree_1_0          thrpt        20    11658.812      120.728   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.pooledDirectAllocAndFree_2_256        thrpt        20    10308.626      147.528   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.pooledDirectAllocAndFree_3_1024       thrpt        20     8855.815       55.933   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.pooledDirectAllocAndFree_4_4096       thrpt        20     5545.538     1279.721   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.pooledDirectAllocAndFree_5_16384      thrpt        20     6741.581       75.975   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.pooledDirectAllocAndFree_6_65536      thrpt        20     7252.869       70.609   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.pooledHeapAllocAndFree_1_0            thrpt        20     9750.225       73.900   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.pooledHeapAllocAndFree_2_256          thrpt        20     9936.639      657.818   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.pooledHeapAllocAndFree_3_1024         thrpt        20     8903.130      197.533   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.pooledHeapAllocAndFree_4_4096         thrpt        20     6664.157       74.163   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.pooledHeapAllocAndFree_5_16384        thrpt        20     6374.924      337.869   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.pooledHeapAllocAndFree_6_65536        thrpt        20     6386.337       44.960   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.unpooledDirectAllocAndFree_1_0        thrpt        20     2137.241       30.792   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.unpooledDirectAllocAndFree_2_256      thrpt        20     1873.727       41.843   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.unpooledDirectAllocAndFree_3_1024     thrpt        20     1902.025       34.473   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.unpooledDirectAllocAndFree_4_4096     thrpt        20     1534.347       20.509   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.unpooledDirectAllocAndFree_5_16384    thrpt        20      838.804       12.575   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.unpooledDirectAllocAndFree_6_65536    thrpt        20      276.976        3.021   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.unpooledHeapAllocAndFree_1_0          thrpt        20    35820.568      259.187   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.unpooledHeapAllocAndFree_2_256        thrpt        20    19660.951      295.012   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.unpooledHeapAllocAndFree_3_1024       thrpt        20     6264.614       77.704   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.unpooledHeapAllocAndFree_4_4096       thrpt        20     2921.598       95.492   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.unpooledHeapAllocAndFree_5_16384      thrpt        20      991.631       49.220   ops/ms
i.n.m.b.ByteBufAllocatorBenchmark.unpooledHeapAllocAndFree_6_65536      thrpt        20      261.718       11.108   ops/ms
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 993.382 sec - in io.netty.microbench.buffer.ByteBufAllocatorBenchmark
```

벤치마크는 IDE에서 직접 실행할 수도 있습니다. netty 부모 프로젝트를 임포트했다면 `microbench` 서브프로젝트를 열고 `src/main/java/io/netty/microbench` 네임스페이스로 이동합니다. `buffer` 네임스페이스에서 `ByteBufAllocatorBenchmark`를 다른 JUnit 기반 테스트처럼 실행할 수 있습니다. 차이점은 (현재로서는) 전체 벤치마크를 한꺼번에만 실행할 수 있고 개별 서브 벤치마크를 따로 실행할 수는 없다는 점입니다. 콘솔에서 `mvn`으로 직접 실행했을 때와 같은 출력을 보게 됩니다.

## 벤치마크 작성하기

벤치마크 자체를 작성하는 일은 어렵지 않지만, 제대로 작성하기는 까다롭습니다. microbench 프로젝트가 사용하기 어려워서가 아니라, 벤치마크를 작성할 때 흔히 빠지는 함정을 피해야 하기 때문입니다. 다행히 JMH 스위트는 그런 함정 대부분을 완화해주는 유용한 애너테이션과 기능을 제공합니다. 시작하려면 벤치마크가 `AbstractMicrobenchmark`를 상속하도록 만들면 됩니다. 이는 JUnit으로 테스트가 실행되도록 보장하고 일부 기본값을 설정해줍니다.

```java
public class MyBenchmark extends AbstractMicrobenchmark {

}
```

다음 단계는 `@GenerateMicroBenchmark`로 애노테이션된(그리고 설명적인 이름을 가진) 메서드를 만드는 것입니다.

```java
@GenerateMicroBenchmark
public void measureSomethingHere() {

}
```

지금부터 가장 좋은 방법은 [여기](http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/)에서 적절한 JMH 테스트 작성법에 대한 샘플과 영감을 얻는 것입니다. 또한 JMH 주요 작성자 중 한 명의 [발표 자료](http://shipilev.net/#benchmarking)도 확인해보세요.

## 런타임 조건 커스터마이징

`AbstractMicrobenchmark`의 기본 설정은 다음과 같습니다.

- 워밍업 반복: 10
- 측정 반복: 10
- fork 수: 2

이 설정들은 런타임에 시스템 프로퍼티(`warmupIterations`, `measureIterations`, `forks`)로 변경할 수 있습니다.

```
mvn -DskipTests=false -DwarmupIterations=2 -DmeasureIterations=3 -Dforks=1 test
```

일반적으로 이렇게 적은 반복 횟수를 사용하는 것은 권장되지 않지만, 벤치마크가 잘 동작하는지만 확인하고 나중에 본격적인 벤치마크를 돌릴 때 유용할 수 있습니다.

애노테이션을 통해 테스트 단위로 기본 설정을 커스터마이징할 수도 있습니다.

```java
@Warmup(iterations = 20)
@Fork(1)
public class MyBenchmark extends AbstractMicrobenchmark {

}
```

이는 클래스 단위와 메서드(벤치마크) 단위로 적용할 수 있습니다. 명령줄 인자는 항상 애노테이션 기본값보다 우선합니다.
