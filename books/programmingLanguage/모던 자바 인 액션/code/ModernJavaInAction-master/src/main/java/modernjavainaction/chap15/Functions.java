package modernjavainaction.chap15;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Functions {

  public static void main(String[] args) {
    int x = 5;
    sequential(x);
    try {
      futureBased(x);
    }
    catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  public static int f(int x) {
    return x * 2;
  }

  public static int g(int x) {
    return x + 1;
  }

  public static Integer fo(int x) {
    return Integer.valueOf(x * 2);
  }

  public static Integer go(int x) {
    return Integer.valueOf(x + 1);
  }

  public static Future<Integer> ff(int x) {
    return new CompletableFuture<Integer>().completeAsync(() -> Integer.valueOf(x * 2));
  }

  public static Future<Integer> gf(int x) {
    return new CompletableFuture<Integer>().completeAsync(() -> Integer.valueOf(x + 1));
  }

  private static void sequential(int x) {
    int y = f(x);
    int z = g(x);
    System.out.println(y + z);
  }

  private static void futureBased(int x) throws InterruptedException, ExecutionException {
    Future<Integer> y = ff(x);
    Future<Integer> z = gf(x);
    System.out.println(y.get() + z.get());
  }

}
