# Java 8 출시 1년 후, IDE와 컴파일러는 아직 완전히 준비되지 않았다

> 원문: https://blog.jooq.org/one-year-after-java-8s-release-ides-and-compilers-are-not-fully-ready-yet/

게시일: 2015년 3월 18일

작성자: lukaseder

---

정확히 1년 전 오늘, 2014년 3월 18일에 Java SE 8이 출시되었습니다. 1주년을 축하합니다! 그러나 아직 완전히 준비되지 않은 것이 있습니다.

## 컴파일러 버그

오늘 Stack Overflow에서 매우 흥미로운 질문을 보았습니다. 다음 코드를 살펴보세요:

```java
class TestException extends Exception {
}

interface Task<E extends Exception> {
    void call() throws E;
}

public class TaskPerformer {

    private <E extends Exception> void perform(Task<E> task) throws E {
        task.call();
    }

    public static void main(String[] args) {
        // 컴파일 오류
        new TaskPerformer().perform(() -> {
            try {
                throw new TestException();
            } catch (TestException e) {
                return;
            }
        });
    }
}
```

위의 코드는 문제가 없습니다. `TestException`은 람다 내에서 완전히 처리되며, `Task<E>` 인터페이스의 `E` 타입은 확인되지 않은(unchecked) 예외 타입으로 추론될 수 있습니다. 예를 들어 `RuntimeException`처럼요.

그러나 실행하면 다음과 같은 컴파일 오류가 발생합니다:

```
java: unreported exception TestException; must be caught or declared to be thrown
```

이것은 오탐지(false positive) 컴파일 오류입니다. 위의 버그는 다음의 매우 미묘한 조합에 의해 발생했습니다:

- 확인된(checked) 예외 vs 확인되지 않은(unchecked) 예외
- 제네릭(그리고 예외)
- 람다 표현식
- 타입 추론
- 흐름 분석

이것은 분명히 해결하기 어려운 문제이며, 버그 429430에서 논의된 것처럼 최근에야 컴파일러에서 수정된 것 같습니다.

## javac의 상태

javac의 최신 릴리스에서는 이 문제가 해결되었습니다. 특히 빌드 1.8.0_40-ea-b23에서요. 하지만 물론, 프로덕션 환경에서 사전 체험판(early access) 릴리스를 사용하고 싶지는 않을 것입니다.

## Eclipse의 상태

Eclipse는 자체적인 매우 정교한 증분 Java 컴파일러를 탑재하고 있습니다. 이 컴파일러는 정말 훌륭한 소프트웨어입니다. 변경을 저장할 때마다 거의 즉시 증분 컴파일 결과를 얻을 수 있다는 것은 정말 대단합니다. 또한 문장의 일부가 의미적으로 잘못되어도 중간 문장의 자동 완성을 얻을 수 있다는 것도요.

단점은 이 Eclipse 컴파일러가 다른 컴파일러들과 약간 다르게 컴파일한다는 것입니다. 그리고 jOOQ와 jOOλ 개발을 통해 우리는 꽤 많은 버그를 발견하고 Eclipse에 보고했습니다:

- [Bug 426676 – 람다 표현식에서 잘못된 제네릭 메서드 타입 추론](https://bugs.eclipse.org/bugs/show_bug.cgi?id=426676)
- [Bug 429430 – 람다와 더블 콜론 연산자에서 F3이 작동하지 않음](https://bugs.eclipse.org/bugs/show_bug.cgi?id=429430)
- [Bug 434297 – 보이지 않는 메서드 인수에 람다가 불법적으로 전달됨](https://bugs.eclipse.org/bugs/show_bug.cgi?id=434297)
- [Bug 439234 – 람다 표현식에서 잘못된 제네릭 메서드 타입 추론](https://bugs.eclipse.org/bugs/show_bug.cgi?id=439234)
- [Bug 439707 – 람다의 보이지 않는 메서드에서 오해를 일으키는 오류 "expression must be a functional interface"](https://bugs.eclipse.org/bugs/show_bug.cgi?id=439707)
- [Bug 458321 – 람다 자동 완성 시 세미콜론이 과도하게 삽입됨](https://bugs.eclipse.org/bugs/show_bug.cgi?id=458321)
- [Bug 460511 – 다이아몬드 연산자가 새 생성자 생성 제안을 방해함](https://bugs.eclipse.org/bugs/show_bug.cgi?id=460511)
- [Bug 460515 – 람다에서 보이지 않는 멤버 접근으로 인한 오해를 일으키는 오류](https://bugs.eclipse.org/bugs/show_bug.cgi?id=460515)
- [Bug 460517 – static 호출, 박싱, 단항 연산자를 결합할 때 컴파일 오류](https://bugs.eclipse.org/bugs/show_bug.cgi?id=460517)

## IntelliJ의 상태

IntelliJ는 백그라운드에서 javac를 사용하므로 실제로 동일한 컴파일 결과를 얻게 됩니다. 그러나 IntelliJ의 자체 구문 강조 및 코드 분석은 때때로 실제로는 없는 오류를 표시할 수 있습니다.

## NetBeans의 상태

NetBeans 또한 javac를 사용합니다. NetBeans 팀은 최근 javac 수정 사항을 통합했다고 알려져 있습니다.

## 버그를 보고하세요!

버그를 발견하면 보고해 주세요. 우리 모두는 아마도 다음 세 가지 IDE 중 하나를 사용하고 있을 것입니다:

- [Eclipse 버그 추적기](https://bugs.eclipse.org/bugs)
- [IntelliJ 버그 추적기](https://youtrack.jetbrains.com)
- [NetBeans 버그 추적기](https://netbeans.org/bugzilla)

컴파일러와 IDE가 더 빨리 안정화될수록, 우리 모두가 더 빨리 Java 8의 모든 이점을 누릴 수 있습니다.
