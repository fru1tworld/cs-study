package modernjavainaction.chap17.rxjava;

import static modernjavainaction.chap17.rxjava.TempObservable.getTemperature;

import io.reactivex.Observable;
import modernjavainaction.chap17.TempInfo;

public class Main {

  public static void main(String[] args) {
    Observable<TempInfo> observable = getTemperature("New York");
    observable.subscribe(new TempObserver());

    try {
      Thread.sleep(10000L);
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
