# jOOQ로 ASCII 차트 포맷팅하기
> 원문: https://blog.jooq.org/formatting-ascii-charts-with-jooq/

jOOQ에서 잘 알려지지 않은 기능 중 하나는 `Formattable.formatChart()` 기능입니다. 이 기능을 사용하면 jOOQ 결과를 ASCII 차트로 포맷팅할 수 있습니다. 콘솔 애플리케이션에서 결과를 빠르게 시각화하고 싶을 때 유용합니다.

## 기본 사용법

다음과 같은 테이블 형식의 결과가 있다고 가정해 봅시다:

```
+---+---+---+---+
|c  | v1| v2| v3|
+---+---+---+---+
|a  |  1|  2|  3|
|b  |  2|  1|  1|
|c  |  4|  0|  1|
+---+---+---+---+
```

`formatChart()` 메서드를 사용하면 이 데이터를 ASCII 막대 차트로 변환할 수 있습니다.

## 커스터마이징 옵션

### 기본 크기 설정

차트의 크기를 조절하려면 `ChartFormat` 클래스의 `dimensions()` 메서드를 사용합니다:

```java
result.formatChart(new ChartFormat().dimensions(6, 20));
```

### 여러 값 컬럼을 사용한 누적 차트

여러 값 컬럼을 포함하여 누적 시각화를 만들 수 있습니다:

```java
result.formatChart(new ChartFormat()
    .dimensions(40, 8)
    .values(1, 2, 3)
);
```

### 사용자 정의 문자

기본 ASCII 문자는 90년대 MS-DOS와 BBS 시절의 블록 문자(▒, ▓, █)를 사용합니다. 이러한 문자가 사용 중인 폰트와 잘 맞지 않는 경우, `.shades()` 메서드를 사용하여 다른 문자로 변경할 수 있습니다:

```java
result.formatChart(new ChartFormat()
    .shades('@', 'o', '.')
);
```

### 표시 모드

jOOQ는 여러 차트 유형을 지원합니다:
- 기본 누적 차트
- `Display.HUNDRED_PERCENT_STACKED`: 정규화된 백분율 분포를 표시

## 활용 사례

이 기능은 외부 그래프 라이브러리 없이 빠르게 콘솔 기반 데이터 시각화가 필요할 때 유용합니다. 디버깅, 리포팅, 그리고 Java 애플리케이션에서의 분석 작업에 실용적으로 활용할 수 있습니다.

스프레드시트 애플리케이션을 사용하는 대신, 개발자가 외부 도구 없이 빠르게 결과를 검사해야 할 때 유용한 대안이 됩니다.
