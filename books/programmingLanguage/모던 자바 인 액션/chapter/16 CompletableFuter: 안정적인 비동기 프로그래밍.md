# 16 CompletableFuture: 안정적인 비동기 프로그래밍

## 비동기 API 구현

### 동기 메서드를 비동기 메서드로 변환

**동기 버전:**

```java
public class Shop {
    public double getPrice(String product) {
        return calculatePrice(product);
    }

    private double calculatePrice(String product) {
        delay();  // 1초 지연 시뮬레이션
        return random.nextDouble() * product.charAt(0) + product.charAt(1);
    }

    public static void delay() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
```

**비동기 버전:**

```java
public Future<Double> getPriceAsync(String product) {
    return CompletableFuture.supplyAsync(() -> calculatePrice(product));
}
```

## 비동기 작업 에러 처리

### 명시적 예외 처리

```java
public Future<Double> getPriceAsync(String product) {
    CompletableFuture<Double> futurePrice = new CompletableFuture<>();
    new Thread(() -> {
        try {
            double price = calculatePrice(product);
            futurePrice.complete(price);
        } catch (Exception ex) {
            futurePrice.completeExceptionally(ex);
        }
    }).start();
    return futurePrice;
}
```

### supplyAsync를 사용한 자동 예외 처리

```java
public Future<Double> getPriceAsync(String product) {
    return CompletableFuture.supplyAsync(() -> calculatePrice(product))
        .exceptionally(ex -> {
            System.out.println("Error: " + ex.getMessage());
            return 0.0;
        });
}
```

## 병렬 스트림 vs CompletableFuture

### 병렬 스트림

```java
public List<String> findPricesParallel(String product) {
    return shops.parallelStream()
        .map(shop -> String.format("%s price is %.2f",
            shop.getName(), shop.getPrice(product)))
        .collect(toList());
}
```

**장점:**

- 간결한 코드
- 내부적으로 공통 ForkJoinPool 사용

**단점:**

- 블로킹 I/O에 비효율적
- 스레드 풀 크기 조정 어려움

### CompletableFuture

```java
public List<String> findPricesAsync(String product) {
    Executor executor = Executors.newFixedThreadPool(
        Math.min(shops.size(), 100),
        r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

    List<CompletableFuture<String>> priceFutures = shops.stream()
        .map(shop -> CompletableFuture.supplyAsync(
            () -> shop.getName() + " price is " + shop.getPrice(product),
            executor))
        .collect(toList());

    return priceFutures.stream()
        .map(CompletableFuture::join)
        .collect(toList());
}
```

**장점:**

- 커스텀 Executor 사용 가능
- I/O 바운드 작업에 최적
- 더 세밀한 제어 가능

## 비동기 작업 조합

### thenCompose: 두 비동기 작업 순차 실행

```java
public List<String> findPricesWithDiscount(String product) {
    List<CompletableFuture<String>> priceFutures = shops.stream()
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

### thenCombine: 두 독립적 비동기 작업 결합

```java
Future<Double> futurePriceInUSD =
    CompletableFuture.supplyAsync(() -> shop.getPrice(product))
    .thenCombine(
        CompletableFuture.supplyAsync(
            () -> exchangeService.getRate(Money.EUR, Money.USD)),
        (price, rate) -> price * rate);
```

### thenCombineAsync: 다른 Executor에서 실행

```java
CompletableFuture.supplyAsync(() -> shop.getPrice(product), executor1)
    .thenCombineAsync(
        CompletableFuture.supplyAsync(
            () -> exchangeService.getRate(Money.EUR, Money.USD), executor2),
        (price, rate) -> price * rate,
        executor3);
```

## 독립적인 CompletableFuture와 비독립적인 CompletableFuture 조합

### 독립적인 작업

```java
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Task 1");
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "Task 2");

CompletableFuture<String> combinedFuture = future1.thenCombine(
    future2,
    (result1, result2) -> result1 + " & " + result2);
```

### 비독립적인 작업 (의존 관계)

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> "Get user data")
    .thenCompose(userData -> CompletableFuture.supplyAsync(() ->
        "Process: " + userData));
```

## 타임아웃 효과적으로 사용하기

### completeOnTimeout (Java 9+)

```java
CompletableFuture<Double> future = CompletableFuture
    .supplyAsync(() -> getPrice(product))
    .completeOnTimeout(0.0, 1, TimeUnit.SECONDS);
```

### orTimeout (Java 9+)

```java
CompletableFuture<Double> future = CompletableFuture
    .supplyAsync(() -> getPrice(product))
    .orTimeout(1, TimeUnit.SECONDS)
    .exceptionally(ex -> {
        System.out.println("Timeout occurred!");
        return 0.0;
    });
```

### 커스텀 타임아웃 구현 (Java 8)

```java
private static final ScheduledExecutorService scheduler =
    Executors.newScheduledThreadPool(1);

public static <T> CompletableFuture<T> within(CompletableFuture<T> future, long timeout, TimeUnit unit) {
    final CompletableFuture<T> timeoutFuture = new CompletableFuture<>();

    scheduler.schedule(() -> {
        timeoutFuture.completeExceptionally(new TimeoutException());
    }, timeout, unit);

    return future.applyToEither(timeoutFuture, Function.identity());
}
```

## CompletableFuture의 단계별 실행

### thenApply, thenAccept, thenRun 비교

```java
// thenApply: 결과 변환 후 반환
CompletableFuture<Integer> future1 = CompletableFuture
    .supplyAsync(() -> 10)
    .thenApply(x -> x * 2);  // 반환값 있음

// thenAccept: 결과 소비, 반환값 없음
CompletableFuture<Void> future2 = CompletableFuture
    .supplyAsync(() -> 10)
    .thenAccept(x -> System.out.println(x));  // 반환값 없음

// thenRun: 결과 무시, 단순 실행
CompletableFuture<Void> future3 = CompletableFuture
    .supplyAsync(() -> 10)
    .thenRun(() -> System.out.println("Done"));  // 이전 결과 사용 안 함
```

### Async 버전의 차이

```java
// 같은 스레드에서 실행
future.thenApply(x -> x * 2);

// 다른 스레드에서 실행
future.thenApplyAsync(x -> x * 2);

// 지정된 Executor에서 실행
future.thenApplyAsync(x -> x * 2, customExecutor);
```

## 여러 CompletableFuture 조합하기

### allOf: 모두 완료 대기

```java
CompletableFuture<Void> allFutures = CompletableFuture.allOf(
    CompletableFuture.supplyAsync(() -> "Task 1"),
    CompletableFuture.supplyAsync(() -> "Task 2"),
    CompletableFuture.supplyAsync(() -> "Task 3")
);

allFutures.thenRun(() -> System.out.println("All tasks completed"));
```

### 결과 수집

```java
CompletableFuture<String>[] futures = shops.stream()
    .map(shop -> CompletableFuture.supplyAsync(
        () -> shop.getPrice(product), executor))
    .toArray(CompletableFuture[]::new);

CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);

CompletableFuture<List<String>> allResults = allFutures
    .thenApply(v -> Stream.of(futures)
        .map(CompletableFuture::join)
        .collect(toList()));
```

### anyOf: 가장 빨리 완료된 작업

```java
CompletableFuture<Object> firstCompleted = CompletableFuture.anyOf(
    CompletableFuture.supplyAsync(() -> {
        delay(1000);
        return "Task 1";
    }),
    CompletableFuture.supplyAsync(() -> {
        delay(500);
        return "Task 2";  // 이것이 먼저 완료됨
    })
);

firstCompleted.thenAccept(result ->
    System.out.println("First completed: " + result));
```

## 응답성이 좋은 애플리케이션 만들기

### 가격 정보를 즉시 스트림으로 반환

```java
public Stream<CompletableFuture<String>> findPricesStream(String product) {
    return shops.stream()
        .map(shop -> CompletableFuture.supplyAsync(
            () -> shop.getPrice(product), executor))
        .map(future -> future.thenApply(Quote::parse))
        .map(future -> future.thenCompose(quote ->
            CompletableFuture.supplyAsync(
                () -> Discount.applyDiscount(quote), executor)));
}

// 사용
long start = System.nanoTime();
CompletableFuture[] futures = findPricesStream("myPhone27S")
    .map(f -> f.thenAccept(s -> System.out.println(
        s + " (done in " + ((System.nanoTime() - start) / 1_000_000) + " msecs)")))
    .toArray(CompletableFuture[]::new);

CompletableFuture.allOf(futures).join();
```

## 실전 예제: 할인 서비스 구현

### Quote 클래스

```java
public class Quote {
    private final String shopName;
    private final double price;
    private final Discount.Code discountCode;

    public Quote(String shopName, double price, Discount.Code code) {
        this.shopName = shopName;
        this.price = price;
        this.discountCode = code;
    }

    public static Quote parse(String s) {
        String[] split = s.split(":");
        String shopName = split[0];
        double price = Double.parseDouble(split[1]);
        Discount.Code discountCode = Discount.Code.valueOf(split[2]);
        return new Quote(shopName, price, discountCode);
    }

    public String getShopName() { return shopName; }
    public double getPrice() { return price; }
    public Discount.Code getDiscountCode() { return discountCode; }
}
```

### Discount 서비스

```java
public class Discount {
    public enum Code {
        NONE(0), SILVER(5), GOLD(10), PLATINUM(15), DIAMOND(20);

        private final int percentage;
        Code(int percentage) { this.percentage = percentage; }
    }

    public static String applyDiscount(Quote quote) {
        return quote.getShopName() + " price is " +
            Discount.apply(quote.getPrice(), quote.getDiscountCode());
    }

    private static double apply(double price, Code code) {
        delay();  // 할인 서비스 응답 지연 시뮬레이션
        return price * (100 - code.percentage) / 100;
    }
}
```

## 모범 사례

### 1. 적절한 Executor 선택

```java
// I/O 집약적: 큰 스레드 풀
Executor ioExecutor = Executors.newFixedThreadPool(100);

// CPU 집약적: 코어 수만큼
Executor cpuExecutor = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors());
```

### 2. 데몬 스레드 사용

```java
Executor executor = Executors.newFixedThreadPool(10, r -> {
    Thread t = new Thread(r);
    t.setDaemon(true);  // JVM 종료 시 함께 종료
    return t;
});
```

### 3. 예외 처리 체인

```java
CompletableFuture.supplyAsync(() -> riskyOperation())
    .exceptionally(ex -> fallbackValue)
    .thenApply(result -> transform(result))
    .whenComplete((result, ex) -> {
        if (ex != null) log.error("Error", ex);
        else log.info("Success: " + result);
    });
```

### 4. 취소 가능한 작업

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> longRunningTask());

// 취소
future.cancel(true);
```

## 정리

1. **CompletableFuture로 비동기 API를 쉽게 구현할 수 있다**
2. **커스텀 Executor로 스레드 풀을 세밀하게 제어할 수 있다**
3. **thenCompose와 thenCombine으로 비동기 작업을 조합할 수 있다**
4. **allOf와 anyOf로 여러 Future를 효율적으로 처리할 수 있다**
5. **타임아웃과 예외 처리로 안정성을 높일 수 있다**
6. **스트림과 결합하여 더 반응성 좋은 애플리케이션을 만들 수 있다**
