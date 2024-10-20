package modernjavainaction.chap16.v1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import modernjavainaction.chap16.ExchangeService;
import modernjavainaction.chap16.ExchangeService.Money;

public class BestPriceFinder {

  private final List<Shop> shops = Arrays.asList(
      new Shop("BestPrice"),
      new Shop("LetsSaveBig"),
      new Shop("MyFavoriteShop"),
      new Shop("BuyItAll")/*,
      new Shop("ShopEasy")*/);

  private final Executor executor = Executors.newFixedThreadPool(shops.size(), (Runnable r) -> {
    Thread t = new Thread(r);
    t.setDaemon(true);
    return t;
  });

  public List<String> findPricesSequential(String product) {
    return shops.stream()
        .map(shop -> shop.getName() + " price is " + shop.getPrice(product))
        .collect(Collectors.toList());
  }

  public List<String> findPricesParallel(String product) {
    return shops.parallelStream()
        .map(shop -> shop.getName() + " price is " + shop.getPrice(product))
        .collect(Collectors.toList());
  }

  public List<String> findPricesFuture(String product) {
    List<CompletableFuture<String>> priceFutures =
        shops.stream()
            .map(shop -> CompletableFuture.supplyAsync(() -> shop.getName() + " price is "
                + shop.getPrice(product), executor))
            .collect(Collectors.toList());

    List<String> prices = priceFutures.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList());
    return prices;
  }

  public List<String> findPricesInUSD(String product) {
    List<CompletableFuture<Double>> priceFutures = new ArrayList<>();
    for (Shop shop : shops) {
      // 예제 10-20 시작.
      // 아래 CompletableFuture::join와 호환되도록 futurePriceInUSD의 형식만 CompletableFuture로 바꿈.
      CompletableFuture<Double> futurePriceInUSD =
          CompletableFuture.supplyAsync(() -> shop.getPrice(product))
          .thenCombine(
              CompletableFuture.supplyAsync(
                  () ->  ExchangeService.getRate(Money.EUR, Money.USD))
              // 자바 9에 추가된 타임아웃 관리 기능
              .completeOnTimeout(ExchangeService.DEFAULT_RATE, 1, TimeUnit.SECONDS),
              (price, rate) -> price * rate
          )
          // 자바 9에 추가된 타임아웃 관리 기능
          .orTimeout(3, TimeUnit.SECONDS);
      priceFutures.add(futurePriceInUSD);
    }
    // 단점: 루프 밖에서 shop에 접근할 수 없으므로 아래 getName() 호출을 주석처리함.
    // so the getName() call below has been commented out.
    List<String> prices = priceFutures.stream()
        .map(CompletableFuture::join)
        .map(price -> /*shop.getName() +*/ " price is " + price)
        .collect(Collectors.toList());
    return prices;
  }

  public List<String> findPricesInUSDJava7(String product) {
    ExecutorService executor = Executors.newCachedThreadPool();
    List<Future<Double>> priceFutures = new ArrayList<>();
    for (Shop shop : shops) {
      final Future<Double> futureRate = executor.submit(new Callable<Double>() {
        @Override
        public Double call() {
          return ExchangeService.getRate(Money.EUR, Money.USD);
        }
      });
      Future<Double> futurePriceInUSD = executor.submit(new Callable<Double>() {
        @Override
        public Double call() {
          try {
            double priceInEUR = shop.getPrice(product);
            return priceInEUR * futureRate.get();
          }
          catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
          }
        }
      });
      priceFutures.add(futurePriceInUSD);
    }
    List<String> prices = new ArrayList<>();
    for (Future<Double> priceFuture : priceFutures) {
      try {
        prices.add(/*shop.getName() +*/ " price is " + priceFuture.get());
      }
      catch (ExecutionException | InterruptedException e) {
        e.printStackTrace();
      }
    }
    return prices;
  }

  public List<String> findPricesInUSD2(String product) {
    List<CompletableFuture<String>> priceFutures = new ArrayList<>();
    for (Shop shop : shops) {
      // 루프에서 상점 이름에 접근할 수 있도록 동작을 추가함. 결과적으로 CompletableFuture<String> 인스턴스를 사용할 수 있음.
      CompletableFuture<String> futurePriceInUSD =
          CompletableFuture.supplyAsync(() -> shop.getPrice(product))
          .thenCombine(
              CompletableFuture.supplyAsync(
                  () -> ExchangeService.getRate(Money.EUR, Money.USD)),
              (price, rate) -> price * rate
          ).thenApply(price -> shop.getName() + " price is " + price);
      priceFutures.add(futurePriceInUSD);
    }
    List<String> prices = priceFutures
        .stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList());
    return prices;
  }

  public List<String> findPricesInUSD3(String product) {
    // 루프를 매핑 함수로 바꿈...
    Stream<CompletableFuture<String>> priceFuturesStream = shops.stream()
        .map(shop -> CompletableFuture
            .supplyAsync(() -> shop.getPrice(product))
            .thenCombine(
                CompletableFuture.supplyAsync(() -> ExchangeService.getRate(Money.EUR, Money.USD)),
                (price, rate) -> price * rate)
            .thenApply(price -> shop.getName() + " price is " + price));
    // 하지만 합치기 전에 연산이 실행되도록 CompletableFuture를 리스트로 모음
    List<CompletableFuture<String>> priceFutures = priceFuturesStream.collect(Collectors.toList());
    List<String> prices = priceFutures.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList());
    return prices;
  }

}
