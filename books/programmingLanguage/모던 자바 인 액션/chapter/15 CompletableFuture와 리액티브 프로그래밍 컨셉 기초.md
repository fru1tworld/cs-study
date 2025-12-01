# 15 CompletableFuture와 리액티브 프로그래밍 컨셉 기초

## 동시성과 병렬성

### 용어 정리

- ** 동시성(Concurrency)**: 단일 코어에서 여러 작업을 번갈아 실행
- ** 병렬성(Parallelism)**: 멀티 코어에서 여러 작업을 동시에 실행

### 자바의 동시성 지원 발전

- **Java 5**: java.util.concurrent, ExecutorService, Callable, Future
- **Java 7**: Fork/Join 프레임워크
- **Java 8**: 스트림과 CompletableFuture
- **Java 9**: 리액티브 프로그래밍 (Flow API)

## Future의 한계

### 기본 Future 인터페이스

```java
ExecutorService executor = Executors.newCachedThreadPool();
Future<Double> future = executor.submit(new Callable<Double>() {
    public Double call() {
        return doSomeLongComputation();
    }
});

doSomethingElse();

try {
    Double result = future.get(1, TimeUnit.SECONDS);
} catch (ExecutionException ee) {
    // 계산 중 예외 발생
} catch (InterruptedException ie) {
    // 현재 스레드에서 대기 중 인터럽트 발생
} catch (TimeoutException te) {
    // Future가 완료되기 전에 타임아웃 발생
}
```

### Future의 제약

- 블로킹 호출: get() 메서드는 결과가 준비될 때까지 블로킹
- 조합 불가: 여러 Future를 조합하기 어려움
- 예외 처리 부족: 예외 처리 메커니즘이 약함
- 완료 통지 불가: Future가 완료되었을 때 알림을 받을 수 없음

## CompletableFuture로 비동기 애플리케이션 만들기

### CompletableFuture 기본

```java
public Future<Double> getPriceAsync(String product) {
    CompletableFuture<Double> futurePrice = new CompletableFuture<>();
    new Thread(() -> {
        double price = calculatePrice(product);
        futurePrice.complete(price);
    }).start();
    return futurePrice;
}
```

### 팩토리 메서드 supplyAsync

```java
public Future<Double> getPriceAsync(String product) {
    return CompletableFuture.supplyAsync(() -> calculatePrice(product));
}
```

## 비블로킹 코드 만들기

### 순차 실행 vs 병렬 실행

```java
// 순차 실행 (느림)
public List<String> findPrices(String product) {
    return shops.stream()
        .map(shop -> String.format("%s price is %.2f",
            shop.getName(), shop.getPrice(product)))
        .collect(toList());
}

// 병렬 실행 (빠름)
public List<String> findPricesParallel(String product) {
    return shops.parallelStream()
        .map(shop -> String.format("%s price is %.2f",
            shop.getName(), shop.getPrice(product)))
        .collect(toList());
}

// CompletableFuture 사용 (가장 유연)
public List<String> findPricesAsync(String product) {
    List<CompletableFuture<String>> priceFutures =
        shops.stream()
        .map(shop -> CompletableFuture.supplyAsync(
            () -> String.format("%s price is %.2f",
                shop.getName(), shop.getPrice(product))))
        .collect(toList());

    return priceFutures.stream()
        .map(CompletableFuture::join)
        .collect(toList());
}
```

## 커스텀 Executor 사용

### Executor 설정

```java
private final Executor executor =
    Executors.newFixedThreadPool(Math.min(shops.size(), 100),
        new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);  // 데몬 스레드로 설정
                return t;
            }
        });
```

### Executor를 CompletableFuture에 전달

```java
CompletableFuture.supplyAsync(() -> shop.getPrice(product), executor)
```

### 스레드 풀 크기 결정

```
Nthreads = NCPU * UCPU * (1 + W/C)

NCPU = Runtime.getRuntime().availableProcessors()
UCPU = CPU 활용 비율 (0~1)
W/C = 대기시간 / 계산시간 비율
```

## 비동기 작업 파이프라인 만들기

### thenApply: 변환

```java
public List<String> findPrices(String product) {
    List<CompletableFuture<String>> priceFutures =
        shops.stream()
        .map(shop -> CompletableFuture.supplyAsync(
            () -> shop.getPrice(product), executor))
        .map(future -> future.thenApply(Quote::parse))
        .map(future -> future.thenCompose(quote ->
            CompletableFuture.supplyAsync(
                () -> Discount.applyDiscount(quote), executor)))
        .collect(toList());

    return priceFutures.stream()
        .map(CompletableFuture::join)
        .collect(toList());
}
```

### thenCompose: 비동기 연산 연결

```java
CompletableFuture<Double> futurePriceInUSD =
    CompletableFuture.supplyAsync(() -> shop.getPrice(product))
    .thenCompose(price ->
        CompletableFuture.supplyAsync(() -> exchangeService.getRate(Money.EUR, Money.USD))
        .thenApply(rate -> price * rate));
```

### thenCombine: 두 비동기 연산 결합

```java
Future<Double> futurePriceInUSD =
    CompletableFuture.supplyAsync(() -> shop.getPrice(product))
    .thenCombine(
        CompletableFuture.supplyAsync(
            () -> exchangeService.getRate(Money.EUR, Money.USD)),
        (price, rate) -> price * rate);
```

## 여러 CompletableFuture 조합

### allOf: 모든 작업 완료 대기

```java
CompletableFuture[] futures = findPricesStream("myPhone")
    .map(f -> f.thenAccept(System.out::println))
    .toArray(size -> new CompletableFuture[size]);

CompletableFuture.allOf(futures).join();
```

### anyOf: 가장 빨리 완료된 작업

```java
CompletableFuture[] futures = findPricesStream("myPhone")
    .map(f -> f.thenAccept(
        s -> System.out.println(s + " (done in " +
            ((System.nanoTime() - start) / 1_000_000) + " msecs)")))
    .toArray(size -> new CompletableFuture[size]);

CompletableFuture.anyOf(futures).join();
```

## 예외 처리

### exceptionally: 예외 복구

```java
CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException("Error!");
})
.exceptionally(ex -> {
    System.out.println("Error: " + ex.getMessage());
    return 0.0;
});
```

### handle: 예외와 정상 결과 처리

```java
CompletableFuture.supplyAsync(() -> 10 / 0)
    .handle((result, exception) -> {
        if (exception != null) {
            System.out.println("Error: " + exception.getMessage());
            return 0;
        }
        return result;
    });
```

### whenComplete: 최종 처리

```java
CompletableFuture.supplyAsync(() -> doSomething())
    .whenComplete((result, exception) -> {
        if (exception == null) {
            System.out.println("Success: " + result);
        } else {
            System.out.println("Error: " + exception);
        }
    });
```

## 타임아웃 관리 (Java 9+)

### orTimeout: 타임아웃 설정

```java
CompletableFuture.supplyAsync(() -> doSomethingLong())
    .orTimeout(1, TimeUnit.SECONDS)
    .exceptionally(ex -> "Timeout!");
```

### completeOnTimeout: 타임아웃 시 기본값

```java
CompletableFuture.supplyAsync(() -> doSomethingLong())
    .completeOnTimeout("Default", 1, TimeUnit.SECONDS);
```

## CompletableFuture의 종료 방법

### complete: 강제 완료

```java
CompletableFuture<Integer> future = new CompletableFuture<>();
future.complete(42);  // 강제로 완료
```

### completeExceptionally: 예외로 완료

```java
CompletableFuture<Integer> future = new CompletableFuture<>();
future.completeExceptionally(new RuntimeException("Error!"));
```

## 모범 사례

### 1. 적절한 Executor 사용

- I/O 바운드 작업: 큰 스레드 풀
- CPU 바운드 작업: Runtime.getRuntime().availableProcessors()

### 2. 블로킹 작업 분리

```java
CompletableFuture.supplyAsync(() -> blockingOperation(), blockingExecutor)
    .thenApplyAsync(result -> cpuIntensiveOperation(), computeExecutor);
```

### 3. 예외 처리 명확히

```java
future
    .exceptionally(ex -> handleError(ex))
    .thenAccept(result -> processResult(result));
```

### 4. 메모리 누수 주의

- 완료되지 않은 CompletableFuture는 메모리에 계속 남음
- 타임아웃이나 취소 메커니즘 구현 필요

## 정리

1. **Future의 한계를 CompletableFuture가 해결한다**
2. ** 비블로킹 비동기 프로그래밍이 가능하다**
3. ** 파이프라인 방식으로 비동기 작업을 조합할 수 있다**
4. ** 적절한 Executor 설정이 성능에 중요하다**
5. ** 예외 처리와 타임아웃 관리를 신경써야 한다**
6. ** 리액티브 프로그래밍의 기초가 된다**
