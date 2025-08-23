package modernjavainaction.chap17.rxjava;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import modernjavainaction.chap17.TempInfo;

public class TempObserver implements Observer<TempInfo> {

  @Override
  public void onComplete() {
    System.out.println("Done!");
  }

  @Override
  public void onError(Throwable throwable) {
    System.out.println("Got problem: " + throwable.getMessage());
  }

  @Override
  public void onSubscribe(Disposable disposable) {}

  @Override
  public void onNext(TempInfo tempInfo) {
    System.out.println(tempInfo);
  }

}
