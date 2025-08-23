package modernjavainaction.chap17;

import java.util.concurrent.Flow.Publisher;

public class Main {

  public static void main(String[] args) {
    getTemperatures("New York").subscribe(new TempSubscriber());
  }

  private static Publisher<TempInfo> getTemperatures(String town) {
    return subscriber -> subscriber.onSubscribe(new TempSubscription(subscriber, town));
  }

}
