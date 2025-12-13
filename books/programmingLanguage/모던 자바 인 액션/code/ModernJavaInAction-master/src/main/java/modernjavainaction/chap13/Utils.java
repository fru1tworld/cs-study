package modernjavainaction.chap13;

import java.util.List;

public class Utils {

  public static void paint(List<Resizable> l) {
    l.forEach(r -> {
      r.setAbsoluteSize(42, 42);
    });

    //TODO: 주석 해제, 자세한 사항은 README 참고
    //l.forEach(r -> { r.setRelativeSize(2, 2); });
  }

}
