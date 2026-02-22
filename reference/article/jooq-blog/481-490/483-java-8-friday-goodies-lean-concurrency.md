# Java 8 금요일 선물: 가벼운 동시성

> 원문: https://blog.jooq.org/java-8-friday-goodies-lean-concurrency/

Data Geekery에서는 Java 8이 우리의 주요 제품인 jOOQ에 어떤 영향을 미칠 수 있는지 매우 기대하고 있습니다. 그래서 "Java 8 금요일"이라는 블로그 시리즈를 시작합니다. 매주 금요일마다 람다 표현식, 확장 메서드, 그리고 다른 훌륭한 기능들을 활용하는 멋진 새로운 Java 8 튜토리얼 스타일의 기능들을 보여드리겠습니다. 소스 코드는 GitHub에서 찾으실 수 있습니다.

## 가벼운 동시성

오늘 이 멋진 인용구를 트위터에서 봤습니다:

> 주니어 프로그래머는 동시성이 어렵다고 생각합니다.
> 경험 많은 프로그래머는 동시성이 쉽다고 생각합니다.
> 시니어 프로그래머는 동시성이 어렵다고 생각합니다.

"농담은 반만 진담이다"라는 격언이 있습니다. 하지만 Java 8이 적어도 표면적으로는 동시성 프로그래밍을 더 쉽게 만들 것이라는 희망을 품어봅시다. 이를 위해 람다와 개선된 API에 감사해야 합니다.

## JDK 1.0 API를 개선하는 Java 8

`java.lang.Thread`가 JDK 1.0 이후로 존재했다는 것을 믿을 수 있나요? `java.lang.Runnable`도 마찬가지입니다. `Runnable`은 이제 새로운 `@FunctionalInterface` 어노테이션으로 주석이 달려 있으며, 이는 우리가 `Thread` 객체에 람다를 전달할 수 있음을 의미합니다. 다음과 같은 연산이 있다고 가정해봅시다:

```java
public static int longOperation() {
    System.out.println("Running on thread #"
       + Thread.currentThread().getId());

    // ... 무언가를 한다
    return 42;
}
```

이제 두 개의 쓰레드에서 이 연산을 두 번 호출하고, 두 쓰레드가 완료될 때까지 기다리는 예제를 작성할 수 있습니다:

```java
Thread[] threads = {

    // 먼저 람다 표현식으로 쓰레드 생성
    new Thread(() -> {
        longOperation();
    }),

    // 또는 메서드 참조로
    new Thread(ThreadGoodies::longOperation)
};

// 위 배열의 모든 쓰레드 시작
Arrays.stream(threads).forEach(Thread::start);

// 모든 쓰레드에 조인
Arrays.stream(threads).forEach(t -> {
    try {
        t.join();
    }
    catch (InterruptedException ignore) {}
});
```

보시다시피, 람다 표현식은 체크 예외를 깔끔하게 처리할 방법을 찾지 못했습니다. `java.util.function` 패키지에 새로 추가된 함수형 인터페이스 중 체크 예외를 던지는 것을 허용하는 것은 하나도 없습니다. 이전 포스트에서 우리는 JDK의 각 함수형 인터페이스를 체크 예외 던지기를 허용하는 동등한 함수형 인터페이스로 감싸는 jOOλ(또는 jOOL, jOO-Lambda)를 공개했습니다.

jOOλ를 사용하면 위의 코드를 다음과 같이 작성할 수 있습니다:

```java
// 모든 쓰레드에 조인
Arrays.stream(threads).forEach(Unchecked.consumer(
    t -> t.join()
));
```

## Java 5 API를 개선하는 Java 8

Java 5는 이 블로그에서 자세히 다룬 새로운 동시성 API를 도입했습니다. `ExecutorService`가 도입되어 쓰레드 관리에 대한 전체적인 사고 방식이 바뀌었습니다. 더 이상 쓰레드의 생성과 조인에 대해 걱정할 필요가 없었습니다. 쓰레드 풀에 작업을 제출하면 풀이 모든 것을 관리했습니다. 여기 예제가 있습니다:

```java
ExecutorService service = Executors
    .newFixedThreadPool(5);

Future[] answers = {
    service.submit(() -> longOperation()),
    service.submit(ThreadGoodies::longOperation)
};

Arrays.stream(answers).forEach(Unchecked.consumer(
    f -> System.out.println(f.get())
));
```

다시 한번 `UncheckedConsumer`를 사용했습니다. 이번에는 `Future.get()` 호출에서 던지는 체크 예외를 `RuntimeException`으로 감싸기 위해서입니다.

## Java 8의 병렬 처리와 ForkJoinPool

아마도 새로운 Streams API가 동시성 및 병렬 처리에 가장 큰 영향을 미친다는 것에 동의하실 것입니다. Java 7에서 도입된 ForkJoin API를 모두가 환영한 것은 아니었고, 그것이 도입한 복잡성도 마찬가지였습니다. 하지만 Java 8에서 Streams와 함께 매우 쉽게 사용할 수 있게 되었습니다. 다음을 살펴보세요:

```java
Arrays.stream(new int[]{ 1, 2, 3, 4, 5, 6 })
      .parallel()
      .max()
      .ifPresent(System.out::println);
```

`parallel()`을 호출하는 것만으로 `IntStream.max()` 리듀스 연산이 JDK의 내부 `ForkJoinPool`의 사용 가능한 모든 쓰레드에서 실행됩니다. 관련된 `ForkJoinTask`에 대해 걱정할 필요가 없습니다. 이것은 정말 유용할 수 있습니다.

위의 예제에서 `Optional` 타입과 `OptionalInt.ifPresent()` 메서드도 보셨을 것입니다. 이 메서드는 함수형 인터페이스를 인자로 받습니다. 다가오는 블로그 포스트에서 우리는 `Optional` 타입이 Streams와 어떻게 연관되는지 살펴볼 것입니다.

더 많은 Java 8 멋진 기능들을 기대해주세요!
