# JDK String.replace() vs Apache Commons StringUtils.replace() 벤치마킹

> 원문: https://blog.jooq.org/benchmarking-jdk-string-replace-vs-apache-commons-stringutils-replace/

최근 저는 Java Mission Control과 함께 제공되는 멋진 프로파일러인 JMC를 사용하여 jOOQ의 성능을 분석했습니다. 이를 통해 소스 코드의 다양한 위치에서 할당 압박(allocation pressure)을 발견했습니다. 그중 하나가 `String.replace()` 메서드로, jOOQ 내부에서 아포스트로피(작은따옴표)와 MySQL의 백슬래시 이스케이핑에 사용됩니다.

```java
static final String escape(Object val, Context<?> context) {
    String result = val.toString();

    if (needsBackslashEscaping(context.configuration()))
        result = result.replace("\\", "\\\\");

    return result.replace("'", "''");
}
```

분명히, 이 메서드는 모든 바인드 변수 값에 대해 호출됩니다 - 설정에 따라 내부적으로 인라인될 수 있으므로 - 그리고 저는 프로파일러에서 많은 `int[]` 할당이 여기서 발생하는 것을 볼 수 있었습니다. 왜 그럴까요? 음, Java 8에서 `String.replace(CharSequence, CharSequence)` 메서드는 내부적으로 정규식을 사용합니다.

```java
public String replace(CharSequence target, CharSequence replacement) {
    return Pattern.compile(target.toString(), Pattern.LITERAL).matcher(
            this).replaceAll(Matcher.quoteReplacement(replacement.toString()));
}
```

이것은 확실히 불필요합니다. 정규식이 전혀 필요하지 않습니다. 간단한 루프면 충분합니다.

## Apache Commons Lang의 StringUtils 사용하기

jOOQ는 이미 Apache Commons Lang의 몇 가지 기능에 의존하고 있으므로 `StringUtils.replace()`로 전환하는 것이 간단합니다.

```java
static final String escape(Object val, Context<?> context) {
    String result = val.toString();

    if (needsBackslashEscaping(context.configuration()))
        result = StringUtils.replace(result, "\\", "\\\\");

    return StringUtils.replace(result, "'", "''");
}
```

그들의 구현은 매우 직관적입니다. 성능 관점에서 좋은 점은 일치하는 것이 없으면 빠르게 실패한다는 것입니다.

```java
int end = searchText.indexOf(searchString, start);
if (end == INDEX_NOT_FOUND) {
    return text;
}
```

이렇게 하면 일치하는 것이 없을 때 `String.replace()`에 의해 생성되는 정규식 `Pattern`으로 인한 추가 할당 비용을 피할 수 있습니다.

## 벤치마킹

한 번 벤치마킹을 해봅시다! 저는 JMH 벤치마크를 작성했습니다.

```java
package org.jooq.test.benchmark;

import org.apache.commons.lang3.StringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Fork(value = 3, jvmArgsAppend = "-Djmh.stack.lines=3")
@Warmup(iterations = 5)
@Measurement(iterations = 7)
public class StringReplaceBenchmark {

    private static final String SHORT_STRING_NO_MATCH = "abc";
    private static final String SHORT_STRING_ONE_MATCH = "a'bc";
    private static final String SHORT_STRING_SEVERAL_MATCHES = "'a'b'c'";
    private static final String LONG_STRING_NO_MATCH =
        "abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc";
    private static final String LONG_STRING_ONE_MATCH =
        "abcabcabcabcabcabcabcabcabcabcabca'bcabcabcabcabcabcabcabcabcabcabcabcabc";
    private static final String LONG_STRING_SEVERAL_MATCHES =
        "abcabca'bcabcabcabcabcabc'abcabcabca'bcabcabcabcabcabca'bcabcabcabcabcabcabc";

    @Benchmark
    public void testStringReplaceShortStringNoMatch(Blackhole blackhole) {
        blackhole.consume(SHORT_STRING_NO_MATCH.replace("'", "''"));
    }

    @Benchmark
    public void testStringReplaceLongStringNoMatch(Blackhole blackhole) {
        blackhole.consume(LONG_STRING_NO_MATCH.replace("'", "''"));
    }

    @Benchmark
    public void testStringReplaceShortStringOneMatch(Blackhole blackhole) {
        blackhole.consume(SHORT_STRING_ONE_MATCH.replace("'", "''"));
    }

    @Benchmark
    public void testStringReplaceLongStringOneMatch(Blackhole blackhole) {
        blackhole.consume(LONG_STRING_ONE_MATCH.replace("'", "''"));
    }

    @Benchmark
    public void testStringReplaceShortStringSeveralMatches(Blackhole blackhole) {
        blackhole.consume(SHORT_STRING_SEVERAL_MATCHES.replace("'", "''"));
    }

    @Benchmark
    public void testStringReplaceLongStringSeveralMatches(Blackhole blackhole) {
        blackhole.consume(LONG_STRING_SEVERAL_MATCHES.replace("'", "''"));
    }

    @Benchmark
    public void testStringUtilsReplaceShortStringNoMatch(Blackhole blackhole) {
        blackhole.consume(StringUtils.replace(SHORT_STRING_NO_MATCH, "'", "''"));
    }

    @Benchmark
    public void testStringUtilsReplaceLongStringNoMatch(Blackhole blackhole) {
        blackhole.consume(StringUtils.replace(LONG_STRING_NO_MATCH, "'", "''"));
    }

    @Benchmark
    public void testStringUtilsReplaceShortStringOneMatch(Blackhole blackhole) {
        blackhole.consume(StringUtils.replace(SHORT_STRING_ONE_MATCH, "'", "''"));
    }

    @Benchmark
    public void testStringUtilsReplaceLongStringOneMatch(Blackhole blackhole) {
        blackhole.consume(StringUtils.replace(LONG_STRING_ONE_MATCH, "'", "''"));
    }

    @Benchmark
    public void testStringUtilsReplaceShortStringSeveralMatches(Blackhole blackhole) {
        blackhole.consume(StringUtils.replace(SHORT_STRING_SEVERAL_MATCHES, "'", "''"));
    }

    @Benchmark
    public void testStringUtilsReplaceLongStringSeveralMatches(Blackhole blackhole) {
        blackhole.consume(StringUtils.replace(LONG_STRING_SEVERAL_MATCHES, "'", "''"));
    }
}
```

## Java 8 결과

Java 8 결과는 다음과 같습니다.

| 테스트 시나리오 | 처리량 (ops/s) |
|---|---|
| testStringReplaceLongStringNoMatch | 4,809,343.940 |
| testStringUtilsReplaceLongStringNoMatch | 25,063,493.793 |
| testStringReplaceLongStringOneMatch | 1,406,989.855 |
| testStringUtilsReplaceLongStringOneMatch | 6,961,669.111 |
| testStringReplaceLongStringSeveralMatches | 1,103,323.491 |
| testStringUtilsReplaceLongStringSeveralMatches | 3,899,108.777 |
| testStringReplaceShortStringNoMatch | 5,936,992.874 |
| testStringUtilsReplaceShortStringNoMatch | 171,660,973.829 |
| testStringReplaceShortStringOneMatch | 3,267,435.957 |
| testStringUtilsReplaceShortStringOneMatch | 9,943,846.428 |
| testStringReplaceShortStringSeveralMatches | 2,313,713.015 |
| testStringUtilsReplaceShortStringSeveralMatches | 5,447,065.933 |

이 결과를 보면 Java 8에서 `StringUtils.replace()`가 모든 시나리오에서 `String.replace()`를 크게 앞서는 것을 알 수 있습니다. 특히 짧은 문자열에서 일치하는 것이 없는 경우가 가장 극적인데, `StringUtils`는 약 1억 7천만 ops/s를 달성하는 반면 JDK 메서드는 약 590만 ops/s에 불과합니다 - 약 29배 차이입니다.

긴 문자열에서 여러 개가 일치하는 경우에도 `StringUtils`는 약 390만 ops/s로 JDK의 약 110만 ops/s보다 약 3.5배 빠릅니다.

## Java 9 개선사항

Java 9에서 JDK 개발자들은 `String.replace()` 구현을 최적화했습니다. 새로운 구현은 Apache Commons와 유사한 최적화를 포함하여, 일치하는 것이 없으면 조기에 반환합니다.

```java
public String replace(CharSequence target, CharSequence replacement) {
    String tgtStr = target.toString();
    String replStr = replacement.toString();
    int j = indexOf(tgtStr);
    if (j < 0) {
        return this;
    }
    ...
}
```

핵심 최적화는 조기 종료 검사입니다: 대상 문자열이 발견되지 않으면 불필요한 객체를 할당하지 않고 원본 문자열을 즉시 반환합니다.

## Java 9 결과

Java 9 결과는 다음과 같습니다.

| 테스트 시나리오 | 처리량 (ops/s) |
|---|---|
| testStringReplaceLongStringNoMatch | 55,528,132.674 |
| testStringUtilsReplaceLongStringNoMatch | 55,767,541.806 |
| testStringReplaceLongStringOneMatch | 4,806,322.839 |
| testStringUtilsReplaceLongStringOneMatch | 8,366,539.616 |
| testStringReplaceLongStringSeveralMatches | 2,685,134.029 |
| testStringUtilsReplaceLongStringSeveralMatches | 3,923,819.576 |
| testStringReplaceShortStringNoMatch | 122,398,496.629 |
| testStringUtilsReplaceShortStringNoMatch | 121,139,633.453 |
| testStringReplaceShortStringOneMatch | 18,070,522.151 |
| testStringUtilsReplaceShortStringOneMatch | 11,367,395.622 |
| testStringReplaceShortStringSeveralMatches | 7,548,407.681 |
| testStringUtilsReplaceShortStringSeveralMatches | 5,045,065.948 |

Java 9에서는 성능 격차가 크게 줄어들었습니다. 일치하는 것이 없는 경우에는 두 구현 모두 비슷한 성능을 보입니다 (긴 문자열에서 약 5,500만 ops/s, 짧은 문자열에서 약 1억 2,200만 ops/s).

흥미롭게도 일치하는 것이 있는 시나리오에서는 트레이드오프가 존재합니다. 긴 문자열에서 일치하는 것이 있는 경우에는 여전히 `StringUtils`가 약 1.5~1.7배 빠르지만, 짧은 문자열에서 일치하는 것이 있는 경우에는 Java 9의 `String.replace()`가 실제로 `StringUtils`보다 빠릅니다.

## 결론

jOOQ와 같이 수백만 번의 문자열 연산을 처리하는 라이브러리의 경우, 이러한 최적화는 상당히 중요합니다. Java 8 호환성을 유지해야 하는 경우 Apache Commons Lang의 `StringUtils.replace()`를 사용하는 것이 권장됩니다. 할당 압박 제거는 특히 GC 동작 측면에서 상당한 이점을 제공합니다.

Java 9 이상을 사용하는 경우에는 성능 동등성이 달성되었으므로 네이티브 메서드를 안전하게 사용할 수 있습니다. 물론 일반적인 애플리케이션 코드에서는 이러한 마이크로 최적화를 우선시할 필요가 없지만, 문자열 조작이 많은 라이브러리에서는 이러한 최적화가 정당화됩니다.
