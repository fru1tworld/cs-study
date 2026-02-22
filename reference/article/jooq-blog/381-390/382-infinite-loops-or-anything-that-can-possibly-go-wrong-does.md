# 무한 루프. 또는: 잘못될 수 있는 모든 것은 잘못된다.

> 원문: https://blog.jooq.org/infinite-loops-or-anything-that-can-possibly-go-wrong-does/

게시일: 2015년 1월 16일, lukaseder

---

> "잘못될 수 있는 모든 것은 잘못된다" — 머피

> "좋은 프로그래머는 일방통행 도로를 건널 때도 양쪽을 살피는 사람이다." — Doug Linder

## 문제 제기

완벽한 조건에서 프로그래머들은 종종 무한 루프를 작성하면서, 결국에는 종료될 것이라고 믿습니다. 다양한 언어에서 사용되는 일반적인 패턴들이 있습니다:

Java:
```java
for (;;) {
    // 무언가 수행
}
```

C:
```c
while (1) {
    // 무언가 수행
}
```

BASIC:
```basic
10 something
20 GOTO 10
```

GitHub 검색 결과를 보면 코드베이스 전반에 걸쳐 `while(true)` 패턴이 광범위하게 사용되고 있음을 알 수 있습니다.

## 잠재적으로 무한한 루프를 절대 사용하지 마라

이 글에서는 앨런 튜링의 정지 문제(halting problem)에 대해 논의합니다. 인간은 단순한 프로그램이 종료되는지 여부를 평가할 수 있지만, 복잡한 알고리즘에 대해 이를 결정하는 것은 결정 불가능한(undecidable) 문제로 남아 있습니다. 다음은 제공된 예시입니다:

절대 멈추지 않는 루프:
```java
for (;;) continue;
```

항상 멈추는 루프:
```java
for (;;) break;
```

## 실제 사례 연구: jOOQ 이슈 #3696

jOOQ 프로젝트는 SQL Server의 JDBC 드라이버와 관련된 심각한 버그를 만났습니다. 이 드라이버는 `SQLException` 체인을 올바르게 보고하지 못했습니다. 트리거가 여러 오류를 발생시킬 때, 원래 코드는 모든 예외를 소비하려고 시도했습니다:

```java
consumeLoop: for (;;)
    try {
        if (!stmt.getMoreResults() &&
             stmt.getUpdateCount() == -1)
            break consumeLoop;
    }
    catch (SQLException e) {
        previous.setNextException(e);
        previous = e;
    }
```

SQL Azure와 함께 사용할 때, 이 패턴은 동일한 오류가 반복적으로 보고되는 버그로 인해 무한 루프를 생성했습니다. 이로 인해 `OutOfMemoryError`가 발생하고 거대한 `SQLException` 체인이 만들어졌으며, 잠재적으로 모든 사용자에 대해 서버가 차단될 수 있었습니다.

## 해결책

수정 사항은 최대 반복 제한을 구현합니다:

```java
consumeLoop: for (int i = 0; i < 256; i++)
    try {
        if (!stmt.getMoreResults() &&
             stmt.getUpdateCount() == -1)
            break consumeLoop;
    }
    catch (SQLException e) {
        previous.setNextException(e);
        previous = e;
    }
```

저자는 유머러스하게 "640KB면 누구에게나 충분할 것이다"라는 유명한 인용구를 참조합니다.

## JPL 코딩 표준

제트추진연구소(Jet Propulsion Laboratory)는 C 코딩 표준에서 "규칙 3 (루프 경계)"을 확립했습니다. 이 규칙은 모든 루프가 정적으로 결정 가능한 상한선을 가져야 한다고 요구합니다. 이 표준은 하나의 예외를 허용합니다: "요청을 수신하고 처리하는 태스크 또는 스레드당 하나의 비종료 루프. 이러한 서버 루프는 C 주석 `/* @non-terminating@ */`으로 주석 처리되어야 한다."

## 결론

저자는 `while (true)`, `for (;;)`, 그리고 `do {} while (true);` 문을 포함한 무한 루프 패턴에 대해 코드베이스를 검토할 것을 권장합니다. 대부분의 프로그래머들은 그러한 루프가 절대 문제를 일으키지 않을 것이라고 믿지만, 경험은 그렇지 않음을 증명합니다. 이 글은 과신이 종종 실패에 선행한다는 것을 암시하는 XKCD 만화 #292를 참조하며 마무리됩니다.

---

## 댓글 섹션

세 가지 주목할 만한 댓글이 있습니다:

1. Peter Verhas: "모든 루프는 멈춘다. 무한 루프조차도 잠시 후에는 멈춘다."라고 농담합니다.

2. vladmihalcea: 서버를 다운시킨 정규 표현식과 관련된 유사한 치명적인 실패를 참조합니다.

3. G: 일방통행 도로에서 후진하는 트럭에 거의 치일 뻔한 일화를 공유하며, 방어적 프로그래밍의 중요성을 입증합니다.

---

주요 참고 자료:
- 정지 문제: https://en.wikipedia.org/wiki/Halting_problem
- JPL 코딩 표준: http://lars-lab.jpl.nasa.gov/JPL_Coding_Standard_C.pdf
- GitHub while(true) 검색 결과
- XKCD 만화 #292
