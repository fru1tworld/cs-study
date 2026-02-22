# Java JIT 컴파일러는 최적화를 정말 잘 한다

> 원문: https://blog.jooq.org/the-java-jit-compiler-is-darn-good-in-optimization/

게시일: 2016년 7월 19일

저자: Lukas Eder (게스트 기고: Tagir Valeev)

## 소개

이 글은 Tagir Valeev의 게스트 포스트입니다. Tagir는 StreamEx 라이브러리의 창시자이자 OpenJDK Stream API의 기여자입니다.

최근에 저(Lukas)는 JIT가 불필요한 반복을 최적화할 수 있는지에 대한 도전 과제를 제시했습니다. Tagir Valeev가 이 도전을 받아들였고, JIT가 "훨씬 더 잘할 수 있다"는 것을 보여주었습니다.

## 테스트 케이스

다음과 같이 문자열 길이의 합을 계산하는 메서드가 있다고 가정해 봅시다:

```java
static int testIterator(List<String> list) {
    int sum = 0;
    for (String s : list) {
        sum += s.length();
    }
    return sum;
}
```

이 코드는 실제로 명시적인 이터레이터를 사용한 다음 코드와 동일합니다:

```java
static int testIterator(List<String> list) {
    int sum = 0;
    Iterator<String> it = list.iterator();
    while (it.hasNext()) {
        String s = it.next();
        sum += s.length();
    }
    return sum;
}
```

## JIT 최적화의 작동 원리

HotSpot JVM은 두 개의 JIT 컴파일러를 사용하는 2단계 컴파일 전략을 사용합니다:

- C1 (클라이언트) 컴파일러: 초기 컴파일 단계로, 프로파일링을 수행합니다
- C2 (서버) 컴파일러: 자주 호출되는 메서드에 대해 수집된 통계를 기반으로 고급 최적화를 수행합니다

메서드가 처음 컴파일될 때, C1 컴파일러가 사용되고 일부 통계를 수집하기 위한 특수 명령어가 추가됩니다(이를 프로파일링이라고 합니다). 이 중에는 타입 통계도 포함됩니다. JVM은 `list` 변수가 정확히 어떤 타입을 가지는지 주의 깊게 확인합니다.

예를 들어, 다음과 같이 이 메서드를 100,000번 호출한다고 가정해 봅시다:

```java
for (int i = 0; i < 100_000; i++) {
    testIterator(Collections.singletonList("abc"));
}
```

이 경우, JVM은 100%의 경우가 `Collections.singletonList()`를 사용한다는 것을 발견합니다. 메서드가 핫해지면("뜨거워지면"), C2 컴파일러가 수집된 프로파일링 데이터를 사용하여 다시 컴파일합니다.

## 어셈블리 코드 분석

C2 컴파일러가 생성한 x64 어셈블리 출력을 살펴보면, 놀랍게도 코드가 매우 컴팩트합니다:

```
; 리스트가 Collections$SingletonList인지 확인
mov    r10,QWORD PTR [rsi+0x8]
movabs r11,0x7c0060828
cmp    r10,r11
jne    SLOW_PATH

; SingletonList.element 필드에 직접 접근
mov    r10,QWORD PTR [rsi+0x10]

; 요소가 String인지 확인
mov    r11,QWORD PTR [r10+0x8]
movabs r8,0x7c0062028
cmp    r11,r8
jne    EXCEPTIONAL_PATH

; String.value 배열에 접근하여 길이 반환
mov    r10,QWORD PTR [r10+0x14]
mov    eax,DWORD PTR [r10+0xc]
ret
```

핵심 통찰: 핫 경로에서는 이터레이터가 할당되지 않고, 루프도 없습니다. 단지 몇 개의 역참조와 두 개의 빠른 검사만 있을 뿐입니다.

## 의사 코드 표현

최적화된 로직은 다음과 같은 의사 코드로 표현할 수 있습니다:

```java
if (list.class != Collections$SingletonList) {
    goto SLOW_PATH;
}
str = ((Collections$SingletonList)list).element;
if (str.class != String) {
    goto EXCEPTIONAL_PATH;
}
return ((String)str).value.length;
```

JIT 컴파일러가 이러한 코드 부분들이 불필요하다는 것을 정적으로 증명하고 제거했습니다:
- 이터레이터 할당
- `hasNext()` 호출
- `next()` 호출
- 루프 자체

## 적응형 재컴파일

이것이 JIT 컴파일이 정적 컴파일과 다른 결정적인 장점입니다. 만약 미래에 싱글톤 리스트가 아닌 다른 것으로 이 메서드가 호출된다면, `SLOW_PATH`에서 이 상황을 처리합니다.

JIT가 `SLOW_PATH`가 너무 자주 실행된다는 것을 발견하면, 특수 처리를 제거하기 위해 메서드를 다시 컴파일합니다. 이것은 미리 컴파일된 코드와 달리 JIT가 실제 런타임 동작에 지속적으로 적응한다는 것을 의미합니다.

## 결론

Java JIT 컴파일러는 런타임 프로파일링 정보를 활용하여 놀라운 최적화를 수행할 수 있습니다. 이 예제에서 보았듯이:

1. 타입 프로파일링: JVM은 실제로 어떤 타입이 사용되는지 추적합니다
2. 추측적 최적화: 가장 일반적인 경우에 대해 고도로 최적화된 코드를 생성합니다
3. 탈출 분석과 스칼라 치환: 필요 없는 객체 할당을 제거합니다
4. 루프 제거: 불필요한 루프를 완전히 제거합니다
5. 적응형 재컴파일: 프로그램 동작이 변경되면 이에 맞게 적응합니다

이것이 바로 Java가 "느리다"는 오래된 편견과 달리, 현대 JVM에서 실행되는 Java 코드가 네이티브 코드에 필적하는(때로는 능가하는) 성능을 보일 수 있는 이유입니다.
