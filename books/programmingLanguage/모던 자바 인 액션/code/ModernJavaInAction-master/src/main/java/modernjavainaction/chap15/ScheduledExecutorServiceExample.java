package modernjavainaction.chap15;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutorServiceExample {

  public static void main(String[] args) {
      ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

      work1();
      scheduledExecutorService.schedule(ScheduledExecutorServiceExample::work2, 10, TimeUnit.SECONDS);

      scheduledExecutorService.shutdown();
  }

  public static void work1() {
    System.out.println("Hello from Work1!");
  }

  public static void work2() {
    System.out.println("Hello from Work2!");
  }

}
