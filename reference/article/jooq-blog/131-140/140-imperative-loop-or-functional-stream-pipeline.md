# 명령형 루프 vs 함수형 스트림 파이프라인? 성능 영향에 주의하라!

> 원문: https://blog.jooq.org/imperative-loop-or-functional-stream-pipeline-beware-of-the-performance-impact/

저자: lukaseder
게시일: 2018년 10월 29일
최종 수정일: 2024년 3월 15일

---

저는 간결한 언어 구조를 선호합니다. 물론 코드 가독성과 성능 최적화 사이에는 트레이드오프가 있다는 것을 인정합니다. 특히 라이브러리 코드에서는 더욱 그렇습니다. 트위터에서 누군가와 대화하면서 흔히 발생하는 문제를 해결하는 세 가지 접근 방식을 보여드렸습니다: 조건이 충족되었을 때 중첩 루프에서 빠져나오는 것입니다.

## 세 가지 접근 방식 비교

접근 방식 1: 레이블이 있는 break 문
```java
woot:
if (something) {
  for (Object o : list)
    if (something(o))
      break woot;

  throw new E();
}
```

접근 방식 2: 메서드 추출과 return
```java
if (something && noneMatchSomething(list))
  throw new E();

private boolean noneMatchSomething(List<?> list) {
  for (Object o : list)
    if (something(o))
      return false;
  return true;
}
```

접근 방식 3: 함수형 스트림 파이프라인
```java
if (something && list.stream().noneMatch(this::something))
  throw new E();
```

## 성능 벤치마크

저는 세 가지 구현체(그리고 anyMatch와 allMatch를 사용하는 스트림 변형들)를 비교하는 JMH 벤치마크를 만들었습니다. 전체 벤치마크 코드는 다음과 같습니다:

```java
package org.jooq.test.benchmark;

import java.util.ArrayList;
import java.util.List;
import org.openjdk.jmh.annotations.*;

@Fork(value = 3, jvmArgsAppend = "-Djmh.stack.lines=3")
@Warmup(iterations = 5, time = 3)
@Measurement(iterations = 7, time = 3)
public class ImperativeVsStream {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        boolean something = true;

        @Param({ "2", "8" })
        int listSize;

        List<Integer> list = new ArrayList<>();

        boolean something() {
            return something;
        }

        boolean something(Integer o) {
            return o > 2;
        }

        @Setup(Level.Trial)
        public void setup() throws Exception {
            for (int i = 0; i < listSize; i++)
                list.add(i);
        }

        @TearDown(Level.Trial)
        public void teardown() throws Exception {
            list = null;
        }
    }

    @Benchmark
    public Object testImperativeWithBreak(BenchmarkState state) {
        woot:
        if (state.something()) {
            for (Integer o : state.list)
                if (state.something(o))
                    break woot;

            return 1;
        }
        return 0;
    }

    @Benchmark
    public Object testImperativeWithReturn(BenchmarkState state) {
        if (state.something() && woot(state))
            return 1;
        return 0;
    }

    private boolean woot(BenchmarkState state) {
        for (Integer o : state.list)
            if (state.something(o))
                return false;
        return true;
    }

    @Benchmark
    public Object testStreamNoneMatch(BenchmarkState state) {
        if (state.something() && state.list.stream().noneMatch(state::something))
            return 1;
        return 0;
    }

    @Benchmark
    public Object testStreamAnyMatch(BenchmarkState state) {
        if (state.something() && !state.list.stream().anyMatch(state::something))
            return 1;
        return 0;
    }

    @Benchmark
    public Object testStreamAllMatch(BenchmarkState state) {
        if (state.something() && state.list.stream().allMatch(s -> !state.something(s)))
            return 1;
        return 0;
    }
}
```

## 벤치마크 결과

```
Benchmark                                    (listSize)   Mode  Cnt         Score          Error  Units
ImperativeVsStream.testImperativeWithBreak            2  thrpt   14  86513288.062 ± 11950020.875  ops/s
ImperativeVsStream.testImperativeWithBreak            8  thrpt   14  74147172.906 ± 10089521.354  ops/s
ImperativeVsStream.testImperativeWithReturn           2  thrpt   14  97740974.281 ± 14593214.683  ops/s
ImperativeVsStream.testImperativeWithReturn           8  thrpt   14  81457864.875 ±  7376337.062  ops/s
ImperativeVsStream.testStreamAllMatch                 2  thrpt   14  14924513.929 ±  5446744.593  ops/s
ImperativeVsStream.testStreamAllMatch                 8  thrpt   14  12325486.891 ±  1365682.871  ops/s
ImperativeVsStream.testStreamAnyMatch                 2  thrpt   14  15729363.399 ±  2295020.470  ops/s
ImperativeVsStream.testStreamAnyMatch                 8  thrpt   14  13696297.091 ±   829121.255  ops/s
ImperativeVsStream.testStreamNoneMatch                2  thrpt   14  18991796.562 ±   147748.129  ops/s
ImperativeVsStream.testStreamNoneMatch                8  thrpt   14  15131005.381 ±   389830.420  ops/s
```

## 주요 발견

- break를 사용한 명령형: 리스트 크기에 따라 초당 약 8,600만~7,400만 연산
- return을 사용한 명령형: 초당 약 9,700만~8,100만 연산
- 스트림 변형들: 초당 약 1,500만~1,900만 연산

스트림 접근 방식은 명령형 접근 방식보다 약 6.5배 느립니다.

## 분석 및 권장 사항

### 성능이 덜 중요할 때

비즈니스 로직은 일반적으로 I/O 바운드이며, 특히 데이터베이스에 의존적입니다. 이러한 맥락에서 스트림으로 인한 약간의 CPU 오버헤드는 무시할 수 있습니다. 최적화는 가능하다면 로직을 데이터베이스 레이어로 옮기는 데 초점을 맞춰야 합니다.

### 성능이 중요할 때

라이브러리 코드, 인프라 컴포넌트, 그리고 CPU 바운드 연산에서는 신중한 고려가 필요합니다. 저는 이렇게 말합니다: 라이브러리 컨텍스트에서 "여러분의 로직 중 많은 부분이 CPU 바운드일 가능성이 높습니다."

### 전문가 의견 인용

> "Spring Data에서 우리는 모든 종류의 Stream(그리고 Optional)이 foreach 루프에 비해 상당한 오버헤드를 추가한다는 것을 일관되게 관찰했습니다. 그래서 우리는 핫 코드 경로에서는 이들을 엄격하게 피합니다." — Oliver Drotbohm

## 조기 최적화에 대한 중요한 맥락

저는 "조기 최적화의 카고 컬트(cargo cult)"를 인정합니다. 하지만 조기 최적화와 *언제* 최적화해야 하는지를 아는 것은 구별해야 합니다. 이러한 트레이드오프를 이해하는 것은 성능에 민감한 코드에서 정보에 기반한 결정을 내리는 데 필수적입니다.

## 결론

이 글은 스트림이 함수형의 우아함을 제공하지만, 명령형 루프가 성능에 민감한 컨텍스트에서는 여전히 우월하다는 것을 강조합니다. 개발자들은 애플리케이션을 프로파일링하여 병목 지점을 식별하고, 코드 컨텍스트에 기반하여 측정 가능한 성능 요구사항과 가독성의 균형을 맞추면서 적절한 기법을 사용해야 합니다.
