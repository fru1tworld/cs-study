package modernjavainaction.chap17.rxjava;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import modernjavainaction.chap17.TempInfo;

public class TempObservable {

  public static Observable<TempInfo> getTemperature(String town) {
    return Observable.create(emitter -> Observable.interval(1, TimeUnit.SECONDS).subscribe(i -> {
      if (!emitter.isDisposed()) {
        if (i >= 5) {
          emitter.onComplete();
        }
        else {
          try {
            emitter.onNext(TempInfo.fetch(town));
          }
          catch (Exception e) {
            emitter.onError(e);
          }
        }
      }
    }));
  }

  public static Observable<TempInfo> getCelsiusTemperature(String town) {
    return getTemperature(town)
        .map(temp -> new TempInfo(temp.getTown(), (temp.getTemp() - 32) * 5 / 9));
  }

  public static Observable<TempInfo> getNegativeTemperature(String town) {
    return getCelsiusTemperature(town)
        .filter(temp -> temp.getTemp() < 0);
  }

  public static Observable<TempInfo> getCelsiusTemperatures(String... towns) {
    return Observable.merge(Arrays.stream(towns)
        .map(TempObservable::getCelsiusTemperature)
        .collect(toList()));
  }

}
